// Utility for Task 7 medication priority resolution
package com.example.ui.util

import com.example.domain.model.TaskPriority

/**
 * Resolve the task priority according to the approved Task 7 specification.
 *
 * @param reminderEnabled true if the reminder toggle is ON (i.e., remindersList is non‑empty).
 * @param priorityManuallySet true if the user explicitly selected a priority.
 * @param isNewTask true for brand‑new tasks (no initialTask) – defaults apply.
 * @param currentPriority the current priority state (may be the default P3).
 * @return the priority that should be persisted.
 */
fun resolveMedicationPriority(
    reminderEnabled: Boolean,
    priorityManuallySet: Boolean,
    isNewTask: Boolean,
    currentPriority: TaskPriority
): TaskPriority {
    return when {
        // Explicit user selection always wins
        priorityManuallySet -> currentPriority
        // New task defaults based on reminder state
        isNewTask && reminderEnabled -> TaskPriority.URGENT // P1
        isNewTask && !reminderEnabled -> TaskPriority.P3 // MEDIUM (P3)
        // Editing existing task – preserve whatever is stored
        else -> currentPriority
    }
}
