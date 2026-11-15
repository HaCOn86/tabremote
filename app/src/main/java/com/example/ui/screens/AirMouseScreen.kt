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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MouseButton
import com.example.model.NavDockItemId
import com.example.model.RemoteSettings
import com.example.model.ScrollSide
import com.example.sensor.AirMouseTelemetry
import com.example.ui.components.UnifiedHorizontalPanRail
import com.example.ui.components.UnifiedVerticalScrollRail
import com.example.ui.theme.RedDanger
import com.example.ui.theme.RubyAccent
import com.example.ui.theme.RubyPrimary
import com.example.ui.theme.SurfaceBorderRuby
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.theme.SurfaceContainerCharcoal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AirMouseScreen(
    airMouseActive: Boolean,
    telemetry: AirMouseTelemetry,
    isGyroSupported: Boolean,
    settings: RemoteSettings,
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
    onMoveNavDockItem: (index: Int, delta: Int) -> Unit = { _, _ -> },
    onToggleNavDockHidden: (NavDockItemId) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showTuning by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 4.dp)
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (settings.showHorizontalScrollGutter) {
            UnifiedHorizontalPanRail(
                onStep = onHorizontalStep,
                onPan = onHorizontalPan,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 46.dp)
                    .testTag("air_mouse_horizontal_rail")
            )
        }

        AirMouseVolumeActionsHintBar(settings = settings)

        if (!isGyroSupported) {
            GyroMissingBanner()
        }

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

        AirMousePaddleZone(
            settings = settings,
            onMouseButtonClick = onMouseButtonClick,
            onMouseButtonDown = onMouseButtonDown,
            onMouseButtonUp = onMouseButtonUp,
            onScrollWheel = onScrollWheel,
            onScrollGutterDrag = onScrollGutterDrag,
            onToggleScrollMode = onToggleScrollMode,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

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
internal fun AirMouseVolumeActionsHintBar(settings: RemoteSettings) {
    Surface(
        color = SurfaceCharcoal,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Ses Tuşları Kısayolu",
                tint = RubyPrimary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Ses Tuşları: Kıs = ${settings.airMouseVolumeDownAction.title} • Aç = ${settings.airMouseVolumeUpAction.title}",
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
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
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceCharcoal,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(108.dp)
            .border(1.2.dp, SurfaceBorderRuby, RoundedCornerShape(14.dp))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = minOf(size.width, size.height) * 0.38f

                listOf(0.5f, 1.0f).forEach { factor ->
                    drawCircle(
                        color = SurfaceBorderRuby,
                        radius = maxRadius * factor,
                        center = centerOffset,
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                    )
                }

                drawLine(
                    color = SurfaceBorderRuby,
                    start = Offset(centerOffset.x, centerOffset.y - maxRadius),
                    end = Offset(centerOffset.x, centerOffset.y + maxRadius),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = SurfaceBorderRuby,
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
                        color = RubyPrimary.copy(alpha = 0.25f),
                        radius = 18.dp.toPx(),
                        center = Offset(targetX, targetY)
                    )
                    drawCircle(
                        color = RubyPrimary,
                        radius = 4.dp.toPx(),
                        center = Offset(targetX, targetY)
                    )
                    drawLine(
                        color = RubyPrimary.copy(alpha = 0.45f),
                        start = centerOffset,
                        end = Offset(targetX, targetY),
                        strokeWidth = 1.2.dp.toPx()
                    )
                } else {
                    drawCircle(
                        color = TextMuted.copy(alpha = 0.5f),
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
                    color = if (telemetry.isActive) RubyPrimary else TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Surface(
                    color = if (showTuning) RubyPrimary.copy(alpha = 0.22f) else SurfaceContainerCharcoal,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .border(1.dp, if (showTuning) RubyPrimary else SurfaceBorderRuby, RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onToggleTuning() }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Ayar",
                            tint = RubyPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (showTuning) "KAPAT" else "AYAR",
                            color = RubyPrimary,
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
    Surface(
        color = SurfaceCharcoal,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, RubyPrimary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Jiroskop Hassasiyeti: ${"%.1f".format(settings.airMouseSensitivity)}x",
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = settings.airMouseSensitivity,
                onValueChange = onUpdateSensitivity,
                valueRange = 0.5f..5.0f,
                colors = SliderDefaults.colors(
                    thumbColor = RubyPrimary,
                    activeTrackColor = RubyPrimary,
                    inactiveTrackColor = SurfaceBorderRuby
                )
            )

            Text(
                text = "Titreme Filtresi (Deadband): ${"%.2f".format(settings.airMouseDeadband)}",
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = settings.airMouseDeadband,
                onValueChange = onUpdateDeadband,
                valueRange = 0.01f..0.15f,
                colors = SliderDefaults.colors(
                    thumbColor = RubyPrimary,
                    activeTrackColor = RubyPrimary,
                    inactiveTrackColor = SurfaceBorderRuby
                )
            )
        }
    }
}

@Composable
internal fun AirMousePaddleZone(
    settings: RemoteSettings,
    onMouseButtonClick: (MouseButton) -> Unit,
    onMouseButtonDown: (MouseButton) -> Unit,
    onMouseButtonUp: (MouseButton) -> Unit,
    onScrollWheel: (ticks: Int) -> Unit,
    onScrollGutterDrag: ((Float) -> Unit)? = null,
    onToggleScrollMode: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    var localAccum by remember { mutableFloatStateOf(0f) }
    val dragHandler: (Float) -> Unit = onScrollGutterDrag ?: { dy ->
        localAccum += dy * 0.08f
        val ticks = localAccum.toInt()
        if (ticks != 0) {
            localAccum -= ticks
            onScrollWheel(-ticks)
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MorphableMouseButtonTile(
                title = "SOL TIK",
                compactSubtitle = "Seç / Sürükle",
                expandedSubtitle = "BİRİNCİL TIKLAMA",
                morphFraction = 1f,
                accentColor = RubyPrimary,
                onClick = { onMouseButtonClick(MouseButton.LEFT) },
                onDown = { onMouseButtonDown(MouseButton.LEFT) },
                onUp = { onMouseButtonUp(MouseButton.LEFT) },
                modifier = Modifier.weight(1.3f).testTag("air_mouse_left_btn")
            )

            MorphableMouseButtonTile(
                title = "SAĞ TIK",
                compactSubtitle = "Menü",
                expandedSubtitle = "BAĞLAM MENÜSÜ",
                morphFraction = 1f,
                accentColor = RubyAccent,
                onClick = { onMouseButtonClick(MouseButton.RIGHT) },
                onDown = { onMouseButtonDown(MouseButton.RIGHT) },
                onUp = { onMouseButtonUp(MouseButton.RIGHT) },
                modifier = Modifier.weight(1f).testTag("air_mouse_right_btn")
            )
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
}

@Composable
private fun AirThumbPaddle(
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    onDown: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "paddle_scale"
    )
    val bg by animateColorAsState(
        targetValue = if (isPressed) accentColor.copy(alpha = 0.28f) else SurfaceCharcoal,
        animationSpec = tween(140),
        label = "paddle_bg"
    )

    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxSize()
            .scale(scale)
            .border(
                1.5.dp,
                if (isPressed) accentColor else SurfaceBorderRuby,
                RoundedCornerShape(16.dp)
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
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(if (isPressed) accentColor else SurfaceBorderRuby, CircleShape)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = if (isPressed) accentColor else TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
