package org.comon.pdfredactorm.presentation.editor

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.comon.pdfredactorm.domain.model.DetectedPii
import org.comon.pdfredactorm.domain.model.PdfDocument
import org.comon.pdfredactorm.domain.model.PdfOutlineItem
import org.comon.pdfredactorm.domain.model.RedactionMask
import org.comon.pdfredactorm.domain.repository.PdfRepository
import org.comon.pdfredactorm.domain.usecase.DetectPiiUseCase
import org.comon.pdfredactorm.domain.usecase.GetRedactionsUseCase
import org.comon.pdfredactorm.domain.usecase.SaveRedactedPdfUseCase
import org.comon.pdfredactorm.domain.usecase.SaveRedactionsUseCase
import java.io.File
import java.util.UUID
import javax.inject.Inject
import androidx.core.graphics.createBitmap
import android.app.Application
import android.net.Uri
import org.comon.pdfredactorm.R
import org.comon.pdfredactorm.domain.logger.Logger

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
    val currentMaskColor: Int = 0xFF000000.toInt() // Default: Black
)



@HiltViewModel
class EditorViewModel @Inject constructor(
    private val application: Application,
    private val repository: PdfRepository,
    private val getRedactionsUseCase: GetRedactionsUseCase,
    private val saveRedactionsUseCase: SaveRedactionsUseCase,
    private val saveRedactedPdfUseCase: SaveRedactedPdfUseCase,
    private val detectPiiUseCase: DetectPiiUseCase,
    private val logger: Logger
) : ViewModel() {
private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    fun loadPdf(pdfId: String) {
        viewModelScope.launch {
            logger.info("User opened PDF document: $pdfId")
            _uiState.update { it.copy(isLoading = true) }
            val document = repository.getProject(pdfId)
            if (document != null) {
                val redactions = getRedactionsUseCase(pdfId)
                val outline = repository.getOutline(document.file)
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
                e.printStackTrace()
            }
        }
    }

    private fun loadPage(pageIndex: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withContext(Dispatchers.IO) {
                val renderer = pdfRenderer ?: return@withContext
                if (pageIndex >= renderer.pageCount) return@withContext

                val page = renderer.openPage(pageIndex)
                // Render at a reasonable density (e.g., screen density * 2 for zoom)
                // For simplicity, using a fixed width of 2048 or similar, maintaining aspect ratio
                val width = 2048
                val height = (width.toFloat() / page.width * page.height).toInt()
                
                val bitmap = createBitmap(width, height)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                val pdfWidth = page.width
                val pdfHeight = page.height
                page.close()

                _uiState.update {
                    it.copy(
                        currentPage = pageIndex,
                        currentPageBitmap = bitmap,
                        pdfPageWidth = pdfWidth,
                        pdfPageHeight = pdfHeight,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleMaskingMode() {
        _uiState.update { it.copy(isMaskingMode = !it.isMaskingMode) }
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

    fun savePdf(uri: Uri) {
        val currentState = _uiState.value
        currentState.document?.let { doc ->
            viewModelScope.launch {
                logger.info("User initiated PDF save")
                _uiState.update { it.copy(isLoading = true) }
                try {
                    application.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val result = saveRedactedPdfUseCase(doc.file, currentState.redactions, outputStream)
                        result.onSuccess {
                            logger.info("PDF save completed successfully")
                            // Close renderer to release file lock
                            closeRenderer()
                            
                            // Delete temp file
                            if (doc.file.exists()) {
                                doc.file.delete()
                            }
                            
                            // Delete project from DB
                            repository.deleteProject(doc.id)
                            
                            _uiState.update { it.copy(isLoading = false, saveSuccess = true) }
                        }.onFailure { e ->
                            logger.warning("PDF save failed")
                            _uiState.update { it.copy(isLoading = false, error = e.message) }
                        }
                    } ?: run {
                        _uiState.update { it.copy(isLoading = false, error = application.getString(R.string.error_failed_to_open_output_stream)) }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            }
        }
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
                                isDetecting = false
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
                                isDetecting = false
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
            type = pii.type
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

    override fun onCleared() {
        super.onCleared()
        closeRenderer()
    }
}

