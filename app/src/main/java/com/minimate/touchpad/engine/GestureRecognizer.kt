package com.minimate.touchpad.engine

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import com.minimate.bluetooth.HidDescriptor
import com.minimate.touchpad.model.GestureEvent
import com.minimate.touchpad.model.TouchpadSettings
import kotlin.math.abs
import kotlin.math.hypot

data class TouchPoint(
    val x: Float,
    val y: Float,
    val id: Int,
    val pressure: Float = 1f
)

/**
 * Advanced gesture recognizer modeled after macOS Magic Trackpad gestures.
 */
class GestureRecognizer(
    private val context: Context,
    private val onGesture: (GestureEvent) -> Unit
) {
    private val viewConfig = ViewConfiguration.get(context)
    private val touchSlop = viewConfig.scaledTouchSlop.toFloat()
    private val tapTimeout = 220L
    private val doubleTapTimeout = 300L

    private val filterDx = OneEuroFilter(minCutoff = 1.5, beta = 0.08)
    private val filterDy = OneEuroFilter(minCutoff = 1.5, beta = 0.08)
    private val accelerationCurve = MacAccelerationCurve()

    private var velocityTracker: VelocityTracker? = null

    // 1-finger tracking state
    private var downTime1 = 0L
    private var downX1 = 0f
    private var downY1 = 0f
    private var lastX1 = 0f
    private var lastY1 = 0f
    private var lastTime1 = 0L
    private var totalMovedDistance1 = 0f
    private var isDragging = false
    private var lastTapUpTime = 0L

    // 2-finger tracking state
    private var downTime2 = 0L
    private var downAvgX = 0f
    private var downAvgY = 0f
    private var lastAvgX = 0f
    private var lastAvgY = 0f
    private var lastTime2 = 0L
    private var totalMovedDistance2 = 0f
    private var initialPinchSpan = 0f
    private var isTwoFingerGestureActive = false

    var settings: TouchpadSettings = TouchpadSettings()
    var screenWidth = 0f
    var screenHeight = 0f

    // Live active touch points for visual finger effects
    val activeTouchPoints = mutableListOf<TouchPoint>()
    var onTouchPointsUpdated: ((List<TouchPoint>) -> Unit)? = null

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)

        updateActiveTouchPoints(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleActionDown(event)
            MotionEvent.ACTION_POINTER_DOWN -> handlePointerDown(event)
            MotionEvent.ACTION_MOVE -> handleActionMove(event)
            MotionEvent.ACTION_POINTER_UP -> handlePointerUp(event)
            MotionEvent.ACTION_UP -> handleActionUp(event)
            MotionEvent.ACTION_CANCEL -> handleActionCancel()
        }
        return true
    }

    private fun updateActiveTouchPoints(event: MotionEvent) {
        activeTouchPoints.clear()
        if (event.actionMasked != MotionEvent.ACTION_UP && event.actionMasked != MotionEvent.ACTION_CANCEL) {
            for (i in 0 until event.pointerCount) {
                activeTouchPoints.add(
                    TouchPoint(
                        x = event.getX(i),
                        y = event.getY(i),
                        id = event.getPointerId(i),
                        pressure = event.getPressure(i)
                    )
                )
            }
        }
        onTouchPointsUpdated?.invoke(activeTouchPoints.toList())
    }

    private fun isEdgeTouch(x: Float, y: Float): Boolean {
        val marginPx = settings.edgeMarginDp * context.resources.displayMetrics.density
        return x < marginPx || x > (screenWidth - marginPx) || y < marginPx || y > (screenHeight - marginPx)
    }

    private fun handleActionDown(event: MotionEvent) {
        val x = event.x
        val y = event.y
        val now = event.eventTime

        if (isEdgeTouch(x, y)) return

        downX1 = x
        downY1 = y
        lastX1 = x
        lastY1 = y
        lastTime1 = now
        downTime1 = now
        totalMovedDistance1 = 0f

        filterDx.reset()
        filterDy.reset()
        accelerationCurve.reset()

        // Check for Double-Tap & Drag
        if (settings.doubleTapDrag && (now - lastTapUpTime) < doubleTapTimeout) {
            val distFromLastTap = hypot(x - downX1, y - downY1)
            if (distFromLastTap < touchSlop * 2) {
                isDragging = true
                onGesture(GestureEvent.Click(HidDescriptor.BUTTON_LEFT, down = true))
                onGesture(GestureEvent.DoubleTapDragStart)
            }
        }
    }

    private fun handlePointerDown(event: MotionEvent) {
        if (event.pointerCount == 2) {
            isTwoFingerGestureActive = true
            downTime2 = event.eventTime
            val x0 = event.getX(0)
            val y0 = event.getY(0)
            val x1 = event.getX(1)
            val y1 = event.getY(1)

            downAvgX = (x0 + x1) / 2f
            downAvgY = (y0 + y1) / 2f
            lastAvgX = downAvgX
            lastAvgY = downAvgY
            lastTime2 = downTime2
            totalMovedDistance2 = 0f
            initialPinchSpan = hypot(x0 - x1, y0 - y1)
        }
    }

    private fun handleActionMove(event: MotionEvent) {
        val pointerCount = event.pointerCount

        if (pointerCount == 1 && !isTwoFingerGestureActive) {
            processHistorical1FingerMovement(event)
        } else if (pointerCount == 2) {
            processHistorical2FingerMovement(event)
        }
    }

    private fun processHistorical1FingerMovement(event: MotionEvent) {
        step1Finger(event.getX(0), event.getY(0), event.eventTime)
    }

    private fun step1Finger(rawX: Float, rawY: Float, timeMs: Long) {
        val dt = (timeMs - lastTime1).toFloat().coerceAtLeast(1f)
        val rawDx = rawX - lastX1
        val rawDy = rawY - lastY1

        totalMovedDistance1 += hypot(rawDx, rawDy)
        lastX1 = rawX
        lastY1 = rawY
        lastTime1 = timeMs

        if (rawDx == 0f && rawDy == 0f) return

        // Filter deltas with 1€ Filter
        val smoothDx = filterDx.filter(rawDx.toDouble(), timeMs.toDouble()).toFloat()
        val smoothDy = filterDy.filter(rawDy.toDouble(), timeMs.toDouble()).toFloat()

        val adjustedDy = if (settings.invertCursorY) -smoothDy else smoothDy

        val output = accelerationCurve.process(
            smoothDx,
            adjustedDy,
            dt,
            settings.trackingSpeed,
            settings.acceleration
        )

        if (output.dx != 0 || output.dy != 0) {
            onGesture(GestureEvent.Move(output.dx, output.dy, isDragging = isDragging))
        }
    }

    private fun processHistorical2FingerMovement(event: MotionEvent) {
        val historySize = event.historySize
        for (h in 0 until historySize) {
            val x0 = event.getHistoricalX(0, h)
            val y0 = event.getHistoricalY(0, h)
            val x1 = event.getHistoricalX(1, h)
            val y1 = event.getHistoricalY(1, h)
            val ht = event.getHistoricalEventTime(h)
            step2Finger(x0, y0, x1, y1, ht)
        }
        step2Finger(event.getX(0), event.getY(0), event.getX(1), event.getY(1), event.eventTime)
    }

    private fun step2Finger(x0: Float, y0: Float, x1: Float, y1: Float, timeMs: Long) {
        val avgX = (x0 + x1) / 2f
        val avgY = (y0 + y1) / 2f
        val rawDx = avgX - lastAvgX
        val rawDy = avgY - lastAvgY

        totalMovedDistance2 += hypot(rawDx, rawDy)

        lastAvgX = avgX
        lastAvgY = avgY
        lastTime2 = timeMs

        // Natural scrolling ONLY affects two-finger scroll, never pointer cursor
        val scrollSign = if (settings.naturalScrolling) -1 else 1
        val vScroll = (rawDy * 0.12f * settings.scrollSpeed * scrollSign).toInt()
        val hScroll = (rawDx * 0.12f * settings.scrollSpeed * scrollSign).toInt()

        if (vScroll != 0 || hScroll != 0) {
            onGesture(GestureEvent.Scroll(vScroll, hScroll))
        }

        // Pinch detection foundation
        val currentSpan = hypot(x0 - x1, y0 - y1)
        if (initialPinchSpan > 0 && abs(currentSpan - initialPinchSpan) > touchSlop * 3) {
            val scaleFactor = currentSpan / initialPinchSpan
            onGesture(GestureEvent.Pinch(scaleFactor))
        }
    }

    private fun handlePointerUp(event: MotionEvent) {
        if (event.pointerCount == 2 && isTwoFingerGestureActive) {
            val duration = event.eventTime - downTime2
            if (totalMovedDistance2 < touchSlop && duration < tapTimeout && settings.twoFingerRightClick) {
                onGesture(GestureEvent.RightClick)
            } else if (settings.momentumScrolling) {
                velocityTracker?.computeCurrentVelocity(1000)
                val vy = velocityTracker?.yVelocity ?: 0f
                val vx = velocityTracker?.xVelocity ?: 0f
                onGesture(GestureEvent.Scroll(0, 0))
            }
            isTwoFingerGestureActive = false
        }
    }

    private fun handleActionUp(event: MotionEvent) {
        val now = event.eventTime
        val duration = now - downTime1

        if (isDragging) {
            isDragging = false
            onGesture(GestureEvent.Click(HidDescriptor.BUTTON_LEFT, down = false))
            onGesture(GestureEvent.DragEnd)
        } else if (totalMovedDistance1 < touchSlop && duration < tapTimeout && settings.tapToClick) {
            onGesture(GestureEvent.TapClick)
            lastTapUpTime = now
        }

        resetState()
    }

    private fun handleActionCancel() {
        if (isDragging) {
            onGesture(GestureEvent.Click(HidDescriptor.BUTTON_LEFT, down = false))
            onGesture(GestureEvent.DragEnd)
        }
        resetState()
    }

    private fun resetState() {
        isDragging = false
        isTwoFingerGestureActive = false
        velocityTracker?.recycle()
        velocityTracker = null
        filterDx.reset()
        filterDy.reset()
        accelerationCurve.reset()
    }

    fun getVelocityTracker(): VelocityTracker? = velocityTracker
}
