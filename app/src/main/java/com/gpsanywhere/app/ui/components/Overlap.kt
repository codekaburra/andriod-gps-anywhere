package com.gpsanywhere.app.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

/**
 * Draw this composable [overlap] higher than its slot and shrink the space it
 * occupies by the same amount, so everything below it moves up too.
 *
 * Used to let a translucent [GlassCard] ride up over the bottom of the map above
 * it: the map stays readable through the card, and the vertical space the overlap
 * saves goes to the list underneath. Pair it with `Modifier.zIndex(1f)` on the
 * card and `Modifier.clipToBounds()` on the map — the osmdroid MapView is an
 * AndroidView and will otherwise paint over the card.
 */
fun Modifier.overlapAbove(overlap: Dp) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val dy = overlap.roundToPx()
    layout(placeable.width, (placeable.height - dy).coerceAtLeast(0)) {
        placeable.place(0, -dy)
    }
}
