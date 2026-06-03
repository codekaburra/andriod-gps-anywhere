package com.gpsanywhere.app.ui.location

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.data.SavedLocation
import com.gpsanywhere.app.routes.LocationPoint
import com.gpsanywhere.app.service.SpoofService
import com.gpsanywhere.app.settings.HistoryEntry
import com.gpsanywhere.app.ui.components.MapViewComposable
import com.gpsanywhere.app.viewmodel.SavedLocationsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    viewModel: SavedLocationsViewModel,
    modifier: Modifier = Modifier
) {
    val locations by viewModel.locations.observeAsState(emptyList())
    val history by viewModel.history.collectAsState()
    val routeHints by viewModel.routeHints.collectAsState()

    // Walk-mode awareness
    val isWalking by SpoofService.isRunning.observeAsState(false)
    val fakeLat by SpoofService.currentLat.observeAsState(0.0)
    val fakeLng by SpoofService.currentLng.observeAsState(0.0)

    var showAddSheet by remember { mutableStateOf(false) }
    var confirmLocation by remember { mutableStateOf<SavedLocation?>(null) }
    var confirmHistory by remember { mutableStateOf<HistoryEntry?>(null) }
    var walkBreakLocation by remember { mutableStateOf<SavedLocation?>(null) }
    var walkBreakHistory by remember { mutableStateOf<HistoryEntry?>(null) }
    var deleteLocation by remember { mutableStateOf<SavedLocation?>(null) }
    var deleteHistory by remember { mutableStateOf<HistoryEntry?>(null) }
    var clearHistory by remember { mutableStateOf(false) }
    var selectedLocation by remember(locations) { mutableStateOf(locations.firstOrNull()) }

    // When walking, map follows the live fake location; otherwise show selected/history
    val mapCenter = if (isWalking && (fakeLat != 0.0 || fakeLng != 0.0)) {
        GeoPoint(fakeLat, fakeLng)
    } else {
        val fallback = selectedLocation?.let {
            LocationPoint(it.latitude, it.longitude, it.name)
        } ?: history.firstOrNull()?.let {
            LocationPoint(it.lat, it.lng, it.label)
        } ?: LocationPoint(22.9747562, 120.2215652, "台南市文化中心")
        GeoPoint(fallback.latitude, fallback.longitude)
    }

    val previewPoint = selectedLocation?.let {
        LocationPoint(it.latitude, it.longitude, it.name)
    } ?: history.firstOrNull()?.let {
        LocationPoint(it.lat, it.lng, it.label)
    } ?: LocationPoint(22.9747562, 120.2215652, "台南市文化中心")

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Location") },
                actions = {
                    IconButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add location")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Walk-mode banner ──────────────────────────────────────────────
            if (isWalking) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Walk mode active — map follows your fake location.\nSetting a custom location will stop the walk.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            item {
                MapViewComposable(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    center = mapCenter,
                    zoom = 15.0,
                    waypoints = if (isWalking) listOf(LocationPoint(fakeLat, fakeLng, "Current position"))
                                else listOf(previewPoint)
                )
            }

            item {
                SectionHeader(title = "Saved Locations")
            }

            if (locations.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.LocationOn,
                        title = "No saved locations",
                        body = "Tap + to add a custom location"
                    )
                }
            } else {
                items(locations, key = { it.id }) { loc ->
                    LocationCard(
                        location = loc,
                        routeHint = if (loc.sourceId != null) {
                            viewModel.routeHintFor(loc, routeHints)
                        } else {
                            null
                        },
                        onClick = {
                            selectedLocation = loc
                            if (isWalking) walkBreakLocation = loc else confirmLocation = loc
                        },
                        onDelete = if (!loc.isPreinstalled) {
                            { deleteLocation = loc }
                        } else {
                            null
                        }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = "記錄")
                    if (history.isNotEmpty()) {
                        TextButton(onClick = { clearHistory = true }) {
                            Text("Clear all")
                        }
                    }
                }
            }

            if (history.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.History,
                        title = "No recent locations",
                        body = "Started locations will appear here"
                    )
                }
            } else {
                items(history, key = { "${it.lat}-${it.lng}-${it.timestamp}" }) { entry ->
                    HistoryCard(
                        entry = entry,
                        onClick = { if (isWalking) walkBreakHistory = entry else confirmHistory = entry },
                        onDelete = { deleteHistory = entry }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    confirmLocation?.let { loc ->
        AlertDialog(
            onDismissRequest = { confirmLocation = null },
            title = { Text("Use location?") },
            text = { Text("Use \"${loc.name}\" as custom GPS location?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.startSpoofing(loc)
                    confirmLocation = null
                }) { Text("Start") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLocation = null }) { Text("Cancel") }
            }
        )
    }

    confirmHistory?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmHistory = null },
            title = { Text("Use this location?") },
            text = { Text("Use \"${entry.displayName()}\" as custom GPS location?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.startSpoofing(entry)
                    confirmHistory = null
                }) { Text("Start") }
            },
            dismissButton = {
                TextButton(onClick = { confirmHistory = null }) { Text("Cancel") }
            }
        )
    }

    // ── Walk-break warning dialogs ────────────────────────────────────────────
    walkBreakLocation?.let { loc ->
        AlertDialog(
            onDismissRequest = { walkBreakLocation = null },
            title = { Text("Stop walk mode?") },
            text = { Text("Setting \"${loc.name}\" as your location will stop the current walk. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.startSpoofing(loc)
                    walkBreakLocation = null
                }) { Text("Stop walk & use location") }
            },
            dismissButton = {
                TextButton(onClick = { walkBreakLocation = null }) { Text("Cancel") }
            }
        )
    }

    walkBreakHistory?.let { entry ->
        AlertDialog(
            onDismissRequest = { walkBreakHistory = null },
            title = { Text("Stop walk mode?") },
            text = { Text("Setting \"${entry.displayName()}\" as your location will stop the current walk. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.startSpoofing(entry)
                    walkBreakHistory = null
                }) { Text("Stop walk & use location") }
            },
            dismissButton = {
                TextButton(onClick = { walkBreakHistory = null }) { Text("Cancel") }
            }
        )
    }

    deleteLocation?.let { loc ->
        AlertDialog(
            onDismissRequest = { deleteLocation = null },
            title = { Text("Delete location?") },
            text = { Text("Delete \"${loc.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLocation(loc)
                    deleteLocation = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteLocation = null }) { Text("Cancel") }
            }
        )
    }

    deleteHistory?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteHistory = null },
            title = { Text("Delete history?") },
            text = { Text("Remove \"${entry.displayName()}\" from 記錄?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteHistoryEntry(entry)
                    deleteHistory = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteHistory = null }) { Text("Cancel") }
            }
        )
    }

    if (clearHistory) {
        AlertDialog(
            onDismissRequest = { clearHistory = false },
            title = { Text("Clear 記錄?") },
            text = { Text("Remove all recent locations?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    clearHistory = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { clearHistory = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddSheet) {
        AddLocationSheet(
            onDismiss = { showAddSheet = false },
            onSave = { name, lat, lng ->
                viewModel.addLocation(name, lat, lng)
                showAddSheet = false
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun LocationCard(
    location: SavedLocation,
    routeHint: String?,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    location.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                routeHint?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "${"%.6f".format(location.longitude)}, ${"%.6f".format(location.latitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    entry: HistoryEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    entry.displayName(),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    recentTime(entry.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete history",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        )
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLocationSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, lat: Double, lng: Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current

    var name by remember { mutableStateOf("") }
    var latText by remember { mutableStateOf("") }
    var lngText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val previewLat = latText.toDoubleOrNull()
    val previewLng = lngText.toDoubleOrNull()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add Location", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = lngText,
                    onValueChange = { lngText = it; error = null },
                    label = { Text("Longitude") },
                    placeholder = { Text("120.2215652") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it; error = null },
                    label = { Text("Latitude") },
                    placeholder = { Text("22.9747562") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedButton(
                onClick = {
                    val raw = clipboard.getText()?.text?.trim().orEmpty()
                    val parsed = parseClipboardCoordinates(raw)
                    if (parsed == null) {
                        error = "Clipboard must be longitude, latitude"
                    } else {
                        lngText = parsed.first.toBigDecimal().stripTrailingZeros().toPlainString()
                        latText = parsed.second.toBigDecimal().stripTrailingZeros().toPlainString()
                        error = null
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContentPaste, contentDescription = null)
                Text("Paste Coordinates", modifier = Modifier.padding(start = 8.dp))
            }
            Text(
                "Expects longitude, latitude",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )

            if (
                previewLat != null &&
                previewLng != null &&
                previewLat in -90.0..90.0 &&
                previewLng in -180.0..180.0
            ) {
                MapViewComposable(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    center = GeoPoint(previewLat, previewLng),
                    zoom = 15.0,
                    waypoints = listOf(
                        LocationPoint(
                            latitude = previewLat,
                            longitude = previewLng,
                            name = name.takeIf { it.isNotBlank() }
                        )
                    )
                )
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(onClick = {
                    val n = name.trim()
                    val lat = latText.trim().toDoubleOrNull()
                    val lng = lngText.trim().toDoubleOrNull()
                    when {
                        n.isEmpty() -> error = "Name is required"
                        lng == null || lng !in -180.0..180.0 -> error = "Longitude must be between -180 and 180"
                        lat == null || lat !in -90.0..90.0 -> error = "Latitude must be between -90 and 90"
                        else -> onSave(n, lat, lng)
                    }
                }) { Text("Save") }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun parseClipboardCoordinates(raw: String): Pair<Double, Double>? {
    val parts = raw.split(",")
    if (parts.size != 2) return null
    val lng = parts[0].trim().toDoubleOrNull() ?: return null
    val lat = parts[1].trim().toDoubleOrNull() ?: return null
    if (lng !in -180.0..180.0 || lat !in -90.0..90.0) return null
    return lng to lat
}

private fun HistoryEntry.displayName(): String =
    label?.takeIf { it.isNotBlank() } ?: "${"%.6f".format(lng)}, ${"%.6f".format(lat)}"

private fun recentTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val ageMs = now - timestamp
    val dayMs = 24L * 60L * 60L * 1000L
    return when {
        ageMs < dayMs -> "Today ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))}"
        ageMs < 2L * dayMs -> "Yesterday"
        else -> SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(timestamp))
    }
}
