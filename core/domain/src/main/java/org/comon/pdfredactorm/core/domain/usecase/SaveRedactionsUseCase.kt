package org.comon.pdfredactorm.core.domain.usecase

import org.comon.pdfredactorm.core.model.RedactionMask
import org.comon.pdfredactorm.core.domain.repository.LocalPdfRepository
import javax.inject.Inject

class SaveRedactionsUseCase @Inject constructor(
    private val repository: LocalPdfRepository
) {
    suspend operator fun invoke(pdfId: String, redactions: List<RedactionMask>) {
        repository.saveRedactions(pdfId, redactions)
    }
}
