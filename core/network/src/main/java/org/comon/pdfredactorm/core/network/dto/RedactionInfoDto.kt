package org.comon.pdfredactorm.core.network.dto

import kotlinx.serialization.Serializable
import org.comon.pdfredactorm.core.model.RedactionMask

@Serializable
data class RedactionInfoDto(
    val pageIndex: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val color: Int = -16777216 // Default black
)

fun RedactionMask.toDto(): RedactionInfoDto {
    return RedactionInfoDto(
        pageIndex = this.pageIndex,
        x = this.x,
        y = this.y,
        width = this.width,
        height = this.height,
        color = this.color
    )
}
