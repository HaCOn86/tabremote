package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.DeviceSyncRepository
import com.example.data.PairedDevice
import com.example.data.SettingsPrefsStore
import com.example.model.AirMouseScrollMode
import com.example.model.AppRole
import com.example.model.AuthSessionMode
import com.example.model.ConnectionStatus
import com.example.model.GamepadStyle
import com.example.model.InputSubMode
import com.example.model.ReceiverTelemetry
import com.example.model.SettingsSubPage
import com.example.model.ThemePreset
import com.example.model.WifiQuickWidgetId
import com.example.ui.components.AccessibilityAndPermissionModal
import com.example.ui.components.BluetoothStatusDot
import com.example.ui.components.ExpandableConnectionPopup
import com.example.ui.components.StatusBarGlassScrim
import com.example.ui.components.auditAppPermissions
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.GamepadScreenContent
import com.example.ui.screens.LandscapePcKeyboardContent
import com.example.ui.screens.MediaPanelScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmartKeyboardScreen
import com.example.ui.screens.TvRemoteScreen
import com.example.ui.screens.UnifiedControlScreen
import com.example.ui.screens.signOutFromGoogle
import com.example.ui.theme.TabRemoteTheme
import com.example.ui.theme.adaptiveWidthClassFor
import com.example.ui.theme.isTablet
import com.example.viewmodel.NavTab
import com.example.viewmodel.RemoteViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    private var activeViewModel: RemoteViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            AppNavigation(
                onBindActiveViewModel = { activeViewModel = it }
            )
        }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (activeViewModel?.handleKeyEvent(event) == true) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

