// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "MiniMateAudio",
    platforms: [.macOS(.v13)],
    targets: [
        .target(
            name: "ModernCameraBridge",
            path: "ModernCameraBridge",
            publicHeadersPath: "include",
            linkerSettings: [
                .linkedFramework("CoreFoundation"),
                .linkedFramework("CoreGraphics"),
                .linkedFramework("CoreMedia"),
                .linkedFramework("CoreMediaIO"),
                .linkedFramework("CoreVideo"),
                .linkedFramework("ImageIO")
            ]
        ),
        .executableTarget(
            name: "MiniMateAudio",
            dependencies: ["ModernCameraBridge"],
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
