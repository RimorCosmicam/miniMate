package com.minimate.audio

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.minimate.touchpad.model.MicrophonePlacement

/**
 * Decides whether the phone is lying on its back or being held, from the gravity vector.
 *
 * The distinction matters because the microphone array's beam should follow it: face-up on a desk
 * the speaker is above and further away, so a narrow beam aimed as if the phone were held at the
 * face will cut them off, while held upright the narrow beam is exactly what rejects the room.
 *
 * Gravity resolves this unambiguously. Face-up, gravity is almost entirely on the device's Z axis
 * (roughly +9.8 m/s²) with little on X or Y. Held or propped, a substantial component appears on
 * X or Y. Hysteresis keeps a phone resting near the boundary — propped at a shallow angle, or
 * being picked up — from oscillating between the two.
 */
class PlacementDetector(context: Context) : SensorEventListener {
    private companion object {
        const val TAG = "MiniMatePlacement"
        /** Above this share of gravity on Z, the phone is flat enough to count as face-up. */
        const val FLAT_ENTER = .93f
        /** Below this it is upright again. The gap between the two is the hysteresis. */
        const val FLAT_EXIT = .82f
    }

    private val sensors = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gravity: Sensor? =
        sensors.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    @Volatile var current: MicrophonePlacement = MicrophonePlacement.HANDHELD
        private set

    /** False when the device exposes no usable sensor, so callers can fall back to the manual choice. */
    val available: Boolean get() = gravity != null

    fun start() {
        val sensor = gravity ?: return
        sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        runCatching { sensors.unregisterListener(this) }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.values.size < 3) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = kotlin.math.sqrt(x * x + y * y + z * z)
        if (magnitude < 1f) return
        // Face-up means gravity points out of the back of the device: +Z in Android's frame.
        val flatness = z / magnitude

        val next = when {
            flatness > FLAT_ENTER -> MicrophonePlacement.DESK
            flatness < FLAT_EXIT -> MicrophonePlacement.HANDHELD
            else -> current
        }
        if (next != current) {
            current = next
            Log.i(TAG, "placement -> ${next.name} (flatness ${"%.2f".format(flatness)})")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
