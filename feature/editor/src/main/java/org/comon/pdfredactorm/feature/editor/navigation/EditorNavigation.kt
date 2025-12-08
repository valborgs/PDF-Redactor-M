package org.comon.pdfredactorm.feature.editor.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import org.comon.pdfredactorm.feature.editor.EditorScreen

/**
 * Editor Feature의 NavGraphBuilder 확장함수
 * 
 * DroidKnights 2025 스타일: 각 Feature가 자신의 네비게이션을 정의
 * 
 * @param onBackClick 뒤로가기 클릭 시 호출되는 콜백
 */
fun NavGraphBuilder.editorScreen(
    onBackClick: () -> Unit
) {
    composable(route = EditorRoute.ROUTE) { backStackEntry ->
        val pdfId = backStackEntry.arguments?.getString(EditorRoute.ARG_PDF_ID)
        if (pdfId != null) {
            EditorScreen(
                pdfId = pdfId,
                onBackClick = onBackClick
            )
        }
    }
}
