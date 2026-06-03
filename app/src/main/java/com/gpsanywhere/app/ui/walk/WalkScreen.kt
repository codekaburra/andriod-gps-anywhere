package com.gpsanywhere.app.ui.walk

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gpsanywhere.app.data.SavedRoute
import com.gpsanywhere.app.data.WaypointJson
import com.gpsanywhere.app.routes.LocationPoint
import com.gpsanywhere.app.viewmodel.WalkViewModel

@Composable
fun WalkScreen(
    viewModel: WalkViewModel,
    modifier: Modifier = Modifier
) {
    val routes by viewModel.routes.observeAsState(emptyList())
    val isSpoofing by viewModel.isSpoofing.observeAsState(false)
    val speed by viewModel.speedKmh.collectAsState()
    val activeRoute by viewModel.activeRoute.collectAsState()
    val currentLat by viewModel.currentLat.observeAsState(0.0)
    val currentLng by viewModel.currentLng.observeAsState(0.0)

    var confirmRoute by remember { mutableStateOf<SavedRoute?>(null) }

    val isActive = isSpoofing && activeRoute != null

    if (isActive) {
        // ═══════════════════════════════════════════════════════════════════════
        // ACTIVE WALK STATE
        // ═══════════════════════════════════════════════════════════════════════
        val route = activeRoute!!
        val waypoints = WaypointJson.fromJson(route.waypointsJson)

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text("Walk", style = MaterialTheme.typography.headlineMedium)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Active Route",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            route.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ── Current Speed (large) ─────────────────────────────────────────
            item {
                Column {
                    Text(
                        "Current Speed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            "${"%.1f".format(speed)}",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 56.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "km/h",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                    Slider(
                        value = speed,
                        onValueChange = viewModel::setSpeed,
                        valueRange = 4f..20f,
                        steps = 15,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Waypoint progress list ────────────────────────────────────────
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }

            itemsIndexed(waypoints) { index, point ->
                val dist = distanceBetween(currentLat, currentLng, point.latitude, point.longitude)
                val isNearest = findNearestIndex(waypoints, currentLat, currentLng) == index

                WaypointProgressRow(
                    index = index,
                    point = point,
                    distanceKm = dist,
                    isNearest = isNearest
                )
            }

            // ── Stop Walk button ──────────────────────────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.stop() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Stop Walk", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    } else {
        // ═══════════════════════════════════════════════════════════════════════
        // IDLE STATE — speed + route picker
        // ═══════════════════════════════════════════════════════════════════════
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                Text("Walk", style = MaterialTheme.typography.headlineMedium)
            }

            // ── Speed ─────────────────────────────────────────────────────────
            item {
                Column {
                    Text(
                        "Speed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${"%.1f".format(speed)}",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 48.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "km/h",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Slider(
                        value = speed,
                        onValueChange = viewModel::setSpeed,
                        valueRange = 4f..20f,
                        steps = 15,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Saved Routes header ───────────────────────────────────────────
            item {
                Text(
                    "Saved Routes",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

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
                        Text("No routes yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Create a route on the Add Route tab",
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
                        onClick = { confirmRoute = route }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // ── Confirm start walk dialog ─────────────────────────────────────────────
    confirmRoute?.let { route ->
        AlertDialog(
            onDismissRequest = { confirmRoute = null },
            title = { Text("Start walk?") },
            text = { Text("Walk \"${route.name}\" at ${"%.1f".format(speed)} km/h?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.startWalk(route)
                    confirmRoute = null
                }) { Text("Start") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRoute = null }) { Text("Cancel") }
            }
        )
    }
}

// ── Idle: compact route row ──────────────────────────────────────────────────

@Composable
private fun RouteRow(
    route: SavedRoute,
    distanceLabel: String,
    waypointCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    route.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "$distanceLabel · $waypointCount stops",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

// ── Active: waypoint progress row ────────────────────────────────────────────

@Composable
private fun WaypointProgressRow(
    index: Int,
    point: LocationPoint,
    distanceKm: String,
    isNearest: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint = if (isNearest)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                point.name?.takeIf { it.isNotBlank() } ?: "Waypoint ${index + 1}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isNearest)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${"%.5f".format(point.latitude)}  ${"%.5f".format(point.longitude)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        Text(
            distanceKm,
            style = MaterialTheme.typography.bodySmall,
            color = if (isNearest)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun distanceBetween(lat1: Double, lng1: Double, lat2: Double, lng2: Double): String {
    val results = FloatArray(1)
    android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, results)
    val meters = results[0]
    return if (meters < 1000) "${"%.0f".format(meters)} m" else "${"%.2f".format(meters / 1000)} km"
}

private fun findNearestIndex(
    waypoints: List<LocationPoint>,
    lat: Double,
    lng: Double
): Int {
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
