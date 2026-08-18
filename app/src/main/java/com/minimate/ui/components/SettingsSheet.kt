package com.minimate.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.Waves
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.bluetooth.BluetoothUiState
import com.minimate.touchpad.model.BackgroundTheme
import com.minimate.touchpad.model.ButtonPressAction
import com.minimate.touchpad.model.ClockPosition
import com.minimate.touchpad.model.ClockStyle
import com.minimate.touchpad.model.FingerEffect
import com.minimate.touchpad.model.HapticIntensity
import com.minimate.touchpad.model.ThemePreset
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
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Settings, 1: Theme, 2: Finger Shader
    var previewEffect by remember { mutableStateOf<FingerEffect?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color(0x20000000), // Transparent glass scrim
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(top = 14.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Glass Bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xD8141522))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Control Center",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // 3 Modern Tabs: Settings, Theme, Finger Shader
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xEE161726))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tabs = listOf(
                            Triple(0, "Settings", Icons.Default.Tune),
                            Triple(1, "Theme", Icons.Default.Palette),
                            Triple(2, "Finger Shader", Icons.Default.TouchApp)
                        )
                        tabs.forEach { (index, title, icon) ->
                            val isSel = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) AccentPink else Color.Transparent)
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = title,
                                        tint = if (isSel) Color.White else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = title,
                                        color = if (isSel) Color.White else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // ==================== TAB 0: SETTINGS ====================
                if (selectedTab == 0) {
                    // Ball Tap Shortcut Configuration
                    item {
                        FloatingSectionTitle("Liquid Glass Ball Tap Action")
                    }
                    item {
                        FloatingCard {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                ButtonPressAction.values().forEach { action ->
                                    val sel = settings.buttonPressAction == action
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (sel) AccentBlue.copy(alpha = 0.25f) else Color.Transparent)
                                            .border(
                                                1.dp,
                                                if (sel) AccentBlue else Color.Transparent,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { onSettingsChange(settings.copy(buttonPressAction = action)) }
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = action.label,
                                                color = if (sel) AccentCyan else TextPrimary,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = action.description,
                                                color = TextSecondary,
                                                fontSize = 9.sp
                                            )
                                        }
                                        if (sel) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pointer Tracking & Acceleration
                    item {
                        FloatingSectionTitle("Pointer Speed & Acceleration")
                    }
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

                    // Scrolling & Gestures
                    item {
                        FloatingSectionTitle("Scrolling & Gestures")
                    }
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
                                "Fluid inertial deceleration",
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
                                "Double tap and slide to drag",
                                settings.doubleTapDrag
                            ) {
                                onSettingsChange(settings.copy(doubleTapDrag = it))
                            }
                        }
                    }

                    // Clock & Battery HUD
                    item {
                        FloatingSectionTitle("Clock & Battery HUD Overlay")
                    }
                    item {
                        FloatingCard {
                            Text("Clock Style", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ClockStyle.values().forEach { style ->
                                    val sel = settings.clockStyle == style
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (sel) AccentCyan else Color(0x22FFFFFF))
                                            .clickable { onSettingsChange(settings.copy(clockStyle = style)) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(style.label, color = if (sel) Color.Black else TextSecondary, fontSize = 9.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            SwitchItem(
                                "24-Hour Format",
                                "Display 24h military time",
                                settings.show24HourFormat
                            ) {
                                onSettingsChange(settings.copy(show24HourFormat = it))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            SwitchItem(
                                "Show Battery %",
                                "Battery charge pill in HUD",
                                settings.showBatteryPercentage
                            ) {
                                onSettingsChange(settings.copy(showBatteryPercentage = it))
                            }
                        }
                    }

                    // Haptics & Reset
                    item {
                        FloatingSectionTitle("Haptic Feedback")
                    }
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

                    // Bluetooth Pairing Hub Shortcut
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(AccentCyan, AccentPink)
                                    )
                                )
                                .clickable { onPairNewDevice() }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Pairing & Host Hub", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Reset Defaults
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x881E1F2C))
                                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                                .clickable { onSettingsChange(TouchpadSettings()) }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset All Defaults", color = TextSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // ==================== TAB 1: THEME ====================
                else if (selectedTab == 1) {
                    // Quick Preset Cycler Slots (5 Presets)
                    item {
                        FloatingSectionTitle("Saved Quick-Switch Presets (Up to 5)")
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            settings.themePresets.take(5).forEachIndexed { index, preset ->
                                val isActive = settings.currentPresetIndex == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isActive) AccentEmerald else Color(0xCC181926))
                                        .border(
                                            1.dp,
                                            if (isActive) Color.White else Color(0x22FFFFFF),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            onSettingsChange(
                                                settings.copy(
                                                    backgroundTheme = preset.theme,
                                                    themeVariant = preset.variant,
                                                    customImageUri = preset.customUri,
                                                    currentPresetIndex = index
                                                )
                                            )
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "Slot ${index + 1}",
                                            color = if (isActive) Color.Black else TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            preset.theme.displayName.take(8),
                                            color = if (isActive) Color.Black.copy(alpha = 0.7f) else TextSecondary,
                                            fontSize = 8.5.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Save Current Theme to Active Preset Slot Button
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x3310B981))
                                .border(1.dp, AccentEmerald.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable {
                                    val updatedPresets = settings.themePresets.toMutableList()
                                    if (settings.currentPresetIndex in updatedPresets.indices) {
                                        updatedPresets[settings.currentPresetIndex] = ThemePreset(
                                            theme = settings.backgroundTheme,
                                            variant = settings.themeVariant,
                                            customUri = settings.customImageUri
                                        )
                                    }
                                    onSettingsChange(settings.copy(themePresets = updatedPresets))
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Bookmark, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Current Theme to Slot ${settings.currentPresetIndex + 1}", color = AccentEmerald, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Color Palette Variant
                    item {
                        FloatingSectionTitle("Color Palette Variant")
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val palettes = listOf(
                                ThemeVariant.VARIANT_A to "Soft Palette",
                                ThemeVariant.VARIANT_B to "Warm Palette",
                                ThemeVariant.VARIANT_C to "Cool Palette"
                            )
                            palettes.forEach { (variant, label) ->
                                val sel = settings.themeVariant == variant
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (sel) AccentPink else Color(0xCC181926))
                                        .border(
                                            1.dp,
                                            if (sel) Color.White.copy(alpha = 0.6f) else Color(0x22FFFFFF),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onSettingsChange(settings.copy(themeVariant = variant)) }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        color = if (sel) Color.White else TextSecondary,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // 10 Procedural Themes
                    item {
                        FloatingSectionTitle("Procedural Themes (Live Background)")
                    }

                    val themes = BackgroundTheme.values().filter { it != BackgroundTheme.CUSTOM_IMAGE }
                    items(themes.toList()) { theme ->
                        val sel = settings.backgroundTheme == theme && settings.customImageUri == null
                        val icon = getThemeIcon(theme)
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
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (sel) AccentPink.copy(alpha = 0.25f) else Color(0x22FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = if (sel) AccentPink else TextSecondary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
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
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }

                    // Custom Wallpaper
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
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (hasCustom) AccentCyan.copy(alpha = 0.25f) else Color(0x22FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = if (hasCustom) AccentCyan else TextSecondary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (hasCustom) "Custom Wallpaper (Active)" else "Choose Image or GIF",
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
                        }
                    }
                }

                // ==================== TAB 2: FINGER SHADER ====================
                else {
                    item {
                        FloatingCard {
                            SwitchItem(
                                title = "Finger Effects",
                                subtitle = "Multi-touch particle trails following active touches",
                                checked = settings.fingerEffectsEnabled,
                                onChange = { onSettingsChange(settings.copy(fingerEffectsEnabled = it)) }
                            )
                        }
                    }

                    if (settings.fingerEffectsEnabled) {
                        items(FingerEffect.values().toList()) { effect ->
                            val sel = settings.fingerEffect == effect
                            val icon = getEffectIcon(effect)
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
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (sel) AccentGold.copy(alpha = 0.25f) else Color(0x22FFFFFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = if (sel) AccentGold else TextSecondary, modifier = Modifier.size(17.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
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
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Top-Right Live Mini Preview Popup for Finger Effects
            (previewEffect ?: if (settings.fingerEffectsEnabled) settings.fingerEffect else null)?.let { effect ->
                if (selectedTab == 2) {
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
}

private fun getThemeIcon(theme: BackgroundTheme): ImageVector {
    return when (theme) {
        BackgroundTheme.SAKURA_PETALS -> Icons.Default.AutoAwesome
        BackgroundTheme.BUBBLE_POP -> Icons.Default.Water
        BackgroundTheme.KAWAII_PAWS -> Icons.Default.Pets
        BackgroundTheme.PRISM_WAVES -> Icons.Default.Waves
        BackgroundTheme.MATCHA_CAFE -> Icons.Default.LocalCafe
        BackgroundTheme.RETRO_ARCADE -> Icons.Default.SportsEsports
        BackgroundTheme.TROPICAL_OCEAN -> Icons.Default.Water
        BackgroundTheme.STRAWBERRY_MOCHI -> Icons.Default.Grain
        BackgroundTheme.STARRY_GALAXY -> Icons.Default.Star
        BackgroundTheme.CLEAN_MINIMAL -> Icons.Default.DarkMode
        BackgroundTheme.CUSTOM_IMAGE -> Icons.Default.Image
    }
}

private fun getEffectIcon(effect: FingerEffect): ImageVector {
    return when (effect) {
        FingerEffect.CHERRY_PETALS -> Icons.Default.AutoAwesome
        FingerEffect.BUBBLE_SPLASH -> Icons.Default.Water
        FingerEffect.CAT_PAW_PRINTS -> Icons.Default.Pets
        FingerEffect.STAR_GLITTER -> Icons.Default.Star
        FingerEffect.RAINBOW_RIBBON -> Icons.Default.Waves
        FingerEffect.WATER_RIPPLES -> Icons.Default.Water
        FingerEffect.PLASMA_LIGHTNING -> Icons.Default.AutoAwesome
        FingerEffect.NEON_RETICLE -> Icons.Default.Tune
        FingerEffect.FIRE_HEARTS -> Icons.Default.AutoAwesome
        FingerEffect.MINIMAL_DOT -> Icons.Default.Grain
    }
}

@Composable
private fun FloatingSectionTitle(title: String) {
    Text(
        title,
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
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
