package org.comon.pdfredactorm.presentation.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import org.comon.pdfredactorm.R
import org.comon.pdfredactorm.domain.model.DetectedPii
import org.comon.pdfredactorm.domain.model.RedactionMask
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    pdfId: String,
    onBackClick: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showDetectionMenu by remember { mutableStateOf(false) }
    var showMainMenu by remember { mutableStateOf(false) }
    var showPrivacyDetectionDialog by remember { mutableStateOf(false) }
    var showPageJumpDialog by remember { mutableStateOf(false) }
    var pageInputValue by remember { mutableStateOf("") }
    var showPageInputError by remember { mutableStateOf(false) }
    var showOutlineDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }

    // Color Picker State
    var colorPickerDragPosition by remember { mutableStateOf<Offset?>(null) }
    var colorPickerPreviewColor by remember { mutableStateOf<Int?>(null) }

    // Mask Deletion State
    var showDeleteMaskDialog by remember { mutableStateOf(false) }
    var maskToDeleteId by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val maskingEnabledMessage = stringResource(R.string.masking_mode_enabled)
    val maskingDisabledMessage = stringResource(R.string.masking_mode_disabled)
    val colorPickerEnabledMessage = stringResource(R.string.color_picker_mode_enabled)
    val colorSelectedMessage = stringResource(R.string.color_selected)
    
    LaunchedEffect(pdfId) {
        viewModel.loadPdf(pdfId)
    }


    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { 
            if (uiState.proRedactionSuccess) {
                viewModel.saveProFile(it)
            } else {
                viewModel.savePdf(it) 
            }
        }
    }

    LaunchedEffect(uiState.proRedactionSuccess) {
        if (uiState.proRedactionSuccess) {
            val fileName = "pro_redacted_${System.currentTimeMillis()}.pdf"
            saveLauncher.launch(fileName)
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            android.util.Log.d("EditorScreen", "PDF Saved Successfully")
            onBackClick()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMsg ->
            android.util.Log.d("EditorScreen", "Error: $errorMsg")
            val message = if (errorMsg.contains("Pro Redaction Failed")) {
                "pdf 마스킹 처리에 실패하였습니다."
            } else {
                errorMsg
            }
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            viewModel.consumeError()
        }
    }

    Scaffold(
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
                                text = { Text("저장") },
                                onClick = {
                                    showMainMenu = false
                                    val fileName = "redacted_${System.currentTimeMillis()}.pdf"
                                    saveLauncher.launch(fileName)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                }
                            )

                            // Table of Contents Option
                            DropdownMenuItem(
                                text = { Text("목차") },
                                onClick = {
                                    showMainMenu = false
                                    if (uiState.outline.isNotEmpty()) {
                                        showOutlineDialog = true
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("목차가 없습니다.", duration = SnackbarDuration.Short)
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                                }
                            )

                            // Privacy Detection Option
                            DropdownMenuItem(
                                text = { Text("개인정보 탐지") },
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
                    IconButton(onClick = { viewModel.prevPage() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.prev_page_content_description))
                    }

                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    LaunchedEffect(isPressed) {
                        if (isPressed) {
                            pageInputValue = (uiState.currentPage + 1).toString()
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

                    IconButton(onClick = { viewModel.nextPage() }) {
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
                                viewModel.toggleMaskingMode()
                                val message = if (!uiState.isMaskingMode) maskingEnabledMessage else maskingDisabledMessage
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
                                }
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
                            viewModel.addRedaction(x, y, w, h)
                        },
                        onRemoveRedaction = { id ->
                            viewModel.removeRedaction(id)
                        },
                        onConvertPiiToMask = { pii ->
                            viewModel.convertDetectedPiiToMask(pii)
                        },
                        onRemoveDetectedPii = { pii ->
                            viewModel.removeDetectedPii(pii)
                        },
                        onColorPicked = { color ->
                            viewModel.setMaskColor(color)
                            viewModel.toggleColorPickingMode()
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(colorSelectedMessage, duration = SnackbarDuration.Short)
                            }
                        },
                        onColorPickDrag = { position, color ->
                            colorPickerDragPosition = position
                            colorPickerPreviewColor = color
                        },
                        maskToDeleteId = maskToDeleteId,
                        showDeleteMaskDialog = showDeleteMaskDialog,
                        onShowDeleteMaskDialog = { id ->
                            maskToDeleteId = id
                            showDeleteMaskDialog = true
                        },
                        onDismissDeleteMaskDialog = {
                            showDeleteMaskDialog = false
                            maskToDeleteId = null
                        },
                        onConfirmDeleteMask = {
                            maskToDeleteId?.let { viewModel.removeRedaction(it) }
                            showDeleteMaskDialog = false
                            maskToDeleteId = null
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
                    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
                    val isLeft = position.x < (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp * androidx.compose.ui.platform.LocalDensity.current.density / 2)
                    
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
        AlertDialog(
            onDismissRequest = {
                showPageJumpDialog = false
                pageInputValue = ""
                showPageInputError = false
            },
            title = { Text(stringResource(R.string.go_to_page_title)) },
            text = {
                OutlinedTextField(
                    value = pageInputValue,
                    onValueChange = {
                        pageInputValue = it
                        showPageInputError = false
                    },
                    label = { Text(stringResource(R.string.go_to_page_hint, uiState.pageCount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = showPageInputError,
                    supportingText = if (showPageInputError) {
                        { Text(stringResource(R.string.invalid_page_number)) }
                    } else null
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pageNumber = pageInputValue.toIntOrNull()
                        if (pageNumber != null && pageNumber in 1..uiState.pageCount) {
                            viewModel.goToPage(pageNumber)
                            showPageJumpDialog = false
                            pageInputValue = ""
                            showPageInputError = false
                        } else {
                            showPageInputError = true
                        }
                    }
                ) {
                    Text(stringResource(R.string.confirm_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPageJumpDialog = false
                        pageInputValue = ""
                        showPageInputError = false
                    }
                ) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    // Outline Dialog
    if (showOutlineDialog) {
        AlertDialog(
            onDismissRequest = { showOutlineDialog = false },
            title = { Text(stringResource(R.string.outline_title)) },
            text = {
                if (uiState.outline.isEmpty()) {
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
                            uiState.outline.forEach { item ->
                                OutlineItemView(
                                    item = item,
                                    level = 0,
                                    onClick = { pageIndex ->
                                        viewModel.goToPage(pageIndex + 1)
                                        showOutlineDialog = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOutlineDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    // Color Picker Dialog
    if (showColorPickerDialog) {
        AlertDialog(
            onDismissRequest = { showColorPickerDialog = false },
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
                                viewModel.setMaskColor(0xFF000000.toInt())
                                showColorPickerDialog = false
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(colorSelectedMessage, duration = SnackbarDuration.Short)
                                }
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
                                viewModel.setMaskColor(0xFFFFFFFF.toInt())
                                showColorPickerDialog = false
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(colorSelectedMessage, duration = SnackbarDuration.Short)
                                }
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
                                viewModel.toggleColorPickingMode()
                                showColorPickerDialog = false
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(colorPickerEnabledMessage, duration = SnackbarDuration.Short)
                                }
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
                TextButton(onClick = { showColorPickerDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    // Mask Deletion Dialog
    if (showDeleteMaskDialog && maskToDeleteId != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteMaskDialog = false
                maskToDeleteId = null
            },
            title = { Text(stringResource(R.string.delete_mask_title)) },
            text = { Text(stringResource(R.string.delete_mask_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        maskToDeleteId?.let { viewModel.removeRedaction(it) }
                        showDeleteMaskDialog = false
                        maskToDeleteId = null
                    }
                ) {
                    Text(stringResource(R.string.delete_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteMaskDialog = false
                        maskToDeleteId = null
                    }
                ) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
    // Privacy Detection Dialog
    if (showPrivacyDetectionDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDetectionDialog = false },
            title = { Text("개인정보 탐지") },
            text = { Text("탐지 범위를 선택해주세요.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.detectPiiInAllPages()
                        showPrivacyDetectionDialog = false
                    }
                ) {
                    Text("모든 페이지 탐지")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.detectPiiInCurrentPage()
                        showPrivacyDetectionDialog = false
                    }
                ) {
                    Text("현재 페이지 탐지")
                }
            }
        )
    }

    // PII Detection Result Dialog
    if (uiState.piiDetectionCount != null) {
        AlertDialog(
            onDismissRequest = { viewModel.consumePiiDetectionResult() },
            title = { Text("알림") },
            text = { Text("문서에 있는 개인정보 탐지를 수행했습니다. \n탐지수 : ${uiState.piiDetectionCount}개") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.consumePiiDetectionResult() }
                ) {
                    Text(stringResource(R.string.confirm_button))
                }
            }
        )
    }
    }
}

@Composable
fun OutlineItemView(
    item: org.comon.pdfredactorm.domain.model.PdfOutlineItem,
    level: Int,
    onClick: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(item.pageIndex) }
                .padding(
                    start = (16 * level).dp + 8.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                    end = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (level == 0) FontWeight.Bold else FontWeight.Normal
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.outline_page_format, item.pageIndex + 1),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Render children recursively
        item.children.forEach { child ->
            OutlineItemView(
                item = child,
                level = level + 1,
                onClick = onClick
            )
        }
    }
}

@Composable
fun PdfViewer(
    bitmap: Bitmap,
    redactions: List<RedactionMask>,
    detectedPii: List<DetectedPii>,
    isMaskingMode: Boolean,
    isColorPickingMode: Boolean,
    pdfPageWidth: Int,
    pdfPageHeight: Int,
    onAddRedaction: (Float, Float, Float, Float) -> Unit,
    onRemoveRedaction: (String) -> Unit,
    onConvertPiiToMask: (DetectedPii) -> Unit,
    onRemoveDetectedPii: (DetectedPii) -> Unit,
    onColorPicked: (Int) -> Unit,
    onColorPickDrag: (Offset?, Int?) -> Unit,
    // Mask deletion state
    maskToDeleteId: String?,
    showDeleteMaskDialog: Boolean,
    onShowDeleteMaskDialog: (String) -> Unit,
    onDismissDeleteMaskDialog: () -> Unit,
    onConfirmDeleteMask: () -> Unit
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Calculate initial scale to fit entire PDF within canvas
    // Use the minimum of width-based and height-based scales to ensure nothing is cut off
    val fitScale = remember(bitmap, canvasSize) {
        if (canvasSize.height > 0 && canvasSize.width > 0 && bitmap.height > 0 && bitmap.width > 0) {
            val widthScale = canvasSize.width.toFloat() / bitmap.width.toFloat()
            val heightScale = canvasSize.height.toFloat() / bitmap.height.toFloat()
            // Use minimum to ensure entire document fits
            minOf(widthScale, heightScale)
        } else {
            1f
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Reset scale and offset when bitmap or canvas size changes
    LaunchedEffect(bitmap, canvasSize) {
        scale = 1f
        offset = Offset.Zero
    }

    // Temporary drag state for drawing new mask
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }

    // Context menu state for detected PII
    var selectedPii by remember { mutableStateOf<DetectedPii?>(null) }
    var showPiiContextMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(fitScale, bitmap) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (!isMaskingMode && !isColorPickingMode) {
                        // Update scale
                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                        scale = newScale

                        // Only allow panning when zoomed in (scale > 1)
                        if (scale > 1f) {
                            // Calculate bounds to keep PDF within view
                            val scaledWidth = bitmap.width * fitScale * scale
                            val scaledHeight = bitmap.height * fitScale * scale

                            val maxOffsetX = 0f
                            val minOffsetX = -(scaledWidth - canvasSize.width).coerceAtLeast(0f)
                            val maxOffsetY = 0f
                            val minOffsetY = -(scaledHeight - canvasSize.height).coerceAtLeast(0f)

                            // Apply pan with bounds
                            val newOffset = offset + pan
                            offset = Offset(
                                x = newOffset.x.coerceIn(minOffsetX, maxOffsetX),
                                y = newOffset.y.coerceIn(minOffsetY, maxOffsetY)
                            )
                        } else {
                            // Reset offset when scale returns to 1
                            offset = Offset.Zero
                        }
                    }
                }

            }
            .pointerInput(isColorPickingMode, fitScale, bitmap) {
                if (isColorPickingMode) {
                    var lastColor: Int? = null
                    detectDragGestures(
                        onDragStart = { dragStartOffset ->
                            // Initial pick
                            val totalScale = fitScale * scale
                            // offset is the pan offset (state), dragStartOffset is the touch position
                             val startX = (dragStartOffset.x - offset.x) / totalScale
                             val startY = (dragStartOffset.y - offset.y) / totalScale
                             
                             if (startX >= 0 && startX < bitmap.width && startY >= 0 && startY < bitmap.height) {
                                 val pixelColor = bitmap.getPixel(startX.toInt(), startY.toInt())
                                 lastColor = pixelColor
                                 onColorPickDrag(dragStartOffset, pixelColor)
                             }
                        },
                        onDragEnd = {
                            lastColor?.let { color ->
                                onColorPicked(color)
                            }
                            onColorPickDrag(null, null)
                            lastColor = null
                        },
                        onDragCancel = {
                            onColorPickDrag(null, null)
                            lastColor = null
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val position = change.position
                            
                            // Calculate bitmap coordinates
                            val totalScale = fitScale * scale
                            // position is relative to the Box (Canvas)
                            // The content is translated by 'offset' and scaled by 'totalScale'
                            
                            val bitmapX = (position.x - offset.x) / totalScale
                            val bitmapY = (position.y - offset.y) / totalScale
                            
                            if (bitmapX >= 0 && bitmapX < bitmap.width && bitmapY >= 0 && bitmapY < bitmap.height) {
                                val pixelColor = bitmap.getPixel(bitmapX.toInt(), bitmapY.toInt())
                                lastColor = pixelColor
                                onColorPickDrag(position, pixelColor)
                            }
                        }
                    )
                }
            }
            .pointerInput(isColorPickingMode, fitScale, bitmap) {
                if (isColorPickingMode) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            val totalScale = fitScale * scale
                            val bitmapX = (tapOffset.x - offset.x) / totalScale
                            val bitmapY = (tapOffset.y - offset.y) / totalScale
                            
                            if (bitmapX >= 0 && bitmapX < bitmap.width && bitmapY >= 0 && bitmapY < bitmap.height) {
                                val pixelColor = bitmap.getPixel(bitmapX.toInt(), bitmapY.toInt())
                                onColorPicked(pixelColor)
                            }
                        },
                        onPress = { pressOffset ->
                            // Handle initial press for drag preview
                            val totalScale = fitScale * scale
                            val bitmapX = (pressOffset.x - offset.x) / totalScale
                            val bitmapY = (pressOffset.y - offset.y) / totalScale
                            
                            if (bitmapX >= 0 && bitmapX < bitmap.width && bitmapY >= 0 && bitmapY < bitmap.height) {
                                val pixelColor = bitmap.getPixel(bitmapX.toInt(), bitmapY.toInt())
                                onColorPickDrag(pressOffset, pixelColor)
                            }
                            tryAwaitRelease()
                            // On release (if not dragged enough to trigger drag gesture), confirm selection
                            // But detectTapGestures handles onTap separately.
                            // We need to clear preview on release if it was a tap.
                            onColorPickDrag(null, null)
                            
                            // If it was a tap, onTap will fire.
                            // If it was a drag, detectDragGestures will take over? 
                            // Compose gesture handling can be tricky. 
                            // Let's use a simpler approach: update preview in onPress, clear in onRelease.
                            // Actual selection happens in onTap or onDragEnd.
                        }
                    )
                }
            }
            .pointerInput(isMaskingMode, fitScale) {
                if (isMaskingMode) {
                    detectDragGestures(
                        onDragStart = { dragStart = it },
                        onDragEnd = {
                            if (dragStart != null && dragEnd != null) {
                                // Convert screen coordinates to bitmap coordinates
                                val totalScale = fitScale * scale
                                val start = (dragStart!! - offset) / totalScale
                                val end = (dragEnd!! - offset) / totalScale

                                // Map from Bitmap size to PDF size
                                val bitmapWidth = bitmap.width.toFloat()
                                val bitmapHeight = bitmap.height.toFloat()

                                val scaleX = pdfPageWidth / bitmapWidth
                                val scaleY = pdfPageHeight / bitmapHeight

                                val x = minOf(start.x, end.x) * scaleX
                                val y = minOf(start.y, end.y) * scaleY
                                val w = kotlin.math.abs(start.x - end.x) * scaleX
                                val h = kotlin.math.abs(start.y - end.y) * scaleY

                                onAddRedaction(x, y, w, h)
                            }
                            dragStart = null
                            dragEnd = null
                        },
                        onDrag = { change, _ ->
                            dragEnd = change.position
                        }
                    )
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = fitScale * scale,
                    scaleY = fitScale * scale,
                    translationX = offset.x,
                    translationY = offset.y,
                    transformOrigin = TransformOrigin(0f, 0f)
                )
                .pointerInput(isMaskingMode, isColorPickingMode, fitScale) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            if (!isMaskingMode && !isColorPickingMode) {
                                val bitmapWidth = bitmap.width.toFloat()
                                val bitmapHeight = bitmap.height.toFloat()
                                val scaleX = pdfPageWidth / bitmapWidth
                                val scaleY = pdfPageHeight / bitmapHeight

                                val tapX = tapOffset.x * scaleX
                                val tapY = tapOffset.y * scaleY

                                // Check if tap is inside any redaction
                                val hitMask = redactions.find { mask ->
                                    tapX >= mask.x && tapX <= mask.x + mask.width &&
                                    tapY >= mask.y && tapY <= mask.y + mask.height
                                }

                                if (hitMask != null) {
                                    onShowDeleteMaskDialog(hitMask.id)
                                    return@detectTapGestures
                                }

                                // Check if tap is inside any detected PII
                                val hitPii = detectedPii.find { pii ->
                                    tapX >= pii.x && tapX <= pii.x + pii.width &&
                                    tapY >= pii.y && tapY <= pii.y + pii.height
                                }

                                if (hitPii != null) {
                                    selectedPii = hitPii
                                    showPiiContextMenu = true
                                }
                            }
                        }
                    )
                }
        ) {
            // Draw PDF Page
            drawImage(bitmap.asImageBitmap())
            
            // Draw Existing Redactions and Detected PII
            val bitmapWidth = bitmap.width.toFloat()
            val bitmapHeight = bitmap.height.toFloat()
            val scaleX = bitmapWidth / pdfPageWidth
            val scaleY = bitmapHeight / pdfPageHeight

            // Draw detected PII (in yellow/blue with border)
            detectedPii.forEach { pii ->
                drawRect(
                    color = Color.Yellow.copy(alpha = 0.3f),
                    topLeft = Offset(pii.x * scaleX, pii.y * scaleY),
                    size = Size(pii.width * scaleX, pii.height * scaleY)
                )
                drawRect(
                    color = Color.Blue,
                    topLeft = Offset(pii.x * scaleX, pii.y * scaleY),
                    size = Size(pii.width * scaleX, pii.height * scaleY),
                    style = Stroke(width = 2f)
                )
            }

            // Draw redactions (with mask color)
            redactions.forEach { mask ->
                // Fill with mask color
                drawRect(
                    color = Color(mask.color).copy(alpha = 0.5f),
                    topLeft = Offset(mask.x * scaleX, mask.y * scaleY),
                    size = Size(mask.width * scaleX, mask.height * scaleY)
                )
                // Draw black border for visibility
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(mask.x * scaleX, mask.y * scaleY),
                    size = Size(mask.width * scaleX, mask.height * scaleY),
                    style = Stroke(width = 2f)
                )
            }
            
            // Draw Current Drag Selection
            if (isMaskingMode && dragStart != null && dragEnd != null) {
                // Calculate the rectangle in bitmap space considering both fitScale and user zoom
                val totalScale = fitScale * scale
                val start = (dragStart!! - offset) / totalScale
                val end = (dragEnd!! - offset) / totalScale

                drawRect(
                    color = Color.Red.copy(alpha = 0.3f),
                    topLeft = Offset(minOf(start.x, end.x), minOf(start.y, end.y)),
                    size = Size(kotlin.math.abs(start.x - end.x), kotlin.math.abs(start.y - end.y))
                )
                drawRect(
                    color = Color.Red,
                    topLeft = Offset(minOf(start.x, end.x), minOf(start.y, end.y)),
                    size = Size(kotlin.math.abs(start.x - end.x), kotlin.math.abs(start.y - end.y)),
                    style = Stroke(width = 2f)
                )
            }
        }

        // Context Menu for Detected PII
        if (showPiiContextMenu && selectedPii != null) {
            AlertDialog(
                onDismissRequest = {
                    showPiiContextMenu = false
                    selectedPii = null
                },
                title = { Text(stringResource(R.string.detected_pii_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.pii_type_label, selectedPii?.type ?: ""))
                        Text(stringResource(R.string.pii_text_label, selectedPii?.text ?: ""))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        selectedPii?.let { onConvertPiiToMask(it) }
                        showPiiContextMenu = false
                        selectedPii = null
                    }) {
                        Text(stringResource(R.string.masking_button))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        selectedPii?.let { onRemoveDetectedPii(it) }
                        showPiiContextMenu = false
                        selectedPii = null
                    }) {
                        Text(stringResource(R.string.cancel_button))
                    }
                }
            )
        }
    }
}
