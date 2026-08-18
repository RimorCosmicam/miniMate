package com.minimate.touchpad.model

enum class HapticIntensity {
    OFF,
    SUBTLE,
    CRISP,
    STRONG
}

enum class BackgroundTheme(
    val displayName: String,
    val description: String,
    val variantNames: List<String>
) {
    CHROME_FLUID(
        "Liquid Cyber Chrome",
        "Viscous metallic mercury with specular highlights and environmental reflection",
        listOf("Mercury Silver", "Cyber Neon Pink", "Golden Amber")
    ),
    DEEP_ABYSS(
        "Bioluminescent Abyss",
        "Deep marine dark waters with pulsing glowing jellyfish light scattering",
        listOf("Electric Cyan", "Violet Deep", "Coral Glow")
    ),
    HYPERDRIVE_WARP(
        "Hyperdrive Warp Tunnel",
        "Relativistic space warp tunnel with light speed celestial streaks",
        listOf("Photon Blue", "Hyperspace Magenta", "Starlight Gold")
    ),
    AURORA_BOREALIS(
        "Aurora Borealis",
        "Silky curtains of polar northern lights dancing over starry skies",
        listOf("Emerald Polar", "Cosmic Violet", "Arctic Cyan")
    ),
    MAGMA_CORE(
        "Molten Magma Fissures",
        "Cracked glowing tectonic plates with subsurface convective heat glow",
        listOf("Volcanic Fire", "Plasma Purple", "Solar White-Hot")
    ),
    HOLO_PRISM(
        "Holographic Prismatic Glass",
        "Crystal refractive facets with spectral rainbow dispersion and thin-film sheen",
        listOf("Prism Rainbow", "Opal Pearl", "Obsidian Iridescent")
    ),
    SYNTHWAVE_3D(
        "Retro Synthwave 3D",
        "3D perspective wireframe terrain moving to infinity with glowing sun",
        listOf("Outrun 1984", "Cyber Tokyo", "Blood Dusk")
    ),
    SAKURA_BREEZE(
        "Sakura Wind Breeze",
        "Swirling cherry blossom petals caught in turbulent wind vortices",
        listOf("Spring Cherry", "Midnight Blossom", "Sunset Sakura")
    ),
    MATRIX_CASCADE(
        "Cyber Matrix Rain",
        "Cascading digital glyph waterfalls glitching on touch",
        listOf("Phosphor Green", "Amber CRT", "Cyan Ghost")
    ),
    STEALTH_OLED(
        "Stealth Titanium OLED",
        "True zero-power 100% OLED pitch black with luxury textures",
        listOf("Pure Pitch Black", "Brushed Titanium", "Carbon Weave")
    ),
    CUSTOM_IMAGE(
        "Custom Wallpaper / GIF",
        "User-selected photo or 60fps animated GIF from gallery",
        listOf("Original", "Dimmed 50%", "Vibrant")
    )
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

enum class AnalogStickMode(val label: String, val description: String) {
    ANALOG_SCROLL("2D Analog Scroller", "Tilt stick in any direction to scroll continuously with velocity scaling"),
    ANALOG_CURSOR("Continuous Cursor", "Tilt stick to glide the mouse pointer continuously"),
    VIRTUAL_DPAD("Directional D-Pad", "Push up/down/left/right to trigger page scroll & navigation")
}

enum class BallAction(val label: String, val description: String) {
    MIDDLE_CLICK("Middle Click (M3)", "Emits standard mouse middle button (Scroll Click)"),
    RIGHT_CLICK("Right Click", "Emits secondary mouse click"),
    LEFT_CLICK("Left Click", "Emits primary mouse click"),
    BACK_BUTTON("Back Button (M4)", "Emits browser/app Back mouse button 4"),
    FORWARD_BUTTON("Forward Button (M5)", "Emits browser/app Forward mouse button 5"),
    CYCLE_THEME("Cycle Theme Preset", "Instantly switches to next saved theme preset"),
    AMOLED_DIM("Amoled Mode", "Toggles display to pure black battery saver"),
    OPEN_SETTINGS("Open Settings", "Opens the 3-tab Control Center"),
    PAIRING_MODE("Pairing Hub", "Opens Bluetooth device manager"),
    SCREEN_EDITOR("Screen Editor", "Enables freeform dragging & resizing of UI elements"),
    LIQUID_WOBBLE("Liquid Ripple", "Plays elastic liquid glass fluid wave animation"),
    DISABLED("Disabled", "No action performed")
}

data class ThemePreset(
    val theme: BackgroundTheme = BackgroundTheme.CHROME_FLUID,
    val variantIndex: Int = 0,
    val customUri: String? = null
)

enum class ClockStyle(val label: String) {
    MINIMAL_PILL("Glass Pill"),
    DIGITAL_BOLD("Bold Digital"),
    CLEAN_SANS("Clean Sans"),
    MONOSPACE("Retro Mono"),
    OFF("Hidden")
}

data class TouchpadSettings(
    val trackingSpeed: Float = 1.0f,
    val acceleration: Float = 1.15f,
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
    val backgroundTheme: BackgroundTheme = BackgroundTheme.CHROME_FLUID,
    val themeVariantIndex: Int = 0,
    val customImageUri: String? = null,
    val themePresets: List<ThemePreset> = listOf(
        ThemePreset(BackgroundTheme.CHROME_FLUID, 0),
        ThemePreset(BackgroundTheme.DEEP_ABYSS, 0),
        ThemePreset(BackgroundTheme.HYPERDRIVE_WARP, 0),
        ThemePreset(BackgroundTheme.AURORA_BOREALIS, 0),
        ThemePreset(BackgroundTheme.STEALTH_OLED, 0)
    ),
    val currentPresetIndex: Int = 0,
    val fingerEffect: FingerEffect = FingerEffect.CHERRY_PETALS,
    val fingerEffectsEnabled: Boolean = false,
    
    // Analog Stick Configurations (Single Hand Mastery)
    val analogStickMode: AnalogStickMode = AnalogStickMode.ANALOG_SCROLL,
    val stickSingleTapAction: BallAction = BallAction.MIDDLE_CLICK,
    val stickDoubleTapAction: BallAction = BallAction.RIGHT_CLICK,
    val stickHoldAction: BallAction = BallAction.AMOLED_DIM,
    val stickScrollSensitivity: Float = 1.0f,
    val stickDeadzone: Float = 0.10f,
    
    // Screen Editor: Freeform Positions & Sizes (Normalized 0.0..1.0 coordinates and dp sizes)
    val ballPositionX: Float = 0.15f, // Left side comfortable for thumb
    val ballPositionY: Float = 0.82f, // Bottom corner
    val ballSizeDp: Float = 64f, // 64dp analog stick base
    
    val clockPositionX: Float = 0.50f, // Centered horizontally
    val clockPositionY: Float = 0.09f, // Near top
    val clockScale: Float = 1.0f,
    
    val isLocked: Boolean = false,
    val isEditorMode: Boolean = false,
    
    // Clock & HUD customization (Tap = Cycle Theme, Hold = Open Settings)
    val clockStyle: ClockStyle = ClockStyle.MINIMAL_PILL,
    val show24HourFormat: Boolean = false,
    val showSeconds: Boolean = false,
    val showBatteryPercentage: Boolean = true
)
