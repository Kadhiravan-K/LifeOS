package com.example.domain.gamification

import com.example.domain.model.HunterRank
import com.example.domain.model.TaskPriority
import com.example.utils.DateUtils
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Deterministic Gamification Engine for LifeOS.
 *
 * Implements:
 * 1. O(1) Level Calculation & Inversion via quadratic summation formula.
 * 2. Hunter Rank thresholds (E -> D -> C -> B -> A -> S).
 * 3. Deterministic XP rewards and anti-farming revocation logic.
 * 4. Consecutive day productivity & habit streak computation.
 */
object GamificationEngine {

    // --- XP Rewards Constants ---
    const val XP_TASK_NORMAL = 10
    const val COINS_TASK_NORMAL = 2

    const val XP_TASK_HIGH_PRIORITY = 20
    const val COINS_TASK_HIGH_PRIORITY = 5

    const val XP_HABIT_CHECKIN = 5
    const val COINS_HABIT_CHECKIN = 1

    const val XP_ALL_HABITS_BONUS = 15
    const val COINS_ALL_HABITS_BONUS = 5

    const val XP_CHALLENGE_OBJECTIVE = 15
    const val COINS_CHALLENGE_OBJECTIVE = 3

    const val XP_CHALLENGE_COMPLETED = 250
    const val COINS_CHALLENGE_COMPLETED = 100

    const val XP_TEMPLATE_INSTALLED = 25
    const val COINS_TEMPLATE_INSTALLED = 5

    /**
     * Determines XP and Coins for completing a task based on its priority.
     */
    fun getTaskXpReward(priority: TaskPriority): Pair<Int, Int> {
        return if (priority == TaskPriority.HIGH || priority == TaskPriority.URGENT) {
            Pair(XP_TASK_HIGH_PRIORITY, COINS_TASK_HIGH_PRIORITY)
        } else {
            Pair(XP_TASK_NORMAL, COINS_TASK_NORMAL)
        }
    }

    /**
     * Cumulative XP required to reach the start of level [level].
     * Level 1 starts at 0 XP.
     * Formula: sum_{i=1}^{L-1} (i * 100) = 50 * (L - 1) * L
     */
    fun cumulativeXpForLevel(level: Int): Long {
        if (level <= 1) return 0L
        val l = level.toLong()
        return 50L * (l - 1L) * l
    }

    /**
     * XP required to advance from [level] to [level + 1].
     * Formula: level * 100
     */
    fun xpRequiredForNextLevel(level: Int): Long {
        return max(1, level) * 100L
    }

    /**
     * Inverts total XP to compute the player's level deterministically in O(1).
     * Solves: 50 * L^2 - 50 * L - totalXp <= 0
     * L = floor( (1 + sqrt(1 + 8 * totalXp / 100)) / 2 )
     */
    fun calculateLevelFromXp(totalXp: Long): Int {
        if (totalXp <= 0L) return 1
        val discriminant = 1.0 + (8.0 * totalXp.toDouble() / 100.0)
        val level = floor((1.0 + sqrt(discriminant)) / 2.0).toInt()
        return max(1, level)
    }

    /**
     * Computes level progression metrics given [totalXp].
     */
    fun computeLevelProgress(totalXp: Long): LevelProgressData {
        val level = calculateLevelFromXp(totalXp)
        val currentLevelBaseXp = cumulativeXpForLevel(level)
        val nextLevelBaseXp = cumulativeXpForLevel(level + 1)
        val xpNeededForNext = xpRequiredForNextLevel(level)
        val xpInCurrentLevel = (totalXp - currentLevelBaseXp).coerceAtLeast(0L)
        val progressFraction = if (xpNeededForNext > 0L) {
            (xpInCurrentLevel.toFloat() / xpNeededForNext.toFloat()).coerceIn(0f, 1f)
        } else {
            1f
        }

        return LevelProgressData(
            currentLevel = level,
            totalXp = totalXp,
            currentLevelBaseXp = currentLevelBaseXp,
            nextLevelBaseXp = nextLevelBaseXp,
            xpInCurrentLevel = xpInCurrentLevel,
            xpNeededForNextLevel = xpNeededForNext,
            progressFraction = progressFraction
        )
    }

    /**
     * Hunter Rank determination based on current Level and Total XP.
     * Thresholds:
     * E: Level 1..5  (0 - 1,499 XP)
     * D: Level 6..12 (1,500 - 7,799 XP)
     * C: Level 13..20 (7,800 - 20,999 XP)
     * B: Level 21..30 (21,000 - 46,499 XP)
     * A: Level 31..45 (46,500 - 103,499 XP)
     * S: Level 46+   (103,500+ XP)
     */
    fun calculateHunterRank(level: Int): HunterRank {
        return when {
            level >= 46 -> HunterRank.S
            level >= 31 -> HunterRank.A
            level >= 21 -> HunterRank.B
            level >= 13 -> HunterRank.C
            level >= 6 -> HunterRank.D
            else -> HunterRank.E
        }
    }

