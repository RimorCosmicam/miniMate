package com.minimate.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.MicrophoneDirection
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.NoiseSuppressor
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.minimate.touchpad.model.AudioTransport
import com.minimate.touchpad.model.AudioOutputPreset
import com.minimate.touchpad.model.AudioDeviceEqProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicInteger
import org.json.JSONArray
import org.json.JSONObject
import com.minimate.audio.MicrophoneEngine
import com.minimate.touchpad.model.MicrophonePlacement
import com.minimate.touchpad.model.MicrophoneVoicePreset
import com.minimate.touchpad.model.ThemeFilter

/** Stable sentinel key for the phone's own built-in speaker/microphone. */
const val PHONE_DEVICE_KEY = "phone"

/** One entry in a tappable device list — no separate "route" toggle, just pick one. */
data class AudioDeviceSummary(val key: String, val name: String)

private val PHONE_OUTPUT = AudioDeviceSummary(PHONE_DEVICE_KEY, "Phone")
private val PHONE_INPUT = AudioDeviceSummary(PHONE_DEVICE_KEY, "Phone")

data class AudioBridgeState(
    val listening: Boolean = false,
    val connected: Boolean = false,
    val hostName: String? = null,
    val transport: AudioTransport = AudioTransport.WIFI,
    val wifiAvailable: Boolean = false,
    val bluetoothAvailable: Boolean = false,
    val outputEnabled: Boolean = true,
    val microphoneEnabled: Boolean = true,
    val outputVolume: Float = .8f,
    val selectedOutputKey: String = PHONE_DEVICE_KEY,
    val outputDevices: List<AudioDeviceSummary> = listOf(PHONE_OUTPUT),
    val outputDeviceKey: String = "phone",
    val outputDeviceName: String = "Phone output",
    val outputPreset: AudioOutputPreset = AudioOutputPreset.FLAT,
    val outputEqGains: List<Float> = AudioOutputPreset.FLAT.gains,
    val microphoneGain: Float = 1f,
    val microphoneLevel: Float = 0f,
    val error: String? = null,
    val receivedPackets: Long = 0,
    val sentPackets: Long = 0,
    val webcamFramesSent: Long = 0
)

@SuppressLint("MissingPermission")
class BluetoothAudioBridge(private val context: Context, private val adapter: BluetoothAdapter?) {
    companion object {
        private const val TAG = "MiniMateAudio"
        const val WIFI_PORT = 42308
        const val NSD_TYPE = "_minimate-audio._tcp."
        const val WIFI_SAMPLE_RATE = 48_000
        const val WIFI_FRAMES_PER_PACKET = 960
        val OUTPUT_EQ_CUTOFFS = floatArrayOf(90f, 180f, 375f, 750f, 1_500f, 3_000f, 6_000f, 12_000f, 23_900f)
    }

    private data class Link(
        val transport: AudioTransport,
        val hostName: String,
        val input: DataInputStream,
        val output: DataOutputStream,
        val isOpen: () -> Boolean,
        val close: () -> Unit
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(AudioBridgeState(bluetoothAvailable = adapter?.isEnabled == true))
    val state: StateFlow<AudioBridgeState> = _state.asStateFlow()
    private val sequence = AtomicInteger()
    private val writeLock = Any()
    private val linkLock = Any()
    @Volatile private var running = false
    @Volatile private var activeLink: Link? = null
    private var bluetoothServer: BluetoothServerSocket? = null
    private var wifiServer: ServerSocket? = null
    private var microphoneJob: Job? = null
    /** The recorder the capture loop is currently blocked on, so a stop can unblock read()
     *  immediately instead of waiting for the current buffer to fill. */
    @Volatile private var activeRecorder: AudioRecord? = null
    @Volatile private var micGeneration = 0
    @Volatile private var microphonePreset: MicrophoneVoicePreset = MicrophoneVoicePreset.CLEAN
    @Volatile private var superhumanBands: List<Float> = emptyList()
    @Volatile private var microphonePlacement: MicrophonePlacement = MicrophonePlacement.HANDHELD
    private var audioTrack: AudioTrack? = null
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var audioTrackRate = 0
    private var audioTrackEncoding = 0
    @Volatile private var deviceEqProfiles: List<AudioDeviceEqProfile> = emptyList()
    private var nsdRegistration: NsdManager.RegistrationListener? = null
    private val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = updateNetworkAvailability()
        override fun onLost(network: Network) = updateNetworkAvailability()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = updateNetworkAvailability()
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = updateAudioDevices()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = updateAudioDevices()
    }

