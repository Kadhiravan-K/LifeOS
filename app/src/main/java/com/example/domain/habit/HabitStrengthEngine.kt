package com.example.domain.habit

import com.example.domain.model.HabitItem
import com.example.utils.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Approved strength levels and score thresholds for Habit Strength.
 */
enum class HabitStrengthLevel(
    val label: String,
    val minScore: Float,
    val maxScoreExclusive: Float,
    val defaultNarrative: String
) {
    BUILDING(
        label = "Building",
        minScore = 0f,
        maxScoreExclusive = 30f,
        defaultNarrative = "Early stage. Consistency is vulnerable to interruptions. Focus on small daily completions to build steady momentum."
    ),
    GROWING(
        label = "Growing",
        minScore = 30f,
        maxScoreExclusive = 60f,
        defaultNarrative = "Momentum is building. Steady repetition is establishing consistent daily practice."
    ),
    STRONG(
        label = "Strong",
        minScore = 60f,
        maxScoreExclusive = 85f,
        defaultNarrative = "Strong consistency. Withstands occasional missed opportunities without collapsing accumulated momentum."
    ),
    EXCELLENT(
        label = "Excellent",
        minScore = 85f,
        maxScoreExclusive = 100.001f,
        defaultNarrative = "Peak consistency. Outstanding long-term commitment built through sustained daily repetition."
    );

    companion object {
        fun fromScore(score: Float): HabitStrengthLevel {
            return when {
                score >= 85f -> EXCELLENT
                score >= 60f -> STRONG
                score >= 30f -> GROWING
                else -> BUILDING
            }
        }
    }
}

/**
 * Structured calculation result from HabitStrengthEngine.
 */
data class HabitStrengthResult(
    val score: Float,
    val scorePercent: Int,
    val level: HabitStrengthLevel,
    val completedCount: Int,
    val missedCount: Int,
    val totalOpportunities: Int,
    val completionRate: Float,
    val isLimitedEvidence: Boolean,
    val hasEligibleOpportunities: Boolean,
    val narrativeText: String
)

/**
 * Pure, deterministic calculation engine for LifeOS Habit Strength.
 *
 * Implements a 13-day half-life Exponential Moving Average (EMA) with
 * bias-corrected initialization.
 */
object HabitStrengthEngine {

    const val HALF_LIFE_DAYS = 13.0
    val LAMBDA: Double = 0.5.pow(1.0 / HALF_LIFE_DAYS)
    val ALPHA: Double = 1.0 - LAMBDA

    private val ISO_DATE_REGEX = Regex("""^\d{4}-\d{2}-\d{2}$""")

    /**
     * Calculates the Habit Strength Score for a given [HabitItem].
     */
    fun calculateStrength(
        habit: HabitItem,
        referenceDateIso: String = DateUtils.todayString()
    ): HabitStrengthResult {
        return calculateStrength(
            createdAtMillis = habit.createdAt,
            completedDates = habit.completedDates,
            referenceDateIso = referenceDateIso
        )
    }

