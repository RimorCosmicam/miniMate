package com.minimate

import android.Manifest
import android.content.pm.PackageManager
import android.content.pm.ActivityInfo
import android.content.Intent
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
import com.minimate.bluetooth.BluetoothAudioBridge
import com.minimate.bluetooth.AudioBridgeService
import com.minimate.bluetooth.BluetoothHidManager
import com.minimate.touchpad.engine.TouchpadEngine
import com.minimate.touchpad.model.HapticIntensity
import com.minimate.ui.TouchpadScreen
import com.minimate.ui.theme.MinimateTheme
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    private lateinit var hidManager: BluetoothHidManager
    private lateinit var audioBridge: BluetoothAudioBridge
    private lateinit var batteryReporter: BatteryReporter
    private lateinit var touchpadEngine: TouchpadEngine

    // State tracking for Volume Up + Down combo unlock
    private var isVolUpPressed = false
    private var isVolDownPressed = false
    private var lastVolUpTime = 0L
    private var lastVolDownTime = 0L
    private var bluetoothServicesStarted = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { startBluetoothServicesIfAllowed() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

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
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        hidManager = BluetoothHidManager(this)
        audioBridge = (application as MinimateApp).audioBridge
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
                    audioBridge = audioBridge,
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
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.RECORD_AUDIO
            )
            val needed = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                permissionLauncher.launch(needed.toTypedArray())
            } else {
                startBluetoothServicesIfAllowed()
            }
        } else {
            val microphoneGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (!microphoneGranted) permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            startBluetoothServicesIfAllowed()
        }
    }

    private fun startBluetoothServicesIfAllowed() {
        if (bluetoothServicesStarted) return
        val connectAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        if (!connectAllowed) return
        bluetoothServicesStarted = true
        hidManager.start()
        ContextCompat.startForegroundService(this, Intent(this, AudioBridgeService::class.java))
    }

    private fun setDimMode(isDim: Boolean) {
        val layoutParams = window.attributes
        // AMOLED mode saves power with true black pixels, not by making its controls unreadable.
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = layoutParams
    }

    override fun onStart() {
        super.onStart()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        batteryReporter.start()
    }

    override fun onResume() {
        super.onResume()
        // Samsung may recreate or move the Activity when the FlexWindow state changes.
        // Reassert the strongest sensor-independent lock on every foreground transition.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
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
