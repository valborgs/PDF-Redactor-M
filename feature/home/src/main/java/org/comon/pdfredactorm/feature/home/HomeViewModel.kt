package org.comon.pdfredactorm.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.comon.pdfredactorm.core.model.PdfDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import org.comon.pdfredactorm.core.domain.usecase.pdf.GetRecentProjectsUseCase
import org.comon.pdfredactorm.core.domain.usecase.pdf.DeletePdfDocumentUseCase
import org.comon.pdfredactorm.core.domain.usecase.settings.GetFirstLaunchUseCase
import org.comon.pdfredactorm.core.domain.usecase.settings.SetFirstLaunchUseCase
import org.comon.pdfredactorm.core.domain.usecase.settings.GetProStatusUseCase
import org.comon.pdfredactorm.core.domain.usecase.settings.GetAppUuidUseCase
import org.comon.pdfredactorm.core.domain.usecase.ValidateCodeUseCase
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.comon.pdfredactorm.core.common.logger.Logger
import org.comon.pdfredactorm.core.domain.usecase.LoadPdfUseCase

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRecentProjectsUseCase: GetRecentProjectsUseCase,
    private val deletePdfDocumentUseCase: DeletePdfDocumentUseCase,
    private val loadPdfUseCase: LoadPdfUseCase,
    private val validateCodeUseCase: ValidateCodeUseCase,
    private val getFirstLaunchUseCase: GetFirstLaunchUseCase,
    private val setFirstLaunchUseCase: SetFirstLaunchUseCase,
    private val getProStatusUseCase: GetProStatusUseCase,
    private val getAppUuidUseCase: GetAppUuidUseCase,
    private val logger: Logger
) : ViewModel() {

    private val _showHelpDialog = MutableStateFlow(false)
    val showHelpDialog: StateFlow<Boolean> = _showHelpDialog.asStateFlow()

    init {
        checkFirstLaunch()
    }

    private fun checkFirstLaunch() {
        viewModelScope.launch {
            getFirstLaunchUseCase().collect { isFirstLaunch ->
                if (isFirstLaunch) {
                    _showHelpDialog.value = true
                    setFirstLaunchUseCase(false)
                }
            }
        }
    }

    fun dismissHelpDialog() {
        _showHelpDialog.value = false
    }

    // Pro Activation Logic
    val isProEnabled: StateFlow<Boolean> = getProStatusUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _validationEvent = MutableSharedFlow<Result<String>>()
    val validationEvent: SharedFlow<Result<String>> = _validationEvent.asSharedFlow()

    fun validateCode(email: String, code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val uuid = getAppUuidUseCase()
                val result = validateCodeUseCase(email, code, uuid)
                result.onSuccess {
                    _validationEvent.emit(Result.success("Pro 기능이 활성화되었습니다."))
                }.onFailure { e ->
                    _validationEvent.emit(Result.failure(e))
                }
            } catch (e: Exception) {
                _validationEvent.emit(Result.failure(e))
            } finally {
                _isLoading.value = false
            }
        }
    }

    val recentProjects: StateFlow<List<PdfDocument>> = getRecentProjectsUseCase()
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
            deletePdfDocumentUseCase(pdfId)
        }
    }
}
