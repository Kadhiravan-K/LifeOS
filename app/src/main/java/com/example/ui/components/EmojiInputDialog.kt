package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.EmojiUtils

data class EmojiMetadata(
    val emoji: String,
    val category: String,
    val keywords: String
)

val MasterEmojiCatalog = listOf(
    // Smileys
    EmojiMetadata("😀", "Smileys", "grinning smile happy cheerful"),
    EmojiMetadata("😊", "Smileys", "blush smile pleasant warm"),
    EmojiMetadata("😎", "Smileys", "cool sunglasses confident win"),
    EmojiMetadata("🤩", "Smileys", "star eyes excited amazing"),
    EmojiMetadata("🥳", "Smileys", "party celebration congrats"),
    EmojiMetadata("😌", "Smileys", "relieved calm peace relaxed"),
    EmojiMetadata("🤔", "Smileys", "thinking ponder idea question"),
    EmojiMetadata("😴", "Smileys", "sleeping tired rest bed"),
    EmojiMetadata("🔥", "Smileys", "fire streak hot energy momentum"),
    EmojiMetadata("⭐", "Smileys", "star favorite gold special"),
    EmojiMetadata("✨", "Smileys", "sparkles shine clean fresh"),

    // Activities
    EmojiMetadata("🏃", "Activities", "running jog sprint cardio workout"),
    EmojiMetadata("🧘", "Activities", "meditation yoga breathe mindfulness zen"),
    EmojiMetadata("🏋️", "Activities", "workout gym weights strength training"),
    EmojiMetadata("🚴", "Activities", "cycling bike cycling ride exercise"),
    EmojiMetadata("🏊", "Activities", "swimming pool swim water swim"),
    EmojiMetadata("🚶", "Activities", "walking stroll steps outdoor"),
    EmojiMetadata("📚", "Activities", "reading book study learn education"),
    EmojiMetadata("🎨", "Activities", "art painting creative drawing design"),
    EmojiMetadata("🎵", "Activities", "music sound song listen audio"),
    EmojiMetadata("🎯", "Activities", "target focus goal bullseye aim"),

    // Objects
    EmojiMetadata("💻", "Objects", "laptop computer code work tech developer"),
    EmojiMetadata("📱", "Objects", "phone mobile call app device"),
    EmojiMetadata("🛠️", "Objects", "tools repair fix build wrench maintenance"),
    EmojiMetadata("🚀", "Objects", "rocket launch start boost ship scale"),
    EmojiMetadata("💡", "Objects", "lightbulb idea brainstorm inspiration"),
    EmojiMetadata("💼", "Objects", "briefcase business work office professional"),
    EmojiMetadata("🛒", "Objects", "shopping cart grocery buy store purchase"),
    EmojiMetadata("🏠", "Objects", "home house indoor chores personal"),
    EmojiMetadata("🚗", "Objects", "car drive travel commute transport"),
    EmojiMetadata("📦", "Objects", "package deliver box mail delivery"),

    // Health
    EmojiMetadata("💧", "Health", "water drink hydrate hydration liquid"),
    EmojiMetadata("🥗", "Health", "salad healthy nutrition diet eat clean"),
    EmojiMetadata("🍎", "Health", "apple fruit snack healthy food vitamin"),
    EmojiMetadata("💊", "Health", "pill medicine supplement vitamin prescription dose"),
    EmojiMetadata("🩺", "Health", "stethoscope doctor health medical clinic checkup"),
    EmojiMetadata("🫀", "Health", "heart organ cardio health pulse"),
    EmojiMetadata("🛌", "Health", "bed sleep rest recovery night recharge"),
    EmojiMetadata("☀️", "Health", "sun morning sunlight daylight energy wake"),
    EmojiMetadata("🌙", "Health", "moon bedtime sleep night rest calm"),

    // Finance
    EmojiMetadata("💰", "Finance", "money bag wealth cash savings earnings"),
    EmojiMetadata("💳", "Finance", "credit card payment subscription buy bill"),
    EmojiMetadata("💵", "Finance", "dollar bill cash currency bank money"),
    EmojiMetadata("📈", "Finance", "chart growth profit invest gain trend"),
    EmojiMetadata("🏦", "Finance", "bank finance institution account deposit"),
    EmojiMetadata("🪙", "Finance", "coin crypto token currency gold penny"),
    EmojiMetadata("🧾", "Finance", "receipt bill expense invoice tax cost"),
    EmojiMetadata("🏷️", "Finance", "tag price label discount offer sale")
)

