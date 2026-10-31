package com.example.ui.screens.projects

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.projectworkspace.AiPromptConfig
import com.example.domain.projectworkspace.AiPromptGenerator
import com.example.domain.projectworkspace.ProjectWorkspaceItem

/**
 * Structured AI Coding-Agent Prompt Generator Dialog (Release 3 Requirement #20).
 * Configurable checkboxes for Categories (Features, Bugs, Improvements), Versions (V1, V2, V3), Status,
 * and custom objective.
 * Generating or copying never modifies database records.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiPromptGeneratorDialog(
    projectName: String,
    projectDescription: String = "",
    workspaceItems: List<ProjectWorkspaceItem>,
    selectedItemIds: Set<Long> = emptySet(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Extract available versions and categories from items
    val availableVersions = remember(workspaceItems) {
        val versions = workspaceItems.map { it.version.trim() }.filter { it.isNotBlank() }.distinct()
        if (versions.isEmpty()) listOf("V1", "V2", "V3") else versions.sorted()
    }

    val availableStatuses = remember(workspaceItems) {
        val statuses = workspaceItems.map { it.status }.distinct()
        if (statuses.isEmpty()) listOf("NOT_STARTED", "IN_PROGRESS", "DONE") else statuses
    }

    // Config states
    var selectedCategories by remember {
        mutableStateOf(setOf("Feature", "Bug", "Improvement"))
    }
    var selectedVersions by remember {
        mutableStateOf(emptySet<String>()) // empty means all
    }
    var selectedStatuses by remember {
        mutableStateOf(emptySet<String>()) // empty means all
    }
    var onlySelectedItems by remember {
        mutableStateOf(selectedItemIds.isNotEmpty())
    }
    var customObjective by remember {
        mutableStateOf("")
    }
    var includeNotes by remember {
        mutableStateOf(true)
    }
    var includeProgress by remember {
        mutableStateOf(true)
    }

    // Live generated prompt
    val promptConfig = remember(
        selectedCategories,
        selectedVersions,
        selectedStatuses,
        onlySelectedItems,
        selectedItemIds,
        customObjective,
        includeNotes,
        includeProgress
    ) {
        AiPromptConfig(
            selectedCategories = selectedCategories,
            selectedVersions = selectedVersions,
            selectedStatuses = selectedStatuses,
            explicitItemIds = if (onlySelectedItems && selectedItemIds.isNotEmpty()) selectedItemIds else null,
            includeNotes = includeNotes,
            includeProgress = includeProgress,
            objectiveDescription = customObjective.trim()
        )
    }

    val generatedMarkdown = remember(projectName, projectDescription, workspaceItems, promptConfig) {
        AiPromptGenerator.generateMarkdownPrompt(
            projectName = projectName,
            projectDescription = projectDescription,
            items = workspaceItems,
            config = promptConfig
        )
    }

    var copiedToClipboard by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AI Coding-Agent Prompt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = projectName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Section: Scope Selection
                if (selectedItemIds.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = onlySelectedItems,
                            onCheckedChange = { onlySelectedItems = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Only export ${selectedItemIds.size} currently selected item(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Section: Category Filters
                Text(
                    text = "Include Categories:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Feature", "Bug", "Improvement").forEach { cat ->
                        val isSelected = selectedCategories.contains(cat)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategories = if (isSelected) {
                                    if (selectedCategories.size > 1) selectedCategories - cat else selectedCategories
                                } else {
                                    selectedCategories + cat
                                }
                            },
                            label = { Text(cat) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section: Version Filters
                Text(
                    text = "Target Versions (Leave empty for all):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableVersions.forEach { ver ->
                        val isSelected = selectedVersions.contains(ver)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedVersions = if (isSelected) {
                                    selectedVersions - ver
                                } else {
                                    selectedVersions + ver
                                }
                            },
                            label = { Text(ver) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section: Status Filter
                Text(
                    text = "Statuses (Leave empty for all):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableStatuses.forEach { st ->
                        val label = when (st) {
                            "NOT_STARTED" -> "Not Started"
                            "IN_PROGRESS" -> "In Progress"
                            "DONE" -> "Completed"
                            else -> st
                        }
                        val isSelected = selectedStatuses.contains(st)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedStatuses = if (isSelected) {
                                    selectedStatuses - st
                                } else {
                                    selectedStatuses + st
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Objective input
                OutlinedTextField(
                    value = customObjective,
                    onValueChange = { customObjective = it },
                    label = { Text("Custom Prompt Objective (Optional)") },
                    placeholder = { Text("e.g. Implement Release 3 Phase 6 project workspace...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Checkbox options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = includeNotes, onCheckedChange = { includeNotes = it })
                    Text("Include task descriptions & notes", style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = includeProgress, onCheckedChange = { includeProgress = it })
                    Text("Include current progress percentages", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Live Preview Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Prompt Preview (Markdown):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${generatedMarkdown.length} chars",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable Markdown Preview Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 220.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = generatedMarkdown,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(generatedMarkdown))
                    copiedToClipboard = true
                    Toast.makeText(context, "AI prompt copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.testTag("copy_ai_prompt_btn")
            ) {
                Icon(
                    imageVector = if (copiedToClipboard) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (copiedToClipboard) "Copied!" else "Copy as AI Prompt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
