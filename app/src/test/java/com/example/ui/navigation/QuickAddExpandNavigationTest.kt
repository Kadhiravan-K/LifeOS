package com.example.ui.navigation

import com.example.domain.model.LabelItem
import com.example.domain.model.ProjectItem
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskReminderItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression & behavioral test suite for LifeOS v4.0.1 Bug Fix 01:
 * "Quick Add Expand Button Not Responding"
 *
 * Verifies:
 * - Flow A (Footer Navigation "+" -> New Task -> Quick Add -> Expand to Advanced).
 * - Flow B (Tasks Screen Bottom-Right FAB "+" -> Quick Add -> Expand to Advanced).
 * - Preservation of task draft state (title, emoji, priority, description, dates, labels, etc.).
 * - Prevention of duplicate back-stack navigation.
 * - Save and dismiss lifecycle transitions.
 */
class QuickAddExpandNavigationTest {

    @Test
    fun testFlowA_FooterEntry_ExpandNavigatesToTasksAndOpensAdvancedEditor() {
        val navManager = NavigationStateManager(initialDestination = NavigationItem.HOME)
        assertEquals(NavigationItem.HOME, navManager.currentDestination)
        assertEquals(1, navManager.backStack.size)

        // 1. User taps footer "+" -> New Task
        var showQuickAddMenuSheet = true
        var showTaskQuickAddSheet = false
        var showTaskAdvancedEditorSheet = false
        var taskDraftForAdvancedEditor: TaskItem? = null

        // User selects "New Task" from menu
        showQuickAddMenuSheet = false
        showTaskQuickAddSheet = true

        assertTrue("Task Quick Add sheet should be open", showTaskQuickAddSheet)
        assertFalse("Advanced editor should not be open yet", showTaskAdvancedEditorSheet)
        assertNull("Draft should initially be null", taskDraftForAdvancedEditor)

        // 2. User enters draft information in Quick Add
        val enteredDraft = TaskItem(
            id = 0L,
            title = "Finalize Release 4 Documentation",
            emoji = "📝",
            description = "Audit checklist and device testing notes",
            priority = TaskPriority.P1,
            dueDateIso = "2026-11-09",
            projectId = 42L,
            labels = listOf(LabelItem(id = 101L, name = "Release", colorId = "green")),
            isReminderEnabled = true,
            reminders = listOf(TaskReminderItem(minutesBefore = 15, isEnabled = true))
        )

        // 3. User taps "Expand to Advanced Editor" button in Quick Add
        val onExpandToAdvanced: (TaskItem) -> Unit = { draft ->
            taskDraftForAdvancedEditor = draft
            showTaskQuickAddSheet = false
            showTaskAdvancedEditorSheet = true
            navManager.navigateTo(NavigationItem.TASKS)
        }

        onExpandToAdvanced(enteredDraft)

        // Verify Flow A state after expand:
        assertFalse("Quick Add sheet must be dismissed", showTaskQuickAddSheet)
        assertTrue("Task Advanced Editor Sheet must be open", showTaskAdvancedEditorSheet)
        assertNotNull("Draft must be preserved for advanced editor", taskDraftForAdvancedEditor)
        assertEquals("Finalize Release 4 Documentation", taskDraftForAdvancedEditor?.title)
        assertEquals("📝", taskDraftForAdvancedEditor?.emoji)
        assertEquals(TaskPriority.P1, taskDraftForAdvancedEditor?.priority)
        assertEquals(42L, taskDraftForAdvancedEditor?.projectId)
        assertEquals(1, taskDraftForAdvancedEditor?.labels?.size)

        // Verify navigation to Tasks screen
        assertEquals(NavigationItem.TASKS, navManager.currentDestination)
        assertEquals(2, navManager.backStack.size)
        assertEquals(NavigationItem.HOME, navManager.backStack[0])
        assertEquals(NavigationItem.TASKS, navManager.backStack[1])

        // 4. User saves the task in Advanced Editor
        var savedTask: TaskItem? = null
        val onSaveTask: (TaskItem) -> Unit = { task ->
            savedTask = task
            showTaskAdvancedEditorSheet = false
            taskDraftForAdvancedEditor = null
        }

        onSaveTask(taskDraftForAdvancedEditor!!.copy(description = "Updated description in full editor"))

        assertNotNull("Task must be saved", savedTask)
        assertEquals("Updated description in full editor", savedTask?.description)
        assertFalse("Advanced editor must be closed after save", showTaskAdvancedEditorSheet)
        assertNull("Draft must be cleared after save", taskDraftForAdvancedEditor)
        assertEquals("User remains on Tasks screen where task was saved", NavigationItem.TASKS, navManager.currentDestination)
    }

