package com.gpsanywhere.app.ui.location

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.R
import com.gpsanywhere.app.ads.rememberInterstitialAd
import com.gpsanywhere.app.data.LocationCsvImport
import com.gpsanywhere.app.ui.components.BUTTON_FILL_ALPHA
import com.gpsanywhere.app.ui.theme.AppAccent
import com.gpsanywhere.app.viewmodel.MainViewModel

/**
 * Bulk-adds locations from CSV the user pastes in.
 *
 * The paste is parsed on every keystroke rather than behind a "preview" button,
 * so the list below the field always describes the text above it. Parsing is a
 * string split over a few dozen lines; there is nothing to defer.
 *
 * Every accepted row is listed in full — both names, coordinates and tags — so
 * the confirmation before importing is of the actual parsed values rather than
 * of a count. A count only tells you how many rows survived, not whether the
 * columns landed where you meant them to; a name in the latitude column is the
 * kind of mistake that parses cleanly and reads wrong.
 *
 * Rejected lines are listed with their line numbers rather than dropped quietly.
 *
 * A LazyColumn rather than a scrolling Column: the preview is unbounded, and a
 * few hundred pasted rows should not all be composed to show the first screen.
 */
@Composable
fun ImportLocationsScreen(
    viewModel: MainViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isImporting by viewModel.isImporting.observeAsState(false)
    val showInterstitial = rememberInterstitialAd()

    var csvText by remember { mutableStateOf("") }

    val parsed = remember(csvText) { LocationCsvImport.parse(csvText) }
    val canImport = parsed.rows.isNotEmpty() && !isImporting

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDone) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                }
                Text(
                    stringResource(R.string.import_csv_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        item {
            ImportCard {
                Text(
                    stringResource(R.string.import_csv_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = BUTTON_FILL_ALPHA)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.import_csv_columns, LocationCsvImport.COLUMNS),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Filling is disabled once there is text, so a mis-tap cannot
                    // replace a long paste the user has no way to undo. Clear is
                    // the deliberate way back to an empty field, and re-enables it.
                    OutlinedButton(
                        onClick = { csvText = LocationCsvImport.EXAMPLE },
                        enabled = csvText.isEmpty()
                    ) {
                        Text(stringResource(R.string.import_csv_fill_example))
                    }
                    OutlinedButton(
                        onClick = { csvText = "" },
                        enabled = csvText.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.action_clear))
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = csvText,
                    onValueChange = { csvText = it },
                    label = { Text(stringResource(R.string.import_csv_label)) },
                    // Monospaced: columns line up, which is how a misplaced comma
                    // becomes visible before the import runs.
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                )
            }
        }

        item {
            ImportCard {
                Text(
                    if (parsed.rows.isEmpty()) stringResource(R.string.import_csv_nothing)
                    else stringResource(R.string.import_csv_ready, parsed.rows.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (parsed.problems.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.import_csv_problems, parsed.problems.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppAccent.stopContainer.container
                    )
                    Spacer(Modifier.height(4.dp))
                    parsed.problems.forEach { problem ->
                        Text(
                            stringResource(problem.reason.messageRes(), problem.lineNumber),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = BUTTON_FILL_ALPHA)
                        )
                    }
                }
            }
        }

        items(parsed.rows, key = { it.lineNumber }) { row ->
            PreviewRow(row)
        }

        item {
            Button(
                onClick = {
                    viewModel.importLocationsFromCsv(parsed.rows) { outcome ->
                        val message = if (outcome.skippedDuplicates > 0) {
                            context.getString(
                                R.string.import_csv_done_with_duplicates,
                                outcome.inserted,
                                outcome.skippedDuplicates
                            )
                        } else {
                            context.getString(R.string.import_csv_done, outcome.inserted)
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        showInterstitial()
                        onDone()
                    }
                },
                enabled = canImport,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppAccent.primaryAction.container.copy(alpha = BUTTON_FILL_ALPHA),
                    contentColor = AppAccent.primaryAction.content
                )
            ) {
                Text(stringResource(if (isImporting) R.string.working else R.string.action_import))
            }
        }
    }
}

/** One parsed row, shown as it will be saved. */
@Composable
private fun PreviewRow(row: LocationCsvImport.Row) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = AppAccent.primaryAction.container,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                // Both names, not the one for the current language: the point of
                // the preview is to check every column landed where it should.
                Text(
                    listOf(row.name, row.nameEn).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "%.6f, %.6f".format(row.latitude, row.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = BUTTON_FILL_ALPHA)
                )
                val tags = listOf(row.tags, row.tagsEn).filter { it.isNotBlank() }
                if (tags.isNotEmpty()) {
                    Text(
                        tags.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = BUTTON_FILL_ALPHA)
                    )
                }
            }
        }
    }
}

/** The shared card chrome used by the input and summary blocks. */
@Composable
private fun ImportCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

/** The localised explanation for each rejection reason. */
private fun LocationCsvImport.Reason.messageRes(): Int = when (this) {
    LocationCsvImport.Reason.TOO_FEW_COLUMNS -> R.string.import_csv_problem_columns
    LocationCsvImport.Reason.BAD_LATITUDE -> R.string.import_csv_problem_latitude
    LocationCsvImport.Reason.BAD_LONGITUDE -> R.string.import_csv_problem_longitude
    LocationCsvImport.Reason.NO_NAME -> R.string.import_csv_problem_name
}
