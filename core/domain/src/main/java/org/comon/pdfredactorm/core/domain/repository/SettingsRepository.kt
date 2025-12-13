package org.comon.pdfredactorm.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val isProEnabled: Flow<Boolean>
    suspend fun checkFirstLaunch(): Boolean
    suspend fun setProEnabled(enabled: Boolean)
    suspend fun setFirstLaunch(isFirst: Boolean)
    suspend fun getAppUuid(): String
    
    // JWT 토큰 관리
    suspend fun saveJwtToken(token: String)
    suspend fun getJwtToken(): String?
    suspend fun clearJwtToken()
}
