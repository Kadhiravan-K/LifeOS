package com.example.domain.medication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationScheduleEngineTest {

    @Test
    fun `test meal based schedule returns enabled meal period times`() {
        val config = MedicationScheduleConfig(
            scheduleType = MedicineScheduleType.MEAL_BASED,
            mealPeriods = listOf(
                MealPeriodConfig(MealPeriod.MORNING, "07:30", FoodRelationship.BEFORE_FOOD, 30, isEnabled = true),
                MealPeriodConfig(MealPeriod.LUNCH, "13:00", FoodRelationship.WITH_FOOD, 0, isEnabled = false),
                MealPeriodConfig(MealPeriod.DINNER, "19:45", FoodRelationship.AFTER_FOOD, 15, isEnabled = true)
            ),
            startDateIso = "2026-10-01"
        )

        val times = MedicationScheduleEngine.calculateDailyDoseTimes(config, "2026-10-23")
        assertEquals(2, times.size)
        assertEquals("07:30", times[0])
        assertEquals("19:45", times[1])
    }

    @Test
    fun `test multiple fixed times schedule deduplicates and sorts chronologically`() {
        val config = MedicationScheduleConfig(
            scheduleType = MedicineScheduleType.FIXED_TIMES,
            fixedTimes = listOf("20:00", "08:00", "14:00", "08:00", "20:00"),
            startDateIso = "2026-10-01"
        )

        val times = MedicationScheduleEngine.calculateDailyDoseTimes(config, "2026-10-23")
        assertEquals(3, times.size)
        assertEquals("08:00", times[0])
        assertEquals("14:00", times[1])
        assertEquals("20:00", times[2])
    }

    @Test
    fun `test repeating intervals schedule within waking hours`() {
        val config = MedicationScheduleConfig(
            scheduleType = MedicineScheduleType.INTERVAL,
            intervalHours = 4,
            intervalAnchorTime = "08:00",
            allowOvernight = false,
            startDateIso = "2026-10-01"
        )

        // Wake: 07:00 (420m), Bed: 22:00 (1320m)
        val times = MedicationScheduleEngine.calculateDailyDoseTimes(
            config,
            targetDateIso = "2026-10-23",
            wakeUpMinutes = 420,
            bedTimeMinutes = 1320
        )
        // 08:00, 12:00, 16:00, 20:00
        assertEquals(4, times.size)
        assertEquals("08:00", times[0])
        assertEquals("12:00", times[1])
        assertEquals("16:00", times[2])
        assertEquals("20:00", times[3])
    }

    @Test
    fun `test selected weekdays schedule only generates times on matching days`() {
        // 2026-10-23 is a Friday
        // 2026-10-24 is a Saturday
        val config = MedicationScheduleConfig(
            scheduleType = MedicineScheduleType.WEEKDAYS,
            selectedDaysOfWeek = listOf("MONDAY", "WEDNESDAY", "FRIDAY"),
            fixedTimes = listOf("09:00"),
            startDateIso = "2026-10-01"
        )

        val fridayTimes = MedicationScheduleEngine.calculateDailyDoseTimes(config, "2026-10-23")
        assertEquals(1, fridayTimes.size)
        assertEquals("09:00", fridayTimes[0])

        val saturdayTimes = MedicationScheduleEngine.calculateDailyDoseTimes(config, "2026-10-24")
        assertTrue(saturdayTimes.isEmpty())
    }

    @Test
    fun `test course limited schedule respects end date and duration`() {
        val config = MedicationScheduleConfig(
            scheduleType = MedicineScheduleType.FIXED_TIMES,
            fixedTimes = listOf("08:00", "20:00"),
            startDateIso = "2026-10-15",
            courseDurationDays = 7 // Ends 2026-10-21
        )

        val activeDayTimes = MedicationScheduleEngine.calculateDailyDoseTimes(config, "2026-10-20")
        assertEquals(2, activeDayTimes.size)

        val afterEndTimes = MedicationScheduleEngine.calculateDailyDoseTimes(config, "2026-10-23")
        assertTrue(afterEndTimes.isEmpty())
    }

    @Test
    fun `test as needed PRN medication does not schedule automated doses`() {
        val config = MedicationScheduleConfig(
            scheduleType = MedicineScheduleType.AS_NEEDED,
            isAsNeeded = true,
            startDateIso = "2026-10-01"
        )

        val times = MedicationScheduleEngine.calculateDailyDoseTimes(config, "2026-10-23")
        assertTrue(times.isEmpty())
    }

    @Test
    fun `test before start date generates no doses`() {
        val config = MedicationScheduleConfig(
            scheduleType = MedicineScheduleType.FIXED_TIMES,
            fixedTimes = listOf("08:00"),
            startDateIso = "2026-10-25"
        )

        val times = MedicationScheduleEngine.calculateDailyDoseTimes(config, "2026-10-23")
        assertTrue(times.isEmpty())
    }

    @Test
    fun `test formatScheduleSummary across modes`() {
        val mealConfig = MedicationScheduleConfig(
            scheduleType = MedicineScheduleType.MEAL_BASED,
            mealPeriods = listOf(
                MealPeriodConfig(MealPeriod.MORNING, "08:00", FoodRelationship.BEFORE_FOOD, 30, isEnabled = true),
                MealPeriodConfig(MealPeriod.DINNER, "20:00", FoodRelationship.AFTER_FOOD, 15, isEnabled = true)
            )
        )
        val mealSummary = MedicationScheduleEngine.formatScheduleSummary(mealConfig)
        assertTrue(mealSummary.contains("Before Morning (08:00)"))
        assertTrue(mealSummary.contains("After Dinner (20:00)"))

        val fixedConfig = MedicationScheduleConfig(
            scheduleType = MedicineScheduleType.FIXED_TIMES,
            fixedTimes = listOf("08:00", "20:00")
        )
        val fixedSummary = MedicationScheduleEngine.formatScheduleSummary(fixedConfig)
        assertEquals("2 doses daily: 08:00, 20:00", fixedSummary)

        val intervalConfig = MedicationScheduleConfig(
            scheduleType = MedicineScheduleType.INTERVAL,
            intervalHours = 6
        )
        val intervalSummary = MedicationScheduleEngine.formatScheduleSummary(intervalConfig)
        assertTrue(intervalSummary.contains("Every 6h"))

        val prnConfig = MedicationScheduleConfig(
            scheduleType = MedicineScheduleType.AS_NEEDED,
            isAsNeeded = true
        )
        val prnSummary = MedicationScheduleEngine.formatScheduleSummary(prnConfig)
        assertEquals("As-needed (PRN) • Log doses manually", prnSummary)
    }

    @Test
    fun `test findNextDoseTime finds chronological upcoming dose`() {
        val times = listOf("08:00", "14:00", "20:00")

        // At 10:00, the 14:00 dose is next
        val nextAt10 = MedicationScheduleEngine.findNextDoseTime(times, "10:00")
        assertEquals("14:00", nextAt10)

        // At 15:00, the 20:00 dose is next
        val nextAt15 = MedicationScheduleEngine.findNextDoseTime(times, "15:00")
        assertEquals("20:00", nextAt15)

        // At 21:00, all doses passed -> wraps to first dose tomorrow
        val nextAt21 = MedicationScheduleEngine.findNextDoseTime(times, "21:00")
        assertEquals("08:00", nextAt21)
    }
}
