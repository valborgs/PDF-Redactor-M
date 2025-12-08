package org.comon.pdfredactorm.feature.editor.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import org.comon.pdfredactorm.feature.editor.EditorScreen

/**
 * Navigation3 스타일 - Editor Feature entryProvider
 * 
 * @param key EditorKey (pdfId 포함)
 * @param onBackClick 뒤로가기 클릭 시 호출되는 콜백
 */
@Composable
fun editorEntry(
    key: EditorKey,
    onBackClick: () -> Unit
): NavEntry<EditorKey> = NavEntry(key) {
    EditorScreen(
        pdfId = key.pdfId,
        onBackClick = onBackClick
    )
}
