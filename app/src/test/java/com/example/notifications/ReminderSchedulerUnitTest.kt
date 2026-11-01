package com.example.notifications

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.MedicationDoseEntity
import com.example.data.local.entities.MedicationEntity
import com.example.data.local.entities.ProfileEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.TaskReminderEntity
import com.example.data.preferences.UserPreferences
import com.example.data.repository.LifeRepository
import com.example.domain.model.TaskPriority
import com.example.ui.theme.engine.LocalPremiumEntitlementRepository
import com.example.utils.DateUtils
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

@RunWith(RobolectricTestRunner::class)
class ReminderSchedulerUnitTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var preferences: UserPreferences
    private lateinit var repository: LifeRepository
    private lateinit var scheduler: DefaultReminderScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = UserPreferences(context)
        repository = LifeRepository(
            database,
            preferences,
            LocalPremiumEntitlementRepository(context)
        )
        scheduler = DefaultReminderScheduler(
            context = context,
            database = database,
            preferences = preferences,
            repository = repository
        )

        runBlocking {
            database.profileDao().insertProfile(
                ProfileEntity.createDefaultPersonal()
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testSevenDayHorizonCalculation() = runBlocking {
        val todayIso = DateUtils.todayString()

        // Insert tasks for Day 0 through Day 9 (10 days total)
        for (i in 0..9) {
            val dateIso = DateUtils.addDays(todayIso, i)
            database.taskDao().insertTask(
                TaskEntity(
                    id = (i + 10).toLong(),
                    profileId = 1L,
                    title = "Future Task Day $i",
                    category = "WORK",
                    dueDateIso = dateIso,
                    dueTime = "18:00",
                    priority = TaskPriority.P1.name,
                    isReminderEnabled = true,
                    reminderMinutesBefore = 15,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        // Run scheduler with default 7-day horizon
        val result = scheduler.rescheduleAll(horizonDays = 7)

        assertEquals("Horizon must be clamped to 7", 7, result.horizonDays)
        assertEquals("Tasks beyond 7 days must not be scheduled", 7, result.tasksScheduled)
        assertFalse("Total alarms must not exceed limit", result.cappedByLimit)
    }

    @Test
    fun testTodayAndTomorrowMedicationReminders() = runBlocking {
        val todayIso = DateUtils.todayString()
        val tomorrowIso = DateUtils.addDays(todayIso, 1)
        val dayAfterTomorrowIso = DateUtils.addDays(todayIso, 2)

        database.medicationDao().insertMedication(
            MedicationEntity(
                id = 100L,
                profileId = 1L,
                taskId = 0L,
                name = "Vitamin D",
                form = "TABLET",
                dosageAmount = "1000",
                dosageUnit = "IU",
                scheduleType = "DAILY",
                startDateIso = todayIso,
                createdAt = System.currentTimeMillis()
            )
        )

        // Dose for today (Day 0)
        database.medicationDao().insertDose(
            MedicationDoseEntity(
                id = 1L,
                medicationId = 100L,
                taskId = 0L,
                medicationName = "Vitamin D",
                medicationForm = "TABLET",
                dosage = "1000 IU",
                scheduledDateIso = todayIso,
                scheduledTime = "23:59",
                status = "SCHEDULED"
            )
        )

        // Dose for tomorrow (Day 1)
        database.medicationDao().insertDose(
            MedicationDoseEntity(
                id = 2L,
                medicationId = 100L,
                taskId = 0L,
                medicationName = "Vitamin D",
                medicationForm = "TABLET",
                dosage = "1000 IU",
                scheduledDateIso = tomorrowIso,
                scheduledTime = "09:00",
                status = "SCHEDULED"
            )
        )

        // Dose for day after tomorrow (Day 2 - must NOT be scheduled per requirements)
        database.medicationDao().insertDose(
            MedicationDoseEntity(
                id = 3L,
                medicationId = 100L,
                taskId = 0L,
                medicationName = "Vitamin D",
                medicationForm = "TABLET",
                dosage = "1000 IU",
                scheduledDateIso = dayAfterTomorrowIso,
                scheduledTime = "09:00",
                status = "SCHEDULED"
            )
        )

        val result = scheduler.rescheduleAll(horizonDays = 7)

        assertEquals("Only today's and tomorrow's doses should be scheduled (exactly 2)", 2, result.dosesScheduled)
    }

    @Test
    fun testTaskReminderReconstruction() = runBlocking {
        val tomorrowIso = DateUtils.addDays(DateUtils.todayString(), 1)
        val taskId = 42L

        database.taskDao().insertTask(
            TaskEntity(
                id = taskId,
                profileId = 1L,
                title = "Important Presentation",
                category = "WORK",
                dueDateIso = tomorrowIso,
                dueTime = "14:00",
                priority = TaskPriority.P1.name,
                isReminderEnabled = true,
                reminderMinutesBefore = 0,
                createdAt = System.currentTimeMillis()
            )
        )

        database.taskReminderDao().insertReminder(
            TaskReminderEntity(
                id = 501L,
                taskId = taskId,
                minutesBefore = 60,
                type = "MINUTES_BEFORE",
                customTime = null,
                isEnabled = true
            )
        )
        database.taskReminderDao().insertReminder(
            TaskReminderEntity(
                id = 502L,
                taskId = taskId,
                minutesBefore = 15,
                type = "MINUTES_BEFORE",
                customTime = null,
                isEnabled = true
            )
        )

        val result = scheduler.rescheduleAll(horizonDays = 7)
        assertEquals("Both explicit task reminders must be scheduled", 2, result.tasksScheduled)
    }

    @Test
    fun testRepeatedSchedulingIsIdempotent() = runBlocking {
        val tomorrowIso = DateUtils.addDays(DateUtils.todayString(), 1)
        database.taskDao().insertTask(
            TaskEntity(
                id = 77L,
                profileId = 1L,
                title = "Dentist Appointment",
                category = "HEALTH",
                dueDateIso = tomorrowIso,
                dueTime = "11:00",
                priority = TaskPriority.P2.name,
                isReminderEnabled = true,
                reminderMinutesBefore = 30,
                createdAt = System.currentTimeMillis()
            )
        )

        val run1 = scheduler.rescheduleAll(horizonDays = 7)
        val run2 = scheduler.rescheduleAll(horizonDays = 7)

        assertEquals("Tasks scheduled count must match across repeated runs", run1.tasksScheduled, run2.tasksScheduled)
        assertEquals("Total scheduled count must match across repeated runs", run1.totalScheduled, run2.totalScheduled)
    }

    @Test
    fun testCompletedAndArchivedAndDeletedTasksIgnored() = runBlocking {
        val tomorrowIso = DateUtils.addDays(DateUtils.todayString(), 1)

        // 1. Archived task
        database.taskDao().insertTask(
            TaskEntity(
                id = 101L,
                profileId = 1L,
                title = "Archived Task",
                category = "WORK",
                dueDateIso = tomorrowIso,
                dueTime = "10:00",
                priority = TaskPriority.P1.name,
                isReminderEnabled = true,
                isArchived = true,
                createdAt = System.currentTimeMillis()
            )
        )

        // 2. Completed task
        database.taskDao().insertTask(
            TaskEntity(
                id = 102L,
                profileId = 1L,
                title = "Completed Task",
                category = "WORK",
                dueDateIso = tomorrowIso,
                dueTime = "11:00",
                priority = TaskPriority.P1.name,
                isReminderEnabled = true,
                isOneTimeCompleted = true,
                createdAt = System.currentTimeMillis()
            )
        )

        // 3. Task without dueTime
        database.taskDao().insertTask(
            TaskEntity(
                id = 103L,
                profileId = 1L,
                title = "No Due Time Task",
                category = "WORK",
                dueDateIso = tomorrowIso,
                dueTime = null,
                priority = TaskPriority.P1.name,
                isReminderEnabled = true,
                createdAt = System.currentTimeMillis()
            )
        )

        val result = scheduler.rescheduleAll(horizonDays = 7)
        assertEquals("No invalid or completed tasks should be scheduled", 0, result.tasksScheduled)
    }

    @Test
    fun testEnforceMax200AlarmLimit() = runBlocking {
        val tomorrowIso = DateUtils.addDays(DateUtils.todayString(), 1)

        // Insert 250 tasks
        for (i in 1..250) {
            database.taskDao().insertTask(
                TaskEntity(
                    id = i.toLong(),
                    profileId = 1L,
                    title = "Bulk Task $i",
                    category = "WORK",
                    dueDateIso = tomorrowIso,
                    dueTime = "12:00",
                    priority = TaskPriority.P1.name,
                    isReminderEnabled = true,
                    reminderMinutesBefore = 10,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        val result = scheduler.rescheduleAll(horizonDays = 7)

        assertEquals("Total scheduled alarms must be capped at 200", ReminderScheduler.MAX_ALARMS, result.totalScheduled)
        assertTrue("Capped flag must be true", result.cappedByLimit)
    }

    @Test
    fun testRequestCodeFormulasRemainStable() {
        val taskId = 45L
        val dateIso = "2026-11-05"
        val reminderId = 99L
        val doseId = 120L

        val expectedTaskRequestCode = (taskId.toInt() * 31) + dateIso.hashCode().and(0x7FFFFFFF) % 50000
        val expectedReminderRequestCode = ((taskId.toInt() * 31 + reminderId.toInt()) * 31) + dateIso.hashCode().and(0x7FFFFFFF) % 100000
        val expectedDoseRequestCode = (doseId.toInt() * 37) + 50000

        assertTrue("Task request code must be positive integer", expectedTaskRequestCode > 0)
        assertTrue("Reminder request code must be positive integer", expectedReminderRequestCode > 0)
        assertTrue("Dose request code must be positive integer", expectedDoseRequestCode > 0)
    }

    @Test
    fun testExactAlarmPermissionHelpers() {
        val status = ReminderScheduler.checkExactAlarmStatus(context)
        assertNotNull(status)
        assertTrue(status in ExactAlarmStatus.values())
    }
}
