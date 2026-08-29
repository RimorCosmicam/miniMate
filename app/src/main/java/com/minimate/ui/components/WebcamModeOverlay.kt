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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.bluetooth.AudioBridgeState
import com.minimate.bluetooth.WebcamCaptureState
import com.minimate.touchpad.model.ThemeFilter
import com.minimate.touchpad.model.WebcamResolution
import kotlin.math.min

@Composable
fun WebcamModeOverlay(
    linkState: AudioBridgeState,
    captureState: WebcamCaptureState,
    enabled: Boolean,
    resolution: WebcamResolution,
    fps: Int,
    mirror: Boolean,
    zoom: Float,
    exposure: Float,
    flashEnabled: Boolean,
    flashIntensity: Float,
    intensity: Float,
    filters: List<ThemeFilter>,
    onEnabled: (Boolean) -> Unit,
    onResolution: (WebcamResolution) -> Unit,
    onFps: (Int) -> Unit,
    onMirror: (Boolean) -> Unit,
    onZoom: (Float) -> Unit,
    onExposure: (Float) -> Unit,
    onFlashEnabled: (Boolean) -> Unit,
    onFlashIntensity: (Float) -> Unit,
    onIntensity: (Float) -> Unit,
    onToggleFilter: (ThemeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {
        StatusBadge(
            text = when {
                !linkState.connected -> "MAC OFFLINE"
                linkState.transport.name != "WIFI" -> "WI-FI REQUIRED"
                captureState.running -> "LIVE · ${resolution.label} · $fps"
                enabled -> "STARTING"
                else -> "CAMERA READY"
            },
            active = captureState.running && linkState.connected && linkState.transport.name == "WIFI",
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 20.dp, end = 20.dp)
        )

        Column(
            Modifier.align(Alignment.Center).fillMaxWidth(.72f)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xC4161618), Color(0xB509090B))))
                .border(1.dp, Color.White.copy(.17f), RoundedCornerShape(24.dp))
                .padding(horizontal = 15.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MiniMate Camera", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("No phone preview · filters render on Mac", color = Color.White.copy(.47f), fontSize = 8.5.sp)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color.White,
                        uncheckedThumbColor = Color.White.copy(.72f),
                        uncheckedTrackColor = Color.White.copy(.12f)
                    )
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Choice(resolution.label, resolution == WebcamResolution.FULL_HD, Modifier.weight(1f)) {
                    onResolution(if (resolution == WebcamResolution.FULL_HD) WebcamResolution.HD else WebcamResolution.FULL_HD)
                }
                Choice("$fps fps", fps == 30, Modifier.weight(1f)) {
                    onFps(when (fps) { 15 -> 24; 24 -> 30; else -> 15 })
                }
            }

            LabeledSlider("ZOOM", "%.1f×".format(zoom), zoom, .5f..min(captureState.maximumZoom, 3f).coerceAtLeast(1f), onZoom)
            LabeledSlider("EXPOSURE", "%+.1f".format(exposure), exposure, -1f..1f, onExposure)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice(if (mirror) "Mirrored" else "Natural", mirror, Modifier.width(82.dp)) { onMirror(!mirror) }
                Text("FLASH", color = Color.White.copy(.48f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Switch(
                    checked = flashEnabled && captureState.flashAvailable,
                    onCheckedChange = { if (captureState.flashAvailable) onFlashEnabled(it) },
                    enabled = captureState.flashAvailable,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color.White,
                        uncheckedThumbColor = Color.White.copy(.72f),
                        uncheckedTrackColor = Color.White.copy(.12f)
                    )
                )
                if (flashEnabled && captureState.flashAvailable && captureState.flashMaximumLevel > 1) {
                    Slider(
                        value = flashIntensity,
                        onValueChange = onFlashIntensity,
                        modifier = Modifier.weight(1f).height(24.dp),
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(.12f))
                    )
                    Text("${(flashIntensity * 100).toInt()}%", color = Color.White.copy(.72f), fontSize = 8.5.sp, modifier = Modifier.width(32.dp))
                } else {
                    Text(
                        if (captureState.flashAvailable) "Torch" else "Unavailable",
                        color = Color.White.copy(.42f), fontSize = 8.sp, modifier = Modifier.weight(1f)
                    )
                }
            }
            LabeledSlider("FILTER MIX", "${(intensity * 100).toInt()}%", intensity, 0f..1f, onIntensity)

            Text("STACKED FILTERS", color = Color.White.copy(.42f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ThemeFilter.values().filter { it != ThemeFilter.NONE }.forEach { filter ->
                    Choice(filter.label, filter in filters) { onToggleFilter(filter) }
                }
            }
            captureState.error?.let { Text(it, color = Color(0xFFFFB4AB), fontSize = 8.5.sp) }
        }
    }
}

@Composable
private fun StatusBadge(text: String, active: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(.62f))
            .border(1.dp, Color.White.copy(.15f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(7.dp).height(7.dp).clip(CircleShape).background(if (active) Color(0xFF77F2B4) else Color.White.copy(.25f)))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White.copy(.76f), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(13.dp))
            .background(if (selected) Color.White else Color.White.copy(.075f))
            .border(1.dp, Color.White.copy(if (selected) .65f else .11f), RoundedCornerShape(13.dp))
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.Black else Color.White.copy(.78f), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun LabeledSlider(label: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(.48f), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(55.dp))
        Slider(
            value = value,
            onValueChange = onValue,
            valueRange = range,
            modifier = Modifier.weight(1f).height(24.dp),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(.12f))
        )
        Text(valueLabel, color = Color.White.copy(.72f), fontSize = 8.5.sp, modifier = Modifier.width(38.dp))
    }
}
