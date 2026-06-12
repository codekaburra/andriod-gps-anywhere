package com.gpsanywhere.app.ui.location

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.data.DefaultLocationSeeder.DefaultLocationAsset
import com.gpsanywhere.app.data.DefaultLocationSeeder.DefaultLocationPack
import com.gpsanywhere.app.data.SavedLocation
import com.gpsanywhere.app.location.CurrentLocationProvider
import com.gpsanywhere.app.routes.LocationPoint
import com.gpsanywhere.app.service.SpoofService
import com.gpsanywhere.app.ui.components.MapViewComposable
import com.gpsanywhere.app.util.parseClipboardCoordinates
import com.gpsanywhere.app.viewmodel.LocationViewModel
import org.osmdroid.util.GeoPoint

private sealed class PendingLocation {
    abstract val name: String
    abstract val latitude: Double
    abstract val longitude: Double
    abstract val selectionKey: String
    abstract val tags: List<String>

    data class Prebuilt(val asset: DefaultLocationAsset) : PendingLocation() {
        override val name get() = asset.name
        override val latitude get() = asset.latitude
        override val longitude get() = asset.longitude
        override val selectionKey get() = "prebuilt_${asset.sourceId}"
        override val tags: List<String>
            get() = if (asset.tags.isBlank()) emptyList() else asset.tags.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    }

    data class Custom(val location: SavedLocation) : PendingLocation() {
        override val name get() = location.name
        override val latitude get() = location.latitude
        override val longitude get() = location.longitude
        override val selectionKey get() = "custom_${location.id}"
        override val tags get() = location.tagList
    }
}

private fun PendingLocation?.matches(other: PendingLocation?): Boolean =
    this != null && other != null && selectionKey == other.selectionKey

private fun coordinatesMatch(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double
): Boolean =
    "%.6f".format(lat1) == "%.6f".format(lat2) &&
        "%.6f".format(lng1) == "%.6f".format(lng2)