val DefaultRecentEmojis = listOf("📚", "🏃", "💧", "🧘", "💻", "⭐", "🔥", "🎯")

@Composable
fun EmojiInputDialog(
    currentEmoji: String,
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var rawInput by remember { mutableStateOf(currentEmoji) }
    var detectedEmoji by remember { mutableStateOf(EmojiUtils.extractFirstEmoji(currentEmoji) ?: "") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf("All", "Smileys", "Activities", "Objects", "Health", "Finance")

    val filteredEmojis = remember(searchQuery, selectedCategory) {
        val q = searchQuery.trim().lowercase()
        MasterEmojiCatalog.filter { item ->
            val matchesCategory = selectedCategory == "All" || item.category == selectedCategory
            val matchesQuery = q.isEmpty() || item.emoji.contains(q) || item.keywords.lowercase().contains(q)
            matchesCategory && matchesQuery
        }
    }

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
                Text("Select Emoji", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Large Preview Circle
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(64.dp)
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
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search input with clear button
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search emoji (e.g. run, water, work)...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emoji_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Tabs Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Recently Used Row (if search is empty)
                if (searchQuery.isEmpty() && selectedCategory == "All") {
                    Text("Recently Used", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DefaultRecentEmojis.forEach { em ->
                            Surface(
                                onClick = {
                                    detectedEmoji = em
                                    rawInput = em
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (detectedEmoji == em) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (detectedEmoji == em) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = em, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Grid of matching emojis
                Text("Suggested Emojis (${filteredEmojis.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filteredEmojis.take(24).forEach { item ->
                        Surface(
                            onClick = {
                                detectedEmoji = item.emoji
                                rawInput = item.emoji
                                errorMessage = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (detectedEmoji == item.emoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (detectedEmoji == item.emoji) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = item.emoji, fontSize = 18.sp)
                            }
                        }
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
                modifier = Modifier.testTag("emoji_confirm_btn")
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Standard compact horizontal carousel of suggested emojis + highlighted "Custom" button
 * that triggers the full [EmojiInputDialog].
 */
@Composable
fun LifeEmojiPickerRow(
    selectedEmoji: String,
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    suggestedEmojis: List<String> = DefaultRecentEmojis.take(6)
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestedEmojis.forEach { em ->
            val isSelected = selectedEmoji == em
            Surface(
                onClick = { onEmojiSelected(em) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = em, fontSize = 20.sp)
                }
            }
        }

        // Custom Button
        Surface(
            onClick = { showDialog = true },
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
            modifier = Modifier.defaultMinSize(minWidth = 72.dp, minHeight = 42.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Custom...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }

    if (showDialog) {
        EmojiInputDialog(
            currentEmoji = selectedEmoji,
            onEmojiSelected = {
                onEmojiSelected(it)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * Standard clickable field displaying the selected emoji and opening [EmojiInputDialog].
 */
@Composable
fun LifeEmojiField(
    emoji: String,
    onEmojiSelected: (String) -> Unit,
    label: String = "Icon / Emoji",
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { showDialog = true }
            .testTag("life_emoji_field")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = if (emoji.isNotBlank()) emoji else "✨", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = if (emoji.isNotBlank()) "Selected: $emoji" else "Tap to choose", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text("Change", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    if (showDialog) {
        EmojiInputDialog(
            currentEmoji = emoji,
            onEmojiSelected = {
                onEmojiSelected(it)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}
