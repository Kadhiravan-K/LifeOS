package com.example.ui.components

import android.graphics.PointF
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.HoneycombAppearanceConfig
import com.example.domain.model.HoneycombShortcut
import com.example.domain.model.ShortcutType
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Reusable Honeycomb Task Grid component.
 * Renders interactive hexagonal cells with thick rounded borders, compact interlocking layout,
 * smooth completion animation, and Edit Mode with reordering & customization.
 */
@Composable
fun HoneycombTaskGrid(
    shortcuts: List<HoneycombShortcut>,
    appearance: HoneycombAppearanceConfig,
    isEditMode: Boolean,
    onShortcutClick: (HoneycombShortcut) -> Unit,
    onShortcutLongClick: (HoneycombShortcut) -> Unit,
    onRemoveShortcut: (Long) -> Unit,
    onMoveShortcut: (Long, Int) -> Unit, // -1 for left/up, +1 for right/down
    onAddShortcutClick: () -> Unit,
    onResetLayoutClick: () -> Unit,
    onDoneEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (shortcuts.isEmpty()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⬡ Honeycomb Dashboard Empty",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Add task and habit shortcuts to create your interactive honeycomb grid",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onResetLayoutClick,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Load Suggested Hexagons")
                }
            }
        }
        return
    }

    val density = LocalDensity.current
    val hexSizeDp = appearance.hexagonSizeDp.dp
    val spacingDp = appearance.spacingDp.dp
    val radiusDp = hexSizeDp / 2f
    val widthDp = (sqrt(3.0) * radiusDp.value).dp

    // Calculate row vertical step for interlocking pointy-topped hexagons
    val verticalStepDp = (1.5f * radiusDp.value).dp + spacingDp

    // Partition items into alternating rows (3 items in even rows, 2 items in odd rows)
    val partitionedRows = remember(shortcuts) {
        partitionShortcuts(shortcuts, baseCols = 3)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("honeycomb_task_grid"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Edit Mode Toolbar ---
        if (isEditMode) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("honeycomb_edit_toolbar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✏️ Edit Layout Mode",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = onAddShortcutClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Hexagon",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        IconButton(
                            onClick = onResetLayoutClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset Default Layout",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        FilledTonalButton(
                            onClick = onDoneEditClick,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "Done",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Done", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- Interlocking Honeycomb Rows ---
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                partitionedRows.forEachIndexed { rowIndex, rowItems ->
                    val isOddRow = rowIndex % 2 != 0
                    val topPadding = if (rowIndex == 0) 0.dp else -(radiusDp * 0.5f)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacingDp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .offset { IntOffset(0, 0) }
                            .padding(top = if (rowIndex > 0) spacingDp * 0.5f else 0.dp)
                    ) {
                        rowItems.forEach { shortcut ->
                            HoneycombCell(
                                shortcut = shortcut,
                                appearance = appearance,
                                isEditMode = isEditMode,
                                sizeDp = hexSizeDp,
                                onClick = { onShortcutClick(shortcut) },
                                onLongClick = { onShortcutLongClick(shortcut) },
                                onRemove = { onRemoveShortcut(shortcut.id) },
                                onMoveLeft = { onMoveShortcut(shortcut.id, -1) },
                                onMoveRight = { onMoveShortcut(shortcut.id, 1) },
                                modifier = Modifier.testTag("honeycomb_cell_${shortcut.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual Hexagonal Cell drawn with rounded vertices using Canvas and Path.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HoneycombCell(
    shortcut: HoneycombShortcut,
    appearance: HoneycombAppearanceConfig,
    isEditMode: Boolean,
    sizeDp: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRemove: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val activeTheme = com.example.ui.theme.engine.LocalLifeTheme.current
    val strokeWidthPx = with(density) { appearance.borderWidthDp.dp.toPx() }
    val radiusPx = with(density) { (sizeDp / 2f).toPx() }
    val cornerRadiusPx = with(density) { (appearance.borderWidthDp * 2.5f).dp.toPx() }

    // Color resolution
    val resolvedCompletionColor = Color(shortcut.customColorHex ?: appearance.defaultColorHex)
    val incompleteBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

    val borderColor = when (appearance.borderColorType) {
        "PRIMARY" -> activeTheme.colors.primary
        "MUTED" -> MaterialTheme.colorScheme.outlineVariant
        "ACCENT" -> activeTheme.colors.accent
        else -> if (activeTheme.id != com.example.ui.theme.engine.ThemeId.DEFAULT) activeTheme.colors.honeycombBorder else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    }

    val animatedFillProgress by animateFloatAsState(
        targetValue = if (shortcut.isCompletedToday) 1f else 0f,
        animationSpec = tween(durationMillis = if (appearance.isAnimationEnabled) 350 else 0),
        label = "honeycomb_fill_${shortcut.id}"
    )

    val animatedBgColor by animateColorAsState(
        targetValue = if (shortcut.isCompletedToday) resolvedCompletionColor else incompleteBgColor,
        animationSpec = tween(durationMillis = if (appearance.isAnimationEnabled) 350 else 0),
        label = "honeycomb_color_${shortcut.id}"
    )

    val widthDp = (sqrt(3.0) * (sizeDp.value / 2f)).dp

    val semanticsDescription = "${shortcut.title}, ${if (shortcut.isCompletedToday) "Completed" else "Incomplete"}. ${shortcut.subtitle}"
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(width = widthDp, height = sizeDp)
            .semantics {
                contentDescription = semanticsDescription
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Hexagonal Canvas drawing
        Canvas(modifier = Modifier.size(width = widthDp, height = sizeDp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = (size.height / 2f) - (strokeWidthPx / 2f)

            val hexPath = createRoundedHexagonPath(
                cx = cx,
                cy = cy,
                radius = r,
                cornerRadius = cornerRadiusPx
            )

            // 1. Draw Background Fill
            drawPath(
                path = hexPath,
                color = animatedBgColor,
                style = Fill
            )

            // Theme-specific decorative engravings
            when (activeTheme.honeycombEngraving) {
                com.example.ui.theme.engine.HoneycombEngravingType.RUNE -> {
                    // Mystic inner rune ring
                    drawCircle(
                        color = activeTheme.colors.secondary.copy(alpha = 0.35f),
                        radius = r * 0.72f,
                        center = androidx.compose.ui.geometry.Offset(cx, cy),
                        style = Stroke(width = 1.2f)
                    )
                }
                com.example.ui.theme.engine.HoneycombEngravingType.CIRCUIT -> {
                    // Cybernetic corner ticks
                    val tickLen = r * 0.25f
                    drawLine(
                        color = activeTheme.colors.primary.copy(alpha = 0.5f),
                        start = androidx.compose.ui.geometry.Offset(cx - tickLen, cy - r * 0.8f),
                        end = androidx.compose.ui.geometry.Offset(cx + tickLen, cy - r * 0.8f),
                        strokeWidth = 1.5f
                    )
                }
                com.example.ui.theme.engine.HoneycombEngravingType.COMPASS_BADGE -> {
                    // Adventure compass apex pip
                    drawCircle(
                        color = activeTheme.colors.primary.copy(alpha = 0.6f),
                        radius = 2.5f,
                        center = androidx.compose.ui.geometry.Offset(cx, cy - r * 0.75f)
                    )
                }
                com.example.ui.theme.engine.HoneycombEngravingType.ORBITAL -> {
                    // Celestial orbital arc
                    drawCircle(
                        color = activeTheme.colors.primary.copy(alpha = 0.25f),
                        radius = r * 0.85f,
                        center = androidx.compose.ui.geometry.Offset(cx, cy),
                        style = Stroke(width = 1f)
                    )
                }
                com.example.ui.theme.engine.HoneycombEngravingType.MINIMAL -> {}
            }

            // 2. Draw Thick Rounded Border
            drawPath(
                path = hexPath,
                color = if (isEditMode) primaryColor else borderColor,
                style = Stroke(width = strokeWidthPx)
            )
        }

        // Center Content: Emoji Icon & Title indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = shortcut.emoji,
                fontSize = appearance.iconSizeSp.sp,
                textAlign = TextAlign.Center
            )

            if (shortcut.title.isNotBlank()) {
                Text(
                    text = shortcut.title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    color = if (shortcut.isCompletedToday) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }

        // Completion indicator badge (subtle checkmark at bottom point)
        if (shortcut.isCompletedToday && !isEditMode) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-4).dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = resolvedCompletionColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // Edit Mode overlay: Remove button and Move arrows
        if (isEditMode) {
            // Remove "×" button at top right
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .combinedClickable(onClick = onRemove)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove Hexagon",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(12.dp)
                )
            }

            // Move Left / Right accessible reorder buttons at bottom
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-2).dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .combinedClickable(onClick = onMoveLeft)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Move Left",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(10.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .combinedClickable(onClick = onMoveRight)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Move Right",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}

/**
 * Creates a mathematical regular pointy-topped hexagon path with smoothly rounded vertices.
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

    val cr = cornerRadius.coerceAtMost(radius * 0.4f)

    for (i in 0 until 6) {
        val curr = vertices[i]
        val prev = vertices[(i + 5) % 6]
        val next = vertices[(i + 1) % 6]

        val vPrevX = prev.x - curr.x
        val vPrevY = prev.y - curr.y
        val lenPrev = hypot(vPrevX, vPrevY).coerceAtLeast(0.001f)
        val pStartX = curr.x + (vPrevX / lenPrev) * cr
        val pStartY = curr.y + (vPrevY / lenPrev) * cr

        val vNextX = next.x - curr.x
        val vNextY = next.y - curr.y
        val lenNext = hypot(vNextX, vNextY).coerceAtLeast(0.001f)
        val pEndX = curr.x + (vNextX / lenNext) * cr
        val pEndY = curr.y + (vNextY / lenNext) * cr

        if (i == 0) {
            path.moveTo(pStartX, pStartY)
        } else {
            path.lineTo(pStartX, pStartY)
        }
        path.quadraticTo(curr.x, curr.y, pEndX, pEndY)
    }
    path.close()
    return path
}

/**
 * Partitions shortcuts into interlocking honeycomb rows.
 * Even rows have baseCols items (default 3), odd rows have baseCols - 1 items (default 2).
 */
fun partitionShortcuts(
    shortcuts: List<HoneycombShortcut>,
    baseCols: Int = 3
): List<List<HoneycombShortcut>> {
    val rows = mutableListOf<List<HoneycombShortcut>>()
    var index = 0
    var isEvenRow = true
    while (index < shortcuts.size) {
        val rowCapacity = if (isEvenRow) baseCols else (baseCols - 1).coerceAtLeast(1)
        val takeCount = minOf(rowCapacity, shortcuts.size - index)
        rows.add(shortcuts.subList(index, index + takeCount))
        index += takeCount
        isEvenRow = !isEvenRow
    }
    return rows
}
