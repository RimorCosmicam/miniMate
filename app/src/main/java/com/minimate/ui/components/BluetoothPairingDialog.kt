package com.minimate.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.bluetooth.BluetoothUiState
import com.minimate.bluetooth.ConnectedHost
import com.minimate.bluetooth.ConnectionStatus
import com.minimate.ui.theme.AccentBlue
import com.minimate.ui.theme.AccentCyan
import com.minimate.ui.theme.AccentEmerald
import com.minimate.ui.theme.AccentPink
import com.minimate.ui.theme.GlassSurface
import com.minimate.ui.theme.ObsidianSurface
import com.minimate.ui.theme.TextPrimary
import com.minimate.ui.theme.TextSecondary
import com.minimate.ui.theme.TextTertiary

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothPairingDialog(
    bluetoothState: BluetoothUiState,
    onMakeDiscoverable: () -> Unit,
    onConnectHost: (String) -> Unit,
    onDisconnect: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val transition = rememberInfiniteTransition(label = "RadarPulse")
    val pulse1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Pulse1"
    )
    val pulse2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Pulse2"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        scrimColor = Color(0xBB000000),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Pairing & Host Manager",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Radar Visualizer & Status Badge
            item {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxR = size.minDimension / 2f

                        // Outer ring 1
                        val r1 = maxR * pulse1
                        val alpha1 = (1f - pulse1) * 0.7f
                        drawCircle(
                            color = AccentCyan.copy(alpha = alpha1),
                            radius = r1,
                            center = center,
                            style = Stroke(2.5f)
                        )

                        // Outer ring 2
                        val r2 = maxR * pulse2
                        val alpha2 = (1f - pulse2) * 0.7f
                        drawCircle(
                            color = AccentPink.copy(alpha = alpha2),
                            radius = r2,
                            center = center,
                            style = Stroke(2f)
                        )

                        // Base glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(AccentCyan.copy(alpha = 0.25f), Color.Transparent),
                                center = center,
                                radius = maxR * 0.8f
                            ),
                            radius = maxR * 0.8f,
                            center = center
                        )
                    }

                    // Center Bluetooth Icon Orb
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(AccentCyan, AccentPink)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (bluetoothState.status == ConnectionStatus.CONNECTED) Icons.Default.BluetoothConnected else Icons.Default.BluetoothSearching,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Connection Status Banner
            item {
                val (statusText, statusBg, statusColor) = when (bluetoothState.status) {
                    ConnectionStatus.CONNECTED -> Triple(
                        "Connected to ${bluetoothState.connectedHost?.name ?: "Host"}",
                        AccentEmerald.copy(alpha = 0.15f),
                        AccentEmerald
                    )
                    ConnectionStatus.CONNECTING -> Triple(
                        "Connecting...",
                        AccentCyan.copy(alpha = 0.15f),
                        AccentCyan
                    )
                    ConnectionStatus.BLUETOOTH_OFF -> Triple(
                        "Bluetooth is Disabled",
                        Color(0x33EF4444),
                        Color(0xFFEF4444)
                    )
                    ConnectionStatus.NO_PERMISSION -> Triple(
                        "Bluetooth Permission Required",
                        Color(0x33F59E0B),
                        Color(0xFFF59E0B)
                    )
                    else -> Triple(
                        "Ready to Pair • Broadcast Active",
                        GlassSurface,
                        TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(statusBg)
                        .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Primary "Make Discoverable (180s)" Action Button
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
                        .clickable { onMakeDiscoverable() }
                        .padding(vertical = 14.dp),
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
                        "Make Discoverable (180s)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Easy Pairing Guide Steps
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GlassSurface.copy(alpha = 0.6f))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Pairing Instructions",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    GuideStep(number = "1", text = "Tap 'Make Discoverable' above")
                    GuideStep(number = "2", text = "On host computer/tablet, open Bluetooth Settings")
                    GuideStep(number = "3", text = "Select your Galaxy Z Flip from available devices")
                    GuideStep(number = "4", text = "Click Pair to connect as wireless trackpad")
                }
            }

            // Paired Devices List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Paired Host Devices (${bluetoothState.pairedHosts.size})",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onRefresh() }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = AccentCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refresh", color = AccentCyan, fontSize = 11.sp)
                    }
                }
            }

            // Paired Devices List
            if (bluetoothState.pairedHosts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassSurface.copy(alpha = 0.3f))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No paired devices found.\nMake discoverable to connect your first host.",
                            color = TextTertiary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(bluetoothState.pairedHosts) { host ->
                    PairedDeviceCard(
                        host = host,
                        onConnect = { onConnectHost(host.address) },
                        onDisconnect = onDisconnect
                    )
                }
            }

            // Open System Bluetooth Settings Shortcut
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassSurface.copy(alpha = 0.4f))
                        .clickable {
                            try {
                                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            } catch (_: Exception) {}
                        }
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Android Bluetooth Settings",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideStep(number: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(AccentPink.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = AccentPink, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun PairedDeviceCard(
    host: ConnectedHost,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (host.isConnected) AccentEmerald.copy(alpha = 0.12f) else GlassSurface.copy(alpha = 0.5f)
            )
            .border(
                1.dp,
                if (host.isConnected) AccentEmerald.copy(alpha = 0.35f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Default.Laptop,
                contentDescription = null,
                tint = if (host.isConnected) AccentEmerald else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = host.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (host.isConnected) "Active Connection" else host.address,
                    color = if (host.isConnected) AccentEmerald else TextTertiary,
                    fontSize = 10.sp
                )
            }
        }

        if (host.isConnected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x33EF4444))
                    .clickable { onDisconnect() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    "Disconnect",
                    color = Color(0xFFEF4444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentBlue)
                    .clickable { onConnect() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "Connect",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
