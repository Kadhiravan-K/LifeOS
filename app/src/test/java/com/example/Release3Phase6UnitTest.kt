package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.DataExportImportManager
import com.example.data.local.AppDatabase
import com.example.data.local.MIGRATION_13_14
import com.example.data.local.entities.ProfileEntity
import com.example.data.local.entities.ProjectEntity
import com.example.data.local.entities.ProjectWorkspaceItemEntity
import com.example.data.preferences.UserPreferences
import com.example.data.repository.LifeRepository
import com.example.domain.projectworkspace.AiPromptConfig
import com.example.domain.projectworkspace.AiPromptGenerator
import com.example.domain.projectworkspace.ProjectCustomStatus
import com.example.domain.projectworkspace.ProjectWorkspaceItem
import com.example.domain.projectworkspace.ProjectWorkspaceView
import com.example.domain.projectworkspace.WorkspaceGroupBy
import com.example.domain.projectworkspace.WorkspaceSortBy
import com.example.domain.projectworkspace.WorkspaceViewType
import kotlinx.coroutines.flow.first
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

/**
 * Release 3 Phase 6 — Advanced Project Database Workspace (#20) Automated Test Suite.
 */
@RunWith(RobolectricTestRunner::class)
class Release3Phase6UnitTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var preferences: UserPreferences
    private lateinit var repository: LifeRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = UserPreferences(context)
        repository = LifeRepository(database, preferences)

        runBlocking {
            database.profileDao().insertProfile(ProfileEntity.createDefaultPersonal())
            preferences.setActiveProfileId(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testRoomMigration13To14_schemaAndDataPreservation() {
        val helperConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("migration_test_v13_v14.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(13) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `profiles` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `emoji` TEXT NOT NULL DEFAULT '👤',
                            `colorHex` INTEGER NOT NULL DEFAULT 4282168054,
                            `type` TEXT NOT NULL DEFAULT 'PERSONAL',
                            `orderIndex` INTEGER NOT NULL DEFAULT 0,
                            `isDefault` INTEGER NOT NULL DEFAULT 0,
                            `createdAt` INTEGER NOT NULL DEFAULT 0,
                            `updatedAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `projects` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `profileId` INTEGER NOT NULL DEFAULT 1,
                            `name` TEXT NOT NULL,
                            `description` TEXT NOT NULL DEFAULT '',
                            `colorId` TEXT NOT NULL DEFAULT 'blue',
                            `emoji` TEXT NOT NULL DEFAULT '📁',
                            `isFavorite` INTEGER NOT NULL DEFAULT 0,
                            `isArchived` INTEGER NOT NULL DEFAULT 0,
                            `orderIndex` INTEGER NOT NULL DEFAULT 0,
                            `createdAt` INTEGER NOT NULL DEFAULT 0,
                            `updatedAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        INSERT INTO `projects` (`id`, `profileId`, `name`, `description`, `colorId`, `emoji`, `isFavorite`, `isArchived`, `orderIndex`, `createdAt`, `updatedAt`)
                        VALUES (1, 1, 'Embedded Systems', 'Core IoT firmware', 'blue', '⚙️', 1, 0, 0, 1000, 1000)
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
        val db = helper.writableDatabase

        // Pre-migration validation
        val preCursor = db.query("SELECT name FROM projects WHERE id = 1")
        assertTrue(preCursor.moveToFirst())
        assertEquals("Embedded Systems", preCursor.getString(0))
        preCursor.close()

        // Execute MIGRATION_13_14
        MIGRATION_13_14.migrate(db)

        // Validate new tables exist
        val tableCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='project_workspace_items'")
        assertTrue("project_workspace_items table must exist after migration", tableCursor.moveToFirst())
        tableCursor.close()

        val viewsCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='project_workspace_views'")
        assertTrue("project_workspace_views table must exist after migration", viewsCursor.moveToFirst())
        viewsCursor.close()

        val statusCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='project_custom_statuses'")
        assertTrue("project_custom_statuses table must exist after migration", statusCursor.moveToFirst())
        statusCursor.close()

        // Validate existing project was preserved
        val postCursor = db.query("SELECT name, description FROM projects WHERE id = 1")
        assertTrue(postCursor.moveToFirst())
        assertEquals("Embedded Systems", postCursor.getString(0))
        assertEquals("Core IoT firmware", postCursor.getString(1))
        postCursor.close()

        db.close()
        context.deleteDatabase("migration_test_v13_v14.db")
    }

    @Test
    fun testWorkspaceItemCRUD_andProfileScoping() = runBlocking {
        // Create project
        val projectId = database.projectDao().insertProject(
            ProjectEntity(
                name = "LifeOS Core",
                profileId = ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID
            )
        )

        // Insert Item
        val item = ProjectWorkspaceItem(
            projectId = projectId,
            title = "Implement Table View",
            status = "IN_PROGRESS",
            progress = 60,
            category = "Feature",
            version = "V3",
            dueDateIso = "2026-11-15",
            notes = "Support inline editing and multi-select",
            priority = "P1",
            customProperties = mapOf("Effort" to "3d", "Reviewer" to "Sam")
        )

        val insertedId = repository.insertWorkspaceItem(item)
        assertTrue(insertedId > 0)

        // Query items
        val items = repository.getWorkspaceItemsForProject(projectId)
        assertEquals(1, items.size)
        val retrieved = items[0]
        assertEquals("Implement Table View", retrieved.title)
        assertEquals("IN_PROGRESS", retrieved.status)
        assertEquals(60, retrieved.progress)
        assertEquals("V3", retrieved.version)
        assertEquals("3d", retrieved.customProperties["Effort"])

        // Test Profile Isolation: Switch to Profile 2
        val profile2Id = repository.createProfile(name = "Work", type = ProfileEntity.TYPE_WORK)
        repository.setActiveProfileId(profile2Id)

        val profile2Items = repository.getWorkspaceItemsForProject(projectId)
        assertTrue("Profile 2 must have zero access to Profile 1 workspace items", profile2Items.isEmpty())

        // Switch back to Profile 1
        repository.setActiveProfileId(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
        val profile1Items = repository.getWorkspaceItemsForProject(projectId)
        assertEquals(1, profile1Items.size)

        // Update item
        repository.updateWorkspaceItem(retrieved.copy(status = "DONE", progress = 100))
        val updated = repository.getWorkspaceItemsForProject(projectId)[0]
        assertEquals("DONE", updated.status)
        assertEquals(100, updated.progress)

        // Delete item
        val deleted = repository.deleteWorkspaceItem(updated)
        assertTrue(deleted)
        assertTrue(repository.getWorkspaceItemsForProject(projectId).isEmpty())
    }

    @Test
    fun testBulkOperations() = runBlocking {
        val projectId = database.projectDao().insertProject(
            ProjectEntity(name = "Backend Services", profileId = ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
        )

        val id1 = repository.insertWorkspaceItem(ProjectWorkspaceItem(projectId = projectId, title = "Auth API", version = "V1"))
        val id2 = repository.insertWorkspaceItem(ProjectWorkspaceItem(projectId = projectId, title = "Sync Worker", version = "V1"))
        val id3 = repository.insertWorkspaceItem(ProjectWorkspaceItem(projectId = projectId, title = "Backup Engine", version = "V1"))

        assertEquals(3, repository.getWorkspaceItemsForProject(projectId).size)

        // Bulk status update
        repository.bulkUpdateWorkspaceItemStatus(listOf(id1, id2), "DONE")
        val itemsAfterStatus = repository.getWorkspaceItemsForProject(projectId).associateBy { it.id }
        assertEquals("DONE", itemsAfterStatus[id1]?.status)
        assertEquals(100, itemsAfterStatus[id1]?.progress)
        assertEquals("DONE", itemsAfterStatus[id2]?.status)
        assertEquals("NOT_STARTED", itemsAfterStatus[id3]?.status)

        // Bulk version update
        repository.bulkUpdateWorkspaceItemVersion(listOf(id1, id2, id3), "V2")
        val itemsAfterVersion = repository.getWorkspaceItemsForProject(projectId)
        assertTrue(itemsAfterVersion.all { it.version == "V2" })

        // Bulk delete
        val deletedCount = repository.deleteWorkspaceItemsByIds(listOf(id1, id3))
        assertEquals(2, deletedCount)
        val remaining = repository.getWorkspaceItemsForProject(projectId)
        assertEquals(1, remaining.size)
        assertEquals(id2, remaining[0].id)
    }

    @Test
    fun testSavedViewsAndCustomStatuses() = runBlocking {
        val projectId = database.projectDao().insertProject(
            ProjectEntity(name = "Mobile App", profileId = ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
        )

        // Saved View
        val view = ProjectWorkspaceView(
            projectId = projectId,
            name = "V3 Board View",
            viewType = WorkspaceViewType.BOARD,
            filterVersion = "V3",
            sortBy = WorkspaceSortBy.PRIORITY,
            groupBy = WorkspaceGroupBy.STATUS,
            visibleProperties = setOf("status", "progress", "version")
        )
        val viewId = repository.insertWorkspaceView(view)
        assertTrue(viewId > 0)

        val views = repository.getWorkspaceViewsForProjectFlow(projectId).first()
        assertEquals(1, views.size)
        assertEquals("V3 Board View", views[0].name)
        assertEquals(WorkspaceViewType.BOARD, views[0].viewType)
        assertEquals("V3", views[0].filterVersion)

        // Custom Status
        val status = ProjectCustomStatus(
            projectId = projectId,
            name = "Under Review",
            colorHex = 0xFFF59E0B
        )
        val statusId = repository.insertCustomStatus(status)
        assertTrue(statusId > 0)

        val statuses = repository.getCustomStatusesForProjectFlow(projectId).first()
        assertEquals(1, statuses.size)
        assertEquals("Under Review", statuses[0].name)

        // Delete view and status
        assertTrue(repository.deleteWorkspaceView(viewId))
        assertTrue(repository.deleteCustomStatus(statusId))
        assertTrue(repository.getWorkspaceViewsForProjectFlow(projectId).first().isEmpty())
        assertTrue(repository.getCustomStatusesForProjectFlow(projectId).first().isEmpty())
    }

    @Test
    fun testAiPromptGenerator_structuredOutputNeverModifiesData() = runBlocking {
        val projectId = database.projectDao().insertProject(
            ProjectEntity(name = "Release 3 Workspace", profileId = ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
        )

        val items = listOf(
            ProjectWorkspaceItem(id = 1, projectId = projectId, title = "Table View Support", category = "Feature", version = "V3", priority = "P0", status = "DONE", progress = 100),
            ProjectWorkspaceItem(id = 2, projectId = projectId, title = "Drag & Drop Glitch", category = "Bug", version = "V3", priority = "P1", status = "NOT_STARTED", notes = "Fix touch slop threshold"),
            ProjectWorkspaceItem(id = 3, projectId = projectId, title = "Export Formatting", category = "Improvement", version = "V3", priority = "P2", status = "IN_PROGRESS", progress = 40),
            ProjectWorkspaceItem(id = 4, projectId = projectId, title = "Legacy Cleanup", category = "Feature", version = "V1", priority = "P3", status = "DONE")
        )

        val config = AiPromptConfig(
            selectedCategories = setOf("Feature", "Bug", "Improvement"),
            selectedVersions = setOf("V3"),
            objectiveDescription = "Deliver Release 3 Advanced Database View"
        )

        val generatedPrompt = AiPromptGenerator.generateMarkdownPrompt(
            projectName = "LifeOS",
            projectDescription = "Personal operating system",
            items = items,
            config = config
        )

        // Verify output content
        assertTrue(generatedPrompt.contains("# TASK CONTEXT: LifeOS"))
        assertTrue(generatedPrompt.contains("## Objective"))
        assertTrue(generatedPrompt.contains("Deliver Release 3 Advanced Database View"))
        assertTrue(generatedPrompt.contains("### Features (1)"))
        assertTrue(generatedPrompt.contains("Table View Support"))
        assertTrue(generatedPrompt.contains("`[V3]`"))
        assertTrue(generatedPrompt.contains("### Bug Fixes (1)"))
        assertTrue(generatedPrompt.contains("Drag & Drop Glitch"))
        assertTrue(generatedPrompt.contains("Fix touch slop threshold"))
        assertTrue(generatedPrompt.contains("### Improvements (1)"))
        assertTrue(generatedPrompt.contains("Export Formatting"))
        assertFalse("V1 items should be excluded by version filter", generatedPrompt.contains("Legacy Cleanup"))

        // Guarantee items remain unmodified
        assertEquals("DONE", items[0].status)
        assertEquals(100, items[0].progress)
    }

    @Test
    fun testDataExportAndImport_roundtripWorkspacePreservation() = runBlocking {
        val projectId = database.projectDao().insertProject(
            ProjectEntity(id = 10L, name = "Export Test Project", profileId = 1L)
        )

        val item = ProjectWorkspaceItemEntity(
            id = 101L,
            profileId = 1L,
            projectId = 10L,
            title = "Exported Workspace Item",
            status = "IN_PROGRESS",
            progress = 75,
            category = "Feature",
            version = "V2",
            notes = "Test export preservation"
        )
        database.projectWorkspaceDao().insertItem(item)

        val view = com.example.data.local.entities.ProjectWorkspaceViewEntity(
            id = 201L,
            profileId = 1L,
            projectId = 10L,
            name = "Exported Board View",
            viewType = "BOARD"
        )
        database.projectWorkspaceDao().insertView(view)

        val customStatus = com.example.data.local.entities.ProjectCustomStatusEntity(
            id = 301L,
            profileId = 1L,
            projectId = 10L,
            name = "Testing In Progress"
        )
        database.projectWorkspaceDao().insertCustomStatus(customStatus)

        // Export to JSON string
        val exportedJson = DataExportImportManager.exportAllData(database)
        assertTrue(exportedJson.contains("project_workspace_items"))
        assertTrue(exportedJson.contains("Exported Workspace Item"))
        assertTrue(exportedJson.contains("project_workspace_views"))
        assertTrue(exportedJson.contains("Exported Board View"))
        assertTrue(exportedJson.contains("project_custom_statuses"))
        assertTrue(exportedJson.contains("Testing In Progress"))

        // Create new clean database and import
        val freshDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val importSuccess = DataExportImportManager.importData(freshDb, exportedJson)
            assertTrue("Import must succeed", importSuccess)

            val importedItems = freshDb.projectWorkspaceDao().getAllItems()
            assertEquals(1, importedItems.size)
            assertEquals("Exported Workspace Item", importedItems[0].title)
            assertEquals("IN_PROGRESS", importedItems[0].status)
            assertEquals(75, importedItems[0].progress)

            val importedViews = freshDb.projectWorkspaceDao().getAllViews()
            assertEquals(1, importedViews.size)
            assertEquals("Exported Board View", importedViews[0].name)

            val importedStatuses = freshDb.projectWorkspaceDao().getAllCustomStatuses()
            assertEquals(1, importedStatuses.size)
            assertEquals("Testing In Progress", importedStatuses[0].name)
        } finally {
            freshDb.close()
        }
    }
}
