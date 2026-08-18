package com.minimate.touchpad.engine

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.minimate.touchpad.model.HapticIntensity

/**
 * High-fidelity haptic feedback engine for physical click feel.
 */
class HapticFeedbackEngine(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun playClick(intensity: HapticIntensity) {
        if (intensity == HapticIntensity.OFF || vibrator == null || !vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effectId = when (intensity) {
                HapticIntensity.SUBTLE -> VibrationEffect.EFFECT_TICK
                HapticIntensity.CRISP -> VibrationEffect.EFFECT_CLICK
                HapticIntensity.STRONG -> VibrationEffect.EFFECT_HEAVY_CLICK
                HapticIntensity.OFF -> return
            }
            vibrator.vibrate(VibrationEffect.createPredefined(effectId))
        } else {
            val duration = when (intensity) {
                HapticIntensity.SUBTLE -> 10L
                HapticIntensity.CRISP -> 20L
                HapticIntensity.STRONG -> 35L
                HapticIntensity.OFF -> return
            }
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    fun playSecondaryClick(intensity: HapticIntensity) {
        if (intensity == HapticIntensity.OFF || vibrator == null || !vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 15, 40, 15), -1)
        }
    }

    fun playModeTransition(intensity: HapticIntensity) {
        if (intensity == HapticIntensity.OFF || vibrator == null || !vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(25L)
        }
    }
}
