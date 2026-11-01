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

---

## 5. Phase P0: Engineering Safety Net & Schema Export (Completed)

### 5.1 Room Schema Export Configuration
- Room schema export enabled with `exportSchema = true` in `com.example.data.local.AppDatabase`.
- Configured KSP compiler argument in `app/build.gradle.kts`:
  ```kotlin
  ksp {
      arg("room.schemaLocation", "$projectDir/schemas")
  }
  ```
- Configured Gradle `sourceSets` for `test` and `androidTest` to include `$projectDir/schemas` as asset directories for migration testing.
- Schema artifact generated and committed: `app/schemas/com.example.data.local.AppDatabase/18.json` (formatVersion: 1, identityHash: `bfee67a22c51641ea30e02c65c2198b0`).

### 5.2 Room Database Migrations Test Suite
Verified complete sequential migration chain across historical SQLite versions (1 through 18) using Robolectric in `RoomMigrationRobolectricUnitTest.kt`:
1. `testRealSqliteMigrationV1ToV6`: Tests M1_2 through M5_6 on real SQLite schemas.
2. `testRealSqliteMigrationV6ToV10`: Tests gamification, medication, tracker, and finance migrations.
3. `testRealSqliteMigrationV10ToV15`: Tests profile partitioning (M10_11), recurrence windows, intervals, workspaces, and glossary migrations.
4. `testFullSequentialMigrationV1ToV18`: Real SQLite database initialized at Version 1 and migrated sequentially through all 17 migrations (1 -> 2 -> ... -> 18). Confirms `PRAGMA integrity_check == "ok"`, table count, column persistence, and profile isolation default constraints.

### 5.3 40-Entity Baseline Inventory & P1 Backup Gap Analysis
The Release 4 Room database contains **40 entities**. Current backup export in `DataExportImportManager.kt` covers 27 entity types. Exactly **13 tables** are missing from current backup and restoration routines, scheduled for full coverage in Phase P1:
1. `challenges` (`ChallengeEntity`)
2. `task_label_cross_ref` (`TaskLabelCrossRef`)
3. `task_reminders` (`TaskReminderEntity`)
4. `medication_doses` (`MedicationDoseEntity`)
5. `trash_items` (`TrashItemEntity`)
6. `xp_events` (`XpEventEntity`)
7. `achievements` (`AchievementEntity`)
8. `reward_items` (`RewardItemEntity`)
9. `reward_redemptions` (`RewardRedemptionEntity`)
10. `usage_entries` (`UsageEntryEntity`)
11. `inventory_adjustments` (`InventoryAdjustmentEntity`)
12. `price_history` (`PriceHistoryEntity`)
13. `mcp_audit_logs` (`McpAuditLogEntity`)

---

## 6. Phase P1: Complete & Verifiable Backup/Restore v2 (Completed)

### 6.1 Architecture & Format v2 Specification
- **100% Coverage across all 40 Room entities**: Eliminates the 13-entity omission gap identified during P0. Exactly 0 database views exist (`"views": []` in Room schema 18.json).
- **Format v2 JSON Schema**:
  ```json
  {
    "version": 2,
    "formatVersion": 2,
    "appVersion": "4.0.0",
    "databaseVersion": 18,
    "exportedAt": 1730467200000,
    "metadata": {
      "formatVersion": 2,
      "appVersion": "4.0.0",
      "databaseVersion": 18,
      "exportedAt": 1730467200000,
      "checksumSha256": "4a58b8...",
      "totalRecords": 45
    },
    "profiles": [ ... ],
    "profile_surveys": [ ... ],
    ...
    "mcp_audit_logs": [ ... ]
  }
  ```
- **Payload Integrity Checksum**:
  - SHA-256 digest calculated strictly over the canonical entity data blocks (excluding variable metadata).
  - Any byte tampering or accidental file truncation is detected during pre-restore validation and strictly rejected.

### 6.2 Transactional REPLACE_ALL Restore & Safety Guarantees
- **Atomic Transaction**:
  - Restorations execute entirely within `database.withTransaction`.
  - In the event of any JSON error, disk exhaustion, or foreign key violation, Room rolls back the database to its pristine pre-restore state.
