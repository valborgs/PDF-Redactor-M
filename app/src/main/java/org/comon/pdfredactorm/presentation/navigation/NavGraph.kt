package org.comon.pdfredactorm.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.comon.pdfredactorm.BuildConfig
import org.comon.pdfredactorm.feature.home.HomeScreen
import org.comon.pdfredactorm.feature.home.HomeScreenConfig
import org.comon.pdfredactorm.feature.editor.EditorScreen

@Composable
fun NavGraph(navController: NavHostController) {
    val homeScreenConfig = HomeScreenConfig(
        appName = "PDF안심이",
        coffeeChatUrl = BuildConfig.COFFEE_CHAT_URL,
        nativeAdUnitId = BuildConfig.ADMOB_NATIVE_ID
    )

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                config = homeScreenConfig,
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
