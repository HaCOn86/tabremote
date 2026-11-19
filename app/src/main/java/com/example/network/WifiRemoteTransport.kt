package com.example.network

import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.example.accessibility.RemoteAccessibilityService
import com.example.accessibility.RemoteMediaNotificationService
import com.example.model.ConnectionStatus
import com.example.model.ReceiverTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import kotlin.math.roundToInt

object WifiNetworkUtils {
    const val DEFAULT_PORT = 38472

    fun getLocalWifiIpv4Address(context: Context): String {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val caps = if (activeNetwork != null) cm.getNetworkCapabilities(activeNetwork) else null
            val hasWifiOrEth = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true

            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            val candidates = mutableListOf<Pair<Int, String>>()

            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback) continue
                val name = iface.name.lowercase()
                val priority = when {
                    name.startsWith("wlan") -> 0
                    name.startsWith("ap") || name.startsWith("swlan") -> 1
                    name.startsWith("eth") -> 2
                    hasWifiOrEth -> 3
                    else -> 5
                }
                for (addr in iface.inetAddresses.toList()) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress ?: continue
                        if (!ip.startsWith("169.254.") && !ip.startsWith("127.")) {
                            candidates.add(priority to ip)
                        }
                    }
                }
            }
            return candidates.minByOrNull { it.first }?.second.orEmpty()
        } catch (_: Exception) {
            return ""
        }
    }
}

class WifiReceiverServer {
    private val tag = "WifiReceiverServer"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private var appContextRef: Context? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _connectedController = MutableStateFlow<String?>(null)
    val connectedController: StateFlow<String?> = _connectedController.asStateFlow()

    @Volatile
    private var activeToken: String = ""

    fun start(port: Int, expectedToken: String, context: Context? = null) {
        if (context != null) appContextRef = context.applicationContext
        activeToken = expectedToken
        if (_isRunning.value && acceptJob?.isActive == true) return

        stop()
        acceptJob = scope.launch {
            try {
                val ss = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
                serverSocket = ss
                _isRunning.value = true
                Log.d(tag, "Wi-Fi Receiver listening on port $port")

                while (isActive && !ss.isClosed) {
                    val client = try {
                        ss.accept()
                    } catch (_: Exception) {
                        break
                    }
                    launch { handleClient(client) }
                }
            } catch (e: Exception) {
                Log.e(tag, "Receiver server error", e)
            } finally {
                _isRunning.value = false
                _connectedController.value = null
            }
        }
    }

    fun updateToken(token: String) {
        activeToken = token
    }

