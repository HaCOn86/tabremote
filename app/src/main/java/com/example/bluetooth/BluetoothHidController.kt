package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
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

data class TouchContact(
    val contactId: Int,
    val tipDown: Boolean,
    val x: Int,
    val y: Int
)

class BluetoothHidController(private val context: Context) {

    private val tag = "BluetoothHidController"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val executor = Executors.newSingleThreadExecutor()

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var hidDevice: BluetoothHidDevice? = null
    private var isAppRegistered = false
    private var isProxyRequested = false
    private var isReceiverRegistered = false
    private var activeUsePrecisionTouchpad = true
    private var customSdpName: String = "TabRemote"
    private var pendingBondAddress: String? = null

    @Volatile
    private var tpInputMode: Byte = 0x03

    @Volatile
    private var tpSelectiveReporting: Byte = 0x03

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

    private var currentButtonsMask = 0

    private val btBroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_ON) {
                        initialize(activeUsePrecisionTouchpad, customSdpName)
                        refreshBondedDevices()
                    } else if (state == BluetoothAdapter.STATE_OFF) {
                        isAppRegistered = false
                        isProxyRequested = false
                        hidDevice = null
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

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.d(tag, "Bluetooth HID Device profile connected")
                hidDevice = proxy as? BluetoothHidDevice
                registerHidApp(activeUsePrecisionTouchpad)
                refreshBondedDevices()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.d(tag, "Bluetooth HID Device profile disconnected")
                hidDevice = null
                isAppRegistered = false
                isProxyRequested = false
                _connectionStatus.value = ConnectionStatus.Disconnected
                _connectedDevice.value = null
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            super.onAppStatusChanged(pluggedDevice, registered)
            Log.d(tag, "onAppStatusChanged: registered=$registered, device=$pluggedDevice")
            isAppRegistered = registered
            if (registered) {
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
                _connectionStatus.value = ConnectionStatus.Disconnected
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            super.onConnectionStateChanged(device, state)
            Log.d(tag, "onConnectionStateChanged: device=$device, state=$state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
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
                    _connectedDevice.value = null
                    _connectionStatus.value = if (isAppRegistered) ConnectionStatus.ReadyToPair else ConnectionStatus.Disconnected
                }
                BluetoothProfile.STATE_DISCONNECTING -> Unit
            }
        }

        @SuppressLint("MissingPermission")
        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            super.onGetReport(device, type, id, bufferSize)
            val hid = hidDevice ?: return
            if (device == null || !hasBluetoothPermissions()) return

            if (type == BluetoothHidDevice.REPORT_TYPE_FEATURE) {
                val payload: ByteArray? = when (id) {
                    HidConstants.REPORT_ID_TP_CAPABILITIES ->
                        byteArrayOf(HidConstants.TP_MAX_CONTACTS.toByte())
                    HidConstants.REPORT_ID_TP_INPUT_MODE ->
                        byteArrayOf(tpInputMode)
                    HidConstants.REPORT_ID_TP_SELECTIVE ->
                        byteArrayOf(tpSelectiveReporting)
                    HidConstants.REPORT_ID_TP_CERT ->
                        HidConstants.PTP_CERT_BLOB
                    else -> null
                }
                if (payload != null) {
                    try {
                        hid.replyReport(device, type, id, payload)
                    } catch (e: Exception) {
                        Log.e(tag, "Error replying feature report $id", e)
                    }
                } else {
                    hid.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
                }
            } else {
                hid.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            super.onSetReport(device, type, id, data)
            if (type == BluetoothHidDevice.REPORT_TYPE_FEATURE && data != null && data.isNotEmpty()) {
                when (id) {
                    HidConstants.REPORT_ID_TP_INPUT_MODE -> tpInputMode = data[0]
                    HidConstants.REPORT_ID_TP_SELECTIVE -> tpSelectiveReporting = data[0]
                }
            }
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
                context.registerReceiver(btBroadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(btBroadcastReceiver, filter)
            }
            isReceiverRegistered = true
        } catch (e: Exception) {
            Log.w(tag, "Failed to register BT receiver", e)
        }
    }

    fun initialize(usePrecisionTouchpad: Boolean = true, sdpName: String = customSdpName) {
        activeUsePrecisionTouchpad = usePrecisionTouchpad
        if (sdpName.isNotBlank()) customSdpName = sdpName.trim().take(30)
        ensureReceiverRegistered()

        if (!hasBluetoothPermissions()) {
            _connectionStatus.value = ConnectionStatus.Error("Bluetooth bağlantısı için Yakındaki Cihazlar izni gerekli.")
            return
        }

        val adapter = bluetoothAdapter
        if (adapter == null) {
            _connectionStatus.value = ConnectionStatus.Error("Bu cihazda Bluetooth donanımı yok.")
            return
        }

        if (!adapter.isEnabled) {
            _connectionStatus.value = ConnectionStatus.Error("Bluetooth kapalı. Lütfen Bluetooth'u açın.")
            return
        }

        val currentProxy = hidDevice
        if (currentProxy != null) {
            if (!isAppRegistered) {
                registerHidApp(usePrecisionTouchpad)
            }
            refreshBondedDevices()
            return
        }

        if (!isProxyRequested) {
            try {
                val supported = adapter.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE)
                isProxyRequested = supported
                if (!supported) {
                    _connectionStatus.value = ConnectionStatus.Error("Bluetooth HID profili bu ROM/cihazda desteklenmiyor. Tablet Alıcı Modu'nu kullanabilirsiniz.")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error requesting HID_DEVICE profile proxy", e)
                _connectionStatus.value = ConnectionStatus.Error("Bluetooth HID başlatılamadı: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun reregisterProfile(usePrecisionTouchpad: Boolean, newSdpName: String? = null) {
        activeUsePrecisionTouchpad = usePrecisionTouchpad
        if (!newSdpName.isNullOrBlank()) {
            customSdpName = newSdpName.trim().take(30)
        }
        val hid = hidDevice ?: run {
            initialize(usePrecisionTouchpad, customSdpName)
            return
        }
        if (!hasBluetoothPermissions()) return
        val previousDevice = _connectedDevice.value

        scope.launch {
            try {
                if (isAppRegistered) {
                    hid.unregisterApp()
                    isAppRegistered = false
                    delay(250)
                }
                registerHidApp(usePrecisionTouchpad)
                if (previousDevice != null) {
                    delay(400)
                    connectToDevice(previousDevice)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error re-registering HID profile", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun renameBluetoothDevice(newName: String): Boolean {
        val clean = newName.trim().take(30)
        if (clean.isBlank()) return false
        customSdpName = clean
        val adapterRenamed = try {
            if (hasBluetoothPermissions() && bluetoothAdapter != null) {
                bluetoothAdapter.setName(clean)
            } else false
        } catch (_: Exception) {
            false
        }
        reregisterProfile(activeUsePrecisionTouchpad, clean)
        return adapterRenamed
    }

    @SuppressLint("MissingPermission")
    private fun registerHidApp(usePrecisionTouchpad: Boolean) {
        val hid = hidDevice ?: return
        if (isAppRegistered) return
        if (!hasBluetoothPermissions()) return

        val descriptor = if (usePrecisionTouchpad) {
            HidConstants.PRECISION_COMBO_REPORT_DESCRIPTOR
        } else {
            HidConstants.CLASSIC_COMBO_REPORT_DESCRIPTOR
        }

        try {
            val sdp = BluetoothHidDeviceAppSdpSettings(
                customSdpName,
                "TabRemote Trackpad, Keyboard & Gamepad",
                "TabRemote",
                BluetoothHidDevice.SUBCLASS1_COMBO,
                descriptor
            )

            val qos = BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                800,
                9,
                0,
                11250,
                BluetoothHidDeviceAppQosSettings.MAX
            )

            val ok = hid.registerApp(sdp, null, qos, executor, hidCallback)
            Log.d(tag, "registerApp(precision=$usePrecisionTouchpad, name=$customSdpName) -> $ok")
            if (!ok && usePrecisionTouchpad) {
                // Fallback automatically to compact Classic descriptor if OEM stack rejects PTP descriptor size
                _hidDiagnostics.value = "Precision HID tanımlayıcısı reddedildi; Klasik HID profiline geçiliyor."
                activeUsePrecisionTouchpad = false
                val fallbackSdp = BluetoothHidDeviceAppSdpSettings(
                    customSdpName,
                    "TabRemote Classic HID Mouse & Keyboard",
                    "TabRemote",
                    BluetoothHidDevice.SUBCLASS1_COMBO,
                    HidConstants.CLASSIC_COMBO_REPORT_DESCRIPTOR
                )
                val fallbackOk = hid.registerApp(fallbackSdp, null, qos, executor, hidCallback)
                if (!fallbackOk) {
                    _connectionStatus.value = ConnectionStatus.Error("HID servisi kaydedilemedi. Telefonun Bluetooth'unu kapatıp açın.")
                }
            } else if (!ok) {
                _connectionStatus.value = ConnectionStatus.Error("HID servisi bu cihazda kaydedilemedi.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception registering HID App", e)
            _connectionStatus.value = ConnectionStatus.Error("Kayıt hatası: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun startDeviceScan() {
        if (!hasScanPermission()) {
            _hidDiagnostics.value = "Cihaz taraması için BLUETOOTH_SCAN izni gerekli."
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
            val started = adapter.startDiscovery()
            _isScanning.value = started
            if (!started) {
                _hidDiagnostics.value = "Bluetooth taraması başlatılamadı."
            }
        } catch (e: Exception) {
            Log.e(tag, "startDiscovery failed", e)
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDeviceScan() {
        if (!hasScanPermission()) return
        try {
            bluetoothAdapter?.cancelDiscovery()
            _isScanning.value = false
        } catch (_: Exception) {}
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
        if (!hasBluetoothPermissions()) return
        stopDeviceScan()
        val hid = hidDevice
        if (hid == null || !isAppRegistered) {
            initialize(activeUsePrecisionTouchpad, customSdpName)
            _connectionStatus.value = ConnectionStatus.Error("HID hazır değil, tekrar deneniyor...")
            return
        }
        try {
            _connectionStatus.value = ConnectionStatus.Initializing
            val initiated = hid.connect(device)
            if (!initiated) {
                _connectionStatus.value = ConnectionStatus.Error(
                    "${getDeviceName(device)} bağlantıyı kabul etmedi. Önce tabletten telefonun adını seçerek bağlamayı deneyin."
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to connect to device", e)
            _connectionStatus.value = ConnectionStatus.Error("Bağlantı hatası: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectDevice() {
        val dev = _connectedDevice.value ?: return
        if (!hasBluetoothPermissions()) return
        try {
            hidDevice?.disconnect(dev)
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
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                hasBluetoothPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    fun getDeviceName(device: BluetoothDevice?): String {
        if (device == null) return "Unknown"
        return if (hasBluetoothPermissions()) {
            device.name ?: device.address ?: "Tablet / Ekran"
        } else {
            device.address ?: "Tablet / Ekran"
        }
    }

    // ==========================================
    // 5-Button Mouse + Wheel + AC Pan
    // ==========================================

    @SuppressLint("MissingPermission")
    fun sendMouseDelta(dx: Int, dy: Int, wheel: Int = 0, hPan: Int = 0) {
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
            hidDevice?.sendReport(dev, HidConstants.REPORT_ID_MOUSE.toInt(), reportData)
        } catch (e: Exception) {
            Log.e(tag, "Error sending mouse delta report", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun sendMouseButtonDown(button: MouseButton) {
        currentButtonsMask = currentButtonsMask or button.mask
        sendMouseDelta(0, 0, 0, 0)
    }

    @SuppressLint("MissingPermission")
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

    @SuppressLint("MissingPermission")
    fun sendTouchpadContacts(contacts: List<TouchContact>, clickpadPressed: Boolean = false) {
        val dev = _connectedDevice.value ?: return
        if (!hasBluetoothPermissions()) return

        val payload = ByteArray(24)
        val clampedCount = contacts.size.coerceAtMost(HidConstants.TP_MAX_CONTACTS)

        for (i in 0 until HidConstants.TP_MAX_CONTACTS) {
            val base = i * 5
            if (i < clampedCount) {
                val c = contacts[i]
                val flags = (if (c.tipDown) 0x01 else 0x00) or 0x02 or ((c.contactId and 0x0F) shl 2)
                val x = c.x.coerceIn(0, HidConstants.TP_LOGICAL_MAX_X)
                val y = c.y.coerceIn(0, HidConstants.TP_LOGICAL_MAX_Y)
                payload[base] = flags.toByte()
                payload[base + 1] = (x and 0xFF).toByte()
                payload[base + 2] = ((x shr 8) and 0xFF).toByte()
                payload[base + 3] = (y and 0xFF).toByte()
                payload[base + 4] = ((y shr 8) and 0xFF).toByte()
            }
        }

        val scanTime = ((System.nanoTime() / 100_000L) and 0xFFFFL).toInt()
        payload[20] = (scanTime and 0xFF).toByte()
        payload[21] = ((scanTime shr 8) and 0xFF).toByte()
        payload[22] = clampedCount.toByte()
        payload[23] = if (clickpadPressed) 1.toByte() else 0.toByte()

        try {
            hidDevice?.sendReport(dev, HidConstants.REPORT_ID_TOUCHPAD.toInt(), payload)
        } catch (e: Exception) {
            Log.e(tag, "Error sending Precision Touchpad report", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun sendKeyboardKey(hidKeyCode: Int, modifier: Int = 0) {
        val dev = _connectedDevice.value ?: return
        if (!hasBluetoothPermissions()) return
        val keyDownReport = byteArrayOf(
            modifier.toByte(), 0.toByte(), hidKeyCode.toByte(),
            0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte()
        )
        val keyUpReport = ByteArray(8)
        try {
            hidDevice?.sendReport(dev, HidConstants.REPORT_ID_KEYBOARD.toInt(), keyDownReport)
            scope.launch {
                delay(28)
                hidDevice?.sendReport(dev, HidConstants.REPORT_ID_KEYBOARD.toInt(), keyUpReport)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error sending keyboard report", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun sendKeyboardState(modifierMask: Int, keyCodes: List<Int>) {
        val dev = _connectedDevice.value ?: return
        if (!hasBluetoothPermissions()) return
        val report = ByteArray(8)
        report[0] = modifierMask.toByte()
        report[1] = 0
        for (i in 0 until minOf(6, keyCodes.size)) {
            report[2 + i] = keyCodes[i].toByte()
        }
        try {
            hidDevice?.sendReport(dev, HidConstants.REPORT_ID_KEYBOARD.toInt(), report)
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

    @SuppressLint("MissingPermission")
    fun sendConsumerKey(usageCode: Int) {
        val dev = _connectedDevice.value ?: return
        if (!hasBluetoothPermissions()) return
        val keyDownReport = byteArrayOf(
            (usageCode and 0xFF).toByte(),
            ((usageCode shr 8) and 0xFF).toByte()
        )
        val keyUpReport = byteArrayOf(0.toByte(), 0.toByte())
        try {
            hidDevice?.sendReport(dev, HidConstants.REPORT_ID_CONSUMER.toInt(), keyDownReport)
            scope.launch {
                delay(40)
                hidDevice?.sendReport(dev, HidConstants.REPORT_ID_CONSUMER.toInt(), keyUpReport)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error sending consumer report", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun sendGamepadReport(
        buttonsMask: Int,
        hat: Int,
        lx: Int,
        ly: Int,
        rx: Int,
        ry: Int
    ) {
        val dev = _connectedDevice.value ?: return
        if (!hasBluetoothPermissions()) return
        val reportData = byteArrayOf(
            (buttonsMask and 0xFF).toByte(),
            ((buttonsMask shr 8) and 0xFF).toByte(),
            (hat and 0x0F).toByte(),
            lx.coerceIn(-127, 127).toByte(),
            ly.coerceIn(-127, 127).toByte(),
            rx.coerceIn(-127, 127).toByte(),
            ry.coerceIn(-127, 127).toByte()
        )
        try {
            hidDevice?.sendReport(dev, HidConstants.REPORT_ID_GAMEPAD.toInt(), reportData)
        } catch (e: Exception) {
            Log.e(tag, "Error sending gamepad report", e)
        }
    }

    fun cleanup() {
        try {
            if (isReceiverRegistered) {
                context.unregisterReceiver(btBroadcastReceiver)
                isReceiverRegistered = false
            }
            val hid = hidDevice
            if (hid != null && isAppRegistered && hasBluetoothPermissions()) {
                hid.unregisterApp()
            }
            bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hid)
        } catch (e: Exception) {
            Log.e(tag, "Error during cleanup", e)
        }
    }
}
