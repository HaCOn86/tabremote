package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.accessibility.RemoteMediaNotificationService
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TabRemoteTheme

data class PermissionAuditState(
    val bluetoothReady: Boolean,
    val notificationReady: Boolean,
    val accessibilityReady: Boolean,
    val notificationListenerReady: Boolean,
    val missingPermissions: List<String>
)

fun auditAppPermissions(
    context: Context,
    accessibilityEnabled: Boolean,
    notificationListenerEnabled: Boolean = RemoteMediaNotificationService.isNotificationListenerEnabled(context)
): PermissionAuditState {
    val missing = mutableListOf<String>()
    val bluetoothReady = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val btConnect = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        val btScan = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        val btAdv = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
        if (!btConnect) missing.add(Manifest.permission.BLUETOOTH_CONNECT)
        if (!btScan) missing.add(Manifest.permission.BLUETOOTH_SCAN)
        if (!btAdv) missing.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        btConnect && btScan && btAdv
    } else {
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!coarse) missing.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        coarse
    }

    val notificationReady = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) missing.add(Manifest.permission.POST_NOTIFICATIONS)
        granted
    } else {
        true
    }

    return PermissionAuditState(
        bluetoothReady = bluetoothReady,
        notificationReady = notificationReady,
        accessibilityReady = accessibilityEnabled,
        notificationListenerReady = notificationListenerEnabled,
        missingPermissions = missing
    )
}

@Composable
fun AccessibilityAndPermissionModal(
    title: String,
    subtitle: String,
    audit: PermissionAuditState,
    allowSkipForController: Boolean,
    onRequestMissingPermissions: (List<String>) -> Unit,
    onRefreshStatus: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = TabRemoteTheme.palette
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = palette.surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, palette.primary.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessibilityNew,
                        contentDescription = "Erişilebilirlik ve İzinler",
                        tint = palette.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Column {
                        Text(
                            text = title,
                            color = palette.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = subtitle,
                            color = palette.textSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Live Permission Status Checklist
                Surface(
                    color = palette.surfaceContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.borderAccent, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusCheckRow(
                            icon = Icons.Default.AccessibilityNew,
                            label = "Erişilebilirlik Servisi (Alıcı İmleç, Kontrast & Aktif Uygulama)",
                            granted = audit.accessibilityReady
                        )
                        StatusCheckRow(
                            icon = Icons.Default.MusicNote,
                            label = "Bildirim Dinleme İzni (Alıcıda Çalan Medya / Şarkı Adı)",
                            granted = audit.notificationListenerReady
                        )
                        StatusCheckRow(
                            icon = Icons.Default.Bluetooth,
                            label = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                "Yakındaki Cihazlar / Bluetooth Taraması (Nearby Scan)"
                            } else {
                                "Bluetooth & Konum Taraması"
                            },
                            granted = audit.bluetoothReady
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            StatusCheckRow(
                                icon = Icons.Default.Notifications,
                                label = "Bildirimler (Arka Plan Bağlantı Durumu)",
                                granted = audit.notificationReady
                            )
                        }
                    }
                }

                if (!audit.accessibilityReady || !audit.notificationListenerReady) {
                    Surface(
                        color = palette.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.primary.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .testTag("android13_restricted_settings_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "Kısıtlanmış Ayarlar",
                                    tint = palette.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Android 13+ 'Kısıtlanmış Ayar' Çözümü",
                                    color = palette.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Erişilebilirlik veya Bildirim Dinleme açılırken 'Kısıtlanmış Ayar' uyarısı verirse:\n" +
                                    "1. Aşağıdan 'Uygulama Bilgisi'ni açın.\n" +
                                    "2. Sağ üstteki üç nokta (⋮) menüsüne dokunun.\n" +
                                    "3. 'Kısıtlanmış ayarlara izin ver' seçeneğini onaylayın.\n" +
                                    "4. Ardından Erişilebilirlik ve Bildirim Erişimini açın.",
                                color = palette.textSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )

                            OutlinedButton(
                                onClick = {
                                    val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(detailsIntent)
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, palette.primary.copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("open_app_info_restricted_btn")
                            ) {
                                Text(
                                    text = "Uygulama Bilgisi (⋮ Kısıtlı Ayarı Aç)",
                                    color = palette.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (!audit.accessibilityReady) {
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary,
                                contentColor = palette.onAccent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("wizard_open_accessibility_btn")
                        ) {
                            Text(
                                text = "Erişilebilirlik Ayarlarına Git",
                                color = palette.onAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!audit.notificationListenerReady) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, palette.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("wizard_open_notification_listener_btn")
                        ) {
                            Text(
                                text = "Bildirim Dinleme Ayarlarına Git (Medya)",
                                color = palette.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (audit.missingPermissions.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { onRequestMissingPermissions(audit.missingPermissions) },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, palette.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("wizard_request_permissions_btn")
                    ) {
                        Text(
                            text = "Bluetooth / Nearby & Bildirim İzinlerini Ver",
                            color = palette.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onRefreshStatus) {
                        Text("Durumu Yenile", color = palette.textSecondary, fontSize = 11.5.sp)
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("wizard_skip_or_done_btn")
                    ) {
                        Text(
                            text = if (allowSkipForController && !audit.accessibilityReady) {
                                "Kontrolcü Modunda Atla"
                            } else {
                                "Tamam / Kapat"
                            },
                            color = if (audit.accessibilityReady && audit.bluetoothReady) GreenSuccess else palette.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCheckRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    granted: Boolean
) {
    val palette = TabRemoteTheme.palette
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = if (granted) "Verildi" else "Eksik",
            tint = if (granted) GreenSuccess else RedDanger,
            modifier = Modifier.size(16.dp)
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.textMuted,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            color = if (granted) palette.textPrimary else RedDanger,
            fontSize = 11.sp,
            fontWeight = if (granted) FontWeight.Medium else FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}
