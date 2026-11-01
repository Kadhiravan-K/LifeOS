package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.domain.analytics.ProductivityRadarData
import com.example.domain.analytics.ProductivityRadarEngine
import com.example.domain.model.HabitItem
import com.example.domain.model.PlanItem
import com.example.domain.model.PlanLevel
import com.example.domain.model.PlanMilestone
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.notifications.runAsync
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

/**
 * Enhanced Productivity Radar & Content Selection Home Screen Widget Provider (#17, Issue #4).
 *
 * Supports:
 * 1. Per-instance profile binding via [WidgetProfileManager].
 * 2. Multi-content mode selection (Radar, Today's Tasks, Priority Overview, Completion Summary, Consistency).
 * 3. Elimination of synthetic mock data and genuine zero-state handling.
 * 4. High-resolution vector polygon rendering on IO thread.
 */
class ProductivityRadarWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_REFRESH_DATA = "com.example.lifeos.ACTION_RADAR_WIDGET_REFRESH"

        fun notifyDataChanged(context: Context) {
            val intent = Intent(context, ProductivityRadarWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val mgr = AppWidgetManager.getInstance(context)
                val ids = mgr.getAppWidgetIds(
                    android.content.ComponentName(context, ProductivityRadarWidgetProvider::class.java)
                )
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }

        /**
         * Renders the 5-axis radar polygon directly onto an in-memory ARGB Bitmap for RemoteViews.
         */
        fun generateRadarBitmap(data: ProductivityRadarData, size: Int = 420): Bitmap {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val center = size / 2f
            val maxR = size * 0.36f
            val n = data.dimensions.size.coerceAtLeast(3)

            // 1. Concentric pentagon web rings (20%, 40%, 60%, 80%, 100%)
            val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#33FFFFFF")
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
            }

            listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f).forEach { scale ->
                val gridPath = Path()
                for (i in 0 until n) {
                    val angle = (2 * Math.PI * i / n - Math.PI / 2).toFloat()
                    val x = center + maxR * scale * cos(angle)
                    val y = center + maxR * scale * sin(angle)
                    if (i == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
                }
                gridPath.close()
                canvas.drawPath(gridPath, gridPaint)
            }

            // 2. Spoke lines
            for (i in 0 until n) {
                val angle = (2 * Math.PI * i / n - Math.PI / 2).toFloat()
                val x = center + maxR * cos(angle)
                val y = center + maxR * sin(angle)
                canvas.drawLine(center, center, x, y, gridPaint)
            }

            // 3. Shaded Data Polygon (only rendered when genuine live data exists)
            val hasLivePolygon = data.hasSufficientData && data.overallScorePercent > 0
            val dataPath = Path()
            val vertexPoints = mutableListOf<Pair<Float, Float>>()
            if (hasLivePolygon) {
                for (i in 0 until n) {
                    val fraction = (data.dimensions[i].scorePercent.toFloat() / 100f).coerceIn(0.08f, 1f)
                    val angle = (2 * Math.PI * i / n - Math.PI / 2).toFloat()
                    val r = maxR * fraction
                    val x = center + r * cos(angle)
                    val y = center + r * sin(angle)
                    vertexPoints.add(Pair(x, y))
                    if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                }
                dataPath.close()

                // Fill with emerald
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#4410B981")
                    style = Paint.Style.FILL
                }
                canvas.drawPath(dataPath, fillPaint)

                // Outline with crisp vibrant emerald stroke
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#FF10B981")
                    style = Paint.Style.STROKE
                    strokeWidth = 6f
                    strokeJoin = Paint.Join.ROUND
                }
                canvas.drawPath(dataPath, strokePaint)

                // 4. Vertex dots
                val dotOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#FF10B981")
                    style = Paint.Style.FILL
                }
                val dotInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    style = Paint.Style.FILL
                }
                for ((vx, vy) in vertexPoints) {
                    canvas.drawCircle(vx, vy, 8f, dotOuterPaint)
                    canvas.drawCircle(vx, vy, 4f, dotInnerPaint)
                }
            }

            // 5. Dimension labels around perimeter
            val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 28f
                textAlign = Paint.Align.CENTER
            }
            for (i in 0 until n) {
                val angle = (2 * Math.PI * i / n - Math.PI / 2).toFloat()
                val labelR = maxR + 32f
                val lx = center + labelR * cos(angle)
                val ly = center + labelR * sin(angle) + 10f
                canvas.drawText(data.dimensions[i].dimension.emoji, lx, ly, emojiPaint)
            }

            // 6. Center Score Pill
            val centerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#E6181E29")
                style = Paint.Style.FILL
            }
            val centerStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#6010B981")
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawCircle(center, center, 32f, centerBgPaint)
            canvas.drawCircle(center, center, 32f, centerStrokePaint)

            val scoreTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#FF10B981")
                textSize = 20f
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val displayScore = if (hasLivePolygon) "${data.overallScorePercent}%" else "0%"
            canvas.drawText(displayScore, center, center + 7f, scoreTextPaint)

            return bitmap
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        for (id in appWidgetIds) {
            WidgetProfileManager.removeWidget(context, id)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        runAsync(timeoutMs = 9_000L) {
            try {
                val db = AppDatabase.getInstance(context)
                val prefs = UserPreferences(context)
                val todayIso = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

                val globalProfileId = try {
                    prefs.activeProfileId.first()
                } catch (_: Exception) {
                    1L
                }

                for (widgetId in appWidgetIds) {
                    val targetProfileId = WidgetProfileManager.getWidgetProfileId(context, widgetId, globalProfileId)
                    val contentMode = WidgetProfileManager.getWidgetContentMode(context, widgetId)

                    val activeProfile = db.profileDao().getProfileById(targetProfileId)
                    val profileName = activeProfile?.name ?: "Personal"
                    val profileEmoji = activeProfile?.emoji ?: "👤"

                    // Fetch raw entity data for targeted profile
                    val taskEntities = db.taskDao().getTasksForProfileList(targetProfileId)
                    val habitEntities = db.habitDao().getHabitsForProfileList(targetProfileId)
                    val waterLog = db.waterLogDao().getWaterLog(todayIso)
                    val waterGoal = try { prefs.waterGoalMl.first() } catch (_: Exception) { 2000 }
                    val planEntities = db.planDao().getPlansForProfileList(targetProfileId)

                    // Convert to domain items
                    val taskItems = taskEntities.map { entity ->
                        TaskItem(
                            id = entity.id,
                            profileId = entity.profileId,
                            title = entity.title,
                            description = entity.description,
                            category = entity.category,
                            priority = TaskPriority.fromString(entity.priority),
                            dueDateIso = entity.dueDateIso,
                            isOneTimeCompleted = entity.isOneTimeCompleted,
                            isArchived = entity.isArchived
                        )
                    }

                    val habitItems = habitEntities.map { entity ->
                        val isDoneToday = try {
                            entity.completedDatesJson.contains(todayIso)
                        } catch (_: Exception) {
                            false
                        }
                        HabitItem(
                            id = entity.id,
                            profileId = entity.profileId,
                            title = entity.title,
                            emoji = entity.emoji,
                            description = entity.description,
                            isCompletedToday = isDoneToday
                        )
                    }

                    val planItems = planEntities.map { entity ->
                        val milestones = db.planDao().getMilestonesForPlanList(entity.id).map { m ->
                            PlanMilestone(
                                id = m.id,
                                planId = m.planId,
                                title = m.title,
                                isCompleted = m.isCompleted,
                                orderIndex = m.orderIndex
                            )
                        }
                        PlanItem(
                            id = entity.id,
                            profileId = entity.profileId,
                            title = entity.title,
                            description = entity.description,
                            level = PlanLevel.fromString(entity.level),
                            startDateIso = entity.startDateIso,
                            targetDateIso = entity.targetDateIso,
                            milestones = milestones
                        )
                    }

                    val currentStreak = habitItems.maxOfOrNull { it.currentStreak } ?: 0

                    val radarData = ProductivityRadarEngine.calculate(
                        tasks = taskItems,
                        habits = habitItems,
                        waterCurrentMl = waterLog?.amountMl ?: 0,
                        waterGoalMl = waterGoal,
                        plans = planItems,
                        currentStreak = currentStreak,
                        todayIso = todayIso
                    )

                    val views = RemoteViews(context.packageName, R.layout.widget_productivity_radar)
                    views.setTextViewText(R.id.widget_radar_profile_badge, "$profileEmoji $profileName")

                    when (contentMode) {
                        WidgetProfileManager.WidgetContentMode.RADAR -> {
                            val radarBitmap = generateRadarBitmap(radarData, size = 420)
                            views.setViewVisibility(R.id.widget_radar_chart_image, View.VISIBLE)
                            views.setImageViewBitmap(R.id.widget_radar_chart_image, radarBitmap)
                            views.setTextViewText(R.id.widget_radar_header_title, "⚡ LifeOS • Radar")
                            views.setTextViewText(R.id.widget_radar_score, "${radarData.overallScorePercent}%")
                            views.setTextViewText(R.id.widget_radar_tier, "${radarData.ratingTier} ${radarData.ratingTierEmoji}")
                            val summaryStr = radarData.dimensions.joinToString(" • ") {
                                "${it.dimension.emoji} ${it.scorePercent}%"
                            }
                            views.setTextViewText(R.id.widget_radar_summary, summaryStr)
                        }

                        WidgetProfileManager.WidgetContentMode.TODAY_TASKS -> {
                            val todayTasks = taskItems.filter { !it.isArchived && it.dueDateIso == todayIso }
                            val completedToday = todayTasks.count { it.isOneTimeCompleted }
                            val totalToday = todayTasks.size
                            val radarBitmap = generateRadarBitmap(radarData, size = 420)
                            views.setViewVisibility(R.id.widget_radar_chart_image, View.VISIBLE)
                            views.setImageViewBitmap(R.id.widget_radar_chart_image, radarBitmap)
                            views.setTextViewText(R.id.widget_radar_header_title, "📅 LifeOS • Today's Tasks")
                            views.setTextViewText(R.id.widget_radar_score, "$completedToday / $totalToday")
                            views.setTextViewText(R.id.widget_radar_tier, if (totalToday == 0) "No tasks scheduled" else "$completedToday done")
                            val todayTitles = if (todayTasks.isEmpty()) "Tap to add tasks for today" else todayTasks.take(2).joinToString(" • ") { it.title }
                            views.setTextViewText(R.id.widget_radar_summary, todayTitles)
                        }

                        WidgetProfileManager.WidgetContentMode.UPCOMING -> {
                            val upcomingTasks = taskItems.filter { !it.isArchived && !it.isOneTimeCompleted && it.dueDateIso > todayIso }
                            val radarBitmap = generateRadarBitmap(radarData, size = 420)
                            views.setViewVisibility(R.id.widget_radar_chart_image, View.VISIBLE)
                            views.setImageViewBitmap(R.id.widget_radar_chart_image, radarBitmap)
                            views.setTextViewText(R.id.widget_radar_header_title, "⏳ LifeOS • Upcoming")
                            views.setTextViewText(R.id.widget_radar_score, "${upcomingTasks.size}")
                            views.setTextViewText(R.id.widget_radar_tier, "Upcoming Tasks")
                            val nextTitle = upcomingTasks.firstOrNull()?.let { "Next: ${it.title} (${it.dueDateIso})" } ?: "No upcoming tasks queued"
                            views.setTextViewText(R.id.widget_radar_summary, nextTitle)
                        }

                        WidgetProfileManager.WidgetContentMode.PRIORITY -> {
                            val activeUncompleted = taskItems.filter { !it.isArchived && !it.isOneTimeCompleted }
                            val p1Count = activeUncompleted.count { it.priority == TaskPriority.P1 }
                            val p2Count = activeUncompleted.count { it.priority == TaskPriority.P2 }
                            val p3Count = activeUncompleted.count { it.priority == TaskPriority.P3 }
                            val radarBitmap = generateRadarBitmap(radarData, size = 420)
                            views.setViewVisibility(R.id.widget_radar_chart_image, View.VISIBLE)
                            views.setImageViewBitmap(R.id.widget_radar_chart_image, radarBitmap)
                            views.setTextViewText(R.id.widget_radar_header_title, "🎯 LifeOS • Priority")
                            views.setTextViewText(R.id.widget_radar_score, "${p1Count} P1")
                            views.setTextViewText(R.id.widget_radar_tier, "${p2Count} High • ${p3Count} Normal")
                            views.setTextViewText(R.id.widget_radar_summary, "Urgent focus: $p1Count priority tasks")
                        }

                        WidgetProfileManager.WidgetContentMode.COMPLETION_SUMMARY -> {
                            val activeTasks = taskItems.filter { !it.isArchived }
                            val completedTasks = activeTasks.count { it.isOneTimeCompleted }
                            val totalTasks = activeTasks.size
                            val pct = if (totalTasks > 0) ((completedTasks.toFloat() / totalTasks) * 100).toInt() else 0
                            val radarBitmap = generateRadarBitmap(radarData, size = 420)
                            views.setViewVisibility(R.id.widget_radar_chart_image, View.VISIBLE)
                            views.setImageViewBitmap(R.id.widget_radar_chart_image, radarBitmap)
                            views.setTextViewText(R.id.widget_radar_header_title, "📊 LifeOS • Summary")
                            views.setTextViewText(R.id.widget_radar_score, "$pct%")
                            views.setTextViewText(R.id.widget_radar_tier, "$completedTasks of $totalTasks done")
                            val habitsDone = habitItems.count { it.isCompletedToday }
                            views.setTextViewText(R.id.widget_radar_summary, "$habitsDone / ${habitItems.size} habits checked today")
                        }

                        WidgetProfileManager.WidgetContentMode.CONSISTENCY -> {
                            val radarBitmap = generateRadarBitmap(radarData, size = 420)
                            views.setViewVisibility(R.id.widget_radar_chart_image, View.VISIBLE)
                            views.setImageViewBitmap(R.id.widget_radar_chart_image, radarBitmap)
                            views.setTextViewText(R.id.widget_radar_header_title, "🔥 LifeOS • Consistency")
                            views.setTextViewText(R.id.widget_radar_score, "${currentStreak}d")
                            views.setTextViewText(R.id.widget_radar_tier, if (currentStreak > 0) "Daily Streak Ongoing" else "Start a new streak")
                            views.setTextViewText(R.id.widget_radar_summary, "Habit momentum: $currentStreak consecutive days")
                        }
                    }

                    // Intent to launch LifeOS Analytics screen for this widget's profile
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("extra_destination", "analytics")
                        putExtra("extra_profile_id", targetProfileId)
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        widgetId,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_radar_root, pendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (_: Exception) {
                // Graceful degradation on database busy or cold start
            }
        }
    }
}
