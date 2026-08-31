package com.minimate.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.touchpad.model.HostPlatform
import com.minimate.ui.theme.Mont
import kotlin.math.roundToInt

/** One thing the app needs, and whether it has it yet. */
data class PermissionItem(val label: String, val detail: String, val granted: Boolean)

private val Mustard = Color(0xFFD8A628)

/**
 * The welcome's ground: interleaved diagonal bands, mustard and black.
 *
 * [split] sends the whole ground away along the diagonal the bands are drawn on — both colours
 * together, in the direction they already point, thinning as they run until nothing is left. The
 * ground is drawn on nothing rather than over a black fill, so what shows through the widening
 * gaps is whatever is behind it. [invert] exchanges the two colours.
 */
@Composable
private fun MustardDiagonals(
    travel: Float,
    split: Float,
    invert: Float = 0f,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val spacing = 34.dp.toPx()
        val band = spacing * 0.5f
        val drift = travel * spacing

        // The bands run down and to the right, so that is the way they leave: one direction, both
        // colours, along the line they were already drawn on. The stroke thins as they run, which
        // is what actually opens the ground up — a sheet of parallel lines sliding along its own
        // axis looks perfectly still no matter how fast it is going.
        val run = split * (size.width + size.height)
        val away = Offset(run * 0.894f, run * 0.447f)
        val stroke = band * (1f - split).coerceIn(0f, 1f)

        if (stroke <= 0f) return@Canvas

        fun bands(color: Color, offset: Float) {
            var y = -size.width - spacing * 2f
            while (y < size.height + size.width + spacing * 2f) {
                drawLine(
                    color,
                    Offset(-size.width + away.x, y + offset + away.y),
                    Offset(size.width * 2f + away.x, y + offset + size.width * 1.5f + away.y),
                    stroke
                )
                y += spacing
            }
        }

        bands(lerp(Mustard, Color.Black, invert), drift)
        bands(lerp(Color.Black, Mustard, invert), drift + band)
    }
}

/**
 * The first run, as one card.
 *
 * Permissions, then the switcher, then the ground parting to reveal the app already running behind
 * it. The card is a single surface throughout: it changes what it holds and resizes to fit, rather
 * than one screen vanishing and another taking its place — the box is the thing being followed, so
 * it should never be the thing that blinks.
 */
@Composable
fun Welcome(
    permissions: List<PermissionItem>,
    platform: HostPlatform,
    onPlatform: (HostPlatform) -> Unit,
    onGrant: () -> Unit,
    pill: @Composable () -> Unit,
    targetX: Float,
    targetY: Float,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "welcome")
    val travel by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(5200, easing = LinearEasing)), label = "stripes"
    )

    var showingSwitcher by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }

    // Windows turns the ground inside out. It says nothing about Windows; it is the most obvious
    // thing the screen can do to acknowledge that the answer changed.
    val inverted by animateFloatAsState(
        targetValue = if (platform == HostPlatform.WINDOWS) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "platformInvert"
    )
    val journey by animateFloatAsState(
        targetValue = if (leaving) 1f else 0f,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "journey",
        finishedListener = { if (it == 1f) onFinished() }
    )

    // The gap's position has to be resolved against this box, not against whichever nested column
    // happens to contain it — positionInParent() answered relative to the card's inner content, so
    // the switcher came out near the origin and hung off the top-left corner.
    var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    BoxWithConstraints(modifier.fillMaxSize().onGloballyPositioned { rootCoords = it }) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // Where the card is holding the switcher, and where it belongs afterwards. The rest
        // position is measured off the gap the card leaves for it, so the two never disagree.
        var pillSize by remember { mutableStateOf(IntSize.Zero) }
        var berthTopLeft by remember { mutableStateOf(Offset.Zero) }
        var berthSize by remember { mutableStateOf(IntSize.Zero) }
        // Centred in the gap, and recomputed if either measurement lands later than the other.
        val restX = berthTopLeft.x + (berthSize.width - pillSize.width) / 2f
        val restY = berthTopLeft.y + (berthSize.height - pillSize.height) / 2f
        val homeX = (targetX * widthPx - pillSize.width / 2f)
            .coerceIn(8f, (widthPx - pillSize.width - 8f).coerceAtLeast(8f))
        val homeY = (targetY * heightPx - pillSize.height / 2f)
            .coerceIn(8f, (heightPx - pillSize.height - 8f).coerceAtLeast(8f))

        MustardDiagonals(travel, journey, inverted, modifier = Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 18.dp)
                .alpha(1f - (journey / 0.30f).coerceAtMost(1f))
                .background(Color.Black.copy(MONT_SURFACE_ALPHA))
                .padding(start = 22.dp, top = 22.dp, end = 18.dp, bottom = 16.dp)
        ) {
            Text(
                "mini",
                color = Color.White,
                fontFamily = Mont,
                fontWeight = FontWeight.Thin,
                fontSize = 40.sp
            )
            Text(
                "Mate",
                color = Color.White,
                fontFamily = Mont,
                fontWeight = FontWeight.Black,
                fontSize = 40.sp
            )

            Spacer(Modifier.height(14.dp))

            // The card keeps its place and its heading; only what sits under them is exchanged,
            // and the box grows or shrinks to whatever that needs.
            AnimatedContent(
                targetState = showingSwitcher,
                transitionSpec = {
                    (fadeIn(tween(240, delayMillis = 120)) togetherWith fadeOut(tween(140)))
                        .using(SizeTransform(clip = false) { _, _ -> tween(360, easing = FastOutSlowInEasing) })
                },
                label = "welcomeStep"
            ) { switcher ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (switcher) {
                        SwitcherStep(
                            onBerth = { child ->
                                rootCoords?.let { root ->
                                    berthTopLeft = root.localPositionOf(child, Offset.Zero)
                                    berthSize = child.size
                                }
                            },
                            onOkay = { leaving = true }
                        )
                    } else {
                        PermissionsStep(
                            permissions = permissions,
                            platform = platform,
                            onPlatform = onPlatform,
                            onGrant = onGrant,
                            onDone = { showingSwitcher = true }
                        )
                    }
                }
            }
        }

        // Drawn above the card so it can leave it, but resting in the gap the card set aside.
        // Held back until that gap has been measured, so it never appears at the origin first.
        if (showingSwitcher && berthSize.width > 0) {
            Box(
                Modifier
                    .offset {
                        IntOffset(
                            (restX + (homeX - restX) * journey).roundToInt(),
                            (restY + (homeY - restY) * journey).roundToInt()
                        )
                    }
                    .onSizeChanged { pillSize = it }
            ) { pill() }
        }
    }
}