private fun activeLocationKey(
    lat: Double?,
    lng: Double?,
    isSpoofing: Boolean,
    isWalkMode: Boolean,
    locationPacks: List<DefaultLocationPack>,
    customLocations: List<SavedLocation>
): String? {
    if (isWalkMode || !isSpoofing || lat == null || lng == null) return null

    locationPacks.flatMap { it.locations }.firstOrNull { asset ->
        coordinatesMatch(asset.latitude, asset.longitude, lat, lng)
    }?.let { return "prebuilt_${it.sourceId}" }

    customLocations.firstOrNull { loc ->
        coordinatesMatch(loc.latitude, loc.longitude, lat, lng)
    }?.let { return "custom_${it.id}" }

    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    viewModel: LocationViewModel,
    modifier: Modifier = Modifier
) {
    val locationPacks by viewModel.locationPacks.collectAsState()
    val customLocations by viewModel.customLocations.observeAsState(emptyList())
    val routeHints by viewModel.routeHints.collectAsState()

    val prebuiltLocations = remember(locationPacks) {
        locationPacks.flatMap { pack -> pack.locations.map { pack.packName to it } }
    }

    val isSpoofing by viewModel.isSpoofing.observeAsState(false)
    val isWalkMode by SpoofService.isWalkMode.observeAsState(false)
    val currentLat by CurrentLocationProvider.latitude.observeAsState()
    val currentLng by CurrentLocationProvider.longitude.observeAsState()
    val spiralSpeed by viewModel.spiralSpeedKmh.collectAsState()
    val liveSpeedKmh by SpoofService.currentSpeedKmh.observeAsState(0f)


    var showAddSheet by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<PendingLocation?>(null) }
    var walkBreakLocation by remember { mutableStateOf<PendingLocation?>(null) }
    var deleteLocation by remember { mutableStateOf<SavedLocation?>(null) }
    var editLocation by remember { mutableStateOf<SavedLocation?>(null) }

    // Custom location jump panel
    var jumpCoordinateText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    val activeLocationKey = remember(
        currentLat,
        currentLng,
        isSpoofing,
        isWalkMode,
        locationPacks,
        customLocations
    ) {
        activeLocationKey(currentLat, currentLng, isSpoofing, isWalkMode, locationPacks, customLocations)
    }

    fun onLocationSelected(pending: PendingLocation) {
        selectedLocation = pending
    }

    fun applyJump(pending: PendingLocation) {
        when (pending) {
            is PendingLocation.Prebuilt -> viewModel.startSpoofing(pending.asset)
            is PendingLocation.Custom -> viewModel.startSpoofing(pending.location)
        }
        selectedLocation = null
    }

    fun applySpiral(pending: PendingLocation) {
        when (pending) {
            is PendingLocation.Prebuilt -> viewModel.startSpiralWalk(pending.asset)
            is PendingLocation.Custom -> viewModel.startSpiralWalk(pending.location)
        }
        selectedLocation = null
    }

    fun onJump(pending: PendingLocation) {
        if (isWalkMode) {
            walkBreakLocation = pending
        } else {
            applySpiral(pending)
        }
    }

    fun onSpiral(pending: PendingLocation) {
        if (isWalkMode) {
            walkBreakLocation = pending  // reuse same walk-break dialog; user confirms stop+restart
        } else {
            applySpiral(pending)
        }
    }

    val mapCenter: GeoPoint? = when {
        selectedLocation != null ->
            GeoPoint(selectedLocation!!.latitude, selectedLocation!!.longitude)
        isSpoofing && currentLat != null && currentLng != null ->
            GeoPoint(currentLat!!, currentLng!!)
        currentLat != null && currentLng != null ->
            GeoPoint(currentLat!!, currentLng!!)
        else -> null
    }

    val previewPoint: LocationPoint? = when {
        selectedLocation != null ->
            LocationPoint(selectedLocation!!.latitude, selectedLocation!!.longitude, selectedLocation!!.name)
        isSpoofing && currentLat != null && currentLng != null ->
            LocationPoint(currentLat!!, currentLng!!, "Current position")
        currentLat != null && currentLng != null ->
            LocationPoint(currentLat!!, currentLng!!, "Current position")
        else -> null
    }

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Walk-mode banner ──────────────────────────────────────────────
            if (isWalkMode) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.DirectionsWalk,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Walk Around active",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            // Live speed badge
                            Text(
                                "${"%.1f".format(liveSpeedKmh)} km/h",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Speed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Slider(
                                value = spiralSpeed,
                                onValueChange = { viewModel.setSpiralSpeed(it) },
                                valueRange = 0f..20f,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${"%.0f".format(spiralSpeed)} km/h",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        // Stop button
                        OutlinedButton(
                            onClick = { viewModel.stopSpoofing() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Stop Walk Around")
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            item {
                if (mapCenter != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        MapViewComposable(
                            modifier = Modifier.fillMaxSize(),
                            center = mapCenter,
                            zoom = 15.0,
                            waypoints = previewPoint?.let { listOf(it) } ?: emptyList()
                        )

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            IconButton(onClick = { showAddSheet = true }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add location",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            IconButton(onClick = { showAddSheet = true }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add location",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            item {
                CustomJumpPanel(
                    coordinateText = jumpCoordinateText,
                    onCoordinateChange = { jumpCoordinateText = it },
                    onJump = {
                        val parsed = parseClipboardCoordinates(jumpCoordinateText.trim())
                        if (parsed != null) {
                            // parseClipboardCoordinates returns Pair(lng, lat)
                            viewModel.startSpiralWalk(parsed.second, parsed.first)
                        }
                    },
                    onPaste = {
                        val raw = clipboardManager.getText()?.text?.trim().orEmpty()
                        val parsed = parseClipboardCoordinates(raw)
                        if (parsed != null) {
                            // parseClipboardCoordinates returns Pair(lng, lat)
                            jumpCoordinateText = "%.6f,%.6f".format(parsed.second, parsed.first)
                        } else {
                            jumpCoordinateText = raw
                        }
                    }
                )
            }

            item {
                SectionHeader(title = "Saved Locations")
            }

            locationPacks.filter { it.locations.isNotEmpty() }.forEach { pack ->
                item(key = "pack_header_${pack.packName}") {
                    SectionHeader(title = pack.packName)
                }
                items(pack.locations, key = { "prebuilt_${it.sourceId}" }) { asset ->
                    val pending = PendingLocation.Prebuilt(asset)
                    LocationCard(
                        name = asset.name,
                        nameEng = asset.nameEng,
                        latitude = asset.latitude,
                        longitude = asset.longitude,
                        tags = pending.tags,
                        routeHint = viewModel.routeHintFor(asset.name, asset.latitude, asset.longitude, routeHints),
                        isSelected = selectedLocation.matches(pending),
                        isActive = !selectedLocation.matches(pending) &&
                            activeLocationKey == pending.selectionKey,
                        showJumpButton = selectedLocation.matches(pending),
                        onClick = { onLocationSelected(pending) },
                        onJump = { onJump(pending) },
                        onSpiral = { onSpiral(pending) },
                        onDelete = null
                    )
                }
            }

            if (prebuiltLocations.isNotEmpty() && customLocations.isNotEmpty()) {
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
            }

            item {
                SectionHeader(title = "My Locations")
            }

            if (customLocations.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.LocationOn,
                        title = "No custom locations",
                        body = "Tap + to add your own location"
                    )
                }
            } else {
                items(customLocations, key = { it.id }) { loc ->
                    val pending = PendingLocation.Custom(loc)
                    LocationCard(
                        name = loc.name,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        tags = loc.tagList,
                        routeHint = null,
                        isSelected = selectedLocation.matches(pending),
                        isActive = !selectedLocation.matches(pending) &&
                            activeLocationKey == pending.selectionKey,
                        showJumpButton = selectedLocation.matches(pending),
                        onClick = { onLocationSelected(pending) },
                        onJump = { onJump(pending) },
                        onSpiral = { onSpiral(pending) },
                        onEdit = { editLocation = loc },
                        onDelete = { deleteLocation = loc }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    // ── Walk-break warning dialogs ────────────────────────────────────────────
    walkBreakLocation?.let { loc ->
        AlertDialog(
            onDismissRequest = { walkBreakLocation = null },
            title = { Text("Stop walk mode?") },
            text = { Text("Starting Walk Around at \"${loc.name}\" will stop the current walk. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    applySpiral(loc)
                    walkBreakLocation = null
                }) { Text("Stop & start new walk") }
            },
            dismissButton = {
                TextButton(onClick = { walkBreakLocation = null }) { Text("Cancel") }
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

    editLocation?.let { loc ->
        AddLocationSheet(
            title = "Edit Location",
            initialName = loc.name,
            initialLat = "%.6f".format(loc.latitude),
            initialLng = "%.6f".format(loc.longitude),
            onDismiss = { editLocation = null },
            onSave = { name, lat, lng ->
                viewModel.updateLocation(loc, name, lat, lng)
                editLocation = null
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
    name: String,
    nameEng: String = "",
    latitude: Double,
    longitude: Double,
    tags: List<String> = emptyList(),
    routeHint: String?,
    isSelected: Boolean,
    isActive: Boolean,
    showJumpButton: Boolean,
    onClick: () -> Unit,
    onJump: () -> Unit,
    onSpiral: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)?
) {
    val border = when {
        isSelected || isActive -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            // ── Info row ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Column(
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                ) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (nameEng.isNotBlank()) {
                        Text(
                            nameEng,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (tags.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            tags.take(3).forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (tags.size > 3) {
                                Text(
                                    "+${tags.size - 3}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
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
                        "${"%.6f".format(latitude)}, ${"%.6f".format(longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                if (!showJumpButton && (onEdit != null || onDelete != null)) {
                    if (onEdit != null) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary
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

            // ── Expanded: speed + action buttons ──────────────────────────────
            if (showJumpButton) {
                Spacer(Modifier.height(10.dp))
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onJump, modifier = Modifier.weight(1f)) {
                        Text("Walk Around")
                    }
                    Button(onClick = onSpiral, modifier = Modifier.weight(1f)) {
                        Text("Walk Around")
                    }
                }
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
    onSave: (name: String, lat: Double, lng: Double) -> Unit,
    title: String = "Add Location",
    initialName: String = "",
    initialLat: String = "",
    initialLng: String = ""
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current

    var name by remember { mutableStateOf(initialName) }
    var latText by remember { mutableStateOf(initialLat) }
    var lngText by remember { mutableStateOf(initialLng) }
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
            Text(title, style = MaterialTheme.typography.titleLarge)

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
                    value = latText,
                    onValueChange = { latText = it; error = null },
                    label = { Text("Latitude") },
                    placeholder = { Text("Latitude") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = lngText,
                    onValueChange = { lngText = it; error = null },
                    label = { Text("Longitude") },
                    placeholder = { Text("Longitude") },
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
                        error = "Invalid Format [$raw]. Clipboard must be latitude, longitude"
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
                "Format: latitude, longitude  (e.g. 25.0330, 121.5654)",
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

@Composable
private fun CustomJumpPanel(
    coordinateText: String,
    onCoordinateChange: (String) -> Unit,
    onJump: () -> Unit,
    onPaste: () -> Unit
) {
    val parsed = parseClipboardCoordinates(coordinateText.trim())
    val hasInput = coordinateText.isNotBlank()
    val isValid = parsed != null
    val canJump = hasInput && isValid

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = coordinateText,
                    onValueChange = onCoordinateChange,
                    label = { Text("Coordinate") },
                    placeholder = { Text("22.3168,114.0451") },
                    isError = hasInput && !isValid,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onPaste,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Button(
                    onClick = onJump,
                    enabled = canJump,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (hasInput && !isValid) {
                Text(
                    text = "Use format: latitude,longitude (e.g. 22.3168,114.0451)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

