package com.example.widgets

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.JsonMappers
import com.example.domain.model.TodoistColorPalette
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class QuickRemindersRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return QuickRemindersRemoteViewsFactory(this.applicationContext)
    }
}

internal class QuickRemindersRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    data class WidgetQrItem(
        val shortcutId: Long,
        val type: String,
        val targetId: String,
        val taskId: Long?,
        val title: String,
        val emoji: String,
        val shape: String,
        val colorArgb: Int,
        val isCompleted: Boolean,
        val coordQ: Int = 0,
        val coordR: Int = 0
    )

    data class WidgetRow(
        val isThreeColumn: Boolean,
        val items: List<WidgetQrItem>
    )

    private var tileItems = listOf<WidgetQrItem>()
    private var widgetRows = listOf<WidgetRow>()

    private val containerIds = intArrayOf(R.id.tile_0_container, R.id.tile_1_container, R.id.tile_2_container)
    private val shapeIds = intArrayOf(R.id.tile_0_shape, R.id.tile_1_shape, R.id.tile_2_shape)
    private val emojiIds = intArrayOf(R.id.tile_0_emoji, R.id.tile_1_emoji, R.id.tile_2_emoji)
    private val titleIds = intArrayOf(R.id.tile_0_title, R.id.tile_1_title, R.id.tile_2_title)
    private val checkIds = intArrayOf(R.id.tile_0_check, R.id.tile_1_check, R.id.tile_2_check)

    private fun buildRows(items: List<WidgetQrItem>): List<WidgetRow> {
        val rows = mutableListOf<WidgetRow>()
        if (items.isEmpty()) return rows

        val hasAxialCoords = items.any { it.coordR != 0 || it.coordQ != 0 }
        if (hasAxialCoords) {
            val groupedByRow = items.groupBy { it.coordR }
            val sortedRows = groupedByRow.keys.sorted()

            sortedRows.forEach { r ->
                val rowItems = groupedByRow[r]!!.sortedBy { it.coordQ.toFloat() + (r.toFloat() / 2f) }
                val isThree = (r % 2 == 0)
                val maxCol = if (isThree) 3 else 2

                var idx = 0
                while (idx < rowItems.size) {
                    val chunk = rowItems.subList(idx, minOf(idx + maxCol, rowItems.size))
                    rows.add(WidgetRow(isThreeColumn = isThree, items = chunk))
                    idx += chunk.size
                }
            }
        } else {
            var index = 0
            var isThree = true
            while (index < items.size) {
                val count = if (isThree) 3 else 2
                val chunk = items.subList(index, minOf(index + count, items.size))
                rows.add(WidgetRow(isThreeColumn = isThree, items = chunk))
                index += chunk.size
                isThree = !isThree
            }
        }
        return rows
    }

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val identityToken = Binder.clearCallingIdentity()
        try {
            runBlocking {
                val db = AppDatabase.getInstance(context)
                val shortcuts = db.honeycombShortcutDao().getAllShortcutsList()
                val tasks = db.taskDao().getAllTasksList()
                val habits = db.habitDao().getAllHabitsList()
                val projects = db.projectDao().getAllProjectsList()
                val todayIso = DateUtils.todayString()
                val waterLog = db.waterLogDao().getWaterLog(todayIso)
                val prefs = UserPreferences(context)
                val waterGoal = try { prefs.waterGoalMl.first() } catch (_: Exception) { 2000 }
                val activeChallenge = db.challengeDao().getActiveChallenge()
                val challengeLogs = activeChallenge?.let { JsonMappers.parseChallengeDailyLogs(it.dailyLogsJson) }
                val todayChallengeLog = challengeLogs?.get(todayIso)

                val items = mutableListOf<WidgetQrItem>()
                for (sc in shortcuts) {
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
                            isCompleted = todayChallengeLog?.completedRuleIds?.contains(sc.targetId) == true ||
                                          todayChallengeLog?.completedRuleIds?.contains(sc.title) == true
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

                    items.add(
                        WidgetQrItem(
                            shortcutId = sc.id,
                            type = sc.type,
                            targetId = sc.targetId,
                            taskId = taskId,
                            title = displayTitle,
                            emoji = sc.emoji,
                            shape = sc.shape,
                            colorArgb = effectiveColor.toInt(),
                            isCompleted = isCompleted,
                            coordQ = sc.coordQ,
                            coordR = sc.coordR
                        )
                    )
                }
                tileItems = items
                widgetRows = buildRows(items)
            }
        } finally {
            Binder.restoreCallingIdentity(identityToken)
        }
    }

    override fun onDestroy() {
        tileItems = emptyList()
        widgetRows = emptyList()
    }

    override fun getCount(): Int = widgetRows.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position !in widgetRows.indices) {
            return RemoteViews(context.packageName, R.layout.widget_qr_row_3)
        }

        val row = widgetRows[position]
        val layoutRes = if (row.isThreeColumn) R.layout.widget_qr_row_3 else R.layout.widget_qr_row_2
        val rv = RemoteViews(context.packageName, layoutRes)
        val maxTiles = if (row.isThreeColumn) 3 else 2

        for (i in 0 until maxTiles) {
            if (i < row.items.size) {
                val item = row.items[i]
                rv.setViewVisibility(containerIds[i], View.VISIBLE)
                rv.setTextViewText(emojiIds[i], item.emoji)
                rv.setTextViewText(titleIds[i], item.title)

                val bitmap = QuickRemindersWidgetHelper.generateTileBitmap(
                    widthPx = 180,
                    heightPx = 180,
                    shape = item.shape,
                    colorArgb = item.colorArgb,
                    isCompleted = item.isCompleted
                )
                rv.setImageViewBitmap(shapeIds[i], bitmap)

                rv.setViewVisibility(
                    checkIds[i],
                    if (item.isCompleted) View.VISIBLE else View.GONE
                )

                val fillInIntent = Intent().apply {
                    putExtra("extra_shortcut_id", item.shortcutId)
                    putExtra("extra_type", item.type)
                    putExtra("extra_target_id", item.targetId)
                    putExtra("extra_task_id", item.taskId ?: -1L)
                    putExtra("extra_is_completed", item.isCompleted)
                }
                rv.setOnClickFillInIntent(containerIds[i], fillInIntent)
            } else {
                rv.setViewVisibility(containerIds[i], View.INVISIBLE)
            }
        }

        return rv
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