@Composable
private fun ColumnScope.PermissionsStep(
    permissions: List<PermissionItem>,
    platform: HostPlatform,
    onPlatform: (HostPlatform) -> Unit,
    onGrant: () -> Unit,
    onDone: () -> Unit
) {
    val everythingGranted = permissions.all { it.granted }

    permissions.forEach { item ->
        Row(
            Modifier.fillMaxWidth().clickable(enabled = !item.granted) { onGrant() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Label(item.label.uppercase(), if (item.granted) 1f else .55f, 12)
                Detail(item.detail)
            }
            Label(if (item.granted) "GRANTED" else "ALLOW", if (item.granted) .55f else 1f, 11)
        }
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Label("SENDS TO", 1f, 12)
            Detail("Names the modifier keys. Both send the same codes.")
        }
        HostPlatform.entries.forEach { option ->
            Text(
                option.label.uppercase(),
                modifier = Modifier
                    .clickable { onPlatform(option) }
                    .padding(start = 10.dp, top = 4.dp, bottom = 4.dp),
                color = Color.White.copy(if (option == platform) 1f else .38f),
                fontFamily = Mont,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp
            )
        }
    }

    // Text alone, like every other commitment in this app. Dim until there is nothing left to
    // grant, so it reads as the end of the list rather than a way past it.
    Text(
        "ALL DONE",
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = everythingGranted, onClick = onDone)
            .padding(vertical = 6.dp),
        color = Color.White.copy(if (everythingGranted) 1f else .30f),
        fontFamily = Mont,
        fontWeight = FontWeight.Black,
        fontSize = 15.sp
    )
}

@Composable
private fun ColumnScope.SwitcherStep(onBerth: (LayoutCoordinates) -> Unit, onOkay: () -> Unit) {
    Label("THE SWITCHER", .55f, 11)
    // The gap the switcher sits in. Reporting where it landed is what lets the pill be drawn
    // above the card and still appear to be inside it.
    Spacer(
        Modifier
            .height(36.dp)
            .onGloballyPositioned { onBerth(it) }
    )
    TourLine("ONE TAP", "Change mode")
    TourLine("TWO TAPS", "AMOLED black")
    TourLine("HOLD", "Open settings")
    Spacer(Modifier.height(4.dp))
    Text(
        "OKAY",
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOkay).padding(vertical = 6.dp),
        color = Color.White,
        fontFamily = Mont,
        fontWeight = FontWeight.Black,
        fontSize = 15.sp
    )
}

@Composable
private fun TourLine(gesture: String, meaning: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            gesture,
            modifier = Modifier.weight(.42f),
            color = Color.White,
            fontFamily = Mont,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp
        )
        Text(
            meaning,
            modifier = Modifier.weight(.58f),
            color = Color.White.copy(.62f),
            fontFamily = Mont,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun Label(text: String, alpha: Float, size: Int) {
    Text(
        text,
        color = Color.White.copy(alpha),
        fontFamily = Mont,
        fontWeight = FontWeight.Black,
        fontSize = size.sp
    )
}

@Composable
private fun Detail(text: String) {
    Text(
        text,
        color = Color.White.copy(.42f),
        fontFamily = Mont,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp
    )
}
