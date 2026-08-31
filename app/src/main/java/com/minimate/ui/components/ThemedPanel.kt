package com.minimate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.minimate.touchpad.model.PanelLayout
import com.minimate.touchpad.model.PanelMaterial
import com.minimate.touchpad.model.PanelTheme
import kotlin.math.roundToInt

/**
 * A floating panel that can be moved, resized and restyled.
 *
 * Position is stored as a fraction of the display so a panel dragged clear of the camera cutout
 * stays clear of it, and the same layout is meaningful on both of this device's screens. Drag with
 * one finger to move, pinch to resize; both are only live while editing, so an ordinary tap on a
 * control is never mistaken for a drag.
 */
@Composable
fun ThemedPanel(
    theme: PanelTheme,
    layout: PanelLayout,
    editing: Boolean,
    onLayoutChange: (PanelLayout) -> Unit,
    modifier: Modifier = Modifier,
    widthFraction: Float = .70f,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val boxWidth = constraints.maxWidth.toFloat()
        val boxHeight = constraints.maxHeight.toFloat()
        val shape = RoundedCornerShape(theme.cornerRadius.dp)

        var panelModifier = Modifier
            .fillMaxWidth(widthFraction * layout.scale.coerceIn(.6f, 1.6f))
            .offset {
                // Centre on the stored fraction rather than anchoring a corner, so resizing grows
                // the panel about its own middle instead of walking it across the screen.
                val w = boxWidth * widthFraction * layout.scale
                IntOffset(
                    (layout.x * boxWidth - w / 2f).coerceIn(0f, (boxWidth - w).coerceAtLeast(0f)).roundToInt(),
                    (layout.y * boxHeight - boxHeight * .18f).coerceIn(0f, boxHeight * .8f).roundToInt()
                )
            }

        panelModifier = when (theme.material) {
            PanelMaterial.NONE -> panelModifier
            PanelMaterial.TERMINAL -> panelModifier
                .clip(shape)
                .background(Color(theme.background))
                .border(1.dp, Color(theme.stroke), shape)
            PanelMaterial.SOLID -> panelModifier
                .clip(shape)
                .background(Color(theme.background))
                .border(1.dp, Color(theme.stroke), shape)
            PanelMaterial.VEIL -> panelModifier
                .clip(shape)
                .background(Color(theme.background))
                .border(1.dp, Color(theme.stroke), shape)
            PanelMaterial.GLASS -> panelModifier
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(theme.background), Color(theme.background).copy(alpha = 0.78f))
                    )
                )
                .border(1.dp, Color(theme.stroke), shape)
        }

        if (editing) {
            panelModifier = panelModifier
                .pointerInput(layout) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        onLayoutChange(
                            layout.copy(
                                x = (layout.x + drag.x / boxWidth).coerceIn(.1f, .9f),
                                y = (layout.y + drag.y / boxHeight).coerceIn(.05f, .95f)
                            )
                        )
                    }
                }
                .pointerInput(layout) {
                    detectTransformGestures { _, _, zoom, _ ->
                        onLayoutChange(layout.copy(scale = (layout.scale * zoom).coerceIn(.6f, 1.6f)))
                    }
                }
                .border(1.dp, Color.White.copy(.75f), shape)
        }

        Column(
            panelModifier.padding(
                horizontal = if (theme.material == PanelMaterial.NONE) 4.dp else 11.dp,
                vertical = if (theme.material == PanelMaterial.NONE) 4.dp else 9.dp
            )
        ) { content() }
    }
}
