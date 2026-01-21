package org.comon.pdfredactorm.core.network.api

import org.comon.pdfredactorm.core.network.dto.CheckDeviceRequestDto
import org.comon.pdfredactorm.core.network.dto.CheckDeviceResponseDto
import org.comon.pdfredactorm.core.network.dto.ValidateCodeRequestDto
import org.comon.pdfredactorm.core.network.dto.ValidateCodeResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RedeemApi {
    @POST("pdf-redeem/redeem/validate/")
    suspend fun validateCode(
        @Body request: ValidateCodeRequestDto
    ): Response<ValidateCodeResponseDto>

    @POST("pdf-redeem/redeem/check-device/")
    suspend fun checkDevice(
        @Body request: CheckDeviceRequestDto
    ): Response<CheckDeviceResponseDto>
}
