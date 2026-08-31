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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import com.minimate.ui.components.PermissionItem
import com.minimate.ui.components.ClockBatteryOverlay
import com.minimate.ui.components.Welcome
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.minimate.bluetooth.BatteryReporter
import com.minimate.bluetooth.BluetoothAudioBridge
import com.minimate.bluetooth.AudioBridgeService
import com.minimate.bluetooth.BluetoothHidManager
import com.minimate.bluetooth.WebcamCapture
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
    private lateinit var webcamCapture: WebcamCapture

    // State tracking for Volume Up + Down combo unlock
    private var isVolUpPressed = false
    private var isVolDownPressed = false
    private var lastVolUpTime = 0L
    private var lastVolDownTime = 0L
    private var bluetoothServicesStarted = false

    /** Bumped when the system dialogs return, so the welcome list re-reads what was granted. */
    private var permissionTick by mutableIntStateOf(0)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionTick++
        startBluetoothServicesIfAllowed()
    }

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
        enterImmersive()

        hidManager = BluetoothHidManager(this)
        audioBridge = (application as MinimateApp).audioBridge
        batteryReporter = BatteryReporter(this)
        touchpadEngine = TouchpadEngine(this, hidManager)
        webcamCapture = WebcamCapture(this, audioBridge)

        setContent {
            MinimateTheme {
                val bluetoothState by hidManager.uiState.collectAsState()
                val batteryPercentage by batteryReporter.batteryLevel.collectAsState()

                val settings by touchpadEngine.settings.collectAsState()

                // The app is composed and running underneath the introduction, so when the ground
                // pulls apart what is revealed is the real thing already in motion rather than a
                // screen that starts loading once the transition is over.
                Box(Modifier.fillMaxSize()) {
                    TouchpadScreen(
                        touchpadEngine = touchpadEngine,
                        hidManager = hidManager,
                        audioBridge = audioBridge,
                        webcamCapture = webcamCapture,
                        bluetoothState = bluetoothState,
                        batteryPercentage = batteryPercentage,
                        onRequestPermissions = { checkAndRequestPermissions() },
                        pillVisible = settings.onboardingSeen,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (!settings.onboardingSeen) {
                        Welcome(
                            permissions = permissionState(permissionTick),
                            platform = settings.hostPlatform,
                            onPlatform = { chosen ->
                                touchpadEngine.updateSettings(
                                    touchpadEngine.settings.value.copy(hostPlatform = chosen)
                                )
                            },
                            onGrant = { checkAndRequestPermissions() },
                            pill = {
                                ClockBatteryOverlay(
                                    clockStyle = settings.clockStyle,
                                    positionXFraction = .5f,
                                    positionYFraction = .5f,
                                    clockScale = settings.clockScale,
                                    screenWidthPx = 0f,
                                    screenHeightPx = 0f,
                                    show24Hour = settings.show24HourFormat,
                                    showSeconds = settings.showSeconds,
                                    showBattery = settings.showBatteryPercentage,
                                    batteryPercentage = batteryPercentage,
                                    bluetoothState = bluetoothState,
                                    amoledMode = false,
                                    onTap = {},
                                    onDoubleTap = {},
                                    onLongPress = {}
                                )
                            },
                            targetX = settings.clockPositionX,
                            targetY = settings.clockPositionY,
                            onFinished = {
                                touchpadEngine.updateSettings(
                                    touchpadEngine.settings.value.copy(onboardingSeen = true)
                                )
                                startBluetoothServicesIfAllowed()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
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

    /** Everything the app asks for, with what it is for and whether it has been granted yet. */
    private fun permissionState(@Suppress("UNUSED_PARAMETER") tick: Int): List<PermissionItem> {
        fun granted(permission: String) =
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

        return buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(PermissionItem("Bluetooth", "Acts as your Mac's mouse and keyboard",
                    granted(Manifest.permission.BLUETOOTH_CONNECT) &&
                        granted(Manifest.permission.BLUETOOTH_ADVERTISE) &&
                        granted(Manifest.permission.BLUETOOTH_SCAN)))
            }
            add(PermissionItem("Microphone", "Sends your voice to the Mac",
                granted(Manifest.permission.RECORD_AUDIO)))
            add(PermissionItem("Camera", "Sends a picture to the Mac",
                granted(Manifest.permission.CAMERA)))
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
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
            val needed = listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA).filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
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
        enterImmersive()
        // Permissions can be granted from outside the app entirely. Re-reading on resume means
        // the welcome list is never showing a state that stopped being true while it was away.
        permissionTick++
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Anything that takes focus away — the shade, a permission dialog, the recents switcher —
        // brings the bars back with it, and nothing was asking for them to leave again.
        if (hasFocus) enterImmersive()
    }

    /**
     * Hides the system bars, and asks for them back only transiently.
     *
     * Under BEHAVIOR_DEFAULT a swipe restores the bars permanently, so the first time the
     * navigation bar was summoned it simply stayed for the rest of the session and the app
     * stopped being fullscreen. Transient is the behaviour this wants: swipe to see them, and
     * they withdraw on their own.
     */
    private fun enterImmersive() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onStop() {
        super.onStop()
        batteryReporter.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        touchpadEngine.close()
        webcamCapture.close()
        hidManager.stop()
    }
}
