package org.comon.pdfredactorm.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API 에러 응답을 위한 공통 DTO
 * 서버에서 반환하는 에러 응답 형식에 맞춤
 */
@Serializable
data class ApiErrorResponseDto(
    val message: String = "Unknown error",
    @SerialName("is_valid")
    val isValid: Boolean? = null,
    @SerialName("error_code")
    val errorCode: Int? = null
)
