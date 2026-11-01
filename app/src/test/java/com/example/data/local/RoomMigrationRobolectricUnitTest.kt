package com.example.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomMigrationRobolectricUnitTest {

    @Test
    fun testRealSqliteMigrationV6ToV10() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val dbName = "test_robolectric_migration.db"
        context.deleteDatabase(dbName)

        val factory = FrameworkSQLiteOpenHelperFactory()

        // 1. Create DB at version 6
        val v6Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `tasks` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `durationMinutes` INTEGER NOT NULL DEFAULT 30,
                            `recurrenceEndDateIso` TEXT DEFAULT NULL,
                            `skippedDatesJson` TEXT NOT NULL DEFAULT '[]',
                            `rescheduledDatesJson` TEXT NOT NULL DEFAULT '{}',
                            `notes` TEXT NOT NULL DEFAULT '',
                            `isReminderEnabled` INTEGER NOT NULL DEFAULT 0,
                            `reminderMinutesBefore` INTEGER NOT NULL DEFAULT 0,
                            `isArchived` INTEGER NOT NULL DEFAULT 0,
                            `isPaused` INTEGER NOT NULL DEFAULT 0,
                            `projectId` INTEGER DEFAULT NULL,
                            `deadlineIso` TEXT DEFAULT NULL,
                            `isAllDay` INTEGER NOT NULL DEFAULT 1,
                            `schedulingMode` TEXT NOT NULL DEFAULT 'DURATION',
                            `durationAmount` INTEGER NOT NULL DEFAULT 30,
                            `durationUnit` TEXT NOT NULL DEFAULT 'MINUTES',
                            `deadlineTime` TEXT DEFAULT NULL,
                            `recurrenceEndCondition` TEXT NOT NULL DEFAULT 'NEVER',
                            `recurrenceCount` INTEGER NOT NULL DEFAULT 1,
                            `recurrenceUnit` TEXT NOT NULL DEFAULT 'DAYS'
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `honeycomb_shortcuts` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `type` TEXT NOT NULL,
                            `targetId` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `emoji` TEXT NOT NULL DEFAULT '✅',
                            `customColorHex` INTEGER DEFAULT NULL,
                            `orderIndex` INTEGER NOT NULL DEFAULT 0,
                            `isPinned` INTEGER NOT NULL DEFAULT 0,
                            `createdAt` INTEGER NOT NULL DEFAULT 0,
                            `coordQ` INTEGER NOT NULL DEFAULT 0,
                            `coordR` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("INSERT INTO tasks (id, title, durationMinutes) VALUES (101, 'Pre-migration Task 101', 45)")
                    db.execSQL("INSERT INTO honeycomb_shortcuts (id, type, targetId, title, emoji, customColorHex, orderIndex) VALUES (1, 'TASK', '101', 'Task 101 Shortcut', '✅', NULL, 0)")
                    db.execSQL("INSERT INTO honeycomb_shortcuts (id, type, targetId, title, emoji, customColorHex, orderIndex) VALUES (2, 'HABIT', '201', 'Habit 201 Shortcut', '🏃', -16711936, 1)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val v6Db = factory.create(v6Config).writableDatabase
        v6Db.close()

        // 2. Upgrade to version 10 applying migrations 6->7, 7->8, 8->9, 9->10
        val v10Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(10) {
                override fun onCreate(db: SupportSQLiteDatabase) {}

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion <= 6) MIGRATION_6_7.migrate(db)
                    if (oldVersion <= 7) MIGRATION_7_8.migrate(db)
                    if (oldVersion <= 8) MIGRATION_8_9.migrate(db)
                    if (oldVersion <= 9) MIGRATION_9_10.migrate(db)
                }
            })
            .build()

        val v10Db = factory.create(v10Config).writableDatabase

        // 3. Verify task 101 is intact with new column isQuickReminder = 0
        val taskCursor = v10Db.query("SELECT id, title, durationMinutes, isQuickReminder FROM tasks WHERE id = 101")
        assertTrue("Task 101 must exist after migration", taskCursor.moveToFirst())
        assertEquals(101L, taskCursor.getLong(0))
        assertEquals("Pre-migration Task 101", taskCursor.getString(1))
        assertEquals(45, taskCursor.getInt(2))
        assertEquals("isQuickReminder default must be 0", 0, taskCursor.getInt(3))
        taskCursor.close()

        // 4. Verify shortcut 1 migrated to colorSource = 'PROJECT', shape = 'HEXAGON'
        val s1Cursor = v10Db.query("SELECT id, title, colorSource, shape FROM honeycomb_shortcuts WHERE id = 1")
        assertTrue("Shortcut 1 must exist after migration", s1Cursor.moveToFirst())
        assertEquals("Task 101 Shortcut", s1Cursor.getString(1))
        assertEquals("PROJECT", s1Cursor.getString(2))
        assertEquals("HEXAGON", s1Cursor.getString(3))
        s1Cursor.close()

        // 5. Verify shortcut 2 migrated to colorSource = 'CUSTOM', shape = 'HEXAGON'
        val s2Cursor = v10Db.query("SELECT id, title, colorSource, shape, customColorHex FROM honeycomb_shortcuts WHERE id = 2")
        assertTrue("Shortcut 2 must exist after migration", s2Cursor.moveToFirst())
        assertEquals("Habit 201 Shortcut", s2Cursor.getString(1))
        assertEquals("CUSTOM", s2Cursor.getString(2))
        assertEquals("HEXAGON", s2Cursor.getString(3))
        assertEquals(-16711936, s2Cursor.getInt(4))
        s2Cursor.close()

        // 6. Verify new tables from migrations 7_8, 9_10 exist
        val medCursor = v10Db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='medications'")
        assertTrue("Medications table must exist in v10", medCursor.moveToFirst())
        medCursor.close()

        val trackerCursor = v10Db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='trackers'")
        assertTrue("Trackers table must exist in v10", trackerCursor.moveToFirst())
        trackerCursor.close()

        val subCursor = v10Db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='subscriptions'")
        assertTrue("Subscriptions table must exist in v10", subCursor.moveToFirst())
        subCursor.close()

        v10Db.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun testRealSqliteMigrationV10ToV15() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val dbName = "test_robolectric_migration_v10_v15.db"
        context.deleteDatabase(dbName)

        val factory = FrameworkSQLiteOpenHelperFactory()

        // 1. Create DB at version 10 with v10 schema tables
        val v10Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(10) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `tasks` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `notes` TEXT NOT NULL DEFAULT '',
                            `isCompleted` INTEGER NOT NULL DEFAULT 0,
                            `priority` INTEGER NOT NULL DEFAULT 0,
                            `dueDate` INTEGER DEFAULT NULL,
                            `dueDateIso` TEXT DEFAULT NULL,
                            `hasTime` INTEGER NOT NULL DEFAULT 0,
                            `dueTime` TEXT DEFAULT NULL,
                            `projectId` INTEGER DEFAULT NULL,
                            `recurrenceType` TEXT NOT NULL DEFAULT 'NONE',
                            `recurrenceInterval` INTEGER NOT NULL DEFAULT 1,
                            `recurrenceDaysOfWeek` TEXT DEFAULT NULL,
                            `durationMinutes` INTEGER NOT NULL DEFAULT 30,
                            `isQuickReminder` INTEGER NOT NULL DEFAULT 0,
                            `createdAt` INTEGER NOT NULL DEFAULT 0,
                            `updatedAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `habits` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `frequency` TEXT NOT NULL DEFAULT 'DAILY',
                            `targetDaysPerWeek` INTEGER NOT NULL DEFAULT 7,
                            `targetCount` INTEGER NOT NULL DEFAULT 1,
                            `colorHex` INTEGER NOT NULL DEFAULT 0,
                            `createdAt` INTEGER NOT NULL DEFAULT 0,
                            `updatedAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `projects` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `colorHex` INTEGER NOT NULL DEFAULT 0,
                            `createdAt` INTEGER NOT NULL DEFAULT 0,
                            `updatedAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `labels` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `colorHex` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `honeycomb_shortcuts` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `type` TEXT NOT NULL,
                            `targetId` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `emoji` TEXT NOT NULL DEFAULT '✅',
                            `customColorHex` INTEGER DEFAULT NULL,
                            `orderIndex` INTEGER NOT NULL DEFAULT 0,
                            `isPinned` INTEGER NOT NULL DEFAULT 0,
                            `createdAt` INTEGER NOT NULL DEFAULT 0,
                            `coordQ` INTEGER NOT NULL DEFAULT 0,
                            `coordR` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `plans` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `description` TEXT NOT NULL DEFAULT '',
                            `level` TEXT NOT NULL DEFAULT 'MONTHLY',
                            `startDateIso` TEXT NOT NULL DEFAULT '',
                            `targetDateIso` TEXT NOT NULL DEFAULT '',
                            `status` TEXT NOT NULL DEFAULT 'NOT_STARTED',
                            `manualProgress` INTEGER NOT NULL DEFAULT 0,
                            `isAutoProgress` INTEGER NOT NULL DEFAULT 1,
                            `parentPlanId` INTEGER DEFAULT NULL,
                            `projectId` INTEGER DEFAULT NULL,
                            `priority` INTEGER NOT NULL DEFAULT 2,
                            `createdAt` INTEGER NOT NULL DEFAULT 0,
                            `updatedAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `trackers` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `emoji` TEXT NOT NULL DEFAULT '📦',
                            `description` TEXT NOT NULL DEFAULT '',
                            `projectId` INTEGER DEFAULT NULL,
                            `labelIdsJson` TEXT NOT NULL DEFAULT '[]',
                            `customColorHex` INTEGER DEFAULT NULL,
                            `trackDuration` INTEGER NOT NULL DEFAULT 1,
                            `trackDistance` INTEGER NOT NULL DEFAULT 0,
                            `trackQuantity` INTEGER NOT NULL DEFAULT 0,
                            `distanceUnit` TEXT NOT NULL DEFAULT 'KILOMETERS',
                            `quantityUnit` TEXT NOT NULL DEFAULT 'PIECES',
                            `customQuantityUnitName` TEXT NOT NULL DEFAULT '',
                            `inventoryPolicy` TEXT NOT NULL DEFAULT 'CONSUMABLE',
                            `currentStock` REAL NOT NULL DEFAULT 0.0,
                            `emptyStock` REAL NOT NULL DEFAULT 0.0,
                            `isReminderEnabled` INTEGER NOT NULL DEFAULT 0,
                            `reminderDaysInterval` INTEGER NOT NULL DEFAULT 30,
                            `isArchived` INTEGER NOT NULL DEFAULT 0,
                            `createdAt` INTEGER NOT NULL DEFAULT 0,
                            `updatedAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `subscriptions` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `emoji` TEXT NOT NULL DEFAULT '💳',
                            `description` TEXT NOT NULL DEFAULT '',
                            `projectId` INTEGER DEFAULT NULL,
                            `labelIdsJson` TEXT NOT NULL DEFAULT '[]',
                            `customColorHex` INTEGER DEFAULT NULL,
                            `amount` REAL NOT NULL DEFAULT 0.0,
                            `currency` TEXT NOT NULL DEFAULT '₹',
                            `frequency` TEXT NOT NULL DEFAULT 'MONTHLY',
                            `customIntervalCount` INTEGER NOT NULL DEFAULT 1,
                            `customIntervalUnit` TEXT NOT NULL DEFAULT 'MONTHS',
                            `anchorDateIso` TEXT NOT NULL DEFAULT '',
                            `anchorDayOfMonth` INTEGER NOT NULL DEFAULT 1,
                            `nextRenewalDateIso` TEXT NOT NULL DEFAULT '',
                            `missingDatePolicy` TEXT NOT NULL DEFAULT 'LAST_VALID_DAY',
                            `autoRenew` INTEGER NOT NULL DEFAULT 1,
                            `paymentMethod` TEXT NOT NULL DEFAULT '',
                            `isTrial` INTEGER NOT NULL DEFAULT 0,
                            `trialStartDateIso` TEXT DEFAULT NULL,
                            `trialEndDateIso` TEXT DEFAULT NULL,
                            `status` TEXT NOT NULL DEFAULT 'ACTIVE',
                            `cancellationDateIso` TEXT DEFAULT NULL,
                            `cancellationReason` TEXT NOT NULL DEFAULT '',
                            `reminderDaysBefore` INTEGER NOT NULL DEFAULT 3,
                            `overdueReminderEnabled` INTEGER NOT NULL DEFAULT 1,
                            `notes` TEXT NOT NULL DEFAULT '',
                            `createdAt` INTEGER NOT NULL DEFAULT 0,
                            `updatedAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `medications` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `taskId` INTEGER NOT NULL DEFAULT 0,
                            `name` TEXT NOT NULL,
                            `form` TEXT NOT NULL DEFAULT 'TABLET',
                            `dosageAmount` TEXT NOT NULL DEFAULT '1',
                            `dosageUnit` TEXT NOT NULL DEFAULT 'PILL',
                            `strength` TEXT NOT NULL DEFAULT '',
                            `instructions` TEXT NOT NULL DEFAULT '',
                            `scheduleType` TEXT NOT NULL DEFAULT 'DAILY',
                            `mealPeriodsJson` TEXT NOT NULL DEFAULT '[]',
                            `fixedTimesJson` TEXT NOT NULL DEFAULT '[]',
                            `intervalHours` INTEGER NOT NULL DEFAULT 4,
                            `intervalModel` TEXT NOT NULL DEFAULT 'FIXED_SCHEDULE',
                            `intervalAnchorTime` TEXT NOT NULL DEFAULT '08:00',
                            `allowOvernight` INTEGER NOT NULL DEFAULT 0,
                            `selectedDaysOfWeek` TEXT NOT NULL DEFAULT '',
                            `startDateIso` TEXT NOT NULL DEFAULT '',
                            `endDateIso` TEXT DEFAULT NULL,
                            `courseDurationDays` INTEGER DEFAULT NULL,
                            `isAsNeeded` INTEGER NOT NULL DEFAULT 0,
                            `isPaused` INTEGER NOT NULL DEFAULT 0,
                            `reminderSound` TEXT NOT NULL DEFAULT 'CHIME',
                            `alertStyle` TEXT NOT NULL DEFAULT 'STANDARD',
                            `snoozeMinutes` INTEGER NOT NULL DEFAULT 10,
                            `followUpEnabled` INTEGER NOT NULL DEFAULT 1,
                            `followUpIntervalMinutes` INTEGER NOT NULL DEFAULT 15,
                            `maxFollowUps` INTEGER NOT NULL DEFAULT 2,
                            `privacySafeNotification` INTEGER NOT NULL DEFAULT 1,
                            `createdAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `trash_items` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `originalTable` TEXT NOT NULL,
                            `originalId` INTEGER NOT NULL,
                            `title` TEXT NOT NULL,
                            `jsonData` TEXT NOT NULL,
                            `deletedAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    // Insert pre-existing v10 task and project
                    db.execSQL("INSERT INTO tasks (id, title, durationMinutes) VALUES (501, 'Pre-migration Task 501', 25)")
                    db.execSQL("INSERT INTO projects (id, name) VALUES (10, 'Pre-migration Project 10')")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val v10Db = factory.create(v10Config).writableDatabase
        v10Db.close()

        // 2. Open DB at version 18, applying all migrations from 10 to 18
        val v15Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(18) {
                override fun onCreate(db: SupportSQLiteDatabase) {}

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion <= 10) MIGRATION_10_11.migrate(db)
                    if (oldVersion <= 11) MIGRATION_11_12.migrate(db)
                    if (oldVersion <= 12) MIGRATION_12_13.migrate(db)
                    if (oldVersion <= 13) MIGRATION_13_14.migrate(db)
                    if (oldVersion <= 14) MIGRATION_14_15.migrate(db)
                    if (oldVersion <= 15) MIGRATION_15_16.migrate(db)
                    if (oldVersion <= 16) MIGRATION_16_17.migrate(db)
                    if (oldVersion <= 17) MIGRATION_17_18.migrate(db)
                }
            })
            .build()

        val v15Db = factory.create(v15Config).writableDatabase

        // 3. Verify Profiles table and default Personal profile created
        val profileCursor = v15Db.query("SELECT id, name, type, isDefault FROM profiles WHERE id = 1")
        assertTrue("Profiles table must contain default Personal profile", profileCursor.moveToFirst())
        assertEquals(1L, profileCursor.getLong(0))
        assertEquals("Personal", profileCursor.getString(1))
        assertEquals("PERSONAL", profileCursor.getString(2))
        assertEquals(1, profileCursor.getInt(3))
        profileCursor.close()

        // 4. Verify tasks table has profileId default 1 and recurrence window columns
        val taskCursor = v15Db.query("SELECT id, title, profileId, recurrenceWindowStartDay, recurrenceWindowEndDay FROM tasks WHERE id = 501")
        assertTrue("Task 501 must be preserved with migrated columns", taskCursor.moveToFirst())
        assertEquals("Pre-migration Task 501", taskCursor.getString(1))
        assertEquals("profileId must default to 1", 1L, taskCursor.getLong(2))
        taskCursor.close()

        // 5. Verify profile_surveys table exists and supports cascade constraint
        val surveyTableCursor = v15Db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='profile_surveys'")
        assertTrue("profile_surveys table must exist in v15", surveyTableCursor.moveToFirst())
        surveyTableCursor.close()

        // 6. Verify project_workspace_items and views exist from MIGRATION_13_14
        val workspaceCursor = v15Db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='project_workspace_items'")
        assertTrue("project_workspace_items table must exist in v15", workspaceCursor.moveToFirst())
        workspaceCursor.close()

        // 7. Verify glossary_packs and glossary_items exist and core pack is seeded from MIGRATION_14_15
        val glossaryPackCursor = v15Db.query("SELECT id, title, itemCount FROM glossary_packs WHERE id = 'lifeos_core'")
        assertTrue("lifeos_core pack must be seeded in v15", glossaryPackCursor.moveToFirst())
        assertEquals("LifeOS Productivity & Systems Thinking", glossaryPackCursor.getString(1))
        assertEquals(8, glossaryPackCursor.getInt(2))
        glossaryPackCursor.close()

        val glossaryItemsCursor = v15Db.query("SELECT COUNT(*) FROM glossary_items WHERE packId = 'lifeos_core'")
        assertTrue(glossaryItemsCursor.moveToFirst())
        assertEquals(8, glossaryItemsCursor.getInt(0))
        glossaryItemsCursor.close()

        // 8. Verify project_custom_properties table exists from MIGRATION_15_16
        val customPropCursor = v15Db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='project_custom_properties'")
        assertTrue("project_custom_properties table must exist in v16", customPropCursor.moveToFirst())
        customPropCursor.close()

        // 9. Verify glossary_user_data table exists from MIGRATION_16_17
        val userDataCursor = v15Db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='glossary_user_data'")
        assertTrue("glossary_user_data table must exist in v17", userDataCursor.moveToFirst())
        userDataCursor.close()

        // 10. Verify mcp_servers table exists from MIGRATION_17_18
        val mcpServersCursor = v15Db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='mcp_servers'")
        assertTrue("mcp_servers table must exist in v18", mcpServersCursor.moveToFirst())
        mcpServersCursor.close()

        // 11. Verify mcp_audit_logs table exists from MIGRATION_17_18
        val mcpAuditLogsCursor = v15Db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='mcp_audit_logs'")
        assertTrue("mcp_audit_logs table must exist in v18", mcpAuditLogsCursor.moveToFirst())
        mcpAuditLogsCursor.close()

        v15Db.close()
        context.deleteDatabase(dbName)
    }
}