# Google Play In-App Updates SDK 개발 문서

이 문서는 PDF-Redactor-M 앱에 적용된 Google Play In-App Updates SDK의 구현 상세와 활용 방법을 설명합니다.

## 개요
사용자가 항상 최신 버전의 앱을 사용하도록 유도하여 새로운 기능 제공 및 버그 수정을 보장합니다.

## 주요 기능
- **Immediate Update (즉시 업데이트)**: 전체 화면으로 업데이트를 강제하며, 업데이트 완료 전에는 앱을 사용할 수 없습니다. (현재 앱에 적용된 방식)

## 구현 상세

### 1. 의존성 설정
`libs.versions.toml` 및 `app/build.gradle.kts`에 아래 라이브러리가 추가되었습니다.
- `com.google.android.play:app-update`
- `com.google.android.play:app-update-ktx`

### 2. InAppUpdateManager
업데이트 로직을 캡슐화한 클래스(`org.comon.pdfredactorm.ui.InAppUpdateManager`)를 사용합니다. `checkForUpdates`는 비동기로 동작하며 완료 시점을 알리는 콜백을 지원합니다.

```kotlin
// 업데이트 체크 및 시작 (onComplete 콜백 지원)
inAppUpdateManager.checkForUpdates(activity, updateLauncher) {
    // 체크 완료 후 실행될 로직
}
```

### 3. MainActivity 통합 및 스플래시 연동
`MainActivity`에서 `isUpdateCheckComplete` 상태를 관리하여 스플래시 화면이 떠 있는 동안 업데이트 체크가 완료되기를 기다립니다.

```kotlin
splashScreen.setKeepOnScreenCondition {
    // ViewModel 로딩과 업데이트 체크가 모두 완료될 때까지 스플래시 유지
    mainViewModel.isLoading.value || !isUpdateCheckComplete
}
```

## 테스트 방법

### Play Store 등록 앱 테스트
1. 앱이 Google Play 스토어에 등록되어 있어야 합니다.
2. 테스트하려는 기기에 현재 설치된 버전보다 높은 버전이 스토어에 업로드되어 있어야 합니다.
3. 동일한 패키지 이름과 서명(Signing)을 사용해야 합니다.

### FakeAppUpdateManager (로컬 테스트용)
단위 테스트나 로컬 UI 테스트 시에는 `FakeAppUpdateManager`를 주입하여 업데이트 시나리오를 시뮬레이션할 수 있습니다.

## 주의사항
- **업데이트 우선순위**: 현재 구현은 `Immediate`가 가능하면 우선적으로 실행합니다. 비즈니스 로직에 따라 우선순위를 조정할 수 있습니다.
- **취소 처리**: 사용자가 업데이트를 취소했을 때의 앱 동작(종료 여부 등)을 정책에 따라 결정해야 합니다.
- **네트워크**: Wi-Fi 권장 등의 안내는 Google Play 시스템이 자동으로 처리합니다.
