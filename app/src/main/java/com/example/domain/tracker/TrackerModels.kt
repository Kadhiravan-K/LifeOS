package com.example.domain.tracker

import androidx.compose.ui.graphics.Color
import com.example.domain.model.TodoistColorPalette

/**
 * Measurement dimensions that a Smart Tracker can track.
 * Independently toggleable (not mutually exclusive).
 */
data class TrackerMeasurements(
    val trackDuration: Boolean = true,
    val trackDistance: Boolean = false,
    val trackQuantity: Boolean = false
)

enum class DistanceUnit(val symbol: String, val displayName: String) {
    KILOMETERS("km", "Kilometers"),
    MILES("mi", "Miles");

    companion object {
        fun fromString(value: String?): DistanceUnit {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.symbol.equals(value, ignoreCase = true) }
                ?: KILOMETERS
        }
    }
}

enum class QuantityUnit(val symbol: String, val displayName: String) {
    PIECES("pcs", "Pieces / Count"),
    LITERS("L", "Liters"),
    MILLILITERS("mL", "Milliliters"),
    KILOGRAMS("kg", "Kilograms"),
    GRAMS("g", "Grams"),
    CUSTOM("units", "Custom Units");

    companion object {
        fun fromString(value: String?): QuantityUnit {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.symbol.equals(value, ignoreCase = true) }
                ?: PIECES
        }
    }
}

enum class InventoryPolicy(val displayName: String, val description: String) {
    CONSUMABLE("Consumable", "Items are used up and depleted upon cycle finish (e.g. groceries, oil)."),
    REFILLABLE_EXCHANGEABLE("Refillable / Exchangeable", "Containers move to empty/returnable state upon finish (e.g. LPG cylinders, water cans)."),
    REUSABLE_REPLACEABLE("Reusable / Replaceable", "Equipment or parts replaced upon finish (e.g. filters, tyres)."),
    NONE("No Inventory", "Cycles are logged without tracking physical stock.");

    companion object {
        fun fromString(value: String?): InventoryPolicy {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: CONSUMABLE
        }
    }
}

enum class CycleStatus {
    NOT_STARTED,
    ACTIVE,
    PAUSED,
    FINISHED;

    companion object {
        fun fromString(value: String?): CycleStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: ACTIVE
        }
    }
}

/**
 * Domain model representing a Smart Tracker definition.
 */
data class TrackerItem(
    val id: Long = 0,
    val profileId: Long = 1,
    val title: String,
    val emoji: String = "📦",
    val description: String = "",
    val projectId: Long? = null,
    val labelIds: List<Long> = emptyList(),
    val customColorHex: Long? = null,
    val measurements: TrackerMeasurements = TrackerMeasurements(),
    val distanceUnit: DistanceUnit = DistanceUnit.KILOMETERS,
    val quantityUnit: QuantityUnit = QuantityUnit.PIECES,
    val customQuantityUnitName: String = "",
    val inventoryPolicy: InventoryPolicy = InventoryPolicy.CONSUMABLE,
    val currentStock: Double = 0.0,
    val emptyStock: Double = 0.0, // for refillable/exchangeable containers
    val isReminderEnabled: Boolean = false,
    val reminderDaysInterval: Int = 30, // advisory check-in or replacement estimate
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun resolveColor(projectColorHex: Long?): Color {
        return when {
            customColorHex != null -> Color(customColorHex)
            projectColorHex != null -> Color(projectColorHex)
            else -> Color(0xFF3B82F6) // default blue
        }
    }
}

/**
 * Domain model representing a single Usage Cycle for a Tracker.
 */
data class UsageCycleItem(
    val id: Long = 0,
    val trackerId: Long,
    val cycleName: String = "",
    val purchaseId: Long? = null,
    val purchaseCost: Double = 0.0,
    val startDateIso: String,
    val finishDateIso: String? = null,
    val status: CycleStatus = CycleStatus.ACTIVE,
    val isPaused: Boolean = false,
    val startOdometer: Double = 0.0,
    val currentOdometer: Double = 0.0,
    val finishOdometer: Double? = null,
    val initialQuantity: Double = 0.0,
    val remainingQuantity: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalDistance: Double
        get() {
            val end = finishOdometer ?: currentOdometer
            return (end - startOdometer).coerceAtLeast(0.0)
        }

    val quantityConsumed: Double
        get() = (initialQuantity - remainingQuantity).coerceAtLeast(0.0)
}

/**
 * Log entry for intermediate usage or manual adjustments within a cycle.
 */
data class UsageEntryItem(
    val id: Long = 0,
    val cycleId: Long,
    val dateIso: String,
    val odometerReading: Double? = null,
    val quantityDeducted: Double? = null,
    val manualRemainingQuantity: Double? = null,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Purchase record for tracker inventory.
 */
data class TrackerPurchaseItem(
    val id: Long = 0,
    val trackerId: Long,
    val purchaseDateIso: String,
    val quantity: Double,
    val unitPrice: Double,
    val totalCost: Double,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Manual stock adjustment (breakage, count reconciliation, refill return, etc.).
 */
data class InventoryAdjustmentItem(
    val id: Long = 0,
    val trackerId: Long,
    val dateIso: String,
    val deltaQuantity: Double, // positive or negative
    val isRefillExchange: Boolean = false,
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
