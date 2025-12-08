package org.comon.pdfredactorm.core.domain.usecase.pdf

import org.comon.pdfredactorm.core.model.PdfOutlineItem
import org.comon.pdfredactorm.core.domain.repository.LocalPdfRepository
import java.io.File
import javax.inject.Inject

class GetPdfOutlineUseCase @Inject constructor(
    private val repository: LocalPdfRepository
) {
    suspend operator fun invoke(file: File): List<PdfOutlineItem> {
        return repository.getOutline(file)
    }
}
