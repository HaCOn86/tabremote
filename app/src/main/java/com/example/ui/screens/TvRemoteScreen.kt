package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetooth.HidConstants
import com.example.model.AppRole
import com.example.model.RemoteSettings
import com.example.ui.components.ReceiverLockedControlContainer
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TabRemoteTheme

/**
 * Smart TV / Media Box Remote (Akıllı Kumanda) optimized for Bluetooth HID & Wi-Fi:
 * - Power, Source/Input, Mute, Menu & collapsible 0-9 Numpad drawer
 * - Ergonomic circular D-Pad (Up/Down/Left/Right/OK) + Android/TV Nav row
 * - Volume & Channel dual rockers + full Media playback bar
 */
@Composable
fun TvRemoteScreen(
    settings: RemoteSettings,
    isTabletLayout: Boolean = false,
    onSendKey: (hidKeyCode: Int, modifier: Int) -> Unit,
    onSendConsumerKey: (usageCode: Int, name: String) -> Unit,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onRecentAppsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    var showNumpad by remember { mutableStateOf(false) }

    val topHeaderBlock: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvTopActionButton(
                icon = Icons.Default.PowerSettingsNew,
                label = "GÜÇ",
                tint = RedDanger,
                onClick = { onSendConsumerKey(HidConstants.CONSUMER_POWER, "Power") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tv_power_btn")
            )
            TvTopActionButton(
                icon = Icons.AutoMirrored.Filled.Input,
                label = "KAYNAK",
                tint = palette.textPrimary,
                onClick = { onSendKey(HidConstants.KEY_F12, 0) },
                modifier = Modifier.weight(1f).testTag("tv_source_btn")
            )
            TvTopActionButton(
                icon = Icons.AutoMirrored.Filled.VolumeOff,
                label = "SESSİZ",
                tint = palette.primary,
                onClick = { onSendConsumerKey(HidConstants.CONSUMER_MUTE, "Mute") },
                modifier = Modifier.weight(1f).testTag("tv_mute_btn")
            )
            TvTopActionButton(
                icon = Icons.Default.Menu,
                label = "MENÜ",
                tint = palette.textPrimary,
                onClick = { onSendConsumerKey(HidConstants.CONSUMER_MENU, "Menu") },
                modifier = Modifier.weight(1f).testTag("tv_menu_btn")
            )
            TvTopActionButton(
                icon = Icons.Default.Dialpad,
                label = "123",
                tint = if (showNumpad) palette.onAccent else palette.textPrimary,
                containerColor = if (showNumpad) palette.primary else palette.surface,
                onClick = { showNumpad = !showNumpad },
                modifier = Modifier.weight(1f).testTag("tv_numpad_toggle_btn")
            )
        }
    }

    val numpadDrawer: @Composable () -> Unit = {
        AnimatedVisibility(
            visible = showNumpad,
            enter = expandVertically(tween(200)) + fadeIn(tween(150)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(120))
        ) {
            Surface(
                color = palette.surfaceContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.borderElevated, RoundedCornerShape(16.dp))
                    .testTag("tv_numpad_drawer")
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val rows = listOf(
                        listOf("1" to HidConstants.KEY_1, "2" to HidConstants.KEY_2, "3" to HidConstants.KEY_3),
                        listOf("4" to HidConstants.KEY_4, "5" to HidConstants.KEY_5, "6" to HidConstants.KEY_6),
                        listOf("7" to HidConstants.KEY_7, "8" to HidConstants.KEY_8, "9" to HidConstants.KEY_9)
                    )
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { (digit, code) ->
                                TvDigitKey(
                                    label = digit,
                                    onClick = { onSendKey(code, 0) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TvDigitKey(
                            label = "SİL",
                            isSubtle = true,
                            onClick = { onSendKey(HidConstants.KEY_BACKSPACE, 0) },
                            modifier = Modifier.weight(1f)
                        )
                        TvDigitKey(
                            label = "0",
                            onClick = { onSendKey(HidConstants.KEY_0, 0) },
                            modifier = Modifier.weight(1f)
                        )
                        TvDigitKey(
                            label = "GİRİŞ",
                            isSubtle = true,
                            onClick = { onSendKey(HidConstants.KEY_ENTER, 0) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    val dpadCluster: @Composable () -> Unit = {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                color = palette.surface,
                shape = CircleShape,
                modifier = Modifier
                    .size(204.dp)
                    .border(1.5.dp, palette.borderElevated, CircleShape)
                    .testTag("tv_circular_dpad")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    TvDpadDirectionButton(
                        icon = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Yukarı",
                        onClick = { onSendKey(HidConstants.KEY_UP, 0) },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .testTag("tv_dpad_up")
                    )
                    TvDpadDirectionButton(
                        icon = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Aşağı",
                        onClick = { onSendKey(HidConstants.KEY_DOWN, 0) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .testTag("tv_dpad_down")
                    )
                    TvDpadDirectionButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Sol",
                        onClick = { onSendKey(HidConstants.KEY_LEFT, 0) },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .testTag("tv_dpad_left")
                    )
                    TvDpadDirectionButton(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Sağ",
                        onClick = { onSendKey(HidConstants.KEY_RIGHT, 0) },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .testTag("tv_dpad_right")
                    )

                    // Center OK button
                    var okPressed by remember { mutableStateOf(false) }
                    Surface(
                        color = if (okPressed) palette.primary else palette.controlThumb,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(72.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .border(
                                1.5.dp,
                                if (okPressed) palette.primary else palette.primary.copy(alpha = 0.6f),
                                CircleShape
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        okPressed = true
                                        val ok = tryAwaitRelease()
                                        okPressed = false
                                        if (ok) onSendKey(HidConstants.KEY_ENTER, 0)
                                    }
                                )
                            }
                            .testTag("tv_dpad_ok")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "OK",
                                color = if (okPressed) palette.onAccent else palette.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TvNavChip(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    label = "Geri",
                    onClick = onBackClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tv_nav_back")
                )
                TvNavChip(
                    icon = Icons.Default.Home,
                    label = "Ana Ekran",
                    onClick = onHomeClick,
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("tv_nav_home")
                )
                TvNavChip(
                    icon = Icons.Default.Layers,
                    label = "Son Uyg.",
                    onClick = onRecentAppsClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tv_nav_recents")
                )
            }
        }
    }

    val rockersAndMediaBlock: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Dual Vertical Rockers: Volume & Channel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TvVerticalRocker(
                    title = "SES",
                    topIcon = Icons.AutoMirrored.Filled.VolumeUp,
                    bottomIcon = Icons.AutoMirrored.Filled.VolumeDown,
                    onTop = { onSendConsumerKey(HidConstants.CONSUMER_VOLUME_UP, "Vol Up") },
                    onBottom = { onSendConsumerKey(HidConstants.CONSUMER_VOLUME_DOWN, "Vol Down") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tv_volume_rocker")
                )

                Column(
                    modifier = Modifier
                        .weight(0.75f)
                        .height(112.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TvCompactPillButton(
                        icon = Icons.AutoMirrored.Filled.VolumeOff,
                        label = "Sessiz",
                        onClick = { onSendConsumerKey(HidConstants.CONSUMER_MUTE, "Mute") },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                    TvCompactPillButton(
                        icon = Icons.Default.Menu,
                        label = "Çıkış",
                        onClick = { onSendKey(HidConstants.KEY_ESC, 0) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }

                TvVerticalRocker(
                    title = "KANAL",
                    topIcon = Icons.Default.Add,
                    bottomIcon = Icons.Default.Remove,
                    onTop = { onSendConsumerKey(HidConstants.CONSUMER_CH_UP, "CH+") },
                    onBottom = { onSendConsumerKey(HidConstants.CONSUMER_CH_DOWN, "CH-") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tv_channel_rocker")
                )
            }

            // Media Transport Bar
            Surface(
                color = palette.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.borderSubtle, RoundedCornerShape(16.dp))
                    .testTag("tv_media_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvMediaIconBtn(
                        icon = Icons.Default.FastRewind,
                        contentDescription = "Geri Sar",
                        onClick = { onSendConsumerKey(HidConstants.CONSUMER_REWIND, "Rewind") }
                    )
                    TvMediaIconBtn(
                        icon = Icons.Default.SkipPrevious,
                        contentDescription = "Önceki",
                        onClick = { onSendConsumerKey(HidConstants.CONSUMER_PREV_TRACK, "Prev") }
                    )
                    Surface(
                        color = palette.primary,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSendConsumerKey(HidConstants.CONSUMER_PLAY_PAUSE, "Play/Pause") }
                            .testTag("tv_media_play_pause")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Oynat",
                                    tint = palette.onAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Duraklat",
                                    tint = palette.onAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    TvMediaIconBtn(
                        icon = Icons.Default.Stop,
                        contentDescription = "Durdur",
                        onClick = { onSendConsumerKey(HidConstants.CONSUMER_STOP, "Stop") }
                    )
                    TvMediaIconBtn(
                        icon = Icons.Default.SkipNext,
                        contentDescription = "Sonraki",
                        onClick = { onSendConsumerKey(HidConstants.CONSUMER_NEXT_TRACK, "Next") }
                    )
                    TvMediaIconBtn(
                        icon = Icons.Default.FastForward,
                        contentDescription = "İleri Sar",
                        onClick = { onSendConsumerKey(HidConstants.CONSUMER_FAST_FORWARD, "Fast Forward") }
                    )
                }
            }
        }
    }

    ReceiverLockedControlContainer(
        isLockedForReceiver = settings.appRole == AppRole.RECEIVER,
        modifier = modifier.fillMaxSize()
    ) {
        if (isTabletLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 6.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    dpadCluster()
                }
                Column(
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    topHeaderBlock()
                    numpadDrawer()
                    rockersAndMediaBlock()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp, bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                topHeaderBlock()
                numpadDrawer()
                dpadCluster()
                rockersAndMediaBlock()
            }
        }
    }
}

@Composable
private fun TvTopActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    containerColor: Color? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    Surface(
        color = containerColor ?: palette.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = tint,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TvDpadDirectionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    var pressed by remember { mutableStateOf(false) }
    Surface(
        color = if (pressed) palette.primary else palette.surfaceContainer,
        shape = CircleShape,
        modifier = modifier
            .size(50.dp)
            .clip(CircleShape)
            .border(1.dp, if (pressed) palette.primary else palette.borderSubtle, CircleShape)
            .pointerInput(contentDescription) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        val ok = tryAwaitRelease()
                        pressed = false
                        if (ok) onClick()
                    }
                )
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (pressed) palette.onAccent else palette.textPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun TvNavChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = palette.textPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = palette.textPrimary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TvVerticalRocker(
    title: String,
    topIcon: ImageVector,
    bottomIcon: ImageVector,
    onTop: () -> Unit,
    onBottom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .height(112.dp)
            .border(1.dp, palette.borderElevated, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { onTop() },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = topIcon, contentDescription = "$title Artır", tint = palette.textPrimary, modifier = Modifier.size(22.dp))
            }
            Text(
                text = title,
                color = palette.textMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { onBottom() },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = bottomIcon, contentDescription = "$title Azalt", tint = palette.textPrimary, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun TvCompactPillButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = palette.textSecondary, modifier = Modifier.size(16.dp))
            Text(text = label, color = palette.textPrimary, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TvDigitKey(
    label: String,
    isSubtle: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    Surface(
        color = if (isSubtle) palette.controlThumb else palette.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = palette.textPrimary,
                fontSize = if (isSubtle) 11.sp else 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TvMediaIconBtn(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val palette = TabRemoteTheme.palette
    Surface(
        color = palette.surfaceContainer,
        shape = CircleShape,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = palette.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
