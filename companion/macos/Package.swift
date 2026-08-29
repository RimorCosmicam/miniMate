// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "MiniMateAudio",
    platforms: [.macOS(.v13)],
    targets: [
        .executableTarget(
            name: "MiniMateAudio",
            path: "Sources",
            swiftSettings: [.swiftLanguageMode(.v5)],
            linkerSettings: [
                .linkedFramework("AppKit"),
                .linkedFramework("IOBluetooth"),
                .linkedFramework("Network")
            ]
        )
    ]
)
