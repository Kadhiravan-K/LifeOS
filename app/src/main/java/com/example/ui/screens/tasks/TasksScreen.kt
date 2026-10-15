package com.example.ui.screens.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DefaultTaskCategories
import com.example.domain.model.TaskItem
import com.example.ui.components.BulkActionConfirmDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.RescheduleDialog
import com.example.ui.components.TaskCard
import com.example.ui.components.TaskEditModal
import com.example.ui.components.TaskHistoryDialog
import com.example.ui.viewmodel.LifeViewModel
import com.example.ui.viewmodel.TasksUiState

@Composable
fun TasksScreen(
    viewModel: LifeViewModel,
    uiState: TasksUiState,
    modifier: Modifier = Modifier
) {
    var showTaskModal by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }
    var taskToReschedule by remember { mutableStateOf<TaskItem?>(null) }
    var historyTask by remember { mutableStateOf<TaskItem?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var pendingBulkAction by remember { mutableStateOf<String?>(null) }

    val filterOptions = listOf("All", "Today", "Upcoming", "Completed", "Archived")

    Scaffold(
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                FloatingActionButton(
                    onClick = {
                        editingTask = null
                        showTaskModal = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_create_task")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Create Task")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("tasks_screen")
        ) {
            // Header Row: Title, Select Mode, Sort
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = if (uiState.isSelectionMode) "${uiState.selectedTaskIds.size} Selected" else "Task Hub",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${uiState.filteredTasks.size} tasks in view",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.toggleSelectionMode() },
                        modifier = Modifier.testTag("toggle_selection_mode_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isSelectionMode) Icons.Default.Close else Icons.Default.SelectAll,
                            contentDescription = "Selection Mode"
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("sort_menu_button")
                        ) {
                            Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort Tasks")
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            listOf("Priority", "Due Date", "Title").forEach { sortOption ->
                                DropdownMenuItem(
                                    text = { Text(sortOption) },
                                    onClick = {
                                        viewModel.setTaskSort(sortOption)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Bulk Actions Bar (When in selection mode)
            AnimatedVisibility(visible = uiState.isSelectionMode) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.selectAllFilteredTasks() },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Select All", fontSize = 11.sp)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { viewModel.bulkCompleteSelected() },
                                enabled = uiState.selectedTaskIds.isNotEmpty()
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Complete Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { pendingBulkAction = "Archive" },
                                enabled = uiState.selectedTaskIds.isNotEmpty()
                            ) {
                                Icon(Icons.Default.Archive, contentDescription = "Archive Selected")
                            }
                            IconButton(
                                onClick = { pendingBulkAction = "Delete" },
                                enabled = uiState.selectedTaskIds.isNotEmpty()
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setTaskSearchQuery(it) },
                placeholder = { Text("Search tasks...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setTaskSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tasks_search_field")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Tabs (All, Today, Upcoming, Completed, Archived)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                filterOptions.forEach { filter ->
                    FilterChip(
                        selected = uiState.selectedFilter == filter,
                        onClick = { viewModel.setTaskFilter(filter) },
                        label = { Text(filter, fontSize = 12.sp) },
                        modifier = Modifier.testTag("filter_chip_${filter.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Category Chips (Horizontal Scroll)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    selected = uiState.selectedCategory == null,
                    onClick = { viewModel.setTaskCategoryFilter(null) },
                    label = { Text("All Categories", fontSize = 11.sp) }
                )
                DefaultTaskCategories.list.forEach { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category.name,
                        onClick = {
                            if (uiState.selectedCategory == category.name) {
                                viewModel.setTaskCategoryFilter(null)
                            } else {
                                viewModel.setTaskCategoryFilter(category.name)
                            }
                        },
                        label = { Text("${category.emoji} ${category.name}", fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tasks List
            if (uiState.filteredTasks.isEmpty()) {
                EmptyStateCard(
                    emoji = "📝",
                    title = "No tasks found",
                    description = if (uiState.searchQuery.isNotEmpty()) "Try searching for a different keyword or category" else "Tap + to create a task or install master life routines!",
                    actionButtonText = if (uiState.allTasks.isEmpty()) "Install Default Routines" else "New Task",
                    onActionClick = {
                        if (uiState.allTasks.isEmpty()) {
                            viewModel.installDefaultRoutines()
                        } else {
                            editingTask = null
                            showTaskModal = true
                        }
                    }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.filteredTasks, key = { it.id }) { task ->
                        val isSelected = uiState.selectedTaskIds.contains(task.id)
                        TaskCard(
                            task = task,
                            isSelectionMode = uiState.isSelectionMode,
                            isSelected = isSelected,
                            onSelectToggle = { viewModel.toggleTaskSelection(task.id) },
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

    // Task Create / Edit Modal
    if (showTaskModal) {
        TaskEditModal(
            initialTask = editingTask,
            existingTasksOnDate = uiState.filteredTasks,
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

    // Reschedule Occurrence Dialog
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

    // Completion History Dialog
    if (historyTask != null) {
        TaskHistoryDialog(
            task = historyTask!!,
            onDismiss = { historyTask = null }
        )
    }

    // Bulk Action Confirmation Dialog
    if (pendingBulkAction != null) {
        BulkActionConfirmDialog(
            actionName = pendingBulkAction!!,
            count = uiState.selectedTaskIds.size,
            onDismiss = { pendingBulkAction = null },
            onConfirm = {
                if (pendingBulkAction == "Delete") {
                    viewModel.bulkDeleteSelected()
                } else if (pendingBulkAction == "Archive") {
                    viewModel.bulkArchiveSelected()
                }
                pendingBulkAction = null
            }
        )
    }
}
