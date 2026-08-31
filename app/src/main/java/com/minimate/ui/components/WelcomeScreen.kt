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
import com.minimate.ui.theme.Mont

/** One thing the app needs, and whether it has it yet. */
data class PermissionItem(val label: String, val detail: String, val granted: Boolean)

private val Mustard = Color(0xFFD8A628)

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

    Box(modifier.fillMaxSize().background(Color.Black)) {
        Canvas(Modifier.fillMaxSize()) {
            // Diagonals, drawn past both edges so they never end anywhere you can see, and offset
            // by one full period so the loop has no seam.
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
 * The one thing that is not discoverable.
 *
 * Every other control is visible on the screen it belongs to, but the pill carries three separate
 * gestures and looks like a clock. Shown once, over the trackpad it is describing, so the reader
 * can see the thing being talked about while they read about it.
 */
@Composable
fun PillTourCard(onAcknowledge: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Color.Black.copy(.45f))) {
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 18.dp)
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
            TourLine("ONE TAP", "Change mode")
            TourLine("TWO TAPS", "AMOLED black")
            TourLine("HOLD", "Open settings")

            Spacer(Modifier.height(4.dp))
            Text(
                "OKAY",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAcknowledge)
                    .padding(vertical = 6.dp),
                color = Color.White,
                fontFamily = Mont,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
        }
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
