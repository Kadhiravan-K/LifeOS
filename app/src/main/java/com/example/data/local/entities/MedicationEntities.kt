package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medications",
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["taskId"]),
        Index(value = ["isPaused"])
    ]
)
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 1,
    val taskId: Long,
    val name: String,
    val form: String,
    val dosageAmount: String,
    val dosageUnit: String,
    val strength: String = "",
    val instructions: String = "",
    val scheduleType: String,
    val mealPeriodsJson: String = "[]",
    val fixedTimesJson: String = "[]",
    val intervalHours: Int = 4,
    val intervalModel: String = "FIXED_SCHEDULE",
    val intervalAnchorTime: String = "08:00",
    val allowOvernight: Boolean = false,
    val selectedDaysOfWeek: String = "",
    val startDateIso: String,
    val endDateIso: String? = null,
    val courseDurationDays: Int? = null,
    val isAsNeeded: Boolean = false,
    val isPaused: Boolean = false,
    val reminderSound: String = "CHIME",
    val alertStyle: String = "STANDARD",
    val snoozeMinutes: Int = 10,
    val followUpEnabled: Boolean = true,
    val followUpIntervalMinutes: Int = 15,
    val maxFollowUps: Int = 2,
    val privacySafeNotification: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "medication_doses",
    indices = [
        Index(value = ["medicationId"]),
        Index(value = ["taskId"]),
        Index(value = ["scheduledDateIso"]),
        Index(value = ["status"]),
        Index(value = ["medicationId", "scheduledDateIso", "scheduledTime"], unique = true)
    ]
)
data class MedicationDoseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val taskId: Long,
    val medicationName: String,
    val medicationForm: String,
    val dosage: String,
    val instructions: String = "",
    val scheduledDateIso: String,
    val scheduledTime: String,
    val status: String = "SCHEDULED", // SCHEDULED, TAKEN, SKIPPED, MISSED, SNOOZED
    val actionTimestamp: Long? = null,
    val snoozeCount: Int = 0,
    val followUpCount: Int = 0,
    val notes: String = ""
)
