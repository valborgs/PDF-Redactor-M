package org.comon.pdfredactorm.core.model

/**
 * API 호출 중 발생하는 예외를 정의하는 클래스
 * @param errorCode 서버에서 반환한 에러 코드
 * @param message 에러 메시지
 */
class ApiException(
    val errorCode: Int,
    override val message: String
) : Exception(message) {
    /** 마스킹 API 에러 코드를 상수로 변환 */
    val redactionErrorCode: RedactionErrorCode
        get() = RedactionErrorCode.fromCode(errorCode)

    /** 리딤 API 에러 코드를 상수로 변환 */
    val redeemErrorCode: RedeemErrorCode
        get() = RedeemErrorCode.fromCode(errorCode)
}
