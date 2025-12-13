package org.comon.pdfredactorm.core.domain.usecase

import org.comon.pdfredactorm.core.domain.repository.RedeemRepository
import org.comon.pdfredactorm.core.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * 리딤 코드 검증 UseCase
 * 
 * 리딤 코드를 검증하고, 검증 성공 시 JWT 토큰을 저장하고 Pro 기능을 활성화합니다.
 */
class ValidateCodeUseCase @Inject constructor(
    private val redeemRepository: RedeemRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(email: String, code: String): Result<Boolean> {
        // device_id로 app_uuid 사용
        val deviceId = settingsRepository.getAppUuid()
        
        return redeemRepository.validateCode(email, code, deviceId)
            .map { jwtToken ->
                // JWT 토큰 저장
                settingsRepository.saveJwtToken(jwtToken)
                // Pro 기능 활성화
                settingsRepository.setProEnabled(true)
                true
            }
    }
}
