package com.gpsanywhere.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.outlined.Bookmarks
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
import com.gpsanywhere.app.ui.route.RouteScreen
import com.gpsanywhere.app.ui.saved.SavedRoutesScreen
import com.gpsanywhere.app.ui.theme.GPSAnywhereTheme
import com.gpsanywhere.app.viewmodel.LocationViewModel
import com.gpsanywhere.app.viewmodel.MainViewModel
import com.gpsanywhere.app.viewmodel.RouteViewModel
import com.gpsanywhere.app.viewmodel.SavedRoutesViewModel

@Composable
fun MainApp(preferences: AppPreferences) {
    val mainViewModel: MainViewModel = viewModel()
    val locationViewModel: LocationViewModel = viewModel()
    val routeViewModel: RouteViewModel = viewModel()
    val savedViewModel: SavedRoutesViewModel = viewModel()

    val themeMode by mainViewModel.themeMode.observeAsState(ThemeMode.SYSTEM)
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.HOME

    var showOnboarding by remember {
        mutableStateOf(!preferences.onboardingShown)
    }

    LaunchedEffect(Unit) {
        mainViewModel.loadTheme()
    }

    LaunchedEffect(RouteEditHolder.pendingRoute) {
        RouteEditHolder.pendingRoute?.let { route ->
            routeViewModel.loadRoute(route)
            RouteEditHolder.pendingRoute = null
            navController.navigate(Routes.ROUTE) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    GPSAnywhereTheme(themeMode = themeMode) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.HOME,
                        onClick = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.LOCATION,
                        onClick = {
                            navController.navigate(Routes.LOCATION) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.LocationOn, contentDescription = "Location") },
                        label = { Text("Location") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.ROUTE,
                        onClick = {
                            navController.navigate(Routes.ROUTE) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Route, contentDescription = "Route") },
                        label = { Text("Route") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SAVED,
                        onClick = {
                            navController.navigate(Routes.SAVED) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Outlined.Bookmarks, contentDescription = "Saved") },
                        label = { Text("Saved") }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        viewModel = mainViewModel,
                        onNavigateToLocation = {
                            navController.navigate(Routes.LOCATION) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(Routes.LOCATION) {
                    LocationScreen(
                        viewModel = locationViewModel,
                        preferences = preferences
                    )
                }
                composable(Routes.ROUTE) {
                    RouteScreen(viewModel = routeViewModel)
                }
                composable(Routes.SAVED) {
                    SavedRoutesScreen(
                        viewModel = savedViewModel,
                        onAddNew = {
                            navController.navigate(Routes.ROUTE) {
                                launchSingleTop = true
                            }
                        },
                        onEditRoute = { route ->
                            RouteEditHolder.pendingRoute = route
                            routeViewModel.loadRoute(route)
                            navController.navigate(Routes.ROUTE) {
                                launchSingleTop = true
                            }
                        }
                    )
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
