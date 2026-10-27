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

enum class QrTileShape(val displayName: String, val icon: String) {
    HEXAGON("Hexagon", "⬡"),
    CIRCLE("Circle", "○"),
    SQUARE("Square", "□");

    companion object {
        fun fromString(value: String?): QrTileShape {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: HEXAGON
        }
    }
}

enum class QrColorSource(val displayName: String) {
    PROJECT("Inherit Project Color"),
    CUSTOM("Custom Color"),
    THEME_DEFAULT("Theme Default");

    companion object {
        fun fromString(value: String?): QrColorSource {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: PROJECT
        }
    }
}

data class HoneycombShortcut(
    val id: Long = 0,
    val type: ShortcutType,
    val targetId: String,
    val title: String,
    val customTileName: String? = null,
    val emoji: String,
    val customColorHex: Long? = null,
    val colorSource: String = "PROJECT", // "PROJECT", "CUSTOM", "THEME_DEFAULT"
    val shape: String = "HEXAGON", // "HEXAGON", "CIRCLE", "SQUARE"
    val orderIndex: Int = 0,
    val coordQ: Int = 0,
    val coordR: Int = 0,
    val isPinned: Boolean = false,
    val isCompletedToday: Boolean = false,
    val subtitle: String = "",
    val progressFraction: Float = 0f,
    val isBinary: Boolean = true,
    val linkedProjectId: Long? = null,
    val projectColorHex: Long? = null
) {
    val displayTitle: String get() = if (!customTileName.isNullOrBlank()) customTileName else title

    val effectiveColorHex: Long?
        get() = when (colorSource) {
            "CUSTOM" -> customColorHex
            "PROJECT" -> projectColorHex ?: customColorHex ?: when (type) {
                ShortcutType.WATER -> 0xFF3B82F6L
                ShortcutType.HABIT -> 0xFF10B981L
                ShortcutType.CHALLENGE_RULE -> 0xFFF59E0BL
                else -> null
            }
            "THEME_DEFAULT" -> null
            else -> customColorHex ?: projectColorHex ?: when (type) {
                ShortcutType.WATER -> 0xFF3B82F6L
                ShortcutType.HABIT -> 0xFF10B981L
                ShortcutType.CHALLENGE_RULE -> 0xFFF59E0BL
                else -> null
            }
        }

    val tileShape: QrTileShape get() = QrTileShape.fromString(shape)
}

enum class HoneycombLayoutTemplate(val displayName: String, val icon: String) {
    STANDARD("Standard Honeycomb", "⬡"),
    PYRAMID("Pyramid", "▲"),
    DIAMOND("Diamond", "◆"),
    HEART("Heart", "♥"),
    SNAKE("Snake", "〰"),
    ZIGZAG("Zigzag", "⚡"),
    FREEFORM("Freeform", "✎");

    companion object {
        fun fromString(name: String): HoneycombLayoutTemplate {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: STANDARD
        }

        fun generateCoordinates(template: HoneycombLayoutTemplate, count: Int): List<Pair<Int, Int>> {
            val coords = mutableListOf<Pair<Int, Int>>()
            when (template) {
                STANDARD -> {
                    // Staggered honeycomb: 3 items in even rows, 2 items in odd rows
                    // Compensates for axial shift (X = q + r/2) with q = col - (r / 2)
                    var r = 0
                    var col = 0
                    for (i in 0 until count) {
                        val rowLen = if (r % 2 == 0) 3 else 2
                        val q = col - (r / 2)
                        coords.add(Pair(q, r))
                        col++
                        if (col >= rowLen) {
                            col = 0
                            r++
                        }
                    }
                }
                PYRAMID -> {
                    // Centered pyramid: row r has (r + 1) items centered symmetrically at physical X = 0
                    // q = col - r yields physical X = col - r/2 across [-r/2, +r/2]
                    var r = 0
                    var col = 0
                    for (i in 0 until count) {
                        val rowLen = r + 1
                        val q = col - r
                        coords.add(Pair(q, r))
                        col++
                        if (col >= rowLen) {
                            col = 0
                            r++
                        }
                    }
                }
                DIAMOND -> {
                    // Symmetrical diamond centered at physical X = 0
                    val diamondRowLens = listOf(1, 2, 3, 4, 3, 2, 1)
                    var r = 0
                    var col = 0
                    for (i in 0 until count) {
                        val rowLen = diamondRowLens[r % diamondRowLens.size]
                        val q = col - ((r + rowLen) / 2)
                        coords.add(Pair(q, r))
                        col++
                        if (col >= rowLen) {
                            col = 0
                            r++
                        }
                    }
                }
                HEART -> {
                    // Symmetrical heart coordinates on pointy-top hex grid:
                    // Row 0: Dual peaks at X = -1 and +1 -> q = -1, +1
                    // Row 1: Full width at X in [-1.5, 1.5] -> q = col - 2
                    // Row 2: Inward taper at X in [-1, 1] -> q = col - 2
                    // Row 3: Lower taper at X in [-0.5, 0.5] -> q = col - 2
                    // Row 4: Heart apex at X = 0 -> q = -2
                    val heartLayout = listOf(
                        Pair(-1, 0), Pair(1, 0),
                        Pair(-2, 1), Pair(-1, 1), Pair(0, 1), Pair(1, 1),
                        Pair(-2, 2), Pair(-1, 2), Pair(0, 2),
                        Pair(-2, 3), Pair(-1, 3),
                        Pair(-2, 4)
                    )
                    for (i in 0 until count) {
                        if (i < heartLayout.size) {
                            coords.add(heartLayout[i])
                        } else {
                            val extraR = 5 + ((i - heartLayout.size) / 2)
                            val extraCol = (i - heartLayout.size) % 2
                            coords.add(Pair(extraCol - (extraR / 2) - 1, extraR))
                        }
                    }
                }
                SNAKE -> {
                    // Meanders in a 3-tile wide column without rightward drift:
                    var r = 0
                    var col = 0
                    var goingRight = true
                    for (i in 0 until count) {
                        val q = col - (r / 2)
                        coords.add(Pair(q, r))
                        if (goingRight) {
                            col++
                            if (col >= 3) {
                                col = 1
                                r++
                                goingRight = false
                            }
                        } else {
                            col--
                            if (col < 0) {
                                col = 0
                                r++
                                goingRight = true
                            }
                        }
                    }
                }
                ZIGZAG -> {
                    // Alternates left and right vertically with zero lateral drift
                    for (i in 0 until count) {
                        val r = i
                        val q = when (r % 4) {
                            1 -> -(r / 2)
                            3 -> -(r / 2) - 1
                            else -> -(r / 2)
                        }
                        coords.add(Pair(q, r))
                    }
                }
                FREEFORM -> {
                    var r = 0
                    var col = 0
                    for (i in 0 until count) {
                        coords.add(Pair(col - (r / 2), r))
                        col++
                        if (col >= 3) {
                            col = 0
                            r++
                        }
                    }
                }
            }
            return coords
        }
    }
}

