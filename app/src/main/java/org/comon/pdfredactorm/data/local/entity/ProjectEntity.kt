package org.comon.pdfredactorm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val filePath: String,
    val fileName: String,
    val pageCount: Int,
    val lastModified: Long,
    val thumbnailUri: String?
)
