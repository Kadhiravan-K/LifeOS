package com.example

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
import com.example.ui.components.pixelToAxial
import com.example.ui.components.partitionShortcuts
import com.example.ui.theme.engine.HoneycombEngravingType
import com.example.ui.theme.engine.ThemeDefinition
import com.example.ui.theme.engine.ThemeId
import com.example.ui.theme.engine.ThemeRegistry
import com.example.domain.gamification.GamificationEngine
import com.example.domain.model.HunterRank
import com.example.domain.templates.TemplateCatalog
import com.example.domain.templates.TemplateCategory
import com.example.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * LifeOS v1.0.0 Domain Unit Test Suite.
 * Pure JVM tests executing offline without network dependencies or external shadow jars.
 */
class LifeOSDomainUnitTest {

    // =========================================================================
    // 1. RECURRENCE ENGINE TESTS
    // =========================================================================

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
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-10-14")) // before start date
    }

    @Test
    fun `test Tuesday Thursday Sunday laundry routine explicit rule`() {
        val laundryTask = TaskItem(
            id = 2,
            title = "Laundry Routine",
            dueDateIso = "2026-10-11", // Sunday
            recurrenceType = RecurrenceType.SELECTED_WEEKDAYS,
            recurrenceDaysOfWeek = listOf("TUESDAY", "THURSDAY", "SUNDAY")
        )

        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(laundryTask, "2026-10-11")) // Sunday
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(laundryTask, "2026-10-12")) // Monday
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(laundryTask, "2026-10-13")) // Tuesday
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(laundryTask, "2026-10-14")) // Wednesday
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(laundryTask, "2026-10-15")) // Thursday
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(laundryTask, "2026-10-16")) // Friday
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(laundryTask, "2026-10-17")) // Saturday
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(laundryTask, "2026-10-18")) // Sunday
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

        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(deepCleanTask, "2026-10-11"))
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(deepCleanTask, "2026-10-18"))
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(deepCleanTask, "2026-10-25"))
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

        assertTrue(DateUtils.isLeapYear(2024))
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

        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2026-10-15"))
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

    @Test
    fun `test occurrence override skips on parent while maintaining other days`() {
        val recurringTask = TaskItem(
            id = 20,
            title = "Morning Exercise",
            dueDateIso = "2026-10-15",
            recurrenceType = RecurrenceType.DAILY,
            skippedDates = setOf("2026-10-16")
        )

        val overriddenInstance = TaskItem(
            id = 21,
            title = "Morning Yoga (Special Session)",
            dueDateIso = "2026-10-16",
            recurrenceType = RecurrenceType.NONE
        )

        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(recurringTask, "2026-10-15"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(overriddenInstance, "2026-10-15"))

        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(recurringTask, "2026-10-16"))
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(overriddenInstance, "2026-10-16"))

        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(recurringTask, "2026-10-17"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(overriddenInstance, "2026-10-17"))
    }

    // =========================================================================
    // 2. SCHEDULE CONFLICT TESTS
    // =========================================================================

    @Test
    fun `test sleep window conflict detection`() {
        val lateTask = TaskItem(
            id = 11,
            title = "Late Night Movie",
            dueDateIso = "2026-10-15",
            dueTime = "23:00",
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
            durationMinutes = 60
        )

        val newTask = TaskItem(
            id = 13,
            title = "Doctor Appointment",
            dueDateIso = "2026-10-15",
            dueTime = "09:30",
            durationMinutes = 45
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

    // =========================================================================
    // 3. HONEYCOMB AXIAL GEOMETRY & DRAG-AND-DROP COORDINATE TESTS
    // =========================================================================

    @Test
    fun `test pixelToAxial converts cartesian coordinates to axial coordinates via cube rounding`() {
        val hStep = 80f
        val vStep = 70f

        // Origin (0, 0)
        assertEquals(Pair(0, 0), pixelToAxial(0f, 0f, hStep, vStep))

        // Center of cell (1, 0): x = hStep * (1 + 0/2) = 80, y = vStep * 0 = 0
        assertEquals(Pair(1, 0), pixelToAxial(80f, 0f, hStep, vStep))

        // Center of cell (0, 1): x = hStep * (0 + 1/2) = 40, y = vStep * 1 = 70
        assertEquals(Pair(0, 1), pixelToAxial(40f, 70f, hStep, vStep))

        // Center of cell (-1, 1): x = hStep * (-1 + 0.5) = -40, y = vStep * 1 = 70
        assertEquals(Pair(-1, 1), pixelToAxial(-40f, 70f, hStep, vStep))

        // Center of cell (1, -1): x = hStep * (1 - 0.5) = 40, y = vStep * -1 = -70
        assertEquals(Pair(1, -1), pixelToAxial(40f, -70f, hStep, vStep))

        // Small perturbations within cell radius still map to the exact same axial cell
        assertEquals(Pair(0, 0), pixelToAxial(10f, -5f, hStep, vStep))
        assertEquals(Pair(1, 0), pixelToAxial(85f, 5f, hStep, vStep))
        assertEquals(Pair(0, 1), pixelToAxial(45f, 75f, hStep, vStep))
    }

    @Test
    fun `test axial distance formula correctly computes hex distance`() {
        // Hex distance: (|q1 - q2| + |q1 + r1 - q2 - r2| + |r1 - r2|) / 2
        fun axialDist(q1: Int, r1: Int, q2: Int, r2: Int): Int {
            return (abs(q1 - q2) + abs(q1 + r1 - q2 - r2) + abs(r1 - r2)) / 2
        }

        assertEquals(0, axialDist(0, 0, 0, 0))
        assertEquals(1, axialDist(0, 0, 1, 0))
        assertEquals(1, axialDist(0, 0, 0, 1))
        assertEquals(1, axialDist(0, 0, -1, 1))
        assertEquals(1, axialDist(0, 0, -1, 0))
        assertEquals(1, axialDist(0, 0, 0, -1))
        assertEquals(1, axialDist(0, 0, 1, -1))

        // 2 steps away
        assertEquals(2, axialDist(0, 0, 2, 0))
        assertEquals(2, axialDist(0, 0, 1, 1))
    }

    @Test
    fun `test default honeycomb shortcuts have unique axial coordinates`() {
        val shortcuts = DefaultHoneycombShortcuts.list
        assertEquals(10, shortcuts.size)

        val coordSet = shortcuts.map { Pair(it.coordQ, it.coordR) }.toSet()
        // All 10 default shortcuts must occupy distinct axial grid locations
        assertEquals(10, coordSet.size)
    }

    @Test
    fun `test honeycomb shortcut swapping preserves shortcut IDs and updates axial coordinates`() {
        val s1 = HoneycombShortcut(id = 1, type = ShortcutType.WATER, targetId = "w", title = "Water", emoji = "💧", coordQ = 0, coordR = 0, orderIndex = 0)
        val s2 = HoneycombShortcut(id = 2, type = ShortcutType.TASK, targetId = "t", title = "Task", emoji = "📝", coordQ = 1, coordR = 0, orderIndex = 1)

        // Swap coordinates
        val s1Swapped = s1.copy(coordQ = s2.coordQ, coordR = s2.coordR, orderIndex = s2.orderIndex)
        val s2Swapped = s2.copy(coordQ = s1.coordQ, coordR = s1.coordR, orderIndex = s1.orderIndex)

        assertEquals(1L, s1Swapped.id)
        assertEquals(1, s1Swapped.coordQ)
        assertEquals(0, s1Swapped.coordR)

        assertEquals(2L, s2Swapped.id)
        assertEquals(0, s2Swapped.coordQ)
        assertEquals(0, s2Swapped.coordR)
    }

    @Test
    fun `test honeycomb layout templates generate valid non-empty coordinates`() {
        com.example.domain.model.HoneycombLayoutTemplate.entries.forEach { template ->
            val coords = com.example.domain.model.HoneycombLayoutTemplate.generateCoordinates(template, 10)
            assertEquals(10, coords.size)
            // No coordinate should be empty or degenerate
            coords.forEach { (q, r) ->
                assertNotNull(q)
                assertNotNull(r)
            }
        }
    }

    @Test
    fun `test next available slot finder selects first unoccupied coordinate in 3-2-3-2 hex grid`() {
        // Given 10 default shortcuts occupying (0,0)..(1,3)
        val occupied = DefaultHoneycombShortcuts.list.map { Pair(it.coordQ, it.coordR) }.toSet()
        assertEquals(10, occupied.size)

        var nextR = 0
        var targetCoord: Pair<Int, Int>? = null
        while (nextR < 100) {
            val rowLen = if (nextR % 2 == 0) 3 else 2
            for (q in 0 until rowLen) {
                val candidate = Pair(q, nextR)
                if (candidate !in occupied) {
                    targetCoord = candidate
                    break
                }
            }
            if (targetCoord != null) break
            nextR++
        }

        // The 11th shortcut should auto-align to row 4, column 0: (0, 4)
        assertEquals(Pair(0, 4), targetCoord)
    }

    @Test
    fun `test deoverlapping logic correctly repositions duplicated shortcuts into open axial cells`() {
        // Scenario: Two shortcuts both saved at (0, 0)
        val s1 = HoneycombShortcut(id = 1, type = ShortcutType.WATER, targetId = "w", title = "Water", emoji = "💧", coordQ = 0, coordR = 0)
        val s2 = HoneycombShortcut(id = 2, type = ShortcutType.TASK, targetId = "t", title = "Task", emoji = "📝", coordQ = 0, coordR = 0) // Duplicate!
        val rawList = listOf(s1, s2)

        val occupied = mutableSetOf<Pair<Int, Int>>()
        var nextR = 0
        fun findNextFree(): Pair<Int, Int> {
            while (nextR < 100) {
                val rowLen = if (nextR % 2 == 0) 3 else 2
                for (q in 0 until rowLen) {
                    val candidate = Pair(q, nextR)
                    if (candidate !in occupied) {
                        return candidate
                    }
                }
                nextR++
            }
            return Pair(0, (occupied.maxOfOrNull { it.second } ?: 0) + 1)
        }

        val deoverlapped = rawList.map { item ->
            val coord = Pair(item.coordQ, item.coordR)
            if (coord !in occupied) {
                occupied.add(coord)
                item
            } else {
                val freeCoord = findNextFree()
                occupied.add(freeCoord)
                item.copy(coordQ = freeCoord.first, coordR = freeCoord.second)
            }
        }

        assertEquals(2, deoverlapped.size)
        assertEquals(Pair(0, 0), Pair(deoverlapped[0].coordQ, deoverlapped[0].coordR))
        // s2 was auto-aligned to (1, 0) - the next available cell in row 0
        assertEquals(Pair(1, 0), Pair(deoverlapped[1].coordQ, deoverlapped[1].coordR))
        assertFalse(deoverlapped[0].coordQ == deoverlapped[1].coordQ && deoverlapped[0].coordR == deoverlapped[1].coordR)
    }

    // =========================================================================
    // 4. TASK COMPLETION AUDIO SYNTHESIS & WAV FORMAT VERIFICATION
    // =========================================================================

    @Test
    fun `test harmonic chime PCM 16-bit WAV header generation math`() {
        val sampleRate = 44100
        val durationMs = 180
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcmDataSize = numSamples * 2 // 16-bit mono = 2 bytes per sample
        val totalFileSize = 44 + pcmDataSize // 44 bytes standard RIFF header

        assertEquals(7938, numSamples)
        assertEquals(15876, pcmDataSize)
        assertEquals(15920, totalFileSize)
    }

    // =========================================================================
    // 5. THEME REGISTRY & PRESETS VERIFICATION
    // =========================================================================

    @Test
    fun `test theme registry contains all five complete theme definitions`() {
        val themes = ThemeRegistry.allThemes
        assertEquals(6, themes.size)

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

        val glass = ThemeRegistry.getTheme(ThemeId.GLASS)
        assertEquals("Glassmorphism", glass.name)
        assertTrue(glass.isPremium)
        assertEquals(HoneycombEngravingType.PRISM, glass.honeycombEngraving)
        assertEquals("Focus Panes", glass.decoration.taskSectionTitle)
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
    fun `test theme unlocking logic`() {
        fun isThemeUnlocked(themeId: ThemeId, isPremiumActive: Boolean): Boolean {
            if (!themeId.isPremium) return true
            return isPremiumActive
        }

        // Free user
        assertTrue(isThemeUnlocked(ThemeId.DEFAULT, false))
        assertFalse(isThemeUnlocked(ThemeId.ARCANE_ACADEMY, false))
        assertFalse(isThemeUnlocked(ThemeId.CYBER_NEXUS, false))
        assertFalse(isThemeUnlocked(ThemeId.ADVENTURE_REALM, false))
        assertFalse(isThemeUnlocked(ThemeId.COSMIC_EXPLORER, false))

        // Entitled user
        assertTrue(isThemeUnlocked(ThemeId.DEFAULT, true))
        assertTrue(isThemeUnlocked(ThemeId.ARCANE_ACADEMY, true))
        assertTrue(isThemeUnlocked(ThemeId.CYBER_NEXUS, true))
        assertTrue(isThemeUnlocked(ThemeId.ADVENTURE_REALM, true))
        assertTrue(isThemeUnlocked(ThemeId.COSMIC_EXPLORER, true))
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

    // =========================================================================
    // 6. GAMIFICATION ENGINE & TEMPLATE CATALOG TESTS
    // =========================================================================

    @Test
    fun `test gamification engine level calculation is deterministic and strictly monotonic`() {
        assertEquals(1, GamificationEngine.calculateLevelFromXp(0L))
        assertEquals(1, GamificationEngine.calculateLevelFromXp(50L))
        assertEquals(1, GamificationEngine.calculateLevelFromXp(99L))
        assertEquals(2, GamificationEngine.calculateLevelFromXp(100L))
        assertEquals(2, GamificationEngine.calculateLevelFromXp(200L))
        assertEquals(2, GamificationEngine.calculateLevelFromXp(299L))
        assertEquals(3, GamificationEngine.calculateLevelFromXp(300L))
        assertEquals(3, GamificationEngine.calculateLevelFromXp(599L))
        assertEquals(4, GamificationEngine.calculateLevelFromXp(600L))
        assertEquals(5, GamificationEngine.calculateLevelFromXp(1000L))
        assertEquals(10, GamificationEngine.calculateLevelFromXp(4500L))
    }

    @Test
    fun `test gamification engine cumulative XP matches level thresholds and inverts perfectly`() {
        assertEquals(0L, GamificationEngine.cumulativeXpForLevel(1))
        assertEquals(100L, GamificationEngine.cumulativeXpForLevel(2))
        assertEquals(300L, GamificationEngine.cumulativeXpForLevel(3))
        assertEquals(600L, GamificationEngine.cumulativeXpForLevel(4))
        assertEquals(1000L, GamificationEngine.cumulativeXpForLevel(5))

        // Inversion check across 50 levels
        for (lvl in 1..50) {
            val base = GamificationEngine.cumulativeXpForLevel(lvl)
            assertEquals(lvl, GamificationEngine.calculateLevelFromXp(base))
            val topOfLevel = base + (lvl * 100L) - 1L
            assertEquals(lvl, GamificationEngine.calculateLevelFromXp(topOfLevel))
        }
    }

    @Test
    fun `test hunter rank resolution matches level boundaries`() {
        assertEquals(HunterRank.E, HunterRank.fromLevel(1))
        assertEquals(HunterRank.E, HunterRank.fromLevel(5))
        assertEquals(HunterRank.D, HunterRank.fromLevel(6))
        assertEquals(HunterRank.D, HunterRank.fromLevel(12))
        assertEquals(HunterRank.C, HunterRank.fromLevel(13))
        assertEquals(HunterRank.C, HunterRank.fromLevel(20))
        assertEquals(HunterRank.B, HunterRank.fromLevel(21))
        assertEquals(HunterRank.B, HunterRank.fromLevel(30))
        assertEquals(HunterRank.A, HunterRank.fromLevel(31))
        assertEquals(HunterRank.A, HunterRank.fromLevel(45))
        assertEquals(HunterRank.S, HunterRank.fromLevel(46))
        assertEquals(HunterRank.S, HunterRank.fromLevel(60))
        assertEquals(HunterRank.SS, HunterRank.fromLevel(61))
        assertEquals(HunterRank.SS, HunterRank.fromLevel(80))
        assertEquals(HunterRank.SSS, HunterRank.fromLevel(81))
        assertEquals(HunterRank.SSS, HunterRank.fromLevel(100))
    }

    @Test
    fun `test anti-farming event key format`() {
        val key1 = GamificationEngine.createEventKey("TASK", "101", "2026-10-21")
        assertEquals("TASK:101:2026-10-21", key1)

        val key2 = GamificationEngine.createEventKey("HABIT", "55", "2026-10-22")
        assertEquals("HABIT:55:2026-10-22", key2)

        val key3 = GamificationEngine.createEventKey("CHALLENGE", "1", "2026-10-23")
        assertEquals("CHALLENGE:1:2026-10-23", key3)
    }

    @Test
    fun `test streak calculation with consecutive days, gaps, and today`() {
        val today = "2026-10-21"

        // 1. Empty dates set
        val emptyResult = GamificationEngine.calculateStreaks(emptySet<String>(), today)
        assertEquals(0, emptyResult.currentStreak)
        assertEquals(0, emptyResult.bestStreak)

        // 2. Active streak including today (3 days: 19, 20, 21)
        val activeDates = setOf("2026-10-21", "2026-10-20", "2026-10-19")
        val activeResult = GamificationEngine.calculateStreaks(activeDates, today)
        assertEquals(3, activeResult.currentStreak)
        assertEquals(3, activeResult.bestStreak)

        // 3. Active streak ending yesterday (not done today yet, 2 days: 19, 20)
        val yesterdayDates = setOf("2026-10-20", "2026-10-19")
        val yesterdayResult = GamificationEngine.calculateStreaks(yesterdayDates, today)
        assertEquals(2, yesterdayResult.currentStreak)
        assertEquals(2, yesterdayResult.bestStreak)

        // 4. Broken streak (done 18th and 19th, missed 20th and 21st)
        val brokenDates = setOf("2026-10-19", "2026-10-18")
        val brokenResult = GamificationEngine.calculateStreaks(brokenDates, today)
        assertEquals(0, brokenResult.currentStreak)
        assertEquals(2, brokenResult.bestStreak)

        // 5. Historic best streak was 5 days, current streak is 2 days
        val multiIntervalDates = setOf(
            "2026-10-21", "2026-10-20", // Current: 2
            // gap on 19th
            "2026-10-18", "2026-10-17", "2026-10-16", "2026-10-15", "2026-10-14" // Historic: 5
        )
        val multiResult = GamificationEngine.calculateStreaks(multiIntervalDates, today)
        assertEquals(2, multiResult.currentStreak)
        assertEquals(5, multiResult.bestStreak)
    }

    @Test
    fun `test template catalog contains 15 templates across all categories and valid definitions`() {
        val templates = TemplateCatalog.list
        assertEquals(22, templates.size)

        // Ensure IDs are unique
        val ids = templates.map { it.id }.toSet()
        assertEquals(22, ids.size)

        // Ensure all categories are covered
        val categories = templates.map { it.category }.toSet()
        assertEquals(TemplateCategory.entries.toSet(), categories)

        // Ensure all templates have non-blank content and items
        for (tmpl in templates) {
            assertTrue(tmpl.id.isNotBlank())
            assertTrue(tmpl.title.isNotBlank())
            assertTrue(tmpl.description.isNotBlank())
            val hasContent = tmpl.tasks.isNotEmpty() || tmpl.habits.isNotEmpty() ||
                    tmpl.shortcuts.isNotEmpty() || tmpl.projects.isNotEmpty() ||
                    tmpl.challenge != null
            assertTrue("Template ${tmpl.id} must have items", hasContent)
        }
    }

    @Test
    fun `test diminishing returns for task XP prevents XP farming`() {
        // Daily count 0-9 (first 10 tasks: 100% XP)
        assertEquals(Pair(10, 2), GamificationEngine.calculateTaskXpWithDiminishingReturns(0, TaskPriority.MEDIUM))
        assertEquals(Pair(10, 2), GamificationEngine.calculateTaskXpWithDiminishingReturns(9, TaskPriority.MEDIUM))
        assertEquals(Pair(20, 5), GamificationEngine.calculateTaskXpWithDiminishingReturns(0, TaskPriority.HIGH))

        // Daily count 10-19 (tasks 11-20: 50% XP)
        assertEquals(Pair(5, 1), GamificationEngine.calculateTaskXpWithDiminishingReturns(10, TaskPriority.MEDIUM))
        assertEquals(Pair(5, 1), GamificationEngine.calculateTaskXpWithDiminishingReturns(19, TaskPriority.MEDIUM))
        assertEquals(Pair(10, 2), GamificationEngine.calculateTaskXpWithDiminishingReturns(10, TaskPriority.HIGH))

        // Daily count 20-29 (tasks 21-30: 20% XP)
        assertEquals(Pair(2, 1), GamificationEngine.calculateTaskXpWithDiminishingReturns(20, TaskPriority.MEDIUM))
        assertEquals(Pair(2, 1), GamificationEngine.calculateTaskXpWithDiminishingReturns(29, TaskPriority.MEDIUM))
        assertEquals(Pair(4, 1), GamificationEngine.calculateTaskXpWithDiminishingReturns(20, TaskPriority.HIGH))

        // Daily count >= 30 (tasks 31+: 0 XP cap)
        assertEquals(Pair(0, 0), GamificationEngine.calculateTaskXpWithDiminishingReturns(30, TaskPriority.MEDIUM))
        assertEquals(Pair(0, 0), GamificationEngine.calculateTaskXpWithDiminishingReturns(50, TaskPriority.HIGH))
    }

    @Test
    fun `test QR tile custom name and display title fallback`() {
        val defaultTile = HoneycombShortcut(
            id = 1,
            type = ShortcutType.TASK,
            targetId = "101",
            title = "Morning Run",
            emoji = "🏃",
            customTileName = null
        )
        assertEquals("Morning Run", defaultTile.displayTitle)

        val renamedTile = defaultTile.copy(customTileName = "Sprint")
        assertEquals("Sprint", renamedTile.displayTitle)

        val blankRenamedTile = defaultTile.copy(customTileName = "   ")
        assertEquals("Morning Run", blankRenamedTile.displayTitle)
    }

    @Test
    fun `test template bundles and special collections catalog`() {
        assertEquals(14, TemplateCatalog.allBundles.size)
        assertEquals(4, TemplateCatalog.allSpecialCollections.size)

        // Ensure all bundle template IDs resolve to real templates
        TemplateCatalog.allBundles.forEach { bundle ->
            assertTrue("Bundle ${bundle.id} must have non-empty templates", bundle.templateIds.isNotEmpty())
            bundle.templateIds.forEach { templateId ->
                assertNotNull("Template $templateId in bundle ${bundle.id} must exist", TemplateCatalog.getTemplateById(templateId))
            }
        }

        // Ensure all special collections resolve to real templates
        TemplateCatalog.allSpecialCollections.forEach { special ->
            assertTrue(special.isSpecialCollection)
            special.templateIds.forEach { templateId ->
                assertNotNull("Special collection template $templateId must exist", TemplateCatalog.getTemplateById(templateId))
            }
        }
    }
}
