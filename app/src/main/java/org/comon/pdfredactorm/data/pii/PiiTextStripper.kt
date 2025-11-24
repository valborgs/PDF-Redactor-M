package org.comon.pdfredactorm.data.pii

import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition

class PiiTextStripper : PDFTextStripper() {
    data class TextWithPosition(
        val text: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    )

    val textPositions = mutableListOf<TextWithPosition>()

    override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
        super.writeString(text, textPositions)

        if (textPositions.isNotEmpty()) {
            val firstPosition = textPositions.first()
            val lastPosition = textPositions.last()

            // Calculate bounding box for the entire text
            val x = firstPosition.x
            val y = firstPosition.y
            val width = lastPosition.endX - firstPosition.x
            val height = firstPosition.height

            this.textPositions.add(
                TextWithPosition(
                    text = text,
                    x = x,
                    y = y,
                    width = width,
                    height = height
                )
            )
        }
    }

    fun reset() {
        textPositions.clear()
    }
}
