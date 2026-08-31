package com.minimate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.touchpad.model.TouchpadSettings
import kotlin.math.roundToInt

@Composable
fun ScreenEditorOverlay(
    settings: TouchpadSettings,
    screenWidthPx: Float,
    screenHeightPx: Float,
    onSettingsChange: (TouchpadSettings) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .fillMaxSize()

    ) {
        // Drag Target 2: Clock Handle
        val clockWidth = (140f * settings.clockScale) * 2.7f
        val clockHeight = (38f * settings.clockScale) * 2.7f
        val clockX = (settings.clockPositionX * screenWidthPx - clockWidth / 2f).coerceIn(0f, screenWidthPx - clockWidth)
        val clockY = (settings.clockPositionY * screenHeightPx - clockHeight / 2f).coerceIn(0f, screenHeightPx - clockHeight)

        Box(
            modifier = Modifier
                .offset { IntOffset(clockX.roundToInt(), clockY.roundToInt()) }
                .size(width = (140f * settings.clockScale).dp, height = (38f * settings.clockScale).dp)
                .border(2.dp, Color.White.copy(.85f), RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newX = (clockX + dragAmount.x + clockWidth / 2f) / screenWidthPx
                            val newY = (clockY + dragAmount.y + clockHeight / 2f) / screenHeightPx
                            onSettingsChange(
                                settings.copy(
                                    clockPositionX = newX.coerceIn(0.05f, 0.95f),
                                    clockPositionY = newY.coerceIn(0.05f, 0.95f)
                                )
                            )
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Drag",
                color = Color.White.copy(.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // The camera cutout occupies the bottom-right corner of the cover display, so a
        // bottom-anchored panel sat directly underneath it. Anchored top-left instead, using the
        // same glass chrome as every other studio rather than a bespoke navy-and-cyan card.
        StudioPanel(
            onCancel = onClose,
            onDone = onClose,
            modifier = Modifier.align(Alignment.TopStart).padding(top = MONT_TOP_INSET)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    String.format("Size %.2fx", settings.clockScale),
                    color = Color.White.copy(.7f),
                    fontSize = 8.sp,
                    modifier = Modifier.width(58.dp)
                )
                MontSlider(
                    value = settings.clockScale,
                    range = 0.65f..1.60f,
                    onChange = { onSettingsChange(settings.copy(clockScale = it)) },
                    modifier = Modifier.weight(1f).height(22.dp)
                )
            }
            StudioChip("Reset position", false) {
                onSettingsChange(
                    settings.copy(
                        clockPositionX = 0.248f,
                        clockPositionY = 0.882f,
                        clockScale = 1.18f
                    )
                )
            }
        }
    }
}
