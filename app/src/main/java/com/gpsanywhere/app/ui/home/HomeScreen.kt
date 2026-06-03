package com.gpsanywhere.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.settings.ThemeMode
import com.gpsanywhere.app.ui.components.CoordinateCard
import com.gpsanywhere.app.ui.components.PowerToggleButton
import com.gpsanywhere.app.ui.theme.GalaxyAccent
import com.gpsanywhere.app.ui.theme.GalaxyMuted
import com.gpsanywhere.app.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSpoofing by viewModel.isSpoofing.observeAsState(false)
    val currentLat by viewModel.currentLat.observeAsState(0.0)
    val currentLng by viewModel.currentLng.observeAsState(0.0)
    val themeMode by viewModel.themeMode.observeAsState(ThemeMode.SYSTEM)

    var showNoSessionDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GPS Anywhere",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { viewModel.cycleTheme() }) {
                Icon(
                    imageVector = when (themeMode) {
                        ThemeMode.LIGHT -> Icons.Default.Brightness7
                        ThemeMode.DARK -> Icons.Default.Brightness4
                        ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                    },
                    contentDescription = "Toggle theme",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        PowerToggleButton(
            isActive = isSpoofing,
            onClick = {
                if (isSpoofing) {
                    viewModel.toggleSpoofing()
                } else {
                    showNoSessionDialog = true
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isSpoofing) "Custom Location" else "Real Location",
            style = MaterialTheme.typography.headlineMedium,
            color = if (isSpoofing) GalaxyAccent else GalaxyMuted
        )

        if (isSpoofing && (currentLat != 0.0 || currentLng != 0.0)) {
            Spacer(modifier = Modifier.height(32.dp))
            CoordinateCard(
                latitude = currentLat,
                longitude = currentLng
            )
        }
    }

    if (showNoSessionDialog) {
        AlertDialog(
            onDismissRequest = { showNoSessionDialog = false },
            title = { Text("No Location Set") },
            text = {
                Text("Go to the Location or Route tab to choose a custom location before activating.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showNoSessionDialog = false
                    onNavigateToLocation()
                }) {
                    Text("Go to Location")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoSessionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
