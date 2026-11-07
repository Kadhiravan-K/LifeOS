package com.example.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomMigration20To21UnitTest {

    private fun createV20Database(context: Context, dbName: String): SupportSQLiteDatabase {
        context.deleteDatabase(dbName)
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(20) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `profiles` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `emoji` TEXT NOT NULL,
                            `colorHex` INTEGER NOT NULL,
                            `type` TEXT NOT NULL,
                            `orderIndex` INTEGER NOT NULL,
                            `isDefault` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("INSERT INTO `profiles` (`id`, `name`, `emoji`, `colorHex`, `type`, `orderIndex`, `isDefault`, `createdAt`, `updatedAt`) VALUES (1, 'Personal', '👤', 4282168054, 'PERSONAL', 0, 1, 1000, 1000)")

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `profile_security` (
                            `profileId` INTEGER NOT NULL,
                            `isLockEnabled` INTEGER NOT NULL DEFAULT 0,
                            `authMethod` TEXT NOT NULL DEFAULT 'NONE',
                            `credentialHash` TEXT NOT NULL DEFAULT '',
                            `salt` TEXT NOT NULL DEFAULT '',
                            `biometricEnabled` INTEGER NOT NULL DEFAULT 0,
                            `autoLockTimeoutSeconds` INTEGER NOT NULL DEFAULT 300,
                            `failedAttempts` INTEGER NOT NULL DEFAULT 0,
                            `lockoutUntilEpochMs` INTEGER NOT NULL DEFAULT 0,
                            `updatedAt` INTEGER NOT NULL DEFAULT 0,
                            PRIMARY KEY(`profileId`),
                            FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON DELETE CASCADE
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        return factory.create(config).writableDatabase
    }

    @Test
    fun testMigration20To21CreatesShiftsAndGiveGetTables() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = createV20Database(context, "test_migration_20_21.db")

        // Run migration
        MIGRATION_20_21.migrate(db)

        // 1. Verify shifts table
        val shiftCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='shifts'")
        assertTrue("shifts table must exist after migration 20->21", shiftCursor.moveToFirst())
        shiftCursor.close()

        // 2. Verify shift_rotations table
        val rotCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='shift_rotations'")
        assertTrue("shift_rotations table must exist after migration 20->21", rotCursor.moveToFirst())
        rotCursor.close()

        // 3. Verify shift_assignments table
        val assignCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='shift_assignments'")
        assertTrue("shift_assignments table must exist after migration 20->21", assignCursor.moveToFirst())
        assignCursor.close()

        // 4. Verify give_get_transactions table
        val giveGetCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='give_get_transactions'")
        assertTrue("give_get_transactions table must exist after migration 20->21", giveGetCursor.moveToFirst())
        giveGetCursor.close()

        // Test inserting and querying a shift
        db.execSQL("""
            INSERT INTO `shifts` (`id`, `profileId`, `name`, `colorHex`, `startTime`, `endTime`, `wakeUpTime`, `sleepTime`, `isOvernight`, `shiftHabitIdsJson`, `shiftTaskTitlesJson`, `notes`, `createdAt`, `updatedAt`)
            VALUES (101, 1, 'Night Shift', '#8B5CF6', '22:00', '06:00', '19:30', '10:00', 1, '[]', '[]', 'Overnight work', 1000, 1000)
        """.trimIndent())

        val testShiftQuery = db.query("SELECT * FROM `shifts` WHERE `id` = 101")
        assertTrue(testShiftQuery.moveToFirst())
        assertEquals("Night Shift", testShiftQuery.getString(testShiftQuery.getColumnIndexOrThrow("name")))
        assertEquals(1, testShiftQuery.getInt(testShiftQuery.getColumnIndexOrThrow("isOvernight")))
        assertEquals("22:00", testShiftQuery.getString(testShiftQuery.getColumnIndexOrThrow("startTime")))
        assertEquals("06:00", testShiftQuery.getString(testShiftQuery.getColumnIndexOrThrow("endTime")))
        testShiftQuery.close()

        // Test inserting and querying a Give & Get record
        db.execSQL("""
            INSERT INTO `give_get_transactions` (`id`, `profileId`, `type`, `personName`, `personContactId`, `personPhoneNumber`, `amount`, `returnedAmount`, `itemDescription`, `dateIso`, `expectedReturnDateIso`, `actualReturnDateIso`, `status`, `notes`, `hasReminder`, `reminderDaysBefore`, `createdAt`, `updatedAt`)
            VALUES (201, 1, 'GIVE_LENT_MONEY', 'Alice Smith', NULL, NULL, 50.0, 20.0, 'Lunch bill share', '2026-11-01', '2026-11-08', NULL, 'PARTIALLY_RETURNED', 'Friendly loan', 1, 1, 1000, 1000)
        """.trimIndent())

        val testGiveGetQuery = db.query("SELECT * FROM `give_get_transactions` WHERE `id` = 201")
        assertTrue(testGiveGetQuery.moveToFirst())
        assertEquals("Alice Smith", testGiveGetQuery.getString(testGiveGetQuery.getColumnIndexOrThrow("personName")))
        assertEquals("PARTIALLY_RETURNED", testGiveGetQuery.getString(testGiveGetQuery.getColumnIndexOrThrow("status")))
        assertEquals(50.0, testGiveGetQuery.getDouble(testGiveGetQuery.getColumnIndexOrThrow("amount")), 0.001)
        assertEquals(20.0, testGiveGetQuery.getDouble(testGiveGetQuery.getColumnIndexOrThrow("returnedAmount")), 0.001)
        testGiveGetQuery.close()

        db.close()
    }
}
