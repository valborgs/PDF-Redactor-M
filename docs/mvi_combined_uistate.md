# Combine과 StateIn을 활용한 MVI UiState 패턴

## 개요

MVI(Model-View-Intent) 패턴에서 여러 개의 개별 `Flow`를 **단일 UI 상태 객체**로 합치는 방법을 설명합니다.

## 코드 예시

```kotlin
val uiState: StateFlow<HomeUiState> = combine(
    _isLoading,                  // MutableStateFlow<Boolean>
    _isFirstLaunch,              // MutableStateFlow<Boolean>
    getRecentProjectsUseCase(),  // Flow<List<PdfDocument>>
    getProStatusUseCase()        // Flow<Boolean>
) { isLoading, isFirstLaunch, recentProjects, isProEnabled ->
    HomeUiState(
        isLoading = isLoading,
        isFirstLaunch = isFirstLaunch,
        recentProjects = recentProjects,
        isProEnabled = isProEnabled
    )
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = HomeUiState()
)
```

---

## 핵심 연산자

### `combine()`

| 특징 | 설명 |
|-----|------|
| 동작 | 여러 Flow를 합쳐서 **어느 하나라도 변경되면** 새로운 값 emit |
| 반환 타입 | `Flow<R>` (Cold Flow) |
| 사용 시점 | 여러 데이터 소스를 하나의 상태로 합칠 때 |

```kotlin
// 예: A, B 중 하나라도 변경되면 새 Pair 생성
combine(flowA, flowB) { a, b -> Pair(a, b) }
```

> [!NOTE]
> `combine`은 **모든 소스가 최소 한 번 emit**해야 첫 번째 결과를 생성합니다.

### `stateIn()`

Cold Flow를 Hot `StateFlow`로 변환합니다.

| 파라미터 | 설명 |
|---------|------|
| `scope` | Flow가 공유되는 CoroutineScope |
| `started` | 공유 시작 전략 |
| `initialValue` | 구독 시작 전 초기값 |

---

## SharingStarted 전략

| 전략 | 동작 | 사용 사례 |
|-----|------|----------|
| `Eagerly` | 즉시 시작, scope 종료까지 유지 | 항상 최신 데이터 필요 |
| `Lazily` | 첫 구독자 등장 시 시작, scope 종료까지 유지 | 한 번 시작하면 계속 필요 |
| `WhileSubscribed(timeout)` | 구독자 있을 때만 활성, timeout 후 중단 | **UI에서 가장 권장** |

### WhileSubscribed(5000) 동작 흐름

```
구독자 등록 → 업스트림 Flow 활성화
    ↓
구독자 해제 → 5초 대기
    ↓
5초 내 재구독 → 캐시된 값 즉시 반환
5초 경과 → 업스트림 중단, 리소스 해제
```

> [!TIP]
> 5초 timeout은 **화면 회전** 같은 짧은 Configuration Change 시에도 Flow를 유지해줍니다.

---

## 장점

1. **단일 상태 관리**: UI에서 `uiState` 하나만 구독
2. **자동 갱신**: 어느 소스든 변경되면 자동으로 새 상태 생성
3. **Lifecycle-aware**: `WhileSubscribed`로 불필요한 리소스 사용 방지
4. **테스트 용이**: 단일 상태 객체로 테스트 검증 간소화

---

## 참고

- [Kotlin Flow 공식 문서](https://kotlinlang.org/docs/flow.html)
- [StateFlow and SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)
