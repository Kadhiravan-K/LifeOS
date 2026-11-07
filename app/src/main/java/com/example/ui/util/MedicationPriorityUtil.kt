// Utility for Task 7 medication priority resolution
package com.example.ui.util

import com.example.domain.model.TaskPriority

/**
 * Resolve the task priority according to the unified specification:
 * | Medicine reminder | QR Quick Reminder | Default priority |
 * | ON                | OFF               | P1               |
 * | ON                | ON                | P1               |
 * | OFF               | ON                | P2               |
 * | OFF               | OFF               | Preserved default|
 */
fun resolveMedicationPriority(
    reminderEnabled: Boolean,
    priorityManuallySet: Boolean,
    isNewTask: Boolean,
    currentPriority: TaskPriority,
    isQrQuickReminder: Boolean = false
): TaskPriority {
    return when {
        // Explicit user selection always wins
        priorityManuallySet -> currentPriority
        // Medicine reminder ON -> P1
        reminderEnabled -> TaskPriority.P1
        // QR Quick Reminder ON (and medicine OFF) -> P2
        isQrQuickReminder -> TaskPriority.P2
        // Brand-new task fallback -> P3
        isNewTask -> TaskPriority.P3
        // Editing existing task -> preserve current
        else -> currentPriority
    }
}
