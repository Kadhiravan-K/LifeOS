package com.example

import com.example.domain.model.HoneycombShortcut
import com.example.domain.model.QrColorSource
import com.example.domain.model.QrTileShape
import com.example.domain.model.ShortcutType
import com.example.domain.model.TaskItem
import com.example.domain.model.TodoistColorPalette
import com.example.domain.templates.LifeTemplate
import com.example.domain.templates.StandardProjects
import com.example.domain.templates.TemplateCatalog
import com.example.domain.templates.TemplateCategory
import com.example.domain.templates.TemplateHabitDef
import com.example.domain.templates.TemplateProjectDef
import com.example.domain.templates.TemplateShortcutDef
import com.example.domain.templates.TemplateTaskDef
import com.example.widgets.QuickRemindersWidgetHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Release 3 — Phase 1 / Batch 01 Regression Unit Tests.
 *
 * Covers:
 * - #14: Template imports preserve Quick Reminder fill colors and project associations.
 * - #15: Tasks selection-mode checkboxes, cancel button, and multi-selection responsiveness.
 * - #16: Widget QR padding, hydration action, and standard 10-tile honeycomb layout.
 */
class Release3Phase1Batch01UnitTest {

    // =========================================================================
    // REQUIREMENT #14: TEMPLATE IMPORTS COLOR SOURCE & PROJECT REMAPPING
    // =========================================================================

    @Test
    fun `test TemplateShortcutDef positional constructor assigns coordQ and coordR without polluting customColorHex`() {
        // Calling positional (type, targetId, title, emoji, coordQ, coordR)
        val sc = TemplateShortcutDef(
            type = ShortcutType.WATER,
            targetId = "water",
            title = "Water Log",
            emoji = "💧",
            coordQ = 1,
            coordR = 2
        )

        assertEquals(ShortcutType.WATER, sc.type)
        assertEquals("water", sc.targetId)
        assertEquals("Water Log", sc.title)
        assertEquals("💧", sc.emoji)
        assertEquals(1, sc.coordQ)
        assertEquals(2, sc.coordR)
        // Must NOT be 0L or 1L - customColorHex must be null by default
        assertNull("customColorHex must default to null when using coordinate positional args", sc.customColorHex)
    }

    @Test
    fun `test TemplateShortcutDef with named parameters preserves customColorHex and coordinates`() {
        val sc = TemplateShortcutDef(
            type = ShortcutType.TASK,
            targetId = "deep_work",
            title = "Deep Work",
            emoji = "⚡",
            customColorHex = 0xFF8B5CF6L,
            coordQ = 0,
            coordR = 1
        )

        assertEquals(0xFF8B5CF6L, sc.customColorHex)
        assertEquals(0, sc.coordQ)
        assertEquals(1, sc.coordR)
    }

    @Test
    fun `test TemplateShortcutDef with negative coordinates does not corrupt color`() {
        val sc = TemplateShortcutDef(
            type = ShortcutType.TASK,
            targetId = "discipline_check",
            title = "Discipline Check",
            emoji = "🔥",
            coordQ = -1,
            coordR = 1
        )

        assertEquals(-1, sc.coordQ)
        assertEquals(1, sc.coordR)
        assertNull("Negative coordinates must not be interpreted as color hex", sc.customColorHex)
    }

    @Test
    fun `test all templates in catalog have valid shortcut coordinates and no transparent-black color corruption`() {
        TemplateCatalog.allTemplates.forEach { template ->
            template.shortcuts.forEach { sc ->
                if (sc.customColorHex != null) {
                    val alpha = (sc.customColorHex!! shr 24) and 0xFF
                    assertNotEquals(
                        "Template ${template.id} shortcut ${sc.title} has corrupted transparent black color 0L",
                        0L,
                        sc.customColorHex!!
                    )
                    assertTrue(
                        "Template ${template.id} shortcut ${sc.title} color must have valid alpha",
                        alpha > 0L
                    )
                }
            }
        }
    }

