package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardBackspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.bluetooth.HidConstants
import com.example.model.PlacedQuickTool
import com.example.model.QuickToolId
import com.example.model.QuickToolsLayoutEngine
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RubyAccent
import com.example.ui.theme.RubyPrimary
import com.example.ui.theme.SurfaceBorderRuby
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.theme.SurfaceContainerCharcoal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

/**
 * Samsung One UI-style 6-column flexible Quick Tools grid & upper spare pool.
 * - Long-press any tile to activate edit mode (shows eye/remove badge on tiles & enables drag-and-drop reordering).
 * - Removing a tile sends it to the upper Spare Pool; tapping any pool item inserts it back into the grid.
 */
@Composable
fun QuickToolsGridSection(
    activeTools: List<QuickToolId>,
    poolTools: List<QuickToolId>,
    liveTypingMode: Boolean,
    isTurkishLayout: Boolean,
    onMoveQuickTool: (fromIndex: Int, toIndex: Int) -> Unit,
    onRemoveToPool: (QuickToolId) -> Unit,
    onAddFromPool: (QuickToolId) -> Unit,
    onTriggerTool: (QuickToolId) -> Unit,
    onSendKey: (hidKeyCode: Int, modifier: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditMode by remember { mutableStateOf(false) }
    var poolExpanded by remember { mutableStateOf(false) }
    var draggedTool by remember { mutableStateOf<QuickToolId?>(null) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }

    val (placedTools, totalRows) = remember(activeTools) {
        QuickToolsLayoutEngine.pack(activeTools, QuickToolsLayoutEngine.GRID_COLUMNS)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Minimal Quick Tools Header & Pool Chip (no distracting side pencil icon)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isEditMode) "SÜRÜKLE & DÜZENLE" else "HIZLI ARAÇLAR",
                    color = if (isEditMode) RubyPrimary else TextMuted,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Surface(
                    color = if (poolExpanded || isEditMode) RubyPrimary.copy(alpha = 0.16f) else SurfaceCharcoal,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (poolExpanded || isEditMode) RubyPrimary.copy(alpha = 0.55f) else SurfaceBorderRuby,
                            RoundedCornerShape(10.dp)
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { poolExpanded = !poolExpanded }
                        .testTag("quick_tools_pool_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Havuz",
                            tint = RubyPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Havuz (${poolTools.size})",
                            color = RubyPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isEditMode) {
                    Surface(
                        color = GreenSuccess.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(1.dp, GreenSuccess.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                isEditMode = false
                                draggedTool = null
                                dragDelta = Offset.Zero
                            }
                            .testTag("quick_tools_done_edit_btn")
                    ) {
                        Text(
                            text = "BİTTİ",
                            color = GreenSuccess,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Text(
                        text = if (isTurkishLayout) "TR-Q • Basılı Tut: Düzenle" else "US-EN • Basılı Tut: Düzenle",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Upper Spare Pool (Yedek Havuz): Visible when expanded OR during Edit Mode
        AnimatedVisibility(
            visible = poolExpanded || isEditMode,
            enter = expandVertically(tween(200)) + fadeIn(tween(150)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(120))
        ) {
            Surface(
                color = SurfaceCharcoal,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, RubyPrimary.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .testTag("quick_tools_pool_bar")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = if (poolTools.isEmpty()) {
                            "Tüm araçlar ızgarada. Kaldırmak için herhangi bir tuşa basılı tutup göz simgesine dokunun."
                        } else {
                            "YEDEK HAVUZ (Eklemek için dokunun)"
                        },
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (poolTools.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            poolTools.forEach { tool ->
                                Surface(
                                    color = SurfaceContainerCharcoal,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .border(1.dp, RubyPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onAddFromPool(tool) }
                                        .testTag("pool_add_${tool.name.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Ekle",
                                            tint = RubyPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "${tool.shortLabel} (${tool.colSpan}x${tool.rowSpan})",
                                            color = TextPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6-Column Flexible Quick Tools Grid Container
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick_tools_grid")
        ) {
            val density = LocalDensity.current
            val gapDp = 5.dp
            val cellHeightDp = 38.dp
            val gapPx = with(density) { gapDp.toPx() }
            val cellHeightPx = with(density) { cellHeightDp.toPx() }
            val totalWidthPx = with(density) { maxWidth.toPx() }
            val cellWidthPx = ((totalWidthPx - gapPx * (QuickToolsLayoutEngine.GRID_COLUMNS - 1)) /
                QuickToolsLayoutEngine.GRID_COLUMNS).coerceAtLeast(10f)
            val totalRowsEffective = totalRows.coerceAtLeast(1)
            val gridHeightDp = with(density) {
                (totalRowsEffective * cellHeightPx + (totalRowsEffective - 1) * gapPx).toDp()
            }

            // Compute exact pixel rects for live drag-and-drop hit testing
            val tileRects = remember(placedTools, cellWidthPx, cellHeightPx, gapPx) {
                placedTools.associate { placed ->
                    val x = placed.col * (cellWidthPx + gapPx)
                    val y = placed.row * (cellHeightPx + gapPx)
                    val w = placed.colSpan * cellWidthPx + (placed.colSpan - 1) * gapPx
                    val h = placed.rowSpan * cellHeightPx + (placed.rowSpan - 1) * gapPx
                    placed.tool to Rect(x, y, x + w, y + h)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeightDp)
            ) {
                placedTools.forEach { placed ->
                    val tool = placed.tool
                    val isBeingDragged = draggedTool == tool
                    val baseX = placed.col * (cellWidthPx + gapPx)
                    val baseY = placed.row * (cellHeightPx + gapPx)
                    val wPx = placed.colSpan * cellWidthPx + (placed.colSpan - 1) * gapPx
                    val hPx = placed.rowSpan * cellHeightPx + (placed.rowSpan - 1) * gapPx

                    val animX by animateDpAsState(
                        targetValue = with(density) { baseX.toDp() },
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
                        label = "qt_x_${tool.name}"
                    )
                    val animY by animateDpAsState(
                        targetValue = with(density) { baseY.toDp() },
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
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

                    val tileInteractionModifier = if (isEditMode) {
                        Modifier.pointerInput(tool, activeTools, tileRects) {
                            detectDragGestures(
                                onDragStart = {
                                    draggedTool = tool
                                    dragDelta = Offset.Zero
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragDelta += dragAmount
                                    val draggedRect = tileRects[tool] ?: return@detectDragGestures
                                    val center = Offset(
                                        draggedRect.center.x + dragDelta.x,
                                        draggedRect.center.y + dragDelta.y
                                    )
                                    val hitEntry = tileRects.entries.firstOrNull { (otherTool, rect) ->
                                        otherTool != tool && rect.contains(center)
                                    }
                                    if (hitEntry != null) {
                                        val fromIdx = activeTools.indexOf(tool)
                                        val toIdx = activeTools.indexOf(hitEntry.key)
                                        if (fromIdx >= 0 && toIdx >= 0 && fromIdx != toIdx) {
                                            val oldRect = tileRects[tool]
                                            val newRect = tileRects[hitEntry.key]
                                            if (oldRect != null && newRect != null) {
                                                // Compensate dragDelta when underlying grid position jumps
                                                dragDelta -= (newRect.topLeft - oldRect.topLeft)
                                            }
                                            onMoveQuickTool(fromIdx, toIdx)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggedTool = null
                                    dragDelta = Offset.Zero
                                },
                                onDragCancel = {
                                    draggedTool = null
                                    dragDelta = Offset.Zero
                                }
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
                            .then(tileInteractionModifier)
                    ) {
                        if (tool.isDpad) {
                            QuickToolsDpadTile(
                                isEditMode = isEditMode,
                                onLongPressEdit = { isEditMode = true },
                                onSendKey = onSendKey,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("qt_tile_dpad")
                            )
                        } else {
                            QuickToolsActionTile(
                                tool = tool,
                                isEditMode = isEditMode,
                                liveTypingMode = liveTypingMode,
                                onLongPressEdit = { isEditMode = !isEditMode },
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

                        // Eye / Remove Badge directly ON the tile in Edit Mode
                        if (isEditMode) {
                            Surface(
                                color = RubyPrimary,
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(20.dp)
                                    .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                                    .clip(CircleShape)
                                    .clickable { onRemoveToPool(tool) }
                                    .testTag("qt_remove_${tool.name.lowercase()}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.VisibilityOff,
                                        contentDescription = "${tool.label} Havuza Kaldır",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
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
    isEditMode: Boolean,
    liveTypingMode: Boolean,
    onLongPressEdit: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPrimaryAccent = tool == QuickToolId.PLAY_PAUSE || tool == QuickToolId.ENTER
    val isTurkishChar = tool.name.startsWith("TR_") || tool.name.startsWith("PUNCT_")
    var isPressed by remember { mutableStateOf(false) }

    val bg by animateColorAsState(
        targetValue = when {
            isPressed -> RubyPrimary.copy(alpha = 0.34f)
            isPrimaryAccent -> RubyPrimary.copy(alpha = 0.16f)
            else -> SurfaceCharcoal
        },
        animationSpec = tween(120),
        label = "qt_bg"
    )

    val (icon, tint) = when (tool) {
        QuickToolId.PREV -> Icons.Default.SkipPrevious to TextPrimary
        QuickToolId.NEXT -> Icons.Default.SkipNext to TextPrimary
        QuickToolId.VOL_DOWN -> Icons.AutoMirrored.Filled.VolumeDown to TextPrimary
        QuickToolId.VOL_UP -> Icons.AutoMirrored.Filled.VolumeUp to TextPrimary
        QuickToolId.MUTE -> Icons.AutoMirrored.Filled.VolumeMute to TextSecondary
        QuickToolId.PLAY_PAUSE -> Icons.Default.PlayArrow to RubyPrimary
        QuickToolId.SPACE -> Icons.Default.SpaceBar to TextPrimary
        QuickToolId.BACKSPACE -> Icons.AutoMirrored.Filled.KeyboardBackspace to RubyAccent
        QuickToolId.ENTER -> Icons.AutoMirrored.Filled.KeyboardReturn to RubyPrimary
        QuickToolId.CUT -> Icons.Default.ContentCut to TextPrimary
        QuickToolId.COPY -> Icons.Default.ContentCopy to TextPrimary
        QuickToolId.PASTE -> Icons.Default.ContentPaste to TextPrimary
        QuickToolId.ZOOM_IN -> Icons.Default.Add to TextPrimary
        QuickToolId.ZOOM_OUT -> Icons.Default.Remove to TextPrimary
        else -> null to if (isTurkishChar) RubyPrimary else TextPrimary
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(9.dp),
        modifier = modifier
            .border(
                1.dp,
                when {
                    isEditMode -> RubyPrimary.copy(alpha = 0.7f)
                    isPrimaryAccent -> RubyPrimary.copy(alpha = 0.55f)
                    else -> SurfaceBorderRuby
                },
                RoundedCornerShape(9.dp)
            )
            .clip(RoundedCornerShape(9.dp))
            .then(
                if (!isEditMode) {
                    Modifier.pointerInput(tool, liveTypingMode) {
                        detectTapGestures(
                            onLongPress = { onLongPressEdit() },
                            onPress = {
                                isPressed = true
                                val released = tryAwaitRelease()
                                isPressed = false
                                if (released) onClick()
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = tool.label,
                    tint = tint,
                    modifier = Modifier.size(if (tool.colSpan >= 2) 15.dp else 16.dp)
                )
            }
            if (icon == null || tool.colSpan >= 2) {
                if (icon != null) Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isTurkishChar) tool.shortLabel else tool.shortLabel,
                    color = tint,
                    fontSize = if (isTurkishChar) 14.sp else 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
private fun QuickToolsDpadTile(
    isEditMode: Boolean,
    onLongPressEdit: () -> Unit,
    onSendKey: (hidKeyCode: Int, modifier: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceCharcoal,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(
            1.dp,
            if (isEditMode) RubyPrimary.copy(alpha = 0.75f) else SurfaceBorderRuby,
            RoundedCornerShape(12.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DpadCellButton(
                    icon = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Yukarı Ok",
                    isEditMode = isEditMode,
                    onLongPressEdit = onLongPressEdit,
                    onClick = { onSendKey(HidConstants.KEY_UP, 0) },
                    modifier = Modifier
                        .fillMaxWidth(0.36f)
                        .fillMaxSize()
                        .padding(2.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DpadCellButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Sol Ok",
                    isEditMode = isEditMode,
                    onLongPressEdit = onLongPressEdit,
                    onClick = { onSendKey(HidConstants.KEY_LEFT, 0) },
                    modifier = Modifier.weight(1f).fillMaxSize()
                )
                DpadCellButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Aşağı Ok",
                    isEditMode = isEditMode,
                    onLongPressEdit = onLongPressEdit,
                    onClick = { onSendKey(HidConstants.KEY_DOWN, 0) },
                    modifier = Modifier.weight(1f).fillMaxSize()
                )
                DpadCellButton(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Sağ Ok",
                    isEditMode = isEditMode,
                    onLongPressEdit = onLongPressEdit,
                    onClick = { onSendKey(HidConstants.KEY_RIGHT, 0) },
                    modifier = Modifier.weight(1f).fillMaxSize()
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
    onLongPressEdit: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceContainerCharcoal,
        shape = RoundedCornerShape(9.dp),
        modifier = modifier
            .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(9.dp))
            .clip(RoundedCornerShape(9.dp))
            .then(
                if (!isEditMode) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { onLongPressEdit() },
                            onTap = { onClick() }
                        )
                    }
                } else Modifier
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = RubyPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
