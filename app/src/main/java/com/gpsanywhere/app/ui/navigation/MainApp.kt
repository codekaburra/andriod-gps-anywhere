package com.gpsanywhere.app.ui.navigation

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gpsanywhere.app.settings.AppPreferences
import com.gpsanywhere.app.settings.ColorTheme
import com.gpsanywhere.app.settings.ThemeMode
import com.gpsanywhere.app.ui.location.LocationScreen
import com.gpsanywhere.app.ui.onboarding.OnboardingDialog
import com.gpsanywhere.app.ui.theme.GPSAnywhereTheme
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

    val themeMode by mainViewModel.themeMode.observeAsState(ThemeMode.SYSTEM)
    val colorTheme by mainViewModel.colorTheme.observeAsState(ColorTheme.COCOA_SAGE)
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.WALK

    var showOnboarding by remember { mutableStateOf(!preferences.onboardingShown) }

    LaunchedEffect(Unit) { mainViewModel.loadTheme() }

    GPSAnywhereTheme(themeMode = themeMode, colorTheme = colorTheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
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
                        icon = { Icon(Icons.Default.LocationOn, contentDescription = "Location") },
                        label = { Text("Location") },
                        colors = navigationItemColors()
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.WALK,
                        onClick = { nav(Routes.WALK) },
                        icon = { Icon(Icons.Default.Route, contentDescription = "Route") },
                        label = { Text("Route") },
                        colors = navigationItemColors()
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETTINGS,
                        onClick = { nav(Routes.SETTINGS) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        colors = navigationItemColors()
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.WALK,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Routes.LOCATION) {
                    LocationScreen(viewModel = locationViewModel)
                }
                composable(Routes.WALK) {
                    WalkScreen(viewModel = walkViewModel)
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

@Composable
private fun navigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
)
