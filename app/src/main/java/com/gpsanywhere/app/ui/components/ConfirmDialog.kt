package com.gpsanywhere.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gpsanywhere.app.R

/**
 * The confirm/cancel dialog used for deletes and imports.
 *
 * Seven copies of this shape existed across the location, route and settings
 * screens, differing only in their strings — which is exactly the arrangement
 * that lets one of them quietly fall behind the others.
 *
 * [confirmLabel] defaults to Delete because most callers are destructive;
 * the import prompts pass their own.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = stringResource(R.string.action_delete)
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
