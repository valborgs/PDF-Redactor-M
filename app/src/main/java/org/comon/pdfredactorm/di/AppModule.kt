package org.comon.pdfredactorm.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.comon.pdfredactorm.data.local.AppDatabase
import org.comon.pdfredactorm.data.local.dao.ProjectDao
import org.comon.pdfredactorm.data.local.dao.RedactionDao
import org.comon.pdfredactorm.data.logger.AndroidLogger
import org.comon.pdfredactorm.data.repository.PdfRepositoryImpl
import org.comon.pdfredactorm.domain.logger.Logger
import org.comon.pdfredactorm.domain.repository.PdfRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add color column to redactions table with default black color
            db.execSQL("ALTER TABLE redactions ADD COLUMN color INTEGER NOT NULL DEFAULT -16777216")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pdf_redactor_db"
        ).addMigrations(MIGRATION_1_2).build()
    }

    @Provides
    fun provideProjectDao(database: AppDatabase): ProjectDao {
        return database.projectDao()
    }

    @Provides
    fun provideRedactionDao(database: AppDatabase): RedactionDao {
        return database.redactionDao()
    }

    @Provides
    @Singleton
    fun providePdfRepository(
        @ApplicationContext context: Context,
        projectDao: ProjectDao,
        redactionDao: RedactionDao,
        logger: Logger
    ): PdfRepository {
        // Initialize PDFBox
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
        
        return PdfRepositoryImpl(projectDao, redactionDao, logger)
    }
    
    @Provides
    @Singleton
    fun provideLogger(): Logger {
        return AndroidLogger()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): android.content.SharedPreferences {
        return context.getSharedPreferences("pdf_redactor_prefs", Context.MODE_PRIVATE)
    }
}
