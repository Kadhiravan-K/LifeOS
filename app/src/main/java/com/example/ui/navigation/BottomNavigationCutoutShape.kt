package com.example.ui.navigation

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Custom Material 3 Shape for the bottom navigation bar featuring a
 * smooth semicircular cradle cutout in the top center for the floating Quick Add button.
 */
class BottomNavigationCutoutShape(
    private val cutoutRadius: Dp = 32.dp,
    private val cornerRadius: Dp = 16.dp,
    private val cradleMargin: Dp = 6.dp
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val width = size.width
        val height = size.height

        with(density) {
            val r = (cutoutRadius + cradleMargin).toPx()
            val cr = cornerRadius.toPx()
            val centerX = width / 2f
            val shoulder = 8.dp.toPx()

            path.reset()
            // Top-left corner
            path.moveTo(0f, cr)
            path.quadraticTo(0f, 0f, cr, 0f)

            // Line to left side of cutout
            val cutoutLeft = centerX - r
            path.lineTo(cutoutLeft - shoulder, 0f)

            // Smooth cubic curve down into semicircular cradle and back up
            path.cubicTo(
                cutoutLeft, 0f,
                centerX - r * 0.7f, r * 0.95f,
                centerX, r * 0.95f
            )
            path.cubicTo(
                centerX + r * 0.7f, r * 0.95f,
                centerX + r, 0f,
                centerX + r + shoulder, 0f
            )

            // Line to top-right corner
            path.lineTo(width - cr, 0f)
            path.quadraticTo(width, 0f, width, cr)

            // Bottom-right and bottom-left edges
            path.lineTo(width, height)
            path.lineTo(0f, height)
            path.close()
        }

        return Outline.Generic(path)
    }
}
