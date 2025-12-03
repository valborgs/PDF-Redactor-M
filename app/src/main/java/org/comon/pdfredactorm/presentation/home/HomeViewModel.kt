package org.comon.pdfredactorm.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.comon.pdfredactorm.domain.model.PdfDocument
import org.comon.pdfredactorm.domain.usecase.LoadPdfUseCase
import org.comon.pdfredactorm.domain.logger.Logger
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import androidx.core.content.edit
import org.comon.pdfredactorm.domain.repository.LocalPdfRepository

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LocalPdfRepository,
    private val loadPdfUseCase: LoadPdfUseCase,
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

