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

    /**
     * Draws an immersive Arcane Constellation & Runic pattern background.
     */
    fun drawArcaneBackgroundPattern(
        scope: DrawScope,
        width: Float,
        height: Float,
        primaryColor: Color,
        goldColor: Color
    ) {
        scope.apply {
            // Concentric astral circles in top right and bottom left
            val centerTopRight = Offset(width * 0.88f, height * 0.12f)
            drawCircle(color = goldColor.copy(alpha = 0.18f), radius = width * 0.45f, center = centerTopRight, style = Stroke(width = 1.2f))
            drawCircle(color = primaryColor.copy(alpha = 0.12f), radius = width * 0.30f, center = centerTopRight, style = Stroke(width = 1f))
            drawCircle(color = goldColor.copy(alpha = 0.08f), radius = width * 0.15f, center = centerTopRight, style = Stroke(width = 0.8f))

            val centerBottomLeft = Offset(width * 0.12f, height * 0.85f)
            drawCircle(color = primaryColor.copy(alpha = 0.15f), radius = width * 0.40f, center = centerBottomLeft, style = Stroke(width = 1.2f))
            drawCircle(color = goldColor.copy(alpha = 0.10f), radius = width * 0.22f, center = centerBottomLeft, style = Stroke(width = 0.8f))

            // Mystic constellation stars and lines
            val points = listOf(
                Offset(width * 0.25f, height * 0.18f),
                Offset(width * 0.40f, height * 0.12f),
                Offset(width * 0.60f, height * 0.20f),
                Offset(width * 0.75f, height * 0.35f),
                Offset(width * 0.35f, height * 0.45f),
                Offset(width * 0.50f, height * 0.60f),
                Offset(width * 0.70f, height * 0.75f)
            )
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = goldColor.copy(alpha = 0.15f),
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 1f
                )
            }
            points.forEach { pt ->
                drawCircle(color = goldColor.copy(alpha = 0.5f), radius = 3f, center = pt)
                drawCircle(color = primaryColor.copy(alpha = 0.3f), radius = 6f, center = pt, style = Stroke(1f))
            }
        }
    }

    /**
     * Draws an immersive Cyber Matrix & Circuit trace background.
     */
    fun drawCyberMatrixBackground(
        scope: DrawScope,
        width: Float,
        height: Float,
        cyanColor: Color,
        magentaColor: Color
    ) {
        scope.apply {
            // Isometric circuit traces
            val stepY = height / 10f
            for (i in 1..9) {
                val y = i * stepY
                val alpha = if (i % 3 == 0) 0.14f else 0.06f
                drawLine(
                    color = cyanColor.copy(alpha = alpha),
                    start = Offset(0f, y),
                    end = Offset(width * 0.35f, y),
                    strokeWidth = 1.2f
                )
                drawLine(
                    color = cyanColor.copy(alpha = alpha),
                    start = Offset(width * 0.35f, y),
                    end = Offset(width * 0.50f, y + 24f),
                    strokeWidth = 1.2f
                )
                drawLine(
                    color = cyanColor.copy(alpha = alpha),
                    start = Offset(width * 0.50f, y + 24f),
                    end = Offset(width, y + 24f),
                    strokeWidth = 1.2f
                )
                drawCircle(color = cyanColor.copy(alpha = alpha * 2f), radius = 3f, center = Offset(width * 0.35f, y))
                drawCircle(color = magentaColor.copy(alpha = alpha * 2f), radius = 2.5f, center = Offset(width * 0.50f, y + 24f))
            }

            // High-tech corner telemetry brackets
            val bracketLen = 32f
            drawLine(cyanColor.copy(alpha = 0.4f), Offset(16f, 16f), Offset(16f + bracketLen, 16f), 2f)
            drawLine(cyanColor.copy(alpha = 0.4f), Offset(16f, 16f), Offset(16f, 16f + bracketLen), 2f)
            drawLine(cyanColor.copy(alpha = 0.4f), Offset(width - 16f, 16f), Offset(width - 16f - bracketLen, 16f), 2f)
            drawLine(cyanColor.copy(alpha = 0.4f), Offset(width - 16f, 16f), Offset(width - 16f, 16f + bracketLen), 2f)
        }
    }

    /**
     * Draws an immersive Adventure Guild Map & Compass background.
     */
    fun drawAdventureMapBackground(
        scope: DrawScope,
        width: Float,
        height: Float,
        woodGold: Color,
        emeraldGreen: Color
    ) {
        scope.apply {
            // Cartographic longitude/latitude arcs
            val centerX = width * 0.5f
            val centerY = height * 0.4f
            drawCircle(woodGold.copy(alpha = 0.12f), radius = width * 0.48f, center = Offset(centerX, centerY), style = Stroke(1.5f))
            drawCircle(woodGold.copy(alpha = 0.08f), radius = width * 0.32f, center = Offset(centerX, centerY), style = Stroke(1f))
            drawCircle(emeraldGreen.copy(alpha = 0.06f), radius = width * 0.65f, center = Offset(centerX, centerY), style = Stroke(0.8f))

            // Guild compass rays
            val rayCount = 8
            for (i in 0 until rayCount) {
                val angle = Math.toRadians((i * (360.0 / rayCount)))
                val ex = centerX + width * 0.48f * cos(angle).toFloat()
                val ey = centerY + width * 0.48f * sin(angle).toFloat()
                drawLine(woodGold.copy(alpha = 0.10f), Offset(centerX, centerY), Offset(ex, ey), 1f)
            }

            // Top and bottom leather stitch lines
            val dashStep = 12f
            var sx = 10f
            while (sx < width - 10f) {
                drawLine(woodGold.copy(alpha = 0.20f), Offset(sx, 8f), Offset(sx + 6f, 8f), 1.5f)
                drawLine(woodGold.copy(alpha = 0.20f), Offset(sx, height - 8f), Offset(sx + 6f, height - 8f), 1.5f)
                sx += dashStep
            }
        }
    }

    /**
     * Draws an immersive Cosmic Explorer Deep Nebula & Orbit background.
     */
    fun drawCosmicDeepSpaceBackground(
        scope: DrawScope,
        width: Float,
        height: Float,
        starColor: Color,
        nebulaColor: Color
    ) {
        scope.apply {
            // Background starfield
            drawStarfield(scope = this, width = width, height = height, starColor = starColor)

            // Deep space elliptical planetary trajectories
            val cx = width * 0.5f
            val cy = height * 0.45f
            drawCircle(starColor.copy(alpha = 0.16f), radius = width * 0.44f, center = Offset(cx, cy), style = Stroke(1.2f))
            drawCircle(nebulaColor.copy(alpha = 0.12f), radius = width * 0.62f, center = Offset(cx, cy), style = Stroke(1f))
            drawCircle(starColor.copy(alpha = 0.08f), radius = width * 0.22f, center = Offset(cx, cy), style = Stroke(0.8f))

            // Planetary beacon nodes
            val angleA = Math.toRadians(45.0)
            val pAx = cx + width * 0.44f * cos(angleA).toFloat()
            val pAy = cy + width * 0.44f * sin(angleA).toFloat()
            drawCircle(starColor.copy(alpha = 0.7f), radius = 4f, center = Offset(pAx, pAy))

            val angleB = Math.toRadians(210.0)
            val pBx = cx + width * 0.62f * cos(angleB).toFloat()
            val pBy = cy + width * 0.62f * sin(angleB).toFloat()
            drawCircle(nebulaColor.copy(alpha = 0.6f), radius = 3.5f, center = Offset(pBx, pBy))
        }
    }

    /**
     * Draws an immersive Minimal Executive Dot-Matrix background.
     */
    fun drawMinimalExecutiveBackground(
        scope: DrawScope,
        width: Float,
        height: Float,
        primaryColor: Color,
        outlineColor: Color
    ) {
        scope.apply {
            val step = 32f
            var x = 16f
            while (x < width) {
                var y = 16f
                while (y < height) {
                    val isAccent = ((x.toInt() * 17) + (y.toInt() * 31)) % 13 == 0
                    val dotColor = if (isAccent) primaryColor.copy(alpha = 0.15f) else outlineColor.copy(alpha = 0.07f)
                    drawCircle(color = dotColor, radius = if (isAccent) 1.5f else 1f, center = Offset(x, y))
                    y += step
                }
                x += step
            }
        }
    }

    /**
     * Draws an immersive Glass Prismatic Specular Mesh background.
     */
    fun drawGlassPrismMesh(
        scope: DrawScope,
        width: Float,
        height: Float,
        primaryCyan: Color,
        secondaryViolet: Color
    ) {
        scope.apply {
            // Diagonal specular refraction beams
            val step = 64f
            var d = 0f
            while (d < width + height) {
                drawLine(
                    color = primaryCyan.copy(alpha = 0.06f),
                    start = Offset(d, 0f),
                    end = Offset(d - height, height),
                    strokeWidth = 1f
                )
                d += step
            }

            // Prismatic refractive diamond nodes
            val centerX = width * 0.5f
            val centerY = height * 0.35f
            val rad = width * 0.35f

            val diamondPath = Path().apply {
                moveTo(centerX, centerY - rad)
                lineTo(centerX + rad, centerY)
                lineTo(centerX, centerY + rad)
                lineTo(centerX - rad, centerY)
                close()
            }
            drawPath(diamondPath, color = primaryCyan.copy(alpha = 0.10f), style = Stroke(1.5f))

            val innerDiamond = Path().apply {
                moveTo(centerX, centerY - rad * 0.55f)
                lineTo(centerX + rad * 0.55f, centerY)
                lineTo(centerX, centerY + rad * 0.55f)
                lineTo(centerX - rad * 0.55f, centerY)
                close()
            }
            drawPath(innerDiamond, color = secondaryViolet.copy(alpha = 0.08f), style = Stroke(1f))
        }
    }
}
