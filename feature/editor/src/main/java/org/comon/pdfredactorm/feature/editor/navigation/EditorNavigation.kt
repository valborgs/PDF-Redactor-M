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
    // Create a new ViewModelStore for this specific entry
    // This ensures a fresh ViewModel is created every time this screen is entered
    org.comon.pdfredactorm.core.ui.ScopedViewModelContainer {
        EditorScreen(
            pdfId = key.pdfId,
            onBackClick = onBackClick
        )
    }
}
