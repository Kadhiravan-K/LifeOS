package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.LabelEntity
import com.example.data.local.entities.TaskLabelCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {
    @Query("SELECT * FROM labels WHERE isArchived = 0 ORDER BY isFavorite DESC, orderIndex ASC, name ASC")
    fun getActiveLabelsFlow(): Flow<List<LabelEntity>>

    @Query("SELECT * FROM labels ORDER BY isArchived ASC, isFavorite DESC, orderIndex ASC, name ASC")
    fun getAllLabelsFlow(): Flow<List<LabelEntity>>

    @Query("SELECT * FROM labels WHERE id = :id LIMIT 1")
    suspend fun getLabelById(id: Long): LabelEntity?

    @Query("SELECT * FROM labels ORDER BY orderIndex ASC")
    suspend fun getAllLabelsList(): List<LabelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: LabelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabels(labels: List<LabelEntity>)

    @Update
    suspend fun updateLabel(label: LabelEntity)

    @Query("UPDATE labels SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setLabelFavorite(id: Long, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM labels WHERE id = :id")
    suspend fun deleteLabelById(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskLabelCrossRef(ref: TaskLabelCrossRef)

    @Query("SELECT * FROM task_label_cross_ref")
    fun getAllCrossRefsFlow(): Flow<List<TaskLabelCrossRef>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskLabelCrossRefs(refs: List<TaskLabelCrossRef>)

    @Query("DELETE FROM task_label_cross_ref WHERE taskId = :taskId")
    suspend fun clearLabelsForTask(taskId: Long)

    @Query("DELETE FROM task_label_cross_ref WHERE labelId = :labelId")
    suspend fun clearTaskRefsForLabel(labelId: Long)

    @Query("DELETE FROM task_label_cross_ref WHERE taskId = :taskId AND labelId = :labelId")
    suspend fun removeLabelFromTask(taskId: Long, labelId: Long)

    @Query("""
        SELECT l.* FROM labels l
        INNER JOIN task_label_cross_ref x ON l.id = x.labelId
        WHERE x.taskId = :taskId AND l.isArchived = 0
        ORDER BY l.name ASC
    """)
    fun getLabelsForTaskFlow(taskId: Long): Flow<List<LabelEntity>>

    @Query("""
        SELECT l.* FROM labels l
        INNER JOIN task_label_cross_ref x ON l.id = x.labelId
        WHERE x.taskId = :taskId
    """)
    suspend fun getLabelsForTaskList(taskId: Long): List<LabelEntity>

    @Query("SELECT taskId FROM task_label_cross_ref WHERE labelId = :labelId")
    fun getTaskIdsForLabelFlow(labelId: Long): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM task_label_cross_ref WHERE labelId = :labelId")
    fun getTaskCountForLabel(labelId: Long): Flow<Int>
}
