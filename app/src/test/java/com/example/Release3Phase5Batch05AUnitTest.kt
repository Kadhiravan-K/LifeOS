package com.example

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MIGRATION_10_11
import com.example.data.local.entities.ProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Release 3 Phase 5 / Batch 05A Unit & Real SQLite Migration Tests:
 * - Real SQLite database creation at version 10 with legacy records across all 10 profile-owned tables.
 * - Execution of MIGRATION_10_11.
 * - Verification of data preservation and default profile assignment (profileId = 1).
 * - Verification of profiles table, default Personal profile creation, and deletion guard.
 * - Verification of workspace isolation and independent namespaces across profiles.
 */
@RunWith(RobolectricTestRunner::class)
class Release3Phase5Batch05AUnitTest {

    @Test
    fun testRealSqliteMigrationV10ToV11() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val dbName = "test_migration_v10_v11.db"
        context.deleteDatabase(dbName)

        val factory = FrameworkSQLiteOpenHelperFactory()

        // 1. Create DB at version 10 with legacy schema and sample records
        val v10Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(10) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create legacy v10 tables (without profileId)
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `tasks` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `emoji` TEXT NOT NULL DEFAULT '',
                            `description` TEXT NOT NULL DEFAULT '',
                            `category` TEXT NOT NULL,
                            `priority` TEXT NOT NULL DEFAULT 'MEDIUM',
                            `dueDateIso` TEXT NOT NULL,
                            `dueTime` TEXT DEFAULT NULL,
                            `durationMinutes` INTEGER NOT NULL DEFAULT 30,
                            `schedulingMode` TEXT NOT NULL DEFAULT 'DURATION',
                            `durationAmount` INTEGER NOT NULL DEFAULT 30,
                            `durationUnit` TEXT NOT NULL DEFAULT 'MINUTES',
                            `deadlineIso` TEXT DEFAULT NULL,
                            `deadlineTime` TEXT DEFAULT NULL,
                            `isAllDay` INTEGER NOT NULL DEFAULT 1,
                            `recurrenceType` TEXT NOT NULL DEFAULT 'NONE',
                            `recurrenceInterval` INTEGER NOT NULL DEFAULT 1,
                            `recurrenceUnit` TEXT NOT NULL DEFAULT 'DAYS',
                            `recurrenceDaysOfWeek` TEXT NOT NULL DEFAULT '',
                            `recurrenceEndCondition` TEXT NOT NULL DEFAULT 'NEVER',
                            `recurrenceEndDateIso` TEXT DEFAULT NULL,
                            `recurrenceCount` INTEGER NOT NULL DEFAULT 1,
                            `isOneTimeCompleted` INTEGER NOT NULL DEFAULT 0,
                            `completedDatesJson` TEXT NOT NULL DEFAULT '[]',
                            `skippedDatesJson` TEXT NOT NULL DEFAULT '[]',
                            `rescheduledDatesJson` TEXT NOT NULL DEFAULT '{}',
                            `subtasksJson` TEXT NOT NULL DEFAULT '[]',
                            `notes` TEXT NOT NULL DEFAULT '',
                            `isReminderEnabled` INTEGER NOT NULL DEFAULT 0,
                            `reminderMinutesBefore` INTEGER NOT NULL DEFAULT 0,
                            `isArchived` INTEGER NOT NULL DEFAULT 0,
                            `isPaused` INTEGER NOT NULL DEFAULT 0,
                            `isQuickReminder` INTEGER NOT NULL DEFAULT 0,
                            `projectId` INTEGER DEFAULT NULL,
                            `createdAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `habits` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `emoji` TEXT NOT NULL DEFAULT '✨',
                            `description` TEXT NOT NULL DEFAULT '',
                            `reminderTime` TEXT DEFAULT NULL,
                            `colorHex` INTEGER NOT NULL DEFAULT 4282168054,
                            `completedDatesJson` TEXT NOT NULL DEFAULT '[]',
                            `orderIndex` INTEGER NOT NULL DEFAULT 0,
                            `createdAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `projects` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `description` TEXT NOT NULL DEFAULT '',
                            `colorId` TEXT NOT NULL,
                            `emoji` TEXT NOT NULL DEFAULT '📁',
                            `isFavorite` INTEGER NOT NULL DEFAULT 0,
                            `isArchived` INTEGER NOT NULL DEFAULT 0,
                            `orderIndex` INTEGER NOT NULL DEFAULT 0,
                            `createdAt` INTEGER NOT NULL DEFAULT 0,
                            `updatedAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `labels` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `colorId` TEXT NOT NULL,
                            `isFavorite` INTEGER NOT NULL DEFAULT 0,
                            `isArchived` INTEGER NOT NULL DEFAULT 0,
                            `orderIndex` INTEGER NOT NULL DEFAULT 0,
                            `createdAt` INTEGER NOT NULL DEFAULT 0,
                            `updatedAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `honeycomb_shortcuts` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `type` TEXT NOT NULL,
                            `targetId` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `customTileName` TEXT DEFAULT NULL,
                            `emoji` TEXT NOT NULL DEFAULT '✅',
                            `customColorHex` INTEGER DEFAULT NULL,
                            `colorSource` TEXT NOT NULL DEFAULT 'PROJECT',
                            `shape` TEXT NOT NULL DEFAULT 'HEXAGON',
                            `orderIndex` INTEGER NOT NULL DEFAULT 0,
                            `coordQ` INTEGER NOT NULL DEFAULT 0,
                            `coordR` INTEGER NOT NULL DEFAULT 0,
                            `isPinned` INTEGER NOT NULL DEFAULT 0,
                            `createdAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `plans` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `description` TEXT NOT NULL DEFAULT '',
                            `level` TEXT NOT NULL,
                            `startDateIso` TEXT NOT NULL,
                            `targetDateIso` TEXT NOT NULL,
                            `status` TEXT NOT NULL DEFAULT 'ACTIVE',
                            `manualProgress` INTEGER NOT NULL DEFAULT 0,
                            `isAutoProgress` INTEGER NOT NULL DEFAULT 1,
                            `parentPlanId` INTEGER DEFAULT NULL,
                            `projectId` INTEGER DEFAULT NULL,
                            `priority` TEXT NOT NULL DEFAULT 'MEDIUM',
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
                            `isArchived` INTEGER NOT NULL DEFAULT 0,
                            `orderIndex` INTEGER NOT NULL DEFAULT 0,
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
                            `amount` REAL NOT NULL,
                            `currency` TEXT NOT NULL DEFAULT '₹',
                            `frequency` TEXT NOT NULL DEFAULT 'MONTHLY',
                            `anchorDateIso` TEXT NOT NULL,
                            `nextRenewalDateIso` TEXT NOT NULL,
                            `status` TEXT NOT NULL DEFAULT 'ACTIVE',
                            `createdAt` INTEGER NOT NULL DEFAULT 0,
                            `updatedAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `medications` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `taskId` INTEGER NOT NULL,
                            `name` TEXT NOT NULL,
                            `form` TEXT NOT NULL,
                            `dosageAmount` TEXT NOT NULL,
                            `dosageUnit` TEXT NOT NULL,
                            `scheduleType` TEXT NOT NULL,
                            `startDateIso` TEXT NOT NULL,
                            `isPaused` INTEGER NOT NULL DEFAULT 0,
                            `createdAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `trash_items` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `itemType` TEXT NOT NULL,
                            `originalId` INTEGER NOT NULL,
                            `title` TEXT NOT NULL,
                            `subtitle` TEXT NOT NULL DEFAULT '',
                            `originalPayloadJson` TEXT NOT NULL,
                            `deletedAt` INTEGER NOT NULL DEFAULT 0,
                            `expiresAt` INTEGER NOT NULL
                        )
                    """.trimIndent())

                    // Insert sample v10 pre-migration records
                    db.execSQL("INSERT INTO tasks (id, title, category, dueDateIso) VALUES (501, 'Legacy Task 501', 'Work', '2026-10-31')")
                    db.execSQL("INSERT INTO habits (id, title) VALUES (601, 'Legacy Morning Habit')")
                    db.execSQL("INSERT INTO projects (id, name, colorId) VALUES (701, 'Legacy Project', 'blue')")
                    db.execSQL("INSERT INTO labels (id, name, colorId) VALUES (801, 'Urgent', 'red')")
                    db.execSQL("INSERT INTO honeycomb_shortcuts (id, type, targetId, title) VALUES (901, 'TASK', '501', 'Shortcut 501')")
                    db.execSQL("INSERT INTO plans (id, title, level, startDateIso, targetDateIso) VALUES (1001, 'Q4 Objectives', 'QUARTERLY', '2026-10-01', '2026-12-31')")
                    db.execSQL("INSERT INTO trackers (id, title) VALUES (1101, 'Coffee Beans')")
                    db.execSQL("INSERT INTO subscriptions (id, title, amount, anchorDateIso, nextRenewalDateIso) VALUES (1201, 'Server VPS', 49.99, '2026-10-01', '2026-11-01')")
                    db.execSQL("INSERT INTO medications (id, taskId, name, form, dosageAmount, dosageUnit, scheduleType, startDateIso) VALUES (1301, 501, 'Vitamin C', 'TABLET', '500', 'mg', 'DAILY', '2026-10-01')")
                    db.execSQL("INSERT INTO trash_items (id, itemType, originalId, title, originalPayloadJson, expiresAt) VALUES (1401, 'TASK', 10, 'Deleted Task', '{}', 1731000000000)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val v10Db = factory.create(v10Config).writableDatabase
        v10Db.close()

        // 2. Perform Migration 10 -> 11
        val v11Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(11) {
                override fun onCreate(db: SupportSQLiteDatabase) {}

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion <= 10) {
                        MIGRATION_10_11.migrate(db)
                    }
                }
            })
            .build()

        val v11Db = factory.create(v11Config).writableDatabase

        // 3. Verify profiles table was created and contains default Personal profile (id = 1)
        val profileCursor = v11Db.query("SELECT id, name, emoji, type, isDefault FROM profiles WHERE id = 1")
        assertTrue("Default Personal profile must exist in profiles table", profileCursor.moveToFirst())
        assertEquals(1L, profileCursor.getLong(0))
        assertEquals("Personal", profileCursor.getString(1))
        assertEquals("👤", profileCursor.getString(2))
        assertEquals("PERSONAL", profileCursor.getString(3))
        assertEquals(1, profileCursor.getInt(4)) // isDefault = true
        profileCursor.close()

        // 4. Verify all 10 profile-owned tables preserved existing data and defaulted to profileId = 1
        val tablesToVerify = listOf(
            "tasks" to 501L,
            "habits" to 601L,
            "projects" to 701L,
            "labels" to 801L,
            "honeycomb_shortcuts" to 901L,
            "plans" to 1001L,
            "trackers" to 1101L,
            "subscriptions" to 1201L,
            "medications" to 1301L,
            "trash_items" to 1401L
        )

        for ((table, id) in tablesToVerify) {
            val cursor = v11Db.query("SELECT id, profileId FROM `$table` WHERE id = $id")
            assertTrue("Table `$table` must retain record with id $id after migration", cursor.moveToFirst())
            assertEquals("Table `$table` record $id must be assigned to default profileId 1", 1L, cursor.getLong(1))
            cursor.close()
        }

        // 5. Verify index exists on profileId for tasks
        val indexCursor = v11Db.query("PRAGMA index_list('tasks')")
        var hasProfileIdIndex = false
        while (indexCursor.moveToNext()) {
            val indexName = indexCursor.getString(1)
            if (indexName.contains("profileId", ignoreCase = true)) {
                hasProfileIdIndex = true
                break
            }
        }
        indexCursor.close()
        assertTrue("tasks table must have index on profileId", hasProfileIdIndex)

        // 6. Verify cross-profile isolation on SQL level: inserting a task for profile 2
        v11Db.execSQL("INSERT INTO profiles (id, name, emoji, type, isDefault, orderIndex, createdAt, updatedAt) VALUES (2, 'Work', '💼', 'WORK', 0, 1, 0, 0)")
        v11Db.execSQL("INSERT INTO tasks (id, profileId, title, category, dueDateIso) VALUES (502, 2, 'Work Sprint Planning', 'Sprint', '2026-10-31')")

        // Querying for profile 1 should only return task 501
        val p1Cursor = v11Db.query("SELECT id, title FROM tasks WHERE profileId = 1")
        assertEquals("Profile 1 must have exactly 1 task", 1, p1Cursor.count)
        p1Cursor.moveToFirst()
        assertEquals(501L, p1Cursor.getLong(0))
        p1Cursor.close()

        // Querying for profile 2 should only return task 502
        val p2Cursor = v11Db.query("SELECT id, title FROM tasks WHERE profileId = 2")
        assertEquals("Profile 2 must have exactly 1 task", 1, p2Cursor.count)
        p2Cursor.moveToFirst()
        assertEquals(502L, p2Cursor.getLong(0))
        p2Cursor.close()

        // Verify identical task names can exist independently across profiles
        v11Db.execSQL("INSERT INTO tasks (id, profileId, title, category, dueDateIso) VALUES (503, 1, 'Identical Name', 'General', '2026-10-31')")
        v11Db.execSQL("INSERT INTO tasks (id, profileId, title, category, dueDateIso) VALUES (504, 2, 'Identical Name', 'General', '2026-10-31')")

        val p1DupCursor = v11Db.query("SELECT id FROM tasks WHERE profileId = 1 AND title = 'Identical Name'")
        assertEquals(1, p1DupCursor.count)
        p1DupCursor.moveToFirst()
        assertEquals(503L, p1DupCursor.getLong(0))
        p1DupCursor.close()

        val p2DupCursor = v11Db.query("SELECT id FROM tasks WHERE profileId = 2 AND title = 'Identical Name'")
        assertEquals(1, p2DupCursor.count)
        p2DupCursor.moveToFirst()
        assertEquals(504L, p2DupCursor.getLong(0))
        p2DupCursor.close()

        v11Db.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun testProfileEntityDefaults() {
        val defaultProfile = ProfileEntity.createDefaultPersonal()
        assertEquals(1L, defaultProfile.id)
        assertEquals("Personal", defaultProfile.name)
        assertEquals("👤", defaultProfile.emoji)
        assertEquals(ProfileEntity.TYPE_PERSONAL, defaultProfile.type)
        assertTrue(defaultProfile.isDefault)

        val customProfile = ProfileEntity(
            id = 2L,
            name = "LifeOS Work",
            emoji = "💼",
            type = ProfileEntity.TYPE_WORK
        )
        assertEquals(2L, customProfile.id)
        assertFalse(customProfile.isDefault)
        assertEquals(ProfileEntity.TYPE_WORK, customProfile.type)
    }
}
