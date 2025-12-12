package org.comon.pdfredactorm.feature.editor

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
import org.comon.pdfredactorm.core.model.RedactionMask
import org.comon.pdfredactorm.core.domain.usecase.pdf.GetPdfDocumentUseCase
import org.comon.pdfredactorm.core.domain.usecase.pdf.GetPdfOutlineUseCase
import org.comon.pdfredactorm.core.domain.usecase.pdf.DeletePdfDocumentUseCase
import org.comon.pdfredactorm.core.domain.usecase.DetectPiiUseCase
import org.comon.pdfredactorm.core.domain.usecase.GetRedactionsUseCase
import org.comon.pdfredactorm.core.domain.usecase.SaveRedactedPdfUseCase
import org.comon.pdfredactorm.core.domain.usecase.SaveRedactionsUseCase
import org.comon.pdfredactorm.core.domain.usecase.CheckNetworkUseCase
import java.io.File
import java.util.UUID
import javax.inject.Inject
import androidx.core.graphics.createBitmap
import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import org.comon.pdfredactorm.core.common.logger.Logger
import org.comon.pdfredactorm.core.domain.usecase.settings.GetProStatusUseCase

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
    private val checkNetworkUseCase: CheckNetworkUseCase,
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

    // ==================== MVI Entry Point ====================

    /**
     * MVI 패턴의 단일 진입점.
     * 모든 사용자 행동(Intent)은 이 함수를 통해 처리됩니다.
     */
    fun handleIntent(intent: EditorIntent) {
        logger.info("Processing Intent: ${intent::class.simpleName}")
        when (intent) {
            is EditorIntent.LoadPdf -> loadPdf(intent.pdfId)
            is EditorIntent.NextPage -> nextPage()
            is EditorIntent.PrevPage -> prevPage()
            is EditorIntent.GoToPage -> goToPage(intent.pageNumber)
            is EditorIntent.ToggleMaskingMode -> toggleMaskingMode()
            is EditorIntent.ToggleColorPickingMode -> toggleColorPickingMode()
            is EditorIntent.SetMaskColor -> setMaskColor(intent.color)
            is EditorIntent.AddRedaction -> addRedaction(intent.x, intent.y, intent.width, intent.height)
            is EditorIntent.RemoveRedaction -> removeRedaction(intent.id)
            is EditorIntent.PerformRedaction -> performRedaction()
            is EditorIntent.SaveFinalDocument -> saveFinalDocument(intent.uri)
            is EditorIntent.DetectPiiInCurrentPage -> detectPiiInCurrentPage()
            is EditorIntent.DetectPiiInAllPages -> detectPiiInAllPages()
            is EditorIntent.ConvertPiiToMask -> convertDetectedPiiToMask(intent.pii)
            is EditorIntent.RemoveDetectedPii -> removeDetectedPii(intent.pii)
            is EditorIntent.ConsumePiiDetectionResult -> consumePiiDetectionResult()
            is EditorIntent.ProceedWithLocalRedaction -> proceedWithLocalRedaction()
        }
    }

    // ==================== Internal Logic (Private) ====================

    private fun loadPdf(pdfId: String) {
        viewModelScope.launch {
            logger.info("User opened PDF document: $pdfId")
            _uiState.update { 
                it.copy(
                    isLoading = true,
                    saveSuccess = false
                )
            }
            val document = getPdfDocumentUseCase(pdfId)
            if (document != null) {
                val redactions = getRedactionsUseCase(pdfId)
                val tableOfContents = getPdfOutlineUseCase(document.file)
                _uiState.update {
                    it.copy(
                        document = document,
                        pageCount = document.pageCount,
                        redactions = redactions,
                        tableOfContents = tableOfContents,
                        isLoading = false
                    )
                }
                initializeRenderer(document.file)
                loadPage(0)
            } else {
                _uiState.update { it.copy(isLoading = false) }
                sendSnackbar(application.getString(R.string.error_document_not_found))
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
                sendSnackbar(application.getString(R.string.error_pdf_rendering_failed, e.message ?: ""))
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
            val maxDimension = 2048
            val pageAspectRatio = page.width.toFloat() / page.height.toFloat()
            
            val width: Int
            val height: Int
            
            if (page.width > page.height) {
                width = minOf(maxDimension, page.width * 2)
                height = (width / pageAspectRatio).toInt()
            } else {
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

    private fun toggleMaskingMode() {
        val newMode = !_uiState.value.isMaskingMode
        _uiState.update { it.copy(isMaskingMode = newMode) }
        val message = if (newMode) {
            application.getString(R.string.masking_mode_enabled)
        } else {
            application.getString(R.string.masking_mode_disabled)
        }
        sendSnackbar(message)
    }

    private fun toggleColorPickingMode() {
        val newMode = !_uiState.value.isColorPickingMode
        _uiState.update { it.copy(isColorPickingMode = newMode) }
        if (newMode) {
            sendSnackbar(application.getString(R.string.color_picker_mode_enabled))
        }
    }

    private fun setMaskColor(color: Int) {
        _uiState.update { it.copy(currentMaskColor = color, isColorPickingMode = false) }
        sendSnackbar(application.getString(R.string.color_selected))
    }

    private fun addRedaction(x: Float, y: Float, width: Float, height: Float) {
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

    private fun removeRedaction(id: String) {
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

    private fun nextPage() {
        val currentState = _uiState.value
        if (currentState.currentPage < currentState.pageCount - 1) {
            loadPage(currentState.currentPage + 1)
        }
    }

    private fun prevPage() {
        val currentState = _uiState.value
        if (currentState.currentPage > 0) {
            loadPage(currentState.currentPage - 1)
        }
    }

    private fun goToPage(pageNumber: Int) {
        val currentState = _uiState.value
        val pageIndex = pageNumber - 1
        if (pageIndex in 0 until currentState.pageCount) {
            loadPage(pageIndex)
        }
    }

    private suspend fun cleanupProject(document: PdfDocument) {
        if (document.file.exists()) {
            document.file.delete()
        }
        deletePdfDocumentUseCase(document.id)
    }

    private fun detectPiiInCurrentPage() {
        val currentState = _uiState.value
        currentState.document?.let { doc ->
            viewModelScope.launch {
                logger.info("User triggered PII detection on current page ${currentState.currentPage}")
                _uiState.update { it.copy(isDetecting = true) }
                detectPiiUseCase.detectInPage(doc.file, currentState.currentPage)
                    .onSuccess { detectedList ->
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
                        _uiState.update { it.copy(isDetecting = false) }
                        sendSnackbar(e.message ?: application.getString(R.string.error_pii_detection_failed))
                    }
            }
        }
    }

    private fun detectPiiInAllPages() {
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
                        _uiState.update { it.copy(isDetecting = false) }
                        sendSnackbar(e.message ?: application.getString(R.string.error_pii_detection_failed))
                    }
            }
        }
    }

    private fun consumePiiDetectionResult() {
        _uiState.update { it.copy(piiDetectionCount = null) }
    }

    private fun convertDetectedPiiToMask(pii: DetectedPii) {
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

    private fun removeDetectedPii(pii: DetectedPii) {
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

    private fun performRedaction() {
        val currentState = _uiState.value
        currentState.document?.let { doc ->
            viewModelScope.launch {
                logger.info("Initiating Redaction Process")
                
                // Pro 기능 활성화 시 파일 용량 체크 (50MB 제한)
                val maxFileSizeBytes = 50 * 1024 * 1024L // 50MB
                if (currentState.isProEnabled && doc.file.length() > maxFileSizeBytes) {
                    logger.info("File size exceeded: ${doc.file.length()} bytes (max: $maxFileSizeBytes bytes)")
                    _sideEffect.send(EditorSideEffect.ShowFileSizeExceededDialog)
                    return@launch
                }
                
                // Pro 기능 활성화 시 네트워크 확인
                if (currentState.isProEnabled && !checkNetworkUseCase()) {
                    logger.info("Network unavailable, showing fallback dialog")
                    _sideEffect.send(EditorSideEffect.ShowNetworkFallbackDialog)
                    return@launch
                }
                
                executeRedaction(doc, currentState)
            }
        }
    }
    
    /**
     * 로컬 마스킹으로 강제 진행 (네트워크 fallback 다이얼로그에서 "진행" 선택 시)
     */
    private fun proceedWithLocalRedaction() {
        val currentState = _uiState.value
        currentState.document?.let { doc ->
            viewModelScope.launch {
                logger.info("Proceeding with local redaction (network fallback)")
                executeRedaction(doc, currentState, forceLocal = true)
            }
        }
    }
    
    private suspend fun executeRedaction(doc: PdfDocument, currentState: EditorUiState, forceLocal: Boolean = false) {
        _uiState.update { it.copy(isLoading = true) }
        
        try {
            val tempFile = File.createTempFile("redacted_", ".pdf", application.cacheDir)
            val result = saveRedactedPdfUseCase(doc.file, currentState.redactions, tempFile, forceLocal)
            
            result.onSuccess { redactedFile ->
                logger.info("Redaction successful")
                 _uiState.update { 
                     it.copy(
                         isLoading = false, 
                         tempRedactedFile = redactedFile
                     ) 
                 }
                 _sideEffect.send(EditorSideEffect.OpenSaveLauncher(isLocalFallback = forceLocal))
            }.onFailure { e ->
                logger.error("Redaction failed", e)
                _uiState.update { it.copy(isLoading = false) }
                sendSnackbar(application.getString(R.string.error_redaction_failed, e.message ?: ""))
            }
        } catch (e: Exception) {
            logger.error("Redaction process error", e)
            _uiState.update { it.copy(isLoading = false) }
            sendSnackbar(application.getString(R.string.error_redaction_process, e.message ?: ""))
        }
    }

    private fun saveFinalDocument(uri: Uri) {
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
                    _sideEffect.send(EditorSideEffect.NavigateBack)
                }
                .onFailure { e ->
                    logger.error("Failed to save file", e)
                    _uiState.update { it.copy(isLoading = false) }
                    sendSnackbar(application.getString(R.string.error_save_file_failed, e.message ?: ""))
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

    // ==================== Helper Functions ====================

    private fun sendSnackbar(message: String) {
        viewModelScope.launch {
            _sideEffect.send(EditorSideEffect.ShowSnackbar(message))
        }
    }

    override fun onCleared() {
        super.onCleared()
        closeRenderer()
    }
}
