package com.minimate.ui.shader

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.minimate.touchpad.model.BackgroundTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val VW = 237f
private const val VH = 262f

/** Native logical resolution is exactly one quarter of the Flip7 FlexWindow. */
@Composable
internal fun SceneryThemeCanvas(theme: BackgroundTheme, variant: Int, speed: Float, modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "SceneryClock")
    val raw by transition.animateFloat(0f, 600f, infiniteRepeatable(tween(120_000, easing = LinearEasing)), label = "SceneryTime")
    Canvas(modifier) {
        withTransform({ scale(size.width / VW, size.height / VH, pivot = Offset.Zero) }) {
            val t = raw * speed
            when (theme) {
                BackgroundTheme.SCENERY_COAST -> coast(t, variant)
                BackgroundTheme.SCENERY_ALPINE -> alpine(t, variant)
                BackgroundTheme.SCENERY_RAIN_CITY -> rainCity(t, variant)
                BackgroundTheme.SCENERY_SAKURA -> sakura(t, variant)
                BackgroundTheme.SCENERY_DESERT -> desert(t, variant)
                BackgroundTheme.SCENERY_COSMOS -> cosmos(t, variant)
                else -> Unit
            }
        }
    }
}

private fun DrawScope.coast(t: Float, variant: Int) {
    val sky = listOf(Color(0xFF57CBE6), Color(0xFFF7A15C), Color(0xFF152957))[variant.mod(3)]
    val sea = listOf(Color(0xFF087FA9), Color(0xFF166B99), Color(0xFF102E63))[variant.mod(3)]
    drawRect(sky, size = Size(VW, VH)); drawRect(sea, Offset(0f, 35f), Size(VW, 227f))
    // Deep water texture, aligned to the 4x physical pixel grid.
    repeat(90) { i ->
        val x = ((i * 47 + (t * (1 + i % 3)).toInt()) % 257) - 10f
        val y = 39f + (i * 83 % 205)
        val c = if (i % 5 == 0) Color(0xFF54D9D1) else Color(0xFF1C9DBD)
        drawRect(c.copy(.16f + i % 3 * .07f), Offset(x, y), Size(2f + i % 7, 1f))
    }
    // Reef shelves below the surface.
    repeat(22) { i ->
        val x = 12f + (i * 61 % 211)
        val y = 72f + (i * 37 % 171)
        val r = 3f + i % 7
        drawCircle(Color(0xFF2A746E).copy(.48f), r, Offset(x, y))
        drawCircle(Color(0xFF64B997).copy(.28f), r * .55f, Offset(x - 1f, y - 1f))
    }
    // Curved sand coast on the right.
    val sand = Path().apply {
        moveTo(237f, 18f); cubicTo(184f, 47f, 216f, 104f, 168f, 135f)
        cubicTo(139f, 156f, 189f, 199f, 132f, 262f); lineTo(237f, 262f); close()
    }
    drawPath(sand, if (variant.mod(3) == 2) Color(0xFFBDA985) else Color(0xFFF1D18F))
    // Independent wave fronts move at different speeds.
    repeat(6) { band ->
        val p = Path(); repeat(28) { i ->
            val y = i * 10f
            val coastX = 207f - sin(i * .55f) * 14f - band * 4.3f + sin(t * (.42f + band * .03f) + i) * 2.2f
            if (i == 0) p.moveTo(coastX, y) else p.lineTo(coastX, y)
        }
        drawPath(p, Color.White.copy(.7f - band * .085f), style = Stroke(if (band == 0) 2.2f else 1f))
    }
    // Rocky headland with vegetation.
    repeat(28) { i ->
        val x = 192f + i % 6 * 8f
        val y = 30f + i / 6 * 7f
        drawRect(if (i % 3 == 0) Color(0xFF496C42) else Color(0xFF315539), Offset(x, y), Size(9f, 8f))
        if (i % 4 == 0) drawRect(Color(0xFF7EA35A), Offset(x + 2f, y), Size(3f, 2f))
    }
    // Boats and wakes.
    repeat(4) { i ->
        val x = 26f + i * 39f + sin(t * .12f + i) * 7f
        val y = 64f + i * 43f + cos(t * .1f + i) * 5f
        drawLine(Color.White.copy(.35f), Offset(x - 15f, y + 7f), Offset(x, y), 1f)
        pixelBoat(Offset(x, y), if (i % 2 == 0) Color(0xFFFF665C) else Color(0xFFFFD85D))
    }
    if (variant.mod(3) == 2) stars(t, Color(0xFFFFF3C1), 35, 34f)
}

