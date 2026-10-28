package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.core.net.toUri
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.HoneycombShortcutEntity
import com.example.data.local.entities.ProjectEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.WaterLogEntity
import com.example.data.preferences.UserPreferences
import com.example.data.repository.JsonMappers
import com.example.data.repository.LifeRepository
import com.example.domain.model.ChallengeDailyLog
import com.example.domain.model.RecurrenceType
import com.example.domain.model.TodoistColorPalette
import com.example.ui.theme.engine.LocalPremiumEntitlementRepository
import com.example.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Quick Reminders home-screen widget provider — v2 static flat layout.
 *
 * Architecture: Static RemoteViews grid (3+2+3 honeycomb) rendered directly,
 * replacing the old ListView / RemoteViewsService approach (RC-1 fix).
 * Data is loaded on IO dispatcher and bound to individual tile views by ID.
 */
class QuickRemindersWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_TOGGLE_TASK = "com.example.lifeos.ACTION_WIDGET_TOGGLE_TASK"

        // Tile container IDs (8 tiles: 3+2+3 honeycomb)
        val tileContainerIds = intArrayOf(
            R.id.qr_t0, R.id.qr_t1, R.id.qr_t2,
            R.id.qr_t3, R.id.qr_t4,
            R.id.qr_t5, R.id.qr_t6, R.id.qr_t7
        )

        // Shape ImageView IDs
        val tileShapeIds = intArrayOf(
            R.id.qr_t0_shape, R.id.qr_t1_shape, R.id.qr_t2_shape,
            R.id.qr_t3_shape, R.id.qr_t4_shape,
            R.id.qr_t5_shape, R.id.qr_t6_shape, R.id.qr_t7_shape
        )

        // Emoji TextView IDs
        val tileEmojiIds = intArrayOf(
            R.id.qr_t0_emoji, R.id.qr_t1_emoji, R.id.qr_t2_emoji,
            R.id.qr_t3_emoji, R.id.qr_t4_emoji,
            R.id.qr_t5_emoji, R.id.qr_t6_emoji, R.id.qr_t7_emoji
        )

        // Title TextView IDs
        val tileTitleIds = intArrayOf(
            R.id.qr_t0_title, R.id.qr_t1_title, R.id.qr_t2_title,
            R.id.qr_t3_title, R.id.qr_t4_title,
            R.id.qr_t5_title, R.id.qr_t6_title, R.id.qr_t7_title
        )

        // Check ImageView IDs
        val tileCheckIds = intArrayOf(
            R.id.qr_t0_check, R.id.qr_t1_check, R.id.qr_t2_check,
            R.id.qr_t3_check, R.id.qr_t4_check,
            R.id.qr_t5_check, R.id.qr_t6_check, R.id.qr_t7_check
        )

        /**
         * Notifies all active widget instances to reload by broadcasting ACTION_APPWIDGET_UPDATE.
         * This triggers a full onUpdate() cycle (RC-5 and RC-9 fix).
         */
        fun notifyDataChanged(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, QuickRemindersWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, QuickRemindersWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    /**
     * Launches data load on IO thread, then calls appWidgetManager.updateAppWidget.
     * Never blocks the main thread (RC-4 fix).
     */
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            val views = buildWidgetViews(context, appWidgetId)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    /**
     * Builds the full static RemoteViews for the v2 flat honeycomb layout.
     * Loads all data from Room synchronously on the IO thread.
     */
    private suspend fun buildWidgetViews(context: Context, appWidgetId: Int): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_quick_reminders_v2)

        // Header click → open app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPi = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        rv.setOnClickPendingIntent(R.id.widget_header, openAppPi)
        rv.setOnClickPendingIntent(R.id.widget_empty_view, openAppPi)

        // Load data from Room on IO thread
        val db = AppDatabase.getInstance(context)
        val prefs = UserPreferences(context)
        val entitlementRepo = LocalPremiumEntitlementRepository(context)
        val repository = LifeRepository(db, prefs, entitlementRepo)
        val todayIso = DateUtils.todayString()

        val shortcuts = db.honeycombShortcutDao().getAllShortcutsList()

        if (shortcuts.isEmpty()) {
            rv.setViewVisibility(R.id.widget_empty_view, View.VISIBLE)
            rv.setViewVisibility(R.id.qr_row1, View.GONE)
            rv.setViewVisibility(R.id.qr_row2, View.GONE)
            rv.setViewVisibility(R.id.qr_row3, View.GONE)
            return rv
        }

        rv.setViewVisibility(R.id.widget_empty_view, View.GONE)
        rv.setViewVisibility(R.id.qr_row1, View.VISIBLE)
        rv.setViewVisibility(R.id.qr_row2, View.VISIBLE)
        rv.setViewVisibility(R.id.qr_row3, View.VISIBLE)

        // Load supporting data for completion state
        val tasks = db.taskDao().getAllTasksList()
        val habits = db.habitDao().getAllHabitsList()
        val projects = db.projectDao().getAllProjectsList()
        val waterLog = db.waterLogDao().getWaterLog(todayIso)
        val waterGoal = try { prefs.waterGoalMl.first() } catch (_: Exception) { 2000 }
        val activeChallenge = db.challengeDao().getActiveChallenge()
        val challengeLogs = activeChallenge?.let { JsonMappers.parseChallengeDailyLogs(it.dailyLogsJson) }
        val todayChallengeLog = challengeLogs?.get(todayIso)

        // Build tile data list (max 8 tiles for 3+2+3 layout)
        val tileDataList = buildTileData(
            shortcuts, tasks, habits, waterLog, waterGoal,
            todayChallengeLog, projects, todayIso
        )

        // Bind each tile slot
        for (i in tileContainerIds.indices) {
            if (i < tileDataList.size) {
                val tile = tileDataList[i]
                rv.setViewVisibility(tileContainerIds[i], View.VISIBLE)
                rv.setTextViewText(tileEmojiIds[i], tile.emoji)
                rv.setTextViewText(tileTitleIds[i], tile.title)

                // Use cached bitmap (RC-4 fix)
                val tileSizePx = 160
                val bitmap = QuickRemindersWidgetHelper.getCachedTileBitmap(
                    tileSizePx, tileSizePx, tile.shape, tile.colorArgb, tile.isCompleted
                )
                rv.setImageViewBitmap(tileShapeIds[i], bitmap)
                rv.setViewVisibility(
                    tileCheckIds[i],
                    if (tile.isCompleted) View.VISIBLE else View.GONE
                )

                // Individual PendingIntent per tile (no fill-in template needed)
                val toggleIntent = Intent(context, QuickRemindersWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_TOGGLE_TASK
                    putExtra("extra_type", tile.type)
                    putExtra("extra_task_id", tile.taskId ?: -1L)
                    putExtra("extra_target_id", tile.targetId)
                    // Unique URI ensures each tile gets a distinct PendingIntent
                    data = "lifeos://widget/qr/$appWidgetId/$i".toUri()
                }
                val togglePi = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 100 + i,
                    toggleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                rv.setOnClickPendingIntent(tileContainerIds[i], togglePi)
            } else {
                rv.setViewVisibility(tileContainerIds[i], View.INVISIBLE)
            }
        }

        return rv
    }

    /**
     * Data class representing a single rendered tile.
     */
    data class TileData(
        val shortcutId: Long,
        val type: String,
        val targetId: String,
        val taskId: Long?,
        val title: String,
        val emoji: String,
        val shape: String,
        val colorArgb: Int,
        val isCompleted: Boolean
    )

    /**
     * Builds the ordered list of TileData from the shortcuts + supporting data.
     * Ordered by coordR (row) then coordQ (column), falling back to orderIndex.
     * Max 8 tiles for the 3+2+3 layout.
     */
    private fun buildTileData(
        shortcuts: List<HoneycombShortcutEntity>,
        tasks: List<TaskEntity>,
        habits: List<HabitEntity>,
        waterLog: WaterLogEntity?,
        waterGoal: Int,
        todayChallengeLog: ChallengeDailyLog?,
        projects: List<ProjectEntity>,
        todayIso: String
    ): List<TileData> {
        // Sort: primary by coordR (row), secondary by coordQ (col), fallback by orderIndex
        val sorted = shortcuts.sortedWith(compareBy({ it.coordR }, { it.coordQ }, { it.orderIndex }))

        val result = mutableListOf<TileData>()
        for (sc in sorted) {
            if (result.size >= 8) break

            val displayTitle = if (!sc.customTileName.isNullOrBlank()) sc.customTileName else sc.title
            var taskId: Long? = null
            var isCompleted = false
            var projectColorHex: Long? = null
            var typeDefaultColor: Long = 0xFF3B82F6L

            when (sc.type) {
                "TASK" -> {
                    typeDefaultColor = 0xFF3B82F6L
                    val matchedTask = tasks.find {
                        it.id.toString() == sc.targetId ||
                        it.title.equals(sc.targetId, ignoreCase = true) ||
                        it.title.contains(sc.targetId, ignoreCase = true) ||
                        it.title.equals(sc.title, ignoreCase = true) ||
                        it.title.contains(sc.title, ignoreCase = true)
                    }
                    if (matchedTask != null) {
                        taskId = matchedTask.id
                        isCompleted = matchedTask.isOneTimeCompleted ||
                                      matchedTask.completedDatesJson.contains(todayIso)
                        val proj = matchedTask.projectId?.let { pId -> projects.find { it.id == pId } }
                        if (proj != null) {
                            projectColorHex = TodoistColorPalette.getColorLong(proj.colorId)
                        }
                    }
                }
                "HABIT" -> {
                    typeDefaultColor = 0xFF10B981L
                    val matchedHabit = habits.find {
                        it.id.toString() == sc.targetId ||
                        it.title.equals(sc.targetId, ignoreCase = true) ||
                        it.title.contains(sc.targetId, ignoreCase = true) ||
                        it.title.equals(sc.title, ignoreCase = true) ||
                        it.title.contains(sc.title, ignoreCase = true)
                    }
                    isCompleted = matchedHabit?.completedDatesJson?.contains(todayIso) == true
                }
                "WATER" -> {
                    typeDefaultColor = 0xFF3B82F6L
                    val currentAmount = waterLog?.amountMl ?: 0
                    isCompleted = currentAmount >= waterGoal
                }
                "CHALLENGE_RULE" -> {
                    typeDefaultColor = 0xFFF59E0BL
                    val ruleCompleted = todayChallengeLog?.completedRuleIds?.contains(sc.targetId) == true ||
                                  todayChallengeLog?.completedRuleIds?.contains(sc.title) == true
                    if (ruleCompleted) {
                        isCompleted = true
                    } else {
                        // Fallback: check if backed by a Task entity
                        val matchedTask = tasks.find {
                            it.id.toString() == sc.targetId ||
                            it.title.equals(sc.targetId, ignoreCase = true) ||
                            it.title.equals(sc.title, ignoreCase = true) ||
                            it.title.contains(sc.title, ignoreCase = true)
                        }
                        if (matchedTask != null) {
                            taskId = matchedTask.id
                            isCompleted = matchedTask.isOneTimeCompleted ||
                                          matchedTask.completedDatesJson.contains(todayIso)
                        }
                    }
                }
                else -> {
                    typeDefaultColor = 0xFF8B5CF6L
                    isCompleted = false
                }
            }

            val effectiveColor = when (sc.colorSource) {
                "CUSTOM" -> sc.customColorHex ?: typeDefaultColor
                "PROJECT" -> projectColorHex ?: sc.customColorHex ?: typeDefaultColor
                else -> typeDefaultColor
            }

            result.add(
                TileData(
                    shortcutId = sc.id,
                    type = sc.type,
                    targetId = sc.targetId,
                    taskId = taskId,
                    title = displayTitle,
                    emoji = sc.emoji,
                    shape = sc.shape,
                    colorArgb = effectiveColor.toInt(),
                    isCompleted = isCompleted
                )
            )
        }

        return result
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_TOGGLE_TASK) {
            val type = intent.getStringExtra("extra_type") ?: "TASK"
            val taskId = intent.getLongExtra("extra_task_id", -1L)
            val targetId = intent.getStringExtra("extra_target_id") ?: ""

            val db = AppDatabase.getInstance(context)
            val prefs = UserPreferences(context)
            val entitlementRepo = LocalPremiumEntitlementRepository(context)
            val repository = LifeRepository(db, prefs, entitlementRepo)
            val todayIso = DateUtils.todayString()

            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                when (type) {
                    "TASK" -> {
                        val validTaskId = if (taskId != -1L) {
                            taskId
                        } else {
                            val candidateId = targetId.toLongOrNull()
                            if (candidateId != null) {
                                candidateId
                            } else {
                                val task = repository.getOrCreateTaskForShortcut(
                                    title = targetId.ifBlank { "Discipline" },
                                    emoji = "🔥",
                                    category = "Routines",
                                    recurrence = RecurrenceType.DAILY
                                )
                                task.id
                            }
                        }
                        repository.toggleTaskCompletion(validTaskId, todayIso)
                        WidgetRefreshCoordinator.notifyAllWidgetsChanged(context)
                    }
                    "HABIT" -> {
                        val habitId = targetId.toLongOrNull()
                        if (habitId != null) {
                            repository.toggleHabitCompletion(habitId, todayIso)
                            WidgetRefreshCoordinator.notifyAllWidgetsChanged(context)
                        } else {
                            val habits = db.habitDao().getAllHabitsList()
                            val matched = habits.find { it.title.equals(targetId, ignoreCase = true) }
                            if (matched != null) {
                                repository.toggleHabitCompletion(matched.id, todayIso)
                                WidgetRefreshCoordinator.notifyAllWidgetsChanged(context)
                            }
                        }
                    }
                    "WATER" -> {
                        repository.addWater(250, todayIso)
                        WidgetRefreshCoordinator.notifyAllWidgetsChanged(context)
                    }
                    "CHALLENGE_RULE" -> {
                        val activeChallenge = db.challengeDao().getActiveChallenge()
                        val rules = activeChallenge?.let { JsonMappers.parseChallengeRules(it.rulesJson) } ?: emptyList()
                        val matchedRule = rules.find {
                            it.id == targetId || it.title.equals(targetId, ignoreCase = true)
                        }
                        if (matchedRule != null) {
                            repository.toggleChallengeRule(matchedRule.id, todayIso)
                            WidgetRefreshCoordinator.notifyAllWidgetsChanged(context)
                        } else {
                            // Fallback to task deduplication & daily routine toggle
                            val task = repository.getOrCreateTaskForShortcut(
                                title = targetId.ifBlank { "Discipline" },
                                emoji = "🔥",
                                category = "Routines",
                                recurrence = RecurrenceType.DAILY
                            )
                            repository.toggleTaskCompletion(task.id, todayIso)
                            WidgetRefreshCoordinator.notifyAllWidgetsChanged(context)
                        }
                    }
                    else -> {
                        val openIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        context.startActivity(openIntent)
                    }
                }
            }
        }
    }
}
