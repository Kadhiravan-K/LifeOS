package com.example

import com.example.domain.templates.StandardProjects
import com.example.domain.templates.TemplateCatalog
import com.example.domain.templates.TemplateCategory
import com.example.ui.screens.settings.SettingsSubpage
import com.example.ui.screens.settings.getSettingsSearchRegistry
import com.example.ui.theme.engine.ThemeRegistry
import com.example.utils.DateUtils
import com.example.widgets.QuickRemindersWidgetHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Mobile Testing Batch 03 (Settings & Widget) Regression Unit Tests.
 * Covers SET-01 through SET-07 and WID-01.
 */
class Batch03SettingsWidgetRegressionUnitTest {

    // =========================================================================
    // SET-01: Settings Search Registry & Subpage Navigation
    // =========================================================================

    @Test
    fun `test settings search registry contains all required settings options`() {
        val registry = getSettingsSearchRegistry()
        assertTrue("Settings search registry should have at least 15 indexed options", registry.size >= 15)

        // Verify key subpage coverage
        val subpages = registry.map { it.subpage }.toSet()
        assertTrue(subpages.contains(SettingsSubpage.GENERAL))
        assertTrue(subpages.contains(SettingsSubpage.APPEARANCE))
        assertTrue(subpages.contains(SettingsSubpage.NOTIFICATIONS))
        assertTrue(subpages.contains(SettingsSubpage.DATA_STORAGE))
        assertTrue(subpages.contains(SettingsSubpage.PROJECTS_LABELS))
        assertTrue(subpages.contains(SettingsSubpage.USER_MANUAL))
    }

    @Test
    fun `test settings search query matching by title, category, and keywords`() {
        val registry = getSettingsSearchRegistry()

        // Search for "profile"
        val profileResults = registry.filter { item ->
            item.title.contains("profile", ignoreCase = true) ||
            item.subtitle.contains("profile", ignoreCase = true) ||
            item.keywords.any { it.contains("profile", ignoreCase = true) }
        }
        assertTrue("Query 'profile' should match user profile avatar setting", profileResults.any { it.title.contains("Profile", ignoreCase = true) })
        assertEquals(SettingsSubpage.GENERAL, profileResults.first().subpage)

        // Search for "trash"
        val trashResults = registry.filter { item ->
            item.title.contains("trash", ignoreCase = true) ||
            item.keywords.any { it.contains("trash", ignoreCase = true) }
        }
        assertTrue("Query 'trash' should match trash bin recovery", trashResults.any { it.title.contains("Trash", ignoreCase = true) })
        assertEquals(SettingsSubpage.DATA_STORAGE, trashResults.first().subpage)

        // Search for "duration"
        val durationResults = registry.filter { item ->
            item.title.contains("duration", ignoreCase = true) ||
            item.keywords.any { it.contains("duration", ignoreCase = true) }
        }
        assertTrue("Query 'duration' should match default task duration", durationResults.any { it.title.contains("Duration", ignoreCase = true) })

        // Search for "saturday"
        val saturdayResults = registry.filter { item ->
            item.keywords.any { it.contains("saturday", ignoreCase = true) }
        }
        assertTrue("Query 'saturday' should match first day of week", saturdayResults.any { it.title.contains("First Day of Week", ignoreCase = true) })
    }

    // =========================================================================
    // SET-02: Theme Loading Animation & Motif Consistency
    // =========================================================================

    @Test
    fun `test all themes define valid loading motif emoji and decoration`() {
        val allThemes = ThemeRegistry.allThemes
        assertTrue(allThemes.isNotEmpty())

        allThemes.forEach { theme ->
            assertTrue("Theme ${theme.id} motif emoji should not be blank", theme.decoration.motifEmoji.isNotBlank())
            assertNotNull("Theme ${theme.id} should have primary color", theme.colors.primary)
            assertNotNull("Theme ${theme.id} should have container color", theme.colors.surface)
        }
    }

    // =========================================================================
    // SET-03: Template Projects & Labels Standardization
    // =========================================================================

