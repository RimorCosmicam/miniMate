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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.minimate.touchpad.engine.TouchPoint
import com.minimate.touchpad.model.BackgroundTheme
import com.minimate.touchpad.model.AbstractShaderTheme
import com.minimate.touchpad.model.ShaderRecolor
import com.minimate.touchpad.model.ThemeFilter
import com.minimate.touchpad.model.ThemeCollection
import com.minimate.touchpad.model.collection
import com.minimate.touchpad.model.isScenery
import com.minimate.ui.theme.Black
import kotlin.math.cos
import kotlin.math.sin

private const val AGSL_MINIMATE_SHADER = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float2 uTouch0;
    uniform float2 uTouch1;
    uniform float uTouchActive0;
    uniform float uTouchActive1;
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

    // Fractional Brownian Motion (Multi-octave volumetric smoke/fluid)
    float fbm(float2 p) {
        float v = 0.0;
        float a = 0.5;
        float2 shift = float2(100.0, 100.0);
        for (int i = 0; i < 4; ++i) {
            v += a * noise(p);
            p = p * 2.0 + shift;
            a *= 0.5;
        }
        return v;
    }

    // Domain Warped Fluid Smoke
    float fluidSmoke(float2 p, float time) {
        float2 q = float2(fbm(p + float2(0.0, 0.0)), fbm(p + float2(5.2, 1.3)));
        float2 r = float2(fbm(p + 4.0 * q + float2(1.7, -9.2) + float2(time * 0.15, time * 0.1)),
                          fbm(p + 4.0 * q + float2(8.3, 2.8) - float2(time * 0.12, time * 0.18)));
        return fbm(p + 4.0 * r);
    }

    half4 main(in float2 fragCoord) {
        float2 uv = (fragCoord - 0.5 * iResolution.xy) / iResolution.y;
        float time = iTime * 0.7;

        // Dynamic Multi-Touch Fluid Interaction (Warping & Displacing coordinates)
        float2 t0 = (uTouch0 - 0.5 * iResolution.xy) / iResolution.y;
        float2 t1 = (uTouch1 - 0.5 * iResolution.xy) / iResolution.y;

        float d0 = length(uv - t0);
        float d1 = length(uv - t1);

        float2 fluidWarp = float2(0.0, 0.0);
        float touchShockwave = 0.0;

        if (uTouchActive0 > 0.01) {
            float2 dir = uv - t0;
            float len = length(dir) + 0.001;
            float push = sin(len * 28.0 - time * 12.0) * exp(-len * 4.5);
            fluidWarp += (dir / len) * push * 0.08 * uTouchActive0;
            touchShockwave += max(0.0, push) * exp(-len * 3.5) * uTouchActive0;
        }

        if (uTouchActive1 > 0.01) {
            float2 dir = uv - t1;
            float len = length(dir) + 0.001;
            float push = sin(len * 28.0 - time * 12.0) * exp(-len * 4.5);
            fluidWarp += (dir / len) * push * 0.08 * uTouchActive1;
            touchShockwave += max(0.0, push) * exp(-len * 3.5) * uTouchActive1;
        }

        float2 p = uv + fluidWarp;

        int themeId = int(uTheme + 0.5);
        int varId = int(uVariant + 0.5);

        // 1. LIQUID CYBER SMOKE & MERCURY (Volumetric fluid smoke & metallic chrome)
        if (themeId == 0) {
            float smoke = fluidSmoke(p * 2.8, time);
            float smoke2 = fluidSmoke(p * 4.2 + float2(smoke * 1.5, -time * 0.2), time * 0.8);
            
            float2 norm = float2(
                fluidSmoke(p * 2.8 + float2(0.03, 0.0), time) - fluidSmoke(p * 2.8 - float2(0.03, 0.0), time),
                fluidSmoke(p * 2.8 + float2(0.0, 0.03), time) - fluidSmoke(p * 2.8 - float2(0.0, 0.03), time)
            ) * 5.0;

            float spec = pow(max(0.0, dot(normalize(float3(norm, 1.0)), normalize(float3(0.6, 0.8, 1.0)))), 18.0);
            float rim = 1.0 - max(0.0, dot(normalize(float3(norm, 1.0)), float3(0.0, 0.0, 1.0)));

            half3 baseCol, smokeTint, specCol;
            if (varId == 0) {
                // Mercury Silver
                baseCol = half3(0.04, 0.05, 0.08);
                smokeTint = half3(0.25, 0.30, 0.38);
                specCol = half3(0.95, 0.98, 1.0);
            } else if (varId == 1) {
                // Cyber Neon Pink
                baseCol = half3(0.08, 0.01, 0.06);
                smokeTint = half3(0.55, 0.05, 0.38);
                specCol = half3(1.0, 0.25, 0.75);
            } else {
                // Golden Amber
                baseCol = half3(0.08, 0.04, 0.01);
                smokeTint = half3(0.55, 0.35, 0.08);
                specCol = half3(1.0, 0.85, 0.35);
            }

            half3 col = baseCol + smokeTint * smoke * 1.6 + smokeTint * smoke2 * 0.8;
            col += specCol * spec * 2.2 + half3(0.2, 0.8, 1.0) * rim * 0.5;
            col += specCol * touchShockwave * 2.0;
            return half4(col, 1.0);
        }

        // 2. COSMIC NEBULA VAPOR (Interstellar vapor clouds swirling with stellar gas)
        else if (themeId == 1) {
            float n1 = fluidSmoke(p * 2.5 + float2(time * 0.1, -time * 0.08), time);
            float n2 = fbm(p * 6.0 - float2(time * 0.15, time * 0.1));
            float stars = hash(floor(p * 60.0));
            float starGlow = smoothstep(0.985, 1.0, stars) * (sin(time * 3.0 + stars * 10.0) * 0.3 + 0.7);

            half3 nebA, nebB;
            if (varId == 0) {
                nebA = half3(0.6, 0.1, 0.9); // Violet Void
                nebB = half3(0.1, 0.4, 0.9);
            } else if (varId == 1) {
                nebA = half3(0.0, 0.9, 0.85); // Cosmic Cyan
                nebB = half3(0.1, 0.2, 0.8);
            } else {
                nebA = half3(1.0, 0.4, 0.1); // Solar Flare
                nebB = half3(0.9, 0.1, 0.4);
            }

            half3 bg = half3(0.02, 0.01, 0.04);
            half3 col = bg + nebA * n1 * 1.5 + nebB * n2 * 0.8;
            col += half3(1.0, 1.0, 1.0) * starGlow * 1.6;
            col += nebA * touchShockwave * 2.4;
            return half4(col, 1.0);
        }

        // 3. ABYSSAL BIOLUMINESCENT FLUID (Deep sea pressure waves and glowing vortex eddies)
        else if (themeId == 2) {
            float smoke = fluidSmoke(p * 3.0, time * 0.9);
            float pulse = sin(time * 1.8 + smoke * 4.0) * 0.5 + 0.5;
            float glow = exp(-length(p) * 1.2) * (0.6 + 0.4 * pulse);

            half3 lumA, lumB;
            if (varId == 0) {
                lumA = half3(0.0, 0.95, 0.85); // Electric Cyan
                lumB = half3(0.05, 0.15, 0.6);
            } else if (varId == 1) {
                lumA = half3(0.7, 0.1, 1.0); // Abyss Violet
                lumB = half3(0.15, 0.02, 0.4);
            } else {
                lumA = half3(1.0, 0.35, 0.5); // Coral
                lumB = half3(0.8, 0.7, 0.1);
            }

            half3 bg = half3(0.01, 0.02, 0.05);
            half3 col = bg + mix(lumB, lumA, smoke) * smoke * 1.5 + lumA * glow * 0.9;
            col += lumA * touchShockwave * 2.5;
            return half4(col, 1.0);
        }

        // 4. MOLTEN VOLCANIC SMOLDER (Subterranean volcanic smoke with convective magma heat)
        else if (themeId == 3) {
            float smoke = fluidSmoke(p * 2.6, time * 0.85);
            float heat = fbm(p * 5.0 - time * 0.3);
            float core = smoothstep(0.35, 0.75, smoke + heat * 0.4);

            half3 magmaCol;
            if (varId == 0) magmaCol = half3(1.0, 0.35, 0.05); // Magma Ember
            else if (varId == 1) magmaCol = half3(0.85, 0.1, 0.9); // Plasma Ash
            else magmaCol = half3(1.0, 0.8, 0.2); // Obsidian Fire

            half3 crust = half3(0.04, 0.02, 0.01);
            half3 col = crust + half3(0.18, 0.08, 0.04) * smoke * 1.2 + magmaCol * core * 2.2;
            col += magmaCol * touchShockwave * 2.6;
            return half4(col, 1.0);
        }

        // 5. POLAR AURORA CURRENTS (Ionized atmospheric fluid curtains waving across space)
        else if (themeId == 4) {
            float2 pAur = p * 2.2;
            float wave1 = sin(pAur.x * 2.0 + time * 0.7 + sin(pAur.y * 2.0));
            float wave2 = sin(pAur.x * 3.5 - time * 0.5 + cos(pAur.y * 1.5));
            float curtain = exp(-abs(pAur.y - (wave1 + wave2) * 0.28) * 3.8);
            float smoke = fluidSmoke(pAur * 1.8, time * 0.6);

            half3 aurA, aurB;
            if (varId == 0) {
                aurA = half3(0.0, 1.0, 0.55); // Emerald Polar
                aurB = half3(0.35, 0.1, 0.9);
            } else if (varId == 1) {
                aurA = half3(0.85, 0.1, 0.9); // Cosmic Magenta
                aurB = half3(0.1, 0.6, 1.0);
            } else {
                aurA = half3(0.0, 0.9, 1.0); // Arctic Teal
                aurB = half3(0.1, 1.0, 0.7);
            }

            half3 bg = half3(0.01, 0.02, 0.04);
            half3 col = bg + mix(aurB, aurA, curtain) * curtain * 1.8 + aurA * smoke * 0.4;
            col += aurA * touchShockwave * 2.2;
            return half4(col, 1.0);
        }

        // 6. PRISMATIC OIL SHEEN (Iridescent thin-film interference fluid on dark liquid)
        else if (themeId == 5) {
            float smoke = fluidSmoke(p * 3.0, time * 0.75);
            float prism = smoke * 6.2831 + time * 0.5;

            half3 col;
            if (varId == 0) {
                // Prism Dispersion
                col.r = sin(prism + 0.0) * 0.5 + 0.5;
                col.g = sin(prism + 2.09) * 0.5 + 0.5;
                col.b = sin(prism + 4.18) * 0.5 + 0.5;
                col *= (smoke * 1.2 + 0.15);
            } else if (varId == 1) {
                // Opal Pearl
                col.r = sin(prism * 0.6) * 0.2 + 0.8;
                col.g = sin(prism * 0.6 + 2.0) * 0.2 + 0.8;
                col.b = sin(prism * 0.6 + 4.0) * 0.2 + 0.9;
                col *= smoke * 1.1;
            } else {
                // Dark Obsidian
                col.r = sin(prism) * 0.35 + 0.15;
                col.g = sin(prism + 2.0) * 0.35 + 0.25;
                col.b = sin(prism + 4.0) * 0.45 + 0.35;
            }

            col += half3(1.0, 1.0, 1.0) * touchShockwave * 1.8;
            return half4(col, 1.0);
        }

        // 7. DARK MATTER SPACETIME WARP (Gravitational lensing fluid horizon)
        else if (themeId == 6) {
            float r = length(p);
            float angle = atan(p.y, p.x);
            float warp = fluidSmoke(p * 3.0 + float2(sin(r * 8.0 - time), cos(angle * 3.0)), time);
            float horizon = exp(-r * 3.0) * (0.8 + 0.2 * sin(time * 2.0));

            half3 warpCol;
            if (varId == 0) warpCol = half3(0.2, 0.6, 1.0); // Singularity Blue
            else if (varId == 1) warpCol = half3(1.0, 0.2, 0.3); // Event Horizon Red
            else warpCol = half3(1.0, 0.85, 0.3); // Photon Gold

            half3 bg = half3(0.01, 0.01, 0.03);
            half3 col = bg + warpCol * warp * 1.4 + warpCol * horizon * 1.8;
            col += warpCol * touchShockwave * 2.4;
            return half4(col, 1.0);
        }

        // 8. ELECTRIC CYAN VAPOR (High-energy ionized plasma vapor and turbulent eddies)
        else if (themeId == 7) {
            float smoke = fluidSmoke(p * 3.2, time * 1.1);
            float bolt = fbm(p * 8.0 + float2(time * 0.4, -time * 0.3));
            float arc = smoothstep(0.75, 0.95, bolt);

            half3 vaporCol;
            if (varId == 0) vaporCol = half3(0.0, 0.95, 0.95); // Electric Cyan
            else if (varId == 1) vaporCol = half3(0.75, 0.15, 1.0); // Neon Violet
            else vaporCol = half3(0.1, 1.0, 0.65); // Supercharged Mint

            half3 bg = half3(0.02, 0.03, 0.06);
            half3 col = bg + vaporCol * smoke * 1.6 + half3(1.0, 1.0, 1.0) * arc * 1.8;
            col += vaporCol * touchShockwave * 2.5;
            return half4(col, 1.0);
        }

        // 9. STEALTH TITANIUM OLED (Zero-power OLED deep black & luxury textures)
        else if (themeId == 8) {
            if (varId == 0) {
                return half4(0.0, 0.0, 0.0, 1.0);
            } else if (varId == 1) {
                float brush = hash(float2(fragCoord.x * 0.08, fragCoord.y * 0.008)) * 0.05;
                return half4(half3(0.07 + brush), 1.0);
            } else {
                float cx = sin(fragCoord.x * 0.35);
                float cy = sin(fragCoord.y * 0.35);
                float carbon = (cx * cy > 0.0 ? 0.03 : -0.02);
                return half4(half3(0.06 + carbon), 1.0);
            }
        }

        // 10. PIXEL ARCADE (theme 9 is reserved for custom media)
        else if (themeId == 10) {
            float2 px = floor((p + 0.5) * float2(54.0, 34.0));
            float checker = mod(px.x + px.y + floor(time * 3.0), 2.0);
            float stars = step(0.92, hash(px + floor(time * 2.0)));
            float scan = sin(fragCoord.y * 3.14159) * 0.06;
            half3 a = varId == 0 ? half3(0.2, 0.95, 1.0) : (varId == 1 ? half3(1.0, 0.35, 0.65) : half3(0.3, 1.0, 0.35));
            half3 b = varId == 0 ? half3(0.55, 0.2, 1.0) : (varId == 1 ? half3(1.0, 0.75, 0.15) : half3(0.05, 0.5, 1.0));
            half3 col = half3(0.015, 0.02, 0.05) + mix(a, b, checker) * stars * 1.4;
            float grid = step(0.93, fract((p.x + time * 0.03) * 12.0)) + step(0.93, fract(p.y * 12.0));
            col += mix(a, b, p.y + 0.5) * grid * 0.25 + scan + a * touchShockwave * 2.0;
            return half4(col, 1.0);
        }

        // 11. NEON RACER
        else if (themeId == 11) {
            float horizon = -0.08;
            float depth = 1.0 / max(0.035, abs(p.y - horizon));
            float lanes = smoothstep(0.91, 1.0, sin((p.x * depth + time * 0.8) * 0.72) * 0.5 + 0.5);
            float road = step(p.y, horizon) * (lanes + smoothstep(0.96, 1.0, sin(depth * 0.7 - time * 5.0) * 0.5 + 0.5));
            float skyline = step(p.y, horizon + hash(floor((p.x + 0.5) * 28.0)) * 0.22) * step(horizon, p.y);
            half3 neon = varId == 0 ? half3(0.05, 0.8, 1.0) : (varId == 1 ? half3(1.0, 0.15, 0.55) : half3(0.55, 1.0, 0.05));
            half3 sun = varId == 1 ? half3(1.0, 0.55, 0.12) : half3(0.75, 0.15, 1.0);
            float disc = smoothstep(0.24, 0.22, length(p - float2(0.0, 0.08)));
            half3 col = half3(0.01, 0.01, 0.045) + neon * road * 1.5 + neon * skyline * 0.22 + sun * disc * 1.1;
            col += neon * touchShockwave * 2.2;
            return half4(col, 1.0);
        }

        // 12. LOW-POLY REALMS
        else if (themeId == 12) {
            float2 cell = floor((p + float2(time * 0.025, 0.0)) * 9.0);
            float terrain = noise(float2(cell.x * 0.35, floor((p.y + 0.5) * 12.0))) + p.y * 0.9;
            float facets = floor(terrain * 7.0) / 7.0;
            float edge = smoothstep(0.46, 0.5, abs(fract(terrain * 7.0) - 0.5));
            half3 low = varId == 0 ? half3(0.1, 0.4, 0.75) : (varId == 1 ? half3(0.8, 0.25, 0.08) : half3(0.1, 0.65, 0.35));
            half3 high = varId == 0 ? half3(0.65, 0.9, 1.0) : (varId == 1 ? half3(1.0, 0.75, 0.28) : half3(0.7, 1.0, 0.45));
            half3 col = mix(low * 0.12, high, facets) + high * edge * 0.16;
            col *= 0.55 + 0.45 * smoothstep(-0.5, 0.5, p.y);
            col += high * touchShockwave * 1.8;
            return half4(col, 1.0);
        }

        // 13. MIDNIGHT JAZZ CLUB
        else if (themeId == 13) {
            float smoke = fluidSmoke(p * 2.1, time * 0.35);
            float keys = step(0.78, fract((p.x + 0.5) * 11.0)) * smoothstep(-0.15, -0.48, p.y);
            float groove = sin(length(p - float2(0.2, 0.12)) * 55.0 - time * 2.2) * 0.5 + 0.5;
            groove *= smoothstep(0.38, 0.05, length(p - float2(0.2, 0.12)));
            half3 brass = varId == 0 ? half3(0.15, 0.45, 1.0) : (varId == 1 ? half3(1.0, 0.63, 0.15) : half3(0.75, 0.1, 0.3));
            half3 col = half3(0.018, 0.012, 0.028) + brass * smoke * 0.45 + brass * groove * 0.75 + half3(keys) * 0.18;
            col += brass * touchShockwave * 2.0;
            return half4(col, 1.0);
        }

        // 14. LIVING WAVEFORM
        else if (themeId == 14) {
            float waveA = abs(p.y - sin(p.x * 13.0 + time * 2.5) * 0.12);
            float waveB = abs(p.y - cos(p.x * 21.0 - time * 1.7) * 0.06);
            float lineA = exp(-waveA * 75.0);
            float lineB = exp(-waveB * 95.0);
            float bars = step(0.72, fract((p.x + 0.5) * 22.0)) * smoothstep(abs(p.y), 0.0, 0.18 + 0.14 * noise(float2(floor(p.x * 22.0), time)));
            half3 a = varId == 0 ? half3(0.0, 0.9, 1.0) : (varId == 1 ? half3(1.0, 0.2, 0.55) : half3(0.55, 0.15, 1.0));
            half3 b = varId == 0 ? half3(0.2, 0.35, 1.0) : (varId == 1 ? half3(1.0, 0.8, 0.15) : half3(0.1, 1.0, 0.55));
            half3 col = half3(0.01, 0.015, 0.04) + a * lineA * 1.7 + b * lineB + mix(a, b, p.y + 0.5) * bars * 0.5;
            col += a * touchShockwave * 2.4;
            return half4(col, 1.0);
        }

        // 15. DIGITAL COUTURE
        else if (themeId == 15) {
            float fold = sin(p.x * 8.0 + fluidSmoke(p * 2.4, time * 0.4) * 5.0 + time * 0.6);
            float sheen = pow(abs(fold), 9.0);
            float weave = sin((p.x + p.y) * 110.0) * 0.025;
            half3 a = varId == 0 ? half3(0.92, 0.72, 1.0) : (varId == 1 ? half3(0.12, 0.11, 0.16) : half3(1.0, 0.12, 0.55));
            half3 b = varId == 0 ? half3(0.35, 0.9, 0.95) : (varId == 1 ? half3(0.55, 0.5, 0.68) : half3(0.15, 0.75, 1.0));
            half3 col = mix(a * 0.16, b, fold * 0.5 + 0.5) + half3(sheen * 0.8 + weave);
            col += b * touchShockwave * 1.6;
            return half4(col, 1.0);
        }

        // 16. BREAK THE MATRIX
        else if (themeId == 16) {
            float2 glyph = floor(float2((p.x + 0.5) * 34.0, (p.y + 0.5 + time * 0.22) * 48.0));
            float code = step(0.76, hash(glyph));
            float trail = hash(float2(glyph.x, floor(time * 2.0))) * 0.8 + 0.2;
            float glitch = step(0.965, hash(float2(floor(p.y * 30.0), floor(time * 8.0)))) * sin(p.x * 140.0);
            half3 ink = varId == 0 ? half3(0.1, 1.0, 0.35) : (varId == 1 ? half3(1.0, 0.12, 0.18) : half3(0.88, 0.95, 1.0));
            half3 col = half3(0.002, 0.008, 0.01) + ink * code * trail + ink * abs(glitch) * 0.55;
            col += ink * touchShockwave * 2.8;
            return half4(col, 1.0);
        }

        // 17. IMPOSSIBLE PORTALS
        else if (themeId == 17) {
            float r = length(p) + 0.001;
            float a = atan(p.y, p.x);
            float tunnel = sin(16.0 / r - time * 3.0 + a * 5.0) * 0.5 + 0.5;
            float ribs = pow(tunnel, 5.0) * smoothstep(0.72, 0.05, r);
            float rooms = step(0.78, fract((a / 6.2831 + log(r) * 0.55 - time * 0.04) * 12.0));
            half3 portal = varId == 0 ? half3(0.25, 0.72, 1.0) : (varId == 1 ? half3(1.0, 0.72, 0.18) : half3(0.72, 0.22, 1.0));
            half3 col = half3(0.008, 0.006, 0.025) + portal * ribs * 1.4 + portal * rooms * smoothstep(0.7, 0.05, r) * 0.45;
            col += portal * touchShockwave * 2.2;
            return half4(col, 1.0);
        }

        // 18. GALACTIC NAVIGATOR
        else if (themeId == 18) {
            float2 sky = p + float2(time * 0.008, 0.0);
            float stars = step(0.975, hash(floor(sky * 68.0)));
            float fineStars = step(0.992, hash(floor(sky * 125.0 + 31.0)));
            float longitude = smoothstep(0.985, 1.0, cos(atan(p.y, p.x) * 12.0));
            float latitude = smoothstep(0.982, 1.0, cos(length(p) * 72.0 - time * 0.3));
            float orbit = smoothstep(0.012, 0.0, abs(length(p - float2(0.12, -0.04)) - 0.29));
            half3 ink = varId == 0 ? half3(0.35, 0.65, 1.0) : (varId == 1 ? half3(1.0, 0.72, 0.2) : half3(1.0, 0.35, 0.7));
            half3 col = half3(0.006, 0.009, 0.035) + half3(stars) * 0.9 + ink * fineStars * 1.4;
            col += ink * (longitude + latitude) * smoothstep(0.48, 0.05, length(p)) * 0.14 + ink * orbit * 0.7;
            col += ink * touchShockwave * 2.1;
            return half4(col, 1.0);
        }

        // 19. EXOPLANET HORIZON
        else if (themeId == 19) {
            float2 planetCenter = float2(0.0, 0.42);
            float planetDistance = length((p - planetCenter) * float2(1.0, 0.72));
            float planet = smoothstep(0.47, 0.455, planetDistance);
            float atmosphere = smoothstep(0.51, 0.455, planetDistance) - planet;
            float bands = sin((p.y - planetCenter.y) * 95.0 + fluidSmoke(p * 3.0, time * 0.2) * 4.0) * 0.5 + 0.5;
            float moon = smoothstep(0.075, 0.068, length(p - float2(-0.26 + sin(time * 0.12) * 0.05, -0.18)));
            float ring = smoothstep(0.016, 0.0, abs(length((p - planetCenter) * float2(1.0, 3.8)) - 0.53));
            half3 world = varId == 0 ? half3(0.95, 0.4, 0.12) : (varId == 1 ? half3(1.0, 0.72, 0.2) : half3(0.2, 0.95, 0.55));
            half3 skyCol = varId == 2 ? half3(0.01, 0.06, 0.08) : half3(0.018, 0.008, 0.04);
            half3 col = skyCol + world * planet * (0.18 + bands * 0.65) + world * atmosphere * 1.4 + half3(0.9, 0.95, 1.0) * moon + world * ring * 0.9;
            col += world * touchShockwave * 2.0;
            return half4(col, 1.0);
        }

        // 20. DREAM KOI LAGOON
        else if (themeId == 20) {
            float water = fluidSmoke(p * 2.4, time * 0.45);
            float caustic = pow(abs(sin((p.x + water * 0.15) * 18.0) * cos((p.y - water * 0.12) * 16.0)), 8.0);
            float2 fishCell = floor((p + float2(time * 0.035, 0.0)) * float2(7.0, 9.0));
            float fish = step(0.88, hash(fishCell)) * smoothstep(0.42, 0.0, length(fract((p + float2(time * 0.035, 0.0)) * float2(7.0, 9.0)) - 0.5));
            half3 waterCol = varId == 0 ? half3(0.05, 0.35, 0.55) : (varId == 1 ? half3(0.1, 0.42, 0.27) : half3(0.48, 0.18, 0.32));
            half3 koi = varId == 0 ? half3(0.95, 0.95, 1.0) : (varId == 1 ? half3(1.0, 0.72, 0.15) : half3(1.0, 0.62, 0.75));
            half3 col = half3(0.005, 0.025, 0.04) + waterCol * water * 1.15 + koi * caustic * 0.25 + koi * fish * 0.8;
            col += koi * touchShockwave * 1.7;
            return half4(col, 1.0);
        }

        // 21. HADAL OCEAN
        else if (themeId == 21) {
            float marineSnow = step(0.982, hash(floor(float2(p.x * 90.0, (p.y + time * 0.035) * 75.0))));
            float darkness = smoothstep(0.55, -0.5, p.y);
            float jellyBody = smoothstep(0.14, 0.11, length((p - float2(0.14, -0.05)) * float2(1.0, 1.5)));
            float jellyRim = smoothstep(0.16, 0.115, length((p - float2(0.14, -0.05)) * float2(1.0, 1.5))) - jellyBody;
            float tentacles = pow(max(0.0, sin((p.x - 0.14) * 55.0 + sin(p.y * 18.0 - time) * 2.0)), 14.0) * smoothstep(-0.05, 0.38, p.y) * smoothstep(0.34, 0.05, abs(p.x - 0.14));
            half3 glow = varId == 0 ? half3(0.05, 0.75, 1.0) : (varId == 1 ? half3(0.65, 0.2, 1.0) : half3(1.0, 0.58, 0.12));
            half3 col = half3(0.002, 0.012, 0.028) * darkness + glow * marineSnow * 0.5 + glow * jellyRim * 1.4 + glow * jellyBody * 0.15 + glow * tentacles * 0.42;
            col += glow * touchShockwave * 2.3;
            return half4(col, 1.0);
        }

        // 22. INFINITE CANDY FACTORY
        else if (themeId == 22) {
            float2 q = p + float2(time * 0.06, 0.0);
            float stripes = sin((q.x + q.y * 0.45) * 42.0) * 0.5 + 0.5;
            float conveyor = step(0.82, fract((q.x + 0.5) * 12.0)) * step(0.18, fract((p.y + 0.5) * 7.0));
            float candy = step(0.86, hash(floor(q * float2(13.0, 9.0))));
            half3 a = varId == 0 ? half3(1.0, 0.32, 0.65) : (varId == 1 ? half3(1.0, 0.62, 0.08) : half3(0.6, 0.25, 1.0));
            half3 b = varId == 0 ? half3(0.3, 0.85, 1.0) : (varId == 1 ? half3(0.55, 1.0, 0.2) : half3(1.0, 0.35, 0.8));
            half3 col = mix(a, b, stripes) * 0.28 + half3(conveyor) * 0.15 + mix(a, b, hash(floor(q * 9.0))) * candy * 0.9;
            col += half3(1.0) * pow(candy, 4.0) * 0.4 + a * touchShockwave * 1.7;
            return half4(col, 1.0);
        }

        // 23. DESSERT PLANET
        else if (themeId == 23) {
            float frosting = fluidSmoke(p * 2.6 + float2(0.0, time * 0.05), time * 0.18);
            float layers = floor((p.y + frosting * 0.16) * 13.0) / 13.0;
            float sprinkles = step(0.965, hash(floor((p + float2(time * 0.012, 0.0)) * 55.0)));
            float moon = smoothstep(0.17, 0.155, length(p - float2(0.23, -0.18)));
            half3 cream = varId == 0 ? half3(1.0, 0.62, 0.75) : (varId == 1 ? half3(0.48, 0.78, 0.38) : half3(0.52, 0.48, 0.95));
            half3 icing = varId == 0 ? half3(1.0, 0.9, 0.82) : (varId == 1 ? half3(0.9, 1.0, 0.7) : half3(0.82, 0.88, 1.0));
            half3 col = mix(cream * 0.22, icing * 0.72, layers + 0.45) + icing * moon * 0.65 + half3(1.0, 0.72, 0.25) * sprinkles * 0.8;
            col += cream * touchShockwave * 1.6;
            return half4(col, 1.0);
        }

        // 24. ROCOCO DREAM GARDEN
        else if (themeId == 24) {
            float r = length(p);
            float a = atan(p.y, p.x);
            float ornament = pow(abs(cos(a * 6.0 + sin(r * 18.0 - time * 0.25))), 12.0) * smoothstep(0.48, 0.06, r);
            float porcelain = fluidSmoke(p * 2.0, time * 0.12);
            float gilt = smoothstep(0.87, 1.0, sin(r * 68.0 + a * 5.0) * 0.5 + 0.5);
            half3 rose = varId == 0 ? half3(0.95, 0.5, 0.68) : (varId == 1 ? half3(0.38, 0.72, 0.62) : half3(0.15, 0.12, 0.28));
            half3 gold = varId == 1 ? half3(0.85, 0.78, 0.48) : half3(1.0, 0.72, 0.25);
            half3 col = half3(0.08, 0.055, 0.08) + rose * porcelain * 0.8 + gold * ornament * 0.8 + gold * gilt * ornament * 0.45;
            col += rose * touchShockwave * 1.7;
            return half4(col, 1.0);
        }

        // 25. LOLITA LACE ATELIER
        else if (themeId == 25) {
            float2 tile = fract((p + 0.5) * 8.0) - 0.5;
            float laceR = length(tile);
            float lace = smoothstep(0.055, 0.025, abs(laceR - 0.28)) + smoothstep(0.04, 0.015, abs(laceR - 0.16));
            lace += pow(abs(cos(atan(tile.y, tile.x) * 4.0)), 18.0) * smoothstep(0.34, 0.08, laceR);
            float pearls = smoothstep(0.055, 0.035, length(fract((p + float2(time * 0.01, 0.0)) * 14.0) - 0.5));
            half3 fabric = varId == 0 ? half3(1.0, 0.62, 0.78) : (varId == 1 ? half3(0.18, 0.08, 0.22) : half3(0.9, 0.82, 0.68));
            half3 thread = varId == 1 ? half3(0.65, 0.5, 0.78) : half3(1.0, 0.94, 0.88);
            half3 col = fabric * (0.17 + fluidSmoke(p * 2.2, time * 0.16) * 0.35) + thread * lace * 0.65 + thread * pearls * 0.28;
            col += thread * touchShockwave * 1.5;
            return half4(col, 1.0);
        }

        return half4(0.0, 0.0, 0.0, 1.0);
    }
