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
import com.minimate.ui.theme.Black
import kotlin.math.cos
import kotlin.math.sin

private const val COSMIC_WARP_AGSL = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float2 uTouchPos;
    uniform float uTouchActive;

    half4 main(in float2 fragCoord) {
        float2 uv = (fragCoord - 0.5 * iResolution.xy) / iResolution.y;
        float2 touch = (uTouchPos - 0.5 * iResolution.xy) / iResolution.y;
        
        float distToTouch = length(uv - touch);
        float warp = exp(-distToTouch * 4.0) * uTouchActive * 0.35;
        float2 warpedUv = uv - normalize(uv - touch + 0.001) * warp;
        
        float r = length(warpedUv);
        float a = atan(warpedUv.y, warpedUv.x) + iTime * 0.25;
        
        float rings = sin(r * 24.0 - iTime * 1.5 + sin(a * 4.0) * 1.2);
        rings = smoothstep(0.1, 0.9, rings) * exp(-r * 2.2);
        
        half3 deepSpace = half3(0.02, 0.025, 0.05);
        half3 neonCyan = half3(0.0, 0.9, 1.0);
        half3 neonPurple = half3(0.65, 0.15, 0.95);
        
        half3 color = deepSpace + mix(neonCyan, neonPurple, sin(a + iTime) * 0.5 + 0.5) * rings * 0.6;
        
        // Touch luminous glow
        if (uTouchActive > 0.01) {
            float touchGlow = exp(-distToTouch * 6.0) * uTouchActive;
            color += mix(half3(0.0, 0.8, 1.0), half3(1.0, 0.2, 0.8), sin(iTime * 2.0) * 0.5 + 0.5) * touchGlow * 0.8;
        }
        
        return half4(color, 1.0);
    }
"""

private const val FLUID_AURORA_AGSL = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float2 uTouchPos;
    uniform float uTouchActive;

    half4 main(in float2 fragCoord) {
        float2 uv = fragCoord / iResolution.xy;
        float2 touchUv = uTouchPos / iResolution.xy;
        float time = iTime * 0.45;
        
        float distTouch = length(uv - touchUv);
        float touchRipple = sin(distTouch * 25.0 - iTime * 4.0) * exp(-distTouch * 4.0) * uTouchActive * 0.15;
        
        float wave1 = sin(uv.x * 4.0 + time + touchRipple) * 0.3;
        float wave2 = cos(uv.y * 3.5 - time * 0.7) * 0.25;
        float dist = length(uv - float2(0.5 + wave1 * 0.2, 0.5 + wave2 * 0.2));
        
        half3 col1 = half3(0.03, 0.04, 0.09);
        half3 col2 = half3(0.0, 0.75, 0.85);
        half3 col3 = half3(0.55, 0.10, 0.90);
        
        float blend = smoothstep(0.85, 0.1, dist);
        half3 finalColor = mix(col1, mix(col2, col3, sin(time + uv.x * 2.0) * 0.5 + 0.5), blend * 0.5);
        
        if (uTouchActive > 0.01) {
            finalColor += half3(0.0, 0.9, 0.8) * exp(-distTouch * 8.0) * uTouchActive * 0.7;
        }
        
        return half4(finalColor, 1.0);
    }
"""

private const val LIQUID_GLASS_AGSL = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float2 uTouchPos;
    uniform float uTouchActive;

    half4 main(in float2 fragCoord) {
        float2 uv = fragCoord / iResolution.xy;
        float2 touchUv = uTouchPos / iResolution.xy;
        float time = iTime * 0.35;
        
        float dist = length(uv - touchUv);
        float touchWarp = sin(dist * 30.0 - iTime * 5.0) * exp(-dist * 5.0) * uTouchActive * 0.08;
        uv += (uv - touchUv) * touchWarp;
        
        float d1 = sin(uv.x * 6.0 + time) * cos(uv.y * 6.0 + time);
        float d2 = cos(uv.x * 4.0 - time * 0.5) * sin(uv.y * 4.0 + time * 0.5);
        
        half3 base = mix(half3(0.05, 0.06, 0.10), half3(0.12, 0.22, 0.40), d1 * 0.5 + 0.5);
        half3 prism = mix(base, half3(0.4, 0.1, 0.6), d2 * 0.4 + 0.4);
        
        if (uTouchActive > 0.01) {
            prism += half3(0.3, 0.7, 1.0) * exp(-dist * 7.0) * uTouchActive * 0.5;
        }
        
        return half4(prism, 1.0);
    }
