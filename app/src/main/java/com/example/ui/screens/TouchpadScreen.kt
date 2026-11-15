package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetooth.HidConstants
import com.example.bluetooth.TouchContact
import com.example.model.ButtonPosition
import com.example.model.HidProfileMode
import com.example.model.MouseButton
import com.example.model.NavDockItemId
import com.example.model.RemoteSettings
import com.example.model.ScrollSide
import com.example.ui.components.DockOrderEditorPanel
import com.example.ui.components.ExpressiveDockRow
import com.example.ui.components.expressivePressDetect
import com.example.ui.theme.RubyAccent
import com.example.ui.theme.RubyPrimary
import com.example.ui.theme.RubySecondary
import com.example.ui.theme.ScrollGutterBg
import com.example.ui.theme.ScrollGutterThumb
import com.example.ui.theme.SurfaceBorderRuby
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrackpadGridDot
import com.example.ui.theme.TrackpadSurface
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun TouchpadScreen(
    settings: RemoteSettings,
    isWifiMode: Boolean = false,
    onTouchpadContacts: (contacts: List<TouchContact>, clickpadPressed: Boolean) -> Unit,
    onTrackpadDrag: (dx: Float, dy: Float) -> Unit,
    onScrollGutterDrag: (deltaY: Float) -> Unit,
    onScrollStep: (ticks: Int) -> Unit,
    onHorizontalPan: (deltaX: Float) -> Unit,
    onTwoFingerHorizontalSwipe: (deltaX: Float) -> Unit,
    onHorizontalStep: (direction: Int) -> Unit,
    onTrackpadTap: () -> Unit,
    onMouseButtonClick: (MouseButton) -> Unit,
    onMouseButtonDown: (MouseButton) -> Unit,
    onMouseButtonUp: (MouseButton) -> Unit,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onRecentAppsClick: () -> Unit,
    onMoveNavDockItem: (index: Int, delta: Int) -> Unit = { _, _ -> },
    onToggleNavDockHidden: (NavDockItemId) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 4.dp)
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (settings.buttonPosition == ButtonPosition.TOP) {
            MouseButtonsBar(
                settings = settings,
                onMouseButtonClick = onMouseButtonClick,
                onMouseButtonDown = onMouseButtonDown,
                onMouseButtonUp = onMouseButtonUp,
                modifier = Modifier.padding(end = 46.dp)
            )
        }

        if (settings.showHorizontalScrollGutter) {
            HorizontalScrollGutter(
                onStep = onHorizontalStep,
                onPan = onHorizontalPan,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = if (settings.buttonPosition == ButtonPosition.TOP) 0.dp else 46.dp)
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (settings.scrollSide == ScrollSide.LEFT) {
                InteractiveScrollGutter(
                    onScrollDrag = onScrollGutterDrag,
                    onScrollStep = onScrollStep,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            TrackpadSurfacePanel(
                settings = settings,
                isWifiMode = isWifiMode,
                onTouchpadContacts = onTouchpadContacts,
                onTrackpadDrag = onTrackpadDrag,
                onScrollGutterDrag = onScrollGutterDrag,
                onTwoFingerHorizontalSwipe = onTwoFingerHorizontalSwipe,
                onTrackpadTap = onTrackpadTap,
                onMouseButtonClick = onMouseButtonClick,
                onHomeClick = onHomeClick,
                onRecentAppsClick = onRecentAppsClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("trackpad_canvas_area")
            )

            if (settings.scrollSide == ScrollSide.RIGHT) {
                InteractiveScrollGutter(
                    onScrollDrag = onScrollGutterDrag,
                    onScrollStep = onScrollStep,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }

        // Mouse Clicks sit ABOVE Navigation Dock so Touchpad aligns seamlessly with Air Mouse bottom
        if (settings.buttonPosition == ButtonPosition.BOTTOM || settings.buttonPosition == ButtonPosition.SPLIT) {
            MouseButtonsBar(
                settings = settings,
                onMouseButtonClick = onMouseButtonClick,
                onMouseButtonDown = onMouseButtonDown,
                onMouseButtonUp = onMouseButtonUp
            )
        }

        CustomizableNavigationDock(
            settings = settings,
            onBackClick = onBackClick,
            onHomeClick = onHomeClick,
            onRecentAppsClick = onRecentAppsClick,
            onMouseButtonClick = onMouseButtonClick,
            onMoveNavDockItem = onMoveNavDockItem,
            onToggleNavDockHidden = onToggleNavDockHidden
        )

        Spacer(modifier = Modifier.height(74.dp))
    }
}

@Composable
internal fun TrackpadSurfacePanel(
    settings: RemoteSettings,
    isWifiMode: Boolean,
    onTouchpadContacts: (contacts: List<TouchContact>, clickpadPressed: Boolean) -> Unit,
    onTrackpadDrag: (dx: Float, dy: Float) -> Unit,
    onScrollGutterDrag: (deltaY: Float) -> Unit,
    onTwoFingerHorizontalSwipe: (deltaX: Float) -> Unit,
    onTrackpadTap: () -> Unit,
    onMouseButtonClick: (MouseButton) -> Unit,
    onHomeClick: () -> Unit,
    onRecentAppsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var activeTouchPositions by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var trackpadPixelSize by remember { mutableStateOf(IntSize(1000, 1000)) }
    val touchRippleAlpha = remember { Animatable(0f) }

    val usePrecisionProtocol = !isWifiMode && settings.hidProfileMode == HidProfileMode.PRECISION_TOUCHPAD

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TrackpadSurface)
            .border(1.5.dp, SurfaceBorderRuby, RoundedCornerShape(16.dp))
            .onSizeChanged { trackpadPixelSize = it }
            .pointerInput(usePrecisionProtocol, settings.tapToClick, settings.pointerSensitivity) {
                awaitPointerEventScope {
                    while (true) {
                        val downEvent = awaitPointerEvent()
                        val downPointers = downEvent.changes.filter { it.pressed }
                        if (downPointers.isEmpty()) continue

                        val startTime = System.currentTimeMillis()
                        var maxPointers = downPointers.size
                        var totalMovement = 0f
                        var lastPositions = downPointers.associate { it.id to it.position }
                        var threeFingerSwipeTriggered = false
                        var threeFingerAccumDy = 0f

                        val pointerToSlot = mutableMapOf<PointerId, Int>()
                        val usedSlots = mutableSetOf<Int>()
                        val lastSlotCoords = mutableMapOf<Int, Pair<Int, Int>>()

                        fun toLogicalCoords(pos: Offset): Pair<Int, Int> {
                            val w = trackpadPixelSize.width.coerceAtLeast(1).toFloat()
                            val h = trackpadPixelSize.height.coerceAtLeast(1).toFloat()
                            val lx = ((pos.x / w) * HidConstants.TP_LOGICAL_MAX_X).roundToInt()
                                .coerceIn(0, HidConstants.TP_LOGICAL_MAX_X)
                            val ly = ((pos.y / h) * HidConstants.TP_LOGICAL_MAX_Y).roundToInt()
                                .coerceIn(0, HidConstants.TP_LOGICAL_MAX_Y)
                            return lx to ly
                        }

                        fun assignSlot(id: PointerId): Int? {
                            pointerToSlot[id]?.let { return it }
                            for (s in 0 until HidConstants.TP_MAX_CONTACTS) {
                                if (s !in usedSlots) {
                                    usedSlots.add(s)
                                    pointerToSlot[id] = s
                                    return s
                                }
                            }
                            return null
                        }

                        activeTouchPositions = downPointers.map { it.position }
                        coroutineScope.launch { touchRippleAlpha.snapTo(0.45f) }

                        if (usePrecisionProtocol) {
                            val initialContacts = downPointers.mapNotNull { ch ->
                                val slot = assignSlot(ch.id) ?: return@mapNotNull null
                                val (lx, ly) = toLogicalCoords(ch.position)
                                lastSlotCoords[slot] = lx to ly
                                TouchContact(slot, true, lx, ly)
                            }
                            if (initialContacts.isNotEmpty()) {
                                onTouchpadContacts(initialContacts, false)
                            }
                        }

                        while (true) {
                            val moveEvent = awaitPointerEvent()
                            val currentPressed = moveEvent.changes.filter { it.pressed }

                            if (currentPressed.isEmpty()) {
                                val duration = System.currentTimeMillis() - startTime
                                if (usePrecisionProtocol) {
                                    val releaseContacts = pointerToSlot.values.map { slot ->
                                        val coords = lastSlotCoords[slot] ?: (1600 to 1000)
                                        TouchContact(slot, false, coords.first, coords.second)
                                    }
                                    if (releaseContacts.isNotEmpty()) {
                                        onTouchpadContacts(releaseContacts, false)
                                    }
                                    onTouchpadContacts(emptyList(), false)
                                } else {
                                    if (duration < 280 && totalMovement < 18f) {
                                        if (maxPointers == 1) {
                                            onTrackpadTap()
                                        } else if (maxPointers == 2) {
                                            onMouseButtonClick(MouseButton.RIGHT)
                                        }
                                    }
                                }

                                if (duration < 280 && totalMovement < 18f && maxPointers == 1 && settings.tapToClick) {
                                    coroutineScope.launch {
                                        touchRippleAlpha.snapTo(0.75f)
                                        touchRippleAlpha.animateTo(0f, tween(240))
                                    }
                                } else {
                                    coroutineScope.launch {
                                        touchRippleAlpha.animateTo(0f, tween(150))
                                    }
                                }

                                activeTouchPositions = emptyList()
                                break
                            }

                            if (currentPressed.size > maxPointers) {
                                maxPointers = currentPressed.size
                            }
                            activeTouchPositions = currentPressed.map { it.position }

                            if (usePrecisionProtocol) {
                                val activeIds = currentPressed.map { it.id }.toSet()
                                val liftedIds = pointerToSlot.keys.filter { it !in activeIds }
                                val contactsToSend = mutableListOf<TouchContact>()

                                for (id in liftedIds) {
                                    val slot = pointerToSlot.remove(id)
                                    if (slot != null) {
                                        usedSlots.remove(slot)
                                        val coords = lastSlotCoords.remove(slot) ?: (1600 to 1000)
                                        contactsToSend.add(TouchContact(slot, false, coords.first, coords.second))
                                    }
                                }

                                currentPressed.forEach { change ->
                                    val prevPos = lastPositions[change.id] ?: change.position
                                    totalMovement += abs(change.position.x - prevPos.x) + abs(change.position.y - prevPos.y)
                                    change.consume()

                                    val slot = assignSlot(change.id)
                                    if (slot != null) {
                                        val (lx, ly) = toLogicalCoords(change.position)
                                        lastSlotCoords[slot] = lx to ly
                                        contactsToSend.add(TouchContact(slot, true, lx, ly))
                                    }
                                }

                                if (contactsToSend.isNotEmpty()) {
                                    onTouchpadContacts(contactsToSend, false)
                                }
                            } else {
                                if (currentPressed.size == 1) {
                                    val change = currentPressed[0]
                                    val prevPos = lastPositions[change.id] ?: change.position
                                    val dx = change.position.x - prevPos.x
                                    val dy = change.position.y - prevPos.y
                                    totalMovement += abs(dx) + abs(dy)
                                    change.consume()
                                    onTrackpadDrag(dx, dy)
                                } else if (currentPressed.size == 2) {
                                    val p0 = currentPressed[0]
                                    val p1 = currentPressed[1]
                                    val prev0 = lastPositions[p0.id] ?: p0.position
                                    val prev1 = lastPositions[p1.id] ?: p1.position

                                    val prevDist = hypot((prev0.x - prev1.x).toDouble(), (prev0.y - prev1.y).toDouble())
                                    val currDist = hypot((p0.position.x - p1.position.x).toDouble(), (p0.position.y - p1.position.y).toDouble())
                                    val pinchDelta = (currDist - prevDist).toFloat()

                                    val avgDx = ((p0.position.x - prev0.x) + (p1.position.x - prev1.x)) / 2f
                                    val avgDy = ((p0.position.y - prev0.y) + (p1.position.y - prev1.y)) / 2f
                                    totalMovement += abs(avgDx) + abs(avgDy)

                                    p0.consume()
                                    p1.consume()

                                    if (abs(pinchDelta) > 6f && abs(pinchDelta) > abs(avgDx) + abs(avgDy)) {
                                        onScrollGutterDrag(if (pinchDelta > 0) -14f else 14f)
                                    } else if (abs(avgDx) > abs(avgDy) * 1.15f && abs(avgDx) > 3f) {
                                        onTwoFingerHorizontalSwipe(avgDx)
                                    } else if (abs(avgDy) > 2f) {
                                        onScrollGutterDrag(avgDy)
                                    }
                                } else if (currentPressed.size >= 3) {
                                    val avgDy = currentPressed.map { ch ->
                                        val prev = lastPositions[ch.id] ?: ch.position
                                        ch.position.y - prev.y
                                    }.average().toFloat()
                                    totalMovement += abs(avgDy)
                                    currentPressed.forEach { it.consume() }
                                    threeFingerAccumDy += avgDy
                                    if (!threeFingerSwipeTriggered) {
                                        if (threeFingerAccumDy < -70f) {
                                            threeFingerSwipeTriggered = true
                                            onRecentAppsClick()
                                        } else if (threeFingerAccumDy > 70f) {
                                            threeFingerSwipeTriggered = true
                                            onHomeClick()
                                        }
                                    }
                                }
                            }

                            lastPositions = currentPressed.associate { it.id to it.position }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 34.dp.toPx()
            var x = step
            while (x < size.width) {
                var y = step
                while (y < size.height) {
                    drawCircle(
                        color = TrackpadGridDot,
                        radius = 1.6f,
                        center = Offset(x, y)
                    )
                    y += step
                }
                x += step
            }

            val glowColors = listOf(RubyPrimary, RubyAccent, RubySecondary, Color.White)
            activeTouchPositions.forEachIndexed { idx, pos ->
                val c = glowColors[idx % glowColors.size]
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            c.copy(alpha = touchRippleAlpha.value.coerceAtLeast(0.25f)),
                            Color.Transparent
                        ),
                        center = pos,
                        radius = 58.dp.toPx()
                    ),
                    radius = 58.dp.toPx(),
                    center = pos
                )
                drawCircle(
                    color = c.copy(alpha = 0.85f),
                    radius = 5.dp.toPx(),
                    center = pos
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = when {
                    isWifiMode -> "WI-FI AKILLI İMLEÇ"
                    settings.hidProfileMode == HidProfileMode.PRECISION_TOUCHPAD -> "PRECISION TOUCHPAD"
                    else -> "TOUCHPAD (KLASİK)"
                },
                color = TextMuted.copy(alpha = 0.28f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (usePrecisionProtocol) {
                    "1 Parmak: İmleç • 2 Parmak: Kaydır / Zoom / Sayfa Geçişi • 3 Parmak: Ana Ekran / Son"
                } else {
                    "1 Parmak: İmleç • 2 Parmak: Yatay/Dikey Kaydır • 3 Parmak: Ana Ekran / Son"
                },
                color = TextMuted.copy(alpha = 0.48f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
internal fun HorizontalScrollGutter(
    onStep: (direction: Int) -> Unit,
    onPan: (deltaX: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ScrollGutterBg)
            .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(10.dp))
    ) {
        val totalWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val arrowWidthPx = with(LocalDensity.current) { 42.dp.toPx() }
        val thumbWidthPx = with(LocalDensity.current) { 54.dp.toPx() }

        val minThumbX = arrowWidthPx
        val maxThumbX = (totalWidthPx - arrowWidthPx - thumbWidthPx).coerceAtLeast(minThumbX)
        val centerThumbX = (minThumbX + maxThumbX) / 2f

        val thumbAnimatable = remember(maxWidth) { Animatable(centerThumbX) }
        val scope = rememberCoroutineScope()

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(42.dp)
                .fillMaxHeight()
                .clickable { onStep(-1) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Önceki",
                tint = RubyPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Surface(
            color = ScrollGutterThumb.copy(alpha = 0.22f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(thumbAnimatable.value.roundToInt(), 0) }
                .width(54.dp)
                .height(28.dp)
                .border(1.2.dp, ScrollGutterThumb, RoundedCornerShape(8.dp))
                .pointerInput(minThumbX, maxThumbX) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val nextX = (thumbAnimatable.value + dragAmount.x).coerceIn(minThumbX, maxThumbX)
                            scope.launch { thumbAnimatable.snapTo(nextX) }
                            onPan(dragAmount.x)
                        },
                        onDragEnd = {
                            scope.launch {
                                thumbAnimatable.animateTo(
                                    targetValue = centerThumbX,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                thumbAnimatable.animateTo(
                                    targetValue = centerThumbX,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    )
                }
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Yatay Kaydır",
                    tint = RubyPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "PAN",
                    color = RubyPrimary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(42.dp)
                .fillMaxHeight()
                .clickable { onStep(1) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Sonraki",
                tint = RubyPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
internal fun InteractiveScrollGutter(
    onScrollDrag: (deltaY: Float) -> Unit,
    onScrollStep: (ticks: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .width(50.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(ScrollGutterBg)
            .border(1.2.dp, SurfaceBorderRuby, RoundedCornerShape(12.dp))
            .testTag("dedicated_scroll_gutter")
    ) {
        val totalHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val arrowAreaHeightPx = with(LocalDensity.current) { 40.dp.toPx() }
        val thumbHeightPx = with(LocalDensity.current) { 52.dp.toPx() }

        val minThumbY = arrowAreaHeightPx
        val maxThumbY = (totalHeightPx - arrowAreaHeightPx - thumbHeightPx).coerceAtLeast(minThumbY)
        val centerThumbY = (minThumbY + maxThumbY) / 2f

        val thumbAnimatable = remember(maxHeight) { Animatable(centerThumbY) }
        val scope = rememberCoroutineScope()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val grooveColor = SurfaceBorderRuby
            val step = 14.dp.toPx()
            var y = arrowAreaHeightPx + 10.dp.toPx()
            while (y < size.height - arrowAreaHeightPx - 10.dp.toPx()) {
                drawLine(
                    color = grooveColor,
                    start = Offset(14.dp.toPx(), y),
                    end = Offset(size.width - 14.dp.toPx(), y),
                    strokeWidth = 1.5.dp.toPx()
                )
                y += step
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .clickable { onScrollStep(2) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Yukarı Kaydır",
                tint = RubyPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Surface(
            color = ScrollGutterThumb.copy(alpha = 0.25f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, thumbAnimatable.value.roundToInt()) }
                .width(38.dp)
                .height(52.dp)
                .border(1.5.dp, ScrollGutterThumb, RoundedCornerShape(8.dp))
                .pointerInput(minThumbY, maxThumbY) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val nextY = (thumbAnimatable.value + dragAmount.y).coerceIn(minThumbY, maxThumbY)
                            scope.launch { thumbAnimatable.snapTo(nextY) }
                            onScrollDrag(dragAmount.y)
                        },
                        onDragEnd = {
                            scope.launch {
                                thumbAnimatable.animateTo(
                                    targetValue = centerThumbY,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                thumbAnimatable.animateTo(
                                    targetValue = centerThumbY,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    )
                }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Kaydırma Tutacı",
                    tint = RubyPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "KAYDIR",
                    color = RubyPrimary,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                .clickable { onScrollStep(-2) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Aşağı Kaydır",
                tint = RubyPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
internal fun CustomizableNavigationDock(
    settings: RemoteSettings,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onRecentAppsClick: () -> Unit,
    onMouseButtonClick: (MouseButton) -> Unit,
    onMoveNavDockItem: (index: Int, delta: Int) -> Unit,
    onToggleNavDockHidden: (NavDockItemId) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditor by remember { mutableStateOf(false) }

    val visibleItems = remember(
        settings.navDockOrder,
        settings.hiddenNavDockItems,
        settings.showFunctionButtons
    ) {
        settings.navDockOrder.filter { item ->
            val allowedByFnToggle = settings.showFunctionButtons || (item != NavDockItemId.F1 && item != NavDockItemId.F2)
            allowedByFnToggle && item !in settings.hiddenNavDockItems
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DockOrderEditorPanel(
            visible = showEditor,
            title = "ALT DOCK TUŞ SIRASI & GİZLE (BASILI TUT VEYA DÜZENLE)",
            orderedItems = settings.navDockOrder.filter {
                settings.showFunctionButtons || (it != NavDockItemId.F1 && it != NavDockItemId.F2)
            },
            hiddenItems = settings.hiddenNavDockItems,
            labelOf = { it.defaultLabel },
            canHide = { item -> item != NavDockItemId.HOME },
            onMoveLeft = { idx ->
                val activeList = settings.navDockOrder.filter {
                    settings.showFunctionButtons || (it != NavDockItemId.F1 && it != NavDockItemId.F2)
                }
                val item = activeList.getOrNull(idx) ?: return@DockOrderEditorPanel
                val realIdx = settings.navDockOrder.indexOf(item)
                if (realIdx > 0) onMoveNavDockItem(realIdx, -1)
            },
            onMoveRight = { idx ->
                val activeList = settings.navDockOrder.filter {
                    settings.showFunctionButtons || (it != NavDockItemId.F1 && it != NavDockItemId.F2)
                }
                val item = activeList.getOrNull(idx) ?: return@DockOrderEditorPanel
                val realIdx = settings.navDockOrder.indexOf(item)
                if (realIdx in 0 until settings.navDockOrder.lastIndex) onMoveNavDockItem(realIdx, 1)
            },
            onToggleHidden = onToggleNavDockHidden,
            onDone = { showEditor = false }
        )

        Surface(
            color = SurfaceCharcoal,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(12.dp))
                .testTag("customizable_navigation_dock")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressiveDockRow(
                    items = visibleItems,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalSpacing = 3.dp,
                    itemWeight = { item ->
                        if (item == NavDockItemId.F1 || item == NavDockItemId.F2) 0.58f else 1f
                    }
                ) { item, _, isPressed, _, _, onPressChange ->
                    when (item) {
                        NavDockItemId.F1 -> FunctionBadgeTile(
                            label = "F1",
                            isPressed = isPressed,
                            onPressChange = onPressChange,
                            onLongPress = { showEditor = !showEditor },
                            onClick = { onMouseButtonClick(MouseButton.BUTTON_4) },
                            modifier = Modifier.fillMaxSize().testTag("dock_btn_f1")
                        )
                        NavDockItemId.F2 -> FunctionBadgeTile(
                            label = "F2",
                            isPressed = isPressed,
                            onPressChange = onPressChange,
                            onLongPress = { showEditor = !showEditor },
                            onClick = { onMouseButtonClick(MouseButton.BUTTON_5) },
                            modifier = Modifier.fillMaxSize().testTag("dock_btn_f2")
                        )
                        NavDockItemId.BACK -> NavDockTile(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            label = "GERİ",
                            isPressed = isPressed,
                            onPressChange = onPressChange,
                            onLongPress = { showEditor = !showEditor },
                            onClick = onBackClick,
                            modifier = Modifier.fillMaxSize().testTag("dock_btn_back")
                        )
                        NavDockItemId.HOME -> NavDockTile(
                            icon = Icons.Default.Home,
                            label = "ANA EKRAN",
                            isPressed = isPressed,
                            onPressChange = onPressChange,
                            onLongPress = { showEditor = !showEditor },
                            onClick = onHomeClick,
                            modifier = Modifier.fillMaxSize().testTag("dock_btn_home")
                        )
                        NavDockItemId.RECENT -> NavDockTile(
                            icon = Icons.Default.Layers,
                            label = "SON",
                            isPressed = isPressed,
                            onPressChange = onPressChange,
                            onLongPress = { showEditor = !showEditor },
                            onClick = onRecentAppsClick,
                            modifier = Modifier.fillMaxSize().testTag("dock_btn_recent")
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(width = 30.dp, height = 34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showEditor = !showEditor }
                        .testTag("edit_nav_dock_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Alt Dock Sırasını Düzenle",
                        tint = if (showEditor) RubyPrimary else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FunctionBadgeTile(
    label: String,
    isPressed: Boolean,
    onPressChange: (Boolean) -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .expressivePressDetect(
                key = label,
                onPressChange = onPressChange,
                onLongPress = onLongPress,
                onTap = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = if (isPressed) RubyPrimary else RubyPrimary.copy(alpha = 0.16f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.border(
                1.dp,
                if (isPressed) RubyPrimary else RubyPrimary.copy(alpha = 0.5f),
                RoundedCornerShape(6.dp)
            )
        ) {
            Text(
                text = label,
                color = if (isPressed) Color.White else RubyPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun NavDockTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isPressed: Boolean,
    onPressChange: (Boolean) -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (isPressed) RubyPrimary.copy(alpha = 0.22f) else Color.Transparent,
        animationSpec = tween(140),
        label = "nav_tile_bg"
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .expressivePressDetect(
                key = label,
                onPressChange = onPressChange,
                onLongPress = onLongPress,
                onTap = onClick
            )
            .padding(horizontal = 3.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = RubyPrimary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            color = if (isPressed) RubyPrimary else TextPrimary,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            softWrap = false,
            maxLines = 1
        )
    }
}

@Composable
internal fun MouseButtonsBar(
    settings: RemoteSettings,
    onMouseButtonClick: (MouseButton) -> Unit,
    onMouseButtonDown: (MouseButton) -> Unit,
    onMouseButtonUp: (MouseButton) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MouseButtonTile(
            title = "SOL TIK",
            subtitle = "Seç / Sürükle",
            accentColor = RubyPrimary,
            onClick = { onMouseButtonClick(MouseButton.LEFT) },
            onDown = { onMouseButtonDown(MouseButton.LEFT) },
            onUp = { onMouseButtonUp(MouseButton.LEFT) },
            modifier = Modifier.weight(1.3f).testTag("mouse_btn_left")
        )

        if (settings.showMiddleClick) {
            MouseButtonTile(
                title = "ORTA",
                subtitle = "Tekerlek Tık",
                accentColor = RubyAccent,
                onClick = { onMouseButtonClick(MouseButton.MIDDLE) },
                onDown = { onMouseButtonDown(MouseButton.MIDDLE) },
                onUp = { onMouseButtonUp(MouseButton.MIDDLE) },
                modifier = Modifier.weight(0.8f).testTag("mouse_btn_middle")
            )
        }

        if (settings.showRightClick) {
            MouseButtonTile(
                title = "SAĞ TIK",
                subtitle = "Menü",
                accentColor = RubyPrimary,
                onClick = { onMouseButtonClick(MouseButton.RIGHT) },
                onDown = { onMouseButtonDown(MouseButton.RIGHT) },
                onUp = { onMouseButtonUp(MouseButton.RIGHT) },
                modifier = Modifier.weight(1f).testTag("mouse_btn_right")
            )
        }
    }
}

@Composable
private fun MouseButtonTile(
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    onDown: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Surface(
        color = if (isPressed) accentColor.copy(alpha = 0.28f) else SurfaceCharcoal,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxHeight()
            .border(
                1.3.dp,
                if (isPressed) accentColor else SurfaceBorderRuby,
                RoundedCornerShape(12.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onDown()
                        val released = tryAwaitRelease()
                        isPressed = false
                        onUp()
                        if (released) {
                            onClick()
                        }
                    }
                )
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = title,
                color = if (isPressed) accentColor else TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 9.5.sp
            )
        }
    }
}
