package org.comon.pdfredactorm.feature.editor.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import org.comon.pdfredactorm.feature.editor.R

@Composable
fun PageJumpDialog(
    pageCount: Int,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit
) {
    var pageInputValue by remember { mutableStateOf("") }
    var showPageInputError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.go_to_page_title)) },
        text = {
            OutlinedTextField(
                value = pageInputValue,
                onValueChange = {
                    pageInputValue = it
                    showPageInputError = false
                },
                label = { Text(stringResource(R.string.go_to_page_hint, pageCount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = showPageInputError,
                supportingText = if (showPageInputError) {
                    { Text(stringResource(R.string.invalid_page_number)) }
                } else null
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val pageNumber = pageInputValue.toIntOrNull()
                    if (pageNumber != null && pageNumber in 1..pageCount) {
                        onJump(pageNumber)
                        onDismiss()
                    } else {
                        showPageInputError = true
                    }
                }
            ) {
                Text(stringResource(R.string.confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}
