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
    // Accent colours — same warm toffee tones as light mode
    primary = MilkTeaAccent,           // #DDAA7A — buttons, icons, highlights
    secondary = MilkTeaAccentDeep,     // #C99464 — secondary actions
    tertiary = MilkTeaAccent,

    // Backgrounds — layered from darkest to lightest
    background = MilkTeaBackgroundDark,        // #12100E — app canvas
    surface = MilkTeaSurfaceDark,              // #1C1815 — cards / panels
    surfaceVariant = MilkTeaSurfaceVariantDark, // #262019 — input fields, chips

    // Containers (tinted surfaces for buttons, tags, etc.)
    primaryContainer = Color(0xFF3A2A1A),      // warm dark container for accent elements
    onPrimaryContainer = MilkTeaTextOnDark,    // #F3EFE8
    secondaryContainer = Color(0xFF2C2118),    // slightly cooler container
    onSecondaryContainer = MilkTeaTextOnDark,
    tertiaryContainer = Color(0xFF332519),
    onTertiaryContainer = MilkTeaTextOnDark,

    // Text on accents — near-black for contrast on warm accent buttons
    onPrimary = MilkTeaBackgroundDark,         // #12100E
    onSecondary = MilkTeaBackgroundDark,
    onTertiary = MilkTeaBackgroundDark,

    // Text on backgrounds/surfaces
    onBackground = MilkTeaTextOnDark,          // #F3EFE8 — primary text
    onSurface = MilkTeaTextOnDark,             // #F3EFE8
    onSurfaceVariant = MilkTeaMutedOnDark,     // #C5B4A5 — secondary / muted text

    // Borders & errors
    outline = MilkTeaBorderDark,               // #3D342E
    error = ErrorRed
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
