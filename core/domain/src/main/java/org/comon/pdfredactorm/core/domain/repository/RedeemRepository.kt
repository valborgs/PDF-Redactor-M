package org.comon.pdfredactorm.core.domain.repository

interface RedeemRepository {
    /**
     * 리딤코드를 검증하고 성공 시 JWT 토큰을 반환합니다.
     * @return JWT 토큰 (검증 성공 시) 또는 예외 (실패 시)
     */
    suspend fun validateCode(email: String, code: String, deviceId: String): Result<String>
}
