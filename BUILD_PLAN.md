# Build Plan — 6 Claude Code Sessions

## Session 1 — Project Scaffold + Master Toggle

```
Please read PLANNING.md and all existing project files 
first to understand the full scope, then implement the following.

SESSION 1 GOAL: Project scaffold + Feature 1 (Master Spoof Toggle)

Tasks:
1. Set up all dependencies in build.gradle:
   - OSMDroid
   - Room database
   - Kotlin coroutines
   - Any other dependencies needed for the full project

2. Create AndroidManifest.xml with all required permissions:
   - ACCESS_FINE_LOCATION
   - ACCESS_COARSE_LOCATION
   - ACCESS_MOCK_LOCATION
   - FOREGROUND_SERVICE
   - FOREGROUND_SERVICE_LOCATION (Android 10+)
   - POST_NOTIFICATIONS (Android 13+)
   - INTERNET
   - ACCESS_NETWORK_STATE
   - Foreground Service declaration with android:foregroundServiceType="location"

3. Create the Room database skeleton:
   - SavedRoute entity
   - RouteDao
   - AppDatabase

5. Create the Foreground Service skeleton (SpoofService.kt)
   - Start/stop mock location
   - Show/dismiss notification

6. Create the main screen (MainActivity + Jetpack Compose) with:
   - HomeScreen: big ON/OFF power toggle, status label, coordinate card
   - Bottom navigation: Home / Location / Route / Saved
   - Theme toggle on HomeScreen (System / Light / Dark)

7. Create onboarding/help dialog that shows on first launch
   instructing user to enable Developer Options and set Mock Location App

Do not implement the other features yet. 
Just build a solid foundation for everything to plug into.
```

## Session 2 — Set Mock Location

```
Please read PLANNING.md and all existing project files 
first to understand the current structure, then implement 
the following on top of what is already built.

SESSION 2 GOAL: Feature 2 — Set Mock Location (LocationScreen.kt)

Note: LocationScreen Compose UI exists. Polish map pin sync, pause behavior, and edge cases.

Tasks:
1. Enhance LocationScreen with:
   - OSMDroid map taking up the top half of the screen
   - Tap anywhere on map to drop a pin at that location
   - Latitude and longitude input fields below the map
   - Tapping map updates the input fields automatically
   - Typing in input fields moves the pin on the map
   - Start / Pause / Stop buttons
   
2. Wire up the Start button to:
   - Pass the selected coordinates to SpoofService
   - Activate the Master Toggle (set to ON)
   - Start the Foreground Service with the spoofed location
   - Update the notification with current coordinates

3. Wire up Stop to:
   - Stop SpoofService
   - Set Master Toggle to OFF
   - Restore real GPS

Make sure both input methods (map tap and manual typing) 
work independently and stay in sync with each other.
```

## Session 3 — Walk Route Simulation

```
Please read PLANNING.md and all existing project files 
first to understand the current structure, then implement 
the following on top of what is already built.

SESSION 3 GOAL: Feature 3 — Walk Route Simulation (RouteScreen.kt)

Note: RouteScreen Compose UI exists with Manual Pins + OSRM tabs. Add route interpolation engine.

Tasks:
1. Enhance RouteScreen with:
   - OSMDroid map at the top
   - Tab or toggle to switch between 3 route building methods

2. Method A — Manual waypoints:
   - Tap map to drop numbered pins (1, 2, 3...)
   - Draw a line connecting pins in order
   - Button to delete last waypoint
   - Button to clear all waypoints

3. Method B — OSRM auto route (free, no API key):
   - Start point input field (coordinates, or address via Nominatim geocoding)
   - End point input field (coordinates, or address via Nominatim geocoding)
   - "Get Route" button that calls OSRM API (https://router.project-osrm.org)
     in walking/foot mode via HTTP
   - Draw the returned route polyline on the map
   - Show distance and estimated time

4. Method C — Manual coordinate list:
   - Form to add coordinates one by one
   - Scrollable list showing added waypoints
   - Delete individual waypoints

5. Shared controls below the map:
   - Walking speed slider (1–15 km/h, default 4 km/h)
   - Start / Pause / Stop buttons
   - Save Route button (opens dialog to name and save the route)

6. Wire up Start to:
   - Pass the full waypoint list and speed to SpoofService
   - SpoofService smoothly interpolates GPS position 
     between waypoints based on speed (no teleporting)
   - Update notification with active route info
   - Activate Master Toggle
```

