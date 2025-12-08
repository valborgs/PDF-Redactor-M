package org.comon.pdfredactorm.core.domain.usecase.pdf

import kotlinx.coroutines.flow.Flow
import org.comon.pdfredactorm.core.model.PdfDocument
import org.comon.pdfredactorm.core.domain.repository.LocalPdfRepository
import javax.inject.Inject

class GetRecentProjectsUseCase @Inject constructor(
    private val repository: LocalPdfRepository
) {
    operator fun invoke(): Flow<List<PdfDocument>> {
        return repository.getRecentProjects()
    }
}
