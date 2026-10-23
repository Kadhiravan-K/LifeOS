package com.example.domain.essentials

import com.example.domain.subscription.BillingFrequency
import com.example.domain.subscription.MissingDatePolicy
import com.example.domain.subscription.SubscriptionItem
import com.example.domain.tracker.DistanceUnit
import com.example.domain.tracker.InventoryPolicy
import com.example.domain.tracker.QuantityUnit
import com.example.domain.tracker.TrackerItem
import com.example.domain.tracker.TrackerMeasurements
import java.time.LocalDate

data class TrackerTemplate(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String,
    val category: String,
    val measurements: TrackerMeasurements,
    val distanceUnit: DistanceUnit = DistanceUnit.KILOMETERS,
    val quantityUnit: QuantityUnit = QuantityUnit.PIECES,
    val inventoryPolicy: InventoryPolicy = InventoryPolicy.NONE,
    val currentStock: Double = 0.0,
    val emptyStock: Double = 0.0,
    val reminderDaysInterval: Int = 30
) {
    fun toTrackerItem(projectId: Long? = null): TrackerItem = TrackerItem(
        title = title,
        emoji = emoji,
        description = description,
        projectId = projectId,
        measurements = measurements,
        distanceUnit = distanceUnit,
        quantityUnit = quantityUnit,
        inventoryPolicy = inventoryPolicy,
        currentStock = currentStock,
        emptyStock = emptyStock,
        isReminderEnabled = reminderDaysInterval > 0,
        reminderDaysInterval = reminderDaysInterval
    )
}

data class SubscriptionTemplate(
    val id: String,
    val title: String,
    val emoji: String,
    val category: String,
    val defaultAmount: Double,
    val currency: String = "₹",
    val frequency: BillingFrequency = BillingFrequency.MONTHLY,
    val reminderDaysBefore: Int = 3,
    val notes: String = ""
) {
    fun toSubscriptionItem(projectId: Long? = null, todayIso: String = LocalDate.now().toString()): SubscriptionItem {
        val today = LocalDate.now()
        return SubscriptionItem(
            title = title,
            emoji = emoji,
            description = "Category: $category",
            projectId = projectId,
            amount = defaultAmount,
            currency = currency,
            frequency = frequency,
            anchorDateIso = todayIso,
            anchorDayOfMonth = today.dayOfMonth,
            nextRenewalDateIso = todayIso,
            reminderDaysBefore = reminderDaysBefore,
            notes = notes
        )
    }
}

