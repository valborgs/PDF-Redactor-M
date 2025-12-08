package org.comon.pdfredactorm.feature.editor

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.comon.pdfredactorm.core.model.DetectedPii
import org.comon.pdfredactorm.core.model.PdfDocument
import org.comon.pdfredactorm.core.model.PdfOutlineItem
import org.comon.pdfredactorm.core.model.RedactionMask
import org.comon.pdfredactorm.core.domain.usecase.pdf.GetPdfDocumentUseCase
import org.comon.pdfredactorm.core.domain.usecase.pdf.GetPdfOutlineUseCase
import org.comon.pdfredactorm.core.domain.usecase.pdf.DeletePdfDocumentUseCase
import org.comon.pdfredactorm.core.domain.usecase.DetectPiiUseCase
import org.comon.pdfredactorm.core.domain.usecase.GetRedactionsUseCase
import org.comon.pdfredactorm.core.domain.usecase.SaveRedactedPdfUseCase
import org.comon.pdfredactorm.core.domain.usecase.SaveRedactionsUseCase
import java.io.File
import java.util.UUID
import javax.inject.Inject
import androidx.core.graphics.createBitmap
import android.app.Application
import android.net.Uri
import org.comon.pdfredactorm.core.common.logger.Logger
import org.comon.pdfredactorm.core.domain.usecase.settings.GetProStatusUseCase

data class EditorUiState(
    val document: PdfDocument? = null,
    val currentPage: Int = 0,
    val pageCount: Int = 0,
    val currentPageBitmap: Bitmap? = null,
    val pdfPageWidth: Int = 0,
    val pdfPageHeight: Int = 0,
    val redactions: List<RedactionMask> = emptyList(),
    val detectedPii: List<DetectedPii> = emptyList(),
    val outline: List<PdfOutlineItem> = emptyList(),
    val isMaskingMode: Boolean = false,
    val isDetecting: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val currentMaskColor: Int = 0xFF000000.toInt(), // Default: Black
    val isColorPickingMode: Boolean = false,
    val tempRedactedFile: File? = null,
    val piiDetectionCount: Int? = null,
    val isProEnabled: Boolean = false
)

sealed interface EditorSideEffect {
    data object OpenSaveLauncher : EditorSideEffect
    data class ShowSnackbar(val message: String) : EditorSideEffect
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val application: Application,
    private val getPdfDocumentUseCase: GetPdfDocumentUseCase,
    private val getPdfOutlineUseCase: GetPdfOutlineUseCase,
    private val deletePdfDocumentUseCase: DeletePdfDocumentUseCase,
    private val getRedactionsUseCase: GetRedactionsUseCase,
    private val saveRedactionsUseCase: SaveRedactionsUseCase,
    private val saveRedactedPdfUseCase: SaveRedactedPdfUseCase,
    private val detectPiiUseCase: DetectPiiUseCase,
    private val getProStatusUseCase: GetProStatusUseCase,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _sideEffect = kotlinx.coroutines.channels.Channel<EditorSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    init {
        viewModelScope.launch {
            getProStatusUseCase().collect { isPro ->
                _uiState.update { it.copy(isProEnabled = isPro) }
            }
        }
    }
    
    fun onSaveClicked() {
        performRedaction()
    }

    fun loadPdf(pdfId: String) {
        viewModelScope.launch {
            logger.info("User opened PDF document: $pdfId")
            // Reset success and error states to prevent LaunchedEffect from triggering with stale values
            _uiState.update { 
                it.copy(
                    isLoading = true,
                    saveSuccess = false,
                    error = null
                )
            }
            val document = getPdfDocumentUseCase(pdfId)
            if (document != null) {
                val redactions = getRedactionsUseCase(pdfId)
                val outline = getPdfOutlineUseCase(document.file)
                _uiState.update {
                    it.copy(
                        document = document,
                        pageCount = document.pageCount,
                        redactions = redactions,
                        outline = outline,
                        isLoading = false
                    )
                }
                initializeRenderer(document.file)
                loadPage(0)
            } else {
                _uiState.update { it.copy(isLoading = false, error = application.getString(R.string.error_document_not_found)) }
            }
        }
    }

    private suspend fun initializeRenderer(file: File) {
        withContext(Dispatchers.IO) {
            try {
                fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(fileDescriptor!!)
            } catch (e: Exception) {
                logger.error("Failed to initialize renderer", e)
                _uiState.update { it.copy(error = "PDF 렌더링 실패: ${e.message}") }
            }
        }
    }

