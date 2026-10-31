package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE isArchived = 0 ORDER BY isFavorite DESC, orderIndex ASC, name ASC")
    fun getActiveProjectsFlow(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY isArchived ASC, isFavorite DESC, orderIndex ASC, name ASC")
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedProjectsFlow(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE profileId = :profileId AND isArchived = 0 ORDER BY isFavorite DESC, orderIndex ASC, name ASC")
    fun getActiveProjectsForProfileFlow(profileId: Long): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE profileId = :profileId ORDER BY isArchived ASC, isFavorite DESC, orderIndex ASC, name ASC")
    fun getAllProjectsForProfileFlow(profileId: Long): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE profileId = :profileId AND isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedProjectsForProfileFlow(profileId: Long): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id AND profileId = :profileId LIMIT 1")
    suspend fun getProjectByIdForProfile(id: Long, profileId: Long): ProjectEntity?

    @Query("SELECT * FROM projects ORDER BY orderIndex ASC")
    suspend fun getAllProjectsList(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE profileId = :profileId ORDER BY orderIndex ASC")
    suspend fun getAllProjectsForProfileList(profileId: Long): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("UPDATE projects SET isArchived = :isArchived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setProjectArchived(id: Long, isArchived: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setProjectFavorite(id: Long, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("DELETE FROM projects WHERE id = :id AND profileId = :profileId")
    suspend fun deleteProjectByIdForProfile(id: Long, profileId: Long)

    @Query("DELETE FROM projects WHERE profileId = :profileId")
    suspend fun deleteAllProjectsForProfile(profileId: Long)

    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId")
    fun getTaskCountForProject(projectId: Long): Flow<Int>

    @Query("UPDATE tasks SET projectId = NULL WHERE projectId = :projectId")
    suspend fun unassignTasksFromProject(projectId: Long)
}
