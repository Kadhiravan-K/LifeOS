package com.example.domain.templates

import com.example.domain.model.ChallengeRule
import com.example.domain.model.RecurrenceType
import com.example.domain.model.ShortcutType
import com.example.domain.model.TaskPriority

/**
 * Standard, meaningful project categories for LifeOS templates.
 * Enforces architectural rule: templates must use meaningful project categories
 * (Daily Habits, Home, Office, Personal, Health, Learning, Travel) and NEVER bundle names.
 */
object StandardProjects {
    val DAILY_HABITS = TemplateProjectDef("Daily Habits", "☀️", "amber", "Daily routines and recurring habits")
    val HOME = TemplateProjectDef("Home", "🏠", "orange", "Home maintenance, family, and domestic life")
    val OFFICE = TemplateProjectDef("Office", "💼", "blue", "Work, business, startup, and office execution")
    val PERSONAL = TemplateProjectDef("Personal", "👤", "purple", "Personal life, goals, relationships, and well-being")
    val HEALTH = TemplateProjectDef("Health", "🏃", "green", "Fitness, movement, nutrition, and hydration")
    val LEARNING = TemplateProjectDef("Learning", "📚", "teal", "Studies, reading, skills development, and research")
    val TRAVEL = TemplateProjectDef("Travel", "✈️", "indigo", "Trips, expeditions, itineraries, and packing")

