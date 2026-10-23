package com.example.ui.screens.essentials

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.essentials.EssentialsCatalog
import com.example.domain.essentials.SubscriptionTemplate
import com.example.domain.essentials.TrackerTemplate
import com.example.domain.subscription.BillingOccurrenceItem
import com.example.domain.subscription.BillStatus
import com.example.domain.subscription.SubscriptionItem
import com.example.domain.subscription.SubscriptionStatus
import com.example.domain.tracker.InventoryPolicy
import com.example.domain.tracker.TrackerItem
import com.example.ui.viewmodel.LifeViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackersSubscriptionsScreen(
    viewModel: LifeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Trackers, 1: Subscriptions
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    // Active sub-destination in this screen
    var viewingTrackerId by remember { mutableStateOf<Long?>(null) }
    var viewingSubscriptionId by remember { mutableStateOf<Long?>(null) }

    // Sheets
    var showTrackerEditor by remember { mutableStateOf(false) }
    var trackerToEdit by remember { mutableStateOf<TrackerItem?>(null) }
    var showSubscriptionEditor by remember { mutableStateOf(false) }
    var subscriptionToEdit by remember { mutableStateOf<SubscriptionItem?>(null) }
    var showTemplateCatalogSheet by remember { mutableStateOf(false) }

    // Filters
    var trackerFilter by remember { mutableStateOf("ALL") } // ALL, ACTIVE, LOW_STOCK
    var subscriptionFilter by remember { mutableStateOf("ALL") } // ALL, ACTIVE, TRIAL, PAUSED

    // Data flows
    val trackers by viewModel.allTrackers.collectAsStateWithLifecycle()
    val subscriptions by viewModel.allSubscriptions.collectAsStateWithLifecycle()
    val bills by viewModel.allBills.collectAsStateWithLifecycle()
    val projects by viewModel.allProjects.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremiumActive.collectAsStateWithLifecycle()

    // Handle back button when viewing detail
    if (viewingTrackerId != null) {
        BackHandler { viewingTrackerId = null }
        TrackerDetailScreen(
            trackerId = viewingTrackerId!!,
            viewModel = viewModel,
            onNavigateBack = { viewingTrackerId = null },
            onEditTracker = { t ->
                trackerToEdit = t
                showTrackerEditor = true
            }
        )
        if (showTrackerEditor) {
            TrackerEditorSheet(
                tracker = trackerToEdit,
                projects = projects,
                onSave = { item ->
                    viewModel.saveTracker(item)
                },
                onDismiss = { showTrackerEditor = false }
            )
        }
        return
    }

    if (viewingSubscriptionId != null) {
        BackHandler { viewingSubscriptionId = null }
        SubscriptionDetailScreen(
            subscriptionId = viewingSubscriptionId!!,
            viewModel = viewModel,
            onNavigateBack = { viewingSubscriptionId = null },
            onEditSubscription = { s ->
                subscriptionToEdit = s
                showSubscriptionEditor = true
            }
        )
        if (showSubscriptionEditor) {
            SubscriptionEditorSheet(
                subscription = subscriptionToEdit,
                projects = projects,
                onSave = { item ->
                    viewModel.saveSubscription(item)
                },
                onDismiss = { showSubscriptionEditor = false }
            )
        }
        return
    }

    // Filtered lists
    val filteredTrackers = remember(trackers, searchQuery, trackerFilter) {
        trackers.filter { t ->
            val matchesSearch = searchQuery.isBlank() ||
                    t.title.contains(searchQuery, ignoreCase = true) ||
                    t.description.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (trackerFilter) {
                "ACTIVE" -> !t.isArchived
                "LOW_STOCK" -> t.inventoryPolicy != InventoryPolicy.NONE && t.currentStock <= 0.0
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val filteredSubscriptions = remember(subscriptions, searchQuery, subscriptionFilter) {
        subscriptions.filter { s ->
            val matchesSearch = searchQuery.isBlank() ||
                    s.title.contains(searchQuery, ignoreCase = true) ||
                    s.description.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (subscriptionFilter) {
                "ACTIVE" -> s.status == SubscriptionStatus.ACTIVE
                "TRIAL" -> s.status == SubscriptionStatus.TRIAL
                "PAUSED" -> s.status == SubscriptionStatus.PAUSED
                else -> true
            }
            matchesSearch && matchesFilter
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
                            placeholder = { Text("Search essentials...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp)
                                .testTag("essentials_search_input"),
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
                            text = "Trackers & Subscriptions",
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
                        modifier = Modifier.testTag("essentials_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        }
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }

                    // Export Menu
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export")
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export CSV (Free)") },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                onClick = {
                                    showExportMenu = false
                                    if (selectedTabIndex == 0) {
                                        val csv = EssentialsExportHelper.generateTrackersCsv(trackers, emptyList())
                                        EssentialsExportHelper.shareCsvContent(context, "LifeOS_Trackers_${LocalDate.now()}.csv", csv)
                                    } else {
                                        val csv = EssentialsExportHelper.generateSubscriptionsCsv(subscriptions, bills)
                                        EssentialsExportHelper.shareCsvContent(context, "LifeOS_Subscriptions_${LocalDate.now()}.csv", csv)
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Export PDF (Report)")
                                        if (!isPremium) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFFFB300)
                                            ) {
                                                Text("PRO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                                onClick = {
                                    showExportMenu = false
                                    if (selectedTabIndex == 0) {
                                        EssentialsExportHelper.generateAndShareTrackersPdf(context, trackers, emptyList())
                                    } else {
                                        EssentialsExportHelper.generateAndShareSubscriptionsPdf(context, subscriptions, bills)
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) {
                        trackerToEdit = null
                        showTrackerEditor = true
                    } else {
                        subscriptionToEdit = null
                        showSubscriptionEditor = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_essential_fab")
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
            // Main Tab Row: Trackers vs Subscriptions
            SecondaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Smart Trackers (${trackers.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Subscriptions (${subscriptions.size})") }
                )
            }

            // Quick actions & Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (selectedTabIndex == 0) {
                        item {
                            FilterChip(
                                selected = trackerFilter == "ALL",
                                onClick = { trackerFilter = "ALL" },
                                label = { Text("All") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = trackerFilter == "ACTIVE",
                                onClick = { trackerFilter = "ACTIVE" },
                                label = { Text("Active") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = trackerFilter == "LOW_STOCK",
                                onClick = { trackerFilter = "LOW_STOCK" },
                                label = { Text("Low Stock") }
                            )
                        }
                    } else {
                        item {
                            FilterChip(
                                selected = subscriptionFilter == "ALL",
                                onClick = { subscriptionFilter = "ALL" },
                                label = { Text("All") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = subscriptionFilter == "ACTIVE",
                                onClick = { subscriptionFilter = "ACTIVE" },
                                label = { Text("Active") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = subscriptionFilter == "TRIAL",
                                onClick = { subscriptionFilter = "TRIAL" },
                                label = { Text("Trials") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = subscriptionFilter == "PAUSED",
                                onClick = { subscriptionFilter = "PAUSED" },
                                label = { Text("Paused") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = { showTemplateCatalogSheet = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Templates", fontSize = 12.sp)
                }
            }

            // List Content
            if (selectedTabIndex == 0) {
                // Smart Trackers Tab
                if (filteredTrackers.isEmpty()) {
                    EmptyEssentialsView(
                        title = if (searchQuery.isNotBlank()) "No Trackers Match '$searchQuery'" else "No Smart Trackers Yet",
                        subtitle = "Track mileage on running shoes, car service intervals, water filters, or gas cylinders with cycle tracking.",
                        actionText = "Create Tracker",
                        templateText = "Browse Templates",
                        onAction = {
                            trackerToEdit = null
                            showTrackerEditor = true
                        },
                        onBrowseTemplates = { showTemplateCatalogSheet = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredTrackers, key = { it.id }) { tracker ->
                            TrackerSummaryCard(
                                tracker = tracker,
                                onClick = { viewingTrackerId = tracker.id }
                            )
                        }
                    }
                }
            } else {
                // Subscriptions Tab
                if (filteredSubscriptions.isEmpty()) {
                    EmptyEssentialsView(
                        title = if (searchQuery.isNotBlank()) "No Subscriptions Match '$searchQuery'" else "No Subscriptions Tracked",
                        subtitle = "Never forget renewal dates, trial expirations, or annual commitments for Netflix, Spotify, iCloud, and memberships.",
                        actionText = "Add Subscription",
                        templateText = "Popular Services",
                        onAction = {
                            subscriptionToEdit = null
                            showSubscriptionEditor = true
                        },
                        onBrowseTemplates = { showTemplateCatalogSheet = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredSubscriptions, key = { it.id }) { sub ->
                            val pendingBill = bills.find { it.subscriptionId == sub.id && it.status != BillStatus.PAID }
                            SubscriptionSummaryCard(
                                subscription = sub,
                                pendingBill = pendingBill,
                                onClick = { viewingSubscriptionId = sub.id }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Bottom Sheet: Tracker Editor ---
    if (showTrackerEditor) {
        TrackerEditorSheet(
            tracker = trackerToEdit,
            projects = projects,
            onSave = { item ->
                viewModel.saveTracker(item)
            },
            onDismiss = { showTrackerEditor = false }
        )
    }

    // --- Bottom Sheet: Subscription Editor ---
    if (showSubscriptionEditor) {
        SubscriptionEditorSheet(
            subscription = subscriptionToEdit,
            projects = projects,
            onSave = { item ->
                viewModel.saveSubscription(item)
            },
            onDismiss = { showSubscriptionEditor = false }
        )
    }

    // --- Bottom Sheet: Template Catalog ---
    if (showTemplateCatalogSheet) {
        TemplateCatalogBottomSheet(
            selectedTabIndex = selectedTabIndex,
            onSelectTrackerTemplate = { template ->
                showTemplateCatalogSheet = false
                viewModel.saveTracker(template.toTrackerItem())
                Toast.makeText(context, "Added '${template.title}' from templates!", Toast.LENGTH_SHORT).show()
            },
            onSelectSubscriptionTemplate = { template ->
                showTemplateCatalogSheet = false
                viewModel.saveSubscription(template.toSubscriptionItem())
                Toast.makeText(context, "Added '${template.title}' subscription!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showTemplateCatalogSheet = false }
        )
    }
}

@Composable
private fun TrackerSummaryCard(
    tracker: TrackerItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLowStock = tracker.inventoryPolicy != InventoryPolicy.NONE && tracker.currentStock <= 0.0

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("tracker_card_${tracker.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = tracker.emoji, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = tracker.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (tracker.description.isNotBlank()) {
                            Text(
                                text = tracker.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (isLowStock) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Empty", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Measurement badges
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (tracker.measurements.trackDuration) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(
                            text = "⏱️ Duration",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (tracker.measurements.trackDistance) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(
                            text = "🛣️ ${tracker.distanceUnit.symbol}",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (tracker.inventoryPolicy != InventoryPolicy.NONE) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(
                            text = "📦 ${tracker.currentStock} ${tracker.quantityUnit.symbol}",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionSummaryCard(
    subscription: SubscriptionItem,
    pendingBill: BillingOccurrenceItem?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("subscription_card_${subscription.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = subscription.emoji, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = subscription.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${subscription.currency} ${String.format("%.2f", subscription.amount)} / ${subscription.frequency.displayName.lowercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (subscription.status) {
                        SubscriptionStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                        SubscriptionStatus.TRIAL -> Color(0xFFFFE082)
                        SubscriptionStatus.PAUSED -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        text = subscription.status.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (subscription.status) {
                            SubscriptionStatus.ACTIVE -> MaterialTheme.colorScheme.primary
                            SubscriptionStatus.TRIAL -> Color(0xFFE65100)
                            SubscriptionStatus.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Renewal: ${subscription.nextRenewalDateIso}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (pendingBill != null) {
                    Text(
                        text = "Due: ${pendingBill.currency} ${pendingBill.expectedAmount}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyEssentialsView(
    title: String,
    subtitle: String,
    actionText: String,
    templateText: String,
    onAction: () -> Unit,
    onBrowseTemplates: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAction) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(actionText)
                }
                OutlinedButton(onClick = onBrowseTemplates) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(templateText)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateCatalogBottomSheet(
    selectedTabIndex: Int,
    onSelectTrackerTemplate: (TrackerTemplate) -> Unit,
    onSelectSubscriptionTemplate: (SubscriptionTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = if (selectedTabIndex == 0) "Predefined Tracker Templates" else "Popular Subscription Templates",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap any template to add it immediately with recommended settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (selectedTabIndex == 0) {
                    items(EssentialsCatalog.trackerTemplates, key = { it.id }) { t ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTrackerTemplate(t) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = t.emoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = t.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = t.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                } else {
                    items(EssentialsCatalog.subscriptionTemplates, key = { it.id }) { s ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectSubscriptionTemplate(s) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Text(text = s.emoji, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = s.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = s.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text(
                                    text = "${s.currency} ${s.defaultAmount} / ${s.frequency.displayName.lowercase()}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
