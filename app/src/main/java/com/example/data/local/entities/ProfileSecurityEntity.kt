package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ProfileSecurityEntity manages per-profile security, application lock,
 * credential protection, and biometric authentication configuration.
 */
@Entity(
    tableName = "profile_security",
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
data class ProfileSecurityEntity(
    @PrimaryKey
    val profileId: Long,
    val isLockEnabled: Boolean = false,
    val authMethod: String = AUTH_METHOD_NONE, // NONE, PIN, PASSWORD, PATTERN
    val credentialHash: String = "",
    val salt: String = "",
    val biometricEnabled: Boolean = false,
    val autoLockTimeoutSeconds: Int = DEFAULT_AUTO_LOCK_SECONDS,
    val failedAttempts: Int = 0,
    val lockoutUntilEpochMs: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val AUTH_METHOD_NONE = "NONE"
        const val AUTH_METHOD_PIN = "PIN"
        const val AUTH_METHOD_PASSWORD = "PASSWORD"
        const val AUTH_METHOD_PATTERN = "PATTERN"

        const val DEFAULT_AUTO_LOCK_SECONDS = 300 // 5 minutes default
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 30_000L // 30 seconds

        fun defaultForProfile(profileId: Long): ProfileSecurityEntity {
            return ProfileSecurityEntity(
                profileId = profileId,
                isLockEnabled = false,
                authMethod = AUTH_METHOD_NONE,
                credentialHash = "",
                salt = "",
                biometricEnabled = false,
                autoLockTimeoutSeconds = DEFAULT_AUTO_LOCK_SECONDS,
                failedAttempts = 0,
                lockoutUntilEpochMs = 0L,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
