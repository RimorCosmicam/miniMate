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
final class MicrophoneMonitor {
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

    func start() throws {
        guard !running else { return }
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
