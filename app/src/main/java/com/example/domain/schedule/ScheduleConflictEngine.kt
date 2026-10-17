package com.example.domain.schedule

import com.example.domain.model.TaskItem
import com.example.utils.DateUtils

enum class ConflictSeverity {
    WARNING,
    INFO
}

enum class ConflictType {
    SLEEP_WINDOW,
    OVERLAPPING_TASK,
    PROTECTED_BLOCK,
    DUPLICATE_TASK,
    EXCESSIVE_WORKLOAD
}

data class ScheduleConflict(
    val type: ConflictType,
    val severity: ConflictSeverity = ConflictSeverity.WARNING,
    val message: String,
    val suggestedTime: String? = null
)

data class ProtectedTimeBlock(
    val name: String,
    val startMinutes: Int,
    val endMinutes: Int,
    val isWeekdayOnly: Boolean = false
)

object ScheduleConflictEngine {

    /**
     * Default protected blocks from user personal rules:
     * - Morning fitness & meditation: 6:00 AM - 7:30 AM (360 - 450)
     * - Career development: 9:00 AM - 5:00 PM (540 - 1020) on weekdays
     */
    val defaultProtectedBlocks = listOf(
        ProtectedTimeBlock("Fitness & Meditation", 6 * 60, 7 * 60 + 30, false),
        ProtectedTimeBlock("Career Development", 9 * 60, 17 * 60, true)
    )

    /**
     * Checks a task against other tasks on the date and personal rules.
     */
    fun detectConflicts(
        task: TaskItem,
        existingTasksOnDate: List<TaskItem>,
        targetDateIso: String,
        wakeUpMinutes: Int = 5 * 60 + 30, // 5:30 AM = 330
        bedTimeMinutes: Int = 21 * 60 + 30, // 9:30 PM = 1290
        protectedBlocks: List<ProtectedTimeBlock> = defaultProtectedBlocks
    ): List<ScheduleConflict> {
        val conflicts = mutableListOf<ScheduleConflict>()

        // 1. Check for Duplicate Task Definition (same title & category)
        val duplicate = existingTasksOnDate.firstOrNull {
            it.id != task.id && it.title.trim().equals(task.title.trim(), ignoreCase = true)
        }
        if (duplicate != null) {
            conflicts.add(
                ScheduleConflict(
                    type = ConflictType.DUPLICATE_TASK,
                    severity = ConflictSeverity.WARNING,
                    message = "⚠️ Duplicate task: '${duplicate.title}' is already scheduled for this day."
                )
            )
        }

        // If task has no specified due time, check total workload only
        val taskStartMins = DateUtils.timeToMinutes(task.dueTime)
        if (taskStartMins < 0) {
            checkExcessiveWorkload(task, existingTasksOnDate, conflicts)
            return conflicts
        }

        val taskDuration = task.durationMinutes.coerceAtLeast(15)
        val taskEndMins = taskStartMins + taskDuration

        // 2. Detect Tasks scheduled during Sleep Protection (9:30 PM - 5:30 AM)
        val isDuringSleep = taskStartMins < wakeUpMinutes || taskStartMins >= bedTimeMinutes || taskEndMins > bedTimeMinutes
        if (isDuringSleep) {
            val suggested = findNextAvailableSlot(
                durationMinutes = taskDuration,
                existingTasks = existingTasksOnDate.filter { it.id != task.id },
                wakeUpMinutes = wakeUpMinutes,
                bedTimeMinutes = bedTimeMinutes,
                startSearchFrom = wakeUpMinutes + 120 // start search after morning routine (7:30 AM)
            )
            conflicts.add(
                ScheduleConflict(
                    type = ConflictType.SLEEP_WINDOW,
                    severity = ConflictSeverity.WARNING,
                    message = "⚠️ Scheduled during sleep protection (${DateUtils.minutesToTime(bedTimeMinutes)} – ${DateUtils.minutesToTime(wakeUpMinutes)}).",
                    suggestedTime = suggested
                )
            )
        }

        // 3. Detect Overlapping Tasks with other scheduled tasks on the same date
        val otherTasks = existingTasksOnDate.filter { it.id != task.id && !it.dueTime.isNullOrBlank() }
        for (other in otherTasks) {
            val otherStart = DateUtils.timeToMinutes(other.dueTime)
            val otherDuration = other.durationMinutes.coerceAtLeast(15)

            if (DateUtils.doIntervalsOverlap(taskStartMins, taskDuration, otherStart, otherDuration)) {
                val suggested = findNextAvailableSlot(
                    durationMinutes = taskDuration,
                    existingTasks = otherTasks,
                    wakeUpMinutes = wakeUpMinutes,
                    bedTimeMinutes = bedTimeMinutes,
                    startSearchFrom = otherStart + otherDuration + 15
                )
                conflicts.add(
                    ScheduleConflict(
                        type = ConflictType.OVERLAPPING_TASK,
                        severity = ConflictSeverity.WARNING,
                        message = "⚠️ Overlaps with '${other.title}' (${DateUtils.formatTime(other.dueTime)}).",
                        suggestedTime = suggested
                    )
                )
                break // report top overlap
            }
        }

        // 4. Detect Overlap with Protected Time Blocks
        val dayOfWeek = DateUtils.getDayOfWeek(targetDateIso)
        val isWeekday = dayOfWeek != "SATURDAY" && dayOfWeek != "SUNDAY"

        for (block in protectedBlocks) {
            if (block.isWeekdayOnly && !isWeekday) continue
            // Skip check if the task is explicitly belonging to that block (e.g. habits/work)
            if (task.category.contains("Work", ignoreCase = true) && block.name.contains("Career", ignoreCase = true)) continue
            if (task.category.contains("Habits", ignoreCase = true) && block.name.contains("Fitness", ignoreCase = true)) continue

            if (DateUtils.doIntervalsOverlap(taskStartMins, taskDuration, block.startMinutes, block.endMinutes - block.startMinutes)) {
                val suggested = findNextAvailableSlot(
                    durationMinutes = taskDuration,
                    existingTasks = otherTasks,
                    wakeUpMinutes = wakeUpMinutes,
                    bedTimeMinutes = bedTimeMinutes,
                    startSearchFrom = block.endMinutes + 15
                )
                conflicts.add(
                    ScheduleConflict(
                        type = ConflictType.PROTECTED_BLOCK,
                        severity = ConflictSeverity.WARNING,
                        message = "⚠️ Overlaps protected block '${block.name}' (${DateUtils.minutesToTime(block.startMinutes)} – ${DateUtils.minutesToTime(block.endMinutes)}).",
                        suggestedTime = suggested
                    )
                )
                break
            }
        }

        // 5. Detect Excessive Daily Workload
        checkExcessiveWorkload(task, existingTasksOnDate, conflicts)

        return conflicts
    }

