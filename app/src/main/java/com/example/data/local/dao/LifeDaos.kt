package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.ChallengeEntity
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.WaterLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY orderIndex ASC, createdAt ASC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: Long): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(habits: List<HabitEntity>)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Long)

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()
}

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenges WHERE id = 1 LIMIT 1")
    fun getActiveChallengeFlow(): Flow<ChallengeEntity?>

    @Query("SELECT * FROM challenges WHERE id = 1 LIMIT 1")
    suspend fun getActiveChallenge(): ChallengeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateChallenge(challenge: ChallengeEntity)

    @Query("DELETE FROM challenges")
    suspend fun deleteChallenge()
}

@Dao
interface WaterLogDao {
    @Query("SELECT * FROM water_logs WHERE dateIso = :dateIso LIMIT 1")
    fun getWaterLogFlow(dateIso: String): Flow<WaterLogEntity?>

    @Query("SELECT * FROM water_logs WHERE dateIso = :dateIso LIMIT 1")
    suspend fun getWaterLog(dateIso: String): WaterLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWaterLog(waterLog: WaterLogEntity)

    @Query("SELECT * FROM water_logs ORDER BY dateIso DESC LIMIT 30")
    fun getRecentWaterLogs(): Flow<List<WaterLogEntity>>
}
