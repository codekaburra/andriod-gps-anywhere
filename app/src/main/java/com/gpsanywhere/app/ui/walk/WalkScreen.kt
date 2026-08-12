package com.gpsanywhere.app.ui.walk

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gpsanywhere.app.R
import com.gpsanywhere.app.data.SavedRoute
import com.gpsanywhere.app.data.WaypointJson
import com.gpsanywhere.app.routes.LocationPoint
import com.gpsanywhere.app.settings.AppLanguage
import com.gpsanywhere.app.ui.components.GlassCard
import com.gpsanywhere.app.ui.theme.AppAccent
import com.gpsanywhere.app.ui.theme.LocalIsDarkTheme
import com.gpsanywhere.app.ui.components.overlapAbove
import com.gpsanywhere.app.ui.components.MapViewComposable
import com.gpsanywhere.app.viewmodel.WalkViewModel
import org.osmdroid.util.GeoPoint

@Composable
fun WalkScreen(
    viewModel: WalkViewModel,
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
    modifier: Modifier = Modifier
) {
    val routes by viewModel.routes.observeAsState(emptyList())
    val isSpoofing by viewModel.isSpoofing.observeAsState(false)
    val isPaused by viewModel.isPaused.observeAsState(false)
    val speed by viewModel.speedKmh.collectAsState()
    val minSpeed by viewModel.minSpeedKmh.collectAsState()
    val maxSpeed by viewModel.maxSpeedKmh.collectAsState()
    val liveSpeed by viewModel.currentSpeedKmh.observeAsState(0f)
    val activeRoute by viewModel.activeRoute.collectAsState()
    val mapCenterLat by viewModel.mapCenterLat.observeAsState()
    val mapCenterLng by viewModel.mapCenterLng.observeAsState()



    // Route selected by the user — persists after stop so user stays in walk view
    var selectedRoute by remember { mutableStateOf<SavedRoute?>(null) }

    // Route editor state: when active, the editor screen replaces the list/walk view.
    var editorOpen by remember { mutableStateOf(false) }
    var editorTarget by remember { mutableStateOf<SavedRoute?>(null) }
    var deleteRouteTarget by remember { mutableStateOf<SavedRoute?>(null) }

    deleteRouteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteRouteTarget = null },
            title = { Text(stringResource(R.string.dialog_delete_route_title)) },
            text = { Text(stringResource(R.string.dialog_delete_route_text, target.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRoute(target)
                    deleteRouteTarget = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteRouteTarget = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (editorOpen) {
        RouteEditorScreen(
            initial = editorTarget,
            initialPoints = editorTarget?.let { viewModel.waypointsOf(it) } ?: emptyList(),
            mapCenter = mapCenterLat?.let { lat -> mapCenterLng?.let { lng -> GeoPoint(lat, lng) } },
            onCancel = { editorOpen = false; editorTarget = null },
            onSave = { name, points ->
                viewModel.saveCustomRoute(name, points, editorTarget)
                editorOpen = false
                editorTarget = null
            },
            modifier = modifier
        )
        return
    }

    LaunchedEffect(selectedRoute) {
        if (selectedRoute != null) viewModel.setSpeed(16f)
    }

    val isWalking = isSpoofing && activeRoute != null
    // Show walk view when a route is selected OR a walk is active
    val showWalkView = selectedRoute != null || isWalking
    // The route to display (prefer the live active route while walking)
    val displayRoute = activeRoute ?: selectedRoute

    val positionLat = mapCenterLat
    val positionLng = mapCenterLng
    val currentPin = if (positionLat != null && positionLng != null) {
        listOf(LocationPoint(positionLat, positionLng, stringResource(R.string.current_position)))
    } else {
        emptyList()
    }

    // Height reserved for the floating bottom button bar
    val bottomBarHeight = 80.dp

    if (showWalkView && displayRoute != null) {
        // ═══════════════════════════════════════════════════════════════════════
        // WALK VIEW — pre-start (selected) or active (walking / paused)
        // ═══════════════════════════════════════════════════════════════════════
        val route = displayRoute
        val waypoints = WaypointJson.fromJson(route.waypointsJson)

        Box(modifier = modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = bottomBarHeight + 16.dp)
            ) {
                // ── Header ────────────────────────────────────────────────────
                item {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(stringResource(R.string.walk_title), style = MaterialTheme.typography.headlineMedium)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                route.displayName(appLanguage),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // ── Live map ──────────────────────────────────────────────────
                item {
                    if (positionLat != null && positionLng != null) {
                        MapViewComposable(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ROUTE_MAP_HEIGHT)
                                .clipToBounds(),
                            center = GeoPoint(positionLat, positionLng),
                            zoom = 16.0,
                            waypoints = currentPin
                        )
                    }
                }

                // ── Current Speed (large, live, centred) ──────────────────────
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(if (!isWalking) R.string.not_started else R.string.current_speed),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (!isWalking)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                if (isWalking) "${"%.1f".format(liveSpeed)}" else "—",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 80.sp,
                                    color = if (isWalking)
                                        Color(0xFFAB482D)
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "km/h",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 14.dp)
                            )
                        }
                    }
                }

                // ── Speed controls (editable while walking) ───────────────────
                item {
                    SpeedControlPanel(
                        speed = speed,
                        onSpeedChange = viewModel::setSpeed
                    )
                }

                // ── Waypoint progress list ─────────────────────────────────────
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

                itemsIndexed(waypoints) { index, point ->
                    val lat = positionLat ?: 0.0
                    val lng = positionLng ?: 0.0
                    val dist = distanceBetween(lat, lng, point.latitude, point.longitude)
                    val isNearest = findNearestIndex(waypoints, lat, lng) == index
                    WaypointProgressRow(
                        index = index,
                        point = point,
                        distanceKm = dist,
                        isNearest = isNearest,
                        onJumpTo = { viewModel.jumpToWaypoint(index, route) }
                    )
                }
            }

            // ── Floating bottom bar ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isWalking) {
                    FilledIconButton(
                        onClick = { selectedRoute = null },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = AppAccent.neutral.container.copy(alpha = 0.72f),
                            contentColor = AppAccent.neutral.content
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                    FilledIconButton(
                        onClick = { viewModel.startWalk(route, reversed = true) },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = AppAccent.neutral.container.copy(alpha = 0.72f),
                            contentColor = AppAccent.neutral.content
                        )
                    ) {
                        Icon(Icons.Default.SwapVert, contentDescription = stringResource(R.string.reverse_direction))
                    }
                    Button(
                        onClick = { viewModel.startWalk(route) },
                        modifier = Modifier.weight(1.5f).height(56.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppAccent.start.copy(alpha = 0.72f),
                            contentColor = AppAccent.onStart
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_start), style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    // Walking: Back, Revert, Pause/Resume (yellow), Stop
                    FilledIconButton(
                        onClick = { viewModel.stop(); selectedRoute = null },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = AppAccent.neutral.container.copy(alpha = 0.72f),
                            contentColor = AppAccent.neutral.content
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                    FilledIconButton(
                        onClick = { viewModel.revertWalk(route) },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = AppAccent.neutral.container.copy(alpha = 0.72f),
                            contentColor = AppAccent.neutral.content
                        )
                    ) {
                        Icon(Icons.Default.SwapVert, contentDescription = stringResource(R.string.reverse_direction))
                    }
                    Button(
                        onClick = { if (isPaused) viewModel.resume() else viewModel.pause() },
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppAccent.action.copy(alpha = 0.72f),
                            contentColor = AppAccent.onAction
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Icon(
                            if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = stringResource(if (isPaused) R.string.action_resume else R.string.action_pause)
                        )
                    }
                    Button(
                        onClick = { viewModel.stop() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppAccent.stop.copy(alpha = 0.72f),
                            contentColor = androidx.compose.ui.graphics.Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_stop))
                    }
                }
            }
        }

    } else {
        // ═══════════════════════════════════════════════════════════════════════
        // IDLE STATE — map + speed settings + route picker
        // ═══════════════════════════════════════════════════════════════════════
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Current location map (matches Location view header) ───────────
            item {
                if (positionLat != null && positionLng != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ROUTE_MAP_HEIGHT)
                            .clipToBounds()
                    ) {
                        MapViewComposable(
                            modifier = Modifier.fillMaxSize(),
                            center = GeoPoint(positionLat, positionLng),
                            zoom = 15.0,
                            waypoints = currentPin
                        )

                        // Floats on the map like the Location screen's add button.
                        Surface(
                            shape = CircleShape,
                            color = AppAccent.action.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            IconButton(onClick = { editorTarget = null; editorOpen = true }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = stringResource(R.string.add_route),
                                    tint = AppAccent.onAction
                                )
                            }
                        }
                    }
                }
            }

            // ── Speed settings ────────────────────────────────────────────────
            item {
                SpeedControlPanel(
                    speed = speed,
                    onSpeedChange = viewModel::setSpeed,
                    // Translucent card rides up over the map's lower quarter.
                    modifier = Modifier
                        .zIndex(1f)
                        .overlapAbove(ROUTE_MAP_HEIGHT * 0.25f),
                    header = {
                        Text(
                            stringResource(R.string.base_speed),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "—",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 48.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "km/h",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                )
            }

            // ── Saved Routes header ───────────────────────────────────────────
            // ── Route list ────────────────────────────────────────────────────
            if (routes.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.DirectionsWalk,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.no_routes_yet), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.no_routes_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                items(routes, key = { it.id }) { route ->
                    RouteRow(
                        route = route,
                        distanceLabel = viewModel.distanceKm(route),
                        waypointCount = viewModel.waypointCount(route),
                        appLanguage = appLanguage,
                        onClick = { selectedRoute = route },
                        onEdit = { editorTarget = route; editorOpen = true },
                        onDelete = { deleteRouteTarget = route }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

}

// ── Route speed slider: 0────────20 | 100 | 300 ─────────────────────────────
// The 0–20 km/h walking range takes up the first 80% of the track; the faster
// presets (20→100→300) share the remaining 20%.
/** Height of the route-screen map previews. */
private val ROUTE_MAP_HEIGHT = 180.dp

private const val ROUTE_MAX_SPEED_KMH = 300f
private val SPEED_STOPS = floatArrayOf(0f, 20f, 100f, 300f)
private val SPEED_FRACS = floatArrayOf(0f, 0.80f, 0.90f, 1f)

private fun routeSpeedToSlider(kmh: Float): Float {
    for (i in 1 until SPEED_STOPS.size) {
        if (kmh <= SPEED_STOPS[i]) {
            val lo = SPEED_STOPS[i - 1]
            val hi = SPEED_STOPS[i]
            val fLo = SPEED_FRACS[i - 1]
            val fHi = SPEED_FRACS[i]
            return fLo + ((kmh - lo) / (hi - lo)) * (fHi - fLo)
        }
    }
    return 1f
}

private fun routeSliderToSpeed(frac: Float): Float {
    for (i in 1 until SPEED_FRACS.size) {
        if (frac <= SPEED_FRACS[i]) {
            val fLo = SPEED_FRACS[i - 1]
            val fHi = SPEED_FRACS[i]
            val lo = SPEED_STOPS[i - 1]
            val hi = SPEED_STOPS[i]
            val local = if (fHi > fLo) (frac - fLo) / (fHi - fLo) else 0f
            return (lo + local * (hi - lo)).coerceIn(0f, ROUTE_MAX_SPEED_KMH)
        }
    }
    return ROUTE_MAX_SPEED_KMH
}

private fun formatRouteSpeed(kmh: Float): String =
    if (kmh < 10f) "${"%.1f".format(kmh)} km/h" else "${"%.0f".format(kmh)} km/h"

// ── Shared speed control panel ───────────────────────────────────────────────

@Composable
private fun SpeedControlPanel(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    /** Optional readout rendered inside the card, above the slider row. */
    header: (@Composable () -> Unit)? = null
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            header?.invoke()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.speed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = routeSpeedToSlider(speed),
                    onValueChange = { onSpeedChange(routeSliderToSpeed(it)) },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = AppAccent.slider.copy(alpha = 0.72f),
                        activeTrackColor = AppAccent.slider.copy(alpha = 0.72f),
                        inactiveTrackColor = com.gpsanywhere.app.ui.theme.SliderInactiveTrack.copy(alpha = 0.9f)
                    )
                )
                Text(
                    formatRouteSpeed(speed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ── Idle: compact route row ──────────────────────────────────────────────────

@Composable
private fun RouteRow(
    route: SavedRoute,
    distanceLabel: String,
    waypointCount: Int,
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val nameColor = if (isDark) com.gpsanywhere.app.ui.theme.CardNameTextDark else com.gpsanywhere.app.ui.theme.CardNameText
    val subColor = if (isDark) com.gpsanywhere.app.ui.theme.CardCoordTextDark else com.gpsanywhere.app.ui.theme.CardCoordText
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppAccent.cardFill),
        border = BorderStroke(1.5.dp, AppAccent.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(route.displayName(appLanguage), style = MaterialTheme.typography.titleSmall, color = nameColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.route_meta, distanceLabel, waypointCount), style = MaterialTheme.typography.bodySmall,
                    color = subColor)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_route), tint = subColor, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_route), tint = AppAccent.stop.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = subColor)
        }
    }
}

