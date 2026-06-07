# GPS Anywhere

> Spoof your Android GPS — fixed points, walking routes, and full background simulation. Built for developers and testers, no Google Play Services required.

---

## What It Does

GPS Anywhere lets you override your Android device's location with a fake one. Use it to test location-dependent apps, simulate walk routes, or replay saved paths — all without moving a step.

- **Fixed location spoofing** — drop a pin on the map, paste from Google Maps, or type coordinates manually
- **Walk route simulation** — build a route from waypoints or let OSRM generate one, then simulate walking it at a configurable speed *(in development)*
- **Saved routes** — name, store, and replay routes with a single tap *(in development)*
- **Background service** — spoofing persists with a foreground notification even when the screen is off
- **No API keys, no sign-in** — OSMDroid tiles and OSRM routing are both open and free

---

## Screenshots

<p align="center">
  <img src="docs/screenshots/Screenshot_20260603_233437.png" width="30%" alt="Location screen">
  &nbsp;&nbsp;
  <img src="docs/screenshots/Screenshot_20260603_233446.png" width="30%" alt="Walk screen — route list">
  &nbsp;&nbsp;
  <img src="docs/screenshots/Screenshot_20260603_233457.png" width="30%" alt="Walk screen — ready to start">
</p>

---

## UI Design Mockups

| Home | Location | Route | Saved Routes |
|------|----------|-------|--------------|
| ✅ Built | ✅ Built | 🔄 In progress | 🔄 In progress |

<p align="center">
  <img src="docs/ui-mock-up/iMlXF.jpg" width="22%" alt="Home">
  <img src="docs/ui-mock-up/S7N89.jpg" width="22%" alt="Location">
  <img src="docs/ui-mock-up/7uvcV.jpg" width="22%" alt="Route">
  <img src="docs/ui-mock-up/ua4Rp.jpg" width="22%" alt="Saved Routes">
</p>

---

## Setup

### Requirements

- Android Studio Ladybug or later
- Android device or emulator running API 24+ (Android 7.0+)
- Developer Options enabled on the target device

### 1. Clone the repo

```bash
git clone <your-repo-url>
```

Open the project root in Android Studio and let Gradle sync.

### 2. Enable mock location on your device

This step is mandatory — without it the app installs but spoofing silently does nothing.

1. Go to **Settings → About Phone**
2. Tap **Build Number** seven times to unlock Developer Options
3. Go to **Settings → Developer Options**
4. Tap **Select Mock Location App** → choose **GPS Anywhere**

### 3. Build and install

```bash
./gradlew assembleDebug
```

Or press **Run ▶** in Android Studio.

---

## How to Use

### Fixed location (Location tab)

1. Open the **Location** tab
2. Choose your fake location in one of three ways:
   - **Tap the map** to drop a pin
   - **Paste** coordinates copied from Google Maps (e.g. `25.0457, 121.5764`)
   - **Type** latitude and longitude directly into the fields
3. Tap **Start Spoofing**
4. Use **Pause** to temporarily let real GPS through without ending the session
5. Tap **Stop** to fully restore real GPS

### Home screen

- The large **power button** is the master toggle — ON means spoofing is active
- Active coordinates are shown beneath the button when spoofing is running
- The **theme icon** (top-right) cycles between System / Light / Dark

### Location history

- Every successfully started location is auto-saved to the history list at the bottom of the Location tab
- Tap any entry to reload it
- Swipe left or tap **×** to remove an entry
- Tap the **pencil icon** to give it a friendly name like "Home" or "Office"

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Maps | OSMDroid (no Google Play Services) |
| Routing | OSRM public API — free, no key needed |
| Database | Room via KSP |
| Mock GPS | `LocationManager.addTestProvider()` |
| Background | Android Foreground Service |
| Theme | AppCompat `setDefaultNightMode` + Compose |

**Min SDK:** API 24 (Android 7.0)  
**Package:** `com.gpsanywhere.app`

---

## Project Structure

```
app/src/main/java/com/gpsanywhere/app/
├── ui/
│   ├── home/           HomeScreen.kt
│   ├── location/       LocationScreen.kt
│   ├── route/          RouteScreen.kt
│   ├── saved/          SavedRoutesScreen.kt
│   ├── onboarding/     OnboardingDialog.kt
│   ├── components/     PowerToggleButton, CoordinateCard, MapViewComposable
│   ├── navigation/     MainApp.kt, Routes.kt, RouteEditHolder.kt
│   └── theme/          Color.kt, Type.kt, Theme.kt
├── viewmodel/
│   ├── MainViewModel.kt
│   ├── LocationViewModel.kt
│   ├── RouteViewModel.kt
│   └── SavedRoutesViewModel.kt
├── service/
│   └── SpoofService.kt         ← foreground service + mock location provider
├── data/
│   ├── AppDatabase.kt
│   ├── RouteDao.kt
│   ├── SavedRoute.kt
│   └── WaypointJson.kt
├── settings/
│   ├── AppPreferences.kt       ← theme, onboarding, compliance flags
│   ├── LocationHistoryStore.kt ← recent locations (SharedPrefs + Gson)
│   └── ThemeMode.kt
├── directions/
│   └── OsrmClient.kt           ← OSRM walking route fetcher
└── routes/
    └── LocationPoint.kt
```

---

## Permissions

| Permission | Purpose |
|------------|---------|
| `ACCESS_FINE_LOCATION` | Read real GPS to centre the map |
| `ACCESS_COARSE_LOCATION` | Fallback when fine location is unavailable |
| `ACCESS_MOCK_LOCATION` | Inject fake coordinates into the system |
| `FOREGROUND_SERVICE` | Keep spoofing alive in the background |
| `FOREGROUND_SERVICE_LOCATION` | Required on Android 10+ for location foreground services |
| `POST_NOTIFICATIONS` | Show the persistent spoofing notification on Android 13+ |
| `INTERNET` | Load OSMDroid map tiles and call the OSRM routing API |
| `ACCESS_NETWORK_STATE` | Check connectivity before making network requests |

---

## Build Notes (AGP 9 + Kotlin 2.0)

A few non-obvious gotchas when building or modifying the project:

- **Do not add `org.jetbrains.kotlin.android`** — AGP 9 already bundles Kotlin; adding it causes an "extension 'kotlin' already registered" error
- **Compose needs its own plugin:** `org.jetbrains.kotlin.plugin.compose` (separate from `kotlin.android`)
- **Room requires KSP** — use `ksp(libs.androidx.room.compiler)`, not `kapt`
- **KSP source set conflict** — add `android.disallowKotlinSourceSets=false` to `gradle.properties`
- **`kotlinOptions { jvmTarget }` is gone in AGP 9** — use `compileOptions` only

---

## Roadmap

- [ ] **Session 3** — Walk route simulation with smooth GPS interpolation along OSRM polylines
- [ ] **Session 4** — Saved routes with static map thumbnails and Nominatim reverse geocoding
- [ ] **Session 5** — Live route progress in the background notification
- [ ] **Session 6** — Settings / About screen, final light & dark mode polish

---

## Disclaimer

This app is intended for **development and testing purposes only**. GPS spoofing may violate the terms of service of other applications or local regulations. Use responsibly.

---

*Built with Jetpack Compose and OSMDroid.*
