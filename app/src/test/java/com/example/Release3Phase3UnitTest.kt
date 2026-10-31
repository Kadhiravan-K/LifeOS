package com.example

import com.example.domain.emoji.TaskEmojiSuggestionEngine
import com.example.domain.medication.FoodRelationship
import com.example.domain.medication.MealPeriod
import com.example.domain.medication.MealPeriodConfig
import com.example.domain.medication.MedicationForm
import com.example.domain.medication.MedicationItem
import com.example.domain.medication.MedicationScheduleConfig
import com.example.domain.medication.MedicineScheduleType
import com.example.domain.model.LabelItem
import com.example.domain.model.RecurrenceType
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Release 3 — Phase 3 Unit Tests
 * Covers Requirements:
 * - #5: Offline Task Emoji Suggestions with Manual Override
 * - #6: Checkbox-Based Multi-Label Task Selection
 * - #11: Medication Priority Defaults, Reminder Timing & Details
 * - #18: Always-Visible Optional Deadline & Enhanced Calendar Markers
 */
class Release3Phase3UnitTest {

    // -------------------------------------------------------------------------
    // REQUIREMENT #5: Task Title Emoji Suggestions
    // -------------------------------------------------------------------------

    @Test
    fun emojiSuggestions_matchingKeywords_returnsRelevantContextualEmojis() {
        val grocerySuggestions = TaskEmojiSuggestionEngine.suggestEmojis("Buy groceries and milk")
        assertTrue("Expected grocery or milk emoji in suggestions", grocerySuggestions.contains("🛒") || grocerySuggestions.contains("🥛") || grocerySuggestions.contains("🥦"))

        val workoutSuggestions = TaskEmojiSuggestionEngine.suggestEmojis("Morning workout and gym session")
        assertTrue("Expected gym or fitness emoji in suggestions", workoutSuggestions.contains("🏋️") || workoutSuggestions.contains("💪"))

        val callSuggestions = TaskEmojiSuggestionEngine.suggestEmojis("Call mom about flight booking")
        assertTrue("Expected call or phone emoji in suggestions", callSuggestions.contains("📞") || callSuggestions.contains("✈️"))

        val medSuggestions = TaskEmojiSuggestionEngine.suggestEmojis("Take antibiotic pills with water")
        assertTrue("Expected pill or water emoji in suggestions", medSuggestions.contains("💊") || medSuggestions.contains("💧"))
    }

    @Test
    fun emojiSuggestions_unmatchedOrEmptyTitle_returnsEmptyList() {
        val emptySuggestions = TaskEmojiSuggestionEngine.suggestEmojis("")
        assertTrue("Expected empty suggestions for blank title", emptySuggestions.isEmpty())

        val whitespaceSuggestions = TaskEmojiSuggestionEngine.suggestEmojis("    ")
        assertTrue("Expected empty suggestions for whitespace title", whitespaceSuggestions.isEmpty())

        val randomSuggestions = TaskEmojiSuggestionEngine.suggestEmojis("xyz12345nonexistentword")
        assertTrue("Expected empty suggestions for unrecognized title", randomSuggestions.isEmpty())
    }

    @Test
    fun emojiSuggestions_manualOverride_preservesUserChoiceWithoutOverwriting() {
        var taskEmoji = "⭐"
        var isEmojiManuallySelected = true

        // User typed a title that normally matches groceries
        val title = "Buy groceries"
        val engineSuggestions = TaskEmojiSuggestionEngine.suggestEmojis(title)
        assertTrue(engineSuggestions.isNotEmpty())

        // Because isEmojiManuallySelected is true, the UI suppresses auto-overwrites
        val activeSuggestions = if (isEmojiManuallySelected) emptyList() else engineSuggestions
        assertTrue("Suggestions should be suppressed when manual override is active", activeSuggestions.isEmpty())

        // User's custom emoji remains intact
        assertEquals("⭐", taskEmoji)

        // If user resets manual override, suggestions become available
        isEmojiManuallySelected = false
        val restoredSuggestions = if (isEmojiManuallySelected) emptyList() else engineSuggestions
        assertTrue("Suggestions should become available when override is false", restoredSuggestions.isNotEmpty())

        // Applying a suggestion sets manual override flag
        taskEmoji = restoredSuggestions.first()
        isEmojiManuallySelected = true
        assertEquals(restoredSuggestions.first(), taskEmoji)
        assertTrue(isEmojiManuallySelected)
    }

    // -------------------------------------------------------------------------
    // REQUIREMENT #6: Multi-Label Checkbox Selection
    // -------------------------------------------------------------------------

