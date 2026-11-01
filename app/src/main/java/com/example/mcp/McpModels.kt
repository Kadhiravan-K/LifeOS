package com.example.mcp

import org.json.JSONArray
import org.json.JSONObject

/**
 * Standard Model Context Protocol (MCP) JSON-RPC 2.0 message models.
 * Complies with Anthropic Model Context Protocol specification (2024-11-05).
 */

sealed class McpMessage {
    abstract val jsonrpc: String
}

data class McpRequest(
    override val jsonrpc: String = "2.0",
    val id: Any? = null,
    val method: String,
    val params: JSONObject? = null
) : McpMessage() {
    companion object {
        fun fromJson(jsonStr: String): McpRequest {
            val root = JSONObject(jsonStr)
            val jsonrpc = root.optString("jsonrpc", "2.0")
            val id = if (root.has("id")) root.get("id") else null
            val method = root.getString("method")
            val params = root.optJSONObject("params")
            return McpRequest(jsonrpc, id, method, params)
        }
    }
}

data class McpResponse(
    override val jsonrpc: String = "2.0",
    val id: Any? = null,
    val result: Any? = null,
    val error: McpError? = null
) : McpMessage() {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("jsonrpc", jsonrpc)
        root.put("id", id ?: JSONObject.NULL)
        if (error != null) {
            root.put("error", error.toJson())
        } else {
            root.put("result", result ?: JSONObject())
        }
        return root.toString()
    }
}

data class McpError(
    val code: Int,
    val message: String,
    val data: Any? = null
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("code", code)
        obj.put("message", message)
        if (data != null) {
            obj.put("data", data)
        }
        return obj
    }

    companion object {
        const val PARSE_ERROR = -32700
        const val INVALID_REQUEST = -32600
        const val METHOD_NOT_FOUND = -32601
        const val INVALID_PARAMS = -32602
        const val INTERNAL_ERROR = -32603
        const val UNAUTHORIZED = -32001
        const val PROFILE_ISOLATION_VIOLATION = -32002
    }
}

data class McpToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: JSONObject
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("name", name)
        obj.put("description", description)
        obj.put("inputSchema", inputSchema)
        return obj
    }
}

data class McpResourceDefinition(
    val uri: String,
    val name: String,
    val description: String,
    val mimeType: String = "application/json"
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("uri", uri)
        obj.put("name", name)
        obj.put("description", description)
        obj.put("mimeType", mimeType)
        return obj
    }
}

data class McpPromptDefinition(
    val name: String,
    val description: String,
    val arguments: List<McpPromptArgument> = emptyList()
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("name", name)
        obj.put("description", description)
        val argsArr = JSONArray()
        arguments.forEach { argsArr.put(it.toJson()) }
        obj.put("arguments", argsArr)
        return obj
    }
}

data class McpPromptArgument(
    val name: String,
    val description: String,
    val required: Boolean = false
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("name", name)
        obj.put("description", description)
        obj.put("required", required)
        return obj
    }
}

/**
 * Lifecycle connection state of an external MCP server connection.
 */
enum class McpConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR;

    val isConnected: Boolean get() = this == CONNECTED
}

/**
 * Transport options for MCP communication.
 */
enum class McpTransportType(val displayName: String) {
    HTTP_SSE("HTTP with Server-Sent Events (SSE)"),
    STREAMABLE_HTTP("Streamable HTTP / Localhost"),
    STDIO_IPC("Standard I/O (STDIO) IPC"),
    LOCAL_IN_PROCESS("Local In-Process Sovereign IPC")
}

/**
 * Capabilities negotiated during MCP 'initialize' handshake.
 */
data class McpServerCapabilities(
    val protocolVersion: String = "2024-11-05",
    val serverName: String = "Unknown Server",
    val serverVersion: String = "1.0.0",
    val hasTools: Boolean = true,
    val hasResources: Boolean = true,
    val hasPrompts: Boolean = true
) {
    companion object {
        fun fromInitializeResult(resultObj: JSONObject?): McpServerCapabilities {
            if (resultObj == null) return McpServerCapabilities()
            val version = resultObj.optString("protocolVersion", "2024-11-05")
            val serverInfo = resultObj.optJSONObject("serverInfo")
            val name = serverInfo?.optString("name", "Unknown Server") ?: "Unknown Server"
            val sVer = serverInfo?.optString("version", "1.0.0") ?: "1.0.0"
            val capabilities = resultObj.optJSONObject("capabilities")
            val hasTools = capabilities?.has("tools") == true
            val hasResources = capabilities?.has("resources") == true
            val hasPrompts = capabilities?.has("prompts") == true
            return McpServerCapabilities(
                protocolVersion = version,
                serverName = name,
                serverVersion = sVer,
                hasTools = hasTools,
                hasResources = hasResources,
                hasPrompts = hasPrompts
            )
        }
    }
}

/**
 * Typed result of an MCP tool invocation.
 */
data class McpToolCallResult(
    val isError: Boolean,
    val content: List<String>,
    val rawResult: JSONObject? = null,
    val errorMessage: String? = null
) {
    val textOutput: String
        get() = content.joinToString("\n")
}

/**
 * Immutable audit event recorded for MCP security auditing (#3, #5).
 */
data class McpAuditEvent(
    val profileId: Long = 1,
    val direction: String, // "INBOUND_SERVER" or "OUTBOUND_CLIENT"
    val eventType: String, // "AUTH", "TOOL_DISCOVERY", "TOOL_CALL", "TOOL_APPROVAL", "WRITE_OP", "CONNECTION"
    val toolOrResource: String,
    val details: String,
    val status: String, // "SUCCESS", "DENIED", "FAILED", "PENDING_APPROVAL"
    val timestamp: Long = System.currentTimeMillis()
)

