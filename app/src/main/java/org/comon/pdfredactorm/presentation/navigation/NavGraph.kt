package org.comon.pdfredactorm.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.comon.pdfredactorm.presentation.home.HomeScreen
import org.comon.pdfredactorm.presentation.editor.EditorScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onPdfClick = { pdfId ->
                    navController.navigate(Screen.Editor.createRoute(pdfId))
                }
            )
        }
        composable(Screen.Editor.route) { backStackEntry ->
            val pdfId = backStackEntry.arguments?.getString("pdfId")
            if (pdfId != null) {
                EditorScreen(
                    pdfId = pdfId,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
