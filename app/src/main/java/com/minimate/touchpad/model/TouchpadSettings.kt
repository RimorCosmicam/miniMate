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
        "Liquid Cyber Smoke",
        "Volumetric turbulent smoke plumes and molten liquid chrome with metallic specular reflections",
        listOf("Mercury Silver", "Cyber Neon Pink", "Golden Amber")
    ),
    NEBULA_SMOKE(
        "Cosmic Nebula Vapor",
        "Deep atmospheric galactic vapor clouds swirling with stellar gas and starlight",
        listOf("Violet Void", "Cosmic Cyan", "Solar Flare")
    ),
    ABYSSAL_FLUID(
        "Abyssal Bioluminescent Fluid",
        "Dark deep-ocean fluid dynamics with radiant underwater pressure waves and vortex eddies",
        listOf("Electric Cyan", "Abyss Violet", "Coral Luminescence")
    ),
    VOLCANIC_SMOKE(
        "Molten Volcanic Smolder",
        "Billowing subterranean volcanic smoke with convective magma heat glow",
        listOf("Magma Ember", "Plasma Ash", "Obsidian Fire")
    ),
    AURORA_CURRENTS(
        "Polar Aurora Currents",
        "Ethereal ionized atmospheric fluid curtains waving across space",
        listOf("Emerald Polar", "Cosmic Magenta", "Arctic Teal")
    ),
    PRISM_OIL(
        "Prismatic Oil Sheen",
        "Iridescent thin-film interference fluid swirling on dark liquid with rainbow dispersion",
        listOf("Prism Dispersion", "Opal Pearl", "Dark Obsidian")
    ),
    GRAVITY_WARP(
        "Dark Matter Spacetime Warp",
        "Gravitational lensing fluid horizon warping spacetime around finger touches",
        listOf("Singularity Blue", "Event Horizon Red", "Photon Gold")
    ),
    CYAN_VAPOR(
        "Electric Cyan Vapor",
        "High-energy ionized plasma vapor and turbulent atmospheric eddies",
        listOf("Electric Cyan", "Neon Violet", "Supercharged Mint")
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
    ),
    PIXEL_ARCADE(
        "Arcade Attract Mode",
        "Three original games play themselves: a falling-block puzzler, pixel football, and an alien cabinet shooter",
        listOf("Block Drop Autoplay", "Pixel Cup Autoplay", "Alien Cabinet Autoplay")
    ),
    NEON_RACER(
        "Neon Racer",
        "A synth-speed horizon with luminous lanes, city light streaks, and endless velocity",
        listOf("Coastal Sprint", "Midnight Traffic", "Desert Rally")
    ),
    LOW_POLY_REALMS(
        "Pocket Game Realms",
        "Tiny isometric worlds where villagers build, farm, and explore while the trackpad rests",
        listOf("Village Builder", "Dungeon Party", "Alien Farm")
    ),
    JAZZ_CLUB(
        "Midnight Jazz Session",
        "A tiny animated trio trades piano, saxophone, and drum solos on a living club stage",
        listOf("Blue Note Trio", "Velvet Brass", "After Hours Jam")
    ),
    SYNTH_WAVEFORM(
        "Pocket Music Studio",
        "An autoplaying step sequencer with notes, meters, turntables, and a moving playhead",
        listOf("Lo-Fi Desk", "Disco Sequencer", "Bass Laboratory")
    ),
    COUTURE_SILK(
        "Living Couture Runway",
        "Original animated dress silhouettes cross a miniature runway as photographers flash",
        listOf("Organza Runway", "Noir Salon", "Electric Atelier")
    ),
    MATRIX_BREAK(
        "System Escape",
        "A tiny pixel runner escapes scanning drones through a collapsing computer city",
        listOf("Terminal Run", "Red Alert", "White Rabbit Protocol")
    ),
    ESCHER_PORTAL(
        "Impossible Stairwalk",
        "Small travelers continuously navigate an original looping impossible architecture",
        listOf("Infinite Rooms", "Golden Staircase", "Violet Observatory")
    ),
    GALACTIC_NAVIGATOR(
        "Living Space Program",
        "Ships navigate, colony domes work, and orbital stations trade beneath blinking stars",
        listOf("Starship Voyage", "Pixel Colony", "Orbital Port")
    ),
    EXOPLANET_HORIZON(
        "Exoplanet Expedition",
        "An autonomous rover surveys alien terrain, samples crystals, and calls its orbiter",
        listOf("Titan Rover", "Twin-Sun Survey", "Emerald Eclipse Camp")
    ),
    KOI_LAGOON(
        "Living Koi Garden",
        "Koi weave through lotus pads, bridge shadows, falling petals, and touch-made ripples",
        listOf("Moon Garden", "Golden Pond", "Sakura Bridge")
    ),
    HADAL_OCEAN(
        "Hadal Expedition",
        "A tiny research submarine explores jelly migrations, ruins, and hydrothermal vents",
        listOf("Abyss Dive", "Jelly Migration", "Vent Discovery")
    ),
    CANDY_FACTORY(
        "Candy Factory Shift",
        "Conveyors sort sweets while gears turn, inspectors bounce, and wrapped candy ships",
        listOf("Bubblegum Line", "Citrus Works", "Grape Night Shift")
    ),
    DESSERT_PLANET(
        "Dessert Planet Platformer",
        "A tiny pastry astronaut jumps between cake cliffs, mochi moons, and cookie platforms",
        listOf("Strawberry Quest", "Matcha Moon", "Blueberry Kingdom")
    ),
    ROCOCO_GARDEN(
        "Rococo Tea Garden",
        "A living porcelain tea party with animated fountains, cake service, roses, and gilded pavilions",
        listOf("Rose Tea Party", "Celadon Picnic", "Midnight Masquerade")
    ),
    LOLITA_LACE(
        "Lolita Storybook",
        "Original pixel friends in elaborate lace dresses share tea, browse an atelier, and promenade",
        listOf("Sweet Tea Friends", "Gothic Atelier", "Classic Promenade")
    ),
    BEACH_WORLD(
        "Endless Beach Day",
        "Living coastlines seen from above: rolling tide, a busy tropical cove, and a reef lagoon",
        listOf("Aerial Shoreline", "Tropical Cove", "Reef Lagoon")
    ),
    SCENERY_COAST(
        "Azure Coast",
        "Code-rendered high-resolution pixel coastline with living surf, boats, reef shadows, and changing light",
        listOf("Sunlit Cove", "Golden Tide", "Moonlit Shore")
    ),
    SCENERY_ALPINE(
        "Alpine Sanctuary",
        "Layered mountain lake, waterfalls, pine silhouettes, cabin light, stars, and weather",
        listOf("First Light", "Blue Hour Cabin", "Snowfall")
    ),
    SCENERY_RAIN_CITY(
        "Rain City",
        "A cinematic pixel metropolis with wet reflections, trains, windows, signs, and real rain depth",
        listOf("Neon Rain", "Last Train", "Quiet Dawn")
    ),
    SCENERY_SAKURA(
        "Sakura Valley",
        "A hillside shrine landscape with drifting petals, river reflections, lanterns, and distant villages",
        listOf("Spring Morning", "Lantern Evening", "Petal Storm")
    ),
    SCENERY_DESERT(
        "Desert Monuments",
        "Vast dunes, ancient silhouettes, caravan lights, heat shimmer, and a deep celestial sky",
        listOf("Amber Crossing", "Violet Dusk", "Star Caravan")
    ),
    SCENERY_COSMOS(
        "Cosmic Frontier",
        "A detailed pixel vista of alien terrain, ringed worlds, distant stations, and luminous atmosphere",
        listOf("Orbital Dawn", "Crystal Moon", "Eclipse Outpost")
    )
}

