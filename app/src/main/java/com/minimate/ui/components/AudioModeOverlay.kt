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
import androidx.compose.runtime.CompositionLocalProvider
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
import com.minimate.touchpad.model.PanelLayout
import com.minimate.touchpad.model.PanelTheme
import com.minimate.touchpad.model.MicrophonePlacement

private enum class AudioEditorTab(val label: String) {
    OUTPUT("Output"), INPUT("Input")
}


@Composable
fun AudioModeOverlay(
    state: AudioBridgeState,
    onOutputEnabled: (Boolean) -> Unit,
    onOutputDeviceSelected: (String) -> Unit,
    onMicrophoneEnabled: (Boolean) -> Unit,
    onOutputVolume: (Float) -> Unit,
    onMicrophoneGain: (Float) -> Unit,
    onPlacement: (MicrophonePlacement) -> Unit,
    onPlacementAuto: (Boolean) -> Unit,
    placement: MicrophonePlacement,
    placementAuto: Boolean,
    panelTheme: PanelTheme,
    panelLayout: PanelLayout,
    editingPanel: Boolean,
    onPanelLayoutChange: (PanelLayout) -> Unit,
    /** Draws the scene behind the panel, so the glass materials have something to work on. */
    backdrop: (@Composable (Modifier) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(AudioEditorTab.OUTPUT) }
    // The whole page, chrome included, follows the panel material — a square panel
    // full of rounded cards still reads as a rounded design with its outline cropped.
    CompositionLocalProvider(LocalPanelCornerScale provides cornerScaleFor(panelTheme.material),
        LocalPanelChrome provides chromeScaleFor(panelTheme.material)) {
        Box(modifier.fillMaxSize()) {
            AudioTopBar(
                state = state,
                selected = selectedTab,
                onSelected = { selectedTab = it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 18.dp, end = 20.dp, top = 20.dp)
            )
            ThemedPanel(
                theme = panelTheme,
                layout = panelLayout,
                editing = editingPanel,
                onLayoutChange = onPanelLayoutChange,
                backdrop = backdrop
            ) {
                when (selectedTab) {
                    AudioEditorTab.OUTPUT -> OutputControls(
                        state, onOutputEnabled, onOutputDeviceSelected, onOutputVolume
                    )
                    AudioEditorTab.INPUT -> MicrophoneControls(
                        state, onMicrophoneEnabled, onMicrophoneGain,
                        placement, onPlacement, placementAuto, onPlacementAuto
                    )
                }
                state.error?.let {
                    Text(it, color = Color(0xFFFFB4AB), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }
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
            Modifier.clip(panelShape(19.dp)).background(panelChrome(Color.Black.copy(.58f)))
                .border(1.dp, panelChrome(Color.White.copy(.16f)), panelShape(19.dp)).padding(4.dp)
        ) {
            AudioEditorTab.values().forEach { tab ->
                val active = tab == selected
                Box(
                    Modifier.clip(panelShape(15.dp))
                        .background(if (active) panelSelectionFill() else Color.Transparent)
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
    onVolume: (Float) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(panelShape(22.dp))
            .background(panelChrome(Brush.linearGradient(listOf(Color(0xB319191B), Color(0x99101012)))))
            .border(1.dp, panelChrome(Color.White.copy(if (state.outputEnabled) .22f else .10f)), panelShape(22.dp))
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(panelChrome(Color.White.copy(.11f))), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Speaker, null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.size(8.dp))
            Column(Modifier.weight(1f)) {
                Text(state.outputDeviceName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
    }
}

@Composable
private fun CompactChoice(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.clip(panelShape(11.dp))
            .background(if (selected) panelSelectionFill() else panelChrome(Color.White.copy(.06f)))
            .border(1.dp, panelChrome(Color.White.copy(if (selected) .9f else .12f)), panelShape(11.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (selected) panelSelectedText() else Color.White.copy(if (enabled) .76f else .26f), fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold)
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
    onPlacement: (MicrophonePlacement) -> Unit,
    placementAuto: Boolean,
    onPlacementAuto: (Boolean) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(panelShape(22.dp))
            .background(panelChrome(Brush.linearGradient(listOf(Color(0xB319191B), Color(0x99101012)))))
            .border(1.dp, panelChrome(Color.White.copy(if (state.microphoneEnabled) .22f else .10f)), panelShape(22.dp))
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(panelChrome(Color.White.copy(.11f))), contentAlignment = Alignment.Center) {
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.weight(1f)) {
                CompactChoice("Auto", placementAuto, state.microphoneEnabled) { onPlacementAuto(true) }
            }
            MicrophonePlacement.entries.forEach { option ->
                Box(Modifier.weight(1f)) {
                    CompactChoice(option.label, !placementAuto && option == placement, state.microphoneEnabled) {
                        onPlacementAuto(false)
                        onPlacement(option)
                    }
                }
            }
        }
        Text(
            if (placementAuto) {
                "Follows whether the phone is lying on its back or in your hand."
            } else when (placement) {
                MicrophonePlacement.HANDHELD -> "Narrow beam, close range. Rejects most of the room."
                MicrophonePlacement.DESK -> "Wider beam and more gain for arm's length."
            },
            color = Color.White.copy(.42f), fontSize = 7.sp
        )
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
