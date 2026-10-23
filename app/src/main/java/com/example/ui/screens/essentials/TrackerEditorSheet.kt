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
import androidx.compose.material.icons.filled.Check
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
import com.example.domain.tracker.DistanceUnit
import com.example.domain.tracker.InventoryPolicy
import com.example.domain.tracker.QuantityUnit
import com.example.domain.tracker.TrackerItem
import com.example.domain.tracker.TrackerMeasurements

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerEditorSheet(
    tracker: TrackerItem?,
    projects: List<ProjectItem>,
    onSave: (TrackerItem) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(tracker?.title ?: "") }
    var emoji by remember { mutableStateOf(tracker?.emoji ?: "📦") }
    var description by remember { mutableStateOf(tracker?.description ?: "") }
    var selectedProjectId by remember { mutableStateOf(tracker?.projectId) }

    // Measurements (independently toggleable, not mutually exclusive)
    var trackDuration by remember { mutableStateOf(tracker?.measurements?.trackDuration ?: true) }
    var trackDistance by remember { mutableStateOf(tracker?.measurements?.trackDistance ?: false) }
    var trackQuantity by remember { mutableStateOf(tracker?.measurements?.trackQuantity ?: false) }

    var distanceUnit by remember { mutableStateOf(tracker?.distanceUnit ?: DistanceUnit.KILOMETERS) }
    var quantityUnit by remember { mutableStateOf(tracker?.quantityUnit ?: QuantityUnit.PIECES) }
    var customQuantityUnitName by remember { mutableStateOf(tracker?.customQuantityUnitName ?: "") }

    // Inventory
    var inventoryPolicy by remember { mutableStateOf(tracker?.inventoryPolicy ?: InventoryPolicy.CONSUMABLE) }
    var currentStockText by remember { mutableStateOf(tracker?.currentStock?.toString() ?: "1.0") }
    var emptyStockText by remember { mutableStateOf(tracker?.emptyStock?.toString() ?: "0.0") }

    // Reminders
    var isReminderEnabled by remember { mutableStateOf(tracker?.isReminderEnabled ?: false) }
    var reminderDaysText by remember { mutableStateOf(tracker?.reminderDaysInterval?.toString() ?: "30") }

    var projectDropdownExpanded by remember { mutableStateOf(false) }

    val suggestedEmojis = listOf("🚗", "👟", "💧", "🔥", "👁️", "🪥", "🖨️", "🚲", "⚙️", "🔋")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("tracker_editor_sheet")
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
                        text = if (tracker == null) "New Smart Tracker" else "Edit Tracker",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        val hasAtLeastOneMeasurement = trackDuration || trackDistance || trackQuantity
                        if (title.isNotBlank() && hasAtLeastOneMeasurement) {
                            val item = (tracker ?: TrackerItem(title = title.trim())).copy(
                                title = title.trim(),
                                emoji = emoji.trim(),
                                description = description.trim(),
                                projectId = selectedProjectId,
                                measurements = TrackerMeasurements(
                                    trackDuration = trackDuration,
                                    trackDistance = trackDistance,
                                    trackQuantity = trackQuantity
                                ),
                                distanceUnit = distanceUnit,
                                quantityUnit = quantityUnit,
                                customQuantityUnitName = customQuantityUnitName.trim(),
                                inventoryPolicy = inventoryPolicy,
                                currentStock = currentStockText.toDoubleOrNull() ?: 0.0,
                                emptyStock = emptyStockText.toDoubleOrNull() ?: 0.0,
                                isReminderEnabled = isReminderEnabled,
                                reminderDaysInterval = reminderDaysText.toIntOrNull() ?: 30
                            )
                            onSave(item)
                            onDismiss()
                        }
                    },
                    enabled = title.isNotBlank() && (trackDuration || trackDistance || trackQuantity),
                    modifier = Modifier.testTag("save_tracker_btn")
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
                label = { Text("Tracker Name *") },
                placeholder = { Text("e.g. Car Engine Oil, Water Purifier Filter") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tracker_name_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Usage Notes") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )

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

            // Measurements Selection
            Text(
                "Tracking Dimensions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Select one or more dimensions to measure concurrently per usage cycle.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = trackDuration,
                    onClick = { trackDuration = !trackDuration },
                    label = { Text("⏳ Duration") },
                    leadingIcon = if (trackDuration) { { Icon(Icons.Default.Check, null) } } else null
                )
                FilterChip(
                    selected = trackDistance,
                    onClick = { trackDistance = !trackDistance },
                    label = { Text("🚗 Distance") },
                    leadingIcon = if (trackDistance) { { Icon(Icons.Default.Check, null) } } else null
                )
                FilterChip(
                    selected = trackQuantity,
                    onClick = { trackQuantity = !trackQuantity },
                    label = { Text("📦 Quantity") },
                    leadingIcon = if (trackQuantity) { { Icon(Icons.Default.Check, null) } } else null
                )
            }

            if (trackDistance) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Distance Unit", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DistanceUnit.entries.forEach { unit ->
                            FilterChip(
                                selected = distanceUnit == unit,
                                onClick = { distanceUnit = unit },
                                label = { Text(unit.symbol) }
                            )
                        }
                    }
                }
            }

            if (trackQuantity) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Quantity Unit", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(QuantityUnit.PIECES, QuantityUnit.LITERS, QuantityUnit.CUSTOM).forEach { unit ->
                            FilterChip(
                                selected = quantityUnit == unit,
                                onClick = { quantityUnit = unit },
                                label = { Text(unit.symbol) }
                            )
                        }
                    }
                }
                if (quantityUnit == QuantityUnit.CUSTOM) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customQuantityUnitName,
                        onValueChange = { customQuantityUnitName = it },
                        label = { Text("Custom Unit Name") },
                        placeholder = { Text("e.g. cans, capsules") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Inventory Policy
            Text(
                "Inventory Policy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            InventoryPolicy.entries.forEach { policy ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (inventoryPolicy == policy) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { inventoryPolicy = policy }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            policy.displayName,
                            fontWeight = FontWeight.SemiBold,
                            color = if (inventoryPolicy == policy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            policy.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (inventoryPolicy != InventoryPolicy.NONE) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentStockText,
                        onValueChange = { currentStockText = it },
                        label = { Text("Current Stock (${quantityUnit.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    if (inventoryPolicy == InventoryPolicy.REFILLABLE_EXCHANGEABLE) {
                        OutlinedTextField(
                            value = emptyStockText,
                            onValueChange = { emptyStockText = it },
                            label = { Text("Empty Containers") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Reminders
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Advisory Reminder", fontWeight = FontWeight.Bold)
                    Text(
                        "Periodic check-in or replacement estimate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isReminderEnabled,
                    onCheckedChange = { isReminderEnabled = it }
                )
            }

            if (isReminderEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reminderDaysText,
                    onValueChange = { reminderDaysText = it },
                    label = { Text("Reminder Interval (Days)") },
                    placeholder = { Text("e.g. 30, 90, 180") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}
