package com.example.ui.theme.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

/**
 * Offline Canvas rendering engine for theme motifs, runes, cyber grids, compasses, and starfields.
 * All artwork is calculated dynamically on-device with zero network and zero bitmap bloat.
 */
object ThemeAssetManager {

    /**
     * Draws a rotating Arcane Magic Sigil with concentric circles and runic spokes.
     */
    fun drawArcaneSigil(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        rotationDegrees: Float,
        primaryColor: Color,
        goldColor: Color
    ) {
        scope.apply {
            // Outer glowing ring
            drawCircle(
                color = goldColor.copy(alpha = 0.8f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // Inner mystic circle
            drawCircle(
                color = primaryColor.copy(alpha = 0.6f),
                radius = radius * 0.7f,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // Core sigil node
            drawCircle(
                color = goldColor,
                radius = radius * 0.15f,
                center = center
            )

            // Rotating runic hexagram / spokes
            rotate(rotationDegrees, pivot = center) {
                val spokeCount = 6
                for (i in 0 until spokeCount) {
                    val angle = (i * (360f / spokeCount)) * (Math.PI / 180.0)
                    val outerX = (center.x + radius * cos(angle)).toFloat()
                    val outerY = (center.y + radius * sin(angle)).toFloat()
                    drawLine(
                        color = goldColor.copy(alpha = 0.6f),
                        start = center,
                        end = Offset(outerX, outerY),
                        strokeWidth = 1.5f
                    )
                    // Small outer rune nodes
                    drawCircle(
                        color = primaryColor,
                        radius = 4f,
                        center = Offset(outerX, outerY)
                    )
                }
            }
        }
    }

    /**
     * Draws a Cyber Nexus tech grid with scanning pulse lines and glowing corner notches.
     */
    fun drawCyberGrid(
        scope: DrawScope,
        width: Float,
        height: Float,
        scanLineProgress: Float,
        cyanColor: Color,
        gridColor: Color
    ) {
        scope.apply {
            val step = 40f
            // Vertical grid lines
            var x = 0f
            while (x < width) {
                drawLine(
                    color = gridColor.copy(alpha = 0.12f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                x += step
            }
            // Horizontal grid lines
            var y = 0f
            while (y < height) {
                drawLine(
                    color = gridColor.copy(alpha = 0.12f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                y += step
            }

            // Sweeping scan line
            val scanY = height * scanLineProgress
            drawLine(
                color = cyanColor.copy(alpha = 0.45f),
                start = Offset(0f, scanY),
                end = Offset(width, scanY),
                strokeWidth = 2.5f
            )
        }
    }

    /**
     * Draws an Adventure Realm fantasy compass with a spinning directional needle.
     */
    fun drawFantasyCompass(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        needleAngle: Float,
        woodGold: Color,
        mossGreen: Color
    ) {
        scope.apply {
            // Embossed brass compass ring
            drawCircle(
                color = woodGold,
                radius = radius,
                center = center,
                style = Stroke(width = 3f)
            )
            drawCircle(
                color = woodGold.copy(alpha = 0.4f),
                radius = radius * 0.85f,
                center = center,
                style = Stroke(width = 1f)
            )

            // Compass needle rotated
            rotate(needleAngle, pivot = center) {
                val needlePath = Path().apply {
                    moveTo(center.x, center.y - radius * 0.75f) // North tip
                    lineTo(center.x + radius * 0.2f, center.y)
                    lineTo(center.x, center.y + radius * 0.75f) // South tip
                    lineTo(center.x - radius * 0.2f, center.y)
                    close()
                }
                drawPath(path = needlePath, color = woodGold.copy(alpha = 0.85f))

                // North tip highlight
                val northTip = Path().apply {
                    moveTo(center.x, center.y - radius * 0.75f)
                    lineTo(center.x + radius * 0.2f, center.y)
                    lineTo(center.x, center.y)
                    close()
                }
                drawPath(path = northTip, color = mossGreen)
            }

            // Center brass pin
            drawCircle(
                color = woodGold,
                radius = radius * 0.18f,
                center = center
            )
        }
    }

    /**
     * Draws a Cosmic Explorer celestial system with orbiting satellite/planets.
     */
    fun drawCosmicOrbits(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        orbitProgress: Float,
        starColor: Color,
        nebulaColor: Color
    ) {
        scope.apply {
            // Primary orbital ellipse 1
            drawCircle(
                color = starColor.copy(alpha = 0.35f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // Secondary orbital ellipse 2
            drawCircle(
                color = nebulaColor.copy(alpha = 0.35f),
                radius = radius * 0.65f,
                center = center,
                style = Stroke(width = 1f)
            )

            // Radiant Central Star
            drawCircle(
                color = starColor,
                radius = radius * 0.22f,
                center = center
            )

            // Orbiting Planet 1
            val angle1 = (orbitProgress * 360f) * (Math.PI / 180.0)
            val p1X = (center.x + radius * cos(angle1)).toFloat()
            val p1Y = (center.y + radius * sin(angle1)).toFloat()
            drawCircle(
                color = starColor,
                radius = 6f,
                center = Offset(p1X, p1Y)
            )

            // Orbiting Moon 2 (counter-orbit)
            val angle2 = (-orbitProgress * 360f * 1.5) * (Math.PI / 180.0)
            val p2X = (center.x + (radius * 0.65f) * cos(angle2)).toFloat()
            val p2Y = (center.y + (radius * 0.65f) * sin(angle2)).toFloat()
            drawCircle(
                color = nebulaColor,
                radius = 4f,
                center = Offset(p2X, p2Y)
            )
        }
    }

    /**
     * Draws subtle ambient starfield particles for Cosmic theme.
     */
    fun drawStarfield(
        scope: DrawScope,
        width: Float,
        height: Float,
        starColor: Color
    ) {
        scope.apply {
            // Deterministic procedural stars
            val count = 28
            for (i in 0 until count) {
                val seed = i * 7919
                val x = (seed % 1000) / 1000f * width
                val y = ((seed * 31) % 1000) / 1000f * height
                val starRadius = if (i % 5 == 0) 2.2f else 1.2f
                val alpha = if (i % 3 == 0) 0.65f else 0.35f
                drawCircle(
                    color = starColor.copy(alpha = alpha),
                    radius = starRadius,
                    center = Offset(x, y)
                )
            }
        }
    }

    /**
     * Draws a rotating Glass Prismatic Refraction crystal with specular dispersion rays.
     */
    fun drawPrismRefraction(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        rotationDegrees: Float,
        primaryCyan: Color,
        secondaryViolet: Color
    ) {
        scope.apply {
            // Outer specular ring
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // Refractive hexagon prism
            rotate(rotationDegrees, pivot = center) {
                val hexPath = Path()
                for (i in 0 until 6) {
                    val angle = Math.toRadians((i * 60.0))
                    val px = center.x + radius * 0.75f * cos(angle).toFloat()
                    val py = center.y + radius * 0.75f * sin(angle).toFloat()
                    if (i == 0) hexPath.moveTo(px, py) else hexPath.lineTo(px, py)
                }
                hexPath.close()

                drawPath(
                    path = hexPath,
                    color = primaryCyan.copy(alpha = 0.7f),
                    style = Stroke(width = 2f)
                )

                // Refraction spokes
                for (i in 0 until 6) {
                    val angle = Math.toRadians((i * 60.0))
                    val px = center.x + radius * 0.75f * cos(angle).toFloat()
                    val py = center.y + radius * 0.75f * sin(angle).toFloat()
                    drawLine(
                        color = secondaryViolet.copy(alpha = 0.5f),
                        start = center,
                        end = Offset(px, py),
                        strokeWidth = 1.2f
                    )
                }
            }

            // Central specular lens node
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = radius * 0.18f,
                center = center
            )
        }
    }
}
