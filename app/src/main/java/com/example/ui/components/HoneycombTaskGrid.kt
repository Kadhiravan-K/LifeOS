package com.example.ui.components

import android.graphics.PointF
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.domain.model.HoneycombAppearanceConfig
import com.example.domain.model.HoneycombLayoutTemplate
import com.example.domain.model.HoneycombShortcut
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Reusable Honeycomb Task Grid component with:
 * 1. Mathematical regular pointy-top axial hexagonal placement.
 * 2. True touch drag-and-drop: LONG-PRESS -> DRAG -> HIGHLIGHT TARGET -> DROP/SWAP.
 * 3. Responsive scaling, centered layout, zero clipping or arbitrary negative padding hacks.
 * 4. Supports templates: Standard, Pyramid, Diamond, Heart, Snake, Zigzag, Freeform.
 */
@Composable
fun HoneycombTaskGrid(
    shortcuts: List<HoneycombShortcut>,
    appearance: HoneycombAppearanceConfig,
    isEditMode: Boolean,
    onShortcutClick: (HoneycombShortcut) -> Unit,
    onShortcutLongClick: (HoneycombShortcut) -> Unit,
    onRemoveShortcut: (Long) -> Unit,
    onSwapShortcuts: (Long, Long) -> Unit,
    onMoveShortcutToCoord: (Long, Int, Int) -> Unit,
    onAddShortcutClick: () -> Unit,
    onResetLayoutClick: () -> Unit,
    onDoneEditClick: () -> Unit,
    onApplyTemplate: ((HoneycombLayoutTemplate) -> Unit)? = null,
    onBrowseTemplatesClick: (() -> Unit)? = null,
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
                    text = "⚡ QR — Quick Reminders Canvas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your daily cockpit is ready. Add quick reminder tiles or explore curated master productivity templates.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onAddShortcutClick,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+ Add Shortcut")
                    }
                    if (onBrowseTemplatesClick != null) {
                        Button(
                            onClick = onBrowseTemplatesClick,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Browse Templates")
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onResetLayoutClick,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Load Defaults")
                        }
                    }
                }
            }
        }
        return
    }

    val density = LocalDensity.current
    val hexSizeDp = appearance.hexagonSizeDp.dp
    val spacingDp = appearance.spacingDp.dp
    val radiusDp = hexSizeDp / 2f
    val hexWidthDp = (sqrt(3.0) * radiusDp.value).dp
    val hexHeightDp = hexSizeDp

    val radiusPx = with(density) { radiusDp.toPx() }
    val spacingPx = with(density) { spacingDp.toPx() }
    val hexWidthPx = (sqrt(3.0) * radiusPx).toFloat()
    val hexHeightPx = hexSizeDp.value * density.density

    // Step spacing for pointy-top hexagons with cell gap S
    val hStepPx = hexWidthPx + spacingPx
    val vStepPx = (1.5f * radiusPx) + spacingPx

    // Drag-and-drop state
    var draggedShortcutId by remember { mutableStateOf<Long?>(null) }
    var dragPixelOffset by remember { mutableStateOf(Offset.Zero) }
    var candidateDropTargetId by remember { mutableStateOf<Long?>(null) }
    var candidateDropCoord by remember { mutableStateOf<Pair<Int, Int>?>(null) }

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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "✏️ Drag & Drop to Reorder",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

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

                    // Template Chips Row
                    if (onApplyTemplate != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Preset Shapes:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HoneycombLayoutTemplate.entries.forEach { tmpl ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onApplyTemplate.invoke(tmpl) },
                                    label = { Text("${tmpl.icon} ${tmpl.displayName}", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Mathematical Axial Coordinate Grid ---
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val containerWidthPx = constraints.maxWidth.toFloat()

            // Calculate center positions in axial space:
            // x(q, r) = hStep * (q + r / 2.0)
            // y(q, r) = vStep * r
            val positions = remember(shortcuts, hStepPx, vStepPx) {
                shortcuts.associate { item ->
                    val cx = hStepPx * (item.coordQ.toFloat() + item.coordR.toFloat() / 2f)
                    val cy = vStepPx * item.coordR.toFloat()
                    item.id to Offset(cx, cy)
                }
            }

            // Determine grid bounds
            val allCenters = positions.values
            val minX = allCenters.minOfOrNull { it.x } ?: 0f
            val maxX = allCenters.maxOfOrNull { it.x } ?: 0f
            val minY = allCenters.minOfOrNull { it.y } ?: 0f
            val maxY = allCenters.maxOfOrNull { it.y } ?: 0f

            val gridContentWidthPx = (maxX - minX) + hexWidthPx
            val gridContentHeightPx = (maxY - minY) + hexHeightPx
            val paddingPx = with(density) { 16.dp.toPx() }

            // Center grid horizontally in available container width
            val originOffsetX = ((containerWidthPx - gridContentWidthPx) / 2f) - minX + (hexWidthPx / 2f)
            val originOffsetY = paddingPx - minY + (hexHeightPx / 2f)

            val totalContainerHeightDp = with(density) {
                (gridContentHeightPx + (paddingPx * 2f)).toDp()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalContainerHeightDp)
            ) {
                shortcuts.forEach { shortcut ->
                    val isBeingDragged = draggedShortcutId == shortcut.id
                    val isCandidateTarget = candidateDropTargetId == shortcut.id
                    val baseCenter = positions[shortcut.id] ?: Offset.Zero

                    val visualCenter = if (isBeingDragged) {
                        Offset(
                            x = baseCenter.x + originOffsetX + dragPixelOffset.x,
                            y = baseCenter.y + originOffsetY + dragPixelOffset.y
                        )
                    } else {
                        Offset(
                            x = baseCenter.x + originOffsetX,
                            y = baseCenter.y + originOffsetY
                        )
                    }

                    val leftPx = visualCenter.x - (hexWidthPx / 2f)
                    val topPx = visualCenter.y - (hexHeightPx / 2f)

                    val itemLeftDp = with(density) { leftPx.toDp() }
                    val itemTopDp = with(density) { topPx.toDp() }

                    Box(
                        modifier = Modifier
                            .offset(x = itemLeftDp, y = itemTopDp)
                            .zIndex(if (isBeingDragged) 100f else if (isCandidateTarget) 50f else 1f)
                            .then(
                                if (isEditMode) {
                                    Modifier.pointerInput(shortcut.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggedShortcutId = shortcut.id
                                                dragPixelOffset = Offset.Zero
                                                candidateDropTargetId = null
                                                candidateDropCoord = null
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragPixelOffset += dragAmount

                                                // Current dragged center in grid coordinate space
                                                val currCenterPx = baseCenter + dragPixelOffset
                                                val targetAxial = pixelToAxial(
                                                    px = currCenterPx.x,
                                                    py = currCenterPx.y,
                                                    hStep = hStepPx,
                                                    vStep = vStepPx
                                                )
                                                candidateDropCoord = targetAxial

                                                // Find if another shortcut occupies candidate axial position
                                                val target = shortcuts.find {
                                                    it.coordQ == targetAxial.first &&
                                                    it.coordR == targetAxial.second &&
                                                    it.id != shortcut.id
                                                }
                                                candidateDropTargetId = target?.id
                                            },
                                            onDragEnd = {
                                                val targetId = candidateDropTargetId
                                                val targetCoord = candidateDropCoord
                                                if (targetId != null) {
                                                    onSwapShortcuts(shortcut.id, targetId)
                                                } else if (targetCoord != null) {
                                                    onMoveShortcutToCoord(shortcut.id, targetCoord.first, targetCoord.second)
                                                }
                                                draggedShortcutId = null
                                                dragPixelOffset = Offset.Zero
                                                candidateDropTargetId = null
                                                candidateDropCoord = null
                                            },
                                            onDragCancel = {
                                                draggedShortcutId = null
                                                dragPixelOffset = Offset.Zero
                                                candidateDropTargetId = null
                                                candidateDropCoord = null
                                            }
                                        )
                                    }
                                } else Modifier
                            )
                    ) {
                        HoneycombCell(
                            shortcut = shortcut,
                            appearance = appearance,
                            isEditMode = isEditMode,
                            sizeDp = hexSizeDp,
                            isDragged = isBeingDragged,
                            isHighlightedTarget = isCandidateTarget,
                            onClick = {
                                if (!isEditMode) onShortcutClick(shortcut)
                            },
                            onLongClick = {
                                if (!isEditMode) onShortcutLongClick(shortcut)
                            },
                            onRemove = { onRemoveShortcut(shortcut.id) },
                            modifier = Modifier.testTag("honeycomb_cell_${shortcut.id}")
                        )
                    }
                }
            }
        }
    }
}

