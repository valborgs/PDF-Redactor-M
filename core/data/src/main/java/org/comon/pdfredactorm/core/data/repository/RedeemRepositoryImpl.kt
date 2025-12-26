package org.comon.pdfredactorm.core.data.repository

import org.comon.pdfredactorm.core.common.logger.Logger
import org.comon.pdfredactorm.core.network.dto.ValidateCodeRequestDto
import org.comon.pdfredactorm.core.network.dto.CheckDeviceRequestDto
import org.comon.pdfredactorm.core.network.dto.CheckDeviceResponseDto
import org.comon.pdfredactorm.core.network.api.RedeemApi
import org.comon.pdfredactorm.core.network.util.ApiErrorParser
import org.comon.pdfredactorm.core.domain.repository.RedeemRepository
import org.comon.pdfredactorm.core.model.ApiException
import javax.inject.Inject

class RedeemRepositoryImpl @Inject constructor(
    private val api: RedeemApi,
    private val errorParser: ApiErrorParser,
    private val logger: Logger
) : RedeemRepository {

    override suspend fun validateCode(email: String, code: String, deviceId: String): Result<String> {
        logger.debug("Validating redeem code for email: $email, deviceId: $deviceId")
        return try {
            val request = ValidateCodeRequestDto(email, code, deviceId)
            val response = api.validateCode(request)

            if (response.isSuccessful && response.body() != null) {
                val validateResponse = response.body()!!
                val jwtToken = validateResponse.jwtToken
                
                when {
                    validateResponse.isValid && jwtToken != null -> {
                        logger.info("Redeem code validation successful, JWT token received")
                        Result.success(jwtToken)
                    }
                    validateResponse.isValid -> {
                        logger.warning("Redeem code valid but no JWT token received")
                        Result.failure(Exception("서버에서 JWT 토큰을 발급하지 않았습니다."))
                    }
                    else -> {
                        val errorCode = validateResponse.errorCode ?: -1
                        logger.warning("Redeem code validation failed (code: $errorCode): ${validateResponse.message}")
                        Result.failure(ApiException(errorCode, validateResponse.message))
                    }
                }
            } else {
                val errorDto = errorParser.parseError(response)
                val errorCode = errorDto.errorCode ?: response.code()
                val errorMessage = errorDto.message
                logger.error("Redeem API error ${response.code()}, code: $errorCode: $errorMessage")
                Result.failure(ApiException(errorCode, errorMessage))
            }
        } catch (e: Exception) {
            logger.error("Redeem code validation exception", e)
            Result.failure(e)
        }
    }

    override suspend fun checkDeviceMismatch(code: String, deviceId: String): Result<Boolean> {
        logger.debug("Checking device consistency for code: $code, deviceId: $deviceId")
        return try {
            val request = CheckDeviceRequestDto(code, deviceId)
            val response = api.checkDevice(request)

            if (response.isSuccessful && response.body() != null) {
                val checkResponse = response.body()!!
                logger.info("Device consistency check successful: isMismatch=${checkResponse.isMismatch}")
                Result.success(checkResponse.isMismatch)
            } else {
                val errorDto = errorParser.parseError(response)
                val errorCode = errorDto.errorCode ?: response.code()
                val errorMessage = errorDto.message
                logger.warning("Device check API error ${response.code()}, code: $errorCode: $errorMessage")
                Result.failure(ApiException(errorCode, errorMessage))
            }
        } catch (e: Exception) {
            logger.error("Device consistency check exception", e)
            Result.failure(e)
        }
    }
}
