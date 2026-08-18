package com.minimate.touchpad.model

enum class HapticIntensity {
    OFF,
    SUBTLE,
    CRISP,
    STRONG
}

enum class BackgroundTheme {
    COSMIC_WARP,     // Touch-reactive gravity distortion
    FLUID_AURORA,    // Interactive harmonic plasma waves
    LIQUID_GLASS,    // Refractive light prism warping
    CYBER_GRID,      // Interactive reactive digital matrix grid
    CUSTOM_IMAGE,    // Custom user image or GIF
    OLED_BLACK       // Pure OLED black
}

data class TouchpadSettings(
    val trackingSpeed: Float = 1.15f,
    val acceleration: Float = 1.20f,
    val scrollSpeed: Float = 1.0f,
    val naturalScrolling: Boolean = true,
    val invertCursorY: Boolean = false,
    val momentumScrolling: Boolean = true,
    val momentumFriction: Float = 0.92f,
    val tapToClick: Boolean = true,
    val twoFingerRightClick: Boolean = true,
    val doubleTapDrag: Boolean = true,
    val dragReleaseDelayMs: Long = 250L,
    val edgeMarginDp: Float = 12f,
    val hapticIntensity: HapticIntensity = HapticIntensity.CRISP,
    val backgroundTheme: BackgroundTheme = BackgroundTheme.COSMIC_WARP,
    val fingerEffectsEnabled: Boolean = true,
    val customImageUri: String? = null,
    val stealthDimHold: Boolean = true,
    val isLocked: Boolean = false
)
