package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
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
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardAlt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetooth.HidConstants
import com.example.model.AppRole
import com.example.model.GamepadLayoutOffsets
import com.example.model.GamepadStyle
import com.example.model.InputSubMode
import com.example.model.QuickToolId
import com.example.model.RemoteSettings
import com.example.model.SparseGridEngine
import com.example.model.SparseGridNode
import com.example.ui.components.QuickToolsDpadTile
import com.example.ui.components.QuickToolsGridSection
import com.example.ui.components.ReceiverLockedControlContainer
import com.example.ui.components.quickToolIcon
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.TabRemoteTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InputsScreen(
    selectedSubMode: InputSubMode,
    settings: RemoteSettings,
    isFullscreenOverlay: Boolean,
    isTabletLayout: Boolean = false,
    onEnterFullscreenMode: (InputSubMode) -> Unit,
    onExitFullscreenToSmartKeyboard: () -> Unit,
    onUpdateSettings: ((RemoteSettings) -> RemoteSettings) -> Unit,
    onUpdateGamepadOffsets: ((GamepadLayoutOffsets) -> GamepadLayoutOffsets) -> Unit = {},
    onResetGamepadOffsets: () -> Unit = {},
    onSendText: (String) -> Unit,
    onSendKey: (hidKeyCode: Int, modifier: Int) -> Unit,
    onSendConsumerKey: (usageCode: Int, name: String) -> Unit,
    onGamepadState: (buttonsMask: Int, hat: Int, lx: Int, ly: Int, rx: Int, ry: Int) -> Unit,
    onTrackpadDrag: (dx: Float, dy: Float) -> Unit,
    onTrackpadTap: () -> Unit,
    onPlaceQuickToolAt: (tool: QuickToolId, targetCol: Int, targetRow: Int) -> Unit = { _, _, _ -> },
    onResizeQuickTool: (QuickToolId) -> Unit = {},
    onRemoveQuickToolToPool: (QuickToolId) -> Unit = {},
    onAddQuickToolFromPool: (QuickToolId) -> Unit = {},
    onSetLiveTypingMode: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    val activeViewKey = if (isFullscreenOverlay) selectedSubMode else InputSubMode.HUB_HOME
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
            (fadeIn(tween(240)) + scaleIn(
                initialScale = 0.94f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
            )) togetherWith (fadeOut(tween(180)) + scaleOut(targetScale = 0.96f))
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
                    isReceiverLocked = settings.appRole == AppRole.RECEIVER,
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
                    gamepadOffsets = settings.gamepadOffsets,
                    swapLeftWing = settings.swapGamepadLeftWing,
                    swapRightWing = settings.swapGamepadRightWing,
                    mirrorHands = settings.mirrorGamepadHands,
                    isReceiverLocked = settings.appRole == AppRole.RECEIVER,
                    onToggleStyle = {
                        onUpdateSettings { s ->
                            s.copy(
                                gamepadStyle = if (s.gamepadStyle == GamepadStyle.PLAYSTATION) {
                                    GamepadStyle.XBOX
                                } else {
                                    GamepadStyle.PLAYSTATION
                                }
                            )
                        }
                    },
                    onUpdateOffsets = onUpdateGamepadOffsets,
                    onResetLayout = onResetGamepadOffsets,
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
                    onTrackpadDrag = onTrackpadDrag,
                    onTrackpadTap = onTrackpadTap,
                    modifier = Modifier.fillMaxSize()
                )
            }
            InputSubMode.TV_REMOTE -> {
                TvRemoteScreen(
                    settings = settings,
                    isTabletLayout = isTabletLayout,
                    onSendKey = onSendKey,
                    onSendConsumerKey = onSendConsumerKey,
                    onBackClick = { onSendConsumerKey(HidConstants.CONSUMER_BACK, "Back") },
                    onHomeClick = { onSendConsumerKey(HidConstants.CONSUMER_HOME, "Home") },
                    onRecentAppsClick = { onSendKey(HidConstants.KEY_TAB, HidConstants.MOD_LALT) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            InputSubMode.HUB_HOME -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = palette.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 46.dp)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, palette.borderSubtle, RoundedCornerShape(12.dp))
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
                                modifier = Modifier
                                    .weight(1.42f)
                                    .testTag("input_mode_smart")
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
                                modifier = Modifier
                                    .weight(1.04f)
                                    .testTag("input_mode_pc")
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
                                modifier = Modifier
                                    .weight(0.94f)
                                    .testTag("input_mode_gamepad")
                            )
                        }
                    }

                    SmartKeyboardScreen(
                        settings = settings,
                        isTabletLayout = isTabletLayout,
                        onSendText = onSendText,
                        onSendKey = onSendKey,
                        onSendConsumerKey = onSendConsumerKey,
                        onPlaceQuickToolAt = onPlaceQuickToolAt,
                        onResizeQuickTool = onResizeQuickTool,
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
fun SmartKeyboardScreen(
    settings: RemoteSettings,
    isTabletLayout: Boolean,
    onSendText: (String) -> Unit,
    onSendKey: (hidKeyCode: Int, modifier: Int) -> Unit,
    onSendConsumerKey: (usageCode: Int, name: String) -> Unit,
    onPlaceQuickToolAt: (tool: QuickToolId, targetCol: Int, targetRow: Int) -> Unit = { _, _, _ -> },
    onResizeQuickTool: (QuickToolId) -> Unit = {},
    onRemoveQuickToolToPool: (QuickToolId) -> Unit = {},
    onAddQuickToolFromPool: (QuickToolId) -> Unit = {},
    onSetLiveTypingMode: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    ReceiverLockedControlContainer(
        isLockedForReceiver = settings.appRole == AppRole.RECEIVER,
        modifier = modifier.fillMaxSize()
    ) {
        SmartKeyboardSubView(
            settings = settings,
            isTabletLayout = isTabletLayout,
            onSendText = onSendText,
            onSendKey = onSendKey,
            onSendConsumerKey = onSendConsumerKey,
            onPlaceQuickToolAt = onPlaceQuickToolAt,
            onResizeQuickTool = onResizeQuickTool,
            onRemoveQuickToolToPool = onRemoveQuickToolToPool,
            onAddQuickToolFromPool = onAddQuickToolFromPool,
            onSetLiveTypingMode = onSetLiveTypingMode,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun InputModeTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    val bg by animateColorAsState(
        targetValue = if (selected) palette.controlThumb else Color.Transparent,
        animationSpec = tween(140),
        label = "mode_tab_bg"
    )
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (selected) Modifier.border(1.dp, palette.borderElevated, RoundedCornerShape(8.dp))
                else Modifier
            )
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
                tint = if (selected) palette.textPrimary else palette.textMuted,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (selected) palette.textPrimary else palette.textSecondary,
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
    isTabletLayout: Boolean,
    onSendText: (String) -> Unit,
    onSendKey: (hidKeyCode: Int, modifier: Int) -> Unit,
    onSendConsumerKey: (usageCode: Int, name: String) -> Unit,
    onPlaceQuickToolAt: (tool: QuickToolId, targetCol: Int, targetRow: Int) -> Unit,
    onResizeQuickTool: (QuickToolId) -> Unit,
    onRemoveQuickToolToPool: (QuickToolId) -> Unit,
    onAddQuickToolFromPool: (QuickToolId) -> Unit,
    onSetLiveTypingMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    var inputText by remember { mutableStateOf("") }
    var liveSentinelField by remember {
        mutableStateOf(TextFieldValue(text = " ", selection = TextRange(1)))
    }
    var isQuickToolsEditMode by remember { mutableStateOf(false) }
    val batchFocusRequester = remember { FocusRequester() }
    val liveFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val liveTypingMode = settings.liveTypingMode
    val activeSet = remember(settings.quickToolsPlacements) {
        settings.quickToolsPlacements.map { it.tool }.toSet()
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val imeAboveNav = (imeBottom - navBarBottom).coerceAtLeast(0.dp)
    val smoothBottomClearance = if (isTabletLayout) {
        maxOf(imeAboveNav + 8.dp, 8.dp)
    } else {
        maxOf(imeAboveNav + 6.dp, 74.dp)
    }

    fun dispatchQuickTool(tool: QuickToolId) {
        when (tool) {
            QuickToolId.SPACE -> onSendKey(HidConstants.KEY_SPACE, 0)
            QuickToolId.BACKSPACE -> onSendKey(HidConstants.KEY_BACKSPACE, 0)
            QuickToolId.ENTER -> onSendKey(HidConstants.KEY_ENTER, 0)
            QuickToolId.DEL -> onSendKey(HidConstants.KEY_DELETE, 0)
            QuickToolId.ZOOM_IN -> onSendKey(HidConstants.KEY_EQUAL, HidConstants.MOD_LCTRL)
            QuickToolId.ZOOM_OUT -> onSendKey(HidConstants.KEY_MINUS, HidConstants.MOD_LCTRL)
            QuickToolId.CUT -> onSendKey(HidConstants.KEY_X, HidConstants.MOD_LCTRL)
            QuickToolId.COPY -> onSendKey(HidConstants.KEY_C, HidConstants.MOD_LCTRL)
            QuickToolId.PASTE -> onSendKey(HidConstants.KEY_V, HidConstants.MOD_LCTRL)
            QuickToolId.ESC -> onSendKey(HidConstants.KEY_ESC, 0)
            QuickToolId.TAB -> onSendKey(HidConstants.KEY_TAB, 0)
            QuickToolId.HOME_KEY -> onSendKey(HidConstants.KEY_HOME, 0)
            QuickToolId.END_KEY -> onSendKey(HidConstants.KEY_END, 0)
            QuickToolId.PG_UP -> onSendKey(HidConstants.KEY_PAGEUP, 0)
            QuickToolId.PG_DN -> onSendKey(HidConstants.KEY_PAGEDOWN, 0)
            QuickToolId.NEW_TAB -> onSendKey(HidConstants.KEY_T, HidConstants.MOD_LCTRL)
            QuickToolId.CLOSE_TAB -> onSendKey(HidConstants.KEY_W, HidConstants.MOD_LCTRL)
            QuickToolId.REFRESH -> onSendKey(HidConstants.KEY_R, HidConstants.MOD_LCTRL)
            QuickToolId.ADDRESS_BAR -> onSendKey(HidConstants.KEY_L, HidConstants.MOD_LCTRL)
            QuickToolId.FULLSCREEN -> onSendKey(HidConstants.KEY_F11, 0)
            QuickToolId.ALT_F4 -> onSendKey(HidConstants.KEY_F4, HidConstants.MOD_LALT)
            QuickToolId.SELECT_ALL -> onSendKey(HidConstants.KEY_A, HidConstants.MOD_LCTRL)
            QuickToolId.UNDO -> onSendKey(HidConstants.KEY_Z, HidConstants.MOD_LCTRL)
            QuickToolId.REDO -> onSendKey(HidConstants.KEY_Y, HidConstants.MOD_LCTRL)
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
            QuickToolId.DPAD -> onSendKey(HidConstants.KEY_ENTER, 0)
        }
    }

    fun handleUpperTileTap(tool: QuickToolId) {
        if (isQuickToolsEditMode && tool !in activeSet) {
            onAddQuickToolFromPool(tool)
        } else {
            dispatchQuickTool(tool)
        }
    }

    val fnTools = remember(activeSet) {
        listOf(
            QuickToolId.ESC, QuickToolId.TAB, QuickToolId.HOME_KEY,
            QuickToolId.END_KEY, QuickToolId.PG_UP, QuickToolId.PG_DN
        ).filter { it !in activeSet }
    }
    val browserTools = remember(activeSet) {
        listOf(
            QuickToolId.NEW_TAB, QuickToolId.CLOSE_TAB, QuickToolId.REFRESH, QuickToolId.ADDRESS_BAR
        ).filter { it !in activeSet }
    }
    val tabletZoomTools = remember(activeSet) {
        listOf(
            QuickToolId.FULLSCREEN, QuickToolId.ZOOM_IN, QuickToolId.ZOOM_OUT, QuickToolId.ALT_F4
        ).filter { it !in activeSet }
    }
    val clipboardTools = remember(activeSet) {
        listOf(
            QuickToolId.SELECT_ALL, QuickToolId.CUT, QuickToolId.COPY,
            QuickToolId.PASTE, QuickToolId.UNDO, QuickToolId.REDO
        ).filter { it !in activeSet }
    }
    val poolExtras = remember(activeSet) {
        listOf(
            QuickToolId.DPAD, QuickToolId.SPACE, QuickToolId.BACKSPACE, QuickToolId.ENTER, QuickToolId.DEL,
            QuickToolId.TR_CCEDIL, QuickToolId.TR_GBREVE, QuickToolId.TR_DOTLESS_I,
            QuickToolId.TR_OUML, QuickToolId.TR_SCEDIL, QuickToolId.TR_UUML,
            QuickToolId.TR_CAPITAL_I, QuickToolId.PUNCT_COMMA, QuickToolId.PUNCT_DOT
        ).filter { it !in activeSet }
    }

    val upperCatalogCard: @Composable (Modifier) -> Unit = { cardMod ->
        Surface(
            color = palette.shadedPanel,
            shape = RoundedCornerShape(14.dp),
            modifier = cardMod
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (isQuickToolsEditMode) palette.borderElevated else palette.borderSubtle,
                    RoundedCornerShape(14.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "EKLEME MODU: Alttaki ızgaraya eklemek için (+) simgeli tuşa dokunun",
                    color = palette.primary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )

                if (fnTools.isNotEmpty()) {
                    SectionHeader("FONKSİYON TUŞLARI")
                    ProportionalUpperPoolGrid(
                        tools = fnTools,
                        isEditMode = isQuickToolsEditMode,
                        onToolTap = ::handleUpperTileTap,
                        onSendKey = onSendKey
                    )
                }

                if (browserTools.isNotEmpty()) {
                    SectionHeader("TARAYICI & SEKMELER")
                    ProportionalUpperPoolGrid(
                        tools = browserTools,
                        isEditMode = isQuickToolsEditMode,
                        onToolTap = ::handleUpperTileTap,
                        onSendKey = onSendKey
                    )
                }

                if (tabletZoomTools.isNotEmpty()) {
                    SectionHeader("EKRAN & YAKINLAŞTIRMA")
                    ProportionalUpperPoolGrid(
                        tools = tabletZoomTools,
                        isEditMode = isQuickToolsEditMode,
                        onToolTap = ::handleUpperTileTap,
                        onSendKey = onSendKey
                    )
                }

                if (clipboardTools.isNotEmpty()) {
                    SectionHeader("PANO & METİN KISAYOLLARI")
                    ProportionalUpperPoolGrid(
                        tools = clipboardTools,
                        isEditMode = isQuickToolsEditMode,
                        onToolTap = ::handleUpperTileTap,
                        onSendKey = onSendKey
                    )
                }

                if (poolExtras.isNotEmpty()) {
                    SectionHeader("YÖN & TÜRKÇE KARAKTER HAVUZU")
                    ProportionalUpperPoolGrid(
                        tools = poolExtras,
                        isEditMode = isQuickToolsEditMode,
                        onToolTap = ::handleUpperTileTap,
                        onSendKey = onSendKey
                    )
                }
            }
        }
    }

    val typingBar: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // LEFT: Canlı / Bekle mode toggle
            Surface(
                color = palette.controlThumb,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (liveTypingMode) palette.primary else GreenSuccess,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        val nextLive = !liveTypingMode
                        onSetLiveTypingMode(nextLive)
                        if (nextLive) {
                            liveSentinelField = TextFieldValue(" ", TextRange(1))
                        }
                    }
                    .testTag("live_batch_typing_toggle")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (liveTypingMode) Icons.Default.Bolt else Icons.Default.Pause,
                        contentDescription = if (liveTypingMode) "Canlı Yazım Aktif" else "Bekleme Modu Aktif",
                        tint = if (liveTypingMode) palette.primary else GreenSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (liveTypingMode) "CANLI" else "BEKLE",
                        color = if (liveTypingMode) palette.primary else GreenSuccess,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        softWrap = false
                    )
                }
            }

            if (liveTypingMode) {
                // Hidden 1dp sentinel text receiver + tactile clickable live trigger pill
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = liveSentinelField,
                        onValueChange = { updated ->
                            val newTxt = updated.text
                            when {
                                newTxt.length > 1 -> {
                                    val typed = if (newTxt.startsWith(" ")) newTxt.substring(1) else newTxt
                                    if (typed.isNotEmpty()) {
                                        onSendText(typed.replace("\n", ""))
                                        if (typed.contains("\n")) {
                                            onSendKey(HidConstants.KEY_ENTER, 0)
                                        }
                                    }
                                }
                                newTxt.isEmpty() -> {
                                    onSendKey(HidConstants.KEY_BACKSPACE, 0)
                                }
                            }
                            liveSentinelField = TextFieldValue(text = " ", selection = TextRange(1))
                        },
                        singleLine = true,
                        cursorBrush = SolidColor(Color.Transparent),
                        textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                onSendKey(HidConstants.KEY_ENTER, 0)
                            }
                        ),
                        modifier = Modifier
                            .size(1.dp)
                            .alpha(0f)
                            .focusRequester(liveFocusRequester)
                            .testTag("live_typing_hidden_field")
                    )

                    Surface(
                        color = palette.surfaceContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.2.dp, palette.primary.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                            .clickable {
                                liveFocusRequester.requestFocus()
                                keyboardController?.show()
                            }
                            .testTag("live_typing_ime_trigger")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Canlı Yazım Klavyesini Aç",
                                tint = palette.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Canlı Giriş • Klavyeyi Aç",
                                color = palette.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Surface(
                    color = palette.primary,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSendKey(HidConstants.KEY_ENTER, 0) }
                        .testTag("keyboard_send_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                            contentDescription = "Enter",
                            tint = palette.onAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Taslak yazın ve gönderin...",
                            color = palette.textMuted,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = false,
                    minLines = 1,
                    maxLines = 4,
                    trailingIcon = {
                        if (inputText.isNotEmpty()) {
                            IconButton(onClick = { inputText = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Temizle",
                                    tint = palette.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = palette.surfaceContainer,
                        unfocusedContainerColor = palette.surfaceContainer,
                        focusedBorderColor = palette.primary,
                        unfocusedBorderColor = palette.borderSubtle,
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotEmpty()) {
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
                        .focusRequester(batchFocusRequester)
                        .testTag("soft_keyboard_input_field")
                )

                Surface(
                    color = palette.primary,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (inputText.isNotEmpty()) {
                                onSendText(inputText)
                                inputText = ""
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
                            tint = palette.onAccent,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }

    if (isTabletLayout) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = smoothBottomClearance),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isQuickToolsEditMode) {
                // Edit Mode: Left pane = Pool Catalog, Right pane = Editable 2D Grid + Typing Bar
                Column(
                    modifier = Modifier
                        .weight(0.96f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    upperCatalogCard(Modifier.weight(1f))
                }

                Column(
                    modifier = Modifier
                        .weight(1.04f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickToolsGridSection(
                            placements = settings.quickToolsPlacements,
                            liveTypingMode = liveTypingMode,
                            isTurkishLayout = settings.isTurkishKeyboardLayout,
                            isEditMode = true,
                            onEditModeChange = { isQuickToolsEditMode = it },
                            onPlaceQuickToolAt = onPlaceQuickToolAt,
                            onResizeQuickTool = onResizeQuickTool,
                            onRemoveToPool = onRemoveQuickToolToPool,
                            onTriggerTool = { tool -> dispatchQuickTool(tool) },
                            onSendKey = onSendKey,
                            headerEndPadding = 0.dp
                        )
                    }
                    typingBar()
                }
            } else {
                // Normal Mode: Left pane = Quick Tools Grid, Right pane = Typing Workspace + Bottom-Right Typing Bar
                Column(
                    modifier = Modifier
                        .weight(1.08f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickToolsGridSection(
                        placements = settings.quickToolsPlacements,
                        liveTypingMode = liveTypingMode,
                        isTurkishLayout = settings.isTurkishKeyboardLayout,
                        isEditMode = false,
                        onEditModeChange = { isQuickToolsEditMode = it },
                        onPlaceQuickToolAt = onPlaceQuickToolAt,
                        onResizeQuickTool = onResizeQuickTool,
                        onRemoveToPool = onRemoveQuickToolToPool,
                        onTriggerTool = { tool -> dispatchQuickTool(tool) },
                        onSendKey = onSendKey,
                        headerEndPadding = 0.dp
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(0.92f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = palette.shadedPanel,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, palette.borderSubtle, RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (liveTypingMode) "ANLIK METİN GİRİŞİ (CANLI)" else "TASLAK METİN EDİTÖRÜ (BEKLE)",
                                    color = palette.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = if (liveTypingMode) {
                                        "Sağ alttaki 'Klavyeyi Aç' alanına dokunarak bastığınız her harfi ve geri silme tuşunu anında alıcı cihaza aktarabilirsiniz."
                                    } else {
                                        "Metninizi sağ alttaki çok satırlı alanda hazırlayıp Gönder butonuna bastığınızda tek seferde iletin."
                                    },
                                    color = palette.textSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Triple("Geri Sil", HidConstants.KEY_BACKSPACE, 0),
                                    Triple("Boşluk", HidConstants.KEY_SPACE, 0),
                                    Triple("Enter", HidConstants.KEY_ENTER, 0),
                                    Triple("Yapıştır", HidConstants.KEY_V, HidConstants.MOD_LCTRL)
                                ).forEach { (lbl, code, mod) ->
                                    Surface(
                                        color = palette.surfaceContainer,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.dp, palette.borderSubtle, RoundedCornerShape(10.dp))
                                            .clickable { onSendKey(code, mod) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = lbl,
                                                color = palette.textPrimary,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    typingBar()
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = smoothBottomClearance),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AnimatedVisibility(
                visible = isQuickToolsEditMode,
                enter = expandVertically(tween(220)) + fadeIn(tween(160)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(120)),
                modifier = Modifier.then(if (isQuickToolsEditMode) Modifier.weight(1f) else Modifier)
            ) {
                upperCatalogCard(Modifier.fillMaxSize())
            }

            if (!isQuickToolsEditMode) {
                Spacer(modifier = Modifier.weight(1f))
            }

            QuickToolsGridSection(
                placements = settings.quickToolsPlacements,
                liveTypingMode = liveTypingMode,
                isTurkishLayout = settings.isTurkishKeyboardLayout,
                isEditMode = isQuickToolsEditMode,
                onEditModeChange = { isQuickToolsEditMode = it },
                onPlaceQuickToolAt = onPlaceQuickToolAt,
                onResizeQuickTool = onResizeQuickTool,
                onRemoveToPool = onRemoveQuickToolToPool,
                onTriggerTool = { tool -> dispatchQuickTool(tool) },
                onSendKey = onSendKey,
                headerEndPadding = 46.dp
            )

            typingBar()
        }
    }
}

@Composable
private fun ProportionalUpperPoolGrid(
    tools: List<QuickToolId>,
    isEditMode: Boolean,
    onToolTap: (QuickToolId) -> Unit,
    onSendKey: (hidKeyCode: Int, modifier: Int) -> Unit
) {
    val palette = TabRemoteTheme.palette
    val cells = remember(tools) {
        SparseGridEngine.packLinear(
            items = tools.map { tool ->
                val upperColSpan = when {
                    tool.isDpad -> 3
                    tool.colSpan >= 2 -> 2
                    else -> 1
                }
                SparseGridNode(tool, upperColSpan, tool.rowSpan)
            },
            columns = SparseGridEngine.GRID_COLUMNS
        )
    }
    val totalRows = remember(cells) { SparseGridEngine.totalRows(cells, minRows = 1) }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val gapDp = 5.dp
        val cellHeightDp = 42.dp
        val gapPx = with(density) { gapDp.toPx() }
        val cellHeightPx = with(density) { cellHeightDp.toPx() }
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val cellWidthPx = ((totalWidthPx - gapPx * (SparseGridEngine.GRID_COLUMNS - 1)) /
            SparseGridEngine.GRID_COLUMNS).coerceAtLeast(10f)
        val totalHeightDp = with(density) {
            (totalRows * cellHeightPx + (totalRows - 1) * gapPx).toDp()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeightDp)
        ) {
            cells.forEach { cell ->
                val tool = cell.id
                val xDp = with(density) { (cell.col * (cellWidthPx + gapPx)).toDp() }
                val yDp = with(density) { (cell.row * (cellHeightPx + gapPx)).toDp() }
                val wDp = with(density) { (cell.colSpan * cellWidthPx + (cell.colSpan - 1) * gapPx).toDp() }
                val hDp = with(density) { (cell.rowSpan * cellHeightPx + (cell.rowSpan - 1) * gapPx).toDp() }

                Box(
                    modifier = Modifier
                        .offset(x = xDp, y = yDp)
                        .size(width = wDp, height = hDp)
                        .testTag("upper_tool_${tool.name.lowercase()}")
                ) {
                    if (tool.isDpad) {
                        QuickToolsDpadTile(
                            isEditMode = isEditMode,
                            onSendKey = onSendKey,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (isEditMode) Modifier.clickable { onToolTap(tool) } else Modifier)
                        )
                    } else {
                        UpperPoolProportionalTile(
                            tool = tool,
                            colSpan = cell.colSpan,
                            showAddHighlight = isEditMode,
                            onClick = { onToolTap(tool) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (isEditMode) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(17.dp)
                                .clip(CircleShape)
                                .background(palette.primary)
                                .border(1.dp, palette.onAccent.copy(alpha = 0.7f), CircleShape)
                                .clickable { onToolTap(tool) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "${tool.label} Hızlı Araçlara Ekle",
                                tint = palette.onAccent,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpperPoolProportionalTile(
    tool: QuickToolId,
    colSpan: Int,
    showAddHighlight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    val icon = quickToolIcon(tool) ?: upperFallbackIcon(tool)
    val isTurkishChar = tool.name.startsWith("TR_") || tool.name.startsWith("PUNCT_")

    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(
                1.dp,
                if (showAddHighlight) palette.primary.copy(alpha = 0.85f) else palette.borderSubtle,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
    ) {
        if (colSpan >= 2) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tool.label,
                        tint = palette.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = if (icon != null) Alignment.End else Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = tool.label,
                        color = palette.textPrimary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (tool.shortcutHint.isNotBlank()) {
                        Text(
                            text = tool.shortcutHint,
                            color = palette.textMuted,
                            fontSize = 7.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 3.dp, vertical = 3.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tool.label,
                        tint = palette.textPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    if (tool.shortcutHint.isNotBlank()) {
                        Text(
                            text = tool.shortcutHint,
                            color = palette.textMuted,
                            fontSize = 7.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                } else {
                    Text(
                        text = tool.shortLabel,
                        color = palette.textPrimary,
                        fontSize = if (isTurkishChar) 14.sp else 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (tool.shortcutHint.isNotBlank()) {
                        Text(
                            text = tool.shortcutHint,
                            color = palette.textMuted,
                            fontSize = 7.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun upperFallbackIcon(tool: QuickToolId): ImageVector? = when (tool) {
    QuickToolId.NEW_TAB -> Icons.Default.Add
    QuickToolId.CLOSE_TAB -> Icons.Default.Close
    QuickToolId.REFRESH -> Icons.Default.Refresh
    QuickToolId.ADDRESS_BAR -> Icons.Default.Search
    QuickToolId.FULLSCREEN -> Icons.Default.Fullscreen
    QuickToolId.SELECT_ALL -> Icons.Default.SelectAll
    QuickToolId.CUT -> Icons.Default.ContentCut
    QuickToolId.COPY -> Icons.Default.ContentCopy
    QuickToolId.PASTE -> Icons.Default.ContentPaste
    QuickToolId.UNDO -> Icons.AutoMirrored.Filled.Undo
    QuickToolId.REDO -> Icons.AutoMirrored.Filled.Redo
    else -> null
}

@Composable
private fun SectionHeader(title: String) {
    val palette = TabRemoteTheme.palette
    Text(
        text = title,
        color = palette.textMuted,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 2.dp, top = 2.dp)
    )
}
