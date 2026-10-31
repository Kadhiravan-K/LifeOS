package com.example.domain.emoji

import java.util.Locale

/**
 * Requirement #5: Offline Task-Title Emoji Suggestion Engine.
 * Recommends relevant emojis based on task titles using an offline dictionary
 * of stemmed keywords and compound phrases. Runs locally with zero network access.
 */
object TaskEmojiSuggestionEngine {

    private data class KeywordRule(
        val keywords: List<String>,
        val emojis: List<String>,
        val weight: Int = 10
    )

    private val rules: List<KeywordRule> = listOf(
        // Shopping & Groceries
        KeywordRule(
            listOf("groceries", "grocery", "supermarket", "cart", "pantry", "produce"),
            listOf("🛒", "🍎", "🥦", "🛍️"),
            weight = 20
        ),
        KeywordRule(
            listOf("buy", "purchase", "shop", "shopping", "order", "store", "mall"),
            listOf("🛍️", "🛒", "🏷️", "💳"),
            weight = 15
        ),

        // Communication
        KeywordRule(
            listOf("call", "phone", "dial", "ring", "reach out", "zoom", "teleconference"),
            listOf("📞", "📱", "📲"),
            weight = 20
        ),
        KeywordRule(
            listOf("email", "mail", "inbox", "newsletter", "reply", "forward"),
            listOf("📧", "✉️", "📨"),
            weight = 20
        ),
        KeywordRule(
            listOf("message", "text", "chat", "slack", "discord", "dm", "whatsapp"),
            listOf("💬", "📱", "🗨️"),
            weight = 15
        ),

        // Fitness & Physical Activity
        KeywordRule(
            listOf("run", "running", "jog", "jogging", "sprint", "cardio", "treadmill"),
            listOf("🏃", "👟", "⏱️"),
            weight = 20
        ),
        KeywordRule(
            listOf("gym", "workout", "weights", "lift", "lifting", "bench", "deadlift", "squat", "fitness"),
            listOf("🏋️", "💪", "🏃"),
            weight = 20
        ),
        KeywordRule(
            listOf("yoga", "stretch", "stretching", "meditate", "meditation", "mindfulness", "zen"),
            listOf("🧘", "✨", "🕯️"),
            weight = 20
        ),
        KeywordRule(
            listOf("cycle", "cycling", "bike", "biking", "ride"),
            listOf("🚴", "🚲"),
            weight = 20
        ),
        KeywordRule(
            listOf("swim", "swimming", "pool", "laps"),
            listOf("🏊", "🌊", "🥽"),
            weight = 20
        ),
        KeywordRule(
            listOf("walk", "walking", "stroll", "steps", "hike", "hiking", "trail"),
            listOf("🚶", "🥾", "🌲"),
            weight = 15
        ),

        // Learning & Reading
        KeywordRule(
            listOf("read", "reading", "book", "chapter", "novel", "literature", "ebook"),
            listOf("📚", "📖", "🔖"),
            weight = 20
        ),
        KeywordRule(
            listOf("study", "exam", "test", "quiz", "revision", "homework", "assignment", "lecture"),
            listOf("📝", "🎓", "📚", "🧠"),
            weight = 20
        ),
        KeywordRule(
            listOf("learn", "course", "tutorial", "practice", "lesson"),
            listOf("🎓", "💡", "📖"),
            weight = 15
        ),

        // Work & Development
        KeywordRule(
            listOf("code", "coding", "program", "programming", "software", "developer", "debug", "bug", "git", "github", "pr", "commit"),
            listOf("💻", "👨‍💻", "⚙️", "🚀"),
            weight = 25
        ),
        KeywordRule(
            listOf("meeting", "sync", "standup", "presentation", "interview", "demo", "client", "pitch", "discuss"),
            listOf("👥", "💼", "📊", "🗓️"),
            weight = 20
        ),
        KeywordRule(
            listOf("design", "ui", "ux", "figma", "sketch", "wireframe", "prototype", "art", "drawing"),
            listOf("🎨", "🖌️", "✨", "📐"),
            weight = 20
        ),
        KeywordRule(
            listOf("write", "writing", "blog", "draft", "article", "doc", "notes", "journal"),
            listOf("✍️", "📝", "📄"),
            weight = 15
        ),

        // Health & Medication
        KeywordRule(
            listOf("doctor", "dentist", "physician", "checkup", "clinic", "hospital", "appointment"),
            listOf("🩺", "🏥", "🦷"),
            weight = 25
        ),
        KeywordRule(
            listOf("medicine", "medication", "pill", "tablet", "capsule", "dose", "prescription", "vitamin", "supplement", "antibiotic"),
            listOf("💊", "🧪", "🩹"),
            weight = 25
        ),
        KeywordRule(
            listOf("water", "drink", "hydrate", "hydration", "fluid"),
            listOf("💧", "🥤", "🚰"),
            weight = 20
        ),
        KeywordRule(
            listOf("sleep", "nap", "bed", "bedtime", "rest"),
            listOf("😴", "🛏️", "🌙"),
            weight = 20
        ),

        // Chores & Home
        KeywordRule(
            listOf("clean", "cleaning", "wash", "washing", "laundry", "vacuum", "dishes", "sweep", "mop", "tidy", "trash", "garbage"),
            listOf("🧹", "🧼", "🧺", "✨"),
            weight = 20
        ),
        KeywordRule(
            listOf("cook", "cooking", "bake", "baking", "dinner", "lunch", "breakfast", "meal", "recipe", "food", "kitchen"),
            listOf("🍳", "🍲", "🥗", "🥘"),
            weight = 20
        ),
        KeywordRule(
            listOf("repair", "fix", "tool", "tools", "plumber", "electrician", "maintenance"),
            listOf("🛠️", "🔧", "🪛"),
            weight = 20
        ),
        KeywordRule(
            listOf("plant", "plants", "garden", "gardening", "water plants", "lawn"),
            listOf("🌱", "🪴", "🌻", "🌿"),
            weight = 20
        ),

        // Finance & Bills
        KeywordRule(
            listOf("pay", "payment", "bill", "bills", "rent", "invoice", "salary", "tax", "taxes", "bank", "credit card", "finance", "transfer"),
            listOf("💳", "💰", "💵", "🧾"),
            weight = 25
        ),

        // Travel & Vehicles
        KeywordRule(
            listOf("flight", "fly", "airport", "plane", "travel", "trip", "vacation", "pack", "packing", "hotel"),
            listOf("✈️", "🧳", "🏨", "🗺️"),
            weight = 25
        ),
        KeywordRule(
            listOf("car", "drive", "gas", "fuel", "mechanic", "oil change", "tires", "parking", "commute"),
            listOf("🚗", "⛽", "🔧", "🚙"),
            weight = 20
        ),
        KeywordRule(
            listOf("train", "metro", "subway", "transit", "bus"),
            listOf("🚆", "🚇", "🚌"),
            weight = 20
        ),

        // Social, Pets & Celebrations
        KeywordRule(
            listOf("birthday", "party", "gift", "present", "celebrate", "celebration", "anniversary"),
            listOf("🎂", "🎁", "🎉", "🎈"),
            weight = 25
        ),
        KeywordRule(
            listOf("dog", "puppy", "bark", "walk dog", "vet", "pet"),
            listOf("🐶", "🐕", "🐾"),
            weight = 20
        ),
        KeywordRule(
            listOf("cat", "kitten", "kitty"),
            listOf("🐱", "🐈", "🐾"),
            weight = 20
        ),
        KeywordRule(
            listOf("coffee", "cafe", "latte", "espresso", "tea", "matcha"),
            listOf("☕", "🍵", "🥐"),
            weight = 20
        ),
        KeywordRule(
            listOf("movie", "cinema", "film", "watch", "show", "series", "netflix"),
            listOf("🎬", "🍿", "📺"),
            weight = 20
        ),
        KeywordRule(
            listOf("music", "guitar", "piano", "sing", "song", "concert", "album", "rehearse"),
            listOf("🎵", "🎸", "🎹", "🎤"),
            weight = 20
        ),

        // Goals & Prioritization
        KeywordRule(
            listOf("goal", "target", "focus", "priority", "important", "urgent"),
            listOf("🎯", "⭐", "🔥"),
            weight = 10
        ),
        KeywordRule(
            listOf("idea", "brainstorm", "inspire", "inspiration", "think"),
            listOf("💡", "🤔", "✨"),
            weight = 10
        )
    )

