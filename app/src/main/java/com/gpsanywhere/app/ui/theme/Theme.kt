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
    primary = CandyOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFF3E2723),
    secondary = CandyGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF1B5E20),
    tertiary = CandyBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBBDEFB),
    onTertiaryContainer = Color(0xFF0D47A1),
    background = CandyBackground,
    onBackground = CandyText,
    surface = CandySurface,
    onSurface = CandyText,
    surfaceVariant = CandySurfaceVariant,
    onSurfaceVariant = CandyMuted,
    outline = CandyBorder,
    outlineVariant = Color(0xFFEEEEEE),
    error = StopRed
)

private val DarkColorScheme = darkColorScheme(
    primary = GoldenCopper,
    onPrimary = Color(0xFF1A0E05),
    primaryContainer = Color(0xFF5A3A1F),
    onPrimaryContainer = GoldenTextOnDark,
    secondary = GoldenCream,
    onSecondary = Color(0xFF1A1A12),
    secondaryContainer = Color(0xFF3A3422),
    onSecondaryContainer = GoldenTextOnDark,
    tertiary = GoldenSlate,
    onTertiary = GoldenTextOnDark,
    tertiaryContainer = Color(0xFF2C362C),
    onTertiaryContainer = GoldenTextOnDark,
    background = GoldenBackgroundDark,
    onBackground = GoldenTextOnDark,
    surface = GoldenSurfaceDark,
    onSurface = GoldenTextOnDark,
    surfaceVariant = GoldenSurfaceVariantDark,
    onSurfaceVariant = GoldenMutedOnDark,
    outline = GoldenBorderDark,
    outlineVariant = Color(0xFF2E2E22),
    error = ErrorRed
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
