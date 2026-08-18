package com.minimate.touchpad.engine

import android.content.Context
import android.view.MotionEvent
import com.minimate.bluetooth.BluetoothHidManager
import com.minimate.bluetooth.HidDescriptor
import com.minimate.touchpad.model.GestureEvent
import com.minimate.touchpad.model.TouchpadSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Main coordinator between Touch Gestures, Physics Momentum, Haptics, and Bluetooth HID output.
 */
class TouchpadEngine(
    private val context: Context,
    private val hidManager: BluetoothHidManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val hapticEngine = HapticFeedbackEngine(context)

    private val _settings = MutableStateFlow(TouchpadSettings())
    val settings: StateFlow<TouchpadSettings> = _settings.asStateFlow()

    private val _activeTouchPoints = MutableStateFlow<List<TouchPoint>>(emptyList())
    val activeTouchPoints: StateFlow<List<TouchPoint>> = _activeTouchPoints.asStateFlow()

    private val momentumScroller = PhysicsMomentumScroller(scope) { vScroll, hScroll ->
        hidManager.sendMouseInput(
            buttons = HidDescriptor.BUTTON_NONE,
            dx = 0,
            dy = 0,
            wheel = vScroll,
            pan = hScroll
        )
    }

    private val gestureRecognizer = GestureRecognizer(context) { event ->
        handleGestureEvent(event)
    }.apply {
        onTouchPointsUpdated = { points ->
            _activeTouchPoints.value = points
        }
    }

    fun updateSettings(newSettings: TouchpadSettings) {
        _settings.value = newSettings
        gestureRecognizer.settings = newSettings
    }

    fun setScreenDimensions(width: Float, height: Float) {
        gestureRecognizer.screenWidth = width
        gestureRecognizer.screenHeight = height
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            momentumScroller.stop()
        }
        return gestureRecognizer.onTouchEvent(event)
    }

    private fun handleGestureEvent(event: GestureEvent) {
        val currentSettings = _settings.value

        when (event) {
            is GestureEvent.Move -> {
                val button = if (event.isDragging) HidDescriptor.BUTTON_LEFT else HidDescriptor.BUTTON_NONE
                hidManager.sendMouseInput(
                    buttons = button,
                    dx = event.dx,
                    dy = event.dy,
                    wheel = 0,
                    pan = 0
                )
            }

            is GestureEvent.Scroll -> {
                if (event.vScroll == 0 && event.hScroll == 0) {
                    // Momentum trigger
                    val tracker = gestureRecognizer.getVelocityTracker()
                    if (tracker != null) {
                        momentumScroller.startFling(
                            initialVelocityY = tracker.yVelocity,
                            initialVelocityX = tracker.xVelocity,
                            friction = currentSettings.momentumFriction,
                            scrollSpeed = currentSettings.scrollSpeed,
                            isNatural = currentSettings.naturalScrolling
                        )
                    }
                } else {
                    hidManager.sendMouseInput(
                        buttons = HidDescriptor.BUTTON_NONE,
                        dx = 0,
                        dy = 0,
                        wheel = event.vScroll,
                        pan = event.hScroll
                    )
                }
            }

            is GestureEvent.TapClick -> {
                hapticEngine.playClick(currentSettings.hapticIntensity)
                scope.launch {
                    hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_LEFT, dx = 0, dy = 0)
                    delay(16)
                    hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_NONE, dx = 0, dy = 0)
                }
            }

            is GestureEvent.RightClick -> {
                hapticEngine.playSecondaryClick(currentSettings.hapticIntensity)
                scope.launch {
                    hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_RIGHT, dx = 0, dy = 0)
                    delay(16)
                    hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_NONE, dx = 0, dy = 0)
                }
            }

            is GestureEvent.Click -> {
                hidManager.sendMouseInput(buttons = event.button, dx = 0, dy = 0)
                if (event.down) {
                    hapticEngine.playClick(currentSettings.hapticIntensity)
                }
            }

            is GestureEvent.DoubleTapDragStart -> {
                hapticEngine.playClick(currentSettings.hapticIntensity)
            }

            is GestureEvent.DragEnd -> {
                hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_NONE, dx = 0, dy = 0)
            }

            is GestureEvent.Pinch -> {
                // Foundation for pinch gesture: can be mapped to zoom
            }
        }
    }
}
