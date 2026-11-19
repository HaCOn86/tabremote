package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.PairedDevice
import com.example.model.AppRole
import com.example.model.ConnectionStatus
import com.example.model.ReceiverTelemetry
import com.example.model.RemoteSettings
import com.example.model.SparseGridEngine
import com.example.model.WifiQuickWidgetId
import com.example.model.WifiWidgetPlacement
import com.example.ui.components.ReceiverLockedControlContainer
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TabRemoteTheme
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Unified "Medya & Panel" tab:
 * - Strictly targets the connected remote device (never shows local phone telemetry/media).
 * - Locks interactive media/volume/Quick Panel controls when disconnected or in Receiver role.
 * - QuickConnectPeersBar stays interactive when disconnected and Wi-Fi receiver peers exist.
 */
@Composable
fun MediaPanelScreen(
    settings: RemoteSettings,
    connectionStatus: ConnectionStatus,
    receiverTelemetry: ReceiverTelemetry,
    cloudDevices: List<PairedDevice>,
    localDeviceId: String,
    isTabletLayout: Boolean,
    onConnectWifiReceiver: (PairedDevice) -> Unit,
    onOpenPermissionWizard: () -> Unit,
    onMediaAction: (String) -> Unit,
    onSendWifiWidget: (WifiQuickWidgetId) -> Unit,
    onSetVolumePercent: (Int) -> Unit,
    onPlaceWifiWidgetAt: (WifiQuickWidgetId, Int, Int) -> Unit,
    onResizeWifiWidget: (WifiQuickWidgetId) -> Unit,
    onRemoveWifiWidget: (WifiQuickWidgetId) -> Unit,
    onAddWifiWidget: (WifiQuickWidgetId) -> Unit,
    modifier: Modifier = Modifier
) {
    val isWifiConnected = (connectionStatus as? ConnectionStatus.Connected)?.isWifi == true
    val isBtConnected = (connectionStatus as? ConnectionStatus.Connected)?.isWifi == false
    val hasActiveRemote = isWifiConnected || isBtConnected
    val connectedDeviceName = (connectionStatus as? ConnectionStatus.Connected)?.deviceName.orEmpty()
    val isReceiverRole = settings.appRole == AppRole.RECEIVER
    val controlsEnabled = hasActiveRemote && !isReceiverRole

    val receiverPeers = remember(cloudDevices, localDeviceId, isReceiverRole) {
        if (isReceiverRole) emptyList()
        else cloudDevices.filter { it.id != localDeviceId && it.role == PairedDevice.ROLE_RECEIVER }
    }

    val remoteDisplayTelemetry = if (isWifiConnected) receiverTelemetry else ReceiverTelemetry()

    val mediaCardsContent: @Composable () -> Unit = {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Separate Row: Battery Indicator Card & Active Foreground App Card (remote only)
            BatteryAndActiveAppRow(
                telemetry = remoteDisplayTelemetry,
                isWifiConnected = isWifiConnected
            )

            // Android 14/15 Expressive Material Media Player with Wavy Progress Bar
            AndroidMaterialMediaPlayerCard(
                telemetry = remoteDisplayTelemetry,
                isWifiConnected = isWifiConnected,
                isBtConnected = isBtConnected,
                connectedDeviceName = connectedDeviceName,
                controlsEnabled = controlsEnabled,
                onOpenPermissionWizard = onOpenPermissionWizard,
                onMediaAction = onMediaAction
            )

            // Separate Volume Slider + Mute Card
            VolumeControlCard(
                telemetry = remoteDisplayTelemetry,
                controlsEnabled = controlsEnabled,
                onMuteToggle = { onSendWifiWidget(WifiQuickWidgetId.MUTE) },
                onSetVolumePercent = onSetVolumePercent
            )
        }
    }

    if (isTabletLayout) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = 4.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isWifiConnected && receiverPeers.isNotEmpty() && !isReceiverRole) {
                QuickConnectPeersBar(
                    receiverPeers = receiverPeers,
                    onConnectWifiReceiver = onConnectWifiReceiver
                )
            }

            RemotePanelLockWrapper(
                isReceiverRole = isReceiverRole,
                hasActiveRemote = hasActiveRemote,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        mediaCardsContent()
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MediaQuickPanelCard(
                            placements = settings.wifiControlPlacements,
                            enabled = controlsEnabled,
                            onSendWifiWidget = onSendWifiWidget,
                            onPlaceWifiWidgetAt = onPlaceWifiWidgetAt,
                            onResizeWifiWidget = onResizeWifiWidget,
                            onRemoveWifiWidget = onRemoveWifiWidget,
                            onAddWifiWidget = onAddWifiWidget
                        )
                    }
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 4.dp, bottom = 86.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!isWifiConnected && receiverPeers.isNotEmpty() && !isReceiverRole) {
                QuickConnectPeersBar(
                    receiverPeers = receiverPeers,
                    onConnectWifiReceiver = onConnectWifiReceiver
                )
            }

            RemotePanelLockWrapper(
                isReceiverRole = isReceiverRole,
                hasActiveRemote = hasActiveRemote,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    mediaCardsContent()
                    MediaQuickPanelCard(
                        placements = settings.wifiControlPlacements,
                        enabled = controlsEnabled,
                        onSendWifiWidget = onSendWifiWidget,
                        onPlaceWifiWidgetAt = onPlaceWifiWidgetAt,
                        onResizeWifiWidget = onResizeWifiWidget,
                        onRemoveWifiWidget = onRemoveWifiWidget,
                        onAddWifiWidget = onAddWifiWidget
                    )
                }
            }
        }
    }
}

