package com.minimate.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.ui.theme.AccentBlue
import com.minimate.ui.theme.AccentCyan
import com.minimate.ui.theme.AccentPink
import com.minimate.ui.theme.TextPrimary
import com.minimate.ui.theme.TextSecondary
import kotlin.math.roundToInt

enum class LiquidMenuAction {
    LOCK,
    AMOLED_MODE,
    SETTINGS
}

/**
 * Pure Crystal Liquid Glass Ball inspired by Kyant0/AndroidLiquidGlass.
 * 100% pristine translucent glass with smooth specular rim lighting,
 * zero internal clutter/dots/icons in collapsed state, and hold-and-drag menu selection.
 */
@Composable
fun LiquidGlassBall(
    isDimMode: Boolean,
    isLocked: Boolean,
    showAmoledInMenu: Boolean,
    ballSizeDp: Float,
    positionXFraction: Float,
    positionYFraction: Float,
    screenWidthPx: Float,
    screenHeightPx: Float,
    onSingleClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onHoldAction: () -> Unit,
    onMenuAction: (LiquidMenuAction) -> Unit,
    onTouchDown: () -> Unit,
    onHoverItemChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLocked) return

    var isExpanded by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    var highlightedIndex by remember { mutableIntStateOf(-1) }

    // Dynamic menu item bounds for hold-and-drag collision detection
    val itemBounds = remember { mutableMapOf<Int, androidx.compose.ui.geometry.Rect>() }

    // Items present in menu
    val menuItems = remember(showAmoledInMenu, isDimMode) {
        mutableListOf<Pair<LiquidMenuAction, Triple<String, String, ImageVector>>>().apply {
            add(LiquidMenuAction.LOCK to Triple("Lock Trackpad", "Hardware Vol unlock", Icons.Default.Lock))
            if (showAmoledInMenu) {
                add(LiquidMenuAction.AMOLED_MODE to Triple("Amoled Mode", if (isDimMode) "Active" else "OLED black", if (isDimMode) Icons.Default.Brightness2 else Icons.Default.DarkMode))
            }
            add(LiquidMenuAction.SETTINGS to Triple("Settings", "Control center", Icons.Default.Settings))
        }
    }

    // Elastic fluid spring physics for liquid morphing
    val morphProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "LiquidMorphProgress"
    )

    val targetWidth = if (isExpanded) 205.dp else ballSizeDp.dp
    val targetHeight = if (isExpanded) (menuItems.size * 46 + 42).dp else ballSizeDp.dp
    val cornerRadius = if (isExpanded) 22.dp else (ballSizeDp / 2f).dp

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "LiquidWidth"
    )
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "LiquidHeight"
    )

    // Calculate pixel coordinates based on fractions
    val ballPx = ballSizeDp * 2.7f
    val posX = (positionXFraction * screenWidthPx - (if (isExpanded) 102f * 2.7f else ballPx / 2f))
        .coerceIn(10f, screenWidthPx - (if (isExpanded) 205f * 2.7f else ballPx) - 10f)
    val posY = (positionYFraction * screenHeightPx - (if (isExpanded) (menuItems.size * 46f + 42f) * 2.7f else ballPx / 2f))
        .coerceIn(10f, screenHeightPx - (if (isExpanded) (menuItems.size * 46f + 42f) * 2.7f else ballPx) - 10f)

    Box(
        modifier = modifier
            .offset { IntOffset(posX.roundToInt(), posY.roundToInt()) }
    ) {
        // Pristine Liquid Glass Container
        Box(
            modifier = Modifier
                .size(width = animatedWidth, height = animatedHeight)
                .shadow(
                    elevation = if (isExpanded) 20.dp else 12.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    spotColor = AccentCyan.copy(alpha = 0.45f),
                    ambientColor = Color.Black
                )
                .clip(RoundedCornerShape(cornerRadius))
                // Tap Gestures (Single Tap vs Double Tap vs Long Press)
                .pointerInput(isExpanded) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            onTouchDown()
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = {
                            if (!isExpanded) {
                                onSingleClick()
                            }
                        },
                        onDoubleTap = {
                            if (!isExpanded) {
                                onDoubleClick()
                            }
                        },
                        onLongPress = {
                            if (!isExpanded) {
                                isExpanded = true
                                onTouchDown()
                                onHoldAction()
                            }
                        }
                    )
                }
                // Drag Gestures for Hold-and-Slide Selection
                .pointerInput(isExpanded) {
                    if (isExpanded) {
                        detectDragGestures(
                            onDragStart = {
                                isPressed = true
                            },
                            onDrag = { change, _ ->
                                val currentPos = change.position
                                var foundHover = -1
                                itemBounds.forEach { (idx, bounds) ->
                                    if (currentPos.y >= bounds.top && currentPos.y <= bounds.bottom) {
                                        foundHover = idx
                                    }
                                }
                                if (foundHover != highlightedIndex) {
                                    highlightedIndex = foundHover
                                    if (foundHover != -1) {
                                        onHoverItemChanged()
                                    }
                                }
                            },
                            onDragEnd = {
                                if (highlightedIndex in menuItems.indices) {
                                    val action = menuItems[highlightedIndex].first
                                    onMenuAction(action)
                                }
                                isExpanded = false
                                highlightedIndex = -1
                                isPressed = false
                            },
                            onDragCancel = {
                                isExpanded = false
                                highlightedIndex = -1
                                isPressed = false
                            }
                        )
                    }
                }
        ) {
            // Liquid Glass Refractive Backdrop Canvas (Pure, clean glass lens)
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val cr = if (isExpanded) 22.dp.toPx() else (size.minDimension / 2f)

                // 1. Crystal Refraction & Frosted Glass Base
                val liquidFillBrush = if (isExpanded) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xEE161824),
                            Color(0xF510111A),
                            Color(0xFA08090E)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                } else {
                    // Pristine Liquid Glass Sphere (Pure translucent glass)
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x70FFFFFF),
                            Color(0x4000F5D4),
                            Color(0x30FF69B4),
                            Color(0x4010121C),
                            Color(0x75080910)
                        ),
                        center = Offset(w * 0.35f, h * 0.35f),
                        radius = w * 0.90f
                    )
                }

                drawRoundRect(
                    brush = liquidFillBrush,
                    size = Size(w, h),
                    cornerRadius = CornerRadius(cr, cr)
                )

                // 2. Specular Top-Left Fresnel Highlight
                val specularBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xF0FFFFFF),
                        Color(0x66FFFFFF),
                        Color.Transparent,
                        Color(0x22FFFFFF)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w * 0.75f, h * 0.75f)
                )

                drawRoundRect(
                    brush = specularBrush,
                    size = Size(w, h),
                    cornerRadius = CornerRadius(cr, cr),
                    style = Stroke(width = if (isPressed) 2.2f else 1.5f)
                )

                // 3. Bottom-Right Iridescent Chromatic Glint Rim
                val rimBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        AccentPink.copy(alpha = 0.40f),
                        AccentCyan.copy(alpha = 0.50f),
                        Color(0x55FFFFFF)
                    ),
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

            // Expanded Liquid Glass Menu with Hold-and-Drag Highlighting
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Slide to Select",
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .clickable { isExpanded = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("×", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Dynamic Menu Items
                    menuItems.forEachIndexed { index, (action, info) ->
                        val (title, subtitle, icon) = info
                        val isHovered = highlightedIndex == index
                        val tint = when (action) {
                            LiquidMenuAction.LOCK -> Color(0xFFEF4444)
                            LiquidMenuAction.AMOLED_MODE -> if (isDimMode) AccentPink else AccentCyan
                            LiquidMenuAction.SETTINGS -> AccentBlue
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    val bounds = coordinates.boundsInWindow()
                                    itemBounds[index] = bounds
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isHovered) tint.copy(alpha = 0.35f)
                                    else Color(0x401A1C2A)
                                )
                                .border(
                                    1.2.dp,
                                    if (isHovered) tint else Color(0x1AFFFFFF),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    isExpanded = false
                                    onMenuAction(action)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(tint.copy(alpha = if (isHovered) 0.5f else 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isHovered) Color.White else tint,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    color = if (isHovered) Color.White else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = subtitle,
                                    color = if (isHovered) Color.White.copy(alpha = 0.8f) else TextSecondary,
                                    fontSize = 8.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
