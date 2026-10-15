package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ChallengePreset
import com.example.domain.model.ChallengePresetCatalog
import com.example.domain.model.ChallengeRule
import com.example.domain.model.ChallengeState
import com.example.domain.model.HabitItem
import com.example.domain.model.HoneycombColorPresets
import com.example.domain.model.HoneycombShortcut
import com.example.domain.model.ShortcutType
import com.example.domain.model.TaskItem

/**
 * Context Menu modal for a long-pressed Honeycomb Cell.
 */
@Composable
fun HoneycombContextMenuDialog(
    shortcut: HoneycombShortcut,
    onDismiss: () -> Unit,
    onToggleCompletion: () -> Unit,
    onEnterEditMode: () -> Unit,
    onChangeEmoji: (String) -> Unit,
    onChangeColor: (Long?) -> Unit,
    onRemoveFromDashboard: () -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = shortcut.emoji, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = shortcut.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (shortcut.isCompletedToday) "✅ Completed Today" else "⏳ Incomplete for Today",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (shortcut.isCompletedToday) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Action 1: Toggle Completion
                FilledTonalButton(
                    onClick = {
                        onToggleCompletion()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Toggle",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (shortcut.isCompletedToday) "Mark as Incomplete" else "Mark as Complete")
                }

                // Action 2: Change Icon
                OutlinedButton(
                    onClick = { showEmojiPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Change Icon / Emoji (${shortcut.emoji})")
                }

                // Action 3: Change Hexagon Color
                OutlinedButton(
                    onClick = { showColorPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Color",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Hexagon Color")
                }

                // Action 4: Enter Edit Layout Mode
                OutlinedButton(
                    onClick = {
                        onEnterEditMode()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Layout",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enter Layout Edit Mode")
                }

                // Action 5: Remove from Dashboard
                Button(
                    onClick = {
                        onRemoveFromDashboard()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remove from Dashboard")
                }

                Text(
                    text = "Note: Removing a shortcut from the dashboard does not delete your underlying task or habit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = shortcut.customColorHex,
            onDismiss = { showColorPicker = false },
            onSelectColor = { color ->
                onChangeColor(color)
                showColorPicker = false
                onDismiss()
            }
        )
    }

    if (showEmojiPicker) {
        EmojiPickerDialog(
            currentEmoji = shortcut.emoji,
            onDismiss = { showEmojiPicker = false },
            onSelectEmoji = { emoji ->
                onChangeEmoji(emoji)
                showEmojiPicker = false
                onDismiss()
            }
        )
    }
}

/**
 * Color picker dialog with presets and custom hex option.
 */
@Composable
fun ColorPickerDialog(
    currentColor: Long?,
    onDismiss: () -> Unit,
    onSelectColor: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Hexagon Color", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Preset Colors:", style = MaterialTheme.typography.bodySmall)

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HoneycombColorPresets.list.forEach { (name, hex) ->
                        val isSelected = currentColor == hex
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onSelectColor(hex) }
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = name,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { onSelectColor(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use Default Theme Color")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Quick emoji picker dialog.
 */
@Composable
fun EmojiPickerDialog(
    currentEmoji: String,
    onDismiss: () -> Unit,
    onSelectEmoji: (String) -> Unit
) {
    val commonEmojis = listOf(
        "💧", "📖", "🧘", "🏃", "☀️", "🛏️", "💼", "🏠", "🌙", "📅",
        "🏋️", "🥗", "📸", "⚡", "🎯", "🔥", "🛡️", "🌟", "🧼", "🚿",
        "👕", "🪥", "🍎", "🚴", "💻", "🧠", "📚", "✨", "✅", "👑"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Icon / Emoji", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val rows = commonEmojis.chunked(5)
                rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { emoji ->
                            val isSelected = emoji == currentEmoji
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { onSelectEmoji(emoji) }
                            ) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Quick Hydration Control modal for non-binary water goal.
 */
@Composable
fun QuickHydrationDialog(
    currentMl: Int,
    goalMl: Int,
    onAddWater: (Int) -> Unit,
    onResetWater: () -> Unit,
    onDismiss: () -> Unit
) {
    val progress = (currentMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalDrink,
                    contentDescription = "Hydration",
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Quick Hydration Tracker", fontWeight = FontWeight.Bold)
                    Text("$currentMl / $goalMl ml (${(progress * 100).toInt()}%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = Color(0xFF3B82F6),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Text("Quick Add Water:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onAddWater(250) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+250 ml\n(Glass)", textAlign = TextAlign.Center, fontSize = 11.sp)
                    }

                    FilledTonalButton(
                        onClick = { onAddWater(500) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+500 ml\n(Bottle)", textAlign = TextAlign.Center, fontSize = 11.sp)
                    }

                    FilledTonalButton(
                        onClick = { onAddWater(1000) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+1.0 L\n(Flask)", textAlign = TextAlign.Center, fontSize = 11.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            val remaining = (goalMl - currentMl).coerceAtLeast(0)
                            if (remaining > 0) onAddWater(remaining)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Fill Goal 💧", fontSize = 11.sp)
                    }

                    TextButton(
                        onClick = onResetWater,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reset Today", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

/**
 * Feature 5: Customize Dashboard Dialog.
 * Allows adding existing tasks, habits, challenge rules, or water shortcuts.
 */
@Composable
fun CustomizeDashboardDialog(
    currentShortcuts: List<HoneycombShortcut>,
    allTasks: List<TaskItem>,
    allHabits: List<HabitItem>,
    challengeState: ChallengeState,
    onAddShortcut: (HoneycombShortcut) -> Unit,
    onRemoveShortcut: (Long) -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val tabs = listOf("Current", "Add Task", "Add Habit", "Add Challenge", "Water")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Customize Dashboard", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 12.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (selectedTab) {
                    0 -> {
                        // Current Shortcuts
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${currentShortcuts.size} active hexagon shortcuts", style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = onResetDefaults) {
                                    Text("Reset Defaults", fontSize = 11.sp)
                                }
                            }

                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(currentShortcuts) { item ->
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Text(text = item.emoji, fontSize = 20.sp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(text = item.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                    Text(text = "${item.type.name} • ${item.subtitle}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            IconButton(
                                                onClick = { onRemoveShortcut(item.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Add from Tasks
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search tasks...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val filteredTasks = allTasks.filter {
                                !it.isArchived && (searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true))
                            }

                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(filteredTasks) { task ->
                                    val alreadyAdded = currentShortcuts.any { it.type == ShortcutType.TASK && it.targetId == task.id.toString() }
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Text(text = task.emoji, fontSize = 20.sp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(text = task.title, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                                    Text(text = "${task.category} • ${task.dueTime ?: "Anytime"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            if (alreadyAdded) {
                                                Text("Added", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            } else {
                                                FilledTonalButton(
                                                    onClick = {
                                                        onAddShortcut(
                                                            HoneycombShortcut(
                                                                type = ShortcutType.TASK,
                                                                targetId = task.id.toString(),
                                                                title = task.title.take(12),
                                                                emoji = task.emoji
                                                            )
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text("+ Add", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Add from Habits
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(allHabits) { habit ->
                                val alreadyAdded = currentShortcuts.any { it.type == ShortcutType.HABIT && it.targetId == habit.id.toString() }
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Text(text = habit.emoji, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(text = habit.title, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                                Text(text = "${habit.currentStreak} day streak", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        if (alreadyAdded) {
                                            Text("Added", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            FilledTonalButton(
                                                onClick = {
                                                    onAddShortcut(
                                                        HoneycombShortcut(
                                                            type = ShortcutType.HABIT,
                                                            targetId = habit.id.toString(),
                                                            title = habit.title.take(12),
                                                            emoji = habit.emoji,
                                                            customColorHex = habit.colorHex
                                                        )
                                                    )
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("+ Add", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        // Add from 75 Hard Rules
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(challengeState.rules) { rule ->
                                val alreadyAdded = currentShortcuts.any { it.type == ShortcutType.CHALLENGE_RULE && it.targetId == rule.id }
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Text(text = rule.emoji, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(text = rule.title, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                                Text(text = rule.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                            }
                                        }
                                        if (alreadyAdded) {
                                            Text("Added", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            FilledTonalButton(
                                                onClick = {
                                                    onAddShortcut(
                                                        HoneycombShortcut(
                                                            type = ShortcutType.CHALLENGE_RULE,
                                                            targetId = rule.id,
                                                            title = rule.title.take(12),
                                                            emoji = rule.emoji,
                                                            customColorHex = 0xFFDC2626L // Crimson
                                                        )
                                                    )
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("+ Add", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    4 -> {
                        // Hydration shortcut
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "💧", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Hydration Tracker Shortcut", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Adds a quick honeycomb cell linked to daily water logging. Tapping provides instant +250ml logging or opens the hydration panel.",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val hasWater = currentShortcuts.any { it.type == ShortcutType.WATER }
                            if (hasWater) {
                                Text("Hydration cell already present on dashboard", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            } else {
                                Button(
                                    onClick = {
                                        onAddShortcut(
                                            HoneycombShortcut(
                                                type = ShortcutType.WATER,
                                                targetId = "water",
                                                title = "Hydration",
                                                emoji = "💧",
                                                customColorHex = 0xFF3B82F6L
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Add Water Hexagon")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

/**
 * Feature 7: Customize 75 Hard Program & Presets Modal.
 */
@Composable
fun CustomizeChallengePresetsDialog(
    challengeState: ChallengeState,
    onApplyPreset: (ChallengePreset, Boolean) -> Unit,
    onUpdateRules: (List<ChallengeRule>, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPreset by remember { mutableStateOf<ChallengePreset?>(null) }
    var showConfirmPresetDialog by remember { mutableStateOf(false) }
    var currentRules by remember { mutableStateOf(challengeState.rules) }
    var showAddRuleDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Customize 75-Day Program", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Program Presets:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                ChallengePresetCatalog.list.forEach { preset ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPreset = preset
                                showConfirmPresetDialog = true
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = preset.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("💧 ${preset.waterTargetMl}ml", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = preset.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Program Rules (${currentRules.size}):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showAddRuleDialog = true }) {
                        Text("+ Add Rule", fontSize = 11.sp)
                    }
                }

                currentRules.forEach { rule ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(text = rule.emoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = rule.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text(text = rule.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                            if (currentRules.size > 1) {
                                IconButton(
                                    onClick = {
                                        currentRules = currentRules.filterNot { it.id == rule.id }
                                        onUpdateRules(currentRules, false)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )

    // Confirmation dialog before changing preset
    if (showConfirmPresetDialog && selectedPreset != null) {
        val preset = selectedPreset!!
        AlertDialog(
            onDismissRequest = { showConfirmPresetDialog = false },
            title = { Text("Apply ${preset.name}?") },
            text = {
                Text(
                    "You are about to apply the ${preset.name} preset with ${preset.rules.size} daily discipline rules and ${preset.waterTargetMl}ml water goal.\n\n" +
                            "Would you like to keep your existing challenge progress or reset to Day 1?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onApplyPreset(preset, false) // keep progress
                        showConfirmPresetDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Keep Progress")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        onApplyPreset(preset, true) // reset to Day 1
                        showConfirmPresetDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Reset to Day 1")
                }
            }
        )
    }

    // Add Custom Rule Dialog
    if (showAddRuleDialog) {
        var ruleTitle by remember { mutableStateOf("") }
        var ruleEmoji by remember { mutableStateOf("🎯") }
        var ruleDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddRuleDialog = false },
            title = { Text("Add Challenge Rule") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = ruleEmoji,
                            onValueChange = { ruleEmoji = it.take(2) },
                            label = { Text("Emoji") },
                            modifier = Modifier.width(72.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = ruleTitle,
                            onValueChange = { ruleTitle = it },
                            label = { Text("Rule Title") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = ruleDesc,
                        onValueChange = { ruleDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ruleTitle.isNotBlank()) {
                            val newRule = ChallengeRule(
                                id = "rule_custom_${System.currentTimeMillis()}",
                                title = ruleTitle.trim(),
                                emoji = ruleEmoji.ifBlank { "🎯" },
                                description = ruleDesc.trim()
                            )
                            currentRules = currentRules + newRule
                            onUpdateRules(currentRules, false)
                            showAddRuleDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRuleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
