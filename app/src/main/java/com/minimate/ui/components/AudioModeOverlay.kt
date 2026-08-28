package com.minimate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.bluetooth.AudioBridgeState
import com.minimate.touchpad.model.AudioTransport
import com.minimate.touchpad.model.AudioDeviceRoute
import com.minimate.touchpad.model.AudioOutputPreset
import com.minimate.touchpad.model.MicrophoneVoicePreset

private enum class AudioEditorTab(val label: String) { OUTPUT("Output"), INPUT("Input") }

@Composable
fun AudioModeOverlay(
    state: AudioBridgeState,
    onOutputEnabled: (Boolean) -> Unit,
    onOutputRoute: (AudioDeviceRoute) -> Unit,
    onMicrophoneEnabled: (Boolean) -> Unit,
    onInputRoute: (AudioDeviceRoute) -> Unit,
    onVoiceIsolation: (Boolean) -> Unit,
    onOutputVolume: (Float) -> Unit,
    onOutputPreset: (AudioOutputPreset) -> Unit,
    onOutputEqBand: (Int, Float) -> Unit,
    onMicrophoneGain: (Float) -> Unit,
    onMicrophoneNoiseGate: (Float) -> Unit,
    onMicrophonePreset: (MicrophoneVoicePreset) -> Unit,
    onConsumerControl: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(AudioEditorTab.OUTPUT) }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.fillMaxWidth(.76f).padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AudioTopBar(state, selectedTab) { selectedTab = it }
            if (selectedTab == AudioEditorTab.OUTPUT) {
                OutputControls(
                    state = state,
                    onEnabled = onOutputEnabled,
                    onRoute = onOutputRoute,
                    onVolume = onOutputVolume,
                    onPreset = onOutputPreset,
                    onBand = onOutputEqBand
                )
            } else {
                MicrophoneControls(
                    state = state,
                    onEnabled = onMicrophoneEnabled,
                    onRoute = onInputRoute,
                    onVoiceIsolation = onVoiceIsolation,
                    onGain = onMicrophoneGain,
                    onNoiseGate = onMicrophoneNoiseGate,
                    onPreset = onMicrophonePreset
                )
            }
            MediaControls(onSend = onConsumerControl)
            state.error?.let {
                Text(it, color = Color(0xFFFFB4AB), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}

@Composable
private fun AudioTopBar(
    state: AudioBridgeState,
    selected: AudioEditorTab,
    onSelected: (AudioEditorTab) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(
                if (state.connected) {
                    val link = if (state.transport == AudioTransport.WIFI) "LOSSLESS WI-FI" else "BLUETOOTH"
                    "$link · ${state.hostName ?: "CONNECTED"}"
                } else "WAITING FOR COMPANION",
                color = Color.White.copy(.62f), fontSize = 8.sp, fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(if (state.connected) Color(0xFF78F0B3) else Color.White.copy(.28f)))
                Spacer(Modifier.size(5.dp))
                Text(state.outputDeviceName, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.size(10.dp))
        Row(
            Modifier.clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(.55f))
                .border(1.dp, Color.White.copy(.14f), RoundedCornerShape(16.dp)).padding(3.dp)
        ) {
            AudioEditorTab.values().forEach { tab ->
                val active = tab == selected
                Box(
                    Modifier.clip(RoundedCornerShape(12.dp))
                        .background(if (active) Color.White else Color.Transparent)
                        .clickable { onSelected(tab) }.padding(horizontal = 13.dp, vertical = 8.dp)
                ) {
                    Text(tab.label, color = if (active) Color.Black else Color.White.copy(.62f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun OutputControls(
    state: AudioBridgeState,
    onEnabled: (Boolean) -> Unit,
    onRoute: (AudioDeviceRoute) -> Unit,
    onVolume: (Float) -> Unit,
    onPreset: (AudioOutputPreset) -> Unit,
    onBand: (Int, Float) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xB319191B), Color(0x99101012))))
            .border(1.dp, Color.White.copy(if (state.outputEnabled) .22f else .10f), RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(.11f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Speaker, null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.size(8.dp))
            Column(Modifier.weight(1f)) {
                Text(state.outputDeviceName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Independent tuning for this device", color = Color.White.copy(.46f), fontSize = 8.sp)
            }
            Switch(checked = state.outputEnabled, onCheckedChange = onEnabled, colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color.White))
        }
        RouteSelector(
            selected = state.outputRoute,
            connectedName = state.connectedOutputName,
            onSelected = onRoute
        )
        LabeledAudioSlider(
            label = "VOLUME", value = state.outputVolume, range = 0f..1f,
            valueLabel = "${(state.outputVolume * 100).toInt()}%", enabled = state.outputEnabled, onValue = onVolume
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            AudioOutputPreset.values().filter { it != AudioOutputPreset.CUSTOM }.forEach { preset ->
                CompactChoice(preset.label, preset == state.outputPreset, state.outputEnabled) { onPreset(preset) }
            }
        }
        EqualizerGraph(state.outputEqGains, state.outputEnabled, onBand)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("60", "120", "250", "500", "1k", "2k", "4k", "8k", "16k").forEach {
                Text(it, color = Color.White.copy(.38f), fontSize = 6.5.sp)
            }
        }
    }
}

