package com.minimate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.ui.theme.Mont

/**
 * The studios, set the same way as the command bar.
 *
 * Everything the old chrome used to say with decoration — rounded cards, gradients, hairlines,
 * shadows, icon buttons in circles — the type says on its own. A studio is now black, Mont Black,
 * white, and the selected thing is simply the bright one. The point of a theme editor is to look
 * at what is being edited, so the panel that edits it should not be the most elaborate object on
 * the screen.
 */
private val StudioWeight = FontWeight.Black

/**
 * The Mont surface: black, with the scene faintly present through the last eight percent. One
 * definition, used by the command bar, every studio and the Mont panel material, so the design
 * language cannot drift apart between them. The figure is the user's, arrived at by looking at it.
 */
const val MONT_SURFACE_ALPHA = .92f

internal val StudioBackground = Color.Black.copy(alpha = MONT_SURFACE_ALPHA)

/**
 * How far a top-anchored Mont surface holds off the top edge.
 *
 * The Flip's cover display is awkward to reach at the very top — a case lips over it, and Samsung
 * reserves a few pixels along the edge against mis-taps — so a row placed hard against it is a row
 * that takes two or three attempts.
 */
val MONT_TOP_INSET = 44.dp

@Composable
internal fun StudioPanel(
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Hard ceiling on how tall a studio may grow. A studio sits over the thing it is editing, and
     * what it contains changes as options are chosen, so without a bound its own content decides
     * whether it still fits on the display. It scrolls inside this instead.
     */
    maxHeight: Dp = 150.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            // Full width. A studio that stops three quarters of the way across leaves a strip
            // of dead screen beside every row it contains.
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .background(StudioBackground)
            .padding(start = 22.dp, top = 16.dp, end = 14.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        // No header at all. A panel that opens over the thing it edits does not need to announce
        // which panel it is, and the row that used to say so was costing the first line of the
        // list. Cancel and Done end the list instead, the same way Close ends the command bar's.
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            content()
            StudioRow("CANCEL", dim = true, onClick = onCancel)
            StudioRow("DONE", dim = false, onClick = onDone)
        }
    }
}

/** A full-width row, like the command bar's. */
@Composable
private fun StudioRow(label: String, dim: Boolean, onClick: () -> Unit) {
    StudioLabel(
        label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 2.dp),
        dim = dim
    )
}

/**
 * A choice. No pill, no border, no fill: selected is bright, unselected is dim, which is the same
 * rule the command bar's rows follow.
 */
@Composable
internal fun StudioChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    StudioLabel(
        label,
        dim = !selected,
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 5.dp, horizontal = 2.dp)
    )
}

@Composable
internal fun StudioLabel(
    text: String,
    modifier: Modifier = Modifier,
    dim: Boolean = false,
    size: Int = 14
) {
    Text(
        text,
        modifier = modifier,
        color = if (dim) Color.White.copy(.58f) else Color.White,
        fontFamily = Mont,
        fontWeight = StudioWeight,
        fontSize = size.sp,
        maxLines = 1
    )
}
