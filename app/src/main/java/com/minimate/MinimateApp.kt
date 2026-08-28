package com.minimate

import android.app.Application
import android.bluetooth.BluetoothManager
import com.minimate.bluetooth.BluetoothAudioBridge

class MinimateApp : Application() {
    lateinit var audioBridge: BluetoothAudioBridge
        private set

    override fun onCreate() {
        super.onCreate()
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        audioBridge = BluetoothAudioBridge(this, bluetoothManager.adapter)
    }
}
