package org.comon.pdfredactorm.data.repository

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.comon.pdfredactorm.data.remote.RedactionApi
import org.comon.pdfredactorm.domain.model.RedactionInfo
import org.comon.pdfredactorm.domain.repository.RemoteRedactionRepository
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class RemoteRedactionRepositoryImpl @Inject constructor(
    private val api: RedactionApi
) : RemoteRedactionRepository {

    override suspend fun redactPdf(file: File, redactions: List<RedactionInfo>): Result<File> {
        return try {
            val requestFile = file.asRequestBody("application/pdf".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val gson = Gson()
            val redactionsJson = gson.toJson(redactions)
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
                Result.success(outputFile)
            } else {
                Result.failure(Exception("Redaction failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
