package com.minimate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.BluetoothConnected
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
    dimRatio: Float,
    modifier: Modifier = Modifier
) {
    if (clockStyle == ClockStyle.OFF || dimRatio > 0.85f) return

    var currentTimeText by remember { mutableStateOf("") }
    var currentAmPm by remember { mutableStateOf("") }

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

    // Dynamic pixel position based on normalized coordinates
    val widgetWidth = (140f * clockScale) * 2.7f
    val widgetHeight = (36f * clockScale) * 2.7f
    val posX = (positionXFraction * screenWidthPx - widgetWidth / 2f).coerceIn(8f, screenWidthPx - widgetWidth - 8f)
    val posY = (positionYFraction * screenHeightPx - widgetHeight / 2f).coerceIn(8f, screenHeightPx - widgetHeight - 8f)

    Box(
        modifier = modifier
            .offset { IntOffset(posX.roundToInt(), posY.roundToInt()) }
            .scale(clockScale)
    ) {
        when (clockStyle) {
            ClockStyle.MINIMAL_PILL -> {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xD912131F))
                        .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentTimeText,
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    if (currentAmPm.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = currentAmPm,
                            color = AccentPink,
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
                                tint = if (batteryPercentage < 20) Color(0xFFEF4444) else AccentEmerald,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
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
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(AccentCyan)
                        )
                    }
                }
            }

            ClockStyle.DIGITAL_BOLD -> {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xCC000000))
                        .border(1.dp, AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
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
                        .background(Color(0x88000000))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentTimeText,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (showBattery) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$batteryPercentage%",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 9.5.sp
                        )
                    }
                }
            }

            ClockStyle.MONOSPACE -> {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xEE0A0C14))
                        .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
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
