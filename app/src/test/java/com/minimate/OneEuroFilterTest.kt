package com.minimate

import com.minimate.touchpad.engine.OneEuroFilter
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class OneEuroFilterTest {

    @Test
    fun testFilterSmoothsSlowMicroVibrations() {
        val filter = OneEuroFilter(minCutoff = 1.0, beta = 0.0)
        var t = 0.0
        val value = 100.0

        filter.filter(value, t)

        // Inject high-frequency noise
        val outputs = mutableListOf<Double>()
        for (i in 1..20) {
            t += 16.66
            val noise = if (i % 2 == 0) 2.0 else -2.0
            val filtered = filter.filter(value + noise, t)
            outputs.add(filtered)
        }

        // Filtered signal deviation should be substantially smaller than raw noise
        val maxDeviation = outputs.maxOf { abs(it - value) }
        assertTrue("Expected smoothed max deviation < 1.5, got $maxDeviation", maxDeviation < 1.5)
    }

    @Test
    fun testFilterAdaptsQuicklyToRapidMovement() {
        val filter = OneEuroFilter(minCutoff = 1.0, beta = 0.5)
        var t = 0.0
        filter.filter(0.0, t)

        // Fast jump
        t += 16.66
        filter.filter(500.0, t)
        t += 16.66
        val rapid2 = filter.filter(1000.0, t)

        // Should track fast movement closely
        assertTrue("Rapid response should follow fast velocity", rapid2.compareTo(800.0) > 0)
    }
}
