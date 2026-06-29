package com.gpsanywhere.app.ui.location

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.res.painterResource
import com.gpsanywhere.app.R
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.data.DefaultLocationSeeder.DefaultLocationAsset
import com.gpsanywhere.app.data.DefaultLocationSeeder.DefaultLocationPack
import com.gpsanywhere.app.data.SavedLocation
import com.gpsanywhere.app.location.CurrentLocationProvider
import com.gpsanywhere.app.routes.LocationPoint
import com.gpsanywhere.app.service.SpoofService
import com.gpsanywhere.app.ui.components.GlassCard
import com.gpsanywhere.app.ui.components.MapViewComposable
import com.gpsanywhere.app.util.parseClipboardCoordinates
import com.gpsanywhere.app.viewmodel.LocationViewModel
import com.gpsanywhere.app.viewmodel.LocationViewModel.Companion.FLY_HELI_KMH
import com.gpsanywhere.app.viewmodel.LocationViewModel.Companion.FLY_FLIGHT_KMH
import com.gpsanywhere.app.viewmodel.LocationViewModel.Companion.FLY_ROCKET_KMH
import com.gpsanywhere.app.viewmodel.LocationViewModel.Companion.MAX_SPEED_KMH
import com.gpsanywhere.app.viewmodel.LocationViewModel.Companion.MOVE_STEP_DEG
import org.osmdroid.util.GeoPoint

private const val WALK_ZONE_KMH = 20f
private const val WALK_ZONE_FRAC = 0.8f

private fun speedToSlider(kmh: Float): Float {
    return if (kmh <= WALK_ZONE_KMH) {
        (kmh / WALK_ZONE_KMH) * WALK_ZONE_FRAC
    } else {
        WALK_ZONE_FRAC + ((kmh - WALK_ZONE_KMH) / (MAX_SPEED_KMH - WALK_ZONE_KMH)) * (1f - WALK_ZONE_FRAC)
    }
}

