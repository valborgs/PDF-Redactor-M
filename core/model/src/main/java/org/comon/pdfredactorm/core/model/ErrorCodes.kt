package org.comon.pdfredactorm.core.model

/**
 * API 에러 코드를 정의하는 클래스
 */
/**
 * 마스킹(Redaction) API 관련 에러 코드
 * 문서: docs/pdf_redactor_api_guide.md
 */
enum class RedactionErrorCode(val code: Int) {
    /** API Key (X-Redact-Api-Key)가 없거나 올바르지 않음 (403 Forbidden) */
    INVALID_API_KEY(1001),

    /** Authorization 헤더가 없음 (401 Unauthorized) */
    MISSING_AUTH_HEADER(2001),

    /** 인증 토큰이 유효하지 않거나 만료됨 (401 Unauthorized) */
    INVALID_TOKEN(2002),

    /** 기기 미매칭: 다른 기기에서 Pro 기능이 활성화됨 (401 Unauthorized) */
    DEVICE_MISMATCH(2003),

    /** 필수 파라미터(file, redactions) 누락 (400 Bad Request) */
    MISSING_PARAMETERS(3001),

    /** redactions JSON 데이터의 형식이 올바르지 않음 (400 Bad Request) */
    INVALID_JSON_FORMAT(3002),

    /** 서버 내부 로직 처리 중 발생한 알 수 없는 오류 (500 Internal Server Error) */
    INTERNAL_SERVER_ERROR(5001),

    /** 알 수 없는 에러 코드 */
    UNKNOWN(-1);

    companion object {
        fun fromCode(code: Int?): RedactionErrorCode = 
            entries.find { it.code == code } ?: UNKNOWN
    }
}

/**
 * 리딤 코드(Redeem) API 관련 에러 코드
 * 문서: docs/redactor_pro_code_issuance_api.md
 */
enum class RedeemErrorCode(val code: Int) {
    /** API Key (X-Redeem-Api-Key) 검증 실패 (403 Forbidden) */
    INVALID_API_KEY(1002),

    /** 유효하지 않은 데이터 형식 (이메일 누락 등) (400 Bad Request) */
    INVALID_DATA_FORMAT(3003),

    /** 이미 삭제 처리된 리딤코드 (400 Bad Request) */
    DELETED_CODE(4001),

    /** DB에 존재하지 않는 이메일/코드 조합 (404 Not Found) */
    INVALID_CODE_COMBINATION(4002),

    /** 알 수 없는 에러 코드 */
    UNKNOWN(-1);

    companion object {
        fun fromCode(code: Int?): RedeemErrorCode = 
            entries.find { it.code == code } ?: UNKNOWN
    }
}
