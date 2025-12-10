package org.comon.pdfredactorm.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import org.comon.pdfredactorm.core.common.logger.Logger

/**
 * API 호출 시 지정된 헤더에 API 키를 자동으로 추가하는 Interceptor
 *
 * @param headerName 헤더 이름 (예: "X-Redact-Api-Key", "X-Redeem-Api-Key")
 * @param apiKey API 키 값
 * @param logger 로깅을 위한 Logger 인스턴스
 */
class ApiKeyInterceptor(
    private val headerName: String,
    private val apiKey: String,
    private val logger: Logger
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        logger.debug("Adding API key header '$headerName' to ${originalRequest.url}")
        val request = originalRequest.newBuilder()
            .addHeader(headerName, apiKey)
            .build()
        return chain.proceed(request)
    }
}
