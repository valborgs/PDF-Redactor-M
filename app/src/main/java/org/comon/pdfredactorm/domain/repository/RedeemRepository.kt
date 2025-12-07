package org.comon.pdfredactorm.domain.repository

interface RedeemRepository {
    suspend fun validateCode(email: String, code: String, uuid: String): Result<Boolean>
}
