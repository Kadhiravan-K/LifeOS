package com.example.domain.medication

/**
 * Supported medication forms for LifeOS.
 * Extensible enum supporting all physical drug administration routes.
 */
enum class MedicationForm(val displayName: String, val emoji: String, val defaultUnit: String) {
    TABLET("Tablet", "💊", "tablet"),
    CAPSULE("Capsule", "💊", "capsule"),
    INJECTION("Injection", "💉", "ml"),
    ORAL_LIQUID("Oral liquid / syrup", "🧪", "ml"),
    EYE_DROPS("Eye drops", "👁️", "drops"),
    EAR_DROPS("Ear drops", "👂", "drops"),
    NASAL_DROPS("Nasal drops", "👃", "sprays"),
    INHALER("Inhaler", "🫁", "puffs"),
    CREAM("Cream", "🧴", "application"),
    OINTMENT("Ointment", "🧴", "application"),
    GEL("Gel", "🧴", "application"),
    PATCH("Patch", "🩹", "patch"),
    POWDER("Powder", "🥄", "sachet"),
    OTHER("Other / Custom", "🏷️", "dose");

    companion object {
        fun fromString(value: String): MedicationForm {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: TABLET
        }
    }
}

/**
 * Schedule modes supported by the medication scheduling engine.
 */
enum class MedicineScheduleType(val displayName: String) {
    MEAL_BASED("Meal-based"),
    FIXED_TIMES("Specific Times"),
    INTERVAL("Repeating Interval"),
    WEEKDAYS("Selected Weekdays"),
    AS_NEEDED("As-Needed (PRN)")
}

/**
 * The four daily meal periods.
 */
enum class MealPeriod(val displayName: String, val defaultTime: String) {
    MORNING_EMPTY_STOMACH("Morning (Empty Stomach)", "06:30"),
    BREAKFAST("Breakfast", "08:30"),
    LUNCH("Lunch", "13:00"),
    EVENING("Evening", "17:30"),
    DINNER("Dinner", "20:30"),
    BEFORE_SLEEP("Before Sleep", "22:30"),
    MORNING("Morning / Breakfast", "08:00")
}

/**
 * Relationship between medication intake and food.
 */
enum class FoodRelationship(val displayName: String) {
    BEFORE_FOOD("Before food"),
    WITH_FOOD("With food"),
    AFTER_FOOD("After food"),
    NO_FOOD("No food relationship")
}

/**
 * Configuration for a single meal period administration.
 */
data class MealPeriodConfig(
    val period: MealPeriod,
    val scheduledTime: String,
    val foodRelation: FoodRelationship = FoodRelationship.NO_FOOD,
    val offsetMinutes: Int = 0,
    val isEnabled: Boolean = true
)

/**
 * Repeating interval calculation models.
 */
enum class IntervalModel(val displayName: String) {
    FIXED_SCHEDULE("Fixed schedule from anchor time"),
    AFTER_CONFIRMED_DOSE("After confirmed dose taken")
}

/**
 * Full schedule configuration for a medication.
 */
data class MedicationScheduleConfig(
    val scheduleType: MedicineScheduleType = MedicineScheduleType.FIXED_TIMES,
    val mealPeriods: List<MealPeriodConfig> = emptyList(),
    val fixedTimes: List<String> = listOf("08:00"),
    val intervalHours: Int = 4,
    val intervalModel: IntervalModel = IntervalModel.FIXED_SCHEDULE,
    val intervalAnchorTime: String = "08:00",
    val allowOvernight: Boolean = false,
    val selectedDaysOfWeek: List<String> = emptyList(), // "MONDAY", "WEDNESDAY", etc.
    val startDateIso: String = "",
    val endDateIso: String? = null,
    val courseDurationDays: Int? = null,
    val isAsNeeded: Boolean = false
)

/**
 * Medication dose statuses.
 */
enum class DoseStatus(val displayName: String, val emoji: String) {
    SCHEDULED("Scheduled", "⏳"),
    TAKEN("Taken", "✅"),
    SKIPPED("Skipped", "⏭️"),
    MISSED("Missed", "⚠️"),
    SNOOZED("Snoozed", "⏰")
}

/**
 * Alert styles for medication notifications.
 */
enum class AlertStyle(val displayName: String) {
    SILENT("Silent (Notification only)"),
    STANDARD("Standard (Sound & Vibration)"),
    ALARM("Alarm-style (Prominent Alert)")
}

/**
 * Complete medication domain model.
 */
data class MedicationItem(
    val id: Long = 0,
    val profileId: Long = 1,
    val taskId: Long,
    val name: String,
    val form: MedicationForm = MedicationForm.TABLET,
    val dosageAmount: String = "1",
    val dosageUnit: String = "tablet",
    val strength: String = "",
    val instructions: String = "",
    val scheduleConfig: MedicationScheduleConfig = MedicationScheduleConfig(),
    val isPaused: Boolean = false,
    val reminderSound: String = "CHIME",
    val alertStyle: AlertStyle = AlertStyle.STANDARD,
    val snoozeMinutes: Int = 10,
    val followUpEnabled: Boolean = true,
    val followUpIntervalMinutes: Int = 15,
    val maxFollowUps: Int = 2,
    val privacySafeNotification: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * An individual scheduled dose occurrence.
 */
data class MedicationDoseItem(
    val id: Long = 0,
    val medicationId: Long,
    val taskId: Long,
    val medicationName: String,
    val medicationForm: MedicationForm = MedicationForm.TABLET,
    val dosage: String,
    val instructions: String = "",
    val scheduledDateIso: String,
    val scheduledTime: String,
    val status: DoseStatus = DoseStatus.SCHEDULED,
    val actionTimestamp: Long? = null,
    val snoozeCount: Int = 0,
    val followUpCount: Int = 0,
    val notes: String = ""
)
