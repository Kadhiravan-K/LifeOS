# Copilot instructions for LifeOS

## Project overview

LifeOS is a privacy-first Android productivity app built with Kotlin, Jetpack Compose, Material 3, Room SQLite, and DataStore. The app is intentionally local-first: data lives on-device, there is no cloud sync/backend, and the repository is designed around offline-first persistence and deterministic import/export flows.

The important architectural shape is:

- `app/src/main/java/com/example/MainActivity.kt` boots the app, creates the database/preferences objects, and wires the repository into the main `LifeViewModel`.
- `app/src/main/java/com/example/data/local/AppDatabase.kt` is the central Room database and is the place to add schema changes and explicit migration steps.
- `app/src/main/java/com/example/data/repository/LifeRepository.kt` is the main repository for app state; it coordinates DAO access, preferences, and domain engines.
- `app/src/main/java/com/example/ui/viewmodel/LifeViewModel.kt` exposes `StateFlow`/UI state for Compose screens; the UI reads these flows rather than directly manipulating Room/DAO objects.
- `app/src/main/java/com/example/domain/...` holds the domain logic for scheduling, recurrence, analytics, medication, gamification, templates, and related business rules.
- `app/src/main/java/com/example/ui/...` contains Compose screens and reusable UI components.
- Feature areas such as notifications, security, widgets, and MCP integrations are kept as separate top-level packages rather than mixed into the data layer.

When making changes, follow the existing boundaries: keep persistence in `data`, domain rules in `domain`, UI state in `viewmodel`, and Compose drawing in `ui`.

## Build, test, and lint commands

Run everything from the repo root:

```bash
./gradlew compileDebugSources --console=plain
./gradlew testDebugUnitTest --console=plain
./gradlew lintDebug --console=plain
./gradlew assembleDebug --console=plain
```

The repo CI workflow in `.github/workflows/android-ci.yml` runs:

```bash
./gradlew testDebugUnitTest --console=plain
./gradlew assembleDebug --console=plain
```

To run a single test class or method:

```bash
./gradlew testDebugUnitTest --tests "com.example.LifeOSDomainUnitTest"
./gradlew testDebugUnitTest --tests "com.example.domain.recurrence.*"
```

Test files live under `app/src/test/java/com/example` and are JVM/unit tests meant to run offline without requiring network access or emulator-only behavior.

## Key repository conventions

- Privacy and architecture constraints are not optional: this project intentionally does not use internet permissions, cloud backends, analytics SDKs, or tracking libraries.
- Database changes must remain explicit and forward-compatible. When a schema changes, add a new `Migration` in `AppDatabase.kt`; do not use destructive migration patterns.
- Prefer Room/DAO + `DataStore` + repository patterns over ad hoc state storage or direct database access from Compose screens.
- Domain logic should stay in the `domain` layer and be covered by unit tests in `app/src/test/java/com/example` rather than mixing business rules into UI code.
- Compose screens use Material 3 and flow-based UI state (`StateFlow` + `collectAsStateWithLifecycle()`). Keep the screen layer focused on presentation and orchestration, not database logic.
- This app has a large number of local feature domains (tasks, habits, calendar, widgets, subscriptions, medication, security, QR/template flows, MCP tooling, backup/restore). Reuse existing abstractions before creating new patterns.
- The codebase is highly feature-rich and release-oriented; keep changes consistent with the existing feature-by-feature, local state design rather than introducing generic app architecture changes.

## General guidance for future sessions

- Prefer surgical edits that match the surrounding package patterns and naming conventions.
- For Room schema updates or app-state changes, check the existing data layer and migration code before adding a new pattern.
- For recurring logic, scheduling, or analytics changes, validate with the domain/unit test suite rather than only relying on manual UI verification.
- Keep changes offline, deterministic, and local-first; avoid introducing network calls or cloud storage dependencies.
