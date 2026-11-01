package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Metadata for a downloadable or bundled offline knowledge pack (#2).
 */
@Entity(tableName = "glossary_packs")
data class GlossaryPackEntity(
    @PrimaryKey
    val id: String, // e.g. "lifeos_core", "mental_models", "neuroscience_habits"
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
 * Individual glossary term record within a knowledge pack.
 * Enforces CASCADE deletion if its parent pack is uninstalled.
 */
@Entity(
    tableName = "glossary_items",
    foreignKeys = [
        ForeignKey(
            entity = GlossaryPackEntity::class,
            parentColumns = ["id"],
            childColumns = ["packId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("packId"),
        Index("term"),
        Index("category")
    ]
)
data class GlossaryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packId: String,
    val term: String,
    val phonetic: String? = null,
    val category: String,
    val definition: String,
    val detailedExplanation: String = "",
    val examplesJson: String = "[]",
    val relatedTermsJson: String = "[]",
    val tagsJson: String = "[]",
    val isBookmarked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Profile-isolated user state for a glossary term (#2, #8).
 * Ensures bookmarks, personal notes, and view timestamps are strictly scoped
 * to individual profiles while shared reference definitions remain global.
 */
@Entity(
    tableName = "glossary_user_data",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GlossaryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["termId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("profileId"),
        Index("termId"),
        Index(value = ["profileId", "termId"], unique = true)
    ]
)
data class GlossaryUserDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 1,
    val termId: Long,
    val isBookmarked: Boolean = false,
    val personalNote: String = "",
    val lastViewedAt: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)

