package org.comon.pdfredactorm.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.comon.pdfredactorm.BuildConfig
import org.comon.pdfredactorm.core.data.di.RedeemApiKey
import org.comon.pdfredactorm.core.data.logger.IsDebugBuild
import javax.inject.Singleton

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
}
