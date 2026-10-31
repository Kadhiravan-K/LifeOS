package com.example.mcp

import com.example.domain.intelligence.IntelligentPlanningEngine
import com.example.domain.model.HabitItem
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data provider abstraction that bridges MCP tool invocations to local Room/Repository data.
 */
interface McpDataProvider {
    suspend fun getTasks(profileId: Long, status: String?, priority: String?): List<TaskItem>
    suspend fun createTask(profileId: Long, title: String, priority: TaskPriority, notes: String?, dueDate: String?): TaskItem
    suspend fun getProjects(profileId: Long): List<String>
    suspend fun getHabits(profileId: Long): List<HabitItem>
    suspend fun getProfileName(profileId: Long): String
}

/**
 * Core bidirectional Model Context Protocol (MCP) protocol processor (#3).
 * Operates strictly in-memory / local IPC — zero network telemetry.
 */
class McpProtocolEngine(
    private val dataProvider: McpDataProvider
) {

    val supportedTools: List<McpToolDefinition> = listOf(
        McpToolDefinition(
            name = "get_tasks",
            description = "Retrieve tasks for the authorized profile, optionally filtered by status and priority.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "properties": {
                        "status": {
                            "type": "string",
                            "enum": ["ALL", "PENDING", "COMPLETED"],
                            "description": "Filter by completion status (default ALL)"
                        },
                        "priority": {
                            "type": "string",
                            "enum": ["ALL", "HIGH", "MEDIUM", "LOW"],
                            "description": "Filter by task priority"
                        }
                    }
                }
            """.trimIndent())
        ),
        McpToolDefinition(
            name = "create_task",
            description = "Create a new task strictly within the authorized profile.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "required": ["title"],
                    "properties": {
                        "title": { "type": "string", "description": "Title of the task" },
                        "priority": { "type": "string", "enum": ["HIGH", "MEDIUM", "LOW"], "description": "Priority level" },
                        "notes": { "type": "string", "description": "Optional notes or details" },
                        "dueDate": { "type": "string", "description": "Optional ISO-8601 due date YYYY-MM-DD" }
                    }
                }
            """.trimIndent())
        ),
        McpToolDefinition(
            name = "list_projects",
            description = "List all workspace projects within the authorized profile.",
            inputSchema = JSONObject("""{ "type": "object", "properties": {} }""")
        ),
        McpToolDefinition(
            name = "get_daily_summary",
            description = "Get completion metrics, remaining tasks, and habit streaks for today.",
            inputSchema = JSONObject("""{ "type": "object", "properties": {} }""")
        ),
        McpToolDefinition(
            name = "get_intelligent_suggestions",
            description = "Get 100% offline heuristic workload intelligence and smart suggestions.",
            inputSchema = JSONObject("""{ "type": "object", "properties": {} }""")
        ),
        McpToolDefinition(
            name = "get_profile_context",
            description = "Return the metadata of the authorized profile.",
            inputSchema = JSONObject("""{ "type": "object", "properties": {} }""")
        )
    )

    val supportedResources: List<McpResourceDefinition> = listOf(
        McpResourceDefinition(
            uri = "lifeos://profile/summary",
            name = "Profile Summary",
            description = "Live snapshot of profile completion metrics and active streak."
        ),
        McpResourceDefinition(
            uri = "lifeos://tasks/pending",
            name = "Pending Tasks Queue",
            description = "Active pending tasks ordered by priority and due date."
        ),
        McpResourceDefinition(
            uri = "lifeos://intelligence/suggestions",
            name = "Planning Suggestions",
            description = "Heuristic intelligence recommendations generated locally."
        )
    )

    val supportedPrompts: List<McpPromptDefinition> = listOf(
        McpPromptDefinition(
            name = "daily_standup_assistant",
            description = "Generate a structured daily execution plan from today's priorities."
        ),
        McpPromptDefinition(
            name = "weekly_retro_review",
            description = "Summarize weekly achievements, habit consistency, and radar balance."
        ),
        McpPromptDefinition(
            name = "task_breakdown_planner",
            description = "Deconstruct a broad goal into actionable sub-tasks.",
            arguments = listOf(
                McpPromptArgument("goal", "The high-level goal or objective to decompose", required = true)
            )
        )
    )

    /**
     * Dispatches an incoming JSON-RPC 2.0 message with bearer token authentication
     * and profile isolation guard.
     */
    suspend fun handleMessage(
        rawJson: String,
        providedToken: String?,
        storedToken: String?,
        isBridgeEnabled: Boolean,
        authorizedProfileId: Long
    ): McpResponse {
        val request = try {
            McpRequest.fromJson(rawJson)
        } catch (e: Exception) {
            return McpResponse(
                id = null,
                error = McpError(McpError.PARSE_ERROR, "Invalid JSON-RPC format: ${e.message}")
            )
        }

        if (!isBridgeEnabled) {
            return McpResponse(
                id = request.id,
                error = McpError(McpError.UNAUTHORIZED, "LifeOS MCP Bridge is disabled in Settings.")
            )
        }

        // Methods that don't require auth: 'initialize' (discovery handshake)
        if (request.method != "initialize") {
            if (!McpSecurityManager.validateToken(providedToken, storedToken)) {
                return McpResponse(
                    id = request.id,
                    error = McpError(McpError.UNAUTHORIZED, "Invalid or missing Bearer token.")
                )
            }
        }

        return when (request.method) {
            "initialize" -> handleInitialize(request)
            "notifications/initialized" -> McpResponse(id = request.id, result = JSONObject())
            "tools/list" -> handleToolsList(request)
            "tools/call" -> handleToolCall(request, authorizedProfileId)
            "resources/list" -> handleResourcesList(request)
            "resources/read" -> handleResourceRead(request, authorizedProfileId)
            "prompts/list" -> handlePromptsList(request)
            "prompts/get" -> handlePromptGet(request, authorizedProfileId)
            else -> McpResponse(
                id = request.id,
                error = McpError(McpError.METHOD_NOT_FOUND, "Method '${request.method}' not recognized.")
            )
        }
    }

    private fun handleInitialize(request: McpRequest): McpResponse {
        val result = JSONObject()
        result.put("protocolVersion", "2024-11-05")

        val capabilities = JSONObject()
        capabilities.put("tools", JSONObject())
        capabilities.put("resources", JSONObject())
        capabilities.put("prompts", JSONObject())
        result.put("capabilities", capabilities)

        val serverInfo = JSONObject()
        serverInfo.put("name", "LifeOS-MCP-Bridge")
        serverInfo.put("version", "3.0.0")
        result.put("serverInfo", serverInfo)

        return McpResponse(id = request.id, result = result)
    }

    private fun handleToolsList(request: McpRequest): McpResponse {
        val toolsArr = JSONArray()
        supportedTools.forEach { toolsArr.put(it.toJson()) }

        val result = JSONObject()
        result.put("tools", toolsArr)
        return McpResponse(id = request.id, result = result)
    }

    private suspend fun handleToolCall(request: McpRequest, profileId: Long): McpResponse {
        val params = request.params ?: JSONObject()
        val toolName = params.optString("name", "")
        val arguments = params.optJSONObject("arguments") ?: JSONObject()

        return try {
            val contentText = when (toolName) {
                "get_tasks" -> {
                    val status = arguments.optString("status", "ALL")
                    val priority = arguments.optString("priority", "ALL")
                    val tasks = dataProvider.getTasks(profileId, status, priority)
                    val tasksArr = JSONArray()
                    tasks.forEach { t ->
                        val obj = JSONObject()
                        obj.put("id", t.id)
                        obj.put("title", t.title)
                        obj.put("priority", t.priority.name)
                        obj.put("isCompleted", t.isOneTimeCompleted)
                        obj.put("dueDate", t.dueDateIso ?: JSONObject.NULL)
                        obj.put("notes", t.notes ?: JSONObject.NULL)
                        tasksArr.put(obj)
                    }
                    JSONObject().put("tasks", tasksArr).put("count", tasks.size).toString(2)
                }
                "create_task" -> {
                    val title = arguments.getString("title").trim()
                    if (title.isBlank()) throw IllegalArgumentException("Task title cannot be blank.")
                    val priorityStr = arguments.optString("priority", "MEDIUM")
                    val priority = try { TaskPriority.valueOf(priorityStr.uppercase()) } catch (_: Exception) { TaskPriority.MEDIUM }
                    val notes = if (arguments.has("notes") && !arguments.isNull("notes")) arguments.getString("notes") else null
                    val dueDate = if (arguments.has("dueDate") && !arguments.isNull("dueDate")) arguments.getString("dueDate") else null

                    val created = dataProvider.createTask(profileId, title, priority, notes, dueDate)
                    JSONObject()
                        .put("success", true)
                        .put("id", created.id)
                        .put("title", created.title)
                        .put("profileId", profileId)
                        .toString(2)
                }
                "list_projects" -> {
                    val projects = dataProvider.getProjects(profileId)
                    JSONObject().put("projects", JSONArray(projects)).toString(2)
                }
                "get_daily_summary" -> {
                    val tasks = dataProvider.getTasks(profileId, "ALL", "ALL")
                    val habits = dataProvider.getHabits(profileId)
                    val completedTasks = tasks.count { it.isOneTimeCompleted }
                    val pendingTasks = tasks.count { !it.isOneTimeCompleted }
                    val completedHabits = habits.count { it.isCompletedToday }

                    JSONObject()
                        .put("totalTasks", tasks.size)
                        .put("completedTasks", completedTasks)
                        .put("pendingTasks", pendingTasks)
                        .put("totalHabits", habits.size)
                        .put("completedHabitsToday", completedHabits)
                        .put("profileName", dataProvider.getProfileName(profileId))
                        .toString(2)
                }
                "get_intelligent_suggestions" -> {
                    val tasks = dataProvider.getTasks(profileId, "ALL", "ALL")
                    val habits = dataProvider.getHabits(profileId)
                    val analysis = IntelligentPlanningEngine.analyzeWorkload(tasks, habits)

                    val suggArr = JSONArray()
                    analysis.suggestions.forEach { s ->
                        val obj = JSONObject()
                        obj.put("title", s.title)
                        obj.put("type", s.type.name)
                        obj.put("description", s.description)
                        obj.put("action", s.actionRecommendation)
                        suggArr.put(obj)
                    }

                    JSONObject()
                        .put("overloadScore", analysis.overloadScore)
                        .put("riskLevel", analysis.riskLevel.name)
                        .put("pendingTasks", analysis.pendingCount)
                        .put("overdueTasks", analysis.overdueCount)
                        .put("suggestions", suggArr)
                        .toString(2)
                }
                "get_profile_context" -> {
                    val name = dataProvider.getProfileName(profileId)
                    JSONObject()
                        .put("profileId", profileId)
                        .put("profileName", name)
                        .put("sovereignty", "100% Offline-First")
                        .put("mcpProtocolVersion", "2024-11-05")
                        .toString(2)
                }
                else -> throw NoSuchMethodException("Unknown tool '$toolName'")
            }

            val contentArr = JSONArray()
            val textItem = JSONObject()
            textItem.put("type", "text")
            textItem.put("text", contentText)
            contentArr.put(textItem)

            val result = JSONObject()
            result.put("content", contentArr)
            result.put("isError", false)

            McpResponse(id = request.id, result = result)
        } catch (e: Exception) {
            val contentArr = JSONArray()
            val textItem = JSONObject()
            textItem.put("type", "text")
            textItem.put("text", "Error executing tool '$toolName': ${e.message}")
            contentArr.put(textItem)

            val result = JSONObject()
            result.put("content", contentArr)
            result.put("isError", true)

            McpResponse(id = request.id, result = result)
        }
    }

    private fun handleResourcesList(request: McpRequest): McpResponse {
        val resArr = JSONArray()
        supportedResources.forEach { resArr.put(it.toJson()) }

        val result = JSONObject()
        result.put("resources", resArr)
        return McpResponse(id = request.id, result = result)
    }

    private suspend fun handleResourceRead(request: McpRequest, profileId: Long): McpResponse {
        val params = request.params ?: JSONObject()
        val uri = params.optString("uri", "")

        val text = when (uri) {
            "lifeos://profile/summary" -> {
                val tasks = dataProvider.getTasks(profileId, "ALL", "ALL")
                val habits = dataProvider.getHabits(profileId)
                val analysis = IntelligentPlanningEngine.analyzeWorkload(tasks, habits)
                "Profile: ${dataProvider.getProfileName(profileId)}\nOverload Score: ${analysis.overloadScore}/100 (${analysis.riskLevel})\nPending: ${analysis.pendingCount}, Overdue: ${analysis.overdueCount}"
            }
            "lifeos://tasks/pending" -> {
                val tasks = dataProvider.getTasks(profileId, "PENDING", "ALL")
                tasks.joinToString("\n") { "- [${it.priority}] ${it.title} (Due: ${it.dueDateIso ?: "None"})" }
            }
            "lifeos://intelligence/suggestions" -> {
                val tasks = dataProvider.getTasks(profileId, "ALL", "ALL")
                val habits = dataProvider.getHabits(profileId)
                val analysis = IntelligentPlanningEngine.analyzeWorkload(tasks, habits)
                analysis.suggestions.joinToString("\n\n") { "• ${it.title}:\n  ${it.description}\n  -> ${it.actionRecommendation}" }
            }
            else -> return McpResponse(id = request.id, error = McpError(McpError.INVALID_PARAMS, "Resource '$uri' not found."))
        }

        val contentsArr = JSONArray()
        val item = JSONObject()
        item.put("uri", uri)
        item.put("mimeType", "text/plain")
        item.put("text", text)
        contentsArr.put(item)

        val result = JSONObject()
        result.put("contents", contentsArr)
        return McpResponse(id = request.id, result = result)
    }

    private fun handlePromptsList(request: McpRequest): McpResponse {
        val promptArr = JSONArray()
        supportedPrompts.forEach { promptArr.put(it.toJson()) }

        val result = JSONObject()
        result.put("prompts", promptArr)
        return McpResponse(id = request.id, result = result)
    }

    private suspend fun handlePromptGet(request: McpRequest, profileId: Long): McpResponse {
        val params = request.params ?: JSONObject()
        val promptName = params.optString("name", "")
        val arguments = params.optJSONObject("arguments") ?: JSONObject()

        val promptText = when (promptName) {
            "daily_standup_assistant" -> {
                val tasks = dataProvider.getTasks(profileId, "PENDING", "HIGH")
                val taskList = tasks.joinToString("\n") { "  - ${it.title}" }
                """
                    You are LifeOS Daily Standup Assistant.
                    Review today's high-priority tasks and construct a distraction-free deep-work execution sequence:
                    High-Priority Tasks:
                    $taskList
                    
                    Instructions:
                    1. Group tasks by cognitive energy level.
                    2. Allocate 90-minute morning deep work blocks.
                    3. Highlight potential dependencies.
                """.trimIndent()
            }
            "weekly_retro_review" -> {
                val habits = dataProvider.getHabits(profileId)
                val streaks = habits.joinToString("\n") { "  - ${it.title}: ${it.currentStreak} day streak" }
                """
                    You are LifeOS Retrospective Reviewer.
                    Analyze weekly consistency and habit retention:
                    Habit Performance:
                    $streaks
                    
                    Provide 3 actionable refinements for the coming week.
                """.trimIndent()
            }
            "task_breakdown_planner" -> {
                val goal = arguments.optString("goal", "Primary Objective")
                """
                    Deconstruct the following high-level goal into actionable, MECE (Mutually Exclusive, Collectively Exhaustive) sub-tasks:
                    Goal: $goal
                    
                    Output 3-5 concrete steps with priority ratings and estimated completion times.
                """.trimIndent()
            }
            else -> return McpResponse(id = request.id, error = McpError(McpError.INVALID_PARAMS, "Prompt '$promptName' not found."))
        }

        val messagesArr = JSONArray()
        val msg = JSONObject()
        msg.put("role", "user")
        val content = JSONObject()
        content.put("type", "text")
        content.put("text", promptText)
        msg.put("content", content)
        messagesArr.put(msg)

        val result = JSONObject()
        result.put("messages", messagesArr)
        result.put("description", "Generated prompt for $promptName")
        return McpResponse(id = request.id, result = result)
    }
}
