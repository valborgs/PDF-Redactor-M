package org.comon.pdfredactorm.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Editor : Screen("editor/{pdfId}") {
        fun createRoute(pdfId: String) = "editor/$pdfId"
    }
}
