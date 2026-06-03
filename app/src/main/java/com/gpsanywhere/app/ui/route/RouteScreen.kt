package com.gpsanywhere.app.ui.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.ui.components.MapViewComposable
import com.gpsanywhere.app.viewmodel.RouteTab
import com.gpsanywhere.app.viewmodel.RouteViewModel
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

@Composable
fun RouteScreen(
    viewModel: RouteViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val waypoints by viewModel.waypoints.collectAsState()
    val speed by viewModel.speedKmh.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val osrmResult by viewModel.osrmResult.collectAsState()
    val startLat by viewModel.startLat.collectAsState()
    val startLng by viewModel.startLng.collectAsState()
    val endLat by viewModel.endLat.collectAsState()
    val endLng by viewModel.endLng.collectAsState()
    val isSpoofing by viewModel.isSpoofing.observeAsState(false)

    var showSaveDialog by remember { mutableStateOf(false) }
    var routeName by remember { mutableStateOf("") }
    var pasteText by remember { mutableStateOf("") }
    var pasteStart by remember { mutableStateOf("") }
    var pasteEnd by remember { mutableStateOf("") }

    // Name search (Nominatim geocoding) state
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val searchLoading by viewModel.searchLoading.collectAsState()

    val scope = rememberCoroutineScope()

    val mapCenter = when {
        waypoints.isNotEmpty() -> GeoPoint(waypoints.first().latitude, waypoints.first().longitude)
        else -> {
            val sLat = startLat.toDoubleOrNull()
            val sLng = startLng.toDoubleOrNull()
            if (sLat != null && sLng != null) GeoPoint(sLat, sLng) else GeoPoint(25.0330, 121.5654)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SegmentedButton(
                selected = selectedTab == RouteTab.MANUAL,
                onClick = { viewModel.selectTab(RouteTab.MANUAL) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Manual Pins")
            }
            SegmentedButton(
                selected = selectedTab == RouteTab.OSRM,
                onClick = { viewModel.selectTab(RouteTab.OSRM) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("OSRM Route")
            }
        }

        MapViewComposable(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f),
            center = mapCenter,
            waypoints = waypoints,
            showNumberedPins = selectedTab == RouteTab.MANUAL,
            onMapClick = if (selectedTab == RouteTab.MANUAL) {
                { viewModel.addWaypoint(it) }
            } else null
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedTab == RouteTab.MANUAL) {
                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        label = { Text("Paste coords") },
                        placeholder = { Text("25.0330, 121.5654") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            if (viewModel.addPastedWaypoint(pasteText)) pasteText = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add as waypoint") }
                }

                if (selectedTab == RouteTab.OSRM) {
                    // --- Search by name (online geocoding via Nominatim) ---
                    Text("Or search by name", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Place name or address") },
                            placeholder = { Text("e.g. Eiffel Tower, Central Park") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = { viewModel.searchByName(searchQuery) },
                            enabled = searchQuery.isNotBlank() && !searchLoading,
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            if (searchLoading) {
                                CircularProgressIndicator(modifier = Modifier.height(18.dp))
                            } else {
                                Text("Search")
                            }
                        }
                    }

                    // Search results
                    if (searchResults.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            searchResults.forEach { result ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = result.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.applySearchResult(result, isStart = true) },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("Start", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.applySearchResult(result, isStart = false) },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("End", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            TextButton(
                                onClick = { viewModel.clearSearchResults() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Clear results", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pasteStart,
                            onValueChange = { pasteStart = it },
                            label = { Text("Paste Start (lat, lng)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = {
                            if (viewModel.setStartFromPaste(pasteStart)) pasteStart = ""
                        }) { Text("Set") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startLat,
                            onValueChange = viewModel::setStartLat,
                            label = { Text("Start Lat") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = startLng,
                            onValueChange = viewModel::setStartLng,
                            label = { Text("Start Lng") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pasteEnd,
                            onValueChange = { pasteEnd = it },
                            label = { Text("Paste Destination (lat, lng)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = {
                            if (viewModel.setEndFromPaste(pasteEnd)) pasteEnd = ""
                        }) { Text("Set") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = endLat,
                            onValueChange = viewModel::setEndLat,
                            label = { Text("End Lat") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endLng,
                            onValueChange = viewModel::setEndLng,
                            label = { Text("End Lng") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Button(
                        onClick = { viewModel.fetchOsrmRoute() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading
                    ) {
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp))
                        } else {
                            Text("Get Route")
                        }
                    }
                    osrmResult?.let { result ->
                        Text(
                            text = "${"%.1f".format(result.distanceKm)} km · ${result.durationMinutes} min",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { viewModel.removeLastWaypoint() }) {
                            Text("Delete Last")
                        }
                        OutlinedButton(onClick = { viewModel.clearWaypoints() }) {
                            Text("Clear All")
                        }
                    }
                    if (waypoints.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        ) {
                            itemsIndexed(waypoints) { index, point ->
                                Text(
                                    text = "${index + 1}. ${"%.4f".format(point.latitude)}, ${"%.4f".format(point.longitude)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Text("Speed: ${speed.toInt()} km/h", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = speed,
                    onValueChange = viewModel::setSpeed,
                    valueRange = 4f..20f,
                    steps = 15
                )

                Button(
                    onClick = { viewModel.startWalk() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(999.dp),
                    enabled = waypoints.isNotEmpty()
                ) {
                    Text("Start Walk")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* pause stub */ },
                        modifier = Modifier.weight(1f),
                        enabled = isSpoofing
                    ) {
                        Text("Pause")
                    }
                    OutlinedButton(
                        onClick = { viewModel.stopWalk() },
                        modifier = Modifier.weight(1f),
                        enabled = isSpoofing
                    ) {
                        Text("Stop")
                    }
                }

                TextButton(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = waypoints.isNotEmpty()
                ) {
                    Text("Save Route")
                }
            }
        }
    }

    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Route") },
            text = {
                OutlinedTextField(
                    value = routeName,
                    onValueChange = { routeName = it },
                    label = { Text("Route name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val method = if (selectedTab == RouteTab.OSRM) "OSRM" else "MANUAL_MAP"
                        if (viewModel.saveRoute(routeName, method)) {
                            showSaveDialog = false
                            routeName = ""
                        }
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
