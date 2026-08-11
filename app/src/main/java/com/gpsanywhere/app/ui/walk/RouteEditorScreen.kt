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
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.R
import com.gpsanywhere.app.data.DefaultSavedRouteSeeder
import com.gpsanywhere.app.data.SavedRoute
import com.gpsanywhere.app.routes.LocationPoint
import com.gpsanywhere.app.util.parseClipboardCoordinates
import com.gpsanywhere.app.ui.components.GlassCard
import com.gpsanywhere.app.ui.components.MapViewComposable
import org.osmdroid.util.GeoPoint
import com.gpsanywhere.app.ui.theme.AppAccent

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

    // CSV edit mode: shows the route as editable CSV text instead of map + list.
    var csvMode by remember { mutableStateOf(false) }
    var csvText by remember { mutableStateOf("") }
    var csvError by remember { mutableStateOf<String?>(null) }
    val csvParseError = stringResource(R.string.csv_parse_error)

    fun applyCsv() {
        val parsed = DefaultSavedRouteSeeder.parseCsv(csvText)
        if (parsed == null) {
            csvError = csvParseError
            return
        }
        points.clear()
        points.addAll(parsed.toLocationPoints())
        if (parsed.routeName.isNotBlank()) name = parsed.routeName
        csvError = null
        csvMode = false
    }

    val canSave = !csvMode && name.isNotBlank() && points.size >= 2

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
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                }
                Text(
                    stringResource(
                        if (initial == null || initial.isPreinstalled) R.string.new_route_title
                        else R.string.edit_route_title
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = {
                        if (csvMode) {
                            csvMode = false
                            csvError = null
                        } else {
                            csvText = buildRouteCsv(name, points)
                            csvError = null
                            csvMode = true
                        }
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                ) { Text(stringResource(if (csvMode) R.string.editor_map else R.string.editor_csv)) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { if (canSave) onSave(name.trim(), points.toList()) },
                    enabled = canSave,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.gpsanywhere.app.ui.theme.GlassGreen,
                        contentColor = Color.White
                    )
                ) { Text(stringResource(R.string.action_save)) }
            }
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.route_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        }

        // ── CSV edit mode ─────────────────────────────────────────────────────
        if (csvMode) {
            item {
                Text(
                    stringResource(R.string.csv_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            item {
                OutlinedTextField(
                    value = csvText,
                    onValueChange = { csvText = it; csvError = null },
                    label = { Text(stringResource(R.string.route_csv)) },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    isError = csvError != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 280.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }
            csvError?.let { err ->
                item {
                    Text(
                        err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            item {
                val clipboard = LocalClipboardManager.current
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { clipboard.getText()?.text?.let { csvText = it; csvError = null } },
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_paste))
                        }
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(androidx.compose.ui.text.AnnotatedString(csvText))
                            },
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_copy))
                        }
                    }
                    Button(
                        onClick = { applyCsv() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.gpsanywhere.app.ui.theme.GlassGreen,
                            contentColor = Color.White
                        )
                    ) { Text(stringResource(R.string.action_apply)) }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
            return@LazyColumn
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
                stringResource(R.string.tap_map_hint, points.size),
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
                    label = { Text(stringResource(R.string.coordinate)) },
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
                        containerColor = AppAccent.action.copy(alpha = 0.8f),
                        contentColor = AppAccent.onAction
                    )
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = stringResource(R.string.action_paste), modifier = Modifier.size(18.dp))
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
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_point), modifier = Modifier.size(18.dp))
                }
            }
        }

        if (points.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.click_map_hint),
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
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up), modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = { if (index < points.size - 1) points.add(index + 1, points.removeAt(index)) },
                            enabled = index < points.size - 1
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down), modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { points.removeAt(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = AppAccent.stop, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

/** Quote a CSV field when it contains a comma or quote. */
private fun csvEscape(field: String): String =
    if (field.contains(',') || field.contains('"')) "\"${field.replace("\"", "\"\"")}\"" else field

/**
 * Render the current editor state as CSV. When the route has no points yet, a couple
 * of example rows are included as a starting skeleton for the user to overwrite.
 */
private fun buildRouteCsv(name: String, points: List<LocationPoint>): String = buildString {
    appendLine("# route_name: ${name.trim().ifBlank { "My Route" }}")
    appendLine("latitude,longitude,name_tc,name_en")
    if (points.isEmpty()) {
        appendLine("22.294270,114.169930,尖沙咀鐘樓,Clock Tower")
        appendLine("22.293720,114.173190,星光大道,Avenue of Stars")
    } else {
        points.forEach { p ->
            appendLine("${"%.6f".format(p.latitude)},${"%.6f".format(p.longitude)},${csvEscape(p.name.orEmpty())},")
        }
    }
}
