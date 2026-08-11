package com.gpsanywhere.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.settings.ColorTheme
import com.gpsanywhere.app.settings.ThemeMode

/**
 * Whether the app is rendering its dark theme.
 *
 * Read this instead of inferring the theme from `colorScheme.background.luminance()`:
 * the light background is a mid-tone cream whose luminance sits below the 0.5
 * threshold, so a luminance guess reports "dark" while the light theme is active.
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

/**
 * Accent colours that follow the active theme.
 *
 * Light mode draws from the nature-inspired palette so the whole screen stays in
 * one family; dark mode keeps the original warm accents, which already read well
 * against the dark artwork.
 */
object AppAccent {
    /** Icon buttons and other primary actions. */
    val action: Color
        @Composable get() = if (LocalIsDarkTheme.current) CandyYellow else GoldenTan

    /** Content drawn on top of [action]. */
    val onAction: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color.White else OnWarmAccent

    /** Start / go. */
    val start: Color
        @Composable get() = if (LocalIsDarkTheme.current) CandyGreen else SageGreen

    /** Content drawn on top of [start]. */
    val onStart: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color.White else OnWarmAccent

    /** Stop / delete. */
    val stop: Color
        @Composable get() = if (LocalIsDarkTheme.current) StopRed else DustyRose

    /** Selected navigation item. */
    val navSelected: Color
        @Composable get() = if (LocalIsDarkTheme.current) NavSelected else TerracottaBrown

    /** Slider thumb and active track. */
    val slider: Color
        @Composable get() = if (LocalIsDarkTheme.current) SliderThumb else GoldenTan
}

// Light theme roles map onto the nature-inspired palette: terracotta leads,
// sage carries the secondary actions, golden tan the tertiary accents.
private val LightColorScheme = lightColorScheme(
    primary = TerracottaBrown,
    onPrimary = Color(0xFFFBF4E9),
    primaryContainer = Color(0xFFEBD9C2),
    onPrimaryContainer = Color(0xFF3F2E22),
    secondary = SageGreen,
    onSecondary = Color(0xFF23281C),
    secondaryContainer = Color(0xFFDDE3D2),
    onSecondaryContainer = Color(0xFF2F3627),
    tertiary = GoldenTan,
    onTertiary = Color(0xFF3A2C17),
    tertiaryContainer = Color(0xFFF0E1C8),
    onTertiaryContainer = Color(0xFF4A3821),
    background = GlassBackgroundLight,
    onBackground = GlassTextLight,
    surface = GlassSurfaceLight,
    onSurface = GlassTextLight,
    surfaceVariant = GlassSurfaceVariantLight,
    onSurfaceVariant = GlassMutedLight,
    outline = GlassBorderLight,
    outlineVariant = TerracottaBrown.copy(alpha = 0.18f),
    error = DustyRose
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

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = AppShapes,
            typography = Typography,
            content = content
        )
    }
}
