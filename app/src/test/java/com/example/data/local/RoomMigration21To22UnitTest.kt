package com.example.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomMigration21To22UnitTest {

    @Test
    fun migrationAddsNullableTemplateIdentityAndUniqueProfileIndex() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_21_22.db"
        context.deleteDatabase(dbName)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(21) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE profiles (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                name TEXT NOT NULL,
                                emoji TEXT NOT NULL,
                                colorHex INTEGER NOT NULL,
                                type TEXT NOT NULL,
                                orderIndex INTEGER NOT NULL,
                                isDefault INTEGER NOT NULL,
                                createdAt INTEGER NOT NULL,
                                updatedAt INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE qr_codes (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                profileId INTEGER NOT NULL,
                                title TEXT NOT NULL,
                                type TEXT NOT NULL,
                                fieldsJson TEXT NOT NULL,
                                payload TEXT NOT NULL,
                                createdAt INTEGER NOT NULL,
                                updatedAt INTEGER NOT NULL,
                                FOREIGN KEY(profileId) REFERENCES profiles(id) ON DELETE CASCADE
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX index_qr_codes_profileId ON qr_codes(profileId)")
                        db.execSQL("CREATE INDEX index_qr_codes_profileId_updatedAt ON qr_codes(profileId, updatedAt)")
                        db.execSQL(
                            "INSERT INTO profiles VALUES (1, 'Personal', '👤', 1, 'PERSONAL', 0, 1, 1000, 1000)"
                        )
                        db.execSQL(
                            "INSERT INTO qr_codes (profileId, title, type, fieldsJson, payload, createdAt, updatedAt) " +
                                "VALUES (1, 'Existing QR', 'TEXT', '[\"keep\"]', 'keep', 1000, 1000)"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )

        val db = helper.writableDatabase
        MIGRATION_21_22.migrate(db)
        val columns = db.query("PRAGMA table_info(qr_codes)")
        val names = mutableSetOf<String>()
        while (columns.moveToNext()) names += columns.getString(columns.getColumnIndexOrThrow("name"))
        columns.close()
        assertTrue(names.contains("sourceTemplateId"))
        val preserved = db.query("SELECT title, payload, sourceTemplateId FROM qr_codes WHERE id = 1")
        assertTrue(preserved.moveToFirst())
        assertTrue(preserved.getString(preserved.getColumnIndexOrThrow("title")) == "Existing QR")
        assertTrue(preserved.getString(preserved.getColumnIndexOrThrow("payload")) == "keep")
        assertTrue(preserved.isNull(preserved.getColumnIndexOrThrow("sourceTemplateId")))
        preserved.close()

        val index = db.query("PRAGMA index_list(qr_codes)")
        var uniqueIndexFound = false
        while (index.moveToNext()) {
            if (index.getString(index.getColumnIndexOrThrow("name")) == "index_qr_codes_profileId_sourceTemplateId") {
                uniqueIndexFound = index.getInt(index.getColumnIndexOrThrow("unique")) == 1
            }
        }
        index.close()
        assertTrue(uniqueIndexFound)

        helper.close()
        context.deleteDatabase(dbName)
    }
}
