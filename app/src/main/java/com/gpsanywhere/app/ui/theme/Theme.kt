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
    /** A fill and the content colour that stays legible on it. */
    data class Pair(val container: Color, val content: Color)

    /** Map controls: the add button, paste, the D-pad. Accent role. */
    val action: Color
        @Composable get() = if (LocalIsDarkTheme.current) CandyYellow else LightAccent

    /** Content on [action]. */
    val onAction: Color
        @Composable get() = Color.White

    /** Primary buttons — the transport row, Settings imports. */
    val primaryAction: Pair
        @Composable get() = Pair(if (LocalIsDarkTheme.current) CandyGreen else LightPrimary, Color.White)

    /** Transport buttons before a coordinate is entered. */
    val primaryActionIdle: Color
        @Composable get() = if (LocalIsDarkTheme.current) GlassMutedDark else LightNeutralLight

    /** Start / go. */
    val start: Color
        @Composable get() = if (LocalIsDarkTheme.current) CandyGreen else LightPrimary

    /** Content drawn on top of [start]. */
    val onStart: Color
        @Composable get() = Color.White

    /** Stop and delete — the strongest colour, so destructive reads as destructive. */
    val stop: Color
        @Composable get() = if (LocalIsDarkTheme.current) StopRed else LightPrimary

    /** Edit. A step down from [stop] so the two icons are told apart. */
    val edit: Color
        @Composable get() = if (LocalIsDarkTheme.current) GlassIndigoLight else LightSecondary

    /** Pill behind the selected navigation item. */
    val navIndicator: Color
        @Composable get() = if (LocalIsDarkTheme.current) NavSelected.copy(alpha = 0.15f) else SelectedPill

    /** Selected list item — its border and icon. */
    val selected: Color
        @Composable get() = if (LocalIsDarkTheme.current) NavSelected else LightPrimary

    /** Selected navigation item. */
    val navSelected: Color
        @Composable get() = if (LocalIsDarkTheme.current) NavSelected else LightPrimary

    /** Unselected navigation items, and unselected icons generally. */
    val navUnselected: Color
        @Composable get() = if (LocalIsDarkTheme.current) GlassMutedDark else LightNeutralLight

    /** Map pins and other markers. */
    val marker: Color
        @Composable get() = if (LocalIsDarkTheme.current) GlassIndigoLight else LightNeutralDark

    /** The waypoint the walk is closest to. */
    val nearestWaypoint: Color
        @Composable get() = if (LocalIsDarkTheme.current) GlassIndigoLight else LightPrimary

    /**
     * Cards in a list. Translucent white rather than solid, so the textured
     * background reads through and the cards match the glass panels above them.
     */
    val cardFill: Color
        @Composable get() = if (LocalIsDarkTheme.current) CardFill else SurfaceWhite.copy(alpha = 0.42f)

    /** Card outline. */
    val cardBorder: Color
        @Composable get() = if (LocalIsDarkTheme.current) CardBorder else BorderSubtle

    /** The little tag pills on a location card. Accent role. */
    val tagChip: Pair
        @Composable get() = if (LocalIsDarkTheme.current) Pair(CardFill, GlassTextDark)
        else Pair(LightAccent.copy(alpha = 0.72f), Color.White)

    /** Low-emphasis buttons: back, reverse. */
    val neutral: Pair
        @Composable get() = if (LocalIsDarkTheme.current) Pair(GlassSurfaceDark, GlassTextDark)
        else Pair(SurfaceWhite, LightNeutralDark)

    /** Slider thumb and active track. */
    val slider: Color
        @Composable get() = if (LocalIsDarkTheme.current) SliderThumb else LightAccent
}



// Light theme roles map onto the autumn palette: burnt orange leads, sage carries
// the secondary actions, gold the tertiary accents, maple spice the errors.
private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDD8C9),
    onPrimaryContainer = MapleSpice,
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDFE2D0),
    onSecondaryContainer = LightNeutralDark,
    tertiary = LightAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF2E3C6),
    onTertiaryContainer = Color(0xFF54401A),
    background = GlassBackgroundLight,
    onBackground = GlassTextLight,
    surface = GlassSurfaceLight,
    onSurface = GlassTextLight,
    surfaceVariant = GlassSurfaceVariantLight,
    onSurfaceVariant = GlassMutedLight,
    outline = GlassBorderLight,
    outlineVariant = LightNeutralLight.copy(alpha = 0.4f),
    error = LightPrimary
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
