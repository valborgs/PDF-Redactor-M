package org.comon.pdfredactorm.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.comon.pdfredactorm.core.common.logger.Logger
import org.comon.pdfredactorm.core.domain.repository.SettingsRepository
import org.comon.pdfredactorm.core.model.ProInfo
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val logger: Logger
) : SettingsRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private object PreferencesKeys {
        val IS_PRO_ENABLED = booleanPreferencesKey("is_pro_enabled")
        val IS_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val APP_UUID = stringPreferencesKey("app_uuid")
        val JWT_TOKEN = stringPreferencesKey("jwt_token")
        val PRO_INFO = stringPreferencesKey("pro_info")
    }

    override val isProEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_PRO_ENABLED] ?: false
        }

    override suspend fun checkFirstLaunch(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.IS_FIRST_LAUNCH] ?: true
        }.firstOrNull() ?: true
    }

    override suspend fun setProEnabled(enabled: Boolean) {
        logger.info("Pro status changed: $enabled")
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_PRO_ENABLED] = enabled
        }
    }

    override suspend fun setFirstLaunch(isFirst: Boolean) {
        logger.debug("First launch status changed: $isFirst")
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FIRST_LAUNCH] = isFirst
        }
    }

    override suspend fun getAppUuid(): String {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.APP_UUID]
        }.firstOrNull() ?: run {
            val newUuid = java.util.UUID.randomUUID().toString()
            logger.info("Generated new app UUID: $newUuid")
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.APP_UUID] = newUuid
            }
            newUuid
        }
    }

    override suspend fun saveJwtToken(token: String) {
        logger.info("Saving JWT token")
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.JWT_TOKEN] = token
        }
    }

    override suspend fun getJwtToken(): String? {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.JWT_TOKEN]
        }.firstOrNull()
    }

    override suspend fun clearJwtToken() {
        logger.info("Clearing JWT token")
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.JWT_TOKEN)
            preferences[PreferencesKeys.IS_PRO_ENABLED] = false
        }
    }

    override suspend fun getProInfo(): ProInfo? {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.PRO_INFO]
        }.firstOrNull()?.let { jsonString ->
            try {
                json.decodeFromString<ProInfo>(jsonString)
            } catch (e: Exception) {
                logger.warning("Failed to parse ProInfo: ${e.message}")
                null
            }
        }
    }

    override suspend fun setProInfo(proInfo: ProInfo) {
        logger.info("Saving ProInfo for email: ${proInfo.email}")
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRO_INFO] = json.encodeToString(proInfo)
        }
    }
}
