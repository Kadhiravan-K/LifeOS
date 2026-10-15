package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ChallengeDao
import com.example.data.local.dao.HabitDao
import com.example.data.local.dao.HoneycombShortcutDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.WaterLogDao
import com.example.data.local.entities.ChallengeEntity
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.HoneycombShortcutEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.WaterLogEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN durationMinutes INTEGER NOT NULL DEFAULT 30")
        db.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceEndDateIso TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE tasks ADD COLUMN skippedDatesJson TEXT NOT NULL DEFAULT '[]'")
        db.execSQL("ALTER TABLE tasks ADD COLUMN rescheduledDatesJson TEXT NOT NULL DEFAULT '{}'")
        db.execSQL("ALTER TABLE tasks ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE tasks ADD COLUMN isReminderEnabled INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tasks ADD COLUMN reminderMinutesBefore INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tasks ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tasks ADD COLUMN isPaused INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
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
                `createdAt` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
    }
}

@Database(
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        ChallengeEntity::class,
        WaterLogEntity::class,
        HoneycombShortcutEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun waterLogDao(): WaterLogDao
    abstract fun honeycombShortcutDao(): HoneycombShortcutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lifeos_database.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
