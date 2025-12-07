package org.comon.pdfredactorm.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val isProEnabled: Flow<Boolean>
    suspend fun setProEnabled(enabled: Boolean)
    suspend fun getAppUuid(): String
}
