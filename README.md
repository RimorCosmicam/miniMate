# Minimate ⚡
> **The ultimate wireless Bluetooth trackpad experience designed specifically for the Samsung Galaxy Z Flip 7 Cover Screen.**

Minimate turns your Samsung Galaxy Z Flip 7 Flex Window into an ultra-responsive, physics-modeled wireless trackpad for **Mac, iPad, tablet, PC, or Android**.

Designed with the precision, tactile feedback, and fluid momentum of an **Apple Magic Trackpad**, Minimate connects directly as a standard **Bluetooth HID (Human Interface Device)** with **zero companion apps required** on the host.

---

## ✨ Zero-Menu Hardware UX

Minimate has **no traditional settings menus or cluttered sheets**. The entire cover screen is a pure, edge-to-edge touch canvas.

### 🪐 The Floating Interaction Ball
Located in the bottom-left corner, the ball is your single gestural control point:

- **Hold & Slide Radial Action Ring**:
  Press and slide your finger in any direction to reveal orbital action petals:
  - 🔵 **Pair Host**: Instantly activates Bluetooth discoverable mode for pairing on Mac/PC/iPad.
  - 🟣 **Themes**: Cycles through live GPU-accelerated interactive shader backgrounds.
  - 🟡 **Custom BG**: Pick any custom image or animated GIF from your device as background.
  - 🔴 **Lock Mode**: Hides the floating ball completely for 100% uninterrupted touchpad immersion.
- **Hold (without sliding)**: **Stealth Dim Mode** — dims the screen to pitch OLED black while the ball morphs into a soft luminous white orb.

### 🔓 Hardware Unlock Mechanism
When **Locked**, the ball is completely hidden and the entire screen is an active touchpad.
- **To Unlock**: Press **Volume Up (+) and Volume Down (-)** simultaneously.
- Restores the ball instantly with a confirming haptic pulse and transient HUD badge.

---

## 🎨 Touch-Reactive GPU Shaders & Finger Effects

Minimate features GPU-accelerated AGSL runtime shaders (Android 13+) with Canvas fallbacks:
1. **Cosmic Warp**: Gravitational light-bending spacetime distortion warping directly under active fingers.
2. **Fluid Aurora Plasma**: Interactive harmonic plasma waves radiating from finger touch points.
3. **Liquid Glass Prism**: Refractive caustic prism light waves warping under multi-touch gestures.
4. **Cyber Matrix Grid**: Reactive digital matrix grid emitting radiant pulse waves on touch.
5. **Custom Image / GIF**: Load any wallpaper or animated GIF with subtle ambient glass overlays.
6. **Pure OLED Black**: True zero-power `#000000` dark mode.

### 🖐️ Multi-Touch Finger Effects
- Interactive glowing halo rings, bioluminescent particle wakes, and center contact points follow all active fingers in real-time.

---

## 🎛️ High-Precision Touchpad Engine

- **Sub-Millisecond Touch Sampling**: Ingests historical digitizer points (`MotionEvent.getHistoricalX/Y`) from high refresh rate panels.
- **1€ Filter Delta Smoothing**: Adaptive dual-cutoff jitter filter that removes finger tremors at slow speeds without adding phase lag during fast flicks.
- **macOS Dynamic Acceleration Curve**: Non-linear power-law curve ($v_{out} = v_{in} \cdot (1 + \alpha \cdot v_{in}^\beta)$) with fractional sub-pixel carryover accumulator.
- **Correct Coordinate Mapping**: Natural scrolling applies strictly to 2-finger scrolling; 1-finger cursor movement tracks 1:1 with natural finger orientation.
- **Kinetic Momentum Scrolling**: Physics-based fluid inertial deceleration when lifting fingers during a scroll.
- **Gestures**:
  - **1-Finger Move**: Precision pointer tracking.
  - **1-Finger Tap**: Left click with crisp haptic response.
  - **1-Finger Double-Tap & Drag**: Double-tap lock to drag windows and files.
  - **2-Finger Scroll**: Vertical scrolling & horizontal pan.
  - **2-Finger Tap**: Secondary / Right click.
  - **Bezel Palm Rejection**: Filters accidental edge contacts along the Z Flip cover frame.

---

## 🔵 Native Bluetooth HID Profile (HOGP)

- **No Companion App Needed**: Host machines (macOS, iPadOS, Windows 11, Linux, Android) recognize Minimate as a physical Bluetooth Mouse/Trackpad.
- **16-Bit High-Resolution Coordinate Descriptors**: Hand-crafted USB-IF HID 1.11 report descriptor supporting 16-bit relative X/Y coordinate deltas (`-32767..32767`), vertical scroll wheel, horizontal AC pan, and 5 buttons.
- **Guaranteed Low-Latency QOS**: Optimized SDP & QOS profile settings targeting sub-10ms packet dispatching.
- **Battery Reporting**: Broadcasts host phone battery percentage over standard HID battery reports.

---

## 🚀 Building & Installation

```bash
# Build Debug & Release APKs
chmod +x gradlew
./gradlew test assembleDebug assembleRelease
```
Compiled APKs:
- **Debug**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release**: `app/build/outputs/apk/release/app-release.apk`

---

## 🤖 GitHub Actions Automated Builds

The repository includes `.github/workflows/build.yml` which automatically compiles both Debug and Release APKs and uploads them as workflow artifacts on every push.

---

## 📄 License
Created with ❤️ for the Samsung Galaxy Z Flip community.
