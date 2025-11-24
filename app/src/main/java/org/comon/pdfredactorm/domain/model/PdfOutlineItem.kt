package org.comon.pdfredactorm.domain.model

data class PdfOutlineItem(
    val title: String,
    val pageIndex: Int,
    val children: List<PdfOutlineItem> = emptyList()
)
