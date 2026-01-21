package org.comon.pdfredactorm.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.comon.pdfredactorm.BuildConfig
import org.comon.pdfredactorm.config.RemoteConfigManager
import org.comon.pdfredactorm.core.domain.repository.ConfigProvider
import org.comon.pdfredactorm.core.common.logger.Logger
import org.comon.pdfredactorm.core.network.di.ApiBaseUrl
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Coffee Chat URL을 식별하기 위한 Qualifier
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CoffeeChatUrl

/**
 * Remote Config 관련 의존성을 제공하는 Hilt Module
 * 
 * - ConfigProvider 인터페이스를 RemoteConfigManager로 바인딩
 * - URL 값을 Qualifier로 제공
 * 
 * 주의: ConfigProvider.fetchAndActivate()가 Splash Screen에서 완료된 후에
 * URL 값들이 사용되어야 합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object RemoteConfigModule {

    /**
     * ConfigProvider 구현체(RemoteConfigManager) 제공
     * 
     * 생성자에 필요한 기본값들을 BuildConfig에서 주입합니다.
     */
    @Provides
    @Singleton
    fun provideConfigProvider(
        logger: Logger
    ): ConfigProvider {
        return RemoteConfigManager(
            defaultApiBaseUrl = BuildConfig.DEFAULT_API_BASE_URL,
            defaultCoffeeChatUrl = BuildConfig.DEFAULT_COFFEE_CHAT_URL,
            isDebug = BuildConfig.DEBUG,
            logger = logger
        )
    }

    @Provides
    @Singleton
    @ApiBaseUrl
    fun provideApiBaseUrl(configProvider: ConfigProvider): String {
        return configProvider.getApiBaseUrl()
    }

    @Provides
    @Singleton
    @CoffeeChatUrl
    fun provideCoffeeChatUrl(configProvider: ConfigProvider): String {
        return configProvider.getCoffeeChatUrl()
    }
}


