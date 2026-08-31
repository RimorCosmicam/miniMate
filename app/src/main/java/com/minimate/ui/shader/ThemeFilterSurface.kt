package com.minimate.ui.shader

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.minimate.touchpad.model.ThemeFilter
import kotlin.math.sin

/**
 * Each filter compiles its own small, independent AGSL pass instead of sharing one
 * branching mega-shader driven by a mutable "uMode" uniform. Two reasons: a bug in one
 * filter's math can no longer affect the other sixteen, and — the actual cause filters
 * were doing nothing — mutating a single shared RuntimeShader's uniform in place does not
 * reliably invalidate a graphicsLayer's cached render; a genuinely new shader/RenderEffect
 * object per filter selection does.
 */
private const val COMMON_UNIFORMS = """
    uniform shader content;
    uniform float uTime;
    uniform float2 uResolution;

    /**
     * Sampling a layer outside its own bounds returns transparent, which composites to black.
     * That is where the black margins came from on every filter that displaces its lookup — VHS
     * pushed its scanlines sideways and read nothing back along the whole left edge. Clamping to
     * the edge keeps the displacement visible instead of punching a hole in the frame.
     *
     * The guard matters: if the layer size has not reached the shader yet, clamping to it would
     * collapse the entire frame into one corner pixel, which is worse than the margin.
     */
    half4 tap(float2 p) {
        return uResolution.x > 1.0
            ? content.eval(clamp(p, float2(0.5), uResolution - float2(0.5)))
            : content.eval(p);
    }

    half4 tapUV(float2 uv) { return tap(uv * uResolution); }

    float rnd(float2 seed) { return fract(sin(dot(seed, float2(12.9898, 78.233))) * 43758.5453); }
"""

/** The exact source a filter compiles, so a test can check it without driving the UI. */
internal fun filterShaderSource(filter: ThemeFilter): String = COMMON_UNIFORMS + shaderBodyFor(filter)

