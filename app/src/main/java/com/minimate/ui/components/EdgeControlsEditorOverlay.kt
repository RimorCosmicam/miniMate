package com.minimate.ui.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.touchpad.model.EdgeControlSide
import com.minimate.touchpad.model.EdgeControlMaterial
import com.minimate.touchpad.model.TouchpadSettings
import kotlin.math.roundToInt

private enum class EdgeEditorTarget(val label: String) { RAIL("Scroll rail"), CORNER("Right click") }

@Composable
fun EdgeControlsEditorOverlay(
    settings: TouchpadSettings,
    onSettingsChange: (TouchpadSettings) -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onCancel)
    var target by remember { mutableStateOf(EdgeEditorTarget.RAIL) }

    Box(modifier.fillMaxSize()) {
        StudioPanel(
            onCancel = onCancel,
            onDone = onDone,
            // Low and to the side. The right-click corner sits at the top edge and the scroll
            // rail runs the full height, so a panel anchored to the top covers the very controls
            // being resized; the bottom margin keeps it clear of the camera cutout as well.
            modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 96.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                EdgeEditorTarget.entries.forEach { option ->
                    StudioChip(option.label, target == option, Modifier.weight(1f)) { target = option }
                }
            }
            StudioLabel("OPTICS", dim = true, size = 11)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                EdgeControlMaterial.entries.forEach { material ->
                    val selected = if (target == EdgeEditorTarget.RAIL) settings.edgeRailMaterial == material else settings.edgeCornerMaterial == material
                    StudioChip(material.label, selected) {
                        onSettingsChange(
                            if (target == EdgeEditorTarget.RAIL) settings.copy(edgeRailMaterial = material)
                            else settings.copy(edgeCornerMaterial = material)
                        )
                    }
                }
            }
            // The two carry their own size. Editing one never moves the other.
            val scale = if (target == EdgeEditorTarget.RAIL) settings.edgeRailScale else settings.edgeCornerScale
            StudioSizeSlider("${target.label} size", scale) { next ->
                onSettingsChange(
                    if (target == EdgeEditorTarget.RAIL) settings.copy(edgeRailScale = next)
                    else settings.copy(edgeCornerScale = next)
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StudioLabel("SIDE", dim = true, size = 11)
                Spacer(Modifier.width(8.dp))
                EdgeControlSide.entries.forEach { side ->
                    Box(Modifier.weight(1f).padding(horizontal = 2.dp)) {
                        StudioChip(if (side == EdgeControlSide.LEFT) "Rail left" else "Rail right", settings.edgeControlSide == side, Modifier.fillMaxWidth()) {
                            onSettingsChange(settings.copy(edgeControlSide = side))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioSizeSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StudioLabel(label.uppercase(), dim = true, size = 11)
            StudioLabel("${(value * 100f).roundToInt()}%", size = 11)
        }
        Slider(
            value = value.coerceIn(.65f, 1.8f),
            onValueChange = onChange,
            valueRange = .65f..1.8f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(.14f)
            ),
            modifier = Modifier.fillMaxWidth().height(28.dp)
        )
    }
}
