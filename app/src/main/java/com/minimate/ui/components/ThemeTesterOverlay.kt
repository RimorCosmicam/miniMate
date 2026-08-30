package com.minimate.ui.components

import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.touchpad.model.AbstractSubtheme
import com.minimate.touchpad.model.BackgroundAnimation
import com.minimate.touchpad.model.AbstractShaderTheme
import com.minimate.touchpad.model.SceneColorway
import com.minimate.touchpad.model.ShaderRecolor
import com.minimate.touchpad.model.StickTheme
import com.minimate.touchpad.model.ThemeFilter
import com.minimate.touchpad.model.TouchpadSettings
import com.minimate.touchpad.model.colorwaysFor
import com.minimate.touchpad.model.sceneColorwayFor
import com.minimate.touchpad.model.subthemesFor
import com.minimate.touchpad.model.validColorway

private enum class StudioPicker(val label: String) {
    SCENE("Scene"), FILTERS("Filters"), MORE("More")
}

/** A bottom-sheet Theme Studio: browsable scene grid, a real filter grid, live above it all. */
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
    var picker by remember { mutableStateOf(StudioPicker.SCENE) }
    var customRole by remember { mutableStateOf(0) }
    val subtheme = subthemesFor(settings.abstractShaderTheme)
        .getOrElse(settings.abstractSubthemeIndex) { subthemesFor(settings.abstractShaderTheme).first() }

    Box(modifier.fillMaxSize().navigationBarsPadding()) {
        Box(Modifier.fillMaxSize().pointerInteropFilter { onPreviewTouchEvent(it) })

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                // The cover display's camera housing measures ~84dp tall in its bottom-right
                // corner (220px at 2.625x density). Clearing the whole bottom edge by more than
                // that — not just the right side — keeps the sheet's rounded box and everything
                // in it off the housing entirely, regardless of horizontal position.
                .padding(bottom = 96.dp)
                .fillMaxHeight(.66f)
                .shadow(24.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), spotColor = Color.Black)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xED1B1C1E), Color(0xF80B0C0E))))
                .border(1.dp, Color.White.copy(.16f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("THEME STUDIO", color = Color.White.copy(.55f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(
                        "${settings.abstractShaderTheme.label} · ${subtheme.label}",
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                EdgeAction(Icons.Default.Close, "", Color(0x33FFFFFF), onCancel)
                Spacer(Modifier.width(6.dp))
                EdgeAction(Icons.Default.Check, "", Color.White, onKeep)
            }

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x40000000)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StudioPicker.entries.forEach { value -> StudioChip(value.label, picker == value, Modifier.weight(1f)) { picker = value } }
            }

            Box(Modifier.weight(1f)) {
                when (picker) {
                    StudioPicker.SCENE -> ScenePicker(settings, onSettingsChange)
                    StudioPicker.FILTERS -> FilterGrid(settings, onSettingsChange)
                    StudioPicker.MORE -> MorePicker(settings, onSettingsChange)
                }
            }

            if (picker == StudioPicker.SCENE && settings.shaderRecolor == ShaderRecolor.CUSTOM) {
                CustomPaletteEditor(settings.customShaderColors, customRole, { customRole = it }) { colors ->
                    onSettingsChange(settings.copy(customShaderColors = colors))
                }
            }
        }
    }
}

@Composable
private fun ScenePicker(settings: TouchpadSettings, onSettingsChange: (TouchpadSettings) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AbstractShaderTheme.entries.forEach { theme ->
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
        }

        val subthemes = subthemesFor(settings.abstractShaderTheme)
        subthemes.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    SceneCard(
                        option = option,
                        selected = settings.abstractSubthemeIndex == option.index,
                        modifier = Modifier.weight(1f)
                    ) {
                        onSettingsChange(
                            settings.copy(
                                abstractSubthemeIndex = option.index,
                                shaderRecolor = validColorway(settings.abstractShaderTheme, option.index, settings.shaderRecolor),
                                customImageUri = null
                            )
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Text("COLORWAY", color = Color.White.copy(.45f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            colorwaysFor(settings.abstractShaderTheme, settings.abstractSubthemeIndex).forEach { recolor ->
                val colorway = sceneColorwayFor(settings.abstractShaderTheme, settings.abstractSubthemeIndex, recolor)
                ColorwaySwatch(colorway, settings.shaderRecolor == recolor) {
                    onSettingsChange(settings.copy(shaderRecolor = recolor, customImageUri = null))
                }
            }
        }
    }
}

@Composable
private fun SceneCard(option: AbstractSubtheme, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val stops = option.colors
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color.White.copy(.14f) else Color(0x40000000))
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Color.White.copy(.7f) else Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(9.dp))
                .background(Brush.linearGradient(listOf(Color(stops.getOrElse(1) { stops.first() }), Color(stops.getOrElse(2) { stops.last() }))))
        )
        Text(option.label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ColorwaySwatch(colorway: SceneColorway, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(11.dp))
            .background(if (selected) Color.White.copy(.16f) else Color.Transparent)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Color.White.copy(.7f) else Color(0x22FFFFFF), RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row {
            colorway.stops.take(4).forEach { stop ->
                Box(Modifier.size(14.dp).background(Color(stop)))
            }
        }
        Text(colorway.label, color = Color.White.copy(.8f), fontSize = 7.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FilterGrid(settings: TouchpadSettings, onSettingsChange: (TouchpadSettings) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (settings.themeFilters.isEmpty()) "No filters — tap any to stack up to 4" else "${settings.themeFilters.size} active · tap to toggle",
            color = Color.White.copy(.5f), fontSize = 9.sp
        )
        ThemeFilter.entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { filter ->
                    val selected = filter == ThemeFilter.NONE && settings.themeFilters.isEmpty() || filter in settings.themeFilters
                    FilterCard(filter.label, selected, Modifier.weight(1f)) {
                        onSettingsChange(
                            settings.copy(
                                themeFilters = if (filter == ThemeFilter.NONE) emptyList()
                                else if (filter in settings.themeFilters) settings.themeFilters - filter
                                else settings.themeFilters + filter
                            )
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun FilterCard(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color.White else Color(0x40000000))
            .border(1.dp, if (selected) Color.White else Color(0x22FFFFFF), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, color = if (selected) Color(0xFF111214) else Color.White.copy(.82f),
            fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MorePicker(settings: TouchpadSettings, onSettingsChange: (TouchpadSettings) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("MOTION", color = Color.White.copy(.45f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BackgroundAnimation.entries.forEach { motion ->
                    StudioChip(motion.label, settings.backgroundAnimation == motion) { onSettingsChange(settings.copy(backgroundAnimation = motion)) }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("TOUCH REACTION", color = Color.White.copy(.45f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StudioChip("On", settings.fingerEffectsEnabled) { onSettingsChange(settings.copy(fingerEffectsEnabled = true)) }
                StudioChip("Off", !settings.fingerEffectsEnabled) { onSettingsChange(settings.copy(fingerEffectsEnabled = false)) }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("ANALOG STICK", color = Color.White.copy(.45f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StickTheme.entries.forEach { theme ->
                    StudioChip(theme.label, settings.stickTheme == theme) { onSettingsChange(settings.copy(stickTheme = theme)) }
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
private fun EdgeAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(color).border(1.dp, Color(0x2AFFFFFF), CircleShape).clickable(onClick = onClick).padding(horizontal = if (label.isEmpty()) 8.dp else 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (color == Color.White) Color(0xFF111214) else Color.White)
        if (label.isNotEmpty()) {
            Spacer(Modifier.width(4.dp))
            Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}
