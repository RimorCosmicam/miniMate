import AppKit
import SwiftUI

@main
struct MiniMateAudioMain {
    @MainActor
    static func main() {
        let app = NSApplication.shared
        let delegate = AppDelegate()
        app.delegate = delegate
        app.setActivationPolicy(.accessory)
        app.run()
    }
}

@MainActor
final class AppDelegate: NSObject, NSApplicationDelegate, NSWindowDelegate {
    private let controller = BridgeController()
    private var statusItem: NSStatusItem!
    private var window: NSWindow!

    func applicationDidFinishLaunching(_ notification: Notification) {
        if let iconURL = Bundle.module.url(forResource: "Mini", withExtension: "png"),
           let icon = NSImage(contentsOf: iconURL) {
            NSApp.applicationIconImage = icon
        }
        statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
        statusItem.button?.image = NSImage(systemSymbolName: "waveform.circle.fill", accessibilityDescription: "MiniMate Audio")
        let menu = NSMenu()
        menu.addItem(withTitle: "Open MiniMate Audio", action: #selector(showWindow), keyEquivalent: "")
        menu.addItem(NSMenuItem.separator())
        menu.addItem(withTitle: "Quit", action: #selector(quit), keyEquivalent: "q")
        statusItem.menu = menu

        let hosting = NSHostingController(rootView: CompanionView(controller: controller))
        window = NSWindow(contentViewController: hosting)
        window.title = "MiniMate Audio"
        window.styleMask = [.titled, .closable, .miniaturizable]
        window.setContentSize(NSSize(width: 390, height: 445))
        window.center()
        window.delegate = self
        // An installer launch must be visible. The app returns to menu-bar-only mode when
        // this window closes, but its first run should never look like nothing was installed.
        showWindow()
    }

    @objc private func showWindow() {
        NSApp.setActivationPolicy(.regular)
        NSApp.activate(ignoringOtherApps: true)
        window.makeKeyAndOrderFront(nil)
    }

    func windowWillClose(_ notification: Notification) {
        NSApp.setActivationPolicy(.accessory)
    }

    @objc private func quit() {
        controller.disconnect()
        NSApp.terminate(nil)
    }
}

struct CompanionView: View {
    @ObservedObject var controller: BridgeController

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack {
                Image(nsImage: NSApp.applicationIconImage).resizable().frame(width: 44, height: 44).clipShape(RoundedRectangle(cornerRadius: 11))
                VStack(alignment: .leading, spacing: 2) {
                    Text("MiniMate Audio").font(.title2.bold())
                    Text(controller.status).font(.caption).foregroundStyle(.secondary)
                }
            }

            GroupBox("Wi-Fi · lossless") {
                VStack(alignment: .leading, spacing: 8) {
                    if controller.wifiServices.isEmpty {
                        Text("Searching this network…").foregroundStyle(.secondary)
                    } else {
                        ForEach(Array(controller.wifiServices.enumerated()), id: \.offset) { _, service in
                            Button("Connect to \(service.name)") { controller.connectWiFi(service) }
                                .buttonStyle(.borderedProminent)
                        }
                    }
                    Text("24-bit / 48 kHz PCM · no audio compression").font(.caption2).foregroundStyle(.secondary)
                }.frame(maxWidth: .infinity, alignment: .leading).padding(4)
            }

            GroupBox("Bluetooth · fallback") {
                VStack(alignment: .leading, spacing: 7) {
                    ForEach(controller.bluetoothDevices, id: \.addressString) { device in
                        Button("Connect to \(device.name ?? device.addressString ?? "Paired device")") {
                            controller.connectBluetooth(device)
                        }
                    }
                    if controller.bluetoothDevices.isEmpty { Text("Pair the Z Flip in System Settings first.").foregroundStyle(.secondary) }
                }.frame(maxWidth: .infinity, alignment: .leading).padding(4)
            }

            GroupBox("macOS audio devices") {
                VStack(alignment: .leading, spacing: 8) {
                    Label("MiniMate Speaker · output", systemImage: "speaker.wave.2.fill")
                    Label("MiniMate Microphone · input", systemImage: "mic.fill")
                    if controller.driverInstalled {
                        Label("Installed and available to apps", systemImage: "checkmark.circle.fill")
                            .foregroundStyle(.green)
                    } else {
                        Button("Install Audio Devices") { controller.installAudioDevices() }
                            .buttonStyle(.borderedProminent)
                        Text("One administrator prompt installs both CoreAudio endpoints. No screen-recording permission or routing picker is used.")
                            .font(.caption2).foregroundStyle(.secondary)
                    }
                }.frame(maxWidth: .infinity, alignment: .leading).padding(4)
            }

            HStack {
                Button(controller.streaming ? "Stop audio" : "Start audio") {
                    controller.streaming ? controller.stopStreaming() : controller.startStreaming()
                }.disabled(!controller.connected)
                Spacer()
                Button("Disconnect") { controller.disconnect() }.disabled(!controller.connected)
            }
        }
        .padding(22)
        .frame(minWidth: 390, minHeight: 445)
    }
}
