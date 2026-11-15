package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.model.ConnectionStatus
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedDanger

/**
 * Top-right unmistakable Bluetooth / Wi-Fi link badge (replaces ambiguous plain circle).
 * Tapping jumps directly to the Settings & Pairing screen.
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
        else -> "Bağlı değil, ayarlara gitmek için dokunun"
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
                .size(30.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(badgeColor.copy(alpha = 0.16f))
                .border(1.2.dp, badgeColor.copy(alpha = 0.65f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = badgeColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
