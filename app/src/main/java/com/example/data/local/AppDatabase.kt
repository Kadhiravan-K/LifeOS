package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ChallengeDao
import com.example.data.local.dao.HabitDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.WaterLogDao
import com.example.data.local.entities.ChallengeEntity
import com.example.data.local.entities.HabitEntity
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

@Database(
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        ChallengeEntity::class,
        WaterLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun waterLogDao(): WaterLogDao

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
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
