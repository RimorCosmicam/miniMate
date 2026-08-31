package com.minimate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

/** Matched to the command bar so the two never read as different apps. */
internal val StudioBackground = Color.Black.copy(alpha = .95f)

@Composable
internal fun StudioPanel(
    title: String,
    subtitle: String,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth(.78f)
            .background(StudioBackground)
            .padding(start = 22.dp, top = 26.dp, end = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                StudioLabel(title, dim = true, size = 11)
                Text(
                    subtitle,
                    color = Color.White,
                    fontFamily = Mont,
                    fontWeight = StudioWeight,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StudioLabel("CANCEL", dim = true, modifier = Modifier.clickable(onClick = onCancel).padding(6.dp))
            Spacer(Modifier.width(10.dp))
            StudioLabel("DONE", modifier = Modifier.clickable(onClick = onDone).padding(6.dp))
        }
        content()
    }
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
