package com.gpsanywhere.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.gpsanywhere.app.settings.ColorTheme
import com.gpsanywhere.app.settings.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = CocoaSagePrimary,
    secondary = CocoaSageSage,
    tertiary = CocoaSageTerracotta,
    background = CocoaSageBackground,
    surface = CocoaSageSurface,
    error = ErrorRed,
    primaryContainer = Color(0xFFE8D7CA),
    onPrimaryContainer = CocoaSageCharcoal,
    secondaryContainer = Color(0xFFD7DDD5),
    onSecondaryContainer = CocoaSageCharcoal,
    tertiaryContainer = Color(0xFFECD8CA),
    onTertiaryContainer = CocoaSageCharcoal,
    onPrimary = Color(0xFFFFF8F0),
    onSecondary = Color(0xFFFFF8F0),
    onTertiary = CocoaSageCharcoal,
    onBackground = CocoaSageText,
    onSurface = CocoaSageText,
    surfaceVariant = CocoaSageSurfaceVariant,
    onSurfaceVariant = CocoaSageMuted,
    outline = CocoaSageBorder,
    outlineVariant = Color(0xFFD6DBD2)
)

private val DarkColorScheme = darkColorScheme(
    primary = CocoaSagePrimary,
    secondary = CocoaSageSage,
    tertiary = CocoaSageCocoa,
    background = CocoaSageBackgroundDark,
    surface = CocoaSageSurfaceDark,
    surfaceVariant = CocoaSageSurfaceVariantDark,
    primaryContainer = Color(0xFF123C43),
    onPrimaryContainer = CocoaSageTextOnDark,
    secondaryContainer = Color(0xFF26382F),
    onSecondaryContainer = CocoaSageTextOnDark,
    tertiaryContainer = Color(0xFF3B2B23),
    onTertiaryContainer = CocoaSageTextOnDark,
    onPrimary = Color(0xFF031113),
    onSecondary = Color(0xFF071013),
    onTertiary = CocoaSageTextOnDark,
    onBackground = CocoaSageTextOnDark,
    onSurface = CocoaSageTextOnDark,
    onSurfaceVariant = CocoaSageMutedOnDark,
    outline = CocoaSageBorderDark,
    outlineVariant = Color(0xFF263638),
    error = ErrorRed
)

// ── Champagne Toast ──────────────────────────────────────────────────────────

private val ChampagneLightColorScheme = lightColorScheme(
    primary = ChampagneTerra,
    secondary = ChampagneTan,
    tertiary = ChampagnePeach,
    background = ChampagneBackground,
    surface = ChampagneSurface,
    error = ErrorRed,
    primaryContainer = Color(0xFFF5DDD2),
    onPrimaryContainer = Color(0xFF3B1A0E),
    secondaryContainer = Color(0xFFEDE2DE),
    onSecondaryContainer = Color(0xFF352825),
    tertiaryContainer = Color(0xFFF0E0D2),
    onTertiaryContainer = Color(0xFF3A2A1D),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFF3A2A1D),
    onBackground = ChampagneText,
    onSurface = ChampagneText,
    surfaceVariant = ChampagneSurfaceVariant,
    onSurfaceVariant = ChampagneMuted,
    outline = ChampagneBorder,
    outlineVariant = Color(0xFFE5DDD5)
)

private val ChampagneDarkColorScheme = darkColorScheme(
    primary = ChampagnePeach,
    secondary = ChampagneTan,
    tertiary = ChampagneTerra,
    background = ChampagneBackgroundDark,
    surface = ChampagneSurfaceDark,
    surfaceVariant = ChampagneSurfaceVariantDark,
    primaryContainer = Color(0xFF5A2E1E),
    onPrimaryContainer = ChampagneTextOnDark,
    secondaryContainer = Color(0xFF3E302C),
    onSecondaryContainer = ChampagneTextOnDark,
    tertiaryContainer = Color(0xFF4A2E20),
    onTertiaryContainer = ChampagneTextOnDark,
    onPrimary = Color(0xFF1A0E08),
    onSecondary = Color(0xFF1A1412),
    onTertiary = ChampagneTextOnDark,
    onBackground = ChampagneTextOnDark,
    onSurface = ChampagneTextOnDark,
    onSurfaceVariant = ChampagneMutedOnDark,
    outline = ChampagneBorderDark,
    outlineVariant = Color(0xFF3E3530),
    error = ErrorRed
)

// ── Golden Hour ──────────────────────────────────────────────────────────────

private val GoldenLightColorScheme = lightColorScheme(
    primary = GoldenCopper,
    secondary = GoldenOlive,
    tertiary = GoldenSlate,
    background = GoldenBackground,
    surface = GoldenSurface,
    error = ErrorRed,
    primaryContainer = Color(0xFFF2DEC9),
    onPrimaryContainer = Color(0xFF3A1F0E),
    secondaryContainer = Color(0xFFE8DEC9),
    onSecondaryContainer = Color(0xFF312A1B),
    tertiaryContainer = Color(0xFFD9E0D7),
    onTertiaryContainer = Color(0xFF1F2A22),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = GoldenText,
    onSurface = GoldenText,
    surfaceVariant = GoldenSurfaceVariant,
    onSurfaceVariant = GoldenMuted,
    outline = GoldenBorder,
    outlineVariant = Color(0xFFE2DAC8)
)

private val GoldenDarkColorScheme = darkColorScheme(
    primary = GoldenCopper,
    secondary = GoldenCream,
    tertiary = GoldenSlate,
    background = GoldenBackgroundDark,
    surface = GoldenSurfaceDark,
    surfaceVariant = GoldenSurfaceVariantDark,
    primaryContainer = Color(0xFF5A3A1F),
    onPrimaryContainer = GoldenTextOnDark,
    secondaryContainer = Color(0xFF3A3422),
    onSecondaryContainer = GoldenTextOnDark,
    tertiaryContainer = Color(0xFF2C362C),
    onTertiaryContainer = GoldenTextOnDark,
    onPrimary = Color(0xFF1A0E05),
    onSecondary = Color(0xFF1A1A12),
    onTertiary = GoldenTextOnDark,
    onBackground = GoldenTextOnDark,
    onSurface = GoldenTextOnDark,
    onSurfaceVariant = GoldenMutedOnDark,
    outline = GoldenBorderDark,
    outlineVariant = Color(0xFF2E2E22),
    error = ErrorRed
)

@Composable
fun GPSAnywhereTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorTheme: ColorTheme = ColorTheme.COCOA_SAGE,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when (colorTheme) {
        ColorTheme.COCOA_SAGE -> if (darkTheme) DarkColorScheme else LightColorScheme
        ColorTheme.CHAMPAGNE_TOAST -> if (darkTheme) ChampagneDarkColorScheme else ChampagneLightColorScheme
        ColorTheme.GOLDEN_HOUR -> if (darkTheme) GoldenDarkColorScheme else GoldenLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
