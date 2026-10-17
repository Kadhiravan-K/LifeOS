# Contributing to LifeOS

Thank you for your interest in contributing to **LifeOS**! We welcome community contributions to help improve code quality, enhance stability, and refine user experience.

---

## 🔒 Foundational Principles (Non-Negotiable)

LifeOS is an offline-first, privacy-respecting life operating system. Every contribution must strictly adhere to the following principles:

1. **No Internet Permissions:** The application will never request `android.permission.INTERNET`. No networking libraries (Retrofit, Ktor, OkHttp) or remote APIs may be added.
2. **No Tracking or Cloud SDKs:** No Firebase, Google Analytics, telemetry libraries, or cloud sync backends.
3. **No Monetization SDKs or Ads:** No advertising networks or in-app payment libraries.
4. **Zero Data Loss:** Never use destructive database migrations (`fallbackToDestructiveMigration`). Schema updates must include explicit, forward-compatible Room migrations.
5. **No Committed Secrets or Private Keys:** Do not commit keystores, passwords, or personal signing configurations.

---

## 🛠️ Development Workflow

### 1. Fork & Clone
Fork the repository on GitHub, then clone your fork locally:
```bash
git clone https://github.com/<your-username>/LifeOS.git
cd LifeOS
```

### 2. Create a Feature Branch
Create a branch with a descriptive name:
```bash
git checkout -b feature/my-enhancement
```

### 3. Build & Verify Locally
Ensure your code compiles and all unit tests pass:
```bash
./gradlew compileDebugSources --console=plain
./gradlew testDebugUnitTest --console=plain
```

### 4. Code Style & Architecture
- **Language:** Kotlin 2.0+ idiomatic code.
- **UI:** Jetpack Compose with Material 3 tokens.
- **State Management:** Unidirectional Data Flow (UDF) with `StateFlow` and Compose `collectAsStateWithLifecycle()`.
- **Formatting:** Clean indentation, meaningful variable names, and preservation of existing architecture comments.

### 5. Submit a Pull Request
1. Commit your changes with clear, descriptive commit messages.
2. Push your branch to GitHub.
3. Open a Pull Request against the `main` branch.
4. Provide a clear summary of what your PR introduces and verify that all unit tests pass.

---

## 🧪 Testing Guidelines

When adding or modifying domain logic, recurrence calculations, or UI geometry, always add corresponding unit tests in `app/src/test/java/com/example/`. Tests must be pure JVM tests that run offline without external network dependencies.
