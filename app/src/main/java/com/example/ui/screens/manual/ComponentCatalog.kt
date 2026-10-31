package com.example.ui.screens.manual

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Storage

enum class ComponentCategory(val displayName: String, val icon: ImageVector) {
    ALL("All Categories", Icons.Default.Layers),
    DATE_TIME("Date & Time", Icons.Default.AccessTime),
    INPUTS_FORMS("Inputs & Forms", Icons.Default.HourglassTop),
    SELECTION_CONTROLS("Selection Controls", Icons.Default.FilterList),
    VISUAL_STYLING("Visual Styling", Icons.Default.Palette),
    TASK_PRODUCTIVITY("Task & Productivity", Icons.Default.CheckCircle),
    NAVIGATION_LAYOUT("Navigation & Layout", Icons.Default.GridView),
    FEEDBACK_DIALOGS("Feedback & Dialogs", Icons.Default.Warning),
    SPECIALIZED_LIFEOS("Specialized LifeOS", Icons.Default.AutoAwesome)
}

enum class ComponentStatus(val label: String) {
    EXISTING("EXISTING"),
    PARTIAL("PARTIAL"),
    PROPOSED("PROPOSED")
}

data class ComponentParam(
    val name: String,
    val type: String,
    val description: String
)

data class ComponentEntry(
    val id: String,
    val name: String,
    val category: ComponentCategory,
    val status: ComponentStatus,
    val description: String,
    val whenToUse: String,
    val sourceFile: String,
    val composableName: String,
    val parameters: List<ComponentParam>,
    val usageLocations: List<String>,
    val reuseRecommendations: String,
    val codeExample: String,
    val tags: List<String>
)

object MasterComponentCatalog {

