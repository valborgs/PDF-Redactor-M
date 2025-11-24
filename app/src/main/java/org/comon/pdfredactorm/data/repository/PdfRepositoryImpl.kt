package org.comon.pdfredactorm.data.repository

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.comon.pdfredactorm.data.local.dao.ProjectDao
import org.comon.pdfredactorm.data.local.dao.RedactionDao
import org.comon.pdfredactorm.data.local.entity.ProjectEntity
import org.comon.pdfredactorm.data.local.entity.RedactionEntity
import org.comon.pdfredactorm.data.pii.PiiPatterns
import org.comon.pdfredactorm.data.pii.PiiTextStripper
import org.comon.pdfredactorm.domain.model.DetectedPii
import org.comon.pdfredactorm.domain.model.PdfDocument
import org.comon.pdfredactorm.domain.model.RedactionMask
import org.comon.pdfredactorm.domain.repository.PdfRepository
import java.io.File
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject

class PdfRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectDao: ProjectDao,
    private val redactionDao: RedactionDao
) : PdfRepository {

    override suspend fun loadPdf(file: File): PdfDocument {
        return withContext(Dispatchers.IO) {
            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fileDescriptor)
            val pageCount = renderer.pageCount
            renderer.close()
            fileDescriptor.close()

            PdfDocument(
                id = UUID.randomUUID().toString(), // Generate a new ID for the session/project
                file = file,
                fileName = file.name,
                pageCount = pageCount,
                lastModified = System.currentTimeMillis()
            )
        }
    }

    override suspend fun getPdfPageCount(file: File): Int {
        return withContext(Dispatchers.IO) {
            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fileDescriptor)
            val count = renderer.pageCount
            renderer.close()
            fileDescriptor.close()
            count
        }
    }

    override suspend fun saveRedactedPdf(
        originalFile: File,
        redactions: List<RedactionMask>,
        outputStream: OutputStream
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val document = PDDocument.load(originalFile)
                
                redactions.groupBy { it.pageIndex }.forEach { (pageIndex, masks) ->
                    if (pageIndex < document.numberOfPages) {
                        val page = document.getPage(pageIndex)
                        val contentStream = PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)
                        
                        contentStream.setNonStrokingColor(0, 0, 0)
                        
                        for (mask in masks) {
                            // PDF coordinates usually start from bottom-left, but Android/Canvas is top-left.
                            // We need to handle coordinate conversion if the UI sends top-left based coordinates.
                            // For now, assuming the UI sends coordinates relative to the PDF page size correctly.
                            // If UI sends 0..1 normalized coordinates, we multiply by page width/height.
                            // If UI sends absolute coordinates, we use them directly.
                            // Let's assume absolute coordinates for now matching the page size.
                            
                            // Draw rectangle
                            // In PDFBox, addRect(x, y, width, height)
                            // Note: y is from bottom in PDF. If we get y from top, we need: pageHeight - y - height
                            val pageHeight = page.mediaBox.height
                            val pdfY = pageHeight - mask.y - mask.height
                            
                            contentStream.addRect(mask.x, pdfY, mask.width, mask.height)
                            contentStream.fill()
                        }
                        contentStream.close()
                    }
                }
                
                document.save(outputStream)
                document.close()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun getRecentProjects(): Flow<List<PdfDocument>> {
        return projectDao.getAllProjects().map { entities ->
            entities.map { entity ->
                PdfDocument(
                    id = entity.id,
                    file = File(entity.filePath),
                    fileName = entity.fileName,
                    pageCount = entity.pageCount,
                    lastModified = entity.lastModified,
                    thumbnailUri = entity.thumbnailUri
                )
            }
        }
    }

    override suspend fun saveProject(pdfDocument: PdfDocument) {
        projectDao.insertProject(
            ProjectEntity(
                id = pdfDocument.id,
                filePath = pdfDocument.file.absolutePath,
                fileName = pdfDocument.fileName,
                pageCount = pdfDocument.pageCount,
                lastModified = pdfDocument.lastModified,
                thumbnailUri = pdfDocument.thumbnailUri
            )
        )
    }

    override suspend fun saveRedactions(pdfId: String, redactions: List<RedactionMask>) {
        val entities = redactions.map { mask ->
            RedactionEntity(
                id = mask.id,
                projectId = pdfId,
                pageIndex = mask.pageIndex,
                x = mask.x,
                y = mask.y,
                width = mask.width,
                height = mask.height,
                type = mask.type
            )
        }
        redactionDao.insertRedactions(entities)
    }

    override suspend fun getRedactions(pdfId: String): List<RedactionMask> {
        return redactionDao.getRedactionsForProject(pdfId).map { entity ->
            RedactionMask(
                id = entity.id,
                pageIndex = entity.pageIndex,
                x = entity.x,
                y = entity.y,
                width = entity.width,
                height = entity.height,
                type = entity.type
            )
        }
    }

    override suspend fun deleteProject(pdfId: String) {
        projectDao.deleteProject(pdfId)
        redactionDao.deleteRedactionsForProject(pdfId)
    }

    override suspend fun getProject(pdfId: String): PdfDocument? {
        return projectDao.getProjectById(pdfId)?.let { entity ->
            PdfDocument(
                id = entity.id,
                file = File(entity.filePath),
                fileName = entity.fileName,
                pageCount = entity.pageCount,
                lastModified = entity.lastModified,
                thumbnailUri = entity.thumbnailUri
            )
        }
    }

    override suspend fun detectPii(file: File, pageIndex: Int): List<DetectedPii> {
        return withContext(Dispatchers.IO) {
            val detectedPiiList = mutableListOf<DetectedPii>()

            try {
                val document = PDDocument.load(file)
                if (pageIndex >= document.numberOfPages) {
                    document.close()
                    return@withContext emptyList()
                }

                val page = document.getPage(pageIndex)
                val pageHeight = page.mediaBox.height

                val stripper = PiiTextStripper()
                stripper.startPage = pageIndex + 1
                stripper.endPage = pageIndex + 1

                // Extract text with positions
                stripper.getText(document)

                // Analyze each text fragment for PII
                stripper.textPositions.forEach { textWithPos ->
                    val matches = PiiPatterns.detectAll(textWithPos.text)

                    matches.forEach { (type, match) ->
                        // Calculate position for the matched text
                        // This is a simplified approach - in production, you'd need more precise text positioning
                        val matchedText = match.value
                        val textRatio = match.range.first.toFloat() / textWithPos.text.length

                        // Estimate the position of the matched text
                        val estimatedX = textWithPos.x + (textWithPos.width * textRatio)
                        val estimatedWidth = textWithPos.width * (matchedText.length.toFloat() / textWithPos.text.length)

                        // Convert from PDF coordinates (bottom-left) to UI coordinates (top-left)
                        val uiY = pageHeight - textWithPos.y - textWithPos.height

                        detectedPiiList.add(
                            DetectedPii(
                                text = matchedText,
                                type = type,
                                pageIndex = pageIndex,
                                x = estimatedX,
                                y = uiY,
                                width = estimatedWidth,
                                height = textWithPos.height
                            )
                        )
                    }
                }

                document.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            detectedPiiList
        }
    }

    override suspend fun detectPiiInAllPages(file: File): List<DetectedPii> {
        return withContext(Dispatchers.IO) {
            val allDetectedPii = mutableListOf<DetectedPii>()

            try {
                val pageCount = getPdfPageCount(file)

                for (pageIndex in 0 until pageCount) {
                    val piiOnPage = detectPii(file, pageIndex)
                    allDetectedPii.addAll(piiOnPage)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            allDetectedPii
        }
    }
}