- **Topological Dependency Ordering**:
  - Table deletions execute in reverse-dependency order:
    `mcp_audit_logs` -> `mcp_servers` -> `glossary_user_data` -> `glossary_items` -> `glossary_packs` -> `project_custom_properties` -> `project_custom_statuses` -> `project_workspace_views` -> `project_workspace_items` -> `subscription_price_history` -> `subscription_payments` -> `subscription_bills` -> `subscriptions` -> `tracker_inventory_adjustments` -> `tracker_purchases` -> `tracker_usage_entries` -> `tracker_cycles` -> `trackers` -> `medication_doses` -> `medications` -> `reward_redemptions` -> `reward_items` -> `achievements` -> `xp_events` -> `gamification_profile` -> `plan_task_cross_refs` -> `plan_milestones` -> `plans` -> `trash_items` -> `honeycomb_shortcuts` -> `task_reminders` -> `task_label_cross_ref` -> `tasks` -> `labels` -> `projects` -> `water_logs` -> `challenges` -> `habits` -> `profile_surveys` -> `profiles`.
  - Table insertions execute in forward-dependency order, starting with root `profiles` and ending with audit logs.
- **Pre-Restore Recovery Backup**:
  - Prior to initiating the Room transaction, `createPreRestoreRecovery` exports the entire active database to `context.filesDir/recovery_backups/pre_restore_recovery.json`.
  - Ensures a local recovery snapshot remains accessible even in the event of unexpected application termination.
- **Storage Access Framework (SAF) Integration**:
  - Safe import/export using Android's `ActivityResultContracts.CreateDocument("application/json")` and `OpenDocument()`.
  - Requires zero broad file permissions (`WRITE_EXTERNAL_STORAGE` / `MANAGE_EXTERNAL_STORAGE`).
- **Backward Compatibility**:
  - Legacy v1 backups (which lack metadata, checksums, and profile partition fields) validate with `isLegacy = true` and restore cleanly with default profile assignment (`profileId = 1L`).

### 6.3 Verification & Test Evidence
- **Automated Test Suite**: `app/src/test/java/com/example/data/backup/Release4BackupRestoreV2UnitTest.kt` (7 tests, all passing):
  1. `testAll40EntitiesRoundTripWithFieldPreservation`: Full round-trip seed -> export -> validation -> restore -> verification across all 40 entities.
  2. `testChecksumTamperingDetectedAndRejected`: Asserts modified payloads fail validation and abort restore.
  3. `testUnsupportedFutureVersionRejected`: Asserts unknown backup format versions (e.g. 99) are safely rejected.
  4. `testBackwardCompatibilityWithLegacyV1Backup`: Reconstructed v1 backup restores tasks, habits, and water logs cleanly with default profile mapping.
  5. `testReplaceAllSemanticsAndOrphanRemoval`: Confirms previous obsolete records in target DB are completely removed.
  6. `testPreRestoreRecoveryBackupCreated`: Verifies recovery backup file is written before transaction execution.
  7. `testStreamExportAndImportRoundTrip`: Verifies SAF stream-based reading and writing without corruption.
- **Build Quality**:
  - `.\gradlew.bat testDebugUnitTest`: **BUILD SUCCESSFUL** (0 failures).
  - `.\gradlew.bat assembleDebug`: **BUILD SUCCESSFUL** (clean APK packaging).
  - `.\gradlew.bat lintDebug`: **BUILD SUCCESSFUL** (0 errors).

---

## 7. Phase P2-L1: Receiver Security, goAsync(), and Widget Action Hardening (Completed)

### 7.1 Component Isolation & Receiver Hardening (SEC-01)
- **Non-Exported Reminder Component**:
  - `ReminderBroadcastReceiver` is unexported (`android:exported="false"`), preventing arbitrary external applications from triggering notification alerts or snoozing/completing user tasks.
  - All `PendingIntent` creation sites across notifications and widgets enforce explicit targeting (`Intent(context, TargetClass::class.java)`) and immutable flags (`FLAG_IMMUTABLE` / `FLAG_UPDATE_CURRENT`).
- **Strict Intent Action Whitelist**:
  - `ReminderBroadcastReceiver.onReceive()` strictly evaluates inbound actions against a closed set:
    `ACTION_DAILY_REMINDER`, `ACTION_TASK_REMINDER`, `ACTION_TASK_COMPLETE`, `ACTION_TASK_SNOOZE`, `ACTION_MEDICINE_REMINDER`, `ACTION_MEDICINE_TAKEN`, `ACTION_MEDICINE_SNOOZE`, `ACTION_MEDICINE_SKIP`.
  - Unknown, null, or foreign actions are immediately rejected and logged without processing.
- **Dedicated Boot Completion Handling**:
  - System boot completion is isolated to `BootCompletedReceiver`, protected by `android.permission.RECEIVE_BOOT_COMPLETED`.
  - Re-scheduling of active alarms delegates cleanly to `ReminderActionHandler.rescheduleAllAlarmsOnBoot(context)`.

