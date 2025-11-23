# PDF 개인정보 마스킹 앱 - 개발 계획서

## 1. 프로젝트 개요
**목표**: 사용자가 PDF 파일을 열어 개인정보(PII)를 탐지하고 마스킹/삭제할 수 있는 안드로이드 애플리케이션 개발.
**타겟 OS**: Android

## 2. 기술 스택 및 아키텍처
요청된 사양을 엄격히 준수합니다:

- **언어**: Kotlin
- **UI 프레임워크**: Jetpack Compose
- **아키텍처 패턴**: Clean Architecture (Presentation, Domain, Data 레이어)
- **상태 관리**: MVVM (Model-View-ViewModel) 또는 MVI
- **의존성 주입**: Hilt
- **네트워크**: Retrofit (향후 API 연동 또는 원격 OCR을 위해 예약)
- **네비게이션**: Jetpack Compose Navigation
- **로컬 데이터베이스**: Room (SQLite 추상화) - 기록 또는 설정 저장용.
- **PDF 처리**:
    - **뷰어**: Android `PdfRenderer` (기본 내장, 빠름).
    - **조작/저장**: `PdfBox-Android` (Apache 2.0, 무료) 또는 `Android PdfDocument` (Flattening 방식).

## 3. 아키텍처 구조

### 3.1 Domain Layer (Pure Kotlin)
비즈니스 로직을 포함하며 안드로이드 프레임워크와 독립적입니다.
- **모델**: `PdfDocument`, `RedactionMask`, `PiiType`
- **리포지토리 (인터페이스)**: `PdfRepository`, `SettingsRepository`
- **유스케이스**:
    - `LoadPdfUseCase`
    - `AnalyzePageForPiiUseCase` (정규식 또는 ML 기반)
    - `SaveRedactedPdfUseCase`

### 3.2 Data Layer (Android/Java)
데이터 검색 및 저장을 처리합니다.
- **데이터베이스**: `RedactionHistory`를 위한 Room Database.
- **네트워크**: Retrofit 서비스 (클라우드 분석 필요 시).
- **리포지토리 구현**:
    - `PdfRepositoryImpl`: 파일 I/O 및 PDF 라이브러리 상호 작용 처리.
- **데이터 소스**: 로컬 파일 시스템, Room DB.

### 3.3 Presentation Layer (Android/Compose)
UI 및 사용자 상호 작용을 처리합니다.
- **ViewModels**: `HomeViewModel`, `EditorViewModel`
- **화면**:
    - `HomeScreen`: 최근 파일 목록, 새 PDF 선택 버튼.
    - `EditorScreen`: PDF 페이지 표시, 마스킹 그리기 또는 PII 자동 탐지 허용.
- **네비게이션**: 라우트를 정의하는 `NavHost`.

## 4. 주요 기능 및 워크플로우

### 4.1 UI 레이아웃
- **상단 메뉴**:
    - `뒤로가기`: 홈 화면으로 이동.
    - `로드`: 다른 PDF 파일 불러오기.
    - `저장`: 작업 내용을 새 PDF 파일로 내보내기.
- **메인 뷰**: PDF 렌더링 영역.
- **하단 탐색 메뉴**:
    - `이전/다음`: 페이지 이동.
    - `페이지 이동`: 페이지 번호 직접 입력하여 이동.
    - `목차`: PDF 목차 보기 및 이동.
    - `마스킹 토글`: 마스킹 모드 ON/OFF 전환.
    - `취소`: 마지막 작업 취소 (Undo).

### 4.2 인터랙션 로직
1.  **기본 모드 (마스킹 OFF)**:
    -   **제스처**: 드래그로 화면 이동(Panning), 핀치로 확대/축소(Zoom).
    -   **마스크 편집**: 이미 생성된 마스킹 영역을 길게 클릭(Long Press)하면 삭제 컨텍스트 메뉴 표시 -> '삭제' 선택 시 해당 마스킹 제거.
2.  **마스킹 모드 (마스킹 ON)**:
    -   **제스처**: 화면 드래그 시 해당 영역에 사각형 마스킹 생성.
    -   화면 이동 및 확대/축소 비활성화.

### 4.3 데이터 영속성 (Room DB)
- **자동 저장**: 작업 도중 앱이 종료되어도 다시 실행 시 마지막 작업 상태(열린 파일 경로, 현재 페이지, 생성된 마스킹 좌표들)를 복원.
- **작업 완료**: 작업이 완료되어 저장하거나 사용자가 명시적으로 닫을 때 DB에서 해당 작업 내용 삭제 가능.

## 5. 라이브러리 조사 및 선정
마스킹(Redaction) 기능을 구현하기 위해 다음 라이브러리들을 검토했습니다.

### 5.1 후보군
1.  **PdfBox-Android** (추천)
    -   **특징**: Apache PDFBox의 안드로이드 포팅 버전.
    -   **장점**: Apache 2.0 라이선스(무료, 상업적 이용 가능), 순수 Java/Kotlin, PDF 생성 및 조작 가능.
    -   **단점**: 원본 텍스트 데이터를 완전히 삭제하는 "True Redaction"은 구현이 복잡함. (단순 마스킹은 가능)
2.  **iText 7 (pdfSweep)**
    -   **특징**: 강력한 PDF 처리 및 보안 삭제(Redaction) 기능.
    -   **장점**: 확실한 데이터 삭제 기능 제공.
    -   **단점**: AGPL 라이선스 (앱 소스 공개 의무) 또는 상용 라이선스 구매 필요.
3.  **Android Native (PdfRenderer + Canvas)**
    -   **특징**: 안드로이드 기본 API 사용.
    -   **구현 방식**: PDF 페이지를 비트맵(이미지)으로 렌더링 -> 그 위에 검은 사각형 그리기 -> 다시 PDF로 저장.
    -   **장점**: 외부 라이브러리 의존성 없음, 이미지화되므로 원본 텍스트 완벽 차단(보안성 높음).
    -   **단점**: 파일 용량 증가, 텍스트 선택 불가, 화질 저하 가능성.

### 5.2 선정 결과
**PdfBox-Android**를 기본으로 사용하되, 보안 요구사항에 따라 **Native Flattening(이미지화)** 방식을 옵션으로 고려합니다.
- **이유**: 무료이며 라이선스 제약이 적고, PDF 조작에 유연함.

## 6. 개발 단계
1.  **설정**: Hilt, Compose, Room으로 프로젝트 초기화.
2.  **코어**: PDF 로딩 및 뷰어 구현 (Zoomable/Pannable).
3.  **UI/UX**: 상/하단 메뉴 및 마스킹 토글 로직 구현.
4.  **마스킹 로직**: 좌표 매핑 및 마스킹 그리기/삭제 구현.
5.  **영속성**: Room을 사용하여 실시간 상태 저장 및 복구 구현.
6.  **내보내기**: 마스킹이 적용된 최종 PDF 생성.
