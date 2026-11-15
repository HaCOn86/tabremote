package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardBackspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SpaceBar
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
import androidx.compose.ui.platform.LocalDensity
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
import com.example.model.MediaDockItemId
import com.example.model.RemoteSettings
import com.example.ui.components.DockOrderEditorPanel
import com.example.ui.components.ExpressiveDockRow
import com.example.ui.components.expressivePressDetect
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
    onMoveMediaDockItem: (index: Int, delta: Int) -> Unit = { _, _ -> },
    onToggleMediaDockHidden: (MediaDockItemId) -> Unit = {},
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
                        .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Top Mode Bar with unclipped "Akıllı Klavye" label and crimson flash launch
                    Surface(
                        color = SurfaceCharcoal,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 46.dp)
                            .height(42.dp)
                            .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(12.dp))
                            .padding(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            InputModeTab(
                                label = "Akıllı Klavye",
                                icon = Icons.Default.Keyboard,
                                selected = highlightedLaunchTab == null,
                                onClick = { highlightedLaunchTab = null },
                                modifier = Modifier.weight(1.38f).testTag("input_mode_smart")
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
                                modifier = Modifier.weight(1.02f).testTag("input_mode_pc")
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
                                modifier = Modifier.weight(0.95f).testTag("input_mode_gamepad")
                            )
                        }
                    }

                    SmartKeyboardSubView(
                        settings = settings,
                        onSendText = onSendText,
                        onSendKey = onSendKey,
                        onSendConsumerKey = onSendConsumerKey,
                        onMoveMediaDockItem = onMoveMediaDockItem,
                        onToggleMediaDockHidden = onToggleMediaDockHidden,
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
        shape = RoundedCornerShape(9.dp),
        modifier = modifier
            .fillMaxSize()
            .then(
                if (selected) Modifier.border(1.dp, RubyPrimary.copy(alpha = 0.7f), RoundedCornerShape(9.dp))
                else Modifier
            )
            .clip(RoundedCornerShape(9.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) RubyPrimary else TextMuted,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                color = if (selected) RubyPrimary else TextSecondary,
                fontSize = 9.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                softWrap = false,
                overflow = TextOverflow.Visible,
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
    onMoveMediaDockItem: (index: Int, delta: Int) -> Unit,
    onToggleMediaDockHidden: (MediaDockItemId) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var liveTypingMode by remember { mutableStateOf(true) }
    var showMediaEditor by remember { mutableStateOf(false) }

    val imeBottomPadding = WindowInsets.ime.asPaddingValues(LocalDensity.current).calculateBottomPadding()
    val isImeOpen = imeBottomPadding > 10.dp

    val visibleMediaItems = remember(settings.mediaDockOrder, settings.hiddenMediaDockItems) {
        settings.mediaDockOrder.filter { it !in settings.hiddenMediaDockItems }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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

        Spacer(modifier = Modifier.height(4.dp))

        // Inline Reorder/Hide Editor for Media Dock
        DockOrderEditorPanel(
            visible = showMediaEditor,
            title = "MEDYA TUŞ SIRASI & GİZLE (BASILI TUT VEYA DÜZENLE)",
            orderedItems = settings.mediaDockOrder,
            hiddenItems = settings.hiddenMediaDockItems,
            labelOf = { it.label },
            onMoveLeft = { idx -> onMoveMediaDockItem(idx, -1) },
            onMoveRight = { idx -> onMoveMediaDockItem(idx, 1) },
            onToggleHidden = onToggleMediaDockHidden,
            onDone = { showMediaEditor = false },
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Customizable Media Transport & Volume Dock (Play/Pause defaults to FAR RIGHT)
        Surface(
            color = SurfaceCharcoal,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(12.dp))
                .padding(3.dp)
                .testTag("inputs_media_dock")
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressiveDockRow(
                    items = visibleMediaItems,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalSpacing = 4.dp,
                    itemWeight = { item ->
                        when (item) {
                            MediaDockItemId.PLAY_PAUSE -> 1.45f
                            MediaDockItemId.PREV, MediaDockItemId.NEXT -> 0.9f
                            else -> 0.8f
                        }
                    }
                ) { item, _, isPressed, _, _, onPressChange ->
                    val (icon, usage, name) = when (item) {
                        MediaDockItemId.PREV -> Triple(Icons.Default.SkipPrevious, HidConstants.CONSUMER_PREV_TRACK, "Prev")
                        MediaDockItemId.NEXT -> Triple(Icons.Default.SkipNext, HidConstants.CONSUMER_NEXT_TRACK, "Next")
                        MediaDockItemId.VOL_DOWN -> Triple(Icons.AutoMirrored.Filled.VolumeDown, HidConstants.CONSUMER_VOLUME_DOWN, "Vol Down")
                        MediaDockItemId.VOL_UP -> Triple(Icons.AutoMirrored.Filled.VolumeUp, HidConstants.CONSUMER_VOLUME_UP, "Vol Up")
                        MediaDockItemId.MUTE -> Triple(Icons.AutoMirrored.Filled.VolumeMute, HidConstants.CONSUMER_MUTE, "Mute")
                        MediaDockItemId.PLAY_PAUSE -> Triple(Icons.Default.PlayArrow, HidConstants.CONSUMER_PLAY_PAUSE, "Play/Pause")
                    }
                    DockMediaButton(
                        icon = icon,
                        label = item.label,
                        isPrimary = item == MediaDockItemId.PLAY_PAUSE,
                        isPressed = isPressed,
                        onPressChange = onPressChange,
                        onLongPress = { showMediaEditor = !showMediaEditor },
                        onClick = { onSendConsumerKey(usage, name) },
                        modifier = Modifier.fillMaxSize().testTag("media_dock_${item.name.lowercase()}")
                    )
                }

                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showMediaEditor = !showMediaEditor }
                        .testTag("edit_media_dock_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Medya Tuş Sırasını Düzenle",
                        tint = if (showMediaEditor) RubyPrimary else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Lower Ergonomic Cluster: Core Actions (Left) + Seamless D-Pad (Right)
        Surface(
            color = SurfaceCharcoal,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(14.dp))
                .padding(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KeyTile(
                            label = "SPACE",
                            icon = Icons.Default.SpaceBar,
                            onClick = { onSendKey(HidConstants.KEY_SPACE, 0) },
                            modifier = Modifier.weight(1f).height(40.dp)
                        )
                        KeyTile(
                            label = "BACKSPACE",
                            icon = Icons.AutoMirrored.Filled.KeyboardBackspace,
                            accentColor = RubyAccent,
                            onClick = { onSendKey(HidConstants.KEY_BACKSPACE, 0) },
                            modifier = Modifier.weight(1f).height(40.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KeyTile(
                            label = "DEL",
                            accentColor = TextSecondary,
                            onClick = { onSendKey(HidConstants.KEY_DELETE, 0) },
                            modifier = Modifier.weight(0.7f).height(44.dp)
                        )
                        KeyTile(
                            label = "ENTER",
                            icon = Icons.AutoMirrored.Filled.KeyboardReturn,
                            accentColor = RubyPrimary,
                            onClick = { onSendKey(HidConstants.KEY_ENTER, 0) },
                            modifier = Modifier.weight(1.3f).height(44.dp)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BigArrowButton(
                        icon = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Yukarı Ok",
                        onClick = { onSendKey(HidConstants.KEY_UP, 0) }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        BigArrowButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Sol Ok",
                            onClick = { onSendKey(HidConstants.KEY_LEFT, 0) }
                        )
                        BigArrowButton(
                            icon = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Aşağı Ok",
                            onClick = { onSendKey(HidConstants.KEY_DOWN, 0) }
                        )
                        BigArrowButton(
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Sağ Ok",
                            onClick = { onSendKey(HidConstants.KEY_RIGHT, 0) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Typing Mode Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (liveTypingMode) RubyPrimary.copy(alpha = 0.2f) else SurfaceCharcoal,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .border(
                        1.dp,
                        if (liveTypingMode) RubyPrimary else GreenSuccess.copy(alpha = 0.7f),
                        RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { liveTypingMode = !liveTypingMode }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("live_batch_typing_toggle")
            ) {
                Text(
                    text = if (liveTypingMode) "CANLI YAZIM (Anlık İlet)" else "BEKLET & GÖNDER (Taslak Düzenle)",
                    color = if (liveTypingMode) RubyPrimary else GreenSuccess,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = if (settings.isTurkishKeyboardLayout) "TR-Q" else "US-EN",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Turkish Character Quick-Access Bar
        Surface(
            color = SurfaceCharcoal,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(10.dp))
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val trChars = listOf("ç", "ğ", "ı", "ö", "ş", "ü", "İ", ",", ".")
                trChars.forEach { charStr ->
                    Surface(
                        color = SurfaceContainerCharcoal,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .padding(horizontal = 2.dp)
                            .border(1.dp, RubyPrimary.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                if (liveTypingMode) {
                                    onSendText(charStr)
                                } else {
                                    inputText += charStr
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = charStr,
                                color = RubyPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Flush Messaging Input Box
        Surface(
            color = SurfaceCharcoal,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, RubyPrimary.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 3.dp),
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
                            if (liveTypingMode) "Anlık yazmak için dokunun..." else "Taslak yazın, düzenleyin ve GÖNDER'e basın...",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    },
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
                    shape = RoundedCornerShape(10.dp),
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

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    color = RubyPrimary,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(46.dp)
                        .width(if (!liveTypingMode && inputText.isNotEmpty()) 68.dp else 46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (inputText.isNotEmpty()) {
                                if (!liveTypingMode) {
                                    onSendText(inputText)
                                }
                                inputText = ""
                                onSendKey(HidConstants.KEY_ENTER, 0)
                            } else {
                                liveTypingMode = !liveTypingMode
                            }
                        }
                        .testTag("keyboard_send_btn")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (inputText.isNotEmpty() || !liveTypingMode) {
                                Icons.AutoMirrored.Filled.Send
                            } else {
                                Icons.Default.Keyboard
                            },
                            contentDescription = "Gönder",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        if (!liveTypingMode && inputText.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("YAZ", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (!isImeOpen) {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun DockMediaButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isPrimary: Boolean,
    isPressed: Boolean,
    onPressChange: (Boolean) -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = when {
            isPressed -> RubyPrimary
            isPrimary -> RubyPrimary.copy(alpha = 0.18f)
            else -> SurfaceContainerCharcoal
        },
        animationSpec = tween(130),
        label = "media_btn_bg"
    )
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxHeight()
            .border(
                1.dp,
                if (isPrimary || isPressed) RubyPrimary.copy(alpha = 0.65f) else SurfaceBorderRuby,
                RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .expressivePressDetect(
                key = label,
                onPressChange = onPressChange,
                onLongPress = onLongPress,
                onTap = onClick
            )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = when {
                    isPressed -> Color.White
                    isPrimary -> RubyPrimary
                    else -> TextPrimary
                },
                modifier = Modifier.size(14.dp)
            )
            if (isPrimary) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = label,
                    color = if (isPressed) Color.White else RubyPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    softWrap = false,
                    maxLines = 1
                )
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
            .height(46.dp)
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

@Composable
private fun KeyTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    accentColor: Color = TextPrimary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceCharcoal,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun BigArrowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        color = SurfaceCharcoal,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .size(44.dp)
            .border(1.2.dp, SurfaceBorderRuby, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = RubyPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
