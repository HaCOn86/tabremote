package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.AppRole
import com.example.model.HorizontalPanPosition
import com.example.model.MouseButton
import com.example.model.RemoteSettings
import com.example.model.ScrollSide
import com.example.sensor.AirMouseTelemetry
import com.example.ui.components.ReceiverLockedControlContainer
import com.example.ui.components.UnifiedHorizontalPanRail
import com.example.ui.components.UnifiedVerticalScrollRail
import com.example.ui.theme.TabRemoteTheme

@Composable
private fun rememberTopLeftNotchedShape(
    notchWidth: Dp = 148.dp,
    notchHeight: Dp = 42.dp,
    outerCorner: Dp = 16.dp,
    innerCorner: Dp = 12.dp
): Shape {
    val density = LocalDensity.current
    return remember(notchWidth, notchHeight, outerCorner, innerCorner, density) {
        val nwRaw = with(density) { notchWidth.toPx() }
        val nhRaw = with(density) { notchHeight.toPx() }
        val rRaw = with(density) { outerCorner.toPx() }
        val nrRaw = with(density) { innerCorner.toPx() }

        GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val r = rRaw.coerceAtMost(minOf(w, h) * 0.22f)
            val nr = nrRaw.coerceAtMost(minOf(w, h) * 0.18f)
            val nw = nwRaw.coerceIn(r + nr + 4f, w * 0.65f)
            val nh = nhRaw.coerceIn(r + nr + 4f, h * 0.65f)

            moveTo(nw + r, 0f)
            lineTo(w - r, 0f)
            arcTo(Rect(w - 2f * r, 0f, w, 2f * r), -90f, 90f, false)
            lineTo(w, h - r)
            arcTo(Rect(w - 2f * r, h - 2f * r, w, h), 0f, 90f, false)
            lineTo(r, h)
            arcTo(Rect(0f, h - 2f * r, 2f * r, h), 90f, 90f, false)
            lineTo(0f, nh + r)
            arcTo(Rect(0f, nh, 2f * r, nh + 2f * r), 180f, 90f, false)
            lineTo(nw - nr, nh)
            arcTo(Rect(nw - 2f * nr, nh - 2f * nr, nw, nh), 90f, -90f, false)
            lineTo(nw, r)
            arcTo(Rect(nw, 0f, nw + 2f * r, 2f * r), 180f, 90f, false)
            close()
        }
    }
}

@Composable
private fun AirMouseCutoutSwitch(
    airMouseActive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    val pillBg by animateColorAsState(
        targetValue = if (airMouseActive) palette.primary.copy(alpha = 0.16f) else palette.surfaceContainer,
        animationSpec = tween(220),
        label = "air_switch_pill_bg"
    )
    val trackColor by animateColorAsState(
        targetValue = if (airMouseActive) palette.primary else palette.controlThumb,
        animationSpec = tween(220),
        label = "air_switch_track"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (airMouseActive) 14.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "air_switch_thumb_offset"
    )

    Surface(
        color = pillBg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .width(140.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (airMouseActive) palette.primary else palette.borderElevated,
                RoundedCornerShape(12.dp)
            )
            .semantics {
                contentDescription = if (airMouseActive) "Air Mouse açık, kapatmak için dokunun" else "Air Mouse kapalı, açmak için dokunun"
            }
            .clickable(role = Role.Switch) { onToggle() }
            .testTag("unified_air_mouse_toggle")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Air Mouse",
                color = if (airMouseActive) palette.primary else palette.textPrimary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                softWrap = false
            )

            Box(
                modifier = Modifier
                    .width(34.dp)
                    .height(20.dp)
                    .clip(CircleShape)
                    .background(trackColor)
                    .border(
                        1.dp,
                        if (airMouseActive) palette.primary else palette.borderElevated,
                        CircleShape
                    )
                    .padding(horizontal = 3.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffset)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (airMouseActive) palette.onAccent else palette.textSecondary)
                )
            }
        }
    }
}

