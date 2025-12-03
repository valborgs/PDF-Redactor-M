package org.comon.pdfredactorm.domain.model

data class RedactionInfo(
    val pageIndex: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val color: Int = -16777216 // Default black
)
