package com.example.ui.screens.security

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/**
 * Interactive 3x3 Pattern Lock View supporting fluid drag gestures,
 * intermediate dot auto-bridging, tactile connecting lines, and error states.
 */
@Composable
fun InteractivePatternLockView(
    onPatternCompleted: (patternDots: List<Int>) -> Unit,
    onPatternTooShort: () -> Unit = {},
    onPatternChanged: (patternDots: List<Int>) -> Unit = {},
    isError: Boolean = false,
    enabled: Boolean = true,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    errorColor: Color = MaterialTheme.colorScheme.error,
    dotColor: Color = MaterialTheme.colorScheme.outline,
    modifier: Modifier = Modifier
) {
    var selectedDots by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentTouchPosition by remember { mutableStateOf<Offset?>(null) }

    val currentOnCompleted by rememberUpdatedState(onPatternCompleted)
    val currentOnTooShort by rememberUpdatedState(onPatternTooShort)
    val currentOnChanged by rememberUpdatedState(onPatternChanged)

    // Clear internal selection when error resets or explicitly instructed
    if (isError && selectedDots.isEmpty()) {
        // preserve external reset
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(16.dp)
            .testTag("interactive_pattern_lock_view")
            .pointerInput(enabled, isError) {
                if (!enabled) return@pointerInput

                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Main, requireUnconsumed = false)
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    val hitRadius = (width / 3f) * 0.42f

                    val firstDot = findDotAt(down.position, width, height, hitRadius)
                    if (firstDot != null) {
                        selectedDots = listOf(firstDot)
                        currentTouchPosition = down.position
                        currentOnChanged(selectedDots)
                        down.consume()
                    }

                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: break

                        if (change.pressed) {
                            currentTouchPosition = change.position
                            val hitDot = findDotAt(change.position, width, height, hitRadius)
                            if (hitDot != null && !selectedDots.contains(hitDot)) {
                                val lastDot = selectedDots.lastOrNull()
                                val intermediate = if (lastDot != null) getIntermediateDot(lastDot, hitDot) else null
                                val updatedList = selectedDots.toMutableList()
                                if (intermediate != null && !updatedList.contains(intermediate)) {
                                    updatedList.add(intermediate)
                                }
                                updatedList.add(hitDot)
                                selectedDots = updatedList
                                currentOnChanged(selectedDots)
                            }
                            change.consume()
                        } else {
                            // Touch released / gesture finished
                            currentTouchPosition = null
                            val finalPattern = selectedDots
                            if (finalPattern.size >= 4) {
                                currentOnCompleted(finalPattern)
                            } else if (finalPattern.isNotEmpty()) {
                                currentOnTooShort()
                            }
                            selectedDots = emptyList()
                            change.consume()
                            break
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val cellWidth = width / 3f
            val cellHeight = height / 3f

            fun getDotCenter(index: Int): Offset {
                val row = index / 3
                val col = index % 3
                return Offset(
                    x = col * cellWidth + cellWidth / 2f,
                    y = row * cellHeight + cellHeight / 2f
                )
            }

            val strokeColor = if (isError) errorColor else activeColor
            val lineStrokeWidth = 7.dp.toPx()

            // 1. Draw connecting lines between consecutive selected dots
            if (selectedDots.size > 1) {
                for (i in 0 until selectedDots.size - 1) {
                    val p1 = getDotCenter(selectedDots[i])
                    val p2 = getDotCenter(selectedDots[i + 1])
                    drawLine(
                        color = strokeColor,
                        start = p1,
                        end = p2,
                        strokeWidth = lineStrokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 2. Draw active connecting line to current finger position
            val currentPos = currentTouchPosition
            if (currentPos != null && selectedDots.isNotEmpty()) {
                val lastCenter = getDotCenter(selectedDots.last())
                drawLine(
                    color = strokeColor.copy(alpha = 0.8f),
                    start = lastCenter,
                    end = currentPos,
                    strokeWidth = lineStrokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // 3. Draw 9 dots (outer ring & inner core)
            val outerRadius = cellWidth * 0.28f
            val innerRadiusNormal = 6.dp.toPx()
            val innerRadiusSelected = 9.dp.toPx()

            for (i in 0 until 9) {
                val center = getDotCenter(i)
                val isSelected = selectedDots.contains(i)

                if (isSelected) {
                    // Outer halo
                    drawCircle(
                        color = strokeColor.copy(alpha = 0.22f),
                        radius = outerRadius,
                        center = center
                    )
                    // Outer ring stroke
                    drawCircle(
                        color = strokeColor,
                        radius = outerRadius,
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    // Solid inner active core
                    drawCircle(
                        color = strokeColor,
                        radius = innerRadiusSelected,
                        center = center
                    )
                } else {
                    // Unselected subtle guide ring
                    drawCircle(
                        color = dotColor.copy(alpha = 0.35f),
                        radius = outerRadius * 0.85f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // Unselected inner point
                    drawCircle(
                        color = dotColor.copy(alpha = 0.75f),
                        radius = innerRadiusNormal,
                        center = center
                    )
                }
            }
        }
    }
}

/**
 * Finds dot index under the touch position within hitRadius.
 */
private fun findDotAt(pos: Offset, width: Float, height: Float, hitRadius: Float): Int? {
    val cellWidth = width / 3f
    val cellHeight = height / 3f

    for (row in 0 until 3) {
        for (col in 0 until 3) {
            val cx = col * cellWidth + cellWidth / 2f
            val cy = row * cellHeight + cellHeight / 2f
            val dist = hypot(pos.x - cx, pos.y - cy)
            if (dist <= hitRadius) {
                return row * 3 + col
            }
        }
    }
    return null
}

/**
 * Returns the intermediate dot if connecting a straight line between two collinear dots.
 */
private fun getIntermediateDot(dot1: Int, dot2: Int): Int? {
    val r1 = dot1 / 3
    val c1 = dot1 % 3
    val r2 = dot2 / 3
    val c2 = dot2 % 3

    // Check if on same row with 1 skipped dot
    if (r1 == r2 && kotlin.math.abs(c1 - c2) == 2) {
        return r1 * 3 + 1
    }
    // Check if on same column with 1 skipped dot
    if (c1 == c2 && kotlin.math.abs(r1 - r2) == 2) {
        return 1 * 3 + c1
    }
    // Check main diagonal (0 <-> 8)
    if ((dot1 == 0 && dot2 == 8) || (dot1 == 8 && dot2 == 0)) {
        return 4
    }
    // Check anti-diagonal (2 <-> 6)
    if ((dot1 == 2 && dot2 == 6) || (dot1 == 6 && dot2 == 2)) {
        return 4
    }
    return null
}
