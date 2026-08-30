package com.minimate.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Microphone processing engine.
 *
 * Rebuilt as an explicit chain of single-purpose stages rather than one inline loop, because the
 * previous design measured and corrected level on the raw signal and had no frequency shaping at
 * all. That ordering is the root problem: a cable-mounted capsule picks up mains hum, cable
 * handling and body rumble, all well below the voice band. Those dominate both the RMS and the
 * peak of the raw block, so every downstream decision was made about the rumble rather than the
 * speech — the gain solver saw a "loud enough" signal, the peak ceiling clamped gain to protect
 * headroom that low-frequency noise was consuming, and the noise-floor estimate sat high. The
 * audible result is a quiet, distant voice buried under a hum, which is exactly what was
 * reported.
 *
 * Signal order here is deliberate: filter first, then measure the filtered signal, then decide.
 *
 *   DC blocker -> voice-band high-pass -> level metering -> noise floor -> voice activity
 *   -> gain solver -> gain smoothing -> soft limiter
 *
 * Every stage is independently constructed and unit-testable, and every threshold is stated in
 * dBFS with the reasoning attached, so tuning is an informed change rather than a guess.
 */

/** Transposed direct-form II biquad. Stable, low coefficient sensitivity, one sample of state. */
internal class Biquad(
    private val b0: Float,
    private val b1: Float,
    private val b2: Float,
    private val a1: Float,
    private val a2: Float
) {
    private var z1 = 0f
    private var z2 = 0f

    fun reset() { z1 = 0f; z2 = 0f }

    fun process(input: Float): Float {
        val output = b0 * input + z1
        z1 = b1 * input - a1 * output + z2
        z2 = b2 * input - a2 * output
        return output
    }

    companion object {
        /** Butterworth-response high-pass (RBJ cookbook), Q = 0.707. */
        fun highPass(sampleRate: Int, cutoffHz: Float, q: Float = .707f): Biquad {
            val w0 = 2.0 * PI * cutoffHz / sampleRate
            val cosW0 = cos(w0)
            val alpha = sin(w0) / (2.0 * q)
            val a0 = 1.0 + alpha
            return Biquad(
                b0 = (((1.0 + cosW0) / 2.0) / a0).toFloat(),
                b1 = ((-(1.0 + cosW0)) / a0).toFloat(),
                b2 = (((1.0 + cosW0) / 2.0) / a0).toFloat(),
                a1 = ((-2.0 * cosW0) / a0).toFloat(),
                a2 = ((1.0 - alpha) / a0).toFloat()
            )
        }

        /** Gentle top-end roll-off to tame hiss above the speech band. */
        fun lowPass(sampleRate: Int, cutoffHz: Float, q: Float = .707f): Biquad {
            val safeCutoff = cutoffHz.coerceAtMost(sampleRate / 2.2f)
            val w0 = 2.0 * PI * safeCutoff / sampleRate
            val cosW0 = cos(w0)
            val alpha = sin(w0) / (2.0 * q)
            val a0 = 1.0 + alpha
            return Biquad(
                b0 = (((1.0 - cosW0) / 2.0) / a0).toFloat(),
                b1 = (((1.0 - cosW0)) / a0).toFloat(),
                b2 = (((1.0 - cosW0) / 2.0) / a0).toFloat(),
                a1 = ((-2.0 * cosW0) / a0).toFloat(),
                a2 = ((1.0 - alpha) / a0).toFloat()
            )
        }
    }
}

/** Removes any DC bias, which otherwise silently consumes headroom the limiter then has to eat. */
internal class DcBlocker(sampleRate: Int) {
    private val pole = exp(-2.0 * PI * 15.0 / sampleRate).toFloat()
    private var lastInput = 0f
    private var lastOutput = 0f

    fun process(input: Float): Float {
        val output = input - lastInput + pole * lastOutput
        lastInput = input
        lastOutput = output
        return output
    }
}

/**
 * Noise floor by minimum statistics: speech is intermittent, so the quietest block across a
 * multi-second window is room tone rather than voice. A running minimum sits below the true
 * noise RMS by construction, hence the bias correction.
 */
