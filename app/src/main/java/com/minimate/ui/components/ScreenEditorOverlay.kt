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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.minimate.ui.theme.AccentCyan
import com.minimate.ui.theme.AccentEmerald
import com.minimate.ui.theme.AccentPink
import com.minimate.ui.theme.TextPrimary
import com.minimate.ui.theme.TextSecondary
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
            .background(Color.Black.copy(alpha = 0.55f))
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
                .border(2.dp, AccentCyan, RoundedCornerShape(16.dp))
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
                "Drag Clock",
                color = AccentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Bottom Screen Editor Control Panel
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
                .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = AccentCyan)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xF0141624))
                .border(1.5.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Screen Layout Editor",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Reset layout button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x33FFFFFF))
                                .clickable {
                                    onSettingsChange(
                                        settings.copy(
                                            clockPositionX = 0.248f,
                                            clockPositionY = 0.882f,
                                            clockScale = 1.18f
                                        )
                                    )
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text("Reset", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // Done button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(AccentEmerald)
                                .clickable { onClose() }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("Done", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(String.format("Scale (%.2fx)", settings.clockScale), color = TextPrimary, fontSize = 11.sp)
                }
                Slider(
                    value = settings.clockScale,
                    onValueChange = { onSettingsChange(settings.copy(clockScale = it)) },
                    valueRange = 0.65f..1.60f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = Color(0x33FFFFFF)
                    )
                )
            }
        }
    }
}
