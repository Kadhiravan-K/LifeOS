package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.LifeRepository
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class MaintenanceWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "lifeos_maintenance_worker"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(24, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getInstance(applicationContext)
            val preferences = UserPreferences(applicationContext)
            val repository = LifeRepository(database, preferences)

            val todayIso = DateUtils.todayString()
            val tomorrowIso = DateUtils.addDays(todayIso, 1)

            // Verify and schedule reminders for today and tomorrow
            val tasksToday = repository.getTasksForDateFlow(todayIso).first()
            tasksToday.filter { it.isReminderEnabled && !it.isCompletedForDate }.forEach { task ->
                NotificationHelper.scheduleTaskAlarm(applicationContext, task, todayIso)
            }

            val tasksTomorrow = repository.getTasksForDateFlow(tomorrowIso).first()
            tasksTomorrow.filter { it.isReminderEnabled }.forEach { task ->
                NotificationHelper.scheduleTaskAlarm(applicationContext, task, tomorrowIso)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
