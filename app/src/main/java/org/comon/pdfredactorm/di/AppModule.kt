package org.comon.pdfredactorm.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import org.comon.pdfredactorm.BuildConfig
import org.comon.pdfredactorm.core.common.logger.AndroidLogger
import org.comon.pdfredactorm.core.common.logger.Logger
import org.comon.pdfredactorm.core.database.dao.ProjectDao
import org.comon.pdfredactorm.core.database.dao.RedactionDao
import org.comon.pdfredactorm.core.network.api.RedactionApi
import org.comon.pdfredactorm.core.network.api.RedeemApi
import org.comon.pdfredactorm.data.repository.LocalPdfRepositoryImpl
import org.comon.pdfredactorm.data.repository.RedeemRepositoryImpl
import org.comon.pdfredactorm.data.repository.RemoteRedactionRepositoryImpl
import org.comon.pdfredactorm.data.repository.SettingsRepositoryImpl
import org.comon.pdfredactorm.domain.repository.LocalPdfRepository
import org.comon.pdfredactorm.domain.repository.RedeemRepository
import org.comon.pdfredactorm.domain.repository.RemoteRedactionRepository
import org.comon.pdfredactorm.domain.repository.SettingsRepository
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

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
        return AndroidLogger(BuildConfig.DEBUG)
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): android.content.SharedPreferences {
        return context.getSharedPreferences("pdf_redactor_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideRemoteRedactionRepository(api: RedactionApi, json: Json): RemoteRedactionRepository {
        return RemoteRedactionRepositoryImpl(api, json)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository {
        return SettingsRepositoryImpl(dataStore)
    }

    @Provides
    @Singleton
    fun provideRedeemRepository(api: RedeemApi, settingsRepository: SettingsRepository): RedeemRepository {
        return RedeemRepositoryImpl(api, settingsRepository)
    }
}
