package org.comon.pdfredactorm.core.model

import kotlinx.serialization.Serializable

/**
 * Pro 버전 활성화 정보
 */
@Serializable
data class ProInfo(
    val email: String,
    val uuid: String,
    val redeemCode: String,
    val activationDate: Long
)
