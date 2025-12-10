package org.comon.pdfredactorm.core.domain.usecase.settings

import org.comon.pdfredactorm.core.domain.repository.SettingsRepository
import javax.inject.Inject

class GetFirstLaunchUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Boolean = settingsRepository.checkFirstLaunch()
}
