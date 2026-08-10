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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.gpsanywhere.app.R
import com.gpsanywhere.app.location.CurrentLocationProvider
import com.gpsanywhere.app.settings.AppPreferences
import com.gpsanywhere.app.ui.navigation.MainApp

class MainActivity : ComponentActivity() {

    private val preferences by lazy { AppPreferences(this) }

    // Hoisted out of composition so the permission-result callback can flip it.
    private var showPermissionDialog by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            CurrentLocationProvider.ensureStarted(this)
        } else {
            // Without location permission the app can't spoof; guide the user instead
            // of leaving them with a silently non-functional Start button.
            showPermissionDialog = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        preferences.applySavedTheme()
        super.onCreate(savedInstanceState)

        requestNeededPermissions()
        CurrentLocationProvider.ensureStarted(this)

        setContent {
            MainApp(preferences = preferences)

            if (showPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog = false },
                    title = { Text(stringResource(R.string.perm_required_title)) },
                    text = { Text(stringResource(R.string.perm_required_body)) },
                    confirmButton = {
                        TextButton(onClick = { showPermissionDialog = false }) {
                            Text(stringResource(R.string.action_got_it))
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
