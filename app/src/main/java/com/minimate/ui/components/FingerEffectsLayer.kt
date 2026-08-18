package com.minimate.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.touchpad.engine.TouchPoint
import com.minimate.touchpad.model.FingerEffect
import com.minimate.ui.theme.AccentCyan
import com.minimate.ui.theme.AccentPink
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun FingerEffectsLayer(
    touchPoints: List<TouchPoint>,
    effect: FingerEffect = FingerEffect.CHERRY_PETALS,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!enabled || touchPoints.isEmpty()) return

    val transition = rememberInfiniteTransition(label = "FxPulse")
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
        touchPoints.forEach { pt ->
            drawSingleFingerEffect(
                effect = effect,
                center = Offset(pt.x, pt.y),
                pulse = pulse
            )
        }

        // Multi-point interactions
        if (touchPoints.size >= 2) {
            when (effect) {
                FingerEffect.PLASMA_LIGHTNING -> drawPlasmaArcs(touchPoints, pulse)
                FingerEffect.RAINBOW_RIBBON -> drawRainbowConnections(touchPoints, pulse)
                else -> {}
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

    val icon = getEffectPreviewIcon(effect)

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
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(AccentPink.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = AccentPink, modifier = Modifier.size(13.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = effect.displayName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

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
                    text = "Live Effect Preview",
                    color = AccentCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun getEffectPreviewIcon(effect: FingerEffect): ImageVector {
    return when (effect) {
        FingerEffect.CHERRY_PETALS -> Icons.Default.AutoAwesome
        FingerEffect.BUBBLE_SPLASH -> Icons.Default.Water
        FingerEffect.CAT_PAW_PRINTS -> Icons.Default.Pets
        FingerEffect.STAR_GLITTER -> Icons.Default.Star
        FingerEffect.RAINBOW_RIBBON -> Icons.Default.Waves
        FingerEffect.WATER_RIPPLES -> Icons.Default.Water
        FingerEffect.PLASMA_LIGHTNING -> Icons.Default.AutoAwesome
        FingerEffect.NEON_RETICLE -> Icons.Default.Tune
        FingerEffect.FIRE_HEARTS -> Icons.Default.AutoAwesome
        FingerEffect.MINIMAL_DOT -> Icons.Default.Grain
    }
}

// ===== ACTUAL REAL VISUAL EFFECTS =====

fun DrawScope.drawSingleFingerEffect(
    effect: FingerEffect,
    center: Offset,
    pulse: Float
) {
    when (effect) {
        FingerEffect.CHERRY_PETALS -> drawSakuraAura(center, pulse)
        FingerEffect.BUBBLE_SPLASH -> drawLiquidBubbleField(center, pulse)
        FingerEffect.CAT_PAW_PRINTS -> drawGlowingPawStamp(center, pulse)
        FingerEffect.STAR_GLITTER -> drawPrismaticStarBurst(center, pulse)
        FingerEffect.RAINBOW_RIBBON -> drawChromaticHalo(center, pulse)
        FingerEffect.WATER_RIPPLES -> drawShockwaveRipples(center, pulse)
        FingerEffect.PLASMA_LIGHTNING -> drawPlasmaCore(center, pulse)
        FingerEffect.NEON_RETICLE -> drawHolographicReticle(center, pulse)
        FingerEffect.FIRE_HEARTS -> drawEmberHearts(center, pulse)
        FingerEffect.MINIMAL_DOT -> drawGlassDot(center, pulse)
    }
}

/**
 * Sakura Aura: Soft radiating glow with orbiting petal shapes that warp light around them.
 */
private fun DrawScope.drawSakuraAura(center: Offset, pulse: Float) {
    // Outer aura glow that bleeds into the wallpaper
    val auraRadius = 55f + sin(pulse * PI.toFloat() * 2f) * 8f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x55FFB7B2),
                Color(0x33FF69B4),
                Color(0x11FF1493),
                Color.Transparent
            ),
            center = center,
            radius = auraRadius
        ),
        radius = auraRadius,
        center = center,
        blendMode = BlendMode.Screen
    )

    // Orbiting petals with proper geometry
    for (i in 0 until 6) {
        val angle = (i * PI.toFloat() * 2f / 6f) + pulse * PI.toFloat()
        val dist = 22f + sin(pulse * PI.toFloat() * 2f + i * 1.2f) * 7f
        val px = center.x + cos(angle) * dist
        val py = center.y + sin(angle) * dist
        val petalAlpha = (0.7f + sin(pulse * PI.toFloat() * 4f + i) * 0.3f).coerceIn(0f, 1f)

        // Each petal is an elongated ellipse rotated along its orbit
        rotate(degrees = Math.toDegrees(angle.toDouble()).toFloat() + 90f, pivot = Offset(px, py)) {
            drawOval(
                color = Color(0xFFFF69B4).copy(alpha = petalAlpha),
                topLeft = Offset(px - 5f, py - 9f),
                size = Size(10f, 18f)
            )
            drawOval(
                color = Color(0xFFFFB7B2).copy(alpha = petalAlpha * 0.7f),
                topLeft = Offset(px - 3f, py - 6f),
                size = Size(6f, 12f)
            )
        }
    }

    // Hot center bead
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xDDFFF0F5), Color(0x88FF69B4), Color.Transparent),
            center = center,
            radius = 10f
        ),
        radius = 10f,
        center = center,
        blendMode = BlendMode.Screen
    )
}

