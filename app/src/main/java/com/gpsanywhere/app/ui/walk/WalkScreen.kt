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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.gpsanywhere.app.data.SavedRoute
import com.gpsanywhere.app.data.WaypointJson
import com.gpsanywhere.app.ui.components.MapPreviewSheet
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

    var confirmRoute by remember { mutableStateOf<SavedRoute?>(null) }
    var previewRoute by remember { mutableStateOf<SavedRoute?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(12.dp))
            Text("Walk", style = MaterialTheme.typography.headlineMedium)
        }

        // ── Speed panel ───────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Speed: ${"%.1f".format(speed)} km/h",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = speed,
                        onValueChange = viewModel::setSpeed,
                        valueRange = 4f..20f,
                        steps = 15,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("4 km/h (walk)", style = MaterialTheme.typography.labelSmall)
                        Text("20 km/h (run)", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // ── Active walk banner ────────────────────────────────────────────────
        if (isSpoofing && activeRoute != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Walking", style = MaterialTheme.typography.labelMedium)
                            Text(
                                activeRoute!!.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${"%.1f".format(speed)} km/h",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Button(
                            onClick = { viewModel.stop() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Text("Stop", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        }

        // ── Routes list ───────────────────────────────────────────────────────
        item {
            Text(
                "Saved Routes",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (routes.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("No routes yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Create a route on the Route tab",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            items(routes, key = { it.id }) { route ->
                WalkRouteCard(
                    route = route,
                    distanceLabel = viewModel.distanceKm(route),
                    waypointCount = viewModel.waypointCount(route),
                    isActive = activeRoute?.id == route.id && isSpoofing,
                    onTap = { confirmRoute = route },
                    onPreview = { previewRoute = route }
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    // ── Confirm start walk ─────────────────────────────────────────────────
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

    // ── Map preview sheet ──────────────────────────────────────────────────
    previewRoute?.let { route ->
        MapPreviewSheet(
            routeName = route.name,
            waypoints = WaypointJson.fromJson(route.waypointsJson),
            onDismiss = { previewRoute = null }
        )
    }
}

@Composable
private fun WalkRouteCard(
    route: SavedRoute,
    distanceLabel: String,
    waypointCount: Int,
    isActive: Boolean,
    onTap: () -> Unit,
    onPreview: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.tertiaryContainer
            else if (route.isPreinstalled)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(route.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(distanceLabel, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text("$waypointCount waypoints", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            IconButton(onClick = onPreview) {
                Icon(Icons.Default.Map, contentDescription = "Preview map")
            }
        }
    }
}
