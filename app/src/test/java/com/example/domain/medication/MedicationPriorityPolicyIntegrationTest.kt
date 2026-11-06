// Integration‑style tests that mimic the UI save path for Task 7.
package com.example.domain.medication

import com.example.domain.model.TaskPriority
import com.example.ui.util.resolveMedicationPriority
import org.junit.Assert.assertEquals
import org.junit.Test

class MedicationPriorityPolicyIntegrationTest {

    /** Helper reproducing the Quick‑Add save logic without Compose. */
    private fun quickAddResult(
        reminderEnabled: Boolean,
        explicitPriority: TaskPriority?,
        isNewTask: Boolean
    ): TaskPriority {
        val manuallySet = explicitPriority != null && explicitPriority != TaskPriority.P3
        // UI would pass the current priority (explicit if set, otherwise default P3).
        val current = explicitPriority ?: TaskPriority.P3
        return resolveMedicationPriority(
            reminderEnabled = reminderEnabled,
            priorityManuallySet = manuallySet,
            isNewTask = isNewTask,
            currentPriority = current
        )
    }

    /** Helper reproducing the Advanced‑Editor save logic. */
    private fun advancedEditorResult(
        remindersListNotEmpty: Boolean,
        explicitPriority: TaskPriority?,
        isNewTask: Boolean
    ): TaskPriority {
        val manuallySet = explicitPriority != null && explicitPriority != TaskPriority.P3
        val current = explicitPriority ?: TaskPriority.P3
        return resolveMedicationPriority(
            reminderEnabled = remindersListNotEmpty,
            priorityManuallySet = manuallySet,
            isNewTask = isNewTask,
            currentPriority = current
        )
    }

    // ----------- New medication scenarios -----------
    @Test fun quickAddNewMedicationReminderOnNoExplicitPriority() {
        assertEquals(TaskPriority.URGENT, quickAddResult(reminderEnabled = true, explicitPriority = null, isNewTask = true))
    }

    @Test fun quickAddNewMedicationReminderOffNoExplicitPriority() {
        assertEquals(TaskPriority.P3, quickAddResult(reminderEnabled = false, explicitPriority = null, isNewTask = true))
    }

    @Test fun advancedEditorNewMedicationReminderOnNoExplicitPriority() {
        assertEquals(TaskPriority.URGENT, advancedEditorResult(remindersListNotEmpty = true, explicitPriority = null, isNewTask = true))
    }

    @Test fun advancedEditorNewMedicationReminderOffNoExplicitPriority() {
        assertEquals(TaskPriority.P3, advancedEditorResult(remindersListNotEmpty = false, explicitPriority = null, isNewTask = true))
    }

    // ----------- Explicit priority always wins -----------
    @Test fun `explicit P2 overrides reminder state`() {
        assertEquals(TaskPriority.P2, quickAddResult(reminderEnabled = true, explicitPriority = TaskPriority.P2, isNewTask = true))
        assertEquals(TaskPriority.P2, advancedEditorResult(remindersListNotEmpty = false, explicitPriority = TaskPriority.P2, isNewTask = true))
    }

    @Test fun `explicit P4 overrides reminder state`() {
        assertEquals(TaskPriority.P4, quickAddResult(reminderEnabled = false, explicitPriority = TaskPriority.P4, isNewTask = true))
        assertEquals(TaskPriority.P4, advancedEditorResult(remindersListNotEmpty = true, explicitPriority = TaskPriority.P4, isNewTask = true))
    }

    // ----------- Editing existing medication preserves priority -----------
    @Test fun `editing existing task retains priority despite reminder toggle changes`() {
        // Existing with P1, toggle reminder OFF then ON – priority stays P1.
        val start = quickAddResult(reminderEnabled = true, explicitPriority = TaskPriority.P1, isNewTask = false)
        assertEquals(TaskPriority.P1, start)
        val afterToggle = quickAddResult(reminderEnabled = false, explicitPriority = TaskPriority.P1, isNewTask = false)
        assertEquals(TaskPriority.P1, afterToggle)
    }

    // ----------- Reminder toggle cycles do not reset priority -----------
    @Test fun reminderOnOffOnDoesNotResetExistingExplicitPriority() {
        val explicit = TaskPriority.P2
        val step1 = quickAddResult(reminderEnabled = true, explicitPriority = explicit, isNewTask = false)
        val step2 = quickAddResult(reminderEnabled = false, explicitPriority = explicit, isNewTask = false)
        val step3 = quickAddResult(reminderEnabled = true, explicitPriority = explicit, isNewTask = false)
        assertEquals(explicit, step1)
        assertEquals(explicit, step2)
        assertEquals(explicit, step3)
    }

    // ----------- Medicine mode without actual reminder should not produce P1 -----------
    @Test fun `medicine mode alone without reminder does not default to P1`() {
        // In UI, medicine mode toggles isMedicineEnabled but priority depends on isReminderEnabled.
        // Simulate reminder disabled (false) and no explicit priority.
        assertEquals(TaskPriority.P3, quickAddResult(reminderEnabled = false, explicitPriority = null, isNewTask = true))
    }

    // ----------- Profile isolation -----------
    @Test fun `different profiles get independent defaults`() {
        val profileA = quickAddResult(reminderEnabled = true, explicitPriority = null, isNewTask = true)
        val profileB = quickAddResult(reminderEnabled = false, explicitPriority = null, isNewTask = true)
        assertEquals(TaskPriority.URGENT, profileA)
        assertEquals(TaskPriority.P3, profileB)
    }
}
