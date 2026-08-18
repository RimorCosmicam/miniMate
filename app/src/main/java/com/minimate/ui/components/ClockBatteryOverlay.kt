package com.minimate.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.bluetooth.BluetoothUiState
import com.minimate.bluetooth.ConnectionStatus
import com.minimate.touchpad.model.ClockPosition
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

@Composable
fun BoxScope.ClockBatteryOverlay(
    clockStyle: ClockStyle,
    clockPosition: ClockPosition,
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
            delay(1000)
        }
    }

    val alignment = when (clockPosition) {
        ClockPosition.TOP_LEFT -> Alignment.TopStart
        ClockPosition.TOP_CENTER -> Alignment.TopCenter
        ClockPosition.TOP_RIGHT -> Alignment.TopEnd
        ClockPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
        ClockPosition.BOTTOM_CENTER -> Alignment.BottomCenter
    }

    val paddingModifier = when (clockPosition) {
        ClockPosition.TOP_LEFT -> Modifier.padding(start = 16.dp, top = 16.dp)
        ClockPosition.TOP_CENTER -> Modifier.padding(top = 16.dp)
        ClockPosition.TOP_RIGHT -> Modifier.padding(end = 16.dp, top = 16.dp)
        ClockPosition.BOTTOM_RIGHT -> Modifier.padding(end = 16.dp, bottom = 16.dp)
        ClockPosition.BOTTOM_CENTER -> Modifier.padding(bottom = 16.dp)
    }

    AnimatedVisibility(
        visible = currentTimeText.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .align(alignment)
            .then(paddingModifier)
    ) {
        when (clockStyle) {
            ClockStyle.MINIMAL_PILL -> {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x7012131C))
                        .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentTimeText,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    if (currentAmPm.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentAmPm,
                            color = AccentCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (showBattery) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color(0x55FFFFFF))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.BatteryFull,
                            contentDescription = null,
                            tint = if (batteryPercentage <= 20) AccentPink else AccentEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$batteryPercentage%",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (bluetoothState.status == ConnectionStatus.CONNECTED) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.BluetoothConnected,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            ClockStyle.DIGITAL_BOLD -> {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x88000000))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = currentTimeText,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    if (currentAmPm.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentAmPm,
                            color = AccentCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    if (showBattery) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$batteryPercentage%",
                            color = if (batteryPercentage <= 20) AccentPink else AccentEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                }
            }

            ClockStyle.CLEAN_SANS -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentTimeText,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.5.sp
                    )
                    if (currentAmPm.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentAmPm,
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    }
                    if (showBattery) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$batteryPercentage%",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            ClockStyle.MONOSPACE -> {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x99101018))
                        .border(1.dp, AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentTimeText,
                        color = AccentCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    if (showBattery) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "[$batteryPercentage%]",
                            color = AccentEmerald,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            ClockStyle.OFF -> Unit
        }
    }
}