    private data class RenderedPage(
        val bitmap: Bitmap,
        val pdfWidth: Int,
        val pdfHeight: Int
    )

    private fun loadPage(pageIndex: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val renderedPage = renderPage(pageIndex)
            
            if (renderedPage != null) {
                _uiState.update {
                    it.copy(
                        currentPage = pageIndex,
                        currentPageBitmap = renderedPage.bitmap,
                        pdfPageWidth = renderedPage.pdfWidth,
                        pdfPageHeight = renderedPage.pdfHeight,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun renderPage(pageIndex: Int): RenderedPage? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        if (pageIndex >= renderer.pageCount) return@withContext null

        val page = renderer.openPage(pageIndex)
        
        try {
            // Calculate bitmap size to fit within max dimension while maintaining aspect ratio
            // This ensures the entire page is always renderable without cutting off
            val maxDimension = 2048
            val pageAspectRatio = page.width.toFloat() / page.height.toFloat()
            
            val width: Int
            val height: Int
            
            if (page.width > page.height) {
                // Wider than tall: limit width
                width = minOf(maxDimension, page.width * 2)
                height = (width / pageAspectRatio).toInt()
            } else {
                // Taller than wide: limit height
                height = minOf(maxDimension, page.height * 2)
                width = (height * pageAspectRatio).toInt()
            }
            
            val bitmap = createBitmap(width, height)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            
            RenderedPage(bitmap, page.width, page.height)
        } finally {
            page.close()
        }
    }

    fun toggleMaskingMode() {
        _uiState.update { it.copy(isMaskingMode = !it.isMaskingMode) }
    }

    fun toggleColorPickingMode() {
        _uiState.update { it.copy(isColorPickingMode = !it.isColorPickingMode) }
    }

    fun setMaskColor(color: Int) {
        _uiState.update { it.copy(currentMaskColor = color) }
    }

    fun addRedaction(x: Float, y: Float, width: Float, height: Float) {
        val currentState = _uiState.value
        val newRedaction = RedactionMask(
            id = UUID.randomUUID().toString(),
            pageIndex = currentState.currentPage,
            x = x,
            y = y,
            width = width,
            height = height,
            color = currentState.currentMaskColor
        )
        val updatedRedactions = currentState.redactions + newRedaction
        _uiState.update { it.copy(redactions = updatedRedactions) }
        saveRedactions()
    }

    fun removeRedaction(id: String) {
        val updatedRedactions = _uiState.value.redactions.filter { it.id != id }
        _uiState.update { it.copy(redactions = updatedRedactions) }
        saveRedactions()
    }

    private fun saveRedactions() {
        val currentState = _uiState.value
        currentState.document?.let { doc ->
            viewModelScope.launch {
                saveRedactionsUseCase(doc.id, currentState.redactions)
            }
        }
    }

    fun nextPage() {
        val currentState = _uiState.value
        if (currentState.currentPage < currentState.pageCount - 1) {
            loadPage(currentState.currentPage + 1)
        }
    }

    fun prevPage() {
        val currentState = _uiState.value
        if (currentState.currentPage > 0) {
            loadPage(currentState.currentPage - 1)
        }
    }

    fun goToPage(pageNumber: Int) {
        val currentState = _uiState.value
        val pageIndex = pageNumber - 1 // Convert 1-based to 0-based index
        if (pageIndex in 0 until currentState.pageCount) {
            loadPage(pageIndex)
        }
    }


    // 저장 로직 (IO 작업)


    // 정리 작업 (파일/DB 삭제)
    private suspend fun cleanupProject(document: PdfDocument) {
        if (document.file.exists()) {
            document.file.delete()
        }
        deletePdfDocumentUseCase(document.id)
    }

    fun detectPiiInCurrentPage() {
        val currentState = _uiState.value
        currentState.document?.let { doc ->
            viewModelScope.launch {
                logger.info("User triggered PII detection on current page ${currentState.currentPage}")
                _uiState.update { it.copy(isDetecting = true) }
                detectPiiUseCase.detectInPage(doc.file, currentState.currentPage)
                    .onSuccess { detectedList ->
                        // Add only PII from current page
                        val filteredExisting = currentState.detectedPii.filter { it.pageIndex != currentState.currentPage }
                        _uiState.update {
                            it.copy(
                                detectedPii = filteredExisting + detectedList,
                                isDetecting = false,
                                piiDetectionCount = detectedList.size
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isDetecting = false, error = e.message) }
                    }
            }
        }
    }

    fun detectPiiInAllPages() {
        val currentState = _uiState.value
        currentState.document?.let { doc ->
            viewModelScope.launch {
                logger.info("User triggered PII detection on all pages")
                _uiState.update { it.copy(isDetecting = true) }
                detectPiiUseCase.detectInAllPages(doc.file)
                    .onSuccess { detectedList ->
                        _uiState.update {
                            it.copy(
                                detectedPii = detectedList,
                                isDetecting = false,
                                piiDetectionCount = detectedList.size
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isDetecting = false, error = e.message) }
                    }
            }
        }
    }

    fun convertDetectedPiiToMask(pii: DetectedPii) {
        val mask = RedactionMask(
            id = UUID.randomUUID().toString(),
            pageIndex = pii.pageIndex,
            x = pii.x,
            y = pii.y,
            width = pii.width,
            height = pii.height,
            type = pii.type,
            color = _uiState.value.currentMaskColor
        )
        val updatedRedactions = _uiState.value.redactions + mask
        val updatedDetectedPii = _uiState.value.detectedPii.filter { it != pii }
        _uiState.update {
            it.copy(
                redactions = updatedRedactions,
                detectedPii = updatedDetectedPii
            )
        }
        saveRedactions()
    }

    fun removeDetectedPii(pii: DetectedPii) {
        val updatedDetectedPii = _uiState.value.detectedPii.filter { it != pii }
        _uiState.update { it.copy(detectedPii = updatedDetectedPii) }
    }

    private fun closeRenderer() {
        try {
            pdfRenderer?.close()
            pdfRenderer = null
            fileDescriptor?.close()
            fileDescriptor = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun performRedaction() {
        val currentState = _uiState.value
        currentState.document?.let { doc ->
            viewModelScope.launch {
                logger.info("Initiating Redaction Process")
                _uiState.update { it.copy(isLoading = true) }
                
                try {
                    val tempFile = File.createTempFile("redacted_", ".pdf", application.cacheDir)
                    val result = saveRedactedPdfUseCase(doc.file, currentState.redactions, tempFile)
                    
                    result.onSuccess { redactedFile ->
                        logger.info("Redaction successful")
                         _uiState.update { 
                             it.copy(
                                 isLoading = false, 
                                 tempRedactedFile = redactedFile
                             ) 
                         }
                         _sideEffect.send(EditorSideEffect.OpenSaveLauncher)
                    }.onFailure { e ->
                        logger.error("Redaction failed", e)
                        _uiState.update { it.copy(isLoading = false, error = "Redaction Failed: ${e.message}") }
                    }
                } catch (e: Exception) {
                    logger.error("Redaction process error", e)
                    _uiState.update { it.copy(isLoading = false, error = "Redaction Error: ${e.message}") }
                }
            }
        }
    }

    fun saveFinalDocument(uri: Uri) {
        val currentState = _uiState.value
        val tempFile = currentState.tempRedactedFile ?: return
        val document = currentState.document ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            executeSaveFile(tempFile, uri)
                .onSuccess {
                    logger.info("File saved successfully to $uri")
                    cleanupTempRedactedFile()
                    closeRenderer()
                    cleanupProject(document)
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            saveSuccess = true
                        ) 
                    }
                }
                .onFailure { e ->
                    logger.error("Failed to save file", e)
                    _uiState.update { it.copy(isLoading = false, error = "Failed to save file: ${e.message}") }
                }
        }
    }

    private fun cleanupTempRedactedFile() {
        _uiState.value.tempRedactedFile?.let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
        _uiState.update { it.copy(tempRedactedFile = null) }
    }

    private suspend fun executeSaveFile(sourceFile: File, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
             application.contentResolver.openOutputStream(uri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw Exception(application.getString(R.string.error_failed_to_open_output_stream))
            Unit
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(error = null) }
    }

    fun consumePiiDetectionResult() {
        _uiState.update { it.copy(piiDetectionCount = null) }
    }

    override fun onCleared() {
        super.onCleared()
        closeRenderer()
    }
}

