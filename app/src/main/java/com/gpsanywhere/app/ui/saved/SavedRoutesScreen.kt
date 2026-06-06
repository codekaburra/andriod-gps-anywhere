package com.gpsanywhere.app.ui.saved

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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.data.DefaultSavedRouteSeeder.DefaultRouteAsset
import com.gpsanywhere.app.data.SavedRoute
import com.gpsanywhere.app.data.WaypointJson
import com.gpsanywhere.app.ui.components.MapPreviewSheet
import com.gpsanywhere.app.viewmodel.WalkViewModel

@Composable
fun SavedRoutesScreen(
    viewModel: WalkViewModel,
    modifier: Modifier = Modifier
) {
    val routes by viewModel.routes.observeAsState(emptyList())
    val defaultRoutes by viewModel.defaultRoutes.collectAsState()
    var routeToDelete by remember { mutableStateOf<SavedRoute?>(null) }
    var previewRoute by remember { mutableStateOf<Pair<String, List<com.gpsanywhere.app.routes.LocationPoint>>?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Saved Routes", style = MaterialTheme.typography.headlineMedium)
            }
        }

        // ── Default (bundled) routes section ──────────────────────────────────
        if (defaultRoutes.isNotEmpty()) {
            item {
                Text(
                    "🗺  Default Routes",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(defaultRoutes, key = { "default_${it.routeName}" }) { asset ->
                DefaultRouteCard(
                    asset = asset,
                    distanceLabel = viewModel.defaultRouteDistanceKm(asset),
                    onPlay = { viewModel.startDefaultRoute(asset) },
                    onSave = { viewModel.saveDefaultRoute(asset) },
                    onPreview = { previewRoute = asset.routeName to asset.toLocationPoints() }
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
        }

        // ── My saved routes section ───────────────────────────────────────────
        item {
            Text(
                "📍  My Routes",
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
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No saved routes yet",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Routes will appear here once saved",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(routes, key = { it.id }) { route ->
                SavedRouteCard(
                    route = route,
                    distanceLabel = viewModel.distanceKm(route),
                    waypointCount = viewModel.waypointCount(route),
                    onPlay = { viewModel.startWalk(route) },
                    onDelete = { routeToDelete = route },
                    onPreview = {
                        previewRoute = route.name to WaypointJson.fromJson(route.waypointsJson)
                    }
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    previewRoute?.let { (name, points) ->
        MapPreviewSheet(
            routeName = name,
            waypoints = points,
            onDismiss = { previewRoute = null }
        )
    }

    routeToDelete?.let { route ->
        AlertDialog(
            onDismissRequest = { routeToDelete = null },
            title = { Text("Delete route?") },
            text = { Text("Delete \"${route.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRoute(route)
                    routeToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { routeToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// ── Default route card ────────────────────────────────────────────────────────

@Composable
private fun DefaultRouteCard(
    asset: DefaultRouteAsset,
    distanceLabel: String,
    onPlay: () -> Unit,
    onSave: () -> Unit,
    onPreview: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(asset.routeName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                distanceLabel,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "${asset.coordinates.size} waypoints",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreview) {
                    Icon(Icons.Default.Map, contentDescription = "Preview on map")
                }
                IconButton(onClick = onPlay) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                }
                IconButton(onClick = onSave) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = "Save to my routes")
                }
            }
        }
    }
}

// ── My route card ─────────────────────────────────────────────────────────────

@Composable
private fun SavedRouteCard(
    route: SavedRoute,
    distanceLabel: String,
    waypointCount: Int,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onPreview: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(route.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                distanceLabel,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "$waypointCount waypoints",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreview) {
                    Icon(Icons.Default.Map, contentDescription = "Preview on map")
                }
                IconButton(onClick = onPlay) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start route")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete route")
                }
            }
        }
    }
}
