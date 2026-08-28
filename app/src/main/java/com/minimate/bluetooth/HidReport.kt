package com.minimate.bluetooth

/**
 * Pre-allocated, zero-garbage report builders for high-frequency HID transmission.
 */
object HidReport {

    // Boot-compatible mouse payload: buttons, X, Y, vertical wheel, horizontal pan.
    const val REPORT_MOUSE_SIZE = 5
    const val REPORT_KEYBOARD_SIZE = 8
    const val REPORT_CONSUMER_SIZE = 2
    const val REPORT_BATTERY_SIZE = 1

    /**
     * Constructs a mouse input report.
     * @param buttons Bitmask of active buttons (BUTTON_LEFT, BUTTON_RIGHT, etc.)
     * @param dx Relative X movement (-127..127)
     * @param dy Relative Y movement (-127..127)
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
        require(buffer.size >= REPORT_MOUSE_SIZE) { "Mouse report buffer must be at least $REPORT_MOUSE_SIZE bytes" }
        val clampedDx = dx.coerceIn(-127, 127)
        val clampedDy = dy.coerceIn(-127, 127)
        val clampedWheel = wheel.coerceIn(-127, 127)
        val clampedPan = pan.coerceIn(-127, 127)

        buffer[0] = buttons
        buffer[1] = clampedDx.toByte()
        buffer[2] = clampedDy.toByte()
        buffer[3] = clampedWheel.toByte()
        buffer[4] = clampedPan.toByte()
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

    /** Standard keyboard report: modifiers, reserved byte, then up to six usages. */
    fun packKeyboardReport(
        buffer: ByteArray,
        modifiers: Byte,
        keyCodes: ByteArray = byteArrayOf()
    ): ByteArray {
        require(buffer.size >= REPORT_KEYBOARD_SIZE) { "Keyboard report buffer must be at least $REPORT_KEYBOARD_SIZE bytes" }
        buffer.fill(0)
        buffer[0] = modifiers
        keyCodes.take(6).forEachIndexed { index, keyCode -> buffer[index + 2] = keyCode }
        return buffer
    }

    fun createKeyboardReport(
        modifiers: Byte = 0,
        vararg keyCodes: Byte
    ): ByteArray = packKeyboardReport(ByteArray(REPORT_KEYBOARD_SIZE), modifiers, keyCodes)

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
