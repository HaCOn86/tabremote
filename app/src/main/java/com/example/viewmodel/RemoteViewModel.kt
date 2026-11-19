package com.example.viewmodel

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accessibility.RemoteAccessibilityService
import com.example.accessibility.RemoteMediaNotificationService
import com.example.bluetooth.BluetoothHidController
import com.example.bluetooth.HidConstants
import com.example.data.DeviceSyncRepository
import com.example.data.PairedDevice
import com.example.data.SettingsPrefsStore
import com.example.model.ActiveTransport
import com.example.model.AppRole
import com.example.model.AuthSessionMode
import com.example.model.ConnectionStatus
import com.example.model.GamepadLayoutOffsets
import com.example.model.InputSubMode
import com.example.model.MediaDockItemId
import com.example.model.MouseButton
import com.example.model.NavDockItemId
import com.example.model.QuickToolId
import com.example.model.QuickToolPlacement
import com.example.model.QuickToolsLayoutEngine
import com.example.model.ReceiverTelemetry
import com.example.model.RemoteSettings
import com.example.model.SettingsSubPage
import com.example.model.SparseCell
import com.example.model.SparseGridEngine
import com.example.model.VolumeAction
import com.example.model.WifiQuickWidgetId
import com.example.model.WifiWidgetPlacement
import com.example.network.WifiNetworkUtils
import com.example.network.WifiReceiverServer
import com.example.network.WifiRemoteClient
import com.example.sensor.AirMouseSensorManager
import com.example.sensor.AirMouseTelemetry
import com.example.service.RemoteConnectionService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class RemoteViewModel(
    private val appContext: Context,
    private val deviceSyncRepository: DeviceSyncRepository,
    val currentUserId: String?,
    val authSessionMode: AuthSessionMode = if (currentUserId != null) AuthSessionMode.GOOGLE else AuthSessionMode.BLUETOOTH_ONLY
) : ViewModel() {

    private val prefsStore = SettingsPrefsStore(appContext)
    val localDeviceId: String = prefsStore.getOrCreateDeviceId()
    val sessionToken: String = prefsStore.getOrCreateSessionToken()

    val hidController = BluetoothHidController(appContext)
    val wifiClient = WifiRemoteClient()
    val wifiReceiverServer = WifiReceiverServer()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val _settings = MutableStateFlow(prefsStore.loadSettings())
    val settings: StateFlow<RemoteSettings> = _settings.asStateFlow()

    private val _currentTab = MutableStateFlow(NavTab.TOUCHPAD)
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    private val _pendingSettingsSubPage = MutableStateFlow<SettingsSubPage?>(null)
    val pendingSettingsSubPage: StateFlow<SettingsSubPage?> = _pendingSettingsSubPage.asStateFlow()

    private val _unifiedAirMouseExpanded = MutableStateFlow(false)
    val unifiedAirMouseExpanded: StateFlow<Boolean> = _unifiedAirMouseExpanded.asStateFlow()

    private val _airMouseActive = MutableStateFlow(false)
    val airMouseActive: StateFlow<Boolean> = _airMouseActive.asStateFlow()

    private val _inputSubMode = MutableStateFlow(InputSubMode.HUB_HOME)
    val inputSubMode: StateFlow<InputSubMode> = _inputSubMode.asStateFlow()

    private val _preferredTransport = MutableStateFlow(ActiveTransport.BLUETOOTH_HID)

    private val _localWifiIp = MutableStateFlow(WifiNetworkUtils.getLocalWifiIpv4Address(appContext))
    val localWifiIp: StateFlow<String> = _localWifiIp.asStateFlow()

    private val _accessibilityEnabled = MutableStateFlow(
        RemoteAccessibilityService.isServiceConnected.value ||
            RemoteAccessibilityService.isAccessibilitySettingEnabled(appContext)
    )
    val accessibilityEnabled: StateFlow<Boolean> = _accessibilityEnabled.asStateFlow()

    private val _notificationListenerEnabled = MutableStateFlow(
        RemoteMediaNotificationService.isNotificationListenerEnabled(appContext)
    )
    val notificationListenerEnabled: StateFlow<Boolean> = _notificationListenerEnabled.asStateFlow()

    val receiverTelemetry: StateFlow<ReceiverTelemetry> = combine(
        wifiClient.connectionStatus,
        wifiClient.receiverTelemetry
    ) { wifiStatus, remote ->
        if (wifiStatus is ConnectionStatus.Connected) remote else ReceiverTelemetry()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReceiverTelemetry())

    val cloudDevices: StateFlow<List<PairedDevice>> =
        (if (currentUserId != null) {
            deviceSyncRepository.observeUserDevices(currentUserId)
                .catch { emit(emptyList()) }
        } else {
            flowOf(emptyList())
        }).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeConnectionStatus: StateFlow<ConnectionStatus> = combine(
        hidController.connectionStatus,
        wifiClient.connectionStatus,
        _preferredTransport
    ) { btStatus, wifiStatus, preferred ->
        when {
            wifiStatus is ConnectionStatus.Connected -> wifiStatus
            wifiStatus is ConnectionStatus.Initializing -> wifiStatus
            btStatus is ConnectionStatus.Connected -> btStatus
            preferred == ActiveTransport.WIFI_RECEIVER && wifiStatus is ConnectionStatus.Error -> wifiStatus
            else -> btStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionStatus.Initializing)

    private val isWifiConnected: Boolean
        get() = wifiClient.connectionStatus.value is ConnectionStatus.Connected

    private val airMouseSensorManager = AirMouseSensorManager(appContext) { dx, dy ->
        if (isWifiConnected) {
            wifiClient.sendMove(dx.toFloat(), dy.toFloat())
        } else {
            hidController.sendMouseDelta(dx, dy, 0, 0)
        }
    }

    val airMouseTelemetry: StateFlow<AirMouseTelemetry> = airMouseSensorManager.telemetry
    val isGyroSupported: Boolean = airMouseSensorManager.isSupported

    private var scrollAccumulator = 0f
    private var twoFingerScrollAccumulator = 0f
    private var horizontalPanAccumulator = 0f
    private var classicPinchAccumulator = 0f
    private var isWifiScrollActive = false

    init {
        val initial = _settings.value
        hidController.initialize()
        syncSensorSettings()
        RemoteAccessibilityService.updateCursorAppearance(
            scale = initial.cursorScale,
            colorArgb = initial.cursorColorArgb,
            borderColorArgb = initial.cursorBorderColorArgb,
            contrastMode = initial.cursorContrastMode
        )
        applyRoleSideEffects(initial.appRole)
        observeAccessibilityAndForeground()
        startCloudPresenceHeartbeat()
    }

    private fun observeAccessibilityAndForeground() {
        viewModelScope.launch {
            RemoteAccessibilityService.isServiceConnected.collect { connected ->
                _accessibilityEnabled.value =
                    connected || RemoteAccessibilityService.isAccessibilitySettingEnabled(appContext)
                syncLocalDeviceToCloud()
            }
        }

        viewModelScope.launch {
            RemoteMediaNotificationService.isListenerConnected.collect { connected ->
                _notificationListenerEnabled.value =
                    connected || RemoteMediaNotificationService.isNotificationListenerEnabled(appContext)
            }
        }

        viewModelScope.launch {
            combine(
                activeConnectionStatus,
                wifiReceiverServer.isRunning,
                wifiReceiverServer.connectedController,
                _settings
            ) { status, receiverRunning, controllerName, currentSettings ->
                Triple(status, receiverRunning to controllerName, currentSettings.appRole)
            }.collect { (status, receiverPair, role) ->
                val (receiverRunning, controllerName) = receiverPair
                when {
                    role == AppRole.RECEIVER && receiverRunning -> {
                        val sub = if (!controllerName.isNullOrBlank()) {
                            "Bağlı kumanda: $controllerName"
                        } else {
                            "Wi-Fi Alıcı Modu arka planda dinliyor"
                        }
                        RemoteConnectionService.startOrUpdate(
                            appContext,
                            title = "TabRemote Alıcı Aktif",
                            text = sub
                        )
                    }
                    status is ConnectionStatus.Connected -> {
                        val modeLabel = if (status.isWifi) "Wi-Fi Alıcı" else "Bluetooth HID"
                        RemoteConnectionService.startOrUpdate(
                            appContext,
                            title = "TabRemote Bağlı (${status.deviceName})",
                            text = "$modeLabel oturumu arka planda korunuyor"
                        )
                    }
                    else -> {
                        RemoteConnectionService.stop(appContext)
                    }
                }
            }
        }
    }

    private fun startCloudPresenceHeartbeat() {
        viewModelScope.launch {
            syncLocalDeviceToCloud()
            while (isActive) {
                delay(20_000L)
                _localWifiIp.value = WifiNetworkUtils.getLocalWifiIpv4Address(appContext)
                _accessibilityEnabled.value =
                    RemoteAccessibilityService.isServiceConnected.value ||
                    RemoteAccessibilityService.isAccessibilitySettingEnabled(appContext)
                _notificationListenerEnabled.value =
                    RemoteMediaNotificationService.isNotificationListenerEnabled(appContext)
                syncLocalDeviceToCloud()
            }
        }
    }

    fun refreshLocalEnvironment() {
        _localWifiIp.value = WifiNetworkUtils.getLocalWifiIpv4Address(appContext)
        _accessibilityEnabled.value =
            RemoteAccessibilityService.isServiceConnected.value ||
            RemoteAccessibilityService.isAccessibilitySettingEnabled(appContext)
        _notificationListenerEnabled.value =
            RemoteMediaNotificationService.isNotificationListenerEnabled(appContext)
        syncLocalDeviceToCloud()
    }

    private fun syncLocalDeviceToCloud() {
        val current = _settings.value
        val ip = WifiNetworkUtils.getLocalWifiIpv4Address(appContext)
        _localWifiIp.value = ip
        val accessReady = RemoteAccessibilityService.isServiceConnected.value ||
            RemoteAccessibilityService.isAccessibilitySettingEnabled(appContext)
        _accessibilityEnabled.value = accessReady

        val uid = currentUserId ?: return
        viewModelScope.launch {
            try {
                deviceSyncRepository.upsertDevice(
                    userId = uid,
                    deviceId = localDeviceId,
                    deviceName = current.localDeviceName.ifBlank { Build.MODEL ?: "TabRemote Cihaz" },
                    role = if (current.appRole == AppRole.RECEIVER) {
                        PairedDevice.ROLE_RECEIVER
                    } else {
                        PairedDevice.ROLE_CONTROLLER
                    },
                    localIp = ip,
                    port = WifiNetworkUtils.DEFAULT_PORT,
                    sessionToken = sessionToken,
                    isOnline = true,
                    accessibilityReady = accessReady
                )
            } catch (_: Exception) {
            }
        }
    }

    fun setAppRole(role: AppRole) {
        if (_settings.value.appRole == role) return
        updateSettings { it.copy(appRole = role) }
        applyRoleSideEffects(role)
        syncLocalDeviceToCloud()
        triggerHapticClick()
    }

    private fun applyRoleSideEffects(role: AppRole) {
        if (role == AppRole.RECEIVER) {
            wifiClient.disconnect()
            wifiReceiverServer.start(WifiNetworkUtils.DEFAULT_PORT, sessionToken, appContext)
        } else {
            wifiReceiverServer.stop()
        }
    }

    fun toggleUnifiedAirMouse() {
        setUnifiedAirMouseExpanded(!_unifiedAirMouseExpanded.value)
    }

    fun setUnifiedAirMouseExpanded(expanded: Boolean) {
        _unifiedAirMouseExpanded.value = expanded
        if (_currentTab.value == NavTab.TOUCHPAD) {
            setAirMouseState(expanded)
        }
    }

    fun selectTab(tab: NavTab) {
        _currentTab.value = tab
        if (tab == NavTab.CONTROL_HUB && isWifiConnected) {
            wifiClient.requestTelemetry()
        }
        val shouldRunAir = tab == NavTab.TOUCHPAD && _unifiedAirMouseExpanded.value
        setAirMouseState(shouldRunAir)
    }

    fun openSettingsSubPage(subPage: SettingsSubPage) {
        _pendingSettingsSubPage.value = subPage
        selectTab(NavTab.SETTINGS)
    }

    fun consumePendingSettingsSubPage() {
        _pendingSettingsSubPage.value = null
    }

    fun selectInputSubMode(mode: InputSubMode) {
        _inputSubMode.value = mode
        if (_currentTab.value == NavTab.CONTROL_HUB && mode == InputSubMode.HUB_HOME && isWifiConnected) {
            wifiClient.requestTelemetry()
        }
        triggerHapticTick()
    }

    // ==========================================
    // Bluetooth HID & Wi-Fi Receiver Connections
    // ==========================================

    fun refreshBluetooth() {
        hidController.initialize()
        hidController.refreshBondedDevices()
    }

    fun unpairBluetoothDevice(device: BluetoothDevice) {
        hidController.unpairDevice(device)
        triggerHapticClick()
    }

    fun toggleBluetoothScan() {
        if (hidController.isScanning.value) {
            hidController.stopDeviceScan()
        } else {
            hidController.startDeviceScan()
        }
        triggerHapticTick()
    }

    fun createDiscoverableIntent(durationSeconds: Int = 120): Intent =
        hidController.createDiscoverableIntent(durationSeconds)

    fun updateLocalCloudDeviceName(newName: String) {
        val clean = newName.trim().take(60)
        if (clean.isBlank()) return
        updateSettings { it.copy(localDeviceName = clean) }
        syncLocalDeviceToCloud()
        triggerHapticClick()
    }

    fun pairAndConnectBluetooth(device: BluetoothDevice) {
        _preferredTransport.value = ActiveTransport.BLUETOOTH_HID
        hidController.pairAndConnect(device)
        triggerHapticTick()
    }

    fun connectToDevice(device: BluetoothDevice) {
        _preferredTransport.value = ActiveTransport.BLUETOOTH_HID
        hidController.connectToDevice(device)
        triggerHapticTick()
    }

    fun connectWifiReceiver(device: PairedDevice) {
        _preferredTransport.value = ActiveTransport.WIFI_RECEIVER
        val controllerName = _settings.value.localDeviceName.ifBlank { Build.MODEL ?: "Telefon" }
        wifiClient.connect(
            hostIp = device.localIp,
            port = device.port.toInt().takeIf { it > 0 } ?: WifiNetworkUtils.DEFAULT_PORT,
            sessionToken = device.sessionToken,
            targetDeviceName = device.deviceName,
            controllerName = controllerName
        )
        triggerHapticClick()
    }

    fun disconnectActiveTransport() {
        if (isWifiConnected || wifiClient.connectionStatus.value is ConnectionStatus.Initializing) {
            wifiClient.disconnect()
        }
        hidController.disconnectDevice()
        triggerHapticTick()
    }

    fun toggleFavoriteDevice(address: String) {
        updateSettings { current ->
            val updated = current.favoriteDeviceAddresses.toMutableSet()
            if (updated.contains(address)) updated.remove(address) else updated.add(address)
            current.copy(favoriteDeviceAddresses = updated)
        }
        triggerHapticTick()
    }

    // ==========================================
    // Touchpad Interactions (Continuous Wi-Fi + BT Classic)
    // ==========================================

    fun onTrackpadDrag(rawDx: Float, rawDy: Float) {
        val s = _settings.value.pointerSensitivity
        val factorX = (rawDx.absoluteValue.coerceAtLeast(1f) / 10f).coerceIn(1.0f, 2.5f)
        val factorY = (rawDy.absoluteValue.coerceAtLeast(1f) / 10f).coerceIn(1.0f, 2.5f)

        val scaledX = rawDx * s * factorX * (if (isWifiConnected) 0.95f else 0.45f)
        val scaledY = rawDy * s * factorY * (if (isWifiConnected) 0.95f else 0.45f)

        if (isWifiConnected) {
            if (scaledX.absoluteValue > 0.05f || scaledY.absoluteValue > 0.05f) {
                wifiClient.sendMove(scaledX, scaledY)
            }
        } else {
            val dx = scaledX.roundToInt()
            val dy = scaledY.roundToInt()
            if (dx != 0 || dy != 0) {
                hidController.sendMouseDelta(dx, dy, 0, 0)
            }
        }
    }

    fun onScrollGutterDrag(deltaY: Float) {
        val s = _settings.value.scrollSensitivity
        val direction = if (_settings.value.invertScroll) -1f else 1f
        if (isWifiConnected) {
            val phase = if (isWifiScrollActive) "MOVE" else "START"
            isWifiScrollActive = true
            wifiClient.sendContinuousScroll(phase, 0f, -deltaY * s * direction * 1.4f)
            return
        }

        scrollAccumulator += deltaY * s * direction * 0.08f
        val ticks = scrollAccumulator.toInt()
        if (ticks != 0) {
            scrollAccumulator -= ticks
            hidController.sendMouseWheel(-ticks)
            triggerHapticTick()
        }
    }

    fun onTwoFingerVerticalScroll(deltaY: Float) {
        val s = _settings.value.scrollSensitivity
        val direction = HidConstants.twoFingerScrollDirectionSign(_settings.value.invertScroll)

        if (isWifiConnected) {
            val phase = if (isWifiScrollActive) "MOVE" else "START"
            isWifiScrollActive = true
            wifiClient.sendContinuousScroll(phase, 0f, deltaY * s * direction * 1.6f)
            return
        }

        twoFingerScrollAccumulator += deltaY * s * direction * 0.08f
        val ticks = twoFingerScrollAccumulator.toInt()
        if (ticks != 0) {
            twoFingerScrollAccumulator -= ticks
            hidController.sendMouseWheel(ticks)
            triggerHapticTick()
        }
    }

    fun onTouchpadTwoFingerHorizontalSwipe(deltaX: Float) {
        val s = _settings.value.scrollSensitivity
        if (isWifiConnected) {
            val phase = if (isWifiScrollActive) "MOVE" else "START"
            isWifiScrollActive = true
            wifiClient.sendContinuousScroll(phase, deltaX * s * 1.5f, 0f)
            return
        }

        horizontalPanAccumulator += deltaX * s
        if (horizontalPanAccumulator < -36f) {
            horizontalPanAccumulator = 0f
            hidController.sendKeyboardKey(HidConstants.KEY_RIGHT, 0)
            triggerHapticTick()
        } else if (horizontalPanAccumulator > 36f) {
            horizontalPanAccumulator = 0f
            hidController.sendKeyboardKey(HidConstants.KEY_LEFT, 0)
            triggerHapticTick()
        }
    }

    fun onTwoFingerGestureEnd() {
        if (isWifiConnected && isWifiScrollActive) {
            isWifiScrollActive = false
            wifiClient.sendContinuousScroll("END", 0f, 0f)
        }
    }

    fun onClassicPinchZoomStep(pinchDelta: Float) {
        if (isWifiConnected) {
            wifiClient.sendContinuousPinch("MOVE", pinchDelta * 1.15f)
            return
        }

        classicPinchAccumulator += pinchDelta
        val threshold = 18f
        if (classicPinchAccumulator.absoluteValue >= threshold) {
            val tick = HidConstants.classicPinchZoomWheelTick(classicPinchAccumulator)
            classicPinchAccumulator = 0f
            if (tick != 0) {
                hidController.sendCtrlScrollZoomStep(tick)
                triggerHapticTick()
            }
        }
    }

    fun onClassicPinchZoomEnd() {
        classicPinchAccumulator = 0f
        if (isWifiConnected) {
            wifiClient.sendContinuousPinch("END", 0f)
        } else {
            hidController.releasePinchZoomModifiers()
        }
    }

    fun onTouchpadHoldStart() {
        if (isWifiConnected) {
            wifiClient.sendHoldTouch("START")
        } else {
            hidController.sendMouseButtonDown(MouseButton.LEFT)
        }
        triggerHapticClick()
    }

    fun onTouchpadHoldEnd() {
        if (isWifiConnected) {
            wifiClient.sendHoldTouch("END")
        } else {
            hidController.sendMouseButtonUp(MouseButton.LEFT)
        }
    }

    fun onHorizontalPan(deltaX: Float) {
        val s = _settings.value.scrollSensitivity
        if (isWifiConnected) {
            val phase = if (isWifiScrollActive) "MOVE" else "START"
            isWifiScrollActive = true
            wifiClient.sendContinuousScroll(phase, -deltaX * s * 1.4f, 0f)
            return
        }

        horizontalPanAccumulator += deltaX * s
        if (horizontalPanAccumulator > 32f) {
            horizontalPanAccumulator = 0f
            hidController.sendKeyboardKey(HidConstants.KEY_RIGHT, 0)
            triggerHapticTick()
        } else if (horizontalPanAccumulator < -32f) {
            horizontalPanAccumulator = 0f
            hidController.sendKeyboardKey(HidConstants.KEY_LEFT, 0)
            triggerHapticTick()
        }
    }

    fun stepHorizontal(direction: Int) {
        if (isWifiConnected) {
            wifiClient.sendScroll(0, if (direction > 0) -3 else 3)
        } else {
            val key = if (direction > 0) HidConstants.KEY_RIGHT else HidConstants.KEY_LEFT
            hidController.sendKeyboardKey(key, 0)
        }
        triggerHapticTick()
    }

    fun scrollStep(ticks: Int) {
        val direction = if (_settings.value.invertScroll) -1 else 1
        if (isWifiConnected) {
            wifiClient.sendScroll(ticks * direction, 0)
        } else {
            hidController.sendMouseWheel(ticks * direction)
        }
        triggerHapticTick()
    }

    fun onTrackpadTap() {
        if (_settings.value.tapToClick) {
            onMouseButtonClick(MouseButton.LEFT)
        }
    }

    fun onMouseButtonClick(button: MouseButton) {
        if (isWifiConnected) {
            wifiClient.sendClick(button.name)
        } else {
            hidController.sendMouseClick(button)
        }
        triggerHapticClick()
    }

    fun onMouseButtonDown(button: MouseButton) {
        if (isWifiConnected) {
            wifiClient.sendButtonDown(button.name)
        } else {
            hidController.sendMouseButtonDown(button)
        }
        triggerHapticClick()
    }

    fun onMouseButtonUp(button: MouseButton) {
        if (isWifiConnected) {
            wifiClient.sendButtonUp(button.name)
        } else {
            hidController.sendMouseButtonUp(button)
        }
    }

    // ==========================================
    // Tablet System Navigation & Wi-Fi Kontrol
    // ==========================================

    fun openRecentApps() {
        triggerHapticClick()
        if (isWifiConnected) {
            wifiClient.sendNav("RECENTS")
        } else {
            hidController.sendConsumerKey(HidConstants.CONSUMER_AL_TASK_MANAGER)
        }
    }

    fun navigateHome() {
        if (isWifiConnected) {
            wifiClient.sendNav("HOME")
        } else {
            hidController.sendConsumerKey(HidConstants.CONSUMER_HOME)
        }
        triggerHapticClick()
    }

    fun navigateBack() {
        if (isWifiConnected) {
            wifiClient.sendNav("BACK")
        } else {
            hidController.sendConsumerKey(HidConstants.CONSUMER_BACK)
        }
        triggerHapticClick()
    }

    private var lastBtVolumePercent = 50

    fun setReceiverVolumePercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        if (isWifiConnected) {
            wifiClient.sendSetVolume(clamped)
            return
        }
        val btConnected = hidController.connectedDevice.value != null
        if (btConnected) {
            val diff = clamped - lastBtVolumePercent
            lastBtVolumePercent = clamped
            val step = if (diff >= 0) HidConstants.CONSUMER_VOLUME_UP else HidConstants.CONSUMER_VOLUME_DOWN
            hidController.sendConsumerKey(step)
        }
    }

    fun sendMediaPlayerAction(action: String) {
        if (isWifiConnected) {
            triggerHapticClick()
            wifiClient.sendMediaAction(action)
            wifiClient.requestTelemetry()
            return
        }
        val btConnected = hidController.connectedDevice.value != null
        if (btConnected) {
            triggerHapticClick()
            val usage = when (action) {
                "PREV" -> HidConstants.CONSUMER_PREV_TRACK
                "NEXT" -> HidConstants.CONSUMER_NEXT_TRACK
                else -> HidConstants.CONSUMER_PLAY_PAUSE
            }
            hidController.sendConsumerKey(usage)
        }
    }

    fun sendWifiQuickWidget(widget: WifiQuickWidgetId) {
        triggerHapticClick()
        when (widget) {
            WifiQuickWidgetId.PLAY_PAUSE -> {
                if (isWifiConnected) wifiClient.sendMediaAction("PLAY_PAUSE")
                else hidController.sendConsumerKey(HidConstants.CONSUMER_PLAY_PAUSE)
            }
            WifiQuickWidgetId.PREV -> {
                if (isWifiConnected) wifiClient.sendMediaAction("PREV")
                else hidController.sendConsumerKey(HidConstants.CONSUMER_PREV_TRACK)
            }
            WifiQuickWidgetId.NEXT -> {
                if (isWifiConnected) wifiClient.sendMediaAction("NEXT")
                else hidController.sendConsumerKey(HidConstants.CONSUMER_NEXT_TRACK)
            }
            WifiQuickWidgetId.MUTE -> sendConsumerKey(HidConstants.CONSUMER_MUTE, "Mute")
            WifiQuickWidgetId.VOL_DOWN -> sendConsumerKey(HidConstants.CONSUMER_VOLUME_DOWN, "Vol Down")
            WifiQuickWidgetId.VOL_UP -> sendConsumerKey(HidConstants.CONSUMER_VOLUME_UP, "Vol Up")
            WifiQuickWidgetId.BACK -> navigateBack()
            WifiQuickWidgetId.HOME -> navigateHome()
            WifiQuickWidgetId.RECENTS -> openRecentApps()
            WifiQuickWidgetId.NOTIFICATIONS -> if (isWifiConnected) wifiClient.sendSystemPanel("NOTIFICATIONS")
            WifiQuickWidgetId.QUICK_SETTINGS -> if (isWifiConnected) wifiClient.sendSystemPanel("QUICK_SETTINGS")
            WifiQuickWidgetId.SCREENSHOT -> if (isWifiConnected) wifiClient.sendSystemPanel("SCREENSHOT")
            WifiQuickWidgetId.LOCK_SCREEN -> if (isWifiConnected) wifiClient.sendSystemPanel("LOCK_SCREEN")
            WifiQuickWidgetId.ZOOM_IN -> {
                if (isWifiConnected) {
                    wifiClient.sendContinuousPinch("START", 48f)
                    viewModelScope.launch {
                        delay(70L)
                        wifiClient.sendContinuousPinch("END", 0f)
                    }
                } else {
                    hidController.sendKeyboardKey(HidConstants.KEY_EQUAL, HidConstants.MOD_LCTRL)
                }
            }
            WifiQuickWidgetId.ZOOM_OUT -> {
                if (isWifiConnected) {
                    wifiClient.sendContinuousPinch("START", -48f)
                    viewModelScope.launch {
                        delay(70L)
                        wifiClient.sendContinuousPinch("END", 0f)
                    }
                } else {
                    hidController.sendKeyboardKey(HidConstants.KEY_MINUS, HidConstants.MOD_LCTRL)
                }
            }
            WifiQuickWidgetId.REFRESH -> sendKey(HidConstants.KEY_R, HidConstants.MOD_LCTRL)
            WifiQuickWidgetId.NEW_TAB -> sendKey(HidConstants.KEY_T, HidConstants.MOD_LCTRL)
            WifiQuickWidgetId.CLOSE_TAB -> sendKey(HidConstants.KEY_W, HidConstants.MOD_LCTRL)
            WifiQuickWidgetId.ADDRESS_BAR -> sendKey(HidConstants.KEY_L, HidConstants.MOD_LCTRL)
            WifiQuickWidgetId.FULLSCREEN -> sendKey(HidConstants.KEY_F11, 0)
        }
        if (isWifiConnected) {
            wifiClient.requestTelemetry()
        }
    }

    // ==========================================
    // Air Mouse
    // ==========================================

    fun setAirMouseState(enabled: Boolean) {
        if (_airMouseActive.value == enabled) return
        _airMouseActive.value = enabled
        if (enabled) {
            syncSensorSettings()
            airMouseSensorManager.start()
            triggerHapticClick()
        } else {
            airMouseSensorManager.stop()
        }
    }

    private fun syncSensorSettings() {
        airMouseSensorManager.sensitivity = _settings.value.airMouseSensitivity
        airMouseSensorManager.deadband = _settings.value.airMouseDeadband
        airMouseSensorManager.smoothing = _settings.value.airMouseSmoothing
    }

    // ==========================================
    // Keyboard, PC Keyboard & Gamepad Inputs
    // ==========================================

    fun sendText(text: String) {
        if (isWifiConnected) {
            wifiClient.sendText(text)
        } else {
            hidController.sendText(text, _settings.value.isTurkishKeyboardLayout)
        }
    }

    fun sendKey(hidKeyCode: Int, modifier: Int = 0) {
        if (isWifiConnected) {
            wifiClient.sendKey(hidKeyCode, modifier)
        } else {
            hidController.sendKeyboardKey(hidKeyCode, modifier)
        }
        triggerHapticTick()
    }

    fun sendConsumerKey(usageCode: Int, @Suppress("UNUSED_PARAMETER") name: String) {
        if (isWifiConnected) {
            wifiClient.sendConsumer(usageCode)
        } else {
            hidController.sendConsumerKey(usageCode)
        }
        triggerHapticTick()
    }

    fun sendGamepadState(
        buttonsMask: Int,
        hat: Int,
        lx: Int,
        ly: Int,
        rx: Int,
        ry: Int
    ) {
        if (isWifiConnected) {
            wifiClient.sendGamepadState(buttonsMask, hat, lx, ly, rx, ry)
        }
    }

    // ==========================================
    // Hardware Volume Key Interception
    // ==========================================

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false

        val isAirMouse = _airMouseActive.value
        val action = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (isAirMouse) _settings.value.airMouseVolumeDownAction
                else _settings.value.touchpadVolumeDownAction
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (isAirMouse) _settings.value.airMouseVolumeUpAction
                else _settings.value.touchpadVolumeUpAction
            }
            else -> return false
        }

        return executeVolumeAction(action)
    }

    private fun executeVolumeAction(action: VolumeAction): Boolean {
        if (action == VolumeAction.NONE) return false

        triggerHapticClick()
        when (action) {
            VolumeAction.MOUSE_LEFT_CLICK -> onMouseButtonClick(MouseButton.LEFT)
            VolumeAction.MOUSE_RIGHT_CLICK -> onMouseButtonClick(MouseButton.RIGHT)
            VolumeAction.SCROLL_DOWN -> scrollStep(-3)
            VolumeAction.SCROLL_UP -> scrollStep(3)
            VolumeAction.MEDIA_VOLUME_DOWN -> sendConsumerKey(HidConstants.CONSUMER_VOLUME_DOWN, "Vol Down")
            VolumeAction.MEDIA_VOLUME_UP -> sendConsumerKey(HidConstants.CONSUMER_VOLUME_UP, "Vol Up")
            VolumeAction.NONE -> Unit
        }
        return true
    }

    // ==========================================
    // Dock Customization & 2D Sparse Grids
    // ==========================================

    fun moveNavDockItem(index: Int, delta: Int) {
        val target = index + delta
        moveNavDockItemTo(index, target)
    }

    fun moveNavDockItemTo(fromIndex: Int, toIndex: Int) {
        updateSettings { s ->
            if (fromIndex == toIndex ||
                fromIndex !in s.navDockOrder.indices ||
                toIndex !in s.navDockOrder.indices
            ) {
                return@updateSettings s
            }
            val mutable = s.navDockOrder.toMutableList()
            val moved = mutable.removeAt(fromIndex)
            mutable.add(toIndex, moved)
            s.copy(navDockOrder = mutable)
        }
        triggerHapticTick()
    }

    fun toggleNavDockItemHidden(item: NavDockItemId) {
        updateSettings { s ->
            val updated = s.hiddenNavDockItems.toMutableSet()
            if (item in updated) {
                updated.remove(item)
            } else {
                val remainingCount = s.navDockOrder.count { it !in updated && it != item }
                if (remainingCount >= 1) updated.add(item)
            }
            s.copy(hiddenNavDockItems = updated)
        }
        triggerHapticTick()
    }

    fun moveMediaDockItem(index: Int, delta: Int) {
        updateSettings { s ->
            val target = index + delta
            if (index !in s.mediaDockOrder.indices || target !in s.mediaDockOrder.indices) return@updateSettings s
            val mutable = s.mediaDockOrder.toMutableList()
            val moved = mutable.removeAt(index)
            mutable.add(target, moved)
            s.copy(mediaDockOrder = mutable)
        }
        triggerHapticTick()
    }

    fun toggleMediaDockItemHidden(item: MediaDockItemId) {
        updateSettings { s ->
            val updated = s.hiddenMediaDockItems.toMutableSet()
            if (item in updated) {
                updated.remove(item)
            } else {
                val remaining = s.mediaDockOrder.count { it !in updated && it != item }
                if (remaining >= 1) updated.add(item)
            }
            s.copy(hiddenMediaDockItems = updated)
        }
        triggerHapticTick()
    }

    fun resetDockCustomizations() {
        val defaults = RemoteSettings()
        updateSettings { s ->
            s.copy(
                navDockOrder = defaults.navDockOrder,
                hiddenNavDockItems = emptySet(),
                mediaDockOrder = defaults.mediaDockOrder,
                hiddenMediaDockItems = emptySet(),
                quickToolsActiveOrder = defaults.quickToolsActiveOrder,
                quickToolsPoolOrder = defaults.quickToolsPoolOrder,
                quickToolsPlacements = defaults.quickToolsPlacements,
                wifiControlPlacements = defaults.wifiControlPlacements
            )
        }
        triggerHapticClick()
    }

    fun placeQuickToolAt(tool: QuickToolId, targetCol: Int, targetRow: Int) {
        updateSettings { s ->
            val existing = s.quickToolsPlacements.firstOrNull { it.tool == tool }
            val spanCol = existing?.colSpan ?: tool.colSpan
            val spanRow = existing?.rowSpan ?: tool.rowSpan
            val updatedCells = SparseGridEngine.placeWithPush(
                cells = QuickToolsLayoutEngine.toSparseCells(s.quickToolsPlacements),
                targetId = tool,
                targetCol = targetCol,
                targetRow = targetRow,
                targetColSpan = spanCol,
                targetRowSpan = spanRow
            )
            val updatedPlacements = QuickToolsLayoutEngine.fromSparseCells(updatedCells)
            val updatedActive = updatedPlacements.map { it.tool }
            val updatedPool = (s.quickToolsPoolOrder.filterNot { it in updatedActive } +
                QuickToolId.entries.filter { it !in updatedActive && it !in s.quickToolsPoolOrder }).distinct()
            s.copy(
                quickToolsPlacements = updatedPlacements,
                quickToolsActiveOrder = updatedActive,
                quickToolsPoolOrder = updatedPool
            )
        }
        triggerHapticTick()
    }

    fun resizeQuickTool(tool: QuickToolId) {
        updateSettings { s ->
            val cur = s.quickToolsPlacements.firstOrNull { it.tool == tool } ?: return@updateSettings s
            val (nextColSpan, nextRowSpan) = tool.nextSpan(cur.colSpan, cur.rowSpan)
            if (nextColSpan == cur.colSpan && nextRowSpan == cur.rowSpan) return@updateSettings s
            val updatedCells = SparseGridEngine.placeWithPush(
                cells = QuickToolsLayoutEngine.toSparseCells(s.quickToolsPlacements),
                targetId = tool,
                targetCol = cur.col,
                targetRow = cur.row,
                targetColSpan = nextColSpan,
                targetRowSpan = nextRowSpan
            )
            val updatedPlacements = QuickToolsLayoutEngine.fromSparseCells(updatedCells)
            s.copy(
                quickToolsPlacements = updatedPlacements,
                quickToolsActiveOrder = updatedPlacements.map { it.tool }
            )
        }
        triggerHapticClick()
    }

    fun moveQuickTool(fromIndex: Int, toIndex: Int) {
        updateSettings { s ->
            if (fromIndex == toIndex) return@updateSettings s
            val placements = s.quickToolsPlacements
            if (fromIndex !in placements.indices || toIndex !in placements.indices) return@updateSettings s
            val moving = placements[fromIndex]
            val dest = placements[toIndex]
            val updatedCells = SparseGridEngine.placeWithPush(
                cells = QuickToolsLayoutEngine.toSparseCells(placements),
                targetId = moving.tool,
                targetCol = dest.col,
                targetRow = dest.row,
                targetColSpan = moving.colSpan,
                targetRowSpan = moving.rowSpan
            )
            val updatedPlacements = QuickToolsLayoutEngine.fromSparseCells(updatedCells)
            s.copy(
                quickToolsPlacements = updatedPlacements,
                quickToolsActiveOrder = updatedPlacements.map { it.tool }
            )
        }
        triggerHapticTick()
    }

    fun removeQuickToolToPool(tool: QuickToolId) {
        updateSettings { s ->
            val updatedPlacements = s.quickToolsPlacements.filterNot { it.tool == tool }
            val updatedActive = updatedPlacements.map { it.tool }
            val updatedPool = (s.quickToolsPoolOrder.filterNot { it == tool }) + tool
            s.copy(
                quickToolsPlacements = updatedPlacements,
                quickToolsActiveOrder = updatedActive,
                quickToolsPoolOrder = updatedPool
            )
        }
        triggerHapticTick()
    }

    fun addQuickToolFromPool(tool: QuickToolId) {
        updateSettings { s ->
            if (s.quickToolsPlacements.any { it.tool == tool }) return@updateSettings s
            val updatedCells = SparseGridEngine.addAtFirstOpenSlot(
                cells = QuickToolsLayoutEngine.toSparseCells(s.quickToolsPlacements),
                id = tool,
                colSpan = tool.colSpan,
                rowSpan = tool.rowSpan
            )
            val updatedPlacements = QuickToolsLayoutEngine.fromSparseCells(updatedCells)
            val updatedActive = updatedPlacements.map { it.tool }
            val updatedPool = s.quickToolsPoolOrder.filterNot { it == tool }
            s.copy(
                quickToolsPlacements = updatedPlacements,
                quickToolsActiveOrder = updatedActive,
                quickToolsPoolOrder = updatedPool
            )
        }
        triggerHapticTick()
    }

    // Wi-Fi Kontrol 2D Sparse Grid mutations
    fun placeWifiWidgetAt(widget: WifiQuickWidgetId, targetCol: Int, targetRow: Int) {
        updateSettings { s ->
            val existing = s.wifiControlPlacements.firstOrNull { it.widget == widget }
            val updated = SparseGridEngine.placeWithPush(
                cells = s.wifiControlPlacements.map { SparseCell(it.widget, it.col, it.row, it.colSpan, it.rowSpan) },
                targetId = widget,
                targetCol = targetCol,
                targetRow = targetRow,
                targetColSpan = existing?.colSpan ?: widget.defaultColSpan,
                targetRowSpan = existing?.rowSpan ?: widget.defaultRowSpan
            ).map { WifiWidgetPlacement(it.id, it.col, it.row, it.colSpan, it.rowSpan) }
            s.copy(wifiControlPlacements = updated)
        }
        triggerHapticTick()
    }

    fun resizeWifiWidget(widget: WifiQuickWidgetId) {
        updateSettings { s ->
            val cur = s.wifiControlPlacements.firstOrNull { it.widget == widget } ?: return@updateSettings s
            val (nextColSpan, nextRowSpan) = widget.nextSpan(cur.colSpan, cur.rowSpan)
            val updated = SparseGridEngine.placeWithPush(
                cells = s.wifiControlPlacements.map { SparseCell(it.widget, it.col, it.row, it.colSpan, it.rowSpan) },
                targetId = widget,
                targetCol = cur.col,
                targetRow = cur.row,
                targetColSpan = nextColSpan,
                targetRowSpan = nextRowSpan
            ).map { WifiWidgetPlacement(it.id, it.col, it.row, it.colSpan, it.rowSpan) }
            s.copy(wifiControlPlacements = updated)
        }
        triggerHapticClick()
    }

    fun removeWifiWidget(widget: WifiQuickWidgetId) {
        updateSettings { s ->
            s.copy(wifiControlPlacements = s.wifiControlPlacements.filterNot { it.widget == widget })
        }
        triggerHapticTick()
    }

    fun addWifiWidget(widget: WifiQuickWidgetId) {
        updateSettings { s ->
            if (s.wifiControlPlacements.any { it.widget == widget }) return@updateSettings s
            val updated = SparseGridEngine.addAtFirstOpenSlot(
                cells = s.wifiControlPlacements.map { SparseCell(it.widget, it.col, it.row, it.colSpan, it.rowSpan) },
                id = widget,
                colSpan = widget.defaultColSpan,
                rowSpan = widget.defaultRowSpan
            ).map { WifiWidgetPlacement(it.id, it.col, it.row, it.colSpan, it.rowSpan) }
            s.copy(wifiControlPlacements = updated)
        }
        triggerHapticTick()
    }

    fun setLiveTypingMode(enabled: Boolean) {
        updateSettings { it.copy(liveTypingMode = enabled) }
        triggerHapticTick()
    }

    fun updateGamepadOffsets(transform: (GamepadLayoutOffsets) -> GamepadLayoutOffsets) {
        updateSettings { s -> s.copy(gamepadOffsets = transform(s.gamepadOffsets)) }
    }

    fun resetGamepadOffsets() {
        updateSettings { s ->
            s.copy(
                gamepadOffsets = GamepadLayoutOffsets(),
                swapGamepadLeftWing = false,
                swapGamepadRightWing = false,
                mirrorGamepadHands = false
            )
        }
        triggerHapticClick()
    }

    fun updateSettings(transform: (RemoteSettings) -> RemoteSettings) {
        _settings.update { prev ->
            val updated = transform(prev)
            prefsStore.saveSettings(updated)
            syncSensorSettings()
            RemoteAccessibilityService.updateCursorAppearance(
                scale = updated.cursorScale,
                colorArgb = updated.cursorColorArgb,
                borderColorArgb = updated.cursorBorderColorArgb,
                contrastMode = updated.cursorContrastMode
            )
            updated
        }
    }

    fun prepareForSignOut() {
        airMouseSensorManager.stop()
        wifiClient.disconnect()
        wifiReceiverServer.stop()
        hidController.disconnectDevice()
        RemoteConnectionService.stop(appContext)
        val uid = currentUserId ?: return
        viewModelScope.launch {
            try {
                deviceSyncRepository.markOffline(uid, localDeviceId)
            } catch (_: Exception) {
            }
        }
    }

    fun triggerHapticClick() {
        if (!_settings.value.hapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25)
            }
        } catch (_: Exception) {}
    }

    fun triggerHapticTick() {
        if (!_settings.value.hapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(10)
            }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        airMouseSensorManager.stop()
        wifiClient.disconnect()
        wifiReceiverServer.stop()
        hidController.cleanup()
        RemoteConnectionService.stop(appContext)
    }
}

enum class NavTab(val title: String) {
    TOUCHPAD("Touchpad"),
    KEYBOARD("Klavye"),
    CONTROL_HUB("Kontrol Hub"),
    SETTINGS("Ayarlar")
}
