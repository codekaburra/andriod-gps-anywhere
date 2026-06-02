---
name: gpsanywhere-dev
description: GPS Anywhere Android project skill. Covers architecture, tech stack, known AGP 9 + Kotlin 2.0 build issues, screen/ViewModel structure, BUILD_PLAN session order, and how to extend the codebase. Use when working on this project, adding new features, fixing build errors, implementing BUILD_PLAN sessions, or asking about how any part of the app is structured.
---

# GPS Anywhere — Developer Skill

## Project at a Glance

| Item | Value |
|------|-------|
| Package | `com.gpsanywhere.app` |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Min SDK | 24 (Android 7.0) |
| Maps | OSMDroid (no Google Play Services) |
| Routing | OSRM public server — free, no API key |
| Storage | Room database via KSP |
| Mock GPS | `LocationManager.addTestProvider()` |
| Background | `SpoofService` — Android Foreground Service |

## Architecture

```
MainActivity (ComponentActivity)
└── MainApp (Compose)
    ├── GPSAnywhereTheme (Color/Type/Theme.kt)
    ├── NavigationBar (4 tabs: Home, Location, Route, Saved)
    └── NavHost
        ├── HomeScreen       ← MainViewModel
        ├── LocationScreen   ← LocationViewModel
        ├── RouteScreen      ← RouteViewModel
        └── SavedRoutesScreen ← SavedRoutesViewModel
```

**Service layer (singleton state):**
- `SpoofService` owns all active spoof state via `companion object` `LiveData`
- `SpoofService.isRunning`, `.currentLat`, `.currentLng` — observed across all screens
- Call `SpoofService.startFixed(context, lat, lng)` / `SpoofService.stop(context)` from any ViewModel

**Data layer:**
- `AppDatabase` (Room singleton) → `RouteDao` → `SavedRoute` entity
- Waypoints stored as JSON string via `WaypointJson` (Gson)
- `AppPreferences` — SharedPreferences wrapper for theme, onboarding, compliance flags

## Key File Locations

| File | Purpose |
|------|---------|
| `ui/navigation/MainApp.kt` | Root Compose shell, NavHost, 4-tab nav |
| `ui/navigation/Routes.kt` | Route string constants |
| `ui/navigation/RouteEditHolder.kt` | Singleton to pass SavedRoute → RouteScreen edit |
| `ui/theme/` | Color.kt, Type.kt, Theme.kt (Material 3 palette) |
| `ui/components/` | PowerToggleButton, CoordinateCard, MapViewComposable |
| `ui/home/HomeScreen.kt` | Toggle, status, coordinate card, theme button |
| `ui/location/LocationScreen.kt` | Map + bottom panel, tap-to-pin, Start/Stop |
| `ui/route/RouteScreen.kt` | Manual Pins / OSRM segmented tabs, speed slider |
| `ui/saved/SavedRoutesScreen.kt` | Route cards, Play/Edit/Delete |
| `ui/onboarding/OnboardingDialog.kt` | 3-step HorizontalPager |
| `service/SpoofService.kt` | Foreground service, mock location provider |
| `viewmodel/` | One VM per screen (AndroidViewModel, no Hilt) |
| `directions/OsrmClient.kt` | OkHttp call to router.project-osrm.org/route/v1/foot/ |
| `data/` | Room entities, DAO, WaypointJson helper |
| `settings/AppPreferences.kt` | Theme, onboarding, compliance preferences |
| `routes/LocationPoint.kt` | `data class LocationPoint(lat, lng)` |

## Theme System

- `ThemeMode` enum: `SYSTEM`, `LIGHT`, `DARK`
- `AppPreferences.themeMode` persists choice; calls `AppCompatDelegate.setDefaultNightMode()`
- `GPSAnywhereTheme(themeMode)` in `MainApp` applies Compose color scheme
- `MainViewModel.cycleTheme()` → cycles `SYSTEM → LIGHT → DARK → SYSTEM`
- On Android 12+ with `ThemeMode.SYSTEM`, dynamic color is used

## OSMDroid in Compose

Always use `MapViewComposable` wrapper (never instantiate `MapView` directly in screens):

```kotlin
MapViewComposable(
    modifier = Modifier.fillMaxWidth().weight(0.55f),
    center = GeoPoint(lat, lng),
    waypoints = listOf(LocationPoint(lat, lng)),
    showNumberedPins = true,       // numbered markers for route mode
    onMapClick = { point -> }      // null = map not tappable
)
```

- One map per screen only — never put MapView inside LazyColumn rows
- Lifecycle handled via `DisposableEffect` + `LifecycleEventObserver`
- OSMDroid tile cache configured automatically via `Configuration.getInstance()`

## OSRM Route Fetching

```kotlin
// In RouteViewModel:
viewModelScope.launch {
    val result = osrmClient.fetchWalkingRoute(startPoint, endPoint)
    // result.waypoints, result.distanceKm, result.durationMinutes
}
```

- Runs on `Dispatchers.IO` inside `OsrmClient`
- Returns `OsrmRouteResult` with full polyline waypoints
- On error throws `IllegalStateException` — catch in ViewModel, post to `_errorMessage`

## BUILD_PLAN Session Order

Sessions 2–6 are in `BUILD_PLAN.md`. Compose UI shells are already built for all screens. Remaining work per session:

| Session | Key remaining work |
|---------|--------------------|
| **2** | Pause behavior for fixed location, map pin sync edge cases |
| **3** | `RouteSimulationEngine` — smooth interpolation in `SpoofService` |
| **4** | Static map thumbnail on saved route cards; Nominatim geocoding |
| **5** | `SpoofService` route tick loop, notification progress update |
| **6** | Settings/About screen, verify light/dark on all screens |

## Known Build Gotchas (AGP 9 + Kotlin 2.0)

**DO NOT add `org.jetbrains.kotlin.android` plugin** — AGP 9 bundles Kotlin and will error with "extension 'kotlin' already registered".

**Compose requires** `org.jetbrains.kotlin.plugin.compose` (separate from kotlin.android):
```toml
# libs.versions.toml
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version = "2.0.21" }
```

**Room requires KSP** (not `annotationProcessor` or `kapt`):
```toml
ksp = { id = "com.google.devtools.ksp", version = "2.0.21-1.0.28" }
```
```kotlin
// app/build.gradle.kts
ksp(libs.androidx.room.compiler)
```

**KSP source set conflict** — add to `gradle.properties`:
```properties
android.disallowKotlinSourceSets=false
```

**`kotlinOptions { jvmTarget }` is removed** in AGP 9 — use only `compileOptions` block.

## ViewModels

All ViewModels are `AndroidViewModel(application)`. No Hilt. Use default `viewModel()` composable factory in `MainApp.kt` — it handles `AndroidViewModel` automatically.

To add a new ViewModel for a new screen:
1. Create `viewmodel/FooViewModel.kt` extending `AndroidViewModel`
2. Call `val fooViewModel: FooViewModel = viewModel()` inside the composable in `MainApp`
3. Pass it as parameter to the screen composable

## Adding a New Screen

1. Add route string to `Routes.kt`
2. Add `NavigationBarItem` in `MainApp.kt` bottom bar
3. Add `composable(Routes.FOO) { FooScreen(...) }` in NavHost
4. Create `ui/foo/FooScreen.kt`
5. Create `viewmodel/FooViewModel.kt` if needed
