package com.example.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.HoneycombColorPresets
import com.example.ui.components.ColorPickerDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.components.TrashBinBottomSheet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.LifeViewModel

@Composable
fun SettingsScreen(
    viewModel: LifeViewModel,
    userName: String,
    themeMode: String,
    waterGoalMl: Int,
    notificationsEnabled: Boolean,
    hasInstalledDefaults: Boolean,
    wakeUpTime: String,
    bedTime: String,
    quietHoursStart: String,
    quietHoursEnd: String,
    clockFormat: String,
    firstDayOfWeek: String,
    defaultDuration: Int,
    onNavigateToThemeGallery: () -> Unit = {},
    onNavigateToPlanning: () -> Unit = {},
    onNavigateToGamification: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showEditNameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(userName) }
    var showInstallConfirmDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showTrashBinSheet by remember { mutableStateOf(false) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }

    var tempWakeUp by remember { mutableStateOf(wakeUpTime) }
    var tempBedTime by remember { mutableStateOf(bedTime) }
    var tempQuietStart by remember { mutableStateOf(quietHoursStart) }
    var tempQuietEnd by remember { mutableStateOf(quietHoursEnd) }
    var showHoneycombColorPicker by remember { mutableStateOf(false) }

    val appearance by viewModel.honeycombAppearance.collectAsStateWithLifecycle()
    val completionSound by viewModel.completionSoundEnabled.collectAsStateWithLifecycle()
    val completionVibration by viewModel.completionVibrationEnabled.collectAsStateWithLifecycle()
    val completionAnimation by viewModel.completionAnimationEnabled.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("settings_screen")
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Personalize your offline life management environment",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Profile / Greeting Name
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Your Name", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                IconButton(onClick = {
                    newName = userName
                    showEditNameDialog = true
                }) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Name")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sleep Protection & Quiet Hours Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bedtime, contentDescription = "Sleep", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Sleep & Quiet Hours", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Wake: $wakeUpTime • Bedtime: $bedTime", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = {
                        tempWakeUp = wakeUpTime
                        tempBedTime = bedTime
                        tempQuietStart = quietHoursStart
                        tempQuietEnd = quietHoursEnd
                        showScheduleDialog = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Sleep Window")
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Quiet hours ($quietHoursStart – $quietHoursEnd) suppresses non-critical reminder alarms.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Clock & Calendar Preferences
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Clock & Calendar Format", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))

                Text("Time Display Format", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("12_HOUR" to "12-Hour (AM/PM)", "24_HOUR" to "24-Hour").forEach { (fmt, label) ->
                        FilterChip(
                            selected = clockFormat == fmt,
                            onClick = { viewModel.updateClockFormat(fmt) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("First Day of the Week", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("MONDAY" to "Monday", "SUNDAY" to "Sunday").forEach { (day, label) ->
                        FilterChip(
                            selected = firstDayOfWeek == day,
                            onClick = { viewModel.updateFirstDayOfWeek(day) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Default Task Duration", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(15 to "15m", 30 to "30m", 45 to "45m", 60 to "60m").forEach { (dur, label) ->
                        FilterChip(
                            selected = defaultDuration == dur,
                            onClick = { viewModel.updateDefaultTaskDuration(dur) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Theme & Immersive Design System Selection
        val activeThemeDef by viewModel.activeThemeDefinition.collectAsStateWithLifecycle()
        val isPremiumActive by viewModel.isPremiumActive.collectAsStateWithLifecycle()

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, activeThemeDef.colors.outline),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("appearance_theme_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(activeThemeDef.colors.badgeBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = activeThemeDef.decoration.motifEmoji, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Design Theme: ${activeThemeDef.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeThemeDef.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onNavigateToThemeGallery,
                        modifier = Modifier.testTag("open_theme_gallery_btn"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gallery", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Quick Theme Switcher", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    com.example.ui.theme.engine.ThemeRegistry.allThemes.forEach { t ->
                        val isSelected = activeThemeDef.id == t.id
                        val isUnlocked = viewModel.isThemeUnlocked(t.id)

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isUnlocked) {
                                    viewModel.selectTheme(t.id)
                                } else {
                                    onNavigateToThemeGallery()
                                }
                            },
                            label = {
                                Text(
                                    text = if (!isUnlocked) "🔒 ${t.name}" else "${t.decoration.motifEmoji} ${t.name.split(" ").first()}",
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Theme Mode", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("SYSTEM", "DARK", "LIGHT").forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { viewModel.updateThemeMode(mode) },
                            label = { Text(mode, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Water Daily Goal
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Opacity, contentDescription = "Water", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Daily Hydration Target", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(1500, 2000, 2500, 3000, 3700).forEach { goal ->
                        FilterChip(
                            selected = waterGoalMl == goal,
                            onClick = { viewModel.updateWaterGoal(goal) },
                            label = { Text("${goal}ml", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- ⬡ HONEYCOMB APPEARANCE CARD ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("honeycomb_appearance_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⬡", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Honeycomb Dashboard Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { viewModel.resetHoneycombAppearance() }) {
                        Text("Reset", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Default Completion Color Presets
                Text(text = "Default Completion Color", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HoneycombColorPresets.list.forEach { (name, hex) ->
                        val isSelected = appearance.defaultColorHex == hex
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setHoneycombDefaultColor(hex) }
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = name,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showHoneycombColorPicker = true },
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Custom", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Border Color Style
                Text(text = "Border Style", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("DEFAULT" to "Subtle", "PRIMARY" to "Primary", "MUTED" to "Muted", "ACCENT" to "Purple").forEach { (type, label) ->
                        FilterChip(
                            selected = appearance.borderColorType == type,
                            onClick = { viewModel.setHoneycombBorderColor(type) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Border Thickness Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Border Thickness", style = MaterialTheme.typography.labelMedium)
                    Text(text = "${appearance.borderWidthDp} dp", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = appearance.borderWidthDp.toFloat(),
                    onValueChange = { viewModel.setHoneycombBorderWidth(it.toInt()) },
                    valueRange = 2f..8f,
                    steps = 5
                )

                // Hexagon Size Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Hexagon Cell Size", style = MaterialTheme.typography.labelMedium)
                    Text(text = "${appearance.hexagonSizeDp} dp", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = appearance.hexagonSizeDp.toFloat(),
                    onValueChange = { viewModel.setHoneycombSize(it.toInt()) },
                    valueRange = 56f..90f,
                    steps = 16
                )

                // Hexagon Spacing Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Cell Interlocking Spacing", style = MaterialTheme.typography.labelMedium)
                    Text(text = "${appearance.spacingDp} dp", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = appearance.spacingDp.toFloat(),
                    onValueChange = { viewModel.setHoneycombSpacing(it.toInt()) },
                    valueRange = 2f..14f,
                    steps = 5
                )

                // Icon / Emoji Size Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Icon / Symbol Size", style = MaterialTheme.typography.labelMedium)
                    Text(text = "${appearance.iconSizeSp} sp", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = appearance.iconSizeSp.toFloat(),
                    onValueChange = { viewModel.setHoneycombIconSize(it.toInt()) },
                    valueRange = 20f..36f,
                    steps = 7
                )

                // Smooth Animation Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Smooth Fill Animation", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(text = "Animate hexagon fill color on tap", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = appearance.isAnimationEnabled,
                        onCheckedChange = { viewModel.setHoneycombAnimationEnabled(it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Master Schedule & Routines Installer
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Routines", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "LifeOS Master Routines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Sleep 9:30 PM–5:30 AM, Laundry (Tue/Thu/Sun), Light cleaning, Sunday deep cleaning, Wednesday head bath, Saturday oil bath, 6-month toothbrush replacement, daily hydration, and weekly/monthly reviews.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showInstallConfirmDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("install_master_routines_button")
                ) {
                    Text(if (hasInstalledDefaults) "Re-install Default Routines" else "Install Default Routines")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Notifications Toggle
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = "Reminders", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Daily Digest Reminder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = "Local device notifications at 8:00 AM", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { viewModel.toggleNotifications(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Task Completion Feedback Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("task_completion_feedback_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Task Feedback",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Task Completion Feedback",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Immediate sensory feedback when marking tasks complete across all screens.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Sound Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Completion Sound", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = "Pleasant minimal harmonic chime", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = completionSound,
                        onCheckedChange = { viewModel.setCompletionSoundEnabled(it) },
                        modifier = Modifier.testTag("switch_completion_sound")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                // Vibration Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Completion Vibration", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = "Crisp, subtle haptic tap", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = completionVibration,
                        onCheckedChange = { viewModel.setCompletionVibrationEnabled(it) },
                        modifier = Modifier.testTag("switch_completion_vibration")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                // Animation Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Completion Animation", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = "Smooth circle fill and checkmark spring effect", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = completionAnimation,
                        onCheckedChange = { viewModel.setCompletionAnimationEnabled(it) },
                        modifier = Modifier.testTag("switch_completion_animation")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Data & Recovery (Trash Bin + Offline Backup)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_data_management_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Data Management & Recovery",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Trash Bin Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Trash Bin",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Trash Bin", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = "Restore deleted items • 30-day auto-purge", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedButton(
                        onClick = { showTrashBinSheet = true },
                        modifier = Modifier.testTag("open_trash_bin_button")
                    ) {
                        Text("View Trash")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Offline Backup & Restore Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Backup",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Offline Backup & Restore", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = "Export or restore full JSON database", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(
                        onClick = { showBackupRestoreDialog = true },
                        modifier = Modifier.testTag("open_backup_restore_button")
                    ) {
                        Text("Backup / Restore")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Advanced Life Modules
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LifeOS Strategic Modules",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Planning
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Planning",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Multi-Horizon Strategic Planning", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = "Daily, Weekly, Monthly, Quarterly, Yearly reviews", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedButton(
                        onClick = onNavigateToPlanning,
                        modifier = Modifier.testTag("settings_open_planning_button")
                    ) {
                        Text("Open")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Gamification
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Hunter Ranks",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Hunter Ranks & Rewards Store", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = "Level up, earn coins, and redeem custom rewards", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedButton(
                        onClick = onNavigateToGamification,
                        modifier = Modifier.testTag("settings_open_gamification_button")
                    ) {
                        Text("Open")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Privacy, Architecture & Release Info Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_privacy_architecture_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = "Security", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "LifeOS v1.0.0-rc.1 • 100% Offline", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Developed by Kadhiravan\n• Open-source native Android release candidate\n• Room SQLite database v6 (100% on-device)\n• Local AlarmManager & WorkManager background scheduling\n• Zero network calls, telemetry SDKs, analytics, ads, or accounts\n• Complete offline data ownership with JSON backup & restore",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Your Name") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.updateUserName(newName.trim())
                        }
                        showEditNameDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Sleep & Quiet Hours Dialog
    if (showScheduleDialog) {
        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            title = { Text("Configure Sleep & Quiet Hours") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempWakeUp,
                        onValueChange = { tempWakeUp = it },
                        label = { Text("Wake-up Time (HH:mm)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempBedTime,
                        onValueChange = { tempBedTime = it },
                        label = { Text("Bedtime (HH:mm)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempQuietStart,
                        onValueChange = { tempQuietStart = it },
                        label = { Text("Quiet Hours Start (HH:mm)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempQuietEnd,
                        onValueChange = { tempQuietEnd = it },
                        label = { Text("Quiet Hours End (HH:mm)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempWakeUp.isNotBlank()) viewModel.updateWakeUpTime(tempWakeUp.trim())
                        if (tempBedTime.isNotBlank()) viewModel.updateBedTime(tempBedTime.trim())
                        if (tempQuietStart.isNotBlank() && tempQuietEnd.isNotBlank()) {
                            viewModel.updateQuietHours(tempQuietStart.trim(), tempQuietEnd.trim())
                        }
                        showScheduleDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm Install Dialog
    if (showInstallConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showInstallConfirmDialog = false },
            title = { Text("Install Master Routines?") },
            text = {
                Text("This will populate your tasks and habits with the complete LifeOS schedule routines (Sleep, Laundry, Light Cleaning, Sunday Deep Cleaning, Head Baths, Toothbrush, 75-Day Challenge).")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.installDefaultRoutines(force = true)
                        showInstallConfirmDialog = false
                    }
                ) {
                    Text("Install Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showHoneycombColorPicker) {
        ColorPickerDialog(
            currentColor = appearance.defaultColorHex,
            onDismiss = { showHoneycombColorPicker = false },
            onSelectColor = { color ->
                if (color != null) {
                    viewModel.setHoneycombDefaultColor(color)
                }
                showHoneycombColorPicker = false
            }
        )
    }

    if (showTrashBinSheet) {
        TrashBinBottomSheet(
            viewModel = viewModel,
            onDismiss = { showTrashBinSheet = false }
        )
    }

    if (showBackupRestoreDialog) {
        BackupRestoreDialog(
            viewModel = viewModel,
            onDismiss = { showBackupRestoreDialog = false }
        )
    }
}

