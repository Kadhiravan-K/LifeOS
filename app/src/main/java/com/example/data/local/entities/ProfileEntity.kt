package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ProfileEntity represents an isolated user workspace (Personal, Work, Custom).
 * Requirement #9 & #19 foundation.
 */
@Entity(
    tableName = "profiles",
    indices = [
        Index("type"),
        Index("orderIndex")
    ]
)
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emoji: String = "👤",
    val colorHex: Long = 0xFF3B82F6,
    val type: String = "PERSONAL", // "PERSONAL", "WORK", "CUSTOM"
    val orderIndex: Int = 0,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_PERSONAL_PROFILE_ID = 1L
        const val TYPE_PERSONAL = "PERSONAL"
        const val TYPE_WORK = "WORK"
        const val TYPE_CUSTOM = "CUSTOM"

        fun createDefaultPersonal(): ProfileEntity {
            return ProfileEntity(
                id = DEFAULT_PERSONAL_PROFILE_ID,
                name = "Personal",
                emoji = "👤",
                colorHex = 0xFF3B82F6,
                type = TYPE_PERSONAL,
                orderIndex = 0,
                isDefault = true
            )
        }
    }
}
