package org.comon.pdfredactorm.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValidateCodeResponseDto(
    val message: String,
    @SerialName("is_valid")
    val isValid: Boolean
)
