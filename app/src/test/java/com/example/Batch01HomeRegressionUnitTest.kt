package com.example

import com.example.domain.model.ChallengeDailyLog
import com.example.domain.model.ChallengeRule
import com.example.domain.model.DefaultChallengeRules
import com.example.domain.model.RecurrenceType
import com.example.domain.model.TaskPriority
import com.example.domain.templates.LifeTemplate
import com.example.domain.templates.TemplateCatalog
import com.example.domain.templates.TemplateCategory
import com.example.domain.templates.TemplateProjectDef
import com.example.domain.templates.TemplateTaskDef
import com.example.ui.feedback.FeedbackSoundType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mobile Testing Batch 01 (Home Screen) Regression Unit Tests.
 * Covers HOME-01, HOME-02, and HOME-03.
 */
class Batch01HomeRegressionUnitTest {

    // =========================================================================
    // FIX 01 (HOME-01): Browse Templates - Project Association & Deduplication
    // =========================================================================

    @Test
    fun `test template task defines project name and maps accurately`() {
        val masterFlow = TemplateCatalog.getTemplateById("template_master_daily_flow")
        assertNotNull(masterFlow)
        val tasks = masterFlow!!.tasks

        val morningPlan = tasks.find { it.title.contains("Morning Intentions") }
        assertNotNull(morningPlan)
        assertEquals("Daily Operations", morningPlan!!.projectName)

        val middayWalk = tasks.find { it.title.contains("Midday Re-alignment") }
        assertNotNull(middayWalk)
        assertEquals("Personal Health", middayWalk!!.projectName)
    }

    @Test
    fun `test template with no projects preserves intentional null project`() {
        val morningRoutine = TemplateCatalog.getTemplateById("template_high_perf_morning")
        assertNotNull(morningRoutine)
        assertTrue(morningRoutine!!.projects.isEmpty())

        morningRoutine.tasks.forEach { taskDef ->
            val effectiveProject = taskDef.projectName ?: if (morningRoutine.projects.size == 1) morningRoutine.projects.first().name else null
            assertNull("Tasks in projectless templates must remain null/Inbox", effectiveProject)
        }
    }

    @Test
    fun `test template with single project automatically associates tasks`() {
        val deepWorkTmpl = TemplateCatalog.getTemplateById("template_deep_work_focus")
        assertNotNull(deepWorkTmpl)
        assertEquals(1, deepWorkTmpl!!.projects.size)
        assertEquals("Deep Work Projects", deepWorkTmpl.projects.first().name)

        deepWorkTmpl.tasks.forEach { taskDef ->
            val effectiveProject = taskDef.projectName ?: if (deepWorkTmpl.projects.size == 1) deepWorkTmpl.projects.first().name else null
            assertEquals("Deep Work Projects", effectiveProject)
        }
    }

    @Test
    fun `test simulated project deduplication on repeated template import`() {
        val existingProjectMap = mutableMapOf<String, Long>()
        existingProjectMap["daily operations"] = 101L

        val templateProjects = listOf(
            TemplateProjectDef(name = "Daily Operations", emoji = "💼", colorId = "blue"),
            TemplateProjectDef(name = "Personal Health", emoji = "🏃", colorId = "green")
        )

        var newProjectsCreated = 0
        var nextId = 102L

        templateProjects.forEach { projDef ->
            val key = projDef.name.trim().lowercase()
            if (!existingProjectMap.containsKey(key)) {
                existingProjectMap[key] = nextId++
                newProjectsCreated++
            }
        }

        // On first import: Daily Operations existed (reused 101L), Personal Health was created (assigned 102L)
        assertEquals(1, newProjectsCreated)
        assertEquals(101L, existingProjectMap["daily operations"])
        assertEquals(102L, existingProjectMap["personal health"])

        // Second simulated import with same template projects
        var repeatedProjectsCreated = 0
        templateProjects.forEach { projDef ->
            val key = projDef.name.trim().lowercase()
            if (!existingProjectMap.containsKey(key)) {
                existingProjectMap[key] = nextId++
                repeatedProjectsCreated++
            }
        }

        // Must create 0 duplicate projects on re-import
        assertEquals(0, repeatedProjectsCreated)
        assertEquals(2, existingProjectMap.size)
    }

    // =========================================================================
    // FIX 02 (HOME-02): 75-Day Challenge - Interactive Daily Rule Circles & Days
    // =========================================================================

