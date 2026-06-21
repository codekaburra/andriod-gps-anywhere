package com.gpsanywhere.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** A numeric field flanked by − / + buttons for fine, one-step adjustments. */
@Composable
fun StepperInput(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    unit: String? = null,
    fieldWidth: Int = 96
) {
    var text by remember(value) { mutableStateOf(value.toString()) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledIconButton(onClick = { onValueChange(value - 1) }) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(20.dp))
        }
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                text = input.filter { it.isDigit() }.take(3)
                text.toIntOrNull()?.let { onValueChange(it) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(fieldWidth.dp)
        )
        FilledIconButton(onClick = { onValueChange(value + 1) }) {
            Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(20.dp))
        }
        if (unit != null) {
            Text(unit, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