enum class ThemeCollection(val label: String, val tagline: String) {
    ABSTRACTS("Abstracts", "Fluid matter, light, smoke, and color"),
    ARCADE("Arcade", "Pixels, cabinets, speed, and play"),
    GAME_WORLDS("Game Worlds", "Playable landscapes beyond the glass"),
    SOUND_AND_SOUL("Sound & Soul", "Jazz, rhythm, and living waveforms"),
    FASHION("Fashion", "Digital couture and impossible materials"),
    EXPERIMENTAL("Experimental", "Reality-breaking visual laboratories"),
    SPACE("Space", "Star charts, exoplanets, and cosmic navigation"),
    OCEAN("Ocean", "Dream lagoons and the living abyss"),
    CANDY("Candy", "Sugar physics and confectionery planets"),
    ROCOCO_LOLITA("Rococo Lolita", "Ornament, lace, porcelain, and fantasy couture"),
    BEACH("Beach", "Aerial shores, living coves, and reef lagoons"),
    SCENERY("Scenery", "Authored high-resolution pixel landscapes")
}

val BackgroundTheme.collection: ThemeCollection
    get() = when (this) {
        BackgroundTheme.PIXEL_ARCADE -> ThemeCollection.ARCADE
        BackgroundTheme.NEON_RACER, BackgroundTheme.LOW_POLY_REALMS -> ThemeCollection.GAME_WORLDS
        BackgroundTheme.JAZZ_CLUB, BackgroundTheme.SYNTH_WAVEFORM -> ThemeCollection.SOUND_AND_SOUL
        BackgroundTheme.COUTURE_SILK -> ThemeCollection.FASHION
        BackgroundTheme.MATRIX_BREAK, BackgroundTheme.ESCHER_PORTAL -> ThemeCollection.EXPERIMENTAL
        BackgroundTheme.GALACTIC_NAVIGATOR, BackgroundTheme.EXOPLANET_HORIZON -> ThemeCollection.SPACE
        BackgroundTheme.KOI_LAGOON, BackgroundTheme.HADAL_OCEAN -> ThemeCollection.OCEAN
        BackgroundTheme.CANDY_FACTORY, BackgroundTheme.DESSERT_PLANET -> ThemeCollection.CANDY
        BackgroundTheme.ROCOCO_GARDEN, BackgroundTheme.LOLITA_LACE -> ThemeCollection.ROCOCO_LOLITA
        BackgroundTheme.BEACH_WORLD -> ThemeCollection.BEACH
        BackgroundTheme.SCENERY_COAST,
        BackgroundTheme.SCENERY_ALPINE,
        BackgroundTheme.SCENERY_RAIN_CITY,
        BackgroundTheme.SCENERY_SAKURA,
        BackgroundTheme.SCENERY_DESERT,
        BackgroundTheme.SCENERY_COSMOS -> ThemeCollection.SCENERY
        else -> ThemeCollection.ABSTRACTS
    }

