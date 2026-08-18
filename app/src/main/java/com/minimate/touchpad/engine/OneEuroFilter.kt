package com.minimate.touchpad.engine

import kotlin.math.PI

/**
 * 1€ Filter (Casiez et al., CHI 2012) for low-latency, jitter-free pointer tracking.
 * Dynamically adjusts filtering cutoff based on instantaneous velocity:
 * - At low speed: Low cutoff removes sensor jitter and micro-finger tremor.
 * - At high speed: High cutoff eliminates phase lag, keeping cursor tracking 1:1.
 */
class OneEuroFilter(
    private var minCutoff: Double = 1.2,
    private var beta: Double = 0.05,
    private var dCutoff: Double = 1.0
) {
    private var xPrev = 0.0
    private var dxPrev = 0.0
    private var tPrev = -1.0

    private fun alpha(rate: Double, cutoff: Double): Double {
        val tau = 1.0 / (2.0 * PI * cutoff)
        val te = 1.0 / rate
        return 1.0 / (1.0 + tau / te)
    }

    fun filter(value: Double, timestampMs: Double): Double {
        if (tPrev < 0) {
            xPrev = value
            dxPrev = 0.0
            tPrev = timestampMs
            return value
        }

        val dt = (timestampMs - tPrev) / 1000.0
        tPrev = timestampMs

        if (dt <= 0.0) return xPrev

        val rate = 1.0 / dt

        // Estimate derivative (velocity)
        val dx = (value - xPrev) * rate
        val aD = alpha(rate, dCutoff)
        val dxHat = aD * dx + (1.0 - aD) * dxPrev
        dxPrev = dxHat

        // Dynamic cutoff
        val cutoff = minCutoff + beta * kotlin.math.abs(dxHat)
        val a = alpha(rate, cutoff)
        val xHat = a * value + (1.0 - a) * xPrev
        xPrev = xHat

        return xHat
    }

    fun reset() {
        tPrev = -1.0
        xPrev = 0.0
        dxPrev = 0.0
    }
}
