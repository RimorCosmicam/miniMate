package com.minimate.touchpad.model

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Robust persistent storage for TouchpadSettings using Android SharedPreferences.
 * Preserves all user configurations, themes, analog stick settings, and layout coordinates across launches.
 */
class TouchpadPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("minimate_touchpad_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SETTINGS_JSON = "saved_touchpad_settings_json"
    }

    fun saveSettings(settings: TouchpadSettings) {
        try {
            val json = JSONObject().apply {
                put("trackingSpeed", settings.trackingSpeed.toDouble())
                put("acceleration", settings.acceleration.toDouble())
                put("scrollSpeed", settings.scrollSpeed.toDouble())
                put("naturalScrolling", settings.naturalScrolling)
                put("invertCursorY", settings.invertCursorY)
                put("momentumScrolling", settings.momentumScrolling)
                put("momentumFriction", settings.momentumFriction.toDouble())
                put("tapToClick", settings.tapToClick)
                put("twoFingerRightClick", settings.twoFingerRightClick)
                put("doubleTapDrag", settings.doubleTapDrag)
                put("dragReleaseDelayMs", settings.dragReleaseDelayMs)
                put("edgeMarginDp", settings.edgeMarginDp.toDouble())
                put("hapticIntensity", settings.hapticIntensity.name)

                // Active theme
                put("backgroundTheme", settings.backgroundTheme.name)
                put("themeVariantIndex", settings.themeVariantIndex)
                put("backgroundAnimation", settings.backgroundAnimation.name)
                put("themeFilters", JSONArray(settings.themeFilters.map { it.name }))
                put("abstractShaderTheme", settings.abstractShaderTheme.name)
                put("abstractSubthemeIndex", settings.abstractSubthemeIndex)
                put("shaderRecolor", settings.shaderRecolor.name)
                put("customShaderColors", JSONArray(settings.customShaderColors))
                put("customImageUri", settings.customImageUri ?: "")

                // Finger FX
                put("fingerEffectsEnabled", settings.fingerEffectsEnabled)

                // Analog Stick (Single Hand Mode)
                put("analogStickMode", settings.analogStickMode.name)
                put("stickEnabled", settings.stickEnabled)
                put("stickTheme", settings.stickTheme.name)
                put("stickSingleTapAction", settings.stickSingleTapAction.name)
                put("stickDoubleTapAction", settings.stickDoubleTapAction.name)
                put("stickHoldAction", settings.stickHoldAction.name)
                put("stickScrollSensitivity", settings.stickScrollSensitivity.toDouble())
                put("stickDeadzone", settings.stickDeadzone.toDouble())
                put("edgeScrollEnabled", settings.edgeScrollEnabled)
                put("edgeRightClickEnabled", settings.edgeRightClickEnabled)
                put("edgeControlSide", settings.edgeControlSide.name)
                put("edgeRailMaterial", settings.edgeRailMaterial.name)
                put("edgeRailScale", settings.edgeRailScale.toDouble())
                put("edgeCornerMaterial", settings.edgeCornerMaterial.name)
                put("edgeCornerScale", settings.edgeCornerScale.toDouble())
                put("keyboardShortcuts", JSONArray().apply {
                    settings.keyboardShortcuts.forEach { shortcut ->
                        put(JSONObject().apply {
                            put("label", shortcut.label)
                            put("modifiers", shortcut.modifiers)
                            put("usage", shortcut.usage)
                        })
                    }
                })
                put("keyboardTheme", settings.keyboardTheme.name)
                put("keyboardLanguage", settings.keyboardLanguage.name)
                put("keyboardTrail", settings.keyboardTrail.name)
                put("keyboardFont", settings.keyboardFont.name)
                put("keyboardFontWeight", settings.keyboardFontWeight.name)
                put("keyboardOpaque", settings.keyboardOpaque)
                put("audioOutputEnabled", settings.audioOutputEnabled)
                put("audioMicrophoneEnabled", settings.audioMicrophoneEnabled)
                put("audioOutputVolume", settings.audioOutputVolume.toDouble())
                put("audioOutputRoute", settings.audioOutputRoute.name)
                put("audioDeviceEqProfiles", JSONArray().apply {
                    settings.audioDeviceEqProfiles.forEach { profile ->
                        put(JSONObject().apply {
                            put("deviceKey", profile.deviceKey)
                            put("deviceName", profile.deviceName)
                            put("preset", profile.preset.name)
                            put("gains", JSONArray(profile.gains))
                        })
                    }
                })
                put("audioMicrophoneGain", settings.audioMicrophoneGain.toDouble())
                put("audioInputRoute", settings.audioInputRoute.name)
                put("audioVoiceIsolation", settings.audioVoiceIsolation)
                put("audioMicrophoneNoiseGate", settings.audioMicrophoneNoiseGate.toDouble())
                put("audioMicrophonePreset", settings.audioMicrophonePreset.name)
                put("audioTransport", settings.audioTransport.name)
                put("webcamEnabled", settings.webcamEnabled)
                put("webcamLens", settings.webcamLens.name)
                put("webcamResolution", settings.webcamResolution.name)
                put("webcamFps", settings.webcamFps)
                put("webcamMirror", settings.webcamMirror)
                put("webcamZoom", settings.webcamZoom.toDouble())
                put("webcamExposure", settings.webcamExposure.toDouble())
                put("webcamFilterIntensity", settings.webcamFilterIntensity.toDouble())
                put("webcamFilters", JSONArray(settings.webcamFilters.map { it.name }))

                // Screen Editor layout
                put("ballPositionX", settings.ballPositionX.toDouble())
                put("ballPositionY", settings.ballPositionY.toDouble())
                put("ballSizeDp", settings.ballSizeDp.toDouble())
                put("clockPositionX", settings.clockPositionX.toDouble())
                put("clockPositionY", settings.clockPositionY.toDouble())
                put("clockScale", settings.clockScale.toDouble())

                // Clock style
                put("clockStyle", settings.clockStyle.name)
                put("show24HourFormat", settings.show24HourFormat)
                put("showSeconds", settings.showSeconds)
                put("showBatteryPercentage", settings.showBatteryPercentage)
            }

            prefs.edit().putString(KEY_SETTINGS_JSON, json.toString()).apply()
        } catch (_: Exception) {}
    }

    fun loadSettings(): TouchpadSettings {
        val jsonStr = prefs.getString(KEY_SETTINGS_JSON, null) ?: return TouchpadSettings()
        return try {
            val json = JSONObject(jsonStr)

            TouchpadSettings(
                trackingSpeed = json.optDouble("trackingSpeed", 1.0).toFloat(),
                acceleration = json.optDouble("acceleration", 1.15).toFloat(),
                scrollSpeed = json.optDouble("scrollSpeed", 1.0).toFloat(),
                naturalScrolling = json.optBoolean("naturalScrolling", true),
                invertCursorY = json.optBoolean("invertCursorY", false),
                momentumScrolling = json.optBoolean("momentumScrolling", true),
                momentumFriction = json.optDouble("momentumFriction", 0.92).toFloat(),
                tapToClick = json.optBoolean("tapToClick", true),
                twoFingerRightClick = json.optBoolean("twoFingerRightClick", true),
                doubleTapDrag = json.optBoolean("doubleTapDrag", true),
                dragReleaseDelayMs = json.optLong("dragReleaseDelayMs", 250L),
                edgeMarginDp = json.optDouble("edgeMarginDp", 12.0).toFloat(),
                hapticIntensity = try {
                    HapticIntensity.valueOf(json.optString("hapticIntensity", "CRISP"))
                } catch (_: Exception) {
                    HapticIntensity.CRISP
                },
                backgroundTheme = try {
                    BackgroundTheme.valueOf(json.optString("backgroundTheme", "CHROME_FLUID"))
                } catch (_: Exception) {
                    BackgroundTheme.CHROME_FLUID
                }.takeIf {
                    it.ordinal <= BackgroundTheme.CUSTOM_IMAGE.ordinal || it.isScenery
                } ?: BackgroundTheme.CHROME_FLUID,
                themeVariantIndex = json.optInt("themeVariantIndex", 0),
                backgroundAnimation = try {
                    BackgroundAnimation.valueOf(json.optString("backgroundAnimation", "FLOW"))
                } catch (_: Exception) {
                    BackgroundAnimation.FLOW
                },
                themeFilters = json.optJSONArray("themeFilters")?.let { values ->
                    buildList {
                        for (index in 0 until values.length()) {
                            runCatching { ThemeFilter.valueOf(values.getString(index)) }.getOrNull()
                                ?.takeIf { it != ThemeFilter.NONE && it !in this }
                                ?.let(::add)
                        }
                    }
                } ?: runCatching { ThemeFilter.valueOf(json.optString("themeFilter", "NONE")) }
                    .getOrNull()?.takeIf { it != ThemeFilter.NONE }?.let(::listOf).orEmpty(),
                abstractShaderTheme = runCatching {
                    AbstractShaderTheme.valueOf(json.optString("abstractShaderTheme", "ARCADE"))
                }.getOrDefault(AbstractShaderTheme.ARCADE),
                abstractSubthemeIndex = json.optInt("abstractSubthemeIndex", 1).coerceIn(0, 9),
                shaderRecolor = runCatching {
                    ShaderRecolor.valueOf(json.optString("shaderRecolor", "AUTHORED"))
                }.getOrDefault(ShaderRecolor.AUTHORED),
                customShaderColors = json.optJSONArray("customShaderColors")?.let { values ->
                    List(values.length().coerceAtMost(4)) { values.optLong(it) }
                }?.takeIf { it.size == 4 } ?: DEFAULT_CUSTOM_SHADER_COLORS,
                customImageUri = json.optString("customImageUri", "").takeIf { it.isNotEmpty() },
                fingerEffectsEnabled = json.optBoolean("fingerEffectsEnabled", true),
                analogStickMode = try {
                    AnalogStickMode.valueOf(json.optString("analogStickMode", "ANALOG_SCROLL"))
                } catch (_: Exception) {
                    AnalogStickMode.ANALOG_SCROLL
                },
                stickEnabled = json.optBoolean("stickEnabled", true),
                stickTheme = try {
                    StickTheme.valueOf(json.optString("stickTheme", "PRECISION_DISC"))
                } catch (_: Exception) {
                    StickTheme.PRECISION_DISC
                },
                stickSingleTapAction = try {
                    BallAction.valueOf(json.optString("stickSingleTapAction", "MIDDLE_CLICK"))
                } catch (_: Exception) {
                    BallAction.MIDDLE_CLICK
                },
                stickDoubleTapAction = try {
                    BallAction.valueOf(json.optString("stickDoubleTapAction", "RIGHT_CLICK"))
                } catch (_: Exception) {
                    BallAction.RIGHT_CLICK
                },
                stickHoldAction = try {
                    BallAction.valueOf(json.optString("stickHoldAction", "AMOLED_DIM"))
                } catch (_: Exception) {
                    BallAction.AMOLED_DIM
                },
                stickScrollSensitivity = json.optDouble("stickScrollSensitivity", 1.0).toFloat(),
                stickDeadzone = json.optDouble("stickDeadzone", 0.10).toFloat(),
                edgeScrollEnabled = json.optBoolean("edgeScrollEnabled", true),
                edgeRightClickEnabled = json.optBoolean("edgeRightClickEnabled", true),
                edgeControlSide = runCatching {
                    EdgeControlSide.valueOf(json.optString("edgeControlSide", "LEFT"))
                }.getOrDefault(EdgeControlSide.LEFT),
                edgeRailMaterial = runCatching {
                    EdgeControlMaterial.valueOf(json.optString("edgeRailMaterial", "CLEAR_GLASS"))
                }.getOrDefault(EdgeControlMaterial.CLEAR_GLASS),
                edgeRailScale = json.optDouble(
                    "edgeRailScale",
                    runCatching { EdgeControlSize.valueOf(json.optString("edgeRailSize", "STANDARD")).scale.toDouble() }.getOrDefault(1.0)
                ).toFloat().coerceIn(.65f, 1.8f),
                edgeCornerMaterial = runCatching {
                    EdgeControlMaterial.valueOf(json.optString("edgeCornerMaterial", "CLEAR_GLASS"))
                }.getOrDefault(EdgeControlMaterial.CLEAR_GLASS),
                edgeCornerScale = json.optDouble(
                    "edgeCornerScale",
                    runCatching { EdgeControlSize.valueOf(json.optString("edgeCornerSize", "STANDARD")).scale.toDouble() }.getOrDefault(1.0)
                ).toFloat().coerceIn(.65f, 1.8f),
                keyboardShortcuts = json.optJSONArray("keyboardShortcuts")?.let { values ->
                    buildList {
                        for (index in 0 until values.length().coerceAtMost(8)) {
                            val item = values.optJSONObject(index) ?: continue
                            val label = item.optString("label").trim().take(24)
                            val modifiers = item.optInt("modifiers", -1)
                            val usage = item.optInt("usage", -1)
                            if (label.isNotEmpty() && modifiers in 0..0x0F && usage in 1..0xFF) {
                                add(KeyboardShortcut(label, modifiers, usage))
                            }
                        }
                    }
                } ?: DEFAULT_KEYBOARD_SHORTCUTS,
                keyboardTheme = runCatching {
                    KeyboardTheme.valueOf(json.optString("keyboardTheme", "GLASS"))
                }.getOrDefault(KeyboardTheme.GLASS),
                keyboardLanguage = runCatching {
                    KeyboardLanguage.valueOf(json.optString("keyboardLanguage", "ENGLISH"))
                }.getOrDefault(KeyboardLanguage.ENGLISH),
                keyboardTrail = runCatching {
                    KeyboardTrail.valueOf(json.optString("keyboardTrail", "AURORA"))
                }.getOrDefault(KeyboardTrail.AURORA),
                keyboardFont = runCatching {
                    KeyboardFont.valueOf(json.optString("keyboardFont", "SYSTEM"))
                }.getOrDefault(KeyboardFont.SYSTEM),
                keyboardFontWeight = runCatching {
                    KeyboardFontWeight.valueOf(json.optString("keyboardFontWeight", "REGULAR"))
                }.getOrDefault(KeyboardFontWeight.REGULAR),
                keyboardOpaque = json.optBoolean("keyboardOpaque", false),
                audioOutputEnabled = json.optBoolean("audioOutputEnabled", true),
                audioMicrophoneEnabled = json.optBoolean("audioMicrophoneEnabled", true),
                audioOutputVolume = json.optDouble("audioOutputVolume", .8).toFloat().coerceIn(0f, 1f),
                audioOutputRoute = runCatching {
                    AudioDeviceRoute.valueOf(json.optString("audioOutputRoute", "CONNECTED"))
                }.getOrDefault(AudioDeviceRoute.CONNECTED),
                audioDeviceEqProfiles = json.optJSONArray("audioDeviceEqProfiles")?.let { profiles ->
                    (0 until profiles.length()).mapNotNull { index ->
                        profiles.optJSONObject(index)?.let { profile ->
                            val gains = profile.optJSONArray("gains")?.let { values ->
                                (0 until values.length()).map { values.optDouble(it, 0.0).toFloat().coerceIn(-12f, 12f) }
                            }?.takeIf { it.size == 9 } ?: AudioOutputPreset.FLAT.gains
                            AudioDeviceEqProfile(
                                deviceKey = profile.optString("deviceKey", "default"),
                                deviceName = profile.optString("deviceName", "Phone output"),
                                preset = runCatching { AudioOutputPreset.valueOf(profile.optString("preset", "CUSTOM")) }.getOrDefault(AudioOutputPreset.CUSTOM),
                                gains = gains
                            )
                        }
                    }
                } ?: emptyList(),
                audioMicrophoneGain = json.optDouble("audioMicrophoneGain", 1.0).toFloat().coerceIn(0f, 2f),
                audioInputRoute = runCatching {
                    AudioDeviceRoute.valueOf(json.optString("audioInputRoute", "BUILT_IN"))
                }.getOrDefault(AudioDeviceRoute.BUILT_IN),
                audioVoiceIsolation = json.optBoolean("audioVoiceIsolation", true),
                audioMicrophoneNoiseGate = json.optDouble("audioMicrophoneNoiseGate", .015).toFloat().coerceIn(0f, .15f),
                audioMicrophonePreset = runCatching {
                    MicrophoneVoicePreset.valueOf(json.optString("audioMicrophonePreset", "CLEAN"))
                }.getOrDefault(MicrophoneVoicePreset.CLEAN),
                audioTransport = runCatching {
                    AudioTransport.valueOf(json.optString("audioTransport", "WIFI"))
                }.getOrDefault(AudioTransport.WIFI),
                webcamEnabled = json.optBoolean("webcamEnabled", false),
                webcamLens = runCatching {
                    WebcamLens.valueOf(json.optString("webcamLens", "REAR"))
                }.getOrDefault(WebcamLens.REAR),
                webcamResolution = runCatching {
                    WebcamResolution.valueOf(json.optString("webcamResolution", "FULL_HD"))
                }.getOrDefault(WebcamResolution.FULL_HD),
                webcamFps = json.optInt("webcamFps", 30).let { if (it in listOf(15, 24, 30)) it else 30 },
                webcamMirror = json.optBoolean("webcamMirror", false),
                webcamZoom = json.optDouble("webcamZoom", 1.0).toFloat().coerceIn(1f, 8f),
                webcamExposure = json.optDouble("webcamExposure", 0.0).toFloat().coerceIn(-1f, 1f),
                webcamFilterIntensity = json.optDouble("webcamFilterIntensity", 1.0).toFloat().coerceIn(0f, 1f),
                webcamFilters = json.optJSONArray("webcamFilters")?.let { values ->
                    buildList {
                        for (index in 0 until values.length()) {
                            runCatching { ThemeFilter.valueOf(values.getString(index)) }.getOrNull()
                                ?.takeIf { it != ThemeFilter.NONE && it !in this }
                                ?.let(::add)
                        }
                    }
                } ?: emptyList(),
                ballPositionX = json.optDouble("ballPositionX", 0.15).toFloat(),
                ballPositionY = json.optDouble("ballPositionY", 0.82).toFloat(),
                ballSizeDp = json.optDouble("ballSizeDp", 64.0).toFloat(),
                // Migrate the old centered-top default to the camera-safe lower-left layout.
                clockPositionX = json.optDouble("clockPositionX", 0.248).toFloat().let {
                    if (it == 0.50f && json.optDouble("clockPositionY", 0.882).toFloat() == 0.09f) 0.248f else it
                },
                clockPositionY = json.optDouble("clockPositionY", 0.882).toFloat().let {
                    if (it == 0.09f && json.optDouble("clockPositionX", 0.248).toFloat() == 0.50f) 0.882f else it
                },
                clockScale = json.optDouble("clockScale", 1.18).toFloat().let { if (it == 1.0f) 1.18f else it },
                clockStyle = try {
                    ClockStyle.valueOf(json.optString("clockStyle", "MINIMAL_PILL"))
                } catch (_: Exception) {
                    ClockStyle.MINIMAL_PILL
                },
                show24HourFormat = json.optBoolean("show24HourFormat", false),
                showSeconds = json.optBoolean("showSeconds", false),
                showBatteryPercentage = json.optBoolean("showBatteryPercentage", true),
                isLocked = false,
                isEditorMode = false
            )
        } catch (_: Exception) {
            TouchpadSettings()
        }
    }
}
