package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.ProfileSurveyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileSurveyDao {
    @Query("SELECT * FROM profile_surveys WHERE profileId = :profileId LIMIT 1")
    fun getSurveyForProfileFlow(profileId: Long): Flow<ProfileSurveyEntity?>

    @Query("SELECT * FROM profile_surveys WHERE profileId = :profileId LIMIT 1")
    suspend fun getSurveyForProfile(profileId: Long): ProfileSurveyEntity?

    @Query("SELECT * FROM profile_surveys")
    suspend fun getAllSurveys(): List<ProfileSurveyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSurvey(survey: ProfileSurveyEntity): Long

    @Update
    suspend fun updateSurvey(survey: ProfileSurveyEntity)

    @Query("DELETE FROM profile_surveys WHERE profileId = :profileId")
    suspend fun deleteSurveyForProfile(profileId: Long): Int
}
