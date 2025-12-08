package org.comon.pdfredactorm.core.domain.repository

import org.comon.pdfredactorm.core.model.RedactionMask
import java.io.File

interface RemoteRedactionRepository {
    suspend fun redactPdf(file: File, redactions: List<RedactionMask>): Result<File>
}
