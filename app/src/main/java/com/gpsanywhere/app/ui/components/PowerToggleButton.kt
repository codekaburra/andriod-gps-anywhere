package com.gpsanywhere.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
@Composable
fun PowerToggleButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive) 0.35f else 0.1f,
        label = "glow"
    )
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1f,
        label = "scale"
    )

    // Soft active glow for custom location mode.
    Box(
        modifier = modifier
            .size(140.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = glowAlpha)
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = if (isActive) "Custom Location active" else "Real Location active" },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White
            )
        }
    }
}
