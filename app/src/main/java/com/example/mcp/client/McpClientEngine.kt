package com.example.mcp.client

import com.example.data.local.dao.McpDao
import com.example.data.local.entities.McpAuditLogEntity
import com.example.data.local.entities.McpServerEntity
import com.example.mcp.McpError
import com.example.mcp.McpRequest
import com.example.mcp.McpResourceDefinition
import com.example.mcp.McpResponse
import com.example.mcp.McpServerCapabilities
import com.example.mcp.McpToolCallResult
import com.example.mcp.McpToolDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Transport abstraction for outgoing MCP Client requests (#4).
 */
interface McpClientTransport {
    suspend fun sendRequest(
        endpoint: String,
        request: McpRequest,
        authToken: String? = null
    ): McpResponse
}

/**
 * Sovereign In-Process IPC Transport for offline local agents and testing.
 * Simulates external MCP servers (e.g., `ipc://mock-assistant`) without network sockets.
 */
class LocalIpcMcpTransport : McpClientTransport {

    private val mockTools = listOf(
        McpToolDefinition(
            name = "calculate_workload_capacity",
            description = "Calculates remaining focus hours and bandwidth based on tasks count.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "required": ["pendingTasks", "availableHours"],
                    "properties": {
                        "pendingTasks": { "type": "integer", "description": "Number of open tasks" },
                        "availableHours": { "type": "number", "description": "Free hours in day" }
                    }
                }
            """.trimIndent())
        ),
        McpToolDefinition(
            name = "format_executive_brief",
            description = "Formats bullet points into a clean executive summary.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "required": ["title", "points"],
                    "properties": {
                        "title": { "type": "string", "description": "Summary title" },
                        "points": { "type": "array", "description": "List of key accomplishments" }
                    }
                }
            """.trimIndent())
        )
    )

    private val mockResources = listOf(
        McpResourceDefinition(
            uri = "mock://agent/status",
            name = "Local Agent Status",
            description = "Real-time state of the connected in-process assistant."
        )
    )

    override suspend fun sendRequest(
        endpoint: String,
        request: McpRequest,
        authToken: String?
    ): McpResponse = withContext(Dispatchers.Default) {
        when (request.method) {
            "initialize" -> {
                val result = JSONObject()
                result.put("protocolVersion", "2024-11-05")
                val serverInfo = JSONObject()
                serverInfo.put("name", "Local-Assistant-Agent")
                serverInfo.put("version", "1.2.0")
                result.put("serverInfo", serverInfo)
                val capabilities = JSONObject()
                capabilities.put("tools", JSONObject())
                capabilities.put("resources", JSONObject())
                result.put("capabilities", capabilities)
                McpResponse(id = request.id, result = result)
            }
            "notifications/initialized" -> {
                McpResponse(id = request.id, result = JSONObject())
            }
            "tools/list" -> {
                val toolsArr = JSONArray()
                mockTools.forEach { toolsArr.put(it.toJson()) }
                val result = JSONObject().put("tools", toolsArr)
                McpResponse(id = request.id, result = result)
            }
            "tools/call" -> {
                val params = request.params ?: JSONObject()
                val toolName = params.optString("name", "")
                val arguments = params.optJSONObject("arguments") ?: JSONObject()

                val text = when (toolName) {
                    "calculate_workload_capacity" -> {
                        val tasks = arguments.optInt("pendingTasks", 0)
                        val hours = arguments.optDouble("availableHours", 0.0)
                        val estimatedHoursNeeded = tasks * 0.75
                        val remainingCapacity = hours - estimatedHoursNeeded
                        val status = if (remainingCapacity >= 0) "SUFFICIENT_BANDWIDTH" else "OVERLOAD_RISK"
                        JSONObject()
                            .put("status", status)
                            .put("neededHours", estimatedHoursNeeded)
                            .put("surplusHours", remainingCapacity)
                            .toString(2)
                    }
                    "format_executive_brief" -> {
                        val title = arguments.optString("title", "Executive Brief")
                        val points = arguments.optJSONArray("points") ?: JSONArray()
                        val sb = StringBuilder("# $title\n\n")
                        for (i in 0 until points.length()) {
                            sb.append("• ").append(points.getString(i)).append("\n")
                        }
                        sb.toString()
                    }
                    else -> throw IllegalArgumentException("Unknown tool: $toolName")
                }

                val contentArr = JSONArray()
                contentArr.put(JSONObject().put("type", "text").put("text", text))
                val result = JSONObject().put("content", contentArr).put("isError", false)
                McpResponse(id = request.id, result = result)
            }
            "resources/list" -> {
                val resArr = JSONArray()
                mockResources.forEach { resArr.put(it.toJson()) }
                val result = JSONObject().put("resources", resArr)
                McpResponse(id = request.id, result = result)
            }
            else -> {
                McpResponse(
                    id = request.id,
                    error = McpError(McpError.METHOD_NOT_FOUND, "Method '${request.method}' not supported by mock agent")
                )
            }
        }
    }
}

