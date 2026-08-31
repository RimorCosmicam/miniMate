package com.minimate.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import com.minimate.ui.theme.Mont

/** One thing the app needs, and whether it has it yet. */
data class PermissionItem(val label: String, val detail: String, val granted: Boolean)

private val Mustard = Color(0xFFD8A628)

/** The welcome's ground, shared by both of its steps so the second is not a different place. */
@Composable
private fun MustardDiagonals(travel: Float, modifier: Modifier = Modifier) {
    Box(modifier.background(Color.Black)) {
        Canvas(Modifier.fillMaxSize()) {
            // Drawn past both edges so they never end anywhere you can see, and offset by exactly
            // one period so the loop has no seam.
            val spacing = 34.dp.toPx()
            val bandWidth = 15.dp.toPx()
            val drop = travel * spacing
            var y = -size.width - spacing
            while (y < size.height + spacing) {
                drawLine(
                    Mustard,
                    Offset(-size.width, y + drop),
                    Offset(size.width * 2f, y + drop + size.width * 1.5f),
                    bandWidth
                )
                y += spacing
            }
        }
    }
}

/**
 * The first thing anyone sees.
 *
 * Mustard diagonals running down behind a single black card, the name split across the two ends of
 * the Mont range — the lightest weight over the heaviest — and the permissions as a plain list
 * that states what it wants and why. Nothing here is a dialog: the same black surface, the same
 * type and the same all-or-nothing colour as the rest of the app, so the first screen is not the
 * one screen that looks like somebody else built it.
 */
@Composable
fun WelcomeScreen(
    permissions: List<PermissionItem>,
    onGrant: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "welcome")
    val travel by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(5200, easing = LinearEasing)), label = "stripes"
    )
    val everythingGranted = permissions.all { it.granted }

    Box(modifier.fillMaxSize()) {
        MustardDiagonals(travel, Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 18.dp)
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

            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                permissions.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = !item.granted) { onGrant() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.label.uppercase(),
                                color = Color.White.copy(if (item.granted) 1f else .55f),
                                fontFamily = Mont,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                            Text(
                                item.detail,
                                color = Color.White.copy(.42f),
                                fontFamily = Mont,
                                fontWeight = FontWeight.Normal,
                                fontSize = 9.sp
                            )
                        }
                        Text(
                            if (item.granted) "GRANTED" else "ALLOW",
                            color = Color.White.copy(if (item.granted) .55f else 1f),
                            fontFamily = Mont,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Text alone, like every other commitment in this app. It stays dim until there is
            // nothing left to grant, so it reads as the end of the list rather than a way past it.
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
    }
}

/**
 * The switcher, introduced where it can be seen.
 *
 * On the welcome's own ground rather than over the trackpad, with the real thing sitting inside
 * the card next to what it does — three gestures on an object that looks like a clock is the one
 * part of this app nobody could work out by looking at it.
 *
 * On Okay it travels to the corner it will live in rather than cutting there. Watching it go is
 * what tells you where to look for it afterwards; a jump leaves you hunting for it later.
 */
@Composable
fun SwitcherIntro(
    pill: @Composable () -> Unit,
    targetX: Float,
    targetY: Float,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "switcherIntro")
    val travel by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(5200, easing = LinearEasing)), label = "stripes"
    )

    var leaving by remember { mutableStateOf(false) }
    val journey by animateFloatAsState(
        targetValue = if (leaving) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "journey",
        finishedListener = { if (it == 1f) onFinished() }
    )

    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // Where the card holds it, and where it belongs. The pill is drawn once and moved between
        // the two, so what arrives in the corner is the same object that was being explained.
        var pillSize by remember { mutableStateOf(IntSize.Zero) }
        val restX = widthPx * .5f - pillSize.width / 2f
        val restY = heightPx * .34f
        val homeX = (targetX * widthPx - pillSize.width / 2f)
            .coerceIn(8f, (widthPx - pillSize.width - 8f).coerceAtLeast(8f))
        val homeY = (targetY * heightPx - pillSize.height / 2f)
            .coerceIn(8f, (heightPx - pillSize.height - 8f).coerceAtLeast(8f))

        MustardDiagonals(travel, Modifier.fillMaxSize().alpha(1f - journey))

        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 18.dp)
                .alpha(1f - journey)
                .background(Color.Black.copy(MONT_SURFACE_ALPHA))
                .padding(start = 22.dp, top = 20.dp, end = 18.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "THE SWITCHER",
                color = Color.White.copy(.55f),
                fontFamily = Mont,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp
            )
            // Room for the pill, which is drawn above the card so it can leave it later.
            Spacer(Modifier.height(34.dp))
            TourLine("ONE TAP", "Change mode")
            TourLine("TWO TAPS", "AMOLED black")
            TourLine("HOLD", "Open settings")

            Spacer(Modifier.height(4.dp))
            Text(
                "OKAY",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !leaving) { leaving = true }
                    .padding(vertical = 6.dp),
                color = Color.White,
                fontFamily = Mont,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
        }

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
