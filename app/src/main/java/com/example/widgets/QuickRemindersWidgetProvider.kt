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
import com.example.domain.model.HabitItem
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
 * Quick Reminders home-screen widget provider — v2 interlocking honeycomb layout.
 *
 * Architecture: Static RemoteViews grid (3+2+3+2 honeycomb = 10 tiles) rendered directly,
 * interlocking adjacent rows by 12dp with pointy-top rounded hexagons, mathematically
 * matching the in-app QR HoneycombTaskGrid canvas.
 *
 * Data is loaded on the IO dispatcher and bound to individual tile views by ID.
 * Clicks on tiles directly toggle the underlying Room database entities and immediately
 * refresh both the launcher widget and in-app states.
 */
class QuickRemindersWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_TOGGLE_TASK = "com.example.lifeos.ACTION_WIDGET_TOGGLE_TASK"

        // Row container IDs (4 interlocking rows: 3 + 2 + 3 + 2)
        val rowIds = intArrayOf(
            R.id.qr_row1,
            R.id.qr_row2,
            R.id.qr_row3,
            R.id.qr_row4
        )

        // Tile container IDs (10 tiles: 3+2+3+2 honeycomb)
        val tileContainerIds = intArrayOf(
            R.id.qr_t0, R.id.qr_t1, R.id.qr_t2,
            R.id.qr_t3, R.id.qr_t4,
            R.id.qr_t5, R.id.qr_t6, R.id.qr_t7,
            R.id.qr_t8, R.id.qr_t9
        )

        // Shape ImageView IDs
        val tileShapeIds = intArrayOf(
            R.id.qr_t0_shape, R.id.qr_t1_shape, R.id.qr_t2_shape,
            R.id.qr_t3_shape, R.id.qr_t4_shape,
            R.id.qr_t5_shape, R.id.qr_t6_shape, R.id.qr_t7_shape,
            R.id.qr_t8_shape, R.id.qr_t9_shape
        )

        // Emoji TextView IDs
        val tileEmojiIds = intArrayOf(
            R.id.qr_t0_emoji, R.id.qr_t1_emoji, R.id.qr_t2_emoji,
            R.id.qr_t3_emoji, R.id.qr_t4_emoji,
            R.id.qr_t5_emoji, R.id.qr_t6_emoji, R.id.qr_t7_emoji,
            R.id.qr_t8_emoji, R.id.qr_t9_emoji
        )

        // Title TextView IDs
        val tileTitleIds = intArrayOf(
            R.id.qr_t0_title, R.id.qr_t1_title, R.id.qr_t2_title,
            R.id.qr_t3_title, R.id.qr_t4_title,
            R.id.qr_t5_title, R.id.qr_t6_title, R.id.qr_t7_title,
            R.id.qr_t8_title, R.id.qr_t9_title
        )

        // Check ImageView IDs
        val tileCheckIds = intArrayOf(
            R.id.qr_t0_check, R.id.qr_t1_check, R.id.qr_t2_check,
            R.id.qr_t3_check, R.id.qr_t4_check,
            R.id.qr_t5_check, R.id.qr_t6_check, R.id.qr_t7_check,
            R.id.qr_t8_check, R.id.qr_t9_check
        )

        /**
         * Notifies all active widget instances to reload by broadcasting ACTION_APPWIDGET_UPDATE.
         * This triggers a full onUpdate() cycle.
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
     * Never blocks the main thread.
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
     * Builds the full static RemoteViews for the interlocking honeycomb layout.
     * Loads all data from Room synchronously on the IO thread.
     */
    private suspend fun buildWidgetViews(context: Context, appWidgetId: Int): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_quick_reminders_v2)

        // Header click → open app directly
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

        var shortcuts = db.honeycombShortcutDao().getAllShortcutsList()

        // Auto-seed defaults if database shortcuts are empty so widget never stays blank
        if (shortcuts.isEmpty()) {
            try {
                repository.seedDefaultHoneycombShortcuts()
                shortcuts = db.honeycombShortcutDao().getAllShortcutsList()
            } catch (_: Exception) {}
        }

        if (shortcuts.isEmpty()) {
            rv.setViewVisibility(R.id.widget_empty_view, View.VISIBLE)
            rv.setViewVisibility(R.id.qr_row1, View.GONE)
            rv.setViewVisibility(R.id.qr_row2, View.GONE)
            rv.setViewVisibility(R.id.qr_row3, View.GONE)
            rv.setViewVisibility(R.id.qr_row4, View.GONE)
            return rv
        }

        // Load supporting data for completion state
        val tasks = db.taskDao().getAllTasksList()
        val habits = db.habitDao().getAllHabitsList()
        val projects = db.projectDao().getAllProjectsList()
        val waterLog = db.waterLogDao().getWaterLog(todayIso)
        val waterGoal = try { prefs.waterGoalMl.first() } catch (_: Exception) { 2000 }
        val activeChallenge = db.challengeDao().getActiveChallenge()
        val challengeLogs = activeChallenge?.let { JsonMappers.parseChallengeDailyLogs(it.dailyLogsJson) }
        val todayChallengeLog = challengeLogs?.get(todayIso)

        // Build tile data list (up to 10 tiles for 3+2+3+2 layout)
        val tileDataList = buildTileData(
            shortcuts, tasks, habits, waterLog, waterGoal,
            todayChallengeLog, projects, todayIso
        )
        val tileCount = tileDataList.size

        rv.setViewVisibility(R.id.widget_empty_view, if (tileCount == 0) View.VISIBLE else View.GONE)

        // Staggered Row Visibility: dynamically collapse empty rows to keep the honeycomb tightly centered
        rv.setViewVisibility(R.id.qr_row1, if (tileCount >= 1) View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.qr_row2, if (tileCount >= 4) View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.qr_row3, if (tileCount >= 6) View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.qr_row4, if (tileCount >= 9) View.VISIBLE else View.GONE)

        // Bind each tile slot
        val tileSizeW = 144
        val tileSizeH = 168

        for (i in tileContainerIds.indices) {
            if (i < tileCount) {
                val tile = tileDataList[i]
                rv.setViewVisibility(tileContainerIds[i], View.VISIBLE)
                rv.setTextViewText(tileEmojiIds[i], tile.emoji)
                rv.setTextViewText(tileTitleIds[i], tile.title)

                // Render crisp rounded hexagon matching in-app HoneycombTaskGrid
                val bitmap = QuickRemindersWidgetHelper.getCachedTileBitmap(
                    tileSizeW, tileSizeH, tile.shape, tile.colorArgb, tile.isCompleted
                )
                rv.setImageViewBitmap(tileShapeIds[i], bitmap)
                rv.setViewVisibility(
                    tileCheckIds[i],
                    if (tile.isCompleted) View.VISIBLE else View.GONE
                )

                // Individual PendingIntent per tile passing shortcutId for 100% linked execution
                val toggleIntent = Intent(context, QuickRemindersWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_TOGGLE_TASK
                    putExtra("extra_shortcut_id", tile.shortcutId)
                    putExtra("extra_type", tile.type)
                    putExtra("extra_task_id", tile.taskId ?: -1L)
                    putExtra("extra_target_id", tile.targetId)
                    putExtra("extra_widget_id", appWidgetId)
                    data = "lifeos://widget/qr/$appWidgetId/$i/${tile.shortcutId}".toUri()
                }
                val togglePi = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 100 + i,
                    toggleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                rv.setOnClickPendingIntent(tileContainerIds[i], togglePi)
            } else {
                // Invisible preserves honeycomb layout spacing for neighbor tiles
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
     * Max 10 tiles for the 3+2+3+2 layout.
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
            if (result.size >= 10) break

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
            val shortcutId = intent.getLongExtra("extra_shortcut_id", -1L)
            val fallbackType = intent.getStringExtra("extra_type") ?: "TASK"
            val fallbackTaskId = intent.getLongExtra("extra_task_id", -1L)
            val fallbackTargetId = intent.getStringExtra("extra_target_id") ?: ""

            val db = AppDatabase.getInstance(context)
            val prefs = UserPreferences(context)
            val entitlementRepo = LocalPremiumEntitlementRepository(context)
            val repository = LifeRepository(db, prefs, entitlementRepo)
            val todayIso = DateUtils.todayString()

            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                val sc = if (shortcutId != -1L) {
                    db.honeycombShortcutDao().getShortcutById(shortcutId)
                } else null

                val effectiveType = sc?.type ?: fallbackType
                val effectiveTargetId = sc?.targetId ?: fallbackTargetId
                val effectiveTitle = sc?.title ?: effectiveTargetId

                when (effectiveType) {
                    "TASK" -> {
                        val validTaskId = if (fallbackTaskId != -1L) {
                            fallbackTaskId
                        } else {
                            val candidateId = effectiveTargetId.toLongOrNull()
                            if (candidateId != null) {
                                candidateId
                            } else {
                                val task = repository.getOrCreateTaskForShortcut(
                                    title = effectiveTitle.ifBlank { "Discipline" },
                                    emoji = sc?.emoji ?: "🔥",
                                    category = "Routines",
                                    recurrence = RecurrenceType.DAILY
                                )
                                task.id
                            }
                        }
                        repository.toggleTaskCompletion(validTaskId, todayIso)
                    }
                    "HABIT" -> {
                        val habitId = effectiveTargetId.toLongOrNull()
                        val matchedHabitId = if (habitId != null) {
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
                                    title = effectiveTitle.ifBlank { "Routine Habit" },
                                    emoji = sc?.emoji ?: "☀️",
                                    colorHex = sc?.customColorHex ?: 0xFF10B981L
                                )
                            )
                        }
                        repository.toggleHabitCompletion(matchedHabitId, todayIso)
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
                            // Fallback to task deduplication & daily routine toggle
                            val task = repository.getOrCreateTaskForShortcut(
                                title = effectiveTitle.ifBlank { "Discipline" },
                                emoji = sc?.emoji ?: "🔥",
                                category = "Routines",
                                recurrence = RecurrenceType.DAILY
                            )
                            repository.toggleTaskCompletion(task.id, todayIso)
                        }
                    }
                    else -> {
                        val task = repository.getOrCreateTaskForShortcut(
                            title = effectiveTitle.ifBlank { "Discipline" },
                            emoji = sc?.emoji ?: "✅",
                            category = "Routines",
                            recurrence = RecurrenceType.DAILY
                        )
                        repository.toggleTaskCompletion(task.id, todayIso)
                    }
                }

                // 1. Notify global coordinator for in-app and widget sync
                WidgetRefreshCoordinator.notifyAllWidgetsChanged(context)

                // 2. Immediately update all active widget instances in-place
                try {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val thisWidget = ComponentName(context, QuickRemindersWidgetProvider::class.java)
                    val widgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                    for (wId in widgetIds) {
                        val updatedViews = buildWidgetViews(context, wId)
                        appWidgetManager.updateAppWidget(wId, updatedViews)
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
