package com.minimate.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.minimate.touchpad.model.MicrophoneVoicePreset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh
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
    val selectedInputKey: String = PHONE_DEVICE_KEY,
    val inputDevices: List<AudioDeviceSummary> = listOf(PHONE_INPUT),
    val voiceIsolation: Boolean = true,
    val ambientReferenceActive: Boolean = false,
    val microphoneNoiseGate: Float = .015f,
    val microphonePreset: MicrophoneVoicePreset = MicrophoneVoicePreset.CLEAN,
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
    @Volatile private var micGeneration = 0
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
        inputDeviceKey: String,
        voiceIsolation: Boolean,
        microphoneNoiseGate: Float,
        microphonePreset: MicrophoneVoicePreset
    ) {
        val oldState = _state.value
        val restartMicrophone = oldState.selectedInputKey != inputDeviceKey || oldState.voiceIsolation != voiceIsolation
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
                microphoneGain = microphoneGain.coerceIn(0f, 2f),
                selectedInputKey = inputDeviceKey,
                voiceIsolation = voiceIsolation,
                microphoneNoiseGate = microphoneNoiseGate.coerceIn(0f, .15f),
                microphonePreset = microphonePreset,
                error = null
            )
        }
        updateAudioDevices()
        audioTrack?.setVolume(outputVolume.coerceIn(0f, 1f))
        audioTrack?.setPreferredDevice(preferredOutputDevice())
        applyOutputProcessing()
        if (restartMicrophone && microphoneJob?.isActive == true) {
            stopMicrophone()
            if (microphoneEnabled && activeLink != null) scope.launch { delay(80); startMicrophone() }
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
        val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        val outputDevices = listOf(PHONE_OUTPUT) + outputs.filter(::isSelectableOutput).map { it.toSummary() }
        val inputDevices = listOf(PHONE_INPUT) + inputs.filter(::isSelectableInput).map { it.toSummary() }
        val resolvedOutput = outputs.firstOrNull { it.toKey() == _state.value.selectedOutputKey }
        val key = resolvedOutput?.toKey() ?: PHONE_DEVICE_KEY
        val name = resolvedOutput?.let(::deviceLabel) ?: "Phone output"
        val profile = deviceEqProfiles.firstOrNull { it.deviceKey == key }
        _state.update {
            it.copy(
                outputDevices = outputDevices,
                inputDevices = inputDevices,
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

    private fun preferredInputDevice(): AudioDeviceInfo? {
        val key = _state.value.selectedInputKey
        if (key == PHONE_DEVICE_KEY) return preferredBuiltInInputDevice()
        return audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).firstOrNull { it.toKey() == key }
    }

    private fun preferredBuiltInInputDevice(
        devices: Array<AudioDeviceInfo> = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
    ): AudioDeviceInfo? = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }

    private fun isSelectableOutput(device: AudioDeviceInfo): Boolean = when (device.type) {
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> true
        else -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && device.type == AudioDeviceInfo.TYPE_BLE_HEADSET
    }

    private fun isSelectableInput(device: AudioDeviceInfo): Boolean = when (device.type) {
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> true
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

    /**
     * Bluetooth carries a microphone signal only over its SCO (classic) or BLE-audio link — never
     * over A2DP. Selecting such a device for input requires actively engaging that link first, or
     * AudioRecord silently keeps capturing from whatever the system defaulted to.
     */
    private fun needsCommunicationRouting(device: AudioDeviceInfo?): Boolean = device != null && (
        device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && device.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
        )

    @Volatile private var scoReceiver: BroadcastReceiver? = null
    @Volatile private var scoActive = false

    private suspend fun engageInputRouting(device: AudioDeviceInfo?): Boolean {
        if (!needsCommunicationRouting(device)) return true
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { audioManager.setCommunicationDevice(device!!) }.getOrDefault(false)
        } else {
            startClassicSco()
        }
    }

    private fun releaseInputRouting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { audioManager.clearCommunicationDevice() }
        } else if (scoActive) {
            stopClassicSco()
        }
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    private suspend fun startClassicSco(): Boolean {
        if (scoActive) return true
        val connected = CompletableDeferred<Boolean>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                when (intent?.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)) {
                    AudioManager.SCO_AUDIO_STATE_CONNECTED -> connected.complete(true)
                    AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> connected.complete(false)
                }
            }
        }
        scoReceiver = receiver
        val filter = IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        audioManager.startBluetoothSco()
        audioManager.isBluetoothScoOn = true
        val result = withTimeoutOrNull(4_000) { connected.await() } ?: false
        scoActive = result
        if (!result) {
            runCatching { context.unregisterReceiver(receiver) }
            scoReceiver = null
        }
        return result
    }

    private fun stopClassicSco() {
        scoActive = false
        audioManager.isBluetoothScoOn = false
        runCatching { audioManager.stopBluetoothSco() }
        scoReceiver?.let { runCatching { context.unregisterReceiver(it) } }
        scoReceiver = null
    }

    private fun startMicrophone(explicitLink: Link? = null) {
        if (microphoneJob?.isActive == true) return
        val link = explicitLink ?: activeLink ?: return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _state.update { it.copy(error = "Microphone permission is required") }
            return
        }
        val myGeneration = ++micGeneration
        microphoneJob = scope.launch {
            val initialState = _state.value
            val isPhoneMic = initialState.selectedInputKey == PHONE_DEVICE_KEY
            val targetDevice = preferredInputDevice()
            val routed = engageInputRouting(targetDevice)
            if (!isPhoneMic && !routed) {
                _state.update { it.copy(error = "Couldn't connect to the selected microphone") }
            }
            // Communication-device routing takes a moment to actually apply; starting the
            // recorder before it does silently captures nothing from the intended device.
            if (!isPhoneMic && routed) delay(200)
            val sampleRate = if (link.transport == AudioTransport.WIFI) WIFI_SAMPLE_RATE else AudioBridgeProtocol.SAMPLE_RATE
            val frames = if (link.transport == AudioTransport.WIFI) WIFI_FRAMES_PER_PACKET else AudioBridgeProtocol.FRAMES_PER_PACKET
            val min = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val recorder = AudioRecord(
                if (isPhoneMic && initialState.voiceIsolation) {
                    MediaRecorder.AudioSource.VOICE_RECOGNITION
                } else MediaRecorder.AudioSource.MIC,
                sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, maxOf(min, frames * 2 * 4)
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                _state.update { it.copy(error = "Microphone failed to initialize") }
                recorder.release()
                releaseInputRouting()
                return@launch
            }
            recorder.setPreferredDevice(targetDevice)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Direction/field-dimension beamforming hints describe the phone's OWN
                // multi-capsule mic array. Applying them to an external mic's single-capsule
                // mono signal (wired or Bluetooth) has no array to shape and was producing
                // garbled/warbling audio there — restrict both to the phone mic.
                if (isPhoneMic) {
                    recorder.setPreferredMicrophoneDirection(MicrophoneDirection.MIC_DIRECTION_TOWARDS_USER)
                    recorder.setPreferredMicrophoneFieldDimension(if (initialState.voiceIsolation) .75f else 0f)
                } else {
                    recorder.setPreferredMicrophoneDirection(MicrophoneDirection.MIC_DIRECTION_EXTERNAL)
                }
            }
            val noiseSuppressor = if (initialState.voiceIsolation && isPhoneMic && NoiseSuppressor.isAvailable()) {
                runCatching { NoiseSuppressor.create(recorder.audioSessionId)?.apply { enabled = true } }.getOrNull()
            } else null
            val echoCanceler = if (initialState.voiceIsolation && isPhoneMic && AcousticEchoCanceler.isAvailable()) {
                runCatching { AcousticEchoCanceler.create(recorder.audioSessionId)?.apply { enabled = true } }.getOrNull()
            } else null
            val samples = ShortArray(frames)
            val processor = MicrophoneProcessor(sampleRate)
            var ambientReference: AmbientReferenceCapture? = null
            try {
                recorder.startRecording()
                check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Microphone did not start recording" }
                if (initialState.voiceIsolation && !isPhoneMic) {
                    ambientReference = AmbientReferenceCapture.create(
                        sampleRate = sampleRate,
                        frames = frames,
                        device = preferredBuiltInInputDevice()
                    )?.takeIf { it.start() }
                    _state.update { it.copy(ambientReferenceActive = ambientReference != null) }
                }
                var consecutiveEmptyReads = 0
                while (running && link === activeLink && link.isOpen() && _state.value.microphoneEnabled) {
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
                    val current = _state.value
                    val adjusted = processor.process(
                        samples = samples,
                        count = count,
                        gain = current.microphoneGain,
                        gateThreshold = current.microphoneNoiseGate,
                        preset = current.microphonePreset,
                        ambientNoiseLevel = ambientReference?.noiseFloor ?: 0f
                    )
                    val codec = if (link.transport == AudioTransport.WIFI) AudioBridgeProtocol.CODEC_PCM16 else AudioBridgeProtocol.CODEC_IMA_ADPCM
                    val payload = if (codec == AudioBridgeProtocol.CODEC_PCM16) adjusted.toByteArrayLittleEndian() else ImaAdpcm.encode(adjusted, 1)
                    writeFrame(link.output, AudioBridgeProtocol.Frame(
                        AudioBridgeProtocol.TYPE_MICROPHONE, codec, 1, sampleRate,
                        sequence.getAndIncrement(), payload
                    ))
                    _state.update { it.copy(sentPackets = it.sentPackets + 1, microphoneLevel = processor.level) }
                }
            } catch (e: Exception) {
                if (running && link === activeLink) _state.update { it.copy(error = e.message ?: "Microphone stream failed") }
            } finally {
                ambientReference?.close()
                runCatching { recorder.stop() }
                noiseSuppressor?.release()
                echoCanceler?.release()
                recorder.release()
                // Only the most recent session releases routing: if a newer one has already
                // started (e.g. the user switched devices while this one was still tearing
                // down), it owns cleanup now, and releasing here would stomp its routing instead.
                if (myGeneration == micGeneration) releaseInputRouting()
                _state.update { it.copy(ambientReferenceActive = false) }
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
        _state.update { it.copy(microphoneLevel = 0f, ambientReferenceActive = false) }
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
        releaseInputRouting()
        scope.cancel()
    }
}

