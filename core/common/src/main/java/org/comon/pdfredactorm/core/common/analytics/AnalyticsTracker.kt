package org.comon.pdfredactorm.core.common.analytics

/**
 * Analytics 추적 인터페이스
 * 
 * Clean Architecture의 Common Layer에 위치하여 플랫폼 독립적인 Analytics 추적을 제공합니다.
 * 실제 구현체는 Data Layer에 배치되며, DI를 통해 주입됩니다.
 */
interface AnalyticsTracker {
    /**
     * 이벤트 로깅
     * 
     * @param eventName 이벤트 이름 (예: "pdf_opened", "mask_saved")
     * @param params 이벤트 파라미터 (선택사항)
     */
    fun logEvent(eventName: String, params: Map<String, Any>? = null)
    
    /**
     * 화면 조회 로깅
     * 
     * @param screenName 화면 이름
     * @param screenClass 화면 클래스 이름 (선택사항)
     */
    fun logScreenView(screenName: String, screenClass: String? = null)
    
    /**
     * 사용자 속성 설정
     * 
     * @param name 속성 이름
     * @param value 속성 값
     */
    fun setUserProperty(name: String, value: String)
}
