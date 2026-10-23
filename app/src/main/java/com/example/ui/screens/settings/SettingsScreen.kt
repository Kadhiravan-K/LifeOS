package com.example.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Medication
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import android.content.Intent
import android.provider.Settings
import com.example.notifications.NotificationHelper
import com.example.ui.feedback.FeedbackSoundType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.HoneycombColorPresets
import com.example.domain.templates.TemplateCatalog
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.components.ColorPickerDialog
import com.example.ui.components.TrashBinBottomSheet
import com.example.ui.screens.templates.TemplateBrowserDialog
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
    onNavigateToMedication: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(userName) }
    var showInstallConfirmDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showTrashBinSheet by remember { mutableStateOf(false) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }
    var showHoneycombColorPicker by remember { mutableStateOf(false) }
    var showTemplateBrowserDialog by remember { mutableStateOf(false) }
    var showResetGamificationDialog by remember { mutableStateOf(false) }
    var showClearTemplatesDialog by remember { mutableStateOf(false) }

    var tempWakeUp by remember { mutableStateOf(wakeUpTime) }
    var tempBedTime by remember { mutableStateOf(bedTime) }
    var tempQuietStart by remember { mutableStateOf(quietHoursStart) }
    var tempQuietEnd by remember { mutableStateOf(quietHoursEnd) }

    val appearance by viewModel.honeycombAppearance.collectAsStateWithLifecycle()
    val completionSound by viewModel.completionSoundEnabled.collectAsStateWithLifecycle()
    val completionVibration by viewModel.completionVibrationEnabled.collectAsStateWithLifecycle()
    val completionAnimation by viewModel.completionAnimationEnabled.collectAsStateWithLifecycle()
    val gamificationProfile by viewModel.gamificationProfile.collectAsStateWithLifecycle()
    val gamificationSound by viewModel.gamificationSoundEnabled.collectAsStateWithLifecycle()
    val gamificationVibration by viewModel.gamificationVibrationEnabled.collectAsStateWithLifecycle()
    val installedTemplates by viewModel.installedTemplateIds.collectAsStateWithLifecycle()
    val defaultReminderBehavior by viewModel.defaultReminderBehavior.collectAsStateWithLifecycle()
    val activeThemeDef by viewModel.activeThemeDefinition.collectAsStateWithLifecycle()

    // Smart Alerts & Notifications flows
    val notifMedicine by viewModel.notifMedicineEnabled.collectAsStateWithLifecycle()
    val notifTasks by viewModel.notifTasksEnabled.collectAsStateWithLifecycle()
    val notifHabits by viewModel.notifHabitsEnabled.collectAsStateWithLifecycle()
    val notifHydration by viewModel.notifHydrationEnabled.collectAsStateWithLifecycle()
    val notifCalendar by viewModel.notifCalendarEnabled.collectAsStateWithLifecycle()
    val notifQr by viewModel.notifQrEnabled.collectAsStateWithLifecycle()
    val notifChallenges by viewModel.notifChallengesEnabled.collectAsStateWithLifecycle()
    val notifGamification by viewModel.notifGamificationEnabled.collectAsStateWithLifecycle()
    val notifDailySummary by viewModel.notifDailySummaryEnabled.collectAsStateWithLifecycle()

    val defaultAlertStyle by viewModel.defaultAlertStyle.collectAsStateWithLifecycle()
    val notificationSoundChoice by viewModel.notificationSoundChoice.collectAsStateWithLifecycle()
    val notificationVibration by viewModel.notificationVibrationEnabled.collectAsStateWithLifecycle()
    val defaultSnoozeMinutes by viewModel.defaultSnoozeMinutes.collectAsStateWithLifecycle()
    val followUpEnabled by viewModel.followUpEnabled.collectAsStateWithLifecycle()
    val maxFollowUpCount by viewModel.maxFollowUpCount.collectAsStateWithLifecycle()
    val medicineOverrideQuietHours by viewModel.medicineOverrideQuietHours.collectAsStateWithLifecycle()
    val notificationPrivacyEnabled by viewModel.notificationPrivacyEnabled.collectAsStateWithLifecycle()
    val p1AlarmEnabled by viewModel.p1AlarmEnabled.collectAsStateWithLifecycle()
    val p1SnoozeMinutes by viewModel.p1SnoozeMinutes.collectAsStateWithLifecycle()
    val p1MaxSnoozeCount by viewModel.p1MaxSnoozeCount.collectAsStateWithLifecycle()

    fun isSectionVisible(vararg keywords: String): Boolean {
        if (searchQuery.isBlank()) return true
        val query = searchQuery.trim().lowercase()
        return keywords.any { it.lowercase().contains(query) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("settings_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "8 strategic modules for your offline operating system",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search settings (theme, sound, templates, notifications)...") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search Settings")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear Search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_search_bar")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // SECTION 1: GENERAL
        // ==========================================
        if (isSectionVisible("general", "name", "user", "profile", "clock", "calendar", "duration", "first day", "reminder")) {
            SectionHeader(icon = Icons.Default.Settings, title = "1. General")

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Profile Name Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "Your Name", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        IconButton(onClick = {
                            newName = userName
                            showEditNameDialog = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Name")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // Clock Format
                    Text("Time Display Format", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
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

                    // First Day of Week
                    Text("First Day of Week", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("MONDAY" to "Monday", "SUNDAY" to "Sunday", "SATURDAY" to "Saturday").forEach { (day, label) ->
                            FilterChip(
                                selected = firstDayOfWeek == day,
                                onClick = { viewModel.updateFirstDayOfWeek(day) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Default Task Duration
                    Text("Default Task Duration", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(15 to "15m", 30 to "30m", 45 to "45m", 60 to "60m").forEach { (dur, label) ->
                            FilterChip(
                                selected = defaultDuration == dur,
                                onClick = { viewModel.updateDefaultTaskDuration(dur) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Default Reminder Offset
                    Text("Default Reminder Timing", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            "AT_TIME" to "At Event Time",
                            "5_MIN" to "5m Before",
                            "10_MIN" to "10m Before",
                            "15_MIN" to "15m Before",
                            "30_MIN" to "30m Before"
                        ).forEach { (b, label) ->
                            FilterChip(
                                selected = defaultReminderBehavior == b,
                                onClick = { viewModel.updateDefaultReminderBehavior(b) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // ==========================================
        // SECTION 2: APPEARANCE & THEME
        // ==========================================
        if (isSectionVisible("appearance", "theme", "dark", "light", "color", "honeycomb", "sound", "vibration", "animation")) {
            SectionHeader(icon = Icons.Default.Palette, title = "2. Appearance")

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("appearance_theme_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Active Design Theme Header
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
                                    .background(activeThemeDef.colors.badgeBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = activeThemeDef.decoration.motifEmoji, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "Design Theme: ${activeThemeDef.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = activeThemeDef.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        OutlinedButton(
                            onClick = onNavigateToThemeGallery,
                            modifier = Modifier.testTag("open_theme_gallery_btn")
                        ) {
                            Text("Gallery", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Theme Mode Chips
                    Text("Theme Mode", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("SYSTEM", "DARK", "LIGHT").forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { viewModel.updateThemeMode(mode) },
                                label = { Text(mode, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Task Completion Feedback
                    Text("Task Sensory Feedback", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Completion Sound Chime", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Minimal harmonic chime on completion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = completionSound, onCheckedChange = { viewModel.setCompletionSoundEnabled(it) })
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Completion Haptic Tap", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Subtle tactile vibration feedback", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = completionVibration, onCheckedChange = { viewModel.setCompletionVibrationEnabled(it) })
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Completion Spring Animation", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Dynamic circle checkmark transition", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = completionAnimation, onCheckedChange = { viewModel.setCompletionAnimationEnabled(it) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Honeycomb Customizer Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⬡", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Honeycomb Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { viewModel.resetHoneycombAppearance() }) {
                            Text("Reset Defaults", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Default Completion Color", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
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
                                    .size(32.dp)
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
                                    Icon(imageVector = Icons.Default.Check, contentDescription = name, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { showHoneycombColorPicker = true },
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Custom", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sliders
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Hexagon Size: ${appearance.hexagonSizeDp} dp", style = MaterialTheme.typography.labelSmall)
                    }
                    Slider(
                        value = appearance.hexagonSizeDp.toFloat(),
                        onValueChange = { viewModel.setHoneycombSize(it.toInt()) },
                        valueRange = 56f..90f
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Cell Spacing: ${appearance.spacingDp} dp", style = MaterialTheme.typography.labelSmall)
                    }
                    Slider(
                        value = appearance.spacingDp.toFloat(),
                        onValueChange = { viewModel.setHoneycombSpacing(it.toInt()) },
                        valueRange = 2f..14f
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // ==========================================
        // SECTION 3: NOTIFICATIONS, MEDICINE & SLEEP
        // ==========================================
        if (isSectionVisible("notification", "sleep", "quiet", "water", "hydration", "wake", "bedtime", "alert", "reminder", "medicine", "sound", "vibration", "snooze")) {
            SectionHeader(icon = Icons.Default.Notifications, title = "3. Smart Alerts & Notifications")

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Master Notification Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "Master Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(text = "Local offline scheduled alarms & reminders", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { viewModel.toggleNotifications(it) }
                        )
                    }

                    val systemNotifsAllowed = remember { NotificationManagerCompat.from(context).areNotificationsEnabled() }
                    if (!systemNotifsAllowed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "System notifications are disabled in Android Settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                }) {
                                    Text("Fix", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // Dedicated Medicine Reminders Card Shortcut & Privacy Options
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Medication, contentDescription = "Medicine", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "Medicine Reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = "Manage schedules, doses & adherence", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        OutlinedButton(
                            onClick = onNavigateToMedication,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Open", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Allow Medicine in Quiet Hours", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = "Critical medicine alarms ring even during quiet hours", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = medicineOverrideQuietHours,
                            onCheckedChange = { viewModel.setMedicineOverrideQuietHours(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Discreet Lock-Screen Privacy", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = "Hide medication name & dosage on lock screen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = notificationPrivacyEnabled,
                            onCheckedChange = { viewModel.setNotificationPrivacyEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // 9 Channel Categories
                    Text(text = "Notification Channels", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    val categories = listOf(
                        Triple("MEDICINE", "Medicine Reminders", notifMedicine),
                        Triple("TASKS", "Task Due Alarms", notifTasks),
                        Triple("HABITS", "Habit Reminders", notifHabits),
                        Triple("HYDRATION", "Hydration Intervals", notifHydration),
                        Triple("CALENDAR", "Calendar Event Alerts", notifCalendar),
                        Triple("QR", "Quick Reminders", notifQr),
                        Triple("CHALLENGES", "75-Day Challenge", notifChallenges),
                        Triple("GAMIFICATION", "Hunter Ranks & Level-Ups", notifGamification),
                        Triple("DAILY_SUMMARY", "Morning/Evening Summaries", notifDailySummary)
                    )

                    categories.forEach { (catId, title, isEnabled) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Text(text = title, style = MaterialTheme.typography.bodySmall)
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { viewModel.setNotifCategoryEnabled(catId, it) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // Default Alert Style Selector
                    Text(text = "Default Alert Style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            "STANDARD" to "Standard (Chime + Banner)",
                            "ALARM" to "Alarm (Full Screen)",
                            "SILENT" to "Silent Tray"
                        ).forEach { (style, label) ->
                            FilterChip(
                                selected = defaultAlertStyle == style,
                                onClick = { viewModel.setDefaultAlertStyle(style) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Notification Sound & Preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Notification Sound", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = {
                                val soundType = when (notificationSoundChoice) {
                                    "BELL" -> FeedbackSoundType.TASK
                                    "DROP" -> FeedbackSoundType.WATER
                                    "RADAR" -> FeedbackSoundType.LEVEL_UP
                                    "GENTLE" -> FeedbackSoundType.HABIT
                                    else -> FeedbackSoundType.MILESTONE
                                }
                                viewModel.playSoundPreview(soundType)
                            }
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Preview", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Preview", fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        NotificationHelper.NOTIFICATION_SOUNDS.forEach { sound ->
                            FilterChip(
                                selected = notificationSoundChoice == sound,
                                onClick = { viewModel.setNotificationSound(sound) },
                                label = { Text(sound, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Vibration & Snooze Duration
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Alert Vibration", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Haptic pulse with notification alerts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = notificationVibration,
                            onCheckedChange = { viewModel.setNotificationVibrationEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Default Snooze Duration", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(5, 10, 15, 20, 30).forEach { mins ->
                            FilterChip(
                                selected = defaultSnoozeMinutes == mins,
                                onClick = { viewModel.setDefaultSnoozeMinutes(mins) },
                                label = { Text("${mins}m", fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Persistent Follow-Up Reminders
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Persistent Follow-Up Reminders", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = "Re-alerts up to $maxFollowUpCount times if unconfirmed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = followUpEnabled,
                            onCheckedChange = { viewModel.setFollowUpEnabled(it) }
                        )
                    }

                    if (followUpEnabled) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(1, 2, 3).forEach { count ->
                                FilterChip(
                                    selected = maxFollowUpCount == count,
                                    onClick = { viewModel.setMaxFollowUpCount(count) },
                                    label = { Text("${count}x repeat", fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // Priority 1 (P1) Smart Alarms Settings
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "🚨 Priority 1 (P1) Smart Alarms", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = "Prominent full-screen alarm reminders for critical tasks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = p1AlarmEnabled,
                            onCheckedChange = { viewModel.setP1AlarmEnabled(it) }
                        )
                    }

                    if (p1AlarmEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "P1 Snooze Duration:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(5, 10, 15, 30).forEach { mins ->
                                FilterChip(
                                    selected = p1SnoozeMinutes == mins,
                                    onClick = { viewModel.setP1SnoozeMinutes(mins) },
                                    label = { Text("${mins}m", fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "P1 Max Snooze Limit:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(1, 2, 3, 5).forEach { count ->
                                FilterChip(
                                    selected = p1MaxSnoozeCount == count,
                                    onClick = { viewModel.setP1MaxSnoozeCount(count) },
                                    label = { Text("${count}x max", fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // Sleep & Quiet Hours Row
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
                                Text(text = "Wake: $wakeUpTime • Bed: $bedTime • Quiet: $quietHoursStart–$quietHoursEnd", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = {
                            tempWakeUp = wakeUpTime
                            tempBedTime = bedTime
                            tempQuietStart = quietHoursStart
                            tempQuietEnd = quietHoursEnd
                            showScheduleDialog = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Schedule")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // Hydration Target
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Opacity, contentDescription = "Water", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Daily Hydration Target", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
        }

        // ==========================================
        // SECTION 4: TEMPLATES
        // ==========================================
        if (isSectionVisible("template", "routine", "catalog", "browse", "preset", "installed")) {
            SectionHeader(icon = Icons.Default.Layers, title = "4. Templates")

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📋", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Offline Master Templates",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${installedTemplates.size} installed • 11 categories available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { showTemplateBrowserDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("settings_browse_templates_button")
                        ) {
                            Text("Browse", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Import curated daily routines, morning/evening rituals, deep work blocks, study systems, hydration plans, and fitness structures with one tap. Zero duplication guaranteed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (installedTemplates.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = { showClearTemplatesDialog = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Clear Installed History", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // ==========================================
        // SECTION 5: GAMIFICATION & HUNTER RANK
        // ==========================================
        if (isSectionVisible("gamification", "xp", "rank", "level", "hunter", "rewards", "coins", "achievements")) {
            SectionHeader(icon = Icons.Default.EmojiEvents, title = "5. Gamification")

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(gamificationProfile.hunterRank.badgeColorHex)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = gamificationProfile.hunterRank.rankCode,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Rank ${gamificationProfile.hunterRank.rankCode} • Level ${gamificationProfile.currentLevel}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${gamificationProfile.currentXp} XP • 🪙 ${gamificationProfile.currentCoins} Coins",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onNavigateToGamification,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("settings_open_gamification_button")
                        ) {
                            Text("Open", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // Gamification Feedback Toggles
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Gamification Sound FX", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Level up and reward redemption chimes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = gamificationSound, onCheckedChange = { viewModel.setGamificationSound(it) })
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Gamification Haptic Vibration", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Haptic pulses when gaining XP or ranks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = gamificationVibration, onCheckedChange = { viewModel.setGamificationVibration(it) })
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = { showResetGamificationDialog = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Reset Gamification Progress", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // ==========================================
        // SECTION 6: DATA MANAGEMENT
        // ==========================================
        if (isSectionVisible("data", "backup", "restore", "trash", "export", "import", "reset", "database")) {
            SectionHeader(icon = Icons.Default.Storage, title = "6. Data Management")

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_data_management_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Trash Bin Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Delete, contentDescription = "Trash Bin", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Soft-Deleted Trash Bin", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(text = "Restore deleted items • 30-day auto-purge", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        OutlinedButton(
                            onClick = { showTrashBinSheet = true },
                            modifier = Modifier.testTag("open_trash_bin_button")
                        ) {
                            Text("View Trash", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // Backup & Restore Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Backup", tint = MaterialTheme.colorScheme.primary)
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
                            Text("Backup / Restore", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // Re-seed Defaults
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Master Life Routines", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = "Re-seed original schedule & habits", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { showInstallConfirmDialog = true }) {
                            Text(if (hasInstalledDefaults) "Re-install" else "Install", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // ==========================================
        // SECTION 7: PRIVACY & SECURITY
        // ==========================================
        if (isSectionVisible("privacy", "security", "offline", "network", "permission", "encryption", "local")) {
            SectionHeader(icon = Icons.Default.Security, title = "7. Privacy & Security")

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
                        Text(text = "100% Offline & Private Guarantee", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Zero Internet Permission in AndroidManifest\n• Local Room SQLite database v6 (100% on-device)\n• Encrypted Jetpack DataStore for preferences\n• AlarmManager & WorkManager local scheduling\n• Zero analytics, telemetry SDKs, ads, or tracking\n• Complete data sovereignty with zero external dependencies",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // ==========================================
        // SECTION 8: ABOUT
        // ==========================================
        if (isSectionVisible("about", "version", "kadhiravan", "license", "open source", "github")) {
            SectionHeader(icon = Icons.Default.Info, title = "8. About")

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "LifeOS v1.0.0-beta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Offline-First Personal Operating System for Android", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Architect & Lead Engineer: Kadhiravan", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Repository: https://github.com/Kadhiravan-K/LifeOS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Text(text = "License: MIT Open Source License", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // --- DIALOGS ---

    // Template Browser Dialog
    if (showTemplateBrowserDialog) {
        TemplateBrowserDialog(
            installedTemplateIds = installedTemplates,
            onInstallTemplate = { template, taskIdxs, habitIdxs, onDone ->
                viewModel.installTemplate(template, taskIdxs, habitIdxs, onDone)
            },
            onDismiss = { showTemplateBrowserDialog = false }
        )
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
                    singleLine = true,
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

    // Confirm Reset Gamification
    if (showResetGamificationDialog) {
        AlertDialog(
            onDismissRequest = { showResetGamificationDialog = false },
            title = { Text("Reset Gamification Progress?") },
            text = {
                Text("This will reset your Hunter Rank to E, Level to 1, total XP to 0, coins to 50, and relock all achievements. This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetGamificationProgress()
                        showResetGamificationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset Progress")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetGamificationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm Clear Installed Templates History
    if (showClearTemplatesDialog) {
        AlertDialog(
            onDismissRequest = { showClearTemplatesDialog = false },
            title = { Text("Clear Installed Templates History?") },
            text = {
                Text("This will clear the history of imported template badges. Your created tasks, habits, and projects will NOT be deleted.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearInstalledTemplates()
                        showClearTemplatesDialog = false
                    }
                ) {
                    Text("Clear History")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearTemplatesDialog = false }) {
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

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
