package com.example.widgets

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.local.AppDatabase
import com.example.domain.model.TodoistColorPalette
import com.example.utils.DateUtils
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
        val taskId: Long?,
        val title: String,
        val emoji: String,
        val shape: String,
        val colorArgb: Int,
        val isCompleted: Boolean
    )

    private var tileItems = listOf<WidgetQrItem>()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val identityToken = Binder.clearCallingIdentity()
        try {
            runBlocking {
                val db = AppDatabase.getInstance(context)
                val shortcuts = db.honeycombShortcutDao().getAllShortcutsList()
                val tasks = db.taskDao().getAllTasksList()
                val projects = db.projectDao().getAllProjectsList()
                val todayIso = DateUtils.todayString()

                val items = mutableListOf<WidgetQrItem>()
                for (sc in shortcuts) {
                    val displayTitle = if (!sc.customTileName.isNullOrBlank()) sc.customTileName else sc.title
                    var taskId: Long? = null
                    var isCompleted = false
                    var projectColorHex: Long? = null

                    if (sc.type == "TASK") {
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

                    val effectiveColor = when (sc.colorSource) {
                        "CUSTOM" -> sc.customColorHex ?: 0xFF3B82F6
                        "PROJECT" -> projectColorHex ?: sc.customColorHex ?: 0xFF3B82F6
                        else -> 0xFF3B82F6
                    }

                    items.add(
                        WidgetQrItem(
                            shortcutId = sc.id,
                            taskId = taskId,
                            title = displayTitle,
                            emoji = sc.emoji,
                            shape = sc.shape,
                            colorArgb = effectiveColor.toInt(),
                            isCompleted = isCompleted
                        )
                    )
                }
                tileItems = items
            }
        } finally {
            Binder.restoreCallingIdentity(identityToken)
        }
    }

    override fun onDestroy() {
        tileItems = emptyList()
    }

    override fun getCount(): Int = tileItems.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position !in tileItems.indices) {
            return RemoteViews(context.packageName, R.layout.widget_quick_reminder_item)
        }

        val item = tileItems[position]
        val rv = RemoteViews(context.packageName, R.layout.widget_quick_reminder_item)

        // 1. Text & Emoji
        rv.setTextViewText(R.id.widget_item_emoji, item.emoji)
        rv.setTextViewText(R.id.widget_item_title, item.title)

        // 2. Shape Bitmap
        val bitmap = QuickRemindersWidgetHelper.generateTileBitmap(
            widthPx = 180,
            heightPx = 180,
            shape = item.shape,
            colorArgb = item.colorArgb,
            isCompleted = item.isCompleted
        )
        rv.setImageViewBitmap(R.id.widget_item_shape, bitmap)

        // 3. Completion Checkmark
        rv.setViewVisibility(
            R.id.widget_item_check,
            if (item.isCompleted) View.VISIBLE else View.GONE
        )

        // 4. Fill-In Intent for click handling
        val fillInIntent = Intent().apply {
            putExtra("extra_shortcut_id", item.shortcutId)
            putExtra("extra_task_id", item.taskId ?: -1L)
            putExtra("extra_is_completed", item.isCompleted)
        }
        rv.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        return rv
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return if (position in tileItems.indices) tileItems[position].shortcutId else position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}
