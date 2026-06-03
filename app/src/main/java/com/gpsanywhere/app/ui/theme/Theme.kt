package com.gpsanywhere.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.gpsanywhere.app.settings.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = GalaxyPrimary,
    secondary = GalaxyAccent,
    background = BackgroundLight,
    surface = SurfaceLight,
    error = ErrorRed,
    onPrimary = Color.White,
    onBackground = GalaxyPrimary,
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFE0E7FF),
    onSurfaceVariant = Color(0xFF475569)
)

private val DarkColorScheme = darkColorScheme(
    primary = GalaxyPrimaryLight,
    secondary = GalaxyAccent,
    background = GalaxyBackgroundDark,
    surface = GalaxySurfaceDark,
    error = ErrorRed,
    onPrimary = Color.White,
    onBackground = GalaxyTextOnDark,
    onSurface = GalaxyTextOnDark,
    surfaceVariant = Color(0xFF1F253D),
    onSurfaceVariant = Color(0xFF94A3B8)
)

@Composable
fun GPSAnywhereTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