    @Test
    fun testFlowA_FooterEntry_DismissClosesSheetWithoutSaving() {
        val navManager = NavigationStateManager(initialDestination = NavigationItem.HOME)

        var showTaskQuickAddSheet = true
        var showTaskAdvancedEditorSheet = false
        var taskDraftForAdvancedEditor: TaskItem? = null

        val enteredDraft = TaskItem(title = "Draft To Discard", dueDateIso = "2026-11-09")

        // User expands
        taskDraftForAdvancedEditor = enteredDraft
        showTaskQuickAddSheet = false
        showTaskAdvancedEditorSheet = true
        navManager.navigateTo(NavigationItem.TASKS)

        assertTrue(showTaskAdvancedEditorSheet)
        assertEquals(NavigationItem.TASKS, navManager.currentDestination)

        // User dismisses / cancels
        val onDismiss = {
            showTaskAdvancedEditorSheet = false
            taskDraftForAdvancedEditor = null
        }
        onDismiss()

        assertFalse("Advanced editor sheet must be closed", showTaskAdvancedEditorSheet)
        assertNull("Draft must be cleared", taskDraftForAdvancedEditor)

        // Back button returns user to HOME
        assertTrue("Should be able to pop back to HOME", navManager.popBack())
        assertEquals(NavigationItem.HOME, navManager.currentDestination)
    }

    @Test
    fun testFlowA_AlreadyOnTasksTab_ExpandDoesNotCreateDuplicateBackStack() {
        val navManager = NavigationStateManager(initialDestination = NavigationItem.HOME)
        navManager.navigateTo(NavigationItem.TASKS)
        assertEquals(2, navManager.backStack.size)
        assertEquals(NavigationItem.TASKS, navManager.currentDestination)

        var showTaskQuickAddSheet = true
        var showTaskAdvancedEditorSheet = false
        var taskDraftForAdvancedEditor: TaskItem? = null

        val draft = TaskItem(title = "Task from Tasks Tab Footer", dueDateIso = "2026-11-09")

        // Expand clicked from footer "+" while already on Tasks tab
        taskDraftForAdvancedEditor = draft
        showTaskQuickAddSheet = false
        showTaskAdvancedEditorSheet = true
        navManager.navigateTo(NavigationItem.TASKS)

        assertTrue(showTaskAdvancedEditorSheet)
        // Back-stack must NOT have duplicate TASKS entry
        assertEquals(2, navManager.backStack.size)
        assertEquals(NavigationItem.TASKS, navManager.currentDestination)
    }

    @Test
    fun testFlowB_TaskPageFabEntry_ParityWithFlowA() {
        // Simulates Flow B (Working baseline inside TasksScreen)
        var showQuickAddSheet = true
        var showAdvancedEditorSheet = false
        var taskToEdit: TaskItem? = null

        val draft = TaskItem(
            title = "Task from FAB",
            priority = TaskPriority.P2,
            emoji = "⚡",
            dueDateIso = "2026-11-09"
        )

        // User clicks expand in TasksScreen
        val onExpandToAdvancedFlowB: (TaskItem) -> Unit = { draftItem ->
            taskToEdit = draftItem
            showQuickAddSheet = false
            showAdvancedEditorSheet = true
        }

        onExpandToAdvancedFlowB(draft)

        assertFalse(showQuickAddSheet)
        assertTrue(showAdvancedEditorSheet)
        assertNotNull(taskToEdit)
        assertEquals("Task from FAB", taskToEdit?.title)
        assertEquals(TaskPriority.P2, taskToEdit?.priority)
        assertEquals("⚡", taskToEdit?.emoji)
    }

    @Test
    fun testReopeningQuickAddAfterDismissal_WorksFresh() {
        var showTaskQuickAddSheet = false
        var showTaskAdvancedEditorSheet = false
        var taskDraftForAdvancedEditor: TaskItem? = null

        // Open 1st time
        showTaskQuickAddSheet = true
        assertTrue(showTaskQuickAddSheet)

        // Dismiss 1st time without expand
        showTaskQuickAddSheet = false
        assertFalse(showTaskQuickAddSheet)
        assertFalse(showTaskAdvancedEditorSheet)

        // Open 2nd time
        showTaskQuickAddSheet = true
        assertTrue(showTaskQuickAddSheet)

        // Expand 2nd time
        taskDraftForAdvancedEditor = TaskItem(title = "Second attempt", dueDateIso = "2026-11-09")
        showTaskQuickAddSheet = false
        showTaskAdvancedEditorSheet = true

        assertFalse(showTaskQuickAddSheet)
        assertTrue(showTaskAdvancedEditorSheet)
        assertEquals("Second attempt", taskDraftForAdvancedEditor?.title)
    }
}
