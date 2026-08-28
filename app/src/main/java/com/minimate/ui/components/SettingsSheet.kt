package com.minimate.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.bluetooth.BluetoothUiState
import com.minimate.bluetooth.ConnectionStatus
import com.minimate.touchpad.model.*

private val GlassSurface = Color(0xA61B1B1D)
private val GlassStroke = Color(0x42FFFFFF)
private val GlassText = Color(0xFFF7F7F7)
private val GlassMuted = Color(0xFFB5B5B7)
private val GlassAccent = Color(0xFFF7F7F7)
private val GlassAccentMuted = Color(0xFFCAD0D8)

private enum class MenuPane(val label: String, val icon: ImageVector) {
    THEMES("Themes", Icons.Default.Palette),
    MOUSE("Mouse", Icons.Default.Mouse),
    PAIRING("Pairing", Icons.Default.Bluetooth)
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: TouchpadSettings,
    bluetoothState: BluetoothUiState,
    batteryPercentage: Int,
    onSettingsChange: (TouchpadSettings) -> Unit,
    onOpenThemeTester: () -> Unit,
    onOpenKeyboardThemeEditor: () -> Unit,
    onOpenEdgeThemeEditor: () -> Unit,
    onOpenScreenEditor: () -> Unit,
    onOpenTrackpadTester: () -> Unit,
    onOpenStickTester: () -> Unit,
    onConnectAddress: (String) -> Unit,
    onDisconnect: () -> Unit,
    onPairNewDevice: () -> Unit,
    onRefreshDevices: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pane by remember { mutableStateOf(MenuPane.THEMES) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color(0x72000000),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(.94f)
                // Samsung reports the Flip cover cameras as a 220 px bottom-right cutout,
                // but ModalBottomSheet consumes that inset before Compose can pad for it.
                // Reserve the physical camera band explicitly on this cover-screen UI.
                .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 112.dp)
                .shadow(28.dp, RoundedCornerShape(30.dp), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(listOf(Color(0xF217181A), Color(0xEE101113), Color(0xF208090A))))
                .border(1.dp, GlassStroke, RoundedCornerShape(30.dp)).padding(12.dp)
        ) {
            GlassHeader(onDismiss)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0x66222224))
                    .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(18.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MenuPane.values().forEach { destination ->
                    LiquidTab(destination, pane == destination, Modifier.weight(1f)) { pane = destination }
                }
            }
            Spacer(Modifier.height(10.dp))
            when (pane) {
                MenuPane.THEMES -> ThemesPane(
                    settings,
                    onSettingsChange,
                    { onDismiss(); onOpenThemeTester() },
                    { onDismiss(); onOpenKeyboardThemeEditor() },
                    { onDismiss(); onOpenEdgeThemeEditor() }
                )
                MenuPane.MOUSE -> MousePane(
                    settings = settings,
                    onSettingsChange = onSettingsChange,
                    onOpenEditor = { onDismiss(); onOpenScreenEditor() },
                    onOpenTrackpadTester = { onDismiss(); onOpenTrackpadTester() },
                    onOpenStickTester = { onDismiss(); onOpenStickTester() }
                )
                MenuPane.PAIRING -> PairingPane(
                    bluetoothState, batteryPercentage, onConnectAddress, onDisconnect,
                    onPairNewDevice, onRefreshDevices
                )
            }
        }
    }
}

@Composable
private fun GlassHeader(onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(34.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color.White.copy(.22f), Color.White.copy(.06f))))
                .border(1.dp, Color.White.copy(.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Tune, null, tint = Color.White, modifier = Modifier.size(17.dp)) }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text("MiniMate", color = GlassText, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text("Control Center", color = GlassMuted, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0x25FFFFFF)).border(1.dp, Color(0x25FFFFFF), CircleShape)
        ) { Icon(Icons.Default.Close, "Close", tint = GlassText, modifier = Modifier.size(16.dp)) }
    }
}

