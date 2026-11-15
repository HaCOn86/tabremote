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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetooth.HidConstants
import com.example.bluetooth.TouchContact
import com.example.model.GamepadStyle
import com.example.model.HidProfileMode
import com.example.ui.theme.BackgroundPitch
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RubyPrimary
import com.example.ui.theme.SurfaceBorderRuby
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.theme.SurfaceContainerCharcoal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrackpadGridDot
import com.example.ui.theme.TrackpadSurface
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

@Composable
fun GamepadScreenContent(
    gamepadStyle: GamepadStyle,
    hidProfileMode: HidProfileMode,
    isWifiMode: Boolean = false,
    swapLeftWing: Boolean = false,
    swapRightWing: Boolean = false,
    mirrorHands: Boolean = false,
    onToggleStyle: () -> Unit,
    onToggleSwapLeftWing: () -> Unit = {},
    onToggleSwapRightWing: () -> Unit = {},
    onToggleMirrorHands: () -> Unit = {},
    onExitFullscreen: () -> Unit,
    onGamepadState: (buttonsMask: Int, hat: Int, lx: Int, ly: Int, rx: Int, ry: Int) -> Unit,
    onTouchpadContacts: (contacts: List<TouchContact>, clickpadPressed: Boolean) -> Unit,
    onTrackpadDrag: (dx: Float, dy: Float) -> Unit,
    onTrackpadTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var buttonsMask by remember { mutableIntStateOf(0) }
    var hatSwitch by remember { mutableIntStateOf(8) }
    var lx by remember { mutableIntStateOf(0) }
    var ly by remember { mutableIntStateOf(0) }
    var rx by remember { mutableIntStateOf(0) }
    var ry by remember { mutableIntStateOf(0) }
    var showLayoutEditor by remember { mutableStateOf(false) }

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
            .background(BackgroundPitch)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Top Shoulder Triggers Row (PS5/XBOX toggle fixed width 82.dp so shoulders NEVER jump!)
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
                modifier = Modifier.weight(0.95f)
            )
            ShoulderTriggerButton(
                label = if (isPs) "L1" else "LB",
                pressed = (buttonsMask and PadBtn.L1_LB) != 0,
                onStateChange = { setButtonPressed(PadBtn.L1_LB, it) },
                modifier = Modifier.weight(0.95f)
            )

            // Fixed 82.dp Width PS5 / XBOX Style Toggle
            Surface(
                color = SurfaceCharcoal,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .width(82.dp)
                    .fillMaxHeight()
                    .border(1.dp, RubyPrimary.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleStyle() }
                    .testTag("gamepad_style_switch")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = "Stil Değiştir",
                        tint = RubyPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPs) "PS5" else "XBOX",
                        color = RubyPrimary,
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
                modifier = Modifier.weight(0.8f)
            )

            // Center Home Jewel
            Surface(
                color = if ((buttonsMask and PadBtn.HOME) != 0) RubyPrimary else SurfaceCharcoal,
                shape = CircleShape,
                modifier = Modifier
                    .size(38.dp)
                    .border(1.4.dp, RubyPrimary, CircleShape)
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
                        tint = if ((buttonsMask and PadBtn.HOME) != 0) Color.White else RubyPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            ShoulderTriggerButton(
                label = if (isPs) "OPT" else "MENU",
                pressed = (buttonsMask and PadBtn.START_MENU) != 0,
                onStateChange = { setButtonPressed(PadBtn.START_MENU, it) },
                modifier = Modifier.weight(0.8f)
            )

            // Wing / Analog Layout Customization Toggle
            Surface(
                color = if (showLayoutEditor) RubyPrimary else SurfaceCharcoal,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .width(74.dp)
                    .fillMaxHeight()
                    .border(1.dp, if (showLayoutEditor) RubyPrimary else SurfaceBorderRuby, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showLayoutEditor = !showLayoutEditor }
                    .testTag("gamepad_layout_edit_btn")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Yerleşim Düzenle",
                        tint = if (showLayoutEditor) Color.White else RubyPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "DÜZEN",
                        color = if (showLayoutEditor) Color.White else RubyPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        softWrap = false
                    )
                }
            }

            Surface(
                color = RubyPrimary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .border(1.dp, RubyPrimary, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onExitFullscreen() }
                    .testTag("gamepad_exit_btn")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Çıkış",
                        tint = RubyPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("ÇIKIŞ", color = RubyPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, softWrap = false)
                }
            }

            ShoulderTriggerButton(
                label = if (isPs) "R1" else "RB",
                pressed = (buttonsMask and PadBtn.R1_RB) != 0,
                onStateChange = { setButtonPressed(PadBtn.R1_RB, it) },
                modifier = Modifier.weight(0.95f)
            )
            ShoulderTriggerButton(
                label = if (isPs) "R2" else "RT",
                pressed = (buttonsMask and PadBtn.R2_RT) != 0,
                onStateChange = { setButtonPressed(PadBtn.R2_RT, it) },
                modifier = Modifier.weight(0.95f)
            )
        }

        // Animated Gamepad Wing & Stick Position Customizer
        AnimatedVisibility(
            visible = showLayoutEditor,
            enter = expandVertically(tween(200)) + fadeIn(tween(150)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(120))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCharcoal, RoundedCornerShape(10.dp))
                    .border(1.dp, RubyPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LayoutSwapChip(
                    label = "Sol Analog ⇄ D-Pad",
                    active = swapLeftWing,
                    onClick = onToggleSwapLeftWing,
                    modifier = Modifier.weight(1f)
                )
                LayoutSwapChip(
                    label = "Sağ Analog ⇄ Aksiyon",
                    active = swapRightWing,
                    onClick = onToggleSwapRightWing,
                    modifier = Modifier.weight(1f)
                )
                LayoutSwapChip(
                    label = "Sol / Sağ Kanat Ters",
                    active = mirrorHands,
                    onClick = onToggleMirrorHands,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        val leftWingContent: @Composable () -> Unit = {
            // Effective order flips Xbox <-> PS or user's manual swapLeftWing
            val dpadOnTop = if (isPs) !swapLeftWing else swapLeftWing
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (dpadOnTop) {
                    CrossDPad(
                        currentHat = hatSwitch,
                        onHatChange = { hatSwitch = it; pushState() },
                        modifier = Modifier.size(124.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (mirrorHands) Arrangement.Start else Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VirtualAnalogStick(
                            label = if (isPs) "L" else "LS",
                            onStickMove = { sx, sy -> lx = sx; ly = sy; pushState() },
                            onStickRelease = { lx = 0; ly = 0; pushState() },
                            modifier = Modifier.size(116.dp).testTag("gamepad_left_stick")
                        )
                    }
                } else {
                    VirtualAnalogStick(
                        label = if (isPs) "L" else "LS",
                        onStickMove = { sx, sy -> lx = sx; ly = sy; pushState() },
                        onStickRelease = { lx = 0; ly = 0; pushState() },
                        modifier = Modifier.size(120.dp).testTag("gamepad_left_stick")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (mirrorHands) Arrangement.Start else Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CrossDPad(
                            currentHat = hatSwitch,
                            onHatChange = { hatSwitch = it; pushState() },
                            modifier = Modifier.size(116.dp)
                        )
                    }
                }
            }
        }

        val rightWingContent: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val faceDiamond: @Composable () -> Unit = {
                    if (isPs) {
                        PlayStationSymbolDiamond(
                            buttonsMask = buttonsMask,
                            onButtonState = { bit, down -> setButtonPressed(bit, down) },
                            modifier = Modifier.size(132.dp)
                        )
                    } else {
                        XboxAbxyDiamond(
                            buttonsMask = buttonsMask,
                            onButtonState = { bit, down -> setButtonPressed(bit, down) },
                            modifier = Modifier.size(132.dp)
                        )
                    }
                }
                val rightStick: @Composable () -> Unit = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (mirrorHands) Arrangement.End else Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VirtualAnalogStick(
                            label = if (isPs) "R" else "RS",
                            onStickMove = { sx, sy -> rx = sx; ry = sy; pushState() },
                            onStickRelease = { rx = 0; ry = 0; pushState() },
                            modifier = Modifier.size(116.dp).testTag("gamepad_right_stick")
                        )
                    }
                }

                if (!swapRightWing) {
                    faceDiamond()
                    rightStick()
                } else {
                    rightStick()
                    faceDiamond()
                }
            }
        }

        // Main Dual-Analog Console Body (Supports Mirrored Wings + Wing Swap)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (mirrorHands) {
                rightWingContent()
            } else {
                leftWingContent()
            }

            // CENTER BRIDGE
            Column(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ConsoleTouchpadSurface(
                    title = if (isPs) "DUALSENSE TOUCHPAD" else "TABLET TOUCHPAD",
                    hidProfileMode = hidProfileMode,
                    isWifiMode = isWifiMode,
                    onTouchpadContacts = onTouchpadContacts,
                    onTrackpadDrag = onTrackpadDrag,
                    onTrackpadTap = onTrackpadTap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("gamepad_center_touchpad")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShoulderTriggerButton(
                        label = "L3",
                        pressed = (buttonsMask and PadBtn.L3) != 0,
                        onStateChange = { setButtonPressed(PadBtn.L3, it) },
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = SurfaceCharcoal,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1.6f)
                            .fillMaxHeight()
                            .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(6.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "L(${lx / 10},${ly / 10}) R(${rx / 10},${ry / 10})",
                                color = TextMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    ShoulderTriggerButton(
                        label = "R3",
                        pressed = (buttonsMask and PadBtn.R3) != 0,
                        onStateChange = { setButtonPressed(PadBtn.R3, it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (mirrorHands) {
                leftWingContent()
            } else {
                rightWingContent()
            }
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
    Surface(
        color = if (active) RubyPrimary.copy(alpha = 0.22f) else SurfaceContainerCharcoal,
        shape = RoundedCornerShape(7.dp),
        modifier = modifier
            .height(28.dp)
            .border(1.dp, if (active) RubyPrimary else SurfaceBorderRuby, RoundedCornerShape(7.dp))
            .clip(RoundedCornerShape(7.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = label,
                tint = if (active) RubyPrimary else TextMuted,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (active) RubyPrimary else TextSecondary,
                fontSize = 9.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                softWrap = false,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ConsoleTouchpadSurface(
    title: String,
    hidProfileMode: HidProfileMode,
    isWifiMode: Boolean,
    onTouchpadContacts: (contacts: List<TouchContact>, clickpadPressed: Boolean) -> Unit,
    onTrackpadDrag: (dx: Float, dy: Float) -> Unit,
    onTrackpadTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var touchPos by remember { mutableStateOf<Offset?>(null) }
    var panelSize by remember { mutableStateOf(IntSize(400, 220)) }
    val usePtp = !isWifiMode && hidProfileMode == HidProfileMode.PRECISION_TOUCHPAD

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(TrackpadSurface)
            .border(1.3.dp, SurfaceBorderRuby, RoundedCornerShape(14.dp))
            .onSizeChanged { panelSize = it }
            .pointerInput(usePtp) {
                awaitPointerEventScope {
                    while (true) {
                        val downEvent = awaitPointerEvent()
                        val downPointers = downEvent.changes.filter { it.pressed }
                        if (downPointers.isEmpty()) continue

                        val startTime = System.currentTimeMillis()
                        var totalMove = 0f
                        var lastPositions = downPointers.associate { it.id to it.position }
                        val pointerToSlot = mutableMapOf<PointerId, Int>()
                        val lastCoords = mutableMapOf<Int, Pair<Int, Int>>()

                        fun toLogical(p: Offset): Pair<Int, Int> {
                            val w = panelSize.width.coerceAtLeast(1).toFloat()
                            val h = panelSize.height.coerceAtLeast(1).toFloat()
                            val x = ((p.x / w) * HidConstants.TP_LOGICAL_MAX_X).roundToInt()
                                .coerceIn(0, HidConstants.TP_LOGICAL_MAX_X)
                            val y = ((p.y / h) * HidConstants.TP_LOGICAL_MAX_Y).roundToInt()
                                .coerceIn(0, HidConstants.TP_LOGICAL_MAX_Y)
                            return x to y
                        }

                        touchPos = downPointers.first().position

                        if (usePtp) {
                            val initContacts = downPointers.take(2).mapIndexed { idx, ch ->
                                pointerToSlot[ch.id] = idx
                                val coords = toLogical(ch.position)
                                lastCoords[idx] = coords
                                TouchContact(idx, true, coords.first, coords.second)
                            }
                            if (initContacts.isNotEmpty()) {
                                onTouchpadContacts(initContacts, false)
                            }
                        }

                        while (true) {
                            val ev = awaitPointerEvent()
                            val pressed = ev.changes.filter { it.pressed }
                            if (pressed.isEmpty()) {
                                val elapsed = System.currentTimeMillis() - startTime
                                if (usePtp) {
                                    val rel = pointerToSlot.values.map { slot ->
                                        val c = lastCoords[slot] ?: (1600 to 1000)
                                        TouchContact(slot, false, c.first, c.second)
                                    }
                                    if (rel.isNotEmpty()) onTouchpadContacts(rel, false)
                                    onTouchpadContacts(emptyList(), false)
                                } else if (elapsed < 260 && totalMove < 16f) {
                                    onTrackpadTap()
                                }
                                touchPos = null
                                break
                            }

                            touchPos = pressed.first().position

                            if (usePtp) {
                                val contacts = pressed.take(2).mapIndexed { idx, ch ->
                                    val prev = lastPositions[ch.id] ?: ch.position
                                    totalMove += abs(ch.position.x - prev.x) + abs(ch.position.y - prev.y)
                                    ch.consume()
                                    val slot = pointerToSlot.getOrPut(ch.id) { idx }
                                    val c = toLogical(ch.position)
                                    lastCoords[slot] = c
                                    TouchContact(slot, true, c.first, c.second)
                                }
                                if (contacts.isNotEmpty()) onTouchpadContacts(contacts, false)
                            } else {
                                val ch = pressed.first()
                                val prev = lastPositions[ch.id] ?: ch.position
                                val dx = ch.position.x - prev.x
                                val dy = ch.position.y - prev.y
                                totalMove += abs(dx) + abs(dy)
                                ch.consume()
                                onTrackpadDrag(dx, dy)
                            }

                            lastPositions = pressed.associate { it.id to it.position }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 22.dp.toPx()
            var x = step
            while (x < size.width) {
                var y = step
                while (y < size.height) {
                    drawCircle(color = TrackpadGridDot, radius = 1.4f, center = Offset(x, y))
                    y += step
                }
                x += step
            }
            touchPos?.let { p ->
                drawCircle(color = RubyPrimary.copy(alpha = 0.28f), radius = 26.dp.toPx(), center = p)
                drawCircle(color = RubyPrimary, radius = 5.dp.toPx(), center = p)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = TextMuted.copy(alpha = 0.45f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "İmleç & Tık",
                color = TextMuted.copy(alpha = 0.35f),
                fontSize = 8.sp
            )
        }
    }
}

@Composable
private fun CrossDPad(
    currentHat: Int,
    onHatChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var boxSize by remember { mutableStateOf(IntSize(100, 100)) }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(SurfaceCharcoal.copy(alpha = 0.7f))
            .border(1.4.dp, SurfaceBorderRuby, CircleShape)
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
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
            .testTag("gamepad_dpad"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = "Up",
            tint = if (currentHat in listOf(7, 0, 1)) RubyPrimary else TextSecondary,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp).size(26.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Down",
            tint = if (currentHat in listOf(3, 4, 5)) RubyPrimary else TextSecondary,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).size(26.dp)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Left",
            tint = if (currentHat in listOf(5, 6, 7)) RubyPrimary else TextSecondary,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size(24.dp)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Right",
            tint = if (currentHat in listOf(1, 2, 3)) RubyPrimary else TextSecondary,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).size(24.dp)
        )

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (currentHat != 8) RubyPrimary.copy(alpha = 0.35f) else SurfaceContainerCharcoal)
                .border(1.dp, SurfaceBorderRuby, CircleShape)
        )
    }
}

@Composable
private fun VirtualAnalogStick(
    label: String,
    onStickMove: (Int, Int) -> Unit,
    onStickRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize(100, 100)) }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(SurfaceCharcoal)
            .border(1.4.dp, SurfaceBorderRuby, CircleShape)
            .onSizeChanged { boxSize = it }
            .pointerInput(label) {
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
                            } else delta
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
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = SurfaceBorderRuby,
                radius = size.minDimension * 0.34f,
                center = center,
                style = Stroke(width = 1.8.dp.toPx())
            )
            val thumbCenter = center + knobOffset
            drawCircle(
                color = RubyPrimary.copy(alpha = 0.32f),
                radius = 24.dp.toPx(),
                center = thumbCenter
            )
            drawCircle(
                color = RubyPrimary,
                radius = 18.dp.toPx(),
                center = thumbCenter
            )
        }
        if (knobOffset == Offset.Zero) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun PlayStationSymbolDiamond(
    buttonsMask: Int,
    onButtonState: (bit: Int, down: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        PsSymbolButton(
            symbol = PsSymbol.TRIANGLE,
            accent = GreenSuccess,
            pressed = (buttonsMask and PadBtn.Y_TRIANGLE) != 0,
            onStateChange = { onButtonState(PadBtn.Y_TRIANGLE, it) },
            modifier = Modifier.align(Alignment.TopCenter).testTag("gamepad_btn_triangle")
        )
        PsSymbolButton(
            symbol = PsSymbol.SQUARE,
            accent = Color(0xFFF48FB1),
            pressed = (buttonsMask and PadBtn.X_SQUARE) != 0,
            onStateChange = { onButtonState(PadBtn.X_SQUARE, it) },
            modifier = Modifier.align(Alignment.CenterStart).testTag("gamepad_btn_square")
        )
        PsSymbolButton(
            symbol = PsSymbol.CIRCLE,
            accent = RubyPrimary,
            pressed = (buttonsMask and PadBtn.B_CIRCLE) != 0,
            onStateChange = { onButtonState(PadBtn.B_CIRCLE, it) },
            modifier = Modifier.align(Alignment.CenterEnd).testTag("gamepad_btn_circle")
        )
        PsSymbolButton(
            symbol = PsSymbol.CROSS,
            accent = Color(0xFF4FC3F7),
            pressed = (buttonsMask and PadBtn.A_CROSS) != 0,
            onStateChange = { onButtonState(PadBtn.A_CROSS, it) },
            modifier = Modifier.align(Alignment.BottomCenter).testTag("gamepad_btn_cross")
        )
    }
}

@Composable
private fun PsSymbolButton(
    symbol: PsSymbol,
    accent: Color,
    pressed: Boolean,
    onStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (pressed) accent.copy(alpha = 0.35f) else SurfaceCharcoal,
        shape = CircleShape,
        modifier = modifier
            .size(46.dp)
            .border(1.4.dp, if (pressed) accent else SurfaceBorderRuby, CircleShape)
            .pointerInput(symbol) {
                detectTapGestures(
                    onPress = {
                        onStateChange(true)
                        tryAwaitRelease()
                        onStateChange(false)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val w = size.width
            val h = size.height
            val strokeColor = if (pressed) Color.White else accent
            val stroke = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
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
                    drawRect(color = strokeColor, topLeft = Offset(1f, 1f), size = androidx.compose.ui.geometry.Size(w - 2f, h - 2f), style = stroke)
                }
            }
        }
    }
}

@Composable
private fun XboxAbxyDiamond(
    buttonsMask: Int,
    onButtonState: (bit: Int, down: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        XboxFaceButton(
            label = "Y",
            accent = Color(0xFFFFD54F),
            pressed = (buttonsMask and PadBtn.Y_TRIANGLE) != 0,
            onStateChange = { onButtonState(PadBtn.Y_TRIANGLE, it) },
            modifier = Modifier.align(Alignment.TopCenter).testTag("gamepad_btn_y")
        )
        XboxFaceButton(
            label = "X",
            accent = Color(0xFF4FC3F7),
            pressed = (buttonsMask and PadBtn.X_SQUARE) != 0,
            onStateChange = { onButtonState(PadBtn.X_SQUARE, it) },
            modifier = Modifier.align(Alignment.CenterStart).testTag("gamepad_btn_x")
        )
        XboxFaceButton(
            label = "B",
            accent = RubyPrimary,
            pressed = (buttonsMask and PadBtn.B_CIRCLE) != 0,
            onStateChange = { onButtonState(PadBtn.B_CIRCLE, it) },
            modifier = Modifier.align(Alignment.CenterEnd).testTag("gamepad_btn_b")
        )
        XboxFaceButton(
            label = "A",
            accent = GreenSuccess,
            pressed = (buttonsMask and PadBtn.A_CROSS) != 0,
            onStateChange = { onButtonState(PadBtn.A_CROSS, it) },
            modifier = Modifier.align(Alignment.BottomCenter).testTag("gamepad_btn_a")
        )
    }
}

@Composable
private fun XboxFaceButton(
    label: String,
    accent: Color,
    pressed: Boolean,
    onStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (pressed) accent.copy(alpha = 0.35f) else SurfaceCharcoal,
        shape = CircleShape,
        modifier = modifier
            .size(46.dp)
            .border(1.4.dp, if (pressed) accent else SurfaceBorderRuby, CircleShape)
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
                color = if (pressed) Color.White else accent,
                fontSize = 16.sp,
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
    Surface(
        color = if (pressed) RubyPrimary.copy(alpha = 0.35f) else SurfaceCharcoal,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxHeight()
            .border(
                1.2.dp,
                if (pressed) RubyPrimary else SurfaceBorderRuby,
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
                color = if (pressed) Color.White else TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                softWrap = false,
                maxLines = 1
            )
        }
    }
}
