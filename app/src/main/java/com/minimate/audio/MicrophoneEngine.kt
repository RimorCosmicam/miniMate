package com.minimate.audio

import com.minimate.touchpad.model.MicrophoneVoicePreset
import com.minimate.touchpad.model.SUPERHUMAN_BAND_HZ
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/** Transposed direct-form II biquad. One sample of state, stable coefficients. */
internal class Biquad(
    private val b0: Float, private val b1: Float, private val b2: Float,
    private val a1: Float, private val a2: Float
) {
    private var z1 = 0f
    private var z2 = 0f

    fun process(input: Float): Float {
        val output = b0 * input + z1
        z1 = b1 * input - a1 * output + z2
        z2 = b2 * input - a2 * output
        return output
    }

    companion object {
        /** Peaking EQ (RBJ cookbook): boosts or cuts around a centre frequency. */
        fun peaking(sampleRate: Int, centreHz: Float, gainDb: Float, q: Float = 1.0f): Biquad {
            val a = 10.0.pow(gainDb / 40.0)
            val w0 = 2.0 * PI * centreHz.coerceIn(20f, sampleRate / 2.2f) / sampleRate
            val alpha = sin(w0) / (2.0 * q)
            val a0 = 1.0 + alpha / a
            return Biquad(
                b0 = ((1.0 + alpha * a) / a0).toFloat(),
                b1 = ((-2.0 * cos(w0)) / a0).toFloat(),
                b2 = ((1.0 - alpha * a) / a0).toFloat(),
                a1 = ((-2.0 * cos(w0)) / a0).toFloat(),
                a2 = ((1.0 - alpha / a) / a0).toFloat()
            )
        }
    }
}

/**
 * Time-domain pitch shifter: two crossfaded delay taps swept in opposite directions. Cheap enough
 * for the capture loop, and adequate for a voice effect rather than transparent transposition.
 */
internal class PitchShifter {
    private val buffer = FloatArray(4096)
    private var writeIndex = 0
    private var phase = 0.0

    fun process(input: Float, factor: Float): Float {
        buffer[writeIndex] = input
        if (factor == 1f) {
            writeIndex = (writeIndex + 1) % buffer.size
            return input
        }
        val range = 2_048f
        val minimumDelay = 192f
        val maximumDelay = minimumDelay + range
        phase = (phase + abs(factor - 1f) / range) % 1.0

        fun tap(at: Double): Float {
            val delay = if (factor > 1f) maximumDelay - at.toFloat() * range
            else minimumDelay + at.toFloat() * range
            var position = writeIndex - delay
            while (position < 0f) position += buffer.size
            val first = position.toInt() % buffer.size
            val next = (first + 1) % buffer.size
            val fraction = position - position.toInt()
            return buffer[first] * (1f - fraction) + buffer[next] * fraction
        }

        val second = (phase + .5) % 1.0
        val firstWeight = (.5 - .5 * cos(2.0 * PI * phase)).toFloat()
        val secondWeight = (.5 - .5 * cos(2.0 * PI * second)).toFloat()
        val result = tap(phase) * firstWeight + tap(second) * secondWeight
        writeIndex = (writeIndex + 1) % buffer.size
        return result
    }
}

