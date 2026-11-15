package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.example.bluetooth.TouchContact
import com.example.model.MouseButton
import com.example.model.NavDockItemId
import com.example.model.RemoteSettings
import com.example.model.ScrollSide
import com.example.sensor.AirMouseTelemetry
import com.example.ui.components.UnifiedHorizontalPanRail
import com.example.ui.components.UnifiedVerticalScrollRail
import com.example.ui.theme.RubyAccent
import com.example.ui.theme.RubyPrimary
import com.example.ui.theme.SurfaceCharcoal

@Composable
fun UnifiedControlScreen(
    settings: RemoteSettings,
    isWifiMode: Boolean,
    airMouseExpanded: Boolean,
    airMouseActive: Boolean,
    telemetry: AirMouseTelemetry,
    isGyroSupported: Boolean,
    onToggleAirMouseMode: () -> Unit,
    onTouchpadContacts: (contacts: List<TouchContact>, clickpadPressed: Boolean) -> Unit,
    onTrackpadDrag: (dx: Float, dy: Float) -> Unit,
    onScrollGutterDrag: (deltaY: Float) -> Unit,
    onTwoFingerVerticalScroll: (deltaY: Float) -> Unit = onScrollGutterDrag,
    onClassicPinchZoomStep: (pinchDelta: Float) -> Unit = {},
    onClassicPinchZoomEnd: () -> Unit = {},
    onScrollStep: (ticks: Int) -> Unit,
    onHorizontalPan: (deltaX: Float) -> Unit,
    onTwoFingerHorizontalSwipe: (deltaX: Float) -> Unit,
    onHorizontalStep: (direction: Int) -> Unit,
    onTrackpadTap: () -> Unit,
    onMouseButtonClick: (MouseButton) -> Unit,
    onMouseButtonDown: (MouseButton) -> Unit,
    onMouseButtonUp: (MouseButton) -> Unit,
    onUpdateSensitivity: (Float) -> Unit,
    onUpdateDeadband: (Float) -> Unit,
    onToggleScrollMode: () -> Unit,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onRecentAppsClick: () -> Unit,
    onMoveNavDockItem: (index: Int, delta: Int) -> Unit,
    onToggleNavDockHidden: (NavDockItemId) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAirTuning by remember { mutableStateOf(false) }

    val morphFraction by animateFloatAsState(
        targetValue = if (airMouseExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.84f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "unified_control_morph_fraction"
    )

    val morphPillBg by animateColorAsState(
        targetValue = if (airMouseExpanded) RubyPrimary else SurfaceCharcoal,
        animationSpec = tween(220),
        label = "air_morph_bg"
    )
    val morphPillScale by animateFloatAsState(
        targetValue = if (airMouseExpanded) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "air_morph_scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 4.dp)
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Top Header Bar: Unified Notched Horizontal Pan Rail + Morph Mode Toggle Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 46.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (settings.showHorizontalScrollGutter) {
                UnifiedHorizontalPanRail(
                    onStep = onHorizontalStep,
                    onPan = onHorizontalPan,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Surface(
                color = morphPillBg,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(38.dp)
                    .scale(morphPillScale)
                    .border(
                        1.2.dp,
                        if (airMouseExpanded) RubyPrimary else RubyPrimary.copy(alpha = 0.6f),
                        RoundedCornerShape(10.dp)
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggleAirMouseMode() }
                    .testTag("unified_air_mouse_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (airMouseExpanded) Icons.Default.TouchApp else Icons.Default.Sensors,
                        contentDescription = if (airMouseExpanded) "Touchpad Moduna Dön" else "Air Mouse Moduna Geç",
                        tint = if (airMouseExpanded) Color.White else RubyPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (airMouseExpanded) "TOUCHPAD" else "AIR MOUSE",
                        color = if (airMouseExpanded) Color.White else RubyPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        softWrap = false
                    )
                }
            }
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

        AnimatedVisibility(
            visible = airMouseExpanded,
            enter = expandVertically(tween(200)) + fadeIn(tween(160)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(130))
        ) {
            if (!isGyroSupported) {
                GyroMissingBanner()
            } else {
                AirMouseVolumeActionsHintBar(settings = settings)
            }
        }

        // Persistent Morph Arena: top Touchpad surface shrinks into Gyro Radar while SOL TIK / SAĞ TIK
        // remain mounted and stretch upward into thumb paddles; UnifiedVerticalScrollRail glides alongside!
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("unified_control_morph_arena")
        ) {
            val totalHeight = maxHeight
            val middleGap = 6.dp
            val railWidth = 52.dp
            val railGap = 6.dp
            val railReservedWidth = railWidth + railGap

            val hasTouchpadRail = settings.scrollSide != ScrollSide.HIDDEN
            val railOnLeft = settings.scrollSide == ScrollSide.LEFT

            val airUpperTargetHeight = 108.dp.coerceAtMost((totalHeight * 0.36f).coerceAtLeast(88.dp))
            val touchpadBottomBarHeight = 52.dp
            val touchpadUpperHeight = (totalHeight - touchpadBottomBarHeight - middleGap).coerceAtLeast(140.dp)

            val upperHeight = lerp(touchpadUpperHeight, airUpperTargetHeight, morphFraction)
            val lowerHeight = (totalHeight - upperHeight - middleGap).coerceAtLeast(touchpadBottomBarHeight)
            val lowerTopY = upperHeight + middleGap

            val upperRailInset = if (hasTouchpadRail) lerp(railReservedWidth, 0.dp, morphFraction) else 0.dp
            val lowerRailInset = lerp(0.dp, railReservedWidth, morphFraction)

            val railTopY = lerp(0.dp, lowerTopY, morphFraction)
            val railHeight = if (hasTouchpadRail) {
                lerp(upperHeight, lowerHeight, morphFraction)
            } else {
                lowerHeight
            }
            val railAlpha = if (hasTouchpadRail) 1f else morphFraction

            // 1. Upper Surface Container (TrackpadSurfacePanel morphs smoothly into GyroscopeRadarCard)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(
                        start = if (railOnLeft) upperRailInset else 0.dp,
                        end = if (!railOnLeft) upperRailInset else 0.dp
                    )
                    .height(upperHeight)
            ) {
                if (morphFraction < 0.99f) {
                    TrackpadSurfacePanel(
                        settings = settings,
                        isWifiMode = isWifiMode,
                        onTouchpadContacts = onTouchpadContacts,
                        onTrackpadDrag = onTrackpadDrag,
                        onTwoFingerVerticalScroll = onTwoFingerVerticalScroll,
                        onClassicPinchZoomStep = onClassicPinchZoomStep,
                        onClassicPinchZoomEnd = onClassicPinchZoomEnd,
                        onTwoFingerHorizontalSwipe = onTwoFingerHorizontalSwipe,
                        onTrackpadTap = onTrackpadTap,
                        onMouseButtonClick = onMouseButtonClick,
                        onHomeClick = onHomeClick,
                        onRecentAppsClick = onRecentAppsClick,
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
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(((morphFraction - 0.1f) / 0.9f).coerceIn(0f, 1f))
                    )
                }
            }

            // 2. Persistent Click Tiles (SOL TIK, optional ORTA, SAĞ TIK) stretching from bottom bars into Air Paddles
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(y = lowerTopY)
                    .fillMaxWidth()
                    .padding(
                        start = if (railOnLeft) lowerRailInset else 0.dp,
                        end = if (!railOnLeft) lowerRailInset else 0.dp
                    )
                    .height(lowerHeight),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MorphableMouseButtonTile(
                    title = "SOL TIK",
                    compactSubtitle = "Seç / Sürükle",
                    expandedSubtitle = "BİRİNCİL TIKLAMA",
                    morphFraction = morphFraction,
                    accentColor = RubyPrimary,
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
                        accentColor = RubyAccent,
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
                        accentColor = RubyAccent,
                        onClick = { onMouseButtonClick(MouseButton.RIGHT) },
                        onDown = { onMouseButtonDown(MouseButton.RIGHT) },
                        onUp = { onMouseButtonUp(MouseButton.RIGHT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag(if (airMouseExpanded) "air_mouse_right_btn" else "mouse_btn_right")
                    )
                }
            }

            // 3. Persistent Unified Vertical Scroll Rail gliding smoothly between upper touchpad and lower Air Mouse paddles
            if (railAlpha > 0.02f) {
                UnifiedVerticalScrollRail(
                    scrollMode = settings.airMouseScrollMode,
                    onToggleScrollMode = onToggleScrollMode,
                    onScrollDrag = onScrollGutterDrag,
                    onScrollStep = onScrollStep,
                    modifier = Modifier
                        .align(if (railOnLeft) Alignment.TopStart else Alignment.TopEnd)
                        .offset(y = railTopY)
                        .height(railHeight)
                        .alpha(railAlpha)
                )
            }
        }

        // Fixed Bottom Navigation Dock (identically aligned across both morphed states)
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
