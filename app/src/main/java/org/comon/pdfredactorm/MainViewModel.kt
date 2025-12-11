package org.comon.pdfredactorm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.comon.pdfredactorm.core.common.logger.Logger
import org.comon.pdfredactorm.core.domain.usecase.settings.GetProStatusUseCase
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getProStatusUseCase: GetProStatusUseCase,
    private val logger: Logger
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _isProEnabled = MutableStateFlow(false)
    val isProEnabled = _isProEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                // Check Pro status on startup
                _isProEnabled.value = getProStatusUseCase().first()
                logger.debug("App initialized, Pro status: ${_isProEnabled.value}")
            } catch (e: Exception) {
                logger.error("Failed to initialize app settings", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
