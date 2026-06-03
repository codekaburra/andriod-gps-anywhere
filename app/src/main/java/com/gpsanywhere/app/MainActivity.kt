package com.gpsanywhere.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.gpsanywhere.app.settings.AppPreferences
import com.gpsanywhere.app.ui.navigation.MainApp

class MainActivity : ComponentActivity() {

    private val preferences by lazy { AppPreferences(this) }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineDenied = results[Manifest.permission.ACCESS_FINE_LOCATION] == false
        if (fineDenied) {
            // Shown via compose state on next frame if needed
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        preferences.applySavedTheme()
        super.onCreate(savedInstanceState)

        requestNeededPermissions()

        setContent {
            val showPermissionDialog = remember { mutableStateOf(false) }

            MainApp(preferences = preferences)

            if (showPermissionDialog.value) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog.value = false },
                    title = { Text("Permissions Required") },
                    text = {
                        Text("Location permission is required for custom location mode. Please grant it in app settings.")
                    },
                    confirmButton = {
                        TextButton(onClick = { showPermissionDialog.value = false }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }
}
