package org.comon.pdfredactorm.core.data.repository

import org.comon.pdfredactorm.core.common.logger.Logger
import org.comon.pdfredactorm.core.network.api.RedactionApi
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

class RemoteRedactionRepositoryImpl @Inject constructor(
    private val api: RedactionApi,
    private val json: Json,
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
                val errorMsg = "Redaction failed: ${response.code()} ${response.message()}"
                logger.error(errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            logger.error("Remote redaction exception", e)
            Result.failure(e)
        }
    }
}
