package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetooth.HidConstants
import com.example.ui.theme.BackgroundPitch
import com.example.ui.theme.RubyAccent
import com.example.ui.theme.RubyPrimary
import com.example.ui.theme.SurfaceBorderRuby
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.theme.SurfaceContainerCharcoal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private data class PcKeyDef(
    val primary: String,
    val shiftLabel: String = "",
    val hidCode: Int,
    val weight: Float = 1f,
    val isAccent: Boolean = false
)

@Composable
fun LandscapePcKeyboardContent(
    isTurkishLayout: Boolean,
    showNumberRow: Boolean,
    showNumpad: Boolean,
    splitKeyboard: Boolean,
    splitGapWeight: Float = 1.6f,
    onToggleLayout: () -> Unit,
    onToggleNumberRow: () -> Unit,
    onToggleNumpad: () -> Unit,
    onToggleSplit: () -> Unit,
    onUpdateSplitGapWeight: (Float) -> Unit = {},
    onExitFullscreen: () -> Unit,
    onSendKey: (hidKeyCode: Int, modifier: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var stickyModifiers by remember { mutableIntStateOf(0) }
    var capsLockActive by remember { mutableStateOf(false) }

    val effectiveSplit = splitKeyboard && !showNumpad
    val animatedGapWeight by animateFloatAsState(
        targetValue = splitGapWeight.coerceIn(0.4f, 3.2f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "pc_split_gap"
    )

    fun toggleMod(modMask: Int) {
        stickyModifiers = stickyModifiers xor modMask
    }

    fun dispatchKey(hidCode: Int) {
        onSendKey(hidCode, stickyModifiers)
        if (stickyModifiers != 0) {
            stickyModifiers = 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPitch)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Immersive Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarChip(
                label = if (isTurkishLayout) "DÜZEN: TR-Q" else "DÜZEN: US QWERTY",
                selected = true,
                onClick = onToggleLayout,
                modifier = Modifier.testTag("pc_kb_layout_btn")
            )
            ToolbarChip(
                label = if (showNumberRow) "1-0 AÇIK" else "1-0 KAPALI",
                selected = showNumberRow,
                onClick = onToggleNumberRow,
                modifier = Modifier.testTag("pc_kb_numrow_btn")
            )
            ToolbarChip(
                label = if (showNumpad) "NUMPAD AÇIK" else "NUMPAD KAPALI",
                selected = showNumpad,
                onClick = onToggleNumpad,
                modifier = Modifier.testTag("pc_kb_numpad_btn")
            )
            if (!showNumpad) {
                ToolbarChip(
                    label = if (splitKeyboard) "BÖLÜNME: AÇIK" else "BÖLÜNME: KAPALI",
                    selected = splitKeyboard,
                    onClick = onToggleSplit,
                    modifier = Modifier.testTag("pc_kb_split_btn")
                )
            }

            // Draggable & Quick-Step Split Gap Controller when split keyboard is enabled
            AnimatedVisibility(visible = effectiveSplit) {
                Surface(
                    color = SurfaceCharcoal,
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier
                        .fillMaxHeight()
                        .border(1.dp, RubyPrimary.copy(alpha = 0.55f), RoundedCornerShape(7.dp))
                        .testTag("pc_kb_split_handle")
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .pointerInput(splitGapWeight) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    change.consume()
                                    onUpdateSplitGapWeight((splitGapWeight + dragAmount * 0.01f).coerceIn(0.4f, 3.2f))
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "−",
                            color = RubyPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onUpdateSplitGapWeight((splitGapWeight - 0.35f).coerceIn(0.4f, 3.2f)) }
                                .padding(horizontal = 4.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.DragIndicator,
                            contentDescription = "Boşluğu Sürükle",
                            tint = RubyPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "BOŞLUK",
                            color = TextSecondary,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "+",
                            color = RubyPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onUpdateSplitGapWeight((splitGapWeight + 0.35f).coerceIn(0.4f, 3.2f)) }
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                color = RubyPrimary.copy(alpha = 0.22f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxHeight()
                    .border(1.dp, RubyPrimary, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onExitFullscreen() }
                    .testTag("pc_kb_exit_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Tam Ekrandan Çık",
                        tint = RubyPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ÇIKIŞ",
                        color = RubyPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Main Keyboard + Optional Numpad Container
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(if (showNumpad) 3.4f else 1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Row 0: Esc + F1..F6 | [split gap] | F7..F12 + PrtSc + Del
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(0.85f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        PcKeyTile("Esc", hidCode = HidConstants.KEY_ESC, accent = true, onTap = { dispatchKey(HidConstants.KEY_ESC) })
                        val leftFKeys = listOf(
                            "F1" to HidConstants.KEY_F1, "F2" to HidConstants.KEY_F2,
                            "F3" to HidConstants.KEY_F3, "F4" to HidConstants.KEY_F4,
                            "F5" to HidConstants.KEY_F5, "F6" to HidConstants.KEY_F6
                        )
                        leftFKeys.forEach { (lbl, code) ->
                            PcKeyTile(lbl, hidCode = code, onTap = { dispatchKey(code) })
                        }
                        if (effectiveSplit) {
                            PcSplitGapSpacer(
                                weight = animatedGapWeight,
                                splitGapWeight = splitGapWeight,
                                onUpdateSplitGapWeight = onUpdateSplitGapWeight
                            )
                        }
                        val rightFKeys = listOf(
                            "F7" to HidConstants.KEY_F7, "F8" to HidConstants.KEY_F8,
                            "F9" to HidConstants.KEY_F9, "F10" to HidConstants.KEY_F10,
                            "F11" to HidConstants.KEY_F11, "F12" to HidConstants.KEY_F12
                        )
                        rightFKeys.forEach { (lbl, code) ->
                            PcKeyTile(lbl, hidCode = code, onTap = { dispatchKey(code) })
                        }
                        PcKeyTile("PrtSc", hidCode = HidConstants.KEY_SYSRQ, onTap = { dispatchKey(HidConstants.KEY_SYSRQ) })
                        PcKeyTile("Del", hidCode = HidConstants.KEY_DELETE, accent = true, onTap = { dispatchKey(HidConstants.KEY_DELETE) })
                    }

                    // Row 1 (Optional Number Row)
                    AnimatedVisibility(
                        visible = showNumberRow,
                        enter = expandVertically(tween(200)) + fadeIn(tween(140)),
                        exit = shrinkVertically(tween(180)) + fadeOut(tween(120)),
                        modifier = Modifier.weight(if (showNumberRow) 1f else 0.001f)
                    ) {
                        val leftNums = if (isTurkishLayout) {
                            listOf(
                                PcKeyDef("\"", "é", HidConstants.KEY_GRAVE, 0.85f),
                                PcKeyDef("1", "!", HidConstants.KEY_1),
                                PcKeyDef("2", "'", HidConstants.KEY_2),
                                PcKeyDef("3", "^", HidConstants.KEY_3),
                                PcKeyDef("4", "+", HidConstants.KEY_4),
                                PcKeyDef("5", "%", HidConstants.KEY_5)
                            )
                        } else {
                            listOf(
                                PcKeyDef("`", "~", HidConstants.KEY_GRAVE, 0.85f),
                                PcKeyDef("1", "!", HidConstants.KEY_1),
                                PcKeyDef("2", "@", HidConstants.KEY_2),
                                PcKeyDef("3", "#", HidConstants.KEY_3),
                                PcKeyDef("4", "$", HidConstants.KEY_4),
                                PcKeyDef("5", "%", HidConstants.KEY_5)
                            )
                        }
                        val rightNums = if (isTurkishLayout) {
                            listOf(
                                PcKeyDef("6", "&", HidConstants.KEY_6),
                                PcKeyDef("7", "/", HidConstants.KEY_7),
                                PcKeyDef("8", "(", HidConstants.KEY_8),
                                PcKeyDef("9", ")", HidConstants.KEY_9),
                                PcKeyDef("0", "=", HidConstants.KEY_0),
                                PcKeyDef("*", "?", HidConstants.KEY_MINUS),
                                PcKeyDef("-", "_", HidConstants.KEY_EQUAL),
                                PcKeyDef("⌫", "", HidConstants.KEY_BACKSPACE, 1.55f, isAccent = true)
                            )
                        } else {
                            listOf(
                                PcKeyDef("6", "^", HidConstants.KEY_6),
                                PcKeyDef("7", "&", HidConstants.KEY_7),
                                PcKeyDef("8", "*", HidConstants.KEY_8),
                                PcKeyDef("9", "(", HidConstants.KEY_9),
                                PcKeyDef("0", ")", HidConstants.KEY_0),
                                PcKeyDef("-", "_", HidConstants.KEY_MINUS),
                                PcKeyDef("=", "+", HidConstants.KEY_EQUAL),
                                PcKeyDef("⌫", "", HidConstants.KEY_BACKSPACE, 1.55f, isAccent = true)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            leftNums.forEach { k ->
                                PcKeyTile(k.primary, k.shiftLabel, k.hidCode, k.weight, k.isAccent, onTap = { dispatchKey(k.hidCode) })
                            }
                            if (effectiveSplit) {
                                PcSplitGapSpacer(
                                    weight = animatedGapWeight,
                                    splitGapWeight = splitGapWeight,
                                    onUpdateSplitGapWeight = onUpdateSplitGapWeight
                                )
                            }
                            rightNums.forEach { k ->
                                PcKeyTile(
                                    k.primary, k.shiftLabel, k.hidCode, k.weight, k.isAccent,
                                    repeatable = k.hidCode == HidConstants.KEY_BACKSPACE,
                                    onTap = { dispatchKey(k.hidCode) }
                                )
                            }
                        }
                    }

                    // Row 2: Tab + Q..T | [split gap] | Y..Ü
                    val leftRow2 = listOf(
                        PcKeyDef("Tab", "", HidConstants.KEY_TAB, 1.3f),
                        PcKeyDef("Q", "", HidConstants.KEY_Q),
                        PcKeyDef("W", "", HidConstants.KEY_W),
                        PcKeyDef("E", "", HidConstants.KEY_E),
                        PcKeyDef("R", "", HidConstants.KEY_R),
                        PcKeyDef("T", "", HidConstants.KEY_T)
                    )
                    val rightRow2 = if (isTurkishLayout) {
                        buildList {
                            add(PcKeyDef("Y", "", HidConstants.KEY_Y))
                            add(PcKeyDef("U", "", HidConstants.KEY_U))
                            add(PcKeyDef("I", "", HidConstants.KEY_I))
                            add(PcKeyDef("O", "", HidConstants.KEY_O))
                            add(PcKeyDef("P", "", HidConstants.KEY_P))
                            add(PcKeyDef("Ğ", "", HidConstants.KEY_LEFTBRACE))
                            add(PcKeyDef("Ü", "", HidConstants.KEY_RIGHTBRACE))
                            if (!showNumberRow) {
                                add(PcKeyDef("⌫", "", HidConstants.KEY_BACKSPACE, 1.2f, isAccent = true))
                            }
                        }
                    } else {
                        buildList {
                            add(PcKeyDef("Y", "", HidConstants.KEY_Y))
                            add(PcKeyDef("U", "", HidConstants.KEY_U))
                            add(PcKeyDef("I", "", HidConstants.KEY_I))
                            add(PcKeyDef("O", "", HidConstants.KEY_O))
                            add(PcKeyDef("P", "", HidConstants.KEY_P))
                            add(PcKeyDef("[", "{", HidConstants.KEY_LEFTBRACE))
                            add(PcKeyDef("]", "}", HidConstants.KEY_RIGHTBRACE))
                            add(
                                PcKeyDef(
                                    if (!showNumberRow) "⌫" else "\\",
                                    if (!showNumberRow) "" else "|",
                                    if (!showNumberRow) HidConstants.KEY_BACKSPACE else HidConstants.KEY_BACKSLASH,
                                    1.1f, isAccent = !showNumberRow
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        leftRow2.forEach { k ->
                            PcKeyTile(k.primary, k.shiftLabel, k.hidCode, k.weight, k.isAccent, onTap = { dispatchKey(k.hidCode) })
                        }
                        if (effectiveSplit) {
                            PcSplitGapSpacer(
                                weight = animatedGapWeight,
                                splitGapWeight = splitGapWeight,
                                showHandle = false,
                                onUpdateSplitGapWeight = onUpdateSplitGapWeight
                            )
                        }
                        rightRow2.forEach { k ->
                            PcKeyTile(
                                k.primary, k.shiftLabel, k.hidCode, k.weight, k.isAccent,
                                repeatable = k.hidCode == HidConstants.KEY_BACKSPACE,
                                onTap = { dispatchKey(k.hidCode) }
                            )
                        }
                    }

                    // Row 3: Caps + A..G | [split gap] | H..İ + , + Enter (Split moves `,` to inner center edge)
                    val leftRow3 = listOf(
                        PcKeyDef("A", "", HidConstants.KEY_A),
                        PcKeyDef("S", "", HidConstants.KEY_S),
                        PcKeyDef("D", "", HidConstants.KEY_D),
                        PcKeyDef("F", "", HidConstants.KEY_F),
                        PcKeyDef("G", "", HidConstants.KEY_G)
                    )
                    val rightRow3 = if (isTurkishLayout) {
                        if (effectiveSplit) {
                            listOf(
                                PcKeyDef(",", ";", HidConstants.KEY_BACKSLASH),
                                PcKeyDef("H", "", HidConstants.KEY_H),
                                PcKeyDef("J", "", HidConstants.KEY_J),
                                PcKeyDef("K", "", HidConstants.KEY_K),
                                PcKeyDef("L", "", HidConstants.KEY_L),
                                PcKeyDef("Ş", "", HidConstants.KEY_SEMICOLON),
                                PcKeyDef("İ", "", HidConstants.KEY_APOSTROPHE)
                            )
                        } else {
                            listOf(
                                PcKeyDef("H", "", HidConstants.KEY_H),
                                PcKeyDef("J", "", HidConstants.KEY_J),
                                PcKeyDef("K", "", HidConstants.KEY_K),
                                PcKeyDef("L", "", HidConstants.KEY_L),
                                PcKeyDef("Ş", "", HidConstants.KEY_SEMICOLON),
                                PcKeyDef("İ", "", HidConstants.KEY_APOSTROPHE),
                                PcKeyDef(",", ";", HidConstants.KEY_BACKSLASH)
                            )
                        }
                    } else {
                        if (effectiveSplit) {
                            listOf(
                                PcKeyDef("'", "\"", HidConstants.KEY_APOSTROPHE),
                                PcKeyDef(";", ":", HidConstants.KEY_SEMICOLON),
                                PcKeyDef("H", "", HidConstants.KEY_H),
                                PcKeyDef("J", "", HidConstants.KEY_J),
                                PcKeyDef("K", "", HidConstants.KEY_K),
                                PcKeyDef("L", "", HidConstants.KEY_L)
                            )
                        } else {
                            listOf(
                                PcKeyDef("H", "", HidConstants.KEY_H),
                                PcKeyDef("J", "", HidConstants.KEY_J),
                                PcKeyDef("K", "", HidConstants.KEY_K),
                                PcKeyDef("L", "", HidConstants.KEY_L),
                                PcKeyDef(";", ":", HidConstants.KEY_SEMICOLON),
                                PcKeyDef("'", "\"", HidConstants.KEY_APOSTROPHE)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        PcModifierTile(
                            label = "Caps",
                            active = capsLockActive,
                            weight = 1.45f,
                            onClick = {
                                capsLockActive = !capsLockActive
                                onSendKey(HidConstants.KEY_CAPSLOCK, 0)
                            }
                        )
                        leftRow3.forEach { k ->
                            PcKeyTile(k.primary, k.shiftLabel, k.hidCode, onTap = { dispatchKey(k.hidCode) })
                        }
                        if (effectiveSplit) {
                            PcSplitGapSpacer(
                                weight = animatedGapWeight,
                                splitGapWeight = splitGapWeight,
                                showHandle = false,
                                onUpdateSplitGapWeight = onUpdateSplitGapWeight
                            )
                        }
                        rightRow3.forEach { k ->
                            PcKeyTile(k.primary, k.shiftLabel, k.hidCode, onTap = { dispatchKey(k.hidCode) })
                        }
                        PcKeyTile("Enter", hidCode = HidConstants.KEY_ENTER, weight = 1.65f, accent = true, onTap = { dispatchKey(HidConstants.KEY_ENTER) })
                    }

                    // Row 4: Shift + Z..B | [split gap] | N..Ç + . + Up + Shift (Split moves `.` to inner center edge)
                    val leftRow4 = listOf(
                        PcKeyDef("Z", "", HidConstants.KEY_Z),
                        PcKeyDef("X", "", HidConstants.KEY_X),
                        PcKeyDef("C", "", HidConstants.KEY_C),
                        PcKeyDef("V", "", HidConstants.KEY_V),
                        PcKeyDef("B", "", HidConstants.KEY_B)
                    )
                    val rightRow4 = if (isTurkishLayout) {
                        if (effectiveSplit) {
                            listOf(
                                PcKeyDef(".", ":", HidConstants.KEY_SLASH),
                                PcKeyDef("N", "", HidConstants.KEY_N),
                                PcKeyDef("M", "", HidConstants.KEY_M),
                                PcKeyDef("Ö", "", HidConstants.KEY_COMMA),
                                PcKeyDef("Ç", "", HidConstants.KEY_DOT)
                            )
                        } else {
                            listOf(
                                PcKeyDef("N", "", HidConstants.KEY_N),
                                PcKeyDef("M", "", HidConstants.KEY_M),
                                PcKeyDef("Ö", "", HidConstants.KEY_COMMA),
                                PcKeyDef("Ç", "", HidConstants.KEY_DOT),
                                PcKeyDef(".", ":", HidConstants.KEY_SLASH)
                            )
                        }
                    } else {
                        if (effectiveSplit) {
                            listOf(
                                PcKeyDef(",", "<", HidConstants.KEY_COMMA),
                                PcKeyDef(".", ">", HidConstants.KEY_DOT),
                                PcKeyDef("/", "?", HidConstants.KEY_SLASH),
                                PcKeyDef("N", "", HidConstants.KEY_N),
                                PcKeyDef("M", "", HidConstants.KEY_M)
                            )
                        } else {
                            listOf(
                                PcKeyDef("N", "", HidConstants.KEY_N),
                                PcKeyDef("M", "", HidConstants.KEY_M),
                                PcKeyDef(",", "<", HidConstants.KEY_COMMA),
                                PcKeyDef(".", ">", HidConstants.KEY_DOT),
                                PcKeyDef("/", "?", HidConstants.KEY_SLASH)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        PcModifierTile(
                            label = "Shift",
                            active = (stickyModifiers and HidConstants.MOD_LSHIFT) != 0,
                            weight = 1.7f,
                            onClick = { toggleMod(HidConstants.MOD_LSHIFT) }
                        )
                        leftRow4.forEach { k ->
                            PcKeyTile(k.primary, k.shiftLabel, k.hidCode, weight = k.weight, onTap = { dispatchKey(k.hidCode) })
                        }
                        if (effectiveSplit) {
                            PcSplitGapSpacer(
                                weight = animatedGapWeight,
                                splitGapWeight = splitGapWeight,
                                showHandle = false,
                                onUpdateSplitGapWeight = onUpdateSplitGapWeight
                            )
                        }
                        rightRow4.forEach { k ->
                            PcKeyTile(k.primary, k.shiftLabel, k.hidCode, weight = k.weight, onTap = { dispatchKey(k.hidCode) })
                        }
                        PcArrowTile(
                            icon = Icons.Default.KeyboardArrowUp,
                            desc = "Yukarı",
                            onTap = { dispatchKey(HidConstants.KEY_UP) },
                            modifier = Modifier.weight(1f)
                        )
                        PcModifierTile(
                            label = "Shift",
                            active = (stickyModifiers and HidConstants.MOD_RSHIFT) != 0,
                            weight = 1.25f,
                            onClick = { toggleMod(HidConstants.MOD_RSHIFT) }
                        )
                    }

                    // Row 5: Ctrl + Alt + SPACE (Split Left/Right or Unified) + AltGr + Left/Down/Right
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        PcModifierTile(
                            label = "Ctrl",
                            active = (stickyModifiers and HidConstants.MOD_LCTRL) != 0,
                            weight = 1.3f,
                            onClick = { toggleMod(HidConstants.MOD_LCTRL) }
                        )
                        PcModifierTile(
                            label = "Alt",
                            active = (stickyModifiers and HidConstants.MOD_LALT) != 0,
                            weight = 1.15f,
                            onClick = { toggleMod(HidConstants.MOD_LALT) }
                        )
                        if (effectiveSplit) {
                            PcKeyTile("SPACE", hidCode = HidConstants.KEY_SPACE, weight = 2.6f, onTap = { dispatchKey(HidConstants.KEY_SPACE) })
                            PcSplitGapSpacer(
                                weight = animatedGapWeight,
                                splitGapWeight = splitGapWeight,
                                onUpdateSplitGapWeight = onUpdateSplitGapWeight
                            )
                            PcKeyTile("SPACE", hidCode = HidConstants.KEY_SPACE, weight = 2.6f, onTap = { dispatchKey(HidConstants.KEY_SPACE) })
                        } else {
                            PcKeyTile("SPACE", hidCode = HidConstants.KEY_SPACE, weight = 5.4f, onTap = { dispatchKey(HidConstants.KEY_SPACE) })
                        }
                        PcModifierTile(
                            label = "AltGr",
                            active = (stickyModifiers and HidConstants.MOD_RALT) != 0,
                            weight = 1.2f,
                            onClick = { toggleMod(HidConstants.MOD_RALT) }
                        )
                        PcArrowTile(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            desc = "Sol",
                            onTap = { dispatchKey(HidConstants.KEY_LEFT) },
                            modifier = Modifier.weight(1f)
                        )
                        PcArrowTile(
                            icon = Icons.Default.KeyboardArrowDown,
                            desc = "Aşağı",
                            onTap = { dispatchKey(HidConstants.KEY_DOWN) },
                            modifier = Modifier.weight(1f)
                        )
                        PcArrowTile(
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            desc = "Sağ",
                            onTap = { dispatchKey(HidConstants.KEY_RIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Optional Right Side Numeric Keypad (Animated Expand/Shrink)
            AnimatedVisibility(
                visible = showNumpad,
                enter = expandHorizontally(tween(220)) + fadeIn(tween(150)),
                exit = shrinkHorizontally(tween(200)) + fadeOut(tween(120)),
                modifier = Modifier
                    .weight(if (showNumpad) 1f else 0.001f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceCharcoal.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(8.dp))
                        .padding(3.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        PcKeyTile("Num", hidCode = HidConstants.KEY_NUMLOCK, onTap = { dispatchKey(HidConstants.KEY_NUMLOCK) })
                        PcKeyTile("/", hidCode = HidConstants.KEY_KP_SLASH, onTap = { dispatchKey(HidConstants.KEY_KP_SLASH) })
                        PcKeyTile("*", hidCode = HidConstants.KEY_KP_ASTERISK, onTap = { dispatchKey(HidConstants.KEY_KP_ASTERISK) })
                        PcKeyTile("-", hidCode = HidConstants.KEY_KP_MINUS, onTap = { dispatchKey(HidConstants.KEY_KP_MINUS) })
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        PcKeyTile("7", hidCode = HidConstants.KEY_KP_7, onTap = { dispatchKey(HidConstants.KEY_KP_7) })
                        PcKeyTile("8", hidCode = HidConstants.KEY_KP_8, onTap = { dispatchKey(HidConstants.KEY_KP_8) })
                        PcKeyTile("9", hidCode = HidConstants.KEY_KP_9, onTap = { dispatchKey(HidConstants.KEY_KP_9) })
                        PcKeyTile("+", hidCode = HidConstants.KEY_KP_PLUS, accent = true, onTap = { dispatchKey(HidConstants.KEY_KP_PLUS) })
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        PcKeyTile("4", hidCode = HidConstants.KEY_KP_4, onTap = { dispatchKey(HidConstants.KEY_KP_4) })
                        PcKeyTile("5", hidCode = HidConstants.KEY_KP_5, onTap = { dispatchKey(HidConstants.KEY_KP_5) })
                        PcKeyTile("6", hidCode = HidConstants.KEY_KP_6, onTap = { dispatchKey(HidConstants.KEY_KP_6) })
                        PcKeyTile("Home", hidCode = HidConstants.KEY_HOME, onTap = { dispatchKey(HidConstants.KEY_HOME) })
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        PcKeyTile("1", hidCode = HidConstants.KEY_KP_1, onTap = { dispatchKey(HidConstants.KEY_KP_1) })
                        PcKeyTile("2", hidCode = HidConstants.KEY_KP_2, onTap = { dispatchKey(HidConstants.KEY_KP_2) })
                        PcKeyTile("3", hidCode = HidConstants.KEY_KP_3, onTap = { dispatchKey(HidConstants.KEY_KP_3) })
                        PcKeyTile("End", hidCode = HidConstants.KEY_END, onTap = { dispatchKey(HidConstants.KEY_END) })
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        PcKeyTile("0", hidCode = HidConstants.KEY_KP_0, weight = 2f, onTap = { dispatchKey(HidConstants.KEY_KP_0) })
                        PcKeyTile(".", hidCode = HidConstants.KEY_KP_DOT, weight = 1f, onTap = { dispatchKey(HidConstants.KEY_KP_DOT) })
                        PcKeyTile("↵", hidCode = HidConstants.KEY_KP_ENTER, weight = 1f, accent = true, onTap = { dispatchKey(HidConstants.KEY_KP_ENTER) })
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.PcSplitGapSpacer(
    weight: Float,
    splitGapWeight: Float,
    showHandle: Boolean = false,
    onUpdateSplitGapWeight: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .weight(weight.coerceAtLeast(0.4f))
            .fillMaxHeight()
            .then(
                if (showHandle) {
                    Modifier.pointerInput(splitGapWeight) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            onUpdateSplitGapWeight((splitGapWeight + dragAmount * 0.01f).coerceIn(0.4f, 3.2f))
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (showHandle) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(22.dp)
                    .background(RubyPrimary.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun ToolbarChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) RubyPrimary.copy(alpha = 0.16f) else SurfaceCharcoal,
        shape = RoundedCornerShape(7.dp),
        modifier = modifier
            .fillMaxHeight()
            .border(1.dp, if (selected) RubyPrimary.copy(alpha = 0.6f) else SurfaceBorderRuby, RoundedCornerShape(7.dp))
            .clip(RoundedCornerShape(7.dp))
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = label,
                color = if (selected) RubyPrimary else TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RowScope.PcKeyTile(
    label: String,
    shiftLabel: String = "",
    hidCode: Int,
    weight: Float = 1f,
    accent: Boolean = false,
    repeatable: Boolean = false,
    onTap: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        color = when {
            pressed -> RubyPrimary.copy(alpha = 0.38f)
            accent -> RubyPrimary.copy(alpha = 0.14f)
            else -> SurfaceCharcoal
        },
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .border(
                1.dp,
                if (pressed || accent) RubyPrimary.copy(alpha = 0.7f) else SurfaceBorderRuby,
                RoundedCornerShape(6.dp)
            )
            .pointerInput(hidCode, repeatable) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onTap()
                        val holdJob = if (repeatable) {
                            scope.launch {
                                delay(260)
                                while (isActive) {
                                    onTap()
                                    delay(55)
                                }
                            }
                        } else null
                        tryAwaitRelease()
                        holdJob?.cancel()
                        pressed = false
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            if (shiftLabel.isNotEmpty()) {
                Text(
                    text = shiftLabel,
                    color = TextMuted,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 3.dp)
                )
            }
            Text(
                text = label,
                color = if (pressed || accent) RubyPrimary else TextPrimary,
                fontSize = if (label.length > 2) 10.5.sp else 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                softWrap = false,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RowScope.PcModifierTile(
    label: String,
    active: Boolean,
    weight: Float,
    onClick: () -> Unit
) {
    Surface(
        color = if (active) RubyPrimary else SurfaceContainerCharcoal,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .border(
                1.dp,
                if (active) RubyPrimary else SurfaceBorderRuby,
                RoundedCornerShape(6.dp)
            )
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 2.dp)) {
            Text(
                text = label,
                color = if (active) Color.White else RubyAccent,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                softWrap = false,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PcArrowTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        color = if (pressed) RubyPrimary.copy(alpha = 0.35f) else SurfaceCharcoal,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
            .fillMaxHeight()
            .border(1.dp, if (pressed) RubyPrimary else SurfaceBorderRuby, RoundedCornerShape(6.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onTap()
                        val holdJob = scope.launch {
                            delay(240)
                            while (isActive) {
                                onTap()
                                delay(65)
                            }
                        }
                        tryAwaitRelease()
                        holdJob.cancel()
                        pressed = false
                    }
                )
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = desc,
                tint = RubyPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
