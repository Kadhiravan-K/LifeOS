package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ProfileEntity

val PROFILE_EMOJI_PRESETS = listOf("📁", "👤", "💼", "🏠", "🚀", "💻", "🎨", "📚", "🏃", "🧪", "🎮", "🌟")

val PROFILE_COLOR_PRESETS = listOf(
    0xFF3B82F6L to "Blue",
    0xFF8B5CF6L to "Purple",
    0xFF10B981L to "Emerald",
    0xFFF59E0BL to "Amber",
    0xFFF43F5EL to "Rose",
    0xFF06B6D4L to "Cyan",
    0xFF6366F1L to "Indigo",
    0xFF64748BL to "Slate"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String, colorHex: Long, type: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("💼") }
    var selectedColor by remember { mutableStateOf(0xFF3B82F6L) }
    var selectedType by remember { mutableStateOf(ProfileEntity.TYPE_WORK) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = selectedEmoji, fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                Text(
                    text = "New Workspace Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Workspace profiles keep your tasks, projects, habits, and shortcuts isolated.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g. Work, Freelance, Studies") },
                    isError = isError,
                    supportingText = if (isError) { { Text("Profile name cannot be blank") } } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Profile Type
                Text(
                    text = "Profile Type",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        ProfileEntity.TYPE_WORK to "Work",
                        ProfileEntity.TYPE_PERSONAL to "Personal",
                        ProfileEntity.TYPE_CUSTOM to "Custom"
                    ).forEach { (typeKey, label) ->
                        FilterChip(
                            selected = selectedType == typeKey,
                            onClick = { selectedType = typeKey },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Emoji Picker
                Text(
                    text = "Icon Emoji",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PROFILE_EMOJI_PRESETS.forEach { emoji ->
                        val isSelected = selectedEmoji == emoji
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Color Picker
                Text(
                    text = "Theme Accent Color",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PROFILE_COLOR_PRESETS.forEach { (colorLong, _) ->
                        val isSelected = selectedColor == colorLong
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorLong))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorLong },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        isError = true
                    } else {
                        onConfirm(name.trim(), selectedEmoji, selectedColor, selectedType)
                    }
                },
                modifier = Modifier.testTag("profile_create_button")
            ) {
                Text("Create Workspace")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteProfileConfirmationDialog(
    profile: ProfileEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Delete Workspace?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to permanently delete \"${profile.name}\"?",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "All tasks, projects, habits, shortcuts, and trackers belonging to this profile will be permanently deleted. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("confirm_delete_profile_btn")
            ) {
                Text("Delete Workspace")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
