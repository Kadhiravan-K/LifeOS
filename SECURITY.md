# Security Policy

## Supported Versions

| Version | Supported |
| ------- | --------- |
| 4.0.x / Release 4 | :white_check_mark: |
| 3.0.x / Release 3 | :white_check_mark: |
| < 3.0.0 | :x: |

## Security Model & Threat Assessment

**LifeOS is architected as an offline-only application.**
- There are no internet sockets, HTTP clients, web views, or network listeners.
- No network traffic can be sent or received because the `android.permission.INTERNET` permission is completely omitted from the `AndroidManifest.xml`.
- Attack vectors related to remote code execution, server-side data leaks, man-in-the-middle (MITM) attacks, and insecure network transports are not present.

## Profile Security Architecture & Credential Protection

LifeOS enforces independent security configurations for every profile:
- **Independent Profile Credentials**: Each profile independently manages its own security enablement, authentication method (PIN, Password, Pattern), and biometric unlock settings. Credential configurations are keyed directly to each profile's stable identifier (`profileId`).
- **Cryptographic Credential Storage**: Passwords, numeric PINs, and pattern sequences are securely salted using 32-byte cryptographically secure pseudorandom numbers (`SecureRandom`) and hashed with `PBKDF2WithHmacSHA256` over 10,000 iterations. Plaintext credentials and raw patterns are never stored or logged.
- **Interactive Pattern Engine**: 3x3 pattern lock drawing operates via a dedicated drag gesture surface with automatic intermediate collinear dot bridging, connecting line rendering, and minimum 4-dot complexity enforcement.
- **Biometric Authentication**: Biometric verification integrates exclusively with Android Jetpack `androidx.biometric.BiometricPrompt`. LifeOS never accesses or stores raw biometric data. Biometric capabilities are checked dynamically with real-time lifecycle refresh, and per-profile biometric toggles are isolated per workspace.
- **Profile Switching & Deletion Protection**: Profile switching evaluates the target profile's security credentials and requires explicit authentication before activating the workspace. Deleting a profile immediately cascades and purges its stored security configuration.

## Reporting a Vulnerability

If you discover a local security vulnerability (such as improper intent handling, insecure exported components, or data leakage via IPC), please report it responsibly:

1. **Do not open a public GitHub issue.**
2. Send an email to the lead maintainer with the subject `[SECURITY] LifeOS Vulnerability Report`.
3. Include:
   - Description of the vulnerability.
   - Steps to reproduce or proof-of-concept.
   - Affected Android versions or device models.
4. You will receive an acknowledgment within 48 hours, followed by a remediation timeline.
