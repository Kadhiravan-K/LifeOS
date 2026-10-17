package com.example.ui.screens.projects

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
import com.example.domain.model.ProjectItem
import com.example.domain.model.TodoistColorPalette
import com.example.ui.components.TodoistColorPickerSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProjectBottomSheet(
    project: ProjectItem?,
    colorOrderingMode: ColorOrderingMode,
    colorUsageCounts: Map<String, Int>,
    colorCustomOrder: List<String>,
    onSave: (name: String, description: String, emoji: String, colorId: String, isFavorite: Boolean) -> Unit,
    onColorUsed: (String) -> Unit,
    onModeChanged: (ColorOrderingMode) -> Unit,
    onSaveCustomOrder: (List<String>) -> Unit,
    onResetCustomOrder: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(project?.name ?: "") }
    var description by remember { mutableStateOf(project?.description ?: "") }
    var emoji by remember { mutableStateOf(project?.emoji ?: "📁") }
    var selectedColorId by remember { mutableStateOf(project?.colorId ?: "blue") }
    var isFavorite by remember { mutableStateOf(project?.isFavorite ?: false) }
    var showColorPicker by remember { mutableStateOf(false) }

    val currentColor = TodoistColorPalette.getColorById(selectedColorId)
    val suggestedEmojis = listOf("📁", "💼", "📚", "🚀", "💡", "🎯", "🏠", "🏃", "💰", "🎨")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("edit_project_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("project_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Text(
                        text = if (project == null) "New Project" else "Edit Project",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isNotBlank()) {
                            onColorUsed(selectedColorId)
                            onSave(trimmed, description.trim(), emoji, selectedColorId, isFavorite)
                            onDismiss()
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.testTag("save_project_btn")
                ) {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Emoji & Name Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { if (it.isNotBlank()) emoji = it.take(4) },
                    label = { Text("Icon") },
                    modifier = Modifier
                        .width(72.dp)
                        .testTag("project_emoji_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    placeholder = { Text("e.g. Embedded Systems") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("project_name_input"),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Emoji Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                suggestedEmojis.forEach { em ->
                    Surface(
                        onClick = { emoji = em },
                        shape = RoundedCornerShape(8.dp),
                        color = if (emoji == em) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = em, fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Description field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                placeholder = { Text("Add notes or goals for this project...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("project_desc_input"),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Color selection row
            Surface(
                onClick = { showColorPicker = true },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("project_color_row")
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
                                text = "Pin project to top of project list",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isFavorite,
                        onCheckedChange = { isFavorite = it },
                        modifier = Modifier.testTag("project_favorite_switch")
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
