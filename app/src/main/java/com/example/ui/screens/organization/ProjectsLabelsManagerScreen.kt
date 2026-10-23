package com.example.ui.screens.organization

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.domain.model.TodoistColorPalette
import com.example.ui.screens.labels.EditLabelBottomSheet
import com.example.ui.screens.projects.EditProjectBottomSheet
import com.example.ui.viewmodel.LifeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsLabelsManagerScreen(
    viewModel: LifeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Projects & Labels data
    val projects by viewModel.allProjects.collectAsStateWithLifecycle()
    val labels by viewModel.allLabels.collectAsStateWithLifecycle()
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle(initialValue = emptyList())

    // Bottom sheet & dialog states
    var projectToEdit by remember { mutableStateOf<ProjectItem?>(null) }
    var showProjectSheet by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<ProjectItem?>(null) }

    var labelToEdit by remember { mutableStateOf<LabelItem?>(null) }
    var showLabelSheet by remember { mutableStateOf(false) }
    var labelToDelete by remember { mutableStateOf<LabelItem?>(null) }

    // Color usage state
    val colorOrderingMode by viewModel.colorOrderingMode.collectAsStateWithLifecycle()
    val colorUsageCounts by viewModel.colorUsageCounts.collectAsStateWithLifecycle()
    val colorCustomOrder by viewModel.colorCustomOrder.collectAsStateWithLifecycle()

    // Calculate usage counts
    val projectTaskCounts = remember(tasks) {
        tasks.filter { !it.isArchived && !it.isOneTimeCompleted }
            .groupBy { it.projectId }
            .mapValues { it.value.size }
    }

    val labelTaskCounts = remember(tasks) {
        val map = mutableMapOf<Long, Int>()
        tasks.filter { !it.isArchived && !it.isOneTimeCompleted }.forEach { task ->
            task.labels.forEach { label ->
                map[label.id] = (map[label.id] ?: 0) + 1
            }
        }
        map
    }

    // Filtered lists
    val filteredProjects = remember(projects, searchQuery) {
        if (searchQuery.isBlank()) projects
        else projects.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredLabels = remember(labels, searchQuery) {
        if (searchQuery.isBlank()) labels
        else labels.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search projects & labels...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp)
                                .testTag("manager_search_input"),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                    } else {
                        Text(
                            text = "Projects & Labels",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSearchActive) {
                                isSearchActive = false
                                searchQuery = ""
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("manager_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        },
                        modifier = Modifier.testTag("manager_search_toggle")
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = if (isSearchActive) "Close Search" else "Search"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) {
                        projectToEdit = null
                        showProjectSheet = true
                    } else {
                        labelToEdit = null
                        showLabelSheet = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("manager_fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SecondaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Projects (${projects.size})", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    modifier = Modifier.testTag("tab_projects")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Labels (${labels.size})", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    modifier = Modifier.testTag("tab_labels")
                )
            }

            if (selectedTabIndex == 0) {
                // Projects List
                if (filteredProjects.isEmpty()) {
                    EmptyManagerView(
                        message = if (searchQuery.isNotBlank()) "No projects match '$searchQuery'" else "No projects yet. Create one to organize your life!",
                        actionText = "Create Project",
                        onAction = {
                            projectToEdit = null
                            showProjectSheet = true
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredProjects, key = { it.id }) { project ->
                            ProjectRowCard(
                                project = project,
                                activeTaskCount = projectTaskCounts[project.id] ?: 0,
                                onEdit = {
                                    projectToEdit = project
                                    showProjectSheet = true
                                },
                                onToggleFavorite = {
                                    viewModel.setProjectFavorite(project.id, !project.isFavorite)
                                },
                                onDelete = {
                                    projectToDelete = project
                                }
                            )
                        }
                    }
                }
            } else {
                // Labels List
                if (filteredLabels.isEmpty()) {
                    EmptyManagerView(
                        message = if (searchQuery.isNotBlank()) "No labels match '$searchQuery'" else "No labels yet. Add labels to quickly categorize tasks!",
                        actionText = "Create Label",
                        onAction = {
                            labelToEdit = null
                            showLabelSheet = true
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredLabels, key = { it.id }) { label ->
                            LabelRowCard(
                                label = label,
                                activeTaskCount = labelTaskCounts[label.id] ?: 0,
                                onEdit = {
                                    labelToEdit = label
                                    showLabelSheet = true
                                },
                                onToggleFavorite = {
                                    viewModel.setLabelFavorite(label.id, !label.isFavorite)
                                },
                                onDelete = {
                                    labelToDelete = label
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Bottom Sheet: Edit / Create Project ---
    if (showProjectSheet) {
        EditProjectBottomSheet(
            project = projectToEdit,
            colorOrderingMode = colorOrderingMode,
            colorUsageCounts = colorUsageCounts,
            colorCustomOrder = colorCustomOrder,
            onSave = { name, desc, emoji, colorId, isFav ->
                val existing = projectToEdit
                if (existing == null) {
                    viewModel.insertProject(
                        ProjectItem(
                            name = name,
                            description = desc,
                            emoji = emoji,
                            colorId = colorId,
                            isFavorite = isFav
                        )
                    )
                } else {
                    viewModel.updateProject(
                        existing.copy(
                            name = name,
                            description = desc,
                            emoji = emoji,
                            colorId = colorId,
                            isFavorite = isFav
                        )
                    )
                }
            },
            onColorUsed = { colorId -> viewModel.recordColorUsage(colorId) },
            onModeChanged = { mode -> viewModel.setColorOrderingMode(mode) },
            onSaveCustomOrder = { order -> viewModel.saveCustomColorOrder(order) },
            onResetCustomOrder = { viewModel.resetCustomColorOrder() },
            onDismiss = { showProjectSheet = false }
        )
    }

    // --- Bottom Sheet: Edit / Create Label ---
    if (showLabelSheet) {
        EditLabelBottomSheet(
            label = labelToEdit,
            colorOrderingMode = colorOrderingMode,
            colorUsageCounts = colorUsageCounts,
            colorCustomOrder = colorCustomOrder,
            onSave = { name, colorId, isFav ->
                val existing = labelToEdit
                if (existing == null) {
                    viewModel.insertLabel(
                        LabelItem(
                            name = name,
                            colorId = colorId,
                            isFavorite = isFav
                        )
                    )
                } else {
                    viewModel.updateLabel(
                        existing.copy(
                            name = name,
                            colorId = colorId,
                            isFavorite = isFav
                        )
                    )
                }
            },
            onColorUsed = { colorId -> viewModel.recordColorUsage(colorId) },
            onModeChanged = { mode -> viewModel.setColorOrderingMode(mode) },
            onSaveCustomOrder = { order -> viewModel.saveCustomColorOrder(order) },
            onResetCustomOrder = { viewModel.resetCustomColorOrder() },
            onDismiss = { showLabelSheet = false }
        )
    }

    // --- Safe Project Delete Confirmation Dialog ---
    projectToDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Project '${project.name}'?") },
            text = {
                Text(
                    "This action is safe and non-destructive. Associated tasks will NOT be deleted; they will simply be moved to Inbox. Any assigned trackers or subscriptions will also be safely unassigned."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(project.id)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Project")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Safe Label Delete Confirmation Dialog ---
    labelToDelete?.let { label ->
        AlertDialog(
            onDismissRequest = { labelToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Label '${label.name}'?") },
            text = {
                Text(
                    "This action is safe and non-destructive. Tasks tagged with this label will NOT be deleted; the label reference will simply be removed from them."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLabel(label.id)
                        labelToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Label")
                }
            },
            dismissButton = {
                TextButton(onClick = { labelToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProjectRowCard(
    project: ProjectItem,
    activeTaskCount: Int,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val projectColor = TodoistColorPalette.getColorById(project.colorId).composeColor

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("project_item_${project.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji & Color Circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(projectColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = project.emoji.ifBlank { "📁" },
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Project Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (project.isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (project.description.isNotBlank()) {
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (activeTaskCount == 1) "1 active task" else "$activeTaskCount active tasks",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Action Buttons
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (project.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Toggle Favorite",
                    tint = if (project.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Project",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Project",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun LabelRowCard(
    label: LabelItem,
    activeTaskCount: Int,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val labelColor = TodoistColorPalette.getColorById(label.colorId).composeColor

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("label_item_${label.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Label Color Tag
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(labelColor.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(labelColor)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Label Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "@${label.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (label.isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (activeTaskCount == 1) "1 tagged task" else "$activeTaskCount tagged tasks",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Action Buttons
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (label.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Toggle Favorite",
                    tint = if (label.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Label",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Label",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyManagerView(
    message: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(onClick = onAction) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(actionText)
            }
        }
    }
}
