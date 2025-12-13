# 스낵바 표시 중 사이드 이펙트 블로킹 문제

## 문제 상황

`EditorScreen`에서 스낵바가 표시되는 동안 파일 저장 런처(`ActivityResultContracts.CreateDocument`)가 실행되지 않는 문제가 발생했습니다.

## 원인 분석

`LaunchedEffect` 블록 내에서 `sideEffect`를 수집할 때, `showSnackbar()`가 **suspend 함수**이기 때문에 스낵바가 표시되고 사라질 때까지 다음 사이드 이펙트 처리가 블로킹됩니다.

### 문제 코드

```kotlin
LaunchedEffect(Unit) {
    viewModel.sideEffect.collect { effect ->
        when (effect) {
            is EditorSideEffect.OpenSaveLauncher -> {
                saveLauncher.launch(fileName)
            }
            is EditorSideEffect.ShowSnackbar -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                // ❌ 이 함수가 완료될 때까지 다음 사이드 이펙트 처리가 블로킹됨
                snackbarHostState.showSnackbar(effect.message, duration = SnackbarDuration.Short)
            }
            // ...
        }
    }
}
```

`showSnackbar()`는 스낵바가 표시되고 **사라질 때까지 suspend** 상태를 유지합니다. 따라서 스낵바가 표시되는 동안 `collect` 블록이 다른 사이드 이펙트를 처리하지 못합니다.

## 해결 방법

`showSnackbar()` 호출을 별도의 `launch` 블록으로 분리하여 비동기로 실행합니다.

### 수정된 코드

```kotlin
LaunchedEffect(Unit) {
    viewModel.sideEffect.collect { effect ->
        when (effect) {
            is EditorSideEffect.OpenSaveLauncher -> {
                saveLauncher.launch(fileName)
            }
            is EditorSideEffect.ShowSnackbar -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                // ✅ 별도 코루틴에서 실행하여 블로킹 방지
                launch {
                    snackbarHostState.showSnackbar(effect.message, duration = SnackbarDuration.Short)
                }
            }
            // ...
        }
    }
}
```

## 핵심 포인트

| 항목 | 수정 전 | 수정 후 |
|------|---------|---------|
| 실행 방식 | 순차적 (Sequential) | 비동기 (Concurrent) |
| 블로킹 여부 | 스낵바 종료까지 블로킹 | 즉시 다음 이펙트 처리 가능 |
| 코루틴 스코프 | `LaunchedEffect` 스코프 직접 사용 | `launch`로 새 코루틴 생성 |

## 추가 고려사항

- `launch`를 사용하면 스낵바가 표시되는 동안에도 다른 사이드 이펙트가 즉시 처리됩니다.
- 기존 스낵바를 `dismiss()`한 후 새 스낵바를 표시하므로, 스낵바 중복 표시 문제는 발생하지 않습니다.
- `LaunchedEffect`의 `CoroutineScope`에서 `launch`를 호출하므로, 컴포저블이 dispose될 때 자동으로 취소됩니다.

## 관련 파일

- [EditorScreen.kt](file:///d:/comon/PDF-Redactor-M/feature/editor/src/main/java/org/comon/pdfredactorm/feature/editor/EditorScreen.kt)