"""

private const val CYBER_GRID_AGSL = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float2 uTouchPos;
    uniform float uTouchActive;

    half4 main(in float2 fragCoord) {
        float2 uv = (fragCoord - 0.5 * iResolution.xy) / iResolution.y;
        float2 touch = (uTouchPos - 0.5 * iResolution.xy) / iResolution.y;
        
        float dist = length(uv - touch);
        float gridScale = 18.0;
        
        float2 gridUv = fract(uv * gridScale + float2(0.0, iTime * 0.2)) - 0.5;
        float line = smoothstep(0.05, 0.02, abs(gridUv.x)) + smoothstep(0.05, 0.02, abs(gridUv.y));
        
        half3 bg = half3(0.02, 0.02, 0.04);
        half3 gridCol = half3(0.0, 0.4, 0.7) * line * 0.4;
        
        // Touch reactive pulse
        if (uTouchActive > 0.01) {
            float pulse = sin(dist * 20.0 - iTime * 6.0) * exp(-dist * 4.0) * uTouchActive;
            gridCol += half3(0.0, 0.9, 1.0) * max(0.0, pulse) * 1.5;
            gridCol += half3(0.0, 1.0, 0.7) * exp(-dist * 8.0) * uTouchActive;
        }
        
        return half4(bg + gridCol, 1.0);
    }
"""

@Composable
fun BackgroundShaderCanvas(
    theme: BackgroundTheme,
    touchPoints: List<TouchPoint> = emptyList(),
    customImageUri: String? = null,
    dimRatio: Float = 0f,
    modifier: Modifier = Modifier
) {
    if (theme == BackgroundTheme.OLED_BLACK || dimRatio >= 0.99f) {
        Box(modifier = modifier.fillMaxSize().background(Black))
        return
    }

    if (theme == BackgroundTheme.CUSTOM_IMAGE && customImageUri != null) {
        CustomImageBackground(uriString = customImageUri, dimRatio = dimRatio, modifier = modifier)
        return
    }

    val transition = rememberInfiniteTransition(label = "ShaderTime")
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
        AgslInteractiveBackground(
            theme = theme,
            time = time,
            touchX = touchX,
            touchY = touchY,
            touchActive = touchActive,
            dimRatio = dimRatio,
            modifier = modifier
        )
    } else {
        FallbackInteractiveBackground(
            theme = theme,
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
private fun AgslInteractiveBackground(
    theme: BackgroundTheme,
    time: Float,
    touchX: Float,
    touchY: Float,
    touchActive: Float,
    dimRatio: Float,
    modifier: Modifier
) {
    val shaderCode = when (theme) {
        BackgroundTheme.COSMIC_WARP -> COSMIC_WARP_AGSL
        BackgroundTheme.FLUID_AURORA -> FLUID_AURORA_AGSL
        BackgroundTheme.LIQUID_GLASS -> LIQUID_GLASS_AGSL
        BackgroundTheme.CYBER_GRID -> CYBER_GRID_AGSL
        else -> COSMIC_WARP_AGSL
    }

    val shader = remember(theme) { RuntimeShader(shaderCode) }

    Canvas(modifier = modifier.fillMaxSize()) {
        shader.setFloatUniform("iResolution", size.width, size.height)
        shader.setFloatUniform("iTime", time)
        shader.setFloatUniform("uTouchPos", touchX, touchY)
        shader.setFloatUniform("uTouchActive", touchActive)

        drawRect(
            brush = ShaderBrush(shader),
            alpha = (1.0f - (dimRatio * 0.95f)).coerceIn(0f, 1f)
        )
    }
}

@Composable
private fun FallbackInteractiveBackground(
    theme: BackgroundTheme,
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

        // Base gradient
        val centerOffset = if (touchActive > 0.5f) {
            Offset(touchX, touchY)
        } else {
            Offset(w * 0.5f + cos(time) * 50f, h * 0.5f + sin(time) * 50f)
        }

        val colors = when (theme) {
            BackgroundTheme.COSMIC_WARP -> listOf(Color(0xFF00E5FF), Color(0xFF8B5CF6), Color(0xFF080911))
            BackgroundTheme.FLUID_AURORA -> listOf(Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFF060810))
            BackgroundTheme.LIQUID_GLASS -> listOf(Color(0xFF60A5FA), Color(0xFF6366F1), Color(0xFF0B0D14))
            BackgroundTheme.CYBER_GRID -> listOf(Color(0xFF06B6D4), Color(0xFF1E1B4B), Color(0xFF030712))
            else -> listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617))
        }

        drawRect(
            brush = Brush.radialGradient(
                colors = colors,
                center = centerOffset,
                radius = w * 0.9f
            ),
            alpha = alpha * 0.4f
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
        // Dark glass vignette overlay
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
