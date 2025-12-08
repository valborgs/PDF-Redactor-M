package org.comon.pdfredactorm.presentation.home

import android.net.Uri
import android.content.Context
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.comon.pdfredactorm.BuildConfig
import org.comon.pdfredactorm.core.model.PdfDocument
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.Image
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.comon.pdfredactorm.R
import org.comon.pdfredactorm.core.ui.dialog.HelpDialog
import org.comon.pdfredactorm.core.ui.dialog.ExitConfirmationDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
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
                showResultDialog = exception.message ?: "Unknown Error"
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
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    val uriHandler = LocalUriHandler.current
                    
                    if (!isProEnabled) {
                        IconButton(onClick = { showRedeemDialog = true }) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_crown),
                                contentDescription = "Activate Pro",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    IconButton(onClick = {
                        uriHandler.openUri(BuildConfig.COFFEE_CHAT_URL)
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
                nativeAdUnitId = BuildConfig.ADMOB_NATIVE_ID,
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
                title = { Text(if(isSuccessResult) "성공" else "실패") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { showResultDialog = null }) {
                        Text("확인")
                    }
                }
            )
        }
    }
}

@Composable
fun RedeemCodeDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var codeError by remember { mutableStateOf<String?>(null) }

    fun validateAndSubmit() {
        var hasError = false
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "유효한 이메일 주소를 입력해주세요."
            hasError = true
        } else {
            emailError = null
        }

        if (code.length != 8) {
            codeError = "리딤 코드는 8자리여야 합니다."
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
        title = { Text("Pro 기능 활성화") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("이메일과 리딤 코드를 입력하세요.")
                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        if (emailError != null) emailError = null
                    },
                    label = { Text("이메일") },
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
                    label = { Text("리딤 코드") },
                    singleLine = true,
                    enabled = !isLoading,
                    isError = codeError != null,
                    supportingText = if (codeError != null) { { Text(codeError!!) } } else null
                )
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
                Text("제출")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("취소")
            }
        }
    )
}

@Composable
fun ProjectItem(
    project: PdfDocument,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = project.fileName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.page_count, project.pageCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_content_description))
            }
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
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "unknown.pdf"
}
