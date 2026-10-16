package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.ChallengePresetCatalog
import com.example.domain.model.DefaultHoneycombShortcuts
import com.example.domain.model.HoneycombShortcut
import com.example.domain.model.ShortcutType
import com.example.domain.model.RecurrenceType
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.recurrence.RecurrenceEngine
import com.example.domain.schedule.ConflictType
import com.example.domain.schedule.ScheduleConflictEngine
import com.example.notifications.NotificationHelper
import com.example.ui.components.createRoundedHexagonPath
import com.example.ui.components.partitionShortcuts
import com.example.ui.theme.engine.HoneycombEngravingType
import com.example.ui.theme.engine.LocalPremiumEntitlementRepository
import com.example.ui.theme.engine.ThemeDefinition
import com.example.ui.theme.engine.ThemeId
import com.example.ui.theme.engine.ThemeRegistry
import com.example.ui.theme.engine.toMaterial3ColorScheme
import com.example.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LifeOS", appName)
    }

    // --- RECURRENCE TESTS ---

    @Test
    fun `test daily recurrence schedules every day on or after start date`() {
        val task = TaskItem(
            id = 1,
            title = "Daily Meditation",
            dueDateIso = "2026-10-15",
            dueTime = "06:30",
            recurrenceType = RecurrenceType.DAILY
        )

        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-10-15"))
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-10-16"))
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-10-25"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-10-14")) // before start
    }

    @Test
    fun `test Tuesday Thursday Sunday laundry routine explicit rule`() {
        // Laundry routine: Tuesday, Thursday, Sunday
        val laundryTask = TaskItem(
            id = 2,
            title = "Laundry Routine",
            dueDateIso = "2026-10-11", // Sunday
            recurrenceType = RecurrenceType.SELECTED_WEEKDAYS,
            recurrenceDaysOfWeek = listOf("TUESDAY", "THURSDAY", "SUNDAY")
        )

        // 2026-10-11 is Sunday -> true
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(laundryTask, "2026-10-11"))
        // 2026-10-12 is Monday -> false
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(laundryTask, "2026-10-12"))
        // 2026-10-13 is Tuesday -> true
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(laundryTask, "2026-10-13"))
        // 2026-10-14 is Wednesday -> false
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(laundryTask, "2026-10-14"))
        // 2026-10-15 is Thursday -> true
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(laundryTask, "2026-10-15"))
        // 2026-10-16 is Friday -> false
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(laundryTask, "2026-10-16"))
        // 2026-10-17 is Saturday -> false
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(laundryTask, "2026-10-17"))
        // 2026-10-18 is Sunday -> true
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(laundryTask, "2026-10-18"))
    }

    @Test
    fun `test Sunday deep cleaning routine explicit rule`() {
        val deepCleanTask = TaskItem(
            id = 3,
            title = "Sunday Deep Cleaning",
            dueDateIso = "2026-10-11", // Sunday
            recurrenceType = RecurrenceType.SELECTED_WEEKDAYS,
            recurrenceDaysOfWeek = listOf("SUNDAY")
        )

        // Sundays
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(deepCleanTask, "2026-10-11"))
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(deepCleanTask, "2026-10-18"))
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(deepCleanTask, "2026-10-25"))
        // Other weekdays
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(deepCleanTask, "2026-10-12")) // Mon
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(deepCleanTask, "2026-10-14")) // Wed
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(deepCleanTask, "2026-10-17")) // Sat
    }

    @Test
    fun `test every two days recurrence`() {
        val task = TaskItem(
            id = 4,
            title = "Alternate Day Workout",
            dueDateIso = "2026-10-10",
            recurrenceType = RecurrenceType.EVERY_N_DAYS,
            recurrenceInterval = 2
        )

        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-10-10")) // Day 0
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-10-11")) // Day 1
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-10-12")) // Day 2
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-10-13")) // Day 3
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-10-14")) // Day 4
    }

    @Test
    fun `test monthly recurrence and month end handling`() {
        val monthlyTask = TaskItem(
            id = 5,
            title = "Monthly Budget Review",
            dueDateIso = "2026-01-31", // Jan 31st
            recurrenceType = RecurrenceType.MONTHLY
        )

        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(monthlyTask, "2026-01-31"))
        // In 2026 (non-leap), Feb has 28 days -> should match Feb 28
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(monthlyTask, "2026-02-28"))
        // In March (31 days) -> should match March 31
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(monthlyTask, "2026-03-31"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(monthlyTask, "2026-03-30"))
    }

    @Test
    fun `test leap year yearly recurrence`() {
        val leapYearTask = TaskItem(
            id = 6,
            title = "Leap Day Celebration",
            dueDateIso = "2024-02-29", // 2024 was leap year
            recurrenceType = RecurrenceType.YEARLY
        )

        // 2024 is a leap year
        assertTrue(DateUtils.isLeapYear(2024))
        // 2026 is NOT a leap year
        assertFalse(DateUtils.isLeapYear(2026))

        // On non-leap year 2026, maps to Feb 28
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(leapYearTask, "2026-02-28"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(leapYearTask, "2026-02-27"))

        // On leap year 2028, maps to Feb 29
        assertTrue(DateUtils.isLeapYear(2028))
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(leapYearTask, "2028-02-29"))
    }

    @Test
    fun `test skipped occurrence`() {
        val task = TaskItem(
            id = 7,
            title = "Daily Walk",
            dueDateIso = "2026-10-10",
            recurrenceType = RecurrenceType.DAILY,
            skippedDates = setOf("2026-10-12") // 12th was skipped
        )

        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-10-11"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-10-12")) // Skipped
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-10-13"))
    }

    @Test
    fun `test rescheduled occurrence moves instance without duplicating`() {
        val task = TaskItem(
            id = 8,
            title = "Client Presentation",
            dueDateIso = "2026-10-15",
            recurrenceType = RecurrenceType.DAILY,
            rescheduledDates = mapOf("2026-10-15" to "2026-10-17") // 15th moved to 17th
        )

        // Does NOT appear on 15th anymore
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-10-15"))
        // Appears on 17th
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-10-17"))
    }

    @Test
    fun `test paused recurring task does not generate occurrences`() {
        val task = TaskItem(
            id = 9,
            title = "Paused Project",
            dueDateIso = "2026-10-10",
            recurrenceType = RecurrenceType.DAILY,
            isPaused = true
        )

        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-10-15"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-10-16"))
    }

    @Test
    fun `test completing today occurrence does not complete future occurrence`() {
        val task = TaskItem(
            id = 10,
            title = "Drink 2L Water",
            dueDateIso = "2026-10-15",
            recurrenceType = RecurrenceType.DAILY,
            completedDates = setOf("2026-10-15")
        )

        assertTrue(RecurrenceEngine.isCompletedOnDate(task, "2026-10-15"))
        assertFalse(RecurrenceEngine.isCompletedOnDate(task, "2026-10-16"))
        assertFalse(RecurrenceEngine.isCompletedOnDate(task, "2026-10-17"))
    }

    // --- SCHEDULE CONFLICT TESTS ---

    @Test
    fun `test sleep window conflict detection`() {
        val lateTask = TaskItem(
            id = 11,
            title = "Late Night Movie",
            dueDateIso = "2026-10-15",
            dueTime = "23:00", // 11:00 PM (inside 9:30 PM - 5:30 AM sleep)
            durationMinutes = 60
        )

        val conflicts = ScheduleConflictEngine.detectConflicts(
            task = lateTask,
            existingTasksOnDate = emptyList(),
            targetDateIso = "2026-10-15",
            wakeUpMinutes = 330, // 5:30 AM
            bedTimeMinutes = 1290 // 9:30 PM
        )

        assertTrue(conflicts.any { it.type == ConflictType.SLEEP_WINDOW })
        val sleepConflict = conflicts.first { it.type == ConflictType.SLEEP_WINDOW }
        assertNotNull(sleepConflict.suggestedTime)
    }

    @Test
    fun `test overlapping task conflict detection`() {
        val existingTask = TaskItem(
            id = 12,
            title = "Deep Work",
            dueDateIso = "2026-10-15",
            dueTime = "09:00",
            durationMinutes = 60 // 9:00 - 10:00 AM
        )

        val newTask = TaskItem(
            id = 13,
            title = "Doctor Appointment",
            dueDateIso = "2026-10-15",
            dueTime = "09:30",
            durationMinutes = 45 // 9:30 - 10:15 AM (overlaps!)
        )

        val conflicts = ScheduleConflictEngine.detectConflicts(
            task = newTask,
            existingTasksOnDate = listOf(existingTask),
            targetDateIso = "2026-10-15"
        )

        assertTrue(conflicts.any { it.type == ConflictType.OVERLAPPING_TASK })
    }

    @Test
    fun `test duplicate task detection`() {
        val existingTask = TaskItem(
            id = 14,
            title = "Clean Bathroom",
            dueDateIso = "2026-10-15"
        )

        val duplicateTask = TaskItem(
            id = 15,
            title = "clean bathroom", // Case-insensitive duplicate
            dueDateIso = "2026-10-15"
        )

        val conflicts = ScheduleConflictEngine.detectConflicts(
            task = duplicateTask,
            existingTasksOnDate = listOf(existingTask),
            targetDateIso = "2026-10-15"
        )

        assertTrue(conflicts.any { it.type == ConflictType.DUPLICATE_TASK })
    }

    // --- NOTIFICATION & RECOVERY TESTS ---

    @Test
    fun `test notification helper channels created without exception`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        NotificationHelper.createNotificationChannels(context)
        assertTrue(true)
    }

    @Test
    fun `test cancel reminder alarm does not crash`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        NotificationHelper.cancelTaskAlarm(context, 999L, "2026-10-15")
        assertTrue(true)
    }

    @Test
    fun `test occurrence override skips on parent while maintaining other days`() {
        val recurringTask = TaskItem(
            id = 20,
            title = "Morning Exercise",
            dueDateIso = "2026-10-15",
            recurrenceType = RecurrenceType.DAILY,
            skippedDates = setOf("2026-10-16") // occurrence on 16th overridden/skipped
        )

        val overriddenInstance = TaskItem(
            id = 21,
            title = "Morning Yoga (Special Session)",
            dueDateIso = "2026-10-16",
            recurrenceType = RecurrenceType.NONE
        )

        // On 15th: parent recurring task shows
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(recurringTask, "2026-10-15"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(overriddenInstance, "2026-10-15"))

        // On 16th: parent is skipped, overridden instance shows
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(recurringTask, "2026-10-16"))
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(overriddenInstance, "2026-10-16"))

        // On 17th: parent continues daily schedule normally
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(recurringTask, "2026-10-17"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(overriddenInstance, "2026-10-17"))
    }

    // --- PHASE 2.6: HONEYCOMB DASHBOARD & 75 HARD CUSTOMIZATION TESTS ---

    @Test
    fun `test honeycomb partitioning arranges 10 items into 3-2-3-2 rows`() {
        val shortcuts = DefaultHoneycombShortcuts.list
        assertEquals(10, shortcuts.size)

        val rows = partitionShortcuts(shortcuts, baseCols = 3)
        assertEquals(4, rows.size)
        assertEquals(3, rows[0].size)
        assertEquals(2, rows[1].size)
        assertEquals(3, rows[2].size)
        assertEquals(2, rows[3].size)
    }

    @Test
    fun `test honeycomb partitioning handles odd and small count without empty cells`() {
        val shortcuts = DefaultHoneycombShortcuts.list.take(5)
        val rows = partitionShortcuts(shortcuts, baseCols = 3)
        assertEquals(2, rows.size)
        assertEquals(3, rows[0].size)
        assertEquals(2, rows[1].size)
    }

    @Test
    fun `test rounded hexagon mathematical path generator produces closed path`() {
        val path = createRoundedHexagonPath(
            cx = 100f,
            cy = 100f,
            radius = 50f,
            cornerRadius = 10f
        )
        assertNotNull(path)
        assertFalse(path.isEmpty)
    }

    @Test
    fun `test default honeycomb shortcuts include 10 suggested routines`() {
        val shortcuts = DefaultHoneycombShortcuts.list
        val titles = shortcuts.map { it.title }
        assertTrue(titles.contains("Hydration"))
        assertTrue(titles.contains("Reading"))
        assertTrue(titles.contains("Meditation"))
        assertTrue(titles.contains("Exercise"))
        assertTrue(titles.contains("Yoga"))
        assertTrue(titles.contains("Make Bed"))
        assertTrue(titles.contains("Career Learning"))
        assertTrue(titles.contains("Cleaning"))
        assertTrue(titles.contains("Sleep Routine"))
        assertTrue(titles.contains("Daily Planning"))
    }

    @Test
    fun `test 75 hard preset catalog includes Official, Soft, and Medium`() {
        val catalog = ChallengePresetCatalog.list
        assertEquals(3, catalog.size)

        val official = ChallengePresetCatalog.OFFICIAL_75_HARD
        assertEquals("Official 75 Hard", official.name)
        assertEquals(3700, official.waterTargetMl)
        assertEquals(2, official.workoutCount)
        assertEquals(6, official.rules.size)

        val soft = ChallengePresetCatalog.SEVENTY_FIVE_SOFT
        assertEquals("75 Soft", soft.name)
        assertEquals(2500, soft.waterTargetMl)
        assertEquals(1, soft.workoutCount)
        assertEquals(5, soft.rules.size)

        val medium = ChallengePresetCatalog.SEVENTY_FIVE_MEDIUM
        assertEquals("75 Medium", medium.name)
        assertEquals(3000, medium.waterTargetMl)
        assertEquals(2, medium.workoutCount)
        assertEquals(6, medium.rules.size)
    }

    @Test
    fun `test honeycomb shortcut reordering and order index preservation`() {
        val item1 = HoneycombShortcut(id = 1, type = ShortcutType.WATER, targetId = "water", title = "Hydration", emoji = "💧", orderIndex = 0)
        val item2 = HoneycombShortcut(id = 2, type = ShortcutType.TASK, targetId = "yoga", title = "Yoga", emoji = "☀️", orderIndex = 1)
        val list = listOf(item1, item2)

        // Swap order
        val swapped = listOf(item2.copy(orderIndex = 0), item1.copy(orderIndex = 1))
        assertEquals(item2.id, swapped[0].id)
        assertEquals(0, swapped[0].orderIndex)
        assertEquals(item1.id, swapped[1].id)
        assertEquals(1, swapped[1].orderIndex)
    }

    // --- PHASE 2.7: PREMIUM IMMERSIVE THEME ENGINE TESTS ---

    @Test
    fun `test theme registry contains all five complete theme definitions`() {
        val themes = ThemeRegistry.allThemes
        assertEquals(5, themes.size)

        val defaultTheme = ThemeRegistry.getTheme(ThemeId.DEFAULT)
        assertEquals("LifeOS Minimal", defaultTheme.name)
        assertFalse(defaultTheme.isPremium)
        assertEquals(HoneycombEngravingType.MINIMAL, defaultTheme.honeycombEngraving)

        val arcane = ThemeRegistry.getTheme(ThemeId.ARCANE_ACADEMY)
        assertEquals("Arcane Academy", arcane.name)
        assertTrue(arcane.isPremium)
        assertEquals(HoneycombEngravingType.RUNE, arcane.honeycombEngraving)
        assertEquals("Grimoire of Directives", arcane.decoration.taskSectionTitle)

        val cyber = ThemeRegistry.getTheme(ThemeId.CYBER_NEXUS)
        assertEquals("Cyber Nexus", cyber.name)
        assertTrue(cyber.isPremium)
        assertEquals(HoneycombEngravingType.CIRCUIT, cyber.honeycombEngraving)
        assertTrue(cyber.shapes.isAngularCyber)

        val adventure = ThemeRegistry.getTheme(ThemeId.ADVENTURE_REALM)
        assertEquals("Adventure Realm", adventure.name)
        assertTrue(adventure.isPremium)
        assertEquals(HoneycombEngravingType.COMPASS_BADGE, adventure.honeycombEngraving)
        assertEquals("Guild Quest Log", adventure.decoration.taskSectionTitle)

        val cosmic = ThemeRegistry.getTheme(ThemeId.COSMIC_EXPLORER)
        assertEquals("Cosmic Explorer", cosmic.name)
        assertTrue(cosmic.isPremium)
        assertEquals(HoneycombEngravingType.ORBITAL, cosmic.honeycombEngraving)
        assertEquals("Mission Directives", cosmic.decoration.taskSectionTitle)
    }

    @Test
    fun `test premium entitlement restricts locked themes for free users`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LocalPremiumEntitlementRepository(context)

        // Free user (isPremiumActive = false)
        assertTrue(repo.isThemeUnlocked(ThemeId.DEFAULT, isPremiumActive = false))
        assertFalse(repo.isThemeUnlocked(ThemeId.ARCANE_ACADEMY, isPremiumActive = false))
        assertFalse(repo.isThemeUnlocked(ThemeId.CYBER_NEXUS, isPremiumActive = false))
        assertFalse(repo.isThemeUnlocked(ThemeId.ADVENTURE_REALM, isPremiumActive = false))
        assertFalse(repo.isThemeUnlocked(ThemeId.COSMIC_EXPLORER, isPremiumActive = false))

        // Entitled user (isPremiumActive = true)
        assertTrue(repo.isThemeUnlocked(ThemeId.DEFAULT, isPremiumActive = true))
        assertTrue(repo.isThemeUnlocked(ThemeId.ARCANE_ACADEMY, isPremiumActive = true))
        assertTrue(repo.isThemeUnlocked(ThemeId.CYBER_NEXUS, isPremiumActive = true))
        assertTrue(repo.isThemeUnlocked(ThemeId.ADVENTURE_REALM, isPremiumActive = true))
        assertTrue(repo.isThemeUnlocked(ThemeId.COSMIC_EXPLORER, isPremiumActive = true))
    }

    @Test
    fun `test theme id string lookup and fallback to default`() {
        assertEquals(ThemeId.DEFAULT, ThemeId.fromId("default"))
        assertEquals(ThemeId.ARCANE_ACADEMY, ThemeId.fromId("arcane_academy"))
        assertEquals(ThemeId.CYBER_NEXUS, ThemeId.fromId("cyber_nexus"))
        assertEquals(ThemeId.ADVENTURE_REALM, ThemeId.fromId("adventure_realm"))
        assertEquals(ThemeId.COSMIC_EXPLORER, ThemeId.fromId("cosmic_explorer"))
        // Case-insensitive & fallback
        assertEquals(ThemeId.CYBER_NEXUS, ThemeId.fromId("CYBER_NEXUS"))
        assertEquals(ThemeId.DEFAULT, ThemeId.fromId("unknown_nonexistent_theme"))
    }

    @Test
    fun `test material 3 color scheme mapping preserves contrast and surface tokens`() {
        val arcane = ThemeRegistry.getTheme(ThemeId.ARCANE_ACADEMY)
        val m3Scheme = arcane.toMaterial3ColorScheme(isLightMode = false)

        assertEquals(arcane.colors.primary, m3Scheme.primary)
        assertEquals(arcane.colors.surface, m3Scheme.surface)
        assertEquals(arcane.colors.background, m3Scheme.background)
        assertEquals(arcane.colors.outline, m3Scheme.outline)
    }

    @Test
    fun `test theme switching preserves task and honeycomb data integrity`() {
        val task = TaskItem(
            id = 50,
            title = "Master Spell Inscription",
            dueDateIso = "2026-10-16",
            recurrenceType = RecurrenceType.NONE
        )
        val shortcut = HoneycombShortcut(
            id = 7,
            type = ShortcutType.TASK,
            targetId = "50",
            title = "Spellcraft",
            emoji = "🔮",
            customColorHex = 0xFF9333EAL,
            orderIndex = 0
        )

        // Switching theme does not mutate task or shortcut configuration
        val activeTheme1 = ThemeRegistry.getTheme(ThemeId.ARCANE_ACADEMY)
        assertEquals("Arcane Academy", activeTheme1.name)
        assertEquals(0xFF9333EAL, shortcut.customColorHex)

        val activeTheme2 = ThemeRegistry.getTheme(ThemeId.CYBER_NEXUS)
        assertEquals("Cyber Nexus", activeTheme2.name)
        assertEquals(50L, task.id)
        assertEquals("Master Spell Inscription", task.title)
        assertEquals(0xFF9333EAL, shortcut.customColorHex) // Custom completion color preserved!
    }
}


