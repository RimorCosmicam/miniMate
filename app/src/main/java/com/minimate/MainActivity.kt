package com.minimate

import android.Manifest
import android.content.pm.PackageManager
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.minimate.bluetooth.BatteryReporter
import com.minimate.bluetooth.BluetoothHidManager
import com.minimate.touchpad.engine.TouchpadEngine
import com.minimate.touchpad.model.HapticIntensity
import com.minimate.ui.TouchpadScreen
import com.minimate.ui.theme.MinimateTheme
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    private lateinit var hidManager: BluetoothHidManager
    private lateinit var batteryReporter: BatteryReporter
    private lateinit var touchpadEngine: TouchpadEngine

    // State tracking for Volume Up + Down combo unlock
    private var isVolUpPressed = false
    private var isVolDownPressed = false
    private var lastVolUpTime = 0L
    private var lastVolDownTime = 0L

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            hidManager.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR

        // Keep screen alive while touchpad is active
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // MiniMate is a dedicated-device experience: request the fastest native cover-display
        // mode so shader motion and touch distortion are not artificially capped at 60 Hz.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            val fastestMode = windowManager.defaultDisplay.supportedModes.maxByOrNull { it.refreshRate }
            if (fastestMode != null) {
                window.attributes = window.attributes.apply {
                    preferredDisplayModeId = fastestMode.modeId
                    preferredRefreshRate = fastestMode.refreshRate
                }
            }
        }

        // Immersive edge-to-edge fullscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        hidManager = BluetoothHidManager(this)
        batteryReporter = BatteryReporter(this)
        touchpadEngine = TouchpadEngine(this, hidManager)

        checkAndRequestPermissions()

        setContent {
            MinimateTheme {
                val bluetoothState by hidManager.uiState.collectAsState()
                val batteryPercentage by batteryReporter.batteryLevel.collectAsState()

                TouchpadScreen(
                    touchpadEngine = touchpadEngine,
                    hidManager = hidManager,
                    bluetoothState = bluetoothState,
                    batteryPercentage = batteryPercentage,
                    onRequestPermissions = { checkAndRequestPermissions() },
                    onDimModeChanged = { isDim ->
                        setDimMode(isDim)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val now = SystemClock.uptimeMillis()

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            isVolUpPressed = true
            lastVolUpTime = now
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            isVolDownPressed = true
            lastVolDownTime = now
        } else {
            return super.onKeyDown(keyCode, event)
        }

        if (touchpadEngine.settings.value.isLocked) {
            checkAndHandleUnlock()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> isVolUpPressed = false
            KeyEvent.KEYCODE_VOLUME_DOWN -> isVolDownPressed = false
            else -> return super.onKeyUp(keyCode, event)
        }
        return if (touchpadEngine.settings.value.isLocked) true else super.onKeyUp(keyCode, event)
    }

    private fun checkAndHandleUnlock(): Boolean {
        val currentSettings = touchpadEngine.settings.value
        val chordIsPressed = isVolUpPressed && isVolDownPressed &&
            abs(lastVolUpTime - lastVolDownTime) <= 500L
        if (currentSettings.isLocked && chordIsPressed) {
            touchpadEngine.updateSettings(currentSettings.copy(isLocked = false))
            touchpadEngine.hapticEngine.playModeTransition(HapticIntensity.STRONG)
            isVolUpPressed = false
            isVolDownPressed = false
            lastVolUpTime = 0L
            lastVolDownTime = 0L
            return true
        }
        return false
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN
            )
            val needed = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                permissionLauncher.launch(needed.toTypedArray())
            } else {
                hidManager.start()
            }
        } else {
            hidManager.start()
        }
    }

    private fun setDimMode(isDim: Boolean) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = if (isDim) 0.01f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = layoutParams
    }

    override fun onStart() {
        super.onStart()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
        batteryReporter.start()
    }

    override fun onStop() {
        super.onStop()
        batteryReporter.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        touchpadEngine.close()
        hidManager.stop()
    }
}