## Session 4 — Saved Routes

```
Please read PLANNING.md and all existing project files 
first to understand the current structure, then implement 
the following on top of what is already built.

SESSION 4 GOAL: Feature 4 — Saved Routes (SavedRoutesScreen.kt)

Note: SavedRoutesScreen Compose UI exists. Add static map preview thumbnails.

Tasks:
1. Enhance SavedRoutesScreen with:
   - Scrollable list of saved routes
   - Each list item shows:
     - Route name
     - Static map preview (avoid active OSMDroid maps in list for performance)
     - Total distance and number of waypoints
   - Empty state message when no routes saved yet

2. Each saved route card has three actions:
   - Load & Start — loads the route into Walk Route screen 
     and starts simulation immediately
   - Edit — opens the route in Walk Route screen for editing, 
     user can modify waypoints, speed, name and re-save
   - Delete — shows confirmation dialog before deleting

3. Save route flow (triggered from Walk Route screen):
   - Dialog asks user to enter a route name
   - Saves all waypoints, speed setting, and route method 
     to Room database
   - Saved route appears immediately in Saved Routes list

4. Make sure Room database correctly stores and retrieves:
   - Route name
   - List of waypoints (lat/lng sequence)
   - Walking speed
   - Route method used (A, B, or C)
   - Date saved
```

## Session 5 — Background Notification + Foreground Service

```
Please read PLANNING.md and all existing project files 
first to understand the current structure, then implement 
the following on top of what is already built.

SESSION 5 GOAL: Feature 5 — Polish the Background Notification 
and Foreground Service

Tasks:
1. Polish SpoofService (Foreground Service):
   - Make sure it runs reliably in the background on Android 7–8
   - Handle all edge cases: app killed, screen off, battery saver
   - Properly release mock location provider when stopped

2. Notification must show:
   - App icon and title "GPS Anywhere — Active"
   - When in Set Location mode: current spoofed lat/lng
   - When in Walk Route mode: route name + progress 
     (e.g. "Office to Mall — 40% complete")
   - Updates in real time as location changes

3. Notification actions:
   - Tap notification → opens app on correct screen
   - Stop button → stops spoofing, dismisses notification, 
     sets Master Toggle to OFF

4. Make sure Master Toggle on main screen always stays 
   in sync with service state:
   - If service is running → toggle shows ON
   - If service is stopped → toggle shows OFF
   - Works correctly even if user force-closes and reopens app
```

## Session 6 — Theme Toggle + Final Polish

```
Please read PLANNING.md and all existing project files 
first to understand the current structure, then implement 
the following on top of what is already built.

SESSION 6 GOAL: Feature 6 — Theme Toggle + final polish

Tasks:
1. Theme toggle on HomeScreen (Compose):
   - Cycles between: System default → Light → Dark → System default
   - Shows correct icon for current mode 
     (sun = light, moon = dark, auto = system)
   - Applies theme immediately without restarting the app
   - Saves preference to SharedPreferences
   - Reads preference on launch and applies correct theme

2. Final polish across all screens:
   - Make sure all screens look correct in both light and dark mode
   - Check all text is readable in both modes
   - Check OSMDroid map tiles look reasonable in both modes

3. Review and fix any rough edges:
   - Make sure bottom navigation switches screens correctly
   - Make sure Master Toggle stays in sync everywhere
   - Make sure onboarding dialog only shows on first launch
   - Test that saved routes persist after app restart

4. Add a Settings or About section (simple screen) with:
   - App version
   - Link to instructions for enabling Developer Options
   - Button to re-show onboarding dialog
   - Theme preference (alternative access point)
```