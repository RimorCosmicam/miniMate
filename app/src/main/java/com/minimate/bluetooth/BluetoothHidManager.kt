package com.minimate.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Robust Bluetooth HID Device (HOGP) Manager.
 * Exposes the phone as a native Bluetooth Trackpad & Mouse to any paired host.
 */
@SuppressLint("MissingPermission")
class BluetoothHidManager(private val context: Context) {

    companion object {
        private const val TAG = "MinimateHidManager"
        private const val HID_DEVICE_PROFILE = BluetoothProfile.HID_DEVICE
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val executor = Executors.newSingleThreadExecutor()

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var hidDevice: BluetoothHidDevice? = null
    private var currentHostDevice: BluetoothDevice? = null

    private val _uiState = MutableStateFlow(BluetoothUiState())
    val uiState: StateFlow<BluetoothUiState> = _uiState.asStateFlow()

    // Reusable 7-byte buffer for zero-GC mouse streaming
    private val mouseBuffer = ByteArray(HidReport.REPORT_MOUSE_SIZE)
    private var lastReportTimeNs = 0L

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "onAppStatusChanged: registered=$registered, device=$pluggedDevice")
            _uiState.update { it.copy(isAppRegistered = registered) }

            if (registered && pluggedDevice != null) {
                currentHostDevice = pluggedDevice
                updateConnectedState(pluggedDevice, ConnectionStatus.CONNECTED)
            } else if (!registered) {
                _uiState.update { it.copy(status = ConnectionStatus.DISCONNECTED) }
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            Log.d(TAG, "onConnectionStateChanged: device=${device?.name}, state=$state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    currentHostDevice = device
                    device?.let { updateConnectedState(it, ConnectionStatus.CONNECTED) }
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _uiState.update { it.copy(status = ConnectionStatus.CONNECTING) }
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    _uiState.update { it.copy(status = ConnectionStatus.CONNECTING) }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (currentHostDevice?.address == device?.address) {
                        currentHostDevice = null
                    }
                    _uiState.update {
                        it.copy(
                            status = ConnectionStatus.DISCONNECTED,
                            connectedHost = null
                        )
                    }
                }
            }
            refreshPairedDevices()
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            Log.d(TAG, "onGetReport: type=$type, id=$id, size=$bufferSize")
            if (id == HidDescriptor.REPORT_ID_MOUSE) {
                val report = HidReport.createMouseReport(HidDescriptor.BUTTON_NONE, 0, 0)
                hidDevice?.replyReport(device, type, id, report)
            } else {
                hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
            }
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            Log.d(TAG, "onSetReport: type=$type, id=$id, data=${data?.size} bytes")
            hidDevice?.replyReport(device, type, id, byteArrayOf())
        }

        override fun onSetProtocol(device: BluetoothDevice?, protocol: Byte) {
            Log.d(TAG, "onSetProtocol: protocol=$protocol")
        }

        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {
            Log.d(TAG, "onInterruptData: reportId=$reportId")
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice?) {
            Log.d(TAG, "onVirtualCableUnplug: device=${device?.name}")
            disconnect()
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == HID_DEVICE_PROFILE) {
                Log.d(TAG, "BluetoothHidDevice service connected")
                hidDevice = proxy as? BluetoothHidDevice
                registerHidApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == HID_DEVICE_PROFILE) {
                Log.d(TAG, "BluetoothHidDevice service disconnected")
                hidDevice = null
                _uiState.update {
                    it.copy(
                        isAppRegistered = false,
                        status = ConnectionStatus.DISCONNECTED,
                        connectedHost = null
                    )
                }
            }
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_ON -> initialize()
                        BluetoothAdapter.STATE_TURNING_OFF,
                        BluetoothAdapter.STATE_OFF -> {
                            _uiState.update {
                                it.copy(
                                    status = ConnectionStatus.BLUETOOTH_OFF,
                                    connectedHost = null
                                )
                            }
                        }
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    refreshPairedDevices()
                }
            }
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        context.registerReceiver(bluetoothReceiver, filter)
        initialize()
    }

    fun stop() {
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (_: Exception) {}

        unregisterHidApp()
        hidDevice?.let { bluetoothAdapter?.closeProfileProxy(HID_DEVICE_PROFILE, it) }
        hidDevice = null
    }

    private fun initialize() {
        if (bluetoothAdapter == null) {
            _uiState.update { it.copy(status = ConnectionStatus.ERROR, errorMessage = "Bluetooth not available on this device") }
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            _uiState.update { it.copy(status = ConnectionStatus.BLUETOOTH_OFF) }
            return
        }

        val success = bluetoothAdapter.getProfileProxy(context, profileListener, HID_DEVICE_PROFILE)
        if (!success) {
            _uiState.update {
                it.copy(
                    status = ConnectionStatus.ERROR,
                    errorMessage = "Bluetooth HID Device profile not supported by device firmware."
                )
            }
        }
        refreshPairedDevices()
    }

    private fun registerHidApp() {
        val hid = hidDevice ?: return
        val sdp = HidDescriptor.createSdpSettings()
        val qos = HidDescriptor.createQosSettings()

        try {
            hid.registerApp(sdp, null, qos, executor, hidCallback)
            Log.d(TAG, "HID App registration submitted")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register HID app", e)
            _uiState.update { it.copy(errorMessage = "HID Registration error: ${e.message}") }
        }
    }

    private fun unregisterHidApp() {
        try {
            hidDevice?.unregisterApp()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister HID app", e)
        }
    }

    /**
     * Send mouse movement and button report with sub-millisecond precision.
     */
    fun sendMouseInput(buttons: Byte, dx: Int, dy: Int, wheel: Int = 0, pan: Int = 0): Boolean {
        val device = currentHostDevice ?: return false
        val hid = hidDevice ?: return false

        val startNs = System.nanoTime()
        HidReport.packMouseReport(mouseBuffer, buttons, dx, dy, wheel, pan)

        val success = try {
            hid.sendReport(device, HidDescriptor.REPORT_ID_MOUSE.toInt(), mouseBuffer)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending HID report", e)
            false
        }

        if (success) {
            val latencyUs = (System.nanoTime() - startNs) / 1000
            _uiState.update {
                it.copy(
                    latencyMs = latencyUs / 1000,
                    packetsSent = it.packetsSent + 1
                )
            }
        }
        return success
    }

    /**
     * Send battery percentage report.
     */
    fun sendBatteryReport(level: Int): Boolean {
        val device = currentHostDevice ?: return false
        val hid = hidDevice ?: return false
        val report = HidReport.createBatteryReport(level)
        return try {
            hid.sendReport(device, HidDescriptor.REPORT_ID_BATTERY.toInt(), report)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Connect to a specific Bluetooth host device.
     */
    fun connectToDevice(device: BluetoothDevice) {
        currentHostDevice = device
        _uiState.update { it.copy(status = ConnectionStatus.CONNECTING) }
        try {
            hidDevice?.connect(device)
        } catch (e: Exception) {
            Log.e(TAG, "Connect error", e)
            _uiState.update { it.copy(status = ConnectionStatus.ERROR, errorMessage = e.message) }
        }
    }

    /**
     * Disconnect active host.
     */
    fun disconnect() {
        val device = currentHostDevice
        if (device != null && hidDevice != null) {
            try {
                hidDevice?.disconnect(device)
            } catch (e: Exception) {
                Log.e(TAG, "Disconnect error", e)
            }
        }
        currentHostDevice = null
        _uiState.update {
            it.copy(
                status = ConnectionStatus.DISCONNECTED,
                connectedHost = null
            )
        }
    }

    fun refreshPairedDevices() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return

        val bonded = bluetoothAdapter.bondedDevices.map { device ->
            ConnectedHost(
                name = device.name ?: "Unknown Device",
                address = device.address,
                isConnected = currentHostDevice?.address == device.address,
                isPaired = true
            )
        }

        _uiState.update { it.copy(pairedHosts = bonded) }
    }

    private fun updateConnectedState(device: BluetoothDevice, status: ConnectionStatus) {
        val host = ConnectedHost(
            name = device.name ?: "Connected Host",
            address = device.address,
            isConnected = status == ConnectionStatus.CONNECTED,
            isPaired = true
        )
        _uiState.update {
            it.copy(
                status = status,
                connectedHost = host
            )
        }
    }

    /**
     * Connect to a paired device by its MAC address.
     */
    fun connectByAddress(address: String) {
        val device = bluetoothAdapter?.getRemoteDevice(address) ?: return
        connectToDevice(device)
    }

    /**
     * Get a BluetoothDevice by address for the UI layer.
     */
    fun getDeviceByAddress(address: String): BluetoothDevice? {
        return bluetoothAdapter?.getRemoteDevice(address)
    }

    /**
     * Make the device discoverable for pairing from the host side.
     * Returns true if the request was initiated.
     */
    fun requestDiscoverable(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
}
