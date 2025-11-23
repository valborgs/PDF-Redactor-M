package org.comon.pdfredactorm.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.comon.pdfredactorm.data.local.AppDatabase
import org.comon.pdfredactorm.data.local.dao.ProjectDao
import org.comon.pdfredactorm.data.local.dao.RedactionDao
import org.comon.pdfredactorm.data.repository.PdfRepositoryImpl
import org.comon.pdfredactorm.domain.repository.PdfRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pdf_redactor_db"
        ).build()
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
        redactionDao: RedactionDao
    ): PdfRepository {
        // Initialize PDFBox
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
        
        return PdfRepositoryImpl(context, projectDao, redactionDao)
    }
}
