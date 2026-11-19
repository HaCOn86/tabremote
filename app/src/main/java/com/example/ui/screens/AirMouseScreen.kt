package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppRole
import com.example.model.HorizontalPanPosition
import com.example.model.MouseButton
import com.example.model.RemoteSettings
import com.example.model.ScrollSide
import com.example.sensor.AirMouseTelemetry
import com.example.ui.components.ReceiverLockedControlContainer
import com.example.ui.components.UnifiedHorizontalPanRail
import com.example.ui.components.UnifiedVerticalScrollRail
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TabRemoteTheme

@Composable
fun AirMouseScreen(
    airMouseActive: Boolean,
    telemetry: AirMouseTelemetry,
    isGyroSupported: Boolean,
    settings: RemoteSettings,
    isTabletLayout: Boolean = false,
    onMouseButtonClick: (MouseButton) -> Unit,
    onMouseButtonDown: (MouseButton) -> Unit,
    onMouseButtonUp: (MouseButton) -> Unit,
    onScrollWheel: (ticks: Int) -> Unit,
    onScrollGutterDrag: ((Float) -> Unit)? = null,
    onHorizontalPan: (deltaX: Float) -> Unit,
    onHorizontalStep: (direction: Int) -> Unit,
    onUpdateSensitivity: (Float) -> Unit,
    onUpdateDeadband: (Float) -> Unit,
    onToggleScrollMode: (() -> Unit)? = null,
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onRecentAppsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    var showTuning by remember { mutableStateOf(false) }
    var localAccum by remember { mutableFloatStateOf(0f) }
    val dragHandler: (Float) -> Unit = onScrollGutterDrag ?: { dy ->
        localAccum += dy * 0.08f
        val ticks = localAccum.toInt()
        if (ticks != 0) {
            localAccum -= ticks
            onScrollWheel(-ticks)
        }
    }

    ReceiverLockedControlContainer(
        isLockedForReceiver = settings.appRole == AppRole.RECEIVER,
        modifier = modifier.fillMaxSize()
    ) {
    if (isTabletLayout) {
        val railOnLeft = settings.scrollSide == ScrollSide.LEFT
        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 6.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.75f)
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
                        morphFraction = 1f,
                        accentColor = palette.primary,
                        onClick = { onMouseButtonClick(MouseButton.LEFT) },
                        onDown = { onMouseButtonDown(MouseButton.LEFT) },
                        onUp = { onMouseButtonUp(MouseButton.LEFT) },
                        modifier = Modifier
                            .weight(1.25f)
                            .fillMaxWidth()
                            .testTag("air_mouse_left_btn")
                    )

                    MorphableMouseButtonTile(
                        title = "SAĞ TIK",
                        compactSubtitle = "Menü",
                        expandedSubtitle = "BAĞLAM MENÜSÜ",
                        morphFraction = 1f,
                        accentColor = palette.accent,
                        onClick = { onMouseButtonClick(MouseButton.RIGHT) },
                        onDown = { onMouseButtonDown(MouseButton.RIGHT) },
                        onUp = { onMouseButtonUp(MouseButton.RIGHT) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("air_mouse_right_btn")
                    )
                }

                CustomizableNavigationDock(
                    settings = settings,
                    onBackClick = onBackClick,
                    onHomeClick = onHomeClick,
                    onRecentAppsClick = onRecentAppsClick,
                    onMouseButtonClick = onMouseButtonClick
                )
            }

            Column(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (settings.showHorizontalScrollGutter && settings.horizontalPanPosition == HorizontalPanPosition.ABOVE_BUTTONS) {
                    UnifiedHorizontalPanRail(
                        onStep = onHorizontalStep,
                        onPan = onHorizontalPan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("air_mouse_horizontal_rail")
                    )
                }

                if (!isGyroSupported) {
                    GyroMissingBanner()
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (railOnLeft) {
                        UnifiedVerticalScrollRail(
                            scrollMode = settings.airMouseScrollMode,
                            onToggleScrollMode = { onToggleScrollMode?.invoke() },
                            onScrollDrag = dragHandler,
                            onScrollStep = onScrollWheel,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }

                    GyroscopeRadarCard(
                        airMouseActive = airMouseActive,
                        telemetry = telemetry,
                        showTuning = showTuning,
                        onToggleTuning = { showTuning = !showTuning },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    if (!railOnLeft) {
                        UnifiedVerticalScrollRail(
                            scrollMode = settings.airMouseScrollMode,
                            onToggleScrollMode = { onToggleScrollMode?.invoke() },
                            onScrollDrag = dragHandler,
                            onScrollStep = onScrollWheel,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showTuning,
                    enter = expandVertically(tween(220)) + fadeIn(tween(150)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(140))
                ) {
                    AirTuningPanel(
                        settings = settings,
                        onUpdateSensitivity = onUpdateSensitivity,
                        onUpdateDeadband = onUpdateDeadband
                    )
                }

                if (settings.showHorizontalScrollGutter && settings.horizontalPanPosition == HorizontalPanPosition.BELOW_BUTTONS) {
                    UnifiedHorizontalPanRail(
                        onStep = onHorizontalStep,
                        onPan = onHorizontalPan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("air_mouse_horizontal_rail")
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (settings.showHorizontalScrollGutter && settings.horizontalPanPosition == HorizontalPanPosition.ABOVE_BUTTONS) {
                UnifiedHorizontalPanRail(
                    onStep = onHorizontalStep,
                    onPan = onHorizontalPan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("air_mouse_horizontal_rail")
                )
            }

        if (!isGyroSupported) {
            GyroMissingBanner()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (settings.scrollSide == ScrollSide.LEFT) {
                UnifiedVerticalScrollRail(
                    scrollMode = settings.airMouseScrollMode,
                    onToggleScrollMode = { onToggleScrollMode?.invoke() },
                    onScrollDrag = dragHandler,
                    onScrollStep = onScrollWheel,
                    modifier = Modifier.fillMaxHeight()
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GyroscopeRadarCard(
                    airMouseActive = airMouseActive,
                    telemetry = telemetry,
                    showTuning = showTuning,
                    onToggleTuning = { showTuning = !showTuning }
                )

                AnimatedVisibility(
                    visible = showTuning,
                    enter = expandVertically(tween(220)) + fadeIn(tween(150)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(140))
                ) {
                    AirTuningPanel(
                        settings = settings,
                        onUpdateSensitivity = onUpdateSensitivity,
                        onUpdateDeadband = onUpdateDeadband
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MorphableMouseButtonTile(
                        title = "SOL TIK",
                        compactSubtitle = "Seç / Sürükle",
                        expandedSubtitle = "BİRİNCİL TIKLAMA",
                        morphFraction = 1f,
                        accentColor = palette.primary,
                        onClick = { onMouseButtonClick(MouseButton.LEFT) },
                        onDown = { onMouseButtonDown(MouseButton.LEFT) },
                        onUp = { onMouseButtonUp(MouseButton.LEFT) },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("air_mouse_left_btn")
                    )

                    MorphableMouseButtonTile(
                        title = "SAĞ TIK",
                        compactSubtitle = "Menü",
                        expandedSubtitle = "BAĞLAM MENÜSÜ",
                        morphFraction = 1f,
                        accentColor = palette.accent,
                        onClick = { onMouseButtonClick(MouseButton.RIGHT) },
                        onDown = { onMouseButtonDown(MouseButton.RIGHT) },
                        onUp = { onMouseButtonUp(MouseButton.RIGHT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("air_mouse_right_btn")
                    )
                }
            }

            if (settings.scrollSide != ScrollSide.LEFT) {
                UnifiedVerticalScrollRail(
                    scrollMode = settings.airMouseScrollMode,
                    onToggleScrollMode = { onToggleScrollMode?.invoke() },
                    onScrollDrag = dragHandler,
                    onScrollStep = onScrollWheel,
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }

        if (settings.showHorizontalScrollGutter && settings.horizontalPanPosition == HorizontalPanPosition.BELOW_BUTTONS) {
            UnifiedHorizontalPanRail(
                onStep = onHorizontalStep,
                onPan = onHorizontalPan,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("air_mouse_horizontal_rail")
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
internal fun GyroMissingBanner() {
    Surface(
        color = RedDanger.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, RedDanger.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Bilgi",
                tint = RedDanger,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Jiroskop donanım sensörü bu cihazda algılanamadı.",
                color = RedDanger,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
internal fun GyroscopeRadarCard(
    airMouseActive: Boolean,
    telemetry: AirMouseTelemetry,
    showTuning: Boolean,
    onToggleTuning: () -> Unit,
    cardShape: Shape = RoundedCornerShape(14.dp),
    isNotchedTopLeft: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    val gridLineColor = palette.borderElevated
    val primaryColor = palette.primary

    Surface(
        color = palette.surface,
        shape = cardShape,
        modifier = modifier
            .fillMaxWidth()
            .height(108.dp)
            .clip(cardShape)
            .border(1.2.dp, palette.borderSubtle, cardShape)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = if (isNotchedTopLeft) (size.width * 0.58f) else (size.width / 2f)
                val centerOffset = Offset(centerX, size.height / 2f)
                val maxRadius = minOf(size.width * 0.34f, size.height * 0.38f)

                listOf(0.5f, 1.0f).forEach { factor ->
                    drawCircle(
                        color = gridLineColor,
                        radius = maxRadius * factor,
                        center = centerOffset,
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                    )
                }

                drawLine(
                    color = gridLineColor,
                    start = Offset(centerOffset.x, centerOffset.y - maxRadius),
                    end = Offset(centerOffset.x, centerOffset.y + maxRadius),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = gridLineColor,
                    start = Offset(centerOffset.x - maxRadius, centerOffset.y),
                    end = Offset(centerOffset.x + maxRadius, centerOffset.y),
                    strokeWidth = 1.dp.toPx()
                )

                if (airMouseActive) {
                    val targetX = (centerOffset.x + (telemetry.deltaX * 2.8f)).coerceIn(
                        centerOffset.x - maxRadius,
                        centerOffset.x + maxRadius
                    )
                    val targetY = (centerOffset.y + (telemetry.deltaY * 2.8f)).coerceIn(
                        centerOffset.y - maxRadius,
                        centerOffset.y + maxRadius
                    )

                    drawCircle(
                        color = primaryColor.copy(alpha = 0.25f),
                        radius = 18.dp.toPx(),
                        center = Offset(targetX, targetY)
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 4.dp.toPx(),
                        center = Offset(targetX, targetY)
                    )
                    drawLine(
                        color = primaryColor.copy(alpha = 0.45f),
                        start = centerOffset,
                        end = Offset(targetX, targetY),
                        strokeWidth = 1.2.dp.toPx()
                    )
                } else {
                    drawCircle(
                        color = palette.textMuted.copy(alpha = 0.5f),
                        radius = 3.dp.toPx(),
                        center = centerOffset
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "GYRO (${telemetry.deltaX}, ${telemetry.deltaY})",
                    color = if (telemetry.isActive) primaryColor else palette.textMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Surface(
                    color = if (showTuning) palette.controlThumb else palette.surfaceContainer,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .border(
                            1.dp,
                            if (showTuning) primaryColor else palette.borderSubtle,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onToggleTuning() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Ayar",
                            tint = if (showTuning) primaryColor else palette.textPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (showTuning) "KAPAT" else "AYAR",
                            color = if (showTuning) primaryColor else palette.textPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AirTuningPanel(
    settings: RemoteSettings,
    onUpdateSensitivity: (Float) -> Unit,
    onUpdateDeadband: (Float) -> Unit
) {
    val palette = TabRemoteTheme.palette
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Jiroskop Hassasiyeti: ${"%.1f".format(settings.airMouseSensitivity)}x",
                color = palette.textPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = settings.airMouseSensitivity,
                onValueChange = onUpdateSensitivity,
                valueRange = 0.5f..5.0f,
                colors = SliderDefaults.colors(
                    thumbColor = palette.primary,
                    activeTrackColor = palette.primary,
                    inactiveTrackColor = palette.borderElevated
                )
            )

            Text(
                text = "Titreme Filtresi (Deadband): ${"%.2f".format(settings.airMouseDeadband)}",
                color = palette.textPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = settings.airMouseDeadband,
                onValueChange = onUpdateDeadband,
                valueRange = 0.01f..0.15f,
                colors = SliderDefaults.colors(
                    thumbColor = palette.primary,
                    activeTrackColor = palette.primary,
                    inactiveTrackColor = palette.borderElevated
                )
            )
        }
    }
}
