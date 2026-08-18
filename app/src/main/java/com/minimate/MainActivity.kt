package com.minimate

import android.Manifest
import android.content.pm.PackageManager
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

        // Keep screen alive while touchpad is active
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (checkAndHandleUnlock()) return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val isDown = event.action == KeyEvent.ACTION_DOWN
        val now = SystemClock.uptimeMillis()

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            isVolUpPressed = isDown
            if (isDown) lastVolUpTime = now
            if (isDown && checkAndHandleUnlock()) return true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            isVolDownPressed = isDown
            if (isDown) lastVolDownTime = now
            if (isDown && checkAndHandleUnlock()) return true
        }

        return super.dispatchKeyEvent(event)
    }

    private fun checkAndHandleUnlock(): Boolean {
        val currentSettings = touchpadEngine.settings.value
        if (currentSettings.isLocked) {
            // Unlock on any volume button press while locked
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
        batteryReporter.start()
    }

    override fun onStop() {
        super.onStop()
        batteryReporter.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        hidManager.stop()
    }
}
