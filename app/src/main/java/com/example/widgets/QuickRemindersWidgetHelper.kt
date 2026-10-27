package com.example.widgets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

object QuickRemindersWidgetHelper {

    /**
     * Generates a crisp rasterized Bitmap for widget tile shapes:
     * - HEXAGON (pointy-top axial regular hexagon)
     * - CIRCLE (smooth circle)
     * - SQUARE (modern rounded rect)
     */
    fun generateTileBitmap(
        widthPx: Int = 180,
        heightPx: Int = 180,
        shape: String = "HEXAGON",
        colorArgb: Int = 0xFF3B82F6.toInt(),
        isCompleted: Boolean = false
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            val alpha = if (isCompleted) 220 else 75
            color = (colorArgb and 0x00FFFFFF) or (alpha shl 24)
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = if (isCompleted) colorArgb else 0x55FFFFFF
        }

        val cx = widthPx / 2f
        val cy = heightPx / 2f
        val padding = 8f
        val r = (heightPx / 2f) - padding

        when (shape.uppercase()) {
            "CIRCLE" -> {
                canvas.drawCircle(cx, cy, r, fillPaint)
                canvas.drawCircle(cx, cy, r, borderPaint)
            }
            "SQUARE" -> {
                val halfSide = r * 0.85f
                val rect = RectF(cx - halfSide, cy - halfSide, cx + halfSide, cy + halfSide)
                val cornerRadius = 24f
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint)
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
            }
            else -> { // HEXAGON (Default)
                val path = createHexagonPath(cx, cy, r)
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, borderPaint)
            }
        }

        return bitmap
    }

    fun calculateHexagonVertices(cx: Float, cy: Float, radius: Float): List<Pair<Float, Float>> {
        return (0 until 6).map { i ->
            val angleRad = Math.toRadians((i * 60.0) - 30.0)
            val x = (cx + radius * cos(angleRad)).toFloat()
            val y = (cy + radius * sin(angleRad)).toFloat()
            Pair(x, y)
        }
    }

    private fun createHexagonPath(cx: Float, cy: Float, radius: Float): Path {
        val path = Path()
        val vertices = calculateHexagonVertices(cx, cy, radius)
        vertices.forEachIndexed { i, (x, y) ->
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return path
    }

    /**
     * Mathematical geometry formula for pointy-top axial honeycomb grid:
     * - Physical X = hStep * (q + r / 2.0)
     * - Physical Y = vStep * r
     * where hStep = hexWidth + spacing = sqrt(3) * radius + spacing
     *       vStep = 1.5 * radius + spacing
     *
     * In an Android RemoteViews ListView:
     * Even rows (r % 2 == 0) have no horizontal stagger: 3 columns (weights: 1, 1, 1).
     * Odd rows (r % 2 != 0) are staggered by exactly +0.5 tile width: 2 columns with
     * half-weight margin spacers (weights: 0.5, 1, 1, 0.5).
     */
    fun calculateAxialStaggerFactor(r: Int): Float = if (r % 2 != 0) 0.5f else 0f

    fun isOddStaggeredRow(r: Int): Boolean = (r % 2 != 0)

    fun calculateHexWidth(radius: Float): Float = (kotlin.math.sqrt(3.0) * radius).toFloat()

    fun calculateHexHeight(radius: Float): Float = 2f * radius
}
