package org.comon.pdfredactorm.core.network.auth

/**
 * JWT 토큰 제공자 인터페이스
 * 
 * Network 모듈에서 JWT 토큰을 가져오기 위한 인터페이스입니다.
 * Data 모듈에서 이 인터페이스를 구현하여 순환 의존성을 방지합니다.
 */
interface JwtTokenProvider {
    suspend fun getJwtToken(): String?
}
