package com.minimate.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.bluetooth.BluetoothUiState
import com.minimate.touchpad.model.BackgroundTheme
import com.minimate.touchpad.model.ButtonPressAction
import com.minimate.touchpad.model.FingerEffect
import com.minimate.touchpad.model.HapticIntensity
import com.minimate.touchpad.model.ThemeVariant
import com.minimate.touchpad.model.TouchpadSettings
import com.minimate.ui.theme.AccentBlue
import com.minimate.ui.theme.AccentCyan
import com.minimate.ui.theme.AccentEmerald
import com.minimate.ui.theme.AccentGold
import com.minimate.ui.theme.AccentPink
import com.minimate.ui.theme.TextPrimary
import com.minimate.ui.theme.TextSecondary
import com.minimate.ui.theme.TextTertiary

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: TouchpadSettings,
    bluetoothState: BluetoothUiState,
    batteryPercentage: Int,
    onSettingsChange: (TouchpadSettings) -> Unit,
    onPickCustomImage: () -> Unit,
    onConnectAddress: (String) -> Unit,
    onDisconnect: () -> Unit,
    onPairNewDevice: () -> Unit,
    onRefreshDevices: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var previewEffect by remember { mutableStateOf<FingerEffect?>(null) }

    // Fully transparent sheet so live shader background is 100% visible beneath floating glass cells
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color(0x22000000), // Ultra sheer scrim
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Glass Card
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xD8141522))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✨", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Touchpad Preferences",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // ========= THEMES & LIVE PALETTES =========
                item {
                    FloatingSectionTitle("🎨 Live Background Themes")
                }

                // Subtheme 3 Palettes Selector
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val palettes = listOf(
                            ThemeVariant.VARIANT_A to "🌸 Soft Palette",
                            ThemeVariant.VARIANT_B to "🔥 Warm Palette",
                            ThemeVariant.VARIANT_C to "🌊 Cool Palette"
                        )
                        palettes.forEach { (variant, label) ->
                            val sel = settings.themeVariant == variant
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (sel) AccentPink else Color(0xCC181926))
                                    .border(
                                        1.dp,
                                        if (sel) Color.White.copy(alpha = 0.6f) else Color(0x22FFFFFF),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onSettingsChange(settings.copy(themeVariant = variant)) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = if (sel) Color.White else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 10 Themes List as Floating Glass Cells
                val themes = BackgroundTheme.values().filter { it != BackgroundTheme.CUSTOM_IMAGE }
                items(themes.toList()) { theme ->
                    val sel = settings.backgroundTheme == theme && settings.customImageUri == null
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (sel) Color(0xD9251833) else Color(0xBF141520))
                            .border(
                                1.5.dp,
                                if (sel) AccentPink else Color(0x1FFFFFFF),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSettingsChange(settings.copy(backgroundTheme = theme, customImageUri = null))
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(theme.iconEmoji, fontSize = 22.sp, modifier = Modifier.width(32.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                theme.displayName,
                                color = if (sel) AccentPink else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                theme.description,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (sel) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(AccentPink),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Custom wallpaper option
                item {
                    val hasCustom = settings.backgroundTheme == BackgroundTheme.CUSTOM_IMAGE && settings.customImageUri != null
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (hasCustom) Color(0xD910243E) else Color(0xBF141520))
                            .border(
                                1.5.dp,
                                if (hasCustom) AccentCyan else Color(0x1FFFFFFF),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onPickCustomImage() }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🖼️", fontSize = 22.sp, modifier = Modifier.width(32.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (hasCustom) "Custom Wallpaper (Active)" else "Pick Image or GIF…",
                                color = if (hasCustom) AccentCyan else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Pick animated GIF or photo from gallery",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            tint = if (hasCustom) AccentCyan else TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // ========= FINGER EFFECTS =========
                item { Spacer(modifier = Modifier.height(10.dp)) }
                item {
                    FloatingSectionTitle("🖐️ Interactive Touch FX (Live Preview)")
                }

                item {
                    FloatingCard {
                        SwitchItem(
                            title = "Finger Effects",
                            subtitle = "Live particle trails following touches",
                            checked = settings.fingerEffectsEnabled,
                            onChange = { onSettingsChange(settings.copy(fingerEffectsEnabled = it)) }
                        )
                    }
                }

                if (settings.fingerEffectsEnabled) {
                    items(FingerEffect.values().toList()) { effect ->
                        val sel = settings.fingerEffect == effect
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (sel) Color(0xD92E2416) else Color(0xBF141520))
                                .border(
                                    1.5.dp,
                                    if (sel) AccentGold else Color(0x1FFFFFFF),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    previewEffect = effect
                                    onSettingsChange(settings.copy(fingerEffect = effect))
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(effect.iconEmoji, fontSize = 20.sp, modifier = Modifier.width(30.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    effect.displayName,
                                    color = if (sel) AccentGold else TextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    effect.description,
                                    color = TextSecondary,
                                    fontSize = 9.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (sel) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(AccentGold),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ========= BALL TAP SHORTCUT =========
                item { Spacer(modifier = Modifier.height(10.dp)) }
                item { FloatingSectionTitle("⚡ Ball Tap Shortcut") }
                item {
                    FloatingCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val actions = listOf(
                                ButtonPressAction.STEALTH_DIM to "🌙 Dim",
                                ButtonPressAction.OPEN_SETTINGS to "⚙️ Settings",
                                ButtonPressAction.PAIRING_MODE to "📡 Pair",
                                ButtonPressAction.MIDDLE_CLICK to "🖱️ Click"
                            )
                            actions.forEach { (action, label) ->
                                val sel = settings.buttonPressAction == action
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (sel) AccentBlue else Color(0x33FFFFFF))
                                        .clickable { onSettingsChange(settings.copy(buttonPressAction = action)) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        color = if (sel) Color.White else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // ========= BLUETOOTH HUB BUTTON =========
                item { Spacer(modifier = Modifier.height(10.dp)) }
                item { FloatingSectionTitle("📡 Wireless Bluetooth") }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(AccentCyan, AccentPink)
                                )
                            )
                            .clickable { onPairNewDevice() }
                            .padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.BluetoothSearching,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Open Pairing & Host Manager",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // ========= POINTER & TRACKING =========
                item { Spacer(modifier = Modifier.height(10.dp)) }
                item { FloatingSectionTitle("🎯 Pointer & Tracking Speed") }
                item {
                    FloatingCard {
                        SliderItem("Tracking Speed", settings.trackingSpeed, 0.4f..2.5f) {
                            onSettingsChange(settings.copy(trackingSpeed = it))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        SliderItem("Acceleration", settings.acceleration, 0.5f..2.5f) {
                            onSettingsChange(settings.copy(acceleration = it))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        SwitchItem(
                            "Invert Cursor Y",
                            "Reverses up/down pointer direction",
                            settings.invertCursorY
                        ) {
                            onSettingsChange(settings.copy(invertCursorY = it))
                        }
                    }
                }

                // ========= SCROLLING & GESTURES =========
                item { Spacer(modifier = Modifier.height(10.dp)) }
                item { FloatingSectionTitle("📜 Scrolling & Gestures") }
                item {
                    FloatingCard {
                        SliderItem("Scroll Speed", settings.scrollSpeed, 0.4f..2.5f) {
                            onSettingsChange(settings.copy(scrollSpeed = it))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        SwitchItem(
                            "Natural Scrolling",
                            "Content moves with finger direction",
                            settings.naturalScrolling
                        ) {
                            onSettingsChange(settings.copy(naturalScrolling = it))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        SwitchItem(
                            "Kinetic Momentum",
                            "Fluid scrolling inertia",
                            settings.momentumScrolling
                        ) {
                            onSettingsChange(settings.copy(momentumScrolling = it))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        SwitchItem(
                            "Tap to Click",
                            "Single tap = primary click",
                            settings.tapToClick
                        ) {
                            onSettingsChange(settings.copy(tapToClick = it))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        SwitchItem(
                            "Two-Finger Right Click",
                            "Two-finger tap = secondary click",
                            settings.twoFingerRightClick
                        ) {
                            onSettingsChange(settings.copy(twoFingerRightClick = it))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        SwitchItem(
                            "Double-Tap Drag",
                            "Double tap + slide to drag windows",
                            settings.doubleTapDrag
                        ) {
                            onSettingsChange(settings.copy(doubleTapDrag = it))
                        }
                    }
                }

                // ========= HAPTICS =========
                item { Spacer(modifier = Modifier.height(10.dp)) }
                item { FloatingSectionTitle("📳 Haptic Tactile Feedback") }
                item {
                    FloatingCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HapticIntensity.values().forEach { h ->
                                val sel = settings.hapticIntensity == h
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (sel) AccentCyan else Color(0x22FFFFFF))
                                        .clickable { onSettingsChange(settings.copy(hapticIntensity = h)) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        h.name.lowercase().replaceFirstChar { it.uppercase() },
                                        color = if (sel) Color.Black else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // ========= RESET =========
                item { Spacer(modifier = Modifier.height(14.dp)) }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x881E1F2C))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                            .clickable { onSettingsChange(TouchpadSettings()) }
                            .padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Reset All Defaults",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Top-Right Live Mini Preview Popup for Finger Effects
            (previewEffect ?: if (settings.fingerEffectsEnabled) settings.fingerEffect else null)?.let { effect ->
                FingerEffectPreviewPopup(
                    effect = effect,
                    visible = true,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun FloatingSectionTitle(title: String) {
    Text(
        title,
        color = Color.White,
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun FloatingCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xD9151622))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column { content() }
    }
}

@Composable
private fun SliderItem(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(
                String.format("%.2fx", value),
                color = AccentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = AccentPink,
                activeTrackColor = AccentPink,
                inactiveTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}

@Composable
private fun SwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSecondary, fontSize = 9.5.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentPink,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}
