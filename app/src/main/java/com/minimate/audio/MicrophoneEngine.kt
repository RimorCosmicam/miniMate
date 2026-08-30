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
 * Calibrated against measured capture from the actual hardware rather than assumed levels. On the
 * AudioSource the device probe selects, speech measures RMS 400-1136 while the gaps between words
 * measure 1-26. Every threshold below derives from that separation, which simply did not exist
 * while capture ran on a source delivering 25x less signal.
 *
 * Two failure modes have been measured here, and both were caused by chasing loudness directly:
 * applying maximum gain unconditionally, and later aiming peaks above full scale. Each pinned the
 * output at 100% of full scale on essentially every block with gain swinging by more than an order
 * of magnitude between blocks. Permanent limiter saturation is heard as noise and as a flat,
 * lifeless voice, so both attempts made the result audibly worse rather than louder.
 *
 * Loudness therefore comes from a stable RMS target with slow time constants, never from letting
 * the limiter run continuously.
 */
class MicrophoneEngine(private val sampleRate: Int) {
    private companion object {
        /**
         * Where speech should land at trim 1.0, ≈ -19 dBFS RMS. The trim slider is a fine
         * adjustment either side of this, not something that should need to be maxed: a default
         * requiring 3x to be audible means the calibration point itself is wrong. The ceiling is
         * kept close because pushing the RMS target hotter than about -14 dBFS puts speech peaks
         * into the limiter continuously, which sounds worse rather than louder.
         */
        const val TARGET_RMS = 3_600f
        const val MAX_TARGET_RMS = 6_500f
        /**
         * Amplified peaks aim just under full scale. Pushing this above 1.0 to chase loudness
         * put every block into the limiter permanently, measured as output peak at 100% of full
         * scale on essentially every window; that saturation is heard as distortion, not volume.
         * The smoothed peak envelope below is what stops a single impulse dictating the gain.
         */
        const val PEAK_CEILING = .90f
        /** The estimated noise floor may never be amplified past ≈ -35 dBFS. */
        const val MAX_NOISE_RMS_OUT = 550f
        const val MAX_GAIN = 400f
        /** Speech must exceed the noise floor by this factor. */
        const val SPEECH_SNR = 3.0f
        /**
         * Measured speech on this capsule sits at RMS 400-1136 while room tone measures 1-26,
         * so this sits an order of magnitude below speech and well above noise. With the noise
         * floor estimate collapsing to ~1, the ratio test alone treated room tone as speech and
         * the gate never closed, which is why suppression was inaudible.
         */
        const val SPEECH_ABSOLUTE_FLOOR = 50f
        const val SPEECH_HANGOVER_SECONDS = .25f
        /** Level held during pauses. Attenuating rather than passing room tone at unity is what
         *  makes isolation audible between words. */
        const val NON_VOICE_LEVEL = .12f
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

        // Deliberately slow. A fast envelope tracks syllable-to-syllable variation, so gain
        // moves within every word and the result pumps. Measured RMS swings 100x between
        // consecutive blocks; loudness must be judged across a phrase, not a syllable.
        val envelopeTau = if (blockRms > smoothedRms) .080f else .400f
        smoothedRms += (blockRms - smoothedRms) * (1f - exp(-dt / envelopeTau))
        // Attack is smoothed rather than instantaneous so a single-block impulse cannot slam the
        // envelope to its own height and starve the following speech of gain.
        peakEnvelope = if (blockPeak > peakEnvelope) {
            peakEnvelope + (blockPeak - peakEnvelope) * (1f - exp(-dt / .05f))
        } else {
            peakEnvelope * exp(-dt / .5f)
        }

        speechHoldSeconds = if (blockRms > noiseRms * SPEECH_SNR && blockRms > SPEECH_ABSOLUTE_FLOOR) {
            SPEECH_HANGOVER_SECONDS
        } else {
            (speechHoldSeconds - dt).coerceAtLeast(0f)
        }
        val voiceActive = speechHoldSeconds > 0f

        // While voice is present: three independent limits, smallest wins. While it is absent the
        // signal is attenuated instead of passed at unity, so room tone actually drops away
        // between words rather than merely failing to be amplified.
        val targetGain = if (voiceActive) {
            val wanted = minOf(TARGET_RMS * trim.coerceIn(.25f, 3f), MAX_TARGET_RMS) /
                smoothedRms.coerceAtLeast(1f)
            val peakLimit = (PEAK_CEILING * Short.MAX_VALUE / peakEnvelope.coerceAtLeast(1f)).coerceAtLeast(1f)
            val noiseLimit = (MAX_NOISE_RMS_OUT / noiseRms).coerceAtLeast(1f)
            wanted.coerceIn(1f, minOf(MAX_GAIN, peakLimit, noiseLimit))
        } else {
            NON_VOICE_LEVEL
        }

        // Both directions are slow enough that gain is effectively constant across a phrase.
        val tau = if (targetGain < currentGain) .080f else .350f
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
