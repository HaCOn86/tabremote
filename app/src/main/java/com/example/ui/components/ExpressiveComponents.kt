package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BackgroundPitch
import com.example.ui.theme.RubyPrimary
import com.example.ui.theme.SurfaceBorderRuby
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.theme.SurfaceContainerCharcoal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Top transparent-status-bar scrim so scrolling content fades organically behind the system clock/icons
 * instead of sitting under a flat black rectangle.
 */
@Composable
fun StatusBarGlassScrim(
    topInsetHeight: Dp,
    modifier: Modifier = Modifier
) {
    val totalHeight = topInsetHeight + 18.dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BackgroundPitch.copy(alpha = 0.86f),
                        BackgroundPitch.copy(alpha = 0.42f),
                        Color.Transparent
                    )
                )
            )
    )
}

/**
 * Material 3 Expressive button physics row helper:
 * When index `i` is pressed, it expands/scales up smoothly while directly adjacent neighbors (`i-1`, `i+1`)
 * gently nudge sideways cleanly with spring physics.
 */
@Composable
fun <T> ExpressiveDockRow(
    items: List<T>,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 4.dp,
    itemWeight: (T) -> Float = { 1f },
    content: @Composable (
        item: T,
        index: Int,
        isPressed: Boolean,
        neighborNudgeX: Float,
        scaleFactor: Float,
        onPressChange: (Boolean) -> Unit
    ) -> Unit
) {
    var pressedIndex by remember(items.size) { mutableIntStateOf(-1) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val isPressed = pressedIndex == index
            val isLeftNeighbor = pressedIndex != -1 && index == pressedIndex - 1
            val isRightNeighbor = pressedIndex != -1 && index == pressedIndex + 1

            val targetScale = when {
                isPressed -> 1.04f
                isLeftNeighbor || isRightNeighbor -> 0.97f
                else -> 1f
            }
            val scale by animateFloatAsState(
                targetValue = targetScale,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "expressive_scale_$index"
            )

            val nudgeDp by animateDpAsState(
                targetValue = when {
                    isLeftNeighbor -> (-2).dp
                    isRightNeighbor -> 2.dp
                    else -> 0.dp
                },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "expressive_nudge_$index"
            )

            Box(
                modifier = Modifier
                    .weight(itemWeight(item))
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = nudgeDp.toPx()
                    }
            ) {
                content(
                    item,
                    index,
                    isPressed,
                    nudgeDp.value,
                    scale
                ) { pressing ->
                    pressedIndex = if (pressing) index else if (pressedIndex == index) -1 else pressedIndex
                }
            }
        }
    }
}

/**
 * Interactive dock editor allowing users to reorder items Left/Right and toggle visibility
 * directly in place on Touchpad/Air Mouse or Inputs Media Dock.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> DockOrderEditorPanel(
    visible: Boolean,
    title: String,
    orderedItems: List<T>,
    hiddenItems: Set<T>,
    labelOf: (T) -> String,
    canHide: (T) -> Boolean = { true },
    onMoveLeft: (Int) -> Unit,
    onMoveRight: (Int) -> Unit,
    onToggleHidden: (T) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(220)) + fadeIn(tween(150)),
        exit = shrinkVertically(tween(200)) + fadeOut(tween(140)),
        modifier = modifier
    ) {
        Surface(
            color = SurfaceCharcoal,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.2.dp, RubyPrimary.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                .animateContentSize()
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = RubyPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                    Surface(
                        color = RubyPrimary,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onDone() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Tamam",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Bitti", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    orderedItems.forEachIndexed { idx, item ->
                        val isHidden = item in hiddenItems
                        Surface(
                            color = if (isHidden) Color(0xFF141416) else SurfaceContainerCharcoal,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.border(
                                1.dp,
                                if (isHidden) SurfaceBorderRuby.copy(alpha = 0.4f) else RubyPrimary.copy(alpha = 0.45f),
                                RoundedCornerShape(8.dp)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (idx > 0) {
                                    IconButton(
                                        onClick = { onMoveLeft(idx) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Sola Taşı",
                                            tint = RubyPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = labelOf(item),
                                    color = if (isHidden) TextMuted else TextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                if (canHide(item)) {
                                    IconButton(
                                        onClick = { onToggleHidden(item) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (isHidden) "Göster" else "Gizle",
                                            tint = if (isHidden) TextMuted else TextSecondary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                if (idx < orderedItems.lastIndex) {
                                    IconButton(
                                        onClick = { onMoveRight(idx) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Sağa Taşı",
                                            tint = RubyPrimary,
                                            modifier = Modifier.size(13.dp)
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
}

fun Modifier.expressivePressDetect(
    key: Any? = Unit,
    onPressChange: (Boolean) -> Unit,
    onLongPress: (() -> Unit)? = null,
    onTap: () -> Unit
): Modifier = pointerInput(key) {
    detectTapGestures(
        onPress = {
            onPressChange(true)
            tryAwaitRelease()
            onPressChange(false)
        },
        onTap = { onTap() },
        onLongPress = if (onLongPress != null) {
            { onLongPress() }
        } else null
    )
}
