package org.comon.pdfredactorm.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckDeviceResponseDto(
    @SerialName("is_mismatch")
    val isMismatch: Boolean,
    val message: String,
    @SerialName("error_code")
    val errorCode: Int? = null
)
