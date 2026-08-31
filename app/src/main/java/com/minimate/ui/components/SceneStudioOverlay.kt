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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.touchpad.model.BackgroundAnimation
import com.minimate.touchpad.model.ShaderFamily
import com.minimate.touchpad.model.ThemeFilter
import com.minimate.touchpad.model.TouchpadSettings
import com.minimate.touchpad.model.sceneById
import com.minimate.touchpad.model.scenesInFamily

private enum class StudioTab(val label: String) {
    SCENE("Scene"), TUNE("Tune"), COLOUR("Colour"), FILTER("Filter"), MOTION("Motion")
}

/**
 * Scene studio.
 *
 * The panel stays small and corner-anchored so the live canvas underneath remains visible while
 * browsing — the point is to judge a scene as it actually looks, not to read a menu. Tune is built
 * from whatever the selected scene declares, so it shows fall speed and trail for rain and seed
 * and detail for a fractal, instead of the same three generic knobs for everything.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SceneStudioOverlay(
    settings: TouchpadSettings,
    onSettingsChange: (TouchpadSettings) -> Unit,
    onPreviewTouchEvent: (MotionEvent) -> Boolean,
    onKeep: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onCancel)
    var tab by remember { mutableStateOf(StudioTab.SCENE) }
    var family by remember { mutableStateOf(sceneById(settings.shaderSceneId).family) }
    val scene = sceneById(settings.shaderSceneId)
    val values = settings.shaderParams.takeIf { it.size == scene.params.size } ?: scene.defaults

    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().pointerInteropFilter { onPreviewTouchEvent(it) })

        StudioPanel(
            title = scene.label,
            subtitle = scene.blurb,
            onCancel = onCancel,
            onDone = onKeep,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StudioTab.entries.forEach { value ->
                    StudioChip(value.label, tab == value) { tab = value }
                }
            }

            when (tab) {
                StudioTab.SCENE -> {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ShaderFamily.entries.forEach { value ->
                            StudioChip(value.label, family == value) { family = value }
                        }
                    }
                    PanelRow {
                        scenesInFamily(family).forEach { option ->
                            StudioChip(option.label, option.id == scene.id) {
                                // Switching scene resets the controls: the previous scene's
                                // values mean nothing here, and carrying them over silently
                                // produces a scene that looks broken on arrival.
                                onSettingsChange(
                                    settings.copy(
                                        shaderSceneId = option.id,
                                        shaderParams = option.defaults,
                                        shaderPaletteIndex = 0
                                    )
                                )
                            }
                        }
                    }
                }

                StudioTab.TUNE -> {
                    if (scene.params.isEmpty()) {
                        Text("This scene has no controls.", color = Color.White.copy(.5f), fontSize = 8.sp)
                    } else {
                        scene.params.forEachIndexed { index, param ->
                            val value = values.getOrElse(index) { param.default }
                            Row(
                                Modifier.fillMaxWidth().height(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    param.label,
                                    color = Color.White.copy(.7f),
                                    fontSize = 8.sp,
                                    modifier = Modifier.width(62.dp)
                                )
                                Slider(
                                    value = value.coerceIn(param.min, param.max),
                                    onValueChange = { next ->
                                        val updated = values.toMutableList()
                                        while (updated.size < scene.params.size) updated += 0f
                                        updated[index] = next
                                        onSettingsChange(settings.copy(shaderParams = updated))
                                    },
                                    valueRange = param.min..param.max,
                                    modifier = Modifier.weight(1f).height(22.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color.White.copy(.14f)
                                    )
                                )
                            }
                        }
                        StudioChip("Reset", false) {
                            onSettingsChange(settings.copy(shaderParams = scene.defaults))
                        }
                    }
                }

                StudioTab.COLOUR -> PanelRow {
                    scene.palettes.forEachIndexed { index, palette ->
                        PaletteChip(palette.stops, index == settings.shaderPaletteIndex) {
                            onSettingsChange(settings.copy(shaderPaletteIndex = index))
                        }
                    }
                }

                StudioTab.FILTER -> PanelRow {
                    ThemeFilter.entries.forEach { filter ->
                        val selected = if (filter == ThemeFilter.NONE) settings.themeFilters.isEmpty()
                        else filter in settings.themeFilters
                        StudioChip(filter.label, selected) {
                            onSettingsChange(
                                settings.copy(
                                    themeFilters = when {
                                        filter == ThemeFilter.NONE -> emptyList()
                                        filter in settings.themeFilters -> settings.themeFilters - filter
                                        else -> settings.themeFilters + filter
                                    }
                                )
                            )
                        }
                    }
                }

                StudioTab.MOTION -> {
                    PanelRow {
                        BackgroundAnimation.entries.forEach { motion ->
                            StudioChip(motion.label, settings.backgroundAnimation == motion) {
                                onSettingsChange(settings.copy(backgroundAnimation = motion))
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().height(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Touch", color = Color.White.copy(.7f), fontSize = 8.sp, modifier = Modifier.width(62.dp))
                        Slider(
                            value = settings.shaderTouchStrength.coerceIn(0f, 2f),
                            onValueChange = { onSettingsChange(settings.copy(shaderTouchStrength = it)) },
                            valueRange = 0f..2f,
                            modifier = Modifier.weight(1f).height(22.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(.14f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelRow(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color(0x4D000000)).padding(vertical = 5.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) { content() }
    }
}

/** Shows the palette itself rather than its name — the colours are the decision being made. */
@Composable
private fun PaletteChip(stops: List<Long>, selected: Boolean, onClick: () -> Unit) {
    val colors = (if (stops.size >= 4) stops else List(4) { 0xFF202020L }).map { Color(it) }
    Box(
        Modifier
            .width(46.dp)
            .height(20.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Brush.horizontalGradient(colors))
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) Color.White else Color.White.copy(.22f),
                RoundedCornerShape(7.dp)
            )
            .clickable(onClick = onClick)
    )
}
