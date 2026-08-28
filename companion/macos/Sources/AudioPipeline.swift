import AppKit
import Darwin
import Foundation

/// Lossless localhost IPC between the CoreAudio HAL plug-in and the companion.
final class CoreAudioEndpointBridge {
    static let installedDriverURL = URL(fileURLWithPath: "/Library/Audio/Plug-Ins/HAL/MiniMateAudio.driver")

    var onSpeakerPCM16: ((Data) -> Void)?
    private let queue = DispatchQueue(label: "MiniMate.CoreAudioEndpoints", qos: .userInteractive)
    private var speakerSocket: Int32 = -1
    private var microphoneSocket: Int32 = -1
    private var source: DispatchSourceRead?
    private var pendingSpeaker = Data()

    var isInstalled: Bool { FileManager.default.fileExists(atPath: Self.installedDriverURL.path) }

    func start() throws {
        guard source == nil else { return }
        speakerSocket = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)
        guard speakerSocket >= 0 else { throw POSIXError(.ENOTSOCK) }

        var address = sockaddr_in()
        address.sin_len = UInt8(MemoryLayout<sockaddr_in>.size)
        address.sin_family = sa_family_t(AF_INET)
        address.sin_port = in_port_t(42310).bigEndian
        address.sin_addr = in_addr(s_addr: INADDR_LOOPBACK.bigEndian)
        let bindResult = withUnsafePointer(to: &address) {
            $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                Darwin.bind(speakerSocket, $0, socklen_t(MemoryLayout<sockaddr_in>.size))
            }
        }
        guard bindResult == 0 else {
            let error = POSIXError(POSIXErrorCode(rawValue: errno) ?? .EADDRINUSE)
            close(speakerSocket); speakerSocket = -1
            throw error
        }

        microphoneSocket = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)
        guard microphoneSocket >= 0 else {
            close(speakerSocket); speakerSocket = -1
            throw POSIXError(.ENOTSOCK)
        }

        let source = DispatchSource.makeReadSource(fileDescriptor: speakerSocket, queue: queue)
        source.setEventHandler { [weak self] in self?.readSpeakerPackets() }
        source.setCancelHandler { [weak self] in
            guard let self, self.speakerSocket >= 0 else { return }
            close(self.speakerSocket)
            self.speakerSocket = -1
        }
        self.source = source
        source.resume()
    }

    func stop() {
        source?.cancel()
        source = nil
        pendingSpeaker.removeAll(keepingCapacity: false)
        if microphoneSocket >= 0 { close(microphoneSocket); microphoneSocket = -1 }
    }

    func sendMicrophonePCM16(_ data: Data) {
        guard microphoneSocket >= 0, !data.isEmpty else { return }
        var destination = sockaddr_in()
        destination.sin_len = UInt8(MemoryLayout<sockaddr_in>.size)
        destination.sin_family = sa_family_t(AF_INET)
        destination.sin_port = in_port_t(42311).bigEndian
        destination.sin_addr = in_addr(s_addr: INADDR_LOOPBACK.bigEndian)
        data.withUnsafeBytes { bytes in
            guard let base = bytes.baseAddress else { return }
            withUnsafePointer(to: &destination) {
                $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                    _ = sendto(microphoneSocket, base, bytes.count, MSG_DONTWAIT, $0,
                               socklen_t(MemoryLayout<sockaddr_in>.size))
                }
            }
        }
    }

    private func readSpeakerPackets() {
        var storage = [UInt8](repeating: 0, count: 4096)
        while true {
            let count = recv(speakerSocket, &storage, storage.count, MSG_DONTWAIT)
            if count <= 0 { break }
            pendingSpeaker.append(contentsOf: storage.prefix(count))
            // Exactly 20 ms at 48 kHz, stereo, signed 16-bit. Keeping the network
            // cadence stable also makes the Bluetooth 48 -> 32 kHz conversion exact.
            while pendingSpeaker.count >= 3_840 {
                let packet = Data(pendingSpeaker.prefix(3_840))
                pendingSpeaker.removeFirst(3_840)
                onSpeakerPCM16?(packet)
            }
        }
    }

    static func pcm16StereoToPCM24(_ data: Data) -> Data {
        var output = Data(capacity: data.count / 2 * 3)
        var index = 0
        while index + 1 < data.count {
            let sample = Int16(bitPattern: UInt16(data[index]) | UInt16(data[index + 1]) << 8)
            let expanded = Int32(sample) << 8
            output.append(UInt8(truncatingIfNeeded: expanded))
            output.append(UInt8(truncatingIfNeeded: expanded >> 8))
            output.append(UInt8(truncatingIfNeeded: expanded >> 16))
            index += 2
        }
        return output
    }

    static func microphonePCM16(frame: MMAudioProtocol.Frame) -> Data? {
        if frame.codec == MMAudioProtocol.codecPCM16 { return frame.payload }
        guard frame.codec == MMAudioProtocol.codecIMA else { return nil }
        let samples = IMAADPCM.decode(frame.payload, channels: 1)
        var output = Data(capacity: samples.count * 2)
        for sample in samples {
            output.append(UInt8(truncatingIfNeeded: sample))
            output.append(UInt8(truncatingIfNeeded: sample >> 8))
        }
        return output
    }
}

enum CoreAudioDriverInstaller {
    static func install() throws {
        guard let bundled = Bundle.main.url(forResource: "MiniMateAudio", withExtension: "driver") else {
            throw NSError(domain: "MiniMateAudio", code: 1, userInfo: [
                NSLocalizedDescriptionKey: "The MiniMate audio driver is missing from this app build."
            ])
        }
        let source = shellQuote(bundled.path)
        let destination = shellQuote(CoreAudioEndpointBridge.installedDriverURL.path)
        let command = "mkdir -p /Library/Audio/Plug-Ins/HAL && /usr/bin/ditto \(source) \(destination) && /usr/sbin/chown -R root:wheel \(destination) && /bin/chmod -R a+rX \(destination) && (/usr/bin/killall coreaudiod || true)"
        let script = "do shell script \(appleScriptQuote(command)) with administrator privileges"
        var error: NSDictionary?
        let result = NSAppleScript(source: script)?.executeAndReturnError(&error)
        if result == nil {
            throw NSError(domain: "MiniMateAudio", code: 2, userInfo: [
                NSLocalizedDescriptionKey: error?[NSAppleScript.errorMessage] as? String ?? "Driver installation was cancelled."
            ])
        }
    }

    private static func shellQuote(_ value: String) -> String {
        "'" + value.replacingOccurrences(of: "'", with: "'\\''") + "'"
    }

    private static func appleScriptQuote(_ value: String) -> String {
        "\"" + value.replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "\"", with: "\\\"") + "\""
    }
}