/**
 * Microphone level control and character.
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
        const val TARGET_RMS = 3_600f
        const val MAX_TARGET_RMS = 6_500f
        const val PEAK_CEILING = .90f
        const val MAX_NOISE_RMS_OUT = 550f
        const val MAX_GAIN = 400f
        const val SPEECH_SNR = 2.0f
        const val SPEECH_ABSOLUTE_FLOOR = 8f
        const val SPEECH_HANGOVER_SECONDS = .60f
        const val NON_VOICE_LEVEL = .45f
        const val LIMIT_THRESHOLD = .88f
        const val NOISE_WINDOW_BLOCKS = 200

        /**
         * Tool modes aim only slightly hotter than speech, and their gain ceiling is modest.
         * Earlier values of 7000 / 900x were set on the reasoning that faint sound needs
         * enormous amplification; in practice that drives room tone to full scale the instant
         * nothing else is happening, which is heard as being blasted with noise rather than as
         * sensitivity. Amplifying a noise floor never reveals anything underneath it.
         */
        const val TOOL_TARGET_RMS = 2_600f
        const val TOOL_MAX_GAIN = 60f
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

    // Character state
    private var lowPass = 0f
    private var subBass = 0f
    private var previousDry = 0f
    private var robotPhase = 0.0
    private val pitchShifter = PitchShifter()

    // Super Human band shaper, rebuilt only when the requested shape changes.
    private var bandFilters: List<Biquad> = emptyList()
    private var bandShape: List<Float> = emptyList()

    var level: Float = 0f
        private set
    var lastStats: MicrophoneFrameStats? = null
        private set

    private fun ensureBands(shape: List<Float>) {
        if (shape == bandShape && bandFilters.isNotEmpty()) return
        bandShape = shape
        bandFilters = SUPERHUMAN_BAND_HZ.mapIndexed { index, hz ->
            Biquad.peaking(sampleRate, hz, shape.getOrElse(index) { 0f }.coerceIn(-18f, 18f))
        }
    }

    fun process(
        samples: ShortArray,
        count: Int,
        trim: Float,
        preset: MicrophoneVoicePreset = MicrophoneVoicePreset.CLEAN,
        superhumanBands: List<Float> = emptyList()
    ): ShortArray {
        val output = ShortArray(count)
        if (count <= 0) return output
        val dt = count.toFloat() / sampleRate
        val isTool = preset == MicrophoneVoicePreset.STETHO || preset == MicrophoneVoicePreset.SUPERHUMAN
        if (preset == MicrophoneVoicePreset.SUPERHUMAN) ensureBands(superhumanBands)

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

        recentRms[recentIndex] = blockRms
        recentIndex = (recentIndex + 1) % recentRms.size
        if (recentCount < recentRms.size) recentCount++
        var minimum = Float.MAX_VALUE
        for (index in 0 until recentCount) if (recentRms[index] < minimum) minimum = recentRms[index]
        val noiseRms = (minimum * 1.5f).coerceAtLeast(.5f)

        val envelopeTau = if (blockRms > smoothedRms) .080f else .400f
        smoothedRms += (blockRms - smoothedRms) * (1f - exp(-dt / envelopeTau))
        peakEnvelope = if (blockPeak > peakEnvelope) {
            peakEnvelope + (blockPeak - peakEnvelope) * (1f - exp(-dt / .05f))
        } else {
            peakEnvelope * exp(-dt / .5f)
        }

        // Tool modes never gate. Their entire purpose is faint continuous sound, and a speech
        // detector would discard precisely what the listener is straining to hear.
        speechHoldSeconds = if (blockRms > noiseRms * SPEECH_SNR && blockRms > SPEECH_ABSOLUTE_FLOOR) {
            SPEECH_HANGOVER_SECONDS
        } else {
            (speechHoldSeconds - dt).coerceAtLeast(0f)
        }
        val voiceActive = isTool || speechHoldSeconds > 0f

        val targetGain = if (voiceActive) {
            val aim = if (isTool) TOOL_TARGET_RMS else minOf(TARGET_RMS * trim.coerceIn(.25f, 3f), MAX_TARGET_RMS)
            val wanted = aim * (if (isTool) trim.coerceIn(.25f, 3f) else 1f) / smoothedRms.coerceAtLeast(1f)
            val peakLimit = (PEAK_CEILING * Short.MAX_VALUE / peakEnvelope.coerceAtLeast(1f)).coerceAtLeast(1f)
            val noiseLimit = if (isTool) Float.MAX_VALUE else (MAX_NOISE_RMS_OUT / noiseRms).coerceAtLeast(1f)
            wanted.coerceIn(1f, minOf(if (isTool) TOOL_MAX_GAIN else MAX_GAIN, peakLimit, noiseLimit))
        } else {
            NON_VOICE_LEVEL
        }

        val tau = if (targetGain < currentGain) .150f else .350f
        val previousGain = currentGain
        currentGain += (targetGain - currentGain) * (1f - exp(-dt / tau))

        val pitchFactor = when (preset) {
            MicrophoneVoicePreset.BABY -> 1.38f
            MicrophoneVoicePreset.ARENA_ANNOUNCER -> .72f
            MicrophoneVoicePreset.DEEP -> .82f
            else -> 1f
        }

        var outputPeak = 0f
        for (index in 0 until count) {
            val dry = samples[index].toFloat()

            lowPass += (dry - lowPass) * .12f
            subBass += (dry - subBass) * .018f
            val highPass = dry - lowPass
            val shifted = pitchShifter.process(dry, pitchFactor)

            val coloured = when (preset) {
                MicrophoneVoicePreset.CLEAN -> dry
                MicrophoneVoicePreset.RICH -> dry * .82f + lowPass * .28f + highPass * .10f
                MicrophoneVoicePreset.WARM -> dry * .72f + lowPass * .42f
                MicrophoneVoicePreset.BRIGHT -> dry + highPass * .48f
                MicrophoneVoicePreset.DEEP -> shifted * .82f + lowPass * .38f
                MicrophoneVoicePreset.RADIO -> highPass * 1.45f
                MicrophoneVoicePreset.ROBOT -> {
                    robotPhase += 2.0 * PI * 46.0 / sampleRate
                    if (robotPhase > PI * 2.0) robotPhase -= PI * 2.0
                    dry * (.35f + .65f * sin(robotPhase).toFloat())
                }
                MicrophoneVoicePreset.BABY -> shifted * .9f + highPass * .18f
                MicrophoneVoicePreset.ARENA_ANNOUNCER -> shifted * .86f + lowPass * .52f
                MicrophoneVoicePreset.STETHO -> {
                    // Contact listening. Structure-borne sound is carried in the low-mid band,
                    // so slow rumble below it is removed and the resonant body emphasised, while
                    // transient edges are preserved so taps stay crisp. The airborne signal is
                    // largely rejected — a contact microphone that still hears the room is just
                    // an amplifier, which is what the first attempt at this was.
                    val body = lowPass - subBass
                    val transient = dry - previousDry
                    (body * 1.2f + transient * .25f) - highPass * .35f
                }
                MicrophoneVoicePreset.SUPERHUMAN -> {
                    var shaped = dry
                    for (filter in bandFilters) shaped = filter.process(shaped)
                    shaped
                }
            }

            val driven = when (preset) {
                // Tools stay well below the limiter. Saturating them turns every surface tap
                // into a wall of distortion and loses the detail the tool exists to reveal.
                MicrophoneVoicePreset.STETHO -> tanh(coloured / 20_000f) * 15_000f
                MicrophoneVoicePreset.SUPERHUMAN -> tanh(coloured / 22_000f) * 16_000f
                MicrophoneVoicePreset.RADIO -> tanh(coloured / 9_000f) * 18_000f
                MicrophoneVoicePreset.ROBOT -> (coloured / 900f).roundToInt() * 900f
                MicrophoneVoicePreset.RICH -> tanh(coloured / 20_000f) * 22_000f
                MicrophoneVoicePreset.ARENA_ANNOUNCER -> tanh(coloured / 12_000f) * 21_000f
                else -> coloured
            }
            previousDry = dry

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
