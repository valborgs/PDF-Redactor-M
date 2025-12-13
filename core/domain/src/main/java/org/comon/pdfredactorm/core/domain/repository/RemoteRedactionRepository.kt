package org.comon.pdfredactorm.core.domain.repository

import org.comon.pdfredactorm.core.model.RedactionMask
import java.io.File

interface RemoteRedactionRepository {
    /**
     * 원격 서버에서 PDF를 마스킹 처리합니다.
     * JWT 토큰은 OkHttp Interceptor에서 자동으로 추가됩니다.
     * 
     * @param file PDF 파일
     * @param redactions 마스킹 영역 목록
     * @return 마스킹 처리된 파일 또는 실패
     */
    suspend fun redactPdf(file: File, redactions: List<RedactionMask>): Result<File>
}
