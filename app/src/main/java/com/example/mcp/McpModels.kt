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
