package org.comon.pdfredactorm.feature.home

import android.net.Uri
import android.content.Context
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import org.comon.pdfredactorm.feature.home.component.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.Image
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import org.comon.pdfredactorm.core.ui.dialog.HelpDialog
import org.comon.pdfredactorm.core.ui.dialog.ExitConfirmationDialog

/**
 * 홈 화면에 필요한 설정값
 */
data class HomeScreenConfig(
    val appName: String,
    val coffeeChatUrl: String,
    val nativeAdUnitId: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    config: HomeScreenConfig,
    viewModel: HomeViewModel = viewModel(),
    onPdfClick: (String) -> Unit
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val recentProjects by viewModel.recentProjects.collectAsState()
    val showHelpDialog by viewModel.showHelpDialog.collectAsState()
    var showManualHelpDialog by remember { mutableStateOf(false) }

    val isProEnabled by viewModel.isProEnabled.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showRedeemDialog by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf<String?>(null) }
    var isSuccessResult by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.validationEvent.collect { result ->
            result.onSuccess { message ->
                showRedeemDialog = false
                isSuccessResult = true
                showResultDialog = message
            }.onFailure { exception ->
                isSuccessResult = false
                showResultDialog = exception.message ?: context.getString(R.string.unknown_error)
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Copy to cache to get a File object
            val inputStream = context.contentResolver.openInputStream(it)
            val fileName = getFileName(context, it)
            val tempFileName = if (fileName.lowercase().endsWith(".pdf")) {
                "temp_$fileName"
            } else {
                "temp_$fileName.pdf"
            }
            val file = File(context.cacheDir, tempFileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            viewModel.loadPdf(file) { pdfId ->
                onPdfClick(pdfId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(config.appName) },
                actions = {
                    val uriHandler = LocalUriHandler.current
                    
                    if (!isProEnabled) {
                        IconButton(onClick = { showRedeemDialog = true }) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_crown),
                                contentDescription = "Activate Functions",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    IconButton(onClick = {
                        uriHandler.openUri(config.coffeeChatUrl)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_coffee),
                            contentDescription = "Coffee Chat",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { showManualHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Help,
                            contentDescription = stringResource(R.string.help_content_description)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                launcher.launch(arrayOf("application/pdf"))
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.open_pdf_content_description))
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.5f),
                alpha = 0.3f
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentProjects) { project ->
                    ProjectItem(
                        project = project,
                        onClick = { onPdfClick(project.id) },
                        onDelete = { viewModel.deleteProject(project.id) }
                    )
                }
            }
        }

        // Show help dialog (First Launch)
        if (showHelpDialog) {
            HelpDialog(onDismiss = { viewModel.dismissHelpDialog() })
        }

        // Show help dialog (Manual)
        if (showManualHelpDialog) {
            HelpDialog(onDismiss = { showManualHelpDialog = false })
        }

        // Exit Confirmation Dialog
        var showExitDialog by remember { mutableStateOf(false) }

        BackHandler {
            showExitDialog = true
        }

        if (showExitDialog) {
            ExitConfirmationDialog(
                nativeAdUnitId = config.nativeAdUnitId,
                onDismiss = { showExitDialog = false },
                onConfirm = { activity?.finish() }
            )
        }
        
        if (showRedeemDialog) {
            RedeemCodeDialog(
                isLoading = isLoading,
                onDismiss = { showRedeemDialog = false },
                onSubmit = { email, code ->
                    viewModel.validateCode(email, code)
                }
            )
        }

        showResultDialog?.let { message ->
            AlertDialog(
                onDismissRequest = { showResultDialog = null },
                title = { Text(if(isSuccessResult) stringResource(R.string.success_title) else stringResource(R.string.failure_title)) },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { showResultDialog = null }) {
                        Text(stringResource(R.string.confirm_button))
                    }
                }
            )
        }
    }
}

private fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result.substring(cut + 1)
        }
    }
    return result ?: "unknown.pdf"
}
