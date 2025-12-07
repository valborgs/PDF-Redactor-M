package org.comon.pdfredactorm.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.comon.pdfredactorm.core.model.PdfDocument
import org.comon.pdfredactorm.domain.usecase.LoadPdfUseCase
import org.comon.pdfredactorm.core.common.logger.Logger
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import androidx.core.content.edit
import org.comon.pdfredactorm.domain.repository.LocalPdfRepository

import org.comon.pdfredactorm.domain.usecase.ValidateCodeUseCase
import org.comon.pdfredactorm.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LocalPdfRepository,
    private val loadPdfUseCase: LoadPdfUseCase,
    private val validateCodeUseCase: ValidateCodeUseCase,
    private val settingsRepository: SettingsRepository,
    private val logger: Logger,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    companion object {
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }

    private val _showHelpDialog = MutableStateFlow(false)
    val showHelpDialog: StateFlow<Boolean> = _showHelpDialog.asStateFlow()

    init {
        checkFirstLaunch()
    }

    private fun checkFirstLaunch() {
        val isFirstLaunch = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
        if (isFirstLaunch) {
            _showHelpDialog.value = true
            sharedPreferences.edit { putBoolean(KEY_FIRST_LAUNCH, false) }
        }
    }

    fun dismissHelpDialog() {
        _showHelpDialog.value = false
    }

    // Pro Activation Logic
    val isProEnabled: StateFlow<Boolean> = settingsRepository.isProEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _validationEvent = MutableSharedFlow<Result<String>>()
    val validationEvent: SharedFlow<Result<String>> = _validationEvent.asSharedFlow()

    fun validateCode(email: String, code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val uuid = settingsRepository.getAppUuid()
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

