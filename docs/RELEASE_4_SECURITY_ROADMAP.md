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

---

## 8. Phase P2-L2: Reminder Rescheduling, System Events & Exact-Alarm Permission (Completed)

### 8.1 Centralized ReminderScheduler Architecture (REM-02 / REM-03 / REM-04)
- **Unified Scheduling Abstraction**:
  - Created `ReminderScheduler` interface and `DefaultReminderScheduler` in `com.example.notifications.ReminderScheduler.kt`.
  - Replaces fragmented scheduling code previously scattered across `ReminderActionHandler` and `MaintenanceWorker`.
  - Standardized entry point: `ReminderScheduler.rescheduleAll(context, horizonDays = 7)`.
- **Bounded Scheduling Horizon & Quota Protection**:
  - **Task Reminders Horizon**: Calculates reminders across up to 7 future days (`MAX_HORIZON_DAYS = 7`).
  - **Medication Doses Horizon**: Rebuilds today's and tomorrow's doses (Days 0 and 1) to respect dynamic prescription adherence windows without pre-populating distant doses.
  - **Hard Alarm Limit (200 Alarms)**: Enforces `MAX_ALARMS = 200` to prevent exhausting Android OS system alarm queues. Returns `cappedByLimit = true` if quota is hit.
- **Idempotency & Request Code Preservation**:
  - Re-scheduling is completely idempotent; repeated broadcasts or maintenance jobs replace existing alarms in place via deterministic request codes:
    - Task Alarm: `(taskId * 31) + dateIso.hashCode().and(0x7FFFFFFF) % 50000`
    - Task Reminder Item: `((taskId * 31 + reminderId) * 31) + dateIso.hashCode().and(0x7FFFFFFF) % 100000`
    - Medication Dose Alarm: `(doseId * 37) + 50000`
    - Daily Morning Digest: `2001`
  - Never cancels alarms belonging to unrelated task IDs.
  - Safely ignores completed, archived, or deleted tasks.
- **Exact-Alarm Resilience**:
  - In `NotificationHelper`, all alarm scheduling calls wrap `canScheduleExactAlarms()` and `setExactAndAllowWhileIdle()` in comprehensive try-catch blocks.
  - Catches `SecurityException` if permission is revoked or restricted at runtime, falling back to inexact alarms (`alarmManager.set(...)`) without crashing.

### 8.2 System Broadcast Handling (REM-03 / REM-04)
- **Unified Event Handling in `BootCompletedReceiver`**:
  - Handles 5 system-protected broadcast actions:
    1. `Intent.ACTION_BOOT_COMPLETED` (Device restart)
    2. `Intent.ACTION_MY_PACKAGE_REPLACED` (App update)
    3. `Intent.ACTION_TIME_CHANGED` (`android.intent.action.TIME_SET` - Clock changes)
    4. `Intent.ACTION_TIMEZONE_CHANGED` (Timezone / DST transitions)
    5. `AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` (Exact alarm permission changes)
- **Component Security**:
  - Guarded strictly by privileged `android.permission.RECEIVE_BOOT_COMPLETED`.
  - Whitelist enforcement: incoming intents with null or unapproved actions are rejected immediately and logged.
  - Invokes `runAsync(timeoutMs = 9_000L)` to guarantee background execution and `PendingResult.finish()` completion within the 10-second ANR threshold.

### 8.3 WorkManager Maintenance Integration
- `MaintenanceWorker.doWork()` updated to invoke `ReminderScheduler.rescheduleAll(applicationContext, horizonDays = 7)`.
- Eliminates duplicated task iteration loops while preserving 24-hour periodic execution (`ExistingPeriodicWorkPolicy.KEEP`) and safe retry semantics on failure.

### 8.4 Precise Reminders Settings UI (REM-05)
- **UI Settings Row**:
  - Added "Precise reminders" card under `SettingsSubpage.NOTIFICATIONS` in `SettingsScreen.kt`.
  - Displays dynamic permission status:
    - **Granted**: Exact alarms are enabled. Reminders trigger precisely at scheduled times.
    - **Not granted**: Exact alarms are restricted. Explains that Android may delay notifications by 10–15 minutes during standby / Doze mode.
    - **Not needed**: On Android 11 and below (API < 31), exact alarms do not require special permission.
  - Displays action button ("Enable in Settings") when `NOT_GRANTED`, launching `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` with package URI.
  - Automatically refreshes displayed state on `ON_RESUME` via Compose `DisposableEffect` with `LifecycleEventObserver`.
- **Manifest Constraints**:
  - `USE_EXACT_ALARM` was NOT added to the manifest.

### 8.5 Automated Test Evidence
- **Automated Test Suites (All passing)**:
  1. `ReminderSchedulerUnitTest` (8 tests):
     - `testSevenDayHorizonCalculation`: Clamps horizon to 7 days, excludes distant tasks.
     - `testTodayAndTomorrowMedicationReminders`: Schedules doses for days 0 and 1 only.
     - `testTaskReminderReconstruction`: Rebuilds multiple explicit reminder offsets.
     - `testRepeatedSchedulingIsIdempotent`: Verifies identical alarm counts across repeated calls.
     - `testCompletedAndArchivedAndDeletedTasksIgnored`: Excludes inactive tasks.
     - `testEnforceMax200AlarmLimit`: Capped at 200 alarms with `cappedByLimit = true`.
     - `testRequestCodeFormulasRemainStable`: Confirms request codes are positive and stable.
     - `testExactAlarmPermissionHelpers`: Verifies non-crashing status check.
  2. `BootRescheduleReceiverRobolectricTest` (8 tests):
     - Confirms `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_CHANGED`, `TIMEZONE_CHANGED`, and `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` all delegate to centralized scheduler.
     - Rejects unauthorized / null actions without invoking scheduler.
     - Idempotent across sequential system events.
  3. `MaintenanceWorkerUnitTest` (3 tests):
     - Verifies worker invokes `ReminderScheduler`.
     - Verifies `Result.retry()` on exception.
     - Validates unique work name `lifeos_maintenance_worker`.
  4. `PreciseRemindersSettingsUnitTest` (4 tests):
     - Validates enum display strings.
     - Verifies permission check does not crash.
     - Validates `openExactAlarmSettings` safe execution.
     - Verifies `LifeViewModel` status refresh.
