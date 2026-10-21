package com.example.domain.templates

import com.example.domain.model.ChallengeRule
import com.example.domain.model.RecurrenceType
import com.example.domain.model.ShortcutType
import com.example.domain.model.TaskPriority

/**
 * Curated offline catalog of structured LifeOS templates.
 * Fully bundled, 100% offline, zero network dependency.
 */
object TemplateCatalog {

    val list: List<LifeTemplate>
        get() = allTemplates

    val allTemplates: List<LifeTemplate> = listOf(
        // 1. Daily Routine
        LifeTemplate(
            id = "template_master_daily_flow",
            version = 1,
            title = "Master Daily Flow",
            description = "Balanced high-performance day: structured morning, focused deep work, hydration, and wind-down.",
            category = TemplateCategory.DAILY_ROUTINE,
            emoji = "☀️",
            tasks = listOf(
                TemplateTaskDef(
                    title = "Morning Intentions & Plan Review",
                    description = "Review today's top 3 priorities before checking messages",
                    category = "Routine",
                    priority = TaskPriority.HIGH,
                    dueTime = "06:30",
                    durationMinutes = 15,
                    recurrenceType = RecurrenceType.DAILY
                ),
                TemplateTaskDef(
                    title = "Deep Work Block #1",
                    description = "90 minutes distraction-free focus on the hardest priority",
                    category = "Focus",
                    priority = TaskPriority.HIGH,
                    dueTime = "09:00",
                    durationMinutes = 90,
                    recurrenceType = RecurrenceType.DAILY
                ),
                TemplateTaskDef(
                    title = "Midday Re-alignment & Walk",
                    description = "Step away from screens, hydrate, and stretch for 15 minutes",
                    category = "Health",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "13:00",
                    durationMinutes = 20,
                    recurrenceType = RecurrenceType.DAILY
                ),
                TemplateTaskDef(
                    title = "Daily Shutdown & Reset",
                    description = "Clear desk, update task progress, log achievements for tomorrow",
                    category = "Routine",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "18:00",
                    durationMinutes = 20,
                    recurrenceType = RecurrenceType.DAILY
                ),
                TemplateTaskDef(
                    title = "Sleep & Recovery (9:30 PM)",
                    description = "Consistent bedtime: screen-free 30 mins before sleep",
                    category = "Health",
                    priority = TaskPriority.HIGH,
                    dueTime = "21:30",
                    durationMinutes = 480,
                    recurrenceType = RecurrenceType.DAILY
                )
            ),
            habits = listOf(
                TemplateHabitDef(title = "Morning Hydration (500ml)", emoji = "💧", reminderTime = "06:15", colorHex = 0xFF2196F3),
                TemplateHabitDef(title = "Daily Reading (15 Mins)", emoji = "📖", reminderTime = "20:30", colorHex = 0xFF9C27B0),
                TemplateHabitDef(title = "Posture & Physical Stretch", emoji = "🧘", reminderTime = "12:30", colorHex = 0xFF4CAF50)
            ),
            shortcuts = listOf(
                TemplateShortcutDef(ShortcutType.WATER, "water", "Water Log", "💧", 0, 0),
                TemplateShortcutDef(ShortcutType.TASK, "Deep Work", "Deep Work", "⚡", 1, 0),
                TemplateShortcutDef(ShortcutType.HABIT, "Daily Reading", "Reading", "📖", 2, 0)
            ),
            projects = listOf(
                TemplateProjectDef("Daily Operations", "💼", "blue"),
                TemplateProjectDef("Personal Health", "🏃", "green")
            ),
            tags = listOf("routine", "productivity", "focus", "balanced")
        ),

        // 2. Morning Routine
        LifeTemplate(
            id = "template_high_perf_morning",
            version = 1,
            title = "High-Performance Morning",
            description = "Win the morning to win the day. Hydration, breathwork, movement, and zero phone for the first hour.",
            category = TemplateCategory.MORNING_ROUTINE,
            emoji = "🌅",
            tasks = listOf(
                TemplateTaskDef("Wake Up & Make Bed", "Start the day with an immediate small win", "Morning", TaskPriority.HIGH, "06:00", 10),
                TemplateTaskDef("Morning Sunlight & Breathwork", "10 minutes of direct sunlight and diaphragmatic breathing", "Morning", TaskPriority.MEDIUM, "06:15", 15),
                TemplateTaskDef("High-Protein Breakfast / Hydration", "Electrolytes and protein fueling", "Nutrition", TaskPriority.MEDIUM, "07:00", 30)
            ),
            habits = listOf(
                TemplateHabitDef("No Social Media (First 60m)", "📵", "Protect morning mental clarity", "06:00", 0xFFE91E63),
                TemplateHabitDef("Morning Movement / Mobility", "🏃", "15-minute bodyweight activation", "06:30", 0xFFFF9800)
            ),
            shortcuts = listOf(
                TemplateShortcutDef(ShortcutType.HABIT, "No Social Media", "No Phone", "📵", 0, 0),
                TemplateShortcutDef(ShortcutType.TASK, "Make Bed", "Make Bed", "🛏️", 1, 0)
            ),
            tags = listOf("morning", "discipline", "mindset")
        ),

        // 3. Evening Routine
        LifeTemplate(
            id = "template_deep_sleep_winddown",
            version = 1,
            title = "Deep Sleep & Wind-Down",
            description = "Optimized circadian wind-down routine for deeper REM and restorative sleep.",
            category = TemplateCategory.EVENING_ROUTINE,
            emoji = "🌙",
            tasks = listOf(
                TemplateTaskDef("Blue Light & Screen Curfew", "Turn off television, laptop, and phone screens", "Evening", TaskPriority.HIGH, "20:45", 15),
                TemplateTaskDef("Night Hygiene & Warm Shower", "Lower core body temperature to trigger sleepiness", "Evening", TaskPriority.MEDIUM, "21:00", 20),
                TemplateTaskDef("Plan Tomorrow's Top 3 Tasks", "Empty your working memory onto paper so you sleep peacefully", "Evening", TaskPriority.HIGH, "21:20", 10)
            ),
            habits = listOf(
                TemplateHabitDef("Evening Gratitude Journal", "📓", "Write 3 things you are grateful for", "21:15", 0xFF673AB7),
                TemplateHabitDef("Bedroom Pitch Black & Cool", "❄️", "Set room temperature to ~19°C", "21:25", 0xFF00BCD4)
            ),
            shortcuts = listOf(
                TemplateShortcutDef(ShortcutType.HABIT, "Evening Gratitude", "Journal", "📓", 0, 0),
                TemplateShortcutDef(ShortcutType.TASK, "Plan Tomorrow", "Tomorrow Plan", "📝", 1, 0)
            ),
            tags = listOf("sleep", "recovery", "evening")
        ),

        // 4. Productivity
        LifeTemplate(
            id = "template_deep_work_focus",
            version = 1,
            title = "Deep Work & Focus Mastery",
            description = "Cal Newport-inspired focus system: time-blocking, zero distractions, and structured cognitive output.",
            category = TemplateCategory.PRODUCTIVITY,
            emoji = "⚡",
            tasks = listOf(
                TemplateTaskDef("Deep Work Session 1: High Cognitive Task", "No tabs, full screen, airplane mode", "Deep Work", TaskPriority.HIGH, "09:30", 90),
                TemplateTaskDef("Shallow Work Batching (Email / Comms)", "Process all administrative and reply tasks in a single block", "Admin", TaskPriority.MEDIUM, "11:30", 45),
                TemplateTaskDef("Deep Work Session 2: Creation & Build", "Second focused block of the day", "Deep Work", TaskPriority.HIGH, "14:30", 90)
            ),
            habits = listOf(
                TemplateHabitDef("Zero Multi-tasking Focus", "🎯", "Single-task with full presence", "09:30", 0xFFF44336),
                TemplateHabitDef("Daily Desktop Cleanup", "🧹", "Zero files left on desktop", "17:30", 0xFF607D8B)
            ),
            shortcuts = listOf(
                TemplateShortcutDef(ShortcutType.TASK, "Deep Work 1", "Focus 1", "⚡", 0, 0),
                TemplateShortcutDef(ShortcutType.TASK, "Deep Work 2", "Focus 2", "🚀", 1, 0)
            ),
            projects = listOf(
                TemplateProjectDef("Deep Work Projects", "🧠", "purple")
            ),
            tags = listOf("focus", "deep work", "efficiency")
        ),

        // 5. Study & Learning
        LifeTemplate(
            id = "template_study_learning",
            version = 1,
            title = "Study & Knowledge Mastery",
            description = "Active recall, spaced repetition, flashcard reviews, and Pomodoro study sessions.",
            category = TemplateCategory.STUDY_LEARNING,
            emoji = "📚",
            tasks = listOf(
                TemplateTaskDef("Pomodoro Study Block 1 (Flashcards / Active Recall)", "Test yourself without looking at answers", "Study", TaskPriority.HIGH, "10:00", 50),
                TemplateTaskDef("Pomodoro Study Block 2 (Problem Solving)", "Apply theory to real exercises", "Study", TaskPriority.HIGH, "11:00", 50),
                TemplateTaskDef("Summarize Concepts in Your Own Words", "Feynman technique synthesis", "Review", TaskPriority.MEDIUM, "16:00", 30)
            ),
            habits = listOf(
                TemplateHabitDef("Read Academic Paper / Chapter", "📑", "At least 10 pages of serious text", "15:00", 0xFF3F51B5),
                TemplateHabitDef("Spaced Repetition Review", "🔁", "Complete daily Anki / card deck", "19:00", 0xFF009688)
            ),
            shortcuts = listOf(
                TemplateShortcutDef(ShortcutType.TASK, "Study Block 1", "Study 1", "📚", 0, 0),
                TemplateShortcutDef(ShortcutType.HABIT, "Read Paper", "Reading", "📑", 1, 0)
            ),
            projects = listOf(
                TemplateProjectDef("Coursework & Exams", "🎓", "yellow")
            ),
            tags = listOf("study", "learning", "memory")
        ),

        // 6. Fitness & Movement
        LifeTemplate(
            id = "template_fitness_movement",
            version = 1,
            title = "Strength, Cardio & Mobility",
            description = "Complete weekly fitness foundation with daily step goals, resistance training, and mobility recovery.",
            category = TemplateCategory.FITNESS,
            emoji = "💪",
            tasks = listOf(
                TemplateTaskDef("Strength & Resistance Training", "Push, pull, legs, or compound training", "Workout", TaskPriority.HIGH, "17:30", 60),
                TemplateTaskDef("Post-Workout Protein & Recovery", "Replenish glycogen and 30g protein within 45 mins", "Nutrition", TaskPriority.MEDIUM, "18:45", 15)
            ),
            habits = listOf(
                TemplateHabitDef("10,000 Steps Daily", "👟", "Walk outdoors or use walking pad", "18:00", 0xFFFF5722),
                TemplateHabitDef("Full Body Mobility & Foam Roll", "🧘", "10 minutes of hip and shoulder release", "20:00", 0xFF8BC34A)
            ),
            shortcuts = listOf(
                TemplateShortcutDef(ShortcutType.TASK, "Strength Training", "Workout", "💪", 0, 0),
                TemplateShortcutDef(ShortcutType.HABIT, "10,000 Steps", "10k Steps", "👟", 1, 0)
            ),
            projects = listOf(
                TemplateProjectDef("Body Transformation", "🏋️", "orange")
            ),
            tags = listOf("fitness", "strength", "health")
        ),

        // 7. Health & Hydration
        LifeTemplate(
            id = "template_health_hydration",
            version = 1,
            title = "Vitality & 2.5L Hydration",
            description = "Optimal energy levels through disciplined fluid intake, whole foods, and outdoor sunlight.",
            category = TemplateCategory.HEALTH_HYDRATION,
            emoji = "💧",
            tasks = listOf(
                TemplateTaskDef("Electrolyte Drink in Morning", "Water with pink salt and lemon for adrenal support", "Health", TaskPriority.MEDIUM, "06:30", 10),
                TemplateTaskDef("Whole-Food Nutritious Meal Prep", "Clean vegetables, lean proteins, and complex carbohydrates", "Nutrition", TaskPriority.HIGH, "12:00", 45)
            ),
            habits = listOf(
                TemplateHabitDef("Drink 2,500ml Water", "💧", "Track throughout the day in LifeOS", "16:00", 0xFF03A9F4),
                TemplateHabitDef("Daily Multivitamin & Omega-3", "💊", "Take with main meal", "13:00", 0xFFFFC107),
                TemplateHabitDef("No Refined Sugars", "🥗", "Stay clean from ultra-processed snacks", "20:00", 0xFF4CAF50)
            ),
            shortcuts = listOf(
                TemplateShortcutDef(ShortcutType.WATER, "water", "Water Log", "💧", 0, 0),
                TemplateShortcutDef(ShortcutType.HABIT, "Drink 2500ml", "Hydration", "🚰", 1, 0)
            ),
            tags = listOf("health", "hydration", "vitality")
        ),

        // 8. Habit Building
        LifeTemplate(
            id = "template_atomic_habits",
            version = 1,
            title = "Atomic Habit Fundamentals",
            description = "James Clear-inspired habit stacking: clear cues, small friction-free actions, and immediate tracking.",
            category = TemplateCategory.HABIT_BUILDING,
            emoji = "🌱",
            tasks = listOf(
                TemplateTaskDef("Habit Trigger Setup", "Place books on pillow, gym clothes by bed, water bottle on desk", "Environment", TaskPriority.MEDIUM, "21:00", 15)
            ),
            habits = listOf(
                TemplateHabitDef("Keystone Habit: 20 Pushups", "💪", "Immediately after brushing teeth", "07:00", 0xFFFF9800),
                TemplateHabitDef("Keystone Habit: Read 5 Pages", "📖", "Before opening morning email", "07:30", 0xFF9C27B0),
                TemplateHabitDef("Keystone Habit: 2-Minute Breath", "🌬️", "Before starting afternoon work session", "14:00", 0xFF00BCD4)
            ),
            shortcuts = listOf(
                TemplateShortcutDef(ShortcutType.HABIT, "20 Pushups", "Pushups", "💪", 0, 0),
                TemplateShortcutDef(ShortcutType.HABIT, "Read 5 Pages", "Read 5p", "📖", 1, 0)
            ),
            tags = listOf("habits", "consistency", "atomic")
        ),

        // 9. Weekly Planning
        LifeTemplate(
            id = "template_sunday_review",
            version = 1,
            title = "Sunday Life Review & Strategy",
            description = "The ultimate weekend reset: retrospective review, calendar audit, meal plan, and weekly OKRs.",
            category = TemplateCategory.WEEKLY_PLANNING,
            emoji = "📅",
            tasks = listOf(
                TemplateTaskDef(
                    title = "Weekly Retrospective & Wins",
                    description = "What went well? What caused friction? Review habit check-in rate",
                    category = "Review",
                    priority = TaskPriority.HIGH,
                    dueTime = "16:00",
                    durationMinutes = 30,
                    recurrenceType = RecurrenceType.SELECTED_WEEKDAYS,
                    recurrenceDaysOfWeek = listOf("SUNDAY")
                ),
                TemplateTaskDef(
                    title = "Calendar & Milestone Audit",
                    description = "Inspect deadlines, scheduled meetings, and allocate 3 primary focus blocks",
                    category = "Planning",
                    priority = TaskPriority.HIGH,
                    dueTime = "16:30",
                    durationMinutes = 30,
                    recurrenceType = RecurrenceType.SELECTED_WEEKDAYS,
                    recurrenceDaysOfWeek = listOf("SUNDAY")
                ),
                TemplateTaskDef(
                    title = "Laundry & Home Cleanliness Reset",
                    description = "Fresh sheets, laundry folded, desk sanitized for Monday morning",
                    category = "Home",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "17:30",
                    durationMinutes = 60,
                    recurrenceType = RecurrenceType.SELECTED_WEEKDAYS,
                    recurrenceDaysOfWeek = listOf("SUNDAY")
                )
            ),
            habits = listOf(
                TemplateHabitDef("Weekly Budget & Expense Review", "💰", "Log expenses and confirm financial goals", "18:00", 0xFF4CAF50)
            ),
            shortcuts = listOf(
                TemplateShortcutDef(ShortcutType.TASK, "Sunday Review", "Review", "📅", 0, 0),
                TemplateShortcutDef(ShortcutType.TASK, "Home Reset", "Cleaning", "🏠", 1, 0)
            ),
            projects = listOf(
                TemplateProjectDef("Life Operations", "🌟", "purple")
            ),
            tags = listOf("planning", "sunday", "weekly", "review")
        ),

        // 10. Personal Development
        LifeTemplate(
            id = "template_personal_growth",
            version = 1,
            title = "Personal Growth & Mindset",
            description = "Continuous self-evolution: daily reflective journaling, language practice, and lifelong skill building.",
            category = TemplateCategory.PERSONAL_DEV,
            emoji = "🧠",
            tasks = listOf(
                TemplateTaskDef("Skill Development Practice", "Dedicate 30 minutes to learning a hard skill (coding, design, instrument)", "Skill", TaskPriority.HIGH, "17:00", 45),
                TemplateTaskDef("Evening Stoic Journaling", "Reflect on actions, temperance, and how to improve tomorrow", "Mindset", TaskPriority.MEDIUM, "21:00", 15)
            ),
            habits = listOf(
                TemplateHabitDef("Language Practice (15 Mins)", "🗣️", "Vocabulary flashcards or conversational practice", "08:00", 0xFFE91E63),
                TemplateHabitDef("Podcast or Audiobook Walk", "🎧", "Listen to high-signal longform discussion while walking", "17:45", 0xFF2196F3)
            ),
            shortcuts = listOf(
                TemplateShortcutDef(ShortcutType.TASK, "Skill Practice", "Practice", "🧠", 0, 0),
                TemplateShortcutDef(ShortcutType.HABIT, "Language Practice", "Language", "🗣️", 1, 0)
            ),
            tags = listOf("growth", "journaling", "learning", "mindset")
        ),

        // 11. 75-Day Challenge
        LifeTemplate(
            id = "template_75_challenge_resilience",
            version = 1,
            title = "Iron Will 75 Challenge",
            description = "Mental toughness program: two 45-minute workouts, strict diet, 3.8L water, 10 pages reading, progress photo.",
            category = TemplateCategory.CHALLENGE_75,
            emoji = "🔥",
            tasks = listOf(
                TemplateTaskDef("Workout #1 (Outdoor 45m)", "45 minutes minimum outdoors regardless of weather", "Challenge", TaskPriority.HIGH, "06:30", 45),
                TemplateTaskDef("Workout #2 (Gym / Strength 45m)", "Second workout separated by at least 3 hours", "Challenge", TaskPriority.HIGH, "17:00", 45),
                TemplateTaskDef("Daily Non-Fiction Reading (10 Pages)", "10 pages of educational or self-development book", "Challenge", TaskPriority.HIGH, "20:30", 30)
            ),
            habits = listOf(
                TemplateHabitDef("Drink 3,800ml Water (1 Gallon)", "💧", "Zero alcohol, zero exceptions", "18:00", 0xFF0288D1),
                TemplateHabitDef("Strict Clean Diet (No Cheat Meals)", "🥗", "No sweets, no cheat snacks", "20:00", 0xFF388E3C),
                TemplateHabitDef("Daily Progress Photo", "📸", "Track physical transformation day by day", "21:00", 0xFF7B1FA2)
            ),
            challenge = TemplateChallengeDef(
                name = "75-Day Iron Will Challenge",
                targetDays = 75,
                rules = listOf(
                    ChallengeRule("rule_workout_1", "Workout #1 (45 mins outdoor)", "🏃", "Complete 45 minutes of exercise outdoors"),
                    ChallengeRule("rule_workout_2", "Workout #2 (45 mins)", "💪", "Complete second 45-minute workout"),
                    ChallengeRule("rule_water", "Drink 3.8 Liters of Water", "💧", "Drink one gallon / 3.8L of pure water"),
                    ChallengeRule("rule_diet", "Follow Strict Clean Diet", "🥗", "No alcohol, no cheat meals, clean eating"),
                    ChallengeRule("rule_reading", "Read 10 Pages Non-Fiction", "📖", "Read 10 pages of a physical book"),
                    ChallengeRule("rule_photo", "Take Daily Progress Photo", "📸", "Capture a daily progress picture")
                )
            ),
            shortcuts = listOf(
                TemplateShortcutDef(ShortcutType.WATER, "water", "Water Log", "💧", 0, 0),
                TemplateShortcutDef(ShortcutType.CHALLENGE_RULE, "rule_workout_1", "Workout 1", "🏃", 1, 0),
                TemplateShortcutDef(ShortcutType.CHALLENGE_RULE, "rule_reading", "Reading", "📖", 2, 0)
            ),
            tags = listOf("75hard", "discipline", "toughness", "challenge")
        )
    )

    fun getTemplateById(id: String): LifeTemplate? {
        return allTemplates.find { it.id == id }
    }

    fun getTemplatesByCategory(category: TemplateCategory): List<LifeTemplate> {
        return allTemplates.filter { it.category == category }
    }
}
