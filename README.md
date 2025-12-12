# PDF Redactor M

안드로이드 환경에서 PDF 파일의 개인정보(PII)를 탐지하고 마스킹(Redaction)할 수 있는 애플리케이션입니다.

## 📱 프로젝트 개요

**PDF Redactor M**은 Clean Architecture 기반의 멀티 모듈 안드로이드 애플리케이션으로, PDF 파일 내 개인정보(PII)를 안전하게 탐지하고 마스킹(Redaction)할 수 있는 전문 도구입니다.

### 핵심 가치
- **개인정보 보호**: 정규식 기반 자동 PII 탐지 및 영구 마스킹
- **오프라인 우선**: 네트워크 없이도 로컬에서 완전한 PDF 편집 가능
- **전문가급 기능**: 색상 선택, 스포이드, 목차 탐색 등 고급 편집 도구 제공
- **확장 가능한 아키텍처**: SOLID 원칙과 Clean Architecture로 유지보수성 극대화

### 모듈 구조

프로젝트는 **11개 모듈**로 구성된 멀티 모듈 아키텍처를 채택하고 있습니다:

#### 📦 App Layer
- **`app`**: 메인 애플리케이션 모듈, DI 설정, 네비게이션 통합

#### 🔧 Core Layer (9개 모듈)
- **`core:model`**: 도메인 모델 정의 (PdfDocument, RedactionMask, PdfOutlineItem, DetectedPii)
- **`core:common`**: 공통 Logger 인터페이스 및 유틸리티
- **`core:domain`**: UseCase 및 Repository 인터페이스 (순수 Kotlin, 플랫폼 독립적)
- **`core:data`**: Repository 구현체, PII 탐지 로직, PdfBox 초기화
- **`core:database`**: Room 기반 로컬 데이터베이스 (작업 내역 영구 저장)
- **`core:network`**: Retrofit 기반 네트워크 레이어 (리딤 코드 검증, 원격 마스킹 API)
- **`core:datastore`**: DataStore 기반 설정 저장소 (Pro 상태, 최초 실행 플래그)
- **`core:designsystem`**: Material3 테마, 타이포그래피, 색상 시스템
- **`core:ui`**: 공통 UI 컴포넌트, 광고 컴포넌트, 재사용 가능한 다이얼로그

#### 🎨 Feature Layer (2개 모듈)
- **`feature:home`**: 홈 화면 (PDF 목록, 리딤 코드 입력, 종료 다이얼로그)
- **`feature:editor`**: PDF 편집기 (마스킹, 뷰어, 색상 선택, PII 탐지, 목차 탐색)

### 주요 기능

#### 📄 핵심 PDF 기능
- Android PdfRenderer 기반 고성능 뷰어
- 화면 크기 자동 스케일링 및 확대/축소
- 페이지 직접 이동 (다이얼로그 입력)
- PDF 목차(북마크) 계층 구조 탐색

#### 🎯 마스킹 기능
- **수동 마스킹**: 터치 드래그로 영역 선택, 탭하여 삭제
- **자동 PII 탐지**: 정규식 기반 전화번호, 주민번호, 이메일 등 자동 감지
- **색상 커스터마이징**: 
  - 흰색/검은색 선택
  - 스포이드 도구로 PDF에서 색상 추출
  - 실시간 확대경 UI 제공
- **두 가지 처리 방식**:
  - 로컬 마스킹 (PdfBox-Android 사용)
  - 원격 마스킹 (Pro 기능, 서버 API 연동)

#### 👑 Pro 기능
- 리딤 코드 기반 Pro 버전 활성화
- 서버 기반 고성능 PDF 마스킹 처리
- DataStore를 통한 영구 상태 관리
- 전용 UI (왕관 아이콘, 활성화 다이얼로그)

#### 🎯 UX/UI 기능
- **다국어 지원**: 한국어/영어 자동 전환 (시스템 언어 기반)
- **도움말 시스템**: ViewPager 기반 6페이지 사용 설명서
- **사용자 피드백**: Snackbar를 통한 실시간 상태 알림
- **광고 통합**: AdMob 배너/네이티브 광고, 종료 시 리뷰 유도
- **Edge-to-Edge UI**: 상태바 가시성 최적화

