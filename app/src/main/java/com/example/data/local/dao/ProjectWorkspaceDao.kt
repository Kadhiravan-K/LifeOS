package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.ProjectCustomStatusEntity
import com.example.data.local.entities.ProjectWorkspaceItemEntity
import com.example.data.local.entities.ProjectWorkspaceViewEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for Advanced Project Workspace (Release 3 Requirement #20).
 */
@Dao
interface ProjectWorkspaceDao {
    // --- Items ---
    @Query("SELECT * FROM project_workspace_items WHERE projectId = :projectId AND profileId = :profileId ORDER BY orderIndex ASC, id ASC")
    fun getItemsForProjectFlow(projectId: Long, profileId: Long): Flow<List<ProjectWorkspaceItemEntity>>

    @Query("SELECT * FROM project_workspace_items WHERE projectId = :projectId AND profileId = :profileId ORDER BY orderIndex ASC, id ASC")
    suspend fun getItemsForProjectList(projectId: Long, profileId: Long): List<ProjectWorkspaceItemEntity>

    @Query("SELECT * FROM project_workspace_items WHERE profileId = :profileId")
    suspend fun getAllItemsForProfile(profileId: Long): List<ProjectWorkspaceItemEntity>

    @Query("SELECT * FROM project_workspace_items")
    suspend fun getAllItems(): List<ProjectWorkspaceItemEntity>

    @Query("SELECT * FROM project_workspace_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): ProjectWorkspaceItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ProjectWorkspaceItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ProjectWorkspaceItemEntity>): List<Long>

    @Update
    suspend fun updateItem(item: ProjectWorkspaceItemEntity)

    @Delete
    suspend fun deleteItem(item: ProjectWorkspaceItemEntity): Int

    @Query("DELETE FROM project_workspace_items WHERE id IN (:ids)")
    suspend fun deleteItemsByIds(ids: List<Long>): Int

    @Query("DELETE FROM project_workspace_items WHERE projectId = :projectId")
    suspend fun deleteAllItemsForProject(projectId: Long): Int

    @Query("DELETE FROM project_workspace_items WHERE profileId = :profileId")
    suspend fun deleteAllItemsForProfile(profileId: Long): Int

    // --- Saved Views ---
    @Query("SELECT * FROM project_workspace_views WHERE projectId = :projectId AND profileId = :profileId ORDER BY isDefault DESC, id ASC")
    fun getViewsForProjectFlow(projectId: Long, profileId: Long): Flow<List<ProjectWorkspaceViewEntity>>

    @Query("SELECT * FROM project_workspace_views WHERE projectId = :projectId AND profileId = :profileId")
    suspend fun getViewsForProjectList(projectId: Long, profileId: Long): List<ProjectWorkspaceViewEntity>

    @Query("SELECT * FROM project_workspace_views WHERE profileId = :profileId")
    suspend fun getAllViewsForProfile(profileId: Long): List<ProjectWorkspaceViewEntity>

    @Query("SELECT * FROM project_workspace_views")
    suspend fun getAllViews(): List<ProjectWorkspaceViewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertView(view: ProjectWorkspaceViewEntity): Long

    @Update
    suspend fun updateView(view: ProjectWorkspaceViewEntity)

    @Query("DELETE FROM project_workspace_views WHERE id = :id")
    suspend fun deleteViewById(id: Long): Int

    @Query("DELETE FROM project_workspace_views WHERE profileId = :profileId")
    suspend fun deleteAllViewsForProfile(profileId: Long): Int

    // --- Custom Statuses ---
    @Query("SELECT * FROM project_custom_statuses WHERE projectId = :projectId AND profileId = :profileId ORDER BY orderIndex ASC")
    fun getCustomStatusesForProjectFlow(projectId: Long, profileId: Long): Flow<List<ProjectCustomStatusEntity>>

    @Query("SELECT * FROM project_custom_statuses WHERE projectId = :projectId AND profileId = :profileId ORDER BY orderIndex ASC")
    suspend fun getCustomStatusesForProjectList(projectId: Long, profileId: Long): List<ProjectCustomStatusEntity>

    @Query("SELECT * FROM project_custom_statuses WHERE profileId = :profileId")
    suspend fun getAllCustomStatusesForProfile(profileId: Long): List<ProjectCustomStatusEntity>

    @Query("SELECT * FROM project_custom_statuses")
    suspend fun getAllCustomStatuses(): List<ProjectCustomStatusEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomStatus(status: ProjectCustomStatusEntity): Long

    @Update
    suspend fun updateCustomStatus(status: ProjectCustomStatusEntity)

    @Query("DELETE FROM project_custom_statuses WHERE id = :id")
    suspend fun deleteCustomStatusById(id: Long): Int

    @Query("DELETE FROM project_custom_statuses WHERE profileId = :profileId")
    suspend fun deleteAllCustomStatusesForProfile(profileId: Long): Int
}