    fun configure(
        outputEnabled: Boolean,
        microphoneEnabled: Boolean,
        outputVolume: Float,
        outputDeviceKey: String,
        outputProfiles: List<AudioDeviceEqProfile>,
        microphoneGain: Float,
        microphonePreset: MicrophoneVoicePreset = MicrophoneVoicePreset.CLEAN,
        superhumanBands: List<Float> = emptyList(),
        microphonePlacement: MicrophonePlacement = MicrophonePlacement.HANDHELD
    ) {
        this.microphonePreset = microphonePreset
        this.superhumanBands = superhumanBands
        this.microphonePlacement = microphonePlacement
        val restartMicrophone = false
        deviceEqProfiles = outputProfiles
        _state.update {
            val profile = outputProfiles.firstOrNull { profile -> profile.deviceKey == it.outputDeviceKey }
            it.copy(
                outputEnabled = outputEnabled,
                microphoneEnabled = microphoneEnabled,
                outputVolume = outputVolume.coerceIn(0f, 1f),
                selectedOutputKey = outputDeviceKey,
                outputPreset = profile?.preset ?: AudioOutputPreset.FLAT,
                outputEqGains = profile?.gains?.takeIf { gains -> gains.size == 9 }?.map { gain -> gain.coerceIn(-12f, 12f) }
                    ?: AudioOutputPreset.FLAT.gains,
                microphoneGain = microphoneGain.coerceIn(0f, 3f),
                error = null
            )
        }
        updateAudioDevices()
        audioTrack?.setVolume(outputVolume.coerceIn(0f, 1f))
        audioTrack?.setPreferredDevice(preferredOutputDevice())
        applyOutputProcessing()
        if (restartMicrophone && microphoneJob?.isActive == true) {
            // Wait for the previous session to fully tear down before opening the next one.
            // A fixed delay was a guess: cancellation is cooperative and the old loop sits
            // inside a blocking read, so the new AudioRecord could be created while the old
            // one still held the route — two recorders on one device, heard as artefacts and
            // noise until the user manually toggled the input off and on again.
            val previous = microphoneJob
            microphoneJob = null
            stopMicrophone()
            if (microphoneEnabled && activeLink != null) scope.launch {
                previous?.join()
                startMicrophone()
            }
        } else if (microphoneEnabled && activeLink != null) startMicrophone() else stopMicrophone()
    }

    fun start() {
        if (running) return
        running = true
        runCatching {
            connectivity.registerNetworkCallback(
                NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
                networkCallback
            )
        }
        updateNetworkAvailability()
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        updateAudioDevices()
        scope.launch { bluetoothAcceptLoop() }
        scope.launch { wifiAcceptLoop() }
    }

    private fun updateNetworkAvailability() {
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        val available = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        _state.update { it.copy(wifiAvailable = available, bluetoothAvailable = adapter?.isEnabled == true) }
    }

    private fun bluetoothAcceptLoop() {
        while (running) {
            try {
                if (adapter?.isEnabled != true) return
                bluetoothServer = adapter.listenUsingRfcommWithServiceRecord(AudioBridgeProtocol.SERVICE_NAME, AudioBridgeProtocol.SERVICE_UUID)
                val socket = bluetoothServer?.accept() ?: return
                bluetoothServer?.close()
                bluetoothServer = null
                val link = socket.asLink()
                if (activeLink?.transport == AudioTransport.WIFI) socket.close() else runLink(link)
            } catch (e: Exception) {
                if (running && e !is EOFException && e !is SocketException) Log.w(TAG, "Bluetooth audio listener restarted", e)
            }
        }
    }

