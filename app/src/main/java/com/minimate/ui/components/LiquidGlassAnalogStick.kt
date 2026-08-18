package com.minimate.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.minimate.touchpad.model.AnalogStickMode
import com.minimate.ui.theme.AccentCyan
import com.minimate.ui.theme.AccentPink
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Pure Liquid Glass 2D Analog Stick for effortless single-hand trackpad operation.
 * - 2D Analog Scroller with velocity scaling based on thumb deflection
 * - Tap to Click (M3 / Middle Click, etc.)
 * - Double Tap & Hold Actions
 * - Viscous spring physics return-to-center
 */
@Composable
fun LiquidGlassAnalogStick(
    stickSizeDp: Float,
    positionXFraction: Float,
    positionYFraction: Float,
    screenWidthPx: Float,
    screenHeightPx: Float,
    mode: AnalogStickMode = AnalogStickMode.ANALOG_SCROLL,
    scrollSensitivity: Float = 1.0f,
    deadzone: Float = 0.10f,
    isLocked: Boolean = false,
    onSingleTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onHold: () -> Unit,
    onAnalogScroll: (vScroll: Int, hScroll: Int) -> Unit,
    onAnalogCursorMove: (dx: Int, dy: Int) -> Unit,
    onTouchDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLocked) return

    val scope = rememberCoroutineScope()
    val baseRadiusPx = (stickSizeDp * 2.7f) / 2f
    val maxDeflectionPx = baseRadiusPx * 0.70f

    // 2D Nub Offset with spring physics
    val nubOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var isDragging by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    // Normalized deflection: -1.0..1.0
    var normDeflectionX by remember { mutableFloatStateOf(0f) }
    var normDeflectionY by remember { mutableFloatStateOf(0f) }

    // Continuous ticker loop while stick is deflected
    LaunchedEffect(isDragging, normDeflectionX, normDeflectionY) {
        var subpixelV = 0f
        var subpixelH = 0f

        while (isDragging && (normDeflectionX != 0f || normDeflectionY != 0f) && isActive) {
            val mag = hypot(normDeflectionX, normDeflectionY)
            if (mag > deadzone) {
                val effectiveMag = (mag - deadzone) / (1f - deadzone)
                val powerCurve = effectiveMag.pow(1.35f)

                when (mode) {
                    AnalogStickMode.ANALOG_SCROLL -> {
                        // Velocity scale: up to ±18 ticks per frame at max deflection
                        val speedMult = 14f * scrollSensitivity
                        val targetV = (-normDeflectionY * powerCurve * speedMult) + subpixelV
                        val targetH = (-normDeflectionX * powerCurve * speedMult) + subpixelH

                        val intV = targetV.toInt()
                        val intH = targetH.toInt()

                        subpixelV = targetV - intV
                        subpixelH = targetH - intH

                        if (intV != 0 || intH != 0) {
                            onAnalogScroll(intV, intH)
                        }
                    }

                    AnalogStickMode.ANALOG_CURSOR -> {
                        val cursorSpeed = 16f * scrollSensitivity
                        val targetDx = (normDeflectionX * powerCurve * cursorSpeed) + subpixelH
                        val targetDy = (normDeflectionY * powerCurve * cursorSpeed) + subpixelV

                        val intDx = targetDx.toInt()
                        val intDy = targetDy.toInt()

                        subpixelH = targetDx - intDx
                        subpixelV = targetDy - intDy

                        if (intDx != 0 || intDy != 0) {
                            onAnalogCursorMove(intDx, intDy)
                        }
                    }

                    AnalogStickMode.VIRTUAL_DPAD -> {
                        val intV = if (normDeflectionY < -0.4f) 5 else if (normDeflectionY > 0.4f) -5 else 0
                        val intH = if (normDeflectionX > 0.4f) -5 else if (normDeflectionX < -0.4f) 5 else 0
                        if (intV != 0 || intH != 0) {
                            onAnalogScroll(intV, intH)
                        }
                    }
                }
            }
            delay(16L) // 60Hz tick
        }
    }

    val stickWidthPx = stickSizeDp * 2.7f
    val posX = (positionXFraction * screenWidthPx - stickWidthPx / 2f).coerceIn(8f, screenWidthPx - stickWidthPx - 8f)
    val posY = (positionYFraction * screenHeightPx - stickWidthPx / 2f).coerceIn(8f, screenHeightPx - stickWidthPx - 8f)

    Box(
        modifier = modifier
            .offset { IntOffset(posX.roundToInt(), posY.roundToInt()) }
            .size(stickSizeDp.dp)
            .shadow(16.dp, CircleShape, spotColor = AccentCyan.copy(alpha = 0.5f))
            .clip(CircleShape)
            // Tap & Hold Gestures
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onTouchDown()
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onSingleTap() },
                    onDoubleTap = { onDoubleTap() },
                    onLongPress = { onHold() }
                )
            }
            // Drag Gestures for Analog Stick Control
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        onTouchDown()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val current = nubOffset.value + dragAmount
                        val dist = hypot(current.x, current.y)
                        val clamped = if (dist > maxDeflectionPx) {
                            Offset(
                                (current.x / dist) * maxDeflectionPx,
                                (current.y / dist) * maxDeflectionPx
                            )
                        } else {
                            current
                        }
                        scope.launch { nubOffset.snapTo(clamped) }
                        normDeflectionX = clamped.x / maxDeflectionPx
                        normDeflectionY = clamped.y / maxDeflectionPx
                    },
                    onDragEnd = {
                        isDragging = false
                        normDeflectionX = 0f
                        normDeflectionY = 0f
                        scope.launch {
                            nubOffset.animateTo(
                                Offset.Zero,
                                spring(dampingRatio = 0.65f, stiffness = 500f)
                            )
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        normDeflectionX = 0f
                        normDeflectionY = 0f
                        scope.launch {
                            nubOffset.animateTo(
                                Offset.Zero,
                                spring(dampingRatio = 0.65f, stiffness = 500f)
                            )
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val centerPt = Offset(w / 2f, h / 2f)
            val outerRadius = w / 2f

            // 1. Outer Liquid Glass Socket Ring (Frosted Translucent Body)
            val socketBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0x35FFFFFF),
                    Color(0x2000F5D4),
                    Color(0x25141624),
                    Color(0x65080A12)
                ),
                center = centerPt,
                radius = outerRadius
            )
            drawCircle(brush = socketBrush, radius = outerRadius, center = centerPt)

            // Socket Rim Specular Highlight
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xD0FFFFFF),
                        Color(0x33FFFFFF),
                        Color.Transparent,
                        AccentCyan.copy(alpha = 0.4f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                ),
                radius = outerRadius - 1.5f,
                center = centerPt,
                style = Stroke(1.5f)
            )

            // 2. Floating Liquid Glass Thumb Nub
            val nubCenter = centerPt + nubOffset.value
            val nubRadius = outerRadius * 0.52f

            // Directional Deflection Glow Vector
            if (isDragging && hypot(nubOffset.value.x, nubOffset.value.y) > 4f) {
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(AccentCyan.copy(alpha = 0.2f), AccentPink.copy(alpha = 0.7f)),
                        start = centerPt,
                        end = nubCenter
                    ),
                    start = centerPt,
                    end = nubCenter,
                    strokeWidth = 3.5f
                )
            }

            // Glass Nub Body
            val nubBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0x85FFFFFF),
                    Color(0x5500F5D4),
                    Color(0x40FF69B4),
                    Color(0x50121422),
                    Color(0x80080910)
                ),
                center = Offset(nubCenter.x - nubRadius * 0.35f, nubCenter.y - nubRadius * 0.35f),
                radius = nubRadius * 1.1f
            )
            drawCircle(brush = nubBrush, radius = nubRadius, center = nubCenter)

            // Glass Nub Specular Rim
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xF5FFFFFF),
                        Color(0x66FFFFFF),
                        Color.Transparent,
                        Color(0x55FFFFFF)
                    ),
                    start = Offset(nubCenter.x - nubRadius, nubCenter.y - nubRadius),
                    end = Offset(nubCenter.x + nubRadius, nubCenter.y + nubRadius)
                ),
                radius = nubRadius,
                center = nubCenter,
                style = Stroke(if (isPressed || isDragging) 2.2f else 1.5f)
            )
        }
    }
}
