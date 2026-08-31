package com.minimate

import android.app.Application
import android.bluetooth.BluetoothManager
import com.minimate.bluetooth.BluetoothAudioBridge
import com.minimate.touchpad.engine.TouchpadEngine
import com.minimate.touchpad.model.TouchpadSettings

class MinimateApp : Application() {
    lateinit var audioBridge: BluetoothAudioBridge
        private set

    /**
     * The running session, while there is one.
     *
     * Held here so a tap on the home-screen widget reaches the app that is already open rather
     * than only its stored settings — otherwise the two would disagree until the next launch.
     */
    var activeEngine: TouchpadEngine? = null

    fun applyWidgetSettings(settings: TouchpadSettings) {
        activeEngine?.updateSettings(settings)
    }

    override fun onCreate() {
        super.onCreate()
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        audioBridge = BluetoothAudioBridge(this, bluetoothManager.adapter)
    }
}
