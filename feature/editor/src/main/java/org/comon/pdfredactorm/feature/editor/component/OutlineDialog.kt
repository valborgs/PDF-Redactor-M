package org.comon.pdfredactorm.feature.editor.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.comon.pdfredactorm.core.model.PdfOutlineItem
import org.comon.pdfredactorm.feature.editor.R

@Composable
fun OutlineDialog(
    outline: List<PdfOutlineItem>,
    onDismiss: () -> Unit,
    onItemClick: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.outline_title)) },
        text = {
            if (outline.isEmpty()) {
                Text(stringResource(R.string.no_outline_available))
            } else {
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 12.dp)
                            .verticalScroll(scrollState)
                    ) {
                        outline.forEach { item ->
                            OutlineItemView(
                                item = item,
                                level = 0,
                                onClick = { pageIndex ->
                                    onItemClick(pageIndex)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}