- **Build Quality**:
  - `.\gradlew.bat testDebugUnitTest`: **BUILD SUCCESSFUL** (0 failures).
  - `.\gradlew.bat lintDebug`: **BUILD SUCCESSFUL** (0 errors).
  - `.\gradlew.bat assembleDebug`: **BUILD SUCCESSFUL** (clean APK packaging).

### 8.6 Manual QA Checklist
1. Schedule a reminder for a task 2 hours in the future.
2. Reboot the device (`adb reboot`).
3. Verify alarm is restored using `adb shell dumpsys alarm | grep -E "com.aistudio.lifeos.vtrqkx|lifeos"`.
4. Change system timezone under device Settings -> Date & Time.
5. Verify alarm trigger time adjusts to match the new local time.
6. Install an updated APK using `adb install -r <apk-path>`.
7. Confirm alarm remains registered.
8. On Android 12+ device, open App Info -> Alarms & Reminders -> Revoke permission.
9. Open LifeOS Settings -> Notifications & Smart Alerts -> Confirm "Precise reminders" displays "Not granted" with warning text.
10. Tap "Enable in Settings", grant permission, return to LifeOS.
11. Confirm "Precise reminders" dynamically updates to "Granted" without app restart.

### 8.7 Remaining Android-Version Limitations
- On heavily customized OEM Android skins (MIUI, ColorOS, EMUI), background alarm delivery may still be delayed by proprietary aggressive battery managers unless the user manually enables autostart / excludes LifeOS from battery optimization.

## 9. Phase P2-L3: Reminder Health Screen (F-04 / IMP-02)

### 9.1 Overview & Architectural Objectives
- Implemented dedicated user-facing diagnostic screen `ReminderHealthScreen.kt` accessible from **Settings -> Notifications & Smart Alerts -> Reminder Health & Diagnostics**.
- Centralized real-time health checks for reminder and notification reliability across Android 8.0 through Android 14+.
- Guaranteed zero database schema mutations (Database remains at version 18 with 40 entities, no new tables).
- Zero external network dependencies, privacy-respecting and fully offline.

### 9.2 Six Diagnostic Sections Implemented
1. **Notification Permission**:
   - Status checked via `NotificationManagerCompat.from(context).areNotificationsEnabled()`.
   - Explains notifications are disabled globally if false, with one-tap action opening `Settings.ACTION_APP_NOTIFICATION_SETTINGS`.
2. **Notification Channels**:
   - Audits all 9 LifeOS notification channels registered in `NotificationHelper` (`tasks`, `subtasks`, `medicine`, `daily_digest`, `system_alerts`, `backup_restore`, `timer`, `screentime`, `export_import`).
   - For each channel, reports whether it exists, is user-muted (`importance == IMPORTANCE_NONE`), or active.
   - Provides direct link to individual channel settings via `Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS`.
