package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AirMouseScrollMode
import com.example.ui.theme.TabRemoteTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Unified Horizontal Pan Rail shared across Touchpad, Air Mouse & Control screens.
 * Uses crisp opaque surfaces with neutral borders at rest and accent fill only while actively dragged.
 */
@Composable
fun UnifiedHorizontalPanRail(
    onStep: (direction: Int) -> Unit,
    onPan: (deltaX: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    val grooveColor = palette.borderSubtle

    BoxWithConstraints(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(palette.scrollGutterBg)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(10.dp))
    ) {
        val density = LocalDensity.current
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val arrowWidthPx = with(density) { 40.dp.toPx() }
        val thumbWidthPx = with(density) { 56.dp.toPx() }

        val minThumbX = arrowWidthPx
        val maxThumbX = (totalWidthPx - arrowWidthPx - thumbWidthPx).coerceAtLeast(minThumbX)
        val centerThumbX = (minThumbX + maxThumbX) / 2f

        val thumbAnimatable = remember(maxWidth) { Animatable(centerThumbX) }
        val scope = rememberCoroutineScope()
        var isDragging by remember { mutableStateOf(false) }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 10.dp.toPx()
            val insetY = 9.dp.toPx()
            var x = arrowWidthPx + 8.dp.toPx()
            while (x < size.width - arrowWidthPx - 8.dp.toPx()) {
                drawLine(
                    color = grooveColor,
                    start = Offset(x, insetY),
                    end = Offset(x, size.height - insetY),
                    strokeWidth = 1.4.dp.toPx()
                )
                x += step
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(40.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                .clickable { onStep(-1) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Önceki",
                tint = palette.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Surface(
            color = if (isDragging) palette.primary else palette.controlThumb,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(thumbAnimatable.value.roundToInt(), 0) }
                .width(56.dp)
                .height(28.dp)
                .border(
                    1.dp,
                    if (isDragging) palette.primary else palette.borderElevated,
                    RoundedCornerShape(8.dp)
                )
                .pointerInput(minThumbX, maxThumbX) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val nextX = (thumbAnimatable.value + dragAmount.x).coerceIn(minThumbX, maxThumbX)
                            scope.launch { thumbAnimatable.snapTo(nextX) }
                            onPan(dragAmount.x)
                        },
                        onDragEnd = {
                            isDragging = false
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
                            isDragging = false
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
                    tint = if (isDragging) palette.onAccent else palette.textPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "PAN",
                    color = if (isDragging) palette.onAccent else palette.textPrimary,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(40.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                .clickable { onStep(1) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Sonraki",
                tint = palette.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Unified Vertical Scroll Rail shared between Touchpad & Air Mouse.
 * Includes the top `RAY` / `TUŞ` toggle to morph the notched scroll rail into physical Up/Down hold buttons.
 */
@Composable
fun UnifiedVerticalScrollRail(
    scrollMode: AirMouseScrollMode,
    onToggleScrollMode: () -> Unit,
    onScrollDrag: (deltaY: Float) -> Unit,
    onScrollStep: (ticks: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    val isStepMode = scrollMode == AirMouseScrollMode.STEP_BUTTONS

    Column(
        modifier = modifier.width(52.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = if (isStepMode) palette.controlThumb else palette.surface,
            shape = RoundedCornerShape(9.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .border(
                    1.dp,
                    if (isStepMode) palette.primary else palette.borderSubtle,
                    RoundedCornerShape(9.dp)
                )
                .clickable { onToggleScrollMode() }
                .testTag("vertical_scroll_mode_toggle")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Kaydırma Tipi Değiştir",
                    tint = if (isStepMode) palette.primary else palette.textPrimary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = if (scrollMode == AirMouseScrollMode.SCROLL_STRIP) "RAY" else "TUŞ",
                    color = if (isStepMode) palette.primary else palette.textPrimary,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    softWrap = false
                )
            }
        }

        AnimatedContent(
            targetState = scrollMode,
            transitionSpec = {
                (fadeIn(tween(180)) + scaleIn(initialScale = 0.94f)) togetherWith
                    (fadeOut(tween(140)) + scaleOut(targetScale = 0.94f))
            },
            label = "vertical_rail_mode_morph",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { mode ->
            if (mode == AirMouseScrollMode.SCROLL_STRIP) {
                NotchedVerticalRailStrip(
                    onScrollDrag = onScrollDrag,
                    onScrollStep = onScrollStep,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("dedicated_scroll_gutter")
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("scroll_button_grid"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HoldScrollStepButton(
                        icon = Icons.Default.KeyboardArrowUp,
                        label = "YUKARI",
                        onScrollStep = { onScrollStep(2) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("rail_hold_scroll_up")
                    )
                    HoldScrollStepButton(
                        icon = Icons.Default.KeyboardArrowDown,
                        label = "AŞAĞI",
                        onScrollStep = { onScrollStep(-2) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("rail_hold_scroll_down")
                    )
                }
            }
        }
    }
}

@Composable
private fun NotchedVerticalRailStrip(
    onScrollDrag: (deltaY: Float) -> Unit,
    onScrollStep: (ticks: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    val grooveColor = palette.borderSubtle

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.scrollGutterBg)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(12.dp))
    ) {
        val density = LocalDensity.current
        val totalHeightPx = with(density) { maxHeight.toPx() }
        val arrowAreaHeightPx = with(density) { 38.dp.toPx() }
        val thumbHeightPx = with(density) { 54.dp.toPx() }

        val minThumbY = arrowAreaHeightPx
        val maxThumbY = (totalHeightPx - arrowAreaHeightPx - thumbHeightPx).coerceAtLeast(minThumbY)
        val centerThumbY = (minThumbY + maxThumbY) / 2f

        val thumbAnimatable = remember(maxWidth, maxHeight) { Animatable(centerThumbY) }
        val scope = rememberCoroutineScope()
        var accumDy by remember { mutableFloatStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }

        LaunchedEffect(isDragging, minThumbY, maxThumbY) {
            if (!isDragging) return@LaunchedEffect
            while (isActive && isDragging) {
                val y = thumbAnimatable.value
                val edgeThreshold = 4f
                when {
                    maxThumbY > minThumbY + edgeThreshold && y <= minThumbY + edgeThreshold -> {
                        onScrollStep(1)
                    }
                    maxThumbY > minThumbY + edgeThreshold && y >= maxThumbY - edgeThreshold -> {
                        onScrollStep(-1)
                    }
                }
                delay(75L)
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 12.dp.toPx()
            val insetX = 13.dp.toPx()
            var y = arrowAreaHeightPx + 8.dp.toPx()
            while (y < size.height - arrowAreaHeightPx - 8.dp.toPx()) {
                drawLine(
                    color = grooveColor,
                    start = Offset(insetX, y),
                    end = Offset(size.width - insetX, y),
                    strokeWidth = 1.5.dp.toPx()
                )
                y += step
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .clickable { onScrollStep(2) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Yukarı Kaydır",
                tint = palette.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Surface(
            color = if (isDragging) palette.primary else palette.controlThumb,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, thumbAnimatable.value.roundToInt()) }
                .width(42.dp)
                .height(54.dp)
                .border(
                    1.dp,
                    if (isDragging) palette.primary else palette.borderElevated,
                    RoundedCornerShape(8.dp)
                )
                .pointerInput(minThumbY, maxThumbY) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val nextY = (thumbAnimatable.value + dragAmount.y).coerceIn(minThumbY, maxThumbY)
                            scope.launch { thumbAnimatable.snapTo(nextY) }
                            accumDy += dragAmount.y
                            if (kotlin.math.abs(accumDy) >= 4f) {
                                onScrollDrag(accumDy)
                                accumDy = 0f
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            accumDy = 0f
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
                            isDragging = false
                            accumDy = 0f
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
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Kaydırma Tutacı",
                    tint = if (isDragging) palette.onAccent else palette.textPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "KAYDIR",
                    color = if (isDragging) palette.onAccent else palette.textPrimary,
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                .clickable { onScrollStep(-2) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Aşağı Kaydır",
                tint = palette.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun HoldScrollStepButton(
    icon: ImageVector,
    label: String,
    onScrollStep: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    var isPressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "hold_scroll_scale"
    )

    Surface(
        color = if (isPressed) palette.primary else palette.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .scale(scale)
            .border(
                1.dp,
                if (isPressed) palette.primary else palette.borderSubtle,
                RoundedCornerShape(10.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onScrollStep()
                        val holdJob = scope.launch {
                            delay(220)
                            while (isActive) {
                                onScrollStep()
                                delay(75)
                            }
                        }
                        tryAwaitRelease()
                        holdJob.cancel()
                        isPressed = false
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isPressed) palette.onAccent else palette.textPrimary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                color = if (isPressed) palette.onAccent else palette.textSecondary,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                softWrap = false
            )
        }
    }
}