#### 📊 데이터 관리
- Room DB 기반 작업 내역 자동 저장
- 임시 파일 자동 정리
- 원본 파일명 기반 스마트 저장 제안

#### 🛡️ 안정성 및 모니터링
- Firebase Crashlytics 및 Analytics 통합
- Clean Architecture 기반 에러 처리
- 로컬/원격 로깅 시스템

## 🛠 기술 스택
- **Language**: Kotlin 2.2.21
- **UI**: Jetpack Compose (Material3)
- **Architecture**: Clean Architecture + MVI
- **DI**: Hilt
- **Async**: Coroutines + Flow
- **Local DB**: Room
- **Network**: Retrofit
- **PDF Engine**: PdfBox-Android (Primary), Android PdfRenderer (Viewer)

## 📂 문서
- [개발 계획서](docs/development_plan.md)
- [PII 탐지 가이드](docs/pii_detection.md)
- [로깅 시스템 가이드](docs/logging.md)
- [Analytics 시스템 가이드](docs/analytics.md)

## 갤러리
<table>
    <tbody>
    <tr><th colspan="3">pro 기능 활성화 흐름도</th></tr>
        <tr>
            <td colspan="3">
                <img width="750px" src="https://github.com/user-attachments/assets/10e85815-806d-4c40-bdb7-ed9ec12c3ecc" />
            </td>
        </tr>
    <tr><th colspan="3">마스킹 기능 아키텍처</th></tr>
        <tr>
            <td colspan="3">
                <img width="750px" src="https://github.com/user-attachments/assets/69102339-195b-4f71-be5f-4e7fedc9ae8b" />
            </td>
        </tr>
    <tr><th colspan="3">마스킹 기능 흐름도</th></tr>
        <tr>
            <td colspan="3">
                <img width="750px" src="https://github.com/user-attachments/assets/7a33fed8-ff73-4cbc-92df-4c47846d36b7" />
            </td>
        </tr>
        <tr>
            <th>마스킹</th>
            <th>마스킹 취소</th>
            <th>리딤코드 입력</th>
        </tr>
        <tr>
            <td><img width="250px" src="https://github.com/user-attachments/assets/e0ca5b79-0bcd-45c3-a67b-252e7f486025" /></td>
            <td><img width="250px" src="https://github.com/user-attachments/assets/b62abc26-60ec-4e99-9470-947b7fbdaa84" /></td>
            <td><img width="250px" src="https://github.com/user-attachments/assets/55d85740-0456-4608-800c-2fadf4d15292" /></td>
        </tr>
        <tr>
            <th>기본색상 마스킹 결과</th>
            <th>컬러피커 기능</th>
            <th>컬러피커 사용 결과물</th>
        </tr>
        <tr>
            <td><img width="250px" src="https://github.com/user-attachments/assets/32a929c1-991c-4674-bedc-2404738cd240" /></td>
            <td><img width="250px" src="https://github.com/user-attachments/assets/bf90fa9c-d24a-42e2-9d70-dfa1c55ab776" /></td>
            <td><img width="250px" src="https://github.com/user-attachments/assets/730442b9-17f9-4279-a715-5d31f4272cac" /></td>
        </tr>
    </tbody>
</table>

