package org.comon.pdfredactorm.core.model

data class DetectedPii(
    val text: String,
    val type: RedactionType,
    val pageIndex: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)
