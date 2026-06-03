package com.gpsanywhere.app.ui.steps

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import com.gpsanywhere.app.viewmodel.StepsViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Composable
fun StepsScreen(
    viewModel: StepsViewModel,
    modifier: Modifier = Modifier
) {
    val steps by viewModel.steps.observeAsState(0)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val healthConnectClient = remember { HealthConnectClient.getOrCreate(context) }

    var permissionsGranted by remember { mutableStateOf(false) }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class)
    )

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionActivityContract()
    ) { granted ->
        permissionsGranted = granted.containsAll(permissions)
    }

    // Check permissions on start
    LaunchedEffect(Unit) {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        permissionsGranted = granted.containsAll(permissions)
    }

    fun requestPermissions() {
        scope.launch {
            requestPermissionLauncher.launch(permissions)
        }
    }

    suspend fun writeStepsToHealthConnect(delta: Int) {
        if (!permissionsGranted) {
            requestPermissions()
            return
        }
        try {
            val now = Instant.now()
            val startTime = now.minus(5, ChronoUnit.MINUTES)
            val stepsRecord = StepsRecord(
                count = delta.toLong(),
                startTime = startTime,
                endTime = now,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime),
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(now)
            )
            healthConnectClient.insertRecords(listOf(stepsRecord))
        } catch (e: Exception) {
            // Ignore for now
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Walking Steps",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = steps.toString(),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "manual step count",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!permissionsGranted) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Grant Health Connect permission to make steps visible to games.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = { requestPermissions() }) {
                Text("Request Permissions")
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Increment",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (!permissionsGranted) requestPermissions()
                    viewModel.increment(100)
                    scope.launch { writeStepsToHealthConnect(100) }
                },
                modifier = Modifier.width(140.dp)
            ) {
                Text("+100")
            }
            Button(
                onClick = {
                    if (!permissionsGranted) requestPermissions()
                    viewModel.increment(1000)
                    scope.launch { writeStepsToHealthConnect(1000) }
                },
                modifier = Modifier.width(140.dp)
            ) {
                Text("+1000")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (!permissionsGranted) requestPermissions()
                    viewModel.increment(3000)
                    scope.launch { writeStepsToHealthConnect(3000) }
                },
                modifier = Modifier.width(140.dp)
            ) {
                Text("+3000")
            }
            Button(
                onClick = {
                    if (!permissionsGranted) requestPermissions()
                    viewModel.increment(5000)
                    scope.launch { writeStepsToHealthConnect(5000) }
                },
                modifier = Modifier.width(140.dp)
            ) {
                Text("+5000")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Use these buttons to simulate walking steps without moving the phone. Steps are written to Health Connect so compatible games can detect them.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
