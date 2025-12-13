# 암호화된 DataStore 구현 가이드

안드로이드 앱에서 민감한 설정 데이터를 안전하게 저장하기 위한 **Encrypted Preferences DataStore** 구현 가이드입니다.

---

## 📚 목차

1. [왜 암호화된 DataStore가 필요한가?](#1-왜-암호화된-datastore가-필요한가)
2. [사전 준비](#2-사전-준비)
3. [핵심 개념 이해하기](#3-핵심-개념-이해하기)
4. [구현 단계](#4-구현-단계)
5. [마이그레이션 (기존 데이터 이전)](#5-마이그레이션-기존-데이터-이전)
6. [주의사항](#6-주의사항)

---

## 1. 왜 암호화된 DataStore가 필요한가?

### 일반 DataStore의 문제점

```
📁 /data/data/com.yourapp/files/datastore/
   └── user_preferences.preferences_pb  ← 평문 저장!
```

일반 `Preferences DataStore`는 데이터를 **평문(Plain Text)**으로 저장합니다.

| 위험 | 설명 |
|------|------|
| 🔓 루팅된 기기 | Root 권한으로 파일을 직접 읽고 수정 가능 |
| 🔄 ADB 백업 | `adb backup` 명령으로 데이터 추출 가능 |
| ☁️ 클라우드 백업 | Google Drive에 민감 데이터 백업 |

### 암호화된 DataStore의 해결책

```
📁 /data/data/com.yourapp/files/
   └── encrypted_user_preferences.preferences_pb  ← AES256-GCM 암호화!
```

**AES256-GCM** 알고리즘으로 데이터를 암호화하여 저장합니다.

---

## 2. 사전 준비

### 의존성 추가

**gradle/libs.versions.toml**
```toml
[versions]
datastore = "1.1.1"
securityCrypto = "1.1.0-alpha06"

[libraries]
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto" }
```

**core/datastore/build.gradle.kts**
```kotlin
dependencies {
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
}
```

> ⚠️ `security-crypto`는 현재 alpha 버전이지만, 2023년부터 안정적으로 사용되고 있습니다.

---

## 3. 핵심 개념 이해하기

### MasterKey (마스터 키)

```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()
```

| 속성 | 설명 |
|------|------|
| **역할** | 암호화/복호화에 사용되는 최상위 키 |
| **저장 위치** | Android Keystore (하드웨어 보안 모듈) |
| **특징** | 앱 삭제 시 자동 삭제, 추출 불가능 |

### EncryptedFile (암호화된 파일)

```kotlin
val encryptedFile = EncryptedFile.Builder(
    context,
    file,
    masterKey,
    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
).build()
```

| 속성 | 설명 |
|------|------|
| **암호화 방식** | AES256-GCM-HKDF |
| **청크 크기** | 4KB (스트리밍 암호화) |
| **자동 처리** | 읽기/쓰기 시 자동 암복호화 |

### 암호화 흐름도

```
┌─────────────────────────────────────────────────────────────┐
│                         앱 코드                              │
│  dataStore.edit { prefs -> prefs[KEY] = "비밀번호" }        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Preferences DataStore                     │
│              (직렬화: Protocol Buffers)                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     EncryptedFile                            │
│              AES256-GCM 암호화 수행                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│          Android Keystore (MasterKey 저장)                   │
│              하드웨어 보안 모듈 (TEE/StrongBox)              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      파일 시스템                             │
│  encrypted_user_preferences.preferences_pb (암호화된 바이트) │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. 구현 단계

### Step 1: MasterKey 제공

```kotlin
@Provides
@Singleton
fun provideMasterKey(@ApplicationContext context: Context): MasterKey {
    return MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
}
```

### Step 2: 암호화된 DataStore 생성

```kotlin
private const val ENCRYPTED_PREFERENCES_NAME = "encrypted_user_preferences.preferences_pb"

@Provides
@Singleton
fun provideDataStore(
    @ApplicationContext context: Context,
    masterKey: MasterKey
): DataStore<Preferences> {
    val encryptedFile = File(context.filesDir, ENCRYPTED_PREFERENCES_NAME)
    
    // EncryptedFile 빌더 생성
    EncryptedFile.Builder(
        context,
        encryptedFile,
        masterKey,
        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
    ).build()
    
    return PreferenceDataStoreFactory.create(
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { encryptedFile }
    )
}
```

### Step 3: Repository에서 사용 (기존과 동일)

```kotlin
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>  // 암호화된 DataStore 주입
) : SettingsRepository {
    
    private object PreferencesKeys {
        val IS_PRO_ENABLED = booleanPreferencesKey("is_pro_enabled")
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
}
```

> 💡 **핵심 포인트**: Repository 코드는 변경할 필요가 없습니다! DI에서 암호화된 DataStore를 주입하기만 하면 됩니다.

---

## 5. 마이그레이션 (기존 데이터 이전)

기존 앱 사용자의 데이터를 안전하게 암호화된 저장소로 이전해야 합니다.

### 마이그레이션 클래스

```kotlin
class DataStoreMigration(private val context: Context) {
    
    companion object {
        private const val LEGACY_PREFERENCES_NAME = "user_preferences"
        private const val MIGRATION_MARKER_FILE = "datastore_migration_complete"
    }
    
    // 기존 DataStore 접근용
    private val Context.legacyDataStore by preferencesDataStore(name = LEGACY_PREFERENCES_NAME)
    
    /**
     * 마이그레이션이 필요한지 확인
     */
    fun isMigrationNeeded(): Boolean {
        val legacyFile = File(context.filesDir, "datastore/$LEGACY_PREFERENCES_NAME.preferences_pb")
        val markerFile = File(context.filesDir, MIGRATION_MARKER_FILE)
        return legacyFile.exists() && !markerFile.exists()
    }
    
    /**
     * 기존 데이터 읽기
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
     * 암호화된 DataStore로 데이터 이전
     */
    suspend fun migrateToEncrypted(
        encryptedDataStore: DataStore<Preferences>,
        legacyData: LegacyData
    ) {
        // 1. 새 저장소에 데이터 저장
        encryptedDataStore.edit { preferences ->
            preferences[IS_PRO_ENABLED] = legacyData.isProEnabled
            preferences[IS_FIRST_LAUNCH] = legacyData.isFirstLaunch
            legacyData.appUuid?.let { preferences[APP_UUID] = it }
        }
        
        // 2. 마이그레이션 완료 마커 생성
        File(context.filesDir, MIGRATION_MARKER_FILE).createNewFile()
        
        // 3. 기존 파일 삭제
        deleteLegacyDataStore()
    }
}
```

### 마이그레이션 흐름도

```
┌────────────────────────────────────────────┐
│              앱 시작                        │
└────────────────────────────────────────────┘
                    │
                    ▼
┌────────────────────────────────────────────┐
│   기존 DataStore 파일이 존재하는가?         │
│   && 마이그레이션 마커가 없는가?            │
└────────────────────────────────────────────┘
                    │
          ┌────────┴────────┐
          │ Yes             │ No
          ▼                 ▼
┌──────────────────┐   ┌──────────────────┐
│ 기존 데이터 읽기  │   │ 정상 사용         │
└──────────────────┘   └──────────────────┘
          │
          ▼
┌──────────────────────────────────────────┐
│ 암호화된 DataStore에 데이터 저장          │
└──────────────────────────────────────────┘
          │
          ▼
┌──────────────────────────────────────────┐
│ 마이그레이션 완료 마커 생성               │
└──────────────────────────────────────────┘
          │
          ▼
┌──────────────────────────────────────────┐
│ 기존 DataStore 파일 삭제                  │
└──────────────────────────────────────────┘
```

---

## 6. 주의사항

### ⚠️ 앱 삭제 시 데이터 손실

MasterKey는 Android Keystore에 저장되며, 앱 삭제 시 함께 삭제됩니다.
→ 암호화된 데이터를 복호화할 수 없게 됩니다.

### ⚠️ 기기 이전 시 데이터 손실

MasterKey는 기기별로 고유하므로 다른 기기로 데이터를 이전할 수 없습니다.
→ 중요한 데이터는 서버에 동기화하는 것을 권장합니다.

### ⚠️ 백업 비활성화 권장

```xml
<application android:allowBackup="false">
```

암호화된 파일을 백업해도 다른 기기에서 복호화할 수 없으므로, 백업을 비활성화하는 것이 좋습니다.

### ⚠️ 예외 처리 필수

```kotlin
try {
    EncryptedFile.Builder(context, file, masterKey, scheme).build()
} catch (e: GeneralSecurityException) {
    // 암호화 실패 처리
} catch (e: IOException) {
    // 파일 I/O 오류 처리
}
```

---

## 📎 프로젝트 파일 구조

```
core/datastore/
├── build.gradle.kts
└── src/main/java/.../core/datastore/
    ├── di/
    │   └── DataStoreModule.kt      ← DI 모듈 (암호화된 DataStore 제공)
    └── migration/
        └── DataStoreMigration.kt   ← 마이그레이션 로직
```

---

## 🔗 참고 자료

- [Android Security Crypto 공식 문서](https://developer.android.com/reference/androidx/security/crypto/package-summary)
- [DataStore 공식 가이드](https://developer.android.com/topic/libraries/architecture/datastore)
- [Android Keystore 시스템](https://developer.android.com/training/articles/keystore)