    /**
     * Pure calculation function accepting primitive inputs for maximum testability and decoupling.
     */
    fun calculateStrength(
        createdAtMillis: Long,
        completedDates: Set<String>,
        referenceDateIso: String = DateUtils.todayString()
    ): HabitStrengthResult {
        // 1. Sanitize and validate completion dates
        val validCompletions = completedDates
            .filter { isValidIsoDate(it) && it <= referenceDateIso }
            .toSet()

        val isTodayCompleted = validCompletions.contains(referenceDateIso)

        // 2. Determine evaluation start date
        // Creation calendar date in local time
        val creationDateIso = DateUtils.todayString(nowMillis = createdAtMillis)
        val earliestCompletionIso = validCompletions.minOrNull()

        val startDateIso = if (earliestCompletionIso != null && earliestCompletionIso < creationDateIso) {
            earliestCompletionIso
        } else {
            creationDateIso
        }

        // 3. Determine evaluation end date
        // If today is completed, include today (positive opportunity).
        // If today is not completed, end at yesterday so the user is not penalized prematurely.
        val endDateIso = if (isTodayCompleted) {
            referenceDateIso
        } else {
            DateUtils.addDays(referenceDateIso, -1)
        }

        // 4. Handle zero-opportunity boundary
        if (startDateIso > endDateIso) {
            return HabitStrengthResult(
                score = 0f,
                scorePercent = 0,
                level = HabitStrengthLevel.BUILDING,
                completedCount = 0,
                missedCount = 0,
                totalOpportunities = 0,
                completionRate = 0f,
                isLimitedEvidence = true,
                hasEligibleOpportunities = false,
                narrativeText = "No opportunities evaluated yet. Check in today to start building habit strength!"
            )
        }

        // 5. Iterate daily sequence from startDateIso to endDateIso
        var ema = 0.0
        var completedCount = 0
        var missedCount = 0
        var opportunityCount = 0

        var currentDateIso = startDateIso
        while (currentDateIso <= endDateIso) {
            opportunityCount++
            val isCompleted = validCompletions.contains(currentDateIso)
            val completionValue = if (isCompleted) {
                completedCount++
                1.0
            } else {
                missedCount++
                0.0
            }

            ema = (LAMBDA * ema) + (ALPHA * completionValue)
            currentDateIso = DateUtils.addDays(currentDateIso, 1)
        }

        if (opportunityCount == 0) {
            return HabitStrengthResult(
                score = 0f,
                scorePercent = 0,
                level = HabitStrengthLevel.BUILDING,
                completedCount = 0,
                missedCount = 0,
                totalOpportunities = 0,
                completionRate = 0f,
                isLimitedEvidence = true,
                hasEligibleOpportunities = false,
                narrativeText = "No opportunities evaluated yet. Check in today to start building habit strength!"
            )
        }

        // 6. Apply bias correction divisor: 1 - lambda^N
        val divisor = 1.0 - LAMBDA.pow(opportunityCount.toDouble())
        val correctedScore = if (divisor > 0.0) {
            (ema / divisor).coerceIn(0.0, 1.0)
        } else {
            0.0
        }

        val rawScoreFloat = (correctedScore * 100.0).toFloat().coerceIn(0f, 100f)
        val scorePercent = rawScoreFloat.roundToInt().coerceIn(0, 100)
        val level = HabitStrengthLevel.fromScore(rawScoreFloat)
        val completionRate = (completedCount.toFloat() / opportunityCount.toFloat()) * 100f

        // Limited evidence if fewer than one full half-life (13 opportunities)
        val isLimitedEvidence = opportunityCount < HALF_LIFE_DAYS.toInt()

        // 7. Contextual narrative selection
        val narrativeText = when {
            opportunityCount == 1 && completedCount == 1 ->
                "Off to a great start! Completed 1 of 1 opportunity. Keep logging daily to build consistent momentum."
            opportunityCount == 1 && missedCount == 1 ->
                "First opportunity was missed. Complete your habit today to begin establishing consistency."
            isLimitedEvidence && completedCount == opportunityCount ->
                "Consistent initial streak! Keep logging daily to build long-term resilience."
            else ->
                level.defaultNarrative
        }

        return HabitStrengthResult(
            score = rawScoreFloat,
            scorePercent = scorePercent,
            level = level,
            completedCount = completedCount,
            missedCount = missedCount,
            totalOpportunities = opportunityCount,
            completionRate = completionRate,
            isLimitedEvidence = isLimitedEvidence,
            hasEligibleOpportunities = true,
            narrativeText = narrativeText
        )
    }

    private fun isValidIsoDate(str: String): Boolean {
        if (!ISO_DATE_REGEX.matches(str)) return false
        return try {
            LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE)
            true
        } catch (_: Exception) {
            false
        }
    }
}