"""

@Composable
fun BackgroundShaderCanvas(
    theme: BackgroundTheme,
    variantIndex: Int = 0,
    touchPoints: List<TouchPoint> = emptyList(),
    customImageUri: String? = null,
    dimRatio: Float = 0f,
    animationSpeed: Float = 1f,
    themeFilters: List<ThemeFilter> = emptyList(),
    shaderTheme: AbstractShaderTheme = AbstractShaderTheme.PRISMATIC,
    shaderSubthemeIndex: Int = 1,
    shaderRecolor: ShaderRecolor = ShaderRecolor.AUTHORED,
    customShaderColors: List<Long> = emptyList(),
    modifier: Modifier = Modifier
) {
    if (theme != BackgroundTheme.CUSTOM_IMAGE) {
        Box(modifier.fillMaxSize()) {
            ThemeFilterStack(themeFilters, Modifier.fillMaxSize()) {
                AbstractShaderRenderer(
                    theme = shaderTheme,
                    subthemeIndex = shaderSubthemeIndex,
                    recolor = shaderRecolor,
                    customColors = customShaderColors,
                    touchPoints = touchPoints,
                    animationSpeed = animationSpeed,
                    filter = ThemeFilter.NONE,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (dimRatio > 0f) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dimRatio.coerceIn(0f, 1f))))
            }
        }
        return
    }
    ThemeFilterStack(themeFilters, modifier.fillMaxSize()) {
        BackgroundCanvasCore(
            theme = theme,
            variantIndex = variantIndex,
            touchPoints = touchPoints,
            customImageUri = customImageUri,
            dimRatio = dimRatio,
            animationSpeed = animationSpeed,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun BackgroundCanvasCore(
    theme: BackgroundTheme,
    variantIndex: Int,
    touchPoints: List<TouchPoint>,
    customImageUri: String?,
    dimRatio: Float,
    animationSpeed: Float,
    modifier: Modifier
) {
    if (theme == BackgroundTheme.STEALTH_OLED && variantIndex == 0 || dimRatio >= 0.99f) {
        Box(modifier = modifier.fillMaxSize().background(Black))
        return
    }

    if (theme == BackgroundTheme.CUSTOM_IMAGE && customImageUri != null) {
        CustomImageBackground(uriString = customImageUri, dimRatio = dimRatio, modifier = modifier)
        return
    }

    if (theme.isScenery) {
        SceneryThemeCanvas(
            theme = theme,
            variant = variantIndex,
            speed = animationSpeed,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    // Retired non-scenery catalog entries are never exposed or restored. Keep a
    // safe abstract render if an in-memory setting survives an app upgrade.
    if (theme.ordinal > BackgroundTheme.CUSTOM_IMAGE.ordinal) {
        AgslOrLegacyAbstract(
            theme = BackgroundTheme.CHROME_FLUID,
            variantIndex = 0,
            touchPoints = touchPoints,
            dimRatio = dimRatio,
            animationSpeed = animationSpeed,
            modifier = modifier
        )
        return
    }

    AgslOrLegacyAbstract(theme, variantIndex, touchPoints, dimRatio, animationSpeed, modifier)
}

@Composable
private fun AgslOrLegacyAbstract(
    theme: BackgroundTheme,
    variantIndex: Int,
    touchPoints: List<TouchPoint>,
    dimRatio: Float,
    animationSpeed: Float,
    modifier: Modifier
) {
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

    val touch0 = touchPoints.getOrNull(0)
    val touch1 = touchPoints.getOrNull(1)

    val t0X = touch0?.x ?: -1000f
    val t0Y = touch0?.y ?: -1000f
    val t0Active = if (touch0 != null) 1.0f else 0.0f

    val t1X = touch1?.x ?: -1000f
    val t1Y = touch1?.y ?: -1000f
    val t1Active = if (touch1 != null) 1.0f else 0.0f

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslProceduralBackground(
            theme = theme,
            variantIndex = variantIndex,
            time = time * animationSpeed,
            animationSpeed = animationSpeed,
            t0X = t0X,
            t0Y = t0Y,
            t0Active = t0Active,
            t1X = t1X,
            t1Y = t1Y,
            t1Active = t1Active,
            dimRatio = dimRatio,
            modifier = modifier
        )
    } else {
        LegacyGpuBackground(
            theme = theme,
            variantIndex = variantIndex,
            animationSpeed = animationSpeed,
            touchX = t0X,
            touchY = t0Y,
            touchActive = t0Active,
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
    animationSpeed: Float,
    t0X: Float,
    t0Y: Float,
    t0Active: Float,
    t1X: Float,
    t1Y: Float,
    t1Active: Float,
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
                shader.setFloatUniform("uTouch0", t0X, t0Y)
                shader.setFloatUniform("uTouch1", t1X, t1Y)
                shader.setFloatUniform("uTouchActive0", t0Active)
                shader.setFloatUniform("uTouchActive1", t1Active)
                shader.setFloatUniform("uTheme", theme.ordinal.toFloat().coerceIn(0f, 25f))
                shader.setFloatUniform("uVariant", variantIndex.toFloat().coerceIn(0f, 2f))

                drawRect(
                    brush = ShaderBrush(shader),
                    alpha = (1.0f - (dimRatio * 0.95f)).coerceIn(0f, 1f)
                )
            } catch (_: Throwable) {}
        }
    } else {
        LegacyGpuBackground(
            theme = theme,
            variantIndex = variantIndex,
            animationSpeed = animationSpeed,
            touchX = t0X,
            touchY = t0Y,
            touchActive = t0Active,
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
    t0X: Float,
    t0Y: Float,
    t0Active: Float,
    dimRatio: Float,
    modifier: Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val alpha = (1.0f - (dimRatio * 0.95f)).coerceIn(0f, 1f)

        val centerOffset = if (t0Active > 0.5f) {
            Offset(t0X, t0Y)
        } else {
            Offset(w * 0.5f + cos(time * 0.5f) * 40f, h * 0.5f + sin(time * 0.5f) * 40f)
        }

        val colors = when (theme) {
            BackgroundTheme.CHROME_FLUID -> listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF0F172A))
            BackgroundTheme.NEBULA_SMOKE -> listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFF090A1A))
            BackgroundTheme.ABYSSAL_FLUID -> listOf(Color(0xFF00F5D4), Color(0xFF0077B6), Color(0xFF03071E))
            BackgroundTheme.VOLCANIC_SMOKE -> listOf(Color(0xFFFF4800), Color(0xFFFFB703), Color(0xFF220901))
            BackgroundTheme.AURORA_CURRENTS -> listOf(Color(0xFF00FF87), Color(0xFF60EFFF), Color(0xFF0B0A26))
            BackgroundTheme.PRISM_OIL -> listOf(Color(0xFFFF007F), Color(0xFF00F5D4), Color(0xFF101020))
            BackgroundTheme.GRAVITY_WARP -> listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFF030712))
            BackgroundTheme.CYAN_VAPOR -> listOf(Color(0xFF06B6D4), Color(0xFFA855F7), Color(0xFF020617))
            BackgroundTheme.PIXEL_ARCADE -> listOf(Color(0xFF00E5FF), Color(0xFFFF3D9A), Color(0xFF090A20))
            BackgroundTheme.NEON_RACER -> listOf(Color(0xFFFF2E93), Color(0xFF00D9FF), Color(0xFF08031C))
            BackgroundTheme.LOW_POLY_REALMS -> listOf(Color(0xFF9DE6FF), Color(0xFF386CB0), Color(0xFF08152C))
            BackgroundTheme.JAZZ_CLUB -> listOf(Color(0xFFE0A83C), Color(0xFF243C78), Color(0xFF100914))
            BackgroundTheme.SYNTH_WAVEFORM -> listOf(Color(0xFF00F5FF), Color(0xFF7040FF), Color(0xFF05051C))
            BackgroundTheme.COUTURE_SILK -> listOf(Color(0xFFF5C4FF), Color(0xFF4BD7DD), Color(0xFF24152C))
            BackgroundTheme.MATRIX_BREAK -> listOf(Color(0xFF32FF67), Color(0xFF087A2B), Color(0xFF000A03))
            BackgroundTheme.ESCHER_PORTAL -> listOf(Color(0xFF7F5CFF), Color(0xFF3AAFFF), Color(0xFF07031B))
            BackgroundTheme.GALACTIC_NAVIGATOR -> listOf(Color(0xFF70A5FF), Color(0xFFFFD15C), Color(0xFF02051A))
            BackgroundTheme.EXOPLANET_HORIZON -> listOf(Color(0xFFFF813D), Color(0xFF7A35CC), Color(0xFF090218))
            BackgroundTheme.KOI_LAGOON -> listOf(Color(0xFF6DE4E8), Color(0xFFFFA3BC), Color(0xFF032936))
            BackgroundTheme.HADAL_OCEAN -> listOf(Color(0xFF18A8F0), Color(0xFF6628B8), Color(0xFF000815))
            BackgroundTheme.CANDY_FACTORY -> listOf(Color(0xFFFF65AC), Color(0xFF70DCFF), Color(0xFF45104C))
            BackgroundTheme.DESSERT_PLANET -> listOf(Color(0xFFFFB3C7), Color(0xFFB8F28C), Color(0xFF5B477D))
            BackgroundTheme.ROCOCO_GARDEN -> listOf(Color(0xFFF3A6BF), Color(0xFFD7A63B), Color(0xFF3C2135))
            BackgroundTheme.LOLITA_LACE -> listOf(Color(0xFFFFD5E4), Color(0xFFAA7AB8), Color(0xFF251126))
            else -> listOf(Color(0xFF212529), Color(0xFF121416), Color(0xFF000000))
        }

        val primary = colors[variantIndex.mod(colors.size)]
        val secondary = colors[(variantIndex + 1).mod(colors.size)]
        val dark = colors.last()
        drawRect(dark.copy(alpha = alpha))

        when (theme.collection) {
            ThemeCollection.ABSTRACTS -> {
                repeat(8) { index ->
                    val phase = time * (0.18f + index * 0.012f) + index * 0.78f
                    val path = Path().apply {
                        moveTo(-w * 0.1f, h * (0.12f + index * 0.11f))
                        cubicTo(
                            w * 0.25f, h * (0.1f + sin(phase) * 0.22f),
                            w * 0.72f, h * (0.9f + cos(phase * 0.8f) * 0.2f),
                            w * 1.1f, h * (0.18f + index * 0.09f)
                        )
                    }
                    drawPath(path, if (index % 2 == 0) primary.copy(alpha = alpha * 0.34f) else secondary.copy(alpha = alpha * 0.27f), style = Stroke(w * (0.035f + index * 0.004f)))
                }
                repeat(5) { index ->
                    val p = Offset(w * (0.15f + index * 0.19f), h * (0.5f + sin(time * 0.25f + index) * 0.3f))
                    drawCircle(Color.White.copy(alpha = alpha * 0.08f), w * (0.04f + index * 0.008f), p, style = Stroke(2f))
                }
            }

            ThemeCollection.ARCADE -> {
                val cell = w / 18f
                repeat(13) { row -> repeat(20) { column ->
                    val alive = ((row * 17 + column * 31 + (time * 3f).toInt()) % 11) < 2
                    if (alive) drawRect(if ((row + column) % 2 == 0) primary else secondary, Offset(column * cell, row * cell), androidx.compose.ui.geometry.Size(cell * 0.72f, cell * 0.72f), alpha = alpha * 0.8f)
                } }
                repeat(14) { line -> drawLine(Color.Black.copy(alpha = 0.22f), Offset(0f, line * h / 14f), Offset(w, line * h / 14f), 2f) }
            }

            ThemeCollection.GAME_WORLDS -> {
                val horizon = h * 0.43f
                drawCircle(secondary.copy(alpha = alpha * 0.65f), w * 0.17f, Offset(w * 0.5f, horizon))
                repeat(13) { index ->
                    val x = index / 12f
                    drawLine(primary.copy(alpha = alpha * 0.55f), Offset(w * 0.5f, horizon), Offset(w * x, h), 2f)
                }
                repeat(9) { index ->
                    val progress = ((index / 9f + time * 0.045f) % 1f)
                    val y = horizon + progress * progress * (h - horizon)
                    drawLine(primary.copy(alpha = alpha * (0.25f + progress * 0.5f)), Offset(0f, y), Offset(w, y), 1f + progress * 3f)
                }
                val mountain = Path().apply { moveTo(0f, horizon); repeat(9) { i -> lineTo(i * w / 8f, horizon - (40f + ((i * 47) % 90))) }; lineTo(w, horizon); close() }
                drawPath(mountain, primary.copy(alpha = alpha * 0.24f))
            }

            ThemeCollection.SOUND_AND_SOUL -> {
                repeat(32) { index ->
                    val x = index * w / 31f
                    val amplitude = (0.08f + 0.25f * kotlin.math.abs(sin(time * 0.65f + index * 0.7f))) * h
                    drawLine(primary.copy(alpha = alpha * 0.48f), Offset(x, h * 0.5f - amplitude), Offset(x, h * 0.5f + amplitude), w / 90f)
                }
                val wave = Path().apply {
                    moveTo(0f, h * 0.5f)
                    repeat(80) { i ->
                        val x = i * w / 79f
                        lineTo(x, h * 0.5f + sin(i * 0.38f + time * 2f) * h * 0.12f + sin(i * 0.12f - time) * h * 0.07f)
                    }
                }
                drawPath(wave, Color.White.copy(alpha = alpha * 0.85f), style = Stroke(3f))
            }

            ThemeCollection.FASHION -> {
                repeat(11) { index ->
                    val x = index * w / 10f
                    val path = Path().apply {
                        moveTo(x, -20f)
                        cubicTo(x + sin(time * 0.4f + index) * w * 0.16f, h * 0.3f, x + cos(time * 0.3f + index) * w * 0.2f, h * 0.7f, x, h + 20f)
                    }
                    drawPath(path, if (index % 2 == 0) primary.copy(alpha = alpha * 0.38f) else secondary.copy(alpha = alpha * 0.3f), style = Stroke(w * 0.055f))
                    drawPath(path, Color.White.copy(alpha = alpha * 0.12f), style = Stroke(2f))
                }
            }

            ThemeCollection.EXPERIMENTAL -> {
                if (theme == BackgroundTheme.MATRIX_BREAK) {
                    repeat(24) { column ->
                        val x = column * w / 23f
                        val offset = ((time * (35f + column % 5 * 8f) + column * 53f) % (h + 220f)) - 110f
                        repeat(8) { glyph -> drawRect(primary.copy(alpha = alpha * (0.15f + glyph * 0.08f)), Offset(x, offset - glyph * 24f), androidx.compose.ui.geometry.Size(w / 55f, 12f)) }
                    }
                } else {
                    repeat(14) { index ->
                        val scale = 1f - index / 15f
                        rotate(time * 5f + index * 7f, centerOffset) {
                            drawRect(if (index % 2 == 0) primary else secondary, centerOffset - Offset(w * 0.42f * scale, h * 0.42f * scale), androidx.compose.ui.geometry.Size(w * 0.84f * scale, h * 0.84f * scale), alpha = alpha * 0.16f, style = Stroke(2f + index * 0.35f))
                        }
                    }
                }
            }

            ThemeCollection.SPACE -> {
                repeat(90) { index ->
                    val x = ((index * 83f + time * (2f + index % 5)) % w)
                    val y = ((index * 47f + sin(index * 1.7f) * 101f) % h + h) % h
                    drawCircle(Color.White.copy(alpha = alpha * (0.25f + (index % 4) * 0.16f)), 1f + index % 3, Offset(x, y))
                }
                val planet = Offset(w * 0.62f, h * 0.48f)
                drawCircle(primary.copy(alpha = alpha * 0.75f), w * 0.17f, planet)
                drawOval(secondary.copy(alpha = alpha * 0.7f), planet - Offset(w * 0.29f, h * 0.055f), androidx.compose.ui.geometry.Size(w * 0.58f, h * 0.11f), style = Stroke(4f))
                repeat(3) { i -> drawCircle(secondary.copy(alpha = alpha * 0.25f), w * (0.23f + i * 0.07f), planet, style = Stroke(1.5f)) }
            }

            ThemeCollection.OCEAN, ThemeCollection.BEACH, ThemeCollection.SCENERY -> {
                repeat(12) { index ->
                    val y = index * h / 11f
                    val path = Path().apply { moveTo(0f, y); repeat(50) { i -> lineTo(i * w / 49f, y + sin(i * 0.45f + time * 0.9f + index) * (8f + index)) } }
                    drawPath(path, if (index % 2 == 0) primary.copy(alpha = alpha * 0.36f) else secondary.copy(alpha = alpha * 0.24f), style = Stroke(2f + index * 0.25f))
                }
                repeat(14) { index ->
                    val p = Offset((index * 97f) % w, h - ((time * (13f + index) + index * 71f) % h))
                    drawCircle(Color.White.copy(alpha = alpha * 0.22f), 3f + index % 8, p, style = Stroke(1.5f))
                }
            }

            ThemeCollection.CANDY -> {
                repeat(16) { index ->
                    rotate(-18f) {
                        drawRect(if (index % 2 == 0) primary else secondary, Offset(index * w / 8f - w, -h), androidx.compose.ui.geometry.Size(w / 9f, h * 3f), alpha = alpha * 0.44f)
                    }
                }
                repeat(24) { index ->
                    val p = Offset((index * 79f + time * 7f) % w, (index * 113f + sin(time + index) * 30f) % h)
                    drawCircle(Color.White.copy(alpha = alpha * 0.32f), 4f + index % 9, p)
                }
            }

            ThemeCollection.ROCOCO_LOLITA -> {
                repeat(5) { ring ->
                    val radius = w * (0.1f + ring * 0.075f)
                    drawCircle(if (ring % 2 == 0) primary.copy(alpha = alpha * 0.4f) else secondary.copy(alpha = alpha * 0.34f), radius, centerOffset, style = Stroke(2f + ring))
                    repeat(10) { petal ->
                        val angle = petal * 2f * kotlin.math.PI.toFloat() / 10f + time * 0.08f * (ring + 1)
                        val p = centerOffset + Offset(cos(angle) * radius, sin(angle) * radius)
                        drawCircle(if (petal % 2 == 0) primary else Color.White, 3f + ring * 1.2f, p, alpha = alpha * 0.42f)
                    }
                }
                repeat(9) { index ->
                    val y = index * h / 8f
                    drawLine(Color.White.copy(alpha = alpha * 0.1f), Offset(0f, y), Offset(w, y), 1f)
                }
            }
        }

        if (t0Active > 0.5f) {
            repeat(3) { index ->
                val radius = 28f + ((time * 45f + index * 46f) % 140f)
                drawCircle(primary.copy(alpha = alpha * (0.42f - index * 0.1f)), radius, Offset(t0X, t0Y), style = Stroke(2f))
            }
        }
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
