package com.minimate.bluetooth

/**
 * Pre-allocated, zero-garbage report builders for high-frequency HID transmission.
 */
object HidReport {

    const val REPORT_MOUSE_SIZE = 7
    const val REPORT_CONSUMER_SIZE = 2
    const val REPORT_BATTERY_SIZE = 1

    /**
     * Constructs a mouse input report.
     * @param buttons Bitmask of active buttons (BUTTON_LEFT, BUTTON_RIGHT, etc.)
     * @param dx Relative X movement (-32767..32767)
     * @param dy Relative Y movement (-32767..32767)
     * @param wheel Vertical wheel scroll (-127..127)
     * @param pan Horizontal wheel / AC pan (-127..127)
     */
    fun packMouseReport(
        buffer: ByteArray,
        buttons: Byte,
        dx: Int,
        dy: Int,
        wheel: Int = 0,
        pan: Int = 0
    ): ByteArray {
        val clampedDx = dx.coerceIn(-32767, 32767)
        val clampedDy = dy.coerceIn(-32767, 32767)
        val clampedWheel = wheel.coerceIn(-127, 127)
        val clampedPan = pan.coerceIn(-127, 127)

        buffer[0] = buttons
        buffer[1] = (clampedDx and 0xFF).toByte()
        buffer[2] = ((clampedDx shr 8) and 0xFF).toByte()
        buffer[3] = (clampedDy and 0xFF).toByte()
        buffer[4] = ((clampedDy shr 8) and 0xFF).toByte()
        buffer[5] = clampedWheel.toByte()
        buffer[6] = clampedPan.toByte()
        return buffer
    }

    fun createMouseReport(
        buttons: Byte,
        dx: Int,
        dy: Int,
        wheel: Int = 0,
        pan: Int = 0
    ): ByteArray {
        val buffer = ByteArray(REPORT_MOUSE_SIZE)
        return packMouseReport(buffer, buttons, dx, dy, wheel, pan)
    }

    /**
     * Constructs a Consumer Control report (e.g. Media/Brightness).
     */
    fun createConsumerReport(usageCode: Int): ByteArray {
        return byteArrayOf(
            (usageCode and 0xFF).toByte(),
            ((usageCode shr 8) and 0xFF).toByte()
        )
    }

    /**
     * Constructs a Battery percentage report (0..100).
     */
    fun createBatteryReport(level: Int): ByteArray {
        return byteArrayOf(level.coerceIn(0, 100).toByte())
    }
}
