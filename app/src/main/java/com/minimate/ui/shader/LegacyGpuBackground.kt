package com.minimate.ui.shader

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.minimate.touchpad.model.BackgroundTheme
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private const val VERTEX_SHADER = """
    attribute vec4 aPosition;
    void main() { gl_Position = aPosition; }
"""

private const val FRAGMENT_SHADER = """
    #ifdef GL_FRAGMENT_PRECISION_HIGH
    precision highp float;
    #else
    precision mediump float;
    #endif
    uniform vec2 uResolution;
    uniform float uTime;
    uniform float uTheme;
    uniform float uVariant;
    uniform vec2 uTouch;
    uniform float uTouchActive;
    uniform float uAlpha;

    float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
    float noise(vec2 p) {
        vec2 i = floor(p), f = fract(p);
        f = f * f * (vec2(3.0) - 2.0 * f);
        return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0)), f.x), f.y);
    }
    float fbm(vec2 p) {
        float v = 0.0;
        v += noise(p) * 0.5; p = p * 2.03 + vec2(17.1);
        v += noise(p) * 0.25; p = p * 2.01 + vec2(9.7);
        v += noise(p) * 0.125; p = p * 2.04 + vec2(5.3);
        v += noise(p) * 0.0625;
        return v;
    }
    vec3 hsv(float h, float s, float v) {
        vec3 k = vec3(1.0, 0.6667, 0.3333);
        vec3 p = abs(fract(vec3(h) + k) * 6.0 - vec3(3.0));
        return v * mix(vec3(1.0), clamp(p - vec3(1.0), vec3(0.0), vec3(1.0)), s);
    }
    float line(float d, float width) { return smoothstep(width, 0.0, abs(d)); }

    void main() {
        vec2 uv = gl_FragCoord.xy / uResolution.xy;
        vec2 p = (gl_FragCoord.xy - 0.5 * uResolution.xy) / uResolution.y;
        float t = uTime;
        int theme = int(uTheme + 0.5);
        float variant = uVariant;
        vec3 col = vec3(0.006, 0.008, 0.02);

        if (theme <= 8) {
            float n = fbm(p * (2.4 + mod(float(theme), 3.0)) + vec2(t * 0.09, -t * 0.06));
            float n2 = fbm(p * 5.0 + vec2(n * 2.0, t * 0.08));
            float hue = fract(float(theme) * 0.117 + variant * 0.19 + n * 0.22);
            vec3 a = hsv(hue, 0.72, 0.95);
            vec3 b = hsv(hue + 0.22, 0.68, 0.72);
            float folds = pow(abs(sin(n * 7.0 + n2 * 4.0 + t * 0.22)), 6.0);
            col = mix(vec3(0.006, 0.008, 0.018), mix(a, b, n2), n * 0.9) + folds * a * 0.55;
            if (theme == 8) col *= variant < 0.5 ? 0.0 : 0.12;
        } else if (theme == 10) {
            vec2 cell = floor((uv + vec2(t * 0.025, 0.0)) * vec2(48.0, 32.0));
            float star = step(0.91, hash(cell));
            float grid = step(0.92, fract(uv.x * 16.0)) + step(0.92, fract(uv.y * 12.0));
            vec3 neon = hsv(0.52 + variant * 0.18, 0.9, 1.0);
            col = vec3(0.008, 0.01, 0.035) + neon * star + hsv(0.92, 0.8, 1.0) * grid * 0.22;
            col *= 0.9 + 0.1 * sin(gl_FragCoord.y * 3.14159);
        } else if (theme == 11) {
            float horizon = 0.08;
            float z = 1.0 / max(0.025, abs(p.y - horizon));
            float road = step(p.y, horizon) * (line(fract(p.x * z * 0.16 + 0.5) - 0.5, 0.05) + line(fract(z * 0.07 - t * 0.5) - 0.5, 0.04));
            float sun = smoothstep(0.23, 0.21, length(p - vec2(0.0, 0.2)));
            vec3 neon = hsv(0.53 + variant * 0.17, 0.88, 1.0);
            col += neon * road * 1.25 + hsv(0.92 - variant * 0.12, 0.75, 1.0) * sun;
        } else if (theme == 12) {
            vec2 q = floor((p + vec2(t * 0.02, 0.0)) * 12.0);
            float facets = floor((noise(q * 0.35) + p.y + 0.5) * 7.0) / 7.0;
            vec3 low = hsv(0.55 + variant * 0.16, 0.65, 0.35);
            vec3 high = hsv(0.5 + variant * 0.14, 0.55, 1.0);
            col = mix(low, high, clamp(facets, 0.0, 1.0));
        } else if (theme == 13) {
            float smoke = fbm(p * 2.3 + vec2(t * 0.03));
            float vinyl = line(fract(length(p - vec2(0.2, 0.05)) * 24.0 - t * 0.2) - 0.5, 0.08);
            float keys = step(0.8, fract(uv.x * 12.0)) * step(uv.y, 0.28);
            vec3 brass = variant < 0.5 ? vec3(0.15, 0.4, 1.0) : (variant < 1.5 ? vec3(1.0, 0.62, 0.12) : vec3(0.75, 0.08, 0.28));
            col = vec3(0.012, 0.008, 0.02) + brass * smoke * 0.32 + brass * vinyl * 0.55 + vec3(keys * 0.18);
        } else if (theme == 14) {
            float w1 = line(p.y - sin(p.x * 14.0 + t * 2.2) * 0.13, 0.012);
            float w2 = line(p.y - cos(p.x * 23.0 - t * 1.4) * 0.06, 0.008);
            float bars = step(0.75, fract(uv.x * 24.0)) * step(abs(p.y), 0.12 + noise(vec2(floor(uv.x * 24.0), t)) * 0.18);
            col += hsv(0.52 + variant * 0.18, 0.86, 1.0) * w1 + hsv(0.78 + variant * 0.11, 0.82, 1.0) * (w2 + bars * 0.35);
        } else if (theme == 15) {
            float silk = sin(p.x * 9.0 + fbm(p * 2.5 + vec2(t * 0.03)) * 5.0 + t * 0.4);
            float sheen = pow(abs(silk), 10.0);
            vec3 a = hsv(0.84 + variant * 0.12, 0.45, 0.95);
            vec3 b = hsv(0.48 + variant * 0.15, 0.7, 0.75);
            col = mix(a * 0.12, b, silk * 0.5 + 0.5) + vec3(sheen * 0.7);
        } else if (theme == 16) {
            vec2 glyph = floor(vec2(uv.x * 38.0, (uv.y + t * 0.16) * 50.0));
            float code = step(0.76, hash(glyph));
            float glitch = step(0.96, hash(vec2(floor(uv.y * 28.0), floor(t * 7.0)))) * sin(uv.x * 150.0);
            vec3 ink = variant < 0.5 ? vec3(0.1, 1.0, 0.3) : (variant < 1.5 ? vec3(1.0, 0.1, 0.15) : vec3(0.85, 0.95, 1.0));
            col += ink * (code + abs(glitch) * 0.6);
        } else if (theme == 17) {
            float r = length(p) + 0.002, a = atan(p.y, p.x);
            float tunnel = pow(sin(15.0 / r - t * 2.8 + a * 5.0) * 0.5 + 0.5, 6.0);
            col += hsv(0.58 + variant * 0.15, 0.75, 1.0) * tunnel * smoothstep(0.72, 0.04, r);
        } else if (theme == 18) {
            float stars = step(0.975, hash(floor((uv + vec2(t * 0.006, 0.0)) * 80.0)));
            float orbit = line(length(p - vec2(0.1, 0.0)) - 0.28, 0.004);
            float chart = line(fract(atan(p.y, p.x) * 1.91) - 0.5, 0.025) * smoothstep(0.5, 0.05, length(p));
            vec3 ink = hsv(0.58 + variant * 0.12, 0.6, 1.0);
            col += vec3(stars) + ink * (orbit + chart * 0.3);
        } else if (theme == 19) {
            vec2 pc = vec2(0.0, -0.32);
            float planet = smoothstep(0.48, 0.465, length((p - pc) * vec2(1.0, 0.72)));
            float atmo = smoothstep(0.52, 0.465, length((p - pc) * vec2(1.0, 0.72))) - planet;
            float rings = line(length((p - pc) * vec2(1.0, 4.2)) - 0.56, 0.012);
            vec3 world = hsv(0.04 + variant * 0.23, 0.72, 1.0);
            col += world * planet * (0.25 + 0.55 * fbm(p * 6.0)) + world * atmo * 1.5 + world * rings;
        } else if (theme == 20 || theme == 21) {
            float water = fbm(p * 3.0 + vec2(0.0, t * 0.05));
            float caustic = pow(abs(sin((p.x + water * 0.12) * 20.0) * cos((p.y - water * 0.1) * 17.0)), 9.0);
            float snow = step(0.982, hash(floor(vec2(uv.x * 90.0, (uv.y + t * 0.03) * 75.0))));
            vec3 sea = hsv(0.48 + variant * 0.14, 0.72, 0.72);
            col = vec3(0.002, 0.012, 0.03) + sea * water + sea * caustic * 0.6 + vec3(snow * 0.55);
        } else if (theme == 22 || theme == 23) {
            float stripe = sin((p.x + p.y * 0.45 + t * 0.03) * 44.0) * 0.5 + 0.5;
            float candy = step(0.94, hash(floor((uv + vec2(t * 0.01, 0.0)) * 48.0)));
            vec3 a = hsv(0.92 + variant * 0.18, 0.62, 1.0);
            vec3 b = hsv(0.48 + variant * 0.16, 0.58, 1.0);
            col = mix(a, b, stripe) * 0.52 + vec3(candy);
        } else {
            float r = length(p), a = atan(p.y, p.x);
            float ornament = pow(abs(cos(a * (theme == 24 ? 6.0 : 8.0) + sin(r * 20.0 - t * 0.15))), 14.0) * smoothstep(0.52, 0.04, r);
            float lace = line(fract(r * 26.0) - 0.5, 0.08);
            vec3 rose = hsv(0.92 + variant * 0.12, 0.38, 0.95);
            vec3 gold = vec3(1.0, 0.7, 0.22);
            col = rose * (0.12 + fbm(p * 2.0) * 0.42) + gold * ornament + vec3(1.0) * lace * ornament * 0.35;
        }

        if (uTouchActive > 0.5) {
            vec2 touch = vec2(uTouch.x, uResolution.y - uTouch.y);
            float d = distance(gl_FragCoord.xy, touch) / uResolution.y;
            float ripple = sin(d * 70.0 - t * 8.0) * exp(-d * 8.0);
            col += vec3(max(ripple, 0.0) * 0.7);
        }
        col = col / (vec3(1.0) + max(col - vec3(1.0), vec3(0.0)));
        gl_FragColor = vec4(col * uAlpha, 1.0);
    }
"""

