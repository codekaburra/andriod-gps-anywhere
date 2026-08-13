package com.gpsanywhere.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.routes.LocationPoint
import com.gpsanywhere.app.ui.theme.AppAccent
import org.osmdroid.util.GeoPoint

/**
 * The map preview with a floating add button in its top-right corner, used as the
 * header of both the location and route screens.
 *
 * [clipToBounds] is not optional: the osmdroid MapView is an AndroidView and will
 * paint past its slot, covering whatever sits below it.
 */
@Composable
fun MapWithAddButton(
    center: GeoPoint,
    waypoints: List<LocationPoint>,
    onAdd: () -> Unit,
    addContentDescription: String,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
    zoom: Double = 15.0
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
    ) {
        MapViewComposable(
            modifier = Modifier.fillMaxSize(),
            center = center,
            zoom = zoom,
            waypoints = waypoints
        )

        Surface(
            shape = CircleShape,
            color = AppAccent.actionSurface,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            IconButton(onClick = onAdd) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = addContentDescription,
                    tint = AppAccent.onAction
                )
            }
        }
    }
}