/**
 * Liquid Bubble Field: Translucent refractive soap bubbles with animated highlight beads.
 */
private fun DrawScope.drawLiquidBubbleField(center: Offset, pulse: Float) {
    // Ambient refraction haze
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0x2289CFF0), Color(0x1100BFFF), Color.Transparent),
            center = center,
            radius = 60f
        ),
        radius = 60f,
        center = center,
        blendMode = BlendMode.Screen
    )

    for (i in 0..3) {
        val phase = (pulse + i * 0.25f) % 1f
        val r = 12f + phase * 40f
        val alpha = ((1f - phase) * 0.75f).coerceIn(0f, 1f)
        val bubbleCol = if (i % 2 == 0) Color(0xFF89CFF0) else Color(0xFFFF85A1)

        // Bubble rim with iridescent tint
        drawCircle(
            color = bubbleCol.copy(alpha = alpha),
            radius = r,
            center = center,
            style = Stroke(2.5f + (1f - phase) * 1.5f)
        )

        // Specular highlight bead sliding along the rim
        val highlightAngle = pulse * PI.toFloat() * 4f + i * 1.5f
        val hx = center.x + cos(highlightAngle) * r * 0.55f - r * 0.3f
        val hy = center.y + sin(highlightAngle) * r * 0.35f - r * 0.3f
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.9f),
            radius = r * 0.15f + 1.5f,
            center = Offset(hx, hy)
        )
    }

    // Center lens
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xAAFFFFFF), Color(0x4489CFF0), Color.Transparent),
            center = center,
            radius = 8f
        ),
        radius = 8f,
        center = center,
        blendMode = BlendMode.Screen
    )
}

/**
 * Glowing Paw Stamp: Full paw geometry with warm glow that blends with the wallpaper.
 */
private fun DrawScope.drawGlowingPawStamp(center: Offset, pulse: Float) {
    val breathe = 1f + sin(pulse * PI.toFloat() * 2f) * 0.1f
    val glowAlpha = (0.3f + sin(pulse * PI.toFloat() * 4f) * 0.15f).coerceIn(0f, 1f)

    // Warm glow aura bleeding into wallpaper
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0x44FF758F), Color(0x22FFB3C1), Color.Transparent),
            center = center,
            radius = 50f
        ),
        radius = 50f,
        center = center,
        blendMode = BlendMode.Screen
    )

    val pawColor = Color(0xFFFF758F)
    val innerColor = Color(0xFFFFB3C1)
    val s = breathe

    // Main palm pad
    drawOval(color = pawColor.copy(alpha = 0.9f), topLeft = Offset(center.x - 14f * s, center.y - 6f * s), size = Size(28f * s, 22f * s))
    drawOval(color = innerColor.copy(alpha = 0.6f), topLeft = Offset(center.x - 10f * s, center.y - 4f * s), size = Size(20f * s, 16f * s))

    // 4 toe beans with individual glow
    val toes = listOf(
        Offset(-12f, -17f) to 5f,
        Offset(-4f, -22f) to 5.5f,
        Offset(4f, -22f) to 5.5f,
        Offset(12f, -17f) to 5f
    )
    toes.forEach { (off, r) ->
        val toeCenter = Offset(center.x + off.x * s, center.y + off.y * s)
        drawCircle(color = pawColor.copy(alpha = 0.85f), radius = r * s, center = toeCenter)
        drawCircle(
            color = Color(0x33FF69B4),
            radius = r * s + 4f,
            center = toeCenter,
            blendMode = BlendMode.Screen
        )
    }
}

/**
 * Prismatic Star Burst: Multi-pointed star geometry with chromatic shimmer trails.
 */
