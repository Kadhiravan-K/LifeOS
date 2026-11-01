package com.example.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.analytics.ProductivityPeriod
import com.example.domain.analytics.ProductivityRadarEngine
import com.example.domain.analytics.RadarDimension
import com.example.domain.model.RecurrenceType
import com.example.domain.model.TaskItem
import com.example.domain.recurrence.RecurrenceEngine
import com.example.utils.EmojiUtils
import com.example.widgets.WidgetProfileManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * Release 3 Bug Fixes & UX Hardening Unit Test Suite.
 * Covers:
 * - Issue 1: Emoji input, ZWJ sequences, skin tones, leading emoji detection.
 * - Issue 2: Productivity analysis zero mock data verification.
 * - Issue 4: WidgetProfileManager per-instance profile binding & content mode.
 * - Issue 7: Calendar dot indicator filtering for daily recurring tasks.
 */
@RunWith(RobolectricTestRunner::class)
class Release3BugFixesUnitTest {

    // --- ISSUE 1: EMOJI KEYBOARD & TEXTFIELD INPUT ---

    @Test
    fun `test single unicode emojis and compound zwj sequences`() {
        val rocket = "🚀"
        val star = "⭐"
        val clock = "⏰"
        val familyZwj = "👨‍👩‍👧‍👦"
        val runnerSkinTone = "🏃🏽‍♀️"
        val sparkles = "✨"

        assertEquals(rocket, EmojiUtils.extractFirstEmoji(rocket))
        assertEquals(star, EmojiUtils.extractFirstEmoji(star))
        assertEquals(clock, EmojiUtils.extractFirstEmoji(clock))
        assertEquals(familyZwj, EmojiUtils.extractFirstEmoji(familyZwj))
        assertEquals(runnerSkinTone, EmojiUtils.extractFirstEmoji(runnerSkinTone))
        assertEquals(sparkles, EmojiUtils.extractFirstEmoji(sparkles))

        assertTrue(EmojiUtils.isSingleEmoji(rocket))
        assertTrue(EmojiUtils.isSingleEmoji(star))
        assertTrue(EmojiUtils.isSingleEmoji(clock))
        assertTrue(EmojiUtils.isSingleEmoji(familyZwj))
        assertTrue(EmojiUtils.isSingleEmoji(runnerSkinTone))
    }

    @Test
    fun `test leading emoji extraction from task title`() {
        val titleWithLeadingEmoji = "🏃 5km morning run"
        val titleWithZwjEmoji = "👨‍👩‍👧‍👦 Family dinner at 7"
        val titleWithoutEmoji = "Write unit tests for Release 3"

        assertEquals("🏃", EmojiUtils.extractLeadingEmoji(titleWithLeadingEmoji))
        assertEquals("👨‍👩‍👧‍👦", EmojiUtils.extractLeadingEmoji(titleWithZwjEmoji))
        assertNull(EmojiUtils.extractLeadingEmoji(titleWithoutEmoji))
    }

    @Test
    fun `test takeGraphemeClusters preserves complex emoji clusters`() {
        val mixedString = "👨‍👩‍👧‍👦🚀⭐"
        val firstCluster = EmojiUtils.takeGraphemeClusters(mixedString, 1)
        val firstTwoClusters = EmojiUtils.takeGraphemeClusters(mixedString, 2)

        assertEquals("👨‍👩‍👧‍👦", firstCluster)
        assertEquals("👨‍👩‍👧‍👦🚀", firstTwoClusters)
    }

    // --- ISSUE 2: NO MOCK DATA IN PRODUCTIVITY RADAR ---

    @Test
    fun `test zero mock data when profile has no tasks or habits`() {
        val result = ProductivityRadarEngine.calculate(
            tasks = emptyList(),
            habits = emptyList(),
            waterCurrentMl = 0,
            waterGoalMl = 2000,
            plans = emptyList(),
            currentStreak = 0,
            period = ProductivityPeriod.DAILY,
            todayIso = "2026-11-01"
        )

        assertFalse("Must indicate hasSufficientData is false", result.hasSufficientData)
        assertEquals("Overall score must be 0, not 50 or 80", 0, result.overallScorePercent)
        assertEquals(0, result.scoreFor(RadarDimension.TASKS))
        assertEquals(0, result.scoreFor(RadarDimension.HABITS))
        assertEquals(0, result.scoreFor(RadarDimension.WELLNESS))
        assertEquals(0, result.scoreFor(RadarDimension.PLANNING))
        assertEquals(0, result.scoreFor(RadarDimension.DISCIPLINE))
        assertEquals("No Activity Recorded", result.ratingTier)
    }