    val ALL_NAMES = setOf("Daily Habits", "Home", "Office", "Personal", "Health", "Learning", "Travel")
    val ALL = listOf(DAILY_HABITS, HOME, OFFICE, PERSONAL, HEALTH, LEARNING, TRAVEL)
}

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
                    recurrenceType = RecurrenceType.DAILY,
                    projectName = "Daily Habits",
                    labels = listOf("routine", "planning")
                ),
                TemplateTaskDef(
                    title = "Deep Work Block #1",
                    description = "90 minutes distraction-free focus on the hardest priority",
                    category = "Focus",
                    priority = TaskPriority.HIGH,
                    dueTime = "09:00",
                    durationMinutes = 90,
                    recurrenceType = RecurrenceType.DAILY,
                    projectName = "Office",
                    labels = listOf("focus", "deep-work")
                ),
                TemplateTaskDef(
                    title = "Midday Re-alignment & Walk",
                    description = "Step away from screens, hydrate, and stretch for 15 minutes",
                    category = "Health",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "13:00",
                    durationMinutes = 20,
                    recurrenceType = RecurrenceType.DAILY,
                    projectName = "Health",
                    labels = listOf("wellness", "movement")
                ),
                TemplateTaskDef(
                    title = "Daily Shutdown & Reset",
                    description = "Clear desk, update task progress, log achievements for tomorrow",
                    category = "Routine",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "18:00",
                    durationMinutes = 20,
                    recurrenceType = RecurrenceType.DAILY,
                    projectName = "Daily Habits",
                    labels = listOf("routine", "wrapup")
                ),
                TemplateTaskDef(
                    title = "Sleep & Recovery (9:30 PM)",
                    description = "Consistent bedtime: screen-free 30 mins before sleep",
                    category = "Health",
                    priority = TaskPriority.HIGH,
                    dueTime = "21:30",
                    durationMinutes = 480,
                    recurrenceType = RecurrenceType.DAILY,
                    projectName = "Health",
                    labels = listOf("sleep", "recovery")
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
                StandardProjects.DAILY_HABITS,
                StandardProjects.OFFICE,
                StandardProjects.HEALTH
            ),
            tags = listOf("productivity", "daily", "balance", "core")
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
                TemplateTaskDef(
                    title = "Wake Up & Make Bed",
                    description = "Start the day with an immediate small win",
                    category = "Morning",
                    priority = TaskPriority.HIGH,
                    dueTime = "06:00",
                    durationMinutes = 10,
                    projectName = "Daily Habits",
                    labels = listOf("morning", "discipline")
                ),
                TemplateTaskDef(
                    title = "Morning Sunlight & Breathwork",
                    description = "10 minutes of direct sunlight and diaphragmatic breathing",
                    category = "Morning",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "06:15",
                    durationMinutes = 15,
                    projectName = "Health",
                    labels = listOf("morning", "breathwork")
                ),
                TemplateTaskDef(
                    title = "High-Protein Breakfast / Hydration",
                    description = "Electrolytes and protein fueling",
                    category = "Nutrition",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "07:00",
                    durationMinutes = 30,
                    projectName = "Health",
                    labels = listOf("nutrition", "energy")
                )
            ),
            habits = listOf(
                TemplateHabitDef("No Social Media (First 60m)", "📵", "Protect morning mental clarity", "06:00", 0xFFE91E63),
                TemplateHabitDef("Morning Movement / Mobility", "🏃", "15-minute bodyweight activation", "06:30", 0xFFFF9800)
            ),
            shortcuts = listOf(
                TemplateShortcutDef(ShortcutType.HABIT, "No Social Media", "No Phone", "📵", 0, 0),
                TemplateShortcutDef(ShortcutType.TASK, "Make Bed", "Make Bed", "🛏️", 1, 0)
            ),
            projects = listOf(
                StandardProjects.DAILY_HABITS,
                StandardProjects.HEALTH
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
                TemplateTaskDef(
                    title = "Blue Light & Screen Curfew",
                    description = "Turn off television, laptop, and phone screens",
                    category = "Evening",
                    priority = TaskPriority.HIGH,
                    dueTime = "20:45",
                    durationMinutes = 15,
                    projectName = "Daily Habits",
                    labels = listOf("evening", "sleep")
                ),
                TemplateTaskDef(
                    title = "Night Hygiene & Warm Shower",
                    description = "Lower core body temperature to trigger sleepiness",
                    category = "Evening",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "21:00",
                    durationMinutes = 20,
                    projectName = "Daily Habits",
                    labels = listOf("hygiene", "relaxation")
                ),
                TemplateTaskDef(
                    title = "Plan Tomorrow's Top 3 Tasks",
                    description = "Empty your working memory onto paper so you sleep peacefully",
                    category = "Evening",
                    priority = TaskPriority.HIGH,
                    dueTime = "21:20",
                    durationMinutes = 10,
                    projectName = "Daily Habits",
                    labels = listOf("planning", "organization")
                )
            ),
            habits = listOf(
                TemplateHabitDef("Evening Gratitude Journal", "📓", "Write 3 things you are grateful for", "21:15", 0xFF673AB7),
                TemplateHabitDef("Bedroom Pitch Black & Cool", "❄️", "Set room temperature to ~19°C", "21:25", 0xFF00BCD4)
            ),
            shortcuts = listOf(
                TemplateShortcutDef(ShortcutType.HABIT, "Evening Gratitude", "Journal", "📓", 0, 0),
                TemplateShortcutDef(ShortcutType.TASK, "Plan Tomorrow", "Tomorrow Plan", "📝", 1, 0)
            ),
            projects = listOf(
                StandardProjects.DAILY_HABITS,
                StandardProjects.HEALTH
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
                TemplateTaskDef(
                    title = "Deep Work Session 1: High Cognitive Task",
                    description = "No tabs, full screen, airplane mode",
                    category = "Deep Work",
                    priority = TaskPriority.HIGH,
                    dueTime = "09:30",
                    durationMinutes = 90,
                    projectName = "Office",
                    labels = listOf("focus", "deep-work")
                ),
                TemplateTaskDef(
                    title = "Shallow Work Batching (Email / Comms)",
                    description = "Process all administrative and reply tasks in a single block",
                    category = "Admin",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "11:30",
                    durationMinutes = 45,
                    projectName = "Office",
                    labels = listOf("admin", "communication")
                ),
                TemplateTaskDef(
                    title = "Deep Work Session 2: Creation & Build",
                    description = "Second focused block of the day",
                    category = "Deep Work",
                    priority = TaskPriority.HIGH,
                    dueTime = "14:30",
                    durationMinutes = 90,
                    projectName = "Office",
                    labels = listOf("focus", "creation")
                )
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
                StandardProjects.OFFICE
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
                TemplateTaskDef(
                    title = "Pomodoro Study Block 1 (Flashcards / Active Recall)",
                    description = "Test yourself without looking at answers",
                    category = "Study",
                    priority = TaskPriority.HIGH,
                    dueTime = "10:00",
                    durationMinutes = 50,
                    projectName = "Learning",
                    labels = listOf("study", "recall")
                ),
                TemplateTaskDef(
                    title = "Pomodoro Study Block 2 (Problem Solving)",
                    description = "Apply theory to real exercises",
                    category = "Study",
                    priority = TaskPriority.HIGH,
                    dueTime = "11:00",
                    durationMinutes = 50,
                    projectName = "Learning",
                    labels = listOf("study", "practice")
                ),
                TemplateTaskDef(
                    title = "Summarize Concepts in Your Own Words",
                    description = "Feynman technique synthesis",
                    category = "Review",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "16:00",
                    durationMinutes = 30,
                    projectName = "Learning",
                    labels = listOf("study", "synthesis")
                )
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
                StandardProjects.LEARNING
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
                TemplateTaskDef(
                    title = "Strength & Resistance Training",
                    description = "Push, pull, legs, or compound training",
                    category = "Workout",
                    priority = TaskPriority.HIGH,
                    dueTime = "17:30",
                    durationMinutes = 60,
                    projectName = "Health",
                    labels = listOf("fitness", "strength")
                ),
                TemplateTaskDef(
                    title = "Post-Workout Protein & Recovery",
                    description = "Replenish glycogen and 30g protein within 45 mins",
                    category = "Nutrition",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "18:45",
                    durationMinutes = 15,
                    projectName = "Health",
                    labels = listOf("nutrition", "recovery")
                )
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
                StandardProjects.HEALTH
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
                TemplateTaskDef(
                    title = "Electrolyte Drink in Morning",
                    description = "Water with pink salt and lemon for adrenal support",
                    category = "Health",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "06:30",
                    durationMinutes = 10,
                    projectName = "Health",
                    labels = listOf("hydration", "wellness")
                ),
                TemplateTaskDef(
                    title = "Whole-Food Nutritious Meal Prep",
                    description = "Clean vegetables, lean proteins, and complex carbohydrates",
                    category = "Nutrition",
                    priority = TaskPriority.HIGH,
                    dueTime = "12:00",
                    durationMinutes = 45,
                    projectName = "Health",
                    labels = listOf("nutrition", "health")
                )
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
            projects = listOf(
                StandardProjects.HEALTH
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
                TemplateTaskDef(
                    title = "Habit Trigger Setup",
                    description = "Place books on pillow, gym clothes by bed, water bottle on desk",
                    category = "Environment",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "21:00",
                    durationMinutes = 15,
                    projectName = "Daily Habits",
                    labels = listOf("habits", "environment")
                )
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
            projects = listOf(
                StandardProjects.DAILY_HABITS
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
                    recurrenceDaysOfWeek = listOf("SUNDAY"),
                    projectName = "Daily Habits",
                    labels = listOf("review", "milestones")
                ),
                TemplateTaskDef(
                    title = "Calendar & Milestone Audit",
                    description = "Inspect deadlines, scheduled meetings, and allocate 3 primary focus blocks",
                    category = "Planning",
                    priority = TaskPriority.HIGH,
                    dueTime = "16:30",
                    durationMinutes = 30,
                    recurrenceType = RecurrenceType.SELECTED_WEEKDAYS,
                    recurrenceDaysOfWeek = listOf("SUNDAY"),
                    projectName = "Daily Habits",
                    labels = listOf("planning", "calendar")
                ),
                TemplateTaskDef(
                    title = "Laundry & Home Cleanliness Reset",
                    description = "Fresh sheets, laundry folded, desk sanitized for Monday morning",
                    category = "Home",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "17:30",
                    durationMinutes = 60,
                    recurrenceType = RecurrenceType.SELECTED_WEEKDAYS,
                    recurrenceDaysOfWeek = listOf("SUNDAY"),
                    projectName = "Home",
                    labels = listOf("home", "cleaning")
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
                StandardProjects.DAILY_HABITS,
                StandardProjects.HOME
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
                TemplateTaskDef(
                    title = "Skill Development Practice",
                    description = "Dedicate 30 minutes to learning a hard skill (coding, design, instrument)",
                    category = "Skill",
                    priority = TaskPriority.HIGH,
                    dueTime = "17:00",
                    durationMinutes = 45,
                    projectName = "Learning",
                    labels = listOf("skill", "learning")
                ),
                TemplateTaskDef(
                    title = "Evening Stoic Journaling",
                    description = "Reflect on actions, temperance, and how to improve tomorrow",
                    category = "Mindset",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "21:00",
                    durationMinutes = 15,
                    projectName = "Personal",
                    labels = listOf("journaling", "reflection")
                )
            ),
            habits = listOf(
                TemplateHabitDef("Language Practice (15 Mins)", "🗣️", "Vocabulary flashcards or conversational practice", "08:00", 0xFFE91E63),
                TemplateHabitDef("Podcast or Audiobook Walk", "🎧", "Listen to high-signal longform discussion while walking", "17:45", 0xFF2196F3)
            ),
            shortcuts = listOf(
                TemplateShortcutDef(ShortcutType.TASK, "Skill Practice", "Practice", "🧠", 0, 0),
                TemplateShortcutDef(ShortcutType.HABIT, "Language Practice", "Language", "🗣️", 1, 0)
            ),
            projects = listOf(
                StandardProjects.LEARNING,
                StandardProjects.PERSONAL
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
                TemplateTaskDef(
                    title = "Workout #1 (Outdoor 45m)",
                    description = "45 minutes minimum outdoors regardless of weather",
                    category = "Challenge",
                    priority = TaskPriority.HIGH,
                    dueTime = "06:30",
                    durationMinutes = 45,
                    projectName = "Health",
                    labels = listOf("challenge", "workout")
                ),
                TemplateTaskDef(
                    title = "Workout #2 (Gym / Strength 45m)",
                    description = "Second workout separated by at least 3 hours",
                    category = "Challenge",
                    priority = TaskPriority.HIGH,
                    dueTime = "17:00",
                    durationMinutes = 45,
                    projectName = "Health",
                    labels = listOf("challenge", "workout")
                ),
                TemplateTaskDef(
                    title = "Daily Non-Fiction Reading (10 Pages)",
                    description = "10 pages of educational or self-development book",
                    category = "Challenge",
                    priority = TaskPriority.HIGH,
                    dueTime = "20:30",
                    durationMinutes = 30,
                    projectName = "Learning",
                    labels = listOf("challenge", "reading")
                )
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
            projects = listOf(
                StandardProjects.HEALTH,
                StandardProjects.LEARNING
            ),
            tags = listOf("75hard", "discipline", "toughness", "challenge")
        ),

        // 12. Special Collection: Life Bucket List
        LifeTemplate(
            id = "special_life_bucket_list",
            version = 1,
            title = "Life Bucket List",
            description = "Lifelong aspirations, transformative milestones, and unforgettable experiences to achieve in your lifetime.",
            category = TemplateCategory.BUCKET_LIST,
            emoji = "🌟",
            tasks = listOf(
                TemplateTaskDef(
                    title = "Run a 10K or Half-Marathon",
                    description = "Train consistently and cross the finish line",
                    category = "Milestone",
                    priority = TaskPriority.HIGH,
                    dueTime = null,
                    durationMinutes = 120,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Personal",
                    labels = listOf("milestone", "fitness")
                ),
                TemplateTaskDef(
                    title = "Master an Instrument Piece",
                    description = "Learn to play a complete classic song with fluency",
                    category = "Skill",
                    priority = TaskPriority.MEDIUM,
                    dueTime = null,
                    durationMinutes = 45,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Personal",
                    labels = listOf("skill", "music")
                ),
                TemplateTaskDef(
                    title = "Write and Publish a Creative Work",
                    description = "Complete a book chapter, essay, or creative article",
                    category = "Legacy",
                    priority = TaskPriority.HIGH,
                    dueTime = null,
                    durationMinutes = 90,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Personal",
                    labels = listOf("legacy", "writing")
                ),
                TemplateTaskDef(
                    title = "Mentor Someone Towards Their Dream Goal",
                    description = "Invest guidance into someone building their future",
                    category = "Impact",
                    priority = TaskPriority.HIGH,
                    dueTime = null,
                    durationMinutes = 60,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Personal",
                    labels = listOf("impact", "mentorship")
                ),
                TemplateTaskDef(
                    title = "Plant a Tree and Watch It Grow",
                    description = "Contribute positively to the Earth and leave roots behind",
                    category = "Nature",
                    priority = TaskPriority.MEDIUM,
                    dueTime = null,
                    durationMinutes = 60,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Personal",
                    labels = listOf("nature", "growth")
                )
            ),
            projects = listOf(
                StandardProjects.PERSONAL,
                StandardProjects.TRAVEL
            ),
            tags = listOf("bucketlist", "dreams", "goals", "milestones")
        ),

        // 13. Special Collection: Couples Bucket List ❤️
        LifeTemplate(
            id = "special_couples_bucket_list",
            version = 1,
            title = "Couples Bucket List ❤️",
            description = "Meaningful shared adventures, intimate traditions, and memorable milestones for couples.",
            category = TemplateCategory.BUCKET_LIST,
            emoji = "❤️",
            tasks = listOf(
                TemplateTaskDef(
                    title = "Sunrise Breakfast Picnic",
                    description = "Pack warm coffee and watch the sunrise from a scenic peak",
                    category = "Romance",
                    priority = TaskPriority.HIGH,
                    dueTime = "06:00",
                    durationMinutes = 90,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Personal",
                    labels = listOf("romance", "date")
                ),
                TemplateTaskDef(
                    title = "Cook an Authentic 3-Course Feast Together",
                    description = "Select an international recipe and cook every dish from scratch",
                    category = "Date Night",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "18:00",
                    durationMinutes = 120,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Home",
                    labels = listOf("cooking", "date")
                ),
                TemplateTaskDef(
                    title = "Unplugged Weekend Cabin Retreat",
                    description = "48 hours in nature with zero smartphone distractions",
                    category = "Connection",
                    priority = TaskPriority.HIGH,
                    dueTime = null,
                    durationMinutes = 2880,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Travel",
                    labels = listOf("connection", "travel")
                ),
                TemplateTaskDef(
                    title = "Midnight Stargazing with Hot Cocoa",
                    description = "Lie back on a blanket and map constellations together",
                    category = "Romance",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "22:00",
                    durationMinutes = 60,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Personal",
                    labels = listOf("romance", "night")
                ),
                TemplateTaskDef(
                    title = "Shared Wellness & Spa Day",
                    description = "Recharge together with massage, sauna, and deep relaxation",
                    category = "Wellness",
                    priority = TaskPriority.MEDIUM,
                    dueTime = null,
                    durationMinutes = 180,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Personal",
                    labels = listOf("wellness", "relaxation")
                )
            ),
            projects = listOf(
                StandardProjects.PERSONAL,
                StandardProjects.HOME,
                StandardProjects.TRAVEL
            ),
            tags = listOf("couples", "love", "date", "bucketlist", "relationship")
        ),

        // 14. Special Collection: Travel & Exploration
        LifeTemplate(
            id = "special_travel_bucket_list",
            version = 1,
            title = "Travel & Exploration",
            description = "Broaden horizons through cultural expeditions, scenic train voyages, and awe-inspiring historic marvels.",
            category = TemplateCategory.BUCKET_LIST,
            emoji = "✈️",
            tasks = listOf(
                TemplateTaskDef(
                    title = "Solo Expedition to a Historic Capital",
                    description = "Navigate, explore, and immerse in local history solo",
                    category = "Travel",
                    priority = TaskPriority.HIGH,
                    dueTime = null,
                    durationMinutes = 1440,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Travel",
                    labels = listOf("travel", "culture")
                ),
                TemplateTaskDef(
                    title = "Scenic Train Journey Across Mountains",
                    description = "Experience panoramic rail transit across peaks and valleys",
                    category = "Adventure",
                    priority = TaskPriority.MEDIUM,
                    dueTime = null,
                    durationMinutes = 360,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Travel",
                    labels = listOf("travel", "scenic")
                ),
                TemplateTaskDef(
                    title = "Explore an Ancient UNESCO Heritage Site",
                    description = "Witness ancient architectural mastery and culture in person",
                    category = "Culture",
                    priority = TaskPriority.HIGH,
                    dueTime = null,
                    durationMinutes = 240,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Travel",
                    labels = listOf("culture", "history")
                ),
                TemplateTaskDef(
                    title = "Coastal Road Trip with Ocean Views",
                    description = "Drive along dramatic coastal highways with favorite soundtrack",
                    category = "Exploration",
                    priority = TaskPriority.MEDIUM,
                    dueTime = null,
                    durationMinutes = 480,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Travel",
                    labels = listOf("travel", "roadtrip")
                ),
                TemplateTaskDef(
                    title = "Camp Under an Open Night Sky",
                    description = "Pitch a tent away from city lights and fall asleep under stars",
                    category = "Outdoors",
                    priority = TaskPriority.MEDIUM,
                    dueTime = null,
                    durationMinutes = 720,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Travel",
                    labels = listOf("outdoors", "camping")
                )
            ),
            projects = listOf(
                StandardProjects.TRAVEL
            ),
            tags = listOf("travel", "wanderlust", "adventure", "explore")
        ),

        // 15. Special Collection: Adventure & Thrills
        LifeTemplate(
            id = "special_adventure_bucket_list",
            version = 1,
            title = "Adventure & Thrills",
            description = "Heart-pounding outdoor challenges, wilderness summits, and adrenaline-charged exploits.",
            category = TemplateCategory.BUCKET_LIST,
            emoji = "🏔️",
            tasks = listOf(
                TemplateTaskDef(
                    title = "Summit a Mountain Peak",
                    description = "Hike to the summit and take in the panoramic vista from above the clouds",
                    category = "Trek",
                    priority = TaskPriority.HIGH,
                    dueTime = null,
                    durationMinutes = 360,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Travel",
                    labels = listOf("adventure", "hiking")
                ),
                TemplateTaskDef(
                    title = "Whitewater River Rafting / Kayak Run",
                    description = "Navigate roaring rapids with teamwork and adrenaline",
                    category = "Water Sport",
                    priority = TaskPriority.HIGH,
                    dueTime = null,
                    durationMinutes = 180,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Travel",
                    labels = listOf("adventure", "watersport")
                ),
                TemplateTaskDef(
                    title = "Scuba Dive or Snorkel a Coral Reef",
                    description = "Descend beneath the surface to explore vibrant marine ecology",
                    category = "Ocean",
                    priority = TaskPriority.MEDIUM,
                    dueTime = null,
                    durationMinutes = 120,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Travel",
                    labels = listOf("adventure", "ocean")
                ),
                TemplateTaskDef(
                    title = "Multi-Day Wilderness Backpacking Trail",
                    description = "Carry everything on your back and navigate wild terrain",
                    category = "Survival",
                    priority = TaskPriority.HIGH,
                    dueTime = null,
                    durationMinutes = 2880,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Travel",
                    labels = listOf("adventure", "survival")
                ),
                TemplateTaskDef(
                    title = "Rock Climbing / Bouldering Challenge",
                    description = "Conquer a technical outdoor or gym climbing route",
                    category = "Fitness",
                    priority = TaskPriority.MEDIUM,
                    dueTime = null,
                    durationMinutes = 90,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Health",
                    labels = listOf("fitness", "climbing")
                )
            ),
            projects = listOf(
                StandardProjects.TRAVEL,
                StandardProjects.HEALTH
            ),
            tags = listOf("adventure", "hiking", "nature", "outdoor", "thrills")
        ),

        // =========================================================================
        // 16. SET-04 BUNDLE: Bucket List
        // =========================================================================
        LifeTemplate(
            id = "template_bucket_list",
            version = 1,
            title = "Bucket List",
            description = "Lifelong aspirations, transformative milestones, and unforgettable experiences to achieve in your lifetime.",
            category = TemplateCategory.BUCKET_LIST,
            emoji = "🌟",
            tasks = listOf(
                TemplateTaskDef(
                    title = "Complete a Half-Marathon / 10K Run",
                    description = "Train consistently with dedicated long runs and cross the finish line",
                    category = "Milestone",
                    priority = TaskPriority.HIGH,
                    durationMinutes = 120,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Personal",
                    labels = listOf("goals", "fitness")
                ),
                TemplateTaskDef(
                    title = "Learn to Play a Favorite Instrument Song",
                    description = "Master guitar, piano, or violin piece with fluency and confidence",
                    category = "Skill",
                    priority = TaskPriority.MEDIUM,
                    durationMinutes = 60,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Personal",
                    labels = listOf("skill", "music")
                ),
                TemplateTaskDef(
                    title = "Write and Publish a Personal Creative Work",
                    description = "Complete an essay, book chapter, short story, or portfolio showcase",
                    category = "Legacy",
                    priority = TaskPriority.HIGH,
                    durationMinutes = 90,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Personal",
                    labels = listOf("goals", "legacy")
                ),
                TemplateTaskDef(
                    title = "Witness the Northern Lights or Solar Eclipse",
                    description = "Travel to optimal viewing coordinates and experience the natural cosmic wonder",
                    category = "Adventure",
                    priority = TaskPriority.HIGH,
                    durationMinutes = 240,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Travel",
                    labels = listOf("adventure", "travel")
                ),
                TemplateTaskDef(
                    title = "Plant a Fruit Tree and Harvest First Crop",
                    description = "Plant roots, nurture the soil, and enjoy home-grown harvest with family",
                    category = "Nature",
                    priority = TaskPriority.MEDIUM,
                    durationMinutes = 60,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Personal",
                    labels = listOf("nature", "legacy")
                )
            ),
            habits = listOf(
                TemplateHabitDef("Weekly Bucket List Action", "🌟", "Spend 30 minutes advancing one lifelong goal", "10:00", 0xFF9C27B0)
            ),
            projects = listOf(
                StandardProjects.PERSONAL,
                StandardProjects.TRAVEL
            ),
            tags = listOf("bucketlist", "dreams", "aspirations", "goals")
        ),

        // =========================================================================
        // 17. SET-04 BUNDLE: Couples Goals
        // =========================================================================
        LifeTemplate(
            id = "template_couples_goals",
            version = 1,
            title = "Couples Goals",
            description = "Strengthen connection, shared values, and relationship happiness through intentional shared habits.",
            category = TemplateCategory.COUPLES_GOALS,
            emoji = "💑",
            tasks = listOf(
                TemplateTaskDef(
                    title = "Schedule Weekly Distraction-Free Date Night",
                    description = "Reserve an evening every week with phones put away for quality presence",
                    category = "Romance",
                    priority = TaskPriority.HIGH,
                    dueTime = "19:00",
                    durationMinutes = 120,
                    recurrenceType = RecurrenceType.WEEKLY,
                    recurrenceDaysOfWeek = listOf("FRIDAY"),
                    projectName = "Personal",
                    labels = listOf("relationship", "romance")
                ),
                TemplateTaskDef(
                    title = "Monthly Couples Budget & Savings Sync",
                    description = "Review joint expenses, upcoming vacation funds, and long-term milestones",
                    category = "Finance",
                    priority = TaskPriority.HIGH,
                    dueTime = "10:00",
                    durationMinutes = 45,
                    recurrenceType = RecurrenceType.MONTHLY,
                    projectName = "Personal",
                    labels = listOf("finances", "planning")
                ),
                TemplateTaskDef(
                    title = "Plan an Annual Unplugged Anniversary Getaway",
                    description = "Curate an itinerary for intimate rest, fine dining, and relaxation",
                    category = "Travel",
                    priority = TaskPriority.MEDIUM,
                    durationMinutes = 60,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Personal",
                    labels = listOf("relationship", "travel")
                ),
                TemplateTaskDef(
                    title = "Cook a Gourmet Multi-Course Meal Together",
                    description = "Choose a new international cuisine and prepare appetizers, main, and dessert together",
                    category = "Home",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "18:00",
                    durationMinutes = 90,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Home",
                    labels = listOf("bonding", "home")
                ),
                TemplateTaskDef(
                    title = "Living Space Declutter & Co-design Project",
                    description = "Reorganize the bedroom or lounge to build a calm, aesthetically pleasing home sanctuary",
                    category = "Home",
                    priority = TaskPriority.MEDIUM,
                    durationMinutes = 90,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Home",
                    labels = listOf("home", "cozy")
                )
            ),
            habits = listOf(
                TemplateHabitDef("Daily Couples Gratitude Check-in", "❤️", "Share 1 thing you appreciate about your partner", "21:30", 0xFFE91E63),
                TemplateHabitDef("Evening Walk & Conversation", "🚶", "15-minute screen-free neighborhood walk together", "19:30", 0xFF4CAF50)
            ),
            projects = listOf(
                StandardProjects.PERSONAL,
                StandardProjects.HOME
            ),
            tags = listOf("couples", "love", "relationship", "connection")
        ),

        // =========================================================================
        // 18. SET-04 BUNDLE: Trip Packing Checklist
        // =========================================================================
        LifeTemplate(
            id = "template_trip_packing",
            version = 1,
            title = "Trip Packing Checklist",
            description = "Foolproof luggage packing checklist for business, vacation, flight, and road trip journeys.",
            category = TemplateCategory.TRAVEL_CHECKLIST,
            emoji = "🧳",
            tasks = listOf(
                TemplateTaskDef(
                    title = "Passports, IDs, Travel Insurance & Boarding Passes",
                    description = "Check passport validity (>6 months), printed visas, and digital wallet passes",
                    category = "Travel",
                    priority = TaskPriority.HIGH,
                    dueTime = "09:00",
                    durationMinutes = 20,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Travel",
                    labels = listOf("documents", "critical")
                ),
                TemplateTaskDef(
                    title = "Prescription Medications & Compact First-Aid Kit",
                    description = "Daily meds, motion sickness tablets, pain relief, and doctor notes in carry-on",
                    category = "Health",
                    priority = TaskPriority.HIGH,
                    dueTime = "10:00",
                    durationMinutes = 20,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Health",
                    labels = listOf("health", "essentials")
                ),
                TemplateTaskDef(
                    title = "Universal Travel Power Adapters & Portable Battery",
                    description = "Pack wall adapters, charging cables for phone/laptop, and 10,000mAh power bank",
                    category = "Tech",
                    priority = TaskPriority.HIGH,
                    dueTime = "11:00",
                    durationMinutes = 15,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Travel",
                    labels = listOf("electronics", "travel")
                ),
                TemplateTaskDef(
                    title = "Weather-Appropriate Attire, Layers & Walking Footwear",
                    description = "Pack daily outfits, versatile rain/wind jacket, comfortable shoes, and socks",
                    category = "Packing",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "14:00",
                    durationMinutes = 45,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Personal",
                    labels = listOf("clothing", "packing")
                ),
                TemplateTaskDef(
                    title = "Toiletries & TSA-Compliant 100ml Liquid Kit",
                    description = "Toothbrush, travel toothpaste, deodorant, shampoo, sunscreen, and moisturizer",
                    category = "Hygiene",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "15:00",
                    durationMinutes = 20,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Personal",
                    labels = listOf("toiletries", "essentials")
                ),
                TemplateTaskDef(
                    title = "Download Offline Maps, City Guides & Itineraries",
                    description = "Cache Google Maps, translation dictionaries, flight confirmations, and hotel vouchers",
                    category = "Logistics",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "16:00",
                    durationMinutes = 20,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Travel",
                    labels = listOf("navigation", "travel")
                )
            ),
            projects = listOf(
                StandardProjects.TRAVEL,
                StandardProjects.PERSONAL
            ),
            tags = listOf("packing", "travel", "checklist", "luggage", "vacation")
        ),

        // =========================================================================
        // 19. SET-04 BUNDLE: Marriage Planning Checklist
        // =========================================================================
        LifeTemplate(
            id = "template_marriage_planning",
            version = 1,
            title = "Marriage Planning Checklist",
            description = "Structured roadmap for organizing your dream wedding: budget, vendors, invitations, and ceremony.",
            category = TemplateCategory.EVENT_PLANNING,
            emoji = "💍",
            tasks = listOf(
                TemplateTaskDef(
                    title = "Define Overall Wedding Budget & Allocation Framework",
                    description = "Set realistic maximum expenditure, emergency buffer (10%), and contribution shares",
                    category = "Budget",
                    priority = TaskPriority.HIGH,
                    dueTime = "10:00",
                    durationMinutes = 60,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Personal",
                    labels = listOf("wedding", "budget", "finance")
                ),
                TemplateTaskDef(
                    title = "Draft Preliminary Guest List & RSVP Tracker",
                    description = "Estimate headcount for Tier A and Tier B guests to inform venue capacity",
                    category = "Guests",
                    priority = TaskPriority.HIGH,
                    dueTime = "14:00",
                    durationMinutes = 60,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Personal",
                    labels = listOf("wedding", "logistics")
                ),
                TemplateTaskDef(
                    title = "Tour, Select & Reserve Ceremony & Reception Venues",
                    description = "Visit shortlisted venues, confirm catering availability, and lock deposit",
                    category = "Venue",
                    priority = TaskPriority.HIGH,
                    dueTime = "11:00",
                    durationMinutes = 180,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Personal",
                    labels = listOf("wedding", "venue", "logistics")
                ),
                TemplateTaskDef(
                    title = "Book Key Vendors: Photographer, Caterer, Music",
                    description = "Interview, review contracts, and secure photographer, DJ/band, and food tasting",
                    category = "Vendors",
                    priority = TaskPriority.HIGH,
                    dueTime = "15:00",
                    durationMinutes = 90,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Office",
                    labels = listOf("wedding", "vendors", "contracts")
                ),
                TemplateTaskDef(
                    title = "Design & Mail Save-the-Dates and Wedding Invitations",
                    description = "Finalize invitation stationery, website link, dietary queries, and postage",
                    category = "Stationery",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "16:00",
                    durationMinutes = 60,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Personal",
                    labels = listOf("wedding", "invitations")
                ),
                TemplateTaskDef(
                    title = "Obtain Marriage License & Legal Filings",
                    description = "Submit legal registrar documentation, verify witness requirements, and pay fees",
                    category = "Legal",
                    priority = TaskPriority.HIGH,
                    dueTime = "09:30",
                    durationMinutes = 60,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Personal",
                    labels = listOf("wedding", "legal", "critical")
                )
            ),
            projects = listOf(
                StandardProjects.PERSONAL,
                StandardProjects.OFFICE
            ),
            tags = listOf("wedding", "marriage", "planning", "event", "ceremony")
        ),

        // =========================================================================
        // 20. SET-04 BUNDLE: Startup Setup Checklist
        // =========================================================================
        LifeTemplate(
            id = "template_startup_setup",
            version = 1,
            title = "Startup Setup Checklist",
            description = "Lean, battle-tested roadmap for launching an early-stage startup from idea to first customers.",
            category = TemplateCategory.STARTUP_LAUNCH,
            emoji = "🚀",
            tasks = listOf(
                TemplateTaskDef(
                    title = "Register Corporate Legal Entity & Employer Identification",
                    description = "Incorporate LLC/C-Corp, obtain EIN, founder agreement, and IP assignment",
                    category = "Legal",
                    priority = TaskPriority.HIGH,
                    dueTime = "10:00",
                    durationMinutes = 90,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Office",
                    labels = listOf("startup", "legal", "compliance")
                ),
                TemplateTaskDef(
                    title = "Open Dedicated Business Banking & Merchant Accounts",
                    description = "Establish zero-fee business checking account, debit cards, and Stripe integration",
                    category = "Finance",
                    priority = TaskPriority.HIGH,
                    dueTime = "13:00",
                    durationMinutes = 45,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Office",
                    labels = listOf("startup", "banking", "finance")
                ),
                TemplateTaskDef(
                    title = "Reserve Domain Name, Primary Workspace & Handles",
                    description = "Secure .com domain, Google Workspace email, GitHub org, and X/LinkedIn handles",
                    category = "Setup",
                    priority = TaskPriority.HIGH,
                    dueTime = "15:00",
                    durationMinutes = 30,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Office",
                    labels = listOf("startup", "infrastructure")
                ),
                TemplateTaskDef(
                    title = "Launch Clean MVP Landing Page & Value Proposition",
                    description = "Single-page responsive site with clear problem statement, demo video, and signup form",
                    category = "Build",
                    priority = TaskPriority.HIGH,
                    dueTime = "16:00",
                    durationMinutes = 180,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Office",
                    labels = listOf("startup", "mvp", "growth")
                ),
                TemplateTaskDef(
                    title = "Conduct 15 Targeted Customer Problem Interviews",
                    description = "Validate pain points, current manual workarounds, and willingness to pay with ideal users",
                    category = "Research",
                    priority = TaskPriority.HIGH,
                    dueTime = "11:00",
                    durationMinutes = 120,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Learning",
                    labels = listOf("startup", "research", "interviews")
                ),
                TemplateTaskDef(
                    title = "Establish Core Metrics Dashboard & Customer Feedback Loop",
                    description = "Set up weekly active users, CAC, retention metrics, and customer support channels",
                    category = "Operations",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "17:00",
                    durationMinutes = 60,
                    recurrenceType = RecurrenceType.NONE,
                    projectName = "Office",
                    labels = listOf("startup", "analytics", "operations")
                )
            ),
            projects = listOf(
                StandardProjects.OFFICE,
                StandardProjects.LEARNING
            ),
            tags = listOf("startup", "business", "entrepreneur", "mvp", "launch")
        ),

        // 17. Developer Acceptance & QA Test Suite
        LifeTemplate(
            id = "template_dev_test",
            version = 1,
            title = "Developer & QA Test Suite",
            description = "High-velocity physical device verification bundle: includes all 4 priority levels (URGENT to LOW), daily and weekly recurrence, standard projects, rich labels, habits, and shortcuts.",
            category = TemplateCategory.PRODUCTIVITY,
            emoji = "🧪",
            tasks = listOf(
                TemplateTaskDef(
                    title = "[URGENT/P1] Critical Smoke Test & Priority Dispatch",
                    description = "Verify P1 red badge rendering, high-priority sort ranking, and urgent alarm chime triggers",
                    category = "QA",
                    priority = TaskPriority.URGENT,
                    dueTime = "09:00",
                    durationMinutes = 15,
                    recurrenceType = RecurrenceType.DAILY,
                    projectName = "Office",
                    labels = listOf("P1", "SmokeTest", "Urgent", "QA")
                ),
                TemplateTaskDef(
                    title = "[HIGH/P2] Room Schema v10 SQLite Upgrade Verification",
                    description = "Verify task persistence, cross-reference associations, and monotonic Room database migrations",
                    category = "Database",
                    priority = TaskPriority.HIGH,
                    dueTime = "11:00",
                    durationMinutes = 30,
                    recurrenceType = RecurrenceType.DAILY,
                    projectName = "Learning",
                    labels = listOf("Database", "Room", "Migration", "P2")
                ),
                TemplateTaskDef(
                    title = "[MEDIUM/P3] Calendar Recurrence Dots & Midnight Rollover",
                    description = "Verify calendar dot indicators match recurrence engine scheduling deterministically across midnight boundaries",
                    category = "Calendar",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "14:00",
                    durationMinutes = 45,
                    recurrenceType = RecurrenceType.DAILY,
                    projectName = "Daily Habits",
                    labels = listOf("Calendar", "Recurrence", "Dots", "P3")
                ),
                TemplateTaskDef(
                    title = "[LOW/P4] Launcher QR Honeycomb Axial Grid Audit",
                    description = "Audit pointy-top axial hex math (q, r), alternating 3/2 row chunking, and widget PendingIntent dispatch",
                    category = "Widget",
                    priority = TaskPriority.LOW,
                    dueTime = "16:00",
                    durationMinutes = 30,
                    recurrenceType = RecurrenceType.DAILY,
                    projectName = "Personal",
                    labels = listOf("Widget", "Honeycomb", "Launcher", "P4")
                ),
                TemplateTaskDef(
                    title = "[P1/Medication] Smart Health & Medication Schedule Alert",
                    description = "Audit meal-relationship scheduling (Before/With/After) and notification audio alert delivery",
                    category = "Health",
                    priority = TaskPriority.URGENT,
                    dueTime = "12:30",
                    durationMinutes = 15,
                    recurrenceType = RecurrenceType.DAILY,
                    projectName = "Health",
                    labels = listOf("Health", "Medication", "Alarms", "Audio")
                ),
                TemplateTaskDef(
                    title = "[Interval/Weekly] Multi-Day Schedule Engine Stress Test",
                    description = "Verify weekly recurring task evaluation and non-duplicating occurrence projections",
                    category = "Testing",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "10:00",
                    durationMinutes = 60,
                    recurrenceType = RecurrenceType.WEEKLY,
                    recurrenceDaysOfWeek = listOf("MONDAY", "WEDNESDAY", "FRIDAY"),
                    projectName = "Travel",
                    labels = listOf("Schedule", "Weekly", "StressTest")
                ),
                TemplateTaskDef(
                    title = "[Home/Domestic] Domestic Chore & Safety Confirmation Test",
                    description = "Verify domestic project categorization and safety phrase verification countdown modal",
                    category = "Routine",
                    priority = TaskPriority.LOW,
                    dueTime = "18:00",
                    durationMinutes = 20,
                    recurrenceType = RecurrenceType.DAILY,
                    projectName = "Home",
                    labels = listOf("Home", "Safety", "Verification")
                ),
                TemplateTaskDef(
                    title = "Discipline Check",
                    description = "Daily discipline verification check-in",
                    category = "Routines",
                    priority = TaskPriority.HIGH,
                    durationMinutes = 0,
                    recurrenceType = RecurrenceType.DAILY,
                    projectName = "Daily Habits",
                    labels = listOf("Discipline", "Routine")
                )
            ),
            habits = listOf(
                TemplateHabitDef("Hydration QA: 2500ml Daily Target", "💧", "Track daily water intake progress and midnight reset", "08:00", 0xFF0288D1),
                TemplateHabitDef("Physical QA: 10,000 Steps Walking", "🏃", "Verify circular habit completion counters and streak calculation", "19:00", 0xFF4CAF50),
                TemplateHabitDef("Code Quality: Zero-Warning Build", "💻", "Verify Git SHA parity, unit test execution, and release integrity", "21:00", 0xFF9C27B0)
            ),
            shortcuts = listOf(
                TemplateShortcutDef(ShortcutType.TASK, "critical_smoke_test", "Critical Smoke Test", "🧪", 0, 0),
                TemplateShortcutDef(ShortcutType.HABIT, "hydration_qa", "Hydration QA", "💧", 1, 0),
                TemplateShortcutDef(ShortcutType.WATER, "drink_water", "Drink Water", "🌊", 0, 1),
                TemplateShortcutDef(ShortcutType.TASK, "discipline_check", "Discipline Check", "🔥", -1, 1)
            ),
            projects = listOf(
                StandardProjects.OFFICE,
                StandardProjects.LEARNING,
                StandardProjects.DAILY_HABITS,
                StandardProjects.PERSONAL,
                StandardProjects.HEALTH,
                StandardProjects.TRAVEL,
                StandardProjects.HOME
            ),
            tags = listOf("dev", "test", "qa", "acceptance", "verification", "smoke")
        ),

        // 18. Quick Hydration Tracker
        LifeTemplate(
            id = "template_quick_hydration_tracker",
            version = 1,
            title = "Quick Hydration Tracker",
            description = "Focused hydration tracker that sets up a 1-tap water logging shortcut on your App QR and home screen widget.",
            category = TemplateCategory.HEALTH_HYDRATION,
            emoji = "💧",
            tasks = listOf(
                TemplateTaskDef(
                    title = "Daily Water Goal (2500ml)",
                    description = "Stay consistently hydrated throughout the day with 250ml intervals",
                    category = "Health",
                    priority = TaskPriority.MEDIUM,
                    dueTime = "08:00",
                    durationMinutes = 0,
                    recurrenceType = RecurrenceType.DAILY,
                    projectName = "Health",
                    labels = listOf("Hydration", "Water", "Health")
                )
            ),
            habits = listOf(
                TemplateHabitDef(
                    title = "Optimal Daily Hydration",
                    emoji = "💧",
                    description = "Log 2500ml total water intake every single day",
                    reminderTime = "09:00",
                    colorHex = 0xFF0288D1
                )
            ),
            shortcuts = listOf(
                TemplateShortcutDef(
                    type = ShortcutType.WATER,
                    targetId = "water_quick",
                    title = "Drink Water",
                    emoji = "💧",
                    coordQ = 0,
                    coordR = 0,
                    customColorHex = 0xFF0288D1
                )
            ),
            projects = listOf(
                StandardProjects.HEALTH
            ),
            tags = listOf("hydration", "water", "health", "tracker", "qr", "widget")
        )
    )

    val allBundles: List<TemplateBundle> = listOf(
        TemplateBundle(
            id = "bundle_morning_productivity",
            title = "Morning Productivity",
            description = "Kickstart high energy: sunlight, breathwork, hydration, and immediate distraction-free focus.",
            emoji = "🌅",
            templateIds = listOf("template_high_perf_morning", "template_health_hydration"),
            bonusXp = 50
        ),
        TemplateBundle(
            id = "bundle_deep_focus",
            title = "Deep Focus",
            description = "Total concentration system: deep work sprint blocks, zero distractions, and clear daily shutdown.",
            emoji = "🎯",
            templateIds = listOf("template_deep_work_focus", "template_master_daily_flow"),
            bonusXp = 50
        ),
        TemplateBundle(
            id = "bundle_sleep_recovery",
            title = "Sleep & Recovery",
            description = "Complete restoration: evening wind-down, sleep environment tuning, and morning recharge.",
            emoji = "🌙",
            templateIds = listOf("template_deep_sleep_winddown", "template_health_hydration"),
            bonusXp = 50
        ),
        TemplateBundle(
            id = "bundle_healthy_lifestyle",
            title = "Healthy Lifestyle",
            description = "Holistic vitality: consistent movement, optimal hydration, balanced meals, and posture.",
            emoji = "🥗",
            templateIds = listOf("template_fitness_movement", "template_health_hydration"),
            bonusXp = 50
        ),
        TemplateBundle(
            id = "bundle_student_success",
            title = "Student Success",
            description = "Academic excellence: active recall, spaced study blocks, lecture notes, and distraction control.",
            emoji = "🎓",
            templateIds = listOf("template_study_learning", "template_deep_work_focus"),
            bonusXp = 50
        ),
        TemplateBundle(
            id = "bundle_personal_growth",
            title = "Personal Growth",
            description = "Self-mastery framework: daily journaling, atomic habit tracking, and high-impact reading.",
            emoji = "🌱",
            templateIds = listOf("template_personal_growth", "template_atomic_habits"),
            bonusXp = 50
        ),
        TemplateBundle(
            id = "bundle_weekly_reset",
            title = "Weekly Reset",
            description = "Executive life alignment: weekly audit, financial check, clean workspace, and calendar setup.",
            emoji = "📅",
            templateIds = listOf("template_sunday_review", "template_master_daily_flow"),
            bonusXp = 50
        ),

        // --- SET-04 New Checklist Bundles ---
        TemplateBundle(
            id = "bundle_bucket_list",
            title = "Bucket List",
            description = "Lifelong aspirations, fitness milestones, creative legacy, and epic life memories.",
            emoji = "🌟",
            templateIds = listOf("template_bucket_list"),
            bonusXp = 75
        ),
        TemplateBundle(
            id = "bundle_couples_goals",
            title = "Couples Goals",
            description = "Relationship bonding, intentional date nights, joint budget sync, and shared home rituals.",
            emoji = "💑",
            templateIds = listOf("template_couples_goals"),
            bonusXp = 75
        ),
        TemplateBundle(
            id = "bundle_trip_packing",
            title = "Trip Packing Checklist",
            description = "Essential, stress-free travel packing checklist for flights, road trips, and adventures.",
            emoji = "🧳",
            templateIds = listOf("template_trip_packing"),
            bonusXp = 60
        ),
        TemplateBundle(
            id = "bundle_marriage_planning",
            title = "Marriage Planning Checklist",
            description = "End-to-end wedding roadmap: budget, venues, vendors, invitations, and marriage legalities.",
            emoji = "💍",
            templateIds = listOf("template_marriage_planning"),
            bonusXp = 80
        ),
        TemplateBundle(
            id = "bundle_startup_setup",
            title = "Startup Setup Checklist",
            description = "Battle-tested company formation: legal entity, business banking, MVP landing page, and discovery.",
            emoji = "🚀",
            templateIds = listOf("template_startup_setup"),
            bonusXp = 80
        ),
        TemplateBundle(
            id = "bundle_dev_test",
            title = "Developer & QA Test Suite",
            description = "High-velocity test suite for physical device verification: P1-P4 priorities, recurrence, standard projects, habits & widgets.",
            emoji = "🧪",
            templateIds = listOf("template_dev_test"),
            bonusXp = 100
        ),
        TemplateBundle(
            id = "bundle_quick_hydration_tracker",
            title = "Quick Hydration Tracker",
            description = "Single-purpose hydration logging: instant 1-tap water logging tile for App QR and home screen widgets.",
            emoji = "💧",
            templateIds = listOf("template_quick_hydration_tracker"),
            bonusXp = 50
        )
    )

    val allSpecialCollections: List<TemplateBundle> = listOf(
        TemplateBundle(
            id = "bundle_special_life",
            title = "Life Bucket List",
            description = "Peak lifetime milestones, creative legacy, and meaningful life ambitions.",
            emoji = "🌟",
            isSpecialCollection = true,
            templateIds = listOf("special_life_bucket_list"),
            bonusXp = 75
        ),
        TemplateBundle(
            id = "bundle_special_couples",
            title = "Couples Bucket List ❤️",
            description = "Romantic adventures, intimate memories, and relationship bonding traditions.",
            emoji = "❤️",
            isSpecialCollection = true,
            templateIds = listOf("special_couples_bucket_list"),
            bonusXp = 75
        ),
        TemplateBundle(
            id = "bundle_special_travel",
            title = "Travel & Exploration",
            description = "World wanderlust, scenic rail journeys, and UNESCO cultural discoveries.",
            emoji = "✈️",
            isSpecialCollection = true,
            templateIds = listOf("special_travel_bucket_list"),
            bonusXp = 75
        ),
        TemplateBundle(
            id = "bundle_special_adventure",
            title = "Adventure & Thrills",
            description = "Mountain peaks, whitewater rapids, and epic outdoor backcountry challenges.",
            emoji = "🏔️",
            isSpecialCollection = true,
            templateIds = listOf("special_adventure_bucket_list"),
            bonusXp = 75
        )
    )

    fun getTemplateById(id: String): LifeTemplate? {
        return allTemplates.find { it.id == id }
    }

    fun getTemplatesByCategory(category: TemplateCategory): List<LifeTemplate> {
        return allTemplates.filter { it.category == category }
    }
}
