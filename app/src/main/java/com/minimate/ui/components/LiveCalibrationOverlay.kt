package com.minimate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.touchpad.model.TouchpadSettings
import java.util.Locale

enum class LiveCalibrationMode { TRACKPAD }

/** Compact live controls that leave the trackpad surface active for immediate testing. */
@Composable
fun LiveCalibrationOverlay(
    mode: LiveCalibrationMode,
    settings: TouchpadSettings,
    onSettingsChange: (TouchpadSettings) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.navigationBarsPadding().padding(start = 10.dp, bottom = 10.dp)) {
        Box(
            Modifier.matchParentSize().blur(22.dp).clip(RoundedCornerShape(24.dp))
                .background(Color(0x33111111))
        )
        Column(
            Modifier.width(176.dp)
                .shadow(22.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xD91D1D1F), Color(0xC9121214), Color(0xE50A0A0C))
                    )
                )
                .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(24.dp)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("LIVE TRACKPAD", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text("Test anywhere outside this glass", color = Color(0xFFAAB8C8), fontSize = 7.5.sp)
                }
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(Color(0x22FFFFFF))
                        .border(1.dp, Color(0x44FFFFFF), CircleShape).clickable(onClick = onDone),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Check, "Done", tint = Color.White, modifier = Modifier.size(15.dp)) }
            }

            StepControl("Speed", settings.trackingSpeed, .4f, 2.5f, .1f) {
                onSettingsChange(settings.copy(trackingSpeed = it))
            }
            StepControl("Acceleration", settings.acceleration, .5f, 2.5f, .1f) {
                onSettingsChange(settings.copy(acceleration = it))
            }
            StepControl("Two-finger scroll", settings.scrollSpeed, .1f, 2f, .05f) {
                onSettingsChange(settings.copy(scrollSpeed = it))
            }
        }
    }
}

@Composable
private fun StepControl(label: String, value: Float, min: Float, max: Float, step: Float, onChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = Color(0xFFAAB8C8), fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArrowButton(false) { onChange((value - step).coerceAtLeast(min)) }
            Text(String.format(Locale.US, "%.2f", value), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            ArrowButton(true) { onChange((value + step).coerceAtMost(max)) }
        }
    }
}

@Composable
private fun ArrowButton(right: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(28.dp).clip(CircleShape).background(Color(0x18FFFFFF))
            .border(1.dp, Color(0x35FFFFFF), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(if (right) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Color.White, modifier = Modifier.size(17.dp))
    }
}
