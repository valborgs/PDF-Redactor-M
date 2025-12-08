package org.comon.pdfredactorm.feature.home.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import org.comon.pdfredactorm.feature.home.HomeScreen
import org.comon.pdfredactorm.feature.home.HomeScreenConfig

/**
 * Navigation3 스타일 - Home Feature entryProvider
 * 
 * @param config 홈 화면 설정
 * @param onPdfClick PDF 클릭 시 호출되는 콜백
 */
@Composable
fun homeEntry(
    config: HomeScreenConfig,
    onPdfClick: (String) -> Unit
): NavEntry<HomeKey> = NavEntry(HomeKey) {
    HomeScreen(
        config = config,
        onPdfClick = onPdfClick
    )
}