private fun DrawScope.alpine(t: Float, variant: Int) {
    val sky = listOf(Color(0xFF86C8DC), Color(0xFF263F82), Color(0xFF79869C))[variant.mod(3)]
    drawRect(sky, size = Size(VW, VH))
    if (variant.mod(3) == 1) stars(t, Color(0xFFFFF1A8), 54, 95f)
    mountain(Offset(12f, 122f), 91f, Color(0xFF607CA0), Color(0xFFD7E5E9))
    mountain(Offset(112f, 112f), 118f, Color(0xFF435E82), Color(0xFFE8F0EF))
    mountain(Offset(204f, 129f), 72f, Color(0xFF526D85), Color(0xFFDDE9E8))
    // Forest layers.
    repeat(30) { i -> pine(Offset(i * 9f - 5f, 143f + i % 4 * 3f), 13f + i % 5 * 3f, Color(0xFF264C4B)) }
    repeat(24) { i -> pine(Offset(i * 11f - 3f, 169f + i % 3 * 5f), 18f + i % 4 * 4f, Color(0xFF163B3D)) }
    val lake = if (variant.mod(3) == 1) Color(0xFF173F66) else Color(0xFF287B8B)
    drawRect(lake, Offset(0f, 171f), Size(VW, 91f))
    repeat(45) { i ->
        val y = 177f + i * 17 % 82
        val x = ((i * 53 + (t * (2 + i % 2)).toInt()) % 250) - 8f
        drawRect(Color(0xFF81D7D0).copy(.18f + i % 4 * .05f), Offset(x, y), Size(4f + i % 13, 1f))
    }
    // Cabin, dock, waterfall.
    drawRect(Color(0xFF51372E), Offset(153f, 157f), Size(35f, 24f))
    val roof = Path().apply { moveTo(148f, 158f); lineTo(170f, 142f); lineTo(193f, 158f); close() }
    drawPath(roof, Color(0xFF29313E)); repeat(4) { i -> drawRect(Color(0xFFFFC65C).copy(.72f + abs(sin(t * 2f + i)) * .28f), Offset(158f + i % 2 * 15f, 163f + i / 2 * 9f), Size(6f, 5f)) }
    drawRect(Color(0xFF6B4B35), Offset(124f, 181f), Size(72f, 3f)); repeat(6) { i -> drawRect(Color(0xFF453126), Offset(128f + i * 12f, 181f), Size(2f, 12f)) }
    repeat(3) { i -> drawRect(Color(0xFFBFE5E8).copy(.65f), Offset(218f + i * 3f, 105f), Size(2f, 58f + sin(t + i) * 3f)) }
    if (variant.mod(3) == 2) repeat(70) { i ->
        val x = (i * 71 + (t * 8).toInt()) % 245 - 4f; val y = (i * 47 + (t * (10 + i % 5)).toInt()) % 270 - 4f
        drawRect(Color.White.copy(.72f), Offset(x, y), Size(if (i % 5 == 0) 2f else 1f, 2f))
    }
}