private fun sliderToSpeed(frac: Float): Float {
    return if (frac <= WALK_ZONE_FRAC) {
        (frac / WALK_ZONE_FRAC) * WALK_ZONE_KMH
    } else {
        WALK_ZONE_KMH + ((frac - WALK_ZONE_FRAC) / (1f - WALK_ZONE_FRAC)) * (MAX_SPEED_KMH - WALK_ZONE_KMH)
    }
}

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
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var customOnly by remember { mutableStateOf(false) }

    val allTags = remember(locationPacks, customLocations) {
        buildSet {
            locationPacks.flatMap { it.locations }.forEach { asset ->
                if (asset.tags.isNotBlank())
                    asset.tags.split("|").map { it.trim() }.filter { it.isNotEmpty() }.forEach { add(it) }
            }
            customLocations.forEach { addAll(it.tagList) }
        }.sorted()
    }

    val filteredPacks = remember(locationPacks, selectedTag, customOnly) {
        if (customOnly) emptyList()
        else if (selectedTag == null) locationPacks
        else locationPacks.mapNotNull { pack ->
            val locs = pack.locations.filter { asset ->
                val t = if (asset.tags.isBlank()) emptyList()
                    else asset.tags.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                selectedTag in t
            }
            if (locs.isEmpty()) null else pack.copy(locations = locs)
        }
    }

    val filteredCustom = remember(customLocations, selectedTag) {
        if (selectedTag == null) customLocations
        else customLocations.filter { loc -> selectedTag in loc.tagList }
    }

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
        when (pending) {
            is PendingLocation.Prebuilt -> viewModel.startSpoofing(pending.asset)
            is PendingLocation.Custom -> viewModel.startSpoofing(pending.location)
        }
        selectedLocation = null
    }

    fun onFly(pending: PendingLocation, speedKmh: Float) {
        when (pending) {
            is PendingLocation.Prebuilt -> viewModel.flyTo(pending.asset, speedKmh)
            is PendingLocation.Custom -> viewModel.flyTo(pending.location, speedKmh)
        }
        selectedLocation = null
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

    Scaffold(modifier = modifier, containerColor = Color.Transparent) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Walk-mode banner ──────────────────────────────────────────────
            if (isWalkMode) {
                val panelDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val panelBg = if (panelDark) Color(0xFF1E2937).copy(alpha = 0.8f)
                              else Color.White.copy(alpha = 0.8f)
                Surface(
                    color = panelBg,
                    shape = RoundedCornerShape(0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 1.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.DirectionsWalk,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    "",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${"%.1f".format(liveSpeedKmh)} km/h",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ResetIntervalInput()
                                ResetCountdown()
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Speed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Slider(
                                value = speedToSlider(spiralSpeed),
                                onValueChange = { viewModel.setSpiralSpeed(sliderToSpeed(it)) },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = com.gpsanywhere.app.ui.theme.SliderThumb.copy(alpha = 0.9f),
                                    activeTrackColor = com.gpsanywhere.app.ui.theme.SliderActiveTrack.copy(alpha = 0.9f),
                                    inactiveTrackColor = com.gpsanywhere.app.ui.theme.SliderInactiveTrack.copy(alpha = 0.9f)
                                )
                            )
                            Text(
                                if (spiralSpeed < 10f) "${"%.1f".format(spiralSpeed)} km/h"
                                else "${"%.0f".format(spiralSpeed)} km/h",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        // Stop button
                        Button(
                            onClick = { viewModel.stopSpoofing() },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = com.gpsanywhere.app.ui.theme.StopRed.copy(alpha = 0.9f),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Text("Stop")
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
//                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            color = com.gpsanywhere.app.ui.theme.CandyYellow.copy(alpha = 0.8f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            IconButton(onClick = { showAddSheet = true }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add location",
                                    tint = Color.White
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
                            color = com.gpsanywhere.app.ui.theme.CandyYellow.copy(alpha = 0.8f)
                        ) {
                            IconButton(onClick = { showAddSheet = true }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add location",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            item {
                GlassDirectionPadCard(
                    enabled = currentLat != null && currentLng != null,
                    onMove = { dLat, dLng -> viewModel.nudgeSpiral(dLat, dLng) }
                )
            }

            item {
                CustomJumpPanel(
                    coordinateText = jumpCoordinateText,
                    onCoordinateChange = { jumpCoordinateText = it },
                    onJump = {
                        val parsed = parseClipboardCoordinates(jumpCoordinateText.trim())
                        if (parsed != null) {
                            viewModel.startSpoofing(parsed.second, parsed.first)
                        }
                    },
                    onSpiral = {
                        val parsed = parseClipboardCoordinates(jumpCoordinateText.trim())
                        if (parsed != null) {
                            viewModel.startSpiralWalk(parsed.second, parsed.first)
                        }
                    },
                    onFly = { speed ->
                        val parsed = parseClipboardCoordinates(jumpCoordinateText.trim())
                        if (parsed != null) {
                            viewModel.flyTo(parsed.second, parsed.first, speed)
                        }
                    },
                    onPaste = {
                        val raw = clipboardManager.getText()?.text?.trim().orEmpty()
                        val parsed = parseClipboardCoordinates(raw)
                        if (parsed != null) {
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

            item(key = "tag_filter") {
                TagFilterRow(
                    allTags = allTags,
                    selectedTags = if (selectedTag != null) setOf(selectedTag!!) else emptySet(),
                    onTagToggle = { tag ->
                        selectedTag = if (selectedTag == tag) null else tag
                    },
                    customOnly = customOnly,
                    onCustomToggle = { customOnly = !customOnly }
                )
            }

            filteredPacks.filter { it.locations.isNotEmpty() }.forEach { pack ->
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
                        onFly = { speed -> onFly(pending, speed) },
                        onDelete = null
                    )
                }
            }

            if (filteredPacks.isNotEmpty() && filteredCustom.isNotEmpty()) {
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
                items(filteredCustom, key = { it.id }) { loc ->
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
                        onFly = { speed -> onFly(pending, speed) },
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TagFilterRow(
    allTags: List<String>,
    selectedTags: Set<String>,
    onTagToggle: (String) -> Unit,
    customOnly: Boolean = false,
    onCustomToggle: () -> Unit = {}
) {
    val chipColors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
        selectedContainerColor = Color.White.copy(alpha = 0.45f),
        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = customOnly,
            onClick = onCustomToggle,
            label = { Text("Custom") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp)) },
            colors = chipColors,
            border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = customOnly,
                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                selectedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        )
        allTags.forEach { tag ->
            val isSelected = tag in selectedTags
            FilterChip(
                selected = isSelected,
                onClick = { onTagToggle(tag) },
                label = { Text(tag) },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color.White.copy(alpha = 0.45f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    selectedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface
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
    onFly: (Float) -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)?
) {
    val selected = isSelected || isActive
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val nameColor = when {
        selected -> com.gpsanywhere.app.ui.theme.CardNameTextSelected
        isDark -> com.gpsanywhere.app.ui.theme.CardNameTextDark
        else -> com.gpsanywhere.app.ui.theme.CardNameText
    }
    val coordColor = when {
        selected -> com.gpsanywhere.app.ui.theme.CardCoordTextSelected
        isDark -> com.gpsanywhere.app.ui.theme.CardCoordTextDark
        else -> com.gpsanywhere.app.ui.theme.CardCoordText
    }
    val border = if (selected)
        BorderStroke(2.5.dp, com.gpsanywhere.app.ui.theme.CardBorderSelected)
    else
        BorderStroke(1.5.dp, com.gpsanywhere.app.ui.theme.CardBorder)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) com.gpsanywhere.app.ui.theme.CardFillSelected
                             else com.gpsanywhere.app.ui.theme.CardFill
        ),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (selected) com.gpsanywhere.app.ui.theme.IconActive
                           else com.gpsanywhere.app.ui.theme.CardCoordText,
                    modifier = Modifier.size(26.dp)
                )
                val nameAnnotated = buildAnnotatedString {
                    withStyle(SpanStyle(
                        fontSize = MaterialTheme.typography.titleSmall.fontSize,
                        fontWeight = MaterialTheme.typography.titleSmall.fontWeight
                    )) { append(name) }
                    if (nameEng.isNotBlank()) {
                        withStyle(SpanStyle(
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                            color = coordColor
                        )) { append("  $nameEng") }
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        nameAnnotated,
                        style = MaterialTheme.typography.titleSmall,
                        color = nameColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    routeHint?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = coordColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        "${"%.6f".format(latitude)}, ${"%.6f".format(longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = coordColor
                    )
                }
                if (tags.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(start = 8.dp).widthIn(max = 80.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        tags.take(3).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            ) {
                                Text(
                                    tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TransportButtons(
                        enabled = true,
                        onJump = onJump,
                        onSpiral = onSpiral,
                        onFly = onFly
                    )
                }
            }
        }
    }
}

/**
 * The five transport action buttons — Jump, Walk Around, Heli, Flight, Rocket —
 * rendered with a single shared style so all stay in sync. Emits the buttons into
 * the caller's Row.
 */
@Composable
private fun TransportButtons(
    enabled: Boolean,
    onJump: () -> Unit,
    onSpiral: () -> Unit,
    onFly: (Float) -> Unit,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp
) {
    val colors = IconButtonDefaults.filledIconButtonColors(
        containerColor = com.gpsanywhere.app.ui.theme.CandyYellow.copy(alpha = 0.8f),
        contentColor = Color.White,
        disabledContainerColor = com.gpsanywhere.app.ui.theme.CandyYellow.copy(alpha = 0.35f),
        disabledContentColor = Color.White.copy(alpha = 0.5f)
    )
    FilledIconButton(onClick = onJump, enabled = enabled, colors = colors) {
        Icon(Icons.Default.DoorFront, contentDescription = "Jump", modifier = Modifier.size(iconSize))
    }
    Spacer(Modifier.width(4.dp))
    FilledIconButton(onClick = onSpiral, enabled = enabled, colors = colors) {
        Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = "Walk Around", modifier = Modifier.size(iconSize))
    }
    Spacer(Modifier.width(4.dp))
    FilledIconButton(onClick = { onFly(FLY_HELI_KMH) }, enabled = enabled, colors = colors) {
        Icon(painterResource(R.drawable.ic_helicopter), contentDescription = "80 km/h", modifier = Modifier.size(iconSize))
    }
    Spacer(Modifier.width(4.dp))
    FilledIconButton(onClick = { onFly(FLY_FLIGHT_KMH) }, enabled = enabled, colors = colors) {
        Icon(Icons.Default.Flight, contentDescription = "500 km/h", modifier = Modifier.size(iconSize))
    }
    Spacer(Modifier.width(4.dp))
    FilledIconButton(onClick = { onFly(FLY_ROCKET_KMH) }, enabled = enabled, colors = colors) {
        Icon(Icons.Default.RocketLaunch, contentDescription = "2000 km/h", modifier = Modifier.size(iconSize))
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
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Name") },
                singleLine = true,
                colors = fieldColors,
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
                    colors = fieldColors,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = lngText,
                    onValueChange = { lngText = it; error = null },
                    label = { Text("Longitude") },
                    placeholder = { Text("Longitude") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = fieldColors,
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
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = com.gpsanywhere.app.ui.theme.CandyYellow
                ),
                border = BorderStroke(1.dp, com.gpsanywhere.app.ui.theme.CandyYellow.copy(alpha = 0.6f))
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
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) { Text("Cancel") }
                Button(
                    onClick = {
                        val n = name.trim()
                        val lat = latText.trim().toDoubleOrNull()
                        val lng = lngText.trim().toDoubleOrNull()
                        when {
                            n.isEmpty() -> error = "Name is required"
                            lng == null || lng !in -180.0..180.0 -> error = "Longitude must be between -180 and 180"
                            lat == null || lat !in -90.0..90.0 -> error = "Latitude must be between -90 and 90"
                            else -> onSave(n, lat, lng)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.gpsanywhere.app.ui.theme.GlassGreen,
                        contentColor = Color.White
                    )
                ) { Text("Save") }
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
    onSpiral: () -> Unit,
    onFly: (Float) -> Unit,
    onPaste: () -> Unit
) {
    val parsed = parseClipboardCoordinates(coordinateText.trim())
    val hasInput = coordinateText.isNotBlank()
    val isValid = parsed != null
    val canJump = hasInput && isValid
    GlassCard(modifier = Modifier.fillMaxWidth()) {
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilledIconButton(
                    onClick = onPaste,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = com.gpsanywhere.app.ui.theme.CandyYellow.copy(alpha = 0.8f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        Icons.Default.ContentPaste,
                        contentDescription = "Paste",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TransportButtons(
                    enabled = canJump,
                    onJump = onJump,
                    onSpiral = onSpiral,
                    onFly = onFly,
                    iconSize = 18.dp
                )
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

@Composable
private fun GlassDirectionPadCard(
    enabled: Boolean,
    onMove: (dLat: Double, dLng: Double) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), filled = false) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sectored circular glass direction pad
            SectoredDpad(enabled = enabled, onMove = onMove)
        }
    }
}

/**
 * A circular D-pad split into four wedges (up / down / left / right) by diagonal
 * dividers, with chevron arrows in each wedge and a solid centre button. Tapping a
 * wedge nudges the target in that direction; press-and-hold repeats. The centre
 * recentres the spiral on the current position.
 */
@Composable
private fun SectoredDpad(
    enabled: Boolean,
    onMove: (dLat: Double, dLng: Double) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val discColor = if (isDark) Color(0xFF1E2937).copy(alpha = 0.42f) else Color.White.copy(alpha = 0.40f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.5f)
    val arrowColor = if (enabled) {
        if (isDark) Color.White else Color(0xFF334155)
    } else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val scope = rememberCoroutineScope()

    val size = 196.dp
    val centerButton = 73.dp
    val arrowOffset = 64.dp   // distance of each chevron from the centre

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Disc + diagonal dividers, with wedge tap handling (hold to repeat)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val c = Offset(this.size.width / 2f, this.size.height / 2f)
                        val centerRadiusPx = centerButton.toPx() / 2f
                        val dx = down.position.x - c.x
                        val dy = down.position.y - c.y
                        // Ignore taps inside the centre button area
                        if (dx * dx + dy * dy < centerRadiusPx * centerRadiusPx) return@awaitEachGesture
                        val move: () -> Unit = if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                            if (dx < 0) { { onMove(0.0, -LocationViewModel.MOVE_STEP_DEG) } }
                            else { { onMove(0.0, LocationViewModel.MOVE_STEP_DEG) } }
                        } else {
                            if (dy < 0) { { onMove(LocationViewModel.MOVE_STEP_DEG, 0.0) } }
                            else { { onMove(-LocationViewModel.MOVE_STEP_DEG, 0.0) } }
                        }
                        move()
                        val repeatJob = scope.launch {
                            delay(300)
                            while (true) {
                                move()
                                delay(120)
                            }
                        }
                        waitForUpOrCancellation()
                        repeatJob.cancel()
                    }
                }
        ) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerR = this.size.minDimension / 2f
            // disc fill + border
            drawCircle(color = discColor, radius = outerR, center = c)
            drawCircle(
                color = borderColor,
                radius = outerR - 0.5.dp.toPx(),
                center = c,
                style = Stroke(width = 1.dp.toPx())
            )
            // four diagonal dividers (the X that splits the wedges)
            val innerR = centerButton.toPx() / 2f + 6.dp.toPx()
            listOf(45.0, 135.0, 225.0, 315.0).forEach { deg ->
                val a = Math.toRadians(deg)
                val start = Offset(c.x + (kotlin.math.cos(a) * innerR).toFloat(), c.y + (kotlin.math.sin(a) * innerR).toFloat())
                val end = Offset(c.x + (kotlin.math.cos(a) * outerR).toFloat(), c.y + (kotlin.math.sin(a) * outerR).toFloat())
                drawLine(borderColor, start, end, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
        }

        // Chevron arrows (visual only — taps handled by the Canvas below)
        Icon(
            Icons.Default.KeyboardArrowUp, "North",
            tint = arrowColor,
            modifier = Modifier.size(24.dp).align(Alignment.Center).offset(y = -arrowOffset)
        )
        Icon(
            Icons.Default.KeyboardArrowDown, "South",
            tint = arrowColor,
            modifier = Modifier.size(24.dp).align(Alignment.Center).offset(y = arrowOffset)
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowLeft, "West",
            tint = arrowColor,
            modifier = Modifier.size(24.dp).align(Alignment.Center).offset(x = -arrowOffset)
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, "East",
            tint = arrowColor,
            modifier = Modifier.size(24.dp).align(Alignment.Center).offset(x = arrowOffset)
        )

        // Centre tap target (recenter spiral) — invisible, no fill or label
        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            onClick = { onMove(0.0, 0.0) },
            enabled = enabled,
            modifier = Modifier.size(centerButton)
        ) {}
    }
}

