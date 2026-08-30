package com.minimate.audio

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Microphone gain stage: push the signal to the loudest it can go, always.
 *
 * No filters, no voice-activity gating, no noise ceiling. Every earlier version decided, on its
 * own, when the microphone "deserved" gain — and on a low-output inline capsule those decisions
 * were consistently wrong, leaving the gain at 1.0 while the wearer heard nothing. There is no
 * such decision here. Every block is normalised toward full scale on the strength of its own
 * peak, so a quiet capsule is driven just as hard as a loud one.
 *
 * Noise suppression is the platform NoiseSuppressor effect applied at the capture session, ahead
 * of this stage, rather than anything reimplemented here.
 *
 * The only restraint is the soft limiter: gain aims peaks near full scale, and the limiter
 * absorbs whatever overshoots. Without it the same signal would wrap around the 16-bit range and
 * arrive as hard clipping, which is louder in no useful sense.
 */
class MicrophoneEngine(private val sampleRate: Int) {
    private companion object {
        /** Aim peaks here, as a fraction of full scale. 1.0 is hard clipping by definition. */
        const val PEAK_TARGET = .97f
        /** Ceiling on the multiplier itself, purely to bound a divide-by-near-zero. */
        const val MAX_GAIN = 400f
        /** Peak envelope decay. Short, so level recovers quickly after a loud transient. */
        const val PEAK_DECAY_SECONDS = .35f
        /** Soft-limit only above this; below it the signal passes through untouched. */
        const val LIMIT_THRESHOLD = .90f
    }

    private var peakEnvelope = 0f
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

        // Instant attack so a sudden loud sound cannot clip; quick decay so the level climbs
        // straight back to maximum afterwards instead of ducking for the next second.
        peakEnvelope = maxOf(blockPeak, peakEnvelope * exp(-dt / PEAK_DECAY_SECONDS))

        // Unconditional: whatever the block's peak is, drive it to the target. Nothing here can
        // decide the signal is "not speech" and withhold gain.
        val targetGain = (PEAK_TARGET * Short.MAX_VALUE / peakEnvelope.coerceAtLeast(1f))
            .coerceIn(1f, MAX_GAIN) * trim.coerceIn(.25f, 3f)

        // Fall fast, rise briskly. The rise is far quicker than a conventional AGC because the
        // complaint was never that level surged, it was that level never arrived.
        val tau = if (targetGain < currentGain) .020f else .120f
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
    val gain: Float,
    val outputPeak: Float
)
