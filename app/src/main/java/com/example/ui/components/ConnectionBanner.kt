package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.ConnectionStatus
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TabRemoteTheme

/**
 * Circular live connection indicator pill (Red disconnected, Green connected / Receiver active).
 */
@Composable
fun BluetoothStatusDot(
    connectionStatus: ConnectionStatus,
    onClick: () -> Unit,
    isReceiverRunning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionStatus is ConnectionStatus.Connected
    val isWifiConnected = (connectionStatus as? ConnectionStatus.Connected)?.isWifi == true

    val badgeColor by animateColorAsState(
        targetValue = when {
            isConnected || isReceiverRunning -> GreenSuccess
            else -> RedDanger
        },
        animationSpec = tween(250),
        label = "status_badge_color"
    )

    val pulse = if (isConnected || isReceiverRunning) {
        val transition = rememberInfiniteTransition(label = "status_pulse")
        val s by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.09f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400),
                repeatMode = RepeatMode.Reverse
            ),
            label = "status_scale"
        )
        s
    } else 1f

    val icon = when {
        isReceiverRunning -> Icons.Default.WifiTethering
        isWifiConnected -> Icons.Default.Wifi
        isConnected -> Icons.Default.BluetoothConnected
        else -> Icons.Default.BluetoothDisabled
    }

    val description = when {
        isReceiverRunning -> "Alıcı modu aktif"
        isConnected -> "Bağlı: ${(connectionStatus as ConnectionStatus.Connected).deviceName}"
        else -> "Bağlı değil, bağlantı detayları için dokunun"
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable { onClick() }
            .semantics { contentDescription = description }
            .testTag("connection_status_dot"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(badgeColor.copy(alpha = 0.16f))
                .border(1.3.dp, badgeColor.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = badgeColor,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

/**
 * Circular-to-card M3 Expressive morphing connection detail popup triggered from the corner badge.
 * Shows only the relevant active transport Settings deep-link (BT or Wi-Fi/Google), or both when disconnected.
 */
@Composable
fun ExpandableConnectionPopup(
    expanded: Boolean,
    connectionStatus: ConnectionStatus,
    isReceiverRunning: Boolean,
    localWifiIp: String,
    expandFromStart: Boolean,
    onDismiss: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    val isConnected = connectionStatus is ConnectionStatus.Connected
    val isWifi = (connectionStatus as? ConnectionStatus.Connected)?.isWifi == true
    val badgeColor = if (isConnected || isReceiverRunning) GreenSuccess else RedDanger
    val origin = if (expandFromStart) TransformOrigin(0.08f, 0.08f) else TransformOrigin(0.92f, 0.08f)
    val morphCorner by animateDpAsState(
        targetValue = if (expanded) 24.dp else 44.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "popup_corner_morph"
    )

    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(tween(150)) +
            scaleIn(
                initialScale = 0.28f,
                transformOrigin = origin,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) +
            expandIn(
                expandFrom = if (expandFromStart) Alignment.TopStart else Alignment.TopEnd,
                initialSize = { IntSize(36, 36) },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        exit = fadeOut(tween(120)) +
            scaleOut(
                targetScale = 0.28f,
                transformOrigin = origin,
                animationSpec = tween(140)
            ) +
            shrinkOut(
                shrinkTowards = if (expandFromStart) Alignment.TopStart else Alignment.TopEnd,
                animationSpec = tween(140)
            ),
        modifier = modifier.zIndex(25f)
    ) {
        val cardShape = RoundedCornerShape(morphCorner)
        Surface(
            color = palette.surfaceContainer,
            shape = cardShape,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .widthIn(min = 248.dp, max = 310.dp)
                .border(1.2.dp, badgeColor.copy(alpha = 0.5f), cardShape)
                .testTag("connection_status_popup")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(badgeColor.copy(alpha = 0.16f))
                                .border(1.dp, badgeColor.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    isReceiverRunning -> Icons.Default.WifiTethering
                                    isWifi -> Icons.Default.Wifi
                                    isConnected -> Icons.Default.BluetoothConnected
                                    else -> Icons.Default.BluetoothDisabled
                                },
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    isReceiverRunning -> "Alıcı Modu Aktif"
                                    isConnected -> (connectionStatus as ConnectionStatus.Connected).deviceName
                                    connectionStatus is ConnectionStatus.Initializing -> "Bağlanıyor..."
                                    else -> "Bağlı Cihaz Yok"
                                },
                                color = palette.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when {
                                    isReceiverRunning -> "Wi-Fi IP: ${localWifiIp.ifBlank { "Hazır" }}"
                                    isWifi -> "Wi-Fi + Erişilebilirlik (${(connectionStatus as ConnectionStatus.Connected).address})"
                                    isConnected -> "Bluetooth HID Profili Bağlı"
                                    connectionStatus is ConnectionStatus.ReadyToPair -> "Bluetooth HID eşleşmeye hazır"
                                    connectionStatus is ConnectionStatus.Error -> connectionStatus.message
                                    else -> "Kumanda bağlantısı bekliyor"
                                },
                                color = palette.textSecondary,
                                fontSize = 10.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (isConnected) {
                    Surface(
                        color = RedDanger.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, RedDanger.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .clickable {
                                onDisconnect()
                                onDismiss()
                            }
                            .testTag("popup_disconnect_btn")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LinkOff,
                                contentDescription = "Bağlantıyı Kes",
                                tint = RedDanger,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bağlantıyı Kes",
                                color = RedDanger,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                val showBtBtn = (isConnected && !isWifi) || (!isConnected && !isReceiverRunning)
                val showWifiBtn = isWifi || isReceiverRunning || (!isConnected && !isReceiverRunning)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showBtBtn) {
                        val highlightBt = isConnected && !isWifi
                        Surface(
                            color = if (highlightBt) palette.primary else palette.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    1.dp,
                                    if (highlightBt) palette.primary else palette.borderSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onDismiss()
                                    onOpenBluetoothSettings()
                                }
                                .testTag("popup_open_bt_settings")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bluetooth,
                                    contentDescription = "Bluetooth Ayarları",
                                    tint = if (highlightBt) palette.onAccent else palette.textPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "BT Ayarları",
                                    color = if (highlightBt) palette.onAccent else palette.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    if (showWifiBtn) {
                        val highlightWifi = isWifi || isReceiverRunning
                        Surface(
                            color = if (highlightWifi) palette.primary else palette.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    1.dp,
                                    if (highlightWifi) palette.primary else palette.borderSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onDismiss()
                                    onOpenWifiSettings()
                                }
                                .testTag("popup_open_wifi_settings")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = "Wi-Fi Ayarları",
                                    tint = if (highlightWifi) palette.onAccent else palette.textPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Wi-Fi Ayarları",
                                    color = if (highlightWifi) palette.onAccent else palette.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Receiver Mode visual lock wrapper: keeps remote controls visible (dimmed) and intercepts all
 * pointer interactions with an explicit banner explaining why sending controls are locked.
 */
@Composable
fun ReceiverLockedControlContainer(
    isLockedForReceiver: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = TabRemoteTheme.palette
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (isLockedForReceiver) 0.42f else 1f),
            content = content
        )
        if (isLockedForReceiver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                    .testTag("receiver_locked_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = palette.surfaceContainer.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .border(1.2.dp, palette.primary.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(palette.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Kilitli",
                                tint = palette.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Alıcı Modunda Kumanda Kilitli",
                                color = palette.textPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Bu cihaz şu an kontrol edilen Alıcı rolünde. Başka cihazı kontrol etmek için Ayarlar'dan Kumanda Modu'na geçin.",
                                color = palette.textSecondary,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
