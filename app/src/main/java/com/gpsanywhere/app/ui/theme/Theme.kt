package com.gpsanywhere.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.gpsanywhere.app.settings.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = AccentGreen,
    background = BackgroundLight,
    surface = SurfaceLight,
    error = ErrorRed,
    onPrimary = SurfaceLight,
    onBackground = PrimaryBlue,
    onSurface = PrimaryBlue
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLight,
    secondary = AccentGreen,
    background = BackgroundDark,
    surface = SurfaceDark,
    error = ErrorRed,
    onPrimary = SurfaceLight,
    onBackground = SurfaceLight,
    onSurface = SurfaceLight
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

    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && themeMode == ThemeMode.SYSTEM -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