enum class DashboardShape(val displayName: String, val icon: String) {
    HEXAGON("Hexagon", "⬡"),
    SQUARE("Square", "■"),
    CIRCLE("Circle", "●");

    companion object {
        fun fromString(value: String): DashboardShape {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: HEXAGON
        }
    }
}

data class HoneycombAppearanceConfig(
    val defaultColorHex: Long = 0xFF10B981, // Emerald Green
    val borderColorType: String = "DEFAULT", // "DEFAULT", "PRIMARY", "MUTED", "ACCENT"
    val borderWidthDp: Int = 4,
    val hexagonSizeDp: Int = 74,
    val spacingDp: Int = 6,
    val iconSizeSp: Int = 26,
    val isAnimationEnabled: Boolean = true,
    val shape: DashboardShape = DashboardShape.HEXAGON
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
            orderIndex = 0,
            coordQ = 0,
            coordR = 0
        ),
        HoneycombShortcut(
            id = 2,
            type = ShortcutType.CHALLENGE_RULE,
            targetId = "rule_reading",
            title = "Reading",
            emoji = "📖",
            orderIndex = 1,
            coordQ = 1,
            coordR = 0
        ),
        HoneycombShortcut(
            id = 3,
            type = ShortcutType.TASK,
            targetId = "Meditation",
            title = "Meditation",
            emoji = "🧘",
            orderIndex = 2,
            coordQ = 2,
            coordR = 0
        ),
        HoneycombShortcut(
            id = 4,
            type = ShortcutType.CHALLENGE_RULE,
            targetId = "rule_workout_1",
            title = "Exercise",
            emoji = "🏃",
            orderIndex = 3,
            coordQ = 0,
            coordR = 1
        ),
        HoneycombShortcut(
            id = 5,
            type = ShortcutType.TASK,
            targetId = "Yoga",
            title = "Yoga",
            emoji = "☀️",
            orderIndex = 4,
            coordQ = 1,
            coordR = 1
        ),
        HoneycombShortcut(
            id = 6,
            type = ShortcutType.TASK,
            targetId = "Make Bed",
            title = "Make Bed",
            emoji = "🛏️",
            orderIndex = 5,
            coordQ = 0,
            coordR = 2
        ),
        HoneycombShortcut(
            id = 7,
            type = ShortcutType.TASK,
            targetId = "Career Learning",
            title = "Career Learning",
            emoji = "💼",
            orderIndex = 6,
            coordQ = 1,
            coordR = 2
        ),
        HoneycombShortcut(
            id = 8,
            type = ShortcutType.TASK,
            targetId = "Clean room",
            title = "Cleaning",
            emoji = "🏠",
            orderIndex = 7,
            coordQ = 2,
            coordR = 2
        ),
        HoneycombShortcut(
            id = 9,
            type = ShortcutType.TASK,
            targetId = "Sleep Protection",
            title = "Sleep Routine",
            emoji = "🌙",
            orderIndex = 8,
            coordQ = 0,
            coordR = 3
        ),
        HoneycombShortcut(
            id = 10,
            type = ShortcutType.TASK,
            targetId = "Daily Planning",
            title = "Daily Planning",
            emoji = "📅",
            orderIndex = 9,
            coordQ = 1,
            coordR = 3
        )
    )
}
