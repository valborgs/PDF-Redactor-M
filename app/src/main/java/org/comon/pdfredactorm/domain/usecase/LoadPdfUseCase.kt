package org.comon.pdfredactorm.domain.usecase

import org.comon.pdfredactorm.domain.model.PdfDocument
import org.comon.pdfredactorm.domain.repository.PdfRepository
import java.io.File
import javax.inject.Inject

class LoadPdfUseCase @Inject constructor(
    private val repository: PdfRepository
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
