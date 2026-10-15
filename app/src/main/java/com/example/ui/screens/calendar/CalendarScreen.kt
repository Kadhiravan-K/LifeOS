package com.example.ui.screens.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.RescheduleDialog
import com.example.ui.components.TaskCard
import com.example.ui.components.TaskEditModal
import com.example.ui.components.TaskHistoryDialog
import com.example.ui.viewmodel.CalendarUiState
import com.example.ui.viewmodel.LifeViewModel
import com.example.utils.DateUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    viewModel: LifeViewModel,
    uiState: CalendarUiState,
    modifier: Modifier = Modifier
) {
    var showTaskModal by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }
    var taskToReschedule by remember { mutableStateOf<TaskItem?>(null) }
    var historyTask by remember { mutableStateOf<TaskItem?>(null) }

    val todayStr = DateUtils.todayString()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTask = null
                    showTaskModal = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_calendar_add_task")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task to Date")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("calendar_screen")
        ) {
            // Header: Month Navigation & Today Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = uiState.monthYearTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = DateUtils.formatDisplayDate(uiState.selectedDateIso),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.selectCalendarDate(todayStr) }
                    ) {
                        Icon(imageVector = Icons.Default.Today, contentDescription = "Today")
                    }
                    IconButton(onClick = { viewModel.previousCalendarMonth() }) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                    }
                    IconButton(onClick = { viewModel.nextCalendarMonth() }) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Month")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // View Mode Toggle (Month vs Week)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = uiState.viewMode == "MONTH",
                    onClick = { viewModel.setCalendarViewMode("MONTH") },
                    label = { Text("Monthly View", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = uiState.viewMode == "WEEK",
                    onClick = { viewModel.setCalendarViewMode("WEEK") },
                    label = { Text("Weekly View", fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Calendar Grid / Strip Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Weekday headers (Mon-Sun)
                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                            Text(
                                text = day,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.viewMode == "MONTH") {
                        // Month Grid
                        FlowRow(
                            maxItemsInEachRow = 7,
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            uiState.monthGridDates.forEach { dateIso ->
                                val isSelected = dateIso == uiState.selectedDateIso
                                val isToday = dateIso == todayStr
                                val dayNum = dateIso.takeLast(2)
                                val isCurrentMonth = DateUtils.getMonthZeroIndexed(dateIso) == uiState.currentMonthZeroIndexed

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { viewModel.selectCalendarDate(dateIso) }
                                ) {
                                    Text(
                                        text = dayNum,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            !isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Week Strip
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            uiState.weekDates.forEach { dateIso ->
                                val isSelected = dateIso == uiState.selectedDateIso
                                val isToday = dateIso == todayStr
                                val dayNum = dateIso.takeLast(2)
                                val dayOfWeek = DateUtils.getDayOfWeek(dateIso).take(3)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.selectCalendarDate(dateIso) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = dayOfWeek,
                                        fontSize = 11.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dayNum,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Conflicts Alert on Selected Date (if any)
            if (uiState.conflictsOnSelectedDate.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WarningAmber, contentDescription = "Conflict", tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Schedule Conflict Detected",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }
                        uiState.conflictsOnSelectedDate.forEach { conflict ->
                            Text(
                                text = conflict.message,
                                fontSize = 11.sp,
                                color = Color(0xFF78350F),
                                modifier = Modifier.padding(top = 2.dp, start = 24.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Category & Priority Filters for Agenda
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    selected = uiState.selectedCategoryFilter == null,
                    onClick = { viewModel.setCalendarCategoryFilter(null) },
                    label = { Text("All Categories", fontSize = 11.sp) }
                )
                DefaultTaskCategories.list.forEach { cat ->
                    FilterChip(
                        selected = uiState.selectedCategoryFilter == cat.name,
                        onClick = {
                            if (uiState.selectedCategoryFilter == cat.name) {
                                viewModel.setCalendarCategoryFilter(null)
                            } else {
                                viewModel.setCalendarCategoryFilter(cat.name)
                            }
                        },
                        label = { Text("${cat.emoji} ${cat.name}", fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Agenda List for Selected Date
            if (uiState.tasksOnSelectedDate.isEmpty()) {
                EmptyStateCard(
                    emoji = "📅",
                    title = "No tasks for ${DateUtils.formatDisplayDate(uiState.selectedDateIso)}",
                    description = "Tap the + button to schedule a task or routine on this date.",
                    actionButtonText = "Schedule Task",
                    onActionClick = {
                        editingTask = null
                        showTaskModal = true
                    }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.tasksOnSelectedDate, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggleComplete = {
                                viewModel.toggleTaskCompletion(task.id, uiState.selectedDateIso)
                            },
                            onToggleSubtask = { subtaskId ->
                                viewModel.toggleSubtask(task.id, subtaskId)
                            },
                            onEdit = {
                                editingTask = task
                                showTaskModal = true
                            },
                            onDelete = {
                                viewModel.deleteTask(task.id)
                            },
                            onDuplicate = {
                                viewModel.duplicateTask(task.id)
                            },
                            onArchive = {
                                viewModel.setTaskArchived(task.id, !task.isArchived)
                            },
                            onReschedule = {
                                taskToReschedule = task
                            },
                            onSkip = {
                                viewModel.skipOccurrence(task.id, uiState.selectedDateIso)
                            },
                            onPauseToggle = {
                                viewModel.setTaskPaused(task.id, !task.isPaused)
                            },
                            onViewHistory = {
                                historyTask = task
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // Modals & Dialogs
    if (showTaskModal) {
        TaskEditModal(
            initialTask = editingTask,
            existingTasksOnDate = uiState.tasksOnSelectedDate,
            defaultDateIso = uiState.selectedDateIso,
            onDismiss = {
                showTaskModal = false
                editingTask = null
            },
            onSaveOccurrenceOnly = { title, emoji, description, category, priority, dueTime, durationMinutes, subtasks, notes, isReminderEnabled, reminderMinutesBefore ->
                if (editingTask != null) {
                    viewModel.editOccurrenceOnly(
                        parentTaskId = editingTask!!.id,
                        occurrenceDateIso = uiState.selectedDateIso,
                        title = title,
                        emoji = emoji,
                        description = description,
                        category = category,
                        priority = priority,
                        dueTime = dueTime,
                        durationMinutes = durationMinutes,
                        subtasks = subtasks,
                        notes = notes,
                        isReminderEnabled = isReminderEnabled,
                        reminderMinutesBefore = reminderMinutesBefore
                    )
                }
                showTaskModal = false
                editingTask = null
            },
            onSave = { title, emoji, description, category, priority, dueDateIso, dueTime, durationMinutes, recurrenceType, recurrenceInterval, recurrenceDaysOfWeek, recurrenceEndDateIso, subtasks, notes, isReminderEnabled, reminderMinutesBefore ->
                viewModel.saveTask(
                    id = editingTask?.id ?: 0L,
                    title = title,
                    emoji = emoji,
                    description = description,
                    category = category,
                    priority = priority,
                    dueDateIso = dueDateIso,
                    dueTime = dueTime,
                    durationMinutes = durationMinutes,
                    recurrenceType = recurrenceType,
                    recurrenceInterval = recurrenceInterval,
                    recurrenceDaysOfWeek = recurrenceDaysOfWeek,
                    recurrenceEndDateIso = recurrenceEndDateIso,
                    subtasks = subtasks,
                    notes = notes,
                    isReminderEnabled = isReminderEnabled,
                    reminderMinutesBefore = reminderMinutesBefore
                )
                showTaskModal = false
                editingTask = null
            }
        )
    }

    if (taskToReschedule != null) {
        RescheduleDialog(
            task = taskToReschedule!!,
            initialDateIso = uiState.selectedDateIso,
            onDismiss = { taskToReschedule = null },
            onConfirm = { newDateIso ->
                viewModel.rescheduleOccurrence(taskToReschedule!!.id, uiState.selectedDateIso, newDateIso)
                taskToReschedule = null
            }
        )
    }

    if (historyTask != null) {
        TaskHistoryDialog(
            task = historyTask!!,
            onDismiss = { historyTask = null }
        )
    }
}
