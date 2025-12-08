package org.comon.pdfredactorm.core.ui.dialog

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.comon.pdfredactorm.core.ui.R
import org.comon.pdfredactorm.core.ui.ads.NativeAdView
import androidx.core.net.toUri

@Composable
fun ExitConfirmationDialog(
    nativeAdUnitId: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current

    var isAdLoaded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.exit_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                NativeAdView(
                    adUnitId = nativeAdUnitId,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(bottom = 16.dp),
                    onAdLoaded = { isAdLoaded = true }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.exit_dialog_no))
                    }

                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = "market://details?id=${context.packageName}".toUri()
                            setPackage("com.android.vending")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback to browser if Play Store app is not available
                            val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
                            }
                            context.startActivity(browserIntent)
                        }
                    }) {
                        Text(stringResource(R.string.exit_dialog_review))
                    }

                    Button(
                        onClick = onConfirm,
                        enabled = isAdLoaded
                    ) {
                        Text(stringResource(R.string.exit_dialog_yes))
                    }
                }
            }
        }
    }
}
