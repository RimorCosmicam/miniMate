import AVFoundation
import Foundation

/**
 Plays the incoming phone microphone stream straight out of the Mac's speakers or headphones.

 This exists to separate the pipeline from whatever application is consuming the virtual
 microphone. When capture sounds wrong it is otherwise impossible to tell whether the fault is in
 the phone, this companion, the CoreAudio driver, or the conferencing app's own processing —
 Discord and similar clients apply their own noise suppression, gain control and codec, any of
 which can account for a poor result on its own.

 What is played here is exactly the audio handed to the virtual microphone device: same samples,
 same point in the chain, no additional processing. If it sounds correct here and wrong in the
 consuming application, the fault is downstream of this project.
 */
/// One selectable destination for monitoring.
struct MonitorOutput: Identifiable, Hashable {
    let id: AudioDeviceID
    let name: String
}

final class MicrophoneMonitor {
    /// Every output device, MiniMate's own virtual speaker included. Routing there sends the
    /// monitor back to the phone and out the same earphones being captured, which is exactly how
    /// you check the microphone while wearing them. It can feed back — the earphone driver sits
    /// close to the cable microphone — so it is offered rather than imposed, not withheld.
    static func availableOutputs() -> [MonitorOutput] {
        var address = AudioObjectPropertyAddress(
            mSelector: kAudioHardwarePropertyDevices,
            mScope: kAudioObjectPropertyScopeGlobal,
            mElement: kAudioObjectPropertyElementMain)
        var size: UInt32 = 0
        guard AudioObjectGetPropertyDataSize(AudioObjectID(kAudioObjectSystemObject), &address, 0, nil, &size) == noErr else {
            return []
        }
        var ids = [AudioDeviceID](repeating: 0, count: Int(size) / MemoryLayout<AudioDeviceID>.size)
        guard AudioObjectGetPropertyData(AudioObjectID(kAudioObjectSystemObject), &address, 0, nil, &size, &ids) == noErr else {
            return []
        }

        func stringProperty(_ device: AudioDeviceID, _ selector: AudioObjectPropertySelector) -> String {
            var addr = AudioObjectPropertyAddress(mSelector: selector,
                mScope: kAudioObjectPropertyScopeGlobal, mElement: kAudioObjectPropertyElementMain)
            var value: CFString? = nil
            var valueSize = UInt32(MemoryLayout<CFString?>.size)
            guard AudioObjectGetPropertyData(device, &addr, 0, nil, &valueSize, &value) == noErr else { return "" }
            return (value as String?) ?? ""
        }

        func outputChannelCount(_ device: AudioDeviceID) -> Int {
            var addr = AudioObjectPropertyAddress(mSelector: kAudioDevicePropertyStreamConfiguration,
                mScope: kAudioObjectPropertyScopeOutput, mElement: kAudioObjectPropertyElementMain)
            var listSize: UInt32 = 0
            guard AudioObjectGetPropertyDataSize(device, &addr, 0, nil, &listSize) == noErr, listSize > 0 else { return 0 }
            let raw = UnsafeMutableRawPointer.allocate(byteCount: Int(listSize), alignment: 16)
            defer { raw.deallocate() }
            guard AudioObjectGetPropertyData(device, &addr, 0, nil, &listSize, raw) == noErr else { return 0 }
            let buffers = UnsafeMutableAudioBufferListPointer(raw.assumingMemoryBound(to: AudioBufferList.self))
            return buffers.reduce(0) { $0 + Int($1.mNumberChannels) }
        }

        return ids.compactMap { device in
            guard outputChannelCount(device) > 0 else { return nil }
            let name = stringProperty(device, kAudioObjectPropertyName)
            return name.isEmpty ? nil : MonitorOutput(id: device, name: name)
        }
    }

    private let engine = AVAudioEngine()
    private let player = AVAudioPlayerNode()
    private let format: AVAudioFormat
    private var running = false
    /// Bounds how much audio may be queued ahead, so monitoring cannot drift into a growing delay.
    private var scheduledBuffers = 0
    private let maximumScheduledBuffers = 8
    private let lock = NSLock()

    init?(sampleRate: Double = 48_000) {
        guard let format = AVAudioFormat(standardFormatWithSampleRate: sampleRate, channels: 1) else {
            return nil
        }
        self.format = format
        engine.attach(player)
        engine.connect(player, to: engine.mainMixerNode, format: format)
    }

    var isRunning: Bool { running }

    /// Binds playback to a specific output device for this process only, leaving the system
    /// default untouched. Passing nil uses whatever the system default currently is.
    func start(deviceID: AudioDeviceID?) throws {
        guard !running else { return }
        if let deviceID {
            try engine.outputNode.auAudioUnit.setDeviceID(deviceID)
        }
        engine.prepare()
        try engine.start()
        player.play()
        running = true
    }

    func stop() {
        guard running else { return }
        player.stop()
        engine.stop()
        running = false
        lock.lock()
        scheduledBuffers = 0
        lock.unlock()
    }

    /// Accepts the same interleaved mono PCM16 that is written to the virtual microphone.
    func enqueue(pcm16: Data) {
        guard running, !pcm16.isEmpty else { return }

        lock.lock()
        let queued = scheduledBuffers
        lock.unlock()
        // Drop rather than queue without bound: falling behind should cost a brief gap, not a
        // monitor that drifts steadily further behind the speaker.
        guard queued < maximumScheduledBuffers else { return }

        let sampleCount = pcm16.count / 2
        guard sampleCount > 0,
              let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(sampleCount)),
              let channel = buffer.floatChannelData?[0] else { return }
        buffer.frameLength = AVAudioFrameCount(sampleCount)

        pcm16.withUnsafeBytes { raw in
            let samples = raw.bindMemory(to: Int16.self)
            for index in 0..<sampleCount {
                channel[index] = Float(Int16(littleEndian: samples[index])) / 32_768.0
            }
        }

        lock.lock()
        scheduledBuffers += 1
        lock.unlock()
        player.scheduleBuffer(buffer, at: nil, options: []) { [weak self] in
            guard let self else { return }
            self.lock.lock()
            self.scheduledBuffers = max(0, self.scheduledBuffers - 1)
            self.lock.unlock()
        }
    }
}
