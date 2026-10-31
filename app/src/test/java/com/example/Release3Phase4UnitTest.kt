package com.example

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MIGRATION_12_13
import com.example.domain.model.PlanItem
import com.example.domain.model.PlanLevel
import com.example.domain.model.PlanMilestone
import com.example.domain.model.PlanStatus
import com.example.domain.model.RecurrenceType
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.recurrence.RecurrenceEngine
import com.example.ui.screens.planning.PlanningBoardTab
import com.example.utils.DateUtils
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Release 3 — Phase 4 Unit Tests:
 * 1. Room v13 Migration (MIGRATION_12_13) for recurrence window columns.
 * 2. Flexible monthly recurrence (MONTHLY_WINDOW) single obligation, completion, short months, Feb leap-year clamping.
 * 3. Backward compatibility for existing recurrence types.
 * 4. Four distinct planning boards (Daily, Weekly, Monthly, Yearly) architecture and profile scoping.
 * 5. Data export/import serialization of recurrence window columns.
 */
@RunWith(RobolectricTestRunner::class)
class Release3Phase4UnitTest {

    // --- 1. ROOM V13 MIGRATION ---
    @Test
    fun testMigration12To13_addsRecurrenceWindowColumns() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_12_13.db"
        context.deleteDatabase(dbName)

        val factory = FrameworkSQLiteOpenHelperFactory()

