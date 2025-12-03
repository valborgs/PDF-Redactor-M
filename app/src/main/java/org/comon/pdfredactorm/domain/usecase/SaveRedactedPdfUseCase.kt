package org.comon.pdfredactorm.domain.usecase

import org.comon.pdfredactorm.domain.model.RedactionMask
import org.comon.pdfredactorm.domain.repository.LocalPdfRepository
import java.io.File
import java.io.OutputStream
import javax.inject.Inject

class SaveRedactedPdfUseCase @Inject constructor(
    private val repository: LocalPdfRepository
) {
    suspend operator fun invoke(originalFile: File, redactions: List<RedactionMask>, outputStream: OutputStream): Result<Unit> {
        return repository.saveRedactedPdf(originalFile, redactions, outputStream)
    }
}
