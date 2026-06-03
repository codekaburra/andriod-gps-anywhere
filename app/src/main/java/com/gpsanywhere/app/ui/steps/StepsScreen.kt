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

    val healthConnectAvailability = remember {
        HealthConnectClient.getSdkStatus(context)
    }
    val isHealthConnectAvailable = healthConnectAvailability == HealthConnectClient.SDK_AVAILABLE

    var permissionsGranted by remember { mutableStateOf(false) }
    var pendingDelta by remember { mutableStateOf(0) }
    var permissionRequestMessage by remember { mutableStateOf<String?>(null) }
    var lastActionMessage by remember { mutableStateOf<String?>(null) }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class)
    )

    // Defined before use in requestPermissionLauncher callback
    suspend fun writeStepsToHealthConnect(delta: Int) {
        val client = if (isHealthConnectAvailable) HealthConnectClient.getOrCreate(context) else null
        if (client == null) return
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
            client.insertRecords(listOf(stepsRecord))
            lastActionMessage = "Wrote +$delta steps to Health Connect (games should see them now)"
            permissionRequestMessage = null
        } catch (e: Exception) {
            lastActionMessage = "Failed to write steps to Health Connect: ${e.message}. Make sure permission is granted in Health Connect app."
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions: Set<String> ->
        permissionRequestMessage = null
        val nowGranted = grantedPermissions.containsAll(permissions)
        permissionsGranted = nowGranted
        if (nowGranted && pendingDelta > 0) {
            scope.launch {
                writeStepsToHealthConnect(pendingDelta)
                pendingDelta = 0
            }
        } else if (!nowGranted) {
            permissionRequestMessage = "Permission request was not granted. You can try again or grant manually in Health Connect settings."
        }
    }

    // Defined before use in LaunchedEffect
    fun requestPermissions() {
        permissionRequestMessage = "Opening Health Connect permissions…"
        // 1. Try the standard ActivityResultLauncher (works on most devices)
        try {
            requestPermissionLauncher.launch(permissions)
            return
        } catch (_: Exception) {}

        // 2. Fallback: open Health Connect settings directly so the user can grant manually
        try {
            context.startActivity(
                android.content.Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            permissionRequestMessage = "Grant Health Connect permissions in the screen that opened, then return here."
        } catch (_: Exception) {
            permissionRequestMessage = "Could not open Health Connect. Please install or update the Health Connect app."
        }
    }

    // Check permissions on start (use fresh client)
    LaunchedEffect(isHealthConnectAvailable) {
        val client = if (isHealthConnectAvailable) HealthConnectClient.getOrCreate(context) else null
        if (client != null) {
            try {
                val granted = client.permissionController.getGrantedPermissions()
                permissionsGranted = granted.containsAll(permissions)
            } catch (e: Exception) {
                permissionsGranted = false
            }
        } else {
            permissionsGranted = false
        }
    }

    fun onIncrement(delta: Int) {
        viewModel.increment(delta)
        if (!isHealthConnectAvailable) {
            return
        }
        if (permissionsGranted) {
            scope.launch {
                writeStepsToHealthConnect(delta)
            }
        } else {
            pendingDelta += delta
            requestPermissions()
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

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "🚧 UNDER DEVELOPMENT - COMING SOON 🚧",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
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

        if (lastActionMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = lastActionMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (permissionRequestMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = permissionRequestMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (isHealthConnectAvailable) {
            Spacer(modifier = Modifier.height(16.dp))
            if (!permissionsGranted) {
                Text(
                    text = "Grant Health Connect permission to make steps visible to games.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(onClick = { requestPermissions() }) {
                Text(if (permissionsGranted) "Health Connect: Granted ✓" else "Grant Health Connect Permission")
            }
            if (!permissionsGranted) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    try {
                        context.startActivity(
                            android.content.Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        permissionRequestMessage = "In Health Connect: go to 'App permissions' > this app > allow 'Steps' (write)."
                    } catch (e: Exception) {
                        permissionRequestMessage = "Could not open Health Connect settings: ${e.message}. Install the Health Connect app."
                    }
                }) {
                    Text("Open Health Connect Settings (manual grant)")
                }
            }
        } else if (!isHealthConnectAvailable) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Health Connect is not available on this device. Steps will only be simulated within the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
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
                onClick = { onIncrement(100) },
                modifier = Modifier.width(140.dp)
            ) {
                Text("+100")
            }
            Button(
                onClick = { onIncrement(1000) },
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
                onClick = { onIncrement(3000) },
                modifier = Modifier.width(140.dp)
            ) {
                Text("+3000")
            }
            Button(
                onClick = { onIncrement(5000) },
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
