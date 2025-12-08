package org.comon.pdfredactorm.core.domain.usecase.pdf

import org.comon.pdfredactorm.core.model.PdfDocument
import org.comon.pdfredactorm.core.domain.repository.LocalPdfRepository
import javax.inject.Inject

class GetPdfDocumentUseCase @Inject constructor(
    private val repository: LocalPdfRepository
) {
    suspend operator fun invoke(pdfId: String): PdfDocument? {
        return repository.getProject(pdfId)
    }
}
