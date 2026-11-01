package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.McpAuditLogEntity
import com.example.data.local.entities.McpServerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for external MCP servers and security audit logs (#3, #5).
 */
@Dao
interface McpDao {

    // --- External MCP Servers ---

    @Query("SELECT * FROM mcp_servers WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun getAllServersFlow(profileId: Long): Flow<List<McpServerEntity>>

    @Query("SELECT * FROM mcp_servers WHERE profileId = :profileId ORDER BY createdAt DESC")
    suspend fun getAllServersList(profileId: Long): List<McpServerEntity>

    @Query("SELECT * FROM mcp_servers WHERE id = :id LIMIT 1")
    suspend fun getServerById(id: Long): McpServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: McpServerEntity): Long

    @Update
    suspend fun updateServer(server: McpServerEntity)

    @Query("DELETE FROM mcp_servers WHERE id = :id")
    suspend fun deleteServer(id: Long)

    @Query("DELETE FROM mcp_servers WHERE profileId = :profileId")
    suspend fun deleteServersForProfile(profileId: Long): Int

    @Query("""
        UPDATE mcp_servers 
        SET status = :status, errorMessage = :errorMessage, lastConnectedAt = :lastConnectedAt, discoveredToolsCount = :toolsCount 
        WHERE id = :id
    """)
    suspend fun updateServerStatus(
        id: Long,
        status: String,
        errorMessage: String?,
        lastConnectedAt: Long?,
        toolsCount: Int
    )

    // --- MCP Security Audit Logs ---

    @Query("SELECT * FROM mcp_audit_logs WHERE profileId = :profileId ORDER BY timestamp DESC LIMIT :limit")
    fun getAuditLogsFlow(profileId: Long, limit: Int = 100): Flow<List<McpAuditLogEntity>>

    @Query("SELECT * FROM mcp_audit_logs WHERE profileId = :profileId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getAuditLogsList(profileId: Long, limit: Int = 100): List<McpAuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: McpAuditLogEntity): Long

    @Query("DELETE FROM mcp_audit_logs WHERE profileId = :profileId")
    suspend fun clearAuditLogs(profileId: Long)
}
