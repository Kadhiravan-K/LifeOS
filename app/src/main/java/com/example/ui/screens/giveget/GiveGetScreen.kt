package com.example.ui.screens.giveget

import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import java.util.Locale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entities.GiveGetTransactionEntity
import com.example.ui.viewmodel.LifeViewModel
import java.time.LocalDate

enum class GiveGetTab(val label: String, val icon: String) {
    ALL("All", "📋"),
    GIVEN("Given / Lent", "📤"),
    BORROWED("Borrowed", "📥"),
    PEOPLE("Balances", "👥")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiveGetScreen(
    viewModel: LifeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(GiveGetTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf<String?>(null) } // null = all

    val allTransactions by viewModel.allGiveGetTransactions.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<GiveGetTransactionEntity?>(null) }
    var repaymentTargetTransaction by remember { mutableStateOf<GiveGetTransactionEntity?>(null) }

    val filteredTransactions = remember(allTransactions, selectedTab, searchQuery, filterStatus) {
        allTransactions.filter { tx ->
            val matchesTab = when (selectedTab) {
                GiveGetTab.ALL -> true
                GiveGetTab.GIVEN -> tx.isLent || tx.isGift
                GiveGetTab.BORROWED -> tx.isBorrowed
                GiveGetTab.PEOPLE -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    tx.personName.contains(searchQuery, ignoreCase = true) ||
                    tx.itemDescription.contains(searchQuery, ignoreCase = true) ||
                    tx.notes.contains(searchQuery, ignoreCase = true)
            val matchesStatus = filterStatus == null || tx.status == filterStatus
            matchesTab && matchesSearch && matchesStatus
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Give & Get",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Contact-based item and debt tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("give_get_back")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTransaction = null
                    showAddDialog = true
                },
                modifier = Modifier.testTag("add_give_get_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Transaction")
            }
        },
        modifier = modifier.fillMaxSize().testTag("give_get_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                GiveGetTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tab.icon, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(tab.label, fontSize = 12.sp, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal)
                            }
                        },
                        modifier = Modifier.testTag("give_get_tab_${tab.name.lowercase()}")
                    )
                }
            }

            // Search Bar & Filter Chips
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by person, item, or notes...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("give_get_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filterStatus == null,
                        onClick = { filterStatus = null },
                        label = { Text("All Statuses", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterStatus == "PENDING",
                        onClick = { filterStatus = if (filterStatus == "PENDING") null else "PENDING" },
                        label = { Text("Pending", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterStatus == "PARTIALLY_RETURNED",
                        onClick = { filterStatus = if (filterStatus == "PARTIALLY_RETURNED") null else "PARTIALLY_RETURNED" },
                        label = { Text("Partial", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterStatus == "RETURNED",
                        onClick = { filterStatus = if (filterStatus == "RETURNED") null else "RETURNED" },
                        label = { Text("Returned", fontSize = 11.sp) }
                    )
                }
            }

            // Content
            if (selectedTab == GiveGetTab.PEOPLE) {
                PersonBalancesView(
                    transactions = allTransactions,
                    onPersonClick = { person ->
                        searchQuery = person
                        selectedTab = GiveGetTab.ALL
                    }
                )
            } else {
                TransactionListView(
                    transactions = filteredTransactions,
                    onMarkReturned = { tx -> viewModel.markGiveGetReturned(tx.id) },
                    onRecordRepayment = { tx -> repaymentTargetTransaction = tx },
                    onEdit = { tx ->
                        editingTransaction = tx
                        showAddDialog = true
                    },
                    onDelete = { tx -> viewModel.deleteGiveGetTransaction(tx) }
                )
            }
        }
    }

    // Add / Edit Transaction Dialog
    if (showAddDialog) {
        GiveGetEditDialog(
            initialTransaction = editingTransaction,
            onDismiss = {
                showAddDialog = false
                editingTransaction = null
            },
            onSave = { tx ->
                viewModel.saveGiveGetTransaction(tx)
                showAddDialog = false
                editingTransaction = null
            }
        )
    }

    // Repayment Record Dialog
    if (repaymentTargetTransaction != null) {
        val tx = repaymentTargetTransaction!!
        GiveGetRepaymentDialog(
            transaction = tx,
            onDismiss = { repaymentTargetTransaction = null },
            onConfirmRepayment = { amount ->
                viewModel.recordGiveGetRepayment(tx.id, amount)
                repaymentTargetTransaction = null
            }
        )
    }
}

// =============================================================================
// LIST OF TRANSACTIONS
// =============================================================================
@Composable
private fun TransactionListView(
    transactions: List<GiveGetTransactionEntity>,
    onMarkReturned: (GiveGetTransactionEntity) -> Unit,
    onRecordRepayment: (GiveGetTransactionEntity) -> Unit,
    onEdit: (GiveGetTransactionEntity) -> Unit,
    onDelete: (GiveGetTransactionEntity) -> Unit
) {
    if (transactions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🤝", fontSize = 36.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No records found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap + to track money lent, borrowed items, or gifts given to friends and colleagues.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(transactions, key = { it.id }) { tx ->
                TransactionCard(
                    tx = tx,
                    onMarkReturned = { onMarkReturned(tx) },
                    onRecordRepayment = { onRecordRepayment(tx) },
                    onEdit = { onEdit(tx) },
                    onDelete = { onDelete(tx) }
                )
            }
        }
    }
}

