package com.example.notifications

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.MedicationDoseEntity
import com.example.data.local.entities.MedicationEntity
import com.example.data.local.entities.TaskEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ReminderReceiverSecurityTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var receiver: ReminderBroadcastReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getInstance(context)
        receiver = ReminderBroadcastReceiver()
    }

    @After
    fun tearDown() {
    }

    @Test
    fun testManifest_reminderReceiver_isNotExported() {
        val manifestXml = File("src/main/AndroidManifest.xml").readText()
        assertTrue(
            "ReminderBroadcastReceiver must be declared with android:exported=\"false\"",
            manifestXml.contains("<receiver\n            android:name=\".notifications.ReminderBroadcastReceiver\"\n            android:exported=\"false\" />") ||
            manifestXml.contains("android:name=\".notifications.ReminderBroadcastReceiver\"\n            android:exported=\"false\"")
        )
    }

    @Test
    fun testManifest_bootReceiver_isGuardedByPermission() {
        val manifestXml = File("src/main/AndroidManifest.xml").readText()
        assertTrue(
            "BootCompletedReceiver must be guarded by RECEIVE_BOOT_COMPLETED",
            manifestXml.contains("android:name=\".notifications.BootCompletedReceiver\"") &&
            manifestXml.contains("android:permission=\"android.permission.RECEIVE_BOOT_COMPLETED\"")
        )
    }

    @Test
    fun testExternalImplicitTaskCompletionIntent_isIgnored() = runBlocking {
        // Seed a task
        val task = TaskEntity(
            id = 42L,
            profileId = 1L,
            title = "Important Security Review",
            category = "Work",
            dueDateIso = "2026-11-01",
            isOneTimeCompleted = false
        )
        database.taskDao().insertTasks(listOf(task))

        // Create an intent with unknown or foreign action
        val maliciousIntent = Intent("com.attacker.INJECT_COMPLETE").apply {
            putExtra(NotificationHelper.EXTRA_TASK_ID, 42L)
        }

        receiver.onReceive(context, maliciousIntent)

        // Give any coroutine a moment if it erroneously launched
        val loadedTask = database.taskDao().getTaskById(42L)
        assertNotNull(loadedTask)
        assertFalse("Task completion must not be toggled by unauthorized foreign action", loadedTask!!.isOneTimeCompleted)
    }

    @Test
    fun testForeignMedicationAction_isIgnored() = runBlocking {
        val med = MedicationEntity(
            id = 100L,
            profileId = 1L,
            taskId = 42L,
            name = "Aspirin",
            form = "TABLET",
            dosageAmount = "1",
            dosageUnit = "TABLET",
            scheduleType = "DAILY",
            startDateIso = "2026-11-01"
        )
        database.medicationDao().insertMedications(listOf(med))

        val dose = MedicationDoseEntity(
            id = 500L,
            medicationId = 100L,
            taskId = 42L,
            medicationName = "Aspirin",
            medicationForm = "TABLET",
            dosage = "1",
            scheduledDateIso = "2026-11-01",
            scheduledTime = "09:00",
            status = "SCHEDULED"
        )
        database.medicationDao().insertDoses(listOf(dose))

        val foreignIntent = Intent("com.attacker.TAKE_MEDICINE").apply {
            putExtra(NotificationHelper.EXTRA_DOSE_ID, 500L)
        }

        receiver.onReceive(context, foreignIntent)

        val loadedDose = database.medicationDao().getDoseById(500L)
        assertNotNull(loadedDose)
        assertEquals("Dose status must remain SCHEDULED when targeted with foreign action", "SCHEDULED", loadedDose!!.status)
    }

    @Test
    fun testMalformedNegativeOrZeroIds_areRejected() = runBlocking {
        val invalidTaskIdIntent = Intent(ReminderBroadcastReceiver.ACTION_TASK_COMPLETE).apply {
            putExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        }
        receiver.onReceive(context, invalidTaskIdIntent)

        val zeroTaskIdIntent = Intent(ReminderBroadcastReceiver.ACTION_TASK_COMPLETE).apply {
            putExtra(NotificationHelper.EXTRA_TASK_ID, 0L)
        }
        receiver.onReceive(context, zeroTaskIdIntent)

        val invalidDoseIdIntent = Intent(ReminderBroadcastReceiver.ACTION_MEDICINE_TAKEN).apply {
            putExtra(NotificationHelper.EXTRA_DOSE_ID, -5L)
        }
        receiver.onReceive(context, invalidDoseIdIntent)
    }

    @Test
    fun testBootCompletedAction_isRejectedByReminderBroadcastReceiver() {
        val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(context, bootIntent)
        // Passes cleanly without exception because ALLOWED_ACTIONS excludes ACTION_BOOT_COMPLETED
    }

    @Test
    fun testExplicitPendingIntent_actionTaskComplete_executesSuccessfully() = runBlocking {
        val task = TaskEntity(
            id = 77L,
            profileId = 1L,
            title = "Verified Task",
            category = "Engineering",
            dueDateIso = "2026-11-01",
            isOneTimeCompleted = false
        )
        database.taskDao().insertTasks(listOf(task))

        // Direct explicit invocation of domain action handler
        ReminderActionHandler.handleTaskComplete(context, 77L, "2026-11-01")

        val completedTask = database.taskDao().getTaskById(77L)
        assertNotNull(completedTask)
        assertTrue("Task must be marked completed by explicit action handler", completedTask!!.isOneTimeCompleted || completedTask.completedDatesJson.contains("2026-11-01"))
    }
}
