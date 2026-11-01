package com.example

import com.example.data.local.dao.McpDao
import com.example.data.local.entities.McpAuditLogEntity
import com.example.data.local.entities.McpServerEntity
import com.example.domain.intelligence.IntelligentPlanningEngine
import com.example.domain.intelligence.OverloadRiskLevel
import com.example.domain.intelligence.SuggestionType
import com.example.domain.model.HabitItem
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.mcp.McpAuditEvent
import com.example.mcp.McpConnectionStatus
import com.example.mcp.McpDataProvider
import com.example.mcp.McpError
import com.example.mcp.McpProtocolEngine
import com.example.mcp.McpRequest
import com.example.mcp.McpResponse
import com.example.mcp.McpSecurityManager
import com.example.mcp.McpTransportType
import com.example.mcp.client.LocalIpcMcpTransport
import com.example.mcp.client.McpClientEngine
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class Release3Phase9McpUnitTest {

    private lateinit var mockDataProvider: MockMcpDataProvider
    private lateinit var mcpEngine: McpProtocolEngine
    private lateinit var clientEngine: McpClientEngine
    private lateinit var fakeMcpDao: FakeMcpDao
    private val validToken = "0123456789abcdef0123456789abcdef"
    private val authProfileId = 1L

    class FakeMcpDao : McpDao {
        val servers = mutableListOf<McpServerEntity>()
        val logs = mutableListOf<McpAuditLogEntity>()

        override fun getAllServersFlow(profileId: Long): kotlinx.coroutines.flow.Flow<List<McpServerEntity>> =
            kotlinx.coroutines.flow.flowOf(servers)
        override suspend fun getAllServersList(profileId: Long): List<McpServerEntity> = servers
        override suspend fun getServerById(id: Long): McpServerEntity? = servers.find { it.id == id }
        override suspend fun insertServer(server: McpServerEntity): Long {
            servers.add(server)
            return server.id
        }
        override suspend fun updateServer(server: McpServerEntity) {}
        override suspend fun deleteServer(id: Long) { servers.removeAll { it.id == id } }
        override suspend fun deleteServersForProfile(profileId: Long): Int {
            val count = servers.count { it.profileId == profileId }
            servers.removeAll { it.profileId == profileId }
            return count
        }
        override suspend fun updateServerStatus(id: Long, status: String, errorMessage: String?, lastConnectedAt: Long?, toolsCount: Int) {
            val idx = servers.indexOfFirst { it.id == id }
            if (idx != -1) {
                servers[idx] = servers[idx].copy(status = status, errorMessage = errorMessage, lastConnectedAt = lastConnectedAt, discoveredToolsCount = toolsCount)
            }
        }
        override fun getAuditLogsFlow(profileId: Long, limit: Int): kotlinx.coroutines.flow.Flow<List<McpAuditLogEntity>> =
            kotlinx.coroutines.flow.flowOf(logs)
        override suspend fun getAuditLogsList(profileId: Long, limit: Int): List<McpAuditLogEntity> = logs
        override suspend fun insertAuditLog(log: McpAuditLogEntity): Long {
            logs.add(log)
            return log.id
        }
        override suspend fun clearAuditLogs(profileId: Long) {
            logs.removeAll { it.profileId == profileId }
        }
    }

    class MockMcpDataProvider : McpDataProvider {
        val tasks = mutableListOf<TaskItem>()
        val habits = mutableListOf<HabitItem>()
        val projects = mutableListOf<Pair<Long, String>>()
        val projectDetails = mutableMapOf<Long, JSONObject>()
        val auditEvents = mutableListOf<McpAuditEvent>()
        var profileName = "Personal Profile"

        override suspend fun getTasks(profileId: Long, status: String?, priority: String?): List<TaskItem> {
            return tasks.filter { it.profileId == profileId }
                .filter { task ->
                    val statusMatch = when (status?.uppercase()) {
                        "COMPLETED" -> task.isOneTimeCompleted
                        "PENDING" -> !task.isOneTimeCompleted
                        else -> true
                    }
                    val priorityMatch = when (priority?.uppercase()) {
                        "HIGH", "MEDIUM", "LOW" -> task.priority.name.equals(priority, ignoreCase = true)
                        else -> true
                    }
                    statusMatch && priorityMatch
                }
        }

        override suspend fun getTaskDetails(profileId: Long, taskId: Long): TaskItem? {
            return tasks.find { it.id == taskId && it.profileId == profileId }
        }

        override suspend fun searchTasks(profileId: Long, query: String): List<TaskItem> {
            val q = query.lowercase()
            return tasks.filter { it.profileId == profileId && (it.title.lowercase().contains(q) || it.notes.lowercase().contains(q)) }
        }

        override suspend fun getOverdueTasks(profileId: Long): List<TaskItem> {
            val today = LocalDate.now().toString()
            return tasks.filter { it.profileId == profileId && !it.isOneTimeCompleted && it.dueDateIso < today }
        }

        override suspend fun getCompletedTasks(profileId: Long, limit: Int): List<TaskItem> {
            return tasks.filter { it.profileId == profileId && it.isOneTimeCompleted }.take(limit)
        }

        override suspend fun getProjects(profileId: Long): List<Pair<Long, String>> {
            return projects
        }

        override suspend fun getProjectDetails(profileId: Long, projectId: Long): JSONObject? {
            return projectDetails[projectId]
        }

        override suspend fun getProjectStatusAndProgress(profileId: Long, projectId: Long): JSONObject? {
            val p = projects.find { it.first == projectId } ?: return null
            return JSONObject().apply {
                put("projectId", projectId)
                put("name", p.second)
                put("status", "ACTIVE")
                put("progressPercent", 65)
            }
        }

        override suspend fun searchProjects(profileId: Long, query: String): List<JSONObject> {
            val q = query.lowercase()
            return projects.filter { it.second.lowercase().contains(q) }.map {
                JSONObject().put("id", it.first).put("name", it.second)
            }
        }

        override suspend fun getDailyPlanning(profileId: Long, dateIso: String?): JSONObject {
            return JSONObject().apply {
                put("targetDate", dateIso ?: LocalDate.now().toString())
                put("tasksCount", tasks.size)
                put("completedTasks", tasks.count { it.isOneTimeCompleted })
                put("habitsCount", habits.size)
                put("completedHabits", habits.count { it.isCompletedToday })
            }
        }

        override suspend fun getWeeklyPlanning(profileId: Long): JSONObject {
            return JSONObject().apply {
                put("weeklyPlansCount", 2)
                put("plans", JSONArray(listOf("Sprint Goal 1", "Sprint Goal 2")))
            }
        }

        override suspend fun getMonthlyPlanning(profileId: Long): JSONObject {
            return JSONObject().apply {
                put("monthlyPlansCount", 1)
                put("plans", JSONArray(listOf("Release 3 Production Launch")))
            }
        }

        override suspend fun getYearlyPlanning(profileId: Long): JSONObject {
            return JSONObject().apply {
                put("yearlyPlansCount", 1)
                put("plans", JSONArray(listOf("Build Sovereign OS Ecosystem")))
            }
        }

        override suspend fun getProductivityMetrics(profileId: Long, horizon: String): JSONObject {
            return JSONObject().apply {
                put("horizon", horizon)
                put("focusScore", 85)
                put("consistencyScore", 90)
                put("wellnessScore", 80)
                put("visionScore", 75)
                put("disciplineScore", 88)
                put("overallScore", 84)
            }
        }

        override suspend fun getStreakInfo(profileId: Long): JSONObject {
            val max = habits.maxOfOrNull { it.currentStreak } ?: 0
            return JSONObject().apply {
                put("maxStreak", max)
                put("habitsCount", habits.size)
                put("habitStreaks", JSONObject())
            }
        }

        override suspend fun getProfileAnalytics(profileId: Long): JSONObject {
            return JSONObject().apply {
                put("overloadScore", 20)
                put("riskLevel", "LOW")
                put("pendingCount", tasks.count { !it.isOneTimeCompleted })
                put("overdueCount", 0)
            }
        }

        override suspend fun searchGlossary(query: String): List<JSONObject> {
            val q = query.lowercase()
            val terms = listOf(
                JSONObject().put("id", 1L).put("term", "Timeboxing").put("category", "Productivity").put("definition", "Allocating fixed time periods to tasks.")
            )
            return terms.filter { it.getString("term").lowercase().contains(q) }
        }

        override suspend fun getGlossaryTerm(termId: Long): JSONObject? {
            return if (termId == 1L) {
                JSONObject().apply {
                    put("id", 1L)
                    put("term", "Timeboxing")
                    put("category", "Productivity")
                    put("definition", "Allocating fixed time periods to tasks.")
                    put("detailedExplanation", "Deep dive into timeboxing technique.")
                    put("examples", JSONArray())
                    put("relatedTerms", JSONArray())
                }
            } else null
        }

        override suspend fun listGlossaryCategories(): List<String> {
            return listOf("Productivity", "Mindfulness", "Engineering")
        }

        override suspend fun createTask(
            profileId: Long,
            title: String,
            priority: TaskPriority,
            notes: String?,
            dueDate: String?,
            projectId: Long?
        ): TaskItem {
            val task = TaskItem(
                id = (tasks.size + 1).toLong(),
                profileId = profileId,
                title = title,
                priority = priority,
                notes = notes ?: "",
                dueDateIso = dueDate ?: LocalDate.now().toString(),
                projectId = projectId
            )
            tasks.add(task)
            return task
        }

        override suspend fun updateTask(
            profileId: Long,
            taskId: Long,
            title: String?,
            priority: TaskPriority?,
            notes: String?,
            dueDate: String?
        ): TaskItem? {
            val index = tasks.indexOfFirst { it.id == taskId && it.profileId == profileId }
            if (index == -1) return null
            val existing = tasks[index]
            val updated = existing.copy(
                title = title ?: existing.title,
                priority = priority ?: existing.priority,
                notes = notes ?: existing.notes,
                dueDateIso = dueDate ?: existing.dueDateIso
            )
            tasks[index] = updated
            return updated
        }

        override suspend fun completeTask(profileId: Long, taskId: Long, isCompleted: Boolean): Boolean {
            val index = tasks.indexOfFirst { it.id == taskId && it.profileId == profileId }
            if (index == -1) return false
            tasks[index] = tasks[index].copy(isOneTimeCompleted = isCompleted)
            return true
        }

        override suspend fun createProject(profileId: Long, name: String, description: String?, colorHex: Long?): Long {
            val id = (projects.size + 1).toLong()
            projects.add(Pair(id, name))
            projectDetails[id] = JSONObject().put("id", id).put("name", name).put("description", description ?: "")
            return id
        }

        override suspend fun updateProjectStatus(profileId: Long, projectId: Long, status: String): Boolean {
            return projects.any { it.first == projectId }
        }

        override suspend fun addTaskLabel(profileId: Long, taskId: Long, labelName: String): Boolean {
            return tasks.any { it.id == taskId && it.profileId == profileId }
        }

        override suspend fun getHabits(profileId: Long): List<HabitItem> = habits.filter { it.profileId == profileId }
        override suspend fun getProfileName(profileId: Long): String = profileName
        override suspend fun recordAuditEvent(event: McpAuditEvent) {
            auditEvents.add(event)
        }
    }

    @Before
    fun setup() {
        mockDataProvider = MockMcpDataProvider()
        mcpEngine = McpProtocolEngine(mockDataProvider)
        fakeMcpDao = FakeMcpDao()
        clientEngine = McpClientEngine(fakeMcpDao)
    }

    @Test
    fun securityManager_generatesAndValidatesToken() {
        val token = McpSecurityManager.generateAuthToken()
        assertEquals("Token must be exactly 32 hex characters", 32, token.length)
        assertTrue("Token must be valid hex", token.matches(Regex("^[0-9a-f]{32}$")))

        // Validate token
        assertTrue(McpSecurityManager.validateToken(token, token))
        assertTrue(McpSecurityManager.validateToken("Bearer $token", token))
        assertFalse(McpSecurityManager.validateToken("wrong_token_1234", token))
        assertFalse(McpSecurityManager.validateToken(null, token))
        assertFalse(McpSecurityManager.validateToken("", token))

        // Profile access validation
        assertTrue(McpSecurityManager.validateProfileAccess(1L, 1L))
        assertFalse(McpSecurityManager.validateProfileAccess(2L, 1L))
        assertTrue(McpSecurityManager.validateProfileAccess(null, 1L))
    }

    @Test
    fun protocolEngine_enforcesBridgeEnabledAndAuthentication() = runBlocking {
        val testJson = """{"jsonrpc":"2.0","id":1,"method":"tools/list"}"""

        // 1. When bridge is disabled -> unauthorized
        val disabledResponse = mcpEngine.handleMessage(
            rawJson = testJson,
            providedToken = validToken,
            storedToken = validToken,
            isBridgeEnabled = false,
            authorizedProfileId = authProfileId
        )
        assertNotNull(disabledResponse.error)
        assertEquals(McpError.UNAUTHORIZED, disabledResponse.error!!.code)

        // 2. When bridge is enabled but token is invalid -> unauthorized
        val wrongTokenResponse = mcpEngine.handleMessage(
            rawJson = testJson,
            providedToken = "invalid_token",
            storedToken = validToken,
            isBridgeEnabled = true,
            authorizedProfileId = authProfileId
        )
        assertNotNull(wrongTokenResponse.error)
        assertEquals(McpError.UNAUTHORIZED, wrongTokenResponse.error!!.code)

        // 3. Handshake 'initialize' succeeds even before token presentation (protocol discovery)
        val initJson = """{"jsonrpc":"2.0","id":1,"method":"initialize"}"""
        val initResponse = mcpEngine.handleMessage(
            rawJson = initJson,
            providedToken = null,
            storedToken = validToken,
            isBridgeEnabled = true,
            authorizedProfileId = authProfileId
        )
        assertNull(initResponse.error)
        assertNotNull(initResponse.result)
        val resultObj = initResponse.result as JSONObject
        assertEquals("2024-11-05", resultObj.getString("protocolVersion"))
        assertEquals("LifeOS-MCP-Server", resultObj.getJSONObject("serverInfo").getString("name"))
    }

    @Test
    fun protocolEngine_toolsListContains27Tools() = runBlocking {
        val listJson = """{"jsonrpc":"2.0","id":2,"method":"tools/list"}"""
        val listResponse = mcpEngine.handleMessage(listJson, validToken, validToken, true, authProfileId)
        assertNull(listResponse.error)
        val toolsResult = listResponse.result as JSONObject
        val toolsArray = toolsResult.getJSONArray("tools")
        assertEquals(27, toolsArray.length())

        val toolNames = (0 until toolsArray.length()).map { toolsArray.getJSONObject(it).getString("name") }
        // Tasks tools
        assertTrue(toolNames.contains("get_tasks"))
        assertTrue(toolNames.contains("get_task_details"))
        assertTrue(toolNames.contains("search_tasks"))
        assertTrue(toolNames.contains("get_overdue_tasks"))
        assertTrue(toolNames.contains("get_completed_tasks"))
        // Project tools
        assertTrue(toolNames.contains("list_projects"))
        assertTrue(toolNames.contains("get_project_details"))
        assertTrue(toolNames.contains("get_project_status_and_progress"))
        assertTrue(toolNames.contains("search_projects"))
        // Planning tools
        assertTrue(toolNames.contains("get_daily_planning"))
        assertTrue(toolNames.contains("get_weekly_planning"))
        assertTrue(toolNames.contains("get_monthly_planning"))
        assertTrue(toolNames.contains("get_yearly_planning"))
        // Productivity tools
        assertTrue(toolNames.contains("get_productivity_metrics"))
        assertTrue(toolNames.contains("get_streak_info"))
        assertTrue(toolNames.contains("get_intelligent_suggestions"))
        assertTrue(toolNames.contains("get_profile_analytics"))
        // Glossary tools
        assertTrue(toolNames.contains("search_glossary"))
        assertTrue(toolNames.contains("get_glossary_term"))
        assertTrue(toolNames.contains("list_glossary_categories"))
        // Controlled Write tools
        assertTrue(toolNames.contains("create_task"))
        assertTrue(toolNames.contains("update_task"))
        assertTrue(toolNames.contains("complete_task"))
        assertTrue(toolNames.contains("create_project"))
        assertTrue(toolNames.contains("update_project_status"))
        assertTrue(toolNames.contains("add_task_label"))
        assertTrue(toolNames.contains("get_profile_context"))
    }

    @Test
    fun protocolEngine_writeToolsAndProfileIsolation() = runBlocking {
        // 1. Execute create_task write tool
        val createJson = """
            {
                "jsonrpc": "2.0",
                "id": 3,
                "method": "tools/call",
                "params": {
                    "name": "create_task",
                    "arguments": {
                        "title": "Deep Work on Architecture",
                        "priority": "HIGH",
                        "notes": "Refactor protocol engine",
                        "dueDate": "2026-11-02"
                    }
                }
            }
        """.trimIndent()
        val createResponse = mcpEngine.handleMessage(createJson, validToken, validToken, true, authProfileId)
        assertNull(createResponse.error)
        val createResult = createResponse.result as JSONObject
        assertFalse(createResult.getBoolean("isError"))
        assertEquals(1, mockDataProvider.tasks.size)
        assertEquals("Deep Work on Architecture", mockDataProvider.tasks[0].title)
        assertEquals(TaskPriority.HIGH, mockDataProvider.tasks[0].priority)
        assertEquals(authProfileId, mockDataProvider.tasks[0].profileId)

        // 2. Complete task
        val completeJson = """
            {
                "jsonrpc": "2.0",
                "id": 4,
                "method": "tools/call",
                "params": {
                    "name": "complete_task",
                    "arguments": {
                        "taskId": 1,
                        "isCompleted": true
                    }
                }
            }
        """.trimIndent()
        val completeResponse = mcpEngine.handleMessage(completeJson, validToken, validToken, true, authProfileId)
        assertNull(completeResponse.error)
        assertTrue(mockDataProvider.tasks[0].isOneTimeCompleted)

        // 3. Profile isolation enforcement: Request specifying a profile outside authorized profile boundary
        val crossProfileJson = """
            {
                "jsonrpc": "2.0",
                "id": 5,
                "method": "tools/call",
                "params": {
                    "name": "get_tasks",
                    "arguments": {
                        "profileId": 999,
                        "status": "ALL"
                    }
                }
            }
        """.trimIndent()
        val crossProfileResponse = mcpEngine.handleMessage(crossProfileJson, validToken, validToken, true, authProfileId)
        assertNotNull(crossProfileResponse.error)
        assertEquals(McpError.PROFILE_ISOLATION_VIOLATION, crossProfileResponse.error!!.code)
    }

    @Test
    fun protocolEngine_resourcesAndPrompts() = runBlocking {
        // 1. resources/list (7 resources)
        val resListJson = """{"jsonrpc":"2.0","id":10,"method":"resources/list"}"""
        val resListResponse = mcpEngine.handleMessage(resListJson, validToken, validToken, true, authProfileId)
        assertNull(resListResponse.error)
        val resResult = resListResponse.result as JSONObject
        val resources = resResult.getJSONArray("resources")
        assertEquals(7, resources.length())

        // 2. resources/read
        val resReadJson = """
            {
                "jsonrpc": "2.0",
                "id": 11,
                "method": "resources/read",
                "params": {
                    "uri": "lifeos://tasks/pending"
                }
            }
        """.trimIndent()
        val resReadResponse = mcpEngine.handleMessage(resReadJson, validToken, validToken, true, authProfileId)
        assertNull(resReadResponse.error)
        val readResult = resReadResponse.result as JSONObject
        val contents = readResult.getJSONArray("contents")
        assertEquals(1, contents.length())
        assertEquals("lifeos://tasks/pending", contents.getJSONObject(0).getString("uri"))

        // 3. prompts/list (4 prompts)
        val promptListJson = """{"jsonrpc":"2.0","id":12,"method":"prompts/list"}"""
        val promptListResponse = mcpEngine.handleMessage(promptListJson, validToken, validToken, true, authProfileId)
        assertNull(promptListResponse.error)
        val promptResult = promptListResponse.result as JSONObject
        val prompts = promptResult.getJSONArray("prompts")
        assertEquals(4, prompts.length())

        // 4. prompts/get
        val promptGetJson = """
            {
                "jsonrpc": "2.0",
                "id": 13,
                "method": "prompts/get",
                "params": {
                    "name": "task_breakdown_planner",
                    "arguments": {
                        "goal": "Launch LifeOS Release 3"
                    }
                }
            }
        """.trimIndent()
        val promptGetResponse = mcpEngine.handleMessage(promptGetJson, validToken, validToken, true, authProfileId)
        assertNull(promptGetResponse.error)
        val pResult = promptGetResponse.result as JSONObject
        val messages = pResult.getJSONArray("messages")
        assertTrue(messages.getJSONObject(0).getJSONObject("content").getString("text").contains("Launch LifeOS Release 3"))
    }

    @Test
    fun mcpClientEngine_connectsAndDiscoversTools() = runBlocking {
        val server = McpServerEntity(
            id = 1L,
            profileId = 1L,
            name = "Local Sovereign Assistant",
            endpoint = "ipc://assistant",
            transport = "LOCAL_IPC",
            createdAt = System.currentTimeMillis()
        )
        fakeMcpDao.servers.add(server)

        val connectResult = clientEngine.connectServer(server)
        assertTrue("Local IPC connection should succeed", connectResult.isSuccess)

        val toolsResult = clientEngine.discoverTools(server)
        assertTrue("Should discover tools from Local IPC transport", toolsResult.isSuccess)
        val tools = toolsResult.getOrThrow()
        assertEquals(2, tools.size)
        assertEquals("calculate_workload_capacity", tools[0].name)
        assertEquals("format_executive_brief", tools[1].name)
    }

    @Test
    fun mcpClientEngine_enforcesUserSecurityApprovalGate() = runBlocking {
        val server = McpServerEntity(
            id = 1L,
            profileId = 1L,
            name = "Local Sovereign Assistant",
            endpoint = "ipc://assistant",
            transport = "LOCAL_IPC",
            createdAt = System.currentTimeMillis()
        )
        fakeMcpDao.servers.add(server)
        clientEngine.connectServer(server)

        val tools = clientEngine.discoverTools(server).getOrThrow()
        val targetTool = tools.first { it.name == "calculate_workload_capacity" }

        val toolArgs = JSONObject().apply {
            put("pendingTasks", 5)
            put("availableHours", 4.0)
        }

        // 1. Calling external tool WITHOUT explicit user approval -> Failure with SecurityException
        val unapprovedResult = clientEngine.executeTool(
            server = server,
            tool = targetTool,
            arguments = toolArgs,
            userApproved = false
        )
        assertTrue(unapprovedResult.isFailure)
        val exception = unapprovedResult.exceptionOrNull()
        assertTrue(exception is SecurityException)
        assertTrue(exception?.message?.contains("explicit user confirmation") == true)
        assertTrue("Denied audit log must be recorded", fakeMcpDao.logs.any { it.status == "DENIED" })

        // 2. Calling external tool WITH explicit user approval -> Execution succeeds
        val approvedResult = clientEngine.executeTool(
            server = server,
            tool = targetTool,
            arguments = toolArgs,
            userApproved = true
        )
        assertTrue(approvedResult.isSuccess)
        val callResult = approvedResult.getOrThrow()
        assertFalse(callResult.isError)
        assertEquals(1, callResult.content.size)
        assertTrue(callResult.content[0].contains("SUFFICIENT_BANDWIDTH"))
        assertTrue("Success audit log must be recorded", fakeMcpDao.logs.any { it.status == "SUCCESS" })
    }

    @Test
    fun intelligentPlanningEngine_analyzesWorkloadAndDetectsOverload() {
        val today = LocalDate.now().toString()
        val yesterday = LocalDate.now().minusDays(1).toString()

        // Create 6 high priority tasks, 2 overdue
        val tasks = listOf(
            TaskItem(id = 1, title = "Task 1", priority = TaskPriority.HIGH, dueDateIso = yesterday),
            TaskItem(id = 2, title = "Task 2", priority = TaskPriority.HIGH, dueDateIso = yesterday),
            TaskItem(id = 3, title = "Task 3", priority = TaskPriority.HIGH, dueDateIso = today),
            TaskItem(id = 4, title = "Task 4", priority = TaskPriority.HIGH, dueDateIso = today),
            TaskItem(id = 5, title = "Task 5", priority = TaskPriority.HIGH, dueDateIso = today),
            TaskItem(id = 6, title = "Task 6", priority = TaskPriority.HIGH, dueDateIso = today)
        )

        val habits = listOf(
            HabitItem(id = 1, title = "Morning Meditation", currentStreak = 5, isCompletedToday = false),
            HabitItem(id = 2, title = "Hydration", currentStreak = 4, isCompletedToday = true)
        )

        val analysis = IntelligentPlanningEngine.analyzeWorkload(
            tasks = tasks,
            habits = habits,
            todayIso = today,
            lowestRadarDimension = "Wellness"
        )

        assertEquals(6, analysis.totalScheduled)
        assertEquals(6, analysis.highPriorityCount)
        assertEquals(2, analysis.overdueCount)
        assertTrue(analysis.overloadScore >= 50)
        assertTrue(analysis.riskLevel == OverloadRiskLevel.HIGH || analysis.riskLevel == OverloadRiskLevel.CRITICAL)

        // Verify generated suggestions
        val suggTypes = analysis.suggestions.map { it.type }
        assertTrue(suggTypes.contains(SuggestionType.OVERLOAD_WARNING))
        assertTrue(suggTypes.contains(SuggestionType.RESCHEDULE_OVERDUE))
        assertTrue(suggTypes.contains(SuggestionType.HABIT_MOMENTUM))
        assertTrue(suggTypes.contains(SuggestionType.RADAR_BALANCE))
    }
}
