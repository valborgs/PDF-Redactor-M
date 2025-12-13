package org.comon.pdfredactorm.core.data.repository

import org.comon.pdfredactorm.core.common.logger.Logger
import org.comon.pdfredactorm.core.network.dto.ValidateCodeRequestDto
import org.comon.pdfredactorm.core.network.api.RedeemApi
import org.comon.pdfredactorm.core.network.util.ApiErrorParser
import org.comon.pdfredactorm.core.domain.repository.RedeemRepository
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
                        // 서버가 아직 JWT를 발급하지 않는 경우 (하위 호환성)
                        logger.warning("Redeem code valid but no JWT token received")
                        Result.failure(Exception("서버에서 JWT 토큰을 발급하지 않았습니다."))
                    }
                    else -> {
                        logger.warning("Redeem code validation failed: ${validateResponse.message}")
                        Result.failure(Exception(validateResponse.message))
                    }
                }
            } else {
                val errorMessage = errorParser.getErrorMessage(response)
                logger.error("Redeem API error ${response.code()}: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            logger.error("Redeem code validation exception", e)
            Result.failure(e)
        }
    }
}
