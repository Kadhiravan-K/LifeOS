# Security Policy

## Supported Versions

| Version | Supported |
| ------- | --------- |
| 1.0.0-rc.1 / 1.0.x | :white_check_mark: |
| < 1.0.0 | :x: |

## Security Model & Threat Assessment

**LifeOS is architected as an offline-only application.**
- There are no internet sockets, HTTP clients, web views, or network listeners.
- No network traffic can be sent or received because the `android.permission.INTERNET` permission is completely omitted from the `AndroidManifest.xml`.
- Attack vectors related to remote code execution, server-side data leaks, man-in-the-middle (MITM) attacks, and insecure network transports are not present.

## Reporting a Vulnerability

If you discover a local security vulnerability (such as improper intent handling, insecure exported components, or data leakage via IPC), please report it responsibly:

1. **Do not open a public GitHub issue.**
2. Send an email to the lead maintainer with the subject `[SECURITY] LifeOS Vulnerability Report`.
3. Include:
   - Description of the vulnerability.
   - Steps to reproduce or proof-of-concept.
   - Affected Android versions or device models.
4. You will receive an acknowledgment within 48 hours, followed by a remediation timeline.
