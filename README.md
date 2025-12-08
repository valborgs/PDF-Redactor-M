# PDF Redactor M

안드로이드 환경에서 PDF 파일의 개인정보(PII)를 탐지하고 마스킹(Redaction)할 수 있는 애플리케이션입니다.

## 📱 프로젝트 개요
- **목표**: 모바일 기기에서 손쉽게 PDF 내 민감 정보를 가리고 안전하게 공유/저장.
- **주요 기능**:
    - PDF 파일 불러오기 및 뷰어
    - 정규식 기반 PII(전화번호, 주민번호 등) 자동 탐지 
    - 터치 드래그를 통한 수동 마스킹
    - 마스킹 된 PDF 내보내기 (Flattening/Redaction)
    - 작업 내역 자동 저장 (Room DB)
    - 다국어 지원 (한국어/영어 자동 전환)

## 🛠 기술 스택
- **Language**: Kotlin 2.2.21
- **UI**: Jetpack Compose (Material3)
- **Architecture**: Clean Architecture + MVVM
- **DI**: Hilt
- **Async**: Coroutines + Flow
- **Local DB**: Room
- **Network**: Retrofit (Future proofing)
- **PDF Engine**: PdfBox-Android (Primary), Android PdfRenderer (Viewer)

## 📂 문서
- [개발 계획서](docs/development_plan.md)
- [PII 탐지 가이드](docs/pii_detection.md)
- [로깅 시스템 가이드](docs/logging.md)

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

## 추후 작업(개선) 사항
- [ ] (없음)