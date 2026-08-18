package com.minimate.bluetooth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors host phone battery percentage to report over Bluetooth HID Battery profile.
 */
class BatteryReporter(private val context: Context) {

    private val _batteryLevel = MutableStateFlow(getInitialBatteryLevel())
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    val percentage = (level * 100) / scale
                    _batteryLevel.value = percentage
                }
            }
        }
    }

    private var isRegistered = false

    fun start() {
        if (!isRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(batteryReceiver, filter)
            isRegistered = true
        }
    }

    fun stop() {
        if (isRegistered) {
            try {
                context.unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {}
            isRegistered = false
        }
    }

    private fun getInitialBatteryLevel(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        return batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
    }
}