@Composable
internal fun LegacyGpuBackground(
    theme: BackgroundTheme,
    variantIndex: Int,
    animationSpeed: Float,
    touchX: Float,
    touchY: Float,
    touchActive: Float,
    dimRatio: Float,
    modifier: Modifier
) {
    val renderer = remember { LegacyRenderer() }
    val viewHolder = remember { arrayOfNulls<GLSurfaceView>(1) }
    AndroidView(
        factory = { context ->
            GLSurfaceView(context).also {
                viewHolder[0] = it
                it.setEGLContextClientVersion(2)
                it.setRenderer(renderer)
                it.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                it.preserveEGLContextOnPause = true
            }
        },
        update = {
            renderer.theme = theme.ordinal.toFloat()
            renderer.variant = variantIndex.toFloat()
            renderer.speed = animationSpeed
            renderer.touchX = touchX
            renderer.touchY = touchY
            renderer.touchActive = touchActive
            renderer.alpha = (1f - dimRatio * 0.95f).coerceIn(0f, 1f)
        },
        modifier = modifier
    )
    DisposableEffect(Unit) {
        onDispose { viewHolder[0]?.onPause() }
    }
}

private class LegacyRenderer : GLSurfaceView.Renderer {
    @Volatile var theme = 0f
    @Volatile var variant = 0f
    @Volatile var speed = 1f
    @Volatile var touchX = -1000f
    @Volatile var touchY = -1000f
    @Volatile var touchActive = 0f
    @Volatile var alpha = 1f