private fun DrawScope.rainCity(t: Float, variant: Int) {
    val sky = listOf(Color(0xFF11162E), Color(0xFF151327), Color(0xFF738795))[variant.mod(3)]
    drawRect(sky, size = Size(VW, VH))
    // Three parallax skylines.
    repeat(18) { i -> building(i * 15f - (t * .08f % 15f), 94f + i % 4 * 8f, 25f + i % 5 * 9f, Color(0xFF28314E), i, t) }
    repeat(14) { i -> building(i * 19f - (t * .15f % 19f), 139f + i % 3 * 7f, 38f + i % 6 * 8f, Color(0xFF171F39), i + 20, t) }
    drawRect(Color(0xFF151923), Offset(0f, 183f), Size(VW, 79f))
    // Elevated train actually crosses the scene.
    drawRect(Color(0xFF394052), Offset(0f, 165f), Size(VW, 4f)); repeat(8) { i -> drawRect(Color(0xFF242A37), Offset(i * 34f, 169f), Size(3f, 28f)) }
    val trainX = (t * 12f % 320f) - 72f
    repeat(4) { car ->
        drawRect(Color(0xFFB7BEC8), Offset(trainX + car * 24f, 153f), Size(22f, 11f))
        repeat(3) { w -> drawRect(Color(0xFF68D7E4), Offset(trainX + car * 24f + 3f + w * 6f, 156f), Size(4f, 4f)) }
    }
    // Wet road and colored reflection stacks.
    repeat(12) { i ->
        val c = listOf(Color(0xFFE945A6), Color(0xFF2DD9EA), Color(0xFFF8BC4C))[i % 3]
        val x = 8f + i * 21f
        val h = 12f + (i * 17 % 43)
        drawRect(c.copy(.18f), Offset(x, 201f), Size(3f, h)); drawRect(c.copy(.1f), Offset(x - 3f, 208f), Size(9f, h * .55f))
    }
    // Rain in three speed/depth bands and splash pixels.
    repeat(95) { i ->
        val depth = 1f + i % 3
        val x = (i * 73 + (t * depth * 18).toInt()) % 250 - 6f
        val y = (i * 41 + (t * depth * 34).toInt()) % 278 - 8f
        drawLine(Color(0xFFB4DDF0).copy(.18f + depth * .13f), Offset(x, y), Offset(x - depth, y + 3f + depth * 2f), depth * .45f)
        if (y > 226f && i % 6 == 0) drawLine(Color(0xFFB4DDF0).copy(.4f), Offset(x - 2f, y), Offset(x + 2f, y), .7f)
    }
}

private fun DrawScope.sakura(t: Float, variant: Int) {
    val sky = listOf(Color(0xFF9BD3E0), Color(0xFF4D416C), Color(0xFFD4A9B9))[variant.mod(3)]
    drawRect(sky, size = Size(VW, VH))
    mountain(Offset(16f, 124f), 102f, Color(0xFF718E91), Color(0xFFC7D7CF)); mountain(Offset(142f, 137f), 72f, Color(0xFF607D78), Color(0xFFBFD2C8))
    // Terraced valley and river.
    repeat(8) { i ->
        val y = 126f + i * 13f
        drawPath(Path().apply { moveTo(0f, y); cubicTo(61f, y - 14f, 151f, y + 17f, 237f, y - 8f) }, Color(0xFF4D7656).copy(.72f - i * .035f), style = Stroke(10f))
    }
    val river = Path().apply { moveTo(93f, 112f); cubicTo(167f, 148f, 56f, 190f, 145f, 262f); lineTo(194f, 262f); cubicTo(91f, 188f, 194f, 150f, 112f, 110f); close() }
    drawPath(river, Color(0xFF69B6C4)); repeat(13) { i -> drawRect(Color.White.copy(.2f), Offset(89f + i * 8f + sin(i.toFloat()) * 23f, 128f + i * 10f), Size(12f, 1f)) }
    // Shrine and lantern path.
    drawRect(Color(0xFFB63E42), Offset(168f, 109f), Size(4f, 35f)); drawRect(Color(0xFFB63E42), Offset(203f, 109f), Size(4f, 35f)); drawRect(Color(0xFF8E2D35), Offset(162f, 108f), Size(51f, 5f)); drawRect(Color(0xFF8E2D35), Offset(169f, 119f), Size(36f, 4f))
    repeat(10) { i ->
        val x = 145f - i * 11f; val y = 154f + i * 10f
        drawRect(Color(0xFF423B35), Offset(x, y), Size(1f, 7f)); drawRect(Color(0xFFFFC65F).copy(.7f + abs(sin(t * 1.8f + i)) * .3f), Offset(x - 2f, y), Size(5f, 4f))
    }
    // Foreground cherry trees and petals offset from finger-safe center.
    repeat(3) { tree ->
        val x = 24f + tree * 94f; drawLine(Color(0xFF533E3E), Offset(x, 230f), Offset(x + 11f, 138f), 6f)
        repeat(21) { i -> drawCircle(if (i % 4 == 0) Color(0xFFFFE4EC) else Color(0xFFF29AB7), 3f + i % 3, Offset(x - 18f + i * 7 % 43, 137f + i * 11 % 52)) }
    }
    val petalCount = if (variant.mod(3) == 2) 90 else 36
    repeat(petalCount) { i ->
        val x = (i * 59 + (t * (5 + i % 4)).toInt()) % 248 - 5f; val y = (i * 31 + (t * (9 + i % 5)).toInt()) % 272 - 5f
        drawRect(if (i % 3 == 0) Color.White else Color(0xFFFFB0C7), Offset(x, y), Size(2f, 1f))
    }
}

