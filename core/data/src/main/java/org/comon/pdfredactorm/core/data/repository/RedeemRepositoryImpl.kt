package org.comon.pdfredactorm.core.data.repository

import org.comon.pdfredactorm.core.common.logger.Logger
import org.comon.pdfredactorm.core.network.dto.ValidateCodeRequestDto
import org.comon.pdfredactorm.core.network.api.RedeemApi
import org.comon.pdfredactorm.core.domain.repository.RedeemRepository
import javax.inject.Inject

class RedeemRepositoryImpl @Inject constructor(
    private val api: RedeemApi,
    private val logger: Logger
) : RedeemRepository {

    override suspend fun validateCode(email: String, code: String, uuid: String): Result<Boolean> {
        logger.debug("Validating redeem code for email: $email")
        return try {
            val request = ValidateCodeRequestDto(email, code, uuid)
            val response = api.validateCode(request)

            if (response.isSuccessful && response.body() != null) {
                val validateResponse = response.body()!!
                if (validateResponse.isValid) {
                    logger.info("Redeem code validation successful for email: $email")
                    Result.success(true)
                } else {
                    logger.warning("Redeem code validation failed: ${validateResponse.message}")
                    Result.failure(Exception(validateResponse.message))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown API Error"
                logger.error("Redeem API error ${response.code()}: $errorMsg")
                Result.failure(Exception("API Error ${response.code()}: $errorMsg"))
            }
        } catch (e: Exception) {
            logger.error("Redeem code validation exception", e)
            Result.failure(e)
        }
    }
}
