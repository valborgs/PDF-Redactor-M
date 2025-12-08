package org.comon.pdfredactorm.feature.editor.navigation

/**
 * Editor Feature의 네비게이션 Route 상수
 */
object EditorRoute {
    const val ROUTE = "editor/{pdfId}"
    const val ARG_PDF_ID = "pdfId"
    
    fun createRoute(pdfId: String) = "editor/$pdfId"
}
