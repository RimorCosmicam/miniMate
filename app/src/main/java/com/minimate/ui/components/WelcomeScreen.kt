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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.touchpad.model.HostPlatform
import com.minimate.ui.theme.Mont

/** One thing the app needs, and whether it has it yet. */
data class PermissionItem(val label: String, val detail: String, val granted: Boolean)

private val Mustard = Color(0xFFD8A628)

/**
 * The welcome's ground: interleaved diagonal bands, mustard and black.
 *
 * Everything is drawn in a frame turned to the bands' own slope, so within it they are simply
 * horizontal rows and the whole thing is easy to reason about. Rows scroll for as long as the
 * welcome is up. [split] then cuts the sheet down the middle and pulls the two halves apart along
 * the bands' own axis — each side leaving the way it points, both colours going with it. The
 * ground is drawn on nothing rather than over a black fill, so what opens up between the halves is
 * whatever is behind it. [invert] exchanges the two colours.
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
        val middle = size.width / 2f
        // Each half is exactly as wide as it travels, so pulling it by its own width clears the
        // side it was covering. The previous version drew a sheet three times wider than the pull,
        // which meant it slid without ever leaving — the ground looked like it simply vanished at
        // the end, because that was the composable being removed rather than anything moving.
        val span = size.width + size.height
        val pull = split * span

        fun half(from: Float, shift: Float) {
            translate(left = shift) {
                var y = -span
                while (y < size.height + span) {
                    drawRect(
                        lerp(Mustard, Color.Black, invert),
                        Offset(from, y + drift),
                        Size(span, band)
                    )
                    drawRect(
                        lerp(Color.Black, Mustard, invert),
                        Offset(from, y + drift + band),
                        Size(span, band)
                    )
                    y += spacing
                }
            }
        }

        rotate(degrees = 26.565f) {
            clipRect(-span, -span, middle, size.height + span) { half(middle - span, -pull) }
            clipRect(middle, -span, size.width + span, size.height + span) { half(middle, pull) }
        }
    }
}

/**
 * A stand-in for the switcher.
 *
 * Drawn rather than borrowed. The real one was being lifted out of the card and flown to its
 * corner, which meant measuring a gap through three layers of layout to find out where it was
 * standing — and when that measurement came back wrong the switcher simply did not appear. The
 * app's own switcher is already in place behind all of this, so the card only ever needed to show
 * what one looks like.
 */
@Composable
private fun FakePill() {
    Row(
        Modifier
            .background(Color.Black.copy(MONT_SURFACE_ALPHA))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "9:41",
            color = Color.White.copy(.92f),
            fontFamily = Mont,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp
        )
        Spacer(Modifier.width(9.dp))
        Text(
            "84%",
            color = Color(0xFF34D399),
            fontFamily = Mont,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp
        )
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(5.dp).background(Color(0xFF22D3EE)))
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
    // Quick. The app behind this is already up and running, so the transition is the only thing
    // standing between the reader and it.
    val journey by animateFloatAsState(
        targetValue = if (leaving) 1f else 0f,
        animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing),
        label = "journey",
        finishedListener = { if (it == 1f) onFinished() }
    )

    Box(modifier.fillMaxSize()) {
        MustardDiagonals(travel, journey, inverted, modifier = Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 18.dp)
                // The card goes first, and faster than the ground it is standing on.
                .alpha(1f - (journey / 0.22f).coerceAtMost(1f))
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
                        SwitcherStep(onOkay = { leaving = true })
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
private fun ColumnScope.SwitcherStep(onOkay: () -> Unit) {
    Label("THE SWITCHER", .55f, 11)
    FakePill()
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
