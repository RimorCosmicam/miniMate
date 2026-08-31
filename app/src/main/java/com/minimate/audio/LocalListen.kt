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
    @Volatile var gain: Float = 1f
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
        worker = thread(name = "MiniMate-local-listen", isDaemon = true) { run() }
        return true
    }

    fun stop() {
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
            recorder = AudioRecord(
                // Tool modes want the microphone's own sensitivity, not a speech-tuned profile
                // that suppresses exactly the faint continuous sound being listened for.
                if (isToolMode()) MediaRecorder.AudioSource.UNPROCESSED
                else MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                maxOf(recordMinimum, frames * 2 * 4)
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                lastError = "Microphone unavailable"
                return
            }
            recorder.setPreferredDevice(builtInMic())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isToolMode()) {
                runCatching {
                    recorder.setPreferredMicrophoneDirection(MicrophoneDirection.MIC_DIRECTION_TOWARDS_USER)
                    recorder.setPreferredMicrophoneFieldDimension(.75f)
                }
            }
            // Suppression and echo cancellation are deliberately off for the tools: both remove
            // steady low-level sound, which is the entire signal of interest here.
            if (!isToolMode()) {
                if (NoiseSuppressor.isAvailable()) {
                    suppressor = runCatching {
                        NoiseSuppressor.create(recorder.audioSessionId)?.apply { enabled = true }
                    }.getOrNull()
                }
                if (AcousticEchoCanceler.isAvailable()) {
                    canceller = runCatching {
                        AcousticEchoCanceler.create(recorder.audioSessionId)?.apply { enabled = true }
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
            recorder.startRecording()
            track.play()
            Log.i(TAG, "local listen started: preset=$preset frames=$frames out=${track.routedDevice?.productName}")

            var emptyReads = 0
            while (running.get()) {
                val read = recorder.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                if (read <= 0) {
                    if (++emptyReads > 200) { lastError = "Microphone stopped producing audio"; break }
                    continue
                }
                emptyReads = 0
                val processed = engine.process(samples, read, gain, preset, bands)
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
