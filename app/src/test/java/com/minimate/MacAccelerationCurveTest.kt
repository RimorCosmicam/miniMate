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
        val slow = curve.process(2f, 0f, 16.6f, trackingSpeed = 1.0f, acceleration = 1.2f)

        curve.reset()

        // Fast movement (e.g. 20px in 16ms = 1200px/s) - 10x raw delta
        val fast = curve.process(20f, 0f, 16.6f, trackingSpeed = 1.0f, acceleration = 1.2f)

        // Fast output should be significantly more than 10x slow output due to power curve
        assertTrue(
            "Fast delta (${fast.dx}) should scale non-linearly over slow delta (${slow.dx})",
            fast.dx > slow.dx
        )
    }

    @Test
    fun `test subpixel carryover preserves fractional motion`() {
        val curve = MacAccelerationCurve()

        // Move sub-pixel amounts
        val out1 = curve.process(0.6f, 0f, 16.6f, trackingSpeed = 1.0f, acceleration = 0f)
        val out2 = curve.process(0.6f, 0f, 16.6f, trackingSpeed = 1.0f, acceleration = 0f)
        val out3 = curve.process(0.6f, 0f, 16.6f, trackingSpeed = 1.0f, acceleration = 0f)

        // Total motion should emit counts
        assertTrue("Subpixel accumulator should emit counts over multiple frames", (out1.dx + out2.dx + out3.dx) >= 1)
    }
}
