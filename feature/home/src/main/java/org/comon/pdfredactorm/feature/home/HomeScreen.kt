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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.comon.pdfredactorm.core.model.ProInfo
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
    showRedeemDialogInitial: Boolean = false,
    onRedeemDialogConsumed: () -> Unit = {},
    onPdfClick: (String) -> Unit
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preloadedNativeAd by viewModel.preloadedNativeAd.collectAsStateWithLifecycle()

    // UI-only states (transient, not business logic)
    var showManualHelpDialog by remember { mutableStateOf(false) }
    var showRedeemDialog by remember { mutableStateOf(showRedeemDialogInitial) }

    // 홈 화면 진입 시 네이티브 광고 사전 로드 (Pro가 아닌 경우에만)
    LaunchedEffect(uiState.isProEnabled) {
        if (!uiState.isProEnabled) {
            viewModel.preloadNativeAd(config.nativeAdUnitId)
        }
    }


    var showResultDialog by remember { mutableStateOf<String?>(null) }
    var isSuccessResult by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var networkError by remember { mutableStateOf<String?>(null) }
    var proInfoToShow by remember { mutableStateOf<ProInfo?>(null) }

    // Collect SideEffects from ViewModel
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is HomeSideEffect.NavigateToEditor -> {
                    onPdfClick(effect.pdfId)
                }
                is HomeSideEffect.ShowValidationResult -> {
                    effect.result.onSuccess { message ->
                        showRedeemDialog = false
                        onRedeemDialogConsumed()
                        networkError = null
                        isSuccessResult = true
                        showResultDialog = message
                    }.onFailure { exception ->
                        isSuccessResult = false
                        showResultDialog = exception.message ?: context.getString(R.string.unknown_error)
                    }
                }
                is HomeSideEffect.ShowNetworkError -> {
                    networkError = context.getString(R.string.error_network_unavailable)
                }
                is HomeSideEffect.ShowProInfoDialog -> {
                    proInfoToShow = effect.proInfo
                }
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

            viewModel.onEvent(HomeEvent.LoadPdf(file))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(config.appName)
                        if (uiState.isProEnabled) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { viewModel.onEvent(HomeEvent.ShowProInfo) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_pro),
                                    contentDescription = "Pro Info",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    val uriHandler = LocalUriHandler.current
                    
                    if (!uiState.isProEnabled) {
                        IconButton(onClick = { showRedeemDialog = true }) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_crown),
                                contentDescription = "Activate Functions",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    IconButton(onClick = {
                        viewModel.onEvent(HomeEvent.CoffeeChatClicked)
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
                items(uiState.recentProjects) { project ->
                    ProjectItem(
                        project = project,
                        onClick = { onPdfClick(project.id) },
                        onDelete = { viewModel.onEvent(HomeEvent.DeleteProject(project.id)) }
                    )
                }
            }
        }

        // Show help dialog (First Launch)
        if (uiState.isFirstLaunch) {
            HelpDialog(onDismiss = { viewModel.onEvent(HomeEvent.ConsumeFirstLaunch) })
        }

        // Show help dialog (Manual)
        if (showManualHelpDialog) {
            HelpDialog(onDismiss = { showManualHelpDialog = false })
        }

        // Exit Confirmation Dialog
        BackHandler {
            showExitDialog = true
        }

        if (showExitDialog) {
            ExitConfirmationDialog(
                preloadedNativeAd = preloadedNativeAd,
                isProEnabled = uiState.isProEnabled,
                onDismiss = { showExitDialog = false },
                onConfirm = { activity?.finish() }
            )
        }
        
        if (showRedeemDialog) {
            RedeemCodeDialog(
                isLoading = uiState.isLoading,
                networkError = networkError,
                onDismiss = { 
                    showRedeemDialog = false
                    networkError = null
                    onRedeemDialogConsumed()
                },
                onSubmit = { email, code ->
                    networkError = null
                    viewModel.onEvent(HomeEvent.ValidateCode(email, code))
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

        proInfoToShow?.let { proInfo ->
            ProInfoDialog(
                proInfo = proInfo,
                onDismiss = { proInfoToShow = null }
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
