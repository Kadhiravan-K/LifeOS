package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.ProfileSecurityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileSecurityDao {
    @Query("SELECT * FROM profile_security WHERE profileId = :profileId LIMIT 1")
    fun getSecurityForProfileFlow(profileId: Long): Flow<ProfileSecurityEntity?>

    @Query("SELECT * FROM profile_security WHERE profileId = :profileId LIMIT 1")
    suspend fun getSecurityForProfile(profileId: Long): ProfileSecurityEntity?

    @Query("SELECT * FROM profile_security")
    suspend fun getAllSecurityConfigs(): List<ProfileSecurityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSecurity(security: ProfileSecurityEntity)

    @Update
    suspend fun updateSecurity(security: ProfileSecurityEntity)

    @Query("UPDATE profile_security SET failedAttempts = :attempts, lockoutUntilEpochMs = :lockoutUntil WHERE profileId = :profileId")
    suspend fun updateFailedAttempts(profileId: Long, attempts: Int, lockoutUntil: Long)

    @Query("UPDATE profile_security SET failedAttempts = 0, lockoutUntilEpochMs = 0 WHERE profileId = :profileId")
    suspend fun resetLockout(profileId: Long)

    @Query("DELETE FROM profile_security WHERE profileId = :profileId")
    suspend fun deleteSecurityForProfile(profileId: Long): Int
}