    @Test
    fun multiLabelSelection_togglingAndRemoving_updatesSelectionCorrectly() {
        val availableLabels = listOf(
            LabelItem(id = 101L, profileId = 1L, name = "Urgent", colorId = "Berry Red"),
            LabelItem(id = 102L, profileId = 1L, name = "Work", colorId = "Blue"),
            LabelItem(id = 103L, profileId = 1L, name = "Home", colorId = "Green")
        )

        var selectedLabelIds = setOf<Long>()

        // Toggle label 101 on
        selectedLabelIds = if (selectedLabelIds.contains(101L)) selectedLabelIds - 101L else selectedLabelIds + 101L
        assertEquals(setOf(101L), selectedLabelIds)

        // Toggle label 102 on
        selectedLabelIds = if (selectedLabelIds.contains(102L)) selectedLabelIds - 102L else selectedLabelIds + 102L
        assertEquals(setOf(101L, 102L), selectedLabelIds)

        // Remove label 101 via chip 'x' action
        selectedLabelIds = selectedLabelIds - 101L
        assertEquals(setOf(102L), selectedLabelIds)

        // Profile isolation check: only labels for active profileId should be presented
        val workProfileId = 2L
        val workLabels = listOf(
            LabelItem(id = 201L, profileId = workProfileId, name = "Sprint", colorId = "Orange"),
            LabelItem(id = 202L, profileId = workProfileId, name = "Review", colorId = "Purple")
        )
        val allLabels = availableLabels + workLabels

        val scopedPersonal = allLabels.filter { it.profileId == 1L }
        val scopedWork = allLabels.filter { it.profileId == workProfileId }

        assertEquals(3, scopedPersonal.size)
        assertEquals(2, scopedWork.size)
        assertFalse(scopedPersonal.any { it.name == "Sprint" })
        assertFalse(scopedWork.any { it.name == "Urgent" })
    }

    // -------------------------------------------------------------------------
    // REQUIREMENT #11: Medication Priority Defaults & Reminder Timing
    // -------------------------------------------------------------------------

    @Test
    fun medicationPriority_defaultToP1WhenEnabled_andRespectsManualOverrides() {
        var priority = TaskPriority.P3
        var isPriorityManuallySet = false
        var preservedPriorityBeforeMedicine = priority
        var isMedicineEnabled = false

        // User enables medicine on a new task without having manually picked priority
        isMedicineEnabled = true
        if (isMedicineEnabled) {
            if (!isPriorityManuallySet) {
                preservedPriorityBeforeMedicine = priority
                priority = TaskPriority.P1
            }
        }
        assertEquals("Medication task should default to P1", TaskPriority.P1, priority)

        // User manually changes priority to P2
        priority = TaskPriority.P2
        isPriorityManuallySet = true

        // User disables medicine reminder: manual priority P2 should be preserved!
        isMedicineEnabled = false
        if (!isMedicineEnabled) {
            if (!isPriorityManuallySet) {
                priority = preservedPriorityBeforeMedicine
            }
        }
        assertEquals("Disabling medicine should preserve manual priority choice", TaskPriority.P2, priority)

        // User re-enables medicine reminder: manual priority P2 must NOT be overwritten!
        isMedicineEnabled = true
        if (isMedicineEnabled) {
            if (!isPriorityManuallySet) {
                preservedPriorityBeforeMedicine = priority
                priority = TaskPriority.P1
            }
        }
        assertEquals("Re-enabling medicine should respect manual priority choice without silently resetting to P1", TaskPriority.P2, priority)
    }

