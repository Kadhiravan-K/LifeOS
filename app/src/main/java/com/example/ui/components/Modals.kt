package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DefaultTaskCategories
import com.example.domain.model.HabitItem
import com.example.domain.model.RecurrenceType
import com.example.domain.model.Subtask
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.schedule.ScheduleConflictEngine
import com.example.utils.DateUtils

val QuickEmojis = listOf(
    "✅", "⚡", "🎯", "🌅", "👤", "💼", "🏠", "💰",
    "📚", "🏥", "🛒", "👨‍👩‍👧", "📅", "🔥", "🚀", "💡",
    "🧘", "🏃", "💧", "📖", "😴", "🧺", "🧹", "🚿",
    "💆‍♂️", "🪥", "🍎", "🥗", "🏋️", "✍️", "🎯", "🛡️"
)

val WeekDaysList = listOf(
    "MONDAY" to "Mon",
    "TUESDAY" to "Tue",
    "WEDNESDAY" to "Wed",
    "THURSDAY" to "Thu",
    "FRIDAY" to "Fri",
    "SATURDAY" to "Sat",
    "SUNDAY" to "Sun"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskEditModal(
    initialTask: TaskItem? = null,
    existingTasksOnDate: List<TaskItem> = emptyList(),
    defaultDateIso: String = DateUtils.todayString(),
    onDismiss: () -> Unit,
    onSaveOccurrenceOnly: ((
        title: String,
        emoji: String,
        description: String,
        category: String,
        priority: TaskPriority,
        dueTime: String?,
        durationMinutes: Int,
        subtasks: List<Subtask>,
        notes: String,
        isReminderEnabled: Boolean,
        reminderMinutesBefore: Int
    ) -> Unit)? = null,
    onSave: (
        title: String,
        emoji: String,
        description: String,
        category: String,
        priority: TaskPriority,
        dueDateIso: String,
        dueTime: String?,
        durationMinutes: Int,
        recurrenceType: RecurrenceType,
        recurrenceInterval: Int,
        recurrenceDaysOfWeek: List<String>,
        recurrenceEndDateIso: String?,
        subtasks: List<Subtask>,
        notes: String,
        isReminderEnabled: Boolean,
        reminderMinutesBefore: Int
    ) -> Unit
) {
    var editScope by remember { mutableStateOf("ALL") }
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var emoji by remember { mutableStateOf(initialTask?.emoji ?: "✅") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var category by remember { mutableStateOf(initialTask?.category ?: "Habits & Daily Routine") }
    var priority by remember { mutableStateOf(initialTask?.priority ?: TaskPriority.MEDIUM) }
    var dueDateIso by remember { mutableStateOf(initialTask?.dueDateIso ?: defaultDateIso) }
    var dueTime by remember { mutableStateOf(initialTask?.dueTime ?: "") }
    var durationMinutes by remember { mutableStateOf(initialTask?.durationMinutes ?: 30) }
    var recurrenceType by remember { mutableStateOf(initialTask?.recurrenceType ?: RecurrenceType.NONE) }
    var recurrenceInterval by remember { mutableStateOf(initialTask?.recurrenceInterval?.toString() ?: "1") }
    var recurrenceEndDateIso by remember { mutableStateOf(initialTask?.recurrenceEndDateIso ?: "") }

    var isReminderEnabled by remember { mutableStateOf(initialTask?.isReminderEnabled ?: false) }
    var reminderMinutesBefore by remember { mutableStateOf(initialTask?.reminderMinutesBefore ?: 0) }
    var notes by remember { mutableStateOf(initialTask?.notes ?: "") }

    val selectedDays = remember {
        mutableStateListOf<String>().apply {
            addAll(initialTask?.recurrenceDaysOfWeek ?: emptyList())
        }
    }

    val subtasks = remember {
        mutableStateListOf<Subtask>().apply {
            addAll(initialTask?.subtasks ?: emptyList())
        }
    }
    var newSubtaskText by remember { mutableStateOf("") }
    var isTitleError by remember { mutableStateOf(false) }

    // Live conflict detection preview
    val conflicts by remember(title, dueTime, durationMinutes, dueDateIso, category) {
        derivedStateOf {
            if (dueTime.isNotBlank()) {
                val candidate = TaskItem(
                    id = initialTask?.id ?: 0L,
                    title = title,
                    category = category,
                    dueDateIso = dueDateIso,
                    dueTime = dueTime,
                    durationMinutes = durationMinutes
                )
                ScheduleConflictEngine.detectConflicts(
                    task = candidate,
                    existingTasksOnDate = existingTasksOnDate,
                    targetDateIso = dueDateIso
                )
            } else {
                emptyList()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTask == null) "New Task" else "Edit Task",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Live Conflict Warning Banner if conflicts detected
                if (conflicts.isNotEmpty()) {
                    val conflict = conflicts.first()
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = "Conflict",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = conflict.message,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF92400E)
                                )
                            }
                            if (conflict.suggestedTime != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = { dueTime = conflict.suggestedTime },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Apply Suggestion: ${conflict.suggestedTime}", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Edit Recurring Task Scope (if editing an existing recurring task)
                if (initialTask != null && initialTask.recurrenceType != RecurrenceType.NONE && onSaveOccurrenceOnly != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Apply Changes To",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilterChip(
                                    selected = editScope == "ALL",
                                    onClick = { editScope = "ALL" },
                                    label = { Text("All Occurrences", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = editScope == "THIS",
                                    onClick = { editScope = "THIS" },
                                    label = { Text("This Occurrence ($defaultDateIso)", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                // Emoji quick bar
                Text("Select Emoji", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickEmojis.take(16).forEach { em ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (emoji == em) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { emoji = em }
                        ) {
                            Text(text = em, fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) isTitleError = false
                    },
                    label = { Text("Task Title *") },
                    isError = isTitleError,
                    supportingText = if (isTitleError) { { Text("Title cannot be empty") } } else null,
                    leadingIcon = { Text(emoji, fontSize = 20.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_input_title")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Priority Selection
                Text("Priority", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TaskPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.displayName, fontSize = 12.sp) },
                            modifier = Modifier.testTag("priority_chip_${p.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Selection
                Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DefaultTaskCategories.list.forEach { cat ->
                        FilterChip(
                            selected = category == cat.name,
                            onClick = { category = cat.name },
                            label = { Text("${cat.emoji} ${cat.name}", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Due Date & Time
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = dueDateIso,
                        onValueChange = { dueDateIso = it },
                        label = { Text("Due Date (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dueTime,
                        onValueChange = { dueTime = it },
                        label = { Text("Time (e.g. 09:30)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Duration Selector
                Text("Duration", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(15 to "15m", 30 to "30m", 45 to "45m", 60 to "1h", 90 to "1.5h", 120 to "2h").forEach { (mins, label) ->
                        FilterChip(
                            selected = durationMinutes == mins,
                            onClick = { durationMinutes = mins },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Recurrence Selection
                Text("Recurrence", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RecurrenceType.entries.forEach { rec ->
                        FilterChip(
                            selected = recurrenceType == rec,
                            onClick = { recurrenceType = rec },
                            label = { Text(rec.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                // If Selected Weekdays, select weekdays
                if (recurrenceType == RecurrenceType.SELECTED_WEEKDAYS || recurrenceType == RecurrenceType.WEEKLY) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select Weekdays", style = MaterialTheme.typography.labelSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        WeekDaysList.forEach { (dayKey, dayShort) ->
                            val isSelected = selectedDays.contains(dayKey)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selectedDays.remove(dayKey) else selectedDays.add(dayKey)
                                },
                                label = { Text(dayShort, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // If interval needed (EVERY_N_DAYS, EVERY_N_WEEKS, EVERY_N_MONTHS)
                if (recurrenceType == RecurrenceType.EVERY_N_DAYS || recurrenceType == RecurrenceType.EVERY_N_WEEKS || recurrenceType == RecurrenceType.EVERY_N_MONTHS) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = recurrenceInterval,
                        onValueChange = { recurrenceInterval = it.filter { char -> char.isDigit() } },
                        label = { Text("Repeat interval N") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Optional Recurrence End Date
                if (recurrenceType != RecurrenceType.NONE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = recurrenceEndDateIso,
                        onValueChange = { recurrenceEndDateIso = it },
                        label = { Text("Recurrence End Date (Optional: YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reminder Settings
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enable Reminder Notification", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = isReminderEnabled,
                        onCheckedChange = { isReminderEnabled = it }
                    )
                }

                if (isReminderEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(0 to "At time", 10 to "10m before", 15 to "15m before", 30 to "30m before").forEach { (offset, label) ->
                            FilterChip(
                                selected = reminderMinutesBefore == offset,
                                onClick = { reminderMinutesBefore = offset },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Subtasks Manager
                Text("Subtasks", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        label = { Text("Add subtask") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (newSubtaskText.isNotBlank()) {
                                subtasks.add(
                                    Subtask(
                                        id = System.currentTimeMillis().toString(),
                                        title = newSubtaskText.trim(),
                                        isCompleted = false
                                    )
                                )
                                newSubtaskText = ""
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Subtask")
                    }
                }

                subtasks.forEachIndexed { index, sub ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text("• ${sub.title}", fontSize = 12.sp, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { subtasks.removeAt(index) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Personal Notes & Checkpoints") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        isTitleError = true
                        return@Button
                    }
                    if (editScope == "THIS" && onSaveOccurrenceOnly != null) {
                        onSaveOccurrenceOnly(
                            title.trim(),
                            emoji,
                            description.trim(),
                            category,
                            priority,
                            if (dueTime.isBlank()) null else dueTime.trim(),
                            durationMinutes,
                            subtasks.toList(),
                            notes.trim(),
                            isReminderEnabled,
                            reminderMinutesBefore
                        )
                    } else {
                        val intervalInt = recurrenceInterval.toIntOrNull() ?: 1
                        onSave(
                            title.trim(),
                            emoji,
                            description.trim(),
                            category,
                            priority,
                            dueDateIso,
                            if (dueTime.isBlank()) null else dueTime.trim(),
                            durationMinutes,
                            recurrenceType,
                            intervalInt,
                            selectedDays.toList(),
                            if (recurrenceEndDateIso.isBlank()) null else recurrenceEndDateIso.trim(),
                            subtasks.toList(),
                            notes.trim(),
                            isReminderEnabled,
                            reminderMinutesBefore
                        )
                    }
                },
                modifier = Modifier.testTag("task_save_button")
            ) {
                Text(if (editScope == "THIS") "Save This Date" else "Save Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitEditModal(
    initialHabit: HabitItem? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, emoji: String, description: String, reminderTime: String?) -> Unit
) {
    var title by remember { mutableStateOf(initialHabit?.title ?: "") }
    var emoji by remember { mutableStateOf(initialHabit?.emoji ?: "✨") }
    var description by remember { mutableStateOf(initialHabit?.description ?: "") }
    var reminderTime by remember { mutableStateOf(initialHabit?.reminderTime ?: "") }
    var isTitleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialHabit == null) "New Habit" else "Edit Habit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Select Emoji", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickEmojis.take(16).forEach { em ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (emoji == em) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { emoji = em }
                        ) {
                            Text(text = em, fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) isTitleError = false
                    },
                    label = { Text("Habit Title *") },
                    isError = isTitleError,
                    supportingText = if (isTitleError) { { Text("Title cannot be empty") } } else null,
                    leadingIcon = { Text(emoji, fontSize = 20.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Why this habit matters") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reminderTime,
                    onValueChange = { reminderTime = it },
                    label = { Text("Reminder Time (e.g. 05:30)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        isTitleError = true
                        return@Button
                    }
                    onSave(
                        title.trim(),
                        emoji,
                        description.trim(),
                        if (reminderTime.isBlank()) null else reminderTime.trim()
                    )
                }
            ) {
                Text("Save Habit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RestartChallengeDialog(
    onDismiss: () -> Unit,
    onConfirmRestart: (newStartDateIso: String) -> Unit
) {
    var startDateIso by remember { mutableStateOf(DateUtils.todayString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Restart 75-Day Challenge?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column {
                Text(
                    text = "This will reset your current progress and restart the 75-day discipline cycle from Day 1.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = startDateIso,
                    onValueChange = { startDateIso = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmRestart(startDateIso) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Confirm Restart")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BulkActionConfirmDialog(
    actionName: String,
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$actionName $count Tasks?") },
        text = {
            Text("Are you sure you want to $actionName $count selected tasks? This operation cannot be easily undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (actionName.contains("Delete", ignoreCase = true))
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                else ButtonDefaults.buttonColors()
            ) {
                Text(actionName)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
