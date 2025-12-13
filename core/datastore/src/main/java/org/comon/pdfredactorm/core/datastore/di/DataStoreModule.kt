package org.comon.pdfredactorm.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.comon.pdfredactorm.core.datastore.migration.DataStoreMigration
import java.io.File
import javax.inject.Singleton

private const val ENCRYPTED_PREFERENCES_NAME = "encrypted_user_preferences.preferences_pb"

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideMasterKey(@ApplicationContext context: Context): MasterKey {
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    @Provides
    @Singleton
    fun provideDataStoreMigration(@ApplicationContext context: Context): DataStoreMigration {
        return DataStoreMigration(context)
    }

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context,
        masterKey: MasterKey,
        migration: DataStoreMigration
    ): DataStore<Preferences> {
        val encryptedFile = File(context.filesDir, ENCRYPTED_PREFERENCES_NAME)
        
        // EncryptedFile 빌더 생성 (파일이 없으면 자동으로 생성됨)
        EncryptedFile.Builder(
            context,
            encryptedFile,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { encryptedFile }
        )
        
        // 기존 DataStore에서 마이그레이션 필요 여부 확인 및 실행
        if (migration.isMigrationNeeded()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val legacyData = migration.readLegacyData()
                    migration.migrateToEncrypted(dataStore, legacyData)
                } catch (e: Exception) {
                    // 마이그레이션 실패 시 로그 (Crashlytics에서 수집됨)
                    e.printStackTrace()
                }
            }
        }
        
        return dataStore
    }
}
