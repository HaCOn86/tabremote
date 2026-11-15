package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.accessibility.RemoteAccessibilityService
import com.example.model.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
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

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _connectedController = MutableStateFlow<String?>(null)
    val connectedController: StateFlow<String?> = _connectedController.asStateFlow()

    @Volatile
    private var activeToken: String = ""

    fun start(port: Int, expectedToken: String) {
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
                            writer.write(JSONObject().put("type", "AUTH_OK").toString())
                            writer.newLine()
                            writer.flush()
                        } else {
                            writer.write(JSONObject().put("type", "AUTH_ERR").put("reason", "Geçersiz oturum anahtarı").toString())
                            writer.newLine()
                            writer.flush()
                            break
                        }
                    } else {
                        break
                    }
                } else {
                    dispatchCommand(type, json, writer)
                }
            }
        } catch (e: Exception) {
            Log.d(tag, "Client disconnected: ${e.message}")
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
            if (_connectedController.value == authedName) {
                _connectedController.value = null
            }
        }
    }

    private fun dispatchCommand(type: String, json: JSONObject, writer: BufferedWriter) {
        val svc = RemoteAccessibilityService.getInstance()
        when (type) {
            "MOVE" -> svc?.moveCursor(
                json.optDouble("dx", 0.0).toFloat(),
                json.optDouble("dy", 0.0).toFloat()
            )
            "CLICK" -> svc?.clickAtCursor(json.optString("btn", "LEFT"))
            "BTN_DOWN" -> svc?.startMouseHold(json.optString("btn", "LEFT"))
            "BTN_UP" -> svc?.endMouseHold(json.optString("btn", "LEFT"))
            "SCROLL" -> svc?.scrollAtCursor(json.optInt("v", 0), json.optInt("h", 0))
            "NAV" -> svc?.performNavAction(json.optString("action", "HOME"))
            "TEXT" -> svc?.handleText(json.optString("text", ""))
            "KEY" -> svc?.handleHidKey(json.optInt("code", 0), json.optInt("mod", 0))
            "CONSUMER" -> svc?.handleConsumerUsage(json.optInt("usage", 0))
            "PING" -> {
                writer.write(JSONObject().put("type", "PONG").toString())
                writer.newLine()
                writer.flush()
            }
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

    private var socket: Socket? = null
    private var readerJob: Job? = null
    private var writerJob: Job? = null
    private var outgoing = Channel<String>(capacity = 128)

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

        val sendQueue = Channel<String>(capacity = 128)
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
                }
            } catch (e: Exception) {
                Log.w(tag, "Wi-Fi connect failed", e)
                _connectionStatus.value = ConnectionStatus.Error("Wi-Fi bağlantısı başarısız: ${e.localizedMessage ?: "Zaman aşımı"}")
            } finally {
                cleanupSocketOnly()
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

    fun sendScroll(verticalTicks: Int, horizontalTicks: Int) {
        enqueue(JSONObject().put("type", "SCROLL").put("v", verticalTicks).put("h", horizontalTicks))
    }

    fun sendNav(action: String) {
        enqueue(JSONObject().put("type", "NAV").put("action", action))
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
        _connectionStatus.value = ConnectionStatus.Disconnected
    }
}