@Composable
fun UnifiedControlScreen(
    settings: RemoteSettings,
    isWifiMode: Boolean,
    isTabletLayout: Boolean = false,
    airMouseExpanded: Boolean,
    airMouseActive: Boolean,
    telemetry: AirMouseTelemetry,
    isGyroSupported: Boolean,
    onToggleAirMouseMode: () -> Unit,
    onTrackpadDrag: (dx: Float, dy: Float) -> Unit,
    onScrollGutterDrag: (deltaY: Float) -> Unit,
    onTwoFingerVerticalScroll: (deltaY: Float) -> Unit = onScrollGutterDrag,
    onTwoFingerGestureEnd: () -> Unit = {},
    onClassicPinchZoomStep: (pinchDelta: Float) -> Unit = {},
    onClassicPinchZoomEnd: () -> Unit = {},
    onScrollStep: (ticks: Int) -> Unit,
    onHorizontalPan: (deltaX: Float) -> Unit,
    onTwoFingerHorizontalSwipe: (deltaX: Float) -> Unit,
    onHorizontalStep: (direction: Int) -> Unit,
    onTrackpadTap: () -> Unit,
    onTouchpadHoldStart: () -> Unit = {},
    onTouchpadHoldEnd: () -> Unit = {},
    onMouseButtonClick: (MouseButton) -> Unit,
    onMouseButtonDown: (MouseButton) -> Unit,
    onMouseButtonUp: (MouseButton) -> Unit,
    onUpdateSensitivity: (Float) -> Unit,
    onUpdateDeadband: (Float) -> Unit,
    onToggleScrollMode: () -> Unit,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onRecentAppsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    var showAirTuning by remember { mutableStateOf(false) }

    val notchedShape = rememberTopLeftNotchedShape()

    val morphFraction by animateFloatAsState(
        targetValue = if (airMouseExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.84f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "unified_control_morph_fraction"
    )

    val railOnLeft = settings.scrollSide == ScrollSide.LEFT
    val showVerticalRail = settings.scrollSide != ScrollSide.HIDDEN || airMouseExpanded

    ReceiverLockedControlContainer(
        isLockedForReceiver = settings.appRole == AppRole.RECEIVER,
        modifier = modifier.fillMaxSize()
    ) {
        if (isTabletLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 6.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // LEFT PANE on Tablet/Landscape: Mouse buttons + Nav Dock (ergonomic left-thumb reach)
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
                            expandedSubtitle = "BİRİNCİL TIKLAMA",
                            morphFraction = morphFraction.coerceAtLeast(0.55f),
                            accentColor = palette.primary,
                            onClick = { onMouseButtonClick(MouseButton.LEFT) },
                            onDown = { onMouseButtonDown(MouseButton.LEFT) },
                            onUp = { onMouseButtonUp(MouseButton.LEFT) },
                            modifier = Modifier
                                .weight(1.3f)
                                .fillMaxWidth()
                                .testTag(if (airMouseExpanded) "air_mouse_left_btn" else "mouse_btn_left")
                        )

                        if (settings.showMiddleClick && morphFraction < 0.7f) {
                            MorphableMouseButtonTile(
                                title = "ORTA",
                                compactSubtitle = "Tekerlek",
                                expandedSubtitle = "ORTA TIK",
                                morphFraction = morphFraction,
                                accentColor = palette.accent,
                                onClick = { onMouseButtonClick(MouseButton.MIDDLE) },
                                onDown = { onMouseButtonDown(MouseButton.MIDDLE) },
                                onUp = { onMouseButtonUp(MouseButton.MIDDLE) },
                                modifier = Modifier
                                    .weight(0.75f)
                                    .fillMaxWidth()
                                    .alpha((1f - morphFraction * 1.4f).coerceIn(0f, 1f))
                                    .testTag("mouse_btn_middle")
                            )
                        }

                        if (settings.showRightClick || morphFraction > 0.05f) {
                            MorphableMouseButtonTile(
                                title = "SAĞ TIK",
                                compactSubtitle = "Menü",
                                expandedSubtitle = "BAĞLAM MENÜSÜ",
                                morphFraction = morphFraction.coerceAtLeast(0.55f),
                                accentColor = palette.accent,
                                onClick = { onMouseButtonClick(MouseButton.RIGHT) },
                                onDown = { onMouseButtonDown(MouseButton.RIGHT) },
                                onUp = { onMouseButtonUp(MouseButton.RIGHT) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .testTag(if (airMouseExpanded) "air_mouse_right_btn" else "mouse_btn_right")
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

                // RIGHT PANE on Tablet/Landscape: Touchpad & Air Mouse Radar Arena + Scroll & Horizontal Pan Rails
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

                    AnimatedVisibility(
                        visible = airMouseExpanded && showAirTuning,
                        enter = expandVertically(tween(200)) + fadeIn(tween(140)),
                        exit = shrinkVertically(tween(180)) + fadeOut(tween(120))
                    ) {
                        AirTuningPanel(
                            settings = settings,
                            onUpdateSensitivity = onUpdateSensitivity,
                            onUpdateDeadband = onUpdateDeadband
                        )
                    }

                    if (airMouseExpanded && !isGyroSupported) {
                        GyroMissingBanner()
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

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("unified_control_morph_arena")
                        ) {
                            if (morphFraction < 0.99f) {
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
                                    surfaceShape = notchedShape,
                                    topContentPadding = 40.dp,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha((1f - morphFraction * 1.15f).coerceIn(0f, 1f))
                                        .testTag("unified_trackpad_area")
                                )
                            }

                            if (morphFraction > 0.01f) {
                                GyroscopeRadarCard(
                                    airMouseActive = airMouseActive,
                                    telemetry = telemetry,
                                    showTuning = showAirTuning,
                                    onToggleTuning = { showAirTuning = !showAirTuning },
                                    cardShape = notchedShape,
                                    isNotchedTopLeft = true,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(((morphFraction - 0.1f) / 0.9f).coerceIn(0f, 1f))
                                )
                            }

                            AirMouseCutoutSwitch(
                                airMouseActive = airMouseExpanded,
                                onToggle = onToggleAirMouseMode,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .zIndex(5f)
                            )
                        }

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

                AnimatedVisibility(
                    visible = airMouseExpanded && showAirTuning,
                    enter = expandVertically(tween(200)) + fadeIn(tween(140)),
                    exit = shrinkVertically(tween(180)) + fadeOut(tween(120))
                ) {
                    AirTuningPanel(
                        settings = settings,
                        onUpdateSensitivity = onUpdateSensitivity,
                        onUpdateDeadband = onUpdateDeadband
                    )
                }

                if (airMouseExpanded && !isGyroSupported) {
                    GyroMissingBanner()
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("unified_control_morph_arena")
                ) {
                    if (railOnLeft && showVerticalRail) {
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

                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        val totalHeight = maxHeight
                        val middleGap = 6.dp

                        val airUpperTargetHeight = 108.dp.coerceAtMost((totalHeight * 0.36f).coerceAtLeast(92.dp))
                        val touchpadBottomBarHeight = 52.dp
                        val touchpadUpperHeight = (totalHeight - touchpadBottomBarHeight - middleGap).coerceAtLeast(140.dp)

                        val upperHeight = lerp(touchpadUpperHeight, airUpperTargetHeight, morphFraction)
                        val lowerHeight = (totalHeight - upperHeight - middleGap).coerceAtLeast(touchpadBottomBarHeight)
                        val lowerTopY = upperHeight + middleGap

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                                .height(upperHeight)
                        ) {
                            if (morphFraction < 0.99f) {
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
                                    surfaceShape = notchedShape,
                                    topContentPadding = 40.dp,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha((1f - morphFraction * 1.15f).coerceIn(0f, 1f))
                                        .testTag("unified_trackpad_area")
                                )
                            }

                            if (morphFraction > 0.01f) {
                                GyroscopeRadarCard(
                                    airMouseActive = airMouseActive,
                                    telemetry = telemetry,
                                    showTuning = showAirTuning,
                                    onToggleTuning = { showAirTuning = !showAirTuning },
                                    cardShape = notchedShape,
                                    isNotchedTopLeft = true,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(((morphFraction - 0.1f) / 0.9f).coerceIn(0f, 1f))
                                )
                            }

                            AirMouseCutoutSwitch(
                                airMouseActive = airMouseExpanded,
                                onToggle = onToggleAirMouseMode,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .zIndex(5f)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(y = lowerTopY)
                                .fillMaxWidth()
                                .height(lowerHeight),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MorphableMouseButtonTile(
                                title = "SOL TIK",
                                compactSubtitle = "Seç / Sürükle",
                                expandedSubtitle = "BİRİNCİL TIKLAMA",
                                morphFraction = morphFraction,
                                accentColor = palette.primary,
                                onClick = { onMouseButtonClick(MouseButton.LEFT) },
                                onDown = { onMouseButtonDown(MouseButton.LEFT) },
                                onUp = { onMouseButtonUp(MouseButton.LEFT) },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag(if (airMouseExpanded) "air_mouse_left_btn" else "mouse_btn_left")
                            )

                            if (settings.showMiddleClick && morphFraction < 0.7f) {
                                val middleWeight = ((1f - morphFraction / 0.7f) * 0.8f).coerceAtLeast(0.01f)
                                MorphableMouseButtonTile(
                                    title = "ORTA",
                                    compactSubtitle = "Tekerlek",
                                    expandedSubtitle = "ORTA TIK",
                                    morphFraction = morphFraction,
                                    accentColor = palette.accent,
                                    onClick = { onMouseButtonClick(MouseButton.MIDDLE) },
                                    onDown = { onMouseButtonDown(MouseButton.MIDDLE) },
                                    onUp = { onMouseButtonUp(MouseButton.MIDDLE) },
                                    modifier = Modifier
                                        .weight(middleWeight)
                                        .alpha((1f - morphFraction * 1.4f).coerceIn(0f, 1f))
                                        .testTag("mouse_btn_middle")
                                )
                            }

                            if (settings.showRightClick || morphFraction > 0.05f) {
                                MorphableMouseButtonTile(
                                    title = "SAĞ TIK",
                                    compactSubtitle = "Menü",
                                    expandedSubtitle = "BAĞLAM MENÜSÜ",
                                    morphFraction = morphFraction,
                                    accentColor = palette.accent,
                                    onClick = { onMouseButtonClick(MouseButton.RIGHT) },
                                    onDown = { onMouseButtonDown(MouseButton.RIGHT) },
                                    onUp = { onMouseButtonUp(MouseButton.RIGHT) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(if (airMouseExpanded) "air_mouse_right_btn" else "mouse_btn_right")
                                )
                            }
                        }
                    }

                    if (!railOnLeft && showVerticalRail) {
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

