package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardAlt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetooth.HidConstants
import com.example.bluetooth.TouchContact
import com.example.model.GamepadStyle
import com.example.model.InputSubMode
import com.example.model.QuickToolId
import com.example.model.RemoteSettings
import com.example.ui.components.QuickToolsGridSection
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RubyAccent
import com.example.ui.theme.RubyPrimary
import com.example.ui.theme.SurfaceBorderRuby
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.theme.SurfaceContainerCharcoal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InputsScreen(
    selectedSubMode: InputSubMode,
    settings: RemoteSettings,
    isWifiMode: Boolean = false,
    isFullscreenOverlay: Boolean,
    onEnterFullscreenMode: (InputSubMode) -> Unit,
    onExitFullscreenToSmartKeyboard: () -> Unit,
    onUpdateSettings: ((RemoteSettings) -> RemoteSettings) -> Unit,
    onSendText: (String) -> Unit,
    onSendKey: (hidKeyCode: Int, modifier: Int) -> Unit,
    onSendConsumerKey: (usageCode: Int, name: String) -> Unit,
    onGamepadState: (buttonsMask: Int, hat: Int, lx: Int, ly: Int, rx: Int, ry: Int) -> Unit,
    onTouchpadContacts: (contacts: List<TouchContact>, clickpadPressed: Boolean) -> Unit,
    onTrackpadDrag: (dx: Float, dy: Float) -> Unit,
    onTrackpadTap: () -> Unit,
    onMoveQuickTool: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onRemoveQuickToolToPool: (QuickToolId) -> Unit = {},
    onAddQuickToolFromPool: (QuickToolId) -> Unit = {},
    onSetLiveTypingMode: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeViewKey = if (isFullscreenOverlay) selectedSubMode else InputSubMode.SMART_KEYBOARD
    var highlightedLaunchTab by remember { mutableStateOf<InputSubMode?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isFullscreenOverlay) {
        if (!isFullscreenOverlay) {
            highlightedLaunchTab = null
        }
    }

    AnimatedContent(
        targetState = activeViewKey,
        transitionSpec = {
            (fadeIn(tween(240)) + scaleIn(initialScale = 0.94f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))) togetherWith
                (fadeOut(tween(180)) + scaleOut(targetScale = 0.96f))
        },
        label = "inputs_fullscreen_transition",
        modifier = modifier
    ) { modeKey ->
        when (modeKey) {
            InputSubMode.PC_KEYBOARD -> {
                LandscapePcKeyboardContent(
                    isTurkishLayout = settings.isTurkishKeyboardLayout,
                    showNumberRow = settings.showPcNumberRow,
                    showNumpad = settings.showPcNumpad,
                    splitKeyboard = settings.splitPcKeyboard,
                    splitGapWeight = settings.pcSplitGapWeight,
                    onToggleLayout = {
                        onUpdateSettings { it.copy(isTurkishKeyboardLayout = !it.isTurkishKeyboardLayout) }
                    },
                    onToggleNumberRow = {
                        onUpdateSettings { it.copy(showPcNumberRow = !it.showPcNumberRow) }
                    },
                    onToggleNumpad = {
                        onUpdateSettings { it.copy(showPcNumpad = !it.showPcNumpad) }
                    },
                    onToggleSplit = {
                        onUpdateSettings { it.copy(splitPcKeyboard = !it.splitPcKeyboard) }
                    },
                    onUpdateSplitGapWeight = { weight ->
                        onUpdateSettings { it.copy(pcSplitGapWeight = weight.coerceIn(0.4f, 3.2f)) }
                    },
                    onExitFullscreen = onExitFullscreenToSmartKeyboard,
                    onSendKey = onSendKey,
                    modifier = Modifier.fillMaxSize()
                )
            }
            InputSubMode.GAMEPAD -> {
                GamepadScreenContent(
                    gamepadStyle = settings.gamepadStyle,
                    hidProfileMode = settings.hidProfileMode,
                    isWifiMode = isWifiMode,
                    swapLeftWing = settings.swapGamepadLeftWing,
                    swapRightWing = settings.swapGamepadRightWing,
                    mirrorHands = settings.mirrorGamepadHands,
                    onToggleStyle = {
                        onUpdateSettings { s ->
                            s.copy(
                                gamepadStyle = if (s.gamepadStyle == GamepadStyle.PLAYSTATION)
                                    GamepadStyle.XBOX else GamepadStyle.PLAYSTATION
                            )
                        }
                    },
                    onToggleSwapLeftWing = {
                        onUpdateSettings { it.copy(swapGamepadLeftWing = !it.swapGamepadLeftWing) }
                    },
                    onToggleSwapRightWing = {
                        onUpdateSettings { it.copy(swapGamepadRightWing = !it.swapGamepadRightWing) }
                    },
                    onToggleMirrorHands = {
                        onUpdateSettings { it.copy(mirrorGamepadHands = !it.mirrorGamepadHands) }
                    },
                    onExitFullscreen = onExitFullscreenToSmartKeyboard,
                    onGamepadState = onGamepadState,
                    onTouchpadContacts = onTouchpadContacts,
                    onTrackpadDrag = onTrackpadDrag,
                    onTrackpadTap = onTrackpadTap,
                    modifier = Modifier.fillMaxSize()
                )
            }
            InputSubMode.SMART_KEYBOARD -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Top Mode Bar with balanced weights & zero edge clipping
                    Surface(
                        color = SurfaceCharcoal,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 46.dp)
                            .height(42.dp)
                            .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            InputModeTab(
                                label = "Akıllı Klavye",
                                icon = Icons.Default.Keyboard,
                                selected = highlightedLaunchTab == null,
                                onClick = { highlightedLaunchTab = null },
                                modifier = Modifier.weight(1.42f).testTag("input_mode_smart")
                            )
                            InputModeTab(
                                label = "PC Klavye",
                                icon = Icons.Default.KeyboardAlt,
                                selected = highlightedLaunchTab == InputSubMode.PC_KEYBOARD,
                                onClick = {
                                    highlightedLaunchTab = InputSubMode.PC_KEYBOARD
                                    coroutineScope.launch {
                                        delay(110)
                                        onEnterFullscreenMode(InputSubMode.PC_KEYBOARD)
                                    }
                                },
                                modifier = Modifier.weight(1.04f).testTag("input_mode_pc")
                            )
                            InputModeTab(
                                label = "Gamepad",
                                icon = Icons.Default.Gamepad,
                                selected = highlightedLaunchTab == InputSubMode.GAMEPAD,
                                onClick = {
                                    highlightedLaunchTab = InputSubMode.GAMEPAD
                                    coroutineScope.launch {
                                        delay(110)
                                        onEnterFullscreenMode(InputSubMode.GAMEPAD)
                                    }
                                },
                                modifier = Modifier.weight(0.94f).testTag("input_mode_gamepad")
                            )
                        }
                    }

                    SmartKeyboardSubView(
                        settings = settings,
                        onSendText = onSendText,
                        onSendKey = onSendKey,
                        onSendConsumerKey = onSendConsumerKey,
                        onMoveQuickTool = onMoveQuickTool,
                        onRemoveQuickToolToPool = onRemoveQuickToolToPool,
                        onAddQuickToolFromPool = onAddQuickToolFromPool,
                        onSetLiveTypingMode = onSetLiveTypingMode,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InputModeTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (selected) RubyPrimary.copy(alpha = 0.24f) else Color.Transparent,
        animationSpec = tween(140),
        label = "mode_tab_bg"
    )
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxSize()
            .then(
                if (selected) Modifier.border(1.dp, RubyPrimary.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                else Modifier
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) RubyPrimary else TextMuted,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (selected) RubyPrimary else TextSecondary,
                fontSize = 9.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SmartKeyboardSubView(
    settings: RemoteSettings,
    onSendText: (String) -> Unit,
    onSendKey: (hidKeyCode: Int, modifier: Int) -> Unit,
    onSendConsumerKey: (usageCode: Int, name: String) -> Unit,
    onMoveQuickTool: (fromIndex: Int, toIndex: Int) -> Unit,
    onRemoveQuickToolToPool: (QuickToolId) -> Unit,
    onAddQuickToolFromPool: (QuickToolId) -> Unit,
    onSetLiveTypingMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val liveTypingMode = settings.liveTypingMode

    // Continuous smooth bottom clearance: eliminates IME closing jump below bottom FloatingNavigationCapsule
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val imeAboveNav = (imeBottom - navBarBottom).coerceAtLeast(0.dp)
    val smoothBottomClearance = maxOf(imeAboveNav + 6.dp, 74.dp)

    fun dispatchQuickTool(tool: QuickToolId) {
        when (tool) {
            QuickToolId.PREV -> onSendConsumerKey(HidConstants.CONSUMER_PREV_TRACK, "Prev")
            QuickToolId.NEXT -> onSendConsumerKey(HidConstants.CONSUMER_NEXT_TRACK, "Next")
            QuickToolId.VOL_DOWN -> onSendConsumerKey(HidConstants.CONSUMER_VOLUME_DOWN, "Vol Down")
            QuickToolId.VOL_UP -> onSendConsumerKey(HidConstants.CONSUMER_VOLUME_UP, "Vol Up")
            QuickToolId.MUTE -> onSendConsumerKey(HidConstants.CONSUMER_MUTE, "Mute")
            QuickToolId.PLAY_PAUSE -> onSendConsumerKey(HidConstants.CONSUMER_PLAY_PAUSE, "Play/Pause")
            QuickToolId.SPACE -> onSendKey(HidConstants.KEY_SPACE, 0)
            QuickToolId.BACKSPACE -> onSendKey(HidConstants.KEY_BACKSPACE, 0)
            QuickToolId.ENTER -> onSendKey(HidConstants.KEY_ENTER, 0)
            QuickToolId.DEL -> onSendKey(HidConstants.KEY_DELETE, 0)
            QuickToolId.ZOOM_IN -> onSendKey(HidConstants.KEY_EQUAL, HidConstants.MOD_LCTRL)
            QuickToolId.ZOOM_OUT -> onSendKey(HidConstants.KEY_MINUS, HidConstants.MOD_LCTRL)
            QuickToolId.CUT -> onSendKey(HidConstants.KEY_X, HidConstants.MOD_LCTRL)
            QuickToolId.COPY -> onSendKey(HidConstants.KEY_C, HidConstants.MOD_LCTRL)
            QuickToolId.PASTE -> onSendKey(HidConstants.KEY_V, HidConstants.MOD_LCTRL)
            QuickToolId.TR_CCEDIL,
            QuickToolId.TR_GBREVE,
            QuickToolId.TR_DOTLESS_I,
            QuickToolId.TR_OUML,
            QuickToolId.TR_SCEDIL,
            QuickToolId.TR_UUML,
            QuickToolId.TR_CAPITAL_I,
            QuickToolId.PUNCT_COMMA,
            QuickToolId.PUNCT_DOT -> {
                if (liveTypingMode) onSendText(tool.shortLabel) else inputText += tool.shortLabel
            }
            QuickToolId.DPAD -> Unit
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = smoothBottomClearance),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Upper Scrollable Shortcuts Area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            SectionHeader("FONKSİYON TUŞLARI")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                SpaciousKeyTile("Esc", "Çıkış", onClick = { onSendKey(HidConstants.KEY_ESC, 0) }, modifier = Modifier.weight(1f))
                SpaciousKeyTile("Tab", "Sekme", onClick = { onSendKey(HidConstants.KEY_TAB, 0) }, modifier = Modifier.weight(1f))
                SpaciousKeyTile("Home", "Başa", onClick = { onSendKey(HidConstants.KEY_HOME, 0) }, modifier = Modifier.weight(1f))
                SpaciousKeyTile("End", "Sona", onClick = { onSendKey(HidConstants.KEY_END, 0) }, modifier = Modifier.weight(1f))
                SpaciousKeyTile("PgUp", "Yukarı", onClick = { onSendKey(HidConstants.KEY_PAGEUP, 0) }, modifier = Modifier.weight(1f))
                SpaciousKeyTile("PgDn", "Aşağı", onClick = { onSendKey(HidConstants.KEY_PAGEDOWN, 0) }, modifier = Modifier.weight(1f))
            }

            SectionHeader("TARAYICI & SEKMELER")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                SpaciousKeyTile("Yeni Sekme", "Ctrl+T", icon = Icons.Default.Add, onClick = {
                    onSendKey(HidConstants.KEY_T, HidConstants.MOD_LCTRL)
                }, modifier = Modifier.weight(1f))
                SpaciousKeyTile("Kapat", "Ctrl+W", icon = Icons.Default.Close, onClick = {
                    onSendKey(HidConstants.KEY_W, HidConstants.MOD_LCTRL)
                }, modifier = Modifier.weight(1f))
                SpaciousKeyTile("Yenile", "Ctrl+R", icon = Icons.Default.Refresh, onClick = {
                    onSendKey(HidConstants.KEY_R, HidConstants.MOD_LCTRL)
                }, modifier = Modifier.weight(1f))
                SpaciousKeyTile("Adres", "Ctrl+L", icon = Icons.Default.Search, onClick = {
                    onSendKey(HidConstants.KEY_L, HidConstants.MOD_LCTRL)
                }, modifier = Modifier.weight(1f))
            }

            SectionHeader("TABLET & YAKINLAŞTIRMA")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                SpaciousKeyTile("Ana Ekran", "Home", icon = Icons.Default.Home, onClick = {
                    onSendConsumerKey(HidConstants.CONSUMER_HOME, "Home")
                }, modifier = Modifier.weight(1f))
                SpaciousKeyTile("Tablet Ara", "Ara", icon = Icons.Default.Search, onClick = {
                    onSendConsumerKey(HidConstants.CONSUMER_AC_SEARCH, "Search")
                }, modifier = Modifier.weight(1f))
                SpaciousKeyTile("Tam Ekran", "F11", icon = Icons.Default.Fullscreen, onClick = {
                    onSendKey(HidConstants.KEY_F11, 0)
                }, modifier = Modifier.weight(1f))
                SpaciousKeyTile("Yakınlaştır", "Ctrl +", icon = Icons.Default.Add, onClick = {
                    onSendKey(HidConstants.KEY_EQUAL, HidConstants.MOD_LCTRL)
                }, modifier = Modifier.weight(1f))
                SpaciousKeyTile("Uzaklaştır", "Ctrl -", icon = Icons.Default.Remove, onClick = {
                    onSendKey(HidConstants.KEY_MINUS, HidConstants.MOD_LCTRL)
                }, modifier = Modifier.weight(1f))
                SpaciousKeyTile("Kapat", "Alt+F4", accentColor = RubyAccent, onClick = {
                    onSendKey(HidConstants.KEY_F4, HidConstants.MOD_LALT)
                }, modifier = Modifier.weight(1f))
            }

            SectionHeader("PANO & METİN KISAYOLLARI")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                SpaciousKeyTile("Seç", "Ctrl+A", icon = Icons.Default.SelectAll, onClick = {
                    onSendKey(HidConstants.KEY_A, HidConstants.MOD_LCTRL)
                }, modifier = Modifier.weight(1f))
                SpaciousKeyTile("Kes", "Ctrl+X", icon = Icons.Default.ContentCut, onClick = {
                    onSendKey(HidConstants.KEY_X, HidConstants.MOD_LCTRL)
                }, modifier = Modifier.weight(1f))
                SpaciousKeyTile("Kopyala", "Ctrl+C", icon = Icons.Default.ContentCopy, onClick = {
                    onSendKey(HidConstants.KEY_C, HidConstants.MOD_LCTRL)
                }, modifier = Modifier.weight(1f))
                SpaciousKeyTile("Yapıştır", "Ctrl+V", icon = Icons.Default.ContentPaste, onClick = {
                    onSendKey(HidConstants.KEY_V, HidConstants.MOD_LCTRL)
                }, modifier = Modifier.weight(1f))
                SpaciousKeyTile("Geri Al", "Ctrl+Z", icon = Icons.AutoMirrored.Filled.Undo, onClick = {
                    onSendKey(HidConstants.KEY_Z, HidConstants.MOD_LCTRL)
                }, modifier = Modifier.weight(1f))
                SpaciousKeyTile("İleri Al", "Ctrl+Y", icon = Icons.AutoMirrored.Filled.Redo, onClick = {
                    onSendKey(HidConstants.KEY_Y, HidConstants.MOD_LCTRL)
                }, modifier = Modifier.weight(1f))
            }
        }

        // Samsung One UI 6-Column Quick Tools Grid + Upper Spare Pool
        QuickToolsGridSection(
            activeTools = settings.quickToolsActiveOrder,
            poolTools = settings.quickToolsPoolOrder,
            liveTypingMode = liveTypingMode,
            isTurkishLayout = settings.isTurkishKeyboardLayout,
            onMoveQuickTool = onMoveQuickTool,
            onRemoveToPool = onRemoveQuickToolToPool,
            onAddFromPool = onAddQuickToolFromPool,
            onTriggerTool = { tool -> dispatchQuickTool(tool) },
            onSendKey = onSendKey
        )

        // Clean Single-Line/Single-Border Text Input + Fixed Right Live/Wait Mode Toggle (No Outer Pill Wrapper!)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { newText ->
                    if (liveTypingMode && newText.length > inputText.length) {
                        val addedChars = newText.substring(inputText.length)
                        onSendText(addedChars)
                    } else if (liveTypingMode && newText.length < inputText.length) {
                        val diff = inputText.length - newText.length
                        repeat(diff) { onSendKey(HidConstants.KEY_BACKSPACE, 0) }
                    }
                    inputText = newText
                },
                placeholder = {
                    Text(
                        if (liveTypingMode) "Anlık yazmak için dokunun..." else "Taslak yazın ve gönderin...",
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                trailingIcon = {
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = { inputText = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Temizle",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceContainerCharcoal,
                    unfocusedContainerColor = SurfaceContainerCharcoal,
                    focusedBorderColor = RubyPrimary,
                    unfocusedBorderColor = SurfaceBorderRuby,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (!liveTypingMode && inputText.isNotEmpty()) {
                            onSendText(inputText)
                            inputText = ""
                        } else {
                            onSendKey(HidConstants.KEY_ENTER, 0)
                        }
                        focusManager.clearFocus()
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .testTag("soft_keyboard_input_field")
            )

            // Send / Enter action button
            Surface(
                color = RubyPrimary,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        if (inputText.isNotEmpty()) {
                            if (!liveTypingMode) {
                                onSendText(inputText)
                            }
                            inputText = ""
                            onSendKey(HidConstants.KEY_ENTER, 0)
                        } else {
                            onSendKey(HidConstants.KEY_ENTER, 0)
                        }
                    }
                    .testTag("keyboard_send_btn")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Gönder / Enter",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Independent Fixed Right-Side Live / Wait Mode Toggle Button
            Surface(
                color = if (liveTypingMode) RubyPrimary.copy(alpha = 0.2f) else SurfaceCharcoal,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(48.dp)
                    .border(
                        1.2.dp,
                        if (liveTypingMode) RubyPrimary else GreenSuccess.copy(alpha = 0.7f),
                        RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSetLiveTypingMode(!liveTypingMode) }
                    .padding(horizontal = 10.dp)
                    .testTag("live_batch_typing_toggle")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (liveTypingMode) Icons.Default.Bolt else Icons.Default.Pause,
                        contentDescription = if (liveTypingMode) "Canlı Yazım Aktif" else "Bekleme Modu Aktif",
                        tint = if (liveTypingMode) RubyPrimary else GreenSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (liveTypingMode) "CANLI" else "BEKLE",
                        color = if (liveTypingMode) RubyPrimary else GreenSuccess,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = TextMuted,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 2.dp, top = 2.dp)
    )
}

@Composable
private fun SpaciousKeyTile(
    label: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    accentColor: Color = TextPrimary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceCharcoal,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .height(44.dp)
            .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 3.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = accentColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Text(
                    text = label,
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}