internal class NoiseFloorEstimator(private val windowBlocks: Int = 200) {
    private val history = FloatArray(windowBlocks)
    private var index = 0
    private var filled = 0

    fun update(blockRms: Float): Float {
        history[index] = blockRms
        index = (index + 1) % history.size
        if (filled < history.size) filled++
        var minimum = Float.MAX_VALUE
        for (i in 0 until filled) if (history[i] < minimum) minimum = history[i]
        return (minimum * 1.5f).coerceAtLeast(.5f)
    }
}

/**
 * Voice activity by signal-to-noise ratio, with hangover so gain does not collapse in the gaps
 * between words. The absolute floor exists only to reject digital silence and must stay far
 * below any usable capsule — a previous fixed floor of RMS 25 sat above a low-output inline
 * mic's entire speech range, so it was classified as silence and never received any gain.
 */
internal class VoiceActivityDetector(
    private val snrThreshold: Float = 2.5f,
    private val absoluteFloor: Float = 4f,
    private val hangoverSeconds: Float = .35f
) {
    private var holdSeconds = 0f
    var active: Boolean = false
        private set

    fun update(blockRms: Float, noiseRms: Float, dt: Float): Boolean {
        holdSeconds = if (blockRms > noiseRms * snrThreshold && blockRms > absoluteFloor) {
            hangoverSeconds
        } else {
            (holdSeconds - dt).coerceAtLeast(0f)
        }
        active = holdSeconds > 0f
        return active
    }
}

/**
 * Gain solver. Four independent constraints, the smallest wins:
 *
 *  - RMS target      : where speech should sit, ≈ -23 dBFS.
 *  - Peak headroom   : speech has a 12-18 dB crest factor, so an RMS-only target drives every
 *                      consonant into the limiter and the constant limiting is heard as
 *                      distortion. Measured at .82 of full scale; raising it to .95 audibly
 *                      degraded every microphone and was reverted.
 *  - Noise ceiling   : the estimated noise floor may never be amplified past ≈ -35 dBFS, which
 *                      is what stops a quiet room being boosted into loud hiss.
 *  - Absolute cap    : guards against a divide-by-tiny only.
 */
internal class GainSolver(
    private val targetRms: Float = 2_200f,
    private val maxTargetRms: Float = 6_000f,
    private val peakCeiling: Float = .82f,
    private val maxNoiseRmsOut: Float = 550f,
    private val maxGain: Float = 200f
) {
    private var smoothedRms = 0f
    private var peakEnvelope = 0f
    var gain: Float = 1f
        private set

    fun solve(blockRms: Float, blockPeak: Float, noiseRms: Float, voiceActive: Boolean, trim: Float, dt: Float): Float {
        val envelopeTau = if (blockRms > smoothedRms) .010f else .150f
        smoothedRms += (blockRms - smoothedRms) * (1f - exp(-dt / envelopeTau))
        // Instant attack, ~0.6 s decay: fast enough to protect against a transient, slow enough
        // that one knock does not audibly duck the following words.
        peakEnvelope = maxOf(blockPeak, peakEnvelope * exp(-dt / .6f))

        val wanted = if (voiceActive) {
            minOf(targetRms * trim.coerceIn(.25f, 3f), maxTargetRms) / smoothedRms.coerceAtLeast(1f)
        } else {
            1f
        }
        val peakLimit = (peakCeiling * Short.MAX_VALUE / peakEnvelope.coerceAtLeast(1f)).coerceAtLeast(1f)
        val noiseLimit = (maxNoiseRmsOut / noiseRms.coerceAtLeast(.5f)).coerceAtLeast(1f)
        val target = wanted.coerceIn(1f, minOf(maxGain, peakLimit, noiseLimit))

        // Fall fast, rise slowly: onsets never clip, and level does not surge between words.
        val tau = if (target < gain) .030f else .400f
        gain += (target - gain) * (1f - exp(-dt / tau))
        return gain
    }
}

