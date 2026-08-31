package com.minimate.ui.shader

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import com.minimate.touchpad.model.ShaderScene
import com.minimate.touchpad.engine.TouchPoint
import kotlinx.coroutines.delay

/**
 * Shared prelude for every scene.
 *
 * Scenes only declare a scene() body; everything reusable lives here so each one stays readable.
 * Parameters arrive as uP0..uP3 in the order the scene declared them, so a scene's controls and
 * its uniforms cannot drift apart.
 */
private const val HEADER = """
uniform float2 uResolution;
uniform float uTime;
uniform float uNow;
uniform float uP0;
uniform float uP1;
uniform float uP2;
uniform float uP3;
uniform float3 uC0;
uniform float3 uC1;
uniform float3 uC2;
uniform float3 uC3;
uniform float2 uTouches[8];
uniform float uTouchAges[8];
uniform float uTouchActive[8];
uniform float uTouchCount;
uniform float uTouchStrength;
uniform float uAberration;
uniform float uGrain;
uniform shader glyphAtlas;

float hash(float2 p){return fract(sin(dot(p,float2(127.1,311.7)))*43758.5453123);}
float2 hash2(float2 p){return fract(sin(float2(dot(p,float2(127.1,311.7)),dot(p,float2(269.5,183.3))))*43758.5453);}
float noise(float2 p){float2 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);return mix(mix(hash(i),hash(i+float2(1,0)),f.x),mix(hash(i+float2(0,1)),hash(i+1.0),f.x),f.y);}
float fbm(float2 p){float v=0.0;v+=noise(p)*.5;p=p*2.03+17.1;v+=noise(p)*.25;p=p*2.01+9.7;v+=noise(p)*.125;p=p*2.04+5.3;return v+noise(p)*.0625;}
float2 rot(float2 p,float a){float c=cos(a),s=sin(a);return float2(c*p.x-s*p.y,s*p.x+c*p.y);}

/** Four-stop ramp through the scene palette. */
float3 pal(float v){
    v=clamp(v,0.0,1.0);
    if(v<.3333) return mix(uC0,uC1,v*3.0);
    if(v<.6666) return mix(uC1,uC2,(v-.3333)*3.0);
    return mix(uC2,uC3,(v-.6666)*3.0);
}

/**
 * One glyph from the atlas. The atlas is 16 columns by 4 rows of 32 px cells: katakana first,
 * then binary, hexadecimal and a row of words. Ids wrap, so any id is valid.
 */
float glyph(float id, float2 g){
    float col = mod(id, 16.0);
    float row = mod(floor(id / 16.0), 4.0);
    float2 c = float2(col * 32.0 + clamp(g.x, 0.0, 1.0) * 32.0,
                      row * 32.0 + clamp(g.y, 0.0, 1.0) * 32.0);
    return glyphAtlas.eval(c).r;
}

// ---------------------------------------------------------------- 3D toolkit
//
// Signed distance primitives, shading and tone mapping, so scenes can raymarch a lit surface
// rather than tint a 2D pattern. AGSL has no function pointers, so each raymarching scene writes
// its own map() and march loop; what is shared is everything around that.

float sdSphere(float3 p, float r){ return length(p) - r; }
float sdBox3(float3 p, float3 b){ float3 d = abs(p) - b; return length(max(d, 0.0)) + min(max(d.x, max(d.y, d.z)), 0.0); }
float sdTorus(float3 p, float2 t){ float2 q = float2(length(p.xz) - t.x, p.y); return length(q) - t.y; }
float sdPlane(float3 p, float h){ return p.y - h; }

/** Smooth union. The blend is what makes separate blobs read as one liquid body. */
float smin(float a, float b, float k){
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

float3 rotY(float3 p, float a){ float c = cos(a), s = sin(a); return float3(c * p.x - s * p.z, p.y, s * p.x + c * p.z); }
float3 rotX(float3 p, float a){ float c = cos(a), s = sin(a); return float3(p.x, c * p.y - s * p.z, s * p.y + c * p.z); }

float3 hash33(float3 p){
    p = fract(p * float3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.xxy + p.yxx) * p.zyx);
}

/** Grazing angles reflect more. Without this, metal and glass read as flat coloured plastic. */
float fresnel(float3 n, float3 v, float power){
    return pow(1.0 - clamp(dot(n, -v), 0.0, 1.0), power);
}

/** Filmic tone curve. Linear output clips to white the moment anything is lit brightly. */
float3 aces(float3 x){
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

/** Touch displacement, shared by every scene so interaction feels consistent across the set. */
float2 touchWarp(float2 q){
    for(int i=0;i<8;i++){
        if(float(i) < uTouchCount){
            float age = uTouchAges[i];
            float2 d = q - uTouches[i];
            float l = max(length(d), 0.008);
            // Held contacts stay at full strength; released ones fade over about a second.
            float life = max(uTouchActive[i], exp(-age * 2.6));
            float wave = sin(l * 40.0 - age * 8.0) * exp(-l * 6.0) * life;
            q += d / l * wave * 0.030 * uTouchStrength;
        }
    }
    return q;
}

"""

