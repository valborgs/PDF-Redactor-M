package org.comon.pdfredactorm.core.network.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface RedactionApi {
    @Multipart
    @POST("api/pdf/redact/")
    suspend fun redactPdf(
        @Part file: MultipartBody.Part,
        @Part("redactions") redactions: RequestBody
    ): Response<ResponseBody>
}