    private fun checkExcessiveWorkload(
        currentTask: TaskItem,
        existingTasks: List<TaskItem>,
        conflicts: MutableList<ScheduleConflict>
    ) {
        val totalMinutes = existingTasks
            .filter { it.id != currentTask.id }
            .sumOf { it.durationMinutes } + currentTask.durationMinutes

        if (totalMinutes > 480) { // More than 8 hours of scheduled tasks
            conflicts.add(
                ScheduleConflict(
                    type = ConflictType.EXCESSIVE_WORKLOAD,
                    severity = ConflictSeverity.INFO,
                    message = "⚠️ Total workload on this day is ${totalMinutes / 60}h ${totalMinutes % 60}m. Excessive daily workload."
                )
            )
        }
    }

    /**
     * Finds the next 15/30-minute interval between waking hours that does not overlap existing tasks.
     */
    fun findNextAvailableSlot(
        durationMinutes: Int,
        existingTasks: List<TaskItem>,
        wakeUpMinutes: Int = 330,
        bedTimeMinutes: Int = 1290,
        startSearchFrom: Int = 360
    ): String? {
        val step = 15
        var currentSlot = (startSearchFrom / step) * step
        if (currentSlot < wakeUpMinutes) currentSlot = wakeUpMinutes

        while (currentSlot + durationMinutes <= bedTimeMinutes) {
            val hasOverlap = existingTasks.any { task ->
                val taskStart = DateUtils.timeToMinutes(task.dueTime)
                if (taskStart < 0) false
                else DateUtils.doIntervalsOverlap(currentSlot, durationMinutes, taskStart, task.durationMinutes)
            }

            if (!hasOverlap) {
                return DateUtils.minutesToTime(currentSlot)
            }
            currentSlot += step
        }
        return null
    }
}
