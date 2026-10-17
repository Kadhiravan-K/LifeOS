package com.example.domain.model

enum class ChallengeStatus {
    ACTIVE,
    COMPLETED,
    RESTARTED
}

data class ChallengeRule(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String
)

data class ChallengeDailyLog(
    val dateIso: String,
    val completedRuleIds: Set<String> = emptySet(),
    val notes: String = ""
)

data class ChallengeState(
    val id: Long = 1,
    val startDateIso: String,
    val targetDays: Int = 75,
    val status: ChallengeStatus = ChallengeStatus.ACTIVE,
    val rules: List<ChallengeRule>,
    val logs: Map<String, ChallengeDailyLog> = emptyMap(),
    val currentDayNumber: Int = 1,
    val completedDaysCount: Int = 0,
    val missedDaysCount: Int = 0,
    val progressPercentage: Float = 0f,
    val currentStreak: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

object DefaultChallengeRules {
    val list = listOf(
        ChallengeRule(
            id = "rule_workout_1",
            title = "45-Minute Workout #1",
            emoji = "🏋️",
            description = "Indoor or outdoor disciplined physical training"
        ),
        ChallengeRule(
            id = "rule_workout_2",
            title = "45-Minute Workout #2 (Outdoor)",
            emoji = "🏃",
            description = "Outdoor training regardless of the weather"
        ),
        ChallengeRule(
            id = "rule_diet",
            title = "Strict Clean Diet",
            emoji = "🥗",
            description = "Zero cheat meals, whole clean foods, zero alcohol"
        ),
        ChallengeRule(
            id = "rule_water",
            title = "Drink 3.7L / 1 Gallon Water",
            emoji = "💧",
            description = "Optimal hydration throughout the waking day"
        ),
        ChallengeRule(
            id = "rule_reading",
            title = "Read 10 Pages (Non-Fiction)",
            emoji = "📖",
            description = "Expand your mental edge and personal mastery"
        ),
        ChallengeRule(
            id = "rule_photo",
            title = "Daily Progress Photo / Log",
            emoji = "📸",
            description = "Honest visual proof and daily self-accountability"
        )
    )

    val milestones = mapOf(
        7 to "🌟 Day 7 Milestone: Foundation Settled!",
        15 to "⚡ Day 15 Milestone: Momentum Unleashed!",
        25 to "🛡️ Day 25 Milestone: Unbreakable Focus!",
        50 to "🔥 Day 50 Milestone: Elite Transformation!",
        75 to "👑 Day 75: Discipline Master Completed!"
    )
}

data class ChallengePreset(
    val id: String,
    val name: String,
    val description: String,
    val waterTargetMl: Int,
    val workoutCount: Int,
    val rules: List<ChallengeRule>
)

object ChallengePresetCatalog {
    val OFFICIAL_75_HARD = ChallengePreset(
        id = "OFFICIAL_75_HARD",
        name = "Official 75 Hard",
        description = "2x 45-min workouts (one outdoor), 3.7L water, strict diet, 10 pages non-fiction reading, daily photo. Zero compromises.",
        waterTargetMl = 3700,
        workoutCount = 2,
        rules = DefaultChallengeRules.list
    )

    val SEVENTY_FIVE_SOFT = ChallengePreset(
        id = "SEVENTY_FIVE_SOFT",
        name = "75 Soft",
        description = "1x 45-min daily workout, 2.5L water, 10 pages reading of any genre, clean balanced eating with social moderation, weekly progress check.",
        waterTargetMl = 2500,
        workoutCount = 1,
        rules = listOf(
            ChallengeRule(
                id = "rule_workout_1",
                title = "45-Minute Daily Workout",
                emoji = "🏃",
                description = "Any physical activity including walking, yoga, gym, or running"
            ),
            ChallengeRule(
                id = "rule_diet",
                title = "Clean Balanced Diet",
                emoji = "🥗",
                description = "Eat mindful healthy food, alcohol only on social occasions"
            ),
            ChallengeRule(
                id = "rule_water",
                title = "Drink 2.5L Water Daily",
                emoji = "💧",
                description = "Stay consistently hydrated"
            ),
            ChallengeRule(
                id = "rule_reading",
                title = "Read 10 Pages (Any Book)",
                emoji = "📖",
                description = "Daily reading habit for focus and relaxation"
            ),
            ChallengeRule(
                id = "rule_photo",
                title = "Weekly Progress Photo / Reflection",
                emoji = "📸",
                description = "Weekly visual and mental check-in"
            )
        )
    )

    val SEVENTY_FIVE_MEDIUM = ChallengePreset(
        id = "SEVENTY_FIVE_MEDIUM",
        name = "75 Medium",
        description = "2x 30-min workouts (indoor or outdoor), 3.0L water, 90% clean diet with 1 cheat meal per week, 10 pages non-fiction, daily photo.",
        waterTargetMl = 3000,
        workoutCount = 2,
        rules = listOf(
            ChallengeRule(
                id = "rule_workout_1",
                title = "30-Minute Workout #1",
                emoji = "🏋️",
                description = "Morning or afternoon fitness session"
            ),
            ChallengeRule(
                id = "rule_workout_2",
                title = "30-Minute Workout #2",
                emoji = "🏃",
                description = "Evening or outdoor active movement"
            ),
            ChallengeRule(
                id = "rule_diet",
                title = "90% Clean Diet (1 Cheat Meal / Wk)",
                emoji = "🥗",
                description = "High protein whole foods, strict alcohol moderation"
            ),
            ChallengeRule(
                id = "rule_water",
                title = "Drink 3.0 Liters Water",
                emoji = "💧",
                description = "Optimal daily water intake"
            ),
            ChallengeRule(
                id = "rule_reading",
                title = "Read 10 Pages (Non-Fiction)",
                emoji = "📖",
                description = "Personal growth or professional skill mastery"
            ),
            ChallengeRule(
                id = "rule_photo",
                title = "Daily Progress Photo",
                emoji = "📸",
                description = "Daily accountability capture"
            )
        )
    )

    val list = listOf(OFFICIAL_75_HARD, SEVENTY_FIVE_SOFT, SEVENTY_FIVE_MEDIUM)
}
