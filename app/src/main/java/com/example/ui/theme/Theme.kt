package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.model.ThemePreset

val LocalTabRemotePalette = compositionLocalOf { paletteForPreset(ThemePreset.RUBY) }

object TabRemoteTheme {
    val palette: TabRemotePalette
        @Composable
        @ReadOnlyComposable
        get() = LocalTabRemotePalette.current
}

@Composable
fun TabRemoteTheme(
    themePreset: ThemePreset = ThemePreset.RUBY,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useSysDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val dynamicScheme = if (useSysDynamic) {
        dynamicDarkColorScheme(context)
    } else null

    val palette = if (dynamicScheme != null) {
        paletteFromDynamicColors(dynamicScheme)
    } else {
        paletteForPreset(themePreset)
    }

    val colorScheme = dynamicScheme ?: darkColorScheme(
        primary = palette.primary,
        onPrimary = palette.onAccent,
        primaryContainer = palette.primary.copy(alpha = 0.22f),
        onPrimaryContainer = palette.secondary,
        secondary = palette.accent,
        onSecondary = palette.onAccent,
        secondaryContainer = palette.surfaceContainer,
        onSecondaryContainer = palette.secondary,
        tertiary = palette.accent,
        onTertiary = palette.onAccent,
        background = palette.background,
        onBackground = palette.textPrimary,
        surface = palette.surface,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.surfaceContainer,
        onSurfaceVariant = palette.textSecondary,
        outline = palette.borderAccent
    )

    CompositionLocalProvider(LocalTabRemotePalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
