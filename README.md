# Minimate
> **High-performance wireless Bluetooth trackpad engineered for the Samsung Galaxy Z Flip cover screen.**

Minimate turns the Samsung Galaxy Z Flip Flex Window into an ultra-responsive, physics-modeled wireless trackpad for **macOS, iPadOS, Windows, Linux, and Android**.

Designed with the precision, tactile feedback, and fluid momentum of standard glass trackpads, Minimate connects directly as a standard **Bluetooth HID (Human Interface Device)** with **zero companion software required** on the host machine.

---

## Architecture & Features

### 1. The Floating Interaction Ball
Located in the bottom-left corner of the cover screen, the ball is the primary tactile controller:
- **Instant Touch Down Response**: Low-level pointer event polling ensures instantaneous tactile scaling and haptic feedback on touch down.
- **Hold & Slide Radial Action Arc**:
  - **Pair**: Opens the dedicated in-app Bluetooth Pairing Hub and discovery manager.
  - **Settings**: Opens the 4-tab translucent control center (Themes, Touch FX, Clock & HUD, Settings).
  - **Themes**: Cycles through live procedural GPU shader backgrounds.
  - **Lock**: Hides the ball completely for 100% full-screen touch immersion.
- **Configurable Single-Tap Action**: Instant trigger for Stealth Dim, Menu, Pairing Hub, or Middle Click without dragging.

### 2. Physical Hardware Unlock
When in **Locked Mode**, the UI disappears completely for uninterrupted full-display touch operation.
- **To Unlock**: Press **Volume Up (+)** and **Volume Down (-)** simultaneously.
- Instantly restores controls with confirming haptic pulses and transient HUD toast.

---

## Live Procedural AGSL Shaders & Touch Effects

### Procedural Background Shaders (3 Color Palettes Each)
1. **Sakura Petals**: Procedural parametric cherry blossoms drifting with dynamic wind turbulence.
2. **Bubble Aquarium**: Transparent spherical refraction bubbles with Fresnel highlights and touch bursts.
3. **Cat Paw Cafe**: Procedural kitten paw print stamps with drifting particle trails.
4. **Prism Waves**: Iridescent chromatic diffraction and fluid ribbon harmonics.
5. **Matcha Latte Art**: Viscous fluid dynamics simulation creating creamy latte froth swirls.
6. **Retro 8-Bit**: Procedural pixel starfield, arcade blocks, and CRT scanlines.
7. **Bioluminescent Sea**: Caustic sun rays and aquatic shockwaves on touch.
8. **Jelly Mochi**: Elastic jiggling grid mesh with specular glossy highlights.
9. **Cosmic Galaxy**: Rotating spiral nebula vortex with twinkling star clusters.
10. **Stealth Titanium**: Brushed titanium micro-texture with pure OLED power-saver mode.
11. **Custom Wallpaper**: Load any image or animated GIF from local gallery.

### Multi-Touch Finger Effects
1. **Sakura Trail**: Floating flower petals under active contact points.
2. **Soap Bubbles**: Wobbling cartoon soap bubbles with specular shines.
3. **Cat Paws**: Soft kitten paw prints stamped on touch.
4. **Star Glitter**: Twinkling fairy dust and star glitter trails.
5. **Rainbow Ribbon**: Concentric glowing fluid rainbow rings.
6. **Water Droplets**: Liquid concentric expanding shockwaves.
7. **Plasma Bolts**: Electric neon energy arcs linking multi-touch fingers.
8. **Neon Reticle**: Precision targeting crosshair reticle.
9. **Floating Hearts**: Glowing love heart particles trailing finger drag.
10. **Clean Dot**: Minimalist precision micro-dots.

---

## Clock & Battery HUD Customization

Configurable HUD widget overlay:
- **Styles**: Glass Pill, Bold Digital, Clean Sans, Retro Monospace, Hidden.
- **Positions**: Top Left, Top Center, Top Right, Bottom Right, Bottom Center.
- **Time Formats**: 12-Hour (AM/PM) and 24-Hour Military Format, Optional live seconds ticker.
- **Battery Indicator**: Real-time connected host and phone battery percentage.

---

## High-Precision Touchpad Engine

- **Sub-Millisecond Sampling**: Ingests historical digitizer points (`MotionEvent.getHistoricalX/Y`).
- **1€ Filter Delta Smoothing**: Adaptive dual-cutoff jitter filter that eliminates finger tremors at slow speeds without adding phase lag during fast flicks.
- **Dynamic Acceleration Curve**: Non-linear power-law curve with sub-pixel carryover accumulator.
- **Correct Coordinate Mapping**: Natural scrolling applies strictly to 2-finger scrolling; 1-finger cursor movement tracks 1:1 with natural finger orientation.
- **Kinetic Momentum Scrolling**: Physics-based fluid inertial deceleration when lifting fingers during a scroll.
- **Gestures**:
  - **1-Finger Move**: Precision pointer tracking.
  - **1-Finger Tap**: Left click with crisp haptic response.
  - **1-Finger Double-Tap & Drag**: Double-tap lock to drag windows and files.
  - **2-Finger Scroll**: Vertical scrolling and horizontal pan.
  - **2-Finger Tap**: Secondary / Right click.
  - **Bezel Palm Rejection**: Filters accidental edge contacts along the Z Flip cover frame.

---

## Building & Installation

```bash
# Build Debug & Release APKs
chmod +x gradlew
./gradlew test assembleDebug assembleRelease
```
Compiled APK artifacts:
- **Debug**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release**: `app/build/outputs/apk/release/app-release.apk`

---

## Automated GitHub Actions
Automated APK compilation and verification runs on every push via `.github/workflows/build.yml`.
