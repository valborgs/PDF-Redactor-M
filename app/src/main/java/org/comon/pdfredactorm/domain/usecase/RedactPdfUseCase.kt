package org.comon.pdfredactorm.domain.usecase

import org.comon.pdfredactorm.domain.model.RedactionInfo
import org.comon.pdfredactorm.domain.repository.RedactionRepository
import java.io.File
import javax.inject.Inject

class RedactPdfUseCase @Inject constructor(
    private val repository: RedactionRepository
) {
    suspend operator fun invoke(file: File, redactions: List<RedactionInfo>): Result<File> {
        return repository.redactPdf(file, redactions)
    }
}
