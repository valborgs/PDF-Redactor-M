package org.comon.pdfredactorm.core.datastore.migration

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import java.io.File

/**
 * 기존 비암호화 DataStore에서 암호화된 DataStore로 데이터를 마이그레이션합니다.
 * 
 * 마이그레이션 순서:
 * 1. 기존 DataStore 파일이 존재하는지 확인
 * 2. 존재하면 기존 데이터 읽기
 * 3. 암호화된 DataStore에 데이터 쓰기
 * 4. 기존 파일 삭제
 * 
 * @param context Application Context
 */
class DataStoreMigration(
    private val context: Context
) {
    companion object {
        // 기존 DataStore 파일 경로
        private const val LEGACY_PREFERENCES_NAME = "user_preferences"
        
        // 마이그레이션 완료 여부를 저장하는 파일
        private const val MIGRATION_MARKER_FILE = "datastore_migration_complete"
        
        // PreferencesKeys (SettingsRepositoryImpl과 동일)
        val IS_PRO_ENABLED = booleanPreferencesKey("is_pro_enabled")
        val IS_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val APP_UUID = stringPreferencesKey("app_uuid")
    }
    
    // 기존 DataStore 접근용 (읽기 전용)
    private val Context.legacyDataStore by preferencesDataStore(name = LEGACY_PREFERENCES_NAME)
    
    /**
     * 마이그레이션이 필요한지 확인합니다.
     * 기존 DataStore 파일이 존재하고 마이그레이션이 완료되지 않은 경우 true 반환
     */
    fun isMigrationNeeded(): Boolean {
        val legacyFile = File(context.filesDir, "datastore/$LEGACY_PREFERENCES_NAME.preferences_pb")
        val markerFile = File(context.filesDir, MIGRATION_MARKER_FILE)
        return legacyFile.exists() && !markerFile.exists()
    }
    
    /**
     * 기존 DataStore에서 데이터를 읽어옵니다.
     */
    suspend fun readLegacyData(): LegacyData {
        val preferences = context.legacyDataStore.data.firstOrNull()
        return LegacyData(
            isProEnabled = preferences?.get(IS_PRO_ENABLED) ?: false,
            isFirstLaunch = preferences?.get(IS_FIRST_LAUNCH) ?: true,
            appUuid = preferences?.get(APP_UUID)
        )
    }
    
    /**
     * 암호화된 DataStore에 데이터를 저장하고 마이그레이션을 완료 처리합니다.
     */
    suspend fun migrateToEncrypted(
        encryptedDataStore: androidx.datastore.core.DataStore<Preferences>,
        legacyData: LegacyData
    ) {
        // 암호화된 DataStore에 데이터 저장
        encryptedDataStore.edit { preferences ->
            preferences[IS_PRO_ENABLED] = legacyData.isProEnabled
            preferences[IS_FIRST_LAUNCH] = legacyData.isFirstLaunch
            legacyData.appUuid?.let { preferences[APP_UUID] = it }
        }
        
        // 마이그레이션 완료 마커 파일 생성
        File(context.filesDir, MIGRATION_MARKER_FILE).createNewFile()
        
        // 기존 DataStore 파일 삭제
        deleteLegacyDataStore()
    }
    
    /**
     * 기존 DataStore 파일을 삭제합니다.
     */
    private fun deleteLegacyDataStore() {
        val datastoreDir = File(context.filesDir, "datastore")
        if (datastoreDir.exists()) {
            datastoreDir.listFiles()?.filter { 
                it.name.startsWith(LEGACY_PREFERENCES_NAME) 
            }?.forEach { it.delete() }
        }
    }
    
    /**
     * 기존 DataStore에서 마이그레이션할 데이터
     */
    data class LegacyData(
        val isProEnabled: Boolean,
        val isFirstLaunch: Boolean,
        val appUuid: String?
    )
}
