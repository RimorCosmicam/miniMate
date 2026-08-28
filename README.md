# MiniMate

MiniMate turns the Samsung Galaxy Z Flip cover display into a Bluetooth HID trackpad, scroll controller, and procedural artwork surface. It is designed specifically around the Galaxy Z Flip 7 FlexWindow and connects to macOS, Windows, Linux, iPadOS, and Android without host-side companion software.

The project is an experimental, privately distributed Android application. It is not prepared for Google Play distribution.

## What it does

- Advertises as a standard Bluetooth mouse using Android's HID device APIs.
- Provides one-finger pointer movement, tap-to-click, double-tap drag, two-finger scrolling, and two-finger secondary click.
- Sends vertical wheel and horizontal pan reports independently.
- Includes momentum scrolling, configurable filtering, acceleration, natural scrolling, edge rejection, and haptic feedback.
- Provides an optional analog controller for continuous two-dimensional scrolling, cursor movement, or directional-pad navigation.
- Locks the activity orientation so the cover-screen coordinate system does not rotate.
- Uses a single active theme configuration rather than theme presets.
- Toggles AMOLED mode immediately when the clock pill is tapped.
- Adds a third Audio mode for simultaneous desktop playback and phone-microphone return.

## Procedural Theme System

MiniMate contains 50 code-generated scenes organized into five worlds:

- **Space** — star travel, astronomical bodies, nebulae, planetary horizons, and spacecraft.
- **Abstract** — cellular geometry, marble, moiré, woven fields, topography, and mathematical membranes.
- **Tech** — several Matrix alphabets, code fields, diagnostic displays, and signal treatments.
- **Arcade** — deterministic miniature game loops inspired by puzzle, racing, football, brick-breaking, maze, crossing, snake, pinball, and platform games.
- **Beach** — aerial coastlines, open-water horizons, coral ecosystems, tidepools, palm beaches, bioluminescent surf, kelp forests, dunes, storms, and underwater environments.

Scenes are rendered at runtime with Android Graphics Shading Language. They do not use full-scene photographs or generated background images. Arcade scenes contain repeatable state progression rather than unrelated animated decorations: objects are collected or destroyed, routes remain legal, rounds reset, and motion follows each game's rules.

Every scene includes authored colorways. Compatible scenes also expose a four-role custom palette. Motion speed and the following full-scene filters can be combined independently:

- Chromatic lens separation
- CRT
- VHS
- Pixelation
- Dream bloom
- Monochrome ink
- Kaleidoscope
- Fisheye
- Halftone
- Thermal
- Negative
- Posterize
- 35 mm film
- Mirror prism
- Liquid glass
- Night vision

### Scene-native touch interaction

Touch input is passed into the shader rather than rendered as a cursor or particle layer. Scenes can bend geometry, redirect currents, focus interference patterns, move game controls, disturb water, or leave persistent changes. The eight most recent released contacts remain available as scene memory until newer contacts replace them.

## Theme Studio

Theme Studio previews changes directly on the trackpad surface. Its neutral dark-glass controller stays in the lower-left portion of the cover screen, leaving the right-side camera area and most of the artwork unobstructed.

The controller provides direct selectors for:

- World
- Scene
- Colorway or custom colors
- Motion
- Filter
- Analog-stick appearance

The clock pill and analog stick are hidden while Theme Studio is open. The uncovered canvas remains touch-interactive during editing. Changes can be kept or reverted from the same compact panel.

## Trackpad engine

The input pipeline consumes Android historical pointer samples and maintains independent state for pointer movement, multi-touch gestures, and shader interaction.

- Adaptive low-speed smoothing with responsive fast movement
- Configurable pointer acceleration and tracking speed
- Correctly separated cursor, wheel, and horizontal-pan axes
- Two-finger momentum with tunable friction
- Sub-pixel motion accumulation
- Tap, secondary tap, and double-tap drag recognition
- Edge rejection for the cover-display bezel
- HID button support for left, middle, right, back, and forward

The analog controller defaults to scrolling. Its sensitivity and deadzone are configurable, it can be disabled, and tap/hold gestures can be assigned independently.

## Interface

The main menu uses a single-pane, neutral liquid-glass layout with three primary areas:

- **Themes** — opens Theme Studio.
- **Mouse** — movement, gestures, scrolling, analog controller, and live calibration.
- **Pairing** — discoverability, saved hosts, connection state, and reconnection controls.

Trackpad-speed calibration returns to the live surface so changes can be tested immediately. Stick calibration centers the controller and hides overlapping configuration UI.

## Audio bridge

Audio mode is full duplex and continues through a foreground service while the phone screen is off. On a shared network it automatically prefers lossless PCM over Wi-Fi (24-bit/48 kHz stereo playback and 16-bit/48 kHz microphone return), with Bluetooth as a compressed fallback—there is no transport switch to manage on the phone. The bottom of Audio mode includes previous, play/pause, next, and host-volume controls. Android's active audio route is respected, so wired TRRS and compatible USB-C headset/IEM microphone combos work in both directions.

The macOS companion is a menu-bar app that appears in the Dock only while its window is open. Its bundled CoreAudio HAL plug-in creates two real devices:

- **MiniMate Speaker** is an output device. Select it in macOS or directly in an app to send that app's audio to the phone.
- **MiniMate Microphone** is an input device. Select it directly in calling, recording, streaming, or music apps to use the phone microphone.

There is no microphone-output picker and no screen-capture permission. The GitHub Actions macOS artifact is a system installer package: it places the companion in `/Applications`, installs both CoreAudio endpoints system-wide, applies the required ownership and permissions, reloads CoreAudio, and opens the installed app so macOS can request Local Network and Bluetooth access. Those privacy prompts must be approved by the signed-in user; they cannot be silently pre-granted outside managed-device deployment.

The companion can also repair its audio endpoints from its **Install Audio Devices** button. Build the app and installer with:

```bash
cd companion/macos
./build-app.sh
./build-installer.sh
```

The Windows companion is under `companion/windows`; its current unsigned development build uses WASAPI loopback and an existing virtual cable for microphone exposure. A branded pair of Windows endpoints requires a separately signed Windows audio driver package.

## Requirements

- Samsung Galaxy Z Flip 7 is the primary target.
- Android 9 / API 28 or newer.
- Bluetooth HID device support on the phone firmware.
- Android 12 and newer require Nearby Devices permissions.
- JDK 17 and Android SDK 35 for local builds.

Other Android devices may run the application, but layout, camera avoidance, performance decisions, and interaction geometry are tuned for the Z Flip 7 cover display.

## Build and test

```bash
chmod +x gradlew
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Artifacts are written to:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

GitHub Actions uses a repository-secret keystore so debug and release artifacts retain a stable update signature across runs and can replace earlier development installs without clearing app data. Replace this development key with a dedicated private release key before distributing the APK outside private devices.

## Continuous integration

[`.github/workflows/build.yml`](.github/workflows/build.yml) runs unit tests and creates debug and release APK artifacts for pushes and pull requests targeting `main` or `master`. Tagged builds beginning with `v` run through the same pipeline.

## Source acknowledgements

Procedural techniques adapted from third-party open-source projects are documented in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md). MiniMate's scene renderer remains code-generated and does not bundle downloaded full-scene artwork.
