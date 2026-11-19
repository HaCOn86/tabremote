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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.PairedDevice
import com.example.model.AppRole
import com.example.model.AuthSessionMode
import com.example.model.ConnectionStatus
import com.example.model.NavDockItemId
import com.example.model.RemoteSettings
import com.example.model.SettingsSubPage
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.SwitchThumbChecked
import com.example.ui.theme.SwitchThumbUnchecked
import com.example.ui.theme.TabRemoteTheme

@Composable
fun SettingsScreen(
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
    bondedDevices: List<BluetoothDevice>,
    discoveredDevices: List<BluetoothDevice>,
    isScanningBluetooth: Boolean,
    hidDiagnostics: String?,
    isTabletLayout: Boolean = false,
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
    onPairAndConnectBluetooth: (BluetoothDevice) -> Unit,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onUnpairDevice: (BluetoothDevice) -> Unit = {},
    bluetoothDeviceTypeLabel: (BluetoothDevice) -> String = { "Bluetooth Cihazı" },
    onDisconnectDevice: () -> Unit,
    onToggleFavoriteDevice: (address: String) -> Unit,
    onMoveNavDockItem: (index: Int, delta: Int) -> Unit = { _, _ -> },
    onMoveNavDockItemTo: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onToggleNavDockHidden: (NavDockItemId) -> Unit = {},
    onResetDockCustomizations: () -> Unit,
    pendingSubPage: SettingsSubPage? = null,
    onConsumePendingSubPage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    var activeSubPage by remember { mutableStateOf(SettingsSubPage.MAIN) }
    LaunchedEffect(pendingSubPage) {
        if (pendingSubPage != null) {
            activeSubPage = pendingSubPage
            onConsumePendingSubPage()
        }
    }
    val showReceiverCursorSettings = settings.appRole == AppRole.RECEIVER

    val detailPage = when {
        activeSubPage == SettingsSubPage.MAIN -> SettingsSubPage.GOOGLE_ACCOUNT
        activeSubPage == SettingsSubPage.CURSOR && !showReceiverCursorSettings -> SettingsSubPage.TOUCHPAD_LAYOUT
        else -> activeSubPage
    }

    @Composable
    fun renderDetailSubPage(page: SettingsSubPage, showBack: Boolean) {
        when (page) {
            SettingsSubPage.BLUETOOTH -> {
                BluetoothSettingsSubPage(
                    settings = settings,
                    connectionStatus = connectionStatus,
                    bondedDevices = bondedDevices,
                    discoveredDevices = discoveredDevices,
                    isScanningBluetooth = isScanningBluetooth,
                    hidDiagnostics = hidDiagnostics,
                    deviceTypeLabel = bluetoothDeviceTypeLabel,
                    onBack = { activeSubPage = SettingsSubPage.MAIN },
                    onRefreshBluetooth = onRefreshBluetooth,
                    onToggleBluetoothScan = onToggleBluetoothScan,
                    onCreateDiscoverableIntent = onCreateDiscoverableIntent,
                    onPairAndConnectBluetooth = onPairAndConnectBluetooth,
                    onConnectDevice = onConnectDevice,
                    onUnpairDevice = onUnpairDevice,
                    onDisconnectDevice = onDisconnectDevice,
                    onToggleFavoriteDevice = onToggleFavoriteDevice,
                    showBackButton = showBack
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
                    notificationListenerEnabled = notificationListenerEnabled,
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
                    onSignOut = onSignOut,
                    showBackButton = showBack
                )
            }

            SettingsSubPage.THEME -> {
                ThemeSettingsSubPage(
                    settings = settings,
                    onBack = { activeSubPage = SettingsSubPage.MAIN },
                    onUpdateSettings = onUpdateSettings,
                    showBackButton = showBack
                )
            }

            SettingsSubPage.SENSITIVITY -> {
                SensitivitySettingsSubPage(
                    settings = settings,
                    onBack = { activeSubPage = SettingsSubPage.MAIN },
                    onUpdateSettings = onUpdateSettings,
                    showBackButton = showBack
                )
            }

            SettingsSubPage.VOLUME_KEYS -> {
                VolumeKeysSettingsSubPage(
                    settings = settings,
                    onBack = { activeSubPage = SettingsSubPage.MAIN },
                    onUpdateSettings = onUpdateSettings,
                    showBackButton = showBack
                )
            }

            SettingsSubPage.CURSOR -> {
                CursorSettingsSubPage(
                    settings = settings,
                    accessibilityEnabled = accessibilityEnabled,
                    onBack = { activeSubPage = SettingsSubPage.MAIN },
                    onUpdateSettings = onUpdateSettings,
                    onOpenPermissionWizard = onOpenPermissionWizard,
                    showBackButton = showBack
                )
            }

            SettingsSubPage.TOUCHPAD_LAYOUT -> {
                TouchpadLayoutSettingsSubPage(
                    settings = settings,
                    onBack = { activeSubPage = SettingsSubPage.MAIN },
                    onUpdateSettings = onUpdateSettings,
                    onMoveNavDockItem = onMoveNavDockItem,
                    onMoveNavDockItemTo = onMoveNavDockItemTo,
                    onToggleNavDockHidden = onToggleNavDockHidden,
                    onResetDockCustomizations = onResetDockCustomizations,
                    showBackButton = showBack
                )
            }

            SettingsSubPage.MAIN -> Unit
        }
    }

    @Composable
    fun renderMainCategoryList(highlightedPage: SettingsSubPage?) {
        SettingsCard(
            title = "BAĞLANTI & HESAP",
            icon = Icons.Default.AccountCircle,
            headerAction = {
                Spacer(modifier = Modifier.width(42.dp))
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsNavEntryTile(
                    icon = Icons.Default.AccountCircle,
                    title = "Google Hesabı & Alıcı Cihazlar",
                    subtitle = if (authSessionMode == AuthSessionMode.GOOGLE && userEmail.isNotBlank()) {
                        "$userEmail • ${settings.appRole.label}"
                    } else {
                        "${authSessionMode.label} • Çalışma modu & alıcı kurulumu"
                    },
                    badgeColor = if (authSessionMode == AuthSessionMode.GOOGLE) GreenSuccess else palette.primary,
                    selected = highlightedPage == SettingsSubPage.GOOGLE_ACCOUNT,
                    onClick = { activeSubPage = SettingsSubPage.GOOGLE_ACCOUNT },
                    testTag = "settings_nav_google"
                )

                SettingsNavEntryTile(
                    icon = Icons.Default.Bluetooth,
                    title = "Bluetooth HID & Cihazlar",
                    subtitle = when (connectionStatus) {
                        is ConnectionStatus.Connected -> "Bağlı: ${connectionStatus.deviceName}"
                        else -> "Fare + Klavye HID • ${bondedDevices.size} eşleşmiş cihaz"
                    },
                    badgeColor = if (connectionStatus is ConnectionStatus.Connected && !connectionStatus.isWifi) {
                        GreenSuccess
                    } else palette.primary,
                    selected = highlightedPage == SettingsSubPage.BLUETOOTH,
                    onClick = { activeSubPage = SettingsSubPage.BLUETOOTH },
                    testTag = "settings_nav_bluetooth"
                )
            }
        }

        SettingsCard(
            title = "GÖRÜNÜM, HASSASİYET & TOUCHPAD",
            icon = Icons.Default.Tune
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsNavEntryTile(
                    icon = Icons.Default.Dashboard,
                    title = "Touchpad, Hizalama & Dock",
                    subtitle = "${settings.horizontalPanPosition.label} • ${settings.scrollSide.label}",
                    badgeColor = palette.primary,
                    selected = highlightedPage == SettingsSubPage.TOUCHPAD_LAYOUT,
                    onClick = { activeSubPage = SettingsSubPage.TOUCHPAD_LAYOUT },
                    testTag = "settings_nav_touchpad_layout"
                )

                if (showReceiverCursorSettings) {
                    SettingsNavEntryTile(
                        icon = Icons.Default.Mouse,
                        title = "Alıcı İmleç Ayarları",
                        subtitle = "${"%.1f".format(settings.cursorScale)}x • ${settings.cursorContrastMode.label} kontrast",
                        badgeColor = Color(settings.cursorColorArgb),
                        selected = highlightedPage == SettingsSubPage.CURSOR,
                        onClick = { activeSubPage = SettingsSubPage.CURSOR },
                        testTag = "settings_nav_cursor"
                    )
                }

                SettingsNavEntryTile(
                    icon = Icons.Default.Palette,
                    title = "Tema & Renk Paleti",
                    subtitle = if (settings.useDynamicColor) {
                        "Sistem Dinamik Rengi (Material You) aktif"
                    } else {
                        "Aktif palet: ${settings.themePreset.label}"
                    },
                    badgeColor = palette.primary,
                    selected = highlightedPage == SettingsSubPage.THEME,
                    onClick = { activeSubPage = SettingsSubPage.THEME },
                    testTag = "settings_nav_theme"
                )

                SettingsNavEntryTile(
                    icon = Icons.Default.Tune,
                    title = "Hassasiyet & Kalibrasyon",
                    subtitle = "Touchpad (${"%.1f".format(settings.pointerSensitivity)}x) • Air Mouse (${"%.1f".format(settings.airMouseSensitivity)}x)",
                    badgeColor = palette.primary,
                    selected = highlightedPage == SettingsSubPage.SENSITIVITY,
                    onClick = { activeSubPage = SettingsSubPage.SENSITIVITY },
                    testTag = "settings_nav_sensitivity"
                )

                SettingsNavEntryTile(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Ses Tuşları",
                    subtitle = "Touchpad & Air Mouse ses açma/kısma kısayolları",
                    badgeColor = palette.primary,
                    selected = highlightedPage == SettingsSubPage.VOLUME_KEYS,
                    onClick = { activeSubPage = SettingsSubPage.VOLUME_KEYS },
                    testTag = "settings_nav_volume_keys"
                )
            }
        }

        Surface(
            color = palette.shadedPanel,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.borderAccent, RoundedCornerShape(14.dp))
                .testTag("settings_info_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(palette.primary.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                        .border(1.dp, palette.primary.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "İpucu ve Bilgi",
                        tint = palette.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "ÖZELLEŞTİRME İPUCU",
                        color = palette.primary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "Alt navigasyon çubuğunu 'Touchpad, Hizalama & Dock' altından; Akıllı Klavye ve Wi-Fi panellerini ise 'Düzenle' butonlarıyla 2D serbest ızgarada özelleştirebilirsiniz.",
                        color = palette.textSecondary,
                        fontSize = 10.5.sp,
                        lineHeight = 14.5.sp
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = palette.surface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .border(1.dp, palette.borderAccent, RoundedCornerShape(20.dp))
                    .testTag("settings_version_badge")
            ) {
                Text(
                    text = "TabRemote v${BuildConfig.VERSION_NAME}",
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                )
            }
        }
    }

    if (isTabletLayout) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.86f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                renderMainCategoryList(highlightedPage = detailPage)
            }

            Surface(
                color = palette.shadedPanel,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1.14f)
                    .fillMaxHeight()
                    .border(1.dp, palette.borderSubtle, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    renderDetailSubPage(page = detailPage, showBack = false)
                }
            }
        }
        return
    }

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
            if (page == SettingsSubPage.MAIN) {
                renderMainCategoryList(highlightedPage = null)
            } else {
                renderDetailSubPage(page = page, showBack = true)
            }

            Spacer(modifier = Modifier.height(92.dp))
        }
    }
}

