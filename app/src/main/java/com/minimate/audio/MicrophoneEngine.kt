package com.minimate.audio

import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Microphone level control.
 *
 * Calibrated against measured capture from this hardware. On the phone's own array, speech
 * measures RMS in the hundreds against a room-tone floor an order of magnitude below it, and
 * every threshold here derives from that separation.
 *
 * Two failure modes were measured during development and both came from chasing loudness
 * directly: unconditional maximum gain, and aiming peaks above full scale. Each pinned output at
 * 100% of full scale on essentially every block, which is heard as noise and as a flat, lifeless
 * voice. Loudness comes from a stable RMS target with slow time constants, never from letting the
 * limiter run continuously.
 */
class MicrophoneEngine(private val sampleRate: Int) {
    private companion object {
        const val SPEECH_SNR = 2.0f
        const val SPEECH_ABSOLUTE_FLOOR = 8f
        const val SPEECH_HANGOVER_SECONDS = .60f
        /**
         * How far the level drops between phrases.
         *
         * It was .45, which is barely a duck at all: with the makeup a communications capture
         * needs, room tone at forty-five percent is plainly audible as a hum sitting under the
         * whole call. The speech hangover keeps this from closing inside a sentence.
         */
        const val NON_VOICE_LEVEL = .08f

        /**
         * Everything below this is rumble, not voice.
         *
         * Mains hum, desk vibration and handling noise all live under the bottom of the human
         * voice, and amplifying them along with it is most of what "ambient hum" is. Removing
         * them before the gain rather than after means the gain has less to amplify.
         */
        const val HIGH_PASS_HZ = 85f
        const val LIMIT_THRESHOLD = .88f
        const val NOISE_WINDOW_BLOCKS = 200

        /**
         * Makeup applied at nought on the dial.
         *
         * It used to be eleven, and the dial multiplied it by nought to three on top — so the
         * usable part of the control was the first fifth of its travel and everything past that
         * was distortion. The capture on this phone already reaches full scale unaided; what it
         * needs is a little makeup and a lot of headroom, not an order of magnitude.
         */
        /**
         * What nought on the dial means, for a capture that already arrives at a usable level.
         *
         * A quieter source is handed its own figure instead — see [process]. Measured on this
         * phone the two differ by around thirty decibels, which is far too much to leave for the
         * dial to make up: a dial that has to be held at its top is a dial with one setting.
         */
        const val UNITY_MAKEUP = 1.2f
    }

    // Two poles, not one. A single pole at this corner leaves fifty hertz at half its amplitude,
    // which is an attenuation nobody would describe as removing the hum; cascading a second
    // section takes it to a quarter and gives the filter the twelve decibels an octave that a
    // microphone channel normally has.
    private var highPassPreviousIn = 0f
    private var highPassPreviousOut = 0f
    private var highPassPreviousIn2 = 0f
    private var highPassPreviousOut2 = 0f
    private val highPassCoefficient = run {
        val rc = 1f / (2f * PI.toFloat() * HIGH_PASS_HZ)
        val dt = 1f / sampleRate
        rc / (rc + dt)
    }

    private fun highPass(sample: Float): Float {
        val first = highPassCoefficient * (highPassPreviousOut + sample - highPassPreviousIn)
        highPassPreviousIn = sample
        highPassPreviousOut = first
        val second = highPassCoefficient * (highPassPreviousOut2 + first - highPassPreviousIn2)
        highPassPreviousIn2 = first
        highPassPreviousOut2 = second
        return second
    }
    private var filtered = FloatArray(0)

    private val recentRms = FloatArray(NOISE_WINDOW_BLOCKS)
    private var recentIndex = 0
    private var recentCount = 0
    private var speechHoldSeconds = 0f
    private var currentGain = 1f
    private val ceiling = LIMIT_THRESHOLD * Short.MAX_VALUE
    private val headroom = Short.MAX_VALUE - ceiling



