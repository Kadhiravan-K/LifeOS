package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ProfileSurveyEntity stores optional, skippable onboarding and profile survey responses.
 * Requirement #19: All answers are strictly local, profile-scoped, and private.
 */
@Entity(
    tableName = "profile_surveys",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("profileId")
    ]
)
data class ProfileSurveyEntity(
    @PrimaryKey
    val profileId: Long,
    val displayName: String? = null,
    val gender: String? = null,
    val sleepingTime: String? = null,
    val dailyWaterGoalMl: Int? = null,
    val discoverySource: String? = null,
    val discoverySocialPlatform: String? = null,
    val discoveryOtherDetail: String? = null,
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false,
    val completedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val MAX_NAME_LENGTH = 50
        const val MAX_DETAIL_LENGTH = 100
        const val MIN_WATER_GOAL_ML = 500
        const val MAX_WATER_GOAL_ML = 6000

        val GENDER_OPTIONS = listOf(
            "Prefer not to say",
            "Female",
            "Male",
            "Non-binary",
            "Self-describe"
        )

        val DISCOVERY_SOURCES = listOf(
            "Social Media",
            "GitHub",
            "Google Search",
            "Friends / Word of mouth",
            "Other"
        )

        val SOCIAL_PLATFORMS = listOf(
            "Facebook",
            "Instagram",
            "Reddit",
            "Slack",
            "YouTube",
            "Other"
        )
    }
}
