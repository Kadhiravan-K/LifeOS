package com.example.domain.legal

/**
 * Domain definitions, version constants, and offline-readable legal documents
 * for LifeOS (Terms of Service & Privacy Policy).
 *
 * LifeOS is an offline-first, privacy-first personal productivity system.
 */
object LegalDocuments {

    const val CURRENT_TERMS_VERSION = "1.0.0"
    const val CURRENT_PRIVACY_VERSION = "1.0.0"

    const val OWNER_REVIEW_DISCLAIMER = """
DISCLAIMER / DRAFT REVIEW NOTICE:
These Terms & Conditions and Privacy Policy are comprehensive draft templates prepared for LifeOS. The repository owner and maintainers should review and adapt them with formal legal counsel prior to commercial distribution or regulatory filing.
"""

    const val TERMS_OF_SERVICE_TITLE = "LifeOS Terms & Conditions"
    const val PRIVACY_POLICY_TITLE = "LifeOS Privacy Policy"

    val TERMS_OF_SERVICE_CONTENT = """
$OWNER_REVIEW_DISCLAIMER

Effective Date: October 24, 2026
Version: $CURRENT_TERMS_VERSION

1. ACCEPTANCE OF TERMS
Welcome to LifeOS. By downloading, installing, launching, accessing, or using the LifeOS mobile application ("Application", "Service", or "Software"), you agree to be bound by these Terms & Conditions ("Terms"). If you do not agree to these Terms, you must not use or access LifeOS and must immediately uninstall the Application.

2. OFFLINE-FIRST ARCHITECTURE & NO REMOTE ACCOUNT
LifeOS is engineered as an offline-first, local-only productivity environment:
- No remote user accounts, cloud authentication, or centralized servers are operated by LifeOS.
- All tasks, habits, projects, notes, medication schedules, hydration records, and personal preferences remain exclusively stored on your physical device.
- You do not need to provide email addresses, passwords, phone numbers, or external credentials to use the Application.

3. USER RESPONSIBILITY FOR DATA & BACKUPS
Because LifeOS operates locally on your hardware:
- You retain 100% ownership and full custodial control over all data created within LifeOS.
- You are solely responsible for creating and maintaining external backups of your data using the built-in Backup & Restore facility.
- LifeOS developers and contributors do not have access to your device, cannot recover lost data, cannot decrypt user-encrypted archives if passwords are forgotten, and cannot restore deleted items once removed from local storage.
- You are responsible for maintaining the physical and digital security of your device, including device screen locks, biometrics, and secure handling of exported backup files.

4. HEALTH, MEDICATION & EMERGENCY DISCLAIMER
CRITICAL SAFETY NOTICE:
- LifeOS provides productivity tools including task scheduling, habit tracking, and Medicine Reminders.
- LifeOS is NOT a medical device, diagnostic system, emergency alert system, or clinical decision support tool.
- LifeOS does NOT provide medical advice, diagnosis, treatment, or clinical supervision. Always consult qualified healthcare professionals regarding medical conditions, medications, dosages, and treatments.
- Notification delivery and smart alarms rely upon the underlying Android operating system, vendor battery optimization policies (Doze mode, App Standby), OEM background execution limits, and hardware state. NEVER rely upon LifeOS as the primary or sole alert mechanism for critical, life-sustaining, or time-sensitive medical care.

5. PERMITTED USE & INTELLECTUAL PROPERTY
- LifeOS is licensed as open-source software under the terms of the MIT License, unless otherwise specified in repository licensing headers.
- You may use LifeOS for personal, non-commercial, or internal productivity purposes in accordance with applicable local, national, and international laws.
- You agree not to use LifeOS for unlawful, fraudulent, or hazardous activities where software malfunction could lead to injury, loss of life, or environmental damage.

6. DISCLAIMER OF WARRANTIES
TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, LIFEOS IS PROVIDED ON AN "AS IS" AND "AS AVAILABLE" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE, INCLUDING BUT NOT LIMITED TO WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE, ACCURACY, FREEDOM FROM DEFECTS, OR UNINTERRUPTED OPERATION.

7. LIMITATION OF LIABILITY
TO THE MAXIMUM EXTENT PERMITTED BY LAW, IN NO EVENT SHALL THE DEVELOPERS, CONTRIBUTORS, REPOSITORY MAINTAINERS, OR COPYRIGHT HOLDERS OF LIFEOS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE, OR CONSEQUENTIAL DAMAGES (INCLUDING LOSS OF DATA, DEVICE DAMAGE, LOSS OF PROFITS, OR INTERRUPTION OF BUSINESS) ARISING OUT OF OR IN CONNECTION WITH THE USE OR INABILITY TO USE THE APPLICATION, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

8. REVISIONS & VERSION UPDATES
We may revise these Terms from time to time. When significant updates occur, the version number will be incremented (e.g., from 1.0.0 to a higher version), and you will be prompted to review and accept the updated Terms before continuing use. Continued use of LifeOS following notice of updated terms signifies your binding acceptance.

9. GOVERNING LAW & SEVERABILITY
If any provision of these Terms is found to be invalid, unenforceable, or illegal under applicable law, such provision shall be severed and the remaining provisions shall continue in full force and effect.
""".trimIndent()

