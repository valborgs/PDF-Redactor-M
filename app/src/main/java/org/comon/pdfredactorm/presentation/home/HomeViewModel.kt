package org.comon.pdfredactorm.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.comon.pdfredactorm.domain.model.PdfDocument
import org.comon.pdfredactorm.domain.repository.PdfRepository
import org.comon.pdfredactorm.domain.usecase.LoadPdfUseCase
import org.comon.pdfredactorm.domain.logger.Logger
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PdfRepository,
    private val loadPdfUseCase: LoadPdfUseCase,
    private val logger: Logger
) : ViewModel() {
val recentProjects: StateFlow<List<PdfDocument>> = repository.getRecentProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun loadPdf(file: File, onLoaded: (String) -> Unit) {
        viewModelScope.launch {
            logger.info("User selected PDF file: ${file.name}")
            val result = loadPdfUseCase(file)
            result.onSuccess { document ->
                onLoaded(document.id)
            }.onFailure {
                logger.warning("PDF load failed")
            }
        }
    }
    
    fun deleteProject(pdfId: String) {
        viewModelScope.launch {
            logger.info("User deleted project: $pdfId")
            repository.deleteProject(pdfId)
        }
    }
}