@Composable
private fun TransactionCard(
    tx: GiveGetTransactionEntity,
    onMarkReturned: () -> Unit,
    onRecordRepayment: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isOverdue = remember(tx) {
        tx.status != "RETURNED" && tx.status != "GIFTED" &&
                !tx.expectedReturnDateIso.isNullOrBlank() &&
                tx.expectedReturnDateIso < LocalDate.now().toString()
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("give_get_item_${tx.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeColor = when {
                        tx.isGift -> Color(0xFF10B981)
                        tx.isLent -> MaterialTheme.colorScheme.primary
                        else -> Color(0xFFF59E0B)
                    }
                    val badgeIcon = when (tx.type) {
                        "GIVE_LENT_MONEY" -> "💸 Lent Money"
                        "GIVE_LENT_ITEM" -> "📦 Lent Item"
                        "GIVE_GIFT_ITEM" -> "🎁 Gift Item"
                        "GIVE_GIFT_MONEY" -> "🎁 Gift Money"
                        "GET_BORROWED_MONEY" -> "💰 Borrowed Money"
                        "GET_BORROWED_ITEM" -> "📥 Borrowed Item"
                        else -> "📋 Other"
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badgeIcon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        isOverdue -> MaterialTheme.colorScheme.errorContainer
                        tx.status == "RETURNED" -> Color(0xFFD1FAE5)
                        tx.status == "PARTIALLY_RETURNED" -> Color(0xFFFEF3C7)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = if (isOverdue) "⚠️ OVERDUE" else tx.status.replace("_", " "),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isOverdue -> MaterialTheme.colorScheme.onErrorContainer
                            tx.status == "RETURNED" -> Color(0xFF065F46)
                            tx.status == "PARTIALLY_RETURNED" -> Color(0xFF92400E)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tx.itemDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (tx.isMoney) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", tx.amount)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (tx.isLent) MaterialTheme.colorScheme.primary else Color(0xFFD97706)
                        )
                        if (tx.returnedAmount > 0.0 && tx.status != "RETURNED") {
                            Text(
                                text = "Paid: $${String.format(Locale.US, "%.2f", tx.returnedAmount)} • Rem: $${String.format(Locale.US, "%.2f", tx.remainingAmount)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Date: ${tx.dateIso}" + (if (!tx.expectedReturnDateIso.isNullOrBlank()) " • Due: ${tx.expectedReturnDateIso}" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (tx.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Note: ${tx.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (tx.status != "RETURNED" && !tx.isGift) {
                    if (tx.isMoney) {
                        OutlinedButton(
                            onClick = onRecordRepayment,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Record Repayment", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Button(
                        onClick = onMarkReturned,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(if (tx.isMoney) "Mark Fully Paid" else "Mark Returned", fontSize = 11.sp)
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// =============================================================================
// TAB: PERSON BALANCES
// =============================================================================
@Composable
private fun PersonBalancesView(
    transactions: List<GiveGetTransactionEntity>,
    onPersonClick: (String) -> Unit
) {
    // Strictly isolate debts from gifts!
    val personGroups = remember(transactions) {
        val map = mutableMapOf<String, Pair<Double, Double>>() // Person -> (They Owe You, You Owe Them)
        transactions.forEach { tx ->
            if (tx.isMoney && !tx.isGift && tx.status != "RETURNED") {
                val current = map.getOrDefault(tx.personName, Pair(0.0, 0.0))
                if (tx.isLent) {
                    map[tx.personName] = Pair(current.first + tx.remainingAmount, current.second)
                } else if (tx.isBorrowed) {
                    map[tx.personName] = Pair(current.first, current.second + tx.remainingAmount)
                }
            }
        }
        map.toList()
    }

    if (personGroups.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("👥", fontSize = 36.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No outstanding balances",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "All debts are settled. Gifts are excluded from financial balances.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(personGroups) { (person, balances) ->
                val (theyOwe, youOwe) = balances
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().clickable { onPersonClick(person) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = person,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (theyOwe > 0.0) {
                                Text(
                                    text = "Owes you: $${String.format(Locale.US, "%.2f", theyOwe)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (youOwe > 0.0) {
                                Text(
                                    text = "You owe: $${String.format(Locale.US, "%.2f", youOwe)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "View Details", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// =============================================================================
// DIALOG: ADD / EDIT TRANSACTION
// =============================================================================
@Composable
private fun GiveGetEditDialog(
    initialTransaction: GiveGetTransactionEntity?,
    onDismiss: () -> Unit,
    onSave: (GiveGetTransactionEntity) -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(initialTransaction?.type ?: "GIVE_LENT_MONEY") }
    var personName by remember { mutableStateOf(initialTransaction?.personName ?: "") }
    var amountText by remember { mutableStateOf(if ((initialTransaction?.amount ?: 0.0) > 0.0) initialTransaction!!.amount.toString() else "") }
    var itemDescription by remember { mutableStateOf(initialTransaction?.itemDescription ?: "") }
    var dateIso by remember { mutableStateOf(initialTransaction?.dateIso ?: LocalDate.now().toString()) }
    var expectedReturnDateIso by remember { mutableStateOf(initialTransaction?.expectedReturnDateIso ?: "") }
    var notes by remember { mutableStateOf(initialTransaction?.notes ?: "") }
    var hasReminder by remember { mutableStateOf(initialTransaction?.hasReminder ?: false) }

    // Contact Picker (optional - uses standard Android PickContact contract)
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri ->
        if (contactUri != null) {
            try {
                val cursor = context.contentResolver.query(contactUri, arrayOf(ContactsContract.Contacts.DISPLAY_NAME), null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                        if (!name.isNullOrBlank()) {
                            personName = name
                        }
                    }
                }
            } catch (_: Exception) {
                // Fallback gracefully
            }
        }
    }

    val typeOptions = listOf(
        "GIVE_LENT_MONEY" to "💸 Lent Money",
        "GIVE_LENT_ITEM" to "📦 Lent Item",
        "GIVE_GIFT_ITEM" to "🎁 Gift Item",
        "GIVE_GIFT_MONEY" to "🎁 Gift Money",
        "GET_BORROWED_MONEY" to "💰 Borrowed Money",
        "GET_BORROWED_ITEM" to "📥 Borrowed Item"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTransaction == null) "New Give & Get Record" else "Edit Record") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Transaction Type", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(typeOptions) { (key, label) ->
                        FilterChip(
                            selected = type == key,
                            onClick = { type = key },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = personName,
                        onValueChange = { personName = it },
                        label = { Text("Person Name") },
                        modifier = Modifier.weight(1f).testTag("give_get_person_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            try {
                                contactPickerLauncher.launch(null)
                            } catch (_: Exception) {
                                // gracefully ignore if no contact app
                            }
                        },
                        modifier = Modifier.testTag("pick_contact_button")
                    ) {
                        Icon(Icons.Default.Contacts, contentDescription = "Pick Contact", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                if (type.contains("MONEY")) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("give_get_amount_input"),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = itemDescription,
                    onValueChange = { itemDescription = it },
                    label = { Text(if (type.contains("MONEY")) "Reason / Description" else "Item Description (e.g. Lawn Mower, Book)") },
                    modifier = Modifier.fillMaxWidth().testTag("give_get_desc_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dateIso,
                        onValueChange = { dateIso = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    if (!type.contains("GIFT")) {
                        OutlinedTextField(
                            value = expectedReturnDateIso,
                            onValueChange = { expectedReturnDateIso = it },
                            label = { Text("Expected Return") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                if (!type.contains("GIFT")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Return Reminder", fontSize = 13.sp)
                        Switch(
                            checked = hasReminder,
                            onCheckedChange = { hasReminder = it }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (personName.isNotBlank()) {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        val initialStatus = if (type.contains("GIFT")) "GIFTED" else "PENDING"
                        val tx = (initialTransaction ?: GiveGetTransactionEntity(
                            profileId = 1L,
                            type = type,
                            personName = personName.trim(),
                            dateIso = dateIso.trim()
                        )).copy(
                            type = type,
                            personName = personName.trim(),
                            amount = amount,
                            itemDescription = itemDescription.trim(),
                            dateIso = dateIso.trim(),
                            expectedReturnDateIso = if (type.contains("GIFT")) null else expectedReturnDateIso.trim().ifBlank { null },
                            hasReminder = hasReminder,
                            notes = notes.trim(),
                            status = if (initialTransaction != null) initialTransaction.status else initialStatus,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(tx)
                    }
                },
                modifier = Modifier.testTag("save_give_get_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// =============================================================================
// DIALOG: RECORD REPAYMENT
// =============================================================================
@Composable
private fun GiveGetRepaymentDialog(
    transaction: GiveGetTransactionEntity,
    onDismiss: () -> Unit,
    onConfirmRepayment: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf(transaction.remainingAmount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Repayment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Person: ${transaction.personName}")
                Text("Total Debt: $${String.format(Locale.US, "%.2f", transaction.amount)}")
                Text("Already Repaid: $${String.format(Locale.US, "%.2f", transaction.returnedAmount)}")
                Text("Remaining: $${String.format(Locale.US, "%.2f", transaction.remainingAmount)}", fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Repayment Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("repayment_amount_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0.0) {
                        onConfirmRepayment(amount)
                    }
                },
                modifier = Modifier.testTag("confirm_repayment_button")
            ) {
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