    /**
     * Analyzes [title] and returns an ordered list of distinct emoji suggestions.
     * Highest matching score appears first. Returns empty list if no matches.
     */
    fun suggestEmojis(title: String, limit: Int = 5): List<String> {
        val trimmed = title.trim().lowercase(Locale.ROOT)
        if (trimmed.isEmpty()) return emptyList()

        // Tokenize and clean punctuation
        val tokens = trimmed
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        if (tokens.isEmpty()) return emptyList()

        // Map to track emoji scores: Emoji -> Total weighted score
        val scores = mutableMapOf<String, Int>()

        // 1. Check whole title and token-level matches
        for (rule in rules) {
            var ruleMatched = false
            var matchWeight = 0

            for (kw in rule.keywords) {
                if (kw.contains(" ")) {
                    // Multi-word phrase matching
                    if (trimmed.contains(kw)) {
                        ruleMatched = true
                        matchWeight = maxOf(matchWeight, rule.weight + 15) // bonus for exact phrase
                    }
                } else {
                    // Single word token matching
                    for (token in tokens) {
                        if (token == kw) {
                            ruleMatched = true
                            matchWeight = maxOf(matchWeight, rule.weight + 10)
                        } else if (token.length >= 4 && (token.startsWith(kw) || kw.startsWith(token))) {
                            ruleMatched = true
                            matchWeight = maxOf(matchWeight, rule.weight)
                        }
                    }
                }
            }

            if (ruleMatched) {
                // Assign score to emojis in this rule
                rule.emojis.forEachIndexed { idx, emoji ->
                    // Earlier emojis in a rule get slightly higher priority
                    val priority = matchWeight - (idx * 2)
                    scores[emoji] = (scores[emoji] ?: 0) + priority
                }
            }
        }

        return scores.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(limit)
    }
}
