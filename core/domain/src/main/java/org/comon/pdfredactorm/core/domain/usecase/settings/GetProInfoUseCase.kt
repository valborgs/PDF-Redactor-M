package org.comon.pdfredactorm.core.domain.usecase.settings

import org.comon.pdfredactorm.core.domain.repository.SettingsRepository
import org.comon.pdfredactorm.core.model.ProInfo
import javax.inject.Inject

class GetProInfoUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): ProInfo? {
        return settingsRepository.getProInfo()
    }
}
