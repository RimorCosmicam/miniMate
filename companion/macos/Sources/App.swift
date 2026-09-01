import AppKit
import CoreText
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

        MontFont.register()
        installMenu()

        let hosting = NSHostingController(rootView: CompanionView(controller: controller))
        window = NSWindow(contentViewController: hosting)
        window.title = "MiniMate"
        // Closable stays in the mask so Cmd+W has something to act on; the buttons themselves are
        // hidden, because the window says Close in words like every other surface in this app.
        window.styleMask = [.titled, .closable, .fullSizeContentView]
        window.titlebarAppearsTransparent = true
        window.titleVisibility = .hidden
        window.isMovableByWindowBackground = true
        window.backgroundColor = .black
        window.standardWindowButton(.closeButton)?.isHidden = true
        window.standardWindowButton(.miniaturizeButton)?.isHidden = true
        window.standardWindowButton(.zoomButton)?.isHidden = true
        window.setContentSize(NSSize(width: 320, height: 300))
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

    /// An accessory app has no menu bar of its own, so Cmd+W and Cmd+Q had nothing to reach.
    private func installMenu() {
        let mainMenu = NSMenu()
        let appItem = NSMenuItem()
        let appMenu = NSMenu()
        appMenu.addItem(withTitle: "Close", action: #selector(NSWindow.performClose(_:)), keyEquivalent: "w")
        appMenu.addItem(NSMenuItem.separator())
        appMenu.addItem(withTitle: "Quit MiniMate", action: #selector(NSApplication.terminate(_:)), keyEquivalent: "q")
        appItem.submenu = appMenu
        mainMenu.addItem(appItem)
        NSApp.mainMenu = mainMenu
    }

    @objc private func quit() {
        controller.disconnect()
        NSApp.terminate(nil)
    }
}

/// Mont, loaded from the bundle. The same typeface the phone uses, so the two halves of the app
/// do not look like they were made by different people.
enum MontFont {
    static let thin = "Mont-Thin"
    static let black = "Mont-Black"

    static func register() {
        for name in ["mont_thin", "mont_black"] {
            guard let url = Bundle.main.url(forResource: name, withExtension: "ttf") else { continue }
            CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
        }
    }
}

/// A row of the window. Text alone, bright when it can be used and dim when it cannot — the rule
/// every control on the phone follows, and the reason there are no buttons drawn here.
private struct MontRow: View {
    var label: String
    var enabled: Bool = true
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label.uppercased())
                .font(.custom(MontFont.black, size: 13))
                .foregroundStyle(.white.opacity(enabled ? 1 : 0.3))
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.vertical, 7)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

struct CompanionView: View {
    @ObservedObject var controller: BridgeController

    private var devicesReady: Bool { controller.driverInstalled && controller.cameraInstalled }

    private var currentOutput: MonitorOutput? {
        controller.monitorOutputs.first { $0.id == controller.monitorOutputID } ?? controller.monitorOutputs.first
    }

    private var currentOutputName: String { currentOutput?.name ?? "the default output" }

    /// A guess, and phrased as one: there is no flag that says "these are the built-in speakers".
    private var outputIsLikelySpeakers: Bool {
        let name = currentOutputName.lowercased()
        return name.contains("speaker") || name.contains("macbook") || name.contains("built-in")
    }

    private func cycleOutput() {
        let outputs = controller.monitorOutputs
        guard !outputs.isEmpty else { return }
        let index = outputs.firstIndex { $0.id == controller.monitorOutputID } ?? -1
        let next = outputs[(index + 1) % outputs.count]
        controller.monitorOutputID = next.id
        // Bound at start, so a running monitor has to be turned over to follow the choice.
        if controller.monitoringMicrophone {
            controller.setMicrophoneMonitoring(false)
            controller.setMicrophoneMonitoring(true)
        }
    }

    /// The first thing anyone came here to do, named after the thing it will do it to.
    private var connection: (label: String, enabled: Bool, act: () -> Void) {
        if controller.connected {
            return ("Disconnect", true, { controller.disconnect() })
        }
        if let service = controller.wifiServices.first {
            return ("Connect to \(service.name)", true, { controller.connectWiFi(service) })
        }
        if let device = controller.bluetoothDevices.first {
            let name = device.name ?? device.addressString ?? "paired device"
            return ("Connect to \(name)", true, { controller.connectBluetooth(device) })
        }
        return ("Looking for your phone", false, {})
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .center, spacing: 12) {
                Image(nsImage: NSApp.applicationIconImage)
                    .resizable()
                    .frame(width: 44, height: 44)

                // Set the way the phone's first screen sets it: the lightest weight over the
                // heaviest, which is the whole identity in two words.
                VStack(alignment: .leading, spacing: -6) {
                    Text("mini").font(.custom(MontFont.thin, size: 26)).foregroundStyle(.white)
                    Text("Mate").font(.custom(MontFont.black, size: 26)).foregroundStyle(.white)
                }
            }
            .padding(.bottom, 18)

            let connect = connection
            MontRow(label: connect.label, enabled: connect.enabled, action: connect.act)

            if !devicesReady {
                MontRow(label: "Install MiniMate devices") { controller.installAudioDevices() }
            }

            MontRow(
                label: controller.streaming ? "Stop audio" : "Start audio",
                enabled: controller.connected
            ) {
                controller.streaming ? controller.stopStreaming() : controller.startStreaming()
            }

            MontRow(
                label: controller.monitoringMicrophone ? "Stop listening to mic" : "Listen to mic",
                enabled: controller.connected
            ) {
                controller.setMicrophoneMonitoring(!controller.monitoringMicrophone)
            }

            // Where listening comes out, which is not a detail. Monitoring through the Mac's own
            // speakers puts the phone's microphone in earshot of its own signal, and the loop that
            // makes takes about a syllable to build into a howl that never stops. This row was
            // dropped in the redesign and that is exactly what happened.
            if !controller.monitorOutputs.isEmpty {
                MontRow(label: "Hear it on \(currentOutputName)") { cycleOutput() }
                if controller.monitoringMicrophone && outputIsLikelySpeakers {
                    Text("This is probably the Mac's own speakers. The phone will hear them and howl — pick headphones.")
                        .font(.custom(MontFont.black, size: 9))
                        .foregroundStyle(.white.opacity(0.5))
                        .fixedSize(horizontal: false, vertical: true)
                        .padding(.bottom, 4)
                }
            }

            Spacer(minLength: 12)

            Text(controller.status.uppercased())
                .font(.custom(MontFont.black, size: 10))
                .foregroundStyle(.white.opacity(0.45))
                .padding(.bottom, 6)

            // Last in the list, the way Close ends the phone's command bar.
            MontRow(label: "Close") { NSApp.keyWindow?.performClose(nil) }
        }
        .padding(.horizontal, 22)
        .padding(.top, 30)
        .padding(.bottom, 16)
        .frame(width: 320, height: 300, alignment: .topLeading)
        .background(Color.black)
        .ignoresSafeArea()
    }
}
