# GPS Anywhere

Mock GPS locations and walking routes on Android — built for developers and QA, without Google Play Services.

> **Development and testing only.** Spoofing GPS may violate other apps' terms of service or local regulations. Use responsibly.

---

## What it does

GPS Anywhere lets you override your device's reported location so you can test location-aware apps under controlled conditions. Set a fixed coordinate, pick from bundled preset locations, or replay a walking route with adjustable speed — all from a foreground service that keeps spoofing active while you switch apps.

The app uses Android's official mock-location API (`LocationManager.addTestProvider()`). It does not root your device or hook into other processes.

---

## Screenshots

| Location | Route list | Walk simulation |
|----------|------------|-----------------|
| Map preview, saved locations by region, coordinate input | Preset routes, speed slider, speed variation | Live map, waypoint progress, Start / Stop |

<p align="center">
  <img src="docs/screenshots/Screenshot_20260603_233437.png" width="30%" alt="Location screen">
  &nbsp;&nbsp;
  <img src="docs/screenshots/Screenshot_20260603_233446.png" width="30%" alt="Route list">
  &nbsp;&nbsp;
  <img src="docs/screenshots/Screenshot_20260603_233457.png" width="30%" alt="Walk simulation">
</p>

---

## Features

### Location tab

- OSMDroid map centred on current GPS
- Tap the map, paste Google Maps coordinates, or type lat/long manually
- Bundled **Saved Locations** grouped by region (Hong Kong, Japan, Taiwan, and more)
- **Recent locations** with rename, swipe-to-delete, and one-tap restore
- Start / Pause / Stop controls wired to the foreground spoof service

### Route tab (Walk)

- Preset walking routes seeded from bundled CSV assets
- Base speed slider (1–20 km/h) with optional min/max variation
- Route detail view with live map tracking and waypoint distances
- Start simulation to interpolate GPS along the route; Stop restores real location

### Background service

- `SpoofService` runs as a foreground service with a persistent notification
- Shared spoof state (`isRunning`, current coordinates) observable across screens

### UI

- Jetpack Compose + Material 3
- Light / Dark / System theme (persisted via `AppPreferences`)

---

## Requirements

| | |
|---|---|
| **Min SDK** | API 24 (Android 7.0) |
| **Package** | `com.gpsanywhere.app` |
| **IDE** | Android Studio Ladybug or later recommended |
| **Device** | Physical device or emulator with mock-location app configured |

---

## Setup

### 1. Clone and open

```bash
git clone <your-repo-url>
cd gpsanywhere
```

Open the project in Android Studio and let Gradle sync finish.

### 2. Enable mock location on the device

This step is **required**. Without it the app installs but spoofing has no effect.

1. **Settings → About phone** — tap **Build number** seven times to enable Developer options.
2. **Settings → Developer options → Select mock location app**
3. Choose **GPS Anywhere**

On first launch, an onboarding dialog walks through the same steps.

### 3. Build and run

```bash
./gradlew assembleDebug
```

Or press **Run** in Android Studio.

---

## Usage

### Set a fixed location

1. Open the **Location** tab.
2. Choose a point — tap the map, paste coordinates (`25.0457, 121.5764`), or enter lat/long fields.
3. Tap **Start Spoofing**.
4. **Pause** temporarily restores real GPS without ending the session.
5. **Stop** ends spoofing and returns to the real location.

Saved and recent locations can be tapped to load them back into the map instantly.

### Simulate a walk

1. Open the **Route** tab.
2. Adjust base speed and optional speed variation.
3. Tap a preset route to open the walk view.
4. Review waypoints on the map, then tap **Start**.
5. Tap **Stop** when finished.

---

## Architecture

```
MainActivity
└── MainApp (Compose)
    ├── GPSAnywhereTheme
    ├── Bottom nav: Location | Route
    └── NavHost
        ├── LocationScreen  ← LocationViewModel
        └── WalkScreen      ← WalkViewModel

SpoofService (foreground)
├── addTestProvider() mock GPS injection
└── companion LiveData — shared spoof state

Data
├── Room — SavedRoute, SavedLocation entities
├── CSV seeders — bundled locations and routes
└── AppPreferences — theme, onboarding flags
```

### Tech stack

| Layer | Choice |
|-------|--------|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Maps | OSMDroid (no Play Services) |
| Routing | OSRM public server — free, no API key |
| Database | Room via KSP |
| HTTP | OkHttp (OSRM, Nominatim) |
| Background | Android Foreground Service |

### Key directories

```
app/src/main/java/com/gpsanywhere/app/
├── ui/location/       LocationScreen
├── ui/walk/           WalkScreen
├── ui/components/     MapViewComposable, CoordinateCard, …
├── ui/navigation/     MainApp, Routes
├── viewmodel/         LocationViewModel, WalkViewModel, MainViewModel
├── service/           SpoofService
├── data/              Room entities, DAOs, CSV seeders
├── directions/        OsrmClient, NominatimClient
├── settings/          AppPreferences, ThemeMode
└── util/              CoordinateUtils
```

---

## Permissions

| Permission | Purpose |
|------------|---------|
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Read GPS for map centreing |
| `ACCESS_MOCK_LOCATION` | Inject mock coordinates |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_LOCATION` | Background spoofing |
| `POST_NOTIFICATIONS` | Persistent notification (Android 13+) |
| `INTERNET` / `ACCESS_NETWORK_STATE` | Map tiles and OSRM requests |

---

## Build notes (AGP 9 + Kotlin 2.0)

- Do **not** add `org.jetbrains.kotlin.android` — AGP 9 bundles Kotlin.
- Compose needs the separate `org.jetbrains.kotlin.plugin.compose` plugin.
- Room uses **KSP**, not kapt: `ksp(libs.androidx.room.compiler)`.
- Set `android.disallowKotlinSourceSets=false` in `gradle.properties`.
- Use `compileOptions` only; `kotlinOptions { jvmTarget }` is removed in AGP 9.

---

## Roadmap

- [ ] Route builder — manual pins and OSRM auto-route on a dedicated editor screen
- [ ] Saved routes UI — create, edit, delete custom routes with map thumbnails
- [ ] Notification polish — live route progress in the foreground notification
- [ ] Settings / About screen and final theme pass

See `BUILD_PLAN.md` for the session-by-session development plan.

---

## Disclaimer

This project is for **educational and internal testing purposes only**. The authors are not responsible for misuse.

Built with Jetpack Compose and OSMDroid.
