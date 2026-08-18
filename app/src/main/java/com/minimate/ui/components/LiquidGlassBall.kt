package com.minimate.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.ui.theme.AccentBlue
import com.minimate.ui.theme.AccentCyan
import com.minimate.ui.theme.AccentEmerald
import com.minimate.ui.theme.AccentPink
import com.minimate.ui.theme.TextPrimary
import com.minimate.ui.theme.TextSecondary

enum class LiquidMenuAction {
    LOCK,
    AMOLED_MODE,
    SETTINGS,
    THEME_CYCLE
}

/**
 * Liquid Glass UI Element inspired by Kyant0/AndroidLiquidGlass.
 * Refractive glassmorphism with specular rim lighting, viscous spring morphing,
 * and elastic fluid ripple feedback.
 */
@Composable
fun LiquidGlassBall(
    isDimMode: Boolean,
    isLocked: Boolean,
    presetIndex: Int,
    totalPresets: Int,
    onTap: () -> Unit,
    onMenuAction: (LiquidMenuAction) -> Unit,
    onTouchDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLocked) return

    var isExpanded by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    var wobbleTrigger by remember { mutableFloatStateOf(0f) }
    var pressStartTime by remember { mutableStateOf(0L) }

    // Elastic fluid spring physics for liquid morphing
    val morphProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "LiquidMorphProgress"
    )

    val targetWidth = if (isExpanded) 200.dp else 48.dp
    val targetHeight = if (isExpanded) 220.dp else 48.dp
    val cornerRadius = if (isExpanded) 26.dp else 24.dp

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "LiquidWidth"
    )
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "LiquidHeight"
    )

    Box(
        modifier = modifier
            .padding(start = 16.dp, bottom = 16.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        // Liquid Glass Container
        Box(
            modifier = Modifier
                .size(width = animatedWidth, height = animatedHeight)
                .shadow(
                    elevation = if (isExpanded) 16.dp else 8.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    spotColor = AccentCyan.copy(alpha = 0.4f),
                    ambientColor = Color.Black
                )
                .clip(RoundedCornerShape(cornerRadius))
                .pointerInput(isExpanded) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Press -> {
                                    isPressed = true
                                    pressStartTime = System.currentTimeMillis()
                                    onTouchDown()
                                }
                                PointerEventType.Release -> {
                                    val duration = System.currentTimeMillis() - pressStartTime
                                    if (!isExpanded) {
                                        if (duration > 320L) {
                                            // Click and hold -> morph into liquid glass list!
                                            isExpanded = true
                                        } else {
                                            // Tap -> trigger liquid wobble & action
                                            wobbleTrigger += 1f
                                            onTap()
                                        }
                                    }
                                    isPressed = false
                                }
                            }
                        }
                    }
                }
        ) {
            // Liquid Glass Refractive Backdrop Canvas
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val cr = if (isExpanded) 26.dp.toPx() else (size.minDimension / 2f)

                // 1. Ambient Chromatic Dispersion / Refraction Base
                val liquidFillBrush = Brush.linearGradient(
                    colors = if (isDimMode) {
                        listOf(Color(0xE0ECEFF4), Color(0xD0D8DEE9), Color(0xC0E5E9F0))
                    } else {
                        listOf(
                            Color(0xD01E202E),
                            Color(0xDD12131C),
                            Color(0xE60A0B10)
                        )
                    },
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )

                drawRoundRect(
                    brush = liquidFillBrush,
                    size = Size(w, h),
                    cornerRadius = CornerRadius(cr, cr)
                )

                // 2. Specular Top-Left Fresnel Highlight
                val specularBrush = Brush.linearGradient(
                    colors = if (isDimMode) {
                        listOf(Color(0x99FFFFFF), Color(0x33FFFFFF), Color.Transparent)
                    } else {
                        listOf(
                            Color(0x88FFFFFF),
                            AccentCyan.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color(0x18FFFFFF)
                        )
                    },
                    start = Offset(0f, 0f),
                    end = Offset(w * 0.7f, h * 0.7f)
                )

                drawRoundRect(
                    brush = specularBrush,
                    size = Size(w, h),
                    cornerRadius = CornerRadius(cr, cr),
                    style = Stroke(width = if (isPressed) 2.2f else 1.4f)
                )

                // 3. Bottom-Right Chromatic Edge Glint
                val rimBrush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, AccentPink.copy(alpha = 0.35f), Color(0x33FFFFFF)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )

                drawRoundRect(
                    brush = rimBrush,
                    size = Size(w, h),
                    cornerRadius = CornerRadius(cr, cr),
                    style = Stroke(width = 1.0f)
                )
            }

            // Expanded Liquid Glass Menu Items
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header with close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Minimate",
                            color = AccentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .clickable { isExpanded = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("×", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 1. Lock Item
                    LiquidMenuItem(
                        title = "Lock",
                        subtitle = "Hardware Vol unlock",
                        icon = Icons.Default.Lock,
                        tint = Color(0xFFEF4444),
                        onClick = {
                            isExpanded = false
                            onMenuAction(LiquidMenuAction.LOCK)
                        }
                    )

                    // 2. Amoled Mode Item
                    LiquidMenuItem(
                        title = "Amoled Mode",
                        subtitle = if (isDimMode) "Active" else "OLED black",
                        icon = if (isDimMode) Icons.Default.Brightness2 else Icons.Default.DarkMode,
                        tint = if (isDimMode) AccentPink else AccentCyan,
                        onClick = {
                            isExpanded = false
                            onMenuAction(LiquidMenuAction.AMOLED_MODE)
                        }
                    )

                    // 3. Settings Item
                    LiquidMenuItem(
                        title = "Settings",
                        subtitle = "Control center",
                        icon = Icons.Default.Settings,
                        tint = AccentBlue,
                        onClick = {
                            isExpanded = false
                            onMenuAction(LiquidMenuAction.SETTINGS)
                        }
                    )

                    // 4. Theme Cycler Item (Presets 1..5)
                    LiquidMenuItem(
                        title = "Theme",
                        subtitle = "Preset ${presetIndex + 1}/$totalPresets",
                        icon = Icons.Default.Palette,
                        tint = AccentEmerald,
                        badge = "${presetIndex + 1}/$totalPresets",
                        onClick = {
                            onMenuAction(LiquidMenuAction.THEME_CYCLE)
                        }
                    )
                }
            } else {
                // Collapsed Ball Center Icon
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDimMode) Icons.Default.Brightness2 else Icons.Default.Palette,
                        contentDescription = null,
                        tint = if (isDimMode) Color(0xFF1E202E) else Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x401A1C2A))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(modifier = Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 9.sp
            )
        }
        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.25f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    color = tint,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
