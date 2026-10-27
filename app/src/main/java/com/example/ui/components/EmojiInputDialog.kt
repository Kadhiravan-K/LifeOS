package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.EmojiUtils

@Composable
fun EmojiInputDialog(
    currentEmoji: String,
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var rawInput by remember { mutableStateOf(currentEmoji) }
    var detectedEmoji by remember { mutableStateOf(EmojiUtils.extractFirstEmoji(currentEmoji) ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetEmojis = listOf(
        "📚", "🏃", "💻", "🧘", "🛠️", "🚀", "🎯", "💡", "💼", "💧", "🥗", "🛒", "⭐", "🔥", "🎨", "🏠"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SentimentSatisfied,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Task Emoji", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Type or paste an emoji using your keyboard, or pick a shortcut below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Large Preview Circle
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (detectedEmoji.isNotBlank()) {
                                Text(
                                    text = detectedEmoji,
                                    fontSize = 32.sp,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.SentimentSatisfied,
                                    contentDescription = "No emoji",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Keyboard input field
                OutlinedTextField(
                    value = rawInput,
                    onValueChange = { input ->
                        val extracted = EmojiUtils.extractFirstEmoji(input)
                        if (extracted != null) {
                            rawInput = extracted
                            detectedEmoji = extracted
                            errorMessage = null
                        } else if (input.trim().isEmpty()) {
                            rawInput = ""
                            detectedEmoji = ""
                            errorMessage = null
                        } else {
                            rawInput = input.take(2)
                            errorMessage = "Please enter only one emoji (e.g. 📚, 🚀)"
                        }
                    },
                    placeholder = { Text("Enter emoji here...") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        } else if (detectedEmoji.isNotBlank()) {
                            Text("1 emoji selected", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    trailingIcon = {
                        if (rawInput.isNotEmpty()) {
                            IconButton(onClick = {
                                rawInput = ""
                                detectedEmoji = ""
                                errorMessage = null
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear input", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emoji_keyboard_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Shortcuts header
                Text("Shortcuts", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable shortcuts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetEmojis.forEach { em ->
                        Surface(
                            onClick = {
                                rawInput = em
                                detectedEmoji = em
                                errorMessage = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (detectedEmoji == em) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (detectedEmoji == em) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = em, fontSize = 20.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Clear button
                if (detectedEmoji.isNotBlank() || currentEmoji.isNotBlank()) {
                    TextButton(
                        onClick = {
                            rawInput = ""
                            detectedEmoji = ""
                            errorMessage = null
                        },
                        modifier = Modifier.testTag("clear_emoji_btn")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove Emoji (No Icon)")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onEmojiSelected(detectedEmoji)
                    onDismiss()
                },
                modifier = Modifier.testTag("emoji_dialog_set_btn")
            ) {
                Text("Set Emoji")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
