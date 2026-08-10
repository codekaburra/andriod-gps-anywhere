package com.gpsanywhere.app.ui.location

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import com.gpsanywhere.app.R
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.data.SavedLocation
import com.gpsanywhere.app.location.CurrentLocationProvider
import com.gpsanywhere.app.routes.LocationPoint
import com.gpsanywhere.app.service.SpoofService
import com.gpsanywhere.app.ui.components.GlassCard
import com.gpsanywhere.app.ui.components.MapViewComposable
import com.gpsanywhere.app.util.parseClipboardCoordinates
import com.gpsanywhere.app.viewmodel.LocationViewModel
import com.gpsanywhere.app.viewmodel.LocationViewModel.Companion.FLY_ROCKET_KMH
import com.gpsanywhere.app.viewmodel.LocationViewModel.Companion.MAX_SPEED_KMH
import com.gpsanywhere.app.viewmodel.LocationViewModel.Companion.MOVE_STEP_DEG
import org.osmdroid.util.GeoPoint

private const val WALK_ZONE_KMH = 20f
private const val WALK_ZONE_FRAC = 0.8f

/** Height of the pinned map preview. */
private val MAP_HEIGHT = 140.dp

/** How far the coordinate card rides up over the map (20% of the map). */
private val MAP_CARD_OVERLAP = MAP_HEIGHT * 0.2f

/**
 * Direction pad size inside the coordinate card. It drives the card's height and
 * eats into the width the transport buttons get, so keep it small enough that all
 * three buttons stay on one row on a 384dp-wide phone.
 */
private val CARD_DPAD_SIZE = 120.dp

/** Fixed transport-button footprint, so the row's width doesn't depend on touch-target padding. */
private val TRANSPORT_BUTTON_SIZE = 40.dp

/**
 * Draw this composable [overlap] higher than its slot and shrink the space it
 * occupies by the same amount, so everything below moves up too. Used to let the
 * translucent coordinate card sit over the bottom of the map instead of below it.
 */
private fun Modifier.overlapAbove(overlap: Dp) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val dy = overlap.roundToPx()
    layout(placeable.width, (placeable.height - dy).coerceAtLeast(0)) {
        placeable.place(0, -dy)
    }
}

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

private fun SavedLocation?.matches(other: SavedLocation?): Boolean =
    this != null && other != null && id == other.id

