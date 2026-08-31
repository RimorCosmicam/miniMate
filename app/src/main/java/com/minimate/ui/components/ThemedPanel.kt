package com.minimate.ui.components

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.minimate.touchpad.model.PanelLayout
import com.minimate.touchpad.model.PanelMaterial
import com.minimate.touchpad.model.PanelTheme
import kotlin.math.roundToInt

/**
 * Refraction through a thick pane.
 *
 * The bend is strongest just inside the rim and falls to nothing across the middle, because that
 * is the shape of a lens — a uniform displacement reads as a smeared copy rather than as glass.
 * The three channels are pulled by slightly different amounts, so the rim picks up the colour
 * fringe a real edge shows.
 */
private const val GLASS_SHADER = """
    uniform shader content;
    uniform float2 uOrigin;
    uniform float2 uSize;
    uniform float uRadius;
    uniform float2 uCanvas;

    half4 tap(float2 p) {
        return content.eval(clamp(p, float2(0.5), uCanvas - float2(0.5)));
    }

    half4 main(float2 p) {
        float2 local = p - uOrigin;
        float2 mid = uSize * 0.5;
        float2 q = abs(local - mid) - (mid - float2(uRadius));
        float sd = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - uRadius;

        // Outside the panel nothing is glass, and the layer is clipped there anyway.
        if (sd > 0.0) return tap(p);

        float depth = clamp(-sd / max(min(mid.x, mid.y), 1.0), 0.0, 1.0);
        float bend = pow(1.0 - depth, 3.0) * min(uSize.x, uSize.y) * 0.16;
        float2 dir = (local - mid) / max(length(local - mid), 0.001);

        half4 c;
        c.r = tap(p - dir * bend * 1.06).r;
        c.g = tap(p - dir * bend).g;
        c.b = tap(p - dir * bend * 0.94).b;
        c.a = 1.0;

        // A bright rim where the glass turns away, and a soft sheen across the upper face.
        float rim = smoothstep(0.10, 0.0, depth);
        float sheen = clamp(1.0 - length((local - mid * float2(1.0, 0.55)) / uSize), 0.0, 1.0);
        c.rgb += half3(half(rim * 0.30 + sheen * sheen * 0.10));
        return c;
    }
"""

/** Uniforms are only settable from API 33, which is also the only place the shader is built. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun glassEffect(
    shader: RuntimeShader,
    originX: Float,
    originY: Float,
    panelWidth: Float,
    panelHeight: Float,
    radiusPx: Float,
    canvasWidth: Float,
    canvasHeight: Float
): AndroidRenderEffect {
    shader.setFloatUniform("uOrigin", originX, originY)
    shader.setFloatUniform("uSize", panelWidth, panelHeight)
    shader.setFloatUniform("uRadius", radiusPx)
    shader.setFloatUniform("uCanvas", canvasWidth, canvasHeight)
    return AndroidRenderEffect.createRuntimeShaderEffect(shader, "content")
}

/**
 * A floating panel that can be moved, resized and restyled.
 *
 * Position is stored as a fraction of the display so a panel dragged clear of the camera cutout
 * stays clear of it, and the same layout is meaningful on both of this device's screens. While
 * editing, one finger moves and two resize; outside editing neither is live, so an ordinary tap
 * on a control is never mistaken for a drag.
 *
 * [backdrop] draws whatever is behind the panel, at full canvas size. The glass materials need it:
 * a panel that only tints itself cannot refract or blur anything, because it has nothing to
 * refract. It is offset back to the canvas origin and clipped to the panel, so what shows through
 * is exactly the region the panel covers.
 */
