package org.comon.pdfredactorm.feature.home.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.comon.pdfredactorm.core.model.ProInfo
import org.comon.pdfredactorm.feature.home.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pro 버전 활성화 정보를 표시하는 다이얼로그
 */
@Composable
fun ProInfoDialog(
    proInfo: ProInfo,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(proInfo.activationDate))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pro_info_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.pro_info_email, proInfo.email))
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.pro_info_uuid, proInfo.uuid))
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.pro_info_activation_date, formattedDate))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.confirm_button))
            }
        }
    )
}
