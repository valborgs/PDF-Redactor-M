package org.comon.pdfredactorm.core.domain.repository

/**
 * 앱 설정값을 제공하는 인터페이스
 * 
 * Clean Architecture의 Domain Layer에 위치하여 플랫폼 독립적인 설정 제공을 지원합니다.
 * 실제 구현체(예: Firebase Remote Config)는 app 모듈에 배치되며, DI를 통해 주입됩니다.
 */
interface ConfigProvider {
    
    /**
     * Remote Config 값을 fetch하고 activate합니다.
     * 
     * @return fetch 및 activate 성공 여부
     */
    suspend fun fetchAndActivate(): Boolean
    
    /**
     * API Base URL을 반환합니다.
     * Remote Config에서 값을 가져오며, 없으면 기본값을 반환합니다.
     */
    fun getApiBaseUrl(): String
    
    /**
     * Coffee Chat URL을 반환합니다.
     * Remote Config에서 값을 가져오며, 없으면 기본값을 반환합니다.
     */
    fun getCoffeeChatUrl(): String
}
