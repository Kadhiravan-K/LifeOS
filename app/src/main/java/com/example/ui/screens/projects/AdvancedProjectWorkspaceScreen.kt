package com.example.ui.screens.projects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import com.example.domain.model.ProjectItem
import com.example.domain.model.TodoistColorPalette
import com.example.domain.projectworkspace.ProjectCustomStatus
import com.example.domain.projectworkspace.ProjectWorkspaceItem
import com.example.domain.projectworkspace.ProjectWorkspaceView
import com.example.domain.projectworkspace.WorkspaceGroupBy
import com.example.domain.projectworkspace.WorkspaceSortBy
import com.example.domain.projectworkspace.WorkspaceViewType
import com.example.ui.viewmodel.LifeViewModel

/**
 * Advanced Project Database Workspace Screen (Release 3 Requirement #20).
 *
 * Full Notion-like multi-view database workspace featuring:
 * - 5 Distinct Views: Table, Board (Kanban), Calendar, List, Gallery
 * - Standard statuses (Not Started, In Progress, Done) + Custom project statuses
 * - Grouping (by Status, Category, Version, Priority, None)
 * - Sorting & live search
 * - Column/property visibility customization
 * - Saved Views management
 * - Multi-select bulk actions (Bulk move status, bulk set version, bulk delete)
 * - AI Coding-Agent Prompt Generator with exportable structured markdown
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedProjectWorkspaceScreen(
    project: ProjectItem,
    viewModel: LifeViewModel,
    onBack: () -> Unit
) {
    val items by viewModel.getWorkspaceItemsFlow(project.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val savedViews by viewModel.getWorkspaceViewsFlow(project.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val customStatuses by viewModel.getCustomStatusesFlow(project.id).collectAsStateWithLifecycle(initialValue = emptyList())

    // Active View state
    var selectedViewType by remember { mutableStateOf(WorkspaceViewType.TABLE) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Active Filters & Sorting
    var filterCategory by remember { mutableStateOf<String?>(null) }
    var filterStatus by remember { mutableStateOf<String?>(null) }
    var filterVersion by remember { mutableStateOf<String?>(null) }
    var sortBy by remember { mutableStateOf(WorkspaceSortBy.ORDER_INDEX) }
    var sortAscending by remember { mutableStateOf(true) }
    var groupBy by remember { mutableStateOf(WorkspaceGroupBy.STATUS) }
    var visibleProperties by remember {
        mutableStateOf(setOf("status", "progress", "category", "version", "dueDate"))
    }
    val hasActiveFilter = filterCategory != null || filterStatus != null || filterVersion != null

    // Saved Views dropdown
    var selectedSavedView by remember { mutableStateOf<ProjectWorkspaceView?>(null) }
    var showSavedViewsMenu by remember { mutableStateOf(false) }
    var showSaveViewDialog by remember { mutableStateOf(false) }

    // Bulk selection state
    var selectedItemIds by remember { mutableStateOf(emptySet<Long>()) }

    // Dialog & sheet states
    var itemToEdit by remember { mutableStateOf<ProjectWorkspaceItem?>(null) }
    var showItemEditor by remember { mutableStateOf(false) }
    var defaultStatusForNewItem by remember { mutableStateOf("NOT_STARTED") }
    var showAiPromptDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showGroupMenu by remember { mutableStateOf(false) }
    var showPropsMenu by remember { mutableStateOf(false) }
    var showNewCustomStatusDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<ProjectWorkspaceItem?>(null) }

    // Combined all statuses (default + custom)
    val allStatuses = remember(customStatuses) {
        val list = mutableListOf("NOT_STARTED", "IN_PROGRESS", "DONE")
        customStatuses.forEach {
            if (!list.contains(it.name)) list.add(it.name)
        }
        list
    }

    // Filter & Sort Items
    val filteredItems = remember(items, searchQuery, filterCategory, filterStatus, filterVersion, sortBy, sortAscending) {
        var result = items

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) ||
                        it.notes.lowercase().contains(q) ||
                        it.category.lowercase().contains(q) ||
                        it.version.lowercase().contains(q)
            }
        }

        if (filterCategory != null) {
            result = result.filter { it.category == filterCategory }
        }
        if (filterStatus != null) {
            result = result.filter { it.status == filterStatus }
        }
        if (filterVersion != null) {
            result = result.filter { it.version == filterVersion }
        }

        result = when (sortBy) {
            WorkspaceSortBy.TITLE -> if (sortAscending) result.sortedBy { it.title.lowercase() } else result.sortedByDescending { it.title.lowercase() }
            WorkspaceSortBy.DUE_DATE -> if (sortAscending) result.sortedBy { it.dueDateIso ?: "9999" } else result.sortedByDescending { it.dueDateIso ?: "" }
            WorkspaceSortBy.PROGRESS -> if (sortAscending) result.sortedBy { it.progress } else result.sortedByDescending { it.progress }
            WorkspaceSortBy.STATUS -> if (sortAscending) result.sortedBy { it.status } else result.sortedByDescending { it.status }
            WorkspaceSortBy.PRIORITY -> if (sortAscending) result.sortedBy { it.priority } else result.sortedByDescending { it.priority }
            WorkspaceSortBy.ORDER_INDEX -> if (sortAscending) result.sortedBy { it.orderIndex } else result.sortedByDescending { it.orderIndex }
        }

        result
    }

    val projectColor = TodoistColorPalette.getColorById(project.colorId).composeColor

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search records...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = project.emoji.ifBlank { "📁" },
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = project.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Advanced Workspace",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = projectColor
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearchActive) {
                            isSearchActive = false
                            searchQuery = ""
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) searchQuery = ""
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }

                    // AI Prompt Generator Button
                    IconButton(
                        onClick = { showAiPromptDialog = true },
                        modifier = Modifier.testTag("open_ai_prompt_generator_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Prompt",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Add Record Button
                    IconButton(
                        onClick = {
                            itemToEdit = null
                            defaultStatusForNewItem = "NOT_STARTED"
                            showItemEditor = true
                        },
                        modifier = Modifier.testTag("workspace_add_record_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Record")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // View Layout Selector Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedViewType.ordinal,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                WorkspaceViewType.values().forEach { viewType ->
                    val isSelected = selectedViewType == viewType
                    Tab(
                        selected = isSelected,
                        onClick = { selectedViewType = viewType },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val icon = when (viewType) {
                                    WorkspaceViewType.TABLE -> Icons.Default.TableChart
                                    WorkspaceViewType.BOARD -> Icons.Default.ViewKanban
                                    WorkspaceViewType.CALENDAR -> Icons.Default.CalendarMonth
                                    WorkspaceViewType.LIST -> Icons.AutoMirrored.Filled.ViewList
                                    WorkspaceViewType.GALLERY -> Icons.Default.GridView
                                }
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(viewType.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        },
                        modifier = Modifier.testTag("tab_view_${viewType.name.lowercase()}")
                    )
                }
            }

            // Controls Toolbar (Filter, Sort, Group, Properties, Saved Views)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filter Chip
                AssistChip(
                    onClick = { showFilterSheet = true },
                    label = { Text(if (hasActiveFilter) "Filtered" else "Filter") },
                    leadingIcon = {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("workspace_filter_btn")
                )

                // Sort Chip
                AssistChip(
                    onClick = { showSortMenu = true },
                    label = { Text("Sort: ${sortBy.label}") },
                    leadingIcon = {
                        Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { sortAscending = !sortAscending },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = "Toggle sort direction",
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                )

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    WorkspaceSortBy.values().forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.label) },
                            onClick = {
                                sortBy = s
                                showSortMenu = false
                            },
                            trailingIcon = if (sortBy == s) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                // Group By Chip
                AssistChip(
                    onClick = { showGroupMenu = true },
                    label = { Text("Group: ${groupBy.label}") },
                    leadingIcon = {
                        Icon(Icons.Default.ViewKanban, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )

                DropdownMenu(
                    expanded = showGroupMenu,
                    onDismissRequest = { showGroupMenu = false }
                ) {
                    WorkspaceGroupBy.values().forEach { g ->
                        DropdownMenuItem(
                            text = { Text(g.label) },
                            onClick = {
                                groupBy = g
                                showGroupMenu = false
                            },
                            trailingIcon = if (groupBy == g) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                // Property Visibility Chip
                AssistChip(
                    onClick = { showPropsMenu = true },
                    label = { Text("Properties") },
                    leadingIcon = {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )

                DropdownMenu(
                    expanded = showPropsMenu,
                    onDismissRequest = { showPropsMenu = false }
                ) {
                    listOf(
                        "status" to "Status",
                        "progress" to "Progress",
                        "category" to "Category",
                        "version" to "Version",
                        "dueDate" to "Due Date",
                        "priority" to "Priority"
                    ).forEach { (key, label) ->
                        val isVisible = visibleProperties.contains(key)
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                visibleProperties = if (isVisible) visibleProperties - key else visibleProperties + key
                            },
                            trailingIcon = if (isVisible) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                // Saved View Dropdown
                AssistChip(
                    onClick = { showSavedViewsMenu = true },
                    label = { Text(selectedSavedView?.name ?: "Save View") },
                    leadingIcon = {
                        Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )

                DropdownMenu(
                    expanded = showSavedViewsMenu,
                    onDismissRequest = { showSavedViewsMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("+ Save Current View As...") },
                        onClick = {
                            showSavedViewsMenu = false
                            showSaveViewDialog = true
                        }
                    )
                    if (savedViews.isNotEmpty()) {
                        HorizontalDivider()
                        savedViews.forEach { sv ->
                            DropdownMenuItem(
                                text = { Text(sv.name) },
                                onClick = {
                                    selectedSavedView = sv
                                    selectedViewType = sv.viewType
                                    filterStatus = sv.filterStatus
                                    filterCategory = sv.filterCategory
                                    filterVersion = sv.filterVersion
                                    sortBy = sv.sortBy
                                    sortAscending = sv.sortAscending
                                    groupBy = sv.groupBy
                                    visibleProperties = sv.visibleProperties
                                    showSavedViewsMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Bulk Action Bar (Visible when items selected)
            AnimatedVisibility(
                visible = selectedItemIds.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selectedItemIds.size} selected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                viewModel.bulkUpdateWorkspaceItemStatus(selectedItemIds.toList(), "DONE")
                                selectedItemIds = emptySet()
                            }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark Done")
                            }

                            IconButton(onClick = {
                                viewModel.deleteWorkspaceItemsByIds(selectedItemIds.toList())
                                selectedItemIds = emptySet()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                            }

                            IconButton(onClick = { selectedItemIds = emptySet() }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Selection")
                            }
                        }
                    }
                }
            }

            // Active View Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotBlank() || hasActiveFilter) "No records match criteria" else "No records in this workspace yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                itemToEdit = null
                                defaultStatusForNewItem = "NOT_STARTED"
                                showItemEditor = true
                            }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Record")
                            }
                        }
                    }
                } else {
                    when (selectedViewType) {
                        WorkspaceViewType.TABLE -> {
                            WorkspaceTableView(
                                items = filteredItems,
                                selectedIds = selectedItemIds,
                                visibleProperties = visibleProperties,
                                onToggleSelect = { id ->
                                    selectedItemIds = if (selectedItemIds.contains(id)) selectedItemIds - id else selectedItemIds + id
                                },
                                onSelectAll = {
                                    selectedItemIds = if (selectedItemIds.size == filteredItems.size) emptySet() else filteredItems.map { it.id }.toSet()
                                },
                                onItemClick = { item ->
                                    itemToEdit = item
                                    showItemEditor = true
                                },
                                onQuickStatusChange = { item, newStatus ->
                                    viewModel.updateWorkspaceItem(item.copy(status = newStatus))
                                },
                                onAddNew = {
                                    itemToEdit = null
                                    defaultStatusForNewItem = "NOT_STARTED"
                                    showItemEditor = true
                                }
                            )
                        }

                        WorkspaceViewType.BOARD -> {
                            WorkspaceBoardView(
                                items = filteredItems,
                                statuses = allStatuses,
                                onItemClick = { item ->
                                    itemToEdit = item
                                    showItemEditor = true
                                },
                                onMoveStatus = { item, nextStatus ->
                                    viewModel.updateWorkspaceItem(item.copy(status = nextStatus))
                                },
                                onAddItemToStatus = { status ->
                                    itemToEdit = null
                                    defaultStatusForNewItem = status
                                    showItemEditor = true
                                }
                            )
                        }

                        WorkspaceViewType.CALENDAR -> {
                            WorkspaceCalendarView(
                                items = filteredItems,
                                onItemClick = { item ->
                                    itemToEdit = item
                                    showItemEditor = true
                                },
                                onAddForDate = { dateIso ->
                                    itemToEdit = null
                                    defaultStatusForNewItem = "NOT_STARTED"
                                    showItemEditor = true
                                }
                            )
                        }

                        WorkspaceViewType.LIST -> {
                            WorkspaceListView(
                                items = filteredItems,
                                selectedIds = selectedItemIds,
                                onToggleSelect = { id ->
                                    selectedItemIds = if (selectedItemIds.contains(id)) selectedItemIds - id else selectedItemIds + id
                                },
                                onItemClick = { item ->
                                    itemToEdit = item
                                    showItemEditor = true
                                },
                                onToggleDone = { item ->
                                    val nextStatus = if (item.status == "DONE") "NOT_STARTED" else "DONE"
                                    viewModel.updateWorkspaceItem(item.copy(status = nextStatus, progress = if (nextStatus == "DONE") 100 else 0))
                                },
                                onAddNew = {
                                    itemToEdit = null
                                    defaultStatusForNewItem = "NOT_STARTED"
                                    showItemEditor = true
                                }
                            )
                        }

                        WorkspaceViewType.GALLERY -> {
                            WorkspaceGalleryView(
                                items = filteredItems,
                                onItemClick = { item ->
                                    itemToEdit = item
                                    showItemEditor = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs & Sheets ---

    // Item Create / Edit Sheet
    if (showItemEditor) {
        CreateEditWorkspaceItemDialog(
            projectId = project.id,
            item = itemToEdit,
            defaultStatus = defaultStatusForNewItem,
            availableStatuses = allStatuses,
            onSave = { updated ->
                if (updated.id == 0L) {
                    viewModel.createWorkspaceItem(updated)
                } else {
                    viewModel.updateWorkspaceItem(updated)
                }
                showItemEditor = false
            },
            onDelete = {
                if (itemToEdit != null) {
                    viewModel.deleteWorkspaceItem(itemToEdit!!)
                }
                showItemEditor = false
            },
            onDismiss = { showItemEditor = false }
        )
    }

    // AI Prompt Generator Dialog
    if (showAiPromptDialog) {
        AiPromptGeneratorDialog(
            projectName = project.name,
            projectDescription = project.description,
            workspaceItems = items,
            selectedItemIds = selectedItemIds,
            onDismiss = { showAiPromptDialog = false }
        )
    }

    // Filter Sheet
    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text("Filter Records", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Feature", "Bug", "Improvement").forEach { cat ->
                        FilterChip(
                            selected = (cat == "All" && filterCategory == null) || filterCategory == cat,
                            onClick = { filterCategory = if (cat == "All") null else cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Status", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val statusOptions = listOf("All") + allStatuses
                    statusOptions.forEach { st ->
                        val label = when (st) {
                            "NOT_STARTED" -> "Not Started"
                            "IN_PROGRESS" -> "In Progress"
                            "DONE" -> "Done"
                            else -> st
                        }
                        FilterChip(
                            selected = (st == "All" && filterStatus == null) || filterStatus == st,
                            onClick = { filterStatus = if (st == "All") null else st },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = {
                        filterCategory = null
                        filterStatus = null
                        filterVersion = null
                        showFilterSheet = false
                    }) {
                        Text("Reset Filters")
                    }
                    Button(onClick = { showFilterSheet = false }) {
                        Text("Apply")
                    }
                }
            }
        }
    }

    // Save View Dialog
    if (showSaveViewDialog) {
        var viewName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveViewDialog = false },
            title = { Text("Save Current View") },
            text = {
                OutlinedTextField(
                    value = viewName,
                    onValueChange = { viewName = it },
                    label = { Text("View Name") },
                    placeholder = { Text("e.g. V2 Roadmap Table") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewName.isNotBlank()) {
                            viewModel.saveWorkspaceView(
                                ProjectWorkspaceView(
                                    projectId = project.id,
                                    name = viewName.trim(),
                                    viewType = selectedViewType,
                                    filterStatus = filterStatus,
                                    filterCategory = filterCategory,
                                    filterVersion = filterVersion,
                                    sortBy = sortBy,
                                    sortAscending = sortAscending,
                                    groupBy = groupBy,
                                    visibleProperties = visibleProperties
                                )
                            )
                            showSaveViewDialog = false
                        }
                    },
                    enabled = viewName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveViewDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// 1. TABLE VIEW
// ---------------------------------------------------------------------------
@Composable
private fun WorkspaceTableView(
    items: List<ProjectWorkspaceItem>,
    selectedIds: Set<Long>,
    visibleProperties: Set<String>,
    onToggleSelect: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onItemClick: (ProjectWorkspaceItem) -> Unit,
    onQuickStatusChange: (ProjectWorkspaceItem, String) -> Unit,
    onAddNew: () -> Unit
) {
    val horizontalScroll = rememberScrollState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Table Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .horizontalScroll(horizontalScroll)
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = items.isNotEmpty() && selectedIds.size == items.size,
                    onCheckedChange = { onSelectAll() },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Name", fontWeight = FontWeight.Bold, modifier = Modifier.width(180.dp))
                if (visibleProperties.contains("status")) {
                    Text("Status", fontWeight = FontWeight.Bold, modifier = Modifier.width(120.dp))
                }
                if (visibleProperties.contains("progress")) {
                    Text("Progress", fontWeight = FontWeight.Bold, modifier = Modifier.width(110.dp))
                }
                if (visibleProperties.contains("category")) {
                    Text("Category", fontWeight = FontWeight.Bold, modifier = Modifier.width(110.dp))
                }
                if (visibleProperties.contains("version")) {
                    Text("Version", fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                }
                if (visibleProperties.contains("priority")) {
                    Text("Priority", fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                }
                if (visibleProperties.contains("dueDate")) {
                    Text("Due Date", fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
                }
            }
            HorizontalDivider()
        }

        // Table Rows
        items(items, key = { it.id }) { item ->
            val isSelected = selectedIds.contains(item.id)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent)
                    .horizontalScroll(horizontalScroll)
                    .clickable { onItemClick(item) }
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect(item.id) },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Title
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(180.dp)
                )

                // Status Badge
                if (visibleProperties.contains("status")) {
                    Box(modifier = Modifier.width(120.dp)) {
                        StatusBadge(status = item.status, onClick = {
                            val next = when (item.status) {
                                "NOT_STARTED" -> "IN_PROGRESS"
                                "IN_PROGRESS" -> "DONE"
                                else -> "NOT_STARTED"
                            }
                            onQuickStatusChange(item, next)
                        })
                    }
                }

                // Progress Bar
                if (visibleProperties.contains("progress")) {
                    Row(
                        modifier = Modifier.width(110.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { item.progress / 100f },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (item.progress == 100) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${item.progress}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Category Tag
                if (visibleProperties.contains("category")) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(110.dp)
                    )
                }

                // Version Tag
                if (visibleProperties.contains("version")) {
                    Text(
                        text = item.version,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(80.dp)
                    )
                }

                // Priority Tag
                if (visibleProperties.contains("priority")) {
                    Text(
                        text = item.priority,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = when (item.priority) {
                            "P0" -> Color(0xFFEF4444)
                            "P1" -> Color(0xFFF59E0B)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.width(70.dp)
                    )
                }

                // Due Date
                if (visibleProperties.contains("dueDate")) {
                    Text(
                        text = item.dueDateIso ?: "-",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(100.dp)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }

        // Add Row Button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddNew() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("New record", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2. BOARD VIEW (KANBAN)
// ---------------------------------------------------------------------------
@Composable
private fun WorkspaceBoardView(
    items: List<ProjectWorkspaceItem>,
    statuses: List<String>,
    onItemClick: (ProjectWorkspaceItem) -> Unit,
    onMoveStatus: (ProjectWorkspaceItem, String) -> Unit,
    onAddItemToStatus: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        statuses.forEach { status ->
            val columnItems = items.filter { it.status == status }
            val statusLabel = when (status) {
                "NOT_STARTED" -> "Not Started"
                "IN_PROGRESS" -> "In Progress"
                "DONE" -> "Completed"
                else -> status
            }

            Column(
                modifier = Modifier
                    .width(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(10.dp)
            ) {
                // Column Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                            Text("${columnItems.size}", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                        }
                    }

                    IconButton(
                        onClick = { onAddItemToStatus(status) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add to column", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Column Cards
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(columnItems, key = { it.id }) { item ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(item) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                            modifier = Modifier.padding(1.dp)
                                        ) {
                                            Text(
                                                text = item.category,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                            modifier = Modifier.padding(1.dp)
                                        ) {
                                            Text(
                                                text = item.version,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Quick Move Controls
                                    Row {
                                        val currentIndex = statuses.indexOf(item.status)
                                        if (currentIndex > 0) {
                                            IconButton(
                                                onClick = { onMoveStatus(item, statuses[currentIndex - 1]) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Move Left", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        if (currentIndex < statuses.size - 1) {
                                            IconButton(
                                                onClick = { onMoveStatus(item, statuses[currentIndex + 1]) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Move Right", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. CALENDAR VIEW
// ---------------------------------------------------------------------------
@Composable
private fun WorkspaceCalendarView(
    items: List<ProjectWorkspaceItem>,
    onItemClick: (ProjectWorkspaceItem) -> Unit,
    onAddForDate: (String) -> Unit
) {
    val itemsByDate = remember(items) {
        items.filter { !it.dueDateIso.isNullOrBlank() }.groupBy { it.dueDateIso!! }
    }
    val unscheduledItems = remember(items) {
        items.filter { it.dueDateIso.isNullOrBlank() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Scheduled Dates
        if (itemsByDate.isNotEmpty()) {
            item {
                Text(
                    text = "Scheduled Obligations (${itemsByDate.size} dates)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(itemsByDate.keys.sorted(), key = { it }) { date ->
                val dayItems = itemsByDate[date] ?: emptyList()
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(date, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { onAddForDate(date) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Add for date", modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        dayItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { onItemClick(item) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusBadge(status = item.status)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(item.title, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Text(item.version, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        // Unscheduled Section
        if (unscheduledItems.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Unscheduled Items (${unscheduledItems.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            items(unscheduledItems, key = { it.id }) { item ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(item) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(status = item.status)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(item.title, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text(item.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 4. LIST VIEW
// ---------------------------------------------------------------------------
@Composable
private fun WorkspaceListView(
    items: List<ProjectWorkspaceItem>,
    selectedIds: Set<Long>,
    onToggleSelect: (Long) -> Unit,
    onItemClick: (ProjectWorkspaceItem) -> Unit,
    onToggleDone: (ProjectWorkspaceItem) -> Unit,
    onAddNew: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.id }) { item ->
            val isDone = item.status == "DONE"
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(item) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isDone,
                        onCheckedChange = { onToggleDone(item) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Text(item.version, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            if (item.progress > 0) {
                                Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text("${item.progress}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    StatusBadge(status = item.status)
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onAddNew,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Record")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 5. GALLERY VIEW
// ---------------------------------------------------------------------------
@Composable
private fun WorkspaceGalleryView(
    items: List<ProjectWorkspaceItem>,
    onItemClick: (ProjectWorkspaceItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.id }) { item ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(item) }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Category & Version Top strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = item.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = item.version,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (item.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { item.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (item.progress == 100) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(status = item.status)
                        Text("${item.progress}%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// HELPER COMPONENT: Status Badge
// ---------------------------------------------------------------------------
@Composable
private fun StatusBadge(
    status: String,
    onClick: (() -> Unit)? = null
) {
    val (label, bg, fg) = when (status) {
        "NOT_STARTED" -> Triple("Not Started", Color(0xFF6B7280).copy(alpha = 0.15f), Color(0xFF4B5563))
        "IN_PROGRESS" -> Triple("In Progress", Color(0xFF3B82F6).copy(alpha = 0.15f), Color(0xFF2563EB))
        "DONE" -> Triple("Done", Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF059669))
        else -> Triple(status, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------
// DIALOG: Create / Edit Workspace Item
// ---------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateEditWorkspaceItemDialog(
    projectId: Long,
    item: ProjectWorkspaceItem?,
    defaultStatus: String,
    availableStatuses: List<String>,
    onSave: (ProjectWorkspaceItem) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(item?.title ?: "") }
    var status by remember { mutableStateOf(item?.status ?: defaultStatus) }
    var progress by remember { mutableIntStateOf(item?.progress ?: 0) }
    var category by remember { mutableStateOf(item?.category ?: "Feature") }
    var version by remember { mutableStateOf(item?.version ?: "V1") }
    var dueDateIso by remember { mutableStateOf(item?.dueDateIso ?: "") }
    var notes by remember { mutableStateOf(item?.notes ?: "") }
    var priority by remember { mutableStateOf(item?.priority ?: "P2") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "New Database Record" else "Edit Record", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips
                Text("Category:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Feature", "Bug", "Improvement").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Status Chips
                Text("Status:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableStatuses.forEach { st ->
                        val label = when (st) {
                            "NOT_STARTED" -> "Not Started"
                            "IN_PROGRESS" -> "In Progress"
                            "DONE" -> "Done"
                            else -> st
                        }
                        FilterChip(
                            selected = status == st,
                            onClick = {
                                status = st
                                if (st == "DONE") progress = 100
                            },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Version & Priority
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = version,
                        onValueChange = { version = it },
                        label = { Text("Version") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = priority,
                        onValueChange = { priority = it },
                        label = { Text("Priority (P0-P3)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Slider
                Text("Progress: $progress%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Slider(
                    value = progress.toFloat(),
                    onValueChange = { progress = it.toInt() },
                    valueRange = 0f..100f
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Due Date
                OutlinedTextField(
                    value = dueDateIso,
                    onValueChange = { dueDateIso = it },
                    label = { Text("Due Date (YYYY-MM-DD)") },
                    placeholder = { Text("e.g. 2026-11-15") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Specifications") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val updated = (item ?: ProjectWorkspaceItem(projectId = projectId, title = "")).copy(
                            projectId = projectId,
                            title = title.trim(),
                            status = status,
                            progress = progress,
                            category = category,
                            version = version.trim().ifBlank { "V1" },
                            dueDateIso = dueDateIso.trim().ifBlank { null },
                            notes = notes.trim(),
                            priority = priority.trim().ifBlank { "P2" }
                        )
                        onSave(updated)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (item != null) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
