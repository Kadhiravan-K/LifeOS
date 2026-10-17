package com.example.ui.screens.labels

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.domain.model.LabelItem
import com.example.domain.model.TodoistColorPalette
import com.example.ui.components.TodoistColorPickerSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLabelBottomSheet(
    label: LabelItem?,
    colorOrderingMode: ColorOrderingMode,
    colorUsageCounts: Map<String, Int>,
    colorCustomOrder: List<String>,
    onSave: (name: String, colorId: String, isFavorite: Boolean) -> Unit,
    onColorUsed: (String) -> Unit,
    onModeChanged: (ColorOrderingMode) -> Unit,
    onSaveCustomOrder: (List<String>) -> Unit,
    onResetCustomOrder: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(label?.name ?: "") }
    var selectedColorId by remember { mutableStateOf(label?.colorId ?: "blue") }
    var isFavorite by remember { mutableStateOf(label?.isFavorite ?: false) }
    var showColorPicker by remember { mutableStateOf(false) }

    val currentColor = TodoistColorPalette.getColorById(selectedColorId)
    val maxChars = 40

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("edit_label_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Top App Bar row with Back, Title, and Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("label_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Text(
                        text = if (label == null) "New Label" else "Edit Label",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        val trimmed = name.trim().removePrefix("#")
                        if (trimmed.isNotBlank()) {
                            onColorUsed(selectedColorId)
                            onSave(trimmed, selectedColorId, isFavorite)
                            onDismiss()
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.testTag("save_label_btn")
                ) {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Name field with character counter
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= maxChars) name = it },
                label = { Text("Label Name") },
                placeholder = { Text("e.g. important, reading") },
                leadingIcon = {
                    Text(
                        text = "#",
                        color = currentColor.composeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                supportingText = {
                    Text(
                        text = "${name.length}/$maxChars",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("label_name_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Color selection row (tappable row opening the color picker bottom sheet)
            Surface(
                onClick = { showColorPicker = true },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("label_color_row")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(currentColor.composeColor)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Color",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentColor.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Change color",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Favorite toggle
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = null,
                            tint = if (isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Add to Favorites",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Keep this label pinned to the top of filters",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isFavorite,
                        onCheckedChange = { isFavorite = it },
                        modifier = Modifier.testTag("label_favorite_switch")
                    )
                }
            }
        }
    }

    if (showColorPicker) {
        TodoistColorPickerSheet(
            selectedColorId = selectedColorId,
            orderingMode = colorOrderingMode,
            usageCounts = colorUsageCounts,
            customOrder = colorCustomOrder,
            onColorSelected = { color ->
                selectedColorId = color.id
                showColorPicker = false
            },
            onModeChanged = onModeChanged,
            onSaveCustomOrder = onSaveCustomOrder,
            onResetCustomOrder = onResetCustomOrder,
            onDismiss = { showColorPicker = false }
        )
    }
}