3. **Exact Alarm Status**:
   - Queries `AlarmManager.canScheduleExactAlarms()` on Android 12+ (API 31+).
   - Reports `Granted`, `Not granted`, or `Not needed` (API < 31).
   - Provides direct intent link to `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.
4. **Battery Optimisation Status**:
   - Checks `PowerManager.isIgnoringBatteryOptimizations(packageName)`.
   - Clear diagnostic explanation of Doze mode and manufacturer background restrictions.
   - Actionable button opening `Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` without automatically triggering aggressive prompts.
5. **Upcoming Scheduled Reminders (Next 5)**:
   - Exposes `ReminderScheduler.getUpcomingReminders(context, limit = 5)` performing read-only query on active tasks and medication dose items across the 7-day horizon.
   - Filters out completed (`isOneTimeCompleted == true`), archived (`isArchived == true`), and soft-deleted records.
   - Computes explicit trigger epoch timestamps and returns items strictly sorted chronologically (`triggerEpochMillis ASC`).
   - No DB records created, altered, or deleted.
6. **Test Reminder Functionality**:
   - "Send test reminder in 1 minute" action using `ACTION_TEST_REMINDER` broadcast intent.
   - Uses dedicated isolated PendingIntent request code `77777` and notification ID `77777`.
   - Idempotent and cancelable at any time without impacting any real user tasks or medication reminders.
   - Zero database mutations.

### 9.3 Lifecycle & Navigation Integration
- Screen observes `Lifecycle.Event.ON_RESUME` via Compose `DisposableEffect` so returning from Android System Settings instantly refreshes diagnostic indicators without app restart.
- Integrated into `SettingsScreen.kt` as `SettingsSubpage.REMINDER_HEALTH` with full routing and search registry coverage.
- Powered by `LifeViewModel.reminderHealthState` `StateFlow` running checks off the UI thread via `Dispatchers.IO`.

### 9.4 Automated Test Evidence
- **Automated Test Suite**:
  - `ReminderHealthStateUnitTest` (9 tests passed):
    - `testEmptyUpcomingReminders`: Empty database returns empty list without crashing.
    - `testUpcomingRemindersChronologicalOrderingAndLimit`: Enforces limit of 5 and strict ascending chronological ordering.
    - `testCompletedAndArchivedTasksExcludedFromUpcoming`: Inactive tasks excluded from diagnostic query.
    - `testMedicationDoseAppearsInUpcomingReminders`: Today's medication dose correctly surfaced.
    - `testTestReminderLifecycleAndIsolation`: Verifies test reminder schedules, flags active, cancels, with zero DB writes.
    - `testRepeatedTestReminderSchedulingIsIdempotent`: Overwrites existing test alarm without duplicate entries.
    - `testBatteryOptimizationCheckDoesNotThrow`: Battery check safe on all Android versions.
    - `testNotificationChannelsCheckReturnsAllCategories`: Audits all 9 LifeOS channels.
    - `testViewModelReminderHealthStateRefresh`: `LifeViewModel` StateFlow integration and state refresh.
- **Full Notification Test Suite**:
  - All tests in `com.example.notifications.*` passed (`ReminderHealthStateUnitTest`, `ReminderSchedulerUnitTest`, `BootRescheduleReceiverRobolectricTest`, `MaintenanceWorkerUnitTest`, `PreciseRemindersSettingsUnitTest`, `ReminderReceiverSecurityTest`, `ReminderReceiverAsyncTest`).
- **Build Quality**:
  - `.\gradlew.bat testDebugUnitTest --tests "com.example.notifications.*"`: **BUILD SUCCESSFUL** (0 failures).
  - `.\gradlew.bat lintDebug`: **BUILD SUCCESSFUL** (0 errors).
  - `.\gradlew.bat assembleDebug`: **BUILD SUCCESSFUL** (clean APK packaging).

### 9.5 Manual QA Checklist
1. Open LifeOS Settings -> Notifications & Smart Alerts.
2. Tap "Reminder Health & Diagnostics".
3. Verify all 6 diagnostic cards are present (Notification Permission, Notification Channels, Exact-Alarm Permission, Battery Optimisation, Next Scheduled Reminders, Test Reminder).
4. Tap "System Settings" under Notification Permission -> disable notifications -> return to LifeOS -> verify status dynamically updates to "Disabled". Re-enable notifications and return.
5. In Notification Channels section, verify 9 channels are listed.
6. Verify Exact-Alarm status card matches device capability.
7. Verify Battery Optimisation card shows status and explanation of Doze mode.
8. Tap "Send Test Reminder in 1 Min" -> Verify "Active (Due in ~60s)" banner and "Cancel Test Reminder" button appear.
9. Verify that during test reminder countdown, no new tasks or medication records are created in the database.
10. Wait 1 minute -> Verify test notification arrives with title "LifeOS Test Reminder" and "Diagnostics" channel.
11. Tap "Cancel Test Reminder" before delivery -> Verify alarm is dismissed.
12. Create a task with reminder in 30 minutes -> Return to Reminder Health screen -> Verify task appears in "Next Scheduled Reminders" with correct title, type, and trigger time.

---

## 10. Phase P3-L1: Replace SimpleDateFormat with java.time (DT-01, DT-02)

### 10.1 Background & Threat Model
In Android applications, `SimpleDateFormat` is notoriously mutable and thread-unsafe. When multiple threads simultaneously invoke `format()` or `parse()` on the same instance, internal `Calendar` state is mutated, leading to race conditions, silent timestamp corruption, unpredictable parse exceptions, or deadlocks.

In LifeOS, `DateUtils` previously maintained four shared mutable `SimpleDateFormat` instances (`DATE_FORMAT`, `FULL_DATE_FORMAT`, `DISPLAY_FORMAT`, `MONTH_YEAR_FORMAT`) on a singleton object, alongside ad-hoc `SimpleDateFormat` instantiations in helper methods. These formatters were accessed concurrently by:
1. Room database query mappers and repository operations running on Dispatchers.IO.
2. Background AlarmManager receivers (`ReminderReceiver`, `BootReceiver`) and WorkManager tasks.
3. UI Composables and ViewModels updating presentation state on the Main dispatcher.

This represented a critical reliability vulnerability (audited as DT-01 and DT-02).

### 10.2 Architectural Implementation
`DateUtils.kt` was completely rewritten using Java 8+ `java.time` APIs:
- **Thread Safety**: All mutable `SimpleDateFormat`, `Calendar`, and `java.util.Date` instances were eliminated and replaced with immutable, thread-safe `java.time` classes:
  - `ISO_FORMATTER` (`DateTimeFormatter.ofPattern("yyyy-MM-dd")`)
  - `DISPLAY_FORMATTER` (`DateTimeFormatter.ofPattern("MMM d, yyyy")`)
  - `FULL_DISPLAY_FORMATTER` (`DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")`)
  - `MONTH_YEAR_FORMATTER` (`DateTimeFormatter.ofPattern("MMMM yyyy")`)
  - `MONTH_DAY_FORMATTER` (`DateTimeFormatter.ofPattern("MMMM d")`)
- **Modern Calendar Math**: Replaced `Calendar` with `LocalDate`, `LocalTime`, `DayOfWeek`, `ChronoUnit`, and `YearMonth`.
  - Day arithmetic implemented cleanly via `LocalDate.plusDays()` / `minusDays()`.
  - Month arithmetic implemented via `LocalDate.plusMonths()` / `minusMonths()`.
  - Interval math implemented via `ChronoUnit.DAYS.between()`.
  - Day-of-week parsing mapped via `DayOfWeek.getDisplayName(TextStyle.FULL, displayLocale)`.
- **Clock & Locale Injection**:
  - Added optional `clock: Clock = Clock.systemDefaultZone()` and `displayLocale: Locale = Locale.US` default parameters to public methods (`todayString`, `todayEpochDay`, `getCurrentHour`, `getCurrentMinuteOfDay`, `formatDisplayDate`, `formatFullDisplayDate`, `formatMonthYear`, `getDayOfWeek`, `getLastNDays`).
  - Preserves 100% backward binary and source compatibility across all existing LifeOS call sites while enabling fully deterministic, zero-flakiness testing.
- **Strict Format Preservation & Zero Migrations**:
  - Persisted date format remains strictly `yyyy-MM-dd`.
  - Room database schema version remains at 18 with 40 entities; 0 schema migrations were required.
  - Backup/Restore v2 JSON payload serialization compatibility is strictly preserved.
- **Resilient Fallback Handling**:
  - Malformed ISO date strings fall back gracefully without crashing or throwing unhandled exceptions.
  - Logging is safeguarded with exception-handling wrappers to avoid unmocked Android framework log crashes during host JVM testing.

### 10.3 Verification & Parity Oracle
A dedicated test suite was implemented in `app/src/test/java/com/example/utils/DateUtilsParityUnitTest.kt`:
1. **Exhaustive 40-Year Parity Oracle**:
   - Built `DateUtilsLegacyOracle` mirroring legacy `Calendar`/`SimpleDateFormat` behavior.
   - Tested **14,976 consecutive calendar days** from `2000-01-01` to `2040-12-31`.
   - Verified 100% exact parity across `isoStringToEpochDay`, `epochDayToIsoString`, `getDayOfWeek`, `addDays` (+1, +7, +30, -1, -7, -30), and `daysBetween`.
2. **Days In Month & Leap Year Verification**:
   - Validated every month (1..12) across 41 years (1900..2100).
   - Validated leap year detection across century leap years (2000), non-leap centuries (1900, 2100), and quadrennial leap years (2024, 2028).
3. **Multithreaded Stress Test**:
   - 8 concurrent threads × 10,000 iterations (80,000 total operations) performing parsing, formatting, and arithmetic concurrently.
   - 0 errors, 0 race conditions, 0 lock contention.
