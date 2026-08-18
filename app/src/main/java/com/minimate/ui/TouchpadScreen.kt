package com.minimate.ui

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Palette
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
import com.minimate.touchpad.model.BackgroundTheme
import com.minimate.touchpad.model.BallAction
import com.minimate.touchpad.model.HapticIntensity
import com.minimate.ui.components.BluetoothPairingDialog
import com.minimate.ui.components.ClockBatteryOverlay
import com.minimate.ui.components.FingerEffectsLayer
import com.minimate.ui.components.HudToast
import com.minimate.ui.components.LiquidGlassBall
import com.minimate.ui.components.LiquidMenuAction
import com.minimate.ui.components.PermissionPrompt
import com.minimate.ui.components.ScreenEditorOverlay
import com.minimate.ui.components.SettingsSheet
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
    val activeTouchPoints by touchpadEngine.activeTouchPoints.collectAsState()
    var isDimMode by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showPairingDialog by remember { mutableStateOf(false) }
    var showScreenEditor by remember { mutableStateOf(false) }
    var hudMessage by remember { mutableStateOf<String?>(null) }
    var hudIcon by remember { mutableStateOf<ImageVector?>(null) }
    
    var screenWidthPx by remember { mutableFloatStateOf(1080f) }
    var screenHeightPx by remember { mutableFloatStateOf(1080f) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            touchpadEngine.updateSettings(
                settings.copy(
                    backgroundTheme = BackgroundTheme.CUSTOM_IMAGE,
                    customImageUri = it.toString()
                )
            )
            hudIcon = Icons.Default.Palette
            hudMessage = "Custom Wallpaper Loaded"
            scope.launch {
                delay(2500)
                hudMessage = null
            }
        }
    }

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

    fun cycleNextThemePreset() {
        val presets = settings.themePresets
        if (presets.isNotEmpty()) {
            val nextIndex = (settings.currentPresetIndex + 1) % presets.size
            val nextPreset = presets[nextIndex]
            touchpadEngine.updateSettings(
                settings.copy(
                    backgroundTheme = nextPreset.theme,
                    themeVariantIndex = nextPreset.variantIndex,
                    customImageUri = nextPreset.customUri,
                    currentPresetIndex = nextIndex
                )
            )
            touchpadEngine.hapticEngine.playModeTransition(settings.hapticIntensity)
            showToast("Preset ${nextIndex + 1}/${presets.size}: ${nextPreset.theme.displayName}", Icons.Default.Palette)
        }
    }

    fun executeBallAction(action: BallAction) {
        touchpadEngine.hapticEngine.playClick(settings.hapticIntensity)
        when (action) {
            BallAction.HOLD_AND_DRAG_MENU -> {
                // Handled natively inside LiquidGlassBall onLongPress / drag
            }
            BallAction.LIQUID_WOBBLE -> {
                touchpadEngine.hapticEngine.playModeTransition(settings.hapticIntensity)
            }
            BallAction.AMOLED_DIM -> {
                isDimMode = !isDimMode
                onDimModeChanged(isDimMode)
                touchpadEngine.hapticEngine.playModeTransition(settings.hapticIntensity)
                showToast(if (isDimMode) "Amoled Mode Active" else "Amoled Mode Disabled", if (isDimMode) Icons.Default.Brightness2 else Icons.Default.DarkMode)
            }
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
            BallAction.CYCLE_THEME -> {
                cycleNextThemePreset()
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
            touchPoints = activeTouchPoints,
            customImageUri = settings.customImageUri,
            dimRatio = dimRatio,
            modifier = Modifier.fillMaxSize()
        )

        // Layer 2: Fullscreen Trackpad Touch Surface
        // Receives all pointer movement events without intercepting child composables
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

        // Layer 3: Multi-Touch Finger Effects Layer (Only if enabled)
        if (settings.fingerEffectsEnabled && dimRatio < 0.8f) {
            FingerEffectsLayer(
                touchPoints = activeTouchPoints,
                effect = settings.fingerEffect,
                enabled = true,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Layer 4: Freeform Clock & Battery HUD Widget
        ClockBatteryOverlay(
            clockStyle = settings.clockStyle,
            positionXFraction = settings.clockPositionX,
            positionYFraction = settings.clockPositionY,
            clockScale = settings.clockScale,
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
            show24Hour = settings.show24HourFormat,
            showSeconds = settings.showSeconds,
            showBattery = settings.showBatteryPercentage,
            batteryPercentage = batteryPercentage,
            bluetoothState = bluetoothState,
            dimRatio = dimRatio
        )

        // Layer 5: Stealth Dim Overlay (Amoled Mode)
        if (dimRatio > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimRatio * 0.98f))
            )
        }

        // Layer 6: Pure Crystal Liquid Glass Ball (Placed on top so it receives all tap/hold/drag events directly)
        if (!showScreenEditor && !settings.isLocked) {
            LiquidGlassBall(
                isDimMode = isDimMode,
                isLocked = settings.isLocked,
                showAmoledInMenu = settings.showAmoledInMenu,
                presetIndex = settings.currentPresetIndex,
                totalPresets = settings.themePresets.size,
                ballSizeDp = settings.ballSizeDp,
                positionXFraction = settings.ballPositionX,
                positionYFraction = settings.ballPositionY,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                onSingleClick = {
                    executeBallAction(settings.ballSingleTapAction)
                },
                onDoubleClick = {
                    executeBallAction(settings.ballDoubleTapAction)
                },
                onHoldAction = {
                    if (settings.ballHoldAction != BallAction.HOLD_AND_DRAG_MENU) {
                        executeBallAction(settings.ballHoldAction)
                    }
                },
                onMenuAction = { action ->
                    touchpadEngine.hapticEngine.playClick(settings.hapticIntensity)
                    when (action) {
                        LiquidMenuAction.LOCK -> {
                            touchpadEngine.updateSettings(settings.copy(isLocked = true))
                            showToast("Locked • Touchpad active • Press Vol to unlock", Icons.Default.Lock)
                        }
                        LiquidMenuAction.AMOLED_MODE -> {
                            isDimMode = !isDimMode
                            onDimModeChanged(isDimMode)
                            touchpadEngine.hapticEngine.playModeTransition(settings.hapticIntensity)
                            showToast(if (isDimMode) "Amoled Mode Active" else "Amoled Mode Disabled", if (isDimMode) Icons.Default.Brightness2 else Icons.Default.DarkMode)
                        }
                        LiquidMenuAction.SETTINGS -> {
                            hidManager.refreshPairedDevices()
                            showSettingsSheet = true
                        }
                        LiquidMenuAction.THEME_CYCLE -> {
                            cycleNextThemePreset()
                        }
                    }
                },
                onTouchDown = {
                    touchpadEngine.hapticEngine.playTouchDown(settings.hapticIntensity)
                },
                onHoverItemChanged = {
                    touchpadEngine.hapticEngine.playClick(settings.hapticIntensity)
                }
            )
        }

        // Layer 7: Freeform Screen Editor Overlay
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
                onOpenScreenEditor = {
                    showScreenEditor = true
                },
                onPickCustomImage = {
                    imagePickerLauncher.launch("image/*")
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
