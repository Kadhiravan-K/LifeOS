# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0-rc.1] - 2026-10-18

### Added
- **Task Completion Feedback System:**
  - Synthesized on-device 180ms harmonic chime (880 Hz + 1320 Hz + 1760 Hz with attack/decay envelope) stored in cache and played via `SoundPool`.
  - Native haptic feedback using `VibrationEffect` (`EFFECT_CLICK` / one-shot vibration).
  - Animated completion circle (`TaskCompletionCircle`) with spring checkmark appearance and pulse scale.
  - Granular Settings toggles for Completion Sound, Completion Vibration, and Completion Animation backed by DataStore.
  - Rapid-tap debouncing (<150ms) to prevent duplicate audio/haptic triggers.
- **Axial Honeycomb Hexagonal Geometry & Touch Drag-and-Drop:**
  - True pointy-top axial geometry: $x = H_{\text{step}}(q + r/2)$, $y = V_{\text{step}}r$ with configurable cell padding.
  - Inverse coordinate mapping (`pixelToAxial`) using hexagonal cube-coordinate rounding.
  - Touch-based long-press drag-and-drop customization using `detectDragGesturesAfterLongPress`.
  - Visual elevation, scale pulse, and candidate drop target highlighting.
  - Swapping and movement persistence in Room database.
  - Support for 7 layout templates: Standard Honeycomb, Pyramid, Diamond, Heart, Snake, Zigzag, and Freeform.
- **UI Polish:**
  - Horizontal scrolling `LazyRow` habit strip on Dashboard to accommodate any number of habits without truncation.
  - Balanced `Modifier.weight(1f)` constraint on Calendar task lists to prevent scroll-trapping in Week and Month views.
  - Updated Settings screen with developer attribution ("Developed by Kadhiravan") and Room v6 status.
- **Open-Source Artifacts:**
  - MIT License, comprehensive `README.md`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md`, `PRIVACY.md`, and CI workflow.
  - 100% offline JVM unit test suite (`LifeOSDomainUnitTest.kt`) with 26 passing tests covering recurrence, conflicts, axial geometry, and audio math.

### Changed
- Replaced legacy arrow-button Honeycomb reordering with direct touch drag-and-drop.
- Cleaned up obsolete network and Firebase build artifacts to ensure strict 100% offline-only architecture.
- Replaced rigid `.take(6)` habit row with responsive horizontal `LazyRow`.

### Fixed
- Fixed irregular hexagonal spacing, clipping, and row misalignment on Honeycomb Dashboard.
- Fixed Calendar nested vertical scroll interception by structuring viewports with flexible weighted bounds.
- Resolved build toolchain and JDK 21 compiler configuration issues.
