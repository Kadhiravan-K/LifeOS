package com.example.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.components.EmojiInputDialog
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import com.example.ui.theme.engine.AnimationSpeed
import com.example.ui.theme.engine.ThemedLoadingIndicator
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.legal.LegalDocuments
import com.example.domain.model.HoneycombColorPresets
import com.example.domain.templates.TemplateCatalog
import com.example.notifications.NotificationHelper
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.components.ColorPickerDialog
import com.example.ui.components.TrashBinBottomSheet
import com.example.ui.feedback.FeedbackSoundType
import com.example.ui.screens.essentials.TrackersSubscriptionsScreen
import com.example.ui.screens.legal.LegalDocumentViewerDialog
import com.example.ui.screens.organization.ProjectsLabelsManagerScreen
import com.example.ui.screens.templates.TemplateBrowserDialog
import com.example.ui.viewmodel.LifeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SettingsSubpage {
    HUB,
    GENERAL,
    APPEARANCE,
    NOTIFICATIONS,
    ORGANIZATION,
    PROJECTS_LABELS,
    TRACKERS_SUBSCRIPTIONS,
    PRODUCTIVITY,
    GAMIFICATION,
    DATA_STORAGE,
    LEGAL_PRIVACY,
    USER_MANUAL
}

data class SettingsSearchItem(
    val title: String,
    val subtitle: String,
    val category: String,
    val subpage: SettingsSubpage,
    val keywords: List<String> = emptyList(),
    val onDirectAction: (() -> Unit)? = null
)

fun getSettingsSearchRegistry(
    onNavigateToThemeGallery: () -> Unit = {},
    onNavigateToPlanning: () -> Unit = {},
    onNavigateToGamification: () -> Unit = {},
    onNavigateToMedication: () -> Unit = {}
): List<SettingsSearchItem> = listOf(
    SettingsSearchItem("User Profile & Name", "Change user name and profile emoji avatar", "General", SettingsSubpage.GENERAL, listOf("username", "profile", "emoji", "avatar", "name", "picture")),
    SettingsSearchItem("Clock Format", "12-hour (AM/PM) vs 24-hour system time format", "General", SettingsSubpage.GENERAL, listOf("12h", "24h", "clock", "time", "format")),
    SettingsSearchItem("First Day of Week", "Configure calendar start day: Monday, Saturday, or Sunday", "General", SettingsSubpage.GENERAL, listOf("week", "calendar", "monday", "saturday", "sunday", "first day")),
    SettingsSearchItem("Default Task Duration", "Set default duration presets (15m, 30m, 45m, 60m, 90m) and custom duration", "General", SettingsSubpage.GENERAL, listOf("duration", "minutes", "task length", "default", "presets")),
    SettingsSearchItem("Theme Gallery", "Explore and activate themes, animations and atmospheres", "Appearance & Themes", SettingsSubpage.APPEARANCE, listOf("themes", "palette", "dark mode", "light mode", "atmosphere", "cosmic", "arcane", "cyber", "adventure"), onDirectAction = onNavigateToThemeGallery),
    SettingsSearchItem("Dark / Light Mode", "Switch between System, Light, and Dark mode", "Appearance & Themes", SettingsSubpage.APPEARANCE, listOf("theme", "mode", "dark", "light", "display")),
    SettingsSearchItem("QR Tile Shapes & Density", "Configure Honeycomb shortcut shapes (Hexagon, Circle, Square) and sizes", "Appearance & Themes", SettingsSubpage.APPEARANCE, listOf("honeycomb", "shape", "hex", "circle", "square", "density", "tiles", "qr")),
    SettingsSearchItem("Completion Effects", "Toggle completion sound, vibration, and celebration animations", "Appearance & Themes", SettingsSubpage.APPEARANCE, listOf("completion", "sound", "vibration", "animation", "celebration", "audio")),
    SettingsSearchItem("Smart Alarms & Notifications", "P1 alarms, medication reminders, snooze intervals, and sound alerts", "Notifications & Alerts", SettingsSubpage.NOTIFICATIONS, listOf("alarm", "notification", "p1", "snooze", "quiet hours", "sound")),
    SettingsSearchItem("Medication Reminders", "Schedule dosage times, interval alerts, and override quiet hours", "Notifications & Alerts", SettingsSubpage.NOTIFICATIONS, listOf("medicine", "medication", "pills", "dose", "prescription"), onDirectAction = onNavigateToMedication),
    SettingsSearchItem("Quiet Hours Schedule", "Mute non-critical notifications during sleep hours", "Notifications & Alerts", SettingsSubpage.NOTIFICATIONS, listOf("quiet hours", "sleep", "do not disturb", "mute", "silence")),
    SettingsSearchItem("Projects & Labels Manager", "Organize tasks by color-coded projects and custom tag labels", "Organization & Essentials", SettingsSubpage.PROJECTS_LABELS, listOf("projects", "labels", "tags", "categories", "folders")),
    SettingsSearchItem("Trackers & Subscriptions (My Essentials)", "Monitor monthly subscriptions, water intake, and recurring expenses", "Organization & Essentials", SettingsSubpage.TRACKERS_SUBSCRIPTIONS, listOf("essentials", "subscriptions", "expenses", "finance", "trackers", "spending")),
    SettingsSearchItem("Daily Schedule (Wake & Bed Time)", "Set wake up and bedtime hours for productivity planning", "Productivity & Schedule", SettingsSubpage.PRODUCTIVITY, listOf("schedule", "wake", "bed", "sleep", "routine", "hours")),
    SettingsSearchItem("Hydration Goal", "Configure daily target water intake in milliliters", "Productivity & Schedule", SettingsSubpage.PRODUCTIVITY, listOf("water", "hydration", "drink", "goal", "ml")),
    SettingsSearchItem("Daily Planning Wizard", "Guided time-blocking and priority alignment workflow", "Productivity & Schedule", SettingsSubpage.PRODUCTIVITY, listOf("planning", "wizard", "daily plan", "routine"), onDirectAction = onNavigateToPlanning),
    SettingsSearchItem("Gamification & XP Progress", "View levels, achievement badges, streaks, and reset XP profile", "Gamification & Progress", SettingsSubpage.GAMIFICATION, listOf("gamification", "xp", "level", "points", "badges", "streak", "rank"), onDirectAction = onNavigateToGamification),
    SettingsSearchItem("Encrypted Backup & Restore", "Export encrypted system JSON or restore LifeOS database", "Data & Storage", SettingsSubpage.DATA_STORAGE, listOf("backup", "restore", "export", "import", "database", "json")),
    SettingsSearchItem("Trash Bin Recovery & Retention", "Restore deleted items and configure auto-delete retention period", "Data & Storage", SettingsSubpage.DATA_STORAGE, listOf("trash", "bin", "recovery", "retention", "delete", "purge")),
    SettingsSearchItem("Starter Productivity Templates", "Browse and import curated habit, task, and shortcut bundles", "Data & Storage", SettingsSubpage.DATA_STORAGE, listOf("templates", "bundles", "browse", "routines", "morning", "deep work", "bucket list", "packing", "wedding", "startup")),
    SettingsSearchItem("Terms & Conditions", "Review legal terms, offline pledge, and application licensing", "Legal & Privacy", SettingsSubpage.LEGAL_PRIVACY, listOf("terms", "conditions", "legal", "license", "eula")),
    SettingsSearchItem("Privacy Policy", "Offline-first privacy architecture, zero telemetry, local storage", "Legal & Privacy", SettingsSubpage.LEGAL_PRIVACY, listOf("privacy", "policy", "offline", "data", "security", "telemetry")),
    SettingsSearchItem("User Manual & UI Components Library", "Explore 36 reusable frontend UI components and documentation", "User Manual", SettingsSubpage.USER_MANUAL, listOf("manual", "help", "guide", "components", "frontend", "ui library", "documentation"))
)

