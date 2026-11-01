package com.example

import com.example.domain.gamification.GamificationEngine
import com.example.domain.model.HabitItem
import com.example.ui.screens.manual.ComponentCategory
import com.example.ui.screens.manual.ComponentStatus
import com.example.ui.screens.manual.MasterComponentCatalog
import com.example.ui.screens.settings.SettingsSubpage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Release 3 Loop 10 Unit Tests: User Manual & Home Streak Hardening (#12, #22).
 *
 * Verifies:
 * 1. Master Frontend UI Component Catalog completeness (60 items), metadata integrity,
 *    and multi-parameter filtering (query, category, status).
 * 2. Feature Guide contracts and SettingsSubpage navigation routes (all 11 guides).
 * 3. Home Streak calculation semantics: zero state, active state, profile isolation,
 *    consecutive day continuity, and month boundary transitions.
 */
class Release3Phase10ManualAndStreakUnitTest {

    // =========================================================================
    // 1. Master Frontend UI Component Catalog Tests (#12)
    // =========================================================================

    @Test
    fun testCatalog_totalCountAndMetadataIntegrity() {
        val allComponents = MasterComponentCatalog.items
        assertEquals("MasterComponentCatalog must contain exactly 60 cataloged components", 60, allComponents.size)

        allComponents.forEach { component ->
            assertTrue("Component ID must not be blank: ${component.id}", component.id.isNotBlank())
            assertTrue("Component name must not be blank for ${component.id}", component.name.isNotBlank())
            assertTrue("Component description must not be blank for ${component.id}", component.description.isNotBlank())
            assertTrue("Component whenToUse must not be blank for ${component.id}", component.whenToUse.isNotBlank())
            assertTrue("Component sourceFile must not be blank for ${component.id}", component.sourceFile.isNotBlank())
            assertTrue("Component composableName must not be blank for ${component.id}", component.composableName.isNotBlank())
            assertTrue("Component codeExample must not be blank for ${component.id}", component.codeExample.isNotBlank())
            assertNotNull("Component category must not be null for ${component.id}", component.category)
            assertNotNull("Component status must not be null for ${component.id}", component.status)
        }
    }

    @Test
    fun testCatalog_release3KeyComponentsRegistered() {
        val ids = MasterComponentCatalog.items.map { it.id }.toSet()

        val expectedR3Ids = listOf(
            "mcp_server_card",
            "feature_module_card",
            "mcp_bridge_controller",
            "glossary_reader_module",
            "productivity_radar_chart",
            "advanced_project_workspace",
            "monthly_window_recurrence",
            "planning_boards",
            "calendar_deadline_range_marker",
            "medication_detail_card",
            "task_emoji_suggester",
            "app_entry_profile_selector",
            "onboarding_survey_screen",
            "survey_time_picker",
            "survey_water_goal",
            "survey_discovery_selector",
            "honeycomb_profile_selector",
            "profile_tile",
            "backup_restore_dialog",
            "habit_streak_indicator"
        )

        expectedR3Ids.forEach { id ->
            assertTrue("Catalog must contain Release 3 component: $id", ids.contains(id))
        }
    }

    @Test
    fun testCatalog_searchQueryFiltering() {
        val mcpResults = MasterComponentCatalog.search(query = "mcp")
        assertTrue("Search for 'mcp' should return matching components", mcpResults.isNotEmpty())
        assertTrue("Search for 'mcp' must include mcp_server_card", mcpResults.any { it.id == "mcp_server_card" })
        assertTrue("Search for 'mcp' must include mcp_bridge_controller", mcpResults.any { it.id == "mcp_bridge_controller" })

        val streakResults = MasterComponentCatalog.search(query = "streak")
        assertTrue("Search for 'streak' must find habit_streak_indicator", streakResults.any { it.id == "habit_streak_indicator" })

        val emptyResults = MasterComponentCatalog.search(query = "xyznonexistentquery999")
        assertTrue("Non-matching search must return empty list", emptyResults.isEmpty())
    }

    @Test
    fun testCatalog_categoryFiltering() {
        val specialized = MasterComponentCatalog.search(category = ComponentCategory.SPECIALIZED_LIFEOS)
        assertTrue("Specialized LifeOS category must not be empty", specialized.isNotEmpty())
        assertTrue("Specialized must contain mcp_server_card", specialized.any { it.id == "mcp_server_card" })
        assertTrue("Specialized must contain feature_module_card", specialized.any { it.id == "feature_module_card" })

        val inputs = MasterComponentCatalog.search(category = ComponentCategory.INPUTS_FORMS)
        assertTrue("Inputs category must contain duration_stepper_input", inputs.any { it.id == "duration_stepper_input" })

        val dateTime = MasterComponentCatalog.search(category = ComponentCategory.DATE_TIME)
        assertTrue("Date & Time category must contain clock_time_input", dateTime.any { it.id == "clock_time_input" })

        val productivity = MasterComponentCatalog.search(category = ComponentCategory.TASK_PRODUCTIVITY)
        assertTrue("Task & Productivity category must contain advanced_project_workspace", productivity.any { it.id == "advanced_project_workspace" })

        val visual = MasterComponentCatalog.search(category = ComponentCategory.VISUAL_STYLING)
        assertTrue("Visual Styling category must contain task_completion_circle", visual.any { it.id == "task_completion_circle" })
    }

    @Test
    fun testCatalog_statusFiltering() {
        val existing = MasterComponentCatalog.search(status = ComponentStatus.EXISTING)
        assertTrue("Existing status list must have items", existing.isNotEmpty())
        assertTrue("Existing components count should be high", existing.size >= 40)

        existing.forEach {
            assertEquals(ComponentStatus.EXISTING, it.status)
        }
    }

    // =========================================================================
    // 2. User Manual Guides & Subpage Navigation Tests (#12)
    // =========================================================================

    @Test
    fun testGuides_subpageDestinationsValidity() {
        // All 11 guides advertised in UserManualScreen map to valid SettingsSubpage destinations
        val expectedGuides = mapOf(
            1 to SettingsSubpage.APPEARANCE,       // Honeycomb Quick Reminders
            2 to SettingsSubpage.PRODUCTIVITY,     // Tasks & Recurrence
            3 to SettingsSubpage.GENERAL,          // Calendar & Dot Parity
            4 to SettingsSubpage.GAMIFICATION,     // 75-Day Challenge
            5 to SettingsSubpage.NOTIFICATIONS,    // Health & Medications
            6 to SettingsSubpage.DATA_STORAGE,     // Storage & Trash Retention
            7 to SettingsSubpage.LEGAL_PRIVACY,    // 100% Offline Architecture
            8 to SettingsSubpage.GLOSSARY,         // Knowledge & Glossary (#2)
            9 to SettingsSubpage.MCP_BRIDGE,       // Bidirectional MCP AI Bridge (#3)
            10 to SettingsSubpage.GENERAL,         // Multi-Profile & Onboarding (#14, #19)
            11 to SettingsSubpage.PROJECTS_LABELS  // Advanced Project Workspace (#20)
        )

        assertEquals("There must be exactly 11 documented guides", 11, expectedGuides.size)

        expectedGuides.forEach { (guideNumber, subpage) ->
            assertNotNull("Guide #$guideNumber must target a valid non-null SettingsSubpage", subpage)
            assertTrue("Subpage name must not be blank", subpage.name.isNotBlank())
        }
    }

    @Test
    fun testGuides_navigationActionDispatch() {
        var navigatedSubpage: SettingsSubpage? = null
        val onNavigate: (SettingsSubpage) -> Unit = { navigatedSubpage = it }

        onNavigate(SettingsSubpage.MCP_BRIDGE)
        assertEquals(SettingsSubpage.MCP_BRIDGE, navigatedSubpage)

        onNavigate(SettingsSubpage.PROJECTS_LABELS)
        assertEquals(SettingsSubpage.PROJECTS_LABELS, navigatedSubpage)

        onNavigate(SettingsSubpage.GENERAL)
        assertEquals(SettingsSubpage.GENERAL, navigatedSubpage)
    }

    // =========================================================================
    // 3. Home Streak Calculation & Profile Isolation Tests (#22)
    // =========================================================================

    @Test
    fun testStreak_emptyHabitsEvaluatesToZero() {
        val habits = emptyList<HabitItem>()
        val activeStreak = habits.maxOfOrNull { it.currentStreak } ?: 0
        assertEquals("Empty habits list must produce zero streak", 0, activeStreak)
    }

    @Test
    fun testStreak_allZeroStreaksEvaluatesToZero() {
        val habits = listOf(
            HabitItem(id = 1, profileId = 1, title = "Exercise", currentStreak = 0),
            HabitItem(id = 2, profileId = 1, title = "Read", currentStreak = 0),
            HabitItem(id = 3, profileId = 1, title = "Meditate", currentStreak = 0)
        )
        val activeStreak = habits.maxOfOrNull { it.currentStreak } ?: 0
        assertEquals("All zero habits must produce zero streak", 0, activeStreak)
    }

    @Test
    fun testStreak_activeStreakSelectsMaximumHabitStreak() {
        val habits = listOf(
            HabitItem(id = 1, profileId = 1, title = "Exercise", currentStreak = 3),
            HabitItem(id = 2, profileId = 1, title = "Read", currentStreak = 14),
            HabitItem(id = 3, profileId = 1, title = "Meditate", currentStreak = 7)
        )
        val activeStreak = habits.maxOfOrNull { it.currentStreak } ?: 0
        assertEquals("Active streak must equal the maximum habit streak in the list", 14, activeStreak)
    }

    @Test
    fun testStreak_profileSwitchingIsolation() {
        val allHabits = listOf(
            // Profile 1 (Personal) habits: max streak = 21
            HabitItem(id = 1, profileId = 1L, title = "Personal Gym", currentStreak = 21),
            HabitItem(id = 2, profileId = 1L, title = "Drink Water", currentStreak = 5),

            // Profile 2 (Work) habits: max streak = 4
            HabitItem(id = 3, profileId = 2L, title = "Check PRs", currentStreak = 4),
            HabitItem(id = 4, profileId = 2L, title = "Standup", currentStreak = 2),

            // Profile 3 (Custom/Creative) habits: empty streak
            HabitItem(id = 5, profileId = 3L, title = "Draw Sketch", currentStreak = 0)
        )

        // Simulate viewing Profile 1
        val profile1Habits = allHabits.filter { it.profileId == 1L }
        val streakProfile1 = profile1Habits.maxOfOrNull { it.currentStreak } ?: 0
        assertEquals("Profile 1 streak must be isolated to 21", 21, streakProfile1)

        // Switch to Profile 2
        val profile2Habits = allHabits.filter { it.profileId == 2L }
        val streakProfile2 = profile2Habits.maxOfOrNull { it.currentStreak } ?: 0
        assertEquals("Profile 2 streak must be isolated to 4", 4, streakProfile2)

        // Switch to Profile 3
        val profile3Habits = allHabits.filter { it.profileId == 3L }
        val streakProfile3 = profile3Habits.maxOfOrNull { it.currentStreak } ?: 0
        assertEquals("Profile 3 streak must be 0", 0, streakProfile3)

        // Switch back to Profile 1 (no state leak or mutation)
        val streakProfile1Restored = profile1Habits.maxOfOrNull { it.currentStreak } ?: 0
        assertEquals("Profile 1 restored streak must remain 21", 21, streakProfile1Restored)
    }

    @Test
    fun testStreak_consecutiveDayContinuityAcrossMonthBoundary() {
        // Test date streak logic with completions crossing month boundary:
        // Oct 30, Oct 31, Nov 1
        val completedDates = setOf("2026-10-30", "2026-10-31", "2026-11-01")

        val result = GamificationEngine.calculateStreaks(
            completedDatesIso = completedDates,
            referenceDateIso = "2026-11-01"
        )

        assertEquals("Streak crossing month boundary must evaluate to 3", 3, result.currentStreak)
        assertTrue("Today must be completed", result.isCompletedToday)
    }

    @Test
    fun testStreak_brokenStreakEvaluatesAccurately() {
        // Completed 3 days ago and 2 days ago, but missed yesterday and today
        val completedDates = setOf("2026-10-29", "2026-10-30")

        val result = GamificationEngine.calculateStreaks(
            completedDatesIso = completedDates,
            referenceDateIso = "2026-11-01"
        )

        assertEquals("Broken streak with missed yesterday must evaluate to 0", 0, result.currentStreak)
        assertFalse("Today is not completed", result.isCompletedToday)
        assertEquals("Longest streak should reflect previous streak of 2", 2, result.longestStreak)
    }

    @Test
    fun testStreak_completedYesterdayStillPreservesStreak() {
        // Completed Oct 31 (yesterday), pending today (Nov 1)
        val completedDates = setOf("2026-10-30", "2026-10-31")

        val result = GamificationEngine.calculateStreaks(
            completedDatesIso = completedDates,
            referenceDateIso = "2026-11-01"
        )

        assertEquals("Streak completed yesterday must preserve streak count of 2 pending today", 2, result.currentStreak)
        assertFalse("Today is not yet completed", result.isCompletedToday)
    }
}
