package com.minimate.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import com.minimate.touchpad.model.PanelMaterial

/**
 * How much the panel's contents should round their corners.
 *
 * The panel frame alone following the material is not enough: a square Mont panel full of cards
 * rounded to twenty-two density-independent pixels still reads as a rounded design with its
 * outline cropped. Every control inside scales its own natural radius by this instead, so the
 * relative softness of each one is kept while the material decides whether there is any at all.
 */
val LocalPanelCornerScale = compositionLocalOf { 1f }

fun cornerScaleFor(material: PanelMaterial): Float =
    if (material == PanelMaterial.MONT) 0f else 1f

/** A control's natural corner radius, adjusted to the panel material in force. */
@Composable
fun panelShape(natural: Dp): RoundedCornerShape =
    RoundedCornerShape(natural * LocalPanelCornerScale.current)

/**
 * How much chrome the panel's contents draw around themselves — outlines and card fills alike.
 *
 * Mont has neither. The panel is the surface, so a card laid on top of it is a grey box sitting on
 * a black one, and a hairline around every control is exactly the decoration the aesthetic exists
 * to do without. Both survived inside the panel after being removed from its frame.
 */
val LocalPanelChrome = compositionLocalOf { 1f }

fun chromeScaleFor(material: PanelMaterial): Float =
    if (material == PanelMaterial.MONT) 0f else 1f

/** A control's natural outline or fill, faded out entirely by materials that carry none. */
@Composable
fun panelChrome(natural: Color): Color =
    natural.copy(alpha = natural.alpha * LocalPanelChrome.current)

@Composable
fun panelChrome(natural: Brush, fallback: Color = Color.Transparent): Brush =
    if (LocalPanelChrome.current > 0f) natural else SolidColor(fallback)

/**
 * The mark of a selected control. Where there is chrome it is a filled block with dark text; where
 * there is none it is simply the bright one, which is the rule the command bar and the studios
 * already follow.
 */
@Composable
fun panelSelectionFill(): Color = if (LocalPanelChrome.current > 0f) Color.White else Color.Transparent

@Composable
fun panelSelectedText(): Color = if (LocalPanelChrome.current > 0f) Color.Black else Color.White
