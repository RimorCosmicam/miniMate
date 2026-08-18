package com.minimate.touchpad.model

enum class HapticIntensity {
    OFF,
    SUBTLE,
    CRISP,
    STRONG
}

enum class BackgroundTheme(val displayName: String, val description: String) {
    COSMIC_WARP("Cosmic Warp", "Spacetime gravity distortion & stellar accretion"),
    FLUID_AURORA("Fluid Aurora", "Harmonic glowing plasma waves"),
    LIQUID_GLASS("Liquid Glass", "Refractive prism caustic light waves"),
    CYBER_GRID("Cyber Grid", "Reactive neon digital matrix grid"),
    QUANTUM_WAVES("Quantum Waves", "Interference wave pattern radiating on touch"),
    VORTEX_NEBULA("Vortex Nebula", "Swirling celestial gas vortex"),
    BIOLUMINESCENCE("Bioluminescence", "Living organic deep sea glow"),
    GEOMETRIC_MORPH("Geometric Morph", "Dynamic morphing polygon tessellation"),
    PARTICLE_STARDUST("Particle Stardust", "Floating starfield with touch gravity"),
    MINIMAL_OLED("Minimal OLED", "Pure luxury OLED dark minimalism"),
    CUSTOM_IMAGE("Custom Wallpaper", "User-picked image or animated GIF")
}

enum class ThemeVariant(val index: Int, val label: String) {
    VARIANT_A(0, "Variant 1"),
    VARIANT_B(1, "Variant 2"),
    VARIANT_C(2, "Variant 3")
}

enum class FingerEffect(val displayName: String, val description: String) {
    LUMINOUS_HALO("Luminous Halo", "Glowing concentric rings with ambient diffusion"),
    SHOCKWAVE_RIPPLE("Shockwave Ripple", "Liquid sonar ripples expanding on touch"),
    PARTICLE_SPARKS("Particle Sparks", "Stardust sparks trailing finger motion"),
    PLASMA_ARC("Plasma Arc", "Electric arcs connecting touch points"),
    NEON_TARGET("Neon Target", "High-tech crosshair & radial tick marks"),
    BIOLUM_GLOW("Bio Glow", "Soft organic pressure-sensitive light puddle"),
    LASER_TRAIL("Laser Trail", "Aerodynamic light ribbon following fingers"),
    DIGITAL_MATRIX("Digital Glitch", "Cybernetic pixel fragments around touch"),
    MAGNETIC_FIELD("Magnetic Field", "Flux lines warping with motion velocity"),
    MINIMAL_DOT("Minimal Dot", "Surgical micro-dot with zero visual clutter")
}

enum class ButtonPressAction {
    STEALTH_DIM,        // Dims screen to OLED black
    OPEN_SETTINGS,      // Opens settings overlay
    PAIRING_MODE,       // Activates discoverable mode
    MIDDLE_CLICK        // Emits middle click
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
    val themeVariant: ThemeVariant = ThemeVariant.VARIANT_A,
    val fingerEffect: FingerEffect = FingerEffect.LUMINOUS_HALO,
    val fingerEffectsEnabled: Boolean = true,
    val customImageUri: String? = null,
    val buttonPressAction: ButtonPressAction = ButtonPressAction.STEALTH_DIM,
    val stealthDimHold: Boolean = true,
    val isLocked: Boolean = false
)