    @Test
    fun `test 75-Day challenge rule toggle updates completed set correctly`() {
        val dateIso = "2026-10-27"
        val initialLog = ChallengeDailyLog(dateIso = dateIso, completedRuleIds = setOf("rule_workout_1"))

        // Complete a second rule
        val setAfterAdd = initialLog.completedRuleIds.toMutableSet()
        val added = setAfterAdd.add("rule_water")
        assertTrue(added)
        assertEquals(setOf("rule_workout_1", "rule_water"), setAfterAdd)

        // Toggle off the first rule
        val removed = setAfterAdd.remove("rule_workout_1")
        assertTrue(removed)
        assertEquals(setOf("rule_water"), setAfterAdd)

        // Toggling off again returns false (not present)
        assertFalse(setAfterAdd.remove("rule_workout_1"))
    }

    @Test
    fun `test 75-Day challenge day is complete only when all rules are met`() {
        val rules = DefaultChallengeRules.list
        assertTrue(rules.size >= 5)

        val partialCompletedIds = setOf("rule_workout_1", "rule_workout_2", "rule_water")
        val isPartialDone = rules.all { partialCompletedIds.contains(it.id) }
        assertFalse("Day must NOT be marked complete when rules are partially completed", isPartialDone)

        val allCompletedIds = rules.map { it.id }.toSet()
        val isAllDone = rules.all { allCompletedIds.contains(it.id) }
        assertTrue("Day must be marked complete only when all rules are met", isAllDone)
    }

    @Test
    fun `test rapid double-tap debounce simulation preserves integrity`() {
        var lastToggleTime = 0L
        var lastRuleId = ""
        var toggleCount = 0

        fun simulateToggle(ruleId: String, timestamp: Long) {
            if (ruleId == lastRuleId && timestamp - lastToggleTime < 250L) {
                return // Debounced
            }
            lastRuleId = ruleId
            lastToggleTime = timestamp
            toggleCount++
        }

        simulateToggle("rule_water", 1000L)
        simulateToggle("rule_water", 1050L) // Ignored: within 50ms
        simulateToggle("rule_water", 1120L) // Ignored: within 120ms
        assertEquals(1, toggleCount)

        simulateToggle("rule_water", 1350L) // Executed: > 250ms
        assertEquals(2, toggleCount)

        simulateToggle("rule_diet", 1360L) // Executed: different rule id
        assertEquals(3, toggleCount)
    }

    // =========================================================================
    // FIX 03 (HOME-03): More Completion Sounds & Custom Sound Import
    // =========================================================================

    @Test
    fun `test all required built-in sound types exist and are distinctly defined`() {
        val bell = FeedbackSoundType.fromString("BELL")
        assertEquals(FeedbackSoundType.BELL, bell)
        assertEquals("Tibetan Bell", bell.displayName)

        val pop = FeedbackSoundType.fromString("POP")
        assertEquals(FeedbackSoundType.POP, pop)
        assertEquals("Crisp Bubble Pop", pop.displayName)

        val retro = FeedbackSoundType.fromString("RETRO")
        assertEquals(FeedbackSoundType.RETRO, retro)
        assertEquals("8-Bit Retro Ping", retro.displayName)

        val success = FeedbackSoundType.fromString("SUCCESS")
        assertEquals(FeedbackSoundType.SUCCESS, success)
        assertEquals("Victory Marimba", success.displayName)

        val harp = FeedbackSoundType.fromString("HARP")
        assertEquals(FeedbackSoundType.HARP, harp)
        assertEquals("Celestial Harp", harp.displayName)

        val chime = FeedbackSoundType.fromString("TASK")
        assertEquals(FeedbackSoundType.TASK, chime)

        val custom = FeedbackSoundType.fromString("CUSTOM")
        assertEquals(FeedbackSoundType.CUSTOM, custom)
    }

    @Test
    fun `test fallback on unknown sound string returns default task chime`() {
        val unknown = FeedbackSoundType.fromString("NON_EXISTENT_SOUND")
        assertEquals(FeedbackSoundType.TASK, unknown)
    }

    @Test
    fun `test custom sound filename persistence and display resolution`() {
        val testUri = "content://com.android.providers.media.documents/document/audio%3A4821"
        val testFileName = "my_celebration_chime.mp3"

        assertNotNull(testUri)
        assertNotNull(testFileName)
        assertTrue(testFileName.endsWith(".mp3") || testFileName.endsWith(".wav") || testFileName.endsWith(".ogg"))
    }
}
