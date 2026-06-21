package com.gpsanywhere.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.R
import com.gpsanywhere.app.util.parseClipboardCoordinates
import com.gpsanywhere.app.viewmodel.LocationViewModel.Companion.FLY_FLIGHT_KMH
import com.gpsanywhere.app.viewmodel.LocationViewModel.Companion.FLY_HELI_KMH
import com.gpsanywhere.app.viewmodel.LocationViewModel.Companion.FLY_ROCKET_KMH

/**
 * Three Fly buttons that move the GPS toward the target at a preset cruising speed:
 * helicopter (200 km/h), flight (1000 km/h) and rocket (5000 km/h). Speed can still be
 * adjusted afterwards via the speed control panel.
 */
@Composable
fun FlySpeedButtons(
    onFly: (Float) -> Unit,
    enabled: Boolean = true
) {
    FilledIconButton(onClick = { onFly(FLY_HELI_KMH) }, enabled = enabled) {
        Icon(painterResource(R.drawable.ic_helicopter), contentDescription = "Helicopter", modifier = Modifier.size(20.dp))
    }
    Spacer(Modifier.width(4.dp))
    FilledIconButton(onClick = { onFly(FLY_FLIGHT_KMH) }, enabled = enabled) {
        Icon(Icons.Default.Flight, contentDescription = "Flight", modifier = Modifier.size(20.dp))
    }
    Spacer(Modifier.width(4.dp))
    FilledIconButton(onClick = { onFly(FLY_ROCKET_KMH) }, enabled = enabled) {
        Icon(Icons.Default.RocketLaunch, contentDescription = "Rocket", modifier = Modifier.size(20.dp))
    }
}

/** Coordinate input with paste plus jump / walk-around / fly actions for an arbitrary location. */
@Composable
fun CustomJumpPanel(
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
    val actionButtonColors = IconButtonDefaults.filledIconButtonColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        disabledContainerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilledIconButton(
                    onClick = onPaste,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
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
                FilledIconButton(onClick = onJump, enabled = canJump, colors = actionButtonColors) {
                    Icon(Icons.Default.DoorFront, contentDescription = "Jump", modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(4.dp))
                FilledIconButton(onClick = onSpiral, enabled = canJump, colors = actionButtonColors) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = "Walk Around", modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                FlySpeedButtons(onFly = onFly, enabled = canJump)
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
