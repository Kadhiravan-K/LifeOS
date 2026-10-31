package com.example.domain.model

data class HabitItem(
    val id: Long = 0,
    val profileId: Long = 1,
    val title: String,
    val emoji: String = "✨",
    val description: String = "",
    val reminderTime: String? = null,
    val colorHex: Long = 0xFF3B82F6,
    val completedDates: Set<String> = emptySet(), // "YYYY-MM-DD"
    val isCompletedToday: Boolean = false,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val weeklyCompletionRate: Float = 0f,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

object DefaultHabits {
    val list = listOf(
        HabitItem(
            title = "Wake up at 5:30 AM",
            emoji = "⏰",
            description = "Start the day with clarity and purpose",
            reminderTime = "05:30",
            colorHex = 0xFFF59E0B,
            orderIndex = 1
        ),
        HabitItem(
            title = "Make bed",
            emoji = "🛏️",
            description = "Accomplish the first task of the morning",
            reminderTime = "06:00",
            colorHex = 0xFF3B82F6,
            orderIndex = 2
        ),
        HabitItem(
            title = "Surya Namaskaram",
            emoji = "🧘",
            description = "12 cycles of revitalizing sun salutations",
            reminderTime = "06:15",
            colorHex = 0xFFF97316,
            orderIndex = 3
        ),
        HabitItem(
            title = "Morning exercise",
            emoji = "🏃",
            description = "Cardio, calisthenics or strength training",
            reminderTime = "06:45",
            colorHex = 0xFF10B981,
            orderIndex = 4
        ),
        HabitItem(
            title = "Meditation for 30 minutes",
            emoji = "🧘‍♂️",
            description = "Mindful breathing and mental calmness",
            reminderTime = "07:30",
            colorHex = 0xFF8B5CF6,
            orderIndex = 5
        ),
        HabitItem(
            title = "Drink 2 liters of water",
            emoji = "💧",
            description = "Stay hydrated throughout the day",
            reminderTime = "08:00",
            colorHex = 0xFF06B6D4,
            orderIndex = 6
        ),
        HabitItem(
            title = "Read 10 pages",
            emoji = "📖",
            description = "Continuous learning from a non-fiction book",
            reminderTime = "14:00",
            colorHex = 0xFF6366F1,
            orderIndex = 7
        ),
        HabitItem(
            title = "English practice",
            emoji = "🗣️",
            description = "Vocabulary, speaking or writing practice",
            reminderTime = "18:00",
            colorHex = 0xFFEC4899,
            orderIndex = 8
        ),
        HabitItem(
            title = "Sleep at 9:30 PM",
            emoji = "😴",
            description = "Rest and full cellular recovery for 8 hours",
            reminderTime = "21:30",
            colorHex = 0xFF475569,
            orderIndex = 9
        )
    )
}
