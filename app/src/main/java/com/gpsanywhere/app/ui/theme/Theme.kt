package com.gpsanywhere.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.gpsanywhere.app.settings.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = MilkTeaPrimary,
    secondary = MilkTeaSecondary,
    tertiary = MilkTeaAccent,
    background = MilkTeaBackground,
    surface = MilkTeaCard,
    error = ErrorRed,
    primaryContainer = MilkTeaSecondary,
    onPrimaryContainer = MilkTeaText,
    secondaryContainer = Color(0xFFF3E8DC),
    onSecondaryContainer = MilkTeaText,
    tertiaryContainer = Color(0xFFF0D8CB),
    onTertiaryContainer = MilkTeaText,
    onPrimary = MilkTeaText,
    onSecondary = MilkTeaText,
    onTertiary = MilkTeaText,
    onBackground = MilkTeaText,
    onSurface = MilkTeaText,
    surfaceVariant = Color(0xFFF1E8DF),
    onSurfaceVariant = MilkTeaMuted,
    outline = MilkTeaBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = MilkTeaAccent,
    secondary = MilkTeaSecondary,
    tertiary = MilkTeaPrimary,
    background = MilkTeaBackgroundDark,
    surface = MilkTeaSurfaceDark,
    error = ErrorRed,
    primaryContainer = Color(0xFF5D4D43),
    onPrimaryContainer = MilkTeaTextOnDark,
    secondaryContainer = Color(0xFF4E433B),
    onSecondaryContainer = MilkTeaTextOnDark,
    tertiaryContainer = Color(0xFF614B41),
    onTertiaryContainer = MilkTeaTextOnDark,
    onPrimary = Color(0xFF2B2420),
    onSecondary = Color(0xFF2B2420),
    onTertiary = Color(0xFF2B2420),
    onBackground = MilkTeaTextOnDark,
    onSurface = MilkTeaTextOnDark,
    surfaceVariant = Color(0xFF4E433B),
    onSurfaceVariant = Color(0xFFD9C9BA),
    outline = Color(0xFF6F6258)
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
