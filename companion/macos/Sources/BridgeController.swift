import Combine
import Foundation
import IOBluetooth

@MainActor
final class BridgeController: ObservableObject {
    @Published var status = "Looking for MiniMate"
    @Published var wifiServices: [(name: String, host: String, port: Int)] = []
    @Published var bluetoothDevices: [IOBluetoothDevice] = BluetoothBridgeTransport.pairedMiniMateCandidates
    @Published var outputDevices: [MacAudioDevice] = MacAudioDevices.outputs()
    @Published var selectedOutput: AudioDeviceID?
    @Published var connected = false
    @Published var streaming = false

    private let discovery = MiniMateDiscovery()
    private let capture = DesktopAudioCapture()
    private let microphonePlayer = PhoneMicrophonePlayer()
    private let parser = MMAudioProtocol.Parser()
    private var transport: BridgeTransport?
    private var transportKind = "Wi-Fi"
    private var sequence: UInt32 = 0

    init() {
        discovery.onChange = { [weak self] services in
            DispatchQueue.main.async { self?.wifiServices = services }
        }
        capture.onPCM24 = { [weak self] packet in self?.sendAudio(packet) }
        capture.onError = { [weak self] error in
            Task { @MainActor in self?.status = error.localizedDescription; self?.streaming = false }
        }
        discovery.start()
    }

    func connectWiFi(_ service: (name: String, host: String, port: Int)) {
        disconnect()
        transportKind = "Wi-Fi"
        let link = TCPBridgeTransport(host: service.host, port: UInt16(service.port))
        install(link)
        status = "Connecting over lossless Wi-Fi…"
        link.start()
        connected = true
        startStreaming()
    }

    func connectBluetooth(_ device: IOBluetoothDevice) {
        disconnect()
        transportKind = "Bluetooth"
        let link = BluetoothBridgeTransport()
        install(link)
        do {
            try link.connect(device)
            status = "Connecting over Bluetooth…"
            connected = true
            startStreaming()
        } catch { status = error.localizedDescription }
    }

    private func install(_ link: BridgeTransport) {
        transport = link
        link.onData = { [weak self] data in self?.receive(data) }
        link.onClosed = { [weak self] error in
            Task { @MainActor in
                self?.status = error?.localizedDescription ?? "Disconnected"
                self?.connected = false
                self?.streaming = false
            }
        }
    }

    func startStreaming() {
        guard !streaming else { return }
        streaming = true
        Task {
            do {
                try await capture.start()
                status = transportKind == "Wi-Fi" ? "Lossless 24-bit / 48 kHz" : "Bluetooth audio"
            } catch {
                status = error.localizedDescription
                streaming = false
            }
        }
    }

    func stopStreaming() {
        streaming = false
        Task { await capture.stop() }
    }

    func selectOutput(_ id: AudioDeviceID) {
        guard let device = outputDevices.first(where: { $0.id == id }) else { return }
        do { try microphonePlayer.selectOutput(device); selectedOutput = id }
        catch { status = "Microphone output: \(error.localizedDescription)" }
    }

    func disconnect() {
        stopStreaming()
        transport?.close()
        transport = nil
        microphonePlayer.stop()
        connected = false
    }

    private nonisolated func sendAudio(_ pcm24: Data) {
        Task { @MainActor in
            guard let transport else { return }
            let frame: MMAudioProtocol.Frame
            if transportKind == "Wi-Fi" {
                frame = .init(type: MMAudioProtocol.typePlayback, codec: MMAudioProtocol.codecPCM24,
                              channels: 2, sampleRate: 48_000, sequence: sequence, payload: pcm24)
            } else {
                let samples = Self.pcm24ToBluetoothADPCM(pcm24)
                frame = .init(type: MMAudioProtocol.typePlayback, codec: MMAudioProtocol.codecIMA,
                              channels: 2, sampleRate: 32_000, sequence: sequence,
                              payload: IMAADPCM.encode(samples, channels: 2))
            }
            sequence &+= 1
            transport.send(frame.encoded)
        }
    }

    private nonisolated func receive(_ data: Data) {
        Task { @MainActor in self.receiveOnMain(data) }
    }

    private func receiveOnMain(_ data: Data) {
        do {
            let frames = try parser.append(data)
            for frame in frames where frame.type == MMAudioProtocol.typeMicrophone {
                microphonePlayer.play(frame: frame)
            }
            if frames.contains(where: { $0.type == MMAudioProtocol.typeHello }) {
                status = transportKind == "Wi-Fi" ? "Lossless 24-bit / 48 kHz" : "Bluetooth audio"
            }
        } catch {
            status = error.localizedDescription
        }
    }

    private static func pcm24ToBluetoothADPCM(_ data: Data) -> [Int16] {
        let frames48 = data.count / 6
        var output = [Int16](); output.reserveCapacity(frames48 * 2 / 3 * 2)
        for frame32 in 0..<(frames48 * 2 / 3) {
            let sourceFrame = frame32 * 3 / 2
            for channel in 0..<2 {
                let offset = sourceFrame * 6 + channel * 3
                var value = Int32(data[offset]) | Int32(data[offset + 1]) << 8 | Int32(data[offset + 2]) << 16
                if value & 0x800000 != 0 { value |= ~0xFFFFFF }
                output.append(Int16(clamping: value >> 8))
            }
        }
        return output
    }
}
