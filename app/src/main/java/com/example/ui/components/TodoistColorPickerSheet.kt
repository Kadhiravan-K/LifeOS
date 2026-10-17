package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ColorOrderingMode
import com.example.domain.model.TodoistColor
import com.example.domain.model.TodoistColorPalette
import com.example.ui.theme.engine.LocalLifeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoistColorPickerSheet(
    selectedColorId: String,
    orderingMode: ColorOrderingMode,
    usageCounts: Map<String, Int>,
    customOrder: List<String>,
    onColorSelected: (TodoistColor) -> Unit,
    onModeChanged: (ColorOrderingMode) -> Unit,
    onSaveCustomOrder: (List<String>) -> Unit,
    onResetCustomOrder: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val lifeTheme = LocalLifeTheme.current

    // Snapshot of sorted list at sheet presentation to prevent jumping during interaction
    val displayedColors = remember(orderingMode, customOrder) {
        when (orderingMode) {
            ColorOrderingMode.AUTO -> TodoistColorPalette.getColorsSortedByUsage(usageCounts)
            ColorOrderingMode.CUSTOM -> TodoistColorPalette.getColorsByCustomOrder(customOrder)
        }
    }

    var localCustomList by remember(displayedColors) { mutableStateOf(displayedColors) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("todoist_color_picker_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header: Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("color_picker_close_btn")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Two Ordering Modes Segmented Switch: Auto vs Custom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = orderingMode == ColorOrderingMode.AUTO,
                        onClick = { onModeChanged(ColorOrderingMode.AUTO) },
                        label = { Text("Auto (Most Used)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("color_mode_auto_chip")
                    )

                    FilterChip(
                        selected = orderingMode == ColorOrderingMode.CUSTOM,
                        onClick = { onModeChanged(ColorOrderingMode.CUSTOM) },
                        label = { Text("Custom Order", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("color_mode_custom_chip")
                    )
                }

                if (orderingMode == ColorOrderingMode.CUSTOM) {
                    TextButton(
                        onClick = {
                            onResetCustomOrder()
                            localCustomList = TodoistColorPalette.allColors
                        },
                        modifier = Modifier.testTag("color_reset_order_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle Description
            Text(
                text = if (orderingMode == ColorOrderingMode.AUTO) {
                    "Frequently used colors automatically appear at the top"
                } else {
                    "Use the arrow controls to manually organize your preferred order"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Vertically scrollable color list with high accessibility touch targets (>= 52dp)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .testTag("color_picker_list"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val listToRender = if (orderingMode == ColorOrderingMode.CUSTOM) localCustomList else displayedColors

                itemsIndexed(
                    items = listToRender,
                    key = { _, item -> item.id }
                ) { index, item ->
                    val isSelected = item.id.equals(selectedColorId, ignoreCase = true)
                    val count = usageCounts[item.id] ?: 0

                    Surface(
                        onClick = {
                            onColorSelected(item)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        } else {
                            Color.Transparent
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("color_row_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Palette icon filled with that color
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(item.composeColor)
                                        .border(
                                            width = 1.5.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (orderingMode == ColorOrderingMode.AUTO && count > 0) {
                                        Text(
                                            text = "$count× used",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Accessible reorder buttons for Custom mode
                                if (orderingMode == ColorOrderingMode.CUSTOM) {
                                    if (index > 0) {
                                        IconButton(
                                            onClick = {
                                                val mutable = localCustomList.toMutableList()
                                                val moved = mutable.removeAt(index)
                                                mutable.add(index - 1, moved)
                                                localCustomList = mutable
                                                onSaveCustomOrder(mutable.map { it.id })
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ArrowUpward,
                                                contentDescription = "Move ${item.name} up",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (index < listToRender.size - 1) {
                                        IconButton(
                                            onClick = {
                                                val mutable = localCustomList.toMutableList()
                                                val moved = mutable.removeAt(index)
                                                mutable.add(index + 1, moved)
                                                localCustomList = mutable
                                                onSaveCustomOrder(mutable.map { it.id })
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ArrowDownward,
                                                contentDescription = "Move ${item.name} down",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Checkmark indicator for selected color
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
