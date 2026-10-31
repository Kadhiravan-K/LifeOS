package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.PlanEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.preferences.UserPreferences
import com.example.domain.analytics.ProductivityRadarData
import com.example.domain.analytics.ProductivityRadarEngine
import com.example.domain.model.HabitItem
import com.example.domain.model.PlanItem
import com.example.domain.model.PlanLevel
import com.example.domain.model.PlanMilestone
import com.example.domain.model.RecurrenceType
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

/**
 * Home Screen Launcher AppWidget displaying the live 5-axis LifeOS Productivity Radar (#17).
 * Renders a crisp vector spider polygon, dimension breakdown, and overall score.
 */
class ProductivityRadarWidgetProvider : AppWidgetProvider() {

    companion object {
        fun notifyDataChanged(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, ProductivityRadarWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, ProductivityRadarWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }

        /**
         * Renders the 5-axis Productivity Radar chart to an Android Bitmap.
         */
        fun generateRadarBitmap(data: ProductivityRadarData, size: Int = 400): Bitmap {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val n = data.dimensions.size.coerceAtLeast(3)
            val center = size / 2f
            val maxR = size * 0.36f

            val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#40FFFFFF")
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
            }

            // 1. Concentric Web Rings (20%, 40%, 60%, 80%, 100%)
            listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f).forEach { scale ->
                val ringPath = Path()
                for (i in 0 until n) {
                    val angle = (2 * Math.PI * i / n - Math.PI / 2).toFloat()
                    val x = center + maxR * scale * cos(angle)
                    val y = center + maxR * scale * sin(angle)
                    if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
                }
                ringPath.close()
                canvas.drawPath(ringPath, gridPaint)
            }

            // 2. Spoke lines
            for (i in 0 until n) {
                val angle = (2 * Math.PI * i / n - Math.PI / 2).toFloat()
                val x = center + maxR * cos(angle)
                val y = center + maxR * sin(angle)
                canvas.drawLine(center, center, x, y, gridPaint)
            }

            // 3. Shaded Data Polygon
            val dataPath = Path()
            val vertexPoints = mutableListOf<Pair<Float, Float>>()
            for (i in 0 until n) {
                val fraction = (data.dimensions[i].scorePercent.toFloat() / 100f).coerceIn(0.12f, 1f)
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
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("${data.overallScorePercent}%", center, center + 7f, scoreTextPaint)

            return bitmap
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val prefs = UserPreferences(context)
                val todayIso = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

                val activeProfileId = try {
                    prefs.activeProfileId.first()
                } catch (_: Exception) {
                    1L
                }

                val activeProfile = db.profileDao().getProfileById(activeProfileId)
                val profileName = activeProfile?.name ?: "Personal"
                val profileEmoji = activeProfile?.emoji ?: "👤"

                // Fetch raw entity data for active profile
                val taskEntities = db.taskDao().getTasksForProfileList(activeProfileId)
                val habitEntities = db.habitDao().getHabitsForProfileList(activeProfileId)
                val waterLog = db.waterLogDao().getWaterLog(todayIso)
                val waterGoal = try { prefs.waterGoalMl.first() } catch (_: Exception) { 2000 }
                val planEntities = db.planDao().getPlansForProfileList(activeProfileId)

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

                val radarBitmap = generateRadarBitmap(radarData, size = 420)

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_productivity_radar)

                    views.setImageViewBitmap(R.id.widget_radar_chart_image, radarBitmap)
                    views.setTextViewText(R.id.widget_radar_score, "${radarData.overallScorePercent}%")
                    views.setTextViewText(R.id.widget_radar_tier, "${radarData.ratingTier} ${radarData.ratingTierEmoji}")
                    views.setTextViewText(R.id.widget_radar_profile_badge, "$profileEmoji $profileName")

                    val summaryStr = radarData.dimensions.joinToString(" • ") {
                        "${it.dimension.emoji} ${it.scorePercent}%"
                    }
                    views.setTextViewText(R.id.widget_radar_summary, summaryStr)

                    // Intent to launch LifeOS Analytics screen
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("extra_destination", "analytics")
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
            } catch (e: Exception) {
                // Graceful degradation on database busy or cold start
            } finally {
                pendingResult.finish()
            }
        }
    }
}