/**
 * Standard HTTP Transport for connecting to localhost/LAN MCP servers (#4).
 */
class HttpMcpTransport : McpClientTransport {

    companion object {
        const val CONNECT_TIMEOUT_MS = 5000
        const val READ_TIMEOUT_MS = 10000
        const val MAX_RESPONSE_BYTES = 5 * 1024 * 1024 // 5 MB limit
    }

    override suspend fun sendRequest(
        endpoint: String,
        request: McpRequest,
        authToken: String?
    ): McpResponse = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(endpoint)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.doOutput = true
            connection.doInput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Accept", "application/json")

            if (!authToken.isNullOrBlank()) {
                val cleanToken = authToken.removePrefix("Bearer ").trim()
                connection.setRequestProperty("Authorization", "Bearer $cleanToken")
            }

            val requestBody = JSONObject().apply {
                put("jsonrpc", request.jsonrpc)
                put("id", request.id ?: 1)
                put("method", request.method)
                if (request.params != null) {
                    put("params", request.params)
                }
            }.toString()

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }

            val responseCode = connection.responseCode
            val inputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseText = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                val sb = StringBuilder()
                var line: String?
                var totalBytes = 0
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line).append("\n")
                    totalBytes += line!!.length
                    if (totalBytes > MAX_RESPONSE_BYTES) {
                        throw SecurityException("Response exceeded maximum payload size ($MAX_RESPONSE_BYTES bytes)")
                    }
                }
                sb.toString()
            }

            val root = JSONObject(responseText)
            val jsonrpc = root.optString("jsonrpc", "2.0")
            val id = if (root.has("id")) root.get("id") else null
            val error = if (root.has("error") && !root.isNull("error")) {
                val errObj = root.getJSONObject("error")
                McpError(errObj.optInt("code", -32603), errObj.optString("message", "Unknown error"), errObj.opt("data"))
            } else null
            val result = if (root.has("result")) root.get("result") else null

            McpResponse(jsonrpc = jsonrpc, id = id, result = result, error = error)
        } catch (e: Exception) {
            McpResponse(
                id = request.id,
                error = McpError(McpError.INTERNAL_ERROR, "Transport error connecting to $endpoint: ${e.message}")
            )
        } finally {
            connection?.disconnect()
        }
    }
}

/**
 * High-level MCP Client Engine for LifeOS (#4).
 * Manages outgoing connections to external MCP servers, handles initialization,
 * enforces tool argument schema validation, requires explicit user confirmation
 * before invoking tools, and records audit trails.
 */
