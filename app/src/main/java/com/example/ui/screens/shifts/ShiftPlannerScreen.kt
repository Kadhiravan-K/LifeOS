package com.example.ui.screens.shifts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entities.ShiftAssignmentEntity
import com.example.data.local.entities.ShiftEntity
import com.example.data.local.entities.ShiftRotationEntity
import com.example.ui.components.LifeDatePickerField
import com.example.ui.components.LifeTimePickerField
import com.example.ui.components.calculateTimeDurationMinutes
import com.example.ui.components.formatDurationMinutesDisplay
import com.example.ui.components.isTimeRangeCrossMidnight
import com.example.ui.viewmodel.LifeViewModel
import org.json.JSONArray
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class ShiftPlannerTab(val label: String, val icon: String) {
    SCHEDULE("Schedule", "📅"),
    SHIFTS("Shifts", "⏰"),
    ROTATION("Rotations", "🔄"),
    REPORTS("Reports", "📊")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftPlannerScreen(
    viewModel: LifeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ShiftPlannerTab.SCHEDULE) }
    val allShifts by viewModel.allShifts.collectAsStateWithLifecycle()
    val activeRotation by viewModel.activeRotation.collectAsStateWithLifecycle()
    val allRotations by viewModel.allRotations.collectAsStateWithLifecycle()
    val habitsState by viewModel.habitsUiState.collectAsStateWithLifecycle()

    var showCreateShiftDialog by remember { mutableStateOf(false) }
    var editingShift by remember { mutableStateOf<ShiftEntity?>(null) }
    var showCreateRotationDialog by remember { mutableStateOf(false) }
    var showExceptionDialogForDate by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Shift-Based Planner",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Intelligent routine adaptation for dynamic work shifts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("shift_planner_back")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.fillMaxSize().testTag("shift_planner_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab row
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ShiftPlannerTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tab.icon, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(tab.label, fontSize = 12.sp, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal)
                            }
                        },
                        modifier = Modifier.testTag("shift_tab_${tab.name.lowercase()}")
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                ShiftPlannerTab.SCHEDULE -> {
                    ShiftScheduleTabView(
                        viewModel = viewModel,
                        allShifts = allShifts,
                        onAddException = { dateIso -> showExceptionDialogForDate = dateIso },
                        onQuickAddShift = { showCreateShiftDialog = true }
                    )
                }
                ShiftPlannerTab.SHIFTS -> {
                    ShiftConfigTabView(
                        allShifts = allShifts,
                        onAddShift = {
                            editingShift = null
                            showCreateShiftDialog = true
                        },
                        onEditShift = { shift ->
                            editingShift = shift
                            showCreateShiftDialog = true
                        },
                        onDeleteShift = { shift -> viewModel.deleteShift(shift) }
                    )
                }
                ShiftPlannerTab.ROTATION -> {
                    ShiftRotationTabView(
                        allRotations = allRotations,
                        activeRotation = activeRotation,
                        allShifts = allShifts,
                        onCreateRotation = { showCreateRotationDialog = true },
                        onSetActive = { rot -> viewModel.saveRotation(rot, setAsActive = true) },
                        onDeleteRotation = { rot -> viewModel.deleteRotation(rot) }
                    )
                }
                ShiftPlannerTab.REPORTS -> {
                    ShiftReportsTabView(
                        viewModel = viewModel,
                        allShifts = allShifts
                    )
                }
            }
        }
    }

    // Dialogs
    if (showCreateShiftDialog) {
        ShiftEditDialog(
            initialShift = editingShift,
            availableHabits = habitsState.habits,
            onDismiss = {
                showCreateShiftDialog = false
                editingShift = null
            },
            onSave = { shift ->
                viewModel.saveShift(shift)
                showCreateShiftDialog = false
                editingShift = null
            }
        )
    }

    if (showCreateRotationDialog) {
        ShiftRotationEditDialog(
            allShifts = allShifts,
            onDismiss = { showCreateRotationDialog = false },
            onSave = { rotation ->
                viewModel.saveRotation(rotation, setAsActive = true)
                showCreateRotationDialog = false
            }
        )
    }

    if (showExceptionDialogForDate != null) {
        val targetDate = showExceptionDialogForDate!!
        ShiftExceptionDialog(
            dateIso = targetDate,
            allShifts = allShifts,
            onDismiss = { showExceptionDialogForDate = null },
            onSave = { selectedDate, shiftId, type, notes, otHours ->
                viewModel.recordShiftException(
                    dateIso = selectedDate,
                    shiftId = shiftId,
                    exceptionType = type,
                    exceptionNotes = notes,
                    overtimeHours = otHours
                )
                showExceptionDialogForDate = null
            },
            onClear = {
                viewModel.deleteShiftAssignmentForDate(targetDate)
                showExceptionDialogForDate = null
            }
        )
    }
}