val BackgroundTheme.isScenery: Boolean
    get() = collection == ThemeCollection.SCENERY

val userFacingThemes: List<BackgroundTheme> = BackgroundTheme.values().filter {
    it.ordinal <= BackgroundTheme.CUSTOM_IMAGE.ordinal || it.isScenery
}

val userFacingCollections: List<ThemeCollection> = listOf(ThemeCollection.ABSTRACTS, ThemeCollection.SCENERY)

enum class BackgroundAnimation(val label: String, val description: String, val speed: Float) {
    FROZEN("Still Frame", "A composed, nearly motionless artwork", 0f),
    DREAM("Slow Dream", "Quiet cinematic drift", 0.32f),
    FLOW("Living Flow", "Balanced ambient motion", 1f),
    PULSE("Pulse", "Expressive rhythmic energy", 1.65f),
    HYPER("Hyperdrive", "Maximum experimental velocity", 2.75f)
}

enum class ThemeFilter(val label: String, val description: String) {
    NONE("Clean", "The world exactly as authored"),
    CHROMATIC("Chromatic", "RGB lens separation and subtle edge distortion"),
    CRT("CRT", "Scanlines, phosphor shimmer, and curved-screen vignette"),
    VHS("VHS", "Tape jitter, tracking lines, and soft color drift"),
    PIXELATE("Pixelate", "Turns every world into chunky display pixels"),
    DREAM_BLOOM("Dream Bloom", "Soft luminous highlights and a hazy lens"),
    MONO_INK("Mono Ink", "High-contrast monochrome editorial treatment"),
    KALEIDOSCOPE("Kaleidoscope", "Mirrored radial sectors turn the scene into moving glass"),
    FISHEYE("Fisheye", "Strong optical barrel curvature with edge compression"),
    HALFTONE("Halftone", "Printed-dot screening driven by the scene luminance"),
    THERMAL("Thermal", "False-color infrared mapping from cold violet to white heat"),
    NEGATIVE("Negative", "A clean photographic color inversion"),
    POSTERIZE("Posterize", "Hard tonal bands with graphic screen-print contrast"),
    FILM_GRAIN("35mm Film", "Organic grain, gate weave, vignette, and warm highlights"),
    MIRROR_PRISM("Mirror Prism", "Angular mirrored facets fracture the complete scene")
}

/**
 * Capture sizes, lowest first. Each step up roughly doubles the JPEG encode cost and the
 * sustained Wi-Fi throughput, and both land on the same thermal budget — 1080p30 was enough to
 * heat the phone with no filters running at all.
 */
