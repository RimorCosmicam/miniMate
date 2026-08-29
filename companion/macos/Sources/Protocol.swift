import Foundation

enum MMAudioProtocol {
    static let magic: UInt32 = 0x4D4D4155
    static let version: UInt8 = 1
    static let serviceUUID = UUID(uuidString: "f7f8c3a4-6bc7-4b7a-9c35-3b41af96e9d2")!
    static let wifiPort: UInt16 = 42308
    static let typePlayback: UInt8 = 1
    static let typeMicrophone: UInt8 = 2
    static let typeHello: UInt8 = 3
    static let typeState: UInt8 = 4
    static let typePing: UInt8 = 5
    static let typeWebcamJPEG: UInt8 = 6
    static let typeWebcamConfig: UInt8 = 7
    static let codecPCM16: UInt8 = 0
    static let codecIMA: UInt8 = 1
    static let codecPCM24: UInt8 = 2

    struct Frame {
        let type: UInt8
        let codec: UInt8
        let channels: UInt8
        let sampleRate: UInt32
        let sequence: UInt32
        let payload: Data

        var encoded: Data {
            var data = Data()
            data.appendBE(MMAudioProtocol.magic)
            data.append(MMAudioProtocol.version)
            data.append(type)
            data.append(codec)
            data.append(channels)
            data.appendBE(sampleRate)
            data.appendBE(sequence)
            data.appendBE(UInt32(payload.count))
            data.append(payload)
            return data
        }
    }

    final class Parser {
        private var buffer = Data()
        func append(_ data: Data) throws -> [Frame] {
            buffer.append(data)
            var frames: [Frame] = []
            while buffer.count >= 20 {
                guard buffer.readBEUInt32(at: 0) == magic else { throw BridgeError.invalidFrame }
                guard buffer[4] == version else { throw BridgeError.unsupportedVersion }
                let length = Int(buffer.readBEUInt32(at: 16))
                guard length <= 4 * 1_024 * 1_024 else { throw BridgeError.invalidFrame }
                guard buffer.count >= 20 + length else { break }
                frames.append(Frame(
                    type: buffer[5], codec: buffer[6], channels: buffer[7],
                    sampleRate: buffer.readBEUInt32(at: 8),
                    sequence: buffer.readBEUInt32(at: 12),
                    payload: buffer.subdata(in: 20..<(20 + length))
                ))
                buffer.removeSubrange(0..<(20 + length))
            }
            return frames
        }
    }

    enum BridgeError: Error { case invalidFrame, unsupportedVersion, noService, connectionFailed }
}

extension Data {
    mutating func appendBE<T: FixedWidthInteger>(_ value: T) {
        var big = value.bigEndian
        Swift.withUnsafeBytes(of: &big) { append(contentsOf: $0) }
    }

    func readBEUInt32(at offset: Int) -> UInt32 {
        UInt32(self[offset]) << 24 | UInt32(self[offset + 1]) << 16 |
        UInt32(self[offset + 2]) << 8 | UInt32(self[offset + 3])
    }
}

enum IMAADPCM {
    private static let steps = [
        7,8,9,10,11,12,13,14,16,17,19,21,23,25,28,31,34,37,41,45,50,55,60,66,73,80,88,97,107,118,
        130,143,157,173,190,209,230,253,279,307,337,371,408,449,494,544,598,658,724,796,876,963,
        1060,1166,1282,1411,1552,1707,1878,2066,2272,2499,2749,3024,3327,3660,4026,4428,4871,
        5358,5894,6484,7132,7845,8630,9493,10442,11487,12635,13899,15289,16818,18500,20350,
        22385,24623,27086,29794,32767
    ]
    private static let indexes = [-1,-1,-1,-1,2,4,6,8]

    static func encode(_ samples: [Int16], channels: Int) -> Data {
        guard channels > 0, samples.count >= channels else { return Data() }
        let frames = samples.count / channels
        var predictor = (0..<channels).map { Int(samples[$0]) }
        var index = Array(repeating: 0, count: channels)
        if frames > 1 {
            for channel in 0..<channels {
                let delta = abs(Int(samples[channels + channel]) - predictor[channel])
                while index[channel] < 88 && steps[index[channel]] < delta { index[channel] += 1 }
                index[channel] = max(0, index[channel] - 3)
            }
        }
        var output = Data()
        for channel in 0..<channels {
            var p = Int16(predictor[channel]).littleEndian
            Swift.withUnsafeBytes(of: &p) { output.append(contentsOf: $0) }
            output.append(UInt8(index[channel])); output.append(0)
        }
        var pending: UInt8?
        for frame in 1..<frames {
            for channel in 0..<channels {
                let nibble = encodeOne(Int(samples[frame * channels + channel]), channel, &predictor, &index)
                if let low = pending { output.append(low | UInt8(nibble << 4)); pending = nil }
                else { pending = UInt8(nibble) }
            }
        }
        if let pending { output.append(pending) }
        return output
    }

    static func decode(_ data: Data, channels: Int) -> [Int16] {
        guard channels > 0, data.count >= channels * 4 else { return [] }
        var predictor = Array(repeating: 0, count: channels)
        var index = Array(repeating: 0, count: channels)
        for channel in 0..<channels {
            let offset = channel * 4
            predictor[channel] = Int(Int16(bitPattern: UInt16(data[offset]) | UInt16(data[offset + 1]) << 8))
            index[channel] = min(88, Int(data[offset + 2]))
        }
        let nibbleCount = (data.count - channels * 4) * 2
        let complete = nibbleCount / channels * channels
        var result = predictor.map(Int16.init)
        for n in 0..<complete {
            let byte = data[channels * 4 + n / 2]
            let nibble = n % 2 == 0 ? Int(byte & 0x0F) : Int(byte >> 4)
            let channel = n % channels
            result.append(Int16(decodeOne(nibble, channel, &predictor, &index)))
        }
        return result
    }

    private static func encodeOne(_ sample: Int, _ channel: Int, _ predictor: inout [Int], _ index: inout [Int]) -> Int {
        var delta = sample - predictor[channel], nibble = 0
        if delta < 0 { nibble = 8; delta = -delta }
        let step = steps[index[channel]]
        var diff = step >> 3
        if delta >= step { nibble |= 4; delta -= step; diff += step }
        if delta >= step >> 1 { nibble |= 2; delta -= step >> 1; diff += step >> 1 }
        if delta >= step >> 2 { nibble |= 1; diff += step >> 2 }
        predictor[channel] = min(32767, max(-32768, predictor[channel] + (nibble & 8 == 0 ? diff : -diff)))
        index[channel] = min(88, max(0, index[channel] + indexes[nibble & 7]))
        return nibble
    }

    private static func decodeOne(_ nibble: Int, _ channel: Int, _ predictor: inout [Int], _ index: inout [Int]) -> Int {
        let step = steps[index[channel]]
        var diff = step >> 3
        if nibble & 4 != 0 { diff += step }
        if nibble & 2 != 0 { diff += step >> 1 }
        if nibble & 1 != 0 { diff += step >> 2 }
        predictor[channel] = min(32767, max(-32768, predictor[channel] + (nibble & 8 == 0 ? diff : -diff)))
        index[channel] = min(88, max(0, index[channel] + indexes[nibble & 7]))
        return predictor[channel]
    }
}
