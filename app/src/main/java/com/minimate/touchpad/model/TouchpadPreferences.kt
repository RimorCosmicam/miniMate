package com.minimate.touchpad.model

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Robust persistent storage for TouchpadSettings using Android SharedPreferences.
 * Preserves all user configurations, themes, presets, analog stick settings, and layout coordinates across launches.
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

                // Theme & presets
                put("backgroundTheme", settings.backgroundTheme.name)
                put("themeVariantIndex", settings.themeVariantIndex)
                put("customImageUri", settings.customImageUri ?: "")
                put("currentPresetIndex", settings.currentPresetIndex)

                val presetsArray = JSONArray()
                settings.themePresets.forEach { preset ->
                    val pObj = JSONObject().apply {
                        put("theme", preset.theme.name)
                        put("variantIndex", preset.variantIndex)
                        put("customUri", preset.customUri ?: "")
                    }
                    presetsArray.put(pObj)
                }
                put("themePresets", presetsArray)

                // Finger FX
                put("fingerEffect", settings.fingerEffect.name)
                put("fingerEffectsEnabled", settings.fingerEffectsEnabled)

                // Analog Stick (Single Hand Mode)
                put("analogStickMode", settings.analogStickMode.name)
                put("stickSingleTapAction", settings.stickSingleTapAction.name)
                put("stickDoubleTapAction", settings.stickDoubleTapAction.name)
                put("stickHoldAction", settings.stickHoldAction.name)
                put("stickScrollSensitivity", settings.stickScrollSensitivity.toDouble())
                put("stickDeadzone", settings.stickDeadzone.toDouble())

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

            val presetsList = mutableListOf<ThemePreset>()
            if (json.has("themePresets")) {
                val pArray = json.getJSONArray("themePresets")
                for (i in 0 until pArray.length()) {
                    val pObj = pArray.getJSONObject(i)
                    val theme = try {
                        BackgroundTheme.valueOf(pObj.getString("theme"))
                    } catch (_: Exception) {
                        BackgroundTheme.CHROME_FLUID
                    }
                    val vIdx = pObj.optInt("variantIndex", 0)
                    val uri = pObj.optString("customUri", "").takeIf { it.isNotEmpty() }
                    presetsList.add(ThemePreset(theme, vIdx, uri))
                }
            }
            if (presetsList.isEmpty()) {
                presetsList.addAll(TouchpadSettings().themePresets)
            }

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
                },
                themeVariantIndex = json.optInt("themeVariantIndex", 0),
                customImageUri = json.optString("customImageUri", "").takeIf { it.isNotEmpty() },
                themePresets = presetsList,
                currentPresetIndex = json.optInt("currentPresetIndex", 0),
                fingerEffect = try {
                    FingerEffect.valueOf(json.optString("fingerEffect", "CHERRY_PETALS"))
                } catch (_: Exception) {
                    FingerEffect.CHERRY_PETALS
                },
                fingerEffectsEnabled = json.optBoolean("fingerEffectsEnabled", false),
                analogStickMode = try {
                    AnalogStickMode.valueOf(json.optString("analogStickMode", "ANALOG_SCROLL"))
                } catch (_: Exception) {
                    AnalogStickMode.ANALOG_SCROLL
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
                ballPositionX = json.optDouble("ballPositionX", 0.15).toFloat(),
                ballPositionY = json.optDouble("ballPositionY", 0.82).toFloat(),
                ballSizeDp = json.optDouble("ballSizeDp", 64.0).toFloat(),
                clockPositionX = json.optDouble("clockPositionX", 0.50).toFloat(),
                clockPositionY = json.optDouble("clockPositionY", 0.09).toFloat(),
                clockScale = json.optDouble("clockScale", 1.0).toFloat(),
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
