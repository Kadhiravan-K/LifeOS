package com.example.domain.tracker

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Calculation and analytics engine for Smart Trackers.
 * Pure JVM, testable, and deterministic.
 */
object TrackerCalculationEngine {

    private val ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Calculates the elapsed calendar days between two ISO dates (inclusive of start day).
     * If start and end are identical, returns 1 day.
     */
    fun calculateElapsedDays(startDateIso: String, endDateIso: String): Long {
        return try {
            val start = LocalDate.parse(startDateIso, ISO_FORMATTER)
            val end = LocalDate.parse(endDateIso, ISO_FORMATTER)
            if (end.isBefore(start)) {
                0L
            } else {
                ChronoUnit.DAYS.between(start, end) + 1L
            }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Calculates approximate months elapsed (e.g., 45 days ~ 1.5 months).
     */
    fun calculateApproximateMonths(elapsedDays: Long): Double {
        if (elapsedDays <= 0) return 0.0
        return Math.round((elapsedDays / 30.4375) * 10.0) / 10.0
    }

    /**
     * Computes the average duration in calendar days across completed (FINISHED) cycles only.
     * Active, paused, or unfinished cycles are explicitly excluded from the average.
     */
    fun calculateAverageCompletedDuration(cycles: List<UsageCycleItem>): Double {
        val completedDurations = cycles
            .filter { it.status == CycleStatus.FINISHED && it.finishDateIso != null }
            .map { calculateElapsedDays(it.startDateIso, it.finishDateIso!!) }

        if (completedDurations.isEmpty()) return 0.0
        val sum = completedDurations.sum()
        return Math.round((sum.toDouble() / completedDurations.size) * 10.0) / 10.0
    }

    /**
     * Computes min and max durations for completed cycles.
     */
    fun calculateMinMaxCompletedDuration(cycles: List<UsageCycleItem>): Pair<Long, Long>? {
        val completedDurations = cycles
            .filter { it.status == CycleStatus.FINISHED && it.finishDateIso != null }
            .map { calculateElapsedDays(it.startDateIso, it.finishDateIso!!) }

        if (completedDurations.isEmpty()) return null
        return Pair(completedDurations.minOrNull() ?: 0L, completedDurations.maxOrNull() ?: 0L)
    }

    /**
     * Computes cost per day for a cycle.
     */
    fun calculateCostPerDay(cost: Double, elapsedDays: Long): Double {
        if (cost <= 0.0 || elapsedDays <= 0) return 0.0
        return Math.round((cost / elapsedDays) * 100.0) / 100.0
    }

    /**
     * Computes cost per distance unit (km or mi).
     */
    fun calculateCostPerDistance(cost: Double, totalDistance: Double): Double {
        if (cost <= 0.0 || totalDistance <= 0.0) return 0.0
        return Math.round((cost / totalDistance) * 100.0) / 100.0
    }

    /**
     * Validates odometer update. An odometer reading must be >= starting odometer and >= current odometer.
     * Returns true if valid, false if invalid/regressed.
     */
    fun isOdometerValid(newReading: Double, currentReading: Double, startReading: Double): Boolean {
        return newReading >= startReading && newReading >= currentReading
    }

    /**
     * Computes the current stock reconciliation based on purchases, usage cycles, and manual adjustments.
     */
    fun calculateReconciledStock(
        purchases: List<TrackerPurchaseItem>,
        activeCycles: List<UsageCycleItem>,
        finishedCycles: List<UsageCycleItem>,
        adjustments: List<InventoryAdjustmentItem>,
        policy: InventoryPolicy
    ): Pair<Double, Double> {
        val totalPurchased = purchases.sumOf { it.quantity }
        val manualAdjustmentsSum = adjustments.sumOf { it.deltaQuantity }

        // Each cycle started takes 1 unit into "in-use"
        val inUseCount = activeCycles.count { it.status == CycleStatus.ACTIVE || it.status == CycleStatus.PAUSED }.toDouble()

        return when (policy) {
            InventoryPolicy.CONSUMABLE -> {
                val finishedCount = finishedCycles.size.toDouble()
                val availableSpare = (totalPurchased + manualAdjustmentsSum - inUseCount - finishedCount).coerceAtLeast(0.0)
                Pair(availableSpare, 0.0)
            }
            InventoryPolicy.REFILLABLE_EXCHANGEABLE -> {
                // When a refillable cycle finishes, it moves to empty stock
                val finishedCount = finishedCycles.size.toDouble()
                val refillReturns = adjustments.filter { it.isRefillExchange }.sumOf { it.deltaQuantity }
                val availableFilled = (totalPurchased + manualAdjustmentsSum - inUseCount - finishedCount).coerceAtLeast(0.0)
                val emptyStock = (finishedCount - refillReturns).coerceAtLeast(0.0)
                Pair(availableFilled, emptyStock)
            }
            InventoryPolicy.REUSABLE_REPLACEABLE -> {
                val finishedCount = finishedCycles.size.toDouble()
                val availableSpare = (totalPurchased + manualAdjustmentsSum - inUseCount - finishedCount).coerceAtLeast(0.0)
                Pair(availableSpare, 0.0)
            }
            InventoryPolicy.NONE -> Pair(0.0, 0.0)
        }
    }
}
