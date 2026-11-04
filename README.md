# LifeOS 📱

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg)](https://developer.android.com/jetpack/compose)
[![Privacy](https://img.shields.io/badge/Privacy-Local--First-success.svg)](PRIVACY.md)
[![Database](https://img.shields.io/badge/Room%20DB-v19%20SQLite-orange.svg)](app/src/main/java/com/example/data/local/AppDatabase.kt)

> **LifeOS** is a privacy-first, local-first native Android productivity and life operating system developed by **Kadhiravan**. Built with Jetpack Compose Material 3, Room SQLite (v19, 40 entities), and Clean MVVM architecture.

---

## 🔒 The Local-First Privacy Principles

LifeOS is built with strict privacy and architectural guarantees:

1. **Zero Cloud Backends & Zero User Tracking**: No proprietary servers, user databases, or cloud sync backends.
2. **Zero Analytics, Telemetry, or Advertising SDKs**: No trackers, device fingerprinting, or diagnostic beacons.
3. **100% On-Device Data Sovereignty**: All tasks, habits, schedules, notes, trackers, and health data reside strictly on your device.
4. **Transparent Google Play Subscriptions**: Core productivity tools are 100% free forever. An optional monthly Premium subscription (with 7-day free trial) unlocks cosmetic themes and bonus perks via Google Play Billing.
5. **Zero Login or Mandatory Accounts**: No registration or email required.
6. **User-Controlled Local Backups**: Full JSON export and restore via Android Storage Access Framework (SAF).

---

## ✨ Features

### ⬡ Axial Honeycomb Dashboard & True Drag-and-Drop
- **Regular Pointy-Top Hexagonal Geometry**: Mathematically placed axial coordinates $(q, r)$ with constant radial metrics:
  $$\text{Width} = \sqrt{3} R, \quad \text{Height} = 2 R$$
  $$H_{\text{step}} = \sqrt{3} R + S, \quad V_{\text{step}} = 1.5 R + S$$
- **Touch Drag-and-Drop Customization**: Long-press any hex cell to elevate, smoothly drag across the grid, highlight candidate drop locations, and release to move or swap coordinates.
- **7 Geometry Layout Templates**: Standard Honeycomb, Pyramid, Diamond, Heart, Snake, Zigzag, and Freeform arrangements.

### 🔔 Task Completion Feedback System
- **Synthesized Harmonic Chime**: Pristine, local 180ms synthesized harmonic WAV audio (880 Hz fundamental + 1320 Hz + 1760 Hz overtones with smooth attack/decay envelope) generated on-device and cached locally.
- **Crisp Haptics**: Native vibration pulses using `VibrationEffect.createPredefined(EFFECT_CLICK)` or `createOneShot`.
- **Spring Checkmark Animation**: Interactive circle fill transition, checkmark emergence, and subtle scale pulse.
- **Full User Control**: Granular ON/OFF toggles for Sound, Vibration, and Animation in Settings.
- **Rapid-Tap Debouncing**: Protected against duplicate triggering on rapid clicks (<150ms).

### 📋 Task Management & Advanced Scheduling
- **Quick Add & Advanced Editor**: Priority levels, recurrence rules, tags, subtasks, notes, labeled links, and duration estimates down to seconds.
- **Reliable Task Reminders**: P1 alarms, explicit reminder-permission feedback, and date-aware reminder rescheduling; medicine reminders keep tasks at P1.
- **Advanced Recurrence Engine**: Daily, Weekly (custom weekday combinations such as Tue/Thu/Sun), Every N Days, Monthly (with month-end clamping), and Yearly (with leap-year handling).
- **Occurrence Overrides**: Skip or reschedule individual occurrences without breaking recurring series.
- **Schedule Conflict Engine**: Proactive detection of sleep window collisions (wake up / bedtime), overlapping commitments, and duplicate tasks.

### 📅 Calendar & Planning
- **Multi-View Calendar**: Seamlessly switch between Day, Week, and Month perspectives.
- **Balanced Non-Trapping Scrolling**: Weighted layout hierarchy ensuring smooth scrolling across dense schedules.
- **Multi-Horizon Planning**: Dedicated daily, weekly, monthly, quarterly, and yearly review and goal tracking.

### 💧 Habits & 75-Day Challenge
- **Horizontally Scrollable Habit Strip**: Compact, scrollable habit tracker providing quick one-tap check-ins.
- **75 Hard Presets**: Official 75 Hard, 75 Soft, and 75 Medium with integrated hydration targets, two-a-day workouts, reading targets, and progress tracking.

### 🎨 14 Handcrafted Theme Environments
1. **Genesis (Default Free)**: Clean, high-contrast obsidian & gold distraction-free neutral theme.
2. **Cyberpunk Neon**: High-contrast neon teal and ultraviolet laser accents.
3. **Tokyo Sakura**: Soft blossom pinks and calm paper lanterns.
4. **Retro Terminal 1984**: Vintage amber CRT phosphor aesthetic.
5. **Obsidian Minimalist**: Pure monochrome AMOLED black and white.
6. **Nordic Frost**: Glacial blue, ice crystal reflections, and crisp lines.
7. **Solar Flare**: Vibrant coronal gold, embers, and energetic amber.
8. **Velvet Midnight**: Royal deep purple and dark midnight blues.
9. **Emerald Forest**: Lush moss greens and organic nature tones.
10. **Sunset Horizon**: Warm gradient dusk oranges and twilight purples.
11. **Royal Amethyst**: Regal violet with gold filigree highlights.
12. **Synthwave 80s**: Retro-futuristic magenta, cyan grids, and sunset neon.
13. **Monokai Pro**: Classic code editor palette tuned for focus.
14. **Deep Abyssal**: Ocean abyss navy with bioluminescent accents.

### 💾 Safe Offline Backup & Restore
- **Deterministic JSON Export & Import (v1 and v2)**: Complete snapshot of all 40 database entities, tasks, habits, challenge progress, and honeycomb layouts.
- **Version 19 Room SQLite Database**: Enforces strict foreign-key integrity with automatic orphan quarantine and zero silent data loss.

---

## 🛠️ Tech Stack & Architecture

- **Language:** [Kotlin 2.0.21](https://kotlinlang.org/)
- **UI Framework:** [Jetpack Compose Material 3](https://developer.android.com/jetpack/compose)
- **Architecture:** Unidirectional Data Flow (UDF), MVVM, and Repository Pattern
- **Local Persistence:** [Room SQLite v6](https://developer.android.com/training/data-storage/room) & [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore)
- **Asynchronous Flow:** Kotlin Coroutines & `StateFlow`
- **Background Work & Alarms:** `AlarmManager` (with `SCHEDULE_EXACT_ALARM`) and `WorkManager`
- **Audio & Haptics:** Android `SoundPool` and `Vibrator`

---

## 🚀 Building & Running from Source

### Prerequisites
- [Android Studio Ladybug (or newer)](https://developer.android.com/studio)
- JDK 17 or JDK 21 (JDK 21 recommended)
- Android SDK Platform 36 and Build-Tools 36.0.0

### Step-by-Step Setup
```bash
# 1. Clone the repository
git clone https://github.com/kadhiravan/LifeOS.git
cd LifeOS

# 2. Compile Debug Sources
./gradlew compileDebugSources --console=plain

# 3. Run Domain Unit Tests
./gradlew testDebugUnitTest --console=plain

# 4. Build Debug APK
./gradlew assembleDebug --console=plain
```
Output APK is located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🧪 Unit Testing

LifeOS includes a comprehensive, 100% offline JVM unit test suite (104 passing tests across 8 test suites) covering:
- Recurrence patterns and edge cases (leap years, month-end clamping, skipped occurrences)
- Schedule conflict detection (sleep windows, overlapping slots, duplicate items)
- Axial hexagonal geometry calculations and cube-coordinate rounding inverse mapping
- Honeycomb template generation and coordinate swapping
- Harmonic chime audio synthesis math and RIFF WAV header verification
- Theme registry and entitlement resolution

To run all unit tests:
```bash
./gradlew testDebugUnitTest
```

---

## 📄 License & Attribution

Distributed under the **MIT License**. See [`LICENSE`](LICENSE) for details.

Developed with ❤️ by **Kadhiravan**.
