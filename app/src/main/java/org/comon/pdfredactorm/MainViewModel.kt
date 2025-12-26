package org.comon.pdfredactorm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.comon.pdfredactorm.core.common.logger.Logger
import org.comon.pdfredactorm.core.domain.repository.SettingsRepository
import org.comon.pdfredactorm.core.domain.usecase.CheckDeviceMismatchUseCase
import org.comon.pdfredactorm.core.domain.usecase.settings.GetProStatusUseCase
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getProStatusUseCase: GetProStatusUseCase,
    private val checkDeviceMismatchUseCase: CheckDeviceMismatchUseCase,
    private val settingsRepository: SettingsRepository,
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
                val isEnabled = getProStatusUseCase().first()
                _isProEnabled.value = isEnabled
                logger.debug("App initialized, cached Pro status: $isEnabled")

                if (isEnabled) {
                    // 서버 실시간 정합성 체크
                    val proInfo = settingsRepository.getProInfo()
                    if (proInfo != null) {
                        checkDeviceMismatch(proInfo.redeemCode, proInfo.uuid)
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to initialize app settings", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun checkDeviceMismatch(code: String, uuid: String) {
        checkDeviceMismatchUseCase(code, uuid).onSuccess { isMismatch ->
            if (isMismatch) {
                logger.warning("Pro device mismatch detected. Deactivating Pro features.")
                settingsRepository.setProEnabled(false)
                settingsRepository.clearJwtToken()
                _isProEnabled.value = false
            } else {
                logger.debug("Pro device consistency verified.")
            }
        }.onFailure { e ->
            logger.error("Failed to verify Pro device consistency", e)
            // 네트워크 오류 등 확인 실패 시에는 사용자 경험을 위해 기존 상태 유지
        }
    }
}