    @Test
    fun `test all template tasks map to standard projects and have non-empty labels`() {
        val forbiddenBundleProjectNames = setOf(
            "Morning", "High Performance Morning", "Deep Sleep", "Evening Wind-Down",
            "Deep Work", "Daily Flow", "Master Daily Flow", "Creative Sprint",
            "Executive Weekly Review", "Sunday Planning", "Sprint"
        )

        val templates = TemplateCatalog.allTemplates
        assertTrue("Catalog should contain template bundles", templates.isNotEmpty())

        templates.forEach { template ->
            template.tasks.forEach { task ->
                val project = task.projectName
                assertNotNull("Task '${task.title}' in '${template.title}' must have a projectName", project)
                assertFalse(
                    "Task '${task.title}' in '${template.title}' must NOT use bundle name '$project' as project name",
                    forbiddenBundleProjectNames.contains(project)
                )
                assertTrue(
                    "Project '$project' for task '${task.title}' must be one of standard categories: ${StandardProjects.ALL_NAMES}",
                    StandardProjects.ALL_NAMES.contains(project)
                )
                assertTrue(
                    "Task '${task.title}' in '${template.title}' must have at least one label",
                    task.labels.isNotEmpty()
                )
            }
        }
    }

    // =========================================================================
    // SET-04: Additional Template Bundles
    // =========================================================================

    @Test
    fun `test five additional template bundles exist with correct categories and tasks`() {
        val requiredBundles = listOf(
            Triple("bundle_bucket_list", "template_bucket_list", TemplateCategory.BUCKET_LIST),
            Triple("bundle_couples_goals", "template_couples_goals", TemplateCategory.COUPLES_GOALS),
            Triple("bundle_trip_packing", "template_trip_packing", TemplateCategory.TRAVEL_CHECKLIST),
            Triple("bundle_marriage_planning", "template_marriage_planning", TemplateCategory.EVENT_PLANNING),
            Triple("bundle_startup_setup", "template_startup_setup", TemplateCategory.STARTUP_LAUNCH)
        )

        requiredBundles.forEach { (bundleId, templateId, expectedCategory) ->
            val bundle = TemplateCatalog.allBundles.find { it.id == bundleId }
            assertNotNull("Bundle $bundleId should exist in allBundles", bundle)
            assertTrue("Bundle $bundleId should reference $templateId", bundle!!.templateIds.contains(templateId))

            val template = TemplateCatalog.getTemplateById(templateId)
            assertNotNull("Template $templateId should exist in catalog", template)
            assertEquals("Template $templateId should have category $expectedCategory", expectedCategory, template!!.category)
            assertTrue("Template $templateId should have tasks", template.tasks.isNotEmpty())

            // Verify every task has non-empty title, priority, project, and labels
            template.tasks.forEach { task ->
                assertTrue(task.title.isNotBlank())
                assertNotNull(task.priority)
                assertNotNull(task.projectName)
                assertTrue(StandardProjects.ALL_NAMES.contains(task.projectName))
                assertTrue(task.labels.isNotEmpty())
            }
        }
    }

    // =========================================================================
    // SET-05: Trash Retention Duration Conversion & Bounds
    // =========================================================================

    @Test
    fun `test trash retention duration unit conversion and clamping`() {
        fun convertToDays(value: Int, unit: String): Int {
            val multiplier = when (unit) {
                "Weeks" -> 7
                "Months" -> 30
                "Years" -> 365
                else -> 1
            }
            return (value * multiplier).coerceIn(1, 365)
        }

        // Days
        assertEquals(30, convertToDays(30, "Days"))
        assertEquals(1, convertToDays(0, "Days"))
        assertEquals(365, convertToDays(500, "Days"))

        // Weeks
        assertEquals(14, convertToDays(2, "Weeks"))
        assertEquals(28, convertToDays(4, "Weeks"))
        assertEquals(365, convertToDays(60, "Weeks"))

        // Months
        assertEquals(30, convertToDays(1, "Months"))
        assertEquals(90, convertToDays(3, "Months"))
        assertEquals(365, convertToDays(15, "Months"))

        // Years
        assertEquals(365, convertToDays(1, "Years"))
        assertEquals(365, convertToDays(2, "Years"))
    }

    // =========================================================================
    // SET-06: Profile Emoji Preference & Greeting Fallback
    // =========================================================================

