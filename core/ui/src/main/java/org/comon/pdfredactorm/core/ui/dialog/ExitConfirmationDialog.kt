package org.comon.pdfredactorm.core.ui.dialog

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.comon.pdfredactorm.core.ui.R
import org.comon.pdfredactorm.core.ui.ads.NativeAdView
import com.google.android.gms.ads.nativead.NativeAd
import androidx.core.net.toUri

@Composable
fun ExitConfirmationDialog(
    preloadedNativeAd: NativeAd?,
    isProEnabled: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current

    // 미리 로드된 광고가 있으면 즉시 버튼 활성화
    val isAdLoaded = preloadedNativeAd != null

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

                // Pro 활성화 시 네이티브 광고 비활성화
                if (!isProEnabled) {
                    NativeAdView(
                        preloadedAd = preloadedNativeAd,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(bottom = 16.dp)
                    )
                }

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
                        enabled = isProEnabled || isAdLoaded,
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(stringResource(R.string.exit_dialog_yes))
                    }
                }
            }
        }
    }
}

