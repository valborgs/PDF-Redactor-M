package org.comon.pdfredactorm.feature.editor

import org.comon.pdfredactorm.feature.editor.component.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    pdfId: String,
    onBackClick: () -> Unit,
    onBackToHomeWithRedeem: () -> Unit = {},
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // UI-only states (transient, not business logic)
    var showMainMenu by remember { mutableStateOf(false) }
    var showPrivacyDetectionDialog by remember { mutableStateOf(false) }
    var showPageJumpDialog by remember { mutableStateOf(false) }
    var showTableOfContentsDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }

    // Color Picker State (high-frequency updates, kept in UI)
    var colorPickerDragPosition by remember { mutableStateOf<Offset?>(null) }
    var colorPickerPreviewColor by remember { mutableStateOf<Int?>(null) }

    // Mask Deletion State
    var showDeleteMaskDialog by remember { mutableStateOf(false) }
    var maskToDeleteId by remember { mutableStateOf<String?>(null) }

    // File Size Exceeded Dialog State
    var showFileSizeExceededDialog by remember { mutableStateOf(false) }
    
    // Network Fallback Dialog State
    var showNetworkFallbackDialog by remember { mutableStateOf(false) }

    // Device Mismatch Dialog State
    var showDeviceMismatchDialog by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Load PDF on initial composition
    LaunchedEffect(pdfId) {
        viewModel.handleIntent(EditorIntent.LoadPdf(pdfId))
    }

    // Save file launcher
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            viewModel.handleIntent(EditorIntent.SaveFinalDocument(it))
        }
    }

    // Collect SideEffects from ViewModel
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is EditorSideEffect.OpenSaveLauncher -> {
                    val originalName = viewModel.uiState.value.document?.fileName ?: "document.pdf"
                    // Pro 모드이지만 네트워크 fallback으로 로컬 마스킹을 사용한 경우 pro_ 접두사 생략
                    val fileName = if (viewModel.uiState.value.isProEnabled && !effect.isLocalFallback) {
                        "pro_redacted_$originalName"
                    } else {
                        "redacted_$originalName"
                    }
                    saveLauncher.launch(fileName)
                }
                is EditorSideEffect.ShowSnackbar -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    launch {
                        snackbarHostState.showSnackbar(effect.message, duration = SnackbarDuration.Short)
                    }
                }
                is EditorSideEffect.NavigateBack -> {
                    onBackClick()
                }
                is EditorSideEffect.ShowFileSizeExceededDialog -> {
                    showFileSizeExceededDialog = true
                }
                is EditorSideEffect.ShowNetworkFallbackDialog -> {
                    showNetworkFallbackDialog = true
                }
                is EditorSideEffect.ShowDeviceMismatchDialog -> {
                    showDeviceMismatchDialog = effect.message
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = uiState.document?.fileName ?: stringResource(R.string.editor_title),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_content_description))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMainMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.menu_content_description))
                        }
                        DropdownMenu(
                            expanded = showMainMenu,
                            onDismissRequest = { showMainMenu = false }
                        ) {
                            // Save Option
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.save_content_description)) },
                                onClick = {
                                    showMainMenu = false
                                    viewModel.handleIntent(EditorIntent.PerformRedaction)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                }
                            )

                            // Table of Contents Option
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.outline_title)) },
                                onClick = {
                                    showMainMenu = false
                                    showTableOfContentsDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                                }
                            )

                            // Privacy Detection Option
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.detect_pii_content_description)) },
                                onClick = {
                                    showMainMenu = false
                                    showPrivacyDetectionDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { viewModel.handleIntent(EditorIntent.PrevPage) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.prev_page_content_description))
                    }

                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    LaunchedEffect(isPressed) {
                        if (isPressed) {
                            showPageJumpDialog = true
                        }
                    }

                    OutlinedTextField(
                        value = stringResource(R.string.page_indicator, uiState.currentPage + 1, uiState.pageCount),
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier
                            .width(100.dp)
                            .padding(horizontal = 4.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp
                        ),
                        singleLine = true,
                        interactionSource = interactionSource
                    )

                    IconButton(onClick = { viewModel.handleIntent(EditorIntent.NextPage) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.next_page_content_description))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Color Picker Button
                        IconButton(
                            onClick = { showColorPickerDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Circle,
                                contentDescription = stringResource(R.string.select_mask_color_content_description),
                                tint = Color(uiState.currentMaskColor),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Icon(
                            painter = painterResource(id = R.drawable.ic_highlighter),
                            contentDescription = stringResource(R.string.toggle_masking_content_description),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = uiState.isMaskingMode,
                            onCheckedChange = { 
                                viewModel.handleIntent(EditorIntent.ToggleMaskingMode)
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading || uiState.isDetecting) {
                CircularProgressIndicator()
            } else {
                uiState.currentPageBitmap?.let { bitmap ->
                    PdfViewer(
                        bitmap = bitmap,
                        redactions = uiState.redactions.filter { it.pageIndex == uiState.currentPage },
                        detectedPii = uiState.detectedPii.filter { it.pageIndex == uiState.currentPage },
                        isMaskingMode = uiState.isMaskingMode,
                        isColorPickingMode = uiState.isColorPickingMode,
                        pdfPageWidth = uiState.pdfPageWidth,
                        pdfPageHeight = uiState.pdfPageHeight,
                        onAddRedaction = { x, y, w, h ->
                            viewModel.handleIntent(EditorIntent.AddRedaction(x, y, w, h))
                        },
                        onConvertPiiToMask = { pii ->
                            viewModel.handleIntent(EditorIntent.ConvertPiiToMask(pii))
                        },
                        onRemoveDetectedPii = { pii ->
                            viewModel.handleIntent(EditorIntent.RemoveDetectedPii(pii))
                        },
                        onColorPicked = { color ->
                            viewModel.handleIntent(EditorIntent.SetMaskColor(color))
                        },
                        onColorPickDrag = { position, color ->
                            colorPickerDragPosition = position
                            colorPickerPreviewColor = color
                        },
                        onShowDeleteMaskDialog = { id ->
                            maskToDeleteId = id
                            showDeleteMaskDialog = true
                        }
                    )
                }
            }
            // Color Picker Magnifier (Preview)
            if (uiState.isColorPickingMode && colorPickerDragPosition != null && colorPickerPreviewColor != null) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val position = colorPickerDragPosition!!
                    val isLeft = position.x < (LocalConfiguration.current.screenWidthDp * LocalDensity.current.density / 2)
                    
                    Box(
                        modifier = Modifier
                            .align(if (isLeft) Alignment.CenterEnd else Alignment.CenterStart)
                            .padding(32.dp)
                            .size(120.dp)
                            .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                            .border(2.dp, Color.Gray, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(Color(colorPickerPreviewColor!!), androidx.compose.foundation.shape.CircleShape)
                                    .border(1.dp, Color.LightGray, androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "#${Integer.toHexString(colorPickerPreviewColor!!).uppercase().takeLast(6)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Page Jump Dialog
        if (showPageJumpDialog) {
            PageJumpDialog(
                pageCount = uiState.pageCount,
                onDismiss = { showPageJumpDialog = false },
                onJump = { pageNumber ->
                    viewModel.handleIntent(EditorIntent.GoToPage(pageNumber))
                }
            )
        }

        // Outline Dialog
        if (showTableOfContentsDialog) {
            TableOfContentsDialog(
                tableOfContents = uiState.tableOfContents,
                onDismiss = { showTableOfContentsDialog = false },
                onItemClick = { pageIndex ->
                    viewModel.handleIntent(EditorIntent.GoToPage(pageIndex + 1))
                }
            )
        }

        // Color Picker Dialog
        if (showColorPickerDialog) {
            ColorPickerDialog(
                onDismiss = { showColorPickerDialog = false },
                onColorSelected = { color ->
                    viewModel.handleIntent(EditorIntent.SetMaskColor(color))
                },
                onPickFromPdf = {
                    viewModel.handleIntent(EditorIntent.ToggleColorPickingMode)
                }
            )
        }

        // Mask Deletion Dialog
        if (showDeleteMaskDialog && maskToDeleteId != null) {
            DeleteMaskDialog(
                onDismiss = {
                    showDeleteMaskDialog = false
                    maskToDeleteId = null
                },
                onDelete = {
                    maskToDeleteId?.let { viewModel.handleIntent(EditorIntent.RemoveRedaction(it)) }
                }
            )
        }
        // Privacy Detection Dialog
        if (showPrivacyDetectionDialog) {
            PrivacyDetectionDialog(
                onDismiss = { showPrivacyDetectionDialog = false },
                onDetectCurrentPage = { viewModel.handleIntent(EditorIntent.DetectPiiInCurrentPage) },
                onDetectAllPages = { viewModel.handleIntent(EditorIntent.DetectPiiInAllPages) }
            )
        }

        // PII Detection Result Dialog
        if (uiState.piiDetectionCount != null) {
            PiiDetectionResultDialog(
                count = uiState.piiDetectionCount ?: 0,
                onDismiss = { viewModel.handleIntent(EditorIntent.ConsumePiiDetectionResult) },
                onConfirm = { viewModel.handleIntent(EditorIntent.ConsumePiiDetectionResult) }
            )
        }

        // File Size Exceeded Dialog
        if (showFileSizeExceededDialog) {
            AlertDialog(
                onDismissRequest = { showFileSizeExceededDialog = false },
                title = { Text(stringResource(R.string.notice_title)) },
                text = { Text(stringResource(R.string.error_file_size_exceeded)) },
                confirmButton = {
                    TextButton(onClick = { showFileSizeExceededDialog = false }) {
                        Text(stringResource(R.string.confirm_button))
                    }
                }
            )
        }
        
        // Network Fallback Dialog (3 buttons: Cancel, Proceed, Settings)
        if (showNetworkFallbackDialog) {
            val context = androidx.compose.ui.platform.LocalContext.current
            AlertDialog(
                onDismissRequest = { showNetworkFallbackDialog = false },
                title = { Text(stringResource(R.string.notice_title)) },
                text = { Text(stringResource(R.string.network_fallback_message)) },
                confirmButton = {
                    // 설정 버튼 (강조)
                    Button(
                        onClick = {
                            showNetworkFallbackDialog = false
                            val intent = android.content.Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
                            context.startActivity(intent)
                        }
                    ) {
                        Text(stringResource(R.string.button_settings))
                    }
                },
                dismissButton = {
                    Row {
                        // 취소 버튼
                        TextButton(onClick = { showNetworkFallbackDialog = false }) {
                            Text(stringResource(R.string.cancel_button))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        // 진행 버튼
                        TextButton(
                            onClick = {
                                showNetworkFallbackDialog = false
                                viewModel.handleIntent(EditorIntent.ProceedWithLocalRedaction)
                            }
                        ) {
                            Text(stringResource(R.string.button_proceed))
                        }
                    }
                }
            )
        }

        // Device Mismatch Dialog (2003 Error)
        showDeviceMismatchDialog?.let { message ->
            AlertDialog(
                onDismissRequest = { /* Dismiss not allowed */ },
                title = { Text(stringResource(R.string.notice_title)) },
                text = { Text(message.ifEmpty { stringResource(R.string.device_mismatch_message) }) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeviceMismatchDialog = null
                            // Navigate back and request to open redeem dialog
                            onBackToHomeWithRedeem() 
                        }
                    ) {
                        Text(stringResource(R.string.button_redeem_input))
                    }
                }
            )
        }
    }
}