@Composable
fun AppNavigation(
    onBindActiveViewModel: (RemoteViewModel?) -> Unit = {}
) {
    val context = LocalContext.current
    val prefsStore = remember(context) { SettingsPrefsStore(context) }
    val initialSettings = remember(prefsStore) { prefsStore.loadSettings() }
    var activeThemePreset by remember { mutableStateOf(initialSettings.themePreset) }
    var activeDynamicColor by remember { mutableStateOf(initialSettings.useDynamicColor) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var sessionMode by remember {
        mutableStateOf(
            if (Firebase.auth.currentUser != null) AuthSessionMode.GOOGLE
            else prefsStore.getAuthSessionMode()
        )
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val u = auth.currentUser
            currentUser = u
            if (u != null) {
                sessionMode = AuthSessionMode.GOOGLE
                prefsStore.setAuthSessionMode(AuthSessionMode.GOOGLE)
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
            onBindActiveViewModel(null)
        }
    }

    val user = currentUser
    val activeMode = when {
        user != null -> AuthSessionMode.GOOGLE
        sessionMode != null -> sessionMode!!
        else -> null
    }

    TabRemoteTheme(
        themePreset = activeThemePreset,
        dynamicColor = activeDynamicColor
    ) {
        if (activeMode == null) {
            LaunchedEffect(Unit) { onBindActiveViewModel(null) }
            AuthScreen(
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    sessionMode = AuthSessionMode.GOOGLE
                    prefsStore.setAuthSessionMode(AuthSessionMode.GOOGLE)
                },
                onContinueBluetoothOnly = {
                    sessionMode = AuthSessionMode.BLUETOOTH_ONLY
                    prefsStore.setAuthSessionMode(AuthSessionMode.BLUETOOTH_ONLY)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val vmKey = user?.uid ?: "local_${activeMode.name}"
            key(vmKey) {
                val factory = remember(vmKey) {
                    viewModelFactory {
                        initializer {
                            val app = this[APPLICATION_KEY]!!
                            val appCtx = app.applicationContext
                            RemoteViewModel(
                                appContext = appCtx,
                                deviceSyncRepository = DeviceSyncRepository(appCtx),
                                currentUserId = user?.uid,
                                authSessionMode = activeMode
                            )
                        }
                    }
                }
                val vm: RemoteViewModel = viewModel(key = vmKey, factory = factory)
                LaunchedEffect(vm) {
                    onBindActiveViewModel(vm)
                }

                RequestPermissionsEffect(onGranted = { vm.refreshBluetooth() })

                TabRemoteApp(
                    viewModel = vm,
                    prefsStore = prefsStore,
                    authSessionMode = activeMode,
                    userEmail = user?.email.orEmpty(),
                    userDisplayName = user?.displayName.orEmpty(),
                    onThemeChanged = { preset, dynamic ->
                        activeThemePreset = preset
                        activeDynamicColor = dynamic
                    },
                    onGoogleLinked = {
                        currentUser = Firebase.auth.currentUser
                        sessionMode = AuthSessionMode.GOOGLE
                        prefsStore.setAuthSessionMode(AuthSessionMode.GOOGLE)
                    },
                    onReturnToAuthScreen = {
                        sessionMode = null
                        prefsStore.setAuthSessionMode(null)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun TabRemoteApp(
    viewModel: RemoteViewModel,
    prefsStore: SettingsPrefsStore,
    authSessionMode: AuthSessionMode,
    userEmail: String,
    userDisplayName: String,
    onThemeChanged: (ThemePreset, Boolean) -> Unit,
    onGoogleLinked: () -> Unit,
    onReturnToAuthScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val inputSubMode by viewModel.inputSubMode.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val unifiedAirMouseExpanded by viewModel.unifiedAirMouseExpanded.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.activeConnectionStatus.collectAsStateWithLifecycle()
    val bondedDevices by viewModel.hidController.bondedDevices.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.hidController.discoveredDevices.collectAsStateWithLifecycle()
    val isScanningBluetooth by viewModel.hidController.isScanning.collectAsStateWithLifecycle()
    val hidDiagnostics by viewModel.hidController.hidDiagnostics.collectAsStateWithLifecycle()
    val airMouseActive by viewModel.airMouseActive.collectAsStateWithLifecycle()
    val airMouseTelemetry by viewModel.airMouseTelemetry.collectAsStateWithLifecycle()
    val localWifiIp by viewModel.localWifiIp.collectAsStateWithLifecycle()
    val accessibilityEnabled by viewModel.accessibilityEnabled.collectAsStateWithLifecycle()
    val notificationListenerEnabled by viewModel.notificationListenerEnabled.collectAsStateWithLifecycle()
    val receiverTelemetry by viewModel.receiverTelemetry.collectAsStateWithLifecycle()
    val cloudDevices by viewModel.cloudDevices.collectAsStateWithLifecycle()
    val isReceiverServerRunning by viewModel.wifiReceiverServer.isRunning.collectAsStateWithLifecycle()
    val connectedControllerName by viewModel.wifiReceiverServer.connectedController.collectAsStateWithLifecycle()

    LaunchedEffect(settings.themePreset, settings.useDynamicColor) {
        onThemeChanged(settings.themePreset, settings.useDynamicColor)
    }

    val palette = TabRemoteTheme.palette
    val pendingSettingsSubPage by viewModel.pendingSettingsSubPage.collectAsStateWithLifecycle()

    val isWifiConnected = (connectionStatus as? ConnectionStatus.Connected)?.isWifi == true
    val isReceiverActive = settings.appRole == AppRole.RECEIVER && isReceiverServerRunning

    var showConnectionPopup by remember { mutableStateOf(false) }
    var previousNonFullscreenSubMode by remember { mutableStateOf(InputSubMode.HUB_HOME) }
    var isFullscreenInputOverlay by remember { mutableStateOf(false) }

    // Accessibility & Permissions Audit Modal State (shown at startup ONLY for Google account sessions)
    var permRefreshNonce by remember { mutableIntStateOf(0) }
    val permAudit = remember(accessibilityEnabled, notificationListenerEnabled, permRefreshNonce) {
        auditAppPermissions(context, accessibilityEnabled, notificationListenerEnabled)
    }
    var showPermissionWizard by remember(authSessionMode) {
        mutableStateOf(
            authSessionMode == AuthSessionMode.GOOGLE &&
                !prefsStore.isStartupPermissionCheckDone() &&
                (!permAudit.accessibilityReady || !permAudit.bluetoothReady)
        )
    }
    var wizardAllowSkip by remember { mutableStateOf(settings.appRole == AppRole.CONTROLLER) }
    var wizardTitle by remember { mutableStateOf("İzinler & Erişilebilirlik Kontrolü") }
    var wizardSubtitle by remember {
        mutableStateOf("Alıcı tablet imleci ve Bluetooth taraması için gerekli sistem izinlerini kontrol edin.")
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLocalEnvironment()
                permRefreshNonce++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val wizardPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshBluetooth()
        permRefreshNonce++
    }

    LaunchedEffect(currentTab, inputSubMode) {
        val inOverlayMode = currentTab == NavTab.CONTROL_HUB &&
            (inputSubMode == InputSubMode.PC_KEYBOARD || inputSubMode == InputSubMode.GAMEPAD)
        if (!inOverlayMode && isFullscreenInputOverlay) {
            isFullscreenInputOverlay = false
            viewModel.selectInputSubMode(previousNonFullscreenSubMode)
        }
    }

    BackHandler(enabled = isFullscreenInputOverlay) {
        isFullscreenInputOverlay = false
        viewModel.selectInputSubMode(previousNonFullscreenSubMode)
    }

    BackHandler(enabled = showConnectionPopup && !isFullscreenInputOverlay) {
        showConnectionPopup = false
    }

    BackHandler(
        enabled = !isFullscreenInputOverlay &&
            !showConnectionPopup &&
            currentTab == NavTab.CONTROL_HUB &&
            inputSubMode != InputSubMode.HUB_HOME
    ) {
        viewModel.selectInputSubMode(InputSubMode.HUB_HOME)
    }

    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val visibleTabs = remember {
        listOf(NavTab.TOUCHPAD, NavTab.KEYBOARD, NavTab.CONTROL_HUB, NavTab.SETTINGS)
    }

    if (showPermissionWizard) {
        AccessibilityAndPermissionModal(
            title = wizardTitle,
            subtitle = wizardSubtitle,
            audit = permAudit,
            allowSkipForController = wizardAllowSkip,
            onRequestMissingPermissions = { missing ->
                if (missing.isNotEmpty()) {
                    wizardPermLauncher.launch(missing.toTypedArray())
                }
            },
            onRefreshStatus = {
                viewModel.refreshLocalEnvironment()
                permRefreshNonce++
            },
            onDismiss = {
                prefsStore.setStartupPermissionCheckDone(true)
                showPermissionWizard = false
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = palette.background,
        modifier = modifier
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideTablet = adaptiveWidthClassFor(maxWidth).isTablet()
            val isLandscape = maxWidth > maxHeight
            val isTabletLayout = isWideTablet || isLandscape
            val isPhoneLandscapeFullscreen = isLandscape && !isWideTablet

            ImmersiveLandscapeEffect(
                forceLandscapeOrientation = isFullscreenInputOverlay,
                hideSystemBars = isFullscreenInputOverlay || isPhoneLandscapeFullscreen
            )

            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!isFullscreenInputOverlay && isTabletLayout) {
                        TabRemoteVerticalPillRail(
                            tabs = visibleTabs,
                            selectedTab = currentTab,
                            topStatusSpacer = (if (isPhoneLandscapeFullscreen) 6.dp else statusBarTopPadding + 6.dp) + 46.dp,
                            onSelectTab = {
                                showConnectionPopup = false
                                viewModel.selectTab(it)
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (isFullscreenInputOverlay || isTabletLayout) Modifier
                                    else Modifier.widthIn(max = 680.dp)
                                )
                                .then(
                                    if (isFullscreenInputOverlay) {
                                        Modifier
                                    } else {
                                        Modifier.padding(
                                            bottom = if (isPhoneLandscapeFullscreen) 4.dp else navBarBottomPadding,
                                            start = if (isTabletLayout) 8.dp else 8.dp,
                                            end = if (isTabletLayout) 12.dp else 8.dp
                                        )
                                    }
                                )
                        ) {
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = {
                                    if (targetState.ordinal > initialState.ordinal) {
                                        (slideInHorizontally { width -> width / 3 } + fadeIn()) togetherWith
                                            (slideOutHorizontally { width -> -width / 3 } + fadeOut())
                                    } else {
                                        (slideInHorizontally { width -> -width / 3 } + fadeIn()) togetherWith
                                            (slideOutHorizontally { width -> width / 3 } + fadeOut())
                                    }
                                },
                                label = "tab_transition",
                                modifier = Modifier.weight(1f)
                            ) { tab ->
                                when (tab) {
                                    NavTab.TOUCHPAD -> {
                                        UnifiedControlScreen(
                                            settings = settings,
                                            isWifiMode = isWifiConnected,
                                            isTabletLayout = isTabletLayout,
                                            airMouseExpanded = unifiedAirMouseExpanded,
                                            airMouseActive = airMouseActive,
                                            telemetry = airMouseTelemetry,
                                            isGyroSupported = viewModel.isGyroSupported,
                                            onToggleAirMouseMode = { viewModel.toggleUnifiedAirMouse() },
                                            onTrackpadDrag = { dx, dy -> viewModel.onTrackpadDrag(dx, dy) },
                                            onScrollGutterDrag = { dy -> viewModel.onScrollGutterDrag(dy) },
                                            onTwoFingerVerticalScroll = { dy -> viewModel.onTwoFingerVerticalScroll(dy) },
                                            onTwoFingerGestureEnd = { viewModel.onTwoFingerGestureEnd() },
                                            onClassicPinchZoomStep = { delta -> viewModel.onClassicPinchZoomStep(delta) },
                                            onClassicPinchZoomEnd = { viewModel.onClassicPinchZoomEnd() },
                                            onScrollStep = { ticks -> viewModel.scrollStep(ticks) },
                                            onHorizontalPan = { dx -> viewModel.onHorizontalPan(dx) },
                                            onTwoFingerHorizontalSwipe = { dx ->
                                                viewModel.onTouchpadTwoFingerHorizontalSwipe(dx)
                                            },
                                            onHorizontalStep = { dir -> viewModel.stepHorizontal(dir) },
                                            onTrackpadTap = { viewModel.onTrackpadTap() },
                                            onTouchpadHoldStart = { viewModel.onTouchpadHoldStart() },
                                            onTouchpadHoldEnd = { viewModel.onTouchpadHoldEnd() },
                                            onMouseButtonClick = { btn -> viewModel.onMouseButtonClick(btn) },
                                            onMouseButtonDown = { btn -> viewModel.onMouseButtonDown(btn) },
                                            onMouseButtonUp = { btn -> viewModel.onMouseButtonUp(btn) },
                                            onUpdateSensitivity = { sens ->
                                                viewModel.updateSettings { it.copy(airMouseSensitivity = sens) }
                                            },
                                            onUpdateDeadband = { deadband ->
                                                viewModel.updateSettings { it.copy(airMouseDeadband = deadband) }
                                            },
                                            onToggleScrollMode = {
                                                viewModel.updateSettings { s ->
                                                    s.copy(
                                                        airMouseScrollMode = if (s.airMouseScrollMode == AirMouseScrollMode.SCROLL_STRIP)
                                                            AirMouseScrollMode.STEP_BUTTONS else AirMouseScrollMode.SCROLL_STRIP
                                                    )
                                                }
                                            },
                                            onBackClick = { viewModel.navigateBack() },
                                            onHomeClick = { viewModel.navigateHome() },
                                            onRecentAppsClick = { viewModel.openRecentApps() },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    NavTab.KEYBOARD -> {
                                        SmartKeyboardScreen(
                                            settings = settings,
                                            isTabletLayout = isTabletLayout,
                                            onSendText = { text -> viewModel.sendText(text) },
                                            onSendKey = { code, mod -> viewModel.sendKey(code, mod) },
                                            onSendConsumerKey = { code, name -> viewModel.sendConsumerKey(code, name) },
                                            onPlaceQuickToolAt = { tool, col, row ->
                                                viewModel.placeQuickToolAt(tool, col, row)
                                            },
                                            onResizeQuickTool = { tool -> viewModel.resizeQuickTool(tool) },
                                            onRemoveQuickToolToPool = { tool -> viewModel.removeQuickToolToPool(tool) },
                                            onAddQuickToolFromPool = { tool -> viewModel.addQuickToolFromPool(tool) },
                                            onSetLiveTypingMode = { enabled -> viewModel.setLiveTypingMode(enabled) },
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(
                                                    if (isPhoneLandscapeFullscreen) Modifier.padding(top = 6.dp)
                                                    else Modifier
                                                        .statusBarsPadding()
                                                        .consumeWindowInsets(WindowInsets.statusBars)
                                                        .padding(top = 4.dp)
                                                )
                                        )
                                    }

                                    NavTab.CONTROL_HUB -> {
                                        ControlHubContainer(
                                            inputSubMode = inputSubMode,
                                            isFullscreenOverlay = isFullscreenInputOverlay,
                                            isTabletLayout = isTabletLayout,
                                            isPhoneLandscapeFullscreen = isPhoneLandscapeFullscreen,
                                            settings = settings,
                                            isWifiConnected = isWifiConnected,
                                            connectionStatus = connectionStatus,
                                            receiverTelemetry = receiverTelemetry,
                                            cloudDevices = cloudDevices,
                                            localDeviceId = viewModel.localDeviceId,
                                            onSelectSubMode = { mode ->
                                                if (mode == InputSubMode.PC_KEYBOARD || mode == InputSubMode.GAMEPAD) {
                                                    if (inputSubMode != InputSubMode.PC_KEYBOARD &&
                                                        inputSubMode != InputSubMode.GAMEPAD
                                                    ) {
                                                        previousNonFullscreenSubMode = inputSubMode
                                                    }
                                                    viewModel.selectInputSubMode(mode)
                                                    isFullscreenInputOverlay = true
                                                } else {
                                                    previousNonFullscreenSubMode = mode
                                                    isFullscreenInputOverlay = false
                                                    viewModel.selectInputSubMode(mode)
                                                }
                                            },
                                            onExitFullscreen = {
                                                isFullscreenInputOverlay = false
                                                viewModel.selectInputSubMode(previousNonFullscreenSubMode)
                                            },
                                            onConnectWifiReceiver = { peer ->
                                                val freshAudit = auditAppPermissions(
                                                    context,
                                                    accessibilityEnabled,
                                                    notificationListenerEnabled
                                                )
                                                if (!freshAudit.bluetoothReady) {
                                                    wizardTitle = "Bağlantı İzinleri Kontrolü"
                                                    wizardSubtitle = "Alıcıya bağlanmadan önce ağ ve yakındaki cihaz izinlerini doğrulayın."
                                                    wizardAllowSkip = true
                                                    showPermissionWizard = true
                                                }
                                                viewModel.connectWifiReceiver(peer)
                                            },
                                            onOpenPermissionWizard = {
                                                permRefreshNonce++
                                                wizardTitle = "İzinler & Bildirim Dinleme"
                                                wizardSubtitle = "Erişilebilirlik, Bildirim Dinleme ve Bluetooth izin durumlarını yönetin."
                                                wizardAllowSkip = settings.appRole == AppRole.CONTROLLER
                                                showPermissionWizard = true
                                            },
                                            onMediaAction = { action -> viewModel.sendMediaPlayerAction(action) },
                                            onSendWifiWidget = { widget -> viewModel.sendWifiQuickWidget(widget) },
                                            onSetVolumePercent = { pct -> viewModel.setReceiverVolumePercent(pct) },
                                            onPlaceWifiWidgetAt = { widget, col, row ->
                                                viewModel.placeWifiWidgetAt(widget, col, row)
                                            },
                                            onResizeWifiWidget = { widget -> viewModel.resizeWifiWidget(widget) },
                                            onRemoveWifiWidget = { widget -> viewModel.removeWifiWidget(widget) },
                                            onAddWifiWidget = { widget -> viewModel.addWifiWidget(widget) },
                                            onTrackpadDrag = { dx, dy -> viewModel.onTrackpadDrag(dx, dy) },
                                            onTwoFingerVerticalScroll = { dy -> viewModel.onTwoFingerVerticalScroll(dy) },
                                            onTwoFingerGestureEnd = { viewModel.onTwoFingerGestureEnd() },
                                            onClassicPinchZoomStep = { delta -> viewModel.onClassicPinchZoomStep(delta) },
                                            onClassicPinchZoomEnd = { viewModel.onClassicPinchZoomEnd() },
                                            onTwoFingerHorizontalSwipe = { dx ->
                                                viewModel.onTouchpadTwoFingerHorizontalSwipe(dx)
                                            },
                                            onTrackpadTap = { viewModel.onTrackpadTap() },
                                            onTouchpadHoldStart = { viewModel.onTouchpadHoldStart() },
                                            onTouchpadHoldEnd = { viewModel.onTouchpadHoldEnd() },
                                            onMouseButtonClick = { btn -> viewModel.onMouseButtonClick(btn) },
                                            onMouseButtonDown = { btn -> viewModel.onMouseButtonDown(btn) },
                                            onMouseButtonUp = { btn -> viewModel.onMouseButtonUp(btn) },
                                            onBackClick = { viewModel.navigateBack() },
                                            onHomeClick = { viewModel.navigateHome() },
                                            onRecentAppsClick = { viewModel.openRecentApps() },
                                            onUpdateSettings = { transform -> viewModel.updateSettings(transform) },
                                            onUpdateGamepadOffsets = { transform -> viewModel.updateGamepadOffsets(transform) },
                                            onResetGamepadOffsets = { viewModel.resetGamepadOffsets() },
                                            onSendKey = { code, mod -> viewModel.sendKey(code, mod) },
                                            onSendConsumerKey = { code, name -> viewModel.sendConsumerKey(code, name) },
                                            onGamepadState = { btns, hat, lx, ly, rx, ry ->
                                                viewModel.sendGamepadState(btns, hat, lx, ly, rx, ry)
                                            }
                                        )
                                    }

                                    NavTab.SETTINGS -> {
                                        SettingsScreen(
                                            settings = settings,
                                            authSessionMode = authSessionMode,
                                            userEmail = userEmail,
                                            userDisplayName = userDisplayName,
                                            localDeviceId = viewModel.localDeviceId,
                                            localWifiIp = localWifiIp,
                                            accessibilityEnabled = accessibilityEnabled,
                                            notificationListenerEnabled = notificationListenerEnabled,
                                            isReceiverRunning = isReceiverServerRunning,
                                            connectedControllerName = connectedControllerName,
                                            cloudDevices = cloudDevices,
                                            connectionStatus = connectionStatus,
                                            bondedDevices = bondedDevices,
                                            discoveredDevices = discoveredDevices,
                                            isScanningBluetooth = isScanningBluetooth,
                                            hidDiagnostics = hidDiagnostics,
                                            isTabletLayout = isTabletLayout,
                                            onUpdateSettings = { transform -> viewModel.updateSettings(transform) },
                                            onChangeAppRole = { role ->
                                                viewModel.setAppRole(role)
                                                if (role == AppRole.RECEIVER && !accessibilityEnabled) {
                                                    wizardTitle = "Alıcı Modu: Erişilebilirlik Gerekli"
                                                    wizardSubtitle = "Tableti özel fare imleciyle kontrol etmek için Erişilebilirlik Servisini açın."
                                                    wizardAllowSkip = false
                                                    showPermissionWizard = true
                                                }
                                            },
                                            onUpdateLocalCloudName = { name -> viewModel.updateLocalCloudDeviceName(name) },
                                            onRefreshEnvironment = { viewModel.refreshLocalEnvironment() },
                                            onConnectWifiReceiver = { peer ->
                                                val freshAudit = auditAppPermissions(
                                                    context,
                                                    accessibilityEnabled,
                                                    notificationListenerEnabled
                                                )
                                                if (!freshAudit.bluetoothReady) {
                                                    wizardTitle = "Bağlantı İzinleri Kontrolü"
                                                    wizardSubtitle = "Seçilen alıcıya bağlanmadan önce ağ ve yakındaki cihaz izinlerini doğrulayın."
                                                    wizardAllowSkip = true
                                                    showPermissionWizard = true
                                                }
                                                viewModel.connectWifiReceiver(peer)
                                            },
                                            onOpenPermissionWizard = {
                                                permRefreshNonce++
                                                wizardTitle = "İzinler & Android 13 Kısıtlı Ayar"
                                                wizardSubtitle = "Erişilebilirlik, Bildirim Dinleme ve Bluetooth izin durumları."
                                                wizardAllowSkip = settings.appRole == AppRole.CONTROLLER
                                                showPermissionWizard = true
                                            },
                                            onGoogleAccountLinked = onGoogleLinked,
                                            onSignOut = {
                                                viewModel.prepareForSignOut()
                                                if (authSessionMode == AuthSessionMode.GOOGLE) {
                                                    signOutFromGoogle(credentialManager, coroutineScope) {
                                                        onReturnToAuthScreen()
                                                    }
                                                } else {
                                                    onReturnToAuthScreen()
                                                }
                                            },
                                            onRefreshBluetooth = { viewModel.refreshBluetooth() },
                                            onToggleBluetoothScan = { viewModel.toggleBluetoothScan() },
                                            onCreateDiscoverableIntent = { viewModel.createDiscoverableIntent(120) },
                                            onPairAndConnectBluetooth = { dev ->
                                                val freshAudit = auditAppPermissions(
                                                    context,
                                                    accessibilityEnabled,
                                                    notificationListenerEnabled
                                                )
                                                if (!freshAudit.bluetoothReady) {
                                                    if (authSessionMode == AuthSessionMode.GOOGLE) {
                                                        wizardTitle = "Bluetooth / Nearby İzni Gerekli"
                                                        wizardSubtitle = "Eşleştirme yapmak için Yakındaki Cihazlar / Bluetooth iznini onaylayın."
                                                        wizardAllowSkip = true
                                                        showPermissionWizard = true
                                                    } else if (freshAudit.missingPermissions.isNotEmpty()) {
                                                        wizardPermLauncher.launch(freshAudit.missingPermissions.toTypedArray())
                                                    }
                                                } else {
                                                    viewModel.pairAndConnectBluetooth(dev)
                                                }
                                            },
                                            onConnectDevice = { dev ->
                                                val freshAudit = auditAppPermissions(
                                                    context,
                                                    accessibilityEnabled,
                                                    notificationListenerEnabled
                                                )
                                                if (!freshAudit.bluetoothReady) {
                                                    if (authSessionMode == AuthSessionMode.GOOGLE) {
                                                        wizardTitle = "Bluetooth İzni Gerekli"
                                                        wizardSubtitle = "Cihaza bağlanmak için Bluetooth iznini onaylayın."
                                                        wizardAllowSkip = true
                                                        showPermissionWizard = true
                                                    } else if (freshAudit.missingPermissions.isNotEmpty()) {
                                                        wizardPermLauncher.launch(freshAudit.missingPermissions.toTypedArray())
                                                    }
                                                } else {
                                                    viewModel.connectToDevice(dev)
                                                }
                                            },
                                            onUnpairDevice = { dev -> viewModel.unpairBluetoothDevice(dev) },
                                            bluetoothDeviceTypeLabel = { dev -> viewModel.hidController.deviceTypeLabel(dev) },
                                            onDisconnectDevice = { viewModel.disconnectActiveTransport() },
                                            onToggleFavoriteDevice = { address -> viewModel.toggleFavoriteDevice(address) },
                                            onMoveNavDockItem = { idx, delta -> viewModel.moveNavDockItem(idx, delta) },
                                            onMoveNavDockItemTo = { fromIdx, toIdx -> viewModel.moveNavDockItemTo(fromIdx, toIdx) },
                                            onToggleNavDockHidden = { item -> viewModel.toggleNavDockItemHidden(item) },
                                            onResetDockCustomizations = { viewModel.resetDockCustomizations() },
                                            pendingSubPage = pendingSettingsSubPage,
                                            onConsumePendingSubPage = { viewModel.consumePendingSettingsSubPage() }
                                        )
                                    }
                                }
                            }
                        }

                        if (!isFullscreenInputOverlay && !isPhoneLandscapeFullscreen) {
                            StatusBarGlassScrim(
                                topInsetHeight = statusBarTopPadding,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }

                        if (!isFullscreenInputOverlay && !isTabletLayout) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(104.dp + navBarBottomPadding)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                palette.background.copy(alpha = 0.45f),
                                                palette.background.copy(alpha = 0.88f),
                                                palette.background
                                            )
                                        )
                                    )
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = navBarBottomPadding + 10.dp)
                                    .padding(horizontal = 16.dp)
                                    .widthIn(max = 520.dp)
                            ) {
                                FloatingNavigationCapsule(
                                    tabs = visibleTabs,
                                    selectedTab = currentTab,
                                    onSelectTab = {
                                        showConnectionPopup = false
                                        viewModel.selectTab(it)
                                    },
                                    modifier = Modifier.testTag("floating_nav_bar")
                                )
                            }
                        }
                    }
                }

                // Connection status badge + dismiss scrim + animated expandable details popup
                if (!isFullscreenInputOverlay) {
                    if (showConnectionPopup) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(24f)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { showConnectionPopup = false }
                        )
                    }

                    val dotTopPad = if (isPhoneLandscapeFullscreen) 6.dp else statusBarTopPadding + 4.dp
                    BluetoothStatusDot(
                        connectionStatus = connectionStatus,
                        isReceiverRunning = isReceiverActive,
                        onClick = { showConnectionPopup = !showConnectionPopup },
                        modifier = if (isTabletLayout) {
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 15.dp, top = dotTopPad)
                                .zIndex(28f)
                        } else {
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = statusBarTopPadding, end = 6.dp)
                                .zIndex(28f)
                        }
                    )

                    ExpandableConnectionPopup(
                        expanded = showConnectionPopup,
                        connectionStatus = connectionStatus,
                        isReceiverRunning = isReceiverActive,
                        localWifiIp = localWifiIp,
                        expandFromStart = isTabletLayout,
                        onDismiss = { showConnectionPopup = false },
                        onDisconnect = { viewModel.disconnectActiveTransport() },
                        onOpenBluetoothSettings = {
                            showConnectionPopup = false
                            viewModel.openSettingsSubPage(SettingsSubPage.BLUETOOTH)
                        },
                        onOpenWifiSettings = {
                            showConnectionPopup = false
                            viewModel.openSettingsSubPage(SettingsSubPage.GOOGLE_ACCOUNT)
                        },
                        modifier = if (isTabletLayout) {
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 76.dp, top = dotTopPad + 4.dp)
                                .zIndex(30f)
                        } else {
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = statusBarTopPadding + 52.dp, end = 12.dp)
                                .zIndex(30f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlHubContainer(
    inputSubMode: InputSubMode,
    isFullscreenOverlay: Boolean,
    isTabletLayout: Boolean,
    isPhoneLandscapeFullscreen: Boolean,
    settings: com.example.model.RemoteSettings,
    isWifiConnected: Boolean,
    connectionStatus: ConnectionStatus,
    receiverTelemetry: ReceiverTelemetry,
    cloudDevices: List<PairedDevice>,
    localDeviceId: String,
    onSelectSubMode: (InputSubMode) -> Unit,
    onExitFullscreen: () -> Unit,
    onConnectWifiReceiver: (PairedDevice) -> Unit,
    onOpenPermissionWizard: () -> Unit,
    onMediaAction: (String) -> Unit,
    onSendWifiWidget: (WifiQuickWidgetId) -> Unit,
    onSetVolumePercent: (Int) -> Unit,
    onPlaceWifiWidgetAt: (WifiQuickWidgetId, Int, Int) -> Unit,
    onResizeWifiWidget: (WifiQuickWidgetId) -> Unit,
    onRemoveWifiWidget: (WifiQuickWidgetId) -> Unit,
    onAddWifiWidget: (WifiQuickWidgetId) -> Unit,
    onTrackpadDrag: (dx: Float, dy: Float) -> Unit,
    onTwoFingerVerticalScroll: (deltaY: Float) -> Unit,
    onTwoFingerGestureEnd: () -> Unit,
    onClassicPinchZoomStep: (pinchDelta: Float) -> Unit,
    onClassicPinchZoomEnd: () -> Unit,
    onTwoFingerHorizontalSwipe: (deltaX: Float) -> Unit,
    onTrackpadTap: () -> Unit,
    onTouchpadHoldStart: () -> Unit,
    onTouchpadHoldEnd: () -> Unit,
    onMouseButtonClick: (com.example.model.MouseButton) -> Unit,
    onMouseButtonDown: (com.example.model.MouseButton) -> Unit,
    onMouseButtonUp: (com.example.model.MouseButton) -> Unit,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onRecentAppsClick: () -> Unit,
    onUpdateSettings: ((com.example.model.RemoteSettings) -> com.example.model.RemoteSettings) -> Unit,
    onUpdateGamepadOffsets: ((com.example.model.GamepadLayoutOffsets) -> com.example.model.GamepadLayoutOffsets) -> Unit,
    onResetGamepadOffsets: () -> Unit,
    onSendKey: (hidKeyCode: Int, modifier: Int) -> Unit,
    onSendConsumerKey: (usageCode: Int, name: String) -> Unit,
    onGamepadState: (buttonsMask: Int, hat: Int, lx: Int, ly: Int, rx: Int, ry: Int) -> Unit
) {
    if (isFullscreenOverlay && inputSubMode == InputSubMode.PC_KEYBOARD) {
        LandscapePcKeyboardContent(
            isTurkishLayout = settings.isTurkishKeyboardLayout,
            showNumberRow = settings.showPcNumberRow,
            showNumpad = settings.showPcNumpad,
            splitKeyboard = settings.splitPcKeyboard,
            splitGapWeight = settings.pcSplitGapWeight,
            isReceiverLocked = settings.appRole == AppRole.RECEIVER,
            isWifiConnected = isWifiConnected,
            telemetry = receiverTelemetry,
            settings = settings,
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
            onExitFullscreen = onExitFullscreen,
            onSendKey = onSendKey,
            onMediaAction = onMediaAction,
            onTrackpadDrag = onTrackpadDrag,
            onTwoFingerVerticalScroll = onTwoFingerVerticalScroll,
            onTwoFingerGestureEnd = onTwoFingerGestureEnd,
            onClassicPinchZoomStep = onClassicPinchZoomStep,
            onClassicPinchZoomEnd = onClassicPinchZoomEnd,
            onTwoFingerHorizontalSwipe = onTwoFingerHorizontalSwipe,
            onTrackpadTap = onTrackpadTap,
            onTouchpadHoldStart = onTouchpadHoldStart,
            onTouchpadHoldEnd = onTouchpadHoldEnd,
            onMouseButtonClick = onMouseButtonClick,
            onMouseButtonDown = onMouseButtonDown,
            onMouseButtonUp = onMouseButtonUp,
            onHomeClick = onHomeClick,
            onRecentAppsClick = onRecentAppsClick,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (isFullscreenOverlay && inputSubMode == InputSubMode.GAMEPAD) {
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
            onExitFullscreen = onExitFullscreen,
            onGamepadState = onGamepadState,
            onTrackpadDrag = onTrackpadDrag,
            onTrackpadTap = onTrackpadTap,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isPhoneLandscapeFullscreen) Modifier.padding(top = 6.dp)
                else Modifier
                    .statusBarsPadding()
                    .consumeWindowInsets(WindowInsets.statusBars)
                    .padding(top = 4.dp)
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ControlHubModeBar(
            selectedMode = inputSubMode,
            isTabletLayout = isTabletLayout,
            onSelectMode = onSelectSubMode
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (inputSubMode) {
                InputSubMode.HUB_HOME -> {
                    MediaPanelScreen(
                        settings = settings,
                        connectionStatus = connectionStatus,
                        receiverTelemetry = receiverTelemetry,
                        cloudDevices = cloudDevices,
                        localDeviceId = localDeviceId,
                        isTabletLayout = isTabletLayout,
                        onConnectWifiReceiver = onConnectWifiReceiver,
                        onOpenPermissionWizard = onOpenPermissionWizard,
                        onMediaAction = onMediaAction,
                        onSendWifiWidget = onSendWifiWidget,
                        onSetVolumePercent = onSetVolumePercent,
                        onPlaceWifiWidgetAt = onPlaceWifiWidgetAt,
                        onResizeWifiWidget = onResizeWifiWidget,
                        onRemoveWifiWidget = onRemoveWifiWidget,
                        onAddWifiWidget = onAddWifiWidget,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                InputSubMode.TV_REMOTE -> {
                    TvRemoteScreen(
                        settings = settings,
                        isTabletLayout = isTabletLayout,
                        onSendKey = onSendKey,
                        onSendConsumerKey = onSendConsumerKey,
                        onBackClick = onBackClick,
                        onHomeClick = onHomeClick,
                        onRecentAppsClick = onRecentAppsClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                InputSubMode.PC_KEYBOARD,
                InputSubMode.GAMEPAD -> {
                    // Handled by fullscreen branch above
                }
            }
        }
    }
}

@Composable
private fun ControlHubModeBar(
    selectedMode: InputSubMode,
    isTabletLayout: Boolean,
    onSelectMode: (InputSubMode) -> Unit
) {
    val palette = TabRemoteTheme.palette
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = if (isTabletLayout) 0.dp else 48.dp)
            .height(42.dp)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
            .testTag("control_hub_mode_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InputSubMode.entries.forEach { mode ->
                val isSelected = selectedMode == mode
                val bg by animateColorAsState(
                    targetValue = if (isSelected) palette.controlThumb else Color.Transparent,
                    animationSpec = tween(150),
                    label = "hub_mode_bg_${mode.name}"
                )
                val icon: ImageVector = when (mode) {
                    InputSubMode.HUB_HOME -> Icons.Default.MusicNote
                    InputSubMode.PC_KEYBOARD -> Icons.Default.Keyboard
                    InputSubMode.TV_REMOTE -> Icons.Default.Tv
                    InputSubMode.GAMEPAD -> Icons.Default.Gamepad
                }

                Surface(
                    color = bg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (isSelected) Modifier.border(1.dp, palette.borderElevated, RoundedCornerShape(10.dp))
                            else Modifier
                        )
                        .clickable { onSelectMode(mode) }
                        .testTag("control_submode_${mode.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = mode.label,
                            tint = if (isSelected) palette.textPrimary else palette.textMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = mode.label,
                            color = if (isSelected) palette.textPrimary else palette.textSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImmersiveLandscapeEffect(
    forceLandscapeOrientation: Boolean,
    hideSystemBars: Boolean
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findMainActivity() }

    DisposableEffect(forceLandscapeOrientation, hideSystemBars, activity) {
        val window = activity?.window
        val controller = if (window != null) WindowCompat.getInsetsController(window, view) else null

        if (activity != null) {
            activity.requestedOrientation = if (forceLandscapeOrientation) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }

            if (hideSystemBars) {
                controller?.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller?.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller?.show(WindowInsetsCompat.Type.systemBars())
            }
        }

        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private tailrec fun Context.findMainActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findMainActivity()
    else -> null
}

@Composable
fun FloatingNavigationCapsule(
    tabs: List<NavTab>,
    selectedTab: NavTab,
    onSelectTab: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    Surface(
        color = palette.surface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 18.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(28.dp))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            val tabCount = tabs.size.coerceAtLeast(1)
            val tabWidth = maxWidth / tabCount
            val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "nav_pill_sliding"
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.controlThumb)
                    .border(1.dp, palette.borderElevated, RoundedCornerShape(18.dp))
            )

            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val (iconSelected, iconUnselected) = tabNavIcons(tab)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSelectTab(tab) }
                            .testTag("nav_tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) iconSelected else iconUnselected,
                                contentDescription = tab.title,
                                tint = if (isSelected) palette.textPrimary else palette.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = tab.title,
                                    color = palette.textPrimary,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    softWrap = false,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabRemoteVerticalPillRail(
    tabs: List<NavTab>,
    selectedTab: NavTab,
    topStatusSpacer: androidx.compose.ui.unit.Dp,
    onSelectTab: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    val slotHeight = 56.dp
    val capsuleVerticalPadding = 6.dp
    val capsuleHeight = slotHeight * tabs.size + capsuleVerticalPadding * 2

    Column(
        modifier = modifier
            .width(78.dp)
            .fillMaxHeight()
            .padding(start = 10.dp, end = 6.dp)
            .testTag("tablet_navigation_rail"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(topStatusSpacer))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = palette.surface.copy(alpha = 0.94f),
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 18.dp,
                modifier = Modifier
                    .width(62.dp)
                    .height(capsuleHeight)
                    .border(1.dp, palette.borderSubtle, RoundedCornerShape(28.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = capsuleVerticalPadding)
                ) {
                    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
                    val indicatorOffsetY by animateDpAsState(
                        targetValue = slotHeight * selectedIndex,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "rail_pill_sliding"
                    )

                    Box(
                        modifier = Modifier
                            .offset(y = indicatorOffsetY)
                            .fillMaxWidth()
                            .height(slotHeight)
                            .clip(RoundedCornerShape(18.dp))
                            .background(palette.controlThumb)
                            .border(1.dp, palette.borderElevated, RoundedCornerShape(18.dp))
                    )

                    Column(modifier = Modifier.fillMaxSize()) {
                        tabs.forEach { tab ->
                            val isSelected = selectedTab == tab
                            val (iconSelected, iconUnselected) = tabNavIcons(tab)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(slotHeight)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onSelectTab(tab) }
                                    .testTag("nav_rail_${tab.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) iconSelected else iconUnselected,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) palette.textPrimary else palette.textMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun tabNavIcons(tab: NavTab) = when (tab) {
    NavTab.TOUCHPAD -> Pair(Icons.Filled.TouchApp, Icons.Outlined.TouchApp)
    NavTab.KEYBOARD -> Pair(Icons.Filled.Keyboard, Icons.Outlined.Keyboard)
    NavTab.CONTROL_HUB -> Pair(Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    NavTab.SETTINGS -> Pair(Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun RequestPermissionsEffect(onGranted: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            onGranted()
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissionsToRequest.isNotEmpty()) {
            launcher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
