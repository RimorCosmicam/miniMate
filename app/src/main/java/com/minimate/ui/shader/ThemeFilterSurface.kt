package com.minimate.ui.shader

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import com.minimate.touchpad.model.ThemeFilter
import kotlin.math.sin

private const val FILTER_SHADER = """
    uniform shader content;
    uniform float uMode;
    uniform float uTime;
    uniform float2 uResolution;

    half4 main(float2 p) {
        if (uMode < 0.5) return content.eval(p);
        if (uMode < 1.5) {
            float wobble = sin(p.y * 0.018 + uTime * 1.7) * 1.4;
            half r = content.eval(p + float2(3.5 + wobble, 0.0)).r;
            half g = content.eval(p).g;
            half b = content.eval(p - float2(3.5 + wobble, 0.0)).b;
            return half4(r, g, b, 1.0);
        }
        if (uMode < 2.5) {
            float2 uv=p/uResolution,q=uv-.5;float r2=dot(q,q);float2 curved=(.5+q*(1.0+r2*.22))*uResolution;
            half4 c=content.eval(curved);float scan=.74+.26*sin(p.y*3.14159);float stripe=mod(floor(p.x),3.0);
            half3 mask=stripe<1.0?half3(1.0,.66,.58):(stripe<2.0?half3(.58,1.0,.66):half3(.66,.58,1.0));
            float2 edge=uv*(1.0-uv);float vignette=pow(clamp(edge.x*edge.y*18.0,0.0,1.0),.28);
            half3 bloom=content.eval(curved+float2(2.0,0.0)).rgb+content.eval(curved-float2(2.0,0.0)).rgb;
            return half4(c.rgb*mask*half(scan*vignette)+bloom*.055,c.a);
        }
        if (uMode < 3.5) {
            float tear = step(0.965, fract(p.y * 0.003 + uTime * 0.21));
            float jitter = sin(p.y * 0.035 + uTime * 8.0) * 2.2 + tear * 13.0;
            half4 c = content.eval(p + float2(jitter, 0.0));
            c.r = content.eval(p + float2(jitter + 2.5, 0.0)).r;
            return half4(c.rgb * (0.88 + 0.12 * sin(p.y * 0.72)), c.a);
        }
        if (uMode < 4.5) {
            float block = 7.0;
            return content.eval(floor(p / block) * block + block * 0.5);
        }
        if (uMode < 5.5) {
            half4 c = content.eval(p);
            half3 halo = content.eval(p + float2(5.0, 0.0)).rgb + content.eval(p - float2(5.0, 0.0)).rgb
                       + content.eval(p + float2(0.0, 5.0)).rgb + content.eval(p - float2(0.0, 5.0)).rgb;
            return half4(c.rgb * 0.78 + halo * 0.095, c.a);
        }
        if (uMode < 6.5) {
            half4 c = content.eval(p); half luma = dot(c.rgb, half3(0.299, 0.587, 0.114));
            luma = smoothstep(0.12, 0.88, luma); return half4(half3(luma), c.a);
        }
        float2 uv=p/uResolution;float2 sampleP=p;
        if(uMode<7.5){float2 q=(uv-.5)*float2(uResolution.x/uResolution.y,1.0);float r=length(q),a=atan(q.y,q.x);a=abs(fract(a/6.283185*8.0+.5)-.5)*6.283185/8.0;sampleP=(.5+float2(cos(a),sin(a))*r/float2(uResolution.x/uResolution.y,1.0))*uResolution;}
        else if(uMode<8.5){float2 q=uv-.5;float r2=dot(q,q);sampleP=(.5+q*(1.0+r2*1.4+r2*r2))*uResolution;}
        else if(uMode<14.5&&uMode>=13.5){float2 q=(uv-.5)*float2(uResolution.x/uResolution.y,1.0);float a=atan(q.y,q.x),r=length(q);a=abs(fract(a/6.283185*6.0+.5)-.5)*6.283185/6.0;sampleP=(.5+float2(cos(a),sin(a))*abs(fract(r*3.0)-.5)*.62/float2(uResolution.x/uResolution.y,1.0))*uResolution;}
        else if(uMode<15.5&&uMode>=14.5){float n=sin(uv.y*31.0+uTime)*cos(uv.x*27.0-uTime*.8);sampleP=p+float2(n,sin(n*4.0))*8.0;}
        half4 c=content.eval(sampleP);half luma=dot(c.rgb,half3(.299,.587,.114));
        if(uMode<9.5){float2 cell=fract(p/6.0)-.5;float dots=1.0-smoothstep(sqrt(float(luma))*.48,sqrt(float(luma))*.48+.08,length(cell));return half4(mix(half3(.01),c.rgb,half(dots)),c.a);}
        if(uMode<10.5){half3 cold=half3(.02,0,.25),mid=half3(.95,.03,0),hot=half3(1,.9,.08);half3 thermal=luma<.5?mix(cold,mid,luma*2):mix(mid,hot,(luma-.5)*2);return half4(thermal,c.a);}
        if(uMode<11.5)return half4(half3(1)-c.rgb,c.a);
        if(uMode<12.5)return half4(floor(c.rgb*5+half3(.5))/5,c.a);
        if(uMode<13.5){float grain=fract(sin(dot(p+floor(uTime*24),float2(12.9898,78.233)))*43758.5453)-.5;float vig=1.0-smoothstep(.3,.75,length(uv-.5));return half4(c.rgb*half(.7+.3*vig)+half3(grain*.13)+half3(.04,.015,-.01),c.a);}
        if(uMode<15.5)return c;
        float noise=fract(sin(dot(p+floor(uTime*20),float2(12.9898,78.233)))*43758.5453)-.5;float vig=1.0-smoothstep(.28,.72,length(uv-.5));return half4(half3(.03,luma*1.35+.12,.05)*half(vig)+half3(noise*.05),c.a);
    }
"""

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
internal fun ThemeFilterStack(
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
    val shader = remember { runCatching { RuntimeShader(FILTER_SHADER) }.getOrNull() }
    if (shader == null) {
        Box(modifier = modifier) {
            content()
            LegacyFilterOverlay(filter, Modifier.fillMaxSize())
        }
        return
    }
    val effect = remember(shader) {
        runCatching { RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect() }.getOrNull()
    }
    if (effect == null) {
        Box(modifier = modifier) {
            content()
            LegacyFilterOverlay(filter, Modifier.fillMaxSize())
        }
        return
    }
    val transition = rememberInfiniteTransition(label = "FilterClock")
    val time by transition.animateFloat(0f, 100f, infiniteRepeatable(tween(100_000, easing = LinearEasing)), label = "FilterTime")
    var width by remember { mutableFloatStateOf(1f) }
    var height by remember { mutableFloatStateOf(1f) }
    SideEffect {
        runCatching {
            shader.setFloatUniform("uMode", filter.ordinal.toFloat())
            shader.setFloatUniform("uTime", time)
            shader.setFloatUniform("uResolution", width, height)
        }
    }
    Box(modifier = modifier
        .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f); height = it.height.toFloat().coerceAtLeast(1f) }
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
            renderEffect = effect
        }) { content() }
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
            ThemeFilter.KALEIDOSCOPE, ThemeFilter.FISHEYE, ThemeFilter.MIRROR_PRISM,
            ThemeFilter.LIQUID_GLASS -> drawRect(Color.Cyan.copy(alpha = .035f))
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
            ThemeFilter.NIGHT_VISION -> drawRect(Color(0x5533FF66), blendMode = androidx.compose.ui.graphics.BlendMode.Color)
            ThemeFilter.NONE -> Unit
        }
    }
}
