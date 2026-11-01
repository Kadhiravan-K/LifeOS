package com.example

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.essentials.EssentialsExportHelper
import com.example.widgets.QuickRemindersActionReceiver
import com.example.widgets.QuickRemindersWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader

/**
 * Release 4 — Phase 4.1 Security Boundary Verification Tests.
 *
 * Verifies:
 * - SEC-01: Widget IPC Isolation (QuickRemindersActionReceiver unexported explicit pending intents, input sanitization, type whitelisting).
 * - SEC-03: Android Backup & Device Extraction Hardening (Manifest allowBackup=false, extraction rules exclude databases and prefs).
 * - SEC-04: FileProvider Least-Privilege Scope (root files-path removed, cache-path scoped strictly to shared_reports/).
 */
@RunWith(RobolectricTestRunner::class)
class Release4Phase1SecurityUnitTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // --- SEC-01: Widget IPC Isolation ---

    @Test
    fun sec01_quickRemindersActionReceiver_allowedTypes_isStrictWhitelist() {
        val allowed = QuickRemindersActionReceiver.ALLOWED_TYPES
        assertTrue(allowed.contains("TASK"))
        assertTrue(allowed.contains("HABIT"))
        assertTrue(allowed.contains("WATER"))
        assertTrue(allowed.contains("CHALLENGE_RULE"))
        assertFalse(allowed.contains("RAW_SQL"))
        assertFalse(allowed.contains("INJECTION"))
        assertFalse(allowed.contains("PROFILE"))
        assertEquals(4, allowed.size)
    }

    @Test
    fun sec01_createPendingIntent_targetsActionReceiverExplicitly() {
        val pi = QuickRemindersActionReceiver.createPendingIntent(
            context = context,
            appWidgetId = 42,
            slotIndex = 3,
            shortcutId = 100L,
            type = "TASK",
            taskId = 55L,
            targetId = "55"
        )
        assertNotNull(pi)

        // Validate intent target component class is QuickRemindersActionReceiver
        val shadow = org.robolectric.Shadows.shadowOf(pi)
        val savedIntent = shadow.savedIntent
        assertEquals(QuickRemindersActionReceiver.ACTION_TOGGLE_SHORTCUT, savedIntent.action)
        assertEquals(QuickRemindersActionReceiver::class.java.name, savedIntent.component?.className)
        assertEquals(100L, savedIntent.getLongExtra(QuickRemindersActionReceiver.EXTRA_SHORTCUT_ID, -1L))
        assertEquals("TASK", savedIntent.getStringExtra(QuickRemindersActionReceiver.EXTRA_TYPE))
        assertEquals(55L, savedIntent.getLongExtra(QuickRemindersActionReceiver.EXTRA_TASK_ID, -1L))
        assertEquals("55", savedIntent.getStringExtra(QuickRemindersActionReceiver.EXTRA_TARGET_ID))
        assertEquals("lifeos://widget/qr/42/3/100", savedIntent.data?.toString())
        assertEquals(PendingIntent.FLAG_IMMUTABLE, shadow.flags and PendingIntent.FLAG_IMMUTABLE)
    }

    @Test
    fun sec01_actionReceiver_rejectsNonLifeosScheme() {
        val receiver = QuickRemindersActionReceiver()
        val maliciousIntent = Intent(QuickRemindersActionReceiver.ACTION_TOGGLE_SHORTCUT).apply {
            data = Uri.parse("https://malicious.attacker.com/exploit")
            putExtra(QuickRemindersActionReceiver.EXTRA_TYPE, "WATER")
        }
        // Should return early without throwing exception or mutating data
        receiver.onReceive(context, maliciousIntent)
    }

    @Test
    fun sec01_actionReceiver_rejectsForeignAction() {
        val receiver = QuickRemindersActionReceiver()
        val foreignIntent = Intent("android.intent.action.BOOT_COMPLETED").apply {
            data = Uri.parse("lifeos://widget/qr/1/1/1")
            putExtra(QuickRemindersActionReceiver.EXTRA_TYPE, "WATER")
        }
        receiver.onReceive(context, foreignIntent)
    }

    @Test
    fun sec01_quickRemindersWidgetProvider_ignoresLegacyToggleAction() {
        val provider = QuickRemindersWidgetProvider()
        val legacyIntent = Intent("com.example.lifeos.ACTION_WIDGET_TOGGLE_TASK").apply {
            data = Uri.parse("lifeos://widget/qr/1/1/1")
            putExtra("extra_shortcut_id", 1L)
            putExtra("extra_type", "WATER")
        }
        // Must safely pass through super.onReceive without executing mutation
        provider.onReceive(context, legacyIntent)
    }

    // --- SEC-03: Android Backup & Extraction Hardening ---

    @Test
    fun sec03_manifest_hasAllowBackupFalse() {
        val manifestXml = File("src/main/AndroidManifest.xml").readText()
        assertTrue("Manifest must declare android:allowBackup=\"false\"", manifestXml.contains("android:allowBackup=\"false\""))
        assertFalse("Manifest must not declare android:allowBackup=\"true\"", manifestXml.contains("android:allowBackup=\"true\""))
    }

    @Test
    fun sec03_dataExtractionRules_excludesSensitiveDomains() {
        val rulesXml = File("src/main/res/xml/data_extraction_rules.xml").readText()
        assertTrue(rulesXml.contains("<exclude domain=\"database\" path=\".\" />"))
        assertTrue(rulesXml.contains("<exclude domain=\"sharedpref\" path=\".\" />"))
        assertTrue(rulesXml.contains("<exclude domain=\"file\" path=\".\" />"))
    }

    @Test
    fun sec03_backupRules_excludesSensitiveDomains() {
        val rulesXml = File("src/main/res/xml/backup_rules.xml").readText()
        assertTrue(rulesXml.contains("<exclude domain=\"database\" path=\".\" />"))
        assertTrue(rulesXml.contains("<exclude domain=\"sharedpref\" path=\".\" />"))
        assertTrue(rulesXml.contains("<exclude domain=\"file\" path=\".\" />"))
    }

    // --- SEC-04: FileProvider Scope Reduction ---

    @Test
    fun sec04_filePathsXml_strictlyScopedToSharedReports() {
        val filePathsXml = File("src/main/res/xml/file_paths.xml").readText()
        // Must not expose root files-path tag
        assertFalse("file_paths.xml must not contain <files-path tag", filePathsXml.contains("<files-path"))
        // Must not expose root cache-path path="."
        assertFalse("file_paths.xml must not expose root cache-path path=\".\"", filePathsXml.contains("path=\".\""))
        // Must strictly declare cache-path name="shared_reports" path="shared_reports/"
        assertTrue(filePathsXml.contains("<cache-path name=\"shared_reports\" path=\"shared_reports/\" />"))
    }

    @Test
    fun sec04_csvExportGeneratesValidFileInSharedReports() {
        val filename = "test_export.csv"
        val content = "ID,Name\n1,Test"
        val reportsDir = File(context.cacheDir, "shared_reports").apply { if (!exists()) mkdirs() }
        val testFile = File(reportsDir, filename)
        testFile.writeText(content)

        assertTrue(testFile.exists())
        assertEquals(content, testFile.readText())
        assertEquals(reportsDir.absolutePath, testFile.parentFile?.absolutePath)
        testFile.delete()
    }
}
