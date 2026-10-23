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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.subscription.BillingOccurrenceItem
import com.example.domain.subscription.BillStatus
import com.example.domain.subscription.PriceHistoryItem
import com.example.domain.subscription.SubscriptionItem
import com.example.domain.subscription.SubscriptionPaymentItem
import com.example.domain.subscription.SubscriptionScheduleEngine
import com.example.domain.subscription.SubscriptionStatus
import com.example.ui.viewmodel.LifeViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionDetailScreen(
    subscriptionId: Long,
    viewModel: LifeViewModel,
    onNavigateBack: () -> Unit,
    onEditSubscription: (SubscriptionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val subscriptions by viewModel.allSubscriptions.collectAsStateWithLifecycle()
    val subscription = subscriptions.find { it.id == subscriptionId }
    val projects by viewModel.allProjects.collectAsStateWithLifecycle()
    val project = projects.find { it.id == subscription?.projectId }

    val billsFlow = remember(subscriptionId) { viewModel.getBillsForSubscription(subscriptionId) }
    val bills by billsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val paymentsFlow = remember(subscriptionId) { viewModel.getPaymentsForSubscription(subscriptionId) }
    val payments by paymentsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val priceHistoryFlow = remember(subscriptionId) { viewModel.getPriceHistory(subscriptionId) }
    val priceHistory by priceHistoryFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Bills & Payments, 1: Price History & Analytics

    // Dialog states
    var billToPay by remember { mutableStateOf<BillingOccurrenceItem?>(null) }
    var showPriceChangeDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (subscription == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Subscription not found")
        }
        return
    }

    val pendingBill = bills.find { it.status == BillStatus.UNPAID || it.status == BillStatus.PARTIALLY_PAID || it.status == BillStatus.OVERDUE }
    val paidBills = bills.filter { it.status == BillStatus.PAID }

    val annualCommitment = SubscriptionScheduleEngine.calculateYearlyEquivalent(
        amount = subscription.amount,
        frequency = subscription.frequency,
        customIntervalCount = subscription.customIntervalCount,
        customIntervalUnit = subscription.customIntervalUnit
    )
    val monthlyEquivalent = SubscriptionScheduleEngine.calculateMonthlyEquivalent(
        amount = subscription.amount,
        frequency = subscription.frequency,
        customIntervalCount = subscription.customIntervalCount,
        customIntervalUnit = subscription.customIntervalUnit
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = subscription.emoji, fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = subscription.title,
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
                    IconButton(onClick = { onEditSubscription(subscription) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Subscription")
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header summary card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${subscription.currency} ${String.format("%.2f", subscription.amount)} / ${subscription.frequency.displayName.lowercase()}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (subscription.description.isNotBlank()) {
                                Text(
                                    text = subscription.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (subscription.status) {
                                SubscriptionStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                                SubscriptionStatus.TRIAL -> Color(0xFFFFE082)
                                SubscriptionStatus.PAUSED -> MaterialTheme.colorScheme.surfaceVariant
                                SubscriptionStatus.CANCELLED, SubscriptionStatus.ARCHIVED -> MaterialTheme.colorScheme.errorContainer
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
                                    SubscriptionStatus.CANCELLED, SubscriptionStatus.ARCHIVED -> MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Next Renewal: ${subscription.nextRenewalDateIso}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "Anchor: Day ${subscription.anchorDayOfMonth}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (project != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "${project.emoji} ${project.name}",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Tabs: Bills & Payments vs Analytics
            SecondaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Bills & Payments") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Price History & Projections") }
                )
            }

            when (selectedTabIndex) {
                0 -> {
                    // Bills & Payments Tab
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Current Pending Bill Card
                        item {
                            Text("Current Due", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        if (pendingBill == null) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("No bills currently due.", fontWeight = FontWeight.SemiBold)
                                        Text("Next bill will automatically appear based on your schedule.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        } else {
                            item {
                                val billPayments = payments.filter { it.billId == pendingBill.id }
                                val paidSoFar = billPayments.sumOf { it.amountPaid }
                                val balanceDue = (pendingBill.expectedAmount - paidSoFar).coerceAtLeast(0.0)

                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (pendingBill.status == BillStatus.OVERDUE)
                                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Due: ${pendingBill.dueDateIso}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                                Text(
                                                    text = "Amount: ${pendingBill.currency} ${String.format("%.2f", pendingBill.expectedAmount)}",
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (paidSoFar > 0) {
                                                    Text(
                                                        text = "Paid so far: ${pendingBill.currency} ${String.format("%.2f", paidSoFar)} (Balance: ${pendingBill.currency} ${String.format("%.2f", balanceDue)})",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }

                                            Button(
                                                onClick = { billToPay = pendingBill },
                                                modifier = Modifier.testTag("pay_bill_btn")
                                            ) {
                                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Pay")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Paid Bills History
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Payment History (${paidBills.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        if (paidBills.isEmpty()) {
                            item {
                                Text("No completed bill cycles yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(paidBills, key = { it.id }) { bill ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Bill Date: ${bill.dueDateIso}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(
                                                text = "Status: Paid in full",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${bill.currency} ${String.format("%.2f", bill.expectedAmount)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Price History & Analytics Tab
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Price History", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                OutlinedButton(onClick = { showPriceChangeDialog = true }) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Record Price Change", fontSize = 12.sp)
                                }
                            }
                        }

                        if (priceHistory.isEmpty()) {
                            item {
                                Text("No recorded price changes. Current plan price: ${subscription.currency} ${subscription.amount}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(priceHistory, key = { it.id }) { ph ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${subscription.currency} ${ph.oldAmount} ➔ ${subscription.currency} ${ph.newAmount}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            if (ph.reason.isNotBlank()) {
                                                Text(text = ph.reason, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Text(text = ph.effectiveDateIso, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Cost Projections & Analytics", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("Monthly Cost", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${subscription.currency} ${String.format("%.2f", monthlyEquivalent)}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("Annual Projected", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${subscription.currency} ${String.format("%.2f", annualCommitment)}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            val totalPaid = payments.sumOf { it.amountPaid }
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Total Paid to Date", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${subscription.currency} ${String.format("%.2f", totalPaid)}",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Across ${payments.size} confirmed payments",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Dialog: Record Payment ---
    billToPay?.let { bill ->
        val billPayments = payments.filter { it.billId == bill.id }
        val paidSoFar = billPayments.sumOf { it.amountPaid }
        val balanceDue = (bill.expectedAmount - paidSoFar).coerceAtLeast(0.0)
        var paymentAmountText by remember { mutableStateOf(String.format("%.2f", balanceDue)) }
        var paymentMethod by remember { mutableStateOf("UPI / Card") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { billToPay = null },
            title = { Text("Record Bill Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Bill due on ${bill.dueDateIso} (Total: ${bill.currency} ${bill.expectedAmount})")
                    OutlinedTextField(
                        value = paymentAmountText,
                        onValueChange = { paymentAmountText = it },
                        label = { Text("Payment Amount (${bill.currency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = { paymentMethod = it },
                        label = { Text("Payment Method") },
                        placeholder = { Text("e.g. Card, UPI, NetBanking") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Reference (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentAmountText.toDoubleOrNull() ?: balanceDue
                        viewModel.recordBillPayment(
                            billId = bill.id,
                            subscriptionId = subscription.id,
                            amountPaid = amount,
                            paymentDateIso = LocalDate.now().toString(),
                            paymentMethod = paymentMethod.trim(),
                            notes = notes.trim()
                        )
                        billToPay = null
                    }
                ) {
                    Text("Confirm Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { billToPay = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Dialog: Record Price Change ---
    if (showPriceChangeDialog) {
        var newAmountText by remember { mutableStateOf("") }
        var effectiveDateText by remember { mutableStateOf(LocalDate.now().toString()) }
        var reasonText by remember { mutableStateOf("Plan upgrade / price change") }

        AlertDialog(
            onDismissRequest = { showPriceChangeDialog = false },
            title = { Text("Record Price Change") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Current price: ${subscription.currency} ${subscription.amount}")
                    OutlinedTextField(
                        value = newAmountText,
                        onValueChange = { newAmountText = it },
                        label = { Text("New Amount (${subscription.currency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = effectiveDateText,
                        onValueChange = { effectiveDateText = it },
                        label = { Text("Effective Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        label = { Text("Reason for Price Change") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newAmount = newAmountText.toDoubleOrNull()
                        if (newAmount != null && newAmount > 0) {
                            viewModel.updateSubscriptionPrice(
                                subscriptionId = subscription.id,
                                newPrice = newAmount,
                                effectiveDateIso = effectiveDateText.trim(),
                                reason = reasonText.trim()
                            )
                            showPriceChangeDialog = false
                        }
                    }
                ) {
                    Text("Save Price Change")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPriceChangeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Dialog: Delete Subscription Confirmation ---
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Subscription '${subscription.title}'?") },
            text = {
                Text("Are you sure? This will delete this subscription, all past bills, and payment records.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubscription(subscription.id)
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
