package com.minimate.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Bluetooth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.minimate.ui.theme.AccentPink
import com.minimate.ui.theme.AccentGold
import com.minimate.ui.theme.GlassSurface
import com.minimate.ui.theme.ObsidianSurface
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        scrimColor = Color(0xAA000000),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        // Use LazyColumn to prevent overlapping and allow smooth scrolling
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✨ Preferences", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // ========= THEMES =========
            item { SectionTitle("🎨 Background Themes") }

            // Palette selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val palettes = listOf(
                        ThemeVariant.VARIANT_A to "🌸 Soft",
                        ThemeVariant.VARIANT_B to "🔥 Warm",
                        ThemeVariant.VARIANT_C to "🌊 Cool"
                    )
                    palettes.forEach { (variant, label) ->
                        val sel = settings.themeVariant == variant
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) AccentPink else GlassSurface)
                                .clickable { onSettingsChange(settings.copy(themeVariant = variant)) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (sel) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }
            }

            // Theme list
            val themes = BackgroundTheme.values().filter { it != BackgroundTheme.CUSTOM_IMAGE }
            items(themes.toList()) { theme ->
                val sel = settings.backgroundTheme == theme && settings.customImageUri == null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) AccentPink.copy(alpha = 0.18f) else GlassSurface.copy(alpha = 0.5f))
                        .clickable { onSettingsChange(settings.copy(backgroundTheme = theme, customImageUri = null)) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(theme.iconEmoji, fontSize = 20.sp, modifier = Modifier.width(30.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(theme.displayName, color = if (sel) AccentPink else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(theme.description, color = TextTertiary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (sel) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = AccentPink, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Custom wallpaper
            item {
                val hasCustom = settings.backgroundTheme == BackgroundTheme.CUSTOM_IMAGE && settings.customImageUri != null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (hasCustom) AccentBlue.copy(alpha = 0.18f) else GlassSurface.copy(alpha = 0.5f))
                        .clickable { onPickCustomImage() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🖼️", fontSize = 20.sp, modifier = Modifier.width(30.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (hasCustom) "Custom Wallpaper (Active)" else "Choose Image or GIF…", color = if (hasCustom) AccentCyan else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Pick image or animated GIF from gallery", color = TextTertiary, fontSize = 10.sp)
                    }
                    Icon(Icons.Default.Image, contentDescription = null, tint = if (hasCustom) AccentCyan else TextTertiary, modifier = Modifier.size(16.dp))
                }
            }

            // ========= FINGER EFFECTS =========
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item { SectionTitle("🖐️ Finger Touch Effects") }

            item {
                SwitchItem(
                    title = "Finger Effects",
                    subtitle = "Live multi-touch visual FX",
                    checked = settings.fingerEffectsEnabled,
                    onChange = { onSettingsChange(settings.copy(fingerEffectsEnabled = it)) }
                )
            }

            if (settings.fingerEffectsEnabled) {
                items(FingerEffect.values().toList()) { effect ->
                    val sel = settings.fingerEffect == effect
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) AccentGold.copy(alpha = 0.18f) else GlassSurface.copy(alpha = 0.4f))
                            .clickable { onSettingsChange(settings.copy(fingerEffect = effect)) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(effect.iconEmoji, fontSize = 18.sp, modifier = Modifier.width(28.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(effect.displayName, color = if (sel) AccentGold else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(effect.description, color = TextTertiary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (sel) Icon(Icons.Default.Check, contentDescription = null, tint = AccentGold, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // ========= BALL TAP SHORTCUT =========
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item { SectionTitle("⚡ Ball Tap Shortcut") }
            item {
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
                                .background(if (sel) AccentBlue else GlassSurface)
                                .clickable { onSettingsChange(settings.copy(buttonPressAction = action)) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (sel) Color.White else TextSecondary, fontSize = 10.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                        }
                    }
                }
            }

            // ========= BLUETOOTH =========
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item { SectionTitle("📡 Bluetooth Hosts") }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentBlue)
                        .clickable { onPairNewDevice() }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pair New Device", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (bluetoothState.pairedHosts.isNotEmpty()) {
                items(bluetoothState.pairedHosts) { host ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(GlassSurface.copy(alpha = 0.5f))
                            .clickable { if (!host.isConnected) onConnectAddress(host.address) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (host.isConnected) Color(0xFF10B981) else Color(0xFF4B5563)))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(host.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(host.address, color = TextTertiary, fontSize = 9.sp)
                            }
                        }
                        if (host.isConnected) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x33EF4444)).clickable { onDisconnect() }.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) { Text("Disconnect", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                        } else {
                            Text("Connect", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ========= POINTER =========
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item { SectionTitle("🎯 Pointer & Tracking") }
            item { SliderItem("Tracking Speed", settings.trackingSpeed, 0.4f..2.5f) { onSettingsChange(settings.copy(trackingSpeed = it)) } }
            item { SliderItem("Acceleration", settings.acceleration, 0.5f..2.5f) { onSettingsChange(settings.copy(acceleration = it)) } }
            item { SwitchItem("Invert Cursor Y", "Reverses up/down pointer direction", settings.invertCursorY) { onSettingsChange(settings.copy(invertCursorY = it)) } }

            // ========= SCROLLING & GESTURES =========
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item { SectionTitle("📜 Scrolling & Gestures") }
            item { SliderItem("Scroll Speed", settings.scrollSpeed, 0.4f..2.5f) { onSettingsChange(settings.copy(scrollSpeed = it)) } }
            item { SwitchItem("Natural Scrolling", "Content moves with finger direction", settings.naturalScrolling) { onSettingsChange(settings.copy(naturalScrolling = it)) } }
            item { SwitchItem("Kinetic Momentum", "Fluid scroll deceleration", settings.momentumScrolling) { onSettingsChange(settings.copy(momentumScrolling = it)) } }
            item { SwitchItem("Tap to Click", "Single tap = primary click", settings.tapToClick) { onSettingsChange(settings.copy(tapToClick = it)) } }
            item { SwitchItem("Two-Finger Right Click", "Two-finger tap = secondary click", settings.twoFingerRightClick) { onSettingsChange(settings.copy(twoFingerRightClick = it)) } }
            item { SwitchItem("Double-Tap Drag", "Double tap + slide to drag", settings.doubleTapDrag) { onSettingsChange(settings.copy(doubleTapDrag = it)) } }

            // ========= HAPTICS =========
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item { SectionTitle("📳 Haptic Feedback") }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HapticIntensity.values().forEach { h ->
                        val sel = settings.hapticIntensity == h
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) AccentCyan else GlassSurface)
                                .clickable { onSettingsChange(settings.copy(hapticIntensity = h)) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(h.name.lowercase().replaceFirstChar { it.uppercase() }, color = if (sel) Color.Black else TextSecondary, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
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
                        .clip(RoundedCornerShape(10.dp))
                        .background(GlassSurface.copy(alpha = 0.4f))
                        .clickable { onSettingsChange(TouchpadSettings()) }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset All Defaults", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun SliderItem(title: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = TextPrimary, fontSize = 12.sp)
            Text(String.format("%.2fx", value), color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = AccentPink, activeTrackColor = AccentPink.copy(alpha = 0.6f), inactiveTrackColor = Color(0x20FFFFFF))
        )
    }
}

@Composable
private fun SwitchItem(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSecondary, fontSize = 10.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentPink, uncheckedThumbColor = TextSecondary, uncheckedTrackColor = Color(0x20FFFFFF))
        )
    }
}