4. **DST Transition & Clock Injection**:
   - Tested US and European daylight saving transitions.
   - Validated deterministic fixed-clock injection for date/time resolution.
5. **ISO Compliance**:
   - Verified strict compliance with `^\d{4}-\d{2}-\d{2}$` and epoch day round-trips.

### 10.4 Quality Gate Status
- `.\gradlew.bat testDebugUnitTest --tests "com.example.utils.DateUtilsParityUnitTest"`: **PASSED** (8/8 tests, 0 failures, 0 errors).
- `.\gradlew.bat testDebugUnitTest`: **BUILD SUCCESSFUL** (All unit tests across repository passing).
- `.\gradlew.bat lintDebug`: **BUILD SUCCESSFUL** (0 errors).
- `.\gradlew.bat assembleDebug`: **BUILD SUCCESSFUL** (Clean debug APK artifact).

---

## 11. Phase P3-L2: Every-N-weeks Recurrence Alignment & Occurrence Count Optimization (REC-01, REC-02)

### 11.1 The Original Weekly Alignment Defect (REC-01)
Previously in `RecurrenceEngine.kt`, weekly interval recurrence (`EVERY_N_WEEKS` and `CUSTOM` / `WEEKS`) calculated recurrence using a rolling 7-day offset from the task's due date:
```kotlin
val daysDiff = DateUtils.daysBetween(task.dueDateIso, targetDateIso)
val weeksDiff = daysDiff / 7
weeksDiff % interval == 0
```
This algorithm suffered from a severe calendar misalignment bug:
- **Bug Scenario**: Due date: Wednesday, 1 January 2025; Recurrence: every 2 weeks (`interval = 2`); Selected weekdays: Monday and Friday.
- **Flawed Execution**:
  - Target Monday, 6 January 2025: `daysDiff = 5`, integer division `5 / 7 = 0`, `0 % 2 == 0`. Monday 6 January was incorrectly scheduled to fire during week 1, violating the 2-week interval constraint.
  - Target Monday, 13 January 2025: `daysDiff = 12`, integer division `12 / 7 = 1`, `1 % 2 != 0`. Monday 13 January was incorrectly skipped, even though it was the rightful start of week 2.
- **Root Cause**: Dividing days by 7 measures elapsed 7-day chunks rather than aligning to real calendar weeks. Dates in the immediately following calendar week that fall fewer than 7 days from the due date are trapped in chunk 0.

### 11.2 Calendar-Week Recurrence Algorithm
The weekly recurrence logic was redesigned to use calendar-week alignment:
```kotlin
val startOfWeekDue = getStartOfWeek(dueLocalDate, firstDayOfWeek)
val startOfWeekTarget = getStartOfWeek(targetLocalDate, firstDayOfWeek)
val weekIndex = (startOfWeekTarget.toEpochDay() - startOfWeekDue.toEpochDay()) / 7
if (targetLocalDate.isBefore(dueLocalDate) || weekIndex < 0L) return false

val dayMatches = if (task.recurrenceDaysOfWeek.isNotEmpty()) {
    task.recurrenceDaysOfWeek.contains(targetDayOfWeek)
} else {
    DateUtils.getDayOfWeek(task.dueDateIso) == targetDayOfWeek
}
if (!dayMatches) return false

return weekIndex % interval == 0L
```
- **Week-Start Convention**:
  - Defaults to `MONDAY` (ISO-8601 standard).
  - Supports configurable `firstDayOfWeek: String = "MONDAY"` (`"MONDAY"`, `"SUNDAY"`, `"SATURDAY"`), matching the user preference in `UserPreferences.KEY_FIRST_DAY_OF_WEEK` and `DateUtils.getWeekDays(firstDayOfWeek)`. No new preferences or settings were introduced.
- **Integer Division & Pre-Due Protections**:
  - Targets before the due date (`targetLocalDate.isBefore(dueLocalDate)`) and negative week indices (`weekIndex < 0L`) are immediately rejected, preventing integer division from incorrectly treating negative offsets (e.g., `-14 / 7 = -2`, `-2 % 2 == 0`) as valid recurrence occurrences.

### 11.3 Monthly and Yearly Edge-Case Investigation (REC-02)
Per prompt instructions, monthly and yearly edge-case behaviors were investigated and audited against the "clamp to last day of month" rule:
- **Monthly Clamping**:
  - Day 31: In 30-day months (Apr, Jun, Sep, Nov), clamps to day 30. In leap Feb (2024), clamps to Feb 29. In non-leap Feb (2025), clamps to Feb 28.
  - Day 30: In Feb, clamps to Feb 28 (non-leap) / Feb 29 (leap).
  - Day 29: In non-leap Feb, clamps to Feb 28.
  - **Audit Finding**: Existing `matchesMonthDay` in `RecurrenceEngine` already implements `startDay > maxDaysInTargetMonth -> targetDay == maxDaysInTargetMonth`. It fully conforms to "clamp to last day of month".
- **Yearly Clamping**:
  - Feb 29 in leap years recurs on Feb 29. In non-leap years (2025, 2026, 2027), it clamps to Feb 28.
  - **Audit Finding**: Existing `matchesYearlyInterval` already handles this via `if (startMonth == 2 && startDay == 29 && !targetDate.isLeapYear) targetDay == 28`.
- **Owner Approval**: No semantic changes were required or introduced because the existing behavior was already compliant with the required clamping policy. The implementation was modernized from `SimpleDateFormat`/`Calendar` to `java.time.LocalDate` with identical behavior.

### 11.4 Occurrence-Count Optimization
Previously, `countOccurrencesUpTo` performed an `O(N)` day-by-day loop (up to 1,460 iterations) evaluating `matchesBasePattern` and date conversions for every evaluated date. When evaluating calendar views or upcoming occurrences, this led to massive redundant loop execution.
- **Optimized Implementation**: Replaced day-by-day stepping with `O(1)` arithmetic occurrence counting across all recurrence types (`DAILY`, `EVERY_N_DAYS`, `WEEKDAYS`, `SELECTED_WEEKDAYS`, `WEEKLY`, `EVERY_N_WEEKS`, `MONTHLY`, `EVERY_N_MONTHS`, `YEARLY`, `EVERY_N_YEARS`, `MONTHLY_WINDOW`).
- **Equivalence Verification**: Validated 100% equivalence against an independent daily-stepping reference counter across 120 consecutive dates for each recurrence type.
- **Performance Benchmark**: Benchmarked 1,000 multi-year (5-year / 1,826 days) occurrence count evaluations; completed in **12ms** (target: < 100ms).

