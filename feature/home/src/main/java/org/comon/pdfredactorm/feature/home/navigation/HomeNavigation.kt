package org.comon.pdfredactorm.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import org.comon.pdfredactorm.feature.home.HomeScreen
import org.comon.pdfredactorm.feature.home.HomeScreenConfig

/**
 * Home Feature의 NavGraphBuilder 확장함수
 * 
 * DroidKnights 2025 스타일: 각 Feature가 자신의 네비게이션을 정의
 * 
 * @param config 홈 화면 설정 (appName, coffeeChatUrl, nativeAdUnitId)
 * @param onPdfClick PDF 클릭 시 호출되는 콜백
 */
fun NavGraphBuilder.homeScreen(
    config: HomeScreenConfig,
    onPdfClick: (String) -> Unit
) {
    composable(route = HomeRoute.ROUTE) {
        HomeScreen(
            config = config,
            onPdfClick = onPdfClick
        )
    }
}
