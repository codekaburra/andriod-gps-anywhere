package com.gpsanywhere.app.ui.step

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.health.connect.client.PermissionController
import com.gpsanywhere.app.R
import com.gpsanywhere.app.health.HealthConnectSteps
import com.gpsanywhere.app.ui.components.GlassCard
import com.gpsanywhere.app.ui.theme.CandyGreen
import com.gpsanywhere.app.viewmodel.StepViewModel

// Health Connect permissions only take effect after the user approves them in
// the official permission screen, so this screen walks through
// install → grant → input instead of assuming the manifest declaration is enough.

@Composable
fun StepScreen(
    viewModel: StepViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val status by viewModel.healthStatus.collectAsState()
    val todaySteps by viewModel.todayHealthSteps.collectAsState()
    val writeResult by viewModel.stepsWriteResult.collectAsState()
    var input by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { viewModel.refreshHealthStatus() }

    LaunchedEffect(Unit) { viewModel.refreshHealthStatus() }
    DisposableEffect(Unit) { onDispose { viewModel.clearWriteResult() } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.step_title), style = MaterialTheme.typography.headlineMedium)

        GlassCard {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.steps_card_title),
                    style = MaterialTheme.typography.titleSmall
                )
                when (status) {
                    HealthConnectSteps.Status.NOT_INSTALLED -> {
                        Text(
                            stringResource(R.string.steps_hc_not_installed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        OutlinedButton(onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "market://details?id=com.google.android.apps.healthdata".toUri()
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }) { Text(stringResource(R.string.steps_hc_install)) }
                    }
                    HealthConnectSteps.Status.UPDATE_REQUIRED -> {
                        Text(
                            stringResource(R.string.steps_hc_update_required),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    HealthConnectSteps.Status.NO_PERMISSION -> {
                        Text(
                            stringResource(R.string.steps_hc_grant_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Button(onClick = { permissionLauncher.launch(HealthConnectSteps.PERMISSIONS) }) {
                            Text(stringResource(R.string.steps_hc_grant))
                        }
                    }
                    HealthConnectSteps.Status.READY -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = input,
                                onValueChange = {
                                    input = it.filter(Char::isDigit).take(6)
                                    viewModel.clearWriteResult()
                                },
                                label = { Text(stringResource(R.string.steps_input_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                enabled = (input.toLongOrNull() ?: 0L) > 0L,
                                onClick = {
                                    input.toLongOrNull()?.let { viewModel.addManualSteps(it) }
                                    input = ""
                                }
                            ) { Text(stringResource(R.string.steps_add)) }
                        }
                        todaySteps?.let {
                            Text(
                                pluralStringResource(R.plurals.steps_today_total, it.toInt(), it),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        writeResult?.let { result ->
                            Text(
                                if (result.success) {
                                    pluralStringResource(
                                        R.plurals.steps_write_success,
                                        result.steps.toInt(),
                                        result.steps
                                    )
                                } else {
                                    // Show a localized message rather than the raw
                                    // exception text; the cause is already logged.
                                    stringResource(R.string.steps_write_failed)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (result.success) CandyGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    null -> {}
                }
            }
        }
    }
}
