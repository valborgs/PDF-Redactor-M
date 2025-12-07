package org.comon.pdfredactorm.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import org.comon.pdfredactorm.domain.repository.SettingsRepository
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object PreferencesKeys {
        val IS_PRO_ENABLED = booleanPreferencesKey("is_pro_enabled")
        val APP_UUID = stringPreferencesKey("app_uuid")
    }

    override val isProEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_PRO_ENABLED] ?: false
        }

    override suspend fun setProEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_PRO_ENABLED] = enabled
        }
    }

    override suspend fun getAppUuid(): String {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.APP_UUID]
        }.firstOrNull() ?: run {
            val newUuid = java.util.UUID.randomUUID().toString()
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.APP_UUID] = newUuid
            }
            newUuid
        }
    }
}
