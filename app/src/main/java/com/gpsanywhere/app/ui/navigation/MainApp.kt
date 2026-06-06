package com.gpsanywhere.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gpsanywhere.app.settings.AppPreferences
import com.gpsanywhere.app.settings.ThemeMode
import com.gpsanywhere.app.ui.home.HomeScreen
import com.gpsanywhere.app.ui.location.LocationScreen
import com.gpsanywhere.app.ui.onboarding.OnboardingDialog
import com.gpsanywhere.app.ui.steps.StepsScreen
import com.gpsanywhere.app.ui.theme.GPSAnywhereTheme
import com.gpsanywhere.app.ui.walk.WalkScreen
import com.gpsanywhere.app.viewmodel.MainViewModel
import com.gpsanywhere.app.viewmodel.SavedLocationsViewModel
import com.gpsanywhere.app.viewmodel.StepsViewModel
import com.gpsanywhere.app.viewmodel.WalkViewModel

@Composable
fun MainApp(preferences: AppPreferences) {
    val mainViewModel: MainViewModel = viewModel()
    val savedLocationsViewModel: SavedLocationsViewModel = viewModel()
    val walkViewModel: WalkViewModel = viewModel()
    val stepsViewModel: StepsViewModel = viewModel()

    val themeMode by mainViewModel.themeMode.observeAsState(ThemeMode.SYSTEM)
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.WALK

    var showOnboarding by remember { mutableStateOf(!preferences.onboardingShown) }

    LaunchedEffect(Unit) { mainViewModel.loadTheme() }

    GPSAnywhereTheme(themeMode = themeMode) {
        Scaffold(
            bottomBar = {
                NavigationBar {
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
                        label = { Text("Location") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.WALK,
                        onClick = { nav(Routes.WALK) },
                        icon = { Icon(Icons.Default.DirectionsWalk, contentDescription = "Walk") },
                        label = { Text("Walk") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.STEPS,
                        onClick = { nav(Routes.STEPS) },
                        icon = { Icon(Icons.Default.DirectionsRun, contentDescription = "Steps") },
                        label = { Text("Steps") }
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
                    LocationScreen(viewModel = savedLocationsViewModel)
                }
                composable(Routes.WALK) {
                    WalkScreen(viewModel = walkViewModel)
                }
                composable(Routes.STEPS) {
                    StepsScreen(viewModel = stepsViewModel)
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
