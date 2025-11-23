package org.comon.pdfredactorm.domain.usecase

import org.comon.pdfredactorm.domain.model.RedactionMask
import org.comon.pdfredactorm.domain.repository.PdfRepository
import javax.inject.Inject

class SaveRedactionsUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    suspend operator fun invoke(pdfId: String, redactions: List<RedactionMask>) {
        repository.saveRedactions(pdfId, redactions)
    }
}