private fun shaderBodyFor(filter: ThemeFilter): String = when (filter) {
    ThemeFilter.NONE -> "half4 main(float2 p) { return tap(p); }"
    ThemeFilter.CHROMATIC -> """
        half4 main(float2 p) {
            float wobble = sin(p.y * 0.018 + uTime * 1.7) * 1.4;
            half r = tap(p + float2(3.5 + wobble, 0.0)).r;
            half g = tap(p).g;
            half b = tap(p - float2(3.5 + wobble, 0.0)).b;
            return half4(r, g, b, 1.0);
        }
    """
    ThemeFilter.CRT -> """
        half4 main(float2 p) {
            float2 uv=p/uResolution,q=uv-.5;float r2=dot(q,q);float2 curved=(.5+q*(1.0+r2*.22))*uResolution;
            half4 c=tap(curved);float scan=.74+.26*sin(p.y*3.14159);float stripe=mod(floor(p.x),3.0);
            half3 mask=stripe<1.0?half3(1.0,.66,.58):(stripe<2.0?half3(.58,1.0,.66):half3(.66,.58,1.0));
            float2 edge=uv*(1.0-uv);float vignette=pow(clamp(edge.x*edge.y*18.0,0.0,1.0),.28);
            half3 bloom=tap(curved+float2(2.0,0.0)).rgb+tap(curved-float2(2.0,0.0)).rgb;
            return half4(c.rgb*mask*half(scan*vignette)+bloom*.055,c.a);
        }
    """
    ThemeFilter.VHS -> """
        half4 main(float2 p) {
            float2 uv = p / uResolution;

            // Tracking error: a band drifting up the frame where the head is misaligned, inside
            // which whole scanlines are thrown sideways by a different amount each line.
            float band = smoothstep(0.55, 1.0, sin(uv.y * 7.0 - uTime * 0.8) * 0.5 + 0.5);
            float lineNoise = rnd(float2(floor(p.y), floor(uTime * 24.0))) - 0.5;
            float shift = lineNoise * band * 0.045 + sin(uv.y * 90.0 + uTime * 6.0) * 0.0012;

            // Head switching: the torn strip at the very bottom of every VHS frame, where the
            // head leaves the tape before the next field starts. It is the single artefact that
            // says "tape" more than any amount of noise.
            float head = smoothstep(0.035, 0.0, uv.y);
            shift += head * (rnd(float2(floor(p.y * 0.7), floor(uTime * 18.0))) - 0.5) * 0.22;

            float2 q = float2(uv.x + shift, uv.y);

            // Luminance and chrominance are recorded separately, and chroma at a fraction of the
            // bandwidth. Smearing only the colour sideways — and leaving the luma sharp — is what
            // makes it read as tape rather than as a blurred image.
            half3 sharp = tapUV(q).rgb;
            half3 smeared = half3(0.0);
            for (int i = 0; i < 5; i++) {
                smeared += tapUV(q - float2(float(i) * 0.005, 0.0)).rgb;
            }
            smeared *= 0.2;

            half luma = dot(sharp, half3(0.299, 0.587, 0.114));
            half chromaLuma = dot(smeared, half3(0.299, 0.587, 0.114));
            half3 c = half3(luma) + (smeared - half3(chromaLuma)) * half(1.25);

            // Dropouts: flecks where the tape has shed oxide and the head reads nothing at all.
            float speck = rnd(float2(floor(p.x * 0.4), floor(p.y) + floor(uTime * 20.0) * 31.0));
            c = mix(c, half3(0.72), half(smoothstep(0.9975, 1.0, speck)));

            // Tape hiss, and the faint horizontal ruling of the scanlines.
            float hiss = rnd(p + floor(uTime * 24.0)) - 0.5;
            c += half3(half(hiss * 0.055));
            c *= half(0.94 + 0.06 * sin(p.y * 2.1));

            // Tape runs slightly hot in the highlights and lifts the blacks; it never crushes to
            // the flat black the old version multiplied everything toward.
            c = clamp(c * half(1.06) + half3(0.015, 0.012, 0.02), half3(0.0), half3(1.0));
            return half4(c, 1.0);
        }
    """
    ThemeFilter.PIXELATE -> """
        half4 main(float2 p) {
            float block = 7.0;
            return tap(floor(p / block) * block + block * 0.5);
        }
    """
    ThemeFilter.DREAM_BLOOM -> """
        half4 main(float2 p) {
            half4 c = tap(p);
            half3 halo = tap(p + float2(5.0, 0.0)).rgb + tap(p - float2(5.0, 0.0)).rgb
                       + tap(p + float2(0.0, 5.0)).rgb + tap(p - float2(0.0, 5.0)).rgb;
            return half4(c.rgb * 0.78 + halo * 0.095, c.a);
        }
    """
    ThemeFilter.MONO_INK -> """
        half4 main(float2 p) {
            half4 c = tap(p); half luma = dot(c.rgb, half3(0.299, 0.587, 0.114));
            luma = smoothstep(0.12, 0.88, luma); return half4(half3(luma), c.a);
        }
    """
    ThemeFilter.KALEIDOSCOPE -> """
        half4 main(float2 p) {
            float2 uv=p/uResolution;float2 q=(uv-.5)*float2(uResolution.x/uResolution.y,1.0);
            float r=length(q),a=atan(q.y,q.x);a=abs(fract(a/6.283185*8.0+.5)-.5)*6.283185/8.0;
            float2 sampleP=(.5+float2(cos(a),sin(a))*r/float2(uResolution.x/uResolution.y,1.0))*uResolution;
            return tap(sampleP);
        }
    """
    ThemeFilter.FISHEYE -> """
        half4 main(float2 p) {
            float2 uv=p/uResolution;float2 q=uv-.5;float r2=dot(q,q);
            float2 sampleP=(.5+q*(1.0+r2*1.4+r2*r2))*uResolution;
            return tap(sampleP);
        }
    """
    ThemeFilter.HALFTONE -> """
        half4 main(float2 p) {
            half4 c=tap(p);half luma=dot(c.rgb,half3(.299,.587,.114));
            float2 cell=fract(p/6.0)-.5;
            float dots=1.0-smoothstep(sqrt(float(luma))*.48,sqrt(float(luma))*.48+.08,length(cell));
            return half4(mix(half3(.01),c.rgb,half(dots)),c.a);
        }
    """
    ThemeFilter.THERMAL -> """
        half4 main(float2 p) {
            half4 c=tap(p);half luma=dot(c.rgb,half3(.299,.587,.114));
            half3 cold=half3(.02,0,.25),mid=half3(.95,.03,0),hot=half3(1,.9,.08);
            half3 thermal=luma<.5?mix(cold,mid,luma*2):mix(mid,hot,(luma-.5)*2);
            return half4(thermal,c.a);
        }
    """
    ThemeFilter.NEGATIVE -> """
        half4 main(float2 p) { half4 c=tap(p); return half4(half3(1)-c.rgb,c.a); }
    """
    ThemeFilter.POSTERIZE -> """
        half4 main(float2 p) { half4 c=tap(p); return half4(floor(c.rgb*5+half3(.5))/5,c.a); }
    """
    ThemeFilter.FILM_GRAIN -> """
        half4 main(float2 p) {
            half4 c=tap(p);float2 uv=p/uResolution;
            float grain=fract(sin(dot(p+floor(uTime*24),float2(12.9898,78.233)))*43758.5453)-.5;
            float vig=1.0-smoothstep(.3,.75,length(uv-.5));
            return half4(c.rgb*half(.7+.3*vig)+half3(grain*.13)+half3(.04,.015,-.01),c.a);
        }
    """
    ThemeFilter.MIRROR_PRISM -> """
        half4 main(float2 p) {
            float2 uv=p/uResolution;float2 q=(uv-.5)*float2(uResolution.x/uResolution.y,1.0);
            float a=atan(q.y,q.x),r=length(q);a=abs(fract(a/6.283185*6.0+.5)-.5)*6.283185/6.0;
            float2 sampleP=(.5+float2(cos(a),sin(a))*abs(fract(r*3.0)-.5)*.62/float2(uResolution.x/uResolution.y,1.0))*uResolution;
            return tap(sampleP);
        }
    """
}

