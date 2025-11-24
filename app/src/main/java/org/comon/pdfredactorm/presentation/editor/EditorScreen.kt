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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.comon.pdfredactorm.domain.model.DetectedPii
import org.comon.pdfredactorm.domain.model.RedactionMask
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    pdfId: String,
    onBackClick: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(pdfId) {
        viewModel.loadPdf(pdfId)
    }


    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { viewModel.savePdf(it) }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            android.util.Log.d("EditorScreen", "PDF Saved Successfully")
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            android.util.Log.d("EditorScreen", "Error: $it")
        }
    }

    var showDetectionMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.document?.fileName ?: "Editor") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Detection Menu
                    Box {
                        IconButton(onClick = { showDetectionMenu = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Detect PII")
                        }
                        DropdownMenu(
                            expanded = showDetectionMenu,
                            onDismissRequest = { showDetectionMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("현재 페이지 탐지") },
                                onClick = {
                                    viewModel.detectPiiInCurrentPage()
                                    showDetectionMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("모든 페이지 탐지") },
                                onClick = {
                                    viewModel.detectPiiInAllPages()
                                    showDetectionMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Done, contentDescription = null)
                                }
                            )
                        }
                    }

                    IconButton(onClick = {
                        val fileName = "redacted_${System.currentTimeMillis()}.pdf"
                        saveLauncher.launch(fileName)
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { viewModel.prevPage() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Prev Page")
                    }
                    Text(
                        "${uiState.currentPage + 1} / ${uiState.pageCount}",
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = { viewModel.nextPage() }) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next Page")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { viewModel.toggleMaskingMode() },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (uiState.isMaskingMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Toggle Masking")
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
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PdfViewer(
    bitmap: Bitmap,
    redactions: List<RedactionMask>,
    detectedPii: List<DetectedPii>,
    isMaskingMode: Boolean,
    pdfPageWidth: Int,
    pdfPageHeight: Int,
    onAddRedaction: (Float, Float, Float, Float) -> Unit,
    onRemoveRedaction: (String) -> Unit,
    onConvertPiiToMask: (DetectedPii) -> Unit,
    onRemoveDetectedPii: (DetectedPii) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

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
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (!isMaskingMode) {
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offset += pan
                        // Simple bounds check could be added here
                    }
                }
            }
            .pointerInput(isMaskingMode) {
                if (isMaskingMode) {
                    detectDragGestures(
                        onDragStart = { dragStart = it },
                        onDragEnd = {
                            if (dragStart != null && dragEnd != null) {
                                // Convert screen coordinates to PDF coordinates
                                val start = (dragStart!! - offset) / scale
                                val end = (dragEnd!! - offset) / scale
                                
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
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .pointerInput(isMaskingMode) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            if (!isMaskingMode) {
                                // Check if tap is inside any detected PII
                                val bitmapWidth = bitmap.width.toFloat()
                                val bitmapHeight = bitmap.height.toFloat()
                                val scaleX = pdfPageWidth / bitmapWidth
                                val scaleY = pdfPageHeight / bitmapHeight

                                val tapX = tapOffset.x * scaleX
                                val tapY = tapOffset.y * scaleY

                                val hitPii = detectedPii.find { pii ->
                                    tapX >= pii.x && tapX <= pii.x + pii.width &&
                                    tapY >= pii.y && tapY <= pii.y + pii.height
                                }

                                if (hitPii != null) {
                                    selectedPii = hitPii
                                    showPiiContextMenu = true
                                }
                            }
                        },
                        onLongPress = { tapOffset ->
                            if (!isMaskingMode) {
                                // Check if tap is inside any redaction
                                val bitmapWidth = bitmap.width.toFloat()
                                val bitmapHeight = bitmap.height.toFloat()
                                val scaleX = pdfPageWidth / bitmapWidth
                                val scaleY = pdfPageHeight / bitmapHeight

                                val tapX = tapOffset.x * scaleX
                                val tapY = tapOffset.y * scaleY

                                val hit = redactions.find { mask ->
                                    tapX >= mask.x && tapX <= mask.x + mask.width &&
                                    tapY >= mask.y && tapY <= mask.y + mask.height
                                }

                                hit?.let { onRemoveRedaction(it.id) }
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

            // Draw redactions (in black)
            redactions.forEach { mask ->
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset(mask.x * scaleX, mask.y * scaleY),
                    size = Size(mask.width * scaleX, mask.height * scaleY)
                )
            }
            
            // Draw Current Drag Selection
            if (isMaskingMode && dragStart != null && dragEnd != null) {
                // We need to reverse the transform to draw in local coordinates?
                // No, we are inside the graphicsLayer, so we draw in "bitmap space"?
                // Wait, the drag coordinates are in "screen space" (outer Box).
                // But the Canvas is transformed.
                // This is tricky.
                // Let's simplify:
                // If we draw inside the transformed canvas, we need to inverse transform the drag coordinates.
                // Or we can draw the selection rectangle in an overlay Canvas that is NOT transformed, 
                // but that would mismatch visually if the user is zoomed in.
                
                // Better approach: Calculate the rectangle in "Bitmap Space" and draw it.
                val start = (dragStart!! - offset) / scale
                val end = (dragEnd!! - offset) / scale
                
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
                title = { Text("탐지된 개인정보") },
                text = {
                    Column {
                        Text("타입: ${selectedPii?.type}")
                        Text("텍스트: ${selectedPii?.text}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        selectedPii?.let { onConvertPiiToMask(it) }
                        showPiiContextMenu = false
                        selectedPii = null
                    }) {
                        Text("마스킹")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        selectedPii?.let { onRemoveDetectedPii(it) }
                        showPiiContextMenu = false
                        selectedPii = null
                    }) {
                        Text("취소")
                    }
                }
            )
        }
    }
}
