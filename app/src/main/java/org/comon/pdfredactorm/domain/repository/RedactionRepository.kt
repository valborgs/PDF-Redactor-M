package org.comon.pdfredactorm.domain.repository

import org.comon.pdfredactorm.domain.model.RedactionInfo
import java.io.File

interface RedactionRepository {
    suspend fun redactPdf(file: File, redactions: List<RedactionInfo>): Result<File>
}
