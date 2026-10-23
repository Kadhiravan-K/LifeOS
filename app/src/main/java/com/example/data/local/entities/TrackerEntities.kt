package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.tracker.DistanceUnit
import com.example.domain.tracker.InventoryPolicy
import com.example.domain.tracker.QuantityUnit
import com.example.domain.tracker.TrackerItem
import com.example.domain.tracker.TrackerMeasurements
import com.example.domain.tracker.UsageCycleItem
import com.example.domain.tracker.CycleStatus
import com.example.domain.tracker.UsageEntryItem
import com.example.domain.tracker.TrackerPurchaseItem
import com.example.domain.tracker.InventoryAdjustmentItem
import org.json.JSONArray

@Entity(
    tableName = "trackers",
    indices = [
        Index("projectId"),
        Index("isArchived")
    ]
)
data class TrackerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val emoji: String = "📦",
    val description: String = "",
    val projectId: Long? = null,
    val labelIdsJson: String = "[]",
    val customColorHex: Long? = null,
    val trackDuration: Boolean = true,
    val trackDistance: Boolean = false,
    val trackQuantity: Boolean = false,
    val distanceUnit: String = "KILOMETERS",
    val quantityUnit: String = "PIECES",
    val customQuantityUnitName: String = "",
    val inventoryPolicy: String = "CONSUMABLE",
    val currentStock: Double = 0.0,
    val emptyStock: Double = 0.0,
    val isReminderEnabled: Boolean = false,
    val reminderDaysInterval: Int = 30,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): TrackerItem {
        val labels = try {
            val arr = JSONArray(labelIdsJson)
            val list = mutableListOf<Long>()
            for (i in 0 until arr.length()) {
                list.add(arr.getLong(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }

        return TrackerItem(
            id = id,
            title = title,
            emoji = emoji,
            description = description,
            projectId = projectId,
            labelIds = labels,
            customColorHex = customColorHex,
            measurements = TrackerMeasurements(
                trackDuration = trackDuration,
                trackDistance = trackDistance,
                trackQuantity = trackQuantity
            ),
            distanceUnit = DistanceUnit.fromString(distanceUnit),
            quantityUnit = QuantityUnit.fromString(quantityUnit),
            customQuantityUnitName = customQuantityUnitName,
            inventoryPolicy = InventoryPolicy.fromString(inventoryPolicy),
            currentStock = currentStock,
            emptyStock = emptyStock,
            isReminderEnabled = isReminderEnabled,
            reminderDaysInterval = reminderDaysInterval,
            isArchived = isArchived,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(item: TrackerItem): TrackerEntity {
            val labelJson = JSONArray().apply {
                item.labelIds.forEach { put(it) }
            }.toString()

            return TrackerEntity(
                id = item.id,
                title = item.title,
                emoji = item.emoji,
                description = item.description,
                projectId = item.projectId,
                labelIdsJson = labelJson,
                customColorHex = item.customColorHex,
                trackDuration = item.measurements.trackDuration,
                trackDistance = item.measurements.trackDistance,
                trackQuantity = item.measurements.trackQuantity,
                distanceUnit = item.distanceUnit.name,
                quantityUnit = item.quantityUnit.name,
                customQuantityUnitName = item.customQuantityUnitName,
                inventoryPolicy = item.inventoryPolicy.name,
                currentStock = item.currentStock,
                emptyStock = item.emptyStock,
                isReminderEnabled = item.isReminderEnabled,
                reminderDaysInterval = item.reminderDaysInterval,
                isArchived = item.isArchived,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt
            )
        }
    }
}

@Entity(
    tableName = "tracker_cycles",
    indices = [
        Index("trackerId"),
        Index("status")
    ]
)
data class UsageCycleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackerId: Long,
    val cycleName: String = "",
    val purchaseId: Long? = null,
    val purchaseCost: Double = 0.0,
    val startDateIso: String,
    val finishDateIso: String? = null,
    val status: String = "ACTIVE",
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
    fun toDomain(): UsageCycleItem {
        return UsageCycleItem(
            id = id,
            trackerId = trackerId,
            cycleName = cycleName,
            purchaseId = purchaseId,
            purchaseCost = purchaseCost,
            startDateIso = startDateIso,
            finishDateIso = finishDateIso,
            status = CycleStatus.fromString(status),
            isPaused = isPaused,
            startOdometer = startOdometer,
            currentOdometer = currentOdometer,
            finishOdometer = finishOdometer,
            initialQuantity = initialQuantity,
            remainingQuantity = remainingQuantity,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(item: UsageCycleItem): UsageCycleEntity {
            return UsageCycleEntity(
                id = item.id,
                trackerId = item.trackerId,
                cycleName = item.cycleName,
                purchaseId = item.purchaseId,
                purchaseCost = item.purchaseCost,
                startDateIso = item.startDateIso,
                finishDateIso = item.finishDateIso,
                status = item.status.name,
                isPaused = item.isPaused,
                startOdometer = item.startOdometer,
                currentOdometer = item.currentOdometer,
                finishOdometer = item.finishOdometer,
                initialQuantity = item.initialQuantity,
                remainingQuantity = item.remainingQuantity,
                notes = item.notes,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt
            )
        }
    }
}

@Entity(
    tableName = "tracker_usage_entries",
    indices = [
        Index("cycleId")
    ]
)
data class UsageEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cycleId: Long,
    val dateIso: String,
    val odometerReading: Double? = null,
    val quantityDeducted: Double? = null,
    val manualRemainingQuantity: Double? = null,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain() = UsageEntryItem(id, cycleId, dateIso, odometerReading, quantityDeducted, manualRemainingQuantity, notes, timestamp)

    companion object {
        fun fromDomain(item: UsageEntryItem) = UsageEntryEntity(
            item.id, item.cycleId, item.dateIso, item.odometerReading, item.quantityDeducted, item.manualRemainingQuantity, item.notes, item.timestamp
        )
    }
}

@Entity(
    tableName = "tracker_purchases",
    indices = [
        Index("trackerId")
    ]
)
data class TrackerPurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackerId: Long,
    val purchaseDateIso: String,
    val quantity: Double,
    val unitPrice: Double,
    val totalCost: Double,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = TrackerPurchaseItem(id, trackerId, purchaseDateIso, quantity, unitPrice, totalCost, notes, createdAt)

    companion object {
        fun fromDomain(item: TrackerPurchaseItem) = TrackerPurchaseEntity(
            item.id, item.trackerId, item.purchaseDateIso, item.quantity, item.unitPrice, item.totalCost, item.notes, item.createdAt
        )
    }
}

@Entity(
    tableName = "tracker_inventory_adjustments",
    indices = [
        Index("trackerId")
    ]
)
data class InventoryAdjustmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackerId: Long,
    val dateIso: String,
    val deltaQuantity: Double,
    val isRefillExchange: Boolean = false,
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain() = InventoryAdjustmentItem(id, trackerId, dateIso, deltaQuantity, isRefillExchange, reason, timestamp)

    companion object {
        fun fromDomain(item: InventoryAdjustmentItem) = InventoryAdjustmentEntity(
            item.id, item.trackerId, item.dateIso, item.deltaQuantity, item.isRefillExchange, item.reason, item.timestamp
        )
    }
}
