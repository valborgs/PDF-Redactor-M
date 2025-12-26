package org.comon.pdfredactorm.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CheckDeviceRequestDto(
    val code: String,
    val uuid: String
)
