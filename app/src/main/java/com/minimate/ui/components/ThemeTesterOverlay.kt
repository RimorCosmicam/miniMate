package com.minimate.ui.components

import android.view.MotionEvent
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
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.touchpad.model.BackgroundAnimation
import com.minimate.touchpad.model.AbstractShaderTheme
import com.minimate.touchpad.model.StickTheme
import com.minimate.touchpad.model.ThemeFilter
import com.minimate.touchpad.model.TouchpadSettings
import com.minimate.touchpad.model.colorwaysFor
import com.minimate.touchpad.model.sceneColorwayFor
import com.minimate.touchpad.model.subthemesFor
import com.minimate.touchpad.model.validColorway

private enum class StudioPicker(val label: String) {
    THEME("Theme"), SUBTHEME("Subtheme"), RECOLOR("Color"),
    MOTION("Motion"), FILTER("Filter"), STICK("Stick")
}

/** A two-row direct picker: no hidden cycle order and almost no artwork occlusion. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ThemeTesterOverlay(
    settings: TouchpadSettings,
    onSettingsChange: (TouchpadSettings) -> Unit,
    onPreviewTouchEvent: (MotionEvent) -> Boolean,
    onKeep: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onCancel)
    var picker by remember { mutableStateOf(StudioPicker.THEME) }
    var customRole by remember { mutableStateOf(0) }
    val subtheme = subthemesFor(settings.abstractShaderTheme)
        .getOrElse(settings.abstractSubthemeIndex) { subthemesFor(settings.abstractShaderTheme).first() }

    Box(modifier.fillMaxSize().displayCutoutPadding().navigationBarsPadding()) {
        Box(Modifier.fillMaxSize().pointerInteropFilter { onPreviewTouchEvent(it) })

        Row(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EdgeAction(Icons.Default.Close, "Revert", Color(0xD617181A), onCancel)
            Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${settings.abstractShaderTheme.label} · ${subtheme.label}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(sceneColorwayFor(settings.abstractShaderTheme, settings.abstractSubthemeIndex, settings.shaderRecolor).label, color = Color.White.copy(.7f), fontSize = 8.5.sp, maxLines = 1)
            }
            EdgeAction(Icons.Default.Check, "Keep", Color(0xE629292C), onKeep)
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xE0101113), RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)).padding(vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                StudioPicker.values().forEach { value -> DirectChip(value.label, picker == value) { picker = value } }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                when (picker) {
                    StudioPicker.THEME -> AbstractShaderTheme.values().forEach { theme ->
                        DirectChip(theme.label, settings.abstractShaderTheme == theme) {
                            onSettingsChange(settings.copy(abstractShaderTheme = theme, abstractSubthemeIndex = 0, shaderRecolor = colorwaysFor(theme, 0).first(), customImageUri = null))
                        }
                    }
                    StudioPicker.SUBTHEME -> subthemesFor(settings.abstractShaderTheme).forEach { option ->
                        DirectChip(option.label, settings.abstractSubthemeIndex == option.index) {
                            onSettingsChange(
                                settings.copy(
                                    abstractSubthemeIndex = option.index,
                                    shaderRecolor = validColorway(settings.abstractShaderTheme, option.index, settings.shaderRecolor),
                                    customImageUri = null
                                )
                            )
                        }
                    }
                    StudioPicker.RECOLOR -> colorwaysFor(settings.abstractShaderTheme, settings.abstractSubthemeIndex).forEach { recolor ->
                        DirectChip(sceneColorwayFor(settings.abstractShaderTheme, settings.abstractSubthemeIndex, recolor).label, settings.shaderRecolor == recolor) {
                            onSettingsChange(settings.copy(shaderRecolor = recolor, customImageUri = null))
                        }
                    }
                    StudioPicker.MOTION -> BackgroundAnimation.values().forEach { motion ->
                        DirectChip(motion.label, settings.backgroundAnimation == motion) { onSettingsChange(settings.copy(backgroundAnimation = motion)) }
                    }
                    StudioPicker.FILTER -> ThemeFilter.values().forEach { filter ->
                        DirectChip(filter.label, settings.themeFilter == filter) { onSettingsChange(settings.copy(themeFilter = filter)) }
                    }
                    StudioPicker.STICK -> StickTheme.values().forEach { theme ->
                        DirectChip(theme.label, settings.stickTheme == theme) { onSettingsChange(settings.copy(stickTheme = theme)) }
                    }
                }
            }
            if (picker == StudioPicker.RECOLOR && settings.shaderRecolor == com.minimate.touchpad.model.ShaderRecolor.CUSTOM) {
                CustomPaletteEditor(settings.customShaderColors, customRole, { customRole = it }) { colors ->
                    onSettingsChange(settings.copy(customShaderColors = colors))
                }
            }
        }
    }
}

@Composable
private fun CustomPaletteEditor(colors: List<Long>, selected: Int, onSelect: (Int) -> Unit, onChange: (List<Long>) -> Unit) {
    val safe = if (colors.size == 4) colors else com.minimate.touchpad.model.DEFAULT_CUSTOM_SHADER_COLORS
    val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(safe[selected].toInt(), it) }
    fun update(component: Int, value: Float) {
        val nextHsv = hsv.copyOf().also { it[component] = value }
        val next = safe.toMutableList()
        next[selected] = android.graphics.Color.HSVToColor(nextHsv).toLong() and 0xFFFFFFFFL
        onChange(next)
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("Background", "Primary", "Secondary", "Highlight").forEachIndexed { index, label ->
                Box(Modifier.clip(CircleShape).background(Color(safe[index])).border(if (selected == index) 2.dp else 1.dp, Color.White, CircleShape).clickable { onSelect(index) }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(label, color = if (Color(safe[index]).luminance() > .55f) Color.Black else Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        CompactColorSlider("Hue", hsv[0], 0f..360f) { update(0, it) }
        CompactColorSlider("Saturation", hsv[1], 0f..1f) { update(1, it) }
        CompactColorSlider("Light", hsv[2], 0.02f..1f) { update(2, it) }
    }
}

@Composable
private fun CompactColorSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(.72f), fontSize = 8.sp, modifier = Modifier.width(52.dp))
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f).height(22.dp))
    }
}

@Composable
private fun DirectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape).background(if (selected) Color.White else Color(0xFF202124))
            .border(1.dp, if (selected) Color.White else Color(0x28FFFFFF), CircleShape)
            .clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 6.dp)
    ) { Text(label, color = if (selected) Color(0xFF111214) else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
}

@Composable
private fun EdgeAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(color).border(1.dp, Color(0x33FFFFFF), CircleShape).clickable(onClick = onClick).padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White)
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