@Composable
internal fun ThemeFilterSurface(
    filter: ThemeFilter,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    if (filter == ThemeFilter.NONE) {
        Box(modifier = modifier) { content() }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ModernFilteredSurface(filter, modifier, content)
    } else {
        Box(modifier = modifier) {
            content()
            LegacyFilterOverlay(filter, Modifier.fillMaxSize())
        }
    }
}

/** Applies each selected filter as a real render pass, preserving the user's stack order. */
@Composable
fun ThemeFilterStack(
    filters: List<ThemeFilter>,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    // Each pass is a hardware graphics layer. Bounding the live stack prevents
    // HWUI/minikin resource exhaustion on Samsung's cover display.
    val active = filters.filter { it != ThemeFilter.NONE }.distinct().takeLast(4)
    Box(modifier = modifier) {
        StackedFilterLayers(active, active.lastIndex, content)
    }
}

@Composable
private fun StackedFilterLayers(filters: List<ThemeFilter>, index: Int, content: @Composable () -> Unit) {
    if (index < 0) {
        content()
    } else {
        ThemeFilterSurface(filters[index], Modifier.fillMaxSize()) {
            StackedFilterLayers(filters, index - 1, content)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ModernFilteredSurface(filter: ThemeFilter, modifier: Modifier, content: @Composable () -> Unit) {
    val shader = remember(filter) {
        runCatching { RuntimeShader(filterShaderSource(filter)) }
            .onFailure { Log.e("MiniMateFilter", "Shader for $filter failed to compile, falling back to overlay", it) }
            .getOrNull()
    }
    if (shader == null) {
        Box(modifier = modifier) {
            content()
            LegacyFilterOverlay(filter, Modifier.fillMaxSize())
        }
        return
    }
    val transition = rememberInfiniteTransition(label = "FilterClock")
    val time by transition.animateFloat(0f, 100f, infiniteRepeatable(tween(100_000, easing = LinearEasing)), label = "FilterTime")

    // Both uniforms are written and the effect rebuilt inside the graphicsLayer block, which runs
    // in the draw phase. That matters twice over. Setting a uniform on a RuntimeShader after its
    // RenderEffect exists does not reliably reach the composited result, so the previous version
    // animated only by luck and left uResolution wherever it happened to land. And the layer's
    // own size is exact here, rather than being inferred from constraints a frame late.
    Box(
        modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
            renderEffect = runCatching {
                shader.setFloatUniform("uTime", time)
                shader.setFloatUniform("uResolution", size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))
                RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
            }.onFailure {
                Log.e("MiniMateFilter", "RenderEffect for $filter failed, drawing unfiltered", it)
            }.getOrNull()
        }
    ) { content() }
}

@Composable
private fun LegacyFilterOverlay(filter: ThemeFilter, modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "LegacyFilter")
    val time by transition.animateFloat(0f, 100f, infiniteRepeatable(tween(100_000, easing = LinearEasing)), label = "LegacyFilterTime")
    Canvas(modifier) {
        when (filter) {
            ThemeFilter.CRT -> repeat(90) { i ->
                drawRect(Color.Black.copy(alpha = .16f), androidx.compose.ui.geometry.Offset(0f, i * size.height / 90f), androidx.compose.ui.geometry.Size(size.width, 1.5f))
            }
            ThemeFilter.VHS -> repeat(4) { i ->
                val y = ((time * (18f + i * 7f) + i * size.height * .27f) % size.height)
                drawRect(Color.White.copy(alpha = .08f), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, 3f + i))
            }
            ThemeFilter.CHROMATIC -> {
                drawRect(Color.Red.copy(alpha = .035f))
                repeat(9) { i -> drawRect(Color.Cyan.copy(alpha = .025f), androidx.compose.ui.geometry.Offset(sin(time + i) * 5f, i * size.height / 9f), androidx.compose.ui.geometry.Size(size.width, 2f)) }
            }
            ThemeFilter.PIXELATE -> repeat(40) { i -> drawRect(Color.Black.copy(alpha = .035f), androidx.compose.ui.geometry.Offset(i * size.width / 40f, 0f), androidx.compose.ui.geometry.Size(1.5f, size.height)) }
            ThemeFilter.DREAM_BLOOM -> drawRect(Color.White.copy(alpha = .08f))
            ThemeFilter.MONO_INK -> drawRect(Color.Black.copy(alpha = .2f))
            ThemeFilter.KALEIDOSCOPE, ThemeFilter.FISHEYE,
            ThemeFilter.MIRROR_PRISM -> drawRect(Color.Cyan.copy(alpha = .035f))
            ThemeFilter.HALFTONE -> repeat(32) { i ->
                repeat(20) { j ->
                    drawCircle(Color.Black.copy(alpha = .16f), 1.4f, androidx.compose.ui.geometry.Offset(i * size.width / 31f, j * size.height / 19f))
                }
            }
            ThemeFilter.THERMAL -> drawRect(Color(0x33FF3D00))
            ThemeFilter.NEGATIVE -> drawRect(Color.White, blendMode = androidx.compose.ui.graphics.BlendMode.Difference)
            ThemeFilter.POSTERIZE -> drawRect(Color.Black.copy(alpha = .08f))
            ThemeFilter.FILM_GRAIN -> repeat(120) { i ->
                val x = ((i * 73f + time * 97f) % size.width)
                val y = ((i * 41f + time * 61f) % size.height)
                drawCircle(Color.White.copy(alpha = .09f), 1f, androidx.compose.ui.geometry.Offset(x, y))
            }
            ThemeFilter.NONE -> Unit
        }
    }
}
