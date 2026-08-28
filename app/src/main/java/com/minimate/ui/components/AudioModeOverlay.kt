package com.minimate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
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
import com.minimate.touchpad.model.MicrophoneVoicePreset

@Composable
fun AudioModeOverlay(
    state: AudioBridgeState,
    onOutputEnabled: (Boolean) -> Unit,
    onMicrophoneEnabled: (Boolean) -> Unit,
    onOutputVolume: (Float) -> Unit,
    onMicrophoneGain: (Float) -> Unit,
    onMicrophoneNoiseGate: (Float) -> Unit,
    onMicrophonePreset: (MicrophoneVoicePreset) -> Unit,
    onConsumerControl: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.fillMaxWidth(.62f).padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Box(
                    Modifier.size(8.dp).clip(CircleShape)
                        .background(if (state.connected) Color(0xFF78F0B3) else Color.White.copy(.28f))
                )
                Spacer(Modifier.size(7.dp))
                Column {
                    Text("AUDIO BRIDGE", color = Color.White.copy(.62f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (state.connected) {
                            val connection = if (state.transport == AudioTransport.WIFI) "Lossless Wi-Fi" else "Bluetooth"
                            "$connection · ${state.hostName ?: "Connected"}"
                        } else "Waiting for companion",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
            MicrophoneControls(
                state = state,
                onEnabled = onMicrophoneEnabled,
                onGain = onMicrophoneGain,
                onNoiseGate = onMicrophoneNoiseGate,
                onPreset = onMicrophonePreset
            )
            MediaControls(onSend = onConsumerControl)
            state.error?.let {
                Text(it, color = Color(0xFFFFB4AB), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}

@Composable
private fun MicrophoneControls(
    state: AudioBridgeState,
    onEnabled: (Boolean) -> Unit,
    onGain: (Float) -> Unit,
    onNoiseGate: (Float) -> Unit,
    onPreset: (MicrophoneVoicePreset) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xB319191B), Color(0x99101012))))
            .border(1.dp, Color.White.copy(if (state.microphoneEnabled) .22f else .10f), RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(.11f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Microphone", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Live processing to the desktop", color = Color.White.copy(.48f), fontSize = 8.5.sp)
            }
            Switch(
                checked = state.microphoneEnabled,
                onCheckedChange = onEnabled,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color.White)
            )
        }
        LinearProgressIndicator(
            progress = state.microphoneLevel,
            modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
            color = Color.White,
            trackColor = Color.White.copy(.10f)
        )
        LabeledAudioSlider(
            label = "GAIN", value = state.microphoneGain, range = 0f..2f,
            valueLabel = "${state.microphoneGain.formatGain()}×", enabled = state.microphoneEnabled,
            onValue = onGain
        )
        LabeledAudioSlider(
            label = "NOISE GATE", value = state.microphoneNoiseGate, range = 0f..0.15f,
            valueLabel = "${(state.microphoneNoiseGate * 100).toInt()}%", enabled = state.microphoneEnabled,
            onValue = onNoiseGate
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MicrophoneVoicePreset.values().forEach { preset ->
                val selected = preset == state.microphonePreset
                Box(
                    Modifier.clip(RoundedCornerShape(12.dp))
                        .background(if (selected) Color.White else Color.White.copy(.06f))
                        .border(1.dp, Color.White.copy(if (selected) .9f else .12f), RoundedCornerShape(12.dp))
                        .clickable(enabled = state.microphoneEnabled) { onPreset(preset) }
                        .padding(horizontal = 11.dp, vertical = 7.dp)
                ) {
                    Text(preset.label, color = if (selected) Color.Black else Color.White.copy(.76f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun LabeledAudioSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    enabled: Boolean,
    onValue: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(if (enabled) .56f else .22f), fontSize = 8.sp, modifier = Modifier.weight(.42f))
        Slider(
            value = value, onValueChange = onValue, valueRange = range, enabled = enabled,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White, activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(.14f)
            )
        )
        Text(valueLabel, color = Color.White.copy(if (enabled) .72f else .25f), fontSize = 9.sp, modifier = Modifier.weight(.25f))
    }
}

private object AudioMediaUsage {
    const val PREVIOUS = 0x00B6
    const val PLAY_PAUSE = 0x00CD
    const val NEXT = 0x00B5
    const val VOLUME_UP = 0x00E9
    const val VOLUME_DOWN = 0x00EA
}

@Composable
private fun MediaControls(onSend: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(.42f))
            .border(1.dp, Color.White.copy(.13f), RoundedCornerShape(20.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MediaButton(Icons.Default.VolumeDown, "Volume down") { onSend(AudioMediaUsage.VOLUME_DOWN) }
        MediaButton(Icons.Default.SkipPrevious, "Previous") { onSend(AudioMediaUsage.PREVIOUS) }
        MediaButton(Icons.Default.PlayArrow, "Play or pause", emphasized = true) { onSend(AudioMediaUsage.PLAY_PAUSE) }
        MediaButton(Icons.Default.SkipNext, "Next") { onSend(AudioMediaUsage.NEXT) }
        MediaButton(Icons.Default.VolumeUp, "Volume up") { onSend(AudioMediaUsage.VOLUME_UP) }
    }
}

@Composable
private fun MediaButton(
    icon: ImageVector,
    description: String,
    emphasized: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(if (emphasized) 40.dp else 34.dp)
            .clip(CircleShape)
            .background(if (emphasized) Color.White else Color.White.copy(.07f))
    ) {
        Icon(
            icon,
            description,
            tint = if (emphasized) Color.Black else Color.White.copy(.88f),
            modifier = Modifier.size(if (emphasized) 21.dp else 17.dp)
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
