package org.comon.pdfredactorm.core.domain.usecase

import kotlinx.coroutines.flow.first
import org.comon.pdfredactorm.core.model.RedactionMask
import org.comon.pdfredactorm.core.domain.repository.LocalPdfRepository
import org.comon.pdfredactorm.core.domain.repository.RemoteRedactionRepository
import org.comon.pdfredactorm.core.domain.repository.SettingsRepository
import java.io.File
import java.io.OutputStream
import javax.inject.Inject

class SaveRedactedPdfUseCase @Inject constructor(
    private val localRepository: LocalPdfRepository,
    private val remoteRepository: RemoteRedactionRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(originalFile: File, redactions: List<RedactionMask>, outputFile: File): Result<File> {
        val isPro = settingsRepository.isProEnabled.first()
        
        return if (isPro) {
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
