package org.comon.pdfredactorm.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * API 호출 시 지정된 헤더에 API 키를 자동으로 추가하는 Interceptor
 *
 * @param headerName 헤더 이름 (예: "X-Redact-Api-Key", "X-Redeem-Api-Key")
 * @param apiKey API 키 값
 */
class ApiKeyInterceptor(
    private val headerName: String,
    private val apiKey: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader(headerName, apiKey)
            .build()
        return chain.proceed(request)
    }
}
