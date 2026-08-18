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

private const val AGSL_MINIMATE_SHADER = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float2 uTouchPos;
    uniform float uTouchActive;
    uniform int uTheme;
    uniform int uVariant;

    // Fast procedural pseudo-random hash
    float hash(float2 p) {
        return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453123);
    }

    // 2D Noise
    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float2 u = f * f * (3.0 - 2.0 * f);
        return mix(mix(hash(i + float2(0.0, 0.0)), hash(i + float2(1.0, 0.0)), u.x),
                   mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x), u.y);
    }

    // Voronoi distance field
    float voronoi(float2 p) {
        float2 n = floor(p);
        float2 f = fract(p);
        float md = 8.0;
        for (int j = -1; j <= 1; j++) {
            for (int i = -1; i <= 1; i++) {
                float2 g = float2(float(i), float(j));
                float2 o = float2(hash(n + g), hash(n + g + 13.7));
                o = 0.5 + 0.5 * sin(iTime * 1.5 + 6.2831 * o);
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
        float time = iTime * 0.6;

        half3 colA = half3(1.0, 0.5, 0.7);
        half3 colB = half3(0.4, 0.8, 1.0);
        half3 bg   = half3(0.04, 0.04, 0.07);

        // 1. SAKURA PETALS (Procedural 5-lobed cherry blossoms drifting in wind)
        if (uTheme == 0) {
            if (uVariant == 0) { colA = half3(1.0, 0.6, 0.75); colB = half3(1.0, 0.85, 0.9); bg = half3(0.08, 0.03, 0.06); }
            else if (uVariant == 1) { colA = half3(1.0, 0.35, 0.6); colB = half3(0.95, 0.7, 0.4); bg = half3(0.09, 0.04, 0.03); }
            else { colA = half3(0.65, 0.45, 0.95); colB = half3(0.85, 0.75, 1.0); bg = half3(0.04, 0.02, 0.08); }

            half3 col = bg;
            // Drifting petal field
            float2 pUv = uv * 3.5;
            pUv.y += time * 0.8;
            pUv.x += sin(pUv.y * 1.5 + time) * 0.4;
            
            float2 cell = fract(pUv) - 0.5;
            float2 cellId = floor(pUv);
            float angle = atan2(cell.y, cell.x) + time * (hash(cellId) - 0.5) * 2.0;
            float r = length(cell);
            // 5 petal flower polar shape: r = cos(5 * theta)
            float flower = 0.18 + 0.06 * cos(5.0 * angle);
            float petalMask = smoothstep(flower + 0.02, flower - 0.01, r);
            col += mix(colA, colB, hash(cellId)) * petalMask * 0.85;

            // Touch interaction ripple
            if (uTouchActive > 0.01) {
                float ripple = sin(distToTouch * 30.0 - time * 10.0) * exp(-distToTouch * 6.0);
                col += colA * max(0.0, ripple) * 1.5 * uTouchActive;
            }
            return half4(col, 1.0);
        }

        // 2. BUBBLE AQUARIUM (Spherical bubbles with Fresnel rim lighting & burst)
        else if (uTheme == 1) {
            if (uVariant == 0) { colA = half3(0.3, 0.8, 1.0); colB = half3(1.0, 0.5, 0.85); bg = half3(0.02, 0.05, 0.09); }
            else if (uVariant == 1) { colA = half3(0.2, 0.95, 0.7); colB = half3(0.9, 0.95, 0.3); bg = half3(0.02, 0.07, 0.05); }
            else { colA = half3(0.8, 0.4, 1.0); colB = half3(0.3, 0.7, 1.0); bg = half3(0.05, 0.02, 0.09); }

            half3 col = bg;
            float2 bUv = uv * 3.0;
            bUv.y -= time * 0.7; // Float upwards
            bUv.x += sin(bUv.y * 2.0 + time * 1.2) * 0.2;
            
            float2 cell = fract(bUv) - 0.5;
            float d = length(cell);
            float bubbleRadius = 0.22;
            // Fresnel rim & specular shine
            float rim = smoothstep(bubbleRadius, bubbleRadius - 0.03, d) * smoothstep(bubbleRadius - 0.08, bubbleRadius - 0.01, d);
            float spec = smoothstep(0.06, 0.01, length(cell - float2(-0.07, 0.07)));
            
            col += colA * rim * 1.2;
            col += colB * spec * 1.4;

            if (uTouchActive > 0.01) {
                col += colA * exp(-distToTouch * 5.0) * uTouchActive * 1.2;
            }
            return half4(col, 1.0);
        }

        // 3. CAT PAW CAFE (Soft paw prints & drifting heart particles)
        else if (uTheme == 2) {
            if (uVariant == 0) { colA = half3(1.0, 0.55, 0.65); colB = half3(1.0, 0.85, 0.7); bg = half3(0.07, 0.03, 0.04); }
            else if (uVariant == 1) { colA = half3(0.95, 0.65, 0.35); colB = half3(1.0, 0.9, 0.75); bg = half3(0.07, 0.05, 0.02); }
            else { colA = half3(0.55, 0.65, 0.95); colB = half3(0.85, 0.8, 1.0); bg = half3(0.03, 0.03, 0.07); }

            half3 col = bg;
            float2 gridUv = uv * 4.0;
            float2 cell = fract(gridUv) - 0.5;
            float2 id = floor(gridUv);

            // Paw main pad & 3 toe beans
            float pad = smoothstep(0.14, 0.11, length(cell + float2(0.0, 0.04)));
            float toe1 = smoothstep(0.06, 0.04, length(cell - float2(-0.10, 0.12)));
            float toe2 = smoothstep(0.07, 0.05, length(cell - float2(0.0, 0.16)));
            float toe3 = smoothstep(0.06, 0.04, length(cell - float2(0.10, 0.12)));
            float paw = max(pad, max(toe1, max(toe2, toe3)));

            float pulse = sin(time * 2.0 + hash(id) * 6.28) * 0.5 + 0.5;
            col += mix(colA, colB, pulse) * paw * 0.75;

            if (uTouchActive > 0.01) {
                col += colA * exp(-distToTouch * 6.0) * uTouchActive * 1.5;
            }
            return half4(col, 1.0);
        }

        // 4. PRISM WAVES (Chromatic light diffraction & fluid ribbon waves)
        else if (uTheme == 3) {
            float wave1 = sin(uv.x * 4.0 + uv.y * 3.0 + time);
            float wave2 = cos(uv.x * 3.0 - uv.y * 5.0 + time * 1.2);
            float w = sin(wave1 + wave2);

            half3 col;
            col.r = sin(w * 3.14 + 0.0 + time) * 0.5 + 0.5;
            col.g = sin(w * 3.14 + 2.09 + time) * 0.5 + 0.5;
            col.b = sin(w * 3.14 + 4.18 + time) * 0.5 + 0.5;

            col *= 0.55;
            if (uTouchActive > 0.01) {
                col += half3(1.0, 1.0, 1.0) * exp(-distToTouch * 5.0) * uTouchActive * 0.9;
            }
            return half4(col, 1.0);
        }

        // 5. MATCHA LATTE ART (Fluid froth swirls with touch vortex)
        else if (uTheme == 4) {
            if (uVariant == 0) { colA = half3(0.4, 0.75, 0.4); colB = half3(0.96, 0.93, 0.82); bg = half3(0.03, 0.06, 0.03); }
            else if (uVariant == 1) { colA = half3(0.85, 0.6, 0.35); colB = half3(0.98, 0.92, 0.8); bg = half3(0.06, 0.04, 0.02); }
            else { colA = half3(0.65, 0.5, 0.85); colB = half3(0.95, 0.9, 0.98); bg = half3(0.05, 0.03, 0.07); }

            float2 fUv = uv * 3.0;
            float n1 = noise(fUv + time * 0.2);
            float n2 = noise(fUv * 2.0 + n1 * 2.0 + time * 0.15);
            float swirl = smoothstep(0.35, 0.75, n2);

            half3 col = mix(bg, colA, swirl * 0.8);
            col = mix(col, colB, smoothstep(0.6, 0.9, n2) * 0.9);

            if (uTouchActive > 0.01) {
                col += colB * exp(-distToTouch * 6.0) * uTouchActive;
            }
            return half4(col, 1.0);
        }

        // 6. RETRO 8-BIT ARCADE (Pixel starfield & scanlines)
        else if (uTheme == 5) {
            float2 pUv = floor(uv * 32.0) / 32.0;
            float star = hash(pUv + floor(time * 4.0) * 0.001);
            float starGlow = smoothstep(0.96, 1.0, star);

            float scanline = sin(fragCoord.y * 1.5) * 0.15 + 0.85;
            half3 col = half3(0.02, 0.02, 0.06);
            col += half3(0.0, 0.9, 0.8) * starGlow * 1.6;
            col += half3(0.9, 0.1, 0.6) * (sin(pUv.y * 6.0 + time * 2.0) * 0.5 + 0.5) * 0.35;
            col *= scanline;

            if (uTouchActive > 0.01) {
                col += half3(0.0, 1.0, 0.8) * exp(-distToTouch * 7.0) * uTouchActive * 1.4;
            }
            return half4(col, 1.0);
        }

        // 7. BIOLUMINESCENT SEA (Caustic sun rays & aquatic shockwaves)
        else if (uTheme == 6) {
            float v1 = voronoi(uv * 6.0 + time * 0.8);
            float v2 = voronoi(uv * 10.0 - time * 0.5);
            float caustic = pow(1.0 - (v1 * 0.6 + v2 * 0.4), 2.5);

            half3 col = half3(0.01, 0.05, 0.09);
            col += half3(0.0, 0.8, 0.95) * caustic * 0.85;

            if (uTouchActive > 0.01) {
                float ripple = sin(distToTouch * 35.0 - time * 12.0) * exp(-distToTouch * 5.0);
                col += half3(0.3, 0.95, 0.95) * max(0.0, ripple) * 1.8 * uTouchActive;
            }
            return half4(col, 1.0);
        }

        // 8. JELLY MOCHI SQUISH (Elastic jiggling jelly with glossy specular highlights)
        else if (uTheme == 7) {
            float2 jUv = uv * 5.0;
            float jelly = sin(length(jUv) * 4.0 - time * 3.0) * exp(-length(jUv) * 0.8);
            float spec = pow(max(0.0, sin(uv.x * 8.0 + uv.y * 8.0 + time)), 16.0);

            half3 col = half3(0.07, 0.02, 0.04);
            col += half3(1.0, 0.3, 0.5) * (jelly * 0.5 + 0.5) * 0.7;
            col += half3(1.0, 1.0, 1.0) * spec * 0.6;

            if (uTouchActive > 0.01) {
                col += half3(1.0, 0.5, 0.7) * exp(-distToTouch * 5.0) * uTouchActive * 1.3;
            }
            return half4(col, 1.0);
        }

        // 9. COSMIC GALAXY (Spiral nebula vortex & twinkling star clusters)
        else if (uTheme == 8) {
            float r = length(uv);
            float a = atan2(uv.y, uv.x) + r * 3.5 - time * 0.5;
            float spiral = sin(a * 2.0) * 0.5 + 0.5;
            float core = exp(-r * 3.0);

            half3 col = half3(0.02, 0.02, 0.06);
            col += half3(0.4, 0.2, 0.9) * spiral * exp(-r * 1.5) * 0.9;
            col += half3(0.2, 0.8, 1.0) * core * 1.5;

            // Twinkling stars
            float stars = hash(floor(uv * 40.0));
            col += half3(1.0, 1.0, 1.0) * smoothstep(0.985, 1.0, stars) * (sin(time * 4.0 + stars * 100.0) * 0.5 + 0.5);

            if (uTouchActive > 0.01) {
                col += half3(0.5, 0.8, 1.0) * exp(-distToTouch * 5.0) * uTouchActive * 1.5;
            }
            return half4(col, 1.0);
        }

        // 10. STEALTH TITANIUM (Luxury brushed titanium & OLED black)
        else {
            if (uVariant == 0) {
                return half4(0.0, 0.0, 0.0, 1.0); // Pure OLED
            } else if (uVariant == 1) {
                float brush = hash(float2(fragCoord.x * 0.1, fragCoord.y * 0.01)) * 0.04;
                return half4(half3(0.06 + brush), 1.0);
            } else {
                float carbon = sin(fragCoord.x * 0.3) * sin(fragCoord.y * 0.3) * 0.03;
                return half4(half3(0.07 + carbon), 1.0);
            }
        }
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
            variant = variant,
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
private fun AgslProceduralBackground(
    theme: BackgroundTheme,
    variant: ThemeVariant,
    time: Float,
    touchX: Float,
    touchY: Float,
    touchActive: Float,
    dimRatio: Float,
    modifier: Modifier
) {
    val shader = remember { RuntimeShader(AGSL_MINIMATE_SHADER) }

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
private fun FallbackProceduralBackground(
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
            Offset(w * 0.5f + cos(time * 0.5f) * 40f, h * 0.5f + sin(time * 0.5f) * 40f)
        }

        val colors = when (theme) {
            BackgroundTheme.SAKURA_PETALS -> listOf(Color(0xFFFFB7B2), Color(0xFFFF69B4), Color(0xFF1F0C16))
            BackgroundTheme.BUBBLE_POP -> listOf(Color(0xFF89CFF0), Color(0xFFFF6EC7), Color(0xFF0A0F1F))
            BackgroundTheme.KAWAII_PAWS -> listOf(Color(0xFFF4A261), Color(0xFFFF758F), Color(0xFF1E1018))
            BackgroundTheme.PRISM_WAVES -> listOf(Color(0xFFFF9A8B), Color(0xFF70A6FF), Color(0xFF120A1C))
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
                radius = w * 0.9f
            ),
            alpha = alpha * 0.55f
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
                        colors = listOf(Color.Transparent, Color(0x66000000), Color(0xCC000000))
                    )
                )
        )
    }
}
