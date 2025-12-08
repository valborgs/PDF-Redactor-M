package org.comon.pdfredactorm.feature.editor.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.comon.pdfredactorm.feature.editor.R

@Composable
fun PiiDetectionResultDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notice_title)) },
        text = { Text(stringResource(R.string.pii_detection_result_message, count)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(stringResource(R.string.confirm_button))
            }
        }
    )
}
