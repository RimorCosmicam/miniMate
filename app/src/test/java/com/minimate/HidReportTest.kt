package com.minimate

import com.minimate.bluetooth.HidDescriptor
import com.minimate.bluetooth.HidReport
import com.minimate.touchpad.model.DEFAULT_KEYBOARD_SHORTCUTS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HidReportTest {

    @Test
    fun testMouseReportButtonPacking() {
        val report = HidReport.createMouseReport(
            buttons = (HidDescriptor.BUTTON_LEFT.toInt() or HidDescriptor.BUTTON_RIGHT.toInt()).toByte(),
            dx = 0,
            dy = 0,
            wheel = 0,
            pan = 0
        )

        assertEquals(HidReport.REPORT_MOUSE_SIZE.toLong(), report.size.toLong())
        assertEquals(0x03.toByte(), report[0]) // Buttons Left (0x01) + Right (0x02)
        assertEquals(0x00.toByte(), report[1]) // dx
        assertEquals(0x00.toByte(), report[2]) // dy
        assertEquals(0x00.toByte(), report[3]) // wheel
        assertEquals(0x00.toByte(), report[4]) // pan
    }

    @Test
    fun testMouseReportAxisOrderAndClamping() {
        val report = HidReport.createMouseReport(
            buttons = HidDescriptor.BUTTON_NONE,
            dx = 300,
            dy = -500,
            wheel = 5,
            pan = -3
        )

        assertEquals(127.toByte(), report[1])
        assertEquals((-127).toByte(), report[2])
        assertEquals(5.toByte(), report[3])
        assertEquals((-3).toByte(), report[4])
    }

    @Test
    fun testBatteryReportPacking() {
        val report = HidReport.createBatteryReport(85)
        assertEquals(1L, report.size.toLong())
        assertEquals(85.toByte(), report[0])
    }

    @Test
    fun testKeyboardReportPackingAndRelease() {
        val pressed = HidReport.createKeyboardReport(0x03, 0x04, 0x05)

        assertEquals(HidReport.REPORT_KEYBOARD_SIZE.toLong(), pressed.size.toLong())
        assertEquals(0x03.toByte(), pressed[0])
        assertEquals(0x00.toByte(), pressed[1])
        assertEquals(0x04.toByte(), pressed[2])
        assertEquals(0x05.toByte(), pressed[3])
        assertEquals(0x00.toByte(), pressed[7])

        val released = HidReport.createKeyboardReport()
        released.forEach { assertEquals(0x00.toByte(), it) }
    }

    @Test
    fun compositeDescriptorDeclaresMouseKeyboardAndConsumerReportIds() {
        val descriptor = HidDescriptor.COMPOSITE_REPORT_DESCRIPTOR
        val ids = (0 until descriptor.lastIndex)
            .filter { descriptor[it] == 0x85.toByte() }
            .map { descriptor[it + 1] }

        assertEquals(
            listOf(HidDescriptor.REPORT_ID_MOUSE, HidDescriptor.REPORT_ID_KEYBOARD, HidDescriptor.REPORT_ID_CONSUMER),
            ids
        )
    }

    @Test
    fun consumerReportPacksLittleEndianUsageAndRelease() {
        val playPause = HidReport.createConsumerReport(0x00CD)
        assertEquals(2L, playPause.size.toLong())
        assertEquals(0xCD.toByte(), playPause[0])
        assertEquals(0x00.toByte(), playPause[1])
        HidReport.createConsumerReport(0).forEach { assertEquals(0x00.toByte(), it) }
    }

    @Test
    fun defaultMacShortcutsAreValidDistinctHidChords() {
        assertEquals(DEFAULT_KEYBOARD_SHORTCUTS.size, DEFAULT_KEYBOARD_SHORTCUTS.map { it.label }.distinct().size)
        assertEquals(DEFAULT_KEYBOARD_SHORTCUTS.size, DEFAULT_KEYBOARD_SHORTCUTS.map { it.modifiers to it.usage }.distinct().size)
        DEFAULT_KEYBOARD_SHORTCUTS.forEach { shortcut ->
            assertTrue(shortcut.modifiers in 0..0x0F)
            assertTrue(shortcut.usage in 1..0xFF)
        }
    }
}
