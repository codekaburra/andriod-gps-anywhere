package com.gpsanywhere.app.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.R
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gpsanywhere.app.settings.AppLanguage
import com.gpsanywhere.app.settings.AppPreferences
import com.gpsanywhere.app.settings.ColorTheme
import com.gpsanywhere.app.settings.ThemeMode
import java.util.Locale
import com.gpsanywhere.app.ui.location.LocationScreen
import com.gpsanywhere.app.ui.onboarding.OnboardingDialog
import com.gpsanywhere.app.ui.theme.GPSAnywhereTheme
import com.gpsanywhere.app.ui.components.TexturedBackground
import com.gpsanywhere.app.ui.theme.AppAccent
import com.gpsanywhere.app.ui.theme.DustyRose
import com.gpsanywhere.app.ui.theme.GlassBackgroundLight
import com.gpsanywhere.app.ui.theme.GoldenTan
import com.gpsanywhere.app.ui.theme.SageGreen
import com.gpsanywhere.app.ui.theme.TerracottaBrown
import com.gpsanywhere.app.ui.theme.LocalIsDarkTheme
import com.gpsanywhere.app.ui.theme.GlassNavDark
import com.gpsanywhere.app.ui.theme.GlassNavLight
import com.gpsanywhere.app.ui.theme.NavUnselected
import com.gpsanywhere.app.ui.settings.SettingsScreen
import com.gpsanywhere.app.ui.walk.WalkScreen
import com.gpsanywhere.app.viewmodel.MainViewModel
import com.gpsanywhere.app.viewmodel.LocationViewModel
import com.gpsanywhere.app.viewmodel.WalkViewModel

@Composable
fun MainApp(preferences: AppPreferences) {
    val mainViewModel: MainViewModel = viewModel()
    val locationViewModel: LocationViewModel = viewModel()
    val walkViewModel: WalkViewModel = viewModel()

    val themeMode by mainViewModel.themeMode.observeAsState(ThemeMode.LIGHT)
    val colorTheme = if (themeMode == ThemeMode.DARK) ColorTheme.GOLDEN_HOUR else ColorTheme.COCOA_SAGE
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.LOCATION

    var showOnboarding by remember { mutableStateOf(!preferences.onboardingShown) }

    LaunchedEffect(Unit) { mainViewModel.loadTheme() }

    // In-app language override: provide a locale-adjusted context so every
    // stringResource() call below resolves against the chosen language.
    val appLanguage by mainViewModel.appLanguage.observeAsState(AppLanguage.SYSTEM)
    val baseContext = LocalContext.current
    val localizedContext = remember(appLanguage, baseContext) {
        if (appLanguage == AppLanguage.SYSTEM) baseContext
        else baseContext.createConfigurationContext(
            Configuration(baseContext.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(appLanguage.tag))
            }
        )
    }

    CompositionLocalProvider(LocalContext provides localizedContext) {
    GPSAnywhereTheme(themeMode = themeMode, colorTheme = colorTheme) {
      val isDark = LocalIsDarkTheme.current
      Box(modifier = Modifier.fillMaxSize()) {
        if (isDark) {
            Image(
                painter = painterResource(R.drawable.bg_dark),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Soften the artwork so the foreground glass UI stays legible.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
            )
        } else {
            // Light mode draws its texture procedurally from the theme colour, so
            // there is no bitmap to re-author when that colour changes.
            TexturedBackground(
                base = GlassBackgroundLight,
                accents = listOf(SageGreen, GoldenTan, TerracottaBrown, DustyRose),
                bloomAlpha = 0.30f
            )
        }
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                val navIsDark = isDark
                NavigationBar(
                    containerColor = if (navIsDark) GlassNavDark else GlassNavLight,
                    tonalElevation = 0.dp
                ) {
                    fun nav(route: String) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    NavigationBarItem(
                        selected = currentRoute == Routes.LOCATION,
                        onClick = { nav(Routes.LOCATION) },
                        icon = { Icon(Icons.Default.LocationOn, contentDescription = stringResource(R.string.nav_location)) },
                        label = { Text(stringResource(R.string.nav_location)) },
                        colors = navItemColors(AppAccent.navSelected)
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.WALK,
                        onClick = { nav(Routes.WALK) },
                        icon = { Icon(Icons.Default.Route, contentDescription = stringResource(R.string.nav_route)) },
                        label = { Text(stringResource(R.string.nav_route)) },
                        colors = navItemColors(AppAccent.navSelected)
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETTINGS,
                        onClick = { nav(Routes.SETTINGS) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings)) },
                        label = { Text(stringResource(R.string.nav_settings)) },
                        colors = navItemColors(AppAccent.navSelected)
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.LOCATION,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Routes.LOCATION) {
                    LocationScreen(viewModel = locationViewModel)
                }
                composable(Routes.WALK) {
                    WalkScreen(viewModel = walkViewModel, appLanguage = appLanguage)
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(viewModel = mainViewModel)
                }
            }
        }

        if (showOnboarding) {
            OnboardingDialog(
                onDismiss = {
                    preferences.onboardingShown = true
                    showOnboarding = false
                }
            )
        }
      }
    }
    }
}

@Composable
private fun navItemColors(activeColor: androidx.compose.ui.graphics.Color) = NavigationBarItemDefaults.colors(
    selectedIconColor = activeColor,
    selectedTextColor = activeColor,
    indicatorColor = activeColor.copy(alpha = 0.15f),
    unselectedIconColor = NavUnselected,
    unselectedTextColor = NavUnselected
)
