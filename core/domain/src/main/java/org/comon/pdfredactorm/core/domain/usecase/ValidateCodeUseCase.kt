package org.comon.pdfredactorm.core.domain.usecase

import org.comon.pdfredactorm.core.domain.repository.RedeemRepository
import org.comon.pdfredactorm.core.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * 리딤 코드 검증 UseCase
 * 
 * 리딤 코드를 검증하고, 검증 성공 시 Pro 기능을 활성화합니다.
 */
class ValidateCodeUseCase @Inject constructor(
    private val redeemRepository: RedeemRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(email: String, code: String, uuid: String): Result<Boolean> {
        return redeemRepository.validateCode(email, code, uuid)
            .onSuccess {
                // 검증 성공 시 Pro 기능 활성화
                settingsRepository.setProEnabled(true)
            }
    }
}
