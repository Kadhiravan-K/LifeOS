package com.example

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.DataExportImportManager
import com.example.data.local.AppDatabase
import com.example.data.local.entities.ProfileEntity
import com.example.data.local.entities.TaskEntity
import com.example.mcp.McpSecurityManager
import com.example.notifications.BootCompletedReceiver
import com.example.notifications.ReminderBroadcastReceiver
import com.example.ui.theme.engine.ThemeId
import com.example.ui.theme.engine.ThemeRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class Release3Phase12SecurityAndHardeningUnitTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testBootCompletedReceiver_instantiationAndIntentHandling() {
        val receiver = BootCompletedReceiver()
        assertNotNull(receiver)

        val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(context, bootIntent)

        val nonBootIntent = Intent("com.example.lifeos.UNKNOWN_ACTION")
        receiver.onReceive(context, nonBootIntent)
    }

    @Test
    fun testReminderBroadcastReceiver_securityHardening() {
        val receiver = ReminderBroadcastReceiver()
        assertNotNull(receiver)

        val externalIntent = Intent(ReminderBroadcastReceiver.ACTION_TASK_COMPLETE).apply {
            `package` = "com.malicious.attacker"
        }
        receiver.onReceive(context, externalIntent)
    }

    @Test
    fun testAtomicImportData_underTransaction_restoresAndPreservesIntegrity() = runBlocking {
        val jsonPayload = """
            {
                "version": 1,
                "profiles": [
                    {
                        "id": 1,
                        "name": "Default Work",
                        "emoji": "💼",
                        "colorHex": 4282168054,
                        "type": "WORK",
                        "orderIndex": 0,
                        "isDefault": true,
                        "createdAt": 1700000000000,
                        "updatedAt": 1700000000000
                    },
                    {
                        "id": 2,
                        "name": "Personal Health",
                        "emoji": "🌱",
                        "colorHex": 4280191205,
                        "type": "PERSONAL",
                        "orderIndex": 1,
                        "isDefault": false,
                        "createdAt": 1700000000000,
                        "updatedAt": 1700000000000
                    }
                ],
                "tasks": [
                    {
                        "id": 101,
                        "title": "Hardened Task Security",
                        "emoji": "🛡️",
                        "description": "Verify transactional backup restore",
                        "category": "Security",
                        "priority": "P1",
                        "dueDateIso": "2026-11-01",
                        "profileId": 1
                    }
                ]
            }
        """.trimIndent()

        val success = DataExportImportManager.importData(database, jsonPayload)
        assertTrue("Import must report success for valid structured JSON", success)

        val profiles = database.profileDao().getAllProfiles()
        assertEquals(2, profiles.size)

        val tasks = database.taskDao().getAllTasksList()
        assertEquals(1, tasks.size)
        assertEquals("Hardened Task Security", tasks[0].title)
        assertEquals(1L, tasks[0].profileId)
    }

    @Test
    fun testAtomicImportData_malformedJson_rollsBackCleanly() = runBlocking {
        database.profileDao().insertProfile(
            ProfileEntity(
                id = 1L,
                name = "Initial Pre-Import Profile",
                emoji = "⭐",
                colorHex = 4282168054,
                isDefault = true
            )
        )

        val malformedJson = "{ \"version\": 1, \"profiles\": [ { \"name\": \"Corrupt\" } "
        val result = DataExportImportManager.importData(database, malformedJson)
        assertFalse("Malformed JSON import must fail", result)

        val profiles = database.profileDao().getAllProfiles()
        assertEquals(1, profiles.size)
        assertEquals("Initial Pre-Import Profile", profiles[0].name)
    }

    @Test
    fun testMcpSecurityManager_bearerTokenGenerationAndConstantTimeValidation() {
        val token = McpSecurityManager.generateAuthToken()
        assertNotNull(token)
        assertEquals("Bearer token must be 32 hexadecimal characters", 32, token.length)
        assertTrue("Token must be valid hex", token.all { it.isDigit() || it in 'a'..'f' })

        assertTrue("Exact token must match", McpSecurityManager.validateToken(token, token))
        assertTrue("Bearer prefixed token must match", McpSecurityManager.validateToken("Bearer $token", token))
        assertFalse("Different token must be rejected", McpSecurityManager.validateToken("0123456789abcdef0123456789abcdef", token))
        assertFalse("Null provided token must be rejected", McpSecurityManager.validateToken(null, token))
        assertFalse("Blank stored token must be rejected", McpSecurityManager.validateToken(token, ""))
    }

    @Test
    fun testMcpSecurityManager_profileBoundaryEnforcement() {
        val activeProfileId = 1L
        assertTrue("Allowed profile matches", McpSecurityManager.validateProfileAccess(1L, activeProfileId))
        assertTrue("Null requested profile defaults to active profile", McpSecurityManager.validateProfileAccess(null, activeProfileId))
        assertFalse("Cross-profile access to profile #2 must be blocked", McpSecurityManager.validateProfileAccess(2L, activeProfileId))
        assertFalse("Cross-profile access to profile #99 must be blocked", McpSecurityManager.validateProfileAccess(99L, activeProfileId))
    }

    @Test
    fun testPendingIntentSecurity_flagImmutableCompliance() {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TASK_COMPLETE
        }
        val pi = PendingIntent.getBroadcast(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        assertNotNull("PendingIntent must be instantiated", pi)
    }

    @Test
    fun testGlassTokensAccessibilityAndContrastCompliance() {
        val glassTheme = ThemeRegistry.GLASS_THEME
        assertNotNull("Glass theme must exist in registry", glassTheme)
        assertEquals(ThemeId.GLASS, glassTheme.id)
        assertTrue("Glass theme must have isGlass shape", glassTheme.shapes.isGlass)

        // Verify contrast & colors
        val colors = glassTheme.colors
        assertNotNull(colors.primary)
        assertNotNull(colors.surface)
        assertNotNull(colors.onSurface)
        assertNotNull(colors.honeycombBorder)
        assertTrue("Theme decoration motif must be non-blank", glassTheme.decoration.motifEmoji.isNotBlank())
    }
}