### 11.5 Data Preservation & Compatibility
- **Database Schema**: Untouched (Room DB v18, 40 entities preserved, 0 migrations).
- **Task Records**: Stored recurrence rules, `completedDates`, `skippedDates`, and `rescheduledDates` were NOT modified.
- **Impact on Existing Tasks**:
  - Existing bi-weekly and every-N-weeks recurring tasks will now correctly align with calendar weeks rather than rolling 7-day intervals from their due dates.
  - Historical completion records in `completedDates` are permanently preserved.

### 11.6 Quality Gate Status
- `.\gradlew.bat testDebugUnitTest --tests "com.example.domain.recurrence.RecurrenceEngineWeeklyAlignmentUnitTest"`: **PASSED** (10/10 tests, 0 failures, 0 errors).
- `.\gradlew.bat testDebugUnitTest --tests "com.example.Batch02TasksCalendarRegressionUnitTest" --tests "com.example.ui.Release3BugFixesUnitTest" --tests "com.example.Release3Phase4UnitTest" --tests "com.example.utils.DateUtilsParityUnitTest"`: **PASSED** (All regression test suites passing).
- `.\gradlew.bat testDebugUnitTest`: **BUILD SUCCESSFUL** (Full repository unit test suite passing).
- `.\gradlew.bat lintDebug`: **BUILD SUCCESSFUL** (0 errors).
- `.\gradlew.bat assembleDebug`: **BUILD SUCCESSFUL** (Clean debug APK artifact).

---

## 12. Phase P4-L1: Transactional Task Save, Update, and Delete (I1 / DI-01)

### 12.1 Defect Analysis (DI-01)
Previously, task write operations in `LifeRepository.kt` (`insertTask`, `updateTask`, `deleteTask`, `saveTaskLabels`, and `saveTaskReminders`) executed multiple discrete DAO statements sequentially without a database transaction wrapper (`database.withTransaction`):
- **Task Insertion Failure Scenario**:
  - `taskDao.insertTask` inserted the task entity into the `tasks` table.
  - Subsequently, `saveTaskLabels` (clearing and writing to `task_label_cross_ref`) and `saveTaskReminders` (clearing and writing to `task_reminders`) executed.
  - If an exception occurred during label cross-reference insertion or reminder insertion (e.g. process termination, disk error, constraint error), the outer method failed, but the row in `tasks` remained committed in SQLite without its associated labels or reminders (partial write / orphaned record).
- **Task Update Failure Scenario**:
  - `taskDao.updateTask` updated task title/properties in `tasks`.
  - `saveTaskLabels` cleared existing labels and began inserting new ones; `saveTaskReminders` cleared existing reminders and inserted new ones.
  - If a failure occurred during label or reminder insertion, the task's prior labels/reminders were wiped out while the updated task was left in a corrupted, half-migrated state.
- **Task Deletion Failure Scenario**:
  - `deleteTask` sequentially deleted entries from `task_label_cross_ref`, `task_reminders`, `honeycomb_shortcuts`, `medications`, and `medication_doses`, and finally deleted the row from `tasks`.
  - If a failure occurred prior to executing `taskDao.deleteTaskById(taskId)`, all related labels, reminders, shortcuts, and medication records were stripped from the database, while the parent task row itself remained alive in SQLite without its associated relations.

### 12.2 Atomic Transaction Boundaries
All task write and delete operations in `LifeRepository.kt` are now enclosed in atomic database transactions using Room's `database.withTransaction`:
- **`insertTask(task: TaskItem): Long`**:
  - Enclosed entirely in `database.withTransaction { ... }`.
  - Sequentially inserts the task entity, inserts task-label cross-references via `saveTaskLabelsInternal`, and inserts task reminders via `saveTaskRemindersInternal`.
  - Any exception thrown anywhere in the block causes SQLite to roll back all changes completely, ensuring no orphan task row ever remains.
- **`updateTask(task: TaskItem)`**:
  - Enclosed entirely in `database.withTransaction { ... }`.
  - Updates the task entity, clears and re-inserts task labels via `saveTaskLabelsInternal`, and clears and re-inserts reminders via `saveTaskRemindersInternal`.
  - Any mid-flight failure rolls back the entire block, preserving the previous task state, labels, and reminders intact.
- **`deleteTask(taskId: Long)`**:
  - Enclosed in `database.withTransaction { ... }`.
  - Clears task labels, reminders, shortcuts, linked medications, linked medication doses, and deletes the task row itself in a single atomic transaction.
  - If any step fails, all deletions are rolled back and all relationships remain preserved.
- **`saveTaskLabels` and `saveTaskReminders`**:
  - Refactored to delegate to `saveTaskLabelsInternal` and `saveTaskRemindersInternal`.
  - Public entry points are wrapped in `database.withTransaction { ... }` so standalone calls from `LifeViewModel` execute atomically.

### 12.3 Controlled Failure Testing & Rollback Verification
Controlled failure injection was implemented via `@VisibleForTesting internal var testTransactionInterceptor: ((step: String) -> Unit)? = null` in `LifeRepository.kt` and verified in `LifeRepositoryTransactionUnitTest.kt`:
1. **`testSuccessfulTaskInsertWithLabelsAndReminders`**:
   - Confirms normal, end-to-end insertion of task, labels, and reminders within the transaction.
2. **`testTaskInsertRollbackOnFailure`**:
   - Injects a failure immediately after `taskDao.insertTask` (`AFTER_INSERT_TASK`).
   - Prior to transaction wrapping, the defect was reproduced: the task row persisted in `tasks`.
   - With transaction wrapping: Room rolls back 100%; database verifies 0 tasks, 0 label cross-references, and 0 reminders exist.
3. **`testTaskUpdateRollbackOnFailure`**:
   - Injects a failure after `taskDao.updateTask` during label update (`AFTER_UPDATE_TASK`).
   - Verifies complete rollback: task title, original labels, and original reminders are preserved exactly as before the failed update.
