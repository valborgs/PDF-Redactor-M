package org.comon.pdfredactorm.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import org.comon.pdfredactorm.BuildConfig
import org.comon.pdfredactorm.feature.home.HomeScreenConfig
import org.comon.pdfredactorm.feature.home.navigation.HomeRoute
import org.comon.pdfredactorm.feature.home.navigation.homeScreen
import org.comon.pdfredactorm.feature.editor.navigation.EditorRoute
import org.comon.pdfredactorm.feature.editor.navigation.editorScreen

/**
 * 앱의 메인 네비게이션 그래프
 * 
 * DroidKnights 2025 스타일: Feature 모듈의 확장함수를 호출하여 그래프 구성
 */
@Composable
fun AppNavHost(navController: NavHostController) {
    val homeScreenConfig = HomeScreenConfig(
        appName = "PDF안심이",
        coffeeChatUrl = BuildConfig.COFFEE_CHAT_URL,
        nativeAdUnitId = BuildConfig.ADMOB_NATIVE_ID
    )

    NavHost(
        navController = navController, 
        startDestination = HomeRoute.ROUTE
    ) {
        homeScreen(
            config = homeScreenConfig,
            onPdfClick = { pdfId ->
                navController.navigate(EditorRoute.createRoute(pdfId))
            }
        )
        
        editorScreen(
            onBackClick = { navController.popBackStack() }
        )
    }
}
