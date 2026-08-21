package com.minimate.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.minimate.touchpad.model.AnalogStickMode
import com.minimate.touchpad.model.StickTheme
import com.minimate.ui.theme.AccentCyan
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
    theme: StickTheme = StickTheme.PRECISION_DISC,
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
    val density = LocalDensity.current
    val stickSizePx = with(density) { stickSizeDp.dp.toPx() }
    val baseRadiusPx = stickSizePx / 2f
    // The thumb is roughly half the socket radius, so its center must remain
    // inside the remaining half. The old 2.7x calculation pushed it far past
    // the clipped canvas and made the control look unresponsive.
    val maxDeflectionPx = baseRadiusPx * 0.42f

    // 2D Nub Offset with spring physics
    val nubOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var isDragging by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    // Normalized deflection: -1.0..1.0
    var normDeflectionX by remember { mutableFloatStateOf(0f) }
    var normDeflectionY by remember { mutableFloatStateOf(0f) }

    // One stable ticker preserves sub-pixel carry while the thumb moves. Keying
    // this effect to every deflection update used to restart and starve it.
    LaunchedEffect(Unit) {
        var subpixelV = 0f
        var subpixelH = 0f
        var velocityV = 0f
        var velocityH = 0f

        while (isActive) {
            val x = normDeflectionX
            val y = normDeflectionY
            val mag = hypot(x, y)
            if (isDragging && mag > deadzone) {
                val effectiveMag = (mag - deadzone) / (1f - deadzone)
                val powerCurve = effectiveMag.pow(1.35f)
                when (mode) {
                    AnalogStickMode.ANALOG_SCROLL -> {
                        // Smooth velocity acceleration, expressed in HID ticks/sec.
                        // A hard low ceiling prevents the host's own wheel multiplier
                        // from turning full deflection into an uncontrollable jump.
                        val directionX = x / mag
                        val directionY = y / mag
                        val sensitivityGain = 0.72f + scrollSensitivity * 0.55f
                        val rate = (effectiveMag.pow(1.42f) * 24f * sensitivityGain).coerceAtMost(24f)
                        val desiredV = -directionY * rate
                        val desiredH = -directionX * rate
                        velocityV += (desiredV - velocityV) * 0.16f
                        velocityH += (desiredH - velocityH) * 0.16f
                        val targetV = velocityV / 60f + subpixelV
                        val targetH = velocityH / 60f + subpixelH

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
                        val targetDx = (x * powerCurve * cursorSpeed) + subpixelH
                        val targetDy = (y * powerCurve * cursorSpeed) + subpixelV
                        val intDx = targetDx.toInt()
                        val intDy = targetDy.toInt()
                        subpixelH = targetDx - intDx
                        subpixelV = targetDy - intDy
                        if (intDx != 0 || intDy != 0) onAnalogCursorMove(intDx, intDy)
                    }

                    AnalogStickMode.VIRTUAL_DPAD -> {
                        val rate = (8f + scrollSensitivity * 5f).coerceAtMost(14f) / 60f
                        val targetV = (if (y < -0.4f) rate else if (y > 0.4f) -rate else 0f) + subpixelV
                        val targetH = (if (x > 0.4f) -rate else if (x < -0.4f) rate else 0f) + subpixelH
                        val intV = targetV.toInt()
                        val intH = targetH.toInt()
                        subpixelV = targetV - intV
                        subpixelH = targetH - intH
                        if (intV != 0 || intH != 0) {
                            onAnalogScroll(intV, intH)
                        }
                    }
                }
            } else {
                subpixelV = 0f
                subpixelH = 0f
                velocityV = 0f
                velocityH = 0f
            }
            delay(16L) // 60Hz tick
        }
    }

    val stickWidthPx = stickSizePx
    val posX = (positionXFraction * screenWidthPx - stickWidthPx / 2f).coerceIn(8f, screenWidthPx - stickWidthPx - 8f)
    val posY = (positionYFraction * screenHeightPx - stickWidthPx / 2f).coerceIn(8f, screenHeightPx - stickWidthPx - 8f)

    Box(
        modifier = modifier
            .offset { IntOffset(posX.roundToInt(), posY.roundToInt()) }
            .size(stickSizeDp.dp)
            .shadow(16.dp, CircleShape, spotColor = AccentCyan.copy(alpha = 0.5f))
            .clip(CircleShape)
            // A single detector owns the stream. Separate tap and drag detectors
            // both consumed ACTION_DOWN, which made the stick appear inert.
            .pointerInput(Unit) {
                var lastTapUpTime = 0L
                var pendingSingleTap: kotlinx.coroutines.Job? = null

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    onTouchDown()

                    var dragging = false
                    var longPressTriggered = false
                    val longPressJob = scope.launch {
                        delay(viewConfiguration.longPressTimeoutMillis)
                        if (isPressed && !dragging) {
                            longPressTriggered = true
                            onHold()
                        }
                    }

                    var upTime = down.uptimeMillis
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        upTime = change.uptimeMillis
                        val displacement = change.position - down.position

                        if (!dragging && displacement.getDistance() >= viewConfiguration.touchSlop) {
                            dragging = true
                            isDragging = true
                            longPressJob.cancel()
                        }

                        if (dragging) {
                            val dist = displacement.getDistance()
                            val clamped = if (dist > maxDeflectionPx) {
                                Offset(
                                    displacement.x / dist * maxDeflectionPx,
                                    displacement.y / dist * maxDeflectionPx
                                )
                            } else {
                                displacement
                            }
                            scope.launch { nubOffset.snapTo(clamped) }
                            normDeflectionX = clamped.x / maxDeflectionPx
                            normDeflectionY = clamped.y / maxDeflectionPx
                            change.consume()
                        }
                    } while (change.pressed)

                    longPressJob.cancel()
                    isPressed = false

                    if (dragging) {
                        isDragging = false
                        normDeflectionX = 0f
                        normDeflectionY = 0f
                        scope.launch {
                            nubOffset.animateTo(
                                Offset.Zero,
                                spring(dampingRatio = 0.65f, stiffness = 500f)
                            )
                        }
                    } else if (!longPressTriggered) {
                        if (upTime - lastTapUpTime in
                            viewConfiguration.doubleTapMinTimeMillis..viewConfiguration.doubleTapTimeoutMillis
                        ) {
                            pendingSingleTap?.cancel()
                            pendingSingleTap = null
                            lastTapUpTime = 0L
                            onDoubleTap()
                        } else {
                            lastTapUpTime = upTime
                            pendingSingleTap?.cancel()
                            pendingSingleTap = scope.launch {
                                delay(viewConfiguration.doubleTapTimeoutMillis)
                                if (lastTapUpTime == upTime) {
                                    lastTapUpTime = 0L
                                    onSingleTap()
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val centerPt = Offset(w / 2f, h / 2f)
            val outerRadius = w / 2f

            val palette = when (theme) {
                StickTheme.PRECISION_DISC -> listOf(Color(0xFFDFFFFB), Color(0xFF5FE6D1), Color(0xFF1A4A50), Color(0xFF050A0D))
                StickTheme.ALUMINUM_DIAL -> listOf(Color(0xFFF4F7F9), Color(0xFF9DA9B2), Color(0xFF36424B), Color(0xFF070A0C))
                StickTheme.CLASSIC_TRACKBALL -> listOf(Color(0xFFD9EAFF), Color(0xFF3979D7), Color(0xFF102E66), Color(0xFF030711))
                StickTheme.ARCADE_BALL -> listOf(Color(0xFFEEEFF1), Color(0xFF555B65), Color(0xFF171A20), Color(0xFF020304))
                StickTheme.PIXEL_DPAD -> listOf(Color(0xFFDFFFEF), Color(0xFF4ABF8A), Color(0xFF163E31), Color(0xFF030906))
                StickTheme.VINYL_JOG -> listOf(Color(0xFFE6E7EA), Color(0xFF5B6068), Color(0xFF1C1E23), Color(0xFF030304))
                StickTheme.CANDY_CAP -> listOf(Color(0xFFFFE9ED), Color(0xFF9A314C), Color(0xFF47101F), Color(0xFF090205))
                StickTheme.CAMEO_SEAL -> listOf(Color(0xFFFFF8E8), Color(0xFFC5A96A), Color(0xFF5B4930), Color(0xFF090704))
            }

            // 1. Themed socket ring
            val socketBrush = Brush.radialGradient(
                colors = listOf(
                    palette[0].copy(alpha = 0.22f),
                    palette[1].copy(alpha = 0.18f),
                    palette[2].copy(alpha = 0.16f),
                    palette[3].copy(alpha = 0.78f)
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
                        palette[1].copy(alpha = 0.55f)
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
                        colors = listOf(Color.Transparent, palette[1].copy(alpha = 0.58f)),
                        start = centerPt,
                        end = nubCenter
                    ),
                    start = centerPt,
                    end = nubCenter,
                    strokeWidth = 3.5f
                )
            }

            // Every material uses the same restrained optical-disc silhouette.
            // Variants are material studies, not toy/controller costumes.
            val nubBrush = Brush.radialGradient(
                colors = listOf(palette[0].copy(.92f), palette[1].copy(.78f), palette[2].copy(.9f), palette[3]),
                center = Offset(nubCenter.x - nubRadius * 0.35f, nubCenter.y - nubRadius * 0.35f),
                radius = nubRadius * 1.1f
            )
            drawCircle(brush = nubBrush, radius = nubRadius * .92f, center = nubCenter)
            drawCircle(Color.White.copy(alpha = .08f), nubRadius * .66f, nubCenter)
            drawCircle(palette[1].copy(alpha = .32f), nubRadius * .28f, nubCenter)
            drawCircle(Color.White.copy(alpha = .72f), nubRadius * .11f, nubCenter - Offset(nubRadius * .27f, nubRadius * .3f))

            val rim = Brush.linearGradient(
                colors = listOf(Color(0xF5FFFFFF), Color(0x66FFFFFF), Color.Transparent, Color(0x55FFFFFF)),
                start = Offset(nubCenter.x - nubRadius, nubCenter.y - nubRadius),
                end = Offset(nubCenter.x + nubRadius, nubCenter.y + nubRadius)
            )
            val rimWidth = if (isPressed || isDragging) 2.2f else 1.5f
            drawCircle(rim, nubRadius * .92f, nubCenter, style = Stroke(rimWidth))
            drawCircle(Color.White.copy(alpha = .12f), nubRadius * .76f, nubCenter, style = Stroke(.8f))
        }
    }
}
