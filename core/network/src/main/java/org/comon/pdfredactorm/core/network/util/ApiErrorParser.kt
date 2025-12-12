package org.comon.pdfredactorm.core.network.util

import kotlinx.serialization.json.Json
import org.comon.pdfredactorm.core.network.dto.ApiErrorResponseDto
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API 에러 응답을 파싱하는 유틸리티 클래스
 * Repository에서 Json을 직접 사용하지 않고 이 클래스를 통해 에러를 파싱합니다.
 */
@Singleton
class ApiErrorParser @Inject constructor(
    private val json: Json
) {
    /**
     * Retrofit Response의 errorBody를 ApiErrorResponseDto로 파싱합니다.
     * 파싱에 실패하면 기본 에러 메시지를 반환합니다.
     */
    fun <T> parseError(response: Response<T>): ApiErrorResponseDto {
        val rawError = response.errorBody()?.string()
        return if (rawError != null) {
            try {
                json.decodeFromString<ApiErrorResponseDto>(rawError)
            } catch (e: Exception) {
                ApiErrorResponseDto(message = "Error ${response.code()}: ${response.message()}")
            }
        } else {
            ApiErrorResponseDto(message = "Error ${response.code()}: ${response.message()}")
        }
    }

    /**
     * 에러 메시지 문자열만 추출합니다.
     */
    fun <T> getErrorMessage(response: Response<T>): String {
        return parseError(response).message
    }
}
