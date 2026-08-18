package com.minimate.touchpad.model

enum class HapticIntensity {
    OFF,
    SUBTLE,
    CRISP,
    STRONG
}

enum class BackgroundTheme(val displayName: String, val description: String, val iconEmoji: String) {
    SAKURA_PETALS("Sakura Petals", "Floating cherry blossom petals & spring breeze", "🌸"),
    BUBBLE_POP("Bubble Pop", "Cute floating pastel soap bubbles that pop on touch", "🫧"),
    KAWAII_PAWS("Kawaii Paws", "Cute cat paw prints and subtle sparkles", "🐾"),
    RAINBOW_PASTEL("Rainbow Dreams", "Soothing iridescent pastel waves", "🌈"),
    MATCHA_CAFE("Matcha Cafe", "Creamy matcha latte art ripples & warm cafe vibe", "🍵"),
    RETRO_ARCADE("Retro Arcade", "8-bit pixel clouds and neon arcade glow", "👾"),
    TROPICAL_OCEAN("Ocean Waves", "Crystal clear turquoise water & sun caustics", "🌊"),
    STRAWBERRY_MOCHI("Berry Mochi", "Bouncy strawberry gelatin physics", "🍓"),
    STARRY_GALAXY("Starry Sky", "Twinkling starfield with shooting star trails", "✨"),
    CLEAN_MINIMAL("Clean Minimal", "Aesthetic minimal matte paper & pure OLED", "🖤"),
    CUSTOM_IMAGE("Custom Wallpaper", "User-picked image or animated GIF", "🖼️")
}

enum class ThemeVariant(val index: Int, val label: String) {
    VARIANT_A(0, "Palette 1"),
    VARIANT_B(1, "Palette 2"),
    VARIANT_C(2, "Palette 3")
}

enum class FingerEffect(val displayName: String, val description: String, val iconEmoji: String) {
    CHERRY_PETALS("Sakura Trail", "Floating flower petals bursting under fingers", "🌸"),
    BUBBLE_SPLASH("Soap Bubbles", "Colorful cartoon bubbles that wobble and pop", "🫧"),
    CAT_PAW_PRINTS("Cat Paws", "Cute kitten paw prints stamped on touch", "🐾"),
    STAR_GLITTER("Star Glitter", "Sparkling anime fairy dust & star glitter trail", "✨"),
    RAINBOW_RIBBON("Rainbow Ribbon", "Glowing fluid rainbow ribbon following drag", "🌈"),
    WATER_RIPPLES("Water Droplets", "Realistic concentric liquid water shockwaves", "💧"),
    PLASMA_LIGHTNING("Plasma Bolts", "Electric neon energy arcs between contact points", "⚡"),
    NEON_RETICLE("Neon Reticle", "High-tech precision glowing crosshair target", "🎯"),
    FIRE_HEARTS("Floating Hearts", "Cute glowing heart sparks trailing your finger", "💖"),
    MINIMAL_DOT("Clean Dot", "Minimalist surgical micro-dot with zero clutter", "🔘")
}

enum class ButtonPressAction {
    STEALTH_DIM,        // Dims screen to OLED black
    OPEN_SETTINGS,      // Opens settings overlay
    PAIRING_MODE,       // Opens pairing hub
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
    val backgroundTheme: BackgroundTheme = BackgroundTheme.SAKURA_PETALS,
    val themeVariant: ThemeVariant = ThemeVariant.VARIANT_A,
    val fingerEffect: FingerEffect = FingerEffect.CHERRY_PETALS,
    val fingerEffectsEnabled: Boolean = true,
    val customImageUri: String? = null,
    val buttonPressAction: ButtonPressAction = ButtonPressAction.STEALTH_DIM,
    val stealthDimHold: Boolean = true,
    val isLocked: Boolean = false
)
