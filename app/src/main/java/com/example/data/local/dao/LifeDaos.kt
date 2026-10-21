package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.ChallengeEntity
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.HoneycombShortcutEntity
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

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    suspend fun getAllTasksList(): List<TaskEntity>

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY orderIndex ASC, createdAt ASC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY orderIndex ASC, createdAt ASC")
    suspend fun getAllHabitsList(): List<HabitEntity>

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

    @Query("SELECT * FROM water_logs ORDER BY dateIso DESC")
    suspend fun getAllWaterLogsList(): List<WaterLogEntity>
}

@Dao
interface HoneycombShortcutDao {
    @Query("SELECT * FROM honeycomb_shortcuts ORDER BY orderIndex ASC, id ASC")
    fun getAllShortcuts(): Flow<List<HoneycombShortcutEntity>>

    @Query("SELECT * FROM honeycomb_shortcuts ORDER BY orderIndex ASC, id ASC")
    suspend fun getAllShortcutsList(): List<HoneycombShortcutEntity>

    @Query("SELECT * FROM honeycomb_shortcuts WHERE id = :id LIMIT 1")
    suspend fun getShortcutById(id: Long): HoneycombShortcutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(shortcut: HoneycombShortcutEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcuts(shortcuts: List<HoneycombShortcutEntity>)

    @Update
    suspend fun updateShortcut(shortcut: HoneycombShortcutEntity)

    @Query("UPDATE honeycomb_shortcuts SET coordQ = :q, coordR = :r, orderIndex = :orderIndex WHERE id = :id")
    suspend fun updateCoords(id: Long, q: Int, r: Int, orderIndex: Int)

    @Query("DELETE FROM honeycomb_shortcuts WHERE id = :id")
    suspend fun deleteShortcutById(id: Long)

    @Query("DELETE FROM honeycomb_shortcuts WHERE type = :type AND targetId = :targetId")
    suspend fun deleteShortcutByTarget(type: String, targetId: String)

    @Query("DELETE FROM honeycomb_shortcuts")
    suspend fun clearAllShortcuts()
}
