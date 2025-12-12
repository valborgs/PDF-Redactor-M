# Analytics 시스템 가이드

PDF Redactor M 애플리케이션의 Google Analytics(Firebase Analytics) 이벤트 추적 시스템에 대한 문서입니다.

## 1. 아키텍처 (Clean Architecture)

Analytics 시스템은 Logger와 동일한 Clean Architecture 패턴을 따릅니다.

```mermaid
graph TD
    subgraph "Feature Layer"
        HVM["HomeViewModel"]
        EVM["EditorViewModel"]
    end
    
    subgraph "core:common (순수 Kotlin)"
        AT["AnalyticsTracker\n(Interface)"]
    end
    
    subgraph "core:data (Android)"
        FAT["FirebaseAnalyticsTracker\n(Implementation)"]
    end
    
    subgraph "Firebase"
        FA["Firebase Analytics SDK"]
    end
    
    HVM --> AT
    EVM --> AT
    AT -.-> FAT
    FAT --> FA
```

### Common Layer
- **`AnalyticsTracker` 인터페이스**: Analytics 추적 기능을 추상화한 인터페이스입니다.
- 플랫폼(Android)에 독립적이며, ViewModel에서 사용됩니다.

### Data Layer
- **`FirebaseAnalyticsTracker`**: Firebase Analytics SDK를 사용한 구현체입니다.
- Android 플랫폼에 의존적입니다.

### DI (Dependency Injection)
- Hilt를 통해 `DataModule`에서 `AnalyticsTracker` 인터페이스에 `FirebaseAnalyticsTracker` 구현체를 바인딩합니다.


## 2. 추적 이벤트 목록

| 이벤트 이름 | 설명 | 파라미터 | 위치 |
|-------------|------|----------|------|
| `pro_activated` | Pro 버전 활성화 성공 | - | `HomeViewModel` |
| `open_coffeechat` | 커피챗 버튼 클릭 | - | `HomeViewModel` |
| `pii_detected_current_page` | 현재 페이지 PII 탐지 완료 | `count`, `types` | `EditorViewModel` |
| `pii_detected_all_pages` | 전체 페이지 PII 탐지 완료 | `count`, `types` | `EditorViewModel` |
| `mask_saved` | 마스킹 저장 완료 | `mask_count`, `method` | `EditorViewModel` |

### 파라미터 설명

#### `pii_detected_*` 이벤트
- `count`: 탐지된 PII 개수 (Int)
- `types`: 탐지된 PII 유형 목록, 쉼표로 구분 (String, 예: "PHONE,EMAIL,RRN")

#### `mask_saved` 이벤트
- `mask_count`: 적용된 마스크 개수 (Int)
- `method`: 마스킹 방식 - `local` (로컬 처리) 또는 `remote` (서버 API) (String)

## 3. 사용 방법

### ViewModel에서 이벤트 로깅

```kotlin
@HiltViewModel
class SampleViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {
    
    fun onSomeAction() {
        // 파라미터 없는 이벤트
        analyticsTracker.logEvent("event_name")
        
        // 파라미터가 있는 이벤트
        analyticsTracker.logEvent("event_name", mapOf(
            "param1" to "value1",
            "param2" to 123
        ))
    }
}
```

### 화면 조회 로깅

```kotlin
analyticsTracker.logScreenView("HomeScreen", "HomeScreen")
```

### 사용자 속성 설정

```kotlin
analyticsTracker.setUserProperty("is_pro_user", "true")
```

## 4. 디버깅

### Firebase DebugView 활성화

Android Studio 터미널에서 다음 명령어 실행:

```bash
adb shell setprop debug.firebase.analytics.app org.comon.pdfredactorm
```

비활성화:

```bash
adb shell setprop debug.firebase.analytics.app .none.
```

### Logcat 필터

Firebase Analytics 로그 확인:

```
tag:FA
```

## 5. 이벤트 추가 가이드

새로운 이벤트를 추가할 때:

1. **이벤트 이름**: 소문자와 언더스코어 사용 (예: `button_clicked`)
2. **파라미터 이름**: 소문자와 언더스코어 사용 (예: `button_name`)
3. **이벤트 이름 길이**: 최대 40자
4. **파라미터 개수**: 이벤트당 최대 25개
5. **파라미터 값 길이**: 문자열은 최대 100자

### 이벤트 네이밍 컨벤션

```
<object>_<action>
```

예시:
- `pdf_opened`
- `mask_saved`
- `pii_detected_current_page`