    private fun wifiAcceptLoop() {
        while (running) {
            try {
                wifiServer = ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(WIFI_PORT)) }
                registerNsd()
                _state.update { it.copy(listening = true) }
                val socket = wifiServer?.accept() ?: return
                // Lossless Wi-Fi automatically replaces a Bluetooth fallback connection.
                runLink(socket.asLink())
            } catch (e: Exception) {
                if (running && e !is EOFException && e !is SocketException) {
                    Log.w(TAG, "Wi-Fi audio listener restarted", e)
                    _state.update { it.copy(error = e.message ?: "Wi-Fi audio listener failed") }
                }
            } finally {
                unregisterNsd()
                runCatching { wifiServer?.close() }
                wifiServer = null
            }
        }
    }

    private fun runLink(link: Link) {
        Log.i(TAG, "runLink: connected via ${link.transport} to ${link.hostName}")
        synchronized(linkLock) { activeLink?.close?.invoke(); activeLink = link }
        _state.update {
            it.copy(
                listening = false,
                connected = true,
                hostName = link.hostName,
                transport = link.transport,
                error = null
            )
        }
        val codec = if (link.transport == AudioTransport.WIFI) AudioBridgeProtocol.CODEC_PCM24 else AudioBridgeProtocol.CODEC_IMA_ADPCM
        val rate = if (link.transport == AudioTransport.WIFI) WIFI_SAMPLE_RATE else AudioBridgeProtocol.SAMPLE_RATE
        writeFrame(link.output, AudioBridgeProtocol.Frame(
            AudioBridgeProtocol.TYPE_HELLO, codec, AudioBridgeProtocol.PLAYBACK_CHANNELS, rate,
            sequence.getAndIncrement(),
            byteArrayOf(if (_state.value.outputEnabled) 1 else 0, if (_state.value.microphoneEnabled) 1 else 0)
        ))
        if (_state.value.microphoneEnabled) startMicrophone(link)
        try {
            while (running && link.isOpen()) {
                val frame = AudioBridgeProtocol.read(link.input)
                when (frame.type) {
                    AudioBridgeProtocol.TYPE_PLAYBACK -> if (_state.value.outputEnabled) play(frame)
                    AudioBridgeProtocol.TYPE_PING -> writeFrame(link.output, frame.copy(sequence = sequence.getAndIncrement()))
                }
                _state.update { it.copy(receivedPackets = it.receivedPackets + 1) }
            }
        } finally {
            disconnectLink(link)
        }
    }

    private fun play(frame: AudioBridgeProtocol.Frame) {
        if (frame.channels != 2 || frame.sampleRate !in 16_000..48_000) return
        val encoding = if (frame.codec == AudioBridgeProtocol.CODEC_PCM24) AudioFormat.ENCODING_PCM_24BIT_PACKED else AudioFormat.ENCODING_PCM_16BIT
        val track = if (audioTrack == null || audioTrackRate != frame.sampleRate || audioTrackEncoding != encoding) {
            releaseTrack()
            createAudioTrack(frame.sampleRate, encoding).also {
                audioTrack = it
                audioTrackRate = frame.sampleRate
                audioTrackEncoding = encoding
                it.setPreferredDevice(preferredOutputDevice())
                attachOutputProcessing(it)
                it.play()
            }
        } else audioTrack!!
        track.setVolume(_state.value.outputVolume)
        when (frame.codec) {
            AudioBridgeProtocol.CODEC_PCM24 -> track.write(frame.payload, 0, frame.payload.size, AudioTrack.WRITE_BLOCKING)
            AudioBridgeProtocol.CODEC_IMA_ADPCM -> {
                val samples = ImaAdpcm.decode(frame.payload, 2)
                track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            }
            AudioBridgeProtocol.CODEC_PCM16 -> {
                val samples = frame.payload.toShortArrayLittleEndian()
                track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            }
            else -> return
        }
    }

    private fun createAudioTrack(sampleRate: Int, encoding: Int): AudioTrack {
        val min = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, encoding)
        return AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(encoding).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
            .setBufferSizeInBytes(maxOf(min, sampleRate / 50 * 2 * (if (encoding == AudioFormat.ENCODING_PCM_24BIT_PACKED) 3 else 2) * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
    }

    private fun attachOutputProcessing(track: AudioTrack) {
        runCatching { dynamicsProcessing?.release() }
        dynamicsProcessing = runCatching {
            val config = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                2,
                true,
                OUTPUT_EQ_CUTOFFS.size,
                false,
                0,
                false,
                0,
                false
            ).setPreEqAllChannelsTo(buildOutputEq())
                .setInputGainAllChannelsTo(-(_state.value.outputEqGains.maxOrNull() ?: 0f).coerceAtLeast(0f))
                .build()
            DynamicsProcessing(0, track.audioSessionId, config).apply { enabled = true }
        }.onFailure { Log.w(TAG, "Dynamics EQ unavailable", it) }.getOrNull()
    }

    private fun applyOutputProcessing() {
        val effect = dynamicsProcessing ?: return
        runCatching {
            effect.setPreEqAllChannelsTo(buildOutputEq())
            effect.setInputGainAllChannelsTo(-(_state.value.outputEqGains.maxOrNull() ?: 0f).coerceAtLeast(0f))
        }.onFailure { Log.w(TAG, "Unable to update output EQ", it) }
    }

    private fun buildOutputEq() = DynamicsProcessing.Eq(true, true, OUTPUT_EQ_CUTOFFS.size).apply {
        val gains = _state.value.outputEqGains
        val maximumCutoff = audioTrackRate.coerceAtLeast(AudioBridgeProtocol.SAMPLE_RATE) / 2f - 100f
        OUTPUT_EQ_CUTOFFS.forEachIndexed { index, cutoff ->
            setBand(index, DynamicsProcessing.EqBand(true, cutoff.coerceAtMost(maximumCutoff), gains.getOrElse(index) { 0f }))
        }
    }

    private fun updateAudioDevices() {
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val outputDevices = listOf(PHONE_OUTPUT) + outputs.filter(::isSelectableOutput).map { it.toSummary() }
        val resolvedOutput = outputs.firstOrNull { it.toKey() == _state.value.selectedOutputKey }
        val key = resolvedOutput?.toKey() ?: PHONE_DEVICE_KEY
        val name = resolvedOutput?.let(::deviceLabel) ?: "Phone output"
        val profile = deviceEqProfiles.firstOrNull { it.deviceKey == key }
        _state.update {
            it.copy(
                outputDevices = outputDevices,
                outputDeviceKey = key,
                outputDeviceName = name,
                outputPreset = profile?.preset ?: AudioOutputPreset.FLAT,
                outputEqGains = profile?.gains ?: AudioOutputPreset.FLAT.gains
            )
        }
        applyOutputProcessing()
    }

    private fun preferredOutputDevice(): AudioDeviceInfo? {
        val key = _state.value.selectedOutputKey
        if (key == PHONE_DEVICE_KEY) {
            return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
        }
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { it.toKey() == key }
    }

    /**
     * Always the phone's own microphone array. External microphones are not supported: a
     * cable-mounted capsule sits far from the mouth with no array to steer, and measured
     * signal-to-noise on one was effectively zero, which no amount of processing can recover.
     * There is deliberately no way to select anything else.
     */
    private fun preferredInputDevice(): AudioDeviceInfo? = preferredBuiltInInputDevice()

    private fun preferredBuiltInInputDevice(
        devices: Array<AudioDeviceInfo> = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
    ): AudioDeviceInfo? = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }

    private fun isSelectableOutput(device: AudioDeviceInfo): Boolean = when (device.type) {
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> true
        else -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && device.type == AudioDeviceInfo.TYPE_BLE_HEADSET
    }

    private fun AudioDeviceInfo.toKey(): String = "$type:${address.ifBlank { productName?.toString().orEmpty() }}"

    private fun deviceLabel(device: AudioDeviceInfo): String =
        device.productName?.toString()?.takeIf { it.isNotBlank() } ?: when (device.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth device"
            AudioDeviceInfo.TYPE_BLE_HEADSET -> "Bluetooth headset"
            AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> "USB device"
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headset"
            else -> "Connected device"
        }

    private fun AudioDeviceInfo.toSummary() = AudioDeviceSummary(toKey(), deviceLabel(this))

    private fun startMicrophone(explicitLink: Link? = null) {
        if (microphoneJob?.isActive == true) {
            Log.i(TAG, "startMicrophone: already running, ignoring")
            return
        }
        val link = explicitLink ?: activeLink ?: run {
            Log.i(TAG, "startMicrophone: no active link, aborting")
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "startMicrophone: RECORD_AUDIO not granted")
            _state.update { it.copy(error = "Microphone permission is required") }
            return
        }
        val myGeneration = ++micGeneration
        microphoneJob = scope.launch {
            val targetDevice = preferredInputDevice()
            Log.i(TAG, "startMicrophone: builtin mic targetDevice=${targetDevice?.type} id=${targetDevice?.id} product=${targetDevice?.productName}")
            val sampleRate = if (link.transport == AudioTransport.WIFI) WIFI_SAMPLE_RATE else AudioBridgeProtocol.SAMPLE_RATE
            val frames = if (link.transport == AudioTransport.WIFI) WIFI_FRAMES_PER_PACKET else AudioBridgeProtocol.FRAMES_PER_PACKET
            val min = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            // One capture configuration, always: the phone array with speech-tuned capture and
            // beamforming. This is what the last version anyone was happy with used, and with
            // external microphones gone there is nothing left to choose between.
            val isPhoneMic = true
            val chosenSource = MediaRecorder.AudioSource.VOICE_RECOGNITION
            val recorder = AudioRecord(
                chosenSource,
                sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, maxOf(min, frames * 2 * 4)
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                Log.w(TAG, "startMicrophone: AudioRecord failed to initialize, state=${recorder.state}")
                _state.update { it.copy(error = "Microphone failed to initialize") }
                recorder.release()
                return@launch
            }
            val preferSet = recorder.setPreferredDevice(targetDevice)
            Log.i(TAG, "startMicrophone: setPreferredDevice($targetDevice) returned $preferSet, routedDevice=${recorder.preferredDevice?.type}/${recorder.preferredDevice?.id}")
            // Beamforming. This is the single largest quality lever available on the phone
            // microphone and it runs in the audio HAL against the physical capsule array, so
            // nothing applied afterwards in software can substitute for it: the hardware steers
            // its pickup toward the user and rejects the rest of the room before the signal is
            // ever digitised. Removing these two calls while stripping out noise processing is
            // what cost the phone microphone its quality.
            //
            // Restricted to the built-in array on purpose. An external single-capsule mic has no
            // array to steer, and applying direction hints there previously produced garbled
            // audio rather than no effect.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isPhoneMic) {
                runCatching {
                    recorder.setPreferredMicrophoneDirection(MicrophoneDirection.MIC_DIRECTION_TOWARDS_USER)
                    recorder.setPreferredMicrophoneFieldDimension(microphonePlacement.fieldDimension)
                }.onFailure { Log.w(TAG, "startMicrophone: beamforming unavailable", it) }
                Log.i(TAG, "startMicrophone: beamforming towards user, placement=${microphonePlacement.name} field=${microphonePlacement.fieldDimension}")
            }
            // Platform AGC pre-processor. This is the piece that was missing: it runs inside the
            // capture chain, so it raises a low-sensitivity capsule's signal before we ever see
            // it, which post-hoc digital gain fundamentally cannot do (amplifying afterwards
            // amplifies the quantisation/self noise by exactly the same factor). Unlike the
            // VOICE_COMMUNICATION source, this is a targeted effect and does not drag the
            // capture through the telephony narrowband path.
            val automaticGain = if (AutomaticGainControl.isAvailable()) {
                runCatching {
                    AutomaticGainControl.create(recorder.audioSessionId)?.apply { enabled = true }
                }.onFailure { Log.w(TAG, "startMicrophone: AGC effect failed", it) }.getOrNull()
            } else null
            // Platform noise suppression, on for every microphone. This is a single-channel
            // spectral effect, so it needs no mic array and works for external capsules too.
            val noiseSuppressor = if (NoiseSuppressor.isAvailable()) {
                runCatching {
                    NoiseSuppressor.create(recorder.audioSessionId)?.apply { enabled = true }
                }.onFailure { Log.w(TAG, "startMicrophone: NoiseSuppressor failed", it) }.getOrNull()
            } else null
            // Echo cancellation only makes sense against the phone's own array, which is what it
            // is calibrated for; it also stops the phone's speaker leaking back into capture.
            val echoCanceler = if (isPhoneMic && AcousticEchoCanceler.isAvailable()) {
                runCatching {
                    AcousticEchoCanceler.create(recorder.audioSessionId)?.apply { enabled = true }
                }.onFailure { Log.w(TAG, "startMicrophone: AEC failed", it) }.getOrNull()
            } else null
            Log.i(TAG, "startMicrophone: phoneMic=$isPhoneMic source=$chosenSource; AGC available=${AutomaticGainControl.isAvailable()} enabled=${automaticGain?.enabled}; NS available=${NoiseSuppressor.isAvailable()} enabled=${noiseSuppressor?.enabled}; AEC enabled=${echoCanceler?.enabled}")
            activeRecorder = recorder
            val samples = ShortArray(frames)
            val engine = MicrophoneEngine(sampleRate)
            try {
                recorder.startRecording()
                check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Microphone did not start recording" }
                Log.i(TAG, "startMicrophone: source=$chosenSource recording started, actual routed device=${recorder.routedDevice?.type}/${recorder.routedDevice?.id}/${recorder.routedDevice?.productName}")
                var consecutiveEmptyReads = 0
                // Rolling 1-second window of the LOUDEST moment, not a random snapshot — a
                // sparse per-N-reads sample mostly lands between words and looks falsely quiet.
                var windowRawPeak = 0
                var windowOutPeak = 0
                var windowStart = System.nanoTime()
                // isActive must be checked here: AudioRecord.read(READ_BLOCKING) is a plain
                // blocking call, not a suspend function, so cancelling this job from
                // stopMicrophone() has zero effect unless this loop itself observes it. Without
                // this, switching input devices leaves the OLD capture loop running forever
                // alongside the new one — two AudioRecord sessions fighting over the same
                // physical route, which is why the newly selected device reads silence.
                while (isActive && running && link === activeLink && link.isOpen() && _state.value.microphoneEnabled) {
                    val count = recorder.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                    if (count <= 0) {
                        // A misrouted device (e.g. Bluetooth audio that never actually connected)
                        // reads 0/negative forever. Without a bound this loop never exits, the
                        // job stays "active" forever, and every later attempt — including
                        // switching back to the phone mic — silently no-ops against that guard.
                        check(++consecutiveEmptyReads < 200) { "Microphone stopped producing audio" }
                        continue
                    }
                    consecutiveEmptyReads = 0
                    val gain = _state.value.microphoneGain
                    val adjusted = engine.process(samples, count, gain, microphonePreset, superhumanBands, microphonePlacement.gainScale)
                    for (index in 0 until count) {
                        val rawAbs = kotlin.math.abs(samples[index].toInt())
                        if (rawAbs > windowRawPeak) windowRawPeak = rawAbs
                        val outAbs = kotlin.math.abs(adjusted[index].toInt())
                        if (outAbs > windowOutPeak) windowOutPeak = outAbs
                    }
                    val now = System.nanoTime()
                    if (now - windowStart > 1_000_000_000L) {
                        Log.i(TAG, "startMicrophone: 1s window rawPeak=$windowRawPeak (${"%.1f".format(windowRawPeak * 100f / Short.MAX_VALUE)}% FS) rms=${"%.0f".format(engine.lastStats?.rawRms ?: 0f)} noise=${"%.0f".format(engine.lastStats?.noiseRms ?: 0f)} speech=${engine.lastStats?.voiceActive} trim=$gain gain=${"%.1f".format(engine.lastStats?.gain ?: 0f)} outPeak=$windowOutPeak (${"%.1f".format(windowOutPeak * 100f / Short.MAX_VALUE)}% FS) routedDevice=${recorder.routedDevice?.id}")
                        windowRawPeak = 0
                        windowOutPeak = 0
                        windowStart = now
                    }
                    // Mono mic audio at this sample rate is ~64 kbps either way — trivial for
                    // even classic Bluetooth RFCOMM. There's no bandwidth reason to lossy-compress
                    // it with IMA-ADPCM on the Bluetooth path; both companions already decode
                    // PCM16 mic frames regardless of transport, so always send it raw.
                    val payload = adjusted.toByteArrayLittleEndian()
                    writeFrame(link.output, AudioBridgeProtocol.Frame(
                        AudioBridgeProtocol.TYPE_MICROPHONE, AudioBridgeProtocol.CODEC_PCM16, 1, sampleRate,
                        sequence.getAndIncrement(), payload
                    ))
                    _state.update { it.copy(sentPackets = it.sentPackets + 1, microphoneLevel = engine.level) }
                }
            } catch (e: Exception) {
                Log.w(TAG, "startMicrophone: capture loop ended", e)
                if (running && link === activeLink) _state.update { it.copy(error = e.message ?: "Microphone stream failed") }
            } finally {
                if (activeRecorder === recorder) activeRecorder = null
                runCatching { recorder.stop() }
                runCatching { automaticGain?.release() }
                runCatching { noiseSuppressor?.release() }
                runCatching { echoCanceler?.release() }
                recorder.release()
            }
        }
    }

    private fun writeFrame(output: DataOutputStream, frame: AudioBridgeProtocol.Frame) {
        synchronized(writeLock) { AudioBridgeProtocol.write(output, frame) }
    }

    fun sendWebcamFrame(jpeg: ByteArray) {
        val link = activeLink ?: return
        // RFCOMM remains available for audio and control fallback, but live
        // camera imagery is Wi-Fi only so quality never collapses silently.
        if (link.transport != AudioTransport.WIFI || jpeg.isEmpty()) return
        runCatching {
            writeFrame(link.output, AudioBridgeProtocol.Frame(
                AudioBridgeProtocol.TYPE_WEBCAM_JPEG,
                AudioBridgeProtocol.CODEC_JPEG,
                0,
                0,
                sequence.getAndIncrement(),
                jpeg
            ))
            _state.update { it.copy(webcamFramesSent = it.webcamFramesSent + 1) }
        }.onFailure { Log.w(TAG, "Unable to send webcam frame", it) }
    }

    fun sendWebcamConfiguration(
        enabled: Boolean,
        mirror: Boolean,
        intensity: Float,
        filters: List<ThemeFilter>
    ) {
        val link = activeLink ?: return
        val payload = JSONObject().apply {
            put("enabled", enabled)
            put("mirror", mirror)
            put("intensity", intensity.coerceIn(0f, 1f).toDouble())
            put("filters", JSONArray(filters.filter { it != ThemeFilter.NONE }.map { it.name }))
        }.toString().toByteArray(Charsets.UTF_8)
        runCatching {
            writeFrame(link.output, AudioBridgeProtocol.Frame(
                AudioBridgeProtocol.TYPE_WEBCAM_CONFIG,
                AudioBridgeProtocol.CODEC_JSON,
                0,
                0,
                sequence.getAndIncrement(),
                payload
            ))
        }.onFailure { Log.w(TAG, "Unable to send webcam configuration", it) }
    }

    private fun stopMicrophone() {
        microphoneJob?.cancel()
        microphoneJob = null
        // Cancellation alone cannot interrupt AudioRecord.read(), so stop the recorder directly.
        // This returns the pending read at once and lets the loop observe cancellation now
        // rather than after the current buffer fills.
        runCatching { activeRecorder?.stop() }
        _state.update { it.copy(microphoneLevel = 0f) }
    }

    private fun disconnectLink(link: Link) {
        synchronized(linkLock) {
            if (activeLink !== link) return
            stopMicrophone()
            link.close()
            activeLink = null
            releaseTrack()
            _state.update { it.copy(connected = false, hostName = null, listening = true) }
        }
    }

    private fun disconnectActiveLink() { activeLink?.let(::disconnectLink) }

    private fun releaseTrack() {
        runCatching { dynamicsProcessing?.release() }
        dynamicsProcessing = null
        audioTrack?.let { runCatching { it.stop() }; it.release() }
        audioTrack = null
        audioTrackRate = 0
        audioTrackEncoding = 0
    }

    private fun BluetoothSocket.asLink() = Link(
        AudioTransport.BLUETOOTH, remoteDevice.name ?: "Bluetooth desktop",
        DataInputStream(inputStream.buffered()), DataOutputStream(outputStream),
        { isConnected }, { runCatching { close() } }
    )

    private fun Socket.asLink() = Link(
        AudioTransport.WIFI, inetAddress.hostName ?: inetAddress.hostAddress ?: "Wi-Fi desktop",
        DataInputStream(getInputStream().buffered()), DataOutputStream(getOutputStream().buffered()),
        { isConnected && !isClosed }, { runCatching { close() } }
    )

    private fun registerNsd() {
        if (nsdRegistration != null) return
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) { Log.w(TAG, "mDNS registration failed: $errorCode") }
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        }
        nsdRegistration = listener
        val info = NsdServiceInfo().apply {
            serviceName = "MiniMate-${Build.MODEL}"
            serviceType = NSD_TYPE
            port = WIFI_PORT
        }
        runCatching { nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure { Log.w(TAG, "Unable to advertise Wi-Fi audio", it) }
    }

    private fun unregisterNsd() {
        val listener = nsdRegistration ?: return
        nsdRegistration = null
        runCatching { nsd.unregisterService(listener) }
    }

    fun close() {
        running = false
        runCatching { connectivity.unregisterNetworkCallback(networkCallback) }
        runCatching { audioManager.unregisterAudioDeviceCallback(audioDeviceCallback) }
        unregisterNsd()
        runCatching { bluetoothServer?.close() }
        runCatching { wifiServer?.close() }
        disconnectActiveLink()
        releaseTrack()
        scope.cancel()
    }
}

private fun ByteArray.toShortArrayLittleEndian(): ShortArray {
    val output = ShortArray(size / 2)
    for (index in output.indices) output[index] = ((this[index * 2].toInt().and(0xFF)) or (this[index * 2 + 1].toInt() shl 8)).toShort()
    return output
}

private fun ShortArray.toByteArrayLittleEndian(): ByteArray {
    val output = ByteArray(size * 2)
    forEachIndexed { index, sample ->
        output[index * 2] = sample.toInt().toByte()
        output[index * 2 + 1] = (sample.toInt() shr 8).toByte()
    }
    return output
}