@Composable
private fun LiquidTab(destination: MenuPane, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val fill = if (selected) Brush.horizontalGradient(listOf(Color.White.copy(.18f), Color.White.copy(.10f)))
    else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(fill).clickable(onClick = onClick).padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(destination.icon, null, tint = if (selected) Color.White else GlassMuted, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(destination.label, color = if (selected) Color.White else GlassMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ThemesPane(
    settings: TouchpadSettings,
    onSettingsChange: (TouchpadSettings) -> Unit,
    onOpenStudio: () -> Unit,
    onOpenKeyboardStudio: () -> Unit,
    onOpenEdgeStudio: () -> Unit
) {
    val subtheme = subthemesFor(settings.abstractShaderTheme).getOrNull(settings.abstractSubthemeIndex)
    val activeColorway = validColorway(settings.abstractShaderTheme, settings.abstractSubthemeIndex, settings.shaderRecolor)
    val authoredColorway = sceneColorwayFor(settings.abstractShaderTheme, settings.abstractSubthemeIndex, activeColorway)
    val previewStops = authoredColorway.stops
    LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item {
            GlassCard(accent = GlassAccentMuted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(15.dp))
                            .background(Brush.linearGradient(listOf(Color(previewStops.getOrElse(1) { previewStops.first() }), Color(previewStops.getOrElse(2) { previewStops.last() }))))
                            .border(1.dp, Color.White.copy(.3f), RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Palette, null, tint = Color.White) }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(settings.abstractShaderTheme.label, color = GlassText, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        Text(subtheme?.label ?: "Shader ${settings.abstractSubthemeIndex + 1}", color = GlassMuted, fontSize = 10.sp)
                        Text("${authoredColorway.label} · ${if (settings.themeFilters.isEmpty()) "Clean" else settings.themeFilters.joinToString(" + ") { it.label }}", color = GlassAccentMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(12.dp))
                LiquidButton("Open Theme Studio", Icons.Default.OpenInFull, GlassAccent, onOpenStudio)
            }
        }
        item {
            GlassCard(accent = GlassAccentMuted) {
                SectionLabel("Keyboard", "Preview keys, glide trail, fonts, weights, and opacity")
                Spacer(Modifier.height(10.dp))
                LiquidButton("Open Keyboard Studio", Icons.Default.Keyboard, GlassAccent, onOpenKeyboardStudio)
            }
        }
        item {
            GlassCard(accent = GlassAccentMuted) {
                SectionLabel("Buttons", "Preview the scroll rail and right-click corner as a pair")
                Spacer(Modifier.height(10.dp))
                LiquidButton("Open Buttons Studio", Icons.Default.TouchApp, GlassAccent, onOpenEdgeStudio)
            }
        }
        item {
            Text(
                "Each studio opens on the main screen so its changes can be judged against the active scene.",
                color = GlassMuted, fontSize = 9.5.sp, lineHeight = 13.sp, modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}

@Composable
private fun MousePane(
    settings: TouchpadSettings,
    onSettingsChange: (TouchpadSettings) -> Unit,
    onOpenEditor: () -> Unit,
    onOpenTrackpadTester: () -> Unit,
    onOpenStickTester: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item {
            GlassCard(accent = GlassAccentMuted) {
                SectionLabel("Movement", "Pointer response and acceleration")
                GlassSlider("Tracking speed", settings.trackingSpeed, .4f..2.5f) { onSettingsChange(settings.copy(trackingSpeed = it)) }
                GlassSlider("Acceleration", settings.acceleration, .5f..2.5f) { onSettingsChange(settings.copy(acceleration = it)) }
                GlassSwitch("Invert vertical movement", "Reverse pointer up and down", settings.invertCursorY) { onSettingsChange(settings.copy(invertCursorY = it)) }
                Spacer(Modifier.height(8.dp))
                LiquidButton("Test movement on trackpad", Icons.Default.OpenInFull, GlassAccent, onOpenTrackpadTester)
            }
        }
        item {
            GlassCard {
                SectionLabel("Scrolling", "Two-finger and kinetic behavior")
                GlassSlider("Scroll speed", settings.scrollSpeed, .10f..2f) { onSettingsChange(settings.copy(scrollSpeed = it)) }
                GlassSwitch("Natural scrolling", "Content follows finger direction", settings.naturalScrolling) { onSettingsChange(settings.copy(naturalScrolling = it)) }
                GlassSwitch("Kinetic momentum", "Continue smoothly after release", settings.momentumScrolling) { onSettingsChange(settings.copy(momentumScrolling = it)) }
                if (settings.momentumScrolling) GlassSlider("Momentum glide", settings.momentumFriction, .82f..98f) { onSettingsChange(settings.copy(momentumFriction = it)) }
            }
        }
        item {
            GlassCard {
                SectionLabel("Click & gesture", "Trackpad behavior")
                GlassSwitch("Tap to click", "One-finger primary click", settings.tapToClick) { onSettingsChange(settings.copy(tapToClick = it)) }
                GlassSwitch("Two-finger right click", "Two-finger context click", settings.twoFingerRightClick) { onSettingsChange(settings.copy(twoFingerRightClick = it)) }
                GlassSwitch("Double-tap drag", "Drag without holding a button", settings.doubleTapDrag) { onSettingsChange(settings.copy(doubleTapDrag = it)) }
                ChoiceRow("Haptics", HapticIntensity.values().map { it.name.lowercase().replaceFirstChar(Char::uppercase) }, settings.hapticIntensity.ordinal) { index ->
                    onSettingsChange(settings.copy(hapticIntensity = HapticIntensity.values()[index]))
                }
            }
        }
        item {
            GlassCard(accent = GlassAccentMuted) {
                SectionLabel("Single-hand edges", "Subtle controls that mirror for either hand")
                GlassSwitch("Edge scroll rail", "Drag the slim full-height edge to scroll", settings.edgeScrollEnabled) {
                    onSettingsChange(settings.copy(edgeScrollEnabled = it))
                }
                GlassSwitch("Corner right click", "Tap the opposite top corner for a context click", settings.edgeRightClickEnabled) {
                    onSettingsChange(settings.copy(edgeRightClickEnabled = it))
                }
                ChoiceRow(
                    "Scroll rail side",
                    EdgeControlSide.values().map { it.label },
                    settings.edgeControlSide.ordinal
                ) { index ->
                    onSettingsChange(settings.copy(edgeControlSide = EdgeControlSide.values()[index]))
                }
                Text(
                    if (settings.edgeControlSide == EdgeControlSide.LEFT)
                        "Left rail · right-click target in the top-right corner"
                    else
                        "Right rail · right-click target in the top-left corner",
                    color = GlassMuted,
                    fontSize = 9.sp,
                    lineHeight = 12.sp
                )
            }
        }
        item {
            GlassCard(accent = GlassAccentMuted) {
                SectionLabel("Analog stick", "Movement, scrolling, and actions")
                GlassSwitch("Show analog stick", "Hide the control completely when disabled", settings.stickEnabled) {
                    onSettingsChange(settings.copy(stickEnabled = it))
                }
                ChoiceRow("Mode", AnalogStickMode.values().map { it.label }, settings.analogStickMode.ordinal) { index ->
                    onSettingsChange(settings.copy(analogStickMode = AnalogStickMode.values()[index]))
                }
                Text("Stick design", color = GlassMuted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StickTheme.values().forEach { theme -> GlassChip(theme.label, settings.stickTheme == theme) { onSettingsChange(settings.copy(stickTheme = theme)) } }
                }
                GlassSlider("Stick speed", settings.stickScrollSensitivity, .1f..2f) { onSettingsChange(settings.copy(stickScrollSensitivity = it)) }
                GlassSlider("Dead zone", settings.stickDeadzone, .04f..0.35f) { onSettingsChange(settings.copy(stickDeadzone = it)) }
                ActionSelector("Tap", settings.stickSingleTapAction) { onSettingsChange(settings.copy(stickSingleTapAction = it)) }
                ActionSelector("Double tap", settings.stickDoubleTapAction) { onSettingsChange(settings.copy(stickDoubleTapAction = it)) }
                ActionSelector("Hold", settings.stickHoldAction) { onSettingsChange(settings.copy(stickHoldAction = it)) }
                Spacer(Modifier.height(8.dp))
                LiquidButton("Test centered stick", Icons.Default.OpenWith, GlassAccent, onOpenStickTester)
            }
        }
        item { LiquidButton("Arrange Stick & Clock", Icons.Default.OpenInFull, GlassAccent, onOpenEditor) }
    }
}

@Composable
private fun PairingPane(
    bluetoothState: BluetoothUiState,
    batteryPercentage: Int,
    onConnectAddress: (String) -> Unit,
    onDisconnect: () -> Unit,
    onPairNewDevice: () -> Unit,
    onRefreshDevices: () -> Unit
) {
    val connected = bluetoothState.status == ConnectionStatus.CONNECTED
    LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item {
            GlassCard(accent = GlassAccentMuted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(if (connected) .16f else .08f))
                            .border(1.dp, Color.White.copy(if (connected) .50f else .22f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Bluetooth, null, tint = if (connected) Color.White else GlassMuted) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        val status = bluetoothState.status.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
                        Text(if (connected) "Connected" else status, color = GlassText, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        Text(bluetoothState.connectedHost?.name ?: "No active computer", color = GlassMuted, fontSize = 10.sp)
                    }
                    Text("$batteryPercentage%", color = GlassAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                bluetoothState.errorMessage?.let { Text(it, color = GlassAccentMuted, fontSize = 9.sp) }
                if (connected) LiquidButton("Disconnect", Icons.Default.Close, GlassAccent, onDisconnect)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { LiquidButton("Pair New", Icons.AutoMirrored.Filled.BluetoothSearching, GlassAccent, onPairNewDevice) }
                Box(Modifier.weight(1f)) { LiquidButton("Refresh", Icons.Default.Refresh, GlassAccent, onRefreshDevices) }
            }
        }
        item { SectionLabel("Known computers", "Tap a host to connect") }
        if (bluetoothState.pairedHosts.isEmpty()) {
            item {
                GlassCard {
                    Text("No paired hosts yet", color = GlassText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Choose Pair New and add MiniMate as a Bluetooth mouse from your computer.", color = GlassMuted, fontSize = 9.5.sp, lineHeight = 13.sp)
                }
            }
        } else {
            items(bluetoothState.pairedHosts, key = { it.address }) { host ->
                val active = bluetoothState.connectedHost?.address == host.address
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                        .background(if (active) Color.White.copy(.14f) else GlassSurface)
                        .border(1.dp, if (active) Color.White.copy(.52f) else Color(0x22FFFFFF), RoundedCornerShape(17.dp))
                        .clickable(enabled = !active) { onConnectAddress(host.address) }.padding(11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0x22FFFFFF)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Mouse, null, tint = if (active) Color.White else GlassMuted, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(host.name, color = GlassText, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(host.address, color = GlassMuted, fontSize = 8.5.sp)
                    }
                    if (active) Icon(Icons.Default.Check, "Connected", tint = Color.White, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
private fun GlassCard(accent: Color = Color.Transparent, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(GlassSurface, Color(0x74242426), Color(0x8A151517))))
            .border(1.dp, if (accent == Color.Transparent) Color(0x28FFFFFF) else accent.copy(.42f), RoundedCornerShape(20.dp))
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp), content = content
    )
}

@Composable
private fun SectionLabel(title: String, subtitle: String) {
    Column { Text(title, color = GlassText, fontSize = 12.5.sp, fontWeight = FontWeight.Black); Text(subtitle, color = GlassMuted, fontSize = 9.sp) }
}

@Composable
private fun LiquidButton(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
            .background(Brush.horizontalGradient(listOf(Color.White.copy(.18f), Color.White.copy(.10f))))
            .border(1.dp, Color.White.copy(.28f), RoundedCornerShape(15.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(7.dp))
        Text(label, color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun GlassSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = GlassText, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
            Text("%.2f".format(value), color = GlassAccent, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
        }
        Slider(value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color(0x2AFFFFFF)), modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun GlassSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = GlassText, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = GlassMuted, fontSize = 8.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Switch(checked, onCheckedChange = onChange, modifier = Modifier.size(42.dp, 24.dp),
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF17181A), checkedTrackColor = Color.White, uncheckedThumbColor = GlassMuted, uncheckedTrackColor = Color(0x24FFFFFF)))
    }
}

@Composable
private fun ChoiceRow(label: String, options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = GlassMuted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEachIndexed { index, option -> GlassChip(option, selectedIndex == index) { onSelect(index) } }
        }
    }
}

@Composable
private fun GlassChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape).background(if (selected) Color.White.copy(.92f) else Color(0x24FFFFFF))
            .border(1.dp, if (selected) Color.White.copy(.65f) else Color(0x22FFFFFF), CircleShape)
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp)
    ) { Text(label, color = if (selected) Color(0xFF071019) else GlassMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
}

@Composable
private fun ActionSelector(title: String, currentAction: BallAction, onSelect: (BallAction) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = GlassMuted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            GlassChip(currentAction.label, expanded) { expanded = !expanded }
        }
        if (expanded) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            BallAction.values().forEach { action -> GlassChip(action.label, action == currentAction) { onSelect(action); expanded = false } }
        }
    }
}