    // --- ISSUE 4: WIDGET INSTANCE PROFILE & CONTENT SELECTION ---

    @Test
    fun `test widget profile manager per-instance binding and content mode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val widgetId1 = 101
        val widgetId2 = 102

        // Bind widget 1 to Personal profile (1), widget 2 to Work profile (2)
        WidgetProfileManager.saveWidgetProfileId(context, widgetId1, 1L)
        WidgetProfileManager.saveWidgetProfileId(context, widgetId2, 2L)

        // Set different content modes
        WidgetProfileManager.saveWidgetContentMode(context, widgetId1, WidgetProfileManager.WidgetContentMode.RADAR)
        WidgetProfileManager.saveWidgetContentMode(context, widgetId2, WidgetProfileManager.WidgetContentMode.TODAY_TASKS)

        assertEquals(1L, WidgetProfileManager.getWidgetProfileId(context, widgetId1))
        assertEquals(2L, WidgetProfileManager.getWidgetProfileId(context, widgetId2))
        assertEquals(WidgetProfileManager.WidgetContentMode.RADAR, WidgetProfileManager.getWidgetContentMode(context, widgetId1))
        assertEquals(WidgetProfileManager.WidgetContentMode.TODAY_TASKS, WidgetProfileManager.getWidgetContentMode(context, widgetId2))

        // Clean up on widget deletion
        WidgetProfileManager.removeWidget(context, widgetId1)
        assertEquals(1L, WidgetProfileManager.getWidgetProfileId(context, widgetId1, defaultProfileId = 1L))
    }

    // --- ISSUE 7: CALENDAR DOT FILTERING ---

    @Test
    fun `test daily recurring task does not produce calendar dot indicators across every day`() {
        val dailyTask = TaskItem(
            id = 1,
            title = "Daily Meditation",
            dueDateIso = "2026-11-01",
            recurrenceType = RecurrenceType.DAILY
        )

        val oneTimeTask = TaskItem(
            id = 2,
            title = "Dentist Appointment",
            dueDateIso = "2026-11-05",
            recurrenceType = RecurrenceType.NONE
        )

        val weeklyTask = TaskItem(
            id = 3,
            title = "Weekly Team Sync",
            dueDateIso = "2026-11-02",
            recurrenceType = RecurrenceType.WEEKLY
        )

        val allTasks = listOf(dailyTask, oneTimeTask, weeklyTask)

        // Indicator filtering logic matching ViewModel and CalendarScreen
        val indicatorTasks = allTasks.filter { task ->
            if (task.isArchived) return@filter false
            val isDailyRecurring = task.recurrenceType == RecurrenceType.DAILY ||
                    (task.recurrenceType == RecurrenceType.CUSTOM && task.recurrenceUnit.equals("DAYS", ignoreCase = true) && task.recurrenceInterval == 1)
            !isDailyRecurring
        }

        // 1. Daily recurring task is filtered OUT of indicator tasks
        assertFalse(indicatorTasks.contains(dailyTask))
        assertTrue(indicatorTasks.contains(oneTimeTask))
        assertTrue(indicatorTasks.contains(weeklyTask))

        // 2. Dates with tasks calculation for visible dates
        val dates = listOf("2026-11-01", "2026-11-02", "2026-11-03", "2026-11-04", "2026-11-05")
        val datesWithDots = dates.filter { dateIso ->
            indicatorTasks.any { task ->
                RecurrenceEngine.isTaskScheduledOnDate(task, dateIso)
            }
        }.toSet()

        // Nov 01: only has dailyTask -> should NOT have a dot indicator!
        assertFalse("Nov 1 must not have a dot for daily recurring task", datesWithDots.contains("2026-11-01"))

        // Nov 05: has oneTimeTask -> MUST have dot
        assertTrue("Nov 5 must have dot for one-time appointment", datesWithDots.contains("2026-11-05"))

        // Nov 02: has weeklyTask (scheduled on Monday Nov 2) -> MUST have dot
        assertTrue("Nov 2 must have dot for weekly sync", datesWithDots.contains("2026-11-02"))

        // 3. BUT RecurrenceEngine still schedules dailyTask for Nov 01 when inspected directly
        assertTrue("Daily task is still scheduled on Nov 1 in task list", RecurrenceEngine.isTaskScheduledOnDate(dailyTask, "2026-11-01"))
    }
}
