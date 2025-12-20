package org.comon.pdfredactorm.core.domain.usecase

import org.comon.pdfredactorm.core.model.RedactionMask
import org.comon.pdfredactorm.core.domain.repository.RemoteRedactionRepository
import org.comon.pdfredactorm.core.domain.repository.SettingsRepository
import org.comon.pdfredactorm.core.model.ApiException
import org.comon.pdfredactorm.core.model.RedactionErrorCode
import java.io.File
import javax.inject.Inject

/**
 * Pro 마스킹 UseCase
 * 
 * 원격 마스킹 API를 호출합니다.
 * JWT 토큰은 OkHttp Interceptor에서 자동으로 처리됩니다.
 */
class RemoteRedactPdfUseCase @Inject constructor(
    private val repository: RemoteRedactionRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(file: File, redactions: List<RedactionMask>): Result<File> {
        val result = repository.redactPdf(file, redactions)
        
        result.onFailure { e ->
            if (e is ApiException && e.redactionErrorCode == RedactionErrorCode.DEVICE_MISMATCH) {
                // 비즈니스 정책: 기기 미매칭 에러 발생 시 현재 기기의 Pro 권한 즉시 회수
                settingsRepository.setProEnabled(false)
                settingsRepository.clearJwtToken()
            }
        }
        
        return result
    }
}
