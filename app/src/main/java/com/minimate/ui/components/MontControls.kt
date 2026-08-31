package com.minimate.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.ui.theme.Mont

/**
 * A toggle built from the same rectangle as the slider.
 *
 * Material's switch is a rounded track with a travelling bead — the most decorated object in a
 * design that has removed every other fill, border and corner. This is the slider stopped at two
 * positions: a white block occupying one half, and the state written in the half it has left. The
 * word names what the control currently is, not what tapping it would do, because a switch
 * labelled with its own opposite is a puzzle every time you meet it.
 */
@Composable
fun MontToggle(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val slide by animateFloatAsState(if (checked) 0f else 1f, label = "montToggle")
    val alpha = if (enabled) 1f else .35f

    Box(
        modifier
            .width(56.dp)
            .height(18.dp)
            .clickable(enabled = enabled) { onChange(!checked) }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val half = size.width * .5f
            drawRect(Color.White.copy(.09f * alpha), Offset.Zero, size)
            drawRect(Color.White.copy(alpha), Offset(half * slide, 0f), Size(half, size.height))
        }
        Text(
            if (checked) "ON" else "OFF",
            modifier = Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .width(28.dp),
            color = Color.White.copy(alpha),
            fontFamily = Mont,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * A slider as a rectangle that fills.
 *
 * No track, no bead, no rounding: the bar is dark and sliding fills it with white from the left,
 * so the value is an area rather than the position of a dot. The unfilled part carries a faint
 * white wash rather than being literally black — on a black panel a black rectangle at zero would
 * be a control you cannot see until you have already found it.
 */
@Composable
fun MontSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var width by remember { mutableFloatStateOf(1f) }
    val span = (range.endInclusive - range.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - range.start) / span).coerceIn(0f, 1f)

    fun valueAt(x: Float) = range.start + (x / width).coerceIn(0f, 1f) * span

    Box(
        modifier
            .fillMaxWidth()
            .height(18.dp)
            .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }
            // One gesture handler for press and drag alike. Two separate detectors compete for the
            // same pointer stream and the second never sees an event.
            .pointerInput(enabled, range) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onChange(valueAt(down.position.x))
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        onChange(valueAt(change.position.x))
                        change.consume()
                    }
                }
            }
    ) {
        Canvas(Modifier.fillMaxWidth().height(18.dp)) {
            val alpha = if (enabled) 1f else .35f
            drawRect(Color.White.copy(.09f * alpha), Offset.Zero, size)
            drawRect(
                Color.White.copy(alpha),
                Offset.Zero,
                Size(size.width * fraction, size.height)
            )
        }
    }
}
