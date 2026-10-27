package com.example

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.local.MIGRATION_6_7
import com.example.data.local.MIGRATION_7_8
import com.example.data.local.MIGRATION_8_9
import com.example.data.local.MIGRATION_9_10
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real SQLite Migration Instrumentation Test.
 * Verifies non-destructive schema migration from pre-v7 to v10 on actual SQLite storage,
 * validating data retention for tasks, habits, and honeycomb shortcut customizations.
 */
@RunWith(AndroidJUnit4::class)
class RoomMigrationInstrumentationTest {

    private val dbName = "test_migration_v6_to_v10.db"

    @Test
    fun testMigrationFromV6ToV10PreservesUserData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(dbName)

        val factory = FrameworkSQLiteOpenHelperFactory()

        // 1. Create SQLite DB at version 6 with historical schema
        val v6Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Pre-v7 Tasks Table
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

                    // Pre-v7 Honeycomb Shortcuts Table
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

                    // Insert Pre-migration records
                    db.execSQL("INSERT INTO tasks (id, title, durationMinutes) VALUES (101, 'Pre-migration Task 101', 45)")
                    db.execSQL("INSERT INTO honeycomb_shortcuts (id, type, targetId, title, emoji, customColorHex, orderIndex) VALUES (1, 'TASK', '101', 'Task 101 Shortcut', '✅', NULL, 0)")
                    db.execSQL("INSERT INTO honeycomb_shortcuts (id, type, targetId, title, emoji, customColorHex, orderIndex) VALUES (2, 'HABIT', '201', 'Habit 201 Shortcut', '🏃', -16711936, 1)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val v6Db = factory.create(v6Config).writableDatabase
        v6Db.close()

        // 2. Open DB and sequentially apply migrations 6->7, 7->8, 8->9, 9->10
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

        // 3. Verify task 101 was preserved with newly added columns
        val taskCursor = v10Db.query("SELECT id, title, durationMinutes, isQuickReminder FROM tasks WHERE id = 101")
        assertTrue("Task 101 must exist after migration", taskCursor.moveToFirst())
        assertEquals(101L, taskCursor.getLong(0))
        assertEquals("Pre-migration Task 101", taskCursor.getString(1))
        assertEquals(45, taskCursor.getInt(2))
        assertEquals("isQuickReminder default must be 0", 0, taskCursor.getInt(3))
        taskCursor.close()

        // 4. Verify shortcut 1 (no custom color) migrated to colorSource = 'PROJECT', shape = 'HEXAGON'
        val s1Cursor = v10Db.query("SELECT id, title, colorSource, shape FROM honeycomb_shortcuts WHERE id = 1")
        assertTrue("Shortcut 1 must exist after migration", s1Cursor.moveToFirst())
        assertEquals("Task 101 Shortcut", s1Cursor.getString(1))
        assertEquals("PROJECT", s1Cursor.getString(2))
        assertEquals("HEXAGON", s1Cursor.getString(3))
        s1Cursor.close()

        // 5. Verify shortcut 2 (with custom color) migrated to colorSource = 'CUSTOM', shape = 'HEXAGON'
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
}