## 🚀 현재 진행 상황
- [x] 프로젝트 초기화 및 Gradle 설정 (Kotlin 2.2.21, Version Catalog)
- [x] 개발 계획서 및 기술 스택 선정 완료
- [x] Clean Architecture 레이어 구현 (Domain, Data, Presentation)
- [x] PDF 파일 불러오기 및 렌더링 (PdfRenderer)
- [x] 마스킹 UI 및 로직 구현 (터치 드래그, 삭제)
- [x] 마스킹 된 PDF 저장 기능 (PdfBox, System File Picker)
- [x] 작업 내역 자동 저장 (Room DB)
- [x] PII 자동 탐지 로직 구현 (정규식 기반)
- [x] PDF 저장 후 임시 파일 자동 삭제 및 홈 화면 복귀 로직 구현
- [x] 홈 화면 배경 아이콘 추가 및 UI 개선
- [x] 도움말 다이얼로그 구현 (ViewPager, 6페이지 사용 설명서)
- [x] 상태바 가시성 개선 (Edge-to-Edge 적용, 밝은 배경에서 어두운 아이콘 표시)
- [x] 다국어 지원 구현 (한국어/영어, 시스템 언어 기반 자동 전환)
- [x] 도움말 다이얼로그 세로 스크롤 기능 추가 (텍스트 오버플로우 방지)
- [x] 마스킹 토글 UI 개선: 아이콘을 지우개(고무지우개) 형태로 교체하고, 스위치 컴포넌트를 추가해 토글 상태를 명확히 구분
- [x] 파일명 텍스트 크기 축소: 에디터 화면 상단 파일명 텍스트 크기를 절반(≈11sp)으로 감소
- [x] 도움말 페이지 3 업데이트: 마스킹 토글 버튼 사용 방법을 추가 ("먼저 하단의 마스킹 토글 버튼을 클릭합니다" 및 "마스킹이 완료되면 토글 버튼을 다시 클릭하여 마스킹 모드를 해제합니다")
- [x] PDF 뷰어 UX 개선: 화면 높이에 맞춰 자동 스케일링, 왼쪽 상단 정렬, 초기 상태에서 드래그 비활성화, 확대 시 이동 범위 제한
- [x] 페이지 이동 기능 추가: 하단 페이지 표시를 클릭 가능한 TextField로 변경, 다이얼로그를 통한 직접 페이지 입력 및 유효성 검증
- [x] 목차(북마크) 이동 기능 구현: PDFBox를 활용한 PDF 목차 추출, 계층 구조 지원, 목차 항목 클릭 시 해당 페이지로 즉시 이동
- [x] 마스킹 색상 선택 기능 구현: 흰색/검은색 중 선택 가능한 색상 다이얼로그, 현재 색상 표시 버튼 추가, 마스킹 영역에 검은색 외곽선(2px) 추가로 가시성 개선, Database 마이그레이션 (v1→v2)
- [x] 로깅 시스템 구축: Clean Architecture 기반 Logger 구현, 주요 에러 발생 지점(Repository) 및 사용자 액션(ViewModel) 로깅 추가
- [x] PDF 뷰어 스케일링 개선: 문서의 가로/세로 비율과 관계없이 전체 내용이 화면에 보이도록 `fitScale` 로직 수정 (너비/높이 중 더 작은 스케일 적용)
- [x] 원본 PDF에서 마스킹 색상 추출 (스포이드) 기능 구현 완료: 색상 선택 다이얼로그에 'PDF에서 색상 추출' 옵션 추가, 확대경 UI 제공, 제스처 충돌 방지 처리
- [x] 사용자 피드백 강화: 마스킹 모드 토글 및 색상 선택(다이얼로그/스포이드) 시 스낵바(Snackbar)를 통해 상태 변경 알림 제공
- [x] 임시 파일명 개선: 임시 파일 생성 시 숫자 대신 파일명 형식 사용 `temp_<원본파일명>.pdf`
- [x] 마스킹 삭제 기능 개선: 탭하여 삭제 확인 대화상자 표시로 사용성 개선 및 삭제 내용 DB 동기화(지속성) 문제 해결
- [x] 앱 최초 실행 시 도움말 다이얼로그 자동 표시: SharedPreferences를 활용하여 최초 실행 여부를 확인하고, 첫 실행 시에만 자동으로 도움말을 띄워 사용자 편의성 증대
- [x] Firebase Crashlytics 통합: 앱 안정성 모니터링을 위한 Crashlytics 및 Analytics 연동
- [x] 앱 종료 확인 다이얼로그 구현: 홈 화면에서 뒤로가기 시 종료 여부를 묻는 다이얼로그 표시, AdMob Native 광고 통합, "종료"/"취소"/"리뷰남기기" 버튼 제공, 다국어 지원 및 UX 개선 (광고 로딩 중 프로그래스바 표시)
- [x] 하단 배너 광고 UI 개선: 네비게이션 바가 있는 기기에서 배너 광고가 가려지지 않도록 패딩(`navigationBarsPadding`) 적용
- [x] Pro PDF 마스킹 기능 구현: 서버 API(`POST /api/pdf/redact/`)를 연동하여 PDF 파일과 마스킹 좌표를 전송하고 처리된 파일을 수신하는 기능 추가
- [x] Pro 기능 파일 저장 흐름 개선: 처리된 PDF 수신 시 시스템 파일 저장 대화상자(Save As)를 띄워 사용자 지정 위치에 저장, 저장 후 임시 파일 및 원본 프로젝트 자동 삭제
- [x] 에디터 화면 상단바(TopBar) 리팩토링: 기존 개별 액션 버튼들을 하나의 메뉴 버튼(List 아이콘)으로 통합, "저장", "목차", "개인정보 탐지" 메뉴 제공
- [x] 종료 다이얼로그 수정: 광고 로딩 중 종료 버튼 비활성화하여 광고 노출 보장
- [x] 개인정보 탐지 결과 다이얼로그 구현: 탐지 완료 후 탐지된 항목 수를 다이얼로그로 표시
- [x] 홈 화면 상단바에 커피챗(후원) 버튼 추가: 클릭 시 후원 페이지로 이동
- [x] Pro 기능 활성화 구현: 리딤 코드 검증 API 연동, DataStore 상태 관리, 스플래시 화면 및 전용 UI(왕관 아이콘, 다이얼로그) 통합
- [x] 멀티 모듈 리팩토링: `:core:model` 및 `:core:common` 모듈 분리, 모델 클래스(PdfDocument, RedactionMask 등) 및 Logger 인터페이스/구현체 모듈화, 네임스페이스 규칙 적용(`org.comon.pdfredactorm.core.*`)
- [x] 데이터 소스 모듈 분리: `:core:database`(Room), `:core:network`(Retrofit/API/DTO), `:core:datastore`(DataStore) 모듈화 및 Hilt DI 모듈 구성
- [x] Domain/Data 레이어 분리: `:core:domain`(순수 Kotlin - UseCase/Repository 인터페이스), `:core:data`(Repository 구현체/PII 로직) 분리, `:core:common` 순수 Kotlin 모듈화, Hilt `@Binds` DI 구성
- [x] UI/DesignSystem 분리: `:core:designsystem`(테마/타이포그래피), `:core:ui`(광고 컴포넌트/공통 다이얼로그) 모듈화, 광고 ID 파라미터화로 BuildConfig 의존성 제거
- [x] Feature 모듈 분리: `:feature:home`(홈 화면), `:feature:editor`(PDF 편집기) 모듈화, `HomeScreenConfig` 도입으로 app 의존성 분리, 각 모듈 자체 리소스(strings/drawable) 관리
- [x] 네비게이션 리팩토링: `NavGraphBuilder` 확장함수 패턴 적용, Feature별 Route/Navigation 분리, `AppNavHost`로 통합
- [x] Navigation3 마이그레이션: `NavDisplay`/`rememberNavBackStack`/`entryProvider` 적용, `@Serializable` NavKey 클래스 도입, 기존 Navigation Compose에서 Navigation3(1.0.0)으로 전환
- [x] 버그 수정: 로컬 마스킹 저장 후 파일을 다시 열 때 에디터가 바로 닫히는 문제 해결 (`saveSuccess` 상태 초기화 로직 추가)
- [x] 뷰모델 생명주기 관리 개선: `ScopedViewModelContainer`(`core:ui`)를 도입하여 Editor 화면 진입 시마다 독립적인 `ViewModelScope`를 생성, 마스킹 색상/모드 등 이전 상태가 남지 않도록 개선 및 안정성 확보
- [x] 리팩토링: SharedPreferences를 DataStore로 마이그레이션 (`First Launch` 플래그), `Clean Architecture` 원칙에 따라 모든 ViewModel(`Home`, `Main`, `Editor`)에서 Repository 직접 사용을 UseCase로 대체
- [x] 저장 로직 리팩토링: Pro/일반 저장의 프로세스 통합(`performRedaction` -> `saveFinalDocument` 파이프라인), 입출력 로직을 File 기반으로 변경하여 유연성 확보
- [x] UI 상태 관리 개선: ViewModel-driven 아키텍처 강화, SideEffect 패턴 도입으로 일회성 UI 이벤트(파일 피커 표시 등) 처리 구조화
- [x] 사용성 개선: 저장 시 원본 파일명을 프리픽스(`redacted_` / `pro_redacted_`)와 함께 자동 제안
- [x] 다국어 지원 강화: Home/Editor 화면 내 하드코딩 텍스트 전면 리소스화(KO/EN), 다국어 대응 완료
- [x] 컴포저블 리팩토링: HomeScreen 및 EditorScreen의 보조 컴포저블(Dialog, Item 등)을 개별 파일로 분리하고 `component` 패키지로 구조화하여 유지보수성 향상
- [x] 버그 수정 및 리팩토링: PdfBox 초기화 로직 복원(`PdfRepositoryInitializer`) 및 `core:data` 모듈로 캡슐화하여 Clean Architecture 원칙 준수 (앱 크래시 해결)
- [x] 릴리즈 빌드 최적화: R8 난독화(`minifyEnabled`) 및 리소스 축소(`shrinkResources`) 설정 활성화, Proguard 규칙 추가(PdfBox, Retrofit, Room, Network DTO 등 예외 처리)로 앱 보안성 및 용량 최적화
- [x] Editor 화면 MVI 패턴 적용: `EditorIntent` sealed interface 도입, `handleIntent()` 단일 진입점으로 사용자 행동 처리, 스낵바 메시지 Channel SideEffect 통합, `collectAsStateWithLifecycle` 적용으로 Lifecycle 인식 상태 수집
- [x] DI 모듈 리팩토링: `AppModule` 제거, BuildConfig 설정을 각 모듈로 분산 (`core:network` - API_BASE_URL, `core:data` - API 키/DEBUG), Qualifier 어노테이션 제거로 DI 단순화
- [x] 클린 아키텍처 개선: `RedeemRepositoryImpl`에서 `SettingsRepository` 의존성 제거, 비즈니스 로직("검증 성공 시 Pro 활성화")을 `ValidateCodeUseCase`로 이동하여 레이어 책임 명확화
- [x] API Key Interceptor 리팩토링: API 키 관리를 `core:data`에서 `core:network`로 이동, `ApiKeyInterceptor`를 통한 헤더 자동 추가, API별 OkHttpClient 분리 및 Qualifier 적용으로 Repository의 인프라 관심사 제거
- [x] 로깅 시스템 확장: `RedeemRepositoryImpl`, `RemoteRedactionRepositoryImpl`, `SettingsRepositoryImpl`, `MainViewModel`, `ApiKeyInterceptor`에 Logger 주입 추가
- [x] Repository 함수 타입 리팩토링: `isFirstLaunch`를 `Flow` 프로퍼티에서 `suspend fun checkFirstLaunch()`로 변경 (일회성 조회에 적합한 타입으로 개선, 클린코드 명명 규칙 적용)
- [x] Home Feature MVI 리팩토링: `HomeContract.kt` 신규 생성(`HomeUiState`, `HomeEvent`, `HomeSideEffect`), `HomeViewModel` 단일 `uiState` 및 `onEvent()` 패턴 적용, `HomeScreen` `collectAsStateWithLifecycle` 적용 및 다이얼로그 상태 관리 정책 정리(비즈니스 로직은 ViewModel, UI 토글은 Screen에서 관리)
- [x] API 에러 응답 DTO 구조화: `ApiErrorResponseDto` 및 `ApiErrorParser` 유틸리티 클래스 추가(`core:network`), Repository에서 JSON 직접 파싱 제거 및 에러 메시지 파싱 로직 캡슐화
- [x] Google Analytics 이벤트 추적 시스템 구현: `core:common`에 `AnalyticsTracker` 인터페이스, `core:data`에 `FirebaseAnalyticsTracker` 구현체 생성, Logger와 동일한 Clean Architecture 패턴 적용, 5개 이벤트 추적(`pro_activated`, `open_coffeechat`, `pii_detected_current_page`, `pii_detected_all_pages`, `mask_saved`)
- [x] 종료 다이얼로그 네이티브 광고 사전 로딩: `HomeViewModel`에서 홈 화면 진입 시 광고를 미리 로드하여 종료 다이얼로그 표시 시 즉시 광고 노출 및 종료 버튼 활성화, `NativeAdView` Composable을 `preloadedAd` 파라미터 기반으로 리팩토링

## 추후 작업(개선) 사항
- [ ] 스플래시 스크린 구버전 대응 + Non-Blocking Splash Screens 알아보기
