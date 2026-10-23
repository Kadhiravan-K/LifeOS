package com.example.ui.screens.essentials

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.tracker.CycleStatus
import com.example.domain.tracker.InventoryAdjustmentItem
import com.example.domain.tracker.InventoryPolicy
import com.example.domain.tracker.TrackerCalculationEngine
import com.example.domain.tracker.TrackerItem
import com.example.domain.tracker.TrackerPurchaseItem
import com.example.domain.tracker.UsageCycleItem
import com.example.domain.tracker.UsageEntryItem
import com.example.ui.viewmodel.LifeViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerDetailScreen(
    trackerId: Long,
    viewModel: LifeViewModel,
    onNavigateBack: () -> Unit,
    onEditTracker: (TrackerItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackers by viewModel.allTrackers.collectAsStateWithLifecycle()
    val tracker = trackers.find { it.id == trackerId }
    val projects by viewModel.allProjects.collectAsStateWithLifecycle()
    val project = projects.find { it.id == tracker?.projectId }

    val cyclesFlow = remember(trackerId) { viewModel.getCyclesForTracker(trackerId) }
    val cycles by cyclesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val purchasesFlow = remember(trackerId) { viewModel.getPurchasesForTracker(trackerId) }
    val purchases by purchasesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val adjustmentsFlow = remember(trackerId) { viewModel.getAdjustmentsForTracker(trackerId) }
    val adjustments by adjustmentsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Active Cycles, 1: Inventory & History, 2: Analytics

    // Dialog states
    var showStartCycleDialog by remember { mutableStateOf(false) }
    var cycleToLogEntry by remember { mutableStateOf<UsageCycleItem?>(null) }
    var cycleToFinish by remember { mutableStateOf<UsageCycleItem?>(null) }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var showAdjustmentDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (tracker == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Tracker not found")
        }
        return
    }

    val activeCycles = cycles.filter { it.status == CycleStatus.ACTIVE || it.status == CycleStatus.PAUSED }
    val finishedCycles = cycles.filter { it.status == CycleStatus.FINISHED }
    val isLowStock = tracker.inventoryPolicy != InventoryPolicy.NONE && tracker.currentStock <= 0.0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = tracker.emoji, fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tracker.title,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditTracker(tracker) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Tracker")
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Tracker", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = { showStartCycleDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("start_cycle_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Start New Cycle")
                }
            } else if (selectedTabIndex == 1 && tracker.inventoryPolicy != InventoryPolicy.NONE) {
                FloatingActionButton(
                    onClick = { showPurchaseDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_purchase_fab")
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Restock Spares")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Info & Status
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (tracker.description.isNotBlank()) {
                    Text(
                        text = tracker.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (project != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "${project.emoji} ${project.name}",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = tracker.inventoryPolicy.displayName,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (tracker.inventoryPolicy != InventoryPolicy.NONE) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isLowStock) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Stock: ${tracker.currentStock} ${tracker.quantityUnit.symbol}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Tabs
            SecondaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Active Cycles (${activeCycles.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Inventory & History") }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Analytics") }
                )
            }

            when (selectedTabIndex) {
                0 -> {
                    // Active Cycles Tab
                    if (activeCycles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No Active Cycles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Tap '+' below to start a new tracking cycle.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { showStartCycleDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Start Cycle")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(activeCycles, key = { it.id }) { cycle ->
                                ActiveCycleCard(
                                    cycle = cycle,
                                    tracker = tracker,
                                    onLogEntry = { cycleToLogEntry = cycle },
                                    onTogglePause = {
                                        viewModel.pauseResumeCycle(cycle, !cycle.isPaused)
                                    },
                                    onFinish = { cycleToFinish = cycle }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // Inventory & History Tab
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (tracker.inventoryPolicy != InventoryPolicy.NONE) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Physical Inventory", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                Text(
                                                    text = "Stock: ${tracker.currentStock} ${tracker.quantityUnit.symbol}" +
                                                            if (tracker.inventoryPolicy == InventoryPolicy.REFILLABLE_EXCHANGEABLE) " (Empty: ${tracker.emptyStock})" else "",
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(onClick = { showAdjustmentDialog = true }) {
                                                    Text("Adjust", fontSize = 12.sp)
                                                }
                                                Button(onClick = { showPurchaseDialog = true }) {
                                                    Text("Restock", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Completed Cycles
                        item {
                            Text("Completed Cycles (${finishedCycles.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        if (finishedCycles.isEmpty()) {
                            item {
                                Text("No finished cycles yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(finishedCycles, key = { it.id }) { cycle ->
                                FinishedCycleCard(cycle = cycle, tracker = tracker)
                            }
                        }

                        // Purchases
                        if (purchases.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Restock & Purchase Logs (${purchases.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            items(purchases, key = { it.id }) { p ->
                                PurchaseRowCard(purchase = p, unit = tracker.quantityUnit.symbol)
                            }
                        }
                    }
                }

                2 -> {
                    // Analytics Tab
                    AnalyticsView(tracker = tracker, cycles = cycles, purchases = purchases)
                }
            }
        }
    }

    // --- Dialog: Start Cycle ---
    if (showStartCycleDialog) {
        var cycleName by remember { mutableStateOf("") }
        var startOdometerText by remember { mutableStateOf("") }
        var initialQuantityText by remember { mutableStateOf(tracker.currentStock.toString()) }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showStartCycleDialog = false },
            title = { Text("Start Tracking Cycle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = cycleName,
                        onValueChange = { cycleName = it },
                        label = { Text("Cycle Name / Label") },
                        placeholder = { Text("e.g. Michelin Front Tyre, Batch #4") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (tracker.measurements.trackDistance) {
                        OutlinedTextField(
                            value = startOdometerText,
                            onValueChange = { startOdometerText = it },
                            label = { Text("Start Odometer (${tracker.distanceUnit.symbol})") },
                            placeholder = { Text("e.g. 42500") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (tracker.measurements.trackQuantity) {
                        OutlinedTextField(
                            value = initialQuantityText,
                            onValueChange = { initialQuantityText = it },
                            label = { Text("Initial Quantity (${tracker.quantityUnit.symbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedOdo = startOdometerText.toDoubleOrNull() ?: 0.0
                        val parsedQty = initialQuantityText.toDoubleOrNull() ?: 0.0
                        val cycle = UsageCycleItem(
                            trackerId = tracker.id,
                            cycleName = cycleName.trim().ifBlank { "Cycle #${cycles.size + 1}" },
                            startDateIso = LocalDate.now().toString(),
                            startOdometer = parsedOdo,
                            currentOdometer = parsedOdo,
                            initialQuantity = parsedQty,
                            remainingQuantity = parsedQty,
                            notes = notes.trim()
                        )
                        viewModel.saveCycle(cycle)
                        showStartCycleDialog = false
                    }
                ) {
                    Text("Start Cycle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartCycleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Dialog: Log Entry ---
    cycleToLogEntry?.let { cycle ->
        var currentOdometerText by remember { mutableStateOf(cycle.currentOdometer.toString()) }
        var quantityDeductedText by remember { mutableStateOf("") }
        var entryNotes by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { cycleToLogEntry = null },
            title = { Text("Log Entry — ${cycle.cycleName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (tracker.measurements.trackDistance) {
                        OutlinedTextField(
                            value = currentOdometerText,
                            onValueChange = { currentOdometerText = it },
                            label = { Text("Current Odometer (${tracker.distanceUnit.symbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (tracker.measurements.trackQuantity) {
                        OutlinedTextField(
                            value = quantityDeductedText,
                            onValueChange = { quantityDeductedText = it },
                            label = { Text("Quantity Consumed (${tracker.quantityUnit.symbol})") },
                            placeholder = { Text("e.g. 1") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = entryNotes,
                        onValueChange = { entryNotes = it },
                        label = { Text("Entry Note (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    errorMessage?.let { err ->
                        Text(text = err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newOdo = currentOdometerText.toDoubleOrNull()
                        val deltaQty = quantityDeductedText.toDoubleOrNull()

                        if (newOdo != null && newOdo < cycle.currentOdometer) {
                            errorMessage = "Odometer cannot regress (${newOdo} < ${cycle.currentOdometer})!"
                            return@Button
                        }

                        val entry = UsageEntryItem(
                            cycleId = cycle.id,
                            dateIso = LocalDate.now().toString(),
                            odometerReading = newOdo,
                            quantityDeducted = deltaQty,
                            notes = entryNotes.trim()
                        )
                        viewModel.logUsageEntry(entry, cycle)
                        cycleToLogEntry = null
                    }
                ) {
                    Text("Save Entry")
                }
            },
            dismissButton = {
                TextButton(onClick = { cycleToLogEntry = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Dialog: Finish Cycle Confirmation ---
    cycleToFinish?.let { cycle ->
        var finalOdometerText by remember { mutableStateOf(cycle.currentOdometer.toString()) }
        var finalRemainingQuantityText by remember { mutableStateOf(cycle.remainingQuantity.toString()) }
        var finishNotes by remember { mutableStateOf("") }

        val elapsedDays = TrackerCalculationEngine.calculateElapsedDays(
            startDateIso = cycle.startDateIso,
            endDateIso = LocalDate.now().toString()
        )

        AlertDialog(
            onDismissRequest = { cycleToFinish = null },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Finish Cycle?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Finishing records final metrics and completes this cycle.")
                    Text("• Calendar days elapsed: $elapsedDays days")
                    if (tracker.measurements.trackDistance) {
                        Text("• Total distance: ${cycle.totalDistance} ${tracker.distanceUnit.symbol}")
                        OutlinedTextField(
                            value = finalOdometerText,
                            onValueChange = { finalOdometerText = it },
                            label = { Text("Final Odometer (${tracker.distanceUnit.symbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (tracker.measurements.trackQuantity) {
                        OutlinedTextField(
                            value = finalRemainingQuantityText,
                            onValueChange = { finalRemainingQuantityText = it },
                            label = { Text("Final Remaining Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = finishNotes,
                        onValueChange = { finishNotes = it },
                        label = { Text("Completion Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalOdo = finalOdometerText.toDoubleOrNull() ?: cycle.currentOdometer
                        val finalQty = finalRemainingQuantityText.toDoubleOrNull() ?: cycle.remainingQuantity
                        viewModel.finishCycle(
                            cycle = cycle,
                            finishDateIso = LocalDate.now().toString(),
                            finishOdometer = finalOdo,
                            finalRemainingQuantity = finalQty,
                            notes = finishNotes.trim()
                        )
                        cycleToFinish = null
                    }
                ) {
                    Text("Finish Cycle")
                }
            },
            dismissButton = {
                TextButton(onClick = { cycleToFinish = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Dialog: Restock / Buy Spares ---
    if (showPurchaseDialog) {
        var quantityText by remember { mutableStateOf("1") }
        var unitPriceText by remember { mutableStateOf("") }
        var notesText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPurchaseDialog = false },
            title = { Text("Restock Spares / Purchase") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Quantity Restocked (${tracker.quantityUnit.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = unitPriceText,
                        onValueChange = { unitPriceText = it },
                        label = { Text("Unit Price (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Notes (Store, Brand, Invoice)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = quantityText.toDoubleOrNull() ?: 1.0
                        val price = unitPriceText.toDoubleOrNull() ?: 0.0
                        val total = qty * price
                        val purchase = TrackerPurchaseItem(
                            trackerId = tracker.id,
                            purchaseDateIso = LocalDate.now().toString(),
                            quantity = qty,
                            unitPrice = price,
                            totalCost = total,
                            notes = notesText.trim()
                        )
                        viewModel.recordPurchase(purchase)
                        showPurchaseDialog = false
                    }
                ) {
                    Text("Record Restock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurchaseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Dialog: Stock Adjustment ---
    if (showAdjustmentDialog) {
        var deltaText by remember { mutableStateOf("1") }
        var isRefillReturn by remember { mutableStateOf(false) }
        var reasonText by remember { mutableStateOf("Physical stock audit") }

        AlertDialog(
            onDismissRequest = { showAdjustmentDialog = false },
            title = { Text("Adjust Stock") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = deltaText,
                        onValueChange = { deltaText = it },
                        label = { Text("Adjustment Quantity (+ or -)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (tracker.inventoryPolicy == InventoryPolicy.REFILLABLE_EXCHANGEABLE) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isRefillReturn = !isRefillReturn }
                        ) {
                            Text("Container exchange/return (swaps full/empty)", modifier = Modifier.weight(1f), fontSize = 13.sp)
                            androidx.compose.material3.Checkbox(checked = isRefillReturn, onCheckedChange = { isRefillReturn = it })
                        }
                    }
                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        label = { Text("Reason for adjustment") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val delta = deltaText.toDoubleOrNull() ?: 0.0
                        val adj = InventoryAdjustmentItem(
                            trackerId = tracker.id,
                            dateIso = LocalDate.now().toString(),
                            deltaQuantity = delta,
                            isRefillExchange = isRefillReturn,
                            reason = reasonText.trim()
                        )
                        viewModel.recordStockAdjustment(adj)
                        showAdjustmentDialog = false
                    }
                ) {
                    Text("Save Adjustment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustmentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Dialog: Delete Tracker Confirmation ---
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Tracker '${tracker.title}'?") },
            text = {
                Text("Are you sure? This will permanently delete this tracker, all cycle logs, entries, and purchase history.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTracker(tracker.id)
                        showDeleteConfirmDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ActiveCycleCard(
    cycle: UsageCycleItem,
    tracker: TrackerItem,
    onLogEntry: () -> Unit,
    onTogglePause: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPaused = cycle.isPaused || cycle.status == CycleStatus.PAUSED
    val elapsedDays = TrackerCalculationEngine.calculateElapsedDays(
        startDateIso = cycle.startDateIso,
        endDateIso = LocalDate.now().toString()
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = cycle.cycleName.ifBlank { "Active Cycle" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Started: ${cycle.startDateIso}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPaused) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (isPaused) "PAUSED" else "ACTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Duration Metric
            if (tracker.measurements.trackDuration) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Duration", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "$elapsedDays days elapsed",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Distance Metric
            if (tracker.measurements.trackDistance) {
                val currentDist = cycle.totalDistance
                val unit = tracker.distanceUnit.symbol
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Distance Mileage", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "$currentDist $unit (Odo: ${cycle.currentOdometer})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Quantity Metric
            if (tracker.measurements.trackQuantity) {
                val currentQty = cycle.remainingQuantity
                val unit = tracker.quantityUnit.symbol
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Quantity Remaining", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "$currentQty $unit",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onLogEntry,
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Entry", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onTogglePause,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isPaused) "Resume" else "Pause", fontSize = 12.sp)
                }

                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Finish", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun FinishedCycleCard(cycle: UsageCycleItem, tracker: TrackerItem) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(cycle.cycleName.ifBlank { "Completed Cycle" }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Finished: ${cycle.finishDateIso ?: "Completed"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            val details = buildList {
                if (tracker.measurements.trackDistance) add("${cycle.totalDistance} ${tracker.distanceUnit.symbol}")
                if (tracker.measurements.trackDuration && cycle.finishDateIso != null) {
                    val days = TrackerCalculationEngine.calculateElapsedDays(cycle.startDateIso, cycle.finishDateIso)
                    add("$days days")
                }
                if (tracker.measurements.trackQuantity) add("Consumed: ${cycle.quantityConsumed} ${tracker.quantityUnit.symbol}")
            }.joinToString(" • ")
            Text(
                text = details,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PurchaseRowCard(purchase: TrackerPurchaseItem, unit: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("+${purchase.quantity} $unit restocked", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    text = purchase.purchaseDateIso + if (purchase.notes.isNotBlank()) " • " + purchase.notes else "",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (purchase.totalCost > 0) {
                Text(
                    text = String.format("₹%.2f", purchase.totalCost),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AnalyticsView(
    tracker: TrackerItem,
    cycles: List<UsageCycleItem>,
    purchases: List<TrackerPurchaseItem>
) {
    val totalPurchasesCost = purchases.sumOf { it.totalCost }
    val totalDistance = cycles.sumOf { it.totalDistance }
    val finishedCycles = cycles.filter { it.status == CycleStatus.FINISHED }
    val avgDuration = TrackerCalculationEngine.calculateAverageCompletedDuration(cycles)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Tracker Performance Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalyticsStatCard(
                    title = "Total Cycles",
                    value = cycles.size.toString(),
                    subtitle = "${finishedCycles.size} completed",
                    modifier = Modifier.weight(1f)
                )
                AnalyticsStatCard(
                    title = "Average Duration",
                    value = "${avgDuration}d",
                    subtitle = "per finished cycle",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalyticsStatCard(
                    title = "Parts & Spares Spent",
                    value = String.format("₹%.2f", totalPurchasesCost),
                    subtitle = "${purchases.size} restocks",
                    modifier = Modifier.weight(1f)
                )
                AnalyticsStatCard(
                    title = "Current Stock",
                    value = "${tracker.currentStock} ${tracker.quantityUnit.symbol}",
                    subtitle = tracker.inventoryPolicy.displayName,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (tracker.measurements.trackDistance) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "Total Distance Across Cycles", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format("%.1f", totalDistance)} ${tracker.distanceUnit.symbol}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsStatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
