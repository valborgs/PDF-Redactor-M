package org.comon.pdfredactorm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import org.comon.pdfredactorm.data.local.dao.ProjectDao
import org.comon.pdfredactorm.data.local.dao.RedactionDao
import org.comon.pdfredactorm.data.local.entity.ProjectEntity
import org.comon.pdfredactorm.data.local.entity.RedactionEntity

@Database(entities = [ProjectEntity::class, RedactionEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun redactionDao(): RedactionDao
}
