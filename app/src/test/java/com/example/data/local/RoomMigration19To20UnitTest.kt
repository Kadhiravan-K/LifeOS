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
class RoomMigration19To20UnitTest {

    private fun createV19Database(context: Context, dbName: String): SupportSQLiteDatabase {
        context.deleteDatabase(dbName)
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(19) {
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
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        return factory.create(config).writableDatabase
    }

    @Test
    fun testMigration19To20CreatesProfileSecurityTable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = createV19Database(context, "test_migration_19_20.db")

        // Run migration
        MIGRATION_19_20.migrate(db)

        // Verify table creation
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='profile_security'")
        assertTrue("profile_security table must exist after migration 19->20", cursor.moveToFirst())
        cursor.close()

        // Verify column structure
        val pragmaCursor = db.query("PRAGMA table_info(`profile_security`)")
        val columns = mutableListOf<String>()
        while (pragmaCursor.moveToNext()) {
            columns.add(pragmaCursor.getString(pragmaCursor.getColumnIndexOrThrow("name")))
        }
        pragmaCursor.close()

        assertTrue(columns.contains("profileId"))
        assertTrue(columns.contains("isLockEnabled"))
        assertTrue(columns.contains("authMethod"))
        assertTrue(columns.contains("credentialHash"))
        assertTrue(columns.contains("salt"))
        assertTrue(columns.contains("biometricEnabled"))
        assertTrue(columns.contains("autoLockTimeoutSeconds"))
        assertTrue(columns.contains("failedAttempts"))
        assertTrue(columns.contains("lockoutUntilEpochMs"))
        assertTrue(columns.contains("updatedAt"))

        // Verify inserting security configuration
        db.execSQL("""
            INSERT INTO `profile_security` (`profileId`, `isLockEnabled`, `authMethod`, `credentialHash`, `salt`, `biometricEnabled`, `autoLockTimeoutSeconds`, `failedAttempts`, `lockoutUntilEpochMs`, `updatedAt`)
            VALUES (1, 1, 'PIN', 'hash123', 'salt123', 1, 300, 0, 0, 2000)
        """.trimIndent())

        val secCursor = db.query("SELECT * FROM `profile_security` WHERE `profileId` = 1")
        assertTrue(secCursor.moveToFirst())
        assertEquals("PIN", secCursor.getString(secCursor.getColumnIndexOrThrow("authMethod")))
        assertEquals(1, secCursor.getInt(secCursor.getColumnIndexOrThrow("isLockEnabled")))
        secCursor.close()

        db.close()
    }
}