    private fun handleClient(socket: Socket) {
        var authedName: String? = null
        var telemetryJob: Job? = null
        val writeLock = Any()

        fun writeFrame(writer: BufferedWriter, obj: JSONObject) {
            synchronized(writeLock) {
                writer.write(obj.toString())
                writer.newLine()
                writer.flush()
            }
        }

        try {
            socket.tcpNoDelay = true
            socket.soTimeout = 15000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

            while (true) {
                val line = reader.readLine() ?: break
                if (line.isBlank()) continue
                val json = JSONObject(line)
                val type = json.optString("type")

                if (authedName == null) {
                    if (type == "AUTH") {
                        val token = json.optString("token")
                        val ctrlName = json.optString("controllerName", "Telefon")
                        if (token == activeToken) {
                            authedName = ctrlName
                            _connectedController.value = ctrlName
                            socket.soTimeout = 0
                            writeFrame(writer, JSONObject().put("type", "AUTH_OK"))
                            writeFrame(writer, buildTelemetryFrame())

                            telemetryJob = scope.launch {
                                var lastPayload = ""
                                while (isActive && !socket.isClosed) {
                                    delay(900L)
                                    val frame = buildTelemetryFrame()
                                    val encoded = frame.toString()
                                    if (encoded != lastPayload) {
                                        lastPayload = encoded
                                        try {
                                            writeFrame(writer, frame)
                                        } catch (_: Exception) {
                                            break
                                        }
                                    }
                                }
                            }
                        } else {
                            writeFrame(
                                writer,
                                JSONObject().put("type", "AUTH_ERR").put("reason", "Geçersiz oturum anahtarı")
                            )
                            break
                        }
                    } else {
                        break
                    }
                } else {
                    dispatchCommand(type, json) { frame ->
                        try {
                            writeFrame(writer, frame)
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(tag, "Client disconnected: ${e.message}")
        } finally {
            telemetryJob?.cancel()
            try {
                socket.close()
            } catch (_: Exception) {}
            if (_connectedController.value == authedName) {
                _connectedController.value = null
            }
        }
    }

    private fun buildTelemetryFrame(): JSONObject {
        val svc = RemoteAccessibilityService.getInstance()
        val telemetry = if (svc != null) {
            svc.buildCurrentTelemetry()
        } else {
            buildFallbackTelemetry()
        }
        return JSONObject()
            .put("type", "TELEMETRY")
            .put("activeAppPackage", telemetry.activeAppPackage)
            .put("activeAppName", telemetry.activeAppName)
            .put("mediaTitle", telemetry.mediaTitle)
            .put("mediaArtist", telemetry.mediaArtist)
            .put("mediaAppName", telemetry.mediaAppName)
            .put("isMediaPlaying", telemetry.isMediaPlaying)
            .put("mediaPositionMs", telemetry.mediaPositionMs)
            .put("mediaDurationMs", telemetry.mediaDurationMs)
            .put("volumePercent", telemetry.volumePercent)
            .put("isMuted", telemetry.isMuted)
            .put("batteryPercent", telemetry.batteryPercent)
            .put("isBatteryCharging", telemetry.isBatteryCharging)
            .put("notificationListenerEnabled", telemetry.notificationListenerEnabled)
            .put("updatedAtMs", telemetry.updatedAtMs)
    }

    private fun buildFallbackTelemetry(): ReceiverTelemetry {
        val ctx = appContextRef
        val am = ctx?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val maxVol = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
        val curVol = am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7
        val volPct = ((curVol.toFloat() / maxVol) * 100f).roundToInt().coerceIn(0, 100)
        val isMuted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am?.isStreamMute(AudioManager.STREAM_MUSIC) == true || curVol == 0
        } else {
            curVol == 0
        }
        val (batPct, isCharging) = if (ctx != null) {
            RemoteAccessibilityService.readBatterySnapshot(ctx)
        } else {
            -1 to false
        }
        val media = RemoteMediaNotificationService.snapshot.value
        val listenerEnabled = if (ctx != null) {
            RemoteMediaNotificationService.isNotificationListenerEnabled(ctx)
        } else {
            RemoteMediaNotificationService.isListenerConnected.value
        }
        return ReceiverTelemetry(
            mediaTitle = media.title,
            mediaArtist = media.artist,
            mediaAppName = media.appName,
            isMediaPlaying = media.isPlaying,
            mediaPositionMs = media.resolveCurrentPositionMs(),
            mediaDurationMs = media.durationMs,
            volumePercent = volPct,
            isMuted = isMuted,
            batteryPercent = batPct,
            isBatteryCharging = isCharging,
            notificationListenerEnabled = listenerEnabled,
            updatedAtMs = System.currentTimeMillis()
        )
    }

    private fun dispatchCommand(
        type: String,
        json: JSONObject,
        sendBack: (JSONObject) -> Unit
    ) {
        val svc = RemoteAccessibilityService.getInstance()
        when (type) {
            "MOVE" -> svc?.moveCursor(
                json.optDouble("dx", 0.0).toFloat(),
                json.optDouble("dy", 0.0).toFloat()
            )
            "CLICK" -> svc?.clickAtCursor(json.optString("btn", "LEFT"))
            "BTN_DOWN" -> svc?.startMouseHold(json.optString("btn", "LEFT"))
            "BTN_UP" -> svc?.endMouseHold(json.optString("btn", "LEFT"))
            "HOLD_TOUCH" -> svc?.handleHoldTouch(json.optString("phase", "START"))
            "SCROLL" -> svc?.scrollAtCursor(json.optInt("v", 0), json.optInt("h", 0))
            "SCROLL_CONT" -> svc?.handleContinuousScroll(
                phase = json.optString("phase", "MOVE"),
                dx = json.optDouble("dx", 0.0).toFloat(),
                dy = json.optDouble("dy", 0.0).toFloat()
            )
            "PINCH_CONT" -> svc?.handleContinuousPinch(
                phase = json.optString("phase", "MOVE"),
                delta = json.optDouble("delta", 0.0).toFloat()
            )
            "NAV", "SYSTEM_PANEL" -> svc?.performNavAction(json.optString("action", "HOME"))
            "TEXT", "TEXT_COMMIT" -> svc?.handleText(json.optString("text", ""))
            "KEY" -> svc?.handleHidKey(json.optInt("code", 0), json.optInt("mod", 0))
            "CONSUMER" -> svc?.handleConsumerUsage(json.optInt("usage", 0))
            "GAMEPAD" -> svc?.handleGamepadInput(
                buttonsMask = json.optInt("buttons", 0),
                hat = json.optInt("hat", 8),
                lx = json.optInt("lx", 0),
                ly = json.optInt("ly", 0),
                rx = json.optInt("rx", 0),
                ry = json.optInt("ry", 0)
            )
            "SET_VOLUME" -> {
                val pct = json.optInt("percent", 50)
                if (svc != null) {
                    svc.setVolumePercent(pct)
                } else {
                    val am = appContextRef?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    val max = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
                    val target = ((pct.coerceIn(0, 100) / 100f) * max).roundToInt().coerceIn(0, max)
                    am?.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
                }
                sendBack(buildTelemetryFrame())
            }
            "MEDIA_ACTION" -> {
                val action = json.optString("action", "PLAY_PAUSE")
                appContextRef?.let { RemoteMediaNotificationService.dispatchMediaControl(it, action) }
                sendBack(buildTelemetryFrame())
            }
            "GET_TELEMETRY" -> sendBack(buildTelemetryFrame())
            "PING" -> sendBack(JSONObject().put("type", "PONG"))
        }
    }

    fun stop() {
        acceptJob?.cancel()
        acceptJob = null
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        _isRunning.value = false
        _connectedController.value = null
    }
}

class WifiRemoteClient {
    private val tag = "WifiRemoteClient"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _receiverTelemetry = MutableStateFlow(ReceiverTelemetry())
    val receiverTelemetry: StateFlow<ReceiverTelemetry> = _receiverTelemetry.asStateFlow()

    private var socket: Socket? = null
    private var readerJob: Job? = null
    private var writerJob: Job? = null
    private var outgoing = Channel<String>(capacity = 512)

    fun connect(
        hostIp: String,
        port: Int,
        sessionToken: String,
        targetDeviceName: String,
        controllerName: String
    ) {
        if (hostIp.isBlank()) {
            _connectionStatus.value = ConnectionStatus.Error("Alıcı tabletin Wi-Fi IP adresi bulunamadı.")
            return
        }
        disconnect()
        _connectionStatus.value = ConnectionStatus.Initializing

        val sendQueue = Channel<String>(capacity = 512)
        outgoing = sendQueue

        readerJob = scope.launch {
            try {
                val sock = Socket()
                sock.connect(InetSocketAddress(hostIp, port), 4000)
                sock.tcpNoDelay = true
                socket = sock

                val writer = BufferedWriter(OutputStreamWriter(sock.getOutputStream()))
                val reader = BufferedReader(InputStreamReader(sock.getInputStream()))

                val authFrame = JSONObject()
                    .put("type", "AUTH")
                    .put("token", sessionToken)
                    .put("controllerName", controllerName)
                    .toString()
                writer.write(authFrame)
                writer.newLine()
                writer.flush()

                val respLine = reader.readLine()
                val respJson = if (!respLine.isNullOrBlank()) JSONObject(respLine) else null
                if (respJson?.optString("type") != "AUTH_OK") {
                    val reason = respJson?.optString("reason") ?: "Tablet bağlantıyı reddetti"
                    _connectionStatus.value = ConnectionStatus.Error(reason)
                    sock.close()
                    return@launch
                }

                _connectionStatus.value = ConnectionStatus.Connected(
                    deviceName = targetDeviceName,
                    address = "$hostIp:$port",
                    isWifi = true
                )

                writerJob = scope.launch {
                    try {
                        for (msg in sendQueue) {
                            writer.write(msg)
                            writer.newLine()
                            writer.flush()
                        }
                    } catch (e: Exception) {
                        Log.d(tag, "Writer closed: ${e.message}")
                    }
                }

                while (isActive && !sock.isClosed) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) continue
                    try {
                        val incoming = JSONObject(line)
                        if (incoming.optString("type") == "TELEMETRY") {
                            _receiverTelemetry.value = ReceiverTelemetry(
                                activeAppPackage = incoming.optString("activeAppPackage", ""),
                                activeAppName = incoming.optString("activeAppName", ""),
                                mediaTitle = incoming.optString("mediaTitle", ""),
                                mediaArtist = incoming.optString("mediaArtist", ""),
                                mediaAppName = incoming.optString("mediaAppName", ""),
                                isMediaPlaying = incoming.optBoolean("isMediaPlaying", false),
                                mediaPositionMs = incoming.optLong("mediaPositionMs", 0L).coerceAtLeast(0L),
                                mediaDurationMs = incoming.optLong("mediaDurationMs", 0L).coerceAtLeast(0L),
                                volumePercent = incoming.optInt("volumePercent", 50).coerceIn(0, 100),
                                isMuted = incoming.optBoolean("isMuted", false),
                                batteryPercent = incoming.optInt("batteryPercent", -1),
                                isBatteryCharging = incoming.optBoolean("isBatteryCharging", false),
                                notificationListenerEnabled = incoming.optBoolean("notificationListenerEnabled", false),
                                updatedAtMs = incoming.optLong("updatedAtMs", System.currentTimeMillis())
                            )
                        }
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.w(tag, "Wi-Fi connect failed", e)
                _connectionStatus.value = ConnectionStatus.Error("Wi-Fi bağlantısı başarısız: ${e.localizedMessage ?: "Zaman aşımı"}")
            } finally {
                cleanupSocketOnly()
                _receiverTelemetry.value = ReceiverTelemetry()
                if (_connectionStatus.value is ConnectionStatus.Connected) {
                    _connectionStatus.value = ConnectionStatus.Disconnected
                }
            }
        }
    }

    fun sendMove(dx: Float, dy: Float) {
        enqueue(JSONObject().put("type", "MOVE").put("dx", dx.toDouble()).put("dy", dy.toDouble()))
    }

    fun sendClick(button: String) {
        enqueue(JSONObject().put("type", "CLICK").put("btn", button))
    }

    fun sendButtonDown(button: String) {
        enqueue(JSONObject().put("type", "BTN_DOWN").put("btn", button))
    }

    fun sendButtonUp(button: String) {
        enqueue(JSONObject().put("type", "BTN_UP").put("btn", button))
    }

    fun sendHoldTouch(phase: String) {
        enqueue(JSONObject().put("type", "HOLD_TOUCH").put("phase", phase))
    }

    fun sendScroll(verticalTicks: Int, horizontalTicks: Int) {
        enqueue(JSONObject().put("type", "SCROLL").put("v", verticalTicks).put("h", horizontalTicks))
    }

    fun sendContinuousScroll(phase: String, dx: Float, dy: Float) {
        enqueue(
            JSONObject()
                .put("type", "SCROLL_CONT")
                .put("phase", phase)
                .put("dx", dx.toDouble())
                .put("dy", dy.toDouble())
        )
    }

    fun sendContinuousPinch(phase: String, delta: Float) {
        enqueue(
            JSONObject()
                .put("type", "PINCH_CONT")
                .put("phase", phase)
                .put("delta", delta.toDouble())
        )
    }

    fun sendNav(action: String) {
        enqueue(JSONObject().put("type", "NAV").put("action", action))
    }

    fun sendSystemPanel(action: String) {
        enqueue(JSONObject().put("type", "SYSTEM_PANEL").put("action", action))
    }

    fun sendText(text: String) {
        enqueue(JSONObject().put("type", "TEXT").put("text", text))
    }

    fun sendKey(hidCode: Int, modifier: Int) {
        enqueue(JSONObject().put("type", "KEY").put("code", hidCode).put("mod", modifier))
    }

    fun sendConsumer(usageCode: Int) {
        enqueue(JSONObject().put("type", "CONSUMER").put("usage", usageCode))
    }

    fun sendGamepadState(
        buttonsMask: Int,
        hat: Int,
        lx: Int,
        ly: Int,
        rx: Int,
        ry: Int
    ) {
        enqueue(
            JSONObject()
                .put("type", "GAMEPAD")
                .put("buttons", buttonsMask)
                .put("hat", hat)
                .put("lx", lx.coerceIn(-127, 127))
                .put("ly", ly.coerceIn(-127, 127))
                .put("rx", rx.coerceIn(-127, 127))
                .put("ry", ry.coerceIn(-127, 127))
        )
    }

    fun sendSetVolume(percent: Int) {
        enqueue(JSONObject().put("type", "SET_VOLUME").put("percent", percent.coerceIn(0, 100)))
    }

    fun sendMediaAction(action: String) {
        enqueue(JSONObject().put("type", "MEDIA_ACTION").put("action", action))
    }

    fun requestTelemetry() {
        enqueue(JSONObject().put("type", "GET_TELEMETRY"))
    }

    private fun enqueue(json: JSONObject) {
        if (_connectionStatus.value !is ConnectionStatus.Connected) return
        outgoing.trySend(json.toString())
    }

    private fun cleanupSocketOnly() {
        writerJob?.cancel()
        writerJob = null
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
    }

    fun disconnect() {
        readerJob?.cancel()
        readerJob = null
        outgoing.close()
        cleanupSocketOnly()
        _receiverTelemetry.value = ReceiverTelemetry()
        _connectionStatus.value = ConnectionStatus.Disconnected
    }
}
