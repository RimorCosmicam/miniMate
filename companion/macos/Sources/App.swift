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
        window.setContentSize(NSSize(width: 400, height: 470))
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

// MARK: - Liquid glass design system

private extension Color {
    static let miniCyan = Color(red: 0x4C / 255, green: 0xC9 / 255, blue: 0xF0 / 255)
    static let miniPink = Color(red: 0xFF / 255, green: 0x69 / 255, blue: 0xB4 / 255)
    static let miniEmerald = Color(red: 0x10 / 255, green: 0xB9 / 255, blue: 0x81 / 255)
    static let miniAmber = Color(red: 0xFF / 255, green: 0xCA / 255, blue: 0x3A / 255)
}

/// A frosted, softly-bordered panel — the base unit of every section in this window.
private struct GlassCard<Content: View>: View {
    var title: String
    var systemImage: String
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Label(title, systemImage: systemImage)
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(.secondary)
                .textCase(.uppercase)
            content
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .strokeBorder(
                    LinearGradient(colors: [.white.opacity(0.55), .white.opacity(0.06)], startPoint: .topLeading, endPoint: .bottomTrailing),
                    lineWidth: 1
                )
        )
        .shadow(color: .black.opacity(0.16), radius: 14, y: 6)
    }
}

/// A translucent capsule button — used for every action in this window instead of the stock macOS button.
private struct GlassButtonStyle: ButtonStyle {
    var tint: Color = .miniCyan
    var filled: Bool = false

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 12, weight: .semibold))
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .frame(maxWidth: filled ? .infinity : nil)
            .background {
                Capsule().fill(tint.opacity(configuration.isPressed ? 0.30 : 0.20))
                Capsule().fill(.regularMaterial)
            }
            .overlay(Capsule().strokeBorder(tint.opacity(configuration.isPressed ? 0.9 : 0.55), lineWidth: 1))
            .foregroundStyle(tint)
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .opacity(configuration.isPressed ? 0.9 : 1)
            .animation(.spring(response: 0.25, dampingFraction: 0.75), value: configuration.isPressed)
    }
}

private struct StatusPill: View {
    var connected: Bool
    var text: String

    var body: some View {
        HStack(spacing: 6) {
            Circle().fill(connected ? Color.miniEmerald : Color.secondary.opacity(0.5)).frame(width: 6, height: 6)
            Text(text).font(.system(size: 11, weight: .semibold)).foregroundStyle(.secondary)
        }
        .padding(.horizontal, 10).padding(.vertical, 5)
        .background(.thinMaterial, in: Capsule())
        .overlay(Capsule().strokeBorder(.white.opacity(0.18), lineWidth: 1))
    }
}

struct CompanionView: View {
    @ObservedObject var controller: BridgeController

    private var devicesReady: Bool { controller.driverInstalled && controller.cameraInstalled }

    var body: some View {
        ZStack {
            Rectangle().fill(.ultraThinMaterial).ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack(spacing: 12) {
                        Image(nsImage: NSApp.applicationIconImage)
                            .resizable().frame(width: 46, height: 46)
                            .clipShape(RoundedRectangle(cornerRadius: 13, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: 13, style: .continuous).strokeBorder(.white.opacity(0.35), lineWidth: 1))
                            .shadow(color: .black.opacity(0.25), radius: 8, y: 3)
                        VStack(alignment: .leading, spacing: 4) {
                            Text("MiniMate Audio").font(.system(size: 18, weight: .bold))
                            StatusPill(connected: controller.connected, text: controller.status)
                        }
                        Spacer()
                    }

                    GlassCard(title: "Wi-Fi · Lossless", systemImage: "wifi") {
                        if controller.wifiServices.isEmpty {
                            Text("Searching this network…").font(.system(size: 12)).foregroundStyle(.secondary)
                        } else {
                            ForEach(Array(controller.wifiServices.enumerated()), id: \.offset) { _, service in
                                Button("Connect to \(service.name)") { controller.connectWiFi(service) }
                                    .buttonStyle(GlassButtonStyle(tint: .miniCyan, filled: true))
                            }
                        }
                        Text("24-bit / 48 kHz PCM · no audio compression")
                            .font(.system(size: 10)).foregroundStyle(.tertiary)
                    }

                    GlassCard(title: "Bluetooth · Fallback", systemImage: "dot.radiowaves.left.and.right") {
                        if controller.bluetoothDevices.isEmpty {
                            Text("Pair the Z Flip in System Settings first.").font(.system(size: 12)).foregroundStyle(.secondary)
                        } else {
                            ForEach(controller.bluetoothDevices, id: \.addressString) { device in
                                Button("Connect to \(device.name ?? device.addressString ?? "Paired device")") {
                                    controller.connectBluetooth(device)
                                }
                                .buttonStyle(GlassButtonStyle(tint: .miniPink, filled: true))
                            }
                        }
                    }

                    GlassCard(title: "MiniMate Devices", systemImage: "checkmark.seal") {
                        HStack(spacing: 7) {
                            Image(systemName: devicesReady ? "checkmark.seal.fill" : "exclamationmark.triangle.fill")
                                .foregroundStyle(devicesReady ? Color.miniEmerald : Color.miniAmber)
                            Text(devicesReady ? "Installed and available to apps" : "Not installed yet")
                                .font(.system(size: 12, weight: .semibold))
                        }
                        if controller.cameraDeviceName.hasPrefix("OBS") {
                            Text("Select OBS Virtual Camera in Photo Booth. MiniMate feeds it directly; OBS does not need to be open.")
                                .font(.system(size: 10)).foregroundStyle(.tertiary)
                        }
                        if !devicesReady {
                            Button("Install") { controller.installAudioDevices() }
                                .buttonStyle(GlassButtonStyle(tint: .miniAmber))
                        }
                    }

                    HStack(spacing: 10) {
                        Button(controller.streaming ? "Stop audio" : "Start audio") {
                            controller.streaming ? controller.stopStreaming() : controller.startStreaming()
                        }
                        .buttonStyle(GlassButtonStyle(tint: .miniEmerald, filled: true))
                        .disabled(!controller.connected)

                        Button("Disconnect") { controller.disconnect() }
                            .buttonStyle(GlassButtonStyle(tint: .secondary, filled: true))
                            .disabled(!controller.connected)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 20)
                .padding(.top, 34)
            }
        }
        .frame(minWidth: 400, minHeight: 470)
    }
}