private const val FOOTER = """
half4 main(float2 fragCoord){
    float2 res = uResolution;
    float2 uv = fragCoord / res;
    float2 p = (fragCoord - 0.5 * res) / min(res.x, res.y);
    float2 wp = touchWarp(p);
    float2 wuv = uv + (wp - p);

    float3 c = aces(scene(wp, wuv, uTime));

    // Grain goes on after tone mapping so it sits in the image rather than being crushed by it,
    // and eases off in the highlights, where sensor noise genuinely is less visible.
    float g = hash(fragCoord + fract(uTime) * 917.0) - 0.5;
    c += g * uGrain * 0.055 * (1.0 - 0.6 * dot(c, float3(0.2126, 0.7152, 0.0722)));

    float r = length(p);
    c *= 1.0 - 0.26 * r * r;
    return half4(clamp(c, 0.0, 1.0), 1.0);
}
"""

/**
 * The lens, as a pass over the finished frame rather than three more trips through the scene.
 *
 * Dispersion needs the image sampled at three slightly different points, and doing that inside the
 * scene shader meant evaluating the whole scene three times for every pixel — tripling the cost of
 * a full-screen shader that already runs continuously, which is heat. Here the scene is drawn once
 * and the three samples are texture reads off the result, which costs almost nothing. The offset is
 * radial and grows toward the edges, because that is what a lens does; a uniform shift just looks
 * like three misregistered copies.
 */
private const val LENS_SHADER = """
uniform shader content;
uniform float2 uSize;
uniform float uAmount;

half4 main(float2 p){
    float2 mid = uSize * 0.5;
    float2 d = p - mid;
    float radius = length(d) / max(length(mid), 1.0);
    float2 dir = d / max(length(d), 0.001);
    float2 offset = dir * uAmount * (0.35 + radius) * min(uSize.x, uSize.y) * 0.0055;
    float2 low = float2(0.5);
    float2 high = uSize - float2(0.5);
    half4 mids = content.eval(p);
    return half4(
        content.eval(clamp(p + offset, low, high)).r,
        mids.g,
        content.eval(clamp(p - offset, low, high)).b,
        mids.a);
}
"""

internal fun sceneShaderSource(scene: ShaderScene): String = HEADER + scene.body + FOOTER

/**
 * The atlas the Matrix scenes read from. Katakana leads because that is what makes the rain read
 * as itself rather than as generic falling characters; the remaining rows give the other scenes
 * something less literal to work with.
 */
private fun createGlyphAtlas(): Bitmap {
    val cell = 32
    val rows = listOf(
        "アイウエオカキクケコサシスセソン",
        "ヤユヨラリルレロワヲタチツテトナ",
        "0123456789ABCDEF",
        "0101101001011010"
    )
    return Bitmap.createBitmap(cell * 16, cell * rows.size, Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(AndroidColor.BLACK)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 24f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        rows.forEachIndexed { r, chars ->
            chars.take(16).forEachIndexed { c, ch ->
                canvas.drawText(ch.toString(), c * cell + cell * .5f, r * cell + cell * .76f, paint)
            }
        }
    }
}

private fun rgb(color: Long) = floatArrayOf(
    ((color shr 16) and 255).toFloat() / 255f,
    ((color shr 8) and 255).toFloat() / 255f,
    (color and 255).toFloat() / 255f
)

