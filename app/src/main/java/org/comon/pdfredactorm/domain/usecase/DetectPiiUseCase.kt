package org.comon.pdfredactorm.domain.usecase

import org.comon.pdfredactorm.domain.model.DetectedPii
import org.comon.pdfredactorm.domain.model.RedactionMask
import org.comon.pdfredactorm.domain.repository.PdfRepository
import java.io.File
import java.util.UUID
import javax.inject.Inject

class DetectPiiUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    suspend fun detectInPage(file: File, pageIndex: Int): Result<List<DetectedPii>> {
        return try {
            val detectedPii = repository.detectPii(file, pageIndex)
            Result.success(detectedPii)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun detectInAllPages(file: File): Result<List<DetectedPii>> {
        return try {
            val detectedPii = repository.detectPiiInAllPages(file)
            Result.success(detectedPii)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun convertToRedactionMasks(detectedPiiList: List<DetectedPii>): List<RedactionMask> {
        return detectedPiiList.map { pii ->
            RedactionMask(
                id = UUID.randomUUID().toString(),
                pageIndex = pii.pageIndex,
                x = pii.x,
                y = pii.y,
                width = pii.width,
                height = pii.height,
                type = pii.type
            )
        }
    }
}
