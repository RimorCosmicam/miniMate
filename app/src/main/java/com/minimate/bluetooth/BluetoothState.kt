package com.minimate.bluetooth

import android.bluetooth.BluetoothDevice

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    SUSPENDED,
    ERROR,
    BLUETOOTH_OFF,
    NO_PERMISSION
}

data class ConnectedHost(
    val name: String,
    val address: String,
    val isConnected: Boolean = false,
    val isPaired: Boolean = true,
    val batteryLevel: Int = 100
)

data class BluetoothUiState(
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val connectedHost: ConnectedHost? = null,
    val pairedHosts: List<ConnectedHost> = emptyList(),
    val isAppRegistered: Boolean = false,
    val errorMessage: String? = null,
    val latencyMs: Long = 0L,
    val packetsSent: Long = 0L
)
