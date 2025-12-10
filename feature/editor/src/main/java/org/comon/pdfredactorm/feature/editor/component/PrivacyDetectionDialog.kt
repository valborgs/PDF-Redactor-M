package org.comon.pdfredactorm.feature.editor.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.comon.pdfredactorm.feature.editor.R

@Composable
fun PrivacyDetectionDialog(
    onDismiss: () -> Unit,
    onDetectCurrentPage: () -> Unit,
    onDetectAllPages: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.privacy_detection_title)) },
        text = { Text(stringResource(R.string.privacy_detection_message)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDetectAllPages()
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.detect_all_pages))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDetectCurrentPage()
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.detect_current_page))
            }
        }
    )
}