### 7.2 Asynchronous Receiver Architecture & ANR Prevention (REM-01)
- **`runAsync` Coroutine Extension**:
  - Implemented `BroadcastReceiver.runAsync(timeoutMs = 9_000L)` and `runAsyncInternal()` in `com.example.notifications.BroadcastReceiverExtensions.kt`.
  - Synchronously invokes `goAsync()` on the main thread, holding a `BroadcastReceiver.PendingResult` to inform the Android OS of active background processing.
  - Offloads database I/O, notification posting, and alarm scheduling to `Dispatchers.IO`.
  - Enforces bounded execution with `withTimeout(timeoutMs)` (9,000ms ceiling, safely preventing the Android 10-second BroadcastQueue ANR threshold).
  - Guarantees `pendingResult.finish()` is invoked exactly once inside a `finally` block under all execution outcomes: normal completion, timeout cancellation, or unhandled exception.
  - Built-in headless test resilience: logging calls gracefully catch unmocked `android.util.Log` exceptions during pure JVM unit testing.
- **Adoption Across Receivers & AppWidget Providers**:
  - Fully integrated in `ReminderBroadcastReceiver`, `BootCompletedReceiver`, and `QuickRemindersActionReceiver`.
  - Integrated into widget update cycles across `QuickRemindersWidgetProvider`, `SubscriptionWidgetProvider`, `TrackerWidgetProvider`, and `ProductivityRadarWidgetProvider`.

### 7.3 Reminder Domain Action Handler (RFR-04)
- Extracted domain business logic out of `ReminderBroadcastReceiver` into dedicated `ReminderActionHandler`.
- Manages complete lifecycle for daily reminders, task alarms, snooze intervals, task completions, and medication doses.
- Eliminates receiver bloat while ensuring high testability and clean domain separation.

### 7.4 Quick Reminders Widget Task Resolution & Action Hardening (SEC-02 / WID-01)
- **Resolution Strategy**:
  - Primary resolution: Resolves tasks strictly by numeric ID (`targetId`) parsed from the URI or intent extras.
  - Fallback resolution: Looks up tasks by exact title (case-insensitive) ONLY if exactly one task matches.
  - Disambiguation: If 0 or >1 tasks match the title, resolution is rejected. Substring matching is strictly eliminated.
- **Elimination of Silent Task Creation**:
  - Completely removed `getOrCreateTaskForShortcut`.
  - Unlinked, corrupt, or unresolvable shortcuts fail safely without silently injecting unexpected tasks into the user's database.

### 7.5 Verification & Test Evidence
- **Automated Test Suites**:
  1. `ReminderReceiverSecurityTest` (6 tests):
     - `testUnknownActionRejected`: Rejects unapproved action strings.
     - `testNullActionHandledSafely`: Handles null actions without crashing.
     - `testBootCompletedNotHandledByReminderReceiver`: Confirms `ACTION_BOOT_COMPLETED` is discarded by reminder receiver.
     - `testAllowedActionsAreWhitelisted`: Verifies all approved actions pass through.
     - `testPendingIntentTargetingIsExplicit`: Confirms explicit component targeting.
     - `testPendingIntentUsesImmutableFlag`: Enforces immutable PendingIntent creation.
  2. `ReminderReceiverAsyncTest` (4 tests):
     - `testFinishCalledOnSuccess`: Verifies coroutine block execution and single finish call.
     - `testFinishCalledOnException`: Verifies exception handling and guaranteed finish call.
     - `testFinishCalledOnTimeout`: Verifies cancellation and guaranteed finish call on timeout.
     - `testMultipleBranchesFinishExactlyOnce`: Verifies determinism across branching logic.
  3. `WidgetTileResolutionTest` (5 tests):
     - `testExactIdMatchResolvesCorrectly`: Fast-path numeric ID lookup.
     - `testExactUniqueTitleMatchResolvesCorrectly`: Unique exact title resolution.
     - `testAmbiguousTitleMatchRejectsResolution`: Rejects multiple matching titles.
     - `testSubstringTitleMatchRejectsResolution`: Rejects substring matching.
     - `testNonExistentTitleRejectsWithoutCreation`: Verifies non-creation for non-existent shortcuts.
- **Build Quality**:
  - `.\gradlew.bat testDebugUnitTest`: **BUILD SUCCESSFUL** (0 failures across all unit test suites).
  - `.\gradlew.bat lintDebug`: **BUILD SUCCESSFUL** (0 errors).
  - `.\gradlew.bat assembleDebug`: **BUILD SUCCESSFUL** (clean APK compilation and packaging).



