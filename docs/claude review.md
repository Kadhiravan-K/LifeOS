# LifeOS — Release 4 Research, Audit & Master Plan

| | |
|---|---|
| **Baseline audited** | `main` @ `1d4f43f` = tag `v1.1.0` (Release 2). Verified locally: `git rev-parse v1.1.0^{commit}` equals `HEAD`; `versionCode 2`, `versionName 1.1.0` (`app/build.gradle.kts`). |
| **Audit date** | 2026-11-01 (source access dates in §27) |
| **Scope** | Research and documentation only. No source, schema, tag or release was changed. |
| **Release boundaries** | Release 2 immutable. **Release 3 reserved for the owner's own plans, nothing assigned here.** Release 4 is the main target. |

---

## 0. Read this first: what this audit did and did not do

**Done (verified):** cloned the repository, read the manifest, Gradle files, Room database and migrations, all entity/DAO packages, the backup manager, the notification receiver and helper, widgets, `RecurrenceEngine`, `DateUtils`, the premium engine, the navigation shell, CI workflow, lint baseline and test inventory. Every finding below cites a path and symbol. Line numbers are those at `1d4f43f`.

**Not done (gaps, stated so nothing is silently assumed):**

| Gap | Effect on this document |
|---|---|
| Gradle build and unit tests were **not run** (no Android SDK in the sandbox). | No finding claims a test fails. "Repro" columns are proposed tests, not executed ones. |
| No on-device or emulator testing. | Section 5 (UX) is a **code-based** review of screens, flows and components, not a usability session. UX items are marked *suspected* unless the code proves them. |
| Competitor research covers **Todoist, TickTick, Loop Habit Tracker, Tasks.org, Habitica** only. | Microsoft To Do, Google Tasks, Notion, Any.do, Structured are **not researched** here (Any.do appears only incidentally). They are listed as open research in §25. |
| Competitor prices from aggregator sites **conflict** (e.g. Todoist Pro quoted as $4, $5 and $7 per month). | No Todoist price is asserted. Only prices seen on a store listing are recorded. |
| `git push` / PR creation not possible from this environment (no credentials for the repo). | The file is delivered for you to commit. Commands are in §26. |
| Several large files were sampled, not read line by line (`LifeRepository.kt` 2,731 lines, `LifeViewModel.kt` 2,647, `SettingsScreen.kt` 2,059, `TemplateCatalog.kt` 1,659). | Findings in them come from targeted greps. Anything not listed there is **unreviewed**, not clean. |

Confidence tags used throughout: **[C]** confirmed by reading the code, **[S]** suspected (code pattern is real, runtime effect not demonstrated), **[P]** product/policy decision, not a code bug.

---

## 1. Executive Summary

LifeOS is a large (≈54k lines of Kotlin), offline-first Android app: tasks with a custom recurrence engine, habits, a 75-Day Challenge, Quick Reminders "honeycomb" tiles, three home-screen widgets, medication reminders, smart trackers, a subscriptions manager, gamification and a theme engine. The manifest declares **no `INTERNET` permission**, which is a verified and genuinely strong privacy property that very few competitors can claim.

The most important finding is not a crash. **The backup feature silently omits 16 of the 28 Room tables**, including the 75-Day Challenge, water logs, medications and dose history, task reminders, label links, plans, XP events, achievements and rewards. The README says challenge progress is backed up. A user who "restores" after a phone change will lose that data and will not be warned.

**Top themes**

1. **Data safety (P0/P1):** incomplete and non-atomic backup/import, no foreign keys, multi-step writes outside transactions, backup that is clipboard/paste based, and Android Auto Backup left on with empty rules (a quiet conflict with the privacy promise).
2. **Reminder reliability (P1):** broadcast receiver launches coroutines without `goAsync()`, no handler for time/timezone change, no exact-alarm permission flow, rescheduling after reboot covers only today.
3. **Correctness (P1):** `EVERY_N_WEEKS` / custom-weeks recurrence fires a week early in some configurations; `DateUtils` shares non-thread-safe `SimpleDateFormat` instances across threads.
4. **Security (P1/P2):** an exported receiver whose "reject foreign package" check passes when the package is `null`; the widget click action is also reachable from other apps.
5. **Product coherence (P1, decision):** a production "Unlock Premium" button grants premium themes for free, and the UI and code comments disagree on what premium means.
6. **Architecture debt (P2):** two ~2.7k-line god classes, no DI, no localization (4-line `strings.xml`), 138 of 316 `contentDescription`s are `null`, CI runs no lint and no instrumented tests.

**Recommendation:** Release 4 should be a **reliability-and-trust release**. Fix backup, atomicity, reminders and date handling first, then split the monoliths behind tests, and add a *small* set of high-value features (SAF backup with file import, saved filters, per-habit insights). Everything cloud-flavoured stays out.

**Counts:** 15 confirmed defects, 9 suspected risks, 7 refactor items, 9 removal/deprecation candidates, 14 feature proposals. Detail in §8, §14–§16, §20.

---

## 2. Research Scope and Methodology

| Activity | Method | Evidence standard |
|---|---|---|
| Repository inventory | `git clone`, `find`, `wc -l`, targeted `grep`, direct file reads | Path + symbol + line number |
| Bug claims | Read the code path end to end. "Confirmed" only when the faulty logic is visible in the source. | Else tagged [S] |
| Competitor research | Web search; prefer official or primary pages (project README/site, store listing). Aggregator and review-blog claims are labelled as such. | URL + access date in §27; conflicts recorded, not resolved by guessing |
| Prioritisation | Severity × user impact × frequency × effort × risk × confidence (§20) | Scores are judgement; the inputs are listed |

No competitor popularity, market-share or download numbers are used anywhere, because none were found from a reliable primary source.

---

## 3. Current LifeOS Architecture

### 3.1 Stack (verified: `app/build.gradle.kts`, `gradle/libs.versions.toml`)

| Area | Fact |
|---|---|
| SDK | `minSdk 24`, `targetSdk 36`, `compileSdk 36`, core-library desugaring enabled (java.time is available) |
| UI | Jetpack Compose, Material 3, BOM `2024.09.00` (old relative to SDK 36) |
| Persistence | Room `2.7.0`, DB `lifeos_database.db`, **version 10**, `exportSchema = false`, 9 hand-written migrations; DataStore preferences |
| Background | `AlarmManager` (exact when permitted), one `PeriodicWork` (`MaintenanceWorker`, 24h), `BroadcastReceiver`s |
| Concurrency | Coroutines; `CoroutineScope(Dispatchers.IO).launch` in 12 places outside lifecycle owners |
| DI | None. `LifeRepository(...)` is hand-constructed in `MainActivity`, `ReminderBroadcastReceiver`, `MaintenanceWorker`, `QuickRemindersWidgetProvider` |
| Navigation | **No navigation library.** `MainScreen.kt:49` keeps `currentItem` in `rememberSaveable`; back handling is ad-hoc `BackHandler`s. `navigationCompose` is in the version catalog but not in `dependencies {}` |
| Release build | `isMinifyEnabled = false` |
| Tests | 17 files, ≈184 `@Test` methods; 1 Robolectric migration test; 1 instrumented migration test |
| CI | `.github/workflows/android-ci.yml`: `testDebugUnitTest` + `assembleDebug` only |

### 3.2 Package map

```
com.example   (namespace; applicationId is com.aistudio.lifeos.vtrqkx)
├─ MainActivity.kt            DB + repo + VM wiring, channels, worker scheduling
├─ data/local/                AppDatabase (+migrations), entities/ (6 files, 28 tables), dao/ (8 files)
├─ data/repository/           LifeRepository.kt   2,731 lines, 148 funs
├─ data/backup/               DataExportImportManager.kt (JSON, 650 lines)
├─ data/preferences/          UserPreferences.kt  (DataStore, 604 lines)
├─ domain/                    model/, recurrence/RecurrenceEngine, medication/, tracker/, subscription/, templates/ (TemplateCatalog 1,659 lines), quickreminders
├─ notifications/             NotificationHelper (739), ReminderBroadcastReceiver, MaintenanceWorker
├─ widgets/                   QuickReminders (provider + RemoteViewsService), Tracker, Subscription, WidgetRefreshCoordinator
├─ ui/viewmodel/              LifeViewModel.kt   2,647 lines, 179 funs, 117 StateFlow references
├─ ui/screens/                dashboard, tasks, calendar, habits, challenge, planning, projects, labels, medication, essentials(trackers/subs), gamification, analytics, settings, templates, themes, manual, legal
├─ ui/components/             21 reusable files (largest: TaskAdvancedEditorSheet 1,373; HoneycombModals 1,274; CommonComponents 1,200)
└─ ui/theme/engine/           theme registry, tokens, premium entitlement
```

### 3.3 Largest files (maintainability hotspots)

| Lines | File |
|---|---|
| 2,731 | `data/repository/LifeRepository.kt` |
| 2,647 | `ui/viewmodel/LifeViewModel.kt` |
| 2,059 | `ui/screens/settings/SettingsScreen.kt` |
| 1,659 | `domain/templates/TemplateCatalog.kt` (data embedded in code) |
| 1,499 | `ui/screens/manual/ComponentDetailSheet.kt` (in-app component manual) |
| 1,373 | `ui/components/TaskAdvancedEditorSheet.kt` |

### 3.4 Verified strengths (keep these)

- No `INTERNET` permission in `AndroidManifest.xml`. PRIVACY.md's "no network" claim is **true**.
- Notification/PendingIntent flags use `FLAG_IMMUTABLE` everywhere except the widget click template (which legitimately needs mutable, see SEC-02).
- Lint is a hard gate (`abortOnError = true`) with a baseline; desugaring is configured.
- Room migrations 1→10 exist with no `fallbackToDestructiveMigration`.
- Widget providers for trackers/subscriptions already use `goAsync()` (`TrackerWidgetProvider.kt:40`, `SubscriptionWidgetProvider.kt:40`). The pattern exists; it just was not applied to the reminder receiver.

---

## 4. Complete Feature Inventory (verified from entities, packages and manifest)

| Area | Implementation evidence | Notes |
|---|---|---|
| Tasks, subtasks, priorities P1–P3 | `TaskEntity`, `subtasksJson`; `TasksScreen`, `TaskAdvancedEditorSheet`, `TaskQuickAddSheet` | Subtasks stored as JSON text inside the task row |
| Recurrence | `domain/recurrence/RecurrenceEngine.kt`; types NONE, DAILY, EVERY_N_DAYS, WEEKDAYS, SELECTED_WEEKDAYS, WEEKLY, EVERY_N_WEEKS, MONTHLY, EVERY_N_MONTHS, EVERY_6_MONTHS, YEARLY, EVERY_N_YEARS, CUSTOM | Skip/reschedule/completed dates kept as JSON per task. See REC-01 |
| Calendar | `CalendarScreen.kt` (1,003 lines) | |
| Projects, labels | `ProjectEntity`, `LabelEntity`, `TaskLabelCrossRef`; `ProjectsLabelsManagerScreen` | |
| Task reminders | `TaskReminderEntity`; `NotificationHelper`; snooze/complete/dismiss actions | |
| Habits | `HabitEntity`; `habits/` screen | |
| 75-Day Challenge | `ChallengeEntity`; daily logs as JSON; `ChallengeScreen` | **Not in backup (BK-01)** |
| Quick Reminders honeycomb | `HoneycombShortcutEntity`; `HoneycombTaskGrid`, `HoneycombModals` | Also rendered in a widget |
| Widgets | Quick Reminders (RemoteViews list), Smart Trackers, Subscriptions | All `exported="true"` providers |
| Hydration | `WaterLogEntity` | **Not in backup** |
| Medication reminders | `MedicationEntity`, `MedicationDoseEntity` (unique index on medication+date+time); `MedicineReminderSection`, `MedicationScreen` | **Not in backup**; health-adjacent data |
| Smart Trackers | `TrackerEntity`, cycles, entries, purchases, inventory adjustments | Entries and adjustments **not in backup** |
| Subscriptions manager | `SubscriptionEntity`, bills, payments, price history | Price history **not in backup** |
| Plans / milestones | `PlanEntity`, `PlanMilestoneEntity`, `PlanTaskCrossRef` | **Not in backup** |
| Trash / restore | `TrashItemEntity`, `TrashDao` | But `deleteTask` is a hard delete (see DI-01) |
| Gamification | profile, XP events, achievements, rewards, redemptions; `GamificationScreen` | Only the profile row is exported |
| Analytics | `AnalyticsScreen` (537 lines) | |
| Templates and bundles | `TemplateCatalog.kt`, `TemplateBrowserDialog` | Catalog compiled into the app |
| Themes | `ui/theme/engine/*`, `ThemeGalleryScreen` | Premium-gated themes (MON-01) |
| Legal consent | `LegalConsentScreen` | Not reviewed in depth |
| In-app manual | `ui/screens/manual/*` (≈3.3k lines) | Large; see RM-04 in §15 |
| Settings hub | `SettingsScreen.kt` (2,059 lines) | |
| Backup | `DataExportImportManager`, `BackupRestoreDialog` | JSON, clipboard / share / paste |

