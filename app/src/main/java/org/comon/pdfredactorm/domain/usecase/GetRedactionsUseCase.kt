package org.comon.pdfredactorm.domain.usecase

import org.comon.pdfredactorm.domain.model.RedactionMask
import org.comon.pdfredactorm.domain.repository.LocalPdfRepository
import javax.inject.Inject

class GetRedactionsUseCase @Inject constructor(
    private val repository: LocalPdfRepository
) {
    suspend operator fun invoke(pdfId: String): List<RedactionMask> {
        return repository.getRedactions(pdfId)
    }
}
