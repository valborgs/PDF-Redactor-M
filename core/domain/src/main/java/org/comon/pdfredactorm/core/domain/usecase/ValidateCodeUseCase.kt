package org.comon.pdfredactorm.core.domain.usecase

import org.comon.pdfredactorm.core.domain.repository.RedeemRepository
import javax.inject.Inject

class ValidateCodeUseCase @Inject constructor(
    private val repository: RedeemRepository
) {
    suspend operator fun invoke(email: String, code: String, uuid: String): Result<Boolean> {
        return repository.validateCode(email, code, uuid)
    }
}
