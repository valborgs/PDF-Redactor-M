package org.comon.pdfredactorm.feature.home

import org.comon.pdfredactorm.core.model.PdfDocument
import java.io.File

/**
 * Home Feature MVI Contract
 */

// UI 상태 (단일 State 객체)
data class HomeUiState(
    val isLoading: Boolean = false,
    val isFirstLaunch: Boolean = false,
    val recentProjects: List<PdfDocument> = emptyList(),
    val isProEnabled: Boolean = false
)

// 사용자 의도 (Intent/Event)
sealed interface HomeEvent {
    object ConsumeFirstLaunch : HomeEvent
    data class LoadPdf(val file: File) : HomeEvent
    data class ValidateCode(val email: String, val code: String) : HomeEvent
    data class DeleteProject(val pdfId: String) : HomeEvent
    data object CoffeeChatClicked : HomeEvent
}

// 부수 효과 (Side Effect) - 일회성 이벤트
sealed interface HomeSideEffect {
    data class NavigateToEditor(val pdfId: String) : HomeSideEffect
    data class ShowValidationResult(val result: Result<String>) : HomeSideEffect
    data object ShowNetworkError : HomeSideEffect
}
