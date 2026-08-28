// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "MiniMateAudio",
    platforms: [.macOS(.v13)],
    targets: [
        .executableTarget(
            name: "MiniMateAudio",
            path: "Sources",
            resources: [.copy("Resources/Mini.png")],
            swiftSettings: [.swiftLanguageMode(.v5)],
            linkerSettings: [
                .linkedFramework("AppKit"),
                .linkedFramework("AVFoundation"),
                .linkedFramework("CoreAudio"),
                .linkedFramework("CoreMedia"),
                .linkedFramework("IOBluetooth"),
                .linkedFramework("Network"),
                .linkedFramework("ScreenCaptureKit")
            ]
        )
    ]
)
