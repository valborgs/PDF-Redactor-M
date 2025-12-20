package org.comon.pdfredactorm.core.data.repository

import org.comon.pdfredactorm.core.common.logger.Logger
import org.comon.pdfredactorm.core.network.api.RedactionApi
import org.comon.pdfredactorm.core.network.util.ApiErrorParser
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.comon.pdfredactorm.core.network.dto.toDto
import org.comon.pdfredactorm.core.model.RedactionMask
import org.comon.pdfredactorm.core.domain.repository.RemoteRedactionRepository
import java.io.File
import java.io.FileOutputStream
import kotlinx.serialization.json.Json
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

import org.comon.pdfredactorm.core.model.ApiException

class RemoteRedactionRepositoryImpl @Inject constructor(
    private val api: RedactionApi,
    private val json: Json,
    private val errorParser: ApiErrorParser,
    private val logger: Logger
) : RemoteRedactionRepository {

    override suspend fun redactPdf(file: File, redactions: List<RedactionMask>): Result<File> {
        logger.debug("Starting remote PDF redaction: ${file.name}, ${redactions.size} redactions")
        return try {
            val requestFile = file.asRequestBody("application/pdf".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val redactionDtos = redactions.map { it.toDto() }
            val redactionsJson = json.encodeToString(redactionDtos)
            val redactionsPart = redactionsJson.toRequestBody("text/plain".toMediaTypeOrNull())

            // JWT 토큰은 JwtAuthInterceptor에서 자동으로 헤더에 추가됨
            val response = api.redactPdf(filePart, redactionsPart)

            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                val outputFile = File(file.parent, "redacted_${file.name}")
                
                responseBody.byteStream().use { inputStream ->
                    FileOutputStream(outputFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                logger.info("Remote redaction successful: ${outputFile.name}")
                Result.success(outputFile)
            } else {
                val errorDto = errorParser.parseError(response)
                val errorCode = errorDto.errorCode
                val errorMessage = errorDto.message

                logger.error("Redaction API error ${response.code()}, code: $errorCode: $errorMessage")
                Result.failure(ApiException(errorCode ?: response.code(), errorMessage))
            }
        } catch (e: Exception) {
            logger.error("Remote redaction exception", e)
            Result.failure(e)
        }
    }
}
