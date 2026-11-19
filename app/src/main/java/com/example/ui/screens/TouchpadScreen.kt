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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.model.AppRole
import com.example.model.HorizontalPanPosition
import com.example.model.MouseButton
import com.example.model.NavDockItemId
import com.example.model.RemoteSettings
import com.example.model.ScrollSide
import com.example.ui.components.ExpressiveDockRow
import com.example.ui.components.ReceiverLockedControlContainer
import com.example.ui.components.UnifiedHorizontalPanRail
import com.example.ui.components.UnifiedVerticalScrollRail
import com.example.ui.components.expressivePressDetect
import com.example.ui.theme.TabRemoteTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

@Composable
fun TouchpadScreen(
    settings: RemoteSettings,
    isWifiMode: Boolean = false,
    isTabletLayout: Boolean = false,
    onTrackpadDrag: (dx: Float, dy: Float) -> Unit,
    onScrollGutterDrag: (deltaY: Float) -> Unit,
    onTwoFingerVerticalScroll: (deltaY: Float) -> Unit = onScrollGutterDrag,
    onTwoFingerGestureEnd: () -> Unit = {},
    onClassicPinchZoomStep: (pinchDelta: Float) -> Unit = {},
    onClassicPinchZoomEnd: () -> Unit = {},
    onScrollStep: (ticks: Int) -> Unit,
    onToggleScrollMode: () -> Unit = {},
    onHorizontalPan: (deltaX: Float) -> Unit,
    onTwoFingerHorizontalSwipe: (deltaX: Float) -> Unit,
    onHorizontalStep: (direction: Int) -> Unit,
    onTrackpadTap: () -> Unit,
    onTouchpadHoldStart: () -> Unit = {},
    onTouchpadHoldEnd: () -> Unit = {},
    onMouseButtonClick: (MouseButton) -> Unit,
    onMouseButtonDown: (MouseButton) -> Unit,
    onMouseButtonUp: (MouseButton) -> Unit,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onRecentAppsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLockedForReceiver = settings.appRole == AppRole.RECEIVER
    ReceiverLockedControlContainer(
        isLockedForReceiver = isLockedForReceiver,
        modifier = modifier.fillMaxSize()
    ) {
    if (isTabletLayout) {
        val railOnLeft = settings.scrollSide == ScrollSide.LEFT
        val showVerticalRail = settings.scrollSide != ScrollSide.HIDDEN
        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 6.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // LEFT PANE on Tablet/Landscape: Mouse click tiles + Nav Dock
            Column(
                modifier = Modifier
                    .weight(0.64f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MorphableMouseButtonTile(
                        title = "SOL TIK",
                        compactSubtitle = "Seç / Sürükle",
                        expandedSubtitle = "SOL TIKLAMA",
                        morphFraction = 0.65f,
                        accentColor = TabRemoteTheme.palette.primary,
                        onClick = { onMouseButtonClick(MouseButton.LEFT) },
                        onDown = { onMouseButtonDown(MouseButton.LEFT) },
                        onUp = { onMouseButtonUp(MouseButton.LEFT) },
                        modifier = Modifier
                            .weight(1.25f)
                            .fillMaxWidth()
                            .testTag("mouse_btn_left")
                    )

                    if (settings.showMiddleClick) {
                        MorphableMouseButtonTile(
                            title = "ORTA",
                            compactSubtitle = "Tekerlek",
                            expandedSubtitle = "ORTA TIK",
                            morphFraction = 0.35f,
                            accentColor = TabRemoteTheme.palette.accent,
                            onClick = { onMouseButtonClick(MouseButton.MIDDLE) },
                            onDown = { onMouseButtonDown(MouseButton.MIDDLE) },
                            onUp = { onMouseButtonUp(MouseButton.MIDDLE) },
                            modifier = Modifier
                                .weight(0.75f)
                                .fillMaxWidth()
                                .testTag("mouse_btn_middle")
                        )
                    }

                    if (settings.showRightClick) {
                        MorphableMouseButtonTile(
                            title = "SAĞ TIK",
                            compactSubtitle = "Menü",
                            expandedSubtitle = "SAĞ TIKLAMA",
                            morphFraction = 0.65f,
                            accentColor = TabRemoteTheme.palette.primary,
                            onClick = { onMouseButtonClick(MouseButton.RIGHT) },
                            onDown = { onMouseButtonDown(MouseButton.RIGHT) },
                            onUp = { onMouseButtonUp(MouseButton.RIGHT) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("mouse_btn_right")
                        )
                    }
                }

                CustomizableNavigationDock(
                    settings = settings,
                    onBackClick = onBackClick,
                    onHomeClick = onHomeClick,
                    onRecentAppsClick = onRecentAppsClick,
                    onMouseButtonClick = onMouseButtonClick
                )
            }

            // RIGHT PANE on Tablet/Landscape: Touchpad surface + Vertical & Horizontal rails
            Column(
                modifier = Modifier
                    .weight(1.36f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (settings.showHorizontalScrollGutter && settings.horizontalPanPosition == HorizontalPanPosition.ABOVE_BUTTONS) {
                    UnifiedHorizontalPanRail(
                        onStep = onHorizontalStep,
                        onPan = onHorizontalPan,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (railOnLeft && showVerticalRail) {
                        UnifiedVerticalScrollRail(
                            scrollMode = settings.airMouseScrollMode,
                            onToggleScrollMode = onToggleScrollMode,
                            onScrollDrag = onScrollGutterDrag,
                            onScrollStep = onScrollStep,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }

                    TrackpadSurfacePanel(
                        settings = settings,
                        isWifiMode = isWifiMode,
                        onTrackpadDrag = onTrackpadDrag,
                        onTwoFingerVerticalScroll = onTwoFingerVerticalScroll,
                        onTwoFingerGestureEnd = onTwoFingerGestureEnd,
                        onClassicPinchZoomStep = onClassicPinchZoomStep,
                        onClassicPinchZoomEnd = onClassicPinchZoomEnd,
                        onTwoFingerHorizontalSwipe = onTwoFingerHorizontalSwipe,
                        onTrackpadTap = onTrackpadTap,
                        onTouchpadHoldStart = onTouchpadHoldStart,
                        onTouchpadHoldEnd = onTouchpadHoldEnd,
                        onMouseButtonClick = onMouseButtonClick,
                        onHomeClick = onHomeClick,
                        onRecentAppsClick = onRecentAppsClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("trackpad_canvas_area")
                    )

                    if (!railOnLeft && showVerticalRail) {
                        UnifiedVerticalScrollRail(
                            scrollMode = settings.airMouseScrollMode,
                            onToggleScrollMode = onToggleScrollMode,
                            onScrollDrag = onScrollGutterDrag,
                            onScrollStep = onScrollStep,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }

                if (settings.showHorizontalScrollGutter && settings.horizontalPanPosition == HorizontalPanPosition.BELOW_BUTTONS) {
                    UnifiedHorizontalPanRail(
                        onStep = onHorizontalStep,
                        onPan = onHorizontalPan,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 4.dp)
                .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (settings.showHorizontalScrollGutter && settings.horizontalPanPosition == HorizontalPanPosition.ABOVE_BUTTONS) {
                UnifiedHorizontalPanRail(
                    onStep = onHorizontalStep,
                    onPan = onHorizontalPan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 46.dp)
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (settings.scrollSide == ScrollSide.LEFT) {
                    UnifiedVerticalScrollRail(
                        scrollMode = settings.airMouseScrollMode,
                        onToggleScrollMode = onToggleScrollMode,
                        onScrollDrag = onScrollGutterDrag,
                        onScrollStep = onScrollStep,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(end = 6.dp)
                    )
                }

                TrackpadSurfacePanel(
                    settings = settings,
                    isWifiMode = isWifiMode,
                    onTrackpadDrag = onTrackpadDrag,
                    onTwoFingerVerticalScroll = onTwoFingerVerticalScroll,
                    onTwoFingerGestureEnd = onTwoFingerGestureEnd,
                    onClassicPinchZoomStep = onClassicPinchZoomStep,
                    onClassicPinchZoomEnd = onClassicPinchZoomEnd,
                    onTwoFingerHorizontalSwipe = onTwoFingerHorizontalSwipe,
                    onTrackpadTap = onTrackpadTap,
                    onTouchpadHoldStart = onTouchpadHoldStart,
                    onTouchpadHoldEnd = onTouchpadHoldEnd,
                    onMouseButtonClick = onMouseButtonClick,
                    onHomeClick = onHomeClick,
                    onRecentAppsClick = onRecentAppsClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("trackpad_canvas_area")
                )

                if (settings.scrollSide == ScrollSide.RIGHT) {
                    UnifiedVerticalScrollRail(
                        scrollMode = settings.airMouseScrollMode,
                        onToggleScrollMode = onToggleScrollMode,
                        onScrollDrag = onScrollGutterDrag,
                        onScrollStep = onScrollStep,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(start = 6.dp)
                    )
                }
            }

            MouseButtonsBar(
                settings = settings,
                onMouseButtonClick = onMouseButtonClick,
                onMouseButtonDown = onMouseButtonDown,
                onMouseButtonUp = onMouseButtonUp
            )

            if (settings.showHorizontalScrollGutter && settings.horizontalPanPosition == HorizontalPanPosition.BELOW_BUTTONS) {
                UnifiedHorizontalPanRail(
                    onStep = onHorizontalStep,
                    onPan = onHorizontalPan,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            CustomizableNavigationDock(
                settings = settings,
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
                onRecentAppsClick = onRecentAppsClick,
                onMouseButtonClick = onMouseButtonClick
            )

            Spacer(modifier = Modifier.height(74.dp))
        }
    }
    }
}

@Composable
internal fun TrackpadSurfacePanel(
    settings: RemoteSettings,
    isWifiMode: Boolean,
    onTrackpadDrag: (dx: Float, dy: Float) -> Unit,
    onTwoFingerVerticalScroll: (deltaY: Float) -> Unit,
    onTwoFingerGestureEnd: () -> Unit = {},
    onClassicPinchZoomStep: (pinchDelta: Float) -> Unit,
    onClassicPinchZoomEnd: () -> Unit,
    onTwoFingerHorizontalSwipe: (deltaX: Float) -> Unit,
    onTrackpadTap: () -> Unit,
    onTouchpadHoldStart: () -> Unit = {},
    onTouchpadHoldEnd: () -> Unit = {},
    onMouseButtonClick: (MouseButton) -> Unit,
    onHomeClick: () -> Unit,
    onRecentAppsClick: () -> Unit,
    surfaceShape: Shape = RoundedCornerShape(16.dp),
    topContentPadding: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    val dotColor = palette.trackpadGridDot
    val glowColors = listOf(palette.primary, palette.accent, palette.secondary, Color.White)

    val coroutineScope = rememberCoroutineScope()
    var activeTouchPositions by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var isHoldingGesture by remember { mutableStateOf(false) }
    val touchRippleAlpha = remember { Animatable(0f) }

    val latestOnHoldStart by rememberUpdatedState(onTouchpadHoldStart)
    val latestOnHoldEnd by rememberUpdatedState(onTouchpadHoldEnd)
    val latestOnTwoFingerEnd by rememberUpdatedState(onTwoFingerGestureEnd)

    Box(
        modifier = modifier
            .clip(surfaceShape)
            .background(palette.trackpadSurface)
            .border(
                width = 1.2.dp,
                color = if (isHoldingGesture) palette.primary else palette.borderSubtle,
                shape = surfaceShape
            )
            .pointerInput(settings.tapToClick, settings.pointerSensitivity) {
                awaitPointerEventScope {
                    var lastSingleTapUpMs = 0L

                    while (true) {
                        val downEvent = awaitPointerEvent()
                        val downPointers = downEvent.changes.filter { it.pressed }
                        if (downPointers.isEmpty()) continue

                        var pinchZoomActive = false
                        var twoFingerScrollActive = false
                        var holdActivated = false
                        val startTime = System.currentTimeMillis()
                        val isSecondTapFast = (startTime - lastSingleTapUpMs) in 1L..320L && downPointers.size == 1
                        var maxPointers = downPointers.size
                        var prevPointerCount = downPointers.size
                        var totalMovement = 0f
                        var lastPositions = downPointers.associate { it.id to it.position }
                        var threeFingerSwipeTriggered = false
                        var threeFingerAccumDy = 0f

                        // Double-tap + drag starts live hold mode immediately
                        if (isSecondTapFast) {
                            lastSingleTapUpMs = 0L
                            holdActivated = true
                            isHoldingGesture = true
                            latestOnHoldStart()
                        }

                        // Stationary long press (430ms) triggers press-and-hold without racing normal 360ms taps
                        val longHoldJob = if (!holdActivated && downPointers.size == 1) {
                            coroutineScope.launch {
                                delay(430L)
                                if (!holdActivated && maxPointers == 1 && totalMovement < 18f) {
                                    holdActivated = true
                                    isHoldingGesture = true
                                    latestOnHoldStart()
                                }
                            }
                        } else null

                        activeTouchPositions = downPointers.map { it.position }
                        coroutineScope.launch { touchRippleAlpha.snapTo(0.45f) }

                        while (true) {
                            val moveEvent = awaitPointerEvent()
                            val currentPressed = moveEvent.changes.filter { it.pressed }

                            if (currentPressed.isEmpty()) {
                                longHoldJob?.cancel()
                                val duration = System.currentTimeMillis() - startTime
                                if (pinchZoomActive) {
                                    pinchZoomActive = false
                                    onClassicPinchZoomEnd()
                                }
                                if (twoFingerScrollActive) {
                                    twoFingerScrollActive = false
                                    latestOnTwoFingerEnd()
                                }
                                if (holdActivated) {
                                    holdActivated = false
                                    isHoldingGesture = false
                                    latestOnHoldEnd()
                                } else if (duration <= 360L && totalMovement < 22f) {
                                    if (maxPointers == 1) {
                                        lastSingleTapUpMs = System.currentTimeMillis()
                                        onTrackpadTap()
                                    } else if (maxPointers == 2) {
                                        onMouseButtonClick(MouseButton.RIGHT)
                                    }
                                }

                                if (!holdActivated && duration <= 360L && totalMovement < 22f && maxPointers == 1 && settings.tapToClick) {
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
                                longHoldJob?.cancel()
                            }
                            activeTouchPositions = currentPressed.map { it.position }

                            // Reset coordinate baselines cleanly when finger count transitions (prevents 2->1 lift cursor jump)
                            if (currentPressed.size != prevPointerCount) {
                                if (prevPointerCount >= 2 && currentPressed.size == 1) {
                                    if (pinchZoomActive) {
                                        pinchZoomActive = false
                                        onClassicPinchZoomEnd()
                                    }
                                    if (twoFingerScrollActive) {
                                        twoFingerScrollActive = false
                                        latestOnTwoFingerEnd()
                                    }
                                }
                                prevPointerCount = currentPressed.size
                                lastPositions = currentPressed.associate { it.id to it.position }
                                currentPressed.forEach { it.consume() }
                                continue
                            }

                            if (currentPressed.size == 1) {
                                if (pinchZoomActive) {
                                    pinchZoomActive = false
                                    onClassicPinchZoomEnd()
                                }
                                if (twoFingerScrollActive) {
                                    twoFingerScrollActive = false
                                    latestOnTwoFingerEnd()
                                }
                                val change = currentPressed[0]
                                val prevPos = lastPositions[change.id] ?: change.position
                                val dx = change.position.x - prevPos.x
                                val dy = change.position.y - prevPos.y
                                totalMovement += abs(dx) + abs(dy)
                                if (totalMovement > 18f && !holdActivated) {
                                    longHoldJob?.cancel()
                                }
                                change.consume()
                                onTrackpadDrag(dx, dy)
                            } else if (currentPressed.size == 2) {
                                longHoldJob?.cancel()
                                if (holdActivated) {
                                    holdActivated = false
                                    isHoldingGesture = false
                                    latestOnHoldEnd()
                                }
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

                                if (abs(pinchDelta) > 4.2f && abs(pinchDelta) > (abs(avgDx) + abs(avgDy)) * 0.85f) {
                                    if (twoFingerScrollActive) {
                                        twoFingerScrollActive = false
                                        latestOnTwoFingerEnd()
                                    }
                                    pinchZoomActive = true
                                    onClassicPinchZoomStep(pinchDelta)
                                } else if (abs(avgDx) > abs(avgDy) * 1.15f && abs(avgDx) > 2.2f) {
                                    twoFingerScrollActive = true
                                    onTwoFingerHorizontalSwipe(avgDx)
                                } else if (abs(avgDy) > 1.6f) {
                                    twoFingerScrollActive = true
                                    onTwoFingerVerticalScroll(avgDy)
                                }
                            } else if (currentPressed.size >= 3) {
                                longHoldJob?.cancel()
                                if (pinchZoomActive) {
                                    pinchZoomActive = false
                                    onClassicPinchZoomEnd()
                                }
                                if (twoFingerScrollActive) {
                                    twoFingerScrollActive = false
                                    latestOnTwoFingerEnd()
                                }
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
                        color = dotColor,
                        radius = 1.6f,
                        center = Offset(x, y)
                    )
                    y += step
                }
                x += step
            }

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = topContentPadding, bottom = 12.dp)
        ) {
            Text(
                text = when {
                    isHoldingGesture -> "PARMAK BASILI TUTULUYOR / SÜRÜKLENİYOR"
                    isWifiMode -> "WI-FI AKILLI İMLEÇ"
                    else -> "TOUCHPAD (CTRL + SCROLL ZOOM)"
                },
                color = if (isHoldingGesture) palette.primary else palette.textMuted.copy(alpha = 0.4f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = if (isWifiMode) {
                    "1 Parmak: İmleç • Basılı Tut / Çift Tık+Sürükle: Canlı Sürükle\n2 Parmak: Kesintisiz Kaydır & Gerçek Pinch Zoom"
                } else {
                    "1 Parmak: İmleç • Basılı Tut / Çift Tık+Sürükle: Seç & Sürükle\n2 Parmak: Kaydır & Ctrl+Zoom • 3 Parmak: Ana Ekran / Son"
                },
                color = palette.textMuted.copy(alpha = 0.56f),
                fontSize = 9.5.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Clean Navigation Dock (Geri, Ana Ekran, Son, F1, F2).
 * Customization & ordering are cleanly managed in Settings -> Touchpad, Hizalama & Dock.
 */
@Composable
internal fun CustomizableNavigationDock(
    settings: RemoteSettings,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onRecentAppsClick: () -> Unit,
    onMouseButtonClick: (MouseButton) -> Unit,
    modifier: Modifier = Modifier
) {
    val allowedItems = remember(settings.navDockOrder, settings.showFunctionButtons) {
        settings.navDockOrder.filter { item ->
            settings.showFunctionButtons || (item != NavDockItemId.F1 && item != NavDockItemId.F2)
        }
    }

    val visibleItems = remember(allowedItems, settings.hiddenNavDockItems) {
        allowedItems.filter { it !in settings.hiddenNavDockItems }
    }

    val palette = TabRemoteTheme.palette
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(12.dp))
            .testTag("customizable_navigation_dock")
    ) {
        ExpressiveDockRow(
            items = visibleItems,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalSpacing = 4.dp,
            itemWeight = { item ->
                if (item == NavDockItemId.F1 || item == NavDockItemId.F2) 0.62f else 1f
            }
        ) { item, _, isPressed, _, _, onPressChange ->
            when (item) {
                NavDockItemId.F1 -> FunctionBadgeTile(
                    label = "F1",
                    isPressed = isPressed,
                    onPressChange = onPressChange,
                    onClick = { onMouseButtonClick(MouseButton.BUTTON_4) },
                    modifier = Modifier.fillMaxSize().testTag("dock_btn_f1")
                )
                NavDockItemId.F2 -> FunctionBadgeTile(
                    label = "F2",
                    isPressed = isPressed,
                    onPressChange = onPressChange,
                    onClick = { onMouseButtonClick(MouseButton.BUTTON_5) },
                    modifier = Modifier.fillMaxSize().testTag("dock_btn_f2")
                )
                NavDockItemId.BACK -> NavDockTile(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    label = "GERİ",
                    isPressed = isPressed,
                    onPressChange = onPressChange,
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxSize().testTag("dock_btn_back")
                )
                NavDockItemId.HOME -> NavDockTile(
                    icon = Icons.Default.Home,
                    label = "ANA EKRAN",
                    isPressed = isPressed,
                    onPressChange = onPressChange,
                    onClick = onHomeClick,
                    modifier = Modifier.fillMaxSize().testTag("dock_btn_home")
                )
                NavDockItemId.RECENT -> NavDockTile(
                    icon = Icons.Default.Layers,
                    label = "SON",
                    isPressed = isPressed,
                    onPressChange = onPressChange,
                    onClick = onRecentAppsClick,
                    modifier = Modifier.fillMaxSize().testTag("dock_btn_recent")
                )
            }
        }
    }
}

@Composable
private fun FunctionBadgeTile(
    label: String,
    isPressed: Boolean,
    onPressChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    val bg by animateColorAsState(
        targetValue = if (isPressed) palette.primary else palette.surfaceContainer,
        animationSpec = tween(120),
        label = "fn_tile_bg"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                1.dp,
                if (isPressed) palette.primary else palette.borderSubtle,
                RoundedCornerShape(8.dp)
            )
            .expressivePressDetect(
                key = label,
                onPressChange = onPressChange,
                onTap = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) palette.onAccent else palette.textPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            softWrap = false
        )
    }
}

@Composable
private fun NavDockTile(
    icon: ImageVector,
    label: String,
    isPressed: Boolean,
    onPressChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    val bg by animateColorAsState(
        targetValue = if (isPressed) palette.primary else palette.surfaceContainer,
        animationSpec = tween(120),
        label = "nav_tile_bg"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                1.dp,
                if (isPressed) palette.primary else palette.borderSubtle,
                RoundedCornerShape(8.dp)
            )
            .expressivePressDetect(
                key = label,
                onPressChange = onPressChange,
                onTap = onClick
            )
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isPressed) palette.onAccent else palette.textPrimary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            color = if (isPressed) palette.onAccent else palette.textPrimary,
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
    val palette = TabRemoteTheme.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MouseButtonTile(
            title = "SOL TIK",
            subtitle = "Seç / Sürükle",
            accentColor = palette.primary,
            onClick = { onMouseButtonClick(MouseButton.LEFT) },
            onDown = { onMouseButtonDown(MouseButton.LEFT) },
            onUp = { onMouseButtonUp(MouseButton.LEFT) },
            modifier = Modifier
                .weight(1.3f)
                .testTag("mouse_btn_left")
        )

        if (settings.showMiddleClick) {
            MouseButtonTile(
                title = "ORTA",
                subtitle = "Tekerlek Tık",
                accentColor = palette.accent,
                onClick = { onMouseButtonClick(MouseButton.MIDDLE) },
                onDown = { onMouseButtonDown(MouseButton.MIDDLE) },
                onUp = { onMouseButtonUp(MouseButton.MIDDLE) },
                modifier = Modifier
                    .weight(0.8f)
                    .testTag("mouse_btn_middle")
            )
        }

        if (settings.showRightClick) {
            MouseButtonTile(
                title = "SAĞ TIK",
                subtitle = "Menü",
                accentColor = palette.primary,
                onClick = { onMouseButtonClick(MouseButton.RIGHT) },
                onDown = { onMouseButtonDown(MouseButton.RIGHT) },
                onUp = { onMouseButtonUp(MouseButton.RIGHT) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("mouse_btn_right")
            )
        }
    }
}

@Composable
internal fun MorphableMouseButtonTile(
    title: String,
    compactSubtitle: String,
    expandedSubtitle: String,
    morphFraction: Float,
    accentColor: Color,
    onClick: () -> Unit,
    onDown: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    var isPressed by remember { mutableStateOf(false) }
    val cornerDp = lerp(12f, 16f, morphFraction).dp
    val titleSizeSp = lerp(13f, 16f, morphFraction).sp

    Surface(
        color = if (isPressed) palette.primary else palette.surface,
        shape = RoundedCornerShape(cornerDp),
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(cornerDp))
            .border(
                1.2.dp,
                if (isPressed) accentColor else palette.borderSubtle,
                RoundedCornerShape(cornerDp)
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
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (morphFraction > 0.25f) {
                Box(
                    modifier = Modifier
                        .size((10f * morphFraction).dp)
                        .background(
                            if (isPressed) palette.onAccent else palette.borderElevated,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.height((6f * morphFraction).dp))
            }
            Text(
                text = title,
                color = if (isPressed) palette.onAccent else palette.textPrimary,
                fontSize = titleSizeSp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp
            )
            Text(
                text = if (morphFraction > 0.45f) expandedSubtitle else compactSubtitle,
                color = if (isPressed) palette.onAccent.copy(alpha = 0.85f) else palette.textSecondary,
                fontSize = 9.5.sp,
                maxLines = 1
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
    MorphableMouseButtonTile(
        title = title,
        compactSubtitle = subtitle,
        expandedSubtitle = subtitle,
        morphFraction = 0f,
        accentColor = accentColor,
        onClick = onClick,
        onDown = onDown,
        onUp = onUp,
        modifier = modifier
    )
}
