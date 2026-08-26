package com.minimate.ui

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.minimate.bluetooth.BluetoothHidManager
import com.minimate.bluetooth.BluetoothUiState
import com.minimate.bluetooth.ConnectionStatus
import com.minimate.bluetooth.HidDescriptor
import com.minimate.touchpad.engine.TouchpadEngine
import com.minimate.touchpad.model.BallAction
import com.minimate.touchpad.model.HapticIntensity
import com.minimate.touchpad.model.TouchpadSettings
import com.minimate.touchpad.model.validColorway
import com.minimate.ui.components.BluetoothPairingDialog
import com.minimate.ui.components.ClockBatteryOverlay
import com.minimate.ui.components.HudToast
import com.minimate.ui.components.LiquidGlassAnalogStick
import com.minimate.ui.components.LiveCalibrationMode
import com.minimate.ui.components.LiveCalibrationOverlay
import com.minimate.ui.components.PermissionPrompt
import com.minimate.ui.components.ScreenEditorOverlay
import com.minimate.ui.components.SettingsSheet
import com.minimate.ui.components.ThemeTesterOverlay
import com.minimate.ui.shader.BackgroundShaderCanvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TouchpadScreen(
    touchpadEngine: TouchpadEngine,
    hidManager: BluetoothHidManager,
    bluetoothState: BluetoothUiState,
    batteryPercentage: Int,
    onRequestPermissions: () -> Unit,
    onDimModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by touchpadEngine.settings.collectAsState()
    val shaderTouchPoints by touchpadEngine.shaderTouchPoints.collectAsState()
    var isDimMode by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showPairingDialog by remember { mutableStateOf(false) }
    var showScreenEditor by remember { mutableStateOf(false) }
    var showThemeTester by remember { mutableStateOf(false) }
    var liveCalibrationMode by remember { mutableStateOf<LiveCalibrationMode?>(null) }
    var themeTesterOriginal by remember { mutableStateOf<TouchpadSettings?>(null) }
    var hudMessage by remember { mutableStateOf<String?>(null) }
    var hudIcon by remember { mutableStateOf<ImageVector?>(null) }
    
    var screenWidthPx by remember { mutableFloatStateOf(1080f) }
    var screenHeightPx by remember { mutableFloatStateOf(1080f) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val dimRatio by animateFloatAsState(
        targetValue = if (isDimMode) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "DimRatio"
    )

    fun showToast(msg: String, icon: ImageVector? = null) {
        hudMessage = msg
        hudIcon = icon
        scope.launch {
            delay(2500)
            if (hudMessage == msg) hudMessage = null
        }
    }

    fun makeDeviceDiscoverable() {
        try {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            showToast("Discoverable for 180s • Connect on Host", Icons.Default.Bluetooth)
        } catch (_: Exception) {
            showToast("Bluetooth Discovery Request Sent")
        }
    }

    fun executeStickAction(action: BallAction) {
        touchpadEngine.hapticEngine.playClick(settings.hapticIntensity)
        when (action) {
            BallAction.MIDDLE_CLICK -> {
                scope.launch {
                    hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_MIDDLE, dx = 0, dy = 0)
                    delay(20)
                    hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_NONE, dx = 0, dy = 0)
                }
            }
            BallAction.RIGHT_CLICK -> {
                scope.launch {
                    hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_RIGHT, dx = 0, dy = 0)
                    delay(20)
                    hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_NONE, dx = 0, dy = 0)
                }
            }
            BallAction.LEFT_CLICK -> {
                scope.launch {
                    hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_LEFT, dx = 0, dy = 0)
                    delay(20)
                    hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_NONE, dx = 0, dy = 0)
                }
            }
            BallAction.BACK_BUTTON -> {
                scope.launch {
                    hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_BACK, dx = 0, dy = 0)
                    delay(20)
                    hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_NONE, dx = 0, dy = 0)
                }
            }
            BallAction.FORWARD_BUTTON -> {
                scope.launch {
                    hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_FORWARD, dx = 0, dy = 0)
                    delay(20)
                    hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_NONE, dx = 0, dy = 0)
                }
            }
            BallAction.AMOLED_DIM -> {
                isDimMode = !isDimMode
                onDimModeChanged(isDimMode)
                touchpadEngine.hapticEngine.playModeTransition(settings.hapticIntensity)
                showToast(if (isDimMode) "Amoled Mode Active" else "Amoled Mode Disabled", if (isDimMode) Icons.Default.Brightness2 else Icons.Default.DarkMode)
            }
            BallAction.OPEN_SETTINGS -> {
                hidManager.refreshPairedDevices()
                showSettingsSheet = true
            }
            BallAction.PAIRING_MODE -> {
                hidManager.refreshPairedDevices()
                showPairingDialog = true
            }
            BallAction.SCREEN_EDITOR -> {
                showScreenEditor = true
            }
            BallAction.LIQUID_WOBBLE -> {
                touchpadEngine.hapticEngine.playModeTransition(settings.hapticIntensity)
            }
            BallAction.DISABLED -> {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { size ->
                screenWidthPx = size.width.toFloat()
                screenHeightPx = size.height.toFloat()
                touchpadEngine.setScreenDimensions(screenWidthPx, screenHeightPx)
            }
    ) {
        // Layer 1: Interactive GPU Background Shader with 10 Inspired Themes
        BackgroundShaderCanvas(
            theme = settings.backgroundTheme,
            variantIndex = settings.themeVariantIndex,
            touchPoints = if (settings.fingerEffectsEnabled) shaderTouchPoints else emptyList(),
            customImageUri = settings.customImageUri,
            dimRatio = dimRatio,
            animationSpeed = settings.backgroundAnimation.speed,
            themeFilter = settings.themeFilter,
            shaderTheme = settings.abstractShaderTheme,
            shaderSubthemeIndex = settings.abstractSubthemeIndex,
            shaderRecolor = validColorway(settings.abstractShaderTheme, settings.abstractSubthemeIndex, settings.shaderRecolor),
            customShaderColors = settings.customShaderColors,
            modifier = Modifier.fillMaxSize()
        )

        // Layer 2: Fullscreen Trackpad Touch Surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { motionEvent ->
                    if (!showScreenEditor) {
                        touchpadEngine.onTouchEvent(motionEvent)
                    } else {
                        false
                    }
                }
        )

        // Layer 3: touch effects are shader-space distortions inside the scene.
        // Layer 4: AMOLED blackout stays below its exit control.
        if (dimRatio > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimRatio * 0.98f))
            )
        }

        // Layer 5: Interactive Clock & Battery HUD Widget (Tap = AMOLED, Hold = Settings).
        // Force the minimal pill while dimmed so AMOLED mode always has a visible escape hatch,
        // even when the user's normal clock style is Hidden.
        if (!showThemeTester) {
            ClockBatteryOverlay(
                clockStyle = if (isDimMode) com.minimate.touchpad.model.ClockStyle.MINIMAL_PILL else settings.clockStyle,
                // The main HUD is anchored to the physical cover-screen center. Its measured width is
                // used by ClockBatteryOverlay, so battery/AM-PM content cannot shift it off-center.
                positionXFraction = 0.5f,
                positionYFraction = settings.clockPositionY,
                clockScale = settings.clockScale,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                show24Hour = settings.show24HourFormat,
                showSeconds = settings.showSeconds,
                showBattery = settings.showBatteryPercentage,
                batteryPercentage = batteryPercentage,
                bluetoothState = bluetoothState,
                onTap = {
                    executeStickAction(BallAction.AMOLED_DIM)
                },
                onLongPress = {
                    touchpadEngine.hapticEngine.playModeTransition(settings.hapticIntensity)
                    hidManager.refreshPairedDevices()
                    showSettingsSheet = true
                }
            )
        }

        // Layer 6: Pure Liquid Glass 2D Analog Stick (Single-Hand Scroll / Click Mastery)
        if (!isDimMode && !showThemeTester && !showScreenEditor && !settings.isLocked && settings.stickEnabled) {
            val centeringStick = liveCalibrationMode == LiveCalibrationMode.STICK
            LiquidGlassAnalogStick(
                stickSizeDp = settings.ballSizeDp,
                positionXFraction = if (centeringStick) 0.5f else settings.ballPositionX,
                positionYFraction = if (centeringStick) 0.5f else settings.ballPositionY,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                mode = settings.analogStickMode,
                theme = settings.stickTheme,
                scrollSensitivity = settings.stickScrollSensitivity,
                deadzone = settings.stickDeadzone,
                isLocked = settings.isLocked,
                onSingleTap = {
                    executeStickAction(settings.stickSingleTapAction)
                },
                onDoubleTap = {
                    executeStickAction(settings.stickDoubleTapAction)
                },
                onHold = {
                    executeStickAction(settings.stickHoldAction)
                },
                onAnalogScroll = { vScroll, hScroll ->
                    hidManager.sendMouseInput(
                        buttons = HidDescriptor.BUTTON_NONE,
                        dx = 0,
                        dy = 0,
                        wheel = vScroll,
                        pan = hScroll
                    )
                },
                onAnalogCursorMove = { dx, dy ->
                    hidManager.sendMouseInput(
                        buttons = HidDescriptor.BUTTON_NONE,
                        dx = dx,
                        dy = dy,
                        wheel = 0,
                        pan = 0
                    )
                },
                onTouchDown = {
                    touchpadEngine.hapticEngine.playTouchDown(settings.hapticIntensity)
                }
            )
        }

        // Layer 7: Freeform Screen Editor Overlay (Move Stick & Clock freely)
        if (showScreenEditor) {
            ScreenEditorOverlay(
                settings = settings,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                onSettingsChange = { newSettings ->
                    touchpadEngine.updateSettings(newSettings)
                },
                onClose = {
                    showScreenEditor = false
                }
            )
        }

        // Layer 8: Settings & Theme Manager Modal Sheet (3 Tabs)
        if (showSettingsSheet) {
            SettingsSheet(
                settings = settings,
                bluetoothState = bluetoothState,
                batteryPercentage = batteryPercentage,
                onSettingsChange = { newSettings ->
                    touchpadEngine.updateSettings(newSettings)
                },
                onOpenThemeTester = {
                    themeTesterOriginal = settings
                    touchpadEngine.updateSettings(
                        settings.copy(shaderRecolor = validColorway(settings.abstractShaderTheme, settings.abstractSubthemeIndex, settings.shaderRecolor))
                    )
                    showSettingsSheet = false
                    showThemeTester = true
                },
                onOpenScreenEditor = {
                    showScreenEditor = true
                },
                onOpenTrackpadTester = {
                    liveCalibrationMode = LiveCalibrationMode.TRACKPAD
                },
                onOpenStickTester = {
                    if (settings.analogStickMode != com.minimate.touchpad.model.AnalogStickMode.ANALOG_SCROLL) {
                        touchpadEngine.updateSettings(settings.copy(analogStickMode = com.minimate.touchpad.model.AnalogStickMode.ANALOG_SCROLL))
                    }
                    liveCalibrationMode = LiveCalibrationMode.STICK
                },
                onConnectAddress = { address ->
                    hidManager.connectByAddress(address)
                },
                onDisconnect = {
                    hidManager.disconnect()
                },
                onPairNewDevice = {
                    showSettingsSheet = false
                    showPairingDialog = true
                },
                onRefreshDevices = {
                    hidManager.refreshPairedDevices()
                },
                onDismiss = { showSettingsSheet = false }
            )
        }

        liveCalibrationMode?.let { mode ->
            LiveCalibrationOverlay(
                mode = mode,
                settings = settings,
                onSettingsChange = touchpadEngine::updateSettings,
                onDone = { liveCalibrationMode = null },
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }

        if (showThemeTester) {
            ThemeTesterOverlay(
                settings = settings,
                onSettingsChange = touchpadEngine::updateSettings,
                onPreviewTouchEvent = touchpadEngine::onPreviewTouchEvent,
                onKeep = {
                    themeTesterOriginal = null
                    showThemeTester = false
                },
                onCancel = {
                    themeTesterOriginal?.let(touchpadEngine::updateSettings)
                    themeTesterOriginal = null
                    showThemeTester = false
                }
            )
        }

        // Layer 9: Dedicated In-App Bluetooth Pairing Hub Dialog
        if (showPairingDialog) {
            BluetoothPairingDialog(
                bluetoothState = bluetoothState,
                onMakeDiscoverable = { makeDeviceDiscoverable() },
                onConnectHost = { address -> hidManager.connectByAddress(address) },
                onDisconnect = { hidManager.disconnect() },
                onRefresh = { hidManager.refreshPairedDevices() },
                onDismiss = { showPairingDialog = false }
            )
        }

        // Layer 10: Minimal HUD Toast Feedback
        HudToast(
            message = hudMessage,
            icon = hudIcon,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
        )

        // Layer 11: Bluetooth Permission Prompt if needed
        if (bluetoothState.status == ConnectionStatus.NO_PERMISSION) {
            PermissionPrompt(
                onRequestPermission = onRequestPermissions
            )
        }
    }
}
