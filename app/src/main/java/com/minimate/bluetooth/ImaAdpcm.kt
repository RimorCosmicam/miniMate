package com.minimate.bluetooth

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/** Packet-local IMA ADPCM. Every packet carries fresh predictor/index state per channel. */
object ImaAdpcm {
    private val stepTable = intArrayOf(
        7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25, 28, 31, 34, 37, 41,
        45, 50, 55, 60, 66, 73, 80, 88, 97, 107, 118, 130, 143, 157, 173, 190, 209,
        230, 253, 279, 307, 337, 371, 408, 449, 494, 544, 598, 658, 724, 796, 876,
        963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066, 2272, 2499, 2749,
        3024, 3327, 3660, 4026, 4428, 4871, 5358, 5894, 6484, 7132, 7845, 8630,
        9493, 10442, 11487, 12635, 13899, 15289, 16818, 18500, 20350, 22385, 24623,
        27086, 29794, 32767
    )
    private val indexTable = intArrayOf(-1, -1, -1, -1, 2, 4, 6, 8)

    fun encode(samples: ShortArray, channels: Int): ByteArray {
        require(channels in 1..2 && samples.size >= channels && samples.size % channels == 0)
        val frames = samples.size / channels
        val nibbleCount = (frames - 1) * channels
        val output = ByteBuffer.allocate(channels * 4 + (nibbleCount + 1) / 2).order(ByteOrder.LITTLE_ENDIAN)
        val predictor = IntArray(channels) { samples[it].toInt() }
        val index = IntArray(channels) { initialIndex(samples, channels, it) }
        repeat(channels) { channel ->
            output.putShort(predictor[channel].toShort())
            output.put(index[channel].toByte())
            output.put(0)
        }
        var packed = 0
        var lowNibble = true
        for (frame in 1 until frames) {
            for (channel in 0 until channels) {
                val nibble = encodeSample(samples[frame * channels + channel].toInt(), channel, predictor, index)
                if (lowNibble) {
                    packed = nibble
                    lowNibble = false
                } else {
                    output.put((packed or (nibble shl 4)).toByte())
                    lowNibble = true
                }
            }
        }
        if (!lowNibble) output.put(packed.toByte())
        return output.array()
    }

    fun decode(data: ByteArray, channels: Int): ShortArray {
        require(channels in 1..2 && data.size >= channels * 4)
        val input = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val predictor = IntArray(channels)
        val index = IntArray(channels)
        repeat(channels) { channel ->
            predictor[channel] = input.short.toInt()
            index[channel] = input.get().toInt().and(0xFF).coerceIn(0, 88)
            input.get()
        }
        val nibbleCount = (data.size - channels * 4) * 2
        val completeFrames = nibbleCount / channels
        val output = ShortArray((completeFrames + 1) * channels)
        repeat(channels) { output[it] = predictor[it].toShort() }
        var nibbleIndex = 0
        while (nibbleIndex < completeFrames * channels) {
            val packed = data[channels * 4 + nibbleIndex / 2].toInt().and(0xFF)
            val nibble = if (nibbleIndex and 1 == 0) packed and 0x0F else packed ushr 4
            val channel = nibbleIndex % channels
            output[channels + nibbleIndex] = decodeNibble(nibble, channel, predictor, index).toShort()
            nibbleIndex++
        }
        return output
    }

    private fun initialIndex(samples: ShortArray, channels: Int, channel: Int): Int {
        if (samples.size < channels * 2) return 0
        val delta = abs(samples[channels + channel].toInt() - samples[channel].toInt())
        var best = 0
        while (best < 88 && stepTable[best] < delta) best++
        return (best - 3).coerceIn(0, 88)
    }

    private fun encodeSample(sample: Int, channel: Int, predictor: IntArray, index: IntArray): Int {
        var delta = sample - predictor[channel]
        var nibble = 0
        if (delta < 0) { nibble = 8; delta = -delta }
        val step = stepTable[index[channel]]
        var difference = step shr 3
        if (delta >= step) { nibble = nibble or 4; delta -= step; difference += step }
        if (delta >= step shr 1) { nibble = nibble or 2; delta -= step shr 1; difference += step shr 1 }
        if (delta >= step shr 2) { nibble = nibble or 1; difference += step shr 2 }
        predictor[channel] = (predictor[channel] + if (nibble and 8 != 0) -difference else difference).coerceIn(-32768, 32767)
        index[channel] = (index[channel] + indexTable[nibble and 7]).coerceIn(0, 88)
        return nibble
    }

    private fun decodeNibble(nibble: Int, channel: Int, predictor: IntArray, index: IntArray): Int {
        val step = stepTable[index[channel]]
        var difference = step shr 3
        if (nibble and 4 != 0) difference += step
        if (nibble and 2 != 0) difference += step shr 1
        if (nibble and 1 != 0) difference += step shr 2
        predictor[channel] = (predictor[channel] + if (nibble and 8 != 0) -difference else difference).coerceIn(-32768, 32767)
        index[channel] = (index[channel] + indexTable[nibble and 7]).coerceIn(0, 88)
        return predictor[channel]
    }
}
