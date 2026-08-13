package com.gpsanywhere.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import com.gpsanywhere.app.ui.theme.AppAccent

/**
 * Controls shared by the list screens and the add/edit forms.
 *
 * These existed as copies before, and had already drifted: the route list's edit
 * icon was tinted with the coordinate-text colour, the route editor's delete icon
 * missed a translucency pass, and the editor's text fields never picked up the
 * themed borders at all. Anything appearing on more than one screen belongs here
 * so a change lands everywhere at once.
 */

/** Alpha that keeps row icons present without competing with the row's text. */
private const val ROW_ICON_ALPHA = 0.75f

private val ROW_ICON_SIZE = 20.dp

/** The pencil on a saved-location or saved-route row. */
@Composable
fun EditIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            Icons.Default.Edit,
            contentDescription = contentDescription,
            tint = AppAccent.edit.copy(alpha = ROW_ICON_ALPHA),
            modifier = Modifier.size(ROW_ICON_SIZE)
        )
    }
}

/** The bin on a saved-location, saved-route or waypoint row. */
@Composable
fun DeleteIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            Icons.Default.Delete,
            contentDescription = contentDescription,
            tint = AppAccent.stop.copy(alpha = ROW_ICON_ALPHA),
            modifier = Modifier.size(ROW_ICON_SIZE)
        )
    }
}

/**
 * The paste button that sits beside a coordinate field.
 *
 * Only the appearance is shared — what each screen does with the clipboard text
 * differs, so the parsing stays with the caller.
 */
@Composable
fun PasteIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = AppAccent.action.copy(alpha = 0.65f),
            contentColor = AppAccent.onAction
        )
    ) {
        Icon(
            Icons.Default.ContentPaste,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Slider colours for every speed control.
 *
 * The screens map speed onto the track differently — the location screen reaches
 * 5000 km/h in two segments, the route screen 300 km/h in three — but they should
 * never *look* different, so the colours live here.
 */
@Composable
fun glassSliderColors(): SliderColors = SliderDefaults.colors(
    thumbColor = AppAccent.slider.copy(alpha = 0.72f),
    activeTrackColor = AppAccent.slider.copy(alpha = 0.72f),
    inactiveTrackColor = AppAccent.sliderTrack
)

/**
 * Text-field colours for every form in the app: transparent container so the glass
 * panel shows through, and borders/labels drawn from the theme rather than
 * Material's defaults.
 */
@Composable
fun glassFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.onSurface
)
