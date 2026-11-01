package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted external MCP server registration for LifeOS MCP Client (#3, #4).
 * Strictly profile-isolated: each profile manages its own trusted external servers.
 */
@Entity(
    tableName = "mcp_servers",
    indices = [
        Index("profileId"),
        Index(value = ["profileId", "name"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class McpServerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 1,
    val name: String,
    val endpoint: String,
    val transport: String = "HTTP_SSE",
    val authToken: String? = null,
    val status: String = "DISCONNECTED",
    val errorMessage: String? = null,
    val lastConnectedAt: Long? = null,
    val discoveredToolsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Immutable audit record for all MCP security events (#3, #5).
 * Records inbound agent server calls and outbound client tool executions.
 */
@Entity(
    tableName = "mcp_audit_logs",
    indices = [
        Index("profileId"),
        Index("timestamp")
    ],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class McpAuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val direction: String, // "INBOUND_SERVER" or "OUTBOUND_CLIENT"
    val eventType: String, // "AUTH", "TOOL_DISCOVERY", "TOOL_CALL", "TOOL_APPROVAL", "WRITE_OP", "CONNECTION"
    val toolOrResource: String,
    val details: String,
    val status: String // "SUCCESS", "DENIED", "FAILED", "PENDING_APPROVAL"
)
