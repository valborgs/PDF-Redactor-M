package org.comon.pdfredactorm.feature.editor.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.comon.pdfredactorm.feature.editor.R

@Composable
fun DeleteMaskDialog(
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_mask_title)) },
        text = { Text(stringResource(R.string.delete_mask_message)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDelete()
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.delete_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}
