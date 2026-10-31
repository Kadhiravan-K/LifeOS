package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.analytics.ProductivityRadarData
import com.example.domain.analytics.RadarDimension
import kotlin.math.cos
import kotlin.math.sin

/**
 * Polished 5-axis Productivity Radar (Spider) Chart Composable (#17).
 * Displays a multi-dimensional productivity polygon with web grid concentric rings,
 * spoke lines, vertex dots, interactive breakdown cards, and overall LifeOS score.
 */
@Composable
fun ProductivityRadarChart(
    data: ProductivityRadarData,
    modifier: Modifier = Modifier,
    onDimensionClick: ((RadarDimension) -> Unit)? = null
) {
    var selectedDimension by remember { mutableStateOf<RadarDimension?>(null) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textPaintColor = MaterialTheme.colorScheme.onSurface

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("productivity_radar_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Overall Score Badge & Rating Tier
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "⚡ Productivity Radar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${data.ratingTierEmoji} ${data.ratingTier}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.testTag("radar_overall_score_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${data.overallScorePercent}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Radar Polygon & Web Rings
            val density = LocalDensity.current
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .testTag("radar_canvas_container"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.size(240.dp)
                ) {
                    val n = data.dimensions.size.coerceAtLeast(3)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxR = size.minDimension / 2f * 0.72f

                    // 1. Web Grid: Concentric Pentagon Rings (20%, 40%, 60%, 80%, 100%)
                    listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f).forEach { scale ->
                        val gridPath = Path()
                        for (i in 0 until n) {
                            val angle = (2 * Math.PI * i / n - Math.PI / 2).toFloat()
                            val x = center.x + maxR * scale * cos(angle)
                            val y = center.y + maxR * scale * sin(angle)
                            if (i == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
                        }
                        gridPath.close()
                        drawPath(
                            path = gridPath,
                            color = gridColor,
                            style = Stroke(width = if (scale == 1.0f) 1.5.dp.toPx() else 1.dp.toPx())
                        )
                    }

                    // 2. Spoke Lines from Center to Vertices
                    for (i in 0 until n) {
                        val angle = (2 * Math.PI * i / n - Math.PI / 2).toFloat()
                        val x = center.x + maxR * cos(angle)
                        val y = center.y + maxR * sin(angle)
                        drawLine(
                            color = gridColor,
                            start = center,
                            end = Offset(x, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // 3. Shaded Data Polygon
                    val dataPath = Path()
                    val vertexPoints = mutableListOf<Offset>()
                    for (i in 0 until n) {
                        val dimScore = data.dimensions[i]
                        val fraction = (dimScore.scorePercent.toFloat() / 100f).coerceIn(0.12f, 1f)
                        val angle = (2 * Math.PI * i / n - Math.PI / 2).toFloat()
                        val r = maxR * fraction
                        val x = center.x + r * cos(angle)
                        val y = center.y + r * sin(angle)
                        val pt = Offset(x, y)
                        vertexPoints.add(pt)
                        if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                    }
                    dataPath.close()

                    // Fill data polygon with primary accent translucency
                    drawPath(
                        path = dataPath,
                        color = primaryColor.copy(alpha = 0.32f),
                        style = Fill
                    )
                    // Draw outer border stroke
                    drawPath(
                        path = dataPath,
                        color = primaryColor,
                        style = Stroke(width = 2.5.dp.toPx())
                    )

                    // 4. Vertex Pins & Outer Labels
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }

                    for (i in 0 until n) {
                        val pt = vertexPoints[i]
                        val isHighlighted = selectedDimension == data.dimensions[i].dimension
                        val pinRadius = if (isHighlighted) 6.dp.toPx() else 4.5.dp.toPx()

                        // Outer ring
                        drawCircle(
                            color = if (isHighlighted) tertiaryColor else primaryColor,
                            radius = pinRadius,
                            center = pt
                        )
                        // Inner white center
                        drawCircle(
                            color = Color.White,
                            radius = pinRadius * 0.45f,
                            center = pt
                        )

                        // Draw dimension emoji label around perimeter
                        val angle = (2 * Math.PI * i / n - Math.PI / 2).toFloat()
                        val labelR = maxR + 24.dp.toPx()
                        val lx = center.x + labelR * cos(angle)
                        val ly = center.y + labelR * sin(angle) + 8f

                        drawContext.canvas.nativeCanvas.drawText(
                            data.dimensions[i].dimension.emoji,
                            lx,
                            ly,
                            textPaint
                        )
                    }
                }

                // Center circular score badge
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${data.overallScorePercent}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dimension Breakdown Chips / List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                data.dimensions.forEach { dim ->
                    val isSelected = selectedDimension == dim.dimension
                    val animProgress by animateFloatAsState(
                        targetValue = dim.scorePercent.toFloat() / 100f,
                        animationSpec = tween(500),
                        label = "progress_${dim.dimension.id}"
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        },
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedDimension = if (isSelected) null else dim.dimension
                                onDimensionClick?.invoke(dim.dimension)
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = dim.dimension.emoji, fontSize = 16.sp)
                                    Text(
                                        text = dim.dimension.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "• ${dim.rawValue}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "${dim.scorePercent}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (dim.scorePercent >= 80) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { animProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            if (isSelected) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${dim.statusSummary} (${dim.dimension.description})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
