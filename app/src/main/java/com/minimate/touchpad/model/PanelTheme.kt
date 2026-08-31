package com.minimate.touchpad.model

/**
 * Appearance and placement of the in-screen panels — audio, camera, and anything else that floats
 * over the canvas.
 *
 * Shared rather than per-screen on purpose: panels that each carry their own look read as
 * different apps stitched together. One material choice applies everywhere, and position and size
 * are stored per panel because the camera cutout sits in one corner and what needs to stay clear
 * of it differs by panel.
 */
enum class PanelMaterial(val label: String) {
    /** Dark chrome with a light hairline. What the panels have always been. */
    DEFAULT("Default"),

    /**
     * The command bar's aesthetic, applied to a window: black at ninety-five percent, Mont Black
     * in white, no border and no corner rounding. State is carried by brightness alone.
     */
    MONT("Mont"),

    /**
     * Real glass. Samples the scene behind the panel and refracts it at the rim, where a thick
     * pane bends light hardest, splitting it slightly into colour as it goes.
     */
    LIQUID_GLASS("Liquid Glass"),

    /** Real frosted glass: the scene behind, blurred, under a light veil. */
    FROSTED("Frosted")
}

data class PanelTheme(
    val label: String,
    val material: PanelMaterial,
    /** ARGB. Tint laid over the panel body; alpha is honoured. */
    val background: Long,
    /** ARGB. Border and hairline colour. */
    val stroke: Long,
    /** ARGB. Primary text and control colour. */
    val accent: Long,
    val cornerRadius: Int
)

/**
 * One entry per material. Deliberately restrained and matched to the lo-fi scene palettes rather
 * than competing with them — a panel is something to read, not the thing being looked at.
 */
val panelThemes: List<PanelTheme> = listOf(
    PanelTheme("Default", PanelMaterial.DEFAULT, 0xB319191B, 0x38FFFFFF, 0xFFFFFFFF, 22),
    PanelTheme("Mont", PanelMaterial.MONT, 0xF2000000, 0x00000000, 0xFFFFFFFF, 0),
    PanelTheme("Liquid Glass", PanelMaterial.LIQUID_GLASS, 0x1FFFFFFF, 0x59FFFFFF, 0xFFFFFFFF, 26),
    PanelTheme("Frosted", PanelMaterial.FROSTED, 0x3D0E0F12, 0x2EFFFFFF, 0xFFF4F7FA, 20)
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
