package com.gpsanywhere.app.ui.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val scope = rememberCoroutineScope()

    val mapCenter = waypoints.firstOrNull()?.let {
        GeoPoint(it.latitude, it.longitude)
    } ?: GeoPoint(25.0330, 121.5654)

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
                if (selectedTab == RouteTab.OSRM) {
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
