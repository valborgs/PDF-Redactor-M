package org.comon.pdfredactorm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.comon.pdfredactorm.core.domain.repository.ConfigProvider
import org.comon.pdfredactorm.core.common.logger.Logger
import org.comon.pdfredactorm.core.domain.usecase.settings.GetProStatusUseCase
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getProStatusUseCase: GetProStatusUseCase,
    private val configProvider: ConfigProvider,
    private val logger: Logger
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _isProEnabled = MutableStateFlow(false)
    val isProEnabled = _isProEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                // 1. Remote Config fetch (성공/실패 상관없이 진행 - 실패 시 기본값 사용)
                val configSuccess = configProvider.fetchAndActivate()
                logger.debug("Remote Config fetched: $configSuccess")
                
                // 2. Pro 상태 확인
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

