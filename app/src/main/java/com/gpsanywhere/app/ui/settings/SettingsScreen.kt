package com.gpsanywhere.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.gpsanywhere.app.R
import com.gpsanywhere.app.settings.AppLanguage
import com.gpsanywhere.app.settings.ThemeMode
import com.gpsanywhere.app.viewmodel.MainViewModel
import com.gpsanywhere.app.ui.theme.AppAccent

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val themeMode by viewModel.themeMode.observeAsState(ThemeMode.LIGHT)
    val appLanguage by viewModel.appLanguage.observeAsState(AppLanguage.SYSTEM)
    val context = LocalContext.current
    val packageInfo = remember {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val isImporting by viewModel.isImporting.observeAsState(false)
    var showImportLocationsConfirm by remember { mutableStateOf(false) }
    var showImportRoutesConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDeleteCustomConfirm by remember { mutableStateOf(false) }
    val versionName = packageInfo.versionName ?: "unknown"
    val lastUpdated = remember {
        val timestamp = packageInfo.lastUpdateTime
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Language section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppLanguage.entries.forEach { language ->
                        FilterChip(
                            selected = appLanguage == language,
                            onClick = { viewModel.setLanguage(language) },
                            label = {
                                Text(
                                    when (language) {
                                        AppLanguage.SYSTEM -> stringResource(R.string.language_system)
                                        AppLanguage.ENGLISH -> stringResource(R.string.language_english)
                                        AppLanguage.TRADITIONAL_CHINESE -> stringResource(R.string.language_traditional_chinese)
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }

        // Theme section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.settings_theme),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(ThemeMode.LIGHT, ThemeMode.DARK).forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                        ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                        else -> ""
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }

        // Prebuilt data import section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.settings_prebuilt),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.settings_prebuilt_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showImportLocationsConfirm = true },
                    enabled = !isImporting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFA1B5AB),
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(if (isImporting) R.string.working else R.string.import_prebuilt_locations))
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showImportRoutesConfirm = true },
                    enabled = !isImporting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFA1B5AB),
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(if (isImporting) R.string.working else R.string.import_prebuilt_routes))
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showDeleteConfirm = true },
                    enabled = !isImporting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppAccent.stop.copy(alpha = 0.9f),
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.delete_prebuilt_locations))
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showDeleteCustomConfirm = true },
                    enabled = !isImporting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppAccent.stop.copy(alpha = 0.9f),
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.delete_custom_data))
                }
            }
        }

        // Developer options section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.settings_dev_options),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.settings_dev_options_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(12.dp))
                val devOptionsHint = stringResource(R.string.dev_options_manual_hint)
                Button(
                    onClick = {
                        try {
                            context.startActivity(
                                android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (e: Exception) {
                            try {
                                context.startActivity(
                                    android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (_: Exception) {
                                Toast.makeText(context, devOptionsHint, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFA1B5AB),
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.open_dev_options))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.dev_options_step1),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.dev_options_step2),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        // Copyright section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.settings_version, versionName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.settings_last_updated, lastUpdated),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_copyright),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.settings_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showImportLocationsConfirm) {
        AlertDialog(
            onDismissRequest = { showImportLocationsConfirm = false },
            title = { Text(stringResource(R.string.dialog_import_locations_title)) },
            text = { Text(stringResource(R.string.dialog_import_locations_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showImportLocationsConfirm = false
                    viewModel.importPrebuiltLocations {
                        Toast.makeText(context, context.getString(R.string.toast_locations_imported), Toast.LENGTH_SHORT).show()
                    }
                }) { Text(stringResource(R.string.action_import)) }
            },
            dismissButton = {
                TextButton(onClick = { showImportLocationsConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showImportRoutesConfirm) {
        AlertDialog(
            onDismissRequest = { showImportRoutesConfirm = false },
            title = { Text(stringResource(R.string.dialog_import_routes_title)) },
            text = { Text(stringResource(R.string.dialog_import_routes_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showImportRoutesConfirm = false
                    viewModel.importPrebuiltRoutes {
                        Toast.makeText(context, context.getString(R.string.toast_routes_imported), Toast.LENGTH_SHORT).show()
                    }
                }) { Text(stringResource(R.string.action_import)) }
            },
            dismissButton = {
                TextButton(onClick = { showImportRoutesConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showDeleteCustomConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteCustomConfirm = false },
            title = { Text(stringResource(R.string.dialog_delete_custom_title)) },
            text = { Text(stringResource(R.string.dialog_delete_custom_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteCustomConfirm = false
                    viewModel.deleteCustom {
                        Toast.makeText(context, context.getString(R.string.toast_custom_deleted), Toast.LENGTH_SHORT).show()
                    }
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCustomConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.dialog_delete_prebuilt_title)) },
            text = { Text(stringResource(R.string.dialog_delete_prebuilt_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deletePrebuiltLocations {
                        Toast.makeText(context, context.getString(R.string.toast_prebuilt_deleted), Toast.LENGTH_SHORT).show()
                    }
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

}