@Composable
fun ThemedPanel(
    theme: PanelTheme,
    layout: PanelLayout,
    editing: Boolean,
    onLayoutChange: (PanelLayout) -> Unit,
    modifier: Modifier = Modifier,
    widthFraction: Float = .70f,
    backdrop: (@Composable (Modifier) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()
        val scale = layout.scale.coerceIn(.6f, 1.6f)
        val shape = RoundedCornerShape(theme.cornerRadius.dp)

        val panelWidth = canvasWidth * widthFraction * scale
        // Centre on the stored fraction rather than anchoring a corner, so resizing grows the
        // panel about its own middle instead of walking it across the screen.
        val originX = (layout.x * canvasWidth - panelWidth / 2f)
            .coerceIn(0f, (canvasWidth - panelWidth).coerceAtLeast(0f))
        val originY = (layout.y * canvasHeight - canvasHeight * .18f)
            .coerceIn(0f, canvasHeight * .8f)

        val bare = theme.material == PanelMaterial.MONT && theme.cornerRadius == 0
        var panelModifier = Modifier
            .fillMaxWidth(widthFraction * scale)
            .offset { IntOffset(originX.roundToInt(), originY.roundToInt()) }
            .then(if (bare) Modifier.clipToBounds() else Modifier.clip(shape))

        if (editing) {
            // One gesture detector, not two. Drag and transform detectors both consume the same
            // pointer stream, so the pinch that was layered on top of the drag never saw an event.
            panelModifier = panelModifier
                .pointerInput(layout) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        onLayoutChange(
                            layout.copy(
                                x = (layout.x + pan.x / canvasWidth).coerceIn(.1f, .9f),
                                y = (layout.y + pan.y / canvasHeight).coerceIn(.05f, .95f),
                                scale = (layout.scale * zoom).coerceIn(.6f, 1.6f)
                            )
                        )
                    }
                }
                .border(1.dp, Color.White.copy(.75f), if (bare) RoundedCornerShape(0.dp) else shape)
        }

        Box(panelModifier) {
            PanelSurface(theme, backdrop, originX, originY, canvasWidth, canvasHeight, shape)
            Column(
                Modifier.padding(
                    horizontal = if (theme.material == PanelMaterial.MONT) 18.dp else 11.dp,
                    vertical = if (theme.material == PanelMaterial.MONT) 14.dp else 9.dp
                )
            ) { content() }
        }
    }
}

/** The panel body: backdrop treatment first where the material has one, then the tint and edge. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.PanelSurface(
    theme: PanelTheme,
    backdrop: (@Composable (Modifier) -> Unit)?,
    originX: Float,
    originY: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    shape: RoundedCornerShape
) {
    val density = LocalDensity.current
    val needsBackdrop = theme.material == PanelMaterial.LIQUID_GLASS ||
        theme.material == PanelMaterial.FROSTED

    if (needsBackdrop && backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val glass = remember(theme.material) {
            if (theme.material == PanelMaterial.LIQUID_GLASS &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ) runCatching { RuntimeShader(GLASS_SHADER) }.getOrNull() else null
        }
        Box(Modifier.matchParentSize().clipToBounds()) {
            backdrop(
                Modifier
                    .requiredSize(
                        with(density) { canvasWidth.toDp() },
                        with(density) { canvasHeight.toDp() }
                    )
                    .offset { IntOffset(-originX.roundToInt(), -originY.roundToInt()) }
                    .graphicsLayer {
                        renderEffect = runCatching {
                            // The explicit version check is what lint needs; `glass` is only
                            // ever built on 33 or above, but that is not something it can see.
                            if (glass != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                glassEffect(
                                    glass, originX, originY, size.width, size.height,
                                    theme.cornerRadius * density.density, canvasWidth, canvasHeight
                                )
                            } else {
                                AndroidRenderEffect.createBlurEffect(26f, 26f, Shader.TileMode.CLAMP)
                            }.asComposeRenderEffect()
                        }.getOrNull()
                    }
            )
        }
    }

    val body = Modifier.matchParentSize()
    when (theme.material) {
        PanelMaterial.MONT -> Box(body.background(Color(theme.background)))
        PanelMaterial.DEFAULT -> Box(
            body
                .background(
                    Brush.linearGradient(
                        listOf(Color(theme.background), Color(theme.background).copy(alpha = .78f))
                    )
                )
                .border(1.dp, Color(theme.stroke), shape)
        )
        // The glass materials are mostly what is behind them; the tint only lifts the text off it.
        PanelMaterial.LIQUID_GLASS, PanelMaterial.FROSTED -> Box(
            body.background(Color(theme.background)).border(1.dp, Color(theme.stroke), shape)
        )
    }
}
