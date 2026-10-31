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
import com.example.data.local.dao.ProfileDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.TaskReminderDao
import com.example.data.local.dao.WaterLogDao
import com.example.data.local.entities.ChallengeEntity
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.HoneycombShortcutEntity
import com.example.data.local.entities.LabelEntity
import com.example.data.local.entities.ProfileEntity
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

val MIGRATION_6_7 = object : Migration(6, 7) {
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

        addColumnIfNotExists("honeycomb_shortcuts", "customTileName", "TEXT DEFAULT NULL")
        addColumnIfNotExists("tasks", "isQuickReminder", "INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `medications` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `taskId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `form` TEXT NOT NULL,
                `dosageAmount` TEXT NOT NULL,
                `dosageUnit` TEXT NOT NULL,
                `strength` TEXT NOT NULL DEFAULT '',
                `instructions` TEXT NOT NULL DEFAULT '',
                `scheduleType` TEXT NOT NULL,
                `mealPeriodsJson` TEXT NOT NULL DEFAULT '[]',
                `fixedTimesJson` TEXT NOT NULL DEFAULT '[]',
                `intervalHours` INTEGER NOT NULL DEFAULT 4,
                `intervalModel` TEXT NOT NULL DEFAULT 'FIXED_SCHEDULE',
                `intervalAnchorTime` TEXT NOT NULL DEFAULT '08:00',
                `allowOvernight` INTEGER NOT NULL DEFAULT 0,
                `selectedDaysOfWeek` TEXT NOT NULL DEFAULT '',
                `startDateIso` TEXT NOT NULL,
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
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_medications_taskId` ON `medications` (`taskId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_medications_isPaused` ON `medications` (`isPaused`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `medication_doses` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `medicationId` INTEGER NOT NULL,
                `taskId` INTEGER NOT NULL,
                `medicationName` TEXT NOT NULL,
                `medicationForm` TEXT NOT NULL,
                `dosage` TEXT NOT NULL,
                `instructions` TEXT NOT NULL DEFAULT '',
                `scheduledDateIso` TEXT NOT NULL,
                `scheduledTime` TEXT NOT NULL,
                `status` TEXT NOT NULL DEFAULT 'SCHEDULED',
                `actionTimestamp` INTEGER DEFAULT NULL,
                `snoozeCount` INTEGER NOT NULL DEFAULT 0,
                `followUpCount` INTEGER NOT NULL DEFAULT 0,
                `notes` TEXT NOT NULL DEFAULT ''
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_medication_doses_medicationId` ON `medication_doses` (`medicationId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_medication_doses_taskId` ON `medication_doses` (`taskId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_medication_doses_scheduledDateIso` ON `medication_doses` (`scheduledDateIso`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_medication_doses_status` ON `medication_doses` (`status`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_medication_doses_medicationId_scheduledDateIso_scheduledTime` ON `medication_doses` (`medicationId`, `scheduledDateIso`, `scheduledTime`)")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `honeycomb_shortcuts` ADD COLUMN `colorSource` TEXT NOT NULL DEFAULT 'PROJECT'")
        db.execSQL("ALTER TABLE `honeycomb_shortcuts` ADD COLUMN `shape` TEXT NOT NULL DEFAULT 'HEXAGON'")
        db.execSQL("UPDATE `honeycomb_shortcuts` SET `colorSource` = 'CUSTOM' WHERE `customColorHex` IS NOT NULL")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- Smart Tracker Tables ---
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
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_trackers_projectId` ON `trackers` (`projectId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_trackers_isArchived` ON `trackers` (`isArchived`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `tracker_cycles` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `trackerId` INTEGER NOT NULL,
                `cycleName` TEXT NOT NULL DEFAULT '',
                `purchaseId` INTEGER DEFAULT NULL,
                `purchaseCost` REAL NOT NULL DEFAULT 0.0,
                `startDateIso` TEXT NOT NULL,
                `finishDateIso` TEXT DEFAULT NULL,
                `status` TEXT NOT NULL DEFAULT 'ACTIVE',
                `isPaused` INTEGER NOT NULL DEFAULT 0,
                `startOdometer` REAL NOT NULL DEFAULT 0.0,
                `currentOdometer` REAL NOT NULL DEFAULT 0.0,
                `finishOdometer` REAL DEFAULT NULL,
                `initialQuantity` REAL NOT NULL DEFAULT 0.0,
                `remainingQuantity` REAL NOT NULL DEFAULT 0.0,
                `notes` TEXT NOT NULL DEFAULT '',
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracker_cycles_trackerId` ON `tracker_cycles` (`trackerId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracker_cycles_status` ON `tracker_cycles` (`status`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `tracker_usage_entries` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `cycleId` INTEGER NOT NULL,
                `dateIso` TEXT NOT NULL,
                `odometerReading` REAL DEFAULT NULL,
                `quantityDeducted` REAL DEFAULT NULL,
                `manualRemainingQuantity` REAL DEFAULT NULL,
                `notes` TEXT NOT NULL DEFAULT '',
                `timestamp` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracker_usage_entries_cycleId` ON `tracker_usage_entries` (`cycleId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `tracker_purchases` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `trackerId` INTEGER NOT NULL,
                `purchaseDateIso` TEXT NOT NULL,
                `quantity` REAL NOT NULL DEFAULT 0.0,
                `unitPrice` REAL NOT NULL DEFAULT 0.0,
                `totalCost` REAL NOT NULL DEFAULT 0.0,
                `notes` TEXT NOT NULL DEFAULT '',
                `createdAt` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracker_purchases_trackerId` ON `tracker_purchases` (`trackerId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `tracker_inventory_adjustments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `trackerId` INTEGER NOT NULL,
                `dateIso` TEXT NOT NULL,
                `deltaQuantity` REAL NOT NULL DEFAULT 0.0,
                `isRefillExchange` INTEGER NOT NULL DEFAULT 0,
                `reason` TEXT NOT NULL DEFAULT '',
                `timestamp` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracker_inventory_adjustments_trackerId` ON `tracker_inventory_adjustments` (`trackerId`)")

        // --- Subscription Tables ---
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
                `anchorDateIso` TEXT NOT NULL,
                `anchorDayOfMonth` INTEGER NOT NULL DEFAULT 1,
                `nextRenewalDateIso` TEXT NOT NULL,
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
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_projectId` ON `subscriptions` (`projectId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_status` ON `subscriptions` (`status`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `subscription_bills` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `subscriptionId` INTEGER NOT NULL,
                `dueDateIso` TEXT NOT NULL,
                `expectedAmount` REAL NOT NULL DEFAULT 0.0,
                `currency` TEXT NOT NULL DEFAULT '₹',
                `status` TEXT NOT NULL DEFAULT 'UNPAID',
                `notes` TEXT NOT NULL DEFAULT '',
                `createdAt` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscription_bills_subscriptionId` ON `subscription_bills` (`subscriptionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscription_bills_dueDateIso` ON `subscription_bills` (`dueDateIso`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscription_bills_status` ON `subscription_bills` (`status`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `subscription_payments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `billId` INTEGER NOT NULL,
                `subscriptionId` INTEGER NOT NULL,
                `amountPaid` REAL NOT NULL DEFAULT 0.0,
                `paymentDateIso` TEXT NOT NULL,
                `paymentMethod` TEXT NOT NULL DEFAULT '',
                `transactionRef` TEXT NOT NULL DEFAULT '',
                `notes` TEXT NOT NULL DEFAULT '',
                `timestamp` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscription_payments_billId` ON `subscription_payments` (`billId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscription_payments_subscriptionId` ON `subscription_payments` (`subscriptionId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `subscription_price_history` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `subscriptionId` INTEGER NOT NULL,
                `oldAmount` REAL NOT NULL DEFAULT 0.0,
                `newAmount` REAL NOT NULL DEFAULT 0.0,
                `effectiveDateIso` TEXT NOT NULL,
                `reason` TEXT NOT NULL DEFAULT '',
                `createdAt` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscription_price_history_subscriptionId` ON `subscription_price_history` (`subscriptionId`)")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create profiles table
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
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_profiles_type` ON `profiles` (`type`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_profiles_orderIndex` ON `profiles` (`orderIndex`)")

        // 2. Insert default Personal profile with id = 1
        db.execSQL("""
            INSERT OR IGNORE INTO `profiles` (`id`, `name`, `emoji`, `colorHex`, `type`, `orderIndex`, `isDefault`, `createdAt`, `updatedAt`)
            VALUES (1, 'Personal', '👤', 4282168054, 'PERSONAL', 0, 1, 1730000000000, 1730000000000)
        """.trimIndent())

        // 3. Add profileId column with default 1 and create indexes across all 10 profile-scoped root tables
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_profileId` ON `tasks` (`profileId`)")

        db.execSQL("ALTER TABLE `habits` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_profileId` ON `habits` (`profileId`)")

        db.execSQL("ALTER TABLE `projects` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_profileId` ON `projects` (`profileId`)")

        db.execSQL("ALTER TABLE `labels` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_labels_profileId` ON `labels` (`profileId`)")

        db.execSQL("ALTER TABLE `honeycomb_shortcuts` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_honeycomb_shortcuts_profileId` ON `honeycomb_shortcuts` (`profileId`)")

        db.execSQL("ALTER TABLE `plans` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plans_profileId` ON `plans` (`profileId`)")

        db.execSQL("ALTER TABLE `trackers` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_trackers_profileId` ON `trackers` (`profileId`)")

        db.execSQL("ALTER TABLE `subscriptions` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_profileId` ON `subscriptions` (`profileId`)")

        db.execSQL("ALTER TABLE `medications` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_medications_profileId` ON `medications` (`profileId`)")

        db.execSQL("ALTER TABLE `trash_items` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_trash_items_profileId` ON `trash_items` (`profileId`)")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `profile_surveys` (
                `profileId` INTEGER PRIMARY KEY NOT NULL,
                `displayName` TEXT DEFAULT NULL,
                `gender` TEXT DEFAULT NULL,
                `sleepingTime` TEXT DEFAULT NULL,
                `dailyWaterGoalMl` INTEGER DEFAULT NULL,
                `discoverySource` TEXT DEFAULT NULL,
                `discoverySocialPlatform` TEXT DEFAULT NULL,
                `discoveryOtherDetail` TEXT DEFAULT NULL,
                `isCompleted` INTEGER NOT NULL DEFAULT 0,
                `isSkipped` INTEGER NOT NULL DEFAULT 0,
                `completedAt` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_profile_surveys_profileId` ON `profile_surveys` (`profileId`)")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `recurrenceWindowStartDay` INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `recurrenceWindowEndDay` INTEGER DEFAULT NULL")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. project_workspace_items
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `project_workspace_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `profileId` INTEGER NOT NULL DEFAULT 1,
                `projectId` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `status` TEXT NOT NULL DEFAULT 'NOT_STARTED',
                `progress` INTEGER NOT NULL DEFAULT 0,
                `category` TEXT NOT NULL DEFAULT 'Feature',
                `version` TEXT NOT NULL DEFAULT 'V1',
                `dueDateIso` TEXT DEFAULT NULL,
                `notes` TEXT NOT NULL DEFAULT '',
                `priority` TEXT NOT NULL DEFAULT 'P2',
                `customPropertiesJson` TEXT NOT NULL DEFAULT '{}',
                `orderIndex` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_workspace_items_profileId` ON `project_workspace_items` (`profileId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_workspace_items_projectId` ON `project_workspace_items` (`projectId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_workspace_items_status` ON `project_workspace_items` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_workspace_items_category` ON `project_workspace_items` (`category`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_workspace_items_version` ON `project_workspace_items` (`version`)")

        // 2. project_workspace_views
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `project_workspace_views` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `profileId` INTEGER NOT NULL DEFAULT 1,
                `projectId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `viewType` TEXT NOT NULL DEFAULT 'TABLE',
                `filterStatus` TEXT DEFAULT NULL,
                `filterCategory` TEXT DEFAULT NULL,
                `filterVersion` TEXT DEFAULT NULL,
                `sortBy` TEXT NOT NULL DEFAULT 'orderIndex',
                `sortAscending` INTEGER NOT NULL DEFAULT 1,
                `groupBy` TEXT NOT NULL DEFAULT 'status',
                `visiblePropertiesJson` TEXT NOT NULL DEFAULT '["status","progress","category","version","dueDate"]',
                `isDefault` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_workspace_views_profileId` ON `project_workspace_views` (`profileId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_workspace_views_projectId` ON `project_workspace_views` (`projectId`)")

        // 3. project_custom_statuses
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `project_custom_statuses` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `profileId` INTEGER NOT NULL DEFAULT 1,
                `projectId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `colorHex` INTEGER NOT NULL DEFAULT 4282168054,
                `orderIndex` INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_custom_statuses_profileId` ON `project_custom_statuses` (`profileId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_custom_statuses_projectId` ON `project_custom_statuses` (`projectId`)")
    }
}

@Database(
    entities = [
        ProfileEntity::class,
        com.example.data.local.entities.ProfileSurveyEntity::class,
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
        com.example.data.local.entities.RewardRedemptionEntity::class,
        com.example.data.local.entities.MedicationEntity::class,
        com.example.data.local.entities.MedicationDoseEntity::class,
        com.example.data.local.entities.TrackerEntity::class,
        com.example.data.local.entities.UsageCycleEntity::class,
        com.example.data.local.entities.UsageEntryEntity::class,
        com.example.data.local.entities.TrackerPurchaseEntity::class,
        com.example.data.local.entities.InventoryAdjustmentEntity::class,
        com.example.data.local.entities.SubscriptionEntity::class,
        com.example.data.local.entities.BillingOccurrenceEntity::class,
        com.example.data.local.entities.SubscriptionPaymentEntity::class,
        com.example.data.local.entities.PriceHistoryEntity::class,
        com.example.data.local.entities.ProjectWorkspaceItemEntity::class,
        com.example.data.local.entities.ProjectWorkspaceViewEntity::class,
        com.example.data.local.entities.ProjectCustomStatusEntity::class
    ],
    version = 14,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun profileSurveyDao(): com.example.data.local.dao.ProfileSurveyDao
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
    abstract fun medicationDao(): com.example.data.local.dao.MedicationDao
    abstract fun trackerDao(): com.example.data.local.dao.TrackerDao
    abstract fun subscriptionDao(): com.example.data.local.dao.SubscriptionDao
    abstract fun projectWorkspaceDao(): com.example.data.local.dao.ProjectWorkspaceDao

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
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            db.execSQL("""
                                INSERT OR IGNORE INTO `profiles` (`id`, `name`, `emoji`, `colorHex`, `type`, `orderIndex`, `isDefault`, `createdAt`, `updatedAt`)
                                VALUES (1, 'Personal', '👤', 4282168054, 'PERSONAL', 0, 1, 1730000000000, 1730000000000)
                            """.trimIndent())
                        }
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("""
                                INSERT OR IGNORE INTO `profiles` (`id`, `name`, `emoji`, `colorHex`, `type`, `orderIndex`, `isDefault`, `createdAt`, `updatedAt`)
                                VALUES (1, 'Personal', '👤', 4282168054, 'PERSONAL', 0, 1, 1730000000000, 1730000000000)
                            """.trimIndent())
                        }
                    })
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