private fun DrawScope.desert(t: Float, variant: Int) {
    val sky = listOf(Color(0xFF69A6C4), Color(0xFF6A3C76), Color(0xFF151B3D))[variant.mod(3)]
    drawRect(sky, size = Size(VW, VH))
    if (variant.mod(3) == 2) stars(t, Color(0xFFFFEBC0), 85, 154f)
    if (variant.mod(3) != 2) drawCircle(if (variant.mod(3) == 0) Color(0xFFFFD36B) else Color(0xFFFF8469), 23f, Offset(179f, 61f))
    // Five dune layers with moving wind texture.
    val duneColors = listOf(Color(0xFFB76A48), Color(0xFFD88A52), Color(0xFFE8AC65), Color(0xFFF0C67B), Color(0xFFD79A5B))
    repeat(5) { layer ->
        val base = 112f + layer * 31f
        val p = Path().apply { moveTo(0f, VH); lineTo(0f, base); cubicTo(52f, base - 30f + layer * 3f, 122f, base + 24f, 237f, base - 18f); lineTo(VW, VH); close() }
        drawPath(p, duneColors[layer])
        repeat(11) { i -> drawLine(Color.White.copy(.08f), Offset(i * 25f - (t * .15f % 25f), base + i % 3 * 8f), Offset(i * 25f + 14f, base + i % 3 * 8f - 3f), 1f) }
    }
    // Monument and caravan.
    val temple = Path().apply { moveTo(150f, 161f); lineTo(178f, 94f); lineTo(207f, 161f); close() }
    drawPath(temple, Color(0xFF8C563C)); repeat(5) { i -> drawRect(Color(0xFF583A35), Offset(161f + i * 8f, 143f), Size(4f, 18f)) }
    repeat(8) { i ->
        val x = ((31f + i * 23f + t * 1.4f) % 260f) - 10f; val y = 218f + sin(i * 1.8f) * 5f
        drawCircle(Color(0xFF493934), 3f, Offset(x, y)); drawLine(Color(0xFF493934), Offset(x - 2f, y + 2f), Offset(x - 4f, y + 8f), 1f); drawLine(Color(0xFF493934), Offset(x + 2f, y + 2f), Offset(x + 4f, y + 8f), 1f)
        if (variant.mod(3) == 2) drawCircle(Color(0xFFFFB94F).copy(.75f), 2f, Offset(x, y - 5f))
    }
}

private fun DrawScope.cosmos(t: Float, variant: Int) {
    val sky = listOf(Color(0xFF18285A), Color(0xFF281947), Color(0xFF351A38))[variant.mod(3)]
    drawRect(sky, size = Size(VW, VH)); stars(t, Color(0xFFEAF6FF), 110, 180f)
    val planet = listOf(Color(0xFFCE7858), Color(0xFF6E75C8), Color(0xFFB65A62))[variant.mod(3)]
    drawCircle(planet, 59f, Offset(187f, 50f)); repeat(9) { i -> drawArc(Color.White.copy(.05f + i * .018f), 170f, 180f, false, Offset(129f, -8f + i * 4f), Size(116f, 116f), style = Stroke(2f)) }
    if (variant.mod(3) == 0) drawOval(Color(0xFFDDC77A).copy(.6f), Offset(110f, 37f), Size(153f, 29f), style = Stroke(5f))
    // Alien ridges and crystalline foreground.
    repeat(4) { layer ->
        val y = 137f + layer * 27f
        val p = Path().apply { moveTo(0f, VH); lineTo(0f, y); repeat(13) { i -> lineTo(i * 20f, y - (i * 17 + layer * 13) % 31f) }; lineTo(VW, VH); close() }
        drawPath(p, listOf(Color(0xFF324571), Color(0xFF29375E), Color(0xFF202B4C), Color(0xFF171F39))[layer])
    }
    repeat(17) { i ->
        val x = 4f + i * 15f; val h = 9f + i * 19 % 48
        val crystal = Path().apply { moveTo(x, 254f); lineTo(x + 5f, 254f - h); lineTo(x + 10f, 254f); close() }
        drawPath(crystal, listOf(Color(0xFF4AE0D1), Color(0xFF9C73F2), Color(0xFFE468C2))[variant.mod(3)].copy(.68f))
    }
    // Outpost and moving shuttle.
    drawRect(Color(0xFFB8C4CF), Offset(151f, 183f), Size(46f, 17f)); drawCircle(Color(0xFF76D6E5), 12f, Offset(161f, 183f)); repeat(4) { i -> drawRect(Color(0xFFFFD057).copy(.65f + abs(sin(t + i)) * .35f), Offset(176f + i * 5f, 188f), Size(3f, 3f)) }
    val sx = (t * 5f % 290f) - 30f; val sy = 103f + sin(t * .4f) * 13f
    drawPath(Path().apply { moveTo(sx + 13f, sy); lineTo(sx, sy - 4f); lineTo(sx - 8f, sy); lineTo(sx, sy + 4f); close() }, Color(0xFFD7EEF2)); drawLine(Color(0xFF5AE3FF).copy(.45f), Offset(sx - 8f, sy), Offset(sx - 23f, sy), 2f)
}

