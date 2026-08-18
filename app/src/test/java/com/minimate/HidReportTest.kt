package com.minimate

import com.minimate.bluetooth.HidDescriptor
import com.minimate.bluetooth.HidReport
import org.junit.Assert.assertEquals
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
        assertEquals(0x00.toByte(), report[1]) // dx low
        assertEquals(0x00.toByte(), report[2]) // dx high
        assertEquals(0x00.toByte(), report[3]) // dy low
        assertEquals(0x00.toByte(), report[4]) // dy high
        assertEquals(0x00.toByte(), report[5]) // wheel
        assertEquals(0x00.toByte(), report[6]) // pan
    }

    @Test
    fun testMouseReport16BitDeltaPacking() {
        val report = HidReport.createMouseReport(
            buttons = HidDescriptor.BUTTON_NONE,
            dx = 300,
            dy = -500,
            wheel = 5,
            pan = -3
        )

        // 300 = 0x012C -> low: 0x2C, high: 0x01
        assertEquals(0x2C.toByte(), report[1])
        assertEquals(0x01.toByte(), report[2])

        // -500 = 0xFE0C -> low: 0x0C, high: 0xFE
        assertEquals(0x0C.toByte(), report[3])
        assertEquals(0xFE.toByte(), report[4])

        assertEquals(5.toByte(), report[5])
        assertEquals((-3).toByte(), report[6])
    }

    @Test
    fun testBatteryReportPacking() {
        val report = HidReport.createBatteryReport(85)
        assertEquals(1L, report.size.toLong())
        assertEquals(85.toByte(), report[0])
    }
}
