package com.minimate.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.bluetooth.AudioBridgeState
import com.minimate.bluetooth.BluetoothUiState
import com.minimate.bluetooth.ConnectionStatus
import com.minimate.ui.theme.Mont

private val Danger = Color(0xFFC0392B)
private val Live = Color(0xFF2E9E5B)

/**
 * Pairing.
 *
 * The ground says the answer before any of the words do: red while nothing is joined up, green
 * once it is. There is one list, of Macs, because a Mac is one thing — the input link runs over
 * Bluetooth and the audio link over Wi-Fi, but nobody pairing a computer thinks of it as two
 * computers. Choosing one does both: the input link is opened directly, and the phone is made
 * discoverable so the companion can find its way back for the audio.
 */
@Composable
fun BluetoothPairingDialog(
    bluetoothState: BluetoothUiState,
    audioState: AudioBridgeState,
    onMakeDiscoverable: () -> Unit,
    onConnectHost: (String) -> Unit,
    onDisconnect: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val inputLive = bluetoothState.status == ConnectionStatus.CONNECTED
    val audioLive = audioState.connected
    val anythingLive = inputLive || audioLive

    val transition = rememberInfiniteTransition(label = "pairing")
    val travel by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(5200, easing = LinearEasing)), label = "stripes"
    )
    val accent by animateColorAsState(
        targetValue = if (anythingLive) Live else Danger,
        animationSpec = tween(durationMillis = 420),
        label = "accent"
    )

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        DiagonalStripes(travel, accent, Color.Black, modifier = Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 16.dp)
                .background(Color.Black.copy(MONT_SURFACE_ALPHA))
                .padding(start = 22.dp, top = 20.dp, end = 16.dp, bottom = 14.dp)
        ) {
            Text(
                "PAIRING",
                color = Color.White,
                fontFamily = Mont,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
            )

            Spacer(Modifier.height(10.dp))

            // The two halves of one connection, each said plainly rather than implied by a colour.
            Link("INPUT", if (inputLive) "Bluetooth" else "Not joined", inputLive)
            Link("AUDIO", if (audioLive) audioState.hostName ?: "Wi-Fi" else "Not joined", audioLive)

            Spacer(Modifier.height(14.dp))

            Column(
                Modifier.heightIn(max = 132.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                if (bluetoothState.pairedHosts.isEmpty()) {
                    Text(
                        "Nothing saved yet. Make this phone discoverable, then pair it from the Mac.",
                        color = Color.White.copy(.45f),
                        fontFamily = Mont,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                } else {
                    bluetoothState.pairedHosts.forEach { host ->
                        val current = bluetoothState.connectedHost?.address == host.address
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onConnectHost(host.address) }
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                host.name,
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(if (current) 1f else .62f),
                                fontFamily = Mont,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (current) Box(Modifier.size(6.dp).background(accent))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // One tap does both halves: the input link is opened here, and the phone is put on the
            // air so the companion can complete the audio link from its own side.
            PairingRow("Pair a new Mac") { onMakeDiscoverable() }
            PairingRow("Look again") { onRefresh() }
            if (anythingLive) PairingRow("Disconnect") { onDisconnect() }
            PairingRow("Close") { onDismiss() }
        }
    }
}

@Composable
private fun Link(label: String, detail: String, live: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            modifier = Modifier.weight(.3f),
            color = Color.White.copy(if (live) 1f else .45f),
            fontFamily = Mont,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp
        )
        Text(
            detail,
            modifier = Modifier.weight(.7f),
            color = Color.White.copy(if (live) .8f else .38f),
            fontFamily = Mont,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PairingRow(label: String, onClick: () -> Unit) {
    Text(
        label.uppercase(),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 7.dp),
        color = Color.White,
        fontFamily = Mont,
        fontWeight = FontWeight.Black,
        fontSize = 14.sp
    )
}
