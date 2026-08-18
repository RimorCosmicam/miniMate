package com.minimate.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.minimate.touchpad.engine.TouchPoint
import com.minimate.touchpad.model.FingerEffect
import com.minimate.ui.theme.AccentCyan
import com.minimate.ui.theme.AccentPurple
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FingerEffectsLayer(
    touchPoints: List<TouchPoint>,
    effect: FingerEffect = FingerEffect.CHERRY_PETALS,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!enabled || touchPoints.isEmpty()) return

    val transition = rememberInfiniteTransition(label = "CuteEffectPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseFloat"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        when (effect) {
            FingerEffect.CHERRY_PETALS -> {
                touchPoints.forEach { pt ->
                    val center = Offset(pt.x, pt.y)
                    // Draw 5 flower petals in a star formation
                    for (i in 0 until 5) {
                        val angle = (i * Math.PI * 2.0 / 5.0 + pulse * Math.PI * 0.5).toFloat()
                        val petalDist = 18f + sin(pulse * 6.28f + i) * 6f
                        val px = center.x + cos(angle) * petalDist
                        val py = center.y + sin(angle) * petalDist
                        drawCircle(color = Color(0xFFFFB7B2).copy(alpha = 0.85f), radius = 8f, center = Offset(px, py))
                        drawCircle(color = Color(0xFFFF69B4), radius = 5f, center = Offset(px, py))
                    }
                    drawCircle(color = Color(0xFFFFF0F5), radius = 6f, center = center)
                }
            }

            FingerEffect.BUBBLE_SPLASH -> {
                touchPoints.forEach { pt ->
                    val center = Offset(pt.x, pt.y)
                    for (i in 0..3) {
                        val phase = (pulse + i * 0.25f) % 1f
                        val r = 12f + phase * 45f
                        val alpha = (1f - phase) * 0.85f
                        val bubbleColor = if (i % 2 == 0) Color(0xFF89CFF0) else Color(0xFFFF85A1)
                        drawCircle(color = bubbleColor.copy(alpha = alpha), radius = r, center = center, style = Stroke(2.5f))
                        // Bubble highlight shine
                        drawCircle(color = Color.White.copy(alpha = alpha * 0.9f), radius = r * 0.2f, center = Offset(center.x - r * 0.4f, center.y - r * 0.4f))
                    }
                    drawCircle(color = Color.White, radius = 5f, center = center)
                }
            }

            FingerEffect.CAT_PAW_PRINTS -> {
                touchPoints.forEach { pt ->
                    val c = Offset(pt.x, pt.y)
                    val pawColor = Color(0xFFFF758F).copy(alpha = 0.9f)
                    // Main palm pad
                    drawOval(color = pawColor, topLeft = Offset(c.x - 14f, c.y - 8f), size = Size(28f, 22f))
                    drawOval(color = Color(0xFFFFB3C1), topLeft = Offset(c.x - 10f, c.y - 6f), size = Size(20f, 16f))
                    // 4 Toe beans
                    drawCircle(color = pawColor, radius = 5f, center = Offset(c.x - 12f, c.y - 18f))
                    drawCircle(color = pawColor, radius = 5.5f, center = Offset(c.x - 4f, c.y - 23f))
                    drawCircle(color = pawColor, radius = 5.5f, center = Offset(c.x + 4f, c.y - 23f))
                    drawCircle(color = pawColor, radius = 5f, center = Offset(c.x + 12f, c.y - 18f))
                }
            }

            FingerEffect.STAR_GLITTER -> {
                touchPoints.forEach { pt ->
                    val center = Offset(pt.x, pt.y)
                    for (i in 0 until 6) {
                        val angle = (i * Math.PI / 3.0 + pulse * Math.PI * 2.0).toFloat()
                        val dist = 22f + (i % 2) * 12f
                        val px = center.x + cos(angle) * dist
                        val py = center.y + sin(angle) * dist
                        val starCol = if (i % 3 == 0) Color(0xFFFFD166) else if (i % 3 == 1) Color(0xFF06D6A0) else Color(0xFFEF476F)
                        drawCircle(color = starCol, radius = 4f, center = Offset(px, py))
                        drawCircle(color = Color.White, radius = 2f, center = Offset(px, py))
                    }
                    drawCircle(color = Color.White, radius = 6f, center = center)
                }
            }

            FingerEffect.RAINBOW_RIBBON -> {
                touchPoints.forEach { pt ->
                    val c = Offset(pt.x, pt.y)
                    val colors = listOf(Color(0xFFFF595E), Color(0xFFFFCA3A), Color(0xFF8AC926), Color(0xFF1982C4), Color(0xFF6A4C93))
                    colors.forEachIndexed { idx, col ->
                        val r = 14f + idx * 7f + sin(pulse * 6.28f + idx) * 3f
                        drawCircle(color = col.copy(alpha = 0.8f), radius = r, center = c, style = Stroke(2.5f))
                    }
                    drawCircle(color = Color.White, radius = 5f, center = c)
                }
            }

            FingerEffect.WATER_RIPPLES -> {
                touchPoints.forEach { pt ->
                    val center = Offset(pt.x, pt.y)
                    for (i in 0..2) {
                        val phase = (pulse + i * 0.33f) % 1f
                        val r = 10f + phase * 60f
                        val alpha = (1f - phase) * 0.8f
                        drawCircle(color = Color(0xFF00B4D8).copy(alpha = alpha), radius = r, center = center, style = Stroke(2f))
                    }
                    drawCircle(color = Color(0xFFE0FBFC), radius = 5f, center = center)
                }
            }

            FingerEffect.PLASMA_LIGHTNING -> {
                if (touchPoints.size >= 2) {
                    for (i in 0 until touchPoints.size - 1) {
                        val p1 = Offset(touchPoints[i].x, touchPoints[i].y)
                        val p2 = Offset(touchPoints[i + 1].x, touchPoints[i + 1].y)
                        drawLine(color = Color(0xFF00F5D4), start = p1, end = p2, strokeWidth = 3.5f)
                        val mid = Offset((p1.x + p2.x) / 2f + sin(pulse * 6.28f) * 14f, (p1.y + p2.y) / 2f + cos(pulse * 6.28f) * 14f)
                        drawCircle(color = Color.White, radius = 5f, center = mid)
                    }
                }
                touchPoints.forEach { pt ->
                    drawCircle(color = Color(0xFF7B2CBF), radius = 18f, center = Offset(pt.x, pt.y), style = Stroke(2.5f))
                    drawCircle(color = Color.White, radius = 5f, center = Offset(pt.x, pt.y))
                }
            }

            FingerEffect.NEON_RETICLE -> {
                touchPoints.forEach { pt ->
                    val c = Offset(pt.x, pt.y)
                    drawCircle(color = AccentCyan.copy(alpha = 0.85f), radius = 24f, center = c, style = Stroke(2f))
                    drawLine(color = AccentCyan, start = Offset(c.x - 32f, c.y), end = Offset(c.x - 10f, c.y), strokeWidth = 2f)
                    drawLine(color = AccentCyan, start = Offset(c.x + 10f, c.y), end = Offset(c.x + 32f, c.y), strokeWidth = 2f)
                    drawLine(color = AccentCyan, start = Offset(c.x, c.y - 32f), end = Offset(c.x, c.y - 10f), strokeWidth = 2f)
                    drawLine(color = AccentCyan, start = Offset(c.x, c.y + 10f), end = Offset(c.x, c.y + 32f), strokeWidth = 2f)
                    drawCircle(color = Color.White, radius = 4f, center = c)
                }
            }

            FingerEffect.FIRE_HEARTS -> {
                touchPoints.forEach { pt ->
                    val c = Offset(pt.x, pt.y)
                    for (i in 0 until 4) {
                        val angle = (i * Math.PI / 2.0 + pulse * Math.PI).toFloat()
                        val dist = 20f + (i % 2) * 10f
                        val hx = c.x + cos(angle) * dist
                        val hy = c.y + sin(angle) * dist
                        drawHeart(Offset(hx, hy), size = 12f, color = Color(0xFFFF1493).copy(alpha = 0.85f))
                    }
                    drawCircle(color = Color(0xFFFFB6C1), radius = 6f, center = c)
                    drawCircle(color = Color.White, radius = 3f, center = c)
                }
            }

            FingerEffect.MINIMAL_DOT -> {
                touchPoints.forEach { pt ->
                    val c = Offset(pt.x, pt.y)
                    drawCircle(color = Color(0x66FFFFFF), radius = 12f, center = c)
                    drawCircle(color = Color.White, radius = 4f, center = c)
                }
            }
        }
    }
}

private fun DrawScope.drawHeart(center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y + size * 0.4f)
        cubicTo(center.x - size * 0.6f, center.y - size * 0.2f, center.x - size * 0.6f, center.y - size * 0.8f, center.x, center.y - size * 0.4f)
        cubicTo(center.x + size * 0.6f, center.y - size * 0.8f, center.x + size * 0.6f, center.y - size * 0.2f, center.x, center.y + size * 0.4f)
        close()
    }
    drawPath(path, color = color)
}