object EssentialsCatalog {
    val trackerTemplates = listOf(
        TrackerTemplate(
            id = "car_engine_oil",
            title = "Car Engine Oil & Service",
            emoji = "🚗",
            description = "Track vehicle mileage and time between oil changes",
            category = "Vehicles",
            measurements = TrackerMeasurements(trackDuration = true, trackDistance = true, trackQuantity = false),
            distanceUnit = DistanceUnit.KILOMETERS,
            inventoryPolicy = InventoryPolicy.REFILLABLE_EXCHANGEABLE,
            currentStock = 1.0,
            reminderDaysInterval = 180
        ),
        TrackerTemplate(
            id = "running_shoes",
            title = "Running Shoes Mileage",
            emoji = "👟",
            description = "Track distance run to replace shoes before injury (recommended ~600km)",
            category = "Fitness",
            measurements = TrackerMeasurements(trackDuration = true, trackDistance = true, trackQuantity = false),
            distanceUnit = DistanceUnit.KILOMETERS,
            inventoryPolicy = InventoryPolicy.REUSABLE_REPLACEABLE,
            reminderDaysInterval = 365
        ),
        TrackerTemplate(
            id = "water_purifier_filter",
            title = "Water Purifier Filter",
            emoji = "💧",
            description = "Track RO/UV filter replacement cycle and spare filters inventory",
            category = "Home",
            measurements = TrackerMeasurements(trackDuration = true, trackDistance = false, trackQuantity = true),
            quantityUnit = QuantityUnit.PIECES,
            inventoryPolicy = InventoryPolicy.CONSUMABLE,
            currentStock = 2.0,
            reminderDaysInterval = 180
        ),
        TrackerTemplate(
            id = "gas_cylinder",
            title = "LPG Gas Cylinder",
            emoji = "🔥",
            description = "Track how many days a cylinder lasts and manage refill spares",
            category = "Home",
            measurements = TrackerMeasurements(trackDuration = true, trackDistance = false, trackQuantity = true),
            quantityUnit = QuantityUnit.PIECES,
            inventoryPolicy = InventoryPolicy.REFILLABLE_EXCHANGEABLE,
            currentStock = 1.0,
            emptyStock = 1.0,
            reminderDaysInterval = 45
        ),
        TrackerTemplate(
            id = "contact_lenses",
            title = "Monthly Contact Lenses",
            emoji = "👁️",
            description = "Track 30-day lens cycle and remaining box supply",
            category = "Health",
            measurements = TrackerMeasurements(trackDuration = true, trackDistance = false, trackQuantity = true),
            quantityUnit = QuantityUnit.PIECES,
            inventoryPolicy = InventoryPolicy.CONSUMABLE,
            currentStock = 6.0,
            reminderDaysInterval = 30
        ),
        TrackerTemplate(
            id = "toothbrush_head",
            title = "Electric Toothbrush Head",
            emoji = "🪥",
            description = "Replace brush head every 3 months for optimal oral hygiene",
            category = "Personal Care",
            measurements = TrackerMeasurements(trackDuration = true, trackDistance = false, trackQuantity = true),
            quantityUnit = QuantityUnit.PIECES,
            inventoryPolicy = InventoryPolicy.CONSUMABLE,
            currentStock = 4.0,
            reminderDaysInterval = 90
        ),
        TrackerTemplate(
            id = "printer_ink",
            title = "Printer Toner / Ink Cartridge",
            emoji = "🖨️",
            description = "Track page count output and backup cartridge stock",
            category = "Office",
            measurements = TrackerMeasurements(trackDuration = true, trackDistance = false, trackQuantity = true),
            quantityUnit = QuantityUnit.PIECES,
            inventoryPolicy = InventoryPolicy.CONSUMABLE,
            currentStock = 1.0,
            reminderDaysInterval = 120
        )
    )

    val subscriptionTemplates = listOf(
        SubscriptionTemplate(
            id = "netflix",
            title = "Netflix",
            emoji = "🎬",
            category = "Entertainment",
            defaultAmount = 499.0,
            frequency = BillingFrequency.MONTHLY,
            reminderDaysBefore = 3
        ),
        SubscriptionTemplate(
            id = "spotify",
            title = "Spotify",
            emoji = "🎵",
            category = "Entertainment",
            defaultAmount = 119.0,
            frequency = BillingFrequency.MONTHLY,
            reminderDaysBefore = 3
        ),
        SubscriptionTemplate(
            id = "youtube_premium",
            title = "YouTube Premium",
            emoji = "▶️",
            category = "Entertainment",
            defaultAmount = 149.0,
            frequency = BillingFrequency.MONTHLY,
            reminderDaysBefore = 3
        ),
        SubscriptionTemplate(
            id = "google_one",
            title = "Google One (Cloud Storage)",
            emoji = "☁️",
            category = "Software & Cloud",
            defaultAmount = 130.0,
            frequency = BillingFrequency.MONTHLY,
            reminderDaysBefore = 3
        ),
        SubscriptionTemplate(
            id = "icloud",
            title = "Apple iCloud+",
            emoji = "🍏",
            category = "Software & Cloud",
            defaultAmount = 75.0,
            frequency = BillingFrequency.MONTHLY,
            reminderDaysBefore = 3
        ),
        SubscriptionTemplate(
            id = "amazon_prime",
            title = "Amazon Prime",
            emoji = "📦",
            category = "Shopping",
            defaultAmount = 1499.0,
            frequency = BillingFrequency.YEARLY,
            reminderDaysBefore = 7
        ),
        SubscriptionTemplate(
            id = "gym_membership",
            title = "Fitness / Gym Membership",
            emoji = "🏋️",
            category = "Health & Fitness",
            defaultAmount = 1500.0,
            frequency = BillingFrequency.MONTHLY,
            reminderDaysBefore = 5
        ),
        SubscriptionTemplate(
            id = "home_internet",
            title = "Home Fiber Internet",
            emoji = "🌐",
            category = "Utilities",
            defaultAmount = 799.0,
            frequency = BillingFrequency.MONTHLY,
            reminderDaysBefore = 4
        )
    )
}
