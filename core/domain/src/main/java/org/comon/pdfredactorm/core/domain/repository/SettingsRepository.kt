package org.comon.pdfredactorm.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val isProEnabled: Flow<Boolean>
    suspend fun checkFirstLaunch(): Boolean
    suspend fun setProEnabled(enabled: Boolean)
    suspend fun setFirstLaunch(isFirst: Boolean)
    suspend fun getAppUuid(): String
}