    var level: Float = 0f
        private set
    var lastStats: MicrophoneFrameStats? = null
        private set

    /**
     * @param gainDb what the dial says, in decibels, nought being a usable level. Decibels
     *   because the ear hears ratios: an even sweep of a linear multiplier is a sprint through the
     *   useful range followed by a long walk through distortion.
     * @param makeup what nought on the dial is worth for the capture in use. Measured on this
     *   phone a communications capture peaks at four percent of full scale where a recognition
     *   capture reaches all of it, so they cannot share a figure — with one, nought is already
     *   right; with the other the dial had to be held at maximum, which put the chain ninety-one
     *   times over and thirteen decibels into the limiter. That is heard as a tone that will not
     *   stop, and it is distortion rather than feedback.
     */
    fun process(
        samples: ShortArray,
        count: Int,
        gainDb: Float,
        placementGain: Float = 1f,
        makeup: Float = UNITY_MAKEUP
    ): ShortArray {
        val output = ShortArray(count)
        if (count <= 0) return output
        val dt = count.toFloat() / sampleRate

        if (filtered.size < count) filtered = FloatArray(count)

        var blockPeak = 0f
        var energy = 0.0
        for (index in 0 until count) {
            // High-passed before anything looks at it, so the level that decides speech from
            // silence is the level of the voice rather than the level of the room's rumble.
            val value = highPass(samples[index].toFloat())
            filtered[index] = value

            val magnitude = abs(value)
            if (magnitude > blockPeak) blockPeak = magnitude
            energy += value.toDouble() * value
        }
        val blockRms = sqrt(energy / count).toFloat()
        level = (blockRms / Short.MAX_VALUE).coerceIn(0f, 1f)

        recentRms[recentIndex] = blockRms
        recentIndex = (recentIndex + 1) % recentRms.size
        if (recentCount < recentRms.size) recentCount++
        var minimum = Float.MAX_VALUE
        for (index in 0 until recentCount) if (recentRms[index] < minimum) minimum = recentRms[index]
        val noiseRms = (minimum * 1.5f).coerceAtLeast(.5f)

        // Tool modes never gate. Their entire purpose is faint continuous sound, and a speech
        // detector would discard precisely what the listener is straining to hear.
        speechHoldSeconds = if (blockRms > noiseRms * SPEECH_SNR && blockRms > SPEECH_ABSOLUTE_FLOOR) {
            SPEECH_HANGOVER_SECONDS
        } else {
            (speechHoldSeconds - dt).coerceAtLeast(0f)
        }
        val voiceActive = speechHoldSeconds > 0f

        // Fixed gain, not automatic gain control.
        //
        // The last version anyone was happy with multiplied by the slider value and a gate
        // envelope, and nothing else. The automatic level control added later was measured
        // moving gain between 1.7x and 6.3x within a single second of continuous speech, and
        // still applying 3-6x with the slider at zero. Gain that moves inside a word is
        // amplitude modulation of the voice, which is heard as a hard, crispy edge, and no
        // amount of retuning the time constants removes it — a level control that reacts fast
        // enough to be useful is fast enough to be audible.
        //
        // So loudness is the user's decision and stays where they put it. The gate ducks between
        // phrases so room tone is not held at speaking level, and the limiter catches peaks.
        val base = makeup * placementGain * (10.0.pow(gainDb.coerceIn(-12f, 12f) / 20.0)).toFloat()
        val targetGain = if (voiceActive) base else base * NON_VOICE_LEVEL

        // Smoothed only enough to keep the gate from stepping; the gain itself does not hunt.
        val tau = if (targetGain < currentGain) .120f else .200f
        val previousGain = currentGain
        currentGain += (targetGain - currentGain) * (1f - exp(-dt / tau))

        var outputPeak = 0f
        for (index in 0 until count) {
            val driven = filtered[index]

            val ramped = previousGain + (currentGain - previousGain) * (index.toFloat() / count)
            val value = driven * ramped
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
