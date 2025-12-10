package org.comon.pdfredactorm.feature.editor

import android.net.Uri
import org.comon.pdfredactorm.core.model.DetectedPii

/**
 * Editor 화면의 모든 사용자 행동(Intent)을 정의하는 sealed interface.
 * MVI 패턴에서 View -> ViewModel로 전달되는 이벤트를 나타냅니다.
 */
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
}
