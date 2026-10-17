package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import com.example.domain.model.DefaultTaskCategories
import com.example.domain.model.HabitItem
import com.example.domain.model.RecurrenceType
import com.example.domain.model.SchedulingMode
import com.example.domain.model.DurationUnit
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.model.TodoistColorPalette
import com.example.utils.DateUtils

@Composable
fun PriorityBadge(priority: TaskPriority) {
    Surface(
        color = priority.composeColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, priority.composeColor.copy(alpha = 0.5f)),
        modifier = Modifier.testTag("priority_badge_${priority.code.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = null,
                tint = priority.composeColor,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = priority.code,
                color = priority.composeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CategoryChip(
    categoryName: String,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val categoryItem = DefaultTaskCategories.getCategory(categoryName)
    val catColor = Color(categoryItem.colorHex)

    val bg = if (isSelected) catColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
    val border = if (isSelected) catColor else Color.Transparent

    Surface(
        color = bg,
        shape = RoundedCornerShape(20.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, border) else null,
        modifier = Modifier
            .testTag("category_chip_${categoryName.replace(" ", "_").lowercase()}")
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(text = categoryItem.emoji, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = categoryItem.name,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TaskCard(
    task: TaskItem,
    onToggleComplete: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null,
    onReschedule: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
    onPauseToggle: (() -> Unit)? = null,
    onViewHistory: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expandedSubtasks by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                task.isCompletedForDate -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompletedForDate) 0.dp else 1.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}")
            .then(
                if (isSelectionMode && onSelectToggle != null)
                    Modifier.clickable { onSelectToggle() }
                else Modifier
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Bulk Selection Mode Checkbox OR Emoji Box
                if (isSelectionMode && onSelectToggle != null) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onSelectToggle() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                if (task.emoji.isNotBlank()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    ) {
                        Text(text = task.emoji, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = if (task.isCompletedForDate) TextDecoration.LineThrough else null
                            ),
                            color = if (task.isCompletedForDate)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else
                                MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        PriorityBadge(task.priority)
                    }

                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Meta chips row (Category, Project, Labels, Recurrence, Time, Reminders)
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CategoryChip(categoryName = task.category)

                        if (!task.projectName.isNullOrBlank()) {
                            val projColorObj = task.projectColorId?.let { TodoistColorPalette.getColorById(it) }
                            val projComposeColor = if (projColorObj != null) Color(projColorObj.colorLong) else MaterialTheme.colorScheme.primary
                            Surface(
                                color = projComposeColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "📁 ${task.projectName}",
                                    fontSize = 11.sp,
                                    color = projComposeColor,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        task.labels.forEach { label ->
                            Surface(
                                color = label.composeColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "#${label.name}",
                                    fontSize = 11.sp,
                                    color = label.composeColor,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (task.recurrenceType != RecurrenceType.NONE) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "🔄 ${task.recurrenceType.displayName}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (task.isPaused) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "⏸️ Paused",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (task.schedulingMode == SchedulingMode.DEADLINE) {
                            if (task.deadlineIso != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    val timeStr = if (!task.deadlineTime.isNullOrBlank()) " at ${task.deadlineTime}" else ""
                                    Text(
                                        text = "⏳ Due ${DateUtils.formatDisplayDate(task.deadlineIso)}$timeStr",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        } else {
                            if (!task.dueTime.isNullOrBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    val durLabel = if (task.durationAmount == 1) task.durationUnit.singularName else task.durationUnit.displayName
                                    Text(
                                        text = "⏰ ${task.dueTime} (${task.durationAmount} $durLabel)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (task.durationAmount > 0 && task.durationUnit != DurationUnit.MINUTES) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    val durLabel = if (task.durationAmount == 1) task.durationUnit.singularName else task.durationUnit.displayName
                                    Text(
                                        text = "⏱️ ${task.durationAmount} $durLabel",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (task.isReminderEnabled) {
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "🔔",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Priority-colored Completion Toggle Circle (normal mode)
                if (!isSelectionMode) {
                    val priorityColor = task.priority.composeColor
                    TaskCompletionCircle(
                        isCompleted = task.isCompletedForDate,
                        onToggle = onToggleComplete,
                        activeColor = MaterialTheme.colorScheme.primary,
                        borderColor = priorityColor,
                        isAnimationEnabled = true,
                        size = 24.dp,
                        modifier = Modifier.testTag("task_checkbox_${task.id}")
                    )
                }
            }

            // Subtasks preview & expand
            if (task.subtasks.isNotEmpty()) {
                val completedSubtasks = task.subtasks.count { it.isCompleted }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { expandedSubtasks = !expandedSubtasks }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Subtasks ($completedSubtasks/${task.subtasks.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (expandedSubtasks) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand subtasks",
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = expandedSubtasks) {
                    Column(modifier = Modifier.padding(top = 6.dp, start = 8.dp)) {
                        task.subtasks.forEach { subtask ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleSubtask(subtask.id) }
                                    .padding(vertical = 2.dp)
                            ) {
                                Checkbox(
                                    checked = subtask.isCompleted,
                                    onCheckedChange = { onToggleSubtask(subtask.id) },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = subtask.title,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null
                                    ),
                                    color = if (subtask.isCompleted)
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Notes preview if present
            if (task.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "📝 ${task.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Actions row (Edit, Delete, and More Options Menu)
            if (!isSelectionMode) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("task_edit_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Task",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // More Options Dropdown
                    Box {
                        IconButton(
                            onClick = { showOptionsMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            if (onDuplicate != null) {
                                DropdownMenuItem(
                                    text = { Text("Duplicate") },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        onDuplicate()
                                    }
                                )
                            }
                            if (onReschedule != null) {
                                DropdownMenuItem(
                                    text = { Text("Reschedule") },
                                    leadingIcon = { Icon(Icons.Default.EditCalendar, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        onReschedule()
                                    }
                                )
                            }
                            if (task.recurrenceType != RecurrenceType.NONE && onSkip != null) {
                                DropdownMenuItem(
                                    text = { Text("Skip this occurrence") },
                                    leadingIcon = { Icon(Icons.Default.SkipNext, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        onSkip()
                                    }
                                )
                            }
                            if (task.recurrenceType != RecurrenceType.NONE && onPauseToggle != null) {
                                DropdownMenuItem(
                                    text = { Text(if (task.isPaused) "Resume recurrence" else "Pause recurrence") },
                                    leadingIcon = { Icon(if (task.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        onPauseToggle()
                                    }
                                )
                            }
                            if (onViewHistory != null && task.completedDates.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("View History (${task.completedDates.size})") },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        onViewHistory()
                                    }
                                )
                            }
                            if (onArchive != null) {
                                DropdownMenuItem(
                                    text = { Text(if (task.isArchived) "Unarchive" else "Archive") },
                                    leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        onArchive()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showOptionsMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskHistoryDialog(
    task: TaskItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Completion History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${task.emoji} ${task.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Total completed occurrences: ${task.completedDates.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (task.completedDates.isEmpty()) {
                    Text("No completion history recorded yet.")
                } else {
                    task.completedDates.sortedDescending().take(15).forEach { date ->
                        Text("• ${DateUtils.formatDisplayDate(date)} ($date)", fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun RescheduleDialog(
    task: TaskItem,
    initialDateIso: String,
    onDismiss: () -> Unit,
    onConfirm: (newDateIso: String) -> Unit
) {
    var newDate by remember { mutableStateOf(initialDateIso) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reschedule Task") },
        text = {
            Column {
                Text("Move '${task.title}' to a new date:")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newDate,
                    onValueChange = { newDate = it },
                    label = { Text("New Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newDate.isNotBlank()) {
                        onConfirm(newDate.trim())
                    }
                }
            ) {
                Text("Reschedule")
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
fun HabitCard(
    habit: HabitItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (habit.isCompletedToday)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (habit.isCompletedToday)
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        else null,
        modifier = modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (habit.isCompletedToday)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onToggle() }
                    .testTag("habit_toggle_${habit.id}")
            ) {
                if (habit.isCompletedToday) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Text(text = habit.emoji, fontSize = 22.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (habit.isCompletedToday) TextDecoration.LineThrough else null
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (habit.description.isNotBlank()) {
                    Text(
                        text = habit.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "🔥", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${habit.currentStreak}d streak",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }

                    if (habit.reminderTime != null) {
                        Text(
                            text = "⏰ ${habit.reminderTime}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "Week: ${(habit.weeklyCompletionRate * 100).toInt()}%",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WaterTrackerCard(
    currentMl: Int,
    goalMl: Int,
    onAdd: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (currentMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "water_progress")

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .testTag("water_tracker_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2FE))
                    ) {
                        Text(text = "💧", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Daily Hydration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$currentMl / $goalMl ml (${(progress * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onReset,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Water",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = Color(0xFF0284C7),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalButton(
                    onClick = { onAdd(250) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("water_add_250")
                ) {
                    Text("+250 ml", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                FilledTonalButton(
                    onClick = { onAdd(500) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("water_add_500")
                ) {
                    Text("+500 ml", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { onAdd(-250) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.7f)
                        .testTag("water_sub_250")
                ) {
                    Text("-250", fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarHeatmapView(
    days: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .testTag("calendar_heatmap_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Consistency Heatmap (Last 5 Weeks)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Track your daily momentum and habit intensity",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                maxItemsInEachRow = 7,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                days.forEach { (dateIso, ratio) ->
                    val blockColor = when {
                        ratio >= 0.8f -> Color(0xFF059669)
                        ratio >= 0.5f -> Color(0xFF34D399)
                        ratio > 0f -> Color(0xFFA7F3D0)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(blockColor),
                        contentAlignment = Alignment.Center
                    ) {
                        val dayNum = dateIso.takeLast(2)
                        Text(
                            text = dayNum,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (ratio >= 0.8f) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    emoji: String,
    title: String,
    description: String,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .testTag("empty_state_card")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = emoji, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (actionButtonText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = actionButtonText, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
