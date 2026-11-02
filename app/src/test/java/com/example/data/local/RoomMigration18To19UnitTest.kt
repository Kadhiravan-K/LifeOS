package com.example.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomMigration18To19UnitTest {

    private fun createV18Database(context: Context, dbName: String): SupportSQLiteDatabase {
        context.deleteDatabase(dbName)
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(18) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create base parent tables
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
                        CREATE TABLE IF NOT EXISTS `tasks` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `profileId` INTEGER NOT NULL DEFAULT 1,
                            `title` TEXT NOT NULL,
                            `category` TEXT NOT NULL DEFAULT 'Inbox',
                            `priority` TEXT NOT NULL DEFAULT 'MEDIUM',
                            `dueDateIso` TEXT NOT NULL DEFAULT '',
                            `dueTime` TEXT DEFAULT NULL,
                            `isCompleted` INTEGER NOT NULL DEFAULT 0,
                            `completedAt` INTEGER DEFAULT NULL,
                            `createdAt` INTEGER NOT NULL DEFAULT 0,
                            `orderIndex` INTEGER NOT NULL DEFAULT 0,
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
                            `recurrenceEndCount` INTEGER NOT NULL DEFAULT 0,
                            `subtasksJson` TEXT NOT NULL DEFAULT '[]',
                            `completedDatesJson` TEXT NOT NULL DEFAULT '[]',
                            `recurrenceInterval` INTEGER NOT NULL DEFAULT 1,
                            `recurrenceType` TEXT NOT NULL DEFAULT 'NONE',
                            `recurrenceDaysOfWeek` TEXT NOT NULL DEFAULT ''
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `labels` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `profileId` INTEGER NOT NULL DEFAULT 1,
                            `name` TEXT NOT NULL,
                            `colorId` TEXT NOT NULL DEFAULT 'blue',
                            `isFavorite` INTEGER NOT NULL DEFAULT 0,
                            `isArchived` INTEGER NOT NULL DEFAULT 0,
                            `orderIndex` INTEGER NOT NULL DEFAULT 0,
                            `createdAt` INTEGER NOT NULL DEFAULT 0,
                            `updatedAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `medications` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `profileId` INTEGER NOT NULL DEFAULT 1,
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

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `trackers` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `profileId` INTEGER NOT NULL DEFAULT 1,
                            `projectId` INTEGER DEFAULT NULL,
                            `name` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `trackingType` TEXT NOT NULL,
                            `quantityUnit` TEXT NOT NULL,
                            `distanceUnit` TEXT NOT NULL,
                            `iconEmoji` TEXT NOT NULL DEFAULT '📦',
                            `colorHex` INTEGER NOT NULL DEFAULT 0,
                            `targetQuantity` REAL DEFAULT NULL,
                            `warningThreshold` REAL DEFAULT NULL,
                            `criticalThreshold` REAL DEFAULT NULL,
                            `fullCapacityStock` REAL DEFAULT NULL,
                            `emptyStock` REAL DEFAULT NULL,
                            `isReminderEnabled` INTEGER NOT NULL DEFAULT 0,
                            `reminderDaysInterval` INTEGER NOT NULL DEFAULT 0,
                            `isArchived` INTEGER NOT NULL DEFAULT 0,
                            `createdAt` INTEGER NOT NULL DEFAULT 0,
                            `updatedAt` INTEGER NOT NULL DEFAULT 0,
                            `labelIdsJson` TEXT NOT NULL DEFAULT '[]'
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `tracker_cycles` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `trackerId` INTEGER NOT NULL,
                            `cycleName` TEXT NOT NULL DEFAULT '',
                            `purchaseId` INTEGER DEFAULT NULL,
                            `purchaseCost` REAL NOT NULL DEFAULT 0.0,
                            `startDateIso` TEXT NOT NULL,
                            `endDateIso` TEXT DEFAULT NULL,
                            `status` TEXT NOT NULL,
                            `initialQuantity` REAL DEFAULT NULL,
                            `remainingQuantity` REAL DEFAULT NULL,
                            `notes` TEXT NOT NULL DEFAULT '',
                            `createdAt` INTEGER NOT NULL DEFAULT 0,
                            `updatedAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `subscriptions` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `profileId` INTEGER NOT NULL DEFAULT 1,
                            `projectId` INTEGER DEFAULT NULL,
                            `name` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `billingFrequency` TEXT NOT NULL,
                            `customIntervalMonths` INTEGER NOT NULL DEFAULT 1,
                            `amount` REAL NOT NULL,
                            `currency` TEXT NOT NULL DEFAULT '₹',
                            `firstBillDateIso` TEXT NOT NULL,
                            `cancellationDateIso` TEXT DEFAULT NULL,
                            `status` TEXT NOT NULL,
                            `missingDatePolicy` TEXT NOT NULL DEFAULT 'EXACT_OR_LAST_DAY',
                            `alertStyle` TEXT NOT NULL DEFAULT 'STANDARD',
                            `notes` TEXT NOT NULL DEFAULT '',
                            `isAutoRenew` INTEGER NOT NULL DEFAULT 1,
                            `iconEmoji` TEXT NOT NULL DEFAULT '💳',
                            `colorHex` INTEGER NOT NULL DEFAULT 0,
                            `createdAt` INTEGER NOT NULL DEFAULT 0,
                            `updatedAt` INTEGER NOT NULL DEFAULT 0,
                            `labelIdsJson` TEXT NOT NULL DEFAULT '[]'
                        )
                    """.trimIndent())

                    // Create the 6 target unconstrained v18 child tables
                    db.execSQL("CREATE TABLE IF NOT EXISTS `task_label_cross_ref` (`taskId` INTEGER NOT NULL, `labelId` INTEGER NOT NULL, PRIMARY KEY(`taskId`, `labelId`))")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_label_cross_ref_taskId` ON `task_label_cross_ref` (`taskId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_label_cross_ref_labelId` ON `task_label_cross_ref` (`labelId`)")

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `task_reminders` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `taskId` INTEGER NOT NULL,
                            `minutesBefore` INTEGER NOT NULL,
                            `customTime` TEXT,
                            `type` TEXT NOT NULL,
                            `referenceType` TEXT NOT NULL,
                            `isEnabled` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_reminders_taskId` ON `task_reminders` (`taskId`)")

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `medication_doses` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `medicationId` INTEGER NOT NULL,
                            `taskId` INTEGER NOT NULL,
                            `medicationName` TEXT NOT NULL,
                            `medicationForm` TEXT NOT NULL,
                            `dosage` TEXT NOT NULL,
                            `instructions` TEXT NOT NULL,
                            `scheduledDateIso` TEXT NOT NULL,
                            `scheduledTime` TEXT NOT NULL,
                            `status` TEXT NOT NULL,
                            `actionTimestamp` INTEGER,
                            `snoozeCount` INTEGER NOT NULL,
                            `followUpCount` INTEGER NOT NULL,
                            `notes` TEXT NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_medication_doses_medicationId` ON `medication_doses` (`medicationId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_medication_doses_taskId` ON `medication_doses` (`taskId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_medication_doses_scheduledDateIso` ON `medication_doses` (`scheduledDateIso`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_medication_doses_status` ON `medication_doses` (`status`)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_medication_doses_medicationId_scheduledDateIso_scheduledTime` ON `medication_doses` (`medicationId`, `scheduledDateIso`, `scheduledTime`)")

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `tracker_usage_entries` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `cycleId` INTEGER NOT NULL,
                            `dateIso` TEXT NOT NULL,
                            `odometerReading` REAL,
                            `quantityDeducted` REAL,
                            `manualRemainingQuantity` REAL,
                            `notes` TEXT NOT NULL,
                            `timestamp` INTEGER NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracker_usage_entries_cycleId` ON `tracker_usage_entries` (`cycleId`)")

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `subscription_bills` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `subscriptionId` INTEGER NOT NULL,
                            `dueDateIso` TEXT NOT NULL,
                            `expectedAmount` REAL NOT NULL,
                            `currency` TEXT NOT NULL,
                            `status` TEXT NOT NULL,
                            `notes` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL
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
                            `amountPaid` REAL NOT NULL,
                            `paymentDateIso` TEXT NOT NULL,
                            `paymentMethod` TEXT NOT NULL,
                            `transactionRef` TEXT NOT NULL,
                            `notes` TEXT NOT NULL,
                            `timestamp` INTEGER NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscription_payments_billId` ON `subscription_payments` (`billId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscription_payments_subscriptionId` ON `subscription_payments` (`subscriptionId`)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        return factory.create(config).writableDatabase
    }

    @Test
    fun testMigration18To19_preservesValidRecords_andQuarantinesOrphans() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val dbName = "test_migration_18_to_19_orphans.db"
        val db = createV18Database(context, dbName)

        // Seed valid parents
        db.execSQL("INSERT INTO `tasks` (`id`, `title`) VALUES (101, 'Valid Task 101')")
        db.execSQL("INSERT INTO `labels` (`id`, `name`) VALUES (201, 'Valid Label 201')")
        db.execSQL("INSERT INTO `medications` (`id`, `taskId`, `name`, `form`, `dosageAmount`, `dosageUnit`, `scheduleType`, `startDateIso`) VALUES (301, 101, 'Amoxicillin', 'CAPSULE', '500', 'MG', 'DAILY', '2026-11-01')")
        db.execSQL("INSERT INTO `trackers` (`id`, `name`, `category`, `trackingType`, `quantityUnit`, `distanceUnit`) VALUES (401, 'Car Odometer', 'VEHICLE', 'ODOMETER', 'LITERS', 'KM')")
        db.execSQL("INSERT INTO `tracker_cycles` (`id`, `trackerId`, `startDateIso`, `status`) VALUES (501, 401, '2026-11-01', 'ACTIVE')")
        db.execSQL("INSERT INTO `subscriptions` (`id`, `name`, `category`, `billingFrequency`, `amount`, `firstBillDateIso`, `status`) VALUES (601, 'Netflix', 'ENTERTAINMENT', 'MONTHLY', 499.0, '2026-11-01', 'ACTIVE')")

        // Seed VALID child rows
        db.execSQL("INSERT INTO `task_label_cross_ref` (`taskId`, `labelId`) VALUES (101, 201)")
        db.execSQL("INSERT INTO `task_reminders` (`id`, `taskId`, `minutesBefore`, `type`, `referenceType`, `isEnabled`, `createdAt`) VALUES (11, 101, 15, 'OFFSET', 'START_TIME', 1, 1000)")
        db.execSQL("INSERT INTO `medication_doses` (`id`, `medicationId`, `taskId`, `medicationName`, `medicationForm`, `dosage`, `instructions`, `scheduledDateIso`, `scheduledTime`, `status`, `snoozeCount`, `followUpCount`, `notes`) VALUES (21, 301, 101, 'Amoxicillin', 'CAPSULE', '500 MG', '', '2026-11-01', '08:00', 'SCHEDULED', 0, 0, '')")
        db.execSQL("INSERT INTO `tracker_usage_entries` (`id`, `cycleId`, `dateIso`, `notes`, `timestamp`) VALUES (31, 501, '2026-11-01', 'Valid entry', 1000)")
        db.execSQL("INSERT INTO `subscription_bills` (`id`, `subscriptionId`, `dueDateIso`, `expectedAmount`, `currency`, `status`, `notes`, `createdAt`) VALUES (41, 601, '2026-11-01', 499.0, '₹', 'UNPAID', '', 1000)")
        db.execSQL("INSERT INTO `subscription_payments` (`id`, `billId`, `subscriptionId`, `amountPaid`, `paymentDateIso`, `paymentMethod`, `transactionRef`, `notes`, `timestamp`) VALUES (51, 41, 601, 499.0, '2026-11-01', 'UPI', '', '', 1000)")

        // Seed ORPHAN child rows (pointing to non-existent parent IDs: 9999)
        db.execSQL("INSERT INTO `task_label_cross_ref` (`taskId`, `labelId`) VALUES (9999, 201)") // orphan task
        db.execSQL("INSERT INTO `task_label_cross_ref` (`taskId`, `labelId`) VALUES (101, 9999)") // orphan label
        db.execSQL("INSERT INTO `task_label_cross_ref` (`taskId`, `labelId`) VALUES (8888, 8888)") // orphan both

        db.execSQL("INSERT INTO `task_reminders` (`id`, `taskId`, `minutesBefore`, `type`, `referenceType`, `isEnabled`, `createdAt`) VALUES (12, 9999, 30, 'OFFSET', 'START_TIME', 1, 2000)")

        db.execSQL("INSERT INTO `medication_doses` (`id`, `medicationId`, `taskId`, `medicationName`, `medicationForm`, `dosage`, `instructions`, `scheduledDateIso`, `scheduledTime`, `status`, `snoozeCount`, `followUpCount`, `notes`) VALUES (22, 9999, 9999, 'Phantom Drug', 'PILL', '10 MG', '', '2026-11-01', '12:00', 'SCHEDULED', 0, 0, '')")

        db.execSQL("INSERT INTO `tracker_usage_entries` (`id`, `cycleId`, `dateIso`, `notes`, `timestamp`) VALUES (32, 9999, '2026-11-01', 'Orphan entry', 2000)")

        db.execSQL("INSERT INTO `subscription_bills` (`id`, `subscriptionId`, `dueDateIso`, `expectedAmount`, `currency`, `status`, `notes`, `createdAt`) VALUES (42, 9999, '2026-11-01', 199.0, '₹', 'UNPAID', 'Orphan bill', 2000)")

        db.execSQL("INSERT INTO `subscription_payments` (`id`, `billId`, `subscriptionId`, `amountPaid`, `paymentDateIso`, `paymentMethod`, `transactionRef`, `notes`, `timestamp`) VALUES (52, 9999, 601, 100.0, '2026-11-01', 'UPI', '', 'Orphan billId', 2000)")
        db.execSQL("INSERT INTO `subscription_payments` (`id`, `billId`, `subscriptionId`, `amountPaid`, `paymentDateIso`, `paymentMethod`, `transactionRef`, `notes`, `timestamp`) VALUES (53, 41, 9999, 100.0, '2026-11-01', 'UPI', '', 'Orphan subId', 2000)")

        // Execute MIGRATION_18_19
        MIGRATION_18_19.migrate(db)

        // 1. Verify PRAGMA foreign_key_check passes with zero violations
        val fkCheckCursor = db.query("PRAGMA foreign_key_check")
        assertFalse("PRAGMA foreign_key_check must return 0 violations after migration", fkCheckCursor.moveToFirst())
        fkCheckCursor.close()

        // 2. Verify Valid Records remain in active tables
        val validRefCursor = db.query("SELECT * FROM task_label_cross_ref")
        assertEquals(1, validRefCursor.count)
        validRefCursor.moveToFirst()
        assertEquals(101L, validRefCursor.getLong(0))
        assertEquals(201L, validRefCursor.getLong(1))
        validRefCursor.close()

        val validReminderCursor = db.query("SELECT id, taskId FROM task_reminders")
        assertEquals(1, validReminderCursor.count)
        validReminderCursor.moveToFirst()
        assertEquals(11L, validReminderCursor.getLong(0))
        assertEquals(101L, validReminderCursor.getLong(1))
        validReminderCursor.close()

        val validDoseCursor = db.query("SELECT id, medicationId FROM medication_doses")
        assertEquals(1, validDoseCursor.count)
        validDoseCursor.moveToFirst()
        assertEquals(21L, validDoseCursor.getLong(0))
        assertEquals(301L, validDoseCursor.getLong(1))
        validDoseCursor.close()

        val validUsageCursor = db.query("SELECT id, cycleId FROM tracker_usage_entries")
        assertEquals(1, validUsageCursor.count)
        validUsageCursor.moveToFirst()
        assertEquals(31L, validUsageCursor.getLong(0))
        assertEquals(501L, validUsageCursor.getLong(1))
        validUsageCursor.close()

        val validBillCursor = db.query("SELECT id, subscriptionId FROM subscription_bills")
        assertEquals(1, validBillCursor.count)
        validBillCursor.moveToFirst()
        assertEquals(41L, validBillCursor.getLong(0))
        assertEquals(601L, validBillCursor.getLong(1))
        validBillCursor.close()

        val validPaymentCursor = db.query("SELECT id, billId, subscriptionId FROM subscription_payments")
        assertEquals(1, validPaymentCursor.count)
        validPaymentCursor.moveToFirst()
        assertEquals(51L, validPaymentCursor.getLong(0))
        assertEquals(41L, validPaymentCursor.getLong(1))
        assertEquals(601L, validPaymentCursor.getLong(2))
        validPaymentCursor.close()

        // 3. Verify Quarantine Tables contain all orphan records with quarantineReason and quarantinedAt
        val qRefCursor = db.query("SELECT taskId, labelId, quarantineReason FROM _orphans_backup_task_label_cross_ref ORDER BY taskId")
        assertEquals(3, qRefCursor.count)
        qRefCursor.close()

        val qReminderCursor = db.query("SELECT id, taskId, quarantineReason FROM _orphans_backup_task_reminders")
        assertEquals(1, qReminderCursor.count)
        qReminderCursor.moveToFirst()
        assertEquals(12L, qReminderCursor.getLong(0))
        assertEquals(9999L, qReminderCursor.getLong(1))
        assertEquals("PARENT_TASK_MISSING", qReminderCursor.getString(2))
        qReminderCursor.close()

        val qDoseCursor = db.query("SELECT id, medicationId, quarantineReason FROM _orphans_backup_medication_doses")
        assertEquals(1, qDoseCursor.count)
        qDoseCursor.moveToFirst()
        assertEquals(22L, qDoseCursor.getLong(0))
        assertEquals(9999L, qDoseCursor.getLong(1))
        assertEquals("PARENT_MEDICATION_MISSING", qDoseCursor.getString(2))
        qDoseCursor.close()

        val qUsageCursor = db.query("SELECT id, cycleId, quarantineReason FROM _orphans_backup_tracker_usage_entries")
        assertEquals(1, qUsageCursor.count)
        qUsageCursor.moveToFirst()
        assertEquals(32L, qUsageCursor.getLong(0))
        assertEquals(9999L, qUsageCursor.getLong(1))
        assertEquals("PARENT_CYCLE_MISSING", qUsageCursor.getString(2))
        qUsageCursor.close()

        val qBillCursor = db.query("SELECT id, subscriptionId, quarantineReason FROM _orphans_backup_subscription_bills")
        assertEquals(1, qBillCursor.count)
        qBillCursor.moveToFirst()
        assertEquals(42L, qBillCursor.getLong(0))
        assertEquals(9999L, qBillCursor.getLong(1))
        assertEquals("PARENT_SUBSCRIPTION_MISSING", qBillCursor.getString(2))
        qBillCursor.close()

        val qPaymentCursor = db.query("SELECT id, billId, subscriptionId, quarantineReason FROM _orphans_backup_subscription_payments ORDER BY id")
        assertEquals(2, qPaymentCursor.count)
        qPaymentCursor.close()

        // 4. Test Foreign Key Constraint Rejection at runtime
        db.execSQL("PRAGMA foreign_keys = ON")
        try {
            db.execSQL("INSERT INTO `task_reminders` (`taskId`, `minutesBefore`, `type`, `referenceType`, `isEnabled`, `createdAt`) VALUES (9999, 10, 'OFFSET', 'START_TIME', 1, 3000)")
            fail("Expected SQLiteConstraintException when inserting reminder with non-existent taskId")
        } catch (e: SQLiteConstraintException) {
            // Success: foreign key constraint enforced!
        }

        try {
            db.execSQL("INSERT INTO `medication_doses` (`medicationId`, `taskId`, `medicationName`, `medicationForm`, `dosage`, `instructions`, `scheduledDateIso`, `scheduledTime`, `status`, `snoozeCount`, `followUpCount`, `notes`) VALUES (9999, 101, 'Ghost', 'PILL', '1', '', '2026-11-01', '10:00', 'SCHEDULED', 0, 0, '')")
            fail("Expected SQLiteConstraintException when inserting dose with non-existent medicationId")
        } catch (e: SQLiteConstraintException) {
            // Success: foreign key constraint enforced!
        }

        // 5. Test Cascade Deletion
        db.execSQL("DELETE FROM `tasks` WHERE `id` = 101")
        val postDeleteRefCursor = db.query("SELECT * FROM task_label_cross_ref WHERE taskId = 101")
        assertEquals("task_label_cross_ref must cascade delete when task is deleted", 0, postDeleteRefCursor.count)
        postDeleteRefCursor.close()

        val postDeleteReminderCursor = db.query("SELECT * FROM task_reminders WHERE taskId = 101")
        assertEquals("task_reminders must cascade delete when task is deleted", 0, postDeleteReminderCursor.count)
        postDeleteReminderCursor.close()

        db.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun testMigration18To19_largeFixturePerformance() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val dbName = "test_migration_18_to_19_perf.db"
        val db = createV18Database(context, dbName)

        // Seed large batch (~50,000 records) in a single transaction
        db.beginTransaction()
        try {
            for (i in 1..5000) {
                db.execSQL("INSERT INTO `tasks` (`id`, `title`) VALUES ($i, 'Task $i')")
                db.execSQL("INSERT INTO `labels` (`id`, `name`) VALUES ($i, 'Label $i')")
                db.execSQL("INSERT INTO `task_label_cross_ref` (`taskId`, `labelId`) VALUES ($i, $i)")
                db.execSQL("INSERT INTO `task_reminders` (`id`, `taskId`, `minutesBefore`, `type`, `referenceType`, `isEnabled`, `createdAt`) VALUES ($i, $i, 15, 'OFFSET', 'START_TIME', 1, 1000)")
                db.execSQL("INSERT INTO `subscriptions` (`id`, `name`, `category`, `billingFrequency`, `amount`, `firstBillDateIso`, `status`) VALUES ($i, 'Sub $i', 'CAT', 'MONTHLY', 10.0, '2026-11-01', 'ACTIVE')")
                db.execSQL("INSERT INTO `subscription_bills` (`id`, `subscriptionId`, `dueDateIso`, `expectedAmount`, `currency`, `status`, `notes`, `createdAt`) VALUES ($i, $i, '2026-11-01', 10.0, '₹', 'UNPAID', '', 1000)")
                db.execSQL("INSERT INTO `subscription_payments` (`id`, `billId`, `subscriptionId`, `amountPaid`, `paymentDateIso`, `paymentMethod`, `transactionRef`, `notes`, `timestamp`) VALUES ($i, $i, $i, 10.0, '2026-11-01', 'UPI', '', '', 1000)")
                db.execSQL("INSERT INTO `trackers` (`id`, `name`, `category`, `trackingType`, `quantityUnit`, `distanceUnit`) VALUES ($i, 'Tracker $i', 'CAT', 'SIMPLE', 'UNIT', 'KM')")
                db.execSQL("INSERT INTO `tracker_cycles` (`id`, `trackerId`, `startDateIso`, `status`) VALUES ($i, $i, '2026-11-01', 'ACTIVE')")
                db.execSQL("INSERT INTO `tracker_usage_entries` (`id`, `cycleId`, `dateIso`, `notes`, `timestamp`) VALUES ($i, $i, '2026-11-01', '', 1000)")
            }
            // Seed a small set of orphans within the large dataset
            for (orphan in 90001..90100) {
                db.execSQL("INSERT INTO `task_label_cross_ref` (`taskId`, `labelId`) VALUES ($orphan, 1)")
                db.execSQL("INSERT INTO `task_reminders` (`id`, `taskId`, `minutesBefore`, `type`, `referenceType`, `isEnabled`, `createdAt`) VALUES ($orphan, $orphan, 15, 'OFFSET', 'START_TIME', 1, 1000)")
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        val startTime = System.currentTimeMillis()
        MIGRATION_18_19.migrate(db)
        val elapsedMs = System.currentTimeMillis() - startTime

        val fkCheckCursor = db.query("PRAGMA foreign_key_check")
        assertFalse("PRAGMA foreign_key_check must pass after 50,000+ row migration", fkCheckCursor.moveToFirst())
        fkCheckCursor.close()

        val qReminderCursor = db.query("SELECT COUNT(*) FROM _orphans_backup_task_reminders")
        assertTrue(qReminderCursor.moveToFirst())
        assertEquals(100L, qReminderCursor.getLong(0))
        qReminderCursor.close()

        val validReminderCursor = db.query("SELECT COUNT(*) FROM task_reminders")
        assertTrue(validReminderCursor.moveToFirst())
        assertEquals(5000L, validReminderCursor.getLong(0))
        validReminderCursor.close()

        println("MIGRATION_18_19 50,000+ row migration completed in ${elapsedMs}ms")
        assertTrue("Migration of 50,000 rows must complete in under 10,000ms", elapsedMs < 10000)

        db.close()
        context.deleteDatabase(dbName)
    }
}
