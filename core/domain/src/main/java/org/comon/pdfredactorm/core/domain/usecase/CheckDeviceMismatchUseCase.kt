package org.comon.pdfredactorm.core.domain.usecase

import org.comon.pdfredactorm.core.domain.repository.RedeemRepository
import javax.inject.Inject

/**
 * 리딤 코드 기기 일치 여부 확인 UseCase
 */
class CheckDeviceMismatchUseCase @Inject constructor(
    private val redeemRepository: RedeemRepository
) {
    suspend operator fun invoke(code: String, deviceId: String): Result<Boolean> {
        return redeemRepository.checkDeviceMismatch(code, deviceId)
    }
}