private fun SavedLocation.selectionKey(): String = "custom_$id"

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
    allLocations: List<SavedLocation>
): String? {
    if (isWalkMode || !isSpoofing || lat == null || lng == null) return null

    allLocations.firstOrNull { loc ->
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
    val allLocations by viewModel.allLocations.observeAsState(emptyList())
    val routeHints by viewModel.routeHints.collectAsState()

    val isSpoofing by viewModel.isSpoofing.observeAsState(false)
    val isWalkMode by SpoofService.isWalkMode.observeAsState(false)
    val currentLat by CurrentLocationProvider.latitude.observeAsState()
    val currentLng by CurrentLocationProvider.longitude.observeAsState()
    val spiralSpeed by viewModel.spiralSpeedKmh.collectAsState()
    val liveSpeedKmh by SpoofService.currentSpeedKmh.observeAsState(0f)


    var showAddSheet by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<SavedLocation?>(null) }
    var walkBreakLocation by remember { mutableStateOf<SavedLocation?>(null) }
    var deleteLocation by remember { mutableStateOf<SavedLocation?>(null) }
    var editLocation by remember { mutableStateOf<SavedLocation?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var customOnly by remember { mutableStateOf(false) }

    val allTags = remember(allLocations) {
        buildSet { allLocations.forEach { addAll(it.tagList) } }.sorted()
    }

    val filteredLocations = remember(allLocations, selectedTag, customOnly) {
        allLocations.filter { loc ->
            (!customOnly || !loc.isPreinstalled) &&
                (selectedTag == null || selectedTag in loc.tagList)
        }
    }

    // Custom location jump panel
    var jumpCoordinateText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    val activeLocationKey = remember(
        currentLat,
        currentLng,
        isSpoofing,
        isWalkMode,
        allLocations
    ) {
        activeLocationKey(currentLat, currentLng, isSpoofing, isWalkMode, allLocations)
    }

    fun onLocationSelected(location: SavedLocation) {
        selectedLocation = location
    }

    fun applyJump(location: SavedLocation) {
        viewModel.startSpoofing(location)
        selectedLocation = null
    }

    fun applySpiral(location: SavedLocation) {
        viewModel.startSpiralWalk(location)
        selectedLocation = null
    }

    fun onJump(location: SavedLocation) {
        viewModel.startSpoofing(location)
        selectedLocation = null
    }

    fun onFly(location: SavedLocation, speedKmh: Float) {
        viewModel.flyTo(location, speedKmh)
        selectedLocation = null
    }

    fun onSpiral(location: SavedLocation) {
        if (isWalkMode) {
            walkBreakLocation = location
        } else {
            applySpiral(location)
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

    val currentPositionLabel = stringResource(R.string.current_position)
    val previewPoint: LocationPoint? = when {
        selectedLocation != null ->
            LocationPoint(selectedLocation!!.latitude, selectedLocation!!.longitude, selectedLocation!!.name)
        isSpoofing && currentLat != null && currentLng != null ->
            LocationPoint(currentLat!!, currentLng!!, currentPositionLabel)
        currentLat != null && currentLng != null ->
            LocationPoint(currentLat!!, currentLng!!, currentPositionLabel)
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
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
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
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "${"%.1f".format(liveSpeedKmh)} km/h",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ResetIntervalInput()
                                ResetCountdown()
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                stringResource(R.string.speed),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Slider(
                                value = speedToSlider(spiralSpeed),
                                onValueChange = { viewModel.setSpiralSpeed(sliderToSpeed(it)) },
                                valueRange = 0f..1f,
                                // Trim the slider's 48dp touch slot; the track is much shorter.
                                modifier = Modifier.weight(1f).height(28.dp),
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
                            modifier = Modifier.fillMaxWidth().height(34.dp),
                            shape = RoundedCornerShape(17.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = com.gpsanywhere.app.ui.theme.StopRed.copy(alpha = 0.9f),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Text(stringResource(R.string.action_stop))
                        }
                    }
                }
            }

            // Pinned header: the map and the custom-coordinate controls stay put;
            // only the saved-locations list below them scrolls.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (mapCenter != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MAP_HEIGHT)
                            // The osmdroid MapView is an AndroidView and will paint
                            // past its slot, covering the card below, unless clipped.
                            .clipToBounds()
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
                                    contentDescription = stringResource(R.string.add_location),
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
                                    contentDescription = stringResource(R.string.add_location),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                CustomJumpPanel(
                    // Ride up over the map's lower edge; without a map there is
                    // nothing to overlap, so sit normally. zIndex keeps the card
                    // painted above the map it overlaps.
                    modifier = if (mapCenter != null) {
                        Modifier.zIndex(1f).overlapAbove(MAP_CARD_OVERLAP)
                    } else Modifier,
                    dpadEnabled = currentLat != null && currentLng != null,
                    onMove = { dLat, dLng -> viewModel.nudgeSpiral(dLat, dLng) },
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

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
            item {
                SectionHeader(title = stringResource(R.string.saved_locations))
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

            if (filteredLocations.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.LocationOn,
                        title = stringResource(
                            if (allLocations.isEmpty()) R.string.no_locations_yet
                            else R.string.no_matching_locations
                        ),
                        body = stringResource(
                            if (allLocations.isEmpty()) R.string.no_locations_hint
                            else R.string.try_different_filter
                        )
                    )
                }
            } else {
                items(filteredLocations, key = { it.id }) { loc ->
                    LocationCard(
                        name = loc.name,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        tags = loc.tagList,
                        routeHint = viewModel.routeHintFor(loc.name, loc.latitude, loc.longitude, routeHints),
                        isSelected = selectedLocation.matches(loc),
                        isActive = !selectedLocation.matches(loc) &&
                            activeLocationKey == loc.selectionKey(),
                        showJumpButton = selectedLocation.matches(loc),
                        onClick = { onLocationSelected(loc) },
                        onJump = { onJump(loc) },
                        onSpiral = { onSpiral(loc) },
                        onFly = { speed -> onFly(loc, speed) },
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
            title = { Text(stringResource(R.string.dialog_stop_walk_title)) },
            text = { Text(stringResource(R.string.dialog_stop_walk_text, loc.name)) },
            confirmButton = {
                TextButton(onClick = {
                    applySpiral(loc)
                    walkBreakLocation = null
                }) { Text(stringResource(R.string.stop_and_start_new_walk)) }
            },
            dismissButton = {
                TextButton(onClick = { walkBreakLocation = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    deleteLocation?.let { loc ->
        AlertDialog(
            onDismissRequest = { deleteLocation = null },
            title = { Text(stringResource(R.string.dialog_delete_location_title)) },
            text = { Text(stringResource(R.string.dialog_delete_location_text, loc.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLocation(loc)
                    deleteLocation = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteLocation = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    editLocation?.let { loc ->
        AddLocationSheet(
            title = stringResource(R.string.edit_location),
            initialName = loc.name,
            initialLat = "%.6f".format(loc.latitude),
            initialLng = "%.6f".format(loc.longitude),
            initialTags = loc.tagList.joinToString(" | "),
            onDismiss = { editLocation = null },
            onSave = { name, lat, lng, tags ->
                viewModel.updateLocation(loc, name, lat, lng, tags)
                editLocation = null
            }
        )
    }

    if (showAddSheet) {
        AddLocationSheet(
            title = stringResource(R.string.add_location),
            onDismiss = { showAddSheet = false },
            onSave = { name, lat, lng, tags ->
                viewModel.addLocation(name, lat, lng, tags)
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
            label = { Text(stringResource(R.string.filter_custom)) },
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
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
    // Lets a narrow caller pin the button footprint instead of inheriting the
    // 48dp minimum touch target, which is what pushes buttons onto a second row.
    buttonModifier: Modifier = Modifier
) {
    val colors = IconButtonDefaults.filledIconButtonColors(
        containerColor = com.gpsanywhere.app.ui.theme.CandyYellow.copy(alpha = 0.8f),
        contentColor = Color.White,
        disabledContainerColor = com.gpsanywhere.app.ui.theme.CandyYellow.copy(alpha = 0.35f),
        disabledContentColor = Color.White.copy(alpha = 0.5f)
    )
    FilledIconButton(onClick = onJump, enabled = enabled, colors = colors, modifier = buttonModifier) {
        Icon(Icons.Default.DoorFront, contentDescription = stringResource(R.string.transport_jump), modifier = Modifier.size(iconSize))
    }
    Spacer(Modifier.width(4.dp))
    FilledIconButton(onClick = onSpiral, enabled = enabled, colors = colors, modifier = buttonModifier) {
        Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = stringResource(R.string.transport_walk_around), modifier = Modifier.size(iconSize))
    }
    Spacer(Modifier.width(4.dp))
    FilledIconButton(onClick = { onFly(FLY_ROCKET_KMH) }, enabled = enabled, colors = colors, modifier = buttonModifier) {
        Icon(Icons.Default.RocketLaunch, contentDescription = stringResource(R.string.transport_rocket), modifier = Modifier.size(iconSize))
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
    onSave: (name: String, lat: Double, lng: Double, tags: String) -> Unit,
    title: String,
    initialName: String = "",
    initialLat: String = "",
    initialLng: String = "",
    initialTags: String = ""
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current
    val sheetContext = LocalContext.current

    var name by remember { mutableStateOf(initialName) }
    var latText by remember { mutableStateOf(initialLat) }
    var lngText by remember { mutableStateOf(initialLng) }
    var tagsText by remember { mutableStateOf(initialTags) }
    var error by remember { mutableStateOf<String?>(null) }

    val previewLat = latText.toDoubleOrNull()
    val previewLng = lngText.toDoubleOrNull()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Scroll keeps the tags field and buttons reachable when the
                // map preview + keyboard push content past the sheet height.
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                stringResource(R.string.coordinate_format_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it; error = null },
                    label = { Text(stringResource(R.string.latitude)) },
                    placeholder = { Text(stringResource(R.string.latitude)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = lngText,
                    onValueChange = { lngText = it; error = null },
                    label = { Text(stringResource(R.string.longitude)) },
                    placeholder = { Text(stringResource(R.string.longitude)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.weight(1f)
                )
                FilledIconButton(
                    onClick = {
                        val raw = clipboard.getText()?.text?.trim().orEmpty()
                        val parsed = parseClipboardCoordinates(raw)
                        if (parsed == null) {
                            error = sheetContext.getString(R.string.clipboard_invalid_format, raw)
                        } else {
                            lngText = parsed.first.toBigDecimal().stripTrailingZeros().toPlainString()
                            latText = parsed.second.toBigDecimal().stripTrailingZeros().toPlainString()
                            error = null
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = com.gpsanywhere.app.ui.theme.CandyYellow.copy(alpha = 0.8f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = stringResource(R.string.action_paste), modifier = Modifier.size(18.dp))
                }
            }

            OutlinedTextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                label = { Text(stringResource(R.string.tags)) },
                placeholder = { Text(stringResource(R.string.tags_hint_2)) },
                supportingText = { Text(stringResource(R.string.tags_hint_1)) },
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.gpsanywhere.app.ui.theme.NeutralButtonBg,
                        contentColor = com.gpsanywhere.app.ui.theme.NeutralButtonContent
                    )
                ) { Text(stringResource(R.string.action_cancel)) }
                Button(
                    onClick = {
                        val n = name.trim()
                        val lat = latText.trim().toDoubleOrNull()
                        val lng = lngText.trim().toDoubleOrNull()
                        when {
                            n.isEmpty() -> error = sheetContext.getString(R.string.error_name_required)
                            lng == null || lng !in -180.0..180.0 -> error = sheetContext.getString(R.string.error_longitude_range)
                            lat == null || lat !in -90.0..90.0 -> error = sheetContext.getString(R.string.error_latitude_range)
                            else -> {
                                val normTags = tagsText.split("|", ",")
                                    .map { it.trim() }.filter { it.isNotEmpty() }
                                    .joinToString("|")
                                onSave(n, lat, lng, normTags)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.gpsanywhere.app.ui.theme.GlassGreen,
                        contentColor = Color.White
                    )
                ) { Text(stringResource(R.string.action_save)) }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Custom-coordinate controls and the direction pad, side by side in one card:
 * coordinate entry plus the transport buttons on the left, the D-pad on the right.
 * Pairing them keeps this block short so the saved-locations list stays visible.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomJumpPanel(
    coordinateText: String,
    onCoordinateChange: (String) -> Unit,
    onJump: () -> Unit,
    onSpiral: () -> Unit,
    onFly: (Float) -> Unit,
    onPaste: () -> Unit,
    dpadEnabled: Boolean,
    onMove: (dLat: Double, dLng: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val parsed = parseClipboardCoordinates(coordinateText.trim())
    val hasInput = coordinateText.isNotBlank()
    val isValid = parsed != null
    val canJump = hasInput && isValid
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = coordinateText,
                        onValueChange = onCoordinateChange,
                        label = { Text(stringResource(R.string.coordinate)) },
                        placeholder = { Text("22.3168,114.0451") },
                        isError = hasInput && !isValid,
                        // The narrower column can't fit a full coordinate pair on one
                        // line, so let the value wrap instead of scrolling out of view.
                        singleLine = false,
                        maxLines = 2,
                        textStyle = MaterialTheme.typography.bodySmall,
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
                        )
                    ) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = stringResource(R.string.action_paste),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Wraps to a second line when all five don't fit the narrow column.
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TransportButtons(
                        enabled = canJump,
                        onJump = onJump,
                        onSpiral = onSpiral,
                        onFly = onFly,
                        iconSize = 18.dp,
                        buttonModifier = Modifier.size(TRANSPORT_BUTTON_SIZE)
                    )
                }

                if (hasInput && !isValid) {
                    Text(
                        text = stringResource(R.string.coordinate_format_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            SectoredDpad(enabled = dpadEnabled, onMove = onMove, size = CARD_DPAD_SIZE)
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
    onMove: (dLat: Double, dLng: Double) -> Unit,
    size: androidx.compose.ui.unit.Dp = 196.dp
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val discColor = if (isDark) Color(0xFF1E2937).copy(alpha = 0.42f) else Color.White.copy(alpha = 0.40f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.5f)
    val arrowColor = if (enabled) {
        if (isDark) Color.White else Color(0xFF334155)
    } else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val scope = rememberCoroutineScope()

    // Keep the disc's proportions when the caller scales it down.
    val centerButton = size * (73f / 196f)
    val arrowOffset = size * (64f / 196f)   // distance of each chevron from the centre

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
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ResetIntervalInput() {
    val defaultMin = LocationViewModel.SPIRAL_RESET_INTERVAL_MS / 60_000L
    var text by remember { mutableStateOf(defaultMin.toString()) }
    var appliedMin by remember { mutableStateOf(defaultMin) }
    val labelColor = MaterialTheme.colorScheme.onSurface
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    val mins = text.toLongOrNull()
    val valid = mins != null && mins > 0
    val dirty = mins != appliedMin

    fun submit() {
        val m = text.toLongOrNull() ?: return
        if (m <= 0) return
        SpoofService.liveResetIntervalMs = m * 60_000L
        appliedMin = m
        keyboard?.hide()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.reset_every), style = MaterialTheme.typography.bodySmall, color = labelColor)
        OutlinedTextField(
            value = text,
            onValueChange = { input -> text = input.filter { it.isDigit() } },
            isError = text.isNotEmpty() && !valid,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = androidx.compose.ui.text.input.ImeAction.Done
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { submit() }),
            // Height is pinned well under the 56dp default: this row sets the
            // walk panel's height, and the field only ever holds a couple digits.
            modifier = Modifier.width(62.dp).height(50.dp),
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = true
        )
        Text(stringResource(R.string.minutes_short), style = MaterialTheme.typography.bodySmall, color = labelColor)
        FilledIconButton(
            onClick = { submit() },
            enabled = valid && dirty,
            modifier = Modifier.size(32.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = com.gpsanywhere.app.ui.theme.CandyYellow.copy(alpha = 0.85f),
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.action_apply), modifier = Modifier.size(18.dp))
        }
    }
}
