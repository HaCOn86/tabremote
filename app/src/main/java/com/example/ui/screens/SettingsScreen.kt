package com.example.ui.screens

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.PairedDevice
import com.example.model.AirMouseScrollMode
import com.example.model.AppRole
import com.example.model.ButtonPosition
import com.example.model.ConnectionStatus
import com.example.model.HidProfileMode
import com.example.model.RemoteSettings
import com.example.model.ScrollSide
import com.example.model.VolumeAction
import com.example.network.WifiNetworkUtils
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedDanger
import com.example.ui.theme.RubyPrimary
import com.example.ui.theme.SurfaceBorderRuby
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.theme.SurfaceContainerCharcoal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    settings: RemoteSettings,
    userEmail: String,
    userDisplayName: String,
    localDeviceId: String,
    localWifiIp: String,
    accessibilityEnabled: Boolean,
    isReceiverRunning: Boolean,
    connectedControllerName: String?,
    cloudDevices: List<PairedDevice>,
    connectionStatus: ConnectionStatus,
    bondedDevices: List<BluetoothDevice>,
    discoveredDevices: List<BluetoothDevice>,
    isScanningBluetooth: Boolean,
    hidDiagnostics: String?,
    onUpdateSettings: ((RemoteSettings) -> RemoteSettings) -> Unit,
    onChangeAppRole: (AppRole) -> Unit,
    onUpdateLocalCloudName: (String) -> Unit,
    onRefreshEnvironment: () -> Unit,
    onConnectWifiReceiver: (PairedDevice) -> Unit,
    onSignOut: () -> Unit,
    onRefreshBluetooth: () -> Unit,
    onToggleBluetoothScan: () -> Unit,
    onCreateDiscoverableIntent: () -> Intent,
    onRenameBluetoothDevice: (String) -> Unit,
    onPairAndConnectBluetooth: (BluetoothDevice) -> Unit,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onDisconnectDevice: () -> Unit,
    onToggleFavoriteDevice: (address: String) -> Unit,
    onChangeHidProfile: (HidProfileMode) -> Unit,
    onSetUnifiedControlMode: (Boolean) -> Unit,
    onResetDockCustomizations: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isBluetoothListExpanded by remember { mutableStateOf(true) }
    var deviceSearchQuery by remember { mutableStateOf("") }
    var btNameDraft by remember(settings.customBluetoothName) {
        mutableStateOf(settings.customBluetoothName)
    }
    var cloudNameDraft by remember(settings.localDeviceName) {
        mutableStateOf(settings.localDeviceName)
    }

    val discoverableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    val receiverPeers = remember(cloudDevices, localDeviceId) {
        cloudDevices.filter { it.id != localDeviceId && it.role == PairedDevice.ROLE_RECEIVER }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(top = 6.dp, start = 8.dp, end = 8.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. GOOGLE ACCOUNT, ROLE (CONTROLLER vs TABLET RECEIVER) & AUTO-DISCOVERY
        SettingsCard(
            title = "GOOGLE HESABI & WI-FI ALICI MODU",
            icon = Icons.Default.AccountCircle,
            headerAction = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 44.dp)
                ) {
                    Surface(
                        color = RedDanger.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(1.dp, RedDanger.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSignOut() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("sign_out_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Çıkış Yap",
                                tint = RedDanger,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Çıkış", color = RedDanger, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) {
            Column(
                modifier = Modifier.animateContentSize(tween(220)),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userDisplayName.ifBlank { "Google Hesabı Bağlı" },
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (userEmail.isNotBlank()) {
                            Text(
                                text = userEmail,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                    IconButton(onClick = onRefreshEnvironment) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Cihaz Durumunu Yenile",
                            tint = RubyPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Editable Cloud Device Name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = cloudNameDraft,
                        onValueChange = { cloudNameDraft = it },
                        label = { Text("Bu Cihazın Hesap Adı", fontSize = 10.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceContainerCharcoal,
                            unfocusedContainerColor = SurfaceContainerCharcoal,
                            focusedBorderColor = RubyPrimary,
                            unfocusedBorderColor = SurfaceBorderRuby,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onUpdateLocalCloudName(cloudNameDraft) },
                        colors = ButtonDefaults.buttonColors(containerColor = RubyPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Kaydet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                ChoiceRow(
                    label = "Uygulama Çalışma Modu",
                    options = listOf(AppRole.CONTROLLER, AppRole.RECEIVER),
                    selected = settings.appRole,
                    optionLabel = { it.label },
                    onSelect = onChangeAppRole
                )
                Text(
                    text = settings.appRole.subtitle,
                    color = TextSecondary,
                    fontSize = 10.sp
                )

                if (settings.appRole == AppRole.RECEIVER) {
                    Surface(
                        color = SurfaceContainerCharcoal,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (accessibilityEnabled && isReceiverRunning) GreenSuccess.copy(alpha = 0.6f)
                                else RubyPrimary.copy(alpha = 0.55f),
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = "Alıcı Sunucu",
                                        tint = if (isReceiverRunning) GreenSuccess else RubyPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isReceiverRunning) {
                                            "Sunucu Aktif (${localWifiIp.ifBlank { "IP Bekleniyor" }}:${WifiNetworkUtils.DEFAULT_PORT})"
                                        } else {
                                            "Wi-Fi Sunucusu Başlatılıyor..."
                                        },
                                        color = if (isReceiverRunning) GreenSuccess else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Text(
                                text = if (!connectedControllerName.isNullOrBlank()) {
                                    "Bağlı Telefon: $connectedControllerName"
                                } else {
                                    "Telefonunuzda aynı Google hesabıyla oturum açıp Wi-Fi Bağlan'a dokunun."
                                },
                                color = TextSecondary,
                                fontSize = 10.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessibilityNew,
                                        contentDescription = "Erişilebilirlik",
                                        tint = if (accessibilityEnabled) GreenSuccess else RedDanger,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (accessibilityEnabled) {
                                            "Erişilebilirlik Aktif — Özel Fare İmleci Hazır"
                                        } else {
                                            "Erişilebilirlik Kapalı (İmleç & Tıklama için açın)"
                                        },
                                        color = if (accessibilityEnabled) GreenSuccess else RedDanger,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (accessibilityEnabled) SurfaceCharcoal else RubyPrimary
                                ),
                                shape = RoundedCornerShape(9.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, RubyPrimary.copy(alpha = 0.55f), RoundedCornerShape(9.dp))
                                    .testTag("open_accessibility_settings_btn")
                            ) {
                                Text(
                                    text = if (accessibilityEnabled) "Erişilebilirlik Ayarlarına Git" else "Erişilebilirlik Servisini Aç",
                                    color = if (accessibilityEnabled) RubyPrimary else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // CONTROLLER MODE: Auto-discovered Receiver Tablets from Google Account
                    Text(
                        text = "AYNI HESAPTAKİ ALICI TABLETLER (WI-FI)",
                        color = RubyPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp
                    )

                    if (receiverPeers.isEmpty()) {
                        Surface(
                            color = SurfaceContainerCharcoal,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "Henüz aktif Alıcı Tablet bulunamadı. Tabletinize de TabRemote yükleyip aynı Google hesabıyla giriş yapın ve Alıcı Modu'nu seçin.",
                                color = TextMuted,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        receiverPeers.forEach { peer ->
                            val isThisWifiConnected = (connectionStatus as? ConnectionStatus.Connected)?.let {
                                it.isWifi && it.address == "${peer.localIp}:${peer.port}"
                            } == true

                            Surface(
                                color = SurfaceContainerCharcoal,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isThisWifiConnected) GreenSuccess else SurfaceBorderRuby,
                                        RoundedCornerShape(10.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .background(
                                                    if (peer.isOnline) GreenSuccess else TextMuted,
                                                    CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = peer.deviceName,
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${peer.localIp.ifBlank { "IP yok" }}:${peer.port} • " +
                                                    if (peer.accessibilityReady) "İmleç Hazır" else "Erişilebilirlik Kapalı",
                                                color = if (peer.accessibilityReady) TextSecondary else RedDanger,
                                                fontSize = 9.5.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (isThisWifiConnected) onDisconnectDevice()
                                            else onConnectWifiReceiver(peer)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isThisWifiConnected) RedDanger.copy(alpha = 0.25f) else RubyPrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(
                                            text = if (isThisWifiConnected) "Kes" else "Wi-Fi Bağlan",
                                            color = if (isThisWifiConnected) RedDanger else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. BLUETOOTH HID, LIVE SCANNING, DISCOVERABILITY & CUSTOM DEVICE NAME
        SettingsCard(
            title = "BLUETOOTH HID & CANLI TARAMA",
            icon = Icons.Default.Bluetooth,
            headerAction = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRefreshBluetooth) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Bluetooth Yenile",
                            tint = RubyPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = { isBluetoothListExpanded = !isBluetoothListExpanded }) {
                        Icon(
                            imageVector = if (isBluetoothListExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Listeyi Aç/Kapat",
                            tint = RubyPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        ) {
            Column(
                modifier = Modifier.animateContentSize(tween(250)),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = when (connectionStatus) {
                        is ConnectionStatus.Connected -> if (connectionStatus.isWifi) {
                            "Aktif Bağlantı (Wi-Fi): ${connectionStatus.deviceName}"
                        } else {
                            "Bağlandı (Bluetooth HID): ${connectionStatus.deviceName}"
                        }
                        is ConnectionStatus.ReadyToPair -> "HID Hazır: Eşleştirme veya bağlanma bekleniyor"
                        is ConnectionStatus.Initializing -> "Durum: Bağlantı başlatılıyor..."
                        is ConnectionStatus.Disconnected -> "Durum: Bağlantı kesildi"
                        is ConnectionStatus.Error -> "Durum: ${connectionStatus.message}"
                    },
                    color = when (connectionStatus) {
                        is ConnectionStatus.Connected -> GreenSuccess
                        is ConnectionStatus.Error -> RedDanger
                        else -> TextSecondary
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                if (!hidDiagnostics.isNullOrBlank()) {
                    Surface(
                        color = RubyPrimary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, RubyPrimary.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = hidDiagnostics,
                            color = RubyPrimary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Custom Bluetooth / HID Advertised Name Editor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = btNameDraft,
                        onValueChange = { btNameDraft = it.take(30) },
                        label = { Text("Bluetooth HID Cihaz Adı", fontSize = 10.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceContainerCharcoal,
                            unfocusedContainerColor = SurfaceContainerCharcoal,
                            focusedBorderColor = RubyPrimary,
                            unfocusedBorderColor = SurfaceBorderRuby,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onRenameBluetoothDevice(btNameDraft) },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerCharcoal),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .border(1.dp, RubyPrimary.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    ) {
                        Text("Adı Uygula", color = RubyPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onToggleBluetoothScan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanningBluetooth) RubyPrimary.copy(alpha = 0.24f) else SurfaceContainerCharcoal
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, RubyPrimary.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                            .testTag("bt_live_scan_btn")
                    ) {
                        if (isScanningBluetooth) {
                            CircularProgressIndicator(
                                color = RubyPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Taranıyor (Durdur)", color = RubyPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = RubyPrimary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Cihazları Tara", color = RubyPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            discoverableLauncher.launch(onCreateDiscoverableIntent())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerCharcoal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("120sn Görünür Yap", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerCharcoal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(10.dp))
                    ) {
                        Text("Sistem BT Ayarları", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    if (connectionStatus is ConnectionStatus.Connected) {
                        Button(
                            onClick = onDisconnectDevice,
                            colors = ButtonDefaults.buttonColors(containerColor = RedDanger.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, RedDanger.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        ) {
                            Text("Bağlantıyı Kes", color = RedDanger, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isBluetoothListExpanded,
                    enter = expandVertically(tween(220)) + fadeIn(tween(150)),
                    exit = shrinkVertically(tween(220)) + fadeOut(tween(150))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Spacer(modifier = Modifier.height(2.dp))

                        OutlinedTextField(
                            value = deviceSearchQuery,
                            onValueChange = { deviceSearchQuery = it },
                            placeholder = { Text("Kayıtlı / bulunan cihaz ara...", color = TextMuted, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Ara", tint = TextMuted, modifier = Modifier.size(16.dp))
                            },
                            trailingIcon = {
                                if (deviceSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { deviceSearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Temizle", tint = TextMuted, modifier = Modifier.size(16.dp))
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
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )

                        @Suppress("MissingPermission")
                        val filteredBonded = bondedDevices
                            .filter {
                                val name = it.name ?: it.address ?: ""
                                name.contains(deviceSearchQuery, ignoreCase = true)
                            }
                            .sortedByDescending { settings.favoriteDeviceAddresses.contains(it.address) }

                        val bondedAddresses = bondedDevices.mapNotNull { it.address }.toSet()

                        @Suppress("MissingPermission")
                        val filteredDiscovered = discoveredDevices.filter { dev ->
                            dev.address !in bondedAddresses &&
                                (dev.name ?: dev.address ?: "").contains(deviceSearchQuery, ignoreCase = true)
                        }

                        if (filteredBonded.isEmpty() && filteredDiscovered.isEmpty()) {
                            Text(
                                text = "Eşleşmiş cihaz yok. 'Cihazları Tara'ya basın veya tabletin Bluetooth ayarlarından telefonunuzu seçin.",
                                color = TextMuted,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        filteredBonded.forEach { dev ->
                            @Suppress("MissingPermission")
                            val name = dev.name ?: dev.address ?: "Tablet"
                            val isFavorite = settings.favoriteDeviceAddresses.contains(dev.address)

                            Surface(
                                color = SurfaceContainerCharcoal,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, if (isFavorite) RubyPrimary.copy(alpha = 0.5f) else SurfaceBorderRuby, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).clickable { onConnectDevice(dev) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BluetoothConnected,
                                            contentDescription = "Cihaz",
                                            tint = RubyPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            Text("${dev.address} • Eşleşmiş", color = TextMuted, fontSize = 9.sp)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onToggleFavoriteDevice(dev.address) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = "Favori",
                                                tint = if (isFavorite) RubyPrimary else TextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Button(
                                            onClick = { onConnectDevice(dev) },
                                            colors = ButtonDefaults.buttonColors(containerColor = RubyPrimary),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Bağlan", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        if (filteredDiscovered.isNotEmpty()) {
                            Text(
                                text = "YAKINDA BULUNAN CİHAZLAR",
                                color = RubyPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            filteredDiscovered.forEach { dev ->
                                @Suppress("MissingPermission")
                                val name = dev.name ?: dev.address ?: "Bilinmeyen Cihaz"
                                Surface(
                                    color = SurfaceContainerCharcoal,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            Text(dev.address, color = TextMuted, fontSize = 9.sp)
                                        }
                                        Button(
                                            onClick = { onPairAndConnectBluetooth(dev) },
                                            colors = ButtonDefaults.buttonColors(containerColor = RubyPrimary),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Eşle & Bağlan", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. TOUCHPAD & AIR MOUSE DÜZENİ VE ÖZELLEŞTİRME
        SettingsCard(
            title = "TOUCHPAD, HİZALAMA & DOCK ÖZELLEŞTİRME",
            icon = Icons.Default.Dashboard
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingToggleRow(
                    label = "Birleşik Kontrol Modu (Touchpad + Air Mouse)",
                    description = "Touchpad içinden tek dokunuşla animasyonlu Air Mouse moduna geçer. Kapatırsanız ayrı sekmelere ayrılır.",
                    checked = settings.unifiedControlMode,
                    onCheckedChange = onSetUnifiedControlMode
                )

                ChoiceRow(
                    label = "Bluetooth HID Profili",
                    options = listOf(HidProfileMode.PRECISION_TOUCHPAD, HidProfileMode.CLASSIC_MOUSE),
                    selected = settings.hidProfileMode,
                    optionLabel = {
                        if (it == HidProfileMode.PRECISION_TOUCHPAD) "Precision Touchpad" else "Klasik Fare"
                    },
                    onSelect = onChangeHidProfile
                )
                Text(
                    text = settings.hidProfileMode.description,
                    color = TextMuted,
                    fontSize = 9.sp
                )

                ChoiceRow(
                    label = "Air Mouse Kaydırma Modu",
                    options = listOf(AirMouseScrollMode.SCROLL_STRIP, AirMouseScrollMode.STEP_BUTTONS),
                    selected = settings.airMouseScrollMode,
                    optionLabel = { it.label },
                    onSelect = { onUpdateSettings { s -> s.copy(airMouseScrollMode = it) } }
                )

                SettingToggleRow(
                    label = "F1 & F2 Ekstra Fare Tuşlarını Göster",
                    description = "Alt dock'ta özelleştirilebilir 4. ve 5. fare tuşları (basılı tutarak yeri değişir)",
                    checked = settings.showFunctionButtons,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(showFunctionButtons = it) } }
                )

                Button(
                    onClick = onResetDockCustomizations,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerCharcoal),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(9.dp))
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = RubyPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Alt Dock ve Medya Tuş Sırasını Sıfırla", color = RubyPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                SettingToggleRow(
                    label = "Üst Yatay Kaydırma Rayı",
                    description = "Üst bölümde yatay galeri/sekme geçiş rayı gösterir",
                    checked = settings.showHorizontalScrollGutter,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(showHorizontalScrollGutter = it) } }
                )

                ChoiceRow(
                    label = "Dikey Kaydırma Alanı Yeri",
                    options = listOf(ScrollSide.RIGHT, ScrollSide.LEFT, ScrollSide.HIDDEN),
                    selected = settings.scrollSide,
                    optionLabel = { it.label },
                    onSelect = { onUpdateSettings { s -> s.copy(scrollSide = it) } }
                )

                ChoiceRow(
                    label = "Fare Tuşları Konumu",
                    options = listOf(ButtonPosition.BOTTOM, ButtonPosition.TOP, ButtonPosition.SPLIT),
                    selected = settings.buttonPosition,
                    optionLabel = { it.label },
                    onSelect = { onUpdateSettings { s -> s.copy(buttonPosition = it) } }
                )

                SettingToggleRow(
                    label = "Sağ Tık Butonunu Göster",
                    description = "Tablette bağlam menüsü açmak için sağ tık butonu",
                    checked = settings.showRightClick,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(showRightClick = it) } }
                )

                SettingToggleRow(
                    label = "Orta Tuş Butonunu Göster",
                    description = "Bağlantı ve sekme açmak için orta tık butonu",
                    checked = settings.showMiddleClick,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(showMiddleClick = it) } }
                )

                SettingToggleRow(
                    label = "Dokunarak Sol Tıklama (Tap to Click)",
                    description = "Touchpad yüzeyine tek dokunma sol tık gönderir",
                    checked = settings.tapToClick,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(tapToClick = it) } }
                )

                SettingToggleRow(
                    label = "Ters Kaydırma (Invert Scroll)",
                    description = "Kaydırma yönünü tersine çevirir",
                    checked = settings.invertScroll,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(invertScroll = it) } }
                )

                SettingToggleRow(
                    label = "Titreşimli Geri Bildirim (Haptics)",
                    description = "Tıklama ve kaydırma adımlarında dokunsal titreşim",
                    checked = settings.hapticsEnabled,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(hapticsEnabled = it) } }
                )
            }
        }

        // 4. TOUCHPAD VOLUME KEYS
        SettingsCard(
            title = "TOUCHPAD SES TUŞU AYARLARI",
            icon = Icons.Default.TouchApp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Touchpad ekranındayken telefonun ses tuşlarına basıldığında yapılacak eylemi belirleyin.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                ModernVolumeActionSelector(
                    label = "Ses Kısma Tuşu (Volume Down)",
                    selectedAction = settings.touchpadVolumeDownAction,
                    onSelect = { onUpdateSettings { s -> s.copy(touchpadVolumeDownAction = it) } }
                )
                ModernVolumeActionSelector(
                    label = "Ses Açma Tuşu (Volume Up)",
                    selectedAction = settings.touchpadVolumeUpAction,
                    onSelect = { onUpdateSettings { s -> s.copy(touchpadVolumeUpAction = it) } }
                )
            }
        }

        // 5. AIR MOUSE VOLUME KEYS
        SettingsCard(
            title = "AIR MOUSE SES TUŞU AYARLARI",
            icon = Icons.Default.Sensors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Air Mouse modundayken fiziksel ses tuşları ile havada sol/sağ tık veya kaydırma yapın.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                ModernVolumeActionSelector(
                    label = "Air Mouse Ses Kısma Tuşu",
                    selectedAction = settings.airMouseVolumeDownAction,
                    onSelect = { onUpdateSettings { s -> s.copy(airMouseVolumeDownAction = it) } }
                )
                ModernVolumeActionSelector(
                    label = "Air Mouse Ses Açma Tuşu",
                    selectedAction = settings.airMouseVolumeUpAction,
                    onSelect = { onUpdateSettings { s -> s.copy(airMouseVolumeUpAction = it) } }
                )
            }
        }

        // 6. HASSASIYET VE HAREKET AYARLARI
        SettingsCard(
            title = "HASSASİYET VE HAREKET AYARLARI",
            icon = Icons.Default.Sensors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SliderRow(
                    label = "Touchpad İmleç Hızı",
                    value = settings.pointerSensitivity,
                    range = 0.5f..3.0f,
                    displayValue = "${"%.1f".format(settings.pointerSensitivity)}x",
                    onValueChange = { onUpdateSettings { s -> s.copy(pointerSensitivity = it) } }
                )

                SliderRow(
                    label = "Kaydırma Hızı (Scroll Speed)",
                    value = settings.scrollSensitivity,
                    range = 0.4f..2.5f,
                    displayValue = "${"%.1f".format(settings.scrollSensitivity)}x",
                    onValueChange = { onUpdateSettings { s -> s.copy(scrollSensitivity = it) } }
                )

                SliderRow(
                    label = "Air Mouse Jiroskop Hassasiyeti",
                    value = settings.airMouseSensitivity,
                    range = 0.5f..5.0f,
                    displayValue = "${"%.1f".format(settings.airMouseSensitivity)}x",
                    onValueChange = { onUpdateSettings { s -> s.copy(airMouseSensitivity = it) } }
                )

                SliderRow(
                    label = "Titreme Filtresi (Deadband)",
                    value = settings.airMouseDeadband,
                    range = 0.01f..0.12f,
                    displayValue = "${"%.2f".format(settings.airMouseDeadband)}",
                    onValueChange = { onUpdateSettings { s -> s.copy(airMouseDeadband = it) } }
                )
            }
        }

        // Compact Footer Version Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = SurfaceCharcoal,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(20.dp))
                    .testTag("settings_version_badge")
            ) {
                Text(
                    text = "TabRemote v${BuildConfig.VERSION_NAME}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(92.dp))
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    headerAction: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        color = SurfaceCharcoal,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = RubyPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp
                    )
                }
                headerAction?.invoke()
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernVolumeActionSelector(
    label: String,
    selectedAction: VolumeAction,
    onSelect: (VolumeAction) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Column {
        Text(text = label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            color = SurfaceContainerCharcoal,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, RubyPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .clickable { showSheet = true }
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(
                        text = selectedAction.title,
                        color = RubyPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedAction.description,
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Değiştir",
                    tint = RubyPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = SurfaceCharcoal,
                contentColor = TextPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Ses Tuşu İşlevi Seçin",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val grouped = VolumeAction.entries.groupBy { it.category }
                    grouped.forEach { (category, actions) ->
                        Text(
                            text = category,
                            color = RubyPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        actions.forEach { action ->
                            val isSelected = action == selectedAction
                            Surface(
                                color = if (isSelected) RubyPrimary.copy(alpha = 0.2f) else SurfaceContainerCharcoal,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) RubyPrimary else SurfaceBorderRuby,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        onSelect(action)
                                        showSheet = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = action.title,
                                            color = if (isSelected) RubyPrimary else TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = action.description,
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Seçili",
                                            tint = RubyPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> ChoiceRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column {
        Text(text = label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                Surface(
                    color = if (isSelected) RubyPrimary.copy(alpha = 0.22f) else SurfaceContainerCharcoal,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            1.dp,
                            if (isSelected) RubyPrimary else SurfaceBorderRuby,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelect(option) }
                        .padding(vertical = 8.dp),
                    contentColor = if (isSelected) RubyPrimary else TextSecondary
                ) {
                    Text(
                        text = optionLabel(option),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(text = description, color = TextSecondary, fontSize = 9.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = RubyPrimary,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceBorderRuby
            )
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(text = displayValue, color = RubyPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = RubyPrimary,
                activeTrackColor = RubyPrimary,
                inactiveTrackColor = SurfaceBorderRuby
            )
        )
    }
}
