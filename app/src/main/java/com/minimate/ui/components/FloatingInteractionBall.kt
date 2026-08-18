package com.minimate.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.ui.theme.AccentBlue
import com.minimate.ui.theme.AccentCyan
import com.minimate.ui.theme.AccentPurple
import com.minimate.ui.theme.BallDim
import com.minimate.ui.theme.BallNormal
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

enum class RadialAction {
    NONE,
    PAIRING,
    SETTINGS,
    THEMES,
    LOCK
}

data class PetalConfig(
    val action: RadialAction,
    val label: String,
    val icon: ImageVector,
    val angleDeg: Float, // Negative angles: fan upwards & right from bottom-left corner
    val color: Color
)

@Composable
fun FloatingInteractionBall(
    isDimMode: Boolean,
    isLocked: Boolean,
    onTapShortcut: () -> Unit,
    onActionSelected: (RadialAction) -> Unit,
    onTouchDown: () -> Unit,
    onHapticTick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLocked) return // Hides ball completely in Lock mode

    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var activeAction by remember { mutableStateOf(RadialAction.NONE) }
    val density = LocalDensity.current

    // The 4 essential petals fanning perfectly into the screen viewable area
    val petals = remember {
        listOf(
            PetalConfig(RadialAction.PAIRING, "Pair", Icons.Default.Bluetooth, -15f, AccentBlue),
            PetalConfig(RadialAction.SETTINGS, "Settings", Icons.Default.Settings, -40f, AccentCyan),
            PetalConfig(RadialAction.THEMES, "Themes", Icons.Default.Palette, -65f, AccentPurple),
            PetalConfig(RadialAction.LOCK, "Lock", Icons.Default.Lock, -90f, Color(0xFFEF4444))
        )
    }

    val ballSize by animateDpAsState(
        targetValue = if (isDimMode || isDragging) 38.dp else 48.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "BallSize"
    )

    val ballColor by animateColorAsState(
        targetValue = if (isDimMode) BallDim else BallNormal,
        animationSpec = tween(durationMillis = 200),
        label = "BallColor"
    )

    Box(
        modifier = modifier
            .padding(start = 18.dp, bottom = 18.dp)
            .size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        // Radial Action Petals Fan Arc (Active while holding & dragging)
        if (isDragging) {
            val radiusPx = with(density) { 68.dp.toPx() } // Compact radius so it NEVER clips
            petals.forEach { petal ->
                val angleRad = Math.toRadians(petal.angleDeg.toDouble())
                val targetX = (cos(angleRad) * radiusPx).toFloat()
                val targetY = (sin(angleRad) * radiusPx).toFloat()

                val isSelected = activeAction == petal.action

                val petalScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.22f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.7f),
                    label = "PetalScale"
                )

                Box(
                    modifier = Modifier
                        .offset { IntOffset(targetX.roundToInt(), targetY.roundToInt()) }
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) petal.color else Color(0xEE161722))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = petal.icon,
                            contentDescription = petal.label,
                            tint = if (isSelected) Color.White else petal.color,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = petal.label,
                            color = if (isSelected) Color.White else Color(0xFF9E9EA7),
                            fontSize = 8.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // The Floating Sphere
        Canvas(
            modifier = Modifier
                .size(ballSize)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            dragOffset = Offset.Zero
                            activeAction = RadialAction.NONE
                            onTouchDown() // Immediate physical haptic feedback on touch!
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount

                            val dist = hypot(dragOffset.x, dragOffset.y)
                            val triggerDist = with(density) { 32.dp.toPx() }

                            if (dist > triggerDist) {
                                val angleRad = atan2(dragOffset.y.toDouble(), dragOffset.x.toDouble())
                                val angleDeg = Math.toDegrees(angleRad).toFloat()

                                // Select closest petal
                                val closest = petals.minByOrNull { petal ->
                                    var diff = Math.abs(petal.angleDeg - angleDeg)
                                    if (diff > 180) diff = 360 - diff
                                    diff
                                }

                                val newAction = closest?.action ?: RadialAction.NONE
                                if (newAction != activeAction && newAction != RadialAction.NONE) {
                                    activeAction = newAction
                                    onHapticTick()
                                }
                            } else {
                                if (activeAction != RadialAction.NONE) {
                                    activeAction = RadialAction.NONE
                                    onHapticTick()
                                }
                            }
                        },
                        onDragEnd = {
                            if (activeAction != RadialAction.NONE) {
                                onActionSelected(activeAction)
                            } else {
                                // Tap / press without dragging -> triggers user's configured shortcut
                                onTapShortcut()
                            }
                            isDragging = false
                            dragOffset = Offset.Zero
                            activeAction = RadialAction.NONE
                        },
                        onDragCancel = {
                            isDragging = false
                            dragOffset = Offset.Zero
                            activeAction = RadialAction.NONE
                        }
                    )
                }
        ) {
            val radius = size.minDimension / 2f
            val centerOffset = Offset(size.width / 2f, size.height / 2f)

            // Tactile shadow
            drawCircle(
                color = Color(0x77000000),
                radius = radius + 3f,
                center = centerOffset + Offset(0f, 4f)
            )

            // Sphere base gradient
            val fillBrush = if (isDimMode) {
                Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFE2E4E9), Color(0xFF9E9EA7)),
                    center = centerOffset - Offset(radius * 0.3f, radius * 0.3f),
                    radius = radius * 1.4f
                )
            } else {
                Brush.radialGradient(
                    colors = listOf(Color(0xFF2C2D35), Color(0xFF16171E), Color(0xFF090A0D)),
                    center = centerOffset - Offset(radius * 0.35f, radius * 0.35f),
                    radius = radius * 1.5f
                )
            }

            drawCircle(brush = fillBrush, radius = radius, center = centerOffset)

            // Metallic rim highlight
            drawCircle(
                color = if (isDimMode) Color(0x40000000) else Color(0x2EFFFFFF),
                radius = radius - 0.5f,
                center = centerOffset,
                style = Stroke(width = 1.2f)
            )
        }
    }
}
