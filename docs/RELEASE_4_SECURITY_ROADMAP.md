# LifeOS Release 4 — Security Hardening Roadmap & Remediation Log

This document tracks the validation, architecture, and remediation of security findings across **LifeOS Release 4**, building upon the immutable production baseline **v3.0.0** (`2233a958a23194e5461bea4c76d4b69b99ad3147`).

---

## 1. Overall Security Findings Status

| Finding ID | Description | Severity | Target Phase | Status |
|---|---|:---:|:---:|:---:|
| **SEC-01** | Exported widget receiver IPC injection | 🔴 HIGH | Phase 4.1 | **REMEDIATED** |
| **SEC-02** | Plaintext SQLite storage of sensitive/medical data | 🔴 HIGH | Phase 4.4 | Planned |
| **SEC-03** | Android backup and extraction exposure | 🔴 HIGH | Phase 4.1 | **REMEDIATED** |
| **SEC-04** | Overly broad FileProvider path mappings | 🟡 MEDIUM | Phase 4.1 | **REMEDIATED** |
| **SEC-05** | Disabled R8 minification and resource shrinking | 🟡 MEDIUM | Phase 4.5 | Planned |
| **SEC-06** | McpSecurityManager null profile ID check | 🟢 LOW | Phase 4.2 | Planned |
| **SEC-07** | Missing dynamic `FLAG_SECURE` window protection | 🟡 MEDIUM | Phase 4.2 | Planned |
| **SEC-08** | Plaintext JSON user data backup export | 🟡 MEDIUM | Phase 4.3 | Planned |
| **SEC-09** | Lack of root / device integrity awareness | ⚪ INFO | Phase 4.2 | Planned |

---

## 2. Phase 4.1: Component Isolation & Storage Boundary Hardening (Completed)

### Remediated Findings

#### SEC-01 — Widget IPC Isolation
- **Vulnerability**: `QuickRemindersWidgetProvider` was declared with `exported="true"` and an intent-filter accepting `com.example.lifeos.ACTION_WIDGET_TOGGLE_TASK`. Any third-party application on the device could broadcast this intent to mutate tasks, habits, and hydration logs without user interaction.
- **Remediation**:
  1. Removed `com.example.lifeos.ACTION_WIDGET_TOGGLE_TASK` from public `<intent-filter>` in `AndroidManifest.xml`.
  2. Implemented `QuickRemindersActionReceiver` as a strictly unexported component (`android:exported="false"`).
  3. Routed tile `PendingIntent`s through `QuickRemindersActionReceiver.createPendingIntent(...)` using explicit intents.
  4. Added parameter validation, length bounding, URI scheme check (`lifeos://`), and an allowed type whitelist (`TASK`, `HABIT`, `WATER`, `CHALLENGE_RULE`).
  5. Stripped mutation logic from `QuickRemindersWidgetProvider.onReceive()`, restricting it solely to AppWidget lifecycle updates.
- **Verification**:
  - `Release4Phase1SecurityUnitTest.sec01_createPendingIntent_targetsActionReceiverExplicitly`: PASS.
  - `Release4Phase1SecurityUnitTest.sec01_quickRemindersActionReceiver_allowedTypes_isStrictWhitelist`: PASS.
  - `Release4Phase1SecurityUnitTest.sec01_actionReceiver_rejectsNonLifeosScheme`: PASS.
  - `Release4Phase1SecurityUnitTest.sec01_quickRemindersWidgetProvider_ignoresLegacyToggleAction`: PASS.

#### SEC-03 — Android Backup & Extraction Hardening
- **Vulnerability**: `android:allowBackup="true"` with commented-out extraction rules allowed `adb backup` and automated Google Drive cloud backup to extract the entire unencrypted SQLite database and preferences.
- **Remediation**:
  1. Set `android:allowBackup="false"` in `AndroidManifest.xml`.
  2. Configured `data_extraction_rules.xml` (Android 12+ / API 31+) to explicitly exclude `database`, `sharedpref`, `file`, and `root` domains for both cloud backup and device transfer.
  3. Configured `backup_rules.xml` (Android 7.0–11 / API 24–30) to explicitly exclude `database`, `sharedpref`, `file`, and `root` domains.
  4. User-controlled in-app JSON/PDF export remains 100% operational via intentional sharing intents.
- **Verification**:
  - `Release4Phase1SecurityUnitTest.sec03_manifest_hasAllowBackupFalse`: PASS.
  - `Release4Phase1SecurityUnitTest.sec03_dataExtractionRules_excludesSensitiveDomains`: PASS.
  - `Release4Phase1SecurityUnitTest.sec03_backupRules_excludesSensitiveDomains`: PASS.

#### SEC-04 — FileProvider Scope Reduction
- **Vulnerability**: `file_paths.xml` declared `<files-path name="app_files" path="." />` and `<cache-path name="cached_reports" path="." />`, exposing the entire root of internal private files and cache directories.
- **Remediation**:
  1. Completely removed the unused `<files-path>` root mapping.
  2. Restricted `<cache-path>` strictly to `<cache-path name="shared_reports" path="shared_reports/" />`.
  3. Updated `EssentialsExportHelper.kt` to write CSV and PDF reports into `File(context.cacheDir, "shared_reports")`.
- **Verification**:
  - `Release4Phase1SecurityUnitTest.sec04_filePathsXml_strictlyScopedToSharedReports`: PASS.
  - `Release4Phase1SecurityUnitTest.sec04_csvExportGeneratesValidFileInSharedReports`: PASS.

---

## 3. Platform & Version Considerations

- **Android 14+ (API 34+)**: The unexported `QuickRemindersActionReceiver` complies with Android 14's strict broadcast receiver export rules (`RECEIVER_NOT_EXPORTED` behavior enforced at manifest level).
- **Android 12+ (API 31+)**: `data-extraction-rules` take precedence over legacy backup rules, protecting data during both cloud backup and device-to-device transfers.
- **Android 7.0–11 (API 24–30)**: Protected by `android:allowBackup="false"` and `backup_rules.xml` domain exclusions.

---

## 4. Remaining Security Roadmap

- **Phase 4.2**: MCP Authorization Tightening (SEC-06), Dynamic `FLAG_SECURE` (SEC-07), Non-blocking Environment Check (SEC-09).
- **Phase 4.3**: Authenticated Password-Protected Encrypted Export/Import (`.lifeospkg` - SEC-08).
- **Phase 4.4**: SQLCipher Hardware-Backed KeyStore Database Migration (SEC-02).
- **Phase 4.5**: Release Build Minification & R8 Shrinking (SEC-05).
