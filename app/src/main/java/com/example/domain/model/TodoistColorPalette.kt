package com.example.domain.model

import androidx.compose.ui.graphics.Color

enum class ColorOrderingMode {
    AUTO,
    CUSTOM;

    companion object {
        fun fromString(value: String): ColorOrderingMode {
            return try {
                valueOf(value.uppercase())
            } catch (e: Exception) {
                AUTO
            }
        }
    }
}

data class TodoistColor(
    val id: String,
    val name: String,
    val hex: String,
    val colorLong: Long,
    val defaultOrder: Int
) {
    val composeColor: Color get() = Color(colorLong)
}

object TodoistColorPalette {
    // Exact standard 20-color Todoist project & label palette with verified HEX codes
    val BERRY_RED = TodoistColor("berry_red", "Berry red", "#B8256F", 0xFFB8256F, 0)
    val RED = TodoistColor("red", "Red", "#DB4035", 0xFFDB4035, 1)
    val ORANGE = TodoistColor("orange", "Orange", "#FF9933", 0xFFFF9933, 2)
    val YELLOW = TodoistColor("yellow", "Yellow", "#FAD000", 0xFFFAD000, 3)
    val OLIVE_GREEN = TodoistColor("olive_green", "Olive green", "#AFB83B", 0xFFAFB83B, 4)
    val LIME_GREEN = TodoistColor("lime_green", "Lime green", "#7ECC49", 0xFF7ECC49, 5)
    val GREEN = TodoistColor("green", "Green", "#299438", 0xFF299438, 6)
    val MINT_GREEN = TodoistColor("mint_green", "Mint green", "#6ACCBC", 0xFF6ACCBC, 7)
    val TEAL = TodoistColor("teal", "Teal", "#158FAD", 0xFF158FAD, 8)
    val SKY_BLUE = TodoistColor("sky_blue", "Sky blue", "#14AAF5", 0xFF14AAF5, 9)
    val LIGHT_BLUE = TodoistColor("light_blue", "Light blue", "#96C3EB", 0xFF96C3EB, 10)
    val BLUE = TodoistColor("blue", "Blue", "#4073FF", 0xFF4073FF, 11)
    val GRAPE = TodoistColor("grape", "Grape", "#884DFF", 0xFF884DFF, 12)
    val VIOLET = TodoistColor("violet", "Violet", "#AF38EB", 0xFFAF38EB, 13)
    val LAVENDER = TodoistColor("lavender", "Lavender", "#EB96EB", 0xFFEB96EB, 14)
    val MAGENTA = TodoistColor("magenta", "Magenta", "#E05194", 0xFFE05194, 15)
    val SALMON = TodoistColor("salmon", "Salmon", "#FF8D85", 0xFFFF8D85, 16)
    val CHARCOAL = TodoistColor("charcoal", "Charcoal", "#808080", 0xFF808080, 17)
    val GREY = TodoistColor("grey", "Grey", "#B8B8B8", 0xFFB8B8B8, 18)
    val TAUPE = TodoistColor("taupe", "Taupe", "#CCAC93", 0xFFCCAC93, 19)

    val allColors: List<TodoistColor> = listOf(
        BERRY_RED, RED, ORANGE, YELLOW, OLIVE_GREEN,
        LIME_GREEN, GREEN, MINT_GREEN, TEAL, SKY_BLUE,
        LIGHT_BLUE, BLUE, GRAPE, VIOLET, LAVENDER,
        MAGENTA, SALMON, CHARCOAL, GREY, TAUPE
    )

    private val colorMapById = allColors.associateBy { it.id }

    fun getColorById(id: String): TodoistColor {
        return colorMapById[id] ?: BLUE
    }

    fun getColorHex(id: String): String {
        return getColorById(id).hex
    }

    fun getColorLong(id: String): Long {
        return getColorById(id).colorLong
    }

    /**
     * Sorts colors by usage frequency descending (Mode A: AUTO).
     * Tie-breaker: original default palette order.
     */
    fun getColorsSortedByUsage(usageCounts: Map<String, Int>): List<TodoistColor> {
        return allColors.sortedWith(
            compareByDescending<TodoistColor> { usageCounts[it.id] ?: 0 }
                .thenBy { it.defaultOrder }
        )
    }

    /**
     * Sorts colors according to a custom manual order list (Mode B: CUSTOM).
     * Any colors not present in customOrder are appended at the end in default order.
     */
    fun getColorsByCustomOrder(customOrder: List<String>): List<TodoistColor> {
        if (customOrder.isEmpty()) return allColors
        val orderMap = customOrder.mapIndexed { index, id -> id to index }.toMap()
        return allColors.sortedWith(
            compareBy<TodoistColor> { orderMap[it.id] ?: (Int.MAX_VALUE - (allColors.size - it.defaultOrder)) }
                .thenBy { it.defaultOrder }
        )
    }
}
