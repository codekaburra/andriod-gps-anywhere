package com.gpsanywhere.app.ui.walk

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.data.SavedRoute
import com.gpsanywhere.app.routes.LocationPoint
import com.gpsanywhere.app.util.parseClipboardCoordinates
import com.gpsanywhere.app.ui.components.GlassCard
import com.gpsanywhere.app.ui.components.MapViewComposable
import org.osmdroid.util.GeoPoint

/**
 * Full-screen editor to create a new route or edit an existing one.
 * Tap the map to append waypoints; reorder or delete them in the list below.
 */
@Composable
fun RouteEditorScreen(
    initial: SavedRoute?,
    initialPoints: List<LocationPoint>,
    mapCenter: GeoPoint?,
    onCancel: () -> Unit,
    onSave: (name: String, points: List<LocationPoint>) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var coordText by remember { mutableStateOf("") }
    val points = remember { mutableStateListOf<LocationPoint>().apply { addAll(initialPoints) } }
    val center = mapCenter ?: points.firstOrNull()?.let { GeoPoint(it.latitude, it.longitude) }

    val canSave = name.isNotBlank() && points.size >= 2

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
                Text(
                    if (initial == null || initial.isPreinstalled) "New Route" else "Edit Route",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { if (canSave) onSave(name.trim(), points.toList()) },
                    enabled = canSave,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.gpsanywhere.app.ui.theme.GlassGreen,
                        contentColor = Color.White
                    )
                ) { Text("Save") }
            }
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Route name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        }

        item {
            Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                MapViewComposable(
                    modifier = Modifier.fillMaxSize(),
                    center = center,
                    zoom = 14.0,
                    waypoints = points.toList(),
                    showNumberedPins = true,
                    onMapClick = { p -> points.add(LocationPoint(p.latitude, p.longitude)) }
                )
            }
        }

        item {
            Text(
                "Tap the map to add a waypoint · ${points.size} stop(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        // ── Paste / type a coordinate to add a waypoint ──────────────────────
        item {
            val clipboard = LocalClipboardManager.current
            val parsed = parseClipboardCoordinates(coordText.trim())
            val valid = parsed != null
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = coordText,
                    onValueChange = { coordText = it },
                    label = { Text("Coordinate") },
                    placeholder = { Text("22.3168,114.0451") },
                    singleLine = true,
                    isError = coordText.isNotBlank() && !valid,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                FilledIconButton(
                    onClick = {
                        clipboard.getText()?.text?.let { coordText = it }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = com.gpsanywhere.app.ui.theme.CandyYellow.copy(alpha = 0.8f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(18.dp))
                }
                FilledIconButton(
                    onClick = {
                        parsed?.let {
                            points.add(LocationPoint(it.first, it.second))
                            coordText = ""
                        }
                    },
                    enabled = valid,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = com.gpsanywhere.app.ui.theme.GlassGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add point", modifier = Modifier.size(18.dp))
                }
            }
        }

        if (points.isEmpty()) {
            item {
                Text(
                    "Click the map to add points",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            itemsIndexed(points) { index, point ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(28.dp)
                        )
                        Text(
                            "${"%.5f".format(point.latitude)}, ${"%.5f".format(point.longitude)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { if (index > 0) points.add(index - 1, points.removeAt(index)) },
                            enabled = index > 0
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up", modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = { if (index < points.size - 1) points.add(index + 1, points.removeAt(index)) },
                            enabled = index < points.size - 1
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { points.removeAt(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = com.gpsanywhere.app.ui.theme.StopRed, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