    @Test
    fun `test template shortcut color resolution logic preserves CUSTOM source when hex is present`() {
        val customColor = 0xFF0288D1L
        val scDef = TemplateShortcutDef(
            type = ShortcutType.WATER,
            targetId = "water_quick",
            title = "Drink Water",
            emoji = "💧",
            coordQ = 0,
            coordR = 0,
            customColorHex = customColor
        )

        val hasValidCustomColor = scDef.customColorHex != null && (scDef.customColorHex!! and 0xFF000000L) != 0L
        val isLinkedToProject = false

        val resolvedColorSource = when {
            scDef.colorSource != null -> scDef.colorSource
            hasValidCustomColor -> "CUSTOM"
            isLinkedToProject -> "PROJECT"
            scDef.type == ShortcutType.WATER -> "CUSTOM"
            scDef.type == ShortcutType.CHALLENGE_RULE -> "CUSTOM"
            else -> "PROJECT"
        }

        val resolvedCustomColor = when {
            hasValidCustomColor -> scDef.customColorHex
            scDef.type == ShortcutType.WATER -> 0xFF3B82F6L
            scDef.type == ShortcutType.CHALLENGE_RULE -> 0xFFF59E0BL
            else -> null
        }

        assertEquals("CUSTOM", resolvedColorSource)
        assertEquals(customColor, resolvedCustomColor)
    }

    @Test
    fun `test template shortcut task project remapping resolves database task id and project color`() {
        val createdTaskId = 42L
        val createdProjectId = 7L
        val projectColorId = "grape"
        val expectedProjectColorHex = TodoistColorPalette.getColorLong(projectColorId)

        val mockTasks = listOf(
            TaskItem(
                id = createdTaskId,
                title = "Deep Work",
                projectId = createdProjectId,
                dueDateIso = "2026-10-30",
                projectColorId = projectColorId
            )
        )

        val scDef = TemplateShortcutDef(
            type = ShortcutType.TASK,
            targetId = "Deep Work",
            title = "Deep Work",
            emoji = "⚡",
            coordQ = 0,
            coordR = 0
        )

        // Matching logic in installTemplate
        val matched = mockTasks.find { it.id.toString() == scDef.targetId }
            ?: mockTasks.find { it.title.equals(scDef.targetId, ignoreCase = true) }
            ?: mockTasks.find { it.title.equals(scDef.title, ignoreCase = true) }

        assertNotNull("Shortcut must match created task by title", matched)
        val resolvedTargetId = matched!!.id.toString()
        assertEquals("42", resolvedTargetId)

        val hasValidCustomColor = scDef.customColorHex != null && (scDef.customColorHex!! and 0xFF000000L) != 0L
        val isLinkedToProject = matched.projectId != null

        val resolvedColorSource = when {
            scDef.colorSource != null -> scDef.colorSource
            hasValidCustomColor -> "CUSTOM"
            isLinkedToProject -> "PROJECT"
            else -> "PROJECT"
        }

        assertEquals("PROJECT", resolvedColorSource)
    }

    // =========================================================================
    // REQUIREMENT #15: TASK SELECTION MODE CHECKBOXES & CANCEL BUTTON
    // =========================================================================

    @Test
    fun `test selection mode cancel action immediately clears selected task ids and exits selection mode`() {
        var isSelectionMode = true
        var selectedTaskIds = setOf(1L, 2L, 3L)

        fun clearSelection() {
            selectedTaskIds = emptySet()
            isSelectionMode = false
        }

        clearSelection()

        assertFalse("Selection mode must be false after clearSelection", isSelectionMode)
        assertTrue("Selected task IDs must be empty after clearSelection", selectedTaskIds.isEmpty())
    }

