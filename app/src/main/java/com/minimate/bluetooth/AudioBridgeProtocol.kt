package com.minimate.bluetooth

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.util.UUID

object AudioBridgeProtocol {
    val SERVICE_UUID: UUID = UUID.fromString("f7f8c3a4-6bc7-4b7a-9c35-3b41af96e9d2")
    const val SERVICE_NAME = "MiniMate Audio"
    const val MAGIC = 0x4D4D4155 // MMAU
    const val VERSION = 1
    const val SAMPLE_RATE = 32_000
    const val PLAYBACK_CHANNELS = 2
    const val MICROPHONE_CHANNELS = 1
    const val FRAMES_PER_PACKET = 640 // 20 ms at 32 kHz

    const val TYPE_PLAYBACK = 1
    const val TYPE_MICROPHONE = 2
    const val TYPE_HELLO = 3
    const val TYPE_STATE = 4
    const val TYPE_PING = 5
    const val TYPE_WEBCAM_JPEG = 6
    const val TYPE_WEBCAM_CONFIG = 7
    const val CODEC_PCM16 = 0
    const val CODEC_IMA_ADPCM = 1
    const val CODEC_PCM24 = 2
    const val CODEC_JPEG = 10
    const val CODEC_JSON = 11
    private const val MAX_PAYLOAD = 4 * 1024 * 1024

    data class Frame(
        val type: Int,
        val codec: Int,
        val channels: Int,
        val sampleRate: Int,
        val sequence: Int,
        val payload: ByteArray
    )

    fun write(output: DataOutputStream, frame: Frame) {
        require(frame.payload.size <= MAX_PAYLOAD)
        output.writeInt(MAGIC)
        output.writeByte(VERSION)
        output.writeByte(frame.type)
        output.writeByte(frame.codec)
        output.writeByte(frame.channels)
        output.writeInt(frame.sampleRate)
        output.writeInt(frame.sequence)
        output.writeInt(frame.payload.size)
        output.write(frame.payload)
        output.flush()
    }

    fun read(input: DataInputStream): Frame {
        val magic = input.readInt()
        if (magic != MAGIC) throw IllegalStateException("Invalid MiniMate audio frame")
        val version = input.readUnsignedByte()
        if (version != VERSION) throw IllegalStateException("Unsupported protocol version $version")
        val type = input.readUnsignedByte()
        val codec = input.readUnsignedByte()
        val channels = input.readUnsignedByte()
        val sampleRate = input.readInt()
        val sequence = input.readInt()
        val length = input.readInt()
        if (length !in 0..MAX_PAYLOAD) throw IllegalStateException("Invalid payload length $length")
        val payload = ByteArray(length)
        try {
            input.readFully(payload)
        } catch (e: EOFException) {
            throw e
        }
        return Frame(type, codec, channels, sampleRate, sequence, payload)
    }
}
