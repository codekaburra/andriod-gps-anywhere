# GPS Anywhere

A mock GPS toolkit for Android — set any location, walk any route, no root required.

> **Development & testing tool only.** GPS spoofing may violate other apps' terms of service or local regulations. Use responsibly.

---

## What It Does

GPS Anywhere turns your phone into a GPS simulator. Point at a spot on the map and your device believes it's there. Draw a route and your device walks it — complete with realistic speed variation and smooth interpolation between waypoints.

**Fixed location** — tap the map, paste coordinates from Google Maps, or type them in. One tap to start spoofing; every other app on your device sees the fake position.

**Route simulation** — build a walking path with manual pins or auto-generate one via OSRM. Set your speed (1–20 km/h), add natural variation, and press play. The GPS position moves smoothly along the route — no teleporting.

**Persistent service** — spoofing continues in the background with a foreground notification. Lock your screen, switch apps, it keeps going.

---

## Screenshots

<p align="center">
  <img src="docs/screenshots/Screenshot_20260603_233437.png" width="30%" alt="Location screen">
  &nbsp;&nbsp;
  <img src="docs/screenshots/Screenshot_20260603_233446.png" width="30%" alt="Walk screen — route list">
  &nbsp;&nbsp;
  <img src="docs/screenshots/Screenshot_20260603_233457.png" width="30%" alt="Walk screen — ready to start">
</p>

**Left:** Location tab — map preview centred on current GPS, saved locations grouped by region. Tap any entry to jump.

**Centre:** Walk tab — speed slider with tick marks, speed variation panel (min/max/vary), preset routes showing distance and stop count.

**Right:** Walk view — live map follows position, waypoint list with distances, Start button to begin simulation.

---

## Quick Start

### Prerequisites

- Android Studio (Ladybug or later)
- A device or emulator running Android 8.0+ (API 26)

### 1. Clone

```bash
git clone https://github.com/your-username/gpsanywhere.git
```

### 2. Configure Your Device

This step is **required** — without it, the app installs fine but spoofing silently does nothing.

1. Go to **Settings > About Phone** and tap **Build Number** 7 times to unlock Developer Options.
2. Go to **Settings > Developer Options > Select Mock Location App**.
3. Choose **GPS Anywhere**.

### 3. Build & Run

```bash
./gradlew assembleDebug
```

Or hit **Run** in Android Studio.

---

## Usage

### Spoofing a Fixed Location

1. Open the **Location** tab.
2. Choose a location:
   - **Tap the map** to drop a pin
   - **Paste** coordinates from Google Maps (e.g. `25.0457, 121.5764`)
   - **Type** lat/lng manually
3. Tap **Start Spoofing** — the master toggle flips on, the foreground service starts, and every app on the device sees the faked position.
4. **Pause** temporarily restores real GPS without ending the session.
5. **Stop** kills the service and restores real GPS.

### Walking a Route

1. Open the **Walk** tab.
2. Build a route using manual pins or import one via OSRM.
3. Set walking speed and optional speed variation.
4. Tap **Start** — your GPS position smoothly moves along the path.

### Location History

Every location you spoof is auto-saved. Tap to reload, swipe to delete, or rename entries with the pencil icon.

### Home Screen

The large power button is the master toggle. When spoofing is active, current coordinates are shown. The theme icon (top-right) cycles System / Light / Dark.

---

## Architecture

```
MainActivity
└── MainApp (Compose NavHost, bottom nav)
    ├── HomeScreen       ← MainViewModel
    ├── LocationScreen   ← LocationViewModel
    ├── RouteScreen      ← RouteViewModel
    └── SavedRoutesScreen ← SavedRoutesViewModel
```

- **Service layer:** `SpoofService` (foreground service) owns all active spoof state via `companion object` LiveData, observed across screens.
- **Data layer:** Room database (KSP) for saved routes. SharedPreferences for theme, onboarding, and compliance flags.
- **ViewModels:** All extend `AndroidViewModel`. No DI framework — just default `viewModel()` factories.

### Project Layout

```
app/src/main/java/com/gpsanywhere/app/
├── ui/
│   ├── home/           Home screen — master toggle, status
│   ├── location/       Map + bottom panel, tap-to-pin, Start/Stop
│   ├── route/          Manual pins / OSRM tabs, speed controls
│   ├── saved/          Saved route cards — play, edit, delete
│   ├── onboarding/     First-launch setup dialog
│   ├── components/     PowerToggleButton, CoordinateCard, MapViewComposable
│   ├── navigation/     NavHost, routes, route-edit holder
│   └── theme/          Material 3 colour scheme, typography
├── viewmodel/          One ViewModel per screen
├── service/            SpoofService — foreground mock location provider
├── data/               Room entities, DAO, waypoint JSON helper
├── directions/         OSRM walking route client (OkHttp)
├── settings/           Preferences — theme, onboarding, compliance
├── routes/             LocationPoint data class
└── util/               Coordinate parsing helpers
```

---

## Tech Stack

| Component | Choice |
|-----------|--------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Maps | OSMDroid — no Google Play Services dependency |
| Routing | OSRM (free, no API key) |
| Database | Room with KSP |
| Mock GPS | `LocationManager.addTestProvider()` |
| Background | Android Foreground Service |
| Networking | OkHttp + Gson |
| Build | AGP 9, Gradle version catalog |

**Target SDK:** 36 &nbsp;|&nbsp; **Min SDK:** 26 (Android 8.0)

---

## Permissions

| Permission | Purpose |
|------------|---------|
| `ACCESS_FINE_LOCATION` | Centre map on real GPS |
| `ACCESS_COARSE_LOCATION` | Fallback location |
| `ACCESS_MOCK_LOCATION` | Inject fake coordinates |
| `FOREGROUND_SERVICE` | Keep spoofing alive in background |
| `FOREGROUND_SERVICE_LOCATION` | Required on Android 10+ |
| `POST_NOTIFICATIONS` | Persistent notification on Android 13+ |
| `INTERNET` | Map tiles + OSRM route fetching |
| `ACCESS_NETWORK_STATE` | Pre-flight connectivity check |

---

## Build Notes

This project uses **AGP 9 + Kotlin 2.0**, which has some sharp edges:

- **Do not** add the `org.jetbrains.kotlin.android` plugin — AGP 9 bundles Kotlin internally and will throw a "duplicate extension" error.
- Compose needs `org.jetbrains.kotlin.plugin.compose` as a separate plugin.
- Room uses KSP, not kapt: `ksp(libs.androidx.room.compiler)`.
- Add `android.disallowKotlinSourceSets=false` to `gradle.properties` to avoid KSP source set conflicts.
- `kotlinOptions { jvmTarget }` no longer exists in AGP 9 — use `compileOptions` only.

---

## Roadmap

- [ ] Smooth GPS interpolation engine for walk routes
- [ ] Static map thumbnails on saved route cards
- [ ] Live route progress in background notification
- [ ] Settings / About screen with theme controls

---

## License

For educational and development testing purposes only.
