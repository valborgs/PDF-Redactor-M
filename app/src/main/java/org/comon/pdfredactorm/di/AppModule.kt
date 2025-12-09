package org.comon.pdfredactorm.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.comon.pdfredactorm.BuildConfig
import org.comon.pdfredactorm.core.data.di.RedactApiKey
import org.comon.pdfredactorm.core.data.di.RedeemApiKey
import org.comon.pdfredactorm.core.data.logger.IsDebugBuild

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @IsDebugBuild
    fun provideIsDebugBuild(): Boolean {
        return BuildConfig.DEBUG
    }


    @Provides
    @RedeemApiKey
    fun provideRedeemApiKey(): String {
        return BuildConfig.REDEEM_API_KEY
    }

    @Provides
    @RedactApiKey
    fun provideRedactApiKey(): String {
        return BuildConfig.REDACT_API_KEY
    }
}
