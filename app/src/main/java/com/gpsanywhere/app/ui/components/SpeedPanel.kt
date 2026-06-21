package com.gpsanywhere.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SpeedPanel(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    maxKmh: Float,
    labelColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Speed",
            style = MaterialTheme.typography.labelMedium,
            color = labelColor
        )
        Slider(
            value = speedToSlider(speed, maxKmh),
            onValueChange = { onSpeedChange(sliderToSpeed(it, maxKmh)) },
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f)
        )
        Text(
            if (speed < 10f) "${"%.1f".format(speed)} km/h"
            else "${"%.0f".format(speed)} km/h",
            style = MaterialTheme.typography.labelMedium,
            color = labelColor
        )
    }
}
