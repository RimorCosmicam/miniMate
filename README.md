# Minimate
> **High-performance wireless Bluetooth trackpad engineered for the Samsung Galaxy Z Flip cover screen.**

Minimate turns the Samsung Galaxy Z Flip Flex Window into an ultra-responsive, physics-modeled wireless trackpad for **macOS, iPadOS, Windows, Linux, and Android**.

Designed with the precision, tactile feedback, and fluid momentum of standard glass trackpads, Minimate connects directly as a standard **Bluetooth HID (Human Interface Device)** with **zero companion software required** on the host machine.

---

## High-Precision Touchpad Engine

- **Sub-Millisecond Sampling**: Ingests historical digitizer points (`MotionEvent.getHistoricalX/Y`).
- **1€ Filter Delta Smoothing**: Adaptive dual-cutoff jitter filter that eliminates finger tremors at slow speeds without adding phase lag during fast flicks.
- **Dynamic Acceleration Curve**: Non-linear power-law curve with sub-pixel carryover accumulator.
- **Correct Coordinate Mapping**: Natural scrolling applies strictly to 2-finger scrolling; 1-finger cursor movement tracks 1:1 with natural finger orientation.
- **Kinetic Momentum Scrolling**: Physics-based fluid inertial deceleration when lifting fingers during a scroll.
- **Gestures**:
  - **1-Finger Move**: Precision pointer tracking.
  - **1-Finger Tap**: Left-click with crisp haptic response.
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
---

## Automated GitHub Actions
Automated APK compilation and verification runs on every push via `.github/workflows/build.yml`.