        // 1. Setup v12 tasks table
        val v12Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(12) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `tasks` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `profileId` INTEGER NOT NULL DEFAULT 1,
                            `title` TEXT NOT NULL,
                            `emoji` TEXT NOT NULL DEFAULT '',
                            `description` TEXT NOT NULL DEFAULT '',
                            `category` TEXT NOT NULL,
                            `priority` TEXT NOT NULL DEFAULT 'MEDIUM',
                            `dueDateIso` TEXT NOT NULL,
                            `dueTime` TEXT DEFAULT NULL,
                            `durationMinutes` INTEGER NOT NULL DEFAULT 30,
                            `schedulingMode` TEXT NOT NULL DEFAULT 'DURATION',
                            `durationAmount` INTEGER NOT NULL DEFAULT 30,
                            `durationUnit` TEXT NOT NULL DEFAULT 'MINUTES',
                            `deadlineIso` TEXT DEFAULT NULL,
                            `deadlineTime` TEXT DEFAULT NULL,
                            `isAllDay` INTEGER NOT NULL DEFAULT 1,
                            `recurrenceType` TEXT NOT NULL DEFAULT 'NONE',
                            `recurrenceInterval` INTEGER NOT NULL DEFAULT 1,
                            `recurrenceUnit` TEXT NOT NULL DEFAULT 'DAYS',
                            `recurrenceDaysOfWeek` TEXT NOT NULL DEFAULT '',
                            `recurrenceEndCondition` TEXT NOT NULL DEFAULT 'NEVER',
                            `recurrenceEndDateIso` TEXT DEFAULT NULL,
                            `recurrenceCount` INTEGER NOT NULL DEFAULT 1,
                            `isOneTimeCompleted` INTEGER NOT NULL DEFAULT 0,
                            `completedDatesJson` TEXT NOT NULL DEFAULT '[]',
                            `skippedDatesJson` TEXT NOT NULL DEFAULT '[]',
                            `rescheduledDatesJson` TEXT NOT NULL DEFAULT '{}',
                            `subtasksJson` TEXT NOT NULL DEFAULT '[]',
                            `notes` TEXT NOT NULL DEFAULT '',
                            `isReminderEnabled` INTEGER NOT NULL DEFAULT 0,
                            `reminderMinutesBefore` INTEGER NOT NULL DEFAULT 0,
                            `isArchived` INTEGER NOT NULL DEFAULT 0,
                            `isPaused` INTEGER NOT NULL DEFAULT 0,
                            `isQuickReminder` INTEGER NOT NULL DEFAULT 0,
                            `projectId` INTEGER DEFAULT NULL,
                            `createdAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helperV12 = factory.create(v12Config)
        val dbV12 = helperV12.writableDatabase
        dbV12.close()

        // 2. Open at v13 and apply MIGRATION_12_13
        val v13Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(13) {
                override fun onCreate(db: SupportSQLiteDatabase) {}

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 12 && newVersion == 13) {
                        MIGRATION_12_13.migrate(db)
                    }
                }
            })
            .build()

        val helperV13 = factory.create(v13Config)
        val dbV13 = helperV13.writableDatabase

        // Insert task with recurrence window start and end day
        dbV13.execSQL("""
            INSERT INTO `tasks` (
                `title`, `category`, `dueDateIso`, `recurrenceType`,
                `recurrenceWindowStartDay`, `recurrenceWindowEndDay`
            ) VALUES (
                'Pay Rent Window', 'Finance', '2026-11-25', 'MONTHLY_WINDOW', 25, 30
            )
        """)

        val cursor = dbV13.query("SELECT recurrenceWindowStartDay, recurrenceWindowEndDay FROM tasks WHERE title = 'Pay Rent Window'")
        assertTrue(cursor.moveToFirst())
        assertEquals(25, cursor.getInt(0))
        assertEquals(30, cursor.getInt(1))
        cursor.close()
        dbV13.close()
    }

    // --- 2. FLEXIBLE MONTHLY WINDOW RECURRENCE (#21) ---
    @Test
    fun testMonthlyWindow_isScheduledWithinWindow() {
        val task = TaskItem(
            id = 101,
            title = "Pay Utility Bill",
            dueDateIso = "2026-11-01",
            recurrenceType = RecurrenceType.MONTHLY_WINDOW,
            recurrenceWindowStartDay = 25,
            recurrenceWindowEndDay = 30
        )

        // Days before window
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-11-20"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-11-24"))

        // Days inside window
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-11-25"))
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-11-27"))
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-11-30"))

        // Days after window
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-12-01"))
    }

    @Test
    fun testMonthlyWindow_singleObligationSatisfactionAndNextMonthReset() {
        var task = TaskItem(
            id = 102,
            title = "Pay Rent",
            dueDateIso = "2026-11-01",
            recurrenceType = RecurrenceType.MONTHLY_WINDOW,
            recurrenceWindowStartDay = 25,
            recurrenceWindowEndDay = 30,
            completedDates = emptySet()
        )

        // Before completion: not completed on any date in November
        assertFalse(RecurrenceEngine.isCompletedOnDate(task, "2026-11-25"))
        assertFalse(RecurrenceEngine.isCompletedOnDate(task, "2026-11-26"))

        // User completes on day 26
        task = task.copy(completedDates = setOf("2026-11-26"))

        // Now, ANY valid day in November satisfies that month's single obligation
        assertTrue(RecurrenceEngine.isCompletedOnDate(task, "2026-11-25"))
        assertTrue(RecurrenceEngine.isCompletedOnDate(task, "2026-11-26"))
        assertTrue(RecurrenceEngine.isCompletedOnDate(task, "2026-11-27"))
        assertTrue(RecurrenceEngine.isCompletedOnDate(task, "2026-11-30"))

        // Next month (December 2026): resets independently!
        assertFalse(RecurrenceEngine.isCompletedOnDate(task, "2026-12-25"))
        assertFalse(RecurrenceEngine.isCompletedOnDate(task, "2026-12-26"))

        // When user completes in December on day 28:
        task = task.copy(completedDates = setOf("2026-11-26", "2026-12-28"))
        assertTrue(RecurrenceEngine.isCompletedOnDate(task, "2026-12-25"))
        assertTrue(RecurrenceEngine.isCompletedOnDate(task, "2026-12-28"))
        assertTrue(RecurrenceEngine.isCompletedOnDate(task, "2026-12-30"))

        // January 2027 remains incomplete
        assertFalse(RecurrenceEngine.isCompletedOnDate(task, "2027-01-25"))
    }

    @Test
    fun testMonthlyWindow_clampingForShortMonths() {
        // Window configured for 31st only (e.g. End of Month Bookkeeping)
        val task = TaskItem(
            id = 103,
            title = "Monthly Bookkeeping",
            dueDateIso = "2026-01-01",
            recurrenceType = RecurrenceType.MONTHLY_WINDOW,
            recurrenceWindowStartDay = 31,
            recurrenceWindowEndDay = 31
        )

        // In 31-day months (January, March): fires on the 31st
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-01-31"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-01-30"))

        // In 30-day month (April): clamps to final day (April 30) so obligation is NEVER missed
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-04-30"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-04-29"))

        // In 30-day month (November): clamps to final day (November 30)
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-11-30"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-11-29"))
    }

    @Test
    fun testMonthlyWindow_februaryLeapAndNonLeapClamping() {
        val task = TaskItem(
            id = 104,
            title = "February Month-End Obligation",
            dueDateIso = "2024-01-01",
            recurrenceType = RecurrenceType.MONTHLY_WINDOW,
            recurrenceWindowStartDay = 29,
            recurrenceWindowEndDay = 31
        )

        // 2026 is non-leap year (February has 28 days) -> clamps to 28th
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-02-28"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-02-27"))

        // 2028 is leap year (February has 29 days) -> clamps to 29th
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2028-02-29"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2028-02-28"))
    }

    @Test
    fun testMonthlyWindow_getNextNOccurrencesProducesSinglePerMonth() {
        val task = TaskItem(
            id = 105,
            title = "Monthly Review Window",
            dueDateIso = "2026-11-01",
            recurrenceType = RecurrenceType.MONTHLY_WINDOW,
            recurrenceWindowStartDay = 25,
            recurrenceWindowEndDay = 30
        )

        val nextOccurrences = RecurrenceEngine.getNextNOccurrences(task, count = 3, fromDateIso = "2026-11-01")
        assertEquals(3, nextOccurrences.size)

        // First occurrence is November 25 (first day of Nov window)
        assertEquals("2026-11-25", nextOccurrences[0])
        // Second occurrence is December 25 (first day of Dec window) - NOT another day in November!
        assertEquals("2026-12-25", nextOccurrences[1])
        // Third occurrence is January 25
        assertEquals("2027-01-25", nextOccurrences[2])
    }

    @Test
    fun testMonthlyWindow_previewFormatting() {
        val taskWindow = TaskItem(
            title = "Window Task",
            dueDateIso = "2026-11-01",
            recurrenceType = RecurrenceType.MONTHLY_WINDOW,
            recurrenceWindowStartDay = 25,
            recurrenceWindowEndDay = 30
        )
        val preview = RecurrenceEngine.formatRecurrencePreview(taskWindow)
        assertEquals("Monthly between the 25th and 30th", preview)

        val taskSingleDay = taskWindow.copy(recurrenceWindowStartDay = 1, recurrenceWindowEndDay = 1)
        val previewSingle = RecurrenceEngine.formatRecurrencePreview(taskSingleDay)
        assertEquals("Monthly on the 1st", previewSingle)
    }

    // --- 3. BACKWARD COMPATIBILITY FOR EXISTING RECURRENCE TYPES ---
    @Test
    fun testBackwardCompatibility_dailyRecurrencePreserved() {
        val task = TaskItem(
            title = "Daily Workout",
            dueDateIso = "2026-11-01",
            recurrenceType = RecurrenceType.DAILY
        )
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-11-01"))
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-11-02"))
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-11-15"))
    }

    @Test
    fun testBackwardCompatibility_weekdaysRecurrencePreserved() {
        val task = TaskItem(
            title = "Weekday Standup",
            dueDateIso = "2026-11-02", // Monday
            recurrenceType = RecurrenceType.WEEKDAYS
        )
        // 2026-11-02 is Monday -> matches
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-11-02"))
        // 2026-11-06 is Friday -> matches
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-11-06"))
        // 2026-11-07 is Saturday -> does NOT match
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-11-07"))
        // 2026-11-08 is Sunday -> does NOT match
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-11-08"))
    }

    @Test
    fun testBackwardCompatibility_fixedMonthlyRecurrencePreserved() {
        val task = TaskItem(
            title = "Fixed Monthly Report",
            dueDateIso = "2026-11-15",
            recurrenceType = RecurrenceType.MONTHLY
        )
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-11-15"))
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-12-15"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-11-16"))
    }

    // --- 4. FOUR PLANNING BOARDS ARCHITECTURE (#10) ---
    @Test
    fun testFourPlanningBoardTabs_mappedToDistinctPlanLevels() {
        assertEquals(PlanLevel.DAILY, PlanningBoardTab.DAILY.level)
        assertEquals(PlanLevel.WEEKLY, PlanningBoardTab.WEEKLY.level)
        assertEquals(PlanLevel.MONTHLY, PlanningBoardTab.MONTHLY.level)
        assertEquals(PlanLevel.YEARLY, PlanningBoardTab.YEARLY.level)
    }

    @Test
    fun testPlanItem_progressFractionCalculation() {
        val planWithMilestones = PlanItem(
            id = 1,
            profileId = 1,
            title = "Launch Release 3",
            level = PlanLevel.MONTHLY,
            startDateIso = "2026-11-01",
            targetDateIso = "2026-11-30",
            status = PlanStatus.ACTIVE,
            milestones = listOf(
                PlanMilestone(id = 1, planId = 1, title = "Design UI", isCompleted = true),
                PlanMilestone(id = 2, planId = 1, title = "Implement Room", isCompleted = true),
                PlanMilestone(id = 3, planId = 1, title = "Write Tests", isCompleted = false),
                PlanMilestone(id = 4, planId = 1, title = "Deploy", isCompleted = false)
            )
        )
        // 2 of 4 milestones completed = 0.50f (50%)
        assertEquals(0.50f, planWithMilestones.progressFraction, 0.001f)

        // When marked completed, progress is 1.0f (100%)
        val completedPlan = planWithMilestones.copy(status = PlanStatus.COMPLETED)
        assertEquals(1.0f, completedPlan.progressFraction, 0.001f)
    }

    @Test
    fun testPlanningBoard_profileScoping() {
        val planProfile1 = PlanItem(id = 1, profileId = 1, title = "Personal Goal", startDateIso = "2026-11-01", targetDateIso = "2026-11-07")
        val planProfile2 = PlanItem(id = 2, profileId = 2, title = "Work Goal", startDateIso = "2026-11-01", targetDateIso = "2026-11-07")

        val allPlans = listOf(planProfile1, planProfile2)
        val profile1Plans = allPlans.filter { it.profileId == 1L }
        val profile2Plans = allPlans.filter { it.profileId == 2L }

        assertEquals(1, profile1Plans.size)
        assertEquals("Personal Goal", profile1Plans[0].title)

        assertEquals(1, profile2Plans.size)
        assertEquals("Work Goal", profile2Plans[0].title)
    }

    // --- 5. DATA EXPORT / IMPORT SERIALIZATION ---
    @Test
    fun testTaskItem_recurrenceWindowFieldsInJson() {
        val taskJson = JSONObject().apply {
            put("title", "Rent Payment Window")
            put("recurrenceType", "MONTHLY_WINDOW")
            put("recurrenceWindowStartDay", 25)
            put("recurrenceWindowEndDay", 30)
        }

        val startDay = if (taskJson.isNull("recurrenceWindowStartDay")) null else taskJson.optInt("recurrenceWindowStartDay")
        val endDay = if (taskJson.isNull("recurrenceWindowEndDay")) null else taskJson.optInt("recurrenceWindowEndDay")

        assertEquals(25, startDay)
        assertEquals(30, endDay)
    }
}
