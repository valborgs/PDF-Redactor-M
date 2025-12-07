package org.comon.pdfredactorm.data.remote

import org.comon.pdfredactorm.data.dto.ValidateCodeRequestDto
import org.comon.pdfredactorm.data.dto.ValidateCodeResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface RedeemApi {
    @POST("api/pdf-redeem/redeem/validate/")
    suspend fun validateCode(
        @Header("X-Redeem-Api-Key") apiKey: String,
        @Body request: ValidateCodeRequestDto
    ): Response<ValidateCodeResponseDto>
}
