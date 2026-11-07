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
            try {
                val workManager = try {
                    WorkManager.getInstance(context)
                } catch (e: IllegalStateException) {
                    val config = androidx.work.Configuration.Builder().build()
                    WorkManager.initialize(context.applicationContext, config)
                    WorkManager.getInstance(context)
                }
                val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(24, TimeUnit.HOURS)
                    .build()
                workManager.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            } catch (e: Throwable) {
                android.util.Log.e("MaintenanceWorker", "Failed to enqueue maintenance periodic work", e)
            }
        }
    }

    override suspend fun doWork(): Result {
        return try {
            ReminderScheduler.rescheduleAll(applicationContext, horizonDays = 7)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