@Composable
private fun EqualizerGraph(gains: List<Float>, enabled: Boolean, onBand: (Int, Float) -> Unit) {
    Canvas(
        Modifier.fillMaxWidth().height(104.dp).clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(.34f))
            .pointerInput(enabled, gains) {
                fun update(x: Float, y: Float) {
                    if (!enabled) return
                    val band = ((x / size.width) * 9).toInt().coerceIn(0, 8)
                    val gain = (12f - (y / size.height) * 24f).coerceIn(-12f, 12f)
                    onBand(band, gain)
                }
                detectDragGestures(
                    onDragStart = { update(it.x, it.y) },
                    onDrag = { change, _ -> update(change.position.x, change.position.y) }
                )
            }
    ) {
        val zeroY = size.height / 2f
        listOf(0f, .25f, .5f, .75f, 1f).forEach { fraction ->
            drawLine(Color.White.copy(if (fraction == .5f) .18f else .06f), androidx.compose.ui.geometry.Offset(0f, size.height * fraction), androidx.compose.ui.geometry.Offset(size.width, size.height * fraction), 1f)
        }
        val points = (0 until 9).map { index ->
            val x = size.width * (index + .5f) / 9f
            val gain = gains.getOrElse(index) { 0f }.coerceIn(-12f, 12f)
            val y = zeroY - gain / 24f * size.height
            androidx.compose.ui.geometry.Offset(x, y)
        }
        points.zipWithNext().forEach { (start, end) -> drawLine(Color.White.copy(if (enabled) .88f else .25f), start, end, 3f) }
        points.forEach { point ->
            drawCircle(Color.White.copy(if (enabled) 1f else .3f), 5f, point)
            drawCircle(Color.Black, 2f, point)
        }
    }
}

@Composable
private fun CompactChoice(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(11.dp))
            .background(if (selected) Color.White else Color.White.copy(.06f))
            .border(1.dp, Color.White.copy(if (selected) .9f else .12f), RoundedCornerShape(11.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (selected) Color.Black else Color.White.copy(if (enabled) .76f else .26f), fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RouteSelector(
    selected: AudioDeviceRoute,
    connectedName: String?,
    onSelected: (AudioDeviceRoute) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.weight(1f)) {
            CompactChoice("Phone", selected == AudioDeviceRoute.BUILT_IN) { onSelected(AudioDeviceRoute.BUILT_IN) }
        }
        Box(Modifier.weight(1f)) {
            CompactChoice(
                connectedName?.take(18) ?: "No connected device",
                selected == AudioDeviceRoute.CONNECTED,
                connectedName != null
            ) { onSelected(AudioDeviceRoute.CONNECTED) }
        }
    }
}

@Composable
private fun MicrophoneControls(
    state: AudioBridgeState,
    onEnabled: (Boolean) -> Unit,
    onRoute: (AudioDeviceRoute) -> Unit,
    onVoiceIsolation: (Boolean) -> Unit,
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
        RouteSelector(
            selected = state.inputRoute,
            connectedName = state.connectedInputName,
            onSelected = onRoute
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("VOICE ISOLATION", color = Color.White.copy(.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text("Mic array focus · noise + echo removal", color = Color.White.copy(.4f), fontSize = 7.5.sp)
            }
            Switch(
                checked = state.voiceIsolation,
                onCheckedChange = onVoiceIsolation,
                enabled = state.microphoneEnabled,
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
            label = "CUTOFF", value = state.microphoneNoiseGate, range = 0f..0.15f,
            valueLabel = "${(state.microphoneNoiseGate * 100).toInt()}%", enabled = state.microphoneEnabled,
            onValue = onNoiseGate
        )
        Text("VOICE", color = Color.White.copy(.4f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                MicrophoneVoicePreset.CLEAN, MicrophoneVoicePreset.RICH, MicrophoneVoicePreset.WARM,
                MicrophoneVoicePreset.BRIGHT
            ).forEach { preset ->
                CompactChoice(preset.label, preset == state.microphonePreset, state.microphoneEnabled) { onPreset(preset) }
            }
        }
        Text("VOICE TOYS", color = Color.White.copy(.4f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                MicrophoneVoicePreset.DEEP, MicrophoneVoicePreset.RADIO, MicrophoneVoicePreset.ROBOT,
                MicrophoneVoicePreset.BABY, MicrophoneVoicePreset.ARENA_ANNOUNCER
            ).forEach { preset ->
                CompactChoice(preset.label, preset == state.microphonePreset, state.microphoneEnabled) { onPreset(preset) }
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
