package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.TaskReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskReminderDao {
    @Query("SELECT * FROM task_reminders WHERE taskId = :taskId ORDER BY minutesBefore ASC")
    fun getRemindersForTaskFlow(taskId: Long): Flow<List<TaskReminderEntity>>

    @Query("SELECT * FROM task_reminders WHERE taskId = :taskId ORDER BY minutesBefore ASC")
    suspend fun getRemindersForTaskList(taskId: Long): List<TaskReminderEntity>

    @Query("SELECT * FROM task_reminders WHERE isEnabled = 1")
    suspend fun getAllActiveReminders(): List<TaskReminderEntity>

    @Query("SELECT * FROM task_reminders WHERE isEnabled = 1")
    fun getAllActiveRemindersFlow(): Flow<List<TaskReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: TaskReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<TaskReminderEntity>)

    @Update
    suspend fun updateReminder(reminder: TaskReminderEntity)

    @Query("DELETE FROM task_reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("DELETE FROM task_reminders WHERE taskId = :taskId")
    suspend fun deleteRemindersForTask(taskId: Long)
}
