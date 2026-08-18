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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.minimate.ui.theme.AccentPurple
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
        scrimColor = Color(0x99000000),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Preferences",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ==========================================
            // 1. 10 THEMES & 3 SUBTHEME COLOR VARIANTS
            // ==========================================
            SectionHeader(title = "10 THEMES & COLOR PALETTES", icon = Icons.Default.Palette)

            // 3 Subtheme / Color Variant Selector Pills (Applies to active theme)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ThemeVariant.values().forEach { v ->
                    val isVarSelected = settings.themeVariant == v
                    val varTitle = when (v) {
                        ThemeVariant.VARIANT_A -> "Palette 1 (Cyan/Violet)"
                        ThemeVariant.VARIANT_B -> "Palette 2 (Gold/Red)"
                        ThemeVariant.VARIANT_C -> "Palette 3 (Emerald/Blue)"
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isVarSelected) AccentCyan else Color(0x14FFFFFF))
                            .clickable { onSettingsChange(settings.copy(themeVariant = v)) }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (v) {
                                ThemeVariant.VARIANT_A -> "Palette 1"
                                ThemeVariant.VARIANT_B -> "Palette 2"
                                ThemeVariant.VARIANT_C -> "Palette 3"
                            },
                            color = if (isVarSelected) Color.Black else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isVarSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // The 10 Themes List
            val themes = BackgroundTheme.values().filter { it != BackgroundTheme.CUSTOM_IMAGE }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                themes.forEach { theme ->
                    val isSelected = settings.backgroundTheme == theme && settings.customImageUri == null
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AccentPurple.copy(alpha = 0.25f) else Color(0x0AFFFFFF))
                            .clickable {
                                onSettingsChange(settings.copy(backgroundTheme = theme, customImageUri = null))
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = theme.displayName, color = if (isSelected) AccentCyan else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = theme.description, color = TextSecondary, fontSize = 10.sp)
                        }
                        Icon(
                            imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) AccentCyan else TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Custom Wallpaper / GIF Option
                val hasCustomImage = settings.backgroundTheme == BackgroundTheme.CUSTOM_IMAGE && settings.customImageUri != null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (hasCustomImage) AccentBlue.copy(alpha = 0.25f) else Color(0x0AFFFFFF))
                        .clickable { onPickCustomImage() }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (hasCustomImage) "Custom Wallpaper / GIF (Active)" else "Choose Custom Wallpaper / GIF…",
                                color = if (hasCustomImage) AccentCyan else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(text = "Pick image or animated GIF from device gallery", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 2. 10 FINGER EFFECTS
            // ==========================================
            SectionHeader(title = "10 FINGER EFFECTS", icon = Icons.Default.TouchApp)

            SwitchSettingItem(
                title = "Finger Effects Enabled",
                subtitle = "Render live multi-touch visual FX under active fingers",
                checked = settings.fingerEffectsEnabled,
                onCheckedChange = { onSettingsChange(settings.copy(fingerEffectsEnabled = it)) }
            )

            if (settings.fingerEffectsEnabled) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FingerEffect.values().forEach { effect ->
                        val isEffSelected = settings.fingerEffect == effect
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isEffSelected) AccentBlue.copy(alpha = 0.22f) else Color(0x08FFFFFF))
                            .clickable { onSettingsChange(settings.copy(fingerEffect = effect)) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = effect.displayName, color = if (isEffSelected) AccentCyan else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = effect.description, color = TextSecondary, fontSize = 9.sp)
                        }
                        Icon(
                            imageVector = if (isEffSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isEffSelected) AccentCyan else TextTertiary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 3. BALL TAP SHORTCUT
            // ==========================================
            SectionHeader(title = "BALL TAP SHORTCUT", icon = Icons.Default.TouchApp)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ButtonPressAction.values().forEach { action ->
                    val isSelected = settings.buttonPressAction == action
                    val label = when (action) {
                        ButtonPressAction.STEALTH_DIM -> "Dim Screen"
                        ButtonPressAction.OPEN_SETTINGS -> "Settings"
                        ButtonPressAction.PAIRING_MODE -> "Pair Mode"
                        ButtonPressAction.MIDDLE_CLICK -> "Mid Click"
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AccentBlue else Color(0x10FFFFFF))
                            .clickable { onSettingsChange(settings.copy(buttonPressAction = action)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 4. BLUETOOTH PAIRING & HOSTS
            // ==========================================
            SectionHeader(title = "BLUETOOTH HOSTS", icon = Icons.Default.Bluetooth)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentBlue)
                    .clickable { onPairNewDevice() }
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pair New Device (Discoverable)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val paired = bluetoothState.pairedHosts
            if (paired.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x08FFFFFF))
                ) {
                    paired.forEachIndexed { index, host ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (!host.isConnected) onConnectAddress(host.address) }
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = host.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(text = host.address, color = TextTertiary, fontSize = 9.sp)
                            }
                            if (host.isConnected) {
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x33EF4444)).clickable { onDisconnect() }.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Disconnect", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Text("Connect", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (index < paired.lastIndex) {
                            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0x10FFFFFF)))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 5. POINTER & ACCELERATION
            // ==========================================
            SectionHeader(title = "POINTER & TRACKING", icon = Icons.Default.Speed)

            SliderSettingItem(
                title = "Tracking Speed",
                value = settings.trackingSpeed,
                valueRange = 0.4f..2.5f,
                displayValue = String.format("%.2fx", settings.trackingSpeed),
                onValueChange = { onSettingsChange(settings.copy(trackingSpeed = it)) }
            )

            SliderSettingItem(
                title = "Acceleration Power",
                value = settings.acceleration,
                valueRange = 0.5f..2.5f,
                displayValue = String.format("%.2fx", settings.acceleration),
                onValueChange = { onSettingsChange(settings.copy(acceleration = it)) }
            )

            SwitchSettingItem(
                title = "Invert Cursor Y",
                subtitle = "Reverses up and down pointer direction",
                checked = settings.invertCursorY,
                onCheckedChange = { onSettingsChange(settings.copy(invertCursorY = it)) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 6. SCROLLING & GESTURES
            // ==========================================
            SectionHeader(title = "SCROLLING & GESTURES", icon = Icons.Default.TouchApp)

            SliderSettingItem(
                title = "Scroll Sensitivity",
                value = settings.scrollSpeed,
                valueRange = 0.4f..2.5f,
                displayValue = String.format("%.2fx", settings.scrollSpeed),
                onValueChange = { onSettingsChange(settings.copy(scrollSpeed = it)) }
            )

            SwitchSettingItem(
                title = "Natural Scrolling",
                subtitle = "Two-finger content scrolling matches finger direction",
                checked = settings.naturalScrolling,
                onCheckedChange = { onSettingsChange(settings.copy(naturalScrolling = it)) }
            )

            SwitchSettingItem(
                title = "Kinetic Momentum",
                subtitle = "Fluid deceleration when releasing scroll",
                checked = settings.momentumScrolling,
                onCheckedChange = { onSettingsChange(settings.copy(momentumScrolling = it)) }
            )

            SwitchSettingItem(
                title = "Tap to Click",
                subtitle = "Single tap registers as primary click",
                checked = settings.tapToClick,
                onCheckedChange = { onSettingsChange(settings.copy(tapToClick = it)) }
            )

            SwitchSettingItem(
                title = "Two-Finger Right Click",
                subtitle = "Two-finger tap registers as secondary click",
                checked = settings.twoFingerRightClick,
                onCheckedChange = { onSettingsChange(settings.copy(twoFingerRightClick = it)) }
            )

            SwitchSettingItem(
                title = "Double-Tap Drag",
                subtitle = "Double tap and slide to drag items",
                checked = settings.doubleTapDrag,
                onCheckedChange = { onSettingsChange(settings.copy(doubleTapDrag = it)) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 7. HAPTICS
            // ==========================================
            SectionHeader(title = "TACTILE FEEDBACK", icon = Icons.Default.Vibration)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HapticIntensity.values().forEach { intensity ->
                    val isSelected = settings.hapticIntensity == intensity
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AccentBlue else Color(0x10FFFFFF))
                            .clickable { onSettingsChange(settings.copy(hapticIntensity = intensity)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = intensity.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Reset to defaults
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x0AFFFFFF))
                    .clickable { onSettingsChange(TouchpadSettings()) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset All to Defaults", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(title, color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
    }
}

@Composable
private fun SliderSettingItem(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = TextPrimary, fontSize = 12.sp)
            Text(displayValue, color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = AccentCyan,
                activeTrackColor = AccentBlue,
                inactiveTrackColor = Color(0x20FFFFFF)
            )
        )
    }
}

@Composable
private fun SwitchSettingItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSecondary, fontSize = 10.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentBlue,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = Color(0x20FFFFFF)
            )
        )
    }
}
