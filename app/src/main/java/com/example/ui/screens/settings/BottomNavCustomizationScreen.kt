package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.NavigationItem
import com.example.ui.viewmodel.LifeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavCustomizationScreen(
    viewModel: LifeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val rawSlot2 by viewModel.bottomNavSlot2Flow.collectAsState()
    val rawSlot4 by viewModel.bottomNavSlot4Flow.collectAsState()

    val currentSlot2 = remember(rawSlot2) {
        NavigationItem.fromSlotKey(rawSlot2, NavigationItem.TASKS)
    }
    val currentSlot4 = remember(rawSlot4) {
        NavigationItem.fromSlotKey(rawSlot4, NavigationItem.CALENDAR)
    }

    var showPickerForSlot by remember { mutableStateOf<Int?>(null) }
    var validationNotice by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bottom Navigation") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("bottom_nav_customization_back")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.fillMaxSize().testTag("bottom_nav_customization_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Five-Position Navigation Bar",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Customize the shortcuts in positions 2 and 4. Home, Center Quick Add, and Settings remain fixed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Visual Diagram of the 5 positions
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PositionPill(position = 1, label = "Home", isFixed = true)
                    PositionPill(position = 2, label = currentSlot2.label, isFixed = false)
                    PositionPill(position = 3, label = "Quick +", isFixed = true, isCenter = true)
                    PositionPill(position = 4, label = currentSlot4.label, isFixed = false)
                    PositionPill(position = 5, label = "Settings", isFixed = true)
                }
            }

            if (validationNotice != null) {
                Text(
                    text = validationNotice!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Slot 2 Selector Card
            DestinationSelectorCard(
                slotNumber = 2,
                currentDestination = currentSlot2,
                onClick = {
                    validationNotice = null
                    showPickerForSlot = 2
                },
                testTag = "selector_slot_2"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Slot 4 Selector Card
            DestinationSelectorCard(
                slotNumber = 4,
                currentDestination = currentSlot4,
                onClick = {
                    validationNotice = null
                    showPickerForSlot = 4
                },
                testTag = "selector_slot_4"
            )
        }
    }

    if (showPickerForSlot != null) {
        val targetSlot = showPickerForSlot!!
        val otherSlotDestination = if (targetSlot == 2) currentSlot4 else currentSlot2

        DestinationPickerDialog(
            title = "Choose Destination for Slot $targetSlot",
            options = NavigationItem.customizableDestinations,
            selectedItem = if (targetSlot == 2) currentSlot2 else currentSlot4,
            disabledItem = otherSlotDestination,
            onDismiss = { showPickerForSlot = null },
            onSelect = { selectedItem ->
                showPickerForSlot = null
                if (selectedItem == otherSlotDestination) {
                    validationNotice = "Both positions cannot have the same destination"
                } else {
                    if (targetSlot == 2) {
                        viewModel.setBottomNavSlots(selectedItem.name, currentSlot4.name)
                    } else {
                        viewModel.setBottomNavSlots(currentSlot2.name, selectedItem.name)
                    }
                }
            }
        )
    }
}

@Composable
private fun PositionPill(
    position: Int,
    label: String,
    isFixed: Boolean,
    isCenter: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(if (isCenter) 40.dp else 34.dp)
                .clip(CircleShape)
                .background(
                    if (isCenter) MaterialTheme.colorScheme.primary
                    else if (isFixed) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$position",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCenter) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isFixed) FontWeight.Normal else FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun DestinationSelectorCard(
    slotNumber: Int,
    currentDestination: NavigationItem,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth().testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = currentDestination.selectedIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Position $slotNumber Shortcut",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currentDestination.label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Button(onClick = onClick) {
                Text("Change")
            }
        }
    }
}

@Composable
private fun DestinationPickerDialog(
    title: String,
    options: List<NavigationItem>,
    selectedItem: NavigationItem,
    disabledItem: NavigationItem?,
    onDismiss: () -> Unit,
    onSelect: (NavigationItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEach { item ->
                    val isCurrent = item == selectedItem
                    val isDisabled = item == disabledItem

                    Surface(
                        onClick = { if (!isDisabled) onSelect(item) },
                        enabled = !isDisabled,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else if (isDisabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .testTag("destination_option_${item.name}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = item.selectedIcon,
                                contentDescription = null,
                                tint = if (isDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                       else if (isCurrent) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.label,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            else MaterialTheme.colorScheme.onSurface
                                )
                                if (isDisabled) {
                                    Text(
                                        text = "Already selected in other slot",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