@Composable
fun SceneShaderCanvas(
    scene: ShaderScene,
    params: List<Float>,
    palette: List<Long>,
    touchPoints: List<TouchPoint>,
    animationSpeed: Float,
    touchStrength: Float,
    aberration: Float,
    grain: Float,
    modifier: Modifier = Modifier
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        SceneFallback(palette, modifier)
        return
    }
    SceneAgsl(scene, params, palette, touchPoints, animationSpeed, touchStrength, aberration, grain, modifier)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun SceneAgsl(
    scene: ShaderScene,
    params: List<Float>,
    palette: List<Long>,
    touchPoints: List<TouchPoint>,
    animationSpeed: Float,
    touchStrength: Float,
    aberration: Float,
    grain: Float,
    modifier: Modifier
) {
    // Building and linking the GPU program blocks the render thread on a cold shader cache, long
    // enough on a fresh install to trip the ANR watchdog. Present the cheap fallback for the
    // first frames so the window is up and dispatching input before that work begins.
    var ready by remember(scene.id) { mutableStateOf(false) }
    LaunchedEffect(scene.id) {
        withFrameNanos { }
        withFrameNanos { }
        delay(90)
        ready = true
    }

    val runtime = remember(scene.id, ready) {
        if (!ready) null else runCatching {
            RuntimeShader(sceneShaderSource(scene)).apply {
                setInputShader(
                    "glyphAtlas",
                    BitmapShader(createGlyphAtlas(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                )
            }
        }.onFailure {
            Log.e("MiniMateShader", "scene ${scene.id} failed to compile", it)
        }.getOrNull()
    }

    if (runtime == null) {
        SceneFallback(palette, modifier)
        return
    }

    val transition = rememberInfiniteTransition(label = "sceneTime")
    val time by transition.animateFloat(
        0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "t"
    )

    // Scenes that split wavelengths in their own material — a prism, a Doppler-shifted disk —
    // have already done this where it physically happens, and a lens pass on top would only
    // fringe them twice.
    val lens = remember(scene.dispersive) {
        if (scene.dispersive) null else runCatching { RuntimeShader(LENS_SHADER) }.getOrNull()
    }
    val lensModifier = if (lens == null || aberration <= 0.001f) Modifier else Modifier.graphicsLayer {
        renderEffect = runCatching {
            lens.setFloatUniform("uSize", size.width, size.height)
            lens.setFloatUniform("uAmount", aberration)
            RenderEffect.createRuntimeShaderEffect(lens, "content").asComposeRenderEffect()
        }.getOrNull()
    }

    Canvas(modifier.fillMaxSize().then(lensModifier)) {
        val stops = if (palette.size >= 4) palette else listOf(0xFF000000L, 0xFF202020L, 0xFF808080L, 0xFFFFFFFFL)
        runtime.setFloatUniform("uResolution", size.width, size.height)
        runtime.setFloatUniform("uTime", time * animationSpeed)
        runtime.setFloatUniform("uNow", time)
        runtime.setFloatUniform("uTouchStrength", touchStrength)
        runtime.setFloatUniform("uAberration", aberration)
        runtime.setFloatUniform("uGrain", grain)
        // Uniform slots follow the scene's declared order, and unused slots are held at their
        // defaults rather than left undefined.
        for (i in 0 until 4) {
            val value = params.getOrNull(i) ?: scene.params.getOrNull(i)?.default ?: 0f
            runtime.setFloatUniform("uP$i", value)
        }
        for (i in 0 until 4) {
            val c = rgb(stops[i])
            runtime.setFloatUniform("uC$i", c[0], c[1], c[2])
        }

        val nowSeconds = android.os.SystemClock.elapsedRealtime() / 1000f
        val points = touchPoints.takeLast(8)
        val positions = FloatArray(16) { -10f }
        val ages = FloatArray(8) { 99f }
        val active = FloatArray(8)
        points.forEachIndexed { index, point ->
            positions[index * 2] = (point.x / size.width - 0.5f) * (size.width / minOf(size.width, size.height))
            positions[index * 2 + 1] = (point.y / size.height - 0.5f) * (size.height / minOf(size.width, size.height))
            // Stamped with elapsedRealtime, so the age has to be measured against the same clock.
            // Comparing it to animation time made every released touch read as brand new.
            ages[index] = (nowSeconds - point.startedAtSeconds).coerceAtLeast(0f)
            active[index] = if (point.active) 1f else 0f
        }
        runtime.setFloatUniform("uTouches", positions)
        runtime.setFloatUniform("uTouchAges", ages)
        runtime.setFloatUniform("uTouchActive", active)
        runtime.setFloatUniform("uTouchCount", points.size.toFloat())

        drawContext.canvas.nativeCanvas.drawPaint(
            Paint().apply { shader = runtime }
        )
    }
}

/** Static gradient for pre-Tiramisu devices and for the frames before a scene is compiled. */
@Composable
private fun SceneFallback(palette: List<Long>, modifier: Modifier) {
    val stops = if (palette.size >= 4) palette else listOf(0xFF000000L, 0xFF202020L, 0xFF808080L, 0xFFFFFFFFL)
    androidx.compose.foundation.layout.Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(stops.map { Color(it) }))
    )
}
