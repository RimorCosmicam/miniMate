package com.minimate

import com.minimate.touchpad.engine.MacAccelerationCurve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MacAccelerationCurveTest {

    @Test
    fun `test zero movement returns zero`() {
        val curve = MacAccelerationCurve()
        val out = curve.process(0f, 0f, 16.6f)
        assertEquals(0, out.dx)
        assertEquals(0, out.dy)
    }

    @Test
    fun `test acceleration increases nonlinearly with velocity`() {
        val curve = MacAccelerationCurve()

        // Slow movement (e.g. 2px in 16ms = 120px/s)
        val slow = curve.process(2f, 0f, 16.6f, trackingSpeed = 1.0f, accelerationExponent = 1.2f)

        curve.reset()

        // Fast movement (e.g. 20px in 16ms = 1200px/s) - 10x raw delta
        val fast = curve.process(20f, 0f, 16.6f, trackingSpeed = 1.0f, accelerationExponent = 1.2f)

        // Fast output should be significantly more than 10x slow output due to power curve
        assertTrue(
            "Fast delta (${fast.dx}) should scale non-linearly over slow delta (${slow.dx})",
            fast.dx > (slow.dx * 10)
        )
    }

    @Test
    fun `test subpixel carryover preserves fractional motion`() {
        val curve = MacAccelerationCurve()

        // Move 0.4 pixels twice
        val out1 = curve.process(0.4f, 0f, 16.6f, trackingSpeed = 1.0f, accelerationExponent = 0f)
        assertEquals(0, out1.dx) // 0.4 accumulates

        val out2 = curve.process(0.4f, 0f, 16.6f, trackingSpeed = 1.0f, accelerationExponent = 0f)
        assertEquals(0, out2.dx) // 0.8 accumulates

        val out3 = curve.process(0.4f, 0f, 16.6f, trackingSpeed = 1.0f, accelerationExponent = 0f)
        assertEquals(1, out3.dx) // 1.2 emits 1 and retains 0.2
    }
}
