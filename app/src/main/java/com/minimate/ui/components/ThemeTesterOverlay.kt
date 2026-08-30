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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.touchpad.model.BackgroundAnimation
import com.minimate.touchpad.model.AbstractShaderTheme
import com.minimate.touchpad.model.ShaderRecolor
import com.minimate.touchpad.model.StickTheme
import com.minimate.touchpad.model.ThemeFilter
import com.minimate.touchpad.model.TouchpadSettings
import com.minimate.touchpad.model.colorwaysFor
import com.minimate.touchpad.model.sceneColorwayFor
import com.minimate.touchpad.model.subthemesFor
import com.minimate.touchpad.model.validColorway

private enum class StudioPicker(val label: String) {
    THEME("Theme"), FILTER("Filter"), SUBTHEME("Subtheme"), RECOLOR("Color"),
    MOTION("Motion"), TOUCH("Touch"), STICK("Stick")
}

/** A small, compact, corner-anchored picker — content-sized, never full-screen, so the live
 *  canvas underneath stays visible while browsing. */
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

    Box(modifier.fillMaxSize().navigationBarsPadding()) {
        Box(Modifier.fillMaxSize().pointerInteropFilter { onPreviewTouchEvent(it) })

        StudioPanel(
            title = "Theme Studio",
            subtitle = "${settings.abstractShaderTheme.label} · ${subtheme.label}",
            onCancel = onCancel,
            onDone = onKeep,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StudioPicker.entries.forEach { value -> StudioChip(value.label, picker == value) { picker = value } }
            }
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Color(0x4D000000)).padding(vertical = 5.dp)
            ) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    when (picker) {
                        StudioPicker.THEME -> AbstractShaderTheme.entries.forEach { theme ->
                            StudioChip(theme.label, settings.abstractShaderTheme == theme) {
                                onSettingsChange(
                                    settings.copy(
                                        abstractShaderTheme = theme,
                                        abstractSubthemeIndex = 0,
                                        shaderRecolor = colorwaysFor(theme, 0).first(),
                                        customImageUri = null
                                    )
                                )
                            }
                        }
                        StudioPicker.FILTER -> ThemeFilter.entries.forEach { filter ->
                            val selected = filter == ThemeFilter.NONE && settings.themeFilters.isEmpty() || filter in settings.themeFilters
                            StudioChip(filter.label, selected) {
                                onSettingsChange(
                                    settings.copy(
                                        themeFilters = if (filter == ThemeFilter.NONE) emptyList()
                                        else if (filter in settings.themeFilters) settings.themeFilters - filter
                                        else settings.themeFilters + filter
                                    )
                                )
                            }
                        }
                        StudioPicker.SUBTHEME -> subthemesFor(settings.abstractShaderTheme).forEach { option ->
                            StudioChip(option.label, settings.abstractSubthemeIndex == option.index) {
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
                            StudioChip(sceneColorwayFor(settings.abstractShaderTheme, settings.abstractSubthemeIndex, recolor).label, settings.shaderRecolor == recolor) {
                                onSettingsChange(settings.copy(shaderRecolor = recolor, customImageUri = null))
                            }
                        }
                        StudioPicker.MOTION -> BackgroundAnimation.entries.forEach { motion ->
                            StudioChip(motion.label, settings.backgroundAnimation == motion) { onSettingsChange(settings.copy(backgroundAnimation = motion)) }
                        }
                        StudioPicker.TOUCH -> {
                            StudioChip("Distortion on", settings.fingerEffectsEnabled) {
                                onSettingsChange(settings.copy(fingerEffectsEnabled = true))
                            }
                            StudioChip("Distortion off", !settings.fingerEffectsEnabled) {
                                onSettingsChange(settings.copy(fingerEffectsEnabled = false))
                            }
                        }
                        StudioPicker.STICK -> StickTheme.entries.forEach { theme ->
                            StudioChip(theme.label, settings.stickTheme == theme) { onSettingsChange(settings.copy(stickTheme = theme)) }
                        }
                    }
                }
            }
            if (picker == StudioPicker.RECOLOR && settings.shaderRecolor == ShaderRecolor.CUSTOM) {
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
