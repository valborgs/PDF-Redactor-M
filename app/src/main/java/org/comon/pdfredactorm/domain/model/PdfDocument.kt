package org.comon.pdfredactorm.domain.model

import java.io.File

data class PdfDocument(
    val id: String,
    val file: File,
    val fileName: String,
    val pageCount: Int,
    val lastModified: Long,
    val thumbnailUri: String? = null
)
