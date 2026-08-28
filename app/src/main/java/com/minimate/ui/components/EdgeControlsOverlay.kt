package com.minimate.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import com.minimate.touchpad.model.EdgeControlSide

/**
 * Low-profile single-hand controls that reserve only two narrow edge hit regions.
 * The rail and corner mirror as a pair so the click target is always opposite the scroll hand.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EdgeControlsOverlay(
    railEnabled: Boolean,
    rightClickEnabled: Boolean,
    railSide: EdgeControlSide,
    railScale: Float,
    cornerScale: Float,
    scrollSpeed: Float,
    naturalScrolling: Boolean,
    onScroll: (Int) -> Unit,
    onRightClick: () -> Unit,
    onRailTouchDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {
        if (railEnabled) {
            var active by remember { mutableStateOf(false) }
            var lastY by remember { mutableFloatStateOf(0f) }
            var remainder by remember { mutableFloatStateOf(0f) }

            fun consumeY(nextY: Float) {
                val rawDelta = nextY - lastY
                lastY = nextY
                val direction = if (naturalScrolling) -1f else 1f
                val target = rawDelta * .035f * scrollSpeed * direction + remainder
                val ticks = target.toInt()
                remainder = target - ticks
                if (ticks != 0) onScroll(ticks)
            }

            Box(
                Modifier
                    .align(if (railSide == EdgeControlSide.LEFT) Alignment.CenterStart else Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(52.dp * railScale.coerceIn(.65f, 1.8f))
                    .pointerInteropFilter { event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                active = true
                                lastY = event.y
                                remainder = 0f
                                onRailTouchDown()
                                true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                for (index in 0 until event.historySize) consumeY(event.getHistoricalY(0, index))
                                consumeY(event.y)
                                true
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                active = false
                                remainder = 0f
                                true
                            }
                            else -> true
                        }
                    }
            ) {
                Canvas(
                    Modifier
                        .align(if (railSide == EdgeControlSide.LEFT) Alignment.CenterStart else Alignment.CenterEnd)
                        .width((if (active) 32.dp else 28.dp) * railScale.coerceIn(.65f, 1.8f))
                        .fillMaxHeight()
                ) {
                    val boost = if (active) 1.75f else 1f
                    val leftToRight = listOf(Color.White.copy(.018f * boost), Color.White.copy(.038f * boost), Color(0xFFCBF7FF).copy(.026f * boost), Color.Transparent, Color.White.copy(.014f * boost))
                    val horizontal = if (railSide == EdgeControlSide.LEFT) leftToRight else leftToRight.reversed()
                    drawRect(
                        brush = Brush.horizontalGradient(horizontal),
                        size = size
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (active) .10f else .045f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.White.copy(alpha = if (active) .08f else .035f)
                            )
                        ),
                        size = size
                    )
                    val innerX = if (railSide == EdgeControlSide.LEFT) size.width else 0f
                    drawLine(
                        color = Color.White.copy(alpha = if (active) .24f else .12f),
                        start = Offset(innerX, 0f),
                        end = Offset(innerX, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    val firstAccent = Color.White
                    val secondAccent = Color(0xFFD8F8FF)
                    drawLine(
                        color = firstAccent.copy(alpha = if (active) .20f else .09f),
                        start = Offset(innerX + if (railSide == EdgeControlSide.LEFT) -1.4.dp.toPx() else 1.4.dp.toPx(), 0f),
                        end = Offset(innerX + if (railSide == EdgeControlSide.LEFT) -1.4.dp.toPx() else 1.4.dp.toPx(), size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = secondAccent.copy(alpha = if (active) .16f else .065f),
                        start = Offset(innerX + if (railSide == EdgeControlSide.LEFT) 1.4.dp.toPx() else -1.4.dp.toPx(), 0f),
                        end = Offset(innerX + if (railSide == EdgeControlSide.LEFT) 1.4.dp.toPx() else -1.4.dp.toPx(), size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }

        if (rightClickEnabled) {
            val clickOnLeft = railSide == EdgeControlSide.RIGHT
            var pressed by remember { mutableStateOf(false) }
            Box(
                Modifier
                    .align(if (clickOnLeft) Alignment.TopStart else Alignment.TopEnd)
                    .width(104.dp * cornerScale.coerceIn(.65f, 1.8f))
                    .height(104.dp * cornerScale.coerceIn(.65f, 1.8f))
                    .pointerInput(onRightClick) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                val released = tryAwaitRelease()
                                pressed = false
                                if (released) onRightClick()
                            }
                        )
                    }
            ) {
                Canvas(
                    Modifier
                        .align(if (clickOnLeft) Alignment.TopStart else Alignment.TopEnd)
                        .fillMaxSize()
                ) {
                    val center = if (clickOnLeft) Offset.Zero else Offset(size.width, 0f)
                    val radius = (if (pressed) 100.dp.toPx() else 94.dp.toPx()) * cornerScale.coerceIn(.65f, 1.8f)
                    val boost = if (pressed) 1.8f else 1f
                    val glass = listOf(Color.White.copy(.045f * boost), Color.White.copy(.022f * boost), Color(0xFFD9F8FF).copy(.012f * boost), Color.Transparent)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = glass,
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = if (pressed) .28f else .14f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    val firstAccent = Color.White
                    val secondAccent = Color(0xFFD8F8FF)
                    drawCircle(
                        color = firstAccent.copy(alpha = if (pressed) .20f else .09f),
                        radius = radius - .4.dp.toPx(),
                        center = center + Offset(if (clickOnLeft) 1.4.dp.toPx() else -1.4.dp.toPx(), 0f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = secondAccent.copy(alpha = if (pressed) .16f else .065f),
                        radius = radius - .4.dp.toPx(),
                        center = center + Offset(if (clickOnLeft) -1.4.dp.toPx() else 1.4.dp.toPx(), 0f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }
    }
}
