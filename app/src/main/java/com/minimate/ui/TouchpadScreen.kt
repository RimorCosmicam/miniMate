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
import com.minimate.touchpad.model.ButtonPressAction
import com.minimate.touchpad.model.HapticIntensity
import com.minimate.ui.components.BluetoothPairingDialog
import com.minimate.ui.components.ClockBatteryOverlay
import com.minimate.ui.components.FingerEffectsLayer
import com.minimate.ui.components.HudToast
import com.minimate.ui.components.LiquidGlassBall
import com.minimate.ui.components.LiquidMenuAction
import com.minimate.ui.components.PermissionPrompt
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
    var hudMessage by remember { mutableStateOf<String?>(null) }
    var hudIcon by remember { mutableStateOf<ImageVector?>(null) }
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
            hudMessage = "Custom Background Loaded"
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
                    themeVariant = nextPreset.variant,
                    customImageUri = nextPreset.customUri,
                    currentPresetIndex = nextIndex
                )
            )
            touchpadEngine.hapticEngine.playModeTransition(settings.hapticIntensity)
            showToast("Preset ${nextIndex + 1}/${presets.size}: ${nextPreset.theme.displayName}", Icons.Default.Palette)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { size ->
                touchpadEngine.setScreenDimensions(size.width.toFloat(), size.height.toFloat())
            }
    ) {
        // 1. Interactive GPU Background Shader with 10 Procedural Themes
        BackgroundShaderCanvas(
            theme = settings.backgroundTheme,
            variant = settings.themeVariant,
            touchPoints = activeTouchPoints,
            customImageUri = settings.customImageUri,
            dimRatio = dimRatio,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Multi-Touch Finger Effects Layer (10 Effects)
        FingerEffectsLayer(
            touchPoints = activeTouchPoints,
            effect = settings.fingerEffect,
            enabled = settings.fingerEffectsEnabled && dimRatio < 0.8f,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Customizable Clock & Battery HUD Widget
        ClockBatteryOverlay(
            clockStyle = settings.clockStyle,
            clockPosition = settings.clockPosition,
            show24Hour = settings.show24HourFormat,
            showSeconds = settings.showSeconds,
            showBattery = settings.showBatteryPercentage,
            batteryPercentage = batteryPercentage,
            bluetoothState = bluetoothState,
            dimRatio = dimRatio
        )

        // 4. Fullscreen Touch Surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { motionEvent ->
                    touchpadEngine.onTouchEvent(motionEvent)
                }
        )

        // 5. Stealth Dim Overlay (Amoled Mode)
        if (dimRatio > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimRatio * 0.98f))
            )
        }

        // 6. Liquid Glass UI Element (Fluid Morphing into Lock, Amoled, Settings, Theme Cycler)
        LiquidGlassBall(
            isDimMode = isDimMode,
            isLocked = settings.isLocked,
            presetIndex = settings.currentPresetIndex,
            totalPresets = settings.themePresets.size,
            onTap = {
                touchpadEngine.hapticEngine.playClick(settings.hapticIntensity)
                when (settings.buttonPressAction) {
                    ButtonPressAction.LIQUID_WOBBLE -> {
                        touchpadEngine.hapticEngine.playModeTransition(settings.hapticIntensity)
                    }
                    ButtonPressAction.MIDDLE_CLICK -> {
                        scope.launch {
                            hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_MIDDLE, dx = 0, dy = 0)
                            delay(16)
                            hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_NONE, dx = 0, dy = 0)
                        }
                    }
                    ButtonPressAction.RIGHT_CLICK -> {
                        scope.launch {
                            hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_RIGHT, dx = 0, dy = 0)
                            delay(16)
                            hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_NONE, dx = 0, dy = 0)
                        }
                    }
                    ButtonPressAction.BACK_BUTTON -> {
                        scope.launch {
                            hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_BACK, dx = 0, dy = 0)
                            delay(16)
                            hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_NONE, dx = 0, dy = 0)
                        }
                    }
                    ButtonPressAction.FORWARD_BUTTON -> {
                        scope.launch {
                            hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_FORWARD, dx = 0, dy = 0)
                            delay(16)
                            hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_NONE, dx = 0, dy = 0)
                        }
                    }
                    ButtonPressAction.CYCLE_THEME -> {
                        cycleNextThemePreset()
                    }
                    ButtonPressAction.AMOLED_DIM -> {
                        isDimMode = !isDimMode
                        onDimModeChanged(isDimMode)
                        touchpadEngine.hapticEngine.playModeTransition(settings.hapticIntensity)
                    }
                    ButtonPressAction.OPEN_SETTINGS -> {
                        hidManager.refreshPairedDevices()
                        showSettingsSheet = true
                    }
                    ButtonPressAction.PAIRING_MODE -> {
                        hidManager.refreshPairedDevices()
                        showPairingDialog = true
                    }
                }
            },
            onMenuAction = { action ->
                touchpadEngine.hapticEngine.playClick(settings.hapticIntensity)
                when (action) {
                    LiquidMenuAction.LOCK -> {
                        touchpadEngine.updateSettings(settings.copy(isLocked = true))
                        showToast("Locked • Press Vol+ & Vol- to Unlock", Icons.Default.Lock)
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
            modifier = Modifier.align(Alignment.BottomStart)
        )

        // 7. Settings & Theme Manager Modal Sheet (3 Tabs: Settings, Theme, Finger Shader)
        if (showSettingsSheet) {
            SettingsSheet(
                settings = settings,
                bluetoothState = bluetoothState,
                batteryPercentage = batteryPercentage,
                onSettingsChange = { newSettings ->
                    touchpadEngine.updateSettings(newSettings)
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

        // 8. Dedicated In-App Bluetooth Pairing Hub Dialog
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

        // 9. Minimal HUD Toast Feedback
        HudToast(
            message = hudMessage,
            icon = hudIcon,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
        )

        // 10. Bluetooth Permission Prompt if needed
        if (bluetoothState.status == ConnectionStatus.NO_PERMISSION) {
            PermissionPrompt(
                onRequestPermission = onRequestPermissions
            )
        }
    }
}
