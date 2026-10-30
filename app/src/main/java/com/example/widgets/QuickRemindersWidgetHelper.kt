package com.example.widgets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.LruCache
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

object QuickRemindersWidgetHelper {

    /**
     * LruCache for tile bitmaps keyed by shape+color+completion+size.
     * Holds up to 24 entries to cover a 10-tile widget with completion variants
     * without significant memory overhead.
     */
    private val bitmapCache = object : LruCache<String, Bitmap>(24) {}

    /**
     * Returns a cached tile bitmap, generating and caching it if not already present.
     *
     * @param widthPx   Bitmap width in pixels
     * @param heightPx  Bitmap height in pixels
     * @param shape     Shape type: "HEXAGON", "CIRCLE", or "SQUARE"
     * @param colorArgb ARGB color integer for the tile
     * @param isCompleted Whether the tile should display as completed (full opacity + vibrant border)
     */
    fun getCachedTileBitmap(
        widthPx: Int,
        heightPx: Int,
        shape: String,
        colorArgb: Int,
        isCompleted: Boolean
    ): Bitmap {
        val key = "${shape.uppercase()}_${colorArgb}_${isCompleted}_${widthPx}x${heightPx}"
        return bitmapCache.get(key) ?: generateTileBitmap(
            widthPx, heightPx, shape, colorArgb, isCompleted
        ).also { bitmapCache.put(key, it) }
    }

    /**
     * Generates a crisp rasterized Bitmap for widget tile shapes:
     * - HEXAGON (pointy-top axial regular hexagon with rounded vertices)
     * - CIRCLE (smooth circle)
     * - SQUARE (modern rounded rect)
     *
     * Incomplete state: Sleek dark surface background (#20242D) with a crisp border stroke,
     * matching the in-app QR canvas.
     * Completed state: Vibrant full-color fill with matching border.
     */
    fun generateTileBitmap(
        widthPx: Int = 144,
        heightPx: Int = 168,
        shape: String = "HEXAGON",
        colorArgb: Int = 0xFF10B981.toInt(),
        isCompleted: Boolean = false
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            if (isCompleted) {
                val alpha = 240
                color = (colorArgb and 0x00FFFFFF) or (alpha shl 24)
            } else {
                // Sleek dark surface matching in-app QR incomplete state
                color = 0xEE20242D.toInt()
            }
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = if (isCompleted) 5f else 4.5f
            color = if (isCompleted) {
                (colorArgb and 0x00FFFFFF) or (0xFF shl 24)
            } else {
                // Crisp outline stroke matching in-app QR outline
                0xFF424958.toInt()
            }
        }

        val cx = widthPx / 2f
        val cy = heightPx / 2f
        val strokePadding = 5f
        val r = (heightPx / 2f) - strokePadding

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
                val cornerRadius = r * 0.18f
                val path = createRoundedHexagonPath(cx, cy, r, cornerRadius)
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, borderPaint)
            }
        }

        return bitmap
    }

    /**
     * Creates a pointy-top regular hexagon path with smoothly rounded vertices,
     * mathematically identical to the in-app HoneycombTaskGrid composable.
     */
    fun createRoundedHexagonPath(
        cx: Float,
        cy: Float,
        radius: Float,
        cornerRadius: Float
    ): Path {
        val path = Path()
        val vertices = (0 until 6).map { i ->
            val angleRad = Math.toRadians((60.0 * i - 90.0)).toFloat()
            PointF(
                cx + radius * cos(angleRad),
                cy + radius * sin(angleRad)
            )
        }

        val cr = cornerRadius.coerceAtMost(radius * 0.35f)

        for (i in 0 until 6) {
            val curr = vertices[i]
            val prev = vertices[(i + 5) % 6]
            val next = vertices[(i + 1) % 6]

            val toPrevX = prev.x - curr.x
            val toPrevY = prev.y - curr.y
            val lenPrev = hypot(toPrevX, toPrevY)
            val p1x = if (lenPrev > 0f) curr.x + (toPrevX / lenPrev) * cr else curr.x
            val p1y = if (lenPrev > 0f) curr.y + (toPrevY / lenPrev) * cr else curr.y

            val toNextX = next.x - curr.x
            val toNextY = next.y - curr.y
            val lenNext = hypot(toNextX, toNextY)
            val p2x = if (lenNext > 0f) curr.x + (toNextX / lenNext) * cr else curr.x
            val p2y = if (lenNext > 0f) curr.y + (toNextY / lenNext) * cr else curr.y

            if (i == 0) {
                path.moveTo(p1x, p1y)
            } else {
                path.lineTo(p1x, p1y)
            }
            path.quadTo(curr.x, curr.y, p2x, p2y)
        }
        path.close()
        return path
    }

    fun calculateHexagonVertices(cx: Float, cy: Float, radius: Float): List<Pair<Float, Float>> {
        return (0 until 6).map { i ->
            val angleRad = Math.toRadians((i * 60.0) - 30.0)
            val x = (cx + radius * cos(angleRad)).toFloat()
            val y = (cy + radius * sin(angleRad)).toFloat()
            Pair(x, y)
        }
    }

    /**
     * Mathematical geometry formula for pointy-top axial honeycomb grid:
     * - Physical X = hStep * (q + r / 2.0)
     * - Physical Y = vStep * r
     * where hStep = hexWidth + spacing = sqrt(3) * radius + spacing
     *       vStep = 1.5 * radius + spacing
     */
    fun calculateAxialStaggerFactor(r: Int): Float = if (r % 2 != 0) 0.5f else 0f

    fun isOddStaggeredRow(r: Int): Boolean = (r % 2 != 0)

    fun calculateHexWidth(radius: Float): Float = (kotlin.math.sqrt(3.0) * radius).toFloat()

    fun calculateHexHeight(radius: Float): Float = 2f * radius

    /**
     * Evicts all cached bitmaps. Call when app theme/colors change globally.
     */
    fun clearCache() {
        bitmapCache.evictAll()
    }
}
