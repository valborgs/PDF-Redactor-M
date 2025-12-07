package org.comon.pdfredactorm.core.common.logger

/**
 * Logger 인터페이스
 * 
 * Clean Architecture의 Domain Layer에 위치하여 플랫폼 독립적인 로깅을 제공합니다.
 * 실제 구현체는 Data Layer에 배치되며, DI를 통해 주입됩니다.
 * 
 * 모든 로그는 "PDFLogger" 태그를 사용합니다.
 */
interface Logger {
    /**
     * 디버그 레벨 로그
     * 개발 중 상세한 정보를 기록할 때 사용
     * 
     * @param message 로그 메시지
     */
    fun debug(message: String)
    
    /**
     * 정보 레벨 로그
     * 일반적인 정보성 메시지를 기록할 때 사용
     * 
     * @param message 로그 메시지
     */
    fun info(message: String)
    
    /**
     * 경고 레벨 로그
     * 잠재적 문제나 주의가 필요한 상황을 기록할 때 사용
     * 
     * @param message 로그 메시지
     */
    fun warning(message: String)
    
    /**
     * 에러 레벨 로그
     * 에러 발생 시 예외 정보와 함께 기록
     * 
     * @param message 로그 메시지
     * @param throwable 발생한 예외 (선택사항)
     */
    fun error(message: String, throwable: Throwable? = null)
}