enum class WebcamResolution(val label: String, val width: Int, val height: Int) {
    VGA("480p", 640, 480),
    QHD("540p", 960, 540),
    HD("720p", 1280, 720),
    FULL_HD("1080p", 1920, 1080)
}

enum class EdgeControlSide(val label: String) {
    LEFT("Left"),
    RIGHT("Right")
}

enum class EdgeControlMaterial(val label: String) {
    CLEAR_GLASS("Clear Glass"),
    FROSTED_GLASS("Adaptive Glass"),
    SMOKED_GLASS("Deep Glass"),
    PRISM_GLASS("Prismatic Glass"),
    CRYSTAL("Thick Lens")
}

enum class EdgeControlSize(val label: String, val scale: Float) {
    COMPACT("Compact", .82f),
    STANDARD("Standard", 1f),
    LARGE("Large", 1.20f)
}

enum class BallAction(val label: String, val description: String) {
    MIDDLE_CLICK("Middle Click (M3)", "Emits standard mouse middle button (Scroll Click)"),
    RIGHT_CLICK("Right Click", "Emits secondary mouse click"),
    LEFT_CLICK("Left Click", "Emits primary mouse click"),
    BACK_BUTTON("Back Button (M4)", "Emits browser/app Back mouse button 4"),
    FORWARD_BUTTON("Forward Button (M5)", "Emits browser/app Forward mouse button 5"),
    AMOLED_DIM("Amoled Mode", "Toggles display to pure black battery saver"),
    OPEN_SETTINGS("Open Settings", "Opens the Control Center"),
    PAIRING_MODE("Pairing Hub", "Opens Bluetooth device manager tab"),
    SCREEN_EDITOR("Screen Editor", "Enables freeform dragging & resizing of UI elements"),
    LIQUID_WOBBLE("Liquid Ripple", "Plays elastic liquid glass fluid wave animation"),
    DISABLED("Disabled", "No action performed")
}

val DEFAULT_CUSTOM_SHADER_COLORS = listOf(0xFF02080AL, 0xFF0B5056L, 0xFF25D8C7L, 0xFFF4FFFFL)

enum class ClockStyle(val label: String) {
    /** Square, black, Mont Black. The same surface as every other window. */
    MONT("Mont"),
    MINIMAL_PILL("Glass Pill"),
    DIGITAL_BOLD("Bold Digital"),
    CLEAN_SANS("Clean Sans"),
    MONOSPACE("Retro Mono"),
    OFF("Hidden")
}

data class KeyboardShortcut(
    val label: String,
    val modifiers: Int,
    val usage: Int
)

/**
 * Picking Mont brings Mont Black with it. The theme is a whole look, not a colour scheme, and the
 * typeface is the larger half of it — but it stays a default, so the Font menu still works after.
 */
fun TouchpadSettings.withKeyboardTheme(theme: KeyboardTheme): TouchpadSettings =
    if (theme == KeyboardTheme.MONT) {
        copy(keyboardTheme = theme, keyboardFont = KeyboardFont.MONT, keyboardFontWeight = KeyboardFontWeight.BOLD)
    } else copy(keyboardTheme = theme)

enum class KeyboardTheme(val label: String) {
    GLASS("Glass"),
    FROST("Frost"),
    /** Hard black, square keys, Mont Black type. The command bar's aesthetic, as a keyboard. */
    MONT("Mont"),
    TITANIUM("Titanium"),
    NOIR("Noir"),
    PORCELAIN("Porcelain"),
    TERMINAL("Terminal"),
    SUNSET("Sunset"),
    CYBER("Cyber"),
    PAPER("Paper")
}

enum class KeyboardTrail(val label: String) {
    AURORA("Aurora"),
    COMET("Comet"),
    RIBBON("Ribbon"),
    CONSTELLATION("Stars"),
    INK("Ink")
}

enum class KeyboardFont(val label: String) {
    SYSTEM("System"),
    MONT("Mont"),
    MONO("Mono"),
    PIXEL("Pixel"),
    SERIF("Serif")
}

enum class KeyboardFontWeight(val label: String) {
    LIGHT("Light"),
    REGULAR("Regular"),
    BOLD("Bold")
}

/**
 * Which machine is on the other end.
 *
 * The HID usages are identical either way — what Apple calls Command and Option, the standard
 * calls GUI and Alt — so this changes what the modifiers are called, not what they send.
 */
