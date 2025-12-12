package org.comon.pdfredactorm.feature.editor

import android.graphics.Bitmap
import android.net.Uri
import org.comon.pdfredactorm.core.model.DetectedPii
import org.comon.pdfredactorm.core.model.PdfDocument
import org.comon.pdfredactorm.core.model.PdfOutlineItem
import org.comon.pdfredactorm.core.model.RedactionMask
import java.io.File

/**
 * Editor Feature MVI Contract
 */

// UI 상태 (단일 State 객체)
data class EditorUiState(
    val document: PdfDocument? = null,
    val currentPage: Int = 0,
    val pageCount: Int = 0,
    val currentPageBitmap: Bitmap? = null,
    val pdfPageWidth: Int = 0,
    val pdfPageHeight: Int = 0,
    val redactions: List<RedactionMask> = emptyList(),
    val detectedPii: List<DetectedPii> = emptyList(),
    val tableOfContents: List<PdfOutlineItem> = emptyList(),
    val isMaskingMode: Boolean = false,
    val isDetecting: Boolean = false,
    val isLoading: Boolean = false,
    val saveSuccess: Boolean = false,
    val currentMaskColor: Int = 0xFF000000.toInt(), // Default: Black
    val isColorPickingMode: Boolean = false,
    val tempRedactedFile: File? = null,
    val piiDetectionCount: Int? = null,
    val isProEnabled: Boolean = false
)

// 사용자 의도 (Intent/Event)
sealed interface EditorIntent {
    // PDF 로딩
    data class LoadPdf(val pdfId: String) : EditorIntent

    // 페이지 네비게이션
    data object NextPage : EditorIntent
    data object PrevPage : EditorIntent
    data class GoToPage(val pageNumber: Int) : EditorIntent

    // 편집 모드
    data object ToggleMaskingMode : EditorIntent
    data object ToggleColorPickingMode : EditorIntent
    data class SetMaskColor(val color: Int) : EditorIntent

    // 마스킹 작업
    data class AddRedaction(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    ) : EditorIntent
    data class RemoveRedaction(val id: String) : EditorIntent

    // 저장
    data object PerformRedaction : EditorIntent
    data class SaveFinalDocument(val uri: Uri) : EditorIntent

    // PII 탐지
    data object DetectPiiInCurrentPage : EditorIntent
    data object DetectPiiInAllPages : EditorIntent
    data class ConvertPiiToMask(val pii: DetectedPii) : EditorIntent
    data class RemoveDetectedPii(val pii: DetectedPii) : EditorIntent
    data object ConsumePiiDetectionResult : EditorIntent
    data object ProceedWithLocalRedaction : EditorIntent
}

// 부수 효과 (Side Effect) - 일회성 이벤트
sealed interface EditorSideEffect {
    data class OpenSaveLauncher(val isLocalFallback: Boolean = false) : EditorSideEffect
    data class ShowSnackbar(val message: String) : EditorSideEffect
    data object NavigateBack : EditorSideEffect
    data object ShowFileSizeExceededDialog : EditorSideEffect
    data object ShowNetworkFallbackDialog : EditorSideEffect
}

