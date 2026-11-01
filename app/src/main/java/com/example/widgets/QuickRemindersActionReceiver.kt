package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.JsonMappers
import com.example.data.repository.LifeRepository
import com.example.domain.model.HabitItem
import com.example.domain.model.RecurrenceType
import com.example.ui.theme.engine.LocalPremiumEntitlementRepository
import com.example.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Dedicated internal BroadcastReceiver for Quick Reminders widget tile interactions.
 *
 * SEC-01 Remediation:
 * - Declared strictly unexported (android:exported="false") in AndroidManifest.xml.
 * - External applications cannot broadcast to this receiver; Android's ActivityManagerService
 *   rejects any cross-package broadcasts with SecurityException.
 * - Legitimate launcher interactions succeed because the widget PendingIntent carries
 *   the identity and token of the LifeOS application.
 * - Enforces parameter validation on all extras before performing database mutations.
 */
class QuickRemindersActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TOGGLE_SHORTCUT = "com.example.lifeos.ACTION_TOGGLE_SHORTCUT"

        const val EXTRA_SHORTCUT_ID = "extra_shortcut_id"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TARGET_ID = "extra_target_id"
        const val EXTRA_WIDGET_ID = "extra_widget_id"

        val ALLOWED_TYPES = setOf("TASK", "HABIT", "WATER", "CHALLENGE_RULE")

        fun createPendingIntent(
            context: Context,
            appWidgetId: Int,
            slotIndex: Int,
            shortcutId: Long,
            type: String,
            taskId: Long?,
            targetId: String
        ): PendingIntent {
            val intent = Intent(context, QuickRemindersActionReceiver::class.java).apply {
                action = ACTION_TOGGLE_SHORTCUT
                putExtra(EXTRA_SHORTCUT_ID, shortcutId)
                putExtra(EXTRA_TYPE, type)
                putExtra(EXTRA_TASK_ID, taskId ?: -1L)
                putExtra(EXTRA_TARGET_ID, targetId)
                putExtra(EXTRA_WIDGET_ID, appWidgetId)
                data = "lifeos://widget/qr/$appWidgetId/$slotIndex/$shortcutId".toUri()
            }
            return PendingIntent.getBroadcast(
                context,
                appWidgetId * 100 + slotIndex,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE_SHORTCUT) {
            return
        }

        // Validate URI scheme to ensure intent originated from legitimate LifeOS widget PendingIntent
        if (intent.data?.scheme != "lifeos") {
            return
        }

        val shortcutId = intent.getLongExtra(EXTRA_SHORTCUT_ID, -1L)
        val rawType = intent.getStringExtra(EXTRA_TYPE) ?: "TASK"
        val fallbackType = if (rawType in ALLOWED_TYPES) rawType else "TASK"
        val fallbackTaskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val rawTargetId = intent.getStringExtra(EXTRA_TARGET_ID) ?: ""
        val fallbackTargetId = rawTargetId.take(256).trim()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val db = AppDatabase.getInstance(context.applicationContext)
                val prefs = UserPreferences(context.applicationContext)
                val entitlementRepo = LocalPremiumEntitlementRepository(context.applicationContext)
                val repository = LifeRepository(db, prefs, entitlementRepo)
                val todayIso = DateUtils.todayString()

                val sc = if (shortcutId > 0L) {
                    db.honeycombShortcutDao().getShortcutById(shortcutId)
                } else null

                val effectiveType = sc?.type ?: fallbackType
                val effectiveTargetId = sc?.targetId ?: fallbackTargetId
                val effectiveTitle = sc?.title ?: effectiveTargetId

                // Enforce allowed type whitelist
                if (effectiveType !in ALLOWED_TYPES) {
                    return@launch
                }

                when (effectiveType) {
                    "TASK" -> {
                        val validTaskId = if (fallbackTaskId > 0L) {
                            fallbackTaskId
                        } else {
                            val candidateId = effectiveTargetId.toLongOrNull()
                            if (candidateId != null && candidateId > 0L) {
                                candidateId
                            } else {
                                val task = repository.getOrCreateTaskForShortcut(
                                    title = effectiveTitle.ifBlank { "Discipline" }.take(100),
                                    emoji = sc?.emoji ?: "📌",
                                    category = "Routines",
                                    recurrence = RecurrenceType.DAILY
                                )
                                task.id
                            }
                        }
                        if (validTaskId > 0L) {
                            repository.toggleTaskCompletion(validTaskId, todayIso)
                        }
                    }
                    "HABIT" -> {
                        val habitId = effectiveTargetId.toLongOrNull()
                        val matchedHabitId = if (habitId != null && habitId > 0L) {
                            habitId
                        } else {
                            val habits = db.habitDao().getAllHabitsList()
                            val found = habits.find {
                                it.id.toString() == effectiveTargetId ||
                                it.title.equals(effectiveTargetId, ignoreCase = true) ||
                                it.title.equals(effectiveTitle, ignoreCase = true)
                            }
                            found?.id ?: repository.insertHabit(
                                HabitItem(
                                    title = effectiveTitle.ifBlank { "Routine Habit" }.take(100),
                                    emoji = sc?.emoji ?: "⚡",
                                    colorHex = sc?.customColorHex ?: 0xFF10B981L
                                )
                            )
                        }
                        if (matchedHabitId > 0L) {
                            repository.toggleHabitCompletion(matchedHabitId, todayIso)
                        }
                    }
                    "WATER" -> {
                        repository.addWater(250, todayIso)
                    }
                    "CHALLENGE_RULE" -> {
                        val activeChallenge = db.challengeDao().getActiveChallenge()
                        val rules = activeChallenge?.let { JsonMappers.parseChallengeRules(it.rulesJson) } ?: emptyList()
                        val matchedRule = rules.find {
                            it.id == effectiveTargetId ||
                            it.title.equals(effectiveTargetId, ignoreCase = true) ||
                            it.title.equals(effectiveTitle, ignoreCase = true)
                        }
                        if (matchedRule != null) {
                            repository.toggleChallengeRule(matchedRule.id, todayIso)
                        } else {
                            val task = repository.getOrCreateTaskForShortcut(
                                title = effectiveTitle.ifBlank { "Discipline" }.take(100),
                                emoji = sc?.emoji ?: "🎯",
                                category = "Routines",
                                recurrence = RecurrenceType.DAILY
                            )
                            repository.toggleTaskCompletion(task.id, todayIso)
                        }
                    }
                }

                // 1. Notify global coordinator for in-app and widget sync
                WidgetRefreshCoordinator.notifyAllWidgetsChanged(context.applicationContext)

                // 2. Refresh active QuickRemindersWidgetProvider instances
                QuickRemindersWidgetProvider.notifyDataChanged(context.applicationContext)
            } catch (e: Exception) {
                android.util.Log.e("QuickRemindersAction", "Failed executing widget action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
