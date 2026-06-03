package com.gpsanywhere.app.ui.steps

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.viewmodel.StepsViewModel

@Composable
fun StepsScreen(
    viewModel: StepsViewModel,
    modifier: Modifier = Modifier
) {
    val steps by viewModel.steps.observeAsState(0)

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
                onClick = { viewModel.increment(100) },
                modifier = Modifier.width(140.dp)
            ) {
                Text("+100")
            }
            Button(
                onClick = { viewModel.increment(1000) },
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
                onClick = { viewModel.increment(3000) },
                modifier = Modifier.width(140.dp)
            ) {
                Text("+3000")
            }
            Button(
                onClick = { viewModel.increment(5000) },
                modifier = Modifier.width(140.dp)
            ) {
                Text("+5000")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(onClick = { viewModel.reset() }) {
            Text("Reset to 0")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Use these buttons to simulate walking steps without moving the phone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
