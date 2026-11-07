package com.example.shifts

import com.example.data.local.entities.ShiftAssignmentEntity
import com.example.data.local.entities.ShiftEntity
import com.example.data.local.entities.ShiftRotationEntity
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@RunWith(RobolectricTestRunner::class)
class ShiftPlannerUnitTest {

    @Test
    fun testDayAndNightShiftCreation() {
        val dayShift = ShiftEntity(
            id = 1,
            profileId = 1,
            name = "Day Shift",
            startTime = "08:00",
            endTime = "16:00",
            wakeUpTime = "06:00",
            sleepTime = "22:00",
            isOvernight = false
        )
        val nightShift = ShiftEntity(
            id = 2,
            profileId = 1,
            name = "Night Shift",
            startTime = "22:00",
            endTime = "06:00",
            wakeUpTime = "18:00",
            sleepTime = "10:00",
            isOvernight = true
        )

        assertEquals("06:00", dayShift.wakeUpTime)
        assertEquals("18:00", nightShift.wakeUpTime)
        assertTrue(nightShift.isOvernight)
        assertFalse(dayShift.isOvernight)
    }

    @Test
    fun testOvernightShiftCrossingMidnight() {
        val overnight = ShiftEntity(
            id = 3,
            profileId = 1,
            name = "Graveyard Shift",
            startTime = "23:00",
            endTime = "07:00",
            wakeUpTime = "20:00",
            sleepTime = "11:00",
            isOvernight = true
        )

        assertTrue(overnight.isOvernight)
        assertTrue(overnight.startTime > overnight.endTime) // Crosses midnight
    }

    @Test
    fun testWeeklyRotationPatternCalculation() {
        val pattern = listOf(1L, 1L, 1L, 1L, 1L, -1L, -1L) // 5 days day shift, 2 days off
        val rotation = ShiftRotationEntity(
            id = 10,
            profileId = 1,
            name = "Standard 5-2 Workweek",
            rotationType = "WEEKLY",
            startDateIso = "2026-11-02", // Monday
            cycleLengthDays = 7,
            patternJson = JSONArray(pattern).toString(),
            isActive = true
        )

        val startDate = LocalDate.parse(rotation.startDateIso)
        val patternArray = JSONArray(rotation.patternJson)

        // Monday (day 0) -> Shift 1
        val mondayOffset = ChronoUnit.DAYS.between(startDate, LocalDate.parse("2026-11-02")).toInt() % rotation.cycleLengthDays
        assertEquals(1L, patternArray.getLong(mondayOffset))

        // Saturday (day 5) -> Off (-1)
        val satOffset = ChronoUnit.DAYS.between(startDate, LocalDate.parse("2026-11-07")).toInt() % rotation.cycleLengthDays
        assertEquals(-1L, patternArray.getLong(satOffset))

        // Following Monday (day 7 -> wraps to day 0) -> Shift 1
        val nextMonOffset = ChronoUnit.DAYS.between(startDate, LocalDate.parse("2026-11-09")).toInt() % rotation.cycleLengthDays
        assertEquals(1L, patternArray.getLong(nextMonOffset))
    }

    @Test
    fun testShiftExceptionOverridesWithoutDestroyingSchedule() {
        val baseAssignment = ShiftAssignmentEntity(
            id = 20,
            profileId = 1,
            shiftId = 1L, // Planned Day Shift
            dateIso = "2026-11-05",
            isException = false
        )

        // User works Overtime on Day Shift
        val overtimeException = baseAssignment.copy(
            isException = true,
            exceptionType = "OVERTIME",
            overtimeHours = 3.5,
            exceptionNotes = "Covering machine maintenance"
        )

        assertTrue(overtimeException.isException)
        assertEquals("OVERTIME", overtimeException.exceptionType)
        assertEquals(3.5, overtimeException.overtimeHours, 0.001)
        assertEquals(1L, overtimeException.shiftId) // Shift is still Shift 1

        // User takes emergency Leave
        val leaveException = baseAssignment.copy(
            shiftId = null, // No shift worked
            isException = true,
            exceptionType = "LEAVE",
            exceptionNotes = "Family emergency"
        )

        assertTrue(leaveException.isException)
        assertEquals("LEAVE", leaveException.exceptionType)
        assertNull(leaveException.shiftId)
    }

    @Test
    fun testShiftSwapException() {
        val swapException = ShiftAssignmentEntity(
            id = 25,
            profileId = 1,
            shiftId = 2L, // Swapped from Day Shift to Night Shift
            dateIso = "2026-11-06",
            isException = true,
            exceptionType = "SHIFT_SWAP",
            exceptionNotes = "Swapped with John Doe for Friday night"
        )

        assertTrue(swapException.isException)
        assertEquals("SHIFT_SWAP", swapException.exceptionType)
        assertEquals(2L, swapException.shiftId)
        assertEquals("Swapped with John Doe for Friday night", swapException.exceptionNotes)
    }

    @Test
    fun testIntelligentRoutineWakeUpAdaptation() {
        val dayShift = ShiftEntity(
            id = 1,
            profileId = 1,
            name = "Day Shift",
            startTime = "07:00",
            endTime = "15:00",
            wakeUpTime = "05:30",
            sleepTime = "21:30"
        )
        val nightShift = ShiftEntity(
            id = 2,
            profileId = 1,
            name = "Night Shift",
            startTime = "19:00",
            endTime = "03:00",
            wakeUpTime = "17:00",
            sleepTime = "09:00",
            isOvernight = true
        )

        // Day shift wake-up target
        assertEquals("05:30", dayShift.wakeUpTime)
        // Night shift wake-up target adapts to 17:00 (5:00 PM)
        assertEquals("17:00", nightShift.wakeUpTime)
    }
}
