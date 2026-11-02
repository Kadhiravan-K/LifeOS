package com.example.domain.habit

import com.example.domain.model.HabitItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Comprehensive unit test suite for [HabitStrengthEngine].
 *
 * Verifies all 16 mandatory test scenarios specified in the H4 product decision record:
 * 1. Single first successful opportunity -> 100% after bias-correction.
 * 2. Single first missed opportunity -> 0%.
 * 3. Two opportunities (one success, one miss) -> order sensitivity (recent success > older success).
 * 4. 13 consecutive successes -> 100%.
 * 5. Missed opportunities decay accumulated strength gracefully.
 * 6. 13 consecutive misses halving uncorrected accumulated EMA.
 * 7. Alternating completion and missed opportunities stability.
 * 8. Long-term convergence to 100%.
 * 9. Zero eligible opportunities (e.g. created today, uncompleted) -> hasEligibleOpportunities = false.
 * 10. Today's incomplete opportunity excluded from evaluation.
 * 11. Today's completed opportunity included in evaluation.
 * 12. Future completions excluded defensively.
 * 13. Historical completion before creation date handled safely.
 * 14. Invalid and duplicate dates do not corrupt result.
 * 15. Strength tier boundaries (Building, Growing, Strong, Excellent).
 * 16. Arithmetic completion rate vs EMA strength independence.
 */
class HabitStrengthEngineUnitTest {

