package com.minimate.touchpad.model

enum class AbstractShaderTheme(val label: String, val description: String) {
    COSMIC("Space", "Astronomical scenes and deep-space journeys"),
    PRISMATIC("Abstract", "Mathematical light, geometry, fields, and materials"),
    TECH("Tech", "Computing systems, instruments, signals, and code"),
    ARCADE("Arcade", "Playable-looking scenes built from licensed game-art packs"),
    OCEANIC("Beach", "Shorelines, reefs, pools, surf, and underwater light")
}

enum class TouchReaction { RIPPLE, WARP, BURST, TRAIL, PULSE }

data class SceneColorway(val label: String, val stops: List<Long>)

data class AbstractSubtheme(
    val theme: AbstractShaderTheme,
    val index: Int,
    val label: String,
    val reaction: TouchReaction,
    val colorways: List<SceneColorway>,
    val sceneDescription: String,
    val compositionFamily: String,
    val depthModel: String,
    val motionModel: String,
    val interactionModel: String,
    val materialModel: String,
    val supportsCustomColors: Boolean,
    val sourceId: String,
    val sourceUrl: String,
    val sourceLicense: String
) {
    val colors: List<Long> get() = colorways.first().stops
}

val abstractSubthemes: List<AbstractSubtheme> = authoredThemeScenes

fun subthemesFor(theme: AbstractShaderTheme): List<AbstractSubtheme> =
    abstractSubthemes.filter { it.theme == theme }

/** Stable preference keys. Labels and palette values are resolved from the selected scene. */
enum class ShaderRecolor {
    AUTHORED, ALTERNATE_A, ALTERNATE_B, VARIANT_4, VARIANT_5, VARIANT_6, VARIANT_7, VARIANT_8, CUSTOM
}

private val authoredSelectors = ShaderRecolor.values().filterNot { it == ShaderRecolor.CUSTOM }

fun colorwaysFor(theme: AbstractShaderTheme): List<ShaderRecolor> = colorwaysFor(theme, 0)

fun colorwaysFor(theme: AbstractShaderTheme, subthemeIndex: Int): List<ShaderRecolor> {
    val scene = subthemesFor(theme)[subthemeIndex.coerceIn(0, 9)]
    return authoredSelectors.take(scene.colorways.size) + listOfNotNull(ShaderRecolor.CUSTOM.takeIf { scene.supportsCustomColors })
}

fun sceneColorwayFor(theme: AbstractShaderTheme, subthemeIndex: Int, selector: ShaderRecolor): SceneColorway {
    val scene = subthemesFor(theme)[subthemeIndex.coerceIn(0, 9)]
    if (selector == ShaderRecolor.CUSTOM) return SceneColorway("Custom", scene.colors)
    return scene.colorways[authoredSelectors.indexOf(selector).coerceIn(scene.colorways.indices)]
}

fun validColorway(theme: AbstractShaderTheme, requested: ShaderRecolor): ShaderRecolor =
    validColorway(theme, 0, requested)

fun validColorway(theme: AbstractShaderTheme, subthemeIndex: Int, requested: ShaderRecolor): ShaderRecolor =
    requested.takeIf { it in colorwaysFor(theme, subthemeIndex) } ?: ShaderRecolor.AUTHORED
