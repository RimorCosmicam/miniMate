package com.minimate.ui.shader

import android.graphics.ImageDecoder
import android.graphics.RuntimeShader
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.minimate.touchpad.engine.TouchPoint
import com.minimate.touchpad.model.BackgroundTheme
import com.minimate.ui.theme.Black
import kotlin.math.cos
import kotlin.math.sin

private const val AGSL_MINIMATE_SHADER = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float2 uTouchPos;
    uniform float uTouchActive;
    uniform float uTheme;
    uniform float uVariant;

    // Fast pseudo-random hash
    float hash(float2 p) {
        return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453123);
    }

    // 2D Value Noise
    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float2 u = f * f * (3.0 - 2.0 * f);
        return mix(mix(hash(i + float2(0.0, 0.0)), hash(i + float2(1.0, 0.0)), u.x),
                   mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x), u.y);
    }

    // Voronoi Cellular Noise
    float voronoi(float2 p) {
        float2 n = floor(p);
        float2 f = fract(p);
        float md = 8.0;
        for (int j = -1; j <= 1; j++) {
            for (int i = -1; i <= 1; i++) {
                float2 g = float2(float(i), float(j));
                float2 o = float2(hash(n + g), hash(n + g + 13.7));
                o = 0.5 + 0.5 * sin(iTime * 1.6 + 6.2831 * o);
                float2 r = g + o - f;
                float d = dot(r, r);
                if (d < md) md = d;
            }
        }
        return sqrt(md);
    }

    half4 main(in float2 fragCoord) {
        float2 uv = (fragCoord - 0.5 * iResolution.xy) / iResolution.y;
        float2 touch = (uTouchPos - 0.5 * iResolution.xy) / iResolution.y;
        float distToTouch = length(uv - touch);
        float time = iTime * 0.75;

        int themeId = int(uTheme + 0.5);
        int varId = int(uVariant + 0.5);

        // 1. LIQUID CYBER CHROME (Liquid metallic mercury with environmental reflections)
        if (themeId == 0) {
            float2 p = uv * 3.2;
            float n1 = noise(p + float2(time * 0.35, -time * 0.25));
            float n2 = noise(p * 2.2 + n1 * 2.8 + time * 0.2);
            
            float2 norm = float2(
                noise(p + float2(0.05, 0.0) + n2) - noise(p - float2(0.05, 0.0) + n2),
                noise(p + float2(0.0, 0.05) + n2) - noise(p - float2(0.0, 0.05) + n2)
            ) * 4.0;
            
            float spec = pow(max(0.0, dot(normalize(float3(norm, 1.0)), normalize(float3(0.5, 0.8, 1.0)))), 16.0);
            float rim = 1.0 - max(0.0, dot(normalize(float3(norm, 1.0)), float3(0.0, 0.0, 1.0)));

            half3 baseCol, specCol;
            if (varId == 0) {
                // Mercury Silver
                baseCol = half3(0.12, 0.14, 0.18);
                specCol = half3(0.95, 0.98, 1.0);
            } else if (varId == 1) {
                // Cyber Neon Pink
                baseCol = half3(0.25, 0.02, 0.18);
                specCol = half3(1.0, 0.2, 0.75);
            } else {
                // Golden Amber
                baseCol = half3(0.22, 0.14, 0.02);
                specCol = half3(1.0, 0.85, 0.35);
            }

            half3 col = baseCol + specCol * spec * 1.5 + half3(0.2, 0.8, 1.0) * rim * 0.4;
            if (uTouchActive > 0.01) {
                float wave = sin(distToTouch * 32.0 - time * 14.0) * exp(-distToTouch * 6.0);
                col += specCol * max(0.0, wave) * 1.8 * uTouchActive;
            }
            return half4(col, 1.0);
        }

        // 2. BIOLUMINESCENT ABYSS (Deep ocean jellyfish glow and underwater scattering)
        else if (themeId == 1) {
            float2 p = uv * 3.5;
            float pulse = sin(time * 2.0 + length(p) * 2.0) * 0.5 + 0.5;
            float tentacles = sin(p.x * 6.0 + sin(p.y * 3.0 + time * 1.5) * 2.0);
            float glow = exp(-length(p) * 1.4) * (0.6 + 0.4 * pulse);

            half3 colA, colB;
            if (varId == 0) {
                colA = half3(0.0, 0.95, 0.9);
                colB = half3(0.05, 0.2, 0.8);
            } else if (varId == 1) {
                colA = half3(0.8, 0.1, 1.0);
                colB = half3(0.2, 0.0, 0.5);
            } else {
                colA = half3(1.0, 0.4, 0.5);
                colB = half3(0.8, 0.8, 0.1);
            }

            half3 bg = half3(0.01, 0.02, 0.05);
            half3 col = bg + mix(colB, colA, glow) * glow * 1.4 + colA * smoothstep(0.7, 1.0, tentacles) * glow * 0.8;

            if (uTouchActive > 0.01) {
                float shock = sin(distToTouch * 35.0 - time * 15.0) * exp(-distToTouch * 5.0);
                col += colA * max(0.0, shock) * 2.0 * uTouchActive;
            }
            return half4(col, 1.0);
        }

        // 3. HYPERDRIVE WARP TUNNEL (Relativistic space tunnel with light streaks)
        else if (themeId == 2) {
            float r = length(uv);
            float angle = atan(uv.y, uv.x);
            float z = 0.25 / (r + 0.02) + time * 2.5;
            float streaks = sin(angle * 16.0 + z * 8.0) * 0.5 + 0.5;
            float stars = hash(float2(floor(angle * 12.0), floor(z * 4.0)));
            float beam = pow(streaks, 6.0) * exp(-r * 0.8);

            half3 starCol;
            if (varId == 0) starCol = half3(0.1, 0.8, 1.0); // Photon Blue
            else if (varId == 1) starCol = half3(1.0, 0.1, 0.7); // Hyperspace Magenta
            else starCol = half3(1.0, 0.85, 0.3); // Starlight Gold

            half3 col = half3(0.02, 0.02, 0.05);
            col += starCol * beam * 1.8 + half3(1.0, 1.0, 1.0) * smoothstep(0.96, 1.0, stars) * 1.5;
            col += starCol * exp(-r * 3.5) * 1.2; // Core glow

            if (uTouchActive > 0.01) {
                col += starCol * exp(-distToTouch * 6.0) * uTouchActive * 1.6;
            }
            return half4(col, 1.0);
        }

        // 4. AURORA BOREALIS (Silky polar northern lights waving across night sky)
        else if (themeId == 3) {
            float2 p = uv * 2.5;
            float wave1 = sin(p.x * 2.0 + time * 0.8 + sin(p.y * 2.0));
            float wave2 = sin(p.x * 3.5 - time * 0.6 + cos(p.y * 1.5));
            float curtain = exp(-abs(p.y - (wave1 + wave2) * 0.25) * 4.0);

            half3 auroraA, auroraB;
            if (varId == 0) {
                auroraA = half3(0.0, 1.0, 0.5); // Emerald Polar
                auroraB = half3(0.3, 0.1, 0.9);
            } else if (varId == 1) {
                auroraA = half3(0.7, 0.1, 1.0); // Cosmic Violet
                auroraB = half3(0.1, 0.6, 1.0);
            } else {
                auroraA = half3(0.0, 0.9, 1.0); // Arctic Cyan
                auroraB = half3(0.1, 1.0, 0.8);
            }

            float star = hash(floor(uv * 50.0));
            half3 sky = half3(0.01, 0.02, 0.04) + half3(1.0, 1.0, 1.0) * smoothstep(0.985, 1.0, star) * 0.7;
            half3 col = sky + mix(auroraB, auroraA, curtain) * curtain * 1.6;

            if (uTouchActive > 0.01) {
                col += auroraA * exp(-distToTouch * 6.0) * uTouchActive * 1.4;
            }
            return half4(col, 1.0);
        }

        // 5. MOLTEN MAGMA FISSURES (Cracked tectonic plates with heat glow)
        else if (themeId == 4) {
            float v = voronoi(uv * 5.0 + time * 0.2);
            float fissure = smoothstep(0.18, 0.02, abs(v - 0.5));
            float heat = noise(uv * 6.0 - time * 0.4);

            half3 crust = half3(0.06, 0.02, 0.01);
            half3 magmaCore;
            if (varId == 0) magmaCore = half3(1.0, 0.35, 0.0); // Volcanic Fire
            else if (varId == 1) magmaCore = half3(0.8, 0.1, 1.0); // Plasma Purple
            else magmaCore = half3(1.0, 0.9, 0.5); // Solar White-Hot

            half3 col = crust + magmaCore * fissure * 2.0 + half3(1.0, 0.9, 0.2) * pow(fissure, 3.0) * 1.5;
            if (uTouchActive > 0.01) {
                col += magmaCore * exp(-distToTouch * 5.0) * uTouchActive * 2.2;
            }
            return half4(col, 1.0);
        }

        // 6. HOLOGRAPHIC PRISMATIC GLASS (Crystal optical refraction and rainbow sheen)
        else if (themeId == 5) {
            float2 p = uv * 3.5;
            float facet = sin(p.x * 3.0 + p.y * 2.0) * cos(p.y * 3.0 - p.x * 2.0);
            float prism = facet * 6.2831 + time;

            half3 col;
            if (varId == 0) {
                // Prism Rainbow
                col.r = sin(prism + 0.0) * 0.5 + 0.5;
                col.g = sin(prism + 2.09) * 0.5 + 0.5;
                col.b = sin(prism + 4.18) * 0.5 + 0.5;
            } else if (varId == 1) {
                // Opal Pearl
                col.r = sin(prism * 0.5) * 0.2 + 0.85;
                col.g = sin(prism * 0.5 + 2.0) * 0.2 + 0.85;
                col.b = sin(prism * 0.5 + 4.0) * 0.2 + 0.95;
            } else {
                // Obsidian Iridescent
                col.r = sin(prism) * 0.3 + 0.15;
                col.g = sin(prism + 2.0) * 0.3 + 0.25;
                col.b = sin(prism + 4.0) * 0.4 + 0.35;
            }

            if (uTouchActive > 0.01) {
                float wave = sin(distToTouch * 30.0 - time * 12.0) * exp(-distToTouch * 6.0);
                col += half3(1.0, 1.0, 1.0) * max(0.0, wave) * 1.5 * uTouchActive;
            }
            return half4(col, 1.0);
        }

        // 7. RETRO SYNTHWAVE 3D HORIZON (3D perspective grid floor & sun)
        else if (themeId == 6) {
            float2 p = uv;
            half3 col = half3(0.03, 0.02, 0.08);

            // Sun in upper half
            if (p.y < 0.05) {
                float dSun = length(p - float2(0.0, -0.08));
                if (dSun < 0.22) {
                    float sunGlow = smoothstep(0.22, 0.0, dSun);
                    float bars = sin(p.y * 120.0) * 0.5 + 0.5;
                    half3 sunCol = (varId == 2) ? half3(1.0, 0.2, 0.2) : half3(1.0, 0.6, 0.1);
                    col += sunCol * sunGlow * (bars > 0.3 ? 1.4 : 0.4);
                }
            }

            // 3D Perspective Grid in lower half
            if (p.y > 0.0) {
                float z = 1.0 / (p.y + 0.05);
                float x = p.x * z;
                float gridX = abs(fract(x * 1.8) - 0.5);
                float gridZ = abs(fract(z * 0.4 - time * 2.0) - 0.5);
                float lineMask = smoothstep(0.08, 0.0, min(gridX, gridZ));
                
                half3 gridCol;
                if (varId == 0) gridCol = half3(1.0, 0.1, 0.8);
                else if (varId == 1) gridCol = half3(0.0, 0.95, 0.95);
                else gridCol = half3(0.95, 0.2, 0.3);

                col += gridCol * lineMask * min(1.0, p.y * 3.5);
            }

            if (uTouchActive > 0.01) {
                col += half3(0.2, 0.8, 1.0) * exp(-distToTouch * 7.0) * uTouchActive * 1.6;
            }
            return half4(col, 1.0);
        }

        // 8. SAKURA WIND BREEZE (Swirling petals in atmospheric vortices)
        else if (themeId == 7) {
            half3 bg, petalCol;
            if (varId == 0) {
                bg = half3(0.08, 0.03, 0.06);
                petalCol = half3(1.0, 0.65, 0.8);
            } else if (varId == 1) {
                bg = half3(0.03, 0.02, 0.08);
                petalCol = half3(0.75, 0.5, 1.0);
            } else {
                bg = half3(0.09, 0.04, 0.02);
                petalCol = half3(1.0, 0.8, 0.4);
            }

            half3 col = bg;
            float2 pUv = uv * 3.8;
            pUv.y += time * 0.9;
            pUv.x += sin(pUv.y * 1.4 + time) * 0.45;
            
            float2 cell = fract(pUv) - 0.5;
            float2 cellId = floor(pUv);
            float angle = atan(cell.y, cell.x) + time * (hash(cellId) - 0.5) * 2.5;
            float r = length(cell);
            float flower = 0.18 + 0.06 * cos(5.0 * angle);
            float petalMask = smoothstep(flower + 0.02, flower - 0.01, r);
            col += petalCol * petalMask * 0.9;

            if (uTouchActive > 0.01) {
                col += petalCol * exp(-distToTouch * 6.0) * uTouchActive * 1.5;
            }
            return half4(col, 1.0);
        }

        // 9. CYBER MATRIX RAIN (Cascading digital glyph streams)
        else if (themeId == 8) {
            float2 pUv = uv;
            float colId = floor((pUv.x + 0.5) * 24.0);
            float speed = 1.2 + hash(float2(colId, 0.0)) * 2.0;
            float yPos = pUv.y + time * speed;
            float rowId = floor(yPos * 20.0);
            float glyph = hash(float2(colId, rowId));
            float trail = fract(yPos * 0.5);

            half3 tint;
            if (varId == 0) tint = half3(0.0, 1.0, 0.35); // Phosphor Green
            else if (varId == 1) tint = half3(1.0, 0.65, 0.1); // Amber CRT
            else tint = half3(0.1, 0.8, 1.0); // Cyan Ghost

            half3 col = half3(0.01, 0.02, 0.03);
            if (glyph > 0.4) {
                col += tint * (1.0 - trail) * 1.4;
            }

            if (uTouchActive > 0.01) {
                col += tint * exp(-distToTouch * 7.0) * uTouchActive * 2.0;
            }
            return half4(col, 1.0);
        }

        // 10. STEALTH TITANIUM OLED (Zero-power OLED deep black & textures)
        else {
            if (varId == 0) {
                // Pure 100% OLED Pitch Black
                return half4(0.0, 0.0, 0.0, 1.0);
            } else if (varId == 1) {
                // Brushed Titanium
                float brush = hash(float2(fragCoord.x * 0.08, fragCoord.y * 0.008)) * 0.05;
                return half4(half3(0.07 + brush), 1.0);
            } else {
                // Carbon Weave
                float cx = sin(fragCoord.x * 0.35);
                float cy = sin(fragCoord.y * 0.35);
                float carbon = (cx * cy > 0.0 ? 0.03 : -0.02);
                return half4(half3(0.06 + carbon), 1.0);
            }
        }
    }
