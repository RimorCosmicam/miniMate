package com.minimate

import com.minimate.audio.MicrophoneEngine
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * What the level control does to a signal, checked without a room or a headset.
 *
 * These are the three things that went wrong on the device in turn: a dial whose useful range was
 * a fifth of its travel, a limiter held open by too much makeup, and room tone amplified into a
 * hum between phrases.
 */
class MicrophoneEngineTest {

    private val rate = 48_000

    private fun tone(hz: Double, amplitude: Double, seconds: Double = 0.02): ShortArray {
        val count = (rate * seconds).toInt()
        return ShortArray(count) { i ->
            (sin(2 * PI * hz * i / rate) * amplitude * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun rms(samples: ShortArray): Double {
        if (samples.isEmpty()) return 0.0
        var energy = 0.0
        samples.forEach { energy += it.toDouble() * it }
        return sqrt(energy / samples.size)
    }

    /** Settle the engine on a signal before measuring, since the gain is deliberately smoothed. */
    private fun settle(engine: MicrophoneEngine, input: ShortArray, gainDb: Float, blocks: Int = 60): ShortArray {
        var last = ShortArray(0)
        repeat(blocks) { last = engine.process(input, input.size, gainDb) }
        return last
    }

    @Test
    fun theDialCoversItsRangeInEvenSteps() {
        val speech = tone(220.0, 0.05)
        val quiet = rms(settle(MicrophoneEngine(rate), speech, -12f))
        val middle = rms(settle(MicrophoneEngine(rate), speech, 0f))
        val loud = rms(settle(MicrophoneEngine(rate), speech, 12f))

        // Each quarter-turn is the same ratio, which is the entire point of decibels: the old
        // linear dial spent its first fifth on everything useful and the rest on distortion.
        val lowerStep = middle / quiet
        val upperStep = loud / middle
        assertTrue("steps were $lowerStep and $upperStep", lowerStep > 2.5 && lowerStep < 5.5)
        assertTrue("steps were $lowerStep and $upperStep", upperStep > 2.5 && upperStep < 5.5)
    }

    /** A voice at a sensible level and a sensible setting must not sit against the ceiling. */
    @Test
    fun ordinarySpeechDoesNotRunTheLimiter() {
        val speech = tone(220.0, 0.08)
        val out = settle(MicrophoneEngine(rate), speech, 0f)
        val peak = out.maxOf { kotlin.math.abs(it.toInt()) }
        assertTrue("peaked at $peak of ${Short.MAX_VALUE}", peak < Short.MAX_VALUE * 0.95)
    }

    /** Rumble below the voice is removed before the gain rather than amplified by it. */
    @Test
    fun humBelowTheVoiceIsRemoved() {
        val hum = tone(50.0, 0.05)
        val voice = tone(400.0, 0.05)
        val humOut = rms(settle(MicrophoneEngine(rate), hum, 0f))
        val voiceOut = rms(settle(MicrophoneEngine(rate), voice, 0f))
        assertTrue("hum $humOut vs voice $voiceOut", humOut < voiceOut * 0.5)
    }

    /** Between phrases the level drops far enough that room tone is not held at speaking level. */
    @Test
    fun theGateDucksFarEnoughToSilenceRoomTone() {
        val engine = MicrophoneEngine(rate)
        val speech = tone(220.0, 0.08)
        settle(engine, speech, 0f)
        val speaking = rms(engine.process(speech, speech.size, 0f))

        val roomTone = tone(300.0, 0.0015)
        var quiet = ShortArray(0)
        repeat(200) { quiet = engine.process(roomTone, roomTone.size, 0f) }
        assertTrue("room tone came back at ${rms(quiet)} against speech at $speaking",
            rms(quiet) < speaking * 0.05)
    }
}
