package com.minimate.touchpad.model

enum class HapticIntensity {
    OFF,
    SUBTLE,
    CRISP,
    STRONG
}

enum class BackgroundTheme(val displayName: String, val description: String) {
    SAKURA_PETALS("Sakura Petals", "Procedural drifting cherry blossoms with breeze physics"),
    BUBBLE_POP("Bubble Aquarium", "Spherical soap bubbles with surface tension & touch burst"),
    KAWAII_PAWS("Cat Paw Cafe", "Kitten paw print stamps with floating heart particles"),
    PRISM_WAVES("Prism Waves", "Iridescent chromatic diffraction & fluid ribbon waves"),
    MATCHA_CAFE("Matcha Latte Art", "Viscous fluid froth swirls and organic latte art flow"),
    RETRO_ARCADE("Retro 8-Bit", "Procedural pixel starfield, arcade blocks & scanlines"),
    TROPICAL_OCEAN("Bioluminescent Sea", "Caustic sun rays, deep aqua water & touch ripple physics"),
    STRAWBERRY_MOCHI("Jelly Mochi", "Elastic jiggling jelly grid with glossy specular shine"),
    STARRY_GALAXY("Cosmic Galaxy", "Spiral nebula vortex with twinkling stars & meteors"),
    CLEAN_MINIMAL("Stealth Titanium", "Brushed titanium texture with pure OLED black mode"),
    CUSTOM_IMAGE("Custom Wallpaper", "User-selected photo or animated GIF from gallery")
}

enum class ThemeVariant(val index: Int, val label: String) {
    VARIANT_A(0, "Soft Palette"),
    VARIANT_B(1, "Warm Palette"),
    VARIANT_C(2, "Cool Palette")
}

enum class FingerEffect(val displayName: String, val description: String) {
    CHERRY_PETALS("Sakura Trail", "Floating flower petals bursting under fingers"),
    BUBBLE_SPLASH("Soap Bubbles", "Cartoon bubbles that wobble and pop with highlights"),
    CAT_PAW_PRINTS("Cat Paws", "Soft kitten paw prints stamped on touch"),
    STAR_GLITTER("Star Glitter", "Sparkling fairy dust & star glitter trail"),
    RAINBOW_RIBBON("Rainbow Ribbon", "Glowing fluid rainbow ribbon following drag"),
    WATER_RIPPLES("Water Droplets", "Realistic concentric liquid water shockwaves"),
    PLASMA_LIGHTNING("Plasma Bolts", "Electric neon energy arcs between contact points"),
    NEON_RETICLE("Neon Reticle", "High-tech precision glowing crosshair target"),
    FIRE_HEARTS("Floating Hearts", "Glowing heart sparks trailing touch movement"),
    MINIMAL_DOT("Clean Dot", "Minimalist precision micro-dot with zero clutter")
}

enum class ButtonPressAction(val label: String, val description: String) {
    LIQUID_WOBBLE("Liquid Ripple", "Plays elastic liquid glass fluid wave animation"),
    MIDDLE_CLICK("Middle Click", "Emits standard mouse middle button (Scroll Click)"),
    RIGHT_CLICK("Right Click", "Emits secondary mouse click"),
    BACK_BUTTON("Back Button (M4)", "Emits browser/app Back mouse button 4"),
    FORWARD_BUTTON("Forward Button (M5)", "Emits browser/app Forward mouse button 5"),
    CYCLE_THEME("Cycle Theme Preset", "Instantly switches to next saved theme preset"),
    AMOLED_DIM("Amoled Mode", "Dims display to pure black battery saver"),
    OPEN_SETTINGS("Open Settings", "Opens the 3-tab Control Center"),
    PAIRING_MODE("Pairing Hub", "Opens Bluetooth device manager")
}

data class ThemePreset(
    val theme: BackgroundTheme = BackgroundTheme.SAKURA_PETALS,
    val variant: ThemeVariant = ThemeVariant.VARIANT_A,
    val customUri: String? = null
)

enum class ClockStyle(val label: String) {
    MINIMAL_PILL("Glass Pill"),
    DIGITAL_BOLD("Bold Digital"),
    CLEAN_SANS("Clean Sans"),
    MONOSPACE("Retro Mono"),
    OFF("Hidden")
}

enum class ClockPosition(val label: String) {
    TOP_LEFT("Top Left"),
    TOP_CENTER("Top Center"),
    TOP_RIGHT("Top Right"),
    BOTTOM_RIGHT("Bottom Right"),
    BOTTOM_CENTER("Bottom Center")
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
    // Active theme & 5 quick presets
    val backgroundTheme: BackgroundTheme = BackgroundTheme.SAKURA_PETALS,
    val themeVariant: ThemeVariant = ThemeVariant.VARIANT_A,
    val customImageUri: String? = null,
    val themePresets: List<ThemePreset> = listOf(
        ThemePreset(BackgroundTheme.SAKURA_PETALS, ThemeVariant.VARIANT_A),
        ThemePreset(BackgroundTheme.BUBBLE_POP, ThemeVariant.VARIANT_A),
        ThemePreset(BackgroundTheme.KAWAII_PAWS, ThemeVariant.VARIANT_A),
        ThemePreset(BackgroundTheme.PRISM_WAVES, ThemeVariant.VARIANT_A),
        ThemePreset(BackgroundTheme.CLEAN_MINIMAL, ThemeVariant.VARIANT_A)
    ),
    val currentPresetIndex: Int = 0,
    val fingerEffect: FingerEffect = FingerEffect.CHERRY_PETALS,
    val fingerEffectsEnabled: Boolean = true,
    val buttonPressAction: ButtonPressAction = ButtonPressAction.LIQUID_WOBBLE,
    val stealthDimHold: Boolean = true,
    val isLocked: Boolean = false,
    // Clock & Battery HUD customization
    val clockStyle: ClockStyle = ClockStyle.MINIMAL_PILL,
    val clockPosition: ClockPosition = ClockPosition.TOP_CENTER,
    val show24HourFormat: Boolean = false,
    val showSeconds: Boolean = false,
    val showBatteryPercentage: Boolean = true
)
