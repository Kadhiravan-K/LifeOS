package com.example.domain.analytics

/**
 * The 5 core productivity dimensions analyzed by the LifeOS Productivity Radar.
 */
enum class RadarDimension(
    val id: String,
    val displayName: String,
    val emoji: String,
    val description: String
) {
    TASKS("tasks", "Focus", "🎯", "Daily task completion & focus"),
    HABITS("habits", "Consistency", "🌱", "Habit adherence & routine building"),
    WELLNESS("wellness", "Wellness", "💧", "Hydration & physical vitality"),
    PLANNING("planning", "Vision", "🧭", "Milestone & strategic goal progress"),
    DISCIPLINE("discipline", "Discipline", "🔥", "Streak momentum & daily grit");

    companion object {
        fun fromId(id: String): RadarDimension {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: TASKS
        }
    }
}

/**
 * Time evaluation period for the Productivity Radar.
 */
enum class ProductivityPeriod(
    val id: String,
    val displayName: String,
    val emoji: String,
    val description: String
) {
    DAILY("daily", "Daily", "📅", "Today's dynamic flow"),
    WEEKLY("weekly", "Weekly", "🗓️", "Past 7 days performance"),
    MONTHLY("monthly", "Monthly", "📆", "30-day consistency"),
    YEARLY("yearly", "Yearly", "🌟", "365-day macro vision");

    companion object {
        fun fromId(id: String?): ProductivityPeriod {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: DAILY
        }
    }
}

/**
 * Score and diagnostic details for an individual radar dimension.
 */
data class RadarDimensionScore(
    val dimension: RadarDimension,
    val scorePercent: Int, // 0..100
    val rawValue: String,
    val targetValue: String,
    val statusSummary: String
)

/**
 * Complete snapshot of the LifeOS Productivity Radar Chart.
 */
data class ProductivityRadarData(
    val overallScorePercent: Int,
    val ratingTier: String,
    val ratingTierEmoji: String,
    val dimensions: List<RadarDimensionScore>,
    val calculatedAtIso: String = "",
    val period: ProductivityPeriod = ProductivityPeriod.DAILY,
    val hasSufficientData: Boolean = true,
    val guidanceTip: String = ""
) {
    fun scoreFor(dimension: RadarDimension): Int {
        return dimensions.find { it.dimension == dimension }?.scorePercent ?: 0
    }
}

