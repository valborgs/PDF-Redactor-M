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
import org.comon.pdfredactorm.BuildConfig
import org.comon.pdfredactorm.data.local.AppDatabase
import org.comon.pdfredactorm.data.local.dao.ProjectDao
import org.comon.pdfredactorm.data.local.dao.RedactionDao
import org.comon.pdfredactorm.data.logger.AndroidLogger
import org.comon.pdfredactorm.data.repository.LocalPdfRepositoryImpl
import org.comon.pdfredactorm.data.repository.RemoteRedactionRepositoryImpl
import org.comon.pdfredactorm.domain.logger.Logger
import org.comon.pdfredactorm.domain.repository.LocalPdfRepository
import org.comon.pdfredactorm.domain.repository.RemoteRedactionRepository
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.comon.pdfredactorm.data.remote.RedactionApi
import retrofit2.converter.kotlinx.serialization.asConverterFactory

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
    fun provideLocalPdfRepository(
        @ApplicationContext context: Context,
        projectDao: ProjectDao,
        redactionDao: RedactionDao,
        logger: Logger
    ): LocalPdfRepository {
        // Initialize PDFBox
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
        
        return LocalPdfRepositoryImpl(projectDao, redactionDao, logger)
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

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json { ignoreUnknownKeys = true }
    }

    @Provides
    @Singleton
    fun provideRetrofit(json: Json): retrofit2.Retrofit {
        val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
        }
        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return retrofit2.Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideRedactionApi(retrofit: retrofit2.Retrofit): RedactionApi {
        return retrofit.create(RedactionApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRemoteRedactionRepository(api: RedactionApi, json: Json): RemoteRedactionRepository {
        return RemoteRedactionRepositoryImpl(api, json)
    }
}