4. **`testTaskDeleteRollbackOnFailure`**:
   - Injects a failure immediately before `taskDao.deleteTaskById` (`BEFORE_DELETE_TASK_ROW`).
   - Verifies complete rollback: task row, label cross-references, reminders, shortcuts, medications, and doses are 100% preserved.

### 12.4 Caller Audit: Hard Deletion vs. Soft Deletion (Trash)
An audit of all deletion callers across the codebase was conducted:
- **`TasksScreen.kt:687`**: Calls `viewModel.deleteTask(task.id)` which delegates directly to `repository.deleteTask(taskId)`. This performs a hard deletion that removes the task and its child relations from SQLite without sending it to Trash.
- **`LifeViewModel.kt:1579`**: Provides `bulkDeleteTasks(taskIds)` which delegates to `repository.bulkDeleteTasks(taskIds)` (hard delete).
- **`LifeViewModel.kt:2693`**: Provides `softDeleteTask(task: TaskItem)` which serializes the task into JSON and inserts it into `trashDao.insertTrashItem` with an expiration date for retention. However, `softDeleteTask` is not currently invoked from the primary UI task deletion paths.
- **Policy Adherence**: In accordance with the prompt constraints, existing user-facing deletion behavior was preserved without alterations (no hard deletes were converted to soft deletes in this loop).

### 12.5 Database Schema & Migration Verification
- **Schema Modification**: **None**.
- **Room Version**: Maintained at **18**.
- **Room Entities**: Maintained at **40**.
- **Migrations**: **0** migrations added or modified.
- **Foreign Keys**: Foreign keys were **NOT** introduced in this loop; foreign key constraints are strictly isolated to Loop P4-L2.

### 12.6 Quality Gate Status
- `.\gradlew.bat testDebugUnitTest --tests "com.example.data.repository.LifeRepositoryTransactionUnitTest"`: **PASSED** (4/4 tests, 0 failures, 0 errors).
- `.\gradlew.bat testDebugUnitTest`: **BUILD SUCCESSFUL** (Full repository unit test suite passing).
- `.\gradlew.bat lintDebug`: **BUILD SUCCESSFUL** (0 errors).
- `.\gradlew.bat assembleDebug`: **BUILD SUCCESSFUL** (Clean debug APK artifact).

---

## 13. Phase P4-L2: Foreign-Key Enforcement and Orphan-Data Migration (I1 / DI-01)

### 13.1 Preflight & Database Version Approval
- **Preceding Loop Verification**: P0, P1, P2-L1, P2-L2, P2-L3, P3-L1, P3-L2, and P4-L1 verified present, tested, committed, and synchronized.
- **Release 3 Tag Baseline**: Tag `v3.0.0` confirmed pointing to commit `2233a958a23194e5461bea4c76d4b69b99ad3147` (unchanged).
- **Approved Target Database Version**: **Version 19** (`MIGRATION_18_19`) explicitly authorized by product owner.
- **Migration History Continuity**: Migrations `1_2` through `17_18` verified contiguous and conflict-free. `MIGRATION_18_19` sequentially advances Room database version from 18 to 19.

### 13.2 Relationship Matrix

| Parent Table & Key | Child Table | Foreign Key Column | `ON DELETE` Action | Orphan Risk Scenario | Backup/Restore Impact |
|---|---|---|---|---|---|
| `tasks(id)` | `task_label_cross_ref` | `taskId` | `CASCADE` | Direct or non-transactional task deletion leaves lingering label refs. | Validated in v2 backup (FK ordered). |
| `labels(id)` | `task_label_cross_ref` | `labelId` | `CASCADE` | Direct or legacy label deletion leaves invalid task associations. | Validated in v2 backup (FK ordered). |
| `tasks(id)` | `task_reminders` | `taskId` | `CASCADE` | Task deletion without reminder deletion triggers orphaned alarm triggers. | Validated in v2 backup (FK ordered). |
| `medications(id)` | `medication_doses` | `medicationId` | `CASCADE` | Deleting medication record leaves unlinked dosage records in SQLite. | Validated in v2 backup (FK ordered). |
| `tracker_cycles(id)` | `tracker_usage_entries` | `cycleId` | `CASCADE` | Cycle deletion leaves lingering usage logs without parent context. | Validated in v2 backup (FK ordered). |
| `subscriptions(id)` | `subscription_bills` | `subscriptionId` | `CASCADE` | Subscription deletion leaves unpaid/paid occurrence records alive. | Validated in v2 backup (FK ordered). |
| `subscription_bills(id)` | `subscription_payments` | `billId` | `CASCADE` | Bill deletion leaves lingering payment transactions with null parent. | Validated in v2 backup (FK ordered). |
| `subscriptions(id)` | `subscription_payments` | `subscriptionId` | `CASCADE` | Subscription deletion leaves payment entries with orphan subscriptionId. | Validated in v2 backup (FK ordered). |

### 13.3 Orphan-Data Quarantine & Preservation Strategy
In accordance with zero-silent-data-loss safeguards:
1. **Quarantine Tables**:
   - `_orphans_backup_task_label_cross_ref` (`taskId`, `labelId`, `quarantineReason`, `quarantinedAt`)
   - `_orphans_backup_task_reminders` (`id`, `taskId`, `minutesBefore`, `customTime`, `type`, `referenceType`, `isEnabled`, `createdAt`, `quarantineReason`, `quarantinedAt`)
   - `_orphans_backup_medication_doses` (`id`, `medicationId`, `taskId`, `medicationName`, `medicationForm`, `dosage`, `instructions`, `scheduledDateIso`, `scheduledTime`, `status`, `actionTimestamp`, `snoozeCount`, `followUpCount`, `notes`, `quarantineReason`, `quarantinedAt`)
   - `_orphans_backup_tracker_usage_entries` (`id`, `cycleId`, `dateIso`, `odometerReading`, `quantityDeducted`, `manualRemainingQuantity`, `notes`, `timestamp`, `quarantineReason`, `quarantinedAt`)
   - `_orphans_backup_subscription_bills` (`id`, `subscriptionId`, `dueDateIso`, `expectedAmount`, `currency`, `status`, `notes`, `createdAt`, `quarantineReason`, `quarantinedAt`)
   - `_orphans_backup_subscription_payments` (`id`, `billId`, `subscriptionId`, `amountPaid`, `paymentDateIso`, `paymentMethod`, `transactionRef`, `notes`, `timestamp`, `quarantineReason`, `quarantinedAt`)
