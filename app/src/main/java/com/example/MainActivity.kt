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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.Sensors
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.data.SettingsPrefsStore
import com.example.model.AirMouseScrollMode
import com.example.model.AppRole
import com.example.model.AuthSessionMode
import com.example.model.ConnectionStatus
import com.example.model.InputSubMode
import com.example.ui.components.AccessibilityAndPermissionModal
import com.example.ui.components.BluetoothStatusDot
import com.example.ui.components.StatusBarGlassScrim
import com.example.ui.components.auditAppPermissions
import com.example.ui.screens.AirMouseScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.InputsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TouchpadScreen
import com.example.ui.screens.UnifiedControlScreen
import com.example.ui.screens.signOutFromGoogle
import com.example.ui.theme.BackgroundPitch
import com.example.ui.theme.RubyPrimary
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.theme.TabRemoteTheme
import com.example.ui.theme.TextMuted
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
            TabRemoteTheme {
                AppNavigation(
                    onBindActiveViewModel = { activeViewModel = it }
                )
            }
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
            onContinueGuest = {
                sessionMode = AuthSessionMode.GUEST_LOCAL
                prefsStore.setAuthSessionMode(AuthSessionMode.GUEST_LOCAL)
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

@Composable
fun TabRemoteApp(
    viewModel: RemoteViewModel,
    prefsStore: SettingsPrefsStore,
    authSessionMode: AuthSessionMode,
    userEmail: String,
    userDisplayName: String,
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
    val cloudDevices by viewModel.cloudDevices.collectAsStateWithLifecycle()
    val isReceiverServerRunning by viewModel.wifiReceiverServer.isRunning.collectAsStateWithLifecycle()
    val connectedControllerName by viewModel.wifiReceiverServer.connectedController.collectAsStateWithLifecycle()

    val isWifiConnected = (connectionStatus as? ConnectionStatus.Connected)?.isWifi == true
    val isReceiverActive = settings.appRole == AppRole.RECEIVER && isReceiverServerRunning

    var isFullscreenInputOverlay by remember { mutableStateOf(false) }

    // Accessibility & Permissions Audit Modal State
    var permRefreshNonce by remember { mutableIntStateOf(0) }
    val permAudit = remember(accessibilityEnabled, permRefreshNonce) {
        auditAppPermissions(context, accessibilityEnabled)
    }
    var showPermissionWizard by remember {
        mutableStateOf(
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

    LaunchedEffect(currentTab) {
        if (currentTab != NavTab.INPUTS && isFullscreenInputOverlay) {
            isFullscreenInputOverlay = false
            viewModel.selectInputSubMode(InputSubMode.SMART_KEYBOARD)
        }
    }

    ImmersiveLandscapeEffect(enabled = isFullscreenInputOverlay)

    BackHandler(enabled = isFullscreenInputOverlay) {
        isFullscreenInputOverlay = false
        viewModel.selectInputSubMode(InputSubMode.SMART_KEYBOARD)
    }

    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val visibleTabs = remember(settings.unifiedControlMode) {
        if (settings.unifiedControlMode) {
            listOf(NavTab.CONTROL, NavTab.INPUTS, NavTab.SETTINGS)
        } else {
            listOf(NavTab.TOUCHPAD, NavTab.AIR_MOUSE, NavTab.INPUTS, NavTab.SETTINGS)
        }
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
        containerColor = BackgroundPitch,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isFullscreenInputOverlay) Modifier else Modifier.widthIn(max = 680.dp))
                    .then(
                        if (isFullscreenInputOverlay) {
                            Modifier
                        } else {
                            Modifier.padding(
                                bottom = navBarBottomPadding,
                                start = 8.dp,
                                end = 8.dp
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
                        NavTab.CONTROL -> {
                            UnifiedControlScreen(
                                settings = settings,
                                isWifiMode = isWifiConnected,
                                airMouseExpanded = unifiedAirMouseExpanded,
                                airMouseActive = airMouseActive,
                                telemetry = airMouseTelemetry,
                                isGyroSupported = viewModel.isGyroSupported,
                                onToggleAirMouseMode = { viewModel.toggleUnifiedAirMouse() },
                                onTouchpadContacts = { contacts, click ->
                                    viewModel.sendTouchpadContacts(contacts, click)
                                },
                                onTrackpadDrag = { dx, dy -> viewModel.onTrackpadDrag(dx, dy) },
                                onScrollGutterDrag = { dy -> viewModel.onScrollGutterDrag(dy) },
                                onTwoFingerVerticalScroll = { dy -> viewModel.onTwoFingerVerticalScroll(dy) },
                                onClassicPinchZoomStep = { delta -> viewModel.onClassicPinchZoomStep(delta) },
                                onClassicPinchZoomEnd = { viewModel.onClassicPinchZoomEnd() },
                                onScrollStep = { ticks -> viewModel.scrollStep(ticks) },
                                onHorizontalPan = { dx -> viewModel.onHorizontalPan(dx) },
                                onTwoFingerHorizontalSwipe = { dx ->
                                    viewModel.onTouchpadTwoFingerHorizontalSwipe(dx)
                                },
                                onHorizontalStep = { dir -> viewModel.stepHorizontal(dir) },
                                onTrackpadTap = { viewModel.onTrackpadTap() },
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
                                onMoveNavDockItem = { idx, delta -> viewModel.moveNavDockItem(idx, delta) },
                                onToggleNavDockHidden = { item -> viewModel.toggleNavDockItemHidden(item) }
                            )
                        }
                        NavTab.TOUCHPAD -> {
                            TouchpadScreen(
                                settings = settings,
                                isWifiMode = isWifiConnected,
                                onTouchpadContacts = { contacts, click ->
                                    viewModel.sendTouchpadContacts(contacts, click)
                                },
                                onTrackpadDrag = { dx, dy -> viewModel.onTrackpadDrag(dx, dy) },
                                onScrollGutterDrag = { dy -> viewModel.onScrollGutterDrag(dy) },
                                onTwoFingerVerticalScroll = { dy -> viewModel.onTwoFingerVerticalScroll(dy) },
                                onClassicPinchZoomStep = { delta -> viewModel.onClassicPinchZoomStep(delta) },
                                onClassicPinchZoomEnd = { viewModel.onClassicPinchZoomEnd() },
                                onScrollStep = { ticks -> viewModel.scrollStep(ticks) },
                                onToggleScrollMode = {
                                    viewModel.updateSettings { s ->
                                        s.copy(
                                            airMouseScrollMode = if (s.airMouseScrollMode == AirMouseScrollMode.SCROLL_STRIP)
                                                AirMouseScrollMode.STEP_BUTTONS else AirMouseScrollMode.SCROLL_STRIP
                                        )
                                    }
                                },
                                onHorizontalPan = { dx -> viewModel.onHorizontalPan(dx) },
                                onTwoFingerHorizontalSwipe = { dx ->
                                    viewModel.onTouchpadTwoFingerHorizontalSwipe(dx)
                                },
                                onHorizontalStep = { dir -> viewModel.stepHorizontal(dir) },
                                onTrackpadTap = { viewModel.onTrackpadTap() },
                                onMouseButtonClick = { btn -> viewModel.onMouseButtonClick(btn) },
                                onMouseButtonDown = { btn -> viewModel.onMouseButtonDown(btn) },
                                onMouseButtonUp = { btn -> viewModel.onMouseButtonUp(btn) },
                                onBackClick = { viewModel.navigateBack() },
                                onHomeClick = { viewModel.navigateHome() },
                                onRecentAppsClick = { viewModel.openRecentApps() },
                                onMoveNavDockItem = { idx, delta -> viewModel.moveNavDockItem(idx, delta) },
                                onToggleNavDockHidden = { item -> viewModel.toggleNavDockItemHidden(item) }
                            )
                        }
                        NavTab.AIR_MOUSE -> {
                            AirMouseScreen(
                                airMouseActive = airMouseActive,
                                telemetry = airMouseTelemetry,
                                isGyroSupported = viewModel.isGyroSupported,
                                settings = settings,
                                onMouseButtonClick = { btn -> viewModel.onMouseButtonClick(btn) },
                                onMouseButtonDown = { btn -> viewModel.onMouseButtonDown(btn) },
                                onMouseButtonUp = { btn -> viewModel.onMouseButtonUp(btn) },
                                onScrollWheel = { ticks -> viewModel.scrollStep(ticks) },
                                onScrollGutterDrag = { dy -> viewModel.onScrollGutterDrag(dy) },
                                onHorizontalPan = { dx -> viewModel.onHorizontalPan(dx) },
                                onHorizontalStep = { dir -> viewModel.stepHorizontal(dir) },
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
                                onMoveNavDockItem = { idx, delta -> viewModel.moveNavDockItem(idx, delta) },
                                onToggleNavDockHidden = { item -> viewModel.toggleNavDockItemHidden(item) }
                            )
                        }
                        NavTab.INPUTS -> {
                            InputsScreen(
                                selectedSubMode = inputSubMode,
                                settings = settings,
                                isWifiMode = isWifiConnected,
                                isFullscreenOverlay = isFullscreenInputOverlay,
                                onEnterFullscreenMode = { targetMode ->
                                    viewModel.selectInputSubMode(targetMode)
                                    isFullscreenInputOverlay = true
                                },
                                onExitFullscreenToSmartKeyboard = {
                                    isFullscreenInputOverlay = false
                                    viewModel.selectInputSubMode(InputSubMode.SMART_KEYBOARD)
                                },
                                onUpdateSettings = { transform -> viewModel.updateSettings(transform) },
                                onSendText = { text -> viewModel.sendText(text) },
                                onSendKey = { code, mod -> viewModel.sendKey(code, mod) },
                                onSendConsumerKey = { code, name -> viewModel.sendConsumerKey(code, name) },
                                onGamepadState = { btns, hat, lx, ly, rx, ry ->
                                    viewModel.sendGamepadState(btns, hat, lx, ly, rx, ry)
                                },
                                onTouchpadContacts = { contacts, click ->
                                    viewModel.sendTouchpadContacts(contacts, click)
                                },
                                onTrackpadDrag = { dx, dy -> viewModel.onTrackpadDrag(dx, dy) },
                                onTrackpadTap = { viewModel.onTrackpadTap() },
                                onMoveQuickTool = { from, to -> viewModel.moveQuickTool(from, to) },
                                onRemoveQuickToolToPool = { tool -> viewModel.removeQuickToolToPool(tool) },
                                onAddQuickToolFromPool = { tool -> viewModel.addQuickToolFromPool(tool) },
                                onSetLiveTypingMode = { enabled -> viewModel.setLiveTypingMode(enabled) }
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
                                isReceiverRunning = isReceiverServerRunning,
                                connectedControllerName = connectedControllerName,
                                cloudDevices = cloudDevices,
                                connectionStatus = connectionStatus,
                                bondedDevices = bondedDevices,
                                discoveredDevices = discoveredDevices,
                                isScanningBluetooth = isScanningBluetooth,
                                hidDiagnostics = hidDiagnostics,
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
                                    val freshAudit = auditAppPermissions(context, accessibilityEnabled)
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
                                    wizardSubtitle = "Erişilebilirlik Servisi ve sürüm uyumlu Bluetooth/Bildirim izin durumları."
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
                                onRenameBluetoothDevice = { name -> viewModel.renameBluetoothDevice(name) },
                                onPairAndConnectBluetooth = { dev ->
                                    val freshAudit = auditAppPermissions(context, accessibilityEnabled)
                                    if (!freshAudit.bluetoothReady) {
                                        wizardTitle = "Bluetooth / Nearby İzni Gerekli"
                                        wizardSubtitle = "Eşleştirme yapmak için Yakındaki Cihazlar / Bluetooth iznini onaylayın."
                                        wizardAllowSkip = true
                                        showPermissionWizard = true
                                    } else {
                                        viewModel.pairAndConnectBluetooth(dev)
                                    }
                                },
                                onConnectDevice = { dev ->
                                    val freshAudit = auditAppPermissions(context, accessibilityEnabled)
                                    if (!freshAudit.bluetoothReady) {
                                        wizardTitle = "Bluetooth İzni Gerekli"
                                        wizardSubtitle = "Cihaza bağlanmak için Bluetooth iznini onaylayın."
                                        wizardAllowSkip = true
                                        showPermissionWizard = true
                                    } else {
                                        viewModel.connectToDevice(dev)
                                    }
                                },
                                onDisconnectDevice = { viewModel.disconnectActiveTransport() },
                                onToggleFavoriteDevice = { address -> viewModel.toggleFavoriteDevice(address) },
                                onChangeHidProfile = { mode -> viewModel.setHidProfileMode(mode) },
                                onSetUnifiedControlMode = { enabled -> viewModel.setUnifiedControlMode(enabled) },
                                onResetDockCustomizations = { viewModel.resetDockCustomizations() }
                            )
                        }
                    }
                }
            }

            if (!isFullscreenInputOverlay) {
                StatusBarGlassScrim(
                    topInsetHeight = statusBarTopPadding,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                BluetoothStatusDot(
                    connectionStatus = connectionStatus,
                    isReceiverRunning = isReceiverActive,
                    onClick = { viewModel.selectTab(NavTab.SETTINGS) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = statusBarTopPadding, end = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(104.dp + navBarBottomPadding)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    BackgroundPitch.copy(alpha = 0.45f),
                                    BackgroundPitch.copy(alpha = 0.88f),
                                    BackgroundPitch
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = navBarBottomPadding + 10.dp)
                        .padding(horizontal = 16.dp)
                        .widthIn(max = 480.dp)
                ) {
                    FloatingNavigationCapsule(
                        tabs = visibleTabs,
                        selectedTab = currentTab,
                        onSelectTab = { viewModel.selectTab(it) },
                        modifier = Modifier.testTag("floating_nav_bar")
                    )
                }
            }
        }
    }
}

@Composable
private fun ImmersiveLandscapeEffect(enabled: Boolean) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findMainActivity() }

    DisposableEffect(enabled, activity) {
        val window = activity?.window
        val controller = if (window != null) WindowCompat.getInsetsController(window, view) else null

        if (enabled && activity != null) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        } else if (activity != null) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            controller?.show(WindowInsetsCompat.Type.systemBars())
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
    Surface(
        color = SurfaceCharcoal.copy(alpha = 0.88f),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 18.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .border(1.2.dp, RubyPrimary.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
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
                    .padding(horizontal = 3.dp, vertical = 2.dp)
                    .background(RubyPrimary.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
                    .border(1.dp, RubyPrimary.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            )

            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val (iconSelected, iconUnselected) = when (tab) {
                        NavTab.CONTROL, NavTab.TOUCHPAD -> Pair(Icons.Filled.TouchApp, Icons.Outlined.TouchApp)
                        NavTab.AIR_MOUSE -> Pair(Icons.Filled.Sensors, Icons.Outlined.Sensors)
                        NavTab.INPUTS -> Pair(Icons.Filled.Gamepad, Icons.Outlined.Gamepad)
                        NavTab.SETTINGS -> Pair(Icons.Filled.Settings, Icons.Outlined.Settings)
                    }

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
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) iconSelected else iconUnselected,
                                contentDescription = tab.title,
                                tint = if (isSelected) RubyPrimary else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tab.title,
                                    color = RubyPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    softWrap = false,
                                    maxLines = 1
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
        } else {
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
