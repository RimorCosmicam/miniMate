package com.minimate.touchpad.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

/**
 * Physics-based kinetic momentum scrolling scroller.
 * Emulates the fluid inertial deceleration of macOS Magic Trackpad scrolling.
 */
class PhysicsMomentumScroller(
    private val scope: CoroutineScope,
    private val onScrollStep: (vScroll: Int, hScroll: Int) -> Unit
) {
    private var scrollJob: Job? = null
    private var subpixelV = 0f
    private var subpixelH = 0f

    fun startFling(
        initialVelocityY: Float,
        initialVelocityX: Float,
        friction: Float = 0.92f,
        scrollSpeed: Float = 1.0f,
        isNatural: Boolean = true
    ) {
        stop()
        if (abs(initialVelocityY) < 50f && abs(initialVelocityX) < 50f) return

        scrollJob = scope.launch {
            var vx = initialVelocityX * scrollSpeed
            var vy = initialVelocityY * scrollSpeed
            val minVelocity = 15f
            val frameIntervalMs = 12L // ~83 Hz smooth dispatch

            val sign = if (isNatural) -1 else 1

            while (isActive && (abs(vx) > minVelocity || abs(vy) > minVelocity)) {
                val dt = frameIntervalMs / 1000f

                // Convert velocity (pixels/sec) to scroll steps
                val stepY = (vy * dt * 0.04f * sign) + subpixelV
                val stepX = (vx * dt * 0.04f * sign) + subpixelH

                val intY = stepY.toInt()
                val intX = stepX.toInt()

                subpixelV = stepY - intY
                subpixelH = stepX - intX

                if (intY != 0 || intX != 0) {
                    onScrollStep(intY, intX)
                }

                // Decay velocity with friction
                val decayFactor = friction.pow(dt * 60f)
                vx *= decayFactor
                vy *= decayFactor

                delay(frameIntervalMs)
            }
            subpixelV = 0f
            subpixelH = 0f
        }
    }

    fun stop() {
        scrollJob?.cancel()
        scrollJob = null
        subpixelV = 0f
        subpixelH = 0f
    }
}
