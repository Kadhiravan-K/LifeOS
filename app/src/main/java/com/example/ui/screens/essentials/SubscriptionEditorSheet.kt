package com.example.ui.screens.essentials

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.example.domain.model.ProjectItem
import com.example.domain.subscription.BillingFrequency
import com.example.domain.subscription.CustomIntervalUnit
import com.example.domain.subscription.MissingDatePolicy
import com.example.domain.subscription.SubscriptionItem
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionEditorSheet(
    subscription: SubscriptionItem?,
    projects: List<ProjectItem>,
    onSave: (SubscriptionItem) -> Unit,
    onDismiss: () -> Unit
) {
    val today = remember { LocalDate.now() }

    var title by remember { mutableStateOf(subscription?.title ?: "") }
    var emoji by remember { mutableStateOf(subscription?.emoji ?: "💳") }
    var description by remember { mutableStateOf(subscription?.description ?: "") }
    var selectedProjectId by remember { mutableStateOf(subscription?.projectId) }

    var amountText by remember { mutableStateOf(subscription?.amount?.toString() ?: "") }
    var currency by remember { mutableStateOf(subscription?.currency ?: "₹") }

    var frequency by remember { mutableStateOf(subscription?.frequency ?: BillingFrequency.MONTHLY) }
    var customIntervalCountText by remember { mutableStateOf(subscription?.customIntervalCount?.toString() ?: "1") }
    var customIntervalUnit by remember { mutableStateOf(subscription?.customIntervalUnit ?: CustomIntervalUnit.MONTHS) }

    var anchorDateIso by remember { mutableStateOf(subscription?.anchorDateIso ?: today.toString()) }
    var anchorDayOfMonth by remember { mutableIntStateOf(subscription?.anchorDayOfMonth ?: today.dayOfMonth) }
    var missingDatePolicy by remember { mutableStateOf(subscription?.missingDatePolicy ?: MissingDatePolicy.LAST_VALID_DAY) }

    var autoRenew by remember { mutableStateOf(subscription?.autoRenew ?: true) }
    var paymentMethod by remember { mutableStateOf(subscription?.paymentMethod ?: "") }

    var isTrial by remember { mutableStateOf(subscription?.isTrial ?: false) }
    var trialEndDateIso by remember { mutableStateOf(subscription?.trialEndDateIso ?: today.plusDays(14).toString()) }

    var reminderDaysBefore by remember { mutableIntStateOf(subscription?.reminderDaysBefore ?: 3) }
    var overdueReminderEnabled by remember { mutableStateOf(subscription?.overdueReminderEnabled ?: true) }
    var notes by remember { mutableStateOf(subscription?.notes ?: "") }

    var projectDropdownExpanded by remember { mutableStateOf(false) }

    val suggestedEmojis = listOf("🎬", "🎵", "☁️", "🍏", "📦", "🏋️", "🌐", "⚡", "📰", "🔒")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("subscription_editor_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = if (subscription == null) "New Subscription" else "Edit Subscription",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        val parsedAmount = amountText.toDoubleOrNull()
                        if (title.isNotBlank() && parsedAmount != null) {
                            val item = (subscription ?: SubscriptionItem(
                                title = title.trim(),
                                amount = parsedAmount,
                                anchorDateIso = anchorDateIso,
                                nextRenewalDateIso = anchorDateIso
                            )).copy(
                                title = title.trim(),
                                emoji = emoji.trim(),
                                description = description.trim(),
                                projectId = selectedProjectId,
                                amount = parsedAmount,
                                currency = currency,
                                frequency = frequency,
                                customIntervalCount = customIntervalCountText.toIntOrNull() ?: 1,
                                customIntervalUnit = customIntervalUnit,
                                anchorDateIso = anchorDateIso,
                                anchorDayOfMonth = anchorDayOfMonth,
                                missingDatePolicy = missingDatePolicy,
                                autoRenew = autoRenew,
                                paymentMethod = paymentMethod.trim(),
                                isTrial = isTrial,
                                trialStartDateIso = if (isTrial) anchorDateIso else null,
                                trialEndDateIso = if (isTrial) trialEndDateIso else null,
                                reminderDaysBefore = reminderDaysBefore,
                                overdueReminderEnabled = overdueReminderEnabled,
                                notes = notes.trim()
                            )
                            onSave(item)
                            onDismiss()
                        }
                    },
                    enabled = title.isNotBlank() && amountText.toDoubleOrNull() != null,
                    modifier = Modifier.testTag("save_subscription_btn")
                ) {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Emoji selector row
            Text("Icon / Emoji", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                suggestedEmojis.forEach { em ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (emoji == em) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { emoji = em }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(em, fontSize = 18.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Description
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Subscription Title *") },
                placeholder = { Text("e.g. Netflix Premium, Spotify Family") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subscription_title_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Provider / Plan") },
                placeholder = { Text("e.g. 4K Ultra HD 4 Screens") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Amount and Currency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it },
                    label = { Text("Currency") },
                    modifier = Modifier.width(90.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Billing Amount *") },
                    placeholder = { Text("e.g. 499.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subscription_amount_input"),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Project Selector
            ExposedDropdownMenuBox(
                expanded = projectDropdownExpanded,
                onExpandedChange = { projectDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = projects.find { it.id == selectedProjectId }?.name ?: "None (General)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Project / Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = projectDropdownExpanded,
                    onDismissRequest = { projectDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None (General)") },
                        onClick = {
                            selectedProjectId = null
                            projectDropdownExpanded = false
                        }
                    )
                    projects.forEach { proj ->
                        DropdownMenuItem(
                            text = { Text(proj.name) },
                            onClick = {
                                selectedProjectId = proj.id
                                projectDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Billing Frequency
            Text(
                "Billing Cadence",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(BillingFrequency.MONTHLY, BillingFrequency.YEARLY, BillingFrequency.QUARTERLY, BillingFrequency.WEEKLY).forEach { freq ->
                    FilterChip(
                        selected = frequency == freq,
                        onClick = { frequency = freq },
                        label = { Text(freq.displayName) }
                    )
                }
            }

            FilterChip(
                selected = frequency == BillingFrequency.CUSTOM,
                onClick = { frequency = BillingFrequency.CUSTOM },
                label = { Text("Custom Interval") }
            )

            if (frequency == BillingFrequency.CUSTOM) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customIntervalCountText,
                        onValueChange = { customIntervalCountText = it },
                        label = { Text("Every") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(90.dp),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CustomIntervalUnit.entries.forEach { unit ->
                            FilterChip(
                                selected = customIntervalUnit == unit,
                                onClick = { customIntervalUnit = unit },
                                label = { Text(unit.displayName) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Anchor Day & Missing Date Policy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = anchorDayOfMonth.toString(),
                    onValueChange = { v ->
                        v.toIntOrNull()?.let { if (it in 1..31) anchorDayOfMonth = it }
                    },
                    label = { Text("Anchor Day (1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = anchorDateIso,
                    onValueChange = { anchorDateIso = it },
                    label = { Text("Anchor Date (YYYY-MM-DD)") },
                    modifier = Modifier.weight(1.5f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Missing Date Policy (Short months like Feb)", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            MissingDatePolicy.entries.forEach { policy ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (missingDatePolicy == policy) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable { missingDatePolicy = policy }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(policy.displayName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(policy.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Trial Lifecycle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Free Trial Mode", fontWeight = FontWeight.Bold)
                    Text(
                        "Track trial expiration before first payment charge",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = isTrial, onCheckedChange = { isTrial = it })
            }

            if (isTrial) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = trialEndDateIso,
                    onValueChange = { trialEndDateIso = it },
                    label = { Text("Trial End Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Details & Reminders
            OutlinedTextField(
                value = paymentMethod,
                onValueChange = { paymentMethod = it },
                label = { Text("Payment Method (Optional)") },
                placeholder = { Text("e.g. HDFC Credit Card, UPI Autopay") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Renewal Advisory Notification", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 7).forEach { days ->
                    FilterChip(
                        selected = reminderDaysBefore == days,
                        onClick = { reminderDaysBefore = days },
                        label = { Text("$days day${if (days > 1) "s" else ""} before") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Cancellation Policy") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
        }
    }
}
