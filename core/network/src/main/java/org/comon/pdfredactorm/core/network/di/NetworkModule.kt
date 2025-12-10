package org.comon.pdfredactorm.core.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.comon.pdfredactorm.core.common.logger.Logger
import org.comon.pdfredactorm.core.network.api.RedactionApi
import org.comon.pdfredactorm.core.network.api.RedeemApi
import org.comon.pdfredactorm.core.network.BuildConfig
import org.comon.pdfredactorm.core.network.interceptor.ApiKeyInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RedactOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RedeemOkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json { ignoreUnknownKeys = true }
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    @RedactOkHttpClient
    fun provideRedactOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        logger: Logger
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(ApiKeyInterceptor("X-Redact-Api-Key", BuildConfig.REDACT_API_KEY, logger))
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @RedeemOkHttpClient
    fun provideRedeemOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        logger: Logger
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(ApiKeyInterceptor("X-Redeem-Api-Key", BuildConfig.REDEEM_API_KEY, logger))
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRedactionApi(
        @RedactOkHttpClient okHttpClient: OkHttpClient,
        json: Json
    ): RedactionApi {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RedactionApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRedeemApi(
        @RedeemOkHttpClient okHttpClient: OkHttpClient,
        json: Json
    ): RedeemApi {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RedeemApi::class.java)
    }
}
