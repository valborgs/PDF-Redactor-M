package org.comon.pdfredactorm.core.network.interceptor

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import org.comon.pdfredactorm.core.common.logger.Logger

/**
 * Pro API 호출 시 JWT 토큰을 Authorization 헤더에 자동으로 추가하는 Interceptor
 * 
 * JWT 토큰이 없으면 헤더를 추가하지 않고 요청을 진행합니다.
 * 서버에서 401 응답을 받으면 Repository에서 에러를 처리합니다.
 *
 * @param jwtTokenProvider JWT 토큰을 제공하는 suspend 함수
 * @param logger 로깅을 위한 Logger 인스턴스
 */
class JwtAuthInterceptor(
    private val jwtTokenProvider: suspend () -> String?,
    private val logger: Logger
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // runBlocking을 사용하여 suspend 함수 호출
        val jwtToken = runBlocking { jwtTokenProvider() }
        
        return if (jwtToken != null) {
            logger.debug("Adding JWT Authorization header to ${originalRequest.url}")
            val request = originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $jwtToken")
                .build()
            chain.proceed(request)
        } else {
            logger.debug("No JWT token available, proceeding without Authorization header")
            chain.proceed(originalRequest)
        }
    }
}
