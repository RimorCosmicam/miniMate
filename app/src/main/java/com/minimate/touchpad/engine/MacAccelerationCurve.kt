package com.minimate.touchpad.engine

import kotlin.math.hypot
import kotlin.math.pow

/**
 * Calibrated trackpad acceleration curve model.
 * Provides high-precision sub-pixel control for micro-movements and smooth, controllable
 * acceleration for larger swipes without erratic jumps or overshoots.
 */
class MacAccelerationCurve {

    private var subpixelX = 0f
    private var subpixelY = 0f

    data class Output(val dx: Int, val dy: Int)

    /**
     * Applies smooth dynamic acceleration curve to pointer movement.
     *
     * @param rawDx Raw X delta in screen pixels
     * @param rawDy Raw Y delta in screen pixels
     * @param dtMs Time delta in milliseconds
     * @param trackingSpeed Base sensitivity multiplier (0.4x - 2.5x, calibrated default 1.0x)
     * @param acceleration Acceleration exponent factor
     */
    fun process(
        rawDx: Float,
        rawDy: Float,
        dtMs: Float,
        trackingSpeed: Float = 1.0f,
        acceleration: Float = 1.15f
    ): Output {
        if (rawDx == 0f && rawDy == 0f) return Output(0, 0)

        val safeDt = dtMs.coerceIn(4f, 33f)
        val distance = hypot(rawDx, rawDy)
        // Velocity in dp-equivalent units per millisecond
        val velocity = (distance / safeDt) * 16.67f // normalized per-frame speed (60fps baseline)

        // Calibrated baseline multiplier
        // 1.0x speed maps ~1 screen pixel to ~0.75 desktop count for pixel-perfect precision
        val baseScale = 0.75f * trackingSpeed

        // Smooth Sigmoid/Power Acceleration Gain
        val gain = if (acceleration > 0.01f && velocity > 1.2f) {
            val speedFactor = ((velocity - 1.2f) / 12f).coerceIn(0f, 3.5f)
            1.0f + (speedFactor.pow(acceleration.coerceIn(0.5f, 1.8f)) * 0.65f)
        } else {
            1.0f
        }

        val totalMultiplier = baseScale * gain

        val targetDx = rawDx * totalMultiplier + subpixelX
        val targetDy = rawDy * totalMultiplier + subpixelY

        val intDx = targetDx.toInt()
        val intDy = targetDy.toInt()

        subpixelX = targetDx - intDx
        subpixelY = targetDy - intDy

        return Output(intDx, intDy)
    }

    fun reset() {
        subpixelX = 0f
        subpixelY = 0f
    }
}
