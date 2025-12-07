package org.comon.pdfredactorm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.comon.pdfredactorm.domain.repository.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                // Check Pro status on startup
                // We just need to ensure the DataStore is ready or any necessary checks are done.
                // For now, simply reading the value to ensure initialization.
                settingsRepository.isProEnabled.first()
            } catch (e: Exception) {
                // Log error or handle fallback defaults
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
