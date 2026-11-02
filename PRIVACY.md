# Privacy Policy

**Effective Date:** November 2, 2026  
**Application:** LifeOS (Android)  
**Developer:** Kadhiravan  
**Repository:** https://github.com/Kadhiravan-K/LifeOS  

---

## Executive Summary

LifeOS is an offline-first, local-first personal productivity and execution operating system designed to give users complete sovereignty over their data.

- **Zero Remote Tracking**: LifeOS does not collect, log, or transmit personal productivity information (tasks, habits, schedules, notes, trackers, or health logs).
- **Zero Advertising or Analytics SDKs**: There are no third-party tracking frameworks, Firebase analytics, advertising identifiers (AAID), or diagnostic beacons integrated into the application.
- **On-Device Data Storage**: All user data resides strictly within local SQLite storage on your physical device.
- **Transparent Google Play Billing**: An optional monthly Premium subscription is available exclusively through the Google Play Billing Library. Billing transactions, subscription renewals, and receipt validation are processed directly by Google Play under Google's Privacy Policy.

---

## Detailed Privacy Disclosures

### 1. Network Connectivity and Technical Scope
- **Merged Manifest Permissions**: While the application source code does not declare standalone network endpoints, library dependencies (specifically the official Google Play Billing Library and Android Jetpack WorkManager) merge standard connectivity permissions (`android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE`, and `com.android.vending.BILLING`) into the final application package.
- **Scope of Network Use**: Network transmission is restricted exclusively to the official Google Play Billing Library interacting with the Google Play Store client (`com.android.vending`) and Google Play infrastructure for the sole purpose of:
  1. Querying current regional subscription pricing and 7-day trial offer details.
  2. Initiating and completing subscription purchase flows.
  3. Verifying and restoring subscription purchases upon app startup or user request.
- **No Proprietary Server Infrastructure**: LifeOS operates zero backend web services, zero cloud databases, and zero user-tracking APIs. Your personal productivity content never traverses the internet.

### 2. On-Device Data Storage
All information entered into LifeOS is stored exclusively on your local device:
- **Room SQLite Database (`lifeos_database` v19)**: Contains user-created entities including tasks, subtasks, recurrence patterns, project workspaces, habits, daily habit logs, custom reminder schedules, medication profiles, usage trackers, gamification profiles, and custom rewards.
- **Jetpack DataStore Preferences**: Stores local application configuration such as active theme IDs, appearance geometry, vibration/audio feedback settings, and notification channel configurations.

### 3. User Accounts and Identity
- LifeOS requires **no user account registration, login credentials, email address, or phone number**.
- No profile is maintained on any external server.
- Subscriptions are tied exclusively to the user's authenticated Google Play Store account on the Android device.

### 4. Backups and Data Sovereignty
- LifeOS provides user-directed, full database backup and export via the Android Storage Access Framework (SAF).
- Backup files (JSON v1 and v2 formats) are written directly to local file system directories or external storage locations explicitly selected by the user.
- LifeOS never automatically uploads, copies, or synchronizes backup files to any remote cloud service.

### 5. Local Notifications and Alarms
- Reminder alerts and medication schedules are scheduled on-device using Android's native `AlarmManager` (with `SCHEDULE_EXACT_ALARM`) and delivered via `NotificationManager`.
- Reminders do not rely on remote push notifications, Firebase Cloud Messaging (FCM), or webhooks.

### 6. Subscriptions, Google Play Billing & Trial Transparency
LifeOS offers an optional monthly Premium subscription for cosmetic visual environments (themes) and bonus perks:
- **Commercial Model**: Monthly recurring subscription only (`P1M`). Annual plans, lifetime purchases, and one-time unlock fees are not offered.
- **7-Day Free Trial**: New eligible subscribers receive a 7-day introductory free trial at no initial charge.
- **Billing Commences Post-Trial**: Unless cancelled at least 24 hours before the 7-day trial ends, Google Play will automatically charge the localized monthly subscription fee to the user's selected payment method.
- **Automatic Renewal**: Subscriptions automatically renew monthly until cancelled by the user.
- **Cancellation Anytime**: Users may cancel their subscription at any time directly through Google Play (**Google Play Store → Account → Payments & Subscriptions → Subscriptions**). A direct shortcut to this management screen is provided inside LifeOS settings and purchase dialogs.
- **Entitlement on Cancellation**: If cancelled, Premium benefits remain active until the conclusion of the current prepaid billing period.
- **Non-Destructive Downgrade Policy**: When a subscription ends or expires, the visual theme smoothly reverts to the default Genesis theme. **No user-created data (tasks, habits, projects, notes, reminders, or backups) will ever be deleted, locked, or restricted upon expiration.** All core productivity tools remain 100% free and functional.

### 7. Google Play Data Safety Declaration Reference
For Google Play Console Data Safety review:
| Data Category | Data Type | Collected or Shared? | Purpose | Recipient |
|---|---|---|---|---|
| **Personal Info** | Name, Email, Phone, User ID | **Not Collected** | N/A | None |
| **Financial Info** | Credit card, payment details, purchase history | **Processed by Google Play** | Payment processing & subscription management | Google LLC (via Play Billing) |
| **Messages / Content** | Tasks, notes, habits, routines | **Not Collected** | Stored locally on-device | None |
| **Location** | Precise / Approximate Location | **Not Collected** | N/A | None |
| **Health & Fitness** | Medication schedules, habit logs | **Not Collected** | Stored locally on-device | None |
| **App Activity** | Page views, clicks, interactions | **Not Collected** | N/A | None |
| **Identifiers** | Device ID, Advertising ID (AAID) | **Not Collected** | N/A | None |
| **Diagnostics** | Crash logs, performance telemetry | **Not Collected** | N/A | None |

### 8. Children's Privacy (COPPA Compliance)
Because LifeOS collects no personal data whatsoever from any user, it fully complies with the Children's Online Privacy Protection Act (COPPA), GDPR-K, and equivalent international standards.

---

## Contact Information

For inquiries, vulnerability disclosures, or questions regarding this Privacy Policy or LifeOS technical data practices, please contact:
- **Developer:** Kadhiravan
- **Support / Project Issues:** https://github.com/Kadhiravan-K/LifeOS/issues
- *(Owner Note: Provide a designated commercial developer contact email address in Google Play Console prior to production release).*
