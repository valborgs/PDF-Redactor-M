# Editor 화면 MVI 리팩토링 가이드

이 문서는 기존의 MVVM 패턴으로 작성된 `EditorViewModel`과 `EditorScreen`을 MVI(Model-View-Intent) 패턴으로 리팩토링하는 예시를 보여줍니다.

## 1. Intent 정의 (`EditorIntent.kt`)

모든 사용자의 행위(User Action)를 `Sealed Interface`로 정의합니다.

```kotlin
package org.comon.pdfredactorm.feature.editor.mvi

import android.net.Uri
import org.comon.pdfredactorm.core.model.DetectedPii

sealed interface EditorIntent {
    // Navigation & PDF Loading
    data class LoadPdf(val pdfId: String) : EditorIntent
    data object NavigateBack : EditorIntent // Optional: if navigation is handled by intent

    // Page Navigation
    data object NextPage : EditorIntent
    data object PrevPage : EditorIntent
    data class GoToPage(val pageNumber: Int) : EditorIntent

    // Editor Modes
    data object ToggleMaskingMode : EditorIntent
    data object ToggleColorPickingMode : EditorIntent
    data class SetMaskColor(val color: Int) : EditorIntent

    // Redaction Actions
    data class AddRedaction(val x: Float, val y: Float, val width: Float, val height: Float) : EditorIntent
    data class RemoveRedaction(val id: String) : EditorIntent
    data object PerformRedaction : EditorIntent // Save clicked
    data class SaveFinalDocument(val uri: Uri) : EditorIntent

    // PII Detection
    data object DetectPiiInCurrentPage : EditorIntent
    data object DetectPiiInAllPages : EditorIntent
    data class ConvertPiiToMask(val pii: DetectedPii) : EditorIntent
    data class RemoveDetectedPii(val pii: DetectedPii) : EditorIntent
    data object ConsumePiiDetectionResult : EditorIntent

    // Error Handling
    data object ConsumeError : EditorIntent
}
```

## 2. ViewModel 리팩토링 (`EditorViewModel.kt`)

개별 메소드들을 `handleIntent` (또는 `process`) 하나의 진입점으로 통합합니다.

```kotlin
// ... imports

@HiltViewModel
class EditorViewModel @Inject constructor(
    // ... use cases and dependencies
) : ViewModel() {

    // ... State and Effect definitions (Unchanged)
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _sideEffect = Channel<EditorSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()

    // MVI Entry Point
    fun handleIntent(intent: EditorIntent) {
        // Logging the user journey!
        logger.info("Processing Intent: $intent") 
        
        when (intent) {
            is EditorIntent.LoadPdf -> loadPdf(intent.pdfId)
            is EditorIntent.NextPage -> nextPage()
            is EditorIntent.PrevPage -> prevPage()
            is EditorIntent.GoToPage -> goToPage(intent.pageNumber)
            
            is EditorIntent.ToggleMaskingMode -> toggleMaskingMode()
            is EditorIntent.ToggleColorPickingMode -> toggleColorPickingMode()
            is EditorIntent.SetMaskColor -> setMaskColor(intent.color)
            
            is EditorIntent.AddRedaction -> addRedaction(intent.x, intent.y, intent.width, intent.height)
            is EditorIntent.RemoveRedaction -> removeRedaction(intent.id)
            
            is EditorIntent.PerformRedaction -> performRedaction()
            is EditorIntent.SaveFinalDocument -> saveFinalDocument(intent.uri)
            
            is EditorIntent.DetectPiiInCurrentPage -> detectPiiInCurrentPage()
            is EditorIntent.DetectPiiInAllPages -> detectPiiInAllPages()
            is EditorIntent.ConvertPiiToMask -> convertDetectedPiiToMask(intent.pii)
            is EditorIntent.RemoveDetectedPii -> removeDetectedPii(intent.pii)
            is EditorIntent.ConsumePiiDetectionResult -> consumePiiDetectionResult()
            
            is EditorIntent.ConsumeError -> consumeError()
            
            // ... handle other intents
        }
    }

    // Existing private logic methods remain mostly generic, 
    // but are now called exclusively by handleIntent.
    // For brevity, implementation details are omitted if unchanged.
    
    private fun loadPdf(pdfId: String) { /* ... */ }
    private fun nextPage() { /* ... */ }
    // ... etc
}
```

## 3. Screen (UI) 리팩토링 (`EditorScreen.kt`)

ViewModel의 개별 함수를 호출하는 대신 `handleIntent`에 객체를 전달합니다.

```kotlin
@Composable
fun EditorScreen(
    pdfId: String,
    onBackClick: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // ...

    // Example: Page Navigation
    IconButton(onClick = { viewModel.handleIntent(EditorIntent.PrevPage) }) {
        Icon(...)
    }
    
    // Example: Saving
    DropdownMenuItem(
        text = { Text(...) },
        onClick = {
            showMainMenu = false
            viewModel.handleIntent(EditorIntent.PerformRedaction)
        },
        // ...
    )

    // Example: Dialog Interactions
    if (showPageJumpDialog) {
        PageJumpDialog(
            pageCount = uiState.pageCount,
            onDismiss = { showPageJumpDialog = false },
            onJump = { pageNumber ->
                viewModel.handleIntent(EditorIntent.GoToPage(pageNumber))
            }
        )
    }

    // Example: Callback from PdfViewer
    PdfViewer(
        // ...
        onAddRedaction = { x, y, w, h ->
            viewModel.handleIntent(EditorIntent.AddRedaction(x, y, w, h))
        },
        onColorPicked = { color ->
            viewModel.handleIntent(EditorIntent.SetMaskColor(color))
            viewModel.handleIntent(EditorIntent.ToggleColorPickingMode)
        }
        // ...
    )
}
```

## 4. 장점 재확인

이렇게 변경하면 다음과 같은 구체적인 이점을 얻을 수 있습니다:

1.  **로그 일원화**: `handleIntent` 메소드 첫 줄에 로그를 추가하면 사용자가 버튼을 누른 순서(예: `NextPage` -> `ToggleMaskingMode` -> `AddRedaction`)가 그대로 기록됩니다.
2.  **디버깅**: 버그 신고 시 로그에 찍힌 Intent 순서대로 테스트 코드에서 `handleIntent`를 호출하면 상황을 똑같이 재현할 수 있습니다.
3.  **구조적 명확성**: ViewModel의 `public` 메소드 갯수가 줄어들고, 외부에서 호출 가능한 진입점이 하나로 통일됩니다.
