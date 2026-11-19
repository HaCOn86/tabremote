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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.GamepadLayoutOffsets
import com.example.model.GamepadStyle
import com.example.ui.components.ReceiverLockedControlContainer
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.TabRemoteTheme
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private object PadBtn {
    const val A_CROSS = 1 shl 0
    const val B_CIRCLE = 1 shl 1
    const val X_SQUARE = 1 shl 3
    const val Y_TRIANGLE = 1 shl 4
    const val L1_LB = 1 shl 6
    const val R1_RB = 1 shl 7
    const val L2_LT = 1 shl 8
    const val R2_RT = 1 shl 9
    const val SELECT_VIEW = 1 shl 10
    const val START_MENU = 1 shl 11
    const val HOME = 1 shl 12
    const val L3 = 1 shl 13
    const val R3 = 1 shl 14
}

private enum class PsSymbol { TRIANGLE, CIRCLE, CROSS, SQUARE }

private enum class GamepadModuleId {
    DPAD,
    LEFT_STICK,
    FACE_BUTTONS,
    RIGHT_STICK,
    CENTER_PAD
}

@Composable
fun GamepadScreenContent(
    gamepadStyle: GamepadStyle,
    gamepadOffsets: GamepadLayoutOffsets = GamepadLayoutOffsets(),
    swapLeftWing: Boolean = false,
    swapRightWing: Boolean = false,
    mirrorHands: Boolean = false,
    isReceiverLocked: Boolean = false,
    onToggleStyle: () -> Unit,
    onUpdateOffsets: ((GamepadLayoutOffsets) -> GamepadLayoutOffsets) -> Unit = {},
    onResetLayout: () -> Unit = {},
    onToggleSwapLeftWing: () -> Unit = {},
    onToggleSwapRightWing: () -> Unit = {},
    onToggleMirrorHands: () -> Unit = {},
    onExitFullscreen: () -> Unit,
    onGamepadState: (buttonsMask: Int, hat: Int, lx: Int, ly: Int, rx: Int, ry: Int) -> Unit,
    onTrackpadDrag: (dx: Float, dy: Float) -> Unit,
    onTrackpadTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    var buttonsMask by remember { mutableIntStateOf(0) }
    var hatSwitch by remember { mutableIntStateOf(8) }
    var lx by remember { mutableIntStateOf(0) }
    var ly by remember { mutableIntStateOf(0) }
    var rx by remember { mutableIntStateOf(0) }
    var ry by remember { mutableIntStateOf(0) }
    var showLayoutEditor by remember { mutableStateOf(false) }

    var localOffsets by remember { mutableStateOf(gamepadOffsets) }
    LaunchedEffect(gamepadOffsets) {
        localOffsets = gamepadOffsets
    }

    fun pushState() {
        onGamepadState(buttonsMask, hatSwitch, lx, ly, rx, ry)
    }

    fun setButtonPressed(bit: Int, down: Boolean) {
        buttonsMask = if (down) buttonsMask or bit else buttonsMask and bit.inv()
        pushState()
    }

    val isPs = gamepadStyle == GamepadStyle.PLAYSTATION

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Symmetric Top Shoulder Triggers & System Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShoulderTriggerButton(
                label = if (isPs) "L2" else "LT",
                pressed = (buttonsMask and PadBtn.L2_LT) != 0,
                onStateChange = { setButtonPressed(PadBtn.L2_LT, it) },
                modifier = Modifier.weight(1f)
            )
            ShoulderTriggerButton(
                label = if (isPs) "L1" else "LB",
                pressed = (buttonsMask and PadBtn.L1_LB) != 0,
                onStateChange = { setButtonPressed(PadBtn.L1_LB, it) },
                modifier = Modifier.weight(1f)
            )

            Surface(
                color = palette.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .width(78.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, palette.borderSubtle, RoundedCornerShape(8.dp))
                    .clickable { onToggleStyle() }
                    .testTag("gamepad_style_switch")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = "Stil Değiştir",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPs) "PS5" else "XBOX",
                        color = palette.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        softWrap = false,
                        maxLines = 1
                    )
                }
            }

            ShoulderTriggerButton(
                label = if (isPs) "SHARE" else "VIEW",
                pressed = (buttonsMask and PadBtn.SELECT_VIEW) != 0,
                onStateChange = { setButtonPressed(PadBtn.SELECT_VIEW, it) },
                modifier = Modifier.weight(0.85f)
            )

            val homePressed = (buttonsMask and PadBtn.HOME) != 0
            Surface(
                color = if (homePressed) palette.primary else palette.surface,
                shape = CircleShape,
                modifier = Modifier
                    .size(38.dp)
                    .border(
                        1.2.dp,
                        if (homePressed) palette.primary else palette.borderElevated,
                        CircleShape
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                setButtonPressed(PadBtn.HOME, true)
                                tryAwaitRelease()
                                setButtonPressed(PadBtn.HOME, false)
                            }
                        )
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Console Home",
                        tint = if (homePressed) palette.onAccent else palette.textPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            ShoulderTriggerButton(
                label = if (isPs) "OPT" else "MENU",
                pressed = (buttonsMask and PadBtn.START_MENU) != 0,
                onStateChange = { setButtonPressed(PadBtn.START_MENU, it) },
                modifier = Modifier.weight(0.85f)
            )

            Surface(
                color = palette.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .width(78.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, palette.borderSubtle, RoundedCornerShape(8.dp))
                    .clickable { onExitFullscreen() }
                    .testTag("gamepad_exit_btn")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Çıkış",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "ÇIKIŞ",
                        color = palette.textPrimary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        softWrap = false
                    )
                }
            }

            ShoulderTriggerButton(
                label = if (isPs) "R1" else "RB",
                pressed = (buttonsMask and PadBtn.R1_RB) != 0,
                onStateChange = { setButtonPressed(PadBtn.R1_RB, it) },
                modifier = Modifier.weight(1f)
            )
            ShoulderTriggerButton(
                label = if (isPs) "R2" else "RT",
                pressed = (buttonsMask and PadBtn.R2_RT) != 0,
                onStateChange = { setButtonPressed(PadBtn.R2_RT, it) },
                modifier = Modifier.weight(1f)
            )
        }

        // Free-Drag Dual-Analog Gamepad Arena (wrapped in ReceiverLockedControlContainer when in Receiver role)
        val density = LocalDensity.current
        ReceiverLockedControlContainer(
            isLockedForReceiver = isReceiverLocked && !showLayoutEditor,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("gamepad_free_arena")
            ) {
                val arenaWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(320f)
                val arenaHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(180f)

                val dpadSizePx = with(density) { 116.dp.toPx() }.coerceAtMost(arenaHeightPx * 0.46f)
                val stickSizePx = with(density) { 110.dp.toPx() }.coerceAtMost(arenaHeightPx * 0.44f)
                val faceSizePx = with(density) { 124.dp.toPx() }.coerceAtMost(arenaHeightPx * 0.48f)
                val centerW = (arenaWidthPx * 0.36f).coerceIn(172f, 360f)
                val centerH = (arenaHeightPx * 0.62f).coerceIn(118f, 248f)

                val leftWingIsDpadTop = if (isPs) !swapLeftWing else swapLeftWing
                val edgePadPx = with(density) { 12.dp.toPx() }
                val leftColX = if (mirrorHands) arenaWidthPx - dpadSizePx - edgePadPx else edgePadPx
                val rightColX = if (mirrorHands) edgePadPx else arenaWidthPx - faceSizePx - edgePadPx

                val topRowY = (arenaHeightPx * 0.04f)
                val botRowY = (arenaHeightPx - stickSizePx - with(density) { 6.dp.toPx() }).coerceAtLeast(topRowY + dpadSizePx + 6f)

                val innerStepPx = with(density) { 42.dp.toPx() }
                val baseDpad = Offset(
                    x = if (leftWingIsDpadTop) leftColX else if (mirrorHands) leftColX - innerStepPx else leftColX + innerStepPx,
                    y = if (leftWingIsDpadTop) topRowY else botRowY
                )
                val baseLeftStick = Offset(
                    x = if (!leftWingIsDpadTop) leftColX else if (mirrorHands) leftColX - innerStepPx else leftColX + innerStepPx,
                    y = if (!leftWingIsDpadTop) topRowY else botRowY
                )
                val baseFace = Offset(
                    x = if (!swapRightWing) rightColX else if (mirrorHands) rightColX + innerStepPx else rightColX - innerStepPx,
                    y = if (!swapRightWing) topRowY else botRowY
                )
                val baseRightStick = Offset(
                    x = if (swapRightWing) rightColX else if (mirrorHands) rightColX + innerStepPx else rightColX - innerStepPx,
                    y = if (swapRightWing) topRowY else botRowY
                )
                val baseCenterPad = Offset(
                    x = (arenaWidthPx - centerW) / 2f,
                    y = (arenaHeightPx - centerH) * 0.40f
                )

                fun clampedPixelPos(base: Offset, offXFrac: Float, offYFrac: Float, modW: Float, modH: Float): Offset {
                    val x = (base.x + offXFrac * arenaWidthPx).coerceIn(0f, (arenaWidthPx - modW).coerceAtLeast(0f))
                    val y = (base.y + offYFrac * arenaHeightPx).coerceIn(0f, (arenaHeightPx - modH).coerceAtLeast(0f))
                    return Offset(x, y)
                }

                val dpadPos = clampedPixelPos(baseDpad, localOffsets.dpadX, localOffsets.dpadY, dpadSizePx, dpadSizePx)
                val lStickPos = clampedPixelPos(baseLeftStick, localOffsets.leftStickX, localOffsets.leftStickY, stickSizePx, stickSizePx)
                val cPadPos = clampedPixelPos(baseCenterPad, localOffsets.centerPadX, localOffsets.centerPadY, centerW, centerH)
                val rStickPos = clampedPixelPos(baseRightStick, localOffsets.rightStickX, localOffsets.rightStickY, stickSizePx, stickSizePx)
                val facePos = clampedPixelPos(baseFace, localOffsets.faceButtonsX, localOffsets.faceButtonsY, faceSizePx, faceSizePx)

                var activeDragModule by remember { mutableStateOf<GamepadModuleId?>(null) }

                val latestDpadPos by rememberUpdatedState(dpadPos)
                val latestLStickPos by rememberUpdatedState(lStickPos)
                val latestCPadPos by rememberUpdatedState(cPadPos)
                val latestRStickPos by rememberUpdatedState(rStickPos)
                val latestFacePos by rememberUpdatedState(facePos)
                val latestBaseDpad by rememberUpdatedState(baseDpad)
                val latestBaseLStick by rememberUpdatedState(baseLeftStick)
                val latestBaseCPad by rememberUpdatedState(baseCenterPad)
                val latestBaseRStick by rememberUpdatedState(baseRightStick)
                val latestBaseFace by rememberUpdatedState(baseFace)
                val latestArenaW by rememberUpdatedState(arenaWidthPx)
                val latestArenaH by rememberUpdatedState(arenaHeightPx)
                val latestOnUpdateOffsets by rememberUpdatedState(onUpdateOffsets)

                // 1. D-Pad Module
                val dpadSizeDp = with(density) { dpadSizePx.toDp() }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(dpadPos.x.roundToInt(), dpadPos.y.roundToInt()) }
                        .size(dpadSizeDp)
                        .then(
                            if (showLayoutEditor) {
                                Modifier.border(
                                    if (activeDragModule == GamepadModuleId.DPAD) 2.dp else 1.5.dp,
                                    palette.primary,
                                    CircleShape
                                )
                            } else Modifier
                        )
                ) {
                    CrossDPad(
                        currentHat = hatSwitch,
                        enabled = !showLayoutEditor && !isReceiverLocked,
                        onHatChange = { hatSwitch = it; pushState() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 2. Left Analog Stick Module
                val stickSizeDp = with(density) { stickSizePx.toDp() }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(lStickPos.x.roundToInt(), lStickPos.y.roundToInt()) }
                        .size(stickSizeDp)
                        .then(
                            if (showLayoutEditor) {
                                Modifier.border(
                                    if (activeDragModule == GamepadModuleId.LEFT_STICK) 2.dp else 1.5.dp,
                                    palette.primary,
                                    CircleShape
                                )
                            } else Modifier
                        )
                ) {
                    VirtualAnalogStick(
                        label = if (isPs) "L" else "LS",
                        enabled = !showLayoutEditor && !isReceiverLocked,
                        onStickMove = { sx, sy -> lx = sx; ly = sy; pushState() },
                        onStickRelease = { lx = 0; ly = 0; pushState() },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("gamepad_left_stick")
                    )
                }

                // 3. Center Console Touchpad Module
                Box(
                    modifier = Modifier
                        .offset { IntOffset(cPadPos.x.roundToInt(), cPadPos.y.roundToInt()) }
                        .size(with(density) { centerW.toDp() }, with(density) { centerH.toDp() })
                        .then(
                            if (showLayoutEditor) {
                                Modifier.border(
                                    if (activeDragModule == GamepadModuleId.CENTER_PAD) 2.dp else 1.5.dp,
                                    palette.primary,
                                    RoundedCornerShape(14.dp)
                                )
                            } else Modifier
                        )
                ) {
                    ConsoleTouchpadSurface(
                        title = if (isPs) "WI-FI DUALSENSE TOUCHPAD" else "WI-FI TOUCHPAD",
                        enabled = !showLayoutEditor && !isReceiverLocked,
                        onTrackpadDrag = onTrackpadDrag,
                        onTrackpadTap = onTrackpadTap,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("gamepad_center_touchpad")
                    )
                }

                // 4. Right Analog Stick Module
                Box(
                    modifier = Modifier
                        .offset { IntOffset(rStickPos.x.roundToInt(), rStickPos.y.roundToInt()) }
                        .size(stickSizeDp)
                        .then(
                            if (showLayoutEditor) {
                                Modifier.border(
                                    if (activeDragModule == GamepadModuleId.RIGHT_STICK) 2.dp else 1.5.dp,
                                    palette.primary,
                                    CircleShape
                                )
                            } else Modifier
                        )
                ) {
                    VirtualAnalogStick(
                        label = if (isPs) "R" else "RS",
                        enabled = !showLayoutEditor && !isReceiverLocked,
                        onStickMove = { sx, sy -> rx = sx; ry = sy; pushState() },
                        onStickRelease = { rx = 0; ry = 0; pushState() },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("gamepad_right_stick")
                    )
                }

                // 5. Face Action Buttons Diamond
                val faceSizeDp = with(density) { faceSizePx.toDp() }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(facePos.x.roundToInt(), facePos.y.roundToInt()) }
                        .size(faceSizeDp)
                        .then(
                            if (showLayoutEditor) {
                                Modifier.border(
                                    if (activeDragModule == GamepadModuleId.FACE_BUTTONS) 2.dp else 1.5.dp,
                                    palette.primary,
                                    RoundedCornerShape(18.dp)
                                )
                            } else Modifier
                        )
                ) {
                    if (isPs) {
                        PlayStationSymbolDiamond(
                            buttonsMask = buttonsMask,
                            enabled = !showLayoutEditor && !isReceiverLocked,
                            onButtonState = { bit, down -> setButtonPressed(bit, down) },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        XboxAbxyDiamond(
                            buttonsMask = buttonsMask,
                            enabled = !showLayoutEditor && !isReceiverLocked,
                            onButtonState = { bit, down -> setButtonPressed(bit, down) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Parent-coordinate drag overlay (zero oscillation/jitter when dragging)
                if (showLayoutEditor) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(25f)
                            .pointerInput(showLayoutEditor, dpadSizePx, stickSizePx, faceSizePx, centerW, centerH) {
                                detectDragGestures(
                                    onDragStart = { startOffset ->
                                        fun hit(pos: Offset, w: Float, h: Float): Boolean =
                                            startOffset.x >= pos.x && startOffset.x <= pos.x + w &&
                                                startOffset.y >= pos.y && startOffset.y <= pos.y + h

                                        activeDragModule = when {
                                            hit(latestFacePos, faceSizePx, faceSizePx) -> GamepadModuleId.FACE_BUTTONS
                                            hit(latestRStickPos, stickSizePx, stickSizePx) -> GamepadModuleId.RIGHT_STICK
                                            hit(latestLStickPos, stickSizePx, stickSizePx) -> GamepadModuleId.LEFT_STICK
                                            hit(latestDpadPos, dpadSizePx, dpadSizePx) -> GamepadModuleId.DPAD
                                            hit(latestCPadPos, centerW, centerH) -> GamepadModuleId.CENTER_PAD
                                            else -> null
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        val mod = activeDragModule ?: return@detectDragGestures
                                        change.consume()
                                        val aw = latestArenaW.coerceAtLeast(1f)
                                        val ah = latestArenaH.coerceAtLeast(1f)
                                        val dxFrac = dragAmount.x / aw
                                        val dyFrac = dragAmount.y / ah
                                        val cur = localOffsets
                                        localOffsets = when (mod) {
                                            GamepadModuleId.DPAD -> {
                                                val b = latestBaseDpad
                                                val nx = ((b.x + (cur.dpadX + dxFrac) * aw).coerceIn(0f, (aw - dpadSizePx).coerceAtLeast(0f)) - b.x) / aw
                                                val ny = ((b.y + (cur.dpadY + dyFrac) * ah).coerceIn(0f, (ah - dpadSizePx).coerceAtLeast(0f)) - b.y) / ah
                                                cur.copy(dpadX = nx, dpadY = ny)
                                            }
                                            GamepadModuleId.LEFT_STICK -> {
                                                val b = latestBaseLStick
                                                val nx = ((b.x + (cur.leftStickX + dxFrac) * aw).coerceIn(0f, (aw - stickSizePx).coerceAtLeast(0f)) - b.x) / aw
                                                val ny = ((b.y + (cur.leftStickY + dyFrac) * ah).coerceIn(0f, (ah - stickSizePx).coerceAtLeast(0f)) - b.y) / ah
                                                cur.copy(leftStickX = nx, leftStickY = ny)
                                            }
                                            GamepadModuleId.FACE_BUTTONS -> {
                                                val b = latestBaseFace
                                                val nx = ((b.x + (cur.faceButtonsX + dxFrac) * aw).coerceIn(0f, (aw - faceSizePx).coerceAtLeast(0f)) - b.x) / aw
                                                val ny = ((b.y + (cur.faceButtonsY + dyFrac) * ah).coerceIn(0f, (ah - faceSizePx).coerceAtLeast(0f)) - b.y) / ah
                                                cur.copy(faceButtonsX = nx, faceButtonsY = ny)
                                            }
                                            GamepadModuleId.RIGHT_STICK -> {
                                                val b = latestBaseRStick
                                                val nx = ((b.x + (cur.rightStickX + dxFrac) * aw).coerceIn(0f, (aw - stickSizePx).coerceAtLeast(0f)) - b.x) / aw
                                                val ny = ((b.y + (cur.rightStickY + dyFrac) * ah).coerceIn(0f, (ah - stickSizePx).coerceAtLeast(0f)) - b.y) / ah
                                                cur.copy(rightStickX = nx, rightStickY = ny)
                                            }
                                            GamepadModuleId.CENTER_PAD -> {
                                                val b = latestBaseCPad
                                                val nx = ((b.x + (cur.centerPadX + dxFrac) * aw).coerceIn(0f, (aw - centerW).coerceAtLeast(0f)) - b.x) / aw
                                                val ny = ((b.y + (cur.centerPadY + dyFrac) * ah).coerceIn(0f, (ah - centerH).coerceAtLeast(0f)) - b.y) / ah
                                                cur.copy(centerPadX = nx, centerPadY = ny)
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        activeDragModule = null
                                        val committed = localOffsets
                                        latestOnUpdateOffsets { committed }
                                    },
                                    onDragCancel = {
                                        activeDragModule = null
                                        val committed = localOffsets
                                        latestOnUpdateOffsets { committed }
                                    }
                                )
                            }
                    )

                    // Floating overlay editor bar (never changes arena height / never shifts controls)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp)
                            .zIndex(35f)
                            .background(palette.surface.copy(alpha = 0.94f), RoundedCornerShape(10.dp))
                            .border(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tuş gruplarını sürükleyip yerleştirin",
                            color = palette.primary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        LayoutSwapChip(
                            label = "Sol Ters",
                            active = swapLeftWing,
                            onClick = onToggleSwapLeftWing
                        )
                        LayoutSwapChip(
                            label = "Sağ Ters",
                            active = swapRightWing,
                            onClick = onToggleSwapRightWing
                        )
                        LayoutSwapChip(
                            label = "Kanatlar",
                            active = mirrorHands,
                            onClick = onToggleMirrorHands
                        )
                    }
                }
            }
        }

        // Bottom Bar: Fixed Bottom-Left DÜZEN (+ SIFIRLA) & Symmetrically Centered L3 | Telemetry | R3
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
        ) {
            val wideEnough = maxWidth >= 560.dp

            if (wideEnough) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GamepadEditTogglePill(
                            showLayoutEditor = showLayoutEditor,
                            onClick = { showLayoutEditor = !showLayoutEditor }
                        )
                        if (showLayoutEditor) {
                            GamepadResetPill(
                                onClick = {
                                    localOffsets = GamepadLayoutOffsets()
                                    onResetLayout()
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShoulderTriggerButton(
                            label = "L3",
                            pressed = (buttonsMask and PadBtn.L3) != 0,
                            onStateChange = { if (!isReceiverLocked) setButtonPressed(PadBtn.L3, it) },
                            modifier = Modifier.width(74.dp)
                        )

                        Surface(
                            color = palette.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .width(120.dp)
                                .fillMaxHeight()
                                .border(1.dp, palette.borderSubtle, RoundedCornerShape(8.dp))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "L(${lx / 10},${ly / 10}) R(${rx / 10},${ry / 10})",
                                    color = palette.textMuted,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        ShoulderTriggerButton(
                            label = "R3",
                            pressed = (buttonsMask and PadBtn.R3) != 0,
                            onStateChange = { if (!isReceiverLocked) setButtonPressed(PadBtn.R3, it) },
                            modifier = Modifier.width(74.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GamepadEditTogglePill(
                        showLayoutEditor = showLayoutEditor,
                        onClick = { showLayoutEditor = !showLayoutEditor }
                    )
                    if (showLayoutEditor) {
                        GamepadResetPill(
                            onClick = {
                                localOffsets = GamepadLayoutOffsets()
                                onResetLayout()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.weight(0.3f))
                    ShoulderTriggerButton(
                        label = "L3",
                        pressed = (buttonsMask and PadBtn.L3) != 0,
                        onStateChange = { if (!isReceiverLocked) setButtonPressed(PadBtn.L3, it) },
                        modifier = Modifier.weight(0.9f)
                    )
                    Surface(
                        color = palette.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                            .border(1.dp, palette.borderSubtle, RoundedCornerShape(8.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "L(${lx / 10},${ly / 10}) R(${rx / 10},${ry / 10})",
                                color = palette.textMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    ShoulderTriggerButton(
                        label = "R3",
                        pressed = (buttonsMask and PadBtn.R3) != 0,
                        onStateChange = { if (!isReceiverLocked) setButtonPressed(PadBtn.R3, it) },
                        modifier = Modifier.weight(0.9f)
                    )
                    Spacer(modifier = Modifier.weight(0.3f))
                }
            }
        }
    }
}

@Composable
private fun GamepadEditTogglePill(
    showLayoutEditor: Boolean,
    onClick: () -> Unit
) {
    val palette = TabRemoteTheme.palette
    Surface(
        color = if (showLayoutEditor) palette.primary else palette.controlThumb,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (showLayoutEditor) palette.primary else palette.borderElevated,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .testTag("gamepad_layout_edit_btn")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (showLayoutEditor) Icons.Default.Check else Icons.Default.Tune,
                contentDescription = if (showLayoutEditor) "Düzenlemeyi Bitir" else "Yerleşimi Düzenle",
                tint = if (showLayoutEditor) palette.onAccent else palette.textPrimary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (showLayoutEditor) "BİTTİ" else "DÜZEN",
                color = if (showLayoutEditor) palette.onAccent else palette.textPrimary,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                softWrap = false
            )
        }
    }
}

@Composable
private fun GamepadResetPill(onClick: () -> Unit) {
    val palette = TabRemoteTheme.palette
    Surface(
        color = palette.surfaceContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .testTag("gamepad_layout_reset_btn")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = "Yerleşimi Sıfırla",
                tint = palette.textPrimary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "SIFIRLA",
                color = palette.textPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                softWrap = false
            )
        }
    }
}

@Composable
private fun LayoutSwapChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    Surface(
        color = if (active) palette.controlThumb else palette.surfaceContainer,
        shape = RoundedCornerShape(7.dp),
        modifier = modifier
            .height(26.dp)
            .clip(RoundedCornerShape(7.dp))
            .border(1.dp, if (active) palette.primary else palette.borderSubtle, RoundedCornerShape(7.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = label,
                tint = if (active) palette.primary else palette.textSecondary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                color = if (active) palette.primary else palette.textPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                softWrap = false,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ConsoleTouchpadSurface(
    title: String,
    enabled: Boolean = true,
    onTrackpadDrag: (dx: Float, dy: Float) -> Unit,
    onTrackpadTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    val dotColor = palette.trackpadGridDot
    val primaryColor = palette.primary

    var touchPos by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.trackpadSurface)
            .border(1.2.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val downEvent = awaitPointerEvent()
                                val downPointers = downEvent.changes.filter { it.pressed }
                                if (downPointers.isEmpty()) continue

                                val startTime = System.currentTimeMillis()
                                var totalMove = 0f
                                var lastPositions = downPointers.associate { it.id to it.position }
                                touchPos = downPointers.first().position

                                while (true) {
                                    val ev = awaitPointerEvent()
                                    val pressed = ev.changes.filter { it.pressed }
                                    if (pressed.isEmpty()) {
                                        val elapsed = System.currentTimeMillis() - startTime
                                        if (elapsed < 260 && totalMove < 16f) {
                                            onTrackpadTap()
                                        }
                                        touchPos = null
                                        break
                                    }

                                    touchPos = pressed.first().position
                                    val ch = pressed.first()
                                    val prev = lastPositions[ch.id] ?: ch.position
                                    val dx = ch.position.x - prev.x
                                    val dy = ch.position.y - prev.y
                                    totalMove += abs(dx) + abs(dy)
                                    ch.consume()
                                    onTrackpadDrag(dx, dy)
                                    lastPositions = pressed.associate { it.id to it.position }
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 22.dp.toPx()
            var x = step
            while (x < size.width) {
                var y = step
                while (y < size.height) {
                    drawCircle(color = dotColor, radius = 1.4f, center = Offset(x, y))
                    y += step
                }
                x += step
            }
            touchPos?.let { p ->
                drawCircle(color = primaryColor.copy(alpha = 0.28f), radius = 26.dp.toPx(), center = p)
                drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = p)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = palette.textMuted.copy(alpha = 0.45f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Wi-Fi Gamepad • İmleç & Tık",
                color = palette.textMuted.copy(alpha = 0.35f),
                fontSize = 8.sp
            )
        }
    }
}

@Composable
private fun CrossDPad(
    currentHat: Int,
    enabled: Boolean = true,
    onHatChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    var boxSize by remember { mutableStateOf(IntSize(100, 100)) }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(palette.surface)
            .border(1.2.dp, palette.borderSubtle, CircleShape)
            .onSizeChanged { boxSize = it }
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val active = event.changes.firstOrNull { it.pressed }
                                if (active == null) {
                                    onHatChange(8)
                                    continue
                                }
                                active.consume()
                                val cx = boxSize.width / 2f
                                val cy = boxSize.height / 2f
                                val dx = active.position.x - cx
                                val dy = active.position.y - cy
                                val dist = hypot(dx.toDouble(), dy.toDouble())
                                if (dist < boxSize.width * 0.14f) {
                                    onHatChange(8)
                                } else {
                                    val deg = ((Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())) + 360.0) % 360.0)
                                    val sector = (((deg + 22.5) / 45.0).toInt()) % 8
                                    onHatChange(sector)
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
            .testTag("gamepad_dpad"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = "Up",
            tint = if (currentHat in listOf(7, 0, 1)) palette.primary else palette.textSecondary,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .size(26.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Down",
            tint = if (currentHat in listOf(3, 4, 5)) palette.primary else palette.textSecondary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .size(26.dp)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Left",
            tint = if (currentHat in listOf(5, 6, 7)) palette.primary else palette.textSecondary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .size(24.dp)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Right",
            tint = if (currentHat in listOf(1, 2, 3)) palette.primary else palette.textSecondary,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .size(24.dp)
        )

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (currentHat != 8) palette.primary else palette.surfaceContainer)
                .border(1.dp, palette.borderElevated, CircleShape)
        )
    }
}

@Composable
private fun VirtualAnalogStick(
    label: String,
    enabled: Boolean = true,
    onStickMove: (Int, Int) -> Unit,
    onStickRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    val ringColor = palette.borderElevated
    val primaryColor = palette.primary
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize(100, 100)) }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(palette.surface)
            .border(1.2.dp, palette.borderSubtle, CircleShape)
            .onSizeChanged { boxSize = it }
            .then(
                if (enabled) {
                    Modifier.pointerInput(label) {
                        awaitPointerEventScope {
                            while (true) {
                                val downEvent = awaitPointerEvent()
                                val firstDown = downEvent.changes.firstOrNull { it.pressed } ?: continue
                                val pointerId = firstDown.id
                                firstDown.consume()

                                fun updateFromPos(pos: Offset) {
                                    val center = Offset(boxSize.width / 2f, boxSize.height / 2f)
                                    val maxRadius = (minOf(boxSize.width, boxSize.height) / 2f) * 0.64f
                                    val delta = pos - center
                                    val dist = hypot(delta.x.toDouble(), delta.y.toDouble()).toFloat()
                                    val clamped = if (dist > maxRadius && maxRadius > 0f) {
                                        val angle = atan2(delta.y.toDouble(), delta.x.toDouble())
                                        Offset((cos(angle) * maxRadius).toFloat(), (sin(angle) * maxRadius).toFloat())
                                    } else {
                                        delta
                                    }
                                    knobOffset = clamped
                                    val normX = ((clamped.x / maxRadius.coerceAtLeast(1f)) * 127f).roundToInt().coerceIn(-127, 127)
                                    val normY = ((clamped.y / maxRadius.coerceAtLeast(1f)) * 127f).roundToInt().coerceIn(-127, 127)
                                    onStickMove(normX, normY)
                                }

                                updateFromPos(firstDown.position)

                                while (true) {
                                    val ev = awaitPointerEvent()
                                    val tracked = ev.changes.firstOrNull { it.id == pointerId }
                                    if (tracked == null || !tracked.pressed) {
                                        knobOffset = Offset.Zero
                                        onStickRelease()
                                        break
                                    }
                                    tracked.consume()
                                    updateFromPos(tracked.position)
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = ringColor,
                radius = size.minDimension * 0.34f,
                center = center,
                style = Stroke(width = 1.6.dp.toPx())
            )
            val thumbCenter = center + knobOffset
            drawCircle(
                color = primaryColor.copy(alpha = 0.25f),
                radius = 23.dp.toPx(),
                center = thumbCenter
            )
            drawCircle(
                color = primaryColor,
                radius = 17.dp.toPx(),
                center = thumbCenter
            )
        }
        if (knobOffset == Offset.Zero) {
            Text(
                text = label,
                color = palette.onAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun PlayStationSymbolDiamond(
    buttonsMask: Int,
    enabled: Boolean = true,
    onButtonState: (bit: Int, down: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    Box(modifier = modifier) {
        PsSymbolButton(
            symbol = PsSymbol.TRIANGLE,
            accent = GreenSuccess,
            pressed = (buttonsMask and PadBtn.Y_TRIANGLE) != 0,
            enabled = enabled,
            onStateChange = { onButtonState(PadBtn.Y_TRIANGLE, it) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .testTag("gamepad_btn_triangle")
        )
        PsSymbolButton(
            symbol = PsSymbol.SQUARE,
            accent = Color(0xFFF48FB1),
            pressed = (buttonsMask and PadBtn.X_SQUARE) != 0,
            enabled = enabled,
            onStateChange = { onButtonState(PadBtn.X_SQUARE, it) },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .testTag("gamepad_btn_square")
        )
        PsSymbolButton(
            symbol = PsSymbol.CIRCLE,
            accent = palette.primary,
            pressed = (buttonsMask and PadBtn.B_CIRCLE) != 0,
            enabled = enabled,
            onStateChange = { onButtonState(PadBtn.B_CIRCLE, it) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .testTag("gamepad_btn_circle")
        )
        PsSymbolButton(
            symbol = PsSymbol.CROSS,
            accent = Color(0xFF4FC3F7),
            pressed = (buttonsMask and PadBtn.A_CROSS) != 0,
            enabled = enabled,
            onStateChange = { onButtonState(PadBtn.A_CROSS, it) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .testTag("gamepad_btn_cross")
        )
    }
}

@Composable
private fun PsSymbolButton(
    symbol: PsSymbol,
    accent: Color,
    pressed: Boolean,
    enabled: Boolean = true,
    onStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    Surface(
        color = if (pressed) accent.copy(alpha = 0.35f) else palette.surface,
        shape = CircleShape,
        modifier = modifier
            .size(44.dp)
            .border(1.2.dp, if (pressed) accent else palette.borderSubtle, CircleShape)
            .then(
                if (enabled) {
                    Modifier.pointerInput(symbol) {
                        detectTapGestures(
                            onPress = {
                                onStateChange(true)
                                tryAwaitRelease()
                                onStateChange(false)
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(11.dp)
        ) {
            val w = size.width
            val h = size.height
            val strokeColor = if (pressed) Color.White else accent
            val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (symbol) {
                PsSymbol.TRIANGLE -> {
                    val path = Path().apply {
                        moveTo(w / 2f, 1f)
                        lineTo(w - 1f, h - 1f)
                        lineTo(1f, h - 1f)
                        close()
                    }
                    drawPath(path, color = strokeColor, style = stroke)
                }
                PsSymbol.CIRCLE -> {
                    drawCircle(color = strokeColor, radius = (minOf(w, h) / 2f) - 1f, style = stroke)
                }
                PsSymbol.CROSS -> {
                    drawLine(strokeColor, Offset(1f, 1f), Offset(w - 1f, h - 1f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(strokeColor, Offset(w - 1f, 1f), Offset(1f, h - 1f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
                PsSymbol.SQUARE -> {
                    drawRect(
                        color = strokeColor,
                        topLeft = Offset(1f, 1f),
                        size = androidx.compose.ui.geometry.Size(w - 2f, h - 2f),
                        style = stroke
                    )
                }
            }
        }
    }
}

@Composable
private fun XboxAbxyDiamond(
    buttonsMask: Int,
    enabled: Boolean = true,
    onButtonState: (bit: Int, down: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    Box(modifier = modifier) {
        XboxFaceButton(
            label = "Y",
            accent = Color(0xFFFFD54F),
            pressed = (buttonsMask and PadBtn.Y_TRIANGLE) != 0,
            enabled = enabled,
            onStateChange = { onButtonState(PadBtn.Y_TRIANGLE, it) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .testTag("gamepad_btn_y")
        )
        XboxFaceButton(
            label = "X",
            accent = Color(0xFF4FC3F7),
            pressed = (buttonsMask and PadBtn.X_SQUARE) != 0,
            enabled = enabled,
            onStateChange = { onButtonState(PadBtn.X_SQUARE, it) },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .testTag("gamepad_btn_x")
        )
        XboxFaceButton(
            label = "B",
            accent = palette.primary,
            pressed = (buttonsMask and PadBtn.B_CIRCLE) != 0,
            enabled = enabled,
            onStateChange = { onButtonState(PadBtn.B_CIRCLE, it) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .testTag("gamepad_btn_b")
        )
        XboxFaceButton(
            label = "A",
            accent = GreenSuccess,
            pressed = (buttonsMask and PadBtn.A_CROSS) != 0,
            enabled = enabled,
            onStateChange = { onButtonState(PadBtn.A_CROSS, it) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .testTag("gamepad_btn_a")
        )
    }
}

@Composable
private fun XboxFaceButton(
    label: String,
    accent: Color,
    pressed: Boolean,
    enabled: Boolean = true,
    onStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    Surface(
        color = if (pressed) accent.copy(alpha = 0.35f) else palette.surface,
        shape = CircleShape,
        modifier = modifier
            .size(44.dp)
            .border(1.2.dp, if (pressed) accent else palette.borderSubtle, CircleShape)
            .then(
                if (enabled) {
                    Modifier.pointerInput(label) {
                        detectTapGestures(
                            onPress = {
                                onStateChange(true)
                                tryAwaitRelease()
                                onStateChange(false)
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (pressed) Color.White else accent,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun ShoulderTriggerButton(
    label: String,
    pressed: Boolean,
    onStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    Surface(
        color = if (pressed) palette.primary else palette.surface,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxHeight()
            .border(
                1.dp,
                if (pressed) palette.primary else palette.borderSubtle,
                RoundedCornerShape(8.dp)
            )
            .pointerInput(label) {
                detectTapGestures(
                    onPress = {
                        onStateChange(true)
                        tryAwaitRelease()
                        onStateChange(false)
                    }
                )
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (pressed) palette.onAccent else palette.textPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                softWrap = false,
                maxLines = 1
            )
        }
    }
}