@Composable
private fun RemotePanelLockWrapper(
    isReceiverRole: Boolean,
    hasActiveRemote: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = TabRemoteTheme.palette
    if (isReceiverRole) {
        ReceiverLockedControlContainer(
            isLockedForReceiver = true,
            modifier = modifier
        ) {
            content()
        }
    } else if (!hasActiveRemote) {
        Box(modifier = modifier) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0.38f)
            ) {
                content()
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(20f)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                    .testTag("media_panel_disconnected_lock"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = palette.surfaceContainer.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .border(1.2.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(palette.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LinkOff,
                                contentDescription = "Bağlı Cihaz Yok",
                                tint = palette.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Bağlı Cihaz Yok",
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Medya ve hızlı kontrol panelini kullanmak için yukarıdan bir Wi-Fi alıcıya veya Bluetooth cihazına bağlanın.",
                                color = palette.textSecondary,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}

@Composable
private fun QuickConnectPeersBar(
    receiverPeers: List<PairedDevice>,
    onConnectWifiReceiver: (PairedDevice) -> Unit
) {
    val palette = TabRemoteTheme.palette
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        receiverPeers.take(2).forEach { peer ->
            Surface(
                color = palette.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.borderSubtle, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = peer.deviceName,
                            color = palette.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Wi-Fi Alıcı • ${peer.localIp.ifBlank { "IP bekleniyor" }}",
                            color = palette.textSecondary,
                            fontSize = 9.5.sp
                        )
                    }
                    Button(
                        onClick = { onConnectWifiReceiver(peer) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onAccent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("wifi_quick_connect_${peer.id}")
                    ) {
                        Text("Bağlan", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryAndActiveAppRow(
    telemetry: ReceiverTelemetry,
    isWifiConnected: Boolean
) {
    val palette = TabRemoteTheme.palette
    val batteryColor = when {
        telemetry.isBatteryCharging -> GreenSuccess
        telemetry.batteryPercent in 1..20 -> RedDanger
        else -> palette.primary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Battery Card
        Surface(
            color = palette.surface,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(0.42f)
                .border(1.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
                .testTag("media_battery_card")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(batteryColor.copy(alpha = 0.16f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (telemetry.isBatteryCharging) {
                            Icons.Default.BatteryChargingFull
                        } else {
                            Icons.Default.BatteryStd
                        },
                        contentDescription = "Pil Durumu",
                        tint = batteryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "BATARYA",
                        color = palette.textMuted,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = if (isWifiConnected && telemetry.batteryPercent >= 0) {
                            "%${telemetry.batteryPercent}" + if (telemetry.isBatteryCharging) " ⚡" else ""
                        } else {
                            "—"
                        },
                        color = palette.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Active App Card (separate from Media Player)
        Surface(
            color = palette.surface,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(0.58f)
                .border(1.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
                .testTag("media_active_app_card")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(palette.primary.copy(alpha = 0.16f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = "Açık Uygulama",
                        tint = palette.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AÇIK UYGULAMA",
                        color = palette.textMuted,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = if (isWifiConnected) {
                            telemetry.activeAppName.ifBlank {
                                telemetry.activeAppPackage.ifBlank { "Ana Ekran" }
                            }
                        } else {
                            "Wi-Fi Alıcı Bekleniyor"
                        },
                        color = palette.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AndroidMaterialMediaPlayerCard(
    telemetry: ReceiverTelemetry,
    isWifiConnected: Boolean,
    isBtConnected: Boolean,
    connectedDeviceName: String,
    controlsEnabled: Boolean,
    onOpenPermissionWizard: () -> Unit,
    onMediaAction: (String) -> Unit
) {
    val palette = TabRemoteTheme.palette
    val cardShape = RoundedCornerShape(20.dp)
    val durationMs = telemetry.mediaDurationMs.coerceAtLeast(0L)
    val positionMs = telemetry.mediaPositionMs.coerceIn(0L, durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE)
    val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "media_wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )
    val waveAmplitudeFraction by animateFloatAsState(
        targetValue = if (telemetry.isMediaPlaying) 1f else 0f,
        animationSpec = tween(260),
        label = "wave_amp"
    )

    val sourceBadge = when {
        isWifiConnected -> "Wi-Fi Canlı • ${connectedDeviceName.ifBlank { "Alıcı" }}"
        isBtConnected -> "Bluetooth HID • ${connectedDeviceName.ifBlank { "Cihaz" }}"
        else -> "Bağlı Değil"
    }

    Surface(
        color = palette.surfaceContainer,
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.borderElevated, cardShape)
            .testTag("android_media_player_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.primary.copy(alpha = 0.08f))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(palette.primary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Medya Oynatıcı",
                                tint = palette.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = if (isWifiConnected) {
                                telemetry.mediaAppName.ifBlank { "Alıcı Medya Oynatıcı" }
                            } else {
                                "Uzak Medya Kontrolü"
                            },
                            color = palette.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        color = palette.surface,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.border(1.dp, palette.borderSubtle, RoundedCornerShape(999.dp))
                    ) {
                        Text(
                            text = sourceBadge,
                            color = if (isWifiConnected || isBtConnected) GreenSuccess else palette.textMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                !isWifiConnected && isBtConnected -> "Bluetooth Uzaktan Medya"
                                telemetry.mediaTitle.isNotBlank() -> telemetry.mediaTitle
                                else -> "Bağlı cihazda çalan medya yok"
                            },
                            color = palette.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                !isWifiConnected && isBtConnected -> "Pil ve parça adı için Wi-Fi Alıcı gerekir"
                                telemetry.mediaArtist.isNotBlank() -> telemetry.mediaArtist
                                telemetry.isMediaPlaying -> "Oynatılıyor"
                                else -> "Duraklatıldı / Beklemede"
                            },
                            color = palette.textSecondary,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        color = palette.primary,
                        shape = RoundedCornerShape(if (telemetry.isMediaPlaying) 14.dp else 24.dp),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(if (telemetry.isMediaPlaying) 14.dp else 24.dp))
                            .clickable(enabled = controlsEnabled) { onMediaAction("PLAY_PAUSE") }
                            .testTag("media_player_play_pause")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (telemetry.isMediaPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Oynat / Duraklat",
                                tint = palette.onAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { onMediaAction("PREV") },
                        enabled = controlsEnabled,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("media_player_prev")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Önceki Parça",
                            tint = if (controlsEnabled) palette.textPrimary else palette.textMuted
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                        ) {
                            val centerY = size.height / 2f
                            val width = size.width.coerceAtLeast(1f)
                            val playedEnd = (width * progress).coerceIn(0f, width)
                            val inactiveColor = palette.borderElevated
                            val activeColor = palette.primary

                            if (playedEnd < width) {
                                drawLine(
                                    color = inactiveColor,
                                    start = Offset(playedEnd, centerY),
                                    end = Offset(width, centerY),
                                    strokeWidth = 3.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }

                            if (playedEnd > 2f) {
                                val amp = 3.2.dp.toPx() * waveAmplitudeFraction
                                val wavelengthPx = 24.dp.toPx().coerceAtLeast(12f)
                                val path = Path()
                                var x = 0f
                                val step = 3f
                                while (x <= playedEnd) {
                                    val angle = (x / wavelengthPx) * 2f * PI.toFloat() - wavePhase
                                    val y = centerY + sin(angle) * amp
                                    if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                                    x += step
                                }
                                drawPath(
                                    path = path,
                                    color = activeColor,
                                    style = Stroke(
                                        width = 3.2.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            drawCircle(
                                color = activeColor,
                                radius = 5.dp.toPx(),
                                center = Offset(playedEnd, centerY)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatMediaTimestamp(positionMs),
                                color = palette.textMuted,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (durationMs > 0L) formatMediaTimestamp(durationMs) else "--:--",
                                color = palette.textMuted,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    IconButton(
                        onClick = { onMediaAction("NEXT") },
                        enabled = controlsEnabled,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("media_player_next")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Sonraki Parça",
                            tint = if (controlsEnabled) palette.textPrimary else palette.textMuted
                        )
                    }
                }

                if (isWifiConnected && !telemetry.notificationListenerEnabled) {
                    Surface(
                        color = RedDanger.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, RedDanger.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .clickable { onOpenPermissionWizard() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RedDanger, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Alıcıda Bildirim Dinleme kapalı — Tam şarkı bilgisi için dokunun",
                                color = RedDanger,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatMediaTimestamp(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000L
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}

@Composable
private fun VolumeControlCard(
    telemetry: ReceiverTelemetry,
    controlsEnabled: Boolean,
    onMuteToggle: () -> Unit,
    onSetVolumePercent: (Int) -> Unit
) {
    val palette = TabRemoteTheme.palette
    var sliderPos by remember(telemetry.volumePercent) {
        mutableFloatStateOf(telemetry.volumePercent.toFloat())
    }

    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(16.dp))
            .testTag("media_volume_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onMuteToggle,
                enabled = controlsEnabled,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("wifi_volume_mute_btn")
            ) {
                Icon(
                    imageVector = if (telemetry.isMuted || sliderPos <= 1f) {
                        Icons.AutoMirrored.Filled.VolumeMute
                    } else {
                        Icons.AutoMirrored.Filled.VolumeUp
                    },
                    contentDescription = "Sessiz / Aç",
                    tint = if (telemetry.isMuted) RedDanger else palette.textPrimary
                )
            }
            Slider(
                value = sliderPos,
                onValueChange = { if (controlsEnabled) sliderPos = it },
                onValueChangeFinished = { if (controlsEnabled) onSetVolumePercent(sliderPos.roundToInt()) },
                enabled = controlsEnabled,
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = palette.primary,
                    activeTrackColor = palette.primary,
                    inactiveTrackColor = palette.borderElevated
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("wifi_volume_slider")
            )
            Surface(
                color = palette.surfaceContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.border(1.dp, palette.borderSubtle, RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = "%${sliderPos.roundToInt()}",
                    color = palette.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MediaQuickPanelCard(
    placements: List<WifiWidgetPlacement>,
    enabled: Boolean,
    onSendWifiWidget: (WifiQuickWidgetId) -> Unit,
    onPlaceWifiWidgetAt: (WifiQuickWidgetId, Int, Int) -> Unit,
    onResizeWifiWidget: (WifiQuickWidgetId) -> Unit,
    onRemoveWifiWidget: (WifiQuickWidgetId) -> Unit,
    onAddWifiWidget: (WifiQuickWidgetId) -> Unit
) {
    val palette = TabRemoteTheme.palette
    var isEditMode by remember { mutableStateOf(false) }
    var draggedPlacement by remember { mutableStateOf<WifiWidgetPlacement?>(null) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    var ghostCol by remember { mutableIntStateOf(0) }
    var ghostRow by remember { mutableIntStateOf(0) }

    val baseRows = remember(placements) {
        (placements.maxOfOrNull { it.row + it.rowSpan } ?: 1).coerceAtLeast(1)
    }
    val totalRows = if (isEditMode) {
        maxOf(baseRows + 1, ghostRow + (draggedPlacement?.rowSpan ?: 1))
    } else {
        baseRows
    }

    val latestOnPlaceAt by rememberUpdatedState(onPlaceWifiWidgetAt)
    val activeWidgets = remember(placements) { placements.map { it.widget }.toSet() }
    val poolWidgets = remember(activeWidgets) { WifiQuickWidgetId.entries.filter { it !in activeWidgets } }

    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(16.dp))
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy))
            .testTag("wifi_sparse_panel_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditMode) "SÜRÜKLE • ÇİFT TIKLA BOYUTLANDIR" else "HIZLI KONTROL PANELİ",
                    color = if (isEditMode) palette.primary else palette.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                if (isEditMode) {
                    Surface(
                        color = palette.primary,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                isEditMode = false
                                draggedPlacement = null
                                dragDelta = Offset.Zero
                            }
                            .testTag("wifi_grid_done_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Bitti", tint = palette.onAccent, modifier = Modifier.size(12.dp))
                            Text("Bitti", color = palette.onAccent, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                } else {
                    Surface(
                        color = palette.surfaceContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, palette.borderElevated, RoundedCornerShape(8.dp))
                            .clickable(enabled = enabled) { isEditMode = true }
                            .testTag("wifi_grid_edit_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = palette.textPrimary, modifier = Modifier.size(11.dp))
                            Text("Düzenle", color = palette.textPrimary, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wifi_sparse_grid")
            ) {
                val density = LocalDensity.current
                val gapDp = 6.dp
                val cellHeightDp = 46.dp
                val gapPx = with(density) { gapDp.toPx() }
                val cellHeightPx = with(density) { cellHeightDp.toPx() }
                val totalWidthPx = with(density) { maxWidth.toPx() }
                val cellWidthPx = ((totalWidthPx - gapPx * (SparseGridEngine.GRID_COLUMNS - 1)) /
                    SparseGridEngine.GRID_COLUMNS).coerceAtLeast(10f)
                val totalRowsEffective = totalRows.coerceAtLeast(1)
                val gridHeightDp = with(density) {
                    (totalRowsEffective * cellHeightPx + (totalRowsEffective - 1) * gapPx).toDp()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeightDp)
                ) {
                    if (isEditMode) {
                        val dashedStroke = remember {
                            Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                        }
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            for (r in 0 until totalRowsEffective) {
                                for (c in 0 until SparseGridEngine.GRID_COLUMNS) {
                                    val x = c * (cellWidthPx + gapPx)
                                    val y = r * (cellHeightPx + gapPx)
                                    drawRoundRect(
                                        color = palette.borderSubtle.copy(alpha = 0.55f),
                                        topLeft = Offset(x, y),
                                        size = Size(cellWidthPx, cellHeightPx),
                                        cornerRadius = CornerRadius(14f, 14f),
                                        style = Stroke(width = 1.5f)
                                    )
                                }
                            }
                            val activeDrag = draggedPlacement
                            if (activeDrag != null) {
                                val gx = ghostCol * (cellWidthPx + gapPx)
                                val gy = ghostRow * (cellHeightPx + gapPx)
                                val gw = activeDrag.colSpan * cellWidthPx + (activeDrag.colSpan - 1) * gapPx
                                val gh = activeDrag.rowSpan * cellHeightPx + (activeDrag.rowSpan - 1) * gapPx
                                drawRoundRect(
                                    color = palette.primary.copy(alpha = 0.16f),
                                    topLeft = Offset(gx, gy),
                                    size = Size(gw, gh),
                                    cornerRadius = CornerRadius(18f, 18f)
                                )
                                drawRoundRect(
                                    color = palette.primary,
                                    topLeft = Offset(gx, gy),
                                    size = Size(gw, gh),
                                    cornerRadius = CornerRadius(18f, 18f),
                                    style = dashedStroke
                                )
                            }
                        }
                    }

                    placements.forEach { placed ->
                        val widget = placed.widget
                        val isBeingDragged = draggedPlacement?.widget == widget
                        val baseX = placed.col * (cellWidthPx + gapPx)
                        val baseY = placed.row * (cellHeightPx + gapPx)
                        val wPx = placed.colSpan * cellWidthPx + (placed.colSpan - 1) * gapPx
                        val hPx = placed.rowSpan * cellHeightPx + (placed.rowSpan - 1) * gapPx

                        val animX by animateDpAsState(
                            targetValue = with(density) { baseX.toDp() },
                            animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMediumLow),
                            label = "wifi_x_${widget.name}"
                        )
                        val animY by animateDpAsState(
                            targetValue = with(density) { baseY.toDp() },
                            animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMediumLow),
                            label = "wifi_y_${widget.name}"
                        )
                        val tileScale by animateFloatAsState(
                            targetValue = if (isBeingDragged) 1.05f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "wifi_scale_${widget.name}"
                        )

                        val offsetModifier = if (isBeingDragged) {
                            Modifier.offset {
                                IntOffset(
                                    (baseX + dragDelta.x).roundToInt(),
                                    (baseY + dragDelta.y).roundToInt()
                                )
                            }
                        } else {
                            Modifier.offset(x = animX, y = animY)
                        }

                        val stepX = (cellWidthPx + gapPx).coerceAtLeast(1f)
                        val stepY = (cellHeightPx + gapPx).coerceAtLeast(1f)
                        val maxCol = (SparseGridEngine.GRID_COLUMNS - placed.colSpan).coerceAtLeast(0)
                        val maxRow = (baseRows + 1).coerceAtLeast(1)

                        val editGesturesMod = if (isEditMode) {
                            Modifier
                                .pointerInput(placed) {
                                    detectTapGestures(
                                        onDoubleTap = { onResizeWifiWidget(widget) }
                                    )
                                }
                                .pointerInput(placed, maxCol, maxRow, stepX, stepY) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggedPlacement = placed
                                            dragDelta = Offset.Zero
                                            ghostCol = placed.col
                                            ghostRow = placed.row
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragDelta += dragAmount
                                            val currLeft = baseX + dragDelta.x
                                            val currTop = baseY + dragDelta.y
                                            ghostCol = (currLeft / stepX).roundToInt().coerceIn(0, maxCol)
                                            ghostRow = (currTop / stepY).roundToInt().coerceIn(0, maxRow)
                                        },
                                        onDragEnd = {
                                            val tc = ghostCol
                                            val tr = ghostRow
                                            draggedPlacement = null
                                            dragDelta = Offset.Zero
                                            latestOnPlaceAt(widget, tc, tr)
                                        },
                                        onDragCancel = {
                                            draggedPlacement = null
                                            dragDelta = Offset.Zero
                                        }
                                    )
                                }
                        } else {
                            Modifier
                        }

                        Box(
                            modifier = offsetModifier
                                .size(width = with(density) { wPx.toDp() }, height = with(density) { hPx.toDp() })
                                .zIndex(if (isBeingDragged) 20f else 1f)
                                .scale(tileScale)
                                .then(editGesturesMod)
                        ) {
                            val tileShape = RoundedCornerShape(10.dp)
                            val icon = wifiWidgetIcon(widget)
                            Surface(
                                color = if (enabled || isEditMode) palette.surfaceContainer else palette.surface,
                                shape = tileShape,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(tileShape)
                                    .border(
                                        1.dp,
                                        if (isEditMode) palette.borderElevated else palette.borderSubtle,
                                        tileShape
                                    )
                                    .then(
                                        if (!isEditMode) {
                                            Modifier.clickable(enabled = enabled) { onSendWifiWidget(widget) }
                                        } else Modifier
                                    )
                                    .testTag("wifi_widget_${widget.name.lowercase()}")
                            ) {
                                if (placed.colSpan >= 2) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = widget.label,
                                            tint = if (enabled) palette.textPrimary else palette.textMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = widget.label,
                                                color = if (enabled) palette.textPrimary else palette.textMuted,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = widget.shortcutHint,
                                                color = palette.textMuted,
                                                fontSize = 7.5.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                } else {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = widget.label,
                                            tint = if (enabled) palette.textPrimary else palette.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (isEditMode) {
                                Surface(
                                    color = palette.primary,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, palette.onAccent.copy(alpha = 0.7f), CircleShape)
                                        .clickable { onRemoveWifiWidget(widget) }
                                        .testTag("wifi_remove_${widget.name.lowercase()}")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Remove, contentDescription = "Kaldır", tint = palette.onAccent, modifier = Modifier.size(11.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isEditMode && poolWidgets.isNotEmpty()) {
                Text(
                    text = "EKLENEBİLİR PANEL BİLEŞENLERİ",
                    color = palette.textMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    poolWidgets.forEach { widget ->
                        Surface(
                            color = palette.surfaceContainer,
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .border(1.dp, palette.borderElevated, RoundedCornerShape(9.dp))
                                .clickable { onAddWifiWidget(widget) }
                                .testTag("wifi_add_${widget.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Ekle", tint = palette.primary, modifier = Modifier.size(13.dp))
                                Icon(wifiWidgetIcon(widget), contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(13.dp))
                                Text(widget.label, color = palette.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun wifiWidgetIcon(widget: WifiQuickWidgetId): ImageVector = when (widget) {
    WifiQuickWidgetId.PLAY_PAUSE -> Icons.Default.PlayArrow
    WifiQuickWidgetId.PREV -> Icons.Default.SkipPrevious
    WifiQuickWidgetId.NEXT -> Icons.Default.SkipNext
    WifiQuickWidgetId.MUTE -> Icons.AutoMirrored.Filled.VolumeMute
    WifiQuickWidgetId.VOL_DOWN -> Icons.AutoMirrored.Filled.VolumeDown
    WifiQuickWidgetId.VOL_UP -> Icons.AutoMirrored.Filled.VolumeUp
    WifiQuickWidgetId.BACK -> Icons.AutoMirrored.Filled.ArrowBack
    WifiQuickWidgetId.HOME -> Icons.Default.Home
    WifiQuickWidgetId.RECENTS -> Icons.Default.Layers
    WifiQuickWidgetId.NOTIFICATIONS -> Icons.Default.Notifications
    WifiQuickWidgetId.QUICK_SETTINGS -> Icons.Default.Tune
    WifiQuickWidgetId.SCREENSHOT -> Icons.Default.Screenshot
    WifiQuickWidgetId.LOCK_SCREEN -> Icons.Default.Lock
    WifiQuickWidgetId.ZOOM_IN -> Icons.Default.Add
    WifiQuickWidgetId.ZOOM_OUT -> Icons.Default.Remove
    WifiQuickWidgetId.REFRESH -> Icons.Default.Refresh
    WifiQuickWidgetId.NEW_TAB -> Icons.Default.Add
    WifiQuickWidgetId.CLOSE_TAB -> Icons.Default.Close
    WifiQuickWidgetId.ADDRESS_BAR -> Icons.Default.Public
    WifiQuickWidgetId.FULLSCREEN -> Icons.Default.Fullscreen
}
