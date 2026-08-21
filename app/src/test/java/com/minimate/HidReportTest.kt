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
}
