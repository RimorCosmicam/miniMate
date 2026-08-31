package com.minimate.touchpad.model

/**
 * Appearance and placement of the in-screen panels — audio, camera, and anything else that floats
 * over the canvas.
 *
 * Shared rather than per-screen on purpose: panels that each carry their own look read as
 * different apps stitched together. One theme choice applies everywhere, and position and size are
 * stored per panel because the camera cutout sits in one corner and what needs to stay clear of it
 * differs by panel.
 */
enum class PanelMaterial(val label: String) {
    /** Frosted dark glass with a light hairline. The app's default chrome. */
    GLASS("Glass"),
    /** Flat opaque black. Highest contrast, cheapest to draw, no translucency. */
    SOLID("Solid"),
    /** Barely-there tint: mostly the scene, with just enough veil to read text. */
    VEIL("Veil"),
    /** Hard edges and a bright border, terminal-like. */
    TERMINAL("Terminal"),
    /** No background at all — text and controls directly over the scene. */
    NONE("None")
}

data class PanelTheme(
    val label: String,
    val material: PanelMaterial,
    /** ARGB. Tint of the panel body; alpha is honoured. */
    val background: Long,
    /** ARGB. Border and hairline colour. */
    val stroke: Long,
    /** ARGB. Primary text and control colour. */
    val accent: Long,
    val cornerRadius: Int
)

/**
 * The five presets. Deliberately restrained and matched to the lo-fi scene palettes rather than
 * competing with them — a panel is something to read, not the thing being looked at.
 */
val panelThemes: List<PanelTheme> = listOf(
    PanelTheme("Graphite", PanelMaterial.GLASS, 0xB319191B, 0x38FFFFFF, 0xFFFFFFFF, 22),
    PanelTheme("Ink", PanelMaterial.SOLID, 0xF0000000, 0x4DFFFFFF, 0xFFFFFFFF, 14),
    PanelTheme("Mist", PanelMaterial.VEIL, 0x59101014, 0x24FFFFFF, 0xFFF2F6FA, 26),
    PanelTheme("Terminal", PanelMaterial.TERMINAL, 0xE0020806, 0xFF6ED6DD, 0xFF6ED6DD, 4),
    PanelTheme("Bare", PanelMaterial.NONE, 0x00000000, 0x00000000, 0xFFFFFFFF, 0)
)

fun panelThemeAt(index: Int): PanelTheme = panelThemes.getOrElse(index) { panelThemes.first() }

/**
 * Where one panel sits and how large it is, as fractions of the display so it survives rotation
 * and the two different screens this app runs on.
 */
data class PanelLayout(
    val x: Float = .5f,
    val y: Float = .5f,
    val scale: Float = 1f
)