@OptIn(ExperimentalMaterial3Api::class)
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
    var currentSubpage by remember { mutableStateOf(SettingsSubpage.HUB) }
    var searchQuery by remember { mutableStateOf("") }

    // Intercept Back button when inside any subpage
    if (currentSubpage != SettingsSubpage.HUB) {
        BackHandler { currentSubpage = SettingsSubpage.HUB }
    }

    // Direct routing to Organization full screens
    when (currentSubpage) {
        SettingsSubpage.PROJECTS_LABELS -> {
            ProjectsLabelsManagerScreen(
                viewModel = viewModel,
                onNavigateBack = { currentSubpage = SettingsSubpage.HUB }
            )
            return
        }
        SettingsSubpage.TRACKERS_SUBSCRIPTIONS -> {
            TrackersSubscriptionsScreen(
                viewModel = viewModel,
                onNavigateBack = { currentSubpage = SettingsSubpage.HUB }
            )
            return
        }
        SettingsSubpage.USER_MANUAL -> {
            com.example.ui.screens.manual.UserManualScreen(
                onNavigateBack = { currentSubpage = SettingsSubpage.HUB }
            )
            return
        }
        else -> {}
    }

    // Shared dialog states
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
    var showProfileEmojiDialog by remember { mutableStateOf(false) }
    var viewingLegalDoc by remember { mutableStateOf<String?>(null) }

    var tempWakeUp by remember { mutableStateOf(wakeUpTime) }
    var tempBedTime by remember { mutableStateOf(bedTime) }
    var tempQuietStart by remember { mutableStateOf(quietHoursStart) }
    var tempQuietEnd by remember { mutableStateOf(quietHoursEnd) }

    // Reactive State flows
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
    val completionSoundType by viewModel.completionSoundTypeFlow.collectAsStateWithLifecycle()
    val customSoundUri by viewModel.customSoundUriFlow.collectAsStateWithLifecycle()
    val customSoundFileName by viewModel.customSoundFileNameFlow.collectAsStateWithLifecycle()
    val trashRetentionDays by viewModel.trashRetentionDaysFlow.collectAsStateWithLifecycle()
    val userProfileEmoji by viewModel.userProfileEmojiFlow.collectAsStateWithLifecycle()
    val animationSpeed by viewModel.animationSpeedFlow.collectAsStateWithLifecycle()
    val customColorsEnabled by viewModel.customColorsEnabledFlow.collectAsStateWithLifecycle()
    val independentThemeCustomization by viewModel.independentThemeCustomizationFlow.collectAsStateWithLifecycle()

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

    val acceptedTermsVersion by viewModel.acceptedTermsVersion.collectAsStateWithLifecycle()
    val acceptedPrivacyVersion by viewModel.acceptedPrivacyVersion.collectAsStateWithLifecycle()
    val termsAcceptedTimestamp by viewModel.termsAcceptedTimestamp.collectAsStateWithLifecycle()

    // Subpage renderer
    when (currentSubpage) {
        SettingsSubpage.HUB -> {
            SettingsHubView(
                userName = userName,
                themeMode = themeMode,
                activeThemeName = activeThemeDef.name,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onNavigateToSubpage = {
                    searchQuery = ""
                    currentSubpage = it
                },
                onNavigateToGamificationHub = onNavigateToGamification,
                onNavigateToThemeGallery = onNavigateToThemeGallery,
                onNavigateToPlanning = onNavigateToPlanning,
                onNavigateToMedication = onNavigateToMedication,
                modifier = modifier
            )
        }

        SettingsSubpage.GENERAL -> {
            SettingsSubpageScaffold(
                title = "General Settings",
                onBack = { currentSubpage = SettingsSubpage.HUB }
            ) {
                // Profile & Name (SET-06)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
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
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                        .clickable { showProfileEmojiDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = userProfileEmoji, fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = "User Name & Avatar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(text = userName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row {
                                IconButton(onClick = { showProfileEmojiDialog = true }) {
                                    Icon(Icons.Default.Mood, contentDescription = "Change Profile Emoji", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = {
                                    newName = userName
                                    showEditNameDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time & Preferences
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Clock Format", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("12h" to "12-hour (AM/PM)", "24h" to "24-hour").forEach { (valKey, label) ->
                                FilterChip(
                                    selected = clockFormat == valKey,
                                    onClick = { viewModel.updateClockFormat(valKey) },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(14.dp))

                        Text("First Day of Week", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("MONDAY" to "Monday", "SATURDAY" to "Saturday", "SUNDAY" to "Sunday").forEach { (valKey, label) ->
                                FilterChip(
                                    selected = firstDayOfWeek == valKey,
                                    onClick = { viewModel.updateFirstDayOfWeek(valKey) },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Default Task Duration", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 30, 45, 60, 90).forEach { mins ->
                                FilterChip(
                                    selected = defaultDuration == mins,
                                    onClick = { viewModel.updateDefaultTaskDuration(mins) },
                                    label = { Text("$mins min") }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    val newDur = (defaultDuration - 5).coerceAtLeast(5)
                                    viewModel.updateDefaultTaskDuration(newDur)
                                },
                                enabled = defaultDuration > 5
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease Duration")
                            }

                            var customDurationText by remember(defaultDuration) { mutableStateOf(defaultDuration.toString()) }
                            OutlinedTextField(
                                value = customDurationText,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(3)
                                    customDurationText = digits
                                    val d = digits.toIntOrNull()
                                    if (d != null && d in 1..480) {
                                        viewModel.updateDefaultTaskDuration(d)
                                    }
                                },
                                label = { Text("Duration (1-480 min)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )

                            FilledTonalIconButton(
                                onClick = {
                                    val newDur = (defaultDuration + 5).coerceAtMost(480)
                                    viewModel.updateDefaultTaskDuration(newDur)
                                },
                                enabled = defaultDuration < 480
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase Duration")
                            }
                        }
                    }
                }
            }
        }

        SettingsSubpage.APPEARANCE -> {
            SettingsSubpageScaffold(
                title = "Appearance & Themes",
                onBack = { currentSubpage = SettingsSubpage.HUB }
            ) {
                // Theme Mode Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Dark / Light Mode", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (mode, label) ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = { viewModel.updateThemeMode(mode) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Theme & Gallery Button
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Active Theme", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    text = activeThemeDef.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Button(onClick = onNavigateToThemeGallery) {
                                Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Theme Gallery")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // QR Shortcut Tile Shapes (Global)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Quick Reminders (QR) Shape", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = "Choose the shape applied across all Quick Reminder tiles",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                com.example.domain.model.DashboardShape.HEXAGON to "⬡ Hexagon",
                                com.example.domain.model.DashboardShape.CIRCLE to "● Circle",
                                com.example.domain.model.DashboardShape.SQUARE to "■ Square"
                            ).forEach { (shape, label) ->
                                FilterChip(
                                    selected = appearance.shape == shape,
                                    onClick = { viewModel.setGlobalQrShape(shape) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Honeycomb Visual Styling
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Dashboard Layout & Density", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        val isCompact = appearance.hexagonSizeDp <= 64
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("DEFAULT" to "Normal", "COMPACT" to "Compact").forEach { (dens, label) ->
                                FilterChip(
                                    selected = if (dens == "COMPACT") isCompact else !isCompact,
                                    onClick = { viewModel.setHoneycombSize(if (dens == "COMPACT") 60 else 74) },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Completion Confetti Animation", fontSize = 14.sp)
                            Switch(
                                checked = completionAnimation,
                                onCheckedChange = { viewModel.setCompletionAnimationEnabled(it) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Animation Speed Customization (#23)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Loading Animation Speed", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    text = "Adjust speed multiplier for spinners, transitions & atmosphere",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Live Preview Indicator
                            ThemedLoadingIndicator(size = 32.dp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                AnimationSpeed.OFF to "Off (0×)",
                                AnimationSpeed.SLOW to "0.5× Slow",
                                AnimationSpeed.NORMAL to "1.0× Normal",
                                AnimationSpeed.FAST to "1.5× Fast",
                                AnimationSpeed.VERY_FAST to "2.0× Swift"
                            ).forEach { (speedTier, label) ->
                                FilterChip(
                                    selected = animationSpeed == speedTier,
                                    onClick = { viewModel.setAnimationSpeed(speedTier) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Independent Light / Dark Theme Customization (#4)
                var activePaletteModeIsLight by remember { mutableStateOf(themeMode == "LIGHT") }
                val currentPalette = independentThemeCustomization.getActivePalette(activePaletteModeIsLight)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Custom Theme Palettes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    text = "Override primary, card, and background colors independently",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = customColorsEnabled,
                                onCheckedChange = { viewModel.setCustomColorsEnabled(it) }
                            )
                        }

                        if (customColorsEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Mode Selector Tabs (Light / Dark)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = !activePaletteModeIsLight,
                                    onClick = { activePaletteModeIsLight = false },
                                    label = { Text("🌙 Dark Mode") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = activePaletteModeIsLight,
                                    onClick = { activePaletteModeIsLight = true },
                                    label = { Text("☀️ Light Mode") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Live Mini Palette Preview
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = currentPalette.getSurfaceColor(MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (activePaletteModeIsLight) "Light Palette Live Preview" else "Dark Palette Live Preview",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = currentPalette.getPrimaryColor(MaterialTheme.colorScheme.primary)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = currentPalette.getAccentColor(MaterialTheme.colorScheme.tertiary)
                                        ) {
                                            Text(
                                                text = "Accent",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = currentPalette.getCardColor(MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Card Surface Token Preview",
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Slot Customizers (Primary, Accent, Card, Surface, Background)
                            val presetColors = listOf(
                                0xFF3B82F6L to "Blue",
                                0xFF8B5CF6L to "Purple",
                                0xFFEC4899L to "Pink",
                                0xFF10B981L to "Emerald",
                                0xFFF59E0BL to "Amber",
                                0xFFEF4444L to "Red",
                                0xFF06B6D4L to "Cyan",
                                0xFF1E293BL to "Slate",
                                0xFF0F172AL to "Dark Slate",
                                0xFFFFFFFFL to "White"
                            )

                            listOf(
                                Triple("primary", "Primary Brand", currentPalette.primaryHex),
                                Triple("accent", "Accent Highlight", currentPalette.accentHex),
                                Triple("card", "Card Surface", currentPalette.cardHex),
                                Triple("surface", "App Surface", currentPalette.surfaceHex),
                                Triple("background", "Atmosphere Background", currentPalette.backgroundHex)
                            ).forEach { (slot, slotTitle, currentHex) ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(slotTitle, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        if (currentHex != null) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .background(Color(currentHex), CircleShape)
                                                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Reset",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.clickable {
                                                        viewModel.updateCustomColor(activePaletteModeIsLight, slot, null)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        presetColors.forEach { (colorLong, colorLabel) ->
                                            val isSelected = currentHex == colorLong
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(Color(colorLong), CircleShape)
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        viewModel.updateCustomColor(activePaletteModeIsLight, slot, colorLong)
                                                    }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { viewModel.resetCustomColors(activePaletteModeIsLight) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Reset ${if (activePaletteModeIsLight) "Light" else "Dark"} Palette to Defaults", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        SettingsSubpage.NOTIFICATIONS -> {
            val soundLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) {
                    viewModel.importCustomSound(uri)
                }
            }

            SettingsSubpageScaffold(
                title = "Notifications & Smart Alerts",
                onBack = { currentSubpage = SettingsSubpage.HUB }
            ) {
                // Task Completion Sounds Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Task Completion Sound", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Audio effect played on completing tasks & habits", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = completionSound,
                                onCheckedChange = { viewModel.setCompletionSoundEnabled(it) }
                            )
                        }

                        if (completionSound) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Built-in Sound Profiles", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            val availableSounds = listOf(
                                FeedbackSoundType.BELL to "🔔 Tibetan Bell",
                                FeedbackSoundType.POP to "🫧 Crisp Pop",
                                FeedbackSoundType.RETRO to "👾 8-Bit Retro",
                                FeedbackSoundType.SUCCESS to "🏆 Victory Marimba",
                                FeedbackSoundType.HARP to "🪕 Celestial Harp",
                                FeedbackSoundType.TASK to "🎵 Default Chime",
                                FeedbackSoundType.HABIT to "🧘 Habit Bell",
                                FeedbackSoundType.WATER to "💧 Water Drop",
                                FeedbackSoundType.MILESTONE to "🌟 Milestone",
                                FeedbackSoundType.ALL_DONE to "🎉 All Done"
                            )

                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                availableSounds.forEach { (sndType, label) ->
                                    val isSelected = completionSoundType == sndType
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.setCompletionSoundType(sndType)
                                            viewModel.playPreviewSound(sndType)
                                        },
                                        label = { Text(label, fontSize = 12.sp) },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { viewModel.playPreviewSound(sndType) },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Preview $label",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Custom Completion Sound", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))

                            if (customSoundUri != null) {
                                val isCustomActive = completionSoundType == FeedbackSoundType.CUSTOM
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCustomActive)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        else
                                            MaterialTheme.colorScheme.surface
                                    ),
                                    border = if (isCustomActive)
                                        androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                    else
                                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isCustomActive)
                                                            MaterialTheme.colorScheme.primary
                                                        else
                                                            MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                            ) {
                                                Icon(
                                                    Icons.Default.MusicNote,
                                                    contentDescription = null,
                                                    tint = if (isCustomActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = customSoundFileName ?: "Custom Sound",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (isCustomActive) "Currently Active Completion Sound" else "Imported Audio File",
                                                    fontSize = 11.sp,
                                                    color = if (isCustomActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.playPreviewSound(FeedbackSoundType.CUSTOM) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = "Preview custom sound",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (!isCustomActive) {
                                                Button(
                                                    onClick = { viewModel.setCompletionSoundType(FeedbackSoundType.CUSTOM) },
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Use as Sound", fontSize = 11.sp)
                                                }
                                            }
                                            OutlinedButton(
                                                onClick = { soundLauncher.launch("audio/*") },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Change", fontSize = 11.sp)
                                            }
                                            OutlinedButton(
                                                onClick = { viewModel.removeCustomSound() },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { soundLauncher.launch("audio/*") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Import Custom Audio File (.mp3, .wav, .ogg)")
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Select any audio file from your device storage to use as your completion chime.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                // Master Permission Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Allow LifeOS Notifications", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("All reminders are 100% scheduled on-device via AlarmManager", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { isEnabled ->
                                    viewModel.toggleNotifications(isEnabled)
                                    if (isEnabled && !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // P1 Smart Alarms Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Priority 1 (P1) Smart Alarms", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Loud persistent alarm sound & full-screen notification for urgent tasks", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = p1AlarmEnabled,
                                onCheckedChange = { viewModel.setP1AlarmEnabled(it) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notification Channels Breakdown
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Active Alert Categories", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        NotificationToggleRow("Medication & Health", notifMedicine) { viewModel.setNotifCategoryEnabled("MEDICINE", it) }
                        NotificationToggleRow("Task Reminders", notifTasks) { viewModel.setNotifCategoryEnabled("TASKS", it) }
                        NotificationToggleRow("Habit Reminders", notifHabits) { viewModel.setNotifCategoryEnabled("HABITS", it) }
                        NotificationToggleRow("Hydration Alerts", notifHydration) { viewModel.setNotifCategoryEnabled("HYDRATION", it) }
                        NotificationToggleRow("Calendar Schedule", notifCalendar) { viewModel.setNotifCategoryEnabled("CALENDAR", it) }
                        NotificationToggleRow("Quick Reminders (QR)", notifQr) { viewModel.setNotifCategoryEnabled("QR", it) }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quiet Hours Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Quiet Hours Schedule", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Mute non-urgent notifications during sleep ($quietHoursStart - $quietHoursEnd)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(onClick = {
                            tempQuietStart = quietHoursStart
                            tempQuietEnd = quietHoursEnd
                            showScheduleDialog = true
                        }) {
                            Text("Adjust Quiet Hours")
                        }
                    }
                }
            }
        }

        SettingsSubpage.ORGANIZATION -> {
            SettingsSubpageScaffold(
                title = "Organization & Essentials",
                onBack = { currentSubpage = SettingsSubpage.HUB }
            ) {
                // Projects & Labels entry card
                SettingsNavigationCard(
                    icon = Icons.Outlined.Folder,
                    title = "Projects & Labels Manager",
                    subtitle = "Manage personal and work projects, custom color tags, and task associations.",
                    onClick = { currentSubpage = SettingsSubpage.PROJECTS_LABELS }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Trackers & Subscriptions entry card
                SettingsNavigationCard(
                    icon = Icons.Default.Speed,
                    title = "Trackers & Subscriptions (My Essentials)",
                    subtitle = "Smart odometers, vehicle oil, water filters, recurring subscriptions, and free/pro exports.",
                    onClick = { currentSubpage = SettingsSubpage.TRACKERS_SUBSCRIPTIONS }
                )
            }
        }

        SettingsSubpage.PRODUCTIVITY -> {
            SettingsSubpageScaffold(
                title = "Productivity & Schedule",
                onBack = { currentSubpage = SettingsSubpage.HUB }
            ) {
                // Daily Schedule Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Daily Sleep & Wake Schedule", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Wake: $wakeUpTime  •  Bedtime: $bedTime", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(onClick = {
                            tempWakeUp = wakeUpTime
                            tempBedTime = bedTime
                            showScheduleDialog = true
                        }) {
                            Text("Edit Daily Schedule")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hydration Goal
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        var hydrationUnit by remember { mutableStateOf("ml") } // "ml", "L", "oz"
                        var customGoalText by remember(waterGoalMl, hydrationUnit) {
                            mutableStateOf(
                                when (hydrationUnit) {
                                    "L" -> String.format(java.util.Locale.US, "%.2f", waterGoalMl / 1000f)
                                    "oz" -> String.format(java.util.Locale.US, "%.1f", waterGoalMl / 29.5735f)
                                    else -> waterGoalMl.toString()
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Daily Hydration Goal", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            val displayGoal = when (hydrationUnit) {
                                "L" -> String.format(java.util.Locale.US, "%.2f L (%d ml)", waterGoalMl / 1000f, waterGoalMl)
                                "oz" -> String.format(java.util.Locale.US, "%.1f oz (%d ml)", waterGoalMl / 29.5735f, waterGoalMl)
                                else -> "$waterGoalMl ml"
                            }
                            Text(displayGoal, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Unit Selector Chips
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ml" to "ml", "L" to "Liters", "oz" to "fl oz").forEach { (unitKey, unitLabel) ->
                                FilterChip(
                                    selected = hydrationUnit == unitKey,
                                    onClick = { hydrationUnit = unitKey },
                                    label = { Text(unitLabel) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = waterGoalMl.toFloat().coerceIn(1000f, 5000f),
                            onValueChange = { viewModel.updateWaterGoal(it.toInt()) },
                            valueRange = 1000f..5000f,
                            steps = 15
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customGoalText,
                            onValueChange = { input ->
                                customGoalText = input
                                when (hydrationUnit) {
                                    "L" -> {
                                        val l = input.toDoubleOrNull()
                                        if (l != null && l in 0.5..10.0) {
                                            viewModel.updateWaterGoal((l * 1000).toInt())
                                        }
                                    }
                                    "oz" -> {
                                        val oz = input.toDoubleOrNull()
                                        if (oz != null && oz in 16.0..350.0) {
                                            viewModel.updateWaterGoal((oz * 29.5735f).toInt())
                                        }
                                    }
                                    else -> {
                                        val ml = input.filter { it.isDigit() }.toIntOrNull()
                                        if (ml != null && ml in 500..10000) {
                                            viewModel.updateWaterGoal(ml)
                                        }
                                    }
                                }
                            },
                            label = { Text("Custom Goal ($hydrationUnit, allows decimal)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Daily Planning Shortcut
                Button(
                    onClick = onNavigateToPlanning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Daily Planning Wizard")
                }
            }
        }

        SettingsSubpage.GAMIFICATION -> {
            SettingsSubpageScaffold(
                title = "Gamification & Progress",
                onBack = { currentSubpage = SettingsSubpage.HUB }
            ) {
                // Profile Overview Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Level ${gamificationProfile.currentLevel}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                                Text("${gamificationProfile.currentXp} Total XP Points", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = onNavigateToGamification) {
                                Text("Gamification Hub")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Toggles
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Gamification Audio Cues", fontSize = 14.sp)
                            Switch(checked = gamificationSound, onCheckedChange = { viewModel.setGamificationSound(it) })
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Gamification Haptics", fontSize = 14.sp)
                            Switch(checked = gamificationVibration, onCheckedChange = { viewModel.setGamificationVibration(it) })
                        }
                    }
                }
            }
        }

        SettingsSubpage.DATA_STORAGE -> {
            SettingsSubpageScaffold(
                title = "Data & Storage",
                onBack = { currentSubpage = SettingsSubpage.HUB }
            ) {
                // Backup & Restore
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Encrypted Backup & Restore", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Export an offline JSON backup or restore an existing backup.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { showBackupRestoreDialog = true }) {
                            Text("Open Backup & Restore")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Trash Bin
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Trash Bin Recovery", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Restore accidentally deleted tasks or permanently purge old items.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Auto-delete retention period", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        // Preset Pills: 1w, 2w, 1mo, 2mo, 3mo, 1y
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            listOf(
                                7 to "1 Week",
                                14 to "2 Weeks",
                                30 to "1 Month",
                                60 to "2 Months",
                                90 to "3 Months",
                                365 to "1 Year"
                            ).forEach { (days, label) ->
                                FilterChip(
                                    selected = trashRetentionDays == days,
                                    onClick = { viewModel.setTrashRetentionDays(days) },
                                    label = { Text(label) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        var retentionUnit by remember {
                            mutableStateOf(
                                when {
                                    trashRetentionDays >= 365 && trashRetentionDays % 365 == 0 -> "Years"
                                    trashRetentionDays >= 30 && trashRetentionDays % 30 == 0 -> "Months"
                                    trashRetentionDays >= 7 && trashRetentionDays % 7 == 0 -> "Weeks"
                                    else -> "Days"
                                }
                            )
                        }
                        val multiplier = when (retentionUnit) {
                            "Years" -> 365
                            "Months" -> 30
                            "Weeks" -> 7
                            else -> 1
                        }
                        val displayedAmount = (trashRetentionDays / multiplier).coerceAtLeast(1)
                        var retentionAmountText by remember(trashRetentionDays, retentionUnit) {
                            mutableStateOf(displayedAmount.toString())
                        }
                        var showUnitDropdown by remember { mutableStateOf(false) }

                        // Duration Stepper & Numeric Input + Unit Dropdown
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    val currentAmt = retentionAmountText.toIntOrNull() ?: displayedAmount
                                    val newAmt = (currentAmt - 1).coerceAtLeast(1)
                                    val newDays = (newAmt * multiplier).coerceIn(1, 365)
                                    viewModel.setTrashRetentionDays(newDays)
                                }
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease Retention")
                            }

                            OutlinedTextField(
                                value = retentionAmountText,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(3)
                                    retentionAmountText = digits
                                    val amt = digits.toIntOrNull()
                                    if (amt != null && amt > 0) {
                                        val days = (amt * multiplier).coerceIn(1, 365)
                                        viewModel.setTrashRetentionDays(days)
                                    }
                                },
                                label = { Text("Period") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )

                            Box {
                                OutlinedButton(onClick = { showUnitDropdown = true }) {
                                    Text(retentionUnit)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = showUnitDropdown,
                                    onDismissRequest = { showUnitDropdown = false }
                                ) {
                                    listOf("Days", "Weeks", "Months", "Years").forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text(unit) },
                                            onClick = {
                                                retentionUnit = unit
                                                showUnitDropdown = false
                                                val amt = retentionAmountText.toIntOrNull() ?: 1
                                                val newMult = when (unit) {
                                                    "Years" -> 365
                                                    "Months" -> 30
                                                    "Weeks" -> 7
                                                    else -> 1
                                                }
                                                val newDays = (amt * newMult).coerceIn(1, 365)
                                                viewModel.setTrashRetentionDays(newDays)
                                            }
                                        )
                                    }
                                }
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    val currentAmt = retentionAmountText.toIntOrNull() ?: displayedAmount
                                    val newAmt = currentAmt + 1
                                    val newDays = (newAmt * multiplier).coerceIn(1, 365)
                                    viewModel.setTrashRetentionDays(newDays)
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase Retention")
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { showTrashBinSheet = true }) {
                            Text("View Trash Bin")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Default Starter Templates
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Starter Productivity Templates", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Browse curated task templates and morning/evening routines.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showTemplateBrowserDialog = true }) {
                                Text("Browse Catalog")
                            }
                            if (!hasInstalledDefaults) {
                                OutlinedButton(onClick = { showInstallConfirmDialog = true }) {
                                    Text("Install Defaults")
                                }
                            }
                        }
                    }
                }
            }
        }

        SettingsSubpage.LEGAL_PRIVACY -> {
            SettingsSubpageScaffold(
                title = "Legal & Privacy",
                onBack = { currentSubpage = SettingsSubpage.HUB }
            ) {
                // Privacy Pledge Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("100% Offline & Private", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "LifeOS contains ZERO trackers, ZERO analytics, and ZERO internet permissions. Your data never leaves your device.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Documents Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Legal Agreements", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewingLegalDoc = "TERMS" }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Terms & Conditions", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Version: $acceptedTermsVersion", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewingLegalDoc = "PRIVACY" }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Policy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Privacy Policy", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Version: $acceptedPrivacyVersion", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        else -> {}
    }

    // --- Global Dialogs ---

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit User Name") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.updateUserName(newName.trim())
                    }
                    showEditNameDialog = false
                }) {
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

    if (showScheduleDialog) {
        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            title = { Text("Configure Times") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tempWakeUp,
                        onValueChange = { tempWakeUp = it },
                        label = { Text("Wake-up Time (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempBedTime,
                        onValueChange = { tempBedTime = it },
                        label = { Text("Bedtime (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempQuietStart,
                        onValueChange = { tempQuietStart = it },
                        label = { Text("Quiet Hours Start") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempQuietEnd,
                        onValueChange = { tempQuietEnd = it },
                        label = { Text("Quiet Hours End") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateWakeUpTime(tempWakeUp.trim())
                    viewModel.updateBedTime(tempBedTime.trim())
                    viewModel.updateQuietHours(tempQuietStart.trim(), tempQuietEnd.trim())
                    showScheduleDialog = false
                }) {
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

    if (showProfileEmojiDialog) {
        EmojiInputDialog(
            currentEmoji = userProfileEmoji,
            onEmojiSelected = { emoji ->
                viewModel.updateUserProfileEmoji(emoji)
                showProfileEmojiDialog = false
            },
            onDismiss = { showProfileEmojiDialog = false }
        )
    }

    if (showBackupRestoreDialog) {
        BackupRestoreDialog(viewModel = viewModel, onDismiss = { showBackupRestoreDialog = false })
    }

    if (showTrashBinSheet) {
        TrashBinBottomSheet(viewModel = viewModel, onDismiss = { showTrashBinSheet = false })
    }

    if (showTemplateBrowserDialog) {
        TemplateBrowserDialog(
            installedTemplateIds = installedTemplates,
            onInstallTemplate = { template, habitIndices, taskIndices, onComplete ->
                viewModel.installTemplate(template, habitIndices, taskIndices, onComplete)
            },
            onDismiss = { showTemplateBrowserDialog = false }
        )
    }

    if (showInstallConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showInstallConfirmDialog = false },
            title = { Text("Install Default Productivity Templates?") },
            text = { Text("This will add preconfigured habits and starter projects.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.installDefaultRoutines()
                    showInstallConfirmDialog = false
                }) {
                    Text("Install")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    viewingLegalDoc?.let { docType ->
        LegalDocumentViewerDialog(
            title = if (docType == "TERMS") LegalDocuments.TERMS_OF_SERVICE_TITLE else LegalDocuments.PRIVACY_POLICY_TITLE,
            content = if (docType == "TERMS") LegalDocuments.TERMS_OF_SERVICE_CONTENT else LegalDocuments.PRIVACY_POLICY_CONTENT,
            onDismiss = { viewingLegalDoc = null }
        )
    }
}

@Composable
private fun SettingsHubView(
    userName: String,
    themeMode: String,
    activeThemeName: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToSubpage: (SettingsSubpage) -> Unit,
    onNavigateToGamificationHub: () -> Unit,
    onNavigateToThemeGallery: () -> Unit = {},
    onNavigateToPlanning: () -> Unit = {},
    onNavigateToMedication: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val searchRegistry = remember {
        getSettingsSearchRegistry(
            onNavigateToThemeGallery = onNavigateToThemeGallery,
            onNavigateToPlanning = onNavigateToPlanning,
            onNavigateToGamification = onNavigateToGamificationHub,
            onNavigateToMedication = onNavigateToMedication
        )
    }

    val searchResults = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim().lowercase()
            searchRegistry.filter { item ->
                item.title.lowercase().contains(q) ||
                item.subtitle.lowercase().contains(q) ||
                item.category.lowercase().contains(q) ||
                item.keywords.any { it.lowercase().contains(q) }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("settings_hub")
    ) {
        // App Title & Tagline
        Column {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Personalize and manage your offline productivity system",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SET-01: Settings Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search all settings (e.g. theme, alarm, trash, templates)...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Settings") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear Search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_search_bar")
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (searchQuery.isNotBlank()) {
            Text(
                text = "Search Results (${searchResults.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (searchResults.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔍", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No settings found for \"$searchQuery\"",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try searching for keywords like theme, clock, alarm, trash, projects, templates, or backup.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    searchResults.forEach { result ->
                        SettingsSearchResultCard(
                            item = result,
                            onClick = {
                                if (result.onDirectAction != null) {
                                    result.onDirectAction.invoke()
                                } else {
                                    onNavigateToSubpage(result.subpage)
                                }
                            }
                        )
                    }
                }
            }
        } else {
            // HUB ENTRY CARDS
            SettingsNavigationCard(
                icon = Icons.Default.Settings,
                title = "General",
                subtitle = "Profile name, clock format (12h/24h), week start, duration defaults",
                onClick = { onNavigateToSubpage(SettingsSubpage.GENERAL) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsNavigationCard(
                icon = Icons.Default.Palette,
                title = "Appearance & Themes",
                subtitle = "Active Theme: $activeThemeName • $themeMode mode • Honeycomb density",
                onClick = { onNavigateToSubpage(SettingsSubpage.APPEARANCE) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsNavigationCard(
                icon = Icons.Default.Notifications,
                title = "Notifications & Smart Alerts",
                subtitle = "P1 Smart Alarms, medication alerts, channels, quiet hours schedule",
                onClick = { onNavigateToSubpage(SettingsSubpage.NOTIFICATIONS) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsNavigationCard(
                icon = Icons.Outlined.Folder,
                title = "Organization & Essentials",
                subtitle = "Projects & Labels Manager • Smart Trackers & Subscriptions (My Essentials)",
                onClick = { onNavigateToSubpage(SettingsSubpage.ORGANIZATION) },
                highlighted = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsNavigationCard(
                icon = Icons.Default.Speed,
                title = "Productivity & Schedule",
                subtitle = "Daily wake & bedtime schedule, hydration goal, daily planning wizard",
                onClick = { onNavigateToSubpage(SettingsSubpage.PRODUCTIVITY) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsNavigationCard(
                icon = Icons.Default.EmojiEvents,
                title = "Gamification & Progress",
                subtitle = "Levels, XP points, sound & haptic rewards, gamification hub",
                onClick = { onNavigateToSubpage(SettingsSubpage.GAMIFICATION) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsNavigationCard(
                icon = Icons.Default.Storage,
                title = "Data & Storage",
                subtitle = "Encrypted backup & restore, trash bin recovery, template browser",
                onClick = { onNavigateToSubpage(SettingsSubpage.DATA_STORAGE) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsNavigationCard(
                icon = Icons.Default.Security,
                title = "Legal & Privacy",
                subtitle = "Terms & Conditions, Privacy Policy, 100% offline-first pledge",
                onClick = { onNavigateToSubpage(SettingsSubpage.LEGAL_PRIVACY) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsNavigationCard(
                icon = Icons.Default.MenuBook,
                title = "User Manual & UI Components",
                subtitle = "Feature documentation and interactive Reusable Frontend UI Components library (36 components)",
                onClick = { onNavigateToSubpage(SettingsSubpage.USER_MANUAL) },
                highlighted = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // About LifeOS Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("About LifeOS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Version 1.1.0 (Build 2) • Production Release", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text("Zero cloud dependencies. Zero telemetry. Privacy-first offline architecture.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsSearchResultCard(
    item: SettingsSearchItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("settings_search_result_${item.title}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = item.category.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsGridNavigationCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("nav_grid_card_")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsNavigationCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("nav_card_${title.lowercase().replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSubpageScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("subpage_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            content()
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