private fun DrawScope.drawPrismaticStarBurst(center: Offset, pulse: Float) {
    // Chromatic dispersion aura
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0x33FFD166), Color(0x2206D6A0), Color(0x11EF476F), Color.Transparent),
            center = center,
            radius = 55f
        ),
        radius = 55f,
        center = center,
        blendMode = BlendMode.Screen
    )

    // Orbiting multi-colored stars with trail
    for (i in 0 until 8) {
        val angle = (i * PI.toFloat() * 2f / 8f) + pulse * PI.toFloat() * 2.5f
        val dist = 20f + (i % 3) * 7f
        val px = center.x + cos(angle) * dist
        val py = center.y + sin(angle) * dist
        val starCol = when (i % 4) {
            0 -> Color(0xFFFFD166)
            1 -> Color(0xFF06D6A0)
            2 -> Color(0xFFEF476F)
            else -> Color(0xFF118AB2)
        }

        // Star with 4 points
        val starPath = Path().apply {
            for (j in 0 until 8) {
                val sa = j * PI.toFloat() / 4f - PI.toFloat() / 2f
                val sr = if (j % 2 == 0) 5.5f else 2.5f
                val sx = px + cos(sa) * sr
                val sy = py + sin(sa) * sr
                if (j == 0) moveTo(sx, sy) else lineTo(sx, sy)
            }
            close()
        }
        drawPath(starPath, color = starCol.copy(alpha = 0.85f), style = Fill)

        // Light trail behind each star
        val trailAngle = angle - 0.4f
        val tx = center.x + cos(trailAngle) * dist * 0.85f
        val ty = center.y + sin(trailAngle) * dist * 0.85f
        drawCircle(color = starCol.copy(alpha = 0.3f), radius = 3f, center = Offset(tx, ty))
    }

    // Hot white center flash
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xDDFFFFFF), Color(0x44FFD166), Color.Transparent),
            center = center,
            radius = 9f
        ),
        radius = 9f,
        center = center,
        blendMode = BlendMode.Screen
    )
}

/**
 * Chromatic Halo: Concentric iridescent rings with prismatic light separation.
 */
private fun DrawScope.drawChromaticHalo(center: Offset, pulse: Float) {
    val colors = listOf(
        Color(0xFFFF595E), Color(0xFFFFCA3A), Color(0xFF8AC926),
        Color(0xFF1982C4), Color(0xFF6A4C93)
    )

    // Light bleeding aura
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0x22FFFFFF), Color(0x11FFCA3A), Color.Transparent),
            center = center,
            radius = 65f
        ),
        radius = 65f,
        center = center,
        blendMode = BlendMode.Screen
    )

    colors.forEachIndexed { idx, col ->
        val phaseOffset = pulse * PI.toFloat() * 2f + idx * 0.8f
        val r = 14f + idx * 8f + sin(phaseOffset) * 4f
        val weight = 2.2f + sin(phaseOffset + 1f) * 0.8f
        drawCircle(
            color = col.copy(alpha = 0.75f),
            radius = r,
            center = center,
            style = Stroke(weight),
            blendMode = BlendMode.Screen
        )
    }

    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xBBFFFFFF), Color(0x448AC926), Color.Transparent),
            center = center,
            radius = 8f
        ),
        radius = 8f,
        center = center,
        blendMode = BlendMode.Screen
    )
}

/**
 * Shockwave Ripples: Concentric expanding rings with velocity-based distortion.
 */
private fun DrawScope.drawShockwaveRipples(center: Offset, pulse: Float) {
    // Ambient aquatic glow
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0x2200B4D8), Color(0x1190E0EF), Color.Transparent),
            center = center,
            radius = 70f
        ),
        radius = 70f,
        center = center,
        blendMode = BlendMode.Screen
    )

    for (i in 0..3) {
        val phase = (pulse + i * 0.25f) % 1f
        val r = 10f + phase * 55f
        val alpha = ((1f - phase).pow(1.5f) * 0.8f).coerceIn(0f, 1f)
        val width = 2.5f * (1f - phase) + 0.5f

        drawCircle(
            color = Color(0xFF00B4D8).copy(alpha = alpha),
            radius = r,
            center = center,
            style = Stroke(width)
        )

        // Inner bright edge on each wave
        if (phase < 0.6f) {
            drawCircle(
                color = Color(0xFFE0FBFC).copy(alpha = alpha * 0.5f),
                radius = r - 1.5f,
                center = center,
                style = Stroke(0.8f)
            )
        }
    }

    // Bright center drop impact
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xCCE0FBFC), Color(0x4400B4D8), Color.Transparent),
            center = center,
            radius = 7f
        ),
        radius = 7f,
        center = center,
        blendMode = BlendMode.Screen
    )
}

