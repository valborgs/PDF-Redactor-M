package org.comon.pdfredactorm.core.data.repository

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.comon.pdfredactorm.core.database.dao.ProjectDao
import org.comon.pdfredactorm.core.database.dao.RedactionDao
import org.comon.pdfredactorm.core.database.entity.ProjectEntity
import org.comon.pdfredactorm.core.database.entity.RedactionEntity
import org.comon.pdfredactorm.core.data.pii.PiiPatterns
import org.comon.pdfredactorm.core.data.pii.PiiTextStripper
import org.comon.pdfredactorm.core.model.DetectedPii
import org.comon.pdfredactorm.core.model.PdfDocument
import org.comon.pdfredactorm.core.model.PdfOutlineItem
import org.comon.pdfredactorm.core.model.RedactionMask
import org.comon.pdfredactorm.core.domain.repository.LocalPdfRepository
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import org.comon.pdfredactorm.core.common.logger.Logger
import java.io.File
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject

class LocalPdfRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val redactionDao: RedactionDao,
    private val logger: Logger
) : LocalPdfRepository {

    override suspend fun loadPdf(file: File): PdfDocument {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("Loading PDF: ${file.absolutePath}")
                val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fileDescriptor)
                val pageCount = renderer.pageCount
                renderer.close()
                fileDescriptor.close()

                logger.info("PDF loaded successfully: ${file.name}, pages: $pageCount")
                
                PdfDocument(
                    id = UUID.randomUUID().toString(),
                    file = file,
                    fileName = file.name,
                    pageCount = pageCount,
                    lastModified = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                logger.error("Failed to load PDF: ${file.absolutePath}", e)
                throw e
            }
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
                logger.info("Saving redacted PDF: ${originalFile.name}, redactions: ${redactions.size}")
                val document = PDDocument.load(originalFile)
                
                redactions.groupBy { it.pageIndex }.forEach { (pageIndex, masks) ->
                    if (pageIndex < document.numberOfPages) {
                        val page = document.getPage(pageIndex)
                        val contentStream = PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)

                        for (mask in masks) {
                            val red = ((mask.color shr 16) and 0xFF) / 255f
                            val green = ((mask.color shr 8) and 0xFF) / 255f
                            val blue = (mask.color and 0xFF) / 255f
                            val color = PDColor(floatArrayOf(red, green, blue), PDDeviceRGB.INSTANCE)
                            contentStream.setNonStrokingColor(color)

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
                logger.info("PDF saved successfully with ${redactions.size} redactions")
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error("Failed to save redacted PDF: ${originalFile.name}", e)
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
        redactionDao.deleteRedactionsForProject(pdfId)
        
        val entities = redactions.map { mask ->
            RedactionEntity(
                id = mask.id,
                projectId = pdfId,
                pageIndex = mask.pageIndex,
                x = mask.x,
                y = mask.y,
                width = mask.width,
                height = mask.height,
                type = mask.type,
                color = mask.color
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
                type = entity.type,
                color = entity.color
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
                logger.debug("Detecting PII on page $pageIndex of ${file.name}")
                val document = PDDocument.load(file)
                if (pageIndex >= document.numberOfPages) {
                    document.close()
                    return@withContext emptyList()
                }

                val stripper = PiiTextStripper()
                stripper.startPage = pageIndex + 1
                stripper.endPage = pageIndex + 1

                stripper.getText(document)

                stripper.textPositions.forEach { textWithPos ->
                    val matches = PiiPatterns.detectAll(textWithPos.text)

                    matches.forEach { (type, match) ->
                        val startIndex = match.range.first
                        val endIndex = match.range.last

                        if (textWithPos.textPositions.size == textWithPos.text.length && 
                            startIndex >= 0 && endIndex < textWithPos.textPositions.size) {
                            val firstChar = textWithPos.textPositions[startIndex]
                            val lastChar = textWithPos.textPositions[endIndex]

                            val x = firstChar.x
                            val width = lastChar.endX - firstChar.x
                            val height = firstChar.height
                            val y = firstChar.y - height

                            detectedPiiList.add(
                                DetectedPii(
                                    text = match.value,
                                    type = type,
                                    pageIndex = pageIndex,
                                    x = x,
                                    y = y,
                                    width = width,
                                    height = height
                                )
                            )
                        } else {
                            val matchedText = match.value
                            val textRatio = match.range.first.toFloat() / textWithPos.text.length

                            val estimatedX = textWithPos.x + (textWithPos.width * textRatio)
                            val estimatedWidth = textWithPos.width * (matchedText.length.toFloat() / textWithPos.text.length)
                            val y = textWithPos.y - textWithPos.height
                            
                            detectedPiiList.add(
                                DetectedPii(
                                    text = matchedText,
                                    type = type,
                                    pageIndex = pageIndex,
                                    x = estimatedX,
                                    y = y,
                                    width = estimatedWidth,
                                    height = textWithPos.height
                                )
                            )
                        }
                    }
                }

                document.close()
                logger.info("PII detection completed on page $pageIndex: found ${detectedPiiList.size} items")
            } catch (e: Exception) {
                logger.error("Failed to detect PII on page $pageIndex of ${file.name}", e)
            }

            detectedPiiList
        }
    }

    override suspend fun detectPiiInAllPages(file: File): List<DetectedPii> {
        return withContext(Dispatchers.IO) {
            val allDetectedPii = mutableListOf<DetectedPii>()

            try {
                val pageCount = getPdfPageCount(file)
                logger.info("Detecting PII in all pages of ${file.name}, total pages: $pageCount")

                for (pageIndex in 0 until pageCount) {
                    val piiOnPage = detectPii(file, pageIndex)
                    allDetectedPii.addAll(piiOnPage)
                }
                
                logger.info("PII detection completed for all pages: found ${allDetectedPii.size} total items")
            } catch (e: Exception) {
                logger.error("Failed to detect PII in all pages of ${file.name}", e)
            }

            allDetectedPii
        }
    }

    override suspend fun getOutline(file: File): List<PdfOutlineItem> {
        return withContext(Dispatchers.IO) {
            val outlineItems = mutableListOf<PdfOutlineItem>()

            try {
                logger.debug("Extracting outline from ${file.name}")
                val document = PDDocument.load(file)
                val documentOutline = document.documentCatalog.documentOutline

                if (documentOutline != null) {
                    processOutlineNode(documentOutline, outlineItems, document)
                }

                document.close()
                logger.info("Outline extracted: ${outlineItems.size} items")
            } catch (e: Exception) {
                logger.error("Failed to extract outline from ${file.name}", e)
            }

            outlineItems
        }
    }

    private fun processOutlineNode(
        node: PDOutlineNode,
        outlineItems: MutableList<PdfOutlineItem>,
        document: PDDocument
    ) {
        var current: PDOutlineItem? = node.firstChild
        while (current != null) {
            try {
                val title = current.title ?: "Untitled"
                val destination = current.destination
                val pageIndex = if (destination != null && destination is PDPageDestination) {
                    try {
                        val page = destination.page
                        if (page != null) {
                            document.pages.indexOf(page)
                        } else {
                            -1
                        }
                    } catch (e: Exception) {
                        -1
                    }
                } else {
                    val action = current.action
                    if (action is com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionGoTo) {
                        try {
                            val dest = action.destination
                            if (dest is PDPageDestination) {
                                val page = dest.page
                                if (page != null) {
                                    document.pages.indexOf(page)
                                } else {
                                    -1
                                }
                            } else {
                                -1
                            }
                        } catch (e: Exception) {
                            -1
                        }
                    } else {
                        -1
                    }
                }

                if (pageIndex >= 0) {
                    val children = mutableListOf<PdfOutlineItem>()
                    if (current.hasChildren()) {
                        processOutlineNode(current, children, document)
                    }

                    outlineItems.add(
                        PdfOutlineItem(
                            title = title,
                            pageIndex = pageIndex,
                            children = children
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            current = current.nextSibling
        }
    }
}
