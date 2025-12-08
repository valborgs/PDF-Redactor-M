package org.comon.pdfredactorm.core.data.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.comon.pdfredactorm.core.data.repository.LocalPdfRepositoryImpl
import org.comon.pdfredactorm.core.data.repository.RedeemRepositoryImpl
import org.comon.pdfredactorm.core.data.repository.RemoteRedactionRepositoryImpl
import org.comon.pdfredactorm.core.data.repository.SettingsRepositoryImpl
import org.comon.pdfredactorm.core.domain.repository.LocalPdfRepository
import org.comon.pdfredactorm.core.domain.repository.RedeemRepository
import org.comon.pdfredactorm.core.domain.repository.RemoteRedactionRepository
import org.comon.pdfredactorm.core.domain.repository.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindLocalPdfRepository(
        impl: LocalPdfRepositoryImpl
    ): LocalPdfRepository

    @Binds
    @Singleton
    abstract fun bindRemoteRedactionRepository(
        impl: RemoteRedactionRepositoryImpl
    ): RemoteRedactionRepository

    @Binds
    @Singleton
    abstract fun bindRedeemRepository(
        impl: RedeemRepositoryImpl
    ): RedeemRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindLogger(
        impl: org.comon.pdfredactorm.core.data.logger.AndroidLogger
    ): org.comon.pdfredactorm.core.common.logger.Logger

    companion object {
        @Provides
        @Singleton
        fun providePdfBoxInitializer(@ApplicationContext context: Context): PdfBoxInitializer {
            com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
            return PdfBoxInitializer
        }
    }
}

// Marker object for PDFBox initialization
object PdfBoxInitializer