/**
 * Plasma Core: Pulsing energetic nucleus with electric aura field.
 */
private fun DrawScope.drawPlasmaCore(center: Offset, pulse: Float) {
    val coreRadius = 6f + sin(pulse * PI.toFloat() * 6f) * 2f

    // Outer energy field
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0x447B2CBF), Color(0x2200F5D4), Color.Transparent),
            center = center,
            radius = 55f
        ),
        radius = 55f,
        center = center,
        blendMode = BlendMode.Screen
    )

    // Spinning energy arcs
    for (i in 0 until 4) {
        val angle = (i * PI.toFloat() / 2f) + pulse * PI.toFloat() * 3f
        val arcLen = 18f + sin(pulse * PI.toFloat() * 4f + i) * 6f
        val ax = center.x + cos(angle) * arcLen
        val ay = center.y + sin(angle) * arcLen
        drawLine(
            color = Color(0xCC00F5D4),
            start = center,
            end = Offset(ax, ay),
            strokeWidth = 1.8f,
            blendMode = BlendMode.Screen
        )
        drawCircle(color = Color(0xDDFFFFFF), radius = 2.5f, center = Offset(ax, ay))
    }

    // Bright plasma core
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xEEFFFFFF), Color(0xBB00F5D4), Color(0x557B2CBF), Color.Transparent),
            center = center,
            radius = coreRadius + 5f
        ),
        radius = coreRadius + 5f,
        center = center,
        blendMode = BlendMode.Screen
    )
    drawCircle(color = Color.White, radius = coreRadius, center = center)
}

/**
 * Holographic Reticle: Precision tech crosshair with scanning sweep and distance indicators.
 */
private fun DrawScope.drawHolographicReticle(center: Offset, pulse: Float) {
    val sweepAngle = pulse * 360f

    // Scanning sweep glow
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0x2200E5FF), Color(0x1100B4D8), Color.Transparent),
            center = center,
            radius = 50f
        ),
        radius = 50f,
        center = center,
        blendMode = BlendMode.Screen
    )

    // Outer targeting ring
    drawCircle(
        color = AccentCyan.copy(alpha = 0.7f),
        radius = 28f,
        center = center,
        style = Stroke(1.8f)
    )

    // Inner precision ring
    drawCircle(
        color = AccentCyan.copy(alpha = 0.5f),
        radius = 16f,
        center = center,
        style = Stroke(1.2f)
    )

    // Crosshair lines with gaps
    val armLen = 35f
    val gapInner = 10f
    val gapOuter = 32f
    val lineAlpha = 0.8f
    // Horizontal
    drawLine(color = AccentCyan.copy(alpha = lineAlpha), start = Offset(center.x - armLen, center.y), end = Offset(center.x - gapInner, center.y), strokeWidth = 1.5f)
    drawLine(color = AccentCyan.copy(alpha = lineAlpha), start = Offset(center.x + gapInner, center.y), end = Offset(center.x + armLen, center.y), strokeWidth = 1.5f)
    // Vertical
    drawLine(color = AccentCyan.copy(alpha = lineAlpha), start = Offset(center.x, center.y - armLen), end = Offset(center.x, center.y - gapInner), strokeWidth = 1.5f)
    drawLine(color = AccentCyan.copy(alpha = lineAlpha), start = Offset(center.x, center.y + gapInner), end = Offset(center.x, center.y + armLen), strokeWidth = 1.5f)

    // Rotating sweep indicator
    val sweepRad = Math.toRadians(sweepAngle.toDouble()).toFloat()
    val sx = center.x + cos(sweepRad) * 28f
    val sy = center.y + sin(sweepRad) * 28f
    drawCircle(color = Color.White, radius = 2.5f, center = Offset(sx, sy))

    // Center dot
    drawCircle(color = AccentCyan, radius = 3f, center = center)
}

/**
 * Ember Hearts: Warm glowing heart shapes with fire particle trails.
 */
