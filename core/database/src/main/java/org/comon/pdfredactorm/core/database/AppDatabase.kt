package org.comon.pdfredactorm.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import org.comon.pdfredactorm.core.database.dao.ProjectDao
import org.comon.pdfredactorm.core.database.dao.RedactionDao
import org.comon.pdfredactorm.core.database.entity.ProjectEntity
import org.comon.pdfredactorm.core.database.entity.RedactionEntity

@Database(entities = [ProjectEntity::class, RedactionEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun redactionDao(): RedactionDao
}
