package com.gpsanywhere.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass card — translucent frosted surface with a soft warm radial glow,
 * a warm-tinted border, large rounded corners and a pronounced soft shadow.
 *
 * Dark/light is derived from the app's resolved theme (via background luminance)
 * so it follows the in-app Light/Dark toggle rather than the OS setting.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val borderColor = if (isDark)
        Color.White.copy(alpha = 0.22f)
    else
        Color.White.copy(alpha = 0.5f)
    val containerColor = when {
        !filled -> Color.Transparent
        isDark -> Color(0xFF1E2937).copy(alpha = 0.45f)
        else -> Color.White.copy(alpha = 0.40f)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}
