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
import androidx.compose.ui.graphics.drawscope.Stroke
import com.minimate.touchpad.engine.TouchPoint
import com.minimate.touchpad.model.FingerEffect
import com.minimate.ui.theme.AccentBlue
import com.minimate.ui.theme.AccentCyan
import com.minimate.ui.theme.AccentEmerald
import com.minimate.ui.theme.AccentPurple
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FingerEffectsLayer(
    touchPoints: List<TouchPoint>,
    effect: FingerEffect = FingerEffect.LUMINOUS_HALO,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!enabled || touchPoints.isEmpty()) return

    val transition = rememberInfiniteTransition(label = "EffectPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseFloat"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        when (effect) {
            FingerEffect.LUMINOUS_HALO -> {
                touchPoints.forEach { pt ->
                    val center = Offset(pt.x, pt.y)
                    val r = 32f * pt.pressure.coerceIn(0.8f, 1.8f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x6600E5FF), Color(0x228B5CF6), Color.Transparent),
                            center = center,
                            radius = r * 2.5f
                        ),
                        radius = r * 2.5f,
                        center = center
                    )
                    drawCircle(color = AccentCyan.copy(alpha = 0.8f), radius = r, center = center, style = Stroke(2.5f))
                    drawCircle(color = Color.White, radius = 4f, center = center)
                }
            }

            FingerEffect.SHOCKWAVE_RIPPLE -> {
                touchPoints.forEach { pt ->
                    val center = Offset(pt.x, pt.y)
                    for (i in 0..2) {
                        val phase = (pulse + i * 0.33f) % 1f
                        val rippleR = 10f + phase * 65f
                        val alpha = (1f - phase) * 0.75f
                        drawCircle(color = AccentCyan.copy(alpha = alpha), radius = rippleR, center = center, style = Stroke(2f))
                    }
                    drawCircle(color = Color.White, radius = 4f, center = center)
                }
            }

            FingerEffect.PARTICLE_SPARKS -> {
                touchPoints.forEach { pt ->
                    val center = Offset(pt.x, pt.y)
                    for (i in 0 until 8) {
                        val angle = (i * Math.PI / 4.0 + pulse * Math.PI * 2.0).toFloat()
                        val dist = 24f + (i % 3) * 10f
                        val px = center.x + cos(angle) * dist
                        val py = center.y + sin(angle) * dist
                        drawCircle(color = if (i % 2 == 0) AccentCyan else AccentPurple, radius = 3.5f, center = Offset(px, py))
                    }
                    drawCircle(color = Color.White, radius = 5f, center = center)
                }
            }

            FingerEffect.PLASMA_ARC -> {
                if (touchPoints.size >= 2) {
                    for (i in 0 until touchPoints.size - 1) {
                        val p1 = Offset(touchPoints[i].x, touchPoints[i].y)
                        val p2 = Offset(touchPoints[i + 1].x, touchPoints[i + 1].y)
                        drawLine(color = AccentCyan.copy(alpha = 0.85f), start = p1, end = p2, strokeWidth = 3f)
                        val mid = Offset((p1.x + p2.x) / 2f + sin(pulse * 6.28f) * 12f, (p1.y + p2.y) / 2f + cos(pulse * 6.28f) * 12f)
                        drawCircle(color = Color.White, radius = 4f, center = mid)
                    }
                }
                touchPoints.forEach { pt ->
                    drawCircle(color = AccentPurple, radius = 18f, center = Offset(pt.x, pt.y), style = Stroke(2f))
                    drawCircle(color = Color.White, radius = 4f, center = Offset(pt.x, pt.y))
                }
            }

            FingerEffect.NEON_TARGET -> {
                touchPoints.forEach { pt ->
                    val c = Offset(pt.x, pt.y)
                    drawCircle(color = AccentCyan.copy(alpha = 0.8f), radius = 26f, center = c, style = Stroke(1.5f))
                    drawLine(color = AccentCyan, start = Offset(c.x - 34f, c.y), end = Offset(c.x - 12f, c.y), strokeWidth = 2f)
                    drawLine(color = AccentCyan, start = Offset(c.x + 12f, c.y), end = Offset(c.x + 34f, c.y), strokeWidth = 2f)
                    drawLine(color = AccentCyan, start = Offset(c.x, c.y - 34f), end = Offset(c.x, c.y - 12f), strokeWidth = 2f)
                    drawLine(color = AccentCyan, start = Offset(c.x, c.y + 12f), end = Offset(c.x, c.y + 34f), strokeWidth = 2f)
                    drawCircle(color = Color.White, radius = 3.5f, center = c)
                }
            }

            FingerEffect.BIOLUM_GLOW -> {
                touchPoints.forEach { pt ->
                    val c = Offset(pt.x, pt.y)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x8810B981), Color(0x4400E5FF), Color.Transparent),
                            center = c,
                            radius = 48f
                        ),
                        radius = 48f,
                        center = c
                    )
                    drawCircle(color = Color(0xFFE6FFFA), radius = 6f, center = c)
                }
            }

            FingerEffect.LASER_TRAIL -> {
                touchPoints.forEach { pt ->
                    val c = Offset(pt.x, pt.y)
                    drawCircle(color = Color(0xFFFF007F), radius = 16f, center = c, style = Stroke(2f))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x77FF007F), Color.Transparent),
                            center = c,
                            radius = 35f
                        ),
                        radius = 35f,
                        center = c
                    )
                    drawCircle(color = Color.White, radius = 4f, center = c)
                }
            }

            FingerEffect.DIGITAL_MATRIX -> {
                touchPoints.forEach { pt ->
                    val c = Offset(pt.x, pt.y)
                    val s = 14f
                    drawRect(color = AccentEmerald.copy(alpha = 0.7f), topLeft = Offset(c.x - s, c.y - s), size = Size(s * 2, s * 2), style = Stroke(1.5f))
                    drawRect(color = Color(0x3300FF66), topLeft = Offset(c.x - s * 1.5f, c.y - s * 1.5f), size = Size(s * 3, s * 3), style = Stroke(1f))
                    drawCircle(color = Color.White, radius = 3f, center = c)
                }
            }

            FingerEffect.MAGNETIC_FIELD -> {
                touchPoints.forEach { pt ->
                    val c = Offset(pt.x, pt.y)
                    for (i in 1..3) {
                        val rx = 16f * i
                        val ry = 28f * i
                        drawOval(color = AccentBlue.copy(alpha = 0.5f / i), topLeft = Offset(c.x - rx, c.y - ry), size = Size(rx * 2, ry * 2), style = Stroke(1.5f))
                    }
                    drawCircle(color = Color.White, radius = 4f, center = c)
                }
            }

            FingerEffect.MINIMAL_DOT -> {
                touchPoints.forEach { pt ->
                    val c = Offset(pt.x, pt.y)
                    drawCircle(color = Color(0x55FFFFFF), radius = 14f, center = c)
                    drawCircle(color = Color.White, radius = 3.5f, center = c)
                }
            }
        }
    }
}
