package com.minimate.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.minimate.touchpad.engine.TouchPoint
import com.minimate.ui.theme.AccentCyan

/**
 * Interactive multi-touch visualizer rendering glowing luminous halo rings
 * and refractive touch spots directly under active fingers.
 */
@Composable
fun FingerEffectsLayer(
    touchPoints: List<TouchPoint>,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!enabled || touchPoints.isEmpty()) return

    Canvas(modifier = modifier.fillMaxSize()) {
        touchPoints.forEach { point ->
            val center = Offset(point.x, point.y)
            val baseRadius = 32f * point.pressure.coerceIn(0.8f, 1.8f)

            // Outer soft glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x5500E5FF),
                        Color(0x228B5CF6),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 2.2f
                ),
                radius = baseRadius * 2.2f,
                center = center
            )

            // Crisp inner halo ring
            drawCircle(
                color = AccentCyan.copy(alpha = 0.75f),
                radius = baseRadius,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // Center tactile dot
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 4.5f,
                center = center
            )
        }
    }
}
