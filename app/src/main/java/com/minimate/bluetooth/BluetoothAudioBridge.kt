package com.minimate.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
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
import com.minimate.touchpad.model.MicrophoneVoicePreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

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
    val microphoneGain: Float = 1f,
    val microphoneNoiseGate: Float = .015f,
    val microphonePreset: MicrophoneVoicePreset = MicrophoneVoicePreset.CLEAN,
    val microphoneLevel: Float = 0f,
    val error: String? = null,
    val receivedPackets: Long = 0,
    val sentPackets: Long = 0
)

@SuppressLint("MissingPermission")
class BluetoothAudioBridge(private val context: Context, private val adapter: BluetoothAdapter?) {
    companion object {
        private const val TAG = "MiniMateAudio"
        const val WIFI_PORT = 42308
        const val NSD_TYPE = "_minimate-audio._tcp."
        const val WIFI_SAMPLE_RATE = 48_000
        const val WIFI_FRAMES_PER_PACKET = 960
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
    private var audioTrack: AudioTrack? = null
    private var audioTrackRate = 0
    private var audioTrackEncoding = 0
    private var nsdRegistration: NsdManager.RegistrationListener? = null
    private val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = updateNetworkAvailability()
        override fun onLost(network: Network) = updateNetworkAvailability()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = updateNetworkAvailability()
    }

    fun configure(
        outputEnabled: Boolean,
        microphoneEnabled: Boolean,
        outputVolume: Float,
        microphoneGain: Float,
        microphoneNoiseGate: Float,
        microphonePreset: MicrophoneVoicePreset
    ) {
        _state.update {
            it.copy(
                outputEnabled = outputEnabled,
                microphoneEnabled = microphoneEnabled,
                outputVolume = outputVolume.coerceIn(0f, 1f),
                microphoneGain = microphoneGain.coerceIn(0f, 2f),
                microphoneNoiseGate = microphoneNoiseGate.coerceIn(0f, .15f),
                microphonePreset = microphonePreset,
                error = null
            )
        }
        audioTrack?.setVolume(outputVolume.coerceIn(0f, 1f))
        if (microphoneEnabled && activeLink != null) startMicrophone() else stopMicrophone()
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

    private fun startMicrophone(explicitLink: Link? = null) {
        if (microphoneJob?.isActive == true) return
        val link = explicitLink ?: activeLink ?: return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _state.update { it.copy(error = "Microphone permission is required") }
            return
        }
        microphoneJob = scope.launch {
            val sampleRate = if (link.transport == AudioTransport.WIFI) WIFI_SAMPLE_RATE else AudioBridgeProtocol.SAMPLE_RATE
            val frames = if (link.transport == AudioTransport.WIFI) WIFI_FRAMES_PER_PACKET else AudioBridgeProtocol.FRAMES_PER_PACKET
            val min = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, maxOf(min, frames * 2 * 4)
            )
            val samples = ShortArray(frames)
            val processor = MicrophoneProcessor(sampleRate)
            try {
                recorder.startRecording()
                while (running && link === activeLink && link.isOpen() && _state.value.microphoneEnabled) {
                    val count = recorder.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                    if (count <= 0) continue
                    val current = _state.value
                    val adjusted = processor.process(
                        samples = samples,
                        count = count,
                        gain = current.microphoneGain,
                        gateThreshold = current.microphoneNoiseGate,
                        preset = current.microphonePreset
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
                runCatching { recorder.stop() }
                recorder.release()
            }
        }
    }

    private fun writeFrame(output: DataOutputStream, frame: AudioBridgeProtocol.Frame) {
        synchronized(writeLock) { AudioBridgeProtocol.write(output, frame) }
    }

    private fun stopMicrophone() {
        microphoneJob?.cancel()
        microphoneJob = null
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
        unregisterNsd()
        runCatching { bluetoothServer?.close() }
        runCatching { wifiServer?.close() }
        disconnectActiveLink()
        releaseTrack()
        scope.cancel()
    }
}

/** Lightweight, stateful microphone color that is safe to run in the capture loop. */
private class MicrophoneProcessor(private val sampleRate: Int) {
    private var lowPass = 0f
    private var gateEnvelope = 0f
    private var detectorEnvelope = 0f
    private var robotPhase = 0.0
    var level: Float = 0f
        private set

    fun process(
        samples: ShortArray,
        count: Int,
        gain: Float,
        gateThreshold: Float,
        preset: MicrophoneVoicePreset
    ): ShortArray {
        val output = ShortArray(count)
        var energy = 0.0
        val threshold = gateThreshold * Short.MAX_VALUE
        for (index in 0 until count) {
            val dry = samples[index].toFloat()
            energy += dry * dry

            detectorEnvelope = maxOf(abs(dry), detectorEnvelope * .9992f)
            val targetGate = if (detectorEnvelope >= threshold) 1f else 0f
            val gateSpeed = if (targetGate > gateEnvelope) .08f else .001f
            gateEnvelope += (targetGate - gateEnvelope) * gateSpeed

            lowPass += (dry - lowPass) * .12f
            val highPass = dry - lowPass
            val colored = when (preset) {
                MicrophoneVoicePreset.CLEAN -> dry
                MicrophoneVoicePreset.WARM -> dry * .72f + lowPass * .42f
                MicrophoneVoicePreset.DEEP -> lowPass * 1.18f + dry * .28f
                MicrophoneVoicePreset.BRIGHT -> dry + highPass * .48f
                MicrophoneVoicePreset.RADIO -> highPass * 1.45f
                MicrophoneVoicePreset.ROBOT -> {
                    robotPhase += 2.0 * Math.PI * 46.0 / sampleRate
                    if (robotPhase > Math.PI * 2.0) robotPhase -= Math.PI * 2.0
                    dry * (0.35f + 0.65f * sin(robotPhase).toFloat())
                }
            }
            val driven = when (preset) {
                MicrophoneVoicePreset.RADIO -> tanh(colored / 9_000f) * 18_000f
                MicrophoneVoicePreset.ROBOT -> (colored / 900f).roundToInt() * 900f
                else -> colored
            }
            output[index] = (driven * gain * gateEnvelope).roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        level = (sqrt(energy / count.coerceAtLeast(1)) / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)
        return output
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
