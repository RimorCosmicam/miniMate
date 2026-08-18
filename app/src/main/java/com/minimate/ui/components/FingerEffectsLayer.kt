package com.minimate.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.touchpad.engine.TouchPoint
import com.minimate.touchpad.model.FingerEffect
import com.minimate.ui.theme.AccentCyan
import com.minimate.ui.theme.AccentGold
import com.minimate.ui.theme.AccentPink
import com.minimate.ui.theme.TextPrimary
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
        touchPoints.forEach { pt ->
            drawSingleFingerEffect(effect = effect, center = Offset(pt.x, pt.y), pulse = pulse)
        }

        // Multi-point connection for Plasma Lightning
        if (effect == FingerEffect.PLASMA_LIGHTNING && touchPoints.size >= 2) {
            for (i in 0 until touchPoints.size - 1) {
                val p1 = Offset(touchPoints[i].x, touchPoints[i].y)
                val p2 = Offset(touchPoints[i + 1].x, touchPoints[i + 1].y)
                drawLine(color = Color(0xFF00F5D4), start = p1, end = p2, strokeWidth = 3.5f)
                val mid = Offset((p1.x + p2.x) / 2f + sin(pulse * 6.28f) * 14f, (p1.y + p2.y) / 2f + cos(pulse * 6.28f) * 14f)
                drawCircle(color = Color.White, radius = 5f, center = mid)
            }
        }
    }
}

/**
 * Top-Right Floating Live Mini Preview Popup for Finger Effects
 */
@Composable
fun FingerEffectPreviewPopup(
    effect: FingerEffect,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "PreviewPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PreviewPulseFloat"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = AccentPink)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xEE161726))
                .border(1.5.dp, AccentPink.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(effect.iconEmoji, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        effect.displayName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Simulated Mini Interactive Touch Canvas
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x88000000))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f + cos(pulse * 6.28f) * 16f
                        val cy = size.height / 2f + sin(pulse * 6.28f) * 12f
                        drawSingleFingerEffect(effect = effect, center = Offset(cx, cy), pulse = pulse)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Live Effect Preview",
                    color = AccentCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

