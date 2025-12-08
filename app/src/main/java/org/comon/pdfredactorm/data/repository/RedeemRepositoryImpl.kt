package org.comon.pdfredactorm.data.repository

import org.comon.pdfredactorm.BuildConfig
import org.comon.pdfredactorm.core.network.dto.ValidateCodeRequestDto
import org.comon.pdfredactorm.core.network.api.RedeemApi
import org.comon.pdfredactorm.domain.repository.RedeemRepository
import org.comon.pdfredactorm.domain.repository.SettingsRepository
import javax.inject.Inject

class RedeemRepositoryImpl @Inject constructor(
    private val api: RedeemApi,
    private val settingsRepository: SettingsRepository
) : RedeemRepository {

    override suspend fun validateCode(email: String, code: String, uuid: String): Result<Boolean> {
        return try {
            val request = ValidateCodeRequestDto(email, code, uuid)
            val response = api.validateCode(BuildConfig.REDEEM_API_KEY, request)

            if (response.isSuccessful && response.body() != null) {
                val validateResponse = response.body()!!
                if (validateResponse.isValid) {
                    settingsRepository.setProEnabled(true)
                    Result.success(true)
                } else {
                    Result.failure(Exception(validateResponse.message))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown API Error"
                Result.failure(Exception("API Error ${response.code()}: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
