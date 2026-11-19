package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardBackspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.bluetooth.HidConstants
import com.example.model.QuickToolId
import com.example.model.QuickToolPlacement
import com.example.model.SparseGridEngine
import com.example.ui.theme.TabRemoteTheme
import kotlin.math.roundToInt

/**
 * One UI 9 style 6-column coordinate-preserving 2D Quick Tools grid:
 * - Preserves intentional empty gaps between buttons.
 * - Entered ONLY via the "Düzenle" button next to "HIZLI ARAÇLAR" (no long-press hijack).
 * - Smooth zero-lag drag-and-drop with ghost target footprint & nearest-open-cell push on drop.
 * - Per-tile resize toggle (1x1 icon-only <-> 2x1 icon + title + shortcutHint; SPACE 2x1 <-> 3x1).
 */
@Composable
fun QuickToolsGridSection(
    placements: List<QuickToolPlacement>,
    liveTypingMode: Boolean,
    isTurkishLayout: Boolean,
    isEditMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    onPlaceQuickToolAt: (tool: QuickToolId, targetCol: Int, targetRow: Int) -> Unit,
    onResizeQuickTool: (QuickToolId) -> Unit,
    onRemoveToPool: (QuickToolId) -> Unit,
    onTriggerTool: (QuickToolId) -> Unit,
    onSendKey: (hidKeyCode: Int, modifier: Int) -> Unit,
    modifier: Modifier = Modifier,
    headerEndPadding: Dp = 0.dp
) {
    val palette = TabRemoteTheme.palette
    var draggedPlacement by remember { mutableStateOf<QuickToolPlacement?>(null) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    var ghostCol by remember { mutableIntStateOf(0) }
    var ghostRow by remember { mutableIntStateOf(0) }

    val baseRows = remember(placements) {
        (placements.maxOfOrNull { it.row + it.rowSpan } ?: 1).coerceAtLeast(1)
    }
    val totalRows = if (isEditMode) {
        maxOf(baseRows + 1, ghostRow + (draggedPlacement?.rowSpan ?: 1))
    } else {
        baseRows
    }

    val latestOnPlaceAt by rememberUpdatedState(onPlaceQuickToolAt)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 2.dp, end = headerEndPadding + 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isEditMode) "SÜRÜKLE • ÇİFT TIKLA BOYUTLANDIR" else "KLAVYE KISAYOLLARI",
                    color = if (isEditMode) palette.primary else palette.textMuted,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp
                )
                if (!isEditMode) {
                    Text(
                        text = if (isTurkishLayout) "• TR-Q" else "• US-EN",
                        color = palette.textMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (isEditMode) {
                Surface(
                    color = palette.primary,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onEditModeChange(false)
                            draggedPlacement = null
                            dragDelta = Offset.Zero
                        }
                        .testTag("quick_tools_done_edit_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Düzenlemeyi Bitir",
                            tint = palette.onAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Bitti",
                            color = palette.onAccent,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            } else {
                Surface(
                    color = palette.surfaceContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, palette.borderElevated, RoundedCornerShape(10.dp))
                        .clickable { onEditModeChange(true) }
                        .testTag("quick_tools_edit_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Hızlı Araçları Düzenle",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Düzenle",
                            color = palette.textPrimary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick_tools_grid")
        ) {
            val density = LocalDensity.current
            val gapDp = 5.dp
            val cellHeightDp = 42.dp
            val gapPx = with(density) { gapDp.toPx() }
            val cellHeightPx = with(density) { cellHeightDp.toPx() }
            val totalWidthPx = with(density) { maxWidth.toPx() }
            val cellWidthPx = ((totalWidthPx - gapPx * (SparseGridEngine.GRID_COLUMNS - 1)) /
                SparseGridEngine.GRID_COLUMNS).coerceAtLeast(10f)
            val totalRowsEffective = totalRows.coerceAtLeast(1)
            val gridHeightDp = with(density) {
                (totalRowsEffective * cellHeightPx + (totalRowsEffective - 1) * gapPx).toDp()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeightDp)
            ) {
                // Sparse Grid Background Slots & Target Drop Footprint Highlight in Edit Mode
                if (isEditMode) {
                    val dashedStroke = remember {
                        Stroke(
                            width = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                        )
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (r in 0 until totalRowsEffective) {
                            for (c in 0 until SparseGridEngine.GRID_COLUMNS) {
                                val x = c * (cellWidthPx + gapPx)
                                val y = r * (cellHeightPx + gapPx)
                                drawRoundRect(
                                    color = palette.borderSubtle.copy(alpha = 0.55f),
                                    topLeft = Offset(x, y),
                                    size = Size(cellWidthPx, cellHeightPx),
                                    cornerRadius = CornerRadius(14f, 14f),
                                    style = Stroke(width = 1.5f)
                                )
                            }
                        }

                        val activeDrag = draggedPlacement
                        if (activeDrag != null) {
                            val gx = ghostCol * (cellWidthPx + gapPx)
                            val gy = ghostRow * (cellHeightPx + gapPx)
                            val gw = activeDrag.colSpan * cellWidthPx + (activeDrag.colSpan - 1) * gapPx
                            val gh = activeDrag.rowSpan * cellHeightPx + (activeDrag.rowSpan - 1) * gapPx
                            drawRoundRect(
                                color = palette.primary.copy(alpha = 0.16f),
                                topLeft = Offset(gx, gy),
                                size = Size(gw, gh),
                                cornerRadius = CornerRadius(18f, 18f)
                            )
                            drawRoundRect(
                                color = palette.primary,
                                topLeft = Offset(gx, gy),
                                size = Size(gw, gh),
                                cornerRadius = CornerRadius(18f, 18f),
                                style = dashedStroke
                            )
                        }
                    }
                }

                placements.forEach { placed ->
                    val tool = placed.tool
                    val isBeingDragged = draggedPlacement?.tool == tool
                    val baseX = placed.col * (cellWidthPx + gapPx)
                    val baseY = placed.row * (cellHeightPx + gapPx)
                    val wPx = placed.colSpan * cellWidthPx + (placed.colSpan - 1) * gapPx
                    val hPx = placed.rowSpan * cellHeightPx + (placed.rowSpan - 1) * gapPx

                    val animX by animateDpAsState(
                        targetValue = with(density) { baseX.toDp() },
                        animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMediumLow),
                        label = "qt_x_${tool.name}"
                    )
                    val animY by animateDpAsState(
                        targetValue = with(density) { baseY.toDp() },
                        animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMediumLow),
                        label = "qt_y_${tool.name}"
                    )
                    val wDp = with(density) { wPx.toDp() }
                    val hDp = with(density) { hPx.toDp() }

                    val tileScale by animateFloatAsState(
                        targetValue = if (isBeingDragged) 1.05f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "qt_scale_${tool.name}"
                    )

                    val offsetModifier = if (isBeingDragged) {
                        Modifier.offset {
                            IntOffset(
                                (baseX + dragDelta.x).roundToInt(),
                                (baseY + dragDelta.y).roundToInt()
                            )
                        }
                    } else {
                        Modifier.offset(x = animX, y = animY)
                    }

                    val stepX = (cellWidthPx + gapPx).coerceAtLeast(1f)
                    val stepY = (cellHeightPx + gapPx).coerceAtLeast(1f)
                    val maxCol = (SparseGridEngine.GRID_COLUMNS - placed.colSpan).coerceAtLeast(0)
                    val maxRow = (baseRows + 1).coerceAtLeast(1)

                    val tileInteractionModifier = if (isEditMode) {
                        Modifier.pointerInput(placed, maxCol, maxRow, stepX, stepY) {
                            detectDragGestures(
                                onDragStart = {
                                    draggedPlacement = placed
                                    dragDelta = Offset.Zero
                                    ghostCol = placed.col
                                    ghostRow = placed.row
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragDelta += dragAmount
                                    val currLeft = baseX + dragDelta.x
                                    val currTop = baseY + dragDelta.y
                                    ghostCol = (currLeft / stepX).roundToInt().coerceIn(0, maxCol)
                                    ghostRow = (currTop / stepY).roundToInt().coerceIn(0, maxRow)
                                },
                                onDragEnd = {
                                    val targetCol = ghostCol
                                    val targetRow = ghostRow
                                    draggedPlacement = null
                                    dragDelta = Offset.Zero
                                    latestOnPlaceAt(tool, targetCol, targetRow)
                                },
                                onDragCancel = {
                                    draggedPlacement = null
                                    dragDelta = Offset.Zero
                                }
                            )
                        }
                    } else {
                        Modifier
                    }

                    val canResize = isEditMode && tool.allowedSpans().size > 1
                    val doubleTapModifier = if (canResize) {
                        Modifier.pointerInput(placed) {
                            detectTapGestures(
                                onDoubleTap = { onResizeQuickTool(tool) }
                            )
                        }
                    } else {
                        Modifier
                    }

                    Box(
                        modifier = offsetModifier
                            .size(width = wDp, height = hDp)
                            .zIndex(if (isBeingDragged) 20f else 1f)
                            .scale(tileScale)
                            .then(doubleTapModifier)
                            .then(tileInteractionModifier)
                    ) {
                        if (tool.isDpad) {
                            QuickToolsDpadTile(
                                isEditMode = isEditMode,
                                onSendKey = onSendKey,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("qt_tile_dpad")
                            )
                        } else {
                            QuickToolsActionTile(
                                tool = tool,
                                colSpan = placed.colSpan,
                                isEditMode = isEditMode,
                                liveTypingMode = liveTypingMode,
                                onClick = {
                                    if (!isEditMode) {
                                        onTriggerTool(tool)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("qt_tile_${tool.name.lowercase()}")
                            )
                        }

                        if (isEditMode) {
                            // Top-right remove badge (-), tucked cleanly into top-right corner
                            Surface(
                                color = palette.primary,
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 3.dp, y = (-3).dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, palette.onAccent.copy(alpha = 0.7f), CircleShape)
                                    .clickable { onRemoveToPool(tool) }
                                    .testTag("qt_remove_${tool.name.lowercase()}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "${tool.label} Kaldır",
                                        tint = palette.onAccent,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickToolsActionTile(
    tool: QuickToolId,
    colSpan: Int,
    isEditMode: Boolean,
    liveTypingMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    val isAccentAction = tool == QuickToolId.ENTER
    val isTurkishChar = tool.isFixedSingleCell
    var isPressed by remember { mutableStateOf(false) }

    val bg by animateColorAsState(
        targetValue = when {
            isPressed -> palette.primary
            isAccentAction -> palette.controlThumb
            else -> palette.surface
        },
        animationSpec = tween(110),
        label = "qt_bg"
    )

    val contentTint = if (isPressed) {
        palette.onAccent
    } else if (isAccentAction) {
        palette.primary
    } else {
        palette.textPrimary
    }

    val icon = quickToolIcon(tool)

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                when {
                    isPressed -> palette.primary
                    isEditMode -> palette.borderElevated
                    isAccentAction -> palette.borderElevated
                    else -> palette.borderSubtle
                },
                RoundedCornerShape(12.dp)
            )
            .then(
                if (!isEditMode) {
                    Modifier.pointerInput(tool, liveTypingMode) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                val released = tryAwaitRelease()
                                isPressed = false
                                if (released) onClick()
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        if (colSpan >= 2) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tool.label,
                        tint = contentTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = if (icon != null) Alignment.End else Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = tool.shortLabel,
                        color = contentTint,
                        fontSize = if (isTurkishChar) 14.sp else 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        softWrap = false,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (tool.shortcutHint.isNotBlank()) {
                        Text(
                            text = tool.shortcutHint,
                            color = if (isPressed) palette.onAccent.copy(alpha = 0.8f) else palette.textMuted,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else {
            // 1x1 tile: Turkish/punctuation chars are fixed 1x1 text; action shortcuts show icon + crisp short label
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!isTurkishChar && icon != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tool.label,
                            tint = contentTint,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = tool.shortLabel,
                            color = contentTint.copy(alpha = 0.9f),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            softWrap = false,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                } else {
                    Text(
                        text = tool.shortLabel,
                        color = contentTint,
                        fontSize = if (isTurkishChar) 15.sp else 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        softWrap = false,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

internal fun quickToolIcon(tool: QuickToolId): ImageVector? = when (tool) {
    QuickToolId.SPACE -> Icons.Default.SpaceBar
    QuickToolId.BACKSPACE -> Icons.AutoMirrored.Filled.KeyboardBackspace
    QuickToolId.ENTER -> Icons.AutoMirrored.Filled.KeyboardReturn
    QuickToolId.CUT -> Icons.Default.ContentCut
    QuickToolId.COPY -> Icons.Default.ContentCopy
    QuickToolId.PASTE -> Icons.Default.ContentPaste
    QuickToolId.ZOOM_IN -> Icons.Default.Add
    QuickToolId.ZOOM_OUT -> Icons.Default.Remove
    QuickToolId.NEW_TAB -> Icons.Default.Add
    QuickToolId.CLOSE_TAB -> Icons.Default.Close
    QuickToolId.REFRESH -> Icons.Default.Refresh
    QuickToolId.ADDRESS_BAR -> Icons.Default.Search
    QuickToolId.FULLSCREEN -> Icons.Default.Fullscreen
    QuickToolId.SELECT_ALL -> Icons.Default.SelectAll
    QuickToolId.UNDO -> Icons.AutoMirrored.Filled.Undo
    QuickToolId.REDO -> Icons.AutoMirrored.Filled.Redo
    else -> null
}

/**
 * Classic 3x2 compact inverted-T D-Pad:
 * Row 0: [spacer] [UP]   [spacer]
 * Row 1: [LEFT]   [DOWN] [RIGHT]
 */
@Composable
internal fun QuickToolsDpadTile(
    isEditMode: Boolean,
    onSendKey: (hidKeyCode: Int, modifier: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(
            1.dp,
            if (isEditMode) palette.borderElevated else palette.borderSubtle,
            RoundedCornerShape(12.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                DpadCellButton(
                    icon = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Yukarı Ok",
                    isEditMode = isEditMode,
                    onClick = { onSendKey(HidConstants.KEY_UP, 0) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                DpadCellButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Sol Ok",
                    isEditMode = isEditMode,
                    onClick = { onSendKey(HidConstants.KEY_LEFT, 0) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )
                DpadCellButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Aşağı Ok",
                    isEditMode = isEditMode,
                    onClick = { onSendKey(HidConstants.KEY_DOWN, 0) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )
                DpadCellButton(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Sağ Ok",
                    isEditMode = isEditMode,
                    onClick = { onSendKey(HidConstants.KEY_RIGHT, 0) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun DpadCellButton(
    icon: ImageVector,
    contentDescription: String,
    isEditMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    var isPressed by remember { mutableStateOf(false) }

    Surface(
        color = if (isPressed) palette.primary else palette.surfaceContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (isPressed) palette.primary else palette.borderSubtle,
                RoundedCornerShape(8.dp)
            )
            .then(
                if (!isEditMode) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                val released = tryAwaitRelease()
                                isPressed = false
                                if (released) onClick()
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isPressed) palette.onAccent else palette.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