/** Soft limiter that is bit-transparent below threshold, so normal speech is never coloured. */
internal class SoftLimiter(private val threshold: Float = .86f) {
    private val ceiling = threshold * Short.MAX_VALUE
    private val headroom = Short.MAX_VALUE - ceiling

    fun process(value: Float): Float {
        val magnitude = abs(value)
        if (magnitude <= ceiling) return value
        return sign(value) * (ceiling + headroom * tanh((magnitude - ceiling) / headroom))
    }
}

/** Diagnostics for one processed block, so behaviour is observable instead of inferred. */
data class MicrophoneFrameStats(
    val rawPeak: Float,
    val filteredPeak: Float,
    val filteredRms: Float,
    val noiseRms: Float,
    val voiceActive: Boolean,
    val gain: Float,
    val outputPeak: Float
)

/**
 * The full chain. One instance per capture session; not thread safe by design, since it is
 * driven exclusively from the capture loop.
 */
class MicrophoneEngine(private val sampleRate: Int) {
    private val dcBlocker = DcBlocker(sampleRate)
    // 85 Hz: below the male fundamental (~85-180 Hz) so voice is untouched, above mains hum and
    // the cable/handling rumble that dominates a low-output inline capsule.
    private val voiceHighPass = Biquad.highPass(sampleRate, 85f)
    // Second pole for a steeper skirt; a single 12 dB/oct section leaves too much 50-60 Hz.
    private val voiceHighPass2 = Biquad.highPass(sampleRate, 85f)
    private val hissLowPass = Biquad.lowPass(sampleRate, 7_800f)
    private val noiseFloor = NoiseFloorEstimator()
    private val vad = VoiceActivityDetector()
    private val gainSolver = GainSolver()
    private val limiter = SoftLimiter()

    private val scratch = FloatArray(4096)

    var level: Float = 0f
        private set
    var lastStats: MicrophoneFrameStats? = null
        private set

    fun process(samples: ShortArray, count: Int, trim: Float): ShortArray {
        val output = ShortArray(count)
        if (count <= 0) return output
        val dt = count.toFloat() / sampleRate
        val work = if (count <= scratch.size) scratch else FloatArray(count)

        // Stage 1: filter. Everything downstream measures and decides on the filtered signal,
        // so out-of-band energy can no longer distort the level estimate or steal headroom.
        var rawPeak = 0f
        var filteredPeak = 0f
        var energy = 0.0
        for (index in 0 until count) {
            val raw = samples[index].toFloat()
            val rawMagnitude = abs(raw)
            if (rawMagnitude > rawPeak) rawPeak = rawMagnitude

            var value = dcBlocker.process(raw)
            value = voiceHighPass.process(value)
            value = voiceHighPass2.process(value)
            value = hissLowPass.process(value)

            work[index] = value
            val magnitude = abs(value)
            if (magnitude > filteredPeak) filteredPeak = magnitude
            energy += value.toDouble() * value
        }
        val filteredRms = sqrt(energy / count).toFloat()
        level = (filteredRms / Short.MAX_VALUE).coerceIn(0f, 1f)

        // Stage 2: decide.
        val noiseRms = noiseFloor.update(filteredRms)
        val voiceActive = vad.update(filteredRms, noiseRms, dt)
        val previousGain = gainSolver.gain
        val gain = gainSolver.solve(filteredRms, filteredPeak, noiseRms, voiceActive, trim, dt)

        // Stage 3: apply. Gain is interpolated across the block so it never steps between
        // packets, which would otherwise be audible as zipper noise at every boundary.
        var outputPeak = 0f
        for (index in 0 until count) {
            val ramped = previousGain + (gain - previousGain) * (index.toFloat() / count)
            val limited = limiter.process(work[index] * ramped)
            val magnitude = abs(limited)
            if (magnitude > outputPeak) outputPeak = magnitude
            output[index] = limited.roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        lastStats = MicrophoneFrameStats(
            rawPeak = rawPeak,
            filteredPeak = filteredPeak,
            filteredRms = filteredRms,
            noiseRms = noiseRms,
            voiceActive = voiceActive,
            gain = gain,
            outputPeak = outputPeak
        )
        return output
    }
}
