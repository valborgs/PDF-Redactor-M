package org.comon.pdfredactorm.core.domain.usecase.settings

import kotlinx.coroutines.flow.Flow
import org.comon.pdfredactorm.core.domain.repository.SettingsRepository
import javax.inject.Inject

class GetProStatusUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<Boolean> = settingsRepository.isProEnabled
}
