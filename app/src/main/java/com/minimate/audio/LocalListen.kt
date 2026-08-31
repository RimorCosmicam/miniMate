package com.minimate.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.MicrophoneDirection
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.core.content.ContextCompat
import com.minimate.touchpad.model.MicrophonePlacement
import com.minimate.touchpad.model.MicrophoneVoicePreset
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * On-device listening: the phone's microphone, processed, straight back out of the phone's own
 * output — normally the USB-C earphones. The desktop companion is not involved at any point, so
 * this works with nothing connected and adds no network latency.
 *
 * This is what the Stetho and Super Human tools require. They are listening instruments, not
 * transmission modes: the value is hearing the result yourself, immediately, while moving the
 * phone around. Routing that through a Mac would add a wireless round trip and make the tool
 * useless for its purpose.
 *
 * Latency is the design constraint throughout. Buffers are the smallest the device will grant and
 * the track runs in low-latency mode, because anything beyond roughly 40 ms makes it hard to
 * associate what is heard with where the phone is pointed.
 */
class LocalListen(private val context: Context) {
    private companion object {
        const val TAG = "MiniMateListen"
        const val SAMPLE_RATE = 48_000
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val running = AtomicBoolean(false)
    private var worker: Thread? = null

    @Volatile var preset: MicrophoneVoicePreset = MicrophoneVoicePreset.SUPERHUMAN
    @Volatile var bands: List<Float> = emptyList()
    @Volatile var placement: MicrophonePlacement = MicrophonePlacement.HANDHELD
    @Volatile var placementAuto: Boolean = true
    private val placementDetector = PlacementDetector(context)

    private fun activePlacement(): MicrophonePlacement =
        if (placementAuto && placementDetector.available) placementDetector.current else placement
    @Volatile var gain: Float = 1f
    /** Playback level, 0..1. Starts low deliberately — see the feedback note in start(). */
    @Volatile var listenVolume: Float = .30f
    /** Output device key to play into, or null for whatever the system is using. */
    @Volatile var outputDeviceKey: String? = null
    @Volatile var lastError: String? = null
        private set

    val isRunning: Boolean get() = running.get()

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (running.get()) return true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            lastError = "Microphone permission is required"
            return false
        }
        running.set(true)
        placementDetector.start()
        worker = thread(name = "MiniMate-local-listen", isDaemon = true) { run() }
        return true
    }

    fun stop() {
        placementDetector.stop()
        running.set(false)
        runCatching { worker?.join(300) }
        worker = null
    }

    private fun outputDevice(): AudioDeviceInfo? {
        val key = outputDeviceKey ?: return null
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull {
            "${it.type}:${it.address.ifBlank { it.productName?.toString().orEmpty() }}" == key
        }
    }

    private fun builtInMic(): AudioDeviceInfo? =
        audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }

    @SuppressLint("MissingPermission")
    private fun run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        var recorder: AudioRecord? = null
        var track: AudioTrack? = null
        var suppressor: NoiseSuppressor? = null
        var canceller: AcousticEchoCanceler? = null
        try {
            val recordMinimum = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val frames = (SAMPLE_RATE / 100).coerceAtLeast(recordMinimum / 2 / 2) // ~10 ms
            val builtIn = builtInMic()
            fun open(source: Int): AudioRecord? {
                val candidate = AudioRecord(
                    source, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, maxOf(recordMinimum, frames * 2 * 4)
                )
                if (candidate.state != AudioRecord.STATE_INITIALIZED) {
                    candidate.release()
                    return null
                }
                candidate.setPreferredDevice(builtIn)
                return candidate
            }

            // Tool modes want the microphone's own sensitivity rather than a speech-tuned
            // profile that suppresses the faint continuous sound being listened for. But the
            // request that matters more is which capsule: with earphones plugged in, the
            // platform will hand over their inline microphone unless the built-in array is
            // pinned, and UNPROCESSED does not always honour that pin. So the routing is
            // verified after opening, and a source that ignored it is discarded — an
            // instrument listening through a cable-mounted capsule is not the instrument.
            val unprocessed = if (isToolMode()) open(MediaRecorder.AudioSource.UNPROCESSED) else null
            recorder = if (unprocessed != null && builtIn != null &&
                unprocessed.routedDevice?.type != AudioDeviceInfo.TYPE_BUILTIN_MIC
            ) {
                Log.i(TAG, "UNPROCESSED routed to ${unprocessed.routedDevice?.type}, not the built-in array; falling back")
                unprocessed.release()
                null
            } else {
                unprocessed
            }
            if (recorder == null) recorder = open(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            if (recorder == null) {
                lastError = "Microphone unavailable"
                return
            }
            val mic: AudioRecord = recorder
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isToolMode()) {
                runCatching {
                    mic.setPreferredMicrophoneDirection(MicrophoneDirection.MIC_DIRECTION_TOWARDS_USER)
                    mic.setPreferredMicrophoneFieldDimension(activePlacement().fieldDimension)
                }
            }
            // Suppression and echo cancellation are deliberately off for the tools: both remove
            // steady low-level sound, which is the entire signal of interest here.
            if (!isToolMode()) {
                if (NoiseSuppressor.isAvailable()) {
                    suppressor = runCatching {
                        NoiseSuppressor.create(mic.audioSessionId)?.apply { enabled = true }
                    }.getOrNull()
                }
                if (AcousticEchoCanceler.isAvailable()) {
                    canceller = runCatching {
                        AcousticEchoCanceler.create(mic.audioSessionId)?.apply { enabled = true }
                    }.getOrNull()
                }
            }

            val trackMinimum = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(trackMinimum, frames * 2 * 2))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
            outputDevice()?.let { track.setPreferredDevice(it) }

            val engine = MicrophoneEngine(SAMPLE_RATE)
            val samples = ShortArray(frames)
            // Feedback guard. The earphones are centimetres from the microphone driving them, so
            // any loop gain above unity builds into a howl within a fraction of a second. This
            // watches for the signature — output sustained near full scale across consecutive
            // blocks, which speech and incidental sound do not produce — and ducks hard until it
            // clears. Without it, enabling monitoring at any useful level is a blast of noise.
            var hotBlocks = 0
            var duck = 1f
            mic.startRecording()
            track.play()
            Log.i(TAG, "capture routed to ${mic.routedDevice?.type}/${mic.routedDevice?.productName} (builtin=${AudioDeviceInfo.TYPE_BUILTIN_MIC})")
            Log.i(TAG, "local listen started: preset=$preset frames=$frames out=${track.routedDevice?.productName}")

            var emptyReads = 0
            while (running.get()) {
                val read = mic.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                if (read <= 0) {
                    if (++emptyReads > 200) { lastError = "Microphone stopped producing audio"; break }
                    continue
                }
                emptyReads = 0
                val processed = engine.process(samples, read, gain, preset, bands, activePlacement().gainScale)

                var peak = 0
                for (index in 0 until read) {
                    val magnitude = kotlin.math.abs(processed[index].toInt())
                    if (magnitude > peak) peak = magnitude
                }
                if (peak > 30_000) hotBlocks++ else hotBlocks = 0
                if (hotBlocks >= 12) {
                    duck = (duck * .55f).coerceAtLeast(.05f)
                    hotBlocks = 0
                    Log.i(TAG, "feedback guard engaged, duck=$duck")
                } else if (peak < 12_000) {
                    duck = (duck * 1.02f).coerceAtMost(1f)
                }

                val level = listenVolume.coerceIn(0f, 1f) * duck
                for (index in 0 until read) {
                    processed[index] = (processed[index] * level).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                track.write(processed, 0, read, AudioTrack.WRITE_BLOCKING)
            }
        } catch (error: Exception) {
            Log.w(TAG, "local listen failed", error)
            lastError = error.message ?: "Listening failed"
        } finally {
            runCatching { recorder?.stop() }
            suppressor?.let { runCatching { it.release() } }
            canceller?.let { runCatching { it.release() } }
            recorder?.release()
            runCatching { track?.stop() }
            track?.release()
            running.set(false)
            Log.i(TAG, "local listen stopped")
        }
    }

    private fun isToolMode(): Boolean =
        preset == MicrophoneVoicePreset.STETHO || preset == MicrophoneVoicePreset.SUPERHUMAN
}
