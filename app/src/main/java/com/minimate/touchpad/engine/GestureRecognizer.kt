package com.minimate.touchpad.engine

import android.content.Context
import android.view.MotionEvent
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
    val pressure: Float = 1f,
    val startedAtSeconds: Float = 0f,
    val active: Boolean = true
)

/**
 * High-precision gesture recognizer with subpixel accumulation for butter-smooth
 * pointer tracking and 2-finger scrolling.
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

    // 1-finger tracking state
    private var downTime1 = 0L
    private var downX1 = 0f
    private var downY1 = 0f
    private var lastX1 = 0f
    private var lastY1 = 0f
    private var lastTime1 = 0L
    private var totalMovedDistance1 = 0f
    private var isDragging = false
    private var dragArmed = false
    private var gestureAccepted = false
    private var suppressSingleFingerUntilUp = false
    private var lastTapUpTime = 0L
    private var lastTapX = Float.NaN
    private var lastTapY = Float.NaN

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
    private var isScrollingTwoFinger = false
    private var didScrollTwoFinger = false
    private var scrollVelocityX = 0f
    private var scrollVelocityY = 0f

    // Subpixel scroll accumulation for effortless, silky smooth scrolling
    private var subpixelScrollX = 0f
    private var subpixelScrollY = 0f

    var settings: TouchpadSettings = TouchpadSettings()
    var screenWidth = 0f
    var screenHeight = 0f

    // Live active touch points for visual finger effects
    val activeTouchPoints = mutableListOf<TouchPoint>()
    var onTouchPointsUpdated: ((List<TouchPoint>) -> Unit)? = null

    fun onTouchEvent(event: MotionEvent): Boolean {
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
                if (event.actionMasked == MotionEvent.ACTION_POINTER_UP && i == event.actionIndex) continue
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
        if (screenWidth <= marginPx * 2f || screenHeight <= marginPx * 2f) return false
        return x < marginPx || x > (screenWidth - marginPx) || y < marginPx || y > (screenHeight - marginPx)
    }

    private fun handleActionDown(event: MotionEvent) {
        val x = event.x
        val y = event.y
        val now = event.eventTime

        gestureAccepted = !isEdgeTouch(x, y)
        suppressSingleFingerUntilUp = false
        dragArmed = false
        if (!gestureAccepted) return

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

        // Arm on the second tap, but do not press the button until the finger moves.
        // This preserves an ordinary double-click when the second tap is released.
        dragArmed = settings.doubleTapDrag &&
            now - lastTapUpTime in 1 until doubleTapTimeout &&
            !lastTapX.isNaN() && hypot(x - lastTapX, y - lastTapY) < touchSlop * 2f
    }

    private fun handlePointerDown(event: MotionEvent) {
        if (!gestureAccepted) return
        if (event.pointerCount > 2) {
            isTwoFingerGestureActive = false
            isScrollingTwoFinger = false
            suppressSingleFingerUntilUp = true
            return
        }
        if (event.pointerCount != 2) return
        if (event.pointerCount >= 2) {
            if (isDragging) {
                onGesture(GestureEvent.Click(HidDescriptor.BUTTON_LEFT, down = false))
                onGesture(GestureEvent.DragEnd)
                isDragging = false
            }
            dragArmed = false
            isTwoFingerGestureActive = true
            suppressSingleFingerUntilUp = true
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
            isScrollingTwoFinger = false
            didScrollTwoFinger = false
            scrollVelocityX = 0f
            scrollVelocityY = 0f
            initialPinchSpan = hypot(x0 - x1, y0 - y1)
            subpixelScrollX = 0f
            subpixelScrollY = 0f
        }
    }

    private fun handleActionMove(event: MotionEvent) {
        val pointerCount = event.pointerCount

        if (!gestureAccepted) return
        if (pointerCount == 1 && !isTwoFingerGestureActive && !suppressSingleFingerUntilUp) {
            processHistorical1FingerMovement(event)
        } else if (pointerCount >= 2) {
            processHistorical2FingerMovement(event)
        }
    }

    private fun processHistorical1FingerMovement(event: MotionEvent) {
        for (historyIndex in 0 until event.historySize) {
            step1Finger(
                event.getHistoricalX(0, historyIndex),
                event.getHistoricalY(0, historyIndex),
                event.getHistoricalEventTime(historyIndex)
            )
        }
        step1Finger(event.getX(0), event.getY(0), event.eventTime)
    }

    private fun step1Finger(rawX: Float, rawY: Float, timeMs: Long) {
        val dt = (timeMs - lastTime1).toFloat().coerceAtLeast(1f)
        val rawDx = rawX - lastX1
        val rawDy = rawY - lastY1

        totalMovedDistance1 = maxOf(totalMovedDistance1, hypot(rawX - downX1, rawY - downY1))
        lastX1 = rawX
        lastY1 = rawY
        lastTime1 = timeMs

        if (rawDx == 0f && rawDy == 0f) return

        if (dragArmed && totalMovedDistance1 >= touchSlop) {
            dragArmed = false
            isDragging = true
            onGesture(GestureEvent.Click(HidDescriptor.BUTTON_LEFT, down = true))
            onGesture(GestureEvent.DoubleTapDragStart)
        }

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
        if (event.pointerCount < 2) return
        for (historyIndex in 0 until event.historySize) {
            step2Finger(
                event.getHistoricalX(0, historyIndex),
                event.getHistoricalY(0, historyIndex),
                event.getHistoricalX(1, historyIndex),
                event.getHistoricalY(1, historyIndex),
                event.getHistoricalEventTime(historyIndex)
            )
        }
        step2Finger(event.getX(0), event.getY(0), event.getX(1), event.getY(1), event.eventTime)
    }

    private fun step2Finger(x0: Float, y0: Float, x1: Float, y1: Float, timeMs: Long) {
        val avgX = (x0 + x1) / 2f
        val avgY = (y0 + y1) / 2f
        val rawDx = avgX - lastAvgX
        val rawDy = avgY - lastAvgY
        val dtSeconds = ((timeMs - lastTime2).coerceAtLeast(1L)) / 1000f

        totalMovedDistance2 = maxOf(totalMovedDistance2, hypot(avgX - downAvgX, avgY - downAvgY))

        lastAvgX = avgX
        lastAvgY = avgY
        lastTime2 = timeMs

        if (rawDx == 0f && rawDy == 0f) return

        // A small centroid threshold prevents stationary fingers from slowly
        // accumulating wheel ticks due to digitizer noise.
        if (!isScrollingTwoFinger) {
            if (totalMovedDistance2 < touchSlop) return
            isScrollingTwoFinger = true
            subpixelScrollX = 0f
            subpixelScrollY = 0f
            scrollVelocityX = 0f
            scrollVelocityY = 0f
            return
        }

        val instantVelocityX = rawDx / dtSeconds
        val instantVelocityY = rawDy / dtSeconds
        scrollVelocityX = scrollVelocityX * 0.65f + instantVelocityX * 0.35f
        scrollVelocityY = scrollVelocityY * 0.65f + instantVelocityY * 0.35f

        // Natural scrolling ONLY affects two-finger scroll, never pointer cursor
        val scrollSign = if (settings.naturalScrolling) -1 else 1

        // Calibrated scroll scaling with subpixel accumulation (effortless scrolling at any swipe speed)
        val targetV = rawDy * 0.035f * settings.scrollSpeed * scrollSign + subpixelScrollY
        val targetH = rawDx * 0.035f * settings.scrollSpeed * scrollSign + subpixelScrollX

        val intV = targetV.toInt()
        val intH = targetH.toInt()

        subpixelScrollY = targetV - intV
        subpixelScrollX = targetH - intH

        if (intV != 0 || intH != 0) {
            didScrollTwoFinger = true
            onGesture(GestureEvent.Scroll(intV, intH))
        }

        // Pinch detection foundation
        val currentSpan = hypot(x0 - x1, y0 - y1)
        if (initialPinchSpan > 0 && abs(currentSpan - initialPinchSpan) > touchSlop * 3) {
            val scaleFactor = currentSpan / initialPinchSpan
            onGesture(GestureEvent.Pinch(scaleFactor))
        }
    }

    private fun handlePointerUp(event: MotionEvent) {
        if (!gestureAccepted) return
        if (event.pointerCount == 2 && isTwoFingerGestureActive) {
            val duration = event.eventTime - downTime2
            if (totalMovedDistance2 < touchSlop && duration < tapTimeout && settings.twoFingerRightClick) {
                onGesture(GestureEvent.RightClick)
            } else if (didScrollTwoFinger && settings.momentumScrolling) {
                onGesture(GestureEvent.ScrollFling(scrollVelocityX, scrollVelocityY))
            }
            isTwoFingerGestureActive = false
            isScrollingTwoFinger = false
            subpixelScrollX = 0f
            subpixelScrollY = 0f
        }
    }

    private fun handleActionUp(event: MotionEvent) {
        if (!gestureAccepted) {
            resetState()
            return
        }
        val now = event.eventTime
        val duration = now - downTime1

        if (isDragging) {
            isDragging = false
            onGesture(GestureEvent.Click(HidDescriptor.BUTTON_LEFT, down = false))
            onGesture(GestureEvent.DragEnd)
        } else if (!suppressSingleFingerUntilUp && totalMovedDistance1 < touchSlop && duration < tapTimeout && settings.tapToClick) {
            onGesture(GestureEvent.TapClick)
            lastTapUpTime = now
            lastTapX = event.x
            lastTapY = event.y
        } else if (!dragArmed) {
            lastTapUpTime = 0L
            lastTapX = Float.NaN
            lastTapY = Float.NaN
        }

        resetState()
    }

    private fun handleActionCancel() {
        if (isDragging) {
            onGesture(GestureEvent.Click(HidDescriptor.BUTTON_LEFT, down = false))
            onGesture(GestureEvent.DragEnd)
            isDragging = false
        }
        resetState()
    }

    private fun resetState() {
        downX1 = 0f
        downY1 = 0f
        lastX1 = 0f
        lastY1 = 0f
        totalMovedDistance1 = 0f
        gestureAccepted = false
        suppressSingleFingerUntilUp = false
        dragArmed = false
        isTwoFingerGestureActive = false
        isScrollingTwoFinger = false
        subpixelScrollX = 0f
        subpixelScrollY = 0f
        filterDx.reset()
        filterDy.reset()
        accelerationCurve.reset()
    }
}
