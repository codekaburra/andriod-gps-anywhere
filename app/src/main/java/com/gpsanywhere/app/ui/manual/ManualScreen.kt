package com.gpsanywhere.app.ui.manual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.location.CurrentLocationProvider
import com.gpsanywhere.app.service.SpoofService
import com.gpsanywhere.app.ui.components.CustomJumpPanel
import com.gpsanywhere.app.ui.components.MapViewComposable
import com.gpsanywhere.app.ui.components.speedToSlider
import com.gpsanywhere.app.ui.components.sliderToSpeed
import com.gpsanywhere.app.util.parseClipboardCoordinates
import com.gpsanywhere.app.viewmodel.LocationViewModel
import com.gpsanywhere.app.viewmodel.LocationViewModel.Companion.MOVE_STEP_DEG
import org.osmdroid.util.GeoPoint

@Composable
fun ManualScreen(viewModel: LocationViewModel) {
    val spiralSpeed by viewModel.spiralSpeedKmh.collectAsState()
    val spiralResetMinutes by viewModel.spiralResetMinutes.collectAsState()
    val isWalkMode by SpoofService.isWalkMode.observeAsState(false)
    val isSpoofing by viewModel.isSpoofing.observeAsState(false)
    val currentLat by CurrentLocationProvider.latitude.observeAsState()
    val currentLng by CurrentLocationProvider.longitude.observeAsState()
    val clipboardManager = LocalClipboardManager.current

    var jumpCoordinateText by remember { mutableStateOf("") }

    val center = remember(currentLat, currentLng) {
        val lat = currentLat
        val lng = currentLng
        if (lat != null && lng != null) GeoPoint(lat, lng) else null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Map ───────────────────────────────────────────────────────────────
        MapViewComposable(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            center = center
        )

        // ── Config panel ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Start / Stop
            if (isWalkMode) {
                OutlinedButton(
                    onClick = { viewModel.stopSpoofing() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Stop Walk Around")
                }
            } else {
                Button(
                    onClick = {
                        val lat = currentLat
                        val lng = currentLng
                        if (lat != null && lng != null) {
                            viewModel.startSpiralWalk(lat, lng)
                        }
                    },
                    enabled = currentLat != null && currentLng != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSpoofing) "Spiral Walk From Here" else "Start Spiral Walk Here")
                }
            }

            // ── Move target GPS (D-pad) ─────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Move target", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    DirectionPad(
                        enabled = currentLat != null && currentLng != null,
                        onMove = { dLat, dLng -> viewModel.nudgeSpiral(dLat, dLng) }
                    )
                }
            }

            // Speed panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Spiral Speed", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (spiralSpeed < 10f) "${"%.1f".format(spiralSpeed)} km/h"
                            else "${"%.0f".format(spiralSpeed)} km/h",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = speedToSlider(spiralSpeed),
                        onValueChange = { viewModel.setSpiralSpeed(sliderToSpeed(it)) },
                        valueRange = 0f..1f
                    )
                }
            }

            // Custom location panel
            CustomJumpPanel(
                coordinateText = jumpCoordinateText,
                onCoordinateChange = { jumpCoordinateText = it },
                onJump = {
                    parseClipboardCoordinates(jumpCoordinateText.trim())?.let {
                        viewModel.startSpoofing(it.second, it.first)
                    }
                },
                onSpiral = {
                    parseClipboardCoordinates(jumpCoordinateText.trim())?.let {
                        viewModel.startSpiralWalk(it.second, it.first)
                    }
                },
                onFly = { speed ->
                    parseClipboardCoordinates(jumpCoordinateText.trim())?.let {
                        viewModel.flyTo(it.second, it.first, speed)
                    }
                },
                onPaste = {
                    val raw = clipboardManager.getText()?.text?.trim().orEmpty()
                    val parsed = parseClipboardCoordinates(raw)
                    jumpCoordinateText = if (parsed != null) {
                        "%.6f,%.6f".format(parsed.second, parsed.first)
                    } else {
                        raw
                    }
                }
            )

            // Reset interval config
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                var resetText by remember(spiralResetMinutes) {
                    mutableStateOf(spiralResetMinutes.toString())
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Go back to starting position every",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = resetText,
                        onValueChange = { input ->
                            resetText = input.filter { it.isDigit() }.take(3)
                            resetText.toIntOrNull()?.let { viewModel.setSpiralResetMinutes(it) }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(72.dp)
                    )
                    Text("mins", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Up/down/left/right pad. Each tap nudges the target by one [MOVE_STEP_DEG] step. */
@Composable
private fun DirectionPad(
    enabled: Boolean,
    onMove: (dLat: Double, dLng: Double) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(onClick = { onMove(MOVE_STEP_DEG, 0.0) }, enabled = enabled) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "North")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(onClick = { onMove(0.0, -MOVE_STEP_DEG) }, enabled = enabled) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "West")
            }
            Spacer(Modifier.size(56.dp))
            FilledIconButton(onClick = { onMove(0.0, MOVE_STEP_DEG) }, enabled = enabled) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "East")
            }
        }
        FilledIconButton(onClick = { onMove(-MOVE_STEP_DEG, 0.0) }, enabled = enabled) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "South")
        }
    }
}
