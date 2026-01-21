package org.comon.pdfredactorm.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import org.comon.pdfredactorm.core.domain.repository.ConfigProvider
import org.comon.pdfredactorm.core.common.logger.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Firebase Remote Config 기반 ConfigProvider 구현체
 * 
 * API_BASE_URL과 COFFEE_CHAT_URL을 원격으로 관리할 수 있도록 합니다.
 * Splash Screen에서 fetchAndActivate()를 호출하여 최신 값을 가져온 후 앱이 시작됩니다.
 * 
 * @param defaultApiBaseUrl 기본 API Base URL (Remote Config 실패 시 사용)
 * @param defaultCoffeeChatUrl 기본 Coffee Chat URL (Remote Config 실패 시 사용)
 * @param isDebug 디버그 모드 여부 (fetch 간격 설정에 사용)
 * @param logger 로깅을 위한 Logger
 */
@Singleton
class RemoteConfigManager @Inject constructor(
    private val defaultApiBaseUrl: String,
    private val defaultCoffeeChatUrl: String,
    private val isDebug: Boolean,
    private val logger: Logger
) : ConfigProvider {
    
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    companion object {
        const val KEY_API_BASE_URL = "api_base_url"
        const val KEY_COFFEE_CHAT_URL = "coffee_chat_url"
        
        // 개발 중에는 fetch 간격을 짧게 (0초), 프로덕션에서는 12시간
        private const val FETCH_INTERVAL_DEBUG = 0L
        private const val FETCH_INTERVAL_RELEASE = 43200L // 12시간
    }

    init {
        // Remote Config 설정
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(
                if (isDebug) FETCH_INTERVAL_DEBUG else FETCH_INTERVAL_RELEASE
            )
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)

        // 기본값 설정
        val defaults = mapOf(
            KEY_API_BASE_URL to defaultApiBaseUrl,
            KEY_COFFEE_CHAT_URL to defaultCoffeeChatUrl
        )
        remoteConfig.setDefaultsAsync(defaults)
    }

    /**
     * Remote Config 값을 fetch하고 activate합니다.
     * 
     * @return fetch 및 activate 성공 여부
     */
    override suspend fun fetchAndActivate(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        logger.debug("Remote Config fetched and activated successfully")
                    } else {
                        logger.warning("Remote Config fetch failed, using default/cached values")
                    }
                    continuation.resume(task.isSuccessful)
                }
        }
    }

    /**
     * API Base URL을 반환합니다.
     * Remote Config에서 값을 가져오며, 없으면 기본값을 반환합니다.
     */
    override fun getApiBaseUrl(): String {
        val url = remoteConfig.getString(KEY_API_BASE_URL)
        return url.ifEmpty { defaultApiBaseUrl }
    }

    /**
     * Coffee Chat URL을 반환합니다.
     * Remote Config에서 값을 가져오며, 없으면 기본값을 반환합니다.
     */
    override fun getCoffeeChatUrl(): String {
        val url = remoteConfig.getString(KEY_COFFEE_CHAT_URL)
        return url.ifEmpty { defaultCoffeeChatUrl }
    }
}


