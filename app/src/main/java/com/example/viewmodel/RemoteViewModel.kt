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
import com.example.bluetooth.BluetoothHidController
import com.example.bluetooth.HidConstants
import com.example.bluetooth.TouchContact
import com.example.data.DeviceSyncRepository
import com.example.data.PairedDevice
import com.example.data.SettingsPrefsStore
import com.example.model.ActiveTransport
import com.example.model.AppRole
import com.example.model.ConnectionStatus
import com.example.model.HidProfileMode
import com.example.model.InputSubMode
import com.example.model.MediaDockItemId
import com.example.model.MouseButton
import com.example.model.NavDockItemId
import com.example.model.RemoteSettings
import com.example.model.VolumeAction
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class RemoteViewModel(
    private val appContext: Context,
    private val deviceSyncRepository: DeviceSyncRepository,
    val currentUserId: String
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

    private val _currentTab = MutableStateFlow(
        if (_settings.value.unifiedControlMode) NavTab.CONTROL else NavTab.TOUCHPAD
    )
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    private val _unifiedAirMouseExpanded = MutableStateFlow(false)
    val unifiedAirMouseExpanded: StateFlow<Boolean> = _unifiedAirMouseExpanded.asStateFlow()

    private val _airMouseActive = MutableStateFlow(false)
    val airMouseActive: StateFlow<Boolean> = _airMouseActive.asStateFlow()

    private val _inputSubMode = MutableStateFlow(InputSubMode.SMART_KEYBOARD)
    val inputSubMode: StateFlow<InputSubMode> = _inputSubMode.asStateFlow()

    private val _preferredTransport = MutableStateFlow(ActiveTransport.BLUETOOTH_HID)

    private val _localWifiIp = MutableStateFlow(WifiNetworkUtils.getLocalWifiIpv4Address(appContext))
    val localWifiIp: StateFlow<String> = _localWifiIp.asStateFlow()

    private val _accessibilityEnabled = MutableStateFlow(
        RemoteAccessibilityService.isServiceConnected.value ||
            RemoteAccessibilityService.isAccessibilitySettingEnabled(appContext)
    )
    val accessibilityEnabled: StateFlow<Boolean> = _accessibilityEnabled.asStateFlow()

    val cloudDevices: StateFlow<List<PairedDevice>> =
        deviceSyncRepository.observeUserDevices(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    private var horizontalPanAccumulator = 0f

    init {
        val initial = _settings.value
        hidController.initialize(
            usePrecisionTouchpad = initial.hidProfileMode == HidProfileMode.PRECISION_TOUCHPAD,
            sdpName = initial.customBluetoothName
        )
        syncSensorSettings()
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
                syncLocalDeviceToCloud()
            }
        }
    }

    fun refreshLocalEnvironment() {
        _localWifiIp.value = WifiNetworkUtils.getLocalWifiIpv4Address(appContext)
        _accessibilityEnabled.value =
            RemoteAccessibilityService.isServiceConnected.value ||
            RemoteAccessibilityService.isAccessibilitySettingEnabled(appContext)
        syncLocalDeviceToCloud()
    }

    private fun syncLocalDeviceToCloud() {
        val current = _settings.value
        val ip = WifiNetworkUtils.getLocalWifiIpv4Address(appContext)
        _localWifiIp.value = ip
        val accessReady = RemoteAccessibilityService.isServiceConnected.value ||
            RemoteAccessibilityService.isAccessibilitySettingEnabled(appContext)
        _accessibilityEnabled.value = accessReady

        viewModelScope.launch {
            try {
                deviceSyncRepository.upsertDevice(
                    userId = currentUserId,
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
            wifiReceiverServer.start(WifiNetworkUtils.DEFAULT_PORT, sessionToken)
        } else {
            wifiReceiverServer.stop()
        }
    }

    fun setUnifiedControlMode(enabled: Boolean) {
        updateSettings { it.copy(unifiedControlMode = enabled) }
        if (enabled && (_currentTab.value == NavTab.TOUCHPAD || _currentTab.value == NavTab.AIR_MOUSE)) {
            _currentTab.value = NavTab.CONTROL
            setAirMouseState(_unifiedAirMouseExpanded.value)
        } else if (!enabled && _currentTab.value == NavTab.CONTROL) {
            _unifiedAirMouseExpanded.value = false
            _currentTab.value = NavTab.TOUCHPAD
            setAirMouseState(false)
        }
        triggerHapticTick()
    }

    fun toggleUnifiedAirMouse() {
        setUnifiedAirMouseExpanded(!_unifiedAirMouseExpanded.value)
    }

    fun setUnifiedAirMouseExpanded(expanded: Boolean) {
        _unifiedAirMouseExpanded.value = expanded
        if (_currentTab.value == NavTab.CONTROL) {
            setAirMouseState(expanded)
        }
    }

    fun selectTab(tab: NavTab) {
        _currentTab.value = tab
        when (tab) {
            NavTab.AIR_MOUSE -> setAirMouseState(true)
            NavTab.CONTROL -> setAirMouseState(_unifiedAirMouseExpanded.value)
            else -> setAirMouseState(false)
        }
    }

    fun selectInputSubMode(mode: InputSubMode) {
        _inputSubMode.value = mode
        triggerHapticTick()
    }

    // ==========================================
    // Bluetooth HID & Wi-Fi Receiver Connections
    // ==========================================

    fun setHidProfileMode(mode: HidProfileMode) {
        if (_settings.value.hidProfileMode == mode) return
        updateSettings { it.copy(hidProfileMode = mode) }
        hidController.reregisterProfile(
            usePrecisionTouchpad = mode == HidProfileMode.PRECISION_TOUCHPAD,
            newSdpName = _settings.value.customBluetoothName
        )
        triggerHapticClick()
    }

    fun refreshBluetooth() {
        val current = _settings.value
        hidController.initialize(
            usePrecisionTouchpad = current.hidProfileMode == HidProfileMode.PRECISION_TOUCHPAD,
            sdpName = current.customBluetoothName
        )
        hidController.refreshBondedDevices()
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

    fun renameBluetoothDevice(newName: String) {
        val clean = newName.trim().take(30)
        if (clean.isBlank()) return
        updateSettings { it.copy(customBluetoothName = clean) }
        hidController.renameBluetoothDevice(clean)
        triggerHapticClick()
    }

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
    // Touchpad Interactions (Wi-Fi Accessibility + BT Precision Touchpad + Classic)
    // ==========================================

    fun sendTouchpadContacts(contacts: List<TouchContact>, clickpadPressed: Boolean = false) {
        if (!isWifiConnected) {
            hidController.sendTouchpadContacts(contacts, clickpadPressed)
        }
    }

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

        scrollAccumulator += deltaY * s * direction * 0.08f
        val ticks = scrollAccumulator.toInt()

        if (ticks != 0) {
            scrollAccumulator -= ticks
            if (isWifiConnected) {
                wifiClient.sendScroll(verticalTicks = -ticks, horizontalTicks = 0)
            } else {
                hidController.sendMouseWheel(-ticks)
            }
            triggerHapticTick()
        }
    }

    fun onTouchpadTwoFingerHorizontalSwipe(deltaX: Float) {
        val s = _settings.value.scrollSensitivity
        horizontalPanAccumulator += deltaX * s
        if (horizontalPanAccumulator < -36f) {
            horizontalPanAccumulator = 0f
            if (isWifiConnected) {
                wifiClient.sendScroll(0, -2)
            } else {
                hidController.sendKeyboardKey(HidConstants.KEY_RIGHT, 0)
            }
            triggerHapticTick()
        } else if (horizontalPanAccumulator > 36f) {
            horizontalPanAccumulator = 0f
            if (isWifiConnected) {
                wifiClient.sendScroll(0, 2)
            } else {
                hidController.sendKeyboardKey(HidConstants.KEY_LEFT, 0)
            }
            triggerHapticTick()
        }
    }

    fun onHorizontalPan(deltaX: Float) {
        val s = _settings.value.scrollSensitivity
        horizontalPanAccumulator += deltaX * s
        if (horizontalPanAccumulator > 32f) {
            horizontalPanAccumulator = 0f
            if (isWifiConnected) {
                wifiClient.sendScroll(0, -2)
            } else {
                hidController.sendKeyboardKey(HidConstants.KEY_RIGHT, 0)
            }
            triggerHapticTick()
        } else if (horizontalPanAccumulator < -32f) {
            horizontalPanAccumulator = 0f
            if (isWifiConnected) {
                wifiClient.sendScroll(0, 2)
            } else {
                hidController.sendKeyboardKey(HidConstants.KEY_LEFT, 0)
            }
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
    // Tablet System Navigation Controls
    // ==========================================

    fun openRecentApps() {
        triggerHapticClick()
        if (isWifiConnected) {
            wifiClient.sendNav("RECENTS")
            return
        }
        if (_settings.value.hidProfileMode == HidProfileMode.PRECISION_TOUCHPAD) {
            viewModelScope.launch {
                val steps = 12
                val startY = 1700
                val endY = 380
                for (i in 0..steps) {
                    val y = startY + ((endY - startY) * i / steps)
                    hidController.sendTouchpadContacts(
                        listOf(
                            TouchContact(0, true, 1000, y),
                            TouchContact(1, true, 1600, y),
                            TouchContact(2, true, 2200, y)
                        )
                    )
                    delay(12)
                }
                delay(45)
                hidController.sendTouchpadContacts(
                    listOf(
                        TouchContact(0, false, 1000, endY),
                        TouchContact(1, false, 1600, endY),
                        TouchContact(2, false, 2200, endY)
                    )
                )
                delay(15)
                hidController.sendTouchpadContacts(emptyList())
            }
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
        hidController.sendGamepadReport(buttonsMask, hat, lx, ly, rx, ry)
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
    // Dock Customization & Settings Persistence
    // ==========================================

    fun moveNavDockItem(index: Int, delta: Int) {
        updateSettings { s ->
            val target = index + delta
            if (index !in s.navDockOrder.indices || target !in s.navDockOrder.indices) return@updateSettings s
            val mutable = s.navDockOrder.toMutableList()
            val moved = mutable.removeAt(index)
            mutable.add(target, moved)
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
                // Keep at least HOME / BACK available
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
                hiddenMediaDockItems = emptySet()
            )
        }
        triggerHapticClick()
    }

    fun updateSettings(transform: (RemoteSettings) -> RemoteSettings) {
        _settings.update { prev ->
            val updated = transform(prev)
            prefsStore.saveSettings(updated)
            syncSensorSettings()
            updated
        }
    }

    fun prepareForSignOut() {
        airMouseSensorManager.stop()
        wifiClient.disconnect()
        wifiReceiverServer.stop()
        hidController.disconnectDevice()
        RemoteConnectionService.stop(appContext)
        viewModelScope.launch {
            try {
                deviceSyncRepository.markOffline(currentUserId, localDeviceId)
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
    CONTROL("Kontrol"),
    TOUCHPAD("Touchpad"),
    AIR_MOUSE("Air Mouse"),
    INPUTS("Girişler"),
    SETTINGS("Ayarlar")
}
