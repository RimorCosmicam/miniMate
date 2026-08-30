package com.minimate.audio

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Microphone level control.
 *
 * No frequency filtering of any kind: the signal is passed through untouched apart from gain.
 * Noise reduction is the platform NoiseSuppressor effect on the capture session, not anything
 * reimplemented here.
 *
 * Calibrated against measured capture from the actual hardware rather than assumed levels. With
 * the AudioSource the device probe selects, speech blocks measure RMS 150-290 with peaks reaching
 * 12000-plus, while the gaps between words sit at RMS 5-30. That order-of-magnitude separation is
 * what every threshold below is derived from, and it is the separation that did not exist while
 * capture ran on a source delivering 25x less signal — which is why earlier attempts at level
 * control either gated speech off entirely or amplified silence.
 *
 * The immediately preceding version applied maximum gain unconditionally. Measurement showed the
 * consequence plainly: output peak pinned at 100% of full scale on every single block, with gain
 * swinging between 9x and 637x as it chased room tone during pauses. Permanent limiter saturation
 * is heard as noise and as a flat, lifeless voice, which is why "louder" made it worse.
 */
class MicrophoneEngine(private val sampleRate: Int) {
    private companion object {
        /** Where speech should land, ≈ -22 dBFS RMS. */
        const val TARGET_RMS = 2_600f
        const val MAX_TARGET_RMS = 7_000f
        /** Amplified peaks aim here. Leaves the limiter idle during normal speech. */
        const val PEAK_CEILING = .82f
        /** The estimated noise floor may never be amplified past ≈ -38 dBFS. */
        const val MAX_NOISE_RMS_OUT = 420f
        const val MAX_GAIN = 250f
        /** Speech must exceed the noise floor by this factor. Measured separation is ~10x. */
        const val SPEECH_SNR = 2.5f
        /** Rejects digital silence only; far below any usable capsule. */
        const val SPEECH_ABSOLUTE_FLOOR = 4f
        const val SPEECH_HANGOVER_SECONDS = .40f
        const val LIMIT_THRESHOLD = .88f
        const val NOISE_WINDOW_BLOCKS = 200
    }

    private val recentRms = FloatArray(NOISE_WINDOW_BLOCKS)
    private var recentIndex = 0
    private var recentCount = 0
    private var smoothedRms = 0f
    private var peakEnvelope = 0f
    private var speechHoldSeconds = 0f
    private var currentGain = 1f
    private val ceiling = LIMIT_THRESHOLD * Short.MAX_VALUE
    private val headroom = Short.MAX_VALUE - ceiling

    var level: Float = 0f
        private set
    var lastStats: MicrophoneFrameStats? = null
        private set

    fun process(samples: ShortArray, count: Int, trim: Float): ShortArray {
        val output = ShortArray(count)
        if (count <= 0) return output
        val dt = count.toFloat() / sampleRate

        var blockPeak = 0f
        var energy = 0.0
        for (index in 0 until count) {
            val sample = samples[index].toFloat()
            val magnitude = abs(sample)
            if (magnitude > blockPeak) blockPeak = magnitude
            energy += sample.toDouble() * sample
        }
        val blockRms = sqrt(energy / count).toFloat()
        level = (blockRms / Short.MAX_VALUE).coerceIn(0f, 1f)

        // Noise floor by minimum statistics: the quietest block across several seconds is room
        // tone, because speech is intermittent. The running minimum sits below the true noise
        // RMS by construction, hence the bias correction.
        recentRms[recentIndex] = blockRms
        recentIndex = (recentIndex + 1) % recentRms.size
        if (recentCount < recentRms.size) recentCount++
        var minimum = Float.MAX_VALUE
        for (index in 0 until recentCount) if (recentRms[index] < minimum) minimum = recentRms[index]
        val noiseRms = (minimum * 1.5f).coerceAtLeast(.5f)

        val envelopeTau = if (blockRms > smoothedRms) .012f else .160f
        smoothedRms += (blockRms - smoothedRms) * (1f - exp(-dt / envelopeTau))
        peakEnvelope = maxOf(blockPeak, peakEnvelope * exp(-dt / .6f))

        speechHoldSeconds = if (blockRms > noiseRms * SPEECH_SNR && blockRms > SPEECH_ABSOLUTE_FLOOR) {
            SPEECH_HANGOVER_SECONDS
        } else {
            (speechHoldSeconds - dt).coerceAtLeast(0f)
        }
        val voiceActive = speechHoldSeconds > 0f

        // Four independent limits, smallest wins. Holding gain at unity while no voice is present
        // is what stops room tone being lifted to full scale during pauses.
        val wanted = if (voiceActive) {
            minOf(TARGET_RMS * trim.coerceIn(.25f, 3f), MAX_TARGET_RMS) / smoothedRms.coerceAtLeast(1f)
        } else {
            1f
        }
        val peakLimit = (PEAK_CEILING * Short.MAX_VALUE / peakEnvelope.coerceAtLeast(1f)).coerceAtLeast(1f)
        val noiseLimit = (MAX_NOISE_RMS_OUT / noiseRms).coerceAtLeast(1f)
        val targetGain = wanted.coerceIn(1f, minOf(MAX_GAIN, peakLimit, noiseLimit))

        val tau = if (targetGain < currentGain) .025f else .250f
        val previousGain = currentGain
        currentGain += (targetGain - currentGain) * (1f - exp(-dt / tau))

        var outputPeak = 0f
        for (index in 0 until count) {
            val ramped = previousGain + (currentGain - previousGain) * (index.toFloat() / count)
            val value = samples[index].toFloat() * ramped
            val magnitude = abs(value)
            val limited = if (magnitude <= ceiling) {
                value
            } else {
                sign(value) * (ceiling + headroom * tanh((magnitude - ceiling) / headroom))
            }
            val limitedMagnitude = abs(limited)
            if (limitedMagnitude > outputPeak) outputPeak = limitedMagnitude
            output[index] = limited.roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        lastStats = MicrophoneFrameStats(
            rawPeak = blockPeak,
            rawRms = blockRms,
            noiseRms = noiseRms,
            voiceActive = voiceActive,
            gain = currentGain,
            outputPeak = outputPeak
        )
        return output
    }
}

/** Diagnostics for one processed block, so behaviour stays observable instead of inferred. */
data class MicrophoneFrameStats(
    val rawPeak: Float,
    val rawRms: Float,
    val noiseRms: Float,
    val voiceActive: Boolean,
    val gain: Float,
    val outputPeak: Float
)
