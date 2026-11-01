package com.example.domain.glossary

import com.example.data.local.dao.GlossaryDao
import com.example.data.local.entities.GlossaryItemEntity
import com.example.data.local.entities.GlossaryPackEntity
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Manages downloading, importing, verifying, and removing data-driven knowledge packs (#2).
 * Strictly operates on validated JSON structures — zero dynamic code execution is possible.
 */
object GlossaryModuleManager {

    /**
     * Computes the SHA-256 checksum of the raw JSON content string.
     */
    fun computeSha256(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Parses and safely validates a JSON string representing a GlossaryPack.
     * Returns a valid GlossaryPackJson or throws IllegalArgumentException.
     */
    fun parsePackJson(rawJson: String): GlossaryPackJson {
        try {
            val root = JSONObject(rawJson)
            val id = root.getString("id").trim()
            val title = root.getString("title").trim()
            val description = root.optString("description", "").trim()
            val category = root.optString("category", "General").trim()
            val version = root.optInt("version", 1)
            val author = root.optString("author", "LifeOS Community").trim()

            val termsArray = root.optJSONArray("terms") ?: JSONArray()
            val termList = mutableListOf<GlossaryTermJson>()

            for (i in 0 until termsArray.length()) {
                val tObj = termsArray.getJSONObject(i)
                val term = tObj.getString("term").trim()
                val phonetic = if (tObj.has("phonetic") && !tObj.isNull("phonetic")) tObj.getString("phonetic") else null
                val cat = tObj.optString("category", category).trim()
                val definition = tObj.getString("definition").trim()
                val detailedExplanation = tObj.optString("detailedExplanation", "").trim()

                val examplesList = mutableListOf<String>()
                tObj.optJSONArray("examples")?.let { arr ->
                    for (j in 0 until arr.length()) examplesList.add(arr.getString(j))
                }

                val relatedList = mutableListOf<String>()
                tObj.optJSONArray("relatedTerms")?.let { arr ->
                    for (j in 0 until arr.length()) relatedList.add(arr.getString(j))
                }

                val tagsList = mutableListOf<String>()
                tObj.optJSONArray("tags")?.let { arr ->
                    for (j in 0 until arr.length()) tagsList.add(arr.getString(j))
                }

                termList.add(
                    GlossaryTermJson(
                        term = term,
                        phonetic = phonetic,
                        category = cat,
                        definition = definition,
                        detailedExplanation = detailedExplanation,
                        examples = examplesList,
                        relatedTerms = relatedList,
                        tags = tagsList
                    )
                )
            }

            return GlossaryPackJson(
                id = id,
                title = title,
                description = description,
                category = category,
                version = version,
                author = author,
                terms = termList
            )
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid Glossary JSON Pack format: ${e.message}", e)
        }
    }

    /**
     * Installs or updates a glossary pack into Room database via DAO.
     */
    suspend fun installPack(
        dao: GlossaryDao,
        packJson: GlossaryPackJson,
        expectedSha256: String? = null,
        rawJsonContent: String? = null
    ): Result<GlossaryPack> {
        return try {
            if (!expectedSha256.isNullOrBlank() && !rawJsonContent.isNullOrBlank()) {
                val actualHash = computeSha256(rawJsonContent)
                if (!actualHash.equals(expectedSha256.trim(), ignoreCase = true)) {
                    return Result.failure(SecurityException("SHA-256 integrity verification failed. Expected $expectedSha256 but got $actualHash"))
                }
            }

            val packEntity = GlossaryPackEntity(
                id = packJson.id,
                title = packJson.title,
                description = packJson.description,
                category = packJson.category,
                version = packJson.version,
                author = packJson.author,
                itemCount = packJson.terms.size,
                sha256 = if (!rawJsonContent.isNullOrBlank()) computeSha256(rawJsonContent) else "",
                isInstalled = true,
                installedAt = System.currentTimeMillis(),
                sizeBytes = (rawJsonContent?.length ?: 0).toLong()
            )

            dao.insertPack(packEntity)

            val itemEntities = packJson.terms.map { t ->
                GlossaryItemEntity(
                    packId = packJson.id,
                    term = t.term,
                    phonetic = t.phonetic,
                    category = t.category,
                    definition = t.definition,
                    detailedExplanation = t.detailedExplanation,
                    examplesJson = JSONArray(t.examples).toString(),
                    relatedTermsJson = JSONArray(t.relatedTerms).toString(),
                    tagsJson = JSONArray(t.tags).toString(),
                    isBookmarked = false
                )
            }

            dao.deleteItemsForPack(packJson.id)
            dao.insertItems(itemEntities)

            Result.success(packEntity.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uninstalls a pack and cascades deletion of its items.
     */
    suspend fun uninstallPack(dao: GlossaryDao, packId: String) {
        dao.deletePack(packId)
    }

    /**
     * Built-in curated starter pack bundled offline with the app.
     */
    val BUNDLED_CORE_PACK: GlossaryPackJson = GlossaryPackJson(
        id = "lifeos_core",
        title = "LifeOS Productivity & Systems Thinking",
        description = "Essential mental models, focus principles, and time management paradigms.",
        category = "Productivity",
        version = 1,
        author = "LifeOS Core Architecture",
        terms = listOf(
            GlossaryTermJson(
                term = "Deep Work",
                phonetic = "/diːp wɜːk/",
                category = "Focus",
                definition = "Professional activities performed in a state of distraction-free concentration that push your cognitive capabilities to their limit.",
                detailedExplanation = "Coined by Cal Newport. Deep work optimizes myelination around brain circuits, allowing elite performance and skill acquisition.",
                examples = listOf("Coding uninterrupted for 90 minutes with flight mode enabled.", "Writing documentation with zero notifications."),
                relatedTerms = listOf("Flow State", "Time Blocking", "Attention Residue"),
                tags = listOf("focus", "deep-work", "productivity")
            ),
            GlossaryTermJson(
                term = "Time Blocking",
                phonetic = "/taɪm ˈblɒk.ɪŋ/",
                category = "Planning",
                definition = "A time management method that divides your day into dedicated blocks of time, each assigned to a specific task or group of tasks.",
                detailedExplanation = "Instead of keeping an open-ended to-do list, time blocking reserves finite chronological real estate for priorities.",
                examples = listOf("Allocating 09:00 - 11:00 exclusively for software development.", "Reserving 17:00 - 17:30 for daily wrap-up and planning."),
                relatedTerms = listOf("Deep Work", "Pomodoro Technique", "Task Batching"),
                tags = listOf("planning", "calendar", "time")
            ),
            GlossaryTermJson(
                term = "Pareto Principle (80/20 Rule)",
                phonetic = "/pəˈreɪ.toʊ/",
                category = "Systems",
                definition = "The principle stating that for many outcomes, roughly 80% of consequences come from 20% of causes.",
                detailedExplanation = "Focusing effort on the vital 20% of core tasks delivers 80% of overall life and professional results.",
                examples = listOf("20% of code features generate 80% of daily user engagement.", "20% of high-leverage clients generate 80% of business revenue."),
                relatedTerms = listOf("Leverage", "Eisenhower Matrix"),
                tags = listOf("systems", "efficiency", "leverage")
            ),
            GlossaryTermJson(
                term = "Parkinson's Law",
                phonetic = "/ˈpɑː.kɪn.sənz lɔː/",
                category = "Productivity",
                definition = "Work expands so as to fill the period of time available for its completion.",
                detailedExplanation = "Imposing tighter, realistic deadlines prevents artificial bloat, unnecessary perfectionism, and procrastination.",
                examples = listOf("Setting a 2-hour deadline to complete a project proposal instead of allowing 3 days."),
                relatedTerms = listOf("Time Blocking", "Pomodoro Technique"),
                tags = listOf("time", "urgency", "focus")
            ),
            GlossaryTermJson(
                term = "Eisenhower Matrix",
                phonetic = "/ˈaɪ.zən.haʊ.ər/",
                category = "Prioritization",
                definition = "A decision framework that categorizes tasks into four quadrants based on urgency and importance.",
                detailedExplanation = "Quadrant 1: Urgent & Important (Do). Quadrant 2: Not Urgent but Important (Schedule). Quadrant 3: Urgent, Not Important (Delegate). Quadrant 4: Not Urgent, Not Important (Eliminate).",
                examples = listOf("Q2 Focus: Daily exercise, continuous learning, and architecture planning."),
                relatedTerms = listOf("Pareto Principle", "Deep Work"),
                tags = listOf("priority", "quadrants", "decisions")
            ),
            GlossaryTermJson(
                term = "Habit Stacking",
                phonetic = "/ˈhæb.ɪt ˈstæk.ɪŋ/",
                category = "Habits",
                definition = "A strategy where you identify a current habit you already do each day and stack your new behavior onto it.",
                detailedExplanation = "Uses the formula: After/Before [CURRENT HABIT], I will [NEW HABIT]. Leverages existing synaptic pathways.",
                examples = listOf("After I pour my morning coffee, I will drink a 500ml glass of water.", "After I close my laptop for the day, I will write tomorrow's top 3 tasks."),
                relatedTerms = listOf("Implementation Intentions", "Cue-Routine-Reward"),
                tags = listOf("habits", "routine", "behavior")
            ),
            GlossaryTermJson(
                term = "Two-Minute Rule",
                phonetic = "/tuː ˈmɪn.ɪt ruːl/",
                category = "Execution",
                definition = "If an action takes less than two minutes to complete, execute it immediately rather than deferring it.",
                detailedExplanation = "Part of Getting Things Done (GTD). Eliminates cognitive overhead and prevents micro-tasks from cluttering your task queue.",
                examples = listOf("Replying to a one-line confirmation email.", "Putting away laundry right after folding."),
                relatedTerms = listOf("GTD", "Cognitive Load"),
                tags = listOf("action", "speed", "gtd")
            ),
            GlossaryTermJson(
                term = "Flow State",
                phonetic = "/floʊ steɪt/",
                category = "Neuroscience",
                definition = "The optimal state of consciousness where you feel your best and perform your best, fully immersed in energized focus.",
                detailedExplanation = "Identified by Mihaly Csikszentmihalyi. Characterized by distortion of time, transient hypofrontality, and deep engagement.",
                examples = listOf("Writing clean, frictionless code where hours feel like minutes."),
                relatedTerms = listOf("Deep Work", "Dopamine Loop"),
                tags = listOf("flow", "optimal", "mind")
            )
        )
    )

    /**
     * Curated catalog of verified downloadable packs available for optional installation.
     */
    val CATALOG_MODULES: List<GlossaryPackJson> = listOf(
        BUNDLED_CORE_PACK,
        GlossaryPackJson(
            id = "mental_models",
            title = "Cognitive Biases & Mental Models",
            description = "10 foundational decision-making and thinking models for engineers and leaders.",
            category = "Thinking",
            version = 1,
            author = "Shane Parrish / Farnam Street",
            terms = listOf(
                GlossaryTermJson(
                    term = "First Principles Thinking",
                    category = "Thinking",
                    definition = "Act of boiling a problem down to its most fundamental truths and reasoning up from there rather than by analogy.",
                    detailedExplanation = "Break complex assumptions apart to discover fundamental truths that cannot be deduced any further.",
                    examples = listOf("Designing battery chemistry from first-principles raw material costs."),
                    tags = listOf("reasoning", "physics", "first-principles")
                ),
                GlossaryTermJson(
                    term = "Inversion",
                    category = "Thinking",
                    definition = "Approaching situations from the opposite point of view: Invert, always invert.",
                    detailedExplanation = "Instead of asking how to succeed, ask what would guarantee failure, then actively avoid those failure modes.",
                    examples = listOf("Asking 'How could our app deployment fail disastrously?' and building guardrails against those exact vectors."),
                    tags = listOf("inversion", "logic", "strategy")
                ),
                GlossaryTermJson(
                    term = "Occam's Razor",
                    category = "Thinking",
                    definition = "Among competing hypotheses, the one with the fewest assumptions should be selected.",
                    detailedExplanation = "Simpler explanations are more likely to be correct than convoluted theories requiring multiple coincidences.",
                    examples = listOf("If a test fails after one commit, inspect that commit before suspecting compiler bugs."),
                    tags = listOf("logic", "simplicity")
                ),
                GlossaryTermJson(
                    term = "Chesterton's Fence",
                    category = "Systems",
                    definition = "Reforms should not be made until the reasoning behind the existing state of affairs is understood.",
                    detailedExplanation = "Do not tear down a fence or delete code until you understand why it was put there in the first place.",
                    examples = listOf("Understanding why legacy code uses a synchronized block before replacing it with coroutines."),
                    tags = listOf("caution", "refactoring", "systems")
                )
            )
        ),
        GlossaryPackJson(
            id = "habit_neuroscience",
            title = "Habit Neuroscience & Behavioral Design",
            description = "Neurobiology of habit loops, basal ganglia automation, and craving mechanics.",
            category = "Neuroscience",
            version = 1,
            author = "James Clear / BJ Fogg",
            terms = listOf(
                GlossaryTermJson(
                    term = "Basal Ganglia Automation",
                    category = "Neuroscience",
                    definition = "The neurological process where repetitive behaviors shift from prefrontal cortex control to the basal ganglia.",
                    detailedExplanation = "Frees up conscious working memory by turning physical routines into subconscious procedural programs.",
                    examples = listOf("Tying shoes, driving a routine route, or starting a timer without conscious deliberation."),
                    tags = listOf("brain", "habits", "neuroscience")
                ),
                GlossaryTermJson(
                    term = "Friction Reduction",
                    category = "Behavioral Design",
                    definition = "Decreasing the number of physical or cognitive steps required to perform a positive habit.",
                    detailedExplanation = "Human behavior follows the path of least resistance. Reducing friction dramatically increases follow-through.",
                    examples = listOf("Keeping water bottle on the desk, pre-packing gym clothes the night before."),
                    tags = listOf("friction", "design", "habits")
                )
            )
        )
    )

    fun GlossaryPackEntity.toDomain(): GlossaryPack = GlossaryPack(
        id = id,
        title = title,
        description = description,
        category = category,
        version = version,
        author = author,
        itemCount = itemCount,
        sha256 = sha256,
        isInstalled = isInstalled,
        installedAt = installedAt,
        sizeBytes = sizeBytes
    )

    fun GlossaryItemEntity.toDomain(
        isBookmarkedProfile: Boolean? = null,
        personalNoteProfile: String? = null,
        lastViewedAtProfile: Long? = null
    ): GlossaryTerm {
        val examples = try {
            val arr = JSONArray(examplesJson)
            List(arr.length()) { arr.getString(it) }
        } catch (_: Exception) { emptyList() }

        val related = try {
            val arr = JSONArray(relatedTermsJson)
            List(arr.length()) { arr.getString(it) }
        } catch (_: Exception) { emptyList() }

        val tags = try {
            val arr = JSONArray(tagsJson)
            List(arr.length()) { arr.getString(it) }
        } catch (_: Exception) { emptyList() }

        return GlossaryTerm(
            id = id,
            packId = packId,
            term = term,
            phonetic = phonetic,
            category = category,
            definition = definition,
            detailedExplanation = detailedExplanation,
            examples = examples,
            relatedTerms = related,
            tags = tags,
            isBookmarked = isBookmarkedProfile ?: isBookmarked,
            personalNote = personalNoteProfile ?: "",
            lastViewedAt = lastViewedAtProfile ?: 0L,
            createdAt = createdAt
        )
    }
}