/**
 * Captures the phone mic array as an environment-only reference while an external mic is primary.
 * If Android does not allow concurrent capture, creation/start fails silently and the normal
 * platform noise suppressor remains active on the external microphone.
 */
private class AmbientReferenceCapture private constructor(
    private val recorder: AudioRecord,
    private val frames: Int
) {
    private val running = AtomicBoolean(false)
    private var worker: Thread? = null
    @Volatile var noiseFloor: Float = 0f
        private set

    fun start(): Boolean = runCatching {
        recorder.startRecording()
        check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING)
        running.set(true)
        worker = thread(name = "MiniMate-phone-mic-reference", isDaemon = true) {
            val buffer = ShortArray(frames)
            while (running.get()) {
                val count = recorder.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                if (count <= 0) continue
                var energy = 0.0
                for (index in 0 until count) {
                    val sample = buffer[index].toDouble()
                    energy += sample * sample
                }
                val measured = (sqrt(energy / count) / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)
                // Follow quieter ambience quickly, but require sustained sound before raising the
                // reference floor so speech leaking into the phone array is not treated as noise.
                val smoothing = if (measured < noiseFloor) .18f else .025f
                noiseFloor += (measured - noiseFloor) * smoothing
            }
        }
        true
    }.getOrElse {
        close()
        false
    }

    fun close() {
        running.set(false)
        runCatching { recorder.stop() }
        runCatching { worker?.join(120) }
        worker = null
        recorder.release()
        noiseFloor = 0f
    }

    companion object {
        @SuppressLint("MissingPermission")
        fun create(sampleRate: Int, frames: Int, device: AudioDeviceInfo?): AmbientReferenceCapture? = runCatching {
            val min = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            check(min > 0 && device != null)
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(min, frames * 2 * 4)
            )
            check(recorder.state == AudioRecord.STATE_INITIALIZED)
            check(recorder.setPreferredDevice(device))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                recorder.setPreferredMicrophoneDirection(MicrophoneDirection.MIC_DIRECTION_UNSPECIFIED)
                recorder.setPreferredMicrophoneFieldDimension(0f)
            }
            AmbientReferenceCapture(recorder, frames)
        }.getOrNull()
    }
}

