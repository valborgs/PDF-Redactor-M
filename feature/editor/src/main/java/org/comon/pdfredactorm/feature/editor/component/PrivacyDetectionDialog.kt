package org.comon.pdfredactorm.feature.editor.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun PrivacyDetectionDialog(
    onDismiss: () -> Unit,
    onDetectCurrentPage: () -> Unit,
    onDetectAllPages: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("개인정보 탐지") },
        text = { Text("탐지 범위를 선택해주세요.") },
        confirmButton = {
            TextButton(
                onClick = {
                    onDetectAllPages()
                    onDismiss()
                }
            ) {
                Text("모든 페이지 탐지")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDetectCurrentPage()
                    onDismiss()
                }
            ) {
                Text("현재 페이지 탐지")
            }
        }
    )
}