2. **Quarantine Reasons Recorded**:
   - `PARENT_TASK_MISSING`, `PARENT_LABEL_MISSING`, `PARENT_TASK_AND_LABEL_MISSING`
   - `PARENT_MEDICATION_MISSING`
   - `PARENT_CYCLE_MISSING`
   - `PARENT_SUBSCRIPTION_MISSING`
   - `PARENT_BILL_MISSING`, `PARENT_SUB_MISSING`, `PARENT_BILL_AND_SUB_MISSING`
3. **Data Recovery & Inspection**:
   - Quarantined records remain accessible in SQLite for forensic analysis or manual reassignment without polluting active application flows or violating foreign key constraints.

### 13.4 Migration Execution (`MIGRATION_18_19`)
1. Disables foreign keys temporarily during table recreation (`PRAGMA foreign_keys = OFF`).
2. Extracts and copies all orphan records matching `WHERE child_key NOT IN (SELECT id FROM parent)` into the respective `_orphans_backup_*` table.
3. Recreates each child table using Room-conforming schema with `FOREIGN KEY ... ON UPDATE NO ACTION ON DELETE CASCADE`.
4. Copies only valid, constrained rows from the old table to the new table.
5. Drops old table and renames `_new_*` to canonical table name.
6. Explicitly recreates all primary keys, indexes, and unique constraints.
7. Re-enables foreign key constraints (`PRAGMA foreign_keys = ON`).
8. Executes `PRAGMA foreign_key_check` and verifies 0 violations; throws `IllegalStateException` on any violation to trigger full transaction rollback.