/** Lightweight, stateful microphone color that is safe to run in the capture loop. */
private class MicrophoneProcessor(private val sampleRate: Int) {
    private var lowPass = 0f
    private var subBass = 0f
    private var previousDry = 0f
    private var auditionEnvelope = 0f
    private var gateEnvelope = 0f
    private var detectorEnvelope = 0f
    private var robotPhase = 0.0
    private val pitchBuffer = FloatArray(4096)
    private var pitchWriteIndex = 0
    private var pitchPhase = 0.0
    var level: Float = 0f
        private set

    fun process(
        samples: ShortArray,
        count: Int,
        gain: Float,
        gateThreshold: Float,
        preset: MicrophoneVoicePreset,
        ambientNoiseLevel: Float = 0f
    ): ShortArray {
        val output = ShortArray(count)
        var energy = 0.0
        val gateScale = when (preset) {
            MicrophoneVoicePreset.SUPER_AUDITION -> .12f
            MicrophoneVoicePreset.SURFACE_MIC -> .42f
            else -> 1f
        }
        val ambientScale = when (preset) {
            MicrophoneVoicePreset.SUPER_AUDITION -> .10f
            MicrophoneVoicePreset.SURFACE_MIC -> .34f
            else -> .62f
        }
        val adaptiveGate = maxOf(gateThreshold * gateScale, ambientNoiseLevel * ambientScale)
        val threshold = adaptiveGate * Short.MAX_VALUE
        for (index in 0 until count) {
            val dry = samples[index].toFloat()
            energy += dry * dry

            detectorEnvelope = maxOf(abs(dry), detectorEnvelope * .9992f)
            val targetGate = if (detectorEnvelope >= threshold) 1f else 0f
            val gateSpeed = if (targetGate > gateEnvelope) .08f else .001f
            gateEnvelope += (targetGate - gateEnvelope) * gateSpeed

            lowPass += (dry - lowPass) * .12f
            subBass += (dry - subBass) * .018f
            val highPass = dry - lowPass
            val pitchFactor = when (preset) {
                MicrophoneVoicePreset.BABY -> 1.38f
                MicrophoneVoicePreset.ARENA_ANNOUNCER -> .72f
                MicrophoneVoicePreset.DEEP -> .82f
                else -> 1f
            }
            val shifted = pitchShift(dry, pitchFactor)
            val colored = when (preset) {
                MicrophoneVoicePreset.CLEAN -> dry
                MicrophoneVoicePreset.SURFACE_MIC -> {
                    // Contact-style voicing: discard slow room/handling rumble,
                    // emphasize the resonant body band, and preserve impact edges.
                    val bodyBand = lowPass - subBass
                    val transient = dry - previousDry
                    bodyBand * 1.85f + transient * .42f
                }
                MicrophoneVoicePreset.SUPER_AUDITION -> {
                    // Lift genuinely quiet detail without allowing loud events to
                    // explode. The high shelf restores articulation after compression.
                    auditionEnvelope = maxOf(abs(dry), auditionEnvelope * .9985f)
                    val detailGain = (13_000f / auditionEnvelope.coerceAtLeast(900f)).coerceIn(1f, 5.2f)
                    dry * detailGain + highPass * .58f
                }
                MicrophoneVoicePreset.RICH -> dry * .82f + lowPass * .28f + highPass * .10f
                MicrophoneVoicePreset.WARM -> dry * .72f + lowPass * .42f
                MicrophoneVoicePreset.DEEP -> shifted * .82f + lowPass * .38f
                MicrophoneVoicePreset.BRIGHT -> dry + highPass * .48f
                MicrophoneVoicePreset.RADIO -> highPass * 1.45f
                MicrophoneVoicePreset.ROBOT -> {
                    robotPhase += 2.0 * Math.PI * 46.0 / sampleRate
                    if (robotPhase > Math.PI * 2.0) robotPhase -= Math.PI * 2.0
                    dry * (0.35f + 0.65f * sin(robotPhase).toFloat())
                }
                MicrophoneVoicePreset.BABY -> shifted * .9f + highPass * .18f
                MicrophoneVoicePreset.ARENA_ANNOUNCER -> shifted * .86f + lowPass * .52f
            }
            val driven = when (preset) {
                MicrophoneVoicePreset.SURFACE_MIC -> tanh(colored / 14_000f) * 22_000f
                MicrophoneVoicePreset.SUPER_AUDITION -> tanh(colored / 16_000f) * 24_000f
                MicrophoneVoicePreset.RADIO -> tanh(colored / 9_000f) * 18_000f
                MicrophoneVoicePreset.ROBOT -> (colored / 900f).roundToInt() * 900f
                MicrophoneVoicePreset.RICH -> tanh(colored / 20_000f) * 22_000f
                MicrophoneVoicePreset.ARENA_ANNOUNCER -> tanh(colored / 12_000f) * 21_000f
                else -> colored
            }
            previousDry = dry
            output[index] = (driven * gain * gateEnvelope).roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        level = (sqrt(energy / count.coerceAtLeast(1)) / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)
        return output
    }

    private fun pitchShift(input: Float, factor: Float): Float {
        pitchBuffer[pitchWriteIndex] = input
        if (factor == 1f) {
            pitchWriteIndex = (pitchWriteIndex + 1) % pitchBuffer.size
            return input
        }
        val delayRange = 2_048f
        val minimumDelay = 192f
        val maximumDelay = minimumDelay + delayRange
        pitchPhase = (pitchPhase + abs(factor - 1f) / delayRange) % 1.0

        fun head(phase: Double): Float {
            val delay = if (factor > 1f) maximumDelay - phase.toFloat() * delayRange
            else minimumDelay + phase.toFloat() * delayRange
            var position = pitchWriteIndex - delay
            while (position < 0f) position += pitchBuffer.size
            val first = position.toInt() % pitchBuffer.size
            val next = (first + 1) % pitchBuffer.size
            val fraction = position - position.toInt()
            return pitchBuffer[first] * (1f - fraction) + pitchBuffer[next] * fraction
        }

        val secondPhase = (pitchPhase + .5) % 1.0
        val firstWeight = (.5 - .5 * cos(2.0 * Math.PI * pitchPhase)).toFloat()
        val secondWeight = (.5 - .5 * cos(2.0 * Math.PI * secondPhase)).toFloat()
        val result = head(pitchPhase) * firstWeight + head(secondPhase) * secondWeight
        pitchWriteIndex = (pitchWriteIndex + 1) % pitchBuffer.size
        return result
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