    val PRIVACY_POLICY_CONTENT = """
$OWNER_REVIEW_DISCLAIMER

Effective Date: October 24, 2026
Version: $CURRENT_PRIVACY_VERSION

1. PRIVACY COMMITMENT & CORE PRINCIPLES
LifeOS is designed from the ground up around user sovereignty, digital privacy, and data minimalism:
- Zero Remote Telemetry: We do not collect, transmit, track, or analyze your activity.
- Zero Ad Networks: LifeOS contains no advertisements, tracking beacons, or cross-app identifiers.
- Zero Cloud Data Brokers: We do not sell, rent, monetize, or disclose your personal data to any third party.
- 100% On-Device Processing: Everything you enter stays on your local device.

2. WHAT DATA IS STORED ON YOUR DEVICE
LifeOS creates local records in your device's private sandboxed SQLite database (Room) and DataStore preferences:
- Task items, descriptions, due dates, and recurrence rules.
- Habits, streaks, challenge progress, and water intake logs.
- Medication names, dosage instructions, times, and adherence logs.
- Gamification ranks, level XP, unlocked achievements, and avatar states.
- Visual preferences, custom color presets, quiet hours, and notification configurations.
- Legal consent status, accepted document versions, and acceptance timestamps.

3. DEVICE PERMISSIONS & HOW THEY ARE USED
LifeOS requests minimal Android permissions solely for essential local functionality:
- POST_NOTIFICATIONS (Android 13+): Used exclusively to present local task reminders, medicine alerts, and quiet-hours notifications on your device. No notification payloads are sent over any network.
- SCHEDULE_EXACT_ALARM: Used to trigger accurate medicine alerts and P1 priority smart alarms at your specified times.
- VIBRATE: Used to provide tactile feedback for timer completions, alarms, and task toggles.
- Storage / Document Picker: Invoked via standard Android Storage Access Framework (SAF) only when you explicitly initiate a database backup export or restore.

4. NETWORK ACCESS
LifeOS does NOT request or require Android INTERNET permission for its core productivity functions. The application cannot communicate with remote web services, upload analytics, or transmit crash logs without your knowledge.

5. DATA RETENTION & USER CONTROL
- You have absolute control over your information.
- Editing or deleting tasks, habits, medications, or logs immediately updates or purges the corresponding local records.
- Uninstalling LifeOS permanently deletes all application sandboxed storage and local databases from your device (unless you have exported a manual backup to external storage).
- You may export your entire database at any time using the Settings -> Data & Storage -> Backup feature.

6. CHILDREN'S PRIVACY
Because LifeOS operates entirely offline and collects zero personal data, it does not knowingly collect or solicit personal information from children or any individuals.

7. CHANGES TO THIS PRIVACY POLICY
Any modifications to this Privacy Policy will be reflected in an incremented version number. Material policy revisions will be presented in-app for your explicit review and acknowledgment.

8. CONTACT & OPEN SOURCE INQUIRIES
For questions regarding LifeOS privacy design or to inspect the source code, please visit the open-source repository at:
https://github.com/Kadhiravan-K/LifeOS
""".trimIndent()
}

/**
 * Pure helper for validating whether legal consent requirements are fully met.
 */
object LegalConsentManager {

    /**
     * Returns true if both the current Terms and Privacy versions have been explicitly accepted.
     */
    fun isConsentComplete(
        acceptedTermsVersion: String?,
        acceptedPrivacyVersion: String?,
        targetTermsVersion: String = LegalDocuments.CURRENT_TERMS_VERSION,
        targetPrivacyVersion: String = LegalDocuments.CURRENT_PRIVACY_VERSION
    ): Boolean {
        if (acceptedTermsVersion.isNullOrBlank() || acceptedPrivacyVersion.isNullOrBlank()) {
            return false
        }
        return acceptedTermsVersion == targetTermsVersion && acceptedPrivacyVersion == targetPrivacyVersion
    }

    /**
     * Determines whether an updated version requires renewed consent.
     */
    fun requiresReconsent(
        acceptedTermsVersion: String?,
        acceptedPrivacyVersion: String?,
        newTermsVersion: String = LegalDocuments.CURRENT_TERMS_VERSION,
        newPrivacyVersion: String = LegalDocuments.CURRENT_PRIVACY_VERSION
    ): Boolean {
        return !isConsentComplete(
            acceptedTermsVersion = acceptedTermsVersion,
            acceptedPrivacyVersion = acceptedPrivacyVersion,
            targetTermsVersion = newTermsVersion,
            targetPrivacyVersion = newPrivacyVersion
        )
    }
}
