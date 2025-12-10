# Repository 함수 분석: Suspend vs Flow

## 개요

Kotlin 코루틴에서 `suspend` 함수와 `Flow`는 서로 다른 용도로 사용됩니다.

| 구분 | Suspend 함수 | Flow |
|------|-------------|------|
| **반환 값** | 단일 값 (1개) | 여러 값 (0~N개) |
| **실행 시점** | 호출 즉시 | `collect` 시 |
| **사용 사례** | API 호출, 일회성 DB 쿼리 | 실시간 업데이트, 이벤트 스트림 |

---

## 프로젝트 리포지터리 분석

### 1. LocalPdfRepositoryImpl

| 함수 | 타입 | 평가 | 설명 |
|------|------|------|------|
| `loadPdf(file)` | `suspend` | ✅ 적절 | 일회성으로 PDF를 로드하고 단일 `PdfDocument` 반환 |
| `getPdfPageCount(file)` | `suspend` | ✅ 적절 | 일회성으로 페이지 수 조회 |
| `saveRedactedPdf(...)` | `suspend` | ✅ 적절 | PDF 저장은 일회성 작업 |
| `getRecentProjects()` | `Flow` | ✅ 적절 | DB의 프로젝트 목록을 **실시간 관찰** |
| `saveProject(...)` | `suspend` | ✅ 적절 | 일회성 저장 작업 |
| `saveRedactions(...)` | `suspend` | ✅ 적절 | 일회성 저장 작업 |
| `getRedactions(pdfId)` | `suspend` | ✅ 적절 | 특정 시점의 마스크 목록 조회 |
| `deleteProject(pdfId)` | `suspend` | ✅ 적절 | 일회성 삭제 작업 |
| `getProject(pdfId)` | `suspend` | ✅ 적절 | 특정 프로젝트 일회성 조회 |
| `detectPii(...)` | `suspend` | ✅ 적절 | 일회성 PII 탐지 |
| `detectPiiInAllPages(...)` | `suspend` | ✅ 적절 | 일회성 전체 페이지 PII 탐지 |
| `getOutline(file)` | `suspend` | ✅ 적절 | 일회성 목차 추출 |

---

### 2. SettingsRepositoryImpl

| 함수/프로퍼티 | 타입 | 평가 | 설명 |
|------|------|------|------|
| `isProEnabled` | `Flow<Boolean>` | ✅ 적절 | Pro 상태 변경을 **실시간 관찰** |
| `checkFirstLaunch()` | `suspend` | ✅ 적절 | 앱 시작 시 **한 번만** 확인 |
| `setProEnabled(...)` | `suspend` | ✅ 적절 | 일회성 설정 변경 |
| `setFirstLaunch(...)` | `suspend` | ✅ 적절 | 일회성 설정 변경 |
| `getAppUuid()` | `suspend` | ✅ 적절 | UUID는 한 번 가져오면 끝 (변경 없음) |

---

### 3. RedeemRepositoryImpl

| 함수 | 타입 | 평가 | 설명 |
|------|------|------|------|
| `validateCode(...)` | `suspend` | ✅ 적절 | API 호출 후 결과 반환하는 일회성 작업 |

---

### 4. RemoteRedactionRepositoryImpl

| 함수 | 타입 | 평가 | 설명 |
|------|------|------|------|
| `redactPdf(...)` | `suspend` | ✅ 적절 | 서버 API 호출 후 파일 반환하는 일회성 작업 |

---

## 선택 기준

### Suspend 함수를 사용하는 경우
- **"한 번 가져오면 끝"** 인 작업
- API 호출, 파일 저장/로드, 일회성 DB 쿼리

```kotlin
// 예시: 단일 데이터 요청
suspend fun getUser(id: String): User {
    return api.fetchUser(id)
}
```

### Flow를 사용하는 경우
- **"계속 지켜보다가 변경되면 알려줘"** 인 작업
- 실시간 데이터 관찰, 설정값 변경 감지, UI 자동 업데이트

```kotlin
// 예시: 실시간 데이터 관찰
fun observeUsers(): Flow<List<User>> {
    return database.getAllUsers() // Room DAO의 Flow 반환
}
```

---

## 결론

프로젝트의 모든 리포지터리 함수가 용도에 맞게 적절히 구현되어 있습니다.

- `getRecentProjects()` → `Flow`: 프로젝트 추가/삭제 시 홈 화면 자동 업데이트
- `isProEnabled` → `Flow`: Pro 상태 변경 시 앱 전체에 즉시 반영
- 나머지 함수들 → `suspend`: 일회성 작업으로 적절