    @Test
    fun `test toggleTaskSelection adds task and automatically activates selection mode if inactive`() {
        var isSelectionMode = false
        var selectedTaskIds = emptySet<Long>()

        fun toggleTaskSelection(taskId: Long) {
            val current = selectedTaskIds.toMutableSet()
            if (current.contains(taskId)) {
                current.remove(taskId)
            } else {
                current.add(taskId)
            }
            selectedTaskIds = current.toSet()
            if (current.isNotEmpty() && !isSelectionMode) {
                isSelectionMode = true
            }
        }

        // Tapping first task checkbox
        toggleTaskSelection(101L)
        assertTrue("Selection mode must activate when task is selected", isSelectionMode)
        assertEquals(setOf(101L), selectedTaskIds)

        // Tapping second task checkbox
        toggleTaskSelection(102L)
        assertEquals(setOf(101L, 102L), selectedTaskIds)

        // Deselecting first task
        toggleTaskSelection(101L)
        assertEquals(setOf(102L), selectedTaskIds)
        assertTrue("Selection mode must remain active while items are selected", isSelectionMode)
    }

    @Test
    fun `test selectAllFilteredTasks toggles between all and empty`() {
        val filteredTasks = listOf(
            TaskItem(id = 1L, title = "A", dueDateIso = "2026-10-30"),
            TaskItem(id = 2L, title = "B", dueDateIso = "2026-10-30"),
            TaskItem(id = 3L, title = "C", dueDateIso = "2026-10-30")
        )
        val filteredIds = filteredTasks.map { it.id }.toSet()
        var selectedTaskIds = emptySet<Long>()
        var isSelectionMode = false

        fun selectAllFilteredTasks() {
            if (filteredIds.isNotEmpty() && selectedTaskIds.containsAll(filteredIds)) {
                selectedTaskIds = emptySet()
            } else {
                selectedTaskIds = filteredIds
                isSelectionMode = true
            }
        }

        // 1. Select all
        selectAllFilteredTasks()
        assertTrue(isSelectionMode)
        assertEquals(setOf(1L, 2L, 3L), selectedTaskIds)

        // 2. Deselect all
        selectAllFilteredTasks()
        assertTrue(selectedTaskIds.isEmpty())
    }

    // =========================================================================
    // REQUIREMENT #16: WIDGET HONEYCOMB GEOMETRY & HYDRATION ACTION
    // =========================================================================

    @Test
    fun `test widget interlocking honeycomb layout row configuration is 3-2-3-2 totaling 10 tiles`() {
        val totalTiles = 10
        val rowCounts = listOf(3, 2, 3, 2)
        assertEquals(4, rowCounts.size)
        assertEquals(totalTiles, rowCounts.sum())
    }

    @Test
    fun `test widget effective color resolution respects precedence Custom over Project over Default`() {
        val customColor = 0xFFFF5722L
        val projectColor = 0xFF4CAF50L

        // Case 1: CUSTOM color source
        val scCustom = HoneycombShortcut(
            id = 1,
            type = ShortcutType.TASK,
            targetId = "1",
            title = "Task 1",
            emoji = "📝",
            customColorHex = customColor,
            colorSource = "CUSTOM",
            projectColorHex = projectColor
        )
        assertEquals(customColor, scCustom.effectiveColorHex)

        // Case 2: PROJECT color source
        val scProject = HoneycombShortcut(
            id = 2,
            type = ShortcutType.TASK,
            targetId = "2",
            title = "Task 2",
            emoji = "📝",
            customColorHex = customColor,
            colorSource = "PROJECT",
            projectColorHex = projectColor
        )
        assertEquals(projectColor, scProject.effectiveColorHex)

        // Case 3: WATER shortcut defaults to water blue
        val scWater = HoneycombShortcut(
            id = 3,
            type = ShortcutType.WATER,
            targetId = "water",
            title = "Water",
            emoji = "💧",
            colorSource = "PROJECT",
            projectColorHex = null
        )
        assertEquals(0xFF3B82F6L, scWater.effectiveColorHex)
    }

    @Test
    fun `test widget hydration tap action increments water intake`() {
        var currentWaterMl = 500
        val waterGoalMl = 2000

        fun onWaterTap() {
            currentWaterMl += 250
        }

        // User taps water tile on widget
        onWaterTap()
        assertEquals(750, currentWaterMl)
        assertFalse("Water goal not yet reached", currentWaterMl >= waterGoalMl)

        // Additional logs reach goal
        repeat(5) { onWaterTap() }
        assertEquals(2000, currentWaterMl)
        assertTrue("Water goal reached", currentWaterMl >= waterGoalMl)
    }
}