fun DrawScope.drawSingleFingerEffect(
    effect: FingerEffect,
    center: Offset,
    pulse: Float
) {
    when (effect) {
        FingerEffect.CHERRY_PETALS -> {
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

        FingerEffect.BUBBLE_SPLASH -> {
            for (i in 0..3) {
                val phase = (pulse + i * 0.25f) % 1f
                val r = 10f + phase * 36f
                val alpha = (1f - phase) * 0.85f
                val bubbleColor = if (i % 2 == 0) Color(0xFF89CFF0) else Color(0xFFFF85A1)
                drawCircle(color = bubbleColor.copy(alpha = alpha), radius = r, center = center, style = Stroke(2.5f))
                drawCircle(color = Color.White.copy(alpha = alpha * 0.9f), radius = r * 0.2f, center = Offset(center.x - r * 0.4f, center.y - r * 0.4f))
            }
            drawCircle(color = Color.White, radius = 5f, center = center)
        }

        FingerEffect.CAT_PAW_PRINTS -> {
            val pawColor = Color(0xFFFF758F).copy(alpha = 0.9f)
            // Main palm pad
            drawOval(color = pawColor, topLeft = Offset(center.x - 13f, center.y - 7f), size = Size(26f, 20f))
            drawOval(color = Color(0xFFFFB3C1), topLeft = Offset(center.x - 9f, center.y - 5f), size = Size(18f, 14f))
            // 4 Toe beans
            drawCircle(color = pawColor, radius = 4.5f, center = Offset(center.x - 11f, center.y - 16f))
            drawCircle(color = pawColor, radius = 5f, center = Offset(center.x - 4f, center.y - 20f))
            drawCircle(color = pawColor, radius = 5f, center = Offset(center.x + 4f, center.y - 20f))
            drawCircle(color = pawColor, radius = 4.5f, center = Offset(center.x + 11f, center.y - 16f))
        }

        FingerEffect.STAR_GLITTER -> {
            for (i in 0 until 6) {
                val angle = (i * Math.PI / 3.0 + pulse * Math.PI * 2.0).toFloat()
                val dist = 18f + (i % 2) * 10f
                val px = center.x + cos(angle) * dist
                val py = center.y + sin(angle) * dist
                val starCol = if (i % 3 == 0) Color(0xFFFFD166) else if (i % 3 == 1) Color(0xFF06D6A0) else Color(0xFFEF476F)
                drawCircle(color = starCol, radius = 4f, center = Offset(px, py))
                drawCircle(color = Color.White, radius = 2f, center = Offset(px, py))
            }
            drawCircle(color = Color.White, radius = 5f, center = center)
        }

        FingerEffect.RAINBOW_RIBBON -> {
            val colors = listOf(Color(0xFFFF595E), Color(0xFFFFCA3A), Color(0xFF8AC926), Color(0xFF1982C4), Color(0xFF6A4C93))
            colors.forEachIndexed { idx, col ->
                val r = 12f + idx * 6f + sin(pulse * 6.28f + idx) * 3f
                drawCircle(color = col.copy(alpha = 0.8f), radius = r, center = center, style = Stroke(2.5f))
            }
            drawCircle(color = Color.White, radius = 5f, center = center)
        }

        FingerEffect.WATER_RIPPLES -> {
            for (i in 0..2) {
                val phase = (pulse + i * 0.33f) % 1f
                val r = 8f + phase * 45f
                val alpha = (1f - phase) * 0.8f
                drawCircle(color = Color(0xFF00B4D8).copy(alpha = alpha), radius = r, center = center, style = Stroke(2f))
            }
            drawCircle(color = Color(0xFFE0FBFC), radius = 5f, center = center)
        }

        FingerEffect.PLASMA_LIGHTNING -> {
            drawCircle(color = Color(0xFF7B2CBF), radius = 18f, center = center, style = Stroke(2.5f))
            drawCircle(color = Color(0xFF00F5D4), radius = 10f, center = center, style = Stroke(1.5f))
            drawCircle(color = Color.White, radius = 5f, center = center)
        }

        FingerEffect.NEON_RETICLE -> {
            drawCircle(color = AccentCyan.copy(alpha = 0.85f), radius = 22f, center = center, style = Stroke(2f))
            drawLine(color = AccentCyan, start = Offset(center.x - 28f, center.y), end = Offset(center.x - 8f, center.y), strokeWidth = 2f)
            drawLine(color = AccentCyan, start = Offset(center.x + 8f, center.y), end = Offset(center.x + 28f, center.y), strokeWidth = 2f)
            drawLine(color = AccentCyan, start = Offset(center.x, center.y - 28f), end = Offset(center.x, center.y - 8f), strokeWidth = 2f)
            drawLine(color = AccentCyan, start = Offset(center.x, center.y + 8f), end = Offset(center.x, center.y + 28f), strokeWidth = 2f)
            drawCircle(color = Color.White, radius = 4f, center = center)
        }

        FingerEffect.FIRE_HEARTS -> {
            for (i in 0 until 4) {
                val angle = (i * Math.PI / 2.0 + pulse * Math.PI).toFloat()
                val dist = 18f + (i % 2) * 8f
                val hx = center.x + cos(angle) * dist
                val hy = center.y + sin(angle) * dist
                drawHeart(Offset(hx, hy), size = 11f, color = Color(0xFFFF1493).copy(alpha = 0.85f))
            }
            drawCircle(color = Color(0xFFFFB6C1), radius = 6f, center = center)
            drawCircle(color = Color.White, radius = 3f, center = center)
        }

        FingerEffect.MINIMAL_DOT -> {
            drawCircle(color = Color(0x66FFFFFF), radius = 12f, center = center)
            drawCircle(color = Color.White, radius = 4f, center = center)
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
