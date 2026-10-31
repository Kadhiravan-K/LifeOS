package com.example

import com.example.domain.intelligence.IntelligentPlanningEngine
import com.example.domain.intelligence.OverloadRiskLevel
import com.example.domain.model.HabitItem
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.mcp.McpDataProvider
import com.example.mcp.McpError
import com.example.mcp.McpProtocolEngine
import com.example.mcp.McpRequest
import com.example.mcp.McpResponse
import com.example.mcp.McpSecurityManager
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class Release3Phase9McpUnitTest {

    private lateinit var mockDataProvider: MockMcpDataProvider
    private lateinit var mcpEngine: McpProtocolEngine
    private val validToken = "0123456789abcdef0123456789abcdef"
    private val authProfileId = 1L

    class MockMcpDataProvider : McpDataProvider {
        val tasks = mutableListOf<TaskItem>()
        val habits = mutableListOf<HabitItem>()
        val projects = mutableListOf<String>()
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

        override suspend fun createTask(
            profileId: Long,
            title: String,
            priority: TaskPriority,
            notes: String?,
            dueDate: String?
        ): TaskItem {
            val task = TaskItem(
                id = (tasks.size + 1).toLong(),
                profileId = profileId,
                title = title,
                priority = priority,
                notes = notes ?: "",
                dueDateIso = dueDate ?: "2026-11-01"
            )
            tasks.add(task)
            return task
        }

        override suspend fun getProjects(profileId: Long): List<String> = projects
        override suspend fun getHabits(profileId: Long): List<HabitItem> = habits.filter { it.profileId == profileId }
        override suspend fun getProfileName(profileId: Long): String = profileName
    }

    @Before
    fun setup() {
        mockDataProvider = MockMcpDataProvider()
        mcpEngine = McpProtocolEngine(mockDataProvider)
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
        assertEquals("LifeOS-MCP-Bridge", resultObj.getJSONObject("serverInfo").getString("name"))
    }

    @Test
    fun protocolEngine_toolsListAndExecution() = runBlocking {
        // 1. tools/list
        val listJson = """{"jsonrpc":"2.0","id":2,"method":"tools/list"}"""
        val listResponse = mcpEngine.handleMessage(listJson, validToken, validToken, true, authProfileId)
        assertNull(listResponse.error)
        val toolsResult = listResponse.result as JSONObject
        val toolsArray = toolsResult.getJSONArray("tools")
        assertEquals(6, toolsArray.length())

        val toolNames = (0 until toolsArray.length()).map { toolsArray.getJSONObject(it).getString("name") }
        assertTrue(toolNames.contains("get_tasks"))
        assertTrue(toolNames.contains("create_task"))
        assertTrue(toolNames.contains("list_projects"))
        assertTrue(toolNames.contains("get_daily_summary"))
        assertTrue(toolNames.contains("get_intelligent_suggestions"))
        assertTrue(toolNames.contains("get_profile_context"))

        // 2. create_task tool execution
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

        // 3. get_tasks tool execution
        val getJson = """
            {
                "jsonrpc": "2.0",
                "id": 4,
                "method": "tools/call",
                "params": {
                    "name": "get_tasks",
                    "arguments": {
                        "status": "PENDING",
                        "priority": "HIGH"
                    }
                }
            }
        """.trimIndent()
        val getResponse = mcpEngine.handleMessage(getJson, validToken, validToken, true, authProfileId)
        assertNull(getResponse.error)
        val getResult = getResponse.result as JSONObject
        assertFalse(getResult.getBoolean("isError"))
        val contentArr = getResult.getJSONArray("content")
        assertTrue(contentArr.getJSONObject(0).getString("text").contains("Deep Work on Architecture"))
    }

    @Test
    fun protocolEngine_resourcesAndPrompts() = runBlocking {
        // 1. resources/list
        val resListJson = """{"jsonrpc":"2.0","id":10,"method":"resources/list"}"""
        val resListResponse = mcpEngine.handleMessage(resListJson, validToken, validToken, true, authProfileId)
        assertNull(resListResponse.error)
        val resResult = resListResponse.result as JSONObject
        val resources = resResult.getJSONArray("resources")
        assertEquals(3, resources.length())

        // 2. prompts/list
        val promptListJson = """{"jsonrpc":"2.0","id":11,"method":"prompts/list"}"""
        val promptListResponse = mcpEngine.handleMessage(promptListJson, validToken, validToken, true, authProfileId)
        assertNull(promptListResponse.error)
        val promptResult = promptListResponse.result as JSONObject
        val prompts = promptResult.getJSONArray("prompts")
        assertEquals(3, prompts.length())

        // 3. prompts/get
        val promptGetJson = """
            {
                "jsonrpc": "2.0",
                "id": 12,
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
        assertTrue(suggTypes.contains(com.example.domain.intelligence.SuggestionType.OVERLOAD_WARNING))
        assertTrue(suggTypes.contains(com.example.domain.intelligence.SuggestionType.RESCHEDULE_OVERDUE))
        assertTrue(suggTypes.contains(com.example.domain.intelligence.SuggestionType.HABIT_MOMENTUM))
        assertTrue(suggTypes.contains(com.example.domain.intelligence.SuggestionType.RADAR_BALANCE))
    }
}
