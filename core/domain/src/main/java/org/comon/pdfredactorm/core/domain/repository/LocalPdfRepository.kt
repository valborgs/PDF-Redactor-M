package org.comon.pdfredactorm.core.domain.repository

import kotlinx.coroutines.flow.Flow
import org.comon.pdfredactorm.core.model.DetectedPii
import org.comon.pdfredactorm.core.model.PdfDocument
import org.comon.pdfredactorm.core.model.PdfOutlineItem
import org.comon.pdfredactorm.core.model.RedactionMask
import java.io.File

interface LocalPdfRepository {
    suspend fun loadPdf(file: File): PdfDocument
    suspend fun getPdfPageCount(file: File): Int
    suspend fun saveRedactedPdf(originalFile: File, redactions: List<RedactionMask>, outputFile: File): Result<Unit>

    // PII Detection
    suspend fun detectPii(file: File, pageIndex: Int): List<DetectedPii>
    suspend fun detectPiiInAllPages(file: File): List<DetectedPii>

    // Outline/Bookmarks
    suspend fun getOutline(file: File): List<PdfOutlineItem>

    // History/Project management
    fun getRecentProjects(): Flow<List<PdfDocument>>
    suspend fun saveProject(pdfDocument: PdfDocument)
    suspend fun saveRedactions(pdfId: String, redactions: List<RedactionMask>)
    suspend fun getRedactions(pdfId: String): List<RedactionMask>
    suspend fun deleteProject(pdfId: String)
    suspend fun getProject(pdfId: String): PdfDocument?
}
