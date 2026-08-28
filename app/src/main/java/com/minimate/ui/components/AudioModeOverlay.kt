package com.minimate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.bluetooth.AudioBridgeState
import com.minimate.touchpad.model.AudioTransport

@Composable
fun AudioModeOverlay(
    state: AudioBridgeState,
    onOutputEnabled: (Boolean) -> Unit,
    onMicrophoneEnabled: (Boolean) -> Unit,
    onOutputVolume: (Float) -> Unit,
    onMicrophoneGain: (Float) -> Unit,
    onTransport: (AudioTransport) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.padding(start = 18.dp, top = 20.dp, end = 112.dp).widthIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(8.dp).clip(CircleShape)
                    .background(if (state.connected) Color(0xFF78F0B3) else Color.White.copy(.28f))
            )
            Spacer(Modifier.size(7.dp))
            Column {
                Text("AUDIO BRIDGE", color = Color.White.copy(.62f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (state.connected) state.hostName ?: "Desktop connected" else "Waiting for companion",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(.42f)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TransportButton(
                label = "Wi-Fi",
                selected = state.transport == AudioTransport.WIFI,
                enabled = state.wifiAvailable,
                modifier = Modifier.weight(1f)
            ) { onTransport(AudioTransport.WIFI) }
            TransportButton(
                label = "Bluetooth",
                selected = state.transport == AudioTransport.BLUETOOTH,
                enabled = state.bluetoothAvailable,
                modifier = Modifier.weight(1f)
            ) { onTransport(AudioTransport.BLUETOOTH) }
        }
        AudioControlCard(
            icon = Icons.Default.Speaker,
            title = "Output",
            detail = "Desktop audio on this phone",
            enabled = state.outputEnabled,
            value = state.outputVolume,
            valueRange = 0f..1f,
            valueLabel = "${(state.outputVolume * 100).toInt()}%",
            onEnabled = onOutputEnabled,
            onValue = onOutputVolume
        )
        AudioControlCard(
            icon = Icons.Default.Mic,
            title = "Microphone",
            detail = "Phone microphone to desktop",
            enabled = state.microphoneEnabled,
            value = state.microphoneGain,
            valueRange = 0f..2f,
            valueLabel = "${state.microphoneGain.formatGain()}×",
            onEnabled = onMicrophoneEnabled,
            onValue = onMicrophoneGain
        )
        state.error?.let {
            Text(it, color = Color(0xFFFFB4AB), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

@Composable
private fun TransportButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.background(
            if (selected) Color.White else Color.Transparent,
            RoundedCornerShape(12.dp)
        )
    ) {
        Text(
            label,
            color = when { selected -> Color.Black; enabled -> Color.White.copy(.72f); else -> Color.White.copy(.22f) },
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AudioControlCard(
    icon: ImageVector,
    title: String,
    detail: String,
    enabled: Boolean,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onEnabled: (Boolean) -> Unit,
    onValue: (Float) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xB319191B), Color(0x99101012))))
            .border(1.dp, Color.White.copy(if (enabled) .22f else .10f), RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape)
                    .background(Color.White.copy(if (enabled) .13f else .055f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = Color.White.copy(if (enabled) .95f else .35f), modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(detail, color = Color.White.copy(.48f), fontSize = 8.5.sp)
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color.White,
                    uncheckedThumbColor = Color.White.copy(.55f),
                    uncheckedTrackColor = Color.White.copy(.08f),
                    uncheckedBorderColor = Color.White.copy(.12f)
                )
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = value,
                onValueChange = onValue,
                valueRange = valueRange,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(.14f),
                    disabledThumbColor = Color.White.copy(.25f),
                    disabledActiveTrackColor = Color.White.copy(.12f)
                )
            )
            Text(valueLabel, color = Color.White.copy(if (enabled) .72f else .25f), fontSize = 9.sp, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

private fun Float.formatGain(): String = if (this % 1f == 0f) toInt().toString() else String.format("%.1f", this)
