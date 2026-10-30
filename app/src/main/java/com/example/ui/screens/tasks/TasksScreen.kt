package com.example.ui.screens.tasks

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import com.example.ui.components.TaskGridCard
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ColorOrderingMode
import com.example.domain.model.LabelItem
import com.example.domain.model.ProjectItem
import com.example.domain.model.TaskItem
import com.example.domain.model.TodoistColorPalette
import com.example.ui.components.BulkActionConfirmDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.RescheduleDialog
import com.example.ui.components.TaskAdvancedEditorSheet
import com.example.ui.components.TaskCard
import com.example.ui.components.TaskHistoryDialog
import com.example.ui.components.TaskQuickAddSheet
import com.example.ui.screens.labels.EditLabelBottomSheet
import com.example.ui.screens.projects.EditProjectBottomSheet
import com.example.ui.viewmodel.LifeViewModel
import com.example.ui.viewmodel.TasksUiState

@Composable
fun TasksScreen(
    viewModel: LifeViewModel,
    uiState: TasksUiState,
    onNavigateToHabits: () -> Unit = {},
    onNavigateToChallenge: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showQuickAddSheet by remember { mutableStateOf(false) }
    var showAdvancedEditorSheet by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskItem?>(null) }
    var taskToReschedule by remember { mutableStateOf<TaskItem?>(null) }
    var historyTask by remember { mutableStateOf<TaskItem?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
        var pendingBulkAction by remember { mutableStateOf<String?>(null) }
    var isGridView by remember { mutableStateOf(false) }

    // Project & Label sheet states
    var projectToEdit by remember { mutableStateOf<ProjectItem?>(null) }
    var showEditProjectSheet by remember { mutableStateOf(false) }
    var labelToEdit by remember { mutableStateOf<LabelItem?>(null) }
    var showEditLabelSheet by remember { mutableStateOf(false) }

    // Color preferences flows for Project & Label sheets
    val colorOrderingMode by viewModel.colorOrderingMode.collectAsStateWithLifecycle()
    val colorUsageCounts by viewModel.colorUsageCounts.collectAsStateWithLifecycle()
    val colorCustomOrder by viewModel.colorCustomOrder.collectAsStateWithLifecycle()
    val defaultTaskDuration by viewModel.defaultTaskDurationFlow.collectAsStateWithLifecycle()

    // Habit and challenge overview states for navigation cards
    val habitsList by viewModel.habits.collectAsStateWithLifecycle()
    val challengeState by viewModel.challengeState.collectAsStateWithLifecycle()

    val completedHabitsCount = habitsList.count { it.isCompletedToday }
    val totalHabitsCount = habitsList.size

    val filterOptions = listOf("Inbox", "Today", "Upcoming", "Projects", "Labels", "All", "Completed", "Archived")

    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }

    Scaffold(
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                FloatingActionButton(
                    onClick = {
                        taskToEdit = null
                        showQuickAddSheet = true
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
                        text = if (uiState.isSelectionMode) "${uiState.selectedTaskIds.size} Selected" else "Tasks",
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
                        onClick = { isGridView = !isGridView },
                        modifier = Modifier.testTag("toggle_view_mode_button")
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View"
                        )
                    }

                    IconButton(
                        onClick = {
                            if (uiState.isSelectionMode) {
                                viewModel.clearSelection()
                            } else {
                                viewModel.toggleSelectionMode(true)
                            }
                        },
                        modifier = Modifier.testTag("toggle_selection_mode_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isSelectionMode) Icons.Default.Close else Icons.Default.SelectAll,
                            contentDescription = if (uiState.isSelectionMode) "Cancel Selection" else "Selection Mode"
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
                            listOf(
                                "Priority" to "Priority (High to Low)",
                                "Due Date" to "Due Date (Earliest First)",
                                "Title" to "Title (A to Z)",
                                "Created" to "Created (Newest First)"
                            ).forEach { (sortKey, sortLabel) ->
                                DropdownMenuItem(
                                    text = { Text(sortLabel) },
                                    onClick = {
                                        viewModel.setTaskSort(sortKey)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { viewModel.clearSelection() },
                                modifier = Modifier.testTag("cancel_selection_button")
                            ) {
                                Text("Cancel", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            val allFilteredSelected = uiState.filteredTasks.isNotEmpty() &&
                                uiState.filteredTasks.all { uiState.selectedTaskIds.contains(it.id) }
                            TextButton(onClick = { viewModel.selectAllFilteredTasks() }) {
                                Text(if (allFilteredSelected) "Deselect All" else "Select All", fontSize = 12.sp)
                            }
                        }
                        Row {
                            IconButton(
                                onClick = { viewModel.bulkCompleteSelected() },
                                enabled = uiState.selectedTaskIds.isNotEmpty()
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Bulk Complete", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { pendingBulkAction = "Archive" },
                                enabled = uiState.selectedTaskIds.isNotEmpty()
                            ) {
                                Icon(Icons.Default.Archive, contentDescription = "Bulk Archive")
                            }
                            IconButton(
                                onClick = { pendingBulkAction = "Delete" },
                                enabled = uiState.selectedTaskIds.isNotEmpty()
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Bulk Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Search Bar
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setTaskSearchQuery(it) },
                placeholder = { Text("Search tasks, projects, labels...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setTaskSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_search_bar")
            )

            // Section Filter Chips (Inbox, Today, Upcoming, Projects, Labels, All, Completed, Archived)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                filterOptions.forEach { filter ->
                    val isSelected = uiState.selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (filter == "Projects") {
                                viewModel.setTaskFilter("Projects", projectId = null, labelId = null)
                            } else if (filter == "Labels") {
                                viewModel.setTaskFilter("Labels", projectId = null, labelId = null)
                            } else {
                                viewModel.setTaskFilter(filter, projectId = null, labelId = null)
                            }
                        },
                        label = {
                            Text(
                                text = filter,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("filter_chip_${filter.lowercase()}")
                    )
                }
            }

            // Habits & 75-Day Challenge Prominent Navigation Cards
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Habits Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToHabits() }
                        .testTag("tasks_nav_habits_card")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🧘", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Habits", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Text("$completedHabitsCount/$totalHabitsCount done", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // 75-Day Challenge Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToChallenge() }
                        .testTag("tasks_nav_challenge_card")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("75-Day", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Text("Day ${challengeState.currentDayNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Projects Sub-Section View (when "Projects" filter selected)
            if (uiState.selectedFilter == "Projects") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Projects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = {
                            projectToEdit = null
                            showEditProjectSheet = true
                        },
                        modifier = Modifier.testTag("add_project_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Project")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                ) {
                    uiState.projects.forEach { project ->
                        val isSelectedProject = uiState.selectedProjectId == project.id
                        val pColor = TodoistColorPalette.getColorById(project.colorId)
                        val composeColor = Color(pColor.colorLong)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelectedProject) composeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelectedProject) androidx.compose.foundation.BorderStroke(1.5.dp, composeColor) else null,
                            modifier = Modifier
                                .clickable {
                                    if (isSelectedProject) {
                                        viewModel.selectProject(null)
                                    } else {
                                        viewModel.selectProject(project.id)
                                    }
                                }
                                .testTag("project_item_${project.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(composeColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${project.emoji} ${project.name}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelectedProject) FontWeight.Bold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${project.taskCount})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )

                                IconButton(
                                    onClick = {
                                        projectToEdit = project
                                        showEditProjectSheet = true
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Project", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Labels Sub-Section View (when "Labels" filter selected)
            if (uiState.selectedFilter == "Labels") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Labels", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = {
                            labelToEdit = null
                            showEditLabelSheet = true
                        },
                        modifier = Modifier.testTag("add_label_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Label")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                ) {
                    uiState.labels.forEach { label ->
                        val isSelectedLabel = uiState.selectedLabelId == label.id
                        val lColor = label.composeColor

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelectedLabel) lColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelectedLabel) androidx.compose.foundation.BorderStroke(1.5.dp, lColor) else null,
                            modifier = Modifier
                                .clickable {
                                    if (isSelectedLabel) {
                                        viewModel.selectLabel(null)
                                    } else {
                                        viewModel.selectLabel(label.id)
                                    }
                                }
                                .testTag("label_item_${label.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "#${label.name}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = lColor,
                                    fontWeight = if (isSelectedLabel) FontWeight.Bold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${label.taskCount})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )

                                IconButton(
                                    onClick = {
                                        labelToEdit = label
                                        showEditLabelSheet = true
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Label", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Active Project / Label Filter Banner
            if (uiState.selectedProjectId != null || uiState.selectedLabelId != null) {
                Spacer(modifier = Modifier.height(6.dp))
                val bannerText = when {
                    uiState.selectedProjectId != null -> {
                        val proj = uiState.projects.firstOrNull { it.id == uiState.selectedProjectId }
                        "Project: ${proj?.emoji ?: "📁"} ${proj?.name ?: "Selected"}"
                    }
                    uiState.selectedLabelId != null -> {
                        val lbl = uiState.labels.firstOrNull { it.id == uiState.selectedLabelId }
                        "Label: #${lbl?.name ?: "Selected"}"
                    }
                    else -> ""
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(bannerText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        IconButton(
                            onClick = {
                                viewModel.setTaskFilter("All", projectId = null, labelId = null)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Filter", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Task List
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.filteredTasks.isEmpty()) {
                val emptyMessage = when (uiState.selectedFilter) {
                    "Inbox" -> "No tasks in your Inbox. You're all clear!"
                    "Today" -> "No tasks scheduled for today. Take a breather!"
                    "Upcoming" -> "No upcoming tasks on your horizon."
                    "Completed" -> "No completed tasks yet. Finish a task to see it here!"
                    "Archived" -> "No archived tasks."
                    else -> "No tasks match your current view. Tap + to add one."
                }
                EmptyStateCard(
                    emoji = "✅",
                    title = "All Clear",
                    description = emptyMessage
                )
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("task_grid")
                ) {
                    items(uiState.filteredTasks, key = { it.id }) { task ->
                        val isSelected = uiState.selectedTaskIds.contains(task.id)

                        TaskGridCard(
                            task = task,
                            onToggleComplete = { viewModel.toggleTask(task.id, uiState.selectedDateIso) },
                            onEdit = {
                                taskToEdit = task
                                showAdvancedEditorSheet = true
                            },
                            isSelectionMode = uiState.isSelectionMode,
                            isSelected = isSelected,
                            onSelectToggle = { viewModel.toggleTaskSelection(task.id) }
                        )
                    }
                    item(span = { GridItemSpan(2) }) {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("task_list")
                ) {
                    items(uiState.filteredTasks, key = { it.id }) { task ->
                        val isSelected = uiState.selectedTaskIds.contains(task.id)

                        TaskCard(
                            task = task,
                            onToggleComplete = { viewModel.toggleTask(task.id, uiState.selectedDateIso) },
                            onToggleSubtask = { subtaskId -> viewModel.toggleSubtask(task.id, subtaskId) },
                            onEdit = {
                                taskToEdit = task
                                showAdvancedEditorSheet = true
                            },
                            onDelete = { viewModel.deleteTask(task.id) },
                            onDuplicate = { viewModel.duplicateTask(task.id) },
                            onArchive = { viewModel.setTaskArchived(task.id, !task.isArchived) },
                            onReschedule = { taskToReschedule = task },
                            onSkip = if (task.recurrenceType != com.example.domain.model.RecurrenceType.NONE) {
                                { viewModel.skipOccurrence(task.id, uiState.selectedDateIso) }
                            } else null,
                            onPauseToggle = if (task.recurrenceType != com.example.domain.model.RecurrenceType.NONE) {
                                { viewModel.setTaskPaused(task.id, !task.isPaused) }
                            } else null,
                            onViewHistory = if (task.recurrenceType != com.example.domain.model.RecurrenceType.NONE) {
                                { historyTask = task }
                            } else null,
                            isSelectionMode = uiState.isSelectionMode,
                            isSelected = isSelected,
                            onSelectToggle = { viewModel.toggleTaskSelection(task.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Quick Add Sheet (Todoist-inspired bottom sheet for rapid task entry)
    if (showQuickAddSheet) {
        TaskQuickAddSheet(
            initialTask = taskToEdit,
            projects = uiState.projects,
            labels = uiState.labels,
            colorOrderingMode = colorOrderingMode,
            colorUsageCounts = colorUsageCounts,
            colorCustomOrder = colorCustomOrder,
            defaultDuration = defaultTaskDuration,
            onCreateProject = { proj, onDone ->
                viewModel.insertProject(proj) { newId -> onDone(newId) }
            },
            onCreateLabel = { lbl, onDone ->
                viewModel.insertLabel(lbl) { newId -> onDone(newId) }
            },
            onColorUsed = { viewModel.recordColorUsage(it) },
            onModeChanged = { viewModel.setColorOrderingMode(it) },
            onSaveCustomOrder = { viewModel.saveCustomColorOrder(it) },
            onResetCustomOrder = { viewModel.resetCustomColorOrder() },
            onSaveTask = { task ->
                viewModel.saveTaskItem(task)
                showQuickAddSheet = false
                taskToEdit = null
            },
            onSaveTaskWithMedication = { task, med ->
                viewModel.saveTaskItem(task, med)
                showQuickAddSheet = false
                taskToEdit = null
            },
            onExpandToAdvanced = { draft ->
                taskToEdit = draft
                showQuickAddSheet = false
                showAdvancedEditorSheet = true
            },
            onDismiss = {
                showQuickAddSheet = false
                taskToEdit = null
            }
        )
    }

    // Advanced Editor Sheet (Full detailed task editor)
    if (showAdvancedEditorSheet) {
        TaskAdvancedEditorSheet(
            task = taskToEdit,
            projects = uiState.projects,
            labels = uiState.labels,
            colorOrderingMode = colorOrderingMode,
            colorUsageCounts = colorUsageCounts,
            colorCustomOrder = colorCustomOrder,
            defaultDuration = defaultTaskDuration,
            onCreateProject = { proj, onDone ->
                viewModel.insertProject(proj) { newId -> onDone(newId) }
            },
            onCreateLabel = { lbl, onDone ->
                viewModel.insertLabel(lbl) { newId -> onDone(newId) }
            },
            onColorUsed = { viewModel.recordColorUsage(it) },
            onModeChanged = { viewModel.setColorOrderingMode(it) },
            onSaveCustomOrder = { viewModel.saveCustomColorOrder(it) },
            onResetCustomOrder = { viewModel.resetCustomColorOrder() },
            onSaveTask = { task ->
                viewModel.saveTaskItem(task)
                showAdvancedEditorSheet = false
                taskToEdit = null
            },
            onSaveTaskWithMedication = { task, med ->
                viewModel.saveTaskItem(task, med)
                showAdvancedEditorSheet = false
                taskToEdit = null
            },
            onDismiss = {
                showAdvancedEditorSheet = false
                taskToEdit = null
            }
        )
    }

    // Project Edit / Create Sheet
    if (showEditProjectSheet) {
        EditProjectBottomSheet(
            project = projectToEdit,
            colorOrderingMode = colorOrderingMode,
            colorUsageCounts = colorUsageCounts,
            colorCustomOrder = colorCustomOrder,
            onSave = { name, description, emoji, colorId, isFavorite ->
                val p = projectToEdit?.copy(
                    name = name,
                    description = description,
                    emoji = emoji,
                    colorId = colorId,
                    isFavorite = isFavorite
                ) ?: ProjectItem(
                    name = name,
                    description = description,
                    emoji = emoji,
                    colorId = colorId,
                    isFavorite = isFavorite
                )
                if (projectToEdit == null) {
                    viewModel.insertProject(p)
                } else {
                    viewModel.updateProject(p)
                }
                showEditProjectSheet = false
                projectToEdit = null
            },
            onColorUsed = { colorId -> viewModel.recordColorUsage(colorId) },
            onModeChanged = { mode -> viewModel.setColorOrderingMode(mode) },
            onSaveCustomOrder = { order -> viewModel.saveCustomColorOrder(order) },
            onResetCustomOrder = { viewModel.resetCustomColorOrder() },
            onDismiss = {
                showEditProjectSheet = false
                projectToEdit = null
            }
        )
    }

    // Label Edit / Create Sheet
    if (showEditLabelSheet) {
        EditLabelBottomSheet(
            label = labelToEdit,
            colorOrderingMode = colorOrderingMode,
            colorUsageCounts = colorUsageCounts,
            colorCustomOrder = colorCustomOrder,
            onSave = { name, colorId, isFavorite ->
                val l = labelToEdit?.copy(
                    name = name,
                    colorId = colorId,
                    isFavorite = isFavorite
                ) ?: LabelItem(
                    name = name,
                    colorId = colorId,
                    isFavorite = isFavorite
                )
                if (labelToEdit == null) {
                    viewModel.insertLabel(l)
                } else {
                    viewModel.updateLabel(l)
                }
                showEditLabelSheet = false
                labelToEdit = null
            },
            onColorUsed = { colorId -> viewModel.recordColorUsage(colorId) },
            onModeChanged = { mode -> viewModel.setColorOrderingMode(mode) },
            onSaveCustomOrder = { order -> viewModel.saveCustomColorOrder(order) },
            onResetCustomOrder = { viewModel.resetCustomColorOrder() },
            onDismiss = {
                showEditLabelSheet = false
                labelToEdit = null
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
