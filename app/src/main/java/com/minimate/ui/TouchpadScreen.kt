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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.minimate.audio.LocalListen
import com.minimate.bluetooth.BluetoothAudioBridge
import com.minimate.bluetooth.BluetoothHidManager
import com.minimate.bluetooth.WebcamCapture
import com.minimate.bluetooth.BluetoothUiState
import com.minimate.bluetooth.ConnectionStatus
import com.minimate.bluetooth.HidDescriptor
import com.minimate.touchpad.engine.TouchpadEngine
import com.minimate.touchpad.model.BallAction
import com.minimate.touchpad.model.AudioDeviceEqProfile
import com.minimate.touchpad.model.AudioOutputPreset
import com.minimate.touchpad.model.HapticIntensity
import com.minimate.touchpad.model.SUPERHUMAN_BAND_HZ
import com.minimate.touchpad.model.TouchpadSettings
import com.minimate.touchpad.model.validColorway
import com.minimate.ui.components.AudioModeOverlay
import com.minimate.ui.components.BluetoothPairingDialog
import com.minimate.ui.components.BluetoothKeyboardOverlay
import com.minimate.ui.components.ClockBatteryOverlay
import com.minimate.ui.components.EdgeControlsOverlay
import com.minimate.ui.components.EdgeControlsEditorOverlay
import com.minimate.ui.components.EdgeRefractionSurface
import com.minimate.ui.components.HudToast
import com.minimate.ui.components.LiquidGlassAnalogStick
import com.minimate.ui.components.LiveCalibrationMode
import com.minimate.ui.components.LiveCalibrationOverlay
import com.minimate.ui.components.PermissionPrompt
import com.minimate.ui.components.ScreenEditorOverlay
import com.minimate.ui.components.SettingsSheet
import com.minimate.ui.components.ThemeTesterOverlay
import com.minimate.ui.components.WebcamModeOverlay
import com.minimate.ui.shader.BackgroundShaderCanvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TouchpadScreen(
    touchpadEngine: TouchpadEngine,
    hidManager: BluetoothHidManager,
    audioBridge: BluetoothAudioBridge,
    webcamCapture: WebcamCapture,
    bluetoothState: BluetoothUiState,
    batteryPercentage: Int,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by touchpadEngine.settings.collectAsState()
    val audioState by audioBridge.state.collectAsState()
    val webcamState by webcamCapture.state.collectAsState()
    val shaderTouchPoints by touchpadEngine.shaderTouchPoints.collectAsState()
    var isDimMode by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showPairingDialog by remember { mutableStateOf(false) }
    var showScreenEditor by remember { mutableStateOf(false) }
    var showThemeTester by remember { mutableStateOf(false) }
    var showKeyboard by remember { mutableStateOf(false) }
    var showAudio by remember { mutableStateOf(false) }
    var showWebcam by remember { mutableStateOf(false) }
    var showKeyboardThemeEditor by remember { mutableStateOf(false) }
    var showEdgeThemeEditor by remember { mutableStateOf(false) }
    var liveCalibrationMode by remember { mutableStateOf<LiveCalibrationMode?>(null) }
    var themeTesterOriginal by remember { mutableStateOf<TouchpadSettings?>(null) }
    var keyboardThemeEditorOriginal by remember { mutableStateOf<TouchpadSettings?>(null) }
    var edgeThemeEditorOriginal by remember { mutableStateOf<TouchpadSettings?>(null) }
    var hudMessage by remember { mutableStateOf<String?>(null) }
    var hudIcon by remember { mutableStateOf<ImageVector?>(null) }
    
    var screenWidthPx by remember { mutableFloatStateOf(1080f) }
    var screenHeightPx by remember { mutableFloatStateOf(1080f) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(
        settings.audioOutputEnabled,
        settings.audioMicrophoneEnabled,
        settings.audioOutputVolume,
        settings.audioOutputDeviceKey,
        settings.audioDeviceEqProfiles,
        settings.audioMicrophoneGain,
        settings.audioInputDeviceKey,
        settings.audioMicrophonePreset,
        settings.audioSuperhumanBands
    ) {
        audioBridge.configure(
            settings.audioOutputEnabled,
            settings.audioMicrophoneEnabled,
            settings.audioOutputVolume,
            settings.audioOutputDeviceKey,
            settings.audioDeviceEqProfiles,
            settings.audioMicrophoneGain,
            settings.audioInputDeviceKey,
            settings.audioMicrophonePreset,
            settings.audioSuperhumanBands
        )
    }

    // On-device listening runs independently of any desktop link: the tools are instruments to
    // be heard immediately in the phone's own earphones, so they must work with nothing paired.
    val localListen = remember { LocalListen(context) }
    var listening by remember { mutableStateOf(false) }
    LaunchedEffect(
        listening,
        settings.audioMicrophonePreset,
        settings.audioSuperhumanBands,
        settings.audioMicrophoneGain,
        settings.audioOutputDeviceKey
    ) {
        localListen.preset = settings.audioMicrophonePreset
        localListen.bands = settings.audioSuperhumanBands
        localListen.gain = settings.audioMicrophoneGain
        localListen.outputDeviceKey = settings.audioOutputDeviceKey.takeIf { it != "phone" }
        if (listening && !localListen.isRunning) {
            if (!localListen.start()) listening = false
        } else if (listening) {
            // Preset or routing changed while running: restart so the new capture profile and
            // output device actually take effect.
            localListen.stop()
            localListen.start()
        } else {
            localListen.stop()
        }
    }
    DisposableEffect(Unit) { onDispose { localListen.stop() } }

    LaunchedEffect(
        settings.webcamEnabled,
        settings.webcamResolution,
        settings.webcamFps
    ) {
        if (settings.webcamEnabled) {
            webcamCapture.start(
                settings.webcamResolution,
                settings.webcamFps
            )
        } else webcamCapture.stop()
    }

    LaunchedEffect(
        settings.webcamZoom,
        settings.webcamExposure,
        settings.webcamFlashEnabled,
        settings.webcamFlashIntensity
    ) {
        webcamCapture.updateControls(
            settings.webcamZoom,
            settings.webcamExposure,
            settings.webcamFlashEnabled,
            settings.webcamFlashIntensity
        )
    }

    LaunchedEffect(
        settings.webcamEnabled,
        settings.webcamMirror,
        settings.webcamFilterIntensity,
        settings.webcamFilters,
        audioState.connected
    ) {
        audioBridge.sendWebcamConfiguration(
            settings.webcamEnabled,
            settings.webcamMirror,
            settings.webcamFilterIntensity,
            settings.webcamFilters
        )
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
        EdgeRefractionSurface(
            railEnabled = !showKeyboard && !showAudio && !showWebcam && (settings.edgeScrollEnabled || showEdgeThemeEditor),
            cornerEnabled = !showKeyboard && !showAudio && !showWebcam && (settings.edgeRightClickEnabled || showEdgeThemeEditor),
            railSide = settings.edgeControlSide,
            railScale = settings.edgeRailScale,
            cornerScale = settings.edgeCornerScale,
            railMaterial = settings.edgeRailMaterial,
            cornerMaterial = settings.edgeCornerMaterial,
            modifier = Modifier.fillMaxSize()
        ) {
            BackgroundShaderCanvas(
                theme = settings.backgroundTheme,
                variantIndex = settings.themeVariantIndex,
                touchPoints = if (settings.fingerEffectsEnabled) shaderTouchPoints else emptyList(),
                customImageUri = settings.customImageUri,
                dimRatio = dimRatio,
                animationSpeed = settings.backgroundAnimation.speed,
                themeFilters = settings.themeFilters,
                shaderTheme = settings.abstractShaderTheme,
                shaderSubthemeIndex = settings.abstractSubthemeIndex,
                shaderRecolor = validColorway(settings.abstractShaderTheme, settings.abstractSubthemeIndex, settings.shaderRecolor),
                customShaderColors = settings.customShaderColors,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Layer 2: Fullscreen Trackpad Touch Surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { motionEvent ->
                    if (!showScreenEditor && !showKeyboard && !showAudio && !showWebcam) {
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
                    .background(Color.Black.copy(alpha = dimRatio))
            )
        }

        // Mirrored single-hand edge controls sit above the touchpad but below the HUD/stick.
        // Their narrow hit regions consume edge gestures so cursor motion never leaks through.
        if (!isDimMode && !showKeyboard && !showAudio && !showWebcam && !showSettingsSheet && !showPairingDialog && !showScreenEditor &&
            !showThemeTester && liveCalibrationMode == null
        ) {
            EdgeControlsOverlay(
                railEnabled = settings.edgeScrollEnabled || showEdgeThemeEditor,
                rightClickEnabled = settings.edgeRightClickEnabled || showEdgeThemeEditor,
                railSide = settings.edgeControlSide,
                railScale = settings.edgeRailScale,
                cornerScale = settings.edgeCornerScale,
                scrollSpeed = settings.scrollSpeed,
                naturalScrolling = settings.naturalScrolling,
                onScroll = { wheel ->
                    if (!showEdgeThemeEditor) {
                        hidManager.sendMouseInput(
                            buttons = HidDescriptor.BUTTON_NONE,
                            dx = 0,
                            dy = 0,
                            wheel = wheel,
                            pan = 0
                        )
                    }
                },
                onRightClick = { if (!showEdgeThemeEditor) executeStickAction(BallAction.RIGHT_CLICK) },
                onRailTouchDown = {
                    touchpadEngine.hapticEngine.playTouchDown(settings.hapticIntensity)
                }
            )
        }

        if (showKeyboard) {
            BluetoothKeyboardOverlay(
                connected = bluetoothState.status == ConnectionStatus.CONNECTED,
                amoledMode = isDimMode,
                shortcuts = settings.keyboardShortcuts,
                theme = settings.keyboardTheme,
                onThemeChange = { theme ->
                    touchpadEngine.updateSettings(settings.copy(keyboardTheme = theme))
                },
                language = settings.keyboardLanguage,
                onLanguageChange = { language ->
                    touchpadEngine.updateSettings(settings.copy(keyboardLanguage = language))
                },
                trail = settings.keyboardTrail,
                onTrailChange = { trail ->
                    touchpadEngine.updateSettings(settings.copy(keyboardTrail = trail))
                },
                font = settings.keyboardFont,
                onFontChange = { font ->
                    touchpadEngine.updateSettings(settings.copy(keyboardFont = font))
                },
                fontWeight = settings.keyboardFontWeight,
                onFontWeightChange = { weight ->
                    touchpadEngine.updateSettings(settings.copy(keyboardFontWeight = weight))
                },
                opaque = settings.keyboardOpaque,
                onOpaqueChange = { opaque ->
                    touchpadEngine.updateSettings(settings.copy(keyboardOpaque = opaque))
                },
                keyboardScale = settings.keyboardScale,
                onKeyboardScaleChange = { scale ->
                    touchpadEngine.updateSettings(settings.copy(keyboardScale = scale))
                },
                editorMode = showKeyboardThemeEditor,
                onEditorCancel = {
                    keyboardThemeEditorOriginal?.let(touchpadEngine::updateSettings)
                    keyboardThemeEditorOriginal = null
                    showKeyboardThemeEditor = false
                },
                onEditorDone = {
                    keyboardThemeEditorOriginal = null
                    showKeyboardThemeEditor = false
                },
                onShortcutsChange = { shortcuts ->
                    touchpadEngine.updateSettings(settings.copy(keyboardShortcuts = shortcuts))
                },
                onKeyStroke = { modifiers, usage ->
                    scope.launch {
                        hidManager.sendKeyboardInput(modifiers, usage)
                        delay(18)
                        hidManager.sendKeyboardInput()
                    }
                },
                onConsumerControl = { usage ->
                    scope.launch {
                        hidManager.sendConsumerInput(usage)
                        delay(28)
                        hidManager.sendConsumerInput(0)
                    }
                },
                onText = { text ->
                    scope.launch {
                        text.forEach { character ->
                            val shift = if (character.isUpperCase()) 0x02 else 0x00
                            val strokes: List<Pair<Byte, Byte>> = when (val lower = character.lowercaseChar()) {
                                in 'a'..'z' -> listOf(shift.toByte() to (0x04 + (lower - 'a')).toByte())
                                ' ' -> listOf(0x00.toByte() to 0x2C.toByte())
                                'á', 'é', 'í', 'ó', 'ú' -> listOf(
                                    0x04.toByte() to 0x08.toByte(),
                                    shift.toByte() to mapOf('á' to 0x04, 'é' to 0x08, 'í' to 0x0C, 'ó' to 0x12, 'ú' to 0x18).getValue(lower).toByte()
                                )
                                'à' -> listOf(0x04.toByte() to 0x35.toByte(), shift.toByte() to 0x04.toByte())
                                'â', 'ê', 'ô' -> {
                                    val base = mapOf('â' to 0x04, 'ê' to 0x08, 'ô' to 0x12).getValue(lower)
                                    listOf(0x04.toByte() to 0x0C.toByte(), shift.toByte() to base.toByte())
                                }
                                'ã', 'õ' -> {
                                    val base = if (lower == 'ã') 0x04 else 0x12
                                    listOf(0x04.toByte() to 0x11.toByte(), shift.toByte() to base.toByte())
                                }
                                'ü' -> listOf(0x04.toByte() to 0x18.toByte(), shift.toByte() to 0x18.toByte())
                                'ç' -> listOf((0x04 or shift).toByte() to 0x06.toByte())
                                else -> emptyList()
                            }
                            strokes.forEach { (modifiers, usage) ->
                                hidManager.sendKeyboardInput(modifiers, usage)
                                delay(12)
                                hidManager.sendKeyboardInput()
                                delay(8)
                            }
                        }
                    }
                },
                onHaptic = {
                    touchpadEngine.hapticEngine.playClick(settings.hapticIntensity)
                }
            )
        }

        if (showAudio) {
            AudioModeOverlay(
                state = audioState,
                onOutputEnabled = { enabled ->
                    touchpadEngine.updateSettings(settings.copy(audioOutputEnabled = enabled))
                },
                onOutputDeviceSelected = { key ->
                    touchpadEngine.updateSettings(settings.copy(audioOutputDeviceKey = key))
                },
                onMicrophoneEnabled = { enabled ->
                    touchpadEngine.updateSettings(settings.copy(audioMicrophoneEnabled = enabled))
                },
                onInputDeviceSelected = { key ->
                    touchpadEngine.updateSettings(settings.copy(audioInputDeviceKey = key))
                },
                onOutputVolume = { volume ->
                    touchpadEngine.updateSettings(settings.copy(audioOutputVolume = volume))
                },
                onOutputPreset = { preset ->
                    val profile = AudioDeviceEqProfile(
                        deviceKey = audioState.outputDeviceKey,
                        deviceName = audioState.outputDeviceName,
                        preset = preset,
                        gains = preset.gains
                    )
                    touchpadEngine.updateSettings(
                        settings.copy(audioDeviceEqProfiles = settings.audioDeviceEqProfiles.filterNot {
                            it.deviceKey == profile.deviceKey
                        } + profile)
                    )
                },
                onOutputEqBand = { band, gain ->
                    val gains = (settings.audioDeviceEqProfiles.firstOrNull {
                        it.deviceKey == audioState.outputDeviceKey
                    }?.gains ?: audioState.outputEqGains).toMutableList()
                    while (gains.size < 9) gains += 0f
                    gains[band.coerceIn(0, 8)] = gain
                    val profile = AudioDeviceEqProfile(
                        deviceKey = audioState.outputDeviceKey,
                        deviceName = audioState.outputDeviceName,
                        preset = AudioOutputPreset.CUSTOM,
                        gains = gains.take(9)
                    )
                    touchpadEngine.updateSettings(
                        settings.copy(audioDeviceEqProfiles = settings.audioDeviceEqProfiles.filterNot {
                            it.deviceKey == profile.deviceKey
                        } + profile)
                    )
                },
                onMicrophoneGain = { gain ->
                    touchpadEngine.updateSettings(settings.copy(audioMicrophoneGain = gain))
                },
                onMicrophonePreset = { preset ->
                    touchpadEngine.updateSettings(settings.copy(audioMicrophonePreset = preset))
                },
                onSuperhumanBand = { index, value ->
                    val next = settings.audioSuperhumanBands.toMutableList()
                    while (next.size < SUPERHUMAN_BAND_HZ.size) next += 0f
                    next[index.coerceIn(0, next.lastIndex)] = value.coerceIn(-18f, 18f)
                    touchpadEngine.updateSettings(settings.copy(audioSuperhumanBands = next))
                },
                onListenToggled = { listening = it },
                microphonePreset = settings.audioMicrophonePreset,
                superhumanBands = settings.audioSuperhumanBands,
                listening = listening
            )
        }

        if (showWebcam) {
            WebcamModeOverlay(
                linkState = audioState,
                captureState = webcamState,
                enabled = settings.webcamEnabled,
                resolution = settings.webcamResolution,
                fps = settings.webcamFps,
                mirror = settings.webcamMirror,
                zoom = settings.webcamZoom,
                exposure = settings.webcamExposure,
                flashEnabled = settings.webcamFlashEnabled,
                flashIntensity = settings.webcamFlashIntensity,
                intensity = settings.webcamFilterIntensity,
                filters = settings.webcamFilters,
                onEnabled = { touchpadEngine.updateSettings(settings.copy(webcamEnabled = it)) },
                onResolution = { touchpadEngine.updateSettings(settings.copy(webcamResolution = it)) },
                onFps = { touchpadEngine.updateSettings(settings.copy(webcamFps = it)) },
                onMirror = { touchpadEngine.updateSettings(settings.copy(webcamMirror = it)) },
                onZoom = { touchpadEngine.updateSettings(settings.copy(webcamZoom = it)) },
                onExposure = { touchpadEngine.updateSettings(settings.copy(webcamExposure = it)) },
                onFlashEnabled = { touchpadEngine.updateSettings(settings.copy(webcamFlashEnabled = it)) },
                onFlashIntensity = { touchpadEngine.updateSettings(settings.copy(webcamFlashIntensity = it)) },
                onIntensity = { touchpadEngine.updateSettings(settings.copy(webcamFilterIntensity = it)) },
                onToggleFilter = { filter ->
                    val next = if (filter in settings.webcamFilters) settings.webcamFilters - filter else settings.webcamFilters + filter
                    touchpadEngine.updateSettings(settings.copy(webcamFilters = next))
                }
            )
        }

        // Layer 6: Pure Liquid Glass 2D Analog Stick (Single-Hand Scroll / Click Mastery)
        if (!isDimMode && !showKeyboard && !showAudio && !showWebcam && !showThemeTester && !showScreenEditor && !showEdgeThemeEditor && !settings.isLocked && settings.stickEnabled) {
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
                    showWebcam = false
                    showThemeTester = true
                },
                onOpenKeyboardThemeEditor = {
                    keyboardThemeEditorOriginal = settings
                    showEdgeThemeEditor = false
                    showAudio = false
                    showWebcam = false
                    showKeyboard = true
                    showKeyboardThemeEditor = true
                },
                onOpenEdgeThemeEditor = {
                    edgeThemeEditorOriginal = settings
                    showAudio = false
                    showWebcam = false
                    showKeyboard = false
                    showKeyboardThemeEditor = false
                    showEdgeThemeEditor = true
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

        if (showEdgeThemeEditor) {
            EdgeControlsEditorOverlay(
                settings = settings,
                onSettingsChange = touchpadEngine::updateSettings,
                onCancel = {
                    edgeThemeEditorOriginal?.let(touchpadEngine::updateSettings)
                    edgeThemeEditorOriginal = null
                    showEdgeThemeEditor = false
                },
                onDone = {
                    edgeThemeEditorOriginal = null
                    showEdgeThemeEditor = false
                }
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

        // One pill, one gesture contract in every primary mode and editor:
        // tap advances Trackpad -> Keyboard -> Audio -> Webcam -> Trackpad,
        // double-tap toggles true-black AMOLED rendering, and hold opens Settings.
        ClockBatteryOverlay(
            clockStyle = if (isDimMode || showKeyboard || showAudio || showWebcam || showThemeTester) {
                com.minimate.touchpad.model.ClockStyle.MINIMAL_PILL
            } else settings.clockStyle,
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
            amoledMode = isDimMode,
            onTap = {
                touchpadEngine.hapticEngine.playModeTransition(settings.hapticIntensity)
                when {
                    showWebcam -> {
                        showWebcam = false
                    }
                    showAudio -> {
                        showAudio = false
                        showWebcam = true
                    }
                    showKeyboard -> {
                        hidManager.sendKeyboardInput()
                        showKeyboard = false
                        showKeyboardThemeEditor = false
                        keyboardThemeEditorOriginal = null
                        showAudio = true
                    }
                    else -> {
                        showEdgeThemeEditor = false
                        edgeThemeEditorOriginal = null
                        showKeyboard = true
                    }
                }
            },
            onDoubleTap = { executeStickAction(BallAction.AMOLED_DIM) },
            onLongPress = {
                touchpadEngine.hapticEngine.playModeTransition(settings.hapticIntensity)
                hidManager.refreshPairedDevices()
                showSettingsSheet = true
            }
        )

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
