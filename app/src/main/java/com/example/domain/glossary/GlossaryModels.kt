package com.example.domain.glossary

/**
 * Domain model representing a verified offline glossary term.
 */
data class GlossaryTerm(
    val id: Long = 0,
    val packId: String,
    val term: String,
    val phonetic: String? = null,
    val category: String,
    val definition: String,
    val detailedExplanation: String = "",
    val examples: List<String> = emptyList(),
    val relatedTerms: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val isBookmarked: Boolean = false,
    val personalNote: String = "",
    val lastViewedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Domain model representing a knowledge pack module.
 */
data class GlossaryPack(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val version: Int = 1,
    val author: String = "LifeOS Community",
    val itemCount: Int = 0,
    val sha256: String = "",
    val isInstalled: Boolean = true,
    val installedAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0L
)

/**
 * Data-transfer structure for importing/exporting verified JSON content packs.
 */
data class GlossaryPackJson(
    val id: String,
    val title: String,
    val description: String = "",
    val category: String,
    val version: Int = 1,
    val author: String = "LifeOS Community",
    val terms: List<GlossaryTermJson> = emptyList()
)

data class GlossaryTermJson(
    val term: String,
    val phonetic: String? = null,
    val category: String,
    val definition: String,
    val detailedExplanation: String = "",
    val examples: List<String> = emptyList(),
    val relatedTerms: List<String> = emptyList(),
    val tags: List<String> = emptyList()
)
