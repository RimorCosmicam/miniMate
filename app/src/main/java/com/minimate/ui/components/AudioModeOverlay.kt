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
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.bluetooth.AudioBridgeState
import com.minimate.bluetooth.AudioDeviceSummary
import com.minimate.touchpad.model.AudioTransport
import com.minimate.touchpad.model.AudioOutputPreset
import com.minimate.touchpad.model.MicrophonePlacement
import com.minimate.touchpad.model.MicrophoneVoicePreset
import com.minimate.touchpad.model.SuperhumanPreset

private enum class AudioEditorTab(val label: String) {
    OUTPUT("Output"), INPUT("Input"), TOOLS("Tools")
}


@Composable
fun AudioModeOverlay(
    state: AudioBridgeState,
    onOutputEnabled: (Boolean) -> Unit,
    onOutputDeviceSelected: (String) -> Unit,
    onMicrophoneEnabled: (Boolean) -> Unit,
    onOutputVolume: (Float) -> Unit,
    onOutputPreset: (AudioOutputPreset) -> Unit,
    onOutputEqBand: (Int, Float) -> Unit,
    onMicrophoneGain: (Float) -> Unit,
    onMicrophonePreset: (MicrophoneVoicePreset) -> Unit,
    onListenToggled: (Boolean) -> Unit,
    onPlacement: (MicrophonePlacement) -> Unit,
    placement: MicrophonePlacement,
    onSuperhumanPreset: (SuperhumanPreset) -> Unit,
    onListenVolume: (Float) -> Unit,
    listenVolume: Float,
    microphonePreset: MicrophoneVoicePreset,
    superhumanBands: List<Float>,
    listening: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(AudioEditorTab.OUTPUT) }
    Box(modifier.fillMaxSize()) {
        AudioTopBar(
            state = state,
            selected = selectedTab,
            onSelected = { selectedTab = it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 18.dp, end = 20.dp, top = 20.dp)
        )
        // The cover display has a physical camera cutout in the bottom-right corner, roughly
        // 84 dp tall by 198 dp wide on a 361 x 399 dp panel. Content is kept above it and made
        // scrollable rather than allowed to extend underneath, where it cannot be seen or
        // touched. The Tools tab is the tallest panel and was running straight into it.
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(.70f)
                .padding(top = 58.dp, bottom = 96.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (selectedTab) {
                AudioEditorTab.OUTPUT -> OutputControls(
                    state, onOutputEnabled, onOutputDeviceSelected, onOutputVolume,
                    onOutputPreset, onOutputEqBand
                )
                AudioEditorTab.INPUT -> MicrophoneControls(
                    state, onMicrophoneEnabled, onMicrophoneGain,
                    placement, onPlacement
                )
                AudioEditorTab.TOOLS -> ToolsControls(
                    preset = microphonePreset,
                    bands = superhumanBands,
                    listening = listening,
                    listenVolume = listenVolume,
                    onPreset = onMicrophonePreset,
                    onBandPreset = onSuperhumanPreset,
                    onListenVolume = onListenVolume,
                    onListenToggled = onListenToggled
                )
            }
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
    onSelected: (AudioEditorTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(if (state.connected) Color(0xFF78F0B3) else Color.White.copy(.28f)))
            Spacer(Modifier.size(5.dp))
            Text(
                if (!state.connected) "OFFLINE" else if (state.transport == AudioTransport.WIFI) "WI-FI" else "BT",
                color = Color.White.copy(.72f), fontSize = 8.5.sp, fontWeight = FontWeight.Bold
            )
        }
        Row(
            Modifier.clip(RoundedCornerShape(19.dp)).background(Color.Black.copy(.58f))
                .border(1.dp, Color.White.copy(.16f), RoundedCornerShape(19.dp)).padding(4.dp)
        ) {
            AudioEditorTab.values().forEach { tab ->
                val active = tab == selected
                Box(
                    Modifier.clip(RoundedCornerShape(15.dp))
                        .background(if (active) Color.White else Color.Transparent)
                        .clickable { onSelected(tab) }.padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(tab.label, color = if (active) Color.Black else Color.White.copy(.68f), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun OutputControls(
    state: AudioBridgeState,
    onEnabled: (Boolean) -> Unit,
    onDeviceSelected: (String) -> Unit,
    onVolume: (Float) -> Unit,
    onPreset: (AudioOutputPreset) -> Unit,
    onBand: (Int, Float) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xB319191B), Color(0x99101012))))
            .border(1.dp, Color.White.copy(if (state.outputEnabled) .22f else .10f), RoundedCornerShape(22.dp))
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
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
        DeviceList(
            devices = state.outputDevices,
            selectedKey = state.selectedOutputKey,
            enabled = state.outputEnabled,
            onSelected = onDeviceSelected
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
        Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(14.dp))
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

/** Every available device is tappable directly — no separate enable/toggle step. */
@Composable
private fun DeviceList(
    devices: List<AudioDeviceSummary>,
    selectedKey: String,
    enabled: Boolean,
    onSelected: (String) -> Unit
) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        devices.forEach { device ->
            CompactChoice(device.name.take(18), device.key == selectedKey, enabled) { onSelected(device.key) }
        }
    }
}

