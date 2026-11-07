package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Work shift definition for the Shift-Based Life Cycle Planner.
 * Supports day, night, swing, and overnight shifts crossing midnight.
 */
@Entity(
    tableName = "shifts",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("profileId")
    ]
)
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val name: String,
    val colorHex: String = "#3B82F6",
    val startTime: String,      // "HH:mm" e.g. "08:00" or "22:00"
    val endTime: String,        // "HH:mm" e.g. "16:00" or "06:00"
    val wakeUpTime: String,     // "HH:mm" e.g. "06:30" or "19:30"
    val sleepTime: String,      // "HH:mm" e.g. "23:00" or "10:00"
    val isOvernight: Boolean = false,
    val shiftHabitIdsJson: String = "[]",     // Habit IDs linked to this shift
    val shiftTaskTitlesJson: String = "[]",    // Default routine task titles
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Shift rotation configuration defining recurrence patterns.
 * Supports Daily, Weekly, 4-On 4-Off, 2-2-3, or custom repeating patterns.
 */
@Entity(
    tableName = "shift_rotations",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("profileId")
    ]
)
data class ShiftRotationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val name: String,
    val rotationType: String = "WEEKLY", // DAILY, WEEKLY, CUSTOM, FIXED_CHART, REPEATING_PATTERN
    val startDateIso: String,           // Reference start date "YYYY-MM-DD"
    val cycleLengthDays: Int = 7,
    val patternJson: String = "[]",     // Array of shift IDs or -1 for OFF day
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Calendar shift assignment and exceptions.
 * An exception overrides the planned rotation for a given day without altering the rotation pattern.
 */
@Entity(
    tableName = "shift_assignments",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ShiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["shiftId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("profileId"),
        Index("shiftId"),
        Index(value = ["profileId", "dateIso"], unique = true)
    ]
)
data class ShiftAssignmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val shiftId: Long?,                 // Nullable when day is OFF or Leave
    val dateIso: String,                // "YYYY-MM-DD"
    val actualStartTime: String? = null,
    val actualEndTime: String? = null,
    val actualWakeUpTime: String? = null,
    val actualSleepTime: String? = null,
    val isException: Boolean = false,
    val exceptionType: String = "NONE", // NONE, OVERTIME, LEAVE, WEEKLY_OFF, SHIFT_SWAP, UNEXPECTED_CHANGE, EMERGENCY_CHANGE
    val exceptionNotes: String = "",
    val overtimeHours: Double = 0.0,
    val isCompleted: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