enum class HostPlatform(val label: String, val guiKey: String, val altKey: String, val ctrlKey: String) {
    MAC("macOS", "\u2318", "\u2325", "\u2303"),
    WINDOWS("Windows", "Win", "Alt", "Ctrl")
}

enum class KeyboardLanguage(val label: String, val shortLabel: String) {
    ENGLISH("English", "EN"),
    PORTUGUESE_BR("Português (Brasil)", "PT")
}

enum class AudioTransport(val label: String) {
    WIFI("Wi-Fi"),
    BLUETOOTH("Bluetooth")
}

/**
 * Where the phone is while speaking. This is not a cosmetic label: it sets the microphone array's
 * beam width and the gain, both of which depend on distance. Held near the face the beam is
 * narrow, which rejects most of the room. Lying on a desk at arm's length the same narrow beam
 * would cut off anyone who leans or turns, so it widens and the gain rises to cover the distance.
 */
/**
 * Where the phone is, and what that costs in level.
 *
 * The desk boost used to be 2.1, multiplied on top of a base of eleven — twenty-three times before
 * the slider was even consulted. Measured on the desk that drove the capture to full scale on
 * every phrase and held the limiter engaged continuously, which is heard as a hard, distorted
 * voice rather than a loud one. A desk is further away than a hand, but not eleven decibels
 * further, and the wide beam it opens already recovers most of the difference.
 */
enum class MicrophonePlacement(val label: String, val fieldDimension: Float, val gainScale: Float) {
    HANDHELD("Handheld", .75f, 1f),
    DESK("On desk", .55f, 1.15f)
}

enum class AudioOutputPreset(val label: String, val gains: List<Float>) {
    FLAT("Flat", List(9) { 0f }),
    IEM("IEM", listOf(1f, 1f, .5f, 0f, 0f, .5f, 1f, .5f, -1f)),
    WARM("Warm", listOf(3f, 3f, 2f, 1f, .5f, 0f, -1f, -1.5f, -2f)),
    VOCAL("Vocal", listOf(-2f, -1.5f, -1f, .5f, 2f, 3.5f, 3f, 1.5f, 0f)),
    BASS("Bass", listOf(6f, 5f, 3f, 1f, 0f, -1f, -1f, -1.5f, -2f)),
    BRIGHT("Bright", listOf(-1f, -1f, -.5f, 0f, .5f, 1f, 2f, 3.5f, 4f)),
    CUSTOM("Custom", List(9) { 0f })
}

data class AudioDeviceEqProfile(
    val deviceKey: String,
    val deviceName: String,
    val preset: AudioOutputPreset = AudioOutputPreset.FLAT,
    val gains: List<Float> = AudioOutputPreset.FLAT.gains
)