    /**
     * Computes progress towards the next Hunter Rank.
     */
    fun computeRankProgress(totalXp: Long): RankProgressData {
        val level = calculateLevelFromXp(totalXp)
        val currentRank = calculateHunterRank(level)

        val nextRank = when (currentRank) {
            HunterRank.E -> HunterRank.D
            HunterRank.D -> HunterRank.C
            HunterRank.C -> HunterRank.B
            HunterRank.B -> HunterRank.A
            HunterRank.A -> HunterRank.S
            else -> null
        }

        if (nextRank == null) {
            return RankProgressData(
                currentRank = currentRank,
                nextRank = null,
                xpForNextRank = 0L,
                progressFraction = 1f
            )
        }

        val currentRankBaseXp = cumulativeXpForLevel(currentRank.minLevel)
        val nextRankBaseXp = cumulativeXpForLevel(nextRank.minLevel)
        val rankSpan = nextRankBaseXp - currentRankBaseXp
        val rankProgressXp = (totalXp - currentRankBaseXp).coerceAtLeast(0L)

        val fraction = if (rankSpan > 0L) {
            (rankProgressXp.toFloat() / rankSpan.toFloat()).coerceIn(0f, 1f)
        } else {
            1f
        }

        return RankProgressData(
            currentRank = currentRank,
            nextRank = nextRank,
            xpForNextRank = nextRankBaseXp,
            progressFraction = fraction
        )
    }

    /**
     * Deterministic Event Key generator for anti-farming protection.
     */
    fun createEventKey(eventType: String, entityId: String, dateIso: String): String {
        return "$eventType:$entityId:$dateIso"
    }

    /**
     * Calculates consecutive day streaks from a set of completed ISO dates (YYYY-MM-DD).
     * Handles:
     * - Today completed vs Today pending with Yesterday completed.
     * - Gaps / Missed days resetting current streak.
     * - Longest historical streak.
     */
    fun calculateStreaks(completedDatesIso: Set<String>, referenceDateIso: String = DateUtils.todayString()): StreakResult {
        if (completedDatesIso.isEmpty()) {
            return StreakResult(currentStreak = 0, longestStreak = 0, isCompletedToday = false)
        }

        val dates = completedDatesIso
            .mapNotNull { parseIsoDateOrNull(it) }
            .distinct()
            .sorted()

        if (dates.isEmpty()) {
            return StreakResult(currentStreak = 0, longestStreak = 0, isCompletedToday = false)
        }

        val refDate = parseIsoDateOrNull(referenceDateIso) ?: LocalDate.now()
        val yesterday = refDate.minusDays(1)

        val isTodayCompleted = dates.contains(refDate)
        val isYesterdayCompleted = dates.contains(yesterday)

        // 1. Current Streak calculation: Walk backwards from refDate or yesterday
        var currentStreak = 0
        var checkDate = if (isTodayCompleted) refDate else if (isYesterdayCompleted) yesterday else null

        while (checkDate != null && dates.contains(checkDate)) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }

        // 2. Longest Streak calculation: Scan sorted distinct dates
        var longestStreak = 0
        var run = 0
        var prevDate: LocalDate? = null

        for (d in dates) {
            if (prevDate == null) {
                run = 1
            } else {
                val diffDays = ChronoUnit.DAYS.between(prevDate, d)
                if (diffDays == 1L) {
                    run++
                } else if (diffDays > 1L) {
                    run = 1
                }
            }
            if (run > longestStreak) {
                longestStreak = run
            }
            prevDate = d
        }

        return StreakResult(
            currentStreak = currentStreak,
            longestStreak = max(longestStreak, currentStreak),
            isCompletedToday = isTodayCompleted
        )
    }

    private fun parseIsoDateOrNull(isoString: String): LocalDate? {
        return try {
            LocalDate.parse(isoString.trim())
        } catch (_: Exception) {
            null
        }
    }
}

data class LevelProgressData(
    val currentLevel: Int,
    val totalXp: Long,
    val currentLevelBaseXp: Long,
    val nextLevelBaseXp: Long,
    val xpInCurrentLevel: Long,
    val xpNeededForNextLevel: Long,
    val progressFraction: Float
)

data class RankProgressData(
    val currentRank: HunterRank,
    val nextRank: HunterRank?,
    val xpForNextRank: Long,
    val progressFraction: Float
)

data class StreakResult(
    val currentStreak: Int,
    val longestStreak: Int,
    val isCompletedToday: Boolean
) {
    val bestStreak: Int
        get() = longestStreak
}