    val items: List<ComponentEntry> = listOf(
        // 1. Clock / Time Input
        ComponentEntry(
            id = "clock_time_input",
            name = "Unified Clock / Time Input",
            category = ComponentCategory.DATE_TIME,
            status = ComponentStatus.EXISTING,
            description = "Material 3 analog clock picker and numeric keypad input with 12h/24h system format detection, AM/PM handling, and quick presets (Morning, Noon, Evening, Night).",
            whenToUse = "Use whenever the user needs to select or edit a time of day, such as task due times, habit reminder triggers, or medication doses.",
            sourceFile = "app/src/main/java/com/example/ui/components/LifeTimePicker.kt",
            composableName = "LifeTimePickerField & LifeTimePickerDialog",
            parameters = listOf(
                ComponentParam("selectedTime", "String?", "Time string in HH:mm 24-hour format or null"),
                ComponentParam("onTimeSelected", "(String) -> Unit", "Callback invoked when user confirms a time"),
                ComponentParam("label", "String", "Label displayed above the field (default: 'Due Time')"),
                ComponentParam("isNullable", "Boolean", "Whether the time field allows clearing (default: true)"),
                ComponentParam("onClear", "(() -> Unit)?", "Optional callback when clear icon is tapped")
            ),
            usageLocations = listOf("TaskAdvancedEditorSheet", "MedicineReminderSection", "CustomReminderDialog", "HabitsScreen"),
            reuseRecommendations = "Consolidate custom time buttons in HabitEditor and Settings Quiet Hours into LifeTimePickerField to maintain single 12h/24h formatting logic.",
            codeExample = """
var dueTime by remember { mutableStateOf("09:00") }

LifeTimePickerField(
    selectedTime = dueTime,
    onTimeSelected = { dueTime = it },
    label = "Reminder Time",
    onClear = { dueTime = null }
)
            """.trimIndent(),
            tags = listOf("time", "clock", "picker", "12h", "24h", "am", "pm", "presets")
        ),

        // 2. Standardized Date Trigger Field
        ComponentEntry(
            id = "date_trigger_field",
            name = "Standardized Date Trigger Field",
            category = ComponentCategory.DATE_TIME,
            status = ComponentStatus.EXISTING,
            description = "Click-to-pick trigger field integrating Material 3 DatePicker with Today, Tomorrow, +3 Days, and Next Week presets, min/max constraints, and ISO-8601 formatting.",
            whenToUse = "Use for any date selection workflow, including task due dates, challenge start dates, bill due dates, and inventory cycle start dates.",
            sourceFile = "app/src/main/java/com/example/ui/components/LifeDatePicker.kt",
            composableName = "LifeDateField & LifeDateRangeField",
            parameters = listOf(
                ComponentParam("selectedDateIso", "String", "Current ISO-8601 date string (yyyy-MM-dd)"),
                ComponentParam("onDateSelected", "(String) -> Unit", "Callback invoked when a new date is confirmed"),
                ComponentParam("label", "String", "Display label above the field"),
                ComponentParam("minDateIso", "String?", "Optional lower constraint (earlier dates disabled)"),
                ComponentParam("maxDateIso", "String?", "Optional upper constraint (later dates disabled)"),
                ComponentParam("isNullable", "Boolean", "Whether date can be cleared back to blank")
            ),
            usageLocations = listOf("TaskAdvancedEditorSheet", "TaskQuickAddSheet", "ChallengeScreen", "SubscriptionEditorSheet", "TrackerEditorSheet"),
            reuseRecommendations = "Replace boilerplate rememberDatePickerState and DatePickerDialog in ChallengeScreen and TrackerEditorSheet with LifeDatePickerField.",
            codeExample = """
var dueDateIso by remember { mutableStateOf(DateUtils.todayString()) }

LifeDatePickerField(
    selectedDateIso = dueDateIso,
    onDateSelected = { dueDateIso = it },
    label = "Due Date",
    minDateIso = DateUtils.todayString()
)
            """.trimIndent(),
            tags = listOf("date", "picker", "calendar", "presets", "iso", "today", "tomorrow")
        ),

        // 3. Unified Emoji & Icon Selector
        ComponentEntry(
            id = "emoji_icon_selector",
            name = "Unified Emoji & Icon Selector",
            category = ComponentCategory.INPUTS_FORMS,
            status = ComponentStatus.EXISTING,
            description = "Searchable emoji picker dialog with category sections (Smileys, Food, Activities, Objects, Symbols), single-emoji enforcement, and preview badge.",
            whenToUse = "Use for assigning visual identity emojis to tasks, habits, projects, labels, trackers, and Quick Reminder honeycomb shortcuts.",
            sourceFile = "app/src/main/java/com/example/ui/components/EmojiInputDialog.kt",
            composableName = "LifeEmojiPickerRow & LifeEmojiField",
            parameters = listOf(
                ComponentParam("initialEmoji", "String", "Existing single emoji string"),
                ComponentParam("onEmojiSelected", "(String) -> Unit", "Callback with validated single emoji"),
                ComponentParam("onDismiss", "() -> Unit", "Callback when dialog is cancelled")
            ),
            usageLocations = listOf("TaskAdvancedEditorSheet", "EditProjectBottomSheet", "EditLabelBottomSheet", "HabitsScreen", "HoneycombTaskGrid"),
            reuseRecommendations = "Standardize the emoji trigger button across TaskQuickAddSheet and TrackerEditorSheet to use a unified preview chip that launches EmojiInputDialog.",
            codeExample = """
var selectedEmoji by remember { mutableStateOf("⚡") }
var showEmojiDialog by remember { mutableStateOf(false) }

// Trigger button
FilledTonalButton(onClick = { showEmojiDialog = true }) {
    Text(selectedEmoji.ifBlank { "Pick Emoji" })
}

if (showEmojiDialog) {
    EmojiInputDialog(
        initialEmoji = selectedEmoji,
        onEmojiSelected = { 
            selectedEmoji = it
            showEmojiDialog = false 
        },
        onDismiss = { showEmojiDialog = false }
    )
}
            """.trimIndent(),
            tags = listOf("emoji", "icon", "symbols", "picker", "avatar", "graphic")
        ),

        // 4. Duration Stepper & Preset Input
        ComponentEntry(
            id = "duration_stepper_input",
            name = "Duration Stepper & Preset Input",
            category = ComponentCategory.INPUTS_FORMS,
            status = ComponentStatus.EXISTING,
            description = "Custom duration input with numeric stepper (+/- buttons), quick preset chips (15m, 30m, 45m, 60m), unit dropdown (Minutes, Hours, Days, Weeks), and bounds validation.",
            whenToUse = "Use whenever defining expected task durations, focus block allocations, pomodoro timers, or subscription billing cycles.",
            sourceFile = "app/src/main/java/com/example/ui/components/DurationInput.kt",
            composableName = "LifeDurationPicker & DurationInput",
            parameters = listOf(
                ComponentParam("durationAmount", "Int", "Current numeric amount (e.g. 30)"),
                ComponentParam("durationUnit", "DurationUnit", "Unit: MINUTES, HOURS, DAYS, WEEKS, MONTHS, YEARS"),
                ComponentParam("onDurationAmountChanged", "(Int) -> Unit", "Callback when numeric value changes"),
                ComponentParam("onDurationUnitChanged", "(DurationUnit) -> Unit", "Callback when unit changes"),
                ComponentParam("maxAmount", "Int", "Maximum allowed value (default: 480)")
            ),
            usageLocations = listOf("TaskAdvancedEditorSheet", "SettingsScreen (Default Duration)", "TasksScreen"),
            reuseRecommendations = "Directly reuse DurationInput in habit frequency configuration and tracker usage cycle estimation.",
            codeExample = """
var amount by remember { mutableIntStateOf(30) }
var unit by remember { mutableStateOf(DurationUnit.MINUTES) }

DurationInput(
    durationAmount = amount,
    durationUnit = unit,
    onDurationAmountChanged = { amount = it },
    onDurationUnitChanged = { unit = it }
)
            """.trimIndent(),
            tags = listOf("duration", "stepper", "minutes", "hours", "timer", "presets")
        ),

        // 5. Type-Safe Dropdown Selector
        ComponentEntry(
            id = "type_safe_dropdown",
            name = "Type-Safe Dropdown Selector",
            category = ComponentCategory.SELECTION_CONTROLS,
            status = ComponentStatus.EXISTING,
            description = "Generic reusable dropdown component supporting enum and strongly typed collections with leading icons, selection checkmarks, error borders, and Material 3 menu anchor styling.",
            whenToUse = "Use for selecting categories, recurrence modes, recurrence frequencies, units, or alert styles across editor sheets.",
            sourceFile = "app/src/main/java/com/example/ui/components/LifeDropdownField.kt",
            composableName = "LifeExposedDropdown & LifeDropdownField",
            parameters = listOf(
                ComponentParam("label", "String", "Field title label"),
                ComponentParam("items", "List<T>", "Generic list of option items"),
                ComponentParam("selectedItem", "T", "Currently selected item"),
                ComponentParam("onItemSelected", "(T) -> Unit", "Callback when an item is tapped"),
                ComponentParam("itemLabel", "(T) -> String", "Formatter lambda returning display string"),
                ComponentParam("itemIcon", "((T) -> ImageVector?)?", "Optional icon provider per item"),
                ComponentParam("errorMessage", "String?", "Optional error text for validation failures")
            ),
            usageLocations = listOf("TaskAdvancedEditorSheet", "RecurrenceEditorDialog", "SubscriptionEditorSheet", "TrackerEditorSheet"),
            reuseRecommendations = "Replace repetitive exposed dropdown menu implementations in TrackerEditorSheet with LifeDropdownField.",
            codeExample = """
val modes = listOf("Fixed Schedule", "Interval", "As-Needed")
var selectedMode by remember { mutableStateOf(modes[0]) }

LifeDropdownField(
    label = "Schedule Mode",
    items = modes,
    selectedItem = selectedMode,
    onItemSelected = { selectedMode = it }
)
            """.trimIndent(),
            tags = listOf("dropdown", "menu", "select", "enum", "type-safe", "options")
        ),

        // 6. Color Picker & Color Palette Badges
        ComponentEntry(
            id = "color_picker_palette",
            name = "Color Picker & Color Palette Badges",
            category = ComponentCategory.VISUAL_STYLING,
            status = ComponentStatus.EXISTING,
            description = "Comprehensive color selection sheet offering 20 Todoist-inspired palette swatches, usage-based frequency ordering, manual drag/order modes, and contrast badges.",
            whenToUse = "Use for assigning project colors, label tags, Quick Reminder shortcut tints, and tracker category accents.",
            sourceFile = "app/src/main/java/com/example/ui/components/TodoistColorPickerSheet.kt",
            composableName = "TodoistColorPickerSheet & ColorDot",
            parameters = listOf(
                ComponentParam("selectedColorId", "String", "Current color ID from TodoistColorPalette"),
                ComponentParam("colorOrderingMode", "ColorOrderingMode", "AUTO (frequency) or CUSTOM (manual)"),
                ComponentParam("colorUsageCounts", "Map<String, Int>", "Map of color IDs to usage count"),
                ComponentParam("onColorSelected", "(String) -> Unit", "Callback when a color swatch is selected"),
                ComponentParam("onDismiss", "() -> Unit", "Callback when bottom sheet is dismissed")
            ),
            usageLocations = listOf("EditProjectBottomSheet", "EditLabelBottomSheet", "TaskAdvancedEditorSheet", "HoneycombTaskGrid"),
            reuseRecommendations = "Use the lightweight ColorBadge composable across list rows and headers to visually reinforce project branding.",
            codeExample = """
var colorId by remember { mutableStateOf("charcoal") }
var showColorSheet by remember { mutableStateOf(false) }

if (showColorSheet) {
    TodoistColorPickerSheet(
        selectedColorId = colorId,
        onColorSelected = { 
            colorId = it
            showColorSheet = false 
        },
        onDismiss = { showColorSheet = false }
    )
}
            """.trimIndent(),
            tags = listOf("color", "palette", "badge", "todoist", "swatch", "theme")
        ),

        // 7. Priority Selector Group
        ComponentEntry(
            id = "priority_selector_group",
            name = "Priority Selector Group",
            category = ComponentCategory.SELECTION_CONTROLS,
            status = ComponentStatus.EXISTING,
            description = "Segmented chip control allowing single-selection of task priorities across P1 (Urgent/Red), P2 (High/Orange), P3 (Medium/Blue), and P4 (Low/Grey) with distinct flag iconography.",
            whenToUse = "Use in task quick add, task advanced editor, and filter bars for setting and filtering task urgency.",
            sourceFile = "app/src/main/java/com/example/ui/components/PrioritySelectorGroup.kt",
            composableName = "PrioritySelectorGroup & PriorityBadge",
            parameters = listOf(
                ComponentParam("selectedPriority", "TaskPriority", "Currently chosen priority: P1, P2, P3, P4"),
                ComponentParam("onPrioritySelected", "(TaskPriority) -> Unit", "Callback when a priority is selected"),
                ComponentParam("label", "String", "Optional header label (default: 'Priority Level')")
            ),
            usageLocations = listOf("TaskQuickAddSheet", "TaskAdvancedEditorSheet", "CommonComponents (PriorityBadge)"),
            reuseRecommendations = "Replace separate custom chip rows in TaskQuickAddSheet and TaskAdvancedEditorSheet with this unified PrioritySelectorGroup.",
            codeExample = """
var priority by remember { mutableStateOf(TaskPriority.P1) }

PrioritySelectorGroup(
    selectedPriority = priority,
    onPrioritySelected = { priority = it }
)
            """.trimIndent(),
            tags = listOf("priority", "urgent", "p1", "p2", "p3", "p4", "flag", "chips")
        ),

        // 8. Labeled Links & URL List Editor
        ComponentEntry(
            id = "labeled_links_editor",
            name = "Labeled Links & URL List Editor",
            category = ComponentCategory.INPUTS_FORMS,
            status = ComponentStatus.EXISTING,
            description = "Structured link manager allowing users to attach multiple external URLs, assign semantic presets (Registration Link, Meeting Link, Location Map, Docs), and validate URL schemes.",
            whenToUse = "Use for attaching event registration links, video call URLs, map destinations, and documentation links to tasks or subscriptions.",
            sourceFile = "app/src/main/java/com/example/ui/components/LabeledUrlEditor.kt",
            composableName = "LabeledLinksInput & LabeledUrlEditor",
            parameters = listOf(
                ComponentParam("links", "List<LabeledLinkItem>", "Collection of labeled link objects"),
                ComponentParam("onLinksChanged", "(List<LabeledLinkItem>) -> Unit", "Callback with updated link list"),
                ComponentParam("maxLinks", "Int", "Maximum links permitted (default: 5)")
            ),
            usageLocations = listOf("TaskAdvancedEditorSheet", "SubscriptionDetailScreen", "TrackerDetailScreen"),
            reuseRecommendations = "Consolidate single URL string fields in TaskItem into LabeledUrlEditor for rich multi-link support.",
            codeExample = """
var links by remember { mutableStateOf(emptyList<LabeledLinkItem>()) }

LabeledUrlEditor(
    links = links,
    onLinksChanged = { links = it },
    maxLinks = 5
)
            """.trimIndent(),
            tags = listOf("links", "url", "web", "attachment", "meeting", "registration")
        ),

        // 9. Secure Confirmation / Destructive Action Modal
        ComponentEntry(
            id = "secure_confirmation_dialog",
            name = "Secure Confirmation / Destructive Action Modal",
            category = ComponentCategory.FEEDBACK_DIALOGS,
            status = ComponentStatus.EXISTING,
            description = "Standardized confirmation modal with prominent warning iconography, explicit consequence disclosures, cancel button, and high-contrast destructive confirm button.",
            whenToUse = "Use for delete actions, challenge restarts, factory resets, bulk deletions, and irreversible database operations.",
            sourceFile = "app/src/main/java/com/example/ui/components/SecureConfirmationDialog.kt",
            composableName = "LifeSafetyDialog & SecureConfirmationDialog",
            parameters = listOf(
                ComponentParam("title", "String", "Header title of the confirmation modal"),
                ComponentParam("message", "String", "Body explanation of the intended operation"),
                ComponentParam("consequenceText", "String?", "Optional warning highlight (e.g. 'Cannot be undone')"),
                ComponentParam("confirmLabel", "String", "Text on the primary action button (default: 'Confirm')"),
                ComponentParam("isDestructive", "Boolean", "Whether to style the button in error red"),
                ComponentParam("onConfirm", "() -> Unit", "Callback executed on confirmation"),
                ComponentParam("onDismiss", "() -> Unit", "Callback executed on cancellation")
            ),
            usageLocations = listOf("SettingsScreen (Reset/Purge)", "ChallengeScreen (Restart)", "TasksScreen (Bulk Delete)", "TrashBinDialog"),
            reuseRecommendations = "Replace all unstyled AlertDialog instances across SettingsScreen and ChallengeScreen with SecureConfirmationDialog.",
            codeExample = """
var showConfirm by remember { mutableStateOf(false) }

if (showConfirm) {
    SecureConfirmationDialog(
        title = "Delete Project?",
        message = "All tasks inside this project will be moved to the trash bin.",
        consequenceText = "Items in the trash will expire based on your retention policy.",
        confirmLabel = "Delete Project",
        isDestructive = true,
        onConfirm = { 
            showConfirm = false
            /* execute deletion */ 
        },
        onDismiss = { showConfirm = false }
    )
}
            """.trimIndent(),
            tags = listOf("confirm", "dialog", "destructive", "delete", "warning", "modal")
        ),

        // 10. Search Bar & Search Field
        ComponentEntry(
            id = "search_bar_field",
            name = "Search Bar & Search Field",
            category = ComponentCategory.NAVIGATION_LAYOUT,
            status = ComponentStatus.PROPOSED,
            description = "Pill-shaped live search input with leading magnifying glass, clear button, and real-time query state synchronization.",
            whenToUse = "Use at the top of long lists such as Tasks, Settings Hub, Template Browser, and Emoji Selector.",
            sourceFile = "app/src/main/java/com/example/ui/screens/settings/SettingsScreen.kt",
            composableName = "OutlinedTextField (Search Query)",
            parameters = listOf(
                ComponentParam("query", "String", "Current search query string"),
                ComponentParam("onQueryChange", "(String) -> Unit", "Callback when text is modified"),
                ComponentParam("placeholder", "String", "Hint placeholder text")
            ),
            usageLocations = listOf("SettingsScreen", "TasksScreen", "TemplateBrowserDialog", "EmojiInputDialog"),
            reuseRecommendations = "Extract this component into a dedicated LifeSearchBar composable under com.example.ui.components to share across all 4 screens.",
            codeExample = """
var search by remember { mutableStateOf("") }
OutlinedTextField(
    value = search,
    onValueChange = { search = it },
    placeholder = { Text("Search settings or components...") },
    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
    singleLine = true,
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()
)
            """.trimIndent(),
            tags = listOf("search", "query", "filter", "find", "input")
        ),

        // 11. Filter Chips & Multi-Select Filters
        ComponentEntry(
            id = "filter_chips_row",
            name = "Filter Chips & Multi-Select Filters",
            category = ComponentCategory.SELECTION_CONTROLS,
            status = ComponentStatus.EXISTING,
            description = "Horizontal scrolling row of Material 3 FilterChips for single-select or multi-select filtering with active indicator styling.",
            whenToUse = "Use for switching views (Inbox, Today, Upcoming, Projects), filtering by project/label, or selecting audio sounds.",
            sourceFile = "app/src/main/java/com/example/ui/screens/tasks/TasksScreen.kt",
            composableName = "FilterChip (Row)",
            parameters = listOf(
                ComponentParam("options", "List<String>", "List of available filter option names"),
                ComponentParam("selectedOption", "String", "Currently selected filter"),
                ComponentParam("onSelect", "(String) -> Unit", "Callback when a chip is tapped")
            ),
            usageLocations = listOf("TasksScreen", "CalendarScreen", "SettingsScreen", "TemplateBrowserDialog"),
            reuseRecommendations = "Extract as LifeFilterChipsRow with generic type support and optional badges.",
            codeExample = """
val filters = listOf("All", "Today", "Upcoming", "Completed")
var currentFilter by remember { mutableStateOf("All") }

Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
    filters.forEach { filter ->
        FilterChip(
            selected = currentFilter == filter,
            onClick = { currentFilter = filter },
            label = { Text(filter) }
        )
    }
}
            """.trimIndent(),
            tags = listOf("filter", "chips", "tags", "categories", "segmented")
        ),

        // 12. Sort Selector
        ComponentEntry(
            id = "sort_selector_menu",
            name = "Sort Selector Menu",
            category = ComponentCategory.SELECTION_CONTROLS,
            status = ComponentStatus.EXISTING,
            description = "Drop-down action menu attached to a Sort icon button allowing users to order lists by Priority, Due Date, Title, or Created Date.",
            whenToUse = "Use in list headers to allow users to adjust item display ordering.",
            sourceFile = "app/src/main/java/com/example/ui/screens/tasks/TasksScreen.kt",
            composableName = "IconButton + DropdownMenu (Sort)",
            parameters = listOf(
                ComponentParam("currentSort", "String", "Current active sort criteria"),
                ComponentParam("sortOptions", "List<String>", "List of sort criteria"),
                ComponentParam("onSortSelected", "(String) -> Unit", "Callback when a criteria is chosen")
            ),
            usageLocations = listOf("TasksScreen", "CalendarScreen (TaskListSection)"),
            reuseRecommendations = "Extract as SortSelectorButton for plug-and-play addition to any list toolbar.",
            codeExample = """
var sort by remember { mutableStateOf("Priority") }
var showSort by remember { mutableStateOf(false) }

IconButton(onClick = { showSort = true }) {
    Icon(Icons.Default.Sort, contentDescription = "Sort")
}
// DropdownMenu with sort options...
            """.trimIndent(),
            tags = listOf("sort", "order", "priority", "date", "alphabetical")
        ),

        // 13. Grid / List View Toggle
        ComponentEntry(
            id = "grid_list_toggle",
            name = "Grid / List View Toggle",
            category = ComponentCategory.NAVIGATION_LAYOUT,
            status = ComponentStatus.EXISTING,
            description = "Icon toggle button allowing users to switch between 1-column detailed List View and 2-column compact Grid View.",
            whenToUse = "Use on collection screens (Tasks, Calendar task section, Settings Hub) to accommodate different user density preferences.",
            sourceFile = "app/src/main/java/com/example/ui/screens/tasks/TasksScreen.kt",
            composableName = "FilledTonalIconButton (Grid/List Toggle)",
            parameters = listOf(
                ComponentParam("isGridView", "Boolean", "True for 2-column grid, false for list"),
                ComponentParam("onToggle", "(Boolean) -> Unit", "Callback toggling the mode")
            ),
            usageLocations = listOf("TasksScreen", "CalendarScreen", "SettingsScreen (Hub Cards)"),
            reuseRecommendations = "Standardize as a compact pill button with smooth crossfade animation between layouts.",
            codeExample = """
var isGrid by remember { mutableStateOf(false) }

FilledTonalIconButton(onClick = { isGrid = !isGrid }) {
    Icon(
        imageVector = if (isGrid) Icons.Default.ViewList else Icons.Default.GridView,
        contentDescription = "Toggle View Mode"
    )
}
            """.trimIndent(),
            tags = listOf("grid", "list", "layout", "toggle", "columns")
        ),

        // 14. Task Completion Checkbox / Circle
        ComponentEntry(
            id = "task_completion_circle",
            name = "Task Completion Checkbox / Circle",
            category = ComponentCategory.VISUAL_STYLING,
            status = ComponentStatus.EXISTING,
            description = "Circular tap-target for completing tasks, displaying priority-tinted border when pending and filled checkmark with haptic feedback when completed.",
            whenToUse = "Use on all task cards, agenda rows, and calendar day items.",
            sourceFile = "app/src/main/java/com/example/ui/components/TaskCompletionCircle.kt",
            composableName = "TaskCompletionCircle",
            parameters = listOf(
                ComponentParam("isCompleted", "Boolean", "Completion state"),
                ComponentParam("activeColor", "Color", "Color corresponding to task priority"),
                ComponentParam("onToggle", "() -> Unit", "Callback when circle is tapped")
            ),
            usageLocations = listOf("TaskCard", "TaskGridCard", "CalendarScreen (TaskListSection, AgendaView)", "75-Day Challenge Screen", "Dashboard Challenge Card"),
            reuseRecommendations = "Ensure haptic feedback and sound triggers are routed consistently through TaskCompletionFeedbackManager.",
            codeExample = """
TaskCompletionCircle(
    isCompleted = task.isCompletedForDate,
    activeColor = task.priority.composeColor,
    onToggle = { onToggleTask(task) }
)
            """.trimIndent(),
            tags = listOf("checkbox", "completion", "circle", "checkmark", "task")
        ),

        // 15. Habit Progress & Streak Indicator
        ComponentEntry(
            id = "habit_streak_indicator",
            name = "Habit Progress & Streak Indicator",
            category = ComponentCategory.VISUAL_STYLING,
            status = ComponentStatus.EXISTING,
            description = "Fire-icon streak badge with day count, paired with a mini weekly calendar dot row showing completion across the last 7 days.",
            whenToUse = "Use in Habit cards, dashboard widgets, and gamification progress summaries.",
            sourceFile = "app/src/main/java/com/example/ui/components/CommonComponents.kt",
            composableName = "HabitCard (Streak row & 7-day indicators)",
            parameters = listOf(
                ComponentParam("currentStreak", "Int", "Current streak in consecutive days"),
                ComponentParam("completedDates", "Set<String>", "Set of completed ISO dates"),
                ComponentParam("colorHex", "Long", "Habit theme accent color")
            ),
            usageLocations = listOf("HabitCard", "HabitsScreen", "DashboardScreen"),
            reuseRecommendations = "Extract the 7-day dot indicator strip as a reusable HabitMiniWeeklyStrip composable.",
            codeExample = """
Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFF59E0B))
    Text("14d streak", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
}
            """.trimIndent(),
            tags = listOf("streak", "habit", "fire", "progress", "weekly", "dots")
        ),

        // 16. Quick Reminder Honeycomb Tile
        ComponentEntry(
            id = "honeycomb_tile",
            name = "Quick Reminder Honeycomb Tile",
            category = ComponentCategory.VISUAL_STYLING,
            status = ComponentStatus.EXISTING,
            description = "Specialized axial-coordinate canvas tile rendering Pointy-top Hexagon, Circle, or Square geometry with radial completion stroke, emoji center, and dynamic color cascades.",
            whenToUse = "Use on the Home Dashboard quick action canvas and launcher home screen widget.",
            sourceFile = "app/src/main/java/com/example/ui/components/HoneycombTaskGrid.kt",
            composableName = "HoneycombTile & HoneycombTaskGrid",
            parameters = listOf(
                ComponentParam("shortcut", "HoneycombShortcut", "Shortcut data model with coordinates and shape"),
                ComponentParam("appearance", "HoneycombAppearanceConfig", "Global styling preferences"),
                ComponentParam("onTap", "(HoneycombShortcut) -> Unit", "Callback on single tap (toggle)"),
                ComponentParam("onLongPress", "(HoneycombShortcut) -> Unit", "Callback on long press (edit)")
            ),
            usageLocations = listOf("HoneycombTaskGrid", "DashboardScreen", "QuickRemindersWidgetProvider"),
            reuseRecommendations = "Keep mathematical axial alignment formulas (\$q = col - \\lfloor(r + rowLen)/2\\rfloor$) synchronized with launcher RemoteViews widget rows.",
            codeExample = """
HoneycombTile(
    shortcut = shortcutItem,
    appearance = appearanceConfig,
    onTap = { viewModel.toggleHoneycombShortcut(it) },
    onLongPress = { editingShortcut = it }
)
            """.trimIndent(),
            tags = listOf("honeycomb", "hexagon", "tile", "shortcut", "axial", "canvas")
        ),

        // 17. Category Chip & Tag Selector
        ComponentEntry(
            id = "category_chip",
            name = "Category Chip & Tag Selector",
            category = ComponentCategory.SELECTION_CONTROLS,
            status = ComponentStatus.EXISTING,
            description = "Compact pill chip displaying category title or hashtag label with rounded borders and subtle background alpha.",
            whenToUse = "Use on task cards, habit cards, and project metadata displays to convey categorization.",
            sourceFile = "app/src/main/java/com/example/ui/components/CommonComponents.kt",
            composableName = "CategoryChip",
            parameters = listOf(
                ComponentParam("category", "String", "Category name string (e.g. 'Personal', 'Work')"),
                ComponentParam("color", "Color?", "Optional explicit tint color"),
                ComponentParam("onClick", "(() -> Unit)?", "Optional click handler for filtering")
            ),
            usageLocations = listOf("TaskCard", "TaskGridCard", "TaskAdvancedEditorSheet", "ProjectsLabelsManagerScreen"),
            reuseRecommendations = "Standardize label tags (#tag) and project folder chips to share this styling.",
            codeExample = """
CategoryChip(categoryName = "Work & Projects")
            """.trimIndent(),
            tags = listOf("category", "chip", "tag", "pill", "label")
        ),

        // 18. Numeric Input With Unit Conversion
        ComponentEntry(
            id = "numeric_unit_input",
            name = "Numeric Input With Unit Conversion",
            category = ComponentCategory.INPUTS_FORMS,
            status = ComponentStatus.EXISTING,
            description = "Outlined numeric text field coupled with unit selector chips, allowing decimal inputs (e.g. 2.5 Liters or 84.5 fl oz) and converting to base storage units.",
            whenToUse = "Use for hydration targets, medication strength/dosages, and inventory odometer readings.",
            sourceFile = "app/src/main/java/com/example/ui/screens/settings/SettingsScreen.kt",
            composableName = "OutlinedTextField (Daily Hydration Goal)",
            parameters = listOf(
                ComponentParam("value", "String", "Current formatted string value"),
                ComponentParam("unit", "String", "Active unit (e.g. 'ml', 'L', 'oz')"),
                ComponentParam("onValueChange", "(String) -> Unit", "Callback on input with validation")
            ),
            usageLocations = listOf("SettingsScreen (Hydration)", "MedicineReminderSection (Dosage)", "TrackerEditorSheet"),
            reuseRecommendations = "Extract as a generic NumericQuantityField with automatic two-way unit conversion.",
            codeExample = """
var input by remember { mutableStateOf("2.5") }
OutlinedTextField(
    value = input,
    onValueChange = { input = it },
    label = { Text("Daily Hydration (L)") },
    singleLine = true
)
            """.trimIndent(),
            tags = listOf("numeric", "conversion", "liters", "ounces", "ml", "decimal")
        ),

        // 19. Medication Schedule Selector
        ComponentEntry(
            id = "medication_schedule_selector",
            name = "Medication Schedule Selector",
            category = ComponentCategory.TASK_PRODUCTIVITY,
            status = ComponentStatus.EXISTING,
            description = "Specialized health component managing 6 meal period configs (Breakfast, Lunch, Dinner, etc.), meal relationships (Before/With/After Food), interval hours, and custom dosage units.",
            whenToUse = "Use when creating or editing health reminders, prescriptions, and recurring supplements.",
            sourceFile = "app/src/main/java/com/example/ui/components/MedicineReminderSection.kt",
            composableName = "MedicineReminderSection",
            parameters = listOf(
                ComponentParam("isMedicineEnabled", "Boolean", "Toggle state enabling medicine mode"),
                ComponentParam("onMedicineEnabledChange", "(Boolean) -> Unit", "Toggle callback"),
                ComponentParam("taskTitle", "String", "Parent task title for synchronization"),
                ComponentParam("initialMedication", "MedicationItem?", "Existing medication record if editing"),
                ComponentParam("onMedicationDraftChanged", "(MedicationItem?) -> Unit", "Callback emitting medication draft")
            ),
            usageLocations = listOf("TaskAdvancedEditorSheet", "TaskQuickAddSheet", "MedicationScreen"),
            reuseRecommendations = "Encapsulates progressive disclosure so non-medical tasks incur zero UI clutter.",
            codeExample = """
MedicineReminderSection(
    isMedicineEnabled = isMedicine,
    onMedicineEnabledChange = { isMedicine = it },
    taskTitle = title,
    initialMedication = null,
    onMedicationDraftChanged = { draft = it }
)
            """.trimIndent(),
            tags = listOf("medication", "medicine", "health", "pills", "meals", "prescriptions")
        ),

        // 20. Recurrence / Repeat Rule Selector
        ComponentEntry(
            id = "recurrence_rule_selector",
            name = "Recurrence / Repeat Rule Selector",
            category = ComponentCategory.TASK_PRODUCTIVITY,
            status = ComponentStatus.EXISTING,
            description = "Modal dialog for configuring recurrence frequencies (Daily, Weekly, Monthly, Yearly), interval counts, weekday bitmasks, and monthly day rules.",
            whenToUse = "Use for recurring tasks, repeat habits, and periodic calendar events.",
            sourceFile = "app/src/main/java/com/example/ui/components/RecurrenceEditorDialog.kt",
            composableName = "RecurrenceEditorDialog",
            parameters = listOf(
                ComponentParam("initialType", "RecurrenceType", "NONE, DAILY, WEEKLY, MONTHLY, YEARLY"),
                ComponentParam("initialInterval", "Int", "Interval step (e.g. every 2 weeks)"),
                ComponentParam("initialDaysMask", "Int", "Bitmask representing active weekdays"),
                ComponentParam("onSave", "(RecurrenceType, Int, Int) -> Unit", "Confirm callback"),
                ComponentParam("onDismiss", "() -> Unit", "Cancel callback")
            ),
            usageLocations = listOf("TaskAdvancedEditorSheet", "HabitsScreen"),
            reuseRecommendations = "Fully replaces ad-hoc interval pickers; pair with RecurrenceEngine for recurrence date calculations.",
            codeExample = """
if (showRecurrenceDialog) {
    RecurrenceEditorDialog(
        initialType = RecurrenceType.WEEKLY,
        initialInterval = 1,
        initialDaysMask = 62, // Mon-Fri
        onSave = { type, interval, mask -> /* save */ },
        onDismiss = { showRecurrenceDialog = false }
    )
}
            """.trimIndent(),
            tags = listOf("recurrence", "repeat", "daily", "weekly", "monthly", "schedule")
        ),

        // 21. Notification / Reminder Configuration
        ComponentEntry(
            id = "notification_reminder_config",
            name = "Notification / Reminder Configuration",
            category = ComponentCategory.TASK_PRODUCTIVITY,
            status = ComponentStatus.EXISTING,
            description = "Custom reminder dialog enabling users to add multiple timed reminders (e.g. 'At due time', '10 mins before', '1 hour before') with alert sound choices.",
            whenToUse = "Use in task advanced editor and settings notification preferences.",
            sourceFile = "app/src/main/java/com/example/ui/components/CustomReminderDialog.kt",
            composableName = "CustomReminderDialog",
            parameters = listOf(
                ComponentParam("reminders", "List<TaskReminderItem>", "Existing reminders list"),
                ComponentParam("onRemindersChanged", "(List<TaskReminderItem>) -> Unit", "Callback with updated list"),
                ComponentParam("onDismiss", "() -> Unit", "Dismiss callback")
            ),
            usageLocations = listOf("TaskAdvancedEditorSheet", "TaskQuickAddSheet"),
            reuseRecommendations = "Ensure exact alarm permission checks are integrated gracefully when reminders are toggled.",
            codeExample = """
CustomReminderDialog(
    reminders = remindersList,
    onRemindersChanged = { remindersList = it },
    onDismiss = { showReminderDialog = false }
)
            """.trimIndent(),
            tags = listOf("notification", "reminder", "alarm", "alert", "timing")
        ),

        // 22. Empty State Component
        ComponentEntry(
            id = "empty_state_card",
            name = "Empty State Component",
            category = ComponentCategory.FEEDBACK_DIALOGS,
            status = ComponentStatus.EXISTING,
            description = "Centered, friendly illustration card with icon, title, description, and optional action button for empty lists, search results, and filters.",
            whenToUse = "Use whenever a list has 0 items (e.g. empty inbox, zero trash items, no search matches).",
            sourceFile = "app/src/main/java/com/example/ui/components/CommonComponents.kt",
            composableName = "EmptyStateCard",
            parameters = listOf(
                ComponentParam("icon", "ImageVector", "Center thematic icon"),
                ComponentParam("title", "String", "Headline title (e.g. 'Inbox Zero')"),
                ComponentParam("subtitle", "String", "Helpful explanation or guidance"),
                ComponentParam("actionLabel", "String?", "Optional call-to-action button text"),
                ComponentParam("onAction", "(() -> Unit)?", "Optional action button callback")
            ),
            usageLocations = listOf("TasksScreen", "TrashBinDialog", "ProjectsLabelsManagerScreen", "HoneycombTaskGrid"),
            reuseRecommendations = "Use across all screens to ensure users never face a blank, confusing empty screen.",
            codeExample = """
EmptyStateCard(
    icon = Icons.Default.Inbox,
    title = "Inbox Zero",
    subtitle = "You have completed all scheduled tasks for today!",
    actionLabel = "+ Create Task",
    onAction = { showQuickAdd = true }
)
            """.trimIndent(),
            tags = listOf("empty", "state", "zero", "inbox", "placeholder", "illustration")
        ),

        // 23. Loading Skeleton / Shimmer
        ComponentEntry(
            id = "loading_skeleton",
            name = "Loading Skeleton / Shimmer",
            category = ComponentCategory.FEEDBACK_DIALOGS,
            status = ComponentStatus.EXISTING,
            description = "Placeholder cards with pulsating alpha animation shown during cold start while Room database and DataStore preferences initialize.",
            whenToUse = "Use during initial app startup and heavy asynchronous data loading.",
            sourceFile = "app/src/main/java/com/example/ui/screens/dashboard/DashboardScreen.kt",
            composableName = "DashboardLoadingSkeleton",
            parameters = listOf(
                ComponentParam("modifier", "Modifier", "Layout modifier")
            ),
            usageLocations = listOf("DashboardScreen", "MainNavigationTransition"),
            reuseRecommendations = "Prevents any text flash or jumpy layout transitions during cold launch.",
            codeExample = """
if (uiState.isLoading) {
    DashboardLoadingSkeleton()
} else {
    // Normal content...
}
            """.trimIndent(),
            tags = listOf("loading", "skeleton", "shimmer", "placeholder", "cold start")
        ),

        // 24. Snackbar / Toast Feedback
        ComponentEntry(
            id = "feedback_snackbar",
            name = "Snackbar / Audio Feedback",
            category = ComponentCategory.FEEDBACK_DIALOGS,
            status = ComponentStatus.EXISTING,
            description = "Combined visual Snackbar with haptic pulse and synthesized PCM audio chime playback on task or habit completion.",
            whenToUse = "Use on completion toggles, undo actions, and save confirmation alerts.",
            sourceFile = "app/src/main/java/com/example/ui/feedback/TaskCompletionFeedbackManager.kt",
            composableName = "TaskCompletionFeedbackManager.playCompletionFeedback()",
            parameters = listOf(
                ComponentParam("context", "Context", "Android context for Vibrator and AudioTrack"),
                ComponentParam("soundType", "FeedbackSoundType", "Choice of 12 PCM sounds or custom audio"),
                ComponentParam("hapticsEnabled", "Boolean", "Whether to vibrate")
            ),
            usageLocations = listOf("LifeViewModel", "HoneycombTaskGrid", "TasksScreen", "SettingsScreen", "ChallengeScreen"),
            reuseRecommendations = "Never instantiate separate MediaPlayer objects; route all audio through the singleton manager.",
            codeExample = """
TaskCompletionFeedbackManager.playCompletionFeedback(
    context = context,
    soundType = FeedbackSoundType.TASK,
    hapticsEnabled = true
)
            """.trimIndent(),
            tags = listOf("audio", "sound", "feedback", "haptic", "vibrate", "snackbar")
        ),

        // 25. Progress Bar & Circular Progress Indicator
        ComponentEntry(
            id = "progress_indicators",
            name = "Progress Bar & Circular Progress",
            category = ComponentCategory.FEEDBACK_DIALOGS,
            status = ComponentStatus.EXISTING,
            description = "Linear and circular progress indicators rendering completion percentages with animated tween transitions and themed stroke caps.",
            whenToUse = "Use for overall daily progress, 75-Day Challenge completion, water tracker fill levels, and level XP progress.",
            sourceFile = "app/src/main/java/com/example/ui/screens/dashboard/DashboardScreen.kt",
            composableName = "LinearProgressIndicator & OverallProgressArc",
            parameters = listOf(
                ComponentParam("progress", "Float", "Progress value from 0.0f to 1.0f"),
                ComponentParam("color", "Color", "Fill progress color"),
                ComponentParam("trackColor", "Color", "Background track color")
            ),
            usageLocations = listOf("DashboardScreen", "WaterTrackerCard", "GamificationScreen", "ChallengeScreen"),
            reuseRecommendations = "Animate progress using animateFloatAsState to ensure smooth visual transitions.",
            codeExample = """
val animatedProgress by animateFloatAsState(targetValue = 0.75f, label = "progress")
LinearProgressIndicator(
    progress = { animatedProgress },
    modifier = Modifier.fillMaxWidth().height(8.dp),
    color = MaterialTheme.colorScheme.primary
)
            """.trimIndent(),
            tags = listOf("progress", "bar", "arc", "percentage", "linear", "circular")
        ),

        // 26. Settings Navigation Card
        ComponentEntry(
            id = "settings_navigation_card",
            name = "Settings Navigation Card",
            category = ComponentCategory.NAVIGATION_LAYOUT,
            status = ComponentStatus.EXISTING,
            description = "Polished navigation card with rounded corners, themed leading icon in circular container, title, two-line subtitle, and chevron arrow.",
            whenToUse = "Use in Settings Hub and dashboard navigation hubs to organize subpages.",
            sourceFile = "app/src/main/java/com/example/ui/screens/settings/SettingsScreen.kt",
            composableName = "SettingsNavigationCard & SettingsGridNavigationCard",
            parameters = listOf(
                ComponentParam("icon", "ImageVector", "Leading section icon"),
                ComponentParam("title", "String", "Section title"),
                ComponentParam("subtitle", "String", "Summary description of settings inside"),
                ComponentParam("onClick", "() -> Unit", "Navigation callback"),
                ComponentParam("highlighted", "Boolean", "Optional highlighted accent container")
            ),
            usageLocations = listOf("SettingsScreen (Hub View)", "UserManualScreen"),
            reuseRecommendations = "Use for hub-and-spoke navigation across settings, guides, and documentation.",
            codeExample = """
SettingsNavigationCard(
    icon = Icons.Default.Palette,
    title = "Appearance & Themes",
    subtitle = "10 curated themes, dark mode, honeycomb density",
    onClick = { onNavigateToSubpage(SettingsSubpage.APPEARANCE) }
)
            """.trimIndent(),
            tags = listOf("card", "navigation", "settings", "hub", "menu")
        ),

        // 27. Bottom Sheet / Editor Sheet Container
        ComponentEntry(
            id = "bottom_sheet_container",
            name = "Bottom Sheet / Editor Sheet Container",
            category = ComponentCategory.NAVIGATION_LAYOUT,
            status = ComponentStatus.EXISTING,
            description = "Standardized ModalBottomSheet container with drag handle, title row with expand/close icons, and IME/keyboard-safe scrolling column.",
            whenToUse = "Use for all task creation, editing, project creation, and detail views.",
            sourceFile = "app/src/main/java/com/example/ui/components/TaskQuickAddSheet.kt",
            composableName = "ModalBottomSheet (TaskQuickAddSheet / TaskAdvancedEditorSheet)",
            parameters = listOf(
                ComponentParam("onDismiss", "() -> Unit", "Callback when sheet is dismissed"),
                ComponentParam("content", "@Composable ColumnScope.() -> Unit", "Sheet content lambda")
            ),
            usageLocations = listOf("TaskQuickAddSheet", "TaskAdvancedEditorSheet", "EditProjectBottomSheet", "EditLabelBottomSheet"),
            reuseRecommendations = "Always include windowInsetsPadding(WindowInsets.ime) to prevent keyboards from obscuring inputs.",
            codeExample = """
ModalBottomSheet(
    onDismissRequest = { showSheet = false },
    containerColor = MaterialTheme.colorScheme.surface
) {
    Column(modifier = Modifier.padding(16.dp).imePadding()) {
        // Form inputs...
    }
}
            """.trimIndent(),
            tags = listOf("sheet", "modal", "bottomsheet", "dialog", "container")
        ),

        // 28. Expandable Information Card
        ComponentEntry(
            id = "expandable_card",
            name = "Expandable Information Card",
            category = ComponentCategory.NAVIGATION_LAYOUT,
            status = ComponentStatus.EXISTING,
            description = "Card featuring an expandable header with title and chevron icon, revealing secondary configuration or details with AnimatedVisibility.",
            whenToUse = "Use for progressive disclosure of advanced settings, rules, or subtasks.",
            sourceFile = "app/src/main/java/com/example/ui/components/CommonComponents.kt",
            composableName = "ExpandableCard / Subtasks Section",
            parameters = listOf(
                ComponentParam("title", "String", "Header title"),
                ComponentParam("isExpanded", "Boolean", "Expansion state"),
                ComponentParam("onToggle", "() -> Unit", "Toggle callback"),
                ComponentParam("content", "@Composable () -> Unit", "Expandable body content")
            ),
            usageLocations = listOf("TaskAdvancedEditorSheet", "ChallengeScreen", "SettingsScreen (Quiet Hours)"),
            reuseRecommendations = "Reduces initial visual complexity on mobile screens by hiding optional inputs until requested.",
            codeExample = """
var expanded by remember { mutableStateOf(false) }

Card(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
    Column(modifier = Modifier.padding(14.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Advanced Options", fontWeight = FontWeight.Bold)
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
        }
        AnimatedVisibility(visible = expanded) {
            // Expanded content...
        }
    }
}
            """.trimIndent(),
            tags = listOf("expandable", "accordion", "disclosure", "animation", "card")
        ),

        // 29. Toggle / Switch Setting Row
        ComponentEntry(
            id = "toggle_switch_row",
            name = "Toggle / Switch Setting Row",
            category = ComponentCategory.SELECTION_CONTROLS,
            status = ComponentStatus.EXISTING,
            description = "Clean horizontal setting item with title, optional subtitle, and Material 3 Switch.",
            whenToUse = "Use in settings pages, reminder toggles, and privacy preferences.",
            sourceFile = "app/src/main/java/com/example/ui/screens/settings/SettingsScreen.kt",
            composableName = "NotificationToggleRow",
            parameters = listOf(
                ComponentParam("title", "String", "Row label"),
                ComponentParam("checked", "Boolean", "Switch active state"),
                ComponentParam("onCheckedChange", "(Boolean) -> Unit", "Callback when toggled")
            ),
            usageLocations = listOf("SettingsScreen", "TaskAdvancedEditorSheet", "TaskQuickAddSheet"),
            reuseRecommendations = "Replaces repetitive Row + Switch boilerplates across settings screens.",
            codeExample = """
var isEnabled by remember { mutableStateOf(true) }

Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text("Enable Smart Reminders")
    Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
}
            """.trimIndent(),
            tags = listOf("switch", "toggle", "checkbox", "settings", "boolean")
        ),

        // 30. Form Validation & Error Message
        ComponentEntry(
            id = "validation_error_message",
            name = "Form Validation & Error Message",
            category = ComponentCategory.INPUTS_FORMS,
            status = ComponentStatus.EXISTING,
            description = "Animated error callout with red warning icon and text message displayed underneath invalid input fields.",
            whenToUse = "Use when user inputs empty titles, out-of-bound numbers, or invalid URL strings.",
            sourceFile = "app/src/main/java/com/example/ui/components/LabeledUrlEditor.kt",
            composableName = "Text(color = MaterialTheme.colorScheme.error)",
            parameters = listOf(
                ComponentParam("errorMessage", "String?", "Error string to display or null if valid")
            ),
            usageLocations = listOf("LabeledUrlEditor", "TaskAdvancedEditorSheet", "DurationInput"),
            reuseRecommendations = "Extract as a ValidationErrorMessage composable with subtle shake or fade animation.",
            codeExample = """
if (errorMessage != null) {
    Text(
        text = errorMessage,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 12.dp, top = 4.dp)
    )
}
            """.trimIndent(),
            tags = listOf("validation", "error", "message", "form", "input")
        ),

        // 31. Custom Audio / Sound Preview Selector
        ComponentEntry(
            id = "sound_preview_selector",
            name = "Custom Audio / Sound Preview",
            category = ComponentCategory.VISUAL_STYLING,
            status = ComponentStatus.EXISTING,
            description = "Sound effect selection picker with inline play button to preview chime synthesis, custom sound URI import, and feedback type badges.",
            whenToUse = "Use in Settings Gamification & Notifications subpages.",
            sourceFile = "app/src/main/java/com/example/ui/screens/settings/SettingsScreen.kt",
            composableName = "SoundEffectSelectionRow",
            parameters = listOf(
                ComponentParam("selectedSound", "String", "FeedbackSoundType name"),
                ComponentParam("onSoundSelected", "(String) -> Unit", "Callback when selected"),
                ComponentParam("onPlayPreview", "(String) -> Unit", "Callback to audition sound")
            ),
            usageLocations = listOf("SettingsScreen"),
            reuseRecommendations = "Ensures previews never mutate user task records while allowing safe auditioning.",
            codeExample = """
Row(verticalAlignment = Alignment.CenterVertically) {
    Text("Task Bell")
    IconButton(onClick = { playPreview("BELL") }) {
        Icon(Icons.Default.VolumeUp, "Play")
    }
}
            """.trimIndent(),
            tags = listOf("audio", "sound", "preview", "chime", "pcm", "volume")
        ),

        // 32. Calendar Day / Date Indicator
        ComponentEntry(
            id = "calendar_day_indicator",
            name = "Calendar Day / Date Indicator",
            category = ComponentCategory.DATE_TIME,
            status = ComponentStatus.EXISTING,
            description = "Calendar date cell rendering day number, today indicator ring, selection highlight, and RecurrenceEngine-backed dot indicator.",
            whenToUse = "Use in month calendar grids, week strip selectors, and date pickers.",
            sourceFile = "app/src/main/java/com/example/ui/screens/calendar/CalendarScreen.kt",
            composableName = "MonthGridView & WeekStripView",
            parameters = listOf(
                ComponentParam("dateIso", "String", "ISO-8601 date string"),
                ComponentParam("isSelected", "Boolean", "Whether date is currently focused"),
                ComponentParam("hasTasks", "Boolean", "Whether dot indicator should be rendered"),
                ComponentParam("onSelect", "(String) -> Unit", "Click callback")
            ),
            usageLocations = listOf("CalendarScreen", "CalendarHeatmapView"),
            reuseRecommendations = "Extract the single date cell as a reusable CalendarDayCell composable.",
            codeExample = """
Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier.size(38.dp).clip(CircleShape).clickable { onSelect(date) }
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("27")
        if (hasTasks) {
            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color.Red))
        }
    }
}
            """.trimIndent(),
            tags = listOf("calendar", "day", "cell", "dot", "indicator", "month")
        ),

        // 33. Attachment / File Picker Field
        ComponentEntry(
            id = "file_picker_field",
            name = "Attachment / File Picker Field",
            category = ComponentCategory.INPUTS_FORMS,
            status = ComponentStatus.PARTIAL,
            description = "Storage Access Framework (SAF) trigger field allowing users to select local files, audio tracks, or backup archives with URI persistence.",
            whenToUse = "Use for custom sound imports, JSON backup exports, and file attachments.",
            sourceFile = "app/src/main/java/com/example/ui/screens/settings/SettingsScreen.kt",
            composableName = "rememberLauncherForActivityResult(GetContent)",
            parameters = listOf(
                ComponentParam("mimeType", "String", "MIME type filter (e.g. 'audio/*', 'application/json')"),
                ComponentParam("onUriSelected", "(Uri?) -> Unit", "Result callback with selected URI")
            ),
            usageLocations = listOf("SettingsScreen", "BackupRestoreDialog"),
            reuseRecommendations = "Package the SAF contract launcher and URI permission persistable grant into a reusable FilePickerTriggerField.",
            codeExample = """
val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    uri?.let { /* handle URI */ }
}
OutlinedButton(onClick = { launcher.launch("audio/*") }) {
    Icon(Icons.Default.AttachFile, null)
    Text("Import File")
}
            """.trimIndent(),
            tags = listOf("file", "attachment", "saf", "picker", "uri", "import")
        ),

        // 34. Reusable Section Header
        ComponentEntry(
            id = "section_header",
            name = "Reusable Section Header",
            category = ComponentCategory.NAVIGATION_LAYOUT,
            status = ComponentStatus.EXISTING,
            description = "Prominent uppercase section header with optional counter badge, subtitle, and action button.",
            whenToUse = "Use above content sections in long scrollable forms and settings pages.",
            sourceFile = "app/src/main/java/com/example/ui/screens/settings/SettingsScreen.kt",
            composableName = "SectionHeader",
            parameters = listOf(
                ComponentParam("title", "String", "Section header text"),
                ComponentParam("subtitle", "String?", "Optional secondary hint"),
                ComponentParam("actionLabel", "String?", "Optional action button text"),
                ComponentParam("onAction", "(() -> Unit)?", "Optional action callback")
            ),
            usageLocations = listOf("SettingsScreen", "TasksScreen", "TaskAdvancedEditorSheet"),
            reuseRecommendations = "Use consistently instead of bare Text composables for section headings.",
            codeExample = """
Text(
    text = "SCHEDULE & REMINDERS",
    fontSize = 12.sp,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.primary,
    letterSpacing = 1.sp
)
            """.trimIndent(),
            tags = listOf("header", "section", "title", "divider", "typography")
        ),

        // 35. Confirmation / Success Feedback Animation
        ComponentEntry(
            id = "success_feedback_animation",
            name = "Success Feedback Animation",
            category = ComponentCategory.FEEDBACK_DIALOGS,
            status = ComponentStatus.EXISTING,
            description = "Thematic celebration motif overlay and animated checkmark scale for celebrating task milestones, level ups, and challenge wins.",
            whenToUse = "Use when reaching streak milestones, finishing the daily clean slate, or applying a premium theme.",
            sourceFile = "app/src/main/java/com/example/ui/theme/engine/ThemeAtmosphereOverlay.kt",
            composableName = "ThemeAtmosphereOverlay & LevelUpDialog",
            parameters = listOf(
                ComponentParam("motifEmoji", "String", "Celebratory icon or emoji"),
                ComponentParam("onFinished", "() -> Unit", "Callback when animation completes")
            ),
            usageLocations = listOf("MainNavigationTransition", "GamificationScreen", "DashboardScreen"),
            reuseRecommendations = "Respect accessibility reduced motion settings by bypassing animation when requested.",
            codeExample = """
ThemeAtmosphereOverlay(
    activeThemeDef = currentTheme,
    onAnimationEnd = { /* complete */ }
)
            """.trimIndent(),
            tags = listOf("animation", "celebration", "motif", "success", "confetti")
        ),

        // 36. Backup & Restore SAF Dialog
        ComponentEntry(
            id = "backup_restore_dialog",
            name = "Backup & Restore SAF Dialog",
            category = ComponentCategory.SPECIALIZED_LIFEOS,
            status = ComponentStatus.EXISTING,
            description = "Specialized dialog orchestrating encrypted JSON backup creation, local file export via Android Storage Access Framework, and data restoration.",
            whenToUse = "Use in Data & Storage settings to safeguard user data.",
            sourceFile = "app/src/main/java/com/example/ui/components/BackupRestoreDialog.kt",
            composableName = "BackupRestoreDialog",
            parameters = listOf(
                ComponentParam("onExport", "() -> Unit", "Export trigger callback"),
                ComponentParam("onImport", "(Uri) -> Unit", "Import callback with file URI"),
                ComponentParam("onDismiss", "() -> Unit", "Dismiss callback")
            ),
            usageLocations = listOf("SettingsScreen (Data & Storage)"),
            reuseRecommendations = "Enforces zero-cloud offline-first security pledge.",
            codeExample = """
BackupRestoreDialog(
    onExport = { viewModel.exportBackup() },
    onImport = { uri -> viewModel.importBackup(uri) },
    onDismiss = { showBackupDialog = false }
)
            """.trimIndent(),
            tags = listOf("backup", "restore", "json", "export", "import", "saf", "encryption")
        ),

        // 37. Premium Animated Card (#1, #8)
        ComponentEntry(
            id = "premium_card",
            name = "Premium Animated Card",
            category = ComponentCategory.VISUAL_STYLING,
            status = ComponentStatus.EXISTING,
            description = "High-fidelity interactive card supporting 5 styles (DEFAULT, ELEVATED, GLASS, ACCENT_BORDER, THEME_BESPOKE), tactile spring press physics, animated border highlights, selection and completion styling, header badge, and smooth content size expansion.",
            whenToUse = "Use for dashboard summary cards, task tiles, habit counters, project panels, and setting sections where tactile visual polish is desired.",
            sourceFile = "app/src/main/java/com/example/ui/components/PremiumComponents.kt",
            composableName = "PremiumCard",
            parameters = listOf(
                ComponentParam("style", "PremiumCardStyle", "Visual style: DEFAULT, ELEVATED, GLASS, ACCENT_BORDER, THEME_BESPOKE"),
                ComponentParam("isSelected", "Boolean", "Highlights card with primary glow and 2dp border"),
                ComponentParam("isCompleted", "Boolean", "Subtle dimmed surface with strikethrough styling"),
                ComponentParam("isExpanded", "Boolean", "Animates content visibility via animateContentSize"),
                ComponentParam("headerTitle", "String?", "Optional header title string"),
                ComponentParam("headerEmoji", "String?", "Optional header emoji motif"),
                ComponentParam("badgeText", "String?", "Optional status badge chip text in header"),
                ComponentParam("onClick", "(() -> Unit)?", "Optional click handler enabling spring press physics")
            ),
            usageLocations = listOf("DashboardScreen", "TasksScreen", "SettingsScreen", "UserManualScreen"),
            reuseRecommendations = "Standardizes card surfaces, gradients, borders, and animations across all feature screens.",
            codeExample = """
PremiumCard(
    title = "75-Day Challenge",
    headerEmoji = "🏆",
    badgeText = "Day 42/75",
    style = PremiumCardStyle.THEME_BESPOKE,
    isExpanded = isExpanded,
    onClick = { isExpanded = !isExpanded }
) {
    Text("Active daily streak and metrics go here...")
}
            """.trimIndent(),
            tags = listOf("card", "premium", "spring", "glass", "animation", "surface", "expandable", "tactile")
        ),

        // 38. Premium Animated Button (#1)
        ComponentEntry(
            id = "premium_button",
            name = "Premium Spring Animated Button",
            category = ComponentCategory.INPUTS_FORMS,
            status = ComponentStatus.EXISTING,
            description = "High-responsiveness interactive button with tactile spring bounce on press, 5 styles (PRIMARY, SECONDARY, OUTLINED, GLASS, DESTRUCTIVE), built-in micro-spinner loading state maintaining bounds, and success checkmark transition.",
            whenToUse = "Use for primary call-to-action buttons, submit buttons, delete triggers, and dialog confirmation flows requiring rich tactile feedback.",
            sourceFile = "app/src/main/java/com/example/ui/components/PremiumComponents.kt",
            composableName = "PremiumButton",
            parameters = listOf(
                ComponentParam("onClick", "() -> Unit", "Button click callback"),
                ComponentParam("text", "String", "Button label text"),
                ComponentParam("style", "PremiumButtonStyle", "Style tier: PRIMARY, SECONDARY, OUTLINED, GLASS, DESTRUCTIVE"),
                ComponentParam("leadingIcon", "ImageVector?", "Optional leading icon"),
                ComponentParam("isLoading", "Boolean", "Displays micro-spinner and disables interaction"),
                ComponentParam("isSuccess", "Boolean", "Displays animated green checkmark"),
                ComponentParam("enabled", "Boolean", "Whether the button is interactive")
            ),
            usageLocations = listOf("TaskQuickAddSheet", "SettingsScreen", "ThemeGalleryScreen", "LegalConsentScreen"),
            reuseRecommendations = "Replaces boilerplate loading/success state wrappers and ensures uniform spring feedback.",
            codeExample = """
var isSaving by remember { mutableStateOf(false) }
var isSuccess by remember { mutableStateOf(false) }

PremiumButton(
    text = "Save Task",
    isLoading = isSaving,
    isSuccess = isSuccess,
    style = PremiumButtonStyle.PRIMARY,
    onClick = {
        isSaving = true
        saveTask { isSaving = false; isSuccess = true }
    }
)
            """.trimIndent(),
            tags = listOf("button", "premium", "spring", "loading", "success", "glass", "tactile", "bounce")
        ),

        // 39. Glassmorphism Frosted Panel (#7, #8)
        ComponentEntry(
            id = "glass_theme_panel",
            name = "Glassmorphism Frosted Panel & Prism Motif",
            category = ComponentCategory.VISUAL_STYLING,
            status = ComponentStatus.EXISTING,
            description = "Translucent frosted glass surface featuring specular edge highlights, subtle blur-like layered gradients, and the ThemeId.GLASS prism refraction canvas glyph.",
            whenToUse = "Use when displaying high-aesthetic floating controls, Glass theme cards, and translucent overlays.",
            sourceFile = "app/src/main/java/com/example/ui/theme/engine/ThemeRegistry.kt & ThemeAssetManager.kt",
            composableName = "PremiumCard(style = PremiumCardStyle.GLASS) & drawPrismRefraction",
            parameters = listOf(
                ComponentParam("themeId", "ThemeId", "Active theme (ThemeId.GLASS)"),
                ComponentParam("isGlass", "Boolean", "Token shapes.isGlass enabled on Glassmorphism theme"),
                ComponentParam("engraving", "HoneycombEngravingType", "HoneycombEngravingType.PRISM with 6-point refraction spokes")
            ),
            usageLocations = listOf("ThemeGalleryScreen", "SettingsScreen (Appearance)", "DashboardScreen (Glass Theme)"),
            reuseRecommendations = "Leverages low-overhead canvas draw paths and translucent gradient fills rather than costly GPU blur passes.",
            codeExample = """
PremiumCard(
    style = PremiumCardStyle.GLASS,
    title = "Frosted Focus Pane",
    headerEmoji = "💎"
) {
    Text("Translucent glass background with specular white highlight border.")
}
            """.trimIndent(),
            tags = listOf("glass", "glassmorphism", "frosted", "prism", "translucent", "refraction", "specular")
        ),

        // 40. Loading Animation Speed Engine (#23)
        ComponentEntry(
            id = "animation_speed_system",
            name = "Loading Animation Speed & Motion Engine",
            category = ComponentCategory.VISUAL_STYLING,
            status = ComponentStatus.EXISTING,
            description = "Centralized animation velocity control system providing 5 discrete speed tiers (OFF 0.0x, SLOW 0.5x, NORMAL 1.0x, FAST 1.5x, VERY_FAST 2.0x) with zero-speed static canvas safety.",
            whenToUse = "Use to scale any infinite transition, rotation, pulse, or particle duration across the app.",
            sourceFile = "app/src/main/java/com/example/ui/theme/engine/ThemeTokens.kt & ThemeContext.kt",
            composableName = "LocalLifeAnimationSpeed & AnimationSpeed.calculateDuration",
            parameters = listOf(
                ComponentParam("multiplier", "Float", "0.0f (Off), 0.5f, 1.0f, 1.5f, 2.0f"),
                ComponentParam("calculateDuration", "(Int) -> Int", "Calculates scaled duration in ms, returns 0 when Off"),
                ComponentParam("LocalLifeAnimationSpeed", "CompositionLocal", "Provides active AnimationSpeed to Compose subtree")
            ),
            usageLocations = listOf("ThemedLoadingIndicator", "SettingsScreen (Appearance)", "AtmosphereCanvas"),
            reuseRecommendations = "Essential for battery saving, accessibility (reduced motion), and user speed preference.",
            codeExample = """
val speed = LocalLifeAnimationSpeed.current
val durationMs = speed.calculateDuration(1200)

if (speed == AnimationSpeed.OFF) {
    // Render static canvas or zero-animation state
} else {
    // Animate with durationMs
}
            """.trimIndent(),
            tags = listOf("animation", "speed", "motion", "accessibility", "battery", "spinner", "velocity")
        ),

        // 41. Honeycomb Profile Selector (#9)
        ComponentEntry(
            id = "honeycomb_profile_selector",
            name = "Honeycomb Profile Selector",
            category = ComponentCategory.NAVIGATION_LAYOUT,
            status = ComponentStatus.EXISTING,
            description = "Horizontal scrolling workspace selector displaying active and available profiles with tactile spring animations, active profile indicators, and workspace creation tile.",
            whenToUse = "Use at the top of the Dashboard or navigation headers to switch between Personal, Work, and Custom isolated workspaces.",
            sourceFile = "app/src/main/java/com/example/ui/components/HoneycombProfileSelector.kt",
            composableName = "HoneycombProfileSelector",
            parameters = listOf(
                ComponentParam("profiles", "List<ProfileEntity>", "Available workspace profiles"),
                ComponentParam("activeProfileId", "Long", "Currently active profile ID"),
                ComponentParam("onProfileSelected", "(Long) -> Unit", "Callback when a profile is tapped"),
                ComponentParam("onAddProfileClick", "() -> Unit", "Callback when 'New Profile' is tapped"),
                ComponentParam("onDeleteProfileClick", "((ProfileEntity) -> Unit)?", "Optional callback to delete custom profile")
            ),
            usageLocations = listOf("DashboardScreen", "ComponentDetailSheet"),
            reuseRecommendations = "Centralized entrypoint for Requirement #9 profile isolation; guarantees instant reactive flow switching.",
            codeExample = """
HoneycombProfileSelector(
    profiles = allProfiles,
    activeProfileId = activeProfileId,
    onProfileSelected = { viewModel.switchProfile(it) },
    onAddProfileClick = { showCreateDialog = true }
)
            """.trimIndent(),
            tags = listOf("profile", "workspace", "isolation", "honeycomb", "selector", "switch", "work", "personal")
        ),

        // 42. Workspace Profile Tile (#9)
        ComponentEntry(
            id = "profile_tile",
            name = "Workspace Profile Tile",
            category = ComponentCategory.SPECIALIZED_LIFEOS,
            status = ComponentStatus.EXISTING,
            description = "Individual styled workspace tile featuring hexagonal emoji avatar, custom color accent border, active glow, and tactile spring press feedback.",
            whenToUse = "Use inside HoneycombProfileSelector, settings workspace management, or profile switcher dialogs.",
            sourceFile = "app/src/main/java/com/example/ui/components/HoneycombProfileSelector.kt",
            composableName = "HoneycombProfileTile",
            parameters = listOf(
                ComponentParam("profile", "ProfileEntity", "Profile data entity"),
                ComponentParam("isActive", "Boolean", "Whether profile is currently active"),
                ComponentParam("onClick", "() -> Unit", "Tap callback with tactile spring"),
                ComponentParam("onDeleteClick", "(() -> Unit)?", "Optional delete action for custom profiles")
            ),
            usageLocations = listOf("HoneycombProfileSelector", "DashboardScreen"),
            reuseRecommendations = "Pair with LocalLifeAnimationSpeed for custom spring velocity.",
            codeExample = """
HoneycombProfileTile(
    profile = profile,
    isActive = true,
    onClick = { /* switch */ }
)
            """.trimIndent(),
            tags = listOf("profile", "tile", "card", "hex", "avatar", "workspace", "badge")
        ),

        // 43. App-Entry Honeycomb Profile Selector (#9)
        ComponentEntry(
            id = "app_entry_profile_selector",
            name = "App-Entry Honeycomb Profile Selector",
            category = ComponentCategory.NAVIGATION_LAYOUT,
            status = ComponentStatus.EXISTING,
            description = "Tactile fullscreen honeycomb workspace portal presented at app launch. Supports selecting isolated workspaces, auto-entering, and creating new workspaces.",
            whenToUse = "Use during app entry or when returning to profile selection from Settings.",
            sourceFile = "app/src/main/java/com/example/ui/screens/profile/AppEntryProfileSelectorScreen.kt",
            composableName = "AppEntryProfileSelectorScreen",
            parameters = listOf(
                ComponentParam("profiles", "List<ProfileEntity>", "Available workspaces"),
                ComponentParam("activeProfileId", "Long", "Persisted active profile ID"),
                ComponentParam("showOnStartupPreference", "Boolean", "Whether to show at each startup"),
                ComponentParam("onProfileSelected", "(Long) -> Unit", "Profile selection tap callback"),
                ComponentParam("onEnterWorkspace", "(Long) -> Unit", "Enter workspace confirmation callback")
            ),
            usageLocations = listOf("MainActivity", "AppEntryProfileSelectorScreen"),
            reuseRecommendations = "Guarantees zero flicker of other profiles and isolates workspaces completely.",
            codeExample = """
AppEntryProfileSelectorScreen(
    profiles = allProfiles,
    activeProfileId = activeProfileId,
    showOnStartupPreference = showOnStartup,
    onProfileSelected = { id -> viewModel.switchProfile(id) },
    onEnterWorkspace = { id -> viewModel.switchProfile(id); viewModel.dismissAppEntryProfileSelector() }
)
            """.trimIndent(),
            tags = listOf("profile", "entry", "workspace", "honeycomb", "selector", "portal", "cold start")
        ),

        // 44. Onboarding Survey Screen (#19)
        ComponentEntry(
            id = "onboarding_survey_screen",
            name = "First-Run & New-Profile Onboarding Survey",
            category = ComponentCategory.INPUTS_FORMS,
            status = ComponentStatus.EXISTING,
            description = "Optional, skippable 5-step onboarding wizard gathering user display name, demographic gender, sleeping habits, daily hydration goal, and discovery source without telemetry.",
            whenToUse = "Use on fresh installs, new workspace profile creation, or profile settings editing.",
            sourceFile = "app/src/main/java/com/example/ui/screens/onboarding/OnboardingSurveyScreen.kt",
            composableName = "OnboardingSurveyScreen",
            parameters = listOf(
                ComponentParam("profileId", "Long", "Workspace profile ID"),
                ComponentParam("initialSurvey", "ProfileSurveyEntity?", "Existing survey if in edit mode"),
                ComponentParam("isEditMode", "Boolean", "Whether opened from Settings"),
                ComponentParam("onComplete", "(ProfileSurveyEntity) -> Unit", "Completion callback with survey payload"),
                ComponentParam("onSkipAll", "() -> Unit", "Skip all callback with zero synthetic answers")
            ),
            usageLocations = listOf("MainActivity", "SettingsScreen", "OnboardingSurveyScreen"),
            reuseRecommendations = "Ensures zero telemetry: all data stays local to Room database v12.",
            codeExample = """
OnboardingSurveyScreen(
    profileId = 1L,
    isEditMode = false,
    onComplete = { survey -> viewModel.saveProfileSurvey(survey) },
    onSkipAll = { viewModel.skipProfileSurvey(1L) }
)
            """.trimIndent(),
            tags = listOf("survey", "onboarding", "wizard", "questions", "profile", "local", "privacy")
        ),

        // 45. Survey Time Picker (#19)
        ComponentEntry(
            id = "survey_time_picker",
            name = "Onboarding Sleeping Time Picker",
            category = ComponentCategory.DATE_TIME,
            status = ComponentStatus.EXISTING,
            description = "Intuitive bedtime selection control offering standard presets (21:30 to 00:00) with active chips and bed icon indicator.",
            whenToUse = "Use inside onboarding survey or settings quiet hours configuration.",
            sourceFile = "app/src/main/java/com/example/ui/screens/onboarding/OnboardingSurveyScreen.kt",
            composableName = "SleepingTimeStep",
            parameters = listOf(
                ComponentParam("sleepingTime", "String", "Selected bedtime format (HH:mm)"),
                ComponentParam("onSleepingTimeSelected", "(String) -> Unit", "Bedtime selection callback")
            ),
            usageLocations = listOf("OnboardingSurveyScreen"),
            reuseRecommendations = "Aligns with Settings quiet hours to automatically calm evening alerts.",
            codeExample = """
SleepingTimeStep(
    sleepingTime = "22:00",
    onSleepingTimeSelected = { time -> /* update */ }
)
            """.trimIndent(),
            tags = listOf("time", "bedtime", "sleep", "survey", "chips", "picker")
        ),

        // 46. Survey Water Goal Stepper (#19)
        ComponentEntry(
            id = "survey_water_goal",
            name = "Daily Water Goal Stepper & Input",
            category = ComponentCategory.INPUTS_FORMS,
            status = ComponentStatus.EXISTING,
            description = "Validated numeric hydration input with sanity bounds (500ml - 6000ml) and quick preset buttons (1500ml, 2000ml, 2500ml, 3000ml).",
            whenToUse = "Use inside onboarding survey and hydration settings.",
            sourceFile = "app/src/main/java/com/example/ui/screens/onboarding/OnboardingSurveyScreen.kt",
            composableName = "WaterGoalStep",
            parameters = listOf(
                ComponentParam("waterGoalText", "String", "Water goal string in ml"),
                ComponentParam("onWaterGoalChange", "(String) -> Unit", "Value change listener"),
                ComponentParam("errorMessage", "String?", "Validation error text")
            ),
            usageLocations = listOf("OnboardingSurveyScreen", "SettingsScreen"),
            reuseRecommendations = "Never overwrite existing water settings silently when skipped.",
            codeExample = """
WaterGoalStep(
    waterGoalText = "2000",
    onWaterGoalChange = { text -> /* update */ },
    errorMessage = null
)
            """.trimIndent(),
            tags = listOf("water", "goal", "hydration", "stepper", "validation", "ml")
        ),

        // 47. Survey Discovery Source Selector (#19)
        ComponentEntry(
            id = "survey_discovery_selector",
            name = "Discovery Source & Social Media Selector",
            category = ComponentCategory.INPUTS_FORMS,
            status = ComponentStatus.EXISTING,
            description = "Multi-level discovery attribution selector supporting Social Media (alphabetical platform list), GitHub, Google, Friends, and bounded free-text.",
            whenToUse = "Use in Onboarding survey and feedback forms.",
            sourceFile = "app/src/main/java/com/example/ui/screens/onboarding/OnboardingSurveyScreen.kt",
            composableName = "DiscoverySourceStep",
            parameters = listOf(
                ComponentParam("selectedSource", "String", "Main discovery channel"),
                ComponentParam("onSourceSelected", "(String) -> Unit", "Main channel callback"),
                ComponentParam("selectedPlatform", "String", "Social platform if applicable"),
                ComponentParam("onPlatformSelected", "(String) -> Unit", "Platform callback"),
                ComponentParam("otherDetail", "String", "Bounded custom detail text"),
                ComponentParam("onDetailChange", "(String) -> Unit", "Detail text listener")
            ),
            usageLocations = listOf("OnboardingSurveyScreen"),
            reuseRecommendations = "Always bound free-text length to 100 characters max.",
            codeExample = """
DiscoverySourceStep(
    selectedSource = "Social Media",
    onSourceSelected = { /* update */ },
    selectedPlatform = "Reddit",
    onPlatformSelected = { /* update */ },
    otherDetail = "",
    onDetailChange = { /* update */ }
)
            """.trimIndent(),
            tags = listOf("discovery", "source", "social", "survey", "referral", "attribution")
        ),

        // 48. Task Emoji Suggester (#5)
        ComponentEntry(
            id = "task_emoji_suggester",
            name = "Task Title Emoji Suggester",
            category = ComponentCategory.INPUTS_FORMS,
            status = ComponentStatus.EXISTING,
            description = "Offline keyword-driven emoji suggestion engine that recommends contextual emojis based on task titles, with full manual override protection.",
            whenToUse = "Use in task quick add, advanced task creation, and title input fields.",
            sourceFile = "app/src/main/java/com/example/ui/components/EmojiSuggestionRow.kt",
            composableName = "EmojiSuggestionRow",
            parameters = listOf(
                ComponentParam("suggestions", "List<String>", "List of suggested emoji characters"),
                ComponentParam("currentEmoji", "String", "Currently active task emoji"),
                ComponentParam("onEmojiSelected", "(String) -> Unit", "Callback when an emoji is chosen")
            ),
            usageLocations = listOf("TaskQuickAddSheet", "TaskAdvancedEditorSheet"),
            reuseRecommendations = "Pair with TaskEmojiSuggestionEngine. Never overwrite an emoji if isEmojiManuallySelected is true.",
            codeExample = """
EmojiSuggestionRow(
    suggestions = listOf("🛒", "🥛", "🍎"),
    currentEmoji = "🛒",
    onEmojiSelected = { emoji -> /* apply */ }
)
            """.trimIndent(),
            tags = listOf("emoji", "suggestions", "task", "quick add", "offline", "recommendation")
        ),

        // 49. Multi-Label Checkbox Selector (#6)
        ComponentEntry(
            id = "multi_label_checkbox_selector",
            name = "Multi-Label Checkbox Selector & Chips",
            category = ComponentCategory.SELECTION_CONTROLS,
            status = ComponentStatus.EXISTING,
            description = "Checkbox-based multi-label selector bottom sheet and removable label chips strip for workspace-scoped task labeling.",
            whenToUse = "Use in task creation, quick add sheets, and batch tag management.",
            sourceFile = "app/src/main/java/com/example/ui/components/MultiLabelCheckboxSelector.kt",
            composableName = "MultiLabelSelectorBottomSheet & SelectedLabelChips",
            parameters = listOf(
                ComponentParam("availableLabels", "List<LabelItem>", "Labels belonging to current workspace profile"),
                ComponentParam("selectedLabelIds", "Set<Long>", "Active label IDs"),
                ComponentParam("onToggleLabel", "(Long) -> Unit", "Toggle label selection callback"),
                ComponentParam("onCreateNewLabel", "() -> Unit", "Action to create a new label in-line")
            ),
            usageLocations = listOf("TaskQuickAddSheet", "TaskAdvancedEditorSheet"),
            reuseRecommendations = "Always display selected labels with SelectedLabelChips with instant removable 'x' icons.",
            codeExample = """
SelectedLabelChips(
    selectedLabels = activeLabels,
    onRemoveLabel = { id -> /* remove */ }
)

MultiLabelSelectorBottomSheet(
    availableLabels = labels,
    selectedLabelIds = selectedIds,
    onToggleLabel = { id -> /* toggle */ },
    onCreateNewLabel = { /* open sheet */ },
    onDismiss = { /* close */ }
)
            """.trimIndent(),
            tags = listOf("labels", "checkbox", "multi-select", "chips", "tags", "filter")
        ),

        // 50. Medication Priority & Timing Controls (#11)
        ComponentEntry(
            id = "medication_priority_timing_control",
            name = "Medication Priority & Timing Controls",
            category = ComponentCategory.INPUTS_FORMS,
            status = ComponentStatus.EXISTING,
            description = "Progressive medication reminder configuration supporting automatic P1 priority defaults without silent override, timing presets (before/after meals, empty stomach, before sleep), and calibrated sleep/wake hours.",
            whenToUse = "Use in task creation, medication scheduling, and health routine planning.",
            sourceFile = "app/src/main/java/com/example/ui/components/MedicineReminderSection.kt",
            composableName = "MedicineReminderSection",
            parameters = listOf(
                ComponentParam("isMedicineEnabled", "Boolean", "Toggle state for medication reminder"),
                ComponentParam("onMedicineEnabledChange", "(Boolean) -> Unit", "Toggle listener with P1 default logic"),
                ComponentParam("taskTitle", "String", "Task title for medicine name prefill"),
                ComponentParam("wakeUpTime", "String", "Wake-up preference time for calibration (HH:mm)"),
                ComponentParam("bedTime", "String", "Bed-time preference time for calibration (HH:mm)")
            ),
            usageLocations = listOf("TaskQuickAddSheet", "TaskAdvancedEditorSheet"),
            reuseRecommendations = "Calibrate meal periods with user sleep/wake preferences. Preserve manual user priority choices across toggle changes.",
            codeExample = """
MedicineReminderSection(
    isMedicineEnabled = isMedEnabled,
    onMedicineEnabledChange = { enabled -> /* toggle with P1 default */ },
    taskTitle = "Amoxicillin",
    wakeUpTime = "06:30",
    bedTime = "22:30",
    onMedicationDraftChanged = { draft -> /* update */ }
)
            """.trimIndent(),
            tags = listOf("medication", "medicine", "timing", "presets", "meal periods", "priority")
        ),

        // 51. Medication Details & Course Card (#11)
        ComponentEntry(
            id = "medication_detail_card",
            name = "Medication Course Card & Details Sheet",
            category = ComponentCategory.SPECIALIZED_LIFEOS,
            status = ComponentStatus.EXISTING,
            description = "Clear visual hierarchy course card and comprehensive details modal sheet showing dosage, timing, food relationship, remaining doses, adherence rate, quick action chips, and adherence history.",
            whenToUse = "Use in Medicine Tracker screen and course management dialogs.",
            sourceFile = "app/src/main/java/com/example/ui/screens/medication/MedicationScreen.kt",
            composableName = "MedicationCourseCard & MedicationDetailsBottomSheet",
            parameters = listOf(
                ComponentParam("medication", "MedicationItem", "Active medication course item"),
                ComponentParam("doses", "List<MedicationDoseItem>", "Logged dose occurrences for adherence history"),
                ComponentParam("onMarkTaken", "(Long) -> Unit", "Dose taken callback"),
                ComponentParam("onTogglePause", "() -> Unit", "Pause or resume course callback")
            ),
            usageLocations = listOf("MedicationScreen"),
            reuseRecommendations = "Use MedicationDetailsBottomSheet for deep inspection, dosage logging, and adherence tracking.",
            codeExample = """
MedicationCourseCard(
    medication = medItem,
    onTogglePause = { /* pause/resume */ },
    onLogPrnDose = { /* log */ },
    onClick = { /* open details */ }
)
            """.trimIndent(),
            tags = listOf("medication", "details", "course", "adherence", "tracker", "dosage")
        ),

        // 52. Calendar Range & Recurrence Markers (#18)
        ComponentEntry(
            id = "calendar_deadline_range_marker",
            name = "Calendar Range & Recurrence Markers",
            category = ComponentCategory.DATE_TIME,
            status = ComponentStatus.EXISTING,
            description = "Enhanced calendar markers featuring dotted circular borders for intermediate date ranges, distinct double-dot recurrence indicators, and decoupled always-visible deadline fields.",
            whenToUse = "Use in MonthGridView, WeekStripView, and Task Editors.",
            sourceFile = "app/src/main/java/com/example/ui/screens/calendar/CalendarScreen.kt",
            composableName = "MonthGridView & WeekStripView",
            parameters = listOf(
                ComponentParam("datesWithTasks", "Set<String>", "Dates containing active scheduled tasks"),
                ComponentParam("datesWithRecurringTasks", "Set<String>", "Dates containing recurring tasks"),
                ComponentParam("rangeStartDate", "String?", "Start boundary of date range"),
                ComponentParam("rangeEndDate", "String?", "End boundary of date range")
            ),
            usageLocations = listOf("CalendarScreen", "TaskAdvancedEditorSheet", "TaskQuickAddSheet"),
            reuseRecommendations = "Render dotted circular borders with PathEffect.dashPathEffect. Distinguish recurring tasks with multi-dot markers.",
            codeExample = """
MonthGridView(
    monthDates = monthDates,
    selectedDate = selectedDate,
    today = today,
    monthIndex = 10,
    datesWithTasks = datesWithTasks,
    datesWithRecurringTasks = recurringDates,
    rangeStartDate = "2026-11-01",
    rangeEndDate = "2026-11-05",
    onSelectDate = { /* select */ }
)
            """.trimIndent(),
            tags = listOf("calendar", "range", "dotted", "recurrence", "deadline", "markers")
        )
    )

    fun getByCategory(category: ComponentCategory): List<ComponentEntry> {
        return if (category == ComponentCategory.ALL) items else items.filter { it.category == category }
    }

    fun getByStatus(status: ComponentStatus?): List<ComponentEntry> {
        return if (status == null) items else items.filter { it.status == status }
    }

    fun search(query: String, category: ComponentCategory = ComponentCategory.ALL, status: ComponentStatus? = null): List<ComponentEntry> {
        val q = query.trim().lowercase()
        return items.filter { item ->
            val matchesCategory = (category == ComponentCategory.ALL || item.category == category)
            val matchesStatus = (status == null || item.status == status)
            val matchesQuery = q.isBlank() ||
                    item.name.lowercase().contains(q) ||
                    item.description.lowercase().contains(q) ||
                    item.composableName.lowercase().contains(q) ||
                    item.sourceFile.lowercase().contains(q) ||
                    item.tags.any { it.lowercase().contains(q) }
            matchesCategory && matchesStatus && matchesQuery
        }
    }
}
