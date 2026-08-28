import AVFoundation
import CoreAudio
import CoreMedia
import Foundation
import ScreenCaptureKit

final class DesktopAudioCapture: NSObject, SCStreamOutput, SCStreamDelegate {
    var onPCM24: ((Data) -> Void)?
    var onError: ((Error) -> Void)?
    private var stream: SCStream?
    private var pending: [Float] = []
    private let queue = DispatchQueue(label: "MiniMate.ScreenAudio", qos: .userInteractive)

    func start() async throws {
        let content = try await SCShareableContent.excludingDesktopWindows(false, onScreenWindowsOnly: false)
        guard let display = content.displays.first else { throw MMAudioProtocol.BridgeError.connectionFailed }
        let filter = SCContentFilter(display: display, excludingApplications: [], exceptingWindows: [])
        let configuration = SCStreamConfiguration()
        configuration.capturesAudio = true
        configuration.excludesCurrentProcessAudio = true
        configuration.sampleRate = 48_000
        configuration.channelCount = 2
        configuration.width = 2
        configuration.height = 2
        configuration.minimumFrameInterval = CMTime(value: 1, timescale: 1)
        configuration.queueDepth = 3
        let stream = SCStream(filter: filter, configuration: configuration, delegate: self)
        try stream.addStreamOutput(self, type: .audio, sampleHandlerQueue: queue)
        self.stream = stream
        try await stream.startCapture()
    }

    func stop() async {
        try? await stream?.stopCapture()
        stream = nil
        pending.removeAll(keepingCapacity: false)
    }

    func stream(_ stream: SCStream, didOutputSampleBuffer sampleBuffer: CMSampleBuffer, of outputType: SCStreamOutputType) {
        guard outputType == .audio, sampleBuffer.isValid else { return }
        try? sampleBuffer.withAudioBufferList { list, _ in
            guard let description = sampleBuffer.formatDescription?.audioStreamBasicDescription,
                  let format = AVAudioFormat(
                    standardFormatWithSampleRate: description.mSampleRate,
                    channels: description.mChannelsPerFrame
                  ),
                  let pcm = AVAudioPCMBuffer(pcmFormat: format, bufferListNoCopy: list.unsafePointer),
                  let channelData = pcm.floatChannelData else { return }
            let channels = Int(pcm.format.channelCount)
            for frame in 0..<Int(pcm.frameLength) {
                let left = channelData[0][frame]
                let right = channels > 1 ? channelData[1][frame] : left
                pending.append(left)
                pending.append(right)
            }
            while pending.count >= 960 * 2 {
                let packet = Array(pending.prefix(960 * 2))
                pending.removeFirst(960 * 2)
                onPCM24?(Self.packPCM24(packet))
            }
        }
    }

    func stream(_ stream: SCStream, didStopWithError error: Error) { onError?(error) }

    private static func packPCM24(_ samples: [Float]) -> Data {
        var data = Data(capacity: samples.count * 3)
        for value in samples {
            let sample = Int32((max(-1, min(0.9999999, value)) * 8_388_608).rounded())
            data.append(UInt8(truncatingIfNeeded: sample))
            data.append(UInt8(truncatingIfNeeded: sample >> 8))
            data.append(UInt8(truncatingIfNeeded: sample >> 16))
        }
        return data
    }
}

struct MacAudioDevice: Hashable {
    let id: AudioDeviceID
    let name: String
}

enum MacAudioDevices {
    static func outputs() -> [MacAudioDevice] {
        var address = AudioObjectPropertyAddress(
            mSelector: kAudioHardwarePropertyDevices,
            mScope: kAudioObjectPropertyScopeGlobal,
            mElement: kAudioObjectPropertyElementMain
        )
        var size: UInt32 = 0
        guard AudioObjectGetPropertyDataSize(AudioObjectID(kAudioObjectSystemObject), &address, 0, nil, &size) == noErr else { return [] }
        var ids = [AudioDeviceID](repeating: 0, count: Int(size) / MemoryLayout<AudioDeviceID>.size)
        guard AudioObjectGetPropertyData(AudioObjectID(kAudioObjectSystemObject), &address, 0, nil, &size, &ids) == noErr else { return [] }
        return ids.compactMap { id in
            var streamsAddress = AudioObjectPropertyAddress(
                mSelector: kAudioDevicePropertyStreams,
                mScope: kAudioDevicePropertyScopeOutput,
                mElement: kAudioObjectPropertyElementMain
            )
            var streamsSize: UInt32 = 0
            guard AudioObjectGetPropertyDataSize(id, &streamsAddress, 0, nil, &streamsSize) == noErr, streamsSize > 0 else { return nil }
            var nameAddress = AudioObjectPropertyAddress(
                mSelector: kAudioObjectPropertyName,
                mScope: kAudioObjectPropertyScopeGlobal,
                mElement: kAudioObjectPropertyElementMain
            )
            var name: CFString = "Unknown" as CFString
            var nameSize = UInt32(MemoryLayout<CFString>.size)
            guard AudioObjectGetPropertyData(id, &nameAddress, 0, nil, &nameSize, &name) == noErr else { return nil }
            return MacAudioDevice(id: id, name: name as String)
        }.sorted { $0.name < $1.name }
    }
}

final class PhoneMicrophonePlayer {
    private let engine = AVAudioEngine()
    private let player = AVAudioPlayerNode()
    private let format = AVAudioFormat(standardFormatWithSampleRate: 48_000, channels: 1)!
    private var started = false

    init() {
        engine.attach(player)
        engine.connect(player, to: engine.mainMixerNode, format: format)
    }

    func selectOutput(_ device: MacAudioDevice) throws {
        guard let unit = engine.outputNode.audioUnit else { return }
        var id = device.id
        let result = AudioUnitSetProperty(
            unit, kAudioOutputUnitProperty_CurrentDevice, kAudioUnitScope_Global, 0,
            &id, UInt32(MemoryLayout<AudioDeviceID>.size)
        )
        if result != noErr { throw NSError(domain: NSOSStatusErrorDomain, code: Int(result)) }
    }

    func play(frame: MMAudioProtocol.Frame) {
        let samples: [Int16]
        if frame.codec == MMAudioProtocol.codecIMA { samples = IMAADPCM.decode(frame.payload, channels: 1) }
        else if frame.codec == MMAudioProtocol.codecPCM16 { samples = frame.payload.pcm16Samples }
        else { return }
        guard let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(samples.count)),
              let destination = buffer.floatChannelData?[0] else { return }
        buffer.frameLength = AVAudioFrameCount(samples.count)
        for index in samples.indices { destination[index] = Float(samples[index]) / 32768 }
        if !started {
            try? engine.start()
            player.play()
            started = true
        }
        player.scheduleBuffer(buffer)
    }

    func stop() {
        player.stop(); engine.stop(); started = false
    }
}

private extension Data {
    var pcm16Samples: [Int16] {
        stride(from: 0, to: count - 1, by: 2).map {
            Int16(bitPattern: UInt16(self[$0]) | UInt16(self[$0 + 1]) << 8)
        }
    }
}