private fun DrawScope.drawEmberHearts(center: Offset, pulse: Float) {
    // Warm glow aura
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0x33FF1493), Color(0x22FF69B4), Color.Transparent),
            center = center,
            radius = 50f
        ),
        radius = 50f,
        center = center,
        blendMode = BlendMode.Screen
    )

    for (i in 0 until 5) {
        val angle = (i * PI.toFloat() * 2f / 5f) + pulse * PI.toFloat() * 1.5f
        val dist = 18f + (i % 2) * 9f + sin(pulse * PI.toFloat() * 3f + i) * 3f
        val hx = center.x + cos(angle) * dist
        val hy = center.y + sin(angle) * dist
        val heartAlpha = (0.75f + sin(pulse * PI.toFloat() * 4f + i * 1.3f) * 0.25f).coerceIn(0f, 1f)
        val heartSize = 10f + sin(pulse * PI.toFloat() * 2f + i) * 2f

        drawHeartShape(Offset(hx, hy), size = heartSize, color = Color(0xFFFF1493).copy(alpha = heartAlpha))

        // Ember trail sparks
        val trailAngle = angle - 0.5f
        val tx = center.x + cos(trailAngle) * dist * 0.7f
        val ty = center.y + sin(trailAngle) * dist * 0.7f
        drawCircle(color = Color(0x55FF6B6B), radius = 2.5f, center = Offset(tx, ty), blendMode = BlendMode.Screen)
    }

    // Warm center glow
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xAAFFB6C1), Color(0x44FF69B4), Color.Transparent),
            center = center,
            radius = 10f
        ),
        radius = 10f,
        center = center,
        blendMode = BlendMode.Screen
    )
}

/**
 * Glass Dot: Minimal refractive glass bead with subtle ambient light warp.
 */
private fun DrawScope.drawGlassDot(center: Offset, pulse: Float) {
    // Very subtle ambient glow
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0x1AFFFFFF), Color(0x0DFFFFFF), Color.Transparent),
            center = center,
            radius = 30f
        ),
        radius = 30f,
        center = center,
        blendMode = BlendMode.Screen
    )

    // Glass body
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0x88FFFFFF), Color(0x33FFFFFF), Color(0x11FFFFFF)),
            center = Offset(center.x - 2f, center.y - 2f),
            radius = 9f
        ),
        radius = 9f,
        center = center
    )

    // Specular highlight
    drawCircle(color = Color(0xCCFFFFFF), radius = 3f, center = Offset(center.x - 2.5f, center.y - 3f))
}

// ===== MULTI-TOUCH INTERACTIONS =====

private fun DrawScope.drawPlasmaArcs(touchPoints: List<TouchPoint>, pulse: Float) {
    for (i in 0 until touchPoints.size - 1) {
        val p1 = Offset(touchPoints[i].x, touchPoints[i].y)
        val p2 = Offset(touchPoints[i + 1].x, touchPoints[i + 1].y)

        // Electric arc with jitter
        val mid = Offset(
            (p1.x + p2.x) / 2f + sin(pulse * PI.toFloat() * 8f) * 14f,
            (p1.y + p2.y) / 2f + cos(pulse * PI.toFloat() * 6f) * 10f
        )

        // Main arc
        drawLine(color = Color(0xBB00F5D4), start = p1, end = mid, strokeWidth = 2f, blendMode = BlendMode.Screen)
        drawLine(color = Color(0xBB00F5D4), start = mid, end = p2, strokeWidth = 2f, blendMode = BlendMode.Screen)

        // Glow around midpoint
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0x88FFFFFF), Color(0x4400F5D4), Color.Transparent),
                center = mid,
                radius = 12f
            ),
            radius = 12f,
            center = mid,
            blendMode = BlendMode.Screen
        )
    }
}

private fun DrawScope.drawRainbowConnections(touchPoints: List<TouchPoint>, pulse: Float) {
    val colors = listOf(Color(0xFFFF595E), Color(0xFFFFCA3A), Color(0xFF8AC926), Color(0xFF1982C4), Color(0xFF6A4C93))
    for (i in 0 until touchPoints.size - 1) {
        val p1 = Offset(touchPoints[i].x, touchPoints[i].y)
        val p2 = Offset(touchPoints[i + 1].x, touchPoints[i + 1].y)
        colors.forEachIndexed { idx, col ->
            val offset = (idx - 2) * 3f
            drawLine(
                color = col.copy(alpha = 0.5f),
                start = Offset(p1.x, p1.y + offset),
                end = Offset(p2.x, p2.y + offset),
                strokeWidth = 1.8f,
                blendMode = BlendMode.Screen
            )
        }
    }
}

private fun DrawScope.drawHeartShape(center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y + size * 0.35f)
        cubicTo(
            center.x - size * 0.65f, center.y - size * 0.15f,
            center.x - size * 0.65f, center.y - size * 0.75f,
            center.x, center.y - size * 0.35f
        )
        cubicTo(
            center.x + size * 0.65f, center.y - size * 0.75f,
            center.x + size * 0.65f, center.y - size * 0.15f,
            center.x, center.y + size * 0.35f
        )
        close()
    }
    drawPath(path, color = color)
}
