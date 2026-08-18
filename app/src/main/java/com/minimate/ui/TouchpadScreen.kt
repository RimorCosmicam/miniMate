package com.minimate.ui

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
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
import com.minimate.touchpad.model.ThemeVariant
import com.minimate.ui.components.FingerEffectsLayer
import com.minimate.ui.components.FloatingInteractionBall
import com.minimate.ui.components.HudToast
import com.minimate.ui.components.PermissionPrompt
import com.minimate.ui.components.RadialAction
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

    fun startPairing() {
        try {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            showToast("Pairing Mode • Connect on Host", Icons.Default.Bluetooth)
        } catch (_: Exception) {
            showToast("Bluetooth Discovery Triggered")
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
        // 1. Interactive GPU Background Shader with 10 Themes and 3 Subthemes
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

        // 3. Fullscreen Touch Surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { motionEvent ->
                    touchpadEngine.onTouchEvent(motionEvent)
                }
        )

        // 4. Stealth Dim Overlay
        if (dimRatio > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimRatio * 0.98f))
            )
        }

        // 5. Floating Interaction Ball (Hold & Slide Radial Action Ring)
        FloatingInteractionBall(
            isDimMode = isDimMode,
            isLocked = settings.isLocked,
            onTapShortcut = {
                touchpadEngine.hapticEngine.playClick(settings.hapticIntensity)
                when (settings.buttonPressAction) {
                    ButtonPressAction.STEALTH_DIM -> {
                        isDimMode = !isDimMode
                        onDimModeChanged(isDimMode)
                        touchpadEngine.hapticEngine.playModeTransition(settings.hapticIntensity)
                    }
                    ButtonPressAction.OPEN_SETTINGS -> {
                        hidManager.refreshPairedDevices()
                        showSettingsSheet = true
                    }
                    ButtonPressAction.PAIRING_MODE -> {
                        startPairing()
                    }
                    ButtonPressAction.MIDDLE_CLICK -> {
                        scope.launch {
                            hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_MIDDLE, dx = 0, dy = 0)
                            delay(16)
                            hidManager.sendMouseInput(buttons = HidDescriptor.BUTTON_NONE, dx = 0, dy = 0)
                        }
                    }
                }
            },
            onActionSelected = { action ->
                touchpadEngine.hapticEngine.playClick(settings.hapticIntensity)
                when (action) {
                    RadialAction.PAIRING -> {
                        startPairing()
                    }
                    RadialAction.SETTINGS -> {
                        hidManager.refreshPairedDevices()
                        showSettingsSheet = true
                    }
                    RadialAction.THEMES -> {
                        val themes = BackgroundTheme.values().filter { it != BackgroundTheme.CUSTOM_IMAGE }
                        val nextIndex = (themes.indexOf(settings.backgroundTheme) + 1) % themes.size
                        val nextTheme = themes[nextIndex]
                        touchpadEngine.updateSettings(
                            settings.copy(
                                backgroundTheme = nextTheme,
                                customImageUri = null
                            )
                        )
                        showToast("Theme: ${nextTheme.displayName}", Icons.Default.Palette)
                    }
                    RadialAction.LOCK -> {
                        touchpadEngine.updateSettings(settings.copy(isLocked = true))
                        showToast("Locked • Press Vol+ & Vol- to Unlock", Icons.Default.Lock)
                    }
                    RadialAction.NONE -> Unit
                }
            },
            onTouchDown = {
                touchpadEngine.hapticEngine.playTouchDown(settings.hapticIntensity)
            },
            onHapticTick = {
                touchpadEngine.hapticEngine.playClick(HapticIntensity.SUBTLE)
            },
            modifier = Modifier.align(Alignment.BottomStart)
        )

        // 6. Settings & Theme Manager Modal Sheet
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
                    startPairing()
                },
                onRefreshDevices = {
                    hidManager.refreshPairedDevices()
                },
                onDismiss = { showSettingsSheet = false }
            )
        }

        // 7. Minimal HUD Toast Feedback
        HudToast(
            message = hudMessage,
            icon = hudIcon,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
        )

        // 8. Bluetooth Permission Prompt if needed
        if (bluetoothState.status == ConnectionStatus.NO_PERMISSION) {
            PermissionPrompt(
                onRequestPermission = onRequestPermissions
            )
        }
    }
}
