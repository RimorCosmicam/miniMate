import Combine
import CoreAudio
import Foundation
import IOBluetooth
import ModernCameraBridge

@MainActor
final class BridgeController: ObservableObject {
    @Published var status = "Looking for MiniMate"
    @Published var wifiServices: [(name: String, host: String, port: Int)] = []
    @Published var bluetoothDevices: [IOBluetoothDevice] = BluetoothBridgeTransport.pairedMiniMateCandidates
    @Published var driverInstalled = false
    @Published var cameraInstalled = false
    @Published var cameraDeviceName = "MiniMate Camera"
    @Published var connected = false
    @Published var streaming = false
    /// Plays the incoming microphone straight to the Mac's output, so the pipeline can be judged
    /// without a conferencing app's own gain control and noise suppression in the way.
    @Published var monitoringMicrophone = false
    @Published var monitorOutputs: [MonitorOutput] = MicrophoneMonitor.availableOutputs()
    @Published var monitorOutputID: AudioDeviceID? = MicrophoneMonitor.availableOutputs().first?.id

    private let discovery = MiniMateDiscovery()
    private let endpoints = CoreAudioEndpointBridge()
    private let microphoneMonitor = MicrophoneMonitor()
    private let parser = MMAudioProtocol.Parser()
    private let webcam = WebcamPipeline()
    private var transport: BridgeTransport?
    private var transportKind = "Wi-Fi"
    private var sequence: UInt32 = 0

    init() {
        discovery.onChange = { [weak self] services in
            DispatchQueue.main.async { self?.wifiServices = services }
        }
        endpoints.onSpeakerPCM16 = { [weak self] packet in
            self?.sendAudio(CoreAudioEndpointBridge.pcm16StereoToPCM24(packet))
        }
        driverInstalled = endpoints.isInstalled
        refreshCameraAvailability()
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
                self?.endpoints.stop()
            }
        }
    }

    func startStreaming() {
        guard !streaming else { return }
        do {
            try endpoints.start()
            streaming = true
            status = transportKind == "Wi-Fi" ? "Lossless 24-bit / 48 kHz" : "Bluetooth audio"
        } catch {
            status = error.localizedDescription
            streaming = false
        }
    }

    func stopStreaming() {
        streaming = false
        endpoints.stop()
    }

    func installAudioDevices() {
        do {
            try CoreAudioDriverInstaller.install()
            driverInstalled = endpoints.isInstalled
            refreshCameraAvailability()
            status = driverInstalled && cameraInstalled ? "Audio and camera devices installed" : "Restart the companion to finish installation"
        } catch {
            status = error.localizedDescription
        }
    }

    private func refreshCameraAvailability() {
        if MMModernCameraAvailable() {
            cameraInstalled = true
            cameraDeviceName = "OBS Virtual Camera · MiniMate bridge"
        } else if #available(macOS 14.1, *) {
            cameraInstalled = false
            cameraDeviceName = "MiniMate Camera · extension unavailable"
        } else {
            cameraInstalled = FileManager.default.fileExists(atPath: CoreAudioDriverInstaller.installedCameraURL.path)
            cameraDeviceName = "MiniMate Camera"
        }
    }

    func disconnect() {
        stopStreaming()
        setMicrophoneMonitoring(false)
        transport?.close()
        transport = nil
        connected = false
    }

    /// Routes the incoming microphone to this Mac's current output device so it can be heard
    /// directly. Use headphones: the phone's microphone will otherwise pick the speakers back up.
    func setMicrophoneMonitoring(_ enabled: Bool) {
        guard let microphoneMonitor else {
            status = "Monitoring is unavailable on this system"
            return
        }
        if enabled {
            monitorOutputs = MicrophoneMonitor.availableOutputs()
            if monitorOutputID == nil || !monitorOutputs.contains(where: { $0.id == monitorOutputID }) {
                monitorOutputID = monitorOutputs.first?.id
            }
            do {
                try microphoneMonitor.start(deviceID: monitorOutputID)
                monitoringMicrophone = true
            } catch {
                monitoringMicrophone = false
                status = "Could not start monitoring: \(error.localizedDescription)"
            }
        } else {
            microphoneMonitor.stop()
            monitoringMicrophone = false
        }
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
                if let pcm = CoreAudioEndpointBridge.microphonePCM16(frame: frame) {
                    endpoints.sendMicrophonePCM16(pcm)
                    // Identical samples at the identical point in the chain as the virtual
                    // microphone receives, with nothing added.
                    microphoneMonitor?.enqueue(pcm16: pcm)
                }
            }
            for frame in frames where frame.type == MMAudioProtocol.typeWebcamConfig {
                webcam.configure(frame.payload)
            }
            for frame in frames where frame.type == MMAudioProtocol.typeWebcamJPEG {
                webcam.consume(jpeg: frame.payload)
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