// ── Active: waypoint progress row ────────────────────────────────────────────

@Composable
private fun WaypointProgressRow(
    index: Int,
    point: LocationPoint,
    distanceKm: String,
    isNearest: Boolean,
    onJumpTo: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.LocationOn, contentDescription = null,
            tint = if (isNearest) AppAccent.nearestWaypoint
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                point.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.waypoint_n, index + 1),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isNearest) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onJumpTo() }
            )
            Text("${"%.5f".format(point.latitude)}  ${"%.5f".format(point.longitude)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
        Text(distanceKm, style = MaterialTheme.typography.bodySmall,
            color = if (isNearest) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun distanceBetween(lat1: Double, lng1: Double, lat2: Double, lng2: Double): String {
    val results = FloatArray(1)
    android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, results)
    val meters = results[0]
    return if (meters < 1000) "${"%.0f".format(meters)} m" else "${"%.2f".format(meters / 1000)} km"
}

private fun findNearestIndex(waypoints: List<LocationPoint>, lat: Double, lng: Double): Int {
    if (waypoints.isEmpty()) return -1
    var minDist = Float.MAX_VALUE
    var minIdx = 0
    waypoints.forEachIndexed { i, p ->
        val r = FloatArray(1)
        android.location.Location.distanceBetween(lat, lng, p.latitude, p.longitude, r)
        if (r[0] < minDist) { minDist = r[0]; minIdx = i }
    }
    return minIdx
}