class McpClientEngine(
    private val mcpDao: McpDao,
    private val httpTransport: McpClientTransport = HttpMcpTransport(),
    private val ipcTransport: McpClientTransport = LocalIpcMcpTransport()
) {

    private fun getTransportForEndpoint(endpoint: String): McpClientTransport {
        return if (endpoint.startsWith("ipc://") || endpoint.startsWith("local://")) {
            ipcTransport
        } else {
            httpTransport
        }
    }

    /**
     * Connects to an external MCP server, negotiates capabilities, and discovers tools.
     */
    suspend fun connectServer(server: McpServerEntity): Result<McpServerCapabilities> = withContext(Dispatchers.IO) {
        try {
            mcpDao.updateServerStatus(server.id, "CONNECTING", null, null, 0)
            val transport = getTransportForEndpoint(server.endpoint)

            // 1. Send 'initialize'
            val initRequest = McpRequest(
                id = 1,
                method = "initialize",
                params = JSONObject().apply {
                    put("protocolVersion", "2024-11-05")
                    put("clientInfo", JSONObject().put("name", "LifeOS-Client").put("version", "3.0.0"))
                    put("capabilities", JSONObject().put("tools", JSONObject()).put("resources", JSONObject()))
                }
            )

            val initResponse = transport.sendRequest(server.endpoint, initRequest, server.authToken)
            if (initResponse.error != null) {
                val errorMsg = initResponse.error.message
                mcpDao.updateServerStatus(server.id, "ERROR", errorMsg, null, 0)
                recordAudit(server.profileId, "CONNECTION", server.name, "Failed initialize: $errorMsg", "FAILED")
                return@withContext Result.failure(IllegalStateException("Initialization failed: $errorMsg"))
            }

            val resultObj = initResponse.result as? JSONObject
            val capabilities = McpServerCapabilities.fromInitializeResult(resultObj)

            // 2. Send 'notifications/initialized'
            val ackRequest = McpRequest(id = null, method = "notifications/initialized", params = JSONObject())
            transport.sendRequest(server.endpoint, ackRequest, server.authToken)

            // 3. Discover tools
            val toolsResult = discoverTools(server)
            val toolsCount = toolsResult.getOrNull()?.size ?: 0

            mcpDao.updateServerStatus(server.id, "CONNECTED", null, System.currentTimeMillis(), toolsCount)
            recordAudit(
                profileId = server.profileId,
                eventType = "CONNECTION",
                toolOrResource = server.name,
                details = "Connected to '${capabilities.serverName}' v${capabilities.serverVersion} with $toolsCount tools discovered",
                status = "SUCCESS"
            )

            Result.success(capabilities)
        } catch (e: Exception) {
            val msg = e.message ?: "Unknown connection error"
            mcpDao.updateServerStatus(server.id, "ERROR", msg, null, 0)
            recordAudit(server.profileId, "CONNECTION", server.name, "Connection error: $msg", "FAILED")
            Result.failure(e)
        }
    }

    /**
     * Discovers all available tools exposed by an external MCP server.
     */
    suspend fun discoverTools(server: McpServerEntity): Result<List<McpToolDefinition>> = withContext(Dispatchers.IO) {
        try {
            val transport = getTransportForEndpoint(server.endpoint)
            val request = McpRequest(id = 2, method = "tools/list", params = JSONObject())
            val response = transport.sendRequest(server.endpoint, request, server.authToken)

            if (response.error != null) {
                return@withContext Result.failure(IllegalStateException(response.error.message))
            }

            val resultObj = response.result as? JSONObject ?: JSONObject()
            val toolsArr = resultObj.optJSONArray("tools") ?: JSONArray()
            val toolsList = mutableListOf<McpToolDefinition>()

            for (i in 0 until toolsArr.length()) {
                val toolObj = toolsArr.getJSONObject(i)
                val name = toolObj.getString("name")
                val desc = toolObj.optString("description", "")
                val schema = toolObj.optJSONObject("inputSchema") ?: JSONObject()
                toolsList.add(McpToolDefinition(name = name, description = desc, inputSchema = schema))
            }

            recordAudit(
                profileId = server.profileId,
                eventType = "TOOL_DISCOVERY",
                toolOrResource = server.name,
                details = "Discovered ${toolsList.size} tools from server",
                status = "SUCCESS"
            )

            Result.success(toolsList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validates proposed tool arguments against its declared inputSchema.
     */
    fun validateToolArguments(tool: McpToolDefinition, arguments: JSONObject): Result<Unit> {
        val required = tool.inputSchema.optJSONArray("required")
        if (required != null) {
            for (i in 0 until required.length()) {
                val reqProp = required.getString(i)
                if (!arguments.has(reqProp) || arguments.isNull(reqProp)) {
                    return Result.failure(IllegalArgumentException("Missing required parameter '$reqProp' for tool '${tool.name}'"))
                }
            }
        }
        return Result.success(Unit)
    }

    /**
     * Executes a tool on the external server.
     * Enforces the mandatory Security Approval Gate (#4, #5).
     */
    suspend fun executeTool(
        server: McpServerEntity,
        tool: McpToolDefinition,
        arguments: JSONObject,
        userApproved: Boolean
    ): Result<McpToolCallResult> = withContext(Dispatchers.IO) {
        // 1. Mandatory Approval Gate
        if (!userApproved) {
            recordAudit(
                profileId = server.profileId,
                eventType = "TOOL_APPROVAL",
                toolOrResource = "${server.name} / ${tool.name}",
                details = "Execution blocked: missing explicit user approval for tool call",
                status = "DENIED"
            )
            return@withContext Result.failure(
                SecurityException("Tool execution requires explicit user confirmation. Execution denied.")
            )
        }

        // 2. Validate input schema
        val validation = validateToolArguments(tool, arguments)
        if (validation.isFailure) {
            return@withContext Result.failure(validation.exceptionOrNull()!!)
        }

        try {
            val transport = getTransportForEndpoint(server.endpoint)
            val callRequest = McpRequest(
                id = System.currentTimeMillis(),
                method = "tools/call",
                params = JSONObject().apply {
                    put("name", tool.name)
                    put("arguments", arguments)
                }
            )

            val response = transport.sendRequest(server.endpoint, callRequest, server.authToken)
            if (response.error != null) {
                recordAudit(
                    profileId = server.profileId,
                    eventType = "TOOL_CALL",
                    toolOrResource = "${server.name} / ${tool.name}",
                    details = "Error from server: ${response.error.message}",
                    status = "FAILED"
                )
                return@withContext Result.success(
                    McpToolCallResult(
                        isError = true,
                        content = emptyList(),
                        errorMessage = response.error.message
                    )
                )
            }

            val resultObj = response.result as? JSONObject ?: JSONObject()
            val isError = resultObj.optBoolean("isError", false)
            val contentArr = resultObj.optJSONArray("content") ?: JSONArray()
            val textList = mutableListOf<String>()

            for (i in 0 until contentArr.length()) {
                val c = contentArr.getJSONObject(i)
                val text = c.optString("text", "")
                textList.add(text)
            }

            recordAudit(
                profileId = server.profileId,
                eventType = "TOOL_CALL",
                toolOrResource = "${server.name} / ${tool.name}",
                details = "Executed tool call with ${textList.size} response chunks (isError=$isError)",
                status = if (isError) "FAILED" else "SUCCESS"
            )

            Result.success(
                McpToolCallResult(
                    isError = isError,
                    content = textList,
                    rawResult = resultObj
                )
            )
        } catch (e: Exception) {
            recordAudit(
                profileId = server.profileId,
                eventType = "TOOL_CALL",
                toolOrResource = "${server.name} / ${tool.name}",
                details = "Exception: ${e.message}",
                status = "FAILED"
            )
            Result.failure(e)
        }
    }

    /**
     * Disconnects an external server cleanly.
     */
    suspend fun disconnectServer(server: McpServerEntity) = withContext(Dispatchers.IO) {
        mcpDao.updateServerStatus(server.id, "DISCONNECTED", null, null, 0)
        recordAudit(
            profileId = server.profileId,
            eventType = "CONNECTION",
            toolOrResource = server.name,
            details = "Server disconnected by user",
            status = "SUCCESS"
        )
    }

    private suspend fun recordAudit(
        profileId: Long,
        eventType: String,
        toolOrResource: String,
        details: String,
        status: String
    ) {
        try {
            mcpDao.insertAuditLog(
                McpAuditLogEntity(
                    profileId = profileId,
                    direction = "OUTBOUND_CLIENT",
                    eventType = eventType,
                    toolOrResource = toolOrResource,
                    details = details,
                    status = status
                )
            )
        } catch (_: Exception) {}
    }
}
