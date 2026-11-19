package com.example.ui.screens

import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.accessibility.RemoteAccessibilityService
import com.example.model.AirMouseScrollMode
import com.example.model.ButtonPosition
import com.example.model.CursorContrastMode
import com.example.model.HorizontalPanPosition
import com.example.model.NavDockItemId
import com.example.model.RemoteSettings
import com.example.model.ScrollSide
import com.example.model.ThemePreset
import com.example.model.VolumeAction
import com.example.ui.theme.DarkOnAccent
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TabRemoteTheme
import kotlin.math.roundToInt

private val QuickColorSwatches: List<Int> = listOf(
    0xFF0A0A0C.toInt(),
    0xFFFFFFFF.toInt(),
    0xFFFF2A4D.toInt(),
    0xFF4BE3B0.toInt(),
    0xFFFFD740.toInt(),
    0xFF3B82F6.toInt(),
    0xFFD8B4FE.toInt()
)

@Composable
internal fun ThemeSettingsSubPage(
    settings: RemoteSettings,
    onBack: () -> Unit,
    onUpdateSettings: ((RemoteSettings) -> RemoteSettings) -> Unit,
    showBackButton: Boolean = true
) {
    val palette = TabRemoteTheme.palette
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubPageHeader(
            title = "Tema & Renk Paleti",
            subtitle = "Vurgu rengi ve Material You sistem dinamik renk ayarları",
            onBack = onBack,
            showBackButton = showBackButton
        )

        SettingsCard(
            title = "SİSTEM DİNAMİK RENGİ",
            icon = Icons.Default.ColorLens
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingToggleRow(
                    label = "Sistem Dinamik Rengi (Material You)",
                    description = if (supportsDynamicColor) {
                        "Telefonunuzun duvar kağıdı vurgu renklerini uygulamaya otomatik uygular."
                    } else {
                        "Bu özellik Android 12 ve üzeri sürümlerde desteklenir."
                    },
                    checked = settings.useDynamicColor && supportsDynamicColor,
                    onCheckedChange = { enabled ->
                        if (supportsDynamicColor) {
                            onUpdateSettings { it.copy(useDynamicColor = enabled) }
                        }
                    }
                )
            }
        }

        SettingsCard(
            title = "HAZIR VURGU PALETLERİ",
            icon = Icons.Default.Palette
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemePreset.entries.forEach { preset ->
                    val isSelected = !settings.useDynamicColor && settings.themePreset == preset
                    val swatchColor = Color(preset.primaryHex)
                    val shape = RoundedCornerShape(11.dp)

                    Surface(
                        color = if (isSelected) swatchColor.copy(alpha = 0.16f) else palette.surfaceContainer,
                        shape = shape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clip(shape)
                            .border(
                                1.2.dp,
                                if (isSelected) swatchColor else palette.borderAccent,
                                shape
                            )
                            .clickable {
                                onUpdateSettings {
                                    it.copy(
                                        themePreset = preset,
                                        useDynamicColor = false
                                    )
                                }
                            }
                            .testTag("theme_preset_${preset.name.lowercase()}")
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(swatchColor, CircleShape)
                                        .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "${preset.label} seçili",
                                            tint = DarkOnAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = preset.label,
                                        color = palette.textPrimary,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isSelected) "Aktif vurgu paleti" else "Uygulamak için dokunun",
                                        color = if (isSelected) swatchColor else palette.textMuted,
                                        fontSize = 9.5.sp
                                    )
                                }
                            }

                            if (isSelected) {
                                Surface(
                                    color = swatchColor,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Aktif",
                                        color = DarkOnAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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

@Composable
internal fun SensitivitySettingsSubPage(
    settings: RemoteSettings,
    onBack: () -> Unit,
    onUpdateSettings: ((RemoteSettings) -> RemoteSettings) -> Unit,
    showBackButton: Boolean = true
) {
    val palette = TabRemoteTheme.palette

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubPageHeader(
            title = "Hassasiyet & Kalibrasyon",
            subtitle = "Kaydırma sırasında yanlışlıkla değişmemesi için izole ayar sayfası",
            onBack = onBack,
            showBackButton = showBackButton
        )

        SettingsCard(
            title = "HASSASİYET VE HAREKET AYARLARI",
            icon = Icons.Default.Tune
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

                Button(
                    onClick = {
                        onUpdateSettings { s ->
                            s.copy(
                                pointerSensitivity = 1.3f,
                                scrollSensitivity = 0.9f,
                                airMouseSensitivity = 2.0f,
                                airMouseDeadband = 0.04f
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceContainer),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .border(1.dp, palette.borderAccent, RoundedCornerShape(9.dp))
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hassasiyetleri Varsayılana Döndür",
                        color = palette.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
internal fun VolumeKeysSettingsSubPage(
    settings: RemoteSettings,
    onBack: () -> Unit,
    onUpdateSettings: ((RemoteSettings) -> RemoteSettings) -> Unit,
    showBackButton: Boolean = true
) {
    val palette = TabRemoteTheme.palette
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubPageHeader(
            title = "Ses Tuşları",
            subtitle = "Telefon ses tuşlarının Touchpad ve Air Mouse modlarındaki eylemleri",
            onBack = onBack,
            showBackButton = showBackButton
        )

        SettingsCard(
            title = "TOUCHPAD SES TUŞU AYARLARI",
            icon = Icons.Default.TouchApp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Touchpad ekranındayken telefonun ses tuşlarına basıldığında yapılacak eylemi belirleyin.",
                    color = palette.textSecondary,
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

        SettingsCard(
            title = "AIR MOUSE SES TUŞU AYARLARI",
            icon = Icons.Default.Sensors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Air Mouse modundayken fiziksel ses tuşları ile havada sol/sağ tık veya kaydırma yapın.",
                    color = palette.textSecondary,
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
    }
}

@Composable
internal fun CursorSettingsSubPage(
    settings: RemoteSettings,
    accessibilityEnabled: Boolean,
    onBack: () -> Unit,
    onUpdateSettings: ((RemoteSettings) -> RemoteSettings) -> Unit,
    onOpenPermissionWizard: () -> Unit,
    showBackButton: Boolean = true
) {
    val palette = TabRemoteTheme.palette
    // 0 = Dolgu Rengi (İç), 1 = Dış Kenarlık Rengi (Kenar)
    var editingChannel by remember { mutableStateOf(0) }
    var testBgLuminance by remember { mutableFloatStateOf(0.88f) }

    val (previewFillArgb, previewStrokeArgb) = remember(
        settings.cursorColorArgb,
        settings.cursorBorderColorArgb,
        settings.cursorContrastMode,
        testBgLuminance
    ) {
        RemoteAccessibilityService.resolveEffectiveCursorColors(
            manualFillArgb = settings.cursorColorArgb,
            manualBorderArgb = settings.cursorBorderColorArgb,
            mode = settings.cursorContrastMode,
            backgroundLuminance = testBgLuminance
        )
    }

    val activeChannelArgb = if (editingChannel == 0) settings.cursorColorArgb else settings.cursorBorderColorArgb

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubPageHeader(
            title = "İmleç Boyutu, Tam Spektrum & Kontrast",
            subtitle = "Dolgu ve dış kenarlık renklerini bağımsız seçin veya arka plana göre otomatik kontrast kullanın",
            onBack = onBack,
            showBackButton = showBackButton
        )

        // Interactive Light-to-Dark Live Backdrop Preview Card
        Surface(
            color = palette.surface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.borderSubtle, RoundedCornerShape(18.dp))
                .testTag("cursor_preview_card")
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(142.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Color(
                                red = testBgLuminance,
                                green = testBgLuminance,
                                blue = testBgLuminance
                            )
                        )
                        .border(1.dp, palette.borderSubtle, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(72.dp)
                            .semantics {
                                contentDescription = "İmleç önizlemesi (${"%.1f".format(settings.cursorScale)}x)"
                            }
                    ) {
                        val densityPx = 2.6f * settings.cursorScale.coerceIn(0.6f, 2.2f)
                        val offsetX = size.width * 0.5f - (8.8f * densityPx)
                        val offsetY = size.height * 0.5f - (12.0f * densityPx)

                        val arrowPath = Path().apply {
                            moveTo(offsetX + 2.0f * densityPx, offsetY + 2.0f * densityPx)
                            lineTo(offsetX + 2.0f * densityPx, offsetY + 20.5f * densityPx)
                            quadraticTo(
                                offsetX + 2.0f * densityPx,
                                offsetY + 22.0f * densityPx,
                                offsetX + 3.6f * densityPx,
                                offsetY + 21.1f * densityPx
                            )
                            lineTo(offsetX + 8.2f * densityPx, offsetY + 17.8f * densityPx)
                            lineTo(offsetX + 14.8f * densityPx, offsetY + 16.5f * densityPx)
                            quadraticTo(
                                offsetX + 16.6f * densityPx,
                                offsetY + 16.1f * densityPx,
                                offsetX + 15.3f * densityPx,
                                offsetY + 14.8f * densityPx
                            )
                            close()
                        }
                        drawPath(path = arrowPath, color = Color(previewFillArgb))
                        drawPath(
                            path = arrowPath,
                            color = Color(previewStrokeArgb),
                            style = Stroke(
                                width = (1.6f * density).coerceAtLeast(2.0f),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Önizleme Zemini",
                        color = palette.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = testBgLuminance,
                        onValueChange = { testBgLuminance = it },
                        valueRange = 0.04f..0.98f,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.primary,
                            activeTrackColor = palette.primary,
                            inactiveTrackColor = palette.borderElevated
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (testBgLuminance > 0.55f) "Açık" else "Koyu",
                        color = palette.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Separate Cursor Scale Card (isolated cleanly from Colors & Contrast)
        SettingsCard(
            title = "İMLEÇ BOYUTU",
            icon = Icons.Default.Mouse
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ölçek Katsayısı",
                        color = palette.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${"%.1f".format(settings.cursorScale)}x",
                        color = palette.primary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = settings.cursorScale,
                    onValueChange = { raw ->
                        val snapped = (raw * 5f).roundToInt() / 5f
                        onUpdateSettings { s -> s.copy(cursorScale = snapped.coerceIn(0.6f, 2.2f)) }
                    },
                    valueRange = 0.6f..2.2f,
                    steps = 7,
                    colors = SliderDefaults.colors(
                        thumbColor = palette.primary,
                        activeTrackColor = palette.primary,
                        inactiveTrackColor = palette.borderAccent
                    ),
                    modifier = Modifier.testTag("cursor_size_slider")
                )
            }
        }

        // Contrast Mode Selector (Manual / Instant BW / Gradual Adaptive)
        SettingsCard(
            title = "OTOMATİK ARKA PLAN KONTRASTI",
            icon = Icons.Default.AccessibilityNew
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceRow(
                    label = "Kontrast Modu",
                    options = listOf(
                        CursorContrastMode.MANUAL,
                        CursorContrastMode.INSTANT_BW,
                        CursorContrastMode.GRADUAL_ADAPTIVE
                    ),
                    selected = settings.cursorContrastMode,
                    optionLabel = { it.label },
                    onSelect = { mode ->
                        onUpdateSettings { s -> s.copy(cursorContrastMode = mode) }
                    }
                )
                Text(
                    text = settings.cursorContrastMode.description,
                    color = palette.textSecondary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }

        // Manual Full-Spectrum Color Picker Card — shown ONLY when MANUAL color mode is active
        AnimatedVisibility(
            visible = settings.cursorContrastMode == CursorContrastMode.MANUAL,
            enter = expandVertically(androidx.compose.animation.core.tween(200)) + fadeIn(androidx.compose.animation.core.tween(160)),
            exit = shrinkVertically(androidx.compose.animation.core.tween(180)) + fadeOut(androidx.compose.animation.core.tween(120))
        ) {
            SettingsCard(
                title = "TAM SPEKTRUM RENK ÖZELLEŞTİRME",
                icon = Icons.Default.Palette
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ColorTargetTab(
                            title = "Dolgu Rengi (İç)",
                            colorArgb = settings.cursorColorArgb,
                            selected = editingChannel == 0,
                            onClick = { editingChannel = 0 },
                            modifier = Modifier.weight(1f).testTag("cursor_channel_fill_tab")
                        )
                        ColorTargetTab(
                            title = "Dış Kenarlık",
                            colorArgb = settings.cursorBorderColorArgb,
                            selected = editingChannel == 1,
                            onClick = { editingChannel = 1 },
                            modifier = Modifier.weight(1f).testTag("cursor_channel_border_tab")
                        )
                    }

                    FullSpectrumHsvPicker(
                        selectedArgb = activeChannelArgb,
                        onColorChange = { newArgb ->
                            onUpdateSettings { s ->
                                if (editingChannel == 0) s.copy(cursorColorArgb = newArgb)
                                else s.copy(cursorBorderColorArgb = newArgb)
                            }
                        }
                    )
                }
            }
        }

        Surface(
            color = palette.surfaceContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (accessibilityEnabled) GreenSuccess.copy(alpha = 0.5f) else palette.borderAccent,
                    RoundedCornerShape(12.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                            "Erişilebilirlik Servisi Aktif — Değişiklikler ve ekran kontrastı anında uygulanır"
                        } else {
                            "Bu ayarlar Alıcı modunda Erişilebilirlik Servisi açıkken görünür"
                        },
                        color = if (accessibilityEnabled) GreenSuccess else palette.textSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (!accessibilityEnabled) {
                    Button(
                        onClick = onOpenPermissionWizard,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = "Erişilebilirlik Durumunu Aç",
                            color = palette.onAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorTargetTab(
    title: String,
    colorArgb: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = TabRemoteTheme.palette
    val shape = RoundedCornerShape(10.dp)
    Surface(
        color = if (selected) palette.primary.copy(alpha = 0.16f) else palette.surfaceContainer,
        shape = shape,
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .border(1.2.dp, if (selected) palette.primary else palette.borderSubtle, shape)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color(colorArgb), CircleShape)
                    .border(1.2.dp, Color.White.copy(alpha = 0.65f), CircleShape)
            )
            Text(
                text = title,
                color = if (selected) palette.textPrimary else palette.textSecondary,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FullSpectrumHsvPicker(
    selectedArgb: Int,
    onColorChange: (Int) -> Unit
) {
    val palette = TabRemoteTheme.palette
    val hsv = remember(selectedArgb) {
        FloatArray(3).also { AndroidColor.colorToHSV(selectedArgb, it) }
    }
    var hue by remember(selectedArgb) { mutableFloatStateOf(hsv[0]) }
    var sat by remember(selectedArgb) { mutableFloatStateOf(hsv[1]) }
    var value by remember(selectedArgb) { mutableFloatStateOf(hsv[2]) }

    fun emitColor(h: Float = hue, s: Float = sat, v: Float = value) {
        val argb = AndroidColor.HSVToColor(floatArrayOf(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f)))
        onColorChange(argb)
    }

    val spectrumColors = remember {
        listOf(
            Color.Red,
            Color.Yellow,
            Color.Green,
            Color.Cyan,
            Color.Blue,
            Color.Magenta,
            Color.Red
        )
    }

    val hexLabel = remember(selectedArgb) {
        String.format("#%06X", 0xFFFFFF and selectedArgb)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tam Renk Spektrumu",
                color = palette.textPrimary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = hexLabel,
                color = palette.textSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        // Smooth Horizontal Hue Spectrum Slider Bar
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, palette.borderElevated, RoundedCornerShape(10.dp))
                .testTag("cursor_hue_spectrum_bar")
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val thumbFraction = (hue / 360f).coerceIn(0f, 1f)
            val thumbXPx = (thumbFraction * (widthPx - with(density) { 20.dp.toPx() })).roundToInt()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(spectrumColors))
                    .pointerInput(widthPx) {
                        detectTapGestures { offset ->
                            val newHue = ((offset.x / widthPx) * 360f).coerceIn(0f, 360f)
                            hue = newHue
                            if (sat < 0.1f) sat = 0.85f
                            if (value < 0.15f) value = 0.95f
                            emitColor(h = newHue, s = sat, v = value)
                        }
                    }
                    .pointerInput(widthPx) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val newHue = ((change.position.x / widthPx) * 360f).coerceIn(0f, 360f)
                            hue = newHue
                            if (sat < 0.1f) sat = 0.85f
                            if (value < 0.15f) value = 0.95f
                            emitColor(h = newHue, s = sat, v = value)
                        }
                    }
            )

            Box(
                modifier = Modifier
                    .offset { IntOffset(thumbXPx, 0) }
                    .align(Alignment.CenterStart)
                    .size(20.dp)
                    .background(Color.hsv(hue.coerceIn(0f, 360f), 1f, 1f), CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
        }

        SliderRow(
            label = "Doygunluk (Saturation)",
            value = sat,
            range = 0f..1f,
            displayValue = "%${(sat * 100).roundToInt()}",
            onValueChange = { sNew ->
                sat = sNew
                emitColor(s = sNew)
            }
        )

        SliderRow(
            label = "Parlaklık (Luminance)",
            value = value,
            range = 0.04f..1f,
            displayValue = "%${(value * 100).roundToInt()}",
            onValueChange = { vNew ->
                value = vNew
                emitColor(v = vNew)
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickColorSwatches.forEachIndexed { index, argb ->
                val swatchColor = Color(argb)
                val isSelected = selectedArgb == argb
                val checkTint = if (swatchColor.luminance() < 0.3f) Color.White else DarkOnAccent
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onColorChange(argb) }
                        .testTag("cursor_color_swatch_$index"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(swatchColor, CircleShape)
                            .border(
                                width = if (isSelected) 2.2.dp else 1.2.dp,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.45f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Seçili renk",
                                tint = checkTint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModernVolumeActionSelector(
    label: String,
    selectedAction: VolumeAction,
    onSelect: (VolumeAction) -> Unit
) {
    val palette = TabRemoteTheme.palette
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Column {
        Text(text = label, color = palette.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        val selectorShape = RoundedCornerShape(10.dp)
        Surface(
            color = palette.surfaceContainer,
            shape = selectorShape,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(selectorShape)
                .border(1.dp, palette.primary.copy(alpha = 0.5f), selectorShape)
                .clickable { showSheet = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(
                        text = selectedAction.title,
                        color = palette.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedAction.description,
                        color = palette.textSecondary,
                        fontSize = 9.5.sp
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Değiştir",
                    tint = palette.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = palette.surface,
                contentColor = palette.textPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Ses Tuşu İşlevi Seçin",
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    val grouped = VolumeAction.entries.groupBy { it.category }
                    grouped.forEach { (category, actions) ->
                        Text(
                            text = category,
                            color = palette.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )

                        actions.forEach { action ->
                            val isSelected = action == selectedAction
                            val itemShape = RoundedCornerShape(9.dp)
                            Surface(
                                color = if (isSelected) palette.primary.copy(alpha = 0.2f) else palette.surfaceContainer,
                                shape = itemShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(itemShape)
                                    .border(
                                        1.dp,
                                        if (isSelected) palette.primary else palette.borderAccent,
                                        itemShape
                                    )
                                    .clickable {
                                        onSelect(action)
                                        showSheet = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = action.title,
                                            color = if (isSelected) palette.primary else palette.textPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = action.description,
                                            color = palette.textSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Seçili",
                                            tint = palette.primary,
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
internal fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    val palette = TabRemoteTheme.palette
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = palette.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(text = displayValue, color = palette.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = palette.primary,
                activeTrackColor = palette.primary,
                inactiveTrackColor = palette.borderAccent
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TouchpadLayoutSettingsSubPage(
    settings: RemoteSettings,
    onBack: () -> Unit,
    onUpdateSettings: ((RemoteSettings) -> RemoteSettings) -> Unit,
    onMoveNavDockItem: (index: Int, delta: Int) -> Unit = { _, _ -> },
    onMoveNavDockItemTo: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onToggleNavDockHidden: (NavDockItemId) -> Unit = {},
    onResetDockCustomizations: () -> Unit,
    showBackButton: Boolean = true
) {
    val palette = TabRemoteTheme.palette

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubPageHeader(
            title = "Touchpad, Hizalama & Dock",
            subtitle = "Dokunmatik alan, alt navigasyon çubuğu sırası ve yatay kaydırma konumu",
            onBack = onBack,
            showBackButton = showBackButton
        )

        // Visual Navigation Bar Drag-to-Reorder Editor (mirrors real control bar)
        SettingsCard(
            title = "ALT NAVİGASYON ÇUBUĞU DÜZENLEYİCİ",
            icon = Icons.Default.Dashboard
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Tuşları parmağınızla sürükleyerek yerlerini değiştirebilirsiniz. Altındaki göz simgeleriyle görünürlüğü açıp kapatın.",
                    color = palette.textSecondary,
                    fontSize = 10.5.sp,
                    lineHeight = 14.5.sp
                )

                val order = settings.navDockOrder
                val itemCount = order.size.coerceAtLeast(1)
                var draggingIdx by remember { mutableIntStateOf(-1) }
                var dragOffsetX by remember { mutableFloatStateOf(0f) }
                val latestOrder by rememberUpdatedState(order)
                val latestOnMoveTo by rememberUpdatedState(onMoveNavDockItemTo)

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .background(palette.surfaceContainer, RoundedCornerShape(14.dp))
                        .border(1.dp, palette.borderElevated, RoundedCornerShape(14.dp))
                        .padding(5.dp)
                        .testTag("settings_dock_visual_bar")
                ) {
                    val density = LocalDensity.current
                    val gapPx = with(density) { 5.dp.toPx() }
                    val totalWidthPx = with(density) { maxWidth.toPx() }
                    val slotWidthPx = ((totalWidthPx - gapPx * (itemCount - 1)) / itemCount).coerceAtLeast(20f)
                    val stepPx = (slotWidthPx + gapPx).coerceAtLeast(24f)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(itemCount, stepPx) {
                                detectDragGestures(
                                    onDragStart = { startOffset ->
                                        val hitIdx = (startOffset.x / stepPx).toInt().coerceIn(0, itemCount - 1)
                                        draggingIdx = hitIdx
                                        dragOffsetX = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (draggingIdx !in 0 until latestOrder.size) return@detectDragGestures
                                        dragOffsetX += dragAmount.x
                                        if (dragOffsetX > stepPx * 0.55f && draggingIdx < latestOrder.lastIndex) {
                                            val from = draggingIdx
                                            val to = draggingIdx + 1
                                            latestOnMoveTo(from, to)
                                            draggingIdx = to
                                            dragOffsetX -= stepPx
                                        } else if (dragOffsetX < -stepPx * 0.55f && draggingIdx > 0) {
                                            val from = draggingIdx
                                            val to = draggingIdx - 1
                                            latestOnMoveTo(from, to)
                                            draggingIdx = to
                                            dragOffsetX += stepPx
                                        }
                                    },
                                    onDragEnd = {
                                        draggingIdx = -1
                                        dragOffsetX = 0f
                                    },
                                    onDragCancel = {
                                        draggingIdx = -1
                                        dragOffsetX = 0f
                                    }
                                )
                            }
                    ) {
                        order.forEachIndexed { idx, item ->
                            val isHidden = item in settings.hiddenNavDockItems
                            val isDragged = draggingIdx == idx
                            val xPx = idx * stepPx + if (isDragged) dragOffsetX else 0f
                            val slotW = with(density) { slotWidthPx.toDp() }

                            Surface(
                                color = when {
                                    isDragged -> palette.primary
                                    isHidden -> palette.surface
                                    else -> palette.surface.copy(alpha = 0.95f)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .offset { IntOffset(xPx.roundToInt(), 0) }
                                    .width(slotW)
                                    .fillMaxHeight()
                                    .zIndex(if (isDragged) 10f else 1f)
                                    .alpha(if (isHidden && !isDragged) 0.42f else 1f)
                                    .border(
                                        1.dp,
                                        if (isDragged) palette.primary else palette.borderSubtle,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .testTag("settings_dock_item_${item.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 2.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    when (item) {
                                        NavDockItemId.BACK -> Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = item.defaultLabel,
                                            tint = if (isDragged) palette.onAccent else palette.textPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        NavDockItemId.HOME -> Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = item.defaultLabel,
                                            tint = if (isDragged) palette.onAccent else palette.textPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        NavDockItemId.RECENT -> Icon(
                                            imageVector = Icons.Default.Layers,
                                            contentDescription = item.defaultLabel,
                                            tint = if (isDragged) palette.onAccent else palette.textPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        NavDockItemId.F1, NavDockItemId.F2 -> Text(
                                            text = item.defaultLabel,
                                            color = if (isDragged) palette.onAccent else palette.primary,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Text(
                                        text = item.defaultLabel,
                                        color = if (isDragged) palette.onAccent else palette.textSecondary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Compact Visibility Toggle Pills right under the Visual Nav Bar slots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    order.forEach { item ->
                        val isHidden = item in settings.hiddenNavDockItems
                        Surface(
                            color = if (isHidden) palette.surface else palette.surfaceContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    if (isHidden) RedDanger.copy(alpha = 0.4f) else GreenSuccess.copy(alpha = 0.45f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onToggleNavDockHidden(item) }
                                .testTag("settings_dock_toggle_${item.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isHidden) "${item.defaultLabel} Göster" else "${item.defaultLabel} Gizle",
                                    tint = if (isHidden) RedDanger else GreenSuccess,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        SettingsCard(
            title = "KAYDIRMA ÇUBUKLARI & DÜZEN",
            icon = Icons.Default.Dashboard
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChoiceRow(
                    label = "Dikey Kaydırma Tipi",
                    options = listOf(AirMouseScrollMode.SCROLL_STRIP, AirMouseScrollMode.STEP_BUTTONS),
                    selected = settings.airMouseScrollMode,
                    optionLabel = { it.label },
                    onSelect = { onUpdateSettings { s -> s.copy(airMouseScrollMode = it) } }
                )

                SettingToggleRow(
                    label = "Yatay Kaydırma Çubuğu",
                    description = "Galeri ve sekme geçişleri için yatay dokunmatik kaydırma rayı",
                    checked = settings.showHorizontalScrollGutter,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(showHorizontalScrollGutter = it) } }
                )

                if (settings.showHorizontalScrollGutter) {
                    ChoiceRow(
                        label = "Yatay Kaydırma Çubuğu Konumu",
                        options = listOf(HorizontalPanPosition.ABOVE_BUTTONS, HorizontalPanPosition.BELOW_BUTTONS),
                        selected = settings.horizontalPanPosition,
                        optionLabel = { it.label },
                        onSelect = { pos ->
                            onUpdateSettings { s -> s.copy(horizontalPanPosition = pos) }
                        }
                    )
                }

                ChoiceRow(
                    label = "Dikey Kaydırma Alanı Yeri",
                    options = listOf(ScrollSide.RIGHT, ScrollSide.LEFT, ScrollSide.HIDDEN),
                    selected = settings.scrollSide,
                    optionLabel = { it.label },
                    onSelect = { onUpdateSettings { s -> s.copy(scrollSide = it) } }
                )
            }
        }

        SettingsCard(
            title = "FARE TUŞLARI, DOKUNMA & DOCK",
            icon = Icons.Default.TouchApp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingToggleRow(
                    label = "F1 & F2 Ekstra Fare Tuşlarını Göster",
                    description = "Alt dock'ta özelleştirilebilir 4. ve 5. fare tuşları",
                    checked = settings.showFunctionButtons,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(showFunctionButtons = it) } }
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

                Button(
                    onClick = onResetDockCustomizations,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceContainer),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .border(1.dp, palette.borderAccent, RoundedCornerShape(9.dp))
                        .testTag("reset_dock_customizations_btn")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Quick Tools, Wi-Fi Panel & Dock Sırasını Sıfırla",
                        color = palette.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
