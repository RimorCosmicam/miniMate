package com.minimate.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.bluetooth.BluetoothUiState
import com.minimate.bluetooth.ConnectionStatus
import com.minimate.touchpad.model.ClockStyle
import com.minimate.ui.theme.AccentCyan
import com.minimate.ui.theme.AccentEmerald
import com.minimate.ui.theme.AccentPink
import com.minimate.ui.theme.TextPrimary
import com.minimate.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Interactive Clock & Battery HUD Widget.
 * - Tap: Advance through Trackpad, Keyboard, Playback, and Microphone
 * - Double tap: Toggle AMOLED mode
 * - Hold: Open Settings Control Center
 */
@Composable
fun ClockBatteryOverlay(
    clockStyle: ClockStyle,
    positionXFraction: Float,
    positionYFraction: Float,
    clockScale: Float,
    screenWidthPx: Float,
    screenHeightPx: Float,
    show24Hour: Boolean,
    showSeconds: Boolean,
    showBattery: Boolean,
    batteryPercentage: Int,
    bluetoothState: BluetoothUiState,
    amoledMode: Boolean = false,
    onTap: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (clockStyle == ClockStyle.OFF) return

    var currentTimeText by remember { mutableStateOf("") }
    var currentAmPm by remember { mutableStateOf("") }
    var isPressed by remember { mutableStateOf(false) }
    var measuredSize by remember { mutableStateOf(IntSize.Zero) }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "ClockPressScale"
    )

    LaunchedEffect(show24Hour, showSeconds) {
        while (true) {
            val now = Date()
            val pattern = when {
                show24Hour && showSeconds -> "HH:mm:ss"
                show24Hour -> "HH:mm"
                showSeconds -> "h:mm:ss"
                else -> "h:mm"
            }
            val formatter = SimpleDateFormat(pattern, Locale.getDefault())
            currentTimeText = formatter.format(now)

            if (!show24Hour) {
                val amPmFormatter = SimpleDateFormat("a", Locale.getDefault())
                currentAmPm = amPmFormatter.format(now).uppercase()
            } else {
                currentAmPm = ""
            }
            delay(if (showSeconds) 500L else 1000L)
        }
    }

    val widgetWidth = measuredSize.width.toFloat()
    val widgetHeight = measuredSize.height.toFloat()
    val posX = (positionXFraction * screenWidthPx - widgetWidth / 2f)
        .coerceIn(8f, (screenWidthPx - widgetWidth - 8f).coerceAtLeast(8f))
    val posY = (positionYFraction * screenHeightPx - widgetHeight / 2f)
        .coerceIn(8f, (screenHeightPx - widgetHeight - 8f).coerceAtLeast(8f))

    Box(
        modifier = modifier
            .offset { IntOffset(posX.roundToInt(), posY.roundToInt()) }
            .onSizeChanged { measuredSize = it }
            .scale(clockScale * pressScale)
            .shadow(12.dp, RoundedCornerShape(18.dp), spotColor = if (amoledMode) Color.Transparent else AccentCyan.copy(alpha = 0.35f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onTap() },
                    onDoubleTap = { onDoubleTap() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        when (clockStyle) {
            ClockStyle.MINIMAL_PILL -> {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (amoledMode) Brush.linearGradient(listOf(Color.Black, Color.Black)) else Brush.linearGradient(
                                colors = listOf(
                                    Color(0xEA161828),
                                    Color(0xF010111D)
                                )
                            )
                        )
                        .border(
                            1.2.dp,
                            if (amoledMode) Color.White.copy(if (isPressed) .9f else .42f) else if (isPressed) AccentPink else Color(0x35FFFFFF),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentTimeText,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    if (currentAmPm.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = currentAmPm,
                            color = if (amoledMode) Color.White else AccentPink,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (showBattery) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (batteryPercentage > 90) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                                contentDescription = null,
                                tint = if (amoledMode) Color.White else if (batteryPercentage < 20) Color(0xFFEF4444) else AccentEmerald,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$batteryPercentage%",
                                color = TextSecondary,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (bluetoothState.status == ConnectionStatus.CONNECTED) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                            .background(if (amoledMode) Color.White else AccentCyan)
                        )
                    }
                }
            }

            ClockStyle.DIGITAL_BOLD -> {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xD90A0C16))
                        .border(
                            1.2.dp,
                            if (isPressed) AccentPink else AccentCyan.copy(alpha = 0.5f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentTimeText,
                        color = AccentCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    if (showBattery) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$batteryPercentage%",
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            ClockStyle.CLEAN_SANS -> {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xCC10121C))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentTimeText,
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (showBattery) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$batteryPercentage%",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 9.5.sp
                        )
                    }
                }
            }

            ClockStyle.MONOSPACE -> {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xF0080B12))
                        .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 11.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentTimeText,
                        color = Color(0xFF00FF66),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    if (showBattery) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$batteryPercentage%",
                            color = Color(0xFF00FF66).copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            ClockStyle.OFF -> {}
        }
    }
}
