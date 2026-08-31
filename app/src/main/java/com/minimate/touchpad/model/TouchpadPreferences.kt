package com.minimate.touchpad.model

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Robust persistent storage for TouchpadSettings using Android SharedPreferences.
 * Preserves all user configurations, themes, and layout coordinates across launches.
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
                put("keyboardHapticsEnabled", settings.keyboardHapticsEnabled)

                // Active theme
                put("backgroundTheme", settings.backgroundTheme.name)
                put("themeVariantIndex", settings.themeVariantIndex)
                put("backgroundAnimation", settings.backgroundAnimation.name)
                put("themeFilters", JSONArray(settings.themeFilters.map { it.name }))
                put("abstractShaderTheme", settings.abstractShaderTheme.name)
                put("shaderSceneId", settings.shaderSceneId)
                put("shaderParams", JSONArray(settings.shaderParams))
                put("shaderPaletteIndex", settings.shaderPaletteIndex)
                put("shaderTouchStrength", settings.shaderTouchStrength.toDouble())
                put("shaderAberration", settings.shaderAberration.toDouble())
                put("shaderGrain", settings.shaderGrain.toDouble())
                put("panelThemeIndex", settings.panelThemeIndex)
                put("audioPanelX", settings.audioPanelX.toDouble())
                put("audioPanelY", settings.audioPanelY.toDouble())
                put("audioPanelScale", settings.audioPanelScale.toDouble())
                put("cameraPanelX", settings.cameraPanelX.toDouble())
                put("cameraPanelY", settings.cameraPanelY.toDouble())
                put("cameraPanelScale", settings.cameraPanelScale.toDouble())
                put("abstractSubthemeIndex", settings.abstractSubthemeIndex)
                put("shaderRecolor", settings.shaderRecolor.name)
                put("customShaderColors", JSONArray(settings.customShaderColors))
                put("customImageUri", settings.customImageUri ?: "")

                // Finger FX
                put("fingerEffectsEnabled", settings.fingerEffectsEnabled)

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
                put("keyboardScale", settings.keyboardScale.toDouble())
                put("keyboardHeight", settings.keyboardHeight.toDouble())
                put("audioOutputEnabled", settings.audioOutputEnabled)
                put("audioMicrophoneEnabled", settings.audioMicrophoneEnabled)
                put("audioOutputVolume", settings.audioOutputVolume.toDouble())
                put("audioOutputDeviceKey", settings.audioOutputDeviceKey)
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
                put("audioMicrophonePlacement", settings.audioMicrophonePlacement.name)
                put("audioPlacementAuto", settings.audioPlacementAuto)
                put("audioTransport", settings.audioTransport.name)
                put("webcamEnabled", settings.webcamEnabled)
                put("webcamResolution", settings.webcamResolution.name)
                put("webcamFps", settings.webcamFps)
                put("webcamMirror", settings.webcamMirror)
                put("webcamZoom", settings.webcamZoom.toDouble())
                put("webcamExposure", settings.webcamExposure.toDouble())
                put("webcamFlashEnabled", settings.webcamFlashEnabled)
                put("webcamFlashIntensity", settings.webcamFlashIntensity.toDouble())

                // Screen Editor layout
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
                keyboardHapticsEnabled = json.optBoolean("keyboardHapticsEnabled", true),
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
                // These were being written but never read, so every restart quietly reset the
                // scene to the default rather than restoring what was chosen.
                shaderSceneId = json.optString("shaderSceneId", "paradise").ifEmpty { "paradise" },
                shaderParams = json.optJSONArray("shaderParams")?.let { values ->
                    List(values.length()) { values.optDouble(it, 0.0).toFloat() }
                }.orEmpty(),
                shaderPaletteIndex = json.optInt("shaderPaletteIndex", 0).coerceAtLeast(0),
                shaderTouchStrength = json.optDouble("shaderTouchStrength", 1.0).toFloat().coerceIn(0f, 2f),
                shaderAberration = json.optDouble("shaderAberration", 0.6).toFloat().coerceIn(0f, 2f),
                shaderGrain = json.optDouble("shaderGrain", 0.3).toFloat().coerceIn(0f, 1f),
                shaderRecolor = runCatching {
                    ShaderRecolor.valueOf(json.optString("shaderRecolor", "AUTHORED"))
                }.getOrDefault(ShaderRecolor.AUTHORED),
                customShaderColors = json.optJSONArray("customShaderColors")?.let { values ->
                    List(values.length().coerceAtMost(4)) { values.optLong(it) }
                }?.takeIf { it.size == 4 } ?: DEFAULT_CUSTOM_SHADER_COLORS,
                customImageUri = json.optString("customImageUri", "").takeIf { it.isNotEmpty() },
                fingerEffectsEnabled = json.optBoolean("fingerEffectsEnabled", true),
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
                    KeyboardTheme.valueOf(json.optString("keyboardTheme", "MONT"))
                }.getOrDefault(KeyboardTheme.MONT),
                keyboardLanguage = runCatching {
                    KeyboardLanguage.valueOf(json.optString("keyboardLanguage", "ENGLISH"))
                }.getOrDefault(KeyboardLanguage.ENGLISH),
                keyboardTrail = runCatching {
                    KeyboardTrail.valueOf(json.optString("keyboardTrail", "AURORA"))
                }.getOrDefault(KeyboardTrail.AURORA),
                keyboardFont = runCatching {
                    KeyboardFont.valueOf(json.optString("keyboardFont", "MONT"))
                }.getOrDefault(KeyboardFont.MONT),
                keyboardFontWeight = runCatching {
                    KeyboardFontWeight.valueOf(json.optString("keyboardFontWeight", "REGULAR"))
                }.getOrDefault(KeyboardFontWeight.REGULAR),
                keyboardOpaque = json.optBoolean("keyboardOpaque", false),
                keyboardHeight = json.optDouble("keyboardHeight", 1.0).toFloat().coerceIn(.7f, 1.5f),
                keyboardScale = json.optDouble("keyboardScale", 1.0).toFloat().coerceIn(0.65f, 1.3f),
                audioOutputEnabled = json.optBoolean("audioOutputEnabled", true),
                audioMicrophoneEnabled = json.optBoolean("audioMicrophoneEnabled", true),
                audioOutputVolume = json.optDouble("audioOutputVolume", .8).toFloat().coerceIn(0f, 1f),
                audioOutputDeviceKey = json.optString("audioOutputDeviceKey", "phone"),
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
                audioMicrophoneGain = json.optDouble("audioMicrophoneGain", 1.0).toFloat().coerceIn(0f, 3f),
                audioMicrophonePlacement = runCatching {
                    MicrophonePlacement.valueOf(json.optString("audioMicrophonePlacement", "HANDHELD"))
                }.getOrDefault(MicrophonePlacement.HANDHELD),
                audioPlacementAuto = json.optBoolean("audioPlacementAuto", true),
                audioTransport = runCatching {
                    AudioTransport.valueOf(json.optString("audioTransport", "WIFI"))
                }.getOrDefault(AudioTransport.WIFI),
                webcamEnabled = json.optBoolean("webcamEnabled", false),
                webcamResolution = runCatching {
                    WebcamResolution.valueOf(json.optString("webcamResolution", "FULL_HD"))
                }.getOrDefault(WebcamResolution.FULL_HD),
                webcamFps = json.optInt("webcamFps", 20).let { if (it in listOf(15, 20, 24, 30)) it else 20 },
                webcamMirror = json.optBoolean("webcamMirror", false),
                webcamZoom = json.optDouble("webcamZoom", 1.0).toFloat().coerceIn(.5f, 8f),
                webcamExposure = json.optDouble("webcamExposure", 0.0).toFloat().coerceIn(-1f, 1f),
                webcamFlashEnabled = json.optBoolean("webcamFlashEnabled", false),
                webcamFlashIntensity = json.optDouble("webcamFlashIntensity", .5).toFloat().coerceIn(0f, 1f),
                // Migrate the old centered-top default to the camera-safe lower-left layout.
                clockPositionX = json.optDouble("clockPositionX", 0.248).toFloat().let {
                    if (it == 0.50f && json.optDouble("clockPositionY", 0.882).toFloat() == 0.09f) 0.248f else it
                },
                clockPositionY = json.optDouble("clockPositionY", 0.882).toFloat().let {
                    if (it == 0.09f && json.optDouble("clockPositionX", 0.248).toFloat() == 0.50f) 0.882f else it
                },
                clockScale = json.optDouble("clockScale", 1.18).toFloat().let { if (it == 1.0f) 1.18f else it },
                clockStyle = try {
                    ClockStyle.valueOf(json.optString("clockStyle", "MONT"))
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
