package com.example.ui.screens

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.PairedDevice
import com.example.model.AppRole
import com.example.model.AuthSessionMode
import com.example.model.ConnectionStatus
import com.example.model.RemoteSettings
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
internal fun SubPageHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 46.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = SurfaceCharcoal,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .size(42.dp)
                .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .clickable { onBack() }
                .testTag("settings_subpage_back_btn")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Geri",
                    tint = RubyPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 10.5.sp
            )
        }
    }
}

@Composable
internal fun EditSaveLockField(
    label: String,
    currentValue: String,
    maxLength: Int,
    onSave: (String) -> Unit,
    testTagPrefix: String
) {
    var isEditing by remember { mutableStateOf(false) }
    var draft by remember(currentValue) { mutableStateOf(currentValue) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(tween(180)),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (!isEditing) {
            Surface(
                color = SurfaceContainerCharcoal,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Kilitli",
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentValue.ifBlank { "Belirlenmedi" },
                            color = TextPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        color = RubyPrimary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(1.dp, RubyPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                draft = currentValue
                                isEditing = true
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("${testTagPrefix}_edit_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Düzenle",
                                tint = RubyPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Düzenle",
                                color = RubyPrimary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(maxLength) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceContainerCharcoal,
                        unfocusedContainerColor = SurfaceContainerCharcoal,
                        focusedBorderColor = RubyPrimary,
                        unfocusedBorderColor = SurfaceBorderRuby,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${testTagPrefix}_input")
                )
                Button(
                    onClick = {
                        val cleaned = draft.trim()
                        if (cleaned.isNotEmpty()) {
                            onSave(cleaned)
                        }
                        isEditing = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RubyPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("${testTagPrefix}_save_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Kaydet",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kaydet", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun BluetoothSettingsSubPage(
    settings: RemoteSettings,
    connectionStatus: ConnectionStatus,
    bondedDevices: List<BluetoothDevice>,
    discoveredDevices: List<BluetoothDevice>,
    isScanningBluetooth: Boolean,
    hidDiagnostics: String?,
    onBack: () -> Unit,
    onRefreshBluetooth: () -> Unit,
    onToggleBluetoothScan: () -> Unit,
    onCreateDiscoverableIntent: () -> Intent,
    onRenameBluetoothDevice: (String) -> Unit,
    onPairAndConnectBluetooth: (BluetoothDevice) -> Unit,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onDisconnectDevice: () -> Unit,
    onToggleFavoriteDevice: (address: String) -> Unit
) {
    val context = LocalContext.current
    var deviceSearchQuery by remember { mutableStateOf("") }
    val discoverableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubPageHeader(
            title = "Bluetooth HID & Cihazlar",
            subtitle = "Eşleşmiş cihazlar, canlı tarama ve HID yayın adı",
            onBack = onBack
        )

        SettingsCard(
            title = "HID DURUMU & YAYIN ADI",
            icon = Icons.Default.Bluetooth,
            headerAction = {
                IconButton(onClick = onRefreshBluetooth) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Bluetooth Yenile",
                        tint = RubyPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = when (connectionStatus) {
                        is ConnectionStatus.Connected -> if (connectionStatus.isWifi) {
                            "Aktif Bağlantı (Wi-Fi): ${connectionStatus.deviceName}"
                        } else {
                            "Bağlandı (Touchpad HID): ${connectionStatus.deviceName}"
                        }
                        is ConnectionStatus.ReadyToPair -> "Touchpad HID Hazır: Eşleştirme veya bağlanma bekleniyor"
                        is ConnectionStatus.Initializing -> "Durum: HID profili başlatılıyor..."
                        is ConnectionStatus.Disconnected -> "Durum: Bağlantı kesildi"
                        is ConnectionStatus.Error -> "Durum: ${connectionStatus.message}"
                    },
                    color = when (connectionStatus) {
                        is ConnectionStatus.Connected -> GreenSuccess
                        is ConnectionStatus.Error -> RedDanger
                        else -> TextSecondary
                    },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
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

                EditSaveLockField(
                    label = "Bluetooth HID Yayın Adı",
                    currentValue = settings.customBluetoothName,
                    maxLength = 30,
                    onSave = onRenameBluetoothDevice,
                    testTagPrefix = "bt_name"
                )

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
                            .height(44.dp)
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
                            Icon(Icons.Default.Search, contentDescription = null, tint = RubyPrimary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Cihazları Tara", color = RubyPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { discoverableLauncher.launch(onCreateDiscoverableIntent()) },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerCharcoal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
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
                        onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerCharcoal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
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
                                .height(42.dp)
                                .border(1.dp, RedDanger.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        ) {
                            Text("Bağlantıyı Kes", color = RedDanger, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        SettingsCard(
            title = "KAYITLI & YAKINDAKİ CİHAZLAR",
            icon = Icons.Default.BluetoothConnected
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
internal fun GoogleAccountDevicesSubPage(
    settings: RemoteSettings,
    authSessionMode: AuthSessionMode,
    userEmail: String,
    userDisplayName: String,
    localDeviceId: String,
    localWifiIp: String,
    accessibilityEnabled: Boolean,
    isReceiverRunning: Boolean,
    connectedControllerName: String?,
    cloudDevices: List<PairedDevice>,
    connectionStatus: ConnectionStatus,
    onBack: () -> Unit,
    onChangeAppRole: (AppRole) -> Unit,
    onUpdateLocalCloudName: (String) -> Unit,
    onRefreshEnvironment: () -> Unit,
    onConnectWifiReceiver: (PairedDevice) -> Unit,
    onDisconnectDevice: () -> Unit,
    onOpenPermissionWizard: () -> Unit,
    onGoogleAccountLinked: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLinkingGoogle by remember { mutableStateOf(false) }
    var linkError by remember { mutableStateOf<String?>(null) }

    val receiverPeers = remember(cloudDevices, localDeviceId) {
        cloudDevices.filter { it.id != localDeviceId && it.role == PairedDevice.ROLE_RECEIVER }
    }
    val allOtherPeers = remember(cloudDevices, localDeviceId) {
        cloudDevices.filter { it.id != localDeviceId && it.role != PairedDevice.ROLE_RECEIVER }
    }
    val isGoogleSignedIn = authSessionMode == AuthSessionMode.GOOGLE && userEmail.isNotBlank()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubPageHeader(
            title = "Google Hesabı & Cihazlar",
            subtitle = "Bulut cihaz eşleştirme, IP senkronizasyonu ve Alıcı modu",
            onBack = onBack
        )

        SettingsCard(
            title = if (isGoogleSignedIn) "BAĞLI GOOGLE HESABI" else "GOOGLE HESABI BAĞLA",
            icon = Icons.Default.AccountCircle,
            headerAction = {
                if (isGoogleSignedIn) {
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isGoogleSignedIn) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userDisplayName.ifBlank { "Google Hesabı Bağlı" },
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userEmail,
                                color = TextSecondary,
                                fontSize = 10.5.sp
                            )
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

                    EditSaveLockField(
                        label = "Bu Cihazın Hesap / Ağ Adı",
                        currentValue = settings.localDeviceName,
                        maxLength = 60,
                        onSave = onUpdateLocalCloudName,
                        testTagPrefix = "cloud_name"
                    )
                } else {
                    Text(
                        text = "Şu anda ${authSessionMode.label} oturumuyla devam ediyorsunuz. Tabletinizdeki Alıcı Modu ile otomatik Wi-Fi eşleşmesi için şimdi Google hesabınızı bağlayabilirsiniz.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    if (!linkError.isNullOrBlank()) {
                        Text(
                            text = linkError.orEmpty(),
                            color = RedDanger,
                            fontSize = 10.5.sp
                        )
                    }
                    Button(
                        onClick = {
                            linkError = null
                            isLinkingGoogle = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                scope = scope,
                                onAuthSuccess = {
                                    isLinkingGoogle = false
                                    onGoogleAccountLinked()
                                },
                                onAuthError = { msg ->
                                    isLinkingGoogle = false
                                    linkError = msg
                                },
                                onAuthCancelled = {
                                    isLinkingGoogle = false
                                }
                            )
                        },
                        enabled = !isLinkingGoogle,
                        colors = ButtonDefaults.buttonColors(containerColor = RubyPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("link_google_account_btn")
                    ) {
                        if (isLinkingGoogle) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bağlanıyor...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("Google Hesabı Bağla", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderRuby),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("Giriş Ekranına Dön", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        SettingsCard(
            title = "ÇALIŞMA MODU & WI-FI ALICI DURUMU",
            icon = Icons.Default.Wifi
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

                Surface(
                    color = SurfaceContainerCharcoal,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (accessibilityEnabled) GreenSuccess.copy(alpha = 0.55f) else RubyPrimary.copy(alpha = 0.55f),
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (settings.appRole == AppRole.RECEIVER) {
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
                            Text(
                                text = if (!connectedControllerName.isNullOrBlank()) {
                                    "Bağlı Telefon: $connectedControllerName"
                                } else {
                                    "Telefonunuzda aynı Google hesabıyla oturum açıp Wi-Fi Bağlan'a dokunun."
                                },
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    "Erişilebilirlik Kapalı (Android 13 Kısıtlı Ayar / İzin Denetimi)"
                                },
                                color = if (accessibilityEnabled) GreenSuccess else RedDanger,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = onOpenPermissionWizard,
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
                                text = if (accessibilityEnabled) "Erişilebilirlik & İzin Durumunu Aç" else "Erişilebilirlik & Kısıtlı Ayar Rehberi",
                                color = if (accessibilityEnabled) RubyPrimary else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (isGoogleSignedIn) {
            SettingsCard(
                title = "HESAPTAKİ CİHAZLAR & ALICI TABLETLER",
                icon = Icons.Default.Wifi
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (receiverPeers.isEmpty() && allOtherPeers.isEmpty()) {
                        Text(
                            text = "Hesabınıza kayıtlı başka cihaz bulunamadı. Diğer cihazınızda da aynı Google hesabıyla giriş yapın.",
                            color = TextMuted,
                            fontSize = 10.5.sp
                        )
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
                                                    if (peer.accessibilityReady) "Alıcı Hazır" else "Erişilebilirlik Kapalı",
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
                                        modifier = Modifier.height(34.dp)
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

                        if (allOtherPeers.isNotEmpty()) {
                            Text(
                                text = "DİĞER KAYITLI CİHAZLAR (KUMANDA)",
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp
                            )
                            allOtherPeers.forEach { dev ->
                                Surface(
                                    color = SurfaceContainerCharcoal,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(dev.deviceName, color = TextPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                        Text(dev.localIp.ifBlank { "Çevrimdışı" }, color = TextMuted, fontSize = 10.sp)
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
