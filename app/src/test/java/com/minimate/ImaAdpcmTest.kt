package com.minimate

import com.minimate.bluetooth.ImaAdpcm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class ImaAdpcmTest {
    @Test fun stereoPacketRoundTripsWithBoundedError() {
        val samples = ShortArray(640 * 2) { index ->
            val frame = index / 2
            val frequency = if (index % 2 == 0) 440.0 else 660.0
            (sin(frame * frequency * 2.0 * PI / 32_000.0) * 18_000).toInt().toShort()
        }
        val encoded = ImaAdpcm.encode(samples, 2)
        val decoded = ImaAdpcm.decode(encoded, 2)
        assertEquals(samples.size, decoded.size)
        val meanError = samples.indices.sumOf { abs(samples[it].toInt() - decoded[it].toInt()).toLong() }.toDouble() / samples.size
        assertTrue("mean error=$meanError", meanError < 1800.0)
        assertTrue(encoded.size < samples.size)
    }
}
