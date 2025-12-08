package org.comon.pdfredactorm.feature.editor.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.comon.pdfredactorm.feature.editor.R

@Composable
fun ColorPickerDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit,
    onPickFromPdf: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_mask_color_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Black Color Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onColorSelected(0xFF000000.toInt())
                            onDismiss()
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = stringResource(R.string.color_black),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                // White Color Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onColorSelected(0xFFFFFFFF.toInt())
                            onDismiss()
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = stringResource(R.string.color_white),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                // Pick from PDF Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onPickFromPdf()
                            onDismiss()
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = stringResource(R.string.pick_from_pdf),
                        style = MaterialTheme.typography.bodyLarge
                    )
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
