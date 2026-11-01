package com.example.mcp

import com.example.domain.intelligence.IntelligentPlanningEngine
import com.example.domain.model.HabitItem
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data provider abstraction that bridges MCP tool invocations to local Room/Repository data (#3).
 */
interface McpDataProvider {
    // Tasks
    suspend fun getTasks(profileId: Long, status: String?, priority: String?): List<TaskItem>
    suspend fun getTaskDetails(profileId: Long, taskId: Long): TaskItem?
    suspend fun searchTasks(profileId: Long, query: String): List<TaskItem>
    suspend fun getOverdueTasks(profileId: Long): List<TaskItem>
    suspend fun getCompletedTasks(profileId: Long, limit: Int = 20): List<TaskItem>

    // Projects
    suspend fun getProjects(profileId: Long): List<Pair<Long, String>>
    suspend fun getProjectDetails(profileId: Long, projectId: Long): JSONObject?
    suspend fun getProjectStatusAndProgress(profileId: Long, projectId: Long): JSONObject?
    suspend fun searchProjects(profileId: Long, query: String): List<JSONObject>

    // Planning
    suspend fun getDailyPlanning(profileId: Long, dateIso: String?): JSONObject
    suspend fun getWeeklyPlanning(profileId: Long): JSONObject
    suspend fun getMonthlyPlanning(profileId: Long): JSONObject
    suspend fun getYearlyPlanning(profileId: Long): JSONObject

    // Productivity & Metrics
    suspend fun getProductivityMetrics(profileId: Long, horizon: String): JSONObject
    suspend fun getStreakInfo(profileId: Long): JSONObject
    suspend fun getProfileAnalytics(profileId: Long): JSONObject

    // Glossary
    suspend fun searchGlossary(query: String): List<JSONObject>
    suspend fun getGlossaryTerm(termId: Long): JSONObject?
    suspend fun listGlossaryCategories(): List<String>

    // Controlled Writes
    suspend fun createTask(profileId: Long, title: String, priority: TaskPriority, notes: String?, dueDate: String?, projectId: Long? = null): TaskItem
    suspend fun updateTask(profileId: Long, taskId: Long, title: String?, priority: TaskPriority?, notes: String?, dueDate: String?): TaskItem?
    suspend fun completeTask(profileId: Long, taskId: Long, isCompleted: Boolean): Boolean
    suspend fun createProject(profileId: Long, name: String, description: String?, colorHex: Long?): Long
    suspend fun updateProjectStatus(profileId: Long, projectId: Long, status: String): Boolean
    suspend fun addTaskLabel(profileId: Long, taskId: Long, labelName: String): Boolean

    // Profile metadata & Audit
    suspend fun getProfileName(profileId: Long): String
    suspend fun getHabits(profileId: Long): List<HabitItem>
    suspend fun recordAuditEvent(event: McpAuditEvent)
}

/**
 * Core bidirectional Model Context Protocol (MCP) protocol processor (#3).
 * Operates strictly on-device / local IPC — zero network telemetry.
 */
class McpProtocolEngine(
    private val dataProvider: McpDataProvider
) {

    val supportedTools: List<McpToolDefinition> = listOf(
        // --- Tasks (Read-only) ---
        McpToolDefinition(
            name = "get_tasks",
            description = "Retrieve tasks for the authorized profile, optionally filtered by status and priority.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "properties": {
                        "status": { "type": "string", "enum": ["ALL", "PENDING", "COMPLETED"], "description": "Filter by completion status" },
                        "priority": { "type": "string", "enum": ["ALL", "HIGH", "MEDIUM", "LOW"], "description": "Filter by task priority" }
                    }
                }
            """.trimIndent())
        ),
        McpToolDefinition(
            name = "get_task_details",
            description = "Retrieve detailed information for a specific task ID within the authorized profile.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "required": ["taskId"],
                    "properties": {
                        "taskId": { "type": "integer", "description": "Unique task ID" }
                    }
                }
            """.trimIndent())
        ),
        McpToolDefinition(
            name = "search_tasks",
            description = "Search task titles, notes, and labels matching a query string.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "required": ["query"],
                    "properties": {
                        "query": { "type": "string", "description": "Search keyword" }
                    }
                }
            """.trimIndent())
        ),
        McpToolDefinition(
            name = "get_overdue_tasks",
            description = "Retrieve all uncompleted tasks whose due dates are in the past.",
            inputSchema = JSONObject("""{ "type": "object", "properties": {} }""")
        ),
        McpToolDefinition(
            name = "get_completed_tasks",
            description = "Retrieve recently completed tasks for retro analysis.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "properties": {
                        "limit": { "type": "integer", "description": "Max number of tasks to return (default 20)" }
                    }
                }
            """.trimIndent())
        ),

        // --- Projects (Read-only) ---
        McpToolDefinition(
            name = "list_projects",
            description = "List all workspace projects within the authorized profile.",
            inputSchema = JSONObject("""{ "type": "object", "properties": {} }""")
        ),
        McpToolDefinition(
            name = "get_project_details",
            description = "Retrieve full details and items count for a specific project.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "required": ["projectId"],
                    "properties": {
                        "projectId": { "type": "integer", "description": "Unique project ID" }
                    }
                }
            """.trimIndent())
        ),
        McpToolDefinition(
            name = "get_project_status_and_progress",
            description = "Retrieve status distribution and percentage completion progress for a project.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "required": ["projectId"],
                    "properties": {
                        "projectId": { "type": "integer", "description": "Unique project ID" }
                    }
                }
            """.trimIndent())
        ),
        McpToolDefinition(
            name = "search_projects",
            description = "Search project names and descriptions.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "required": ["query"],
                    "properties": {
                        "query": { "type": "string", "description": "Search keyword" }
                    }
                }
            """.trimIndent())
        ),

        // --- Planning (Read-only) ---
        McpToolDefinition(
            name = "get_daily_planning",
            description = "Retrieve daily execution plan, scheduled time blocks, and target habits.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "properties": {
                        "dateIso": { "type": "string", "description": "ISO date YYYY-MM-DD (defaults to today)" }
                    }
                }
            """.trimIndent())
        ),
        McpToolDefinition(
            name = "get_weekly_planning",
            description = "Retrieve weekly goals, active milestones, and workload distribution.",
            inputSchema = JSONObject("""{ "type": "object", "properties": {} }""")
        ),
        McpToolDefinition(
            name = "get_monthly_planning",
            description = "Retrieve monthly planning objectives and key milestones.",
            inputSchema = JSONObject("""{ "type": "object", "properties": {} }""")
        ),
        McpToolDefinition(
            name = "get_yearly_planning",
            description = "Retrieve high-level yearly visions, themes, and macro goals.",
            inputSchema = JSONObject("""{ "type": "object", "properties": {} }""")
        ),

        // --- Productivity (Read-only) ---
        McpToolDefinition(
            name = "get_productivity_metrics",
            description = "Retrieve 5-pillar productivity radar scores (Focus, Consistency, Wellness, Vision, Discipline).",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "properties": {
                        "horizon": { "type": "string", "enum": ["DAILY", "WEEKLY", "MONTHLY", "YEARLY"], "description": "Time horizon (default WEEKLY)" }
                    }
                }
            """.trimIndent())
        ),
        McpToolDefinition(
            name = "get_streak_info",
            description = "Retrieve active daily habit streaks and challenge progressions.",
            inputSchema = JSONObject("""{ "type": "object", "properties": {} }""")
        ),
        McpToolDefinition(
            name = "get_profile_analytics",
            description = "Retrieve profile workload risk, completion rates, and cognitive balance.",
            inputSchema = JSONObject("""{ "type": "object", "properties": {} }""")
        ),

        // --- Glossary (Read-only) ---
        McpToolDefinition(
            name = "search_glossary",
            description = "Search mental models, productivity definitions, and verified knowledge packs.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "required": ["query"],
                    "properties": {
                        "query": { "type": "string", "description": "Search keyword or tag" }
                    }
                }
            """.trimIndent())
        ),
        McpToolDefinition(
            name = "get_glossary_term",
            description = "Retrieve definition, deep-dive explanation, and practical examples for a term ID.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "required": ["termId"],
                    "properties": {
                        "termId": { "type": "integer", "description": "Term ID" }
                    }
                }
            """.trimIndent())
        ),
        McpToolDefinition(
            name = "list_glossary_categories",
            description = "List all available knowledge categories in the offline glossary.",
            inputSchema = JSONObject("""{ "type": "object", "properties": {} }""")
        ),

        // --- Controlled Write Operations ---
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
                        "dueDate": { "type": "string", "description": "Optional ISO-8601 due date YYYY-MM-DD" },
                        "projectId": { "type": "integer", "description": "Optional associated project ID" }
                    }
                }
            """.trimIndent())
        ),
        McpToolDefinition(
            name = "update_task",
            description = "Update properties of an existing task within the authorized profile.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "required": ["taskId"],
                    "properties": {
                        "taskId": { "type": "integer", "description": "Task ID to update" },
                        "title": { "type": "string", "description": "New title" },
                        "priority": { "type": "string", "enum": ["HIGH", "MEDIUM", "LOW"], "description": "New priority" },
                        "notes": { "type": "string", "description": "New notes" },
                        "dueDate": { "type": "string", "description": "New ISO due date" }
                    }
                }
            """.trimIndent())
        ),
        McpToolDefinition(
            name = "complete_task",
            description = "Mark a task as completed or uncompleted within the authorized profile.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "required": ["taskId", "isCompleted"],
                    "properties": {
                        "taskId": { "type": "integer", "description": "Task ID" },
                        "isCompleted": { "type": "boolean", "description": "True to mark completed, false to uncomplete" }
                    }
                }
            """.trimIndent())
        ),
        McpToolDefinition(
            name = "create_project",
            description = "Create a new project workspace within the authorized profile.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "required": ["name"],
                    "properties": {
                        "name": { "type": "string", "description": "Project title" },
                        "description": { "type": "string", "description": "Project description" },
                        "colorHex": { "type": "integer", "description": "Color hex code" }
                    }
                }
            """.trimIndent())
        ),
        McpToolDefinition(
            name = "update_project_status",
            description = "Update the status of a project workspace.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "required": ["projectId", "status"],
                    "properties": {
                        "projectId": { "type": "integer", "description": "Project ID" },
                        "status": { "type": "string", "description": "Status name (e.g. In Progress, Planning, Completed)" }
                    }
                }
            """.trimIndent())
        ),
        McpToolDefinition(
            name = "add_task_label",
            description = "Attach an organization label to a task.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "required": ["taskId", "labelName"],
                    "properties": {
                        "taskId": { "type": "integer", "description": "Task ID" },
                        "labelName": { "type": "string", "description": "Label title" }
                    }
                }
            """.trimIndent())
        ),

        // --- Heuristics & Context ---
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
            uri = "lifeos://tasks/overdue",
            name = "Overdue Tasks Queue",
            description = "Tasks whose deadline has passed."
        ),
        McpResourceDefinition(
            uri = "lifeos://projects/overview",
            name = "Projects Overview",
            description = "Overview of active workspace projects."
        ),
        McpResourceDefinition(
            uri = "lifeos://productivity/radar",
            name = "Productivity Radar",
            description = "Current 5-axis productivity intelligence polygon scores."
        ),
        McpResourceDefinition(
            uri = "lifeos://glossary/categories",
            name = "Knowledge Categories",
            description = "List of mental models and knowledge library categories."
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
        ),
        McpPromptDefinition(
            name = "project_architecture_review",
            description = "Conduct a pre-mortem and architectural risk assessment for a project.",
            arguments = listOf(
                McpPromptArgument("projectName", "Name of the project to review", required = true)
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
                dataProvider.recordAuditEvent(
                    McpAuditEvent(
                        profileId = authorizedProfileId,
                        direction = "INBOUND_SERVER",
                        eventType = "AUTH",
                        toolOrResource = request.method,
                        details = "Authentication failed: invalid or missing Bearer token",
                        status = "DENIED"
                    )
                )
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
        serverInfo.put("name", "LifeOS-MCP-Server")
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

        if (arguments.has("profileId")) {
            val reqProfileId = arguments.getLong("profileId")
            if (!McpSecurityManager.validateProfileAccess(reqProfileId, profileId)) {
                dataProvider.recordAuditEvent(
                    McpAuditEvent(
                        profileId = profileId,
                        direction = "INBOUND_SERVER",
                        eventType = "AUTH",
                        toolOrResource = toolName,
                        details = "Cross-profile boundary access attempt rejected for profile #$reqProfileId",
                        status = "DENIED"
                    )
                )
                return McpResponse(
                    id = request.id,
                    error = McpError(
                        McpError.PROFILE_ISOLATION_VIOLATION,
                        "Profile isolation violation: Cannot access profile #$reqProfileId from authorized profile #$profileId."
                    )
                )
            }
        }

        return try {
            val contentText = when (toolName) {
                // --- Task Read Tools ---
                "get_tasks" -> {
                    val status = arguments.optString("status", "ALL")
                    val priority = arguments.optString("priority", "ALL")
                    val tasks = dataProvider.getTasks(profileId, status, priority)
                    val tasksArr = JSONArray()
                    tasks.forEach { t ->
                        tasksArr.put(taskToJson(t))
                    }
                    JSONObject().put("tasks", tasksArr).put("count", tasks.size).toString(2)
                }
                "get_task_details" -> {
                    val taskId = arguments.getLong("taskId")
                    val task = dataProvider.getTaskDetails(profileId, taskId)
                        ?: throw NoSuchElementException("Task #$taskId not found in profile #$profileId")
                    taskToJson(task).toString(2)
                }
                "search_tasks" -> {
                    val query = arguments.getString("query")
                    val tasks = dataProvider.searchTasks(profileId, query)
                    val arr = JSONArray()
                    tasks.forEach { arr.put(taskToJson(it)) }
                    JSONObject().put("results", arr).put("count", tasks.size).toString(2)
                }
                "get_overdue_tasks" -> {
                    val tasks = dataProvider.getOverdueTasks(profileId)
                    val arr = JSONArray()
                    tasks.forEach { arr.put(taskToJson(it)) }
                    JSONObject().put("overdueTasks", arr).put("count", tasks.size).toString(2)
                }
                "get_completed_tasks" -> {
                    val limit = arguments.optInt("limit", 20)
                    val tasks = dataProvider.getCompletedTasks(profileId, limit)
                    val arr = JSONArray()
                    tasks.forEach { arr.put(taskToJson(it)) }
                    JSONObject().put("completedTasks", arr).put("count", tasks.size).toString(2)
                }

                // --- Project Read Tools ---
                "list_projects" -> {
                    val projects = dataProvider.getProjects(profileId)
                    val arr = JSONArray()
                    projects.forEach { (id, name) ->
                        arr.put(JSONObject().put("id", id).put("name", name))
                    }
                    JSONObject().put("projects", arr).put("count", projects.size).toString(2)
                }
                "get_project_details" -> {
                    val projectId = arguments.getLong("projectId")
                    val details = dataProvider.getProjectDetails(profileId, projectId)
                        ?: throw NoSuchElementException("Project #$projectId not found in profile #$profileId")
                    details.toString(2)
                }
                "get_project_status_and_progress" -> {
                    val projectId = arguments.getLong("projectId")
                    val progress = dataProvider.getProjectStatusAndProgress(profileId, projectId)
                        ?: throw NoSuchElementException("Project #$projectId not found in profile #$profileId")
                    progress.toString(2)
                }
                "search_projects" -> {
                    val query = arguments.getString("query")
                    val results = dataProvider.searchProjects(profileId, query)
                    JSONObject().put("projects", JSONArray(results)).put("count", results.size).toString(2)
                }

                // --- Planning Read Tools ---
                "get_daily_planning" -> {
                    val dateIso = if (arguments.has("dateIso")) arguments.getString("dateIso") else null
                    dataProvider.getDailyPlanning(profileId, dateIso).toString(2)
                }
                "get_weekly_planning" -> {
                    dataProvider.getWeeklyPlanning(profileId).toString(2)
                }
                "get_monthly_planning" -> {
                    dataProvider.getMonthlyPlanning(profileId).toString(2)
                }
                "get_yearly_planning" -> {
                    dataProvider.getYearlyPlanning(profileId).toString(2)
                }

                // --- Productivity Metrics ---
                "get_productivity_metrics" -> {
                    val horizon = arguments.optString("horizon", "WEEKLY")
                    dataProvider.getProductivityMetrics(profileId, horizon).toString(2)
                }
                "get_streak_info" -> {
                    dataProvider.getStreakInfo(profileId).toString(2)
                }
                "get_profile_analytics" -> {
                    dataProvider.getProfileAnalytics(profileId).toString(2)
                }

                // --- Glossary Tools ---
                "search_glossary" -> {
                    val query = arguments.getString("query")
                    val results = dataProvider.searchGlossary(query)
                    JSONObject().put("terms", JSONArray(results)).put("count", results.size).toString(2)
                }
                "get_glossary_term" -> {
                    val termId = arguments.getLong("termId")
                    val term = dataProvider.getGlossaryTerm(termId)
                        ?: throw NoSuchElementException("Glossary term #$termId not found")
                    term.toString(2)
                }
                "list_glossary_categories" -> {
                    val categories = dataProvider.listGlossaryCategories()
                    JSONObject().put("categories", JSONArray(categories)).toString(2)
                }

                // --- Controlled Write Tools ---
                "create_task" -> {
                    val title = arguments.getString("title").trim()
                    if (title.isBlank()) throw IllegalArgumentException("Task title cannot be blank.")
                    val priorityStr = arguments.optString("priority", "MEDIUM")
                    val priority = try { TaskPriority.valueOf(priorityStr.uppercase()) } catch (_: Exception) { TaskPriority.MEDIUM }
                    val notes = if (arguments.has("notes") && !arguments.isNull("notes")) arguments.getString("notes") else null
                    val dueDate = if (arguments.has("dueDate") && !arguments.isNull("dueDate")) arguments.getString("dueDate") else null
                    val projectId = if (arguments.has("projectId") && !arguments.isNull("projectId")) arguments.getLong("projectId") else null

                    val created = dataProvider.createTask(profileId, title, priority, notes, dueDate, projectId)
                    dataProvider.recordAuditEvent(
                        McpAuditEvent(
                            profileId = profileId,
                            direction = "INBOUND_SERVER",
                            eventType = "WRITE_OP",
                            toolOrResource = "create_task",
                            details = "Created task #${created.id} '${created.title}'",
                            status = "SUCCESS"
                        )
                    )
                    JSONObject()
                        .put("success", true)
                        .put("id", created.id)
                        .put("title", created.title)
                        .put("profileId", profileId)
                        .toString(2)
                }
                "update_task" -> {
                    val taskId = arguments.getLong("taskId")
                    val title = if (arguments.has("title")) arguments.getString("title") else null
                    val priority = if (arguments.has("priority")) {
                        try { TaskPriority.valueOf(arguments.getString("priority").uppercase()) } catch (_: Exception) { null }
                    } else null
                    val notes = if (arguments.has("notes")) arguments.getString("notes") else null
                    val dueDate = if (arguments.has("dueDate")) arguments.getString("dueDate") else null

                    val updated = dataProvider.updateTask(profileId, taskId, title, priority, notes, dueDate)
                        ?: throw NoSuchElementException("Task #$taskId not found in profile #$profileId")

                    dataProvider.recordAuditEvent(
                        McpAuditEvent(
                            profileId = profileId,
                            direction = "INBOUND_SERVER",
                            eventType = "WRITE_OP",
                            toolOrResource = "update_task",
                            details = "Updated task #$taskId",
                            status = "SUCCESS"
                        )
                    )
                    JSONObject().put("success", true).put("task", taskToJson(updated)).toString(2)
                }
                "complete_task" -> {
                    val taskId = arguments.getLong("taskId")
                    val isCompleted = arguments.getBoolean("isCompleted")
                    val success = dataProvider.completeTask(profileId, taskId, isCompleted)
                    if (!success) throw NoSuchElementException("Task #$taskId not found in profile #$profileId")

                    dataProvider.recordAuditEvent(
                        McpAuditEvent(
                            profileId = profileId,
                            direction = "INBOUND_SERVER",
                            eventType = "WRITE_OP",
                            toolOrResource = "complete_task",
                            details = "Marked task #$taskId isCompleted=$isCompleted",
                            status = "SUCCESS"
                        )
                    )
                    JSONObject().put("success", true).put("taskId", taskId).put("isCompleted", isCompleted).toString(2)
                }
                "create_project" -> {
                    val name = arguments.getString("name").trim()
                    if (name.isBlank()) throw IllegalArgumentException("Project name cannot be blank.")
                    val description = arguments.optString("description", "")
                    val colorHex = if (arguments.has("colorHex")) arguments.getLong("colorHex") else null

                    val id = dataProvider.createProject(profileId, name, description, colorHex)
                    dataProvider.recordAuditEvent(
                        McpAuditEvent(
                            profileId = profileId,
                            direction = "INBOUND_SERVER",
                            eventType = "WRITE_OP",
                            toolOrResource = "create_project",
                            details = "Created project #$id '$name'",
                            status = "SUCCESS"
                        )
                    )
                    JSONObject().put("success", true).put("projectId", id).put("name", name).toString(2)
                }
                "update_project_status" -> {
                    val projectId = arguments.getLong("projectId")
                    val status = arguments.getString("status")
                    val success = dataProvider.updateProjectStatus(profileId, projectId, status)
                    if (!success) throw NoSuchElementException("Project #$projectId not found in profile #$profileId")

                    dataProvider.recordAuditEvent(
                        McpAuditEvent(
                            profileId = profileId,
                            direction = "INBOUND_SERVER",
                            eventType = "WRITE_OP",
                            toolOrResource = "update_project_status",
                            details = "Updated project #$projectId status to '$status'",
                            status = "SUCCESS"
                        )
                    )
                    JSONObject().put("success", true).put("projectId", projectId).put("status", status).toString(2)
                }
                "add_task_label" -> {
                    val taskId = arguments.getLong("taskId")
                    val labelName = arguments.getString("labelName")
                    val success = dataProvider.addTaskLabel(profileId, taskId, labelName)
                    if (!success) throw NoSuchElementException("Task #$taskId not found in profile #$profileId")

                    dataProvider.recordAuditEvent(
                        McpAuditEvent(
                            profileId = profileId,
                            direction = "INBOUND_SERVER",
                            eventType = "WRITE_OP",
                            toolOrResource = "add_task_label",
                            details = "Added label '$labelName' to task #$taskId",
                            status = "SUCCESS"
                        )
                    )
                    JSONObject().put("success", true).put("taskId", taskId).put("label", labelName).toString(2)
                }

                // --- Heuristics & Context ---
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

            dataProvider.recordAuditEvent(
                McpAuditEvent(
                    profileId = profileId,
                    direction = "INBOUND_SERVER",
                    eventType = "TOOL_CALL",
                    toolOrResource = toolName,
                    details = "Tool executed successfully",
                    status = "SUCCESS"
                )
            )

            val contentArr = JSONArray()
            val textItem = JSONObject().put("type", "text").put("text", contentText)
            contentArr.put(textItem)

            val result = JSONObject().put("content", contentArr).put("isError", false)
            McpResponse(id = request.id, result = result)
        } catch (e: Exception) {
            dataProvider.recordAuditEvent(
                McpAuditEvent(
                    profileId = profileId,
                    direction = "INBOUND_SERVER",
                    eventType = "TOOL_CALL",
                    toolOrResource = toolName,
                    details = "Tool execution failed: ${e.message}",
                    status = "FAILED"
                )
            )

            val contentArr = JSONArray()
            val textItem = JSONObject().put("type", "text").put("text", "Error executing tool '$toolName': ${e.message}")
            contentArr.put(textItem)

            val result = JSONObject().put("content", contentArr).put("isError", true)
            McpResponse(id = request.id, result = result)
        }
    }

    private fun taskToJson(t: TaskItem): JSONObject {
        return JSONObject().apply {
            put("id", t.id)
            put("title", t.title)
            put("priority", t.priority.name)
            put("isCompleted", t.isOneTimeCompleted)
            put("dueDate", t.dueDateIso ?: JSONObject.NULL)
            put("notes", t.notes ?: JSONObject.NULL)
            put("projectId", t.projectId ?: JSONObject.NULL)
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
                tasks.joinToString("\n") { "- [${it.priority}] #${it.id} ${it.title} (Due: ${it.dueDateIso ?: "None"})" }
            }
            "lifeos://tasks/overdue" -> {
                val tasks = dataProvider.getOverdueTasks(profileId)
                tasks.joinToString("\n") { "! [${it.priority}] #${it.id} ${it.title} (Overdue: ${it.dueDateIso ?: "Past"})" }
            }
            "lifeos://projects/overview" -> {
                val projects = dataProvider.getProjects(profileId)
                projects.joinToString("\n") { "• #${it.first}: ${it.second}" }
            }
            "lifeos://productivity/radar" -> {
                dataProvider.getProductivityMetrics(profileId, "WEEKLY").toString(2)
            }
            "lifeos://glossary/categories" -> {
                dataProvider.listGlossaryCategories().joinToString("\n") { "📚 $it" }
            }
            "lifeos://intelligence/suggestions" -> {
                val tasks = dataProvider.getTasks(profileId, "ALL", "ALL")
                val habits = dataProvider.getHabits(profileId)
                val analysis = IntelligentPlanningEngine.analyzeWorkload(tasks, habits)
                analysis.suggestions.joinToString("\n\n") { "• ${it.title}:\n  ${it.description}\n  -> ${it.actionRecommendation}" }
            }
            else -> return McpResponse(id = request.id, error = McpError(McpError.INVALID_PARAMS, "Resource '$uri' not found."))
        }

        dataProvider.recordAuditEvent(
            McpAuditEvent(
                profileId = profileId,
                direction = "INBOUND_SERVER",
                eventType = "RESOURCE_READ",
                toolOrResource = uri,
                details = "Resource accessed",
                status = "SUCCESS"
            )
        )

        val contentsArr = JSONArray()
        val item = JSONObject()
        item.put("uri", uri)
        item.put("mimeType", "text/plain")
        item.put("text", text)
        contentsArr.put(item)

        val result = JSONObject().put("contents", contentsArr)
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
            "project_architecture_review" -> {
                val pName = arguments.optString("projectName", "New System")
                """
                    Conduct an offline architecture risk and Chesterton's Fence assessment for:
                    Project: $pName
                    
                    Review:
                    1. State isolation and Room migration dependencies.
                    2. 100% offline boundaries (zero unexpected network calls).
                    3. Failure cascades and rollback mechanisms.
                """.trimIndent()
            }
            else -> return McpResponse(id = request.id, error = McpError(McpError.INVALID_PARAMS, "Prompt '$promptName' not found."))
        }

        dataProvider.recordAuditEvent(
            McpAuditEvent(
                profileId = profileId,
                direction = "INBOUND_SERVER",
                eventType = "PROMPT_GET",
                toolOrResource = promptName,
                details = "Prompt template rendered",
                status = "SUCCESS"
            )
        )

        val messagesArr = JSONArray()
        val msg = JSONObject()
        msg.put("role", "user")
        val content = JSONObject().put("type", "text").put("text", promptText)
        msg.put("content", content)
        messagesArr.put(msg)

        val result = JSONObject().put("messages", messagesArr).put("description", "Generated prompt for $promptName")
        return McpResponse(id = request.id, result = result)
    }
}
