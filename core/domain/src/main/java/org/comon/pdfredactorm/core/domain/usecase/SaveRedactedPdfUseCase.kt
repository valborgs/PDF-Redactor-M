package org.comon.pdfredactorm.core.domain.usecase

import kotlinx.coroutines.flow.first
import org.comon.pdfredactorm.core.model.RedactionMask
import org.comon.pdfredactorm.core.domain.repository.LocalPdfRepository
import org.comon.pdfredactorm.core.domain.repository.RemoteRedactionRepository
import org.comon.pdfredactorm.core.domain.repository.SettingsRepository
import java.io.File
import javax.inject.Inject

class SaveRedactedPdfUseCase @Inject constructor(
    private val localRepository: LocalPdfRepository,
    private val remoteRepository: RemoteRedactionRepository,
    private val settingsRepository: SettingsRepository
) {
    /**
     * @param forceLocal true이면 Pro 상태와 관계없이 로컬 마스킹을 강제 실행 (네트워크 fallback 시 사용)
     */
    suspend operator fun invoke(
        originalFile: File,
        redactions: List<RedactionMask>,
        outputFile: File,
        forceLocal: Boolean = false
    ): Result<File> {
        val isPro = settingsRepository.isProEnabled.first()
        
        return if (isPro && !forceLocal) {
            val result = remoteRepository.redactPdf(originalFile, redactions)
            result.map { redactedFile ->
                // Copy the remote result (temp file) to the target output file
                redactedFile.copyTo(outputFile, overwrite = true)
                outputFile
            }
        } else {
            localRepository.saveRedactedPdf(originalFile, redactions, outputFile).map { 
                outputFile 
            }
        }
    }
}
