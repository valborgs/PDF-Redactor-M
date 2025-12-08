package org.comon.pdfredactorm.core.domain.usecase

import org.comon.pdfredactorm.core.model.PdfDocument
import org.comon.pdfredactorm.core.domain.repository.LocalPdfRepository
import java.io.File
import javax.inject.Inject

class LoadPdfUseCase @Inject constructor(
    private val repository: LocalPdfRepository
) {
    suspend operator fun invoke(file: File): Result<PdfDocument> {
        return try {
            val document = repository.loadPdf(file)
            repository.saveProject(document) // Auto-save to history
            Result.success(document)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
