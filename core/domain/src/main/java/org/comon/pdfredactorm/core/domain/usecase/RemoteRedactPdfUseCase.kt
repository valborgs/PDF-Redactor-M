package org.comon.pdfredactorm.core.domain.usecase

import org.comon.pdfredactorm.core.model.RedactionMask
import org.comon.pdfredactorm.core.domain.repository.RemoteRedactionRepository
import java.io.File
import javax.inject.Inject

class RemoteRedactPdfUseCase @Inject constructor(
    private val repository: RemoteRedactionRepository
) {
    suspend operator fun invoke(file: File, redactions: List<RedactionMask>): Result<File> {
        return repository.redactPdf(file, redactions)
    }
}
