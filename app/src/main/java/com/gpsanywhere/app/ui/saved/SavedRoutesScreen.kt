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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.data.SavedRoute
import com.gpsanywhere.app.viewmodel.SavedRoutesViewModel

@Composable
fun SavedRoutesScreen(
    viewModel: SavedRoutesViewModel,
    onAddNew: () -> Unit,
    onEditRoute: (SavedRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    val routes by viewModel.routes.observeAsState(emptyList())
    var routeToDelete by remember { mutableStateOf<SavedRoute?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Saved routes",
                style = MaterialTheme.typography.headlineMedium
            )
            OutlinedButton(onClick = onAddNew) {
                Text("+ Add new")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (routes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Route,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No saved routes yet",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Create a route on the Route tab and tap Save",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onAddNew) {
                    Text("Go to Route")
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(routes, key = { it.id }) { route ->
                    SavedRouteCard(
                        route = route,
                        distanceLabel = viewModel.distanceKm(route),
                        waypointCount = viewModel.waypointCount(route),
                        onPlay = { viewModel.startRoute(route) },
                        onEdit = { onEditRoute(route) },
                        onDelete = { routeToDelete = route }
                    )
                }
            }
        }
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
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { routeToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SavedRouteCard(
    route: SavedRoute,
    distanceLabel: String,
    waypointCount: Int,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = route.name,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = distanceLabel,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$waypointCount waypoints",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPlay) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start route")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit route")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete route")
                }
            }
        }
    }
}
