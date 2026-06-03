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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.data.SavedLocation
import com.gpsanywhere.app.viewmodel.SavedLocationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    viewModel: SavedLocationsViewModel,
    modifier: Modifier = Modifier
) {
    val locations by viewModel.locations.observeAsState(emptyList())
    val isSpoofing by viewModel.isSpoofing.observeAsState(false)

    var showAddSheet by remember { mutableStateOf(false) }
    var confirmLocation by remember { mutableStateOf<SavedLocation?>(null) }
    var deleteLocation by remember { mutableStateOf<SavedLocation?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("Location") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add location")
            }
        }
    ) { padding ->
        if (locations.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(16.dp))
                Text("No saved locations", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tap + to add a custom location",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                items(locations, key = { it.id }) { loc ->
                    LocationCard(
                        location = loc,
                        onClick = { confirmLocation = loc },
                        onDelete = if (!loc.isPreinstalled) {
                            { deleteLocation = loc }
                        } else null
                    )
                }
                item { Spacer(Modifier.height(80.dp)) } // FAB clearance
            }
        }
    }

    // ── Confirm use location ───────────────────────────────────────────────
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

    // ── Confirm delete ─────────────────────────────────────────────────────
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

    // ── Add location sheet ─────────────────────────────────────────────────
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
private fun LocationCard(
    location: SavedLocation,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (location.isPreinstalled)
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
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
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
                Text(
                    "${"%.6f".format(location.latitude)}, ${"%.6f".format(location.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                location.category?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLocationSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, lat: Double, lng: Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var latText by remember { mutableStateOf("") }
    var lngText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

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
            OutlinedTextField(
                value = latText,
                onValueChange = { latText = it; error = null },
                label = { Text("Latitude") },
                placeholder = { Text("e.g. 25.0330") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = lngText,
                onValueChange = { lngText = it; error = null },
                label = { Text("Longitude") },
                placeholder = { Text("e.g. 121.5654") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = {
                    val n = name.trim()
                    val lat = latText.trim().toDoubleOrNull()
                    val lng = lngText.trim().toDoubleOrNull()
                    when {
                        n.isEmpty() -> error = "Name is required"
                        lat == null || lat !in -90.0..90.0 -> error = "Latitude must be between -90 and 90"
                        lng == null || lng !in -180.0..180.0 -> error = "Longitude must be between -180 and 180"
                        else -> onSave(n, lat, lng)
                    }
                }) { Text("Save") }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
