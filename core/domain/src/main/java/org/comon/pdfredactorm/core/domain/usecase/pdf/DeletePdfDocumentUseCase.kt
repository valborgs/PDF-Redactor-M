package org.comon.pdfredactorm.core.domain.usecase.pdf

import org.comon.pdfredactorm.core.domain.repository.LocalPdfRepository
import javax.inject.Inject

class DeletePdfDocumentUseCase @Inject constructor(
    private val repository: LocalPdfRepository
) {
    suspend operator fun invoke(pdfId: String) {
        repository.deleteProject(pdfId)
    }
}
