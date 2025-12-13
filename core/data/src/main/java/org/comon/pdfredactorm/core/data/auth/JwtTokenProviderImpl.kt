package org.comon.pdfredactorm.core.data.auth

import org.comon.pdfredactorm.core.domain.repository.SettingsRepository
import org.comon.pdfredactorm.core.network.auth.JwtTokenProvider
import javax.inject.Inject

/**
 * JwtTokenProvider 구현체
 * 
 * SettingsRepository에서 JWT 토큰을 가져옵니다.
 */
class JwtTokenProviderImpl @Inject constructor(
    private val settingsRepository: SettingsRepository
) : JwtTokenProvider {
    override suspend fun getJwtToken(): String? {
        return settingsRepository.getJwtToken()
    }
}
