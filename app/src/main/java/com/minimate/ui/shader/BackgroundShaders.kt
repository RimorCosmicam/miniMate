package com.minimate.ui.shader

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.RuntimeShader
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.minimate.touchpad.engine.TouchPoint
import com.minimate.touchpad.model.BackgroundTheme
import com.minimate.touchpad.model.ThemeVariant
import com.minimate.ui.theme.Black
import kotlin.math.cos
import kotlin.math.sin

private const val AGSL_MASTER_SHADER = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float2 uTouchPos;
    uniform float uTouchActive;
    uniform int uTheme;
    uniform int uVariant;

    half4 main(in float2 fragCoord) {
        float2 uv = (fragCoord - 0.5 * iResolution.xy) / iResolution.y;
        float2 touch = (uTouchPos - 0.5 * iResolution.xy) / iResolution.y;
        float distToTouch = length(uv - touch);
        float time = iTime * 0.4;
        
        half3 colA = half3(0.0, 0.85, 1.0);
        half3 colB = half3(0.7, 0.15, 0.95);
        half3 bg = half3(0.02, 0.025, 0.04);
        
        // Variant Palettes
        if (uVariant == 1) {
            colA = half3(1.0, 0.55, 0.0);
            colB = half3(1.0, 0.1, 0.2);
            bg = half3(0.04, 0.02, 0.02);
        } else if (uVariant == 2) {
            colA = half3(0.0, 1.0, 0.65);
            colB = half3(0.1, 0.35, 0.95);
            bg = half3(0.01, 0.03, 0.03);
        }

        half3 finalCol = bg;

        if (uTheme == 0) { // COSMIC_WARP
            float warp = exp(-distToTouch * 4.0) * uTouchActive * 0.35;
            float2 warpedUv = uv - normalize(uv - touch + 0.001) * warp;
            float r = length(warpedUv);
            float a = atan(warpedUv.y, warpedUv.x) + time * 0.5;
            float rings = sin(r * 22.0 - time * 3.0 + sin(a * 4.0) * 1.2);
            rings = smoothstep(0.1, 0.9, rings) * exp(-r * 2.0);
            finalCol += mix(colA, colB, sin(a + time) * 0.5 + 0.5) * rings * 0.7;
            if (uTouchActive > 0.01) {
                finalCol += colA * exp(-distToTouch * 6.0) * uTouchActive * 0.8;
            }
        } else if (uTheme == 1) { // FLUID_AURORA
            float wave1 = sin(uv.x * 4.0 + time) * 0.3;
            float wave2 = cos(uv.y * 3.5 - time * 0.8) * 0.25;
            float dist = length(uv - float2(wave1 * 0.3, wave2 * 0.3));
            float blend = smoothstep(0.85, 0.1, dist);
            finalCol += mix(colA, colB, sin(time + uv.x * 2.0) * 0.5 + 0.5) * blend * 0.6;
            if (uTouchActive > 0.01) {
                finalCol += colA * exp(-distToTouch * 7.0) * uTouchActive * 0.6;
            }
        } else if (uTheme == 2) { // LIQUID_GLASS
            float touchWarp = sin(distToTouch * 25.0 - time * 8.0) * exp(-distToTouch * 5.0) * uTouchActive * 0.07;
            float2 guv = uv + (uv - touch) * touchWarp;
            float d1 = sin(guv.x * 6.0 + time) * cos(guv.y * 6.0 + time);
            finalCol += mix(colA * 0.4, colB, d1 * 0.5 + 0.5) * 0.5;
        } else if (uTheme == 3) { // CYBER_GRID
            float2 gridUv = fract(uv * 16.0 + float2(0.0, time * 0.3)) - 0.5;
            float line = smoothstep(0.06, 0.02, abs(gridUv.x)) + smoothstep(0.06, 0.02, abs(gridUv.y));
            finalCol += colA * line * 0.4;
            if (uTouchActive > 0.01) {
                float pulse = sin(distToTouch * 18.0 - time * 8.0) * exp(-distToTouch * 4.0) * uTouchActive;
                finalCol += colB * max(0.0, pulse) * 1.5 + colA * exp(-distToTouch * 8.0) * uTouchActive;
            }
        } else if (uTheme == 4) { // QUANTUM_WAVES
            float w1 = sin(length(uv - float2(-0.2, 0.0)) * 20.0 - time * 4.0);
            float w2 = sin(length(uv - float2(0.2, 0.0)) * 20.0 - time * 4.0);
            float wTouch = sin(distToTouch * 24.0 - time * 6.0) * uTouchActive;
            float wave = (w1 + w2 + wTouch * 1.5) * 0.33;
            finalCol += mix(colA, colB, wave * 0.5 + 0.5) * smoothstep(-0.2, 0.8, wave) * 0.6;
        } else if (uTheme == 5) { // VORTEX_NEBULA
            float r = length(uv);
            float angle = atan(uv.y, uv.x) + r * 5.0 - time;
            float spiral = sin(angle * 3.0) * exp(-r * 1.8);
            finalCol += mix(colA, colB, sin(angle + time) * 0.5 + 0.5) * smoothstep(0.0, 0.8, spiral) * 0.8;
            if (uTouchActive > 0.01) {
                finalCol += colA * exp(-distToTouch * 6.0) * uTouchActive;
            }
        } else if (uTheme == 6) { // BIOLUMINESCENCE
            float bio = sin(uv.x * 8.0 + sin(uv.y * 8.0 + time)) * sin(uv.y * 6.0 - time * 0.5);
            finalCol += mix(colA, colB, bio * 0.5 + 0.5) * smoothstep(0.2, 0.9, bio) * 0.5;
            if (uTouchActive > 0.01) {
                finalCol += colA * exp(-distToTouch * 5.0) * uTouchActive * 0.9;
            }
        } else if (uTheme == 7) { // GEOMETRIC_MORPH
            float2 p = abs(fract(uv * 10.0 + time * 0.1) - 0.5);
            float poly = max(p.x, p.y);
            float shape = smoothstep(0.48, 0.44, poly) - smoothstep(0.44, 0.40, poly);
            finalCol += mix(colA, colB, uv.x + 0.5) * shape * 0.6;
            if (uTouchActive > 0.01) {
                finalCol += colA * exp(-distToTouch * 6.0) * uTouchActive;
            }
        } else if (uTheme == 8) { // PARTICLE_STARDUST
            float2 sUv = uv + (touch - uv) * exp(-distToTouch * 4.0) * uTouchActive * 0.2;
            float star = sin(sUv.x * 40.0) * sin(sUv.y * 40.0);
            star = smoothstep(0.92, 0.98, star);
            finalCol += mix(colA, colB, sin(time + uv.y * 4.0) * 0.5 + 0.5) * star * 1.5;
            if (uTouchActive > 0.01) {
                finalCol += colA * exp(-distToTouch * 6.0) * uTouchActive;
            }
        } else if (uTheme == 9) { // MINIMAL_OLED
            if (uVariant == 1) { // Dark Titanium
                finalCol = half3(0.04, 0.045, 0.05) + colA * exp(-length(uv) * 2.0) * 0.08;
            } else if (uVariant == 2) { // Midnight Velvet
                finalCol = half3(0.02, 0.025, 0.04);
            } else { // Pitch Black
                finalCol = half3(0.0, 0.0, 0.0);
            }
        }
        
        return half4(finalCol, 1.0);
    }
