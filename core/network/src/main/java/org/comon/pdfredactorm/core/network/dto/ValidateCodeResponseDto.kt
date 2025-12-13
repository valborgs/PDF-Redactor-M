package org.comon.pdfredactorm.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValidateCodeResponseDto(
    val message: String,
    @SerialName("is_valid")
    val isValid: Boolean,
    @SerialName("jwt_token")
    val jwtToken: String? = null  // 검증 성공 시 서버에서 JWT 토큰 발급
)
