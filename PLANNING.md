# GPS Anywhere — Project Planning

## App Info
- App name: GPS Anywhere
- Package: com.gpsanywhere.app
- Language: Kotlin
- Min SDK: API 24 (Android 7.0 Nougat)
- Target: Android 7–8 era hardware (no heavy animations)

## Tech Stack
- UI: Jetpack Compose + Material 3
- Maps: OSMDroid (no Google Play Services dependency)
- Route generation: OSRM (Open Source Routing Machine) — free, no API key needed
- Local storage: Room database (saved routes)
- Mock location: LocationManager + addTestProvider()
- Background: Android Foreground Service
- Theme: Light / Dark / System (Compose + AppCompat)

## OSRM Routing API
- Uses the public OSRM demo server: https://router.project-osrm.org
- Completely free, no API key required
- Walking profile: /route/v1/foot/
- Returns route geometry as polyline
- Rate-limited on public server (sufficient for personal use)
- Note: For heavy usage, can self-host OSRM server

## Permissions (AndroidManifest.xml)
- ACCESS_FINE_LOCATION
- ACCESS_COARSE_LOCATION
- ACCESS_MOCK_LOCATION
- FOREGROUND_SERVICE
- INTERNET
- ACCESS_NETWORK_STATE

---

## Feature 1 — Master Spoof Toggle
- Big prominent ON/OFF toggle button on main screen (like a VPN switch)
- OFF state: app uses real GPS, button clearly shows inactive
- ON state: spoofing active, button visually highlighted
- Status label below button: "Spoofing Active" or "Using Real GPS"
- Turning OFF instantly stops all spoofing and restores real GPS
- Toggle state synced with background notification

## Feature 2 — Set Mock Location
- Two ways to pick a location:
    1. Tap directly on OSMDroid map (tap-to-pin)
    2. Manually type latitude and longitude in input fields
- Both methods must exist and work independently
- Start / Pause / Stop controls

## Feature 3 — Walk Route Simulation
Three methods to build a route:

Method A — Manual waypoints:
- User taps map to drop numbered pins
- Line connects pins in order showing route preview
- User can delete or reorder waypoints

Method B — OSRM auto route (free):
- User types Start and End point (coordinates, or address via Nominatim geocoding)
- App calls OSRM API in walking/foot mode via HTTP
- Route drawn on map for preview
- User confirms then starts simulation

Method C — Manual coordinate input:
- User types coordinates one by one to build waypoint list

Shared walk settings:
- Walking speed slider (km/h)
- Smooth GPS interpolation between waypoints (no teleporting)
- Start / Pause / Stop controls

## Feature 4 — Saved Routes
- Save any route with a custom name
- Saved routes list shows:
    - Route name
    - Small map preview thumbnail
    - Distance and number of waypoints
- Edit a saved route (name, waypoints, speed)
- Delete a saved route
- Load a saved route and start walking immediately
- Store with Room database

## Feature 5 — Background Notification
- Persistent status bar notification when spoofing is active
- Shows current spoofed coordinates or active route name
- Tap notification → returns to app
- Stop button inside notification
- Implemented as Android Foreground Service

## Feature 6 — Theme Toggle
- Default: follow system (auto light/dark)
- Button in top right corner to switch between Light / Dark / System
- Preference saved to SharedPreferences, remembered on next launch

## Onboarding
- First launch screen or help dialog instructing user to:
    1. Enable Developer Options on their phone
    2. Go to Developer Options → Select Mock Location App → choose GPS Anywhere