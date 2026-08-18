package com.minimate.touchpad.engine

import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sign

/**
 * macOS Magic Trackpad acceleration curve model.
 * Provides high precision at slow speeds and natural power acceleration at higher speeds.
 */
class MacAccelerationCurve {

    private var subpixelX = 0f
    private var subpixelY = 0f

    data class Output(val dx: Int, val dy: Int)

    /**
     * Applies macOS-style dynamic acceleration to raw delta movement.
     *
     * @param rawDx Raw X delta in pixels
     * @param rawDy Raw Y delta in pixels
     * @param dtMs Time delta in milliseconds between samples
     * @param trackingSpeed Base sensitivity multiplier (e.g. 1.0f)
     * @param accelerationExponent Acceleration power factor (e.g. 1.15f)
     */
    fun process(
        rawDx: Float,
        rawDy: Float,
        dtMs: Float,
        trackingSpeed: Float = 1.0f,
        accelerationExponent: Float = 1.15f
    ): Output {
        if (rawDx == 0f && rawDy == 0f) return Output(0, 0)

        val safeDt = if (dtMs <= 0.001f) 8.33f else dtMs // default to ~120Hz frame if zero
        val distance = hypot(rawDx, rawDy)
        val velocity = (distance / safeDt) * 1000f // pixels per second

        // Dynamic macOS Gain Curve
        // v0 = threshold speed (~250 px/s)
        val v0 = 250f
        val normVelocity = (velocity / v0).coerceIn(0f, 15f)

        // Non-linear gain multiplier
        val gain = if (accelerationExponent > 0f) {
            1.0f + (trackingSpeed * (normVelocity.pow(accelerationExponent) * 0.45f))
        } else {
            1.0f
        }

        val totalScale = trackingSpeed * gain

        // Compute accelerated delta with fractional carry-over
        val targetDx = rawDx * totalScale + subpixelX
        val targetDy = rawDy * totalScale + subpixelY

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
