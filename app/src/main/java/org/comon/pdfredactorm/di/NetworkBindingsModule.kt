package org.comon.pdfredactorm.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.comon.pdfredactorm.BuildConfig
import org.comon.pdfredactorm.core.network.di.BaseUrl

@Module
@InstallIn(SingletonComponent::class)
object NetworkBindingsModule {

    @Provides
    @BaseUrl
    fun provideBaseUrl(): String {
        return BuildConfig.API_BASE_URL
    }
}
