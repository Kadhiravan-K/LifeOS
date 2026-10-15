package com.example.domain.model

enum class ShortcutType {
    TASK,
    HABIT,
    CHALLENGE_RULE,
    WATER,
    CUSTOM;

    companion object {
        fun fromString(value: String): ShortcutType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: TASK
        }
    }
}

data class HoneycombShortcut(
    val id: Long = 0,
    val type: ShortcutType,
    val targetId: String,
    val title: String,
    val emoji: String,
    val customColorHex: Long? = null,
    val orderIndex: Int = 0,
    val isPinned: Boolean = false,
    val isCompletedToday: Boolean = false,
    val subtitle: String = "",
    val progressFraction: Float = 0f,
    val isBinary: Boolean = true
)

data class HoneycombAppearanceConfig(
    val defaultColorHex: Long = 0xFF10B981, // Emerald Green
    val borderColorType: String = "DEFAULT", // "DEFAULT", "PRIMARY", "MUTED", "ACCENT"
    val borderWidthDp: Int = 4,
    val hexagonSizeDp: Int = 74,
    val spacingDp: Int = 6,
    val iconSizeSp: Int = 26,
    val isAnimationEnabled: Boolean = true
)

object HoneycombColorPresets {
    const val GREEN = 0xFF10B981
    const val BLUE = 0xFF3B82F6
    const val PURPLE = 0xFF8B5CF6
    const val ORANGE = 0xFFF97316
    const val PINK = 0xFFEC4899
    const val TEAL = 0xFF14B8A6
    const val AMBER = 0xFFF59E0B
    const val INDIGO = 0xFF6366F1

    val list = listOf(
        Pair("Green", GREEN),
        Pair("Blue", BLUE),
        Pair("Purple", PURPLE),
        Pair("Orange", ORANGE),
        Pair("Pink", PINK),
        Pair("Teal", TEAL)
    )
}

object DefaultHoneycombShortcuts {
    // 10 default suggested hexagons:
    // 💧 Water, 📖 Reading, 🧘 Meditation, 🏃 Exercise, ☀️ Yoga, 🛏️ Make Bed, 💼 Career Learning, 🏠 Cleaning, 🌙 Sleep, 📅 Daily Planning
    val list = listOf(
        HoneycombShortcut(
            id = 1,
            type = ShortcutType.WATER,
            targetId = "water",
            title = "Hydration",
            emoji = "💧",
            orderIndex = 0
        ),
        HoneycombShortcut(
            id = 2,
            type = ShortcutType.CHALLENGE_RULE,
            targetId = "rule_reading",
            title = "Reading",
            emoji = "📖",
            orderIndex = 1
        ),
        HoneycombShortcut(
            id = 3,
            type = ShortcutType.TASK,
            targetId = "Meditation",
            title = "Meditation",
            emoji = "🧘",
            orderIndex = 2
        ),
        HoneycombShortcut(
            id = 4,
            type = ShortcutType.CHALLENGE_RULE,
            targetId = "rule_workout_1",
            title = "Exercise",
            emoji = "🏃",
            orderIndex = 3
        ),
        HoneycombShortcut(
            id = 5,
            type = ShortcutType.TASK,
            targetId = "Yoga",
            title = "Yoga",
            emoji = "☀️",
            orderIndex = 4
        ),
        HoneycombShortcut(
            id = 6,
            type = ShortcutType.TASK,
            targetId = "Make Bed",
            title = "Make Bed",
            emoji = "🛏️",
            orderIndex = 5
        ),
        HoneycombShortcut(
            id = 7,
            type = ShortcutType.TASK,
            targetId = "Career Learning",
            title = "Career Learning",
            emoji = "💼",
            orderIndex = 6
        ),
        HoneycombShortcut(
            id = 8,
            type = ShortcutType.TASK,
            targetId = "Clean room",
            title = "Cleaning",
            emoji = "🏠",
            orderIndex = 7
        ),
        HoneycombShortcut(
            id = 9,
            type = ShortcutType.TASK,
            targetId = "Sleep Protection",
            title = "Sleep Routine",
            emoji = "🌙",
            orderIndex = 8
        ),
        HoneycombShortcut(
            id = 10,
            type = ShortcutType.TASK,
            targetId = "Daily Planning",
            title = "Daily Planning",
            emoji = "📅",
            orderIndex = 9
        )
    )
}