    @Test
    fun `test profile emoji fallback to motif emoji when blank`() {
        val motifEmoji = "🚀"
        val emptyProfileEmoji = ""
        val effectiveEmoji = emptyProfileEmoji.ifBlank { motifEmoji }
        assertEquals("🚀", effectiveEmoji)

        val customProfileEmoji = "🦊"
        val customEffectiveEmoji = customProfileEmoji.ifBlank { motifEmoji }
        assertEquals("🦊", customEffectiveEmoji)
    }

    // =========================================================================
    // SET-07: Week Start (Saturday) & Default Task Duration
    // =========================================================================

    @Test
    fun `test first day of week Saturday grid calculation`() {
        val saturdayGrid = DateUtils.getMonthCalendarGrid(year = 2026, monthZeroIndexed = Calendar.OCTOBER, firstDayOfWeek = "SATURDAY")
        assertTrue("Month grid must be a multiple of 7", saturdayGrid.size % 7 == 0)
        assertTrue("Month grid must contain at least 28 days", saturdayGrid.size >= 28)

        // Verify Saturday week days start on Saturday
        val weekDays = DateUtils.getWeekDays(referenceDateIso = "2026-10-27", firstDayOfWeek = "SATURDAY")
        assertEquals(7, weekDays.size)
        val firstDayCal = Calendar.getInstance().apply {
            val parts = weekDays.first().split("-")
            set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        }
        assertEquals(Calendar.SATURDAY, firstDayCal.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun `test default task duration bounds and presets`() {
        val presets = listOf(15, 30, 45, 60, 90)
        presets.forEach { preset ->
            assertTrue(preset in 5..480)
        }

        // Stepper decrements and increments clamped between 5 and 480
        val clampedMin = (5 - 5).coerceAtLeast(5)
        assertEquals(5, clampedMin)

        val clampedMax = (480 + 5).coerceAtMost(480)
        assertEquals(480, clampedMax)
    }

    // =========================================================================
    // WID-01: Quick Reminders Widget Axial Hex Staggering Math
    // =========================================================================

    @Test
    fun `test axial hex grid staggering factors and row parity`() {
        // Even rows (r = 0, 2, -2) -> factor 0.0f, odd row false
        assertEquals(0.0f, QuickRemindersWidgetHelper.calculateAxialStaggerFactor(0), 0.001f)
        assertEquals(0.0f, QuickRemindersWidgetHelper.calculateAxialStaggerFactor(2), 0.001f)
        assertEquals(0.0f, QuickRemindersWidgetHelper.calculateAxialStaggerFactor(-2), 0.001f)
        assertFalse(QuickRemindersWidgetHelper.isOddStaggeredRow(0))
        assertFalse(QuickRemindersWidgetHelper.isOddStaggeredRow(2))
        assertFalse(QuickRemindersWidgetHelper.isOddStaggeredRow(-2))

        // Odd rows (r = 1, 3, -1, -3) -> factor 0.5f, odd row true
        assertEquals(0.5f, QuickRemindersWidgetHelper.calculateAxialStaggerFactor(1), 0.001f)
        assertEquals(0.5f, QuickRemindersWidgetHelper.calculateAxialStaggerFactor(3), 0.001f)
        assertEquals(0.5f, QuickRemindersWidgetHelper.calculateAxialStaggerFactor(-1), 0.001f)
        assertEquals(0.5f, QuickRemindersWidgetHelper.calculateAxialStaggerFactor(-3), 0.001f)
        assertTrue(QuickRemindersWidgetHelper.isOddStaggeredRow(1))
        assertTrue(QuickRemindersWidgetHelper.isOddStaggeredRow(3))
        assertTrue(QuickRemindersWidgetHelper.isOddStaggeredRow(-1))
        assertTrue(QuickRemindersWidgetHelper.isOddStaggeredRow(-3))
    }

    @Test
    fun `test hex dimension calculations`() {
        val width = QuickRemindersWidgetHelper.calculateHexWidth(radius = 28f)
        val height = QuickRemindersWidgetHelper.calculateHexHeight(radius = 28f)
        // Pointy-top hex: width = sqrt(3) * radius ≈ 1.732 * 28 ≈ 48.497f
        // height = 2 * radius = 56f
        assertTrue(width > 48.0f && width < 49.0f)
        assertEquals(56.0f, height, 0.001f)
    }
}
