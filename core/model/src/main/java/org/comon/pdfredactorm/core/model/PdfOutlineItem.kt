package org.comon.pdfredactorm.core.model

data class PdfOutlineItem(
    val title: String,
    val pageIndex: Int,
    val children: List<PdfOutlineItem> = emptyList()
)