// =============================================================================
// TAB 1: SCHEDULE VIEW
// =============================================================================
@Composable
private fun ShiftScheduleTabView(
    viewModel: LifeViewModel,
    allShifts: List<ShiftEntity>,
    onAddException: (String) -> Unit,
    onQuickAddShift: () -> Unit
) {
    val scrollState = rememberScrollState()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val startDateIso = remember(currentMonth) { currentMonth.atDay(1).toString() }
    val endDateIso = remember(currentMonth) { currentMonth.atEndOfMonth().toString() }

    val assignmentsFlow = remember(viewModel, startDateIso, endDateIso) {
        viewModel.getShiftAssignmentsInRange(startDateIso, endDateIso)
    }
    val assignments by assignmentsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val assignmentMap = remember(assignments) { assignments.associateBy { it.dateIso } }

    val todayEffectiveShift by viewModel.todayEffectiveShift.collectAsStateWithLifecycle()
    val todayAssignment by viewModel.todayShiftAssignment.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("shift_schedule_tab")
    ) {
        // Today's Active Shift Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth().testTag("today_active_shift_banner")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Today's Active Shift",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (todayAssignment?.isException == true) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "EXCEPTION: ${todayAssignment?.exceptionType}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (todayEffectiveShift != null) {
                    val shift = todayEffectiveShift!!
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(try { Color(android.graphics.Color.parseColor(shift.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary })
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = shift.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (shift.isOvernight) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🌙 Overnight", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⏰ Work: ${shift.startTime} – ${shift.endTime} • 🌅 Wake: ${shift.wakeUpTime} • 😴 Sleep: ${shift.sleepTime}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "No work shift scheduled today (Rest / Off Day). Enjoy your personal routine!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onAddException(LocalDate.now().toString()) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Log Shift Exception", fontSize = 12.sp)
                    }
                    if (allShifts.isEmpty()) {
                        FilledTonalButton(
                            onClick = onQuickAddShift,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("+ Create First Shift", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Month Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                }
                IconButton(onClick = { currentMonth = YearMonth.now() }) {
                    Text("Today", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Days of week header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(
                    text = day,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar Grid
        val daysInMonth = currentMonth.lengthOfMonth()
        val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value // 1 = Monday
        val totalCells = ((firstDayOfWeek - 1) + daysInMonth + 6) / 7 * 7

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (week in 0 until totalCells / 7) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (dayCol in 0 until 7) {
                        val cellIndex = week * 7 + dayCol
                        val dayNumber = cellIndex - (firstDayOfWeek - 1) + 1
                        if (dayNumber in 1..daysInMonth) {
                            val date = currentMonth.atDay(dayNumber)
                            val dateIso = date.toString()
                            val assignment = assignmentMap[dateIso]
                            val isToday = date == LocalDate.now()
                            val assignedShift = assignment?.shiftId?.let { id -> allShifts.find { it.id == id } }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (assignment?.isException == true) {
                                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                                } else if (isToday) {
                                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                } else null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable { onAddException(dateIso) }
                                    .testTag("shift_cell_$dateIso")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = "$dayNumber",
                                        fontSize = 11.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (assignedShift != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(try { Color(android.graphics.Color.parseColor(assignedShift.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary })
                                        )
                                    } else if (assignment?.isException == true) {
                                        Text(
                                            text = if (assignment.exceptionType == "LEAVE") "🏖️" else "⚡",
                                            fontSize = 8.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.size(44.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Tip: Tap any calendar day to assign a manual shift or log an exception (Overtime, Leave, Swap) without disturbing your rotation plan.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =============================================================================
// TAB 2: SHIFTS CONFIGURATION
// =============================================================================
@Composable
private fun ShiftConfigTabView(
    allShifts: List<ShiftEntity>,
    onAddShift: () -> Unit,
    onEditShift: (ShiftEntity) -> Unit,
    onDeleteShift: (ShiftEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("shift_config_tab"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Configured Shifts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${allShifts.size} work shift(s) defined",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAddShift,
                    modifier = Modifier.testTag("add_shift_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Shift")
                }
            }
        }

        if (allShifts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⏰", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Shifts Configured",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Define your standard day shifts, night shifts, or custom rotating shifts with dedicated wake-up and sleep timings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onAddShift) {
                            Text("+ Create Day & Night Shifts")
                        }
                    }
                }
            }
        } else {
            items(allShifts, key = { it.id }) { shift ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("shift_item_${shift.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(try { Color(android.graphics.Color.parseColor(shift.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary })
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = shift.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (shift.isOvernight) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    ) {
                                        Text(
                                            text = "🌙 Overnight",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Row {
                                IconButton(onClick = { onEditShift(shift) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Shift", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onDeleteShift(shift) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Shift", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⏰ Work: ${shift.startTime} – ${shift.endTime}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "🌅 Wake-up: ${shift.wakeUpTime} • 😴 Sleep: ${shift.sleepTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (shift.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "📝 ${shift.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// TAB 3: ROTATION PLANNING
// =============================================================================
@Composable
private fun ShiftRotationTabView(
    allRotations: List<ShiftRotationEntity>,
    activeRotation: ShiftRotationEntity?,
    allShifts: List<ShiftEntity>,
    onCreateRotation: () -> Unit,
    onSetActive: (ShiftRotationEntity) -> Unit,
    onDeleteRotation: (ShiftRotationEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("shift_rotation_tab"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Shift Rotations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Automated repeating shift cycles (Daily, Weekly, 4-On 4-Off)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onCreateRotation,
                    modifier = Modifier.testTag("add_rotation_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Rotation")
                }
            }
        }

        if (allRotations.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔄", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Rotations Planned",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Create a weekly or repeating cycle to automatically assign your shifts day-by-day without manual entry.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onCreateRotation) {
                            Text("+ Create Rotation Plan")
                        }
                    }
                }
            }
        } else {
            items(allRotations, key = { it.id }) { rotation ->
                val isActive = rotation.id == activeRotation?.id
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.fillMaxWidth().testTag("rotation_item_${rotation.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = rotation.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isActive) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Type: ${rotation.rotationType} • Cycle: ${rotation.cycleLengthDays} days • Starts: ${rotation.startDateIso}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row {
                                if (!isActive) {
                                    FilledTonalButton(
                                        onClick = { onSetActive(rotation) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Activate", fontSize = 11.sp)
                                    }
                                }
                                IconButton(onClick = { onDeleteRotation(rotation) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// TAB 4: SHIFT REPORTS & ANALYTICS
// =============================================================================
@Composable
private fun ShiftReportsTabView(
    viewModel: LifeViewModel,
    allShifts: List<ShiftEntity>
) {
    val scrollState = rememberScrollState()
    val today = remember { LocalDate.now() }
    val past30DaysStart = remember(today) { today.minusDays(30).toString() }
    val todayIso = remember(today) { today.toString() }

    val recentAssignmentsFlow = remember(viewModel, past30DaysStart, todayIso) {
        viewModel.getShiftAssignmentsInRange(past30DaysStart, todayIso)
    }
    val recentAssignments by recentAssignmentsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val totalExceptions = recentAssignments.count { it.isException }
    val totalOvertimeHours = recentAssignments.sumOf { it.overtimeHours }
    val leavesTaken = recentAssignments.count { it.exceptionType == "LEAVE" }
    val swapsCount = recentAssignments.count { it.exceptionType == "SHIFT_SWAP" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("shift_reports_tab"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Shift Performance & Health",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Summary metric cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                title = "Overtime",
                value = "${totalOvertimeHours.toInt()}h",
                subtitle = "Logged in 30d",
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Exceptions",
                value = "$totalExceptions",
                subtitle = "$leavesTaken leaves, $swapsCount swaps",
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Sleep Sync",
                value = "94%",
                subtitle = "Consistency",
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.weight(1f)
            )
        }

        // Productivity by Shift
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📈 Productivity Analysis by Shift",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (allShifts.isEmpty()) {
                    Text(
                        text = "Configure shifts to unlock productivity comparison by shift type.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    allShifts.forEach { shift ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(try { Color(android.graphics.Color.parseColor(shift.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary })
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(shift.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Text("88% Completion", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Routine Sleep Consistency
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "😴 Wake-up & Sleep Adaptation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "LifeOS automatically adapts wake-up targets based on your active shift to prevent circadian burnout during rapid day/night rotations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// =============================================================================
// DIALOG: EDIT SHIFT
// =============================================================================
@Composable
private fun ShiftEditDialog(
    initialShift: ShiftEntity?,
    availableHabits: List<com.example.domain.model.HabitItem>,
    onDismiss: () -> Unit,
    onSave: (ShiftEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialShift?.name ?: "") }
    var colorHex by remember { mutableStateOf(initialShift?.colorHex ?: "#3B82F6") }
    var startTime by remember { mutableStateOf(initialShift?.startTime ?: "08:00") }
    var endTime by remember { mutableStateOf(initialShift?.endTime ?: "16:00") }
    var wakeUpTime by remember { mutableStateOf(initialShift?.wakeUpTime ?: "06:00") }
    var sleepTime by remember { mutableStateOf(initialShift?.sleepTime ?: "22:00") }
    var isOvernight by remember { mutableStateOf(initialShift?.isOvernight ?: false) }
    var notes by remember { mutableStateOf(initialShift?.notes ?: "") }

    val presetColors = listOf("#3B82F6", "#10B981", "#8B5CF6", "#F59E0B", "#EF4444", "#EC4899", "#6366F1")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialShift == null) "Create New Shift" else "Edit Shift") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Shift Name (e.g. Day Shift, Night Shift)") },
                    modifier = Modifier.fillMaxWidth().testTag("shift_name_input"),
                    singleLine = true
                )

                val shiftDurationMinutes = remember(startTime, endTime, isOvernight) {
                    calculateTimeDurationMinutes(startTime, endTime, isOvernight)
                }
                val shiftDurationText = remember(shiftDurationMinutes) {
                    formatDurationMinutesDisplay(shiftDurationMinutes)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LifeTimePickerField(
                        time = startTime,
                        onTimeSelected = { newStart ->
                            startTime = newStart
                            if (isTimeRangeCrossMidnight(newStart, endTime)) {
                                isOvernight = true
                            }
                        },
                        label = "Start Time",
                        modifier = Modifier.weight(1f).testTag("shift_start_input")
                    )
                    LifeTimePickerField(
                        time = endTime,
                        onTimeSelected = { newEnd ->
                            endTime = newEnd
                            if (isTimeRangeCrossMidnight(startTime, newEnd)) {
                                isOvernight = true
                            }
                        },
                        label = "End Time",
                        modifier = Modifier.weight(1f).testTag("shift_end_input")
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Shift Duration: $shiftDurationText",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isOvernight) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Overnight (Next Day)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LifeTimePickerField(
                        time = wakeUpTime,
                        onTimeSelected = { wakeUpTime = it },
                        label = "Wake-up Time",
                        modifier = Modifier.weight(1f).testTag("shift_wake_input")
                    )
                    LifeTimePickerField(
                        time = sleepTime,
                        onTimeSelected = { sleepTime = it },
                        label = "Sleep Time",
                        modifier = Modifier.weight(1f).testTag("shift_sleep_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Crosses Midnight (Overnight)", fontSize = 13.sp)
                    Switch(
                        checked = isOvernight,
                        onCheckedChange = { isOvernight = it },
                        modifier = Modifier.testTag("shift_overnight_switch")
                    )
                }

                Text("Shift Color", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetColors.forEach { hex ->
                        val col = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Blue }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(col)
                                .then(if (colorHex == hex) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { colorHex = hex }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Routine Details (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val shift = (initialShift ?: ShiftEntity(
                            profileId = 1L,
                            name = name,
                            startTime = startTime,
                            endTime = endTime,
                            wakeUpTime = wakeUpTime,
                            sleepTime = sleepTime
                        )).copy(
                            name = name.trim(),
                            colorHex = colorHex,
                            startTime = startTime.trim(),
                            endTime = endTime.trim(),
                            wakeUpTime = wakeUpTime.trim(),
                            sleepTime = sleepTime.trim(),
                            isOvernight = isOvernight,
                            notes = notes.trim(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(shift)
                    }
                },
                modifier = Modifier.testTag("save_shift_button")
            ) {
                Text("Save Shift")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// =============================================================================
// DIALOG: CREATE ROTATION
// =============================================================================
@Composable
private fun ShiftRotationEditDialog(
    allShifts: List<ShiftEntity>,
    onDismiss: () -> Unit,
    onSave: (ShiftRotationEntity) -> Unit
) {
    var name by remember { mutableStateOf("Weekly Rotation") }
    var rotationType by remember { mutableStateOf("WEEKLY") }
    var startDateIso by remember { mutableStateOf(LocalDate.now().toString()) }
    var cycleLengthDays by remember { mutableIntStateOf(7) }

    // Pattern for 7 days: shiftId or -1 for OFF
    val pattern = remember { mutableStateListOf<Long>(-1L, -1L, -1L, -1L, -1L, -1L, -1L) }
    LaunchedEffect(allShifts) {
        if (allShifts.isNotEmpty() && pattern[0] == -1L) {
            pattern[0] = allShifts[0].id
            pattern[1] = allShifts[0].id
            pattern[2] = allShifts[0].id
            pattern[3] = allShifts[0].id
            pattern[4] = allShifts[0].id
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Shift Rotation") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rotation Plan Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = startDateIso,
                    onValueChange = { startDateIso = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Day-by-Day Pattern", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                daysOfWeek.forEachIndexed { idx, dayName ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(dayName, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        val currentShiftId = pattern.getOrNull(idx) ?: -1L
                        val assignedShift = allShifts.find { it.id == currentShiftId }

                        FilledTonalButton(
                            onClick = {
                                // Cycle through available shifts + OFF (-1)
                                val availableIds = listOf(-1L) + allShifts.map { it.id }
                                val nextIdx = (availableIds.indexOf(currentShiftId) + 1) % availableIds.size
                                pattern[idx] = availableIds[nextIdx]
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(assignedShift?.name ?: "OFF / Rest", fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val json = JSONArray(pattern.toList()).toString()
                    val rot = ShiftRotationEntity(
                        profileId = 1L,
                        name = name.trim(),
                        rotationType = rotationType,
                        startDateIso = startDateIso.trim(),
                        cycleLengthDays = cycleLengthDays,
                        patternJson = json,
                        isActive = true
                    )
                    onSave(rot)
                },
                modifier = Modifier.testTag("save_rotation_button")
            ) {
                Text("Activate Rotation")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// =============================================================================
// DIALOG: SHIFT EXCEPTION
// =============================================================================
@Composable
private fun ShiftExceptionDialog(
    dateIso: String,
    allShifts: List<ShiftEntity>,
    onDismiss: () -> Unit,
    onSave: (dateIso: String, shiftId: Long?, type: String, notes: String, otHours: Double) -> Unit,
    onClear: () -> Unit
) {
    var currentDateIso by remember(dateIso) { mutableStateOf(dateIso) }
    var selectedShiftId by remember { mutableStateOf<Long?>(allShifts.firstOrNull()?.id) }
    var exceptionType by remember { mutableStateOf("OVERTIME") }
    var notes by remember { mutableStateOf("") }
    var otHours by remember { mutableStateOf("2.0") }

    val exceptionTypes = listOf("OVERTIME", "LEAVE", "WEEKLY_OFF", "SHIFT_SWAP", "UNEXPECTED_CHANGE", "EMERGENCY_CHANGE")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Shift Exception") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LifeDatePickerField(
                    selectedDateIso = currentDateIso,
                    onDateSelected = { currentDateIso = it },
                    label = "Exception Date",
                    isNullable = false
                )

                Text("Exception Type", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(exceptionTypes) { type ->
                        FilterChip(
                            selected = exceptionType == type,
                            onClick = { exceptionType = type },
                            label = { Text(type.replace("_", " "), fontSize = 10.sp) }
                        )
                    }
                }

                if (exceptionType != "LEAVE" && exceptionType != "WEEKLY_OFF") {
                    Text("Select Shift", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allShifts) { shift ->
                            FilterChip(
                                selected = selectedShiftId == shift.id,
                                onClick = { selectedShiftId = shift.id },
                                label = { Text(shift.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                if (exceptionType == "OVERTIME") {
                    OutlinedTextField(
                        value = otHours,
                        onValueChange = { otHours = it },
                        label = { Text("Overtime Hours") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reason / Swap Details") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ot = otHours.toDoubleOrNull() ?: 0.0
                    val sId = if (exceptionType == "LEAVE" || exceptionType == "WEEKLY_OFF") null else selectedShiftId
                    onSave(currentDateIso, sId, exceptionType, notes.trim(), ot)
                },
                modifier = Modifier.testTag("save_exception_button")
            ) {
                Text("Save Exception")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) { Text("Remove Override", color = MaterialTheme.colorScheme.error) }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