@Composable
private fun ResetCountdown() {
    val deadline by SpoofService.resetDeadlineMs.observeAsState(0L)
    if (deadline <= 0L) return
    var remainingSec by remember { mutableStateOf(0L) }
    LaunchedEffect(deadline) {
        while (true) {
            val left = (deadline - System.currentTimeMillis()) / 1000L
            remainingSec = if (left > 0) left else 0
            kotlinx.coroutines.delay(1000)
        }
    }
    if (remainingSec > 0) {
        val mins = remainingSec / 60
        val secs = remainingSec % 60
        Text(
            "%d:%02d".format(mins, secs),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ResetIntervalInput() {
    val defaultMin = LocationViewModel.SPIRAL_RESET_INTERVAL_MS / 60_000L
    var text by remember { mutableStateOf(defaultMin.toString()) }
    val labelColor = MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Reset every", style = MaterialTheme.typography.bodyMedium, color = labelColor)
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                text = input.filter { it.isDigit() }
                val mins = text.toLongOrNull()
                if (mins != null && mins > 0) {
                    SpoofService.liveResetIntervalMs = mins * 60_000L
                }
            },
            modifier = Modifier.width(72.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true
        )
        Text("min", style = MaterialTheme.typography.bodyMedium, color = labelColor)
    }
}
