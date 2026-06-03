package com.gpsanywhere.app.ui.location

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.settings.AppPreferences
import com.gpsanywhere.app.settings.HistoryEntry
import com.gpsanywhere.app.service.SpoofService
import com.gpsanywhere.app.ui.components.MapViewComposable
import com.gpsanywhere.app.ui.theme.GalaxyAccent
import com.gpsanywhere.app.viewmodel.LocationViewModel
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun LocationScreen(
    viewModel: LocationViewModel,
    preferences: AppPreferences,
    modifier: Modifier = Modifier
) {
    val latitude by viewModel.latitude.collectAsState()
    val longitude by viewModel.longitude.collectAsState()
    val isSpoofing by viewModel.isSpoofing.observeAsState(false)
    val isPaused by viewModel.isPaused.observeAsState(false)
    val inputError by viewModel.inputError.collectAsState()
    val locationHistory by viewModel.locationHistory.collectAsState()

    // Local state + flows for online name search (Nominatim)
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val searchLoading by viewModel.searchLoading.collectAsState()

    var showComplianceDialog by remember { mutableStateOf(false) }
    var showValidationDialog by remember { mutableStateOf(false) }
    var pasteInput by remember { mutableStateOf("") }
    var pasteError by remember { mutableStateOf(false) }

    val defaultCenter = remember {
        val sLat = SpoofService.currentLat.value ?: 0.0
        val sLng = SpoofService.currentLng.value ?: 0.0
        if (sLat != 0.0 && sLng != 0.0) GeoPoint(sLat, sLng)
        else GeoPoint(25.0330, 121.5654)
    }
    var mapCenter by remember { mutableStateOf(defaultCenter) }

    val lat = latitude.toDoubleOrNull() ?: 25.0330
    val lng = longitude.toDoubleOrNull() ?: 121.5654
    val pinPoint = com.gpsanywhere.app.routes.LocationPoint(lat, lng)

    LaunchedEffect(inputError) {
        if (inputError != null) showValidationDialog = true
    }

    // Keep map in sync when lat/lng are set externally (e.g. from name search)
    LaunchedEffect(latitude, longitude) {
        val l = latitude.toDoubleOrNull() ?: return@LaunchedEffect
        val g = longitude.toDoubleOrNull() ?: return@LaunchedEffect
        mapCenter = GeoPoint(l, g)
    }

    Column(modifier = modifier.fillMaxSize()) {
        BoxWithConstraintsMap(
            center = mapCenter,
            pin = pinPoint,
            isPaused = isPaused,
            isSpoofing = isSpoofing,
            onMapClick = { point ->
                viewModel.setCoordinates(point.latitude, point.longitude)
                mapCenter = GeoPoint(point.latitude, point.longitude)
            },
            onRecenter = { mapCenter = GeoPoint(lat, lng) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Search by name (online) ──────────────────────────────
                Text("Search by name or address", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("e.g. Times Square, London Eye") },
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

                // Results list
                if (searchResults.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        searchResults.forEach { result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.applySearchResult(result)
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = result.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Use",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                        TextButton(
                            onClick = { viewModel.clearSearchResults() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Clear", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    HorizontalDivider()
                }

                // ── Coordinate inputs ────────────────────────────────────
                Text("Or enter coordinates", style = MaterialTheme.typography.titleMedium)

                val clipboardManager = LocalClipboardManager.current
                OutlinedTextField(
                    value = pasteInput,
                    onValueChange = { input ->
                        pasteInput = input
                        pasteError = false
                        val parts = input.split(",").map { it.trim() }
                        if (parts.size == 2) {
                            val pLat = parts[0].toDoubleOrNull()
                            val pLng = parts[1].toDoubleOrNull()
                            if (pLat != null && pLng != null) {
                                viewModel.setCoordinates(pLat, pLng)
                                mapCenter = GeoPoint(pLat, pLng)
                            } else if (input.isNotBlank()) {
                                pasteError = true
                            }
                        } else if (input.isNotBlank() && !input.endsWith(",")) {
                            pasteError = true
                        }
                    },
                    label = { Text("Paste from Google Maps  (lat, lng)") },
                    placeholder = { Text("e.g. 25.0457, 121.5764") },
                    trailingIcon = {
                        IconButton(onClick = {
                            val text = clipboardManager.getText()?.text ?: return@IconButton
                            pasteInput = text
                            pasteError = false
                            val parts = text.split(",").map { it.trim() }
                            if (parts.size == 2) {
                                val pLat = parts[0].toDoubleOrNull()
                                val pLng = parts[1].toDoubleOrNull()
                                if (pLat != null && pLng != null) {
                                    viewModel.setCoordinates(pLat, pLng)
                                    mapCenter = GeoPoint(pLat, pLng)
                                } else pasteError = true
                            } else pasteError = true
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste from clipboard")
                        }
                    },
                    isError = pasteError,
                    supportingText = if (pasteError) {
                        { Text("Use format: lat, lng  (e.g. 25.0457, 121.5764)") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = {
                            viewModel.setLatitude(it)
                            it.toDoubleOrNull()?.let { v -> mapCenter = GeoPoint(v, mapCenter.longitude) }
                        },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = {
                            viewModel.setLongitude(it)
                            it.toDoubleOrNull()?.let { v -> mapCenter = GeoPoint(mapCenter.latitude, v) }
                        },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // ── Action buttons ────────────────────────────────────────
                Button(
                    onClick = {
                        if (!preferences.complianceAcknowledged) showComplianceDialog = true
                        else viewModel.startSpoofing()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    val label = if (isSpoofing && !isPaused) "  Update Location"
                                else if (isPaused) "  Resume & Update"
                                else "  Activate Custom"
                    Text(label, modifier = Modifier.padding(vertical = 4.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (isPaused) viewModel.resumeSpoofing() else viewModel.pauseSpoofing()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp),
                        enabled = isSpoofing
                    ) {
                        if (isPaused) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Text("  Resume")
                        } else {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Text("  Pause")
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.stopSpoofing() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp),
                        enabled = isSpoofing
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Text("  Stop")
                    }
                }

                // ── Recent locations ──────────────────────────────────────
                if (locationHistory.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Recent",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text("Clear all", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    locationHistory.forEach { entry ->
                        HistoryRow(
                            entry = entry,
                            onSelect = {
                                viewModel.setCoordinates(entry.lat, entry.lng)
                                mapCenter = GeoPoint(entry.lat, entry.lng)
                            },
                            onDelete = { viewModel.deleteHistoryEntry(entry) },
                            onRename = { newLabel -> viewModel.renameHistoryEntry(entry, newLabel) }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showComplianceDialog) {
        AlertDialog(
            onDismissRequest = { showComplianceDialog = false },
            title = { Text("Development Use Only") },
            text = { Text("Using custom locations is for development and testing only. Please comply with local laws and app terms of service.") },
            confirmButton = {
                TextButton(onClick = {
                    preferences.complianceAcknowledged = true
                    showComplianceDialog = false
                    viewModel.startSpoofing()
                }) { Text("I Understand") }
            },
            dismissButton = {
                TextButton(onClick = { showComplianceDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showValidationDialog && inputError != null) {
        AlertDialog(
            onDismissRequest = { showValidationDialog = false; viewModel.clearInputError() },
            title = { Text("Invalid Coordinates") },
            text = { Text(inputError!!) },
            confirmButton = {
                TextButton(onClick = { showValidationDialog = false; viewModel.clearInputError() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        },
        positionalThreshold = { it * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val bgColor by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surface,
                label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor, RoundedCornerShape(12.dp))
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onSelect)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (!entry.label.isNullOrBlank()) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "%.6f,  %.6f".format(entry.lat, entry.lng),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "%.6f,  %.6f".format(entry.lat, entry.lng),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = relativeTime(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Edit name
            IconButton(onClick = { showRenameDialog = true }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Rename",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Delete
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showRenameDialog) {
        var labelInput by remember { mutableStateOf(entry.label ?: "") }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Name this location") },
            text = {
                OutlinedTextField(
                    value = labelInput,
                    onValueChange = { labelInput = it },
                    label = { Text("Name (optional)") },
                    placeholder = { Text("e.g. Home, Office…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(labelInput)
                    showRenameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private fun relativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
private fun BoxWithConstraintsMap(
    center: GeoPoint,
    pin: com.gpsanywhere.app.routes.LocationPoint,
    isPaused: Boolean,
    isSpoofing: Boolean,
    onMapClick: (com.gpsanywhere.app.routes.LocationPoint) -> Unit,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        MapViewComposable(
            modifier = Modifier.fillMaxSize(),
            center = center,
            waypoints = listOf(pin),
            onMapClick = onMapClick
        )

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isPaused -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f)
                    isSpoofing -> GalaxyAccent.copy(alpha = 0.2f)  // subtle cyan glow for galaxy custom mode
                    else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                }
            )
        ) {
            Text(
                text = when {
                    isPaused -> "⏸ Paused — using real location"
                    isSpoofing -> "● Custom location active"
                    else -> "Tap to pick location"
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    isPaused -> MaterialTheme.colorScheme.onErrorContainer
                    isSpoofing -> GalaxyAccent
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }

        FloatingActionButton(
            onClick = onRecenter,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Recenter map")
        }
    }
}
