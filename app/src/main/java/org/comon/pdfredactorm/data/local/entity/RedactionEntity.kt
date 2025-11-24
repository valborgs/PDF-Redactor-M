package org.comon.pdfredactorm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.comon.pdfredactorm.domain.model.RedactionType

@Entity(tableName = "redactions")
data class RedactionEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val pageIndex: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val type: RedactionType,
    val color: Int = 0xFF000000.toInt() // Default: Black
)