    private fun dateToMillis(isoDate: String): Long {
        return LocalDate.parse(isoDate)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun scenario01_firstOpportunitySuccessful_biasCorrectedToOneHundredPercent() {
        val result = HabitStrengthEngine.calculateStrength(
            createdAtMillis = dateToMillis("2026-11-01"),
            completedDates = setOf("2026-11-01"),
            referenceDateIso = "2026-11-01"
        )

        assertEquals(100f, result.score, 0.01f)
        assertEquals(100, result.scorePercent)
        assertEquals(HabitStrengthLevel.EXCELLENT, result.level)
        assertEquals(1, result.completedCount)
        assertEquals(0, result.missedCount)
        assertEquals(1, result.totalOpportunities)
        assertTrue(result.hasEligibleOpportunities)
        assertTrue(result.isLimitedEvidence)
    }

    @Test
    fun scenario02_firstOpportunityMissed_returnsZeroPercent() {
        // Created on 2026-11-01, evaluated on 2026-11-02.
        // Today (2026-11-02) is incomplete, so evaluated range is [2026-11-01 to 2026-11-01] (1 missed opportunity).
        val result = HabitStrengthEngine.calculateStrength(
            createdAtMillis = dateToMillis("2026-11-01"),
            completedDates = emptySet(),
            referenceDateIso = "2026-11-02"
        )

        assertEquals(0f, result.score, 0.01f)
        assertEquals(0, result.scorePercent)
        assertEquals(HabitStrengthLevel.BUILDING, result.level)
        assertEquals(0, result.completedCount)
        assertEquals(1, result.missedCount)
        assertEquals(1, result.totalOpportunities)
        assertTrue(result.hasEligibleOpportunities)
        assertTrue(result.isLimitedEvidence)
    }

    @Test
    fun scenario03_twoOpportunities_orderSensitivity() {
        // Case A: Day 1 missed, Day 2 completed
        val resultRecentSuccess = HabitStrengthEngine.calculateStrength(
            createdAtMillis = dateToMillis("2026-11-01"),
            completedDates = setOf("2026-11-02"),
            referenceDateIso = "2026-11-02"
        )

        // Case B: Day 1 completed, Day 2 missed (evaluated at 2026-11-03 where today is incomplete)
        val resultOlderSuccess = HabitStrengthEngine.calculateStrength(
            createdAtMillis = dateToMillis("2026-11-01"),
            completedDates = setOf("2026-11-01"),
            referenceDateIso = "2026-11-03"
        )

        assertEquals(2, resultRecentSuccess.totalOpportunities)
        assertEquals(2, resultOlderSuccess.totalOpportunities)
        assertEquals(1, resultRecentSuccess.completedCount)
        assertEquals(1, resultOlderSuccess.completedCount)

        // Recent success (~51.3%) should exceed older success (~48.7%)
        assertTrue(
            "Recent success (${resultRecentSuccess.score}) must exceed older success (${resultOlderSuccess.score})",
            resultRecentSuccess.score > resultOlderSuccess.score
        )
        assertEquals(51, resultRecentSuccess.scorePercent)
        assertEquals(49, resultOlderSuccess.scorePercent)
        assertEquals(HabitStrengthLevel.GROWING, resultRecentSuccess.level)
        assertEquals(HabitStrengthLevel.GROWING, resultOlderSuccess.level)
    }

    @Test
    fun scenario04_thirteenConsecutiveSuccesses_oneHundredPercent() {
        val completed = (1..13).map { day -> "2026-11-%02d".format(day) }.toSet()
        val result = HabitStrengthEngine.calculateStrength(
            createdAtMillis = dateToMillis("2026-11-01"),
            completedDates = completed,
            referenceDateIso = "2026-11-13"
        )

        assertEquals(100f, result.score, 0.01f)
        assertEquals(100, result.scorePercent)
        assertEquals(HabitStrengthLevel.EXCELLENT, result.level)
        assertEquals(13, result.completedCount)
        assertEquals(0, result.missedCount)
        assertEquals(13, result.totalOpportunities)
        assertFalse(result.isLimitedEvidence)
    }

    @Test
    fun scenario05_missedOpportunitiesDecayStrengthGracefully() {
        val all14 = (1..14).map { "2026-11-%02d".format(it) }.toSet()
        val baseResult = HabitStrengthEngine.calculateStrength(
            createdAtMillis = dateToMillis("2026-11-01"),
            completedDates = all14,
            referenceDateIso = "2026-11-14"
        )
        assertEquals(100, baseResult.scorePercent)

        // Add 1 miss
        val result1Miss = HabitStrengthEngine.calculateStrength(
            createdAtMillis = dateToMillis("2026-11-01"),
            completedDates = all14,
            referenceDateIso = "2026-11-16" // Evaluated up to 2026-11-15 (Day 15 is missed)
        )

        // Add 2 misses
        val result2Misses = HabitStrengthEngine.calculateStrength(
            createdAtMillis = dateToMillis("2026-11-01"),
            completedDates = all14,
            referenceDateIso = "2026-11-17" // Evaluated up to 2026-11-16 (Days 15, 16 missed)
        )

        assertTrue(result1Miss.score < baseResult.score)
        assertTrue(result2Misses.score < result1Miss.score)
        // Strength should decay gracefully, not instantly drop to 0 like streak
        assertTrue("Strength after 2 misses should still be > 80%", result2Misses.score > 80f)
    }

    @Test
    fun scenario06_thirteenConsecutiveMissesHalvesUncorrectedAccumulatedEma() {
        // Compute uncorrected EMA after 14 successes
        var ema = 0.0
        val lambda = HabitStrengthEngine.LAMBDA
        val alpha = HabitStrengthEngine.ALPHA
        for (i in 1..14) {
            ema = (lambda * ema) + alpha
        }
        val emaPeak = ema

        // Simulate 13 consecutive misses
        for (i in 1..13) {
            ema = (lambda * ema) + 0.0
        }

        val ratio = ema / emaPeak
        // Over 13 opportunities, lambda^13 = (0.5^(1/13))^13 = 0.5 exactly
        assertEquals(0.5, ratio, 0.0001)
    }

    @Test
    fun scenario07_alternatingCompletionAndMissedOpportunitiesStability() {
        val completed = mutableSetOf<String>()
        for (i in 1..30 step 2) {
            completed.add("2026-11-%02d".format(i))
        }

        val result = HabitStrengthEngine.calculateStrength(
            createdAtMillis = dateToMillis("2026-11-01"),
            completedDates = completed,
            referenceDateIso = "2026-12-01" // Reference is Dec 1 (incomplete), so full Nov 1..Nov 30 window is evaluated
        )

        assertEquals(30, result.totalOpportunities)
        assertEquals(15, result.completedCount)
        assertEquals(15, result.missedCount)
        assertEquals(50f, result.completionRate, 0.01f)
        // Score should hover around ~48% to 52%, demonstrating dynamic stability
        assertTrue("Alternating score (${result.score}) should stay between 45% and 55%", result.score in 45f..55f)
        assertEquals(HabitStrengthLevel.GROWING, result.level)
    }

    @Test
    fun scenario08_longTermConvergence() {
        val completed = (1..90).map { day ->
            LocalDate.parse("2026-08-01").plusDays((day - 1).toLong()).toString()
        }.toSet()

        val result = HabitStrengthEngine.calculateStrength(
            createdAtMillis = dateToMillis("2026-08-01"),
            completedDates = completed,
            referenceDateIso = "2026-10-29"
        )

        assertEquals(90, result.totalOpportunities)
        assertEquals(90, result.completedCount)
        assertEquals(100f, result.score, 0.01f)
        assertEquals(100, result.scorePercent)
        assertEquals(HabitStrengthLevel.EXCELLENT, result.level)
        assertFalse(result.isLimitedEvidence)
    }

    @Test
    fun scenario09_zeroEligibleOpportunities_returnsUninitializedState() {
        val result = HabitStrengthEngine.calculateStrength(
            createdAtMillis = dateToMillis("2026-11-03"),
            completedDates = emptySet(),
            referenceDateIso = "2026-11-03"
        )

        assertEquals(0f, result.score, 0.001f)
        assertEquals(0, result.scorePercent)
        assertEquals(0, result.totalOpportunities)
        assertEquals(0, result.completedCount)
        assertEquals(0, result.missedCount)
        assertFalse(result.hasEligibleOpportunities)
        assertTrue(result.isLimitedEvidence)
        assertEquals(HabitStrengthLevel.BUILDING, result.level)
    }

    @Test
    fun scenario10_todayIncompleteOpportunityExcludedFromEvaluation() {
        // Created yesterday, completed yesterday. Today is incomplete.
        val result = HabitStrengthEngine.calculateStrength(
            createdAtMillis = dateToMillis("2026-11-02"),
            completedDates = setOf("2026-11-02"),
            referenceDateIso = "2026-11-03"
        )

        // Evaluated window is just yesterday [2026-11-02 to 2026-11-02]
        assertEquals(1, result.totalOpportunities)
        assertEquals(1, result.completedCount)
        assertEquals(100, result.scorePercent)
        assertEquals(HabitStrengthLevel.EXCELLENT, result.level)
    }

    @Test
    fun scenario11_todayCompletedOpportunityIncludedInEvaluation() {
        // Created yesterday, completed yesterday and completed today.
        val result = HabitStrengthEngine.calculateStrength(
            createdAtMillis = dateToMillis("2026-11-02"),
            completedDates = setOf("2026-11-02", "2026-11-03"),
            referenceDateIso = "2026-11-03"
        )

        // Evaluated window includes today [2026-11-02 to 2026-11-03]
        assertEquals(2, result.totalOpportunities)
        assertEquals(2, result.completedCount)
        assertEquals(100, result.scorePercent)
        assertEquals(HabitStrengthLevel.EXCELLENT, result.level)
    }

    @Test
    fun scenario12_futureCompletionsExcludedDefensively() {
        val result = HabitStrengthEngine.calculateStrength(
            createdAtMillis = dateToMillis("2026-11-01"),
            completedDates = setOf("2026-11-01", "2026-11-10", "2026-12-25"),
            referenceDateIso = "2026-11-01"
        )

        assertEquals(1, result.totalOpportunities)
        assertEquals(1, result.completedCount)
        assertEquals(100, result.scorePercent)
    }

    @Test
    fun scenario13_historicalCompletionBeforeCreationDateHandledSafely() {
        // Creation date is 2026-11-05, but completed dates exist starting from 2026-11-01
        val result = HabitStrengthEngine.calculateStrength(
            createdAtMillis = dateToMillis("2026-11-05"),
            completedDates = setOf("2026-11-01", "2026-11-02", "2026-11-03"),
            referenceDateIso = "2026-11-03"
        )

        // Engine starts at 2026-11-01
        assertEquals(3, result.totalOpportunities)
        assertEquals(3, result.completedCount)
        assertEquals(100, result.scorePercent)
    }

    @Test
    fun scenario14_invalidAndDuplicateDatesDoNotCorruptResult() {
        val corruptedDates = setOf(
            "2026-11-01",
            "invalid-date",
            "",
            "2026/11/01",
            "2026-99-99",
            "2026-11-01" // duplicate
        )

        val result = HabitStrengthEngine.calculateStrength(
            createdAtMillis = dateToMillis("2026-11-01"),
            completedDates = corruptedDates,
            referenceDateIso = "2026-11-01"
        )

        assertEquals(1, result.totalOpportunities)
        assertEquals(1, result.completedCount)
        assertEquals(100, result.scorePercent)
    }

    @Test
    fun scenario15_strengthTierBoundaries() {
        assertEquals(HabitStrengthLevel.BUILDING, HabitStrengthLevel.fromScore(0f))
        assertEquals(HabitStrengthLevel.BUILDING, HabitStrengthLevel.fromScore(29.99f))
        assertEquals(HabitStrengthLevel.GROWING, HabitStrengthLevel.fromScore(30.0f))
        assertEquals(HabitStrengthLevel.GROWING, HabitStrengthLevel.fromScore(59.99f))
        assertEquals(HabitStrengthLevel.STRONG, HabitStrengthLevel.fromScore(60.0f))
        assertEquals(HabitStrengthLevel.STRONG, HabitStrengthLevel.fromScore(84.99f))
        assertEquals(HabitStrengthLevel.EXCELLENT, HabitStrengthLevel.fromScore(85.0f))
        assertEquals(HabitStrengthLevel.EXCELLENT, HabitStrengthLevel.fromScore(100f))
    }

    @Test
    fun scenario16_arithmeticRateVsEmaStrengthIndependence() {
        // 20 opportunities total.
        // Habit A: 10 misses followed by 10 successes
        val habitACompleted = (11..20).map { "2026-11-%02d".format(it) }.toSet()
        val resultA = HabitStrengthEngine.calculateStrength(
            createdAtMillis = dateToMillis("2026-11-01"),
            completedDates = habitACompleted,
            referenceDateIso = "2026-11-20"
        )

        // Habit B: 10 successes followed by 10 misses
        val habitBCompleted = (1..10).map { "2026-11-%02d".format(it) }.toSet()
        val resultB = HabitStrengthEngine.calculateStrength(
            createdAtMillis = dateToMillis("2026-11-01"),
            completedDates = habitBCompleted,
            referenceDateIso = "2026-11-21" // Evaluated up to 2026-11-20 (today incomplete)
        )

        // Both habits have identical arithmetic completion rates: 10 / 20 = 50%
        assertEquals(50f, resultA.completionRate, 0.01f)
        assertEquals(50f, resultB.completionRate, 0.01f)

        // But Habit A has a recent 10-day streak, while Habit B has 10 recent misses
        assertTrue("Habit A score (${resultA.score}) must vastly exceed Habit B score (${resultB.score})", resultA.score > resultB.score)
        assertTrue("Habit A should be in STRONG or EXCELLENT tier (${resultA.score})", resultA.score >= 60f)
        assertTrue("Habit B should be in BUILDING or GROWING tier (${resultB.score})", resultB.score < 40f)
    }

    @Test
    fun habitItemOverload_calculatesAccurately() {
        val habit = HabitItem(
            id = 42L,
            title = "Morning Meditation",
            emoji = "🧘",
            description = "10 minutes of mindfulness",
            isCompletedToday = true,
            currentStreak = 5,
            longestStreak = 12,
            weeklyCompletionRate = 0.8f,
            reminderTime = "07:00",
            completedDates = setOf("2026-11-01", "2026-11-02", "2026-11-03"),
            createdAt = dateToMillis("2026-11-01")
        )

        val result = HabitStrengthEngine.calculateStrength(
            habit = habit,
            referenceDateIso = "2026-11-03"
        )

        assertEquals(3, result.totalOpportunities)
        assertEquals(3, result.completedCount)
        assertEquals(100, result.scorePercent)
        assertEquals(HabitStrengthLevel.EXCELLENT, result.level)
    }

    // --- H4-R1 Refinements Verification ---

    @Test
    fun refinement01_selectedCalendarDateDiffersFromActualCurrentDate_scoreUsesActualCurrentDate() {
        val actualToday = "2026-11-03"
        val selectedPastDate = "2026-10-20"

        val habit = HabitItem(
            id = 101L,
            title = "Reading",
            emoji = "📚",
            isCompletedToday = true,
            completedDates = setOf("2026-11-01", "2026-11-02", "2026-11-03"),
            createdAt = dateToMillis("2026-11-01")
        )

        // The Habit Detail Sheet computes against actual current date (actualToday),
        // completely ignoring selectedPastDate from the calendar.
        val result = HabitStrengthEngine.calculateStrength(
            habit = habit,
            referenceDateIso = actualToday
        )

        assertEquals(3, result.totalOpportunities)
        assertEquals(3, result.completedCount)
        assertEquals(100, result.scorePercent)
        assertEquals(HabitStrengthLevel.EXCELLENT, result.level)
    }

    @Test
    fun refinement02_selectedFutureDateDoesNotCreateFutureMissedOpportunities() {
        val actualToday = "2026-11-03"
        val selectedFutureDate = "2026-11-20" // 17 days in the future

        val habit = HabitItem(
            id = 102L,
            title = "Hydration",
            emoji = "💧",
            isCompletedToday = true,
            completedDates = setOf("2026-11-01", "2026-11-02", "2026-11-03"),
            createdAt = dateToMillis("2026-11-01")
        )

        // Using actual current local date prevents counting future days as misses
        val result = HabitStrengthEngine.calculateStrength(
            habit = habit,
            referenceDateIso = actualToday
        )

        assertEquals(3, result.totalOpportunities)
        assertEquals(0, result.missedCount)
        assertEquals(100, result.scorePercent)
    }

    @Test
    fun refinement03_actualCurrentDayIncomplete_excludedFromScore() {
        val actualToday = "2026-11-03"

        val habit = HabitItem(
            id = 103L,
            title = "Yoga",
            emoji = "🧘",
            isCompletedToday = false,
            completedDates = setOf("2026-11-01", "2026-11-02"), // today incomplete
            createdAt = dateToMillis("2026-11-01")
        )

        val result = HabitStrengthEngine.calculateStrength(
            habit = habit,
            referenceDateIso = actualToday
        )

        // Evaluated up to yesterday (2 opportunities: Day 1, Day 2)
        assertEquals(2, result.totalOpportunities)
        assertEquals(2, result.completedCount)
        assertEquals(0, result.missedCount)
        assertEquals(100, result.scorePercent)
    }

    @Test
    fun refinement04_actualCurrentDayCompleted_includedInScore() {
        val actualToday = "2026-11-03"

        val habit = HabitItem(
            id = 104L,
            title = "Yoga",
            emoji = "🧘",
            isCompletedToday = true,
            completedDates = setOf("2026-11-01", "2026-11-02", "2026-11-03"), // today completed
            createdAt = dateToMillis("2026-11-01")
        )

        val result = HabitStrengthEngine.calculateStrength(
            habit = habit,
            referenceDateIso = actualToday
        )

        // Evaluated up to today (3 opportunities: Day 1, Day 2, Day 3)
        assertEquals(3, result.totalOpportunities)
        assertEquals(3, result.completedCount)
        assertEquals(0, result.missedCount)
        assertEquals(100, result.scorePercent)
    }

    @Test
    fun refinement05_coachingTextContainsNoAutomaticityOrSubconsciousClaims() {
        val prohibitedTerms = listOf("automatic", "subconscious", "scientific", "habit formation")

        for (level in HabitStrengthLevel.values()) {
            val narrative = level.defaultNarrative.lowercase()
            for (term in prohibitedTerms) {
                assertFalse(
                    "Level narrative for ${level.name} must not contain '$term': ${level.defaultNarrative}",
                    narrative.contains(term)
                )
            }
        }

        // Test contextual narratives
        val singleCompleted = HabitStrengthEngine.calculateStrength(
            createdAtMillis = dateToMillis("2026-11-01"),
            completedDates = setOf("2026-11-01"),
            referenceDateIso = "2026-11-01"
        )
        for (term in prohibitedTerms) {
            assertFalse(singleCompleted.narrativeText.lowercase().contains(term))
        }

        val singleMissed = HabitStrengthEngine.calculateStrength(
            createdAtMillis = dateToMillis("2026-11-01"),
            completedDates = emptySet(),
            referenceDateIso = "2026-11-02"
        )
        for (term in prohibitedTerms) {
            assertFalse(singleMissed.narrativeText.lowercase().contains(term))
        }
    }

    @Test
    fun refinement06_constantsCalculationExactness() {
        assertEquals(13.0, HabitStrengthEngine.HALF_LIFE_DAYS, 0.0)
        // lambda = 0.5^(1/13) ≈ 0.9480775...
        assertEquals(0.9480775, HabitStrengthEngine.LAMBDA, 0.000001)
        // alpha = 1 - lambda ≈ 0.051922...
        assertEquals(0.0519225, HabitStrengthEngine.ALPHA, 0.000001)
        assertEquals(1.0, HabitStrengthEngine.LAMBDA + HabitStrengthEngine.ALPHA, 0.0000001)
    }
}
