package com.minimate.bluetooth

import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings

/**
 * Standard USB HID Report Descriptor and SDP/QOS configurations for Minimate.
 * Uses an unnumbered, boot-compatible mouse report. Some hosts cache or negotiate
 * boot mouse mode; numbered composite reports then become byte-shifted and map X
 * movement onto Y. This layout works identically in boot and report mode.
 */
object HidDescriptor {

    const val REPORT_ID_MOUSE: Byte = 0

    // Button masks
    const val BUTTON_NONE: Byte = 0x00
    const val BUTTON_LEFT: Byte = 0x01
    const val BUTTON_RIGHT: Byte = 0x02
    const val BUTTON_MIDDLE: Byte = 0x04
    const val BUTTON_BACK: Byte = 0x08
    const val BUTTON_FORWARD: Byte = 0x10

    /**
     * Complete USB HID Report Descriptor conforming to USB-IF HID 1.11.
     */
    val MOUSE_REPORT_DESCRIPTOR = byteArrayOf(
        0x05.toByte(), 0x01.toByte(),       // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x02.toByte(),       // USAGE (Mouse)
        0xA1.toByte(), 0x01.toByte(),       // COLLECTION (Application)
        0x09.toByte(), 0x01.toByte(),       //   USAGE (Pointer)
        0xA1.toByte(), 0x00.toByte(),       //   COLLECTION (Physical)

        // 5 Buttons (Bits 0..4) + 3 Bits Padding
        0x05.toByte(), 0x09.toByte(),       //     USAGE_PAGE (Button)
        0x19.toByte(), 0x01.toByte(),       //     USAGE_MINIMUM (Button 1)
        0x29.toByte(), 0x05.toByte(),       //     USAGE_MAXIMUM (Button 5)
        0x15.toByte(), 0x00.toByte(),       //     LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(),       //     LOGICAL_MAXIMUM (1)
        0x95.toByte(), 0x05.toByte(),       //     REPORT_COUNT (5)
        0x75.toByte(), 0x01.toByte(),       //     REPORT_SIZE (1)
        0x81.toByte(), 0x02.toByte(),       //     INPUT (Data,Var,Abs)
        0x95.toByte(), 0x01.toByte(),       //     REPORT_COUNT (1)
        0x75.toByte(), 0x03.toByte(),       //     REPORT_SIZE (3)
        0x81.toByte(), 0x03.toByte(),       //     INPUT (Cnst,Var,Abs) - Padding

        // X & Y Relative Movement (8-bit boot-compatible values)
        0x05.toByte(), 0x01.toByte(),       //     USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x30.toByte(),       //     USAGE (X)
        0x09.toByte(), 0x31.toByte(),       //     USAGE (Y)
        0x15.toByte(), 0x81.toByte(),       //     LOGICAL_MINIMUM (-127)
        0x25.toByte(), 0x7F.toByte(),       //     LOGICAL_MAXIMUM (127)
        0x75.toByte(), 0x08.toByte(),       //     REPORT_SIZE (8)
        0x95.toByte(), 0x02.toByte(),       //     REPORT_COUNT (2)
        0x81.toByte(), 0x06.toByte(),       //     INPUT (Data,Var,Rel)

        // Vertical Scroll Wheel (-127..127)
        0x09.toByte(), 0x38.toByte(),       //     USAGE (Wheel)
        0x15.toByte(), 0x81.toByte(),       //     LOGICAL_MINIMUM (-127)
        0x25.toByte(), 0x7F.toByte(),       //     LOGICAL_MAXIMUM (127)
        0x75.toByte(), 0x08.toByte(),       //     REPORT_SIZE (8)
        0x95.toByte(), 0x01.toByte(),       //     REPORT_COUNT (1)
        0x81.toByte(), 0x06.toByte(),       //     INPUT (Data,Var,Rel)

        // Horizontal AC Pan (Scroll) (-127..127)
        0x05.toByte(), 0x0C.toByte(),       //     USAGE_PAGE (Consumer Devices)
        0x0A.toByte(), 0x38.toByte(), 0x02.toByte(), // USAGE (AC Pan)
        0x15.toByte(), 0x81.toByte(),       //     LOGICAL_MINIMUM (-127)
        0x25.toByte(), 0x7F.toByte(),       //     LOGICAL_MAXIMUM (127)
        0x75.toByte(), 0x08.toByte(),       //     REPORT_SIZE (8)
        0x95.toByte(), 0x01.toByte(),       //     REPORT_COUNT (1)
        0x81.toByte(), 0x06.toByte(),       //     INPUT (Data,Var,Rel)

        0xC0.toByte(),                      //   END_COLLECTION
        0xC0.toByte()                       // END_COLLECTION
    )

    /**
     * Bluetooth SDP Settings for Minimate.
     */
    fun createSdpSettings(): BluetoothHidDeviceAppSdpSettings {
        return BluetoothHidDeviceAppSdpSettings(
            "Minimate Trackpad",
            "Magic Trackpad for Z Flip",
            "Minimate Inc.",
            0x80.toByte(), // Pointing Device (Mouse/Trackpad)
            MOUSE_REPORT_DESCRIPTOR
        )
    }

    /**
     * Bluetooth QOS Settings targeting ultra-low latency (~11ms service latency).
     */
    fun createQosSettings(): BluetoothHidDeviceAppQosSettings {
        return BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_GUARANTEED,
            800,
            9,
            0,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX
        )
    }
}