### 13.5 Performance Benchmark & Verification
- **Test Suite**: [`RoomMigration18To19UnitTest.kt`](file:///c:/Users/thewi/Projects/Software/Open%20soursed/LifeOS/app/src/test/java/com/example/data/local/RoomMigration18To19UnitTest.kt)
- **Functional Verification**: Verified valid record survival, complete orphan quarantine, foreign-key constraint rejection on invalid inserts, and cascade deletion across all 6 child tables (`tests="2" failures="0" errors="0"`).
- **Large-Fixture Performance**: Evaluated on 50,000+ rows in SQLite; completed in **4,029ms** (target: < 5,000ms).
- **Full Sequential Upgrade**: [`RoomMigrationRobolectricUnitTest.kt`](file:///c:/Users/thewi/Projects/Software/Open%20soursed/LifeOS/app/src/test/java/com/example/data/local/RoomMigrationRobolectricUnitTest.kt) verified end-to-end migration from v1 (Genesis) to v19 with `PRAGMA integrity_check = ok` and `PRAGMA foreign_key_check = 0 violations`.
- **Backup & Restore v2**: [`Release4BackupRestoreV2UnitTest.kt`](file:///c:/Users/thewi/Projects/Software/Open%20soursed/LifeOS/app/src/test/java/com/example/data/backup/Release4BackupRestoreV2UnitTest.kt) verified all 40 entities round-trip with `databaseVersion = 19`.

### 13.6 Quality Gate Status
- `.\gradlew.bat testDebugUnitTest --tests "com.example.data.local.RoomMigration18To19UnitTest"`: **PASSED** (2/2 tests, 0 failures).
- `.\gradlew.bat testDebugUnitTest --tests "com.example.data.local.RoomMigrationRobolectricUnitTest" --tests "com.example.data.backup.Release4BackupRestoreV2UnitTest"`: **PASSED** (12/12 tests, 0 failures).
- `.\gradlew.bat testDebugUnitTest`: **BUILD SUCCESSFUL** (Full unit test suite passing).
- `.\gradlew.bat lintDebug`: **BUILD SUCCESSFUL** (0 errors).
- `.\gradlew.bat assembleDebug`: **BUILD SUCCESSFUL** (Clean debug APK artifact).

---

## 14. Phase P5-L1: Premium Subscription Foundation (MON-01 — Revised Owner Decision)

### 14.1 Owner-Approved Commercial Model & Boundaries
In accordance with explicit owner commercial decisions (Prompt P5-L1 Section 1):
- **Business Model**: Free + optional Premium subscription.
- **Billing Provider**: Google Play Billing (`com.android.billingclient:billing-ktx:7.1.1`).
- **Subscription Cadence**: **Monthly only** (`P1M`).
- **Free Trial**: **7 days** introductory trial.
- **Explicitly Rejected Models**: Annual subscription (Not approved), Lifetime purchase (Not approved), One-time purchase (Not approved).
- **Pricing & Currency Sovereignty**: Pricing and currency are owned entirely by the Google Play Console product catalog. Zero hardcoded prices or currency symbols exist in the application code.
- **Core Free Experience**: All core productivity features—tasks, habits, challenges, calendar, reminders, medications, local backups/restore, and gamification XP earning—remain 100% free and functional.

### 14.2 Architecture & Component Separation
The subscription architecture strictly separates billing communication, purchase persistence, entitlement evaluation, and UI feature gating:
1. **Billing Domain Models** ([`BillingModels.kt`](file:///c:/Users/thewi/Projects/Software/Open%20soursed/LifeOS/app/src/main/java/com/example/billing/BillingModels.kt)):
   - `SubscriptionProduct`: Represents the monthly product details queried dynamically from Google Play (product ID, base plan ID, offer ID, formatted regional price, currency, micros, billing period `P1M`, and 7-day trial info).
   - `SubscriptionPurchase`: Encapsulates Google Play purchase tokens, order IDs, purchase state, auto-renewing status, and expiry timestamps.
   - `PurchaseState`: `UNSPECIFIED`, `PURCHASED`, `PENDING`.
   - `BillingResultStatus`: Clean domain mapping for BillingClient response codes.
   - `BillingConnectionState`: `DISCONNECTED`, `CONNECTING`, `CONNECTED`, `CLOSED`.
2. **Entitlement State Machine** ([`PremiumEntitlementState.kt`](file:///c:/Users/thewi/Projects/Software/Open%20soursed/LifeOS/app/src/main/java/com/example/billing/PremiumEntitlementState.kt)):
   - Expresses all subscription lifecycle states: `Free`, `TrialActive`, `PremiumActive`, `PendingPurchase`, `BillingUnavailable`, `Expired`, `CancelledUntilExpiry`, `GracePeriod`, `AccountHold`, and `Unknown`.
   - Single authoritative derived property: `val isEntitled: Boolean`. Feature gates query only this state rather than querying BillingClient or raw purchase tokens directly.
3. **Purchase Verification Contract** ([`PurchaseVerifier.kt`](file:///c:/Users/thewi/Projects/Software/Open%20soursed/LifeOS/app/src/main/java/com/example/billing/PurchaseVerifier.kt)):
   - Defines `PurchaseVerifier` interface and `ClientSidePurchaseVerifier`.
   - Flags client-side purchase verification boundaries: marks untrusted purchases in release builds as `UNVERIFIED_CLIENT_ONLY` / `FAILED_VERIFICATION`, isolating client code from server verification assumptions.
4. **Billing Client Operations** ([`BillingManager.kt`](file:///c:/Users/thewi/Projects/Software/Open%20soursed/LifeOS/app/src/main/java/com/example/billing/BillingManager.kt), [`PlayBillingManager.kt`](file:///c:/Users/thewi/Projects/Software/Open%20soursed/LifeOS/app/src/main/java/com/example/billing/PlayBillingManager.kt)):
   - Wraps Google Play BillingClient 7.x lifecycle, asynchronous connection management, automatic retries with exponential backoff, product details querying (`QueryProductDetailsParams`), purchase flow launching (`launchBillingFlow`), purchase acknowledgment (`acknowledgePurchase`), and queryPurchasesAsync token caching.
5. **Entitlement Repository** ([`PremiumEntitlement.kt`](file:///c:/Users/thewi/Projects/Software/Open%20soursed/LifeOS/app/src/main/java/com/example/ui/theme/engine/PremiumEntitlement.kt)):
   - `LocalPremiumEntitlementRepository` combines Google Play purchase streams with debug verification to expose `entitlementState: Flow<PremiumEntitlementState>` and `isPremiumActive: Flow<Boolean>`.
   - Legacy simulated toggle in Jetpack DataStore is strictly gated by `BuildConfig.DEBUG` and completely ignored in release builds.
6. **ViewModel Exposure** ([`LifeViewModel.kt`](file:///c:/Users/thewi/Projects/Software/Open%20soursed/LifeOS/app/src/main/java/com/example/ui/viewmodel/LifeViewModel.kt)):
   - Exposes `entitlementState: StateFlow<PremiumEntitlementState>` and `subscriptionProduct: StateFlow<SubscriptionProduct?>`.
   - Dispatches `launchSubscriptionPurchase(activity)` and `restoreSubscriptionPurchases()`.
   - Enforces `if (!BuildConfig.DEBUG) return` on `toggleSimulatedPremium`.

### 14.3 Removal of Production Developer Simulator & UI Hardening
- **Theme Gallery Screen** ([`ThemeGalleryScreen.kt`](file:///c:/Users/thewi/Projects/Software/Open%20soursed/LifeOS/app/src/main/java/com/example/ui/screens/themes/ThemeGalleryScreen.kt)):
  - The "Developer Entitlement Simulator" toggle card is enclosed in `if (com.example.BuildConfig.DEBUG) { ... }`, ensuring it is never rendered in release builds.
  - The Theme Unlock modal displays "Enable & Apply (Dev Mode)" only in debug builds; in release builds, it presents "Subscribe (7-Day Trial)" launching Google Play Billing flow.
- **Gamification Screen** ([`GamificationScreen.kt`](file:///c:/Users/thewi/Projects/Software/Open%20soursed/LifeOS/app/src/main/java/com/example/ui/screens/gamification/GamificationScreen.kt)):
  - Removed misleading banner claiming core XP, streaks, and Hunter Ranks were locked.
  - Restored XP and coin awards in `LifeRepository.awardXpAndCoins` for all users without entitlement gating.
  - Subscription banner describes optional Premium perks (custom themes, bonus cosmetic rewards) with a 7-day free trial; unlock button launches `launchSubscriptionPurchase(activity)` in release builds.

### 14.4 Google Play Console Configuration Specification
To deploy the subscription offer to production, configure the following in Google Play Console:
1. **Product ID**: `lifeos_premium_monthly`
2. **Base Plan ID**: `monthly-plan`
   - Type: Auto-renewing
   - Billing period: 1 Month (`P1M`)
   - Renewal type: Automatic renewal
3. **Offer ID**: `7-day-free-trial`
   - Eligibility: New customer acquisition (once per user account)
   - Phase 1: Free trial, duration: 7 days (`P7D`)
   - Phase 2: Recurring monthly price at regional Play Console rates
4. **Grace Period**: 7 days enabled.
5. **Account Hold**: Enabled (up to 30 days while user resolves declined payment methods).

### 14.5 Architecture Boundary & Remote Verification Decision
- **Current Offline-First Baseline**: LifeOS does not possess a backend server, database, or identity provider.
- **Security Boundary**: In client-only environments, Google Play Billing purchase tokens returned to the Android client cannot be cryptographically verified against Google's private signing servers without a server-side component holding Google Cloud credentials.
- **Owner Architecture Recommendation**:
  - Phase 1 (Current): Client-side integration using Google Play Billing Library 7.x with local purchase caching, token acknowledgment, and `BuildConfig.DEBUG`-gated simulator removal.
  - Phase 2 (Optional Cloud Verification): Deploy a lightweight Firebase Cloud Function or Google Cloud Run service with a Service Account authorized for the Google Play Android Developer API (`purchases.subscriptionsv2.get`), coupled with Google Cloud Pub/Sub Real-time Developer Notifications (RTDN) for instantaneous subscription lifecycle updates.

### 14.6 Quality Gate Status
- `.\gradlew.bat testDebugUnitTest --tests "com.example.billing.PremiumEntitlementUnitTest"`: **PASSED** (14/14 tests, 0 failures, 0 errors).
- `.\gradlew.bat testDebugUnitTest`: **BUILD SUCCESSFUL** (Full unit test suite passing).
- `.\gradlew.bat lintDebug`: **BUILD SUCCESSFUL** (0 errors).
- `.\gradlew.bat assembleDebug`: **BUILD SUCCESSFUL** (Clean debug APK artifact).