@Composable
internal fun SettingsNavEntryTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeColor: Color,
    onClick: () -> Unit,
    testTag: String,
    selected: Boolean = false
) {
    val palette = TabRemoteTheme.palette
    val shape = RoundedCornerShape(12.dp)
    Surface(
        color = if (selected) palette.primary.copy(alpha = 0.14f) else palette.surfaceContainer,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .border(1.dp, if (selected) palette.primary else palette.borderAccent, shape)
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
                    .background(badgeColor.copy(alpha = 0.16f), RoundedCornerShape(9.dp))
                    .border(1.dp, badgeColor.copy(alpha = 0.48f), RoundedCornerShape(9.dp)),
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
                    color = palette.textPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = palette.textSecondary,
                    fontSize = 10.sp
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "$title aç",
                tint = palette.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
internal fun SettingsCard(
    title: String,
    icon: ImageVector,
    headerAction: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val palette = TabRemoteTheme.palette
    val shape = RoundedCornerShape(14.dp)
    Surface(
        color = palette.surface,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.borderAccent, shape)
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
                        tint = palette.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = palette.textPrimary,
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

@Composable
internal fun <T> ChoiceRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    val palette = TabRemoteTheme.palette
    Column {
        Text(text = label, color = palette.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                val shape = RoundedCornerShape(8.dp)
                Surface(
                    color = if (isSelected) palette.primary else palette.surfaceContainer,
                    shape = shape,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .clip(shape)
                        .border(
                            1.dp,
                            if (isSelected) palette.primary else palette.borderAccent,
                            shape
                        )
                        .clickable { onSelect(option) },
                    contentColor = if (isSelected) palette.onAccent else palette.textSecondary
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = optionLabel(option),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = TabRemoteTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = label, color = palette.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(text = description, color = palette.textSecondary, fontSize = 9.5.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SwitchThumbChecked,
                checkedTrackColor = palette.primary,
                uncheckedThumbColor = SwitchThumbUnchecked,
                uncheckedTrackColor = palette.surfaceContainer,
                uncheckedBorderColor = palette.borderAccent
            )
        )
    }
}
