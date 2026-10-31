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
    val calculatedAtIso: String = ""
) {
    fun scoreFor(dimension: RadarDimension): Int {
        return dimensions.find { it.dimension == dimension }?.scorePercent ?: 0
    }
}
