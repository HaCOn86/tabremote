package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RubyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A0006),
    onPrimaryContainer = RubySecondary,
    secondary = RubyAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF261217),
    onSecondaryContainer = Color(0xFFFFDADE),
    tertiary = RubyAccent,
    onTertiary = Color.White,
    background = BackgroundPitch,
    onBackground = TextPrimary,
    surface = SurfaceCharcoal,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceContainerCharcoal,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorderRuby
)

@Composable
fun TabRemoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