    private var program = 0
    private var width = 1
    private var height = 1
    private val startedAt = SystemClock.elapsedRealtime()
    private val vertices = java.nio.ByteBuffer.allocateDirect(8 * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)); position(0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.width = width.coerceAtLeast(1)
        this.height = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, this.width, this.height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (program == 0) return
        GLES20.glUseProgram(program)
        val position = GLES20.glGetAttribLocation(program, "aPosition")
        GLES20.glEnableVertexAttribArray(position)
        GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 0, vertices)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "uResolution"), width.toFloat(), height.toFloat())
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uTime"), (SystemClock.elapsedRealtime() - startedAt) / 1000f * speed)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uTheme"), theme)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uVariant"), variant)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "uTouch"), touchX, touchY)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uTouchActive"), touchActive)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uAlpha"), alpha)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(position)
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertex = compile(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragment = compile(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (vertex == 0 || fragment == 0) return 0
        val result = GLES20.glCreateProgram()
        GLES20.glAttachShader(result, vertex)
        GLES20.glAttachShader(result, fragment)
        GLES20.glLinkProgram(result)
        val status = IntArray(1)
        GLES20.glGetProgramiv(result, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e("MinimateGL", "Program link failed: ${GLES20.glGetProgramInfoLog(result)}")
            GLES20.glDeleteProgram(result)
            return 0
        }
        return result
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e("MinimateGL", "Shader compile failed: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }
}
