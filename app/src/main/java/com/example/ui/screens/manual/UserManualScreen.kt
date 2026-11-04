package com.example.ui.screens.manual

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EmptyStateCard

enum class ManualTab(val title: String, val icon: ImageVector) {
    COMPONENTS("REUSABLE FRONTEND UI COMPONENTS", Icons.Default.Code),
    GUIDES("User Manual & Guides", Icons.Default.MenuBook)
}

/**
 * Integrated User Manual Screen containing the interactive Reusable Frontend UI Components library
 * and comprehensive feature guides.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManualScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: ManualTab = ManualTab.COMPONENTS,
    onNavigateToSubpage: ((com.example.ui.screens.settings.SettingsSubpage) -> Unit)? = null
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    var selectedComponent by remember { mutableStateOf<ComponentEntry?>(null) }

    // Component Gallery States
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ComponentCategory.ALL) }
    var selectedStatus by remember { mutableStateOf<ComponentStatus?>(null) }
    var isCompactView by remember { mutableStateOf(false) }

    val filteredComponents = remember(searchQuery, selectedCategory, selectedStatus) {
        MasterComponentCatalog.search(
            query = searchQuery,
            category = selectedCategory,
            status = selectedStatus
        )
    }

    val totalCount = MasterComponentCatalog.items.size
    val existingCount = MasterComponentCatalog.items.count { it.status == ComponentStatus.EXISTING }
    val partialCount = MasterComponentCatalog.items.count { it.status == ComponentStatus.PARTIAL }
    val proposedCount = MasterComponentCatalog.items.count { it.status == ComponentStatus.PROPOSED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LifeOS User Manual",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Developer Reference & Architecture Guide",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("manual_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedTab == ManualTab.COMPONENTS) {
                        FilledTonalIconButton(
                            onClick = { isCompactView = !isCompactView },
                            modifier = Modifier.testTag("gallery_view_toggle")
                        ) {
                            Icon(
                                imageVector = if (isCompactView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle Density"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Top Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ManualTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tab.title, fontSize = 11.sp, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal)
                            }
                        },
                        modifier = Modifier.testTag("manual_tab_${tab.name.lowercase()}")
                    )
                }
            }

            when (selectedTab) {
                ManualTab.COMPONENTS -> {
                    ComponentGallerySection(
                        components = filteredComponents,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedCategory = selectedCategory,
                        onCategorySelect = { selectedCategory = it },
                        selectedStatus = selectedStatus,
                        onStatusSelect = { selectedStatus = it },
                        totalCount = totalCount,
                        existingCount = existingCount,
                        partialCount = partialCount,
                        proposedCount = proposedCount,
                        isCompactView = isCompactView,
                        onComponentClick = { selectedComponent = it }
                    )
                }

                ManualTab.GUIDES -> {
                    FeatureGuidesSection(onNavigateSubpage = onNavigateToSubpage)
                }
            }
        }
    }

    // Detail & Live Preview Sheet
    selectedComponent?.let { comp ->
        ComponentDetailSheet(
            component = comp,
            onDismiss = { selectedComponent = null }
        )
    }
}

@Composable
private fun ComponentGallerySection(
    components: List<ComponentEntry>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: ComponentCategory,
    onCategorySelect: (ComponentCategory) -> Unit,
    selectedStatus: ComponentStatus?,
    onStatusSelect: (ComponentStatus?) -> Unit,
    totalCount: Int,
    existingCount: Int,
    partialCount: Int,
    proposedCount: Int,
    isCompactView: Boolean,
    onComponentClick: (ComponentEntry) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("component_gallery_list")
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search components by name, file, or tag...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("component_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Summary Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$totalCount Cataloged Components",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$existingCount Existing • $partialCount Partial • $proposedCount Proposed",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ComponentCategory.entries.forEach { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategorySelect(category) },
                        label = { Text(category.displayName, fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(category.icon, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        modifier = Modifier.testTag("cat_chip_${category.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Status Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { onStatusSelect(null) },
                    label = { Text("All Statuses", fontSize = 11.sp) }
                )
                ComponentStatus.entries.forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { onStatusSelect(status) },
                        label = { Text(status.label, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (components.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "\uD83D\uDD0D",
                    title = "No Components Found",
                    description = "Try adjusting your search query or removing category/status filters.",
                    actionButtonText = "Clear Filters",
                    onActionClick = {
                        onSearchChange("")
                        onCategorySelect(ComponentCategory.ALL)
                        onStatusSelect(null)
                    }
                )
            }
        } else {
            items(components, key = { it.id }) { item ->
                ComponentCatalogCard(
                    entry = item,
                    isCompact = isCompactView,
                    onClick = { onComponentClick(item) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ComponentCatalogCard(
    entry: ComponentEntry,
    isCompact: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("component_card_${entry.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = entry.category.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = entry.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = entry.category.displayName,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusBadge(entry.status)
            }

            if (!isCompact) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = entry.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "composable: ${entry.composableName}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("✨ Live Interactive Preview", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    FilledTonalButton(onClick = onClick) {
                        Text("Inspect & Preview", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureGuidesSection(
    onNavigateSubpage: ((com.example.ui.screens.settings.SettingsSubpage) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "LifeOS Architecture & Feature Guides",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Comprehensive overview of the 100% offline-first productivity system with direct action navigation (#12).",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        GuideCard(
            icon = Icons.Default.Hexagon,
            title = "1. Quick Reminders (Honeycomb)",
            description = "High-speed dashboard action canvas supporting 7 layout presets (Standard, Pyramid, Diamond, Heart, Snake, Zigzag, Freeform). Powered by mathematical axial coordinates with pinch-to-zoom scaling between 0.75x and 1.35x.",
            actionText = "Customize Honeycomb Appearance",
            onActionClick = { onNavigateSubpage?.invoke(com.example.ui.screens.settings.SettingsSubpage.APPEARANCE) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        GuideCard(
            icon = Icons.Default.CheckCircle,
            title = "2. Tasks & Recurrence Engine",
            description = "Dual-view tasks interface (1-column list vs. 2-column grid). 4-way sorting by Priority, Due Date, Title, or Created Date. RecurrenceEngine evaluates daily, weekly, monthly, and interval-based tasks deterministically across midnight boundaries.",
            actionText = "Open Productivity Settings",
            onActionClick = { onNavigateSubpage?.invoke(com.example.ui.screens.settings.SettingsSubpage.PRODUCTIVITY) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        GuideCard(
            icon = Icons.Default.CalendarMonth,
            title = "3. Calendar System & Dot Parity",
            description = "Month and week views with synchronized task indicators. Dot indicators strictly match the active day's uncompleted tasks using RecurrenceEngine.isTaskScheduledOnDate and !isCompletedOnDate, eliminating false dots on completed dates.",
            actionText = "Configure Time & Calendar",
            onActionClick = { onNavigateSubpage?.invoke(com.example.ui.screens.settings.SettingsSubpage.GENERAL) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        GuideCard(
            icon = Icons.Default.LocalFireDepartment,
            title = "4. 75-Day Challenge Discipline",
            description = "Habit-card aesthetic with daily completion tracking and progress calculation. Restart modal features interactive date selection with today/yesterday chips and custom date picker to anchor challenge timelines accurately.",
            actionText = "View Gamification & Habits",
            onActionClick = { onNavigateSubpage?.invoke(com.example.ui.screens.settings.SettingsSubpage.GAMIFICATION) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        GuideCard(
            icon = Icons.Default.Medication,
            title = "5. Health & Medication Reminders",
            description = "6 configurable meal period slots with customizable clock times and food relationships (Before/With/After). Custom medication names are protected against task title overwrites via an edit-guard state flag.",
            actionText = "Configure Medication Reminders",
            onActionClick = { onNavigateSubpage?.invoke(com.example.ui.screens.settings.SettingsSubpage.NOTIFICATIONS) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        GuideCard(
            icon = Icons.Default.Storage,
            title = "6. Storage, Data & Trash Retention",
            description = "Room SQLite v18 database with monotonic migrations. Soft-deleted tasks, habits, projects, and labels dynamically calculate expiration timestamps from user preferences (1-365 days) rather than hardcoded durations.",
            actionText = "Manage Storage & Backup",
            onActionClick = { onNavigateSubpage?.invoke(com.example.ui.screens.settings.SettingsSubpage.DATA_STORAGE) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        GuideCard(
            icon = Icons.Default.Security,
            title = "7. 100% Offline-First Architecture",
            description = "LifeOS contains ZERO cloud dependencies, zero analytics trackers, and omits the INTERNET permission entirely from AndroidManifest.xml. All backups are exported strictly through user-chosen local folders via Storage Access Framework (SAF).",
            actionText = "Read Privacy Policy & Terms",
            onActionClick = { onNavigateSubpage?.invoke(com.example.ui.screens.settings.SettingsSubpage.LEGAL_PRIVACY) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        GuideCard(
            icon = Icons.Default.MenuBook,
            title = "8. Knowledge & Glossary Library (#2)",
            description = "Downloadable data-only JSON knowledge packs with SHA-256 integrity verification. Preloaded with 8 foundational productivity mental models. Zero dynamic code/dex execution.",
            actionText = "Open Knowledge & Glossary Library",
            onActionClick = { onNavigateSubpage?.invoke(com.example.ui.screens.settings.SettingsSubpage.GLOSSARY) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        GuideCard(
            icon = Icons.Default.AutoAwesome,
            title = "9. Bidirectional Local MCP AI Bridge (#3)",
            description = "Anthropic Model Context Protocol bridge with 32-character bearer token security and profile isolation guard. Connect local LLMs to LifeOS tasks, projects, and heuristics.",
            actionText = "Open AI MCP Bridge",
            onActionClick = { onNavigateSubpage?.invoke(com.example.ui.screens.settings.SettingsSubpage.MCP_BRIDGE) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        GuideCard(
            icon = Icons.Default.Person,
            title = "10. Multi-Profile Workspaces & Onboarding (#14, #19)",
            description = "App-Entry Honeycomb Portal providing total workspace isolation between Personal, Work, and Custom profiles. Multi-profile survey questionnaire captures personalized display names, sleep rhythms, and hydration targets without telemetry.",
            actionText = "Manage Profile & Workspaces",
            onActionClick = { onNavigateSubpage?.invoke(com.example.ui.screens.settings.SettingsSubpage.GENERAL) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        GuideCard(
            icon = Icons.Default.Folder,
            title = "11. Advanced Project Workspace (#20)",
            description = "Notion-inspired multi-view database powering structured workflows across Table, Kanban Board, Calendar, List, and Gallery views. Supports custom status workflows, typed property columns, and profile-scoped project containers.",
            actionText = "Manage Projects & Workspace",
            onActionClick = { onNavigateSubpage?.invoke(com.example.ui.screens.settings.SettingsSubpage.PROJECTS_LABELS) }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun GuideCard(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (!actionText.isNullOrBlank() && onActionClick != null) {
                Spacer(modifier = Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = onActionClick,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(actionText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