val DEFAULT_KEYBOARD_SHORTCUTS = listOf(
    KeyboardShortcut("Copy", 0x08, 0x06),
    KeyboardShortcut("Paste", 0x08, 0x19),
    KeyboardShortcut("Undo", 0x08, 0x1D),
    KeyboardShortcut("Spotlight", 0x08, 0x2C),
    KeyboardShortcut("App Switch", 0x08, 0x2B),
    KeyboardShortcut("Screenshot", 0x0A, 0x20)
)

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
    /** Vibration on each key. On by default: a keyboard with no travel needs something to confirm. */
    val keyboardHapticsEnabled: Boolean = true,
    /**
     * Whether the welcome has been dismissed. Deliberately not derived from whether a permission
     * happens to be missing: that made granting one dismiss the introduction mid-read, and made
     * the screen unreachable the moment everything was allowed.
     */
    val onboardingSeen: Boolean = false,
    
    // One active shader configuration. Theme Studio edits this directly.
    val backgroundTheme: BackgroundTheme = BackgroundTheme.CHROME_FLUID,
    val themeVariantIndex: Int = 0,
    val backgroundAnimation: BackgroundAnimation = BackgroundAnimation.FLOW,
    val themeFilters: List<ThemeFilter> = emptyList(),
    val abstractShaderTheme: AbstractShaderTheme = AbstractShaderTheme.OCEANIC,
    /** Selected scene from the shader catalog, its per-scene control values, and its palette. */
    val shaderSceneId: String = "parallax_sky",
    val shaderParams: List<Float> = emptyList(),
    val shaderPaletteIndex: Int = 0,
    val shaderTouchStrength: Float = 1f,
    /**
     * Lens and film character, applied to every scene after tone mapping. Aberration is on by
     * default because the chromatic split is most of what made the original Paradise Matrix read
     * as photographed rather than drawn.
     */
    val shaderAberration: Float = .6f,
    val shaderGrain: Float = .3f,
    /** Shared look for the floating panels, plus per-panel placement. */
    val panelThemeIndex: Int = 0,
    val audioPanelX: Float = .5f,
    val audioPanelY: Float = .46f,
    val audioPanelScale: Float = 1f,
    val cameraPanelX: Float = .5f,
    val cameraPanelY: Float = .46f,
    val cameraPanelScale: Float = 1f,
    val abstractSubthemeIndex: Int = 0,
    val shaderRecolor: ShaderRecolor = ShaderRecolor.AUTHORED,
    val customShaderColors: List<Long> = DEFAULT_CUSTOM_SHADER_COLORS,
    val customImageUri: String? = null,
    val fingerEffectsEnabled: Boolean = true, // Shaders react directly to touch
    
    // Mirrored single-hand edge controls. The right-click corner is always opposite the rail.
    val edgeScrollEnabled: Boolean = true,
    val edgeRightClickEnabled: Boolean = true,
    val edgeControlSide: EdgeControlSide = EdgeControlSide.LEFT,
    // The sizes and material settled on by hand on the device, rather than the round numbers
    // they started from. The rail and the corner are sized separately because they are gripped
    // differently — one is swept along, the other is hit.
    val edgeRailMaterial: EdgeControlMaterial = EdgeControlMaterial.CRYSTAL,
    val edgeRailScale: Float = 1.32f,
    val edgeCornerMaterial: EdgeControlMaterial = EdgeControlMaterial.CRYSTAL,
    val edgeCornerScale: Float = 1.28f,

    // User-editable Mac chords exposed by the keyboard's Shortcuts panel.
    val keyboardShortcuts: List<KeyboardShortcut> = DEFAULT_KEYBOARD_SHORTCUTS,
    val keyboardTheme: KeyboardTheme = KeyboardTheme.MONT,
    val keyboardLanguage: KeyboardLanguage = KeyboardLanguage.ENGLISH,
    val hostPlatform: HostPlatform = HostPlatform.MAC,
    val keyboardTrail: KeyboardTrail = KeyboardTrail.AURORA,
    val keyboardFont: KeyboardFont = KeyboardFont.MONT,
    val keyboardFontWeight: KeyboardFontWeight = KeyboardFontWeight.BOLD,
    val keyboardOpaque: Boolean = false,
    val keyboardScale: Float = 1f,
    /**
     * Row height, independent of [keyboardScale]. Key width follows the display, so scaling both
     * together is the only way to make the keyboard shorter without also making it narrower.
     */
    val keyboardHeight: Float = 1f,

    // Bidirectional MiniMate Audio companion link.
    val audioOutputEnabled: Boolean = true,
    val audioMicrophoneEnabled: Boolean = true,
    val audioOutputVolume: Float = .80f,
    val audioOutputDeviceKey: String = "phone",
    val audioDeviceEqProfiles: List<AudioDeviceEqProfile> = emptyList(),
    val audioMicrophoneGain: Float = 1f,
    val audioMicrophonePlacement: MicrophonePlacement = MicrophonePlacement.HANDHELD,
    /** Follow the gravity sensor instead of the manual choice above. */
    val audioPlacementAuto: Boolean = true,
    val audioTransport: AudioTransport = AudioTransport.WIFI,

    // MiniMate Camera. Frames use the companion's active Wi-Fi link; the Mac
    // applies this same stack of scene filters before publishing the camera.
    val webcamEnabled: Boolean = false,
    val webcamResolution: WebcamResolution = WebcamResolution.HD,
    val webcamFps: Int = 20,
    val webcamMirror: Boolean = false,
    val webcamZoom: Float = 1f,
    val webcamExposure: Float = 0f,
    val webcamFlashEnabled: Boolean = false,
    val webcamFlashIntensity: Float = .5f,
    
    // Screen Editor: Freeform Positions & Sizes
    
    val clockPositionX: Float = 0.248f,
    val clockPositionY: Float = 0.882f,
    val clockScale: Float = 1.18f,
    
    val isLocked: Boolean = false,
    val isEditorMode: Boolean = false,
    
    // Clock & HUD customization (Tap = AMOLED, Hold = Open Settings)
    val clockStyle: ClockStyle = ClockStyle.MONT,
    val show24HourFormat: Boolean = false,
    val showSeconds: Boolean = false,
    val showBatteryPercentage: Boolean = true
)