---

## 5. Product UX Review (code-based, no on-device session)

Severity: **High** = causes data loss or blocks a core job, **Med** = friction or inconsistency users will notice, **Low** = polish.

| # | Area (from the 20-item brief) | Observation (evidence) | Sev. | Tag |
|---|---|---|---|---|
| U1 | Backup and restore (#15) | Export is "Copy" to clipboard or "Share" as plain text; import is a "Paste JSON backup here…" text box (`BackupRestoreDialog.kt:159-201`). A real backup file is tens or hundreds of KB. Pasting that into a `TextField` is slow and fragile; the importer's cap is 50 million characters. No file picker, no file name, no confirmation of what will be restored. | High | [C] |
| U2 | Backup trust (#15) | Because of BK-01 the user is told "Backup Ready (N bytes)" for a file that lacks 16 data types. | High | [C] |
| U3 | Settings organisation (#14) | One 2,059-line screen. Large single files usually mean long scrolling lists with weak grouping. Needs a screenshot review to confirm. | Med | [S] |
| U4 | Accessibility (#17) | 138 of 316 `contentDescription` occurrences are `null`. Some are decorative (acceptable) but interactive icons with `null` are unreadable to TalkBack. `SmallSp` lint warnings: 6 in baseline. | Med | [C] counts, [S] impact |
| U5 | Localisation (#17,#20) | `res/values/strings.xml` is 4 lines. All UI text is hard-coded; 30 `HardcodedText` entries sit in `lint-baseline.xml`. The app cannot be translated, and `supportsRtl="true"` is declared with no RTL review. | Med | [C] |
| U6 | Notifications (#13) | No exact-alarm permission request UI exists (see REM-02), so P1 "alarm" reminders can silently become inexact on Android 14+. | High | [C] flow absent, [S] device effect |
| U7 | Gamification (#12) | Premium copy on the Gamification screen says Hunter Ranks, XP, streaks and the Reward Shop are "premium perks"; `PremiumEntitlement.kt` says premium is "strictly cosmetic". Users see one story, the code another. | Med | [C] |
| U8 | Widgets (#9) | QR widget list loads **all** tasks, habits and projects on each refresh (`QuickRemindersRemoteViewsService.onDataSetChanged`). Tapping a tile matches tasks by fuzzy title (`WID-01`), so a tile named "Run" can toggle "Run payroll". | Med | [C] |
| U9 | Search and navigation (#16) | State-based navigation with `BackHandler`s, no deep links, no restoration beyond the current tab. Global search not located in the code (not verified absent; not found in packages listed). | Med | [S] |
| U10 | Performance and battery (#18) | See PERF-01/02. Recurrence counting is O(days) per task per render for "ends after N" tasks. | Med | [S] |
| U11 | Offline reliability (#19) | Excellent by construction (no network). The risk is data, not connectivity. | n/a | [C] |
| U12 | Onboarding / empty states (#1) | Not assessable from code alone. **Open item** for a device pass. | ? | gap |
| U13 | Visual consistency (#20) | A reusable component library (21 files) and a theme engine exist, a good sign. Not verified visually. | ? | gap |

Items 2, 3, 4, 5, 6, 7, 8, 10, 11, 12 and 16 of the brief (home dashboard, tasks, recurring, projects/labels, habits, challenge, hydration, templates, XP, search) were **not** assessed beyond the table because they need running the app. I would rather leave them blank than invent friction.

---

## 6. Market and Competitor Analysis

Access date for all sources below: **2026-11-01**. Aggregator/review sites are marked *(agg.)* and treated as weaker than a product's own site or store listing.

### 6.1 Loop Habit Tracker: closest philosophical peer

Primary source (loophabits.org and the project README). Claims: completely free with no in-app purchases; ad-free; open source (GPLv3); works offline with no account; flexible schedules such as "3 times per week" or "every other day"; per-habit reminders where you can check, dismiss or snooze from the notification; home-screen widgets; **export to CSV or SQLite**; checkmarks can be added through task-automation apps such as Tasker; a "habit score" that weakens gradually, so that "a few missed days after a long streak" do not erase progress.

*Lesson for LifeOS:* portable export (CSV/SQLite), forgiving streak math, and notification actions are table stakes in the offline niche. LifeOS has notification actions; it lacks portable export, and its habit model stores `currentStreak`/`longestStreak` (`domain/model/HabitModels.kt:12-13`); a gradual score-based model was not found in the files searched.

### 6.2 Tasks.org: closest offline-first, privacy-first task app

Sources: F-Droid/IzzyOnDroid listings and the APKMirror store text. Claims: open source (GPL-3.0), "no advertisements, no ad trackers, no location tracking", optional sync via Google Tasks, CalDAV or EteSync, supports end-to-end encryption and file-based sync, backups to internal storage, Google Drive or Android backup, widgets and themes. Funding is an optional subscription (store text quotes "starting at $0.99 USD per year"; this text is from an older listing, so **treat the price as unverified**). The F-Droid page lists location permissions, which fits its location reminders.

*Lesson:* optional, user-controlled sync (file or CalDAV) is the accepted way to add multi-device without a vendor cloud. It also shows that a donation/low-price model can coexist with a privacy stance.

### 6.3 TickTick: breadth leader (task + calendar + habits + Pomodoro)

Sources: App Store listing (monthly Premium $3.99, annual $35.99 are shown there), plus *(agg.)* reviews. Built-in calendar view, habit tracker, Pomodoro timer, Eisenhower matrix and kanban are repeatedly cited. **What is free vs paid conflicts across sources** (one says habit tracking is free, another says it needs Premium; one says calendar view is premium-only). I therefore do not assert any free/paid boundary.

*Lesson:* users value having tasks, calendar and habits in one place, which LifeOS already does. TickTick's weak spot, per one source, is the free calendar not syncing externally; LifeOS has no external calendar and should avoid promising it.

### 6.4 Todoist: task-capture and polish benchmark

Sources are aggregators *(agg.)*. Free/Pro/Business tiers exist. Quoted prices **disagree** ($4, $5, $7 per month for Pro across pages), so none is recorded. Claims that a *(agg.)* page makes: free tier limits personal projects and filter views; calendar layout, task duration and custom reminders are paid; no built-in habit tracker or Pomodoro; an AI assistant was added. Natural-language quick add is the feature most often credited.

*Lesson:* natural-language date capture and saved filters are the most copied Todoist ideas. Both can be done fully on-device with no AI.

### 6.5 Habitica: gamification benchmark

Sources: AlternativeTo, review blogs *(agg.)*. Free with an optional subscription (blog quotes $4.99/month, $47.99/year; **verify**). Praised by users who like game mechanics (including some ADHD users); criticised for RPG overhead, a daunting UI, lack of offline support, excessive notifications, and for subscriptions that unlock cosmetics. A 2026 habit-app roundup says the top complaints across the category include **streaks vanishing after reinstall, restore or sync error**, and paywalls on useful analytics.

*Lesson:* the "streak lost after restore" complaint is exactly what LifeOS's incomplete backup (BK-01) will cause. Fixing BK-01 is a competitive point, not just hygiene.

### 6.6 Adopt / adapt / differentiate / avoid

| Idea | Decision | Why |
|---|---|---|
| Portable export (CSV/ICS/JSON) + file-based import | **Adopt** | Loop and Tasks.org set the bar; fits offline identity |
| Forgiving habit strength score alongside streaks | **Adapt** | Reduces "streak shame" Habitica users report |
| Saved filters / smart lists | **Adopt** | Cheap, on-device, high value |
| Natural-language quick add (local parser) | **Adapt** | Local rule-based only; no AI dependency |
| Optional file/CalDAV sync | **Defer to R5+** | Large scope; keep optional |
| Built-in calendar sync with Google/Outlook | **Avoid** | Needs network and accounts |
| Subscription paywall for core features | **Avoid** | Against stated identity; Habitica and Way-of-Life-style gates draw complaints |
| RPG avatars/party features | **Avoid** | Habitica's niche; LifeOS's gamification should stay light |
| Cosmetic-only premium | **Decision needed** (MON-01) | Must be real or removed |
| AI features | **Not recommended by default** | Only as opt-in, on-device, and never required |

---

## 7. Competitor Feature Matrix

Legend: ✅ stated by a primary or store source · ◐ stated only by an aggregator or conflicting sources · ❌ source says absent · — not listed in the sources reviewed (absence not proven) · ? not verified · **L** = verified in LifeOS code.

| Capability | LifeOS | Loop | Tasks.org | TickTick | Todoist | Habitica |
|---|---|---|---|---|---|---|
| Works offline, no account | **L ✅** (no INTERNET perm) | ✅ | ✅ (sync optional) | ? | ? | ◐ (lack of offline cited) |
| Ads / trackers | **None (L)** | ✅ none | ✅ none | ? | ? | ✅ ad-free |
| Open source | ✅ MIT (`LICENSE`) | ✅ GPLv3 | ✅ GPL-3.0 | — | — | ◐ open source (agg.) |
| Task recurrence | **L ✅** (13 types) | habits only | ? | ✅ | ? | ? |
| Habit tracking | **L ✅** | ✅ core | — | ◐ | — (agg. says absent) | ✅ core |
| Gamification | **L ✅** XP, ranks, rewards | — | — | ◐ stats | ? | ✅ core |
| Home-screen widgets | **L ✅** (3) | ✅ | ✅ | ? | ? | ? |
| Calendar view | **L ✅** | — | ? | ◐ (free/paid conflicts) | ◐ paid (agg.) | — |
| Focus / Pomodoro | **L ❌** | — | — | ✅ | — (agg. says absent) | — |
| Notification actions (done/snooze) | **L ✅** | ✅ | ? | ? | ? | ? |
| Portable export (CSV/SQLite/file) | **L ◐ JSON, clipboard only, incomplete** | ✅ CSV + SQLite | ✅ backups | ? | ◐ backups paid (agg.) | ? |
| Optional sync | **L ❌** | — | ✅ CalDAV/Google/EteSync | ✅ cloud | ✅ cloud | ✅ cloud |
| Medication reminders | **L ✅** | — | — | ? | ? | — |
| Subscription/bill tracker | **L ✅** | — | — | — | — | — |
| Localisation | **L ❌** (4-line strings.xml) | ✅ translation project | ✅ 78 languages (older listing) | ? | ? | ? |
| Paid tier | **L: simulated "premium" themes (MON-01)** | — none | optional, low | ✅ | ✅ | optional |

**Reading the matrix:** LifeOS's real differentiator is breadth *plus* zero network (tasks, habits, meds, trackers, subscriptions, widgets, in one offline app). Its gaps against the niche leaders are **portable export, localisation and a forgiving habit model**, not features.

---

## 8. Confirmed Bugs and Suspected Risks

Severity: **P0** data loss or core promise broken · **P1** wrong behaviour or trust/security issue users can hit · **P2** quality and maintainability · **P3** polish.
Complexity: S ≤ 1 day, M 2–4 days, L 1–2 weeks, XL > 2 weeks (single developer, including tests).

### 8.1 Confirmed (visible in source)

#### BK-01 · P0 · Backup omits 16 of 28 tables
- **Where:** `data/backup/DataExportImportManager.kt` (`exportAllData`, lines 41–336; `importData`, 338–646); entity list in `AppDatabase.kt:558–591`.
- **Evidence:** `exportAllData` writes 12 keys: `tasks, habits, honeycomb_shortcuts, projects, labels, gamification_profile, trackers, tracker_cycles, tracker_purchases, subscriptions, subscription_bills, subscription_payments`. Missing: `challenges`, `water_logs`, `task_reminders`, `task_label_cross_ref`, `trash`, `plans`, `plan_milestones`, `plan_task_cross_ref`, `xp_events`, `achievements`, `rewards`, `reward_redemptions`, `medications`, `medication_doses`, `usage_entries`, `inventory_adjustments`, `price_history`. DataStore preferences are also not exported. `README.md:65-66` says the export covers "challenge progress".
- **Root cause:** exporter was written for an early schema and not extended as tables were added (versions 6→10).
- **Repro (proposed test):** seed every DAO, export, wipe DB, import, assert table-by-table row equality. Fails today for 16 tables.
- **Impact:** after a phone change or reinstall, 75-Day Challenge progress, medication schedule and history, hydration logs, reminders, XP and rewards are gone, while the dialog reports success.
- **Fix:** reflection-free explicit exporter for every table, `backupVersion` bump, a unit test that enumerates `AppDatabase` entities and fails when a new entity has no exporter (guardrail).
- **Risk:** medium (format change). Keep reading v1 files. **Complexity:** L. **Tests:** round-trip, v1→v2 import, entity-coverage guard.

#### BK-02 · P1 · Import is non-atomic, ignores version, overwrites by ID
- **Where:** `DataExportImportManager.importData` (338–646).
- **Evidence:** each row is inserted separately via DAOs with `OnConflictStrategy.REPLACE` (e.g. `LifeDaos.kt:23`). No `withTransaction`. `version` is written (line 43) but never read. `catch (e: Exception) { e.printStackTrace(); false }` (≈645) returns only a boolean.
- **Impact:** a malformed or truncated file leaves the DB half-imported with no rollback. Importing into a non-empty DB overwrites rows that share an ID, so tasks from two devices silently clobber each other. Cross-table references (labels↔tasks, bills↔subscriptions) are not validated.
- **Fix:** parse and validate fully first (schema/version check, referential checks), then apply in a single `database.withTransaction`, with an explicit "replace all" vs "merge with new IDs" choice and a typed result (counts, warnings).
- **Risk:** medium. **Complexity:** L. **Tests:** corrupted-file rollback, version mismatch, ID-collision merge.

#### BK-03 · P1 · Backup mechanism contradicts PRIVACY.md and does not scale
- **Where:** `ui/components/BackupRestoreDialog.kt:159–201`; `PRIVACY.md:28`.
- **Evidence:** PRIVACY.md says the backup is saved "via the Android Storage Access Framework (SAF)". The code copies JSON to the **clipboard**, shares it as `EXTRA_TEXT`, and imports from a pasted text box. No `CreateDocument`/`OpenDocument` usage exists.
- **Impact:** a privacy document that does not match behaviour; clipboard contents are readable by other apps on some Android versions, and the user's whole task/subscription history sits in the clipboard. Large payloads in `EXTRA_TEXT` can fail (see BK-04).
- **Fix:** SAF `ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT` with a file name like `lifeos-backup-YYYYMMDD.json`; remove clipboard export or hide it behind "advanced". Update PRIVACY.md to match.
- **Risk:** low. **Complexity:** M. **Tests:** instrumented SAF round-trip (fake provider), UI state tests.

#### PRV-01 · P1 · Android Auto Backup is on with empty rules
- **Where:** `AndroidManifest.xml:12` (`allowBackup="true"`), `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml` (both are unmodified template comments).
- **Evidence:** with no `<exclude>` rules, the Room database and DataStore files are eligible for Google's device backup and device-to-device transfer. PRIVACY.md promises "no cloud" and "no servers"; the *app* never uploads, but the OS may.
- **Impact:** medication, subscription and task data can leave the device through the platform backup, contradicting the app's headline promise. Auto Backup also restores a database whose alarms and widget state are not restored.
- **Fix:** a **product decision [P]**: either (a) `allowBackup="false"` plus working SAF backup, or (b) keep Auto Backup deliberately, with explicit include/exclude rules, and say so in PRIVACY.md. I recommend (a) for Release 4, because it matches the privacy identity.
- **Risk:** low. **Complexity:** S. **Tests:** manifest lint rule, `bmgr` manual check.

#### SEC-01 · P1 · "Reject foreign package" check passes when package is null
- **Where:** `notifications/ReminderBroadcastReceiver.kt:44–50`; `AndroidManifest.xml:34` (`exported="true"`).
- **Evidence:** `val pkg = intent.package ?: intent.component?.packageName; if (pkg != null && pkg != context.packageName) return`. An implicit broadcast from another app has `package == null`, so it is **not** rejected. The action strings (`ACTION_TASK_COMPLETE`, `ACTION_MEDICINE_TAKEN`, `ACTION_TASK_SNOOZE`, …) are public in this open-source manifest.
- **Impact:** any installed app can mark tasks complete, mark a medication dose as taken or skipped, or trigger snoozes by sending a broadcast with a task/dose ID. Local-only, low-effort, and for medication doses a safety-relevant tamper.
- **Fix:** set `exported="false"` for the receiver and register `BOOT_COMPLETED` in a **separate** exported receiver that does nothing but reschedule. All notification actions use explicit `PendingIntent`s to our own component, which work with `exported="false"`.
- **Risk:** low. **Complexity:** S. **Tests:** Robolectric: external implicit intent is ignored; own `PendingIntent` still works.

#### SEC-02 · P2 · Widget click action is reachable by other apps and can create tasks
- **Where:** `AndroidManifest.xml:52–60` (provider exported with `ACTION_WIDGET_TOGGLE_TASK` in its filter); `QuickRemindersWidgetProvider.kt:87` (`FLAG_MUTABLE`), `onReceive` → `getOrCreateTaskForShortcut(...)`.
- **Evidence:** `extra_type`, `extra_task_id`, `extra_target_id` come straight from the intent. If `taskId == -1` and `targetId` is not numeric, a **new task is created** from the attacker-controlled string.
- **Impact:** another app can toggle completion or create tasks. Low severity (no data exfiltration).
- **Fix:** remove the custom action from the exported provider's filter; route clicks through a non-exported receiver/`Service` with explicit component, validate IDs against the DB. `FLAG_MUTABLE` is acceptable for a RemoteViews template once the target is non-exported.
- **Risk:** low. **Complexity:** S–M.

#### WID-01 · P2 · Widget tile resolves tasks by fuzzy title
- **Where:** `widgets/QuickRemindersRemoteViewsService.kt:121–126`, `143–145`, `166`.
- **Evidence:** `it.title.contains(sc.targetId, ignoreCase = true) || it.title.equals(sc.title) || it.title.contains(sc.title, ignoreCase = true)` picks the **first** task whose title merely contains the shortcut text.
- **Impact:** a tile "Run" may show the completion state of, and toggle, "Run payroll" instead. Wrong item completed.
- **Fix:** resolve by `targetId` as a numeric task ID; migrate legacy title-only shortcuts once with a unique-match rule and a user prompt on ambiguity.
- **Risk:** medium (legacy shortcuts). **Complexity:** M. **Tests:** two tasks with overlapping titles.

#### REM-01 · P1 · Receiver does async work without `goAsync()`
- **Where:** `ReminderBroadcastReceiver.kt` lines 60, 93, 115, 143, 177, 235, 302, 329, 367; `QuickRemindersWidgetProvider.kt:120`.
- **Evidence:** 10 `CoroutineScope(Dispatchers.IO).launch { … }` blocks started inside `onReceive` with no `goAsync()`/`PendingResult.finish()`. The tracker and subscription widget providers *do* use `goAsync()` (line 40 each).
- **Impact:** after `onReceive` returns, the process can be frozen or killed before the coroutine finishes, especially on aggressive OEMs. Symptoms: a notification action that does nothing, a dose not marked, or a snooze not scheduled.
- **Fix:** one helper `runAsync { }` that calls `goAsync()`, runs on `Dispatchers.IO` with a timeout, and finishes in `finally`.
- **Risk:** low. **Complexity:** S. **Tests:** Robolectric receiver test with a fake `PendingResult`.

#### REM-02 · P1 · No exact-alarm permission flow
- **Where:** `AndroidManifest.xml:8`; `NotificationHelper.kt` (`canScheduleExactAlarms()` at 355, 540, 629); no `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` anywhere in `src/main`.
- **Evidence:** code checks `canScheduleExactAlarms()` and silently falls back to `setAndAllowWhileIdle`. No screen asks the user to grant the special permission and no receiver reacts when it changes.
- **Impact (to verify on device):** on recent Android versions this permission is not granted by default to new installs of non-alarm/calendar apps, so reminders and "P1 alarm" tasks may arrive late with no explanation.
- **Fix:** Settings row "Precise reminders" that opens `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`, an in-app status chip, a listener for `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` that reschedules, and a documented fallback. Evaluate `USE_EXACT_ALARM` only if Play policy fits (medication/alarms may qualify; this is a **policy question [P]**, not assumed).
- **Risk:** low. **Complexity:** M.

#### REM-03 · P1 · No reschedule on time, timezone or package change
- **Where:** `AndroidManifest.xml:34–47` lists `BOOT_COMPLETED` only; `ReminderBroadcastReceiver` boot branch (≈55–90) handles **today's** tasks and doses.
- **Evidence:** no handler for `TIME_SET`, `TIMEZONE_CHANGED` or `MY_PACKAGE_REPLACED`; `grep` finds none.
- **Impact:** after a timezone change or clock correction, scheduled alarms stay at their old absolute times.
- **Fix:** a single `RescheduleAllReceiver` for those actions that rebuilds alarms for a rolling horizon (see REM-04).
- **Risk:** low. **Complexity:** M.

#### DT-01 · P1 · Shared `SimpleDateFormat` instances
- **Where:** `utils/DateUtils.kt:13–16` (`private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)` and three others); used by `getDayOfWeek` (line ≈101), `formatDisplayDate`, `todayString`.
- **Evidence:** `SimpleDateFormat` is documented as not thread-safe. These singletons are used from the main thread, `Dispatchers.IO` coroutines (receiver, worker, widget service via `runBlocking`) and Compose.
- **Impact:** intermittent wrong dates or `NumberFormatException` under concurrent calls (notably widget refresh racing the UI). `getDayOfWeek` swallows exceptions and returns `"UNKNOWN"`, which hides the problem and makes weekday recurrence skip days.
- **Fix:** replace with `java.time` (desugaring is already enabled): `LocalDate.parse`, `DayOfWeek`, `ChronoUnit.DAYS.between`. Keep `DateUtils` as a thin facade so the 209 call sites in 25 files stay unchanged. Inject a `Clock` for testability.
- **Risk:** medium (behaviour parity). **Complexity:** M. **Tests:** parity tests old vs new over 10 years of dates; DST edges; multithread stress.

#### REC-01 · P1 · "Every N weeks" fires a week early on some configurations
- **Where:** `domain/recurrence/RecurrenceEngine.kt:121–122` (`EVERY_N_WEEKS`) and `158–159` (`CUSTOM`/`WEEKS`).
- **Evidence:** `weeksDiff = daysDiff / 7` where `daysDiff` is days since the **due date**, not since the start of the due date's week. Example: due Wed 1 Jan, `recurrenceDaysOfWeek = {MON, FRI}`, interval 2. Fri 3 Jan: `daysDiff 2 → 0` ✓. **Mon 6 Jan: `daysDiff 5 → 0 → 0 % 2 == 0` ✓ (fires)**, but Mon 6 Jan is in the *next* calendar week, so with "every 2 weeks" it should be skipped. Correct matches would be Mon 13 Jan onward.
- **Impact:** bi-weekly multi-day tasks appear in consecutive weeks for the first cycle, and the same arithmetic misaligns later cycles.
- **Fix:** compute the week index from the Monday (or locale week start) of the due date: `weekIndex = (epochDay(target) - epochDay(weekStart(due))) / 7`.
- **Risk:** medium: existing tasks will move occurrences. Changelog note required. **Complexity:** S. **Tests:** table test above; matrix of intervals 1–4 × day sets × start weekday.

#### DI-01 · P1 · No foreign keys; multi-step writes outside transactions
- **Where:** all `entities/*.kt` (`ForeignKey` is imported in two files but **no `foreignKeys =` appears in the codebase**); `LifeRepository.kt:194` `insertTask`, `201` `updateTask`, `207` `deleteTask`.
- **Evidence:** `insertTask` writes the task row, then labels, then reminders in separate calls. `deleteTask` runs six separate deletes. Only 4 `withTransaction` usages exist in the 2,731-line repository (lines 1840, 2621, 2683, 2713).
- **Impact:** a crash or cancellation between steps leaves orphan label links, reminders or medication rows. With no FKs the database cannot prevent this. `deleteTask` is also a **hard delete** although a `TrashItemEntity` exists.
- **Fix:** wrap the three methods in `withTransaction`; add FKs with `ON DELETE CASCADE` through a migration **10→11** that first cleans orphans; add `PRAGMA foreign_key_check` to the migration test.
- **Risk:** **high** for the FK migration (must clean orphans first). **Complexity:** L. **Tests:** orphan seed → migrate → verify; cancellation mid-write.

#### MON-01 · P1 · "Unlock Premium" button grants premium free; messaging disagrees [P]
- **Where:** `ui/screens/gamification/GamificationScreen.kt:182` → `LifeViewModel.toggleSimulatedPremium` (≈1860) → `PremiumEntitlement.kt` (`setSimulatedPremium`, key `simulated_developer_premium`).
- **Evidence:** the button is shown in the release build. There is no `BuildConfig.DEBUG` gate anywhere in the code (0 occurrences). The screen text calls Hunter Ranks, XP, streaks and the Reward Shop "premium perks"; the interface comment in `PremiumEntitlement.kt` says premium is "strictly cosmetic (themes, particle animations, custom engravings)".
- **Impact:** either premium is free (then "premium" is dead UI) or a paywall is planned but a bypass ships. 40 references to premium/entitlement exist in the code.
- **Fix:** **decide first.** Option A (recommended given the identity): remove the premium concept and unlock all themes; keep gamification free. Option B: keep a real, optional, one-time unlock and remove the simulator from release builds. Do not leave a dev toggle in production.
- **Complexity:** S (A) / L (B).

#### DOC-01 · P2 · Documentation drift
- **Evidence:** `README.md:66` says "Version 6 Room SQLite Database", the DB is version 10. `README.md:65` claims challenge progress in backups (BK-01). `PRIVACY.md:28` describes SAF (BK-03). Seven top-level reports (`RELEASE_2_READINESS_REPORT.md`, `MOBILE_TESTING_REGRESSION_REPORT.md`, `RECOVERY_GAP_REPORT.md`, …) duplicate history that belongs in `CHANGELOG.md`.
- **Fix:** correct README/PRIVACY in the same PR as the code they describe; move reports under `docs/archive/`.
- **Complexity:** S.

### 8.2 Suspected risks (pattern is real; effect not demonstrated)

| ID | Sev. | Where | Hypothesis | How to confirm |
|---|---|---|---|---|
| REM-04 | P1 | `NotificationHelper.scheduleAllTaskReminders` callers: only `ReminderBroadcastReceiver.kt:64` (boot, today) and `MaintenanceWorker.kt:47,52` (today + tomorrow) | Alarms for tasks two or more days ahead rely on creation-time scheduling from other paths (not traced) plus a 24-hour worker; a reboot, force-stop or timezone change may leave day+2… without alarms until the worker runs | Create a task with a reminder 5 days out, reboot, `adb shell dumpsys alarm | grep com.aistudio.lifeos` |
| REM-05 | P2 | manifest | App update or force-stop may drop alarms; no `MY_PACKAGE_REPLACED` rebuild | Update-in-place test with `adb install -r`, dump alarms |
| DT-02 | P2 | `DateUtils.kt:13–16` | `SimpleDateFormat` captures the default time zone at construction, so `todayString` helpers using it can be stale after a timezone change while the process lives (some methods take a `TimeZone`, others do not) | Unit test with `TimeZone.setDefault` between calls |
| PERF-01 | P2 | `RecurrenceEngine.countOccurrencesUpTo` | For "ends after N" tasks, each evaluation loops up to 1,460 days and `addDays`/`daysBetween` allocate `Calendar`s per iteration; called per task per date, per emission | Benchmark 500 tasks, month view |
| PERF-02 | P2 | `QuickRemindersRemoteViewsService.onDataSetChanged` (≈line 96) | `runBlocking` loads all tasks, habits and projects each time the widget refreshes; 36 call sites of `notifyAllWidgetsChanged` | Count refreshes per task toggle; Perfetto trace |
| BK-04 | P2 | `BackupRestoreDialog.kt:170–176` | `ACTION_SEND` with the entire JSON in `EXTRA_TEXT` may exceed Binder transaction limits for large databases (`TransactionTooLargeException`) | Export a 2 MB seed |
| CONC-01 | P2 | `ReminderBroadcastReceiver.onReceive` (≈52–55) | Every broadcast constructs `UserPreferences`, a premium repo and a `LifeRepository` before dispatch, even for actions that do not need them | Profile receiver latency |
| SEC-03 | P3 | `AndroidManifest.xml:9`, `build.gradle.kts` release | `USE_FULL_SCREEN_INTENT` declared (recent Android versions restrict it to calling/alarm apps and the store reviews it); `isMinifyEnabled=false` | Check Play pre-launch report; decide on R8 |
| REC-02 | P2 | `RecurrenceEngine.matchesMonthDay`/`matchesYearly` (not read) | Monthly tasks anchored on the 29th–31st may skip short months, and Feb 29 yearly handling is unknown | Table test for day 31 across 12 months |

### 8.3 Not reviewed (so not claimed clean)

`TemplateCatalog` import logic, `MedicationScheduleEngine` correctness beyond its test file, `TaskCompletionFeedbackManager`, `AnalyticsScreen` calculations, `UserPreferences` migration, `LegalConsentScreen` flow, `ThemeAssetManager` (bitmap work), `CalendarScreen` rendering cost, and the 4-table tracker/subscription domain maths.

---

## 9. Security and Privacy Review

| Check (from brief §3G) | Result |
|---|---|
| Permissions | `POST_NOTIFICATIONS`, `VIBRATE`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`, `USE_FULL_SCREEN_INTENT`. **No INTERNET, no storage, no location, no camera.** Good. |
| Exported components | `MainActivity` (launcher, expected). `ReminderBroadcastReceiver` exported, with 10 actions → **SEC-01**. Three widget providers exported (normal) but one carries a custom action → **SEC-02**. `QuickRemindersRemoteViewsService` is protected by `BIND_REMOTEVIEWS`, not exported. `FileProvider` not exported, grants URI permissions; `file_paths.xml` exposes `cache-path .` and `files-path .` (the **whole** `files/` and `cache/` directories). Narrow to the specific sub-folders that are actually shared. |
| PendingIntent mutability | All `FLAG_IMMUTABLE` except the widget click template (`QuickRemindersWidgetProvider.kt:87`), which needs mutable fill-in. Acceptable once the target is private. |
| Input validation | Import accepts arbitrary JSON up to 50M characters and trusts IDs (BK-02). `getString("title")` throws on a missing field and aborts mid-import. |
| Local file handling | No SAF use today (BK-03). `FileProvider` paths too broad. |
| Backup security | Plain JSON, no integrity hash, no optional passphrase encryption. Auto Backup enabled (PRV-01). |
| Logging | 0 `Log.*` calls (good for privacy); 2 `printStackTrace()` calls (system log; replace with a no-PII logger). |
| Signing | `keystore.properties`/env based; release build resolves `rootProject` path (last commit). `proguard` unused (`minify=false`). The unsigned-APK checksum is recorded in the readiness report; no CI signing. |
| Medication data | Stored unencrypted in the app DB like everything else. For a health-adjacent feature, document it; optional DB-level encryption is an R5+ discussion (SQLCipher adds size and a native dependency). |

---

## 10. Performance and Battery Review

All items are **[S]** until profiled. Highest value first:

1. **Recurrence counting** (PERF-01), a bounded loop but run in a flow `map` over every task for every date query.
2. **`getTasksForDateFlow`** (`LifeRepository.kt:183`) filters the whole in-memory task list each time `allTasksFlow` emits. Fine at hundreds of tasks; poor at thousands. Add a date-range query or memoise per date.
3. **Widget refresh storm** (PERF-02): 36 refresh call sites; coalesce with a debounce in `WidgetRefreshCoordinator`.
4. **God ViewModel**: 117 flow references in one `AndroidViewModel` means many collectors recompose on unrelated changes unless every screen derives narrow state. Needs a recomposition-count audit (Layout Inspector) before claiming harm.
5. **Startup**: `MainActivity.onCreate` builds the DB, preferences, repository and VM synchronously and schedules alarms and work on the main thread. Move scheduling after first frame (Baseline Profile candidate).
6. **No R8** (`isMinifyEnabled=false`): larger APK and slower class loading. Enable with keep rules for Room/reflection users and test the release build.
7. **Battery**: exact alarms and a 24-hour worker are modest. The risk is correctness, not drain.

---

## 11. Data Integrity and Migration Review

| Topic | Finding |
|---|---|
| Schema export | `exportSchema = false` (`AppDatabase.kt:591`). No `schemas/` directory, so Room's `MigrationTestHelper` and schema diffs are impossible. **Turn on `room.schemaLocation` and commit schemas now** so every future migration is testable. |
| Migrations | 9 hand-written (`MIGRATION_1_2` … `9_10`), some using `PRAGMA table_info` to patch columns (5→6, 6→7). Tested: **v6→v10 only** (`RoomMigrationRobolectricUnitTest`, `RoomMigrationInstrumentationTest`). Paths from v1–v5 are untested; Release 1/beta users may still exist. |
| Foreign keys | None (DI-01). Orphans possible for `task_label_cross_ref`, `task_reminders`, `medication_doses`, `usage_entries`, `subscription_bills`, `subscription_payments`. |
| Transactions | Only 4 usages. Task save/delete are multi-step (DI-01). |
| Unique constraints | Only `medication_doses (medicationId, scheduledDateIso, scheduledTime)` is unique. Honeycomb shortcut ↔ task link has no uniqueness; `getOrCreateTaskForShortcut` relies on application logic. |
| JSON-in-column | `subtasksJson`, `completedDatesJson`, `skippedDatesJson`, `rescheduledDatesJson`, challenge `dailyLogsJson`. Cannot be indexed or queried; completion history grows unbounded inside one row. Acceptable now; plan a `task_completions` table in Release 5+ with a safe backfill. |
| Delete/restore | `TrashItemEntity` exists but `deleteTask` hard-deletes (and cascades manually to medications and shortcuts). Verify which UI paths use Trash. |
| Backup compatibility | `BACKUP_VERSION = 1`, unread on import (BK-02). |
| Reset/destructive | No `fallbackToDestructiveMigration` (good). |

**Rule for every Release 4 change here:** no migration may drop or rewrite user data without a pre-migration copy of the DB file and a passing migration test from every supported version.

---

## 12. Architecture and Technical Debt

| ID | Issue | Evidence | Why it matters |
|---|---|---|---|
| ARC-01 | Two god classes | `LifeViewModel` 2,647 lines/179 funs; `LifeRepository` 2,731 lines/148 funs | Every feature touches both, so merge conflicts, slow compiles and untestable units |
| ARC-02 | No dependency injection | `LifeRepository(...)` built by hand in four places; receivers and workers rebuild the graph per event | Duplicated wiring (CONC-01); tests use ad-hoc construction |
| ARC-03 | Business rules inside UI/receivers | `ReminderBroadcastReceiver` contains dose-building and alarm logic (379-line file) | Hard to unit test; REM-01 exists because of this |
| ARC-04 | Data in code | `TemplateCatalog.kt` 1,659 lines of template content | Editing content needs a rebuild; no per-template version for upgrades |
| ARC-05 | Component catalog shipped in the production app | `ui/screens/manual/ComponentCatalog.kt` + `ComponentDetailSheet.kt` ≈ 2.7k lines of "live previews" of 36 components | Developer documentation inside user APK; consider moving to a debug-only source set |
| ARC-06 | State-based navigation | `MainScreen.kt:49` | No deep links (notification → exact task), no back-stack restoration; `navigationCompose` declared and unused |
| ARC-07 | Naming | `namespace = "com.example"` and `applicationId = "com.aistudio.lifeos.vtrqkx"` | `applicationId` cannot change after publishing, but `namespace` **can** be renamed safely in source |
| ARC-08 | Version catalog drift | Catalog entries with 0 imports in code: Retrofit, OkHttp, Moshi, Coil, Firebase (ai, firestore, auth, appcheck), CameraX, play-services-location, credentials, accompanist, plus the `secrets` and `google-services` plugin aliases | No APK impact (not in `dependencies {}`), but misleading to readers and tools, and contradicts the "no cloud" story |
| ARC-09 | Dependency staleness | Compose BOM `2024.09.00` against `compileSdk 36`; lint baseline holds 23 `GradleDependency` and 9 `NewerVersionAvailable` | Security and bug-fix lag |
| ARC-10 | Tests grouped by bug batch | `Batch01…04…RegressionUnitTest`, `Release2RegressionUnitTest` | Fine as regression history; new tests should be grouped by feature |
| ARC-11 | CI too thin | Only `testDebugUnitTest` + `assembleDebug`; no `lintRelease`, no `assembleRelease`, no instrumented migration test, no dependency review, actions pinned by tag not SHA | Lint is a local-only gate despite `abortOnError` |

---

## 13. Features to Improve (works today, needs better UX, performance, accessibility or maintainability)

| ID | Feature | Improvement | Evidence / reason | Effort |
|---|---|---|---|---|
| IMP-01 | Backup & restore | File-based (SAF), named files, preview of counts before restore, "replace" vs "merge", result summary | BK-02, BK-03, U1 | M |
| IMP-02 | Reminders | "Reminder health" panel: notification permission, exact-alarm status, battery-optimisation hint, next scheduled alarms, "test reminder" | REM-02, REM-03, U6 | M |
| IMP-03 | Accessibility | Give every interactive icon a `contentDescription`; ≥48dp touch targets; fix 6 `SmallSp` lint hits; TalkBack pass on Home, Tasks, QR tiles | 138 null of 316 | M |
| IMP-04 | Localisation | Move strings to `strings.xml` (start with Settings, Tasks, Reminders); `Locale`-aware date formatting (today `Locale.US` is hard-coded in `DateUtils`) | U5 | L |
| IMP-05 | Quick Reminders widget | Resolve by ID, debounce refreshes, show an empty state that deep-links to the tile editor | WID-01, PERF-02 | M |
| IMP-06 | Settings | Split the 2,059-line screen into grouped sub-screens with a search box | U3 [S] | M |
| IMP-07 | Notifications | Deep link each notification to its task/dose, not just the home screen | Navigation is state-based (ARC-06) | M |
| IMP-08 | Habit streaks | Keep streaks, add a forgiving "strength" score and a calendar heat-map | Loop's habit score; Habitica streak-loss complaints (§6) | M |
| IMP-09 | Gamification | Make XP/ranks optional (toggle) and consistent with premium messaging | U7, MON-01 | S |
| IMP-10 | Release build | Enable R8 with keep rules; add Baseline Profile; defer alarm/worker scheduling off the main thread | §10 | M |

---

## 14. Code to Refactor or Replace

Every item keeps persisted data and public behaviour. "Strangler" means: introduce the new unit, route one call site at a time, delete the old path last.

| ID | Current | Target | Migration strategy | Why now | Risk |
|---|---|---|---|---|---|
| RFR-01 | `LifeRepository` (2,731 lines) | Per-domain repositories (`TaskRepository`, `HabitRepository`, `MedicationRepository`, `TrackerRepository`, `SubscriptionRepository`, `BackupRepository`) behind small interfaces | Strangler: extract one domain per PR; `LifeRepository` delegates until empty; full suite green at each step | Prerequisite for tests of REC-01, DI-01 | Med |
| RFR-02 | `LifeViewModel` (2,647 lines) | One ViewModel per screen/feature (`TasksViewModel`, `HabitsViewModel`, …) with narrow `StateFlow`s | Extract by screen; keep `LifeViewModel` as facade for unmigrated screens | Fewer recompositions, parallel work | Med |
| RFR-03 | Hand-built object graph | A tiny `AppContainer` (manual DI) first; Hilt/Koin only if the container grows | `Application.onCreate` builds singletons; receivers/workers/widgets read from it (removes CONC-01) | Receivers rebuild everything per broadcast | Low |
| RFR-04 | Receiver contains domain logic | `ReminderActionHandler` (pure suspend functions) + thin receiver calling it via `goAsync` helper | Move logic first, then switch the receiver; keep action strings unchanged so queued notifications still work | REM-01, testability | Low |
| RFR-05 | `DateUtils` (`SimpleDateFormat` singletons) | `java.time` facade with injected `Clock` | Keep method names; implement with `LocalDate`; run old/new parity test for 10 years before deleting old code | DT-01 (209 call sites in 25 files) | Med |
| RFR-06 | Enum-state navigation | Navigation Compose (already in the catalog) with typed routes and `lifeos://task/{id}` deep links | Introduce `NavHost` for new routes only; migrate tabs last | IMP-07, back-stack bugs | Med |
| RFR-07 | JSON date lists in `tasks` rows | `task_completions(taskId, dateIso, status)` table | **Release 5+.** Add table, dual-write, backfill in migration with a verified copy, then switch reads. Never delete the JSON columns in the same release | Queries, analytics, unbounded row growth | High |

---

## 15. Features or Code to Remove or Deprecate

**Rule applied:** nothing here deletes persisted user data. Each item says whether it is referenced, what happens to existing data, and how to retire it safely.

| ID | Candidate | Referenced elsewhere? | Data impact | Compatibility / safe process | Why removal beats repair |
|---|---|---|---|---|---|
| RM-01 | "Unlock Premium" simulator (`toggleSimulatedPremium`, `setSimulatedPremium`, `simulated_developer_premium` key) | `GamificationScreen.kt:182`, VM (≈1860), `LocalPremiumEntitlementRepository` (40 premium references overall) | A DataStore boolean only | If Option A in MON-01: unlock all themes first, then delete the code in the next release; leave the DataStore key unread (harmless) | A dev toggle in production cannot be "repaired" into a purchase flow |
| RM-02 | Clipboard export / paste-import as the primary backup path | `BackupRestoreDialog.kt` | None | Keep paste-import under "Advanced" for one release so people with an old copied backup can still restore | Replaced by SAF (BK-03) |
| RM-03 | Unused catalog entries (Retrofit, OkHttp, Moshi, Coil, Firebase ×4, CameraX ×4, play-services-location, credentials, accompanist) and the `secrets`/`google-services` plugin aliases | 0 imports in `src/main` (`grep` verified) | None | Delete from `libs.versions.toml`, run a full build and `lintRelease` | Dead metadata that implies cloud use |
| RM-04 | Component catalog + detail sheet in release builds (`ui/screens/manual/ComponentCatalog.kt`, `ComponentDetailSheet.kt`, ≈2.7k lines, and their tests) | `UserManualScreen`, `ComponentCatalogUnitTest` | None | Move to a `debug` source set or `internal` module; keep the end-user manual part | Developer documentation does not belong in the user APK |
| RM-05 | Custom actions on exported components | manifest lines 34–47 and 52–60 | None | Split exported/non-exported receivers (SEC-01/02); old `PendingIntent`s in already-posted notifications must still resolve, so keep action strings, change only export flags | Cannot be made safe while exported |
| RM-06 | `printStackTrace()` (2 sites) | `DataExportImportManager` and one more | None | Replace with a PII-free logger | Leaks to system log |
| RM-07 | Seven root-level report markdown files | docs only | None | Move to `docs/archive/`, update links in README | Reduces drift (DOC-01) |
| RM-08 | Template placeholder comments in `backup_rules.xml` / `data_extraction_rules.xml` | manifest | None | Replace with deliberate rules (PRV-01) | Unreviewed boilerplate |
| RM-09 | `FileProvider` paths `files-path .` and `cache-path .` | `file_paths.xml` | None | Narrow to the exact sub-directories used for sharing | Over-broad grant |

---

## 16. New Feature Opportunities

Distinguishing **high value** from **bloat**: each needs an offline implementation, a test plan and a maintenance cost under one engineer's budget.

| ID | Proposal | Evidence of need | Value | Effort | Verdict |
|---|---|---|---|---|---|
| F-01 | **SAF backup/restore UI** with file name, preview and merge/replace | BK-03; Loop/Tasks.org offer file export/backup | Very high | M | **R4 (core)** |
| F-02 | **CSV export** (tasks, habits, challenge, trackers) | Loop exports CSV | High | S–M | **R4** |
| F-03 | **Backup integrity hash** (SHA-256 field) and optional passphrase encryption (AES-GCM via `javax.crypto`) | Backup is plain JSON; Tasks.org mentions E2E encryption for sync | Medium | M | Hash in R4, passphrase R5 |
| F-04 | **Reminder health screen** | REM-02/03/04; common silent-failure class | High | M | **R4** |
| F-05 | **Saved filters / smart lists** (project, label, priority, due range) | Todoist/TickTick both emphasise filters (aggregator sources) | High | M | **R4** |
| F-06 | **Local natural-language quick add** ("buy milk tomorrow 6pm !p1 #home") rule-based | Todoist's most-cited feature (agg.) | High | M | R4 stretch / R5 |
| F-07 | **Habit strength score + heat-map** next to streaks | Loop habit score; streak-loss complaints | Medium–High | M | **R4** |
| F-08 | **Notification deep links** to the exact task/dose | Navigation limitation | Medium | M | R4 (after RFR-06 for new routes) |
| F-09 | **Localisation scaffolding + one pilot language** | 4-line `strings.xml`; Loop/Tasks.org translated | Medium | L | R4 scaffolding; languages R5 |
| F-10 | **TalkBack and large-text pass** | 138 null descriptions | Medium | M | **R4** |
| F-11 | **Focus timer (Pomodoro)** | TickTick bundles it | Low–Medium | M | **Bloat risk.** R5+ only if user research asks |
| F-12 | **Template versioning + user template import/export** | `TemplateCatalog` is compiled code | Medium | L | R5 |
| F-13 | **Optional file/CalDAV sync** | Tasks.org model | High but large | XL | **R5+**, strictly optional |
| F-14 | **Read-only ICS import** (calendar events as tasks) | Calendar interop | Medium | L | R5; no live calendar sync |

**Explicitly not recommended:** ads, analytics SDKs, accounts, mandatory subscriptions, cloud AI. If AI is ever explored it must be **opt-in and on-device**, adds no network permission, and degrades to a no-op. That is a Release 5+ research spike, not a requirement.

---

## 17. Release 3 Reserved Scope and Dependencies

**No features are assigned to Release 3.** It belongs to the owner's separate plans. Only compatibility constraints are recorded so Release 4 and 3 do not collide:

| # | Constraint | Reason |
|---|---|---|
| R3-C1 | **Room version numbering.** `DI-01` proposes a 10→11 migration. If Release 3 changes the schema, Release 4's migration must take the **next free version** and be written against Release 3's final schema. | Both releases touch `AppDatabase` |
| R3-C2 | **Any new table added in Release 3 must be added to Backup v2.** The entity-coverage guard test (BK-01 fix) will fail the build if not. | Prevents repeating BK-01 |
| R3-C3 | Any new alarm/notification type in Release 3 should use the `goAsync` helper and the reschedule receiver from Phase R4-P2, or inherit REM-01/03. | Same root causes |
| R3-C4 | Any new date logic should use the `java.time` facade (R4-P3) or be written so it can be swapped. | Avoids new `SimpleDateFormat` use |
| R3-C5 | `applicationId` stays `com.aistudio.lifeos.vtrqkx`; do not change it. | Published app identity |
| R3-C6 | If Release 3 ships first, rebase Release 4 phases on it. If Release 4 phases ship first, Release 3 inherits the fixes. **Do not assume an unimplemented Release 3 feature is absent from Release 4.** Unknowns are listed in §25. | Per brief |

---

## 18. Release 4 Proposed Scope: "Trust and Reliability"

**Theme:** make backups complete, reminders dependable, dates correct, and the codebase testable, then ship a *small* set of high-value features.

| Phase | Name | Contents | Exit criteria |
|---|---|---|---|
| **R4-P0** | Safety net | Room `exportSchema=true` + committed `schemas/`; migration tests v1→v10 and v5→v10; CI adds `lintRelease`, `assembleRelease`, and an instrumented-test compile step; pre-migration DB copy | CI green on a PR that only changes CI; every migration path tested |
| **R4-P1** | Backup v2 | BK-01, BK-02, BK-03, PRV-01 decision, F-01, F-02, F-03 (hash), RM-02, RM-08 | Round-trip test over **all** entities; corrupt file leaves DB untouched; no clipboard dependency |
| **R4-P2** | Reminder reliability and receiver security | REM-01…05, SEC-01, SEC-02, WID-01, IMP-02/F-04, RFR-04 | Receiver tests with fake `PendingResult`; reschedule on boot/time/timezone/update; foreign intents ignored |
| **R4-P3** | Dates and recurrence | DT-01, DT-02, REC-01, REC-02, RFR-05, PERF-01 | Old/new parity test; recurrence table tests pass; no `SimpleDateFormat` left |
| **R4-P4** | Data integrity | DI-01 (transactions, then FK migration 10→11 per R3-C1), cleanup of orphans, Trash vs hard delete audit | Orphan-seed migration test; FK check clean |
| **R4-P5** | Product decisions and hygiene | MON-01, DOC-01, RM-01/03/06/07/09, IMP-09 | README, PRIVACY, manifest, premium story all consistent |
| **R4-P6** | Architecture split | RFR-01…03 (strangler, one domain per PR) | `LifeRepository`/`LifeViewModel` each under ~800 lines or removed |
| **R4-P7** | Selected features | F-04 (if not done), F-05, F-07, F-08, F-10, F-09 scaffolding, IMP-03/04/05/08 | Each feature has tests and an acceptance list (§23) |

If time is short, **P0–P4 form the minimum valuable Release 4.** P6/P7 can slip to Release 5 without leaving users at risk.

---

## 19. Release 5+ Future Roadmap

| Item | Why not Release 4 |
|---|---|
| RFR-07 completions table | Needs R4's schema export and migration tests first; high-risk data move |
| F-13 optional file/CalDAV sync | XL; needs a conflict model |
| F-03 passphrase-encrypted backups | Needs UX for key loss |
| Navigation Compose full migration (RFR-06 remainder) | Do after the ViewModel split |
| F-12 template versioning and user templates | Depends on `TemplateCatalog` extraction |
| F-14 ICS import | Interop scope |
| F-11 focus timer | Only if users ask |
| Encrypted database (SQLCipher) | Size/native cost; evaluate against threat model |
| Opt-in on-device AI spike | Research only; never required |
| Wear/tablet layouts | Not evidenced as needed |

---

## 20. Prioritised Master Backlog

Score inputs: **Sev** (P0–P3), **Impact** (1–5), **Freq** (1–5), **Diff.** (differentiation 1–5), **Effort** (S/M/L/XL), **Risk** (L/M/H), **Conf.** (evidence confidence H/M/L). Ranked top to bottom.

| Rank | ID | Item | Sev | Imp. | Freq | Diff. | Effort | Risk | Conf. | Priority | Phase |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | BK-01 | Backup covers all 28 tables | P0 | 5 | 4 | 4 | L | M | H | **P0** | R4-P1 |
| 2 | BK-02 | Atomic, validated, versioned import | P1 | 5 | 3 | 3 | L | M | H | **P0** | R4-P1 |
| 3 | SEC-01 | Receiver export/validation | P1 | 4 | 2 | 1 | S | L | H | **P1** | R4-P2 |
| 4 | REM-01 | `goAsync` in receiver | P1 | 4 | 4 | 1 | S | L | H | **P1** | R4-P2 |
| 5 | DT-01 | `java.time` replaces `SimpleDateFormat` | P1 | 4 | 3 | 1 | M | M | H | **P1** | R4-P3 |
| 6 | REC-01 | Week-index recurrence fix | P1 | 3 | 3 | 1 | S | M | H | **P1** | R4-P3 |
| 7 | DI-01 | Transactions, then FKs | P1 | 4 | 2 | 1 | L | H | H | **P1** | R4-P4 |
| 8 | REM-02/03/04 | Exact alarm flow, reschedule triggers, horizon | P1 | 4 | 4 | 2 | M | L | M | **P1** | R4-P2 |
| 9 | PRV-01 / BK-03 | Auto Backup decision + SAF | P1 | 4 | 5 | 4 | M | L | H | **P1** | R4-P1 |
| 10 | MON-01 | Resolve premium story | P1 | 3 | 5 | 2 | S–L | L | H | **P1 [P]** | R4-P5 |
| 11 | R4-P0 | Schema export, migration tests, CI lint | P1 | 4 | n/a | 1 | M | L | H | **P1** | R4-P0 |
| 12 | WID-01 / SEC-02 | ID-based widget tiles, private click path | P2 | 3 | 3 | 2 | M | M | H | **P2** | R4-P2 |
| 13 | F-04 | Reminder health screen | P2 | 4 | 4 | 4 | M | L | M | **P2** | R4-P7 |
| 14 | F-05 | Saved filters | P2 | 4 | 4 | 3 | M | L | M | **P2** | R4-P7 |
| 15 | F-07 / IMP-08 | Habit strength + heat-map | P2 | 3 | 4 | 3 | M | L | M | **P2** | R4-P7 |
| 16 | IMP-03 / F-10 | TalkBack and touch targets | P2 | 4 | 3 | 3 | M | L | H | **P2** | R4-P7 |
| 17 | RFR-01–03 | Repository/VM split, manual DI | P2 | 3 | 5 | 1 | XL | M | H | **P2** | R4-P6 |
| 18 | PERF-01/02 | Recurrence counting, widget refresh | P2 | 2 | 3 | 1 | M | L | L | **P2** | R4-P3 / P2 |
| 19 | RM-03/04 | Remove unused deps, move component catalog | P2 | 2 | 1 | 1 | S–M | L | H | **P2** | R4-P5 |
| 20 | F-09 / IMP-04 | Localisation scaffolding | P2 | 3 | 3 | 3 | L | L | H | **P2** | R4-P7 |
| 21 | IMP-10 | R8, baseline profile | P3 | 2 | 5 | 1 | M | M | M | **P3** | R5 |
| 22 | F-06 | Local NL quick add | P3 | 3 | 4 | 3 | M | L | M | **P3** | R5 |
| 23 | F-08 / RFR-06 | Deep links + nav migration | P3 | 3 | 3 | 1 | L | M | M | **P3** | R5 |
| 24 | F-13 | Optional file/CalDAV sync | P3 | 4 | 3 | 5 | XL | H | M | **P3** | R5+ |
| 25 | F-11 | Focus timer | P3 | 2 | 2 | 1 | M | L | L | **P3 (bloat check)** | R5+ |

---

## 21. Phase-by-Phase Implementation Plan and Estimates

**Assumptions:** one experienced Android developer, ~4 productive days/week, Release 3 not merged in between, a physical device and one emulator on API 34+ available. Ranges include writing tests and updating docs. AI-assisted coding is assumed to speed typing, **not** review or on-device verification.

| Phase | Dependency | Optimistic | Likely | Pessimistic | Largest uncertainty |
|---|---|---|---|---|---|
| R4-P0 Safety net | none | 2 d | 4 d | 6 d | Old migrations needing fixture DBs |
| R4-P1 Backup v2 | P0 | 7 d | 12 d | 18 d | Format versioning; merge semantics |
| R4-P2 Reminders + receiver security | P0 | 6 d | 10 d | 15 d | OEM behaviour; device matrix |
| R4-P3 Dates + recurrence | P0 | 6 d | 9 d | 14 d | Behaviour parity over 209 call sites |
| R4-P4 Data integrity | P0, P1 | 6 d | 10 d | 16 d | Orphan cleanup and FK migration |
| R4-P5 Decisions + hygiene | decisions | 3 d | 5 d | 8 d | MON-01 option chosen |
| R4-P6 Architecture split | P0–P4 | 12 d | 20 d | 30 d | Compose state ownership |
| R4-P7 Selected features | P1, P3 | 15 d | 25 d | 35 d | UX iteration |
| **Total** | | **≈57 d** | **≈95 d** | **≈142 d** | |

Recommended sequence: **P0 → P1 → P2 → P3 → P4 → P5 → (P6 ∥ P7)**. Ship an interim **R4.0 = P0–P5** (≈ 8–9 weeks likely) and R4.1 = P6–P7.

---

## 22. Ready-to-Paste Bug-Fix Prompts (Antigravity)

Run them **in this order**, one branch and one PR each. Do not combine prompts.

**Common rules: paste at the top of every prompt**

```
COMMON RULES
- Repo: https://github.com/Kadhiravan-K/LifeOS. Base branch: main. Never touch tag v1.1.0. Never merge; open a DRAFT PR.
- Preserve all persisted user data. Never use fallbackToDestructiveMigration. Do not change applicationId.
- Change only what this prompt describes. If you find another bug, add it to PR notes, do not fix it here.
- Verify with: ./gradlew testDebugUnitTest lintDebug assembleDebug. Report each command's real result.
  If a command cannot run in your environment, say so; never claim it passed.
- Add or update tests BEFORE the fix where practical (they should fail first, then pass).
- Update README / PRIVACY / CHANGELOG only where this prompt says the behaviour changed.
- STOP and ask me if: a migration or schema change not listed here seems required, a test needs deleting to pass,
  or the fix changes user-visible recurrence/reminder times for existing tasks beyond what is described.
```

### PROMPT B1: Backup v2: complete, atomic, versioned (BK-01, BK-02)

1. **Objective.** Make export/import cover every Room table, import atomically, and read the backup version.
2. **Paths.** `app/src/main/java/com/example/data/backup/DataExportImportManager.kt`, `data/local/AppDatabase.kt`, `data/local/dao/*`, `data/local/entities/*`, `ui/viewmodel/LifeViewModel.kt` (export/import wrappers at lines 2419–2423), `ui/components/BackupRestoreDialog.kt`, tests under `app/src/test`.
3. **Verified problem.** `exportAllData` writes 12 JSON keys; `AppDatabase` has 28 entities. Missing: challenges, water_logs, task_reminders, task_label_cross_ref, trash, plans, plan_milestones, plan_task_cross_ref, xp_events, achievements, rewards, reward_redemptions, medications, medication_doses, usage_entries, inventory_adjustments, price_history. `importData` inserts row-by-row with REPLACE, no transaction, ignores `version`, returns a Boolean.
4. **Steps.** (a) Add a failing test that seeds every table, exports, clears, imports, and compares every row. (b) Add a guard test that reflects over `AppDatabase`'s entity list and fails if an entity has no exporter. (c) Implement exporters/importers for the missing tables; set `BACKUP_VERSION = 2`; keep reading version 1 files (missing keys → empty). (d) Parse and validate the whole file before touching the DB (required fields, version ≤ supported, foreign references exist). (e) Apply all inserts inside one `database.withTransaction`; on any error nothing changes. (f) Return a typed result `{ok, counts per table, warnings, error}`; show counts in the dialog. (g) Offer `REPLACE_ALL` (clear then import, in the same transaction) and `MERGE` (assign new IDs, remap references); default to REPLACE_ALL with a confirmation. (h) Remove `printStackTrace`.
5. **Data preservation.** Take a copy of `lifeos_database.db` into `cacheDir` before REPLACE_ALL; delete it only after commit succeeds. Never delete rows outside the transaction.
6. **Tests/commands.** New `BackupRoundTripTest` (Robolectric, in-memory DB), `BackupEntityCoverageTest`, `BackupCorruptFileRollbackTest`, `BackupV1CompatibilityTest` (use a v1 sample JSON from the current exporter), `BackupMergeIdRemapTest`. Run the common gate.
7. **Acceptance.** All 28 tables round-trip; a truncated JSON leaves the DB byte-for-byte unchanged; a v1 file still restores; adding a 29th entity without an exporter fails the guard test; README line about backup scope is accurate.
8. **Regression risks.** ID remapping in MERGE (labels↔tasks, bills↔subscriptions, doses↔medications); large files on low-memory devices (stream with `JsonReader` if > 5 MB); alarms are not restored by import, so trigger the reschedule helper after a successful restore.
9. **Git.** `git checkout -b fix/backup-v2-complete-atomic`; small commits (tests → exporter → importer → UI); push; draft PR titled "fix(backup): v2 covers all tables, atomic import".
10. **Stop if** a table cannot be represented without a schema change, or the entity guard cannot enumerate entities without KSP changes.

### PROMPT B2: SAF backup UI and Auto Backup stance (BK-03, PRV-01)  *(decision required first: see §25 D1)*

1. **Objective.** Replace clipboard/paste with Storage Access Framework files, and make the Auto Backup stance explicit.
2. **Paths.** `ui/components/BackupRestoreDialog.kt`, `AndroidManifest.xml` (line 12), `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`, `res/xml/file_paths.xml`, `PRIVACY.md`.
3. **Verified problem.** Export uses `ClipboardManager` and `ACTION_SEND` text; import is a paste box; PRIVACY.md says SAF. `allowBackup="true"` with template-only rules.
4. **Steps.** (a) Use `ActivityResultContracts.CreateDocument("application/json")` with default name `lifeos-backup-YYYY-MM-DD.json`, and `OpenDocument` for import. (b) Show a pre-restore summary from B1's typed result before applying. (c) Keep paste-import under an "Advanced" expander for one release; remove clipboard *export*. (d) If decision D1 = "no platform backup": set `allowBackup="false"` and delete the template comments; if D1 = "keep": write explicit `<include>/<exclude>` for the DB and DataStore and document it. (e) Narrow `file_paths.xml` to sub-folders actually shared. (f) Update PRIVACY.md to describe exactly what the app does.
5. **Data preservation.** Importing must use B1's transactional path. Never write a backup into a world-readable location.
6. **Tests.** Instrumented SAF round-trip with a test `DocumentsProvider` or `ActivityResultRegistry` fake; Robolectric UI-state tests; a lint check that `allowBackup` matches the chosen stance.
7. **Acceptance.** User can export to a chosen file and restore from it without the clipboard; PRIVACY.md matches the code; no export path puts the whole DB on the clipboard.
8. **Regression risks.** Users who rely on paste-restore (keep Advanced path); disabling Auto Backup removes a safety net for users who never exported. Show a one-time notice.
9. **Git.** Branch `feat/saf-backup-and-autobackup-stance`; draft PR.
10. **Stop if** D1 has not been answered.

### PROMPT S1: Receiver security and `goAsync` (SEC-01, SEC-02, REM-01, WID-01)

1. **Objective.** Stop other apps from driving reminder/widget actions and stop dropping async work.
2. **Paths.** `AndroidManifest.xml` (receiver at 34–47, widget provider at 52–60), `notifications/ReminderBroadcastReceiver.kt`, `notifications/NotificationHelper.kt`, `widgets/QuickRemindersWidgetProvider.kt`, `widgets/QuickRemindersRemoteViewsService.kt` (121–166), `widgets/TrackerWidgetProvider.kt` (goAsync pattern at line 40).
3. **Verified problem.** The receiver is exported and its package check passes when `package == null` (lines 44–50). Nine `CoroutineScope(Dispatchers.IO).launch` blocks run without `goAsync()`. The QR widget provider accepts a custom exported action and can create tasks. Widget tiles match tasks by `contains()` on titles.
4. **Steps.** (a) Write tests first: an implicit external `ACTION_TASK_COMPLETE` must be ignored; own `PendingIntent`s must still work. (b) Split the manifest: `ReminderBroadcastReceiver` → `exported="false"`; new tiny `BootReceiver` (exported, only `BOOT_COMPLETED`) that calls the existing reschedule path. (c) Add `inline fun BroadcastReceiver.runAsync(timeoutMs = 9_000, block: suspend () -> Unit)` that calls `goAsync()`, launches on IO with `withTimeout`, and calls `finish()` in `finally`; use it in every launch site. (d) Remove `ACTION_WIDGET_TOGGLE_TASK` from the exported provider's intent-filter; route clicks through a non-exported receiver using an explicit component. Validate `extra_task_id` exists. Remove the "create task from `extra_target_id`" fallback for external callers. (e) Resolve widget tiles by numeric `targetId`; for legacy title-only shortcuts, match only when **exactly one** task equals the title (case-insensitive equality, not `contains`); otherwise show the tile as unlinked.
5. **Data preservation.** Do not change DB or action string constants (already-posted notifications must keep working).
6. **Tests.** `ReminderReceiverSecurityTest`, `ReminderReceiverAsyncTest` (fake `PendingResult`), `WidgetTileResolutionTest` (two overlapping titles), existing P1/QR tests still pass.
7. **Acceptance.** Foreign intents are ignored; every async branch finishes its `PendingResult`; two tasks "Run" and "Run payroll" never cross-toggle; the app still reschedules on reboot.
8. **Regression risks.** Boot completion on direct-boot devices; legacy shortcuts shown as unlinked; ensure widget click `PendingIntent` still delivers extras (mutable flag required).
9. **Git.** Branch `fix/receiver-hardening-goasync`; draft PR.
10. **Stop if** any notification action stops working on an emulator after the manifest split.

### PROMPT R1: Reschedule triggers and exact-alarm flow (REM-02, REM-03, REM-04, REM-05)

1. **Objective.** Keep alarms correct after reboot, clock/timezone change, app update, and permission changes; tell the user when exact alarms are unavailable.
2. **Paths.** `NotificationHelper.kt` (`scheduleAllTaskReminders` ~642, `canScheduleExactAlarms` ~355/540/629), `ReminderBroadcastReceiver.kt` (boot branch ~55–90), `MaintenanceWorker.kt`, `AndroidManifest.xml`, `SettingsScreen.kt`, `UserPreferences.kt`.
3. **Verified problem.** Only `BOOT_COMPLETED` is handled; the boot branch reschedules *today* only; the worker covers today and tomorrow; there is no UI or receiver for the exact-alarm permission.
4. **Steps.** (a) Create `ReminderScheduler.rescheduleAll(horizonDays = 7)` that rebuilds task alarms and today's/tomorrow's medicine doses from the DB, idempotent via the existing request-code scheme. (b) Register a non-exported-where-possible receiver for `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED`, `ACTION_MY_PACKAGE_REPLACED`, `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` that calls it through the B/S1 `runAsync` helper. (c) Make the worker call the same function. (d) Add a Settings row "Precise reminders": state chip (granted/not granted/not needed), button to `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`. (e) Do **not** add `USE_EXACT_ALARM` unless I confirm (see §25 D3).
5. **Data preservation.** No schema change. Only schedule; never cancel alarms of other task IDs.
6. **Tests.** Unit tests for the horizon calculation, Robolectric broadcast tests for each action, manual checklist (below) on a real device.
7. **Acceptance.** After each of: reboot, `adb shell cmd alarm`/timezone change, `adb install -r`, revoking and granting exact alarms, `dumpsys alarm | grep aistudio.lifeos` shows the expected future alarms; the Settings chip reflects the permission.
8. **Regression risks.** Duplicate notifications if request codes differ; battery impact if the horizon is too large (cap at 7 days, ≤ 200 alarms).
9. **Git.** Branch `fix/reminder-reschedule-triggers`; draft PR with the manual `dumpsys` evidence.
10. **Stop if** you need to change the notification channel IDs or request-code formula.

### PROMPT D1: `java.time` replaces `SimpleDateFormat` (DT-01, DT-02)

1. **Objective.** Remove thread-unsafe date formatting without changing outputs.
2. **Paths.** `utils/DateUtils.kt` (lines 13–16 and all methods), every `DateUtils.` caller (209 call sites in 25 files; do not edit callers unless a signature must change).
3. **Verified problem.** Shared `SimpleDateFormat` singletons used across threads; `getDayOfWeek` swallows parse errors as `"UNKNOWN"`; `Locale.US` hard-coded for display formats.
4. **Steps.** (a) Write a **parity test** that runs old and new implementations for every date 2000-01-01…2040-12-31 for `addDays`, `daysBetween`, `getDayOfWeek`, `getDaysInMonth`, `epochDay` round-trip. Keep the old implementation copied into the test as the oracle. (b) Re-implement with `LocalDate`, `DayOfWeek`, `ChronoUnit`, `DateTimeFormatter`; keep method names and return strings; add an optional `Clock` parameter (default `Clock.systemDefaultZone()`). (c) Invalid input: return the same fallback values as today, but log once (no PII). (d) Keep display formats identical for `Locale.US` now; add a `displayLocale` parameter defaulting to US so localisation can come later.
5. **Data preservation.** ISO date strings stored in the DB must stay `yyyy-MM-dd`.
6. **Tests.** Parity test, a multithread stress test (8 threads × 10k calls), DST transition dates, leap years.
7. **Acceptance.** Zero `SimpleDateFormat` in `DateUtils`; parity test passes; all existing tests pass.
8. **Regression risks.** `getDayOfWeek` previously used default-timezone `Calendar`; ensure weekday for an ISO date is zone-independent (it should be). Start-of-week assumptions elsewhere.
9. **Git.** Branch `refactor/dateutils-java-time`; draft PR.
10. **Stop if** the parity test finds differences you cannot explain; report them instead of "fixing" the oracle.

### PROMPT C1: Every-N-weeks recurrence fix (REC-01, REC-02)

1. **Objective.** Make multi-week recurrence align to calendar weeks; verify monthly/yearly edge cases.
2. **Paths.** `domain/recurrence/RecurrenceEngine.kt` (`EVERY_N_WEEKS` 121–122, `CUSTOM`/`WEEKS` 158–159, `matchesMonthDay`, `matchesYearly`, `countOccurrencesUpTo`), `test/.../LifeOSDomainUnitTest.kt` and recurrence tests.
3. **Verified problem.** `weeksDiff = daysDiff / 7` counts from the due date, so Mon 6 Jan fires for a bi-weekly task due Wed 1 Jan with days {MON, FRI}.
4. **Steps.** (a) Add a failing table-driven test: intervals 1–4 × day sets × due weekdays, expected from an independent oracle using `java.time` week-start arithmetic. (b) Compute `weekIndex = (epochDay(target) − epochDay(mondayOf(due))) / 7` and match `weekIndex % interval == 0` plus day-of-week membership. (c) Decide the week start (default Monday; read the user's setting if it exists) and document it. (d) Add tests for day 29/30/31 monthly tasks across all months and Feb 29 yearly tasks; report current behaviour first, then change only with my approval if it differs from "clamp to last day of month". (e) Cache `countOccurrencesUpTo` results per (taskId, rule hash) within a call, or switch to arithmetic counting.
5. **Data preservation.** Do not modify stored tasks. Completed/skipped/rescheduled date lists are untouched.
6. **Tests.** As above; run the existing Batch02 recurrence tests.
7. **Acceptance.** Bi-weekly example no longer fires on 6 Jan; first match is 13 Jan; all previous tests pass or are intentionally updated with explanations in the PR.
8. **Regression risks.** Existing bi-weekly tasks shift by one week for the first cycle. Add a CHANGELOG note; do not silently migrate.
9. **Git.** Branch `fix/recurrence-n-weeks-alignment`; draft PR.
10. **Stop if** the change would alter occurrences already marked complete in test fixtures.

### PROMPT I1: Transactions, then foreign keys (DI-01)  *(two PRs; second needs R3-C1 version coordination)*

**PR 1: transactions only (no schema change)**
1. **Objective.** Make task save/delete atomic.
2. **Paths.** `data/repository/LifeRepository.kt` (`insertTask` 194, `updateTask` 201, `deleteTask` 207, `saveTaskLabels`, `saveTaskReminders`).
3. **Problem.** Multi-step writes without `withTransaction`; hard delete although Trash exists.
4. **Steps.** Wrap the three methods in `database.withTransaction`; add a test that cancels the coroutine between steps and asserts no partial state; audit callers: if a UI path should send the task to Trash, list it in the PR but do not change behaviour.
5–7. No data changes; tests as above; acceptance: partial-failure test passes.
8. Risk: nested transactions (the repository already uses `withTransaction` at 1840, 2621, 2683, 2713; do not nest suspend calls that open their own).
9. Branch `fix/task-writes-transactional`. 10. Stop if a DAO method is not `suspend`-safe inside a transaction.

**PR 2: foreign keys (schema change, **ask me for the target DB version first**)**
1. **Objective.** Enforce relations with FKs and clean orphans.
2. **Paths.** `entities/*.kt`, `AppDatabase.kt`, `RoomMigrationRobolectricUnitTest.kt`, new migration `MIGRATION_10_11` (or the next free number).
3. **Problem.** No `foreignKeys =` anywhere; orphans possible in label links, reminders, doses, usage entries, bills, payments.
4. **Steps.** First write a migration test that seeds orphans in each child table. In the migration: back up the DB file; delete or quarantine orphan rows into a `_orphans_backup` table (do not discard silently); recreate each child table with `FOREIGN KEY ... ON DELETE CASCADE`; run `PRAGMA foreign_key_check`. Turn on `exportSchema` first (R4-P0).
5. **Data preservation.** Orphans move to a backup table, never silently deleted. Rollback path documented.
6. **Tests.** Migrations from v1…v10 to the new version; FK violations rejected; cascade delete verified.
7. **Acceptance.** All migration tests pass; `foreign_key_check` returns nothing.
8. **Risk.** High. Table recreation on large DBs; run on a 50k-row fixture.
9. Branch `feat/room-foreign-keys`. 10. **Stop if** the version number conflicts with Release 3 work.

### PROMPT M1: Premium story (MON-01)  *(decision required first: §25 D2)*

1. **Objective.** Remove the production premium simulator and make messaging match the decision.
2. **Paths.** `ui/screens/gamification/GamificationScreen.kt` (~182), `ui/viewmodel/LifeViewModel.kt` (~1860), `ui/theme/engine/PremiumEntitlement.kt`, `ThemeRegistry.kt`, `ThemeGalleryScreen.kt`, `README.md`.
3. **Problem.** A free "Unlock Premium" button ships; UI says gamification is premium, code says premium is cosmetic.
4. **Steps if D2 = A (everything free).** Mark all themes unlocked; remove the button and the premium banner; keep `isPremium` flags in `ThemeId` but ignore them; leave the DataStore key unread. **If D2 = B (real unlock).** Hide the simulator in release (`BuildConfig.DEBUG`), add a clear unlock entry point stub with no payment SDK, and do not ship until a store-billing design is approved.
5. **Data preservation.** Do not delete the DataStore file; a user who "unlocked" keeps all themes.
6. **Tests.** Theme unlock tests; a test that release builds do not expose the simulator.
7. **Acceptance.** No UI path grants entitlement without the decided mechanism; messaging is consistent.
8. **Risk.** Low. 9. Branch `chore/premium-story`. 10. Stop if D2 is unanswered.

---

## 23. Ready-to-Paste Feature Implementation Prompts

Use the **COMMON RULES** block from §22 above each prompt.

### PROMPT F0: R4-P0 Safety net

1. **Objective.** Make migrations and CI trustworthy before any schema change.
2. **Paths.** `app/build.gradle.kts`, `.github/workflows/android-ci.yml`, `app/src/test/.../RoomMigrationRobolectricUnitTest.kt`, `app/src/androidTest/.../RoomMigrationInstrumentationTest.kt`, `app/lint-baseline.xml`.
3. **Problem.** `exportSchema = false`; migrations tested only v6→v10; CI skips lint and release assembly; actions pinned by tag.
4. **Steps.** (a) Add `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`, set `exportSchema = true`, commit generated `schemas/…/10.json`. (b) For versions 1–9 you cannot regenerate schemas retroactively; build fixture DBs by running each `Migration` against a hand-created v(N) database using SQL from git history, and test v1→v10, v3→v10, v5→v10, v6→v10, v9→v10. (c) CI: add `./gradlew lintDebug assembleRelease` (unsigned) and `./gradlew assembleDebugAndroidTest`; pin actions to commit SHAs; cache Gradle. (d) Add a test that checks `lint-baseline.xml` does not grow (count of issues must not exceed today's).
5. **Data.** No runtime change. 6. **Tests.** Above. 7. **Acceptance.** CI green; `schemas/` committed; migration matrix passes.
8. **Risk.** Fixture reconstruction errors; do not "fix" a migration to make a test pass without reporting it.
9. Branch `chore/safety-net-schema-ci`. 10. Stop if a historical migration cannot be reproduced from git history.

### PROMPT F1: Saved filters (F-05, R4-P7)

1. **Objective.** Let users save task filters as named lists.
2. **Paths.** `ui/screens/tasks/TasksScreen.kt`, `domain/model/TaskModels.kt`, `data/preferences/UserPreferences.kt` (store as JSON in DataStore **or** a new table, see 4a), `LifeViewModel` (or the new `TasksViewModel` if R4-P6 is done).
3. **Problem.** No saved filters (not found in TasksScreen/Calendar; verify first with grep and report).
4. **Steps.** (a) Ask me whether to store in DataStore (no migration) or a new Room table (needs migration and Backup v2 inclusion); default DataStore. (b) Filter model: projects, labels, priorities, due range (today/7 days/overdue/custom), completion state. (c) UI: "Save filter" from the filter sheet, a horizontal chips row of saved filters, rename/delete. (d) Include in Backup v2 (guard test will demand it).
5. **Data.** Additive only. 6. **Tests.** Filter evaluation unit tests; persistence round-trip; Backup coverage. 7. **Acceptance.** A saved filter survives restart and restore, returns the same tasks as the manual filter. 8. **Risk.** Low. 9. Branch `feat/saved-filters`. 10. Stop if it needs a schema change not approved.

### PROMPT F2: Habit strength score and heat-map (F-07)

1. **Objective.** Add a forgiving strength score and a calendar heat-map next to existing streaks.
2. **Paths.** `domain/model/HabitModels.kt` (`currentStreak`, `longestStreak` at lines 12–13), `ui/screens/habits/`, `ui/screens/analytics/AnalyticsScreen.kt`, new `domain/habits/HabitStrength.kt`.
3. **Problem.** Streak-only feedback; one missed day resets progress (verify in the current streak code; report what you find).
4. **Steps.** Implement an exponentially smoothed score (document the formula; e.g. each completion moves the score toward 1, each scheduled miss decays it, using the habit's schedule so non-scheduled days do not penalise). Pure function + unit tests. Show the score and a 12-week heat-map; keep streak numbers visible. No new tables: compute from existing completion data.
5. **Data.** Read-only. 6. **Tests.** Known sequences give expected scores; skipped/paused days. 7. **Acceptance.** Score never decreases on non-scheduled days; heat-map matches stored completions. 8. **Risk.** Low; confirm no performance regression on 3 years of data. 9. Branch `feat/habit-strength`. 10. Stop if completion data is only available through JSON blobs that need migration.

### PROMPT F3: Reminder health screen (F-04) *(after PROMPT R1)*

1. **Objective.** One screen that shows why reminders might not fire.
2. **Paths.** `ui/screens/settings/` (new `ReminderHealthScreen.kt`), `NotificationHelper`, `ReminderScheduler` (from R1).
3. **Problem.** REM-02/03/04: failures are silent.
4. **Steps.** Rows: notification permission, notification channels enabled, exact-alarm status with a fix button, battery optimisation status with a link to system settings, next 5 scheduled alarms (from the DB + scheduler, not from `AlarmManager`), a "send test reminder in 1 minute" button.
5. **Data.** None. 6. **Tests.** State mapping unit tests; manual device checklist. 7. **Acceptance.** Each failure state shows a specific, actionable message. 8. **Risk.** Low. 9. Branch `feat/reminder-health`. 10. Stop if reading scheduled alarms requires a new table.

### PROMPT F4: Accessibility pass (F-10, IMP-03)

1. **Objective.** Make the five most-used screens usable with TalkBack and large text.
2. **Paths.** Dashboard, Tasks, Calendar, Habits, Honeycomb grid; `ui/components/CommonComponents.kt`; 138 sites with `contentDescription = null` (list them with `grep -rn "contentDescription = null"`).
3. **Problem.** 138 of 316 descriptions are null; 6 `SmallSp` lint hits; touch target sizes unverified.
4. **Steps.** Classify each null as decorative or interactive; give interactive ones a localisable description via `stringResource`; ensure ≥48dp targets (`minimumInteractiveComponentSize`); fix `SmallSp`; add Compose semantics tests for primary actions; test at 200% font scale.
5. **Data.** None. 6. **Tests.** Compose UI tests with `assertContentDescriptionEquals`; screenshot at large font. 7. **Acceptance.** Zero interactive icons with null descriptions on the five screens; no clipped text at 200%. 8. **Risk.** Low. 9. Branch `feat/a11y-pass-1`. 10. Stop if a component is shared with the theme engine in a way that changes layout.

### PROMPT F5: Repository split, step 1 (RFR-01, R4-P6)

1. **Objective.** Extract `TaskRepository` from `LifeRepository` without behaviour change.
2. **Paths.** `data/repository/LifeRepository.kt` (task functions around lines 183–420), call sites in `LifeViewModel`, receivers, workers, widgets.
3. **Problem.** 2,731-line class, 148 functions.
4. **Steps.** Define `TaskRepository` (interface + implementation) containing task, label, reminder and recurrence-related functions; have `LifeRepository` delegate to it; update call sites gradually; add an `AppContainer` (RFR-03) so receivers/workers stop constructing repositories. One domain per PR.
5. **Data.** None. 6. **Tests.** Existing suite must pass unchanged. 7. **Acceptance.** `LifeRepository` shrinks by the extracted lines; no public behaviour change. 8. **Risk.** Hidden coupling to preferences/entitlement; stop and report. 9. Branch `refactor/task-repository`. 10. Stop if more than one domain must move together.

### PROMPT F6: Localisation scaffolding (F-09)

1. **Objective.** Make the app translatable without translating it yet.
2. **Paths.** `res/values/strings.xml` (4 lines), `app/lint-baseline.xml` (30 `HardcodedText`), `DateUtils` (`Locale.US`), Settings/Tasks/Reminders screens first.
3. **Problem.** All text hard-coded; cannot add languages.
4. **Steps.** Move user-visible text of Settings, Tasks, Reminders and Notifications to `strings.xml`; use plurals where counts appear; make `DateUtils` display formats use the app locale (after PROMPT D1); add a pseudo-locale test (`en-XA`) to catch clipping; one pilot language only if I name it.
5. **Data.** None. 6. **Tests.** Lint `HardcodedText` count must drop; screenshots in `en-XA`. 7. **Acceptance.** Those four areas have zero hard-coded user text. 8. **Risk.** Merge conflicts in large screens; do one screen per commit. 9. Branch `feat/i18n-scaffolding`. 10. Stop if a string is built by concatenation that cannot be localised without a design change.

---

## 24. Testing and Quality Strategy

| Layer | Today (verified) | Target for Release 4 |
|---|---|---|
| Unit tests | ≈184 `@Test` in 17 files, grouped by bug-batch names | Group new tests by feature; add table-driven suites for recurrence, backup, dates |
| Migration | 1 Robolectric + 1 instrumented test, v6→v10 | `schemas/` committed; v1/3/5/6/9→current; orphan and FK checks |
| Receivers/widgets | Some P1/QR unit tests | Fake `PendingResult` tests; external-intent rejection; widget tile resolution |
| Instrumented/UI | 2 example/migration tests | A small Compose suite for backup, task create/complete, reminder health |
| Static analysis | Lint is a local gate with a baseline of ≈100 entries | CI runs `lintRelease`; baseline may not grow |
| CI | `testDebugUnitTest` + `assembleDebug` | Add `lintRelease`, `assembleRelease`, `assembleDebugAndroidTest`; SHA-pinned actions; dependency update bot (review PRs manually) |
| Manual device matrix | Not documented | API 24, 29, 34, current; one Samsung, one Xiaomi/Oppo-class device for background limits. Checklist: reboot, timezone, update-in-place, exact alarm revoke, Doze |
| Release gate | Readiness report markdown | One `docs/RELEASE_CHECKLIST.md`, run before any tag |

**Definition of done for any Release 4 PR:** tests added or updated, `lintDebug` not worse than baseline, migration test passes if the schema is touched, docs updated where behaviour changed, no new permission, no new network dependency.

---

## 25. Risks, Trade-offs and Open Decisions

### 25.1 Risks

| Risk | Mitigation |
|---|---|
| FK migration corrupts or loses rows | Backup file first, orphan quarantine table, 50k-row fixture, staged rollout |
| Recurrence fix shifts existing bi-weekly tasks | CHANGELOG note, no silent data rewrite, one-cycle explanation in release notes |
| Splitting the god classes causes regressions | Strangler pattern, one domain per PR, existing tests must stay green |
| Manifest split breaks queued notification actions | Keep action strings and request codes; emulator checklist |
| Disabling Auto Backup removes a safety net for non-exporting users | Ship SAF backup first; one-time in-app notice |
| Scope creep from F-items | Only P0–P5 are mandatory; P6/P7 can slip |
| Single-developer capacity | §21 gives pessimistic totals; ship R4.0 before R4.1 |

### 25.2 Trade-offs

- **Completeness vs file size of backups:** exporting every table grows the JSON; stream and compress later if needed.
- **Hard delete vs Trash:** a safer default (Trash) costs storage and a purge rule.
- **Premium story:** a paywall conflicts with the privacy-first brand; cosmetic-only monetisation is possible but needs real billing and review.
- **Exact alarms:** more precise reminders vs a special permission and Play policy review.

### 25.3 Open decisions (need the owner)

| ID | Decision | Default recommendation |
|---|---|---|
| **D1** | Platform Auto Backup: off, or on with explicit rules? | **Off**, after SAF backup ships |
| **D2** | Premium: A) everything free, or B) a real optional unlock? | **A** unless a monetisation plan exists |
| **D3** | Request `USE_EXACT_ALARM` (medication/alarms) or only `SCHEDULE_EXACT_ALARM` with a settings prompt? | Settings prompt; revisit after a Play policy check |
| **D4** | Backup merge semantics: default REPLACE_ALL or MERGE? | REPLACE_ALL with confirmation |
| **D5** | Week start for multi-week recurrence: Monday, Sunday, or locale/setting? | Follow an existing setting; else Monday |
| **D6** | Should deleted tasks go to Trash instead of hard delete? | Yes, with a 30-day purge |
| **D7** | Release 3 schema/version plan, and which Release 3 items touch reminders, dates or backups? | Needed before R4-P4 |
| **D8** | Which languages, if any, after scaffolding? | Ask users first |

### 25.4 Research gaps to close

Device pass for UX (onboarding, empty states, dashboard, settings), research on Microsoft To Do, Google Tasks, Notion, Any.do and Structured, and official-source verification of Todoist and TickTick plan limits and prices.

---

## 26. Final Recommendations

**Top 10 priorities**

1. **BK-01**: back up all 28 tables, with a guard test.
2. **BK-02**: atomic, validated, versioned import.
3. **SEC-01**: make the reminder receiver private; split boot receiver.
4. **REM-01**: `goAsync()` for every receiver coroutine.
5. **DT-01**: `java.time` instead of shared `SimpleDateFormat`.
6. **REC-01**: week-aligned multi-week recurrence.
7. **DI-01**: transactions now, foreign keys after schema export.
8. **REM-02/03/04**: exact-alarm flow and reschedule on time/timezone/update.
9. **PRV-01 + BK-03**: decide Auto Backup; SAF backup files.
10. **MON-01**: resolve the free "premium" button.

**Release 4 phases:** P0 Safety net → P1 Backup v2 → P2 Reminders and receiver security → P3 Dates and recurrence → P4 Data integrity → P5 Decisions and hygiene → P6 Architecture split ∥ P7 Selected features.

**Counts:** 15 confirmed defects (§8.1) · 9 suspected risks (§8.2) · 7 refactor items (§14) · 9 removal candidates (§15) · 14 feature proposals (§16) · 8 open decisions (§25.3).

**Delivering this file.** This environment cannot push to your repository. From a clone of `main`:

```bash
git checkout -b docs/claude-release4-research
mkdir -p docs
cp "/path/to/claude review.md" "docs/claude review.md"
git add "docs/claude review.md"
git commit -m "docs: Release 4 research, audit and master plan"
git push -u origin docs/claude-release4-research
gh pr create --draft --title "docs: Release 4 research and master plan" --body "Research only. No code changes."
```

I have no commit SHA or PR link to report. Both depend on you running the commands above.

---

## 27. Research Sources and References

All accessed **2026-11-01**. Repository evidence is from commit `1d4f43f` of `https://github.com/Kadhiravan-K/LifeOS` (tag `v1.1.0`).

| Used for | Source | Type |
|---|---|---|
| Loop Habit Tracker features, privacy, export | https://loophabits.org · https://github.com/iSoron/uhabits | Primary (project site, README) |
| Tasks.org features, licence, sync, pricing text | https://apt.izzysoft.de/ftp/repo/fdroid/index/apk/org.tasks?repo=main · https://cloudflare.f-droid.org/es/packages/org.tasks/ · https://www.apkmirror.com/?p=2101108 | Store/repo listings (price text is from an older listing: unverified) |
| TickTick Premium price | https://apps.apple.com/app/tick-tick/id626144601 | Store listing |
| TickTick free/paid boundary (conflicting) | https://lifestack.ai/blog/ticktick-pricing · https://crm.org/news/ticktick-review · https://thedigitalprojectmanager.com/tools/ticktick-pricing/ · https://www.any.do/blog/?p=8238 | Aggregators/blogs; the Any.do page is a competitor's marketing blog |
| Todoist tiers and quoted prices (conflicting: $4, $5, $7) | https://routine.co/blog/posts/todoist-pricing · https://costbench.com/software/project-management/todoist/free-plan/ · https://subger.com/en/ph/service/todoist-nordic · https://2sync.com/blog/ticktick-vs-todoist | Aggregators; **no price asserted in this document** |
| Habitica features and complaints | https://www.alternativeto.net/software/habitica/about/ · https://unstar.app/blog/habitica-streaks-productive-way-of-life-habit-bull-habit-tracker-apps-ranked-2026 · https://www.helloleads.io/blog/?p=3909 | Aggregators/review blogs; pricing quoted there is unverified |

**Not sourced:** Microsoft To Do, Google Tasks, Notion, Any.do (beyond the one blog), Structured. Cells for them are absent from §7 rather than guessed.
# the user after use this app i need a survar a feetback colllection 