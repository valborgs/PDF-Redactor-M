package org.comon.pdfredactorm.feature.editor.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation3 - Editor 화면 네비게이션 키
 */
@Serializable
data class EditorKey(val pdfId: String) : NavKey
