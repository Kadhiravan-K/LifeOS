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
import com.example.data.local.dao.LabelDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.TaskReminderDao
import com.example.data.local.dao.WaterLogDao
import com.example.data.local.entities.ChallengeEntity
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.HoneycombShortcutEntity
import com.example.data.local.entities.LabelEntity
import com.example.data.local.entities.ProjectEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.TaskLabelCrossRef
import com.example.data.local.entities.TaskReminderEntity
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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Extend tasks table
        db.execSQL("ALTER TABLE tasks ADD COLUMN projectId INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE tasks ADD COLUMN deadlineIso TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE tasks ADD COLUMN isAllDay INTEGER NOT NULL DEFAULT 1")

        // 2. Projects table
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

        // 3. Labels table
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

        // 4. Task-Label cross-reference table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `task_label_cross_ref` (
                `taskId` INTEGER NOT NULL,
                `labelId` INTEGER NOT NULL,
                PRIMARY KEY(`taskId`, `labelId`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_label_cross_ref_taskId` ON `task_label_cross_ref` (`taskId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_label_cross_ref_labelId` ON `task_label_cross_ref` (`labelId`)")

        // 5. Task Reminders table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `task_reminders` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `taskId` INTEGER NOT NULL,
                `minutesBefore` INTEGER NOT NULL DEFAULT 0,
                `customTime` TEXT DEFAULT NULL,
                `type` TEXT NOT NULL DEFAULT 'OFFSET',
                `isEnabled` INTEGER NOT NULL DEFAULT 1,
                `createdAt` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_reminders_taskId` ON `task_reminders` (`taskId`)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN schedulingMode TEXT NOT NULL DEFAULT 'DURATION'")
        db.execSQL("ALTER TABLE tasks ADD COLUMN durationAmount INTEGER NOT NULL DEFAULT 30")
        db.execSQL("ALTER TABLE tasks ADD COLUMN durationUnit TEXT NOT NULL DEFAULT 'MINUTES'")
        db.execSQL("ALTER TABLE tasks ADD COLUMN deadlineTime TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceEndCondition TEXT NOT NULL DEFAULT 'NEVER'")
        db.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceCount INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceUnit TEXT NOT NULL DEFAULT 'DAYS'")
        db.execSQL("ALTER TABLE task_reminders ADD COLUMN referenceType TEXT NOT NULL DEFAULT 'START_TIME'")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        fun addColumnIfNotExists(table: String, column: String, definition: String) {
            val cursor = db.query("PRAGMA table_info($table)")
            var exists = false
            while (cursor.moveToNext()) {
                val colIndex = cursor.getColumnIndex("name")
                if (colIndex >= 0 && cursor.getString(colIndex).equals(column, ignoreCase = true)) {
                    exists = true
                    break
                }
            }
            cursor.close()
            if (!exists) {
                db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
            }
        }

        addColumnIfNotExists("honeycomb_shortcuts", "coordQ", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfNotExists("honeycomb_shortcuts", "coordR", "INTEGER NOT NULL DEFAULT 0")

        // 1. trash_items
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
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_trash_items_itemType` ON `trash_items` (`itemType`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_trash_items_expiresAt` ON `trash_items` (`expiresAt`)")

        // 2. plans
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
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plans_level` ON `plans` (`level`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plans_status` ON `plans` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plans_parentPlanId` ON `plans` (`parentPlanId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plans_projectId` ON `plans` (`projectId`)")

        // 3. plan_milestones
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `plan_milestones` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `planId` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `isCompleted` INTEGER NOT NULL DEFAULT 0,
                `orderIndex` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_milestones_planId` ON `plan_milestones` (`planId`)")

        // 4. plan_task_cross_refs
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `plan_task_cross_refs` (
                `planId` INTEGER NOT NULL,
                `taskId` INTEGER NOT NULL,
                PRIMARY KEY(`planId`, `taskId`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_task_cross_refs_taskId` ON `plan_task_cross_refs` (`taskId`)")

        // 5. gamification_profile
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `gamification_profile` (
                `id` INTEGER PRIMARY KEY NOT NULL,
                `currentXp` INTEGER NOT NULL DEFAULT 0,
                `currentCoins` INTEGER NOT NULL DEFAULT 0,
                `currentLevel` INTEGER NOT NULL DEFAULT 1,
                `hunterRank` TEXT NOT NULL DEFAULT 'E',
                `updatedAt` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        // 6. xp_events
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `xp_events` (
                `eventKey` TEXT PRIMARY KEY NOT NULL,
                `xpAwarded` INTEGER NOT NULL,
                `coinsAwarded` INTEGER NOT NULL,
                `description` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        // 7. achievements
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `achievements` (
                `code` TEXT PRIMARY KEY NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `tier` TEXT NOT NULL,
                `xpReward` INTEGER NOT NULL,
                `coinsReward` INTEGER NOT NULL,
                `isUnlocked` INTEGER NOT NULL DEFAULT 0,
                `unlockedAt` INTEGER DEFAULT NULL
            )
        """.trimIndent())

        // 8. rewards
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `rewards` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT NOT NULL DEFAULT '',
                `coinCost` INTEGER NOT NULL DEFAULT 50,
                `iconEmoji` TEXT NOT NULL DEFAULT '🎁',
                `isArchived` INTEGER NOT NULL DEFAULT 0,
                `redemptionCount` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        // 9. reward_redemptions
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `reward_redemptions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `rewardId` INTEGER NOT NULL,
                `rewardTitle` TEXT NOT NULL,
                `coinsSpent` INTEGER NOT NULL,
                `timestamp` INTEGER NOT NULL DEFAULT 0
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
        HoneycombShortcutEntity::class,
        ProjectEntity::class,
        LabelEntity::class,
        TaskLabelCrossRef::class,
        TaskReminderEntity::class,
        com.example.data.local.entities.TrashItemEntity::class,
        com.example.data.local.entities.PlanEntity::class,
        com.example.data.local.entities.PlanMilestoneEntity::class,
        com.example.data.local.entities.PlanTaskCrossRef::class,
        com.example.data.local.entities.GamificationProfileEntity::class,
        com.example.data.local.entities.XpEventEntity::class,
        com.example.data.local.entities.AchievementEntity::class,
        com.example.data.local.entities.RewardItemEntity::class,
        com.example.data.local.entities.RewardRedemptionEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun waterLogDao(): WaterLogDao
    abstract fun honeycombShortcutDao(): HoneycombShortcutDao
    abstract fun projectDao(): ProjectDao
    abstract fun labelDao(): LabelDao
    abstract fun taskReminderDao(): TaskReminderDao
    abstract fun trashDao(): com.example.data.local.dao.TrashDao
    abstract fun planDao(): com.example.data.local.dao.PlanDao
    abstract fun gamificationDao(): com.example.data.local.dao.GamificationDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
