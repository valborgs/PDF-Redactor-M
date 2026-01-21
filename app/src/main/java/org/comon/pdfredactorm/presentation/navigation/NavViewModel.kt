package org.comon.pdfredactorm.presentation.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.comon.pdfredactorm.core.domain.repository.ConfigProvider
import javax.inject.Inject

/**
 * Navigation을 위한 ViewModel
 * ConfigProvider에서 URL 값을 제공합니다.
 */
@HiltViewModel
class NavViewModel @Inject constructor(
    private val configProvider: ConfigProvider
) : ViewModel() {

    /**
     * Coffee Chat URL을 반환합니다.
     * Remote Config에서 가져온 값 또는 기본값을 반환합니다.
     */
    fun getCoffeeChatUrl(): String {
        return configProvider.getCoffeeChatUrl()
    }
}