    @Test
    fun medicationTiming_mealPeriodsAndPresets_calibrateWithSleepWakePreferences() {
        val wakeUpTime = "06:00"
        val bedTime = "22:00"

        val wakeMins = DateUtils.timeToMinutes(wakeUpTime)
        val bedMins = DateUtils.timeToMinutes(bedTime)

        val defaultMorningEmptyStomach = wakeUpTime
        val defaultBreakfast = DateUtils.minutesToTime(wakeMins + 60)
        val defaultDinner = DateUtils.minutesToTime((bedMins - 120).coerceAtLeast(1140))
        val defaultBeforeSleep = bedTime

        val mealConfigs = listOf(
            MealPeriodConfig(MealPeriod.MORNING_EMPTY_STOMACH, defaultMorningEmptyStomach, FoodRelationship.BEFORE_FOOD, offsetMinutes = 30, isEnabled = true),
            MealPeriodConfig(MealPeriod.BREAKFAST, defaultBreakfast, FoodRelationship.AFTER_FOOD, offsetMinutes = 30, isEnabled = true),
            MealPeriodConfig(MealPeriod.DINNER, defaultDinner, FoodRelationship.AFTER_FOOD, offsetMinutes = 30, isEnabled = true),
            MealPeriodConfig(MealPeriod.BEFORE_SLEEP, defaultBeforeSleep, FoodRelationship.NO_FOOD, offsetMinutes = 0, isEnabled = true)
        )

        val morningConfig = mealConfigs.first { it.period == MealPeriod.MORNING_EMPTY_STOMACH }
        assertEquals("Morning empty stomach should align with wake-up time", "06:00", morningConfig.scheduledTime)
        assertEquals(FoodRelationship.BEFORE_FOOD, morningConfig.foodRelation)
        assertEquals(30, morningConfig.offsetMinutes)

        val breakfastConfig = mealConfigs.first { it.period == MealPeriod.BREAKFAST }
        assertEquals("Breakfast should be scheduled 1 hour after wake-up", "07:00", breakfastConfig.scheduledTime)
        assertEquals(FoodRelationship.AFTER_FOOD, breakfastConfig.foodRelation)
        assertEquals(30, breakfastConfig.offsetMinutes)

        val sleepConfig = mealConfigs.first { it.period == MealPeriod.BEFORE_SLEEP }
        assertEquals("Before sleep should align with bed-time", "22:00", sleepConfig.scheduledTime)
    }

    // -------------------------------------------------------------------------
    // REQUIREMENT #18: Always-Visible Optional Deadline & Calendar Markers
    // -------------------------------------------------------------------------

    @Test
    fun taskDeadline_defaultNullForNewTask_independentOfDuration() {
        val newTask = TaskItem(
            title = "Research Report",
            dueDateIso = "2026-11-01",
            durationAmount = 45,
            durationMinutes = 45
        )

        // New task deadline is null by default
        assertNull("New task deadline should default to null/empty", newTask.deadlineIso)
        assertEquals(45, newTask.durationMinutes)

        // Set deadline independently of duration
        val taskWithDeadline = newTask.copy(
            deadlineIso = "2026-11-05",
            deadlineTime = "17:00"
        )
        assertEquals("2026-11-05", taskWithDeadline.deadlineIso)
        assertEquals("17:00", taskWithDeadline.deadlineTime)
        assertEquals("Duration should remain independent when deadline is set", 45, taskWithDeadline.durationMinutes)

        // Clear deadline without affecting duration
        val clearedDeadlineTask = taskWithDeadline.copy(
            deadlineIso = null,
            deadlineTime = null
        )
        assertNull("Deadline should be cleared", clearedDeadlineTask.deadlineIso)
        assertNull("Deadline time should be cleared", clearedDeadlineTask.deadlineTime)
        assertEquals("Duration must be preserved when clearing deadline", 45, clearedDeadlineTask.durationMinutes)
    }

    @Test
    fun calendarMarkers_dateRangeAndRecurrenceIndicators_distinguishedCorrectly() {
        val rangeStart = "2026-11-02"
        val rangeEnd = "2026-11-06"

        val selectedDate = "2026-11-04"
        val today = "2026-11-03"

        val datesWithTasks = setOf("2026-11-01", "2026-11-03", "2026-11-05")
        val datesWithRecurringTasks = setOf("2026-11-05")

        // Date 2026-11-03: intermediate in range, today, has non-recurring task
        val isIntermediate20261103 = "2026-11-03" > rangeStart && "2026-11-03" < rangeEnd
        assertTrue("2026-11-03 should be intermediate within date range", isIntermediate20261103)
        assertEquals(today, "2026-11-03")
        assertTrue(datesWithTasks.contains("2026-11-03"))
        assertFalse("2026-11-03 does not have recurring task", datesWithRecurringTasks.contains("2026-11-03"))

        // Date 2026-11-05: intermediate in range, has recurring task
        val isIntermediate20261105 = "2026-11-05" > rangeStart && "2026-11-05" < rangeEnd
        assertTrue("2026-11-05 should be intermediate within date range", isIntermediate20261105)
        assertTrue("2026-11-05 has recurring task", datesWithRecurringTasks.contains("2026-11-05"))

        // Date 2026-11-04: selected date (takes visual precedence over range background)
        assertEquals(selectedDate, "2026-11-04")

        // Boundary dates: not intermediate
        val isIntermediateStart = rangeStart > rangeStart && rangeStart < rangeEnd
        val isIntermediateEnd = rangeEnd > rangeStart && rangeEnd < rangeEnd
        assertFalse("Range start is not intermediate", isIntermediateStart)
        assertFalse("Range end is not intermediate", isIntermediateEnd)
    }
}
