package org.comon.pdfredactorm.core.domain.usecase

import org.comon.pdfredactorm.core.model.RedactionMask
import org.comon.pdfredactorm.core.domain.repository.RemoteRedactionRepository
import java.io.File
import javax.inject.Inject

/**
 * Pro 마스킹 UseCase
 * 
 * 원격 마스킹 API를 호출합니다.
 * JWT 토큰은 OkHttp Interceptor에서 자동으로 처리됩니다.
 */
class RemoteRedactPdfUseCase @Inject constructor(
    private val repository: RemoteRedactionRepository
) {
    suspend operator fun invoke(file: File, redactions: List<RedactionMask>): Result<File> {
        return repository.redactPdf(file, redactions)
    }
}
