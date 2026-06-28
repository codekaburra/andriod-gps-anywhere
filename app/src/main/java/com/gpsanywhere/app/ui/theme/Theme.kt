package com.gpsanywhere.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.settings.ColorTheme
import com.gpsanywhere.app.settings.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = GlassIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE3D2),
    onPrimaryContainer = Color(0xFF6B2E14),
    secondary = GlassGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = Color(0xFF06B6D4),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFFAFE),
    onTertiaryContainer = Color(0xFF155E75),
    background = GlassBackgroundLight,
    onBackground = GlassTextLight,
    surface = GlassSurfaceLight,
    onSurface = GlassTextLight,
    surfaceVariant = GlassSurfaceVariantLight,
    onSurfaceVariant = GlassMutedLight,
    outline = GlassBorderLight,
    outlineVariant = Color(0xFFE2E8F0),
    error = StopRed
)

private val DarkColorScheme = darkColorScheme(
    primary = GlassIndigoLight,
    onPrimary = Color(0xFF3A1A0A),
    primaryContainer = Color(0xFF7A3A1E),
    onPrimaryContainer = GlassTextDark,
    secondary = Color(0xFF6EE7B7),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = GlassTextDark,
    tertiary = Color(0xFF22D3EE),
    onTertiary = Color(0xFF0F172A),
    tertiaryContainer = Color(0xFF164E63),
    onTertiaryContainer = GlassTextDark,
    background = GlassBackgroundDark,
    onBackground = GlassTextDark,
    surface = GlassSurfaceDark,
    onSurface = GlassTextDark,
    surfaceVariant = GlassSurfaceVariantDark,
    onSurfaceVariant = GlassMutedDark,
    outline = GlassBorderDark,
    outlineVariant = Color(0xFF334155),
    error = StopRed
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun GPSAnywhereTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    colorTheme: ColorTheme = ColorTheme.COCOA_SAGE,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        typography = Typography,
        content = content
    )
}
