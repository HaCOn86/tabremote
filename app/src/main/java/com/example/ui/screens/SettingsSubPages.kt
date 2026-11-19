package com.example.ui.screens

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import com.example.data.PairedDevice
import com.example.model.AppRole
import com.example.model.AuthSessionMode
import com.example.model.ConnectionStatus
import com.example.model.RemoteSettings
import com.example.network.WifiNetworkUtils
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TabRemoteTheme

import androidx.compose.material.icons.filled.MusicNote
@Composable
internal fun SubPageHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = if (showBackButton) 46.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showBackButton) {
            val backShape = RoundedCornerShape(10.dp)
            Surface(
                color = palette.surface,
                shape = backShape,
                modifier = Modifier
                    .size(48.dp)
                    .clip(backShape)
                    .border(1.dp, palette.borderAccent, backShape)
                    .clickable { onBack() }
                    .testTag("settings_subpage_back_btn")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = palette.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = subtitle,
                color = palette.textSecondary,
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
    val palette = TabRemoteTheme.palette
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
            color = palette.textSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (!isEditing) {
            val containerShape = RoundedCornerShape(10.dp)
            Surface(
                color = palette.surfaceContainer,
                shape = containerShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.borderAccent, containerShape)
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
                            tint = palette.textMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentValue.ifBlank { "Belirlenmedi" },
                            color = palette.textPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    val editBtnShape = RoundedCornerShape(8.dp)
                    Surface(
                        color = palette.primary.copy(alpha = 0.16f),
                        shape = editBtnShape,
                        modifier = Modifier
                            .clip(editBtnShape)
                            .border(1.dp, palette.primary.copy(alpha = 0.5f), editBtnShape)
                            .clickable {
                                draft = currentValue
                                isEditing = true
                            }
                            .testTag("${testTagPrefix}_edit_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Düzenle",
                                tint = palette.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Düzenle",
                                color = palette.primary,
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
                        focusedContainerColor = palette.surfaceContainer,
                        unfocusedContainerColor = palette.surfaceContainer,
                        focusedBorderColor = palette.primary,
                        unfocusedBorderColor = palette.borderAccent,
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onAccent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("${testTagPrefix}_save_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Kaydet",
                        tint = palette.onAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kaydet", color = palette.onAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    deviceTypeLabel: (BluetoothDevice) -> String = { "Bluetooth Cihazı" },
    onBack: () -> Unit,
    onRefreshBluetooth: () -> Unit,
    onToggleBluetoothScan: () -> Unit,
    onCreateDiscoverableIntent: () -> Intent,
    onPairAndConnectBluetooth: (BluetoothDevice) -> Unit,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onUnpairDevice: (BluetoothDevice) -> Unit = {},
    onDisconnectDevice: () -> Unit,
    onToggleFavoriteDevice: (address: String) -> Unit,
    showBackButton: Boolean = true
) {
    val palette = TabRemoteTheme.palette
    val context = LocalContext.current
    var deviceSearchQuery by remember { mutableStateOf("") }
    var permissionRefreshNonce by remember { mutableStateOf(0) }

    val fineLocGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val nearbyGranted = remember(permissionRefreshNonce, isScanningBluetooth, bondedDevices, fineLocGranted) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                fineLocGranted
        } else {
            fineLocGranted
        }
    }

    val discoverableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionRefreshNonce++
        onRefreshBluetooth()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubPageHeader(
            title = "Bluetooth HID & Cihazlar",
            subtitle = "Eşleşmiş cihazlar, Nearby & Konum tarama ve HID Fare+Klavye kaydı",
            onBack = onBack,
            showBackButton = showBackButton
        )

        SettingsCard(
            title = "YAKINDAKİ CİHAZLAR & HID PROFİLİ",
            icon = Icons.Default.Security
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val badgeShape = RoundedCornerShape(9.dp)
                Surface(
                    color = palette.surfaceContainer,
                    shape = badgeShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (nearbyGranted) GreenSuccess.copy(alpha = 0.5f) else RedDanger.copy(alpha = 0.55f),
                            badgeShape
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (nearbyGranted) {
                                "Bluetooth Tarama & Konum İzni Açık"
                            } else {
                                "Bluetooth Tarama veya Konum İzni Eksik"
                            },
                            color = if (nearbyGranted) GreenSuccess else RedDanger,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Birleşik Bluetooth HID SDP (Fare + Klavye + Medya) aktif • Tablet cihazı giriş aygıtı olarak tanır",
                            color = palette.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!nearbyGranted) {
                        Button(
                            onClick = {
                                val reqs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    arrayOf(
                                        Manifest.permission.BLUETOOTH_SCAN,
                                        Manifest.permission.BLUETOOTH_CONNECT,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                } else {
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                                btPermissionLauncher.launch(reqs)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary,
                                contentColor = palette.onAccent
                            ),
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Text("İzinleri Ver", color = palette.onAccent, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceContainer),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .border(1.dp, palette.borderAccent, RoundedCornerShape(9.dp))
                    ) {
                        Text(
                            text = "Uygulama İzinlerini Aç",
                            color = palette.textPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        SettingsCard(
            title = "HID DURUMU & TARAMA",
            icon = Icons.Default.Bluetooth,
            headerAction = {
                IconButton(onClick = onRefreshBluetooth) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Bluetooth Yenile",
                        tint = palette.primary,
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
                            "Bağlandı (Bluetooth HID): ${connectionStatus.deviceName}"
                        }
                        is ConnectionStatus.ReadyToPair -> "Bluetooth HID Hazır: Eşleştirme veya bağlanma bekleniyor"
                        is ConnectionStatus.Initializing -> "Durum: HID profili başlatılıyor..."
                        is ConnectionStatus.Disconnected -> "Durum: Bağlantı kesildi"
                        is ConnectionStatus.Error -> "Durum: ${connectionStatus.message}"
                    },
                    color = when (connectionStatus) {
                        is ConnectionStatus.Connected -> GreenSuccess
                        is ConnectionStatus.Error -> RedDanger
                        else -> palette.textSecondary
                    },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (!hidDiagnostics.isNullOrBlank()) {
                    val diagShape = RoundedCornerShape(8.dp)
                    Surface(
                        color = palette.primary.copy(alpha = 0.14f),
                        shape = diagShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.primary.copy(alpha = 0.45f), diagShape)
                    ) {
                        Text(
                            text = hidDiagnostics,
                            color = palette.primary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onToggleBluetoothScan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanningBluetooth) palette.primary.copy(alpha = 0.24f) else palette.surfaceContainer
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .border(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                            .testTag("bt_live_scan_btn")
                    ) {
                        if (isScanningBluetooth) {
                            CircularProgressIndicator(
                                color = palette.primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Taranıyor (Durdur)", color = palette.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, tint = palette.primary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Cihazları Tara", color = palette.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { discoverableLauncher.launch(onCreateDiscoverableIntent()) },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceContainer),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .border(1.dp, palette.borderAccent, RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("120sn Görünür Yap", color = palette.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceContainer),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .border(1.dp, palette.borderAccent, RoundedCornerShape(10.dp))
                    ) {
                        Text("Sistem BT Ayarları", color = palette.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    if (connectionStatus is ConnectionStatus.Connected) {
                        Button(
                            onClick = onDisconnectDevice,
                            colors = ButtonDefaults.buttonColors(containerColor = RedDanger.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
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
                    placeholder = { Text("Kayıtlı / bulunan cihaz ara...", color = palette.textMuted, fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Ara", tint = palette.textMuted, modifier = Modifier.size(16.dp))
                    },
                    trailingIcon = {
                        if (deviceSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { deviceSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Temizle", tint = palette.textMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = palette.surfaceContainer,
                        unfocusedContainerColor = palette.surfaceContainer,
                        focusedBorderColor = palette.primary,
                        unfocusedBorderColor = palette.borderAccent,
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary
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
                        color = palette.textMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                filteredBonded.forEach { dev ->
                    @Suppress("MissingPermission")
                    val name = dev.name ?: dev.address ?: "Tablet"
                    val isFavorite = settings.favoriteDeviceAddresses.contains(dev.address)
                    val rowShape = RoundedCornerShape(9.dp)

                    Surface(
                        color = palette.surfaceContainer,
                        shape = rowShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (isFavorite) palette.primary.copy(alpha = 0.5f) else palette.borderAccent, rowShape)
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
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onConnectDevice(dev) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BluetoothConnected,
                                    contentDescription = "Cihaz",
                                    tint = palette.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, color = palette.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = "${dev.address} • ${deviceTypeLabel(dev)}",
                                        color = palette.textMuted,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onToggleFavoriteDevice(dev.address) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Favori",
                                        tint = if (isFavorite) palette.primary else palette.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onUnpairDevice(dev) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("bt_unpair_${dev.address}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LinkOff,
                                        contentDescription = "Eşleşmeyi Kaldır",
                                        tint = RedDanger,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(3.dp))
                                Button(
                                    onClick = { onConnectDevice(dev) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.primary,
                                        contentColor = palette.onAccent
                                    ),
                                    shape = RoundedCornerShape(7.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Bağlan", color = palette.onAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (filteredDiscovered.isNotEmpty()) {
                    Text(
                        text = "YAKINDA BULUNAN CİHAZLAR",
                        color = palette.primary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    filteredDiscovered.forEach { dev ->
                        @Suppress("MissingPermission")
                        val name = dev.name ?: dev.address ?: "Bilinmeyen Cihaz"
                        val discShape = RoundedCornerShape(9.dp)
                        Surface(
                            color = palette.surfaceContainer,
                            shape = discShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.borderAccent, discShape)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, color = palette.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(dev.address, color = palette.textMuted, fontSize = 9.sp)
                                }
                                Button(
                                    onClick = { onPairAndConnectBluetooth(dev) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.primary,
                                        contentColor = palette.onAccent
                                    ),
                                    shape = RoundedCornerShape(7.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Eşle & Bağlan", color = palette.onAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
    notificationListenerEnabled: Boolean = false,
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
    onSignOut: () -> Unit,
    showBackButton: Boolean = true
) {
    val palette = TabRemoteTheme.palette
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
    val showReceiverStatusBox = isGoogleSignedIn || settings.appRole == AppRole.RECEIVER

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubPageHeader(
            title = "Google Hesabı & Cihazlar",
            subtitle = "Bulut cihaz eşleştirme, IP senkronizasyonu ve Alıcı modu",
            onBack = onBack,
            showBackButton = showBackButton
        )

        SettingsCard(
            title = if (isGoogleSignedIn) "BAĞLI GOOGLE HESABI" else "GOOGLE HESABI BAĞLA",
            icon = Icons.Default.AccountCircle,
            headerAction = {
                if (isGoogleSignedIn) {
                    val signOutShape = RoundedCornerShape(8.dp)
                    Surface(
                        color = RedDanger.copy(alpha = 0.14f),
                        shape = signOutShape,
                        modifier = Modifier
                            .clip(signOutShape)
                            .border(1.dp, RedDanger.copy(alpha = 0.45f), signOutShape)
                            .clickable { onSignOut() }
                            .testTag("sign_out_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
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
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userEmail,
                                color = palette.textSecondary,
                                fontSize = 10.5.sp
                            )
                        }
                        IconButton(onClick = onRefreshEnvironment) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Cihaz Durumunu Yenile",
                                tint = palette.primary,
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
                        color = palette.textSecondary,
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onAccent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("link_google_account_btn")
                    ) {
                        if (isLinkingGoogle) {
                            CircularProgressIndicator(color = palette.onAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bağlanıyor...", color = palette.onAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("Google Hesabı Bağla", color = palette.onAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, palette.borderAccent),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Giriş Ekranına Dön", color = palette.textSecondary, fontSize = 11.sp)
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
                    color = palette.textSecondary,
                    fontSize = 10.sp
                )

                if (showReceiverStatusBox) {
                    if (settings.appRole == AppRole.RECEIVER) {
                        val statusShape = RoundedCornerShape(10.dp)
                        Surface(
                            color = palette.surfaceContainer,
                            shape = statusShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (isReceiverRunning) GreenSuccess.copy(alpha = 0.45f) else palette.borderAccent,
                                    statusShape
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isReceiverRunning) {
                                        "Sunucu Aktif (${localWifiIp.ifBlank { "IP Bekleniyor" }}:${WifiNetworkUtils.DEFAULT_PORT})"
                                    } else {
                                        "Wi-Fi Sunucusu Başlatılıyor..."
                                    },
                                    color = if (isReceiverRunning) GreenSuccess else palette.textPrimary,
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
                                    color = palette.textSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    SettingsNavEntryTile(
                        icon = Icons.Default.AccessibilityNew,
                        title = "İzinler & Android 13 Kısıtlı Ayar Kontrolü",
                        subtitle = if (accessibilityEnabled) {
                            "Erişilebilirlik aktif • Alıcı ve özel imleç denetimi hazır"
                        } else {
                            "Erişilebilirlik kapalı — Alıcı modda gerekli, açmak için dokunun"
                        },
                        badgeColor = if (accessibilityEnabled) GreenSuccess else RedDanger,
                        onClick = onOpenPermissionWizard,
                        testTag = "open_accessibility_settings_btn"
                    )

                    SettingsNavEntryTile(
                        icon = Icons.Default.MusicNote,
                        title = "Bildirim Dinleme İzni (Çalan Medya)",
                        subtitle = if (notificationListenerEnabled) {
                            "Aktif • Wi-Fi Kontrol sekmesine tam şarkı/sanatçı bilgisi gönderir"
                        } else {
                            "Kapalı — Çalan medya adını görmek için açın"
                        },
                        badgeColor = if (notificationListenerEnabled) GreenSuccess else RedDanger,
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        testTag = "open_notification_listener_btn"
                    )
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
                            color = palette.textMuted,
                            fontSize = 10.5.sp
                        )
                    } else {
                        receiverPeers.forEach { peer ->
                            val isThisWifiConnected = (connectionStatus as? ConnectionStatus.Connected)?.let {
                                it.isWifi && it.address == "${peer.localIp}:${peer.port}"
                            } == true

                            val peerShape = RoundedCornerShape(10.dp)
                            Surface(
                                color = palette.surfaceContainer,
                                shape = peerShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isThisWifiConnected) GreenSuccess else palette.borderAccent,
                                        peerShape
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
                                                    if (peer.isOnline) GreenSuccess else palette.textMuted,
                                                    CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = peer.deviceName,
                                                color = palette.textPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${peer.localIp.ifBlank { "IP yok" }}:${peer.port} • " +
                                                    if (peer.accessibilityReady) "Alıcı Hazır" else "Erişilebilirlik Kapalı",
                                                color = if (peer.accessibilityReady) palette.textSecondary else RedDanger,
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
                                            containerColor = if (isThisWifiConnected) RedDanger.copy(alpha = 0.25f) else palette.primary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(
                                            text = if (isThisWifiConnected) "Kes" else "Wi-Fi Bağlan",
                                            color = if (isThisWifiConnected) RedDanger else palette.onAccent,
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
                                color = palette.textMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp
                            )
                            allOtherPeers.forEach { dev ->
                                val otherShape = RoundedCornerShape(8.dp)
                                Surface(
                                    color = palette.surfaceContainer,
                                    shape = otherShape,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, palette.borderAccent, otherShape)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(dev.deviceName, color = palette.textPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                        Text(dev.localIp.ifBlank { "Çevrimdışı" }, color = palette.textMuted, fontSize = 10.sp)
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
