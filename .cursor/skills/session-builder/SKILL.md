---
name: session-builder
description: Generates a complete, ready-to-paste agent prompt for GPS Anywhere BUILD_PLAN sessions 2–6. Use when the user says "build session 2", "start session 3", "next session", or wants to implement the next BUILD_PLAN step. Outputs a full context-rich prompt referencing existing files, what's already done, and exactly what to implement.
---

# GPS Anywhere — Session Builder

When the user asks to build or start a session, identify the session number and output the prompt below for that session. Do not implement anything yourself — output the prompt for the user to paste into a new agent chat.

## How to Use

1. User says e.g. "build session 2" or "next session"
2. Identify the session number
3. Output the **Ready-to-Paste Prompt** for that session
4. Tell the user to paste it into a new Agent mode chat

---

## Session Status

| Session | Feature | UI Status | Backend Status |
|---------|---------|-----------|----------------|
| 1 | Scaffold + Toggle | Done | Done |
| 2 | Set Mock Location | Shell done | Needs pause + edge cases |
| 3 | Walk Route Simulation | Shell done | Needs interpolation engine |
| 4 | Saved Routes | Shell done | Needs map thumbnails |
| 5 | Background Service Polish | Partial | Needs route tick loop |
| 6 | Theme + Final Polish | Partial | Needs Settings screen |

---

## Ready-to-Paste Prompts

### Session 2 — Set Mock Location

```
Please read PLANNING.md, BUILD_PLAN.md, and the skill at .cursor/skills/gpsanywhere-dev/SKILL.md first.

Key files already built:
- ui/location/LocationScreen.kt — Compose UI shell exists
- viewmodel/LocationViewModel.kt — start/stop wired to SpoofService
- ui/components/MapViewComposable.kt — OSMDroid wrapper
- service/SpoofService.kt — startFixed() and stop() work

SESSION 2 GOAL: Feature 2 — Set Mock Location (polish and complete)

Tasks:
1. Ensure tap-to-pin and manual lat/lng input stay in sync bidirectionally
2. Implement Pause: when paused, hold the last spoofed coordinate (do not stop the provider)
3. Validate coordinate input — show error if out of range or non-numeric
4. After Start, update the notification text with current lat/lng
5. If mock location app is NOT selected in Developer Options, show a clear error dialog with instructions
6. Make sure Stop cleanly removes the test provider and resets HomeScreen toggle

Make both input methods (map tap and manual typing) work independently and stay in sync.
```

---

### Session 3 — Walk Route Simulation

```
Please read PLANNING.md, BUILD_PLAN.md, and the skill at .cursor/skills/gpsanywhere-dev/SKILL.md first.

Key files already built:
- ui/route/RouteScreen.kt — Manual Pins + OSRM tabs Compose UI exists
- viewmodel/RouteViewModel.kt — waypoints, OSRM fetch, speed wired
- directions/OsrmClient.kt — fetches walking route from router.project-osrm.org
- routes/LocationPoint.kt — data class for coordinates

SESSION 3 GOAL: Feature 3 — Walk Route Simulation (add interpolation engine)

Tasks:
1. Create routes/RouteSimulationEngine.kt:
   - Takes List<LocationPoint> and speed in km/h
   - Uses a clock/elapsed-time approach to interpolate position smoothly between waypoints
   - Supports pause/resume without position jump
   - Emits current LocationPoint and completion percentage
   - Must be testable (use a clock abstraction, not System.currentTimeMillis directly)

2. Wire SpoofService to tick the engine:
   - On ACTION_START_ROUTE: accept waypoints + speed, start a coroutine loop
   - Each tick: get current interpolated point, call setMockLocation()
   - On pause: freeze position, stop ticking
   - On stop: clean up provider and cancel coroutine

3. Enhance RouteScreen Method C (manual coordinate list):
   - Form to add coordinates one by one
   - Scrollable list of added waypoints with delete per item

4. Update notification to show route name + progress % during walk
```

---

### Session 4 — Saved Routes

