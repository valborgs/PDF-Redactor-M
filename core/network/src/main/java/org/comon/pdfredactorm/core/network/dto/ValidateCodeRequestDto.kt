package org.comon.pdfredactorm.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ValidateCodeRequestDto(
    val email: String,
    val code: String,
    val uuid: String
)
