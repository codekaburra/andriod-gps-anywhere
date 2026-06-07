# GPS Anywhere

GPS Anywhere is an Android app for testing fixed mock locations and simulated walking routes. It is built for developers, QA testers, and personal lab use where an app needs repeatable GPS input without depending on Google Play Services.

> This project is for development and testing only. Mocking GPS can violate app terms, game rules, platform policies, or local regulations. Use it only on devices and services where you have permission.

## What It Does

- Sets a fixed mock GPS coordinate from a map tap, pasted coordinates, or manual latitude/longitude input.
- Runs route-based walking simulation through a foreground service.
- Supports adjustable walking speed, speed variation, pause/resume, stop, and waypoint jumps.
- Shows OSMDroid maps without requiring Google Maps or Google Play Services.
- Stores saved routes locally with Room.
- Uses Health Connect step permissions so simulated movement can also write step data where supported.
- Provides onboarding instructions for enabling Android mock location mode.

## App Flow

The current app shell has two main tabs:

- `Location` - choose a single coordinate and start fixed-location spoofing.
- `Route` - choose or save walking routes, adjust speed, and start route simulation.

When spoofing is active, `SpoofService` runs as an Android foreground service and keeps pushing mock locations through Android's test provider APIs.

## Tech Stack

| Area | Choice |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| Maps | OSMDroid |
| Routing | OSRM public server, no API key |
| Persistence | Room + KSP |
| Network | OkHttp |
| Serialization | Gson |
| Background work | Android Foreground Service |
| Mock GPS | `LocationManager.addTestProvider()` |
| Steps | Health Connect |

Project package: `com.gpsanywhere.app`

Minimum SDK: API 26

Target/compile SDK: API 36

## Getting Started

Clone the project and open it in Android Studio.

```bash
git clone <repo-url>
cd gpsanywhere
./gradlew assembleDebug
```

You can also build and run directly from Android Studio.

## Required Device Setup

The app will install without this setup, but GPS spoofing will not work until Android allows the app to provide mock locations.

1. Enable Developer Options on the Android device.
2. Open `Settings -> Developer Options`.
3. Find `Select mock location app`.
4. Choose `GPS Anywhere`.
5. Launch the app and grant requested location, notification, and Health Connect permissions as needed.

For route and map features, the device also needs internet access for map tiles and OSRM route data.

## Common Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run local unit tests
./gradlew test

# Run Android instrumentation tests
./gradlew connectedAndroidTest

# Clean build outputs
./gradlew clean
```

## Project Structure

```text
app/src/main/java/com/gpsanywhere/app/
├── MainActivity.kt
├── data/              # Room entities, DAO, database, route seed data
├── directions/        # OSRM and Nominatim clients
├── location/          # Current device location provider
├── routes/            # Route/location domain models and generators
├── service/           # SpoofService foreground mock-location service
├── settings/          # SharedPreferences-backed app settings
├── ui/
│   ├── components/    # Shared Compose components and map wrappers
│   ├── location/      # Fixed-location spoofing screen
│   ├── navigation/    # Routes and root app navigation
│   ├── onboarding/    # First-run help dialog
│   ├── theme/         # Compose theme
│   └── walk/          # Route/walking simulation screen
├── util/              # Coordinate parsing helpers
└── viewmodel/         # AndroidViewModel classes for app screens
```

## Permissions

The Android manifest requests:

- `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` for location access and map centering.
- `ACCESS_MOCK_LOCATION` for mock GPS injection.
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_LOCATION` for long-running spoofing.
- `POST_NOTIFICATIONS` for foreground service status on Android 13+.
- `INTERNET` and `ACCESS_NETWORK_STATE` for map tiles and routing.
- `android.permission.health.READ_STEPS` and `android.permission.health.WRITE_STEPS` for Health Connect step support.

## Build Notes

This project uses AGP 9 and the Kotlin Compose compiler plugin. A few details are intentional:

- Do not add the `org.jetbrains.kotlin.android` plugin separately. AGP 9 already bundles Kotlin integration.
- Compose uses `org.jetbrains.kotlin.plugin.compose`.
- Room uses KSP through `ksp(libs.androidx.room.compiler)`.
- `gradle.properties` includes `android.disallowKotlinSourceSets=false` for KSP compatibility.
- Java compatibility is set through `compileOptions`; there is no `kotlinOptions.jvmTarget` block.

## Routing Notes

GPS Anywhere uses OSMDroid for maps and OSRM for route data. The public OSRM server is free and does not need an API key, but it is rate-limited and intended for light usage. For heavy or commercial use, self-host OSRM or switch to a routing backend you control.

## Development Status

The project currently focuses on fixed-location spoofing, route playback, saved/default route handling, speed controls, foreground service behavior, and coordinate parsing. Some older planning docs mention additional screens and polish tasks; treat the source code as the latest truth for what is active in the app.

## Responsible Use

Use GPS Anywhere for app development, QA, demos, and controlled testing. Do not use it to bypass location restrictions, misrepresent your location to services, or gain unfair advantages in apps or games.
