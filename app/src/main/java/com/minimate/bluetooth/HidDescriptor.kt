package com.minimate.bluetooth

import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings

/**
 * Standard USB HID Report Descriptor and SDP/QOS configurations for Minimate.
 * Composite mouse and keyboard descriptor. Android's BluetoothHidDevice API carries
 * the report ID separately from each payload, so report buffers never include it.
 */
object HidDescriptor {

    const val REPORT_ID_MOUSE: Byte = 1
    const val REPORT_ID_KEYBOARD: Byte = 2
    const val REPORT_ID_CONSUMER: Byte = 3

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
    val COMPOSITE_REPORT_DESCRIPTOR = byteArrayOf(
        0x05.toByte(), 0x01.toByte(),       // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x02.toByte(),       // USAGE (Mouse)
        0xA1.toByte(), 0x01.toByte(),       // COLLECTION (Application)
        0x85.toByte(), REPORT_ID_MOUSE,      //   REPORT_ID (Mouse)
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
        0xC0.toByte(),                      // END_COLLECTION

        // Standard 8-byte keyboard input report + LED output report.
        0x05.toByte(), 0x01.toByte(),       // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x06.toByte(),       // USAGE (Keyboard)
        0xA1.toByte(), 0x01.toByte(),       // COLLECTION (Application)
        0x85.toByte(), REPORT_ID_KEYBOARD,   //   REPORT_ID (Keyboard)
        0x05.toByte(), 0x07.toByte(),       //   USAGE_PAGE (Keyboard)
        0x19.toByte(), 0xE0.toByte(),       //   USAGE_MINIMUM (Left Control)
        0x29.toByte(), 0xE7.toByte(),       //   USAGE_MAXIMUM (Right GUI)
        0x15.toByte(), 0x00.toByte(),       //   LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(),       //   LOGICAL_MAXIMUM (1)
        0x75.toByte(), 0x01.toByte(),       //   REPORT_SIZE (1)
        0x95.toByte(), 0x08.toByte(),       //   REPORT_COUNT (8)
        0x81.toByte(), 0x02.toByte(),       //   INPUT (Data,Var,Abs) modifiers
        0x95.toByte(), 0x01.toByte(),       //   REPORT_COUNT (1)
        0x75.toByte(), 0x08.toByte(),       //   REPORT_SIZE (8)
        0x81.toByte(), 0x03.toByte(),       //   INPUT (Constant) reserved
        0x95.toByte(), 0x05.toByte(),       //   REPORT_COUNT (5)
        0x75.toByte(), 0x01.toByte(),       //   REPORT_SIZE (1)
        0x05.toByte(), 0x08.toByte(),       //   USAGE_PAGE (LEDs)
        0x19.toByte(), 0x01.toByte(),       //   USAGE_MINIMUM (Num Lock)
        0x29.toByte(), 0x05.toByte(),       //   USAGE_MAXIMUM (Kana)
        0x91.toByte(), 0x02.toByte(),       //   OUTPUT (Data,Var,Abs)
        0x95.toByte(), 0x01.toByte(),       //   REPORT_COUNT (1)
        0x75.toByte(), 0x03.toByte(),       //   REPORT_SIZE (3)
        0x91.toByte(), 0x03.toByte(),       //   OUTPUT (Constant) padding
        0x95.toByte(), 0x06.toByte(),       //   REPORT_COUNT (6)
        0x75.toByte(), 0x08.toByte(),       //   REPORT_SIZE (8)
        0x15.toByte(), 0x00.toByte(),       //   LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x65.toByte(),       //   LOGICAL_MAXIMUM (101)
        0x05.toByte(), 0x07.toByte(),       //   USAGE_PAGE (Keyboard)
        0x19.toByte(), 0x00.toByte(),       //   USAGE_MINIMUM (Reserved)
        0x29.toByte(), 0x65.toByte(),       //   USAGE_MAXIMUM (Keyboard Application)
        0x81.toByte(), 0x00.toByte(),       //   INPUT (Data,Array,Abs)
        0xC0.toByte(),                      // END_COLLECTION

        // Native media, volume, and display-brightness controls.
        0x05.toByte(), 0x0C.toByte(),       // USAGE_PAGE (Consumer)
        0x09.toByte(), 0x01.toByte(),       // USAGE (Consumer Control)
        0xA1.toByte(), 0x01.toByte(),       // COLLECTION (Application)
        0x85.toByte(), REPORT_ID_CONSUMER,  //   REPORT_ID (Consumer Control)
        0x15.toByte(), 0x00.toByte(),       //   LOGICAL_MINIMUM (0)
        0x26.toByte(), 0xFF.toByte(), 0x03.toByte(), // LOGICAL_MAXIMUM (1023)
        0x19.toByte(), 0x00.toByte(),       //   USAGE_MINIMUM (0)
        0x2A.toByte(), 0xFF.toByte(), 0x03.toByte(), // USAGE_MAXIMUM (1023)
        0x75.toByte(), 0x10.toByte(),       //   REPORT_SIZE (16)
        0x95.toByte(), 0x01.toByte(),       //   REPORT_COUNT (1)
        0x81.toByte(), 0x00.toByte(),       //   INPUT (Data,Array,Abs)
        0xC0.toByte()                       // END_COLLECTION
    )

    /**
     * Bluetooth SDP Settings for Minimate.
     */
    fun createSdpSettings(): BluetoothHidDeviceAppSdpSettings {
        return BluetoothHidDeviceAppSdpSettings(
            "MiniMate Input",
            "Trackpad and keyboard for Z Flip",
            "Minimate Inc.",
            0xC0.toByte(), // Combo keyboard + pointing device
            COMPOSITE_REPORT_DESCRIPTOR
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
