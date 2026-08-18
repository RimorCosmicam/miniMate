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

private const val AGSL_CUTE_SHADER = """
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
        float time = iTime * 0.5;

        // Dynamic Color Palettes for each theme and variant
        half3 colA = half3(1.0, 0.7, 0.85);  // Soft Pink
        half3 colB = half3(0.6, 0.85, 1.0);  // Soft Sky Blue
        half3 bg   = half3(0.05, 0.03, 0.06); // Dark Pastel Base

        if (uTheme == 0) { // SAKURA_PETALS (Cherry Blossoms)
            if (uVariant == 0) { // Spring Sakura
                colA = half3(1.0, 0.45, 0.7); colB = half3(1.0, 0.85, 0.9); bg = half3(0.08, 0.03, 0.06);
            } else if (uVariant == 1) { // Midnight Bloom
                colA = half3(0.85, 0.2, 0.95); colB = half3(0.4, 0.6, 1.0); bg = half3(0.03, 0.02, 0.08);
            } else { // Golden Blossom
                colA = half3(1.0, 0.7, 0.4); colB = half3(1.0, 0.9, 0.6); bg = half3(0.07, 0.05, 0.02);
            }
        } else if (uTheme == 1) { // BUBBLE_POP (Cute Floating Bubbles)
            if (uVariant == 0) { // Cotton Candy
                colA = half3(0.4, 0.8, 1.0); colB = half3(1.0, 0.5, 0.8); bg = half3(0.04, 0.04, 0.08);
            } else if (uVariant == 1) { // Lemon Soda
                colA = half3(1.0, 0.9, 0.3); colB = half3(0.3, 0.9, 0.7); bg = half3(0.03, 0.06, 0.04);
            } else { // Grape Fizz
                colA = half3(0.75, 0.4, 1.0); colB = half3(0.9, 0.7, 1.0); bg = half3(0.06, 0.02, 0.08);
            }
        } else if (uTheme == 2) { // KAWAII_PAWS
            if (uVariant == 0) { // Calico Peach
                colA = half3(1.0, 0.6, 0.35); colB = half3(1.0, 0.85, 0.7); bg = half3(0.07, 0.04, 0.03);
            } else if (uVariant == 1) { // Berry Kitten
                colA = half3(1.0, 0.35, 0.55); colB = half3(1.0, 0.75, 0.85); bg = half3(0.08, 0.03, 0.05);
            } else { // Midnight Cat
                colA = half3(0.65, 0.7, 0.9); colB = half3(0.85, 0.75, 1.0); bg = half3(0.03, 0.03, 0.06);
            }
        } else if (uTheme == 3) { // RAINBOW_PASTEL
            if (uVariant == 0) { // Sunset Pastel
                colA = half3(1.0, 0.5, 0.6); colB = half3(1.0, 0.8, 0.4); bg = half3(0.06, 0.03, 0.04);
            } else if (uVariant == 1) { // Cloud 9
                colA = half3(0.45, 0.75, 1.0); colB = half3(0.6, 0.95, 0.75); bg = half3(0.03, 0.05, 0.07);
            } else { // Unicorn Aura
                colA = half3(1.0, 0.3, 0.8); colB = half3(0.4, 0.9, 0.95); bg = half3(0.05, 0.02, 0.07);
            }
        } else if (uTheme == 4) { // MATCHA_CAFE
            if (uVariant == 0) { // Matcha Latte
                colA = half3(0.45, 0.8, 0.45); colB = half3(0.95, 0.92, 0.8); bg = half3(0.03, 0.06, 0.03);
            } else if (uVariant == 1) { // Caramel Macchiato
                colA = half3(0.9, 0.65, 0.35); colB = half3(0.98, 0.9, 0.75); bg = half3(0.06, 0.04, 0.02);
            } else { // Taro Milk
                colA = half3(0.65, 0.55, 0.85); colB = half3(0.95, 0.92, 0.98); bg = half3(0.05, 0.03, 0.07);
            }
        } else if (uTheme == 5) { // RETRO_ARCADE
            if (uVariant == 0) { // Neon Cyber
                colA = half3(0.0, 1.0, 0.8); colB = half3(1.0, 0.2, 0.7); bg = half3(0.02, 0.02, 0.05);
            } else if (uVariant == 1) { // Game Boy Lime
                colA = half3(0.6, 0.85, 0.1); colB = half3(0.8, 0.95, 0.3); bg = half3(0.04, 0.07, 0.02);
            } else { // Synthwave Sunset
                colA = half3(1.0, 0.45, 0.0); colB = half3(0.7, 0.1, 0.9); bg = half3(0.06, 0.02, 0.05);
            }
        } else if (uTheme == 6) { // TROPICAL_OCEAN
            if (uVariant == 0) { // Maldives Aqua
                colA = half3(0.0, 0.8, 0.95); colB = half3(0.5, 0.95, 0.95); bg = half3(0.02, 0.05, 0.08);
            } else if (uVariant == 1) { // Sunset Beach
                colA = half3(1.0, 0.5, 0.2); colB = half3(1.0, 0.85, 0.35); bg = half3(0.07, 0.03, 0.02);
            } else { // Deep Lagoon
                colA = half3(0.0, 0.5, 0.9); colB = half3(0.0, 0.85, 0.85); bg = half3(0.01, 0.03, 0.07);
            }
        } else if (uTheme == 7) { // STRAWBERRY_MOCHI
            if (uVariant == 0) { // Fresh Berry
                colA = half3(1.0, 0.25, 0.4); colB = half3(1.0, 0.9, 0.9); bg = half3(0.08, 0.02, 0.04);
            } else if (uVariant == 1) { // Blueberry Mochi
                colA = half3(0.4, 0.35, 0.85); colB = half3(0.85, 0.7, 1.0); bg = half3(0.04, 0.02, 0.07);
            } else { // Banana Pudding
                colA = half3(1.0, 0.85, 0.35); colB = half3(1.0, 0.98, 0.85); bg = half3(0.07, 0.06, 0.02);
            }
        } else if (uTheme == 8) { // STARRY_GALAXY
            if (uVariant == 0) { // Starlight Blue
                colA = half3(0.5, 0.75, 1.0); colB = half3(1.0, 1.0, 1.0); bg = half3(0.02, 0.02, 0.06);
            } else if (uVariant == 1) { // Rose Twilight
                colA = half3(0.95, 0.45, 0.65); colB = half3(1.0, 0.85, 0.7); bg = half3(0.06, 0.02, 0.04);
            } else { // Emerald Starlight
                colA = half3(0.2, 0.9, 0.6); colB = half3(0.85, 1.0, 0.9); bg = half3(0.02, 0.05, 0.04);
            }
        } else if (uTheme == 9) { // CLEAN_MINIMAL
            if (uVariant == 0) { // Pure OLED
                return half4(0.0, 0.0, 0.0, 1.0);
            } else if (uVariant == 1) { // Charcoal Matte
                return half4(0.06, 0.06, 0.07, 1.0);
            } else { // Warm Muted Minimal
                return half4(0.07, 0.065, 0.06, 1.0);
            }
        }

        half3 finalCol = bg;

        // Theme Animations & Dynamics
        if (uTheme == 0) { // SAKURA_PETALS
            float p1 = sin(uv.x * 6.0 + time + uv.y * 3.0);
            float p2 = cos(uv.y * 5.0 - time * 0.8 + uv.x * 2.0);
            float petals = smoothstep(0.6, 0.95, sin(p1 * p2 * 8.0));
            finalCol += mix(colA, colB, petals) * petals * 0.8;
            if (uTouchActive > 0.01) {
                finalCol += colA * exp(-distToTouch * 5.0) * uTouchActive * 0.9;
            }
        } else if (uTheme == 1) { // BUBBLE_POP
            float2 bUv = uv * 4.0 + float2(sin(time * 0.5), time * 0.8);
            float2 grid = fract(bUv) - 0.5;
            float bubble = length(grid);
            float ring = smoothstep(0.35, 0.30, bubble) - smoothstep(0.30, 0.22, bubble);
            finalCol += mix(colA, colB, grid.x + 0.5) * ring * 0.85;
            if (uTouchActive > 0.01) {
                finalCol += colB * exp(-distToTouch * 6.0) * uTouchActive;
            }
        } else if (uTheme == 2) { // KAWAII_PAWS
            float p = sin(uv.x * 5.0 + time * 0.3) * cos(uv.y * 5.0);
            float pawGlow = smoothstep(0.4, 0.9, p);
            finalCol += mix(colA, colB, pawGlow) * pawGlow * 0.6;
            if (uTouchActive > 0.01) {
                finalCol += colA * exp(-distToTouch * 6.0) * uTouchActive * 1.2;
            }
        } else if (uTheme == 3) { // RAINBOW_PASTEL
            float w = sin(uv.x * 3.0 + uv.y * 3.0 + time);
            float wave = sin(w * 3.14 + time);
            finalCol += mix(colA, colB, wave * 0.5 + 0.5) * 0.6;
            if (uTouchActive > 0.01) {
                finalCol += mix(colB, colA, sin(time * 3.0) * 0.5 + 0.5) * exp(-distToTouch * 5.0) * uTouchActive;
            }
        } else if (uTheme == 4) { // MATCHA_CAFE
            float latte = sin(uv.x * 4.0 + sin(uv.y * 4.0 + time * 0.4));
            float foam = smoothstep(-0.2, 0.8, latte);
            finalCol += mix(colA * 0.5, colB, foam) * 0.55;
            if (uTouchActive > 0.01) {
                finalCol += colB * exp(-distToTouch * 6.0) * uTouchActive * 0.7;
            }
        } else if (uTheme == 5) { // RETRO_ARCADE
            float2 pUv = floor(uv * 20.0) / 20.0;
            float pixel = sin(pUv.x * 12.0 + time * 2.0) * cos(pUv.y * 12.0 - time);
            float gridLine = smoothstep(0.5, 0.9, pixel);
            finalCol += mix(colA, colB, pUv.y + 0.5) * gridLine * 0.7;
            if (uTouchActive > 0.01) {
                finalCol += colA * exp(-distToTouch * 7.0) * uTouchActive * 1.3;
            }
        } else if (uTheme == 6) { // TROPICAL_OCEAN
            float water = sin(uv.x * 8.0 + time) * cos(uv.y * 8.0 + time);
            water += sin((uv.x + uv.y) * 10.0 - time * 1.5) * 0.5;
            float caustic = smoothstep(0.2, 0.9, water);
            finalCol += mix(colA, colB, caustic) * caustic * 0.75;
            if (uTouchActive > 0.01) {
                float ripple = sin(distToTouch * 25.0 - time * 8.0) * exp(-distToTouch * 5.0) * uTouchActive;
                finalCol += colB * max(0.0, ripple) * 1.4;
            }
        } else if (uTheme == 7) { // STRAWBERRY_MOCHI
            float dist = length(uv);
            float squish = sin(dist * 12.0 - time * 2.0) * exp(-dist * 2.0);
            finalCol += mix(colA, colB, squish * 0.5 + 0.5) * 0.65;
            if (uTouchActive > 0.01) {
                finalCol += colA * exp(-distToTouch * 5.0) * uTouchActive;
            }
        } else if (uTheme == 8) { // STARRY_GALAXY
            float2 sUv = uv * 15.0;
            float star = sin(sUv.x * 10.0) * sin(sUv.y * 10.0);
            float twinkle = smoothstep(0.94, 0.99, star) * (sin(time * 3.0 + sUv.x) * 0.5 + 0.5);
            finalCol += mix(colA, colB, twinkle) * twinkle * 1.8;
            if (uTouchActive > 0.01) {
                finalCol += colA * exp(-distToTouch * 5.0) * uTouchActive;
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
    if (theme == BackgroundTheme.CLEAN_MINIMAL && variant == ThemeVariant.VARIANT_A || dimRatio >= 0.99f) {
        Box(modifier = modifier.fillMaxSize().background(Black))
        return
    }

    if (theme == BackgroundTheme.CUSTOM_IMAGE && customImageUri != null) {
        CustomImageBackground(uriString = customImageUri, dimRatio = dimRatio, modifier = modifier)
        return
    }

    val transition = rememberInfiniteTransition(label = "CuteShaderTime")
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
        AgslCuteBackground(
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
        FallbackCuteBackground(
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
private fun AgslCuteBackground(
    theme: BackgroundTheme,
    variant: ThemeVariant,
    time: Float,
    touchX: Float,
    touchY: Float,
    touchActive: Float,
    dimRatio: Float,
    modifier: Modifier
) {
    val shader = remember { RuntimeShader(AGSL_CUTE_SHADER) }

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
private fun FallbackCuteBackground(
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

        val colors = when (theme) {
            BackgroundTheme.SAKURA_PETALS -> listOf(Color(0xFFFFB7B2), Color(0xFFFF69B4), Color(0xFF1F0C16))
            BackgroundTheme.BUBBLE_POP -> listOf(Color(0xFF89CFF0), Color(0xFFFF6EC7), Color(0xFF0A0F1F))
            BackgroundTheme.KAWAII_PAWS -> listOf(Color(0xFFF4A261), Color(0xFFFF758F), Color(0xFF1E1018))
            BackgroundTheme.RAINBOW_PASTEL -> listOf(Color(0xFFFF9A8B), Color(0xFF70A6FF), Color(0xFF120A1C))
            BackgroundTheme.MATCHA_CAFE -> listOf(Color(0xFF588157), Color(0xFFF3E9D2), Color(0xFF0F1A0F))
            BackgroundTheme.RETRO_ARCADE -> listOf(Color(0xFF00F5D4), Color(0xFF7B2CBF), Color(0xFF050514))
            BackgroundTheme.TROPICAL_OCEAN -> listOf(Color(0xFF00B4D8), Color(0xFF90E0EF), Color(0xFF031926))
            BackgroundTheme.STRAWBERRY_MOCHI -> listOf(Color(0xFFE63946), Color(0xFFF1FAEE), Color(0xFF22080D))
            BackgroundTheme.STARRY_GALAXY -> listOf(Color(0xFF4EA8DE), Color(0xFFF72585), Color(0xFF08071E))
            else -> listOf(Color(0xFF1E1E24), Color(0xFF121216), Color(0xFF000000))
        }

        drawRect(
            brush = Brush.radialGradient(
                colors = colors,
                center = centerOffset,
                radius = w * 0.85f
            ),
            alpha = alpha * 0.5f
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
                        colors = listOf(Color.Transparent, Color(0x77000000), Color(0xDD000000))
                    )
                )
        )
    }
}