@Composable
private fun MicrophoneControls(
    state: AudioBridgeState,
    onEnabled: (Boolean) -> Unit,
    onGain: (Float) -> Unit,
    placement: MicrophonePlacement,
    onPlacement: (MicrophonePlacement) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xB319191B), Color(0x99101012))))
            .border(1.dp, Color.White.copy(if (state.microphoneEnabled) .22f else .10f), RoundedCornerShape(22.dp))
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(Color.White.copy(.11f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.size(7.dp))
            Column(Modifier.weight(1f)) {
                Text("Microphone", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Phone array, beam aimed at you", color = Color.White.copy(.48f), fontSize = 7.5.sp)
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
        CompactAudioSlider(
            label = "GAIN", value = state.microphoneGain, range = 0f..3f,
            valueLabel = "${state.microphoneGain.formatGain()}×", enabled = state.microphoneEnabled,
            onValue = onGain
        )
        Text("PHONE POSITION", color = Color.White.copy(.4f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MicrophonePlacement.entries.forEach { option ->
                Box(Modifier.weight(1f)) {
                    CompactChoice(option.label, option == placement, state.microphoneEnabled) {
                        onPlacement(option)
                    }
                }
            }
        }
        Text(
            when (placement) {
                MicrophonePlacement.HANDHELD -> "Narrow beam, close range. Rejects most of the room."
                MicrophonePlacement.DESK -> "Wider beam and more gain for arm's length."
            },
            color = Color.White.copy(.42f), fontSize = 7.sp
        )
    }
}

/**
 * Listening instruments. These run entirely on the phone — its microphone straight back out of
 * its own USB-C output — so they work with nothing connected and without a wireless round trip
 * between hearing and moving the phone.
 */
@Composable
private fun ToolsControls(
    preset: MicrophoneVoicePreset,
    bands: List<Float>,
    listening: Boolean,
    listenVolume: Float,
    onPreset: (MicrophoneVoicePreset) -> Unit,
    onBandPreset: (SuperhumanPreset) -> Unit,
    onListenVolume: (Float) -> Unit,
    onListenToggled: (Boolean) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xB319191B), Color(0x99101012))))
            .border(1.dp, Color.White.copy(.20f), RoundedCornerShape(22.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("LISTEN ON DEVICE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Phone mic to your earphones. No desktop needed.",
                    color = Color.White.copy(.44f), fontSize = 7.sp
                )
            }
            Switch(
                checked = listening,
                onCheckedChange = onListenToggled,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color.White)
            )
        }

        // Separate from microphone trim on purpose: the earphones sit centimetres from the
        // microphone feeding them, so this starts low and is the first thing to reach for.
        CompactAudioSlider(
            label = "LISTEN VOLUME", value = listenVolume, range = 0f..1f,
            valueLabel = "${(listenVolume * 100).toInt()}%", enabled = true, onValue = onListenVolume
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(MicrophoneVoicePreset.STETHO, MicrophoneVoicePreset.SUPERHUMAN).forEach { tool ->
                Box(Modifier.weight(1f)) {
                    CompactChoice(tool.label, preset == tool) {
                        onPreset(if (preset == tool) MicrophoneVoicePreset.CLEAN else tool)
                    }
                }
            }
        }

        Text(
            when (preset) {
                MicrophoneVoicePreset.STETHO ->
                    "Contact listening: press the phone against a surface for structure-borne sound."
                MicrophoneVoicePreset.SUPERHUMAN ->
                    "Maximum sensitivity, no filtering, no gate. Pick what you are listening for."
                else -> "Pick a tool. Both bypass noise suppression so faint sound survives."
            },
            color = Color.White.copy(.42f), fontSize = 7.sp
        )

        if (preset == MicrophoneVoicePreset.SUPERHUMAN) {
            Text("LISTENING FOR", color = Color.White.copy(.4f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                SuperhumanPreset.entries.forEach { shape ->
                    CompactChoice(shape.label, bands == shape.bands) { onBandPreset(shape) }
                }
            }
            SuperhumanPreset.entries.firstOrNull { it.bands == bands }?.let { active ->
                Text(active.hint, color = Color.White.copy(.4f), fontSize = 7.sp)
            }
        }
    }
}

@Composable
private fun CompactAudioSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    enabled: Boolean,
    onValue: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White.copy(if (enabled) .56f else .22f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Text(valueLabel, color = Color.White.copy(if (enabled) .72f else .25f), fontSize = 7.sp)
        }
        Slider(
            value = value, onValueChange = onValue, valueRange = range, enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(30.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White, activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(.14f)
            )
        )
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

private fun Float.formatGain(): String = if (this % 1f == 0f) toInt().toString() else String.format("%.1f", this)
