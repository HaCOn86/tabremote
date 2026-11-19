package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.model.ConnectionStatus
import com.example.model.MouseButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Clean Bluetooth HID Device controller supporting:
 * Report ID 1 (Mouse) + 2 (Keyboard) + 3 (Consumer Control) with SUBCLASS1_COMBO SDP
 * and async unregister -> register state recovery so targets classify the device as a
 * wireless touchpad & keyboard rather than internet tethering.
 */
class BluetoothHidController(private val context: Context) {

    private val tag = "BluetoothHidController"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val executor = Executors.newSingleThreadExecutor()

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var hidApi28: HidApi28Delegate? = null
    @Volatile
    private var isAppRegistered = false
    @Volatile
    private var isProxyRequested = false
    private var isReceiverRegistered = false
    private var pendingBondAddress: String? = null
    private var currentButtonsMask = 0

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Initializing)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()

    private val _bondedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val bondedDevices: StateFlow<List<BluetoothDevice>> = _bondedDevices.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _hidDiagnostics = MutableStateFlow<String?>(null)
    val hidDiagnostics: StateFlow<String?> = _hidDiagnostics.asStateFlow()

    private val btBroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_ON) {
                        _hidDiagnostics.value = null
                        initialize()
                        refreshBondedDevices()
                    } else if (state == BluetoothAdapter.STATE_OFF) {
                        isAppRegistered = false
                        isProxyRequested = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            hidApi28?.clearProxy()
                        }
                        _connectedDevice.value = null
                        _isScanning.value = false
                        _connectionStatus.value = ConnectionStatus.Error("Bluetooth kapalı. Lütfen Bluetooth'u açın.")
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _isScanning.value = true
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val dev = getParcelableDevice(intent) ?: return
                    val address = dev.address ?: return
                    val current = _discoveredDevices.value
                    if (current.none { it.address == address }) {
                        _discoveredDevices.value = current + dev
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val dev = getParcelableDevice(intent) ?: return
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                    if (bondState == BluetoothDevice.BOND_BONDED) {
                        refreshBondedDevices()
                        if (pendingBondAddress != null && dev.address == pendingBondAddress) {
                            pendingBondAddress = null
                            scope.launch {
                                delay(450)
                                connectToDevice(dev)
                            }
                        }
                    } else if (bondState == BluetoothDevice.BOND_NONE && dev.address == pendingBondAddress) {
                        pendingBondAddress = null
                    }
                }
            }
        }
    }

    private fun getParcelableDevice(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    private fun requireApi28Delegate(): HidApi28Delegate? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return hidApi28 ?: HidApi28Delegate().also { hidApi28 = it }
    }

    @SuppressLint("MissingPermission")
    private fun systemHostLabel(): String {
        val adapterName = if (hasBluetoothPermissions()) {
            try {
                bluetoothAdapter?.name?.takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
        val base = (adapterName ?: Build.MODEL ?: "Touchpad").trim()
        val prefixed = if (base.startsWith("Remote", ignoreCase = true)) base else "Remote $base"
        return prefixed.take(32)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private inner class HidApi28Delegate {
        var hidDevice: BluetoothHidDevice? = null
            private set

        @Volatile
        private var pendingReregister = false

        fun clearProxy() {
            pendingReregister = false
            hidDevice = null
        }

        private val serviceListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    Log.d(tag, "Bluetooth HID_DEVICE profile connected")
                    hidDevice = proxy as? BluetoothHidDevice
                    isProxyRequested = false
                    registerClassicComboApp()
                    refreshBondedDevices()
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    Log.d(tag, "Bluetooth HID_DEVICE profile disconnected")
                    hidDevice = null
                    isAppRegistered = false
                    isProxyRequested = false
                    _connectedDevice.value = null
                    _connectionStatus.value = ConnectionStatus.Disconnected
                }
            }
        }

        private val hidCallback = object : BluetoothHidDevice.Callback() {
            override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
                super.onAppStatusChanged(pluggedDevice, registered)
                Log.d(tag, "onAppStatusChanged: registered=$registered, pluggedDevice=$pluggedDevice, pendingReregister=$pendingReregister")
                isAppRegistered = registered
                if (registered) {
                    pendingReregister = false
                    _hidDiagnostics.value = null
                    if (pluggedDevice != null) {
                        _connectedDevice.value = pluggedDevice
                        _connectionStatus.value = ConnectionStatus.Connected(
                            deviceName = getDeviceName(pluggedDevice),
                            address = pluggedDevice.address ?: "",
                            isWifi = false
                        )
                    } else {
                        _connectionStatus.value = ConnectionStatus.ReadyToPair
                    }
                } else {
                    currentButtonsMask = 0
                    _connectedDevice.value = null
                    if (pendingReregister) {
                        pendingReregister = false
                        scope.launch {
                            delay(220L)
                            registerClassicComboApp(isRetryAfterCleanup = true)
                        }
                    } else {
                        _connectionStatus.value = ConnectionStatus.Disconnected
                    }
                }
            }

            override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
                super.onConnectionStateChanged(device, state)
                Log.d(tag, "onConnectionStateChanged: device=$device, state=$state")
                when (state) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        currentButtonsMask = 0
                        _connectedDevice.value = device
                        _hidDiagnostics.value = null
                        _connectionStatus.value = ConnectionStatus.Connected(
                            deviceName = getDeviceName(device),
                            address = device?.address ?: "",
                            isWifi = false
                        )
                    }
                    BluetoothProfile.STATE_CONNECTING -> {
                        _connectionStatus.value = ConnectionStatus.Initializing
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        currentButtonsMask = 0
                        _connectedDevice.value = null
                        _connectionStatus.value =
                            if (isAppRegistered) ConnectionStatus.ReadyToPair else ConnectionStatus.Disconnected
                        // Reset HID host-server registration cleanly after disconnect to avoid stale sockets
                        reregisterWithCleanState()
                    }
                    BluetoothProfile.STATE_DISCONNECTING -> Unit
                }
            }

            @SuppressLint("MissingPermission")
            override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
                super.onGetReport(device, type, id, bufferSize)
                val hid = hidDevice ?: return
                if (device == null || !hasBluetoothPermissions()) return
                try {
                    hid.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
                } catch (_: Exception) {
                }
            }
        }

        fun requestProxy(adapter: BluetoothAdapter): Boolean {
            return adapter.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE)
        }

        @SuppressLint("MissingPermission")
        fun registerClassicComboApp(isRetryAfterCleanup: Boolean = false) {
            val hid = hidDevice ?: return
            if (isAppRegistered) return
            if (!hasBluetoothPermissions()) return

            val hostName = systemHostLabel()
            val sdp = BluetoothHidDeviceAppSdpSettings(
                hostName,
                "Wireless Touchpad & Keyboard",
                hostName,
                BluetoothHidDevice.SUBCLASS1_COMBO,
                HidConstants.CLASSIC_COMBO_REPORT_DESCRIPTOR
            )

            try {
                // Null InQos/OutQos avoids vendor SDP/L2CAP rejection on Samsung & Android hosts.
                val ok = hid.registerApp(sdp, null, null, executor, hidCallback)
                Log.d(tag, "registerClassicComboApp(name=$hostName, retry=$isRetryAfterCleanup) -> $ok")
                if (!ok) {
                    if (!isRetryAfterCleanup) {
                        _hidDiagnostics.value = "Önceki HID oturumu temizleniyor ve yeniden kaydediliyor..."
                        pendingReregister = true
                        try {
                            hid.unregisterApp()
                        } catch (_: Exception) {
                        }
                        scope.launch {
                            delay(500L)
                            if (pendingReregister && !isAppRegistered) {
                                pendingReregister = false
                                registerClassicComboApp(isRetryAfterCleanup = true)
                            }
                        }
                    } else {
                        _hidDiagnostics.value = "HID servisi kaydedilemedi. Bluetooth'u kapatıp açın veya hedef cihazdaki eski eşleşmeyi kaldırıp yeniden eşleştirin."
                        _connectionStatus.value = ConnectionStatus.Error("HID servisi kaydedilemedi. Bluetooth'u kapatıp açın.")
                    }
                } else {
                    _hidDiagnostics.value = null
                }
            } catch (e: Exception) {
                Log.e(tag, "Exception registering HID App", e)
                _connectionStatus.value = ConnectionStatus.Error("HID kayıt hatası: ${e.message}")
            }
        }

        @SuppressLint("MissingPermission")
        fun reregisterWithCleanState() {
            val hid = hidDevice ?: return
            if (!hasBluetoothPermissions()) return
            if (pendingReregister) return
            pendingReregister = true
            try {
                if (isAppRegistered) {
                    hid.unregisterApp()
                } else {
                    pendingReregister = false
                    registerClassicComboApp(isRetryAfterCleanup = true)
                }
            } catch (_: Exception) {
                pendingReregister = false
            }
        }

        @SuppressLint("MissingPermission")
        fun connect(device: BluetoothDevice): Boolean {
            return hidDevice?.connect(device) == true
        }

        @SuppressLint("MissingPermission")
        fun disconnect(device: BluetoothDevice) {
            hidDevice?.disconnect(device)
        }

        @SuppressLint("MissingPermission")
        fun sendReport(device: BluetoothDevice, reportId: Int, data: ByteArray) {
            hidDevice?.sendReport(device, reportId, data)
        }

        @SuppressLint("MissingPermission")
        fun close() {
            val hid = hidDevice ?: return
            if (isAppRegistered && hasBluetoothPermissions()) {
                try {
                    hid.unregisterApp()
                } catch (_: Exception) {
                }
                isAppRegistered = false
            }
            try {
                bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hid)
            } catch (_: Exception) {
            }
            hidDevice = null
        }
    }

    private fun ensureReceiverRegistered() {
        if (isReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // System Bluetooth broadcasts MUST use RECEIVER_EXPORTED on Android 13+
                context.registerReceiver(btBroadcastReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(btBroadcastReceiver, filter)
            }
            isReceiverRegistered = true
        } catch (e: Exception) {
            Log.w(tag, "Failed to register BT receiver", e)
        }
    }

    fun initialize() {
        ensureReceiverRegistered()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            _connectionStatus.value = ConnectionStatus.Error("Bluetooth HID profili Android 9+ gerektirir. Wi-Fi Alıcı modunu kullanabilirsiniz.")
            return
        }

        if (!hasBluetoothPermissions()) {
            _connectionStatus.value = ConnectionStatus.Error("Bluetooth bağlantısı için Yakındaki Cihazlar izni gerekli.")
            return
        }

        val adapter = bluetoothAdapter
        if (adapter == null) {
            _connectionStatus.value = ConnectionStatus.Error("Bu cihazda Bluetooth donanımı bulunmuyor.")
            return
        }

        if (!adapter.isEnabled) {
            _connectionStatus.value = ConnectionStatus.Error("Bluetooth kapalı. Lütfen Bluetooth'u açın.")
            return
        }

        val delegate = requireApi28Delegate() ?: return
        val currentProxy = delegate.hidDevice
        if (currentProxy != null) {
            if (!isAppRegistered) {
                delegate.registerClassicComboApp()
            } else if (_connectedDevice.value == null) {
                _connectionStatus.value = ConnectionStatus.ReadyToPair
            }
            refreshBondedDevices()
            return
        }

        if (!isProxyRequested) {
            try {
                _connectionStatus.value = ConnectionStatus.Initializing
                val supported = delegate.requestProxy(adapter)
                isProxyRequested = supported
                if (!supported) {
                    _connectionStatus.value = ConnectionStatus.Error("Bluetooth HID profili bu ROM/cihazda desteklenmiyor. Wi-Fi Alıcı Modu'nu kullanabilirsiniz.")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error requesting HID_DEVICE profile proxy", e)
                _connectionStatus.value = ConnectionStatus.Error("Bluetooth HID başlatılamadı: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startDeviceScan() {
        ensureReceiverRegistered()
        if (!hasScanPermission()) {
            _hidDiagnostics.value = "Cihaz taraması için Yakındaki Cihazlar ve Konum izni gerekli."
            return
        }
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) {
            _hidDiagnostics.value = "Tarama için önce Bluetooth'u açın."
            return
        }
        try {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            _discoveredDevices.value = emptyList()
            _hidDiagnostics.value = null
            val started = adapter.startDiscovery()
            _isScanning.value = started
            if (!started) {
                _hidDiagnostics.value = "Bluetooth taraması başlatılamadı. Konum servisinin açık olduğundan emin olun."
            } else {
                scope.launch {
                    delay(12_500L)
                    if (_isScanning.value) {
                        stopDeviceScan()
                        if (_discoveredDevices.value.isEmpty()) {
                            _hidDiagnostics.value = "Yeni cihaz görünmedi. Hedef tabletin Bluetooth ayar ekranını açık tutup 'Görünür Ol' veya tekrar taramayı deneyin."
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "startDiscovery failed", e)
            _isScanning.value = false
            _hidDiagnostics.value = "Tarama hatası: ${e.localizedMessage ?: "Bilinmeyen hata"}"
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDeviceScan() {
        if (!hasScanPermission()) return
        try {
            bluetoothAdapter?.cancelDiscovery()
            _isScanning.value = false
        } catch (_: Exception) {
        }
    }

    fun createDiscoverableIntent(durationSeconds: Int = 120): Intent {
        return Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, durationSeconds.coerceIn(30, 300))
        }
    }

    @SuppressLint("MissingPermission")
    fun pairAndConnect(device: BluetoothDevice) {
        if (!hasBluetoothPermissions()) return
        stopDeviceScan()
        if (device.bondState == BluetoothDevice.BOND_BONDED) {
            connectToDevice(device)
        } else {
            pendingBondAddress = device.address
            _connectionStatus.value = ConnectionStatus.Initializing
            try {
                device.createBond()
            } catch (e: Exception) {
                pendingBondAddress = null
                _connectionStatus.value = ConnectionStatus.Error("Eşleştirme başlatılamadı: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        if (!hasBluetoothPermissions()) return
        stopDeviceScan()
        val delegate = hidApi28
        if (delegate?.hidDevice == null || !isAppRegistered) {
            initialize()
            _connectionStatus.value = ConnectionStatus.Error("HID servisi hazırlanıyor, lütfen birkaç saniye sonra tekrar dokunun.")
            return
        }
        try {
            _connectionStatus.value = ConnectionStatus.Initializing
            val initiated = delegate.connect(device)
            if (!initiated) {
                _connectionStatus.value = ConnectionStatus.Error(
                    "${getDeviceName(device)} bağlantıyı kabul etmedi. Tabletin Bluetooth ayarlarında 'Giriş Aygıtı' iznini açın veya eşleşmeyi kaldırıp yeniden eşleştirin."
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to connect to device", e)
            _connectionStatus.value = ConnectionStatus.Error("Bağlantı hatası: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun unpairDevice(device: BluetoothDevice): Boolean {
        if (!hasBluetoothPermissions()) return false
        try {
            if (_connectedDevice.value?.address == device.address) {
                disconnectDevice()
            }
            val method = device.javaClass.getMethod("removeBond")
            val result = method.invoke(device) as? Boolean ?: true
            scope.launch {
                delay(350)
                refreshBondedDevices()
                _discoveredDevices.value = _discoveredDevices.value.filterNot { it.address == device.address }
            }
            return result
        } catch (e: Exception) {
            Log.w(tag, "removeBond failed", e)
            _hidDiagnostics.value = "Eşleşme kaldırılamadı; sistem Bluetooth ayarlarından unutmayı deneyin."
            return false
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectDevice() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val dev = _connectedDevice.value ?: return
        if (!hasBluetoothPermissions()) return
        try {
            hidApi28?.disconnect(dev)
        } catch (e: Exception) {
            Log.e(tag, "Failed to disconnect device", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshBondedDevices() {
        if (!hasBluetoothPermissions()) {
            _bondedDevices.value = emptyList()
            return
        }
        val bonded = try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        _bondedDevices.value = bonded
    }

    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADMIN) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasScanPermission(): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED && (hasFine || hasCoarse || true)
        } else {
            (hasFine || hasCoarse) && hasBluetoothPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    fun getDeviceName(device: BluetoothDevice?): String {
        if (device == null) return "Bilinmeyen Cihaz"
        return if (hasBluetoothPermissions()) {
            device.name ?: device.address ?: "Tablet / Ekran"
        } else {
            device.address ?: "Tablet / Ekran"
        }
    }

    @SuppressLint("MissingPermission")
    fun deviceTypeLabel(device: BluetoothDevice): String {
        if (!hasBluetoothPermissions()) return "Bluetooth Cihazı"
        val major = try {
            device.bluetoothClass?.majorDeviceClass
        } catch (_: Exception) {
            null
        }
        return when (major) {
            android.bluetooth.BluetoothClass.Device.Major.COMPUTER -> "Bilgisayar / Tablet"
            android.bluetooth.BluetoothClass.Device.Major.PHONE -> "Telefon / Tablet"
            android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO -> "Akıllı TV / Medya"
            android.bluetooth.BluetoothClass.Device.Major.PERIPHERAL -> "Çevre Birimi"
            else -> "Bluetooth Ana Cihaz"
        }
    }

    // ==========================================
    // 5-Button Mouse + Wheel + AC Pan (Report ID 1)
    // ==========================================

    @SuppressLint("MissingPermission")
    fun sendMouseDelta(dx: Int, dy: Int, wheel: Int = 0, hPan: Int = 0) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val dev = _connectedDevice.value ?: return
        if (!hasBluetoothPermissions()) return

        val reportData = byteArrayOf(
            currentButtonsMask.toByte(),
            dx.coerceIn(-127, 127).toByte(),
            dy.coerceIn(-127, 127).toByte(),
            wheel.coerceIn(-127, 127).toByte(),
            hPan.coerceIn(-127, 127).toByte()
        )
        try {
            hidApi28?.sendReport(dev, HidConstants.REPORT_ID_MOUSE.toInt(), reportData)
        } catch (e: Exception) {
            Log.e(tag, "Error sending mouse delta report", e)
        }
    }

    fun sendMouseButtonDown(button: MouseButton) {
        currentButtonsMask = currentButtonsMask or button.mask
        sendMouseDelta(0, 0, 0, 0)
    }

    fun sendMouseButtonUp(button: MouseButton) {
        currentButtonsMask = currentButtonsMask and button.mask.inv()
        sendMouseDelta(0, 0, 0, 0)
    }

    fun sendMouseClick(button: MouseButton) {
        scope.launch {
            sendMouseButtonDown(button)
            delay(35)
            sendMouseButtonUp(button)
        }
    }

    fun sendMouseWheel(ticks: Int) {
        sendMouseDelta(0, 0, wheel = ticks, hPan = 0)
    }

    fun sendMouseHorizontalPan(ticks: Int) {
        sendMouseDelta(0, 0, wheel = 0, hPan = ticks)
    }

    /**
     * Pinch-to-zoom using Classic Combo HID: holds Left Ctrl while sending mouse wheel ticks
     * so browsers/editors/viewports treat touchpad pinch gestures as Ctrl + Scroll zoom.
     */
    fun sendCtrlScrollZoomStep(wheelTicks: Int) {
        if (wheelTicks == 0) return
        sendKeyboardState(HidConstants.MOD_LCTRL, emptyList())
        sendMouseDelta(0, 0, wheel = wheelTicks.coerceIn(-4, 4), hPan = 0)
    }

    fun releasePinchZoomModifiers() {
        sendKeyboardState(0, emptyList())
    }

    // ==========================================
    // Hardware Keyboard (Report ID 2)
    // ==========================================

    @SuppressLint("MissingPermission")
    fun sendKeyboardKey(hidKeyCode: Int, modifier: Int = 0) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val dev = _connectedDevice.value ?: return
        if (!hasBluetoothPermissions()) return
        val keyDownReport = byteArrayOf(
            modifier.toByte(), 0.toByte(), hidKeyCode.toByte(),
            0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte()
        )
        val keyUpReport = ByteArray(8)
        try {
            hidApi28?.sendReport(dev, HidConstants.REPORT_ID_KEYBOARD.toInt(), keyDownReport)
            scope.launch {
                delay(28)
                hidApi28?.sendReport(dev, HidConstants.REPORT_ID_KEYBOARD.toInt(), keyUpReport)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error sending keyboard report", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun sendKeyboardState(modifierMask: Int, keyCodes: List<Int>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val dev = _connectedDevice.value ?: return
        if (!hasBluetoothPermissions()) return
        val report = ByteArray(8)
        report[0] = modifierMask.toByte()
        report[1] = 0
        for (i in 0 until minOf(6, keyCodes.size)) {
            report[2 + i] = keyCodes[i].toByte()
        }
        try {
            hidApi28?.sendReport(dev, HidConstants.REPORT_ID_KEYBOARD.toInt(), report)
        } catch (e: Exception) {
            Log.e(tag, "Error sending keyboard state report", e)
        }
    }

    fun sendText(text: String, isTurkishLayout: Boolean = true) {
        scope.launch {
            for (char in text) {
                val mapping = HidConstants.charToHid(char, isTurkishLayout)
                if (mapping != null) {
                    val (modifier, keycode) = mapping
                    sendKeyboardKey(keycode, modifier)
                    delay(24)
                }
            }
        }
    }

    // ==========================================
    // Consumer Control (Report ID 3)
    // ==========================================

    @SuppressLint("MissingPermission")
    fun sendConsumerKey(usageCode: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val dev = _connectedDevice.value ?: return
        if (!hasBluetoothPermissions()) return
        val keyDownReport = byteArrayOf(
            (usageCode and 0xFF).toByte(),
            ((usageCode shr 8) and 0xFF).toByte()
        )
        val keyUpReport = byteArrayOf(0.toByte(), 0.toByte())
        try {
            hidApi28?.sendReport(dev, HidConstants.REPORT_ID_CONSUMER.toInt(), keyDownReport)
            scope.launch {
                delay(40)
                hidApi28?.sendReport(dev, HidConstants.REPORT_ID_CONSUMER.toInt(), keyUpReport)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error sending consumer report", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun cleanup() {
        try {
            if (isReceiverRegistered) {
                context.unregisterReceiver(btBroadcastReceiver)
                isReceiverRegistered = false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                hidApi28?.close()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error during cleanup", e)
        }
    }
}
