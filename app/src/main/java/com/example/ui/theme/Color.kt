package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.model.ThemePreset

// Default fallback Ruby constants & shared neutral surface tokens
val RubyPrimary = Color(0xFFFF2A4D)
val RubyPrimaryDark = Color(0xFFCC1835)
val RubyAccent = Color(0xFFFF4D6D)
val RubySecondary = Color(0xFFFF758F)
val RubyMuted = Color(0xFF7A1B28)
val GreenSuccess = Color(0xFF00E676)
val RedDanger = Color(0xFFFF3344)

// Neutral Pitch Black & Shaded Charcoal Surfaces
val BackgroundPitch = Color(0xFF000000)
val SurfaceCharcoal = Color(0xFF0E0E11)
val SurfaceContainerCharcoal = Color(0xFF17181D)
val SurfaceShadedPanel = Color(0xFF131419)
val SurfaceBorderSubtle = Color(0xFF282A34)
val SurfaceBorderElevated = Color(0xFF3A3E4C)

// Fullscreen PC Keyboard elevated readable slate tones (no stark pitch black or harsh white)
val PcKeyCapSurface = Color(0xFF22252F)
val PcKeyCapAccentSurface = Color(0xFF2D3140)
val PcKeyCapPressedText = Color(0xFFE6E9F0)
val ControlThumbSurface = Color(0xFF232631)

// Switch / Toggle neutral balanced dark grey thumbs (less eye strain than pure white)
val SwitchThumbChecked = Color(0xFF23262E)
val SwitchThumbUnchecked = Color(0xFF5D6270)

// High-contrast dark ink foreground for solid accent-filled buttons & badges
val DarkOnAccent = Color(0xFF0E1014)

val TextPrimary = Color(0xFFECEEF3)
val TextSecondary = Color(0xFFC2C6D0)
val TextMuted = Color(0xFF888C96)

val TrackpadSurface = Color(0xFF070709)
val TrackpadGridDot = Color(0x289AA0B0)
val ScrollGutterBg = Color(0xFF0E0F13)
val ScrollGutterThumb = Color(0xFFFF2A4D)

data class TabRemotePalette(
    val primary: Color,
    val primaryDark: Color,
    val accent: Color,
    val secondary: Color,
    val borderAccent: Color,
    val trackpadGridDot: Color,
    val scrollThumb: Color,
    val onAccent: Color = DarkOnAccent,
    val heroGlowTop: Color,
    val background: Color = BackgroundPitch,
    val surface: Color = SurfaceCharcoal,
    val surfaceContainer: Color = SurfaceContainerCharcoal,
    val shadedPanel: Color = SurfaceShadedPanel,
    val controlThumb: Color = ControlThumbSurface,
    val trackpadSurface: Color = TrackpadSurface,
    val scrollGutterBg: Color = ScrollGutterBg,
    val borderSubtle: Color = SurfaceBorderSubtle,
    val borderElevated: Color = SurfaceBorderElevated,
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary,
    val textMuted: Color = TextMuted,
    val pcKeySurface: Color = PcKeyCapSurface,
    val pcKeyAccentSurface: Color = PcKeyCapAccentSurface,
    val pcKeyPressedText: Color = PcKeyCapPressedText
)

fun paletteForPreset(preset: ThemePreset): TabRemotePalette {
    val primary = Color(preset.primaryHex)
    val accent = Color(preset.accentHex)
    val secondary = Color(preset.secondaryHex)
    val dark = Color(preset.darkHex)
    return TabRemotePalette(
        primary = primary,
        primaryDark = dark,
        accent = accent,
        secondary = secondary,
        borderAccent = SurfaceBorderSubtle,
        trackpadGridDot = primary.copy(alpha = 0.16f),
        scrollThumb = primary,
        onAccent = DarkOnAccent,
        heroGlowTop = primary.copy(alpha = 0.12f)
    )
}

fun paletteFromDynamicColors(dynamicScheme: androidx.compose.material3.ColorScheme): TabRemotePalette {
    return TabRemotePalette(
        primary = dynamicScheme.primary,
        primaryDark = dynamicScheme.primaryContainer,
        accent = dynamicScheme.tertiary,
        secondary = dynamicScheme.secondary,
        borderAccent = dynamicScheme.outlineVariant,
        trackpadGridDot = dynamicScheme.primary.copy(alpha = 0.22f),
        scrollThumb = dynamicScheme.primary,
        onAccent = dynamicScheme.onPrimary,
        heroGlowTop = dynamicScheme.primary.copy(alpha = 0.16f),
        background = dynamicScheme.background,
        surface = dynamicScheme.surface,
        surfaceContainer = dynamicScheme.surfaceContainerHigh,
        shadedPanel = dynamicScheme.surfaceContainer,
        controlThumb = dynamicScheme.surfaceContainerHighest,
        trackpadSurface = dynamicScheme.surfaceContainerLowest,
        scrollGutterBg = dynamicScheme.surfaceContainerLow,
        borderSubtle = dynamicScheme.outlineVariant,
        borderElevated = dynamicScheme.outline,
        textPrimary = dynamicScheme.onSurface,
        textSecondary = dynamicScheme.onSurfaceVariant,
        textMuted = dynamicScheme.onSurfaceVariant.copy(alpha = 0.72f),
        pcKeySurface = dynamicScheme.surfaceContainerHigh,
        pcKeyAccentSurface = dynamicScheme.secondaryContainer,
        pcKeyPressedText = dynamicScheme.onSecondaryContainer
    )
}
