package org.comon.pdfredactorm.feature.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.comon.pdfredactorm.feature.home.R

@Composable
fun RedeemCodeDialog(
    isLoading: Boolean,
    networkError: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    val invalidEmailMsg = stringResource(R.string.error_invalid_email)
    val invalidCodeMsg = stringResource(R.string.error_invalid_code)
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var codeError by remember { mutableStateOf<String?>(null) }

    fun validateAndSubmit() {
        var hasError = false
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = invalidEmailMsg
            hasError = true
        } else {
            emailError = null
        }

        if (code.length != 8) {
            codeError = invalidCodeMsg
            hasError = true
        } else {
            codeError = null
        }

        if (!hasError) {
            onSubmit(email, code)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pro_activation_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.pro_activation_message))
                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        if (emailError != null) emailError = null
                    },
                    label = { Text(stringResource(R.string.email_label)) },
                    singleLine = true,
                    enabled = !isLoading,
                    isError = emailError != null,
                    supportingText = if (emailError != null) { { Text(emailError!!) } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { 
                        code = it
                        if (codeError != null) codeError = null
                    },
                    label = { Text(stringResource(R.string.redeem_code_label)) },
                    singleLine = true,
                    enabled = !isLoading,
                    isError = codeError != null,
                    supportingText = if (codeError != null) { { Text(codeError!!) } } else null
                )
                // 네트워크 에러 메시지 (별도 표시)
                if (networkError != null) {
                    Text(
                        text = networkError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { validateAndSubmit() },
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.submit_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}

