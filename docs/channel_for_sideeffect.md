# ViewModel에서 SideEffect에 Channel을 사용하는 이유

## 개요

MVI 아키텍처에서 SideEffect(일회성 이벤트)를 처리할 때 `Channel`을 사용하는 이유와 다른 Flow 타입들과의 비교를 정리합니다.

---

## SideEffect란?

SideEffect는 **화면에서 한 번만 처리되어야 하는 이벤트**입니다.

| SideEffect 예시 | 설명 |
|----------------|------|
| 네비게이션 | 다른 화면으로 이동 |
| 스낵바/토스트 | 메시지 표시 |
| 다이얼로그 | 알림창 표시 |
| 외부 앱 실행 | 브라우저, 이메일 앱 열기 |

---

## Channel vs StateFlow

### StateFlow의 특징

```kotlin
private val _uiState = MutableStateFlow(HomeUiState())
val uiState: StateFlow<HomeUiState> = _uiState
```

- **항상 값을 가짐**: 초기값 필수
- **최신 값만 유지**: 새로운 값이 들어오면 이전 값 덮어씀
- **replay = 1**: 새로운 Collector는 항상 현재 값을 즉시 수신
- **distinctUntilChanged**: 동일한 값은 중복 방출하지 않음

### StateFlow를 SideEffect에 사용하면 안 되는 이유

```kotlin
// ❌ 잘못된 사용
private val _sideEffect = MutableStateFlow<SideEffect?>(null)

// 문제점:
// 1. 화면 회전 시 마지막 값이 다시 방출됨 → 네비게이션 중복 실행
// 2. 동일한 이벤트 연속 발생 시 무시됨 (distinctUntilChanged)
// 3. null 처리 및 상태 초기화 로직이 추가로 필요함
```

### 비교 표

| 특성 | Channel | StateFlow |
|------|---------|-----------|
| **용도** | 일회성 이벤트 | UI 상태 |
| **초기값** | 불필요 | 필수 |
| **값 유지** | 소비되면 사라짐 | 항상 최신값 유지 |
| **replay** | 없음 | 항상 1 (최신값) |
| **중복 값** | 모두 전달 | 무시 (distinctUntilChanged) |
| **Collector 수** | 1개 (단일 소비) | N개 |

### 언제 사용하나?

```kotlin
// ✅ StateFlow: UI 상태 (화면에 표시되는 데이터)
val uiState: StateFlow<HomeUiState> = _uiState

// ✅ Channel: SideEffect (일회성 이벤트)
val sideEffect = _sideEffect.receiveAsFlow()
```

---

## Channel vs SharedFlow

### SharedFlow의 특징

```kotlin
private val _events = MutableSharedFlow<AppEvent>(
    replay = 0,
    extraBufferCapacity = 1
)
val events: SharedFlow<AppEvent> = _events
```

- **초기값 불필요**: StateFlow와 달리 초기값 없이 생성 가능
- **replay 설정 가능**: 0~N개의 과거 이벤트를 새 Collector에게 전달
- **1:N 브로드캐스트**: 모든 Collector가 동일한 이벤트를 수신
- **중복 값 허용**: 동일한 값도 모두 방출

### SharedFlow를 SideEffect에 사용하면 안 되는 이유

```kotlin
// ❌ 문제가 될 수 있는 사용
private val _sideEffect = MutableSharedFlow<SideEffect>(replay = 0)

// 문제점:
// 1. Collector가 여러 개일 경우 모든 Collector가 이벤트를 수신
//    → 네비게이션이 여러 번 실행될 수 있음
// 2. Collector가 없는 순간에 emit하면 이벤트 손실
//    (extraBufferCapacity 설정으로 해결 가능하지만 Channel이 더 명확)
```

### 비교 표

| 특성 | Channel | SharedFlow |
|------|---------|------------|
| **용도** | 일회성 이벤트 | 브로드캐스트 이벤트 |
| **Collector 수** | 1개 (단일 소비) | N개 (모두 수신) |
| **이벤트 소비** | 한 번만 소비됨 | 모든 Collector가 수신 |
| **replay** | 불가 | 설정 가능 (0~N) |
| **버퍼링** | 기본 지원 | extraBufferCapacity 필요 |
| **Collector 없을 때** | 버퍼에 대기 | 손실 가능 (설정에 따라) |

### 동작 방식 시각화

```
Channel (Queue - 단일 소비):
┌─────┐  ┌─────┐  ┌─────┐
│Event│→ │Event│→ │Event│→ [Single Consumer]
└─────┘  └─────┘  └─────┘
         꺼내면 사라짐 ✅

SharedFlow (Broadcast - 모두 수신):
       ┌→ Collector A (수신)
Event ─┼→ Collector B (수신)
       └→ Collector C (수신)
       모두에게 전달 → SideEffect에 부적합 ❌
```

### 언제 사용하나?

```kotlin
// ✅ SharedFlow: 여러 화면이 동시에 받아야 하는 이벤트
// 예: 로그아웃 이벤트, 테마 변경, 실시간 데이터 스트림
private val _logoutEvent = MutableSharedFlow<Unit>()

// ✅ Channel: SideEffect (정확히 한 번만 처리)
// 예: 네비게이션, 스낵바, 다이얼로그
private val _sideEffect = Channel<HomeSideEffect>()
```

---

## 세 가지 타입 요약

| 요구사항 | 추천 타입 | 이유 |
|---------|----------|------|
| UI 상태 | `StateFlow` | 항상 최신 상태 유지, 화면 재구성 시 즉시 복원 |
| 일회성 이벤트 | `Channel` | 정확히 한 번만 소비, 중복 실행 방지 |
| 브로드캐스트 이벤트 | `SharedFlow` | 여러 Collector가 동시에 수신 필요 |

---

## 프로젝트 적용 예시

### HomeViewModel

```kotlin
// UI 상태: StateFlow
val uiState: StateFlow<HomeUiState> = combine(
    _isLoading,
    _isFirstLaunch,
    getRecentProjectsUseCase(),
    getProStatusUseCase()
) { ... }.stateIn(viewModelScope, ...)

// Side Effect: Channel
private val _sideEffect = Channel<HomeSideEffect>()
val sideEffect = _sideEffect.receiveAsFlow()

private fun loadPdf(file: File) {
    viewModelScope.launch {
        val result = loadPdfUseCase(file)
        result.onSuccess { document ->
            _sideEffect.send(HomeSideEffect.NavigateToEditor(document.id))
        }
    }
}
```

### EditorViewModel

```kotlin
private val _sideEffect = Channel<EditorSideEffect>()
val sideEffect = _sideEffect.receiveAsFlow()

// 다양한 SideEffect 발송
_sideEffect.send(EditorSideEffect.ShowSnackbar(message))
_sideEffect.send(EditorSideEffect.NavigateBack)
_sideEffect.send(EditorSideEffect.OpenSaveLauncher(isLocalFallback = false))
```

### UI에서 수집

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is HomeSideEffect.NavigateToEditor -> {
                    // 네비게이션 실행 (한 번만)
                }
                is HomeSideEffect.ShowNetworkError -> {
                    // 에러 메시지 표시 (한 번만)
                }
            }
        }
    }
}
```

---

## 결론

**Channel**은 MVI 아키텍처에서 SideEffect를 처리하는 가장 안전하고 적합한 선택입니다.

- `StateFlow`는 **상태 유지**가 목적이므로 일회성 이벤트에 부적합
- `SharedFlow`는 **브로드캐스트**가 목적이므로 단일 소비 이벤트에 부적합
- `Channel`은 **단일 소비 + 버퍼링**을 기본 제공하여 SideEffect에 최적화