```
Please read PLANNING.md, BUILD_PLAN.md, and the skill at .cursor/skills/gpsanywhere-dev/SKILL.md first.

Key files already built:
- ui/saved/SavedRoutesScreen.kt — card list UI exists, Play/Edit/Delete wired
- viewmodel/SavedRoutesViewModel.kt — Room CRUD via RouteDao
- data/SavedRoute.kt, RouteDao.kt, AppDatabase.kt — Room skeleton done
- data/WaypointJson.kt — Gson serialization helper

SESSION 4 GOAL: Feature 4 — Saved Routes (add map thumbnails + polish)

Tasks:
1. Add static map thumbnail to each SavedRouteCard:
   - Generate a Bitmap snapshot of the route polyline using OSMDroid's MapView (render off-screen, do NOT use a live MapView in the list)
   - Cache the bitmap in memory by route ID; regenerate if route is updated
   - Show as small square image in the card (do not use AsyncImage from Coil — keep it simple with an AndroidView or remember { bitmap })

2. Ensure Save route flow from RouteScreen works end-to-end:
   - Dialog collects name, saves to Room, list updates immediately via LiveData

3. Load & Start: tap Play on a saved route → load waypoints into RouteViewModel → navigate to Route tab → auto-start walk

4. Edit: load route into RouteViewModel with existing waypoints, speed, method → navigate to Route tab

5. Verify Room persists routes across app restart
```

---

### Session 5 — Background Service Polish

```
Please read PLANNING.md, BUILD_PLAN.md, and the skill at .cursor/skills/gpsanywhere-dev/SKILL.md first.

Key files already built:
- service/SpoofService.kt — foreground service with notification, start/stop, mock location provider

SESSION 5 GOAL: Feature 5 — Background Notification + Foreground Service polish

Tasks:
1. Harden SpoofService lifecycle:
   - Handle SecurityException on every provider call (user may revoke or not select mock app)
   - On app killed (onTaskRemoved): decide whether to keep running or stop cleanly
   - On battery saver / Doze: ensure service survives on Android 7–8

2. Notification content:
   - Fixed location mode: "Spoofing: 25.0330° N, 121.5654° E"
   - Route walk mode: "Route Name — 42% complete"
   - Update notification in real time as position changes (use NotificationManager.notify)

3. Notification tap: Intent with FLAG_ACTIVITY_SINGLE_TOP, opens correct tab based on mode

4. Notification Stop action: stops service, removes mock provider, posts isRunning = false

5. HomeScreen toggle must reflect service state when app is reopened after being backgrounded
   - On Activity resume: re-observe SpoofService.isRunning LiveData
```

---

### Session 6 — Theme + Final Polish

```
Please read PLANNING.md, BUILD_PLAN.md, and the skill at .cursor/skills/gpsanywhere-dev/SKILL.md first.

Key files already built:
- ui/theme/ — Color.kt, Type.kt, Theme.kt (Material 3 palette)
- settings/ThemeMode.kt + AppPreferences.kt — theme cycle and persistence
- ui/home/HomeScreen.kt — theme toggle button exists

SESSION 6 GOAL: Feature 6 — Theme Toggle + final polish + Settings screen

Tasks:
1. Verify theme toggle on HomeScreen cycles correctly: System → Light → Dark → System
   - Icon: sun (Light), moon (Dark), auto (System)
   - Applies immediately without restart
   - Persists across app restart

2. Add Settings/About screen:
   - App version (from BuildConfig.VERSION_NAME)
   - Button: "Show setup instructions" → re-shows OnboardingDialog
   - Theme selector (secondary access point)
   - Add as 5th tab or accessible from HomeScreen top bar

3. Light/Dark readability audit:
   - All text readable on both backgrounds
   - OSMDroid map overlay colors visible in dark mode
   - Card surfaces use correct Material 3 surface tokens

4. Navigation and state sanity check:
   - Bottom nav tab state restored correctly after backgrounding
   - Master toggle stays in sync after service stop via notification
   - Onboarding only appears on first launch
   - Saved routes survive app restart
```
