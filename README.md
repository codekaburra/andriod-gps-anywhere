# GPS Anywhere

An Android app for setting a mock GPS location or simulating a walking route, built entirely without Google Play Services.

> **For development & testing use only.** GPS spoofing may violate the terms of service of other apps and local laws. Use responsibly.

---

## Features

| # | Feature | Status |
|---|---------|--------|
| 1 | Master spoof toggle (ON/OFF) | ✅ Done |
| 2 | Set mock location — map tap, manual input, paste from Google Maps | ✅ Done |
| 3 | Walk route simulation (manual pins, OSRM auto-route, coordinate list) | 🔄 Planned |
| 4 | Saved routes with Room database | 🔄 Planned |
| 5 | Background foreground service + notification | ✅ Basic |
| 6 | Light / Dark / System theme toggle | ✅ Done |

---

## Tech Stack

| Layer | Library |
|-------|---------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Maps | OSMDroid (no Google Play Services) |
| Routing | OSRM public server — free, no API key |
| Storage | Room (KSP) |
| Mock GPS | `LocationManager.addTestProvider()` |
| Background | Android Foreground Service |
| Theme | AppCompat `setDefaultNightMode` + Compose |

**Min SDK:** API 24 (Android 7.0)  
**Package:** `com.gpsanywhere.app`

---

## Getting Started

### 1. Clone and open

```bash
git clone <your-repo-url>
```

Open the project in Android Studio (Ladybug or later recommended).

### 2. Device setup — required before the app works

The app uses Android's mock location API. You **must** configure your device first:

1. Enable **Developer Options**
   - Go to *Settings → About Phone*
   - Tap **Build Number** 7 times
2. Go to *Settings → Developer Options*
3. Find **Select Mock Location App**
4. Choose **GPS Anywhere**

> Without this step the app installs but spoofing will silently do nothing.

### 3. Build and run

```bash
./gradlew assembleDebug
```

Or press **Run** in Android Studio.

---

## How to Use

### Set a Fixed Location

1. Open the **Location** tab
2. Pick a location — any of three ways:
   - **Tap the map** to drop a pin
   - **Paste** a coordinate copied from Google Maps (`25.0457, 121.5764` format)
   - **Type** latitude and longitude manually
3. Tap **Start Spoofing**
4. Use **Pause** to temporarily restore real GPS without ending the session
5. Tap **Stop** to restore real GPS permanently

### Recent Locations

- Every successfully started location is saved to a history list at the bottom of the Location tab
- **Tap** any entry to load it back into the map and fields
- **Swipe left** or tap **×** to delete an entry
- Tap the **pencil icon** to give it a name (e.g. "Home", "Office")
- **Clear all** removes the full history

### Home Screen

- The big **power button** toggles spoofing on/off
- When active, the current spoofed coordinates are shown
- The **theme icon** (top-right) cycles between System / Light / Dark

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
│   ├── navigation/     MainApp.kt, Routes.kt
│   └── theme/          Color.kt, Type.kt, Theme.kt
├── viewmodel/
│   ├── MainViewModel.kt
│   ├── LocationViewModel.kt
│   ├── RouteViewModel.kt
│   └── SavedRoutesViewModel.kt
├── service/
│   └── SpoofService.kt         ← Foreground service, mock location provider
├── data/
│   ├── AppDatabase.kt
│   ├── RouteDao.kt
│   ├── SavedRoute.kt
│   └── WaypointJson.kt
├── settings/
│   ├── AppPreferences.kt       ← Theme, onboarding, compliance flags
│   ├── LocationHistoryStore.kt ← Recent locations (SharedPrefs + Gson)
│   └── ThemeMode.kt
├── directions/
│   └── OsrmClient.kt           ← OSRM walking route fetcher
└── routes/
    └── LocationPoint.kt
```

---

## Permissions

The app requests the following Android permissions:

| Permission | Why |
|------------|-----|
| `ACCESS_FINE_LOCATION` | Read current GPS for map centering |
| `ACCESS_COARSE_LOCATION` | Fallback location |
| `ACCESS_MOCK_LOCATION` | Inject mock GPS coordinates |
| `FOREGROUND_SERVICE` | Keep spoofing alive in the background |
| `FOREGROUND_SERVICE_LOCATION` | Required on Android 10+ for location foreground services |
| `POST_NOTIFICATIONS` | Show the persistent spoofing notification on Android 13+ |
| `INTERNET` | Load OSMDroid map tiles and fetch OSRM routes |
| `ACCESS_NETWORK_STATE` | Check connectivity before OSRM requests |

---

## Known Build Notes (AGP 9 + Kotlin 2.0)

- Do **not** add `org.jetbrains.kotlin.android` plugin — AGP 9 bundles Kotlin and will error
- Compose requires `org.jetbrains.kotlin.plugin.compose` as a separate plugin
- Room requires KSP (`ksp(libs.androidx.room.compiler)`) — not kapt
- Add `android.disallowKotlinSourceSets=false` to `gradle.properties` to avoid KSP source set conflicts
- `kotlinOptions { jvmTarget }` is removed in AGP 9 — use only `compileOptions`

---

## Roadmap

- [ ] **Session 3** — Walk route simulation with smooth GPS interpolation
- [ ] **Session 4** — Saved routes with static map thumbnails
- [ ] **Session 5** — Polished background notification with live progress
- [ ] **Session 6** — Settings/About screen, final light/dark polish