/**
 * Converts a 2D Cartesian pixel offset (relative to grid origin) to axial coordinates (q, r)
 * using standard hexagonal cube-coordinate rounding.
 */
fun pixelToAxial(px: Float, py: Float, hStep: Float, vStep: Float): Pair<Int, Int> {
    if (hStep <= 0f || vStep <= 0f) return Pair(0, 0)
    val rFloat = py / vStep
    val qFloat = (px / hStep) - (rFloat / 2f)

    // Cube coordinate representation: x = q, z = r, y = -x - z
    val xCube = qFloat
    val zCube = rFloat
    val yCube = -xCube - zCube

    var rx = xCube.roundToInt()
    var ry = yCube.roundToInt()
    var rz = zCube.roundToInt()

    val xDiff = abs(rx - xCube)
    val yDiff = abs(ry - yCube)
    val zDiff = abs(rz - zCube)

    if (xDiff > yDiff && xDiff > zDiff) {
        rx = -ry - rz
    } else if (yDiff > zDiff) {
        ry = -rx - rz
    } else {
        rz = -rx - ry
    }

    return Pair(rx, rz)
}

/**
 * Individual Hexagonal Cell drawn with rounded vertices using Canvas and Path.
 * Adapts to active themes, highlights on drag/candidate drop target, and displays completion badges.
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
    modifier: Modifier = Modifier,
    isDragged: Boolean = false,
    isHighlightedTarget: Boolean = false
) {
    val density = LocalDensity.current
    val activeTheme = com.example.ui.theme.engine.LocalLifeTheme.current
    val strokeWidthPx = with(density) { appearance.borderWidthDp.dp.toPx() }
    val cornerRadiusPx = with(density) { (appearance.borderWidthDp * 2.5f).dp.toPx() }

    // Color resolution precedence: User Custom > Project > Theme Default > Global Setting
    val themePrimaryHex = activeTheme.colors.primary.toArgb().toLong() and 0xFFFFFFFFL
    val rawColorHex = when (shortcut.colorSource) {
        "THEME_DEFAULT" -> themePrimaryHex
        "CUSTOM" -> shortcut.customColorHex ?: appearance.defaultColorHex
        "PROJECT" -> shortcut.projectColorHex ?: shortcut.customColorHex ?: shortcut.effectiveColorHex ?: themePrimaryHex
        else -> shortcut.effectiveColorHex ?: appearance.defaultColorHex
    }
    val safeColorHex = if (rawColorHex == 0L || (rawColorHex and 0xFF000000L) == 0L) {
        if (appearance.defaultColorHex != 0L) appearance.defaultColorHex else 0xFF10B981L
    } else {
        rawColorHex
    }
    val resolvedCompletionColor = Color(safeColorHex)
    val incompleteBgColor = if (activeTheme.id != com.example.ui.theme.engine.ThemeId.DEFAULT) {
        activeTheme.colors.surfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    val borderColor = when {
        isHighlightedTarget -> activeTheme.colors.accent
        isDragged -> activeTheme.colors.primary
        appearance.borderColorType == "PRIMARY" -> activeTheme.colors.primary
        appearance.borderColorType == "MUTED" -> MaterialTheme.colorScheme.outlineVariant
        appearance.borderColorType == "ACCENT" -> activeTheme.colors.accent
        else -> if (activeTheme.id != com.example.ui.theme.engine.ThemeId.DEFAULT) activeTheme.colors.honeycombBorder else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    }

    val animatedBgColor by animateColorAsState(
        targetValue = if (shortcut.isCompletedToday) resolvedCompletionColor else incompleteBgColor,
        animationSpec = tween(durationMillis = if (appearance.isAnimationEnabled) 350 else 0),
        label = "honeycomb_color_${shortcut.id}"
    )

    val scaleFactor by animateFloatAsState(
        targetValue = when {
            isDragged -> 1.15f
            isHighlightedTarget -> 1.08f
            else -> 1.0f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "hex_scale_${shortcut.id}"
    )

    val widthDp = (sqrt(3.0) * (sizeDp.value / 2f)).dp
    val semanticsDescription = "${shortcut.displayTitle}, ${if (shortcut.isCompletedToday) "Completed" else "Incomplete"}. ${shortcut.subtitle}"

    Box(
        modifier = modifier
            .size(width = widthDp, height = sizeDp)
            .scale(scaleFactor)
            .then(
                if (isDragged) Modifier.shadow(12.dp, CircleShape) else Modifier
            )
            .semantics {
                contentDescription = semanticsDescription
            }
            .then(
                if (!isEditMode) {
                    Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Shape Canvas drawing: HEXAGON, CIRCLE, or SQUARE
        Canvas(modifier = Modifier.size(width = widthDp, height = sizeDp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = (size.height / 2f) - (strokeWidthPx / 2f)

            val effectiveShape = if (shortcut.shape.isNotBlank()) shortcut.shape else appearance.shape.name
            val isCircle = effectiveShape.equals("CIRCLE", ignoreCase = true)
            val isSquare = effectiveShape.equals("SQUARE", ignoreCase = true)

            val hexPath = if (!isCircle && !isSquare) {
                createRoundedHexagonPath(
                    cx = cx,
                    cy = cy,
                    radius = r,
                    cornerRadius = cornerRadiusPx
                )
            } else null

            // 1. Draw Background Fill
            when {
                isCircle -> {
                    drawCircle(
                        color = animatedBgColor,
                        radius = r,
                        center = Offset(cx, cy),
                        style = Fill
                    )
                }
                isSquare -> {
                    val halfW = r * 0.85f
                    drawRoundRect(
                        color = animatedBgColor,
                        topLeft = Offset(cx - halfW, cy - halfW),
                        size = Size(halfW * 2f, halfW * 2f),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        style = Fill
                    )
                }
                else -> {
                    drawPath(
                        path = hexPath!!,
                        color = animatedBgColor,
                        style = Fill
                    )
                }
            }

            // Theme-specific decorative engravings
            when (activeTheme.honeycombEngraving) {
                com.example.ui.theme.engine.HoneycombEngravingType.RUNE -> {
                    drawCircle(
                        color = activeTheme.colors.secondary.copy(alpha = 0.35f),
                        radius = r * 0.72f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.2f)
                    )
                }
                com.example.ui.theme.engine.HoneycombEngravingType.CIRCUIT -> {
                    val tickLen = r * 0.25f
                    drawLine(
                        color = activeTheme.colors.primary.copy(alpha = 0.5f),
                        start = Offset(cx - tickLen, cy - r * 0.8f),
                        end = Offset(cx + tickLen, cy - r * 0.8f),
                        strokeWidth = 1.5f
                    )
                }
                com.example.ui.theme.engine.HoneycombEngravingType.COMPASS_BADGE -> {
                    drawCircle(
                        color = activeTheme.colors.primary.copy(alpha = 0.6f),
                        radius = 2.5f,
                        center = Offset(cx, cy - r * 0.75f)
                    )
                }
                com.example.ui.theme.engine.HoneycombEngravingType.ORBITAL -> {
                    drawCircle(
                        color = activeTheme.colors.primary.copy(alpha = 0.25f),
                        radius = r * 0.85f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1f)
                    )
                }
                com.example.ui.theme.engine.HoneycombEngravingType.PRISM -> {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.3f),
                        radius = r * 0.78f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1f)
                    )
                }
                com.example.ui.theme.engine.HoneycombEngravingType.MINIMAL -> {}
            }

            // 2. Draw Thick Rounded Border
            val borderDrawColor = if (isHighlightedTarget) activeTheme.colors.accent else if (isEditMode) activeTheme.colors.primary else borderColor
            val borderStrokeWidth = if (isHighlightedTarget || isDragged) strokeWidthPx * 1.5f else strokeWidthPx

            when {
                isCircle -> {
                    drawCircle(
                        color = borderDrawColor,
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(width = borderStrokeWidth)
                    )
                }
                isSquare -> {
                    val halfW = r * 0.85f
                    drawRoundRect(
                        color = borderDrawColor,
                        topLeft = Offset(cx - halfW, cy - halfW),
                        size = Size(halfW * 2f, halfW * 2f),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        style = Stroke(width = borderStrokeWidth)
                    )
                }
                else -> {
                    drawPath(
                        path = hexPath!!,
                        color = borderDrawColor,
                        style = Stroke(width = borderStrokeWidth)
                    )
                }
            }
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

            val tileTitle = shortcut.displayTitle
            if (tileTitle.isNotBlank()) {
                Text(
                    text = tileTitle,
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

        // Edit Mode overlay: Remove button at top right
        if (isEditMode && !isDragged) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .combinedClickable(onClick = onRemove)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove Hexagon",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(14.dp)
                )
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
 * Helper to partition shortcuts into interlocking rows for linear display fallbacks.
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
