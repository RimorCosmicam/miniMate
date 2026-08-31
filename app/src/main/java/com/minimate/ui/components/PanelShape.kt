package com.minimate.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
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
