package org.comon.pdfredactorm.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val isProEnabled: Flow<Boolean>
    suspend fun setProEnabled(enabled: Boolean)
    suspend fun getAppUuid(): String
}