"""

@Composable
fun BackgroundShaderCanvas(
    theme: BackgroundTheme,
    variantIndex: Int = 0,
    touchPoints: List<TouchPoint> = emptyList(),
    customImageUri: String? = null,
    dimRatio: Float = 0f,
    modifier: Modifier = Modifier
) {
    if (theme == BackgroundTheme.STEALTH_OLED && variantIndex == 0 || dimRatio >= 0.99f) {
        Box(modifier = modifier.fillMaxSize().background(Black))
        return
    }

    if (theme == BackgroundTheme.CUSTOM_IMAGE && customImageUri != null) {
        CustomImageBackground(uriString = customImageUri, dimRatio = dimRatio, modifier = modifier)
        return
    }

    val transition = rememberInfiniteTransition(label = "ShaderTimeTransition")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 62.8318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 80000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "TimeFloat"
    )

    val primaryTouch = touchPoints.firstOrNull()
    val touchX = primaryTouch?.x ?: -1000f
    val touchY = primaryTouch?.y ?: -1000f
    val touchActive = if (primaryTouch != null) 1.0f else 0.0f

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslProceduralBackground(
            theme = theme,
            variantIndex = variantIndex,
            time = time,
            touchX = touchX,
            touchY = touchY,
            touchActive = touchActive,
            dimRatio = dimRatio,
            modifier = modifier
        )
    } else {
        FallbackProceduralBackground(
            theme = theme,
            variantIndex = variantIndex,
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
private fun AgslProceduralBackground(
    theme: BackgroundTheme,
    variantIndex: Int,
    time: Float,
    touchX: Float,
    touchY: Float,
    touchActive: Float,
    dimRatio: Float,
    modifier: Modifier
) {
    val shader = remember {
        try {
            RuntimeShader(AGSL_MINIMATE_SHADER)
        } catch (_: Throwable) {
            null
        }
    }

    if (shader != null) {
        Canvas(modifier = modifier.fillMaxSize()) {
            try {
                shader.setFloatUniform("iResolution", size.width, size.height)
                shader.setFloatUniform("iTime", time)
                shader.setFloatUniform("uTouchPos", touchX, touchY)
                shader.setFloatUniform("uTouchActive", touchActive)
                shader.setFloatUniform("uTheme", theme.ordinal.toFloat().coerceIn(0f, 9f))
                shader.setFloatUniform("uVariant", variantIndex.toFloat().coerceIn(0f, 2f))

                drawRect(
                    brush = ShaderBrush(shader),
                    alpha = (1.0f - (dimRatio * 0.95f)).coerceIn(0f, 1f)
                )
            } catch (_: Throwable) {}
        }
    } else {
        FallbackProceduralBackground(
            theme = theme,
            variantIndex = variantIndex,
            time = time,
            touchX = touchX,
            touchY = touchY,
            touchActive = touchActive,
            dimRatio = dimRatio,
            modifier = modifier
        )
    }
}

@Composable
private fun FallbackProceduralBackground(
    theme: BackgroundTheme,
    variantIndex: Int,
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
            Offset(w * 0.5f + cos(time * 0.5f) * 40f, h * 0.5f + sin(time * 0.5f) * 40f)
        }

        val colors = when (theme) {
            BackgroundTheme.CHROME_FLUID -> listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF0F172A))
            BackgroundTheme.DEEP_ABYSS -> listOf(Color(0xFF00F5D4), Color(0xFF0077B6), Color(0xFF03071E))
            BackgroundTheme.HYPERDRIVE_WARP -> listOf(Color(0xFF00B4D8), Color(0xFF7209B7), Color(0xFF03045E))
            BackgroundTheme.AURORA_BOREALIS -> listOf(Color(0xFF00FF87), Color(0xFF60EFFF), Color(0xFF0B0A26))
            BackgroundTheme.MAGMA_CORE -> listOf(Color(0xFFFF4800), Color(0xFFFFB703), Color(0xFF220901))
            BackgroundTheme.HOLO_PRISM -> listOf(Color(0xFFFF007F), Color(0xFF00F5D4), Color(0xFF101020))
            BackgroundTheme.SYNTHWAVE_3D -> listOf(Color(0xFFFF007F), Color(0xFF00FFFF), Color(0xFF080415))
            BackgroundTheme.SAKURA_BREEZE -> listOf(Color(0xFFFFB7B2), Color(0xFFFF69B4), Color(0xFF1F0C16))
            BackgroundTheme.MATRIX_CASCADE -> listOf(Color(0xFF00FF66), Color(0xFF008833), Color(0xFF021105))
            else -> listOf(Color(0xFF212529), Color(0xFF121416), Color(0xFF000000))
        }

        drawRect(
            brush = Brush.radialGradient(
                colors = colors,
                center = centerOffset,
                radius = w * 0.95f
            ),
            alpha = alpha * 0.6f
        )
    }
}

/**
 * 60fps Native Animated GIF and Image Renderer.
 */
@Composable
private fun CustomImageBackground(
    uriString: String,
    dimRatio: Float,
    modifier: Modifier
) {
    val context = LocalContext.current
    var loadedDrawable by remember(uriString) { mutableStateOf<Drawable?>(null) }

    DisposableEffect(uriString) {
        try {
            val uri = Uri.parse(uriString)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                val drawable = ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                    decoder.isMutableRequired = false
                }
                if (drawable is AnimatedImageDrawable) {
                    drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                    drawable.start()
                } else if (drawable is Animatable) {
                    drawable.start()
                }
                loadedDrawable = drawable
            }
        } catch (_: Exception) {}

        onDispose {
            (loadedDrawable as? AnimatedImageDrawable)?.stop()
            (loadedDrawable as? Animatable)?.stop()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Black)) {
        if (loadedDrawable != null) {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setImageDrawable(loadedDrawable)
                        if (loadedDrawable is AnimatedImageDrawable) {
                            (loadedDrawable as AnimatedImageDrawable).start()
                        } else if (loadedDrawable is Animatable) {
                            (loadedDrawable as Animatable).start()
                        }
                    }
                },
                update = { iv ->
                    iv.setImageDrawable(loadedDrawable)
                    if (loadedDrawable is AnimatedImageDrawable) {
                        (loadedDrawable as AnimatedImageDrawable).start()
                    } else if (loadedDrawable is Animatable) {
                        (loadedDrawable as Animatable).start()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Ambient dimming overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = (dimRatio * 0.85f + 0.15f).coerceIn(0f, 0.95f))
                        )
                    )
                )
        )
    }
}
