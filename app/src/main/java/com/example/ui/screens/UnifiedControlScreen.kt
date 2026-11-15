package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetooth.TouchContact
import com.example.model.ButtonPosition
import com.example.model.MouseButton
import com.example.model.NavDockItemId
import com.example.model.RemoteSettings
import com.example.model.ScrollSide
import com.example.sensor.AirMouseTelemetry
import com.example.ui.theme.RubyPrimary
import com.example.ui.theme.SurfaceBorderRuby
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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
        // Top Header Bar: Horizontal Pan Gutter + One-Tap Air Mouse Morph Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 46.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (settings.showHorizontalScrollGutter) {
                HorizontalScrollGutter(
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

        // Animated Morph Zone between Trackpad Mode & Air Mouse Mode
        AnimatedContent(
            targetState = airMouseExpanded,
            transitionSpec = {
                if (targetState) {
                    (slideInVertically { -it / 6 } + fadeIn(tween(240)) + scaleIn(initialScale = 0.96f)) togetherWith
                        (slideOutVertically { -it / 6 } + fadeOut(tween(180)) + scaleOut(targetScale = 0.96f))
                } else {
                    (slideInVertically { it / 6 } + fadeIn(tween(240)) + scaleIn(initialScale = 0.96f)) togetherWith
                        (slideOutVertically { it / 6 } + fadeOut(tween(180)) + scaleOut(targetScale = 0.96f))
                }
            },
            label = "unified_control_morph",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { isAirMode ->
            if (!isAirMode) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (settings.buttonPosition == ButtonPosition.TOP) {
                        MouseButtonsBar(
                            settings = settings,
                            onMouseButtonClick = onMouseButtonClick,
                            onMouseButtonDown = onMouseButtonDown,
                            onMouseButtonUp = onMouseButtonUp
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
                                .testTag("unified_trackpad_area")
                        )

                        if (settings.scrollSide == ScrollSide.RIGHT) {
                            InteractiveScrollGutter(
                                onScrollDrag = onScrollGutterDrag,
                                onScrollStep = onScrollStep,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }

                    if (settings.buttonPosition == ButtonPosition.BOTTOM || settings.buttonPosition == ButtonPosition.SPLIT) {
                        MouseButtonsBar(
                            settings = settings,
                            onMouseButtonClick = onMouseButtonClick,
                            onMouseButtonDown = onMouseButtonDown,
                            onMouseButtonUp = onMouseButtonUp
                        )
                    }
                }
            } else {
                // Morphed Air Mouse State: Upper Gyro Radar + Elongated Click Paddles & Vertical Scroll Rail
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GyroscopeRadarCard(
                        airMouseActive = airMouseActive,
                        telemetry = telemetry,
                        showTuning = showAirTuning,
                        onToggleTuning = { showAirTuning = !showAirTuning }
                    )

                    AnimatedVisibility(
                        visible = showAirTuning,
                        enter = expandVertically(tween(200)) + fadeIn(tween(140)),
                        exit = shrinkVertically(tween(180)) + fadeOut(tween(120))
                    ) {
                        AirTuningPanel(
                            settings = settings,
                            onUpdateSensitivity = onUpdateSensitivity,
                            onUpdateDeadband = onUpdateDeadband
                        )
                    }

                    if (!isGyroSupported) {
                        GyroMissingBanner()
                    } else {
                        AirMouseVolumeActionsHintBar(settings = settings)
                    }

                    AirMousePaddleZone(
                        settings = settings,
                        onMouseButtonClick = onMouseButtonClick,
                        onMouseButtonDown = onMouseButtonDown,
                        onMouseButtonUp = onMouseButtonUp,
                        onScrollWheel = { ticks -> onScrollStep(ticks) },
                        onToggleScrollMode = onToggleScrollMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
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
