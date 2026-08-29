import Foundation
import IOBluetooth
import Network

protocol BridgeTransport: AnyObject {
    var onData: ((Data) -> Void)? { get set }
    var onClosed: ((Error?) -> Void)? { get set }
    func send(_ data: Data)
    func close()
}

final class TCPBridgeTransport: BridgeTransport {
    var onData: ((Data) -> Void)?
    var onClosed: ((Error?) -> Void)?
    private let connection: NWConnection
    private let queue = DispatchQueue(label: "MiniMate.TCP", qos: .userInitiated)

    init(host: String, port: UInt16 = MMAudioProtocol.wifiPort) {
        connection = NWConnection(host: NWEndpoint.Host(host), port: NWEndpoint.Port(rawValue: port)!, using: .tcp)
    }

    func start() {
        connection.stateUpdateHandler = { [weak self] state in
            switch state {
            case .ready: self?.receive()
            case .failed(let error): self?.onClosed?(error)
            case .cancelled: self?.onClosed?(nil)
            default: break
            }
        }
        connection.start(queue: queue)
    }

    private func receive() {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 65_536) { [weak self] data, _, complete, error in
            if let data, !data.isEmpty { self?.onData?(data) }
            if complete || error != nil { self?.onClosed?(error); return }
            self?.receive()
        }
    }

    func send(_ data: Data) { connection.send(content: data, completion: .contentProcessed { _ in }) }
    func close() { connection.cancel() }
}

final class BluetoothBridgeTransport: NSObject, BridgeTransport, IOBluetoothRFCOMMChannelDelegate {
    var onData: ((Data) -> Void)?
    var onClosed: ((Error?) -> Void)?
    private var channel: IOBluetoothRFCOMMChannel?
    private var pendingDevice: IOBluetoothDevice?

    static var pairedMiniMateCandidates: [IOBluetoothDevice] {
        (IOBluetoothDevice.pairedDevices() as? [IOBluetoothDevice] ?? []).filter {
            let name = $0.name ?? ""
            let looksLikePhone = name.localizedCaseInsensitiveContains("MiniMate") ||
                name.localizedCaseInsensitiveContains("Galaxy")
            let isAccessory = name.localizedCaseInsensitiveContains("buds")
            return looksLikePhone && !isAccessory
        }
    }

    func connect(_ device: IOBluetoothDevice) throws {
        pendingDevice = device
        let result = device.performSDPQuery(self)
        guard result == kIOReturnSuccess else { throw MMAudioProtocol.BridgeError.connectionFailed }
    }

    @objc func sdpQueryComplete(_ device: IOBluetoothDevice!, status: IOReturn) {
        guard status == kIOReturnSuccess, let device else {
            onClosed?(MMAudioProtocol.BridgeError.noService); return
        }
        let records = device.services as? [IOBluetoothSDPServiceRecord] ?? []
        guard let record = records.first(where: { ($0.getServiceName() ?? "") == "MiniMate Audio" }) else {
            onClosed?(MMAudioProtocol.BridgeError.noService); return
        }
        var id: BluetoothRFCOMMChannelID = 0
        guard record.getRFCOMMChannelID(&id) == kIOReturnSuccess else {
            onClosed?(MMAudioProtocol.BridgeError.noService); return
        }
        var opened: IOBluetoothRFCOMMChannel?
        let result = device.openRFCOMMChannelSync(&opened, withChannelID: id, delegate: self)
        if result == kIOReturnSuccess { channel = opened }
        else { onClosed?(MMAudioProtocol.BridgeError.connectionFailed) }
    }

    func send(_ data: Data) {
        data.withUnsafeBytes { bytes in
            guard let base = bytes.baseAddress else { return }
            _ = channel?.writeSync(UnsafeMutableRawPointer(mutating: base), length: UInt16(data.count))
        }
    }

    func close() { channel?.close(); channel = nil }

    func rfcommChannelData(_ rfcommChannel: IOBluetoothRFCOMMChannel!, data dataPointer: UnsafeMutableRawPointer!, length dataLength: Int) {
        onData?(Data(bytes: dataPointer, count: dataLength))
    }

    func rfcommChannelClosed(_ rfcommChannel: IOBluetoothRFCOMMChannel!) {
        channel = nil
        onClosed?(nil)
    }
}

final class MiniMateDiscovery: NSObject, NetServiceBrowserDelegate, NetServiceDelegate {
    var onChange: (([(name: String, host: String, port: Int)]) -> Void)?
    private let browser = NetServiceBrowser()
    private var services: [NetService] = []

    override init() { super.init(); browser.delegate = self }
    func start() { browser.searchForServices(ofType: "_minimate-audio._tcp.", inDomain: "local.") }
    func stop() { browser.stop(); services.removeAll() }

    func netServiceBrowser(_ browser: NetServiceBrowser, didFind service: NetService, moreComing: Bool) {
        services.append(service); service.delegate = self; service.resolve(withTimeout: 4)
    }
    func netServiceBrowser(_ browser: NetServiceBrowser, didRemove service: NetService, moreComing: Bool) {
        services.removeAll { $0 == service }; publish()
    }
    func netServiceDidResolveAddress(_ sender: NetService) { publish() }

    private func publish() {
        let resolved = services.compactMap { service -> (String, String, Int)? in
            guard let host = service.hostName?.trimmingCharacters(in: CharacterSet(charactersIn: ".")), service.port > 0 else { return nil }
            return (service.name, host, service.port)
        }
        onChange?(resolved)
    }
}
