# 로깅 시스템 가이드

PDF Redactor M 애플리케이션의 로깅 시스템에 대한 문서입니다. Clean Architecture 원칙을 준수하며, 효율적인 디버깅과 모니터링을 위해 설계되었습니다.

## 1. 아키텍처 (Clean Architecture)

로깅 시스템은 Clean Architecture의 의존성 규칙을 따르도록 설계되었습니다.

### Domain Layer
- **`Logger` 인터페이스**: 로깅 기능을 추상화한 인터페이스입니다.
- 플랫폼(Android)에 독립적이며, 비즈니스 로직(UseCase)이나 Repository 인터페이스에서 사용됩니다.

### Data Layer
- **`AndroidLogger` 구현체**: `Logger` 인터페이스를 구현하며, Android의 `Log` API를 사용합니다.
- 실제 로깅이 수행되는 곳이며, 향후 다른 로깅 라이브러리(Crashlytics 등)로 교체하기 용이합니다.

### DI (Dependency Injection)
- Hilt를 통해 `AppModule`에서 `Logger` 인터페이스에 `AndroidLogger` 구현체를 바인딩하여 주입합니다.

## 2. 로깅 전략: Single Point Logging

중복 로깅을 방지하고 로그의 명확성을 높이기 위해 **단일 지점 로깅(Single Point Logging)** 전략을 채택했습니다.

### ✅ Data Layer (Repository) - 상세 로깅
- **역할**: 실제 작업이 수행되고 에러가 발생하는 지점입니다.
- **내용**: 작업 시작/완료, 데이터 처리 결과, **상세한 에러 내용 및 스택 트레이스**를 기록합니다.
- **예시**:
  ```kotlin
  logger.info("Loading PDF: /path/to/file.pdf")
  logger.error("Failed to load PDF", exception)
  ```

### ❌ Domain Layer (UseCase) - 로깅 생략
- **역할**: 비즈니스 로직을 캡슐화하고 데이터를 전달합니다.
- **내용**: Repository에서 이미 상세 로깅을 수행했으므로, 중복을 피하기 위해 별도의 로깅을 하지 않습니다.

### ⚠️ Presentation Layer (ViewModel) - 컨텍스트 로깅
- **역할**: UI와 상호작용하고 사용자 액션을 처리합니다.
- **내용**: **사용자의 의도(액션)**와 작업의 성공/실패 여부만 간단히 기록합니다. 상세한 에러 로그는 남기지 않습니다.
- **예시**:
  ```kotlin
  logger.info("User opened PDF document: doc-123")
  logger.warning("PDF save failed") // 상세 에러는 Repository 로그 참조
  ```

## 3. 태그 규칙 (Tagging)

모든 로그는 **`PDFLogger`** 라는 단일 태그를 사용합니다.

- **이유**: 클래스별로 태그를 달리하면 필터링이 번거로울 수 있습니다. 단일 태그를 사용함으로써 `tag:PDFLogger` 필터 하나로 앱의 모든 주요 로그를 순서대로 확인할 수 있습니다.
- **형식**: `logger.info("Message")` (내부적으로 `Log.i("PDFLogger", "Message")` 호출)

## 4. 향후 계획 (Crashlytics)

현재는 `AndroidLogger`가 Android의 기본 `Log` API를 사용하고 있습니다. 추후 프로덕션 배포 시에는 이를 **Firebase Crashlytics** 등으로 대체할 예정입니다.

### 교체 방법
Clean Architecture 덕분에 기존 코드를 수정할 필요 없이 Data Layer의 구현체만 변경하면 됩니다.

1. `CrashlyticsLogger` 클래스 생성 (`Logger` 인터페이스 구현)
2. `error()` 메서드에서 `FirebaseCrashlytics.getInstance().recordException(throwable)` 호출
3. `AppModule`에서 `provideLogger()`가 `CrashlyticsLogger`를 반환하도록 수정

이렇게 하면 Domain이나 Presentation 레이어의 코드를 전혀 건드리지 않고 로깅 시스템을 업그레이드할 수 있습니다.
