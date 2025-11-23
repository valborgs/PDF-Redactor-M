package org.comon.pdfredactorm.domain.usecase

import org.comon.pdfredactorm.domain.model.RedactionMask
import org.comon.pdfredactorm.domain.repository.PdfRepository
import javax.inject.Inject

class GetRedactionsUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    suspend operator fun invoke(pdfId: String): List<RedactionMask> {
        return repository.getRedactions(pdfId)
    }
}
