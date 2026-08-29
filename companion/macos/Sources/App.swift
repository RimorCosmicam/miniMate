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
        if let iconURL = Bundle.main.url(forResource: "Mini", withExtension: "png"),
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
        window.styleMask = [.titled, .closable, .miniaturizable, .fullSizeContentView]
        window.titlebarAppearsTransparent = true
        window.titleVisibility = .hidden
        window.isMovableByWindowBackground = true
        window.setContentSize(NSSize(width: 340, height: 320))
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

private struct StatusPill: View {
    var connected: Bool
    var text: String

    var body: some View {
        HStack(spacing: 6) {
            Circle().fill(connected ? Color.green : Color.secondary.opacity(0.5)).frame(width: 6, height: 6)
            Text(text).font(.system(size: 10.5, weight: .semibold)).foregroundStyle(.secondary)
        }
        .padding(.horizontal, 9).padding(.vertical, 4)
        .glassEffect(.regular, in: Capsule())
    }
}

struct CompanionView: View {
    @ObservedObject var controller: BridgeController

    private var devicesReady: Bool { controller.driverInstalled && controller.cameraInstalled }

    var body: some View {
        GlassEffectContainer(spacing: 14) {
            VStack(spacing: 18) {
                HStack(spacing: 9) {
                    Image(nsImage: NSApp.applicationIconImage)
                        .resizable().frame(width: 30, height: 30)
                        .clipShape(RoundedRectangle(cornerRadius: 9, style: .continuous))
                    Text("MiniMate Audio").font(.system(size: 14, weight: .bold))
                    Spacer()
                    StatusPill(connected: controller.connected, text: controller.status)
                }

                Spacer()

                if controller.wifiServices.isEmpty {
                    Text("Looking for your phone…")
                        .font(.system(size: 13))
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(Array(controller.wifiServices.enumerated()), id: \.offset) { _, service in
                        Button("Connect to \(service.name)") { controller.connectWiFi(service) }
                            .font(.system(size: 14, weight: .semibold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 3)
                            .buttonStyle(.glassProminent)
                    }
                }

                if !controller.bluetoothDevices.isEmpty {
                    ForEach(controller.bluetoothDevices, id: \.addressString) { device in
                        Button {
                            controller.connectBluetooth(device)
                        } label: {
                            Label(device.name ?? device.addressString ?? "Paired device", systemImage: "dot.radiowaves.left.and.right")
                        }
                        .font(.system(size: 12, weight: .medium))
                        .frame(maxWidth: .infinity)
                        .buttonStyle(.glass)
                    }
                }

                if !devicesReady {
                    Button("Install MiniMate Devices") { controller.installAudioDevices() }
                        .font(.system(size: 11.5, weight: .medium))
                        .buttonStyle(.glass)
                } else if controller.cameraDeviceName.hasPrefix("OBS") {
                    Text("Camera available as OBS Virtual Camera")
                        .font(.system(size: 10)).foregroundStyle(.tertiary)
                }

                Spacer()

                HStack(spacing: 8) {
                    Button(controller.streaming ? "Stop audio" : "Start audio") {
                        controller.streaming ? controller.stopStreaming() : controller.startStreaming()
                    }
                    .frame(maxWidth: .infinity)
                    .buttonStyle(.glass)
                    .disabled(!controller.connected)

                    Button("Disconnect") { controller.disconnect() }
                        .frame(maxWidth: .infinity)
                        .buttonStyle(.glass)
                        .disabled(!controller.connected)
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 18)
            .padding(.top, 32)
        }
        .background(.regularMaterial)
        .ignoresSafeArea()
        .frame(width: 340, height: 320)
    }
}
