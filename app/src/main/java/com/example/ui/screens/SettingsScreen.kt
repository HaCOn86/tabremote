package com.example.ui.screens

import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.PairedDevice
import com.example.model.AirMouseScrollMode
import com.example.model.AppRole
import com.example.model.AuthSessionMode
import com.example.model.ButtonPosition
import com.example.model.ConnectionStatus
import com.example.model.HidProfileMode
import com.example.model.RemoteSettings
import com.example.model.ScrollSide
import com.example.model.VolumeAction
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedDanger
import com.example.ui.theme.RubyPrimary
import com.example.ui.theme.SurfaceBorderRuby
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.theme.SurfaceContainerCharcoal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class SettingsSubPage {
    MAIN,
    BLUETOOTH,
    GOOGLE_ACCOUNT
}

@Composable
fun SettingsScreen(
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
    bondedDevices: List<BluetoothDevice>,
    discoveredDevices: List<BluetoothDevice>,
    isScanningBluetooth: Boolean,
    hidDiagnostics: String?,
    onUpdateSettings: ((RemoteSettings) -> RemoteSettings) -> Unit,
    onChangeAppRole: (AppRole) -> Unit,
    onUpdateLocalCloudName: (String) -> Unit,
    onRefreshEnvironment: () -> Unit,
    onConnectWifiReceiver: (PairedDevice) -> Unit,
    onOpenPermissionWizard: () -> Unit,
    onGoogleAccountLinked: () -> Unit,
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
    var activeSubPage by remember { mutableStateOf(SettingsSubPage.MAIN) }

    BackHandler(enabled = activeSubPage != SettingsSubPage.MAIN) {
        activeSubPage = SettingsSubPage.MAIN
    }

    AnimatedContent(
        targetState = activeSubPage,
        transitionSpec = {
            if (targetState != SettingsSubPage.MAIN) {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 3 } + fadeOut())
            } else {
                (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { it / 3 } + fadeOut())
            }
        },
        label = "settings_subpage_nav",
        modifier = modifier.fillMaxSize()
    ) { page ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(top = 6.dp, start = 8.dp, end = 8.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (page) {
                SettingsSubPage.BLUETOOTH -> {
                    BluetoothSettingsSubPage(
                        settings = settings,
                        connectionStatus = connectionStatus,
                        bondedDevices = bondedDevices,
                        discoveredDevices = discoveredDevices,
                        isScanningBluetooth = isScanningBluetooth,
                        hidDiagnostics = hidDiagnostics,
                        onBack = { activeSubPage = SettingsSubPage.MAIN },
                        onRefreshBluetooth = onRefreshBluetooth,
                        onToggleBluetoothScan = onToggleBluetoothScan,
                        onCreateDiscoverableIntent = onCreateDiscoverableIntent,
                        onRenameBluetoothDevice = onRenameBluetoothDevice,
                        onPairAndConnectBluetooth = onPairAndConnectBluetooth,
                        onConnectDevice = onConnectDevice,
                        onDisconnectDevice = onDisconnectDevice,
                        onToggleFavoriteDevice = onToggleFavoriteDevice
                    )
                }

                SettingsSubPage.GOOGLE_ACCOUNT -> {
                    GoogleAccountDevicesSubPage(
                        settings = settings,
                        authSessionMode = authSessionMode,
                        userEmail = userEmail,
                        userDisplayName = userDisplayName,
                        localDeviceId = localDeviceId,
                        localWifiIp = localWifiIp,
                        accessibilityEnabled = accessibilityEnabled,
                        isReceiverRunning = isReceiverRunning,
                        connectedControllerName = connectedControllerName,
                        cloudDevices = cloudDevices,
                        connectionStatus = connectionStatus,
                        onBack = { activeSubPage = SettingsSubPage.MAIN },
                        onChangeAppRole = onChangeAppRole,
                        onUpdateLocalCloudName = onUpdateLocalCloudName,
                        onRefreshEnvironment = onRefreshEnvironment,
                        onConnectWifiReceiver = onConnectWifiReceiver,
                        onDisconnectDevice = onDisconnectDevice,
                        onOpenPermissionWizard = onOpenPermissionWizard,
                        onGoogleAccountLinked = onGoogleAccountLinked,
                        onSignOut = onSignOut
                    )
                }

                SettingsSubPage.MAIN -> {
                    // 1. Sub-page Entry Tiles: Bluetooth & Google Account + Devices + Working Mode
                    SettingsCard(
                        title = "BAĞLANTI, HESAP VE ÇALIŞMA MODU",
                        icon = Icons.Default.AccountCircle,
                        headerAction = {
                            Spacer(modifier = Modifier.width(42.dp))
                        }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsNavEntryTile(
                                icon = Icons.Default.Bluetooth,
                                title = "Bluetooth HID & Cihazlar",
                                subtitle = when (connectionStatus) {
                                    is ConnectionStatus.Connected -> "Bağlı: ${connectionStatus.deviceName}"
                                    else -> "Yayın adı: ${settings.customBluetoothName} • ${bondedDevices.size} eşleşmiş cihaz"
                                },
                                badgeColor = if (connectionStatus is ConnectionStatus.Connected && !connectionStatus.isWifi) GreenSuccess else RubyPrimary,
                                onClick = { activeSubPage = SettingsSubPage.BLUETOOTH },
                                testTag = "settings_nav_bluetooth"
                            )

                            SettingsNavEntryTile(
                                icon = Icons.Default.AccountCircle,
                                title = "Google Hesabı & Alıcı Cihazlar",
                                subtitle = if (authSessionMode == AuthSessionMode.GOOGLE && userEmail.isNotBlank()) {
                                    "$userEmail • ${cloudDevices.count { it.id != localDeviceId }} kayıtlı cihaz"
                                } else {
                                    "${authSessionMode.label} • Dokunarak Google hesabı bağlayın"
                                },
                                badgeColor = if (authSessionMode == AuthSessionMode.GOOGLE) GreenSuccess else RubyPrimary,
                                onClick = { activeSubPage = SettingsSubPage.GOOGLE_ACCOUNT },
                                testTag = "settings_nav_google"
                            )

                            ChoiceRow(
                                label = "Uygulama Çalışma Modu",
                                options = listOf(AppRole.CONTROLLER, AppRole.RECEIVER),
                                selected = settings.appRole,
                                optionLabel = { it.label },
                                onSelect = onChangeAppRole
                            )

                            SettingsNavEntryTile(
                                icon = Icons.Default.AccessibilityNew,
                                title = "İzinler & Android 13 Kısıtlı Ayar Kontrolü",
                                subtitle = if (accessibilityEnabled) {
                                    "Erişilebilirlik aktif • Alıcı ve imleç denetimi hazır"
                                } else {
                                    "Erişilebilirlik kapalı — Alıcı modda gerekli, açmak için dokunun"
                                },
                                badgeColor = if (accessibilityEnabled) GreenSuccess else RedDanger,
                                onClick = onOpenPermissionWizard,
                                testTag = "settings_open_permission_wizard"
                            )
                        }
                    }

                    // 2. TOUCHPAD & AIR MOUSE DÜZENİ VE ÖZELLEŞTİRME
                    SettingsCard(
                        title = "TOUCHPAD, HİZALAMA & DOCK ÖZELLEŞTİRME",
                        icon = Icons.Default.Dashboard
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SettingToggleRow(
                                label = "Birleşik Kontrol Modu (Touchpad + Air Mouse)",
                                description = "Touchpad içinden tek dokunuşla morf animasyonlu Air Mouse moduna geçer.",
                                checked = settings.unifiedControlMode,
                                onCheckedChange = onSetUnifiedControlMode
                            )

                            ChoiceRow(
                                label = "Bluetooth HID Profili",
                                options = listOf(HidProfileMode.PRECISION_TOUCHPAD, HidProfileMode.CLASSIC_MOUSE),
                                selected = settings.hidProfileMode,
                                optionLabel = {
                                    if (it == HidProfileMode.PRECISION_TOUCHPAD) "Precision Touchpad" else "Klasik + Ctrl Zoom"
                                },
                                onSelect = onChangeHidProfile
                            )
                            Text(
                                text = settings.hidProfileMode.description,
                                color = TextMuted,
                                fontSize = 9.sp
                            )

                            ChoiceRow(
                                label = "Dikey Kaydırma Tipi",
                                options = listOf(AirMouseScrollMode.SCROLL_STRIP, AirMouseScrollMode.STEP_BUTTONS),
                                selected = settings.airMouseScrollMode,
                                optionLabel = { it.label },
                                onSelect = { onUpdateSettings { s -> s.copy(airMouseScrollMode = it) } }
                            )

                            SettingToggleRow(
                                label = "F1 & F2 Ekstra Fare Tuşlarını Göster",
                                description = "Alt dock'ta özelleştirilebilir 4. ve 5. fare tuşları",
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
                                Text("Quick Tools Izgarası & Dock Sırasını Sıfırla", color = RubyPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            SettingToggleRow(
                                label = "Üst Yatay Kaydırma Rayı",
                                description = "Üst bölümde tırtıklı yatay galeri/sekme geçiş rayı gösterir",
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

                    // 3. TOUCHPAD VOLUME KEYS
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

                    // 4. AIR MOUSE VOLUME KEYS
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

                    // 5. HASSASIYET VE HAREKET AYARLARI
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
                }
            }

            Spacer(modifier = Modifier.height(92.dp))
        }
    }
}

@Composable
private fun SettingsNavEntryTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    badgeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = SurfaceContainerCharcoal,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorderRuby, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(badgeColor.copy(alpha = 0.14f), RoundedCornerShape(9.dp))
                    .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = badgeColor,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "$title aç",
                tint = RubyPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
internal fun SettingsCard(
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
internal fun <T> ChoiceRow(
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