private fun DrawScope.stars(t: Float, color: Color, count: Int, maxY: Float) {
    repeat(count) { i ->
        val x = (i * 83 % 237).toFloat(); val y = (i * 47 % maxY.toInt().coerceAtLeast(1)).toFloat()
        val a = .28f + abs(sin(t * (.6f + i % 5 * .13f) + i)) * .72f
        drawRect(color.copy(a), Offset(x, y), Size(if (i % 17 == 0) 2f else 1f, if (i % 17 == 0) 2f else 1f))
    }
}

private fun DrawScope.mountain(base: Offset, width: Float, color: Color, snow: Color) {
    val p = Path().apply { moveTo(base.x - width, base.y); lineTo(base.x, base.y - width * .78f); lineTo(base.x + width, base.y); close() }; drawPath(p, color)
    val cap = Path().apply { moveTo(base.x - width * .27f, base.y - width * .57f); lineTo(base.x, base.y - width * .78f); lineTo(base.x + width * .3f, base.y - width * .55f); lineTo(base.x + width * .12f, base.y - width * .6f); lineTo(base.x, base.y - width * .51f); lineTo(base.x - width * .12f, base.y - width * .62f); close() }; drawPath(cap, snow)
}

private fun DrawScope.pine(o: Offset, h: Float, color: Color) {
    drawRect(Color(0xFF4A3930), Offset(o.x - 1f, o.y - h * .15f), Size(2f, h * .4f)); repeat(4) { i ->
        val y = o.y - h + i * h * .2f; val w = h * (.2f + i * .09f)
        drawPath(Path().apply { moveTo(o.x, y); lineTo(o.x - w, y + h * .42f); lineTo(o.x + w, y + h * .42f); close() }, color)
    }
}

private fun DrawScope.building(x: Float, ground: Float, h: Float, color: Color, seed: Int, t: Float) {
    drawRect(color, Offset(x, ground - h), Size(13f, h)); repeat((h / 8f).toInt()) { row -> repeat(2) { col ->
        val lit = (seed * 7 + row * 5 + col * 3) % 8 < 3
        drawRect(if (lit) Color(0xFFFFC968).copy(.65f + abs(sin(t * .3f + seed)) * .25f) else Color(0xFF25304B), Offset(x + 3f + col * 6f, ground - h + 4f + row * 8f), Size(2f, 3f))
    } }
}

private fun DrawScope.pixelBoat(o: Offset, color: Color) {
    drawRect(Color.White, Offset(o.x - 5f, o.y - 1f), Size(10f, 3f)); drawRect(color, Offset(o.x - 3f, o.y + 2f), Size(6f, 2f)); drawRect(Color(0xFF5A4939), Offset(o.x, o.y - 8f), Size(1f, 8f)); drawPath(Path().apply { moveTo(o.x + 1f, o.y - 7f); lineTo(o.x + 7f, o.y - 1f); lineTo(o.x + 1f, o.y - 1f); close() }, Color(0xFFF4E8C9))
}
