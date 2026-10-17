# Privacy Policy

**Effective Date:** October 18, 2026  
**Developer:** Kadhiravan

## The Short Version

**LifeOS collects ZERO personal information. None. Never.**

There are no analytics, no ads, no trackers, no servers, and no third-party SDKs. The app does not possess network capability and cannot transmit data outside your device.

---

## Detailed Privacy Disclosures

### 1. No Network Transmission
LifeOS does **not** declare the `android.permission.INTERNET` permission in its Android Manifest. The Android OS prevents the app from opening network sockets or transmitting any data over Wi-Fi or mobile data.

### 2. On-Device Data Storage
All data entered into LifeOS—including tasks, schedules, calendar events, habits, hydration records, challenges, and user preferences—is stored exclusively on your device using:
- **Room SQLite Database** (`lifeos_database`)
- **Jetpack DataStore Preferences**

### 3. No User Accounts or Identifiers
You do not need to register an account, provide an email address, or link any external identity to use LifeOS. No advertising IDs (AAID) or device fingerprints are generated or recorded.

### 4. Backups and Exporting
LifeOS allows you to manually export your data as a JSON file. This backup file is stored directly to a local destination or folder chosen by you via the Android Storage Access Framework (SAF). LifeOS never copies or uploads this backup to any cloud storage service.

### 5. Notifications & Alarms
LifeOS uses the device's native `AlarmManager` and `NotificationManager` to deliver local reminders. These reminders are processed entirely on-device and do not pass through Firebase Cloud Messaging (FCM) or any push notification servers.

### 6. Children's Privacy
Because LifeOS collects no personal information whatsoever, it complies with the Children's Online Privacy Protection Act (COPPA) and similar global data protection regulations.

---

## Contact

For questions regarding this privacy policy or LifeOS data practices, please contact the developer via the official GitHub repository.