"""

@Composable
fun BackgroundShaderCanvas(
    theme: BackgroundTheme,
    variant: ThemeVariant = ThemeVariant.VARIANT_A,
    touchPoints: List<TouchPoint> = emptyList(),
    customImageUri: String? = null,
    dimRatio: Float = 0f,
    modifier: Modifier = Modifier
) {
    if (theme == BackgroundTheme.MINIMAL_OLED && variant == ThemeVariant.VARIANT_A || dimRatio >= 0.99f) {
        Box(modifier = modifier.fillMaxSize().background(Black))
        return
    }

    if (theme == BackgroundTheme.CUSTOM_IMAGE && customImageUri != null) {
        CustomImageBackground(uriString = customImageUri, dimRatio = dimRatio, modifier = modifier)
        return
    }

    val transition = rememberInfiniteTransition(label = "MasterShaderTime")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Time"
    )

    val primaryTouch = touchPoints.firstOrNull()
    val touchX = primaryTouch?.x ?: -1000f
    val touchY = primaryTouch?.y ?: -1000f
    val touchActive = if (primaryTouch != null) 1.0f else 0.0f

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslMasterBackground(
            theme = theme,
            variant = variant,
            time = time,
            touchX = touchX,
            touchY = touchY,
            touchActive = touchActive,
            dimRatio = dimRatio,
            modifier = modifier
        )
    } else {
        FallbackMasterBackground(
            theme = theme,
            variant = variant,
            time = time,
            touchX = touchX,
            touchY = touchY,
            touchActive = touchActive,
            dimRatio = dimRatio,
            modifier = modifier
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AgslMasterBackground(
    theme: BackgroundTheme,
    variant: ThemeVariant,
    time: Float,
    touchX: Float,
    touchY: Float,
    touchActive: Float,
    dimRatio: Float,
    modifier: Modifier
) {
    val shader = remember { RuntimeShader(AGSL_MASTER_SHADER) }

    Canvas(modifier = modifier.fillMaxSize()) {
        shader.setFloatUniform("iResolution", size.width, size.height)
        shader.setFloatUniform("iTime", time)
        shader.setFloatUniform("uTouchPos", touchX, touchY)
        shader.setFloatUniform("uTouchActive", touchActive)
        shader.setIntUniform("uTheme", theme.ordinal.coerceIn(0, 9))
        shader.setIntUniform("uVariant", variant.index)

        drawRect(
            brush = ShaderBrush(shader),
            alpha = (1.0f - (dimRatio * 0.95f)).coerceIn(0f, 1f)
        )
    }
}

@Composable
private fun FallbackMasterBackground(
    theme: BackgroundTheme,
    variant: ThemeVariant,
    time: Float,
    touchX: Float,
    touchY: Float,
    touchActive: Float,
    dimRatio: Float,
    modifier: Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val alpha = (1.0f - (dimRatio * 0.95f)).coerceIn(0f, 1f)

        val centerOffset = if (touchActive > 0.5f) {
            Offset(touchX, touchY)
        } else {
            Offset(w * 0.5f + cos(time) * 40f, h * 0.5f + sin(time) * 40f)
        }

        val colors = when (variant) {
            ThemeVariant.VARIANT_A -> listOf(Color(0xFF00E5FF), Color(0xFF8B5CF6), Color(0xFF080911))
            ThemeVariant.VARIANT_B -> listOf(Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF140808))
            ThemeVariant.VARIANT_C -> listOf(Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFF06100E))
        }

        drawRect(
            brush = Brush.radialGradient(
                colors = colors,
                center = centerOffset,
                radius = w * 0.9f
            ),
            alpha = alpha * 0.45f
        )
    }
}

@Composable
private fun CustomImageBackground(
    uriString: String,
    dimRatio: Float,
    modifier: Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<Bitmap?>(null) }

    remember(uriString) {
        try {
            val uri = Uri.parse(uriString)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                bitmap = ImageDecoder.decodeBitmap(source)
            }
        } catch (_: Exception) {}
        true
    }

    Box(modifier = modifier.fillMaxSize().background(Black)) {
        bitmap?.let { b ->
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = (1f - dimRatio * 0.9f).coerceIn(0.1f, 1f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0x99000000), Color(0xEE000000))
                    )
                )
        )
    }
}
