package com.gpsanywhere.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.gpsanywhere.app.settings.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = MilkTeaAccent,
    secondary = MilkTeaAccentDeep,
    tertiary = MilkTeaSecondary,
    background = MilkTeaBackground,
    surface = MilkTeaCard,
    error = ErrorRed,
    primaryContainer = Color(0xFFF3E8DC),
    onPrimaryContainer = MilkTeaText,
    secondaryContainer = Color(0xFFF0D8CB),
    onSecondaryContainer = MilkTeaText,
    tertiaryContainer = Color(0xFFE6D5C3),
    onTertiaryContainer = MilkTeaText,
    onPrimary = Color(0xFF12100E),
    onSecondary = Color(0xFF12100E),
    onTertiary = MilkTeaText,
    onBackground = MilkTeaText,
    onSurface = MilkTeaText,
    surfaceVariant = Color(0xFFF1E8DF),
    onSurfaceVariant = MilkTeaMuted,
    outline = MilkTeaBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = MilkTeaAccent,
    secondary = MilkTeaAccentDeep,
    tertiary = MilkTeaAccentDeep,
    background = MilkTeaBackgroundDark,
    surface = MilkTeaSurfaceDark,
    error = ErrorRed,
    primaryContainer = Color(0xFF3D2E22),
    onPrimaryContainer = MilkTeaTextOnDark,
    secondaryContainer = MilkTeaSurfaceVariantDark,
    onSecondaryContainer = MilkTeaTextOnDark,
    tertiaryContainer = Color(0xFF4A3828),
    onTertiaryContainer = MilkTeaTextOnDark,
    onPrimary = Color(0xFF12100E),
    onSecondary = Color(0xFF12100E),
    onTertiary = Color(0xFF12100E),
    onBackground = MilkTeaTextOnDark,
    onSurface = MilkTeaTextOnDark,
    surfaceVariant = MilkTeaSurfaceVariantDark,
    onSurfaceVariant = MilkTeaMutedOnDark,
    outline = MilkTeaBorderDark
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
